 package com.github.klondike.android.campfire;
 
 import android.app.Activity;
 import android.app.Dialog;
 import android.app.ListActivity;
 import android.app.ProgressDialog;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Bundle;
 import android.os.Handler;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.BaseAdapter;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.LinearLayout;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.github.klondike.java.campfire.Campfire;
 import com.github.klondike.java.campfire.CampfireException;
 import com.github.klondike.java.campfire.Room;
 import com.github.klondike.java.campfire.RoomEvent;
 
 public class RoomView extends ListActivity {
 	private static final int MENU_PREFS = 0;
 	private static final int MENU_LOGOUT = 1;
 
 	private static final int JOINING = 0;
 	private static final int SPEAKING = 1;
 	private static final int POLLING = 2;
 
 	private Campfire campfire;
 	private String roomId;
 	private Room room;
 	private RoomEvent[] events = new RoomEvent[20];
 	private RoomEvent[] newEvents;
 	
 	private EditText message;
 	private Button speak, refresh;
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.room);
 		
 		roomId = getIntent().getStringExtra("room_id");
 		
 		verifyLogin();
 	}
 	
 	// Will only happen after we are definitely logged in, 
 	// and the campfire member variable has been loaded with a logged-in Campfire
 	private void onLogin() {
 		joinRoom();
 	}
 	
 	// Will only happen after we are both logged in and the room has been joined
 	private void onJoined() {
 		setupControls();
 	}
 	
 	private void onPoll() {
 		events = newEvents;
 		loadEvents();
 	}
 	
 	private void setupControls() {
 		//TODO still:
 		// set name of room in window title
 		// populate original messages (forthcoming)
 		
 		message = (EditText) this.findViewById(R.id.room_message);
 		message.setEnabled(true);
 		
 		speak = (Button) this.findViewById(R.id.room_speak);
 		speak.setEnabled(true);
 		speak.setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				String msg = message.getText().toString();
 				if (!msg.equals("")) {
 					speak.setEnabled(false);
 					speak(msg);
 				}
 			}
 		});
 		
 		refresh = (Button) this.findViewById(R.id.room_refresh);
 		refresh.setEnabled(true);
 		refresh.setOnClickListener(new View.OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				poll();
 			}
 		});
		
		getListView().setFocusable(false);
 	}
 	
 	private void loadEvents() {
 		setListAdapter(new RoomAdapter(this, events));
 	}
 	
 	final Handler handler = new Handler();
 	final Runnable joinSuccess = new Runnable() {
 		public void run() {
 			removeDialog(JOINING);
 			onJoined();
 		}
 	};
 	final Runnable joinFailure = new Runnable() {
 		public void run() {
 			removeDialog(JOINING);
 			alert("Couldn't join room. Select the Re-join menu option to try again.");
 		}
 	};
 	
 	final Runnable speakSuccess = new Runnable() {
 		public void run() {
 			alert("Posted to Campfire.");
 			removeDialog(SPEAKING);
 			
 			message.setText("");
 			speak.setEnabled(true);
 		}
 	};
 	final Runnable speakError = new Runnable() {
 		public void run() {
 			alert("Connection error.");
 			removeDialog(SPEAKING);
 			
 			speak.setEnabled(true);
 		}
 	};
 	
 	final Runnable pollSuccess = new Runnable() {
 		public void run() {
 			removeDialog(POLLING);
 			onPoll();
 		}
 	};
 	
 	final Runnable pollFailure = new Runnable() {
 		public void run() {
 			removeDialog(POLLING);
 			alert("Connection error.");
 		}
 	};
 	
 	private void speak(String message) {
 		final String msg = message;
 		Thread speakThread = new Thread() {
 			public void run() {
 				try {
 					if (room.speak(msg))
 						handler.post(speakSuccess);
 					else
 						handler.post(speakError);
 				} catch (CampfireException e) {
 					handler.post(speakError);
 				}
 			}
 		};
 		speakThread.start();
 		showDialog(SPEAKING);
 	}
 
 	private void joinRoom() {
 		Thread joinThread = new Thread() {
 			public void run() {
 				room = new Room(campfire, roomId);
 				try {
 					if (room.join())
 						handler.post(joinSuccess);
 					else
 						handler.post(joinFailure);
 				} catch(CampfireException e) {
 					handler.post(joinFailure);
 				}
 			}
 		};
 		joinThread.start();
 		
 		showDialog(JOINING);
 	}
 	
 	private void poll() {
 		Thread pollThread = new Thread() {
 			public void run() {
 				try {
 					newEvents = room.listen();
 					handler.post(pollSuccess);
 					
 				} catch(CampfireException e) {
 					handler.post(pollFailure);
 				}
 			}
 		};
 		pollThread.start();
 		
 		showDialog(POLLING);
 	}
 	
 	private void verifyLogin() {
 		campfire = Login.getCampfire(this);
         if (campfire.loggedIn())
         	onLogin();
         else
         	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
     }
 	
 	@Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     	switch (requestCode) {
     	case Login.RESULT_LOGIN:
     		if (resultCode == RESULT_OK) {
     			alert("You have been logged in successfully.");
     			campfire = Login.getCampfire(this);
     			onLogin();
     		} else
     			finish();
     	}
     }
 	
 	@Override 
     public boolean onCreateOptionsMenu(Menu menu) { 
 	    boolean result = super.onCreateOptionsMenu(menu);
 	    
         menu.add(0, MENU_PREFS, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
         menu.add(0, MENU_LOGOUT, 0, "Log Out").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
         
         return result;
     }
     
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
     	switch(item.getItemId()) { 
     	case MENU_PREFS:
     		startActivity(new Intent(this, Preferences.class)); 
     		return true;
     	case MENU_LOGOUT:
     		getSharedPreferences("campfire", 0).edit().putString("session", null).commit();
     		finish();
     	}
     	return super.onOptionsItemSelected(item);
     }
     
     protected Dialog onCreateDialog(int id) {
     	ProgressDialog loadingDialog = new ProgressDialog(this);
     	loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         switch(id) {
         case SPEAKING:
             loadingDialog.setMessage("Speaking...");
             return loadingDialog;
         case JOINING:
         	loadingDialog.setMessage("Joining room...");
         	loadingDialog.setCancelable(false);
             return loadingDialog;
         case POLLING:
         	loadingDialog.setMessage("Polling...");
         	return loadingDialog;
         default:
             return null;
         }
     }
 	
 	public void alert(String msg) {
 		Toast.makeText(RoomView.this, msg, Toast.LENGTH_SHORT).show();
 	}
 	
 	protected class RoomAdapter extends BaseAdapter {
     	private RoomEvent[] events;
     	LayoutInflater inflater;
 
         public RoomAdapter(Activity context, RoomEvent[] events) {
             this.events = events;
             inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         }
 
 		public int getCount() {
 			return events.length;
 		}
 
 		public Object getItem(int position) {
 			return events[position];
 		}
 
 		public long getItemId(int position) {
 			return ((long) position);
 		}
 
 		public View getView(int position, View convertView, ViewGroup parent) {
 			RoomEvent item = (RoomEvent) getItem(position);
 			
 			LinearLayout view;
 			if (convertView == null) {
 				view = (LinearLayout) inflater.inflate(viewForType(item.type), null);
 			} else {
 				view = (LinearLayout) convertView;
 			}
 			
 			((TextView) view.findViewById(R.id.text)).setText(item.message);
 			
 			if (item.type != RoomEvent.TIMESTAMP) {
 				((TextView) view.findViewById(R.id.person)).setText(item.person);
 			}
 			
 			return view;
 		}
 		
 		public int viewForType(int type) {
 			switch (type) {
 			case RoomEvent.TEXT:
 				return R.layout.event_text;
 			case RoomEvent.TIMESTAMP:
 				return R.layout.event_timestamp;
 			default:
 				return R.layout.event_text;
 			}
 		}
 
     }
 }
