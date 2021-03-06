 /*
  * 
  * 
  * Copyright (C) 2004 SIPfoundry Inc.
  * Licensed by SIPfoundry under the LGPL license.
  * 
  * Copyright (C) 2004 Pingtel Corp.
  * Licensed to SIPfoundry under a Contributor Agreement.
  * 
  * $
  */
 package org.sipfoundry.sipxconfig.common;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import junit.framework.TestCase;
 
 import org.dbunit.Assertion;
 import org.dbunit.dataset.IDataSet;
 import org.dbunit.dataset.ITable;
 import org.dbunit.dataset.ReplacementDataSet;
 import org.sipfoundry.sipxconfig.TestHelper;
 import org.sipfoundry.sipxconfig.setting.Group;
 import org.sipfoundry.sipxconfig.setting.Setting;
 import org.springframework.context.ApplicationContext;
 
 public class CoreContextImplTestDb extends TestCase {
 
     private CoreContext m_core;
 
     protected void setUp() throws Exception {
         ApplicationContext app = TestHelper.getApplicationContext(); 
         m_core = (CoreContext) app.getBean(CoreContext.CONTEXT_BEAN_NAME);
         TestHelper.cleanInsert("ClearDb.xml");
     }
 
     public void testLoadByUserName() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         assertNotNull(m_core.loadUserByUserName("userseed5"));
         assertNull(m_core.loadUserByUserName("wont find this guy"));
     }
 
     public void testLoadByExtension() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         User user = m_core.loadUserByExtension("5");
         assertNotNull(user);
         assertEquals("userseed5", user.getUserName());
         assertNull(m_core.loadUserByExtension("666"));
     }
 
     public void testSearchByUserName() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         User template = new User();
         template.setUserName("userseed");
         List users = m_core.loadUserByTemplateUser(template);
 
         assertEquals(6, users.size());
     }
 
     public void testSearchFormBlank() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         User template = new User();
        template.setFirstName("");
         List users = m_core.loadUserByTemplateUser(template);
 
         assertEquals(9, users.size());
     }
 
     public void testLoadUsers() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         List users = m_core.loadUsers();
 
         assertEquals(9, users.size());
     }
 
     public void testDeleteUsers() throws Exception {
         TestHelper.cleanInsertFlat("common/UserSearchSeed.xml");
 
         ITable usersTable = TestHelper.getConnection().createDataSet().getTable("users");
         assertEquals(9, usersTable.getRowCount());
         
         List usersToDelete = new ArrayList();
         usersToDelete.add(new Integer(1001));
         usersToDelete.add(new Integer(1002));
         m_core.deleteUsers(usersToDelete);
 
         usersTable = TestHelper.getConnection().createDataSet().getTable("users");
         assertEquals(7, usersTable.getRowCount());
     }
     
     public void testLoadGroups() throws Exception {
         TestHelper.cleanInsert("ClearDb.xml");
         TestHelper.insertFlat("common/UserGroupSeed.xml");
         List groups = m_core.getUserGroups(); 
         assertEquals(1, groups.size());
         Group group = (Group) groups.get(0);
         assertEquals("SeedUserGroup1", group.getName());
     }
     
     public void testGetUserSettingModel() {
         Setting model = m_core.getUserSettingsModel();
         assertNotNull(model.getSetting("permission"));
     }
 
     public void testAliases() throws Exception {
         List userAliases = m_core.getUserAliases();
         assertEquals(0, userAliases.size());
 
         TestHelper.cleanInsertFlat("common/TestUserSeed.xml");
 
         userAliases = m_core.getUserAliases();
         assertEquals(1, userAliases.size());
     }
     
     public void testClear() throws Exception {
         TestHelper.cleanInsertFlat("common/TestUserSeed.xml");
         m_core.clear();
         ITable t = TestHelper.getConnection().createDataSet().getTable("users");
         assertEquals(0, t.getRowCount());
     }
     
     public void testCreateAdminGroupAndInitialUserTask() throws Exception {
         TestHelper.cleanInsert("ClearDb.xml");
         m_core.createAdminGroupAndInitialUserTask();
 
         User admin = m_core.loadUserByUserName("superadmin");        
         Group adminGroup = (Group) admin.getGroups().iterator().next(); 
         IDataSet expectedDs = TestHelper.loadDataSetFlat("common/CreateAdminAndInitialUserExpected.xml");
         ReplacementDataSet expectedRds = new ReplacementDataSet(expectedDs);
         expectedRds.addReplacementObject("[user_id]", admin.getId());
         expectedRds.addReplacementObject("[group_id]", adminGroup.getId());
         expectedRds.addReplacementObject("[weight]", adminGroup.getWeight());
         expectedRds.addReplacementObject("[null]", null);
 
         IDataSet actualDs = TestHelper.getConnection().createDataSet();
 
         Assertion.assertEquals(expectedRds.getTable("users"), actualDs.getTable("users"));
         Assertion.assertEquals(expectedRds.getTable("group_storage"), actualDs.getTable("group_storage"));
         Assertion.assertEquals(expectedRds.getTable("setting_value"), actualDs.getTable("setting_value"));
     }
 }
