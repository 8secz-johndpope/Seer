 /**
  * Mupen64PlusAE, an N64 emulator for the Android platform
  * 
  * Copyright (C) 2013 Paul Lamb
  * 
  * This file is part of Mupen64PlusAE.
  * 
  * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
  * GNU General Public License as published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  * 
  * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
  * not, see <http://www.gnu.org/licenses/>.
  * 
  * Authors: littleguy77
  */
 package paulscode.android.mupen64plusae.persistent;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Locale;
 
 import org.apache.commons.lang.ArrayUtils;
 import org.apache.commons.lang.WordUtils;
 
 import paulscode.android.mupen64plusae.R;
 import paulscode.android.mupen64plusae.input.map.InputMap;
 import paulscode.android.mupen64plusae.input.map.PlayerMap;
 import android.app.Activity;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.preference.PreferenceManager;
 import android.text.TextUtils;
 import android.view.KeyEvent;
 
 /**
  * A convenience class for quickly, safely, and consistently retrieving typed user preferences.
  * <p>
  * <b>Developers:</b> After creating a preference in /res/xml/preferences.xml, you are encouraged to
  * provide convenient access to it by expanding this class. Although this adds an extra step to
  * development, it simplifies code maintenance later since all maintenance can be consolidated to a
  * single file. For example, if you change the name of a key, you only need to update one line in
  * this class:
  * 
  * <pre>
  * {@code
  * myPreference = mPreferences.getString( "myOldKey", "myFallbackValue" );
  *            --> mPreferences.getString( "myNewKey", "myFallbackValue" );
  * }
  * </pre>
  * 
  * Without this class, you would need to search through the entire code base for every call to
  * getString( "myOldKey", ... ) and update each one. This class also ensures that the same fallback
  * value will be used everywhere. A third advantage is that you can easily provide frequently-used
  * "derived" preferences, as in
  * 
  * <pre>
  * {@code
  * isMyPreferenceValid = ( myPreference != null ) && ( myPreference.field != someBadValue );
  * }
  * </pre>
  * 
  * Finally, the cost of looking up a preference value is made up front in this class's constructor,
  * rather than at the point of use. This could improve application performance if the value is used
  * often, such as the frame refresh loop of a game.
  */
 public class UserPrefs
 {
     /** Display names of locales that are both available on the device and translated for Mupen. */
     public final String[] localeNames;
     
     /** Codes of locales that are both available on the device and translated for Mupen. */
     public final String[] localeCodes;
     
     /** The filename of the ROM selected by the user. */
     public final String selectedGame;
     
     /** The filename of the auto-saved session of the ROM selected by the user. */
     public final String selectedGameAutoSavefile;
     
     /** The parent directory containing all save files. */
     public final String gameSaveDir;
     
     /** The subdirectory containing manual save files. */
     public final String manualSaveDir;
     
     /** The subdirectory containing slot save files. */
     public final String slotSaveDir;
     
     /** The subdirectory containing auto save files. */
     public final String autoSaveDir;
     
     /** The selected video plug-in. */
     public final Plugin videoPlugin;
     
     /** The selected audio plug-in. */
     public final Plugin audioPlugin;
     
     /** The selected input plug-in. */
     public final Plugin inputPlugin;
     
     /** The selected Reality Signal Processor. */
     public final Plugin rspPlugin;
     
     /** The selected emulator core. */
     public final Plugin corePlugin;
     
     /** True if the cheats category should be shown in the menu. */
     public final boolean isCheatOptionsShown;
     
     /** True if the touchscreen is enabled. */
     public final boolean isTouchscreenEnabled;
     
     /** True if a custom touchscreen is provided. */
     public final boolean isTouchscreenCustom;
     
     /** The number of frames over which touchscreen is redrawn (0 = disabled). */
     public final int touchscreenRefresh;
     
     /** True if the touchscreen overlay is hidden. */
     public final boolean isTouchscreenHidden;
     
     /** The touchscreen transparency value. */
     public final int touchscreenTransparency;
     
     /** The filename of the selected touchscreen layout. */
     public final String touchscreenLayout;
     
     /** True if Xperia Play touchpad is enabled. */
     public final boolean isTouchpadEnabled;
     
     /** The filename of the selected Xperia Play layout. */
     public final String touchpadLayout;
     
     /** True if Player 1's controller is enabled. */
     public final boolean isInputEnabled1;
     
     /** True if Player 2's controller is enabled. */
     public final boolean isInputEnabled2;
     
     /** True if Player 3's controller is enabled. */
     public final boolean isInputEnabled3;
     
     /** True if Player 4's controller is enabled. */
     public final boolean isInputEnabled4;
     
     /** The button map for Player 1. */
     public final InputMap inputMap1;
     
     /** The button map for Player 2. */
     public final InputMap inputMap2;
     
     /** The button map for Player 3. */
     public final InputMap inputMap3;
     
     /** The button map for Player 4. */
     public final InputMap inputMap4;
     
     /** The player map for multi-player gaming. */
     public final PlayerMap playerMap;
     
     /** True if any type of AbstractController is enabled for Player 1. */
     public final boolean isPlugged1;
     
     /** True if any type of AbstractController is enabled for Player 2. */
     public final boolean isPlugged2;
     
     /** True if any type of AbstractController is enabled for Player 3. */
     public final boolean isPlugged3;
     
     /** True if any type of AbstractController is enabled for Player 4. */
     public final boolean isPlugged4;
     
     /** The set of key codes that are not allowed to be mapped. **/
     public final List<Integer> unmappableKeyCodes;
     
     /** True if the analog is constrained to an octagon. */
     public final boolean isOctagonalJoystick;
     
     /** The screen orientation for the game activity. */
     public final int videoOrientation;
     
     /** The number of frames over which FPS is calculated (0 = disabled). */
     public final int videoFpsRefresh;
     
     /** True if the FPS indicator is displayed. */
     public final boolean isFpsEnabled;
     
     /** True if the video should be stretched. */
     public final boolean isStretched;
     
     /** True if framelimiter is used. */
     public final boolean isFramelimiterEnabled;
     
     /** True if RGBA8888 mode should be used for video. */
     public final boolean isRgba8888;
     
     /** The manually-overridden hardware type, used for flicker reduction. */
     public final int videoHardwareType;
     
     /** True if Gles2N64 video plug-in is enabled. */
     public final boolean isGles2N64Enabled;
     
     /** The maximum frameskip in the gles2n64 library. */
     public final int gles2N64MaxFrameskip;
     
     /** True if auto-frameskip is enabled in the gles2n64 library. */
     public final boolean isGles2N64AutoFrameskipEnabled;
     
     /** True if fog is enabled in the gles2n64 library. */
     public final boolean isGles2N64FogEnabled;
     
     /** True if SaI texture filtering is enabled in the gles2n64 library. */
     public final boolean isGles2N64SaiEnabled;
     
     /** True if force screen clear is enabled in the gles2n64 library. */
     public final boolean isGles2N64ScreenClearEnabled;
     
     /** True if alpha test is enabled in the gles2n64 library. */
     public final boolean isGles2N64AlphaTestEnabled;
     
     /** True if depth test is enabled in the gles2n64 library. */
     public final boolean isGles2N64DepthTestEnabled;
     
     /** True if Gles2Rice video plug-in is enabled. */
     public final boolean isGles2RiceEnabled;
     
     /** True if auto-frameskip is enabled in the gles2rice library. */
     public final boolean isGles2RiceAutoFrameskipEnabled;
     
     /** True if fast texture CRC is enabled in the gles2rice library. */
     public final boolean isGles2RiceFastTextureCrcEnabled;
     
     /** True if fast texture loading is enabled in the gles2rice library. */
     public final boolean isGles2RiceFastTextureLoadingEnabled;
     
     /** True if force texture filter is enabled in the gles2rice library. */
     public final boolean isGles2RiceForceTextureFilterEnabled;
     
     /** True if hi-resolution textures are enabled in the gles2rice library. */
     public final boolean isGles2RiceHiResTexturesEnabled;
     
     /** True if the left and right audio channels are swapped. */
     public final boolean audioSwapChannels;
     
     /** The audio resampling algorithm to use. */
     public final String audioResampleAlg;
     
     // Shared preferences keys and key templates
     private static final String KEYTEMPLATE_MAP_STRING = "inputMapString%1$d";
     private static final String KEYTEMPLATE_SPECIAL_VISIBILITY = "inputSpecialVisibility%1$d";
     // ... add more as needed
     
     // Shared preferences default values
     private static final String DEFAULT_MAP_STRING = "0:22,1:21,2:20,3:19,4:108,12:103,13:102,16:-1,17:-2,18:-3,19:-4";
     private static final boolean DEFAULT_SPECIAL_VISIBILITY = false;
     // ... add more as needed
     
     private final SharedPreferences mPreferences;
     private final Locale mLocale;
     
     /**
      * Instantiates a new user preferences wrapper.
      * 
      * @param context The application context.
      */
     public UserPrefs( Context context )
     {
         AppData appData = new AppData( context );
         mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
         
         // Locale
         String language = mPreferences.getString( "localeOverride", null );
         mLocale = TextUtils.isEmpty( language ) ? Locale.getDefault() : createLocale( language );
         Locale[] availableLocales = Locale.getAvailableLocales();
         String[] values = context.getResources().getStringArray( R.array.localeOverride_values );
         String[] entries = new String[values.length];
         for( int i = values.length - 1; i > 0; i-- )
         {
             Locale locale = createLocale( values[i] );
             
             // Get intersection of languages (available on device) and (translated for Mupen)
             if( ArrayUtils.contains( availableLocales, locale ) )
             {
                 // Get the name of the language, as written natively
                 entries[i] = WordUtils.capitalize( locale.getDisplayName( locale ) );
             }
             else
             {
                 // Remove the item from the list
                 entries = (String[]) ArrayUtils.remove( entries, i );
                 values = (String[]) ArrayUtils.remove( values, i );
             }
         }
         entries[0] = context.getString( R.string.localeOverride_entrySystemDefault );
         localeNames = entries;
         localeCodes = values;
         
         // Files
         selectedGame = mPreferences.getString( "pathSelectedGame", "" );
         gameSaveDir = mPreferences.getString( "pathGameSaves", "" );
         slotSaveDir = gameSaveDir + "/SlotSaves";
         autoSaveDir = gameSaveDir + "/AutoSaves";
         File game = new File( selectedGame );
         manualSaveDir = gameSaveDir + "/" + game.getName();
         selectedGameAutoSavefile = autoSaveDir + "/" + game.getName() + ".sav";
         
         // Plug-ins
         videoPlugin = new Plugin( mPreferences, appData.libsDir, "pluginVideo" );
         audioPlugin = new Plugin( mPreferences, appData.libsDir, "pluginAudio" );
         inputPlugin = new Plugin( mPreferences, appData.libsDir, "pluginInput" );
         rspPlugin = new Plugin( mPreferences, appData.libsDir, "pluginRsp" );
         corePlugin = new Plugin( mPreferences, appData.libsDir, "pluginCore" );
         
         // Play menu
         isCheatOptionsShown = mPreferences.getBoolean( "playShowCheats", false );
         
         // Touchscreen prefs
         isTouchscreenEnabled = mPreferences.getBoolean( "touchscreenEnabled", true );
         touchscreenRefresh = getSafeInt( mPreferences, "touchscreenRefresh", 0 );
         int transparencyPercent = mPreferences.getInt( "touchscreenTransparency", 100 );
         isTouchscreenHidden = transparencyPercent == 0;
         touchscreenTransparency = ( 255 * transparencyPercent ) / 100;
         
         // Xperia PLAY touchpad prefs
         isTouchpadEnabled = mPreferences.getBoolean( "touchpadEnabled", false );
         touchpadLayout = appData.touchpadLayoutsDir + mPreferences.getString( "touchpadLayout", "" );
         
         // Input prefs
         isOctagonalJoystick = mPreferences.getBoolean( "inputOctagonConstraints", true );
         isInputEnabled1 = mPreferences.getBoolean( "inputEnabled1", false );
         isInputEnabled2 = mPreferences.getBoolean( "inputEnabled2", false );
         isInputEnabled3 = mPreferences.getBoolean( "inputEnabled3", false );
         isInputEnabled4 = mPreferences.getBoolean( "inputEnabled4", false );
         
         // Controller prefs
         inputMap1 = new InputMap( getMapString( 1 ) );
         inputMap2 = new InputMap( getMapString( 2 ) );
         inputMap3 = new InputMap( getMapString( 3 ) );
         inputMap4 = new InputMap( getMapString( 4 ) );
         playerMap = new PlayerMap( mPreferences.getString( "playerMap", "" ) );
         
         // Video prefs
         videoOrientation = getSafeInt( mPreferences, "videoOrientation", 0 );
         videoFpsRefresh = getSafeInt( mPreferences, "videoFpsRefresh", 0 );
         isFpsEnabled = videoFpsRefresh > 0;
         videoHardwareType = getSafeInt( mPreferences, "videoHardwareType", -1 );
         isStretched = mPreferences.getBoolean( "videoStretch", false );
         isRgba8888 = mPreferences.getBoolean( "videoRgba8888", false );
         isFramelimiterEnabled = mPreferences.getBoolean( "videoUseFramelimiter", false );
         
         // Video prefs - gles2n64
         isGles2N64Enabled = videoPlugin.name.equals( "libgles2n64.so" );
         int maxFrameskip = getSafeInt( mPreferences, "gles2N64Frameskip", 0 );
         isGles2N64AutoFrameskipEnabled = maxFrameskip < 0;
         gles2N64MaxFrameskip = Math.abs( maxFrameskip );
         isGles2N64FogEnabled = mPreferences.getBoolean( "gles2N64Fog", false );
         isGles2N64SaiEnabled = mPreferences.getBoolean( "gles2N64Sai", false );
         isGles2N64ScreenClearEnabled = mPreferences.getBoolean( "gles2N64ScreenClear", true );
         isGles2N64AlphaTestEnabled = mPreferences.getBoolean( "gles2N64AlphaTest", true );
         isGles2N64DepthTestEnabled = mPreferences.getBoolean( "gles2N64DepthTest", true );
         
         // Video prefs - gles2rice
         isGles2RiceEnabled = videoPlugin.name.equals( "libgles2rice.so" );
         isGles2RiceAutoFrameskipEnabled = mPreferences.getBoolean( "gles2RiceAutoFrameskip", false );
         isGles2RiceFastTextureCrcEnabled = mPreferences.getBoolean( "gles2RiceFastTextureCrc", true );
         isGles2RiceFastTextureLoadingEnabled = mPreferences.getBoolean( "gles2RiceFastTexture", false );
         isGles2RiceForceTextureFilterEnabled = mPreferences.getBoolean( "gles2RiceForceTextureFilter", false );
         isGles2RiceHiResTexturesEnabled = mPreferences.getBoolean( "gles2RiceHiResTextures", true );
         
         // Audio prefs
         audioSwapChannels = mPreferences.getBoolean( "audioSwapChannels", false );
         audioResampleAlg = mPreferences.getString( "audioResampleAlg", "trivial" );
         
         // Determine the touchscreen layout
         boolean isCustom = false;
         String folder = "";
         if( inputPlugin.enabled && isTouchscreenEnabled )
         {
             String layout = mPreferences.getString( "touchscreenLayout", "" );
             if( layout.equals( "Custom" ) )
             {
                 isCustom = true;
                 folder = mPreferences.getString( "pathCustomTouchscreen", "" );
             }
             else
             {
                 // Substitute the "Touch" skin if analog stick is never redrawn
                 if( layout.equals( "Mupen64Plus-AE-Analog" ) && touchscreenRefresh == 0 )
                     layout = "Mupen64Plus-AE-Touch";
                 
                 folder = appData.touchscreenLayoutsDir + layout
                         + mPreferences.getString( "touchscreenSize", "" );
             }
         }
         else if( isFpsEnabled )
         {
             folder = appData.touchscreenLayoutsDir
                     + context.getString( R.string.touchscreenLayout_fpsOnly );
         }
         isTouchscreenCustom = isCustom;
         touchscreenLayout = folder;
         
         // Determine which players are "plugged in"
         isPlugged1 = isInputEnabled1 || isTouchscreenEnabled || isTouchpadEnabled;
         isPlugged2 = isInputEnabled2;
         isPlugged3 = isInputEnabled3;
         isPlugged4 = isInputEnabled4;
         
         // Determine whether controller deconfliction is needed
         int numControllers = 0;
         numControllers += isInputEnabled1 ? 1 : 0;
         numControllers += isInputEnabled2 ? 1 : 0;
         numControllers += isInputEnabled3 ? 1 : 0;
         numControllers += isInputEnabled4 ? 1 : 0;
         boolean isControllerShared = mPreferences.getBoolean( "inputShareController", false );
         playerMap.setEnabled( numControllers > 1 && !isControllerShared );
         
         // Determine the key codes that should not be mapped to controls
         boolean volKeysMappable = mPreferences.getBoolean( "inputVolumeMappable", false );
         List<Integer> unmappables = new ArrayList<Integer>();
         unmappables.add( KeyEvent.KEYCODE_MENU );
         if( AppData.IS_HONEYCOMB )
         {
             // Back key is needed to show/hide the action bar in HC+
             unmappables.add( KeyEvent.KEYCODE_BACK );
         }
         if( !volKeysMappable )
         {
             unmappables.add( KeyEvent.KEYCODE_VOLUME_UP );
             unmappables.add( KeyEvent.KEYCODE_VOLUME_DOWN );
             unmappables.add( KeyEvent.KEYCODE_VOLUME_MUTE );
         }
         unmappableKeyCodes = Collections.unmodifiableList( unmappables );
     }
     
     public void enforceLocale( Activity activity )
     {
         Configuration config = activity.getBaseContext().getResources().getConfiguration();
         if( !mLocale.equals( config.locale ) )
         {
             config.locale = mLocale;
             activity.getBaseContext().getResources().updateConfiguration( config, null );
         }
     }
     
     public boolean getSpecialVisibility( int player )
     {
         String key = String.format( Locale.US, KEYTEMPLATE_SPECIAL_VISIBILITY, player );
         return mPreferences.getBoolean( key, DEFAULT_SPECIAL_VISIBILITY );
     }
     
     public String getMapString( int player )
     {
         String key = String.format( Locale.US, KEYTEMPLATE_MAP_STRING, player );
         return mPreferences.getString( key, DEFAULT_MAP_STRING );
     }
     
     public void putSpecialVisibility( int player, boolean value )
     {
         String key = String.format( Locale.US, KEYTEMPLATE_SPECIAL_VISIBILITY, player );
         putBoolean( key, value );
     }
     
     public void putMapString( int player, String value )
     {
         String key = String.format( Locale.US, KEYTEMPLATE_MAP_STRING, player );
         putString( key, value );
     }
     
     private void putBoolean( String key, boolean value )
     {
         mPreferences.edit().putBoolean( key, value ).commit();
     }
     
     private void putString( String key, String value )
     {
         mPreferences.edit().putString( key, value ).commit();
     }
     
     private Locale createLocale( String code )
     {
         String[] codes = code.split( "_" );
         switch( codes.length )
         {
             case 0:
                 return null;
             case 1:
                 return new Locale( codes[0] );
             case 2:
                 return new Locale( codes[0], codes[1] );
            case 3:
             default:
                 return new Locale( codes[0], codes[1], codes[2] );
         }
     }
     
     /**
      * Gets the selected value of a ListPreference, as an integer.
      * 
      * @param preferences The object containing the ListPreference.
      * @param key The key of the ListPreference.
      * @param defaultValue The value to use if parsing fails.
      * @return The value of the selected entry, as an integer.
      */
     private static int getSafeInt( SharedPreferences preferences, String key, int defaultValue )
     {
         try
         {
             return Integer.parseInt( preferences.getString( key, String.valueOf( defaultValue ) ) );
         }
         catch( NumberFormatException ex )
         {
             return defaultValue;
         }
     }
     
     /**
      * A tiny class containing inter-dependent plug-in information.
      */
     public static class Plugin
     {
         /** The name of the plug-in, with extension, without parent directory. */
         public final String name;
         
         /** The full absolute path name of the plug-in. */
         public final String path;
         
         /** True if the plug-in is enabled. */
         public final boolean enabled;
         
         /**
          * Instantiates a new plug-in meta-info object.
          * 
          * @param prefs The shared preferences containing plug-in information.
          * @param libsDir The directory containing the plug-in file.
          * @param key The shared preference key for the plug-in.
          */
         public Plugin( SharedPreferences prefs, String libsDir, String key )
         {
             name = prefs.getString( key, "" );
             enabled = ( name != null && !name.equals( "" ) );
             path = enabled ? libsDir + name : "dummy";
         }
     }
 }
