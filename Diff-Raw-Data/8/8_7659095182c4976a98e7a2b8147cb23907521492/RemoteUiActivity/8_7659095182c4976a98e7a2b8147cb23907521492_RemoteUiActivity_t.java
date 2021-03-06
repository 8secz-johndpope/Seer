 /* The following code was written by Menny Even Danan
  * and is released under the APACHE 2.0 license
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  */
 package net.evendanan.android.thumbremote.ui;
 
 import java.util.HashSet;
 
 import net.evendanan.android.thumbremote.R;
 import net.evendanan.android.thumbremote.RemoteApplication;
 import net.evendanan.android.thumbremote.ServerState;
 import net.evendanan.android.thumbremote.ShakeListener.OnShakeListener;
 import net.evendanan.android.thumbremote.UiView;
 import net.evendanan.android.thumbremote.network.HttpRequest;
 import net.evendanan.android.thumbremote.service.ServerRemoteService;
 import net.evendanan.android.thumbremote.service.State;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.app.ProgressDialog;
 import android.app.Service;
 import android.content.ComponentName;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnDismissListener;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
 import android.graphics.Bitmap;
 import android.graphics.Point;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.IBinder;
 import android.preference.PreferenceManager;
 import android.text.Editable;
 import android.text.TextUtils;
 import android.text.method.KeyListener;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.animation.Animation;
 import android.view.animation.Animation.AnimationListener;
 import android.view.animation.AnimationUtils;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.ImageView;
 import android.widget.ProgressBar;
 import android.widget.SeekBar;
 import android.widget.SeekBar.OnSeekBarChangeListener;
 import android.widget.TextView;
 import android.widget.ViewFlipper;
 
 
 public class RemoteUiActivity extends Activity implements
 		OnSharedPreferenceChangeListener, OnClickListener, OnShakeListener, UiView, OnSeekBarChangeListener {
 
 	public final static String TAG = RemoteUiActivity.class.toString();
 
 	//
 	private final static HashSet<Character> msPunctuation = new HashSet<Character>();
 	static
 	{
 		String punctuation = "!@#$%^&*()[]{}/?|'\",.<>\n ";
 		for(char c : punctuation.toCharArray())
 			msPunctuation.add(c);
 	}
 	/*
 	private static final int MESSAGE_MEDIA_PLAYING_CHANGED = 97565;
 	private static final int MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED = MESSAGE_MEDIA_PLAYING_CHANGED + 1;
 	private static final int MESSAGE_MEDIA_METADATA_CHANGED = MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED + 1;
 	*/
 	// Menu items
 	private static final int MENU_SETTINGS = Menu.FIRST;
 	private static final int MENU_HELP = MENU_SETTINGS+1;
 	private static final int MENU_RESCAN = MENU_HELP+1;
 	private static final int MENU_EXIT = MENU_RESCAN+1;
 	
 	// ViewFlipper
 	private static final int PAGE_NOTPLAYING = 0;
 	private static final int PAGE_NOWPLAYING = 1;
 	private ViewFlipper mFlipper;
 
 	// Other Views
 	ImageView mImageThumbnail;
 	ImageView mButtonPlayPause;
 	TextView mServerTitle;
 	TextView mTextTitle;
 	TextView mUserMessage;
 	String mUserMessageString = "";
 	TextView mTextElapsed;
 	TextView mDuration;
 	ProgressBar mElapsedBar;
 	TextView mMediaDetails = null;
 	InputMethodManager mImeManager;
 	TextView mKeyboardText;
 	
 	private double mSeekToPercentRequest = -1;
 	private int mTotalDuration = 0;
 	private int mCurrentTime = 0;
 	private boolean mMediaSeekBarInUserChange = false;
 	private SeekBar mMediaSeekBar;
 	private TextView mMediaSeekDialogTime;
 	
 	private static final int DIALOG_NO_PASSWORD = 1;
 	private static final int DIALOG_NO_SERVER = 2;
 	private static final int DIALOG_MEDIA_TIME_SEEK = 3;
 	
 	boolean mThisAcitivityPaused = true;
 	
 	//Not ready for prime time
 	//private ShakeListener mShakeDetector;
 	
 	private Point mTouchPoint = new Point();
 	private boolean mDragged = false;
 	private boolean mIsMediaActive = false;
 	private ProgressDialog mPleaseWaitDialog;
 	private Dialog mAlertDialog;
 	
 	private Handler mHandler;
 
 	/*
 	private final Runnable mRequestStatusUpdateRunnable = new Runnable() {
 		@Override
 		public void run() {
 			if (mStatePoller != null)
 				mStatePoller.checkStateNow();
 		}
 	};*/
 
 	private Animation mInAnimation;
 
 	private Animation mOutAnimation;
 
 	private boolean mIsBound = false;
 	private ServerRemoteService mBoundService;
 
 	private ServiceConnection mConnection = new ServiceConnection() {
 	    public void onServiceConnected(ComponentName className, IBinder service) {
 	    	Log.i(TAG, "Connected to "+className);
 	        // This is called when the connection with the service has been
 	        // established, giving us the service object we can use to
 	        // interact with the service.  Because we have bound to a explicit
 	        // service that we know is running in our own process, we can
 	        // cast its IBinder to a concrete class and directly access it.
 	        mBoundService = ((ServerRemoteService.LocalBinder)service).getService();
 	        mBoundService.setUiView(RemoteUiActivity.this);
 	    }
 
 	    public void onServiceDisconnected(ComponentName className) {
 	        // This is called when the connection with the service has been
 	        // unexpectedly disconnected -- that is, its process crashed.
 	        // Because it is running in our same process, we should never
 	        // see this happen.
 	    	if (mBoundService!= null) mBoundService.setUiView(null);
 	        mBoundService = null;
 	    }
 	};
 
 	void doBindService() {
 		startService(new Intent(getApplicationContext(), ServerRemoteService.class)); 
 		// Establish a connection with the service.  We use an explicit
 	    // class name because we want a specific service implementation that
 	    // we know will be running in our own process (and thus won't be
 	    // supporting component replacement by other applications).
 		mIsBound = bindService(new Intent(getApplicationContext(), ServerRemoteService.class), mConnection, 0);
 	}
 
 	void doUnbindService() {
 	    if (mIsBound) {
 	    	if (mBoundService!= null) mBoundService.setUiView(null);
 	        // Detach our existing connection.
 	        unbindService(mConnection);
 	        mIsBound = false;
 	    }
 	}
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		setRequestedOrientation(RemoteApplication.getConfig().getRequestedScreenOrientation());
 		
 		super.onCreate(savedInstanceState);
 
 		/*
 		mHandler = new Handler() {
 			@Override
 			public void handleMessage(Message msg) {
 				switch (msg.what) {
 				case MESSAGE_MEDIA_PLAYING_CHANGED:
 					refreshPlayingStateChanged(false);
 					break;
 				case MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED:
 					refreshPlayingProgressChanged();
 					break;
 				case MESSAGE_MEDIA_METADATA_CHANGED:
 					refreshMetadataChanged();
 					break;
 				}
 			}
 		};
 		*/
 		mHandler = new Handler();
		mInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.boxee_dialog_in);
		mOutAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.boxee_dialog_out);
 		mOutAnimation.setAnimationListener(new AnimationListener() {
 			@Override
 			public void onAnimationStart(Animation animation) {}
 			
 			@Override
 			public void onAnimationRepeat(Animation animation) {}
 			
 			@Override
 			public void onAnimationEnd(Animation animation) {mUserMessage.setText(""); mUserMessage.setVisibility(View.INVISIBLE);}
 		});
 		
 		//mRemote = new BoxeeConnector();
 		
 		setContentView(R.layout.main);
 
 		// Setup flipper
 		mFlipper = (ViewFlipper) findViewById(R.id.now_playing_flipper);
 		mFlipper.setInAnimation(this, android.R.anim.slide_in_left);
 		mFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
 
 		// Find other views
 		mServerTitle = (TextView)findViewById(R.id.serverTitle);
 		mImageThumbnail = (ImageView) findViewById(R.id.thumbnail);
 		mButtonPlayPause = (ImageView)findViewById(R.id.buttonPlayPause);
 		mTextTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
 		mUserMessage = (TextView)findViewById(R.id.textMessages);
 		mTextElapsed = (TextView) findViewById(R.id.textElapsed);
 		mDuration = (TextView) findViewById(R.id.textDuration);
 		mElapsedBar = (ProgressBar) findViewById(R.id.progressTimeBar);
 		findViewById(R.id.layoutTimeBar).setOnClickListener(this);
 		mMediaDetails = (TextView)findViewById(R.id.textMediaDetails);
 		mImeManager = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
 		mKeyboardText = (TextView)findViewById(R.id.keyboard_text);
 		mKeyboardText.setKeyListener(new KeyListener() {
 			
 			@Override
 			public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
 				//I'm returning false here so the activity will handle the onKeyUp
 				return false;
 			}
 			
 			@Override
 			public boolean onKeyOther(View view, Editable text, KeyEvent event) {
 				//I'm returning false here so the activity will handle the onKeyOther
 				return false;
 			}
 			
 			@Override
 			public boolean onKeyDown(View view, Editable text, int keyCode,
 					KeyEvent event) {
 				//Log.d(TAG, "KeyListener.onKeyDown: "+(char)event.getUnicodeChar());
 				//I'm returning false here so the activity will handle the onKeyDown
 				return false;
 			}
 			
 			@Override
 			public int getInputType() {
 				// TODO Auto-generated method stub
 				return 0;
 			}
 			
 			@Override
 			public void clearMetaKeyState(View view, Editable content, int states) {
 				// TODO Auto-generated method stub
 				
 			}
 		});
 		/*
 		mKeyboardText.setOnKeyListener(new OnKeyListener() {
 			@Override
 			public boolean onKey(View v, int keyCode, KeyEvent event) {
 				RemoteUiActivity.this.onKeyDown(keyCode, event);
 				RemoteUiActivity.this.onKeyUp(keyCode, event);
 				
 				return false;
 			}
 		});*/
 
 		setButtonAction(R.id.back, KeyEvent.KEYCODE_BACK);
 		setButtonAction(R.id.buttonPlayPause, 0);
 		setButtonAction(R.id.buttonStop, 0);
 		setButtonAction(R.id.buttonSmallSkipBack, 0);
 		setButtonAction(R.id.buttonSmallSkipFwd, 0);
 		setButtonAction(R.id.buttonPreviousMedia, 0);
 		setButtonAction(R.id.buttonNextMedia, 0);
 		
 		setButtonAction(R.id.buttonOpenKeyboard, 0);
 		//mShakeDetector = new ShakeListener(getApplicationContext());
 		//mShakeDetector.setOnShakeListener(this);
 		
 		startHelpOnFirstRun();
 	}
 
 	private void startHelpOnFirstRun() {
 		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 		final boolean ranBefore = preferences.getBoolean("has_ran_before", false);
 		if (!ranBefore)
 		{
 			Editor e = preferences.edit();
 			e.putBoolean("has_ran_before", true);
 			e.commit();
 			
 			startHelpActivity();
 		}
 	}
 
 	private void startHelpActivity() {
 		Intent i = new Intent(getApplicationContext(), HelpUiActivity.class);
 		startActivity(i);
 	}
 	
 	private void startSetupActivity() {
 		Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
 		startActivity(i);
 	}
 	
 	@Override
 	protected void onPause() {
 		mSwipeStepSize = -1;
 		mThisAcitivityPaused = true;
 		//mShakeDetector.pause();
 		RemoteApplication.getConfig().unlisten(this);
 		//mHandler.removeCallbacks(mRequestStatusUpdateRunnable);
 		
 		if (mPleaseWaitDialog != null)
 			mPleaseWaitDialog.dismiss();
 		mPleaseWaitDialog = null;
 		
 		doUnbindService();
 		super.onPause();
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		mThisAcitivityPaused = false;
 		
 		RemoteApplication.getConfig().listen(this);
 		loadPreferences();
 		
 		//mShakeDetector.resume();
 		
 		mImageThumbnail.setKeepScreenOn(RemoteApplication.getConfig().getKeepScreenOn());
 		
 		doBindService();
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.settings).setIcon(
 				android.R.drawable.ic_menu_preferences);
 		
 		menu.add(Menu.NONE, MENU_HELP, 0, R.string.help).setIcon(
 				android.R.drawable.ic_menu_help);
 		
 		menu.add(Menu.NONE, MENU_RESCAN, 0, R.string.rescan_servers).setIcon(
 				android.R.drawable.ic_menu_search);
 		
 		menu.add(Menu.NONE, MENU_EXIT, 0, R.string.exit_app).setIcon(
 				android.R.drawable.ic_menu_close_clear_cancel);
 		return true;
 	}
 	
 	@Override
 	public boolean onMenuItemSelected(int featureId, MenuItem item) {
 		switch(item.getItemId())
 		{
 		case MENU_EXIT:
 			if (mBoundService != null) mBoundService.forceStop();
 			finish();
 			return true;
 		case MENU_HELP:
 			startHelpActivity();
 			return true;
 		case MENU_SETTINGS:
 			startSetupActivity();
 			return true;
 		case MENU_RESCAN:
 			if (mBoundService != null) mBoundService.remoteRescanForServers();
 			return true;
 		default:
 			return super.onMenuItemSelected(featureId, item);
 		}
 	}
 
 	@Override
 	public void onClick(View v) {
 		final int id = v.getId();
 		switch (id) {
 		
 		case R.id.buttonPlayPause:
 			if (mBoundService != null) mBoundService.remoteFlipPlayPause();
 			break;
 
 		case R.id.buttonStop:
 			if (mBoundService != null) mBoundService.remoteStop();
 			break;
 
 		case R.id.buttonSmallSkipBack:
 		case R.id.buttonSmallSkipFwd:
 			if (mBoundService != null) mBoundService.remoteSeekOffset((id == R.id.buttonSmallSkipFwd)? 30f : -30f);
 			break;
 		case R.id.back:
 			if (mBoundService != null) mBoundService.remoteBack();
 			break;
 		case R.id.buttonOpenKeyboard:
 			showOnScreenKeyboard();
 			break;
 		case R.id.progressTimeBar:
 		case R.id.layoutTimeBar:
 			showDialog(DIALOG_MEDIA_TIME_SEEK);
 		}
 
 	}
 
 	private void flipTo(int page) {
 		if (mFlipper.getDisplayedChild() != page)
 			mFlipper.setDisplayedChild(page);
 	}
 	
 	@Override
 	public void hello(final ServerState serverState) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				refreshPlayingStateChanged(serverState, true);
 				if (serverState.isMediaActive())
 				{
 					refreshMetadataChanged(serverState);
 				}
 			}
 		});
 	}
 
 	@Override
 	public void onMediaPlayingStateChanged(final ServerState serverState) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				refreshPlayingStateChanged(serverState, false);
 			}
 		});
 	}
 	
 	@Override
 	public void onMediaPlayingProgressChanged(final ServerState serverState) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				refreshPlayingProgressChanged(serverState);
 			}
 		});		
 	}
 	
 	@Override
 	public void onMediaMetadataChanged(final ServerState serverState) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				refreshMetadataChanged(serverState);
 			}
 		});
 	}
 	
 	@Override
 	public void onKeyboardStateChanged(final ServerState serverState) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				if (serverState.isKeyboardActive() && mKeyboardText.getVisibility() != View.VISIBLE)
 				{
 					Log.d(TAG, "Keyboard is active. Showing textbox");
 					showOnScreenKeyboard();
 				}
 				else if (!serverState.isKeyboardActive() && mKeyboardText.getVisibility() == View.VISIBLE)
 				{
 					Log.d(TAG, "Keyboard is inactive. Hiding textbox");
 					mKeyboardText.setVisibility(View.INVISIBLE);
 					mImeManager.hideSoftInputFromWindow(mKeyboardText.getWindowToken(), 0);
 				}
 			}
 		});
 	}
 	
 	private void refreshPlayingStateChanged(ServerState serverState, boolean forceChanged) {
 		final boolean isPlaying = serverState.isMediaPlaying();
 		final boolean newIsMediaActive = serverState.isMediaActive();
 		final boolean mediaDataChanged = forceChanged || (newIsMediaActive != mIsMediaActive);
 		mIsMediaActive = newIsMediaActive;
 
 		if (mIsMediaActive) mButtonPlayPause.setImageResource(isPlaying ? R.drawable.icon_osd_pause_on : R.drawable.icon_osd_play_on);
 		if (!mediaDataChanged) return;
 		
 		if (mIsMediaActive) {
 			final String title = getMediaTitle(serverState);
 			mTextTitle.setText(title);
 			if (mMediaDetails != null) mMediaDetails.setText(serverState.getMediaPlot());
 			refreshPlayingProgressChanged(serverState);
 			
 			flipTo(PAGE_NOWPLAYING);
 			
 			//if (mBoundService!=null) mBoundService.showPlayingNotification(title);
 //			Intent i = new Intent(this, ServerRemoteService.class);
 //			i.putExtra(ServerRemoteService.KEY_DATA_TITLE, title);
 //			startService(i);
 		}
 		else
 		{
 			flipTo(PAGE_NOTPLAYING);
 			
 			mTextTitle.setText("");
 			if (mMediaDetails != null) mMediaDetails.setText("");
 			mImageThumbnail.setImageResource(R.drawable.no_media_background);
 		}
 	}
 
 	private void refreshPlayingProgressChanged(ServerState serverState)
 	{
 		mDuration.setText(serverState.getMediaTotalTime());
 		mTotalDuration = ServerRemoteService.hmsToSeconds(serverState.getMediaTotalTime());
 		mTextElapsed.setText(serverState.getMediaCurrentTime());
 		mCurrentTime = ServerRemoteService.hmsToSeconds(serverState.getMediaCurrentTime());
 		
 		mElapsedBar.setProgress(serverState.getMediaProgressPercent());
 		
 		if (!mMediaSeekBarInUserChange)
 		{
 			if (mMediaSeekBar != null)
 			{
 				mMediaSeekBar.setProgress(mCurrentTime);
 			}
 			if (mMediaSeekDialogTime != null)
 			{
 				mMediaSeekDialogTime.setText(serverState.getMediaCurrentTime());
 			}
 		}
 		
 	}
 	
 	private void refreshMetadataChanged(ServerState serverState) {
 		mIsMediaActive = serverState.isMediaActive();
 		if (mIsMediaActive)
 		{
 			Bitmap poster = serverState.getMediaPoster();
 			if (poster == null)
 				mImageThumbnail.setImageResource(R.drawable.no_media_background);
 			else
 				mImageThumbnail.setImageBitmap(poster);
 			mTextTitle.setText(getMediaTitle(serverState));
 			if (mMediaDetails != null) mMediaDetails.setText(serverState.getMediaPlot());
 		}
 		else
 		{
 			mImageThumbnail.setImageResource(R.drawable.no_media_background);
 			mTextTitle.setText("");
 			if (mMediaDetails != null) mMediaDetails.setText("");
 		}
 	}
 
 	private String getMediaTitle(ServerState serverState) {
 		String showTitle = serverState.getShowTitle();
 		String title = serverState.getMediaTitle();
 		String filename = serverState.getMediaFilename();
 		String season = serverState.getShowSeason();
 		String episode = serverState.getShowEpisode();
 		String mediaTitle = "";
 		if (!TextUtils.isEmpty(showTitle))
 		{
 			mediaTitle = showTitle;
 			if (!TextUtils.isEmpty(season)) mediaTitle += " S"+season;
 			if (!TextUtils.isEmpty(episode)) mediaTitle += "E"+episode;
 		}
 		
 		if (!TextUtils.isEmpty(title))
 		{
 			if (!TextUtils.isEmpty(mediaTitle)) mediaTitle += ": ";
 			mediaTitle += title;
 		}
 		
 		if (!TextUtils.isEmpty(mediaTitle))
 			return mediaTitle;
 
 		if (!TextUtils.isEmpty(filename))
 			return filename;
 
 		return "";
 	}
 
 	/**
 	 * Handler an android keypress and send it to boxee if appropriate.
 	 */
 	@Override
 	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
 		switch (keyCode) {
 		case KeyEvent.KEYCODE_BACK:
 			if (RemoteApplication.getConfig().getHandleBack())
 			{
 				Log.d(TAG, "Will handle back");
 				if (mBoundService != null) mBoundService.remoteBack();
 				return true;
 			}
 			else
 			{
 				Log.d(TAG, "Will NOT handle back");
 				return super.onKeyDown(keyCode, event);
 			}
 
 		case KeyEvent.KEYCODE_DPAD_CENTER:
 			if (mBoundService != null) mBoundService.remoteSelect();
 			return true;
 
 		case KeyEvent.KEYCODE_DPAD_DOWN:
 			if (mBoundService != null) mBoundService.remoteDown();
 			return true;
 
 		case KeyEvent.KEYCODE_DPAD_UP:
 			if (mBoundService != null) mBoundService.remoteUp();
 			return true;
 
 		case KeyEvent.KEYCODE_DPAD_LEFT:
 			if (mBoundService != null) mBoundService.remoteLeft();
 			return true;
 
 		case KeyEvent.KEYCODE_DPAD_RIGHT:
 			if (mBoundService != null) mBoundService.remoteRight();
 			return true;
 
 		case KeyEvent.KEYCODE_VOLUME_UP:
 		case KeyEvent.KEYCODE_VOLUME_DOWN:
 			final int volumeFactor = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)? -1 : 1;
 			final int volumeOffset = (volumeFactor * RemoteApplication.getConfig().getVolumeStep());
 			if (mBoundService != null) mBoundService.remoteVolumeOffset(volumeOffset);
 			return true;
 		case KeyEvent.KEYCODE_DEL:
 			if (mBoundService != null) mBoundService.remoteKeypress((char)8);
 			return true;
 		default:
 			final char unicodeChar = (char)event.getUnicodeChar();
 			
 			//Log.d(TAG, "Unicode is " + ((char)unicodeChar));
 
 			if (Character.isLetterOrDigit(unicodeChar) || msPunctuation.contains(unicodeChar)) {
 				//Log.d(TAG, "handling " + ((char)unicodeChar));
 				if (mBoundService != null) mBoundService.remoteKeypress(unicodeChar);
 				return true;
 			}
 			else
 			{
 				return super.onKeyDown(keyCode, event);
 			}
 		}
 	}
 	
 	@Override
 	public boolean onKeyUp(int keyCode, KeyEvent event) {
 		switch(keyCode)
 		{
 		case KeyEvent.KEYCODE_BACK:
 			if (RemoteApplication.getConfig().getHandleBack())
 			{
 				return true;
 			}
 			else
 			{
 				return super.onKeyUp(keyCode, event);
 			}
 		case KeyEvent.KEYCODE_VOLUME_UP:
 		case KeyEvent.KEYCODE_VOLUME_DOWN:
 			//I catch these two to handle the annoying sound feedback given in some hand-held (Samsung Tab, e.g.)
 			return true;
 
 		default:
 			return super.onKeyDown(keyCode, event);
 		}
 	}
 
 	private Runnable mClearUserMessageRunnable = new Runnable() {
 		@Override
 		public void run() {
 			mUserMessage.startAnimation(mOutAnimation);
 		}
 	};
 
 	private Runnable mSetUserMessageRunnable = new Runnable() {
 		@Override
 		public void run() {
 			mUserMessage.setText(mUserMessageString);
 			mUserMessage.setVisibility(View.VISIBLE);
 			mUserMessage.startAnimation(mInAnimation);
 		}
 	};
 	
 	@Override
 	public void showMessage(final String userMessage, final int messageTime) {
 		mUserMessageString = userMessage;
 		mHandler.removeCallbacks(mClearUserMessageRunnable);
 		mHandler.removeCallbacks(mSetUserMessageRunnable);
 		mHandler.postAtFrontOfQueue(mSetUserMessageRunnable);
 		mHandler.postDelayed(mClearUserMessageRunnable, messageTime);
 	}
 
 	private int mSwipeStepSize = -1;
 
 	@Override
 	public boolean onTouchEvent(MotionEvent event) {
 		
 		int x = (int) event.getX();
 		int y = (int) event.getY();
 		if (mSwipeStepSize < 1)
 			mSwipeStepSize = RemoteApplication.getConfig().getSwipeStepSize(getApplicationContext());
 		switch (event.getAction()) {
 		case MotionEvent.ACTION_UP:
 			if (!mDragged) {
 				if (mBoundService != null) mBoundService.remoteSelect();
 				return true;
 			}
 			break;
 
 		case MotionEvent.ACTION_DOWN:
 			mTouchPoint.x = x;
 			mTouchPoint.y = y;
 			mDragged = false;
 			return true;
 
 		case MotionEvent.ACTION_MOVE:
 			if (x - mTouchPoint.x > mSwipeStepSize) {
 				if (mBoundService != null) mBoundService.remoteRight();
 				mTouchPoint.x += mSwipeStepSize;
 				mTouchPoint.y = y;
 				mDragged = true;
 				return true;
 			} else if (mTouchPoint.x - x > mSwipeStepSize) {
 				if (mBoundService != null) mBoundService.remoteLeft();
 				mTouchPoint.x -= mSwipeStepSize;
 				mTouchPoint.y = y;
 				mDragged = true;
 				return true;
 			} else if (y - mTouchPoint.y > mSwipeStepSize) {
 				if (mBoundService != null) mBoundService.remoteDown();
 				mTouchPoint.y += mSwipeStepSize;
 				mTouchPoint.x = x;
 				mDragged = true;
 				return true;
 			} else if (mTouchPoint.y - y > mSwipeStepSize) {
 				if (mBoundService != null) mBoundService.remoteUp();
 				mTouchPoint.y -= mSwipeStepSize;
 				mTouchPoint.x = x;
 				mDragged = true;
 				return true;
 			}
 			break;
 		}
 
 		return false;
 	}
 
 	/**
 	 * Set up a navigation button in the UI. Sets the focus to false so that we
 	 * can capture KEYCODE_DPAD_CENTER.
 	 * 
 	 * @param id
 	 *            id of the button in the resource file
 	 * 
 	 * @param keycode
 	 *            keyCode we should send to Boxee when this button is pressed
 	 */
 	private void setButtonAction(int id, final int keyCode) {
 		View button = findViewById(id);
 		if (button == null) return;
 		button.setFocusable(false);
 		button.setTag(new Integer(keyCode));
 		button.setOnClickListener(this);
 	}
 	
 	/**
 	 * Set the state of the application based on prefs. This should be called
 	 * after every preference change or when starting up.
 	 * 
 	 * @param prefs
 	 */
 	private void loadPreferences() {
 		mSwipeStepSize = RemoteApplication.getConfig().getSwipeStepSize(getApplicationContext());
 		// Setup the proper pageflipper page:
 		flipTo(PAGE_NOTPLAYING);
 		
 		// Setup the HTTP timeout.
 		int timeout_ms = RemoteApplication.getConfig().getTimeout();
 		HttpRequest.setTimeout(timeout_ms);
 
 		// Parse the credentials, if needed.
 		String user = RemoteApplication.getConfig().getUser();
 		String password = RemoteApplication.getConfig().getPassword();
 		if (!TextUtils.isEmpty(password))
 			HttpRequest.setUserPassword(user, password);
 		else
 			HttpRequest.setUserPassword(null, null);
 	}
 /*
 	private void setServer() {
 		// Only set the host if manual. Otherwise we'll auto-detect it with
 		// Discoverer -> addAnnouncedServers
 		if (RemoteApplication.getConfig().isManuallySetServer()) {
 			mRemote.setServer(RemoteApplication.getConfig().constructServer());
 			requestUpdateASAP(100);
 		}
 		else {
 			if (mDialogToDismiss > 0)
 				dismissDialog(mDialogToDismiss);
 			
 			if (mPleaseWaitDialog != null)
 				mPleaseWaitDialog.dismiss();
 			
 			mPleaseWaitDialog = ProgressDialog.show(this, "", "Looking for a server...", true);
 			
 			if (mServerDiscoverer != null)
 				mServerDiscoverer.setReceiver(null);
 			
 			mServerDiscoverer = new BoxeeDiscovererThread(this, this);
 			mServerDiscoverer.start();
 		}
 	}*/
 
 	@Override
 	protected Dialog onCreateDialog(int id) {
 		switch(id)
 		{
 		case DIALOG_NO_PASSWORD:
 			return createCredentialsRequiredDialog();
 		case DIALOG_NO_SERVER:
 			return  createNoServerDialog();
 		case DIALOG_MEDIA_TIME_SEEK:
 			return createMediaTimeSeekDialog();
 		}
 		
 		return super.onCreateDialog(id);
 	}
 	
 	@Override
 	protected void onPrepareDialog(int id, Dialog dialog) {
 		mAlertDialog = dialog;
		if (mAlertDialog != null) mAlertDialog.getWindow().setWindowAnimations(R.style.BoxeeInOut);
 		if (id == DIALOG_MEDIA_TIME_SEEK)
 		{
 			mMediaSeekBarInUserChange = false;
 			mSeekToPercentRequest = -1;
 			mMediaSeekBar = (SeekBar) dialog.findViewById(R.id.time_seek_bar);
 			mMediaSeekBar.setMax(mTotalDuration);
 			mMediaSeekBar.setProgress(mCurrentTime);
 			mMediaSeekDialogTime = (TextView)dialog.findViewById(R.id.time_string);
 			mMediaSeekDialogTime.setText(mTextElapsed.getText());
 		}
 		super.onPrepareDialog(id, dialog);
 	}
 
 	private Dialog createCredentialsRequiredDialog() {
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder
 			.setTitle(R.string.need_creds_title)
 			.setMessage(R.string.need_creds_message)
 		       .setCancelable(true)
 		       .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   startSetupActivity();
 		        	   dialog.dismiss();
 		           }
 		       })
 		       .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		                dialog.cancel();
 		           }
 		       });
 		AlertDialog alert = builder.create();
 		
 		return alert;
 	}
 
 	private Dialog createMediaTimeSeekDialog() {
 		final Dialog seeker = new Dialog(this, R.style.Popup);
 		seeker.setContentView(R.layout.custom_time_selection);
 		seeker.findViewById(R.id.seeker_close_button).setOnClickListener(new OnClickListener() {
 			@Override
 			public void onClick(View v) {
 				seeker.dismiss();
 			}
 		});
 		seeker.setOnDismissListener(new OnDismissListener() {
 			@Override
 			public void onDismiss(DialogInterface dialog) {
 				mMediaSeekBar = null;
 			}
 		});
 
 		SeekBar seekBar = (SeekBar) seeker.findViewById(R.id.time_seek_bar);
 		seekBar.setOnSeekBarChangeListener(this);
 		
 		return seeker;
 	}
 	
 	private Dialog createNoServerDialog() {
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder
 			.setTitle(R.string.no_server_found_title)
 			.setMessage(R.string.no_server_found_message)
 		       .setCancelable(true)
 		       .setPositiveButton(R.string.no_server_found_action_manual, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   startSetupActivity();
 		        	   dialog.dismiss();
 		           }
 		       })
 		       .setNeutralButton(R.string.no_server_found_action_rescan, new DialogInterface.OnClickListener() {
 		           public void onClick(DialogInterface dialog, int id) {
 		        	   if (mBoundService != null) mBoundService.remoteRescanForServers();
 		        	   dialog.dismiss();
 		           }
 		       })
 				.setNegativeButton(R.string.no_server_found_action_neither, new DialogInterface.OnClickListener() {
 			           public void onClick(DialogInterface dialog, int id) {
 			                dialog.cancel();
 			           }
 			       });
 		AlertDialog alert = builder.create();
 		
 		return alert;
 	}
 
 	@Override
 	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
 		if (fromUser)
 		{
 			mSeekToPercentRequest = 100f * ((float)progress / (float)seekBar.getMax());
 			String totalTime = mDuration.getText().toString();
 			int totalSeconds = ServerRemoteService.hmsToSeconds(totalTime);
 			if (totalSeconds > 0)
 			{
 				int newSeconds = (int)(mSeekToPercentRequest * ((float)totalSeconds))/100;
 				mMediaSeekDialogTime.setText(secondsToHms(newSeconds));
 			}
 			else
 			{
 				mMediaSeekDialogTime.setText("---");
 			}
 		}
 	}
 	
 	private static String secondsToHms(int totalSeconds)
 	{
 		int hours = totalSeconds / (60 * 60);
 		int minutes = (totalSeconds / 60) % 60;
 		int seconds = totalSeconds % 60;
 		
 		if (hours > 0)
 			return String.format("%d:%02d:%02d", hours, minutes, seconds);
 		else
 			return String.format("%02d:%02d", minutes, seconds);
 	}
 	
 	@Override
 	public void onStartTrackingTouch(SeekBar seekBar) {
 		mSeekToPercentRequest = -1f;
 		mMediaSeekBarInUserChange = true;
 	}
 	
 	@Override
 	public void onStopTrackingTouch(SeekBar seekBar) {
 		mMediaSeekBarInUserChange = false;
 		if (mSeekToPercentRequest >= 0)
 		{
 			if (mBoundService != null) mBoundService.remoteSeekTo(mSeekToPercentRequest);
 		}
 	}
 	
 	/**
 	 * Callback when user alters preferences.
 	 */
 	public void onSharedPreferenceChanged(SharedPreferences prefs, String pref) {
 		loadPreferences();
 	}
 	
 	@Override
 	public void onServerConnectionStateChanged(final State state) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				if (mAlertDialog != null && mAlertDialog.isShowing())
 					mAlertDialog.dismiss();
 				mAlertDialog = null;
 				
 				if (mPleaseWaitDialog != null && mPleaseWaitDialog.isShowing())
 					mPleaseWaitDialog.dismiss();
 				
 				mPleaseWaitDialog = null;
 				
 				switch(state)
 				{
 				case DEAD:
 					/*OK*/
 					break;
 				case DISCOVERYING:
 					mPleaseWaitDialog = ProgressDialog.show(RemoteUiActivity.this, getString(R.string.discoverying_dialog_title), getString(R.string.discoverying_dialog_message), true, false);
					mPleaseWaitDialog.getWindow().setWindowAnimations(R.style.BoxeeInOut);
 					break;
 				case ERROR_NO_PASSWORD:
 					showDialog(DIALOG_NO_PASSWORD);
 					break;
 				case ERROR_NO_SERVER:
 					showDialog(DIALOG_NO_SERVER);
 					break;
 				case IDLE:
 				}
 				
 				String serverName = mBoundService!=null? mBoundService.getServerName() : null;
 				if (TextUtils.isEmpty(serverName))
 				{
 					mServerTitle.setText(R.string.not_connected);
 				}
 				else
 				{
 					mServerTitle.setText(getString(R.string.connected_to, serverName));
 				}	
 			}
 		});
 	}
 	
 	@Override
 	public void onShake() {
 		Log.d(TAG, "Shake detect!");
 	}
 
 	private void showOnScreenKeyboard() {
 		mKeyboardText.setVisibility(View.VISIBLE);
 		mKeyboardText.requestFocus();
 		mImeManager.showSoftInput(mKeyboardText, InputMethodManager.SHOW_FORCED);
 	}
 }
