 /*
  * Copyright (C) 2007-2010 Geometer Plus <contact@geometerplus.com>
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
  * 02110-1301, USA.
  */
 
 package org.geometerplus.zlibrary.ui.android.view;
 
 import android.content.Context;
 import android.graphics.*;
 import android.view.*;
 import android.util.AttributeSet;
 
 import org.geometerplus.zlibrary.core.view.ZLView;
 import org.geometerplus.zlibrary.core.application.ZLApplication;
 
 // TODO: remove this dependency!!!
 import org.geometerplus.zlibrary.text.view.ZLTextView;
 
 import org.geometerplus.zlibrary.ui.android.library.ZLAndroidActivity;
 import org.geometerplus.zlibrary.ui.android.util.ZLAndroidKeyUtil;
 import org.geometerplus.zlibrary.ui.android.view.ZLFooter;
 
 public class ZLAndroidWidget extends View {
 	private final Paint myPaint = new Paint();
 	private Bitmap myMainBitmap;
 	private Bitmap mySecondaryBitmap;
 	private boolean mySecondaryBitmapIsUpToDate;
 	private boolean myScrollingInProgress;
 	private int myScrollingShift;
 	private float myScrollingSpeed;
 	private int myScrollingBound;
 	private ZLFooter myFooter = new ZLFooter();
 
 	public ZLAndroidWidget(Context context, AttributeSet attrs, int defStyle) {
 		super(context, attrs, defStyle);
 		setDrawingCacheEnabled(false);
 	}
 
 	public ZLAndroidWidget(Context context, AttributeSet attrs) {
 		super(context, attrs);
 		setDrawingCacheEnabled(false);
 	}
 
 	public ZLAndroidWidget(Context context) {
 		super(context);
 		setDrawingCacheEnabled(false);
 	}
 
 	public ZLAndroidPaintContext getPaintContext() {
 		return ZLAndroidPaintContext.Instance();
 	}
 
 	private Point myViewSize = new Point();
 	// ensure children objects has correct information about widget size
 	private void ensureChildrenSizes() {
 		int width = getWidth();
 		int height = getHeight();
 		if (myViewSize.x != width || myViewSize.y != height) {
 			// pass draw area size to footer
 			// it need need height also, because canvas.getHeight() is buggy)
 			myFooter.setDrawAreaSize(width, height);
 			myViewSize.x = width;
 			myViewSize.y = height;
 		}
 	}
 
 	@Override
 	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
 		super.onSizeChanged(w, h, oldw, oldh);
 		if (myScreenIsTouched) {
 			final ZLView view = ZLApplication.Instance().getCurrentView();
 			myScrollingInProgress = false;
 			myScrollingShift = 0;
 			myScreenIsTouched = false;
 			view.onScrollingFinished(ZLView.PAGE_CENTRAL);
 			setPageToScroll(ZLView.PAGE_CENTRAL);
 		}
 
 		ensureChildrenSizes();
 	}
 
 	@Override
 	protected void onDraw(final Canvas canvas) {
 		final Context context = getContext();
 		if (context instanceof ZLAndroidActivity) {
 			((ZLAndroidActivity)context).createWakeLock();
 		} else {
 			System.err.println("A surprise: view's context is not a ZLAndroidActivity");
 		}
 		super.onDraw(canvas);
 
 		ensureChildrenSizes();
 		final int w = getWidth();
 		final int h = getTextViewHeight();
 
 		if ((myMainBitmap != null) && ((myMainBitmap.getWidth() != w) || (myMainBitmap.getHeight() != h))) {
 			myMainBitmap = null;
 			mySecondaryBitmap = null;
 			System.gc();
 			System.gc();
 			System.gc();
 		}
 		if (myMainBitmap == null) {
 			myMainBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
 			mySecondaryBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
 			mySecondaryBitmapIsUpToDate = false;
 			drawOnBitmap(myMainBitmap);
 		}
 
 		if (myScrollingInProgress || (myScrollingShift != 0)) {
 			onDrawInScrolling(canvas);
 		} else {
 			onDrawStatic(canvas);
 			ZLApplication.Instance().onRepaintFinished();
 		}
 	}
 
 	private void onDrawInScrolling(Canvas canvas) {
 		final int w = getWidth();
 		final int h = getTextViewHeight();
 		//final ZLAndroidPaintContext context = ZLAndroidPaintContext.Instance();
 
 		boolean stopScrolling = false;
 		if (myScrollingInProgress) {
 			myScrollingShift += (int)myScrollingSpeed;
 			if (myScrollingSpeed > 0) {
 				if (myScrollingShift >= myScrollingBound) {
 					myScrollingShift = myScrollingBound;
 					stopScrolling = true;
 				}
 			} else {
 				if (myScrollingShift <= myScrollingBound) {
 					myScrollingShift = myScrollingBound;
 					stopScrolling = true;
 				}
 			}
 			myScrollingSpeed *= 1.5;
 		}
 		final boolean horizontal =
 			(myViewPageToScroll == ZLView.PAGE_RIGHT) ||
 			(myViewPageToScroll == ZLView.PAGE_LEFT);
 		canvas.drawBitmap(
 			myMainBitmap,
 			horizontal ? myScrollingShift : 0,
 			horizontal ? 0 : myScrollingShift,
 			myPaint
 		);
 		final int size = horizontal ? w : h;
 		int shift = (myScrollingShift < 0) ? (myScrollingShift + size) : (myScrollingShift - size);
 		canvas.drawBitmap(
 			mySecondaryBitmap,
 			horizontal ? shift : 0,
 			horizontal ? 0 : shift,
 			myPaint
 		);
 
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (((ZLTextView)view).scrollbarType() == ZLTextView.SCROLLBAR_SHOW_AS_FOOTER) {
 			myFooter.onDraw(canvas, getScrollProgress());
 		}
 
 		if (stopScrolling) {
 			if (myScrollingBound != 0) {
 				Bitmap swap = myMainBitmap;
 				myMainBitmap = mySecondaryBitmap;
 				mySecondaryBitmap = swap;
 				mySecondaryBitmapIsUpToDate = false;
 				view.onScrollingFinished(myViewPageToScroll);
 				ZLApplication.Instance().onRepaintFinished();
 			} else {
 				view.onScrollingFinished(ZLView.PAGE_CENTRAL);
 			}
 			setPageToScroll(ZLView.PAGE_CENTRAL);
 			myScrollingInProgress = false;
 			myScrollingShift = 0;
 		} else {
 			if (shift < 0) {
 				shift += size;
 			}
 			// TODO: set color
 			myPaint.setColor(Color.rgb(127, 127, 127));
 			if (horizontal) {
 				canvas.drawLine(shift, 0, shift, h + 1, myPaint);
 			} else {
 				canvas.drawLine(0, shift, w + 1, shift, myPaint);
 			}
 			if (myScrollingInProgress) {
 				postInvalidate();
 			}
 		}
 	}
 
 	private int myViewPageToScroll = ZLView.PAGE_CENTRAL;
 	private void setPageToScroll(int viewPage) {
 		if (myViewPageToScroll != viewPage) {
 			myViewPageToScroll = viewPage;
 			mySecondaryBitmapIsUpToDate = false;
 		}
 	}
 
 	void scrollToPage(int viewPage, int shift) {
 		switch (viewPage) {
 			case ZLView.PAGE_BOTTOM:
 			case ZLView.PAGE_RIGHT:
 				shift = -shift;
 				break;
 		}
 
 		if (myMainBitmap == null) {
 			return;
 		}
 		if (((shift > 0) && (myScrollingShift <= 0)) ||
 			((shift < 0) && (myScrollingShift >= 0))) {
 			mySecondaryBitmapIsUpToDate = false;
 		}
 		myScrollingShift = shift;
 		setPageToScroll(viewPage);
 		drawOnBitmap(mySecondaryBitmap);
 		postInvalidate();
 	}
 
 	void startAutoScrolling(int viewPage) {
 		if (myMainBitmap == null) {
 			return;
 		}
 		myScrollingInProgress = true;
 		switch (viewPage) {
 			case ZLView.PAGE_CENTRAL:
 				switch (myViewPageToScroll) {
 					case ZLView.PAGE_CENTRAL:
 						myScrollingSpeed = 0;
 						break;
 					case ZLView.PAGE_LEFT:
 					case ZLView.PAGE_TOP:
 						myScrollingSpeed = -3;
 						break;
 					case ZLView.PAGE_RIGHT:
 					case ZLView.PAGE_BOTTOM:
 						myScrollingSpeed = 3;
 						break;
 				}
 				myScrollingBound = 0;
 				break;
 			case ZLView.PAGE_LEFT:
 				myScrollingSpeed = 3;
 				myScrollingBound = getWidth();
 				break;
 			case ZLView.PAGE_RIGHT:
 				myScrollingSpeed = -3;
 				myScrollingBound = -getWidth();
 				break;
 			case ZLView.PAGE_TOP:
 				myScrollingSpeed = 3;
 				myScrollingBound = getTextViewHeight();
 				break;
 			case ZLView.PAGE_BOTTOM:
 				myScrollingSpeed = -3;
 				myScrollingBound = -getTextViewHeight();
 				break;
 		}
 		if (viewPage != ZLView.PAGE_CENTRAL) {
 			setPageToScroll(viewPage);
 		}
 		drawOnBitmap(mySecondaryBitmap);
 		postInvalidate();
 	}
 
 	private void drawOnBitmap(Bitmap bitmap) {
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (view == null) {
 			return;
 		}
 
 		if (bitmap == myMainBitmap) {
 			mySecondaryBitmapIsUpToDate = false;
 		} else if (mySecondaryBitmapIsUpToDate) {
 			return;
 		} else {
 			mySecondaryBitmapIsUpToDate = true;
 		}
 
 		final ZLAndroidPaintContext context = ZLAndroidPaintContext.Instance();
 		Canvas canvas = new Canvas(bitmap);
 		context.beginPaint(canvas);
 		context.setSize(getWidth(), getTextViewHeight(), getWidth() - getTextViewWidth());
 		view.paint((bitmap == myMainBitmap) ? ZLView.PAGE_CENTRAL : myViewPageToScroll);
 		context.endPaint();
 	}
 
 	private void onDrawStatic(Canvas canvas) {
 		drawOnBitmap(myMainBitmap);
 		canvas.drawBitmap(myMainBitmap, 0, 0, myPaint);
 		if (((ZLTextView)ZLApplication.Instance().getCurrentView()).scrollbarType() == ZLTextView.SCROLLBAR_SHOW_AS_FOOTER) {
 			myFooter.onDraw(canvas, getScrollProgress());
 		}
 	}
 
 	@Override
 	public boolean onTrackballEvent(MotionEvent event) {
 		if (event.getAction() == MotionEvent.ACTION_DOWN) {
 			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
 		} else {
 			ZLApplication.Instance().getCurrentView().onTrackballRotated((int)(10 * event.getX()), (int)(10 * event.getY()));
 		}
 		return true;
 	}
 
 
 	private class LongClickRunnable implements Runnable {
 		public void run() {
 			if (performLongClick()) {
 				myLongClickPerformed = true;
 			}
 		}
 	}
 
 	private LongClickRunnable myPendingLongClickRunnable;
 	private boolean myLongClickPerformed;
 
 	private void postLongClickRunnable() {
         myLongClickPerformed = false;
         if (myPendingLongClickRunnable == null) {
             myPendingLongClickRunnable = new LongClickRunnable();
         }
         postDelayed(myPendingLongClickRunnable, 2 * ViewConfiguration.getLongPressTimeout());
     }
 
 	private boolean myPendingPress;
 	private int myPressedX, myPressedY;
 	private boolean myScreenIsTouched;
 	@Override
 	public boolean onTouchEvent(MotionEvent event) {
 		int x = (int)event.getX();
 		int y = (int)event.getY();
 
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		switch (event.getAction()) {
 			case MotionEvent.ACTION_UP:
 				if (!myLongClickPerformed) {
 					if (myPendingLongClickRunnable != null) {
 						removeCallbacks(myPendingLongClickRunnable);
 					}
 					if (myPendingPress) {
 						if (y > getHeight() - myFooter.getTapHeight()) {
 							myFooter.setProgress(view, myPressedX);
 						}
 						else {
 							view.onStylusPress(myPressedX, myPressedY);
 						}
 					}
 					view.onStylusRelease(x, y);
 				}
 				myPendingPress = false;
 				myScreenIsTouched = false;
 				break;
 			case MotionEvent.ACTION_DOWN:
 				postLongClickRunnable();
 				myScreenIsTouched = true;
 				myPendingPress = true;
 				myPressedX = x;
 				myPressedY = y;
 				break;
 			case MotionEvent.ACTION_MOVE:
 				if (!myLongClickPerformed) {
 					if (myPendingPress) {
 						final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
 						if (Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop) {
 							if (myPendingLongClickRunnable != null) {
 								removeCallbacks(myPendingLongClickRunnable);
 							}
 							view.onStylusMovePressed(myPressedX, myPressedY);
 							myPendingPress = false;
 						}
 					}
 					if (!myPendingPress) {
 						view.onStylusMovePressed(x, y);
 					}
 				}
 				break;
 		}
 
 		return true;
 	}
 
 	public boolean onKeyDown(int keyCode, KeyEvent event) {
 		switch (keyCode) {
 			case KeyEvent.KEYCODE_VOLUME_DOWN:
 			case KeyEvent.KEYCODE_VOLUME_UP:
 			case KeyEvent.KEYCODE_BACK:
 			case KeyEvent.KEYCODE_ENTER:
 			case KeyEvent.KEYCODE_DPAD_CENTER:
 				return ZLApplication.Instance().doActionByKey(ZLAndroidKeyUtil.getKeyNameByCode(keyCode));
 			case KeyEvent.KEYCODE_DPAD_DOWN:
 				ZLApplication.Instance().getCurrentView().onTrackballRotated(0, 1);
 				return true;
 			case KeyEvent.KEYCODE_DPAD_UP:
 				ZLApplication.Instance().getCurrentView().onTrackballRotated(0, -1);
 				return true;
 			default:
 				return false;
 		}
 	}
 
 	public boolean onKeyUp(int keyCode, KeyEvent event) {
 		switch (keyCode) {
 			case KeyEvent.KEYCODE_VOLUME_DOWN:
 			case KeyEvent.KEYCODE_VOLUME_UP:
 			case KeyEvent.KEYCODE_BACK:
 			case KeyEvent.KEYCODE_ENTER:
 			case KeyEvent.KEYCODE_DPAD_CENTER:
 				return true;
 			default:
 				return false;
 		}
 	}
 
 	protected int computeVerticalScrollExtent() {
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (!view.isScrollbarShown()) {
 			return 0;
 		}
 		if (myScrollingInProgress || (myScrollingShift != 0)) {
 			final int from = view.getScrollbarThumbLength(ZLView.PAGE_CENTRAL);
 			final int to = view.getScrollbarThumbLength(myViewPageToScroll);
 			final boolean horizontal =
 				(myViewPageToScroll == ZLView.PAGE_RIGHT) ||
 				(myViewPageToScroll == ZLView.PAGE_LEFT);
 			final int size = horizontal ? getWidth() : getTextViewHeight();
 			final int shift = Math.abs(myScrollingShift);
 			return (from * (size - shift) + to * shift) / size;
 		} else {
 			return view.getScrollbarThumbLength(ZLView.PAGE_CENTRAL);
 		}
 	}
 
 	protected int computeVerticalScrollOffset() {
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (!view.isScrollbarShown()) {
 			return 0;
 		}
 		if (myScrollingInProgress || (myScrollingShift != 0)) {
 			final int from = view.getScrollbarThumbPosition(ZLView.PAGE_CENTRAL);
 			final int to = view.getScrollbarThumbPosition(myViewPageToScroll);
 			final boolean horizontal =
 				(myViewPageToScroll == ZLView.PAGE_RIGHT) ||
 				(myViewPageToScroll == ZLView.PAGE_LEFT);
 			final int size = horizontal ? getWidth() : getTextViewHeight();
 			final int shift = Math.abs(myScrollingShift);
 			return (from * (size - shift) + to * shift) / size;
 		} else {
 			return view.getScrollbarThumbPosition(ZLView.PAGE_CENTRAL);
 		}
 	}
 
 	protected int computeVerticalScrollRange() {
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (!view.isScrollbarShown()) {
 			return 0;
 		}
 		return view.getScrollbarFullSize();
 	}
 
 	private int getTextViewHeight() {
 		int height = getHeight();
 		final ZLView view = ZLApplication.Instance().getCurrentView();
 		if (((ZLTextView)view).scrollbarType() == ZLTextView.SCROLLBAR_SHOW_AS_FOOTER) {
 			height -= myFooter.getHeight();
 		}
 		return height;
 	}
 
 	private int getTextViewWidth() {
 		int width = getWidth();
 		ZLView view = ZLApplication.Instance().getCurrentView();
		if (view != null && view.showScrollbar()) {
 			width -= getVerticalScrollbarWidth();
 		}
 		return width;
 	}
 
 	private float getScrollProgress() {
 		// get percentage of page scroll (from 0 to 1)
 		final boolean horizontal =
 			(myViewPageToScroll == ZLView.PAGE_RIGHT) || (myViewPageToScroll == ZLView.PAGE_LEFT);
 		final float pageSize = horizontal ? getTextViewWidth() : getTextViewHeight();
 		return (float)myScrollingShift / pageSize;
 	}
 }
