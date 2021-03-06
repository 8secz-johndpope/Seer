 //
 // Copyright (c) 2011 Linkeos.
 //
 // This file is part of Elveos.org.
 // Elveos.org is free software: you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the
 // Free Software Foundation, either version 3 of the License, or (at your
 // option) any later version.
 //
 // Elveos.org is distributed in the hope that it will be useful, but WITHOUT
 // ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 // FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 // more details.
 // You should have received a copy of the GNU General Public License along
 // with Elveos.org. If not, see http://www.gnu.org/licenses/.
 //
 package com.bloatit.model;
 
 import java.util.Date;
 import java.util.EnumSet;
 import java.util.Locale;
 import java.util.Set;
 
 import org.apache.commons.codec.digest.DigestUtils;
 import org.apache.commons.lang.RandomStringUtils;
 
 import com.bloatit.data.DaoExternalAccount;
 import com.bloatit.data.DaoExternalServiceMembership;
 import com.bloatit.data.DaoExternalServiceMembership.RightLevel;
 import com.bloatit.data.DaoFileMetadata;
 import com.bloatit.data.DaoFollowActor;
 import com.bloatit.data.DaoFollowFeature;
 import com.bloatit.data.DaoFollowSoftware;
 import com.bloatit.data.DaoInternalAccount;
 import com.bloatit.data.DaoJoinTeamInvitation;
 import com.bloatit.data.DaoJoinTeamInvitation.State;
 import com.bloatit.data.DaoMember;
 import com.bloatit.data.DaoMember.EmailStrategy;
 import com.bloatit.data.DaoMember.Role;
 import com.bloatit.data.DaoTeamRight.UserTeamRight;
 import com.bloatit.data.DaoUserContent;
 import com.bloatit.framework.exceptions.lowlevel.MalformedArgumentException;
 import com.bloatit.framework.exceptions.lowlevel.NonOptionalParameterException;
 import com.bloatit.framework.utils.Hash;
 import com.bloatit.framework.utils.PageIterable;
 import com.bloatit.framework.utils.datetime.DateUtils;
 import com.bloatit.framework.webprocessor.context.User;
 import com.bloatit.model.feature.FeatureList;
 import com.bloatit.model.feature.FeatureManager;
 import com.bloatit.model.lists.CommentList;
 import com.bloatit.model.lists.ContributionList;
 import com.bloatit.model.lists.JoinTeamInvitationList;
 import com.bloatit.model.lists.KudosList;
 import com.bloatit.model.lists.ListBinder;
 import com.bloatit.model.lists.MilestoneList;
 import com.bloatit.model.lists.MoneyWithdrawalList;
 import com.bloatit.model.lists.OfferList;
 import com.bloatit.model.lists.TeamList;
 import com.bloatit.model.lists.TranslationList;
 import com.bloatit.model.lists.UserContentList;
 import com.bloatit.model.right.Action;
 import com.bloatit.model.right.AuthToken;
 import com.bloatit.model.right.RgtMember;
 import com.bloatit.model.right.RightManager;
 import com.bloatit.model.right.RightManager.Private;
 import com.bloatit.model.right.UnauthorizedOperationException;
 import com.bloatit.model.right.UnauthorizedOperationException.SpecialCode;
 import com.bloatit.model.right.UnauthorizedPrivateAccessException;
 import com.bloatit.model.right.UnauthorizedPublicAccessException;
 import com.bloatit.model.visitor.ModelClassVisitor;
 
 public final class Member extends Actor<DaoMember> implements User {
 
     // /////////////////////////////////////////////////////////////////////////////////////////
     // CONSTRUCTION
     // /////////////////////////////////////////////////////////////////////////////////////////
 
     private static final int PASSWORD_SALT_LENGTH = 50;
     private static final String RESET_SALT = "GSQISUDHOI1232193IOSDJHOQIOOISQJD";
     private static final String ACTIVATE_SALT = "1Z9UI901IE09II90I31JD091J09DJ01KPO";
 
     private static final class MyCreator extends Creator<DaoMember, Member> {
         @SuppressWarnings("synthetic-access")
         @Override
         public Member doCreate(final DaoMember dao) {
             return new Member(dao);
         }
     }
 
     /**
      * Create a new member using its Dao version.
      *
      * @param dao a DaoMember
      * @return the new member or null if dao is null.
      */
     @SuppressWarnings("synthetic-access")
     public static Member create(final DaoMember dao) {
         return new MyCreator().create(dao);
     }
 
     /**
      * Create a new DaoActor. Initialize the creation date to now. Create a new
      * {@link DaoInternalAccount} and a new {@link DaoExternalAccount}.
      *
      * @param login is the login or name of this actor. It must be non null,
      *            unique, longer than 2 chars and do not contains space chars
      *            ("[^\\p{Space}]+").
      * @throws NonOptionalParameterException if login or mail is null.
      * @throws MalformedArgumentException if the login is to small or contain
      *             space chars.
      */
     private static DaoMember createDaoMember(final String login, final String password, final String email, final Locale locale) {
         final String salt = RandomStringUtils.randomAscii(PASSWORD_SALT_LENGTH);
         final String passwd = Hash.calculateHash(password, salt);
         Reporting.reporter.reportMemberCreation(login);
         return DaoMember.createAndPersist(login, passwd, salt, email, locale);
     }
 
     public Member(final String login, final String password, final String email, final Locale locale) {
         super(createDaoMember(login, password, email, locale));
 
     }
 
     public Member(final String login, final String password, final String email, final String fullname, final Locale locale) {
         this(login, password, email, locale);
         getDao().setFullname(fullname);
     }
 
     private Member(final DaoMember dao) {
         super(dao);
     }
 
     // /////////////////////////////////////////////////////////////////////////////////////////
     // Accessors
     // /////////////////////////////////////////////////////////////////////////////////////////
 
     /**
      * To invite a member into a team you have to have the WRITE right on the
      * "invite" property.
      *
      * @param member The member you want to invite
      * @param team The team in which you invite a member.
      * @throws UnauthorizedOperationException
      */
     public void sendInvitation(final Member member, final Team team) throws UnauthorizedOperationException {
         if (!hasInviteTeamRight(team)) {
             throw new UnauthorizedOperationException(SpecialCode.INVITATION_SEND_NO_RIGHT);
         }
         DaoJoinTeamInvitation.createAndPersist(getDao(), member.getDao(), team.getDao());
     }
 
     /**
      * To accept an invitation you must have the DELETED right on the "invite"
      * property. If the invitation is not in PENDING state then nothing is done,
      * and <i>false</i> is returned.
      *
      * @param invitation the authenticate member must be receiver of the
      *            invitation.
      * @return true if the invitation is accepted, false if there is an error.
      * @throws UnauthorizedOperationException
      */
     public boolean acceptInvitation(final JoinTeamInvitation invitation) throws UnauthorizedOperationException {
         if (!invitation.getReceiver().getId().equals(AuthToken.getMember().getId())) {
             throw new UnauthorizedOperationException(SpecialCode.INVITATION_RECIEVER_MISMATCH);
         }
 
         // Accept the invitation
         if (invitation.accept()) {
             // discard all other invitation to join the same team
             final Team team = invitation.getTeam();
             final PageIterable<JoinTeamInvitation> receivedInvitation = this.getReceivedInvitation(State.PENDING, team);
             for (final JoinTeamInvitation invite : receivedInvitation) {
                 invite.discard();
             }
             return true;
         }
         return false;
     }
 
     /**
      * To refuse an invitation you must have the DELETED right on the "invite"
      * property. If the invitation is not in PENDING state then nothing is done.
      *
      * @param invitation the authenticate member must be receiver of the
      *            invitation.
      * @throws UnauthorizedOperationException
      */
     public void refuseInvitation(final JoinTeamInvitation invitation) throws UnauthorizedOperationException {
         if (!invitation.getReceiver().getId().equals(AuthToken.getMember().getId())) {
             throw new UnauthorizedOperationException(SpecialCode.INVITATION_RECIEVER_MISMATCH);
         }
         invitation.refuse();
     }
 
     /**
      * To remove this member from a team you have to have the DELETED right on
      * the "team" property. If the member is not in the "team", nothing is done.
      * (Although it should be considered as an error and will be logged)
      *
      * @param aTeam is the team from which the user will be removed.
      * @throws UnauthorizedOperationException
      */
     public void kickFromTeam(final Team aTeam, final Member actor) throws UnauthorizedOperationException {
         if (!canBeKickFromTeam(aTeam, actor)) {
             throw new UnauthorizedOperationException(SpecialCode.TEAM_PROMOTE_RIGHT_MISSING);
         }
         getDao().removeFromTeam(aTeam.getDao());
     }
 
     /**
      * To add a user into a public team, you have to make sure you can access
      * the teams with the {@link Action#WRITE} action.
      *
      * @param team must be a public team.
      * @throws UnauthorizedOperationException if the authenticated member do not
      *             have the right to use this methods.
      */
     public void addToPublicTeam(final Team team) throws UnauthorizedOperationException {
         if (!team.isPublic()) {
             throw new UnauthorizedOperationException(SpecialCode.TEAM_NOT_PUBLIC);
         }
         getDao().addToTeam(team.getDao());
     }
 
     /**
      * Adds a user to a team without checking if the team is Public or not
      *
      * @param team the team to which the user will be added
      */
     void addToTeamUnprotected(final Team team) {
         getDao().addToTeam(team.getDao());
     }
 
     // User data modification
 
     /**
      * Updates user password with right checking
      *
      * @param password the new password
      * @throws UnauthorizedPrivateAccessException when the logged user cannot
      *             modify the password
      */
     public void setPassword(final String password) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Password(), Action.WRITE);
         getDao().setPassword(Hash.calculateHash(password, getDao().getSalt()));
     }
 
     /**
      * Checks if an inputed password matches the user password
      *
      * @param password the password to match
      * @return <i>true</i> if the inputed password matches the password in the
      *         database, <i>false</i> otherwise
      */
     public boolean checkPassword(final String password) {
         final String digestedPassword = Hash.calculateHash(password, getDao().getSalt());
         return getDao().passwordEquals(digestedPassword);
     }
 
     public void setFullname(final String fullname) throws UnauthorizedPublicAccessException {
         tryAccess(new RgtMember.FullName(), Action.WRITE);
         getDao().setFullname(fullname);
     }
 
     public void setAvatar(final FileMetadata fileImage) throws UnauthorizedPublicAccessException {
         tryAccess(new RgtMember.Avatar(), Action.WRITE);
         if (fileImage == null) {
             getDao().setAvatar(null);
         } else {
             getDao().setAvatar(fileImage.getDao());
         }
     }
 
     public void setDescription(final String userDescription) throws UnauthorizedPublicAccessException {
         tryAccess(new RgtMember.UserDescription(), Action.WRITE);
         getDao().setDescription(userDescription);
     }
 
     private String libravatar(final String email) {
         final String digest = DigestUtils.md5Hex(email.toLowerCase());
         // return "http://cdn.libravatar.org/avatar/" + digest +
         // "?d=http://elveos.org/resources/commons/img/none.png&s=64";
         return digest;
     }
 
     public void setEmail(final String email) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Email(), Action.WRITE);
         getDao().setEmail(email);
     }
 
     public void setLocal(final Locale loacle) throws UnauthorizedPublicAccessException {
         tryAccess(new RgtMember.Locale(), Action.WRITE);
         getDao().setLocale(loacle);
     }
 
     protected void setRole(final Role role) {
         getDao().setRole(role);
     }
 
     // Activation
     public boolean isActive() {
         return getDao().getActivationState() == ActivationState.ACTIVE;
     }
 
     public boolean activate(final String activationKey) {
         if (getDao().getActivationState() != ActivationState.VALIDATING) {
             return false;
         }
         if (getActivationKey().equals(activationKey) || getResetKey().equals(activationKey)) {
             getDao().setActivationState(ActivationState.ACTIVE);
             addToKarma(ModelConfiguration.getKarmaActivationAmount());
             return true;
         }
         if (getResetKey().equals(activationKey)) {
             getDao().setActivationState(ActivationState.ACTIVE);
             addToKarma(ModelConfiguration.getKarmaActivationAmount());
             return true;
         }
 
         return false;
     }
 
     public boolean hasEmailToActivate() {
         return getDao().getEmailToActivate() != null;
     }
 
     public boolean activateEmail(final String activationKey) {
         if (!hasEmailToActivate()) {
             return false;
         }
 
         if (getEmailActivationKey().equals(activationKey)) {
             getDao().setEmail(getDao().getEmailToActivate());
             getDao().setEmailToActivate(null);
             return true;
         }
         return false;
 
     }
 
     public void setEmailToActivate(final String email) {
         getDao().setEmailToActivate(email);
     }
 
     public void acceptNewsLetter(final boolean newsletter) {
         getDao().acceptNewsLetter(newsletter);
     }
 
     public FollowFeature followOrGetFeature(final Feature f) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         return FollowFeature.create(getDao().followOrGetFeature(((FeatureImplementation) f).getDao()));
     }
 
     public boolean isFollowing(final Feature feature) {
         return getDao().isFollowing(((FeatureImplementation) feature).getDao());
     }
 
     public boolean isFollowing(final Software software) {
         return getDao().isFollowing(software.getDao());
     }
 
     public boolean isFollowing(final Actor<?> actor) {
         return getDao().isFollowing(actor.getDao());
     }
 
     public void unfollowFeature(final Feature f) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         final FollowFeature followFeature = followOrGetFeature(f);
         followFeature.getDao().unfollow();
     }
 
     public FollowSoftware followOrGetSoftware(final Software s) throws UnauthorizedOperationException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         if (!isFollowing(s)) {
             // Follow all features of the software
             for (final Feature feature : s.getFeatures()) {
                 final FollowFeature followFeature = followOrGetFeature(feature);
                 followFeature.setFeatureComment(true);
                 followFeature.setBugComment(true);
             }
         }
         return FollowSoftware.create(getDao().followOrGetSoftware(s.getDao()));
     }
 
     public void unfollowSoftware(final Software s) throws UnauthorizedOperationException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         final FollowSoftware followSoftware = followOrGetSoftware(s);
         followSoftware.getDao().unfollow();
 
         // Unfollow all features of the software
         for (final FollowFeature followFeature : getFollowedFeatures()) {
             if (s.equals(followFeature.getFollowed().getSoftware())) {
                 unfollowFeature(followFeature.getFollowed());
             }
         }
     }
 
     public FollowActor followOrGetActor(final Actor<?> f) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         return FollowActor.create(getDao().followOrGetActor(f.getDao()));
     }
 
     public void unfollowActor(final Actor<?> a) throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Follow(), Action.WRITE);
         final FollowActor followActor = followOrGetActor(a);
         followActor.getDao().unfollow();
     }
 
     public void setLastWatchedEvents(final Date lastWatchedEvents) {
         getDao().setLastWatchedEvents(lastWatchedEvents);
     }
 
     public void setEmailStrategy(final EmailStrategy emailStrategy) throws UnauthorizedPrivateAccessException {
         tryAccess(new Private(), Action.WRITE);
         getDao().setEmailStrategy(emailStrategy);
     }
 
     public void setGlobalFollow(final boolean globalFollow) throws UnauthorizedOperationException {
         tryAccess(new Private(), Action.WRITE);
 
         if (globalFollow && !isGlobalFollow()) {
             // Follow all
             for (final Feature feature : FeatureManager.getAllByCreationDate()) {
                 final FollowFeature followFeature = followOrGetFeature(feature);
                 followFeature.setFeatureComment(true);
                 followFeature.setBugComment(true);
             }
         } else if (!globalFollow && isGlobalFollow()) {
             for (final FollowFeature followFeature : getFollowedFeatures()) {
                 Software software = followFeature.getFollowed().getSoftware();
                 if (software == null || !isFollowing(software)) {
                     unfollowFeature(followFeature.getFollowed());
                 }
             }
         }
         getDao().setGlobalFollow(globalFollow);
     }
 
     public void setGlobalFollowWithMail(final boolean globalFollow) throws UnauthorizedOperationException {
         tryAccess(new Private(), Action.WRITE);
 
         for (final FollowFeature followFeature : getFollowedFeatures()) {
             Software software = followFeature.getFollowed().getSoftware();
             if (software == null || !isFollowing(software)) {
                 followFeature.setMail(globalFollow);
             }
         }
 
         getDao().setGlobalFollowWithMail(globalFollow);
     }
 
     // /////////////////////////////////////////////////////////////////////////////////////////
     // Getters
     // /////////////////////////////////////////////////////////////////////////////////////////
 
     public Date getLastWatchedEvents() {
         final Date lastWatchedEvents = getDao().getLastWatchedEvents();
         if (lastWatchedEvents == null) {
             return DateUtils.dawnOfTime();
         } else {
             return lastWatchedEvents;
         }
     }
 
     /*
      * (non-Javadoc)
      * @see com.bloatit.model.User#getActivationState()
      */
     @Override
     public ActivationState getActivationState() {
         return getDao().getActivationState();
     }
 
     public String getDescription() {
         return getDao().getDescription();
     }
 
     public EmailStrategy getEmailStrategy() throws UnauthorizedPrivateAccessException {
         tryAccess(new Private(), Action.READ);
         return getDao().getEmailStrategy();
     }
 
     public FollowFeature getFollowFeature(final Feature feature) {
         return FollowFeature.create(getDao().getFollowFeature(((FeatureImplementation) feature).getDao()));
     }
 
     public boolean isGlobalFollow() throws UnauthorizedPrivateAccessException {
         tryAccess(new Private(), Action.READ);
         return getDao().isGlobalFollow();
     }
 
     public boolean isGlobalFollowWithMail() throws UnauthorizedPrivateAccessException {
         tryAccess(new Private(), Action.READ);
         return getDao().isGlobalFollowWithMail();
     }
 
     public String getActivationKey() {
         final DaoMember m = getDao();
         final String digest = "" + m.getId() + m.getEmail() + m.getFullname() + m.getPassword() + m.getSalt() + ACTIVATE_SALT;
         return DigestUtils.sha256Hex(digest);
     }
 
     public String getEmailActivationKey() {
         final DaoMember m = getDao();
         final String digest = "" + m.getId() + m.getEmail() + m.getEmailToActivate() + m.getFullname() + m.getPassword() + m.getSalt()
                 + ACTIVATE_SALT;
         return DigestUtils.sha256Hex(digest);
     }
 
     /**
      * @param state can be PENDING, ACCEPTED or REFUSED
      * @return all the received invitation with the specified state.
      */
     public PageIterable<JoinTeamInvitation> getReceivedInvitation(final State state) {
         return new JoinTeamInvitationList(getDao().getReceivedInvitation(state));
     }
 
     /**
      * @param state can be PENDING, ACCEPTED or REFUSED
      * @param team the team invited to join
      * @return all the received invitation with the specified state and team
      */
     private PageIterable<JoinTeamInvitation> getReceivedInvitation(final State state, final Team team) {
         return new JoinTeamInvitationList(getDao().getReceivedInvitation(state, team.getDao()));
     }
 
     /**
      * @param state can be PENDING, ACCEPTED or REFUSED
      * @return all the sent invitation with the specified state.
      */
     public PageIterable<JoinTeamInvitation> getSentInvitation(final State state) {
         return new JoinTeamInvitationList(getDao().getSentInvitation(state));
     }
 
     /**
      * To get the teams you have the have the READ right on the "team" property.
      *
      * @return all the team in which this member is.
      * @throws UnauthorizedOperationException
      */
     public PageIterable<Team> getTeams() throws UnauthorizedOperationException {
         tryAccess(new RightManager.Public(), Action.READ);
         return new TeamList(getDao().getTeams());
     }
 
     public int getKarma() {
         return getDao().getKarma();
     }
 
     public PageIterable<Milestone> getMilestoneToInvoice() {
         return new MilestoneList(getDao().getMilestoneToInvoice());
     }
 
     public PageIterable<Milestone> getMilestoneInvoiced() {
         return new MilestoneList(getDao().getMilestoneInvoiced());
     }
 
     public PageIterable<Milestone> getMilestones() {
         return new MilestoneList(getDao().getMilestones());
     }
 
     // TODO make right managements
     public PageIterable<UserContent<? extends DaoUserContent>> getHistory() {
         return new UserContentList(getDao().getHistory());
     }
 
     private static final float INFLUENCE_MULTIPLICATOR = 2;
     private static final float INFLUENCE_GELIFICATOR = 20;
     // Minimum karma
     private static final float INFLUENCE_BASE = 1;
 
     protected int calculateInfluence() {
         final int karma = getDao().getKarma();
         if (karma >= 0) {
             return (int) (Math.log10((INFLUENCE_GELIFICATOR + karma) / INFLUENCE_GELIFICATOR) * INFLUENCE_MULTIPLICATOR + INFLUENCE_BASE);
         }
         return 0;
     }
 
     @Override
     public String getDisplayName() {
         if (getDao().getFullname() != null && !getDao().getFullname().isEmpty()) {
             return getFullname();
         }
         return getLogin();
     }
 
     public String getEmail() throws UnauthorizedOperationException {
         tryAccess(new RgtMember.Email(), Action.READ);
         return getEmailUnprotected();
     }
 
     public String getEmailToActivate() throws UnauthorizedOperationException {
         tryAccess(new RgtMember.Email(), Action.READ);
         return getDao().getEmailToActivate();
     }
 
     // TODO: Create a send notification / mail
     public String getEmailUnprotected() {
         return getDao().getEmail();
     }
 
     public boolean getNewsletterAccept() {
         return getDao().getNewsletterAccept();
     }
 
     // no right management: this is public data
     public Locale getLocaleUnprotected() {
         return getDao().getLocale();
     }
 
     // no right management: this is public data
     @Override
     public Locale getLocale() {
         return getDao().getLocale();
     }
 
     // no right management: this is public data
     public String getFullname() {
         return getDao().getFullname();
     }
 
     // no right management: this is public data
     @Override
     public Image getAvatar() {
         final DaoFileMetadata avatar = getDao().getAvatar();
         if (avatar != null) {
             return new Image(FileMetadata.create(avatar));
         }
         String libravatar = null;
         libravatar = libravatar(getDao().getEmail().toLowerCase().trim());
         if (libravatar == null) {
             return null;
         }
         return new Image(libravatar);
     }
 
     public long getInvitationCount() {
         // TODO right management
         return getDao().getInvitationCount();
     }
 
     // no right management: this is public data
     public PageIterable<Feature> getFeatures(final boolean asMemberOnly) {
         return new FeatureList(getDao().getFeatures(asMemberOnly));
     }
 
     public PageIterable<Kudos> getKudos() throws UnauthorizedPrivateAccessException {
         tryAccess(new RgtMember.Kudos(), Action.READ);
         return new KudosList(getDao().getKudos());
     }
 
     @Override
     public PageIterable<Contribution> doGetContributions() {
         return getContributions(true);
     }
 
     @Override
     public PageIterable<MoneyWithdrawal> doGetMoneyWithdrawals() throws UnauthorizedOperationException {
         return new MoneyWithdrawalList(getDao().getMoneyWithdrawals());
     }
 
     // no right management: this is public data
     public PageIterable<Contribution> getContributions(final boolean asMemberOnly) {
         return new ContributionList(getDao().getContributions(asMemberOnly));
     }
 
     // no right management: this is public data
     public PageIterable<Comment> getComments(final boolean asMemberOnly) {
         return new CommentList(getDao().getComments(asMemberOnly));
     }
 
     // no right management: this is public data
     public PageIterable<Offer> getOffers(final boolean asMemberOnly) {
         return new OfferList(getDao().getOffers(asMemberOnly));
     }
 
     // no right management: this is public data
     public PageIterable<Translation> getTranslations(final boolean asMemberOnly) {
         return new TranslationList(getDao().getTranslations(asMemberOnly));
     }
 
     public void addToKarma(final int value) {
         getDao().addToKarma(value);
     }
 
     // no right management: this is public data
     public Role getRole() {
         return getDao().getRole();
     }
 
     // TODO make a more integrated method.
     public String getResetKey() {
         final DaoMember m = getDao();
         final String digest = "" + m.getId() + m.getEmail() + m.getFullname() + m.getPassword() + m.getSalt() + RESET_SALT;
         return DigestUtils.sha256Hex(digest);
     }
 
     public PageIterable<ExternalServiceMembership> getExternalServices() {
         return new ListBinder<ExternalServiceMembership, DaoExternalServiceMembership>(getDao().getAuthorizedExternalServices());
     }
 
     public PageIterable<FollowFeature> getFollowedFeatures() {
         return new ListBinder<FollowFeature, DaoFollowFeature>(getDao().getFollowedFeatures());
     }
 
     public PageIterable<FollowActor> getFollowedActors() {
         return new ListBinder<FollowActor, DaoFollowActor>(getDao().getFollowedActors());
     }
 
     public PageIterable<FollowSoftware> getFollowedSoftware() {
         return new ListBinder<FollowSoftware, DaoFollowSoftware>(getDao().getFollowedSoftware());
     }
 
     public void addAuthorizedExternalService(final String clientId, final String token, final EnumSet<RightLevel> rights) {
         getDao().addAuthorizedExternalService(clientId, token, rights);
     }
 
     // /////////////////////////////////////////////////////////////////////////////////////////
     // Accessors
     // /////////////////////////////////////////////////////////////////////////////////////////
 
     /**
      * Tells if the authenticated user can access the <i>Avatar</i> property.
      *
      * @param action the type of access you want to do on the <i>Avatar</i>
      *            property.
      * @return true if you can access the <i>Avatar</i> property.
      */
     public final boolean canAccessAvatar(final Action action) {
         return canAccess(new RgtMember.Avatar(), action);
     }
 
     public boolean canGetTeams() {
         return canAccess(new RgtMember.Teams(), Action.READ);
     }
 
     public boolean canSetPassword() {
         return canAccess(new RgtMember.Password(), Action.WRITE);
     }
 
     public boolean canAccessEmail(final Action action) {
         return canAccess(new RgtMember.Email(), action);
     }
 
     /**
      * Indicates whether the logged member can access <code>ANY</code>of the
      * user's information
      *
      * @param action the type of action
      */
     public boolean canAccessUserInformations(final Action action) {
         return canAccess(new RgtMember.UserInformations(), action);
     }
 
     public boolean canBeKickFromTeam(final Team aTeam, final Member actor) {
         if (actor == null) {
             return false;
         }
         if (this.equals(actor)) {
             if (hasPromoteTeamRight(aTeam)) {
                 return false;
             }
             return true;
         }
         if (!actor.hasPromoteTeamRight(aTeam)) {
             return false;
         }
         return true;
     }
 
     // /////////////////////////////////////////////////////////////////////////////////////////
     // Team Rights
     // /////////////////////////////////////////////////////////////////////////////////////////
 
     public boolean hasTeamRight(final Team aTeam, final UserTeamRight aRight) {
         if (getTeamRights(aTeam) == null) {
             return false;
         }
         return getTeamRights(aTeam).contains(aRight);
     }
 
     public Set<UserTeamRight> getTeamRights(final Team g) {
         return getDao().getTeamRights(g.getDao());
     }
 
     public boolean hasConsultTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.CONSULT);
     }
 
     public boolean hasTalkTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.TALK);
     }
 
     public boolean hasInviteTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.INVITE);
     }
 
     public boolean hasModifyTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.MODIFY);
     }
 
     public boolean hasPromoteTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.PROMOTE);
     }
 
     public boolean hasBankTeamRight(final Team aTeam) {
         return hasTeamRight(aTeam, UserTeamRight.BANK);
     }
 
     public boolean isInTeam(final Team team) {
         return isInTeamUnprotected(team);
     }
 
     protected boolean isInTeamUnprotected(final Team team) {
         return getDao().isInTeam(team.getDao());
     }
 
     @Override
     public <ReturnType> ReturnType accept(final ModelClassVisitor<ReturnType> visitor) {
         return visitor.visit(this);
     }
 
 }
