 package sample;
 
 import static org.hamcrest.CoreMatchers.is;
 import static org.hamcrest.CoreMatchers.notNullValue;
 import static org.junit.Assert.assertThat;
 
 import java.util.Iterator;
 import java.util.TreeMap;
 
 import org.junit.Test;
 
 public class TreeMapTest {
 
 	@Test
 	public void testInstance() {
 		TreeMap<String, Object> tmap = new TreeMap<String, Object>();
 
 		assertThat(tmap, is(notNullValue()));
 		assertThat(0, is(tmap.size()));
 	}
 
 	@Test
 	public void testAdd() throws Exception {
 		TreeMap<String, Object> tmap = new TreeMap<String, Object>();
 
 		tmap.put(new String(), new Object());
 
 		assertThat(1, is(tmap.size()));
 	}
 
 	@Test
 	public void testSort() throws Exception {
 		TreeMap<String, Integer> tmap = new TreeMap<String, Integer>();
 
		tmap.put("", 2);
		tmap.put("", 1);
		tmap.put("", 4);
		tmap.put("", 5);
		tmap.put("", 3);
		
 		Iterator<Integer> valueIterator = tmap.values().iterator();
 		for (int i=0; valueIterator.hasNext(); i++) {
 			assertThat(i+1, is(valueIterator.next()));
 		}
 	}
 
 }
