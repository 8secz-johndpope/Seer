 package redis.clients.jedis.tests.commands;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.junit.Test;
 
 import redis.clients.jedis.exceptions.JedisDataException;
 import redis.clients.jedis.exceptions.JedisException;
 import redis.clients.util.SafeEncoder;
 
 public class ScriptingCommandsTest extends JedisCommandTestBase {
     
     @SuppressWarnings("unchecked")
 	@Test
     public void evalMultiBulk() {
     	String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
     	List<String> keys = new ArrayList<String>();
     	keys.add("key1");
     	keys.add("key2");
     	
         List<String> response = (List<String>)jedis.eval(script, keys, "first", "second" );
         
         assertEquals(4, response.size());
         assertEquals("key1", response.get(0));
         assertEquals("key2", response.get(1));
         assertEquals("first", response.get(2));
         assertEquals("second", response.get(3));
     }
     
 	@Test
     public void evalBulk() {
     	String script = "return KEYS[1]";
     	List<String> keys = new ArrayList<String>();
     	keys.add("key1");
     	
         String response = (String)jedis.eval(script, keys);
         
         assertEquals("key1", response);        
     }
 	
 	@Test
     public void evalInt() {
     	String script = "return 2";
     	List<String> keys = new ArrayList<String>();
     	keys.add("key1");
     	
     	Long response = (Long)jedis.eval(script, keys);
         
         assertEquals(new Long(2), response);
     }
 	
 	@Test
     public void evalNoArgs() {
     	String script = "return KEYS[1]";
     	
     	String response = (String)jedis.eval(script, "key1");
         
         assertEquals("key1", response);
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void evalsha() {
 		jedis.set("foo", "bar");
 		jedis.eval("return redis.call('get','foo')");
 		String result = (String)jedis.evalsha("6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
 		
 		assertEquals("bar", result);
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test(expected=JedisDataException.class)
     public void evalshaShaNotFound() {
 		jedis.evalsha("ffffffffffffffffffffffffffffffffffffffff");
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptFlush() {
 		jedis.set("foo", "bar");
 		jedis.eval("return redis.call('get','foo')");
 		jedis.scriptFlush();
 		assertFalse(jedis.scriptExists("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptExists() {
 		jedis.scriptLoad("return redis.call('get','foo')");
 		List<Boolean> exists = jedis.scriptExists("ffffffffffffffffffffffffffffffffffffffff","6b1bf486c81ceb7edf3c093f4c48582e38c0e791"); 
 		assertFalse(exists.get(0));
 		assertTrue(exists.get(1));
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptExistsBinary() {
 		jedis.scriptLoad(SafeEncoder.encode("return redis.call('get','foo')"));
 		List<Long> exists = jedis.scriptExists(SafeEncoder.encode("ffffffffffffffffffffffffffffffffffffffff"),SafeEncoder.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791")); 
 		assertEquals(new Long(0), exists.get(0));
 		assertEquals(new Long(1), exists.get(1));
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptLoad() {
 		jedis.scriptLoad("return redis.call('get','foo')");
 		assertTrue(jedis.scriptExists("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptLoadBinary() {
 		jedis.scriptLoad(SafeEncoder.encode("return redis.call('get','foo')"));
 		List<Long> exists = jedis.scriptExists(SafeEncoder.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791")); 
 		assertEquals(new Long(1), exists.get(0));
     }
 	
 	@SuppressWarnings("unchecked")
 	@Test
     public void scriptKill() {
		jedis.scriptKill();
    }
 }
