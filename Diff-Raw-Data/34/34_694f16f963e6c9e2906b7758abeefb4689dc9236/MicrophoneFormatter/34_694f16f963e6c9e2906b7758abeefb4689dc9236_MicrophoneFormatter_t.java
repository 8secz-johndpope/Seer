 /* **************************************************
  Copyright (c) 2012, University of Cambridge
  Neal Lathia, neal.lathia@cl.cam.ac.uk
 
 This library was developed as part of the EPSRC Ubhave (Ubiquitous and
 Social Computing for Positive Behaviour Change) Project. For more
 information, please visit http://www.emotionsense.org
 
 Permission to use, copy, modify, and/or distribute this software for any
 purpose with or without fee is hereby granted, provided that the above
 copyright notice and this permission notice appear in all copies.
 
 THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
  ************************************************** */
 
 package com.ubhave.dataformatter.json.pull;
 
 import java.util.ArrayList;
 
 import org.json.simple.JSONArray;
 import org.json.simple.JSONObject;
 
 import android.content.Context;
 
 import com.ubhave.dataformatter.json.PullSensorJSONFormatter;
 import com.ubhave.sensormanager.ESException;
 import com.ubhave.sensormanager.config.SensorConfig;
 import com.ubhave.sensormanager.config.sensors.pull.PullSensorConfig;
 import com.ubhave.sensormanager.data.SensorData;
 import com.ubhave.sensormanager.data.pullsensor.MicrophoneData;
 import com.ubhave.sensormanager.process.AbstractProcessor;
 import com.ubhave.sensormanager.process.pull.AudioProcessor;
 import com.ubhave.sensormanager.sensors.SensorUtils;
 
 public class MicrophoneFormatter extends PullSensorJSONFormatter
 {	
 	private final static String SAMPLE_LENGTH = "sampleLengthMillis";
 	private final static String AMPLITUDE = "amplitude";
 	private final static String TIMESTAMP = "timestamp";
 	
 	public MicrophoneFormatter(final Context context)
 	{
 		super(context, SensorUtils.SENSOR_TYPE_MICROPHONE);
 	}
 	
 	@SuppressWarnings("unchecked")
 	@Override
 	protected void addSensorSpecificData(JSONObject json, SensorData data)
 	{
 		MicrophoneData micData = (MicrophoneData) data;
 		int[] values = micData.getAmplitudeArray();
 		JSONArray valueArray = new JSONArray();
 		for (int i=0; i<values.length; i++)
 		{
 			valueArray.add(values[i]);
 		}
 		
 		json.put(AMPLITUDE, valueArray);
 		
 		long[] tsValues = micData.getTimestampArray();
 		JSONArray tsArray = new JSONArray();
 		for (int i=0; i<values.length; i++)
 		{
 			tsArray.add(tsValues[i]);
 		}
 		
 		json.put(TIMESTAMP, tsArray);		
 	}
 
 	@SuppressWarnings("unchecked")
 	@Override
 	protected void addSensorSpecificConfig(JSONObject json, SensorConfig config)
 	{
 		json.put(SAMPLE_LENGTH, config.getParameter(PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS));
 	}
 	
 	@Override
 	public SensorData toSensorData(String jsonString)
 	{
 		JSONObject jsonData = super.parseData(jsonString);
 		long senseStartTimestamp = super.parseTimeStamp(jsonData);
 		SensorConfig sensorConfig = super.getGenericConfig(jsonData);
 		
 		boolean setRawData = true;
 		boolean setProcessedData = false;
 		
 		int[] ampValues = null;
 		long[] tsValues = null;
 		
 		try
 		{
			ArrayList<Integer> amplitudes = getJSONArray(jsonData, AMPLITUDE, Integer.class);
 			ArrayList<Long> timestamps = getJSONArray(jsonData, TIMESTAMP, Long.class);
 			
 			ampValues = new int[amplitudes.size()];
 			tsValues = new long[timestamps.size()];
 			
 			for (int i=0; i<amplitudes.size(); i++)
 			{
				ampValues[i] = amplitudes.get(i);
 				tsValues[i] = timestamps.get(i);
 			}
 		}
 		catch (Exception e)
 		{
 			setRawData = false;
 		}
 		
 <<<<<<< HEAD
 		MicrophoneData micData = new MicrophoneData(senseStartTimestamp, sensorConfig);
 		micData.setMaxAmplitudeArray(ampValues);
 		micData.setTimestampArray(tsValues);
 
 		return micData;
 =======
 		try
 		{
 			AudioProcessor processor = (AudioProcessor) AbstractProcessor.getProcessor(applicationContext, sensorType, setRawData, setProcessedData);
 			return processor.process(senseStartTimestamp, ampValues, tsValues, sensorConfig);
 		}
 		catch (ESException e)
 		{
 			e.printStackTrace();
 			return null;
 		}
 >>>>>>> Updating to use latest SensorManager library
 	}
 
 }
