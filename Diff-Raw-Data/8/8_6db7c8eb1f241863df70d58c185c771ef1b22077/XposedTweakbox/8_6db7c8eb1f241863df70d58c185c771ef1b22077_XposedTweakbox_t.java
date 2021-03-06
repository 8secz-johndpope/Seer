 package de.robv.android.xposed.mods.tweakbox;
 
 import static de.robv.android.xposed.XposedHelpers.getSurroundingThis;
 import static de.robv.android.xposed.XposedHelpers.setFloatField;
 import static de.robv.android.xposed.XposedHelpers.setIntField;
 
 import java.lang.reflect.AccessibleObject;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.Field;
 import java.lang.reflect.Method;
 import java.util.Locale;
 
 import android.app.AndroidAppHelper;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.content.res.XResources;
 import android.graphics.Color;
 import android.graphics.PixelFormat;
 import android.graphics.drawable.ColorDrawable;
 import android.os.SystemProperties;
 import android.os.Vibrator;
 import android.telephony.SignalStrength;
 import android.util.DisplayMetrics;
 import android.view.Display;
 import android.view.WindowManager;
 import android.widget.TextView;
 import de.robv.android.xposed.IXposedHookInitPackageResources;
 import de.robv.android.xposed.IXposedHookLoadPackage;
 import de.robv.android.xposed.IXposedHookZygoteInit;
 import de.robv.android.xposed.XC_MethodHook;
 import de.robv.android.xposed.XC_MethodReplacement;
 import de.robv.android.xposed.XposedBridge;
 import de.robv.android.xposed.XposedHelpers;
 import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
 import de.robv.android.xposed.callbacks.XC_LayoutInflated;
 import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
 import de.robv.android.xposed.callbacks.XCallback;
 
 
 public class XposedTweakbox implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
 	public static final String MY_PACKAGE_NAME = XposedTweakbox.class.getPackage().getName();
 	private static SharedPreferences pref;
 	private static int signalStrengthBars = 4;
 	private static final int ANIM_SETTING_ON = 0x01;
 	private static final int ANIM_SETTING_OFF = 0x10;
 	
 	@Override
 	public void initZygote(StartupParam startupParam) {
 		pref = AndroidAppHelper.getDefaultSharedPreferencesForPackage(MY_PACKAGE_NAME);
 
 		try {
 			// this is not really necessary if no effects are wanted, but it speeds up turning off the screen
 			XResources.setSystemWideReplacement("android", "bool", "config_animateScreenLights", false);
 			// hook for CRT effects
 			Class<?> classBrightnessState = Class.forName("com.android.server.PowerManagerService$BrightnessState");
                         Method methodRun = classBrightnessState.getDeclaredMethod("run");
                         XposedBridge.hookMethod(methodRun, new XC_MethodHook() {
                             @Override
                             protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                 // About to turn off screen
                                 AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
                                 Object powerManagerService = getSurroundingThis(param.thisObject);
                                 int animationSetting;
                                 animationSetting  = (pref.getBoolean("crt_off_effect", false)) ? ANIM_SETTING_OFF : 0;
                                 animationSetting |= (pref.getBoolean("crt_on_effect", false))  ? ANIM_SETTING_ON  : 0;
                                 setIntField(powerManagerService, "mAnimationSetting", animationSetting);
                             }
                         });
 		} catch (Exception e) { XposedBridge.log(e); }
 		
 		try {
 			if (!pref.getBoolean("unplug_turns_screen_on", true)) {
 				XResources.setSystemWideReplacement("android", "bool", "config_unplugTurnsOnScreen", false);
 			}
 		} catch (Exception e) { XposedBridge.log(e); }
 		
 		XResources.setSystemWideReplacement("android", "bool", "show_ongoing_ime_switcher", false);
 		XResources.setSystemWideReplacement("android", "bool", "config_built_in_sip_phone", true);
 		XResources.setSystemWideReplacement("android", "bool", "config_sip_wifi_only", false);
 		
 		try {
 			XResources.setSystemWideReplacement("android", "integer", "config_longPressOnHomeBehavior", pref.getInt("long_home_press_behaviour", 2));
 		} catch (Exception e) { XposedBridge.log(e); }
 		
 		try {
 			XResources.setSystemWideReplacement("android", "integer", "config_criticalBatteryWarningLevel", pref.getInt("low_battery_critical", 5));
 			XResources.setSystemWideReplacement("android", "integer", "config_lowBatteryWarningLevel", pref.getInt("low_battery_low", 15));
 			XResources.setSystemWideReplacement("android", "integer", "config_lowBatteryCloseWarningLevel", pref.getInt("low_battery_close", 20));
 		} catch (Exception e) { XposedBridge.log(e); }
 		
 		// density / resource configuration manipulation
 		try {
 			Method displayInit = Display.class.getDeclaredMethod("init", int.class);
 			XposedBridge.hookMethod(displayInit, new XC_MethodHook() {
 				@Override
 				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 					AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
 					String packageName = AndroidAppHelper.currentPackageName();
 					
 					int packageDensity = pref.getInt("dpioverride/" + packageName + "/density", pref.getInt("dpioverride/default/density", 0));
 					if (packageDensity > 0)
 						setFloatField(param.thisObject, "mDensity", packageDensity / 160.0f);
 				};
 			});
 			
 			
 			Class<?> classCompatibilityInfo = Class.forName("android.content.res.CompatibilityInfo");
 			Method methodUpdateConfiguration = Resources.class.getDeclaredMethod("updateConfiguration",
 					Configuration.class, DisplayMetrics.class, classCompatibilityInfo);
 			XposedBridge.hookMethod(methodUpdateConfiguration, new XC_MethodHook() {
 				@Override
 				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 					Configuration config = (Configuration) param.args[0];
 					if (config != null && param.thisObject instanceof XResources) {
 						String packageName = ((XResources) param.thisObject).getPackageName();
 						if (packageName != null) {
 							AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
 							
 							int swdp = pref.getInt("dpioverride/" + packageName + "/swdp", pref.getInt("dpioverride/default/swdp", 0));
 							int wdp = pref.getInt("dpioverride/" + packageName + "/wdp", pref.getInt("dpioverride/default/wdp", 0));
 							int hdp = pref.getInt("dpioverride/" + packageName + "/hdp", pref.getInt("dpioverride/default/hdp", 0));
 							Locale packageLocale = getPackageSpecificLocale(packageName);
 							
 							if (swdp > 0 || wdp > 0 || hdp > 0 || packageLocale != null) {
 								Configuration newConfig = new Configuration(config);
 								if (swdp > 0)
 									newConfig.smallestScreenWidthDp = swdp;
 								
 								if (wdp > 0)
 									newConfig.screenWidthDp = wdp;
 								
 								if (hdp > 0)
 									newConfig.screenHeightDp = hdp;
 								
 								if (packageLocale != null) {
 									newConfig.locale = packageLocale;
 									// AndroidAppHelper.currentPackageName() is the package name of the current process,
 									// in contrast to the package name for these settings (that might be loaded by a different
 									// process as well)
 									if (AndroidAppHelper.currentPackageName().equals(packageName))
 										Locale.setDefault(packageLocale);
 								}
 								
 								param.args[0] = newConfig;
 							}
 						}
 					}
 				}
 			});
 		} catch (Exception e) { XposedBridge.log(e); }
 		
 		if (pref.getBoolean("volume_keys_skip_track", false))
 			VolumeKeysSkipTrack.init(pref.getBoolean("volume_keys_skip_track_screenon", false));
 	}
 
 	@Override
 	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
 		AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
 		
 		Locale packageLocale = getPackageSpecificLocale(lpparam.packageName);
 		if (packageLocale != null)
 			Locale.setDefault(packageLocale);
 		
 		if (lpparam.packageName.equals("com.android.systemui")) {
 			if (!pref.getBoolean("battery_full_notification", true)) {
 				try {
 					Class<?> classPowerUI = Class.forName("com.android.systemui.power.PowerUI", false, lpparam.classLoader);
 					Method methodNotifyFullBatteryNotification = classPowerUI.getDeclaredMethod("notifyFullBatteryNotification");
 					XposedBridge.hookMethod(methodNotifyFullBatteryNotification, XC_MethodReplacement.DO_NOTHING);
 				} catch (NoSuchMethodException ignored) {
 				} catch (Exception e) {
 					XposedBridge.log(e);
 				}
 			}
 			
 			if (pref.getBoolean("statusbar_color_enabled", false)) {
 				// http://forum.xda-developers.com/showthread.php?t=1523703
 				try {
 					Constructor<?> constructLayoutParams = WindowManager.LayoutParams.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class);
 					XposedBridge.hookMethod(constructLayoutParams, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
 						@Override
 						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 							if ((Integer)param.args[4] == PixelFormat.RGB_565)
 								param.args[4] = PixelFormat.TRANSLUCENT;
 						}
 					});
 				} catch (Exception e) {	XposedBridge.log(e); }
 			}
 			
 			if (pref.getInt("num_signal_bars", 4) > 4) {
 				try {
 					// correction for signal strength level
 					Method methodGetLevel = SignalStrength.class.getDeclaredMethod("getLevel");
 					XposedBridge.hookMethod(methodGetLevel, new XC_MethodHook() {
 						@Override
 						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 							param.setResult(getCorrectedLevel((Integer) param.getResult()));
 						}
 						private int getCorrectedLevel(int level) {
 							// value was overridden by our more specific method already
 							if (level >= 10000)
 								return level - 10000;
 							
 							// interpolate for other modes
 							if (signalStrengthBars == 4 || level == 0) {
 								return level;
 								
 							} else if (signalStrengthBars == 5) {
 								if (level == 4) return 5;
 								else if (level == 3) return 4;
 								else if (level == 2) return 3;
 								else if (level == 1) return 2;
 								
 							} else if (signalStrengthBars == 6) {
 								if (level == 4) return 6;
 								else if (level == 3) return 4;
 								else if (level == 2) return 3;
 								else if (level == 1) return 2;
 							}
 							// shouldn't get here
 							XposedBridge.log("could not determine signal level (original result was " + level + ")");
 							return 0;
 						}
 					});
 					
 					Method methodGsmGetLevel = SignalStrength.class.getDeclaredMethod("getGsmLevel");
 					XposedBridge.hookMethod(methodGsmGetLevel, new XC_MethodHook() {
 						@Override
 						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 							int asu = ((SignalStrength) param.thisObject).getGsmSignalStrength();
 							param.setResult(getSignalLevel(asu));
 						}
 						private int getSignalLevel(int asu) {
 							switch (signalStrengthBars) {
 								case 6:
 							        if (asu <= 1 || asu == 99) return 10000;
 							        else if (asu >= 12) return 10006;
 							        else if (asu >= 10) return 10005;
 							        else if (asu >= 8)  return 10004;
 							        else if (asu >= 6)  return 10003;
 							        else if (asu >= 4)  return 10002;
 							        else return 10001;
 							        
 								case 5:
 							        if (asu <= 1 || asu == 99) return 10000;
 							        else if (asu >= 12) return 10005;
 							        else if (asu >= 10) return 10004;
 							        else if (asu >= 7)  return 10003;
 							        else if (asu >= 4)  return 10002;
 							        else return 10001;
 							
 								default:
 									// original implementation (well, kind of. should not be needed anyway)
 							        if (asu <= 2 || asu == 99) return 10000;
 							        else if (asu >= 12) return 10004;
 							        else if (asu >= 8)  return 10003;
 							        else if (asu >= 5)  return 10002;
 							        else return 10001;
 							}
 						};
 					});
 				} catch (Exception e) { XposedBridge.log(e); }
 			}
 		} else if (lpparam.packageName.equals("com.android.vending")) {
 			try {
 				Class<?> classDeviceConfigurationProto
 					= Class.forName("com.google.android.vending.remoting.protos.DeviceConfigurationProto", false, lpparam.classLoader);
 				XposedBridge.hookMethod(classDeviceConfigurationProto.getDeclaredMethod("getScreenDensity"), new XC_MethodReplacement() {
 					@Override
 					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
 						return 240;
 					}
 				});
 			} catch (Exception e) { XposedBridge.log(e); }
 		} else if (lpparam.packageName.equals("android")) {
 			try {
 				Class<?> classMOL = Class.forName("com.android.internal.policy.impl.PhoneWindowManager$MyOrientationListener", false, lpparam.classLoader);
 				XposedBridge.hookMethod(classMOL.getDeclaredMethod("onProposedRotationChanged", int.class), new XC_MethodHook() {
 					@Override
 					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 						int rotation = (Integer) param.args[0];
 						if (rotation >= 0) {
						    AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
						    if (pref.getBoolean("crt_effect_orientation", false)) {
						        SystemProperties.set("runtime.xposed.orientation", String.valueOf(rotation));
						    } else {
						        // Must always update, otherwise it might become stuck in landscape if turned Off without reboot
						        SystemProperties.set("runtime.xposed.orientation", "0");
						    }
 						}
 					}
 				});
 			} catch (Throwable t) { XposedBridge.log(t); } 
 		} else if (lpparam.packageName.equals("com.android.phone")) {
 	            try {
 	                Method displaySSInfo = Class.forName("com.android.phone.PhoneUtils", false, lpparam.classLoader).getDeclaredMethod("displaySSInfo",
 	                                                        Class.forName("com.android.internal.telephony.Phone",
 	                                                                      false,
 	                                                                      lpparam.classLoader),
 	                                                        Class.forName("android.content.Context",
 	                                                                      false,
 	                                                                      lpparam.classLoader),
 	                                                        Class.forName("com.android.internal.telephony.gsm.SuppServiceNotification",
 	                                                                      false,
 	                                                                      lpparam.classLoader),
 	                                                        Class.forName("android.os.Message",
 	                                                                      false,
 	                                                                      lpparam.classLoader),
 	                                                        Class.forName("android.app.AlertDialog",
 	                                                                      false,
 	                                                                      lpparam.classLoader));
 	                
 	                Class<?> classSSNotification = Class.forName("com.android.internal.telephony.gsm.SuppServiceNotification",
 	                                                             false,
 	                                                             lpparam.classLoader);
 	                final Field fieldNotificationType = classSSNotification.getDeclaredField("notificationType");
 	                final Field fieldCode = classSSNotification.getDeclaredField("code");
 	                AccessibleObject.setAccessible(new AccessibleObject[] { fieldNotificationType, fieldCode }, true);
 	                
 	                XposedBridge.hookMethod(displaySSInfo, new XC_MethodHook() {
 	                    @Override
 	                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 	                        if (fieldNotificationType.getInt(param.args[2]) == 0) {
 	                            if (fieldCode.getInt(param.args[2]) == 3) { // Waiting for target party
 	                                AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
 	                                if (pref.getBoolean("phone_vibrate_waiting", false)) {
 	                                    Context context = (Context) param.args[1];
 
 	                                    // Get instance of Vibrator from current Context
 	                                    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
 	                                    v.vibrate(new long[] { 0, 200, 200, 200, 200, 800 }, -1); // Vibrate with a simple pattern
 	                                }
 	                            }
 	                        }
 	                    }
 	                });
 	                
 	                
 	                // Handle increasing ringer tone
 	                final ThreadLocal<Object> insideRingerHandler = new ThreadLocal<Object>();
 	                
 	                // Control whether the execution is inside handleMessage or not()
 	                Class<?> classRinger1 = Class.forName("com.android.phone.Ringer$1", false, lpparam.classLoader);
 	                XposedBridge.hookMethod(classRinger1.getMethod("handleMessage", Class.forName("android.os.Message", false, lpparam.classLoader)),
 	                                        new XC_MethodHook() {
                             @Override
                             protected void beforeHookedMethod(MethodHookParam param)
                                     throws Throwable {
                                 super.beforeHookedMethod(param);
                                 
                                 insideRingerHandler.set(new Object());
                             }
     
                             @Override
                             protected void afterHookedMethod(MethodHookParam param)
                                     throws Throwable {
                                 super.afterHookedMethod(param);
                                 
                                 insideRingerHandler.set(null);
                             }
 	                });
 	                
 	                XposedBridge.hookMethod(Class.forName("android.media.AudioManager", false, lpparam.classLoader)
 	                                            .getMethod("setStreamVolume", int.class, int.class, int.class),
 	                                        new XC_MethodHook() {
                             @Override
                             protected void beforeHookedMethod(MethodHookParam param)
                                     throws Throwable {
                                 super.beforeHookedMethod(param);
                                 if (insideRingerHandler.get() != null) {
                                     // Within execution of handleMessage()
                                     AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
                                     if (!pref.getBoolean("phone_increasing_ringer", true)) {
                                         // No increasing ringer; skip changing the ringer volume
                                         param.setResult(null);
                                     }
                                 }
                             }
                         });
 	                
 	                
 	                // Handle call recording
 	                if (pref.getBoolean("phone_call_recording", false)) {
 	                	try {
 	                	    Method hasFeature = Class.forName("com.android.phone.PhoneFeature", false,
 	                	    		lpparam.classLoader).getDeclaredMethod("hasFeature", String.class);
 	                	    XposedBridge.hookMethod(hasFeature, new XC_MethodHook() {
 	                	    	@Override
 	                	    	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
 	                	    		if ("voice_call_recording".equals(param.args[0])) {
 	                	    			param.setResult(Boolean.TRUE);
 	                	    		}
 	                	    	}
 	                	    });
 
 	                	    final Class<?> classMediaRecorder = Class.forName("android.media.MediaRecorder", false, lpparam.classLoader);
 	                	    Method prepare = classMediaRecorder.getDeclaredMethod("prepare");
 	                	    XposedBridge.hookMethod(prepare, new XC_MethodHook() {
 	                	    	@Override
 	                	    	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 	                	    		XposedHelpers.callMethod(param.thisObject, "start");
 	                	    	}
 							});
 
 	                	} catch (Exception e) {
 	                	    XposedBridge.log(e);
 	                	}
 	                }
 	                
 	                
 	            } catch (Exception e) { XposedBridge.log(e); }
 		}
 	}
 	
 	@Override
 	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
 		AndroidAppHelper.reloadSharedPreferencesIfNeeded(pref);
 		if (resparam.packageName.equals("com.android.systemui")) {
 			try {
 				signalStrengthBars = pref.getInt("num_signal_bars", 4);
 				resparam.res.setReplacement("com.android.systemui", "integer", "config_maxLevelOfSignalStrengthIndicator",
 						signalStrengthBars);
 			} catch (Exception e) { XposedBridge.log(e); }
 			
 			if (pref.getBoolean("statusbar_color_enabled", false)) {
 				try {
 					int statusbarColor = pref.getInt("statusbar_color", Color.BLACK);
 					resparam.res.setReplacement("com.android.systemui", "drawable", "status_bar_background", new ColorDrawable(statusbarColor));
 				} catch (Exception e) { XposedBridge.log(e); }
 			}
 			
 			if (pref.getBoolean("statusbar_clock_color_enabled", false)) {
 				try {
 					resparam.res.hookLayout("com.android.systemui", "layout", "tw_status_bar", new XC_LayoutInflated() {
 						@Override
 						public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
 							try {
 								TextView clock = (TextView) liparam.view.findViewById(
 										liparam.res.getIdentifier("clock", "id", "com.android.systemui"));
 								clock.setTextColor(pref.getInt("statusbar_clock_color", 0xffbebebe));
 							} catch (Exception e) { XposedBridge.log(e); }
 						}
 					}); 
 				} catch (Exception e) { XposedBridge.log(e); }
 			}
 		}
 	}
 	
 	private static Locale getPackageSpecificLocale(String packageName) {
 		String locale = pref.getString("dpioverride/" + packageName + "/locale", pref.getString("dpioverride/default/locale", null));
 		if (locale == null || locale.isEmpty())
 			return null;
 		
 		String[] localeParts = locale.split("_", 3);
 		String language = localeParts[0];
 		String region = (localeParts.length >= 2) ? localeParts[1] : "";
 		String variant = (localeParts.length >= 3) ? localeParts[2] : "";
 		return new Locale(language, region, variant);
 	}
 }
