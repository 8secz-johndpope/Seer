 /*
  *      Copyright (C) 2005-2009 Team XBMC
  *      http://xbmc.org
  *
  *  This Program is free software; you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation; either version 2, or (at your option)
  *  any later version.
  *
  *  This Program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with XBMC Remote; see the file license.  If not, write to
  *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
  *  http://www.gnu.org/copyleft/gpl.html
  *
  */
 
 package org.xbmc.android.remote.presentation.activity;
 
 import org.xbmc.android.remote.R;
 import org.xbmc.android.remote.presentation.controller.RemoteController;
 import org.xbmc.android.util.KeyTracker;
 import org.xbmc.android.util.OnLongPressBackKeyTracker;
 import org.xbmc.android.util.KeyTracker.Stage;
 import org.xbmc.eventclient.ButtonCodes;
 
 import android.app.Activity;
 import android.content.Intent;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.Display;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.widget.FrameLayout;
 
 /**
  * Activity for remote control. At the moment that's the good ol' Xbox remote
  * control, more to come...
  * 
  * @author Team XBMC
  */
 public class RemoteActivity extends Activity {
 	
	private final static String TAG = "RemoteActivity";
	
 	private ConfigurationManager mConfigurationManager;
 	private RemoteController mRemoteController;
 
 	private KeyTracker mKeyTracker;
 	
 	public RemoteActivity() {
 		mKeyTracker = new KeyTracker(new OnLongPressBackKeyTracker() {
 
 			@Override
 			public void onLongPressBack(int keyCode, KeyEvent event,
 					Stage stage, int duration) {
 				Intent intent = new Intent(RemoteActivity.this, HomeActivity.class);
 				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
 				startActivity(intent);
 			}
 
 			@Override
 			public void onShortPressBack(int keyCode, KeyEvent event,
 					Stage stage, int duration) {
 				callSuperOnKeyDown(keyCode, event);
 			}
 			
 		});
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		
 		setContentView(R.layout.remote_xbox);
 		
 		// remove nasty top fading edge
 		FrameLayout topFrame = (FrameLayout)findViewById(android.R.id.content);
 		topFrame.setForeground(null);
 		mRemoteController = new RemoteController(getApplicationContext());
 		
 		mConfigurationManager = ConfigurationManager.getInstance(this);
 		mConfigurationManager.initKeyguard(true);
 		
         Display d = getWindowManager().getDefaultDisplay();
         final int w = d.getWidth();
         final int h = d.getHeight();
         final double ar = w > h ? (double)w / (double)h : (double)h / (double)w;
         if (ar > 1.7) {
        	final View sectionRows = findViewById(R.id.RemoteXboxRowSections);
        	if (sectionRows != null) {
        		findViewById(R.id.RemoteXboxRowSections).setVisibility(View.VISIBLE);
        		Log.i(TAG, "AR = " + ar + ", setting section bar to visible.");
        	} else {
        		Log.i(TAG, "AR = " + ar + " but row sections not found.");
        	}
         } else {
        	Log.i(TAG, "AR = " + ar + ", leaving section bar hidden.");
         }
 		setupButtons();
 	}
 
 	@Override
 	public boolean onTrackballEvent(MotionEvent event) {
 		return mRemoteController.onTrackballEvent(event);
 	}
 	
 	@Override
 	public boolean onKeyDown(int keyCode, KeyEvent event) {
 		boolean handled =  mKeyTracker.doKeyDown(keyCode, event);
 		return handled || mRemoteController.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
 	}
 	
 	@Override
 	protected void onResume() {
 		super.onResume();
 		mRemoteController.onActivityResume(this);
 		mConfigurationManager.onActivityResume(this);
 	}
 	
 	@Override
 	protected void onPause() {
 		super.onPause();
 		mConfigurationManager.onActivityPause();
 		mRemoteController.onActivityPause();
 	}
 
 	/**
 	 * Assigns the button events to the views.
 	 */
 	private void setupButtons() {
 		
 		// display
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnDisplay), ButtonCodes.REMOTE_DISPLAY);
 		
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnVideo), ButtonCodes.REMOTE_MY_VIDEOS);
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnMusic), ButtonCodes.REMOTE_MY_MUSIC);
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnImages), ButtonCodes.REMOTE_MY_PICTURES);
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnTv), ButtonCodes.REMOTE_MY_TV);
 		
 		// seek back
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnSeekBack), ButtonCodes.REMOTE_REVERSE);
 		// play
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnPlay), ButtonCodes.REMOTE_PLAY);
 		// seek forward
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnSeekForward), ButtonCodes.REMOTE_FORWARD);
 
 		// previous
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnPrevious), ButtonCodes.REMOTE_SKIP_MINUS);
 		// stop
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnStop), ButtonCodes.REMOTE_STOP);
 		// pause
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnPause), ButtonCodes.REMOTE_PAUSE);
 		// next
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnNext), ButtonCodes.REMOTE_SKIP_PLUS);
 		
 		// title
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnTitle), ButtonCodes.REMOTE_TITLE);
 		// up
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnUp), ButtonCodes.REMOTE_UP);
 		// info
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnInfo), ButtonCodes.REMOTE_INFO);
 		
 		// left
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnLeft), ButtonCodes.REMOTE_LEFT);
 		// select
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnSelect), ButtonCodes.REMOTE_SELECT);
 		// right
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnRight), ButtonCodes.REMOTE_RIGHT);
 		
 		// menu
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnMenu), ButtonCodes.REMOTE_MENU);
 		// down
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnDown), ButtonCodes.REMOTE_DOWN);
 		// back 
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnBack), ButtonCodes.REMOTE_BACK);
 		
 		
 		// videos
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnVideo), ButtonCodes.REMOTE_MY_VIDEOS);
 		// music
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnMusic), ButtonCodes.REMOTE_MY_MUSIC);
 		// pictures
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnImages), ButtonCodes.REMOTE_MY_PICTURES);
 		// tv
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnTv), ButtonCodes.REMOTE_MY_TV);
 		// settings
 		mRemoteController.setupButton(findViewById(R.id.RemoteXboxImgBtnPower), ButtonCodes.REMOTE_POWER);
 	}
 	
 	
 	public boolean onCreateOptionsMenu(Menu menu) {
 		return mRemoteController.onCreateOptionsMenu(menu);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		return mRemoteController.onOptionsItemSelected(item);
 	}
 	
 	protected void callSuperOnKeyDown(int keyCode, KeyEvent event) {
 		super.onKeyDown(keyCode, event);
 	}
 	
 	@Override
 	public boolean onKeyUp(int keyCode, KeyEvent event) {
 		boolean handled = mKeyTracker.doKeyUp(keyCode, event);
 		return handled || super.onKeyUp(keyCode, event);
 	}
 }
