 /* 
  * This file is part of OppiaMobile - http://oppia-mobile.org/
  * 
  * OppiaMobile is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * OppiaMobile is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.digitalcampus.oppia.activity;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.concurrent.Callable;
 
 import org.digitalcampus.mobile.learning.R;
 import org.digitalcampus.oppia.adapter.SectionListAdapter;
 import org.digitalcampus.oppia.model.Activity;
 import org.digitalcampus.oppia.model.Course;
 import org.digitalcampus.oppia.model.Section;
 import org.digitalcampus.oppia.utils.ImageUtils;
 import org.digitalcampus.oppia.utils.UIUtils;
 import org.digitalcampus.oppia.widgets.PageWidget;
 import org.digitalcampus.oppia.widgets.QuizWidget;
 import org.digitalcampus.oppia.widgets.ResourceWidget;
 import org.digitalcampus.oppia.widgets.WidgetFactory;
 
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.graphics.drawable.BitmapDrawable;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.speech.tts.TextToSpeech;
 import android.speech.tts.UtteranceProgressListener;
 import android.speech.tts.TextToSpeech.OnInitListener;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentTransaction;
 import android.util.Log;
 import android.widget.Toast;
 
 import com.actionbarsherlock.app.ActionBar;
 import com.actionbarsherlock.app.ActionBar.Tab;
 import com.actionbarsherlock.app.SherlockFragmentActivity;
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuItem;
 
 
 public class CourseActivity extends SherlockFragmentActivity implements ActionBar.TabListener, OnInitListener {
 
 	public static final String TAG = CourseActivity.class.getSimpleName();
 	public static final String BASELINE_TAG = "BASELINE";
 	private Section section;
 	private Course course;
 	private int currentActivityNo = 0;
 	private WidgetFactory currentActivity;
 	private SharedPreferences prefs;
 	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
 	private ArrayList<Activity> activities;
 	private boolean isBaseline = false;
 	private ActionBar actionBar;
 	
 	private static int TTS_CHECK = 0;
 	private static TextToSpeech myTTS;
 	private boolean ttsRunning = false;
 
 	private HashMap<String, Object> widgetState = new HashMap<String, Object>();
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		setContentView(R.layout.activity_course);
 		actionBar = getSupportActionBar();
 		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
 		actionBar.setDisplayHomeAsUpEnabled(true);
 		actionBar.setHomeButtonEnabled(true);
 
 		prefs = PreferenceManager.getDefaultSharedPreferences(this);
 
 		Bundle bundle = this.getIntent().getExtras();
 		if (bundle != null) {
 			section = (Section) bundle.getSerializable(Section.TAG);
 			course = (Course) bundle.getSerializable(Course.TAG);
 			currentActivityNo = (Integer) bundle.getSerializable(SectionListAdapter.TAG_PLACEHOLDER);
 			if (bundle.getSerializable(CourseActivity.BASELINE_TAG) != null) {
 				this.isBaseline = (Boolean) bundle.getBoolean(CourseActivity.BASELINE_TAG);
 			}
 			//set image
			BitmapDrawable bm = ImageUtils.LoadBMPsdcard(course.getImageFile(), this.getResources(), R.drawable.dc_logo);
 			actionBar.setIcon(bm);
 		}
 	}
 
 	@Override
 	public void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		// Serialize the current tab position.
 		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
 		outState.putSerializable("widget_config", currentActivity.getWidgetConfig());
 		super.onSaveInstanceState(outState);
 	}
 
 	@Override
 	public void onRestoreInstanceState(Bundle savedInstanceState) {
 		// Restore the previously serialized current tab position.
 		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
 			getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
 		}
 		savedInstanceState.putSerializable("widget_config", currentActivity.getWidgetConfig());
 		super.onRestoreInstanceState(savedInstanceState);
 	}
 
 	@Override
 	public void onStart() {
 		super.onStart();
 		String actionBarTitle = section.getTitle(prefs.getString(getString(R.string.prefs_language), Locale.getDefault().getLanguage()));
 		if(actionBarTitle != null){
 			setTitle(actionBarTitle);
 		} else if (isBaseline) {
 			setTitle(getString(R.string.title_baseline));
 		}
 		actionBar.removeAllTabs();
 		activities = section.getActivities();
 		for (int i = 0; i < activities.size(); i++) {
 			String title = section
 					.getActivities()
 					.get(i)
 					.getTitle(
 							prefs.getString(getString(R.string.prefs_language), Locale.getDefault().getLanguage()));
 			boolean tabSelected = false;
 			if(i == currentActivityNo){
 				tabSelected = true;
 			}
 			actionBar.addTab(actionBar.newTab().setText(title).setTabListener(this),tabSelected);
 		}
 	}
 	
 	@Override
 	public void onPause() {
 		super.onPause();
 		if (myTTS != null) {
 			myTTS.shutdown();
 			myTTS = null;
 		}	
 		if (currentActivity != null) {
 			this.widgetState = currentActivity.getWidgetConfig();
 		}
 	}	
 	
 	@Override
 	protected void onDestroy() {
 		if (myTTS != null) {
 			myTTS.shutdown();
 			myTTS = null;
 		}
 		
 		super.onDestroy();
 	}
 	
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		getSupportMenuInflater().inflate(R.menu.activity_course, menu);
 		return true;
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		MenuItem item = (MenuItem) menu.findItem(R.id.menu_tts);
 		if (ttsRunning) {
 			item.setTitle(R.string.menu_stop_read_aloud);
 		} else {
 			item.setTitle(R.string.menu_read_aloud);
 		}
 		return super.onPrepareOptionsMenu(menu);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		// Handle item selection
 		switch (item.getItemId()) {
 		case R.id.menu_language:
 			createLanguageDialog();
 			return true;
 		case R.id.menu_help:
 			startActivity(new Intent(this, HelpActivity.class));
 			return true;
 		case android.R.id.home:
 			this.finish();
 			return true;
 		case R.id.menu_tts:
 			if (myTTS == null && !ttsRunning) {
 				// check for TTS data
 				Intent checkTTSIntent = new Intent();
 				checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
 				startActivityForResult(checkTTSIntent, TTS_CHECK);
 			} else if (myTTS != null && ttsRunning) {
 				this.stopReading();
 			} else {
 				// TTS not installed so show message
 				Toast.makeText(this, this.getString(R.string.error_tts_start), Toast.LENGTH_LONG).show();
 			}
 			supportInvalidateOptionsMenu();
 			return true;
 		default:
 			return super.onOptionsItemSelected(item);
 		}
 	}
 
 
 	private void createLanguageDialog() {
 		UIUtils ui = new UIUtils();
 		ui.createLanguageDialog(this, course.getLangs(), prefs, new Callable<Boolean>() {
 			public Boolean call() throws Exception {
 				CourseActivity.this.onStart();
 				return true;
 			}
 		});
 	}
 
 	public WidgetFactory getCurrentActivity() {
 		return this.currentActivity;
 	}
 
 	public void onTabSelected(Tab tab, FragmentTransaction ft) {
 		stopReading();
 		supportInvalidateOptionsMenu();
 		Fragment fragment = null;
 		if (activities.get(tab.getPosition()).getActType().equals("page")) {
 			fragment =  new PageWidget();
 		} else if (activities.get(tab.getPosition()).getActType().equals("quiz")) {
 			fragment = new QuizWidget();
 		} else if (activities.get(tab.getPosition()).getActType().equals("resource")) {
 			fragment = new ResourceWidget();
 		}
 		
 		if (fragment != null){
 			Bundle args = new Bundle();
 			args.putSerializable(Activity.TAG,activities.get(tab.getPosition()));
 		    args.putSerializable(Course.TAG,course);
 		    args.putBoolean(CourseActivity.BASELINE_TAG, this.isBaseline);
 		    fragment.setArguments(args);
 			getSupportFragmentManager().beginTransaction().replace(R.id.activity_widget, fragment).commit();
 			currentActivity = (WidgetFactory) fragment;
 			currentActivity.setWidgetConfig(this.widgetState);
 			this.widgetState = new HashMap<String, Object>();
 		}
 		
 	}
 
 	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
 
 	}
 
 	public void onTabReselected(Tab tab, FragmentTransaction ft) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	public void onInit(int status) {
 		// check for successful instantiation
 		if (status == TextToSpeech.SUCCESS) {
 			Log.d(TAG, "tts success");
 			ttsRunning = true;
 			currentActivity.setReadAloud(true);
 			supportInvalidateOptionsMenu();
 			HashMap<String,String> params = new HashMap<String,String>();
 			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,TAG);
 			myTTS.speak(currentActivity.getContentToRead(), TextToSpeech.QUEUE_FLUSH, params);
 			myTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
 				
                 @Override
                 public void onDone(String utteranceId){
                 	Log.d(TAG,"Finished reading");
             		CourseActivity.this.ttsRunning = false;
             		myTTS = null;
                 }
 
                 @Override
                 public void onError(String utteranceId){
                 }
 
                 @Override
                 public void onStart(String utteranceId){
                 }
             	});
 		} else {
 			// TTS not installed so show message
 			Toast.makeText(this, this.getString(R.string.error_tts_start), Toast.LENGTH_LONG).show();
 		}
 	}
 
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 		if (requestCode == TTS_CHECK) {
 			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
 				// the user has the necessary data - create the TTS
 				myTTS = new TextToSpeech(this, this);
 				
 			}
 		}
 		super.onActivityResult(requestCode, resultCode, data);
 	}
 
 	private void stopReading() {
 		if (myTTS != null) {
 			myTTS.stop();
 			myTTS = null;
 		}
 		this.ttsRunning = false;
 		
 	}
 	
 	
 }
