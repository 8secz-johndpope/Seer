 package se.leap.leapclient;
 
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.json.JSONException;
 import org.json.JSONObject;
 
 
 public class ProviderListContent {
 
     /**
      * An array of sample (dummy) items.
      */
     public static List<ProviderItem> ITEMS = new ArrayList<ProviderItem>();
 
     /**
      * A map of sample (dummy) items, by ID.
      */
     public static Map<String, ProviderItem> ITEM_MAP = new HashMap<String, ProviderItem>();
 
     static {
         //addItem(new ProviderItem("1", "bitmask", "https://bitmask.net/provider.json", "https://api.bitmask.net:4430/1/config/eip-service.json"));    
     }
     
     public static void addItem(ProviderItem item) {
         ITEMS.add(item);
         ITEM_MAP.put(String.valueOf(ITEMS.size()), item);
     }
 
     /**
      * A dummy item representing a piece of content.
      */
     public static class ProviderItem {  
     	public boolean custom = false;
     	public String id;
         public String name;
         public String provider_json_url;
         public String provider_json_assets;
         public String eip_service_json_url;
         public String cert_json_url;
         
         public ProviderItem(String id, String name, String provider_json_url, String eip_service_json_url, String cert_json_url) {
         	this.id = id;
         	this.name = name;
             this.provider_json_url = provider_json_url;
             this.eip_service_json_url = eip_service_json_url;
             this.cert_json_url = cert_json_url;
         }
 
         public ProviderItem(String name, InputStream urls_file_input_stream, boolean custom) {
         	
         	try {
         		byte[] urls_file_bytes = new byte[urls_file_input_stream.available()];
         		urls_file_input_stream.read(urls_file_bytes);
         		String urls_file_content = new String(urls_file_bytes);
 				JSONObject file_contents = new JSONObject(urls_file_content);
 				id = name;
 				this.name = name;
 				provider_json_url = (String) file_contents.get("json_provider");
 				provider_json_assets = (String) file_contents.get("assets_json_provider");
 				eip_service_json_url = (String) file_contents.get("json_eip_service");
 				cert_json_url = (String) file_contents.get("cert");
 			} catch (JSONException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
         }
 
         public ProviderItem(String name, FileInputStream provider_json, boolean custom) {
         	
         	try {
         		byte[] urls_file_bytes = new byte[provider_json.available()];
         		provider_json.read(urls_file_bytes);
         		String urls_file_content = new String(urls_file_bytes);
 				JSONObject file_contents = new JSONObject(urls_file_content);
 				id = name;
 				this.name = name;
 				eip_service_json_url = (String) file_contents.get("api_uri") + ConfigHelper.eip_service_api_path;
 				cert_json_url = (String) file_contents.get("ca_cert_uri");
 			} catch (JSONException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
         }
 
         @Override
         public String toString() {
             return name;
         }
     }
 }
