 /*
  * Copyright (C) 2012 Peter Brinkmann (peter.brinkmann@gmail.com)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.noisepages.nettoyeur.usbmiditest;
 
 import java.util.List;
 
 import android.app.Activity;
 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.hardware.usb.UsbDevice;
 import android.hardware.usb.UsbManager;
 import android.os.Bundle;
 import android.os.Handler;
 import android.util.Log;
 import android.view.Menu;
 import android.widget.TextView;
 
 import com.noisepages.nettoyeur.midi.MidiReceiver;
 import com.noisepages.nettoyeur.usbmidi.UsbMidiDevice;
 import com.noisepages.nettoyeur.usbmidi.UsbMidiDevice.UsbMidiInput;
 import com.noisepages.nettoyeur.usbmidi.UsbMidiDevice.UsbMidiInterface;
 
 public class MainActivity extends Activity {
 
 	private static final String TAG = "UsbMidiTest";
 
 	private TextView mainText;
 	private UsbMidiDevice midiDevice = null;
 	private Handler handler;
 
 	private static final String ACTION_USB_PERMISSION =
 			"com.android.example.USB_PERMISSION";
 	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
 
 		public void onReceive(Context context, Intent intent) {
 			String action = intent.getAction();
 			if (ACTION_USB_PERMISSION.equals(action)) {
 				synchronized (this) {
 					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
 					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
 						if (device != null) {
 							midiDevice.open(MainActivity.this);
 							List<UsbMidiInput> inputs = midiDevice.getInterfaces().get(0).getInputs();
 							if (!inputs.isEmpty()) {
 								UsbMidiInput input = inputs.get(0);
 								input.setReceiver(new MidiReceiver() {
 									@Override
 									public void onRawByte(int value) {
 										update("raw byte: " + value);
 									}
 
 									@Override
 									public void onProgramChange(int channel, int program) {
 										update("program change: " + channel + ", " + program);
 									}
 
 									@Override
 									public void onPolyAftertouch(int channel, int key, int velocity) {
 										update("poly aftertouch: " + channel + ", " + key + ", " + velocity);
 									}
 
 									@Override
 									public void onPitchBend(int channel, int value) {
 										update("pitch bend: " + channel + ", " + value);
 									}
 
 									@Override
 									public void onNoteOn(int channel, int key, int velocity) {
 										update("note on: " + channel + ", " + key + ", " + velocity);
 									}
 
 									@Override
 									public void onNoteOff(int channel, int key, int velocity) {
 										update("note off: " + channel + ", " + key + ", " + velocity);
 									}
 
 									@Override
 									public void onControlChange(int channel, int controller, int value) {
 										update("control change: " + channel + ", " + controller + ", " + value);
 									}
 
 									@Override
 									public void onAftertouch(final int channel, final int velocity) {
 										update("aftertouch: " + channel + ", " + velocity);
 									}
 								});
 								input.start();
 							}
 						}
 					} 
 					else {
 						Log.d(TAG, "permission denied for device " + device);
 					}
 				}
 			}
 		}
 	};
 
 	private void update(final String n) {
 		handler.post(new Runnable() {
 			@Override
 			public void run() {
 				mainText.setText(n);
 			}
 		});
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		handler = new Handler();
 		setContentView(R.layout.activity_main);
 		mainText = (TextView) findViewById(R.id.mainText);
		UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
 		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
 		registerReceiver(mUsbReceiver, filter);
 		for (UsbMidiDevice device : UsbMidiDevice.getMidiDevices(this)) {
 			for (UsbMidiInterface iface : device.getInterfaces()) {
 				if (!iface.getInputs().isEmpty()) {
 					midiDevice = device;
 					mainText.setText("USB MIDI devices\n\n" + device.toString());
 					mUsbManager.requestPermission(midiDevice.getDevice(), mPermissionIntent);
 					return;
 				}
 			}
 		}
 		mainText.setText("No USB MIDI devices found");
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		getMenuInflater().inflate(R.menu.activity_main, menu);
 		return true;
 	}
 
 	@Override
 	protected void onStop() {
 		super.onStop();
 		if (midiDevice != null) {
 			midiDevice.close();
 		}
 	}
 }
