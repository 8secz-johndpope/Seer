 /*
  * Copyright (C) 2007 The Android Open Source Project
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
 
 package com.android.camera;
 
 import android.app.Activity;
 import android.content.ActivityNotFoundException;
 import android.content.BroadcastReceiver;
 import android.content.ContentProviderClient;
 import android.content.ContentResolver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.hardware.Camera.Parameters;
 import android.hardware.Camera.PictureCallback;
 import android.hardware.Camera.Size;
 import android.location.Location;
 import android.location.LocationManager;
 import android.location.LocationProvider;
 import android.media.AudioManager;
 import android.media.CameraProfile;
 import android.media.ToneGenerator;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.Debug;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.Looper;
 import android.os.Message;
 import android.os.MessageQueue;
 import android.os.SystemClock;
 import android.provider.MediaStore;
 import android.provider.Settings;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.Display;
 import android.view.GestureDetector;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.OrientationEventListener;
 import android.view.SurfaceHolder;
 import android.view.SurfaceView;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.view.WindowManager;
 import android.view.MenuItem.OnMenuItemClickListener;
 import android.widget.FrameLayout;
 import android.widget.ImageView;
 
 import com.android.camera.gallery.IImage;
 import com.android.camera.gallery.IImageList;
 import com.android.camera.ui.CameraHeadUpDisplay;
 import com.android.camera.ui.GLRootView;
 import com.android.camera.ui.HeadUpDisplay;
 import com.android.camera.ui.ZoomControllerListener;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 
 /** The Camera activity which can preview and take pictures. */
 public class Camera extends NoSearchActivity implements View.OnClickListener,
         ShutterButton.OnShutterButtonListener, SurfaceHolder.Callback,
         Switcher.OnSwitchListener {
 
     private static final String TAG = "camera";
 
     private static final int CROP_MSG = 1;
     private static final int FIRST_TIME_INIT = 2;
     private static final int RESTART_PREVIEW = 3;
     private static final int CLEAR_SCREEN_DELAY = 4;
     private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 5;
 
     // The subset of parameters we need to update in setCameraParameters().
     private static final int UPDATE_PARAM_INITIALIZE = 1;
     private static final int UPDATE_PARAM_ZOOM = 2;
     private static final int UPDATE_PARAM_PREFERENCE = 4;
     private static final int UPDATE_PARAM_ALL = -1;
 
     // When setCameraParametersWhenIdle() is called, we accumulate the subsets
     // needed to be updated in mUpdateSet.
     private int mUpdateSet;
 
     // The brightness settings used when it is set to automatic in the system.
     // The reason why it is set to 0.7 is just because 1.0 is too bright.
     private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;
 
     private static final int SCREEN_DELAY = 2 * 60 * 1000;
     private static final int FOCUS_BEEP_VOLUME = 100;
 
     private static final int ZOOM_STOPPED = 0;
     private static final int ZOOM_START = 1;
     private static final int ZOOM_STOPPING = 2;
 
     private int mZoomState = ZOOM_STOPPED;
     private boolean mSmoothZoomSupported = false;
     private int mZoomValue;  // The current zoom value.
     private int mZoomMax;
     private int mTargetZoomValue;
 
     private Parameters mParameters;
     private Parameters mInitialParams;
 
     private OrientationEventListener mOrientationListener;
     private int mLastOrientation = 0;  // No rotation (landscape) by default.
     private ComboPreferences mPreferences;
 
     private static final int IDLE = 1;
     private static final int SNAPSHOT_IN_PROGRESS = 2;
 
     private static final boolean SWITCH_CAMERA = true;
     private static final boolean SWITCH_VIDEO = false;
 
     private int mStatus = IDLE;
     private static final String sTempCropFilename = "crop-temp";
 
     private android.hardware.Camera mCameraDevice;
     private ContentProviderClient mMediaProviderClient;
     private SurfaceView mSurfaceView;
     private SurfaceHolder mSurfaceHolder = null;
     private ShutterButton mShutterButton;
     private FocusRectangle mFocusRectangle;
     private ToneGenerator mFocusToneGenerator;
     private GestureDetector mGestureDetector;
     private Switcher mSwitcher;
     private boolean mStartPreviewFail = false;
 
     private GLRootView mGLRootView;
 
     // mPostCaptureAlert, mLastPictureButton, mThumbController
     // are non-null only if isImageCaptureIntent() is true.
     private ImageView mLastPictureButton;
     private ThumbnailController mThumbController;
 
     // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
     private String mCropValue;
     private Uri mSaveUri;
 
     private ImageCapture mImageCapture = null;
 
     private boolean mPreviewing;
     private boolean mPausing;
     private boolean mSwitching;
     private boolean mFirstTimeInitialized;
     private boolean mIsImageCaptureIntent;
     private boolean mRecordLocation;
 
     private static final int FOCUS_NOT_STARTED = 0;
     private static final int FOCUSING = 1;
     private static final int FOCUSING_SNAP_ON_FINISH = 2;
     private static final int FOCUS_SUCCESS = 3;
     private static final int FOCUS_FAIL = 4;
     private int mFocusState = FOCUS_NOT_STARTED;
 
     private ContentResolver mContentResolver;
     private boolean mDidRegister = false;
 
     private final ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();
 
     private LocationManager mLocationManager = null;
 
     private final ShutterCallback mShutterCallback = new ShutterCallback();
     private final PostViewPictureCallback mPostViewPictureCallback =
             new PostViewPictureCallback();
     private final RawPictureCallback mRawPictureCallback =
             new RawPictureCallback();
     private final AutoFocusCallback mAutoFocusCallback =
             new AutoFocusCallback();
     private final ZoomListener mZoomListener = new ZoomListener();
     // Use the ErrorCallback to capture the crash count
     // on the mediaserver
     private final ErrorCallback mErrorCallback = new ErrorCallback();
 
     private long mFocusStartTime;
     private long mFocusCallbackTime;
     private long mCaptureStartTime;
     private long mShutterCallbackTime;
     private long mPostViewPictureCallbackTime;
     private long mRawPictureCallbackTime;
     private long mJpegPictureCallbackTime;
     private int mPicturesRemaining;
 
     // These latency time are for the CameraLatency test.
     public long mAutoFocusTime;
     public long mShutterLag;
     public long mShutterToPictureDisplayedTime;
     public long mPictureDisplayedToJpegCallbackTime;
     public long mJpegCallbackFinishTime;
 
     // Add for test
     public static boolean mMediaServerDied = false;
 
     // Focus mode. Options are pref_camera_focusmode_entryvalues.
     private String mFocusMode;
     private String mSceneMode;
 
     private final Handler mHandler = new MainHandler();
     private boolean mQuickCapture;
     private CameraHeadUpDisplay mHeadUpDisplay;
 
     // multiple cameras support
     private int mNumberOfCameras;
     private int mCameraId;
 
     /**
      * This Handler is used to post message back onto the main thread of the
      * application
      */
     private class MainHandler extends Handler {
         @Override
         public void handleMessage(Message msg) {
             switch (msg.what) {
                 case RESTART_PREVIEW: {
                     restartPreview();
                     if (mJpegPictureCallbackTime != 0) {
                         long now = System.currentTimeMillis();
                         mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
                         Log.v(TAG, "mJpegCallbackFinishTime = "
                                 + mJpegCallbackFinishTime + "ms");
                         mJpegPictureCallbackTime = 0;
                     }
                     break;
                 }
 
                 case CLEAR_SCREEN_DELAY: {
                     getWindow().clearFlags(
                             WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                     break;
                 }
 
                 case FIRST_TIME_INIT: {
                     initializeFirstTime();
                     break;
                 }
 
                 case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                     setCameraParametersWhenIdle(0);
                     break;
                 }
             }
         }
     }
 
     private void resetExposureCompensation() {
         String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
                 CameraSettings.EXPOSURE_DEFAULT_VALUE);
         if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
             Editor editor = mPreferences.edit();
             editor.putString(CameraSettings.KEY_EXPOSURE, "0");
             editor.commit();
             if (mHeadUpDisplay != null) {
                 mHeadUpDisplay.reloadPreferences();
             }
         }
     }
 
     private void keepMediaProviderInstance() {
         // We want to keep a reference to MediaProvider in camera's lifecycle.
         // TODO: Utilize mMediaProviderClient instance to replace
         // ContentResolver calls.
         if (mMediaProviderClient == null) {
             mMediaProviderClient = getContentResolver()
                     .acquireContentProviderClient(MediaStore.AUTHORITY);
         }
     }
 
     // Snapshots can only be taken after this is called. It should be called
     // once only. We could have done these things in onCreate() but we want to
     // make preview screen appear as soon as possible.
     private void initializeFirstTime() {
         if (mFirstTimeInitialized) return;
 
         // Create orientation listenter. This should be done first because it
         // takes some time to get first orientation.
         mOrientationListener = new OrientationEventListener(Camera.this) {
             @Override
             public void onOrientationChanged(int orientation) {
                 // We keep the last known orientation. So if the user
                 // first orient the camera then point the camera to
                 if (orientation != ORIENTATION_UNKNOWN) {
                     orientation += 90;
                 }
                 orientation = ImageManager.roundOrientation(orientation);
                 if (orientation != mLastOrientation) {
                     mLastOrientation = orientation;
                     if (!mIsImageCaptureIntent)  {
                         setOrientationIndicator(mLastOrientation);
                     }
                     mHeadUpDisplay.setOrientation(mLastOrientation);
                 }
             }
         };
         mOrientationListener.enable();
 
         // Initialize location sevice.
         mLocationManager = (LocationManager)
                 getSystemService(Context.LOCATION_SERVICE);
         mRecordLocation = RecordLocationPreference.get(
                 mPreferences, getContentResolver());
         if (mRecordLocation) startReceivingLocationUpdates();
 
         keepMediaProviderInstance();
         checkStorage();
 
         // Initialize last picture button.
         mContentResolver = getContentResolver();
         if (!mIsImageCaptureIntent)  {
             findViewById(R.id.camera_switch).setOnClickListener(this);
             mLastPictureButton =
                     (ImageView) findViewById(R.id.review_thumbnail);
             mLastPictureButton.setOnClickListener(this);
             mThumbController = new ThumbnailController(
                     getResources(), mLastPictureButton, mContentResolver);
             mThumbController.loadData(ImageManager.getLastImageThumbPath());
             // Update last image thumbnail.
             updateThumbnailButton();
         }
 
         // Initialize shutter button.
         mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
         mShutterButton.setOnShutterButtonListener(this);
         mShutterButton.setVisibility(View.VISIBLE);
 
         mFocusRectangle = (FocusRectangle) findViewById(R.id.focus_rectangle);
         updateFocusIndicator();
 
         initializeScreenBrightness();
         installIntentFilter();
         initializeFocusTone();
         initializeZoom();
         initializeHeadUpDisplay();
         mFirstTimeInitialized = true;
         changeHeadUpDisplayState();
         addIdleHandler();
     }
 
     private void addIdleHandler() {
         MessageQueue queue = Looper.myQueue();
         queue.addIdleHandler(new MessageQueue.IdleHandler() {
             public boolean queueIdle() {
                 ImageManager.ensureOSXCompatibleFolder();
                 return false;
             }
         });
     }
 
     private void updateThumbnailButton() {
         // Update last image if URI is invalid and the storage is ready.
         if (!mThumbController.isUriValid() && mPicturesRemaining >= 0) {
             updateLastImage();
         }
         mThumbController.updateDisplayIfNeeded();
     }
 
     // If the activity is paused and resumed, this method will be called in
     // onResume.
     private void initializeSecondTime() {
         // Start orientation listener as soon as possible because it takes
         // some time to get first orientation.
         mOrientationListener.enable();
 
         // Start location update if needed.
         mRecordLocation = RecordLocationPreference.get(
                 mPreferences, getContentResolver());
         if (mRecordLocation) startReceivingLocationUpdates();
 
         installIntentFilter();
         initializeFocusTone();
         initializeZoom();
         changeHeadUpDisplayState();
 
         keepMediaProviderInstance();
         checkStorage();
 
         if (!mIsImageCaptureIntent) {
             updateThumbnailButton();
         }
     }
 
     private void initializeZoom() {
         if (!mParameters.isZoomSupported()) return;
 
         mZoomMax = mParameters.getMaxZoom();
         mSmoothZoomSupported = mParameters.isSmoothZoomSupported();
         mGestureDetector = new GestureDetector(this, new ZoomGestureListener());
 
         mCameraDevice.setZoomChangeListener(mZoomListener);
     }
 
     private void onZoomValueChanged(int index) {
         if (mSmoothZoomSupported) {
             if (mTargetZoomValue != index && mZoomState != ZOOM_STOPPED) {
                 mTargetZoomValue = index;
                 if (mZoomState == ZOOM_START) {
                     mZoomState = ZOOM_STOPPING;
                     mCameraDevice.stopSmoothZoom();
                 }
             } else if (mZoomState == ZOOM_STOPPED && mZoomValue != index) {
                 mTargetZoomValue = index;
                 mCameraDevice.startSmoothZoom(index);
                 mZoomState = ZOOM_START;
             }
         } else {
             mZoomValue = index;
             setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
         }
     }
 
     private float[] getZoomRatios() {
         List<Integer> zoomRatios = mParameters.getZoomRatios();
         if (zoomRatios != null) {
             float result[] = new float[zoomRatios.size()];
             for (int i = 0, n = result.length; i < n; ++i) {
                 result[i] = (float) zoomRatios.get(i) / 100f;
             }
             return result;
         } else {
             throw new IllegalStateException("cannot get zoom ratios");
         }
     }
 
     private class ZoomGestureListener extends
             GestureDetector.SimpleOnGestureListener {
 
         @Override
         public boolean onDoubleTap(MotionEvent e) {
             // Perform zoom only when preview is started and snapshot is not in
             // progress.
             if (mPausing || !isCameraIdle() || !mPreviewing
                     || mZoomState != ZOOM_STOPPED) {
                 return false;
             }
 
             if (mZoomValue < mZoomMax) {
                 // Zoom in to the maximum.
                 mZoomValue = mZoomMax;
             } else {
                 mZoomValue = 0;
             }
 
             setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
 
             mHeadUpDisplay.setZoomIndex(mZoomValue);
             return true;
         }
     }
 
     @Override
     public boolean dispatchTouchEvent(MotionEvent m) {
         if (!super.dispatchTouchEvent(m) && mGestureDetector != null) {
             return mGestureDetector.onTouchEvent(m);
         }
         return true;
     }
 
     LocationListener [] mLocationListeners = new LocationListener[] {
             new LocationListener(LocationManager.GPS_PROVIDER),
             new LocationListener(LocationManager.NETWORK_PROVIDER)
     };
 
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                     || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                     || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                 checkStorage();
             } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                 checkStorage();
                 if (!mIsImageCaptureIntent)  {
                     updateThumbnailButton();
                 }
             }
         }
     };
 
     private class LocationListener
             implements android.location.LocationListener {
         Location mLastLocation;
         boolean mValid = false;
         String mProvider;
 
         public LocationListener(String provider) {
             mProvider = provider;
             mLastLocation = new Location(mProvider);
         }
 
         public void onLocationChanged(Location newLocation) {
             if (newLocation.getLatitude() == 0.0
                     && newLocation.getLongitude() == 0.0) {
                 // Hack to filter out 0.0,0.0 locations
                 return;
             }
             // If GPS is available before start camera, we won't get status
             // update so update GPS indicator when we receive data.
             if (mRecordLocation
                     && LocationManager.GPS_PROVIDER.equals(mProvider)) {
                 if (mHeadUpDisplay != null) {
                     mHeadUpDisplay.setGpsHasSignal(true);
                 }
             }
             mLastLocation.set(newLocation);
             mValid = true;
         }
 
         public void onProviderEnabled(String provider) {
         }
 
         public void onProviderDisabled(String provider) {
             mValid = false;
         }
 
         public void onStatusChanged(
                 String provider, int status, Bundle extras) {
             switch(status) {
                 case LocationProvider.OUT_OF_SERVICE:
                 case LocationProvider.TEMPORARILY_UNAVAILABLE: {
                     mValid = false;
                     if (mRecordLocation &&
                             LocationManager.GPS_PROVIDER.equals(provider)) {
                         if (mHeadUpDisplay != null) {
                             mHeadUpDisplay.setGpsHasSignal(false);
                         }
                     }
                     break;
                 }
             }
         }
 
         public Location current() {
             return mValid ? mLastLocation : null;
         }
     }
 
     private final class ShutterCallback
             implements android.hardware.Camera.ShutterCallback {
         public void onShutter() {
             mShutterCallbackTime = System.currentTimeMillis();
             mShutterLag = mShutterCallbackTime - mCaptureStartTime;
             Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
             clearFocusState();
         }
     }
 
     private final class PostViewPictureCallback implements PictureCallback {
         public void onPictureTaken(
                 byte [] data, android.hardware.Camera camera) {
             mPostViewPictureCallbackTime = System.currentTimeMillis();
             Log.v(TAG, "mShutterToPostViewCallbackTime = "
                     + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                     + "ms");
         }
     }
 
     private final class RawPictureCallback implements PictureCallback {
         public void onPictureTaken(
                 byte [] rawData, android.hardware.Camera camera) {
             mRawPictureCallbackTime = System.currentTimeMillis();
             Log.v(TAG, "mShutterToRawCallbackTime = "
                     + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
         }
     }
 
     private final class JpegPictureCallback implements PictureCallback {
         Location mLocation;
 
         public JpegPictureCallback(Location loc) {
             mLocation = loc;
         }
 
         public void onPictureTaken(
                 final byte [] jpegData, final android.hardware.Camera camera) {
             if (mPausing) {
                 return;
             }
 
             mJpegPictureCallbackTime = System.currentTimeMillis();
             // If postview callback has arrived, the captured image is displayed
             // in postview callback. If not, the captured image is displayed in
             // raw picture callback.
             if (mPostViewPictureCallbackTime != 0) {
                 mShutterToPictureDisplayedTime =
                         mPostViewPictureCallbackTime - mShutterCallbackTime;
                 mPictureDisplayedToJpegCallbackTime =
                         mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
             } else {
                 mShutterToPictureDisplayedTime =
                         mRawPictureCallbackTime - mShutterCallbackTime;
                 mPictureDisplayedToJpegCallbackTime =
                         mJpegPictureCallbackTime - mRawPictureCallbackTime;
             }
             Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                     + mPictureDisplayedToJpegCallbackTime + "ms");
             mHeadUpDisplay.setEnabled(true);
 
             if (!mIsImageCaptureIntent) {
                 // We want to show the taken picture for a while, so we wait
                 // for at least 1.2 second before restarting the preview.
                 long delay = 1200 - mPictureDisplayedToJpegCallbackTime;
                 if (delay < 0 || mQuickCapture) {
                     restartPreview();
                 } else {
                     mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW, delay);
                 }
             }
             mImageCapture.storeImage(jpegData, camera, mLocation);
 
             // Calculate this in advance of each shot so we don't add to shutter
             // latency. It's true that someone else could write to the SD card in
             // the mean time and fill it, but that could have happened between the
             // shutter press and saving the JPEG too.
             calculatePicturesRemaining();
 
             if (mPicturesRemaining < 1) {
                 updateStorageHint(mPicturesRemaining);
             }
 
             if (!mHandler.hasMessages(RESTART_PREVIEW)) {
                 long now = System.currentTimeMillis();
                 mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
                 Log.v(TAG, "mJpegCallbackFinishTime = "
                         + mJpegCallbackFinishTime + "ms");
                 mJpegPictureCallbackTime = 0;
             }
         }
     }
 
     private final class AutoFocusCallback
             implements android.hardware.Camera.AutoFocusCallback {
         public void onAutoFocus(
                 boolean focused, android.hardware.Camera camera) {
             mFocusCallbackTime = System.currentTimeMillis();
             mAutoFocusTime = mFocusCallbackTime - mFocusStartTime;
             Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
             if (mFocusState == FOCUSING_SNAP_ON_FINISH) {
                 // Take the picture no matter focus succeeds or fails. No need
                 // to play the AF sound if we're about to play the shutter
                 // sound.
                 if (focused) {
                     mFocusState = FOCUS_SUCCESS;
                 } else {
                     mFocusState = FOCUS_FAIL;
                 }
                 mImageCapture.onSnap();
             } else if (mFocusState == FOCUSING) {
                 // User is half-pressing the focus key. Play the focus tone.
                 // Do not take the picture now.
                 ToneGenerator tg = mFocusToneGenerator;
                 if (tg != null) {
                     tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                 }
                 if (focused) {
                     mFocusState = FOCUS_SUCCESS;
                 } else {
                     mFocusState = FOCUS_FAIL;
                 }
             } else if (mFocusState == FOCUS_NOT_STARTED) {
                 // User has released the focus key before focus completes.
                 // Do nothing.
             }
             updateFocusIndicator();
         }
     }
 
     private static final class ErrorCallback
         implements android.hardware.Camera.ErrorCallback {
         public void onError(int error, android.hardware.Camera camera) {
             if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
                  mMediaServerDied = true;
                  Log.v(TAG, "media server died");
             }
         }
     }
 
     private final class ZoomListener
             implements android.hardware.Camera.OnZoomChangeListener {
         public void onZoomChange(
                 int value, boolean stopped, android.hardware.Camera camera) {
             Log.v(TAG, "Zoom changed: value=" + value + ". stopped="+ stopped);
             mZoomValue = value;
             // Keep mParameters up to date. We do not getParameter again in
             // takePicture. If we do not do this, wrong zoom value will be set.
             mParameters.setZoom(value);
             // We only care if the zoom is stopped. mZooming is set to true when
             // we start smooth zoom.
             if (stopped && mZoomState != ZOOM_STOPPED) {
                 if (value != mTargetZoomValue) {
                     mCameraDevice.startSmoothZoom(mTargetZoomValue);
                     mZoomState = ZOOM_START;
                 } else {
                     mZoomState = ZOOM_STOPPED;
                 }
             }
         }
     }
 
     private class ImageCapture {
 
         private Uri mLastContentUri;
 
         byte[] mCaptureOnlyData;
 
         // Returns the rotation degree in the jpeg header.
         private int storeImage(byte[] data, Location loc) {
             try {
                 long dateTaken = System.currentTimeMillis();
                 String title = createName(dateTaken);
                 String filename = title + ".jpg";
                 int[] degree = new int[1];
                 mLastContentUri = ImageManager.addImage(
                         mContentResolver,
                         title,
                         dateTaken,
                         loc, // location from gps/network
                         ImageManager.CAMERA_IMAGE_BUCKET_NAME, filename,
                         null, data,
                         degree);
                 return degree[0];
             } catch (Exception ex) {
                 Log.e(TAG, "Exception while compressing image.", ex);
                 return 0;
             }
         }
 
         public void storeImage(final byte[] data,
                 android.hardware.Camera camera, Location loc) {
             if (!mIsImageCaptureIntent) {
                 int degree = storeImage(data, loc);
                 sendBroadcast(new Intent(
                         "com.android.camera.NEW_PICTURE", mLastContentUri));
                 setLastPictureThumb(data, degree,
                         mImageCapture.getLastCaptureUri());
                 mThumbController.updateDisplayIfNeeded();
             } else {
                 mCaptureOnlyData = data;
                 showPostCaptureAlert();
             }
         }
 
         /**
          * Initiate the capture of an image.
          */
         public void initiate() {
             if (mCameraDevice == null) {
                 return;
             }
 
             capture();
         }
 
         public Uri getLastCaptureUri() {
             return mLastContentUri;
         }
 
         public byte[] getLastCaptureData() {
             return mCaptureOnlyData;
         }
 
         private void capture() {
             mCaptureOnlyData = null;
 
             // Set rotation.
             mParameters.setRotation(mLastOrientation);
 
             // Clear previous GPS location from the parameters.
             mParameters.removeGpsData();
 
             // We always encode GpsTimeStamp
             mParameters.setGpsTimestamp(System.currentTimeMillis() / 1000);
 
             // Set GPS location.
             Location loc = mRecordLocation ? getCurrentLocation() : null;
             if (loc != null) {
                 double lat = loc.getLatitude();
                 double lon = loc.getLongitude();
                 boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);
 
                 if (hasLatLon) {
                     mParameters.setGpsLatitude(lat);
                     mParameters.setGpsLongitude(lon);
                     mParameters.setGpsProcessingMethod(loc.getProvider().toUpperCase());
                     if (loc.hasAltitude()) {
                         mParameters.setGpsAltitude(loc.getAltitude());
                     } else {
                         // for NETWORK_PROVIDER location provider, we may have
                         // no altitude information, but the driver needs it, so
                         // we fake one.
                         mParameters.setGpsAltitude(0);
                     }
                     if (loc.getTime() != 0) {
                         // Location.getTime() is UTC in milliseconds.
                         // gps-timestamp is UTC in seconds.
                         long utcTimeSeconds = loc.getTime() / 1000;
                         mParameters.setGpsTimestamp(utcTimeSeconds);
                     }
                 } else {
                     loc = null;
                 }
             }
 
             mCameraDevice.setParameters(mParameters);
 
             mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                     mPostViewPictureCallback, new JpegPictureCallback(loc));
             mPreviewing = false;
         }
 
         public void onSnap() {
             // If we are already in the middle of taking a snapshot then ignore.
             if (mPausing || mStatus == SNAPSHOT_IN_PROGRESS) {
                 return;
             }
             mCaptureStartTime = System.currentTimeMillis();
             mPostViewPictureCallbackTime = 0;
             mHeadUpDisplay.setEnabled(false);
             mStatus = SNAPSHOT_IN_PROGRESS;
 
             mImageCapture.initiate();
         }
 
         private void clearLastData() {
             mCaptureOnlyData = null;
         }
     }
 
     private boolean saveDataToFile(String filePath, byte[] data) {
         FileOutputStream f = null;
         try {
             f = new FileOutputStream(filePath);
             f.write(data);
         } catch (IOException e) {
             return false;
         } finally {
             MenuHelper.closeSilently(f);
         }
         return true;
     }
 
     private void setLastPictureThumb(byte[] data, int degree, Uri uri) {
         BitmapFactory.Options options = new BitmapFactory.Options();
         options.inSampleSize = 16;
         Bitmap lastPictureThumb =
                 BitmapFactory.decodeByteArray(data, 0, data.length, options);
         lastPictureThumb = Util.rotate(lastPictureThumb, degree);
         mThumbController.setData(uri, lastPictureThumb);
     }
 
     private String createName(long dateTaken) {
         Date date = new Date(dateTaken);
         SimpleDateFormat dateFormat = new SimpleDateFormat(
                 getString(R.string.image_file_name_format));
 
         return dateFormat.format(date);
     }
 
     @Override
     public void onCreate(Bundle icicle) {
         super.onCreate(icicle);
 
         setContentView(R.layout.camera);
         mSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);
 
         mPreferences = new ComboPreferences(this);
         CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
         mCameraId = CameraSettings.readPreferredCameraId(mPreferences);
         mPreferences.setLocalId(this, mCameraId);
         CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
 
         mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
 
         // comment out -- unused now.
         //mQuickCapture = getQuickCaptureSettings();
 
         // we need to reset exposure for the preview
         resetExposureCompensation();
         /*
          * To reduce startup time, we start the preview in another thread.
          * We make sure the preview is started at the end of onCreate.
          */
         Thread startPreviewThread = new Thread(new Runnable() {
             public void run() {
                 try {
                     mStartPreviewFail = false;
                     startPreview();
                 } catch (CameraHardwareException e) {
                     // In eng build, we throw the exception so that test tool
                     // can detect it and report it
                     if ("eng".equals(Build.TYPE)) {
                         throw new RuntimeException(e);
                     }
                     mStartPreviewFail = true;
                 }
             }
         });
         startPreviewThread.start();
 
         // don't set mSurfaceHolder here. We have it set ONLY within
         // surfaceChanged / surfaceDestroyed, other parts of the code
         // assume that when it is set, the surface is also set.
         SurfaceHolder holder = mSurfaceView.getHolder();
         holder.addCallback(this);
         holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
 
         mIsImageCaptureIntent = isImageCaptureIntent();
         if (mIsImageCaptureIntent) {
             setupCaptureParams();
         }
 
         LayoutInflater inflater = getLayoutInflater();
 
         ViewGroup rootView = (ViewGroup) findViewById(R.id.camera);
         if (mIsImageCaptureIntent) {
             View controlBar = inflater.inflate(
                     R.layout.attach_camera_control, rootView);
             controlBar.findViewById(R.id.btn_cancel).setOnClickListener(this);
             controlBar.findViewById(R.id.btn_retake).setOnClickListener(this);
             controlBar.findViewById(R.id.btn_done).setOnClickListener(this);
         } else {
             inflater.inflate(R.layout.camera_control, rootView);
             mSwitcher = ((Switcher) findViewById(R.id.camera_switch));
             mSwitcher.setOnSwitchListener(this);
             mSwitcher.addTouchView(findViewById(R.id.camera_switch_set));
         }
 
         // Make sure preview is started.
         try {
             startPreviewThread.join();
             if (mStartPreviewFail) {
                 showCameraErrorAndFinish();
                 return;
             }
         } catch (InterruptedException ex) {
             // ignore
         }
     }
 
     private void changeHeadUpDisplayState() {
         // If the camera resumes behind the lock screen, the orientation
         // will be portrait. That causes OOM when we try to allocation GPU
         // memory for the GLSurfaceView again when the orientation changes. So,
         // we delayed initialization of HeadUpDisplay until the orientation
         // becomes landscape.
         Configuration config = getResources().getConfiguration();
         if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
                 && !mPausing && !mSwitching && mFirstTimeInitialized) {
             if (mGLRootView == null) attachHeadUpDisplay();
         } else if (mGLRootView != null) {
             detachHeadUpDisplay();
         }
     }
 
     private void overrideHudSettings(final String flashMode,
             final String whiteBalance, final String focusMode) {
         mHeadUpDisplay.overrideSettings(
                 CameraSettings.KEY_FLASH_MODE, flashMode,
                 CameraSettings.KEY_WHITE_BALANCE, whiteBalance,
                 CameraSettings.KEY_FOCUS_MODE, focusMode);
     }
 
     private void updateSceneModeInHud() {
         // If scene mode is set, we cannot set flash mode, white balance, and
         // focus mode, instead, we read it from driver
         if (!Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
             overrideHudSettings(mParameters.getFlashMode(),
                     mParameters.getWhiteBalance(), mParameters.getFocusMode());
         } else {
             overrideHudSettings(null, null, null);
         }
     }
 
     private void initializeHeadUpDisplay() {
         mHeadUpDisplay = new CameraHeadUpDisplay(this);
         CameraSettings settings = new CameraSettings(this, mInitialParams);
         mHeadUpDisplay.initialize(this,
                 settings.getPreferenceGroup(R.xml.camera_preferences));
         mHeadUpDisplay.setListener(new MyHeadUpDisplayListener());
     }
 
     private void attachHeadUpDisplay() {
         mHeadUpDisplay.setOrientation(mLastOrientation);
         if (mParameters.isZoomSupported()) {
             mHeadUpDisplay.setZoomRatios(getZoomRatios());
             mHeadUpDisplay.setZoomIndex(mZoomValue);
             mHeadUpDisplay.setZoomListener(new ZoomControllerListener() {
                 public void onZoomChanged(
                         int index, float ratio, boolean isMoving) {
                     onZoomValueChanged(index);
                 }
             });
         }
         FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
         mGLRootView = new GLRootView(this);
         mGLRootView.setContentPane(mHeadUpDisplay);
         frame.addView(mGLRootView);
 
         updateSceneModeInHud();
     }
 
     private void detachHeadUpDisplay() {
         mHeadUpDisplay.setGpsHasSignal(false);
         mHeadUpDisplay.collapse();
         ((ViewGroup) mGLRootView.getParent()).removeView(mGLRootView);
         mGLRootView = null;
     }
 
     private void setOrientationIndicator(int degree) {
         ((RotateImageView) findViewById(
                 R.id.review_thumbnail)).setDegree(degree);
         ((RotateImageView) findViewById(
                 R.id.camera_switch_icon)).setDegree(degree);
         ((RotateImageView) findViewById(
                 R.id.video_switch_icon)).setDegree(degree);
     }
 
     @Override
     public void onStart() {
         super.onStart();
         if (!mIsImageCaptureIntent) {
             mSwitcher.setSwitch(SWITCH_CAMERA);
         }
     }
 
     @Override
     public void onStop() {
         super.onStop();
         if (mMediaProviderClient != null) {
             mMediaProviderClient.release();
             mMediaProviderClient = null;
         }
     }
 
     private void checkStorage() {
         calculatePicturesRemaining();
         updateStorageHint(mPicturesRemaining);
     }
 
     public void onClick(View v) {
         switch (v.getId()) {
             case R.id.btn_retake:
                 hidePostCaptureAlert();
                 restartPreview();
                 break;
             case R.id.review_thumbnail:
                 if (isCameraIdle()) {
                     viewLastImage();
                 }
                 break;
             case R.id.btn_done:
                 doAttach();
                 break;
             case R.id.btn_cancel:
                 doCancel();
         }
     }
 
     private Bitmap createCaptureBitmap(byte[] data) {
         // This is really stupid...we just want to read the orientation in
         // the jpeg header.
         String filepath = ImageManager.getTempJpegPath();
         int degree = 0;
         if (saveDataToFile(filepath, data)) {
             degree = ImageManager.getExifOrientation(filepath);
             new File(filepath).delete();
         }
 
         // Limit to 50k pixels so we can return it in the intent.
         Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
         bitmap = Util.rotate(bitmap, degree);
         return bitmap;
     }
 
     private void doAttach() {
         if (mPausing) {
             return;
         }
 
         byte[] data = mImageCapture.getLastCaptureData();
 
         if (mCropValue == null) {
             // First handle the no crop case -- just return the value.  If the
             // caller specifies a "save uri" then write the data to it's
             // stream. Otherwise, pass back a scaled down version of the bitmap
             // directly in the extras.
             if (mSaveUri != null) {
                 OutputStream outputStream = null;
                 try {
                     outputStream = mContentResolver.openOutputStream(mSaveUri);
                     outputStream.write(data);
                     outputStream.close();
 
                     setResult(RESULT_OK);
                     finish();
                 } catch (IOException ex) {
                     // ignore exception
                 } finally {
                     Util.closeSilently(outputStream);
                 }
             } else {
                 Bitmap bitmap = createCaptureBitmap(data);
                 setResult(RESULT_OK,
                         new Intent("inline-data").putExtra("data", bitmap));
                 finish();
             }
         } else {
             // Save the image to a temp file and invoke the cropper
             Uri tempUri = null;
             FileOutputStream tempStream = null;
             try {
                 File path = getFileStreamPath(sTempCropFilename);
                 path.delete();
                 tempStream = openFileOutput(sTempCropFilename, 0);
                 tempStream.write(data);
                 tempStream.close();
                 tempUri = Uri.fromFile(path);
             } catch (FileNotFoundException ex) {
                 setResult(Activity.RESULT_CANCELED);
                 finish();
                 return;
             } catch (IOException ex) {
                 setResult(Activity.RESULT_CANCELED);
                 finish();
                 return;
             } finally {
                 Util.closeSilently(tempStream);
             }
 
             Bundle newExtras = new Bundle();
             if (mCropValue.equals("circle")) {
                 newExtras.putString("circleCrop", "true");
             }
             if (mSaveUri != null) {
                 newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
             } else {
                 newExtras.putBoolean("return-data", true);
             }
 
             Intent cropIntent = new Intent("com.android.camera.action.CROP");
 
             cropIntent.setData(tempUri);
             cropIntent.putExtras(newExtras);
 
             startActivityForResult(cropIntent, CROP_MSG);
         }
     }
 
     private void doCancel() {
         setResult(RESULT_CANCELED, new Intent());
         finish();
     }
 
     public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
         if (mPausing) {
             return;
         }
         switch (button.getId()) {
             case R.id.shutter_button:
                 doFocus(pressed);
                 break;
         }
     }
 
     public void onShutterButtonClick(ShutterButton button) {
         if (mPausing) {
             return;
         }
         switch (button.getId()) {
             case R.id.shutter_button:
                 doSnap();
                 break;
         }
     }
 
     private OnScreenHint mStorageHint;
 
     private void updateStorageHint(int remaining) {
         String noStorageText = null;
 
         if (remaining == MenuHelper.NO_STORAGE_ERROR) {
             String state = Environment.getExternalStorageState();
             if (state == Environment.MEDIA_CHECKING) {
                 noStorageText = getString(R.string.preparing_sd);
             } else {
                 noStorageText = getString(R.string.no_storage);
             }
         } else if (remaining == MenuHelper.CANNOT_STAT_ERROR) {
             noStorageText = getString(R.string.access_sd_fail);
         } else if (remaining < 1) {
             noStorageText = getString(R.string.not_enough_space);
         }
 
         if (noStorageText != null) {
             if (mStorageHint == null) {
                 mStorageHint = OnScreenHint.makeText(this, noStorageText);
             } else {
                 mStorageHint.setText(noStorageText);
             }
             mStorageHint.show();
         } else if (mStorageHint != null) {
             mStorageHint.cancel();
             mStorageHint = null;
         }
     }
 
     private void installIntentFilter() {
         // install an intent filter to receive SD card related events.
         IntentFilter intentFilter =
                 new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
         intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
         intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
         intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
         intentFilter.addDataScheme("file");
         registerReceiver(mReceiver, intentFilter);
         mDidRegister = true;
     }
 
     private void initializeFocusTone() {
         // Initialize focus tone generator.
         try {
             mFocusToneGenerator = new ToneGenerator(
                     AudioManager.STREAM_SYSTEM, FOCUS_BEEP_VOLUME);
         } catch (Throwable ex) {
             Log.w(TAG, "Exception caught while creating tone generator: ", ex);
             mFocusToneGenerator = null;
         }
     }
 
     private void initializeScreenBrightness() {
         Window win = getWindow();
         // Overright the brightness settings if it is automatic
         int mode = Settings.System.getInt(
                 getContentResolver(),
                 Settings.System.SCREEN_BRIGHTNESS_MODE,
                 Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
         if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
             WindowManager.LayoutParams winParams = win.getAttributes();
             winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
             win.setAttributes(winParams);
         }
     }
 
     @Override
     protected void onResume() {
         super.onResume();
 
         mPausing = false;
         mJpegPictureCallbackTime = 0;
         mZoomValue = 0;
         mImageCapture = new ImageCapture();
 
         // Start the preview if it is not started.
         if (!mPreviewing && !mStartPreviewFail) {
             resetExposureCompensation();
             if (!restartPreview()) return;
         }
 
         if (mSurfaceHolder != null) {
             // If first time initialization is not finished, put it in the
             // message queue.
             if (!mFirstTimeInitialized) {
                 mHandler.sendEmptyMessage(FIRST_TIME_INIT);
             } else {
                 initializeSecondTime();
             }
         }
         keepScreenOnAwhile();
     }
 
     @Override
     public void onConfigurationChanged(Configuration config) {
         super.onConfigurationChanged(config);
         changeHeadUpDisplayState();
     }
 
     private static ImageManager.DataLocation dataLocation() {
         return ImageManager.DataLocation.EXTERNAL;
     }
 
     @Override
     protected void onPause() {
         mPausing = true;
         stopPreview();
         // Close the camera now because other activities may need to use it.
         closeCamera();
         resetScreenOn();
         changeHeadUpDisplayState();
 
         if (mFirstTimeInitialized) {
             mOrientationListener.disable();
             if (!mIsImageCaptureIntent) {
                 mThumbController.storeData(
                         ImageManager.getLastImageThumbPath());
             }
             hidePostCaptureAlert();
         }
 
         if (mDidRegister) {
             unregisterReceiver(mReceiver);
             mDidRegister = false;
         }
         stopReceivingLocationUpdates();
 
         if (mFocusToneGenerator != null) {
             mFocusToneGenerator.release();
             mFocusToneGenerator = null;
         }
 
         if (mStorageHint != null) {
             mStorageHint.cancel();
             mStorageHint = null;
         }
 
         // If we are in an image capture intent and has taken
         // a picture, we just clear it in onPause.
         mImageCapture.clearLastData();
         mImageCapture = null;
 
         // Remove the messages in the event queue.
         mHandler.removeMessages(RESTART_PREVIEW);
         mHandler.removeMessages(FIRST_TIME_INIT);
 
         super.onPause();
     }
 
     @Override
     protected void onActivityResult(
             int requestCode, int resultCode, Intent data) {
         switch (requestCode) {
             case CROP_MSG: {
                 Intent intent = new Intent();
                 if (data != null) {
                     Bundle extras = data.getExtras();
                     if (extras != null) {
                         intent.putExtras(extras);
                     }
                 }
                 setResult(resultCode, intent);
                 finish();
 
                 File path = getFileStreamPath(sTempCropFilename);
                 path.delete();
 
                 break;
             }
         }
     }
 
     private boolean canTakePicture() {
         return isCameraIdle() && mPreviewing && (mPicturesRemaining > 0);
     }
 
     private void autoFocus() {
         // Initiate autofocus only when preview is started and snapshot is not
         // in progress.
         if (canTakePicture()) {
             mHeadUpDisplay.setEnabled(false);
             Log.v(TAG, "Start autofocus.");
             mFocusStartTime = System.currentTimeMillis();
             mFocusState = FOCUSING;
             updateFocusIndicator();
             mCameraDevice.autoFocus(mAutoFocusCallback);
         }
     }
 
     private void cancelAutoFocus() {
         // User releases half-pressed focus key.
         if (mStatus != SNAPSHOT_IN_PROGRESS && (mFocusState == FOCUSING
                 || mFocusState == FOCUS_SUCCESS || mFocusState == FOCUS_FAIL)) {
             Log.v(TAG, "Cancel autofocus.");
             mHeadUpDisplay.setEnabled(true);
             mCameraDevice.cancelAutoFocus();
         }
         if (mFocusState != FOCUSING_SNAP_ON_FINISH) {
             clearFocusState();
         }
     }
 
     private void clearFocusState() {
         mFocusState = FOCUS_NOT_STARTED;
         updateFocusIndicator();
     }
 
     private void updateFocusIndicator() {
         if (mFocusRectangle == null) return;
 
         if (mFocusState == FOCUSING || mFocusState == FOCUSING_SNAP_ON_FINISH) {
             mFocusRectangle.showStart();
         } else if (mFocusState == FOCUS_SUCCESS) {
             mFocusRectangle.showSuccess();
         } else if (mFocusState == FOCUS_FAIL) {
             mFocusRectangle.showFail();
         } else {
             mFocusRectangle.clear();
         }
     }
 
     @Override
     public void onBackPressed() {
         if (!isCameraIdle()) {
             // ignore backs while we're taking a picture
             return;
         } else if (mHeadUpDisplay == null || !mHeadUpDisplay.collapse()) {
             super.onBackPressed();
         }
     }
 
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         switch (keyCode) {
             case KeyEvent.KEYCODE_FOCUS:
                 if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                     doFocus(true);
                 }
                 return true;
             case KeyEvent.KEYCODE_CAMERA:
                 if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                     doSnap();
                 }
                 return true;
             case KeyEvent.KEYCODE_DPAD_CENTER:
                 // If we get a dpad center event without any focused view, move
                 // the focus to the shutter button and press it.
                 if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                     // Start auto-focus immediately to reduce shutter lag. After
                     // the shutter button gets the focus, doFocus() will be
                     // called again but it is fine.
                     if (mHeadUpDisplay.collapse()) return true;
                     doFocus(true);
                     if (mShutterButton.isInTouchMode()) {
                         mShutterButton.requestFocusFromTouch();
                     } else {
                         mShutterButton.requestFocus();
                     }
                     mShutterButton.setPressed(true);
                 }
                 return true;
         }
 
         return super.onKeyDown(keyCode, event);
     }
 
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         switch (keyCode) {
             case KeyEvent.KEYCODE_FOCUS:
                 if (mFirstTimeInitialized) {
                     doFocus(false);
                 }
                 return true;
         }
         return super.onKeyUp(keyCode, event);
     }
 
     private void doSnap() {
         if (mHeadUpDisplay.collapse()) return;
 
         Log.v(TAG, "doSnap: mFocusState=" + mFocusState);
         // If the user has half-pressed the shutter and focus is completed, we
         // can take the photo right away. If the focus mode is infinity, we can
         // also take the photo.
         if (mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)
                 || (mFocusState == FOCUS_SUCCESS
                 || mFocusState == FOCUS_FAIL)) {
             mImageCapture.onSnap();
         } else if (mFocusState == FOCUSING) {
             // Half pressing the shutter (i.e. the focus button event) will
             // already have requested AF for us, so just request capture on
             // focus here.
             mFocusState = FOCUSING_SNAP_ON_FINISH;
         } else if (mFocusState == FOCUS_NOT_STARTED) {
             // Focus key down event is dropped for some reasons. Just ignore.
         }
     }
 
     private void doFocus(boolean pressed) {
         // Do the focus if the mode is not infinity.
         if (mHeadUpDisplay.collapse()) return;
         if (!mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)) {
             if (pressed) {  // Focus key down.
                 autoFocus();
             } else {  // Focus key up.
                 cancelAutoFocus();
             }
         }
     }
 
     public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
         // Make sure we have a surface in the holder before proceeding.
         if (holder.getSurface() == null) {
             Log.d(TAG, "holder.getSurface() == null");
             return;
         }
 
         // We need to save the holder for later use, even when the mCameraDevice
         // is null. This could happen if onResume() is invoked after this
         // function.
         mSurfaceHolder = holder;
 
         // The mCameraDevice will be null if it fails to connect to the camera
         // hardware. In this case we will show a dialog and then finish the
         // activity, so it's OK to ignore it.
         if (mCameraDevice == null) return;
 
         // Sometimes surfaceChanged is called after onPause or before onResume.
         // Ignore it.
         if (mPausing || isFinishing()) return;
 
         if (mPreviewing && holder.isCreating()) {
             // Set preview display if the surface is being created and preview
             // was already started. That means preview display was set to null
             // and we need to set it now.
             setPreviewDisplay(holder);
         } else {
             // 1. Restart the preview if the size of surface was changed. The
             // framework may not support changing preview display on the fly.
             // 2. Start the preview now if surface was destroyed and preview
             // stopped.
             restartPreview();
         }
 
         // If first time initialization is not finished, send a message to do
         // it later. We want to finish surfaceChanged as soon as possible to let
         // user see preview first.
         if (!mFirstTimeInitialized) {
             mHandler.sendEmptyMessage(FIRST_TIME_INIT);
         } else {
             initializeSecondTime();
         }
     }
 
     public void surfaceCreated(SurfaceHolder holder) {
     }
 
     public void surfaceDestroyed(SurfaceHolder holder) {
         stopPreview();
         mSurfaceHolder = null;
     }
 
     private void closeCamera() {
         if (mCameraDevice != null) {
             CameraHolder.instance().release();
             mCameraDevice.setZoomChangeListener(null);
             mCameraDevice = null;
             mPreviewing = false;
         }
     }
 
     private void ensureCameraDevice() throws CameraHardwareException {
         if (mCameraDevice == null) {
             mCameraDevice = CameraHolder.instance().open(mCameraId);
            Util.setCameraDisplayOrientation(this, mCameraId, mCameraDevice);
             mInitialParams = mCameraDevice.getParameters();
         }
     }
 
     private void updateLastImage() {
         IImageList list = ImageManager.makeImageList(
             mContentResolver,
             dataLocation(),
             ImageManager.INCLUDE_IMAGES,
             ImageManager.SORT_ASCENDING,
             ImageManager.CAMERA_IMAGE_BUCKET_ID);
         int count = list.getCount();
         if (count > 0) {
             IImage image = list.getImageAt(count - 1);
             Uri uri = image.fullSizeImageUri();
             mThumbController.setData(uri, image.miniThumbBitmap());
         } else {
             mThumbController.setData(null, null);
         }
         list.close();
     }
 
     private void showCameraErrorAndFinish() {
         Resources ress = getResources();
         Util.showFatalErrorAndFinish(Camera.this,
                 ress.getString(R.string.camera_error_title),
                 ress.getString(R.string.cannot_connect_camera));
     }
 
     private boolean restartPreview() {
         try {
             startPreview();
         } catch (CameraHardwareException e) {
             showCameraErrorAndFinish();
             return false;
         }
         return true;
     }
 
     private void setPreviewDisplay(SurfaceHolder holder) {
         try {
             mCameraDevice.setPreviewDisplay(holder);
         } catch (Throwable ex) {
             closeCamera();
             throw new RuntimeException("setPreviewDisplay failed", ex);
         }
     }
 
     private void startPreview() throws CameraHardwareException {
         if (mPausing || isFinishing()) return;
 
         ensureCameraDevice();
 
         // If we're previewing already, stop the preview first (this will blank
         // the screen).
         if (mPreviewing) stopPreview();
 
         setPreviewDisplay(mSurfaceHolder);
         setCameraParameters(UPDATE_PARAM_ALL);
 
        final long wallTimeStart = SystemClock.elapsedRealtime();
        final long threadTimeStart = Debug.threadCpuTimeNanos();

         mCameraDevice.setErrorCallback(mErrorCallback);
 
         try {
             Log.v(TAG, "startPreview");
             mCameraDevice.startPreview();
         } catch (Throwable ex) {
             closeCamera();
             throw new RuntimeException("startPreview failed", ex);
         }
         mPreviewing = true;
         mZoomState = ZOOM_STOPPED;
         mStatus = IDLE;
     }
 
     private void stopPreview() {
         if (mCameraDevice != null && mPreviewing) {
             Log.v(TAG, "stopPreview");
             mCameraDevice.stopPreview();
         }
         mPreviewing = false;
         // If auto focus was in progress, it would have been canceled.
         clearFocusState();
     }
 
     private static boolean isSupported(String value, List<String> supported) {
         return supported == null ? false : supported.indexOf(value) >= 0;
     }
 
     private void updateCameraParametersInitialize() {
         // Reset preview frame rate to the maximum because it may be lowered by
         // video camera application.
         List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
         if (frameRates != null) {
             Integer max = Collections.max(frameRates);
             mParameters.setPreviewFrameRate(max);
         }
 
     }
 
     private void updateCameraParametersZoom() {
         // Set zoom.
         if (mParameters.isZoomSupported()) {
             mParameters.setZoom(mZoomValue);
         }
     }
 
     private void updateCameraParametersPreference() {
         // Set picture size.
         String pictureSize = mPreferences.getString(
                 CameraSettings.KEY_PICTURE_SIZE, null);
         if (pictureSize == null) {
             CameraSettings.initialCameraPictureSize(this, mParameters);
         } else {
             List<Size> supported = mParameters.getSupportedPictureSizes();
             CameraSettings.setCameraPictureSize(
                     pictureSize, supported, mParameters);
         }
 
         // Set the preview frame aspect ratio according to the picture size.
         Size size = mParameters.getPictureSize();
         PreviewFrameLayout frameLayout =
                 (PreviewFrameLayout) findViewById(R.id.frame_layout);
         frameLayout.setAspectRatio((double) size.width / size.height);
 
         // Set a preview size that is closest to the viewfinder height and has
         // the right aspect ratio.
         List<Size> sizes = mParameters.getSupportedPreviewSizes();
         Size optimalSize = Util.getOptimalPreviewSize(this,
                 sizes, (double) size.width / size.height);
         if (optimalSize != null) {
             Size original = mParameters.getPreviewSize();
             if (!original.equals(optimalSize)) {
                 mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
 
                 // Zoom related settings will be changed for different preview
                 // sizes, so set and read the parameters to get lastest values
                 mCameraDevice.setParameters(mParameters);
                 mParameters = mCameraDevice.getParameters();
             }
         }
 
         // Since change scene mode may change supported values,
         // Set scene mode first,
         mSceneMode = mPreferences.getString(
                 CameraSettings.KEY_SCENE_MODE,
                 getString(R.string.pref_camera_scenemode_default));
         if (isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
             if (!mParameters.getSceneMode().equals(mSceneMode)) {
                 mParameters.setSceneMode(mSceneMode);
                 mCameraDevice.setParameters(mParameters);
 
                 // Setting scene mode will change the settings of flash mode,
                 // white balance, and focus mode. Here we read back the
                 // parameters, so we can know those settings.
                 mParameters = mCameraDevice.getParameters();
             }
         } else {
             mSceneMode = mParameters.getSceneMode();
             if (mSceneMode == null) {
                 mSceneMode = Parameters.SCENE_MODE_AUTO;
             }
         }
 
         // Set JPEG quality.
         String jpegQuality = mPreferences.getString(
                 CameraSettings.KEY_JPEG_QUALITY,
                 getString(R.string.pref_camera_jpegquality_default));
         mParameters.setJpegQuality(JpegEncodingQualityMappings.getQualityNumber(jpegQuality));
 
         // For the following settings, we need to check if the settings are
         // still supported by latest driver, if not, ignore the settings.
 
         // Set color effect parameter.
         String colorEffect = mPreferences.getString(
                 CameraSettings.KEY_COLOR_EFFECT,
                 getString(R.string.pref_camera_coloreffect_default));
         if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
             mParameters.setColorEffect(colorEffect);
         }
 
         // Set exposure compensation
         String exposure = mPreferences.getString(
                 CameraSettings.KEY_EXPOSURE,
                 getString(R.string.pref_exposure_default));
         try {
             int value = Integer.parseInt(exposure);
             int max = mParameters.getMaxExposureCompensation();
             int min = mParameters.getMinExposureCompensation();
             if (value >= min && value <= max) {
                 mParameters.setExposureCompensation(value);
             } else {
                 Log.w(TAG, "invalid exposure range: " + exposure);
             }
         } catch (NumberFormatException e) {
             Log.w(TAG, "invalid exposure: " + exposure);
         }
 
         if (mGLRootView != null) updateSceneModeInHud();
 
         if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
             // Set flash mode.
             String flashMode = mPreferences.getString(
                     CameraSettings.KEY_FLASH_MODE,
                     getString(R.string.pref_camera_flashmode_default));
             List<String> supportedFlash = mParameters.getSupportedFlashModes();
             if (isSupported(flashMode, supportedFlash)) {
                 mParameters.setFlashMode(flashMode);
             } else {
                 flashMode = mParameters.getFlashMode();
                 if (flashMode == null) {
                     flashMode = getString(
                             R.string.pref_camera_flashmode_no_flash);
                 }
             }
 
             // Set white balance parameter.
             String whiteBalance = mPreferences.getString(
                     CameraSettings.KEY_WHITE_BALANCE,
                     getString(R.string.pref_camera_whitebalance_default));
             if (isSupported(whiteBalance,
                     mParameters.getSupportedWhiteBalance())) {
                 mParameters.setWhiteBalance(whiteBalance);
             } else {
                 whiteBalance = mParameters.getWhiteBalance();
                 if (whiteBalance == null) {
                     whiteBalance = Parameters.WHITE_BALANCE_AUTO;
                 }
             }
 
             // Set focus mode.
             mFocusMode = mPreferences.getString(
                     CameraSettings.KEY_FOCUS_MODE,
                     getString(R.string.pref_camera_focusmode_default));
             if (isSupported(mFocusMode, mParameters.getSupportedFocusModes())) {
                 mParameters.setFocusMode(mFocusMode);
             } else {
                 mFocusMode = mParameters.getFocusMode();
                 if (mFocusMode == null) {
                     mFocusMode = Parameters.FOCUS_MODE_AUTO;
                 }
             }
         } else {
             mFocusMode = mParameters.getFocusMode();
         }
     }
 
     // We separate the parameters into several subsets, so we can update only
     // the subsets actually need updating. The PREFERENCE set needs extra
     // locking because the preference can be changed from GLThread as well.
     private void setCameraParameters(int updateSet) {
         mParameters = mCameraDevice.getParameters();
 
         if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
             updateCameraParametersInitialize();
         }
 
         if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
             updateCameraParametersZoom();
         }
 
         if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
             updateCameraParametersPreference();
         }
 
         mCameraDevice.setParameters(mParameters);
     }
 
     // If the Camera is idle, update the parameters immediately, otherwise
     // accumulate them in mUpdateSet and update later.
     private void setCameraParametersWhenIdle(int additionalUpdateSet) {
         mUpdateSet |= additionalUpdateSet;
         if (mCameraDevice == null) {
             // We will update all the parameters when we open the device, so
             // we don't need to do anything now.
             mUpdateSet = 0;
             return;
         } else if (isCameraIdle()) {
             setCameraParameters(mUpdateSet);
             mUpdateSet = 0;
         } else {
             if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
                 mHandler.sendEmptyMessageDelayed(
                         SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
             }
         }
     }
 
     private void gotoGallery() {
         MenuHelper.gotoCameraImageGallery(this);
     }
 
     private void viewLastImage() {
         if (mThumbController.isUriValid()) {
             Intent intent = new Intent(Util.REVIEW_ACTION, mThumbController.getUri());
             try {
                 startActivity(intent);
             } catch (ActivityNotFoundException ex) {
                 Log.e(TAG, "review image fail", ex);
             }
         } else {
             Log.e(TAG, "Can't view last image.");
         }
     }
 
     private void startReceivingLocationUpdates() {
         if (mLocationManager != null) {
             try {
                 mLocationManager.requestLocationUpdates(
                         LocationManager.NETWORK_PROVIDER,
                         1000,
                         0F,
                         mLocationListeners[1]);
             } catch (java.lang.SecurityException ex) {
                 Log.i(TAG, "fail to request location update, ignore", ex);
             } catch (IllegalArgumentException ex) {
                 Log.d(TAG, "provider does not exist " + ex.getMessage());
             }
             try {
                 mLocationManager.requestLocationUpdates(
                         LocationManager.GPS_PROVIDER,
                         1000,
                         0F,
                         mLocationListeners[0]);
             } catch (java.lang.SecurityException ex) {
                 Log.i(TAG, "fail to request location update, ignore", ex);
             } catch (IllegalArgumentException ex) {
                 Log.d(TAG, "provider does not exist " + ex.getMessage());
             }
         }
     }
 
     private void stopReceivingLocationUpdates() {
         if (mLocationManager != null) {
             for (int i = 0; i < mLocationListeners.length; i++) {
                 try {
                     mLocationManager.removeUpdates(mLocationListeners[i]);
                 } catch (Exception ex) {
                     Log.i(TAG, "fail to remove location listners, ignore", ex);
                 }
             }
         }
     }
 
     private Location getCurrentLocation() {
         // go in best to worst order
         for (int i = 0; i < mLocationListeners.length; i++) {
             Location l = mLocationListeners[i].current();
             if (l != null) return l;
         }
         return null;
     }
 
     private boolean isCameraIdle() {
         return mStatus == IDLE && mFocusState == FOCUS_NOT_STARTED;
     }
 
     private boolean isImageCaptureIntent() {
         String action = getIntent().getAction();
         return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action));
     }
 
     private void setupCaptureParams() {
         Bundle myExtras = getIntent().getExtras();
         if (myExtras != null) {
             mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
             mCropValue = myExtras.getString("crop");
         }
     }
 
     private void showPostCaptureAlert() {
         if (mIsImageCaptureIntent) {
             findViewById(R.id.shutter_button).setVisibility(View.INVISIBLE);
             int[] pickIds = {R.id.btn_retake, R.id.btn_done};
             for (int id : pickIds) {
                 View button = findViewById(id);
                 ((View) button.getParent()).setVisibility(View.VISIBLE);
             }
         }
     }
 
     private void hidePostCaptureAlert() {
         if (mIsImageCaptureIntent) {
             findViewById(R.id.shutter_button).setVisibility(View.VISIBLE);
             int[] pickIds = {R.id.btn_retake, R.id.btn_done};
             for (int id : pickIds) {
                 View button = findViewById(id);
                 ((View) button.getParent()).setVisibility(View.GONE);
             }
         }
     }
 
     private int calculatePicturesRemaining() {
         mPicturesRemaining = MenuHelper.calculatePicturesRemaining();
         return mPicturesRemaining;
     }
 
     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
         super.onPrepareOptionsMenu(menu);
         // Only show the menu when camera is idle.
         for (int i = 0; i < menu.size(); i++) {
             menu.getItem(i).setVisible(isCameraIdle());
         }
 
         return true;
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
 
         if (mIsImageCaptureIntent) {
             // No options menu for attach mode.
             return false;
         } else {
             addBaseMenuItems(menu);
         }
         return true;
     }
 
     private void addBaseMenuItems(Menu menu) {
         MenuHelper.addSwitchModeMenuItem(menu, true, new Runnable() {
             public void run() {
                 switchToVideoMode();
             }
         });
         MenuItem gallery = menu.add(Menu.NONE, Menu.NONE,
                 MenuHelper.POSITION_GOTO_GALLERY,
                 R.string.camera_gallery_photos_text)
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {
             public boolean onMenuItemClick(MenuItem item) {
                 gotoGallery();
                 return true;
             }
         });
         gallery.setIcon(android.R.drawable.ic_menu_gallery);
         mGalleryItems.add(gallery);
 
         if (mNumberOfCameras > 1) {
             menu.add(Menu.NONE, Menu.NONE,
                     MenuHelper.POSITION_SWITCH_CAMERA_ID,
                     R.string.switch_camera_id)
                     .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                 public boolean onMenuItemClick(MenuItem item) {
                     switchCameraId();
                     return true;
                 }
             }).setIcon(android.R.drawable.ic_menu_camera);
         }
     }
 
     private void switchCameraId() {
         mSwitching = true;
 
         mCameraId = (mCameraId + 1) % mNumberOfCameras;
         CameraSettings.writePreferredCameraId(mPreferences, mCameraId);
 
         stopPreview();
         closeCamera();
         changeHeadUpDisplayState();
 
         // Remove the messages in the event queue.
         mHandler.removeMessages(RESTART_PREVIEW);
 
         // Reset variables
         mJpegPictureCallbackTime = 0;
         mZoomValue = 0;
 
         // Reload the preferences.
         mPreferences.setLocalId(this, mCameraId);
         CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
 
 
         // Restart the preview.
         resetExposureCompensation();
         if (!restartPreview()) return;
 
         initializeZoom();
 
         // Reload the UI.
         if (mFirstTimeInitialized) {
             initializeHeadUpDisplay();
         }
 
         mSwitching = false;
         changeHeadUpDisplayState();
     }
 
     private boolean switchToVideoMode() {
         if (isFinishing() || !isCameraIdle()) return false;
         MenuHelper.gotoVideoMode(this);
         mHandler.removeMessages(FIRST_TIME_INIT);
         finish();
         return true;
     }
 
     public boolean onSwitchChanged(Switcher source, boolean onOff) {
         if (onOff == SWITCH_VIDEO) {
             return switchToVideoMode();
         } else {
             return true;
         }
     }
 
     private void onSharedPreferenceChanged() {
         // ignore the events after "onPause()"
         if (mPausing) return;
 
         boolean recordLocation;
 
         recordLocation = RecordLocationPreference.get(
                 mPreferences, getContentResolver());
         mQuickCapture = getQuickCaptureSettings();
 
         if (mRecordLocation != recordLocation) {
             mRecordLocation = recordLocation;
             if (mRecordLocation) {
                 startReceivingLocationUpdates();
             } else {
                 stopReceivingLocationUpdates();
             }
         }
 
         setCameraParametersWhenIdle(UPDATE_PARAM_PREFERENCE);
     }
 
     private boolean getQuickCaptureSettings() {
         String value = mPreferences.getString(
                 CameraSettings.KEY_QUICK_CAPTURE,
                 getString(R.string.pref_camera_quickcapture_default));
         return CameraSettings.QUICK_CAPTURE_ON.equals(value);
     }
 
     @Override
     public void onUserInteraction() {
         super.onUserInteraction();
         keepScreenOnAwhile();
     }
 
     private void resetScreenOn() {
         mHandler.removeMessages(CLEAR_SCREEN_DELAY);
         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     }
 
     private void keepScreenOnAwhile() {
         mHandler.removeMessages(CLEAR_SCREEN_DELAY);
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
         mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
     }
 
     private class MyHeadUpDisplayListener implements HeadUpDisplay.Listener {
 
         public void onSharedPreferencesChanged() {
             Camera.this.onSharedPreferenceChanged();
         }
 
         public void onRestorePreferencesClicked() {
             Camera.this.onRestorePreferencesClicked();
         }
 
         public void onPopupWindowVisibilityChanged(int visibility) {
         }
     }
 
     protected void onRestorePreferencesClicked() {
         if (mPausing) return;
         Runnable runnable = new Runnable() {
             public void run() {
                 mHeadUpDisplay.restorePreferences(mParameters);
             }
         };
         MenuHelper.confirmAction(this,
                 getString(R.string.confirm_restore_title),
                 getString(R.string.confirm_restore_message),
                 runnable);
     }
 }
 
 class FocusRectangle extends View {
 
     @SuppressWarnings("unused")
     private static final String TAG = "FocusRectangle";
 
     public FocusRectangle(Context context, AttributeSet attrs) {
         super(context, attrs);
     }
 
     private void setDrawable(int resid) {
         setBackgroundDrawable(getResources().getDrawable(resid));
     }
 
     public void showStart() {
         setDrawable(R.drawable.focus_focusing);
     }
 
     public void showSuccess() {
         setDrawable(R.drawable.focus_focused);
     }
 
     public void showFail() {
         setDrawable(R.drawable.focus_focus_failed);
     }
 
     public void clear() {
         setBackgroundDrawable(null);
     }
 }
 
 /*
  * Provide a mapping for Jpeg encoding quality levels
  * from String representation to numeric representation.
  */
 class JpegEncodingQualityMappings {
     private static final String TAG = "JpegEncodingQualityMappings";
     private static final int DEFAULT_QUALITY = 85;
     private static HashMap<String, Integer> mHashMap =
             new HashMap<String, Integer>();
 
     static {
         mHashMap.put("normal",    CameraProfile.QUALITY_LOW);
         mHashMap.put("fine",      CameraProfile.QUALITY_MEDIUM);
         mHashMap.put("superfine", CameraProfile.QUALITY_HIGH);
     }
 
     // Retrieve and return the Jpeg encoding quality number
     // for the given quality level.
     public static int getQualityNumber(String jpegQuality) {
         Integer quality = mHashMap.get(jpegQuality);
         if (quality == null) {
             Log.w(TAG, "Unknown Jpeg quality: " + jpegQuality);
             return DEFAULT_QUALITY;
         }
         return CameraProfile.getJpegEncodingQualityParameter(quality.intValue());
     }
 }
