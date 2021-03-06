 /**
  * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
  *
  * This file is part of RxDroid.
  *
  * RxDroid is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * RxDroid is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
  *
  *
  */
 
 package at.caspase.rxdroid;
 
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Date;
 import java.util.List;
 
 import android.app.Activity;
 import android.app.DatePickerDialog;
 import android.app.DatePickerDialog.OnDateSetListener;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.support.v4.view.ViewPager;
 import android.support.v4.view.ViewPager.OnPageChangeListener;
 import android.text.SpannableString;
 import android.text.format.DateFormat;
 import android.text.style.UnderlineSpan;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MenuItem.OnMenuItemClickListener;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.View.OnLongClickListener;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.DatePicker;
 import android.widget.ImageView;
 import android.widget.ListView;
 import android.widget.TextView;
 import at.caspase.rxdroid.InfiniteViewPagerAdapter.ViewFactory;
 import at.caspase.rxdroid.db.Database;
 import at.caspase.rxdroid.db.Drug;
 import at.caspase.rxdroid.db.Entry;
 import at.caspase.rxdroid.db.Intake;
 import at.caspase.rxdroid.util.CollectionUtils;
 import at.caspase.rxdroid.util.Constants;
 import at.caspase.rxdroid.util.DateTime;
 import at.caspase.rxdroid.util.Timer;
 
 public class DrugListActivity extends Activity implements OnLongClickListener,
 		OnDateSetListener, OnSharedPreferenceChangeListener, ViewFactory
 {
 	private static final String TAG = DrugListActivity.class.getName();
 	private static final boolean LOGV = true;
 
 	public static final int MENU_ADD = 0;
 	public static final int MENU_PREFERENCES = 1;
 	public static final int MENU_TOGGLE_FILTERING = 2;
 
 	public static final int CMENU_TOGGLE_INTAKE = 0;
 	// public static final int CMENU_CHANGE_DOSE = 1;
 	public static final int CMENU_EDIT_DRUG = 2;
 	// public static final int CMENU_SHOW_SUPPLY_STATUS = 3;
 	public static final int CMENU_IGNORE_DOSE = 4;
 
 	public static final String EXTRA_DATE = "date";
 	public static final String EXTRA_STARTED_FROM_NOTIFICATION = "started_from_notification";
 
 	private static final int TAG_ID = R.id.tag_drug_id;
 
 	private LayoutInflater mInflater;
 	private SharedPreferences mSharedPreferences;
 
 	private ViewPager mPager;
 	private TextView mMessageOverlay;
 	private TextView mTextDate;
 
 	private Date mDate;
 
 	private boolean mShowingAll = false;
 
 	private int mSwipeDirection = 0;
 
 	private boolean mIsShowing = false;
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState)
 	{
 		super.onCreate(savedInstanceState);
 
 		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		setContentView(R.layout.drug_list);
 
 		mInflater = LayoutInflater.from(this);
 
 		mPager = (ViewPager) findViewById(R.id.drug_list_pager);
 		mMessageOverlay = (TextView) findViewById(android.R.id.empty);
 		mTextDate = (TextView) findViewById(R.id.text_date);
 
 		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
 
 		GlobalContext.set(getApplicationContext());
 		Database.init(); // must be called before mViewSwitcher.setFactory!
 
 		mTextDate.setOnLongClickListener(this);
 		mTextDate.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v)
 			{
 				setDate(DateTime.todayDate(), true);
 			}
 		});
 
 		mPager.setOnPageChangeListener(mPageListener);
 
 
 		Intent intent = getIntent();
 		if(intent != null)
 			mDate = (Date) intent.getSerializableExtra(EXTRA_DATE);
 
 		if(mDate == null)
 			mDate = DateTime.todayDate();
 
 		Database.registerOnChangedListener(mDatabaseListener);
 	}
 
 	@Override
 	protected void onResume()
 	{
 		super.onResume();
 
 		mIsShowing = true;
 
 		final boolean wasStartedFromNotification;
 
 		Intent intent = getIntent();
 		if(intent != null)
 			wasStartedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
 		else
 			wasStartedFromNotification = false;
 
 		if(wasStartedFromNotification)
 		{
 			//int snoozeType = Settings.instance().getListPreferenceValueIndex("snooze_type", -1);
 			//if(snoozeType == NotificationReceiver.ALARM_MODE_SNOOZE)
 			//{
 			//	NotificationService.snooze(this);
 			//	Toast.makeText(this, R.string._toast_snoozing, Toast.LENGTH_SHORT).show();
 			//}
 		}
 
 		startNotificationService();
 
 		setDate(mDate, true);
 	}
 
 	@Override
 	protected void onPause()
 	{
 		super.onPause();
 		mIsShowing = false;
 		// TODO this is an ugly hack, required for now to prevent
 		// FCs that occurs when adding/updating/deleting a drug
 		// in DrugEditActivity (due to DoseView's event handlers
 		// being called)
 		mPager.removeAllViews();
 	}
 
 	@Override
 	protected void onDestroy()
 	{
 		super.onDestroy();
 		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
 		Database.unregisterOnChangedListener(mDatabaseListener);
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu)
 	{
 		menu.add(0, MENU_ADD, 0, R.string._title_add).setIcon(android.R.drawable.ic_menu_add);
 		menu.add(0, MENU_PREFERENCES, 0, R.string._title_preferences).setIcon(android.R.drawable.ic_menu_preferences);
 		menu.add(0, MENU_TOGGLE_FILTERING, 0, R.string._title_toggle_filtering).setIcon(android.R.drawable.ic_menu_view);
 
 		if(!Version.SDK_IS_PRE_HONEYCOMB)
 		{
 			menu.getItem(MENU_ADD).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
 			menu.getItem(MENU_PREFERENCES).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
 			menu.getItem(MENU_TOGGLE_FILTERING).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
 		}
 
 		return super.onCreateOptionsMenu(menu);
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item)
 	{
 		switch(item.getItemId())
 		{
 			case MENU_ADD:
 			{
 				Intent intent = new Intent(Intent.ACTION_INSERT);
 				intent.setClass(this, DrugEditActivity.class);
 				startActivity(intent);
 				return true;
 			}
 			case MENU_PREFERENCES:
 			{
 				Intent intent = new Intent();
 				intent.setClass(this, PreferencesActivity.class);
 				startActivity(intent);
 				return true;
 			}
 			case MENU_TOGGLE_FILTERING:
 			{
 				mShowingAll = !mShowingAll;
 				setDate(mDate, true);
 				return true;
 			}
 		}
 		return super.onOptionsItemSelected(item);
 	}
 
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
 	{
 		final DoseView doseView = (DoseView) v;
 		final Drug drug = Drug.get(doseView.getDrugId());
 		final int doseTime = doseView.getDoseTime();
 
 		// menu.setHeaderIcon(android.R.drawable.ic_menu_agenda);
 		menu.setHeaderTitle(drug.getName());
 
 		final boolean wasDoseTaken = doseView.wasDoseTaken();
 		final int toggleIntakeMessageId;
 
 		if(wasDoseTaken)
 			toggleIntakeMessageId = R.string._title_mark_not_taken;
 		else
 			toggleIntakeMessageId = R.string._title_mark_taken;
 
 		// ////////////////////////////////////////////////
 		menu.add(0, CMENU_TOGGLE_INTAKE, 0, toggleIntakeMessageId).setOnMenuItemClickListener(new OnMenuItemClickListener() {
 				@Override
 				public boolean onMenuItemClick(MenuItem item)
 				{
 					if(!wasDoseTaken)
 						doseView.performClick();
 					else
 					{
 						Fraction dose = new Fraction();
 						for(Intake intake : Intake.findAll(drug, mDate, doseTime))
 						{
 							dose.add(intake.getDose());
 							Database.delete(intake);
 						}
 
 						drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
 						Database.update(drug);
 					}
 
 					return true;
 				}
 		});
 		// ///////////////////////////////////////////////
 
 		// menu.add(0, CMENU_CHANGE_DOSE, 0, R.string._title_change_dose);
 
 		final Intent editIntent = new Intent(this, DrugEditActivity.class);
 		editIntent.setAction(Intent.ACTION_EDIT);
 		editIntent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);
 		menu.add(0, CMENU_EDIT_DRUG, 0, R.string._title_edit_drug).setIntent(editIntent);
 		// menu.add(0, CMENU_SHOW_SUPPLY_STATUS, 0, "Show supply status");
 
 		if(!wasDoseTaken)
 		{
 			menu.add(0, CMENU_IGNORE_DOSE, 0, R.string._title_ignore_dose)
 					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
 
 						@Override
 						public boolean onMenuItemClick(MenuItem item)
 						{
 							Database.create(new Intake(drug, mDate, doseTime));
 							return true;
 						}
 					});
 		}
 	}
 
 	public void onDrugNameClick(View view)
 	{
 		Intent intent = new Intent(Intent.ACTION_EDIT);
 		intent.setClass(this, DrugEditActivity.class);
 
 		Drug drug = Drug.get((Integer) view.getTag(TAG_ID));
 		intent.putExtra(DrugEditActivity.EXTRA_DRUG, drug);
 
 		startActivityForResult(intent, 0);
 	}
 
 	@Override
 	public boolean onLongClick(View view)
 	{
 		if(view.getId() == R.id.text_date)
 		{
 			Calendar cal = Calendar.getInstance();
 			cal.setTime(mDate);
 
 			final int year = cal.get(Calendar.YEAR);
 			final int month = cal.get(Calendar.MONTH);
 			final int day = cal.get(Calendar.DAY_OF_MONTH);
 
 			DatePickerDialog dialog = new DatePickerDialog(this, this, year, month, day);
 			dialog.show();
 			return true;
 		}
 		return false;
 	}
 
 	@Override
 	public void onDateSet(DatePicker view, int year, int month, int day)
 	{
 		setDate(DateTime.date(year, month, day), true);
 	}
 
 	public void onDoseClick(final View view)
 	{
 		final DoseView v = (DoseView) view;
 		final Drug drug = Drug.get(v.getDrugId());
 		final int doseTime = v.getDoseTime();
 		final Date date = v.getDate();
 
 		if(!date.equals(mDate))
 			Log.w(TAG, "Activity date " + mDate + " differs from DoseView date " + date);
 
 
 		IntakeDialog dialog = new IntakeDialog(this, drug, doseTime, date);
 		dialog.show();
 	}
 
 	@Override
 	public void onSharedPreferenceChanged(SharedPreferences preferences, String key)
 	{
 		setDate(mDate, true);
 	}
 
 	@Override
 	public View makeView(int offset)
 	{
 		final ListView lv = new ListView(this);
 
 		final List<Drug> drugs = Database.getAll(Drug.class);
 		Collections.sort(drugs);
 
 		Calendar cal = Calendar.getInstance();
 		cal.setTime(mDate);
 
 		// mSwipeDirection is zero when we're initializing the first 3 pages
 		// of the view pager (-1, 0, +1).
 
 		if(mSwipeDirection == 0)
 			cal.add(Calendar.DAY_OF_MONTH, offset);
 		else
 			cal.add(Calendar.DAY_OF_MONTH, mSwipeDirection < 0 ? -1 : 1);
 
 		updateListAdapter(lv, cal.getTime(), drugs);
 
 		if(drugs.isEmpty())
 		{
 			mMessageOverlay.setText(getString(R.string._msg_empty_list_text, getString(R.string._title_add)));
 			mMessageOverlay.setVisibility(View.VISIBLE);
 		}
 		else if(lv.getAdapter().getCount() == 0)
 		{
 			mMessageOverlay.setText(getString(R.string._msg_no_doses_on_this_day));
 			mMessageOverlay.setVisibility(View.VISIBLE);
 		}
 		else
 			mMessageOverlay.setVisibility(View.GONE);
 
 		return lv;
 	}
 
 	private void setDate(Date date, boolean initPager)
 	{
 		if(!mIsShowing)
 		{
 			if(LOGV) Log.v(TAG, "setDate: activity is not showing; ignoring");
 			return;
 		}
 
 		mDate = date;
 		getIntent().putExtra(EXTRA_DATE, mDate);
 
 		if(initPager)
 		{
 			mSwipeDirection = 0;
 
 			mPager.removeAllViews();
 			mPager.setAdapter(new InfiniteViewPagerAdapter(this));
 			mPager.setCurrentItem(InfiniteViewPagerAdapter.CENTER, false);
 		}
 
 		updateDateString();
 	}
 
 	private void updateListAdapter(ListView listView, Date date, List<Drug> drugs)
 	{
 		if(listView == null)
 		{
 			Log.w(TAG, "updateListAdapter: listView==null");
 			return;
 		}
 
 		if(drugs == null)
 		{
 			drugs = Database.getAll(Drug.class);
 			Collections.sort(drugs);
 		}
 
 		final DrugAdapter adapter = new DrugAdapter(this, R.layout.dose_view, drugs, date);
 		adapter.setFilter(mShowingAll ? null : new DrugFilter(date));
 
 		listView.setAdapter(adapter);
 	}
 
 	private void startNotificationService()
 	{
 		Intent serviceIntent = new Intent();
 		serviceIntent.setClass(this, NotificationService.class);
 
 		startService(serviceIntent);
 	}
 
 	private ListView getCurrentListView() {
 		return (ListView) mPager.getChildAt(mPager.getCurrentItem());
 	}
 
 	private void updateDateString()
 	{
 		//final Date date = DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy);
 		final SpannableString dateString = new SpannableString(DateFormat.getDateFormat(this).format(mDate.getTime()));
 
 		if(mDate.equals(DateTime.todayDate()))
 			dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);
 
 		mTextDate.setText(dateString);
 	}
 
 	private class DrugAdapter extends ArrayAdapter<Drug>
 	{
 		//private static final String TAG = DrugAdapter.class.getName();
 
 		private ArrayList<Drug> mAllItems;
 		private ArrayList<Drug> mItems;
 		private Date mAdapterDate;
 
 		private Timer mTimer = null;
 
 		public DrugAdapter(Context context, int viewResId, List<Drug> items, Date date)
 		{
 			super(context, viewResId, items);
 
 			mAllItems = new ArrayList<Drug>(items);
 			mAdapterDate = date;
 
 			if(LOGV)
 				mTimer = new Timer();
 		}
 
 		public void setFilter(CollectionUtils.Filter<Drug> filter)
 		{
 			if(filter != null)
 				mItems = (ArrayList<Drug>) CollectionUtils.filter(mAllItems, filter);
 			else
 				mItems = mAllItems;
 
 			notifyDataSetChanged();
 		}
 
 		@Override
 		public Drug getItem(int position) {
 			return mItems.get(position);
 		}
 
 		@Override
 		public int getPosition(Drug drug) {
 			return mItems.indexOf(drug);
 		}
 
 		@Override
 		public int getCount() {
 			return mItems.size();
 		}
 
 		@Override
 		public View getView(int position, View v, ViewGroup parent)
 		{
 			if(LOGV && position == 0)
 				mTimer.reset();
 
 			final DoseViewHolder holder;
 
 			if(v == null)
 			{
 				v = mInflater.inflate(R.layout.drug_view2, null);
 
 				holder = new DoseViewHolder();
 
 				holder.name = (TextView) v.findViewById(R.id.drug_name);
 				holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
 
 				for(int i = 0; i != holder.doseViews.length; ++i)
 				{
 					final int doseViewId = Constants.DOSE_VIEW_IDS[i];
 					holder.doseViews[i] = (DoseView) v.findViewById(doseViewId);
 					registerForContextMenu(holder.doseViews[i]);
 				}
 
 				v.setTag(holder);
 			}
 			else
 				holder = (DoseViewHolder) v.getTag();
 
 			final Drug drug = getItem(position);
 			String drugName = drug.getName();
 
 			// shouldn't normally happen, unless there's a DB problem
 			if(drugName == null || drugName.length() == 0)
 				drugName = "<???>";
 
 			holder.name.setText(drugName);
 			holder.name.setTag(TAG_ID, drug.getId());
 			holder.icon.setImageResource(drug.getFormResourceId());
 
 			for(DoseView doseView : holder.doseViews)
 			{
 				if(!doseView.hasInfo(mAdapterDate, drug))
 					doseView.setInfo(mAdapterDate, drug);
 			}
 
 			if(LOGV && position == getCount() - 1)
 			{
 				final double elapsed = mTimer.elapsedSeconds();
 				final int viewCount = getCount() * 4;
 				final double timePerView = elapsed / viewCount;
 
 				Log.v(TAG + "$DrugAdapter", viewCount + " views created in " + elapsed + "s (" + timePerView + "s per view)");
 			}
 
 			return v;
 		}
 	}
 
 	private class DrugFilter implements CollectionUtils.Filter<Drug>
 	{
 		final boolean mShowDoseless = mSharedPreferences.getBoolean("show_doseless", true);
 		final boolean mShowInactive = mSharedPreferences.getBoolean("show_inactive", true);
 
 		private Date mFilterDate;
 
 		public DrugFilter(Date date) {
 			mFilterDate = date;
 		}
 
 		@Override
 		public boolean matches(Drug drug)
 		{
 			boolean result = true;
 
 			if(!mShowDoseless && mFilterDate != null)
 			{
 				if(!drug.hasDoseOnDate(mFilterDate))
 					result = false;
 			}
 
 			if(!mShowInactive && !drug.isActive())
 				result = false;
 
 			if(!result && !Intake.findAll(drug, mFilterDate, null).isEmpty())
 				result = true;
 
 			return result;
 		}
 	}
 
 	private static class DoseViewHolder
 	{
 		TextView name;
 		ImageView icon;
 		DoseView[] doseViews = new DoseView[4];
 	}
 
 	private final OnPageChangeListener mPageListener = new OnPageChangeListener() {
 
		int mPage;
		int mLastPage = -1;
 
 		@Override
 		public void onPageSelected(int page)
 		{
 			mPage = page;
			Log.d(TAG, "onPageSelected: mPage=" + mPage);
 		}
 
 		@Override
 		public void onPageScrolled(int arg0, float arg1, int arg2) {}
 
 		@Override
 		public void onPageScrollStateChanged(int state)
 		{
 			if(state == ViewPager.SCROLL_STATE_IDLE)
 			{
				Log.d(TAG, "onPageScrollStateChanged: mPage=" + mPage + ", mLastPage=" + mLastPage);
 
				mSwipeDirection = mPage != -1 ? mPage - mLastPage : 0;
				mLastPage = mPage;
 
				if(mSwipeDirection != 0)
				{
					final int shiftBy = mSwipeDirection < 0 ? -1 : 1;
					setDate(DateTime.add(mDate, Calendar.DAY_OF_MONTH, shiftBy), false);
				}
 
				updateDateString();
 			}
 
 		}
 	};
 
 	private final Database.OnChangedListener mDatabaseListener = new Database.OnChangedListener() {
 
 		@Override
 		public void onEntryUpdated(Entry entry, int flags) {}
 
 		@Override
 		public void onEntryDeleted(Entry entry, int flags)
 		{
 			if(entry instanceof Drug)
 				setDate(mDate, true);
 		}
 
 		@Override
 		public void onEntryCreated(Entry entry, int flags)
 		{
 			if(entry instanceof Drug)
 				setDate(mDate, true);
 		}
 	};
 }
