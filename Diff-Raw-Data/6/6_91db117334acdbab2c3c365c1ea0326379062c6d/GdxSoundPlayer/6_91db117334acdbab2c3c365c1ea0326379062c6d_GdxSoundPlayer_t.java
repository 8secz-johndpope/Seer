 package net.catacombsnatch.game.core.sound;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import com.badlogic.gdx.Gdx;
 import com.badlogic.gdx.audio.Music;
 import com.badlogic.gdx.audio.Sound;
 
 public class GdxSoundPlayer implements ISoundPlayer {
 
 	private float listenerX = 0.0f;
 	private float listenerY = 0.0f;
 
 	private float maxSoundDistance = 200.0f;
 
 	private boolean paused = false;
 	private boolean muted = false;
 	private float musicVolume = 0.5f;
 	private float soundVolume = 1.0f;
 
 	private long timeBetweenSongs = 10; // in seconds
 	private long songEnded = 0;
 
 	private Map<String, Music> musicMap;
 	private Map<String, Sound> soundsMap;
 
 	private List<Music> backgroundMusicList;
 	private int backgroundPlaying;
 
 	public GdxSoundPlayer() {
 		// Initialize all lists
 		soundsMap = new HashMap<String, Sound>();
 		musicMap = new HashMap<String, Music>();
 		backgroundMusicList = new ArrayList<Music>();
 
 		// Load music
 		loadMusic( Sounds.TITLE_THEME );
 		loadMusic( Sounds.END_THEME );
 		loadMusic( Sounds.BACKGROUND_TRACK_1 );
 		loadMusic( Sounds.BACKGROUND_TRACK_2 );
 		loadMusic( Sounds.BACKGROUND_TRACK_3 );
 		loadMusic( Sounds.BACKGROUND_TRACK_4 );
 
 		// Load sounds
 		loadSound( Sounds.SOUND_SHOT_1 );
 		loadSound( Sounds.SOUND_EXPLOSION );
 	}
 
 	private void loadMusic( Sounds music ) {
 		Music file = Gdx.audio.newMusic( Gdx.files.internal( "sound/" + music.name + ".ogg" ) );
 
 		// If background track, add to playlist
 		if ( music.name.toLowerCase().startsWith( "background" ) ) backgroundMusicList.add( file );
 
 		musicMap.put( music.name, file );
 	}
 
 	private void loadSound( Sounds sound ) {
 		soundsMap.put( sound.name, Gdx.audio.newSound( Gdx.files.internal( "sound/" + sound.name + ".wav" ) ) );
 	}
 
 	private Music getMusic( Sounds music ) {
 		return musicMap.get( music.name );
 	}
 
 	public void startTitleMusic() {
 		stopBackgroundMusic();
 		stopEndMusic();
 
 		Music titleMusic = getMusic( Sounds.TITLE_THEME );
 
 		if ( musicVolume > 0.0f && !titleMusic.isPlaying() && !paused ) {
 			titleMusic.setLooping( true );
 			titleMusic.setVolume( musicVolume );
 			titleMusic.play();
 		}
 	}
 
 	public void stopTitleMusic() {
 		Music titleMusic = getMusic( Sounds.TITLE_THEME );
 
 		if ( titleMusic.isPlaying() ) titleMusic.stop();
 	}
 
 	public void startEndMusic() {
 		stopBackgroundMusic();
 		stopTitleMusic();
 
 		Music endMusic = getMusic( Sounds.END_THEME );
 
 		if ( musicVolume > 0.0f && !endMusic.isPlaying() && !paused ) {
 			endMusic.setLooping( true );
 			endMusic.setVolume( musicVolume );
 			endMusic.play();
 		}
 	}
 
 	public void stopEndMusic() {
 		Music endMusic = getMusic( Sounds.END_THEME );
 
 		if ( endMusic.isPlaying() ) endMusic.stop();
 	}
 
 	public void startBackgroundMusic() {
 		stopTitleMusic();
 		stopEndMusic();
 
 		if ( musicVolume > 0.0f && backgroundMusicList.size() > 0 && !paused ) {
 			if ( backgroundPlaying < 0 ) backgroundPlaying = 0;
 
 			Music music = backgroundMusicList.get( backgroundPlaying );
 
 			if ( !music.isPlaying() ) {
 				if ( songEnded == 0 ) {
 					songEnded = System.currentTimeMillis();
 					backgroundPlaying = (backgroundPlaying + 1) >= backgroundMusicList.size() ? 0 : backgroundPlaying + 1;
 
 					music = backgroundMusicList.get( backgroundPlaying );
 				}
 
 				if ( songEnded == 0 || (songEnded + (timeBetweenSongs * 1000)) < System.currentTimeMillis() ) {
 					music.setLooping( false );
 					music.setVolume( musicVolume );
 					music.play();
 
 					songEnded = 0;
 				}
 			}
 		}
 	}
 
 	public void stopBackgroundMusic() {
 		if ( backgroundPlaying < 0 ) return;
 
 		for ( Music m : backgroundMusicList )
 			if ( m.isPlaying() ) m.stop();
 
 		backgroundPlaying = -1;
 	}
 
 	public void pauseBackgroundMusic() {
 		paused = true;
 
 		if ( backgroundPlaying >= 0 ) backgroundMusicList.get( backgroundPlaying ).pause();
 
 		getMusic( Sounds.TITLE_THEME ).pause();
 		getMusic( Sounds.END_THEME ).pause();
 	}
 
 	public void resumeBackgroundMusic() {
 		paused = true;
 
 		if ( backgroundPlaying >= 0 ) backgroundMusicList.get( backgroundPlaying ).play();
 
 		getMusic( Sounds.TITLE_THEME ).play();
 		getMusic( Sounds.END_THEME ).play();
 	}
 
 	public void setListenerPosition( float x, float y ) {
 		listenerX = x;
 		listenerY = y;
 	}
 
 	public boolean playSound( String soundName, float x, float y ) {
 		return playSound( soundName, x, y, false );
 	}
 
 	public boolean playSound( String soundName, float x, float y, boolean blocking ) {
 		if ( soundVolume > 0.0f ) {
 			if ( soundsMap.containsKey( soundName ) ) {
 				// todo
 
 				// angle -> pan
 				double angle = Math.toDegrees( Math.atan2( x - listenerX, y - listenerY ) );
 				if ( angle < 0 ) angle += 360;
 
 				float pan = (float) Math.sin( Math.toRadians( angle ) );
 
 				// distance
 				double difx = (x - listenerX) * (x - listenerX);
 				double dify = (y - listenerY) * (y - listenerY);
 				float distance = (float) Math.sqrt( difx + dify );
 				System.out.println( "dis " + distance );
 
				float distanceVolume = soundVolume;
				distanceVolume = soundVolume - (distance / maxSoundDistance);
				if ( distanceVolume > soundVolume ) distanceVolume = soundVolume;
 				if ( distanceVolume < 0.0f ) distanceVolume = 0.0f;
 				System.out.println( "disVol " + distanceVolume );
 
 				soundsMap.get( soundName ).play( distanceVolume, 1, pan );
 
 				return true;
 			}
 		}
 
 		return false;
 	}
 
 	public void shutdown() {
 		// unloading Title music
 		stopTitleMusic();
 		getMusic( Sounds.TITLE_THEME ).dispose();
 
 		// unloading End music
 		stopEndMusic();
 		getMusic( Sounds.END_THEME ).dispose();
 
 		// unloading Background music
 		stopBackgroundMusic();
 
 		for ( Music m : backgroundMusicList ) {
 			if ( m.isPlaying() ) m.stop();
 			m.dispose();
 		}
 
 		// unloading sounds
 		if ( soundsMap.size() > 0 ) {
 			for ( Sound s : soundsMap.values() ) {
 				s.stop();
 				s.dispose();
 			}
 		}
 	}
 
 	public boolean isMuted() {
 		return muted;
 	}
 
 	public void setMuted( boolean muted ) {
 		this.muted = muted;
 	}
 
 }
