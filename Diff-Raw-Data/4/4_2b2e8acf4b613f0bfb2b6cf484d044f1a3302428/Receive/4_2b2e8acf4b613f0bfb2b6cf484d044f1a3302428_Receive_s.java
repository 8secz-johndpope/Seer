 // Copyright (C) 2008 The Android Open Source Project
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 // http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 package com.google.gerrit.server.ssh;
 
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_HEAD;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_HEAD_CREATE;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_HEAD_REPLACE;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_HEAD_UPDATE;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_TAG;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_TAG_ANNOTATED;
 import static com.google.gerrit.client.reviewdb.ApprovalCategory.PUSH_TAG_ANY;
 
 import com.google.gerrit.client.Link;
 import com.google.gerrit.client.data.ApprovalType;
 import com.google.gerrit.client.reviewdb.AbstractAgreement;
 import com.google.gerrit.client.reviewdb.Account;
 import com.google.gerrit.client.reviewdb.AccountAgreement;
 import com.google.gerrit.client.reviewdb.AccountExternalId;
 import com.google.gerrit.client.reviewdb.AccountGroup;
 import com.google.gerrit.client.reviewdb.AccountGroupAgreement;
 import com.google.gerrit.client.reviewdb.ApprovalCategory;
 import com.google.gerrit.client.reviewdb.Branch;
 import com.google.gerrit.client.reviewdb.Change;
 import com.google.gerrit.client.reviewdb.ChangeApproval;
 import com.google.gerrit.client.reviewdb.ChangeMessage;
 import com.google.gerrit.client.reviewdb.ContributorAgreement;
 import com.google.gerrit.client.reviewdb.PatchSet;
 import com.google.gerrit.client.reviewdb.PatchSetInfo;
 import com.google.gerrit.client.reviewdb.ReviewDb;
 import com.google.gerrit.client.rpc.Common;
 import com.google.gerrit.git.PatchSetImporter;
 import com.google.gerrit.git.PushQueue;
 import com.google.gerrit.server.ChangeUtil;
 import com.google.gerrit.server.GerritServer;
 import com.google.gerrit.server.mail.CreateChangeSender;
 import com.google.gerrit.server.mail.EmailException;
 import com.google.gerrit.server.mail.MergedSender;
 import com.google.gerrit.server.mail.ReplacePatchSetSender;
 import com.google.gwtorm.client.OrmException;
 import com.google.gwtorm.client.OrmRunnable;
 import com.google.gwtorm.client.Transaction;
 
 import org.kohsuke.args4j.CmdLineException;
 import org.kohsuke.args4j.Option;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.spearce.jgit.lib.Constants;
 import org.spearce.jgit.lib.ObjectId;
 import org.spearce.jgit.lib.PersonIdent;
 import org.spearce.jgit.lib.Ref;
 import org.spearce.jgit.lib.RefUpdate;
 import org.spearce.jgit.revwalk.RevCommit;
 import org.spearce.jgit.revwalk.RevObject;
 import org.spearce.jgit.revwalk.RevSort;
 import org.spearce.jgit.revwalk.RevTag;
 import org.spearce.jgit.revwalk.RevWalk;
 import org.spearce.jgit.transport.PostReceiveHook;
 import org.spearce.jgit.transport.PreReceiveHook;
 import org.spearce.jgit.transport.ReceiveCommand;
 import org.spearce.jgit.transport.ReceivePack;
 import org.spearce.jgit.transport.ReceiveCommand.Result;
 import org.spearce.jgit.transport.ReceiveCommand.Type;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.sql.Timestamp;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /** Receives change upload over SSH using the Git receive-pack protocol. */
 class Receive extends AbstractGitCommand {
   private static final Logger log = LoggerFactory.getLogger(Receive.class);
 
   private static final String NEW_CHANGE = "refs/for/";
   private static final Pattern NEW_PATCHSET =
       Pattern.compile("^refs/changes/(?:[0-9][0-9]/)?([1-9][0-9]*)(?:/new)?$");
 
   private final Set<Account.Id> reviewerId = new HashSet<Account.Id>();
   private final Set<Account.Id> ccId = new HashSet<Account.Id>();
 
   @Option(name = "--reviewer", aliases = {"--re"}, multiValued = true, metaVar = "EMAIL", usage = "request reviewer for change(s)")
   void addReviewer(final String nameOrEmail) throws CmdLineException {
     reviewerId.add(toAccountId(nameOrEmail));
   }
 
   @Option(name = "--cc", aliases = {}, multiValued = true, metaVar = "EMAIL", usage = "CC user on change(s)")
   void addCC(final String nameOrEmail) throws CmdLineException {
     ccId.add(toAccountId(nameOrEmail));
   }
 
   private GerritServer server;
   private ReceivePack rp;
   private PersonIdent refLogIdent;
   private ReceiveCommand newChange;
   private Branch destBranch;
 
   private final Set<String> myEmails = new HashSet<String>();
   private final List<Change.Id> allNewChanges = new ArrayList<Change.Id>();
 
   private final Map<Change.Id, ReceiveCommand> addByChange =
       new HashMap<Change.Id, ReceiveCommand>();
   private final Map<ObjectId, Change> addByCommit =
       new HashMap<ObjectId, Change>();
   private final Map<Change.Id, Change> changeCache =
       new HashMap<Change.Id, Change>();
 
   private Map<ObjectId, Ref> refsById;
 
   @Override
   protected void preRun() throws Failure {
     super.preRun();
     server = getGerritServer();
   }
 
   @Override
   protected void runImpl() throws IOException, Failure {
     if (Common.getGerritConfig().isUseContributorAgreements()
         && proj.isUseContributorAgreements()) {
       verifyActiveContributorAgreement();
     }
     loadMyEmails();
     refLogIdent = ChangeUtil.toReflogIdent(userAccount, getRemoteAddress());
 
     rp = new ReceivePack(repo);
     rp.setAllowCreates(true);
     rp.setAllowDeletes(true);
     rp.setAllowNonFastForwards(true);
     rp.setCheckReceivedObjects(true);
     rp.setRefLogIdent(refLogIdent);
     rp.setPreReceiveHook(new PreReceiveHook() {
       public void onPreReceive(final ReceivePack arg0,
           final Collection<ReceiveCommand> commands) {
         parseCommands(commands);
         createNewChanges();
         appendPatchSets();
       }
     });
     rp.setPostReceiveHook(new PostReceiveHook() {
       public void onPostReceive(final ReceivePack arg0,
           final Collection<ReceiveCommand> commands) {
         for (final ReceiveCommand c : commands) {
           if (c.getResult() == Result.OK) {
             if (isHead(c)) {
               switch (c.getType()) {
                 case CREATE:
                   insertBranchEntity(c);
                   autoCloseChanges(c);
                   break;
                 case DELETE:
                   deleteBranchEntity(c);
                   break;
                 case UPDATE:
                 case UPDATE_NONFASTFORWARD:
                   autoCloseChanges(c);
                   break;
               }
             }
 
             if (isHead(c) || isTag(c)) {
               // We only schedule heads and tags for replication.
               // Change refs are scheduled when they are created.
               //
               PushQueue.scheduleUpdate(proj.getNameKey(), c.getRefName());
             }
           }
         }
       }
     });
     rp.receive(in, out, err);
 
     if (!allNewChanges.isEmpty() && server.getCanonicalURL() != null) {
       // Make sure there isn't anything buffered; we want to give the
       // push client a chance to display its status report before we
       // show our own messages on standard error.
       //
       out.flush();
 
       final String url = server.getCanonicalURL();
       final PrintWriter msg = toPrintWriter(err);
       msg.write("\nNew Changes:\n");
       for (final Change.Id c : allNewChanges) {
         msg.write("  " + url + c.get() + "\n");
       }
       msg.write('\n');
       msg.flush();
     }
   }
 
   private void verifyActiveContributorAgreement() throws Failure {
     AbstractAgreement bestAgreement = null;
     ContributorAgreement bestCla = null;
     try {
       OUTER: for (final AccountGroup.Id groupId : getGroups()) {
         for (final AccountGroupAgreement a : db.accountGroupAgreements()
             .byGroup(groupId)) {
           final ContributorAgreement cla =
               db.contributorAgreements().get(a.getAgreementId());
           if (cla == null) {
             continue;
           }
 
           bestAgreement = a;
           bestCla = cla;
           break OUTER;
         }
       }
 
       if (bestAgreement == null) {
         for (final AccountAgreement a : db.accountAgreements().byAccount(
             userAccount.getId()).toList()) {
           final ContributorAgreement cla =
               db.contributorAgreements().get(a.getAgreementId());
           if (cla == null) {
             continue;
           }
 
           bestAgreement = a;
           bestCla = cla;
           break;
         }
       }
     } catch (OrmException e) {
       throw new Failure(1, "fatal: database error", e);
     }
 
     if (bestCla != null && !bestCla.isActive()) {
       final StringBuilder msg = new StringBuilder();
       msg.append("\nfatal: ");
       msg.append(bestCla.getShortName());
       msg.append(" contributor agreement is expired.\n");
       if (server.getCanonicalURL() != null) {
         msg.append("\nPlease complete a new agreement");
         msg.append(":\n\n  ");
         msg.append(server.getCanonicalURL());
         msg.append("Gerrit#");
         msg.append(Link.SETTINGS_AGREEMENTS);
         msg.append("\n");
       }
       msg.append("\n");
       throw new UnloggedFailure(1, msg.toString());
     }
 
     if (bestCla != null && bestCla.isRequireContactInformation()) {
       boolean fail = false;
       fail |= missing(userAccount.getFullName());
       fail |= missing(userAccount.getPreferredEmail());
       fail |= !userAccount.isContactFiled();
 
       if (fail) {
         final StringBuilder msg = new StringBuilder();
         msg.append("\nfatal: ");
         msg.append(bestCla.getShortName());
         msg.append(" contributor agreement requires");
         msg.append(" current contact information.\n");
         if (server.getCanonicalURL() != null) {
           msg.append("\nPlease review your contact information");
           msg.append(":\n\n  ");
           msg.append(server.getCanonicalURL());
           msg.append("Gerrit#");
           msg.append(Link.SETTINGS_CONTACT);
           msg.append("\n");
         }
         msg.append("\n");
         throw new UnloggedFailure(1, msg.toString());
       }
     }
 
     if (bestAgreement != null) {
       switch (bestAgreement.getStatus()) {
         case VERIFIED:
           return;
         case REJECTED:
           throw new UnloggedFailure(1, "\nfatal: " + bestCla.getShortName()
               + " contributor agreement was rejected."
               + "\n       (rejected on " + bestAgreement.getReviewedOn()
               + ")\n");
         case NEW:
           throw new UnloggedFailure(1, "\nfatal: " + bestCla.getShortName()
               + " contributor agreement is still pending review.\n");
       }
     }
 
     final StringBuilder msg = new StringBuilder();
     msg.append("\nfatal: A Contributor Agreement"
         + " must be completed before uploading");
     if (server.getCanonicalURL() != null) {
       msg.append(":\n\n  ");
       msg.append(server.getCanonicalURL());
       msg.append("Gerrit#");
       msg.append(Link.SETTINGS_AGREEMENTS);
       msg.append("\n");
     } else {
       msg.append(".");
     }
     msg.append("\n");
     throw new UnloggedFailure(1, msg.toString());
   }
 
   private static boolean missing(final String value) {
     return value == null || value.trim().equals("");
   }
 
   private void loadMyEmails() throws Failure {
     addEmail(userAccount.getPreferredEmail());
     try {
       for (final AccountExternalId id : db.accountExternalIds().byAccount(
           userAccount.getId())) {
         addEmail(id.getEmailAddress());
       }
     } catch (OrmException e) {
       throw new Failure(1, "fatal: database error", e);
     }
   }
 
   private void addEmail(final String email) {
     if (email != null && email.length() > 0) {
       myEmails.add(email);
     }
   }
 
   private Account.Id toAccountId(final String nameOrEmail)
       throws CmdLineException {
     final String efmt = server.getEmailFormat();
     final boolean haveFormat = efmt != null && efmt.contains("{0}");
     try {
       final HashSet<Account.Id> matches = new HashSet<Account.Id>();
       String email = splitEmail(nameOrEmail);
 
       if (email == null && haveFormat && !nameOrEmail.contains(" ")) {
         // Not a full name, since it has no space, and not an email
         // address either. Assume it is just the local portion of
         // the organizations standard email format, and complete out.
         //
         email = MessageFormat.format(efmt, nameOrEmail);
       }
 
       if (email == null) {
         // Not an email address implies it was a full name, search by
         // full name hoping to get a unique match.
         //
         final String n = nameOrEmail;
         for (final Account a : db.accounts().suggestByFullName(n, n, 2)) {
           matches.add(a.getId());
         }
       } else {
         // Scan email addresses for any potential matches.
         //
         for (final AccountExternalId e : db.accountExternalIds()
             .byEmailAddress(email)) {
           matches.add(e.getAccountId());
         }
         if (matches.isEmpty()) {
           for (final Account a : db.accounts().byPreferredEmail(email)) {
             matches.add(a.getId());
           }
         }
       }
 
       switch (matches.size()) {
         case 0:
           throw new CmdLineException("\"" + nameOrEmail
               + "\" is not registered");
         case 1:
           return (matches.iterator().next());
         default:
           throw new CmdLineException("\"" + nameOrEmail
               + "\" matches multiple accounts");
       }
     } catch (OrmException e) {
       log.error("Cannot lookup name/email address", e);
       throw new CmdLineException("database is down");
     }
   }
 
   private static String splitEmail(final String nameOrEmail) {
     final int lt = nameOrEmail.indexOf('<');
     final int gt = nameOrEmail.indexOf('>');
     if (lt >= 0 && gt > lt) {
       return nameOrEmail.substring(lt + 1, gt);
     }
     if (nameOrEmail.contains("@")) {
       return nameOrEmail;
     }
     return null;
   }
 
   private void parseCommands(final Collection<ReceiveCommand> commands) {
     for (final ReceiveCommand cmd : commands) {
       if (cmd.getResult() != ReceiveCommand.Result.NOT_ATTEMPTED) {
         // Already rejected by the core receive process.
         //
         continue;
       }
 
       if (cmd.getRefName().startsWith(NEW_CHANGE)) {
         parseNewChangeCommand(cmd);
         continue;
       }
 
       final Matcher m = NEW_PATCHSET.matcher(cmd.getRefName());
       if (m.matches()) {
         // The referenced change must exist and must still be open.
         //
         final Change.Id changeId = Change.Id.parse(m.group(1));
         parseNewPatchSetCommand(cmd, changeId);
         continue;
       }
 
       switch (cmd.getType()) {
         case CREATE:
           parseCreate(cmd);
           continue;
 
         case UPDATE:
           parseUpdate(cmd);
           continue;
 
         case DELETE:
         case UPDATE_NONFASTFORWARD:
           parseRewindOrDelete(cmd);
           continue;
       }
 
       // Everything else is bogus as far as we are concerned.
       //
       reject(cmd);
     }
   }
 
   private void parseCreate(final ReceiveCommand cmd) {
     if (isHead(cmd) && canPerform(PUSH_HEAD, PUSH_HEAD_CREATE)) {
       // Let the core receive process handle it
 
     } else if (isTag(cmd) && canPerform(PUSH_TAG, (short) 1)) {
       parseCreateTag(cmd);
 
     } else {
       reject(cmd);
     }
   }
 
   private void parseCreateTag(final ReceiveCommand cmd) {
     try {
       final RevObject obj = rp.getRevWalk().parseAny(cmd.getNewId());
       if (!(obj instanceof RevTag)) {
         reject(cmd, "not annotated tag");
         return;
       }
 
       if (canPerform(PUSH_TAG, PUSH_TAG_ANY)) {
         // If we can push any tag, validation is sufficient at this point.
         //
         return;
       }
 
       final RevTag tag = (RevTag) obj;
       final PersonIdent tagger = tag.getTaggerIdent();
       if (tagger == null) {
         reject(cmd, "no tagger");
         return;
       }
 
       final String email = tagger.getEmailAddress();
       if (!myEmails.contains(email)) {
         reject(cmd, "invalid tagger " + email);
         return;
       }
 
       if (tag.getFullMessage().contains("-----BEGIN PGP SIGNATURE-----\n")) {
         // Signed tags are currently assumed valid, as we don't have a GnuPG
         // key ring to validate them against, and we might be missing the
         // necessary (but currently optional) BouncyCastle Crypto libraries.
         //
       } else if (canPerform(PUSH_TAG, PUSH_TAG_ANNOTATED)) {
         // User is permitted to push an unsigned annotated tag.
         //
       } else {
         reject(cmd, "must be signed");
         return;
       }
 
       // Let the core receive process handle it
       //
     } catch (IOException e) {
       log.error("Bad tag " + cmd.getRefName() + " " + cmd.getNewId().name(), e);
       reject(cmd, "invalid object");
     }
   }
 
   private void parseUpdate(final ReceiveCommand cmd) {
     if (isHead(cmd) && canPerform(PUSH_HEAD, PUSH_HEAD_UPDATE)) {
       // Let the core receive process handle it
     } else {
       reject(cmd);
     }
   }
 
   private void parseRewindOrDelete(final ReceiveCommand cmd) {
     if (isHead(cmd) && canPerform(PUSH_HEAD, PUSH_HEAD_REPLACE)) {
       // Let the core receive process handle it
 
     } else if (isHead(cmd) && cmd.getType() == Type.UPDATE_NONFASTFORWARD) {
       cmd.setResult(ReceiveCommand.Result.REJECTED_NONFASTFORWARD);
 
     } else {
       reject(cmd);
     }
   }
 
   private void parseNewChangeCommand(final ReceiveCommand cmd) {
     // Permit exactly one new change request per push.
     //
     if (newChange != null) {
       reject(cmd, "duplicate request");
       return;
     }
 
     newChange = cmd;
     String destBranchName = cmd.getRefName().substring(NEW_CHANGE.length());
     if (!destBranchName.startsWith(Constants.R_REFS)) {
       destBranchName = Constants.R_HEADS + destBranchName;
     }
 
     try {
       destBranch =
           db.branches().get(
               new Branch.NameKey(proj.getNameKey(), destBranchName));
     } catch (OrmException e) {
       log.error("Cannot lookup branch " + proj + " " + destBranchName, e);
       reject(cmd, "database error");
       return;
     }
     if (destBranch == null) {
       String n = destBranchName;
       if (n.startsWith(Constants.R_HEADS))
         n = n.substring(Constants.R_HEADS.length());
       reject(cmd, "branch " + n + " not found");
       return;
     }
   }
 
   private void parseNewPatchSetCommand(final ReceiveCommand cmd,
       final Change.Id changeId) {
     if (cmd.getType() != ReceiveCommand.Type.CREATE) {
       reject(cmd, "invalid usage");
       return;
     }
 
     final Change changeEnt;
     try {
       changeEnt = db.changes().get(changeId);
     } catch (OrmException e) {
       log.error("Cannot lookup existing change " + changeId, e);
       reject(cmd, "database error");
       return;
     }
     if (changeEnt == null) {
       reject(cmd, "change " + changeId.get() + " not found");
       return;
     }
     if (changeEnt.getStatus().isClosed()) {
       reject(cmd, "change " + changeId.get() + " closed");
       return;
     }
 
     if (addByChange.containsKey(changeId)) {
       reject(cmd, "duplicate request");
       return;
     }
     if (addByCommit.containsKey(cmd.getNewId())) {
       reject(cmd, "duplicate request");
       return;
     }
 
     addByChange.put(changeId, cmd);
     addByCommit.put(cmd.getNewId(), changeEnt);
     changeCache.put(changeId, changeEnt);
   }
 
   private void createNewChanges() {
     if (newChange == null
         || newChange.getResult() != ReceiveCommand.Result.NOT_ATTEMPTED) {
       return;
     }
 
     final List<RevCommit> toCreate = new ArrayList<RevCommit>();
     final RevWalk walk = rp.getRevWalk();
     walk.reset();
     walk.sort(RevSort.TOPO);
     walk.sort(RevSort.REVERSE, true);
     try {
       walk.markStart(walk.parseCommit(newChange.getNewId()));
       for (final Ref r : rp.getAdvertisedRefs().values()) {
         try {
           walk.markUninteresting(walk.parseCommit(r.getObjectId()));
         } catch (IOException e) {
           continue;
         }
       }
 
       for (;;) {
         final RevCommit c = walk.next();
         if (c == null) {
           break;
         }
         if (addByCommit.containsKey(c.copy())) {
           // This commit is slated to replace an existing PatchSet.
           //
           continue;
         }
         if (!validCommitter(newChange, c)) {
           return;
         }
         toCreate.add(c);
       }
     } catch (IOException e) {
       // Should never happen, the core receive process would have
       // identified the missing object earlier before we got control.
       //
       newChange.setResult(Result.REJECTED_MISSING_OBJECT);
       log.error("Invalid pack upload; one or more objects weren't sent", e);
     }
 
     if (toCreate.isEmpty() && addByChange.isEmpty()) {
       reject(newChange, "no new changes");
       return;
     }
 
     for (final RevCommit c : toCreate) {
       try {
         createChange(walk, c);
       } catch (IOException e) {
         log.error("Error computing patch of commit " + c.name(), e);
         reject(newChange, "diff error");
         return;
       } catch (OrmException e) {
         log.error("Error creating change for commit " + c.name(), e);
         reject(newChange, "database error");
         return;
       }
     }
     newChange.setResult(ReceiveCommand.Result.OK);
   }
 
   private void createChange(final RevWalk walk, final RevCommit c)
       throws OrmException, IOException {
     walk.parseBody(c);
 
     final Transaction txn = db.beginTransaction();
     final Account.Id me = userAccount.getId();
     final Change change =
         new Change(new Change.Id(db.nextChangeId()), me, destBranch
             .getNameKey());
     final PatchSet ps = new PatchSet(change.newPatchSetId());
     ps.setCreatedOn(change.getCreatedOn());
     ps.setUploader(me);
 
     final PatchSetImporter imp =
         new PatchSetImporter(server, db, proj.getNameKey(), repo, c, ps, true);
     imp.setTransaction(txn);
     imp.run();
 
     change.setCurrentPatchSet(imp.getPatchSetInfo());
     ChangeUtil.updated(change);
     db.changes().insert(Collections.singleton(change), txn);
 
     final Set<Account.Id> haveApprovals = new HashSet<Account.Id>();
     final List<ApprovalType> allTypes =
         Common.getGerritConfig().getApprovalTypes();
     haveApprovals.add(me);
 
     if (allTypes.size() > 0) {
       final Account.Id authorId =
           imp.getPatchSetInfo().getAuthor() != null ? imp.getPatchSetInfo()
               .getAuthor().getAccount() : null;
       final Account.Id committerId =
           imp.getPatchSetInfo().getCommitter() != null ? imp.getPatchSetInfo()
               .getCommitter().getAccount() : null;
       final ApprovalCategory.Id catId =
           allTypes.get(allTypes.size() - 1).getCategory().getId();
       if (authorId != null && haveApprovals.add(authorId)) {
         db.changeApprovals().insert(
             Collections.singleton(new ChangeApproval(new ChangeApproval.Key(
                 change.getId(), authorId, catId), (short) 0)), txn);
       }
       if (committerId != null && haveApprovals.add(committerId)) {
         db.changeApprovals().insert(
             Collections.singleton(new ChangeApproval(new ChangeApproval.Key(
                 change.getId(), committerId, catId), (short) 0)), txn);
       }
       for (final Account.Id reviewer : reviewerId) {
         if (haveApprovals.add(reviewer)) {
           db.changeApprovals().insert(
               Collections.singleton(new ChangeApproval(new ChangeApproval.Key(
                   change.getId(), reviewer, catId), (short) 0)), txn);
         }
       }
     }
 
     txn.commit();
 
     final RefUpdate ru = repo.updateRef(ps.getRefName());
     ru.setForceUpdate(true);
     ru.setNewObjectId(c);
     ru.setRefLogIdent(refLogIdent);
     ru.setRefLogMessage("uploaded", false);
     if (ru.update(walk) != RefUpdate.Result.NEW) {
       throw new IOException("Failed to create ref " + ps.getRefName() + " in "
           + repo.getDirectory() + ": " + ru.getResult());
     }
     PushQueue.scheduleUpdate(proj.getNameKey(), ru.getName());
 
     allNewChanges.add(change.getId());
 
     try {
       final CreateChangeSender cm = new CreateChangeSender(server, change);
       cm.setFrom(me);
       cm.setPatchSet(ps, imp.getPatchSetInfo());
       cm.setReviewDb(db);
       cm.addReviewers(reviewerId);
       cm.addExtraCC(ccId);
       cm.send();
     } catch (EmailException e) {
       log.error("Cannot send email for new change " + change.getId(), e);
     }
   }
 
   private void appendPatchSets() {
     for (Map.Entry<Change.Id, ReceiveCommand> e : addByChange.entrySet()) {
       final ReceiveCommand cmd = e.getValue();
       final Change.Id changeId = e.getKey();
       try {
         appendPatchSet(changeId, cmd);
       } catch (IOException err) {
         log.error("Error computing replacement patch for change " + changeId
             + ", commit " + cmd.getNewId().name(), err);
         reject(cmd, "diff error");
       } catch (OrmException err) {
         log.error("Error storing replacement patch for change " + changeId
             + ", commit " + cmd.getNewId().name(), err);
         reject(cmd, "database error");
       }
       if (cmd.getResult() == ReceiveCommand.Result.NOT_ATTEMPTED) {
         log.error("Replacement patch for change " + changeId + ", commit "
             + cmd.getNewId().name() + " wasn't attempted."
             + "  This is a bug in the receive process implementation.");
         reject(cmd, "internal error");
       }
     }
   }
 
   private void appendPatchSet(final Change.Id changeId, final ReceiveCommand cmd)
       throws IOException, OrmException {
     final RevCommit c = rp.getRevWalk().parseCommit(cmd.getNewId());
     rp.getRevWalk().parseBody(c);
     if (!validCommitter(cmd, c)) {
       return;
     }
 
     final Account.Id me = userAccount.getId();
     final ReplaceResult result;
 
     final Set<Account.Id> oldReviewers = new HashSet<Account.Id>();
     final Set<Account.Id> oldCC = new HashSet<Account.Id>();
 
     result = db.run(new OrmRunnable<ReplaceResult, ReviewDb>() {
       public ReplaceResult run(final ReviewDb db, final Transaction txn,
           final boolean isRetry) throws OrmException {
         final Change change;
         if (isRetry) {
           change = db.changes().get(changeId);
           if (change == null) {
             reject(cmd, "change " + changeId.get() + " not found");
             return null;
           }
           if (change.getStatus().isClosed()) {
             reject(cmd, "change " + changeId.get() + " closed");
             return null;
           }
           if (change.getDest() == null
               || !proj.getNameKey().equals(change.getDest().getParentKey())) {
             reject(cmd, "change " + changeId.get() + " not in "
                 + proj.getName());
             return null;
           }
         } else {
           change = changeCache.get(changeId);
         }
 
         final HashSet<String> existingRevisions = new HashSet<String>();
         for (final PatchSet ps : db.patchSets().byChange(changeId)) {
           if (ps.getRevision() != null) {
             existingRevisions.add(ps.getRevision().get());
           }
         }
 
         // Don't allow the same commit to appear twice on the same change
         //
         if (existingRevisions.contains(c.name())) {
           reject(cmd, "patch set exists");
           return null;
         }
 
         // Don't allow a change to directly depend upon itself. This is a
         // very common error due to users making a new commit rather than
         // amending when trying to address review comments.
         //
         for (final RevCommit p : c.getParents()) {
           if (existingRevisions.contains(p.name())) {
             reject(cmd, "squash commits first");
             return null;
           }
         }
 
         final PatchSet ps = new PatchSet(change.newPatchSetId());
         ps.setCreatedOn(new Timestamp(System.currentTimeMillis()));
         ps.setUploader(userAccount.getId());
 
         final PatchSetImporter imp =
             new PatchSetImporter(server, db, proj.getNameKey(), repo, c, ps,
                 true);
         imp.setTransaction(txn);
         try {
           imp.run();
         } catch (IOException e) {
           throw new OrmException(e);
         }
 
         final Ref mergedInto = findMergedInto(change.getDest().get(), c);
         final ReplaceResult result = new ReplaceResult();
         result.mergedIntoRef = mergedInto != null ? mergedInto.getName() : null;
         result.change = change;
         result.patchSet = ps;
         result.info = imp.getPatchSetInfo();
 
         final Account.Id authorId =
             imp.getPatchSetInfo().getAuthor() != null ? imp.getPatchSetInfo()
                 .getAuthor().getAccount() : null;
         final Account.Id committerId =
             imp.getPatchSetInfo().getCommitter() != null ? imp
                 .getPatchSetInfo().getCommitter().getAccount() : null;
 
         boolean haveAuthor = false;
         boolean haveCommitter = false;
         final Set<Account.Id> haveApprovals = new HashSet<Account.Id>();
 
         oldReviewers.clear();
         oldCC.clear();
 
         result.approvals =
             db.changeApprovals().byChange(change.getId()).toList();
         for (ChangeApproval a : result.approvals) {
           haveApprovals.add(a.getAccountId());
 
           if (a.getValue() != 0) {
             oldReviewers.add(a.getAccountId());
           } else {
             oldCC.add(a.getAccountId());
           }
 
           if (!haveAuthor && authorId != null
               && a.getAccountId().equals(authorId)) {
             haveAuthor = true;
           }
           if (!haveCommitter && committerId != null
               && a.getAccountId().equals(committerId)) {
             haveCommitter = true;
           }
 
           if (me.equals(a.getAccountId())) {
             if (a.getValue() > 0
                 && ApprovalCategory.SUBMIT.equals(a.getCategoryId())) {
               a.clear();
             } else {
               // Leave my own approvals alone.
               //
             }
           } else if (a.getValue() > 0) {
             a.clear();
           }
         }
 
         final ChangeMessage msg =
             new ChangeMessage(new ChangeMessage.Key(change.getId(), ChangeUtil
                 .messageUUID(db)), me, ps.getCreatedOn());
         msg.setMessage("Uploaded patch set " + ps.getPatchSetId() + ".");
         db.changeMessages().insert(Collections.singleton(msg), txn);
         result.msg = msg;
 
         if (result.mergedIntoRef != null) {
           // Change was already submitted to a branch, close it.
           //
           markChangeMergedByPush(db, txn, result);
         } else {
           // Change should be new, so it can go through review again.
           //
           change.setStatus(Change.Status.NEW);
           change.setCurrentPatchSet(imp.getPatchSetInfo());
           ChangeUtil.updated(change);
           db.changeApprovals().update(result.approvals, txn);
           db.changes().update(Collections.singleton(change), txn);
         }
 
         final List<ApprovalType> allTypes =
             Common.getGerritConfig().getApprovalTypes();
         if (allTypes.size() > 0) {
           final ApprovalCategory.Id catId =
               allTypes.get(allTypes.size() - 1).getCategory().getId();
           if (authorId != null && haveApprovals.add(authorId)) {
             insertDummyChangeApproval(result, authorId, catId, db, txn);
           }
           if (committerId != null && haveApprovals.add(committerId)) {
             insertDummyChangeApproval(result, committerId, catId, db, txn);
           }
           for (final Account.Id reviewer : reviewerId) {
            insertDummyChangeApproval(result, reviewer, catId, db, txn);
           }
         }
         return result;
       }
     });
     if (result != null) {
       final PatchSet ps = result.patchSet;
       final RefUpdate ru = repo.updateRef(ps.getRefName());
       ru.setForceUpdate(true);
       ru.setNewObjectId(c);
       ru.setRefLogIdent(refLogIdent);
       ru.setRefLogMessage("uploaded", false);
       if (ru.update(rp.getRevWalk()) != RefUpdate.Result.NEW) {
         throw new IOException("Failed to create ref " + ps.getRefName()
             + " in " + repo.getDirectory() + ": " + ru.getResult());
       }
       PushQueue.scheduleUpdate(proj.getNameKey(), ru.getName());
       cmd.setResult(ReceiveCommand.Result.OK);
 
       try {
         final ReplacePatchSetSender cm =
             new ReplacePatchSetSender(server, result.change);
         cm.setFrom(me);
         cm.setPatchSet(ps, result.info);
         cm.setChangeMessage(result.msg);
         cm.setReviewDb(db);
         cm.addReviewers(reviewerId);
         cm.addExtraCC(ccId);
         cm.addReviewers(oldReviewers);
         cm.addExtraCC(oldCC);
         cm.send();
       } catch (EmailException e) {
         log.error("Cannot send email for new patch set " + ps.getId(), e);
       }
     }
     sendMergedEmail(result);
   }
 
   private void insertDummyChangeApproval(final ReplaceResult result,
       final Account.Id forAccount, final ApprovalCategory.Id catId,
       final ReviewDb db, final Transaction txn) throws OrmException {
     final ChangeApproval ca =
         new ChangeApproval(new ChangeApproval.Key(result.change.getId(),
             forAccount, catId), (short) 0);
     ca.cache(result.change);
     db.changeApprovals().insert(Collections.singleton(ca), txn);
   }
 
   private Ref findMergedInto(final String first, final RevCommit commit) {
     try {
       final Map<String, Ref> all = repo.getAllRefs();
       Ref firstRef = all.get(first);
       if (firstRef != null && isMergedInto(commit, firstRef)) {
         return firstRef;
       }
       for (Ref ref : all.values()) {
         if (isHead(ref)) {
           if (isMergedInto(commit, ref)) {
             return ref;
           }
         }
       }
       return null;
     } catch (IOException e) {
       log.warn("Can't check for already submitted change", e);
       return null;
     }
   }
 
   private boolean isMergedInto(final RevCommit commit, final Ref ref)
       throws IOException {
     final RevWalk rw = rp.getRevWalk();
     return rw.isMergedInto(commit, rw.parseCommit(ref.getObjectId()));
   }
 
   private static class ReplaceResult {
     Change change;
     PatchSet patchSet;
     PatchSetInfo info;
     ChangeMessage msg;
     String mergedIntoRef;
     List<ChangeApproval> approvals;
   }
 
   private boolean validCommitter(final ReceiveCommand cmd, final RevCommit c) {
     // Require that committer matches the uploader.
     final PersonIdent committer = c.getCommitterIdent();
     final String email = committer.getEmailAddress();
     if (myEmails.contains(email)) {
       return true;
     } else {
       reject(cmd, "invalid committer " + email);
       return false;
     }
   }
 
   private void insertBranchEntity(final ReceiveCommand c) {
     try {
       final Branch.NameKey nameKey =
           new Branch.NameKey(proj.getNameKey(), c.getRefName());
       final Branch.Id idKey = new Branch.Id(db.nextBranchId());
       final Branch b = new Branch(nameKey, idKey);
       db.branches().insert(Collections.singleton(b));
     } catch (OrmException e) {
       final String msg = "database failure creating " + c.getRefName();
       log.error(msg, e);
 
       try {
         err.write(("remote error: " + msg + "\n").getBytes("UTF-8"));
         err.flush();
       } catch (IOException e2) {
         // Ignore errors writing to the client
       }
     }
   }
 
   private void deleteBranchEntity(final ReceiveCommand c) {
     try {
       final Branch.NameKey nameKey =
           new Branch.NameKey(proj.getNameKey(), c.getRefName());
       final Branch b = db.branches().get(nameKey);
       if (b != null) {
         db.branches().delete(Collections.singleton(b));
       }
     } catch (OrmException e) {
       final String msg = "database failure deleting " + c.getRefName();
       log.error(msg, e);
 
       try {
         err.write(("remote error: " + msg + "\n").getBytes("UTF-8"));
         err.flush();
       } catch (IOException e2) {
         // Ignore errors writing to the client
       }
     }
   }
 
   private void autoCloseChanges(final ReceiveCommand cmd) {
     final RevWalk rw = rp.getRevWalk();
     try {
       rw.reset();
       rw.markStart(rw.parseCommit(cmd.getNewId()));
       if (!ObjectId.zeroId().equals(cmd.getOldId())) {
         rw.markUninteresting(rw.parseCommit(cmd.getOldId()));
       }
 
       final Map<ObjectId, Ref> changes = changeRefsById();
       RevCommit c;
       while ((c = rw.next()) != null) {
         Ref r = changes.get(c.copy());
         if (r != null) {
           closeChange(cmd, PatchSet.Id.fromRef(r.getName()));
         }
       }
     } catch (IOException e) {
       log.error("Can't scan for changes to close", e);
     } catch (OrmException e) {
       log.error("Can't scan for changes to close", e);
     }
   }
 
   private void closeChange(final ReceiveCommand cmd, final PatchSet.Id psi)
       throws OrmException {
     final String refName = cmd.getRefName();
     final Change.Id cid = psi.getParentKey();
     final ReplaceResult result =
         db.run(new OrmRunnable<ReplaceResult, ReviewDb>() {
           @Override
           public ReplaceResult run(ReviewDb db, Transaction txn, boolean retry)
               throws OrmException {
             final Change change = db.changes().get(cid);
             final PatchSet ps = db.patchSets().get(psi);
             final PatchSetInfo psInfo = db.patchSetInfo().get(psi);
             if (change == null || ps == null || psInfo == null) {
               log.warn(proj.getName() + " " + psi + " is missing");
               return null;
             }
 
             if (change.getStatus() == Change.Status.MERGED) {
               // If its already merged, don't make further updates, it
               // might just be moving from an experimental branch into
               // a more stable branch.
               //
               return null;
             }
 
             final ReplaceResult result = new ReplaceResult();
             result.change = change;
             result.patchSet = ps;
             result.info = psInfo;
             result.mergedIntoRef = refName;
             markChangeMergedByPush(db, txn, result);
             return result;
           }
         });
     sendMergedEmail(result);
   }
 
   private Map<ObjectId, Ref> changeRefsById() {
     if (refsById == null) {
       refsById = new HashMap<ObjectId, Ref>();
       for (final Ref r : repo.getAllRefs().values()) {
         if (PatchSet.isRef(r.getName())) {
           refsById.put(r.getObjectId(), r);
         }
       }
     }
     return refsById;
   }
 
   private void markChangeMergedByPush(final ReviewDb db, final Transaction txn,
       final ReplaceResult result) throws OrmException {
     final Change change = result.change;
     final String mergedIntoRef = result.mergedIntoRef;
 
     change.setCurrentPatchSet(result.info);
     change.setStatus(Change.Status.MERGED);
     ChangeUtil.updated(change);
 
     if (result.approvals == null) {
       result.approvals = db.changeApprovals().byChange(change.getId()).toList();
     }
     for (ChangeApproval a : result.approvals) {
       a.cache(change);
     }
 
     final StringBuilder msgBuf = new StringBuilder();
     msgBuf.append("Change has been successfully pushed");
     if (!mergedIntoRef.equals(change.getDest().get())) {
       msgBuf.append(" into ");
       if (mergedIntoRef.startsWith(Constants.R_HEADS)) {
         msgBuf.append("branch ");
         msgBuf.append(repo.shortenRefName(mergedIntoRef));
       } else {
         msgBuf.append(mergedIntoRef);
       }
     }
     msgBuf.append(".");
     final ChangeMessage msg =
         new ChangeMessage(new ChangeMessage.Key(change.getId(), ChangeUtil
             .messageUUID(db)), getAccountId());
     msg.setMessage(msgBuf.toString());
 
     db.changeApprovals().update(result.approvals, txn);
     db.changeMessages().insert(Collections.singleton(msg), txn);
     db.changes().update(Collections.singleton(change), txn);
   }
 
   private void sendMergedEmail(final ReplaceResult result) {
     if (result != null && result.mergedIntoRef != null) {
       try {
         final MergedSender cm = new MergedSender(server, result.change);
         cm.setFrom(getAccountId());
         cm.setReviewDb(db);
         cm.setPatchSet(result.patchSet, result.info);
         cm.setDest(new Branch.NameKey(proj.getNameKey(), result.mergedIntoRef));
         cm.send();
       } catch (EmailException e) {
         final PatchSet.Id psi = result.patchSet.getId();
         log.error("Cannot send email for submitted patch set " + psi, e);
       }
     }
   }
 
   private static void reject(final ReceiveCommand cmd) {
     reject(cmd, "prohibited by Gerrit");
   }
 
   private static void reject(final ReceiveCommand cmd, final String why) {
     cmd.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON, why);
   }
 
   private static boolean isTag(final ReceiveCommand cmd) {
     return cmd.getRefName().startsWith(Constants.R_TAGS);
   }
 
   private static boolean isHead(final Ref ref) {
     return ref.getName().startsWith(Constants.R_HEADS);
   }
 
   private static boolean isHead(final ReceiveCommand cmd) {
     return cmd.getRefName().startsWith(Constants.R_HEADS);
   }
 }
