 package com.iceblaster;
 
 import android.app.Activity;
 import android.os.Bundle;
 
 public class IceblastAppActivity extends Activity {
	private IceblastApp iceblastApp;
     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.main);
         
        iceblastApp = new IceblastApp();
     }
 }
