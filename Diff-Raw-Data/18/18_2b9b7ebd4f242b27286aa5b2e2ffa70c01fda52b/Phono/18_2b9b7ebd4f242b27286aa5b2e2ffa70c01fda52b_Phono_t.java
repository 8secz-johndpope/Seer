 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.phono.android.phonegap;
 
 import java.net.SocketException;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 
 import com.phonegap.api.Plugin;
 import com.phonegap.api.PluginResult;
 import com.phonegap.api.PluginResult.Status;
 import com.phono.android.rtp.PhonoAudioShim;
 import com.phono.api.Codec;
 import com.phono.api.CodecList;
 import com.phono.android.audio.Log;
 import com.phono.android.audio.Play;
 import com.phono.api.Share;
 import com.phono.rtp.Endpoint;
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Hashtable;
 import java.util.Properties;
 
 /**
  *
  * @author tim
  */
 public class Phono extends Plugin {
 
     /** List of possible actions */
     public static final String LOG = "log";
     public static final String START = "start";
     public static final String STOP = "stop";
     public static final String CODECS = "codecs";
     public static final String SHARE = "share";
     public static final String PLAY = "play";
     public static final String GAIN = "gain";
     public static final String DIGIT = "digit";
     public static final String MUTE = "mute";
     public static final String ALLOCATEENDPOINT = "allocateEndpoint";
     public static final String FREEENDPOINT = "freeEndpoint";
     // now option names
     public static final String URI = "uri";
     public static final String AUTOPLAY = "autoplay";
     public static final String CODEC = "codec";
     public static final String LSRTP = "lsrtp";
     public static final String RSRTP = "rsrtp";
     public static final String VALUE = "value";
     public static final String DURATION = "duration";
     public static final String AUDIBLE = "audible";
 
     public static void log(String string) {
         Log.debug("Phono " + string);
        System.out.println("Phono "+string);

     }
     private Hashtable _endpoints;
     private PhonoAudioShim _audio;
     private CodecList _codecList;
 
     public Phono() {
 
         super();
         Log.setLevel(Log.DEBUG);
         _endpoints = new Hashtable();
         _audio = new PhonoAudioShim();
         _codecList = new CodecList(_audio);
         log("in new Constructor");
        System.out.println("in new Constructor");
     }
     /*
      * (non-Javadoc)
      *
      * @see com.phonegap.api.Plugin#execute(java.lang.String,
      * org.json.JSONArray, java.lang.String)
      */
     /*
      *
 
     // allocateEndPoint
     // arguments[0]-> success function which will get invoked with a the allocated local uri "rtp://ipaddress:port"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options will be ignored
      */
 
     boolean allocateEndpoint(JSONObject options, JSONObject reply) throws JSONException {
         boolean ret = false;
 
         synchronized (_endpoints) {
 
             Endpoint end;
             try {
                 end = Endpoint.allocate();
                 _endpoints.put(end.getLocalURI(), end);
                 reply.put(URI, end.getLocalURI());
                 ret = true;
             } catch (SocketException ex) {
                 Log.error("AllocateEndpoint, " + ex.getMessage());
             }
 
         }
         return ret;
 
     }
     /*
 
     // freeEndPoint
     // arguments[0]-> success function which will get invoked with a the deallocated local uri "rtp://ipaddress:port"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain a single entry 'uri' -> "rtp://ipaddress:port" as returned from allocate (or share)
      */
 
     boolean freeEndpoint(JSONObject options, JSONObject reply) throws JSONException {
         boolean ret = false;
         String uri = options.getString(URI);
         synchronized (_endpoints) {
             Endpoint end;
             end = (Endpoint) _endpoints.get(uri);
             if (end != null) {
                 end.release();
                 _endpoints.remove(end);
                 reply.put(URI, end.getLocalURI());
                 ret = true;
             }
         }
         return ret;
     }
     /*
 
     // share
     // arguments[0]-> success function which will get invoked with a the allocated local uri "rtp://ipaddress:port"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport:remoteipaddress:remoteport"
     //                  'autoplay' -> "YES" , if the share should start immediately
     //                  'codec' -> "ULAW" , or other supported codec from the list returned by codecs
      */
 
     boolean share(JSONObject options, JSONObject reply) throws JSONException {
         boolean ret = false;
         String uri = options.getString(URI);
         String autoplay = options.getString(AUTOPLAY);
         String codec = options.getString(CODEC);
         String lsrtp = options.getString(LSRTP);
         String rsrtp = options.getString(RSRTP);
         Codec cod = null;
         if (codec != null) {
             Codec[] cs = _codecList.getCodecs();
             for (int i = 0; i < cs.length; i++) {
                 if (codec.equals(cs[i].name)) {
                     cod = cs[i];
                     break;
                 }
             }
         }
         if ((uri != null) && (cod != null)) {
             boolean as = false;
             if (autoplay != null) {
                 as = "YES".equals(autoplay);
             }
 
             Share sh = share(uri, cod, as, lsrtp, rsrtp);
             if (sh != null) {
                 reply.put(URI, sh.getLocalURI());
                 ret = true;
             }
 
         } else {
             //error messages here
         }
         return ret;
     }
 
     private Share share(String uri, final Codec codec, boolean autoStart, String srtpPropsl, String srtpPropsr) {
         Share ret = null;
         Share s = null;
         Properties spl = null;
         Properties spr = null;
 
         if ((srtpPropsl != null) && (srtpPropsl.length() > 0)) {
             InputStream sr = new ByteArrayInputStream(srtpPropsl.getBytes());
 
             spl = new Properties();
             try {
                 spl.load(sr);
             } catch (IOException ex) {
                 Log.error("srtp Props invalid format" + ex.toString());
             }
         }
         if ((srtpPropsr != null) && (srtpPropsr.length() > 0)) {
             InputStream sr = new ByteArrayInputStream(srtpPropsl.getBytes());
             spr = new Properties();
             try {
                 spr.load(sr);
             } catch (IOException ex) {
                 Log.error("srtp Props invalid format" + ex.toString());
             }
         }
         Log.verb("in share() codec = " + codec.name);
         Log.verb("in share() uri = " + uri);
         try {
             s = new Share(uri, _audio, codec.pt, spl, spr);
 
             _audio.init(codec.iaxcn, 100);
 
             String luri = s.getLocalURI();
             synchronized (_endpoints) {
                 Endpoint e = (Endpoint) _endpoints.get(luri);
                 if (e != null) {
                     e.setShare(s);
                 } else {
                     e = new Endpoint(luri);
                     e.setShare(s);
                     Log.warn("Unexpected local endpoint used :" + luri);
                     _endpoints.put(luri, e);
 
                 }
             }
             // should check auto start here...
             if (autoStart) {
                 s.start();
             }
             ret = s; // only return the share if no exceptions ....
         } catch (Exception ex) {
             if (s != null) {
                 s.stop(); // minimal cleanup on errors.
             }
             Log.error(ex.toString());                // do something useful here....
         }
         return ret;
     }
     // play
     // arguments[0]-> success function which will get invoked with a url "http://somewhere.com/ringing.mp3"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "http://somewhere.com/ringing.mp3"
     //                  'autoplay' -> "YES" , if the play should start immediately
 
     boolean play(JSONObject options, JSONObject reply) throws JSONException {
         boolean ret = false;
         String uri = options.getString(URI);
         String autoplay = options.getString(AUTOPLAY);
         if (uri != null) {
             try {
                 boolean as = false;
                 if (autoplay != null) {
                     as = "YES".equals(autoplay);
                 }
                 Play p = new Play(uri);
                 if (p != null) {
                     _endpoints.put(uri, p);
                     if (as) {
                         p.start();
                     }
                     ret = true;
                 }
             } catch (Exception ex) {
                 Log.error("problem with playing :"+uri+" exception -> "+ex.getMessage());
             }
 
         }
         return ret;
     }
 
     // start
     // arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3"
     boolean start(JSONObject options, JSONObject reply) throws JSONException {
         boolean res = false;
         String uri = options.getString(URI);
         if (uri != null) {
             Endpoint e = (Endpoint) _endpoints.get(uri);
             Share s = e.getShare();
             if (s != null) {
                 s.start();
                 res = true;
             } else {
                 if (e instanceof Play) {
                     ((Play) e).start();
                     res = true;
                 }
             }
         }
         return res;
     }
     // stop
     // arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3"
 
     boolean stop(JSONObject options, JSONObject reply) throws JSONException {
         boolean res = false;
         String uri = options.getString(URI);
         if (uri != null) {
             Endpoint e = (Endpoint) _endpoints.get(uri);
             Share s = e.getShare();
             if (s != null) {
                 s.stop();
                 res = true;
             } else {
                 if (e instanceof Play) {
                     ((Play) e).stop();
                     res = true;
                 }
             }
         }
         return res;
     }
     // gain
     // arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3"
     //                  'value' -> "70.5" % gain
 
     boolean gain(JSONObject options, JSONObject reply) throws JSONException {
         boolean res = false;
         String uri = options.getString(URI);
         String value = options.getString(VALUE);
 
         if ((value != null) && (uri != null)) {
             float val = Float.parseFloat(value);
 
             Endpoint e = (Endpoint) _endpoints.get(uri);
             Share s = e.getShare();
             if (s != null) {
                 s.gain(val);
                 res = true;
             } else {
                 if (e instanceof Play) {
                     ((Play) e).gain(val);
                     res = true;
                 }
             }
         }
         return res;
     }
 
     // mute
     // arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3"
     //                  'value' -> "YES" or "NO"
     boolean mute(JSONObject options, JSONObject reply) throws JSONException {
         boolean res = false;
         String uri = options.getString(URI);
         String value = options.getString(VALUE);
 
         if ((value != null) && (uri != null)) {
             boolean val = value.equals("YES");
             Endpoint e = (Endpoint) _endpoints.get(uri);
             Share s = e.getShare();
             if (s != null) {
                 s.mute(val);
                 res = true;
             }
         }
         return res;
     }
 
     // digit
     // arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or
     // arguments[1]-> failure function which will get invoked with an error message.
     // options must contain the following key-value pairs (all strings):
     //                  'uri' -> "rtp://localipaddress:localport"
     //                  'digit' -> "0" 0-9 etc
     //                  'duration' -> "250" duration in milliseconds
     //                  'audible' -> "YES" or "NO"
     boolean digit(JSONObject options, JSONObject reply) throws JSONException {
         boolean res = false;
         String uri = options.getString(URI);
         String digit = options.getString(DIGIT);
         String duration = options.getString(DURATION);
         String audible = options.getString(AUDIBLE);
 
         if ((digit != null) && (uri != null) && (duration != null) && (audible != null)) {
             Endpoint e = (Endpoint) _endpoints.get(uri);
             Share s = e.getShare();
             if (s != null) {
                 int dur = Integer.parseInt(duration);
                 boolean aud = audible.equals("YES");
 
                 s.digit(digit, dur, aud);
                 res = true;
             }
         }
         return res;
     }
 
     // codecs
     // arguments[0]-> success function which will get invoked with a json string containing a sequence of supported codec objects.
     // arguments[1]-> failure function which will get invoked with an error message.
     // options is ignored
     //
     private boolean codecs(JSONObject options, JSONObject joret) throws JSONException {
         boolean ret = true;
         Codec[] cs = _codecList.getCodecs();
         JSONArray ja = new JSONArray();
         for (int i = 0; i < cs.length; i++) {
             JSONObject joc = new JSONObject();
             joc.put("name", cs[i].name);
             joc.put("rate", cs[i].rate);
             joc.put("ptype", cs[i].pt);
             ja.put(i, joc);
         }
         joret.put("codecs", ja);
         return ret;
     }
 
     @Override
     public PluginResult execute(String action, JSONArray data,
             String callbackId) {
         Log.debug("Plugin Called with action " + action + " and data " + data.toString());
         PluginResult result = null;
         JSONObject options = null;
 
         try {
             if (data != null) {
                 options = data.getJSONObject(0);
             }
             JSONObject joret = new JSONObject();
             boolean status = false;
             if (ALLOCATEENDPOINT.equals(action)) {
                 status = allocateEndpoint(options, joret);
             } else if (CODECS.equals(action)) {
                 status = codecs(options, joret);
             } else if (START.equals(action)) {
                 status = start(options, joret);
             } else if (STOP.equals(action)) {
                 status = stop(options, joret);
             } else if (SHARE.equals(action)) {
                 status = share(options, joret);
             } else if (PLAY.equals(action)) {
                 status = play(options, joret);
             } else if (MUTE.equals(action)) {
                 status = mute(options, joret);
             } else if (GAIN.equals(action)) {
                 status = gain(options, joret);
             } else if (DIGIT.equals(action)) {
                 status = digit(options, joret);
             } else if (FREEENDPOINT.equals(action)) {
                 status = freeEndpoint(options, joret);
             } else if (LOG.equals(action)) {
                 Log.debug(options.getString("message"));
                joret.put(VALUE, "logged");
                 status = true;
             }
             Log.debug("returning " + joret.toString());
            result = status ? new PluginResult(Status.OK, joret) : new PluginResult(Status.INVALID_ACTION);
         } catch (JSONException jsonEx) {
             Log.debug("Got JSON Exception " + jsonEx.getMessage());
             result = new PluginResult(Status.JSON_EXCEPTION);
         }
         Log.debug("result=" + result.getStatus() + " " + result.getJSONString());
         return result;
     }
 }
