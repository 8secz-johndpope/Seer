 /*
  * Copyright (C) 2013 The Android Open Source Project
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
 
 
 package android.support.v4.widget;
 
 import android.content.Context;
 import android.content.res.TypedArray;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.graphics.PixelFormat;
 import android.graphics.drawable.Drawable;
 import android.os.Parcel;
 import android.os.Parcelable;
 import android.support.v4.view.GravityCompat;
 import android.support.v4.view.KeyEventCompat;
 import android.support.v4.view.MotionEventCompat;
 import android.support.v4.view.ViewCompat;
 import android.util.AttributeSet;
 import android.view.Gravity;
 import android.view.KeyEvent;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewGroup;
 
 /**
  * DrawerLayout acts as a top-level container for window content that allows for
  * interactive "drawer" views to be pulled out from the edge of the window.
  *
  * <p>Drawer positioning and layout is controlled using the <code>android:layout_gravity</code>
  * attribute on child views corresponding to </p>
  *
  * <p>As per the Android Design guide, any drawers positioned to the left/start should
  * always contain content for navigating around the application, whereas any drawers
  * positioned to the right/end should always contain actions to take on the current content.
  * This preserves the same navigation left, actions right structure present in the Action Bar
  * and elsewhere.</p>
  */
 public class DrawerLayout extends ViewGroup {
     private static final String TAG = "DrawerLayout";
 
     private static final int INVALID_POINTER = -1;
 
     /**
      * Indicates that any drawers are in an idle, settled state. No animation is in progress.
      */
     public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
 
     /**
      * Indicates that a drawer is currently being dragged by the user.
      */
     public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
 
     /**
      * Indicates that a drawer is in the process of settling to a final position.
      */
     public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;
 
     private static final int MIN_DRAWER_MARGIN = 64; // dp
 
     private static final int DRAWER_PEEK_DISTANCE = 16; // dp
 
     private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
 
     private static final int[] LAYOUT_ATTRS = new int[] {
             android.R.attr.layout_gravity
     };
 
     private int mMinDrawerMargin;
     private int mDrawerPeekDistance;
 
     private int mScrimColor = DEFAULT_SCRIM_COLOR;
     private float mScrimOpacity;
     private Paint mScrimPaint = new Paint();
 
     private final ViewDragHelper mLeftDragger;
     private final ViewDragHelper mRightDragger;
     private int mDrawerState;
     private boolean mInLayout;
     private boolean mFirstLayout = true;
 
     private DrawerListener mListener;
 
     private float mInitialMotionX;
     private float mInitialMotionY;
 
     private Drawable mShadowLeft;
     private Drawable mShadowRight;
 
     /**
      * Listener for monitoring events about drawers.
      */
     public interface DrawerListener {
         /**
          * Called when a drawer's position changes.
          * @param drawerView The child view that was moved
          * @param slideOffset The new offset of this drawer within its range, from 0-1
          */
         public void onDrawerSlide(View drawerView, float slideOffset);
 
         /**
          * Called when a drawer has settled in a completely open state.
          * The drawer is interactive at this point.
          *
          * @param drawerView Drawer view that is now open
          */
         public void onDrawerOpened(View drawerView);
 
         /**
          * Called when a drawer has settled in a completely closed state.
          *
          * @param drawerView Drawer view that is now closed
          */
         public void onDrawerClosed(View drawerView);
 
         /**
          * Called when the drawer motion state changes. The new state will
          * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
          *
          * @param newState The new drawer motion state
          */
         public void onDrawerStateChanged(int newState);
     }
 
     /**
      * Stub/no-op implementations of all methods of {@link DrawerListener}.
      * Override this if you only care about a few of the available callback methods.
      */
     public static abstract class SimpleDrawerListener implements DrawerListener {
         @Override
         public void onDrawerSlide(View drawerView, float slideOffset) {
         }
 
         @Override
         public void onDrawerOpened(View drawerView) {
         }
 
         @Override
         public void onDrawerClosed(View drawerView) {
         }
 
         @Override
         public void onDrawerStateChanged(int newState) {
         }
     }
 
     public DrawerLayout(Context context) {
         this(context, null);
     }
 
     public DrawerLayout(Context context, AttributeSet attrs) {
         this(context, attrs, 0);
     }
 
     public DrawerLayout(Context context, AttributeSet attrs, int defStyle) {
         super(context, attrs, defStyle);
 
         final float density = getResources().getDisplayMetrics().density;
         mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
         mDrawerPeekDistance = (int) (DRAWER_PEEK_DISTANCE * density + 0.5f);
 
         final ViewDragCallback leftCallback = new ViewDragCallback(Gravity.LEFT);
         final ViewDragCallback rightCallback = new ViewDragCallback(Gravity.RIGHT);
 
         mLeftDragger = ViewDragHelper.create(this, 0.5f, leftCallback);
         mLeftDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
         leftCallback.setDragger(mLeftDragger);
 
         mRightDragger = ViewDragHelper.create(this, 0.5f, rightCallback);
         mRightDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
         rightCallback.setDragger(mRightDragger);
 
         // So that we can catch the back button
         setFocusableInTouchMode(true);
     }
 
     /**
      * Set a simple drawable used for the left or right shadow.
      * The drawable provided must have a nonzero intrinsic width.
      *
      * @param shadowDrawable Shadow drawable to use at the edge of a drawer
      * @param gravity Which drawer the shadow should apply to
      */
     public void setDrawerShadow(Drawable shadowDrawable, int gravity) {
         /*
          * TODO Someone someday might want to set more complex drawables here.
          * They're probably nuts, but we might want to consider registering callbacks,
          * setting states, etc. properly.
          */
 
         final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                 ViewCompat.getLayoutDirection(this));
         if ((absGravity & Gravity.LEFT) == Gravity.LEFT) {
             mShadowLeft = shadowDrawable;
             invalidate();
         }
         if ((absGravity & Gravity.RIGHT) == Gravity.RIGHT) {
             mShadowRight = shadowDrawable;
             invalidate();
         }
     }
 
     /**
      * Set a simple drawable used for the left or right shadow.
      * The drawable provided must have a nonzero intrinsic width.
      *
      * @param resId Resource id of a shadow drawable to use at the edge of a drawer
      * @param gravity Which drawer the shadow should apply to
      */
     public void setDrawerShadow(int resId, int gravity) {
         setDrawerShadow(getResources().getDrawable(resId), gravity);
     }
 
     /**
      * Set a listener to be notified of drawer events.
      *
      * @param listener Listener to notify when drawer events occur
      * @see DrawerListener
      */
     public void setDrawerListener(DrawerListener listener) {
         mListener = listener;
     }
 
     /**
      * Resolve the shared state of all drawers from the component ViewDragHelpers.
      * Should be called whenever a ViewDragHelper's state changes.
      */
     void updateDrawerState(int forGravity, int activeState, View activeDrawer) {
         final int leftState = mLeftDragger.getViewDragState();
         final int rightState = mRightDragger.getViewDragState();
 
         final int state;
         if (leftState == STATE_DRAGGING || rightState == STATE_DRAGGING) {
             state = STATE_DRAGGING;
         } else if (leftState == STATE_SETTLING || rightState == STATE_SETTLING) {
             state = STATE_SETTLING;
         } else {
             state = STATE_IDLE;
         }
 
         if (activeDrawer != null && activeState == STATE_IDLE) {
             final LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
             if (lp.onScreen == 0) {
                 dispatchOnDrawerClosed(activeDrawer);
             } else if (lp.onScreen == 1) {
                 dispatchOnDrawerOpened(activeDrawer);
             }
         }
 
         if (state != mDrawerState) {
             mDrawerState = state;
 
             if (mListener != null) {
                 mListener.onDrawerStateChanged(state);
             }
         }
     }
 
     void dispatchOnDrawerClosed(View drawerView) {
         final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
         if (lp.knownOpen) {
             lp.knownOpen = false;
             if (mListener != null) {
                 mListener.onDrawerClosed(drawerView);
             }
         }
     }
 
     void dispatchOnDrawerOpened(View drawerView) {
         final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
         if (!lp.knownOpen) {
             lp.knownOpen = true;
             if (mListener != null) {
                 mListener.onDrawerOpened(drawerView);
             }
         }
     }
 
     void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
         if (mListener != null) {
             mListener.onDrawerSlide(drawerView, slideOffset);
         }
     }
 
     void setDrawerViewOffset(View drawerView, float slideOffset) {
         final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
         if (slideOffset == lp.onScreen) {
             return;
         }
 
         lp.onScreen = slideOffset;
         dispatchOnDrawerSlide(drawerView, slideOffset);
     }
 
     float getDrawerViewOffset(View drawerView) {
         return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
     }
 
     int getDrawerViewGravity(View drawerView) {
         final int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
         return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(drawerView));
     }
 
     boolean checkDrawerViewGravity(View drawerView, int checkFor) {
         final int absGrav = getDrawerViewGravity(drawerView);
         return (absGrav & checkFor) == checkFor;
     }
 
     void moveDrawerToOffset(View drawerView, float slideOffset) {
         final float oldOffset = getDrawerViewOffset(drawerView);
         final int width = drawerView.getWidth();
         final int oldPos = (int) (width * oldOffset);
         final int newPos = (int) (width * slideOffset);
         final int dx = newPos - oldPos;
 
         drawerView.offsetLeftAndRight(checkDrawerViewGravity(drawerView, Gravity.LEFT) ? dx : -dx);
         setDrawerViewOffset(drawerView, slideOffset);
     }
 
     View findDrawerWithGravity(int gravity) {
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
             final int childGravity = getDrawerViewGravity(child);
             if ((childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) ==
                     (gravity & Gravity.HORIZONTAL_GRAVITY_MASK)) {
                 return child;
             }
         }
         return null;
     }
 
     /**
      * Simple gravity to string - only supports LEFT and RIGHT for debugging output.
      *
      * @param gravity Absolute gravity value
      * @return LEFT or RIGHT as appropriate, or a hex string
      */
     static String gravityToString(int gravity) {
         if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
             return "LEFT";
         }
         if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
             return "RIGHT";
         }
         return Integer.toHexString(gravity);
     }
 
     @Override
     protected void onDetachedFromWindow() {
         super.onDetachedFromWindow();
         mFirstLayout = true;
     }
 
     @Override
     protected void onAttachedToWindow() {
         super.onAttachedToWindow();
         mFirstLayout = true;
     }
 
     @Override
     protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
         final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
         final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
         final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
         final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
 
         if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
             throw new IllegalArgumentException(
                     "DrawerLayout must be measured with MeasureSpec.EXACTLY.");
         }
 
         setMeasuredDimension(widthSize, heightSize);
 
         // Gravity value for each drawer we've seen. Only one of each permitted.
         int foundDrawers = 0;
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
 
             if (child.getVisibility() == GONE) {
                 continue;
             }
 
             if (isContentView(child)) {
                 // Content views get measured at exactly the layout's size.
                 child.measure(widthMeasureSpec, heightMeasureSpec);
             } else if (isDrawerView(child)) {
                 final int childGravity =
                         getDrawerViewGravity(child) & Gravity.HORIZONTAL_GRAVITY_MASK;
                 if ((foundDrawers & childGravity) != 0) {
                     throw new IllegalStateException("Child drawer has absolute gravity " +
                             gravityToString(childGravity) + " but this " + TAG + " already has a " +
                             "drawer view along that edge");
                 }
                 final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec, mMinDrawerMargin,
                         child.getLayoutParams().width);
                 child.measure(drawerWidthSpec, heightMeasureSpec);
             } else {
                 throw new IllegalStateException("Child " + child + " at index " + i +
                         " does not have a valid layout_gravity - must be Gravity.LEFT, " +
                         "Gravity.RIGHT or Gravity.NO_GRAVITY");
             }
         }
     }
 
     @Override
     protected void onLayout(boolean changed, int l, int t, int r, int b) {
         mInLayout = true;
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
 
             if (child.getVisibility() == GONE) {
                 continue;
             }
 
             if (isContentView(child)) {
                 child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
             } else { // Drawer, if it wasn't onMeasure would have thrown an exception.
                 final LayoutParams lp = (LayoutParams) child.getLayoutParams();
 
                 final int childWidth = child.getMeasuredWidth();
                 int childLeft;
 
                 if (checkDrawerViewGravity(child, Gravity.LEFT)) {
                     childLeft = -childWidth + (int) (childWidth * lp.onScreen);
                 } else { // Right; onMeasure checked for us.
                     childLeft = r - l - (int) (childWidth * lp.onScreen);
                 }
 
                 child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
 
                 if (lp.onScreen == 0) {
                     child.setVisibility(INVISIBLE);
                 }
             }
         }
         mInLayout = false;
         mFirstLayout = false;
     }
 
     @Override
     public void requestLayout() {
         if (!mInLayout) {
             super.requestLayout();
         }
     }
 
     @Override
     public void computeScroll() {
         final int childCount = getChildCount();
         float scrimOpacity = 0;
         for (int i = 0; i < childCount; i++) {
             final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
             scrimOpacity = Math.max(scrimOpacity, onscreen);
         }
         mScrimOpacity = scrimOpacity;
 
         // "|" used on purpose; both need to run.
         if (mLeftDragger.continueSettling(true) | mRightDragger.continueSettling(true)) {
             ViewCompat.postInvalidateOnAnimation(this);
         }
     }
 
     private static boolean hasOpaqueBackground(View v) {
         final Drawable bg = v.getBackground();
         if (bg != null) {
             return bg.getOpacity() == PixelFormat.OPAQUE;
         }
         return false;
     }
 
     @Override
     protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
         final boolean drawingContent = isContentView(child);
         int clipLeft = 0, clipRight = getWidth();
 
         final int restoreCount = canvas.save();
         if (drawingContent) {
             final int childCount = getChildCount();
             for (int i = 0; i < childCount; i++) {
                 final View v = getChildAt(i);
                 if (v == child || v.getVisibility() != VISIBLE ||
                         !hasOpaqueBackground(v) || !isDrawerView(v)) {
                     continue;
                 }
 
                 if (checkDrawerViewGravity(v, Gravity.LEFT)) {
                     final int vright = v.getRight();
                     if (vright > clipLeft) clipLeft = vright;
                 } else {
                     final int vleft = v.getLeft();
                     if (vleft < clipRight) clipRight = vleft;
                 }
             }
             canvas.clipRect(clipLeft, 0, clipRight, getHeight());
         }
         final boolean result = super.drawChild(canvas, child, drawingTime);
         canvas.restoreToCount(restoreCount);
 
         if (mScrimOpacity > 0 && drawingContent) {
             final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
             final int imag = (int) (baseAlpha * mScrimOpacity);
             final int color = imag << 24 | (mScrimColor & 0xffffff);
             mScrimPaint.setColor(color);
 
             canvas.drawRect(clipLeft, 0, clipRight, getHeight(), mScrimPaint);
         } else if (mShadowLeft != null && checkDrawerViewGravity(child, Gravity.LEFT)) {
             final int shadowWidth = mShadowLeft.getIntrinsicWidth();
             final int childRight = child.getRight();
             final float alpha =
                     Math.max(0, Math.min((float) childRight / mDrawerPeekDistance, 1.f));
             mShadowLeft.setBounds(childRight, child.getTop(),
                     childRight + shadowWidth, child.getBottom());
             mShadowLeft.setAlpha((int) (0xff * alpha));
             mShadowLeft.draw(canvas);
         } else if (mShadowRight != null && checkDrawerViewGravity(child, Gravity.RIGHT)) {
             final int shadowWidth = mShadowRight.getIntrinsicWidth();
             final int childLeft = child.getLeft();
             final int showing = getWidth() - childLeft;
             final float alpha =
                     Math.max(0, Math.min((float) showing / mDrawerPeekDistance, 1.f));
             mShadowRight.setBounds(childLeft - shadowWidth, child.getTop(),
                     childLeft, child.getBottom());
             mShadowRight.setAlpha((int) (0xff * alpha));
             mShadowRight.draw(canvas);
         }
         return result;
     }
 
     boolean isContentView(View child) {
         return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
     }
 
     boolean isDrawerView(View child) {
         final int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
         final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                 ViewCompat.getLayoutDirection(child));
         return (absGravity & (Gravity.LEFT | Gravity.RIGHT)) != 0;
     }
 
     @Override
     public boolean onInterceptTouchEvent(MotionEvent ev) {
         final int action = MotionEventCompat.getActionMasked(ev);
 
         // "|" used deliberately here; both methods should be invoked.
         final boolean interceptForDrag = mLeftDragger.shouldInterceptTouchEvent(ev) |
                 mRightDragger.shouldInterceptTouchEvent(ev);
 
         boolean interceptForTap = false;
 
         switch (action) {
             case MotionEvent.ACTION_DOWN: {
                 final float x = ev.getX();
                 final float y = ev.getY();
                 mInitialMotionX = x;
                 mInitialMotionY = y;
                 if (mScrimOpacity > 0 &&
                         isContentView(mLeftDragger.findTopChildUnder((int) x, (int) y))) {
                     interceptForTap = true;
                 }
                 break;
             }
 
             case MotionEvent.ACTION_CANCEL:
             case MotionEvent.ACTION_UP: {
                 closeDrawers(true);
             }
         }
         return interceptForDrag || interceptForTap;
     }
 
     @Override
     public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
 
             if (lp.isPeeking) {
                 // Don't disallow intercept at all if we have a peeking view, we're probably
                 // going to intercept it later anyway.
                 return;
             }
         }
         super.requestDisallowInterceptTouchEvent(disallowIntercept);
         if (disallowIntercept) {
             closeDrawers(true);
         }
     }
 
     @Override
     public boolean onTouchEvent(MotionEvent ev) {
         mLeftDragger.processTouchEvent(ev);
         mRightDragger.processTouchEvent(ev);
 
         final int action = ev.getAction();
         boolean wantTouchEvents = true;
 
         switch (action & MotionEventCompat.ACTION_MASK) {
             case MotionEvent.ACTION_DOWN: {
                 final float x = ev.getX();
                 final float y = ev.getY();
                 mInitialMotionX = x;
                 mInitialMotionY = y;
                 break;
             }
 
             case MotionEvent.ACTION_UP: {
                 final float x = ev.getX();
                 final float y = ev.getY();
                 boolean peekingOnly = true;
                final View touchedView = mLeftDragger.findTopChildUnder((int) x, (int) y);
                if (touchedView != null && isContentView(touchedView)) {
                     final float dx = x - mInitialMotionX;
                     final float dy = y - mInitialMotionY;
                     final int slop = mLeftDragger.getTouchSlop();
                     if (dx * dx + dy * dy < slop * slop) {
                         // Taps close a dimmed open pane.
                         peekingOnly = false;
                     }
                 }
                 closeDrawers(peekingOnly);
                 break;
             }
 
             case MotionEvent.ACTION_CANCEL: {
                 closeDrawers(true);
                 break;
             }
         }
 
         return wantTouchEvents;
     }
 
     /**
      * Close all currently open drawer views by animating them out of view.
      */
     public void closeDrawers() {
         closeDrawers(false);
     }
 
     void closeDrawers(boolean peekingOnly) {
         boolean needsInvalidate = false;
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
             final LayoutParams lp = (LayoutParams) child.getLayoutParams();
 
             if (!isDrawerView(child) || (peekingOnly && !lp.isPeeking)) {
                 continue;
             }
 
             final int childWidth = child.getWidth();
 
             if (checkDrawerViewGravity(child, Gravity.LEFT)) {
                 needsInvalidate |= mLeftDragger.smoothSlideViewTo(child,
                         -childWidth, child.getTop());
             } else {
                 needsInvalidate |= mRightDragger.smoothSlideViewTo(child,
                         getWidth(), child.getTop());
             }
 
             lp.isPeeking = false;
         }
 
         if (needsInvalidate) {
             invalidate();
         }
     }
 
     /**
      * Open the specified drawer view by animating it into view.
      *
      * @param drawerView Drawer view to open
      */
     public void openDrawer(View drawerView) {
         if (!isDrawerView(drawerView)) {
             throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
         }
 
         if (mFirstLayout) {
             final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
             lp.onScreen = 1.f;
             lp.knownOpen = true;
         } else {
             if (checkDrawerViewGravity(drawerView, Gravity.LEFT)) {
                 mLeftDragger.smoothSlideViewTo(drawerView, 0, drawerView.getTop());
             } else {
                 mRightDragger.smoothSlideViewTo(drawerView, getWidth() - drawerView.getWidth(),
                         drawerView.getTop());
             }
         }
         invalidate();
     }
 
     /**
      * Open the specified drawer by animating it out of view.
      *
      * @param gravity Gravity.LEFT to move the left drawer or Gravity.RIGHT for the right.
      *                GravityCompat.START or GravityCompat.END may also be used.
      */
     public void openDrawer(int gravity) {
         final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                 ViewCompat.getLayoutDirection(this));
         final View drawerView = findDrawerWithGravity(absGravity);
 
         if (drawerView == null) {
             throw new IllegalArgumentException("No drawer view found with absolute gravity " +
                     gravityToString(absGravity));
         }
         openDrawer(drawerView);
     }
 
     /**
      * Close the specified drawer view by animating it into view.
      *
      * @param drawerView Drawer view to close
      */
     public void closeDrawer(View drawerView) {
         if (!isDrawerView(drawerView)) {
             throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
         }
 
         if (mFirstLayout) {
             final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
             lp.onScreen = 0.f;
             lp.knownOpen = false;
         } else {
             if (checkDrawerViewGravity(drawerView, Gravity.LEFT)) {
                 mLeftDragger.smoothSlideViewTo(drawerView, -drawerView.getWidth(),
                         drawerView.getTop());
             } else {
                 mRightDragger.smoothSlideViewTo(drawerView, getWidth(), drawerView.getTop());
             }
         }
         invalidate();
     }
 
     /**
      * Close the specified drawer by animating it out of view.
      *
      * @param gravity Gravity.LEFT to move the left drawer or Gravity.RIGHT for the right.
      *                GravityCompat.START or GravityCompat.END may also be used.
      */
     public void closeDrawer(int gravity) {
         final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                 ViewCompat.getLayoutDirection(this));
         final View drawerView = findDrawerWithGravity(absGravity);
 
         if (drawerView == null) {
             throw new IllegalArgumentException("No drawer view found with absolute gravity " +
                     gravityToString(absGravity));
         }
         closeDrawer(drawerView);
     }
 
     /**
      * Check if the given drawer view is currently in an open state.
      * To be considered "open" the drawer must have settled into its fully
      * visible state. To check for partial visibility use
      * {@link #isDrawerVisible(android.view.View)}.
      *
      * @param drawer Drawer view to check
      * @return true if the given drawer view is in an open state
      * @see #isDrawerVisible(android.view.View)
      */
     public boolean isDrawerOpen(View drawer) {
         if (!isDrawerView(drawer)) {
             throw new IllegalArgumentException("View " + drawer + " is not a drawer");
         }
         return ((LayoutParams) drawer.getLayoutParams()).knownOpen;
     }
 
     /**
      * Check if a given drawer view is currently visible on-screen. The drawer
      * may be only peeking onto the screen, fully extended, or anywhere inbetween.
      *
      * @param drawer Drawer view to check
      * @return true if the given drawer is visible on-screen
      * @see #isDrawerOpen(android.view.View)
      */
     public boolean isDrawerVisible(View drawer) {
         if (!isDrawerView(drawer)) {
             throw new IllegalArgumentException("View " + drawer + " is not a drawer");
         }
         return ((LayoutParams) drawer.getLayoutParams()).onScreen > 0;
     }
 
     @Override
     protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
         return new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
     }
 
     @Override
     protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
         return p instanceof LayoutParams
                 ? new LayoutParams((LayoutParams) p)
                 : new LayoutParams(p);
     }
 
     @Override
     protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
         return p instanceof LayoutParams && super.checkLayoutParams(p);
     }
 
     @Override
     public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
         return new LayoutParams(getContext(), attrs);
     }
 
     private boolean hasVisibleDrawer() {
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
             if (isDrawerView(child) && isDrawerVisible(child)) {
                 return true;
             }
         }
         return false;
     }
 
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_BACK && hasVisibleDrawer()) {
             KeyEventCompat.startTracking(event);
             return true;
         }
         return super.onKeyDown(keyCode, event);
     }
 
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_BACK && hasVisibleDrawer()) {
             closeDrawers();
             return true;
         }
         return super.onKeyUp(keyCode, event);
     }
 
     @Override
     protected void onRestoreInstanceState(Parcelable state) {
         final SavedState ss = (SavedState) state;
         super.onRestoreInstanceState(ss.getSuperState());
 
         if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
             final View toOpen = findDrawerWithGravity(ss.openDrawerGravity);
             if (toOpen != null) {
                 openDrawer(toOpen);
             }
         }
     }
 
     @Override
     protected Parcelable onSaveInstanceState() {
         final Parcelable superState = super.onSaveInstanceState();
 
         final SavedState ss = new SavedState(superState);
 
         final int childCount = getChildCount();
         for (int i = 0; i < childCount; i++) {
             final View child = getChildAt(i);
             if (!isDrawerView(child)) {
                 continue;
             }
 
             final LayoutParams lp = (LayoutParams) child.getLayoutParams();
             if (lp.knownOpen) {
                 ss.openDrawerGravity = lp.gravity;
                 // Only one drawer can be open at a time.
                 break;
             }
         }
 
         return ss;
     }
 
     /**
      * State persisted across instances
      */
     protected static class SavedState extends BaseSavedState {
         int openDrawerGravity = Gravity.NO_GRAVITY;
 
         public SavedState(Parcel in) {
             super(in);
             openDrawerGravity = in.readInt();
         }
 
         public SavedState(Parcelable superState) {
             super(superState);
         }
 
         @Override
         public void writeToParcel(Parcel dest, int flags) {
             super.writeToParcel(dest, flags);
             dest.writeInt(openDrawerGravity);
         }
 
         public static final Parcelable.Creator<SavedState> CREATOR =
                 new Parcelable.Creator<SavedState>() {
             @Override
             public SavedState createFromParcel(Parcel source) {
                 return new SavedState(source);
             }
 
             @Override
             public SavedState[] newArray(int size) {
                 return new SavedState[size];
             }
         };
     }
 
     private class ViewDragCallback extends ViewDragHelper.Callback {
 
         private final int mGravity;
         private ViewDragHelper mDragger;
 
         public ViewDragCallback(int gravity) {
             mGravity = gravity;
         }
 
         public void setDragger(ViewDragHelper dragger) {
             mDragger = dragger;
         }
 
         @Override
         public boolean tryCaptureView(View child, int pointerId) {
             // Only capture views where the gravity matches what we're looking for.
             // This lets us use two ViewDragHelpers, one for each side drawer.
             return isDrawerView(child) && checkDrawerViewGravity(child, mGravity);
         }
 
         @Override
         public void onViewDragStateChanged(int state) {
             updateDrawerState(mGravity, state, mDragger.getCapturedView());
         }
 
         @Override
         public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
             float offset;
             final int childWidth = changedView.getWidth();
 
             // This reverses the positioning shown in onLayout.
             if (checkDrawerViewGravity(changedView, Gravity.LEFT)) {
                 offset = (float) (childWidth + left) / childWidth;
             } else {
                 final int width = getWidth();
                 offset = (float) (width - left) / childWidth;
             }
             setDrawerViewOffset(changedView, offset);
             changedView.setVisibility(offset == 0 ? INVISIBLE : VISIBLE);
             invalidate();
         }
 
         @Override
         public void onViewCaptured(View capturedChild, int activePointerId) {
             final LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
             lp.isPeeking = false;
 
             closeOtherDrawer();
         }
 
         private void closeOtherDrawer() {
             final int otherGrav = mGravity == Gravity.LEFT ? Gravity.RIGHT : Gravity.LEFT;
             final View toClose = findDrawerWithGravity(otherGrav);
             if (toClose != null) {
                 closeDrawer(toClose);
             }
         }
 
         @Override
         public void onViewReleased(View releasedChild, float xvel, float yvel) {
             // Offset is how open the drawer is, therefore left/right values
             // are reversed from one another.
             final float offset = getDrawerViewOffset(releasedChild);
             final int childWidth = releasedChild.getWidth();
 
             int left;
             if (checkDrawerViewGravity(releasedChild, Gravity.LEFT)) {
                 left = xvel > 0 || xvel == 0 && offset > 0.5f ? 0 : -childWidth;
             } else {
                 final int width = getWidth();
                 left = xvel < 0 || xvel == 0 && offset < 0.5f ? width - childWidth : width;
             }
 
             mDragger.settleCapturedViewAt(left, releasedChild.getTop());
             invalidate();
         }
 
         @Override
         public void onEdgeTouched(int edgeFlags, int pointerId) {
             final View toCapture;
             final int childLeft;
             final boolean leftEdge =
                     (edgeFlags & ViewDragHelper.EDGE_LEFT) == ViewDragHelper.EDGE_LEFT;
             if (leftEdge) {
                 toCapture = findDrawerWithGravity(Gravity.LEFT);
                 childLeft = -toCapture.getWidth() + mDrawerPeekDistance;
             } else {
                 toCapture = findDrawerWithGravity(Gravity.RIGHT);
                 childLeft = getWidth() - mDrawerPeekDistance;
             }
 
             // Only peek if it would mean making the drawer more visible
             if (toCapture != null && ((leftEdge && toCapture.getLeft() < childLeft) ||
                     (!leftEdge && toCapture.getLeft() > childLeft))) {
                 final LayoutParams lp = (LayoutParams) toCapture.getLayoutParams();
                 mDragger.smoothSlideViewTo(toCapture, childLeft, toCapture.getTop());
                 lp.isPeeking = true;
                 invalidate();
 
                 closeOtherDrawer();
             }
         }
 
         @Override
         public void onEdgeDragStarted(int edgeFlags, int pointerId) {
             final View toCapture;
             if ((edgeFlags & ViewDragHelper.EDGE_LEFT) == ViewDragHelper.EDGE_LEFT) {
                 toCapture = findDrawerWithGravity(Gravity.LEFT);
             } else {
                 toCapture = findDrawerWithGravity(Gravity.RIGHT);
             }
 
             if (toCapture != null) {
                 mDragger.captureChildView(toCapture, pointerId);
             }
         }
 
         @Override
         public int getViewHorizontalDragRange(View child) {
             return child.getWidth();
         }
 
         @Override
         public int clampViewPositionHorizontal(View child, int left, int dx) {
             if (checkDrawerViewGravity(child, Gravity.LEFT)) {
                 return Math.max(-child.getWidth(), Math.min(left, 0));
             } else {
                 final int width = getWidth();
                 return Math.max(width - child.getWidth(), Math.min(left, width));
             }
         }
     }
 
     public static class LayoutParams extends ViewGroup.LayoutParams {
 
         public int gravity = Gravity.NO_GRAVITY;
         float onScreen;
         boolean isPeeking;
         boolean knownOpen;
 
         public LayoutParams(Context c, AttributeSet attrs) {
             super(c, attrs);
 
             final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
             this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
             a.recycle();
         }
 
         public LayoutParams(int width, int height) {
             super(width, height);
         }
 
         public LayoutParams(int width, int height, int gravity) {
             this(width, height);
             this.gravity = gravity;
         }
 
         public LayoutParams(LayoutParams source) {
             super(source);
             this.gravity = source.gravity;
         }
 
         public LayoutParams(ViewGroup.LayoutParams source) {
             super(source);
         }
     }
 }
