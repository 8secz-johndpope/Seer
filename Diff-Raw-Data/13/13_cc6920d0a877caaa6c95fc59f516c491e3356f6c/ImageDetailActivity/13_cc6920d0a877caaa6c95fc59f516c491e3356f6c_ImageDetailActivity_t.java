 package com.globant.mobile.handson;
 
 import android.annotation.TargetApi;
 import android.app.ActionBar;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentActivity;
 import android.support.v4.app.FragmentManager;
 import android.support.v4.app.FragmentStatePagerAdapter;
 import android.support.v4.app.NavUtils;
 import android.support.v4.view.ViewPager;
 import android.util.DisplayMetrics;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.WindowManager.LayoutParams;
 
 import com.globant.mobile.handson.media.BitmapCache;
 import com.globant.mobile.handson.media.BitmapFetcher;
 import com.globant.mobile.handson.provider.Bitmaps;
 import com.globant.mobile.handson.provider.Images;
 
 public class ImageDetailActivity extends FragmentActivity implements OnClickListener{
 
 	private static final String IMAGE_CACHE_DIR = "images";
     public static final String EXTRA_IMAGE = "extra_image";
 
     private ImagePagerAdapter mAdapter;
     private BitmapFetcher mImageFetcher;
     private ViewPager mPager;
 
     @TargetApi(11)
     @Override
     public void onCreate(Bundle savedInstanceState) {
         if (BuildConfig.DEBUG) {
         	//enableStrictMode();
         }
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_image_detail);
 
         // Fetch screen height and width, to use as our max size when loading images as this
         // activity runs full screen
         final DisplayMetrics displayMetrics = new DisplayMetrics();
         getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
         final int height = displayMetrics.heightPixels;
         final int width = displayMetrics.widthPixels;
 
         // For this sample we'll use half of the longest width to resize our images. As the
         // image scaling ensures the image is larger than this, we should be left with a
         // resolution that is appropriate for both portrait and landscape. For best image quality
         // we shouldn't divide by 2, but this will use more memory and require a larger memory
         // cache.
         final int longest = (height > width ? height : width) / 2;
 
         BitmapCache.ImageCacheParams cacheParams =
                 new BitmapCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
         cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
 
         // The ImageFetcher takes care of loading images into our ImageView children asynchronously
         mImageFetcher = new BitmapFetcher(this, longest);
         mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
         mImageFetcher.setImageFadeIn(false);
 
         // Set up ViewPager and backing adapter
         mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), Images.imageUrls.length);
         mPager = (ViewPager) findViewById(R.id.pager);
         mPager.setAdapter(mAdapter);
         mPager.setPageMargin((int) getResources().getDimension(R.dimen.image_detail_pager_margin));
         mPager.setOffscreenPageLimit(2);
 
         // Set up activity to go full screen
         getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
 
         // Enable some additional newer visibility and ActionBar features to create a more
         // immersive photo viewing experience
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
             final ActionBar actionBar = getActionBar();
 
             // Hide title text and set home as up
             actionBar.setDisplayShowTitleEnabled(false);
             actionBar.setDisplayHomeAsUpEnabled(true);
 
             // Hide and show the ActionBar as the visibility changes
             mPager.setOnSystemUiVisibilityChangeListener(
                     new View.OnSystemUiVisibilityChangeListener() {
                         @Override
                         public void onSystemUiVisibilityChange(int vis) {
                             if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                 actionBar.hide();
                             } else {
                                 actionBar.show();
                             }
                         }
                     });
 
             // Start low profile mode and hide ActionBar
             mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
             actionBar.hide();
         }
 
         // Set the current item based on the extra passed in to this activity
         final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
         if (extraCurrentItem != -1) {
             mPager.setCurrentItem(extraCurrentItem);
         }
     }
 
     @Override
     public void onResume() {
         super.onResume();
        mAdapter.setSize(Bitmaps.imageUrls.length);
        mAdapter.notifyDataSetChanged();
         mImageFetcher.setExitTasksEarly(false);
     }
 
     @Override
     protected void onPause() {
         super.onPause();
         mImageFetcher.setExitTasksEarly(true);
         mImageFetcher.flushCache();
     }
 
     @Override
     protected void onDestroy() {
         super.onDestroy();
         mImageFetcher.closeCache();
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 NavUtils.navigateUpFromSameTask(this);
                 return true;
             /*case R.id.clear_cache:
                 mImageFetcher.clearCache();
                 //Toast.makeText(
                 //        this, R.string.clear_cache_complete_toast,Toast.LENGTH_SHORT).show();
                 return true;*/
         }
         return super.onOptionsItemSelected(item);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         getMenuInflater().inflate(R.menu.main, menu);
         return true;
     }
 
     /**
      * Called by the ViewPager child fragments to load images via the one ImageFetcher
      */
     public BitmapFetcher getImageFetcher() {
         return mImageFetcher;
     }
 
     /**
      * The main adapter that backs the ViewPager. A subclass of FragmentStatePagerAdapter as there
      * could be a large number of items in the ViewPager and we don't want to retain them all in
      * memory at once but create/destroy them on the fly.
      */
     private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private int mSize;
 
         public ImagePagerAdapter(FragmentManager fm, int size) {
             super(fm);
            mSize = size;            
         }
 
         @Override
         public int getCount() {
             return mSize;
         }
 
         @Override
         public Fragment getItem(int position) {
             return ImageDetailFragment.newInstance(Bitmaps.imageUrls[position]);
         }
        
        public void setSize(int size){
        	mSize = size;
        }
     }
 
     /**
      * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
      * when the ImageView is touched.
      */
     @TargetApi(11)
     @Override
     public void onClick(View v) {
         final int vis = mPager.getSystemUiVisibility();
         if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
             mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
         } else {
             mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
         }
     }
     
     /*@TargetApi(11)
     public static void enableStrictMode() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
             StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                     new StrictMode.ThreadPolicy.Builder()
                             .detectAll()
                             .penaltyLog();
             StrictMode.VmPolicy.Builder vmPolicyBuilder =
                     new StrictMode.VmPolicy.Builder()
                             .detectAll()
                             .penaltyLog();
 
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                 threadPolicyBuilder.penaltyFlashScreen();
                 vmPolicyBuilder
                         .setClassInstanceLimit(ImageGrid.class, 1)
                         .setClassInstanceLimit(ImageDetailActivity.class, 1);
             }
             StrictMode.setThreadPolicy(threadPolicyBuilder.build());
             StrictMode.setVmPolicy(vmPolicyBuilder.build());
         }
     }*/
 
 }
