 package br.com.upinterativo.upstopwatch;
 
 import java.util.Timer;
 import java.util.TimerTask;
 
 import android.annotation.SuppressLint;
 import android.app.Activity;
 import android.content.Intent;
 import android.os.Bundle;
 import android.view.GestureDetector;
 import android.view.GestureDetector.SimpleOnGestureListener;
 import android.view.Menu;
 import android.view.MotionEvent;
 import android.view.View;
 import android.widget.LinearLayout;
 import android.widget.TextView;
 
 public class MainStopwatchActivity extends Activity {
 
 	private GestureDetector gd;
 	private LinearLayout layoutButton;
 	private boolean isRunning;
 	private Timer timer;
 	private long elapsedTime;
 	
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main_stopwatch);
         gd = new GestureDetector(this, simpleGestureDetector);
         isRunning = false;
         timer = new Timer();
         elapsedTime = 0;
         
         timer.scheduleAtFixedRate(new TimerTask() {
 			
 			@Override
 			public void run() {
 				if(isRunning){
 					runOnUiThread(new Runnable() {
 						
 						@Override
 						public void run() {
							elapsedTime += 10;
 							TextView milis = (TextView)findViewById(R.id.text_view_mili);
 							TextView seconds = (TextView)findViewById(R.id.text_view_seconds);
 							TextView minutes = (TextView)findViewById(R.id.text_view_minutes);
 							int milisecs = (int) (elapsedTime % 1000);
 							int secs = (int) (elapsedTime/1000) % 60;
 							int min = (int) (elapsedTime/(60*1000));
 							
							milis.setText(String.valueOf(milisecs));
							seconds.setText(String.valueOf(secs));
							minutes.setText(String.valueOf(min));
 						}
 					});
 				}
 				
 			}
 		}, 0, 10);
         
         layoutButton = (LinearLayout)findViewById(R.id.watchstop_circle_button);
         layoutButton.setOnClickListener(new View.OnClickListener() {
 			
 			@SuppressLint("NewApi")
 			@Override
 			public void onClick(View v) {
 				if(!isRunning)
 				{
 					layoutButton.setBackgroundResource(R.drawable.watchstop_background_green);
 					isRunning = true;
 				} else {
 					layoutButton.setBackgroundResource(R.drawable.watchstop_background);
 					isRunning = false;
 				}
 				
 			}
 		});
         
         layoutButton.setOnLongClickListener(new View.OnLongClickListener() {
 			
 			@Override
 			public boolean onLongClick(View v) {
 				if(!isRunning){
 					elapsedTime = 0;
 					TextView milis = (TextView)findViewById(R.id.text_view_mili);
 					TextView seconds = (TextView)findViewById(R.id.text_view_seconds);
 					TextView minutes = (TextView)findViewById(R.id.text_view_minutes);
 					milis.setText(R.string.init_miliseconds);
 					seconds.setText(R.string.init_second);
 					minutes.setText(R.string.init_minute);
 					return true;
 				}
 				return false;
 			}
 		});
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         // Inflate the menu; this adds items to the action bar if it is present.
     	getMenuInflater().inflate(R.menu.activity_main_stopwatch, menu);
         return true;
     }
     
     @Override
     public boolean onTouchEvent(MotionEvent event) {
     	// TODO Auto-generated method stub
     	return gd.onTouchEvent(event);
     }
 
     SimpleOnGestureListener simpleGestureDetector = new SimpleOnGestureListener(){
     	
 		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
 				float velocityY) {
 			int dist = (int) (e1.getX() - e2.getX());
 			if(velocityX < - 5000 && dist > 200){
 				startActivity(new Intent(getApplicationContext(), Settings.class));
 			}
 			return false;
 		}
     };
 }
