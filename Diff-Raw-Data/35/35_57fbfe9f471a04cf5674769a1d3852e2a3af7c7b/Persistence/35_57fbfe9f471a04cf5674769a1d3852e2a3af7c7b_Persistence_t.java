 /*
  * Copyright (C) 2010-2011, Inclusive Design Research Centre
  */
 
 package ca.idi.tekla.util;
 
 import android.bluetooth.BluetoothAdapter;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.preference.PreferenceManager;
 
 public class Persistence {
 	
 	// Deprecated preferences
 	//public static final String PREF_SCAN_DELAY_STRING = "scan_delay";
 	//public static final String PREF_SHIELD_VERSION = "shield_version";
 
 	public static final String PREF_VOICE_INPUT = "voice_input";
 	public static final String PREF_VARIANTS = "variants";
 	public static final String PREF_VARIANTS_KEY = "variants_key";
 	public static final String PREF_PERSISTENT_KEYBOARD = "persistent_keyboard";
 	public static final String PREF_AUTOHIDE_TIMEOUT = "autohide_timeout";
 	public static final String PREF_FULL_RESET_TIMEOUT = "full_reset_timeout";
 	public static final String PREF_CONNECT_TO_SHIELD = "shield_connect";
 	public static final String PREF_SHIELD_ADDRESS = "shield_address";
 	public static final String PREF_FULLSCREEN_SWITCH = "fullscreen_switch";
	public static final String PREF_SPEAKERPHONE_SWITCH = "speakerphone_switch";
 	public static final String PREF_SELF_SCANNING = "self_scanning";
 	public static final String PREF_INVERSE_SCANNING = "inverse_scanning";
 	public static final String PREF_SCAN_DELAY_INT = "scan_delay_int";
 	public static final long DEFAULT_FULL_RESET_TIMEOUT = 3; //second
 	public static final int DEFAULT_SCAN_DELAY = 1000;
 	public static final int MAX_SCAN_DELAY = 3000;
 	public static final int MIN_SCAN_DELAY = 250;
 	public static final int AUTOHIDE_NULL = -999;
 	public static final int NEVER_AUTOHIDE = -1;
 	
 	
 	private boolean mScreenOn, mInverseScanningChanged, mVariantsShowing;
 	
 	private SharedPreferences shared_prefs;
 	private SharedPreferences.Editor prefs_editor;
 	
 	public Persistence(Context context) {
 		
 		shared_prefs = PreferenceManager.getDefaultSharedPreferences(context);
 		prefs_editor = shared_prefs.edit();
 		mVariantsShowing = false;
 		
 	}
 	
 	public boolean isScreenOn() {
 		return mScreenOn;
 	}
 	
 	public void setScreenOn() {
 		mScreenOn = true;
 	}
 	
 	public void setScreenOff() {
 		mScreenOn = false;
 	}
 	
 	public boolean isVoiceInputEnabled() {
 		return shared_prefs.getBoolean(PREF_VOICE_INPUT, false);
 	}
 
 	public boolean isVariantsKeyEnabled() {
 		return shared_prefs.getBoolean(PREF_VARIANTS_KEY, false);
 	}
 	
 	public boolean isVariantsOn() {
 		return shared_prefs.getBoolean(PREF_VARIANTS, false);
 	}
 	
 	public void setVariantsOn() {
 		prefs_editor.putBoolean(PREF_VARIANTS, true);
 		prefs_editor.commit();
 	}
 
 	public void setVariantsOff() {
 		prefs_editor.putBoolean(PREF_VARIANTS, false);
 		prefs_editor.commit();
 	}
 	
 	public void setVariantsShowing (boolean showing) {
 		mVariantsShowing = showing;
 	}
 
 	public boolean isVariantsShowing () {
 		return mVariantsShowing;
 	}
 
 	public boolean isPersistentKeyboardEnabled() {
 		return shared_prefs.getBoolean(PREF_PERSISTENT_KEYBOARD, false);
 	}
 
 	public void setNavigationKeyboardTimeout(int timeout) {
 		prefs_editor.putInt(PREF_AUTOHIDE_TIMEOUT, timeout);
 		prefs_editor.commit();
 	}
 	
 	public void setNeverHideNavigationKeyboard () {
 		prefs_editor.putInt(PREF_AUTOHIDE_TIMEOUT, NEVER_AUTOHIDE);
 		prefs_editor.commit();
 	}
 
 	public int getNavigationKeyboardTimeout() {
 		return shared_prefs.getInt(PREF_AUTOHIDE_TIMEOUT, NEVER_AUTOHIDE);
 	}
 	
 	public void setFullResetTimeout(long timeout) {
 		prefs_editor.putLong(PREF_FULL_RESET_TIMEOUT, timeout);
 		prefs_editor.commit();
 	}
 	
 	public long getFullResetTimeout() {
 		return shared_prefs.getLong(PREF_FULL_RESET_TIMEOUT,DEFAULT_FULL_RESET_TIMEOUT);
 	}
 
 	public void setConnectToShield(boolean shieldConnect) {
 		prefs_editor.putBoolean(PREF_CONNECT_TO_SHIELD, shieldConnect);
 		prefs_editor.commit();
 	}
 
 	public boolean shouldConnectToShield() {
 		return shared_prefs.getBoolean(PREF_CONNECT_TO_SHIELD, false);
 	}
 
 	public void setShieldAddress(String shieldAddress) {
 		prefs_editor.putString(PREF_SHIELD_ADDRESS, shieldAddress);
 		prefs_editor.commit();
 	}
 
 	public String getShieldAddress() {
 		String mac = shared_prefs.getString(PREF_SHIELD_ADDRESS, "");
 		return BluetoothAdapter.checkBluetoothAddress(mac)? mac:null;
 	}
 
 	public boolean isFullscreenSwitchEnabled() {
 		return shared_prefs.getBoolean(PREF_FULLSCREEN_SWITCH, false);
 	}
 
 	public boolean isSelfScanningEnabled() {
 		return shared_prefs.getBoolean(PREF_SELF_SCANNING, false);
 	}
 
 	public boolean isInverseScanningEnabled() {
 		return shared_prefs.getBoolean(PREF_INVERSE_SCANNING, false);
 	}
 
 	public void setInverseScanningChanged() {
 		mInverseScanningChanged = true;
 	}
 	
 	public void unsetInverseScanningChanged() {
 		mInverseScanningChanged = false;
 	}
 	
 	public boolean isInverseScanningChanged() {
 		return mInverseScanningChanged;
 	}
 	
 	public boolean isScanningEnabled() {
 		return  isSelfScanningEnabled() || isInverseScanningEnabled();
 	}
 
 	public void setScanDelay(int delay) {
 		prefs_editor.putInt(PREF_SCAN_DELAY_INT, delay);
 		prefs_editor.commit();
 	}
 
 	public int getScanDelay() {
 		return shared_prefs.getInt(PREF_SCAN_DELAY_INT, DEFAULT_SCAN_DELAY);
 	}
 
 }
