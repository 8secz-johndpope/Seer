 package org.digitalcampus.mobile.learning.task;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.math.BigInteger;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.security.DigestInputStream;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 
 import org.apache.http.client.ClientProtocolException;
 import org.digitalcampus.mobile.learning.R;
 import org.digitalcampus.mobile.learning.application.MobileLearning;
 import org.digitalcampus.mobile.learning.listener.DownloadMediaListener;
 import org.digitalcampus.mobile.learning.model.DownloadProgress;
 import org.digitalcampus.mobile.learning.model.Media;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.os.AsyncTask;
 import android.preference.PreferenceManager;
 import android.util.Log;
 
 import com.bugsense.trace.BugSenseHandler;
 
 public class DownloadMediaTask extends AsyncTask<Payload, DownloadProgress, Payload>{
 
 	public final static String TAG = DownloadMediaTask.class.getSimpleName();
 	private DownloadMediaListener mStateListener;
 	private Context ctx;
 	private SharedPreferences prefs;
 	
 	public DownloadMediaTask(Context ctx) {
 		this.ctx = ctx;
 		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
 	}
 	
 	@Override
 	protected Payload doInBackground(Payload... params) {
 		Payload payload = params[0];
 		for (Object o: payload.data){
 			Media m = (Media) o;
 			try { 
 				
 				URL u = new URL(m.getDownloadUrl());
                 HttpURLConnection c = (HttpURLConnection) u.openConnection();
                 c.setRequestMethod("GET");
                 c.setDoOutput(true);
                 c.connect();
                 c.setConnectTimeout(Integer.parseInt(prefs.getString("prefServerTimeoutConnection",
 								ctx.getString(R.string.prefServerTimeoutConnection))));
                 c.setReadTimeout(Integer.parseInt(prefs.getString("prefServerTimeoutResponse",
 								ctx.getString(R.string.prefServerTimeoutResponse))));
                 
                 int fileLength = c.getContentLength();
 				
                 DownloadProgress dp = new DownloadProgress();
 				dp.setMessage(m.getFilename());
 				dp.setProgress(0);
 				publishProgress(dp);
 				
 				File file = new File(MobileLearning.MEDIA_PATH,m.getFilename());
 				FileOutputStream f = new FileOutputStream(file);
 				InputStream in = c.getInputStream();
 				
 				MessageDigest md = MessageDigest.getInstance("MD5");
 				in = new DigestInputStream(in, md);
 				
                 byte[] buffer = new byte[4096];
                 int len1 = 0;
                 long total = 0;
                 int progress = 0;
                 while ((len1 = in.read(buffer)) > 0) {
                     total += len1; 
                     progress = (int)(total*100)/fileLength;
                     if(progress > 0){
 	                    dp.setProgress(progress);
 	                    publishProgress(dp);
                     }
                     f.write(buffer, 0, len1);
                 }
                 f.close();
 				
 				dp.setProgress(100);
 				publishProgress(dp);
				
 				// check the file digest matches, otherwise delete the file 
 				// (it's either been a corrupted download or it's the wrong file)
 				byte[] digest = md.digest();
				String resultMD5 = "";
				
				for (int i=0; i < digest.length; i++) {
					resultMD5 += Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
			    }
				
 				Log.d(TAG,"supplied   digest: " + m.getDigest());
				Log.d(TAG,"calculated digest: " + resultMD5);
				
				// TODO - this should be changed to .equals once the updated modules have been published and users 
				// are using the updated module versions
				if(!resultMD5.contains(m.getDigest())){
 					if (file.exists() && !file.isDirectory()){
 				        file.delete();
 				    }
 				}
 			} catch (ClientProtocolException e1) { 
 				e1.printStackTrace(); 
 			} catch (IOException e1) { 
 				e1.printStackTrace();
 			} catch (NoSuchAlgorithmException e) {
 				BugSenseHandler.sendException(e);
 				e.printStackTrace();
 			}
 			
 			
 		}
 		return payload;
 	}
 	
 	@Override
 	protected void onProgressUpdate(DownloadProgress... obj) {
 		synchronized (this) {
             if (mStateListener != null) {
                 mStateListener.downloadProgressUpdate(obj[0]);
             }
         }
 	}
 	
 	@Override
 	protected void onPostExecute(Payload results) {
 		synchronized (this) {
             if (mStateListener != null) {
                mStateListener.downloadComplete();
             }
         }
 	}
 	
 	public void setDownloadListener(DownloadMediaListener srl) {
         synchronized (this) {
             mStateListener = srl;
         }
     }
 
 }
