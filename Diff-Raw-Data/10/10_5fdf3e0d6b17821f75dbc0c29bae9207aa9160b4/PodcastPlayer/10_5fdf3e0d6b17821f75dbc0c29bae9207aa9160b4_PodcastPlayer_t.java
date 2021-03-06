 package com.axelby.podax;
 
 import android.content.Context;
 import android.media.AudioManager;
 import android.media.MediaPlayer;
 
 import java.io.IOException;
 import java.util.ArrayList;
 
 public class PodcastPlayer /*extends MediaPlayer*/ {
 
 	// listen for audio focus changes - another app started/stopped, phone call, etc
 	private final AudioManager.OnAudioFocusChangeListener _afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
 		public void onAudioFocusChange(int focusChange) {
 			if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
 				stop();
 			else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
 				unpause(Constants.PAUSE_AUDIOFOCUS);
 			else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
 				pause(Constants.PAUSE_AUDIOFOCUS);
 		}
 	};
 
 	protected Context _context;
 	private MediaPlayer _player;
 	private boolean _prepared = false;
 	private ArrayList<Boolean> _pausingFor = new ArrayList<Boolean>(2);
 
 	private OnPauseListener _onPauseListener = null;
 	private OnPlayListener _onPlayListener = null;
 	private OnStopListener _onStopListener = null;
 	private OnSeekListener _onSeekListener = null;
 	private OnCompletionListener _onCompletionListener = null;
 	private OnUnpreparedListener _onUnpreparedListener = null;
 	private MediaPlayer.OnErrorListener _onErrorListener = null;
 
 	Thread _updateThread;
 
 	public PodcastPlayer(Context context) {
 		super();
 		_context = context;
 		_pausingFor.add(false);
 		_pausingFor.add(false);
 
 		_player = new MediaPlayer();
 		_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
 
 		_player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
 			@Override
 			public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
 				mediaPlayer.reset();
 				if (_onErrorListener != null)
 					_onErrorListener.onError(mediaPlayer, what, extra);
 				return true;
 			}
 		});
 		_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
 			@Override
 			public void onCompletion(MediaPlayer mediaPlayer) {
 				if (_onCompletionListener != null)
 					_onCompletionListener.onCompletion();
 			}
 		});
 		_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
 			@Override
 			public void onPrepared(MediaPlayer mediaPlayer) {
 				_prepared = true;
 			}
 		});
 	}
 
 	public void setOnUnpreparedListener(OnUnpreparedListener onUnpreparedListener) {
 		this._onUnpreparedListener = onUnpreparedListener;
 	}
 	public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
 		this._onErrorListener = onErrorListener;
 	}
 	public void setOnPauseListener(OnPauseListener onPauseListener) {
 		this._onPauseListener = onPauseListener;
 	}
 	public void setOnPlayListener(OnPlayListener onPlayListener) {
 		this._onPlayListener = onPlayListener;
 	}
 	public void setOnStopListener(OnStopListener onStopListener) {
 		this._onStopListener = onStopListener;
 	}
 	public void setOnSeekListener(OnSeekListener onSeekListener) {
 		this._onSeekListener = onSeekListener;
 	}
 	public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
 		this._onCompletionListener = onCompletionListener;
 	}
 
 	/* external functions */
 
 	// specify which podcast file to play and where to start it
 	public boolean prepare(String audioFile, int position) {
 		try {
 			_player.reset();
 			_player.setDataSource(audioFile);
 			_player.prepare();
 			_player.seekTo(position);
 			return true;
 		} catch (IllegalStateException e) {
 			// called if player is not in idle state
 			return false;
 		} catch (IllegalArgumentException e) {
 			return false;
 		} catch (SecurityException e) {
 			return false;
 		} catch (IOException e) {
 			return false;
 		}
 	}
 
 	// change position of podcast
 	public void seekTo(int newPosition) {
 		_player.seekTo(newPosition);
 		// updatethread will pick up new position shortly
 	}
 
 	// if playing, pause. if paused, play.
 	public void playPause(int pauseReason) {
 		if (_player.isPlaying())
 			pause(pauseReason);
 		else
 			play();
 	}
 
 	// if playing, stop. if paused, play.
 	public void playStop() {
 		if (_player.isPlaying())
 			stop();
 		else
 			play();
 	}
 
 	// resume playing the podcast at the current position
 	public void play() {
 		internalPlay();
 	}
 
 	// set a pause reason and pause
 	public void pause(int reason) {
 		_pausingFor.set(reason, true);
 		if (_player.isPlaying())
 			internalPause();
 	}
 
 	// clear a pause reason and play if there's no valid pause reasons
 	public void unpause(int pauseReason) {
 		_pausingFor.set(pauseReason, false);
 		if (!_pausingFor.contains(true))
 			play();
 	}
 
 	// stop the podcast with no intention of resuming in the near future
 	public void stop() {
 		internalStop();
 	}
 
 	/* internal operations and state keeping */
 	private boolean grabAudioFocus() {
 		AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
 		int result = audioManager.requestAudioFocus(_afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
 		if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
 			stop();
 			return false;
 		}
 
 		return true;
 	}
 
 	private void internalPlay() {
 		// if no podcast is prepared, call the unprepared handler and see if that prepares something
 		if (!_prepared) {
 			if (_onUnpreparedListener != null)
 				_onUnpreparedListener.onUnprepared();
 			if (!_prepared)
 				return;
 		}
 
 		if (!grabAudioFocus())
 			return;
 
 		// make sure we're not pausing
 		if (_pausingFor.contains(true))
 			return;
 
 		_player.start();
 
 		// start the seek thread
		if (_updateThread == null || _updateThread.getState() == Thread.State.TERMINATED) {
			_updateThread = new Thread(new UpdatePositionTimerTask());
 			_updateThread.start();
		}
 
 		if (_onPlayListener != null)
 			_onPlayListener.onPlay(_player.getDuration());
 	}
 
 	private void internalPause() {
 		_player.pause();
 
 		// tell the interested party
 		if (_onPauseListener != null)
 			_onPauseListener.onPause(_player.getCurrentPosition());
 	}
 
 	private void internalStop() {
 		// stop playing
 		int position = _player.getCurrentPosition();
 		if (_player.isPlaying())
 			_player.stop();
 
 		// tell Android that we don't want the audio focus
 		AudioManager am = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
 		am.abandonAudioFocus(_afChangeListener);
 
 		// tell the interested party
 		if (_onStopListener != null)
 			_onStopListener.onStop(position);
 	}
 
 
 	public static interface OnPauseListener {
 		public void onPause(int position);
 	}
 	public static interface OnPlayListener {
 		public void onPlay(int duration);
 	}
 	public static interface OnStopListener {
 		public void onStop(int position);
 	}
 	public static interface OnSeekListener {
 		public void onSeek(int position);
 	}
 	public static interface OnCompletionListener {
 		public void onCompletion();
 	}
 	public static interface OnUnpreparedListener {
 		public void onUnprepared();
 	}
 
 	private class UpdatePositionTimerTask implements Runnable {
 		public void run() {
 			int _lastPosition = 0;
 			while (true) {
 				// we're interrupted when it's time to stop
 				try {
 					Thread.sleep(250);
 				} catch (InterruptedException e) {
 					return;
 				}
 
 				// if we're not playing, the pause/stop event sent the current time
 				// and we don't need to update until we're restarted
 				if (!_player.isPlaying())
 					return;
 
 				int currentPosition = _player.getCurrentPosition();
 				if (_onSeekListener != null && _lastPosition / 1000 != currentPosition / 1000)
 					_onSeekListener.onSeek(currentPosition);
 				_lastPosition = currentPosition;
 			}
 		}
 	}
 }
