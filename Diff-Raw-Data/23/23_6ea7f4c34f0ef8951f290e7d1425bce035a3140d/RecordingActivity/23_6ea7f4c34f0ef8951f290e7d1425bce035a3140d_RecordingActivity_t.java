 package org.sfcta.cycletracks;
 
 import java.text.SimpleDateFormat;
 import java.util.TimeZone;
 
 import android.app.Activity;
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.os.Bundle;
 import android.os.IBinder;
 import android.view.View;
 import android.widget.Button;
 import android.widget.TextView;
 import android.widget.Toast;
 
 public class RecordingActivity extends Activity {
 	Intent fi;
 	TripData trip;
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.recording);
 
 		// If service is idle, start recording!
 		Intent rService = new Intent(this, RecordingService.class);
 		startService(rService);
 		ServiceConnection sc = new ServiceConnection() {
 			public void onServiceDisconnected(ComponentName name) {}
 			public void onServiceConnected(ComponentName name, IBinder service) {
 				IRecordService rs = (IRecordService) service;
 				if (rs.getState() == RecordingService.STATE_IDLE) {
 					trip = TripData.createTrip(RecordingActivity.this);
 					rs.startRecording(trip);
 				} else {
 					long id = rs.getCurrentTrip();
 					trip = TripData.fetchTrip(RecordingActivity.this, id);
 				}
 				rs.setListener(RecordingActivity.this);
 				unbindService(this);
 			}
 		};
 		bindService(rService, sc, Context.BIND_AUTO_CREATE);
 
 /*
 		// Pause button
 		final Button pauseButton = (Button) findViewById(R.id.ButtonPause);
 		pauseButton.setOnClickListener(new View.OnClickListener() {
 			public void onClick(View v) {
 				if (isRecording) {
 					// TripData.get().killListener();
 					pauseButton.setText("Resume");
 					RecordingActivity.this.setTitle("CycleTracks - Paused...");
 					Toast.makeText(getBaseContext(),"Recording paused; GPS now offline", Toast.LENGTH_LONG).show();
 				} else {
 					// TripData.get().activateListener();
 					pauseButton.setText("Pause");
 					RecordingActivity.this.setTitle("CycleTracks - Recording...");
 					Toast.makeText(getBaseContext(),"GPS restarted. It may take a moment to resync.", Toast.LENGTH_LONG).show();
 				}
 				isRecording = !isRecording;
 			}
 		});
 */
 		// Finish button
 		final Button finishButton = (Button) findViewById(R.id.ButtonFinished);
 		finishButton.setOnClickListener(new View.OnClickListener() {
 			public void onClick(View v) {
 				// If we have points, go to the save-trip activity
 				if (trip.dirty) {
 					// Save trip so far (points and extent, but no purpose or notes)
 					fi = new Intent(RecordingActivity.this, SaveTrip.class);
 					trip.updateTrip("","","","");
 				}
 				// Otherwise, cancel and go back to main screen
 				else {
 					Toast.makeText(getBaseContext(),"No GPS data acquired; nothing to submit.", Toast.LENGTH_SHORT).show();
 
 					cancelRecording();
 
 			    	// Go back to main screen
 					fi = new Intent(RecordingActivity.this, MainInput.class);
 					fi.putExtra("keep", true);
 				}
 
 				// Either way, activate next task, and then kill this task
 				startActivity(fi);
 				RecordingActivity.this.finish();
 			}
 		});
 	}
 
 	public void updateStatus(int points, float distance, double duration, float spdCurrent, float spdMax) {
         final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
         sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
 
 	    //TODO: check task status before doing this
         TextView txtStat = (TextView) findViewById(R.id.TextRecordStats);
         TextView txtDistance = (TextView) findViewById(R.id.TextDistance);
         TextView txtDuration = (TextView) findViewById(R.id.TextDuration);
         TextView txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
         TextView txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);
 
         txtStat.setText(""+points+" data points received...");
         txtCurSpeed.setText(String.format("Current speed: %1.1f mph", spdCurrent));
         txtMaxSpeed.setText(String.format("Maximum speed: %1.1f mph", spdMax));
 
         txtDuration.setText(String.format("Time Elapsed: %ss", sdf.format(duration)));
 
         // Distance funky!
     	float miles = 0.0006212f * distance;
    	txtDistance.setText(String.format("Distance traveled: %1.1f miles", miles));
 	}
 
 	void cancelRecording() {
 		Intent rService = new Intent(this, RecordingService.class);
 		ServiceConnection sc = new ServiceConnection() {
 			public void onServiceDisconnected(ComponentName name) {}
 			public void onServiceConnected(ComponentName name, IBinder service) {
 				IRecordService rs = (IRecordService) service;
 				rs.cancelRecording();
 				unbindService(this);
 			}
 		};
 		// This should block until the onServiceConnected (above) completes.
 		bindService(rService, sc, Context.BIND_AUTO_CREATE);
 	}
 }
