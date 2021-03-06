 package com.ubhave.sensormanager.process;
 
 import com.ubhave.sensormanager.config.SensorConfig;
 import com.ubhave.sensormanager.data.pushsensor.PhoneStateData;
 
 public class PhoneStateProcessor extends CommunicationProcessor
 {
 	
 	public PhoneStateProcessor(boolean rw, boolean sp)
 	{
 		super(rw, sp);
 	}
 
 	public PhoneStateData process(long timestamp, SensorConfig config, int event, String details, String number)
 	{
 		PhoneStateData data = new PhoneStateData(timestamp, config);
 		if (super.setRawData)
 		{
 			data.setEventType(event);
 			data.setData(details);
 			if (number != null)
 			{
 				number = hashPhoneNumber(number);
 			}
 		}
 		
 		if (super.setProcessedData)
 		{
 			// TODO 
 			data.setDataProcessed(true);
 		}	
 		return data;
 	}
 }
