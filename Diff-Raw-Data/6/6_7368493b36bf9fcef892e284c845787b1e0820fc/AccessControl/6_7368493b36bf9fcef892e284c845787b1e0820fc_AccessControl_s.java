 package com.idega.core.accesscontrol.business;
 
 import java.util.List;
 import java.util.Hashtable;
 import java.util.Vector;
 import java.util.Iterator;
 import java.util.Enumeration;
 import com.idega.presentation.*;
 import com.idega.block.login.business.*;
 import com.idega.core.data.*;
 import com.idega.core.user.data.User;
 import com.idega.data.EntityFinder;
 import com.idega.core.accesscontrol.data.*;
 import com.idega.core.business.*;
 import com.idega.core.user.business.UserBusiness;
 import com.idega.util.IWTimestamp;
 import com.idega.data.SimpleQuerier;
 import com.idega.util.EncryptionType;
 import com.idega.idegaweb.IWServiceImpl;
 import com.idega.idegaweb.IWServiceNotStartedException;
 import com.idega.idegaweb.IWMainApplication;
 import com.idega.core.user.data.UserGroupRepresentative;
 import com.idega.idegaweb.IWUserContext;
 import com.idega.builder.data.IBPage;
 import com.idega.user.data.Group;
 import com.idega.data.IDOLookup;
 
 import java.rmi.RemoteException;
 import java.sql.SQLException;
 import java.util.Set;
 
 
 
 
 /**
  * Title:        AccessControl
  * Description:
  * Copyright:    Copyright (c) 2001 idega.is All Rights Reserved
  * Company:      idega margmilun
  * @author       <a href="mailto:gummi@idega.is">Gumundur gst Smundsson</a>
  * @version 1.0
  */
 
 public class AccessControl extends IWServiceImpl implements AccessController {
 /**
  * @todo change next 4 variables to applicationAddesses
  */
   private PermissionGroup AdministratorPermissionGroup = null;
   private PermissionGroup PermissionGroupEveryOne = null;
   private PermissionGroup PermissionGroupUsers = null;
   private List standardGroups = null;
 
   private static final String _APPADDRESS_ADMINISTRATOR_USER = "ic_super_admin";
   private static final String _ADMINISTRATOR_NAME = "Administrator";
 
 
   //private static final int _GROUP_ID_EVERYONE = -7913;
   //private static final int _GROUP_ID_USERS = -1906;
 
   private static final int _GROUP_ID_EVERYONE = com.idega.user.data.GroupBMPBean.GROUP_ID_EVERYONE;
   private static final int _GROUP_ID_USERS = com.idega.user.data.GroupBMPBean.GROUP_ID_USERS;
 
 
   private static final int _notBuilderPageID = 0;
 
   //temp
   private static ICObject staticPageICObject = null;
   private static ICObject staticFileICObject = null;
 
 
 
 
   private void initAdministratorPermissionGroup() throws Exception {
     PermissionGroup permission = getPermissionGroupHome().create();
     permission.setName(AccessControl.getAdministratorGroupName());
     permission.setDescription("Administrator permission");
     permission.store();
     AdministratorPermissionGroup = permission;
   }
 
   private void initPermissionGroupEveryone() throws Exception {
     PermissionGroup permission = getPermissionGroupHome().create();
     permission.setID(_GROUP_ID_EVERYONE);
     permission.setName("Everyone");
     permission.setDescription("Permission if not logged on");
     permission.store();
     PermissionGroupEveryOne = permission;
   }
 
   private void initPermissionGroupUsers() throws Exception {
     PermissionGroup permission = getPermissionGroupHome().create();
     permission.setID(_GROUP_ID_USERS);
     permission.setName("Users");
     permission.setDescription("Permission if logged on");
     permission.insert();
     PermissionGroupUsers = permission;
   }
 
   public PermissionGroup getPermissionGroupEveryOne() throws Exception {
     if(PermissionGroupEveryOne == null){
       initPermissionGroupEveryone();
     }
     return PermissionGroupEveryOne;
   }
 
   public PermissionGroup getPermissionGroupUsers() throws Exception {
     if(PermissionGroupUsers == null){
       initPermissionGroupUsers();
     }
     return PermissionGroupUsers;
   }
 
   public PermissionGroup getPermissionGroupAdministrator() throws Exception {
     if(AdministratorPermissionGroup == null){
       initAdministratorPermissionGroup();
     }
     return AdministratorPermissionGroup;
   }
 
   public boolean isAdmin(IWUserContext iwc)throws Exception{
     try {
       Object ob = LoginBusiness.getLoginAttribute(getAdministratorGroupName(), iwc);
       if(ob != null){
         return ((Boolean)ob).booleanValue();
       }else{
         if(getAdministratorUser().equals(LoginBusiness.getUser(iwc))){
           LoginBusiness.setLoginAttribute(getAdministratorGroupName(),Boolean.TRUE,iwc);
           return true;
         }
         List groups = LoginBusiness.getPermissionGroups(iwc);
         if (groups != null){
           Iterator iter = groups.iterator();
           while (iter.hasNext()) {
             GenericGroup item = (GenericGroup)iter.next();
             if (getAdministratorGroupName().equals(item.getName())){
               LoginBusiness.setLoginAttribute(getAdministratorGroupName(),Boolean.TRUE,iwc);
               return true;
             }
           }
         }
       }
       LoginBusiness.setLoginAttribute(getAdministratorGroupName(),Boolean.FALSE,iwc);
       return false;
     }
     catch (NotLoggedOnException ex) {
       return false;
     }
   }
 
   /**
    * @todo page ownership
    */
   public boolean isOwner(PresentationObject obj , IWUserContext iwc) throws Exception {
     Boolean returnVal = Boolean.FALSE;
     User user = iwc.getUser();
     if(user != null){
       List[] permissionOrder = new Vector[2];
       permissionOrder[0] = new Vector();
       permissionOrder[0].add( Integer.toString(user.getGroupID()) );
       permissionOrder[1] = new Vector();
       permissionOrder[1].add( Integer.toString(user.getPrimaryGroupID()) );
 
       returnVal = checkForPermission(permissionOrder,obj,AccessControl._PERMISSIONKEY_OWNER,iwc);
     }
 
     if(returnVal != null){
       return returnVal.booleanValue();
     } else {
       return false;
     }
 
   }
 
   public boolean isOwner(int category, String identifier,IWUserContext iwc) throws Exception {
     Boolean returnVal = Boolean.FALSE;
     User user = iwc.getUser();
     if(user != null){
       List[] permissionOrder = new Vector[2];
       permissionOrder[0] = new Vector();
       permissionOrder[0].add( Integer.toString(user.getGroupID()) );
       permissionOrder[1] = new Vector();
       permissionOrder[1].add( Integer.toString(user.getPrimaryGroupID()) );
 
       returnVal = checkForPermission(permissionOrder,category, identifier,AccessControl._PERMISSIONKEY_OWNER,iwc);
     }
 
     if(returnVal != null){
       return returnVal.booleanValue();
     } else {
       return false;
     }
   }
 
   public boolean isOwner(List groupIds, PresentationObject obj,IWUserContext iwc) throws Exception {
     Boolean returnVal = Boolean.FALSE;
     List[] permissionOrder = new Vector[1];
     permissionOrder[0] = groupIds;
     returnVal = checkForPermission(permissionOrder, obj, AccessControl._PERMISSIONKEY_OWNER,iwc);
 
     if(returnVal != null){
       return returnVal.booleanValue();
     } else {
       return false;
     }
   }
 
   public boolean isOwner(ICFile file, IWUserContext iwc)throws Exception{
     return isOwner(AccessController._CATEGORY_FILE_ID, Integer.toString(file.getID()),iwc);
   }
 
   public boolean isOwner(IBPage page, IWUserContext iwc)throws Exception{
     return isOwner(AccessController._CATEGORY_PAGE_INSTANCE, Integer.toString(page.getID()),iwc);
   }
 
   /**
    * @todo implement isOwner(ICObject obj, int entityRecordId, IWUserContext iwc)throws Exception
    */
   public boolean isOwner(ICObject obj, int entityRecordId, IWUserContext iwc)throws Exception{
     return false;
   }
 
   /**
    * use this method when writing to database to avoid errors in database.
    * If the name-string changes this will be the only method to change.
    */
   public static String getAdministratorGroupName(){
     return "administrator";
   }
 
   public boolean hasPermission(String permissionKey, int category, String identifier, IWUserContext iwc) throws Exception{
     Boolean myPermission = null;  // Returned if one has permission for obj instance, true or false. If no instancepermission glopalpermission is checked
 
     if (isAdmin(iwc)){
       return true;
     }
 
     User user = LoginBusiness.getUser(iwc);
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     ICPermission[] Permissions = null;
     List groups = null;
     List tempGroupList = new Vector();
     List[] permissionOrder = null; // Everyone, users, user, primaryGroup, otherGroups
 
     if (user == null){
       permissionOrder = new List[1];
       permissionOrder[0] = new Vector();
       permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
     } else {
 
       groups = LoginBusiness.getPermissionGroups(iwc);
       GenericGroup primaryGroup = LoginBusiness.getPrimaryGroup(iwc);
 
       if (groups != null && groups.size() > 0){
         if(primaryGroup != null){
           groups.remove(primaryGroup);
         }
         List groupIds = new Vector();
         Iterator iter = groups.iterator();
         while (iter.hasNext()) {
           groupIds.add(Integer.toString(((GenericGroup)iter.next()).getID()));
         }
         permissionOrder = new List[5];
         permissionOrder[4] = groupIds;
       } else {
         permissionOrder = new List[4];
       }
         permissionOrder[0] = new Vector();
         permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
         permissionOrder[1] = new Vector();
         permissionOrder[1].add( Integer.toString(getPermissionGroupUsers().getID()) );
         permissionOrder[2] = new Vector();
         permissionOrder[2].add( Integer.toString(user.getGroupID()) );
         permissionOrder[3] = new Vector();
         permissionOrder[3].add( Integer.toString(user.getPrimaryGroupID()) );
         // Everyone, user, primaryGroup, otherGroups
     }
     myPermission = checkForPermission(permissionOrder, category, identifier, permissionKey, iwc);
     if(myPermission != null){
       return myPermission.booleanValue();
     }
 
 
     if(permissionKey.equals(AccessControl._PERMISSIONKEY_EDIT) || permissionKey.equals(AccessControl._PERMISSIONKEY_VIEW)){
       return isOwner(category, identifier, iwc);
     } else {
       return false;
     }
 
   }
 
 
   private static Boolean checkForPermission(List[] permissionGroupLists, int category, String identifier, String permissionKey, IWUserContext iwc ) throws Exception {
     Boolean myPermission = null;
     if(permissionGroupLists != null){
       int arrayLength = permissionGroupLists.length;
       switch (category) {
         case AccessController._CATEGORY_OBJECT_INSTANCE:
         case AccessController._CATEGORY_OBJECT:
         case AccessController._CATEGORY_BUNDLE :
         case AccessController._CATEGORY_PAGE_INSTANCE:
         case AccessController._CATEGORY_PAGE:
           //PageInstance
           if(category == AccessController._CATEGORY_PAGE_INSTANCE &&  !identifier.equals(Integer.toString(_notBuilderPageID)) ){
             for (int i = 0; i < arrayLength; i++) {
               myPermission = PermissionCacher.hasPermissionForPage(identifier,iwc,permissionKey,permissionGroupLists[i]);
               if(myPermission != null){
                 return myPermission;
               }
             }
 
             if(!permissionKey.equals(AccessControl._PERMISSIONKEY_OWNER)){
               // Global - (Page)
               if(!PermissionCacher.anyInstancePerissionsDefinedForPage(identifier,iwc,permissionKey)){
                 ICObject page = getStaticPageICObject();
                 if(page != null){
                   for (int i = 0; i < arrayLength; i++) {
                     myPermission = PermissionCacher.hasPermission(page,iwc,permissionKey,permissionGroupLists[i]);
                     if(myPermission != null){
                       return myPermission;
                     }
                   }
                 }
               }
               // Global - (Page)
             }
 
 
             return myPermission;
           }else{
             //instance
             for (int i = 0; i < arrayLength; i++) {
               myPermission = PermissionCacher.hasPermissionForObjectInstance(identifier,iwc,permissionKey,permissionGroupLists[i]);
               if(myPermission != null){
                 return myPermission;
               }
             }
             //instance
 
             if(!permissionKey.equals(AccessControl._PERMISSIONKEY_OWNER)){
               // Global - (object)
               if(!PermissionCacher.anyInstancePerissionsDefinedForObject(identifier,iwc,permissionKey)){
                 for (int i = 0; i < arrayLength; i++) {
                   myPermission = PermissionCacher.hasPermissionForObject(identifier,iwc,permissionKey,permissionGroupLists[i]);
                   if(myPermission != null){
                     return myPermission;
                   }
                 }
               }
               // Global - (object)
             }
 
             return myPermission;
           }
         case AccessController._CATEGORY_JSP_PAGE:
           for (int i = 0; i < arrayLength; i++) {
             myPermission = PermissionCacher.hasPermissionForJSPPage(identifier,iwc,permissionKey,permissionGroupLists[i]);
             if(myPermission != null){
               return myPermission;
             }
           }
 
           return myPermission;
         case AccessController._CATEGORY_FILE_ID:
           for (int i = 0; i < arrayLength; i++) {
             myPermission = PermissionCacher.hasPermissionForFile(identifier,iwc,permissionKey,permissionGroupLists[i]);
             if(myPermission != null){
               return myPermission;
             }
           }
 
           if(!permissionKey.equals(AccessControl._PERMISSIONKEY_OWNER)){
             // Global - (File)
             if(!PermissionCacher.anyInstancePerissionsDefinedForFile(identifier,iwc,permissionKey)){
               ICObject file = getStaticFileICObject();
               if(file != null){
                 for (int i = 0; i < arrayLength; i++) {
                   myPermission = PermissionCacher.hasPermission(file,iwc,permissionKey,permissionGroupLists[i]);
                   if(myPermission != null){
                     return myPermission;
                   }
                 }
               }
             }
             // Global - (File)
           }
 
           return myPermission;
       }
     }
     return myPermission;
   }
 
 
   public boolean hasPermission(String permissionKey, PresentationObject obj,IWUserContext iwc) throws Exception{
     Boolean myPermission = null;  // Returned if one has permission for obj instance, true or false. If no instancepermission glopalpermission is checked
 
     if (isAdmin(iwc)){
       return true;
     }
 
     User user = LoginBusiness.getUser(iwc);
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     ICPermission[] Permissions = null;
     List groups = null;
     List tempGroupList = new Vector();
     List[] permissionOrder = null; // Everyone, users, user, primaryGroup, otherGroups
 
     if (user == null){
       permissionOrder = new List[1];
       permissionOrder[0] = new Vector();
       permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
     } else {
 
       groups = LoginBusiness.getPermissionGroups(iwc);
       GenericGroup primaryGroup = LoginBusiness.getPrimaryGroup(iwc);
 
       if (groups != null && groups.size() > 0){
         if(primaryGroup != null){
           groups.remove(primaryGroup);
         }
         List groupIds = new Vector();
         Iterator iter = groups.iterator();
         while (iter.hasNext()) {
           groupIds.add(Integer.toString(((GenericGroup)iter.next()).getID()));
         }
         permissionOrder = new List[5];
         permissionOrder[4] = groupIds;
       } else {
         permissionOrder = new List[4];
       }
         permissionOrder[0] = new Vector();
         permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
         permissionOrder[1] = new Vector();
         permissionOrder[1].add( Integer.toString(getPermissionGroupUsers().getID()) );
         permissionOrder[2] = new Vector();
         permissionOrder[2].add( Integer.toString(user.getGroupID()) );
         permissionOrder[3] = new Vector();
         permissionOrder[3].add( Integer.toString(user.getPrimaryGroupID()) );
         // Everyone, user, primaryGroup, otherGroups
     }
     myPermission = checkForPermission(permissionOrder, obj, permissionKey, iwc);
     if(myPermission != null){
       return myPermission.booleanValue();
     }
 
     if(permissionKey.equals(AccessControl._PERMISSIONKEY_EDIT) || permissionKey.equals(AccessControl._PERMISSIONKEY_VIEW)){
       return isOwner(obj,iwc);
     } else {
       return false;
     }
 
   } // method hasPermission
 
 
 
   public boolean hasPermission(List groupIds,String permissionKey, PresentationObject obj,IWUserContext iwc) throws Exception{
     Boolean myPermission = null;  // Returned if one has permission for obj instance, true or false. If no instancepermission glopalpermission is checked
 
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     ICPermission[] Permissions = null;
     List groups = null;
     List[] permissionOrder = null; // Everyone, users, (primaryGroup), otherGroups
 
     if(groupIds != null){
       if (groupIds.contains(Integer.toString(getPermissionGroupAdministrator().getID()))){
         return true;
       } else {
         if(groupIds.size() == 1){
           if(groupIds.get(0).equals(Integer.toString(_GROUP_ID_EVERYONE))){
             permissionOrder = new List[1];
             permissionOrder[0] = new Vector();
             permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
           }else {
             if(groupIds.get(0).equals(Integer.toString(_GROUP_ID_USERS))){
               permissionOrder = new List[2];
             } else{
               permissionOrder = new List[3];
               permissionOrder[2] = groupIds;
             }
               permissionOrder[0] = new Vector();
               permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
               permissionOrder[1] = new Vector();
               permissionOrder[1].add( Integer.toString(getPermissionGroupUsers().getID()) );
           }
         } else if (groupIds.size() > 1){
             permissionOrder = new List[3];
             permissionOrder[0] = new Vector();
             permissionOrder[0].add( Integer.toString(getPermissionGroupEveryOne().getID()) );
             permissionOrder[1] = new Vector();
             permissionOrder[1].add( Integer.toString(getPermissionGroupUsers().getID()) );
             permissionOrder[2] = groupIds;
             // Everyone, users, (primaryGroup), otherGroups
         } else {
           return false;
         }
       }
     } else{
         return false;
     }
     myPermission = checkForPermission(permissionOrder, obj, permissionKey, iwc);
     if(myPermission != null){
       return myPermission.booleanValue();
     }
 
 
 
     if(permissionKey.equals(AccessControl._PERMISSIONKEY_EDIT) || permissionKey.equals(AccessControl._PERMISSIONKEY_VIEW)){
       return isOwner(groupIds,obj,iwc);
     } else {
       return false;
     }
 
   } // method hasPermission
 
 
 
 
   private static Boolean checkForPermission(List[] permissionGroupLists, PresentationObject obj, String permissionKey, IWUserContext iwc ) throws Exception {
     Boolean myPermission = null;
     if(permissionGroupLists != null){
       int arrayLength = permissionGroupLists.length;
       if (obj == null){ // JSP page
         for (int i = 0; i < arrayLength; i++) {
           myPermission = PermissionCacher.hasPermissionForJSPPage(obj,iwc,permissionKey,permissionGroupLists[i]);
           if(myPermission != null){
             return myPermission;
           }
         }
 
         return myPermission;
       } else { // if (obj != null)
 
         if(obj instanceof Page && ((Page)obj).getPageID() != _notBuilderPageID ){
           for (int i = 0; i < arrayLength; i++) {
             myPermission = PermissionCacher.hasPermissionForPage(obj,iwc,permissionKey,permissionGroupLists[i]);
             if(myPermission != null){
               return myPermission;
             }
           }
 
           if(!permissionKey.equals(AccessControl._PERMISSIONKEY_OWNER)){
             // Global - (Page)
             if(!PermissionCacher.anyInstancePerissionsDefinedForPage(obj,iwc,permissionKey)){
               ICObject page = getStaticPageICObject();
               if(page != null){
                 for (int i = 0; i < arrayLength; i++) {
                   myPermission = PermissionCacher.hasPermission(page,iwc,permissionKey,permissionGroupLists[i]);
                   if(myPermission != null){
                     return myPermission;
                   }
                 }
               }
             }
             // Global - (Page)
           }
 
 
           return myPermission;
         }else{
           //instance
           for (int i = 0; i < arrayLength; i++) {
             myPermission = PermissionCacher.hasPermissionForObjectInstance(obj,iwc,permissionKey,permissionGroupLists[i]);
             if(myPermission != null){
               return myPermission;
             }
           }
 
           //instance
 /*
           //page permission inheritance
           if(obj.allowPagePermissionInheritance()){
             Page p = obj.getParentPage();
             if(p != null && p.getPageID() != _notBuilderPageID ){
               myPermission = checkForPermission(permissionGroupLists,p,permissionType,iwc);
               if(myPermission != null){
                 return myPermission;
               }
             }
           }
           //page permission inheritance
 */
           if(!permissionKey.equals(AccessControl._PERMISSIONKEY_OWNER)){
             // Global - (object)
             if(!PermissionCacher.anyInstancePerissionsDefinedForObject(obj,iwc,permissionKey)){
               for (int i = 0; i < arrayLength; i++) {
                 myPermission = PermissionCacher.hasPermissionForObject(obj,iwc,permissionKey,permissionGroupLists[i]);
                 if(myPermission != null){
                   return myPermission;
                 }
               }
             }
             // Global - (object)
           }
 
           return myPermission;
         }
       }
     }
     return myPermission;
   }
 
 
   //temp
   private static ICObject getStaticPageICObject(){
     if(staticPageICObject == null){
       try {
         staticPageICObject = (ICObject)EntityFinder.findAllByColumn((ICObject)com.idega.core.data.ICObjectBMPBean.getStaticInstance(ICObject.class),com.idega.core.data.ICObjectBMPBean.getClassNameColumnName(),Page.class.getName()).get(0);
       }
       catch (Exception ex) {
         ex.printStackTrace();
       }
     }
     return staticPageICObject;
   }
 
   //temp
   private static ICObject getStaticFileICObject(){
     if(staticFileICObject == null){
       try {
         staticPageICObject = (ICObject)EntityFinder.findAllByColumn((ICObject)com.idega.core.data.ICObjectBMPBean.getStaticInstance(ICObject.class),com.idega.core.data.ICObjectBMPBean.getClassNameColumnName(),ICFile.class.getName()).get(0);
       }
       catch (Exception ex) {
         ex.printStackTrace();
       }
     }
     return staticFileICObject;
   }
 
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getObjectInstanceIdString(){
 //    return "ic_object_instance_id";
 //  }
 //
 //
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getObjectIdString(){
 //    return "ic_object_id";
 //  }
 //
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getBundleIdentifierString(){
 //    return "iw_bundle_identifier";
 //  }
 //
 //
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getPageIdString(){
 //    return "page_id";
 //  }
 //
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getPageString(){
 //    return "page";
 //  }
 //
 //  /**
 //   * use this method when writing to database to avoid errors in database.
 //   * If the name-string changes this will be the only method to change.
 //   */
 //  public static String getJSPPageString(){
 //    return "jsp_page";
 //  }
 //
 
 
   public boolean hasEditPermission(PresentationObject obj,IWUserContext iwc)throws Exception{
     return hasPermission( _PERMISSIONKEY_EDIT , obj, iwc);
   }
 
 
   public boolean hasViewPermission(PresentationObject obj,IWUserContext iwc){
     try {
       /*boolean permission = hasPermission( _PERMISSIONKEY_VIEW, obj, iwc);
       System.err.println(obj.getClass().getName()+" has permission: " + permission);
       return permission;
       */
       return hasPermission( _PERMISSIONKEY_VIEW, obj, iwc);
     }
     catch (Exception ex) {
       return false;
     }
   }
 
   public boolean hasViewPermission(List groupIds, PresentationObject obj,IWUserContext iwc){
     try {
       /*boolean permission = hasPermission( _PERMISSIONKEY_VIEW, obj, iwc);
       System.err.println(obj.getClass().getName()+" has permission: " + permission);
       return permission;
       */
       return hasPermission(groupIds, _PERMISSIONKEY_VIEW, obj, iwc);
     }
     catch (Exception ex) {
       return false;
     }
   }
 
 
   public boolean hasAdminPermission(PresentationObject obj,IWUserContext iwc)throws Exception{
     return hasPermission( _PERMISSIONKEY_ADMIN, obj, iwc);
   }
 
   public boolean hasOwnerPermission(PresentationObject obj,IWUserContext iwc)throws Exception{
     return hasPermission( _PERMISSIONKEY_OWNER, obj, iwc);
   }
 
 /*  public static ICObjectPermission[] getPermissionTypes(PresentationObject obj)throws Exception{
     int arobjID = obj.getICObject().getID();
     List permissions =  EntityFinder.findAllByColumn(com.idega.core.accesscontrol.data.ICObjectPermissionBMPBean.getStaticInstance(), com.idega.core.accesscontrol.data.ICObjectPermissionBMPBean.getPermissionTypeColumnName(), arobjID);
     if (permissions != null){
       return (ICObjectPermission[])permissions.toArray((Object[])new ICObjectPermission[0]);
     }else{
       return null;
     }
   }
 */
 
 
   public void setJSPPagePermission(IWUserContext iwc, PermissionGroup group, String PageContextValue, String permissionType, Boolean permissionValue)throws Exception{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     boolean update = true;
     try {
       permission = (ICPermission)(permission.findAll("SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_JSP_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + PageContextValue + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionType + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + group.getID()))[0];
     }
     catch (Exception ex) {
       permission = getPermissionHome().create();
       update = false;
     }
 
     if(!update){
       permission.setContextType(AccessControl._CATEYGORYSTRING_JSP_PAGE);
       // use 'ICJspHandler.getJspPageInstanceID(iwc)' on the current page and send in as PageContextValue
       permission.setContextValue(PageContextValue);
       permission.setGroupID(new Integer(group.getID()));
       permission.setPermissionString(permissionType);
 //        permission.setPermissionStringValue();
       permission.setPermissionValue(permissionValue);
       permission.insert();
     } else{
       permission.setPermissionValue(permissionValue);
       permission.update();
     }
     PermissionCacher.updateJSPPagePermissions(PageContextValue,permissionType,iwc);
   }
 
   public void setObjectPermission(IWUserContext iwc, PermissionGroup group, PresentationObject obj, String permissionType, Boolean permissionValue)throws Exception{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     boolean update = true;
     try {
       permission = (ICPermission)(permission.findAll("SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + obj.getICObjectID() + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionType + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + group.getID()))[0];
     }
     catch (Exception ex) {
       permission = getPermissionHome().create();
       update = false;
     }
 
     if(!update){
       permission.setContextType(AccessControl._CATEYGORYSTRING_OBJECT_ID);
       permission.setContextValue(Integer.toString(obj.getICObjectID()));
       permission.setGroupID(new Integer(group.getID()));
       permission.setPermissionString(permissionType);
 //        permission.setPermissionStringValue();
       permission.setPermissionValue(permissionValue);
       permission.insert();
     } else{
       permission.setPermissionValue(permissionValue);
       permission.update();
     }
     PermissionCacher.updateObjectPermissions(Integer.toString(obj.getICObjectID()),permissionType,iwc);
   }
 
 
   public void setBundlePermission(IWUserContext iwc, PermissionGroup group, PresentationObject obj, String permissionType, Boolean permissionValue)throws Exception{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     boolean update = true;
     try {
       permission = (ICPermission)(permission.findAll("SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_BUNDLE_IDENTIFIER + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + obj.getBundleIdentifier() + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionType + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + group.getID()))[0];
     }
     catch (Exception ex) {
       permission = getPermissionHome().create();
       update = false;
     }
 
     if(!update){
       permission.setContextType(AccessControl._CATEYGORYSTRING_BUNDLE_IDENTIFIER);
       permission.setContextValue(obj.getBundleIdentifier());
       permission.setGroupID(new Integer(group.getID()));
       permission.setPermissionString(permissionType);
   //        permission.setPermissionStringValue();
       permission.setPermissionValue(permissionValue);
       permission.insert();
     } else{
       permission.setPermissionValue(permissionValue);
       permission.update();
     }
     PermissionCacher.updateBundlePermissions(obj.getBundleIdentifier(),permissionType,iwc);
   }
 
 
 
   public void setObjectInstacePermission(IWUserContext iwc, PermissionGroup group, PresentationObject obj, String permissionType, Boolean permissionValue)throws Exception{
     setObjectInstacePermission(iwc,Integer.toString(group.getID()),Integer.toString(obj.getICObjectInstance().getID()),permissionType,permissionValue);
   }
 
   public static boolean removeICObjectInstancePermissionRecords(IWUserContext iwc, String ObjectInstanceId, String permissionKey, String[] groupsToRemove){
     String sGroupList = "";
     if (groupsToRemove != null && groupsToRemove.length > 0){
       for(int g = 0; g < groupsToRemove.length; g++){
         if(g>0){ sGroupList += ", "; }
         sGroupList += groupsToRemove[g];
       }
     }
     if(!sGroupList.equals("")){
       ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
       try {
         boolean done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_INSTATNCE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = " + ObjectInstanceId + " AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
         if(done){
           PermissionCacher.updateObjectInstancePermissions(ObjectInstanceId,permissionKey,iwc);
         }
         return done;
       }
       catch (Exception ex) {
         return false;
       }
     } else {
       return true;
     }
 
   }
 
 
   public static boolean removePermissionRecords(int permissionCategory, IWUserContext iwc, String identifier, String permissionKey, String[] groupsToRemove){
     String sGroupList = "";
     if (groupsToRemove != null && groupsToRemove.length > 0){
       for(int g = 0; g < groupsToRemove.length; g++){
         if(g>0){ sGroupList += ", "; }
         sGroupList += groupsToRemove[g];
       }
     }
     if(!sGroupList.equals("")){
       ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
       try {
         boolean done = false;
 
         switch (permissionCategory) {
           case AccessControl._CATEGORY_OBJECT_INSTANCE :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_INSTATNCE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
           case AccessControl._CATEGORY_OBJECT :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
           case AccessControl._CATEGORY_BUNDLE :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_BUNDLE_IDENTIFIER + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
           case AccessControl._CATEGORY_PAGE_INSTANCE :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
           case AccessControl._CATEGORY_PAGE :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
           case AccessControl._CATEGORY_JSP_PAGE :
             done = SimpleQuerier.execute("DELETE FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_JSP_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " IN (" + sGroupList + ")" );
             break;
         }
 
         PermissionCacher.updatePermissions(permissionCategory,identifier,permissionKey,iwc);
 
         return true;
       }
       catch (Exception ex) {
         ex.printStackTrace();
         return false;
       }
     } else {
       return true;
     }
 
   }
 
 
 
   public void setPermission(int permissionCategory, IWUserContext iwc, String permissionGroupId, String identifier, String permissionKey, Boolean permissionValue)throws Exception{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     boolean update = true;
     try {
       switch (permissionCategory) {
         case AccessControl._CATEGORY_OBJECT_INSTANCE :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_INSTATNCE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_OBJECT :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_BUNDLE :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_BUNDLE_IDENTIFIER + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_PAGE_INSTANCE :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_PAGE :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_JSP_PAGE :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_JSP_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
         case AccessControl._CATEGORY_FILE_ID :
           permission = (ICPermission)EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_FILE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId).get(0);
           break;
 
       }
 
     }
     catch (Exception ex) {
       permission = getPermissionHome().create();
       update = false;
     }
 
     if(!update){
 
       switch (permissionCategory) {
         case AccessControl._CATEGORY_OBJECT_INSTANCE :
           permission.setContextType(AccessControl._CATEYGORYSTRING_OBJECT_INSTATNCE_ID);
           break;
         case AccessControl._CATEGORY_OBJECT :
           permission.setContextType(AccessControl._CATEYGORYSTRING_OBJECT_ID);
           break;
         case AccessControl._CATEGORY_BUNDLE :
           permission.setContextType(AccessControl._CATEYGORYSTRING_BUNDLE_IDENTIFIER);
           break;
         case AccessControl._CATEGORY_PAGE_INSTANCE :
           permission.setContextType(AccessControl._CATEYGORYSTRING_PAGE_ID);
           break;
         case AccessControl._CATEGORY_PAGE :
           permission.setContextType(AccessControl._CATEYGORYSTRING_PAGE);
           break;
         case AccessControl._CATEGORY_JSP_PAGE :
           permission.setContextType(AccessControl._CATEYGORYSTRING_JSP_PAGE);
           break;
         case AccessControl._CATEGORY_FILE_ID :
           permission.setContextType(AccessControl._CATEYGORYSTRING_FILE_ID);
           break;
       }
 
       permission.setContextValue(identifier);
       permission.setGroupID(new Integer(permissionGroupId));
       permission.setPermissionString(permissionKey);
 //        permission.setPermissionStringValue();
       permission.setPermissionValue(permissionValue);
       permission.insert();
     } else{
       permission.setPermissionValue(permissionValue);
       permission.update();
     }
     PermissionCacher.updatePermissions(permissionCategory, identifier, permissionKey, iwc);
   }
 
 
   public void setObjectInstacePermission(IWUserContext iwc, String permissionGroupId, String ObjectInstanceId, String permissionType, Boolean permissionValue)throws Exception{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     boolean update = true;
     try {
       permission = (ICPermission)(permission.findAll("SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_INSTATNCE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = " + ObjectInstanceId + " AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionType + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + permissionGroupId))[0];
     }
     catch (Exception ex) {
       permission = getPermissionHome().create();
       update = false;
     }
 
     if(!update){
       permission.setContextType(AccessControl._CATEYGORYSTRING_OBJECT_INSTATNCE_ID);
       permission.setContextValue(ObjectInstanceId);
       permission.setGroupID(new Integer(permissionGroupId));
       permission.setPermissionString(permissionType);
 //        permission.setPermissionStringValue();
       permission.setPermissionValue(permissionValue);
       permission.insert();
     } else{
       permission.setPermissionValue(permissionValue);
       permission.update();
     }
     PermissionCacher.updateObjectInstancePermissions(ObjectInstanceId,permissionType,iwc);
   }
 
 
 
 
 
   public int createPermissionGroup(String GroupName, String Description, String ExtraInfo, int[] userIDs, int[] groupIDs)throws Exception{
     PermissionGroup newGroup = getPermissionGroupHome().create();
 
     if(GroupName != null)
       newGroup.setName(GroupName);
 
     if(Description != null)
       newGroup.setDescription(Description);
 
     if(ExtraInfo != null)
       newGroup.setExtraInfo(ExtraInfo);
 
     newGroup.insert();
 
     int newGroupID = newGroup.getID();
 
     if(userIDs != null){
       for (int i = 0; i < userIDs.length; i++) {
         addUserToPermissionGroup(newGroup, userIDs[i]);
       }
     }
     if (groupIDs != null){
       for (int j = 0; j < groupIDs.length; j++) {
         addGroupToPermissionGroup(newGroup, groupIDs[j]);
       }
     }
 
     return newGroupID;
 
   }
 
   public static void addUserToPermissionGroup(PermissionGroup group, int userIDtoAdd) throws Exception{
     User userToAdd = ((com.idega.core.user.data.UserHome)com.idega.data.IDOLookup.getHomeLegacy(User.class)).findByPrimaryKeyLegacy(userIDtoAdd);
     group.addUser(userToAdd);
   }
 
 
   public static void addGroupToPermissionGroup(PermissionGroup group, int groupIDtoAdd)throws Exception{
     GenericGroup groupToAdd = ((com.idega.core.data.GenericGroupHome)com.idega.data.IDOLookup.getHomeLegacy(GenericGroup.class)).findByPrimaryKeyLegacy(groupIDtoAdd);
     group.addGroup(groupToAdd);
   }
 
 
   /**
    * @todo implement filter to get grouptypes from property file
    */
   private static String[] getPermissionGroupFilter(){
     //filter begin
     String[] groupsToReturn = new String[2];
     groupsToReturn[0] = com.idega.core.accesscontrol.data.PermissionGroupBMPBean.getStaticPermissionGroupInstance().getGroupTypeValue();
     groupsToReturn[1] = com.idega.builder.dynamicpagetrigger.data.DPTPermissionGroupBMPBean.getStaticGroupInstance().getGroupTypeValue();
 /*
     String[] groupsToReturn = new String[1];
     groupsToReturn[0] = com.idega.core.accesscontrol.data.PermissionGroupBMPBean.getStaticPermissionGroupInstance().getGroupTypeValue();
 */
     //filter end
     return groupsToReturn;
   }
 
   public static List getPermissionGroups(User user) throws Exception{
     //temp - ((com.idega.core.data.GenericGroupHome)com.idega.data.IDOLookup.getHomeLegacy(GenericGroup.class)).createLegacy()
     int groupId = user.getGroupID();
     if(groupId != -1){
       return getPermissionGroups(((com.idega.core.data.GenericGroupHome)com.idega.data.IDOLookup.getHomeLegacy(GenericGroup.class)).findByPrimaryKeyLegacy(groupId));
     }else{
       return null;
     }
   }
 
   public static List getPermissionGroups(GenericGroup group) throws Exception{
     List permissionGroups = UserGroupBusiness.getGroupsContaining(group,getPermissionGroupFilter(),true);
 
     if(permissionGroups != null){
       return permissionGroups;
     }else {
       return null;
     }
   }
 
   public List getAllowedGroups(int permissionCategory, String identifier, String permissionKey) throws Exception {
     List toReturn = new Vector(0);
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     List permissions = null;
 
     switch (permissionCategory) {
       case AccessControl._CATEGORY_OBJECT_INSTANCE :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_INSTATNCE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
       case AccessControl._CATEGORY_OBJECT :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
       case AccessControl._CATEGORY_BUNDLE :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_BUNDLE_IDENTIFIER + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
       case AccessControl._CATEGORY_PAGE_INSTANCE :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
       case AccessControl._CATEGORY_PAGE :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
       case AccessControl._CATEGORY_JSP_PAGE :
         permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_JSP_PAGE + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + identifier + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionKey +"' AND "+com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionValueColumnName() +" = 'Y'");
         break;
     }
 
     if (permissions != null){
       Iterator iter = permissions.iterator();
       while (iter.hasNext()) {
         Object item = iter.next();
         try {
           toReturn.add(((com.idega.core.accesscontrol.data.PermissionGroupHome)com.idega.data.IDOLookup.getHomeLegacy(PermissionGroup.class)).findByPrimaryKeyLegacy(((ICPermission)item).getGroupID()));
         }
         catch (Exception ex) {
           System.err.println("Accesscontrol.getAllowedGroups(): Group not created for id "+((ICPermission)item).getGroupID());
         }
 
       }
     }
     toReturn.remove(AdministratorPermissionGroup);
     return toReturn;
   }
 
 
   public List getAllPermissionGroups()throws Exception {
 
     List permissionGroups = com.idega.core.data.GenericGroupBMPBean.getAllGroups(getPermissionGroupFilter(),true);
     if(permissionGroups != null){
       permissionGroups.remove(getPermissionGroupAdministrator());
     }
 
     return permissionGroups;
   }
 
 
   public List getStandardGroups() throws Exception {
     if(standardGroups == null){
       initStandardGroups();
     }
     return standardGroups;
   }
 
   private void initStandardGroups() throws Exception {
     standardGroups = new Vector();
     //standardGroups.add(AccessControl.getPermissionGroupAdministrator());
     standardGroups.add(this.getPermissionGroupEveryOne());
     standardGroups.add(this.getPermissionGroupUsers());
   }
 
 
   public User getAdministratorUser() throws Exception {
     Object ob = getApplication().getAttribute(_APPADDRESS_ADMINISTRATOR_USER);
     if(ob == null){
       try {
         initAdministratorUser();
         return (User)getApplication().getAttribute(_APPADDRESS_ADMINISTRATOR_USER);
       }
       catch (Exception ex) {
         ex.printStackTrace();
         return null;
       }
 
 
     }else{
       return (User)ob;
     }
   }
 
   private User createAdministratorUser()throws Exception{
     User adminUser = ((com.idega.core.user.data.UserHome)com.idega.data.IDOLookup.getHomeLegacy(User.class)).createLegacy();
     adminUser.setColumn(com.idega.core.user.data.UserBMPBean.getColumnNameFirstName(),_ADMINISTRATOR_NAME);
     adminUser.insert();
 
     UserGroupRepresentative ugr = ((com.idega.core.user.data.UserGroupRepresentativeHome)com.idega.data.IDOLookup.getHomeLegacy(UserGroupRepresentative.class)).createLegacy();
     ugr.setName("admin");
     ugr.insert();
 
     adminUser.setGroupID(ugr.getID());
     adminUser.setPrimaryGroupID(this.getPermissionGroupAdministrator().getID());
     adminUser.update();
 
    LoginDBHandler.createLogin(adminUser.getID(),"Administrator","idega",Boolean.TRUE,IWTimestamp.RightNow(),-1,Boolean.FALSE,Boolean.TRUE,Boolean.FALSE,EncryptionType.MD5);
     return adminUser;
   }
 
   private void initAdministratorUser() throws Exception{
     List list = EntityFinder.findAllByColumn(com.idega.core.user.data.UserBMPBean.getStaticInstance(),com.idega.core.user.data.UserBMPBean.getColumnNameFirstName(),_ADMINISTRATOR_NAME);
     User adminUser = null;
     if(list == null || list.size() < 1){
       adminUser = createAdministratorUser();
     } else {
       adminUser = (User)list.get(0);
     }
     getApplication().setAttribute(_APPADDRESS_ADMINISTRATOR_USER,adminUser);
   }
 
   public void executeService(){
 
     try {
       PermissionGroup permission = com.idega.core.accesscontrol.data.PermissionGroupBMPBean.getStaticPermissionGroupInstance();
       List groups = EntityFinder.findAllByColumn(permission,com.idega.core.accesscontrol.data.PermissionGroupBMPBean.getGroupTypeColumnName(),permission.getGroupTypeValue());
       if(groups != null){
         Iterator iter = groups.iterator();
         while (iter.hasNext()) {
           Object item = iter.next();
           if(getAdministratorGroupName().equals (((GenericGroup)item).getName())){
             AdministratorPermissionGroup = (PermissionGroup)item;
           }
         }
       }
       if(AdministratorPermissionGroup == null){
         initAdministratorPermissionGroup();
       }
     }
     catch (Exception ex) {
       System.err.println("AccessControl: PermissionGroup administrator not initialized");
       ex.printStackTrace();
     }
 
     try {
       PermissionGroupEveryOne = ((com.idega.core.accesscontrol.data.PermissionGroupHome)com.idega.data.IDOLookup.getHomeLegacy(PermissionGroup.class)).findByPrimaryKeyLegacy(_GROUP_ID_EVERYONE);
     }
     catch (Exception e) {
       try {
         initPermissionGroupEveryone();
       }
       catch (Exception ex) {
         System.err.println("AccessControl: PermissionGroup Everyone not initialized");
       }
     }
 
     try {
       PermissionGroupUsers = ((com.idega.core.accesscontrol.data.PermissionGroupHome)com.idega.data.IDOLookup.getHomeLegacy(PermissionGroup.class)).findByPrimaryKeyLegacy(_GROUP_ID_USERS);
     }
     catch (Exception e) {
       try {
         initPermissionGroupUsers();
       }
       catch (Exception ex) {
         System.err.println("AccessControl: PermissionGroup Users not initialized");
       }
     }
 
     try {
       initAdministratorUser();
     }
     catch (Exception ex) {
       System.err.println("AccessControl: User Administrator not initialized");
       ex.printStackTrace();
     }
 
   }
 
   public String getServiceName(){
     return "AccessControl";
   }
 
   public static boolean isValidUsersFirstName(String name){
     return !_ADMINISTRATOR_NAME.equals(name);
   }
 
 
 
   public String[] getICObjectPermissionKeys(Class ICObject){
     String[] keys = new String[2];
 
     keys[0] = _PERMISSIONKEY_VIEW;
     keys[1] = _PERMISSIONKEY_EDIT;
     //keys[2] = _PERMISSIONKEY_DELETE;
 
     return keys;
 
     // return new String[0]; // not null
   }
 
 
   public String[] getBundlePermissionKeys(Class ICObject){
     String[] keys = new String[2];
 
     keys[0] = _PERMISSIONKEY_VIEW;
     keys[1] = _PERMISSIONKEY_EDIT;
     //keys[2] = _PERMISSIONKEY_DELETE;
 
     return keys;
 
     // return new String[0]; // not null
   }
 
   public String[] getBundlePermissionKeys(String BundleIdentifier){
     String[] keys = new String[2];
 
     keys[0] = _PERMISSIONKEY_VIEW;
     keys[1] = _PERMISSIONKEY_EDIT;
     //keys[2] = _PERMISSIONKEY_DELETE;
 
     return keys;
 
     // return new String[0]; // not null
   }
 
   public String[] getPagePermissionKeys(){
     String[] keys = new String[2];
 
     keys[0] = _PERMISSIONKEY_VIEW;
     keys[1] = _PERMISSIONKEY_EDIT;
     //keys[2] = _PERMISSIONKEY_DELETE;
 
     return keys;
 
     // return new String[0]; // not null
   }
 
 
 
 
 
 
   public static void initICObjectPermissions(ICObject obj) throws Exception{
 
     ICPermission permission = ((com.idega.core.accesscontrol.data.ICPermissionHome)com.idega.data.IDOLookup.getHomeLegacy(ICPermission.class)).createLegacy();
     /*
     boolean update = true;
     try {
       permission = (ICPermission)(permission.findAll("SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + _CATEYGORYSTRING_OBJECT_ID + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " = '" + obj.getICObjectID(iwc) + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getPermissionStringColumnName() + " = '" + permissionType + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + group.getID()))[0];
     }
     catch (Exception ex) {
       permission = ((com.idega.core.accesscontrol.data.ICPermissionHome)com.idega.data.IDOLookup.getHomeLegacy(ICPermission.class)).createLegacy();
       update = false;
     }*/
 
     permission.setContextType(AccessControl._CATEYGORYSTRING_OBJECT_ID);
     permission.setContextValue(Integer.toString(obj.getID()));
     permission.setGroupID(new Integer(AccessControl._GROUP_ID_EVERYONE));
     permission.setPermissionString(AccessControl._PERMISSIONKEY_VIEW);
 //        permission.setPermissionStringValue();
     permission.setPermissionValue(Boolean.TRUE);
     permission.insert();
 
     //PermissionCacher.updateObjectPermissions(Integer.toString(obj.getICObjectID(iwc)),permissionType,iwc);
 
 
 
 
   }
 
 
 
 
   /**
    * @todo implement hasPermission(String permissionKey, ICObject obj, IWUserContext iwc) throws Exception
    * temp implementation
    */
   public boolean hasPermission(String permissionKey, ICObject obj, IWUserContext iwc) throws Exception{
     PresentationObject pObj = (PresentationObject)Class.forName(obj.getClassName()).newInstance();
     pObj.setICObject(obj);
     return this.hasPermission(permissionKey,(PresentationObject)pObj,iwc);
   }
 
   /**
    * @todo implement hasFilePermission(String permissionKey, int id, IWUserContext iwc)throws Exception
    */
   public boolean hasFilePermission(String permissionKey, int id, IWUserContext iwc)throws Exception{
     return true;
   }
 
   /**
    * @todo implement hasDataPermission(String permissionKey, ICObject obj, int entityRecordId, IWUserContext iwc)
    */
   public boolean hasDataPermission(String permissionKey, ICObject obj, int entityRecordId, IWUserContext iwc) throws Exception{
     return true;
   }
 
 
 
 /*
   public boolean hasPermission(Class someClass, int id, IWUserContext iwc) throws Exception{
     if(someClass.equals(ICFile.class)){
       return true;
     }else if(someClass.equals(ICObject.class)){
       return true;
     }else {
       return true;
     }
   }
 */
 
 
 
   public void setCurrentUserAsOwner(IBPage page, IWUserContext iwc)throws Exception {
     User user = iwc.getUser();
 //    System.out.println("User = "+ user);
     if(user != null){
       int groupId = -1;
       groupId = user.getPrimaryGroupID();
       if(groupId == -1){
         groupId = user.getGroupID();
       }
 //      System.out.println("Group = "+ groupId);
       if(groupId != -1){
           setAsOwner(page,groupId,iwc);
 //        setPermission(AccessController._CATEGORY_PAGE,iwc,Integer.toString(groupId),Integer.toString(page.getID()),AccessControl._PERMISSIONKEY_EDIT,Boolean.TRUE);
 //        setPermission(AccessController._CATEGORY_PAGE,iwc,Integer.toString(groupId),Integer.toString(page.getID()),AccessControl._PERMISSIONKEY_VIEW,Boolean.TRUE);
       } else {
         // return false;
       }
     } else {
       // return false;
     }
   }
 
   /**
    * @todo implement setAsOwner(ICFile file, IWUserContext iwc)throws Exception
    */
   public void setAsOwner(IBPage page, int groupId, IWUserContext iwc)throws Exception {
     setPermission(AccessController._CATEGORY_PAGE_INSTANCE,iwc,Integer.toString(groupId),Integer.toString(page.getID()),AccessControl._PERMISSIONKEY_OWNER,Boolean.TRUE);
   }
 
 
   /**
    * @todo implement setAsOwner(PresentationObject obj , IWUserContext iwc) throws Exception
    */
   public void setAsOwner(PresentationObject obj, int groupId, IWUserContext iwc) throws Exception {}
 
   /**
    * @todo implement setAsOwner(ICFile file, IWUserContext iwc)throws Exception
    */
   public void setAsOwner(ICFile file, int groupId, IWUserContext iwc)throws Exception {
     setPermission(AccessController._CATEGORY_FILE_ID,iwc,Integer.toString(groupId),Integer.toString(file.getID()),AccessControl._PERMISSIONKEY_OWNER,Boolean.TRUE);
   }
 
   /**
    * @todo implement setAsOwner(ICObject obj, int entityRecordId, IWUserContext iwc)throws Exception
    */
   public void setAsOwner(ICObject obj, int entityRecordId, int groupId, IWUserContext iwc)throws Exception {
     throw new Exception(this.getClass().getName()+".setAsOwner(...) : not implemented");
   }
 
 
 
 
   public static void copyObjectInstancePermissions( String idToCopyFrom, String idToCopyTo) throws SQLException{
     copyPermissions(AccessController._CATEYGORYSTRING_OBJECT_INSTATNCE_ID,idToCopyFrom,idToCopyTo);
   }
 
   public static void copyPagePermissions( String idToCopyFrom, String idToCopyTo) throws SQLException{
     copyPermissions(AccessController._CATEYGORYSTRING_PAGE_ID,idToCopyFrom,idToCopyTo);
   }
 
   public static List getGroupsPermissions(String category, GenericGroup group, Set identifiers) throws SQLException{
     ICPermission permission = com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance();
     List permissions = null;
     String instanceIds = "";
     if(identifiers != null){
       Iterator iter = identifiers.iterator();
       boolean first = true;
       while (iter.hasNext()) {
         if(!first){
           instanceIds += ",";
         }
         instanceIds += "'"+(String)iter.next()+"'";
         first = false;
       }
     }
     String SQLString = null;
     if(!instanceIds.equals("")){
       StringBuffer buffer = new StringBuffer();
       buffer.append("SELECT * FROM ");
       buffer.append(permission.getEntityName());
       buffer.append(" WHERE ");
       buffer.append(com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName());
       buffer.append(" = '");
       buffer.append(category);
       buffer.append("' AND ");
       buffer.append(com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName());
       if(identifiers.size() > 1){
         buffer.append(" in(");
         buffer.append(instanceIds);
         buffer.append(")");
       } else {
         buffer.append(" = '");
         buffer.append(instanceIds);
         buffer.append("'");
       }
       buffer.append(" AND ");
       buffer.append(com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName());
       buffer.append(" = ");
       buffer.append(group.getID());
 
 
       SQLString = buffer.toString();
 
       if(SQLString != null){
         permissions = EntityFinder.findAll(permission,SQLString);
       }
 
       //permissions = EntityFinder.findAll(permission,"SELECT * FROM " + permission.getEntityName() + " WHERE " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName() + " = '" + category + "' AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName() + " in(" + instanceIds + ") AND " + com.idega.core.accesscontrol.data.ICPermissionBMPBean.getGroupIDColumnName() + " = " + group.getID());
 
     }
     //System.err.println(SQLString);
     //System.err.println(" = " + permissions);
     return permissions;
   }
 
   public static List getGroupsPermissionsForInstances(GenericGroup group, Set instances) throws SQLException{
     return getGroupsPermissions(AccessController._CATEYGORYSTRING_OBJECT_INSTATNCE_ID,group, instances);
   }
 
   public static List getGroupsPermissionsForPages(GenericGroup group, Set instances) throws SQLException{
     return getGroupsPermissions(AccessController._CATEYGORYSTRING_PAGE_ID,group, instances);
   }
 
   public static boolean replicatePermissionForNewGroup(ICPermission permission, GenericGroup group){
     try {
       ICPermission p = ((com.idega.core.accesscontrol.data.ICPermissionHome)com.idega.data.IDOLookup.getHomeLegacy(ICPermission.class)).createLegacy();
 
       String s = permission.getContextType();
       if(s != null){
         p.setContextType(s);
       }
 
       String s2 = permission.getContextValue();
       if(s2 != null){
         p.setContextValue(s2);
       }
 
       String s3 = permission.getPermissionString();
       if(s3 != null){
         p.setPermissionString(s3);
       }
 
       String s4 = permission.getPermissionStringValue();
       if(s4 != null){
         p.setPermissionStringValue(s4);
       }
 
       p.setPermissionValue(permission.getPermissionValue());
 
       // groupID changes
       p.setGroupID(group.getID());
 
       p.insert();
 
       //PermissionCacher.updatePermissions(,p.getContextValue(),permissionType,iwc);
       return true;
     }
     catch (Exception ex) {
       System.err.println("AccessControl.replicatePermissionForNewGroup(..) did not succeed");
       return false;
     }
 
   }
 
   public static void copyPermissions( String contextType, String identifierToCopyFrom, String identifierToCopyTo) throws SQLException{
     List permissions = EntityFinder.findAllByColumn(com.idega.core.accesscontrol.data.ICPermissionBMPBean.getStaticInstance(),com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextTypeColumnName(),contextType,com.idega.core.accesscontrol.data.ICPermissionBMPBean.getContextValueColumnName(),identifierToCopyFrom);
     if(permissions != null){
       Iterator iter = permissions.iterator();
       while (iter.hasNext()) {
         ICPermission item = (ICPermission)iter.next();
         ICPermission perm = ((com.idega.core.accesscontrol.data.ICPermissionHome)com.idega.data.IDOLookup.getHomeLegacy(ICPermission.class)).createLegacy();
         perm.setContextType(contextType);
         perm.setContextValue(identifierToCopyTo);
         perm.setGroupID(item.getGroupID());
         String str = item.getPermissionString();
         if(str != null){
           perm.setPermissionString(str);
         }
 
         String str2 = item.getPermissionStringValue();
         if(str2 != null){
           perm.setPermissionStringValue(str2);
         }
         perm.setPermissionValue(item.getPermissionValue());
 
         perm.insert();
       }
     }
   }
 
   public boolean hasEditPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
     return true;
   }
 
   public boolean hasViewPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
     return true;
   }
 
 
   public void addEditPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
   }
 
   public void revokeEditPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
   }
 
 
   public void addViewPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
   }
 
   public void revokeViewPermissionFor(Group group,IWUserContext iwuc){
     /**
      * @todo: Implement
      */
   }
 
   private PermissionGroupHome getPermissionGroupHome()throws RemoteException{
     return (PermissionGroupHome)IDOLookup.getHome(PermissionGroup.class);
   }
 
   private ICPermissionHome getPermissionHome()throws RemoteException{
     return (ICPermissionHome)IDOLookup.getHome(ICPermission.class);
   }
 
 
 
 } // Class AccessControl
