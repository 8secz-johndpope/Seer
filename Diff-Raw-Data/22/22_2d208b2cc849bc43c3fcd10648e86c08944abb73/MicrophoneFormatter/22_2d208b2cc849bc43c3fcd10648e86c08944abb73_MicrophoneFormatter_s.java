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
 
 import org.json.simple.JSONObject;
 
 import com.ubhave.dataformatter.json.PullSensorJSONFormatter;
 import com.ubhave.sensormanager.config.SensorConfig;
 import com.ubhave.sensormanager.data.SensorData;
 import com.ubhave.sensormanager.data.pullsensor.MicrophoneData;
 
 public class MicrophoneFormatter extends PullSensorJSONFormatter
 {	
 	
 	private final static String SAMPLE_LENGTH = "sampleLengthMillis";
 	private final static String AMPLITUDE = "amplitude";
 	
 	@SuppressWarnings("unchecked")
 	@Override
 	protected void addSensorSpecificData(JSONObject json, SensorData data)
 	{
 		MicrophoneData micData = (MicrophoneData) data;
		json.put(AMPLITUDE, micData.getAmplitudeString());
 	}
 
 	@SuppressWarnings("unchecked")
 	@Override
 	protected void addSensorSpecificConfig(JSONObject json, SensorConfig config)
 	{
 		json.put(SAMPLE_LENGTH, config.getParameter(SensorConfig.SENSE_WINDOW_LENGTH_MILLIS));
 	}
 	
 	@Override
 	public SensorData toSensorData(String jsonString)
 	{
 		// TODO
 		return null;
 	}
 
 }
