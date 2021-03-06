 /*
  * Copyright (C) 2009 The Android Open Source Project
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
 
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.database.Cursor;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.Bundle;
 import android.provider.Browser;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.webkit.GeolocationPermissions;
 import android.webkit.WebIconDatabase;
 import android.webkit.WebStorage;
 import android.widget.ArrayAdapter;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.ImageView;
 import android.widget.TextView;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.Vector;
 
 /**
  * Manage the settings for an origin.
  * We use it to keep track of the 'HTML5' settings, i.e. database (webstorage)
  * and Geolocation.
  */
 public class WebsiteSettingsActivity extends ListActivity {
 
     private String LOGTAG = "WebsiteSettingsActivity";
     private static String sMBStored = null;
     private SiteAdapter mAdapter = null;
 
     class Site {
         private String mOrigin;
         private String mTitle;
         private Bitmap mIcon;
         private int mFeatures;
 
         // These constants provide the set of features that a site may support
         // They must be consecutive. To add a new feature, add a new FEATURE_XXX
         // variable with value equal to the current value of FEATURE_COUNT, then
         // increment FEATURE_COUNT.
         private final static int FEATURE_WEB_STORAGE = 0;
         private final static int FEATURE_GEOLOCATION = 1;
         // The number of features available.
         private final static int FEATURE_COUNT = 2;
 
         public Site(String origin) {
             mOrigin = origin;
             mTitle = null;
             mIcon = null;
             mFeatures = 0;
         }
 
         public void addFeature(int feature) {
             mFeatures |= (1 << feature);
         }
 
         public boolean hasFeature(int feature) {
             return (mFeatures & (1 << feature)) != 0;
         }
 
         /**
          * Gets the number of features supported by this site.
          */
         public int getFeatureCount() {
             int count = 0;
             for (int i = 0; i < FEATURE_COUNT; ++i) {
                 count += hasFeature(i) ? 1 : 0;
             }
             return count;
         }
 
         /**
          * Gets the ID of the nth (zero-based) feature supported by this site.
          * The return value is a feature ID - one of the FEATURE_XXX values.
          * This is required to determine which feature is displayed at a given
          * position in the list of features for this site. This is used both
          * when populating the view and when responding to clicks on the list.
          */
         public int getFeatureByIndex(int n) {
             int j = -1;
             for (int i = 0; i < FEATURE_COUNT; ++i) {
                 j += hasFeature(i) ? 1 : 0;
                 if (j == n) {
                     return i;
                 }
             }
             return -1;
         }
 
         public String getOrigin() {
             return mOrigin;
         }
 
         public void setTitle(String title) {
             mTitle = title;
         }
 
         public void setIcon(Bitmap icon) {
             mIcon = icon;
         }
 
         public Bitmap getIcon() {
             return mIcon;
         }
 
         public String getPrettyOrigin() {
             return mTitle == null ? null : hideHttp(mOrigin);
         }
 
         public String getPrettyTitle() {
             return mTitle == null ? hideHttp(mOrigin) : mTitle;
         }
 
         private String hideHttp(String str) {
             Uri uri = Uri.parse(str);
             return "http".equals(uri.getScheme()) ?  str.substring(7) : str;
         }
     }
 
     class SiteAdapter extends ArrayAdapter<Site>
             implements AdapterView.OnItemClickListener {
         private int mResource;
         private LayoutInflater mInflater;
         private Bitmap mDefaultIcon;
         private Site mCurrentSite;
 
         public SiteAdapter(Context context, int rsc) {
             super(context, rsc);
             mResource = rsc;
             mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             mDefaultIcon = BitmapFactory.decodeResource(getResources(),
                     R.drawable.ic_launcher_shortcut_browser_bookmark);
             populateOrigins();
         }
 
         /**
          * Adds the specified feature to the site corresponding to supplied
          * origin in the map. Creates the site if it does not already exist.
          */
         private void addFeatureToSite(Map sites, String origin, int feature) {
             Site site = null;
             if (sites.containsKey(origin)) {
                 site = (Site) sites.get(origin);
             } else {
                 site = new Site(origin);
                 sites.put(origin, site);
             }
             site.addFeature(feature);
         }
 
         public void populateOrigins() {
             clear();
 
             // Get the list of origins we want to display.
             // All 'HTML 5 modules' (Database, Geolocation etc) form these
             // origin strings using WebCore::SecurityOrigin::toString(), so it's
             // safe to group origins here. Note that WebCore::SecurityOrigin
             // uses 0 (which is not printed) for the port if the port is the
             // default for the protocol. Eg http://www.google.com and
             // http://www.google.com:80 both record a port of 0 and hence
             // toString() == 'http://www.google.com' for both.
             Set origins = WebStorage.getInstance().getOrigins();
             Map sites = new HashMap<String, Site>();
             if (origins != null) {
                 Iterator<String> iter = origins.iterator();
                 while (iter.hasNext()) {
                     addFeatureToSite(sites, iter.next(), Site.FEATURE_WEB_STORAGE);
                 }
             }
             origins = GeolocationPermissions.getInstance().getOrigins();
             if (origins != null) {
                 Iterator<String> iter = origins.iterator();
                 while (iter.hasNext()) {
                     addFeatureToSite(sites, iter.next(), Site.FEATURE_GEOLOCATION);
                 }
             }
 
             // Create a map from host to origin. This is used to add metadata
             // (title, icon) for this origin from the bookmarks DB.
             HashMap hosts = new HashMap<String, Set<Site> >();
             Set keys = sites.keySet();
             Iterator<String> originIter = keys.iterator();
             while (originIter.hasNext()) {
                 String origin = originIter.next();
                 Site site = (Site) sites.get(origin);
                 String host = Uri.parse(origin).getHost();
                 Set hostSites = null;
                 if (hosts.containsKey(host)) {
                     hostSites = (Set) hosts.get(host);
                 } else {
                     hostSites = new HashSet<Site>();
                     hosts.put(host, hostSites);
                 }
                 hostSites.add(site);
             }
 
             // Check the bookmark DB. If we have data for a host used by any of
             // our origins, use it to set their title and favicon
             Cursor c = getContext().getContentResolver().query(Browser.BOOKMARKS_URI,
                     new String[] { Browser.BookmarkColumns.URL, Browser.BookmarkColumns.TITLE,
                     Browser.BookmarkColumns.FAVICON }, "bookmark = 1", null, null);
 
             if ((c != null) && c.moveToFirst()) {
                 int urlIndex = c.getColumnIndex(Browser.BookmarkColumns.URL);
                 int titleIndex = c.getColumnIndex(Browser.BookmarkColumns.TITLE);
                 int faviconIndex = c.getColumnIndex(Browser.BookmarkColumns.FAVICON);
                 do {
                     String url = c.getString(urlIndex);
                     String host = Uri.parse(url).getHost();
                     if (hosts.containsKey(host)) {
                         String title = c.getString(titleIndex);
                         Bitmap bmp = null;
                         byte[] data = c.getBlob(faviconIndex);
                         if (data != null) {
                             bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                         }
                         Set matchingSites = (Set) hosts.get(host);
                         Iterator<Site> sitesIter = matchingSites.iterator();
                         while (sitesIter.hasNext()) {
                             Site site = sitesIter.next();
                             site.setTitle(title);
                             if (bmp != null) {
                                 site.setIcon(bmp);
                             }
                         }
                     }
                 } while (c.moveToNext());
             }
 
             // We can now simply populate our array with Site instances
             keys = sites.keySet();
             originIter = keys.iterator();
             while (originIter.hasNext()) {
                 String origin = originIter.next();
                 Site site = (Site) sites.get(origin);
                 add(site);
             }
 
             if (getCount() == 0) {
                 finish(); // we close the screen
             }
         }
 
         public int getCount() {
             if (mCurrentSite == null) {
                 return super.getCount();
             }
             return mCurrentSite.getFeatureCount();
         }
 
         public String sizeValueToString(long value) {
             float mb = (float) value / (1024.0F * 1024.0F);
             int val = (int) (mb * 10);
             float ret = (float) (val / 10.0F);
             if (ret <= 0) {
                 return "0";
             }
             return String.valueOf(ret);
         }
 
         /*
          * If we receive the back event and are displaying
          * site's settings, we want to go back to the main
          * list view. If not, we just do nothing (see
          * dispatchKeyEvent() below).
          */
         public boolean backKeyPressed() {
             if (mCurrentSite != null) {
                 mCurrentSite = null;
                 populateOrigins();
                 notifyDataSetChanged();
                 return true;
             }
             return false;
         }
 
         public View getView(int position, View convertView, ViewGroup parent) {
             View view;
             TextView title;
             TextView subtitle;
             ImageView icon;
 
             if (convertView == null) {
                 view = mInflater.inflate(mResource, parent, false);
             } else {
                 view = convertView;
             }
 
             title = (TextView) view.findViewById(R.id.title);
             subtitle = (TextView) view.findViewById(R.id.subtitle);
             icon = (ImageView) view.findViewById(R.id.icon);
 
             if (mCurrentSite == null) {
                 Site site = getItem(position);
                 title.setText(site.getPrettyTitle());
                 subtitle.setText(site.getPrettyOrigin());
                 icon.setVisibility(View.VISIBLE);
                 Bitmap bmp = site.getIcon();
                 if (bmp == null) {
                     bmp = mDefaultIcon;
                 }
                 icon.setImageBitmap(bmp);
                 // We set the site as the view's tag,
                 // so that we can get it in onItemClick()
                 view.setTag(site);
             } else {
                 icon.setVisibility(View.GONE);
                 String origin = mCurrentSite.getOrigin();
                 switch (mCurrentSite.getFeatureByIndex(position)) {
                     case Site.FEATURE_WEB_STORAGE:
                         long usageValue = WebStorage.getInstance().getUsageForOrigin(origin);
                         String usage = sizeValueToString(usageValue) + " " + sMBStored;
 
                         title.setText(R.string.webstorage_clear_data_title);
                         subtitle.setText(usage);
                         break;
                     case Site.FEATURE_GEOLOCATION:
                         title.setText(R.string.geolocation_settings_page_title);
                         boolean allowed = GeolocationPermissions.getInstance().getAllowed(origin);
                         subtitle.setText(allowed ?
                                          R.string.geolocation_settings_page_summary_allowed :
                                          R.string.geolocation_settings_page_summary_not_allowed);
                         break;
                 }
             }
 
             return view;
         }
 
         public void onItemClick(AdapterView<?> parent,
                                 View view,
                                 int position,
                                 long id) {
             if (mCurrentSite != null) {
                 switch (mCurrentSite.getFeatureByIndex(position)) {
                     case Site.FEATURE_WEB_STORAGE:
                         new AlertDialog.Builder(getContext())
                             .setTitle(R.string.webstorage_clear_data_dialog_title)
                             .setMessage(R.string.webstorage_clear_data_dialog_message)
                             .setPositiveButton(R.string.webstorage_clear_data_dialog_ok_button,
                                                new AlertDialog.OnClickListener() {
                                 public void onClick(DialogInterface dlg, int which) {
                                     WebStorage.getInstance().deleteOrigin(mCurrentSite.getOrigin());
                                     mCurrentSite = null;
                                     populateOrigins();
                                     notifyDataSetChanged();
                                 }})
                             .setNegativeButton(R.string.webstorage_clear_data_dialog_cancel_button, null)
                             .setIcon(android.R.drawable.ic_dialog_alert)
                             .show();
                         break;
                     case Site.FEATURE_GEOLOCATION:
                         new AlertDialog.Builder(getContext())
                             .setTitle(R.string.geolocation_settings_page_dialog_title)
                             .setMessage(R.string.geolocation_settings_page_dialog_message)
                             .setPositiveButton(R.string.geolocation_settings_page_dialog_ok_button,
                                                new AlertDialog.OnClickListener() {
                                 public void onClick(DialogInterface dlg, int which) {
                                     GeolocationPermissions.getInstance().clear(mCurrentSite.getOrigin());
                                     mCurrentSite = null;
                                     populateOrigins();
                                     notifyDataSetChanged();
                                 }})
                             .setNegativeButton(R.string.geolocation_settings_page_dialog_cancel_button, null)
                             .setIcon(android.R.drawable.ic_dialog_alert)
                             .show();
                         break;
                 }
             } else {
                 mCurrentSite = (Site) view.getTag();
                 notifyDataSetChanged();
             }
         }
     }
 
     /**
      * Intercepts the back key to immediately notify
      * NativeDialog that we are done.
      */
     public boolean dispatchKeyEvent(KeyEvent event) {
         if ((event.getKeyCode() == KeyEvent.KEYCODE_BACK)
             && (event.getAction() == KeyEvent.ACTION_DOWN)) {
             if ((mAdapter != null) && (mAdapter.backKeyPressed())){
                 return true; // event consumed
             }
         }
         return super.dispatchKeyEvent(event);
     }
 
     @Override
     protected void onCreate(Bundle icicle) {
         super.onCreate(icicle);
         if (sMBStored == null) {
             sMBStored = getString(R.string.webstorage_origin_summary_mb_stored);
         }
         mAdapter = new SiteAdapter(this, R.layout.application);
         setListAdapter(mAdapter);
         getListView().setOnItemClickListener(mAdapter);
     }
 }
