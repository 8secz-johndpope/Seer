 /*****************************************************************************
  *  Buzzwords is a family friendly word game for mobile phones.
  *  Copyright (C) 2011 Siramix Team
  *  
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ****************************************************************************/
 package com.buzzwords;
 
 import android.app.Application;
 import android.content.Context;
 import android.media.MediaPlayer;
 import android.util.Log;
 
 /**
  * Class extending the standard android application. This allows us to refer to
  * one GameManager from every activity within BuzzWords.
  * 
  * @author Siramix Labs
  */
 public class BuzzWordsApplication extends Application {
   /**
    * Global Debug constant
    */
  public static final boolean DEBUG = false;
 
   /**
    * logging tag
    */
   public static String TAG = "BuzzWordsApplication";
 
   /**
    * The GameManager for all of BuzzWords
    */
   private GameManager mGameManager;
 
   /**
    * The SoundFXManager for all of BuzzWords
    */
   private SoundManager mSoundManager;
 
   /**
    * MediaPlayer for music
    */
   private MediaPlayer mMediaPlayer;
 
   /**
    * Default constructor
    */
   public BuzzWordsApplication() {
     super();
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "BuzzWordsApplication()");
     }
   }
 
   /**
    * @return a reference to the game manager
    */
   public GameManager getGameManager() {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "GetGameManager()");
     }
     return this.mGameManager;
   }
 
   /**
    * @param gm
    *          - a reference to the game manager
    */
   public void setGameManager(GameManager gm) {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "SetGameManager()");
     }
     this.mGameManager = gm;
   }
 
   /**
    * @return a reference to the sound manager
    */
   public SoundManager getSoundManager() {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "GetSoundManager()");
     }
     return this.mSoundManager;
   }
 
   /**
    * Create a SoundManager object
    * @param context, the context in which to create the sound manager
    * @return the sound manager 
    */
   public SoundManager createSoundManager(Context context) {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "CreateSound Manager(" + context);
     }
     mSoundManager = new SoundManager(context);
     return mSoundManager;
   }
 
   /**
    * @param context in which to create the media player
    * @param id of the music to play
    * @return a reference to the media player
    */
   public MediaPlayer createMusicPlayer(Context context, int id) {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "CreateMusicPlayer(" + context + "," + id + ")");
     }
     mMediaPlayer = MediaPlayer.create(context, id);
     return mMediaPlayer;
   }
 
   /**
    * @return a reference to the current media player
    */
   public MediaPlayer getMusicPlayer() {
     if (BuzzWordsApplication.DEBUG) {
       Log.d(TAG, "GetMusicPlayer()");
     }
     return mMediaPlayer;
   }
 
 }
