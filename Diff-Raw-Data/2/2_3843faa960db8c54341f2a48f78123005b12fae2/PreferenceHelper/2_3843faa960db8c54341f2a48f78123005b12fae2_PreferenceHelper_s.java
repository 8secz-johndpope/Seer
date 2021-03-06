 package com.nolanlawson.logcat.helper;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.preference.PreferenceManager;
 
 import com.nolanlawson.logcat.R;
 import com.nolanlawson.logcat.data.ColorScheme;
 import com.nolanlawson.logcat.util.UtilLogger;
 
 public class PreferenceHelper {
 	
 	private static float textSize = -1;
 	private static Boolean showTimestampAndPid = null;
 	private static ColorScheme colorScheme = null;
 	
 	private static UtilLogger log = new UtilLogger(PreferenceHelper.class);
 	
 	private static final String WIDGET_EXISTS_PREFIX = "widget_";
 	
 	public static boolean getWidgetExistsPreference(Context context, int appWidgetId) {
 		
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		String widgetExists = WIDGET_EXISTS_PREFIX.concat(Integer.toString(appWidgetId));
 		
 		return sharedPrefs.getBoolean(widgetExists, false);
 	}
 	
 	public static void setWidgetExistsPreference(Context context, int[] appWidgetIds) {
 		
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		
 		Editor editor = sharedPrefs.edit();
 		
 		for (int appWidgetId : appWidgetIds) {
 			String widgetExists = WIDGET_EXISTS_PREFIX.concat(Integer.toString(appWidgetId));
 			editor.putBoolean(widgetExists, true);
 		}
 		
 		editor.commit();
 		
 		
 	}
 	
 	public static int getLogLinePeriodPreference(Context context) {
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		
 		String defaultValue = context.getText(R.string.pref_log_line_period_default).toString();
 		
 		String intAsString = sharedPrefs.getString(context.getText(R.string.pref_log_line_period).toString(), defaultValue);
 		
 		try { 
 			return Integer.parseInt(intAsString);
 		} catch (NumberFormatException e) {
 			return Integer.parseInt(defaultValue);
 		}
 	}
 	
 	public static void setLogLinePeriodPreference(Context context, int value) {
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		Editor editor = sharedPrefs.edit();
 		
 		editor.putString(context.getText(R.string.pref_log_line_period).toString(), Integer.toString(value));
 		
 		editor.commit();
 	}
 	
 	public static float getTextSizePreference(Context context) {
 		
 		if (textSize == -1) {
 		
 			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 			
 			String textSizePref = sharedPrefs.getString(
 					context.getText(R.string.pref_text_size).toString(), 
					context.getText(R.string.text_size_small_value).toString());
 
 			if (textSizePref.equals(context.getText(R.string.text_size_xsmall_value))) {
 				cacheTextsize(context, R.dimen.text_size_xsmall);
 			} else if (textSizePref.equals(context.getText(R.string.text_size_small_value))) {
 				cacheTextsize(context, R.dimen.text_size_small);
 			} else if (textSizePref.equals(context.getText(R.string.text_size_medium_value))) {
 				cacheTextsize(context, R.dimen.text_size_medium);
 			} else if (textSizePref.equals(context.getText(R.string.text_size_large_value))) {
 				cacheTextsize(context, R.dimen.text_size_large);
 			} else { // xlarge
 				cacheTextsize(context, R.dimen.text_size_xlarge);
 			}
 		}
 		
 		return textSize;
 		
 	}
 	
 	public static void clearCache() {
 		textSize = -1;
 		showTimestampAndPid = null;
 		colorScheme = null;
 	}
 	
 	private static void cacheTextsize(Context context, int dimenId) {
 		
 		float unscaledSize = context.getResources().getDimension(dimenId);
 		
 		log.d("unscaledSize is %g", unscaledSize);
 		
 		textSize = unscaledSize;
 	}
 
 	public static boolean getShowTimestampAndPidPreference(Context context) {
 
 		if (showTimestampAndPid == null) {
 			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 			
 			showTimestampAndPid = sharedPrefs.getBoolean(context.getText(R.string.pref_show_timestamp).toString(), true);
 		}	
 		
 		return showTimestampAndPid;
 		
 	}
 	
 	public static boolean getExpandedByDefaultPreference(Context context) {
 
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		
 		return sharedPrefs.getBoolean(
 				context.getText(R.string.pref_expanded_by_default).toString(), false);
 	}
 	public static void setFirstRunPreference(Context context, boolean bool) {
 
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		Editor editor = sharedPrefs.edit();
 		
 		editor.putBoolean(context.getString(R.string.first_run), bool);
 		
 		editor.commit();
 
 	}
 	public static boolean getFirstRunPreference(Context context) {
 
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		return sharedPrefs.getBoolean(context.getString(R.string.first_run), true);
 
 	}
 	
 	public static ColorScheme getColorScheme(Context context) {
 		
 		if (colorScheme == null) {
 		
 			if (!DonateHelper.isDonateVersionInstalled(context)) {
 				colorScheme = ColorScheme.Dark; // hard-coded in free version
 			} else {
 				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 				String colorSchemeName = sharedPrefs.getString(
 						context.getText(R.string.pref_theme).toString(), ColorScheme.Dark.name());
 				
 				colorScheme = Enum.valueOf(ColorScheme.class, colorSchemeName);
 			}
 		}
 		
 		return colorScheme;
 		
 	}
 		
 	public static void setColorScheme(Context context, ColorScheme colorScheme) {
 		
 		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
 		Editor editor = sharedPrefs.edit();
 		
 		editor.putString(context.getString(R.string.pref_theme).toString(), colorScheme.name());
 		
 		editor.commit();
 		
 	}
 	
 }
