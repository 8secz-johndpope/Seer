 // This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
 package de.schwardtnet.alienblaster;
 
 
 import android.app.Activity;
 import android.content.Context;
 import android.os.Bundle;
 import android.view.MotionEvent;
 import android.view.KeyEvent;
 import android.view.Window;
 import android.view.WindowManager;
 import android.media.AudioTrack;
 import android.media.AudioManager;
 import android.media.AudioFormat;
 import java.io.*;
 import java.nio.ByteBuffer;
 
 
 class AudioThread {
 
 	private Activity mParent;
 	private AudioTrack mAudio;
 	private byte[] mAudioBuffer;
 	private ByteBuffer mAudioBufferNative;
 
 	public AudioThread(Activity parent)
 	{
 		mParent = parent;
 		mAudio = null;
 		mAudioBuffer = null;
 		nativeAudioInitJavaCallbacks();
 	}
 	
 	public int fillBuffer()
 	{
 		mAudio.write( mAudioBuffer, 0, mAudioBuffer.length );
 		return 1;
 	}
 	
	public byte[] initAudio(int[] initParams)
 	{
 			if( mAudio == null )
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
 					mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
 												rate,
 												channels,
 												encoding,
 												bufSize,
 												AudioTrack.MODE_STREAM );
 					mAudio.play();
 			}
 			return mAudioBuffer;
 	}
 	
 	public int deinitAudio()
 	{
 		if( mAudio != null )
 		{
 			mAudio.stop();
 			mAudio.release();
 			mAudio = null;
 		}
 		mAudioBuffer = null;
 		return 1;
 	}
 	
 	private native int nativeAudioInitJavaCallbacks();
 }
 
