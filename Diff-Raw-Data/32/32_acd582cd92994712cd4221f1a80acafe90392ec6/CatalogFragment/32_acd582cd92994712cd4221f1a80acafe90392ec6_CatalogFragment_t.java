 package net.nightwhistler.pageturner.catalog;
 
 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Stack;
 
 import javax.annotation.Nullable;
 
 import android.util.DisplayMetrics;
 import android.widget.*;
 import com.actionbarsherlock.app.SherlockFragmentActivity;
 import com.actionbarsherlock.widget.SearchView;
 import net.nightwhistler.nucular.atom.AtomConstants;
 import net.nightwhistler.nucular.atom.Entry;
 import net.nightwhistler.nucular.atom.Feed;
 import net.nightwhistler.nucular.atom.Link;
 import net.nightwhistler.pageturner.Configuration;
 import net.nightwhistler.pageturner.CustomOPDSSite;
 import net.nightwhistler.pageturner.R;
 import net.nightwhistler.pageturner.activity.DialogFactory;
 import net.nightwhistler.pageturner.activity.LibraryActivity;
 import net.nightwhistler.pageturner.activity.PageTurnerPrefsActivity;
 import net.nightwhistler.pageturner.activity.ReadingActivity;
 import net.nightwhistler.pageturner.catalog.DownloadFileTask.DownloadFileCallback;
 import net.nightwhistler.pageturner.library.LibraryService;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import roboguice.RoboGuice;
 import roboguice.inject.InjectView;
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnCancelListener;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.Bundle;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView.OnItemClickListener;
 
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuInflater;
 import com.actionbarsherlock.view.MenuItem;
 import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 
 public class CatalogFragment extends RoboSherlockFragment implements
 		LoadFeedCallback, DialogFactory.SearchCallBack {
 	
     private static final String STATE_NAV_ARRAY_KEY = "nav_array";    
 
 	private ProgressDialog downloadDialog;
 
 	private static final Logger LOG = LoggerFactory
 			.getLogger("CatalogFragment");
 
 	private Stack<String> navStack = new Stack<String>();
 
 	@InjectView(R.id.catalogList)
 	@Nullable
 	private ListView catalogList;
 
 	@Inject
 	private Configuration config;
 	
 	@Inject
 	private LibraryService libraryService;
 	
 	@Inject
 	private Provider<LoadOPDSTask> loadOPDSTaskProvider;
 	
 	@Inject
 	private Provider<LoadFakeFeedTask> loadFakeFeedTaskProvider;
 	
 	@Inject
 	private Provider<DownloadFileTask> downloadFileTaskProvider;
 
     @Inject
     private DialogFactory dialogFactory;
 	
     @Inject
 	private CatalogListAdapter adapter;
 
     @Inject
     private Provider<DisplayMetrics> metricsProvider;
 
     private int displayDensity;
 	
 	private LinkListener linkListener;
 
     private MenuItem searchMenuItem;
 
 	private static interface LinkListener {
 		void linkUpdated();
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		if (savedInstanceState != null) {
 			List<String> navList = savedInstanceState.getStringArrayList(STATE_NAV_ARRAY_KEY);
 			if (navList != null && navList.size() > 0) {
 				navStack.addAll(navList);
 			}
 		}
 
         DisplayMetrics metrics = metricsProvider.get();
         getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
 
         this.displayDensity = metrics.densityDpi;
         this.adapter.setDisplayDensity(displayDensity);
         LOG.debug("Metrics at init: " + this.displayDensity );
 
 	}
 	
 	public boolean dispatchKeyEvent(KeyEvent event) {
 
 		int action = event.getAction();
 		int keyCode = event.getKeyCode();		
 
 		if( keyCode == KeyEvent.KEYCODE_SEARCH
 				&& action == KeyEvent.ACTION_DOWN) {
 			onSearchRequested();
 			return true;
 
 		} else if ( keyCode == KeyEvent.KEYCODE_BACK
                 && action == KeyEvent.ACTION_DOWN) {
             onBackPressed();
             return true;
         }
 
 		return false;		
 	}
 	
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
 		return inflater.inflate(R.layout.fragment_catalog, container, false);
 	}
 	
 	@Override
 	public void onViewCreated(View view, Bundle savedInstanceState) {
 		super.onViewCreated(view, savedInstanceState);
 
 		setHasOptionsMenu(true);
 		catalogList.setAdapter(adapter);
         catalogList.setOnScrollListener(new LoadingScrollListener());
 		catalogList.setOnItemClickListener(new OnItemClickListener() {			
 			@Override
 			public void onItemClick(AdapterView<?> list, View arg1, int position,
 					long arg3) {
 				Entry entry = adapter.getItem(position);
 				onEntryClicked(entry, position);
 			}
 		});
 
 		this.downloadDialog = new ProgressDialog(getActivity());
 
 		this.downloadDialog.setIndeterminate(false);		
 		this.downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
 		this.downloadDialog.setCancelable(true);
 	}	
 	
 	private void loadOPDSFeed(String url) {
 		loadOPDSFeed(null, url, false, ResultType.REPLACE);
 	}
 	
 	private void loadOPDSFeed( Entry entry, String url, boolean asDetailsFeed, ResultType resultType ) {
 		LoadOPDSTask task = this.loadOPDSTaskProvider.get();
 		task.setCallBack(this);
 
         task.setResultType(resultType);
 		task.setPreviousEntry(entry);	
 		task.setAsDetailsFeed(asDetailsFeed);
 		
 		task.execute(url);
 	}	
 
 	@Override
 	public void onActivityCreated(Bundle savedInstanceState) {
 		super.onActivityCreated(savedInstanceState);
 
 		Intent intent = getActivity().getIntent();
 		
 		if (!navStack.empty()) {			
 			loadOPDSFeed(navStack.peek());
 		} else {
 			Uri uri = intent.getData();
 
 			if (uri != null && uri.toString().startsWith("epub://")) {
 				String downloadUrl = uri.toString().replace("epub://", "http://");
 				startDownload(false, downloadUrl);
 			} else {
 				loadOPDSFeed(config.getBaseOPDSFeed());
 			}
 		}
 	}
 
     @Override
 	public void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		if (!navStack.empty()) {
 			ArrayList<String> navList = new ArrayList<String>(navStack);
 			outState.putStringArrayList(STATE_NAV_ARRAY_KEY, navList);
 		}
 	}
 
     public void performSearch(String searchTerm) {
     	if (searchTerm != null && searchTerm.length() > 0) {
 			String searchString = URLEncoder.encode(searchTerm);
 			String linkUrl = adapter.getFeed().getSearchLink()
 					.getHref();
 
 			linkUrl = linkUrl.replace("{searchTerms}",
 					searchString);
 
 			loadURL(linkUrl);
 		}
     }
     
 	public void onSearchRequested() {
 
         if ( searchMenuItem == null || ! searchMenuItem.isEnabled() ) {
             return;
         }
 
         if ( searchMenuItem.getActionView() != null ) {
             this.searchMenuItem.expandActionView();
             this.searchMenuItem.getActionView().requestFocus();
         } else {
            dialogFactory.showSearchDialog(R.string.search_books, R.string.enter_query, CatalogFragment.this);
         }
 	}
 
 	public void onEntryClicked( Entry entry, int position ) {		
 			
 		if ( entry.getId() != null && entry.getId().equals(Catalog.CUSTOM_SITES_ID) ) {			
 			loadCustomSiteFeed();
 		} else if ( entry.getAlternateLink() != null ) {
 			String href = entry.getAlternateLink().getHref();
 			loadURL(entry, href, true, ResultType.REPLACE);
 		} else if ( entry.getEpubLink() != null ) {
 			loadFakeFeed(entry);
 		} else if ( entry.getAtomLink() != null ) {
 			String href = entry.getAtomLink().getHref();
 			loadURL(entry, href, false, ResultType.REPLACE);
 		} 
 	}	
 	
 	private void loadCustomSiteFeed() {
 		
 		List<CustomOPDSSite> sites = config.getCustomOPDSSites();
 		
 		if ( sites.isEmpty() ) {
 			Toast.makeText(getActivity(), R.string.no_custom_sites, Toast.LENGTH_LONG).show();
 			return;
 		}
 		
 		navStack.add(Catalog.CUSTOM_SITES_ID);
 		
 		Feed customSites = new Feed();
         customSites.setURL(Catalog.CUSTOM_SITES_ID);
 		customSites.setTitle(getString(R.string.custom_site));
 
 		
 		for ( CustomOPDSSite site: sites ) {
 			Entry entry = new Entry();
 			entry.setTitle(site.getName());
 			entry.setSummary(site.getDescription());
 			
 			Link link = new Link(site.getUrl(), AtomConstants.TYPE_ATOM, AtomConstants.REL_BUY);
 			entry.addLink(link);
 			
 			customSites.addEntry(entry);
 		}
 		
 		customSites.setId(Catalog.CUSTOM_SITES_ID);
 		
 		setNewFeed(customSites, ResultType.REPLACE);
 	}
 
 	public void loadFakeFeed(Entry entry) {
 
         String base = entry.getFeed().getURL();
 
 		if (!navStack.isEmpty()) {
 			base = navStack.peek();
 		}
 
 		navStack.push(base);
 		
 		LoadFakeFeedTask task = this.loadFakeFeedTaskProvider.get();
 		task.setCallBack(this);
 		task.setSingleEntry(entry);
 		
 		task.execute(base);
 	}
 
 	private void loadURL(String url) {
 		loadURL(null, url, false, ResultType.REPLACE);
 	}
 
 	private void loadURL(Entry entry, String url, boolean asDetailsFeed, ResultType resultType) {
 
 		String base = entry.getFeed().getURL();
 
 		if (!navStack.isEmpty()) {
 			base = navStack.peek();
 		}
 
 		try {
 			String target = url;
 			
 			if ( base != null && ! base.equals(Catalog.CUSTOM_SITES_ID)) {
 				target = new URL(new URL(base), url).toString();
 			}
 
 			LOG.info("Loading " + target);
 
             if ( resultType == ResultType.REPLACE ) {
 			    navStack.push(target);
             }
 
 			loadOPDSFeed(entry, target, asDetailsFeed, resultType);
 		} catch (MalformedURLException u) {
 			LOG.error("Malformed URL:", u);
 		}
 	}	
 
 	@Override
 	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
 		getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(true);		
 		inflater.inflate(R.menu.catalog_menu, menu);
 
         this.searchMenuItem = menu.findItem(R.id.search);
         if (searchMenuItem != null) {
             final SearchView searchView = (SearchView) searchMenuItem.getActionView();
 
             if (searchView != null) {
 
                 searchView.setSubmitButtonEnabled(true);
                 searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                     @Override
                     public boolean onQueryTextSubmit(String query) {
                         performSearch(query);
                         return true;
                     }
 
                     @Override
                     public boolean onQueryTextChange(String query) {
                         return  false;
                     }
                 } );
             } else {
                 searchMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                     @Override
                     public boolean onMenuItemClick(MenuItem item) {
                        dialogFactory.showSearchDialog(R.string.search_books, R.string.enter_query, CatalogFragment.this);
                         return false;
                     }
                 });
             }
         }
 	}
 	
 	@Override
 	public void onPrepareOptionsMenu(Menu menu) {
 		Feed feed = adapter.getFeed();
 		
 		if ( feed == null ) {
 			return;
 		}
 
 		boolean searchEnabled = feed.getSearchLink() != null;
 		
 		for ( int i=0; i < menu.size(); i++ ) {
 			MenuItem item = menu.getItem(i);
 			
 			boolean enabled = false;
 			
 			switch (item.getItemId()) {
 
 			case R.id.search:
 				enabled = searchEnabled;
 				break;			
 			default:
 				enabled = true;
 			}			
 			
 			item.setEnabled(enabled);
 			item.setVisible(enabled);			
 		}
 	}	
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 
 		
 		switch (item.getItemId()) {
 		case android.R.id.home:
 			navStack.clear();
 			loadOPDSFeed(config.getBaseOPDSFeed());
 			return true;
 		case R.id.prefs:
 			Intent prefsIntent = new Intent(getActivity(), PageTurnerPrefsActivity.class);
 			startActivity(prefsIntent);
 			break;
 		case R.id.open_library:
 			Intent libIntent = new Intent(getActivity(), LibraryActivity.class);
 			startActivity(libIntent);
 			getActivity().finish();
 			break;
 		}		
 		
 		return true;
 	}
 
 	@Override
 	public void onStop() {
 		downloadDialog.dismiss();
 
 		super.onStop();
 	}
 
 	// TODO Refactor this. Let the platform push/pop fragments from the fragment stack.
 	public void onBackPressed() {
 		if (navStack.isEmpty()) {
 			getActivity().finish();
 			return;
 		} 	
 		
 		navStack.pop();
 		
 		if (navStack.isEmpty()) {
 			loadOPDSFeed(config.getBaseOPDSFeed());
 		} else if ( navStack.peek().equals(Catalog.CUSTOM_SITES_ID) ) {
 			loadCustomSiteFeed();
 		}else {
 			loadOPDSFeed(navStack.peek());
 		}
 	}
 
 	public void notifyLinkUpdated() {
 		adapter.notifyDataSetChanged();
 		
 		if ( linkListener != null ) {
 			linkListener.linkUpdated();
 			linkListener = null;
 		}		
 	}
 	
 	private void startDownload(final boolean openOnCompletion, final String url) {
 		
 		DownloadFileCallback callBack = new DownloadFileCallback() {
 			
 			@Override
 			public void onDownloadStart() {
 				downloadDialog.setMessage(getString(R.string.downloading));
 				downloadDialog.show();				
 			}
 			
 			@Override
 			public void progressUpdate(long progress, long total, int percentage) {				
 				downloadDialog.setMax( Long.valueOf(total).intValue() );
 				downloadDialog.setProgress(Long.valueOf(progress).intValue());				
 			}
 			
 			@Override
 			public void downloadSuccess(File destFile) {
 
 				downloadDialog.hide();
 				
 				if ( openOnCompletion ) {				
 					Intent intent;
 					
 					intent = new Intent(getActivity().getBaseContext(),
 						ReadingActivity.class);
 					intent.setData(Uri.parse(destFile.getAbsolutePath()));
 				
 					startActivity(intent);
 					getActivity().finish();			
 				} else {
 					Toast.makeText(getActivity(), R.string.download_complete,
 							Toast.LENGTH_LONG).show();
 				}				
 			}
 			
 			@Override
 			public void downloadFailed() {
 				
 				downloadDialog.hide();
 				
 				Toast.makeText(getActivity(), R.string.book_failed,
 						Toast.LENGTH_LONG).show();				
 			}
 		};
 		
 		final DownloadFileTask task = this.downloadFileTaskProvider.get();
 		
 		OnCancelListener cancelListener = new OnCancelListener() {
 			
 			@Override
 			public void onCancel(DialogInterface dialog) {
 				task.cancel(true);				
 			}
 		};
 		
 		downloadDialog.setOnCancelListener(cancelListener);
 		
 		task.setCallBack(callBack);
 		task.execute(url);
 		
 	}
 	
 	private void showItemPopup(final Feed feed) {
 		
 		//If we're here, the feed always has just 1 entry
 		final Entry entry = feed.getEntries().get(0);
 		
 		//Also, we don't want this entry on the nav-stack
 		navStack.pop();
 		
 		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
 		builder.setTitle(feed.getTitle());
 		View layout = getLayoutInflater(null).inflate(R.layout.catalog_download, null);
 		builder.setView( layout );		
 
 		TextView authorTextView = (TextView) layout
 				.findViewById(R.id.itemAuthor);
 
 		builder.setNegativeButton(android.R.string.cancel, null);
 		
 		if ( entry.getEpubLink() != null ) {
 			
 			String base = feed.getURL();
 
 			if (!navStack.isEmpty()) {
 				base = navStack.peek();
 			}
 			
 			try {
 				final URL url = new URL(new URL(base), entry.getEpubLink().getHref());
 				
 				builder.setPositiveButton(R.string.read, new DialogInterface.OnClickListener() {
 					
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						startDownload(true, url.toExternalForm());						
 					}
 				});
 				
 				builder.setNeutralButton(R.string.add_to_library, new DialogInterface.OnClickListener() {
 					
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						startDownload(false, url.toExternalForm());						
 					}
 				});				
 				
 			} catch (MalformedURLException e) {
 				throw new RuntimeException(e);
 			}
 			
 		}	
 
 		if (entry.getBuyLink() != null) {
 			builder.setNeutralButton(R.string.buy_now, new DialogInterface.OnClickListener() {
 
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					String url = entry.getBuyLink().getHref();
 					Intent i = new Intent(Intent.ACTION_VIEW);
 					i.setData(Uri.parse(url));
 					startActivity(i);
 				}
 			});
 		}
 
 		if (entry.getAuthor() != null) {
 			String authorText = String.format(
 					getString(R.string.book_by), entry.getAuthor()
 							.getName());
 			authorTextView.setText(authorText);
 		} else {
 			authorTextView.setText("");
 		}
 		
 		final Link imgLink = Catalog.getImageLink(feed, entry);
 		
 		Catalog.loadBookDetails(getActivity(), layout, entry, imgLink, false, displayDensity);
 		final ImageView icon = (ImageView) layout.findViewById(R.id.itemIcon);
 		
 		linkListener = new LinkListener() {
 			
 			@Override
 			public void linkUpdated() {
 				Catalog.loadImageLink(getActivity(), icon, imgLink, false, displayDensity);
 			}
 		};
 		
 		builder.show();
 	}
 
 	@Override
 	public void errorLoadingFeed(String error) {
 		Toast.makeText(getActivity(), getString(R.string.feed_failed) + ": " + error,
 				Toast.LENGTH_LONG).show();		
 	}
 	
 	public void setNewFeed(Feed result, ResultType resultType) {
 
 		if (result != null) {
 			
 			if ( result.isDetailFeed() ) {
 				showItemPopup(result);
 			} else {
 
                 if ( resultType == null || resultType == ResultType.REPLACE ) {
 				    adapter.setFeed(result);
                 } else {
                     adapter.addEntriesFromFeed(result);
                 }
 
 				getSherlockActivity().supportInvalidateOptionsMenu();
 				getSherlockActivity().getSupportActionBar().setTitle(result.getTitle());
 			}
 		} 
 	}
 
     private void setSupportProgressBarIndeterminateVisibility(boolean enable) {
         SherlockFragmentActivity activity = getSherlockActivity();
         if ( activity != null) {
             LOG.debug("Setting progress bar to " + enable );
             activity.setSupportProgressBarIndeterminateVisibility(enable);
         } else {
             LOG.debug("Got null activity.");
         }
     }
 
     @Override
     public void onLoadingDone() {
         LOG.debug("Done loading.");
         setSupportProgressBarIndeterminateVisibility(false);
     }
 
     @Override
     public void onLoadingStart() {
         LOG.debug("Start loading.");
         setSupportProgressBarIndeterminateVisibility(true);
     }
 
     private class LoadingScrollListener implements AbsListView.OnScrollListener {
 
         private static final int LOAD_THRESHOLD = 2;
 
         private String lastLoadedUrl = "";
 
         @Override
         public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
 
             if ( totalItemCount - (firstVisibleItem + visibleItemCount) <= LOAD_THRESHOLD && adapter.getCount() > 0) {
 
                 Entry lastEntry = adapter.getItem( adapter.getCount() -1 );
                 Feed feed = lastEntry.getFeed();
 
                 if ( feed == null || feed.getNextLink() == null) {
                     return;
                 }
 
                 Link nextLink = feed.getNextLink();
 
                 if ( ! nextLink.getHref().equals(lastLoadedUrl) ) {
                     Entry nextEntry = new Entry();
                     nextEntry.setFeed(feed);
                     nextEntry.addLink(nextLink);
 
                     LOG.debug("Starting download for " + nextLink.getHref() + " after scroll");
 
                     lastLoadedUrl = nextLink.getHref();
                     loadURL(nextEntry, nextLink.getHref(), false, ResultType.APPEND);
                 }
             }
         }
 
         @Override
         public void onScrollStateChanged(AbsListView view, int scrollState) {
 
         }
     }
 
 }
