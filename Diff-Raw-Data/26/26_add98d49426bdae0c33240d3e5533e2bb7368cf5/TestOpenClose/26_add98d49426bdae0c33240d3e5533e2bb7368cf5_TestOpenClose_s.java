 /*
  * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
  * Version 1.0, and under the Eclipse Public License, Version 1.0
  * (http://h2database.com/html/license.html).
  * Initial Developer: H2 Group
  */
 package org.h2.test.db;
 
 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 
 import org.h2.api.DatabaseEventListener;
 import org.h2.constant.ErrorCode;
 import org.h2.store.fs.FileObject;
 import org.h2.store.fs.FileSystem;
 import org.h2.test.TestBase;
 import org.h2.tools.Restore;
 import org.h2.util.Task;
 import org.h2.util.IOUtils;
 
 /**
  * Tests opening and closing a database.
  */
 public class TestOpenClose extends TestBase implements DatabaseEventListener {
 
     private int nextId = 10;
 
     /**
      * Run just this test.
      *
      * @param a ignored
      */
     public static void main(String... a) throws Exception {
         TestBase.createCaller().init().test();
     }
 
     public void test() throws Exception {
         testErrorMessageLocked();
         testErrorMessageWrongSplit();
         testCloseDelay();
         testBackup();
         testCase();
         testReconnectFast();
         deleteDb("openClose");
     }
 
     private void testErrorMessageLocked() throws Exception {
         if (config.memory) {
             return;
         }
         deleteDb("openClose");
         Connection conn;
         conn = DriverManager.getConnection("jdbc:h2:" + getBaseDir() + "/openClose;FILE_LOCK=FS");
         try {
             DriverManager.getConnection("jdbc:h2:" + getBaseDir() + "/openClose;FILE_LOCK=FS;OPEN_NEW=TRUE");
         } catch (SQLException e) {
             assertEquals(ErrorCode.DATABASE_ALREADY_OPEN_1, e.getErrorCode());
         }
         conn.close();
     }
 
     private void testErrorMessageWrongSplit() throws Exception {
         if (config.memory) {
             return;
         }
        deleteDb("split:" + getBaseDir(), "openClose");
        deleteDb("openClose");
         Connection conn;
        conn = DriverManager.getConnection("jdbc:h2:split:12:" + getBaseDir() + "/openClose");
         conn.close();
        FileObject f = FileSystem.getInstance(getBaseDir()).openFileObject(getBaseDir() + "/openClose.h2.db.1.part", "rw");
         f.setFileLength(f.length() * 2);
         try {
            DriverManager.getConnection("jdbc:h2:split:12:" + getBaseDir() + "/openClose");
         } catch (SQLException e) {
             assertEquals(ErrorCode.IO_EXCEPTION_2, e.getErrorCode());
         }
     }
 
     private void testCloseDelay() throws Exception {
         deleteDb("openClose");
         String url = getURL("openClose;DB_CLOSE_DELAY=1", true);
         String user = getUser(), password = getPassword();
         Connection conn = DriverManager.getConnection(url, user, password);
         conn.close();
         Thread.sleep(950);
         long time = System.currentTimeMillis();
         for (int i = 0; System.currentTimeMillis() - time < 100; i++) {
             conn = DriverManager.getConnection(url, user, password);
             conn.close();
         }
         conn = DriverManager.getConnection(url, user, password);
         conn.createStatement().execute("SHUTDOWN");
         conn.close();
     }
 
     private void testBackup() throws SQLException {
         if (config.memory) {
             return;
         }
         deleteDb("openClose");
         String url = getURL("openClose", true);
         org.h2.Driver.load();
         Connection conn = DriverManager.getConnection(url, "sa", "abc def");
         Statement stat = conn.createStatement();
         stat.execute("CREATE TABLE TEST(C CLOB)");
         stat.execute("INSERT INTO TEST VALUES(SPACE(10000))");
         stat.execute("BACKUP TO '" + getBaseDir() + "/test.zip'");
         conn.close();
         deleteDb("openClose");
         Restore.execute(getBaseDir() + "/test.zip", getBaseDir(), null, true);
         conn = DriverManager.getConnection(url, "sa", "abc def");
         stat = conn.createStatement();
         ResultSet rs = stat.executeQuery("SELECT * FROM TEST");
         rs.next();
         assertEquals(10000, rs.getString(1).length());
         assertFalse(rs.next());
         conn.close();
         IOUtils.delete(getBaseDir() + "/test.zip");
     }
 
     private void testReconnectFast() throws SQLException {
         if (config.memory) {
             return;
         }
 
         deleteDb("openClose");
         String user = getUser(), password = getPassword();
         String url = getURL("openClose;DATABASE_EVENT_LISTENER='" + TestOpenClose.class.getName()
                 + "'", true);
         Connection conn = DriverManager.getConnection(url, user, password);
         Statement stat = conn.createStatement();
         try {
             stat.execute("CREATE TABLE TEST(ID IDENTITY, NAME VARCHAR)");
             stat.execute("SET MAX_MEMORY_UNDO 100000");
             stat.execute("CREATE INDEX IDXNAME ON TEST(NAME)");
             stat.execute("INSERT INTO TEST SELECT X, X || ' Data' FROM SYSTEM_RANGE(1, 1000)");
         } catch (SQLException e) {
             // ok
         }
         stat.close();
         conn.close();
         conn = DriverManager.getConnection(url, user, password);
         stat = conn.createStatement();
         ResultSet rs = stat.executeQuery("SELECT * FROM DUAL");
         if (rs.next()) {
             rs.getString(1);
         }
         rs.close();
         stat.close();
         conn.close();
         conn = DriverManager.getConnection(url, user, password);
         stat = conn.createStatement();
         // stat.execute("SET DB_CLOSE_DELAY 0");
         stat.executeUpdate("SHUTDOWN");
         stat.close();
         conn.close();
     }
 
     private void testCase() throws Exception {
         if (config.memory) {
             return;
         }
 
         org.h2.Driver.load();
         deleteDb("openClose");
         final String url = getURL("openClose;FILE_LOCK=NO", true);
         final String user = getUser(), password = getPassword();
         Connection conn = DriverManager.getConnection(url, user, password);
         conn.createStatement().execute("drop table employee if exists");
         conn.createStatement().execute("create table employee(id int primary key, name varchar, salary int)");
         conn.close();
         // previously using getSize(200, 1000);
         // but for Ubuntu, the default ulimit is 1024,
         // which breaks the test
         int len = getSize(10, 50);
         Task[] tasks = new Task[len];
         for (int i = 0; i < len; i++) {
             tasks[i] = new Task() {
                 public void call() throws SQLException {
                     Connection c = DriverManager.getConnection(url, user, password);
                     PreparedStatement prep = c.prepareStatement("insert into employee values(?, ?, 0)");
                     int id = getNextId();
                     prep.setInt(1, id);
                     prep.setString(2, "employee " + id);
                     prep.execute();
                     c.close();
                 }
             };
             tasks[i].execute();
         }
         // for(int i=0; i<len; i++) {
         // threads[i].start();
         // }
         for (int i = 0; i < len; i++) {
             tasks[i].get();
         }
         conn = DriverManager.getConnection(url, user, password);
         ResultSet rs = conn.createStatement().executeQuery("select count(*) from employee");
         rs.next();
         assertEquals(len, rs.getInt(1));
         conn.close();
     }
 
     synchronized int getNextId() {
         return nextId++;
     }
 
     public void diskSpaceIsLow() {
         throw new RuntimeException("unexpected");
     }
 
     public void exceptionThrown(SQLException e, String sql) {
         throw new AssertionError("unexpected: " + e + " sql: " + sql);
     }
 
     public void setProgress(int state, String name, int current, int max) {
         String stateName;
         switch (state) {
         case STATE_SCAN_FILE:
             stateName = "Scan " + name + " " + current + "/" + max;
             if (current > 0) {
                 throw new AssertionError("unexpected: " + stateName);
             }
             break;
         case STATE_CREATE_INDEX:
             stateName = "Create Index " + name + " " + current + "/" + max;
             if (!"SYS:SYS_ID".equals(name)) {
                 throw new AssertionError("unexpected: " + stateName);
             }
             break;
         case STATE_RECOVER:
             stateName = "Recover " + current + "/" + max;
             break;
         default:
             stateName = "?";
         }
         // System.out.println(": " + stateName);
     }
 
     public void closingDatabase() {
         // nothing to do
     }
 
     public void init(String url) {
         // nothing to do
     }
 
     public void opened() {
         // nothing to do
     }
 
 }
