 package ch.amana.android.cputuner.view.activity;
 
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.database.Cursor;
 import android.os.Bundle;
 import android.support.v4.app.FragmentActivity;
 import android.support.v4.app.FragmentManager;
 import android.support.v4.app.FragmentTransaction;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemSelectedListener;
 import android.widget.SeekBar;
 import android.widget.SeekBar.OnSeekBarChangeListener;
 import android.widget.SimpleCursorAdapter;
 import android.widget.Spinner;
 import android.widget.TableRow;
 import android.widget.TextView;
 import android.widget.Toast;
 import ch.amana.android.cputuner.R;
 import ch.amana.android.cputuner.helper.GovernorConfigHelper;
 import ch.amana.android.cputuner.helper.GovernorConfigHelper.GovernorConfig;
 import ch.amana.android.cputuner.helper.GuiUtils;
 import ch.amana.android.cputuner.helper.Logger;
 import ch.amana.android.cputuner.helper.Notifier;
 import ch.amana.android.cputuner.helper.PulseHelper;
 import ch.amana.android.cputuner.helper.SettingsStorage;
 import ch.amana.android.cputuner.hw.BatteryHandler;
 import ch.amana.android.cputuner.hw.CpuHandler;
 import ch.amana.android.cputuner.hw.HardwareHandler;
 import ch.amana.android.cputuner.model.IGovernorModel;
 import ch.amana.android.cputuner.model.PowerProfiles;
 import ch.amana.android.cputuner.model.ProfileModel;
 import ch.amana.android.cputuner.model.VirtualGovernorModel;
 import ch.amana.android.cputuner.provider.db.DB;
 import ch.amana.android.cputuner.provider.db.DB.VirtualGovernor;
 import ch.amana.android.cputuner.view.fragments.GovernorBaseFragment;
 import ch.amana.android.cputuner.view.fragments.GovernorFragment;
 import ch.amana.android.cputuner.view.fragments.GovernorFragmentCallback;
 import ch.amana.android.cputuner.view.fragments.VirtualGovernorFragment;
 
 public class CurInfo extends FragmentActivity implements GovernorFragmentCallback {
 
 	private static final int[] lock = new int[1];
 	private CpuTunerReceiver receiver;
 
 	private CpuHandler cpuHandler;
 	private SeekBar sbCpuFreqMax;
 	private TextView tvCpuFreqMax;
 	private SeekBar sbCpuFreqMin;
 	private TextView tvCpuFreqMin;
 	private TextView tvBatteryLevel;
 	private TextView tvAcPower;
 	private TextView tvCurrentTrigger;
 	private int[] availCpuFreqsMin;
 	private int[] availCpuFreqsMax;
 	private TextView labelCpuFreqMin;
 	private TextView labelCpuFreqMax;
 	private TextView tvBatteryCurrent;
 	private PowerProfiles powerProfiles;
 	private Spinner spProfiles;
 	private TextView labelBatteryCurrent;
 	private GovernorBaseFragment governorFragment;
 	private GovernorHelperCurInfo governorHelper;
 	private TextView tvPulse;
 	private TableRow trPulse;
 	private TextView spacerPulse;
 	private TableRow trMaxFreq;
 	private TableRow trMinFreq;
 	private TableRow trBatteryCurrent;
 
 	protected class CpuTunerReceiver extends BroadcastReceiver {
 
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			String action = intent.getAction();
 			acPowerChanged();
 			batteryLevelChanged();
 			if (Notifier.BROADCAST_TRIGGER_CHANGED.equals(action)
 					|| Notifier.BROADCAST_PROFILE_CHANGED.equals(action)) {
 				profileChanged();
 			}
 
 		}
 	}
 
 	public void registerReceiver() {
 		synchronized (lock) {
 			IntentFilter deviceStatusFilter = new IntentFilter(Notifier.BROADCAST_DEVICESTATUS_CHANGED);
 			IntentFilter triggerFilter = new IntentFilter(Notifier.BROADCAST_TRIGGER_CHANGED);
 			IntentFilter profileFilter = new IntentFilter(Notifier.BROADCAST_PROFILE_CHANGED);
 			receiver = new CpuTunerReceiver();
 			registerReceiver(receiver, deviceStatusFilter);
 			registerReceiver(receiver, triggerFilter);
 			registerReceiver(receiver, profileFilter);
 			Logger.i("Registered CpuTunerReceiver");
 		}
 	}
 
 	public void unregisterReceiver() {
 		synchronized (lock) {
 			if (receiver != null) {
 				try {
 					unregisterReceiver(receiver);
 					receiver = null;
 				} catch (Throwable e) {
 					Logger.w("Could not unregister BatteryReceiver", e);
 				}
 			}
 		}
 	}
 
 	private class GovernorHelperCurInfo implements IGovernorModel {
 
 		@Override
 		public int getGovernorThresholdUp() {
 			return cpuHandler.getGovThresholdUp();
 		}
 
 		@Override
 		public int getGovernorThresholdDown() {
 			return cpuHandler.getGovThresholdDown();
 		}
 
 		@Override
 		public void setGov(String gov) {
 			cpuHandler.setCurGov(gov);
 		}
 
 		@Override
 		public void setGovernorThresholdUp(String string) {
 			try {
 				setGovernorThresholdUp(Integer.parseInt(string));
 			} catch (Exception e) {
 				Logger.w("Cannot parse " + string + " as int");
 			}
 		}
 
 		@Override
 		public void setGovernorThresholdDown(String string) {
 			try {
 				setGovernorThresholdDown(Integer.parseInt(string));
 			} catch (Exception e) {
 				Logger.w("Cannot parse " + string + " as int");
 			}
 		}
 
 		@Override
 		public void setScript(String string) {
 			// not used
 
 		}
 
 		@Override
 		public String getGov() {
 			return cpuHandler.getCurCpuGov();
 		}
 
 
 		@Override
 		public String getScript() {
 			// not used
 			return "";
 		}
 
 		@Override
 		public void setGovernorThresholdUp(int i) {
 			cpuHandler.setGovThresholdUp(i);
 		}
 
 		@Override
 		public void setGovernorThresholdDown(int i) {
 			cpuHandler.setGovThresholdDown(i);
 		}
 
 		@Override
 		public void setVirtualGovernor(long id) {
 			Cursor c = managedQuery(VirtualGovernor.CONTENT_URI, VirtualGovernor.PROJECTION_DEFAULT, DB.SELECTION_BY_ID, new String[] { id + "" },
 					VirtualGovernor.SORTORDER_DEFAULT);
 			if (c.moveToFirst()) {
 				VirtualGovernorModel vgm = new VirtualGovernorModel(c);
 				cpuHandler.applyGovernorSettings(vgm);
 				powerProfiles.getCurrentProfile().setVirtualGovernor(id);
 			}
 		}
 
 		@Override
 		public long getVirtualGovernor() {
 			return powerProfiles.getCurrentProfile().getVirtualGovernor();
 		}
 
 		@Override
 		public void setPowersaveBias(int powersaveBias) {
 			cpuHandler.setPowersaveBias(powersaveBias);
 		}
 
 		@Override
 		public int getPowersaveBias() {
 			return cpuHandler.getPowersaveBias();
 		}
 
 		@Override
 		public boolean hasScript() {
 			return false;
 		}
 
 	}
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		
 		SettingsStorage settings = SettingsStorage.getInstance();
 		if (!settings.isUserLevelSet()) {
 			UserExperianceLevelChooser uec = new UserExperianceLevelChooser(this);
 			uec.show();
 		}
 		
 		setContentView(R.layout.cur_info);
 		
 		cpuHandler = CpuHandler.getInstance();
 		powerProfiles = PowerProfiles.getInstance();
 
 		availCpuFreqsMax = cpuHandler.getAvailCpuFreq();
 		availCpuFreqsMin = cpuHandler.getAvailCpuFreq(true);
 
 		tvCurrentTrigger = (TextView) findViewById(R.id.tvCurrentTrigger);
 		spProfiles = (Spinner) findViewById(R.id.spProfiles);
 		tvBatteryLevel = (TextView) findViewById(R.id.tvBatteryLevel);
 		tvAcPower = (TextView) findViewById(R.id.tvAcPower);
 		tvBatteryCurrent = (TextView) findViewById(R.id.tvBatteryCurrent);
 		tvBatteryLevel = (TextView) findViewById(R.id.tvBatteryLevel);
 		tvCpuFreqMax = (TextView) findViewById(R.id.tvCpuFreqMax);
 		tvCpuFreqMin = (TextView) findViewById(R.id.tvCpuFreqMin);
 		labelCpuFreqMin = (TextView) findViewById(R.id.labelCpuFreqMin);
 		labelCpuFreqMax = (TextView) findViewById(R.id.labelCpuFreqMax);
 		sbCpuFreqMax = (SeekBar) findViewById(R.id.SeekBarCpuFreqMax);
 		sbCpuFreqMin = (SeekBar) findViewById(R.id.SeekBarCpuFreqMin);
 		labelBatteryCurrent = (TextView) findViewById(R.id.labelBatteryCurrent);
 		trPulse = (TableRow) findViewById(R.id.TableRowPulse);
 		tvPulse = (TextView) findViewById(R.id.tvPulse);
 		spacerPulse = (TextView) findViewById(R.id.spacerPulse);
 		trMaxFreq = (TableRow) findViewById(R.id.TableRowMaxFreq);
 		trMinFreq = (TableRow) findViewById(R.id.TableRowMinFreq);
 		trBatteryCurrent = (TableRow) findViewById(R.id.TableRowBatteryCurrent);
 
 		governorHelper = new GovernorHelperCurInfo();
 		if (settings.isUseVirtualGovernors()) {
 			governorFragment = new VirtualGovernorFragment(this, governorHelper);
 		} else {
 			governorFragment = new GovernorFragment(this, governorHelper, true);
 		}
 		FragmentManager fragmentManager = getSupportFragmentManager();
 		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
 		fragmentTransaction.add(R.id.llGovernorFragmentAncor, governorFragment);
 		fragmentTransaction.commit();
 
 		Cursor cursor = managedQuery(DB.CpuProfile.CONTENT_URI, DB.CpuProfile.PROJECTION_PROFILE_NAME, null, null, DB.CpuProfile.SORTORDER_DEFAULT);
 
 		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, cursor, new String[] { DB.CpuProfile.NAME_PROFILE_NAME },
 				new int[] { android.R.id.text1 });
 		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		spProfiles.setAdapter(adapter);
 
 		spProfiles.setOnItemSelectedListener(new OnItemSelectedListener() {
 
 			@Override
 			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
 				String profile = parent.getItemAtPosition(pos).toString();
 				ProfileModel currentProfile = powerProfiles.getCurrentProfile();
 				if (profile != null && !profile.equals(currentProfile)) {
 					powerProfiles.applyProfile(id);
 					governorFragment.updateView();
 				}
 			}
 
 			@Override
 			public void onNothingSelected(AdapterView<?> arg0) {
 				// TODO Auto-generated method stub
 
 			}
 		});
 
 		sbCpuFreqMax.setMax(availCpuFreqsMax.length - 1);
 		sbCpuFreqMax.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
 
 			@Override
 			public void onStopTrackingTouch(SeekBar seekBar) {
 				try {
 					int val = availCpuFreqsMax[seekBar.getProgress()];
 					if (val != cpuHandler.getMaxCpuFreq()) {
 						if (cpuHandler.setMaxCpuFreq(val)) {
 							Toast.makeText(CurInfo.this, getString(R.string.msg_setting_cpu_max_freq, val), Toast.LENGTH_LONG).show();
 						}
 						updateView();
 					}
 				} catch (ArrayIndexOutOfBoundsException e) {
 					Logger.e("Cannot set max freq in gui", e);
 				}
 			}
 
 			@Override
 			public void onStartTrackingTouch(SeekBar seekBar) {
 			}
 
 			@Override
 			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
 			}
 		});
 
 		sbCpuFreqMin.setMax(availCpuFreqsMin.length - 1);
 		sbCpuFreqMin.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
 
 			@Override
 			public void onStopTrackingTouch(SeekBar seekBar) {
 				try {
 					int val = availCpuFreqsMin[seekBar.getProgress()];
 					if (val != cpuHandler.getMinCpuFreq()) {
 						if (cpuHandler.setMinCpuFreq(val)) {
 							Toast.makeText(CurInfo.this, getString(R.string.setting_cpu_min_freq, val), Toast.LENGTH_LONG).show();
 						}
 						updateView();
 					}
 				} catch (ArrayIndexOutOfBoundsException e) {
 					Logger.e("Cannot set min freq in gui", e);
 				}
 			}
 
 			@Override
 			public void onStartTrackingTouch(SeekBar seekBar) {
 			}
 
 			@Override
 			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
 			}
 		});
 
 		OnClickListener startBattery = new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 				try {
 					Intent i = new Intent();
 					i.setClassName("com.android.settings", "com.android.settings.fuelgauge.PowerUsageSummary");
 					startActivity(i);
 				} catch (Throwable e) {
 				}
 
 			}
 		};
 		((TableRow) findViewById(R.id.TableRowBattery)).setOnClickListener(startBattery);
 		((TableRow) findViewById(R.id.TableRowBatteryCurrent)).setOnClickListener(startBattery);
 		((TableRow) findViewById(R.id.TableRowPower)).setOnClickListener(startBattery);
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		registerReceiver();
 		updateView();
 	}
 
 	@Override
 	protected void onPause() {
 		super.onPause();
 		unregisterReceiver();
 	}
 
 	public void updateView() {
 		batteryLevelChanged();
 		profileChanged();
 		acPowerChanged();
 	}
 
 	private void setSeekbar(int val, int[] valList, SeekBar seekBar, TextView textView) {
 		if (val == HardwareHandler.NO_VALUE_INT) {
 			textView.setText(R.string.notAvailable);
 		} else {
 			textView.setText(ProfileModel.convertFreq2GHz(val));
 		}
 		for (int i = 0; i < valList.length; i++) {
 			if (val == valList[i]) {
 				seekBar.setProgress(i);
 			}
 		}
 	}
 
 	private void batteryLevelChanged() {
 		StringBuilder bat = new StringBuilder();
 		bat.append(powerProfiles.getBatteryLevel()).append("%");
 		bat.append(" (");
 		if (powerProfiles.isBatteryHot()) {
 			bat.append(R.string.label_hot).append(" ");
 		}
 		bat.append(powerProfiles.getBatteryTemperature()).append(" °C)");
 		tvBatteryLevel.setText(bat.toString());
 		StringBuilder currentText = new StringBuilder();
 		BatteryHandler batteryHandler = BatteryHandler.getInstance();
 		int currentNow = batteryHandler.getBatteryCurrentNow();
 		if (currentNow > 0) {
 			currentText.append(batteryHandler.getBatteryCurrentNow()).append(" mA/h");
 		}
 		if (batteryHandler.hasAvgCurrent()) {
 			int currentAvg = batteryHandler.getBatteryCurrentAverage();
 			if (currentAvg != BatteryHandler.NO_VALUE_INT) {
 				currentText.append(" (").append(getString(R.string.label_avgerage)).append(" ").append(batteryHandler.getBatteryCurrentAverage()).append(" mA/h)");
 			}
 		}
 		if (currentText.length() > 0) {
 			GuiUtils.showViews(trBatteryCurrent, new View[] { labelBatteryCurrent, tvBatteryCurrent });
 			tvBatteryCurrent.setText(currentText.toString());
 		} else {
 			GuiUtils.hideViews(trBatteryCurrent, new View[] { labelBatteryCurrent, tvBatteryCurrent });
 		}
 	}
 
 	private void profileChanged() {
 		if (SettingsStorage.getInstance().isEnableProfiles()) {
 			if (PulseHelper.getInstance(this).isPulsing()) {
 				GuiUtils.showViews(trPulse, new View[] { spacerPulse, tvPulse });
 				int res = PulseHelper.getInstance(this).isOn() ? R.string.labelPulseOn : R.string.labelPulseOff;
 				tvPulse.setText(res);
 			} else {
 				GuiUtils.hideViews(trPulse, new View[] { spacerPulse, tvPulse });
 			}
 			ProfileModel currentProfile = powerProfiles.getCurrentProfile();
 			if (currentProfile != null) {
 				GuiUtils.setSpinner(spProfiles, currentProfile.getDbId());
 				spProfiles.setEnabled(true);
 			} else {
 				spProfiles.setEnabled(false);
 			}
 			tvCurrentTrigger.setText(powerProfiles.getCurrentTriggerName());
 		} else {
 			spProfiles.setEnabled(false);
 			tvCurrentTrigger.setText(R.string.notEnabled);
 		}
 
 		setSeekbar(cpuHandler.getMaxCpuFreq(), availCpuFreqsMax, sbCpuFreqMax, tvCpuFreqMax);
 		setSeekbar(cpuHandler.getMinCpuFreq(), availCpuFreqsMin, sbCpuFreqMin, tvCpuFreqMin);
 
 		GovernorConfig governorConfig = GovernorConfigHelper.getGovernorConfig(cpuHandler.getCurCpuGov());
 		if (governorConfig.hasNewLabelCpuFreqMax()) {
 			labelCpuFreqMax.setText(governorConfig.getNewLabelCpuFreqMax(this));
 		} else {
 			labelCpuFreqMax.setText(R.string.labelMax);
 		}
 		if (governorConfig.hasMinFrequency()) {
 			GuiUtils.showViews(trMinFreq, new View[] { labelCpuFreqMin, tvCpuFreqMin, sbCpuFreqMin });
 		} else {
 			GuiUtils.hideViews(trMinFreq, new View[] { labelCpuFreqMin, tvCpuFreqMin, sbCpuFreqMin });
 		}
 		if (governorConfig.hasMaxFrequency()) {
 			GuiUtils.showViews(trMaxFreq, new View[] { labelCpuFreqMax, tvCpuFreqMax, sbCpuFreqMax });
 		} else {
 			GuiUtils.hideViews(trMaxFreq, new View[] { labelCpuFreqMax, tvCpuFreqMax, sbCpuFreqMax });
 		}
 
 		governorFragment.updateView();
 	}
 
 	private void acPowerChanged() {
 		tvAcPower.setText(getText(powerProfiles.isAcPower() ? R.string.yes : R.string.no));
 	}
 
 	@Override
 	public void updateModel() {
 		// not used
 	}
 
 }
