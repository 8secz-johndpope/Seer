 /*
  * Copyright (C) 2012 The Android Open Source Project
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
 
 package com.android.gallery3d.filtershow.filters;
 
 import android.graphics.Bitmap;
 import android.graphics.Canvas;
 import android.graphics.RectF;
 
 import com.adobe.xmp.XMPException;
 import com.adobe.xmp.XMPMeta;
 import com.android.gallery3d.filtershow.presets.ImagePreset;
 
 /**
  * An image filter which creates a tiny planet projection.
  */
 public class ImageFilterTinyPlanet extends ImageFilter {
     private float mAngle = 0;
 
     private static final String TAG = ImageFilterTinyPlanet.class.getSimpleName();
     public static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/panorama/";
 
     public static final String CROPPED_AREA_IMAGE_WIDTH_PIXELS =
             "CroppedAreaImageWidthPixels";
     public static final String CROPPED_AREA_IMAGE_HEIGHT_PIXELS =
             "CroppedAreaImageHeightPixels";
     public static final String CROPPED_AREA_FULL_PANO_WIDTH_PIXELS =
             "FullPanoWidthPixels";
     public static final String CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS =
             "FullPanoHeightPixels";
     public static final String CROPPED_AREA_LEFT =
             "CroppedAreaLeftPixels";
     public static final String CROPPED_AREA_TOP =
             "CroppedAreaTopPixels";
 
     public ImageFilterTinyPlanet() {
         setFilterType(TYPE_TINYPLANET);
         mName = "TinyPlanet";
 
         mMinParameter = 10;
         mMaxParameter = 60;
         mDefaultParameter = 20;
         mPreviewParameter = 20;
         mParameter = 20;
         mAngle = 0;
     }
 
     public void setAngle(float angle) {
         mAngle = angle;
     }
 
     public float getAngle() {
         return mAngle;
     }
 
    public boolean isNil() {
        // TinyPlanet always has an effect
        return false;
    }

     native protected void nativeApplyFilter(
             Bitmap bitmapIn, int width, int height, Bitmap bitmapOut, int outSize, float scale,
             float angle);
 
     @Override
     public Bitmap apply(Bitmap bitmapIn, float scaleFactor, boolean highQuality) {
         int w = bitmapIn.getWidth();
         int h = bitmapIn.getHeight();
         int outputSize = (int) (w / 2f);
 
         ImagePreset preset = getImagePreset();
         if (preset != null && preset.isPanoramaSafe()) {
             bitmapIn = applyXmp(bitmapIn, preset, w);
         }
 
         Bitmap mBitmapOut = Bitmap.createBitmap(
                 outputSize, outputSize, Bitmap.Config.ARGB_8888);
         nativeApplyFilter(bitmapIn, bitmapIn.getWidth(), bitmapIn.getHeight(), mBitmapOut,
                 outputSize, mParameter / 100f, mAngle);
         return mBitmapOut;
     }
 
     private Bitmap applyXmp(Bitmap bitmapIn, ImagePreset preset, int intermediateWidth) {
         try {
             XMPMeta xmp = preset.getImageLoader().getXmpObject();
             if (xmp == null) {
                 // Do nothing, just use bitmapIn as is.
                 return bitmapIn;
             }
             int croppedAreaWidth =
                     getInt(xmp, CROPPED_AREA_IMAGE_WIDTH_PIXELS);
             int croppedAreaHeight =
                     getInt(xmp, CROPPED_AREA_IMAGE_HEIGHT_PIXELS);
             int fullPanoWidth =
                     getInt(xmp, CROPPED_AREA_FULL_PANO_WIDTH_PIXELS);
             int fullPanoHeight =
                     getInt(xmp, CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS);
             int left = getInt(xmp, CROPPED_AREA_LEFT);
             int top = getInt(xmp, CROPPED_AREA_TOP);
 
             // Make sure the intermediate image has the similar size to the
             // input.
             float scale = intermediateWidth / (float) fullPanoWidth;
             Bitmap paddedBitmap = Bitmap.createBitmap(
                     (int) (fullPanoWidth * scale), (int) (fullPanoHeight * scale),
                     Bitmap.Config.ARGB_8888);
             Canvas paddedCanvas = new Canvas(paddedBitmap);
 
             int right = left + croppedAreaWidth;
             int bottom = top + croppedAreaHeight;
             RectF destRect = new RectF(left * scale, top * scale, right * scale, bottom * scale);
             paddedCanvas.drawBitmap(bitmapIn, null, destRect, null);
             bitmapIn = paddedBitmap;
         } catch (XMPException ex) {
             // Do nothing, just use bitmapIn as is.
         }
         return bitmapIn;
     }
 
     private static int getInt(XMPMeta xmp, String key) throws XMPException {
         if (xmp.doesPropertyExist(GOOGLE_PANO_NAMESPACE, key)) {
             return xmp.getPropertyInteger(GOOGLE_PANO_NAMESPACE, key);
         } else {
             return 0;
         }
     }
 }
