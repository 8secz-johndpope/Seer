 package ru.redspell.lighttest;
 
 import android.app.Activity;
 import android.content.pm.ActivityInfo;
 import android.os.Bundle;
 import android.view.View;
 import android.widget.FrameLayout;
 import ru.redspell.lightning.LightView;
 import android.util.Log;
 
 import android.content.Intent;
 import com.facebook.android.*;
 import com.facebook.android.Facebook.*;
 import com.facebook.android.AsyncFacebookRunner.*;
 
 import java.io.FileNotFoundException;
 import java.net.MalformedURLException;
 import java.io.IOException;
 
 
 
 public class LightTest extends Activity
 {
 	private LightView lightView;
/*
     @Override
 	public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
 		lightView.fb.fb.authorizeCallback(requestCode, resultCode, data);
 	}
 	*/
 	//private FrameLayout lightViewParent = null;
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState)
 	{
 			Log.d("LIGHTTEST","onCreate!!!");
 			super.onCreate(savedInstanceState);
 			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
 			//lightViewParent = new FrameLayout(this);
 			lightView = new LightView(this);
 			//lightViewParent.addView(lightView);
 			Log.d("LIGHTTEST","view created");
 			setContentView(lightView);
 
 			/*
 			facebook.authorize(this, new String[] { "email", "publish_checkins"},  new DialogListener() {
 					@Override
 					public void onComplete(Bundle values) {
 						Log.d("LIGHTTEST", "onCOMPLETE"); 
 						updateStatus(values.getString(Facebook.TOKEN));
 					}
 
 					@Override
 					public void onFacebookError(FacebookError error) { Log.d("LIGHTTEST","fb_error:" + error.toString ());}
 
 					@Override
 					public void onError(DialogError e) { Log.d("LIGHTTEST", "error" + e.toString ());}
 
 					@Override
 					public void onCancel() {Log.d("LIGHTTEST", "onCANCEL");}
 			});
 			*/
 
 	}
 /*
 public void updateStatus(String accessToken){
         Bundle params = new Bundle();
         bundle.putString("message", "test update");
         params.putString(Facebook.TOKEN,accessToken);
         Log.d("LIGHTTEST","acces_token="+accessToken);
 				AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);
 
 				mAsyncRunner.request("me", params,  new RequestListener() {
 
 						@Override
 						public void onMalformedURLException(MalformedURLException e, Object state) {
 								// TODO Auto-generated method stub
 								Log.d("LIGHTTEST", "MALFORMED EXCEPTION" + e.toString ());
 						}
 
 						@Override
 						public void onIOException(IOException e, Object state) {
 								// TODO Auto-generated method stub
 								Log.d("LIGHTTEST", "IO EXCEPTION" + e.toString ());
 						}
 
 						@Override
 						public void onFileNotFoundException(FileNotFoundException e, Object state) {
 								// TODO Auto-generated method stub
 								Log.d("LIGHTTEST", "FILE NOT FOUND EXCEPTION" + e.toString ());
 
 						}
 
 						@Override
 						public void onFacebookError(FacebookError e, Object state) {
 								// TODO Auto-generated method stub
 
 								Log.d("LIGHTTEST", "FACEBOOK EXCEPTION" + e.toString ());
 						}
 
 						@Override
 						public void onComplete(String response, Object state) {
 								// TODO Auto-generated method stub
 								Log.d("LIGHTTEST", "RESPONES" + response);
 						}
 				}, null);
 }
 */
 
 
 	@Override
 		protected void onPause() {
 			Log.d("LIGHTNING","ON PAUSE");
 			lightView.onPause();
 			//lightView.setVisibility(View.GONE); 
 			//lightViewParent.removeView(lightView); 
 			super.onPause();
 		}
 
 
 	  @Override
 			protected void onResume() {
 				super.onResume();
 				lightView.onResume();
 			}
 
 		@Override
 			protected void onDestroy() {
 				Log.d("LIGHTNING","ON DESTROY");
 				lightView.onDestroy();
 				super.onDestroy();
 			}
 		/*
 		@Override 
 			public void onWindowFocusChanged(boolean hasFocus) { 
 				Log.d("LIGHTNING","onWindowFocusChanged");
 				if (hasFocus && lightView != null ) { //&& lightView.getVisibility() == View.GONE) { 
 					lightViewParent.addView(lightView); 
 					//lightView.setVisibility(View.VISIBLE); 
 				}
 				super.onWindowFocusChanged(hasFocus); 
 			}
 		*/
 
 		static {
 			System.loadLibrary("test");
 		}
 
 }
