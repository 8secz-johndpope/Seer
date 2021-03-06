 /**
  * Licensed to the Austrian Association for Software Tool Integration (AASTI)
  * under one or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information regarding copyright
  * ownership. The AASTI licenses this file to you under the Apache License,
  * Version 2.0 (the "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.openengsb.core.edb.internal;
 
 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.Matchers.greaterThan;
 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.notNullValue;
 import static org.junit.Assert.fail;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 import org.openengsb.core.api.edb.EDBCommit;
 import org.openengsb.core.api.edb.EDBConstants;
 import org.openengsb.core.api.edb.EDBException;
 import org.openengsb.core.api.edb.EDBLogEntry;
 import org.openengsb.core.api.edb.EDBObject;
 
 public class JPATestIT {
     private static JPADatabase db;
     private static Utils utils;
 
     @Before
     public void initDB() {
         utils = new Utils();
         db = new JPADatabase();
         try {
             db.open();
         } catch (Exception ex) {
             db = null;
             fail("Cannot open database: " + ex.toString());
         }
     }
 
     @After
     public void closeDB() {
         db.close();
     }
 
     @Test
     public void testOpenDatabase_shouldWork() {
         assertThat(db, notNullValue());
     }
 
     @Test
     public void testCommit_shouldWork() {
         try {
             JPACommit ci = db.createCommit("TestCommit", "Role");
             EDBObject obj = new EDBObject("Tester");
             obj.put("Test", "Hooray");
             ci.add(obj);
 
             long time = db.commit(ci);
 
             obj = null;
             obj = db.getObject("Tester");
             String hooray = (String) obj.get("Test");
 
             assertThat(obj, notNullValue());
             assertThat(hooray, notNullValue());
 
             checkTimeStamps(Arrays.asList(time));
         } catch (EDBException ex) {
             fail("Error: " + ex.toString());
         }
     }
 
     @Test
     public void testGetCommits_shouldWork() {
         try {
             JPACommit ci = db.createCommit("TestCommit2", "Testcontext");
             EDBObject obj = new EDBObject("TestObject");
             obj.put("Bla", "Blabla");
             ci.add(obj);
 
             long time = db.commit(ci);
 
             List<EDBCommit> commits1 = db.getCommits("context", "Testcontext");
             List<EDBCommit> commits2 = db.getCommits("context", "DoesNotExist");
 
             assertThat(commits1.size(), is(1));
             assertThat(commits2.size(), is(0));
 
             checkTimeStamps(Arrays.asList(time));
         } catch (EDBException ex) {
             fail("Faild to fetch commit list..." + ex.getLocalizedMessage());
         }
     }
 
     @Test(expected = EDBException.class)
     public void testGetInexistantObject_shouldThrowException() throws Exception {
         db.getObject("/this/object/does/not/exist");
     }
 
     @SuppressWarnings("unchecked")
     @Test
     public void testGetHistoryAndCheckForElements_shouldWork() throws Exception {
         long time1 = 0;
         long time2 = 0;
         long time3 = 0;
         long time4 = 0;
         try {
             HashMap<String, Object> data1 = new HashMap<String, Object>();
             data1.put("Lock", "Key");
             data1.put("Door", "Bell");
             data1.put("Cat", "Spongebob");
             EDBObject v1 = new EDBObject("/history/object", data1);
             JPACommit ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/1"));
             ci.add(v1);
 
             time1 = db.commit(ci);
 
             HashMap<String, Object> data2 = (HashMap<String, Object>) data1.clone();
             data2.put("Lock", "Smith");
             EDBObject v2 = new EDBObject("/history/object", data2);
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/2"));
             ci.add(v2);
 
             time2 = db.commit(ci);
 
             HashMap<String, Object> data3 = (HashMap<String, Object>) data2.clone();
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/3"));
             ci.add(utils.createRandomTestObject("/useless/4"));
             time3 = db.commit(ci);
 
             data3.put("Cat", "Dog");
             EDBObject v3 = new EDBObject("/history/object", data3);
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(v3);
             ci.add(utils.createRandomTestObject("/useless/5"));
 
             time4 = db.commit(ci);
         } catch (EDBException ex) {
             fail("Error: " + ex.toString());
         }
 
         List<EDBObject> history = db.getHistory("/history/object");
 
         boolean ordered = true;
         for (int i = 1; i < 3; i++) {
             if (history.get(i - 1).getTimestamp() > history.get(i).getTimestamp()) {
                 ordered = false;
             }
         }
         assertThat(ordered, is(true));
         assertThat(history.get(0).getString("Lock"), is("Key"));
         assertThat(history.get(0).getString("Cat"), is("Spongebob"));
 
         assertThat(history.get(1).getString("Lock"), is("Smith"));
         assertThat(history.get(1).getString("Cat"), is("Spongebob"));
 
         assertThat(history.get(2).getString("Lock"), is("Smith"));
         assertThat(history.get(2).getString("Cat"), is("Dog"));
 
         checkTimeStamps(Arrays.asList(time1, time2, time3, time4));
     }
 
     @Test
     public void testHistoryOfDeletion_shouldWork() throws Exception {
         JPACommit ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
         ci.add(utils.createRandomTestObject("/deletion/1"));
         long time1 = db.commit(ci);
 
         ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
         ci.delete("/deletion/1");
         long time2 = db.commit(ci);
 
         List<EDBObject> history = db.getHistory("/deletion/1");
 
         assertThat(history.size(), is(2));
         assertThat(history.get(0).isDeleted(), is(false));
         assertThat(history.get(1).isDeleted(), is(true));
 
         checkTimeStamps(Arrays.asList(time1, time2));
     }
 
     @SuppressWarnings("unchecked")
     @Test
     public void testGetLog_shouldWork() throws Exception {
         long time1 = 0;
         long time2 = 0;
         long time3 = 0;
         long time4 = 0;
         try {
             HashMap<String, Object> data1 = new HashMap<String, Object>();
             data1.put("Burger", "Salad");
             data1.put("Bla", "Blub");
             data1.put("Cheese", "Butter");
             EDBObject v1 = new EDBObject("/history/test/object", data1);
             JPACommit ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/test/1"));
             ci.add(utils.createRandomTestObject("/deletion/test/1"));
             ci.add(v1);
 
             time1 = db.commit(ci);
 
             HashMap<String, Object> data2 = (HashMap<String, Object>) data1.clone();
             data2.put("Burger", "Meat");
             EDBObject v2 = new EDBObject("/history/test/object", data2);
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/test/2"));
             ci.delete("/deletion/test/1");
             ci.add(v2);
             time2 = db.commit(ci);
 
             HashMap<String, Object> data3 = (HashMap<String, Object>) data2.clone();
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(utils.createRandomTestObject("/useless/test/3"));
             ci.add(utils.createRandomTestObject("/useless/test/4"));
             time3 = db.commit(ci);
 
             data3.put("Cheese", "Milk");
 
             EDBObject v3 = new EDBObject("/history/test/object", data3);
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(v3);
             ci.add(utils.createRandomTestObject("/useless/test/5"));
 
             time4 = db.commit(ci);
         } catch (Exception ex) {
             ex.printStackTrace();
             fail("getHistory failed, didn't even get to try getLog: " + ex.toString());
         }
 
         List<EDBLogEntry> log = db.getLog("/history/test/object", time1, time4);
         assertThat(log.size(), is(3));
 
         checkTimeStamps(Arrays.asList(time1, time2, time3, time4));
     }
 
     @SuppressWarnings("serial")
     @Test
     public void testQueryWithSomeAspects_shouldWork() {
         try {
             HashMap<String, Object> data1 = new HashMap<String, Object>();
             data1.put("A", "B");
             data1.put("Cow", "Milk");
             data1.put("Dog", "Food");
             EDBObject v1 = new EDBObject("/test/query1", data1);
             JPACommit ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(v1);
             long time1 = db.commit(ci);
 
             HashMap<String, Object> data2 = new HashMap<String, Object>();
             data2.put("Cow", "Milk");
             data2.put("House", "Garden");
             v1 = new EDBObject("/test/query2", data2);
             ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
             ci.add(v1);
             long time2 = db.commit(ci);
 
             List<EDBObject> list1 = db.query("A", "B");
             List<EDBObject> list2 = db.query(new HashMap<String, Object>() {
                 {
                     put("A", "B");
                     put("Dog", "Food");
                 }
             });
 
             List<EDBObject> list3 = db.query(new HashMap<String, Object>() {
                 {
                     put("Cow", "Milk");
                 }
             });
 
             List<EDBObject> list4 = db.query(new HashMap<String, Object>() {
                 {
                     put("A", "B");
                     put("Cow", "Milk");
                     put("House", "Garden");
                 }
             });
 
             assertThat(list1.size(), is(1));
             assertThat(list2.size(), is(1));
             assertThat(list3.size(), is(2));
             assertThat(list4.size(), is(0));
 
             checkTimeStamps(Arrays.asList(time1, time2));
 
             // removed because of the by jpa not supported regex command
             // list = db.query(new HashMap<String, Object>() {
             // {
             // put("Cat", "Dog");
             // put("Lock", Pattern.compile("Smith|Key"));
             // }
             // });
             // assertTrue("There's one object in HEAD with Cat:Dog,Lock:/Smith|Key/", list.size() == 1);
         } catch (EDBException ex) {
             fail("DB error: " + ex.toString());
         }
     }
 
     @SuppressWarnings("unchecked")
     @Test
     public void testDiff_shouldWork() throws Exception {
         long time1 = 0;
         long time2 = 0;
         long time3 = 0;
         try {
             HashMap<String, Object> data1 = new HashMap<String, Object>();
             data1.put("KeyA", "Value A 1");
             data1.put("KeyB", "Value B 1");
             data1.put("KeyC", "Value C 1");
             EDBObject v1 = new EDBObject("/diff/object", data1);
             JPACommit ci = db.createCommit("Blub", "Testing");
             ci.add(v1);
             time1 = db.commit(ci);
 
             HashMap<String, Object> data2 = (HashMap<String, Object>) data1.clone();
 
             data2.put("KeyD", "Value D 1");
             data2.put("KeyA", "Value A 2");
             EDBObject v2 = new EDBObject("/diff/object", data2);
             ci = db.createCommit("Blub", "Testing");
             ci.add(v2);
             time2 = db.commit(ci);
 
             HashMap<String, Object> data3 = (HashMap<String, Object>) data2.clone();
 
             data3.remove("KeyB");
             data3.put("KeyC", "Value C 3");
             EDBObject v3 = new EDBObject("/diff/object", data3);
             ci = db.createCommit("Blub", "Testing");
             ci.add(v3);
             time3 = db.commit(ci);
         } catch (EDBException ex) {
             fail("Failed to prepare commits for comparison!" + ex.getLocalizedMessage());
         }
 
         checkTimeStamps(Arrays.asList(time1, time2, time3));
 
         Diff diffAb = db.getDiff(time1, time2);
         Diff diffBc = db.getDiff(time2, time3);
         Diff diffAc = db.getDiff(time1, time3);
 
         assertThat(diffAb.getDifferenceCount(), is(1));
         assertThat(diffBc.getDifferenceCount(), is(1));
         assertThat(diffAc.getDifferenceCount(), is(1));
     }
 
     @Test
     public void testGetResurrectedOIDs_shouldWork() throws Exception {
         HashMap<String, Object> data1 = new HashMap<String, Object>();
         data1.put("KeyA", "Value A 1");
         EDBObject v1 = new EDBObject("/ress/object", data1);
         JPACommit ci = db.createCommit("Blub", "Testing");
         ci.add(v1);
         long time1 = db.commit(ci);
 
         v1 = new EDBObject("/ress/object2", data1);
         ci = db.createCommit("Blub", "Testing");
         ci.add(v1);
         ci.delete("/ress/object");
         long time2 = db.commit(ci);
 
         v1 = new EDBObject("/ress/object", data1);
         ci = db.createCommit("Blub", "Testing");
         ci.delete("/ress/object2");
         ci.add(v1);
         long time3 = db.commit(ci);
 
         List<String> oids = db.getResurrectedOIDs();
 
         assertThat(oids.contains("/ress/object"), is(true));
         assertThat(oids.contains("/ress/object2"), is(false));
 
         checkTimeStamps(Arrays.asList(time1, time2, time3));
     }
 
     @Test(expected = EDBException.class)
     public void testCommitTwiceSameCommit_shouldThrowError() throws Exception {
         HashMap<String, Object> data1 = new HashMap<String, Object>();
         data1.put("KeyA", "Value A 1");
         EDBObject v1 = new EDBObject("/fail/object", data1);
         JPACommit ci = db.createCommit("Blub", "Testing");
         ci.add(v1);
         db.commit(ci);
         db.commit(ci);
     }
 
     @Test
     public void testQueryOfOldVersionShouldWork() {
         HashMap<String, Object> data1v1 = new HashMap<String, Object>();
         data1v1.put("pre:KeyA", "pre:Value A 1");
         data1v1.put("pre:KeyB", "pre:Value A 1");
         EDBObject v11 = new EDBObject("pre:/test/object1", data1v1);
         JPACommit ci = db.createCommit("Blub", "Testing");
         ci.add(v11);
         HashMap<String, Object> data2v1 = new HashMap<String, Object>();
         data2v1.put("pre:KeyA", "pre:Value A 2");
         data2v1.put("pre:KeyB", "pre:Value A 1");
         EDBObject v12 = new EDBObject("pre:/test/object2", data2v1);
         ci.add(v12);
         HashMap<String, Object> data3v1 = new HashMap<String, Object>();
         data3v1.put("pre:KeyA", "pre:Value A 3");
         data3v1.put("pre:KeyB", "pre:Value A 1");
         EDBObject v13 = new EDBObject("pre:/test/object3", data3v1);
         ci.add(v13);
 
         long time1 = db.commit(ci);
 
         HashMap<String, Object> data1v2 = new HashMap<String, Object>();
         data1v2.put("pre:KeyA", "pre:Value A 1");
         data1v2.put("pre:KeyB", "pre:Value A 1");
         EDBObject v21 = new EDBObject("pre:/test/object1", data1v2);
         ci = db.createCommit("Blub", "Testing");
         ci.add(v21);
         HashMap<String, Object> data2v2 = new HashMap<String, Object>();
         data2v2.put("pre:KeyA", "pre:Value A 2");
         data2v2.put("pre:KeyB", "pre:Value A 1");
         EDBObject v22 = new EDBObject("pre:/test/object2", data2v2);
         ci.add(v22);
         HashMap<String, Object> data4v1 = new HashMap<String, Object>();
         data4v1.put("pre:KeyA", "pre:Value A 4");
         data4v1.put("pre:KeyB", "pre:Value A 1");
         EDBObject v23 = new EDBObject("pre:/test/object4", data4v1);
         ci.add(v23);
 
         long time2 = db.commit(ci);
 
         HashMap<String, Object> data1v3 = new HashMap<String, Object>();
         data1v3.put("pre:KeyA", "pre:Value A 1");
         data1v3.put("pre:KeyB", "pre:Value A 1");
         EDBObject v31 = new EDBObject("pre:/test/object1", data1v3);
         ci = db.createCommit("Blub", "Testing");
         ci.add(v31);
         HashMap<String, Object> data2v3 = new HashMap<String, Object>();
         data2v3.put("pre:KeyA", "pre:Value A 2a");
         data2v3.put("pre:KeyB", "pre:Value A 1");
         EDBObject v32 = new EDBObject("pre:/test/object2", data2v3);
         ci.add(v32);
         HashMap<String, Object> data4v2 = new HashMap<String, Object>();
         data4v2.put("pre:KeyA", "pre:Value A 4");
         data4v2.put("pre:KeyB", "pre:Value A 1");
         EDBObject v33 = new EDBObject("pre:/test/object4", data4v2);
         ci.add(v33);
 
         long time3 = db.commit(ci);
 
         Map<String, Object> map = new HashMap<String, Object>();
         map.put("pre:KeyB", "pre:Value A 1");
         List<EDBObject> result = db.query(map, time2);
 
         boolean b1 = false;
         boolean b2 = false;
         boolean b3 = false;
 
         for (EDBObject e : result) {
             if (e.get("pre:KeyA").equals("pre:Value A 1")) {
                 b1 = true;
             }
             if (e.get("pre:KeyA").equals("pre:Value A 2")) {
                 b2 = true;
             }
             if (e.get("pre:KeyA").equals("pre:Value A 3")) {
                 b3 = true;
             }
         }
 
         assertThat(b1, is(true));
         assertThat(b2, is(true));
         assertThat(b3, is(true));
         assertThat(time1 > 0, is(true));
         assertThat(time2 > 0, is(true));
         assertThat(time3 > 0, is(true));
     }
 
     @Test
     public void testQueryWithTimestamp_shouldWork() {
         HashMap<String, Object> data1 = new HashMap<String, Object>();
         data1.put("K", "B");
         data1.put("Cow", "Milk");
         data1.put("Dog", "Food");
         EDBObject v1 = new EDBObject("/test/querynew1", data1);
         JPACommit ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
         ci.add(v1);
         db.commit(ci);
 
         data1 = new HashMap<String, Object>();
         data1.put("Dog", "Food");
         v1 = new EDBObject("/test/querynew1", data1);
         ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
         ci.add(v1);
         db.commit(ci);
 
         data1 = new HashMap<String, Object>();
         data1.put("K", "B");
         data1.put("Dog", "Food");
         v1 = new EDBObject("/test/querynew2", data1);
         ci = db.createCommit(utils.getRandomCommitter(), utils.getRandomRole());
         ci.add(v1);
         db.commit(ci);
 
         Map<String, Object> map = new HashMap<String, Object>();
         map.put("K", "B");
         List<EDBObject> result = db.query(map, System.currentTimeMillis());
         assertThat(result.size(), is(1));
     }
 
     @Test
     public void testQueryOfLastKnownVersionShouldWork() {
         HashMap<String, Object> data1v1 = new HashMap<String, Object>();
         data1v1.put("KeyA", "Value A 1");
         data1v1.put("KeyB", "Value A 1");
         EDBObject v11 = new EDBObject("/test/object1", data1v1);
         JPACommit ci = db.createCommit("Blub", "Testing");
         ci.add(v11);
         HashMap<String, Object> data2v1 = new HashMap<String, Object>();
         data2v1.put("KeyA", "Value A 2");
         data2v1.put("KeyB", "Value A 1");
         EDBObject v12 = new EDBObject("/test/object2", data2v1);
         ci.add(v12);
         HashMap<String, Object> data3v1 = new HashMap<String, Object>();
         data3v1.put("KeyA", "Value A 3");
         data3v1.put("KeyB", "Value A 1");
         EDBObject v13 = new EDBObject("/test/object3", data3v1);
         ci.add(v13);
 
         long time1 = db.commit(ci);
 
         ci = db.createCommit("Blub", "Testing");
         HashMap<String, Object> data1v2 = new HashMap<String, Object>();
         data1v2.put("KeyA", "Value A 1");
         data1v2.put("KeyB", "Value A 1");
         EDBObject v21 = new EDBObject("/test/object1", data1v2);
         ci.add(v21);
         HashMap<String, Object> data2v2 = new HashMap<String, Object>();
         data2v2.put("KeyA", "Value A 2");
         data2v2.put("KeyB", "Value A 1");
         EDBObject v22 = new EDBObject("/test/object2", data2v2);
         ci.add(v22);
 
         long time2 = db.commit(ci);
 
         ci = db.createCommit("Blub", "Testing");
         HashMap<String, Object> data2v3 = new HashMap<String, Object>();
         data2v3.put("KeyA", "Value A 2a");
         data2v3.put("KeyB", "Value A 1");
         EDBObject v32 = new EDBObject("/test/object2", data2v3);
         ci.add(v32);
         HashMap<String, Object> data4v1 = new HashMap<String, Object>();
         data4v1.put("KeyA", "Value A 4");
         data4v1.put("KeyB", "Value A 1");
         EDBObject v33 = new EDBObject("/test/object4", data4v1);
         ci.add(v33);
 
         long time3 = db.commit(ci);
 
         Map<String, Object> map = new HashMap<String, Object>();
         map.put("KeyB", "Value A 1");
         List<EDBObject> result = db.query(map, time3);
 
         boolean b1 = false;
         boolean b2 = false;
         boolean b3 = false;
         boolean b4 = false;
 
         for (EDBObject e : result) {
             if (e.get("KeyA").equals("Value A 1")) {
                 b1 = true;
             }
             if (e.get("KeyA").equals("Value A 2a")) {
                 b2 = true;
             }
             if (e.get("KeyA").equals("Value A 3")) {
                 b3 = true;
             }
 
             if (e.get("KeyA").equals("Value A 4")) {
                 b4 = true;
             }
         }
 
         assertThat(b1, is(true));
         assertThat(b2, is(true));
         assertThat(b3, is(true));
         assertThat(b4, is(true));
         assertThat(time1 > 0, is(true));
         assertThat(time2 > 0, is(true));
         assertThat(time3 > 0, is(true));
     }
 
     /**
      * iterates through the list of timestamps and checks if every timestamp is bigger than 0
      */
     private void checkTimeStamps(List<Long> timestamps) {
         for (Long timestamp : timestamps) {
             assertThat(timestamp, greaterThan((long) 0));
         }
     }
 
     @Test
     public void testCommitEDBObjectsInsert_shouldWork() {
         EDBObject object = new EDBObject("/commit/test/insert/1");
         object.put("bla", "blub");
         List<EDBObject> inserts = new ArrayList<EDBObject>();
         inserts.add(object);
 
         db.commitEDBObjects(inserts, null, null);
 
         object = db.getObject("/commit/test/insert/1");
         assertThat(object.get("bla").toString(), is("blub"));
         assertThat(Integer.parseInt(object.get(EDBConstants.MODEL_VERSION).toString()), is(1));
     }
     
     @Test(expected = EDBException.class)
     public void testCommitEDBObjectsInsertDouble_shouldThrowException() {
         EDBObject object = new EDBObject("/commit/test/insert/2");
         List<EDBObject> inserts = new ArrayList<EDBObject>();
         inserts.add(object);
         
         db.commitEDBObjects(inserts, null, null);
         db.commitEDBObjects(inserts, null, null);
     }
     
     @Test
     public void testCommitEDBObjectsUpdate_shouldWork() {
         EDBObject object = new EDBObject("/commit/test/update/1");
         object.put("testkey", "testvalue");
         List<EDBObject> objects = new ArrayList<EDBObject>();
         objects.add(object);
         
         db.commitEDBObjects(objects, null, null);
         
         EDBObject first = db.getObject("/commit/test/update/1");
         
         objects.clear();
         object.put("testkey", "testvalue1");
         objects.add(object);
         
         db.commitEDBObjects(null, objects, null);
         
         EDBObject second = db.getObject("/commit/test/update/1");
         
         assertThat(Integer.parseInt(first.get(EDBConstants.MODEL_VERSION).toString()), is(1));
         assertThat(first.get("testkey").toString(), is("testvalue"));
         assertThat(Integer.parseInt(second.get(EDBConstants.MODEL_VERSION).toString()), is(2));
         assertThat(second.get("testkey").toString(), is("testvalue1"));
     }
     
     @Test(expected = EDBException.class)
     public void testCommitEDBObjectsUpdateVerstionConflict_shouldThrowException() {
         EDBObject object = new EDBObject("/commit/test/update/2");
         List<EDBObject> objects = new ArrayList<EDBObject>();
         objects.add(object);
         db.commitEDBObjects(objects, null, null);
         object.put(EDBConstants.MODEL_VERSION, 0);
        object.put("test", "test");
         db.commitEDBObjects(null, objects, null);
     }
     
     @Test
     public void testCommitEDBObjectsDelete_shouldWork() {
         EDBObject object = new EDBObject("/commit/test/delete/1");
         List<EDBObject> objects = new ArrayList<EDBObject>();
         objects.add(object);
         db.commitEDBObjects(objects, null, null);
         db.commitEDBObjects(null, null, objects);
         
         EDBObject entry = db.getObject("/commit/test/delete/1");
         assertThat(entry.isDeleted(), is(true));
     }
     
     @Test(expected = EDBException.class)
     public void testCommitEDBObjectsDeleteNonExisting_shouldThrowException() {
         EDBObject object = new EDBObject("/commit/test/delete/2");
         List<EDBObject> objects = new ArrayList<EDBObject>();
         objects.add(object);
         db.commitEDBObjects(null, null, objects);
     }
     
     @Test(expected = EDBException.class)
     public void testCommitEDBObjectsDeleteAlreadyDeleted_shouldThrowException() {
         EDBObject object = new EDBObject("/commit/test/delete/3");
         List<EDBObject> objects = new ArrayList<EDBObject>();
         objects.add(object);
         db.commitEDBObjects(objects, null, null);
         db.commitEDBObjects(null, null, objects);
         db.commitEDBObjects(null, null, objects);
     }
 }
