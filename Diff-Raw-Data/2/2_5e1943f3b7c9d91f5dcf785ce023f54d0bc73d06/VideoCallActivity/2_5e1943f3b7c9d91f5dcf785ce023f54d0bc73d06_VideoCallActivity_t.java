 /*
 VideoCallActivity.java
 Copyright (C) 2010  Belledonne Communications, Grenoble, France
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 package org.linphone;
 
 
 
 import org.linphone.core.Log;
 import org.linphone.mediastream.video.AndroidVideoWindowImpl;
 import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
 
 import android.content.Context;
 import android.os.Bundle;
 import android.os.PowerManager;
 import android.os.PowerManager.WakeLock;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.SurfaceHolder;
 import android.view.SurfaceView;
 
 /**
  * For Android SDK >= 5
  * @author Guillaume Beraudo
  *
  */
 public class VideoCallActivity extends SoftVolumeActivity {
 	private SurfaceView mVideoView;
 	private SurfaceView mVideoCaptureView;
 	public static boolean launched = false;
 	private WakeLock mWakeLock;
 	
 	AndroidVideoWindowImpl androidVideoWindowImpl;
 
 	public void onCreate(Bundle savedInstanceState) {		
 		launched = true;
 		Log.d("onCreate VideoCallActivity");
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.videocall);
 
 		mVideoView = (SurfaceView) findViewById(R.id.video_surface); 
 
 		mVideoCaptureView = (SurfaceView) findViewById(R.id.video_capture_surface);
 		mVideoCaptureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
 
 		/* force surfaces Z ordering */
 		mVideoView.setZOrderOnTop(false);
 		mVideoCaptureView.setZOrderOnTop(true);
 	
 		androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, mVideoCaptureView);
 		androidVideoWindowImpl.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {
 			
 			@Override
 			public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw) {
 				LinphoneManager.getLc().setVideoWindow(vw);
 			}
 			
 			@Override
 			public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
 				LinphoneManager.getLc().setVideoWindow(null);
 			}
 			
 			@Override 
 			public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw) {
 				LinphoneManager.getLc().setPreviewWindow(mVideoCaptureView);
 			}
 			
 			@Override
 			public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
 				
 			}
 		});
 		
 		androidVideoWindowImpl.init();
 		
 		// When changing phone orientation _DURING_ a call, VideoCallActivity is destroyed then recreated
 		// In this case, the following sequence happen:
 		//   * onDestroy -> sendStaticImage(true)  => destroy video graph
 		//   * onCreate  -> sendStaticImage(false) => recreate the video graph.
 		// Before creating the graph, the orientation must be known to LC => this is done here
 		LinphoneManager.getLc().setDeviceRotation(AndroidVideoWindowImpl.rotationToAngle(getWindowManager().getDefaultDisplay().getRotation()));
 
		if (!LinphoneManager.getInstance().shareMyCamera()) 
 			LinphoneManager.getInstance().sendStaticImage(false);
 		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
 		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,Log.TAG);
 		mWakeLock.acquire();
 	}
 
  
 	@Override
 	protected void onResume() {
 		super.onResume();
 	}
 
 
 	private void rewriteToggleCameraItem(MenuItem item) {
 		if (LinphoneManager.getLc().getCurrentCall().cameraEnabled()) {
 			item.setTitle(getString(R.string.menu_videocall_toggle_camera_disable));
 		} else {
 			item.setTitle(getString(R.string.menu_videocall_toggle_camera_enable));
 		}
 	}
 
 
 	private void rewriteChangeResolutionItem(MenuItem item) {
 		if (BandwidthManager.getInstance().isUserRestriction()) {
 			item.setTitle(getString(R.string.menu_videocall_change_resolution_when_low_resolution));
 		} else {
 			item.setTitle(getString(R.string.menu_videocall_change_resolution_when_high_resolution));
 		}
 	}
 
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		// Inflate the currently selected menu XML resource.
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.videocall_activity_menu, menu);
 
 		rewriteToggleCameraItem(menu.findItem(R.id.videocall_menu_toggle_camera));
 		rewriteChangeResolutionItem(menu.findItem(R.id.videocall_menu_change_resolution));
 
 		if (!AndroidCameraConfiguration.hasSeveralCameras()) {
 			menu.findItem(R.id.videocall_menu_switch_camera).setVisible(false);
 		}
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.videocall_menu_back_to_dialer:
 			finish();
 			break;
 		case R.id.videocall_menu_change_resolution:
 			LinphoneManager.getInstance().changeResolution();
 			// previous call will cause graph reconstruction -> regive preview window
 			rewriteChangeResolutionItem(item);
 			break;
 		case R.id.videocall_menu_terminate_call:
 			LinphoneManager.getInstance().terminateCall();
 			break;
 		case R.id.videocall_menu_toggle_camera:
 			LinphoneManager.getInstance().toggleEnableCamera();
 			rewriteToggleCameraItem(item);
 			// previous call will cause graph reconstruction -> regive preview window
 			LinphoneManager.getLc().setPreviewWindow(mVideoCaptureView);
 			break;
 		case R.id.videocall_menu_switch_camera:
 			int id = LinphoneManager.getLc().getVideoDevice();
 			id = (id + 1) % AndroidCameraConfiguration.retrieveCameras().length;
 			LinphoneManager.getLc().setVideoDevice(id);
 			CallManager.getInstance().updateCall();
 			// previous call will cause graph reconstruction -> regive preview window
 			LinphoneManager.getLc().setPreviewWindow(mVideoCaptureView);
 			break;
 		default:
 			Log.e("Unknown menu item [",item,"]");
 			break;
 		}
 		return true;
 	}
 
 
 	@Override
 	protected void onDestroy() {
 		androidVideoWindowImpl.release();
 		launched = false;
 		super.onDestroy();
 	}
 
 	@Override
 	protected void onPause() {
 		Log.d("onPause VideoCallActivity");
 		LinphoneManager.getInstance().sendStaticImage(true);
 		if (mWakeLock.isHeld())	mWakeLock.release();
 		super.onPause();
 	}
 }
