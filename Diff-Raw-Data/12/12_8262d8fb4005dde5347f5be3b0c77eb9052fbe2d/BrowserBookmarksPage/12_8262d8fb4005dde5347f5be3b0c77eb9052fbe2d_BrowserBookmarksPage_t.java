 /*
  * Copyright (C) 2006 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.browser;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.ClipboardManager;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.graphics.Bitmap;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.os.ServiceManager;
 import android.provider.Browser;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.webkit.WebIconDatabase.IconListener;
 import android.widget.AdapterView;
 import android.widget.GridView;
 import android.widget.ListView;
 import android.widget.Toast;
 import android.widget.AdapterView.OnItemClickListener;
 
 /*package*/ enum BookmarkViewMode { NONE, GRID, LIST }
 /**
  *  View showing the user's bookmarks in the browser.
  */
 public class BrowserBookmarksPage extends Activity implements
         View.OnCreateContextMenuListener {
 
     private BookmarkViewMode        mViewMode = BookmarkViewMode.NONE;
     private GridView                mGridPage;
     private ListView                mVerticalList;
     private BrowserBookmarksAdapter mBookmarksAdapter;
     private static final int        BOOKMARKS_SAVE = 1;
     private boolean                 mDisableNewWindow;
     private BookmarkItem            mContextHeader;
     private AddNewBookmark          mAddHeader;
     private boolean                 mCanceled = false;
     private boolean                 mCreateShortcut;
     private boolean                 mMostVisited;
     private View                    mEmptyView;
     private int                     mIconSize;
 
     private final static String LOGTAG = "browser";
     private final static String PREF_BOOKMARK_VIEW_MODE = "pref_bookmark_view_mode";
     private final static String PREF_MOST_VISITED_VIEW_MODE = "pref_most_visited_view_mode";
 
     @Override
     public boolean onContextItemSelected(MenuItem item) {
         // It is possible that the view has been canceled when we get to
         // this point as back has a higher priority
         if (mCanceled) {
             return true;
         }
         AdapterView.AdapterContextMenuInfo i =
             (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
         // If we have no menu info, we can't tell which item was selected.
         if (i == null) {
             return true;
         }
 
         switch (item.getItemId()) {
         case R.id.new_context_menu_id:
             saveCurrentPage();
             break;
         case R.id.open_context_menu_id:
             loadUrl(i.position);
             break;
         case R.id.edit_context_menu_id:
             editBookmark(i.position);
             break;
         case R.id.shortcut_context_menu_id:
             sendBroadcast(createShortcutIntent(i.position));
             break;
         case R.id.delete_context_menu_id:
             if (mMostVisited) {
                 Browser.deleteFromHistory(getContentResolver(),
                         getUrl(i.position));
                 refreshList();
             } else {
                 displayRemoveBookmarkDialog(i.position);
             }
             break;
         case R.id.new_window_context_menu_id:
             openInNewWindow(i.position);
             break;
         case R.id.share_link_context_menu_id:
             BrowserActivity.sharePage(BrowserBookmarksPage.this,
                     mBookmarksAdapter.getTitle(i.position), getUrl(i.position),
                     getFavicon(i.position),
                     mBookmarksAdapter.getScreenshot(i.position));
             break;
         case R.id.copy_url_context_menu_id:
             copy(getUrl(i.position));
             break;
         case R.id.homepage_context_menu_id:
             BrowserSettings.getInstance().setHomePage(this,
                     getUrl(i.position));
             Toast.makeText(this, R.string.homepage_set,
                     Toast.LENGTH_LONG).show();
             break;
         // Only for the Most visited page
         case R.id.save_to_bookmarks_menu_id:
             boolean isBookmark;
             String name;
             String url;
             if (mViewMode == BookmarkViewMode.GRID) {
                 isBookmark = mBookmarksAdapter.getIsBookmark(i.position);
                 name = mBookmarksAdapter.getTitle(i.position);
                 url = mBookmarksAdapter.getUrl(i.position);
             } else {
                 HistoryItem historyItem = ((HistoryItem) i.targetView);
                 isBookmark = historyItem.isBookmark();
                 name = historyItem.getName();
                 url = historyItem.getUrl();
             }
             // If the site is bookmarked, the item becomes remove from
             // bookmarks.
             if (isBookmark) {
                 Bookmarks.removeFromBookmarks(this, getContentResolver(), url, name);
             } else {
                 Browser.saveBookmark(this, name, url);
             }
             break;
         default:
             return super.onContextItemSelected(item);
         }
         return true;
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v,
                 ContextMenuInfo menuInfo) {
             AdapterView.AdapterContextMenuInfo i =
                     (AdapterView.AdapterContextMenuInfo) menuInfo;
 
             MenuInflater inflater = getMenuInflater();
             if (mMostVisited) {
                 inflater.inflate(R.menu.historycontext, menu);
             } else {
                 inflater.inflate(R.menu.bookmarkscontext, menu);
             }
 
             if (0 == i.position && !mMostVisited) {
                 menu.setGroupVisible(R.id.CONTEXT_MENU, false);
                 if (mAddHeader == null) {
                     mAddHeader = new AddNewBookmark(BrowserBookmarksPage.this);
                 } else if (mAddHeader.getParent() != null) {
                     ((ViewGroup) mAddHeader.getParent()).
                             removeView(mAddHeader);
                 }
                 mAddHeader.setUrl(getIntent().getStringExtra("url"));
                 menu.setHeaderView(mAddHeader);
                 return;
             }
             if (mMostVisited) {
                 if ((mViewMode == BookmarkViewMode.LIST
                         && ((HistoryItem) i.targetView).isBookmark())
                         || mBookmarksAdapter.getIsBookmark(i.position)) {
                     MenuItem item = menu.findItem(
                             R.id.save_to_bookmarks_menu_id);
                     item.setTitle(R.string.remove_from_bookmarks);
                 }
             } else {
                 // The historycontext menu has no ADD_MENU group.
                 menu.setGroupVisible(R.id.ADD_MENU, false);
             }
             if (mDisableNewWindow) {
                 menu.findItem(R.id.new_window_context_menu_id).setVisible(
                         false);
             }
             if (mContextHeader == null) {
                 mContextHeader = new BookmarkItem(BrowserBookmarksPage.this);
             } else if (mContextHeader.getParent() != null) {
                 ((ViewGroup) mContextHeader.getParent()).
                         removeView(mContextHeader);
             }
             if (mViewMode == BookmarkViewMode.GRID) {
                 mBookmarksAdapter.populateBookmarkItem(mContextHeader,
                         i.position);
             } else {
                 BookmarkItem b = (BookmarkItem) i.targetView;
                 b.copyTo(mContextHeader);
             }
             menu.setHeaderView(mContextHeader);
         }
 
     /**
      *  Create a new BrowserBookmarksPage.
      */
     @Override
     protected void onCreate(Bundle icicle) {
         super.onCreate(icicle);
 
         // Grab the app icon size as a resource.
         mIconSize = getResources().getDimensionPixelSize(
                 android.R.dimen.app_icon_size);
 
         Intent intent = getIntent();
         if (Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
             mCreateShortcut = true;
         }
         mDisableNewWindow = intent.getBooleanExtra("disable_new_window",
                 false);
         mMostVisited = intent.getBooleanExtra("mostVisited", false);
 
         if (mCreateShortcut) {
             setTitle(R.string.browser_bookmarks_page_bookmarks_text);
         }
 
         setContentView(R.layout.empty_history);
         mEmptyView = findViewById(R.id.empty_view);
         mEmptyView.setVisibility(View.GONE);
 
         SharedPreferences p = getPreferences(MODE_PRIVATE);
 
         // See if the user has set a preference for the view mode of their
         // bookmarks. Otherwise default to grid mode.
         BookmarkViewMode preference = BookmarkViewMode.NONE;
         if (mMostVisited) {
             // For the most visited page, only use list mode.
             preference = BookmarkViewMode.LIST;
         } else {
             preference = BookmarkViewMode.values()[p.getInt(
                     PREF_BOOKMARK_VIEW_MODE, BookmarkViewMode.GRID.ordinal())];
         }
         switchViewMode(preference);
 
         final boolean createShortcut = mCreateShortcut;
         final boolean mostVisited = mMostVisited;
         final String url = intent.getStringExtra("url");
         final String title = intent.getStringExtra("title");
         final Bitmap thumbnail =
                 (Bitmap) intent.getParcelableExtra("thumbnail");
         new AsyncTask<Void, Void, Void>() {
             @Override
             protected Void doInBackground(Void... unused) {
                 BrowserBookmarksAdapter adapter =
                     new BrowserBookmarksAdapter(
                             BrowserBookmarksPage.this,
                             url,
                             title,
                             thumbnail,
                             createShortcut,
                             mostVisited);
                 mHandler.obtainMessage(ADAPTER_CREATED, adapter).sendToTarget();
                 return null;
             }
         }.execute();
     }
 
     @Override
     protected void onDestroy() {
         mHandler.removeCallbacksAndMessages(null);
         super.onDestroy();
     }
 
     /**
      *  Set the ContentView to be either the grid of thumbnails or the vertical
      *  list.
      */
     private void switchViewMode(BookmarkViewMode viewMode) {
         if (mViewMode == viewMode) {
             return;
         }
 
         mViewMode = viewMode;
 
         // Update the preferences to make the new view mode sticky.
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Editor ed = preferences.edit();
        int pref = mViewMode.ordinal();
        if (mMostVisited && preferences.getInt(PREF_MOST_VISITED_VIEW_MODE, -1) != pref) {
            ed.putInt(PREF_MOST_VISITED_VIEW_MODE, pref);
        } else if (!mMostVisited && preferences.getInt(PREF_BOOKMARK_VIEW_MODE, -1) != pref) {
            ed.putInt(PREF_BOOKMARK_VIEW_MODE, pref);
         }
         ed.commit();
 
         if (mBookmarksAdapter != null) {
             mBookmarksAdapter.switchViewMode(viewMode);
         }
         if (mViewMode == BookmarkViewMode.GRID) {
             if (mGridPage == null) {
                 mGridPage = new GridView(this);
                 if (mBookmarksAdapter != null) {
                     mGridPage.setAdapter(mBookmarksAdapter);
                 }
                 mGridPage.setOnItemClickListener(mListener);
                 mGridPage.setNumColumns(GridView.AUTO_FIT);
                 mGridPage.setColumnWidth(
                         BrowserActivity.getDesiredThumbnailWidth(this));
                 mGridPage.setFocusable(true);
                 mGridPage.setFocusableInTouchMode(true);
                 mGridPage.setSelector(android.R.drawable.gallery_thumb);
                 float density = getResources().getDisplayMetrics().density;
                 mGridPage.setVerticalSpacing((int) (14 * density));
                 mGridPage.setHorizontalSpacing((int) (8 * density));
                 mGridPage.setStretchMode(GridView.STRETCH_SPACING);
                 mGridPage.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                 mGridPage.setDrawSelectorOnTop(true);
                 if (mMostVisited) {
                     mGridPage.setEmptyView(mEmptyView);
                 }
                 if (!mCreateShortcut) {
                     mGridPage.setOnCreateContextMenuListener(this);
                 }
             }
             addContentView(mGridPage, FULL_SCREEN_PARAMS);
             if (mVerticalList != null) {
                 ViewGroup parent = (ViewGroup) mVerticalList.getParent();
                 if (parent != null) {
                     parent.removeView(mVerticalList);
                 }
             }
         } else {
             if (null == mVerticalList) {
                 ListView listView = new ListView(this);
                 if (mBookmarksAdapter != null) {
                     listView.setAdapter(mBookmarksAdapter);
                 }
                 listView.setDrawSelectorOnTop(false);
                 listView.setVerticalScrollBarEnabled(true);
                 listView.setOnItemClickListener(mListListener);
                 if (mMostVisited) {
                     listView.setEmptyView(mEmptyView);
                 }
                 if (!mCreateShortcut) {
                     listView.setOnCreateContextMenuListener(this);
                 }
                 mVerticalList = listView;
             }
             addContentView(mVerticalList, FULL_SCREEN_PARAMS);
             if (mGridPage != null) {
                 ViewGroup parent = (ViewGroup) mGridPage.getParent();
                 if (parent != null) {
                     parent.removeView(mGridPage);
                 }
             }
         }
     }
 
     private static final ViewGroup.LayoutParams FULL_SCREEN_PARAMS
             = new ViewGroup.LayoutParams(
             ViewGroup.LayoutParams.MATCH_PARENT,
             ViewGroup.LayoutParams.MATCH_PARENT);
 
     private static final int SAVE_CURRENT_PAGE = 1000;
     private static final int ADAPTER_CREATED = 1001;
     private final Handler mHandler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
             switch (msg.what) {
                 case SAVE_CURRENT_PAGE:
                     saveCurrentPage();
                     break;
                 case ADAPTER_CREATED:
                     mBookmarksAdapter = (BrowserBookmarksAdapter) msg.obj;
                     mBookmarksAdapter.switchViewMode(mViewMode);
                     if (mGridPage != null) {
                         mGridPage.setAdapter(mBookmarksAdapter);
                     }
                     if (mVerticalList != null) {
                         mVerticalList.setAdapter(mBookmarksAdapter);
                     }
                     // Add our own listener in case there are favicons that
                     // have yet to be loaded.
                     if (mMostVisited) {
                         IconListener listener = new IconListener() {
                             public void onReceivedIcon(String url,
                                     Bitmap icon) {
                                 if (mGridPage != null) {
                                     mGridPage.setAdapter(mBookmarksAdapter);
                                 }
                                 if (mVerticalList != null) {
                                     mVerticalList.setAdapter(mBookmarksAdapter);
                                 }
                             }
                         };
                         CombinedBookmarkHistoryActivity.getIconListenerSet()
                                 .addListener(listener);
                     }
                     break;
             }
         }
     };
 
     private OnItemClickListener mListener = new OnItemClickListener() {
         public void onItemClick(AdapterView parent, View v, int position, long id) {
             // It is possible that the view has been canceled when we get to
             // this point as back has a higher priority
             if (mCanceled) {
                 android.util.Log.e(LOGTAG, "item clicked when dismissing");
                 return;
             }
             if (!mCreateShortcut) {
                 if (0 == position && !mMostVisited) {
                     // XXX: Work-around for a framework issue.
                     mHandler.sendEmptyMessage(SAVE_CURRENT_PAGE);
                 } else {
                     loadUrl(position);
                 }
             } else {
                 setResultToParent(RESULT_OK, createShortcutIntent(position));
                 finish();
             }
         }
     };
 
     private OnItemClickListener mListListener = new OnItemClickListener() {
         public void onItemClick(AdapterView parent, View v, int position, long id) {
             // It is possible that the view has been canceled when we get to
             // this point as back has a higher priority
             if (mCanceled) {
                 android.util.Log.e(LOGTAG, "item clicked when dismissing");
                 return;
             }
             if (!mCreateShortcut) {
                 if (0 == position && !mMostVisited) {
                     // XXX: Work-around for a framework issue.
                     mHandler.sendEmptyMessage(SAVE_CURRENT_PAGE);
                 } else {
                     loadUrl(position);
                 }
             } else {
                 setResultToParent(RESULT_OK, createShortcutIntent(position));
                 finish();
             }
         }
     };
 
     private Intent createShortcutIntent(int position) {
         String url = getUrl(position);
         String title = getBookmarkTitle(position);
         Bitmap touchIcon = getTouchIcon(position);
         Bitmap favicon = getFavicon(position);
         return BookmarkUtils.createAddToHomeIntent(this, url, title, touchIcon, favicon);
     }
 
     private void saveCurrentPage() {
         Intent i = new Intent(BrowserBookmarksPage.this,
                 AddBookmarkPage.class);
         i.putExtras(getIntent());
         startActivityForResult(i, BOOKMARKS_SAVE);
     }
 
     private void loadUrl(int position) {
         Intent intent = (new Intent()).setAction(getUrl(position));
         setResultToParent(RESULT_OK, intent);
         finish();
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         boolean result = super.onCreateOptionsMenu(menu);
         if (!mCreateShortcut && !mMostVisited) {
             MenuInflater inflater = getMenuInflater();
             inflater.inflate(R.menu.bookmarks, menu);
             return true;
         }
         return result;
     }
 
     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
         boolean result = super.onPrepareOptionsMenu(menu);
         if (mCreateShortcut || mMostVisited || mBookmarksAdapter == null
                 || mBookmarksAdapter.getCount() == 0) {
             // No need to show the menu if there are no items.
             return result;
         }
         MenuItem switchItem = menu.findItem(R.id.switch_mode_menu_id);
         int titleResId;
         int iconResId;
         if (mViewMode == BookmarkViewMode.GRID) {
             titleResId = R.string.switch_to_list;
             iconResId = R.drawable.ic_menu_list;
         } else {
             titleResId = R.string.switch_to_thumbnails;
             iconResId = R.drawable.ic_menu_thumbnail;
         }
         switchItem.setTitle(titleResId);
         switchItem.setIcon(iconResId);
         return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
         case R.id.new_context_menu_id:
             saveCurrentPage();
             break;
 
         case R.id.switch_mode_menu_id:
             if (mViewMode == BookmarkViewMode.GRID) {
                 switchViewMode(BookmarkViewMode.LIST);
             } else {
                 switchViewMode(BookmarkViewMode.GRID);
             }
             break;
 
         default:
             return super.onOptionsItemSelected(item);
         }
         return true;
     }
 
     private void openInNewWindow(int position) {
         Bundle b = new Bundle();
         b.putBoolean("new_window", true);
         setResultToParent(RESULT_OK,
                 (new Intent()).setAction(getUrl(position)).putExtras(b));
 
         finish();
     }
 
 
     private void editBookmark(int position) {
         Intent intent = new Intent(BrowserBookmarksPage.this,
             AddBookmarkPage.class);
         intent.putExtra("bookmark", getRow(position));
         startActivityForResult(intent, BOOKMARKS_SAVE);
     }
 
     @Override
     protected void onActivityResult(int requestCode, int resultCode,
                                     Intent data) {
         switch(requestCode) {
             case BOOKMARKS_SAVE:
                 if (resultCode == RESULT_OK) {
                     Bundle extras;
                     if (data != null && (extras = data.getExtras()) != null) {
                         // If there are extras, then we need to save
                         // the edited bookmark. This is done in updateRow()
                         String title = extras.getString("title");
                         String url = extras.getString("url");
                         if (title != null && url != null) {
                             mBookmarksAdapter.updateRow(extras);
                         }
                     } else {
                         // extras == null then a new bookmark was added to
                         // the database.
                         refreshList();
                     }
                 }
                 break;
             default:
                 break;
         }
     }
 
     private void displayRemoveBookmarkDialog(int position) {
         // Put up a dialog asking if the user really wants to
         // delete the bookmark
         final int deletePos = position;
         new AlertDialog.Builder(this)
                 .setTitle(R.string.delete_bookmark)
                 .setIcon(android.R.drawable.ic_dialog_alert)
                 .setMessage(getText(R.string.delete_bookmark_warning).toString().replace(
                         "%s", getBookmarkTitle(deletePos)))
                 .setPositiveButton(R.string.ok,
                         new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int whichButton) {
                                 deleteBookmark(deletePos);
                             }
                         })
                 .setNegativeButton(R.string.cancel, null)
                 .show();
     }
 
     /**
      *  Refresh the shown list after the database has changed.
      */
     private void refreshList() {
         if (mBookmarksAdapter == null) return;
         mBookmarksAdapter.refreshList();
     }
 
     /**
      *  Return a hashmap representing the currently highlighted row.
      */
     public Bundle getRow(int position) {
         return mBookmarksAdapter == null ? null
                 : mBookmarksAdapter.getRow(position);
     }
 
     /**
      *  Return the url of the currently highlighted row.
      */
     public String getUrl(int position) {
         return mBookmarksAdapter == null ? null
                 : mBookmarksAdapter.getUrl(position);
     }
 
     /**
      * Return the favicon of the currently highlighted row.
      */
     public Bitmap getFavicon(int position) {
         return mBookmarksAdapter == null ? null
                 : mBookmarksAdapter.getFavicon(position);
     }
 
     private Bitmap getTouchIcon(int position) {
         return mBookmarksAdapter == null ? null
                 : mBookmarksAdapter.getTouchIcon(position);
     }
 
     private void copy(CharSequence text) {
         ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
         cm.setText(text);
     }
 
     public String getBookmarkTitle(int position) {
         return mBookmarksAdapter == null ? null
                 : mBookmarksAdapter.getTitle(position);
     }
 
     /**
      *  Delete the currently highlighted row.
      */
     public void deleteBookmark(int position) {
         if (mBookmarksAdapter == null) return;
         mBookmarksAdapter.deleteRow(position);
     }
 
     @Override
     public void onBackPressed() {
         setResultToParent(RESULT_CANCELED, null);
         mCanceled = true;
         super.onBackPressed();
     }
 
     // This Activity is generally a sub-Activity of
     // CombinedBookmarkHistoryActivity. In that situation, we need to pass our
     // result code up to our parent. However, if someone calls this Activity
     // directly, then this has no parent, and it needs to set it on itself.
     private void setResultToParent(int resultCode, Intent data) {
         Activity parent = getParent();
         if (parent == null) {
             setResult(resultCode, data);
         } else {
             ((CombinedBookmarkHistoryActivity) parent).setResultFromChild(
                     resultCode, data);
         }
     }
 
 }
