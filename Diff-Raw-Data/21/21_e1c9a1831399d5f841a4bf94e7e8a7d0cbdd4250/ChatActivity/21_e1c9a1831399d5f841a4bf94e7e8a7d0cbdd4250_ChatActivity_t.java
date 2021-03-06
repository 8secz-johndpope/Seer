 package com.trellmor.BerryTubeChat;
 
 import java.net.MalformedURLException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 
 import com.trellmor.BerryTube.BerryTube;
 import com.trellmor.BerryTube.BerryTubeBinder;
 import com.trellmor.BerryTube.BerryTubeCallback;
 import com.trellmor.BerryTube.ChatMessage;
 import com.trellmor.BerryTube.ChatUser;
 import com.trellmor.BerryTube.Poll;
 
 import android.os.Build;
 import android.os.Bundle;
 import android.os.IBinder;
 import android.preference.PreferenceManager;
 import android.annotation.SuppressLint;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.ComponentName;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.content.SharedPreferences;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.WindowManager;
 import android.view.inputmethod.EditorInfo;
 import android.widget.EditText;
 import android.widget.ListView;
 import android.widget.TextView;
 
 public class ChatActivity extends Activity {
 
 	private ChatMessageAdapter mChatAdapter = null;
 	private ListView mListChat;
 	private TextView mTextNick;
 	private EditText mEditChatMsg;
 	private TextView mTextDrinks;
 
 	private BerryTubeBinder mBinder = null;
 	private boolean mServiceConnected = false;
 	private ServiceConnection mService = new ServiceConnection() {
 
 		@Override
 		public void onServiceDisconnected(ComponentName name) {
 			mServiceConnected = false;
 		}
 
 		@Override
 		public void onServiceConnected(ComponentName name, IBinder service) {
 			mServiceConnected = true;
 			mBinder = (BerryTubeBinder) service;
 			mBinder.getService().registerCallback(mCallback);
			
			mBinder.getService().setChatMsgBufferSize(mScrollback);
			
			mChatAdapter = new ChatMessageAdapter(ChatActivity.this, R.layout.chat_item,
					mBinder.getService().getChatMsgBuffer());
			mListChat.setAdapter(mChatAdapter);
			
 			if (mBinder.getService().isConnected()) {
 				setNick(mBinder.getService().getNick());
 				mDrinkCount = mBinder.getService().getDrinkCount();
 				updateDrinkCount();
 			} else {
 				try {
 					mBinder.getService().connect(Username, Password);
 				} catch (MalformedURLException e) {
 					Log.w(ChatActivity.class.toString(), e);
 				}
 			}
 		}
 	};
 
 	private String Username = "";
 	private String Password = "";
 	private String mNick = "";
 
 	public String getNick() {
 		return mNick;
 	}
 
 	protected void setNick(String nick) {
 		if (nick != null) {
 			mNick = nick;
 			mEditChatMsg.setEnabled(true);
 		} else {
 			mNick = "Anonymous";
 			mEditChatMsg.setEnabled(false);
 		}
 
 		mTextNick.setText(mNick);
 		if (mChatAdapter != null)
 			mChatAdapter.setNick(nick);
 	}
 
 	private int mScrollback = 100;
 	private int mDrinkCount = 0;
 	private int mMyDrinkCount = 0;
 	private boolean mShowDrinkCount = true;
 	private boolean mPopupPoll = false;
 	private BerryTubeCallback mCallback = null;
 	private boolean mLogout = false;
 
 	@SuppressLint("NewApi")
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_chat);
 		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
 			getActionBar().setDisplayHomeAsUpEnabled(false);
 		}
 
 		mEditChatMsg = (EditText) findViewById(R.id.edit_chat_msg);
 		TextView.OnEditorActionListener chatMsgListener = new TextView.OnEditorActionListener() {
 			@Override
 			public boolean onEditorAction(TextView v, int actionId,
 					KeyEvent event) {
 				if (actionId == EditorInfo.IME_ACTION_SEND) {
 					sendChatMsg();
 				}
 				return true;
 			}
 		};
 		mEditChatMsg.setOnEditorActionListener(chatMsgListener);
 
 		mTextDrinks = (TextView) findViewById(R.id.text_drinks);
 		mTextNick = (TextView) findViewById(R.id.text_nick);
 		mTextNick.setText("Anonymous");
 
 		getConfigurationInstance();
 		mListChat = (ListView) findViewById(R.id.list_chat);
 
 		Intent intent = getIntent();
 		Username = intent.getStringExtra(MainActivity.KEY_USERNAME);
 		Password = intent.getStringExtra(MainActivity.KEY_PASSWORD);
 
 		createCallback();
 
 		Intent serviceIntent = new Intent(this, BerryTube.class);
 		startService(serviceIntent);
 		bindService(serviceIntent, mService, BIND_ABOVE_CLIENT);
 	}
 
 	@Override
 	public void onStart() {
 		super.onStart();
 
 		loadPreferences();
 		
 		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 	}
 
 	@Override
 	public void onStop() {
 		super.onStop();
 		
 		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 	}
 
 	@Override
 	public void onDestroy() {
 		if (mBinder != null) {
 			mBinder.getService().unregisterCallback(mCallback);
 			mCallback = null;
 		}
 		if (mServiceConnected)
 			unbindService(mService);
 
 		super.onDestroy();
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		getMenuInflater().inflate(R.menu.activity_chat, menu);
 		return super.onCreateOptionsMenu(menu);
 	}
 
 	@Override
 	public void onBackPressed() {
 		Intent backtoHome = new Intent(Intent.ACTION_MAIN);
 		backtoHome.addCategory(Intent.CATEGORY_HOME);
 		backtoHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 		startActivity(backtoHome);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		Intent intent = null;
 
 		switch (item.getItemId()) {
 		case R.id.menu_settings:
 			intent = new Intent(this, SettingsActivity.class);
 			startActivity(intent);
 			return true;
 		case R.id.menu_users:
 			selectUser();
 			return true;
 		case R.id.menu_logout:
 			mLogout = true;
 			stopService(new Intent(this, BerryTube.class));
 			//intent = new Intent(this, MainActivity.class);
 			//startActivity(intent);
 			finish();
 			return true;
 		case R.id.menu_poll:
 			showPoll();
 		default:
 			return super.onOptionsItemSelected(item);
 		}
 	}
 
 	private class ConfigurationInstance {
 		public int MyDrinkCount;
 	}
 
 	@Override
 	public Object onRetainNonConfigurationInstance() {
 		ConfigurationInstance instance = new ConfigurationInstance();
 		instance.MyDrinkCount = mMyDrinkCount;
 		return instance;
 	}
 
 	private void getConfigurationInstance() {
 		@SuppressWarnings("deprecation")
 		ConfigurationInstance instance = (ConfigurationInstance) getLastNonConfigurationInstance();
 		if (instance != null) {
 			mMyDrinkCount = instance.MyDrinkCount;
 		}
 	}
 
 	private void createCallback() {
 		mCallback = new BerryTubeCallback() {
 
 			@Override
 			public void onSetNick(String nick) {
 				setNick(nick);
 			}
 
 			@Override
 			public void onChatMessage(ChatMessage chatMsg) {
 				mChatAdapter.notifyDataSetChanged();
 
 			}
 
 			@Override
 			public void onDrinkCount(int count) {
 				mDrinkCount = count;
 				updateDrinkCount();
 			}
 
 			@Override
 			public void onNewPoll(Poll poll) {
 				if (mPopupPoll)
 					showPoll();
 			}
 
 			@Override
 			public void onUpatePoll(Poll poll) {
 				// TODO Auto-generated method stub
 				
 			}
 			
 			@Override
 			public void onClearPoll() {
 				
 			}
 			
 			@Override 
 			public void onKicked() {
 				mLogout = true;
 				finish();
 			}
 
 			@Override
 			public void onDisconnect() {
 				if (mLogout) return;
 				
 				AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
 				builder.setTitle(R.string.disconnected);
 				builder.setMessage(R.string.message_disconnected);
 				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {					
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						stopService(new Intent(ChatActivity.this, BerryTube.class));
 						ChatActivity.this.finish();						
 					}
 				});
 				
 				AlertDialog dialog = builder.create();
 				dialog.show();
 			}
 		};
 	}
 
 	private void loadPreferences() {
 		SharedPreferences settings = PreferenceManager
 				.getDefaultSharedPreferences(getBaseContext());
 		try {
 			mScrollback = Integer.parseInt(settings.getString(
 					MainActivity.KEY_SCROLLBACK, "100"));
 		} catch (NumberFormatException e) {
 			mScrollback = 100;
 		}
 
 		if (mScrollback <= 0)
 			mScrollback = 100;
 		
 		if (mBinder != null) 
 			mBinder.getService().setChatMsgBufferSize(mScrollback);
 
 		mShowDrinkCount = settings.getBoolean(MainActivity.KEY_DRINKCOUNT, true);
 		mPopupPoll = settings.getBoolean(MainActivity.KEY_POPUP_POLL, false);
 		updateDrinkCount();
 	}
 
 	private void sendChatMsg() {
 		String textmsg = mEditChatMsg.getText().toString();
 		if (mBinder.getService().isConnected() && !"".equals(mNick) && textmsg != "") {
 			mBinder.getService().sendChat(textmsg);
 			mEditChatMsg.setText("");
 		}
 	}
 
 	private void updateDrinkCount() {
 		if (!mShowDrinkCount) {
 			setTextDrinksVisible(false);
 			return;
 		}
 
 		if (mDrinkCount > 0) {
 			if (mMyDrinkCount > mDrinkCount)
 				mMyDrinkCount = 0;
 
 			setTextDrinksVisible(true);
 
 			mTextDrinks.setText(Integer.toString(mMyDrinkCount) + "/"
 					+ Integer.toString(mDrinkCount) + " drinks");
 		} else {
 			setTextDrinksVisible(false);
 			mMyDrinkCount = 0;
 		}
 	}
 
 	public void drink(View view) {
 		if (mMyDrinkCount < mDrinkCount) {
 			mMyDrinkCount++;
 			updateDrinkCount();
 		}
 	}
 
 	private void setTextDrinksVisible(boolean Visible) {
 		int visibility = (Visible) ? View.VISIBLE : View.GONE;
 
 		if (mTextDrinks != null && mTextDrinks.getVisibility() != visibility)
 			mTextDrinks.setVisibility(visibility);
 	}
 
 	private void selectUser() {
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder.setTitle(R.string.select_user);
 
 		ArrayList<ChatUser> userList = new ArrayList<ChatUser>();
 		for (ChatUser chatUser : mBinder.getService().getUsers()) {
 			userList.add(chatUser.clone());
 		}
 		Collections.sort(userList, new Comparator<ChatUser>() {
 
 			@Override
 			public int compare(ChatUser lhs, ChatUser rhs) {
 				if (lhs.getType() == rhs.getType()) {
 					return lhs.getNick().compareTo(rhs.getNick());
 				} else if (lhs.getType() > rhs.getType()) {
 					return -1;
 				} else {
 					return +1;
 				}
 
 			}
 		});
 
 		final ArrayList<String> userNicks = new ArrayList<String>();
 		for (ChatUser chatUser : userList) {
 			userNicks.add(chatUser.getNick());
 		}
 
 		builder.setItems(userNicks.toArray(new String[userList.size()]),
 				new DialogInterface.OnClickListener() {
 
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						String nick = userNicks.get(which);
 						int start = mEditChatMsg.getSelectionStart();
 						int end = mEditChatMsg.getSelectionEnd();
 
 						mEditChatMsg.getText().replace(Math.min(start, end),
 								Math.max(start, end), nick, 0, nick.length());
 
 						dialog.dismiss();
 					}
 				});
 
 		AlertDialog alert = builder.create();
 		alert.show();
 	}
 	
 	private void showPoll() {
 		Poll poll = mBinder.getService().getPoll();
 		if (poll == null) {
 			AlertDialog.Builder builder = new AlertDialog.Builder(this);
 			builder.setTitle(R.string.nopoll);
 			builder.setMessage(R.string.message_nopoll);
 			builder.setPositiveButton(android.R.string.ok, null);
 			
 			AlertDialog dialog = builder.create();
 			dialog.show();
 		} else {
 			AlertDialog.Builder builder = new AlertDialog.Builder(this);
 			builder.setTitle(poll.getTitle());
 			
 			String[] options = new String[mBinder.getService().getPoll().getOptions().size()];
 			for (int i = 0; i < options.length; i++) {
 				options[i] = "[" + Integer.toString(poll.getVotes().get(i)) + "] " + poll.getOptions().get(i);
 			}
 			builder.setItems(options, new DialogInterface.OnClickListener() {
 				
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					mBinder.getService().votePoll(which);
 				}
 			});
 			
 			AlertDialog dialog = builder.create();
 			dialog.show();
 		}
 	}
 }
