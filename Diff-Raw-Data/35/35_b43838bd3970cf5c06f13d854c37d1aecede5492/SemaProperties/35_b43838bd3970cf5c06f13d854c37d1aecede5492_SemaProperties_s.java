 /*  Semaphore Manager
  *  
  *   Copyright (c) 2012 Stratos Karafotis (stratosk@semaphore.gr)
  *   
  * 
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 2 as
  * published by the Free Software Foundation.
  */
 package com.semaphore.smproperties;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.preference.PreferenceManager;
 import com.semaphore.sm.Commander;
 import java.util.ArrayList;
 import java.util.List;
 
 public class SemaProperties {
 
     public SMIntProperty oc;
     public SMStringProperty gov;
     public SMOndemandProperty ondemand;
     public SMConservativeProperty conservative;
     public SMSmartassProperty smartass;
     public SMInteractiveProperty interactive;
     public SMIntProperty deep_idle;
     public SMIntProperty deep_idle_stats;
     public SMIntProperty bluetooth;
     public SMIntProperty lock_min;
     public SMLoggerProperty logger;
     public SMCifsProperty cifs;
     public SMModuleProperty tun;
     public SMModuleProperty configs;
     public SMModuleProperty radio_si4709_i2c;
     public SMModuleProperty mousedev;
     public SMXboxProperty xbox;
     public SMSchedulerProperty scheduler;
     public SMAbProperty autobr;
     public SMIntProperty vibrator;
     public SMIntProperty touch_enable;
     public SMIntProperty touch;
     public SMIntProperty bigmem;
     public SMIntProperty wififast;
     public SMModuleProperty fastchg;
     //public SMTouchscreenProperty touchscreen;
     public SMReadaheadProperty read_ahead;
 
     public SemaProperties() {
 
         oc = new SMIntProperty("oc", "/sys/devices/virtual/misc/liveoc/oc_value", false, 100, 120, 100);
         gov = new SMStringProperty("gov", "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", false, "ondemand");
         ondemand = new SMOndemandProperty();
         conservative = new SMConservativeProperty();
         smartass = new SMSmartassProperty();
         interactive = new SMInteractiveProperty();
         deep_idle = new SMIntProperty("deep_idle", "/sys/devices/virtual/misc/deepidle/enabled", false, 0, 1, 0);
         deep_idle_stats = new SMIntProperty("deep_idle_stats", "/sys/devices/virtual/misc/deepidle/stats_enabled", false, 0, 1, 0);
         lock_min = new SMIntProperty("lock_min", "/sys/devices/system/cpu/cpu0/cpufreq/lock_scaling_min", false, 0, 1, 0);
         bluetooth = new SMIntProperty("bluetooth", "/sys/devices/platform/bt_rfkill/rfkill/rfkill0/soft", false, 0, 1, 0);
 
         logger = new SMLoggerProperty("logger", false);
         tun = new SMModuleProperty("tun", "tun", false, false);
         cifs = new SMCifsProperty(false);
         configs = new SMModuleProperty("configs", "configs", false, false);
         radio_si4709_i2c = new SMModuleProperty("radio_si4709_i2c", "radio-si4709-i2c", false, false);
         mousedev = new SMModuleProperty("mousedev", "mousedev", false, false);
         xbox = new SMXboxProperty(false);
 
         scheduler = new SMSchedulerProperty("scheduler", "noop");
         autobr = new SMAbProperty();
         vibrator = new SMIntProperty("vibrator", "/sys/devices/virtual/timed_output/vibrator/duty", false, 23500, 43640, 43640);
         touch_enable = new SMIntProperty("touch_enable", "/sys/devices/virtual/misc/touchwake/enabled", false, 0, 1, 0);
         touch = new SMIntProperty("touch", "/sys/devices/virtual/misc/touchwake/delay", false, 0, 90000, 0);
         bigmem = new SMIntProperty("bigmem", "sys/kernel/bigmem/enable", false, 0, 1, 0);
         wififast = new SMIntProperty("wififast", "/sys/module/bcmdhd/parameters/wifi_pm", false, 0, 1, 0);
         fastchg = new SMModuleProperty("fastchg", "fastchg", false, false);
         //touchscreen = new SMTouchscreenProperty("touchscreen", "stock");
         read_ahead = new SMReadaheadProperty("read_ahead", "128KB");
     }
 
     public void readValues() {
         oc.readValue();
         gov.readValue();
         ondemand.readValue();
         conservative.readValue();
         smartass.readValue();
         interactive.readValue();
         deep_idle.readValue();
         deep_idle_stats.readValue();
         lock_min.readValue();
         bluetooth.readValue();
 
         logger.readValue();
         tun.readValue();
         cifs.readValue();
         configs.readValue();
         radio_si4709_i2c.readValue();
         mousedev.readValue();
         xbox.readValue();
 
         scheduler.readValue();
         autobr.readValue();
         vibrator.readValue();
         touch_enable.readValue();
         touch.readValue();
         bigmem.readValue();
         wififast.readValue();
         fastchg.readValue();
         //touchscreen.readValue();
         read_ahead.readValue();
     }
 
     public void writeBatch() {
         List<String> cmds = new ArrayList<String>();
 
         oc.writeBatch(cmds);
         if (gov.getValue().equals(conservative.getName())) {
             conservative.cons.writeBatch(cmds);
         } else if (gov.getValue().equals(smartass.getName())) {
             smartass.smart.writeBatch(cmds);
         } else if (gov.getValue().equals(interactive.getName())) {
             interactive.inter.writeBatch(cmds);
         }
         gov.writeBatch(cmds);
         ondemand.writeBatch(cmds);
         conservative.writebatch(cmds);
         smartass.writebatch(cmds);
         interactive.writebatch(cmds);
         deep_idle.writeBatch(cmds);
         deep_idle.writeBatch(cmds);
         deep_idle_stats.writeBatch(cmds);
         lock_min.writeBatch(cmds);
         if (bluetooth.getBoolean())
             bluetooth.writeBatch(cmds);
 
         logger.writeBatch(cmds);
         tun.writeBatch(cmds);
         cifs.writeBatch(cmds);
         configs.writeBatch(cmds);
         radio_si4709_i2c.writeBatch(cmds);
         mousedev.writeBatch(cmds);
         xbox.writeBatch(cmds);
 
         scheduler.writeBatch(cmds);
         autobr.writeBatch(cmds);
         vibrator.writeBatch(cmds);
         touch_enable.writeBatch(cmds);
         touch.writeBatch(cmds);
         bigmem.writeBatch(cmds);
         wififast.writeBatch(cmds);
         fastchg.writeBatch(cmds);
         //touchscreen.writeValue();
         read_ahead.writeBatch(cmds);
 
         Commander.getInstance().runSuBatch(cmds);
     }
 
     public void writeValues() {
         oc.writeValue();
         if (gov.getValue().equals(conservative.getName())) {
             conservative.cons.writeValue();
         } else if (gov.getValue().equals(smartass.getName())) {
             smartass.smart.writeValue();
         } else if (gov.getValue().equals(interactive.getName())) {
             interactive.inter.writeValue();
         }
         gov.writeValue();
         ondemand.writeValue();
         conservative.writeValue();
         smartass.writeValue();
         interactive.writeValue();
         deep_idle.writeValue();
         deep_idle_stats.writeValue();
         lock_min.writeValue();
         if (bluetooth.getBoolean())
             bluetooth.writeValue();
 
         logger.writeValue();
         tun.writeValue();
         cifs.writeValue();
         configs.writeValue();
         radio_si4709_i2c.writeValue();
         mousedev.writeValue();
         xbox.writeValue();
 
         scheduler.writeValue();
         autobr.writeValue();
         vibrator.writeValue();
         touch_enable.writeValue();
         touch.writeValue();
         bigmem.writeValue();
         wififast.writeValue();
         fastchg.writeValue();
         //touchscreen.writeValue();
         read_ahead.writeValue();
     }
 
     public void setPreferences(Context context) {
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
         Editor edit = prefs.edit();
 
         edit.putInt(oc.getName(), oc.getValue());
         edit.putString(gov.getName(), gov.getValue());
         // ondemand tunables
         edit.putBoolean(ondemand.io_is_busy.getName(), ondemand.io_is_busy.getBoolean());
         edit.putString(ondemand.sampling_down_factor.getName(), ondemand.sampling_down_factor.getValString());
         edit.putString(ondemand.sampling_down_max_momentum.getName(), ondemand.sampling_down_max_momentum.getValString());
         edit.putString(ondemand.sampling_rate.getName(), ondemand.sampling_rate.getValString());
         edit.putString(ondemand.up_threshold.getName(), ondemand.up_threshold.getValString());
         edit.putBoolean(ondemand.smooth_ui.getName(), ondemand.smooth_ui.getBoolean());
         // conservative
         edit.putBoolean(conservative.cons.getName(), conservative.cons.getValue());
         edit.putString(conservative.freq_step.getName(), conservative.freq_step.getValString());
         edit.putString(conservative.sampling_down_factor.getName(), conservative.sampling_down_factor.getValString());
         edit.putString(conservative.sampling_rate.getName(), conservative.sampling_rate.getValString());
         edit.putString(conservative.up_threshold.getName(), conservative.up_threshold.getValString());
         edit.putString(conservative.down_threshold.getName(), conservative.down_threshold.getValString());
         edit.putBoolean(conservative.smooth_ui.getName(), conservative.smooth_ui.getBoolean());
         // smartass
         edit.putBoolean(smartass.smart.getName(), smartass.smart.getValue());
         edit.putString(smartass.awake_ideal_freq.getName(), smartass.awake_ideal_freq.getValString());
         edit.putString(smartass.up_rate.getName(), smartass.up_rate.getValString());
         edit.putString(smartass.down_rate.getName(), smartass.down_rate.getValString());
         edit.putString(smartass.max_cpu_load.getName(), smartass.max_cpu_load.getValString());
         edit.putString(smartass.min_cpu_load.getName(), smartass.min_cpu_load.getValString());
         edit.putString(smartass.ramp_up_step.getName(), smartass.ramp_up_step.getValString());
         edit.putString(smartass.ramp_down_step.getName(), smartass.ramp_down_step.getValString());
         edit.putString(smartass.sleep_wakeup_freq.getName(), smartass.sleep_wakeup_freq.getValString());
         edit.putString(smartass.sleep_ideal_freq.getName(), smartass.sleep_ideal_freq.getValString());
         edit.putString(smartass.sample_rate_jiffies.getName(), smartass.sample_rate_jiffies.getValString());
         edit.putBoolean(smartass.smooth_ui.getName(), conservative.smooth_ui.getBoolean());
         // interactive
         edit.putBoolean(interactive.inter.getName(), interactive.inter.getValue());
         edit.putString(interactive.hispeed_freq.getName(), interactive.hispeed_freq.getValString());
         edit.putString(interactive.go_hispeed_load.getName(), interactive.go_hispeed_load.getValString());
         edit.putString(interactive.min_sampling_time.getName(), interactive.min_sampling_time.getValString());
         edit.putString(interactive.above_hispeed_delay.getName(), interactive.above_hispeed_delay.getValString());
         edit.putString(interactive.timer_rate.getName(), interactive.timer_rate.getValString());
         edit.putBoolean(interactive.input_boost.getName(), interactive.input_boost.getBoolean());
         
         edit.putBoolean(deep_idle.getName(), deep_idle.getBoolean());
         edit.putBoolean(deep_idle_stats.getName(), deep_idle_stats.getBoolean());
         edit.putBoolean(lock_min.getName(), lock_min.getBoolean());
         edit.putBoolean(bluetooth.getName(), bluetooth.getBoolean());
 
         edit.putBoolean(logger.getName(), logger.getValue());
         edit.putBoolean(tun.getName(), tun.getValue());
         edit.putBoolean(cifs.getName(), cifs.getValue());
         edit.putBoolean(configs.getName(), configs.getValue());
         edit.putBoolean(radio_si4709_i2c.getName(), radio_si4709_i2c.getValue());
         edit.putBoolean(mousedev.getName(), mousedev.getValue());
         edit.putBoolean(xbox.getName(), xbox.getValue());
 
         edit.putString(scheduler.getName(), scheduler.getValue());
         edit.putBoolean(autobr.sema_autobr.getName(), autobr.sema_autobr.getValue());
         edit.putString(autobr.min_brightness.getName(), String.valueOf(autobr.min_brightness.getValue()));
         edit.putString(autobr.max_brightness.getName(), String.valueOf(autobr.max_brightness.getValue()));
         edit.putString(autobr.max_lux.getName(), String.valueOf(autobr.max_lux.getValue()));
         edit.putString(autobr.instant_update_thres.getName(), String.valueOf(autobr.instant_update_thres.getValue()));
         edit.putString(autobr.effect_delay_ms.getName(), String.valueOf(autobr.effect_delay_ms.getValue()));
 
         edit.putInt(vibrator.getName(), vibrator.getValue());
         edit.putBoolean(touch_enable.getName(), touch_enable.getBoolean());
         edit.putInt(touch.getName(), touch.getValue());
         edit.putBoolean(bigmem.getName(), bigmem.getBoolean());
         edit.putBoolean(wififast.getName(), wififast.getBoolean());
         edit.putBoolean(fastchg.getName(), fastchg.getValue());
         //edit.putString(touchscreen.getName(), touchscreen.getValue());
         edit.putString(read_ahead.getName(), read_ahead.getValue());
         edit.commit();
     }
 
     public void getPreferences(Context context) {
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
 
         oc.setValue(prefs.getInt(oc.getName(), oc.getDefault()));
         gov.setValue(prefs.getString(gov.getName(), gov.getDefValue()));
         ondemand.io_is_busy.setValue(prefs.getBoolean(ondemand.io_is_busy.getName(), ondemand.io_is_busy.getDefault() == 1 ? true : false) == true ? 1 : 0);
         ondemand.sampling_down_factor.setValue(prefs.getString(ondemand.sampling_down_factor.getName(), ondemand.sampling_down_factor.getDefString()));
         ondemand.sampling_down_max_momentum.setValue(prefs.getString(ondemand.sampling_down_max_momentum.getName(), ondemand.sampling_down_max_momentum.getDefString()));
         ondemand.sampling_rate.setValue(prefs.getString(ondemand.sampling_rate.getName(), ondemand.sampling_rate.getDefString()));
         ondemand.up_threshold.setValue(prefs.getString(ondemand.up_threshold.getName(), ondemand.up_threshold.getDefString()));
         ondemand.smooth_ui.setValue(prefs.getBoolean(ondemand.smooth_ui.getName(), ondemand.smooth_ui.getDefBoolean()) == true ? 1 : 0);
 
         conservative.cons.setValue(prefs.getBoolean(conservative.cons.getName(), conservative.cons.getDefValue()));
         conservative.freq_step.setValue(prefs.getString(conservative.freq_step.getName(), conservative.freq_step.getDefString()));
         conservative.sampling_down_factor.setValue(prefs.getString(conservative.sampling_down_factor.getName(), conservative.sampling_down_factor.getDefString()));
         conservative.sampling_rate.setValue(prefs.getString(conservative.sampling_rate.getName(), conservative.sampling_rate.getDefString()));
         conservative.up_threshold.setValue(prefs.getString(conservative.up_threshold.getName(), conservative.up_threshold.getDefString()));
         conservative.down_threshold.setValue(prefs.getString(conservative.down_threshold.getName(), conservative.down_threshold.getDefString()));
         conservative.smooth_ui.setValue(prefs.getBoolean(conservative.smooth_ui.getName(), conservative.smooth_ui.getDefBoolean()) == true ? 1 : 0);
 
         smartass.smart.setValue(prefs.getBoolean(smartass.smart.getName(), smartass.smart.getDefValue()));
         smartass.awake_ideal_freq.setValue(prefs.getString(smartass.awake_ideal_freq.getName(), smartass.awake_ideal_freq.getDefString()));
         smartass.up_rate.setValue(prefs.getString(smartass.up_rate.getName(), smartass.up_rate.getDefString()));
         smartass.down_rate.setValue(prefs.getString(smartass.down_rate.getName(), smartass.down_rate.getDefString()));
         smartass.max_cpu_load.setValue(prefs.getString(smartass.max_cpu_load.getName(), smartass.max_cpu_load.getDefString()));
         smartass.min_cpu_load.setValue(prefs.getString(smartass.min_cpu_load.getName(), smartass.min_cpu_load.getDefString()));
         smartass.ramp_up_step.setValue(prefs.getString(smartass.ramp_up_step.getName(), smartass.ramp_up_step.getDefString()));
         smartass.ramp_down_step.setValue(prefs.getString(smartass.ramp_down_step.getName(), smartass.ramp_down_step.getDefString()));
         smartass.sleep_wakeup_freq.setValue(prefs.getString(smartass.sleep_wakeup_freq.getName(), smartass.sleep_wakeup_freq.getDefString()));
         smartass.sleep_ideal_freq.setValue(prefs.getString(smartass.sleep_ideal_freq.getName(), smartass.sleep_ideal_freq.getDefString()));
         smartass.sample_rate_jiffies.setValue(prefs.getString(smartass.sample_rate_jiffies.getName(), smartass.sample_rate_jiffies.getDefString()));
         smartass.smooth_ui.setValue(prefs.getBoolean(smartass.smooth_ui.getName(), smartass.smooth_ui.getDefBoolean()) == true ? 1 : 0);
 
         interactive.inter.setValue(prefs.getBoolean(interactive.inter.getName(), interactive.inter.getDefValue()));
         interactive.hispeed_freq.setValue(prefs.getString(interactive.hispeed_freq.getName(), interactive.hispeed_freq.getDefString()));
         interactive.go_hispeed_load.setValue(prefs.getString(interactive.go_hispeed_load.getName(), interactive.go_hispeed_load.getDefString()));
         interactive.min_sampling_time.setValue(prefs.getString(interactive.min_sampling_time.getName(), interactive.min_sampling_time.getDefString()));
         interactive.above_hispeed_delay.setValue(prefs.getString(interactive.above_hispeed_delay.getName(), interactive.above_hispeed_delay.getDefString()));
         interactive.timer_rate.setValue(prefs.getString(interactive.timer_rate.getName(), interactive.timer_rate.getDefString()));
         interactive.input_boost.setValue(prefs.getBoolean(interactive.input_boost.getName(), interactive.input_boost.getDefBoolean()) == true ? 1 : 0);
 
         deep_idle.setValue((prefs.getBoolean(deep_idle.getName(), deep_idle.getDefBoolean()) == true ? 1 : 0));
         deep_idle_stats.setValue((prefs.getBoolean(deep_idle_stats.getName(), deep_idle_stats.getDefBoolean()) == true ? 1 : 0));
         lock_min.setValue((prefs.getBoolean(lock_min.getName(), lock_min.getDefBoolean()) == true ? 1 : 0));
         bluetooth.setValue((prefs.getBoolean(bluetooth.getName(), bluetooth.getDefBoolean()) == true ? 1 : 0));
 
         logger.setValue(prefs.getBoolean(logger.getName(), logger.getDefValue()));
         tun.setValue(prefs.getBoolean(tun.getName(), tun.getDefValue()));
         cifs.setValue(prefs.getBoolean(cifs.getName(), cifs.getDefValue()));
         configs.setValue(prefs.getBoolean(configs.getName(), configs.getDefValue()));
         radio_si4709_i2c.setValue(prefs.getBoolean(radio_si4709_i2c.getName(), radio_si4709_i2c.getDefValue()));
         mousedev.setValue(prefs.getBoolean(mousedev.getName(), mousedev.getDefValue()));
         xbox.setValue(prefs.getBoolean(xbox.getName(), xbox.getValue()));
 
         scheduler.setValue(prefs.getString(scheduler.getName(), scheduler.getDefValue()));
         autobr.sema_autobr.setValue(prefs.getBoolean(autobr.sema_autobr.getName(), autobr.sema_autobr.getDefValue()));
         autobr.min_brightness.setValue(prefs.getString(autobr.min_brightness.getName(), autobr.min_brightness.getDefString()));
         autobr.max_brightness.setValue(prefs.getString(autobr.max_brightness.getName(), autobr.max_brightness.getDefString()));
         autobr.max_lux.setValue(prefs.getString(autobr.max_lux.getName(), autobr.max_lux.getDefString()));
         autobr.instant_update_thres.setValue(prefs.getString(autobr.instant_update_thres.getName(), autobr.instant_update_thres.getDefString()));
         autobr.effect_delay_ms.setValue(prefs.getString(autobr.effect_delay_ms.getName(), autobr.effect_delay_ms.getDefString()));
 
         vibrator.setValue(prefs.getInt(vibrator.getName(), vibrator.getDefault()));
         touch_enable.setValue(prefs.getBoolean(touch_enable.getName(), touch_enable.getDefBoolean()) == true ? 1 : 0);
         touch.setValue(prefs.getInt(touch.getName(), touch.getDefault()));
         bigmem.setValue(prefs.getBoolean(bigmem.getName(), bigmem.getDefBoolean()) == true ? 1 : 0);
         wififast.setValue(prefs.getBoolean(wififast.getName(), wififast.getDefBoolean()) == true ? 1 : 0);
         fastchg.setValue(prefs.getBoolean(fastchg.getName(), fastchg.getDefValue()));
         //touchscreen.setValue(prefs.getString(touchscreen.getName(), touchscreen.getDefValue()));
         read_ahead.setValue(prefs.getString(read_ahead.getName(), read_ahead.getDefValue()));
     }
 
     public void resetDefaults() {
         oc.setValue(oc.getDefault());
         gov.setValue(gov.getDefValue());
         ondemand.io_is_busy.setValue(ondemand.io_is_busy.getDefault());
         ondemand.sampling_down_factor.setValue(ondemand.sampling_down_factor.getDefString());
         ondemand.sampling_down_max_momentum.setValue(ondemand.sampling_down_max_momentum.getDefString());
         ondemand.sampling_rate.setValue(ondemand.sampling_rate.getDefString());
         ondemand.up_threshold.setValue(ondemand.up_threshold.getDefString());
         ondemand.smooth_ui.setValue(ondemand.smooth_ui.getDefault());
 
         conservative.cons.setValue(conservative.cons.getDefValue());
         conservative.freq_step.setValue(conservative.freq_step.getDefString());
         conservative.sampling_down_factor.setValue(conservative.sampling_down_factor.getDefString());
         conservative.sampling_rate.setValue(conservative.sampling_rate.getDefString());
         conservative.up_threshold.setValue(conservative.up_threshold.getDefString());
         conservative.down_threshold.setValue(conservative.down_threshold.getDefString());
         conservative.smooth_ui.setValue(conservative.smooth_ui.getDefault());
 
         smartass.smart.setValue(smartass.smart.getDefValue());
         smartass.awake_ideal_freq.setValue(smartass.awake_ideal_freq.getDefString());
         smartass.up_rate.setValue(smartass.up_rate.getDefString());
         smartass.down_rate.setValue(smartass.down_rate.getDefString());
         smartass.max_cpu_load.setValue(smartass.max_cpu_load.getDefString());
         smartass.min_cpu_load.setValue(smartass.min_cpu_load.getDefString());
         smartass.ramp_up_step.setValue(smartass.ramp_up_step.getDefString());
         smartass.ramp_down_step.setValue(smartass.ramp_down_step.getDefString());
         smartass.sleep_wakeup_freq.setValue(smartass.sleep_wakeup_freq.getDefString());
         smartass.sleep_ideal_freq.setValue(smartass.sleep_ideal_freq.getDefString());
         smartass.sample_rate_jiffies.setValue(smartass.sample_rate_jiffies.getDefString());
         smartass.smooth_ui.setValue(smartass.smooth_ui.getDefault());
 
         interactive.inter.setValue(interactive.inter.getDefValue());
         interactive.hispeed_freq.setValue(interactive.hispeed_freq.getDefString());
         interactive.go_hispeed_load.setValue(interactive.go_hispeed_load.getDefString());
         interactive.min_sampling_time.setValue(interactive.min_sampling_time.getDefString());
         interactive.above_hispeed_delay.setValue(interactive.above_hispeed_delay.getDefString());
         interactive.timer_rate.setValue(interactive.timer_rate.getDefString());
         interactive.input_boost.setValue(interactive.input_boost.getDefault());
         
         deep_idle.setValue(deep_idle.getDefault());
         deep_idle_stats.setValue(deep_idle_stats.getDefault());
         lock_min.setValue(lock_min.getDefault());
         bluetooth.setValue(bluetooth.getDefault());
 
         logger.setValue(logger.getDefValue());
         tun.setValue(tun.getDefValue());
         cifs.setValue(cifs.getDefValue());
         configs.setValue(configs.getDefValue());
         radio_si4709_i2c.setValue(radio_si4709_i2c.getDefValue());
         mousedev.setValue(mousedev.getDefValue());
         xbox.setValue(xbox.getValue());
 
         scheduler.setValue(scheduler.getDefValue());
         autobr.sema_autobr.setValue(autobr.sema_autobr.getDefValue());
         autobr.min_brightness.setValue(autobr.min_brightness.getDefString());
         autobr.max_brightness.setValue(autobr.max_brightness.getDefString());
         autobr.max_lux.setValue(autobr.max_lux.getDefString());
         autobr.instant_update_thres.setValue(autobr.instant_update_thres.getDefString());
         autobr.effect_delay_ms.setValue(autobr.effect_delay_ms.getDefString());
 
         vibrator.setValue(vibrator.getDefault());
         touch_enable.setValue(touch_enable.getDefault());
         touch.setValue(touch.getDefault());
         bigmem.setValue(bigmem.getDefault());
         wififast.setValue(wififast.getDefault());
         fastchg.setValue(fastchg.getValue());
         //touchscreen.setValue(touchscreen.getDefValue());
         read_ahead.setValue(read_ahead.getDefValue());
     }
 }
