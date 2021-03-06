 package ee.ioc.phon.g2p;
 
 import java.io.InputStream;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.yaml.snakeyaml.Yaml;
 
 public class Utils {
 
 	public static String arrayToString(String[] phonemes) {
 		StringBuffer result = new StringBuffer();
 		boolean first = true;
 		for (String ph : phonemes) {
 			if (!first) {
 				result.append(" ");
 			}
 			result.append(ph);
 			first = false;
 		}
 		return result.toString();
 	}
 
 	public static Map loadYamlFile(String rulesFile) {
 		InputStream rulesInput = Utils.class.getResourceAsStream("/" + rulesFile);
 		Yaml yaml = new Yaml();
 		
 	    Map rulesMap = (Map) yaml.load(rulesInput);
 	    
 		return rulesMap;
 	}
 
 	public static Map<String, String> linesToMap(Map rulesMap, String nimetav) {
 		Map<String, String> numbersNimetav = new HashMap<String, String>();
 		for (String ruleStr : ((String)rulesMap.get(nimetav)).split("\n")) {
 	    	String[] ruleParts = ruleStr.split("\\s+", 2);
 	    	if (ruleParts.length > 1) {
		    	numbersNimetav.put(ruleParts[0].trim(), ruleParts[1].trim());
 	    	}
 	    }
 		return numbersNimetav;
 	}
 
 }
