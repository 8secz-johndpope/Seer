 package com.kh.beatbot.activity;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.DialogInterface;
 import android.graphics.Typeface;
 import android.os.Bundle;
 import android.text.method.DigitsKeyListener;
 import android.view.KeyEvent;
 import android.widget.EditText;
 import android.widget.LinearLayout;
 
 import com.kh.beatbot.GeneralUtils;
 import com.kh.beatbot.R;
 import com.kh.beatbot.effect.Effect;
 import com.kh.beatbot.event.SampleRenameEvent;
 import com.kh.beatbot.manager.FileManager;
 import com.kh.beatbot.manager.MidiFileManager;
 import com.kh.beatbot.manager.MidiManager;
 import com.kh.beatbot.manager.PlaybackManager;
 import com.kh.beatbot.manager.TrackManager;
 import com.kh.beatbot.ui.color.Colors;
 import com.kh.beatbot.ui.view.View;
 import com.kh.beatbot.ui.view.group.GLSurfaceViewGroup;
 import com.kh.beatbot.ui.view.group.ViewPager;
 import com.kh.beatbot.ui.view.page.MainPage;
 import com.kh.beatbot.ui.view.page.effect.EffectPage;
 
 public class BeatBotActivity extends Activity {
 
 	public static final int BPM_DIALOG_ID = 0, EXIT_DIALOG_ID = 1,
 			SAMPLE_NAME_EDIT_DIALOG_ID = 2, MIDI_FILE_NAME_EDIT_DIALOG_ID = 3;
 
 	private static final int MAIN_PAGE_NUM = 0, EFFECT_PAGE_NUM = 1;
 
 	private static ViewPager activityPager;
 	private static EditText bpmInput, midiFileNameInput, sampleNameInput;
 
 	public static BeatBotActivity mainActivity;
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		mainActivity = this;
 
 		GeneralUtils.initAndroidSettings(this);
 		View.font = Typeface.createFromAsset(getAssets(),
 				"REDRING-1969-v03.ttf");
 		Colors.initColors(this);
 
 		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.FILL_PARENT,
 				LinearLayout.LayoutParams.FILL_PARENT);
 
 		FileManager.init();
 		MidiFileManager.init();
 
 		View.root = new GLSurfaceViewGroup(this);
 		View.root.setLayoutParams(lp);
 
 		LinearLayout layout = new LinearLayout(this);
 		layout.addView(View.root);
 		setContentView(layout, lp);
 
 		View.mainPage = new MainPage();
 		View.effectPage = new EffectPage();
 
 		activityPager = new ViewPager();
 		activityPager.addPages(View.mainPage, View.effectPage);
 		activityPager.setPage(0);
 
 		((GLSurfaceViewGroup) View.root).setBBRenderer(activityPager);
 
		if (savedInstanceState == null) {
			initNativeAudio();
		}

 		TrackManager.init();
 		MidiManager.init();
		
		arm();
 	}
 
 	@Override
 	public void onDestroy() {
 		try {
 			super.onDestroy();
 			if (isFinishing()) {
 				shutdown();
 				// android.os.Process.killProcess(android.os.Process.myPid());
 			}
 		} finally {
 			FileManager.clearTempFiles();
 		}
 	}
 
 	@Override
 	public void onBackPressed() {
 		if (activityPager.getCurrPageNum() == MAIN_PAGE_NUM) {
 			showDialog(EXIT_DIALOG_ID);
 		} else if (activityPager.getCurrPageNum() == EFFECT_PAGE_NUM) {
 			View.mainPage.pageSelectGroup.updateLevelsFXPage();
 			activityPager.setPage(MAIN_PAGE_NUM);
 		}
 	}
 
 	@Override
 	public void onPause() {
 		View.root.onPause();
 		super.onPause();
 	}
 
 	@Override
 	public void onResume() {
 		super.onResume();
 		View.root.onResume();
 	}
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putBoolean("playing",
 				PlaybackManager.getState() == PlaybackManager.State.PLAYING);
 	}
 
 	@Override
 	protected void onPrepareDialog(int id, Dialog dialog) {
 		switch (id) {
 		case BPM_DIALOG_ID:
 			bpmInput.setText(String.valueOf((int) MidiManager.getBPM()));
 			break;
 		case SAMPLE_NAME_EDIT_DIALOG_ID:
 			sampleNameInput.setText(TrackManager.currTrack.getCurrSampleName());
 			break;
 		case MIDI_FILE_NAME_EDIT_DIALOG_ID:
 
 		case EXIT_DIALOG_ID:
 			break;
 		}
 	}
 
 	@Override
 	protected Dialog onCreateDialog(int id) {
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 
 		switch (id) {
 		case BPM_DIALOG_ID:
 			bpmInput = new EditText(this);
 
 			bpmInput.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
 			builder.setTitle("Set BPM")
 					.setView(bpmInput)
 					.setPositiveButton("OK",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									String bpmString = bpmInput.getText()
 											.toString();
 									if (!bpmString.isEmpty()) {
 										View.mainPage.pageSelectGroup
 												.getMasterPage()
 												.setBPM(Integer
 														.valueOf(bpmString));
 									}
 								}
 							})
 					.setNegativeButton("Cancel",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									dialog.cancel();
 								}
 							});
 			break;
 		case SAMPLE_NAME_EDIT_DIALOG_ID:
 			sampleNameInput = new EditText(this);
 
 			builder.setTitle("Edit Sample Name")
 					.setView(sampleNameInput)
 					.setPositiveButton("OK",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									String sampleName = sampleNameInput
 											.getText().toString();
 									new SampleRenameEvent(
 											TrackManager.currTrack, sampleName)
 											.execute();
 								}
 							})
 					.setNegativeButton("Cancel",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									dialog.cancel();
 								}
 							});
 			break;
 		case MIDI_FILE_NAME_EDIT_DIALOG_ID:
 			midiFileNameInput = new EditText(this);
 
 			builder.setTitle("Save MIDI as:")
 					.setView(midiFileNameInput)
 					.setPositiveButton("OK",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									String midiFileName = midiFileNameInput
 											.getText().toString();
 									MidiFileManager.exportMidi(midiFileName);
 								}
 							})
 					.setNegativeButton("Cancel",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									dialog.cancel();
 								}
 							});
 			break;
 
 		case EXIT_DIALOG_ID:
 			builder.setIcon(android.R.drawable.ic_dialog_alert)
 					.setTitle("Closing " + getString(R.string.app_name))
 					.setMessage(
 							"Are you sure you want to exit "
 									+ getString(R.string.app_name) + "?")
 					.setPositiveButton("Yes",
 							new DialogInterface.OnClickListener() {
 								@Override
 								public void onClick(DialogInterface dialog,
 										int which) {
 									try {
 										finish();
 									} catch (Exception e) {
 										FileManager.clearTempFiles();
 									}
 								}
 							}).setNegativeButton("No", null);
 			break;
 		}
 
 		return builder.create();
 	}
 
 	@Override
 	public boolean onKeyUp(int keyCode, KeyEvent event) {
 		if (keyCode == KeyEvent.KEYCODE_MENU) {
 			View.mainPage.notifyMenuExpanded();
 			return true;
 		}
 		return super.onKeyUp(keyCode, event);
 	}
 
 	/*
 	 * Set up the project. For now, this just means setting track 0, page 0 view
 	 */
 	public void setupProject() {
 		TrackManager.getTrack(0).select();
 		View.mainPage.pageSelectGroup.selectPage(0);
 	}
 
 	public void launchEffect(Effect effect) {
 		View.effectPage.loadEffect(effect);
 		activityPager.setPage(EFFECT_PAGE_NUM);
 	}
 
 	private void initNativeAudio() {
 		createEngine();
 		createAudioPlayer();
 	}
 
 	private void shutdown() {
 		nativeShutdown();
 	}
 
 	public static native boolean createAudioPlayer();
 
 	public static native void createEngine();
 
	public static native void arm();

 	public static native void nativeShutdown();
 
 	/** Load jni .so on initialization */
 	static {
 		System.loadLibrary("nativeaudio");
 	}
 }
