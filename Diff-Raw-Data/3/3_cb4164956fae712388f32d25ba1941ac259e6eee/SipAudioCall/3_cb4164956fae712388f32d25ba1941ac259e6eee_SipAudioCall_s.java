 /*
  * Copyright (C) 2010 The Android Open Source Project
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
 
 package android.net.sip;
 
 import android.content.Context;
 import android.media.AudioManager;
 import android.media.Ringtone;
 import android.media.RingtoneManager;
 import android.media.ToneGenerator;
 import android.net.Uri;
 import android.net.rtp.AudioCodec;
 import android.net.rtp.AudioGroup;
 import android.net.rtp.AudioStream;
 import android.net.rtp.RtpStream;
 import android.net.sip.SimpleSessionDescription.Media;
 import android.net.wifi.WifiManager;
 import android.os.Message;
 import android.os.RemoteException;
 import android.os.Vibrator;
 import android.provider.Settings;
 import android.util.Log;
 
 import java.io.IOException;
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Class that handles an audio call over SIP.
  */
 /** @hide */
 public class SipAudioCall extends SipSessionAdapter {
     private static final String TAG = SipAudioCall.class.getSimpleName();
     private static final boolean RELEASE_SOCKET = true;
     private static final boolean DONT_RELEASE_SOCKET = false;
     private static final int SESSION_TIMEOUT = 5; // in seconds
 
     /** Listener class for all event callbacks. */
     public static class Listener {
         /**
          * Called when the call object is ready to make another call.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that is ready to make another call
          */
         public void onReadyToCall(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when a request is sent out to initiate a new call.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onCalling(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when a new call comes in.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          * @param caller the SIP profile of the caller
          */
         public void onRinging(SipAudioCall call, SipProfile caller) {
             onChanged(call);
         }
 
         /**
          * Called when a RINGING response is received for the INVITE request
          * sent. The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onRingingBack(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when the session is established.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onCallEstablished(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when the session is terminated.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onCallEnded(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when the peer is busy during session initialization.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onCallBusy(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when the call is on hold.
          * The default implementation calls {@link #onChange}.
          *
          * @param call the call object that carries out the audio call
          */
         public void onCallHeld(SipAudioCall call) {
             onChanged(call);
         }
 
         /**
          * Called when an error occurs. The default implementation is no op.
          *
          * @param call the call object that carries out the audio call
          * @param errorCode error code of this error
          * @param errorMessage error message
          * @see SipErrorCode
          */
         public void onError(SipAudioCall call, int errorCode,
                 String errorMessage) {
             // no-op
         }
 
         /**
          * Called when an event occurs and the corresponding callback is not
          * overridden. The default implementation is no op. Error events are
          * not re-directed to this callback and are handled in {@link #onError}.
          */
         public void onChanged(SipAudioCall call) {
             // no-op
         }
     }
 
     private Context mContext;
     private SipProfile mLocalProfile;
     private SipAudioCall.Listener mListener;
     private SipSession mSipSession;
 
     private long mSessionId = System.currentTimeMillis();
     private String mPeerSd;
 
     private AudioStream mAudioStream;
     private AudioGroup mAudioGroup;
 
     private boolean mInCall = false;
     private boolean mMuted = false;
     private boolean mHold = false;
 
     private boolean mRingbackToneEnabled = true;
     private boolean mRingtoneEnabled = true;
     private Ringtone mRingtone;
     private ToneGenerator mRingbackTone;
 
     private SipProfile mPendingCallRequest;
     private WifiManager mWm;
     private WifiManager.WifiLock mWifiHighPerfLock;
 
     private int mErrorCode = SipErrorCode.NO_ERROR;
     private String mErrorMessage;
 
     /**
      * Creates a call object with the local SIP profile.
      * @param context the context for accessing system services such as
      *        ringtone, audio, WIFI etc
      */
     public SipAudioCall(Context context, SipProfile localProfile) {
         mContext = context;
         mLocalProfile = localProfile;
         mWm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
     }
 
     /**
      * Sets the listener to listen to the audio call events. The method calls
      * {@code setListener(listener, false)}.
      *
      * @param listener to listen to the audio call events of this object
      * @see #setListener(Listener, boolean)
      */
     public void setListener(SipAudioCall.Listener listener) {
         setListener(listener, false);
     }
 
     /**
      * Sets the listener to listen to the audio call events. A
      * {@link SipAudioCall} can only hold one listener at a time. Subsequent
      * calls to this method override the previous listener.
      *
      * @param listener to listen to the audio call events of this object
      * @param callbackImmediately set to true if the caller wants to be called
      *      back immediately on the current state
      */
     public void setListener(SipAudioCall.Listener listener,
             boolean callbackImmediately) {
         mListener = listener;
         try {
             if ((listener == null) || !callbackImmediately) {
                 // do nothing
             } else if (mErrorCode != SipErrorCode.NO_ERROR) {
                 listener.onError(this, mErrorCode, mErrorMessage);
             } else if (mInCall) {
                 if (mHold) {
                     listener.onCallHeld(this);
                 } else {
                     listener.onCallEstablished(this);
                 }
             } else {
                 int state = getState();
                 switch (state) {
                     case SipSession.State.READY_TO_CALL:
                         listener.onReadyToCall(this);
                         break;
                     case SipSession.State.INCOMING_CALL:
                         listener.onRinging(this, getPeerProfile());
                         break;
                     case SipSession.State.OUTGOING_CALL:
                         listener.onCalling(this);
                         break;
                     case SipSession.State.OUTGOING_CALL_RING_BACK:
                         listener.onRingingBack(this);
                         break;
                 }
             }
         } catch (Throwable t) {
             Log.e(TAG, "setListener()", t);
         }
     }
 
     /**
      * Checks if the call is established.
      *
      * @return true if the call is established
      */
     public synchronized boolean isInCall() {
         return mInCall;
     }
 
     /**
      * Checks if the call is on hold.
      *
      * @return true if the call is on hold
      */
     public synchronized boolean isOnHold() {
         return mHold;
     }
 
     /**
      * Closes this object. This object is not usable after being closed.
      */
     public void close() {
         close(true);
     }
 
     private synchronized void close(boolean closeRtp) {
         if (closeRtp) stopCall(RELEASE_SOCKET);
         stopRingbackTone();
         stopRinging();
 
         mInCall = false;
         mHold = false;
         mSessionId = System.currentTimeMillis();
         mErrorCode = SipErrorCode.NO_ERROR;
         mErrorMessage = null;
 
         if (mSipSession != null) {
             mSipSession.setListener(null);
             mSipSession = null;
         }
     }
 
     /**
      * Gets the local SIP profile.
      *
      * @return the local SIP profile
      */
     public synchronized SipProfile getLocalProfile() {
         return mLocalProfile;
     }
 
     /**
      * Gets the peer's SIP profile.
      *
      * @return the peer's SIP profile
      */
     public synchronized SipProfile getPeerProfile() {
         return (mSipSession == null) ? null : mSipSession.getPeerProfile();
     }
 
     /**
      * Gets the state of the {@link SipSession} that carries this call.
      * The value returned must be one of the states in {@link SipSession.State}.
      *
      * @return the session state
      */
     public synchronized int getState() {
         if (mSipSession == null) return SipSession.State.READY_TO_CALL;
         return mSipSession.getState();
     }
 
 
     /**
      * Gets the {@link SipSession} that carries this call.
      *
      * @return the session object that carries this call
      * @hide
      */
     public synchronized SipSession getSipSession() {
         return mSipSession;
     }
 
     private SipSession.Listener createListener() {
         return new SipSession.Listener() {
             @Override
             public void onCalling(SipSession session) {
                 Log.d(TAG, "calling... " + session);
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         listener.onCalling(SipAudioCall.this);
                     } catch (Throwable t) {
                         Log.i(TAG, "onCalling(): " + t);
                     }
                 }
             }
 
             @Override
             public void onRingingBack(SipSession session) {
                 Log.d(TAG, "sip call ringing back: " + session);
                 if (!mInCall) startRingbackTone();
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         listener.onRingingBack(SipAudioCall.this);
                     } catch (Throwable t) {
                         Log.i(TAG, "onRingingBack(): " + t);
                     }
                 }
             }
 
             @Override
             public synchronized void onRinging(SipSession session,
                     SipProfile peerProfile, String sessionDescription) {
                 if ((mSipSession == null) || !mInCall
                         || !session.getCallId().equals(mSipSession.getCallId())) {
                     // should not happen
                     session.endCall();
                     return;
                 }
 
                 // session changing request
                 try {
                     String answer = createAnswer(sessionDescription).encode();
                     mSipSession.answerCall(answer, SESSION_TIMEOUT);
                 } catch (Throwable e) {
                     Log.e(TAG, "onRinging()", e);
                     session.endCall();
                 }
             }
 
             @Override
             public void onCallEstablished(SipSession session,
                     String sessionDescription) {
                 stopRingbackTone();
                 stopRinging();
                 mPeerSd = sessionDescription;
                 Log.v(TAG, "onCallEstablished()" + mPeerSd);
 
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         if (mHold) {
                             listener.onCallHeld(SipAudioCall.this);
                         } else {
                             listener.onCallEstablished(SipAudioCall.this);
                         }
                     } catch (Throwable t) {
                         Log.i(TAG, "onCallEstablished(): " + t);
                     }
                 }
             }
 
             @Override
             public void onCallEnded(SipSession session) {
                 Log.d(TAG, "sip call ended: " + session);
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         listener.onCallEnded(SipAudioCall.this);
                     } catch (Throwable t) {
                         Log.i(TAG, "onCallEnded(): " + t);
                     }
                 }
                 close();
             }
 
             @Override
             public void onCallBusy(SipSession session) {
                 Log.d(TAG, "sip call busy: " + session);
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         listener.onCallBusy(SipAudioCall.this);
                     } catch (Throwable t) {
                         Log.i(TAG, "onCallBusy(): " + t);
                     }
                 }
                 close(false);
             }
 
             @Override
             public void onCallChangeFailed(SipSession session, int errorCode,
                     String message) {
                 Log.d(TAG, "sip call change failed: " + message);
                 mErrorCode = errorCode;
                 mErrorMessage = message;
                 Listener listener = mListener;
                 if (listener != null) {
                     try {
                         listener.onError(SipAudioCall.this, mErrorCode,
                                 message);
                     } catch (Throwable t) {
                         Log.i(TAG, "onCallBusy(): " + t);
                     }
                 }
             }
 
             @Override
             public void onError(SipSession session, int errorCode,
                     String message) {
                 SipAudioCall.this.onError(errorCode, message);
             }
 
             @Override
             public void onRegistering(SipSession session) {
                 // irrelevant
             }
 
             @Override
             public void onRegistrationTimeout(SipSession session) {
                 // irrelevant
             }
 
             @Override
             public void onRegistrationFailed(SipSession session, int errorCode,
                     String message) {
                 // irrelevant
             }
 
             @Override
             public void onRegistrationDone(SipSession session, int duration) {
                 // irrelevant
             }
         };
     }
 
     private void onError(int errorCode, String message) {
         Log.d(TAG, "sip session error: "
                 + SipErrorCode.toString(errorCode) + ": " + message);
         mErrorCode = errorCode;
         mErrorMessage = message;
         Listener listener = mListener;
         if (listener != null) {
             try {
                 listener.onError(this, errorCode, message);
             } catch (Throwable t) {
                 Log.i(TAG, "onError(): " + t);
             }
         }
         synchronized (this) {
             if ((errorCode == SipErrorCode.DATA_CONNECTION_LOST)
                     || !isInCall()) {
                 close(true);
             }
         }
     }
 
     /**
      * Attaches an incoming call to this call object.
      *
      * @param session the session that receives the incoming call
      * @param sessionDescription the session description of the incoming call
      * @throws SipException if the SIP service fails to attach this object to
      *        the session
      */
     public synchronized void attachCall(SipSession session,
             String sessionDescription) throws SipException {
         mSipSession = session;
         mPeerSd = sessionDescription;
         Log.v(TAG, "attachCall()" + mPeerSd);
         try {
             session.setListener(createListener());
 
             if (getState() == SipSession.State.INCOMING_CALL) startRinging();
         } catch (Throwable e) {
             Log.e(TAG, "attachCall()", e);
             throwSipException(e);
         }
     }
 
     /**
      * Initiates an audio call to the specified profile. The attempt will be
      * timed out if the call is not established within {@code timeout} seconds
      * and {@code Listener.onError(SipAudioCall, SipErrorCode.TIME_OUT, String)}
      * will be called.
      *
      * @param callee the SIP profile to make the call to
      * @param sipManager the {@link SipManager} object to help make call with
      * @param timeout the timeout value in seconds. Default value (defined by
      *        SIP protocol) is used if {@code timeout} is zero or negative.
      * @see Listener.onError
      * @throws SipException if the SIP service fails to create a session for the
      *        call
      */
     public synchronized void makeCall(SipProfile peerProfile,
         SipManager sipManager, int timeout) throws SipException {
         SipSession s = mSipSession = sipManager.createSipSession(
                 mLocalProfile, createListener());
         if (s == null) {
             throw new SipException(
                     "Failed to create SipSession; network available?");
         }
         try {
             mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
             s.makeCall(peerProfile, createOffer().encode(), timeout);
         } catch (IOException e) {
             throw new SipException("makeCall()", e);
         }
     }
 
     /**
      * Ends a call.
      * @throws SipException if the SIP service fails to end the call
      */
     public synchronized void endCall() throws SipException {
         stopRinging();
         stopCall(RELEASE_SOCKET);
         mInCall = false;
 
         // perform the above local ops first and then network op
         if (mSipSession != null) mSipSession.endCall();
     }
 
     /**
      * Puts a call on hold.  When succeeds, {@link Listener#onCallHeld} is
      * called. The attempt will be timed out if the call is not established
      * within {@code timeout} seconds and
      * {@code Listener.onError(SipAudioCall, SipErrorCode.TIME_OUT, String)}
      * will be called.
      *
      * @param timeout the timeout value in seconds. Default value (defined by
      *        SIP protocol) is used if {@code timeout} is zero or negative.
      * @see Listener.onError
      * @throws SipException if the SIP service fails to hold the call
      */
     public synchronized void holdCall(int timeout) throws SipException {
         if (mHold) return;
         mSipSession.changeCall(createHoldOffer().encode(), timeout);
         mHold = true;
 
         AudioGroup audioGroup = getAudioGroup();
         if (audioGroup != null) audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
     }
 
     /**
      * Answers a call. The attempt will be timed out if the call is not
      * established within {@code timeout} seconds and
      * {@code Listener.onError(SipAudioCall, SipErrorCode.TIME_OUT, String)}
      * will be called.
      *
      * @param timeout the timeout value in seconds. Default value (defined by
      *        SIP protocol) is used if {@code timeout} is zero or negative.
      * @see Listener.onError
      * @throws SipException if the SIP service fails to answer the call
      */
     public synchronized void answerCall(int timeout) throws SipException {
         stopRinging();
         try {
             mAudioStream = new AudioStream(InetAddress.getByName(getLocalIp()));
             mSipSession.answerCall(createAnswer(mPeerSd).encode(), timeout);
         } catch (IOException e) {
             throw new SipException("answerCall()", e);
         }
     }
 
     /**
      * Continues a call that's on hold. When succeeds,
      * {@link Listener#onCallEstablished} is called. The attempt will be timed
      * out if the call is not established within {@code timeout} seconds and
      * {@code Listener.onError(SipAudioCall, SipErrorCode.TIME_OUT, String)}
      * will be called.
      *
      * @param timeout the timeout value in seconds. Default value (defined by
      *        SIP protocol) is used if {@code timeout} is zero or negative.
      * @see Listener.onError
      * @throws SipException if the SIP service fails to unhold the call
      */
     public synchronized void continueCall(int timeout) throws SipException {
         if (!mHold) return;
         mSipSession.changeCall(createContinueOffer().encode(), timeout);
         mHold = false;
         AudioGroup audioGroup = getAudioGroup();
         if (audioGroup != null) audioGroup.setMode(AudioGroup.MODE_NORMAL);
     }
 
     private SimpleSessionDescription createOffer() {
         SimpleSessionDescription offer =
                 new SimpleSessionDescription(mSessionId, getLocalIp());
         AudioCodec[] codecs = AudioCodec.getCodecs();
         Media media = offer.newMedia(
                 "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
         for (AudioCodec codec : AudioCodec.getCodecs()) {
             media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
         }
         media.setRtpPayload(127, "telephone-event/8000", "0-15");
         return offer;
     }
 
     private SimpleSessionDescription createAnswer(String offerSd) {
         SimpleSessionDescription offer =
                 new SimpleSessionDescription(offerSd);
         SimpleSessionDescription answer =
                 new SimpleSessionDescription(mSessionId, getLocalIp());
         AudioCodec codec = null;
         for (Media media : offer.getMedia()) {
             if ((codec == null) && (media.getPort() > 0)
                     && "audio".equals(media.getType())
                     && "RTP/AVP".equals(media.getProtocol())) {
                 // Find the first audio codec we supported.
                 for (int type : media.getRtpPayloadTypes()) {
                     codec = AudioCodec.getCodec(type, media.getRtpmap(type),
                             media.getFmtp(type));
                     if (codec != null) {
                         break;
                     }
                 }
                 if (codec != null) {
                     Media reply = answer.newMedia(
                             "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
                     reply.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
 
                     // Check if DTMF is supported in the same media.
                     for (int type : media.getRtpPayloadTypes()) {
                         String rtpmap = media.getRtpmap(type);
                         if ((type != codec.type) && (rtpmap != null)
                                 && rtpmap.startsWith("telephone-event")) {
                             reply.setRtpPayload(
                                     type, rtpmap, media.getFmtp(type));
                         }
                     }
 
                     // Handle recvonly and sendonly.
                     if (media.getAttribute("recvonly") != null) {
                         answer.setAttribute("sendonly", "");
                     } else if(media.getAttribute("sendonly") != null) {
                         answer.setAttribute("recvonly", "");
                     } else if(offer.getAttribute("recvonly") != null) {
                         answer.setAttribute("sendonly", "");
                     } else if(offer.getAttribute("sendonly") != null) {
                         answer.setAttribute("recvonly", "");
                     }
                     continue;
                 }
             }
             // Reject the media.
             Media reply = answer.newMedia(
                     media.getType(), 0, 1, media.getProtocol());
             for (String format : media.getFormats()) {
                 reply.setFormat(format, null);
             }
         }
         if (codec == null) {
             throw new IllegalStateException("Reject SDP: no suitable codecs");
         }
         return answer;
     }
 
     private SimpleSessionDescription createHoldOffer() {
         SimpleSessionDescription offer = createContinueOffer();
         offer.setAttribute("sendonly", "");
         return offer;
     }
 
     private SimpleSessionDescription createContinueOffer() {
         SimpleSessionDescription offer =
                 new SimpleSessionDescription(mSessionId, getLocalIp());
         Media media = offer.newMedia(
                 "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
         AudioCodec codec = mAudioStream.getCodec();
         media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
         int dtmfType = mAudioStream.getDtmfType();
         if (dtmfType != -1) {
             media.setRtpPayload(dtmfType, "telephone-event/8000", "0-15");
         }
         return offer;
     }
 
     private void grabWifiHighPerfLock() {
         if (mWifiHighPerfLock == null) {
             Log.v(TAG, "acquire wifi high perf lock");
             mWifiHighPerfLock = ((WifiManager)
                     mContext.getSystemService(Context.WIFI_SERVICE))
                     .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);
             mWifiHighPerfLock.acquire();
         }
     }
 
     private void releaseWifiHighPerfLock() {
         if (mWifiHighPerfLock != null) {
             Log.v(TAG, "release wifi high perf lock");
             mWifiHighPerfLock.release();
             mWifiHighPerfLock = null;
         }
     }
 
     private boolean isWifiOn() {
         return (mWm.getConnectionInfo().getBSSID() == null) ? false : true;
     }
 
     /** Toggles mute. */
     public synchronized void toggleMute() {
         AudioGroup audioGroup = getAudioGroup();
         if (audioGroup != null) {
             audioGroup.setMode(
                     mMuted ? AudioGroup.MODE_NORMAL : AudioGroup.MODE_MUTED);
             mMuted = !mMuted;
         }
     }
 
     /**
      * Checks if the call is muted.
      *
      * @return true if the call is muted
      */
     public synchronized boolean isMuted() {
         return mMuted;
     }
 
     /** Puts the device to speaker mode. */
     public synchronized void setSpeakerMode(boolean speakerMode) {
         ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                 .setSpeakerphoneOn(speakerMode);
     }
 
     /**
      * Sends a DTMF code. According to RFC2833, event 0--9 maps to decimal
      * value 0--9, '*' to 10, '#' to 11, event 'A'--'D' to 12--15, and event
      * flash to 16. Currently, event flash is not supported.
      *
      * @param code the DTMF code to send. Value 0 to 15 (inclusive) are valid
      *        inputs.
      * @see http://tools.ietf.org/html/rfc2833
      */
     public void sendDtmf(int code) {
         sendDtmf(code, null);
     }
 
     /**
      * Sends a DTMF code. According to RFC2833, event 0--9 maps to decimal
      * value 0--9, '*' to 10, '#' to 11, event 'A'--'D' to 12--15, and event
      * flash to 16. Currently, event flash is not supported.
      *
      * @param code the DTMF code to send. Value 0 to 15 (inclusive) are valid
      *        inputs.
      * @param result the result message to send when done
      */
     public synchronized void sendDtmf(int code, Message result) {
         AudioGroup audioGroup = getAudioGroup();
         if ((audioGroup != null) && (mSipSession != null)
                 && (SipSession.State.IN_CALL == getState())) {
             Log.v(TAG, "send DTMF: " + code);
             audioGroup.sendDtmf(code);
         }
         if (result != null) result.sendToTarget();
     }
 
     /**
      * Gets the {@link AudioStream} object used in this call. The object
      * represents the RTP stream that carries the audio data to and from the
      * peer. The object may not be created before the call is established. And
      * it is undefined after the call ends or the {@link #close} method is
      * called.
      *
      * @return the {@link AudioStream} object or null if the RTP stream has not
      *      yet been set up
      * @hide
      */
     public synchronized AudioStream getAudioStream() {
         return mAudioStream;
     }
 
     /**
      * Gets the {@link AudioGroup} object which the {@link AudioStream} object
      * joins. The group object may not exist before the call is established.
      * Also, the {@code AudioStream} may change its group during a call (e.g.,
      * after the call is held/un-held). Finally, the {@code AudioGroup} object
      * returned by this method is undefined after the call ends or the
      * {@link #close} method is called. If a group object is set by
      * {@link #setAudioGroup(AudioGroup)}, then this method returns that object.
      *
      * @return the {@link AudioGroup} object or null if the RTP stream has not
      *      yet been set up
      * @see #getAudioStream
      * @hide
      */
     public synchronized AudioGroup getAudioGroup() {
         if (mAudioGroup != null) return mAudioGroup;
         return ((mAudioStream == null) ? null : mAudioStream.getGroup());
     }
 
     /**
      * Sets the {@link AudioGroup} object which the {@link AudioStream} object
      * joins. If {@code audioGroup} is null, then the {@code AudioGroup} object
      * will be dynamically created when needed.
      *
      * @see #getAudioStream
      * @hide
      */
     public synchronized void setAudioGroup(AudioGroup group) {
         if ((mAudioStream != null) && (mAudioStream.getGroup() != null)) {
             mAudioStream.join(group);
         }
         mAudioGroup = group;
     }
 
     /**
      * Starts the audio for the established call. This method should be called
      * after {@link Listener#onCallEstablished} is called.
      */
     public void startAudio() {
         try {
             startAudioInternal();
         } catch (UnknownHostException e) {
             onError(SipErrorCode.PEER_NOT_REACHABLE, e.getMessage());
         } catch (Throwable e) {
             onError(SipErrorCode.CLIENT_ERROR, e.getMessage());
         }
     }
 
     private synchronized void startAudioInternal() throws UnknownHostException {
         if (mPeerSd == null) {
             Log.v(TAG, "startAudioInternal() mPeerSd = null");
             throw new IllegalStateException("mPeerSd = null");
         }
 
         stopCall(DONT_RELEASE_SOCKET);
         mInCall = true;
 
         // Run exact the same logic in createAnswer() to setup mAudioStream.
         SimpleSessionDescription offer =
                 new SimpleSessionDescription(mPeerSd);
         AudioStream stream = mAudioStream;
         AudioCodec codec = null;
         for (Media media : offer.getMedia()) {
             if ((codec == null) && (media.getPort() > 0)
                     && "audio".equals(media.getType())
                     && "RTP/AVP".equals(media.getProtocol())) {
                 // Find the first audio codec we supported.
                 for (int type : media.getRtpPayloadTypes()) {
                     codec = AudioCodec.getCodec(
                             type, media.getRtpmap(type), media.getFmtp(type));
                     if (codec != null) {
                         break;
                     }
                 }
 
                 if (codec != null) {
                     // Associate with the remote host.
                     String address = media.getAddress();
                     if (address == null) {
                         address = offer.getAddress();
                     }
                     stream.associate(InetAddress.getByName(address),
                             media.getPort());
 
                     stream.setDtmfType(-1);
                     stream.setCodec(codec);
                     // Check if DTMF is supported in the same media.
                     for (int type : media.getRtpPayloadTypes()) {
                         String rtpmap = media.getRtpmap(type);
                         if ((type != codec.type) && (rtpmap != null)
                                 && rtpmap.startsWith("telephone-event")) {
                             stream.setDtmfType(type);
                         }
                     }
 
                     // Handle recvonly and sendonly.
                     if (mHold) {
                         stream.setMode(RtpStream.MODE_NORMAL);
                     } else if (media.getAttribute("recvonly") != null) {
                         stream.setMode(RtpStream.MODE_SEND_ONLY);
                     } else if(media.getAttribute("sendonly") != null) {
                         stream.setMode(RtpStream.MODE_RECEIVE_ONLY);
                     } else if(offer.getAttribute("recvonly") != null) {
                         stream.setMode(RtpStream.MODE_SEND_ONLY);
                     } else if(offer.getAttribute("sendonly") != null) {
                         stream.setMode(RtpStream.MODE_RECEIVE_ONLY);
                     } else {
                         stream.setMode(RtpStream.MODE_NORMAL);
                     }
                     break;
                 }
             }
         }
         if (codec == null) {
             throw new IllegalStateException("Reject SDP: no suitable codecs");
         }
 
         if (isWifiOn()) grabWifiHighPerfLock();
 
         if (!mHold) {
             /* The recorder volume will be very low if the device is in
              * IN_CALL mode. Therefore, we have to set the mode to NORMAL
              * in order to have the normal microphone level.
              */
             ((AudioManager) mContext.getSystemService
                     (Context.AUDIO_SERVICE))
                     .setMode(AudioManager.MODE_NORMAL);
         }
 
         // AudioGroup logic:
         AudioGroup audioGroup = getAudioGroup();
         if (mHold) {
             if (audioGroup != null) {
                 audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
             }
             // don't create an AudioGroup here; doing so will fail if
             // there's another AudioGroup out there that's active
         } else {
             if (audioGroup == null) audioGroup = new AudioGroup();
             stream.join(audioGroup);
             if (mMuted) {
                 audioGroup.setMode(AudioGroup.MODE_MUTED);
             } else {
                 audioGroup.setMode(AudioGroup.MODE_NORMAL);
             }
         }
     }
 
     private void stopCall(boolean releaseSocket) {
         Log.d(TAG, "stop audiocall");
         releaseWifiHighPerfLock();
         if (mAudioStream != null) {
             mAudioStream.join(null);
 
             if (releaseSocket) {
                 mAudioStream.release();
                 mAudioStream = null;
             }
         }
     }
 
     private String getLocalIp() {
         return mSipSession.getLocalIp();
     }
 
 
     /**
      * Enables/disables the ring-back tone.
      *
      * @param enabled true to enable; false to disable
      */
     public synchronized void setRingbackToneEnabled(boolean enabled) {
         mRingbackToneEnabled = enabled;
     }
 
     /**
      * Enables/disables the ring tone.
      *
      * @param enabled true to enable; false to disable
      */
     public synchronized void setRingtoneEnabled(boolean enabled) {
         mRingtoneEnabled = enabled;
     }
 
     private void startRingbackTone() {
         if (!mRingbackToneEnabled) return;
         if (mRingbackTone == null) {
             // The volume relative to other sounds in the stream
             int toneVolume = 80;
             mRingbackTone = new ToneGenerator(
                     AudioManager.STREAM_VOICE_CALL, toneVolume);
         }
         mRingbackTone.startTone(ToneGenerator.TONE_CDMA_LOW_PBX_L);
     }
 
     private void stopRingbackTone() {
         if (mRingbackTone != null) {
             mRingbackTone.stopTone();
             mRingbackTone.release();
             mRingbackTone = null;
         }
     }
 
     private void startRinging() {
         if (!mRingtoneEnabled) return;
         ((Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE))
                 .vibrate(new long[] {0, 1000, 1000}, 1);
         AudioManager am = (AudioManager)
                 mContext.getSystemService(Context.AUDIO_SERVICE);
         if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {
             String ringtoneUri =
                     Settings.System.DEFAULT_RINGTONE_URI.toString();
             mRingtone = RingtoneManager.getRingtone(mContext,
                     Uri.parse(ringtoneUri));
             mRingtone.play();
         }
     }
 
     private void stopRinging() {
         ((Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE))
                 .cancel();
         if (mRingtone != null) mRingtone.stop();
     }
 
     private void throwSipException(Throwable throwable) throws SipException {
         if (throwable instanceof SipException) {
             throw (SipException) throwable;
         } else {
             throw new SipException("", throwable);
         }
     }
 
     private SipProfile getPeerProfile(SipSession session) {
         return session.getPeerProfile();
     }
 }
