 /*
  * Copyright (C) 2012 Brian Muramatsu
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
 
 package com.btmura.android.reddit.widget;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.ref.WeakReference;
 import java.net.HttpURLConnection;
 import java.net.MalformedURLException;
 import java.net.URL;
 
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.os.AsyncTask;
 import android.text.TextUtils;
 import android.util.Log;
 import android.util.LruCache;
 
 public class ThumbnailLoader {
 
     private static final String TAG = "ThumbnailLoader";
 
     private static final BitmapCache BITMAP_CACHE = new BitmapCache(2 * 1024 * 1024);
 
     public void setThumbnail(Context context, ThingView v, String url) {
         if (!TextUtils.isEmpty(url)) {
             Bitmap b = BITMAP_CACHE.get(url);
             v.setThumbnailBitmap(b);
            if (b == null) {
                 LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
                 if (task == null || !url.equals(task.url)) {
                     if (task != null) {
                         task.cancel(true);
                     }
                     task = new LoadThumbnailTask(v, url);
                     v.setTag(task);
                     task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
             }
         } else {
            LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
            if (task != null) {
                task.cancel(true);
                v.setTag(null);
            }
             v.setThumbnailBitmap(null);
         }
     }
 
     static class BitmapCache extends LruCache<String, Bitmap> {
         BitmapCache(int size) {
             super(size);
         }
 
         @Override
         protected int sizeOf(String key, Bitmap value) {
             return value.getByteCount();
         }
     }
 
     class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
 
         private final WeakReference<ThingView> ref;
         private final String url;
 
         LoadThumbnailTask(ThingView v, String url) {
             this.ref = new WeakReference<ThingView>(v);
             this.url = url;
         }
 
         @Override
         protected Bitmap doInBackground(Void... params) {
             HttpURLConnection conn = null;
             try {
                 URL u = new URL(url);
                 conn = (HttpURLConnection) u.openConnection();
                 if (isCancelled()) {
                     return null;
                 }
 
                 InputStream is = conn.getInputStream();
                 if (isCancelled()) {
                     return null;
                 }
 
                 return BitmapFactory.decodeStream(is);
 
             } catch (MalformedURLException e) {
                 Log.e(TAG, e.getMessage(), e);
             } catch (IOException e) {
                 Log.e(TAG, e.getMessage(), e);
             } finally {
                 if (conn != null) {
                     conn.disconnect();
                 }
             }
             return null;
         }
 
         @Override
         protected void onPostExecute(Bitmap b) {
             if (b != null) {
                 BITMAP_CACHE.put(url, b);
             }
             ThingView v = ref.get();
             if (v != null && equals(v.getTag())) {
                 if (b != null) {
                     v.setThumbnailBitmap(b);
                 }
                 v.setTag(null);
             }
         }
     }
 }
