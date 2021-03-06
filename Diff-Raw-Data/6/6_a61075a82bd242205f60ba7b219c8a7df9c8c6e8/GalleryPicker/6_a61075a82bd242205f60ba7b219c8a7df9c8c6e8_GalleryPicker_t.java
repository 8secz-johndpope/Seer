 /*
  * Copyright (C) 2007 The Android Open Source Project
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
 
 package com.android.camera;
 
 import com.android.camera.gallery.IImage;
 import com.android.camera.gallery.IImageList;
 
 import android.app.Activity;
 import android.app.Dialog;
 import android.app.ProgressDialog;
 import android.content.BroadcastReceiver;
 import android.content.ContentResolver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.res.Resources;
 import android.graphics.Bitmap;
 import android.graphics.Canvas;
 import android.graphics.Matrix;
 import android.graphics.Paint;
 import android.graphics.PorterDuff;
 import android.graphics.PorterDuffXfermode;
 import android.graphics.Rect;
 import android.graphics.drawable.Drawable;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.StatFs;
 import android.provider.MediaStore.Images;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MenuItem.OnMenuItemClickListener;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.BaseAdapter;
 import android.widget.GridView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * The GalleryPicker activity.
  */
 public class GalleryPicker extends Activity {
     private static final String TAG = "GalleryPicker";
 
     Handler mHandler = new Handler();  // handler for the main thread
     Thread mWorkerThread;
     BroadcastReceiver mReceiver;
     GridView mGridView;
     GalleryPickerAdapter mAdapter;  // mAdapter is only accessed in main thread.
     boolean mScanning;
     boolean mUnmounted;
 
     @Override
     public void onCreate(Bundle icicle) {
         super.onCreate(icicle);
 
         setContentView(R.layout.gallerypicker);
 
         mGridView = (GridView) findViewById(R.id.albums);
 
         mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
             public void onItemClick(AdapterView<?> parent, View view,
                                     int position, long id) {
                 launchFolderGallery(position);
             }
         });
 
         mGridView.setOnCreateContextMenuListener(
                 new View.OnCreateContextMenuListener() {
                     public void onCreateContextMenu(ContextMenu menu, View v,
                         final ContextMenuInfo menuInfo) {
                             onCreateGalleryPickerContextMenu(menu, menuInfo);
                     }
                 });
 
         mReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 onReceiveMediaBroadcast(intent);
             }
         };
     }
 
     Dialog mMediaScanningDialog;
 
     // Display a dialog if the storage is being scanned now.
     public void updateScanningDialog(boolean scanning) {
         boolean prevScanning = (mMediaScanningDialog != null);
         if (prevScanning == scanning) return;
         // Now we are certain the state is changed.
         if (prevScanning) {
             mMediaScanningDialog.cancel();
             mMediaScanningDialog = null;
         } else {
             mMediaScanningDialog = ProgressDialog.show(
                     this,
                     null,
                     getResources().getString(R.string.wait),
                     true,
                     true);
         }
     }
 
     private View mNoImagesView;
 
     // Show/Hide the "no images" icon and text. Load resources on demand.
     private void showNoImagesView() {
         if (mNoImagesView == null) {
             ViewGroup root  = (ViewGroup) findViewById(R.id.root);
             getLayoutInflater().inflate(R.layout.gallerypicker_no_images, root);
             mNoImagesView = findViewById(R.id.no_images);
         }
         mNoImagesView.setVisibility(View.VISIBLE);
     }
 
     private void hideNoImagesView() {
         if (mNoImagesView != null) {
             mNoImagesView.setVisibility(View.GONE);
         }
     }
 
     // The storage status is changed, restart the worker or show "no images".
     private void rebake(boolean unmounted, boolean scanning) {
         if (unmounted == mUnmounted && scanning == mScanning) return;
         abortWorker();
         mUnmounted = unmounted;
         mScanning = scanning;
         updateScanningDialog(mScanning);
         if (mUnmounted) {
             showNoImagesView();
         } else {
             hideNoImagesView();
             startWorker();
         }
     }
 
     // This is called when we receive media-related broadcast.
     private void onReceiveMediaBroadcast(Intent intent) {
         String action = intent.getAction();
         if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
             // SD card available
             // TODO put up a "please wait" message
         } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
             // SD card unavailable
             Toast.makeText(GalleryPicker.this,
                     getResources().getString(R.string.wait), 5000);
             rebake(true, false);
         } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
             Toast.makeText(GalleryPicker.this,
                     getResources().getString(R.string.wait), 5000);
             rebake(false, true);
         } else if (action.equals(
                 Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
             rebake(false, false);
         } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
             rebake(true, false);
         }
     }
 
     private void launchFolderGallery(int position) {
         mAdapter.mItems.get(position).launch(this);
     }
 
     private void onCreateGalleryPickerContextMenu(ContextMenu menu,
             final ContextMenuInfo menuInfo) {
         int position = ((AdapterContextMenuInfo) menuInfo).position;
         menu.setHeaderTitle(mAdapter.baseTitleForPosition(position));
         // "Slide Show"
         if ((mAdapter.getIncludeMediaTypes(position)
                 & ImageManager.INCLUDE_IMAGES) != 0) {
             menu.add(0, 207, 0, R.string.slide_show)
                     .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                         public boolean onMenuItemClick(MenuItem item) {
                             return onSlideShowClicked(menuInfo);
                         }
                     });
         }
         // "View"
         menu.add(0, 208, 0, R.string.view)
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                     public boolean onMenuItemClick(MenuItem item) {
                             return onViewClicked(menuInfo);
                     }
                 });
     }
 
     // This is called when the user clicks "Slideshow" from the context menu.
     private boolean onSlideShowClicked(ContextMenuInfo menuInfo) {
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
         int position = info.position;
 
         if (position < 0 || position >= mAdapter.mItems.size()) {
             return true;
         }
         // Slide show starts from the first image on the list.
         Item item = mAdapter.mItems.get(position);
         Uri targetUri = item.mFirstImageUri;
 
         if (targetUri != null && item.mBucketId != null) {
             targetUri = targetUri.buildUpon()
                     .appendQueryParameter("bucketId", item.mBucketId)
                     .build();
         }
         Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
         intent.putExtra("slideshow", true);
         startActivity(intent);
         return true;
     }
 
     // This is called when the user clicks "View" from the context menu.
     private boolean onViewClicked(ContextMenuInfo menuInfo) {
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
         launchFolderGallery(info.position);
         return true;
     }
 
     @Override
     public void onStop() {
         super.onStop();
 
         abortWorker();
 
         unregisterReceiver(mReceiver);
 
         // free up some ram
         mAdapter = null;
         mGridView.setAdapter(null);
         unloadDrawable();
     }
 
     @Override
     public void onStart() {
         super.onStart();
 
         mAdapter = new GalleryPickerAdapter(getLayoutInflater());
         mGridView.setAdapter(mAdapter);
 
         // install an intent filter to receive SD card related events.
         IntentFilter intentFilter = new IntentFilter();
         intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
         intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
         intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
         intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
         intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
         intentFilter.addDataScheme("file");
 
         registerReceiver(mReceiver, intentFilter);
 
         // Assume the storage is mounted and not scanning.
         mUnmounted = false;
         mScanning = false;
         startWorker();
     }
 
     // This is used to stop the worker thread.
     volatile boolean mAbort = false;
 
     // Create the worker thread.
     private void startWorker() {
         mAbort = false;
         mWorkerThread = new Thread("GalleryPicker Worker") {
                     public void run() {
                         workerRun();
                     }
                 };
         mWorkerThread.start();
     }
 
     private void abortWorker() {
         if (mWorkerThread != null) {
             mAbort = true;
             try {
                 mWorkerThread.join();
             } catch (InterruptedException ex) {
                 Log.e(TAG, "join interrupted");
             }
             mWorkerThread = null;
            // Remove all runnables in mHandler.
            // (We assume that the "what" field in the messages are 0
            // for runnables).
            mHandler.removeMessages(0);
             mAdapter.clear();
             mAdapter.updateDisplay();
             clearImageLists();
         }
     }
 
     // This is run in the worker thread.
     private void workerRun() {
         checkScanning();
         if (mAbort) return;
 
         checkImageList();
         if (mAbort) return;
 
         checkBucketIds();
         if (mAbort) return;
 
         checkLowStorage();
         if (mAbort) return;
     }
 
     // This is run in the worker thread.
     private void checkScanning() {
         ContentResolver cr = getContentResolver();
         final boolean scanning =
                 ImageManager.isMediaScannerScanning(cr);
         mHandler.post(new Runnable() {
                     public void run() {
                         checkScanningFinished(scanning);
                     }
                 });
     }
 
     // This is run in the main thread.
     private void checkScanningFinished(boolean scanning) {
         updateScanningDialog(scanning);
     }
 
     // This is run in the worker thread.
     private void checkImageList() {
         IImageList[] lists = new IImageList[4];
         for (int i = 0; i < 4; i++) {
             ImageListData data = IMAGE_LIST_DATA[i];
             lists[i] = createImageList(data.mInclude, data.mBucketId,
                     getContentResolver());
             if (mAbort) return;
             Item item = null;
 
             if (lists[i].isEmpty()) continue;
 
             // i >= 2 means we are looking at All Images/All Videos.
             // lists[i-2] is the corresponding Camera Images/Camera Videos.
             // We want to add the "All" list only if it's different from
             // the "Camera" list.
             if (i >= 2 && lists[i].getCount() == lists[i - 2].getCount()) {
                 continue;
             }
 
             item = new Item(data.mType,
                             data.mBucketId,
                             getResources().getString(data.mStringId),
                             lists[i]);
 
             final Bitmap b = makeMiniThumbBitmap(142, 142, lists[i]);
             if (mAbort) {
                 if (b != null) b.recycle();
                 return;
             }
             item.setThumbBitmap(b);
 
             final Item finalItem = item;
             mHandler.post(new Runnable() {
                         public void run() {
                             checkImageListFinished(finalItem);
                         }
                     });
         }
     }
 
     // This is run in the main thread.
     private void checkImageListFinished(Item item) {
         if (item != null) {
             mAdapter.addItem(item);
             mAdapter.updateDisplay();
         }
     }
 
     private static final String CAMERA_BUCKET =
             ImageManager.CAMERA_IMAGE_BUCKET_ID;
 
     // This is run in the worker thread.
     private void checkBucketIds() {
         final IImageList allImages;
         if (!mScanning && !mUnmounted) {
             allImages = ImageManager.allImages(
                     getContentResolver(),
                     ImageManager.DataLocation.ALL,
                     ImageManager.INCLUDE_IMAGES
                     | ImageManager.INCLUDE_VIDEOS,
                     ImageManager.SORT_DESCENDING);
         } else {
             allImages = ImageManager.emptyImageList();
         }
 
         if (mAbort) {
             allImages.deactivate();
             return;
         }
 
         HashMap<String, String> hashMap = allImages.getBucketIds();
         allImages.deactivate();
         if (mAbort) return;
 
         final ArrayList<Item> items = new ArrayList<Item>();
 
         for (Map.Entry<String, String> entry : hashMap.entrySet()) {
             String key = entry.getKey();
             if (key == null) {
                 continue;
             }
             if (!key.equals(CAMERA_BUCKET)) {
                 IImageList list = createImageList(
                         ImageManager.INCLUDE_IMAGES
                         | ImageManager.INCLUDE_VIDEOS, key,
                         getContentResolver());
                 if (mAbort) return;
 
                 Item item = new Item(Item.TYPE_NORMAL_FOLDERS, key,
                         entry.getValue(), list);
 
                 final Bitmap b = makeMiniThumbBitmap(142, 142, list);
                 if (mAbort) {
                     if (b != null) b.recycle();
                     return;
                 }
                 item.setThumbBitmap(b);
 
                 items.add(item);
             }
         }
 
         mHandler.post(new Runnable() {
                     public void run() {
                         checkBucketIdsFinished(items);
                     }
                 });
     }
 
     // This is run in the main thread.
     private void checkBucketIdsFinished(ArrayList<Item> items) {
         for (Item item : items) {
             mAdapter.addItem(item);
         }
         mAdapter.updateDisplay();
 
         // This is put here just for convenience.
         ImageManager.ensureOSXCompatibleFolder();
 
         // If we just have one folder, open it.
         // If we have zero folder, show the "no images" icon.
         if (!mScanning) {
             int numItems = mAdapter.mItems.size();
             if (numItems == 0) {
                 showNoImagesView();
             } else if (numItems == 1) {
                 mAdapter.mItems.get(0).launch(this);
                 finish();
                 return;
             }
         }
     }
 
     private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 2;
 
     // This is run in the worker thread.
     private void checkLowStorage() {
         // Check available space only if we are writable
         if (ImageManager.hasStorage()) {
             String storageDirectory = Environment
                     .getExternalStorageDirectory().toString();
             StatFs stat = new StatFs(storageDirectory);
             long remaining = (long) stat.getAvailableBlocks()
                     * (long) stat.getBlockSize();
             if (remaining < LOW_STORAGE_THRESHOLD) {
                 mHandler.post(new Runnable() {
                     public void run() {
                         checkLowStorageFinished();
                     }
                 });
             }
         }
     }
 
     // This is run in the main thread.
     // This is called only if the storage is low.
     private void checkLowStorageFinished() {
         Toast.makeText(GalleryPicker.this, R.string.not_enough_space, 5000)
                 .show();
     }
 
     // IMAGE_LIST_DATA stores the parameters for the four image lists
     // we are interested in. The order of the IMAGE_LIST_DATA array is
     // significant (See the implementation of GalleryPickerAdapter.init).
     private static final class ImageListData {
         ImageListData(int type, int include, String bucketId, int stringId) {
             mType = type;
             mInclude = include;
             mBucketId = bucketId;
             mStringId = stringId;
         }
         int mType;
         int mInclude;
         String mBucketId;
         int mStringId;
     }
 
     private static final ImageListData[] IMAGE_LIST_DATA = {
         // Camera Images
         new ImageListData(Item.TYPE_CAMERA_IMAGES,
                           ImageManager.INCLUDE_IMAGES,
                           ImageManager.CAMERA_IMAGE_BUCKET_ID,
                           R.string.gallery_camera_bucket_name),
         // Camera Videos
         new ImageListData(Item.TYPE_CAMERA_VIDEOS,
                           ImageManager.INCLUDE_VIDEOS,
                           ImageManager.CAMERA_IMAGE_BUCKET_ID,
                           R.string.gallery_camera_videos_bucket_name),
         // All Images
         new ImageListData(Item.TYPE_ALL_IMAGES,
                           ImageManager.INCLUDE_IMAGES,
                           null,
                           R.string.all_images),
 
         // All Videos
         new ImageListData(Item.TYPE_ALL_VIDEOS,
                           ImageManager.INCLUDE_VIDEOS,
                           null,
                           R.string.all_videos),
     };
 
 
     // These drawables are loaded on-demand.
     Drawable mFrameGalleryMask;
     Drawable mCellOutline;
     Drawable mVideoOverlay;
 
     private void loadDrawableIfNeeded() {
         if (mFrameGalleryMask != null) return;  // already loaded
         Resources r = getResources();
         mFrameGalleryMask = r.getDrawable(
                 R.drawable.frame_gallery_preview_album_mask);
         mCellOutline = r.getDrawable(android.R.drawable.gallery_thumb);
         mVideoOverlay = r.getDrawable(R.drawable.ic_gallery_video_overlay);
     }
 
     private void unloadDrawable() {
         mFrameGalleryMask = null;
         mCellOutline = null;
         mVideoOverlay = null;
     }
 
     private static void placeImage(Bitmap image, Canvas c, Paint paint,
             int imageWidth, int widthPadding, int imageHeight,
             int heightPadding, int offsetX, int offsetY,
             int pos) {
         int row = pos / 2;
         int col = pos - (row * 2);
 
         int xPos = (col * (imageWidth + widthPadding)) - offsetX;
         int yPos = (row * (imageHeight + heightPadding)) - offsetY;
 
         c.drawBitmap(image, xPos, yPos, paint);
     }
 
     // This is run in worker thread.
     private Bitmap makeMiniThumbBitmap(int width, int height,
             IImageList images) {
         int count = images.getCount();
         // We draw three different version of the folder image depending on the
         // number of images in the folder.
         //    For a single image, that image draws over the whole folder.
         //    For two or three images, we draw the two most recent photos.
         //    For four or more images, we draw four photos.
         final int padding = 4;
         int imageWidth = width;
         int imageHeight = height;
         int offsetWidth = 0;
         int offsetHeight = 0;
 
         imageWidth = (imageWidth - padding) / 2;  // 2 here because we show two
                                                   // images
         imageHeight = (imageHeight - padding) / 2;  // per row and column
 
         final Paint p = new Paint();
         final Bitmap b = Bitmap.createBitmap(width, height,
                 Bitmap.Config.ARGB_8888);
         final Canvas c = new Canvas(b);
         final Matrix m = new Matrix();
 
         // draw the whole canvas as transparent
         p.setColor(0x00000000);
         c.drawPaint(p);
 
         // load the drawables
         loadDrawableIfNeeded();
 
         // draw the mask normally
         p.setColor(0xFFFFFFFF);
         mFrameGalleryMask.setBounds(0, 0, width, height);
         mFrameGalleryMask.draw(c);
 
         Paint pdpaint = new Paint();
         pdpaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
 
         pdpaint.setStyle(Paint.Style.FILL);
         c.drawRect(0, 0, width, height, pdpaint);
 
         for (int i = 0; i < 4; i++) {
             if (mAbort) {
                 return null;
             }
 
             Bitmap temp = null;
             IImage image = i < count ? images.getImageAt(i) : null;
 
             if (image != null) {
                 temp = image.miniThumbBitmap();
             }
 
             if (temp != null) {
                 if (ImageManager.isVideo(image)) {
                     Bitmap newMap = temp.copy(temp.getConfig(), true);
                     Canvas overlayCanvas = new Canvas(newMap);
                     int overlayWidth = mVideoOverlay.getIntrinsicWidth();
                     int overlayHeight = mVideoOverlay.getIntrinsicHeight();
                     int left = (newMap.getWidth() - overlayWidth) / 2;
                     int top = (newMap.getHeight() - overlayHeight) / 2;
                     Rect newBounds = new Rect(left, top, left + overlayWidth,
                             top + overlayHeight);
                     mVideoOverlay.setBounds(newBounds);
                     mVideoOverlay.draw(overlayCanvas);
                     temp.recycle();
                     temp = newMap;
                 }
 
                 Bitmap temp2 = Util.transform(m, temp, imageWidth,
                                               imageHeight, true);
                 if (temp2 != temp) {
                     temp.recycle();
                 }
                 temp = temp2;
             }
 
             Bitmap thumb = Bitmap.createBitmap(imageWidth, imageHeight,
                                                Bitmap.Config.ARGB_8888);
             Canvas tempCanvas = new Canvas(thumb);
             if (temp != null) {
                 tempCanvas.drawBitmap(temp, new Matrix(), new Paint());
             }
             mCellOutline.setBounds(0, 0, imageWidth, imageHeight);
             mCellOutline.draw(tempCanvas);
 
             placeImage(thumb, c, pdpaint, imageWidth, padding, imageHeight,
                        padding, offsetWidth, offsetHeight, i);
 
             thumb.recycle();
 
             if (temp != null) {
                 temp.recycle();
             }
         }
 
         return b;
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
 
         MenuHelper.addCaptureMenuItems(menu, this);
 
         menu.add(0, 0, 5, R.string.camerasettings)
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                     public boolean onMenuItemClick(MenuItem item) {
                         Intent preferences = new Intent();
                         preferences.setClass(GalleryPicker.this,
                                              GallerySettings.class);
                         startActivity(preferences);
                         return true;
                     }
                 })
                 .setAlphabeticShortcut('p')
                 .setIcon(android.R.drawable.ic_menu_preferences);
 
         return true;
     }
 
     // image lists created by createImageList() are collected in mAllLists.
     // They will be deactivated in clearImageList, so they don't hold open files
     // on SD card. We will be killed if we don't close files when the SD card
     // is unmounted.
     ArrayList<IImageList> mAllLists = new ArrayList<IImageList>();
 
     private IImageList createImageList(int mediaTypes, String bucketId,
             ContentResolver cr) {
         IImageList list = ImageManager.allImages(
                 cr,
                 ImageManager.DataLocation.ALL,
                 mediaTypes,
                 ImageManager.SORT_DESCENDING,
                 bucketId);
         mAllLists.add(list);
         return list;
     }
 
     private void clearImageLists() {
         for (IImageList list : mAllLists) {
             list.deactivate();
         }
         mAllLists.clear();
     }
 }
 
 // Item is the underlying data for GalleryPickerAdapter.
 // It is passed from the activity to the adapter.
 class Item {
     public static final int TYPE_NONE = -1;
     public static final int TYPE_ALL_IMAGES = 0;
     public static final int TYPE_ALL_VIDEOS = 1;
     public static final int TYPE_CAMERA_IMAGES = 2;
     public static final int TYPE_CAMERA_VIDEOS = 3;
     public static final int TYPE_NORMAL_FOLDERS = 4;
 
     public final int mType;
     public final String mBucketId;
     public final String mName;
     public final IImageList mImageList;
     public final int mCount;
     public final Uri mFirstImageUri;  // could be null if the list is empty
 
     // The thumbnail bitmap is set by setThumbBitmap() later because we want
     // to let the user sees the folder icon as soon as possible (and possibly
     // select them), then present more detailed information when we have it.
     public Bitmap mThumbBitmap;  // the thumbnail bitmap for the image list
 
     public Item(int type, String bucketId, String name, IImageList list) {
         mType = type;
         mBucketId = bucketId;
         mName = name;
         mImageList = list;
         mCount = list.getCount();
         if (mCount > 0) {
             mFirstImageUri = list.getImageAt(0).fullSizeImageUri();
         } else {
             mFirstImageUri = null;
         }
     }
 
     public void setThumbBitmap(Bitmap thumbBitmap) {
         mThumbBitmap = thumbBitmap;
     }
 
     public boolean needsBucketId() {
         return mType >= TYPE_CAMERA_IMAGES;
     }
 
     public void launch(Activity activity) {
         Uri uri = Images.Media.INTERNAL_CONTENT_URI;
         if (needsBucketId()) {
             uri = uri.buildUpon()
                     .appendQueryParameter("bucketId", mBucketId).build();
         }
         Intent intent = new Intent(Intent.ACTION_VIEW, uri);
         intent.putExtra("windowTitle", mName);
         intent.putExtra("mediaTypes", getIncludeMediaTypes());
         activity.startActivity(intent);
     }
 
     public int getIncludeMediaTypes() {
         return convertItemTypeToIncludedMediaType(mType);
     }
 
     public static int convertItemTypeToIncludedMediaType(int itemType) {
         switch (itemType) {
         case TYPE_ALL_IMAGES:
         case TYPE_CAMERA_IMAGES:
             return ImageManager.INCLUDE_IMAGES;
         case TYPE_ALL_VIDEOS:
         case TYPE_CAMERA_VIDEOS:
             return ImageManager.INCLUDE_VIDEOS;
         case TYPE_NORMAL_FOLDERS:
         default:
             return ImageManager.INCLUDE_IMAGES
                     | ImageManager.INCLUDE_VIDEOS;
         }
     }
 
     public int getOverlay() {
         switch (mType) {
             case TYPE_ALL_IMAGES:
             case TYPE_CAMERA_IMAGES:
                 return R.drawable.frame_overlay_gallery_camera;
             case TYPE_ALL_VIDEOS:
             case TYPE_CAMERA_VIDEOS:
                 return R.drawable.frame_overlay_gallery_video;
             case TYPE_NORMAL_FOLDERS:
             default:
                 return R.drawable.frame_overlay_gallery_folder;
         }
     }
 }
 
 class GalleryPickerAdapter extends BaseAdapter {
     ArrayList<Item> mItems = new ArrayList<Item>();
     LayoutInflater mInflater;
 
     GalleryPickerAdapter(LayoutInflater inflater) {
         mInflater = inflater;
     }
 
     public void addItem(Item item) {
         mItems.add(item);
     }
 
     public void updateDisplay() {
         notifyDataSetChanged();
     }
 
     public void clear() {
         mItems.clear();
     }
 
     public int getCount() {
         return mItems.size();
     }
 
     public Object getItem(int position) {
         return null;
     }
 
     public long getItemId(int position) {
         return position;
     }
 
     public String baseTitleForPosition(int position) {
         return mItems.get(position).mName;
     }
 
     public int getIncludeMediaTypes(int position) {
         return mItems.get(position).getIncludeMediaTypes();
     }
 
     public View getView(final int position, View convertView,
                         ViewGroup parent) {
         View v;
 
         if (convertView == null) {
             v = mInflater.inflate(R.layout.gallery_picker_item, null);
         } else {
             v = convertView;
         }
 
         TextView titleView = (TextView) v.findViewById(R.id.title);
 
         GalleryPickerItem iv =
                 (GalleryPickerItem) v.findViewById(R.id.thumbnail);
         Item item = mItems.get(position);
         iv.setOverlay(item.getOverlay());
         if (item.mThumbBitmap != null) {
             iv.setImageBitmap(item.mThumbBitmap);
             String title = item.mName + " (" + item.mCount + ")";
             titleView.setText(title);
         } else {
             iv.setImageResource(android.R.color.transparent);
             titleView.setText(item.mName);
         }
 
         return v;
     }
 }
