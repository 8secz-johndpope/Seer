 /*
  * Copyright (C) 2010 The Android Open Source Project
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
 
 package com.android.gallery3d.app;
 
 import android.graphics.Bitmap;
 import android.graphics.Canvas;
 import android.graphics.LargeBitmap;
 import android.graphics.Bitmap.Config;
 import android.os.Bundle;
 import android.os.Message;
 import android.util.Log;
 
 import com.android.gallery3d.data.MediaItem;
 import com.android.gallery3d.data.MediaSet;
 import com.android.gallery3d.ui.GLCanvas;
 import com.android.gallery3d.ui.GLRoot;
 import com.android.gallery3d.ui.GLView;
 import com.android.gallery3d.ui.ImageViewer;
 import com.android.gallery3d.ui.SynchronizedHandler;
 import com.android.gallery3d.ui.ImageViewer.ImageData;
 import com.android.gallery3d.util.Future;
 import com.android.gallery3d.util.FutureListener;
 
 import java.util.ArrayList;
 
 public class PhotoPage extends ActivityState {
     private static final String TAG = PhotoPage.class.getSimpleName();
 
     public static final String KEY_SET_INDEX = "keySetIndex";
     public static final String KEY_PHOTO_INDEX = "keyPhotoIndex";
 
     private static final int MSG_UPDATE_SCREENNAIL = 1;
     private static final int MSG_UPDATE_FULLIMAGE = 2;
 
     private static final int TARGET_LENGTH = 1600;
 
     private SynchronizedHandler mHandler;
 
     private ImageViewer mImageViewer;
     private final MyImageViewerModel mModel = new MyImageViewerModel();
     private int mSetIndex;
     private int mPhotoIndex;
 
     private MediaSet mMediaSet;
 
     private GLView mRootPane = new GLView() {
 
         @Override
         protected void renderBackground(GLCanvas view) {
             view.clearBuffer();
         }
 
         @Override
         protected void onLayout(
                 boolean changed, int left, int top, int right, int bottom) {
             if (mImageViewer != null) {
                 mImageViewer.layout(0, 0, right - left, bottom - top);
             }
         }
     };
 
     @Override
     public void onCreate(Bundle data, Bundle restoreState) {
         mHandler = new SynchronizedHandler(mContext.getGLRoot()) {
             @Override
             public void handleMessage(Message message) {
                 switch (message.what) {
                     case MSG_UPDATE_SCREENNAIL: {
                         mModel.updateScreenNail(message.arg1, (Bitmap) message.obj);
                         break;
                     }
                     case MSG_UPDATE_FULLIMAGE: {
                         mModel.updateLargeImage(message.arg1, (LargeBitmap) message.obj);
                         break;
                     }
                     default: throw new AssertionError();
                 }
             }
         };
 
         mSetIndex = data.getInt(KEY_SET_INDEX);
         mPhotoIndex = data.getInt(KEY_PHOTO_INDEX);
 
         mMediaSet = mContext.getDataManager()
                 .getRootSet().getSubMediaSet(mSetIndex);
 
         mImageViewer = new ImageViewer(mContext);
         mImageViewer.setModel(mModel);
         mRootPane.addComponent(mImageViewer);
         mModel.requestNextImage();
     }
 
     @Override
     public void onPause() {
         GLRoot root = mContext.getGLRoot();
         root.lockRenderThread();
         try {
            mImageViewer.freeTextures();
         } finally {
             root.unlockRenderThread();
         }
     }
 
     @Override
     protected void onResume() {
         setContentPane(mRootPane);
        mImageViewer.prepareTextures();
     }
 
     private class MyImageViewerModel implements ImageViewer.Model {
 
         private LargeBitmap mLargeBitmap;
 
         private Bitmap mScreenNails[] = new Bitmap[3]; // prev, curr, next
 
         public LargeBitmap getLargeBitmap() {
             return mLargeBitmap;
         }
 
         public ImageData getImageData(int which) {
             Bitmap screennail = mScreenNails[which];
             if (screennail == null) return null;
 
             int width = 0;
             int height = 0;
 
             if (which == INDEX_CURRENT && mLargeBitmap != null) {
                 width = mLargeBitmap.getWidth();
                 height = mLargeBitmap.getHeight();
             } else {
                 // We cannot get the size of image before getting the
                 // full-size image. In the future, we should add the data to
                 // database or get it from the header in runtime. Now, we
                 // just use the thumb-nail image to estimate the size
                 float scale = (float) TARGET_LENGTH / Math.max(
                         screennail.getWidth(), screennail.getHeight());
                 width = Math.round(screennail.getWidth() * scale);
                 height = Math.round(screennail.getHeight() * scale);
             }
             return new ImageData(width, height, screennail);
         }
 
         public void next() {
             ++mPhotoIndex;
             Bitmap[] screenNails = mScreenNails;
 
             if (screenNails[INDEX_PREVIOUS] != null) {
                 screenNails[INDEX_PREVIOUS].recycle();
             }
             screenNails[INDEX_PREVIOUS] = screenNails[INDEX_CURRENT];
             screenNails[INDEX_CURRENT] = screenNails[INDEX_NEXT];
             screenNails[INDEX_NEXT] = null;
 
             if (mLargeBitmap != null) {
                 mLargeBitmap.recycle();
                 mLargeBitmap = null;
             }
 
             requestNextImage();
         }
 
         public void previous() {
             --mPhotoIndex;
             Bitmap[] screenNails = mScreenNails;
 
             if (screenNails[INDEX_NEXT] != null) {
                 screenNails[INDEX_NEXT].recycle();
             }
             screenNails[INDEX_NEXT] = screenNails[INDEX_CURRENT];
             screenNails[INDEX_CURRENT] = screenNails[INDEX_PREVIOUS];
             screenNails[INDEX_PREVIOUS] = null;
 
             if (mLargeBitmap != null) {
                 mLargeBitmap.recycle();
                 mLargeBitmap = null;
             }
 
             requestNextImage();
         }
 
         public void updateScreenNail(int index, Bitmap screenNail) {
             int offset = (index - mPhotoIndex) + 1;
             if (offset < 0 || offset > 2) {
                 screenNail.recycle();
                 return;
             }
 
             if (screenNail != null) {
                 mScreenNails[offset] = screenNail;
                 mImageViewer.notifyScreenNailInvalidated(offset);
             }
             requestNextImage();
         }
 
         public void updateLargeImage(int index, LargeBitmap largeBitmap) {
             int offset = (index - mPhotoIndex) + 1;
             if (offset != INDEX_CURRENT) {
                 largeBitmap.recycle();
                 return;
             }
 
             if (largeBitmap != null) {
                 mLargeBitmap = largeBitmap;
                 mImageViewer.notifyLargeBitmapInvalidated();
                 // We need to update the estimated width and height
                 mImageViewer.notifyScreenNailInvalidated(INDEX_CURRENT);
             }
             requestNextImage();
         }
 
         private MediaItem getMediaItem(
                 ArrayList<MediaItem> items, int start, int index) {
             index = index - start;
             if (index < 0 || index >= items.size()) return null;
             return items.get(index);
         }
 
         public void requestNextImage() {
 
             int start = Math.max(0, mPhotoIndex - 1);
 
             ArrayList<MediaItem> items = mMediaSet.getMediaItem(start, 3);
             if (items.size() == 0) return;
 
             // First request the current screen nail
             if (mScreenNails[INDEX_CURRENT] == null) {
                 MediaItem current = getMediaItem(items, start, mPhotoIndex);
                 if (current != null) {
                     current.requestImage(MediaItem.TYPE_THUMBNAIL,
                             new ScreenNailListener(mPhotoIndex));
                     return;
                 }
             }
 
             // Next, the next screen nail
             if (mScreenNails[INDEX_NEXT] == null) {
                 MediaItem next = getMediaItem(items, start, mPhotoIndex + 1);
                 if (next != null) {
                     next.requestImage(MediaItem.TYPE_THUMBNAIL,
                             new ScreenNailListener(mPhotoIndex + 1));
                     return;
                 }
             }
 
             // Next, the previous screen nail
             if (mScreenNails[INDEX_PREVIOUS] == null) {
                 MediaItem previous = getMediaItem(items, start, mPhotoIndex - 1);
                 if (previous != null) {
                     previous.requestImage(MediaItem.TYPE_THUMBNAIL,
                             new ScreenNailListener(mPhotoIndex - 1));
                     return;
                 }
             }
 
             // Next, the full size image
             if (mLargeBitmap == null) {
                 MediaItem current = getMediaItem(items, start, mPhotoIndex);
                 if (current != null) {
                     current.requestLargeImage(MediaItem.TYPE_FULL_IMAGE,
                             new LargeImageListener(mPhotoIndex));
                     return;
                 }
             }
         }
     }
 
     private static Bitmap[] getScaledBitmaps(Bitmap bitmap, int minLength) {
         Config config = bitmap.hasAlpha()
                 ? Config.ARGB_8888 : Config.RGB_565;
 
         int width = bitmap.getWidth() / 2;
         int height = bitmap.getHeight() / 2;
 
         ArrayList<Bitmap> list = new ArrayList<Bitmap>();
         list.add(bitmap);
         while (width > minLength || height > minLength) {
             Bitmap half = Bitmap.createBitmap(width, height, config);
             Canvas canvas = new Canvas(half);
             canvas.scale(0.5f, 0.5f);
             canvas.drawBitmap(bitmap, 0, 0, null);
             width /= 2;
             height /= 2;
             bitmap = half;
             list.add(bitmap);
         }
         return list.toArray(new Bitmap[list.size()]);
     }
 
     private class ScreenNailListener implements FutureListener<Bitmap> {
 
         private final int mIndex;
 
         public ScreenNailListener(int index) {
             mIndex = index;
         }
 
         public void onFutureDone(Future<? extends Bitmap> future) {
             Bitmap bitmap = null;
             try {
                 bitmap = future.get();
             } catch (Exception e) {
                 Log.v(TAG, "fail to get image", e);
             }
             mHandler.sendMessage(mHandler.obtainMessage(
                     MSG_UPDATE_SCREENNAIL, mIndex, 0, bitmap));
         }
     }
 
     private class LargeImageListener implements FutureListener<LargeBitmap> {
 
         private final int mIndex;
 
         public LargeImageListener(int index) {
             mIndex = index;
         }
 
         public void onFutureDone(Future<? extends LargeBitmap> future) {
             LargeBitmap largeBitmap = null;
             try {
                 largeBitmap = future.get();
             } catch (Exception e) {
                 Log.v(TAG, "fail to get image", e);
             }
             mHandler.sendMessage(mHandler.obtainMessage(
                     MSG_UPDATE_FULLIMAGE, mIndex, 0, largeBitmap));
         }
     }
 }
