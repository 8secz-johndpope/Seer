 /*
  * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
  * 02110-1301, USA.
  */
 
 package org.geometerplus.android.fbreader.network.browser;
 
 import android.app.Activity;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.Bundle;
 import android.provider.SearchRecentSuggestions;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.Window;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
 import android.webkit.WebChromeClient;
 import android.webkit.WebView;
 import android.webkit.WebViewClient;
 
 import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil;
 import org.geometerplus.zlibrary.ui.android.R;
 
 import org.geometerplus.fbreader.network.NetworkLibrary;
 
 import org.geometerplus.android.fbreader.network.BookDownloaderService;
 
 
 public class BrowserActivity extends Activity {
 
 	protected final ZLResource myResource = ZLResource.resource("browser");
 
 	private String myStoreInRecentUrls;
 	private Object myStoreInRecentUrlsLock = new Object();
 
 	private void setStoreInRecentUrls(String url) {
 		synchronized (myStoreInRecentUrlsLock) {
 			myStoreInRecentUrls = url;
 		}
 	}
 
 	private void storeUrlInRecents() {
 		synchronized (myStoreInRecentUrlsLock) {
 			if (myStoreInRecentUrls != null) {
 				System.err.println("STORE IN RECENTS: " + myStoreInRecentUrls);
 				SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
 						getApplicationContext(),
 						RecentUrlsProvider.AUTHORITY,
 						RecentUrlsProvider.MODE);
 		        suggestions.saveRecentQuery(myStoreInRecentUrls, null);
 			}
 			myStoreInRecentUrls = null;
 		}
 	}
 
 	private void downloadBook(String url) {
 		startService(
 			new Intent(Intent.ACTION_VIEW, Uri.parse(url),
 					getApplicationContext(), BookDownloaderService.class)
 				//.putExtra(BookDownloaderService.BOOK_FORMAT_KEY, ref.BookFormat)
 				//.putExtra(BookDownloaderService.REFERENCE_TYPE_KEY, resolvedType)
 				//.putExtra(BookDownloaderService.CLEAN_URL_KEY, ref.cleanURL())
 				//.putExtra(BookDownloaderService.TITLE_KEY, book.Title)
 				//.putExtra(BookDownloaderService.SSL_CERTIFICATE_KEY, sslCertificate)
 		);
 	}
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		requestWindowFeature(Window.FEATURE_PROGRESS);
 
 		setContentView(R.layout.browser);
 		setTitle(ZLResource.resource("networkView").getResource("browser").getValue());
 
 		WebView view = (WebView) findViewById(R.id.webview);
 		view.setWebChromeClient(new ChromeClient());
 		view.setWebViewClient(new ViewClient());
 
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setUserAgentString(ZLNetworkUtil.getUserAgent());
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setUseWideViewPort(true);

 		final Intent intent = getIntent();
 		final Uri uri = intent.getData();
 
 		final NetworkLibrary library = NetworkLibrary.Instance();
 		final String url;
 		if (uri != null) {
 			url = uri.toString();
 			setStoreInRecentUrls(url);
 		} else {
 			url = library.NetworkBrowserPageOption.getValue();
 		}
 		view.loadUrl(url);
 		library.NetworkBrowserPageOption.setValue(url);
 	}
 
 	@Override
 	protected void onNewIntent(Intent intent) {
 		super.onNewIntent(intent);
 
 		final Uri uri = intent.getData();
 		if (uri != null) {
 			WebView view = (WebView) findViewById(R.id.webview);
 			final String url = uri.toString();
 			setStoreInRecentUrls(url);
 			view.loadUrl(url);
 			NetworkLibrary.Instance().NetworkBrowserPageOption.setValue(url);
 		}
 	}
 
 	private class ChromeClient extends WebChromeClient {
 		@Override
 		public void onReceivedTitle(WebView view, String title) {
 			BrowserActivity.this.setTitle(title);
 		}
 		
 		@Override
 		public void onProgressChanged(WebView view, int newProgress) {
 			final boolean inProgress = newProgress < 100;
 			setProgressBarIndeterminateVisibility(inProgress);
 			setProgressBarVisibility(inProgress);
 			if (inProgress) {
 				setProgress(newProgress);
 			} else {
 				storeUrlInRecents();
 			}
 		}
 	}
 
 	private class ViewClient extends WebViewClient {
 
 		private String[] mySupportedBooksExtensions = {".epub", ".fb2", ".fb2.zip"};
 
 		@Override
 		public void onLoadResource(final WebView view, final String url) {
 			final Uri uri = Uri.parse(url);
 			String path = uri.getPath();
 			if (path != null) {
 				path = path.toLowerCase();
 				for (String ext: mySupportedBooksExtensions) {
 					if (path.endsWith(ext)) {
 						view.stopLoading();
 						downloadBook(url);
 						break;
 					}
 				}
 			}
 		}
 
 		@Override
 		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
 			System.err.println("ON RECIEVED ERROR (" + errorCode + "): " + description);
 			System.err.println("... ON: " + failingUrl);
 		}
 
 		@Override
 		public boolean shouldOverrideUrlLoading(WebView view, String url) {
 			storeUrlInRecents();
 			return false;
 		}
 	}
 
 
 	private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
 		final String label = myResource.getResource("menu").getResource(resourceKey).getValue();
 		return menu.add(0, index, Menu.NONE, label)/*.setIcon(iconId)*/;
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		super.onCreateOptionsMenu(menu);
 		addMenuItem(menu, 1, "go", 0);
 		addMenuItem(menu, 2, "stop", 0);
 		addMenuItem(menu, 3, "reload", 0);
 		return true;
 	}
 
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) {
 		super.onPrepareOptionsMenu(menu);
 		WebView view = (WebView) findViewById(R.id.webview);
 		final boolean loading = view.getUrl() != null && view.getProgress() < 100;
 		menu.findItem(1).setEnabled(!loading).setVisible(!loading);
 		menu.findItem(2).setEnabled(loading).setVisible(loading);
 		menu.findItem(3).setEnabled(!loading && view.getUrl() != null);
 		return true;
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		WebView view = (WebView) findViewById(R.id.webview);
 		switch (item.getItemId()) {
 			default:
 				return true;
 			case 1:
 				return onSearchRequested();
 			case 2:
 				view.stopLoading();
 				return true;
 			case 3:
 				view.reload();
 				return true;
 		}
 	}
 
 	@Override
 	public boolean onSearchRequested() {
 		WebView view = (WebView) findViewById(R.id.webview);
 		startSearch(view.getUrl(), true, null, false);
 		return true;
 	}
 }
