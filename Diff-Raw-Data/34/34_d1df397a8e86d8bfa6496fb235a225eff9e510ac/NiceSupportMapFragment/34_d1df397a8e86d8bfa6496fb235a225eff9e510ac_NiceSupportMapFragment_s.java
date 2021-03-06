 package com.NYXDigital;
 
 import android.annotation.SuppressLint;
 import android.graphics.PixelFormat;
 import android.os.Bundle;
 import android.view.LayoutInflater;
 import android.view.MotionEvent;
 import android.view.SurfaceHolder;
 import android.view.SurfaceView;
 import android.view.TextureView;
 import android.view.View;
 import android.view.View.OnTouchListener;
 import android.view.ViewGroup;
 
 import com.google.android.gms.maps.SupportMapFragment;
 
 public class NiceSupportMapFragment extends SupportMapFragment {
 
 	private View drawingView;
 	private boolean hasTextureViewSupport = false;
 	private boolean preventParentScrolling = true;
	private boolean useZOnTopFix = false;
 
 	private boolean textureViewSupport() {
 		boolean exist = true;
 		try {
 			Class.forName("android.view.TextureView");
 		} catch (ClassNotFoundException e) {
 			exist = false;
 		}
 		return exist;
 	}
 
 	private View searchAndFindDrawingView(ViewGroup group) {
 		int childCount = group.getChildCount();
 		for (int i = 0; i < childCount; i++) {
 			View child = group.getChildAt(i);
 			if (child instanceof ViewGroup) {
 				View view = searchAndFindDrawingView((ViewGroup) child);
 
 				if (view != null) {
					return view;
 				}
 			}
 
 			if (child instanceof SurfaceView) {
 				return (View) child;
 			}
 
 			if (hasTextureViewSupport) { // if we have support for texture view
 				if (child instanceof TextureView) {
 					return (View) child;
 				}
 			}
 		}
 		return null;
 	}
 
 	@SuppressLint("NewApi")
 	public View onCreateView(LayoutInflater inflater, ViewGroup container,
 			Bundle savedInstanceState) {
 
 		ViewGroup view = (ViewGroup) super.onCreateView(inflater, container,
 				savedInstanceState);
 		view.setBackgroundColor(0x00000000); // Set Root View to be transparent to prevent black screen on load
 
 		hasTextureViewSupport = textureViewSupport(); // Find out if we support texture view on this device
 		drawingView = searchAndFindDrawingView(view); // Find the view the map is using for Open GL
 
		if (drawingView == null)
			return view; // If we didn't get anything then abort
 		
 		// texture view
 		if (hasTextureViewSupport) { // If we support texture view and the
 										// drawing view is a TextureView then
 										// tweak it and return the fragment view
 
 			if (drawingView instanceof TextureView) {
 
 				TextureView textureView = (TextureView) drawingView;
 
 				// Stop Containing Views from moving when a user is interacting
 				// with Map View Directly
 				textureView.setOnTouchListener(new OnTouchListener() {
 					public boolean onTouch(View view, MotionEvent event) {
 						view.getParent().requestDisallowInterceptTouchEvent(
 								preventParentScrolling);
 						return false;
 					}
 				});
 
 				return view;
 			}
 
 		}
 
 		// Otherwise continue onto legacy surface view hack
 		final SurfaceView surfaceView = (SurfaceView) drawingView;
 
		if (useZOnTopFix) {
			surfaceView.setZOrderOnTop(true); // Optional fix for flicker, works but can cause overlapping issues
		}

		// Janky "fix" to prevent artifacts when embedding GoogleMaps in a
		// sliding view on older devices.
		// https://github.com/jfeinstein10/SlidingMenu/issues/168

 		// Fix for reducing black view flash issues
		surfaceView.setZOrderMediaOverlay(true);

 		SurfaceHolder holder = surfaceView.getHolder();
		holder.setFormat(PixelFormat.RGB_565);
 
 		// Stop Containing Views from moving when a user is interacting with
 		// Map View Directly
 		surfaceView.setOnTouchListener(new OnTouchListener() {
 			public boolean onTouch(View view, MotionEvent event) {
 				view.getParent().requestDisallowInterceptTouchEvent(
 						preventParentScrolling);
 				return false;
 			}
 		});
 
 		return view;
 	}
 
 	public boolean getPreventParentScrolling() {
 		return preventParentScrolling;
 	}
 
 	public void setPreventParentScrolling(boolean value) {
 		preventParentScrolling = value;
 	}
 
	public boolean isUseZOnTopFix() {
		return useZOnTopFix;
	}

	public void setUseZOnTopFix(boolean useZOnTopFix) {
		this.useZOnTopFix = useZOnTopFix;
	}

 }
