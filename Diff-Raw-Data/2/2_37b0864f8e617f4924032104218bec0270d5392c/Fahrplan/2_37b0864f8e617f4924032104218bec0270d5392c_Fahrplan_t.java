 package nerd.tuxmobil.fahrplan.camp11;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import nerd.tuxmobil.fahrplan.camp11.CustomHttpClient.HTTP_STATUS;
 import nerd.tuxmobil.fahrplan.camp11.MyApp.TASKS;
 import android.app.Activity;
 import android.app.AlarmManager;
 import android.app.AlertDialog;
 import android.app.PendingIntent;
 import android.app.ProgressDialog;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.pm.PackageManager.NameNotFoundException;
 import android.content.res.Configuration;
 import android.database.Cursor;
 import android.database.sqlite.SQLiteDatabase;
 import android.database.sqlite.SQLiteException;
 import android.net.Uri;
 import android.os.Bundle;
 import android.text.format.Time;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.View.OnClickListener;
 import android.view.animation.Animation;
 import android.view.animation.AnimationUtils;
 import android.widget.ArrayAdapter;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.ProgressBar;
 import android.widget.ScrollView;
 import android.widget.Spinner;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.LinearLayout.LayoutParams;
 
 public class Fahrplan extends Activity implements response_callback,
 		OnClickListener, parser_callback {
 	private MyApp global;
 	private ProgressDialog progress = null;
 	private String LOG_TAG = "Fahrplan";
 	private FetchFahrplan fetcher;
 	private float scale;
 	private LayoutInflater inflater;
 	private int firstLectureStart = 0;
 	private int lastLectureEnd = 0;
 	private HashMap<String, Integer> trackColors;
 	private int day = 1;
 	private TextView dayTextView;
 	public static Context context = null;
 	public static String[] rooms = { "Kourou", "Baikonur" };
 	private FahrplanParser parser;
 	private ProgressBar progressSpinner;
 	private LinearLayout statusBar;
 	private Animation slideUpIn;
 	private ImageButton refreshBtn;
 	private Animation slideDownOut;
 	private TextView statusLineText;
 	private LecturesDBOpenHelper lecturesDB;
 	private MetaDBOpenHelper metaDB;
 	private SQLiteDatabase metadb = null;
 	private SQLiteDatabase lecturedb = null;
 	private SQLiteDatabase highlightdb = null;
 	private HashMap<String, Integer> trackColorsHi;
 	private HighlightDBOpenHelper highlightDB;
 	public static final String PREFS_NAME = "settings";
 
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		context = this;
 		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
 		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		setContentView(R.layout.main);
 		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
 				R.layout.custom_title);
 		global = (MyApp) getApplicationContext();
 		fetcher = new FetchFahrplan();
 		parser = new FahrplanParser();
 		scale = getResources().getDisplayMetrics().density;
 
 		trackColors = new HashMap<String, Integer>();
 		trackColors.put("Hacking", R.drawable.hacking_event_border);
 		trackColors.put("Society", R.drawable.society_event_border);
 		trackColors.put("Making", R.drawable.making_event_border);
 		trackColors.put("Community", R.drawable.community_event_border);
 		trackColors.put("Culture", R.drawable.culture_event_border);
 		trackColors.put("Science", R.drawable.science_event_border);
 		trackColors.put("Misc", R.drawable.misc_event_border);
 		trackColors.put("Hacker Space Program", R.drawable.hacker_space_program_event_border);
 		
 		trackColorsHi = new HashMap<String, Integer>();
 		trackColorsHi.put("Hacking", R.drawable.hacking_event_border_highlight);
 		trackColorsHi.put("Society", R.drawable.society_event_border_highlight);
 		trackColorsHi.put("Making", R.drawable.making_event_border_highlight);
 		trackColorsHi.put("Community", R.drawable.community_event_border_highlight);
 		trackColorsHi.put("Culture", R.drawable.culture_event_border_highlight);
 		trackColorsHi.put("Science", R.drawable.science_event_border_highlight);
 		trackColorsHi.put("Misc", R.drawable.misc_event_border_highlight);
 		trackColorsHi.put("Hacker Space Program", R.drawable.hacker_space_program_event_border_highlight);
 
 		final TextView leftText = (TextView) findViewById(R.id.title_left_text);
 		dayTextView = (TextView) findViewById(R.id.title_right_text);
 		leftText.setText(getString(R.string.app_name));
 		statusLineText = (TextView) findViewById(R.id.statusLineText);
 
 		progressSpinner = (ProgressBar) findViewById(R.id.leadProgressBar);
 		progressSpinner.setVisibility(View.GONE);
 		statusBar = (LinearLayout) findViewById(R.id.statusLine);
 		statusBar.setVisibility(View.GONE);
 		refreshBtn = (ImageButton) findViewById(R.id.refresh_button);
 		refreshBtn.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 				fetchFahrplan();
 			}
 		});
 
 		slideUpIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
 		slideDownOut = AnimationUtils
 				.loadAnimation(this, R.anim.slide_down_out);
 		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
 		day = prefs.getInt("displayDay", 1);
 
 		inflater = (LayoutInflater) this
 				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
 		lecturesDB = new LecturesDBOpenHelper(this);
 		metaDB = new MetaDBOpenHelper(this);
 		highlightDB = new HighlightDBOpenHelper(this);
 
 		loadMeta();
 
 		Intent intent = getIntent();
 		String lecture_id = intent.getStringExtra("lecture_id");
 		
 		if (lecture_id != null) {
 			Log.d(LOG_TAG,"Open with lecture_id "+lecture_id);
 			day = intent.getIntExtra("day", day);
 			Log.d(LOG_TAG,"day "+day);
 		}
 		
 		switch (MyApp.task_running) {
 		case FETCH:
 			Log.d(LOG_TAG, "fetch was pending, restart");
 			fetchFahrplan();
 			viewDay(false);
 			break;
 		case PARSE:
 			Log.d(LOG_TAG, "parse was pending, restart");
 			parseFahrplan();
 			break;
 		case NONE:
 			if (MyApp.numdays == 0) {
 				Log.d(LOG_TAG,"fetch in onCreate bc. numdays==0");
 				fetchFahrplan();
 			} else {
 				viewDay(lecture_id != null);	// auf jeden Fall reload, wenn mit Lecture ID gestartet
 			}
 			break;
 		}
 		
 		if (lecture_id != null) {
 			scrollTo(lecture_id);
 		}
 	}
 
 	public void parseFahrplan() {
 		if (MyApp.numdays == 0) {
 			// initial load
 			progress.setMessage(getResources().getString(
 					R.string.progress_processing_data));
 		} else {
 			statusLineText
 					.setText(getString(R.string.progress_processing_data));
 			if (statusBar.getVisibility() != View.VISIBLE) {
 				refreshBtn.setVisibility(View.GONE);
 				progressSpinner.setVisibility(View.VISIBLE);
 				statusBar.setVisibility(View.VISIBLE);
 				statusBar.startAnimation(slideUpIn);
 			}
 		}
 
 		MyApp.task_running = TASKS.PARSE;
 		parser.parse(this, MyApp.fahrplan_xml, global);
 	}
 
 	@Override
 	public void onDestroy() {
 		Log.d(LOG_TAG, "onDestroy");
 		super.onDestroy();
 		fetcher.cancel();
 		parser.cancel();
 		if (metadb != null) metadb.close();
 		if (lecturedb != null) lecturedb.close();
 		if (highlightdb != null) highlightdb.close();
 	}
 
 	@Override
 	public void onResume() {
 		Log.d(LOG_TAG, "onResume");
 		super.onResume();
 		fillTimes();
 	}
 
 	public static void updateRoomTitle(int room) {
 		if (context != null) {
 			TextView roomName = (TextView) ((Activity) context)
 					.findViewById(R.id.roomName);
 			if (roomName != null) {
 				try {
 					roomName.setText(rooms[room]);
 				} catch (ArrayIndexOutOfBoundsException e) {
 					roomName.setText(String.format("unknown %d", room));
 				}
 			}
 		}
 	}
 
 	public boolean onCreateOptionsMenu(Menu menu) {
 		super.onCreateOptionsMenu(menu);
 		MenuInflater mi = new MenuInflater(getApplication());
 		mi.inflate(R.menu.mainmenu, menu);
 		return true;
 	}
 
 	public void fetchFahrplan() {
 		if (MyApp.numdays == 0) {
 			// initial load
 			progress = ProgressDialog.show(this, "", getResources().getString(
 					R.string.progress_loading_data), true);
 		} else {
 			refreshBtn.setVisibility(View.GONE);
 			progressSpinner.setVisibility(View.VISIBLE);
 			statusLineText.setText(getString(R.string.progress_loading_data));
 			statusBar.setVisibility(View.VISIBLE);
 			statusBar.startAnimation(slideUpIn);
 		}
 		MyApp.task_running = TASKS.FETCH;
 		fetcher.fetch(this, "/camp/2011/Fahrplan/schedule.en.xml", global);
 	}
 
 	public boolean onOptionsItemSelected(MenuItem item) {
 		Intent intent;
 		switch (item.getItemId()) {
 		/*
 		case R.id.item_refresh:
 			fetchFahrplan();
 			return true;
 		case R.id.item_load:
 			loadMeta();
 			SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
 			day = prefs.getInt("displayDay", 1);
 			if (day > MyApp.numdays) {
 				day = 1;
 			}
 			viewDay(true);
 			return true;*/
 		case R.id.item_choose_day:
 			chooseDay();
 			return true;
 		case R.id.item_about:
 			aboutDialog();
 			return true;
 		case R.id.item_alarms:
 			intent = new Intent(this, AlarmList.class);
 			startActivity(intent);
 			return true;
 		case R.id.item_settings:
 			intent = new Intent(this, Prefs.class);
 			startActivity(intent);
 			return true;
 		}
 		return super.onOptionsItemSelected(item);
 	}
 
 	private void viewDay(boolean reload) {
 		loadLectureList(day, reload);
 		scanDayLectures();
 		HorizontalSnapScrollView scroller = (HorizontalSnapScrollView) findViewById(R.id.horizScroller);
 		if (scroller != null) {
 			scroller.scrollTo(0, 0);
 		}
 		updateRoomTitle(0);
 
 		fillTimes();
 		fillRoom("Kourou", R.id.raum1);
 		fillRoom("Baikonur", R.id.raum2);
 		dayTextView.setText(String
 				.format("%s %d", getString(R.string.day), day));
 	}
 
 	private void scrollTo(String lecture_id) {
 		int height;
 		switch (getResources().getConfiguration().orientation) {
 		case Configuration.ORIENTATION_LANDSCAPE:
 			Log.d(LOG_TAG, "landscape");
 			height = (int) (30 * scale);
 			break;
 		default:
 			Log.d(LOG_TAG, "other orientation");
 			height = (int) (40 * scale);
 			break;
 		}
 		for (Lecture lecture : MyApp.lectureList) {
 			if (lecture_id.equals(lecture.lecture_id)) {
 				final ScrollView parent = (ScrollView)findViewById(R.id.scrollView1);
 				final int pos = (lecture.relStartTime - firstLectureStart)/15 * height;
 				Log.d(LOG_TAG, "position is "+pos);
 				parent.post(new Runnable() {
 					
 					@Override
 					public void run() {
 						parent.scrollTo(0, pos);						
 					}
 				});
 				if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
 					final HorizontalSnapScrollView horiz = (HorizontalSnapScrollView)findViewById(R.id.horizScroller);
 					if (horiz != null) {
 						for (int i = 0; i < rooms.length; i++) {
 							if (rooms[i].equals(lecture.room)) {
 								Log.d(LOG_TAG,"scroll horiz to "+i);
 								final int hpos = i;
 								horiz.post(new Runnable() {
 									
 									@Override
 									public void run() {
 										horiz.scrollToColumn(hpos);
 									}
 								});
 								break;
 							}
 						}
 					}
 					
 				}
 				break;
 			}
 		}
 	}
 	
 	private void chooseDay() {
 		CharSequence items[] = new CharSequence[MyApp.numdays];
 		for (int i = 0; i < MyApp.numdays; i++) {
 			StringBuilder sb = new StringBuilder();
 			sb.append(getString(R.string.day)).append(" ").append(i + 1);
 			items[i] = sb.toString();
 		}
 
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder.setTitle(getString(R.string.choose_day));
 		builder.setItems(items, new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int item) {
 				day = item + 1;
 				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
 				SharedPreferences.Editor editor = settings.edit();
 				editor.putInt("displayDay", day);
 
 				editor.commit();
 
 				viewDay(true);
 			}
 		});
 		AlertDialog alert = builder.create();
 		alert.show();
 	}
 
 	@Override
 	public void onGotResponse(HTTP_STATUS status, String response) {
 		Log.d(LOG_TAG, "Response... " + status);
 		MyApp.task_running = TASKS.NONE;
 		if (status != HTTP_STATUS.HTTP_OK) {
 			switch (status) {
 			case HTTP_LOGIN_FAIL_UNTRUSTED_CERTIFICATE: {
 				if (MyApp.numdays == 0) {
 					progress.dismiss();
 				} else {
 					progressSpinner.setVisibility(View.GONE);
 					statusBar.startAnimation(slideDownOut);
 					statusBar.setVisibility(View.GONE);
 					refreshBtn.setVisibility(View.VISIBLE);
 				}
 				UntrustedCertDialogs.acceptKeyDialog(
 						R.string.dlg_certificate_message_fmt, this,
 						new cert_accepted() {
 
 							@Override
 							public void cert_accepted() {
 								Log.d(LOG_TAG, "fetch on cert accepted.");
 								fetchFahrplan();
 							}
 						}, (Object) null);
 			}
 				break;
 			}
 			CustomHttpClient.showHttpError(this, global, status);
 			if (MyApp.numdays == 0) {
 				progress.dismiss();
 			} else {
 				progressSpinner.setVisibility(View.GONE);
 				statusBar.startAnimation(slideDownOut);
 				statusBar.setVisibility(View.GONE);
 				refreshBtn.setVisibility(View.VISIBLE);
 			}
 			setProgressBarIndeterminateVisibility(false);
 			return;
 		}
 		Log.d(LOG_TAG, "yehhahh");
 
 		MyApp.fahrplan_xml = response;
 		parseFahrplan();
 	}
 
 	private void scanDayLectures() {
 		firstLectureStart = -1;
 		lastLectureEnd = -1;
 		for (Lecture lecture : MyApp.lectureList) {
 			if (firstLectureStart == -1) {
 				firstLectureStart = lecture.relStartTime;
 			} else if (lecture.relStartTime < firstLectureStart) {
 				firstLectureStart = lecture.relStartTime;
 			}
 			if (lastLectureEnd == -1) {
 				lastLectureEnd = lecture.relStartTime + lecture.duration;
 			} else if ((lecture.relStartTime + lecture.duration) > lastLectureEnd) {
 				lastLectureEnd = lecture.relStartTime + lecture.duration;
 			}
 		}
 	}
 
 	private void fillTimes() {
 		int time = firstLectureStart;
 		int printTime = time;
 		LinearLayout timeSpalte = (LinearLayout) findViewById(R.id.times_layout);
 		timeSpalte.removeAllViews();
 		int height;
 		Time now = new Time();
 		now.setToNow();
 		View event;
 
 		switch (getResources().getConfiguration().orientation) {
 		case Configuration.ORIENTATION_LANDSCAPE:
 			Log.d(LOG_TAG, "landscape");
 			height = (int) (30 * scale);
 			break;
 		default:
 			Log.d(LOG_TAG, "other orientation");
 			height = (int) (40 * scale);
 			break;
 		}
 
 		while (time < lastLectureEnd) {
 			StringBuilder sb = new StringBuilder();
 			int hour = printTime / 60;
 			int minute = printTime % 60;
 			sb.append(String.format("%02d", hour)).append(":");
 			sb.append(String.format("%02d", minute));
 			if ((now.hour == hour) && (now.minute >= minute)
 					&& (now.minute < (minute + 15))) {
 				event = inflater.inflate(R.layout.time_layout_now, null);
 			} else {
 				event = inflater.inflate(R.layout.time_layout, null);
 			}
 			timeSpalte.addView(event, LayoutParams.MATCH_PARENT, height);
 			TextView title = (TextView) event.findViewById(R.id.time);
 			title.setText(sb.toString());
 			time += 15;
 			printTime = time;
 			if (printTime >= (24 * 60)) {
 				printTime -= (24 * 60);
 			}
 		}
 	}
 
 	private int getEventPadding() {
 		int padding;
 		switch (getResources().getConfiguration().orientation) {
 		case Configuration.ORIENTATION_LANDSCAPE:
 			padding = (int) (8 * scale);
 			break;
 		default:
 			padding = (int) (10 * scale);
 			break;
 		}
 		return padding;
 	}
 	
 	private void fillRoom(String roomName, int roomId) {
 		LinearLayout room = (LinearLayout) findViewById(roomId);
 		room.removeAllViews();
 		int endTime = firstLectureStart;
 		int padding = getEventPadding();
 		int standardHeight;
 
 		switch (getResources().getConfiguration().orientation) {
 		case Configuration.ORIENTATION_LANDSCAPE:
 			Log.d(LOG_TAG, "landscape");
 			standardHeight = 30;
 			padding = (int) (8 * scale);
 			break;
 		default:
 			Log.d(LOG_TAG, "other orientation");
 			standardHeight = 40;
 			padding = (int) (10 * scale);
 			break;
 		}
 		for (Lecture lecture : MyApp.lectureList) {
 			if (roomName.equals(lecture.room)) {
 				if (lecture.relStartTime > endTime) {
 					View event = new View(this);
 					int height = (int) (standardHeight
 							* ((lecture.relStartTime - endTime) / 15) * scale);
 					room.addView(event, LayoutParams.MATCH_PARENT, height);
 				}
 				View event = inflater.inflate(R.layout.event_layout, null);
 				int height = (int) (standardHeight * (lecture.duration / 15) * scale);
 				room.addView(event, LayoutParams.MATCH_PARENT, height);
 				TextView title = (TextView) event
 						.findViewById(R.id.event_title);
 				title.setText(lecture.title);
 				title = (TextView) event.findViewById(R.id.event_subtitle);
 				title.setText(lecture.subtitle);
 				title = (TextView) event.findViewById(R.id.event_speakers);
 				title.setText(lecture.speakers.replaceAll(";", ", "));
 				title = (TextView) event.findViewById(R.id.event_track);
 				StringBuilder sb = new StringBuilder();
 				sb.append(lecture.track);
 				if ((lecture.lang != null) && (lecture.lang.length() > 0)) {
 					sb.append(" [").append(lecture.lang).append("]");
 				}
 				title.setText(sb.toString());
 
 				Integer drawable;
 				if (lecture.highlight) {
 					drawable = trackColorsHi.get(lecture.track);
 					padding += (int)(2 * scale);
 				} else {
 					drawable = trackColors.get(lecture.track);
 				}
 				if (drawable != null) {
 					event.setBackgroundResource(drawable);
 					event.setPadding(padding, padding, padding, padding);
 				}
 				event.setOnClickListener(this);
 				event.setLongClickable(true);
 //				event.setOnLongClickListener(this);
 				event.setOnCreateContextMenuListener(this);
 				event.setTag(lecture);
 				endTime = lecture.relStartTime + lecture.duration;
 			}
 		}
 	}
 
 	private void loadLectureList(int day, boolean force) {
 		Log.d(LOG_TAG, "load lectures of day " + day);
 
 		if (lecturedb == null) {
 			lecturedb = lecturesDB.getReadableDatabase();
 		}
 		if (highlightdb == null) {
 			highlightdb = highlightDB.getReadableDatabase();
 		}
 		if ((force == false) && (MyApp.lectureList != null) && (MyApp.lectureListDay == day)) return;
 		MyApp.lectureList = new ArrayList<Lecture>();
 		Cursor cursor, hCursor;
 
 		try {
 			cursor = lecturedb.query("lectures", LecturesDBOpenHelper.allcolumns,
 					"day=?", new String[] { String.format("%d", day) }, null,
 					null, "relStart");
 		} catch (SQLiteException e) {
 			e.printStackTrace();
 			lecturedb.close();
 			lecturedb = null;
 			return;
 		}
 		try {
 			hCursor = highlightdb.query("highlight", HighlightDBOpenHelper.allcolumns,
 					null, null, null,
 					null, null);
 		} catch (SQLiteException e) {
 			e.printStackTrace();
 			lecturedb.close();
 			lecturedb = null;
 			highlightdb.close();
 			highlightdb = null;
 			return;
 		}
 		Log.d(LOG_TAG, "Got " + cursor.getCount() + " rows.");
 		Log.d(LOG_TAG, "Got " + hCursor.getCount() + " highlight rows.");
 		
 		if (cursor.getCount() == 0) {
 			// evtl. Datenbankreset wg. DB Formatänderung -> neu laden
 			cursor.close();
 			Log.d(LOG_TAG,"fetch on loading empty lecture list");
 			fetchFahrplan();
 			return;
 		}
 		
 		cursor.moveToFirst();
 		while (!cursor.isAfterLast()) {
 			Lecture lecture = new Lecture(cursor.getString(0));
 
 			lecture.title = cursor.getString(1);
 			lecture.subtitle = cursor.getString(2);
 			lecture.day = cursor.getInt(3);
 			lecture.room = cursor.getString(4);
 			lecture.startTime = cursor.getInt(5);
 			lecture.duration = cursor.getInt(6);
 			lecture.speakers = cursor.getString(7);
 			lecture.track = cursor.getString(8);
 			lecture.type = cursor.getString(9);
 			lecture.lang = cursor.getString(10);
 			lecture.abstractt = cursor.getString(11);
 			lecture.description = cursor.getString(12);
 			lecture.relStartTime = cursor.getInt(13);
 			lecture.date = cursor.getString(14);
 			lecture.links = cursor.getString(15);
 
 			MyApp.lectureList.add(lecture);
 			cursor.moveToNext();
 		}
 		cursor.close();
 		MyApp.lectureListDay = day;
 		
 		hCursor.moveToFirst();
 		while (!hCursor.isAfterLast()) {
 			String lecture_id = hCursor.getString(1);
 			int highlighted = hCursor.getInt(2);
 			Log.d(LOG_TAG, "lecture "+lecture_id+" is hightlighted:"+highlighted);
 			
 			for (Lecture lecture : MyApp.lectureList) {
 				if (lecture.lecture_id.equals(lecture_id)) {
 					lecture.highlight = (highlighted == 1 ? true : false);
 				}
 			}
 			hCursor.moveToNext();
 		}
 		hCursor.close();
 	}
 
 	private void loadMeta() {
 		if (metadb == null) {
 			metadb = metaDB.getReadableDatabase();
 		}
 		Cursor cursor;
 		try {
 			cursor = metadb.query("meta", MetaDBOpenHelper.allcolumns, null, null,
 					null, null, null);
 		} catch (SQLiteException e) {
 			e.printStackTrace();
 			metadb.close();
 			metadb = null;
 			return;
 		}
 
 		MyApp.numdays = 0;
 		MyApp.version = "";
 		MyApp.title = "";
 		MyApp.subtitle = "";
 		if (cursor.getCount() > 0) {
 			cursor.moveToFirst();
 			if (cursor.getColumnCount() > 0)
 				MyApp.numdays = cursor.getInt(0);
 			if (cursor.getColumnCount() > 1)
 				MyApp.version = cursor.getString(1);
 			if (cursor.getColumnCount() > 2)
 				MyApp.title = cursor.getString(2);
 			if (cursor.getColumnCount() > 3)
 				MyApp.subtitle = cursor.getString(3);
 		}
 
 		Log.d(LOG_TAG, "loadMeta: numdays=" + MyApp.numdays + " version:"
 				+ MyApp.version + " " + MyApp.title);
 		cursor.close();
 	}
 
 	@Override
 	public void onClick(View v) {
 		Lecture lecture = (Lecture) v.getTag();
 		Log.d(LOG_TAG, "Click on " + lecture.title);
 		Intent intent = new Intent(this, EventDetail.class);
 		intent.putExtra("title", lecture.title);
 		intent.putExtra("subtitle", lecture.subtitle);
 		intent.putExtra("abstract", lecture.abstractt);
 		intent.putExtra("descr", lecture.description);
 		intent.putExtra("spkr", lecture.speakers.replaceAll(";", ", "));
 		intent.putExtra("links", lecture.links);
 		intent.putExtra("eventid", lecture.lecture_id);
 		intent.putExtra("time", lecture.startTime);
 		startActivity(intent);
 	}
 
 	@Override
 	public void onParseDone(Boolean result, String version) {
 		Log.d(LOG_TAG, "parseDone: " + result);
 		MyApp.task_running = TASKS.NONE;
 		MyApp.fahrplan_xml = null;
 		boolean refreshDisplay = false;
 		if (result) {
 			if ((MyApp.numdays == 0) && (!version.equals(MyApp.version))) {
 				refreshDisplay = true;
 			}
 		} else {
 			// FIXME Fehlermeldung;
 		}
 		setProgressBarIndeterminateVisibility(false);
 		if (MyApp.numdays == 0) {
 			progress.dismiss();
 		} else {
 			progressSpinner.setVisibility(View.GONE);
 			statusBar.startAnimation(slideDownOut);
 			statusBar.setVisibility(View.GONE);
 			refreshBtn.setVisibility(View.VISIBLE);
 		}
 		
 		if (refreshDisplay) {
 			loadMeta();
 			SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
 			day = prefs.getInt("displayDay", 1);
 			if (day > MyApp.numdays) {
 				day = 1;
 			}
 			viewDay(true);
 			final Toast done = Toast.makeText(global
 					.getApplicationContext(), String.format(
 					getString(R.string.aktualisiert_auf), version),
 					Toast.LENGTH_LONG);
 			done.show();
 		}
 	}
 
 	void aboutDialog() {
 		LayoutInflater inflater = (LayoutInflater) getApplicationContext()
 				.getSystemService(LAYOUT_INFLATER_SERVICE);
 		View layout = inflater.inflate(R.layout.about_dialog,
 				(ViewGroup) findViewById(R.id.layout_root));
 
 		TextView text = (TextView) layout.findViewById(R.id.eventVersion);
 		text.setText(getString(R.string.fahrplan) + " " + MyApp.version);
 		text = (TextView) layout.findViewById(R.id.eventTitle);
 		text.setText(MyApp.title);
 		Log.d(LOG_TAG, "title:" + MyApp.title);
 		text = (TextView) layout.findViewById(R.id.eventSubtitle);
 		text.setText(MyApp.subtitle);
 		text = (TextView) layout.findViewById(R.id.appVersion);
 		try {
 			text
 					.setText(getString(R.string.appVersion)
 							+ " "
 							+ getApplicationContext().getPackageManager()
									.getPackageInfo("nerd.tuxmobil.fahrplan.camp11", 0).versionName);
 		} catch (NameNotFoundException e) {
 			e.printStackTrace();
 			text.setText("");
 		}
 
 		new AlertDialog.Builder(this).setTitle(getString(R.string.app_name))
 				.setView(layout).setPositiveButton(android.R.string.ok,
 						new DialogInterface.OnClickListener() {
 							public void onClick(DialogInterface dialog,
 									int which) {
 							}
 						}).create().show();
 	}
 
 	void getAlarmTimeDialog(final View v) {
 
 		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
 		View layout = inflater.inflate(R.layout.reminder_dialog,
 				(ViewGroup) findViewById(R.id.layout_root));
 
 		final Spinner spinner = (Spinner) layout.findViewById(R.id.spinner);
 		ArrayAdapter<CharSequence> adapter = ArrayAdapter
 				.createFromResource(this, R.array.alarm_array,
 						android.R.layout.simple_spinner_item);
 		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		spinner.setAdapter(adapter);
 		
 		TextView msg = (TextView)layout.findViewById(R.id.message);
 		msg.setText(R.string.choose_alarm_time);
 		
 		new AlertDialog.Builder(this).setTitle(R.string.setup_alarm).setView(layout)
 				.setPositiveButton(android.R.string.ok,
 						new DialogInterface.OnClickListener() {
 							public void onClick(DialogInterface dialog,
 									int which) {
 								int alarm = spinner.getSelectedItemPosition();
 								Log.d(LOG_TAG, "alarm chosen: "+alarm);
 								addAlarm(v, alarm);
 							}
 						}).setNegativeButton(android.R.string.cancel, null)
 				.create().show();
 	}
 
 	private int[] alarm_times = { 0, 5, 10, 15, 30, 45, 60 };
 	private View contextMenuView;
 	
 	public void addAlarm(View v, int alarmTime) {
 		Lecture lecture = (Lecture) v.getTag();
 		Time time = lecture.getTime();
 		long startTime = time.normalize(true);
 		long when = time.normalize(true) - (alarm_times[alarmTime] * 60 * 1000);
 		
 		// DEBUG
 		// when = System.currentTimeMillis() + (30 * 1000);
 		
 		time.set(when);
 		Log.d(LOG_TAG, "Alarm time: "+when);
 		
 		
 		Intent intent = new Intent(this, AlarmReceiver.class);
 		intent.putExtra("lecture_id", lecture.lecture_id);
 		intent.putExtra("day", lecture.day);
 		intent.putExtra("title", lecture.title);
 		intent.putExtra("startTime", startTime);
 		
 		intent.setAction("de.machtnix.fahrplan.ALARM");
 		intent.setData(Uri.parse("alarm://"+lecture.lecture_id));
 		
 		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
 		PendingIntent pendingintent = PendingIntent.getBroadcast(this, Integer.parseInt(lecture.lecture_id), intent, 0);
 		
 		// Cancel any existing alarms for this lecture
 		alarmManager.cancel(pendingintent);
 		
 		// Set new alarm
 		alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingintent);
 		
 		// write to DB
 		
 		AlarmsDBOpenHelper alarmDB = new AlarmsDBOpenHelper(this);
 
 		SQLiteDatabase db = alarmDB.getWritableDatabase();
 		
 		// delete any previous alarms of this lecture
 		db.delete("alarms", "eventid=?", new String[] { lecture.lecture_id });
 		
 		ContentValues values = new ContentValues();
 
 		values.put("eventid", Integer.parseInt(lecture.lecture_id));
 		values.put("title", lecture.title);
 		values.put("time", when);
 		values.put("timeText", time.format("%Y-%m-%d %H:%M"));
 		values.put("displayTime", startTime);
 		values.put("day", lecture.day);
 
 		db.insert("alarms", null, values);
 		db.close();
 	}
 	
 	public void writeHighlight(Lecture lecture) {
 		HighlightDBOpenHelper highlightDB = new HighlightDBOpenHelper(this);
 
 		SQLiteDatabase db = highlightDB.getWritableDatabase();
 		
 		db.delete("highlight", "eventid=?", new String[] { lecture.lecture_id });
 		
 		ContentValues values = new ContentValues();
 
 		values.put("eventid", Integer.parseInt(lecture.lecture_id));
 		values.put("highlight", lecture.highlight ? 1 : 0);
 
 		db.insert("highlight", null, values);
 		db.close();
 	}
 	
 	public boolean onContextItemSelected(MenuItem item) {
 		int menuItemIndex = item.getItemId();
 		Lecture lecture = (Lecture)contextMenuView.getTag();
 		
 		Log.d(LOG_TAG,"clicked on "+((Lecture)contextMenuView.getTag()).lecture_id);
 		
 		switch (menuItemIndex) {
 		case 0:
 			Integer drawable;
 			int padding = getEventPadding();
 			if (lecture.highlight) {
 				drawable = trackColors.get(lecture.track);
 				lecture.highlight = false;
 				writeHighlight(lecture);
 			} else {
 				drawable = trackColorsHi.get(lecture.track);
 				lecture.highlight = true;
 				writeHighlight(lecture);
 				padding += (int)(2 * scale);
 			}
 			if (drawable != null) {
 				contextMenuView.setBackgroundResource(drawable);
 				contextMenuView.setPadding(padding, padding, padding, padding);
 			}
 			break;
 		case 1:
 			getAlarmTimeDialog(contextMenuView);
 			break;
 		}
 		return true;
 	}
 
 	public void onCreateContextMenu(ContextMenu menu, View v,
 			ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 		menu.add(0, 0, 0, getString(R.string.toggle_highlight));
 		menu.add(0, 1, 1, getString(R.string.set_alarm));
 		contextMenuView = v;
 	}
 
 }
