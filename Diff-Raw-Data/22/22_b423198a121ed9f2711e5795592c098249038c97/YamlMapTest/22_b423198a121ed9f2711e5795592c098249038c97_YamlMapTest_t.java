 /* This code is part of Freenet. It is distributed under the GNU General
  * Public License, version 2 (or at your option any later version). See
  * http://www.gnu.org/ for further details of the GPL. */
 package plugins.Interdex.serl;
 
 import junit.framework.TestCase;
 
 import org.yaml.snakeyaml.Yaml;
 
 import java.io.*;
 import java.util.*;
 
 /**
 ** @author infinity0
 */
 public class YamlMapTest extends TestCase {
 
 	public void testYamlMap() throws IOException {
 		Map<String, Bean> data = new TreeMap<String, Bean>();
 		data.put("gold1", new Bean());
 		data.put("gold2", new Bean());
 
 		Yaml yaml = new Yaml();
 		File file = new File("beantest.yml");
 
 		FileOutputStream os = new FileOutputStream(file);
 		yaml.dump(data, new OutputStreamWriter(os));
 		os.close();
 
 		FileInputStream is = new FileInputStream(file);
 		Object o = yaml.load(new InputStreamReader(is));
 		is.close();
 
 		assertTrue(o instanceof Map);
 		Map m = (Map)o;
 		assertTrue(m.get("gold1") instanceof Bean);
		// NOTE these tests fail in snakeYAML 1.2 and below, fixed in hg
		assertTrue(m.get("gold2") instanceof Bean);
 	}
 
 	public static class Bean {
 		private String a;
 		public Bean() { a = ""; }
 		public String getA() { return a; }
 		public void setA(String s) { a = s; }
 
 	}
 
 }
