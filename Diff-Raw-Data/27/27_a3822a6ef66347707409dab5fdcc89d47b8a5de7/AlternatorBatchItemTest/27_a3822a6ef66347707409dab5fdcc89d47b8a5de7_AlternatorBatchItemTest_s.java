 package com.michelboudreau.testv2;
 
 import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
 import com.amazonaws.services.dynamodbv2.model.AttributeValue;
 import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
 import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
 import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
 import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
 import com.amazonaws.services.dynamodbv2.model.ConsumedCapacity;
 import com.amazonaws.services.dynamodbv2.model.DeleteRequest;
 import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
 import com.amazonaws.services.dynamodbv2.model.PutRequest;
 import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
 import com.amazonaws.services.dynamodbv2.model.TableDescription;
 import com.amazonaws.services.dynamodbv2.model.WriteRequest;
 
 import org.junit.After;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 @RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(locations = {"classpath:/applicationContext.xml"})
 public class AlternatorBatchItemTest extends AlternatorTest {
 
     private String tableName1;
     private String tableName2;
     private String hashKeyName1;
     private String hashKeyName2;
 
     @Before
     public void setUp() throws Exception {
         tableName1 = createTableName();
         AttributeDefinition hashAttr1 = createNumberAttributeDefinition();
         TableDescription tableDescription1 = createTable(tableName1, hashAttr1);
         hashKeyName1 = getHashKeyElement(tableDescription1.getKeySchema()).getAttributeName();
 
         tableName2 = createTableName();
         AttributeDefinition hashAttr2 = createNumberAttributeDefinition();
         TableDescription tableDescription2 = createTable(tableName2, hashAttr2);
         hashKeyName2 = getHashKeyElement(tableDescription2.getKeySchema()).getAttributeName();
     }
 
     @After
     public void tearDown() throws Exception {
         deleteAllTables();
     }
 
 	@Test
 	public void vanillaBatchGetItemTest() throws Exception {
         this.vanillaBatchWriteItemTest();
         BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
         Map<String, KeysAndAttributes> requestItems = new HashMap<String, KeysAndAttributes>();
 
         KeysAndAttributes keysAndAttributes1 = new KeysAndAttributes();
         List<Map<String, AttributeValue>> itemKeys1 = new ArrayList<Map<String, AttributeValue>>();
         itemKeys1.add(createItemKey(hashKeyName1, new AttributeValue().withN("6")));
         keysAndAttributes1.setKeys(itemKeys1);
         List<String> attributesToGet1 = new ArrayList<String>();
         attributesToGet1.add(hashKeyName1);
         keysAndAttributes1.setAttributesToGet(attributesToGet1);
 
         KeysAndAttributes keysAndAttributes2 = new KeysAndAttributes();
         List<Map<String, AttributeValue>> itemKeys2 = new ArrayList<Map<String, AttributeValue>>();
         itemKeys2.add(createItemKey(hashKeyName2, new AttributeValue().withN("1")));
         keysAndAttributes2.setKeys(itemKeys2);
         List<String> attributesToGet2 = new ArrayList<String>();
         attributesToGet2.add(hashKeyName2);
         keysAndAttributes2.setAttributesToGet(attributesToGet2);
         //Test case 1: Every request has matches.
 //        keys.add(new Key(new AttributeValue("4")));
 //        keys.add(new Key(new AttributeValue("5")));
 //        keys.add(new Key(new AttributeValue("3")));
 
         //Test case 2: Requests has no match.
 
         //Test case 3: Complicated test, some requests has matches, some doesn't.
 //        keys.add(new Key(new AttributeValue("7")));
 //        keys.add(new Key(new AttributeValue("4")));
 
         //Test case 4: Duplicated request
         //Duplicated requests return duplicated results.
 //        keys.add(new Key(new AttributeValue("7")));
 //        keys.add(new Key(new AttributeValue("7")));
 //        keys.add(new Key(new AttributeValue("4")));
 //        keys.add(new Key(new AttributeValue("4")));
 
         //Test case for Exception: Table doesn't exist.
 //        requestItems.put("Vito's Table", keysAndAttributes);
 
         // Normal test
         // TODO: Multi table test failed. Need to be fixed.
         requestItems.put(tableName1, keysAndAttributes1);
         requestItems.put(tableName2, keysAndAttributes2);
 
         batchGetItemRequest.withRequestItems(requestItems);
 		BatchGetItemResult result  = getClient().batchGetItem(batchGetItemRequest);
         junit.framework.Assert.assertNotNull("UnprocessedKeys should be empty rather than null.", result.getUnprocessedKeys());
 	}
 
     @Test
     public void vanillaBatchWriteItemTest() throws Exception{
         BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
         BatchWriteItemResult result;
 
         // Create a map for the requests in the batch
         Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
 
         // Test: write items to database
         Map<String, AttributeValue> forumItem = new HashMap<String, AttributeValue>();
         forumItem.put(hashKeyName1, new AttributeValue().withN("1"));
         forumItem.put("range", new AttributeValue().withS("a"));
         List<WriteRequest> forumList = new ArrayList<WriteRequest>();
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem)));
 
         Map<String, AttributeValue> forumItem1 = new HashMap<String, AttributeValue>();
         forumItem1.put(hashKeyName1, new AttributeValue().withN("2"));
         forumItem1.put("range", new AttributeValue().withS("b"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem1)));
 
         Map<String, AttributeValue> forumItem5 = new HashMap<String, AttributeValue>();
         forumItem5.put(hashKeyName1, new AttributeValue().withN("3"));
         forumItem5.put("range", new AttributeValue().withS("c"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem5)));
 
         Map<String, AttributeValue> forumItem2 = new HashMap<String, AttributeValue>();
         forumItem2.put(hashKeyName1, new AttributeValue().withN("4"));
         forumItem2.put("range", new AttributeValue().withS("d"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem2)));
 
         Map<String, AttributeValue> forumItem3 = new HashMap<String, AttributeValue>();
         forumItem3.put(hashKeyName1, new AttributeValue().withN("5"));
         forumItem3.put("range", new AttributeValue().withS("e"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem3)));
 
         Map<String, AttributeValue> forumItem4 = new HashMap<String, AttributeValue>();
         forumItem4.put(hashKeyName1, new AttributeValue().withN("6"));
         forumItem4.put("range", new AttributeValue().withS("f"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem4)));
 
         //Test case: with duplicated hashkey item but distinguished range key input.
         Map<String, AttributeValue> forumItem6 = new HashMap<String, AttributeValue>();
         forumItem6.put(hashKeyName1, new AttributeValue().withN("6"));
         forumItem6.put("range", new AttributeValue().withS("ff"));
         forumList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItem6)));
 
         //Test on Table 2
         Map<String, AttributeValue> forumItemT2 = new HashMap<String, AttributeValue>();
         forumItemT2.put(hashKeyName2, new AttributeValue().withN("1"));
         forumItemT2.put("range", new AttributeValue().withS("a"));
         List<WriteRequest> forumListT2 = new ArrayList<WriteRequest>();
         forumListT2.add(new WriteRequest().withPutRequest(new PutRequest().withItem(forumItemT2)));
 
         requestItems.put(tableName1, forumList);
         requestItems.put(tableName2, forumListT2);
         do {
             System.out.println("Making the request.");
 
             batchWriteItemRequest.withRequestItems(requestItems);
             result = getClient().batchWriteItem(batchWriteItemRequest);
 
             // Print consumed capacity units
             for(ConsumedCapacity entry : result.getConsumedCapacity()) {
                 String tableName1 = entry.getTableName();
                 Double consumedCapacityUnits = entry.getCapacityUnits();
                 System.out.println("Consumed capacity units for table " + tableName1 + ": " + consumedCapacityUnits);
             }
 
             // Check for unprocessed keys which could happen if you exceed provisioned throughput
             System.out.println("Unprocessed Put and Delete requests: \n" + result.getUnprocessedItems());
             requestItems = result.getUnprocessedItems();
         } while (result.getUnprocessedItems().size() > 0);
     }
 
     @Test
     public void batchWriteItemWithDeletionsTest() throws Exception{
         this.vanillaBatchWriteItemTest();
 
         BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
         BatchWriteItemResult result;
 
         // Create a map for the requests in the batch
         Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
 
         // Test: delete some items from database
         List<WriteRequest> forumList = new ArrayList<WriteRequest>();
 
         //Test case: Delete Requests
         Map<String, AttributeValue> forumKey3c =
             createItemKey(
                 hashKeyName1, new AttributeValue().withN("3"),
                 "range", new AttributeValue().withS("c"));
 
         Map<String, AttributeValue> forumKey5e =
             createItemKey(
                 hashKeyName1, new AttributeValue().withN("5"),
                 "range", new AttributeValue().withS("e"));
 
         Map<String, AttributeValue> forumKey6f =
             createItemKey(
                 hashKeyName1, new AttributeValue().withN("6"),
                 "range", new AttributeValue().withS("f"));
 
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey3c)));
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey5e)));
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey6f)));
 
         //Test on Table 2
         List<WriteRequest> forumListT2 = new ArrayList<WriteRequest>();
         Map<String, AttributeValue> forumKeyT2 =
             createItemKey(
                 hashKeyName2, new AttributeValue().withN("1"),
                 "range", new AttributeValue().withS("a"));
         forumListT2.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKeyT2)));
 
         requestItems.put(tableName1, forumList);
         requestItems.put(tableName2, forumListT2);
         do {
             System.out.println("Making the request.");
 
             batchWriteItemRequest.withRequestItems(requestItems);
             result = getClient().batchWriteItem(batchWriteItemRequest);
 
             // Print consumed capacity units
             for(ConsumedCapacity entry : result.getConsumedCapacity()) {
                 String tableName1 = entry.getTableName();
                 Double consumedCapacityUnits = entry.getCapacityUnits();
                 System.out.println("Consumed capacity units for table " + tableName1 + ": " + consumedCapacityUnits);
             }
 
             // Check for unprocessed keys which could happen if you exceed provisioned throughput
             System.out.println("Unprocessed Put and Delete requests: \n" + result.getUnprocessedItems());
             requestItems = result.getUnprocessedItems();
         } while (result.getUnprocessedItems().size() > 0);
     }
 
     @Test
     public void batchWriteItemWithDuplicateDeletionTest() throws Exception{
         this.vanillaBatchWriteItemTest();
 
         BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
         BatchWriteItemResult result;
 
         // Create a map for the requests in the batch
         Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
 
         // Test: delete some items from database
         List<WriteRequest> forumList = new ArrayList<WriteRequest>();
 
         //Test case: Delete Requests
         Map<String, AttributeValue> forumKey5e =
             createItemKey(
                 hashKeyName1, new AttributeValue().withN("5"),
                 "range", new AttributeValue().withS("e"));
 
         Map<String, AttributeValue> forumKey6f =
             createItemKey(
                 hashKeyName1, new AttributeValue().withN("6"),
                 "range", new AttributeValue().withS("f"));
 
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey5e)));
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey6f)));
 
         //Test case: Duplicated delete request
         forumList.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(forumKey5e)));
 
         requestItems.put(tableName1, forumList);
         System.out.println("Making the request.");
 
         batchWriteItemRequest.withRequestItems(requestItems);
 
         String exceptionMessage = null;
         try {
             result = getClient().batchWriteItem(batchWriteItemRequest);
         } catch (ResourceNotFoundException ex) {
             exceptionMessage = ex.getMessage();
         }
         // Question: Is this the actual behavior of DynamoDB?
         Assert.assertNotNull("Expected an exception.", exceptionMessage);
        Assert.assertEquals("Incorrect exception message.",
                "The item with hash key '5' doesn't exist in table '" + tableName1 + "'",
                exceptionMessage);
     }
 
     /*
     @Test
     public void batchGetItemInTableTest() {
         BatchGetItemResult result = client.batchGetItem(new BatchGetItemRequest());
         Assert.assertNotNull(result);
     }
     */
 }
