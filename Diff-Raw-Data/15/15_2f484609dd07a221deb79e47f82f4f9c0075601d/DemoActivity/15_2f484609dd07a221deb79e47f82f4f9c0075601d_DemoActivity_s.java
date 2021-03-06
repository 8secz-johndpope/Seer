 package de.schwardtnet.alienblaster;
 
 import javax.microedition.khronos.egl.EGLConfig;
 import javax.microedition.khronos.opengles.GL10;
 
 import android.app.Activity;
 import android.content.Context;
 import android.opengl.GLSurfaceView;
 import android.os.Bundle;
 import android.view.MotionEvent;
 import android.view.KeyEvent;
 import android.view.Window;
 import android.view.WindowManager;
 import android.media.AudioTrack;
 import android.media.AudioManager;
 import android.media.AudioFormat;
 import android.os.PowerManager;
 import android.os.Vibrator;
 import android.hardware.SensorManager;
 import android.hardware.SensorListener;
 
 import android.widget.TextView;
 import org.apache.http.client.methods.*;
 import org.apache.http.*;
 import org.apache.http.impl.*;
 import org.apache.http.impl.client.*;
 import java.util.zip.*;
 import java.io.*;
 import java.nio.ByteBuffer;
 
 
// TODO: export vibrator to SDL - interface is available in SDL 1.3

 class Globals {
 	public static String ApplicationName = "AlienBlaster";
 	// Should be zip file
	public static String DataDownloadUrl = "http://sites.google.com/site/xpelyax/Home/alienblaster110_data.zip?attredirects=0&d=1";
 	// Set DownloadToSdcard to true if your app data is bigger than 5 megabytes.
 	// It will download app data to /sdcard/alienblaster then,
 	// otherwise it will download it to /data/data/de.schwardtnet.alienblaster/files -
 	// set this dir in jni/Android.mk in SDL_CURDIR_PATH
 	public static boolean DownloadToSdcard = false;
 }
 
 class LoadLibrary {
     public LoadLibrary() {}
     static {
         System.loadLibrary("stlport");
         System.loadLibrary("tremor");
         System.loadLibrary("sdl");
         System.loadLibrary("sdl_mixer");
         System.loadLibrary("application");
     }
 }
 
 
 // Accelerometer code partially ripped from http://karanar.net/
 class AccelerometerReader implements SensorListener {
 
     private long timekeeper;
 
     private float [] v;
 
     public AccelerometerReader(Activity context) {
         v = new float[3];
         SensorManager sma = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
         if( sma != null )
         {
             timekeeper = android.os.SystemClock.uptimeMillis();
             int mask = 0;
             mask |= SensorManager.SENSOR_ORIENTATION;
             mask |= SensorManager.SENSOR_ACCELEROMETER;
             sma.registerListener(this, mask, SensorManager.SENSOR_DELAY_GAME);
         }
     }
 
     public synchronized void onSensorChanged(int sensor, float[] values) {
         if (android.os.SystemClock.uptimeMillis() < timekeeper + 20) return;
         timekeeper = android.os.SystemClock.uptimeMillis();
 
         if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
             if( values.length >= 1 )
                 v[0] = values[0];
             if( values.length >= 2 )
                 v[1] = values[1];
             if( values.length >= 3 )
                 v[2] = values[2];
         }
 
     }
 
     public synchronized void onAccuracyChanged(int i, int i1) {
         /* @todo implement method */
     }
     
     public synchronized float[] readAccelerometer()
     {
         float [] ret = new float[3];
         ret[0] = v[0];
         ret[1] = v[1];
         ret[2] = v[2];
         return ret;
     };
 }
 
 
 
 class DemoRenderer implements GLSurfaceView.Renderer {
 
 	public DemoRenderer(Activity context)
 	{
 		super();
         accelerometer = new AccelerometerReader(context);
 	}
 	
     public void onSurfaceCreated(GL10 gl, EGLConfig config) {
         nativeInit();
     }
 
     public void onSurfaceChanged(GL10 gl, int w, int h) {
         //gl.glViewport(0, 0, w, h);
         nativeResize(w, h);
     }
 
     public void onDrawFrame(GL10 gl) {
         float [] f = accelerometer.readAccelerometer();
         nativeRender(f[0], f[1], f[2]);
     }
 
     public void exitApp() {
          nativeDone();
     };
 
     private static native void nativeInit();
     private static native void nativeResize(int w, int h);
     private static native void nativeRender(float accX, float accY, float accZ);
     private static native void nativeDone();
     private AccelerometerReader accelerometer = null;
 }
 
 class DemoGLSurfaceView extends GLSurfaceView {
     public DemoGLSurfaceView(Activity context) {
         super(context);
         mParent = context;
         mRenderer = new DemoRenderer(context);
         setRenderer(mRenderer);
     }
 
     @Override
     public boolean onTouchEvent(final MotionEvent event) 
     {
         // TODO: add multitouch support (added in Android 2.0 SDK)
         int action = -1;
         if( event.getAction() == MotionEvent.ACTION_DOWN )
         	action = 0;
         if( event.getAction() == MotionEvent.ACTION_UP )
         	action = 1;
         if( event.getAction() == MotionEvent.ACTION_MOVE )
         	action = 2;
         if (  action >= 0 ) {
             nativeMouse( (int)event.getX(), (int)event.getY(), action );
         }
         return true;
     }
 
      public void exitApp() {
          mRenderer.exitApp();
      };
 
 	@Override
 	public boolean onKeyDown(int keyCode, final KeyEvent event) {
          nativeKey( keyCode, 1 );
          return true;
      }
 	
 	@Override
 	public boolean onKeyUp(int keyCode, final KeyEvent event) {
          nativeKey( keyCode, 0 );
          return true;
      }
 
     DemoRenderer mRenderer;
     Activity mParent;
 
     public static native void nativeMouse( int x, int y, int action );
     public static native void nativeKey( int keyCode, int down );
 }
 
 class AudioThread extends Thread {
 
 	private Activity mParent;
 	private AudioTrack mAudio;
 	private byte[] mAudioBuffer;
 	private ByteBuffer mAudioBufferNative;
 
 	public AudioThread(Activity parent)
 	{
 		mParent = parent;
 		mAudio = null;
 		mAudioBuffer = null;
 		this.setPriority(Thread.MAX_PRIORITY);
 		this.start();
 	}
 	
 	@Override
 	public void run() 
 	{
 		while( !isInterrupted() )
 		{
 			if( mAudio == null )
 			{
 				int[] initParams = nativeAudioInit();
 				if( initParams == null )
 				{
 					try {
 						sleep(200);
 					} catch( java.lang.InterruptedException e ) { };
 				}
 				else
 				{
 					int rate = initParams[0];
 					int channels = initParams[1];
 					channels = ( channels == 1 ) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : 
 													AudioFormat.CHANNEL_CONFIGURATION_STEREO;
 					int encoding = initParams[2];
 					encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
 													AudioFormat.ENCODING_PCM_8BIT;
 					int bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
 					if( initParams[3] > bufSize )
 						bufSize = initParams[3];
 					mAudioBuffer = new byte[bufSize];
 					nativeAudioInit2(mAudioBuffer);
 					mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
 												rate,
 												channels,
 												encoding,
 												bufSize,
 												AudioTrack.MODE_STREAM );
 					mAudio.play();
 				}
 			}
 			else
 			{
 				int len = nativeAudioBufferLock();
 				if( len > 0 )
 					mAudio.write( mAudioBuffer, 0, len );
 				if( len < 0 )
 					break;
 				nativeAudioBufferUnlock();
 			}
 		}
 		if( mAudio != null )
 		{
 			mAudio.stop();
 			mAudio.release();
 			mAudio = null;
 		}
 	}
 	
 	private static native int[] nativeAudioInit();
 	private static native int nativeAudioInit2(byte[] buf);
 	private static native int nativeAudioBufferLock();
 	private static native int nativeAudioBufferUnlock();
 }
 
 
 public class DemoActivity extends Activity {
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         // fullscreen mode
         requestWindowFeature(Window.FEATURE_NO_TITLE);
         getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN); 
 
         TextView tv = new TextView(this);
         tv.setText("Initializing");
         setContentView(tv);
         downloader = new DataDownloader(this, tv);
     }
 
     public void initSDL()
     {
         mLoadLibraryStub = new LoadLibrary();
         mAudioThread = new AudioThread(this);
         mGLView = new DemoGLSurfaceView(this);
         setContentView(mGLView);
         // Receive keyboard events
         mGLView.setFocusableInTouchMode(true);
         mGLView.setFocusable(true);
         mGLView.requestFocus();
         PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, Globals.ApplicationName);
         wakeLock.acquire();
     }
 
     @Override
     protected void onPause() {
         // TODO: if application pauses it's screen is messed up
         if( wakeLock != null )
             wakeLock.release();
         super.onPause();
         if( mGLView != null )
             mGLView.onPause();
     }
 
     @Override
     protected void onResume() {
         if( wakeLock != null )
             wakeLock.acquire();
         super.onResume();
         if( mGLView != null )
             mGLView.onResume();
     }
 
     @Override
     protected void onStop() 
     {
         if( wakeLock != null )
             wakeLock.release();
         if( mAudioThread != null )
         {
             mAudioThread.interrupt();
             try {
                 mAudioThread.join();
             } catch( java.lang.InterruptedException e ) { };
         }
         if( mGLView != null )
             mGLView.exitApp();
         super.onStop();
         finish();
     }
 
 	@Override
 	public boolean onKeyDown(int keyCode, final KeyEvent event) {
 		// Overrides Back key to use in our app
          if( mGLView != null )
              mGLView.nativeKey( keyCode, 1 );
          if( keyCode == KeyEvent.KEYCODE_BACK && !downloader.DownloadComplete )
              onStop();
          return true;
      }
 	
 	@Override
 	public boolean onKeyUp(int keyCode, final KeyEvent event) {
          if( mGLView != null )
              mGLView.nativeKey( keyCode, 0 );
          return true;
      }
 
     private DemoGLSurfaceView mGLView = null;
     private LoadLibrary mLoadLibraryStub = null;
     private AudioThread mAudioThread = null;
     private PowerManager.WakeLock wakeLock = null;
     private DataDownloader downloader = null;
     
     
     
 class DataDownloader extends Thread
 {
 	class StatusWriter
 	{
 		private TextView Status;
 		private DemoActivity Parent;
 
 		public StatusWriter( TextView _Status, DemoActivity _Parent )
 		{
 			Status = _Status;
 			Parent = _Parent;
 		}
 		
 		public void setText(final String str)
 		{
 			class Callback implements Runnable
 			{
         		public TextView Status;
 	        	public String text;
 	        	public void run()
     	    	{
         			Status.setText(text);
         		}
 	        }
     	    Callback cb = new Callback();
     	    cb.text = new String(str);
     	    cb.Status = Status;
 			Parent.runOnUiThread(cb);
 		}
 		
 	}
 	public DataDownloader( DemoActivity _Parent, TextView _Status )
 	{
 		Parent = _Parent;
 		DownloadComplete = false;
 		Status = new StatusWriter( _Status, _Parent );
 		Status.setText( "Connecting to " + Globals.DataDownloadUrl );
 		this.start();
 	}
 
 	@Override
 	public void run() 
 	{
 	
 		String path = getOutFilePath("DownloadFinished.flag");
 		InputStream checkFile = null;
 		try {
 			checkFile = new FileInputStream( path );
 		} catch( FileNotFoundException e ) {
 		} catch( SecurityException e ) { };
 		if( checkFile != null )
 		{
 			Status.setText( "No need to download" );
 			DownloadComplete = true;
 			initParent();
 			return;
 		}
 		checkFile = null;
 		
 		// Create output directory
 		if( Globals.DownloadToSdcard )
 		{
 			try {
 				(new File( "/sdcard/" + Globals.ApplicationName )).mkdirs();
 			} catch( SecurityException e ) { };
 		}
 		else
 		{
 			try {
 				FileOutputStream dummy = Parent.openFileOutput( "dummy", Parent.MODE_WORLD_READABLE );
 				dummy.write(0);
 				dummy.flush();
 			} catch( FileNotFoundException e ) {
 			} catch( java.io.IOException e ) {};
 		}
 		
 		HttpGet request = new HttpGet(Globals.DataDownloadUrl);
 		request.addHeader("Accept", "*/*");
 		HttpResponse response = null;
 		try {
 			DefaultHttpClient client = new DefaultHttpClient();
 			client.getParams().setBooleanParameter("http.protocol.handle-redirects", true);
 			response = client.execute(request);
 		} catch (IOException e) { } ;
 		if( response == null )
 		{
 			Status.setText( "Error connecting to " + Globals.DataDownloadUrl );
 			return;
 		}
 
 		Status.setText( "Downloading data from " + Globals.DataDownloadUrl );
 		
 		ZipInputStream zip = null;
 		try {
 			zip = new ZipInputStream(response.getEntity().getContent());
 		} catch( java.io.IOException e ) {
 			Status.setText( "Error downloading data from " + Globals.DataDownloadUrl );
 			return;
 		}
 		
 		byte[] buf = new byte[1024];
 		
 		ZipEntry entry = null;
 
 		while(true)
 		{
 			entry = null;
 			try {
 				entry = zip.getNextEntry();
 			} catch( java.io.IOException e ) {
 				Status.setText( "Error downloading data from " + Globals.DataDownloadUrl );
 				return;
 			}
 			if( entry == null )
 				break;
 			if( entry.isDirectory() )
 			{
 				try {
 					(new File( getOutFilePath(entry.getName()) )).mkdirs();
 				} catch( SecurityException e ) { };
 				continue;
 			}
 			
 			OutputStream out = null;
 			path = getOutFilePath(entry.getName());
 			
 			try {
 				out = new FileOutputStream( path );
 			} catch( FileNotFoundException e ) {
 			} catch( SecurityException e ) { };
 			if( out == null )
 			{
 				Status.setText( "Error writing to " + path );
 				return;
 			}
 
 			Status.setText( "Writing file " + path );
 
 			try {
 				int len;
 				while ((len = zip.read(buf)) > 0)
 				{
 					out.write(buf, 0, len);
 				}
 				out.flush();
 			} catch( java.io.IOException e ) {
 				Status.setText( "Error writing file " + path );
 				return;
 			}
 
 		}
 
 		OutputStream out = null;
 		path = getOutFilePath("DownloadFinished.flag");
 		try {
 			out = new FileOutputStream( path );
 			out.write(0);
 			out.flush();
 		} catch( FileNotFoundException e ) {
 		} catch( SecurityException e ) {
 		} catch( java.io.IOException e ) {
 			Status.setText( "Error writing file " + path );
 			return;
 		};
 		
 		if( out == null )
 		{
 			Status.setText( "Error writing to " + path );
 			return;
 		}
 	
 		Status.setText( "Finished" );
 		DownloadComplete = true;
 		
 		initParent();
 	};
 	
 	private void initParent()
 	{
 		class Callback implements Runnable
 		{
        		public DemoActivity Parent;
         	public void run()
    	    	{
     	    		Parent.initSDL();
        		}
         }
    	    Callback cb = new Callback();
         cb.Parent = Parent;
 		Parent.runOnUiThread(cb);
 	}
 	
 	private String getOutFilePath(final String filename)
 	{
 		if( Globals.DownloadToSdcard )
 			return  "/sdcard/" + Globals.ApplicationName + "/" + filename;
 		return Parent.getFilesDir().getAbsolutePath() + "/" + filename;
 	};
 	
 	public boolean DownloadComplete;
 	public StatusWriter Status;
 	private DemoActivity Parent;
 }
 
 }
