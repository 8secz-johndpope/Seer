 /*
  * Copyright (c) 2009, iSENSE Project. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * Redistributions of source code must retain the above copyright notice, this
  * list of conditions and the following disclaimer. Redistributions in binary
  * form must reproduce the above copyright notice, this list of conditions and
  * the following disclaimer in the documentation and/or other materials
  * provided with the distribution. Neither the name of the University of
  * Massachusetts Lowell nor the names of its contributors may be used to
  * endorse or promote products derived from this software without specific
  * prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  * DAMAGE.
  */
 
 package edu.uml.cs.raac;
 
 import java.io.IOException;
 import java.text.DateFormat;
 import java.text.DecimalFormat;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 
 import org.json.JSONArray;
 
 import android.app.Activity;
 import android.app.ProgressDialog;
 import android.bluetooth.BluetoothAdapter;
 import android.bluetooth.BluetoothDevice;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.graphics.Color;
 import android.net.ConnectivityManager;
 import android.os.AsyncTask;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.WindowManager.LayoutParams;
 import android.view.animation.Animation;
 import android.view.animation.AnimationUtils;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.RelativeLayout;
 import android.widget.ScrollView;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.ViewFlipper;
 import edu.uml.cs.raac.comm.RestAPI;
 import edu.uml.cs.raac.exceptions.NoDataException;
 import edu.uml.cs.raac.pincushion.BluetoothService;
 import edu.uml.cs.raac.pincushion.pinpointInterface;
 
 public class Isense extends Activity implements OnClickListener {
 	boolean showConnectOption = false, showTimeOption = false, connectFromSplash = true, dataRdy = false;
 	Button rcrdBtn, pushToISENSE;
 	ScrollView dataScroller;
 	ImageButton pinpointBtn;
 	ImageView spinner;
 	RelativeLayout launchLayout;
 	ViewFlipper flipper;
 	TextView minField, maxField, aveField, medField, btStatus, sensorHead;
 	LinearLayout dataLayout;
 	EditText nameField;
 	static pinpointInterface ppi;
 	private BluetoothService mChatService = null;
 	private BluetoothAdapter mBluetoothAdapter = null;
 	ArrayList<String[]> data;
 	Animation mSlideInTop, mSlideOutTop, rotateInPlace;
 	int flipView = 0; //Currently displayed child of the viewFlipper
 	int btStatNum = 0; //The current status of the bluetooth connection
 	int sessionId = -1;
 	public static String experimentId = "427";
 	String username = "sor";
 	String password = "sor";
 	boolean loggedIn = false;
 	static String sessionUrl;
 	String baseSessionUrl = "http://isense.cs.uml.edu/newvis.php?sessions=";
 	String sensorType;
 	String datMed, datAve, datMax, datMin;
 	private RestAPI rapi = null;
 	private ProgressDialog dia;
 	JSONArray dataSet;
 
 	ArrayList<Double> bta1Data = new ArrayList<Double>();
 	ArrayList<String> timeData = new ArrayList<String>();
 
 	// Intent request codes
 	private static final int REQUEST_CONNECT_DEVICE = 1;
 	private static final int REQUEST_CONNECT_DEVICE_2 = 3;
 	private static final int SENSOR_CHANGE = 2;
 	private static final int REQUEST_ENABLE_BT = 4;
 	private static final int REQUEST_VIEW_DATA = 5;
 	private static final int CHANGE_EXPERIMENT = 6;
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.main);
 
 		mSlideInTop = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
 		mSlideOutTop = AnimationUtils.loadAnimation(this, R.anim.slide_out_top);
 		rotateInPlace = AnimationUtils.loadAnimation(this, R.anim.superspinner);
 
 		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
 		
 		rapi = RestAPI.getInstance((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), getApplicationContext());
 
 		initializeLayout();
 		pinpointBtn.setImageResource(R.drawable.nopptbtn);
 
 		flipper.setInAnimation(mSlideInTop);
 		flipper.setOutAnimation(mSlideOutTop);
 
 		flipView = flipper.getDisplayedChild();
 
 		if (mBluetoothAdapter == null) {
 			//
 		} else {
 			if (!mBluetoothAdapter.isEnabled()) {
 				Intent enableBtIntent = new Intent(
 						BluetoothAdapter.ACTION_REQUEST_ENABLE);
 				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
 			}
 		}
 
 		rcrdBtn.setEnabled(false);
 		pushToISENSE.setEnabled(false);
 
 		launchLayout.setVisibility(View.VISIBLE);
 
 		mChatService = new BluetoothService(this, mHandler);
 	}
 
 	//Set up all views from the XML layout
 	public void initializeLayout() {
 		SharedPreferences prefs = getSharedPreferences("SENSORS", 0);
 		
 		dataScroller = (ScrollView) findViewById(R.id.scrollView1);
 		flipper = (ViewFlipper) findViewById(R.id.flipper);
 		rcrdBtn = (Button) findViewById(R.id.btn_getRcrd);
 		pushToISENSE = (Button) findViewById(R.id.btn_pushToISENSE);
 		pinpointBtn = (ImageButton) findViewById(R.id.pinpoint_select_btn);
 		launchLayout = (RelativeLayout) findViewById(R.id.launchlayout);
 		minField = (TextView) findViewById(R.id.et_min);
 		maxField = (TextView) findViewById(R.id.et_max);
 		aveField = (TextView) findViewById(R.id.et_ave);
 		medField = (TextView) findViewById(R.id.et_medi);
 		btStatus = (TextView) findViewById(R.id.statusField);
 		spinner = (ImageView) findViewById(R.id.mySpin);
 		dataLayout = (LinearLayout) findViewById(R.id.linearLayout1);
 		sensorHead = (TextView) findViewById(R.id.sensorNameHeader);
 		nameField = (EditText) findViewById(R.id.nameField);	
 
 		sensorHead.setText("BTA1: " + prefs.getString("name_bta1", "None"));
 		sensorType = prefs.getString("name_bta1", "None");
 		
 		pinpointBtn.setOnClickListener(this);
 		rcrdBtn.setOnClickListener(this);
 		pushToISENSE.setOnClickListener(this);
 
 		minField.setText(datMin);
 		maxField.setText(datMax);
 		medField.setText(datMed);
 		aveField.setText(datAve);
 
 		setBtStatus();
 	}
 
 	//Override to make sure that the correct layout file is used when the screen orientation changes
 	@Override
 	public void onConfigurationChanged(Configuration newConfig) {
 		super.onConfigurationChanged(newConfig);
 		setContentView(R.layout.main);
 		initializeLayout();
 
 		flipper.setDisplayedChild(flipView);
 
 		if (data != null) {
 			prepDataForUpload();
 			writeDataToScreen();
 		}
 	}
 
 	@Override
 	public synchronized void onResume() {
 		super.onResume();
 		// Performing this check in onResume() covers the case in which BT was
 		// not enabled during onStart(), so we were paused to enable it...
 		// onResume() will be called when ACTION_REQUEST_ENABLE activity
 		// returns.
 		if (mChatService != null) {
 			// Only if the state is STATE_NONE, do we know that we haven't
 			// started already
 			if (mChatService.getState() == BluetoothService.STATE_NONE) {
 				// Start the Bluetooth chat services
 				mChatService.start();
 			}
 		}
 	}
 	
 	@Override
 	public void onStart() {
 		if (!loggedIn) new PerformLogin().execute();
 		
 		super.onStart();
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.menu, menu);
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 
 		if (item.getItemId() == R.id.menu_connect) {
 			Intent serverIntent = new Intent(this, DeviceListActivity.class);
 			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_2);
 		} else if (item.getItemId() == R.id.menu_setTime) {
 			if(ppi.setRealTimeClock())
 				Toast.makeText(Isense.this, "Successfully synced time.", Toast.LENGTH_SHORT).show();
 			else
 				Toast.makeText(Isense.this, "Could not sync time.", Toast.LENGTH_SHORT).show();
 		} else if (item.getItemId() == R.id.menu_setSensors) {
 			Intent i = new Intent(this, SensorSelector.class);
 			startActivityForResult(i, SENSOR_CHANGE);
 		} else if (item.getItemId() == R.id.menu_login) {
 			if (loggedIn)
 				Toast.makeText(Isense.this,  "Already logged in!", Toast.LENGTH_SHORT).show();
 			else
 				new PerformLogin().execute();
 		} else if (item.getItemId() == R.id.menu_experiment) {
 			Intent i = new Intent(this, ChangeExperiment.class);
 			startActivityForResult(i, CHANGE_EXPERIMENT);
 		} 
 		return true;
 
 	}
 
 	@Override
 	protected void onStop() {
 		super.onStop();
 		if (ppi != null)
 			ppi.disconnect();
 	}
 
 	@Override
 	public void onClick(View v) {
 		if (v == pinpointBtn) {
 			Intent serverIntent = new Intent(this, DeviceListActivity.class);
 			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
 		}
 		if (v == rcrdBtn) {
 
 			ppi.setContext(this);
 			final ProgressDialog progressDialog = new ProgressDialog(this);
 			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
 			progressDialog.setMessage("Please wait, preparing to read data");
 			progressDialog.setProgressNumberFormat(null);
 			progressDialog.show();
 
 			final Runnable toastRun = new Runnable() { 
 				public void run() { 
 					Toast.makeText(getApplicationContext(), "No data on PINPoint!", Toast.LENGTH_SHORT).show();
 				}
 			};
 			final Runnable toastRun2 = new Runnable() { 
 				public void run() { 
 					Toast.makeText(getApplicationContext(), "Error getting data from PINPoint!", Toast.LENGTH_SHORT).show();
 				}
 			};
 
 			dataLayout.removeAllViews();
 			if( bta1Data != null || timeData != null ) {
 				bta1Data.clear();
 				timeData.clear();
 			}
 
 			Thread thread=new Thread(
 					new Runnable(){
 
 						public void run(){
 
 							try {
 
 								data = ppi.getData(progressDialog);
 
 							} catch (NoDataException e) {
 								e.printStackTrace();
 								runOnUiThread(toastRun);
 							} catch (IOException e) {
 								e.printStackTrace();
 								runOnUiThread(toastRun2);
 							} catch (Exception e) {
 								e.printStackTrace();
 							}
 
 							runOnUiThread(new Runnable(){
 
 								@Override
 								public void run() {
 									if(progressDialog.isShowing()) {
 										progressDialog.dismiss();
 									}
 									if (data != null) {
 										prepDataForUpload();
 										writeDataToScreen();
 										dataRdy = true;
 									}
 								}
 
 							});
 						}
 
 					});
 			thread.start();
 			pushToISENSE.setEnabled(true);
 		}
 		if (v == pushToISENSE)
 			if (nameField.length() == 0) Toast.makeText(this, "Please enter a name.", Toast.LENGTH_LONG).show();
 			else {
 					if (!dataRdy) {
 						Toast.makeText(this, "There is no data to push.", Toast.LENGTH_LONG).show();
 						pushToISENSE.setEnabled(false);
 					} else {
 						dataRdy = false;
 						uploadData();
 						pushToISENSE.setEnabled(false);
 					}
 					
 			} 
 	}
 	
 	public void prepDataForUpload() {
 		int x = 0;
 		for (int i = 0; i < data.size(); i++) {
 			String[] strray = data.get(i);
 			
 			for (String str : strray) {
 				x++;
 				switch(x) {
 				case 1:  timeData.add(fmtData(str));            break;
 				case 14: bta1Data.add(Double.parseDouble(str)); break;
 				default:                                        break;
 				}
 			}
 			x = 0;
 		}
 
 		findStatistics();	
 	}
 
 	public void writeDataToScreen() {
 		int i = 0;
 		int y = 1;
 		if (data.size() > 10) {
 			i = data.size()-10;
 			y = data.size()-9;
 		}
 
 		int x = 0;
 		int z = 0;
 		String label = "";
 		SharedPreferences prefs = getSharedPreferences("SENSORS", 0);
 		Resources res = getResources();
 
 		try {
 			for (; i<data.size(); i++) {
 				String[] strray = data.get(i);
 
 				LinearLayout newRow = new LinearLayout(getBaseContext());
 				newRow.setOrientation(LinearLayout.HORIZONTAL);
 				if(z%2 != 0) {
 					newRow.setBackgroundColor(res.getColor(R.color.rowcols));
 				}
 				TextView tvLeft1 = new TextView(getBaseContext());
 				tvLeft1.setText("Datapoint " + y);
 				tvLeft1.setTextColor(Color.BLACK);
 				TextView tvRight1 = new TextView(getBaseContext());
 				newRow.addView(tvLeft1);
 				newRow.addView(tvRight1);
 				dataLayout.addView(newRow);
 				for (String str : strray) {
 					x++;
 					z++;
 					switch(x) {
 					case 1: label = "Time (GMT)"; break;
 					case 2: label = "Latitude"; break;
 					case 3: label = "Longitude"; break;
 					case 4: label = "Altitude GPS (m)"; break;
 					case 5: label = "Altitude (m)"; break;
 					case 6: label = "Pressure (atm)"; break;
 					case 7: label = "Air Temperature (c)"; break;
 					case 8: label = "Humidity (%rh)"; break;
 					case 9: label = "Light (lux)"; break;
 					case 10: label = "X-Accel"; break;
 					case 11: label = "Y-Accel"; break;
 					case 12: label = "Z-Accel"; break;
 					case 13: label = "Acceleration"; break;
 					case 14: label = prefs.getString("name_bta1", "BTA 1"); /*bta1Data.add(Double.parseDouble(str));*/ break;
 					case 15: label = prefs.getString("name_bta2", "BTA 2"); break;
 					case 16: label = prefs.getString("name_mini1", "Mini 1"); break;
 					case 17: label = prefs.getString("name_mini2", "Mini 2"); break;
 					}
 					LinearLayout newRow2 = new LinearLayout(getBaseContext());
 					newRow2.setOrientation(LinearLayout.HORIZONTAL);
 					if(x%2 != 0) {
 						newRow2.setBackgroundColor(res.getColor(R.color.rowcols));
 					}
 					TextView tvLeft2 = new TextView(getBaseContext());
 					tvLeft2.setText(label);
 					tvLeft2.setTextColor(Color.BLACK);
 					tvLeft2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
 					TextView tvRight2 = new TextView(getBaseContext());
 					tvRight2.setText(str);
 					tvRight2.setTextColor(Color.BLACK);
 					newRow2.addView(tvLeft2);
 					newRow2.addView(tvRight2);
 					dataLayout.addView(newRow2);
 				}
 				z++;
 				LinearLayout newRow3 = new LinearLayout(getBaseContext());
 				newRow3.setOrientation(LinearLayout.HORIZONTAL);
 				if(z%2 != 0) {
 					newRow3.setBackgroundColor(res.getColor(R.color.rowcols));
 				}
 				TextView tvLeft3 = new TextView(getBaseContext());
 				tvLeft3.setText("\n");
 				TextView tvRight3 = new TextView(getBaseContext());
 				tvRight3.setText("\n");
 				newRow3.addView(tvLeft3);
 				newRow3.addView(tvRight3);
 				dataLayout.addView(newRow3);
 				dataScroller.post(new Runnable() {
 					@Override
 					public void run() {
 						dataScroller.fullScroll(ScrollView.FOCUS_DOWN);
 					}
 				});
 				x = 0;
 				y++;
 			}
 			//findStatistics();
 		} catch (NullPointerException e) {
 			Toast.makeText(getApplicationContext(), "Error collecting data, please try again", Toast.LENGTH_SHORT).show();
 			e.printStackTrace();
 		}
 	}
 
 	private void findStatistics() {
 		DecimalFormat form = new DecimalFormat("0.0000");
 		double min, max, ave, med;
 		double temp = 0;
 
 		if (bta1Data.size() != 0) {
 			min = bta1Data.get(0);
 			max = bta1Data.get(0);
 
 			for (double i : bta1Data) {
 				if (i < min) {
 					min = i;
 				}
 				if (i > max) {
 					max = i;
 				}
 				temp += i;
 			}
 			ave = temp / bta1Data.size();
 
 			datMin = "" + form.format(min);
 			datMax = "" + form.format(max);
 			datAve = "" + form.format(ave);
 
 			minField.setText(datMin);
 			maxField.setText(datMax);
 			aveField.setText(datAve);
 
 			if (bta1Data.size() == 1) {
 				med = bta1Data.get(0);
 			} else if (bta1Data.size() % 2 == 0) {
 				med = bta1Data.get((bta1Data.size() + 1) / 2);
 			} else {
 				med = (bta1Data.get((bta1Data.size() / 2)) + bta1Data.get((bta1Data
 						.size() + 1) / 2)) / 2;
 			}
 
 			datMed = "" + form.format(med);
 			medField.setText(datMed);
 
 		}
 	}
 
 	public void setBtStatus() {
 		if (btStatNum == 0) {
 			btStatus.setText("Status: Disconnected");
 		} else if (btStatNum == 1) {
 			btStatus.setText("Status: Connected");
 		} else {
 
 		}
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		menu.getItem(0).setEnabled(showConnectOption);
 		menu.getItem(2).setEnabled(showTimeOption);
 		menu.getItem(3).setEnabled(!loggedIn);
 		return true;
 	}
 
 	// The Handler that gets information back from the BluetoothChatService
 	private final Handler mHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			switch (msg.what) {
 			case BluetoothService.MESSAGE_STATE_CHANGE:
 				switch (msg.arg1) {
 				case BluetoothService.STATE_CONNECTED:
 					spinner.clearAnimation();
 					spinner.setVisibility(View.GONE);
 					pinpointBtn.setImageResource(R.drawable.pptbtn);
 					ppi = new pinpointInterface(mChatService);
 					rcrdBtn.setEnabled(true);
 					pinpointBtn.setEnabled(false);
 					btStatNum = 1;
 					setBtStatus();
 					if(connectFromSplash) {
 						showConnectOption = true;
 						showTimeOption = true;
 						if(Build.VERSION.SDK_INT >= 11) {
 							invalidateOptionsMenu();
 						}
 						flipper.showNext();
 						flipView = flipper.getDisplayedChild();
 						connectFromSplash = false;
 					} else {
 						Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
 					}
					if(ppi.setRealTimeClock())
						Toast.makeText(Isense.this, "Successfully synced time.", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(Isense.this, "Could not sync time.", Toast.LENGTH_SHORT).show();
 					break;
 				case BluetoothService.STATE_CONNECTING:
 					pinpointBtn.setImageResource(R.drawable.pptbtntry);
 					spinner.setVisibility(View.VISIBLE);
 					spinner.startAnimation(rotateInPlace);
 					btStatNum = 2;
 					setBtStatus();
 					break;
 				case BluetoothService.STATE_LISTEN:
 				case BluetoothService.STATE_NONE:
 					spinner.clearAnimation();
 					spinner.setVisibility(View.GONE);
 					pinpointBtn.setImageResource(R.drawable.nopptbtn);
 					btStatNum = 0;
 					setBtStatus();
 					break;
 				}
 				break;
 			case BluetoothService.MESSAGE_WRITE:
 			case BluetoothService.MESSAGE_READ:
 				byte[] readBuf = (byte[]) msg.obj;
 				String readMessage = new String(readBuf, 0, msg.arg1);
 				break;
 			case BluetoothService.MESSAGE_DEVICE_NAME:
 				break;
 			case BluetoothService.MESSAGE_TOAST:
 			}
 		}
 	};
 
 	public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		switch (requestCode) {
 		case REQUEST_CONNECT_DEVICE:
 			connectFromSplash = true;
 			// When DeviceListActivity returns with a device to connect
 			if (resultCode == Activity.RESULT_OK) {
 				// Get the device MAC address
 				String address = data.getExtras().getString(
 						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
 				// Get the BLuetoothDevice object
 				BluetoothDevice device = mBluetoothAdapter
 						.getRemoteDevice(address);
 				// Attempt to connect to the device
 				mChatService.connect(device);
 			}
 			break;
 		case REQUEST_CONNECT_DEVICE_2:
 			connectFromSplash = false;
 			// When DeviceListActivity returns with a device to connect
 			if (resultCode == Activity.RESULT_OK) {
 				// Get the device MAC address
 				String address = data.getExtras().getString(
 						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
 				// Get the BLuetoothDevice object
 				BluetoothDevice device = mBluetoothAdapter
 						.getRemoteDevice(address);
 				// Attempt to connect to the device
 				mChatService.connect(device);
 				Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
 				rcrdBtn.setEnabled(false);
 			}
 			break;
 		case REQUEST_ENABLE_BT:
 			// When the request to enable Bluetooth returns
 			if (resultCode == Activity.RESULT_OK) {
 				// Bluetooth is now enabled, so set up a chat session
 			} else {
 				// User did not enable Bluetooth or an error occured
 				Toast.makeText(this, R.string.bt_not_enabled_leaving,
 						Toast.LENGTH_SHORT).show();
 				finish();
 			}
 			break;
 		case SENSOR_CHANGE:
 			// When the dialog for selecting sensors is closed
 			if (resultCode == Activity.RESULT_OK) {
 				// Save the selected sensors into their appropriate preferences
 				SharedPreferences prefs = getSharedPreferences("SENSORS", 0);
 				SharedPreferences.Editor editor = prefs.edit();
 
 				editor.putString("sensor_bta1",
 						data.getExtras().getString("bta1"));
 				editor.putString("sensor_bta2",
 						data.getExtras().getString("bta2"));
 				editor.putString("sensor_mini1",
 						data.getExtras().getString("mini1"));
 				editor.putString("sensor_mini2",
 						data.getExtras().getString("mini2"));
 				editor.putString("name_bta1",
 						data.getExtras().getString("btaname1"));
 				editor.putString("name_bta2",
 						data.getExtras().getString("btaname2"));
 				editor.putString("name_mini1",
 						data.getExtras().getString("mininame1"));
 				editor.putString("name_mini2",
 						data.getExtras().getString("mininame2"));
 				
 				sensorHead.setText("BTA1: " + data.getExtras().getString("btaname1"));
 				sensorType = data.getExtras().getString("btaname1");
 
 				editor.commit();
 			}
 			break;
 		case REQUEST_VIEW_DATA:
 			//When the data has been uploaded
 		}
 	}
 	
 	//preps the JSONArray, and pushes time, temp and ph to iSENSE
 	private void uploadData() {
 		if (timeData.size() != bta1Data.size()) {
 			Toast.makeText(this, "Error in preparing data.  Please try pressing \"Get Data\" again.",
 					Toast.LENGTH_LONG).show();
 			return;
 		}
 
 		dataSet = new JSONArray();
 		
 		JSONArray dataJSON;
 		if (sensorType.equals("Vernier Stainless Steel Temperature Probe"))
 			for (int i = 0; i < timeData.size(); i++) {
 				dataJSON = new JSONArray();
 				dataJSON.put(timeData.get(i));
 				dataJSON.put(bta1Data.get(i));
 				dataJSON.put("");
 				dataSet.put(dataJSON);
 			}
 		else if (sensorType.equals("Vernier pH Sensor")) {
 			for (int i = 0; i < timeData.size(); i++) {
 				dataJSON = new JSONArray();
 				dataJSON.put(timeData.get(i));
 				dataJSON.put("");
 				dataJSON.put(bta1Data.get(i));
 				dataSet.put(dataJSON);
 			}
 		}
 		else Toast.makeText(this, "Invalid sensor type.", Toast.LENGTH_LONG).show();
 		
 		new UploadTask().execute();
 	}
 	
 	//the uploading thread that does all the work
 	private Runnable uploader = new Runnable() {
 		
 		@Override
 		public void run() {
 		
 			String nameOfSession = nameField.getText().toString();
 		
 			if (!loggedIn)
 				new PerformLogin().execute();
 			
 			if (loggedIn) {
 				if (sessionId == -1) {
 					sessionId = rapi.createSession(experimentId, 
 							nameOfSession, 
 							"Automated Submission Through Android App", 
 							"500 Pawtucket Blvd.", "Lowell, Massachusetts", "United States");
 					if (sessionId != -1) {
 						rapi.putSessionData(sessionId, experimentId, dataSet);
 						sessionUrl = baseSessionUrl + sessionId;
 					} else {
 						sessionUrl = baseSessionUrl;
 						return;	
 					}
 				
 				}
 				else {
 					rapi.updateSessionData(sessionId, experimentId, dataSet);
 				}
 			} else sessionUrl = baseSessionUrl;
 		
 		}
 		
 	};
 	
 	// Control task for uploading data
 	private class UploadTask extends AsyncTask <Void, Integer, Void> {
 		    
 		@Override protected void onPreExecute() {
 			dia = new ProgressDialog(Isense.this);
 			dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
 			dia.setMessage("Please wait while your data are uploaded to iSENSE...");
 			dia.setCancelable(false);
 			dia.show();
 			
 		}
 		
 		@Override protected Void doInBackground(Void... voids) {
 			
 			uploader.run();
 			publishProgress(100);
 		        
 			return null;
 			
 		}
 
 		@Override  protected void onPostExecute(Void voids)	{
 			sessionId = -1;
 			dia.setMessage("Done");
 			dia.cancel();
 			
 			if (!(sessionUrl.equals(baseSessionUrl))) {
 				Intent i = new Intent(Isense.this, ViewData.class);
 				startActivityForResult(i, REQUEST_VIEW_DATA);  
 			}
 		}
 	}
 		
 	//String formatter
 	public String fmtData(String temp) {
 		String dataString = "";
 		
 		long unixts = 0;
 		DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSS");
 		Date parsed;
 		
 		try {
 			parsed = format.parse(temp);
 			unixts = parsed.getTime();
 			
 			dataString = "" + unixts;
 
 			return dataString;
 		} catch (ParseException e) {
 			System.err.println("Error while parsing date.");
 		}
 
 		return "";
 	}
 	
 	private class PerformLogin extends AsyncTask<Void, Integer, Void> {
 
 		@Override
 		protected Void doInBackground(Void... params) {
 			boolean success = rapi.login(username, password);
 			if (success)
 				loggedIn = true;
 			else
 				loggedIn = false;
 			return null;
 		}
 		
 		@Override
 		protected void onPostExecute(Void result) {
 			super.onPostExecute(result);
 			
 			Toast.makeText(Isense.this, "Logged in: " + loggedIn, Toast.LENGTH_SHORT).show();
 		}
 		
 	}
 		
 
 }
