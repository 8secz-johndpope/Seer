 package com.lsg.app;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.annotation.TargetApi;
 import android.app.Activity;
 import android.app.ProgressDialog;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.database.Cursor;
 import android.database.sqlite.SQLiteDatabase;
 import android.database.sqlite.SQLiteStatement;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Messenger;
 import android.os.Parcelable;
 import android.preference.PreferenceManager;
 import android.support.v4.view.PagerAdapter;
 import android.support.v4.view.ViewPager;
 import android.text.Editable;
 import android.text.TextWatcher;
 import android.util.Log;
import android.util.TypedValue;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.webkit.WebView;
 import android.widget.CursorAdapter;
 import android.widget.EditText;
 import android.widget.LinearLayout;
 import android.widget.ListView;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.lsg.app.interfaces.SQLlist;
 import com.lsg.app.lib.SlideMenu;
 import com.lsg.app.lib.TitleCompat;
 import com.lsg.app.lib.TitleCompat.HomeCall;
 import com.lsg.app.lib.TitleCompat.RefreshCall;
 
 public class VPlan extends Activity implements HomeCall, RefreshCall, WorkerService.WorkerClass {
 	public class VPlanPagerAdapter extends PagerAdapter implements SQLlist, TextWatcher, PagerTitles {
 		private String[] where_conds = new String[4];
 		private String[] where_conds_events = new String[6];
 		private String[] exclude_subjects = new String[4];
 		private String[] titles = new String[3];
 		private final SQLiteDatabase myDB;
 		public Cursor cursor_all;
 		public Cursor cursor_mine;
 		public Cursor cursor_teachers;
 		private VPlan.VertretungAdapter vadapter_all;
 		private VPlan.VertretungAdapter vadapter_mine;
 		private VPlan.VertretungAdapter vadapter_teachers;
 		private String exclude_cond;
 		private String include_cond;
 		private VPlan act;
 		private final Context context;
 		private final SharedPreferences prefs;
 		
 		public VPlanPagerAdapter(VPlan act) {
 			where_conds[0] = "%";
 			where_conds[1] = "%";
 			where_conds[2] = "%";
 			where_conds[3] = "%";
 			where_conds_events[0] = "%";
 			where_conds_events[1] = "%";
 			where_conds_events[2] = "%";
 			where_conds_events[3] = "%";
 			where_conds_events[4] = "%";
 			where_conds_events[5] = "%";
 			titles[0] = getString(R.string.vplan_mine);
 			titles[1] = getString(R.string.vplan_pupils);
 			titles[2] = getString(R.string.vplan_teachers);
 			prefs = PreferenceManager.getDefaultSharedPreferences(act);
 			exclude_subjects[1] = (prefs.getString(Functions.GENDER, "").equals("m")) ? "Sw" : "Sm";
 			if(prefs.getString(Functions.RELIGION, "").equals(Functions.KATHOLISCH)) {
 				exclude_subjects[2] = Functions.EVANGELISCH;
 				exclude_subjects[3] = Functions.ETHIK;
 			} else if(prefs.getString(Functions.RELIGION, "").equals(Functions.EVANGELISCH)) {
 				exclude_subjects[2] = Functions.KATHOLISCH;
 				exclude_subjects[3] = Functions.ETHIK;
 			} else {
 				exclude_subjects[2] = Functions.KATHOLISCH;
 				exclude_subjects[3] = Functions.EVANGELISCH;
 			}
 			context = (Context) act;
 			this.act = act;
 			
 			myDB = context.openOrCreateDatabase(Functions.DB_NAME, Context.MODE_PRIVATE, null);
 			updateCondLists();
 			
 			SQLiteStatement num_rows = myDB.compileStatement("SELECT COUNT(*) FROM " + Functions.DB_VPLAN_TABLE);
 			long count = num_rows.simpleQueryForLong();
 			SQLiteStatement num_rows_2 = myDB.compileStatement("SELECT COUNT(*) FROM " + Functions.DB_VPLAN_TEACHER);
 			long count2 = num_rows_2.simpleQueryForLong();
 			if(count == 0 && count2 == 0)
 				act.updateVP();
 			num_rows.close();
 			vadapter_mine = new VPlan.VertretungAdapter(context, cursor_mine,
 					prefs.getBoolean(Functions.RIGHTS_TEACHER, false));
 			vadapter_all = new VPlan.VertretungAdapter(context, cursor_all, false);
 			vadapter_teachers = new VPlan.VertretungAdapter(context, cursor_teachers, true);
 			updateCursor();
 			}
 		
 		@Override
 		public int getCount() {
 			if(prefs.getBoolean(Functions.RIGHTS_TEACHER, false) || prefs.getBoolean(Functions.RIGHTS_ADMIN, false))
 				return 3;
 			else
 				return 2;
 			}
 		
 		public String getTitle(int pos) {
 			return titles[pos];
 			}
 		 @Override
 	        public CharSequence getPageTitle (int position) {
 	            return titles[position];
 	        }
 		
 		@Override
 		public Object instantiateItem(View pager, int position) {
 				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 				LinearLayout lay = (LinearLayout) inflater.inflate(R.layout.list, null);
 				ListView lv = (ListView) lay.findViewById(android.R.id.list);
 				//set header search bar
 				if(Functions.getSDK() < 11) {
 					View search = inflater.inflate(R.layout.search, null);
 					EditText searchEdit = (EditText) search.findViewById(R.id.search_edit);
 					searchEdit.addTextChangedListener(this);
 					lv.addHeaderView(search);
 					}
 				if(position == 0)
 					lv.setAdapter(vadapter_mine);
 				if(position == 1)
 					lv.setAdapter(vadapter_all);
 				if(position == 2)
 					lv.setAdapter(vadapter_teachers);
 				Functions.styleListView(lv, context);
 				act.registerForContextMenu(lv);
 				lv.setEmptyView(lay.findViewById(R.id.list_view_empty));
 				if(position == 0)
 					((TextView) lay.findViewById(R.id.list_view_empty)).setText(R.string.vplan_mine_empty);
 				if(position == 1)
 					((TextView) lay.findViewById(R.id.list_view_empty)).setText(R.string.vplan_empty);
 				if(position == 2)
 					((TextView) lay.findViewById(R.id.list_view_empty)).setText(R.string.vplan_empty);
 				((ViewPager)pager).addView(lay, 0);
 				return lay;
 				}
 		
 
 		public void updateCondLists() {
 			exclude_cond = new String();
 			Cursor exclude = myDB.query(Functions.DB_EXCLUDE_TABLE, new String[] {Functions.DB_RAW_FACH, Functions.DB_NEEDS_SYNC},
 					null, null, null, null, null);
 			exclude.moveToFirst();
 			int i = 0;
 			while(i < exclude.getCount()) {
 				String fach = exclude.getString(exclude.getColumnIndex(Functions.DB_RAW_FACH));
 				exclude_cond += " AND " + Functions.DB_RAW_FACH + " != '" + fach + "' ";
 				exclude.moveToNext();
 				i++;
 			}
 			exclude.close();
 			include_cond = new String();
 			Cursor include = myDB.query(Functions.INCLUDE_TABLE, new String[] {Functions.DB_FACH, Functions.DB_NEEDS_SYNC},
 					null, null, null, null, null);
 			include.moveToFirst();
 			i = 0;
 			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
 			String connector = "";
 			while(i < include.getCount()) {
 				String fach = include.getString(include.getColumnIndex(Functions.DB_FACH));
 				include_cond += connector + Functions.DB_FACH + " LIKE '%" + fach + "%' ";
 				connector = " OR ";
 				include.moveToNext();
 				i++;
 			}
 			include.close();
 			if(include_cond.length() == 0)
 				include_cond = " 0 ";
 			if(prefs.getBoolean("showonlywhitelist", false))
 				include_cond = "AND (" + include_cond + " ) OR ( " + include_cond + " )";
 			else
 				include_cond = " OR ( " + include_cond + " ) ";
 		}
 		public void updateCursor() {
 			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
 			String klasse = prefs.getString("full_class", "");
 			where_conds[0] =  "%" + klasse + "%";
 			String first = "( " + Functions.DB_KLASSE + " LIKE ? ";
 			String sec = "";
 			if(prefs.getBoolean("showwithoutclass", true))
 				sec = "OR " + Functions.DB_KLASSE + " LIKE 'null'";
 			sec += " OR " + Functions.DB_KLASSE + " LIKE 'infotext') AND ( " + Functions.DB_KLASSE
 					+ " LIKE ? OR " + Functions.DB_FACH + " LIKE ? OR " + Functions.DB_LEHRER + " LIKE ? )";
 			String mine_cond = first + include_cond +  sec + exclude_cond;
 			String all_cond = first + sec;
 			if (prefs.getBoolean(Functions.RIGHTS_TEACHER, false)) {
 				cursor_mine = myDB.query(Functions.DB_VPLAN_TEACHER,
 						new String[] { Functions.DB_ROWID, Functions.DB_KLASSE,
 								Functions.DB_TYPE, Functions.DB_STUNDE,
 								Functions.DB_LEHRER, Functions.DB_FACH,
 								Functions.DB_VERTRETUNGSTEXT,
 								Functions.DB_VERTRETER, Functions.DB_ROOM,
 								Functions.DB_CLASS_LEVEL, Functions.DB_DATE,
 								Functions.DB_LENGTH, "'teachers' AS type" },
 						Functions.DB_RAW_VERTRETER + "=? OR "
 								+ Functions.DB_RAW_LEHRER + "=?", new String[] {
 								prefs.getString(Functions.TEACHER_SHORT, ""),
 								prefs.getString(Functions.TEACHER_SHORT, "") },
 						null, null, null);
 			} else {
 				cursor_mine = myDB.query(Functions.DB_VPLAN_TABLE,
 						new String[] { Functions.DB_ROWID, Functions.DB_KLASSE,
 								Functions.DB_TYPE, Functions.DB_STUNDE,
 								Functions.DB_LEHRER, Functions.DB_FACH,
 								Functions.DB_VERTRETUNGSTEXT,
 								Functions.DB_VERTRETER, Functions.DB_ROOM,
 								Functions.DB_CLASS_LEVEL, Functions.DB_DATE,
 								Functions.DB_LENGTH, "'pupils' AS type" }, mine_cond, where_conds,
 						null, null, null);
 				Log.d("mine_cond", mine_cond);
 				Log.d("klasse", where_conds[0]);
 			}
 			where_conds[0] = "%";
 			cursor_all = myDB.query(Functions.DB_VPLAN_TABLE, new String[] {
 					Functions.DB_ROWID, Functions.DB_KLASSE, Functions.DB_TYPE,
 					Functions.DB_STUNDE, Functions.DB_LEHRER,
 					Functions.DB_FACH, Functions.DB_VERTRETUNGSTEXT,
 					Functions.DB_VERTRETER, Functions.DB_ROOM,
 					Functions.DB_CLASS_LEVEL, Functions.DB_DATE,
 					Functions.DB_LENGTH, "'pupils' AS type" }, all_cond, where_conds, null, null,
 					null);
 			cursor_teachers = myDB.query(Functions.DB_VPLAN_TEACHER,
 					new String[] { Functions.DB_ROWID, Functions.DB_KLASSE,
 							Functions.DB_TYPE, Functions.DB_STUNDE,
 							Functions.DB_LEHRER, Functions.DB_FACH,
 							Functions.DB_VERTRETUNGSTEXT,
 							Functions.DB_VERTRETER, Functions.DB_ROOM,
 							Functions.DB_CLASS_LEVEL, Functions.DB_DATE,
 							Functions.DB_LENGTH, "'teachers' AS type" }, all_cond, where_conds, null,
 					null, Functions.DB_ROWID + ", CASE "
 							+ Functions.DB_VERTRETER
 							+ " WHEN 'null' THEN 0 ELSE 1 END, "
 							+ Functions.DB_VERTRETER + ", "
 							+ Functions.DB_STUNDE);
 			vadapter_mine.changeCursor(cursor_mine);
 			vadapter_all.changeCursor(cursor_all);
 			vadapter_teachers.changeCursor(cursor_teachers);
 		}
 		public void updateWhereCond(String searchText) {
 			where_conds[1] = "%" + searchText + "%";
 			where_conds[2] = "%" + searchText + "%";
 			where_conds[3] = "%" + searchText + "%";
 			
 			where_conds_events[0] = "%" + searchText + "%";
 			where_conds_events[1] = "%" + searchText + "%";
 			where_conds_events[2] = "%" + searchText + "%";
 			where_conds_events[3] = "%" + searchText + "%";
 			where_conds_events[4] = "%" + searchText + "%";
 			where_conds_events[5] = "%" + searchText + "%";
 			updateCursor();
 		}
 		public void updateList() {
 			updateCondLists();
 			updateCursor();
 		}
 		public void afterTextChanged (Editable s) { }
 		public void beforeTextChanged (CharSequence s, int start, int count, int after) { }
 		public void onTextChanged (CharSequence s, int start, int before, int count) {
 			String search = s + "";
 			updateWhereCond(search);
 		}
 		public void closeCursorsDB() {
 			cursor_mine.close();
 			cursor_all.close();
 			myDB.close();
 		}
 		@Override
 		public void destroyItem( View pager, int position, Object view ) {
 			((ViewPager)pager).removeView((View) view);
 			}
 		
 		@Override
 		public boolean isViewFromObject( View view, Object object ) {
 			return view.equals( object );
 			}
 		
 		@Override
 		public void finishUpdate( View view ) {}
 		
 		@Override
 		public void restoreState( Parcelable p, ClassLoader c ) {}
 		
 		@Override
 		public Parcelable saveState() {
 			return null;
 			}
 		@Override
 		public void startUpdate( View view ) {}
 	}
 	public class VertretungAdapter extends CursorAdapter {
 		private boolean teacher;
 		class Standard {
 			public LinearLayout standard;
 			public TextView date;
 			public TextView klasse;
 			public TextView title;
 			public TextView type;
 			public TextView when;
 			public TextView vtext;
 			public TextView bottom;
 			public WebView webv;
 			}
 		public VertretungAdapter(Context context, Cursor c, boolean teacher) {
 			super(context, c, false);
 			this.teacher = teacher;
 			}
 		@Override
 		public View newView(Context context, Cursor cursor, ViewGroup parent) {
 			LayoutInflater inflater =  (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 			View rowView;
 			if(teacher)
 				rowView = inflater.inflate(R.layout.vplan_listitem, null, true);
 			else
 				rowView = inflater.inflate(R.layout.vplan_listitem, null, true);
 			Standard holder = new Standard();
 			holder.standard = (LinearLayout) rowView.findViewById(R.id.standard_rellayout);
 			holder.date = (TextView) rowView.findViewById(R.id.vertretung_date);
 			holder.klasse = (TextView) rowView.findViewById(R.id.vertretung_class);
 			holder.title = (TextView) rowView.findViewById(R.id.vertretung_title);
 			holder.type = (TextView) rowView.findViewById(R.id.vertretung_type);
 			holder.when = (TextView) rowView.findViewById(R.id.vertretung_when);
 			holder.vtext = (TextView) rowView.findViewById(R.id.vertretung_text);
 			holder.bottom = (TextView) rowView.findViewById(R.id.vertretung_bottom);
 			holder.webv = (WebView) rowView.findViewById(R.id.standard_webview);
 			if(Functions.getSDK() < 11)
 				holder.klasse.setBackgroundResource(R.layout.divider_gradient);
 			rowView.setTag(holder);
 			return rowView;
 			}
 		@Override
 		public void bindView(View view, Context context, Cursor cursor) {
 			Standard holder = (Standard) view.getTag();
 			
 			String olddate  = "";
 			String oldclass = "";
 			String oldvertreter = "";
 			int position = cursor.getPosition();
 			if(position > 0) {
 				cursor.moveToPosition(position-1);
 				olddate  = cursor.getString(cursor.getColumnIndex(Functions.DB_DATE));
 				oldclass = cursor.getString(cursor.getColumnIndex(Functions.DB_CLASS_LEVEL));
 				oldvertreter = cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETER));
 				cursor.moveToPosition(position);
 				}
 			
 			String date = cursor.getString(cursor.getColumnIndex(Functions.DB_DATE));
 			if(date.equals(olddate))
 				holder.date.setVisibility(View.GONE);
 			else {
 				holder.date.setVisibility(View.VISIBLE);
 				holder.date.setText(date);
 				oldclass = "";
 				}
 			
 			String klassenstufe = cursor.getString(cursor.getColumnIndex(Functions.DB_CLASS_LEVEL));
 			String klasse = cursor.getString(cursor.getColumnIndex(Functions.DB_KLASSE));
 			if(!klasse.equals("infotext")) {
 				//hide
 				holder.webv.setVisibility(View.GONE);
 				//show needed views
 				holder.title.setVisibility(View.VISIBLE);
 				holder.type.setVisibility(View.VISIBLE);
 				holder.when.setVisibility(View.VISIBLE);
 				holder.bottom.setVisibility(View.VISIBLE);
 
 				if (klassenstufe.equals(oldclass)
						&& (((cursor.getString(cursor.getColumnIndex("type"))
 								.equals("teachers") && cursor.getString(
								cursor.getColumnIndex(Functions.DB_VERTRETER)).equals(oldvertreter)) || cursor.getString(cursor.getColumnIndex("type")).equals("pupils")))
								)
 					holder.klasse.setVisibility(View.GONE);
 				else {
 					holder.klasse.setVisibility(View.VISIBLE);
 					}
 				if(Integer.valueOf(klassenstufe) < 14)
 					holder.klasse.setText(klassenstufe + ". " + context.getString(R.string.classes));
 				else if(cursor.getString(cursor.getColumnIndex("type")).equals("teachers"))
 					holder.klasse.setText(getString(R.string.vplan_of) + " " + cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETER)));
 				else
 					holder.klasse.setText(context.getString(R.string.no_classes));

 				String type = cursor.getString(cursor.getColumnIndex(Functions.DB_TYPE));
 				holder.type.setText(type);

				holder.type.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				holder.title.setVisibility(View.VISIBLE);
				String fach = cursor.getString(cursor.getColumnIndex(Functions.DB_FACH));
				if(fach.equals("null") && klasse.equals("null")) {
					holder.type.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
					holder.title.setVisibility(View.GONE);
				}
				else if (klasse.equals("null"))
					holder.title.setText(fach);
//				else if(fach.equals("null"))
//					holder.title.setText(klasse);	//hypothetic
				else
					holder.title.setText(klasse + " (" + fach + ")");
				
 				Integer lesson;
 				try {
 				lesson = Integer.valueOf(cursor.getString(cursor.getColumnIndex(Functions.DB_STUNDE)));
 				} catch(Exception e) {
 					//old db style, do act!!!
 					lesson = 0;
 					VPlan.this.updateVP();
 				}
 				String when = lesson.toString();
 				int i = 0;
 				int length = cursor.getInt(cursor.getColumnIndex(Functions.DB_LENGTH));
 				while(i < length) {
 					lesson++;
 					when += ", " + lesson.toString();
 					i++;
 					}
 				when += ".";
 				holder.when.setText(when + context.getString(R.string.hour));
 				String vtext = cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETUNGSTEXT));
 				if(vtext.equals("null"))
 					holder.vtext.setVisibility(View.GONE);
 				else {
 					holder.vtext.setVisibility(View.VISIBLE);
 					holder.vtext.setText("[" + vtext + "]");
 					}
 				String lehrer    = cursor.getString(cursor.getColumnIndex(Functions.DB_LEHRER));
 				if(cursor.getString(cursor.getColumnIndex(Functions.DB_TYPE)).equals("Entfall")){
 					holder.bottom.setText(context.getString(R.string.at) + " " + lehrer);
 					} else {
 						String vertreter = cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETER));
 						String raum = cursor.getString(cursor.getColumnIndex(Functions.DB_ROOM));
 						String raumInsert = "";
 						if(!raum.equals("null"))
 							raumInsert = '\n' + context.getString(R.string.room) + " " + raum;
 						holder.bottom.setText(lehrer + " → " + vertreter + raumInsert);
 						}
 				} else {
 					holder.klasse.setText(context.getString(R.string.info));
 					//hide views not needed
 					holder.title.setVisibility(View.GONE);
 					holder.type.setVisibility(View.GONE);
 					holder.when.setVisibility(View.GONE);
 					holder.bottom.setVisibility(View.GONE);
 					holder.vtext.setVisibility(View.GONE);
 					
 					//unhide needed views that could be hidden
 					holder.klasse.setVisibility(View.VISIBLE);
 					holder.webv.setVisibility(View.VISIBLE);
 					String info = cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETUNGSTEXT));
 					holder.webv.loadData(info, "text/html", null);
 					//holder.vtext.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(Functions.DB_VERTRETUNGSTEXT))));
 					}
 			}
 		}
 	public static class VPlanUpdater {
 		Context context;
 		VPlanUpdater(Context c) {
 			context = c;
 		}
 		public String[] updatePupils() {
 			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
 			String add = "";
 			try {
 				add = "&" + URLEncoder.encode("date", "UTF-8") + "=" + URLEncoder.encode(prefs.getString("vplan_date", ""), "UTF-8")
 						+ "&" + URLEncoder.encode("time", "UTF-8") + "=" + URLEncoder.encode(prefs.getString("vplan_time", ""), "UTF-8");
 				} catch(UnsupportedEncodingException e) { Log.w("encoding", e.getMessage()); }
 			String get = Functions.getData(Functions.VP_URL, context, true, add);
 			if(!get.equals("networkerror") && !get.equals("loginerror") && !get.equals("noact")) {
 				try {
 					JSONArray jArray = new JSONArray(get);
 					int i = 0;
 					SQLiteDatabase myDB = context.openOrCreateDatabase(Functions.DB_NAME, Context.MODE_PRIVATE, null);
 					myDB.delete(Functions.DB_VPLAN_TABLE, null, null); //clear vertretungen
 					while(i < jArray.length() - 1) {
 						JSONObject jObject = jArray.getJSONObject(i);
 						ContentValues values = new ContentValues();
 						values.put(Functions.DB_CLASS_LEVEL, jObject.getString("klassenstufe"));
 						values.put(Functions.DB_KLASSE, jObject.getString("klasse"));
 						values.put(Functions.DB_STUNDE, jObject.getString("stunde"));
 						values.put(Functions.DB_VERTRETER, jObject.getString("vertreter"));
 						values.put(Functions.DB_RAW_VERTRETER, jObject.getString("rawvertreter"));
 						values.put(Functions.DB_LEHRER, jObject.getString("lehrer"));
 						values.put(Functions.DB_RAW_LEHRER, jObject.getString("rawlehrer"));
 						values.put(Functions.DB_ROOM, jObject.getString("raum"));
 						values.put(Functions.DB_TYPE, jObject.getString("art"));
 						values.put(Functions.DB_VERTRETUNGSTEXT, jObject.getString("vertretungstext"));
 						values.put(Functions.DB_FACH, jObject.getString("fach"));
 						values.put(Functions.DB_RAW_FACH, jObject.getString("rawfach"));
 						values.put(Functions.DB_DATE, jObject.getString("date"));
 						values.put(Functions.DB_LENGTH,
 								jObject.getInt("length"));
 						values.put(Functions.DB_DAY_OF_WEEK,
 								jObject.getInt("dayofweek"));
 						myDB.insert(Functions.DB_VPLAN_TABLE,
 								null, values);
 						i++;
 					}
 					myDB.close();
 					JSONObject jObject = jArray.getJSONObject(i);
 					String date = jObject.getString("date");
 					String time = jObject.getString("time");
 					SharedPreferences.Editor edit = prefs.edit();
 					edit.putString("vplan_date", date);
 					edit.putString("vplan_time", time);
 					edit.commit();
 					Functions.cleanVPlanTable(context, Functions.DB_VPLAN_TABLE);
 					} catch(JSONException e) {
 						Log.w("jsonerror", e.getMessage());
 						return new String[] {"json", context.getString(R.string.jsonerror)};
 					}
 				}
 			else if(get.equals("noact"))
 				return new String[] {"noact", context.getString(R.string.noact)};
 			else if(get.equals("loginerror"))
 				return new String[] {"loginerror", context.getString(R.string.loginerror)};
 			else if(get.equals("networkerror"))
 				return new String[] {"networkerror", context.getString(R.string.networkerror)};
 			else
 				return new String[] {"unknownerror", context.getString(R.string.unknownerror)};
 			return new String[] {"success", " "};
 			}
 		public String[] updateTeachers() {
 				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
 				String add = "";
 				try {
 					add = "&" + URLEncoder.encode("date", "UTF-8") + "=" + URLEncoder.encode(prefs.getString("vplan_teacher_date", ""), "UTF-8")
 							+ "&" + URLEncoder.encode("time", "UTF-8") + "=" + URLEncoder.encode(prefs.getString("vplan_teacher_time", ""), "UTF-8")
 							+ "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("teachers", "UTF-8");
 					} catch(UnsupportedEncodingException e) { Log.w("encoding", e.getMessage()); }
 				String get = Functions.getData(Functions.VP_URL, context, true, add);
 				if(!get.equals("networkerror") && !get.equals("loginerror") && !get.equals("noact") && !get.equals("rights")) {
 					try {
 						JSONArray jArray = new JSONArray(get);
 						int i = 0;
 						SQLiteDatabase myDB = context.openOrCreateDatabase(Functions.DB_NAME, Context.MODE_PRIVATE, null);
 						myDB.delete(Functions.DB_VPLAN_TEACHER, null, null); //clear vertretungen
 						while(i < jArray.length() - 1) {
 							JSONObject jObject = jArray.getJSONObject(i);
 							ContentValues values = new ContentValues();
 							values.put(Functions.DB_CLASS_LEVEL, jObject.getString("klassenstufe"));
 							values.put(Functions.DB_KLASSE, jObject.getString("klasse"));
 							values.put(Functions.DB_STUNDE, jObject.getString("stunde"));
 							values.put(Functions.DB_VERTRETER, jObject.getString("vertreter"));
 							values.put(Functions.DB_RAW_VERTRETER, jObject.getString("rawvertreter"));
 							values.put(Functions.DB_LEHRER, jObject.getString("lehrer"));
 							values.put(Functions.DB_RAW_LEHRER, jObject.getString("rawlehrer"));
 							values.put(Functions.DB_ROOM, jObject.getString("raum"));
 							values.put(Functions.DB_TYPE, jObject.getString("art"));
 							values.put(Functions.DB_VERTRETUNGSTEXT, jObject.getString("vertretungstext"));
 							values.put(Functions.DB_FACH, jObject.getString("fach"));
 							values.put(Functions.DB_RAW_FACH, jObject.getString("rawfach"));
 							values.put(Functions.DB_DATE, jObject.getString("date"));
 							values.put(Functions.DB_LENGTH, jObject.getInt("length"));
 							myDB.insert(Functions.DB_VPLAN_TEACHER, null, values);
 							i++;
 							}
 						myDB.close();
 						JSONObject jObject            = jArray.getJSONObject(i);
 						String date                   = jObject.getString("date");
 						String time                   = jObject.getString("time");
 						SharedPreferences.Editor edit = prefs.edit();
 						edit.putString("vplan_teacher_date", date);
 						edit.putString("vplan_teacher_time", time);
 						edit.commit();
 						} catch(JSONException e) {
 							Log.w("jsonerror", e.getMessage());
 							return new String[] {"json", context.getString(R.string.jsonerror)};
 						}
 					}
 				else if(get.equals("noact"))
 					return new String[] {"noact", context.getString(R.string.noact)};
 				else if(get.equals("loginerror"))
 					return new String[] {"loginerror", context.getString(R.string.loginerror)};
 				else if(get.equals("networkerror"))
 					return new String[] {"networkerror", context.getString(R.string.networkerror)};
 				else
 					return new String[] {"unknownerror", context.getString(R.string.unknownerror)};
 				return new String[] {"success", " "};
 				}
 		}
 	
 	private VPlanPagerAdapter adapter;
 	private ExtendedViewPager pager;
 	private SharedPreferences prefs;
 	private ProgressDialog loading;
 	private SlideMenu slidemenu;
 	private TitleCompat titlebar;
 	public void onCreate(Bundle savedInstanceState) {
 		titlebar = new TitleCompat(this, true);
 		super.onCreate(savedInstanceState);
 		Functions.setTheme(false, true, this);
         getWindow().setBackgroundDrawableResource(R.layout.background);
 		setContentView(R.layout.viewpager);
 	    adapter = new VPlanPagerAdapter(this);
 	    pager = (ExtendedViewPager)findViewById(R.id.viewpager);
 	    //pager.setOnPageChangeListener(this);
 	    pager.setAdapter(adapter);
 		pager.setPageMargin(Functions.dpToPx(40, this));
 		pager.setPageMarginDrawable(R.layout.viewpager_margin);
 		
 	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
 	    slidemenu = new SlideMenu(this, VPlan.class);
 	    slidemenu.checkEnabled();
 	    titlebar.init(this);
 	    titlebar.addRefresh(this);
 	    titlebar.setTitle(getTitle());
 	    }
 	
 	private MenuItem refresh;
 	private boolean refreshing;
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getMenuInflater();
 	    inflater.inflate(R.menu.vplan, menu);
 	    if(Functions.getSDK() >= 11) {
 	    	AdvancedWrapper ahelp = new AdvancedWrapper();
 			ahelp.searchBar(menu, adapter);
 			refresh = menu.findItem(R.id.search);
 		} else {
 			menu.removeItem(R.id.search);
 			menu.removeItem(R.id.refresh);
 		}
 	    return true;
 	}
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 	    // Handle item selection
 	    switch (item.getItemId()) {
 	    case R.id.refresh:
 	    	onRefreshPress();
 	    	return true;
 	    case R.id.subjects:
 	    	Toast.makeText(this, getString(R.string.subjectlist_info), Toast.LENGTH_LONG).show();
             Intent subjects = new Intent(this, SubjectList.class);
             startActivity(subjects);
 	    	return true;
 	    case R.id.info:
 	    	Intent intent = new Intent(this, InfoActivity.class);
 	    	intent.putExtra("type", "vplan");
 	    	intent.putExtra("vplan_num", Integer.valueOf(adapter.cursor_all.getCount()).toString());
 	    	intent.putExtra("mine_num", Integer.valueOf(adapter.cursor_mine.getCount()).toString());
 	    	intent.putExtra("date", prefs.getString("vplan_date", "") + " / " + prefs.getString("vplan_time", ""));
 	    	intent.putExtra("vplan_num_teachers", Integer.valueOf(adapter.cursor_teachers.getCount()).toString());
 	    	intent.putExtra("date_teachers", prefs.getString("vplan_teacher_date", "") + " / " + prefs.getString("vplan_teacher_time", ""));
 	    	intent.putExtra("teacher", (prefs.getBoolean(Functions.RIGHTS_TEACHER, false) || prefs.getBoolean(Functions.RIGHTS_ADMIN, false)));
 	    	startActivity(intent);
 	    	return true;
         case android.R.id.home:
             onHomePress();
             return true;
 	    default:
 	        return super.onOptionsItemSelected(item);
 	    }
 	}
 	@Override
 	public void onSaveInstanceState(Bundle savedInstanceState) {
 	  super.onSaveInstanceState(savedInstanceState);
 	  savedInstanceState.putBoolean("refreshing", refreshing);
 	}
 	@Override
 	public void onRestoreInstanceState(Bundle savedInstanceState) {
 		refreshing = savedInstanceState.getBoolean("refreshing");
 	}
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 		String table = (pager.getCurrentItem() != 2) ? Functions.DB_VPLAN_TABLE : Functions.DB_VPLAN_TEACHER;
 		Functions.createContextMenu(menu, v, menuInfo, this, table);
 	}
 	@Override
 	public boolean onContextItemSelected(final MenuItem item) {
 		String table = (pager.getCurrentItem() != 2) ? Functions.DB_VPLAN_TABLE : Functions.DB_VPLAN_TEACHER;
 		return Functions.contextMenuSelect(item, this, adapter, table);
 	}
 	public static void blacklistVPlan(Context context) {
 		SQLiteDatabase myDB = context.openOrCreateDatabase(Functions.DB_NAME,
 				Context.MODE_PRIVATE, null);
 		Cursor vplan = myDB.query(Functions.DB_VPLAN_TABLE, new String[] { Functions.DB_ROWID,
 				Functions.DB_RAW_FACH }, null, null, null, null, null);
 		vplan.moveToFirst();
 		ContentValues vals = new ContentValues();
 		vals.put(Functions.DB_DISABLED, 2);
 		myDB.update(Functions.DB_VPLAN_TABLE, vals, null, null);
 		if(vplan.getCount() > 0)
 		do {
 				Cursor exclude = myDB
 						.query(Functions.DB_EXCLUDE_TABLE,
 								new String[] { Functions.DB_ROWID },
 								Functions.DB_RAW_FACH + "=? AND "
 										+ Functions.DB_TYPE + "=?",
 								new String[] {
 										vplan.getString(vplan
 												.getColumnIndex(Functions.DB_RAW_FACH)),
 										"oldstyle" }, null, null, null);
 				if (exclude.getCount() > 0) {
 					myDB.execSQL(
 							"UPDATE " + Functions.DB_VPLAN_TABLE + " SET "
 									+ Functions.DB_DISABLED + "=? WHERE "
 									+ Functions.DB_ROWID + "=?",
 							new String[] {
 									"1",
 									vplan.getString(vplan
 											.getColumnIndex(Functions.DB_ROWID)) });
 			}
 			exclude.close();
 		} while (vplan.moveToNext());
 		vplan.close();
 		myDB.close();
 	}
 	private static ServiceHandler hand;
 	@TargetApi(11)
 	public void updateVP() {
 		refreshing = true;
 		final View actionView;
 		View v;
 		if (Functions.getSDK() >= 11) {
 			try {
 				v = refresh.getActionView();
 				refresh.setActionView(new ProgressBar(this));
 			} catch (NullPointerException e) {
 				loading = ProgressDialog.show(this, null, getString(R.string.loading_vplan));
 				v = null;
 			}
 			actionView = v;
 		} else {
 			actionView = null;
 			loading = ProgressDialog.show(this, null,
 					"Lade...");
 		}
 		hand = new ServiceHandler(new ServiceHandler.ServiceHandlerCallback() {
 			@Override
 			public void onServiceError() {
 				// TODO Auto-generated method stub
 			}
 
 			@Override
 			public void onFinishedService() {
 				if (Functions.getSDK() >= 11 && actionView != null)
 					refresh.setActionView(actionView);
 				else
 					loading.cancel();
 				refreshing = false;
 			}
 		});
 		Handler handler = hand.getHandler();
 		
 		Intent intent = new Intent(this, WorkerService.class);
 	    // Create a new Messenger for the communication back
 	    Messenger messenger = new Messenger(handler);
 	    intent.putExtra(WorkerService.MESSENGER, messenger);
 	    intent.putExtra(WorkerService.WORKER_CLASS, VPlan.class.getCanonicalName());
 	    intent.putExtra(WorkerService.WHAT, WorkerService.UPDATE_ALL);
 	    startService(intent);
 	}
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 		adapter.closeCursorsDB();
 	}
 	@Override
 	public void onHomePress() {
 		slidemenu.show();
 	}
 	@Override
 	public void onRefreshPress() {
 		updateVP();
 	}
 	public void update(int what, Context c) {
 		VPlanUpdater udp = new VPlanUpdater(c);
 		switch(what) {
 		case WorkerService.UPDATE_ALL:
 			udp.updatePupils();
 			udp.updateTeachers();
 			break;
 		case WorkerService.UPDATE_PUPILS:
 			udp.updatePupils();
 			break;
 		case WorkerService.UPDATE_TEACHERS:
 			udp.updateTeachers();
 			break;
 		}
 	}
 }
