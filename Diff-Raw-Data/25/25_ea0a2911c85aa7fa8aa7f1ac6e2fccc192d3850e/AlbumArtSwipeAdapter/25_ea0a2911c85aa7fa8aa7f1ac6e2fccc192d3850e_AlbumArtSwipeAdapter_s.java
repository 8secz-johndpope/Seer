 /* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
  *
  *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
  *
  *   Tomahawk is free software: you can redistribute it and/or modify
  *   it under the terms of the GNU General Public License as published by
  *   the Free Software Foundation, either version 3 of the License, or
  *   (at your option) any later version.
  *
  *   Tomahawk is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU General Public License for more details.
  *
  *   You should have received a copy of the GNU General Public License
  *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
  */
 package org.tomahawk.libtomahawk.audio;
 
 import org.tomahawk.libtomahawk.playlist.Playlist;
 import org.tomahawk.tomahawk_android.R;
 
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.os.Parcelable;
 import android.support.v4.view.PagerAdapter;
 import android.support.v4.view.ViewPager;
 import android.view.View;
 import android.widget.ImageView;
 
 /**
  * @author Enno Gottschalk <mrmaffen@googlemail.com>
  * 
  */
 public class AlbumArtSwipeAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
 
     private Context mContext;
    private int mFakeInfinityCount;
     private int mFakeInfinityOffset;
     private boolean mByUser;
     private boolean mSwiped;
     private ViewPager mViewPager;
     private PlaybackService mPlaybackService;
     private Playlist mPlaylist;
     private int mCurrentViewPage = 0;
 
     /**
      * Constructs a new AlbumArtSwipeAdapter with the given list of AlbumArt
      * images
      */
     public AlbumArtSwipeAdapter(Context context, ViewPager viewPager) {
         this.mContext = context;
        this.mFakeInfinityCount = Integer.MAX_VALUE;
         this.mByUser = true;
         this.mSwiped = false;
         this.mViewPager = viewPager;
         this.mViewPager.setAdapter(this);
         this.mViewPager.setOnPageChangeListener(this);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * android.support.v4.view.PagerAdapter#instantiateItem(android.view.View,
      * int)
      */
     @Override
     public Object instantiateItem(View collection, int position) {
         ImageView albumArt = new ImageView(mContext);
         if (mPlaylist != null) {
             Bitmap albumArtBitmap;
             if (mPlaylist.isRepeating())
                 albumArtBitmap = mPlaylist.getTrackAtPos((position) % mPlaylist.getCount()).getAlbum().getAlbumArt();
             else
                 albumArtBitmap = mPlaylist.getTrackAtPos(position).getAlbum().getAlbumArt();
             if (albumArtBitmap != null)
                 albumArt.setImageBitmap(albumArtBitmap);
             else
                 albumArt.setImageResource(R.drawable.no_album_art_placeholder);
         } else
             albumArt.setImageResource(R.drawable.no_album_art_placeholder);
         ((ViewPager) collection).addView(albumArt);
         return albumArt;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see android.support.v4.view.PagerAdapter#getCount()
      */
     @Override
     public int getCount() {
         if (mPlaylist == null)
             return 1;
         if (mPlaylist.isRepeating())
            return mFakeInfinityCount;
         return mPlaylist.getCount();
     }
 
     /**
      * @return the offset by which the position should be shifted, when playlist is repeating
      */
     public int getFakeInfinityOffset() {
         return mFakeInfinityOffset;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see android.support.v4.view.PagerAdapter#destroyItem(android.view.View,
      * int, java.lang.Object)
      */
     @Override
     public void destroyItem(View arg0, int arg1, Object arg2) {
         ((ViewPager) arg0).removeView((View) arg2);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * android.support.v4.view.PagerAdapter#isViewFromObject(android.view.View,
      * java.lang.Object)
      */
     @Override
     public boolean isViewFromObject(View arg0, Object arg1) {
         return arg0 == ((View) arg1);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see android.support.v4.view.PagerAdapter#saveState()
      */
     @Override
     public Parcelable saveState() {
         return null;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * android.support.v4.view.PagerAdapter#getItemPosition(java.lang.Object)
      */
     @Override
     public int getItemPosition(Object object) {
         return POSITION_NONE;
     }
 
     /** @param position to set the current item to
     /** @param smoothScroll boolean to determine wether or not to show a scrolling animation */
     public void setCurrentItem(int position, boolean smoothScroll) {
        if (position != mCurrentViewPage && position != mCurrentViewPage % mPlaylist.getCount()) {
             if (mPlaylist.isRepeating()) {
                 if (position == (mCurrentViewPage % mPlaylist.getCount()) + 1
                         || ((mCurrentViewPage % mPlaylist.getCount()) == mPlaylist.getCount() - 1 && position == 0))
                     setCurrentToNextItem(smoothScroll);
                 else if (position == (mCurrentViewPage % mPlaylist.getCount()) - 1
                         || ((mCurrentViewPage % mPlaylist.getCount()) == 0 && position == mPlaylist.getCount() - 1))
                     setCurrentToPreviousItem(smoothScroll);
                 else {
                     mViewPager.setCurrentItem(position, false);
                    mCurrentViewPage = position;
                 }
             } else {
                 mViewPager.setCurrentItem(position, smoothScroll);
                mCurrentViewPage = position;
             }
         }
     }
 
     /** @param smoothScroll boolean to determine wether or not to show a scrolling animation */
     public void setCurrentToNextItem(boolean smoothScroll) {
         mViewPager.setCurrentItem(mCurrentViewPage + 1, smoothScroll);
        mCurrentViewPage = mViewPager.getCurrentItem();
     }
 
     /** @param smoothScroll boolean to determine wether or not to show a scrolling animation */
     public void setCurrentToPreviousItem(boolean smoothScroll) {
         mViewPager.setCurrentItem(mCurrentViewPage - 1, smoothScroll);
        mCurrentViewPage = mViewPager.getCurrentItem();
     }
 
     /**
      * update the playlist of the AlbumArtSwipeAdapter to the given Playlist
      * 
      * @param playList
      */
     public void updatePlaylist() {
         if (mPlaybackService != null)
             mPlaylist = mPlaybackService.getCurrentPlaylist();
         if (mPlaylist != null) {
            mFakeInfinityOffset = mPlaylist.getCount() * (10000 / mPlaylist.getCount());
             setByUser(false);
             if (mPlaylist.isRepeating()) {
                 setCurrentItem(mPlaylist.getPosition() + getFakeInfinityOffset(), false);
             } else {
                 setCurrentItem(mPlaylist.getPosition(), false);
             }
             notifyDataSetChanged();
             setByUser(true);
         }
     }
 
     public boolean isByUser() {
         return mByUser;
     }
 
     public void setByUser(boolean byUser) {
         this.mByUser = byUser;
     }
 
     public boolean isSwiped() {
         return mSwiped;
     }
 
     public void setSwiped(boolean isSwiped) {
         this.mSwiped = isSwiped;
     }
 
     public boolean isPlaylistNull() {
         return mPlaylist == null;
     }
 
     public void setPlaybackService(PlaybackService mPlaybackService) {
         this.mPlaybackService = mPlaybackService;
         updatePlaylist();
     }
 
     /* (non-Javadoc)
      * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
      */
     @Override
     public void onPageSelected(int arg0) {
         if (mPlaybackService != null && isByUser()) {
             setSwiped(true);
             if (arg0 == mCurrentViewPage - 1)
                 mPlaybackService.previous();
             else if (arg0 == mCurrentViewPage + 1)
                 mPlaybackService.next();
         }
         mCurrentViewPage = arg0;
     }
 
     /* (non-Javadoc)
      * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
      */
     @Override
     public void onPageScrolled(int arg0, float arg1, int arg2) {
     }
 
     /* (non-Javadoc)
      * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrollStateChanged(int)
      */
     @Override
     public void onPageScrollStateChanged(int arg0) {
     }
 
 }
