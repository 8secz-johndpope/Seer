 /*
  * ServeStream: A HTTP stream browser/player for Android
  * Copyright 2010 William Seemann
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package net.sourceforge.servestream;
 
 import java.util.ArrayList;
 
 import net.sourceforge.servestream.custom.CustomVideoView;
 import net.sourceforge.servestream.service.MusicService;
 import net.sourceforge.servestream.utils.PlaylistHandler;
 import net.sourceforge.servestream.utils.PreferenceConstants;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.ComponentName;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.media.MediaPlayer;
 import android.media.MediaPlayer.OnCompletionListener;
 import android.media.MediaPlayer.OnErrorListener;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.IBinder;
 import android.preference.PreferenceManager;
 import android.view.Display;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.WindowManager;
 import android.view.View.OnClickListener;
 import android.widget.MediaController;
 import android.widget.Toast;
 
 public class StreamMediaActivity extends Activity {
 	public final static String TAG = "ServeStream.StreamMediaActivity";
 	
 	private int displayWidth = 0;
 	private int displayHeight = 0;
 	
     private ArrayList<String> m_mediaFiles = null;
 	private int m_currentMediaFileIndex = 0;
 	private CustomVideoView m_videoView = null;
     private String m_path = "";
 	private int mediaPosition = 0;
 	
 	private MediaController m_mediaController = null;
 	private SharedPreferences preferences = null;	
 	
 	private MusicService m_boundService;
 
 	private ServiceConnection connection = new ServiceConnection() {
 	    public void onServiceConnected(ComponentName className, IBinder service) {
 	        // This is called when the connection with the service has been
 	        // established, giving us the service object we can use to
 	        // interact with the service.  Because we have bound to a explicit
 	        // service that we know is running in our own process, we can
 	        // cast its IBinder to a concrete class and directly access it.
 	        m_boundService = ((MusicService.MusicBinder)service).getService();
 	    }
 
 	    public void onServiceDisconnected(ComponentName className) {
 	        // This is called when the connection with the service has been
 	        // unexpectedly disconnected -- that is, its process crashed.
 	        // Because it is running in our same process, we should never
 	        // see this happen.
 	        m_boundService = null;
 	    }
 	};
 	
 	@Override
 	public void onPause() {
 		super.onPause();
 		
 		/*if (m_videoView.isPlaying()) {
 		    m_videoView.stopPlayback();
             try {
 			    m_boundService.startMusicInBackground(m_mediaFiles, m_currentMediaFileIndex, 
 					    m_videoView.getCurrentPosition());
 		    } catch (Exception e) {
 			    e.printStackTrace();
 		    }
 		}*/
 	}	
 	
 	@Override
 	public void onResume() {
 		super.onResume();
 		
 		/*if (m_boundService != null) {
 		    try {
 			    m_boundService.stopMusicInBackground();
 		    } catch (Exception e) {
 			    e.printStackTrace();
 		    }
 		
 		    m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_boundService.getCurrentMediaFileIndex())));
 		    m_videoView.seekTo(m_boundService.getCurrentMediaFileIndex());
 		    m_videoView.start();
 		}*/
 	}
 	
     @Override
     public void onCreate(Bundle icicle) {
         super.onCreate(icicle);
         setContentView(R.layout.acc_streammedia);
         
 		this.setTitle(String.format("%s: %s",
 				getResources().getText(R.string.app_name),
 				getResources().getText(R.string.title_stream_play))); 
         
 		// get the phone's display width and height
         Display display = getWindowManager().getDefaultDisplay();
         displayWidth = display.getWidth();
         displayHeight = display.getHeight();
 		
         String stream = getIntent().getExtras().getString("net.sourceforge.servestream.TargetStream");
         
 		preferences = PreferenceManager.getDefaultSharedPreferences(this);
         
         if (isPlaylist(stream)) {
             PlaylistHandler playlistHandler = new PlaylistHandler(stream);
             playlistHandler.buildPlaylist();
             m_mediaFiles = playlistHandler.getPlayListFiles();
         } else {
         	m_mediaFiles = new ArrayList<String>();
         	m_mediaFiles.add(stream);
         }
         
         m_videoView = (CustomVideoView) findViewById(R.id.surface_view);
         m_videoView.setDimensions(displayWidth, displayHeight);
         
         // attempt to get data from before device configuration change
         Bundle returnData = (Bundle) getLastNonConfigurationInstance();
         
         if (returnData == null) {
            if (m_mediaFiles.size() > 0) {
     			new AlertDialog.Builder(StreamMediaActivity.this)
     			.setTitle(R.string.cannot_play_media_title)
     			.setMessage("The selected playlist file has no valid song entries.")
     			.setPositiveButton(R.string.cannot_play_media_pos, new DialogInterface.OnClickListener() {
     				public void onClick(DialogInterface dialog, int which) {
     					finish();
     				}
     				}).create().show();
             } else {
                 m_mediaController = new MediaController(this, true);
                 m_mediaController.setPrevNextListeners(null, null);
                 m_videoView.setMediaController(m_mediaController);
             
                 m_videoView.setOnErrorListener(m_onErrorListener);
                 m_videoView.setOnCompletionListener(m_onCompletionListener);
                 startSong(m_currentMediaFileIndex);
             }
         } else {
         	m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
         	m_videoView.seekTo(mediaPosition);
         	m_videoView.start();
         	Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
         }
     }
     
 	@Override
 	public void onStart() {
 		super.onStart();
 		
 		// connect with manager service to find all bridges
 		// when connected it will insert all views
 		//bindService(new Intent(this, MusicService.class), connection, Context.BIND_AUTO_CREATE);
 	}
 	
 	@Override
 	public void onStop() {
 		super.onStop();
 		
         //unbindService(connection);
 	}
     
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		super.onCreateOptionsMenu(menu);
 
 		MenuItem settings = menu.add(R.string.list_menu_settings);
 		settings.setIcon(android.R.drawable.ic_menu_preferences);
 		settings.setIntent(new Intent(StreamMediaActivity.this, SettingsActivity.class));
 
 		MenuItem help = menu.add(R.string.title_help);
 		help.setIcon(android.R.drawable.ic_menu_help);
 		help.setIntent(new Intent(StreamMediaActivity.this, HelpActivity.class));
 
 		return true;
 	}
     
     @Override
     public Object onRetainNonConfigurationInstance() {
         mediaPosition = m_videoView.getCurrentPosition();
  
         // Build bundle to save data for return
         Bundle data = new Bundle();
         data.putString("LOCATION", m_path);
         data.putInt("POSITION", mediaPosition);
       
         return data;
     }
 	
     @Override
 	public void onConfigurationChanged(Configuration newConfig) {
 		super.onConfigurationChanged(newConfig);
 		
 		if (m_videoView.isInTouchMode()) {
 			m_mediaController.hide();
 		}
 		
 		// get new window size on orientation change
         Display display = getWindowManager().getDefaultDisplay();
         displayWidth = display.getWidth();
         displayHeight = display.getHeight();
 		
 	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
 	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
 	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
 	        m_videoView.setDimensions(displayWidth, displayHeight);
 	        m_videoView.getHolder().setFixedSize(displayWidth, displayHeight);
 	    } else {
 	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
 	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
 	        m_videoView.setDimensions(displayWidth, displayHeight);
 	        m_videoView.getHolder().setFixedSize(displayWidth, displayHeight);
 	    }
 	}
     
     private OnCompletionListener m_onCompletionListener = new OnCompletionListener() {
 		public void onCompletion(MediaPlayer mp) {
 
 			// if repeat preference is set to one, play the media file again
 			if (preferences.getString(PreferenceConstants.REPEAT, "Off").equals("One")) {
 				startSong(m_currentMediaFileIndex);
 				return;
 			}
 			
 			m_currentMediaFileIndex++;
 			
 			if (m_currentMediaFileIndex == m_mediaFiles.size()) {
 				if (preferences.getString(PreferenceConstants.REPEAT, "Off").equals("All")) {
 					startSong(0);
 					return;
 				}
 				finish();
 			} else {
                 startSong(m_currentMediaFileIndex);
 			}
 		}
     };
 	
     private OnErrorListener m_onErrorListener = new OnErrorListener() {
 
 		public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
 			new AlertDialog.Builder(StreamMediaActivity.this)
 			.setTitle(R.string.cannot_play_media_title)
 			.setMessage(R.string.cannot_play_media_message)
 			.setPositiveButton(R.string.cannot_play_media_pos, new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					finish();
 				}
 				}).create().show();
 			return true;
 		}
     };
     
     private class previousOnClickListener implements OnClickListener {
 
 		public void onClick(View arg0) {
             startSong(m_currentMediaFileIndex - 1);
 		}
     }
     
     private class nextOnClickListener implements OnClickListener {
 
 		public void onClick(View arg0) {
             startSong(m_currentMediaFileIndex + 1);
 		}
     }
     
     /**
      * Sets the state of the previous and next buttons on the media player
      */
     private void setPlayerButtonStates() {
     	if (!(m_mediaFiles.size() == 1)) {
 	 	    if (m_currentMediaFileIndex == 0) {
                 m_mediaController.setPrevNextListeners(new nextOnClickListener(), null);
 		    } else if (m_currentMediaFileIndex == m_mediaFiles.size() - 1) {
                 m_mediaController.setPrevNextListeners(null, new previousOnClickListener());   	
 		    } else {
                 m_mediaController.setPrevNextListeners(new nextOnClickListener(), new previousOnClickListener());
 		    }
     	}
     }
     
     private void startSong(int playlistFileIndex) {
    
     	m_currentMediaFileIndex = playlistFileIndex;
 		setPlayerButtonStates();
     	
 		m_videoView.stopPlayback();
         m_videoView.setVideoURI(Uri.parse(m_mediaFiles.get(m_currentMediaFileIndex)));
         m_videoView.start();
         Toast.makeText(StreamMediaActivity.this, "Now Playing: " + m_mediaFiles.get(m_currentMediaFileIndex), Toast.LENGTH_LONG).show();
     }
     
     /**
      * Checks if a file is a playlist file
      * 
      * @param mediaFileName The file to check
      * @return boolean True if the file is a playlist, false otherwise
      */
     private boolean isPlaylist(String mediaFileName) {
     	if (mediaFileName.length() > 4) {
     	    if (mediaFileName.substring(mediaFileName.length() - 4, mediaFileName.length()).equalsIgnoreCase(".m3u")) {
     	    	return true;
     	    }
     	}
     	
     	return false;
     }
 }
