 package com.axelby.podax;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import android.app.Notification;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.bluetooth.BluetoothDevice;
 import android.content.BroadcastReceiver;
 import android.content.ComponentName;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.database.Cursor;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.media.AudioManager;
 import android.media.AudioManager.OnAudioFocusChangeListener;
 import android.media.MediaPlayer;
 import android.media.MediaPlayer.OnCompletionListener;
 import android.media.MediaPlayer.OnErrorListener;
 import android.net.Uri;
 import android.os.Build;
 import android.os.IBinder;
 import android.support.v4.app.NotificationCompat;
 import android.telephony.PhoneStateListener;
 import android.telephony.TelephonyManager;
 import android.util.Log;
 import android.widget.RemoteViews;
 import android.widget.Toast;
 
 import com.axelby.podax.PlayerStatus.PlayerStates;
 import com.axelby.podax.ui.PodcastDetailActivity;
 
 public class PlayerService extends Service {
 
 	private class UpdatePositionTimerTask extends TimerTask {
 		protected int _lastPosition = 0;
 		public void run() {
 			if (_player != null && !_player.isPlaying())
 				return;
 			int oldPosition = _lastPosition;
 			_lastPosition = _player.getCurrentPosition();
 			if (oldPosition / 1000 != _lastPosition / 1000)
 				updateActivePodcastPosition(_lastPosition);
 		}
 	};
 
 	private final OnAudioFocusChangeListener _afChangeListener = new OnAudioFocusChangeListener() {
 		public void onAudioFocusChange(int focusChange) {
 			// focusChange could be AUDIOFOCUS_GAIN, AUDIOFOCUS_LOSS,
 			// _LOSS_TRANSIENT or _LOSS_TRANSIENT_CAN_DUCK
 			PodaxLog.log(PlayerService.this, "audio focus change event");
 			if (focusChange == AudioManager.AUDIOFOCUS_GAIN && PlayerStatus.getCurrentState(PlayerService.this).isPaused())
 				PlayerService.play(PlayerService.this);
 			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
 				PlayerService.pause(PlayerService.this);
 		}
 	};
 
 	private final PhoneStateListener _phoneStateListener = new PhoneStateListener() {
 		@Override
 		public void onCallStateChanged(int state, String incomingNumber) {
 			if (_player == null)
 				return;
 
 			_onPhone = (state != TelephonyManager.CALL_STATE_IDLE);
 			if (_onPhone) {
 				PlayerService.pause(PlayerService.this);
 				_pausedForPhone = true;
 			}
 			if (!_onPhone && _pausedForPhone) {
 				PlayerService.play(PlayerService.this);
 				_pausedForPhone = false;
 			}
 		}
 	};
 
 	protected MediaPlayer _player;
 	private boolean _isPlayerPrepared;
 	protected boolean _onPhone;
 	protected boolean _pausedForPhone = false;
 	protected Timer _updateTimer;
 
 	private HeadsetConnectionReceiver _headsetConnectionReceiver = null;
 	private BluetoothConnectionReceiver _bluetoothConnectionReceiver = null;
 	private LockscreenManager _lockscreenManager = null;
 
 	@Override
 	public IBinder onBind(Intent intent) {
 		return null;
 	}
 
 	private void createUpdateTimer() {
 		if (_updateTimer != null)
 			return;
 
 		_updateTimer = new Timer();
 		_updateTimer.schedule(new UpdatePositionTimerTask(), 250, 250);
 	}
 
 	private void stopUpdateTimer() {
 		if (_updateTimer == null)
 			return;
 
 		_updateTimer.cancel();
 		_updateTimer = null;
 	}
 
 	@Override
 	public void onCreate() {
 		super.onCreate();
 
 		setup();
 	}
 
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 
 		safeUnregisterReceiver(_headsetConnectionReceiver);
 		safeUnregisterReceiver(_bluetoothConnectionReceiver);
 	}
 
 	private void setupMediaPlayer() {
 		if (_player == null) {
 			_player = new MediaPlayer();
 			_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
 
 			// handle errors so the onCompletionListener doens't get called
 			_player.setOnErrorListener(new OnErrorListener() {
 				public boolean onError(MediaPlayer mp, int what, int extra) {
 					PodaxLog.log(PlayerService.this, "mediaplayer error - what: %d, extra: %d", what, extra);
 					stopUpdateTimer();
 					_player = null;
 					return true;
 				}
 			});
 
 			_player.setOnCompletionListener(new OnCompletionListener() {
 				public void onCompletion(MediaPlayer player) {
 					playNextPodcast();
 				}
 			});
 
 			_isPlayerPrepared = false;
 		}
 	}
 
 	private void setup() {
 		if (_player == null) {
 			setupMediaPlayer();
 			setupTelephony();
 		}
 
 		if (_headsetConnectionReceiver == null) {
 			_headsetConnectionReceiver = new HeadsetConnectionReceiver();
 			setupReceiver(_headsetConnectionReceiver, Intent.ACTION_HEADSET_PLUG);
 		}
 		if (_bluetoothConnectionReceiver == null) {
 			_bluetoothConnectionReceiver = new BluetoothConnectionReceiver();
 			setupReceiver(_bluetoothConnectionReceiver, BluetoothDevice.ACTION_ACL_DISCONNECTED);
 		}
 
 		if (_lockscreenManager == null)
 			_lockscreenManager = new LockscreenManager();
 	}
 
 	private void setupReceiver(BroadcastReceiver receiver, String action) {
 		safeUnregisterReceiver(receiver);
 		registerReceiver(receiver, new IntentFilter(action));
 	}
 
 	private void safeUnregisterReceiver(BroadcastReceiver receiver) {
 		if (receiver == null)
 			return;
 		try {
 			unregisterReceiver(receiver);
 		} catch (IllegalArgumentException ex) { }
 	}
 
 	private void setupTelephony() {
 		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
 		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
 		_onPhone = (telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE);
 	}
 
 	private void tearDownTelephony() {
 		TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
 		telephony.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
 	}
 
 	@Override
 	public int onStartCommand(Intent intent, int flags, int startId) {
 		handleIntent(intent);
 		return START_STICKY;
 	}
 
 	private void handleIntent(Intent intent) {
 		if (intent == null || intent.getExtras() == null)
 			return;
 
 		if (!intent.getExtras().containsKey(Constants.EXTRA_PLAYER_COMMAND)) 
 			return;
 
 		setup();
 
 		switch (intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND, -1)) {
 		case -1:
 			return;
 		case Constants.PLAYER_COMMAND_SKIPTO:
 			PodaxLog.log(this, "PlayerService got a command: skip to");
 			skipTo(intent.getIntExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, 0));
 			break;
 		case Constants.PLAYER_COMMAND_SKIPTOEND:
 			PodaxLog.log(this, "PlayerService got a command: skip to end");
 			playNextPodcast();
 			break;
 		case Constants.PLAYER_COMMAND_RESTART:
 			PodaxLog.log(this, "PlayerService got a command: restart");
 			restart();
 			break;
 		case Constants.PLAYER_COMMAND_SKIPBACK:
 			PodaxLog.log(this, "PlayerService got a command: skip back");
 			skip(-15);
 			break;
 		case Constants.PLAYER_COMMAND_SKIPFORWARD:
 			PodaxLog.log(this, "PlayerService got a command: skip forward");
 			skip(30);
 			break;
 		case Constants.PLAYER_COMMAND_PLAYPAUSE:
 			PodaxLog.log(this, "PlayerService got a command: playpause");
 			if (_player.isPlaying()) {
 				PodaxLog.log(this, "  pausing");
 				pause();
 			} else {
 				PodaxLog.log(this, "  resuming");
 				grabAudioFocusAndResume();
 			}
 			break;
 		case Constants.PLAYER_COMMAND_PLAYSTOP:
 			PodaxLog.log(this, "PlayerService got a command: playstop");
 			if (_player.isPlaying()) {
 				PodaxLog.log(this, "  stopping");
 				stop();
 			} else {
 				PodaxLog.log(this, "  resuming");
 				grabAudioFocusAndResume();
 			}
 			break;
 		case Constants.PLAYER_COMMAND_PLAY:
 			PodaxLog.log(this, "PlayerService got a command: play");
 			grabAudioFocusAndResume();
 			break;
 		case Constants.PLAYER_COMMAND_PAUSE:
 			PodaxLog.log(this, "PlayerService got a command: pause");
 			pause();
 			break;
 		case Constants.PLAYER_COMMAND_STOP:
 			PodaxLog.log(this, "PlayerService got a command: stop");
 			stop();
 			break;
 		case Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST:
 			PodaxLog.log(this, "PlayerService got a command: play specific podcast");
 			long podcastId = intent.getLongExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, -1);
 			play(podcastId);
 			break;
 		}
 	}
 
 	private void resume() {
 		PodaxLog.log(this, "PlayerService resume");
 
 		if (PlayerStatus.getCurrentState(this).isPaused() && _isPlayerPrepared) {
 			_player.start();
 			PlayerStatus.updateState(this, PlayerStates.PLAYING);
 			_lockscreenManager.setLockscreenPlaying();
 			return;
 		}
 
 		String[] projection = new String[] {
 				PodcastProvider.COLUMN_ID,
 				PodcastProvider.COLUMN_TITLE,
 				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
 				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
 				PodcastProvider.COLUMN_MEDIA_URL,
 				PodcastProvider.COLUMN_LAST_POSITION,
 				PodcastProvider.COLUMN_DURATION,
 				PodcastProvider.COLUMN_FILE_SIZE,
 		};
 		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
 		try {
 			if (c.isAfterLast())
 				return;
 
 			PodcastCursor p = new PodcastCursor(c);
 			if (!p.isDownloaded()) {
 				Toast.makeText(this, R.string.podcast_not_downloaded, Toast.LENGTH_SHORT).show();
 				return;
 			}
 
 			prepareMediaPlayer(p);
 
 			// set this podcast as active -- it may not have been first in queue
 			QueueManager.changeActivePodcast(this, p.getId());
 
 			_lockscreenManager = new LockscreenManager();
 			_lockscreenManager.setupLockscreenControls(this, p);
 		} catch (IOException ex) {
 			stop();
 		} finally {
 			c.close();
 		}
 
 		// the user will probably try this if the podcast is over and the next one didn't start
 		if (_player.getCurrentPosition() >= _player.getDuration() - 1000) {
 			playNextPodcast();
 			return;
 		}
 
 		_player.start();
 		PlayerStatus.updateState(this, PlayerStates.PLAYING);
 
 		_pausedForPhone = false;
 		showNotification();
 		createUpdateTimer();
 		Helper.updateWidgets(this);
 	}
 
 	private void pause() {
 		PodaxLog.log(this, "PlayerService pausing");
 
 		_player.pause();
 		updateActivePodcastPosition(_player.getCurrentPosition());
 		PlayerStatus.updateState(this, PlayerStates.PAUSED);
 		_lockscreenManager.setLockscreenPaused();
 
 		tearDownTelephony();
 	}
 
 	private void stop() {
 		PodaxLog.log(this, "PlayerService stopping");
 
 		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 		am.abandonAudioFocus(_afChangeListener);
 
 		stopUpdateTimer();
 
 		tearDownTelephony();
 
 		removeNotification();
 
 		_lockscreenManager.removeLockscreenControls();
 
 		if (_player != null && _player.isPlaying()) {
 			_player.pause();
 			updateActivePodcastPosition(_player.getCurrentPosition());
 			_player.stop();
 		}
 
 		PlayerStatus.updateState(this, PlayerStates.STOPPED);
 		_player = null;
 		stopSelf();
 
 		// tell anything listening to the active podcast to refresh now that we're stopped
 		ContentValues values = new ContentValues();
 		getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
 
 		Helper.updateWidgets(this);
 	}
 
 	private boolean grabAudioFocus() {
 		if (_onPhone)
 			return false;
 
 		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 		int result = am.requestAudioFocus(_afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
 		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
 			stop();
 			return false;
 		}
 
 		// grab the media button when we have audio focus
 		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 		audioManager.registerMediaButtonEventReceiver(new ComponentName(this, MediaButtonIntentReceiver.class));
 
 		return true;
 	}
 
 	private void grabAudioFocusAndResume() {
		if (grabAudioFocus() && this != null)
 			resume();
 	}
 
 	private void prepareMediaPlayer(PodcastCursor p) throws IOException {
 		_player.reset();
 		_player.setDataSource(p.getFilename());
 		_player.prepare();
 		_player.seekTo(p.getLastPosition());
 		_isPlayerPrepared = true;
 	}
 
 	private void play(long podcastId) {
 		QueueManager.changeActivePodcast(this, podcastId);
 		grabAudioFocusAndResume();
 	}
 
 	private void skip(int secs) {
 		if (_player.isPlaying()) {
 			_player.seekTo(_player.getCurrentPosition() + secs * 1000);
 			updateActivePodcastPosition(_player.getCurrentPosition());
 		} else {
 			String[] projection = { PodcastProvider.COLUMN_LAST_POSITION };
 			Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
 			try {
 				if (c.moveToNext()) {
 					updateActivePodcastPosition(c.getInt(0) + secs * 1000);
 				}
 			} finally {
 				c.close();
 			}
 		}
 	}
 
 	private void skipTo(int secs) {
 		if (_player.isPlaying()) {
 			_player.seekTo(secs * 1000);
 			updateActivePodcastPosition(_player.getCurrentPosition());
 		} else {
 			updateActivePodcastPosition(secs * 1000);
 		}
 	}
 
 	private void restart() {
 		if (_player.isPlaying()) {
 			_player.seekTo(0);
 			updateActivePodcastPosition(_player.getCurrentPosition());
 		} else {
 			updateActivePodcastPosition(0);
 		}
 	}
 
 	private void playNextPodcast() {
 		PodaxLog.log(this, "moving to next podcast");
 
 		if (_player != null) {
 			// stop the player and the updating while we do some administrative stuff
 			_player.pause();
 			stopUpdateTimer();
 			updateActivePodcastPosition(_player.getCurrentPosition());
 		}
 
 		QueueManager.moveToNextInQueue(this);
 
 		grabAudioFocusAndResume();
 	}
 
 	private void showNotification() {
 		String[] projection = new String[] {
 				PodcastProvider.COLUMN_ID,
 				PodcastProvider.COLUMN_TITLE,
 				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
 				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
 		};
 		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
 		if (c.isAfterLast())
 			return;
 		PodcastCursor podcast = new PodcastCursor(c);
 
 		// both paths use the pendingintent
 		Intent showIntent = new Intent(this, PodcastDetailActivity.class);
 		PendingIntent showPendingIntent = PendingIntent.getActivity(this, 0, showIntent, 0);
 
 		Notification notification = null;
 		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
 			RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.player_notification);
 			contentView.setTextViewText(R.id.podcast, podcast.getTitle());
 			contentView.setOnClickPendingIntent(R.id.show_btn, showPendingIntent);
 
 			// set up pause intent
 			Intent pauseIntent = new Intent(this, PlayerService.class);
 			// use data to make intent unique
 			pauseIntent.setData(Uri.parse("podax://playercommand/stop"));
 			pauseIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_STOP);
 			PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
 			contentView.setOnClickPendingIntent(R.id.pause, pausePendingIntent);
 
 			// set up forward intent
 			Intent forwardIntent = new Intent(this, PlayerService.class);
 			// use data to make intent unique
 			forwardIntent.setData(Uri.parse("podax://playercommand/forward"));
 			forwardIntent.putExtra(Constants.EXTRA_PLAYER_COMMAND, Constants.PLAYER_COMMAND_SKIPFORWARD);
 			PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0, forwardIntent, 0);
 			contentView.setOnClickPendingIntent(R.id.forward, forwardPendingIntent);
 
 			// set subscription image
 			try {
 				long subscriptionId = podcast.getSubscriptionId();
 				String imageFilename = SubscriptionCursor.getThumbnailFilename(subscriptionId);
 				if (new File(imageFilename).exists()) {
 					Bitmap bitmap = BitmapFactory.decodeFile(imageFilename);
 					contentView.setImageViewBitmap(R.id.show_btn, bitmap);
 				} else {
 					Log.d("Podax", "file doesn't exist: " + imageFilename);
 					contentView.setImageViewResource(R.id.show_btn, R.drawable.icon);
 				}
 			} catch (OutOfMemoryError e) {
 				Log.d("Podax", "out of memory error");
 				contentView.setImageViewResource(R.id.show_btn, R.drawable.icon);
 			}
 	
 			notification = new NotificationCompat.Builder(this)
 				.setSmallIcon(R.drawable.icon)
 				.setContent(contentView)
 				.setOngoing(true)
 				.getNotification();
 		} else {
 			notification = new NotificationCompat.Builder(this)
 				.setSmallIcon(R.drawable.icon)
 				.setWhen(System.currentTimeMillis())
 				.setContentTitle(podcast.getTitle())
 				.setContentText(podcast.getSubscriptionTitle())
 				.setContentIntent(showPendingIntent)
 				.setOngoing(true)
 				.getNotification();
 		}
 
 		startForeground(Constants.NOTIFICATION_PLAYING, notification);
 
 		c.close();
 	}
 
 	private void removeNotification() {
 		stopForeground(true);
 	}
 
 	private void updateActivePodcastPosition(int position) {
 		ContentValues values = new ContentValues();
 		values.put(PodcastProvider.COLUMN_LAST_POSITION, position);
 		PlayerService.this.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
 
 		Helper.updateWidgets(this);
 	}
 
 	// static functions for easier controls
 	public static void play(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY);
 	}
 
 	public static void pause(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PAUSE);
 	}
 
 	public static void stop(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_STOP);
 	}
 
 	public static void playpause(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYPAUSE);
 	}
 
 	public static void playstop(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAYSTOP);
 	}
 
 	public static void skipForward(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPFORWARD);
 	}
 
 	public static void skipBack(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPBACK);
 	}
 
 	public static void restart(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_RESTART);
 	}
 
 	public static void skipToEnd(Context context) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPTOEND);
 	}
 
 	public static void skipTo(Context context, int secs) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_SKIPTO, secs);
 	}
 
 	public static void play(Context context, long podcastId) {
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcastId);
 	}
 
 	public static void play(Context context, PodcastCursor podcast) {
 		if (podcast == null)
 			return;
 		PlayerService.sendCommand(context, Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcast.getId());
 	}
 
 	private static void sendCommand(Context context, int command) {
 		Intent intent = new Intent(context, PlayerService.class);
 		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
 		context.startService(intent);
 	}
 
 	private static void sendCommand(Context context, int command, int arg) {
 		Intent intent = new Intent(context, PlayerService.class);
 		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
 		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
 		context.startService(intent);
 	}
 
 	private static void sendCommand(Context context, int command, long arg) {
 		Intent intent = new Intent(context, PlayerService.class);
 		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
 		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
 		context.startService(intent);
 	}
 }
