 /*
  * Copyright (c) 2009-2012, Code Aurora Forum. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *        * Redistributions of source code must retain the above copyright
  *            notice, this list of conditions and the following disclaimer.
  *        * Redistributions in binary form must reproduce the above copyright
  *            notice, this list of conditions and the following disclaimer in the
  *            documentation and/or other materials provided with the distribution.
  *        * Neither the name of Code Aurora nor
  *            the names of its contributors may be used to endorse or promote
  *            products derived from this software without specific prior written
  *            permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
  * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
  * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
  * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
  * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package com.quicinc.fmradio;
 
 import java.io.File;
 import java.io.IOException;
 import java.lang.ref.WeakReference;
 
 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.BroadcastReceiver;
 import android.media.AudioManager;
 import android.media.AudioManager.OnAudioFocusChangeListener;
 import android.media.AudioSystem;
 import android.media.MediaRecorder;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.Message;
 import android.os.PowerManager;
 import android.os.PowerManager.WakeLock;
 import android.os.RemoteException;
 import android.telephony.PhoneStateListener;
 import android.telephony.TelephonyManager;
 import android.util.Log;
 import android.widget.RemoteViews;
 import android.widget.Toast;
 import android.view.KeyEvent;
 import android.os.SystemProperties;
 
 import android.hardware.fmradio.FmReceiver;
 import android.hardware.fmradio.FmRxEvCallbacksAdaptor;
 import android.hardware.fmradio.FmRxRdsData;
 import android.hardware.fmradio.FmConfig;
 import android.net.Uri;
 import android.content.res.Resources;
 import java.util.Date;
 import java.text.SimpleDateFormat;
 import android.provider.MediaStore;
 import android.content.ContentResolver;
 import android.content.ContentValues;
 import android.database.Cursor;
 import com.quicinc.utils.A2dpDeviceStatus;
 import android.media.AudioManager;
 import android.content.ComponentName;
 
 /**
  * Provides "background" FM Radio (that uses the hardware) capabilities,
  * allowing the user to switch between activities without stopping playback.
  */
 public class FMRadioService extends Service
 {
 
    public static final int RADIO_AUDIO_DEVICE_WIRED_HEADSET = 0;
    public static final int RADIO_AUDIO_DEVICE_SPEAKER = 1;
 
    private static final int FMRADIOSERVICE_STATUS = 101;
    private static final String FMRADIO_DEVICE_FD_STRING = "/dev/radio0";
    private static final String LOGTAG = "FMService";//FMRadio.LOGTAG;
 
    private FmReceiver mReceiver;
    private BroadcastReceiver mHeadsetReceiver = null;
    private BroadcastReceiver mSdcardUnmountReceiver = null;
    private BroadcastReceiver mMusicCommandListener = null;
    private boolean mOverA2DP = false;
    private BroadcastReceiver mFmMediaButtonListener;
    private IFMRadioServiceCallbacks mCallbacks;
    private static FmSharedPreferences mPrefs;
    private boolean mHeadsetPlugged = false;
    private boolean mInternalAntennaAvailable = false;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private static boolean mMuted = false;
    private static boolean mResumeAfterCall = false;
    private static String mAudioDevice="headset";
    MediaRecorder mRecorder = null;
    MediaRecorder mA2dp = null;
    private boolean mFMOn = false;
    private boolean mFmRecordingOn = false;
    private boolean mSpeakerPhoneOn = false;
    private int mCallStatus = 0;
    private BroadcastReceiver mScreenOnOffReceiver = null;
    final Handler mHandler = new Handler();
    private boolean misAnalogModeSupported = false;
    private boolean misAnalogPathEnabled = false;
    private boolean mA2dpDisconnected = false;
    //PhoneStateListener instances corresponding to each
 
    private FmRxRdsData mFMRxRDSData=null;
    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;
    private File mA2DPSampleFile = null;
    //Track FM playback for reenter App usecases
    private boolean mPlaybackInProgress = false;
    private boolean mStoppedOnFocusLoss = false;
    private File mSampleFile = null;
    long mSampleStart = 0;
    // Messages handled in FM Service
    private static final int FM_STOP =1;
    private static final int RESET_NOTCH_FILTER =2;
    private static final int STOPSERVICE_ONSLEEP = 3;
    private static final int STOPRECORD_ONTIMEOUT = 4;
    private static final int FOCUSCHANGE = 5;
    //Track notch filter settings
    private boolean mNotchFilterSet = false;
    public static final int STOP_SERVICE = 0;
    public static final int STOP_RECORD = 1;
    // A2dp Device Status will be queried through this class
    A2dpDeviceStatus mA2dpDeviceState = null;
    //on shutdown not to send start Intent to AudioManager
    private boolean mAppShutdown = false;
    private boolean mSingleRecordingInstanceSupported = false;
    private AudioManager mAudioManager;
 
    private static final String IOBUSY_UNVOTE = "com.android.server.CpuGovernorService.action.IOBUSY_UNVOTE";
 
    public FMRadioService() {
    }
 
    @Override
    public void onCreate() {
       super.onCreate();
 
       mPrefs = new FmSharedPreferences(this);
       mCallbacks = null;
       TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
       tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE |
                                        PhoneStateListener.LISTEN_DATA_ACTIVITY);
       PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
       mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
       mWakeLock.setReferenceCounted(false);
       misAnalogModeSupported  = SystemProperties.getBoolean("ro.fm.analogpath.supported",false);
       /* Register for Screen On/off broadcast notifications */
       mA2dpDeviceState = new A2dpDeviceStatus(getApplicationContext());
       registerScreenOnOffListener();
       registerHeadsetListener();
       registerExternalStorageListener();
       // registering media button receiver seperately as we need to set
       // different priority for receiving media events
       registerFmMediaButtonReceiver();
       if ( false == SystemProperties.getBoolean("ro.fm.mulinst.recording.support",true)) {
            mSingleRecordingInstanceSupported = true;
       }
 
       // Register for pause commands from other apps to stop FM
       registerMusicServiceCommandReceiver();
 
       // If the service was idle, but got killed before it stopped itself, the
       // system will relaunch it. Make sure it gets stopped again in that case.
       Message msg = mDelayedStopHandler.obtainMessage();
       msg.what = FM_STOP;
       mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }
 
    @Override
    public void onDestroy() {
       Log.d(LOGTAG, "onDestroy");
       if (isFmOn())
       {
          Log.e(LOGTAG, "Service being destroyed while still playing.");
       }
 
       // make sure there aren't any other messages coming
       mDelayedStopHandler.removeCallbacksAndMessages(null);
       //release the audio focus listener
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       if (isMuted()) {
           mMuted = false;
           audioManager.setStreamMute(AudioManager.STREAM_FM,false);
       }
       audioManager.abandonAudioFocus(mAudioFocusListener);
       /* Remove the Screen On/off listener */
       if (mScreenOnOffReceiver != null) {
           unregisterReceiver(mScreenOnOffReceiver);
           mScreenOnOffReceiver = null;
       }
       /* Unregister the headset Broadcase receiver */
       if (mHeadsetReceiver != null) {
           unregisterReceiver(mHeadsetReceiver);
           mHeadsetReceiver = null;
       }
       if( mSdcardUnmountReceiver != null ) {
           unregisterReceiver(mSdcardUnmountReceiver);
           mSdcardUnmountReceiver = null;
       }
       if( mMusicCommandListener != null ) {
           unregisterReceiver(mMusicCommandListener);
           mMusicCommandListener = null;
       }
       if( mFmMediaButtonListener != null ) {
           unregisterReceiver(mFmMediaButtonListener);
           mFmMediaButtonListener = null;
       }
 
       /* Since the service is closing, disable the receiver */
       fmOff();
 
       TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
       tmgr.listen(mPhoneStateListener, 0);
 
       Log.d(LOGTAG, "onDestroy: unbindFromService completed");
 
       //unregisterReceiver(mIntentReceiver);
       mWakeLock.release();
       super.onDestroy();
    }
 
 /**
       * Registers an intent to listen for ACTION_MEDIA_UNMOUNTED notifications.
       * The intent will call closeExternalStorageFiles() if the external media
       * is going to be ejected, so applications can clean up.
       */
      public void registerExternalStorageListener() {
          if (mSdcardUnmountReceiver == null) {
              mSdcardUnmountReceiver = new BroadcastReceiver() {
                  @Override
                  public void onReceive(Context context, Intent intent) {
                      String action = intent.getAction();
                      if ((action.equals(Intent.ACTION_MEDIA_UNMOUNTED))
                            || (action.equals(Intent.ACTION_MEDIA_EJECT))) {
                          Log.d(LOGTAG, "ACTION_MEDIA_UNMOUNTED Intent received");
                          if(isFmOn() && (true == mOverA2DP) &&
                             (false == mStoppedOnFocusLoss) &&
                             (!isSpeakerEnabled())) {
                             stopFM();
                             startFM();
                          }
                          if (mFmRecordingOn == true) {
                              try {
                                   stopRecording();
                              } catch (Exception e) {
                                   e.printStackTrace();
                              }
                          }
                      }else if((action.equals(Intent.ACTION_MEDIA_MOUNTED))){
                          boolean  bA2dpConnected =
                                         mA2dpDeviceState.isConnected(intent);
                          if(isFmOn() && bA2dpConnected &&
                             (false == mStoppedOnFocusLoss) &&
                             (!isSpeakerEnabled())) {
                              stopFM();
                              startFM();
                          }
                      }
                  }
              };
              IntentFilter iFilter = new IntentFilter();
              iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
              iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
              iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
              iFilter.addDataScheme("file");
              registerReceiver(mSdcardUnmountReceiver, iFilter);
          }
      }
 
 
      /**
      * Registers an intent to listen for ACTION_HEADSET_PLUG
      * notifications. This intent is called to know if the headset
      * was plugged in/out
      */
     public void registerHeadsetListener() {
         if (mHeadsetReceiver == null) {
             mHeadsetReceiver = new BroadcastReceiver() {
                 @Override
                 public void onReceive(Context context, Intent intent) {
                     String action = intent.getAction();
                     if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                        Log.d(LOGTAG, "ACTION_HEADSET_PLUG Intent received");
                        // Listen for ACTION_HEADSET_PLUG broadcasts.
                        Log.d(LOGTAG, "mReceiver: ACTION_HEADSET_PLUG");
                        Log.d(LOGTAG, "==> intent: " + intent);
                        Log.d(LOGTAG, "    state: " + intent.getIntExtra("state", 0));
                        Log.d(LOGTAG, "    name: " + intent.getStringExtra("name"));
                        mHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                        // if headset is plugged out it is required to disable
                        // in minimal duration to avoid race conditions with
                        // audio policy manager switch audio to speaker.
                        if ((mHeadsetPlugged == false) && (mReceiver != null) &&
                            (mInternalAntennaAvailable == false) &&
                            (mOverA2DP == false)) {
                           mReceiver.disable();
                           mReceiver = null;
                           if(!isFmRecordingOn())
                              stop();
                        }
                        mHandler.post(mHeadsetPluginHandler);
                     } else if(mA2dpDeviceState.isA2dpStateChange(action) ) {
                         boolean  bA2dpConnected =
                         mA2dpDeviceState.isConnected(intent);
                         if (!bA2dpConnected) {
                             Log.d(LOGTAG, "A2DP device is dis-connected!");
                             mA2dpDisconnected = true;
                         }
                         if (isAnalogModeEnabled()) {
                             Log.d(LOGTAG, "FM Audio Path is Analog Mode: FM Over BT not allowed");
                             return ;
                         }
                        //when playback is overA2Dp and A2dp disconnected
                        //when playback is not overA2DP and A2DP Connected
                        // In above two cases we need to Stop and Start FM which
                        // will take care of audio routing
                        if( (isFmOn()) &&
                            (true == ((bA2dpConnected)^(mOverA2DP))) &&
                            (false == mStoppedOnFocusLoss) &&
                            (!isSpeakerEnabled())) {
                            stopFM();
                            startFM();
                        }
                     } else if (action.equals("HDMI_CONNECTED")) {
                         //FM should be off when HDMI is connected.
                         fmOff();
                         try
                         {
                             /* Notify the UI/Activity, only if the service is "bound"
                                by an activity and if Callbacks are registered
                              */
                             if((mServiceInUse) && (mCallbacks != null) )
                             {
                                 mCallbacks.onDisabled();
                             }
                         } catch (RemoteException e)
                         {
                             e.printStackTrace();
                         }
                     } else if( action.equals(Intent.ACTION_SHUTDOWN)) {
                         mAppShutdown = true;
                     }
 
                 }
             };
             IntentFilter iFilter = new IntentFilter();
             iFilter.addAction(Intent.ACTION_HEADSET_PLUG);
             iFilter.addAction(mA2dpDeviceState.getActionSinkStateChangedString());
             iFilter.addAction("HDMI_CONNECTED");
             iFilter.addAction(Intent.ACTION_SHUTDOWN);
             iFilter.addCategory(Intent.CATEGORY_DEFAULT);
             registerReceiver(mHeadsetReceiver, iFilter);
         }
     }
 
     public void registerFmMediaButtonReceiver() {
         if (mFmMediaButtonListener == null) {
             mFmMediaButtonListener = new BroadcastReceiver() {
                  public void onReceive(Context context, Intent intent) {
                      Log.d(LOGTAG, "FMMediaButtonIntentReceiver.FM_MEDIA_BUTTON");
                      Log.d(LOGTAG, "KeyEvent = " +intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
                      String action = intent.getAction();
                      if (action.equals(FMMediaButtonIntentReceiver.FM_MEDIA_BUTTON)) {
                          KeyEvent event = (KeyEvent)
                                      intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                          int keycode = event.getKeyCode();
                          switch (keycode) {
                              case KeyEvent.KEYCODE_HEADSETHOOK :
                                  if (isFmOn()){
                                      //FM should be off when Headset hook pressed.
                                      fmOff();
                                      if (isOrderedBroadcast()) {
                                         abortBroadcast();
                                      }
                                      try {
                                          /* Notify the UI/Activity, only if the service is "bound"
                                             by an activity and if Callbacks are registered
                                           */
                                          if ((mServiceInUse) && (mCallbacks != null) ) {
                                             mCallbacks.onDisabled();
                                          }
                                      } catch (RemoteException e) {
                                          e.printStackTrace();
                                      }
                                  } else if( mServiceInUse ) {
                                      fmOn();
                                      if (isOrderedBroadcast()) {
                                         abortBroadcast();
                                      }
                                      try {
                                          /* Notify the UI/Activity, only if the service is "bound"
                                             by an activity and if Callbacks are registered
                                           */
                                          if (mCallbacks != null ) {
                                              mCallbacks.onEnabled();
                                          }
                                      } catch (RemoteException e) {
                                          e.printStackTrace();
                                      }
                                  }
                                  break;
                              case KeyEvent.KEYCODE_MEDIA_PAUSE :
                                  if (isFmOn()){
                                      //FM should be off when Headset hook pressed.
                                      fmOff();
                                      if (isOrderedBroadcast()) {
                                         abortBroadcast();
                                      }
                                      try {
                                          /* Notify the UI/Activity, only if the service is "bound"
                                             by an activity and if Callbacks are registered
                                           */
                                          if ((mServiceInUse) && (mCallbacks != null) ) {
                                             mCallbacks.onDisabled();
                                          }
                                      } catch (RemoteException e) {
                                          e.printStackTrace();
                                      }
                                  }
                                  break;
                              case KeyEvent.KEYCODE_MEDIA_PLAY:
                                  if (mServiceInUse ) {
                                      fmOn();
                                      if (isOrderedBroadcast()) {
                                          abortBroadcast();
                                      }
                                      try {
                                           /* Notify the UI/Activity, only if the service is "bound"
                                              by an activity and if Callbacks are registered
                                            */
                                           if (mCallbacks != null ) {
                                                mCallbacks.onEnabled();
                                           }
                                      } catch (RemoteException e) {
                                           e.printStackTrace();
                                      }
                                  }
                                  break;
                          } // end of switch
                      } // end of FMMediaButtonIntentReceiver.FM_MEDIA_BUTTON
                  } // end of onReceive
             };
             IntentFilter iFilter = new IntentFilter();
             iFilter.addAction(FMMediaButtonIntentReceiver.FM_MEDIA_BUTTON);
             registerReceiver(mFmMediaButtonListener, iFilter);
          }
      }
 
     public void registerMusicServiceCommandReceiver() {
         if (mMusicCommandListener == null) {
             mMusicCommandListener = new BroadcastReceiver() {
                 @Override
                 public void onReceive(Context context, Intent intent) {
                     String action = intent.getAction();
 
                     if (action.equals("com.android.music.musicservicecommand")) {
                         String cmd = intent.getStringExtra("command");
                         Log.d(LOGTAG, "Music Service command : "+cmd+ " received");
                         if (cmd != null && cmd.equals("pause")) {
                             if (mA2dpDisconnected) {
                                 Log.d(LOGTAG, "not to pause,this is a2dp disconnected's pause");
                                 mA2dpDisconnected = false;
                                 return;
                             }
                             if(isFmOn()){
                                 if((mReceiver != null) && mReceiver.disable()) {
                                    mReceiver = null;
                                 }
                                 fmOff();
                                 if (isOrderedBroadcast()) {
                                     abortBroadcast();
                                 }
                                 try {
                                     /* Notify the UI/Activity, only if the service is "bound"
                                        by an activity and if Callbacks are registered
                                     */
                                     if((mServiceInUse) && (mCallbacks != null) ){
                                         mCallbacks.onDisabled();
                                     }
                                 } catch (RemoteException e) {
                                     e.printStackTrace();
                                 }
                             }
                         }
                     }
                 }
             };
             IntentFilter commandFilter = new IntentFilter();
             commandFilter.addAction("com.android.music.musicservicecommand");
             registerReceiver(mMusicCommandListener, commandFilter);
         }
     }
     final Runnable    mHeadsetPluginHandler = new Runnable() {
         public void run() {
             /* Update the UI based on the state change of the headset/antenna*/
             if(!isAntennaAvailable())
             {
                 /* Disable FM and let the UI know */
                 fmOff();
                 try
                 {
                     /* Notify the UI/Activity, only if the service is "bound"
                   by an activity and if Callbacks are registered
                      */
                     if((mServiceInUse) && (mCallbacks != null) )
                     {
                         mCallbacks.onDisabled();
                     }
                 } catch (RemoteException e)
                 {
                     e.printStackTrace();
                 }
             }
             else
             {
                 /* headset is plugged back in,
                So turn on FM if:
                - FM is not already ON.
                - If the FM UI/Activity is in the foreground
                  (the service is "bound" by an activity
                   and if Callbacks are registered)
                  */
                 if ((!isFmOn()) && (mServiceInUse)
                         && (mCallbacks != null))
                 {
                     if( true != fmOn() ) {
                         return;
                     }
                     try
                     {
                         mCallbacks.onEnabled();
                     } catch (RemoteException e)
                     {
                         e.printStackTrace();
                     }
                 }
             }
         }
     };
 
 
    @Override
    public IBinder onBind(Intent intent) {
       mDelayedStopHandler.removeCallbacksAndMessages(null);
       mServiceInUse = true;
       /* Application/UI is attached, so get out of lower power mode */
       setLowPowerMode(false);
       Log.d(LOGTAG, "onBind");
       return mBinder;
    }
 
    @Override
    public void onRebind(Intent intent) {
       mDelayedStopHandler.removeCallbacksAndMessages(null);
       mServiceInUse = true;
       /* Application/UI is attached, so get out of lower power mode */
       setLowPowerMode(false);
       if(false == mPlaybackInProgress)
          startFM();
       Log.d(LOGTAG, "onRebind");
    }
 
    @Override
    public void onStart(Intent intent, int startId) {
       Log.d(LOGTAG, "onStart");
       mServiceStartId = startId;
       // adding code for audio focus gain.
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_FM,
               AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
       // make sure the service will shut down on its own if it was
       // just started but not bound to and nothing is playing
       mDelayedStopHandler.removeCallbacksAndMessages(null);
       Message msg = mDelayedStopHandler.obtainMessage();
       msg.what = FM_STOP;
       mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }
 
    @Override
    public boolean onUnbind(Intent intent) {
       mServiceInUse = false;
       Log.d(LOGTAG, "onUnbind");
 
       /* Application/UI is not attached, so go into lower power mode */
       unregisterCallbacks();
       setLowPowerMode(true);
       if (isFmOn())
       {
          // something is currently playing, or will be playing once
          // an in-progress call ends, so don't stop the service now.
          return true;
       }
       gotoIdleState();
       return true;
    }
 
    private void startFM(){
        Log.d(LOGTAG, "In startFM");
        if(true == mAppShutdown) { // not to send intent to AudioManager in Shutdown
            return;
        }
        if (isCallActive()) { // when Call is active never let audio playback
            mResumeAfterCall = true;
            return;
        }
        mResumeAfterCall = false;
        if ( true == mPlaybackInProgress ) // no need to resend event
            return;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_FM,
               AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        Log.d(LOGTAG,"FM registering for registerMediaButtonEventReceiver");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ComponentName fmRadio = new ComponentName(this.getPackageName(),
                                   FMMediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(fmRadio);
        mStoppedOnFocusLoss = false;
 
        if ((true == mA2dpDeviceState.isDeviceAvailable()) &&
            (!isSpeakerEnabled()) && !isAnalogModeEnabled() &&
            isExternalStorageAvailable() && (true == startA2dpPlayback())) {
             mOverA2DP=true;
        } else {
            Log.d(LOGTAG, "FMRadio: sending the intent");
            //reason for resending the Speaker option is we are sending
            //ACTION_FM=1 to AudioManager, the previous state of Speaker we set
            //need not be retained by the Audio Manager.
            if (isSpeakerEnabled()) {
                    mSpeakerPhoneOn = true;
                    AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_SPEAKER);
            }
            Intent intent = new Intent(Intent.ACTION_FM);
            intent.putExtra("state", 1);
            getApplicationContext().sendBroadcast(intent);
        }
        mPlaybackInProgress = true;
    }
 
    private void stopFM(){
        Log.d(LOGTAG, "In stopFM");
        if (mOverA2DP==true){
            mOverA2DP=false;
            stopA2dpPlayback();
        }else{
            Log.d(LOGTAG, "FMRadio: sending the intent");
            Intent intent = new Intent(Intent.ACTION_FM);
            intent.putExtra("state", 0);
            getApplicationContext().sendBroadcast(intent);
        }
        mPlaybackInProgress = false;
    }
 
    private void resetFM(){
        Log.d(LOGTAG, "resetFM");
        if (mOverA2DP==true){
            mOverA2DP=false;
            resetA2dpPlayback();
        }else{
            Log.d(LOGTAG, "FMRadio: sending the intent");
            Intent intent = new Intent(Intent.ACTION_FM);
            intent.putExtra("state", 0);
            getApplicationContext().sendBroadcast(intent);
        }
        mPlaybackInProgress = false;
    }
 
    public boolean startRecording() {
         Log.d(LOGTAG, "In startRecording of Recorder");
     if( (true == mSingleRecordingInstanceSupported) &&
         (true == mOverA2DP )) {
                 Toast.makeText( this,
                                 "playback on BT in progress,can't record now",
                                 Toast.LENGTH_SHORT).show();
                 return false;
        }
         stopRecording();
         mSampleFile = null;
         File sampleDir = Environment.getExternalStorageDirectory();
         if (!sampleDir.canWrite()) // Workaround for broken sdcard support on
                                     // the device.
             sampleDir = new File("/sdcard/sdcard");
         try {
             mSampleFile = File
                     .createTempFile("FMRecording", ".3gpp", sampleDir);
         } catch (IOException e) {
             Log.e(LOGTAG, "Not able to access SD Card");
             Toast.makeText(this, "Not able to access SD Card", Toast.LENGTH_SHORT).show();
             return false;
         }
         mRecorder = new MediaRecorder();
         if (mRecorder == null) {
            Toast.makeText(this,"MediaRecorder failed to create an instance",
                             Toast.LENGTH_SHORT).show();
            return false;
         }
 
         try {
         mRecorder.setAudioSource(MediaRecorder.AudioSource.FM_RX);
         mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
         } catch (RuntimeException exception) {
             mRecorder.reset();
             mRecorder.release();
             mRecorder = null;
             return false;
         }
         mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
         try {
             mRecorder.prepare();
             mRecorder.start();
         } catch (IOException e) {
             mRecorder.reset();
             mRecorder.release();
             mRecorder = null;
             return false;
         } catch (RuntimeException e) {
             mRecorder.reset();
             mRecorder.release();
             mRecorder = null;
             return false;
         }
         mFmRecordingOn = true;
         mSampleStart = System.currentTimeMillis();
         return true;
   }
 
    public boolean startA2dpPlayback() {
         Log.d(LOGTAG, "In startA2dpPlayback");
     if( (true == mSingleRecordingInstanceSupported) &&
         (true == mFmRecordingOn )) {
                 Toast.makeText(this,
                                "Recording already in progress,can't play on BT",
                                Toast.LENGTH_SHORT).show();
                 return false;
        }
         if(mOverA2DP)
            stopA2dpPlayback();
         mA2dp = new MediaRecorder();
         if (mA2dp == null) {
            Toast.makeText(this,"A2dpPlayback failed to create an instance",
                             Toast.LENGTH_SHORT).show();
            return false;
         }
         try {
             mA2dp.setAudioSource(MediaRecorder.AudioSource.FM_RX_A2DP);
             mA2dp.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
             mA2dp.setAudioEncoder(MediaRecorder.OutputFormat.DEFAULT);
             File sampleDir = Environment.getExternalStorageDirectory();
             if (!sampleDir.canWrite())
                 sampleDir = new File("/sdcard/sdcard");
             try {
                 mA2DPSampleFile = File
                     .createTempFile("FMRecording", ".3gpp", sampleDir);
             } catch (IOException e) {
                 Log.e(LOGTAG, "Not able to access SD Card");
                 Toast.makeText(this, "Not able to access SD Card", Toast.LENGTH_SHORT).show();
                 return false;
             }
             mA2dp.setOutputFile(mA2DPSampleFile.getAbsolutePath());
             mA2dp.prepare();
             mA2dp.start();
         } catch (Exception exception) {
             mA2dp.reset();
             mA2dp.release();
             mA2dp = null;
             return false;
         }
         return true;
  }
 
    public void stopA2dpPlayback() {
        if (mA2dp == null)
            return;
        if(mA2DPSampleFile != null)
        {
           try {
               mA2DPSampleFile.delete();
           } catch (Exception e) {
               Log.e(LOGTAG, "Not able to delete file");
           }
        }
        try {
            mA2dp.stop();
            mA2dp.reset();
            mA2dp.release();
            mA2dp = null;
        } catch (Exception exception ) {
            Log.e( LOGTAG, "Stop failed with exception"+ exception);
        }
        return;
    }
 
    private void resetA2dpPlayback() {
        if (mA2dp == null)
            return;
        if(mA2DPSampleFile != null)
        {
           try {
               mA2DPSampleFile.delete();
           } catch (Exception e) {
               Log.e(LOGTAG, "Not able to delete file");
           }
        }
        try {
            // Send Intent for IOBUSY VOTE, because MediaRecorder.stop
            // gets Activity context which might not be always available
            // and would thus fail to send the intent.
            Intent ioBusyUnVoteIntent = new Intent(IOBUSY_UNVOTE);
            // Remove vote for io_is_busy to be turned off.
            ioBusyUnVoteIntent.putExtra("com.android.server.CpuGovernorService.voteType", 0);
            sendBroadcast(ioBusyUnVoteIntent);
 
            mA2dp.stop();
 
            mA2dp.reset();
            mA2dp.release();
            mA2dp = null;
        } catch (Exception exception ) {
            Log.e( LOGTAG, "Stop failed with exception"+ exception);
        }
        return;
    }
 
    private void resetRecording() {
 
        Log.v(LOGTAG, "resetRecording()");
 
        mFmRecordingOn = false;
        if (mRecorder == null)
            return;
 
        // Send Intent for IOBUSY VOTE, because MediaRecorder.stop
        // gets Activity context which might not be always available
        // and would thus fail to send the intent.
        Intent ioBusyUnVoteIntent = new Intent(IOBUSY_UNVOTE);
        // Remove vote for io_is_busy to be turned off.
        ioBusyUnVoteIntent.putExtra("com.android.server.CpuGovernorService.voteType", 0);
        sendBroadcast(ioBusyUnVoteIntent);
 
        mRecorder.stop();
 
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
        int sampleLength = (int)((System.currentTimeMillis() - mSampleStart)/1000 );
        if (sampleLength == 0)
            return;
        String state = Environment.getExternalStorageState();
        Log.d(LOGTAG, "storage state is " + state);
 
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            this.addToMediaDB(mSampleFile);
        }
        else{
            Log.e(LOGTAG, "SD card must have removed during recording. ");
            Toast.makeText(this, "Recording aborted", Toast.LENGTH_SHORT).show();
        }
        return;
    }
 
    public void stopRecording() {
        mFmRecordingOn = false;
        if (mRecorder == null)
            return;
        try {
              mRecorder.stop();
              mRecorder.reset();
              mRecorder.release();
              mRecorder = null;
        } catch(Exception e) {
              e.printStackTrace();
        }
        int sampleLength = (int)((System.currentTimeMillis() - mSampleStart)/1000 );
        if (sampleLength == 0)
            return;
        String state = Environment.getExternalStorageState();
        Log.d(LOGTAG, "storage state is " + state);
 
        if (Environment.MEDIA_MOUNTED.equals(state)) {
           try {
                this.addToMediaDB(mSampleFile);
           }
           catch(Exception e) {
                e.printStackTrace();
           }
        }
        else{
            Log.e(LOGTAG, "SD card must have removed during recording. ");
            Toast.makeText(this, "Recording aborted", Toast.LENGTH_SHORT).show();
        }
        try
        {
            if((mServiceInUse) && (mCallbacks != null) ) {
                mCallbacks.onRecordingStopped();
        }
        } catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return;
    }
 
    /*
     * Adds file and returns content uri.
     */
    private Uri addToMediaDB(File file) {
        Log.d(LOGTAG, "In addToMediaDB");
        Resources res = getResources();
        ContentValues cv = new ContentValues();
        long current = System.currentTimeMillis();
        long modDate = file.lastModified();
        Date date = new Date(current);
        SimpleDateFormat formatter = new SimpleDateFormat(
                res.getString(R.string.audio_db_title_format));
        String title = formatter.format(date);
 
        // Lets label the recorded audio file as NON-MUSIC so that the file
        // won't be displayed automatically, except for in the playlist.
        cv.put(MediaStore.Audio.Media.IS_MUSIC, "1");
 
        cv.put(MediaStore.Audio.Media.TITLE, title);
        cv.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
        cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
        cv.put(MediaStore.Audio.Media.MIME_TYPE, "AUDIO_AAC_MP4");
        cv.put(MediaStore.Audio.Media.ARTIST,
                res.getString(R.string.audio_db_artist_name));
        cv.put(MediaStore.Audio.Media.ALBUM,
                res.getString(R.string.audio_db_album_name));
        Log.d(LOGTAG, "Inserting audio record: " + cv.toString());
        ContentResolver resolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.d(LOGTAG, "ContentURI: " + base);
        Uri result = resolver.insert(base, cv);
        if (result == null) {
            Toast.makeText(this, "Unable to save recorded audio", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (getPlaylistId(res) == -1) {
            createPlaylist(res, resolver);
        }
        int audioId = Integer.valueOf(result.getLastPathSegment());
        addToPlaylist(resolver, audioId, getPlaylistId(res));
 
        // Notify those applications such as Music listening to the
        // scanner events that a recorded audio file just created.
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
        return result;
    }
 
    private int getPlaylistId(Resources res) {
        Uri uri = MediaStore.Audio.Playlists.getContentUri("external");
        final String[] ids = new String[] { MediaStore.Audio.Playlists._ID };
        final String where = MediaStore.Audio.Playlists.NAME + "=?";
        final String[] args = new String[] { res.getString(R.string.audio_db_playlist_name) };
        Cursor cursor = query(uri, ids, where, args, null);
        if (cursor == null) {
            Log.v(LOGTAG, "query returns null");
        }
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }
 
    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
         } catch (UnsupportedOperationException ex) {
            return null;
        }
    }
 
    private Uri createPlaylist(Resources res, ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Playlists.NAME, res.getString(R.string.audio_db_playlist_name));
        Uri uri = resolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), cv);
        if (uri == null) {
            Toast.makeText(this, "Unable to save recorded audio", Toast.LENGTH_SHORT).show();
        }
        return uri;
    }
 
    private void addToPlaylist(ContentResolver resolver, int audioId, long playlistId) {
        String[] cols = new String[] {
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        final int base;
        if (cur != null) {
             cur.moveToFirst();
             base = cur.getInt(0);
             cur.close();
        }
        else {
             base = 0;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
        resolver.insert(uri, values);
    }
    private void fmActionOnCallState( int state ) {
    //if Call Status is non IDLE we need to Mute FM as well stop recording if
    //any. Similarly once call is ended FM should be unmuted.
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mCallStatus = state;
 
        if((TelephonyManager.CALL_STATE_OFFHOOK == state)||
           (TelephonyManager.CALL_STATE_RINGING == state)) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume == 0) {
                    return;
                }
            }
        boolean bTempSpeaker = mSpeakerPhoneOn; //need to restore SpeakerPhone
        boolean bTempMute    = mMuted;// need to restore Mute status
        int bTempCall = mCallStatus;//need to restore call status
        fmOff();
        try
            {
                // Notify the UI/Activity, only if the service is "bound"
                // by an activity and if Callbacks are registered
                if((mServiceInUse) && (mCallbacks != null) )
                {
                    mCallbacks.onDisabled();
                }
             } catch (RemoteException e)
             {
                 e.printStackTrace();
             }
            mResumeAfterCall = true;
            mSpeakerPhoneOn = bTempSpeaker;
            mCallStatus = bTempCall;
            mMuted = bTempMute;
        }
        else if (state == TelephonyManager.CALL_STATE_IDLE) {
           // start playing again
           if (mResumeAfterCall)
           {
              // resume playback only if FM Radio was playing
              // when the call was answered
               if ((isAntennaAvailable()) && (!isFmOn())
                    && (mServiceInUse) && (mCallbacks != null))
               {
                    Log.d(LOGTAG, "Resuming after call:" );
                    if( true != fmOn() ) {
                        return;
                    }
                    mResumeAfterCall = false;
                    try
                    {
                        mCallbacks.onEnabled();
                    } catch (RemoteException e)
                    {
                        e.printStackTrace();
                    }
               }
           }
        }//idle
    }
 
     /* Handle Phone Call + FM Concurrency */
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
       @Override
       public void onCallStateChanged(int state, String incomingNumber) {
           Log.d(LOGTAG, "onCallStateChanged: State - " + state );
           Log.d(LOGTAG, "onCallStateChanged: incomingNumber - " + incomingNumber );
           fmActionOnCallState(state );
       }
 
       @Override
       public void onDataActivity (int direction) {
           Log.d(LOGTAG, "onDataActivity - " + direction );
           if (direction == TelephonyManager.DATA_ACTIVITY_NONE ||
               direction == TelephonyManager.DATA_ACTIVITY_DORMANT) {
                  if (mReceiver != null) {
                        Message msg = mDelayedStopHandler.obtainMessage(RESET_NOTCH_FILTER);
                        mDelayedStopHandler.sendMessageDelayed(msg, 10000);
                  }
          } else {
                if (mReceiver != null) {
                    if( true == mNotchFilterSet )
                    {
                        mDelayedStopHandler.removeMessages(RESET_NOTCH_FILTER);
                    }
                    else
                    {
                        mReceiver.setNotchFilter(true);
                        mNotchFilterSet = true;
                    }
                }
          }
       }
  };
 
    private Handler mDelayedStopHandler = new Handler() {
       @Override
       public void handleMessage(Message msg) {
           switch (msg.what) {
           case FM_STOP:
               // Check again to make sure nothing is playing right now
               if (isFmOn() || mServiceInUse)
               {
                    return;
               }
               Log.d(LOGTAG, "mDelayedStopHandler: stopSelf");
               stopSelf(mServiceStartId);
               break;
           case RESET_NOTCH_FILTER:
               if (mReceiver != null) {
                   mReceiver.setNotchFilter(false);
                   mNotchFilterSet = false;
               }
               break;
           case STOPSERVICE_ONSLEEP:
               fmOff();
               break;
           case STOPRECORD_ONTIMEOUT:
               stopRecording();
               break;
           case FOCUSCHANGE:
               if( false == isFmOn() ) {
                   Log.v(LOGTAG, "FM is not running, not handling change");
                   return;
               }
               switch (msg.arg1) {
                   case AudioManager.AUDIOFOCUS_LOSS:
                       Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                       //intentional fall through.
                   case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                       Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                       if (mSpeakerPhoneOn) {
                           mSpeakerPhoneOn = false;
                           if (isAnalogModeSupported()) {
                               setAudioPath(true);
                           }
                           AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
                       }
                       if(true == isFmRecordingOn())
                           stopRecording();
                       if(true == mPlaybackInProgress)
                           stopFM();
                       mStoppedOnFocusLoss = true;
                       break;
                   case AudioManager.AUDIOFOCUS_GAIN:
                       Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                       if(false == mPlaybackInProgress)
                           startFM();
                       mStoppedOnFocusLoss = false;
                       break;
                   case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                   default:
                       Log.e(LOGTAG, "Unknown audio focus change code"+msg.arg1);
               }
               break;
           }
       }
    };
 
 
      /**
      * Registers an intent to listen for
      * ACTION_SCREEN_ON/ACTION_SCREEN_OFF notifications. This intent
      * is called to know iwhen the screen is turned on/off.
      */
     public void registerScreenOnOffListener() {
         if (mScreenOnOffReceiver == null) {
             mScreenOnOffReceiver = new BroadcastReceiver() {
                 @Override
                 public void onReceive(Context context, Intent intent) {
                     String action = intent.getAction();
                     if (action.equals(Intent.ACTION_SCREEN_ON)) {
                        Log.d(LOGTAG, "ACTION_SCREEN_ON Intent received");
                        //Screen turned on, set FM module into normal power mode
                        mHandler.post(mScreenOnHandler);
                     }
                     else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                        Log.d(LOGTAG, "ACTION_SCREEN_OFF Intent received");
                        //Screen turned on, set FM module into low power mode
                        mHandler.post(mScreenOffHandler);
                     }
                 }
             };
             IntentFilter iFilter = new IntentFilter();
             iFilter.addAction(Intent.ACTION_SCREEN_ON);
             iFilter.addAction(Intent.ACTION_SCREEN_OFF);
             registerReceiver(mScreenOnOffReceiver, iFilter);
         }
     }
 
     /* Handle all the Screen On actions:
        Set FM Power mode to Normal
      */
     final Runnable    mScreenOnHandler = new Runnable() {
        public void run() {
           setLowPowerMode(false);
        }
     };
     /* Handle all the Screen Off actions:
        Set FM Power mode to Low Power
        This will reduce all the interrupts coming up from the SoC, saving power
      */
     final Runnable    mScreenOffHandler = new Runnable() {
        public void run() {
           setLowPowerMode(true);
        }
     };
 
    /* Show the FM Notification */
    public void startNotification() {
       RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
       views.setImageViewResource(R.id.icon, R.drawable.stat_notify_fm);
       if (isFmOn())
       {
          views.setTextViewText(R.id.frequency, getTunedFrequencyString());
       } else
       {
          views.setTextViewText(R.id.frequency, "");
       }
 
       Notification status = new Notification();
       status.contentView = views;
       status.flags |= Notification.FLAG_ONGOING_EVENT;
       status.icon = R.drawable.stat_notify_fm;
       status.contentIntent = PendingIntent.getActivity(this, 0,
                                                        new Intent("com.quicinc.fmradio.FMRADIO_ACTIVITY"), 0);
       startForeground(FMRADIOSERVICE_STATUS, status);
       //NotificationManager nm = (NotificationManager)
       //                         getSystemService(Context.NOTIFICATION_SERVICE);
       //nm.notify(FMRADIOSERVICE_STATUS, status);
       //setForeground(true);
       mFMOn = true;
    }
 
    private void stop() {
       Log.d(LOGTAG,"in stop");
       try {
            if (mFMOn && (mCallbacks != null))
                mCallbacks. onFinishActivity();
       } catch (RemoteException e) {
            e.printStackTrace();
       }
       if (!mServiceInUse) {
           Log.d(LOGTAG,"calling unregisterMediaButtonEventReceiver in stop");
           mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
           ComponentName fmRadio = new ComponentName(this.getPackageName(),
                                   FMMediaButtonIntentReceiver.class.getName());
           mAudioManager.unregisterMediaButtonEventReceiver(fmRadio);
       }
       gotoIdleState();
       mFMOn = false;
    }
 
    private void gotoIdleState() {
       mDelayedStopHandler.removeCallbacksAndMessages(null);
       Message msg = mDelayedStopHandler.obtainMessage();
       msg.what = FM_STOP;
       mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
       //NotificationManager nm =
       //(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       //nm.cancel(FMRADIOSERVICE_STATUS);
       //setForeground(false);
       stopForeground(true);
    }
 
    /** Read's the internal Antenna available state from the FM
     *  Device.
     */
    public void readInternalAntennaAvailable()
    {
       mInternalAntennaAvailable  = false;
       if (mReceiver != null)
       {
          mInternalAntennaAvailable = mReceiver.getInternalAntenna();
          Log.d(LOGTAG, "getInternalAntenna: " + mInternalAntennaAvailable);
       }
    }
    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IFMRadioService.Stub
    {
       WeakReference<FMRadioService> mService;
 
       ServiceStub(FMRadioService service)
       {
          mService = new WeakReference<FMRadioService>(service);
       }
 
       public boolean fmOn() throws RemoteException
       {
          return(mService.get().fmOn());
       }
 
       public boolean fmOff() throws RemoteException
       {
          return(mService.get().fmOff());
       }
 
       public boolean fmRadioReset() throws RemoteException
       {
          return true;
       }
 
       public boolean isFmOn()
       {
          return(mService.get().isFmOn());
       }
 
       public boolean isAnalogModeEnabled()
       {
          return(mService.get().isAnalogModeEnabled());
       }
 
       public boolean isFmRecordingOn()
       {
          return(mService.get().isFmRecordingOn());
       }
 
       public boolean isSpeakerEnabled()
       {
          return(mService.get().isSpeakerEnabled());
       }
 
       public boolean fmReconfigure()
       {
          return(mService.get().fmReconfigure());
       }
 
       public void registerCallbacks(IFMRadioServiceCallbacks cb) throws RemoteException
       {
          mService.get().registerCallbacks(cb);
       }
 
       public void unregisterCallbacks() throws RemoteException
       {
          mService.get().unregisterCallbacks();
       }
 
       public boolean routeAudio(int device)
       {
          return(mService.get().routeAudio(device));
       }
 
       public boolean mute()
       {
          return(mService.get().mute());
       }
 
       public boolean unMute()
       {
          return(mService.get().unMute());
       }
 
       public boolean isMuted()
       {
          return(mService.get().isMuted());
       }
 
       public boolean startRecording()
       {
          return(mService.get().startRecording());
       }
 
       public void stopRecording()
       {
          mService.get().stopRecording();
       }
 
       public boolean tune(int frequency)
       {
          return(mService.get().tune(frequency));
       }
 
       public boolean seek(boolean up)
       {
          return(mService.get().seek(up));
       }
 
       public void enableSpeaker(boolean speakerOn)
       {
           mService.get().enableSpeaker(speakerOn);
       }
 
       public boolean scan(int pty)
       {
          return(mService.get().scan(pty));
       }
 
       public boolean seekPI(int piCode)
       {
          return(mService.get().seekPI(piCode));
       }
       public boolean searchStrongStationList(int numStations)
       {
          return(mService.get().searchStrongStationList(numStations));
       }
 
       public boolean cancelSearch()
       {
          return(mService.get().cancelSearch());
       }
 
       public String getProgramService()
       {
          return(mService.get().getProgramService());
       }
       public String getRadioText()
       {
          return(mService.get().getRadioText());
       }
       public int getProgramType()
       {
          return(mService.get().getProgramType());
       }
       public int getProgramID()
       {
          return(mService.get().getProgramID());
       }
       public int[] getSearchList()
       {
          return(mService.get().getSearchList());
       }
 
       public boolean setLowPowerMode(boolean enable)
       {
          return(mService.get().setLowPowerMode(enable));
       }
 
       public int getPowerMode()
       {
          return(mService.get().getPowerMode());
       }
       public boolean enableAutoAF(boolean bEnable)
       {
          return(mService.get().enableAutoAF(bEnable));
       }
       public boolean enableStereo(boolean bEnable)
       {
          return(mService.get().enableStereo(bEnable));
       }
       public boolean isAntennaAvailable()
       {
          return(mService.get().isAntennaAvailable());
       }
       public boolean isWiredHeadsetAvailable()
       {
          return(mService.get().isWiredHeadsetAvailable());
       }
       public boolean isCallActive()
       {
           return(mService.get().isCallActive());
       }
       public int getRssi()
       {
           return (mService.get().getRssi());
       }
       public int getIoC()
       {
           return (mService.get().getIoC());
       }
       public int getMpxDcc()
       {
           return (mService.get().getMpxDcc());
       }
       public int getIntDet()
       {
           return (mService.get().getIntDet());
       }
       public void setHiLoInj(int inj)
       {
           mService.get().setHiLoInj(inj);
       }
       public void delayedStop(long duration, int nType)
       {
           mService.get().delayedStop(duration, nType);
       }
       public void cancelDelayedStop(int nType)
       {
           mService.get().cancelDelayedStop(nType);
       }
       public void requestFocus()
       {
           mService.get().requestFocus();
       }
       public int getSINR()
       {
           return (mService.get().getSINR());
       }
 
    }
 
    private final IBinder mBinder = new ServiceStub(this);
 
    private boolean setAudioPath(boolean analogMode) {
 
         if (mReceiver == null) {
               return false;
         }
         if (isAnalogModeEnabled() == analogMode) {
                 Log.d(LOGTAG,"Analog Path already is set to "+analogMode);
                 return false;
         }
         if (!isAnalogModeSupported()) {
                 Log.d(LOGTAG,"Analog Path is not supported ");
                 return false;
         }
         if (SystemProperties.getBoolean("hw.fm.digitalpath",false)) {
                 return false;
         }
 
         boolean state = mReceiver.setAnalogMode(analogMode);
         if (false == state) {
             Log.d(LOGTAG, "Error in toggling analog/digital path " + analogMode);
             return false;
         }
         misAnalogPathEnabled = analogMode;
         return true;
    }
   /*
    * Turn ON FM: Powers up FM hardware, and initializes the FM module
    *                                                                                 .
    * @return true if fm Enable api was invoked successfully, false if the api failed.
    */
    private boolean fmOn() {
       boolean bStatus=false;
       mWakeLock.acquire(10*1000);
       if ( TelephonyManager.CALL_STATE_IDLE != getCallState() ) {
          return bStatus;
       }
 
       if(mReceiver == null)
       {
          try {
             mReceiver = new FmReceiver(FMRADIO_DEVICE_FD_STRING, fmCallbacks);
          }
          catch (InstantiationException e)
          {
             throw new RuntimeException("FmReceiver service not available!");
          }
       }
 
       if (mReceiver != null)
       {
          if (isFmOn())
          {
             /* FM Is already on,*/
             bStatus = true;
             Log.d(LOGTAG, "mReceiver.already enabled");
          }
          else
          {
             // This sets up the FM radio device
             FmConfig config = FmSharedPreferences.getFMConfiguration();
             Log.d(LOGTAG, "fmOn: RadioBand   :"+ config.getRadioBand());
             Log.d(LOGTAG, "fmOn: Emphasis    :"+ config.getEmphasis());
             Log.d(LOGTAG, "fmOn: ChSpacing   :"+ config.getChSpacing());
             Log.d(LOGTAG, "fmOn: RdsStd      :"+ config.getRdsStd());
             Log.d(LOGTAG, "fmOn: LowerLimit  :"+ config.getLowerLimit());
             Log.d(LOGTAG, "fmOn: UpperLimit  :"+ config.getUpperLimit());
             bStatus = mReceiver.enable(FmSharedPreferences.getFMConfiguration());
             if (isSpeakerEnabled()) {
                 setAudioPath(false);
             } else {
                 setAudioPath(true);
             }
             Log.d(LOGTAG, "mReceiver.enable done, Status :" +  bStatus);
          }
 
          if (bStatus == true)
          {
             /* Put the hardware into normal mode */
             bStatus = setLowPowerMode(false);
             Log.d(LOGTAG, "setLowPowerMode done, Status :" +  bStatus);
 
 
             AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
             if( (audioManager != null) &&(false == mPlaybackInProgress) )
             {
                Log.d(LOGTAG, "mAudioManager.setFmRadioOn = true \n" );
                //audioManager.setParameters("FMRadioOn="+mAudioDevice);
                int state =  getCallState();
                if ( TelephonyManager.CALL_STATE_IDLE != getCallState() )
                {
                  fmActionOnCallState(state);
                } else {
                    startFM(); // enable FM Audio only when Call is IDLE
                }
                Log.d(LOGTAG, "mAudioManager.setFmRadioOn done \n" );
             }
             if (mReceiver != null) {
                 bStatus = mReceiver.registerRdsGroupProcessing(FmReceiver.FM_RX_RDS_GRP_RT_EBL|
                                                            FmReceiver.FM_RX_RDS_GRP_PS_EBL|
                                                            FmReceiver.FM_RX_RDS_GRP_AF_EBL|
                                                            FmReceiver.FM_RX_RDS_GRP_PS_SIMPLE_EBL);
                 Log.d(LOGTAG, "registerRdsGroupProcessing done, Status :" +  bStatus);
             }
             bStatus = enableAutoAF(FmSharedPreferences.getAutoAFSwitch());
             Log.d(LOGTAG, "enableAutoAF done, Status :" +  bStatus);
 
             /* There is no internal Antenna*/
             bStatus = mReceiver.setInternalAntenna(false);
             Log.d(LOGTAG, "setInternalAntenna done, Status :" +  bStatus);
 
             /* Read back to verify the internal Antenna mode*/
             readInternalAntennaAvailable();
 
             startNotification();
             bStatus = true;
          }
          else
          {
             mReceiver = null; // as enable failed no need to disable
                               // failure of enable can be because handle
                               // already open which gets effected if
                               // we disable
             stop();
          }
       }
       return(bStatus);
    }
 
   /*
    * Turn OFF FM Operations: This disables all the current FM operations             .
    */
    private void fmOperationsOff() {
       if ( mSpeakerPhoneOn)
       {
           mSpeakerPhoneOn = false;
           AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
       }
       if (isFmRecordingOn())
       {
           stopRecording();
       }
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       if(audioManager != null)
       {
          Log.d(LOGTAG, "audioManager.setFmRadioOn = false \n" );
          unMute();
          stopFM();
          //audioManager.setParameters("FMRadioOn=false");
          Log.d(LOGTAG, "audioManager.setFmRadioOn false done \n" );
       }
 
       if (isAnalogModeEnabled()) {
               SystemProperties.set("hw.fm.isAnalog","false");
               misAnalogPathEnabled = false;
       }
    }
 
   /*
    * Reset (OFF) FM Operations: This resets all the current FM operations             .
    */
    private void fmOperationsReset() {
       if ( mSpeakerPhoneOn)
       {
           mSpeakerPhoneOn = false;
           AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
       }
 
       if (isFmRecordingOn())
       {
           resetRecording();
       }
 
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       if(audioManager != null)
       {
          Log.d(LOGTAG, "audioManager.setFmRadioOn = false \n" );
          unMute();
          resetFM();
          //audioManager.setParameters("FMRadioOn=false");
          Log.d(LOGTAG, "audioManager.setFmRadioOn false done \n" );
       }
 
       if (isAnalogModeEnabled()) {
               SystemProperties.set("hw.fm.isAnalog","false");
               misAnalogPathEnabled = false;
       }
    }
 
   /*
    * Turn OFF FM: Disable the FM Host and hardware                                  .
    *                                                                                 .
    * @return true if fm Disable api was invoked successfully, false if the api failed.
    */
    private boolean fmOff() {
       boolean bStatus=false;
 
       fmOperationsOff();
 
       // This will disable the FM radio device
       if (mReceiver != null)
       {
          bStatus = mReceiver.disable();
          mReceiver = null;
       }
       stop();
       return(bStatus);
    }
 
   /*
    * Turn OFF FM: Disable the FM Host when hardware resets asynchronously            .
    *                                                                                 .
    * @return true if fm Reset api was invoked successfully, false if the api failed  .
    */
    private boolean fmRadioReset() {
       boolean bStatus=false;
 
       Log.v(LOGTAG, "fmRadioReset");
 
       fmOperationsReset();
 
       // This will reset the FM radio receiver
       if (mReceiver != null)
       {
          bStatus = mReceiver.reset();
          mReceiver = null;
       }
       stop();
       return(bStatus);
    }
 
    /* Returns whether FM hardware is ON.
     *
     * @return true if FM was tuned, searching. (at the end of
     * the search FM goes back to tuned).
     *
     */
    public boolean isFmOn() {
       return mFMOn;
    }
 
    /* Returns true if Analog Path is enabled */
    public boolean isAnalogModeEnabled() {
          return misAnalogPathEnabled;
    }
 
    public boolean isAnalogModeSupported() {
         return misAnalogModeSupported;
    }
 
    public boolean isFmRecordingOn() {
       return mFmRecordingOn;
    }
 
    public boolean isSpeakerEnabled() {
       return mSpeakerPhoneOn;
    }
    public boolean isExternalStorageAvailable() {
      boolean mStorageAvail = false;
      String state = Environment.getExternalStorageState();
 
      if(Environment.MEDIA_MOUNTED.equals(state)){
          Log.d(LOGTAG, "device available");
          mStorageAvail = true;
      }
      return mStorageAvail;
    }
    public void enableSpeaker(boolean speakerOn) {
        if(isCallActive())
            return ;
        mSpeakerPhoneOn = speakerOn;
        boolean analogmode = isAnalogModeSupported();
        if (false == speakerOn) {
            if (analogmode) {
                 stopFM();
                 setAudioPath(true);
            }
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NONE);
            if (analogmode)
                 startFM();
        }
 
        //Need to turn off BT path when Speaker is set on vice versa.
        if( !analogmode && true == mA2dpDeviceState.isDeviceAvailable()) {
            if( ((true == mOverA2DP) && (true == speakerOn)) ||
                ((false == mOverA2DP) && (false == speakerOn)) ) {
               //disable A2DP playback for speaker option
                stopFM();
                startFM();
            }
        }
        if (speakerOn) {
            if (analogmode) {
                  stopFM();
                  if (mMuted) {
                      setAudioPath(false);
                  } else {
                      mute();
                      setAudioPath(false);
                      unMute();
                  }
            }
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_SPEAKER);
            if (analogmode)
                 startFM();
        }
 
    }
   /*
    *  ReConfigure the FM Setup parameters
    *  - Band
    *  - Channel Spacing (50/100/200 KHz)
    *  - Emphasis (50/75)
    *  - Frequency limits
    *  - RDS/RBDS standard
    *
    * @return true if configure api was invoked successfully, false if the api failed.
    */
    public boolean fmReconfigure() {
       boolean bStatus=false;
       Log.d(LOGTAG, "fmReconfigure");
       if (mReceiver != null)
       {
          // This sets up the FM radio device
          FmConfig config = FmSharedPreferences.getFMConfiguration();
          Log.d(LOGTAG, "RadioBand   :"+ config.getRadioBand());
          Log.d(LOGTAG, "Emphasis    :"+ config.getEmphasis());
          Log.d(LOGTAG, "ChSpacing   :"+ config.getChSpacing());
          Log.d(LOGTAG, "RdsStd      :"+ config.getRdsStd());
          Log.d(LOGTAG, "LowerLimit  :"+ config.getLowerLimit());
          Log.d(LOGTAG, "UpperLimit  :"+ config.getUpperLimit());
          bStatus = mReceiver.configure(config);
       }
       return(bStatus);
    }
 
    /*
     * Register UI/Activity Callbacks
     */
    public void registerCallbacks(IFMRadioServiceCallbacks cb)
    {
       mCallbacks = cb;
    }
 
    /*
     *  unRegister UI/Activity Callbacks
     */
    public void unregisterCallbacks()
    {
       mCallbacks=null;
    }
 
    /*
    *  Route Audio to headset or speaker phone
    *  @return true if routeAudio call succeeded, false if the route call failed.
    */
    public boolean routeAudio(int audioDevice) {
       boolean bStatus=false;
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 
       //Log.d(LOGTAG, "routeAudio: " + audioDevice);
 
       switch (audioDevice) {
 
         case RADIO_AUDIO_DEVICE_WIRED_HEADSET:
             mAudioDevice = "headset";
             break;
 
         case RADIO_AUDIO_DEVICE_SPEAKER:
             mAudioDevice = "speaker";
             break;
 
         default:
             mAudioDevice = "headset";
             break;
       }
 
       if (mReceiver != null)
       {
       //audioManager.setParameters("FMRadioOn=false");
       //Log.d(LOGTAG, "mAudioManager.setFmRadioOn =" + mAudioDevice );
       //audioManager.setParameters("FMRadioOn="+mAudioDevice);
       //Log.d(LOGTAG, "mAudioManager.setFmRadioOn done \n");
        }
 
        return bStatus;
    }
 
   /*
    *  Mute FM Hardware (SoC)
    * @return true if set mute mode api was invoked successfully, false if the api failed.
    */
    public boolean mute() {
       boolean bCommandSent=true;
       if(isMuted())
           return bCommandSent;
       if(isCallActive())
          return false;
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       Log.d(LOGTAG, "mute:");
       if (audioManager != null)
       {
          mMuted = true;
          audioManager.setStreamMute(AudioManager.STREAM_FM,true);
       }
       return bCommandSent;
    }
 
    /*
    *  UnMute FM Hardware (SoC)
    * @return true if set mute mode api was invoked successfully, false if the api failed.
    */
    public boolean unMute() {
       boolean bCommandSent=true;
       if(!isMuted())
           return bCommandSent;
       if(isCallActive())
          return false;
       Log.d(LOGTAG, "unMute:");
       AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
       if (audioManager != null)
       {
          mMuted = false;
          audioManager.setStreamMute(AudioManager.STREAM_FM,false);
          if (mResumeAfterCall)
          {
              //We are unmuting FM in a voice call. Need to enable FM audio routing.
              startFM();
          }
       }
       return bCommandSent;
    }
 
    /* Returns whether FM Hardware(Soc) Audio is Muted.
     *
     * @return true if FM Audio is muted, false if not muted.
     *
     */
    public boolean isMuted() {
       return mMuted;
    }
 
    /* Tunes to the specified frequency
     *
     * @return true if Tune command was invoked successfully, false if not muted.
     *  Note: Callback FmRxEvRadioTuneStatus will be called when the tune
     *        is complete
     */
    public boolean tune(int frequency) {
       boolean bCommandSent=false;
       double doubleFrequency = frequency/1000.00;
 
       Log.d(LOGTAG, "tuneRadio:  " + doubleFrequency);
       if (mReceiver != null)
       {
          mReceiver.setStation(frequency);
          bCommandSent = true;
       }
       return bCommandSent;
    }
 
    /* Seeks (Search for strong station) to the station in the direction specified
     * relative to the tuned station.
     * boolean up: true - Search in the forward direction.
     *             false - Search in the backward direction.
     * @return true if Seek command was invoked successfully, false if not muted.
     *  Note: 1. Callback FmRxEvSearchComplete will be called when the Search
     *        is complete
     *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
     *        at the end of the Search or if the seach was cancelled.
     */
    public boolean seek(boolean up)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          if (up == true)
          {
             Log.d(LOGTAG, "seek:  Up");
             mReceiver.searchStations(FmReceiver.FM_RX_SRCH_MODE_SEEK,
                                              FmReceiver.FM_RX_DWELL_PERIOD_1S,
                                              FmReceiver.FM_RX_SEARCHDIR_UP);
          }
          else
          {
             Log.d(LOGTAG, "seek:  Down");
             mReceiver.searchStations(FmReceiver.FM_RX_SRCH_MODE_SEEK,
                                              FmReceiver.FM_RX_DWELL_PERIOD_1S,
                                              FmReceiver.FM_RX_SEARCHDIR_DOWN);
          }
          bCommandSent = true;
       }
       return bCommandSent;
    }
 
    /* Scan (Search for station with a "preview" of "n" seconds)
     * FM Stations. It always scans in the forward direction relative to the
     * current tuned station.
     * int pty: 0 or a reserved PTY value- Perform a "strong" station search of all stations.
     *          Valid/Known PTY - perform RDS Scan for that pty.
     *
     * @return true if Scan command was invoked successfully, false if not muted.
     *  Note: 1. Callback FmRxEvRadioTuneStatus will be called when tuned to various stations
     *           during the Scan.
     *        2. Callback FmRxEvSearchComplete will be called when the Search
     *        is complete
     *        3. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
     *        at the end of the Search or if the seach was cancelled.
     *
     */
    public boolean scan(int pty)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "scan:  PTY: " + pty);
          if(FmSharedPreferences.isRBDSStd())
          {
             /* RBDS : Validate PTY value?? */
             if( ((pty  > 0) && (pty  <= 23)) || ((pty  >= 29) && (pty  <= 31)) )
             {
                bCommandSent = mReceiver.searchStations(FmReceiver.FM_RX_SRCHRDS_MODE_SCAN_PTY,
                                                        FmReceiver.FM_RX_DWELL_PERIOD_2S,
                                                        FmReceiver.FM_RX_SEARCHDIR_UP,
                                                        pty,
                                                        0);
             }
             else
             {
                bCommandSent = mReceiver.searchStations(FmReceiver.FM_RX_SRCH_MODE_SCAN,
                                                 FmReceiver.FM_RX_DWELL_PERIOD_2S,
                                                 FmReceiver.FM_RX_SEARCHDIR_UP);
             }
          }
          else
          {
             /* RDS : Validate PTY value?? */
             if( (pty  > 0) && (pty  <= 31) )
             {
                bCommandSent = mReceiver.searchStations(FmReceiver.FM_RX_SRCHRDS_MODE_SCAN_PTY,
                                                           FmReceiver.FM_RX_DWELL_PERIOD_2S,
                                                           FmReceiver.FM_RX_SEARCHDIR_UP,
                                                           pty,
                                                           0);
             }
             else
             {
                bCommandSent = mReceiver.searchStations(FmReceiver.FM_RX_SRCH_MODE_SCAN,
                                                 FmReceiver.FM_RX_DWELL_PERIOD_2S,
                                                 FmReceiver.FM_RX_SEARCHDIR_UP);
             }
          }
       }
       return bCommandSent;
    }
 
    /* Search for the 'numStations' number of strong FM Stations.
     *
     * It searches in the forward direction relative to the current tuned station.
     * int numStations: maximum number of stations to search.
     *
     * @return true if Search command was invoked successfully, false if not muted.
     *  Note: 1. Callback FmRxEvSearchListComplete will be called when the Search
     *        is complete
     *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to
     *        the previously tuned station.
     */
    public boolean searchStrongStationList(int numStations)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "searchStrongStationList:  numStations: " + numStations);
          bCommandSent = mReceiver.searchStationList(FmReceiver.FM_RX_SRCHLIST_MODE_STRONG,
                                                     FmReceiver.FM_RX_SEARCHDIR_UP,
                                                     numStations,
                                                     0);
       }
       return bCommandSent;
    }
 
    /* Search for the FM Station that matches the RDS PI (Program Identifier) code.
     * It always scans in the forward direction relative to the current tuned station.
     * int piCode: PI Code of the station to search.
     *
     * @return true if Search command was invoked successfully, false if not muted.
     *  Note: 1. Callback FmRxEvSearchComplete will be called when the Search
     *        is complete
     *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
     *        at the end of the Search or if the seach was cancelled.
     */
    public boolean seekPI(int piCode)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "seekPI:  piCode: " + piCode);
          bCommandSent = mReceiver.searchStations(FmReceiver.FM_RX_SRCHRDS_MODE_SEEK_PI,
                                                             FmReceiver.FM_RX_DWELL_PERIOD_1S,
                                                             FmReceiver.FM_RX_SEARCHDIR_UP,
                                                             0,
                                                             piCode
                                                             );
       }
       return bCommandSent;
    }
 
 
   /* Cancel any ongoing Search (Seek/Scan/SearchStationList).
    *
    * @return true if Search command was invoked successfully, false if not muted.
    *  Note: 1. Callback FmRxEvSearchComplete will be called when the Search
    *        is complete/cancelled.
    *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
    *        at the end of the Search or if the seach was cancelled.
    */
    public boolean cancelSearch()
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "cancelSearch");
          bCommandSent = mReceiver.cancelSearch();
       }
       return bCommandSent;
    }
 
    /* Retrieves the RDS Program Service (PS) String.
     *
     * @return String - RDS PS String.
     *  Note: 1. This is a synchronous call that should typically called when
     *           Callback FmRxEvRdsPsInfo is invoked.
     *        2. Since PS contains multiple fields, this Service reads all the fields and "caches"
     *        the values and provides this helper routine for the Activity to get only the information it needs.
     *        3. The "cached" data fields are always "cleared" when the tune status changes.
     */
    public String getProgramService() {
       String str = "";
       if (mFMRxRDSData != null)
       {
          str = mFMRxRDSData.getPrgmServices();
          if(str == null)
          {
             str= "";
          }
       }
       Log.d(LOGTAG, "Program Service: [" + str + "]");
       return str;
    }
 
    /* Retrieves the RDS Radio Text (RT) String.
     *
     * @return String - RDS RT String.
     *  Note: 1. This is a synchronous call that should typically called when
     *           Callback FmRxEvRdsRtInfo is invoked.
     *        2. Since RT contains multiple fields, this Service reads all the fields and "caches"
     *        the values and provides this helper routine for the Activity to get only the information it needs.
     *        3. The "cached" data fields are always "cleared" when the tune status changes.
     */
    public String getRadioText() {
       String str = "";
       if (mFMRxRDSData != null)
       {
          str = mFMRxRDSData.getRadioText();
          if(str == null)
          {
             str= "";
          }
       }
       Log.d(LOGTAG, "Radio Text: [" + str + "]");
       return str;
    }
 
    /* Retrieves the RDS Program Type (PTY) code.
     *
     * @return int - RDS PTY code.
     *  Note: 1. This is a synchronous call that should typically called when
     *           Callback FmRxEvRdsRtInfo and or FmRxEvRdsPsInfo is invoked.
     *        2. Since RT/PS contains multiple fields, this Service reads all the fields and "caches"
     *        the values and provides this helper routine for the Activity to get only the information it needs.
     *        3. The "cached" data fields are always "cleared" when the tune status changes.
     */
    public int getProgramType() {
       int pty = -1;
       if (mFMRxRDSData != null)
       {
          pty = mFMRxRDSData.getPrgmType();
       }
       Log.d(LOGTAG, "PTY: [" + pty + "]");
       return pty;
    }
 
    /* Retrieves the RDS Program Identifier (PI).
     *
     * @return int - RDS PI code.
     *  Note: 1. This is a synchronous call that should typically called when
     *           Callback FmRxEvRdsRtInfo and or FmRxEvRdsPsInfo is invoked.
     *        2. Since RT/PS contains multiple fields, this Service reads all the fields and "caches"
     *        the values and provides this helper routine for the Activity to get only the information it needs.
     *        3. The "cached" data fields are always "cleared" when the tune status changes.
     */
    public int getProgramID() {
       int pi = -1;
       if (mFMRxRDSData != null)
       {
          pi = mFMRxRDSData.getPrgmId();
       }
       Log.d(LOGTAG, "PI: [" + pi + "]");
       return pi;
    }
 
 
    /* Retrieves the station list from the SearchStationlist.
     *
     * @return Array of integers that represents the station frequencies.
     * Note: 1. This is a synchronous call that should typically called when
     *           Callback onSearchListComplete.
     */
    public int[] getSearchList()
    {
       int[] frequencyList = null;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "getSearchList: ");
          frequencyList = mReceiver.getStationList();
       }
       return frequencyList;
    }
 
    /* Set the FM Power Mode on the FM hardware SoC.
     * Typically used when UI/Activity is in the background, so the Host is interrupted less often.
     *
     * boolean bLowPower: true: Enable Low Power mode on FM hardware.
     *                    false: Disable Low Power mode on FM hardware. (Put into normal power mode)
     * @return true if set power mode api was invoked successfully, false if the api failed.
     */
    public boolean setLowPowerMode(boolean bLowPower)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "setLowPowerMode: " + bLowPower);
          if(bLowPower)
          {
             bCommandSent = mReceiver.setPowerMode(FmReceiver.FM_RX_LOW_POWER_MODE);
          }
          else
          {
             bCommandSent = mReceiver.setPowerMode(FmReceiver.FM_RX_NORMAL_POWER_MODE);
          }
       }
       return bCommandSent;
    }
 
    /* Get the FM Power Mode on the FM hardware SoC.
     *
     * @return the device power mode.
     */
    public int getPowerMode()
    {
       int powerMode=FmReceiver.FM_RX_NORMAL_POWER_MODE;
       if (mReceiver != null)
       {
          powerMode = mReceiver.getPowerMode();
          Log.d(LOGTAG, "getLowPowerMode: " + powerMode);
       }
       return powerMode;
    }
 
   /* Set the FM module to auto switch to an Alternate Frequency for the
    * station if one the signal strength of that frequency is stronger than the
    * current tuned frequency.
    *
    * boolean bEnable: true: Auto switch to stronger alternate frequency.
    *                  false: Do not switch to alternate frequency.
    *
    * @return true if set Auto AF mode api was invoked successfully, false if the api failed.
    *  Note: Callback FmRxEvRadioTuneStatus will be called when tune
    *        is complete to a different frequency.
    */
    public boolean enableAutoAF(boolean bEnable)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "enableAutoAF: " + bEnable);
          bCommandSent = mReceiver.enableAFjump(bEnable);
       }
       return bCommandSent;
    }
 
    /* Set the FM module to Stereo Mode or always force it to Mono Mode.
     * Note: The stereo mode will be available only when the station is broadcasting
     * in Stereo mode.
     *
     * boolean bEnable: true: Enable Stereo Mode.
     *                  false: Always stay in Mono Mode.
     *
     * @return true if set Stereo mode api was invoked successfully, false if the api failed.
     */
    public boolean enableStereo(boolean bEnable)
    {
       boolean bCommandSent=false;
       if (mReceiver != null)
       {
          Log.d(LOGTAG, "enableStereo: " + bEnable);
          bCommandSent = mReceiver.setStereoMode(bEnable);
       }
       return bCommandSent;
    }
 
    /** Determines if an internal Antenna is available.
     *  Returns the cached value initialized on FMOn.
     *
     * @return true if internal antenna is available or wired
     *         headset is plugged in, false if internal antenna is
     *         not available and wired headset is not plugged in.
     */
    public boolean isAntennaAvailable()
    {
       boolean bAvailable = false;
       if ((mInternalAntennaAvailable) || (mHeadsetPlugged) )
       {
          bAvailable = true;
       }
       return bAvailable;
    }
 
    /** Determines if a Wired headset is plugged in. Returns the
     *  cached value initialized on broadcast receiver
     *  initialization.
     *
     * @return true if wired headset is plugged in, false if wired
     *         headset is not plugged in.
     */
    public boolean isWiredHeadsetAvailable()
    {
       return (mHeadsetPlugged);
    }
    public boolean isCallActive()
    {
        //Non-zero: Call state is RINGING or OFFHOOK on the available subscriptions
        //zero: Call state is IDLE on all the available subscriptions
        if(0 != getCallState()) return true;
        return false;
    }
    public int getCallState()
    {
        return mCallStatus;
    }
 
   public void clearStationInfo() {
       if(mFMRxRDSData != null) {
          mFMRxRDSData.setRadioText("");
          mFMRxRDSData.setPrgmId(0);
          mFMRxRDSData.setPrgmType(0);
          mFMRxRDSData.setPrgmServices("");
       }
   }

    /* Receiver callbacks back from the FM Stack */
    FmRxEvCallbacksAdaptor fmCallbacks = new FmRxEvCallbacksAdaptor()
    {
       public void FmRxEvEnableReceiver() {
          Log.d(LOGTAG, "FmRxEvEnableReceiver");
       }
       public void FmRxEvDisableReceiver()
       {
          Log.d(LOGTAG, "FmRxEvDisableReceiver");
       }
       public void FmRxEvRadioReset()
       {
          Log.d(LOGTAG, "FmRxEvRadioReset");
          if(isFmOn()) {
              // Received radio reset event while FM is ON
              Log.d(LOGTAG, "FM Radio reset");
              fmRadioReset();
              try
              {
                 /* Notify the UI/Activity, only if the service is "bound"
                    by an activity and if Callbacks are registered
                 */
                 if((mServiceInUse) && (mCallbacks != null) )
                 {
                     mCallbacks.onRadioReset();
                 }
              }
              catch (RemoteException e)
              {
                 e.printStackTrace();
              }
          }
       }
       public void FmRxEvConfigReceiver()
       {
          Log.d(LOGTAG, "FmRxEvConfigReceiver");
       }
       public void FmRxEvMuteModeSet()
       {
          Log.d(LOGTAG, "FmRxEvMuteModeSet");
       }
       public void FmRxEvStereoModeSet()
       {
          Log.d(LOGTAG, "FmRxEvStereoModeSet");
       }
       public void FmRxEvRadioStationSet()
       {
          Log.d(LOGTAG, "FmRxEvRadioStationSet");
       }
       public void FmRxEvPowerModeSet()
       {
          Log.d(LOGTAG, "FmRxEvPowerModeSet");
       }
       public void FmRxEvSetSignalThreshold()
       {
          Log.d(LOGTAG, "FmRxEvSetSignalThreshold");
       }
 
       public void FmRxEvRadioTuneStatus(int frequency)
       {
          Log.d(LOGTAG, "FmRxEvRadioTuneStatus: Tuned Frequency: " +frequency);
          try
          {
             FmSharedPreferences.setTunedFrequency(frequency);
             mPrefs.Save();
             //Log.d(LOGTAG, "Call mCallbacks.onTuneStatusChanged");
             /* Since the Tuned Status changed, clear out the RDSData cached */
            if(mReceiver != null) {
               clearStationInfo();
            }
             mFMRxRDSData = null;
             if(mCallbacks != null)
             {
                mCallbacks.onTuneStatusChanged();
             }
             /* Update the frequency in the StatusBar's Notification */
             startNotification();
 
          }
          catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
 
       public void FmRxEvStationParameters()
       {
          Log.d(LOGTAG, "FmRxEvStationParameters");
       }
 
       public void FmRxEvRdsLockStatus(boolean bRDSSupported)
       {
          Log.d(LOGTAG, "FmRxEvRdsLockStatus: " + bRDSSupported);
          try
          {
             if(mCallbacks != null)
             {
                mCallbacks.onStationRDSSupported(bRDSSupported);
             }
          }
          catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
 
       public void FmRxEvStereoStatus(boolean stereo)
       {
          Log.d(LOGTAG, "FmRxEvStereoStatus: " + stereo);
          try
          {
             if(mCallbacks != null)
             {
                mCallbacks.onAudioUpdate(stereo);
             }
          }
          catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
       public void FmRxEvServiceAvailable(boolean signal)
       {
          Log.d(LOGTAG, "FmRxEvServiceAvailable");
          if(signal) {
              Log.d(LOGTAG, "FmRxEvServiceAvailable: Tuned frequency is above signal threshold level");
          }
          else {
              Log.d(LOGTAG, "FmRxEvServiceAvailable: Tuned frequency is below signal threshold level");
          }
       }
       public void FmRxEvGetSignalThreshold()
       {
          Log.d(LOGTAG, "FmRxEvGetSignalThreshold");
       }
       public void FmRxEvSearchInProgress()
       {
          Log.d(LOGTAG, "FmRxEvSearchInProgress");
       }
       public void FmRxEvSearchRdsInProgress()
       {
          Log.d(LOGTAG, "FmRxEvSearchRdsInProgress");
       }
       public void FmRxEvSearchListInProgress()
       {
          Log.d(LOGTAG, "FmRxEvSearchListInProgress");
       }
       public void FmRxEvSearchComplete(int frequency)
        {
          Log.d(LOGTAG, "FmRxEvSearchComplete: Tuned Frequency: " +frequency);
          try
          {
             FmSharedPreferences.setTunedFrequency(frequency);
             //Log.d(LOGTAG, "Call mCallbacks.onSearchComplete");
             /* Since the Tuned Status changed, clear out the RDSData cached */
             mFMRxRDSData = null;
             if(mCallbacks != null)
             {
                mCallbacks.onSearchComplete();
             }
             /* Update the frequency in the StatusBar's Notification */
             startNotification();
          }
          catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
 
       public void FmRxEvSearchRdsComplete()
       {
          Log.d(LOGTAG, "FmRxEvSearchRdsComplete");
       }
 
       public void FmRxEvSearchListComplete()
       {
          Log.d(LOGTAG, "FmRxEvSearchListComplete");
          try
          {
             if(mCallbacks != null)
             {
                mCallbacks.onSearchListComplete();
             }
          } catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
 
       public void FmRxEvSearchCancelled()
       {
          Log.d(LOGTAG, "FmRxEvSearchCancelled: Cancelled the on-going search operation.");
       }
       public void FmRxEvRdsGroupData()
       {
          Log.d(LOGTAG, "FmRxEvRdsGroupData");
       }
 
       public void FmRxEvRdsPsInfo() {
          Log.d(LOGTAG, "FmRxEvRdsPsInfo: ");
          try
          {
             if(mReceiver != null)
             {
                mFMRxRDSData = mReceiver.getPSInfo();
                if(mFMRxRDSData != null)
                {
                   Log.d(LOGTAG, "PI: [" + mFMRxRDSData.getPrgmId() + "]");
                   Log.d(LOGTAG, "PTY: [" + mFMRxRDSData.getPrgmType() + "]");
                   Log.d(LOGTAG, "PS: [" + mFMRxRDSData.getPrgmServices() + "]");
                }
                if(mCallbacks != null)
                {
                   mCallbacks.onProgramServiceChanged();
                }
             }
          } catch (RemoteException e)
          {
             e.printStackTrace();
          }
       }
 
       public void FmRxEvRdsRtInfo() {
          Log.d(LOGTAG, "FmRxEvRdsRtInfo");
          try
          {
             //Log.d(LOGTAG, "Call mCallbacks.onRadioTextChanged");
             if(mReceiver != null)
             {
                mFMRxRDSData = mReceiver.getRTInfo();
                if(mFMRxRDSData != null)
                {
                   Log.d(LOGTAG, "PI: [" + mFMRxRDSData.getPrgmId() + "]");
                   Log.d(LOGTAG, "PTY: [" + mFMRxRDSData.getPrgmType() + "]");
                   Log.d(LOGTAG, "RT: [" + mFMRxRDSData.getRadioText() + "]");
                }
                if(mCallbacks != null)
                {
                   mCallbacks.onRadioTextChanged();
                }
             }
          } catch (RemoteException e)
          {
             e.printStackTrace();
          }
 
       }
 
       public void FmRxEvRdsAfInfo()
       {
          Log.d(LOGTAG, "FmRxEvRdsAfInfo");
       }
       public void FmRxEvRdsPiMatchAvailable()
       {
          Log.d(LOGTAG, "FmRxEvRdsPiMatchAvailable");
       }
       public void FmRxEvRdsGroupOptionsSet()
       {
          Log.d(LOGTAG, "FmRxEvRdsGroupOptionsSet");
       }
       public void FmRxEvRdsProcRegDone()
       {
          Log.d(LOGTAG, "FmRxEvRdsProcRegDone");
       }
       public void FmRxEvRdsPiMatchRegDone()
       {
          Log.d(LOGTAG, "FmRxEvRdsPiMatchRegDone");
       }
    };
 
 
    /*
     *  Read the Tuned Frequency from the FM module.
     */
    private String getTunedFrequencyString() {
 
       double frequency = FmSharedPreferences.getTunedFrequency() / 1000.0;
       String frequencyString = getString(R.string.stat_notif_frequency, (""+frequency));
       return frequencyString;
    }
    public int getRssi() {
            return mReceiver.getRssi();
    }
    public int getIoC(){
            return mReceiver.getIoverc();
    }
    public int getIntDet(){
            return mReceiver.getIntDet();
    }
    public int getMpxDcc(){
            return mReceiver.getMpxDcc();
    }
    public void setHiLoInj(int inj){
            mReceiver.setHiLoInj(inj);
    }
    public int getSINR() {
            return mReceiver.getSINR();
    }
    //handling the sleep and record stop when FM App not in focus
    private void delayedStop(long duration, int nType) {
        int whatId = (nType == STOP_SERVICE) ? STOPSERVICE_ONSLEEP: STOPRECORD_ONTIMEOUT ;
        Message finished = mDelayedStopHandler.obtainMessage(whatId);
        mDelayedStopHandler.sendMessageDelayed(finished,duration);
    }
    private void cancelDelayedStop(int nType) {
        int whatId = (nType == STOP_SERVICE) ? STOPSERVICE_ONSLEEP: STOPRECORD_ONTIMEOUT ;
        mDelayedStopHandler.removeMessages(whatId);
    }
    private void requestFocus() {
       if( (false == mPlaybackInProgress) &&
           (true  == mStoppedOnFocusLoss) ) {
            // adding code for audio focus gain.
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_FM,
                   AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            startFM();
            mStoppedOnFocusLoss = false;
        }
    }
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mDelayedStopHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };
 }
