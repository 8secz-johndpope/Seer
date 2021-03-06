 package com.axelby.podax.ui;
 
 import android.app.NotificationManager;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Intent;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentManager;
 import android.support.v4.app.FragmentStatePagerAdapter;
 import android.support.v4.view.ViewPager;
 
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuInflater;
 import com.actionbarsherlock.view.MenuItem;
 import com.axelby.podax.BootReceiver;
 import com.axelby.podax.Constants;
 import com.axelby.podax.R;
 import com.axelby.podax.SubscriptionProvider;
 import com.axelby.podax.UpdateService;
 import com.viewpagerindicator.TitlePageIndicator;
 
 public class MainActivity extends PodaxFragmentActivity {
 
 	public static final int TAB_WELCOME = 0;
 	public static final int TAB_QUEUE = 1;
 	public static final int TAB_SUBSCRIPTIONS = 2;
 	public static final int TAB_SEARCH = 3;
 	private static final int TAB_COUNT = 4;
 
 	protected int _focusedPage;
 	protected FreezableViewPager _viewPager;
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		setContentView(R.layout.main);
 
 		// check if this was opened by android to save an RSS feed
 		Intent intent = getIntent();
 		if (intent.getDataString() != null) {
 			ContentValues values = new ContentValues();
 			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
 			Uri savedSubscription = getContentResolver().insert(SubscriptionProvider.URI, values);
 			UpdateService.updateSubscription(this, Integer.valueOf(savedSubscription.getLastPathSegment()));
 		}
 
 		// clear RSS error notification
 		String ns = Context.NOTIFICATION_SERVICE;
 		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
 		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
 
 		_viewPager = (FreezableViewPager) findViewById(R.id.pager);
 		TabsAdapter tabsAdapter = new TabsAdapter(getSupportFragmentManager());
 		_viewPager.setAdapter(tabsAdapter);
 
 		TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
 		titleIndicator.setViewPager(_viewPager);
 		titleIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
 			@Override
 			public void onPageSelected(int position) {
 				_focusedPage = position;
 				invalidateOptionsMenu();
 			}
 		});
 
 		if (intent.hasExtra(Constants.EXTRA_TAB)) {
 			_focusedPage = intent.getIntExtra(Constants.EXTRA_TAB, TAB_QUEUE);
 			titleIndicator.setCurrentItem(_focusedPage);
 		} else if (savedInstanceState != null) {
 			_focusedPage = savedInstanceState.getInt("focusedPage", 0);
 			titleIndicator.setCurrentItem(_focusedPage);
 		} else {
 			Cursor c = getContentResolver().query(SubscriptionProvider.URI, null, null, null, null);
			try {
				if (c.getCount() > 0) {
					_focusedPage = intent.getIntExtra(Constants.EXTRA_TAB, TAB_QUEUE);
					titleIndicator.setCurrentItem(_focusedPage);
				}
			} catch (Exception ex) {
			} finally {
				c.close();
 			}
 		}
 
 		BootReceiver.setupAlarms(getApplicationContext());
 	}
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putInt("focusedPage", _focusedPage);
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getSupportMenuInflater();
 		switch (_focusedPage) {
 		case TAB_QUEUE:
 			inflater.inflate(R.menu.queue_fragment, menu);
 			break;
 		case TAB_SUBSCRIPTIONS:
 			inflater.inflate(R.menu.subscriptionlist, menu);
 			break;
 		default:
 			inflater.inflate(R.menu.base, menu);
 		}
 
 		return true;
 	}
 
 	@Override
 	public boolean onMenuItemSelected(int featureId, MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.add_subscription:
 			startActivity(new Intent(this, AddSubscriptionActivity.class));
 			return true;
 		case R.id.download:
 			UpdateService.downloadPodcasts(this);
 			return true;
 		case R.id.preferences:
 			startActivity(new Intent(this, Preferences.class));
 			return true;
 		case R.id.about:
 			startActivity(new Intent(this, AboutActivity.class));
 			return true;
 		case R.id.refresh_subscriptions:
 			UpdateService.updateSubscriptions(this);
 			return true;
 		}
 		return super.onMenuItemSelected(featureId, item);
 	}
 
 	public void freezeViewPager() {
 		_viewPager.freeze();
 	}
 
 	public void unfreezeViewPager() {
 		_viewPager.unfreeze();
 	}
 
 	public class TabsAdapter extends FragmentStatePagerAdapter
 	{
 
 		private String[] _titles;
 
 		public TabsAdapter(FragmentManager fm) {
 			super(fm);
 
 			Resources resources = getResources();
 			_titles = new String[] {
 					resources.getString(R.string.welcome),
 					resources.getString(R.string.queue),
 					resources.getString(R.string.subscriptions),
 					resources.getString(R.string.search),
 			};
 		}
 
 		@Override
 		public Fragment getItem(int item) {
 			switch (item) {
 			case TAB_WELCOME:
 				return new WelcomeFragment();
 			case TAB_QUEUE:
 				return new QueueFragment();
 			case TAB_SUBSCRIPTIONS:
 				return new SubscriptionFragment();
 			case TAB_SEARCH:
 				return new SearchFragment();
 			}
 			throw new IllegalArgumentException();
 		}
 
 		@Override
 		public String getPageTitle(int position) {
 			return _titles[position].toUpperCase();
 		}
 
 		@Override
 		public int getCount() {
 			return TAB_COUNT;
 		}
 	}
 
 	public static Intent getSubscriptionIntent(Context context) {
 		Intent intent = new Intent(context, MainActivity.class);
 		intent.putExtra(Constants.EXTRA_TAB, MainActivity.TAB_SUBSCRIPTIONS);
 		return intent;
 	}
 }
