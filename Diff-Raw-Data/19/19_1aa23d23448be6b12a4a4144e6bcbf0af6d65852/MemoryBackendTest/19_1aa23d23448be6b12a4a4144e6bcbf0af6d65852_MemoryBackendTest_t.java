 package de.bwaldvogel;
 
 import static org.fest.assertions.Assertions.assertThat;
 import static org.fest.assertions.Fail.fail;
 
 import java.net.InetSocketAddress;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
 import java.util.Random;
 import java.util.UUID;
 import java.util.regex.Pattern;
 
 import org.bson.BSONObject;
 import org.bson.BasicBSONObject;
 import org.bson.types.ObjectId;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 import com.mongodb.BasicDBObject;
 import com.mongodb.BasicDBObjectBuilder;
 import com.mongodb.CommandResult;
 import com.mongodb.DB;
 import com.mongodb.DBCollection;
 import com.mongodb.DBCursor;
 import com.mongodb.DBObject;
 import com.mongodb.Mongo;
 import com.mongodb.MongoClient;
 import com.mongodb.MongoException;
 import com.mongodb.ServerAddress;
 import com.mongodb.WriteConcern;
 import com.mongodb.WriteResult;
 
 import de.bwaldvogel.mongo.MongoServer;
 import de.bwaldvogel.mongo.backend.Constants;
 import de.bwaldvogel.mongo.backend.Utils;
 import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
 
 public class MemoryBackendTest {
 
     private Mongo client;
     private MongoServer mongoServer;
     private DB db;
     private DBCollection collection;
     private DB admin;
 
     private CommandResult command(String command) {
         return admin.command(command);
     }
 
     @Before
     public void setUp() throws Exception {
         mongoServer = new MongoServer(new MemoryBackend());
         InetSocketAddress serverAddress = mongoServer.bind();
         client = new MongoClient(new ServerAddress(serverAddress));
         db = client.getDB("testdb");
         admin = client.getDB("admin");
         collection = db.getCollection("testcoll");
     }
 
     @After
     public void tearDown() {
         client.close();
         mongoServer.shutdownNow();
     }
 
     @Test
     public void testUnsupportedModifier() throws Exception {
         collection.insert(new BasicDBObject());
         try {
             collection.update(new BasicDBObject(), new BasicDBObject("$foo", new BasicDBObject()));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10147);
             assertThat(e.getMessage()).isEqualTo("Invalid modifier specified: $foo");
         }
     }
 
     @Test
     public void testAnotherUpsert() {
         BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().push("_id").append("f", "ca").push("1")
                 .append("l", 2).pop().push("t").append("t", 11).pop().pop();
         DBObject query = queryBuilder.get();
 
         // { _id: { f: ca, 1: { l: 2 }, t: { t: 11 } } }
         DBObject update = BasicDBObjectBuilder.start().push("$inc").append("n.!", 1).append("n.a.b:false", 1).pop()
                 .get();
 
         // { "$inc" : { "n.!" : 1 , "n.a.b:false" : 1}}
         collection.update(query, update, true, false);
 
         DBObject expected = queryBuilder.push("n").append("!", 1).push("a").append("b:false", 1).pop().pop().get();
         assertThat(collection.findOne()).isEqualTo(expected);
     }
 
     @Test
     public void testBasicUpdate() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2).append("b", 5));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
 
         collection.update(new BasicDBObject("_id", 2), new BasicDBObject("a", 5));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 2))).isEqualTo(
                 new BasicDBObject("_id", 2).append("a", 5));
     }
 
     @Test
     public void testCollectionStats() throws Exception {
         CommandResult stats = collection.getStats();
         assertThat(stats.ok()).isFalse();
         assertThat(stats.getErrorMessage()).isEqualTo("ns not found");
 
         collection.insert(new BasicDBObject());
         collection.insert(new BasicDBObject("abc", "foo"));
         stats = collection.getStats();
         stats.throwOnError();
         assertThat(((Number) stats.get("count")).longValue()).isEqualTo(2);
         assertThat(((Number) stats.get("size")).longValue()).isEqualTo(57);
         assertThat(((Number) stats.get("avgObjSize")).doubleValue()).isEqualTo(28.5);
     }
 
     @Test
     public void testCompoundDateIdUpserts() {
         DBObject query = new BasicDBObjectBuilder().push("_id").push("$lt").add("n", "a").add("t", 10).pop()
                 .push("$gte").add("n", "a").add("t", 1).pop().pop().get();
         List<BasicDBObject> toUpsert = Arrays.asList(
                 new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", 1)), new BasicDBObject("_id",
                         new BasicDBObject("n", "a").append("t", 2)), new BasicDBObject("_id", new BasicDBObject("n",
                         "a").append("t", 3)), new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", 11)));
         for (BasicDBObject dbo : toUpsert) {
             collection.update(dbo, ((BasicDBObject) dbo.copy()).append("foo", "bar"), true, false);
         }
         List<DBObject> results = collection.find(query).toArray();
         assertThat(results).containsExactly(
                 new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", 1)).append("foo", "bar"),
                 new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", 2)).append("foo", "bar"),
                 new BasicDBObject("_id", new BasicDBObject("n", "a").append("t", 3)).append("foo", "bar"));
     }
 
     @Test
     public void testCompoundSort() {
         collection.insert(new BasicDBObject("a", 1).append("_id", 1));
         collection.insert(new BasicDBObject("a", 2).append("_id", 5));
         collection.insert(new BasicDBObject("a", 1).append("_id", 2));
         collection.insert(new BasicDBObject("a", 2).append("_id", 4));
         collection.insert(new BasicDBObject("a", 1).append("_id", 3));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find().sort(new BasicDBObject("a", 1).append("_id", -1));
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("a", 1).append("_id", 3),
                 new BasicDBObject("a", 1).append("_id", 2), new BasicDBObject("a", 1).append("_id", 1),
                 new BasicDBObject("a", 2).append("_id", 5), new BasicDBObject("a", 2).append("_id", 4));
     }
 
     @Test
     public void testCountCommand() {
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testCountWithQueryCommand() {
         collection.insert(new BasicDBObject("n", 1));
         collection.insert(new BasicDBObject("n", 2));
         collection.insert(new BasicDBObject("n", 2));
         assertThat(collection.count(new BasicDBObject("n", 2))).isEqualTo(2);
     }
 
     @Test
     public void testCreateIndexes() {
         collection.ensureIndex("n");
         collection.ensureIndex("b");
         List<DBObject> indexes = db.getCollection("system.indexes").find().toArray();
         assertThat(indexes).containsExactly(
                 new BasicDBObject("key", new BasicDBObject("_id", 1)).append("ns", collection.getFullName()).append(
                         "name", "_id_"),
                 new BasicDBObject("key", new BasicDBObject("n", 1)).append("ns", collection.getFullName()).append(
                         "name", "n_1"),
                 new BasicDBObject("key", new BasicDBObject("b", 1)).append("ns", collection.getFullName()).append(
                         "name", "b_1"));
     }
 
     @Test
     public void testCurrentOperations() throws Exception {
         DBObject currentOperations = admin.getCollection("$cmd.sys.inprog").findOne();
         assertThat(currentOperations).isNotNull();
         assertThat(currentOperations.get("inprog")).isInstanceOf(List.class);
     }
 
     @Test
     public void testDatabaseStats() throws Exception {
         CommandResult stats = db.getStats();
         stats.throwOnError();
         assertThat(((Number) stats.get("objects")).longValue()).isEqualTo(1);
         assertThat(((Number) stats.get("collections")).longValue()).isEqualTo(1);
         assertThat(((Number) stats.get("indexes")).longValue()).isEqualTo(0);
         assertThat(((Number) stats.get("dataSize")).longValue()).isEqualTo(37);
 
         db.getCollection("foo").insert(new BasicDBObject());
         db.getCollection("foo").insert(new BasicDBObject());
         db.getCollection("bar").insert(new BasicDBObject());
 
         stats = db.getStats();
         stats.throwOnError();
 
         assertThat(((Number) stats.get("objects")).longValue()).isEqualTo(8);
         assertThat(((Number) stats.get("collections")).longValue()).isEqualTo(3);
         assertThat(((Number) stats.get("indexes")).longValue()).isEqualTo(2);
         assertThat(((Number) stats.get("dataSize")).longValue()).isEqualTo(271);
     }
 
     @Test
     public void testDeleteDecrementsCount() {
         collection.insert(new BasicDBObject("key", "value"));
         assertThat(collection.count()).isEqualTo(1);
         collection.remove(new BasicDBObject());
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testDeleteInSystemNamespace() throws Exception {
         try {
             db.getCollection("system.foobar").remove(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(12050);
             assertThat(e.getMessage()).startsWith("cannot delete from system namespace");
         }
 
         try {
             db.getCollection("system.namespaces").remove(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(12050);
             assertThat(e.getMessage()).startsWith("cannot delete from system namespace");
         }
     }
 
     @Test
     public void testUpdateInSystemNamespace() throws Exception {
         try {
             db.getCollection("system.foobar").update(new BasicDBObject(), new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10156);
             assertThat(e.getMessage()).startsWith("cannot update system collection");
         }
 
         try {
             db.getCollection("system.namespaces").update(new BasicDBObject(), new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10156);
             assertThat(e.getMessage()).startsWith("cannot update system collection");
         }
     }
 
     @Test
     public void testDistinctQuery() {
         collection.insert(new BasicDBObject("n", 3));
         collection.insert(new BasicDBObject("n", 1));
         collection.insert(new BasicDBObject("n", 2));
         collection.insert(new BasicDBObject("n", 1));
         collection.insert(new BasicDBObject("n", 1));
         assertThat(collection.distinct("n")).containsExactly(1, 2, 3);
         assertThat(collection.distinct("foobar")).isEmpty();
         assertThat(collection.distinct("_id")).hasSize((int) collection.count());
     }
 
     @Test
     public void testDropCollection() throws Exception {
         collection.insert(new BasicDBObject());
         assertThat(db.getCollectionNames()).contains(collection.getName());
         collection.drop();
         assertThat(db.getCollectionNames()).excludes(collection.getName());
     }
 
     @Test
     public void testDropCollectionAlsoDropsFromDB() throws Exception {
         collection.insert(new BasicDBObject());
         collection.drop();
         assertThat(collection.count()).isZero();
         assertThat(db.getCollectionNames()).excludes(collection.getName());
     }
 
     @Test
     public void testDropDatabaseAlsoDropsCollectionData() throws Exception {
         collection.insert(new BasicDBObject());
         db.dropDatabase();
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testDropDatabaseDropsAllData() throws Exception {
         collection.insert(new BasicDBObject());
         DBCollection collection2 = db.getCollection("testcoll2");
         collection2.insert(new BasicDBObject());
 
         client.dropDatabase(db.getName());
         assertThat(client.getDatabaseNames()).excludes(db.getName());
         assertThat(collection.count()).isZero();
         assertThat(db.getCollectionNames()).excludes(collection.getName(), collection2.getName());
     }
 
     @Test
     public void testEmbeddedSort() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4).append("counts", new BasicDBObject("done", 1)));
         collection.insert(new BasicDBObject("_id", 5).append("counts", new BasicDBObject("done", 2)));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find(new BasicDBObject("c", new BasicDBObject("$ne", true))).sort(
                 new BasicDBObject("counts.done", -1));
         assertThat(cursor.toArray()).containsExactly(
                 new BasicDBObject("_id", 5).append("counts", new BasicDBObject("done", 2)),
                 new BasicDBObject("_id", 4).append("counts", new BasicDBObject("done", 1)),
                 new BasicDBObject("_id", 1), new BasicDBObject("_id", 2), new BasicDBObject("_id", 3));
     }
 
     @Test
     public void testFindAndModifyCommandEmpty() throws Exception {
         DBObject cmd = new BasicDBObject("findandmodify", collection.getName());
         CommandResult result = db.command(cmd);
         assertThat(result.getErrorMessage()).isEqualTo("need remove or update");
         assertThat(result.ok()).isFalse();
     }
 
     @Test
     public void testFindAndModifyCommandIllegalOp() throws Exception {
         collection.insert(new BasicDBObject("_id", 1));
 
         DBObject cmd = new BasicDBObject("findAndModify", collection.getName());
         cmd.put("query", new BasicDBObject("_id", 1));
         cmd.put("update", new BasicDBObject("$inc", new BasicDBObject("_id", 1)));
 
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1));
 
         CommandResult result = db.command(cmd);
         try {
             result.throwOnError();
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10148);
             assertThat(e.getMessage()).contains("Mod on _id not allowed");
         }
     }
 
     @Test
     public void testFindAndModifyCommandUpdate() throws Exception {
         collection.insert(new BasicDBObject("_id", 1));
 
         DBObject cmd = new BasicDBObject("findAndModify", collection.getName());
         cmd.put("query", new BasicDBObject("_id", 1));
         cmd.put("update", new BasicDBObject("$inc", new BasicDBObject("a", 1)));
 
         CommandResult result = db.command(cmd);
         assertThat(result.get("lastErrorObject")).isEqualTo(
                 new BasicDBObject("updatedExisting", Boolean.TRUE).append("n", 1));
 
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
         assertThat(result.ok()).isTrue();
     }
 
     @Test
     public void testFindAndModifyError() throws Exception {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
 
         try {
             collection.findAndModify(new BasicDBObject("_id", 1), null, null, false, new BasicDBObject("$inc",
                     new BasicDBObject("_id", 1)), false, false);
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10148);
             assertThat(e.getMessage()).contains("command failed [findandmodify]");
             assertThat(e.getMessage()).contains("Mod on _id not allowed");
         }
     }
 
     @Test
     public void testFindAndModifyFields() throws Exception {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), new BasicDBObject("_id", 1), null,
                 false, new BasicDBObject("$inc", new BasicDBObject("a", 1)), true, false);
 
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1));
     }
 
     @Test
     public void testFindAndModifyNotFound() throws Exception {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 2), null, null, false, new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), false, false);
 
         assertThat(result).isNull();
         assertThat(collection.count()).isEqualTo(1);
     }
 
     @Test
     public void testFindAndModifyRemove() {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), null, null, true, null, false, false);
 
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testFindAndModifyReturnNew() {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), null, null, false, new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), true, false);
 
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1).append("a", 2));
     }
 
     @Test
     public void testFindAndModifyReturnOld() {
         collection.insert(new BasicDBObject("_id", 1).append("a", 1));
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), null, null, false, new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), false, false);
 
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("a", 2));
     }
 
     @Test
     public void testFindAndModifySorted() throws Exception {
         collection.insert(new BasicDBObject("_id", 1).append("a", 15));
         collection.insert(new BasicDBObject("_id", 2).append("a", 10));
         collection.insert(new BasicDBObject("_id", 3).append("a", 20));
 
         DBObject order = new BasicDBObject("a", 1);
         DBObject result = collection.findAndModify(new BasicDBObject(), null, order, false, new BasicDBObject("$inc",
                 new BasicDBObject("a", 1)), true, false);
         assertThat(result).isEqualTo(new BasicDBObject("_id", 2).append("a", 11));
 
         order = new BasicDBObject("a", -1);
         result = collection.findAndModify(new BasicDBObject(), null, order, false, new BasicDBObject("$inc",
                 new BasicDBObject("a", 1)), true, false);
         assertThat(result).isEqualTo(new BasicDBObject("_id", 3).append("a", 21));
 
     }
 
     @Test
     public void testFindAndModifyUpsert() {
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), null, null, false, new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), true, true);
 
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
     }
 
     @Test
     public void testFindAndModifyUpsertReturnNewFalse() {
         DBObject result = collection.findAndModify(new BasicDBObject("_id", 1), null, null, false, new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), false, true);
 
         assertThat(result).isEqualTo(new BasicDBObject());
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
     }
 
     @Test
     public void testFindAndRemoveFromEmbeddedList() {
         BasicDBObject obj = new BasicDBObject("_id", 1).append("a", Arrays.asList(1));
         collection.insert(obj);
         DBObject result = collection.findAndRemove(new BasicDBObject("_id", 1));
         assertThat(result).isEqualTo(obj);
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testFindOne() {
         collection.insert(new BasicDBObject("key", "value"));
         DBObject result = collection.findOne();
         assertThat(result).isNotNull();
         assertThat(result.get("_id")).isNotNull();
     }
 
     @Test
     public void testFindOneById() {
         collection.insert(new BasicDBObject("_id", 1));
         DBObject result = collection.findOne(new BasicDBObject("_id", 1));
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1));
         assertThat(collection.findOne(new BasicDBObject("_id", 2))).isNull();
     }
 
     @Test
     public void testFindOneIn() {
         collection.insert(new BasicDBObject("_id", 1));
         DBObject result = collection.findOne(new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(1, 2))));
         assertThat(result).isEqualTo(new BasicDBObject("_id", 1));
     }
 
     @Test
     public void testFindWithLimit() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find().limit(2);
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2));
     }
 
     @Test
     public void testFindWithPattern() {
         collection.insert(new BasicDBObject("_id", "marta"));
         collection.insert(new BasicDBObject("_id", "john").append("foo", "bar"));
         collection.insert(new BasicDBObject("_id", "jon").append("foo", "ba"));
         collection.insert(new BasicDBObject("_id", "jo"));
 
         assertThat(collection.find(new BasicDBObject("_id", Pattern.compile("mart"))).toArray()).containsExactly(
                 new BasicDBObject("_id", "marta"));
 
         assertThat(collection.find(new BasicDBObject("foo", Pattern.compile("ba"))).toArray()).containsExactly(
                 new BasicDBObject("_id", "john").append("foo", "bar"),
                 new BasicDBObject("_id", "jon").append("foo", "ba"));
 
         assertThat(collection.find(new BasicDBObject("foo", Pattern.compile("ba$"))).toArray()).containsExactly(
                 new BasicDBObject("_id", "jon").append("foo", "ba"));
     }
 
     @Test
     public void testFindWithQuery() {
         collection.insert(new BasicDBObject("name", "jon"));
         collection.insert(new BasicDBObject("name", "leo"));
         collection.insert(new BasicDBObject("name", "neil"));
         collection.insert(new BasicDBObject("name", "neil"));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find(new BasicDBObject("name", "neil"));
         assertThat(cursor.toArray()).hasSize(2);
     }
 
     @Test
     public void testFindWithSkipLimit() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find().limit(2).skip(2);
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("_id", 3), new BasicDBObject("_id", 4));
     }
 
     @Test
     public void testFindWithSkipLimitAfterDelete() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
         collection.insert(new BasicDBObject("_id", 5));
 
         collection.remove(new BasicDBObject("_id", 1));
         collection.remove(new BasicDBObject("_id", 3));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find().limit(2).skip(2);
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("_id", 5));
     }
 
     @Test
     public void testFullUpdateWithSameId() throws Exception {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2).append("b", 5));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
 
         collection.update(new BasicDBObject("_id", 2).append("b", 5), new BasicDBObject("_id", 2).append("a", 5));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 2))).isEqualTo(
                 new BasicDBObject("_id", 2).append("a", 5));
     }
 
     @Test
     public void testGetCollection() {
         DBCollection collection = db.getCollection("coll");
         db.getCollection("coll").insert(new BasicDBObject());
 
         assertThat(collection).isNotNull();
         assertThat(db.getCollection("coll")).isSameAs(collection);
         assertThat(db.getCollectionFromString("coll")).isSameAs(collection);
         assertThat(db.getCollectionNames()).contains("coll");
     }
 
     @Test
     public void testGetLastError() {
         WriteResult result = collection.insert(new BasicDBObject("_id", 1));
         CommandResult error = db.getLastError();
         assertThat(error.ok()).isTrue();
         assertThat(error).isEqualTo(result.getCachedLastError());
 
         assertThat(db.getLastError()).isEqualTo(error);
 
         assertThat(db.command("illegalCommand").ok()).isFalse();
 
         // getlasterror must succeed again
         assertThat(db.getLastError().ok()).isTrue();
     }
 
     /**
      * Test that ObjectId is getting generated even if _id is present in
      * DBObject but it's value is null
      *
      * @throws Exception
      */
     @Test
     public void testIdGenerated() throws Exception {
         DBObject toSave = new BasicDBObject();
         toSave.put("_id", null);
         toSave.put("name", "test");
 
         collection.save(toSave);
         DBObject result = collection.findOne(new BasicDBObject("name", "test"));
         assertThat(result.get(Constants.ID_FIELD)).isInstanceOf(ObjectId.class);
     }
 
     @Test
     public void testIdInQueryResultsInIndexOrder() {
         collection.insert(new BasicDBObject("_id", 4));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find(new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(3, 2, 1))));
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2),
                 new BasicDBObject("_id", 3));
     }
 
     @Test
     public void testIdNotAllowedToBeUpdated() {
         collection.insert(new BasicDBObject("_id", 1));
 
         try {
             collection.update(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2).append("a", 5));
             fail("should throw exception");
         } catch (MongoException e) {
             assertThat(e.getMessage()).isEqualTo(
                     "cannot change _id of a document old:{ \"_id\" : 1} new:{ \"_id\" : 2}");
         }
 
         // test with $set
 
         try {
             collection.update(new BasicDBObject("_id", 1), new BasicDBObject("$set", new BasicDBObject("_id", 2)));
             fail("should throw exception");
         } catch (MongoException e) {
             assertThat(e.getMessage()).isEqualTo("Mod on _id not allowed");
         }
     }
 
     @Test
     public void testIllegalCommand() throws Exception {
         try {
             command("foo").throwOnError();
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("no such cmd: foo");
         }
 
         try {
             client.getDB("bar").command("foo").throwOnError();
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("no such cmd: foo");
         }
     }
 
     @Test
     public void testInsert() throws Exception {
         assertThat(collection.count()).isEqualTo(0);
 
         for (int i = 0; i < 3; i++) {
             collection.insert(new BasicDBObject("_id", Integer.valueOf(i)));
         }
 
         assertThat(collection.count()).isEqualTo(3);
 
         WriteResult result = collection.insert(new BasicDBObject("foo", Arrays.asList(1, 2, 3)));
         assertThat(result.getN()).isZero();
         assertThat(result.getField("updatedExisting")).isNull();
 
         collection.insert(new BasicDBObject("foo", new byte[10]));
         BasicDBObject insertedObject = new BasicDBObject("foo", UUID.randomUUID());
         collection.insert(insertedObject);
         assertThat(collection.findOne(insertedObject)).isEqualTo(insertedObject);
     }
 
     @Test
     public void testInsertDuplicate() throws Exception {
         assertThat(collection.count()).isEqualTo(0);
 
         collection.insert(new BasicDBObject("_id", 1));
         assertThat(collection.count()).isEqualTo(1);
 
         try {
             collection.insert(new BasicDBObject("_id", 1));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("duplicate key error");
         }
 
         try {
             collection.insert(new BasicDBObject("_id", 1.0));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("duplicate key error");
         }
 
         assertThat(collection.count()).isEqualTo(1);
     }
 
     @Test(expected = MongoException.DuplicateKey.class)
     public void testInsertDuplicateThrows() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 1));
     }
 
     @Test(expected = MongoException.DuplicateKey.class)
     public void testInsertDuplicateWithConcernThrows() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 1), WriteConcern.SAFE);
     }
 
     @Test
     public void testInsertIncrementsCount() {
         assertThat(collection.count()).isZero();
         collection.insert(new BasicDBObject("key", "value"));
         assertThat(collection.count()).isEqualTo(1);
     }
 
     @Test
     public void testInsertQuery() throws Exception {
         assertThat(collection.count()).isEqualTo(0);
 
         BasicDBObject insertedObject = new BasicDBObject("_id", 1);
         insertedObject.put("foo", "bar");
         collection.insert(insertedObject);
 
         assertThat(collection.findOne(insertedObject)).isEqualTo(insertedObject);
         assertThat(collection.findOne(new BasicDBObject("_id", 1l))).isEqualTo(insertedObject);
         assertThat(collection.findOne(new BasicDBObject("_id", 1.0))).isEqualTo(insertedObject);
         assertThat(collection.findOne(new BasicDBObject("_id", 1.0001))).isNull();
         assertThat(collection.findOne(new BasicDBObject("foo", "bar"))).isEqualTo(insertedObject);
     }
 
     @Test
     public void testInsertRemove() throws Exception {
         for (int i = 0; i < 10; i++) {
             collection.insert(new BasicDBObject("_id", 1));
             assertThat(collection.count()).isEqualTo(1);
             collection.remove(new BasicDBObject("_id", 1));
             assertThat(collection.count()).isZero();
 
             collection.insert(new BasicDBObject("_id", i));
             collection.remove(new BasicDBObject("_id", i));
         }
         assertThat(collection.count()).isZero();
         collection.remove(new BasicDBObject("doesnt exist", 1));
         assertThat(collection.count()).isZero();
     }
 
     @Test
     public void testInsertInSystemNamespace() throws Exception {
         try {
             db.getCollection("system.foobar").insert(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(16459);
             assertThat(e.getMessage()).startsWith("attempt to insert in system namespace");
         }
 
         try {
             db.getCollection("system.namespaces").insert(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(16459);
             assertThat(e.getMessage()).startsWith("attempt to insert in system namespace");
         }
     }
 
     @Test
     public void testListDatabaseNames() throws Exception {
         assertThat(client.getDatabaseNames()).isEmpty();
         db.getCollection(collection.getName()).insert(new BasicDBObject());
         assertThat(client.getDatabaseNames()).containsExactly(db.getName());
         client.getDB("bar").getCollection(collection.getName()).insert(new BasicDBObject());
         assertThat(client.getDatabaseNames()).containsExactly("bar", db.getName());
     }
 
     @Test
     public void testMaxBsonSize() throws Exception {
         int maxBsonObjectSize = client.getMaxBsonObjectSize();
         assertThat(maxBsonObjectSize).isEqualTo(16777216);
     }
 
     @Test
     public void testQuery() throws Exception {
         DBObject obj = collection.findOne(new BasicDBObject("_id", 1));
         assertThat(obj).isNull();
         assertThat(collection.count()).isEqualTo(0);
     }
 
     @Test
     public void testQueryAll() throws Exception {
         List<Object> inserted = new ArrayList<Object>();
         for (int i = 0; i < 10; i++) {
             BasicDBObject obj = new BasicDBObject("_id", i);
             collection.insert(obj);
             inserted.add(obj);
         }
         assertThat(collection.count()).isEqualTo(10);
 
         assertThat(collection.find().toArray()).isEqualTo(inserted);
     }
 
     @Test
     public void testQueryCount() throws Exception {
         for (int i = 0; i < 100; i++) {
             collection.insert(new BasicDBObject());
         }
         assertThat(collection.count()).isEqualTo(100);
 
         BasicDBObject obj = new BasicDBObject("_id", 1);
         assertThat(collection.find(obj).count()).isEqualTo(0);
         collection.insert(obj);
         assertThat(collection.find(obj).count()).isEqualTo(1);
     }
 
     @Test
     public void testQueryLimit() throws Exception {
         for (int i = 0; i < 5; i++) {
             collection.insert(new BasicDBObject());
         }
         List<DBObject> objects = collection.find().limit(1).toArray();
         assertThat(objects.size()).isEqualTo(1);
     }
 
     @Test
     public void testQueryNull() throws Exception {
         BasicDBObject object = new BasicDBObject("_id", 1);
         collection.insert(object);
         assertThat(collection.findOne(new BasicDBObject("foo", null))).isEqualTo(object);
     }
 
     @Test
     @SuppressWarnings("resource")
     public void testQuerySkipLimit() throws Exception {
         for (int i = 0; i < 10; i++) {
             collection.insert(new BasicDBObject());
         }
 
         DBCursor cursor = collection.find().skip(3);
         assertThat(cursor.itcount()).isEqualTo(7);
 
         cursor = collection.find().skip(3).limit(5);
         assertThat(cursor.itcount()).isEqualTo(5);
     }
 
     @Test
     public void testQuerySort() throws Exception {
         Random random = new Random(4711);
         for (int i = 0; i < 10; i++) {
             collection.insert(new BasicDBObject("_id", Double.valueOf(random.nextDouble())));
         }
 
         List<DBObject> objects = collection.find().sort(new BasicDBObject("_id", Integer.valueOf(1))).toArray();
         double before = Double.MIN_VALUE;
         for (DBObject dbObject : objects) {
             double value = ((Number) dbObject.get("_id")).doubleValue();
             assertThat(value).isGreaterThanOrEqualTo(before);
             before = value;
         }
 
         // reverse sort
         objects = collection.find().sort(new BasicDBObject("_id", Integer.valueOf(-1))).toArray();
         before = Double.MAX_VALUE;
         for (DBObject dbObject : objects) {
             double value = ((Number) dbObject.get("_id")).doubleValue();
             assertThat(value).isLessThanOrEqualTo(before);
             before = value;
         }
     }
 
     @Test
     public void testQueryWithFieldSelector() throws Exception {
         collection.insert(new BasicDBObject("foo", "bar"));
         DBObject obj = collection.findOne(new BasicDBObject(), new BasicDBObject("foo", 1));
         assertThat(obj.keySet()).containsOnly("_id", "foo");
 
         obj = collection.findOne(new BasicDBObject("foo", "bar"), new BasicDBObject("_id", 1));
         assertThat(obj.keySet()).containsOnly("_id");
 
         obj = collection.findOne(new BasicDBObject("foo", "bar"), new BasicDBObject("_id", 0).append("foo", 1));
         assertThat(obj.keySet()).containsOnly("foo");
     }
 
     @Test
     public void testQuerySystemNamespace() throws Exception {
         assertThat(db.getCollection("system.foobar").findOne()).isNull();
         assertThat(db.getCollectionNames()).containsOnly("system.indexes");
 
         collection.insert(new BasicDBObject());
         BasicDBObject expectedObj = new BasicDBObject("name", collection.getFullName());
         DBObject coll = db.getCollection("system.namespaces").findOne(expectedObj);
         assertThat(coll).isEqualTo(expectedObj);
     }
 
     @Test
     public void testRemove() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.insert(new BasicDBObject("_id", 2));
         collection.insert(new BasicDBObject("_id", 3));
         collection.insert(new BasicDBObject("_id", 4));
 
         collection.remove(new BasicDBObject("_id", 2));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 2))).isNull();
     }
 
     @Test
     public void testRemoveReturnsModifiedDocumentCount() {
         collection.insert(new BasicDBObject());
         collection.insert(new BasicDBObject());
 
         WriteResult result = collection.remove(new BasicDBObject());
         assertThat(result.getN()).isEqualTo(2);
     }
 
     @Test
     public void testReservedCollectionNames() throws Exception {
         try {
             db.getCollection("foo$bar").insert(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("cannot insert into reserved $ collection");
         }
 
         try {
             db.getCollection("").insert(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage().toLowerCase()).contains("invalid ns");
         }
 
         try {
             db.getCollection(
                     "verylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstringverylongstring")
                     .insert(new BasicDBObject());
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getMessage()).contains("name too long");
         }
     }
 
     @Test
     public void testSave() {
         BasicDBObject inserted = new BasicDBObject("_id", 1);
         collection.insert(inserted);
         collection.save(inserted);
     }
 
     @Test
     public void testServerStatus() throws Exception {
         Date before = new Date();
         CommandResult serverStatus = command("serverStatus");
         serverStatus.throwOnError();
         assertThat(serverStatus.get("uptime")).isInstanceOf(Number.class);
         assertThat(serverStatus.get("uptimeMillis")).isInstanceOf(Long.class);
         Date serverTime = (Date) serverStatus.get("localTime");
         assertThat(serverTime).isNotNull();
         assertThat(serverTime.after(new Date())).isFalse();
         assertThat(before.after(serverTime)).isFalse();
 
         BSONObject connections = (BSONObject) serverStatus.get("connections");
         assertThat(connections.get("current")).isEqualTo(Integer.valueOf(1));
     }
 
     @Test
     public void testReplSetGetStatus() throws Exception {
         CommandResult result = command("replSetGetStatus");
         assertThat(result.ok()).isFalse();
         assertThat(result.getErrorMessage()).isEqualTo("not running with --replSet");
     }
 
     @Test
     public void testWhatsMyUri() throws Exception {
         for (String dbname : new String[] { "admin", "local", "test" }) {
             CommandResult result = client.getDB(dbname).command("whatsmyuri");
             result.throwOnError();
             assertThat(result.ok()).isTrue();
             assertThat(result.get("you")).isNotNull();
             assertThat(result.get("you").toString()).startsWith("127.0.0.1:");
         }
     }
 
     @Test
     public void testSort() {
         collection.insert(new BasicDBObject("a", 1).append("_id", 1));
         collection.insert(new BasicDBObject("a", 2).append("_id", 2));
         collection.insert(new BasicDBObject("_id", 5));
         collection.insert(new BasicDBObject("a", 3).append("_id", 3));
         collection.insert(new BasicDBObject("a", 4).append("_id", 4));
 
         @SuppressWarnings("resource")
         DBCursor cursor = collection.find().sort(new BasicDBObject("a", -1));
         assertThat(cursor.toArray()).containsExactly(new BasicDBObject("a", 4).append("_id", 4),
                 new BasicDBObject("a", 3).append("_id", 3), new BasicDBObject("a", 2).append("_id", 2),
                 new BasicDBObject("a", 1).append("_id", 1), new BasicDBObject("_id", 5));
     }
 
     @Test
     public void testSortByEmbeddedKey() {
         collection.insert(new BasicDBObject("_id", 1).append("a", new BasicDBObject("b", 1)));
         collection.insert(new BasicDBObject("_id", 2).append("a", new BasicDBObject("b", 2)));
         collection.insert(new BasicDBObject("_id", 3).append("a", new BasicDBObject("b", 3)));
         List<DBObject> results = collection.find().sort(new BasicDBObject("a.b", -1)).toArray();
         assertThat(results).containsExactly(new BasicDBObject("_id", 3).append("a", new BasicDBObject("b", 3)),
                 new BasicDBObject("_id", 2).append("a", new BasicDBObject("b", 2)),
                 new BasicDBObject("_id", 1).append("a", new BasicDBObject("b", 1)));
     }
 
     @Test
     public void testUpdate() throws Exception {
         BasicDBObject object = new BasicDBObject("_id", 1);
 
         BasicDBObject newObject = new BasicDBObject("_id", 1);
         newObject.put("foo", "bar");
 
         collection.insert(object);
         WriteResult result = collection.update(object, newObject);
         assertThat(result.getN()).isEqualTo(1);
         assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.TRUE);
         assertThat(collection.findOne(object)).isEqualTo(newObject);
     }
 
     @Test
     public void testUpdateNothing() throws Exception {
         BasicDBObject object = new BasicDBObject("_id", 1);
         WriteResult result = collection.update(object, object);
         assertThat(result.getN()).isEqualTo(0);
         assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.FALSE);
     }
 
     @Test
     public void testUpdateSubdocument() throws Exception {
         try {
             collection.update(new BasicDBObject(), new BasicDBObject("a.b.c", 123));
             fail("IllegalArgumentException expected");
         } catch (IllegalArgumentException e) {
             assertThat(e.getMessage()).contains("Bad Key");
         }
     }
 
     @Test
     public void testUpdateIdNoChange() {
         collection.insert(new BasicDBObject("_id", 1));
         collection.update(new BasicDBObject("_id", 1), new BasicDBObject("_id", 1).append("a", 5));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 1))).isEqualTo(
                 new BasicDBObject("_id", 1).append("a", 5));
 
         collection.update(new BasicDBObject("_id", 1),
                 new BasicDBObject("$set", new BasicDBObject("_id", 1).append("b", 3)));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 1))).isEqualTo(
                 new BasicDBObject("_id", 1).append("a", 5).append("b", 3));
 
         // test with $set
 
         collection.update(new BasicDBObject("_id", 1),
                 new BasicDBObject("$set", new BasicDBObject("_id", 1).append("a", 7)));
 
         assertThat(collection.findOne(new BasicDBObject("_id", 1))).isEqualTo(
                 new BasicDBObject("_id", 1).append("a", 7).append("b", 3));
     }
 
     @Test
     public void testUpdatePush() throws Exception {
         BasicDBObject idObj = new BasicDBObject("_id", 1);
         collection.insert(idObj);
         collection.update(idObj, new BasicDBObject("$push", new BasicDBObject("field.subfield.subsubfield", "value")));
         DBObject expected = new BasicDBObject("_id", 1).append("field", new BasicDBObject("subfield",
                 new BasicDBObject("subsubfield", Arrays.asList("value"))));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
 
         // push to non-array
         collection.update(idObj, new BasicDBObject("$set", new BasicDBObject("field", "value")));
         try {
             collection.update(idObj, new BasicDBObject("$push", new BasicDBObject("field", "value")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10141);
             assertThat(e.getMessage()).isEqualTo("Cannot apply $push modifier to non-array");
         }
 
         // push with multiple fields
 
         DBObject pushObj = new BasicDBObject("$push", new BasicDBObject("field1", "value").append("field2", "value2"));
         collection.update(idObj, pushObj);
 
         expected = new BasicDBObject("_id", 1).append("field", "value") //
                 .append("field1", Arrays.asList("value")) //
                 .append("field2", Arrays.asList("value2"));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
 
         // push duplicate
         pushObj = new BasicDBObject("$push", new BasicDBObject("field1", "value"));
         collection.update(idObj, pushObj);
         expected.put("field1", Arrays.asList("value", "value"));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
     }
 
     @Test
     public void testUpdatePushAll() throws Exception {
         DBObject idObj = new BasicDBObject("_id", 1);
         collection.insert(idObj);
         try {
             collection.update(idObj, new BasicDBObject("$pushAll", new BasicDBObject("field", "value")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10153);
             assertThat(e.getMessage()).isEqualTo("Modifier $pushAll allowed for arrays only");
         }
 
         collection.update(idObj, new BasicDBObject("$pushAll", //
                 new BasicDBObject("field", Arrays.asList("value", "value2"))));
         DBObject expected = new BasicDBObject("_id", 1).append("field", Arrays.asList("value", "value2"));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
     }
 
     @Test
     public void testUpdateAddToSet() throws Exception {
         BasicDBObject idObj = new BasicDBObject("_id", 1);
         collection.insert(idObj);
         collection.update(idObj, new BasicDBObject("$addToSet",
                 new BasicDBObject("field.subfield.subsubfield", "value")));
         DBObject expected = new BasicDBObject("_id", 1).append("field", new BasicDBObject("subfield",
                 new BasicDBObject("subsubfield", Arrays.asList("value"))));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
 
         // addToSet to non-array
         collection.update(idObj, new BasicDBObject("$set", new BasicDBObject("field", "value")));
         try {
             collection.update(idObj, new BasicDBObject("$addToSet", new BasicDBObject("field", "value")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10141);
             assertThat(e.getMessage()).isEqualTo("Cannot apply $addToSet modifier to non-array");
         }
 
         // addToSet with multiple fields
 
         DBObject pushObj = new BasicDBObject("$addToSet", new BasicDBObject("field1", "value").append("field2",
                 "value2"));
         collection.update(idObj, pushObj);
 
         expected = new BasicDBObject("_id", 1).append("field", "value") //
                 .append("field1", Arrays.asList("value")) //
                 .append("field2", Arrays.asList("value2"));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
 
         // addToSet duplicate
         pushObj = new BasicDBObject("$addToSet", new BasicDBObject("field1", "value"));
         collection.update(idObj, pushObj);
         expected.put("field1", Arrays.asList("value"));
         assertThat(collection.findOne(idObj)).isEqualTo(expected);
     }
 
     @Test
     public void testUpdatePull() throws Exception {
         BasicDBObject obj = new BasicDBObject("_id", 1);
         collection.insert(obj);
 
         // pull from non-existing field
         assertThat(collection.findOne(obj)).isEqualTo(obj);
 
         // pull from non-array
         collection.update(obj, new BasicDBObject("$set", new BasicDBObject("field", "value")));
         try {
             collection.update(obj, new BasicDBObject("$pull", new BasicDBObject("field", "value")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10142);
             assertThat(e.getMessage()).isEqualTo("Cannot apply $pull modifier to non-array");
         }
 
         // pull standard
         collection.update(obj,
                 new BasicDBObject("$set", new BasicDBObject("field", Arrays.asList("value1", "value2", "value1"))));
 
         collection.update(obj, new BasicDBObject("$pull", new BasicDBObject("field", "value1")));
 
         assertThat(collection.findOne(obj).get("field")).isEqualTo(Arrays.asList("value2"));
 
         // pull with multiple fields
 
         collection.update(obj,
                 new BasicDBObject("$set", new BasicDBObject("field1", Arrays.asList("value1", "value2", "value1"))));
         collection.update(obj,
                 new BasicDBObject("$set", new BasicDBObject("field2", Arrays.asList("value3", "value3", "value1"))));
 
         collection.update(obj,
                 new BasicDBObject("$pull", new BasicDBObject("field1", "value2").append("field2", "value3")));
 
         assertThat(collection.findOne(obj).get("field1")).isEqualTo(Arrays.asList("value1", "value1"));
         assertThat(collection.findOne(obj).get("field2")).isEqualTo(Arrays.asList("value1"));
     }
 
     @Test
     public void testUpdatePullAll() throws Exception {
         DBObject obj = new BasicDBObject("_id", 1);
         collection.insert(obj);
         collection.update(obj, new BasicDBObject("$set", new BasicDBObject("field", "value")));
         try {
             collection.update(obj, new BasicDBObject("$pullAll", new BasicDBObject("field", "value")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10142);
             assertThat(e.getMessage()).isEqualTo("Cannot apply $pullAll modifier to non-array");
         }
 
         collection.update(
                 obj,
                 new BasicDBObject("$set", new BasicDBObject("field1", Arrays.asList("value1", "value2", "value1",
                         "value3", "value4", "value3"))));
 
         collection.update(obj,
                 new BasicDBObject("$pullAll", new BasicDBObject("field1", Arrays.asList("value1", "value3"))));
 
         assertThat(collection.findOne(obj).get("field1")).isEqualTo(Arrays.asList("value2", "value4"));
 
         try {
             collection.update(obj, new BasicDBObject("$pullAll", new BasicDBObject("field1", "bar")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10153);
             assertThat(e.getMessage()).isEqualTo("Modifier $pullAll allowed for arrays only");
         }
 
     }
 
     @Test
     public void testUpdateSet() throws Exception {
         BasicDBObject object = new BasicDBObject("_id", 1);
 
         collection.insert(object);
         assertThat(collection.findOne(object)).isEqualTo(object);
 
         collection.update(object, new BasicDBObject("$set", new BasicDBObject("foo", "bar")));
 
         BasicDBObject expected = new BasicDBObject();
         expected.putAll((BSONObject) object);
         expected.put("foo", "bar");
 
         collection.update(object, new BasicDBObject("$set", new BasicDBObject("bar", "bla")));
         expected.put("bar", "bla");
         assertThat(collection.findOne(object)).isEqualTo(expected);
 
         collection.update(object, new BasicDBObject("$set", new BasicDBObject("foo.bar", "bla")));
         expected.put("foo", new BasicDBObject("bar", "bla"));
         assertThat(collection.findOne(object)).isEqualTo(expected);
 
         collection.update(object, new BasicDBObject("$set", new BasicDBObject("foo.foo", "123")));
         ((BasicBSONObject) expected.get("foo")).put("foo", "123");
         assertThat(collection.findOne(object)).isEqualTo(expected);
     }
 
     @Test
     public void testUpdatePop() throws Exception {
         BasicDBObject object = new BasicDBObject("_id", 1);
 
         collection.insert(object);
         collection.update(object, new BasicDBObject("$pop", new BasicDBObject("foo.bar", 1)));
 
         assertThat(collection.findOne(object)).isEqualTo(object);
         collection.update(object, new BasicDBObject("$set", new BasicDBObject("foo.bar", Arrays.asList(1, 2, 3))));
         assertThat((List<?>) Utils.getSubdocumentValue(collection.findOne(object), "foo.bar")).containsExactly(1, 2, 3);
 
         collection.update(object, new BasicDBObject("$pop", new BasicDBObject("foo.bar", 1)));
         assertThat((List<?>) Utils.getSubdocumentValue(collection.findOne(object), "foo.bar")).containsExactly(1, 2);
 
         collection.update(object, new BasicDBObject("$pop", new BasicDBObject("foo.bar", -1)));
         assertThat((List<?>) Utils.getSubdocumentValue(collection.findOne(object), "foo.bar")).containsExactly(2);
 
         collection.update(object, new BasicDBObject("$pop", new BasicDBObject("foo.bar", null)));
         assertThat((List<?>) Utils.getSubdocumentValue(collection.findOne(object), "foo.bar")).isEmpty();
 
     }
 
     @Test
     public void testUpdateUnset() throws Exception {
         DBObject obj = new BasicDBObject("_id", 1).append("a", 1).append("b", null).append("c", "value");
         collection.insert(obj);
         try {
             collection.update(obj, new BasicDBObject("$unset", new BasicDBObject("_id", "")));
             fail("MongoException expected");
         } catch (MongoException e) {
             assertThat(e.getCode()).isEqualTo(10148);
             assertThat(e.getMessage()).isEqualTo("Mod on _id not allowed");
         }
 
         collection.update(obj, new BasicDBObject("$unset", new BasicDBObject("a", "").append("b", "")));
         DBObject expected = new BasicDBObject("_id", 1).append("c", "value");
         assertThat(collection.findOne()).isEqualTo(expected);
 
         collection.update(obj, new BasicDBObject("$unset", new BasicDBObject("c.y", 1)));
         expected = new BasicDBObject("_id", 1).append("c", "value");
         assertThat(collection.findOne()).isEqualTo(expected);
 
         collection.update(new BasicDBObject("_id", 1),
                 new BasicDBObject("a", new BasicDBObject("b", "foo").append("c", "bar")));
 
         collection.update(new BasicDBObject("_id", 1), new BasicDBObject("$unset", new BasicDBObject("a.b", 1)));
         expected = new BasicDBObject("_id", 1).append("a", new BasicDBObject("c", "bar"));
         assertThat(collection.findOne()).isEqualTo(expected);
     }
 
     @Test
     public void testUpdateWithIdIn() {
         collection.insert(new BasicDBObject("_id", 1));
         DBObject query = new BasicDBObjectBuilder().push("_id").append("$in", Arrays.asList(1)).pop().get();
         DBObject update = new BasicDBObjectBuilder().push("$push").push("n").append("_id", 2).append("u", 3).pop()
                 .pop().push("$inc").append("c", 4).pop().get();
         DBObject expected = new BasicDBObjectBuilder().append("_id", 1)
                 .append("n", Arrays.asList(new BasicDBObject("_id", 2).append("u", 3))).append("c", 4).get();
         collection.update(query, update, false, true);
         assertThat(collection.findOne()).isEqualTo(expected);
     }
 
     @Test
     public void testUpdateMulti() throws Exception {
         collection.insert(new BasicDBObject("a", 1));
         collection.insert(new BasicDBObject("a", 1));
         WriteResult result = collection.update(new BasicDBObject("a", 1), new BasicDBObject("$set", new BasicDBObject(
                 "b", 2)));
 
         assertThat(result.getN()).isEqualTo(1);
         assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.TRUE);
 
         assertThat(collection.find(new BasicDBObject("b", 2)).count()).isEqualTo(1);
 
         result = collection.update(new BasicDBObject("a", 1), new BasicDBObject("$set", new BasicDBObject("b", 3)),
                 false, true);
         assertThat(result.getN()).isEqualTo(2);
         assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.TRUE);
         assertThat(collection.find(new BasicDBObject("b", 2)).count()).isEqualTo(0);
         assertThat(collection.find(new BasicDBObject("b", 3)).count()).isEqualTo(2);
     }
 
     @Test
     public void testUpdateWithIdInMulti() {
         collection.insert(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2));
         collection.update(new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(1, 2))), new BasicDBObject(
                 "$set", new BasicDBObject("n", 1)), false, true);
         List<DBObject> results = collection.find().toArray();
         assertThat(results).containsExactly(new BasicDBObject("_id", 1).append("n", 1),
                 new BasicDBObject("_id", 2).append("n", 1));
     }
 
     @Test
     public void testUpdateWithIdInMultiReturnModifiedDocumentCount() {
         collection.insert(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2));
         BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", Arrays.asList(1, 2)));
         BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("n", 1));
         WriteResult result = collection.update(query, update, false, true);
         assertThat(result.getN()).isEqualTo(2);
     }
 
     @Test
     public void testUpdateWithIdQuery() {
         collection.insert(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2));
         collection.update(new BasicDBObject("_id", new BasicDBObject("$gt", 1)), new BasicDBObject("$set",
                 new BasicDBObject("n", 1)), false, true);
         List<DBObject> results = collection.find().toArray();
         assertThat(results).containsExactly(new BasicDBObject("_id", 1), new BasicDBObject("_id", 2).append("n", 1));
     }
 
     @Test
     public void testUpdateWithObjectId() {
         collection.insert(new BasicDBObject("_id", new BasicDBObject("n", 1)));
         DBObject query = new BasicDBObject("_id", new BasicDBObject("n", 1));
         DBObject update = new BasicDBObject("$set", new BasicDBObject("a", 1));
         collection.update(query, update, false, false);
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", new BasicDBObject("n", 1)).append("a", 1));
     }
 
     @Test
     public void testUpdateWithObjectIdReturnModifiedDocumentCount() {
         collection.insert(new BasicDBObject("_id", new BasicDBObject("n", 1)));
         DBObject query = new BasicDBObject("_id", new BasicDBObject("n", 1));
         DBObject update = new BasicDBObject("$set", new BasicDBObject("a", 1));
         WriteResult result = collection.update(query, update, false, false);
         assertThat(result.getN()).isEqualTo(1);
     }
 
     @Test
     public void testUpsert() {
         WriteResult result = collection.update(new BasicDBObject("_id", 1).append("n", "jon"), new BasicDBObject(
                 "$inc", new BasicDBObject("a", 1)), true, false);
         assertThat(result.getN()).isEqualTo(1);
         assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.FALSE);
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("n", "jon").append("a", 1));
     }
 
     @Test
    public void testUpsertWithoutId() {
        WriteResult result = collection.update(new BasicDBObject("a", 1), new BasicDBObject("a", 2), true, false);
        assertThat(result.getN()).isEqualTo(1);
        assertThat(result.getField("updatedExisting")).isEqualTo(Boolean.FALSE);
        assertThat(collection.findOne().get("_id")).isInstanceOf(ObjectId.class);
        assertThat(collection.findOne().get("a")).isEqualTo(2);
    }

    @Test
     public void testUpsertOnIdWithPush() {
         DBObject update1 = BasicDBObjectBuilder.start().push("$push").push("c").append("a", 1) //
                 .append("b", 2).pop().pop().get();
 
         DBObject update2 = BasicDBObjectBuilder.start().push("$push").push("c").append("a", 3) //
                 .append("b", 4).pop().pop().get();
 
         collection.update(new BasicDBObject("_id", 1), update1, true, false);
 
         collection.update(new BasicDBObject("_id", 1), update2, true, false);
 
         DBObject expected = new BasicDBObject("_id", 1).append("c",
                 Arrays.asList(new BasicDBObject("a", 1).append("b", 2), //
                         new BasicDBObject("a", 3).append("b", 4)));
 
         assertThat(collection.findOne(new BasicDBObject("c.a", 3).append("c.b", 4))).isEqualTo(expected);
     }
 
     @Test
     public void testUpsertWithConditional() {
         DBObject query = new BasicDBObject("_id", 1).append("b", new BasicDBObject("$gt", 5));
         BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("a", 1));
         collection.update(query, update, true, false);
         assertThat(collection.findOne()).isEqualTo(new BasicDBObject("_id", 1).append("a", 1));
     }
 
     @Test
     public void testUpsertWithEmbeddedQuery() {
         DBObject update = BasicDBObjectBuilder.start().push("$set").append("a", 1).pop().get();
 
         collection.update(new BasicDBObject("_id", 1).append("e.i", 1), update, true, false);
 
         DBObject expected = BasicDBObjectBuilder.start().append("_id", 1).push("e") //
                 .append("i", 1).pop().append("a", 1).get();
 
         assertThat(collection.findOne(new BasicDBObject("_id", 1))).isEqualTo(expected);
     }
 
     @Test
     public void testUpsertWithIdIn() throws Exception {
         // {_id: {$in: [1]}}
         DBObject query = new BasicDBObjectBuilder().push("_id").append("$in", Arrays.asList(1)).pop().get();
 
         // {$push: {n: {_id: 2 ,u : 3}}, $inc: {c: 4}}
         DBObject update = new BasicDBObjectBuilder().push("$push").push("n") //
                 .append("_id", 2).append("u", 3).pop().pop() //
                 .push("$inc").append("c", 4).pop().get();
 
         DBObject expected = new BasicDBObjectBuilder().append("_id", 1)
                 .append("n", Arrays.asList(new BasicDBObject("_id", 2) //
                         .append("u", 3))).append("c", 4).get();
 
         collection.update(query, update, true, false);
 
         // the ID generation actually differs from official MongoDB which just
         // create a random object id
         DBObject actual = collection.findOne();
         assertThat(actual).isEqualTo(expected);
     }
 
     @Test
     public void testIsMaster() throws Exception {
         CommandResult isMaster = db.command("isMaster");
         assertThat(isMaster.ok()).isTrue();
         assertThat(isMaster.getBoolean("ismaster")).isTrue();
         assertThat(isMaster.getDate("localTime")).isInstanceOf(Date.class);
         assertThat(isMaster.getInt("maxBsonObjectSize")).isGreaterThan(1000);
         assertThat(isMaster.getInt("maxMessageSizeBytes")).isGreaterThan(isMaster.getInt("maxBsonObjectSize"));
     }
 
 }
