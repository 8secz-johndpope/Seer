 package com.droidplanner.fragments;
 
 import android.app.Activity;
 import android.app.Fragment;
 import android.app.FragmentManager;
 import android.os.Bundle;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 
 import com.droidplanner.DroidPlannerApp;
 import com.droidplanner.R;
 import com.droidplanner.MAVLink.MavLinkStreamRates;
 import com.droidplanner.activitys.ConfigurationActivity;
 import com.droidplanner.calibration.CH_CalParameters;
 import com.droidplanner.calibration.CalParameters;
 import com.droidplanner.calibration.CalParameters.OnCalibrationEvent;
 import com.droidplanner.calibration.RC_CalParameters;
 import com.droidplanner.drone.Drone;
 import com.droidplanner.drone.DroneInterfaces.DroneEventsType;
 import com.droidplanner.drone.DroneInterfaces.OnDroneListner;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCCompleted;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCMenu;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCMiddle;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCMinMax;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCOptions;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCPanel;
 import com.droidplanner.fragments.calibration.rc.FragmentSetupRCProgress;
 import com.droidplanner.widgets.FillBar.FillBarMinMaxL;
 import com.droidplanner.widgets.FillBar.FillBarMinMaxR;
 import com.droidplanner.widgets.FillBar.FillBarMinMaxText;
 import com.droidplanner.widgets.RcStick.RcStick;
 
 public class RcSetupFragment extends Fragment implements OnDroneListner,
		OnCalibrationEvent, OnPageChangeListener {
 
 	private static final int RC_MIN = 900;
 	private static final int RC_MAX = 2100;
 	// Extreme RC update rate in this screen
 	private static final int RC_MSG_RATE = 50;
 
 	private static final String[] RCStr = { "AIL ", "ELE ", "THR ", "RUD ",
 			"CH 5", "CH 6", "CH 7", "CH 8" };
 
 	private Drone drone;
 	private ConfigurationActivity parent;
 	
 	private RC_CalParameters rcParameters;
 	private CH_CalParameters chParameters;
 	private CalParameters currParameters = null;
 
 	private FillBarMinMaxR barELE;
 	private FillBarMinMaxL barTHR;
 	private FillBarMinMaxText barYAW;
 	private FillBarMinMaxText barAIL;
 	private FillBarMinMaxText bar5;
 	private FillBarMinMaxText bar6;
 	private FillBarMinMaxText bar7;
 	private FillBarMinMaxText bar8;
 
 	private RcStick stickLeft;
 	private RcStick stickRight;
 
 	private FragmentManager fragmentManager;
 	private FragmentSetupRCPanel setupPanel;
 
 	int data[], cMin[] = new int[8], cMid[] = new int[8], cMax[] = new int[8];
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		rcParameters = new RC_CalParameters(drone);
 		chParameters = new CH_CalParameters(drone);
 		fragmentManager = getFragmentManager();
 		setupPanel = (FragmentSetupRCPanel) fragmentManager
 				.findFragmentById(R.id.fragment_setup_rc_panel);
 	}
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container,
 			Bundle savedInstanceState) {
 		View view = inflater.inflate(R.layout.fragment_setup_rc, container,
 				false);
 		setupLocalViews(view);
 		setupFragmentPanel(view);
 
 		return view;
 	}
 
 	private void setupFragmentPanel(View view) {
 		if (setupPanel == null) {
 			setupPanel = new FragmentSetupRCMenu();
 			((FragmentSetupRCMenu) setupPanel).rcSetupFragment = this;
 			fragmentManager.beginTransaction()
 					.add(R.id.fragment_setup_rc_panel, setupPanel).commit();
 		} else {
 			cancel();
 		}
 	}
 
 	@Override
 	public void onAttach(Activity activity) {
 		parent = (ConfigurationActivity)activity;
		parent.addOnPageChangeListener(this);
 		super.onAttach(activity);
 	}
 
 	@Override
 	public void onDetach() {
 		// TODO Auto-generated method stub
 		super.onDetach();
 	}
 
 	@Override
 	public void onActivityCreated(Bundle savedInstanceState) {
 		this.drone = ((DroidPlannerApp) getActivity().getApplication()).drone;
 		super.onActivityCreated(savedInstanceState);
 	}
 
 	@Override
 	public void onStart() {
 		drone.events.addDroneListener(this);
 		setupDataStreamingForRcSetup();
 		super.onStart();
 	}
 
 	@Override
 	public void onStop() {
 		drone.events.removeDroneListener(this);
 		resetDataStreamingForRcSetup();
 		super.onStop();
 	}
 
 
 	@Override
 	public void onResume() {
 		// TODO Auto-generated method stub
 		super.onResume();
 		if (setupPanel != null) {
 			setupPanel.rcSetupFragment = this;
 		}
 
 		if (rcParameters != null) {
 			rcParameters.setOnCalibrationEventListener(this);
 		}
 
 		if (chParameters != null) {
 			chParameters.setOnCalibrationEventListener(this);
 		}
 
 	}
 
 	@Override
 	public void onDroneEvent(DroneEventsType event, Drone drone) {
 		switch (event) {
 		case RC_IN:
 			onNewInputRcData();
 			break;
 		case RC_OUT:
 			break;
 		case PARAMETER:
 
 			if (currParameters != null) {
 				currParameters.processReceivedParam();
 			}
 			break;
 		default:
 			break;
 		}
 	}
 
 	private void resetDataStreamingForRcSetup() {
 		MavLinkStreamRates
 		.setupStreamRatesFromPref((DroidPlannerApp) getActivity()
 				.getApplication());
 	}
 
 	private void setupDataStreamingForRcSetup() {
 		MavLinkStreamRates.setupStreamRates(drone.MavClient, 1, 0, 1, 1, 1,
 				RC_MSG_RATE, 0, 0);
 	}
 
 	private void onNewInputRcData() {
 		data = drone.RC.in;
 		barAIL.setValue(data[0]);
 		barELE.setValue(data[1]);
 		barTHR.setValue(data[2]);
 		barYAW.setValue(data[3]);
 		bar5.setValue(data[4]);
 		bar6.setValue(data[5]);
 		bar7.setValue(data[6]);
 		bar8.setValue(data[7]);
 
 		float x, y;
 		x = (data[3] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
 		y = (data[2] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
 		stickLeft.setPosition(x, y);
 
 		x = (data[0] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
 		y = (data[1] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
 		stickRight.setPosition(x, -y);
 	}
 
 	private void setupLocalViews(View view) {
 		stickLeft = (RcStick) view.findViewById(R.id.stickLeft);
 		stickRight = (RcStick) view.findViewById(R.id.stickRight);
 		barTHR = (FillBarMinMaxL) view.findViewById(R.id.fillBarTHR);
 		barELE = (FillBarMinMaxR) view.findViewById(R.id.fillBarELE);
 		barYAW = (FillBarMinMaxText) view.findViewById(R.id.fillBarYAW);
 		barAIL = (FillBarMinMaxText) view.findViewById(R.id.fillBarAIL);
 		bar5 = (FillBarMinMaxText) view.findViewById(R.id.fillBar5);
 		bar6 = (FillBarMinMaxText) view.findViewById(R.id.fillBar6);
 		bar7 = (FillBarMinMaxText) view.findViewById(R.id.fillBar7);
 		bar8 = (FillBarMinMaxText) view.findViewById(R.id.fillBar8);
 
 		barAIL.setup("AILERON", RC_MAX, RC_MIN);
 		barELE.setup("ELEVATOR", RC_MAX, RC_MIN);
 		barTHR.setup("THROTTLE", RC_MAX, RC_MIN);
 		barYAW.setup("RUDDER", RC_MAX, RC_MIN);
 		bar5.setup("CH 5", RC_MAX, RC_MIN);
 		bar6.setup("CH 6", RC_MAX, RC_MIN);
 		bar7.setup("CH 7", RC_MAX, RC_MIN);
 		bar8.setup("CH 8", RC_MAX, RC_MIN);
 	}
 
 	private void setFillBarShowMinMax(boolean b) {
 		barAIL.setShowMinMax(b);
 		barELE.setShowMinMax(b);
 		barTHR.setShowMinMax(b);
 		barYAW.setShowMinMax(b);
 		bar5.setShowMinMax(b);
 		bar6.setShowMinMax(b);
 		bar7.setShowMinMax(b);
 		bar8.setShowMinMax(b);
 	}
 
 	public void changeSetupPanel(int step) {
 		switch (step) {
 		case 0:
 			setupPanel = getRCMenuPanel();
 			break;
 		case 1:
 			currParameters = rcParameters;
 			setupPanel = getRCCalibrationPanel();
 			break;
 		case 2:
 			setupPanel = new FragmentSetupRCMiddle();
 			setupPanel.rcSetupFragment = this;
 			break;
 		case 3:
 			setupPanel = new FragmentSetupRCCompleted();
 			setupPanel.rcSetupFragment = this;
 			((FragmentSetupRCCompleted) setupPanel)
 					.setText(getCalibrationStr());
 			break;
 		case 4:
 			currParameters = chParameters;
 			setupPanel = getCHCalibrationPanel();
 			break;
 		}
 		fragmentManager.beginTransaction()
 				.replace(R.id.fragment_setup_rc_panel, setupPanel).commit();
 	}
 
 	private FragmentSetupRCPanel getRCMenuPanel() {
 		if (currParameters != null) {
 			setupPanel = new FragmentSetupRCProgress();
 			((FragmentSetupRCProgress) setupPanel).rcSetupFragment = this;
 		} else {
 			currParameters = null;
 			setupPanel = new FragmentSetupRCMenu();
 			((FragmentSetupRCMenu) setupPanel).rcSetupFragment = this;
 		}
 		return setupPanel;
 	}
 
 	private FragmentSetupRCPanel getRCCalibrationPanel() {
 		if (!rcParameters.isParameterDownloaded()) {
 			currParameters = rcParameters;
 			setupPanel = new FragmentSetupRCProgress();
 			((FragmentSetupRCProgress) setupPanel).rcSetupFragment = this;
 			rcParameters.getCalibrationParameters(drone);
 		} else {
 			setFillBarShowMinMax(true);
 			setupPanel = new FragmentSetupRCMinMax();
 			((FragmentSetupRCMinMax) setupPanel).rcSetupFragment = this;
 		}
 		// TODO Auto-generated method stub
 		return setupPanel;
 	}
 
 	private FragmentSetupRCPanel getCHCalibrationPanel() {
 		if (!chParameters.isParameterDownloaded()) {
 			currParameters = chParameters;
 			setupPanel = new FragmentSetupRCProgress();
 			setupPanel.rcSetupFragment = this;
 			chParameters.getCalibrationParameters(drone);
 		} else {
 			setupPanel = new FragmentSetupRCOptions();
 			setupPanel.rcSetupFragment = this;
 			((FragmentSetupRCOptions) setupPanel)
 					.setOptionCH7((int) chParameters
 							.getParamValueByName("CH7_OPT"));
 			((FragmentSetupRCOptions) setupPanel)
 					.setOptionCH8((int) chParameters
 							.getParamValueByName("CH8_OPT"));
 		}
 		return setupPanel;
 	}
 
 	private String getCalibrationStr() {
 		String txt = "RC #\t\tMIN\t\tMID\t\tMAX";
 
 		cMin[0] = barAIL.getMin();
 		cMin[1] = barELE.getMin();
 		cMin[2] = barTHR.getMin();
 		cMin[3] = barYAW.getMin();
 		cMin[4] = bar5.getMin();
 		cMin[5] = bar6.getMin();
 		cMin[6] = bar7.getMin();
 		cMin[7] = bar8.getMin();
 
 		cMax[0] = barAIL.getMax();
 		cMax[1] = barELE.getMax();
 		cMax[2] = barTHR.getMax();
 		cMax[3] = barYAW.getMax();
 		cMax[4] = bar5.getMax();
 		cMax[5] = bar6.getMax();
 		cMax[6] = bar7.getMax();
 		cMax[7] = bar8.getMax();
 
 		if (data != null)
 			cMid = data;
 
 		for (int i = 0; i < 8; i++) {
 			txt += "\n" + RCStr[i] + "\t";
 			txt += "\t" + String.valueOf(cMin[i]) + "\t";
 			txt += "\t" + String.valueOf(cMid[i]) + "\t";
 			txt += "\t" + String.valueOf(cMax[i]);
 		}
 
 		return txt;
 	}
 
 	public void updateCalibrationData() {
 		currParameters = rcParameters;
 
 		for (int i = 0; i < 8; i++) {
 			rcParameters.setParamValueByName("RC" + String.valueOf(i + 1)
 					+ "_MIN", cMin[i]);
 			rcParameters.setParamValueByName("RC" + String.valueOf(i + 1)
 					+ "_MAX", cMax[i]);
 			rcParameters.setParamValueByName("RC" + String.valueOf(i + 1)
 					+ "_TRIM", cMid[i]);
 		}
 
 		setFillBarShowMinMax(false);
 		changeSetupPanel(0);
 		rcParameters.sendCalibrationParameters();
 
 	}
 
 	public void updateRCOptionsData() {
 		currParameters = chParameters;
 		int ch7 = ((FragmentSetupRCOptions) setupPanel).getOptionCH7();
 		int ch8 = ((FragmentSetupRCOptions) setupPanel).getOptionCH8();
 		chParameters.setParamValueByName("CH7_OPT", ch7);
 		chParameters.setParamValueByName("CH8_OPT", ch8);
 
 		changeSetupPanel(0);
 		chParameters.sendCalibrationParameters();
 	}
 
 	public void cancel() {
 		setFillBarShowMinMax(false);
 		currParameters = null;
 		changeSetupPanel(0);
 	}
 
 	@Override
 	public void onReadCalibration(CalParameters calParameters) {
 		if (currParameters.equals(rcParameters)) {
 			changeSetupPanel(1);
 		} else if (currParameters.equals(chParameters)) {
 			changeSetupPanel(4);
 		}
 	}
 
 	@Override
 	public void onSentCalibration(CalParameters calParameters) {
 		currParameters = null;
 		changeSetupPanel(0);
 	}
 
 	@Override
 	public void onCalibrationData(CalParameters calParameters, int index,
 			int count, boolean isSending) {
 		if (setupPanel != null && currParameters != null) {
 			String title;
 			if (isSending) {
 				if (currParameters.equals(rcParameters))
 					title = "Uploading RC calibration data";
 				else
 					title = "Uploading RC options data";
 			} else {
 				if (currParameters.equals(rcParameters))
 					title = "Downloading RC calibration data";
 				else
 					title = "Downloading RC options data";
 
 			}
 			((FragmentSetupRCProgress) setupPanel).setText(title);
 			((FragmentSetupRCProgress) setupPanel).updateProgress(index, count);
 		}
 	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int arg0) {
		if(arg0==2){
			Log.d("CAL", "RC Setup");
			setupDataStreamingForRcSetup();
		}
	}
 }
