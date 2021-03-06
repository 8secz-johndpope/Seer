 
 package com.android.gallery3d.filtershow.cache;
 
 import android.content.ContentResolver;
 import android.content.Context;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.database.sqlite.SQLiteException;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.BitmapRegionDecoder;
 import android.graphics.Matrix;
 import android.graphics.Rect;
 import android.media.ExifInterface;
 import android.net.Uri;
 import android.provider.MediaStore;
 import android.util.Log;
 
 import com.android.gallery3d.R;
 import com.android.gallery3d.common.Utils;
 import com.android.gallery3d.filtershow.FilterShowActivity;
 import com.android.gallery3d.filtershow.HistoryAdapter;
 import com.android.gallery3d.filtershow.imageshow.ImageShow;
 import com.android.gallery3d.filtershow.presets.ImagePreset;
 import com.android.gallery3d.filtershow.tools.ProcessedBitmap;
 import com.android.gallery3d.filtershow.tools.SaveCopyTask;
 
 import java.io.Closeable;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Vector;
 
 public class ImageLoader {
 
     private static final String LOGTAG = "ImageLoader";
     private final Vector<ImageShow> mListeners = new Vector<ImageShow>();
     private Bitmap mOriginalBitmapSmall = null;
     private Bitmap mOriginalBitmapLarge = null;
     private Bitmap mBackgroundBitmap = null;
     private Bitmap mFullOriginalBitmap = null;
     private Bitmap mSaveCopy = null;
 
     private Cache mCache = null;
     private Cache mHiresCache = null;
     private final ZoomCache mZoomCache = new ZoomCache();
 
     private int mOrientation = 0;
     private HistoryAdapter mAdapter = null;
 
     private FilterShowActivity mActivity = null;
 
     private static final int ORI_NORMAL     = ExifInterface.ORIENTATION_NORMAL;
     private static final int ORI_ROTATE_90  = ExifInterface.ORIENTATION_ROTATE_90;
     private static final int ORI_ROTATE_180 = ExifInterface.ORIENTATION_ROTATE_180;
     private static final int ORI_ROTATE_270 = ExifInterface.ORIENTATION_ROTATE_270;
     private static final int ORI_FLIP_HOR   = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
     private static final int ORI_FLIP_VERT  = ExifInterface.ORIENTATION_FLIP_VERTICAL;
     private static final int ORI_TRANSPOSE  = ExifInterface.ORIENTATION_TRANSPOSE;
     private static final int ORI_TRANSVERSE = ExifInterface.ORIENTATION_TRANSVERSE;
 
     private Context mContext = null;
     private Uri mUri = null;
 
     private Rect mOriginalBounds = null;
 
     public ImageLoader(FilterShowActivity activity, Context context) {
         mActivity = activity;
         mContext = context;
         mCache = new DelayedPresetCache(this, 30);
         mHiresCache = new DelayedPresetCache(this, 2);
     }
 
     public void loadBitmap(Uri uri,int size) {
         mUri = uri;
         mOrientation = getOrientation(uri);

         mOriginalBitmapSmall = loadScaledBitmap(uri, 160);
         if (mOriginalBitmapSmall == null) {
             // Couldn't read the bitmap, let's exit
             mActivity.cannotLoadImage();
         }
         mOriginalBitmapLarge = loadScaledBitmap(uri, size);
         updateBitmaps();
     }
 
     public Uri getUri() {
         return mUri;
     }
 
     public Rect getOriginalBounds() {
         return mOriginalBounds;
     }
 
     private int getOrientation(Uri uri) {
         if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
             return getOrientationFromPath(uri.getPath());
         }
 
         Cursor cursor = null;
         try {
             cursor = mContext.getContentResolver().query(uri,
                     new String[] {
                         MediaStore.Images.ImageColumns.ORIENTATION
                     },
                     null, null, null);
             if (cursor.moveToNext()){
               int ori =   cursor.getInt(0);
 
               switch (ori){
                   case 0:   return ORI_NORMAL;
                   case 90:  return ORI_ROTATE_90;
                   case 270: return ORI_ROTATE_270;
                   case 180: return ORI_ROTATE_180;
                   default:
                       return -1;
               }
             } else{
                 return -1;
             }
         } catch (SQLiteException e){
             return ExifInterface.ORIENTATION_UNDEFINED;
         } finally {
             Utils.closeSilently(cursor);
         }
     }
 
     private int getOrientationFromPath(String path) {
         int orientation = -1;
         try {
             ExifInterface EXIF = new ExifInterface(path);
             orientation = EXIF.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                     1);
         } catch (IOException e) {
             e.printStackTrace();
         }
         return orientation;
     }
 
     private void updateBitmaps() {
         if (mOrientation > 1) {
             mOriginalBitmapSmall = rotateToPortrait(mOriginalBitmapSmall,mOrientation);
             mOriginalBitmapLarge = rotateToPortrait(mOriginalBitmapLarge,mOrientation);
         }
         mCache.setOriginalBitmap(mOriginalBitmapSmall);
         mHiresCache.setOriginalBitmap(mOriginalBitmapLarge);
         warnListeners();
     }
 
     private Bitmap rotateToPortrait(Bitmap bitmap,int ori) {
            Matrix matrix = new Matrix();
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (ori == ORI_ROTATE_90 ||
                    ori == ORI_ROTATE_270 ||
                    ori == ORI_TRANSPOSE||
                    ori == ORI_TRANSVERSE) {
                int tmp = w;
                w = h;
                h = tmp;
            }
            switch(ori){
               case ORI_NORMAL:
                case ORI_ROTATE_90:
                    matrix.setRotate(90,w/2f,h/2f);
                    break;
                case ORI_ROTATE_180:
                    matrix.setRotate(180,w/2f,h/2f);
                    break;
                case ORI_ROTATE_270:
                    matrix.setRotate(270,w/2f,h/2f);
                    break;
                case ORI_FLIP_HOR:
                    matrix.preScale(-1, 1);
                    break;
               case ORI_FLIP_VERT:
                    matrix.preScale(1, -1);
                    break;
                case ORI_TRANSPOSE:
                    matrix.setRotate(90,w/2f,h/2f);
                    matrix.preScale(1, -1);
                    break;
                case ORI_TRANSVERSE:
                    matrix.setRotate(270,w/2f,h/2f);
                    matrix.preScale(1, -1);
                    break;
                default:
             }
 
         return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                 bitmap.getHeight(), matrix, true);
     }
 
     private void closeStream(Closeable stream) {
         if (stream != null) {
             try {
                 stream.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }
 
     private Bitmap loadRegionBitmap(Uri uri, Rect bounds) {
         InputStream is = null;
         try {
             is = mContext.getContentResolver().openInputStream(uri);
             BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
             return decoder.decodeRegion(bounds, null);
         } catch (FileNotFoundException e) {
             Log.e(LOGTAG, "FileNotFoundException: " + uri);
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             closeStream(is);
         }
         return null;
     }
 
     private Bitmap loadScaledBitmap(Uri uri, int size) {
         InputStream is = null;
         try {
             is = mContext.getContentResolver().openInputStream(uri);
             Log.v(LOGTAG, "loading uri " + uri.getPath() + " input stream: "
                     + is);
             BitmapFactory.Options o = new BitmapFactory.Options();
             o.inJustDecodeBounds = true;
             BitmapFactory.decodeStream(is, null, o);
 
             int width_tmp = o.outWidth;
             int height_tmp = o.outHeight;
 
             mOriginalBounds = new Rect(0, 0, width_tmp, height_tmp);
 
             int scale = 1;
             while (true) {
                 if (width_tmp / 2 < size || height_tmp / 2 < size)
                     break;
                 width_tmp /= 2;
                 height_tmp /= 2;
                 scale *= 2;
             }
 
             // decode with inSampleSize
             BitmapFactory.Options o2 = new BitmapFactory.Options();
             o2.inSampleSize = scale;
 
             closeStream(is);
             is = mContext.getContentResolver().openInputStream(uri);
             return BitmapFactory.decodeStream(is, null, o2);
         } catch (FileNotFoundException e) {
             Log.e(LOGTAG, "FileNotFoundException: " + uri);
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             closeStream(is);
         }
         return null;
     }
 
     public Bitmap getBackgroundBitmap(Resources resources) {
         if (mBackgroundBitmap == null) {
             mBackgroundBitmap = BitmapFactory.decodeResource(resources,
                     R.drawable.filtershow_background);
         }
         return mBackgroundBitmap;
 
     }
 
     public Bitmap getOriginalBitmapSmall() {
         return mOriginalBitmapSmall;
     }
 
     public Bitmap getOriginalBitmapLarge() {
         return mOriginalBitmapLarge;
     }
 
     public void addListener(ImageShow imageShow) {
         if (!mListeners.contains(imageShow)) {
             mListeners.add(imageShow);
         }
     }
 
     public void warnListeners() {
         for (int i = 0; i < mListeners.size(); i++) {
             ImageShow imageShow = mListeners.elementAt(i);
             imageShow.updateImage();
         }
     }
 
     // TODO: this currently does the loading + filtering on the UI thread -- need to
     // move this to a background thread.
     public Bitmap getScaleOneImageForPreset(ImageShow caller, ImagePreset imagePreset, Rect bounds,
             boolean force) {
         Bitmap bmp = mZoomCache.getImage(imagePreset, bounds);
         if (force || bmp == null) {
             bmp = loadRegionBitmap(mUri, bounds);
             if (bmp != null) {
                 // TODO: this workaround for RS might not be needed ultimately
                 Bitmap bmp2 = bmp.copy(Bitmap.Config.ARGB_8888, true);
                 float scaleFactor = imagePreset.getScaleFactor();
                 imagePreset.setScaleFactor(1.0f);
                 bmp2 = imagePreset.apply(bmp2);
                 imagePreset.setScaleFactor(scaleFactor);
                 mZoomCache.setImage(imagePreset, bounds, bmp2);
                 return bmp2;
             }
         }
         return bmp;
     }
 
     // Caching method
     public Bitmap getImageForPreset(ImageShow caller, ImagePreset imagePreset,
             boolean hiRes) {
         if (mOriginalBitmapSmall == null) {
             return null;
         }
         if (mOriginalBitmapLarge == null) {
             return null;
         }
 
         Bitmap filteredImage = null;
 
         if (hiRes) {
             filteredImage = mHiresCache.get(imagePreset);
         } else {
             filteredImage = mCache.get(imagePreset);
         }
 
         if (filteredImage == null) {
             if (hiRes) {
                 cachePreset(imagePreset, mHiresCache, caller);
             } else {
                 cachePreset(imagePreset, mCache, caller);
             }
         }
         return filteredImage;
     }
 
     public void resetImageForPreset(ImagePreset imagePreset, ImageShow caller) {
         mHiresCache.reset(imagePreset);
         mCache.reset(imagePreset);
         mZoomCache.reset(imagePreset);
     }
 
     public Uri saveImage(ImagePreset preset, final FilterShowActivity filterShowActivity,
             File destination) {
         BitmapFactory.Options options = new BitmapFactory.Options();
         options.inMutable = true;
 
         if (mFullOriginalBitmap != null) {
             mFullOriginalBitmap.recycle();
         }
 
         InputStream is = null;
         Uri saveUri = null;
         try {
             is = mContext.getContentResolver().openInputStream(mUri);
             mFullOriginalBitmap = BitmapFactory.decodeStream(is, null, options);
             // TODO: on <3.x we need a copy of the bitmap (inMutable doesn't
             // exist)
             mFullOriginalBitmap = rotateToPortrait(mFullOriginalBitmap,mOrientation);
             mSaveCopy = mFullOriginalBitmap;
             preset.setIsHighQuality(true);
             preset.setScaleFactor(1.0f);
             ProcessedBitmap processedBitmap = new ProcessedBitmap(mSaveCopy, preset);
             new SaveCopyTask(mContext, mUri, destination, new SaveCopyTask.Callback() {
 
                 @Override
                 public void onComplete(Uri result) {
                     filterShowActivity.completeSaveImage(result);
                 }
 
             }).execute(processedBitmap);
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         } finally {
             closeStream(is);
         }
 
         return saveUri;
     }
 
     public void setAdapter(HistoryAdapter adapter) {
         mAdapter = adapter;
     }
 
     public HistoryAdapter getHistory() {
         return mAdapter;
     }
 
     private void cachePreset(ImagePreset preset, Cache cache, ImageShow caller) {
         cache.prepare(preset);
         cache.addObserver(caller);
     }
 }
