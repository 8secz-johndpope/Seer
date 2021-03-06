 /***************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 *
 *              ... and it just works.
 *
 ****************************************************/
 package Sirius.server.localserver.user;
 
 import Sirius.server.AbstractShutdownable;
 import Sirius.server.ServerExitError;
 import Sirius.server.Shutdown;
 import Sirius.server.newuser.Membership;
 import Sirius.server.newuser.User;
 import Sirius.server.newuser.UserGroup;
 import Sirius.server.property.ServerProperties;
 import Sirius.server.sql.DBConnection;
 import Sirius.server.sql.DBConnectionPool;
 import Sirius.server.sql.ExceptionHandler;
 
 import org.apache.log4j.Logger;
 
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 
 import java.util.Vector;
 
 /**
  * DOCUMENT ME!
  *
  * @author   sascha.schlobinski@cismet.de
  * @author   thorsten.hell@cismet.de
  * @author   martin.scholl@cismet.de
  * @version  $Revision$, $Date$
  */
 public final class UserStore extends Shutdown {
 
     //~ Static fields/initializers ---------------------------------------------
 
     private static final transient Logger LOG = Logger.getLogger(UserStore.class);
 
     //~ Instance fields --------------------------------------------------------
 
     protected DBConnectionPool conPool;
 
     protected Vector users;
     protected Vector userGroups;
     // protected Hashtable userGroupHash;
     protected Vector memberships;
     // protected Hashtable membershipHash;// by userIDplusLsName
     protected ServerProperties properties;
     protected PreparedStatement validateUser;
 
     //~ Constructors -----------------------------------------------------------
 
     /**
      * Creates a new UserStore object.
      *
      * @param  conPool     DOCUMENT ME!
      * @param  properties  DOCUMENT ME!
      */
     public UserStore(final DBConnectionPool conPool, final ServerProperties properties) {
         this.conPool = conPool;
         this.properties = properties;
         users = new Vector(100, 100);
         userGroups = new Vector(10, 10);
         // userGroupHash = new Hashtable(25);
         memberships = new Vector(100, 100);
         // membershipHash = new Hashtable(101);
 
         try {
             final ResultSet userTable = conPool.submitQuery("get_all_users", new Object[0]); // NOI18N
 
             // --------------------load users--------------------------------------------------
 
             while (userTable.next()) {
                 try {
                     final User tmp = new User(
                             userTable.getInt("id"),                   // NOI18N
                             userTable.getString("login_name").trim(), // NOI18N
                             properties.getServerName(),
                             userTable.getBoolean("administrator"));   // NOI18N
 
                     users.addElement(tmp);
                 } catch (Exception e) {
                     LOG.error(e);
 
                     if (e instanceof java.sql.SQLException) {
                         throw e;
                     }
                 }
             }
 
             userTable.close();
 
             // --------------------load userGroups--------------------------------------------------
 
             final ResultSet userGroupTable = conPool.submitQuery("get_all_usergroups", new Object[0]); // NOI18N
 
             while (userGroupTable.next()) {
                 try {
                     final UserGroup tmp = new UserGroup(
                             userGroupTable.getInt("id"),             // NOI18N
                             userGroupTable.getString("name").trim(), // NOI18N
                            properties.getServerName(),
                             userGroupTable.getString("descr"));      // NOI18N
                     userGroups.addElement(tmp);
                 } catch (Exception e) {
                     LOG.error(e);
 
                     if (e instanceof java.sql.SQLException) {
                         throw e;
                     }
                 }
             }
 
             userGroupTable.close();
 
             // --------------------load memberships--------------------------------------------------
 
             final ResultSet memberTable = conPool.submitQuery("get_all_memberships", new Object[0]); // NOI18N
 
             while (memberTable.next()) {
                 try {
                     final String lsName = properties.getServerName();
 
                     final String login = memberTable.getString("login_name");
                     final String ug = memberTable.getString("ug");
 
                     String ugDomain = memberTable.getString("ugDomain"); // NOI18N
 
                     if ((ugDomain == null) || ugDomain.equalsIgnoreCase("local")) { // NOI18N
                         ugDomain = lsName;
                     }
 
                     final String usrDomain = lsName;
 
                     final Membership tmp = new Membership(login, usrDomain, ug, ugDomain);
                     memberships.addElement(tmp);
                 } catch (Exception e) {
                     LOG.error(e);
 
                     if (e instanceof java.sql.SQLException) {
                         throw e;
                     }
                 }
             }
 
             memberTable.close();
 
             // prepare statement for validate user (called very often) :-)
             final String valUser =
                 "select count(*) from cs_usr as u ,cs_ug as ug ,cs_ug_membership as m where u.id=m.usr_id and  ug.id = m.ug_id and trim(login_name) = ? and trim(ug.name) = ?"; // NOI18N
             validateUser = conPool.getConnection().prepareStatement(valUser);
 
             addShutdown(new AbstractShutdownable() {
 
                     @Override
                     protected void internalShutdown() throws ServerExitError {
                         if (LOG.isDebugEnabled()) {
                             LOG.debug("shutting down UserStore"); // NOI18N
                         }
 
                         users.clear();
                         userGroups.clear();
                         memberships.clear();
                         DBConnection.closeStatements(validateUser);
                     }
                 });
         } catch (java.lang.Exception e) {
             ExceptionHandler.handle(e);
             LOG.error("<LS> ERROR ::  in membership statement" + e.getMessage(), e); // NOI18N
         }
     }                                                                                // end Konstruktor
 
     //~ Methods ----------------------------------------------------------------
 
     /**
      * DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
     public Vector getUsers() {
         return users;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
     public Vector getUserGroups() {
         return userGroups;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
     public Vector getMemberships() {
         return memberships;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param   user         DOCUMENT ME!
      * @param   oldPassword  DOCUMENT ME!
      * @param   newPassword  DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      *
      * @throws  Exception  DOCUMENT ME!
      */
     public boolean changePassword(final User user, final String oldPassword, final String newPassword)
             throws Exception {
         final java.lang.Object[] params = new java.lang.Object[3];
 
         params[0] = newPassword;
         params[1] = user.getName().toLowerCase();
         params[2] = oldPassword;
 
         if (conPool.submitUpdate("change_user_password", params) > 0) { // NOI18N
             return true;
         } else {
             return false;
         }
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param   user  DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
 
     // FIXME: WHATS THE PURPOSE OF THIS IMPL???
     public boolean validateUser(final User user) {
         return true;
     }
 
     /**
      * --------------------------------------------------------------------------
      *
      * @param   user      DOCUMENT ME!
      * @param   password  DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      *
      * @throws  SQLException  DOCUMENT ME!
      */
     public boolean validateUserPassword(final User user, final String password) throws SQLException {
         ResultSet result = null;
         try {
             // TODO: should username and password be trimmed?
             result = conPool.submitInternalQuery(
                     DBConnection.DESC_VERIFY_USER_PW,
                     user.getName().trim().toLowerCase(),
                     password.trim().toLowerCase());
             return result.next() && (result.getInt(1) == 1);
         } finally {
             DBConnection.closeResultSets(result);
         }
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param   user  DOCUMENT ME!
      * @param   key   DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      *
      * @throws  SQLException  DOCUMENT ME!
      */
     public String getConfigAttr(final User user, final String key) throws SQLException {
         if ((user == null) || (key == null)) {
             if (LOG.isDebugEnabled()) {
                 LOG.debug("user and/or key is null, returning null: user: " + user + " || key: " + key);
             }
 
             return null;
         }
 
         ResultSet keyIdSet = null;
         int keyId = -1;
         try {
             keyIdSet = conPool.submitInternalQuery(DBConnection.DESC_FETCH_CONFIG_ATTR_KEY_ID, key);
             if (keyIdSet.next()) {
                 keyId = keyIdSet.getInt(1);
             } else {
                 if (LOG.isInfoEnabled()) {
                     LOG.info("key not present: " + key); // NOI18N
                 }
 
                 return null;
             }
         } finally {
             DBConnection.closeResultSets(keyIdSet);
         }
 
         assert keyId > 0 : "invalid key id"; // NOI18N
 
         ResultSet userValueSet = null;
         ResultSet ugValueSet = null;
         ResultSet domainValueSet = null;
         try {
             final String userName = user.getName();
             final String userGroupName = user.getUserGroup().getName();
             final String domain;
             if (properties.getServerName().equals(user.getUserGroup().getDomain())) {
                 domain = "LOCAL"; // NOI18N
             } else {
                 domain = user.getUserGroup().getDomain();
             }
 
             final String value;
             userValueSet = conPool.submitInternalQuery(
                     DBConnection.DESC_FETCH_CONFIG_ATTR_USER_VALUE,
                     userName,
                     userGroupName,
                     domain,
                     keyId);
             if (userValueSet.next()) {
                 value = userValueSet.getString(1);
             } else {
                 ugValueSet = conPool.submitInternalQuery(
                         DBConnection.DESC_FETCH_CONFIG_ATTR_UG_VALUE,
                         userGroupName,
                         domain,
                         keyId);
                 if (ugValueSet.next()) {
                     value = ugValueSet.getString(1);
                 } else {
                     domainValueSet = conPool.submitInternalQuery(
                             DBConnection.DESC_FETCH_CONFIG_ATTR_DOMAIN_VALUE,
                             domain,
                             keyId);
                     if (domainValueSet.next()) {
                         value = domainValueSet.getString(1);
                     } else {
                         value = null;
                     }
                 }
             }
 
             return value;
         } finally {
             DBConnection.closeResultSets(userValueSet, ugValueSet, domainValueSet);
         }
     }
 }
