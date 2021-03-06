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
 
 package com.android.camera.gallery;
 
 import com.android.camera.BitmapManager;
 import com.android.camera.Util;
 
 import android.content.ContentResolver;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.ParcelFileDescriptor;
 import android.util.Log;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.InputStream;
 
 class UriImage implements IImage {
     private static final String TAG = "UriImage";
     private final Uri mUri;
     private final BaseImageList mContainer;
     private final ContentResolver mContentResolver;
 
     UriImage(BaseImageList container, ContentResolver cr, Uri uri) {
         mContainer = container;
         mContentResolver = cr;
         mUri = uri;
     }
 
     public String getDataPath() {
         return mUri.getPath();
     }
 
     private InputStream getInputStream() {
         try {
             if (mUri.getScheme().equals("file")) {
                 String path = mUri.getPath();
                 return new java.io.FileInputStream(mUri.getPath());
             } else {
                 return mContentResolver.openInputStream(mUri);
             }
         } catch (FileNotFoundException ex) {
             return null;
         }
     }
 
     private ParcelFileDescriptor getPFD() {
         try {
             if (mUri.getScheme().equals("file")) {
                 String path = mUri.getPath();
                 return ParcelFileDescriptor.open(new File(path),
                         ParcelFileDescriptor.MODE_READ_ONLY);
             } else {
                 return mContentResolver.openFileDescriptor(mUri, "r");
             }
         } catch (FileNotFoundException ex) {
             return null;
         }
     }
 
     public Bitmap fullSizeBitmap(int targetWidthHeight) {
         try {
             ParcelFileDescriptor pfdInput = getPFD();
             BitmapFactory.Options options = new BitmapFactory.Options();
             options.inJustDecodeBounds = true;
             BitmapManager.instance().decodeFileDescriptor(
                     pfdInput.getFileDescriptor(), options);
 
             if (targetWidthHeight != -1) {
                 options.inSampleSize =
                         Util.computeSampleSize(options, targetWidthHeight);
             }
 
             options.inJustDecodeBounds = false;
             options.inDither = false;
             options.inPreferredConfig = Bitmap.Config.ARGB_8888;
 
             Bitmap b = BitmapManager.instance().decodeFileDescriptor(
                     pfdInput.getFileDescriptor(), options);
             pfdInput.close();
             return b;
         } catch (Exception ex) {
             Log.e(TAG, "got exception decoding bitmap " + ex.toString());
             return null;
         }
     }
 
     final class LoadBitmapCancelable extends BaseCancelable<Bitmap> {
         ParcelFileDescriptor mPfdInput;
         BitmapFactory.Options mOptions = new BitmapFactory.Options();
         int mTargetWidthOrHeight;
 
         public LoadBitmapCancelable(
                 ParcelFileDescriptor pfd, int targetWidthOrHeight) {
             mPfdInput = pfd;
             mTargetWidthOrHeight = targetWidthOrHeight;
         }
 
         @Override
         public boolean requestCancel() {
             if (super.requestCancel()) {
                 mOptions.requestCancelDecode();
                 return true;
             }
             return false;
         }
 
         @Override
         protected Bitmap execute() {
             try {
                 Bitmap b = Util.makeBitmap(mTargetWidthOrHeight,
                         fullSizeImageUri(), mContentResolver, mPfdInput,
                         mOptions);
                 return b;
             } catch (Exception ex) {
                 return null;
             }
         }
     }
 
     public Cancelable<Bitmap> fullSizeBitmapCancelable(
             int targetWidthOrHeight) {
         try {
             ParcelFileDescriptor pfdInput = getPFD();
             if (pfdInput == null) return null;
             return new LoadBitmapCancelable(pfdInput, targetWidthOrHeight);
         } catch (UnsupportedOperationException ex) {
             return null;
         }
     }
 
     public Uri fullSizeImageUri() {
         return mUri;
     }
 
     public InputStream fullSizeImageData() {
         return getInputStream();
     }
 
     public Bitmap miniThumbBitmap() {
         return thumbBitmap();
     }
 
     public String getTitle() {
         return mUri.toString();
     }
 
     public String getDisplayName() {
         return getTitle();
     }
 
     public Bitmap thumbBitmap() {
         return fullSizeBitmap(THUMBNAIL_TARGET_SIZE);
     }
 
     private BitmapFactory.Options snifBitmapOptions() {
         ParcelFileDescriptor input = getPFD();
         if (input == null) return null;
         try {
             Uri uri = fullSizeImageUri();
             BitmapFactory.Options options = new BitmapFactory.Options();
             options.inJustDecodeBounds = true;
             BitmapManager.instance().decodeFileDescriptor(
                     input.getFileDescriptor(), options);
             return options;
         } finally {
             Util.closeSiliently(input);
         }
     }
 
     public String getMimeType() {
         BitmapFactory.Options options = snifBitmapOptions();
         return (options != null) ? options.outMimeType : "";
     }
 
     public int getHeight() {
         BitmapFactory.Options options = snifBitmapOptions();
         return (options != null) ? options.outHeight : 0;
     }
 
     public int getWidth() {
         BitmapFactory.Options options = snifBitmapOptions();
         return (options != null) ? options.outWidth : 0;
     }
 
     public long fullSizeImageId() {
         return 0;
     }
 
     public IImageList getContainer() {
         return mContainer;
     }
 
     public long getDateTaken() {
         return 0;
     }
 
     public int getRow() {
        return 0;
     }
 
     public boolean isReadonly() {
         return true;
     }
 
     public boolean isDrm() {
         return false;
     }
 
     public void onRemove() {
         throw new UnsupportedOperationException();
     }
 
     public boolean rotateImageBy(int degrees) {
         return false;
     }
 
     public void setTitle(String name) {
         throw new UnsupportedOperationException();
     }
 
     public Uri thumbUri() {
         throw new UnsupportedOperationException();
     }
 }
