 package com.axelby.podax.ui;
 
 import android.accounts.Account;
 import android.accounts.AccountManager;
 import android.app.AlertDialog;
 import android.app.NotificationManager;
 import android.content.ContentResolver;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.res.Configuration;
 import android.database.ContentObserver;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Handler;
 import android.support.v4.app.ActionBarDrawerToggle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentTransaction;
 import android.support.v4.view.GravityCompat;
 import android.support.v4.widget.DrawerLayout;
 import android.support.v7.app.ActionBarActivity;
 import android.text.InputType;
 import android.view.LayoutInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.BaseAdapter;
 import android.widget.EditText;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.androidquery.AQuery;
 import com.axelby.gpodder.AuthenticatorActivity;
 import com.axelby.podax.BootReceiver;
 import com.axelby.podax.Constants;
 import com.axelby.podax.GPodderProvider;
 import com.axelby.podax.Helper;
 import com.axelby.podax.PlayerService;
 import com.axelby.podax.PlayerStatus;
 import com.axelby.podax.PodaxLog;
 import com.axelby.podax.PodcastProvider;
 import com.axelby.podax.R;
 import com.axelby.podax.SubscriptionProvider;
 import com.axelby.podax.UpdateService;
 
 import java.lang.ref.WeakReference;
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.List;
 
 public class MainActivity extends ActionBarActivity {
 
 	private DrawerLayout _drawerLayout;
 	private ActionBarDrawerToggle _drawerToggle;
 	private int _fragmentId;
 	List<WeakReference<Fragment>> _savedFragments = new ArrayList<WeakReference<Fragment>>();
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		// check if this was opened by android to save an RSS feed
 		Intent intent = getIntent();
 		if (intent.getDataString() != null && intent.getData().getScheme().equals("http")) {
 			ContentValues values = new ContentValues();
 			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
 			Uri savedSubscription = getContentResolver().insert(SubscriptionProvider.URI, values);
 			UpdateService.updateSubscription(this, Integer.valueOf(savedSubscription.getLastPathSegment()));
 		}
 
 		// clear RSS error notification
 		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
 		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
 
 		BootReceiver.setupAlarms(getApplicationContext());
 
 		// ui initialization
 		setContentView(R.layout.app);
         _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
         _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, R.drawable.ic_drawer,
                 R.string.open_drawer, R.string.close_drawer);
         _drawerLayout.setDrawerListener(_drawerToggle);
 
         getSupportActionBar().setDisplayHomeAsUpEnabled(true);
 		getSupportActionBar().setHomeButtonEnabled(true);
 
 		// watch active podcast for drawer
 		getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
 		updateDrawerControls();
 
 		AQuery aq = new AQuery(this);
 		_drawerLayout = (DrawerLayout) aq.find(R.id.drawer_layout).getView();
 		_drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
 
 		ListView drawer = aq.find(R.id.drawer_list).getListView();
 		PodaxDrawerAdapter _drawerAdapter = new PodaxDrawerAdapter(this);
 		drawer.setAdapter(_drawerAdapter);
 		drawer.setOnItemClickListener(new OnItemClickListener() {
 			@Override
 			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
 				_drawerLayout.closeDrawer(GravityCompat.START);
 
 				_fragmentId = position;
 
 				switch (position) {
 					case 1 : replaceFragment(WelcomeFragment.class); break;
 					case 2 : replaceFragment(PodcastDetailFragment.class); break;
 					case 3 : replaceFragment(QueueFragment.class); break;
 					case 4 : replaceFragment(SubscriptionListFragment.class); break;
 					case 5 : replaceFragment(SearchFragment.class); break;
 					case 7 : askForRSSUrl(); break;
 					case 8 : replaceFragment(ITunesPopularListFragment.class); break;
 					case 9 : handleGPodder(); break;
 					case 11: replaceFragment(PodaxPreferenceFragment.class); break;
 					case 12: replaceFragment(AboutFragment.class); break;
 					case 13: replaceFragment(LogViewerFragment.class); break;
 				}
 		}
 		});
 		aq.id(R.id.pause).clicked(new OnClickListener() {
 			@Override
 			public void onClick(View view) {
 				PlayerService.playstop(MainActivity.this);
 			}
 		});
 
 		if (intent.hasExtra(Constants.EXTRA_FRAGMENT)) {
 			_fragmentId = intent.getIntExtra("fragmentId", 2);
 			if (_fragmentId == 2) {
 				replaceFragment(PodcastDetailFragment.class);
 			} else if (_fragmentId == 4) {
 				replaceFragment(SubscriptionListFragment.class);
 			}
 		} else if (savedInstanceState == null) {
 			Cursor c = getContentResolver().query(SubscriptionProvider.URI, null, null, null, null);
 			int subscriptionCount = c.getCount();
 			c.close();
 			if (subscriptionCount == 0) {
 				replaceFragment(WelcomeFragment.class);
 				_fragmentId = 1;
 			} else {
 				replaceFragment(PodcastDetailFragment.class);
 				_fragmentId = 2;
 			}
 		} else {
 			_fragmentId = savedInstanceState.getInt("fragmentId");
 		}
 	}
 
 	private void handleGPodder() {
 		AccountManager am = AccountManager.get(this);
 		Account[] gpodder_accounts = am.getAccountsByType(Constants.GPODDER_ACCOUNT_TYPE);
 		if (gpodder_accounts == null || gpodder_accounts.length == 0)
 			startActivity(new Intent(this, AuthenticatorActivity.class));
 		else {
 			Toast.makeText(this, "Already linked to GPodder as " + gpodder_accounts[0].name, Toast.LENGTH_SHORT).show();
 			ContentResolver.requestSync(gpodder_accounts[0], GPodderProvider.AUTHORITY, new Bundle());
 		}
 	}
 
 	protected void askForRSSUrl() {
 		AlertDialog.Builder alert = new AlertDialog.Builder(this);
 		alert.setTitle("Podcast URL");
 		alert.setMessage("Type the URL of the podcast RSS");
 		final EditText input = new EditText(this);
 		//input.setText("http://blog.axelby.com/podcast.xml");
 		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
 		alert.setView(input);
 		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int which) {
 				String subscriptionUrl = input.getText().toString();
 				if (!subscriptionUrl.contains("://"))
 					subscriptionUrl = "http://" + subscriptionUrl;
 				ContentValues values = new ContentValues();
 				values.put(SubscriptionProvider.COLUMN_URL, subscriptionUrl);
 				values.put(SubscriptionProvider.COLUMN_TITLE, subscriptionUrl);
 				Uri subscriptionUri = getContentResolver().insert(SubscriptionProvider.URI, values);
 				UpdateService.updateSubscription(MainActivity.this, subscriptionUri);
 			}
 		});
 		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int which) {
 				// do nothing
 			}
 		});
 		alert.show();
 	}
 
 	public void replaceFragment(Class<? extends Fragment> clazz) {
 		Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment);
 		if (current != null && current.getClass().equals(clazz))
 			return;
 
 		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
 
		for (WeakReference<Fragment> frag : _savedFragments)
			if (frag.get() != null && frag.get().getClass().equals(clazz)) {
				ft.replace(R.id.fragment, frag.get()).commit();
				return;
			}

 		try {
			ft.replace(R.id.fragment, clazz.getConstructor().newInstance());
 		} catch (IllegalArgumentException e) {
 			return;
 		} catch (InstantiationException e) {
 			return;
 		} catch (IllegalAccessException e) {
 			return;
 		} catch (InvocationTargetException e) {
 			return;
 		} catch (NoSuchMethodException e) {
 			return;
 		}

		ft.addToBackStack(null);
		ft.commit();
 	}
 
 	@Override
 	public void onConfigurationChanged(Configuration newConfig) {
 		super.onConfigurationChanged(newConfig);
 		_drawerToggle.onConfigurationChanged(newConfig);
 	}
 
 	@Override
 	protected void onPostCreate(Bundle savedInstanceState) {
 		super.onPostCreate(savedInstanceState);
 		_drawerToggle.syncState();
 	}
 
 	@Override
     public boolean onOptionsItemSelected(MenuItem item) {
         if (_drawerToggle.onOptionsItemSelected(item))
           return true;
         return super.onOptionsItemSelected(item);
     }
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putInt("fragmentId", _fragmentId);
 	}
 	
 	@Override
 	protected void onPause() {
 		super.onPause();
 		getContentResolver().unregisterContentObserver(_activePodcastObserver);
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
 		Helper.registerMediaButtons(this);
 	}
 
 	@Override
 	public void onAttachFragment(Fragment fragment) {
 		super.onAttachFragment(fragment);
 		for (WeakReference<Fragment> frag : _savedFragments)
 			if (frag.get() != null && frag.get().getClass().equals(fragment.getClass()))
 				return;
 		_savedFragments.add(new WeakReference<Fragment>(fragment));
 	}
 
 	private static int _defaultTextColor = 0;
 
 	class PodaxDrawerAdapter extends BaseAdapter {
 		class Item {
 			String label;
 			int drawable;
 			boolean isHeader;
 
 			public Item(String label, int drawable, boolean isHeader) {
 				this.drawable = drawable;
 				this.isHeader = isHeader;
 				if (this.isHeader)
 					this.label = label.toUpperCase();
 				else
 					this.label = label;
 			}
 
 			public Item(int labelId, int drawableId, boolean isHeader) {
 				this(MainActivity.this.getResources().getString(labelId), drawableId, isHeader);
 			}
 		}
 
 		Item _items[] = {
 				new Item(R.string.app_name, 0, true),
 				new Item(R.string.welcome, android.R.drawable.ic_menu_compass, false),
 				new Item(R.string.now_playing, android.R.drawable.ic_menu_sort_by_size, false),
 				new Item(R.string.playlist, android.R.drawable.ic_menu_agenda, false),
 				new Item(R.string.podcasts, android.R.drawable.ic_menu_slideshow, false),
 				new Item(R.string.search, android.R.drawable.ic_menu_search, false),
 
 				new Item(R.string.subscribe_to_podcasts, 0, true),
 				new Item(R.string.add_rss_feed, android.R.drawable.ic_menu_add, false),
 				new Item(R.string.top_itunes_podcasts, android.R.drawable.ic_menu_recent_history, false),
 				new Item(R.string.gpodder_sync, R.drawable.ic_menu_mygpo, false),
 
 				new Item(R.string.settings, 0, true),
 				new Item(R.string.preferences, android.R.drawable.ic_menu_preferences, false),
 				new Item(R.string.about, R.drawable.ic_menu_podax, false),
 				new Item(R.string.log_viewer, android.R.drawable.ic_menu_info_details, false),
 		};
 
 		private final int HEADER = 0;
 		private final int NORMAL = 1;
 
 		private Context _context;
 
 		public PodaxDrawerAdapter(Context context) {
 			_context = context;
 		}
 
 		@Override
 		public int getCount() {
 			// log viewer is only available when debugging
 			if (PodaxLog.isDebuggable(MainActivity.this))
 				return _items.length;
 			return _items.length - 1;
 		}
 
 		@Override
 		public Object getItem(int position) {
 			return _items[position];
 		}
 
 		@Override
 		public long getItemId(int position) {
 			return position;
 		}
 
 		@Override
 		public View getView(int position, View convertView, ViewGroup parent) {
 			boolean isHeader = getItemViewType(position) == HEADER;
 			int layoutId = isHeader ? R.layout.drawer_header_listitem : R.layout.drawer_listitem;
 			if (convertView == null)
 				convertView = LayoutInflater.from(_context).inflate(layoutId, null);
 			if (convertView == null)
 				return null;
 
 			TextView tv = (TextView) convertView;
 			if (_defaultTextColor == 0)
 				_defaultTextColor = tv.getCurrentTextColor();
 
 			tv.setText(_items[position].label);
 			tv.setCompoundDrawablesWithIntrinsicBounds(_items[position].drawable, 0, 0, 0);
 			return tv;
 		}
 
 		@Override
 		public int getItemViewType(int position) {
 			return _items[position].isHeader ? HEADER : NORMAL;
 		}
 
 		@Override
 		public int getViewTypeCount() {
 			return 2;
 		}
 
 		@Override
 		public boolean isEnabled(int position) {
 			return !_items[position].isHeader;
 		}
 
 		@Override
 		public boolean hasStableIds() {
 			return true;
 		}
 	}
 
 	private ContentObserver _activePodcastObserver = new ContentObserver(new Handler()) {
 		@Override
 		public void onChange(boolean selfChange, Uri uri) {
 			updateDrawerControls();
 		}
 
 		@Override
 		public void onChange(boolean selfChange) {
 			this.onChange(selfChange, null);
 		}
 	};
 
 	private void updateDrawerControls() {
 		// we could put the db call in a thread
 		AQuery aq = new AQuery(this);
 		PlayerStatus status = PlayerStatus.getCurrentState(MainActivity.this);
 		aq.id(R.id.pause).image(status.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
 		if (status.hasActivePodcast())
 			aq.id(R.id.podcast_title).text(status.getTitle());
 		else
 			aq.id(R.id.podcast_title).text("Queue empty");
 	}
 }
