 package com.appnexus.opensdk;
 
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 
 import com.appnexus.opensdk.utils.Clog;
 import com.appnexus.opensdk.utils.HashingFunctions;
 import com.appnexus.opensdk.utils.Settings;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.provider.Settings.Secure;
 
 public class InstallTrackerPixel extends BroadcastReceiver{
 	
 	/* SET THIS TO YOUR PIXEL ID */
 	final String pid = "";
 	
 	BroadcastReceiver receiver_install;
 	Context context;
 	
 	
 	//Test with am broadcast -a com.android.vending.INSTALL_REFERRER --es "referrer" "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name"
 	//in adb
 	public InstallTrackerPixel(){
 		super();
 	}
 
 	@Override
 	public void onReceive(Context context, final Intent intent) {
 		this.context=context;
 		Clog.error_context=context;
 		Bundle extras = intent.getExtras();
 		
 		new PixelHttpTask(0).execute(extras);
 		//new Thread(new RequestRunnable(extras)).start();
 
 	}
 
 	private String getInstallUrl(String params){
 		String appid = null;
 		String hidmd5 = null;
 		String hidsha1 = null;
 		if(context!=null){
 			appid = context.getApplicationContext().getPackageName();
 			String aid = android.provider.Settings.Secure.getString(
 					context.getContentResolver(), Secure.ANDROID_ID);
 			// Get hidmd5, hidsha1, the devide ID hashed
 			hidmd5 = HashingFunctions.md5(aid);
 			hidsha1 = HashingFunctions.sha1(aid);
 		}
 		
 		StringBuilder urlBuilder = new StringBuilder(Settings.getSettings().INSTALL_BASE_URL);
 		urlBuilder.append(pid!=null && !pid.equals("")?"&id="+Uri.encode(pid):"");
 		urlBuilder.append(params!=null?params:"");
 		urlBuilder.append(appid!=null?"&appid="+Uri.encode(appid):"");
 		urlBuilder.append(hidmd5!=null?"&md5udid="+Uri.encode(hidmd5):"");
 		urlBuilder.append(hidsha1!=null?"&sha1udid="+Uri.encode(hidmd5):"");
 		
 		return urlBuilder.toString();
 	}
 	
 	private class PixelHttpTask extends AsyncTask<Bundle, Void, Boolean>{
 		
 		Bundle extras;
 		int delay;
 		
 		public PixelHttpTask(int delay){
 			super();
 			this.delay=delay;
 		}
 		
 		@Override
 		synchronized protected Boolean doInBackground(Bundle... params) {
 			
 			if(params == null || params.length<1 || params[0]==null){
 				Clog.d(Clog.baseLogTag, Clog.getString(R.string.conversion_pixel_fail));
 				return true; //Didn't really succeed but can't try again without proper bundle info
 			}
 			
 			if(delay>0){
 				try {
 					Thread.sleep(delay);
 				} catch (InterruptedException e1) {
 					//Just continue until time limit is met
 				}
 			}
 			extras=params[0];
 			String referralString = extras.getString("referrer");
 
 			String url = getInstallUrl(referralString);
 			
 			
 			Clog.d(Clog.baseLogTag, Clog.getString(R.string.conversion_pixel, url));
 			
 			try{
 				HttpClient client = new DefaultHttpClient();
 				HttpGet get = new HttpGet(url);
 				client.execute(get);
 			}catch(Exception e){
 				e.printStackTrace();
 				return false;
 			}
 			return true;
 		}
 		
 		@Override
 		protected void onPostExecute(Boolean succeeded){
 			if(succeeded){
 				Clog.d(Clog.baseLogTag, Clog.getString(R.string.conversion_pixel_success));
 				return;
 			}else{
 				// Wait 30 seconds and try, try again.
 				if(delay==0){
 					delay=30*1000;					
 				}else if(delay<300*1000){
 					delay=delay*2;
 				}else{
 					//Give up
 					Clog.d(Clog.baseLogTag, Clog.getString(R.string.conversion_pixel_fail));
 					return;
 				}
 				Clog.d(Clog.baseLogTag, Clog.getString(R.string.conversion_pixel_delay, delay));
				new PixelHttpTask(delay).execute(extras);
 			}
 		}		
 	}
 
 }
