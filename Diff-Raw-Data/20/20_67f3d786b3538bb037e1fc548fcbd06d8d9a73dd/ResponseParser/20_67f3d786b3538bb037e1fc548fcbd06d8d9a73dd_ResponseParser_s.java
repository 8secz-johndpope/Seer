 package dataobjects;
 
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
import android.util.Log;
 
 
 /**
  * This class provides different parse functions which parse a string in JSON format to the needed objects
  * @author Silvio
  *
  */
 public class ResponseParser
 {
 	/**
 	 * This method parse the given string (which should be valid JSON) to a list of WatchPositionRecords
 	 * @author Silvio
 	 * @param jsonString The string which represent the JSON Object
 	 * @return The List of WatchPositionRecords
 	 */
 	public static ArrayList<WatchPositionRecord> getParsedResponse(String jsonString)
 	{		
 		ArrayList<WatchPositionRecord> deviceInformation = new ArrayList<WatchPositionRecord>();
 		if (jsonString!=null)
 		{
 			
             try
             {
             	/**
             	 * This is to convert JSON into java class object
             	 * @author Maheswari
             	 */
             	JSONObject obj = new JSONObject(jsonString);
             	JSONArray  responseObject = obj.getJSONArray("objects");
 
             	for(int i = 0 ; i < responseObject.length() ; i++)
             	{            		
             		WatchPositionRecord watchPositionRecord = new WatchPositionRecord();
             		
                     JSONObject jsonObj = (JSONObject)responseObject.get(i);
                     
                     // Accessing the different fields of the JSON Object and fill the POJO WatchPositionRecord with the data
                     watchPositionRecord.setMapId(jsonObj.getInt("mapId"));
                     watchPositionRecord.setWatchId(jsonObj.getString("watchId").toString());
                     
                     SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss" );
                     try
 					{
 						Date insertedAt = dateFormatter.parse(jsonObj.getString("insertedAt").toString());
 						watchPositionRecord.setInsertedAt(insertedAt);
 					}
                     catch (ParseException e)
 					{
 						e.printStackTrace();
 					}
 
                     String x = jsonObj.getString("x").toString();
                     Double parseX = Double.parseDouble(x);
                                         
                     String y = jsonObj.getString("y").toString();
                     Double parseY = Double.parseDouble(y);
                     
                     Point position = new Point(parseX.floatValue(),  parseY.floatValue());
                     watchPositionRecord.setPosition(position);
                     
                     deviceInformation.add(watchPositionRecord);
                 }
             	return deviceInformation;
 			}
             catch (JSONException e)
             {
 				e.printStackTrace();
 			}
 		}
 		return null;
 	}
 
 
 	/**
 	 * This method parse the given string (which should be valid JSON) to a MapRecord
 	 * @author Maheswari
 	 * @param jsonString The string which represent the JSON Object
 	 * @return The MapRecord which represents the jsonString
 	 */
 	public static MapRecord parseMapRecord(String jsonString)
 	{	
 		MapRecord mapRecord = null;
 		if (jsonString!=null) 
 		{
 	        try
 	        {	            	
             	JSONObject obj = new JSONObject(jsonString);
             	JSONArray  responseObject = obj.getJSONArray("objects");
 
             	for(int i = 0 ; i < responseObject.length() ; i++)
             	{
             		mapRecord = new MapRecord();
             		
                     JSONObject jsonObj = (JSONObject)responseObject.get(i);
                     mapRecord.setMapId(jsonObj.getString("mapId").toString());
                     
                     mapRecord.setId(jsonObj.getString("id").toString());
                     
                     String height = jsonObj.getString("height").toString();
                     Float parseHeight = Float.parseFloat(height);
                     mapRecord.setHeight(parseHeight);
 
                     String width = jsonObj.getString("width").toString();
                     Float parsewidth =  Float.parseFloat(width);
                     mapRecord.setWidth(parsewidth);
                     
                     String scalingStringX = jsonObj.getString("scalingX").toString();
                     Float scalingX =  Float.parseFloat(scalingStringX);
                     mapRecord.setScalingX(scalingX);
 
                     String scalingStringY = jsonObj.getString("scalingY").toString();
                     Float scalingY =  Float.parseFloat(scalingStringY);
                     mapRecord.setScalingY(scalingY);                    
                     
                     String offsetX = jsonObj.getString("offsetX").toString();
                     Float parseOffsetX =  Float.parseFloat(offsetX);
                     mapRecord.setOffsetX(parseOffsetX);
                     
                     String offsetY = jsonObj.getString("offsetY").toString();
                     Float parseOffsetY =  Float.parseFloat(offsetY);
                     mapRecord.setOffsetY(parseOffsetY);
 
                     String offset2X = jsonObj.getString("offset2X").toString();
                     Float parseOffset2X =  Float.parseFloat(offset2X);
                     mapRecord.setOffset2X(parseOffset2X);
                     
                     String offset2Y = jsonObj.getString("offset2Y").toString();
                     Float parseOffset2Y =  Float.parseFloat(offset2Y);
                     mapRecord.setOffset2Y(parseOffset2Y);
 
                     SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss" );
                     try
 					{
 						Date updatedTime = dateFormatter.parse(jsonObj.getString("updateTime").toString());
 						mapRecord.setUpdateTime(updatedTime);
 					} catch (ParseException e)
 					{
 						e.printStackTrace();
 					}
                 }
             	
             	return mapRecord;
             	
 			}
 	        catch (JSONException e)
 	        {
 				e.printStackTrace();
 			}
 
 		}
 		return null;
 	}
 	/**
 	 * This method parse the given string (which should be valid JSONObject) to a ReceiverRecord(which should be valid java class object)
 	 * @author Maheswari
 	 * @param jsonString The string which represent the JSON Object
 	 * @return The receiverInformation which represents the javaObject
 	 */
 public static ArrayList<ReceiverRecord> parseReceiverRecord(String results) {
 
 	ArrayList<ReceiverRecord> receiverInformation = new ArrayList<ReceiverRecord>();
 	if (results!=null) {
 		
         try {
         	
         	JSONObject obj = new JSONObject(results);
         	JSONArray  responseObject = obj.getJSONArray("objects");
 
         	for(int i = 0 ; i < responseObject.length() ; i++){
         		
         		ReceiverRecord receiverRecord = new ReceiverRecord();
         		
                 JSONObject jsonObj = (JSONObject)responseObject.get(i);
 
                 receiverRecord.setReceiverId(jsonObj.getString("receiverId").toString());
 
                 
                 String x = jsonObj.getString("x").toString();
 
                 Float parseX = Float.parseFloat(x);
                 receiverRecord.setX(parseX);      
 
                 String y = jsonObj.getString("y").toString();
                 Float parseY = Float.parseFloat(y);
                 receiverRecord.setY(parseY);      
 
                                 
                 receiverInformation.add(receiverRecord);
             }
         	return receiverInformation;
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
 
 	}
 	return null;
 }
 
 /**
  * This method parse the given string (which should be valid JSONObject) to a WatchPositionRecord(which should be valid java class object)
  * @author Maheswari
  * @param jsonString The string which represent the JSON Object
  * @return This is to return mapId and WatchId The deviceInformation which represents the javaObject
  */
 public static WatchPositionRecord getMapIdResponse(String jsonString) {
 	
 	ArrayList<WatchPositionRecord> deviceInformation = new ArrayList<WatchPositionRecord>();
 	if (jsonString!=null) {
 		
         try {
         	
         	JSONObject obj = new JSONObject(jsonString);
         	JSONArray  responseObject = obj.getJSONArray("objects");
 
         	for(int i = 0 ; i < responseObject.length() ; i++){
         		
         		WatchPositionRecord watchPositionRecord = new WatchPositionRecord();
         		
                 JSONObject jsonObj = (JSONObject)responseObject.get(i);
                 
                 watchPositionRecord.setMapId(jsonObj.getInt("mapId"));
                 watchPositionRecord.setWatchId(jsonObj.getString("watchId").toString());
                 
                                    
                 deviceInformation.add(watchPositionRecord);
             }
         	return deviceInformation.get(0);
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
 
 	}
 	return null;
 }
 
 }
