 /**
  * This file is part of Simple Last.fm Scrobbler.
  *
  *     http://code.google.com/p/a-simple-lastfm-scrobbler/
  *
  * Copyright 2011 Simple Last.fm Scrobbler Team
  * Copyright 2013 Shahar Kosti (shahar.kosti@gmail.com)
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
 
 package com.adam.aslfms.receiver;
 
 import android.content.Context;
 import android.os.Bundle;
 import com.adam.aslfms.util.Track;
 import com.adam.aslfms.util.Util;
 
 /**
  * A BroadcastReceiver for intents sent by the LG Optimus 4X P880 music player
  *
  * @see AbstractPlayStatusReceiver
  *
  * @author kshahar
  * @since 1.4.4
  */
 public class LgOptimus4xReceiver extends AbstractPlayStatusReceiver {
 
     static final String APP_PACKAGE = "com.lge.music";
     static final String APP_NAME = "LG Music Player";
 
     static final String ACTION_LGE_START = "com.lge.music.metachanged";
     static final String ACTION_LGE_PAUSERESUME = "com.lge.music.playstatechanged";
     static final String ACTION_LGE_STOP = "com.lge.music.endofplayback";
 
     protected void parseIntent(Context ctx, String action, Bundle bundle)
             throws IllegalArgumentException {
 
         MusicAPI musicAPI = MusicAPI.fromReceiver(ctx, APP_NAME, APP_PACKAGE, null,
                 false);
         setMusicAPI(musicAPI);
 
         if (action == ACTION_LGE_START) {
             setState(Track.State.START);
             Track.Builder b = new Track.Builder();
             b.setMusicAPI(musicAPI);
             b.setWhen(Util.currentTimeSecsUTC());
 
             b.setArtist(bundle.getString("artist"));
             b.setAlbum(bundle.getString("album"));
             b.setTrack(bundle.getString("track"));
            b.setDuration(bundle.getInt("duration") / 1000);
 
             // throws on bad data
             setTrack(b.build());
         }
         else if (action == ACTION_LGE_PAUSERESUME) {
             boolean playing = bundle.getBoolean("isPlaying");
             Track.State state =
                     (playing) ? (Track.State.RESUME) : (Track.State.PAUSE);
             setState(state);
             setTrack(Track.SAME_AS_CURRENT);
         }
         else if (action == ACTION_LGE_STOP) {
            // TODO: currently this action is not received, because the bundle is null and
            // AbstractPlayStatusReceiver ignores this case.
             setState(Track.State.COMPLETE);
             setTrack(Track.SAME_AS_CURRENT);
         }
     }
 }
