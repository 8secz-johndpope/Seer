 /* 
  * Copyright (C) 2008 OpenIntents.org
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
 
 package org.openintents.flashlight;
 
 import org.openintents.distribution.AboutActivity;
 import org.openintents.distribution.EulaActivity;
 import org.openintents.distribution.UpdateMenu;
 import org.openintents.intents.FlashlightIntents;
 
 import android.app.Activity;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.IHardwareService;
 import android.os.Message;
 import android.os.PowerManager;
 import android.os.RemoteException;
 import android.os.ServiceManager;
 import android.provider.Settings;
 import android.util.Log;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.Window;
 import android.view.WindowManager;
 import android.widget.LinearLayout;
 import android.widget.TextView;
 
 public class Flashlight extends Activity {
 	
 	private static final String TAG = "Flashlight";
 	private static final boolean debug = true;
 
 	private static final int MENU_COLOR = Menu.FIRST + 1;
 	private static final int MENU_ABOUT = Menu.FIRST + 2;
 	private static final int MENU_UPDATE = Menu.FIRST + 3;
 
     private static final int REQUEST_CODE_PICK_COLOR = 1;
 	
 	private LinearLayout mBackground;
 	private View mIcon;
 	private TextView mText;
 
 	private PowerManager.WakeLock mWakeLock;
 	private boolean mWakeLockLocked = false;
 	
 	private int mColor;
 	
 	private int mUserBrightness;
 	
 	/** Not valid value of brightness */
 	private static final int NOT_VALID = -1;
 	
 	private static final int HIDE_ICON = 1;
 	
 	private static int mTimeout = 5000;
 	
     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         
 		if (!EulaActivity.checkEula(this)) {
 			return;
 		}
 
 		// Turn off the title bar
 		requestWindowFeature(Window.FEATURE_NO_TITLE);
 
         setContentView(R.layout.main);
         
         mColor = 0xffffffff;
         
         mBackground = (LinearLayout) findViewById(R.id.background);
         mIcon = (View) findViewById(R.id.icon);
         mText = (TextView) findViewById(R.id.text);
         
         mBackground.setBackgroundColor(mColor);
         
         mBackground.setOnTouchListener(new View.OnTouchListener() {
 
 			
 			public boolean onTouch(View arg0, MotionEvent arg1) {
 				if (mIcon.getVisibility() == View.VISIBLE) {
 					hideIcon();
 				} else {
 					showIconForAWhile();
 				}
 				return false;
 			}
         	
         });
         
 
 		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
 		//mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
 		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
 				| PowerManager.ACQUIRE_CAUSES_WAKEUP
 				| PowerManager.ON_AFTER_RELEASE,
 				"Flashlight");
 		
 		mUserBrightness = NOT_VALID;
 
 
     }
     
 	@Override
 	protected void onPause() {
 		super.onPause();
 		
 		wakeUnlock();
 	}
 
 
 
 	@Override
 	protected void onResume() {
 		super.onResume();

		IntentFilter i = new IntentFilter(FlashlightIntents.ACTION_SET_FLASHLIGHT);
		registerReceiver(mReceiver, i);
 		
 		wakeLock();
 		
 		showIconForAWhile();
 	}
 
 
 	@Override
 	protected void onStop() {
 		super.onStop();
 		
 		unregisterReceiver(mReceiver);
 	}
 	
 	/////////////////////////////////////////////////////
 	// Menu
 	
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		super.onCreateOptionsMenu(menu);
 
         menu.add(0, MENU_COLOR, 0,R.string.color)
 		  .setIcon(android.R.drawable.ic_menu_manage).setShortcut('3', 'c');
         
         UpdateMenu.addUpdateMenu(this, menu, 0, MENU_UPDATE, 0, R.string.update_menu);
 		
 		menu.add(0, MENU_ABOUT, 0, R.string.about)
 		  .setIcon(android.R.drawable.ic_menu_info_details) .setShortcut('0', 'a');
 
 		return true;
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		super.onPrepareOptionsMenu(menu);
 
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case MENU_COLOR:
             pickColor();
             return true;
         
 		case MENU_ABOUT:
 			showAboutBox();
 			return true;
 
 		case MENU_UPDATE:
 			UpdateMenu.showUpdateBox(this);
 			return true;
 		}
 		return super.onOptionsItemSelected(item);
 
 	}
 
 	/////////////////////////////////////////////////////
 	// Other functions
 	
 	private void wakeLock() {
 		if (!mWakeLockLocked) {
 			Log.d(TAG, "WakeLock: locking");
 			mWakeLock.acquire();
 			mWakeLockLocked = true;
 			boolean res=false;
 			// set screen brightness
 			mUserBrightness = Settings.System.getInt(getContentResolver(), 
 						Settings.System.SCREEN_BRIGHTNESS, NOT_VALID);
 			
 			res=Settings.System.putInt(getContentResolver(), 
 					Settings.System.SCREEN_BRIGHTNESS, 255);
 		//	Log.d(TAG,"res>"+res);
 			setBrightness(true,255);
 			//Log.d(TAG,"brightness>"+Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, NOT_VALID));
 		}
 	}
 
 
 	private void setBrightness(boolean on,int userBrightness) {
 		if (on && !mOldClassAvailable && mNewClassAvailable)
 		{
 			
 			//android 1.5 magic number meaning "full brightness"
 			setBrightnessSDK3(1f);
 		}else if (on && mOldClassAvailable && !mNewClassAvailable)
 		{
 			setBrightnessSDK2(userBrightness);
 		
 		}else if(!on && ! mOldClassAvailable && mNewClassAvailable)
 		{
 			//android 1.5 magic number meaning "default/user brightness"
 			setBrightnessSDK3(-1f);
 		}else if (!on && mOldClassAvailable && !mNewClassAvailable)
 		{
 			setBrightnessSDK2(userBrightness);
 		}
 
 	}
 
 	private void setBrightnessSDK3(float brightness){
 
 		BrightnessNew bn=new BrightnessNew(this);
 		bn.setBrightness(brightness);
 
 	}
 
 
    private static boolean mOldClassAvailable;
    private static boolean mNewClassAvailable;
 
    /* establish whether the "new" class is available to us */
    static {
        try {
            BrightnessOld.checkAvailable();
            mOldClassAvailable = true;
        } catch (Throwable t) {
            mOldClassAvailable = false;
        }
        try {
            BrightnessNew.checkAvailable();
            mNewClassAvailable = true;
        } catch (Throwable t) {
            mNewClassAvailable = false;
        }
 
 
    }
 
 
 
 	private void setBrightnessSDK2(int brightness){
 		BrightnessOld bo=new BrightnessOld();
 		bo.setBrightness(brightness);
 	}
 
 	private void wakeUnlock() {
 		if (mWakeLockLocked) {
 			Log.d(TAG, "WakeLock: unlocking");
 			mWakeLock.release();
 			mWakeLockLocked = false;
 			
 			// Unset screen brightness
 			if (mUserBrightness != NOT_VALID) {
 				Settings.System.putInt(getContentResolver(), 
 						Settings.System.SCREEN_BRIGHTNESS, mUserBrightness);
 				//setBrightness(mUserBrightness);
 			}
 			//android 1.5 magic number meaning "default user setting"
 			setBrightness(false,mUserBrightness);
 		}
 	}
 	
 	
 	private void showAboutBox() {
 		startActivity(new Intent(this, AboutActivity.class));
 	}
 	
 	private void pickColor() {
 		Intent i = new Intent();
 		i.setAction(FlashlightIntents.ACTION_PICK_COLOR);
 		i.putExtra(FlashlightIntents.EXTRA_COLOR, mColor);
 		startActivityForResult(i, REQUEST_CODE_PICK_COLOR);
 	}
 
 	/**
 	 * Broadcast receiver for ACTION_SET_FLASHLIGHT
 	 */
     BroadcastReceiver mReceiver = new BroadcastReceiver() {
 
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			if (intent != null) {
 				mColor = intent.getIntExtra(FlashlightIntents.EXTRA_COLOR, mColor);
 				mBackground.setBackgroundColor(mColor);
 				
 				if (debug) Log.d(TAG, "Receive color " + mColor);
 			}
 		}
     	
     };
 
 	/////////////////////////////////////////////////////
 	// Color changed listener:
 	
     @Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
 		switch(requestCode) {
 		case REQUEST_CODE_PICK_COLOR:
 			if (resultCode == RESULT_OK) {
 				mColor = data.getIntExtra(FlashlightIntents.EXTRA_COLOR, mColor);
 		        mBackground.setBackgroundColor(mColor);
 			}
 			break;
 		}
 	}
     
     ////////////////////
     // Handler
     
     Handler mHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			if (msg.what == HIDE_ICON) {
 				hideIcon();
 			}
 		}
 
 		
 	};
 	
 	/**
 	 * Hides icon and notification bar.
 	 */
 	private void hideIcon() {
 		mIcon.setVisibility(View.GONE);
 		mText.setVisibility(View.GONE);
 		
 
 		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
 				WindowManager.LayoutParams.FLAG_FULLSCREEN);
 	}
 	
 
 	/**
 	 * Shows icon and notification bar, and set timeout for hiding it.
 	 */
 	private void showIconForAWhile() {
 		mIcon.setVisibility(View.VISIBLE);
 		mText.setVisibility(View.VISIBLE);
 		
 		getWindow().setFlags(0,
 						WindowManager.LayoutParams.FLAG_FULLSCREEN);
 		
 
 		mHandler.removeMessages(HIDE_ICON);
 		mHandler.sendMessageDelayed(mHandler
 				.obtainMessage(HIDE_ICON), mTimeout);
 	}
 
 }
