 package com.gallatinsystems.common.util;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Properties;
 
 /**
  * @author Dru Borden
  *
  */
 public class PropertyUtil {
 
 	private static Properties props = null;
 
 	public PropertyUtil() {
 		initProperty();
 	}
 	
 	private void initProperty(){
 		if (props == null) {
 			props = System.getProperties();
 		}
 	}
 
	public String getProperty(String propertyName) {
 		
 		return props.getProperty(propertyName);
 	}
 
	public  HashMap<String, String> getPropertiesMap(ArrayList<String> keyList) {
 		HashMap<String, String> propertyMap = new HashMap<String, String>();
 		for (String key : keyList) {
 			String value = props.getProperty(key);
 			propertyMap.put(key, value);
 		}
 		return propertyMap;
 	}
 
 }
