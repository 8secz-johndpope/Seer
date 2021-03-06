 /*
  * DocDoku, Professional Open Source
  * Copyright 2006 - 2013 DocDoku SARL
  *
  * This file is part of DocDokuPLM.
  *
  * DocDokuPLM is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * DocDokuPLM is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.docdoku.server;
 
 import com.docdoku.core.common.*;
 import com.docdoku.core.document.*;
 import com.docdoku.core.meta.InstanceAttribute;
 import com.docdoku.core.meta.InstanceAttributeTemplate;
 import com.docdoku.core.security.ACL;
 import com.docdoku.core.security.ACLUserEntry;
 import com.docdoku.core.security.ACLUserGroupEntry;
 import com.docdoku.core.services.*;
 import com.docdoku.core.sharing.SharedDocument;
 import com.docdoku.core.sharing.SharedEntityKey;
 import com.docdoku.core.util.NamingConvention;
 import com.docdoku.core.util.Tools;
 import com.docdoku.core.workflow.*;
 import com.docdoku.server.dao.*;
 
 import javax.annotation.Resource;
 import javax.annotation.security.DeclareRoles;
 import javax.annotation.security.RolesAllowed;
 import javax.ejb.EJB;
 import javax.ejb.Local;
 import javax.ejb.SessionContext;
 import javax.ejb.Stateless;
 import javax.jws.WebService;
 import javax.persistence.EntityManager;
 import javax.persistence.NoResultException;
 import javax.persistence.PersistenceContext;
 import java.io.InputStream;
 import java.text.ParseException;
 import java.util.*;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 
 
 @DeclareRoles({"users","admin"})
 @Local(IDocumentManagerLocal.class)
 @Stateless(name = "DocumentManagerBean")
 @WebService(endpointInterface = "com.docdoku.core.services.IDocumentManagerWS")
 public class DocumentManagerBean implements IDocumentManagerWS, IDocumentManagerLocal {
 
     @PersistenceContext
     private EntityManager em;
 
     @Resource
     private SessionContext ctx;
 
     @EJB
     private IUserManagerLocal userManager;
 
     @EJB
     private IMailerLocal mailer;
 
     @EJB
     private IndexerBean indexer;
 
     @EJB
     private IndexSearcherBean indexSearcher;
 
     @EJB
     private IDataManagerLocal dataManager;
 
     private final static Logger LOGGER = Logger.getLogger(DocumentManagerBean.class.getName());
 
     @RolesAllowed("users")
     @Override
     public BinaryResource saveFileInTemplate(DocumentMasterTemplateKey pDocMTemplateKey, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException, AccessRightException {
         User user = userManager.checkWorkspaceWriteAccess(pDocMTemplateKey.getWorkspaceId());
 
         if (!NamingConvention.correct(pName)) {
             throw new NotAllowedException(Locale.getDefault(), "NotAllowedException9");
         }
 
         DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(em);
         DocumentMasterTemplate template = templateDAO.loadDocMTemplate(pDocMTemplateKey);
         BinaryResource binaryResource = null;
         String fullName = template.getWorkspaceId() + "/document-templates/" + template.getId() + "/" + pName;
 
         for (BinaryResource bin : template.getAttachedFiles()) {
             if (bin.getFullName().equals(fullName)) {
                 binaryResource = bin;
                 break;
             }
         }
 
         if (binaryResource == null) {
             binaryResource = new BinaryResource(fullName, pSize, new Date());
             new BinaryResourceDAO(em).createBinaryResource(binaryResource);
             template.addFile(binaryResource);
         } else {
             binaryResource.setContentLength(pSize);
             binaryResource.setLastModified(new Date());
         }
         return binaryResource;
     }
 
     @RolesAllowed("users")
     @Override
     public BinaryResource saveFileInDocument(DocumentIterationKey pDocPK, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException, AccessRightException {
 
         User user = checkDocumentMasterWriteAccess(new DocumentMasterKey(pDocPK.getWorkspaceId(),pDocPK.getDocumentMasterId(),pDocPK.getDocumentMasterVersion()));
 
         if (!NamingConvention.correct(pName)) {
             throw new NotAllowedException(Locale.getDefault(), "NotAllowedException9");
         }
 
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(em);
         DocumentMaster docM = docMDAO.loadDocM(new DocumentMasterKey(pDocPK.getWorkspaceId(), pDocPK.getDocumentMasterId(), pDocPK.getDocumentMasterVersion()));
 
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(pDocPK.getWorkspaceId());
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
 
         // check write access on acl
        if(!isAdmin && (docM.getACL() == null || !docM.getACL().hasWriteAccess(user))){

             throw new AccessRightException(new Locale(user.getLanguage()),user);
         }
 
         DocumentIteration document = docM.getIteration(pDocPK.getIteration());
 
         if (docM.isCheckedOut() && docM.getCheckOutUser().equals(user) && docM.getLastIteration().equals(document)) {
             BinaryResource binaryResource = null;
             String fullName = docM.getWorkspaceId() + "/documents/" + docM.getId() + "/" + docM.getVersion() + "/" + document.getIteration() + "/" + pName;
 
             for (BinaryResource bin : document.getAttachedFiles()) {
                 if (bin.getFullName().equals(fullName)) {
                     binaryResource = bin;
                     break;
                 }
             }
             if (binaryResource == null) {
                 binaryResource = new BinaryResource(fullName, pSize, new Date());
                 new BinaryResourceDAO(em).createBinaryResource(binaryResource);
                 document.addFile(binaryResource);
             } else {
                 binaryResource.setContentLength(pSize);
                 binaryResource.setLastModified(new Date());
             }
             return binaryResource;
         } else {
             throw new NotAllowedException(Locale.getDefault(), "NotAllowedException4");
         }
     }
 
     @LogDocument
     @RolesAllowed("users")
     @Override
     public BinaryResource getBinaryResource(String pFullName) throws WorkspaceNotFoundException, NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {
         User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
 
         BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
         BinaryResource binaryResource = binDAO.loadBinaryResource(pFullName);
 
         DocumentIteration document = binDAO.getDocumentOwner(binaryResource);
         if (document != null) {
             DocumentMaster docM = document.getDocumentMaster();
 
             if((docM.getACL() != null && docM.getACL().hasReadAccess(user)) || docM.getACL() == null){
                 String owner = docM.getLocation().getOwner();
 
                 if (((owner != null) && (!owner.equals(user.getLogin()))) || (docM.isCheckedOut() && !docM.getCheckOutUser().equals(user) && docM.getLastIteration().equals(document))) {
                     throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException34");
                 } else {
                     return binaryResource;
                 }
             }else{
                 throw new AccessRightException(new Locale(user.getLanguage()),user);
             }
 
         } else {
             return binaryResource;
         }
     }
 
     @RolesAllowed("users")
     @Override
     public User whoAmI(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         return userManager.checkWorkspaceReadAccess(pWorkspaceId);
     }
 
     @RolesAllowed("users")
     @Override
     public Workspace getWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return user.getWorkspace();
     }
 
     @RolesAllowed("users")
     @Override
     public String [] getFolders(String pCompletePath) throws WorkspaceNotFoundException, FolderNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(Folder.parseWorkspaceId(pCompletePath));
         Folder[] subFolders = new FolderDAO(new Locale(user.getLanguage()), em).getSubFolders(pCompletePath);
         String[] shortNames = new String[subFolders.length];
         int i = 0;
         for (Folder f : subFolders) {
             shortNames[i++] = f.getShortName();
         }
         return shortNames;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster[] findDocumentMastersByFolder(String pCompletePath) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         String workspaceId = Folder.parseWorkspaceId(pCompletePath);
         User user = userManager.checkWorkspaceReadAccess(workspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findDocMsByFolder(pCompletePath);
         ListIterator<DocumentMaster> ite = docMs.listIterator();
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(workspaceId);
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
         while (ite.hasNext()) {
             DocumentMaster docM = ite.next();
             if (!isAdmin && docM.getACL() != null && !docM.getACL().hasReadAccess(user)) {
                 ite.remove();
             }else if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
                 docM = docM.clone();
                 docM.removeLastIteration();
                 ite.set(docM);
             }
         }
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster[] findDocumentMastersByTag(TagKey pKey) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         String workspaceId = pKey.getWorkspaceId();
         User user = userManager.checkWorkspaceReadAccess(workspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findDocMsByTag(new Tag(user.getWorkspace(), pKey.getLabel()));
         ListIterator<DocumentMaster> ite = docMs.listIterator();
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(workspaceId);
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
         while (ite.hasNext()) {
             DocumentMaster docM = ite.next();
             if (!isAdmin && docM.getACL() != null && !docM.getACL().hasReadAccess(user)) {
                 ite.remove();
             }else if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
                 docM = docM.clone();
                 docM.removeLastIteration();
                 ite.set(docM);
             }
         }
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster getDocumentMaster(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException {
         User user = userManager.checkWorkspaceReadAccess(pDocMPK.getWorkspaceId());
         DocumentMaster docM = new DocumentMasterDAO(new Locale(user.getLanguage()), em).loadDocM(pDocMPK);
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
         }
 
         if((docM.getACL() != null && docM.getACL().hasReadAccess(user)) || docM.getACL() == null){
 
             if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
                 docM = docM.clone();
                 docM.removeLastIteration();
             }
             return docM;
 
         }else{
             throw new AccessRightException(new Locale(user.getLanguage()),user);
         }
 
     }
 
     @Override
     public DocumentMaster getPublicDocumentMaster(DocumentMasterKey pDocMPK) throws DocumentMasterNotFoundException {
         DocumentMaster docM = new DocumentMasterDAO(em).loadDocM(pDocMPK);
         if(docM.isPublicShared()){
             return docM;
         }else{
             return null;
         }
     }
 
     @Override
     public DocumentIteration findDocumentIterationByBinaryResource(BinaryResource pBinaryResource) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pBinaryResource.getWorkspaceId());
         DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()),em);
         return  documentMasterDAO.findDocumentIterationByBinaryResource(pBinaryResource);
     }
 
     @RolesAllowed("users")
     @Override
     public void updateDocumentACL(String pWorkspaceId, DocumentMasterKey docKey, Map<String,ACL.Permission> pACLUserEntries, Map<String,ACL.Permission> pACLUserGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentMasterNotFoundException, AccessRightException {
 
         User user = checkDocumentMasterWriteAccess(docKey);
 
         DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = documentMasterDAO.getDocMRef(docKey);
         Workspace wks = new WorkspaceDAO(em).loadWorkspace(pWorkspaceId);
 
         if (docM.getAuthor().getLogin().equals(user.getLogin()) || wks.getAdmin().getLogin().equals(user.getLogin())) {
 
             if (docM.getACL() == null) {
 
                 ACL acl = new ACL();
 
                 if (pACLUserEntries != null) {
                     for (Map.Entry<String, ACL.Permission> entry : pACLUserEntries.entrySet()) {
                         acl.addEntry(em.getReference(User.class,new UserKey(pWorkspaceId,entry.getKey())),entry.getValue());
                     }
                 }
 
                 if (pACLUserGroupEntries != null) {
                     for (Map.Entry<String, ACL.Permission> entry : pACLUserGroupEntries.entrySet()) {
                         acl.addEntry(em.getReference(UserGroup.class,new UserGroupKey(pWorkspaceId,entry.getKey())),entry.getValue());
                     }
                 }
 
                 new ACLDAO(em).createACL(acl);
                 docM.setACL(acl);
 
             }else{
                 if (pACLUserEntries != null) {
                     for (ACLUserEntry entry : docM.getACL().getUserEntries().values()) {
                         ACL.Permission newPermission = pACLUserEntries.get(entry.getPrincipalLogin());
                         if(newPermission != null){
                             entry.setPermission(newPermission);
                         }
                     }
                 }
 
                 if (pACLUserGroupEntries != null) {
                     for (ACLUserGroupEntry entry : docM.getACL().getGroupEntries().values()) {
                         ACL.Permission newPermission = pACLUserGroupEntries.get(entry.getPrincipalId());
                         if(newPermission != null){
                             entry.setPermission(newPermission);
                         }
                     }
                 }
             }
 
         }else {
                 throw new AccessRightException(new Locale(user.getLanguage()), user);
         }
     }
 
 
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster[] getCheckedOutDocumentMasters(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findCheckedOutDocMs(user);
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public Task[] getTasks(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new TaskDAO(new Locale(user.getLanguage()), em).findTasks(user);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterKey[] getIterationChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new SubscriptionDAO(em).getIterationChangeEventSubscriptions(user);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterKey[] getStateChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new SubscriptionDAO(em).getStateChangeEventSubscriptions(user);
     }
 
 
     @RolesAllowed("users")
     @Override
     public boolean isUserStateChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentMaster docM) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new SubscriptionDAO(em).isUserStateChangeEventSubscribedForGivenDocument(user, docM);
     }
 
     @RolesAllowed("users")
     @Override
     public boolean isUserIterationChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentMaster docM) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return  new SubscriptionDAO(em).isUserIterationChangeEventSubscribedForGivenDocument(user, docM);
     }
 
     @Override
     public DocumentMaster[] getDocumentMastersWithAssignedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findDocWithAssignedTasksForGivenUser(pWorkspaceId, assignedUserLogin);
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @Override
     public DocumentMaster[] getDocumentMastersWithReference(String pWorkspaceId, String reference, int maxResults) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findDocMsWithReferenceLike(pWorkspaceId, reference, maxResults);
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public String generateId(String pWorkspaceId, String pDocMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, DocumentMasterTemplateNotFoundException {
 
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         DocumentMasterTemplate template = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(new DocumentMasterTemplateKey(user.getWorkspaceId(), pDocMTemplateId));
 
         String newId = null;
         try {
             String latestId = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findLatestDocMId(pWorkspaceId, template.getDocumentType());
             String inputMask = template.getMask();
             String convertedMask = Tools.convertMask(inputMask);
             newId = Tools.increaseId(latestId, convertedMask);
         } catch (ParseException ex) {
             //may happen when a different mask has been used for the same document type
         } catch (NoResultException ex) {
             //may happen when no document of the specified type has been created
         }
         return newId;
 
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster[] searchDocumentMasters(SearchQuery pQuery) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pQuery.getWorkspaceId());
         //preparing tag filtering
         Set<Tag> tags = null;
         if (pQuery.getTags() != null) {
             Workspace wks = new Workspace();
             wks.setId(pQuery.getWorkspaceId());
             tags = new HashSet<Tag>();
             for (String label : pQuery.getTags()) {
                 tags.add(new Tag(wks, label));
             }
         }
 
         List<DocumentMaster> fetchedDocMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).searchDocumentMasters(pQuery.getWorkspaceId(), pQuery.getDocMId(), pQuery.getTitle(), pQuery.getVersion(), pQuery.getAuthor(), pQuery.getType(), pQuery.getCreationDateFrom(),
                 pQuery.getCreationDateTo(), tags, pQuery.getAttributes() != null ? Arrays.asList(pQuery.getAttributes()) : null);
 
         //preparing fulltext filtering
         Set<DocumentMasterKey> indexedKeys = null;
         if (fetchedDocMs.size() > 0 && pQuery.getContent() != null && !pQuery.getContent().equals("")) {
             indexedKeys = indexSearcher.searchInIndex(pQuery.getWorkspaceId(), pQuery.getContent());
         }
 
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(pQuery.getWorkspaceId());
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
 
         ListIterator<DocumentMaster> ite = fetchedDocMs.listIterator();
         docMBlock:
         while (ite.hasNext()) {
             DocumentMaster docM = ite.next();
             if (indexedKeys != null && (!indexedKeys.contains(docM.getKey()))) {
                 ite.remove();
                 continue docMBlock;
             }
 
             //TODO search should not fetch back private docM
             if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
                 docM = docM.clone();
                 docM.removeLastIteration();
                 ite.set(docM);
             }
 
             //Check access rights
             if (!isAdmin && docM.getACL() != null && !docM.getACL().hasReadAccess(user)) {
                 ite.remove();
                 continue;
             }
         }
         return fetchedDocMs.toArray(new DocumentMaster[fetchedDocMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterTemplate[] getDocumentMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).findAllDocMTemplates(pWorkspaceId);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterTemplate getDocumentMasterTemplate(DocumentMasterTemplateKey pKey)
             throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());
         return new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(pKey);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterTemplate updateDocumentMasterTemplate(DocumentMasterTemplateKey pKey, String pDocumentType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
 
         DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em);
         DocumentMasterTemplate template = templateDAO.loadDocMTemplate(pKey);
         Date now = new Date();
         template.setCreationDate(now);
         template.setAuthor(user);
         template.setDocumentType(pDocumentType);
         template.setMask(pMask);
         template.setIdGenerated(idGenerated);
 
         Set<InstanceAttributeTemplate> attrs = new HashSet<InstanceAttributeTemplate>();
         for (InstanceAttributeTemplate attr : pAttributeTemplates) {
             attrs.add(attr);
         }
 
         Set<InstanceAttributeTemplate> attrsToRemove = new HashSet<InstanceAttributeTemplate>(template.getAttributeTemplates());
         attrsToRemove.removeAll(attrs);
 
         InstanceAttributeTemplateDAO attrDAO = new InstanceAttributeTemplateDAO(em);
         for (InstanceAttributeTemplate attrToRemove : attrsToRemove) {
             attrDAO.removeAttribute(attrToRemove);
         }
 
         template.setAttributeTemplates(attrs);
         return template;
     }
 
     @RolesAllowed("users")
     @Override
     public void deleteTag(TagKey pKey) throws WorkspaceNotFoundException, AccessRightException, TagNotFoundException, UserNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
         Locale userLocale = new Locale(user.getLanguage());
         Tag tagToRemove = new Tag(user.getWorkspace(), pKey.getLabel());
         List<DocumentMaster> docMs = new DocumentMasterDAO(userLocale, em).findDocMsByTag(tagToRemove);
         for (DocumentMaster docM : docMs) {
             docM.getTags().remove(tagToRemove);
         }
 
         new TagDAO(userLocale, em).removeTag(pKey);
     }
 
     @Override
     public void createTag(String pWorkspaceId, String pLabel) throws WorkspaceNotFoundException, AccessRightException, CreationException, TagAlreadyExistsException, UserNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
         Locale userLocale = new Locale(user.getLanguage());
         TagDAO tagDAO = new TagDAO(userLocale, em);
         Tag tag = new Tag(user.getWorkspace(), pLabel);
         tagDAO.createTag(tag);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster createDocumentMaster(String pParentFolder,
             String pDocMId, String pTitle, String pDescription, String pDocMTemplateId, String pWorkflowModelId,
             ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String,String> roleMappings)
             throws WorkspaceNotFoundException, WorkflowModelNotFoundException, NotAllowedException,
             DocumentMasterTemplateNotFoundException, AccessRightException, DocumentMasterAlreadyExistsException,
             FolderNotFoundException, FileAlreadyExistsException, UserNotFoundException, CreationException,
             RoleNotFoundException {
 
         User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pParentFolder));
         if (!NamingConvention.correct(pDocMId)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException9");
         }
         Folder folder = new FolderDAO(new Locale(user.getLanguage()), em).loadFolder(pParentFolder);
         checkWritingRight(user, folder);
 
         DocumentMaster docM;
         DocumentIteration newDoc;
 
         if (pDocMTemplateId == null) {
             docM = new DocumentMaster(user.getWorkspace(), pDocMId, user);
             newDoc = docM.createNextIteration(user);
             //specify an empty type instead of null
             //so the search will find it with the % character
             docM.setType("");
         } else {
             DocumentMasterTemplate template = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(new DocumentMasterTemplateKey(user.getWorkspaceId(), pDocMTemplateId));
             docM = new DocumentMaster(user.getWorkspace(), pDocMId, user);
             docM.setType(template.getDocumentType());
             newDoc = docM.createNextIteration(user);
 
             Map<String, InstanceAttribute> attrs = new HashMap<String, InstanceAttribute>();
             for (InstanceAttributeTemplate attrTemplate : template.getAttributeTemplates()) {
                 InstanceAttribute attr = attrTemplate.createInstanceAttribute();
                 //attr.setDocument(newDoc);
                 attrs.put(attr.getName(), attr);
             }
             newDoc.setInstanceAttributes(attrs);
 
             BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
             for (BinaryResource sourceFile : template.getAttachedFiles()) {
                 String fileName = sourceFile.getName();
                 long length = sourceFile.getContentLength();
                 Date lastModified = sourceFile.getLastModified();
                 String fullName = docM.getWorkspaceId() + "/documents/" + docM.getId() + "/A/1/" + fileName;
                 BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                 binDAO.createBinaryResource(targetFile);
 
                 newDoc.addFile(targetFile);
                 try {
                     dataManager.copyData(sourceFile, targetFile);
                 } catch (StorageException e) {
                     e.printStackTrace();
                 }
             }
         }
 
         if (pWorkflowModelId != null) {
 
             UserDAO userDAO = new UserDAO(new Locale(user.getLanguage()),em);
             RoleDAO roleDAO = new RoleDAO(new Locale(user.getLanguage()),em);
 
             Map<Role,User> roleUserMap = new HashMap<Role,User>();
 
             Iterator it = roleMappings.entrySet().iterator();
 
             while (it.hasNext()) {
                 Map.Entry pairs = (Map.Entry)it.next();
                 String roleName = (String) pairs.getKey();
                 String userLogin = (String) pairs.getValue();
                 User worker = userDAO.loadUser(new UserKey(Folder.parseWorkspaceId(pParentFolder),userLogin));
                 Role role  = roleDAO.loadRole(new RoleKey(Folder.parseWorkspaceId(pParentFolder),roleName));
                 roleUserMap.put(role,worker);
             }
 
             WorkflowModel workflowModel = new WorkflowModelDAO(new Locale(user.getLanguage()), em).loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
             Workflow workflow = workflowModel.createWorkflow(roleUserMap);
             docM.setWorkflow(workflow);
 
             Collection<Task> runningTasks = workflow.getRunningTasks();
             for (Task runningTask : runningTasks) {
                 runningTask.start();
             }
             mailer.sendApproval(runningTasks, docM);
         }
 
         docM.setTitle(pTitle);
         docM.setDescription(pDescription);
 
         if ((pACLUserEntries != null && pACLUserEntries.length > 0) || (pACLUserGroupEntries != null && pACLUserGroupEntries.length > 0)) {
             ACL acl = new ACL();
             if (pACLUserEntries != null) {
                 for (ACLUserEntry entry : pACLUserEntries) {
                     acl.addEntry(em.getReference(User.class, new UserKey(user.getWorkspaceId(), entry.getPrincipalLogin())), entry.getPermission());
                 }
             }
 
             if (pACLUserGroupEntries != null) {
                 for (ACLUserGroupEntry entry : pACLUserGroupEntries) {
                     acl.addEntry(em.getReference(UserGroup.class, new UserGroupKey(user.getWorkspaceId(), entry.getPrincipalId())), entry.getPermission());
                 }
             }
             docM.setACL(acl);
         }
         Date now = new Date();
         docM.setCreationDate(now);
         docM.setLocation(folder);
         docM.setCheckOutUser(user);
         docM.setCheckOutDate(now);
         newDoc.setCreationDate(now);
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
 
         docMDAO.createDocM(docM);
         return docM;
     }
 
     @RolesAllowed({"users","admin"})
     @Override
     public DocumentMaster[] getAllCheckedOutDocumentMasters(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         List<DocumentMaster> docMs = new DocumentMasterDAO(new Locale(user.getLanguage()), em).findAllCheckedOutDocMs(pWorkspaceId);
         return docMs.toArray(new DocumentMaster[docMs.size()]);
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterTemplate createDocumentMasterTemplate(String pWorkspaceId, String pId, String pDocumentType,
             String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateAlreadyExistsException, UserNotFoundException, NotAllowedException, CreationException {
         User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
         if (!NamingConvention.correct(pId)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException9");
         }
         DocumentMasterTemplate template = new DocumentMasterTemplate(user.getWorkspace(), pId, user, pDocumentType, pMask);
         Date now = new Date();
         template.setCreationDate(now);
         template.setIdGenerated(idGenerated);
 
         Set<InstanceAttributeTemplate> attrs = new HashSet<InstanceAttributeTemplate>();
         for (InstanceAttributeTemplate attr : pAttributeTemplates) {
             attrs.add(attr);
         }
         template.setAttributeTemplates(attrs);
 
         new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).createDocMTemplate(template);
         return template;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster moveDocumentMaster(String pParentFolder, DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, UserNotFoundException, UserNotActiveException {
         //TODO security check if both parameter belong to the same workspace
         User user = checkDocumentMasterWriteAccess(pDocMPK);
 
         Folder newLocation = new FolderDAO(new Locale(user.getLanguage()), em).loadFolder(pParentFolder);
         checkWritingRight(user, newLocation);
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
         //Check access rights on docM
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(pDocMPK.getWorkspaceId());
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
         if (!isAdmin && docM.getACL() != null && !docM.getACL().hasWriteAccess(user)) {
             throw new AccessRightException(new Locale(user.getLanguage()), user);
         }
         Folder oldLocation = docM.getLocation();
         String owner = oldLocation.getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException6");
         } else {
             docM.setLocation(newLocation);
             if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
                 docM = docM.clone();
                 docM.removeLastIteration();
             }
             return docM;
         }
     }
 
     @RolesAllowed("users")
     @Override
     public Folder createFolder(String pParentFolder, String pFolder)
             throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, FolderAlreadyExistsException, UserNotFoundException, CreationException {
         User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pParentFolder));
         if (!NamingConvention.correct(pFolder)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException9");
         }
         FolderDAO folderDAO = new FolderDAO(new Locale(user.getLanguage()), em);
         Folder folder = folderDAO.loadFolder(pParentFolder);
         checkFolderStructureRight(user);
         checkWritingRight(user, folder);
         Folder newFolder = new Folder(pParentFolder, pFolder);
         folderDAO.createFolder(newFolder);
         return newFolder;
     }
 
     @RolesAllowed("users")
     @CheckActivity
     @Override
     public DocumentMaster approve(String pWorkspaceId, TaskKey pTaskKey, String pComment, String pSignature)
             throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException {
         //TODO no check is made that pTaskKey is from the same workspace than pWorkspaceId
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
 
         Task task = new TaskDAO(new Locale(user.getLanguage()), em).loadTask(pTaskKey);
         Workflow workflow = task.getActivity().getWorkflow();
         DocumentMaster docM = new WorkflowDAO(em).getTarget(workflow);
 
 
         if (!task.getWorker().equals(user)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException14");
         }
 
         if (!workflow.getRunningTasks().contains(task)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException15");
         }
 
         if (docM.isCheckedOut()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException16");
         }
 
         int previousStep = workflow.getCurrentStep();
         task.approve(pComment, docM.getLastIteration().getIteration(), pSignature);
         int currentStep = workflow.getCurrentStep();
 
         User[] subscribers = new SubscriptionDAO(em).getStateChangeEventSubscribers(docM);
 
         if (previousStep != currentStep && subscribers.length != 0) {
             mailer.sendStateNotification(subscribers, docM);
         }
 
         Collection<Task> runningTasks = workflow.getRunningTasks();
         for (Task runningTask : runningTasks) {
             runningTask.start();
         }
         mailer.sendApproval(runningTasks, docM);
         return docM;
     }
 
     @RolesAllowed("users")
     @CheckActivity
     @Override
     public DocumentMaster reject(String pWorkspaceId, TaskKey pTaskKey, String pComment, String pSignature)
             throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException {
         //TODO no check is made that pTaskKey is from the same workspace than pWorkspaceId
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
 
         Task task = new TaskDAO(new Locale(user.getLanguage()), em).loadTask(pTaskKey);
         Workflow workflow = task.getActivity().getWorkflow();
         DocumentMaster docM = new WorkflowDAO(em).getTarget(workflow);
 
         if (!task.getWorker().equals(user)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException14");
         }
 
         if (!workflow.getRunningTasks().contains(task)) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException15");
         }
 
         if (docM.isCheckedOut()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException16");
         }
 
         task.reject(pComment, docM.getLastIteration().getIteration(), pSignature);
         return docM;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster checkOutDocument(DocumentMasterKey pDocMPK)
             throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, FileAlreadyExistsException, UserNotFoundException, CreationException, UserNotActiveException {
 
         User user = checkDocumentMasterWriteAccess(pDocMPK);
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
         //Check access rights on docM
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(pDocMPK.getWorkspaceId());
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
 
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
         }
 
         if (docM.isCheckedOut()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException37");
         }
 
         DocumentIteration beforeLastDocument = docM.getLastIteration();
 
         DocumentIteration newDoc = docM.createNextIteration(user);
         //We persist the doc as a workaround for a bug which was introduced
         //since glassfish 3 that set the DTYPE to null in the instance attribute table
         em.persist(newDoc);
         Date now = new Date();
         newDoc.setCreationDate(now);
         docM.setCheckOutUser(user);
         docM.setCheckOutDate(now);
 
         if (beforeLastDocument != null) {
             BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
             for (BinaryResource sourceFile : beforeLastDocument.getAttachedFiles()) {
                 String fileName = sourceFile.getName();
                 long length = sourceFile.getContentLength();
                 Date lastModified = sourceFile.getLastModified();
                 String fullName = docM.getWorkspaceId() + "/documents/" + docM.getId() + "/" + docM.getVersion() + "/" + newDoc.getIteration() + "/" + fileName;
                 BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                 binDAO.createBinaryResource(targetFile);
                 newDoc.addFile(targetFile);
             }
 
             Set<DocumentLink> links = new HashSet<DocumentLink>();
             for (DocumentLink link : beforeLastDocument.getLinkedDocuments()) {
                 DocumentLink newLink = link.clone();
                 links.add(newLink);
             }
             newDoc.setLinkedDocuments(links);
 
             InstanceAttributeDAO attrDAO = new InstanceAttributeDAO(em);
             Map<String, InstanceAttribute> attrs = new HashMap<String, InstanceAttribute>();
             for (InstanceAttribute attr : beforeLastDocument.getInstanceAttributes().values()) {
                 InstanceAttribute newAttr = attr.clone();
                 //newAttr.setDocument(newDoc);
                 //Workaround for the NULL DTYPE bug
                 attrDAO.createAttribute(newAttr);
                 attrs.put(newAttr.getName(), newAttr);
             }
             newDoc.setInstanceAttributes(attrs);
         }
 
         return docM;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster saveTags(DocumentMasterKey pDocMPK, String[] pTags)
             throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException {
 
         User user = checkDocumentMasterWriteAccess(pDocMPK);
 
         Locale userLocale = new Locale(user.getLanguage());
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(userLocale, em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
         }
 
         HashSet<Tag> tags = new HashSet<Tag>();
         for (String label : pTags) {
             tags.add(new Tag(user.getWorkspace(), label));
         }
 
         TagDAO tagDAO = new TagDAO(userLocale, em);
         List<Tag> existingTags = Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));
 
         Set<Tag> tagsToCreate = new HashSet<Tag>(tags);
         tagsToCreate.removeAll(existingTags);
 
         for (Tag t : tagsToCreate) {
             try {
                 tagDAO.createTag(t);
             } catch (CreationException ex) {
                 LOGGER.log(Level.SEVERE, null, ex);
             } catch (TagAlreadyExistsException ex) {
                 LOGGER.log(Level.SEVERE, null, ex);
             }
         }
 
         docM.setTags(tags);
 
         if ((docM.isCheckedOut()) && (!docM.getCheckOutUser().equals(user))) {
             docM = docM.clone();
             docM.removeLastIteration();
         }
         return docM;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster undoCheckOutDocument(DocumentMasterKey pDocMPK)
             throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException {
 
         User user = checkDocumentMasterWriteAccess(pDocMPK);
 
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
         if (docM.isCheckedOut() && docM.getCheckOutUser().equals(user)) {
             DocumentIteration doc = docM.removeLastIteration();
             for (BinaryResource file : doc.getAttachedFiles()) {
                 try {
                     dataManager.deleteData(file);
                 } catch (StorageException e) {
                     e.printStackTrace();
                 }
             }
 
             DocumentDAO docDAO = new DocumentDAO(em);
             docDAO.removeDoc(doc);
             docM.setCheckOutDate(null);
             docM.setCheckOutUser(null);
             return docM;
         } else {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException19");
         }
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster checkInDocument(DocumentMasterKey pDocMPK)
             throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException {
 
         User user = checkDocumentMasterWriteAccess(pDocMPK);
 
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
 
         if (docM.isCheckedOut() && docM.getCheckOutUser().equals(user)) {
             User[] subscribers = new SubscriptionDAO(em).getIterationChangeEventSubscribers(docM);
 
             docM.setCheckOutDate(null);
             docM.setCheckOutUser(null);
 
             if (subscribers.length != 0) {
                 mailer.sendIterationNotification(subscribers, docM);
             }
 
             for (BinaryResource bin : docM.getLastIteration().getAttachedFiles()) {
                 try {
                     InputStream inputStream = dataManager.getBinaryResourceInputStream(bin);
                     indexer.addToIndex(bin.getFullName(), inputStream);
                 } catch (StorageException e) {
                     e.printStackTrace();
                 }
             }
 
             return docM;
         } else {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException20");
         }
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterKey[] deleteFolder(String pCompletePath)
             throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pCompletePath));
         FolderDAO folderDAO = new FolderDAO(new Locale(user.getLanguage()), em);
         Folder folder = folderDAO.loadFolder(pCompletePath);
         String owner = folder.getOwner();
         checkFolderStructureRight(user);
         if (((owner != null) && (!owner.equals(user.getLogin()))) || (folder.isRoot()) || folder.isHome()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException21");
         } else {
             List<DocumentMaster> docMs = folderDAO.removeFolder(folder);
             DocumentMasterKey[] pks = new DocumentMasterKey[docMs.size()];
             int i = 0;
             for (DocumentMaster docM : docMs) {
                 pks[i++] = docM.getKey();
                 for (DocumentIteration doc : docM.getDocumentIterations()) {
                     for (BinaryResource file : doc.getAttachedFiles()) {
                         indexer.removeFromIndex(file.getFullName());
                         try {
                             dataManager.deleteData(file);
                         } catch (StorageException e) {
                             e.printStackTrace();
                         }
                     }
                 }
             }
             return pks;
         }
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterKey[] moveFolder(String pCompletePath, String pDestParentFolder, String pDestFolder)
             throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, CreationException, FolderAlreadyExistsException {
         //TODO security check if both parameter belong to the same workspace
         String workspace = Folder.parseWorkspaceId(pCompletePath);
         User user = userManager.checkWorkspaceWriteAccess(workspace);
         FolderDAO folderDAO = new FolderDAO(new Locale(user.getLanguage()), em);
         Folder folder = folderDAO.loadFolder(pCompletePath);
         String owner = folder.getOwner();
         checkFolderStructureRight(user);
         if (((owner != null) && (!owner.equals(user.getLogin()))) || (folder.isRoot()) || folder.isHome()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException21");
         } else if (!workspace.equals(Folder.parseWorkspaceId(pDestParentFolder))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException23");
         } else {
             Folder newFolder = createFolder(pDestParentFolder, pDestFolder);
             List<DocumentMaster> docMs = folderDAO.moveFolder(folder, newFolder);
             DocumentMasterKey[] pks = new DocumentMasterKey[docMs.size()];
             int i = 0;
             for (DocumentMaster docM : docMs) {
                 pks[i++] = docM.getKey();
             }
             return pks;
         }
     }
 
     @RolesAllowed("users")
     @Override
     public void deleteDocumentMaster(DocumentMasterKey pDocMPK)
             throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException {
 
         User user = checkDocumentMasterWriteAccess(pDocMPK);
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(pDocMPK);
         //Check access rights on
         Workspace wks = new WorkspaceDAO(new Locale(user.getLanguage()), em).loadWorkspace(pDocMPK.getWorkspaceId());
         boolean isAdmin = wks.getAdmin().getLogin().equals(user.getLogin());
         if (!isAdmin && docM.getACL() != null && !docM.getACL().hasWriteAccess(user)) {
             throw new AccessRightException(new Locale(user.getLanguage()), user);
         }
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException22");
         }
 
         docMDAO.removeDocM(docM);
 
         for (DocumentIteration doc : docM.getDocumentIterations()) {
             for (BinaryResource file : doc.getAttachedFiles()) {
                 indexer.removeFromIndex(file.getFullName());
                 try {
                     dataManager.deleteData(file);
                 } catch (StorageException e) {
                     e.printStackTrace();
                 }
             }
         }
     }
 
     @RolesAllowed("users")
     @Override
     public void deleteDocumentMasterTemplate(DocumentMasterTemplateKey pKey)
             throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
         DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em);
         DocumentMasterTemplate template = templateDAO.removeDocMTemplate(pKey);
         for (BinaryResource file : template.getAttachedFiles()) {
             try {
                 dataManager.deleteData(file);
             } catch (StorageException e) {
                 e.printStackTrace();
             }
         }
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster removeFileFromDocument(String pFullName) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
 
         BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
         BinaryResource file = binDAO.loadBinaryResource(pFullName);
 
         DocumentIteration document = binDAO.getDocumentOwner(file);
         DocumentMaster docM = document.getDocumentMaster();
 
         //check access rights on docM
         user = checkDocumentMasterWriteAccess(docM.getKey());
 
         if (docM.isCheckedOut() && docM.getCheckOutUser().equals(user) && docM.getLastIteration().equals(document)) {
             try {
                 dataManager.deleteData(file);
             } catch (StorageException e) {
                 e.printStackTrace();
             }
             document.removeFile(file);
             binDAO.removeBinaryResource(file);
             return docM;
         } else {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException24");
         }
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
         //TODO checkWorkspaceWriteAccess ?
         BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
         BinaryResource file = binDAO.loadBinaryResource(pFullName);
 
         DocumentMasterTemplate template = binDAO.getDocumentTemplateOwner(file);
         try {
             dataManager.deleteData(file);
         } catch (StorageException e) {
             e.printStackTrace();
         }
         template.removeFile(file);
         binDAO.removeBinaryResource(file);
         return template;
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster updateDocument(DocumentIterationKey pKey, String pRevisionNote, InstanceAttribute[] pAttributes, DocumentIterationKey[] pLinkKeys) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException {
 
         User user = checkDocumentMasterWriteAccess(new DocumentMasterKey(pKey.getWorkspaceId(), pKey.getDocumentMasterId(), pKey.getDocumentMasterVersion()));
 
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentLinkDAO linkDAO = new DocumentLinkDAO(new Locale(user.getLanguage()), em);
         DocumentMaster docM = docMDAO.loadDocM(new DocumentMasterKey(pKey.getWorkspaceId(), pKey.getDocumentMasterId(), pKey.getDocumentMasterVersion()));
         //check access rights on docM ?
         if (docM.isCheckedOut() && docM.getCheckOutUser().equals(user) && docM.getLastIteration().getKey().equals(pKey)) {
             DocumentIteration doc = docM.getLastIteration();
 
             Set<DocumentIterationKey> linkKeys = new HashSet<DocumentIterationKey>(Arrays.asList(pLinkKeys));
             Set<DocumentIterationKey> currentLinkKeys = new HashSet<DocumentIterationKey>();
 
             Set<DocumentLink> currentLinks = new HashSet<DocumentLink>(doc.getLinkedDocuments());
 
             for(DocumentLink link:currentLinks){
                 DocumentIterationKey linkKey = link.getTargetDocumentKey();
                 if(!linkKeys.contains(linkKey)){
                     doc.getLinkedDocuments().remove(link);
                 }else
                     currentLinkKeys.add(linkKey);
             }
 
             for(DocumentIterationKey link:linkKeys){
                 if(!currentLinkKeys.contains(link)){
                     DocumentLink newLink = new DocumentLink(em.getReference(DocumentIteration.class,link));
                     linkDAO.createLink(newLink);
                     doc.getLinkedDocuments().add(newLink);
                 }
             }
 
             
             Map<String, InstanceAttribute> attrs = new HashMap<String, InstanceAttribute>();
             for (InstanceAttribute attr : pAttributes) {
                 attrs.put(attr.getName(), attr);
             }
 
             Set<InstanceAttribute> currentAttrs = new HashSet<InstanceAttribute>(doc.getInstanceAttributes().values());
 
             for(InstanceAttribute attr:currentAttrs){
                 if(!attrs.containsKey(attr.getName())){
                     doc.getInstanceAttributes().remove(attr.getName());
                 }
             }
 
             for(InstanceAttribute attr:attrs.values()){
                 if(!doc.getInstanceAttributes().containsKey(attr.getName())){
                     doc.getInstanceAttributes().put(attr.getName(), attr);
                 }else if(doc.getInstanceAttributes().get(attr.getName()).getClass() != attr.getClass()){
                     doc.getInstanceAttributes().remove(attr.getName());
                     doc.getInstanceAttributes().put(attr.getName(), attr);
                 }else{
                     doc.getInstanceAttributes().get(attr.getName()).setValue(attr.getValue());
                 }
             }
 
             doc.setRevisionNote(pRevisionNote);
             //doc.setLinkedDocuments(links);
             return docM;
 
         } else {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException25");
         }
 
     }
 
     @RolesAllowed("users")
     @Override
     public DocumentMaster[] createVersion(DocumentMasterKey pOriginalDocMPK,
             String pTitle, String pDescription, String pWorkflowModelId, ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String,String> roleMappings) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, WorkflowModelNotFoundException, AccessRightException, DocumentMasterAlreadyExistsException, FileAlreadyExistsException, UserNotFoundException, CreationException, RoleNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(pOriginalDocMPK.getWorkspaceId());
         DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
         DocumentMaster originalDocM = docMDAO.loadDocM(pOriginalDocMPK);
         Folder folder = originalDocM.getLocation();
         checkWritingRight(user, folder);
 
         if (originalDocM.isCheckedOut()) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException26");
         }
 
         if (originalDocM.getNumberOfIterations() == 0) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException27");
         }
 
         Version version = new Version(originalDocM.getVersion());
         version.increase();
         DocumentMaster docM = new DocumentMaster(originalDocM.getWorkspace(), originalDocM.getId(), version, user);
         docM.setType(originalDocM.getType());
         //create the first iteration which is a copy of the last one of the original docM
         //of course we duplicate the iteration only if it exists !
         DocumentIteration lastDoc = originalDocM.getLastIteration();
         DocumentIteration firstIte = docM.createNextIteration(user);
         if (lastDoc != null) {
             BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
             for (BinaryResource sourceFile : lastDoc.getAttachedFiles()) {
                 String fileName = sourceFile.getName();
                 long length = sourceFile.getContentLength();
                 Date lastModified = sourceFile.getLastModified();
                 String fullName = docM.getWorkspaceId() + "/documents/" + docM.getId() + "/" + version + "/1/" + fileName;
                 BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                 binDAO.createBinaryResource(targetFile);
                 firstIte.addFile(targetFile);
                 try {
                     dataManager.copyData(sourceFile, targetFile);
                 } catch (StorageException e) {
                     e.printStackTrace();
                 }
             }
 
             Set<DocumentLink> links = new HashSet<DocumentLink>();
             for (DocumentLink link : lastDoc.getLinkedDocuments()) {
                 DocumentLink newLink = link.clone();
                 links.add(newLink);
             }
             firstIte.setLinkedDocuments(links);
 
             Map<String, InstanceAttribute> attrs = new HashMap<String, InstanceAttribute>();
             for (InstanceAttribute attr : lastDoc.getInstanceAttributes().values()) {
                 InstanceAttribute clonedAttribute = attr.clone();
                 //clonedAttribute.setDocument(firstIte);
                 attrs.put(clonedAttribute.getName(), clonedAttribute);
             }
             firstIte.setInstanceAttributes(attrs);
         }
 
         if (pWorkflowModelId != null) {
 
             UserDAO userDAO = new UserDAO(new Locale(user.getLanguage()),em);
             RoleDAO roleDAO = new RoleDAO(new Locale(user.getLanguage()),em);
 
             Map<Role,User> roleUserMap = new HashMap<Role,User>();
 
             Iterator it = roleMappings.entrySet().iterator();
 
             while (it.hasNext()) {
                 Map.Entry pairs = (Map.Entry)it.next();
                 String roleName = (String) pairs.getKey();
                 String userLogin = (String) pairs.getValue();
                 User worker = userDAO.loadUser(new UserKey(pOriginalDocMPK.getWorkspaceId(),userLogin));
                 Role role  = roleDAO.loadRole(new RoleKey(pOriginalDocMPK.getWorkspaceId(),roleName));
                 roleUserMap.put(role,worker);
             }
 
             WorkflowModel workflowModel = new WorkflowModelDAO(new Locale(user.getLanguage()), em).loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
             Workflow workflow = workflowModel.createWorkflow(roleUserMap);
             docM.setWorkflow(workflow);
 
             Collection<Task> runningTasks = workflow.getRunningTasks();
             for (Task runningTask : runningTasks) {
                 runningTask.start();
             }
             mailer.sendApproval(runningTasks, docM);
         }
         docM.setTitle(pTitle);
         docM.setDescription(pDescription);
         if ((pACLUserEntries != null && pACLUserEntries.length > 0) || (pACLUserGroupEntries != null && pACLUserGroupEntries.length > 0)) {
             ACL acl = new ACL();
             if (pACLUserEntries != null) {
                 for (ACLUserEntry entry : pACLUserEntries) {
                     acl.addEntry(em.getReference(User.class, new UserKey(user.getWorkspaceId(), entry.getPrincipalLogin())), entry.getPermission());
                 }
             }
 
             if (pACLUserGroupEntries != null) {
                 for (ACLUserGroupEntry entry : pACLUserGroupEntries) {
                     acl.addEntry(em.getReference(UserGroup.class, new UserGroupKey(user.getWorkspaceId(), entry.getPrincipalId())), entry.getPermission());
                 }
             }
             docM.setACL(acl);
         }
         Date now = new Date();
         docM.setCreationDate(now);
         docM.setLocation(folder);
         docM.setCheckOutUser(user);
         docM.setCheckOutDate(now);
         firstIte.setCreationDate(now);
 
         docMDAO.createDocM(docM);
         return new DocumentMaster[]{originalDocM, docM};
     }
 
     @RolesAllowed("users")
     @Override
     public void subscribeToStateChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {
         User user = checkDocumentMasterReadAccess(pDocMPK);
         DocumentMaster docM = new DocumentMasterDAO(new Locale(user.getLanguage()), em).loadDocM(pDocMPK);
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException30");
         }
 
         new SubscriptionDAO(em).createStateChangeSubscription(new StateChangeSubscription(user, docM));
     }
 
     @RolesAllowed("users")
     @Override
     public void unsubscribeToStateChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentMasterNotFoundException {
         User user = checkDocumentMasterReadAccess(pDocMPK);
         SubscriptionKey key = new SubscriptionKey(user.getWorkspaceId(), user.getLogin(), pDocMPK.getWorkspaceId(), pDocMPK.getId(), pDocMPK.getVersion());
         new SubscriptionDAO(em).removeStateChangeSubscription(key);
     }
 
     @RolesAllowed("users")
     @Override
     public void subscribeToIterationChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {
         User user = checkDocumentMasterReadAccess(pDocMPK);
         DocumentMaster docM = new DocumentMasterDAO(new Locale(user.getLanguage()), em).getDocMRef(pDocMPK);
         String owner = docM.getLocation().getOwner();
         if ((owner != null) && (!owner.equals(user.getLogin()))) {
             throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException30");
         }
 
         new SubscriptionDAO(em).createIterationChangeSubscription(new IterationChangeSubscription(user, docM));
     }
 
     @RolesAllowed("users")
     @Override
     public void unsubscribeToIterationChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentMasterNotFoundException {
         User user = checkDocumentMasterReadAccess(pDocMPK);
         SubscriptionKey key = new SubscriptionKey(user.getWorkspaceId(), user.getLogin(), pDocMPK.getWorkspaceId(), pDocMPK.getId(), pDocMPK.getVersion());
         new SubscriptionDAO(em).removeIterationChangeSubscription(key);
     }
 
     @RolesAllowed("users")
     @Override
     public User savePersonalInfo(String pWorkspaceId, String pName, String pEmail, String pLanguage) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         user.setName(pName);
         user.setEmail(pEmail);
         user.setLanguage(pLanguage);
         return user;
     }
 
     @RolesAllowed({"users","admin"})
     @Override
     public User[] getUsers(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         return new UserDAO(new Locale(user.getLanguage()), em).findAllUsers(pWorkspaceId);
     }
 
     @RolesAllowed("users")
     @Override
     public User[] getReachableUsers() throws AccountNotFoundException {
         String callerLogin = ctx.getCallerPrincipal().getName();
         Account account = new AccountDAO(em).loadAccount(callerLogin);
         return new UserDAO(new Locale(account.getLanguage()), em).findReachableUsersForCaller(callerLogin);
     }
 
     @RolesAllowed("users")
     @Override
     public String[] getTags(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         Tag[] tags = new TagDAO(new Locale(user.getLanguage()), em).findAllTags(pWorkspaceId);
 
         String[] labels = new String[tags.length];
         int i = 0;
         for (Tag t : tags) {
             labels[i++] = t.getLabel();
         }
         return labels;
     }
 
     @RolesAllowed({"users","admin"})
     @Override
     public int getDocumentsCountInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()),em);
         return documentMasterDAO.getDocumentsCountInWorkspace(pWorkspaceId);
     }
 
     @RolesAllowed({"users","admin"})
     @Override
     public Long getDiskUsageForDocumentsInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()),em);
         return documentMasterDAO.getDiskUsageForDocumentsInWorkspace(pWorkspaceId);
     }
 
     @RolesAllowed({"users","admin"})
     @Override
     public Long getDiskUsageForDocumentTemplatesInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
         User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
         DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()),em);
         return documentMasterDAO.getDiskUsageForDocumentTemplatesInWorkspace(pWorkspaceId);
     }
 
     @RolesAllowed({"users"})
     @Override
     public SharedDocument createSharedDocument(DocumentMasterKey pDocMPK, String pPassword, Date pExpireDate) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, DocumentMasterNotFoundException, UserNotActiveException, NotAllowedException {
         User user = userManager.checkWorkspaceWriteAccess(pDocMPK.getWorkspaceId());
         SharedDocument sharedDocument = new SharedDocument(user.getWorkspace(), user, pExpireDate, pPassword, getDocumentMaster(pDocMPK));
         SharedEntityDAO sharedEntityDAO = new SharedEntityDAO(new Locale(user.getLanguage()),em);
         sharedEntityDAO.createSharedDocument(sharedDocument);
         return sharedDocument;
     }
 
     @RolesAllowed({"users"})
     @Override
     public void deleteSharedDocument(SharedEntityKey sharedEntityKey) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, SharedEntityNotFoundException {
         User user = userManager.checkWorkspaceWriteAccess(sharedEntityKey.getWorkspace());
         SharedEntityDAO sharedEntityDAO = new SharedEntityDAO(new Locale(user.getLanguage()),em);
         SharedDocument sharedDocument = sharedEntityDAO.loadSharedDocument(sharedEntityKey.getUuid());
         sharedEntityDAO.deleteSharedDocument(sharedDocument);
     }
 
 
 
     private Folder checkWritingRight(User pUser, Folder pFolder) throws NotAllowedException {
         if (pFolder.isPrivate() && (!pFolder.getOwner().equals(pUser.getLogin()))) {
             throw new NotAllowedException(new Locale(pUser.getLanguage()), "NotAllowedException33");
         }
         return pFolder;
     }
 
     private void checkFolderStructureRight(User pUser) throws NotAllowedException {
         Workspace wks = pUser.getWorkspace();
         if (wks.isFolderLocked() && (!pUser.isAdministrator())) {
             throw new NotAllowedException(new Locale(pUser.getLanguage()), "NotAllowedException7");
         }
     }
 
     private User checkDocumentMasterWriteAccess(DocumentMasterKey documentMasterKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, DocumentMasterNotFoundException {
 
         User user = userManager.checkWorkspaceReadAccess(documentMasterKey.getWorkspaceId());
         if(user.isAdministrator()){
             return user;
         }
         DocumentMaster documentMaster = new DocumentMasterDAO(em).loadDocM(documentMasterKey);
 
         if(documentMaster.getACL()==null){
             return userManager.checkWorkspaceWriteAccess(documentMasterKey.getWorkspaceId());
         }else{
             if(documentMaster.getACL().hasWriteAccess(user)){
                 return user;
             }else{
                 throw new AccessRightException(new Locale(user.getLanguage()),user);
             }
         }
 
     }
 
     private User checkDocumentMasterReadAccess(DocumentMasterKey documentMasterKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, DocumentMasterNotFoundException {
 
         User user = userManager.checkWorkspaceReadAccess(documentMasterKey.getWorkspaceId());
         if(user.isAdministrator()){
             return user;
         }
         DocumentMaster documentMaster = new DocumentMasterDAO(em).loadDocM(documentMasterKey);
 
         if(documentMaster.getACL()==null){
             return user;
         }else{
             if(documentMaster.getACL().hasReadAccess(user)){
                 return user;
             }else{
                 throw new AccessRightException(new Locale(user.getLanguage()),user);
             }
         }
 
     }
 }
