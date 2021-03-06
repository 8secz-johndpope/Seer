 package com.android.internal.widget;
 
 import android.content.Context;
 import android.content.res.TypedArray;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Matrix;
 import android.graphics.Paint;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.Display;
 import android.view.MotionEvent;
 import android.view.Surface;
 import android.view.View;
 import android.view.WindowManager;
 
 public class CircularSelector extends View{
 	
 	
 	// ********************* Debug Variables
 	
 	
 	private String TAG = "CircularSelector";
 	private static final boolean DBG = true;
 	private static final boolean IDBG = true;
 	private static final boolean TDBG = false;
     private static final boolean VISUAL_DEBUG = true;
 	
     
     // ***********Rotation constants and variables
     /**
      * Either {@link #HORIZONTAL} or {@link #VERTICAL}.
      */	
 	 private int mOrientation;
 
 	 public static final int HORIZONTAL = 0;
 	 public static final int VERTICAL = 1;
     
 	 
 	 // ********************* UI Elements
 	 
 	   final Matrix mBgMatrix = new Matrix();
 	   private Paint mPaint = new Paint();
 	   
 	   
 	   // *** Circular areas **
 	   Bitmap mPortraitCircle;
 	   Bitmap mLandscapeCircle;
 	   Bitmap mLockIcon;
 	   private int mLockX, mLockY;
 	   private boolean mIsTouchInCircle = false;
 	   
 	   private float mDensity;
 	   
 	   // ***************
 	   private OnCircularSelectorTriggerListener mCircularTriggerListener;
 	   private int  mGrabbedState = OnCircularSelectorTriggerListener.ICON_GRABBED_STATE_NONE;
 	 
 	 
     //
     //********************** Constructors**********
 	//
 	public CircularSelector(Context context) {
 		this(context,null);
 		// TODO Auto-generated constructor stub
 	}
 	public CircularSelector(Context context, AttributeSet attrs) {
 		super(context,attrs);
 		
 		   TypedArray a =
 	            context.obtainStyledAttributes(attrs, R.styleable.CircularSelector);
 		   // TODO obtain proper orientaion
 		   
 	        mOrientation = a.getInt(R.styleable.CircularSelector_orientation, VERTICAL);
 
 	        a.recycle();
 	        
 	        initializeUI();
 		// TODO Auto-generated constructor stub
 	}
 	
 	//**************** Overridden super methods
 	
 	@Override 
 	public boolean onTouchEvent(MotionEvent event){
 		super.onTouchEvent(event);
 		
 		final int height = getHeight();
 		final int width  = getWidth();
 		
 		final int action = event.getAction();
 		
 	
 		final int eventX = (int) event.getX();
                 
         final int eventY = (int) event.getY();
 
         if (DBG) log("x -" + eventX + " y -" + eventY);
     	                
 		switch (action) {
         case MotionEvent.ACTION_DOWN:
             if (DBG) log("touch-down");
             /* If the event is lower thatn the inner radius than cause the lock icon to move the 
              * position
              */
      
             	if(isVertical() ? isYUnderArc(width/2, eventY, eventX, height, width) : isYWithinCircle(width/3, eventY, eventX, height, width) || TDBG){
             
            	if (DBG) log("touch-down within the arc or circle");
            	setLockXY(eventX, eventY);
            	mIsTouchInCircle = true;
            	setGrabbedState(OnCircularSelectorTriggerListener.ICON_GRABBED_STATE_GRABBED);
            	invalidate();
             	}
            	
        
             break;
 
         case MotionEvent.ACTION_MOVE:
             if (DBG) log("touch-move");
             
            if(isVertical() ?isYUnderArc(width/2, eventY, eventX, height, width) : isYWithinCircle(width/3, eventY, eventX, height, width) || TDBG){
             	if (DBG) log("touch-move within the arc or circle");
             	setLockXY(eventX, eventY);
             	mIsTouchInCircle = true;
             	setGrabbedState(OnCircularSelectorTriggerListener.ICON_GRABBED_STATE_GRABBED);
             	invalidate();
             	
             }
            else{
             	// If the lock moved out of the area when moving then we need 
            	// to dispatch the trigger
             	
             	dispatchTriggerEvent(OnCircularSelectorTriggerListener.LOCK_ICON_TRIGGERED);	// TODO: Set propper trigger dispenser
             	reset();
             	invalidate();
             	
             }
             break;
         case MotionEvent.ACTION_UP:
             if (DBG) log("touch-up");
             reset();
             invalidate();
             break;
         case MotionEvent.ACTION_CANCEL:
             if (DBG) log("touch-cancel");
             reset();
             invalidate();
         
     }
 		
 		return true;
 		
 	}
 	
 	
 	@Override 
 	public void onDraw(Canvas canvas){
 		super.onDraw(canvas);
 		
 		  if (IDBG) log("Redrawing the view");
 
           final int width = getWidth();
           final int height = getHeight();
           final int halfWidth = width/2;
           final int halfHeight = height/2;
 
  	    
           if (DBG) log("The width of the view is " + width + " and the hieght of the veiw is " + height );
 
           if (VISUAL_DEBUG) {
               // draw bounding box around widget
 
 			  if (IDBG) log("Debugging the widget visibly");
               mPaint.setColor(0xffff0000);
               mPaint.setStyle(Paint.Style.STROKE);
               canvas.drawRect(0, 0, width, height , mPaint);
             if(isVertical()){
             	 canvas.drawCircle(halfWidth, height, halfWidth, mPaint);
               }else{
             	  canvas.drawCircle(halfWidth, halfHeight, width/3, mPaint);
               }
           }
           
           
           if(isVertical()){
         	  canvas.drawBitmap(this.mPortraitCircle,  0, 0, mPaint);
           }
           else{
         	  
         	  canvas.drawBitmap(this.mLandscapeCircle, width-mLandscapeCircle.getWidth(), (height-mLandscapeCircle.getHeight())/2, mPaint);
           }
           
           if(mIsTouchInCircle)	
         	  
         	  
         		  canvas.drawBitmap(mLockIcon,  mLockX-(mLockIcon.getWidth()/2), mLockY - mLockIcon.getHeight()/2, mPaint);
         	
           
           else{
         	  if(isVertical()){
 	        	  // Fallback case where the lock is always drawn in the center on the bottom of the view
 	        	   canvas.drawBitmap(mLockIcon,  (width/2)-(mLockIcon.getWidth()/2), height-mLockIcon.getHeight(), mPaint);
 	     	  }else{
 	    		// TODO  
 	     		 canvas.drawBitmap(mLockIcon,  (width - mLockIcon.getWidth())/2, (height -mLockIcon.getHeight())/2, mPaint);
 	    	  }
         	  
           }
 		
 		
 		return;
 	}
 	
     @Override 
     protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
     	   
 		  if (IDBG) log("Measuring the demensions of the view");
     	   
 		  
 		  final int length = isVertical() ?
                   MeasureSpec.getSize(widthMeasureSpec) :
                   MeasureSpec.getSize(heightMeasureSpec);
                   
       	final int height = (isVertical() ?
                       (MeasureSpec.getSize(heightMeasureSpec)/5)*2 :
                       MeasureSpec.getSize(widthMeasureSpec)/2);
 		  
 		 
                 
 
 
 		  if (DBG) log("The demensions of the view is length:" + length + " and height: " + height );
            if (isVertical()) {
                setMeasuredDimension(length, height);
            } else {
                setMeasuredDimension(height, length);
            }
        }
 
     // ************** Interfacees
     
   
     
     // ************* Initilization function
     
     private void initializeUI(){
     	mPortraitCircle = getBitmapFor(R.drawable.lock_ic_port_circ);
     	mLandscapeCircle =  getBitmapFor(R.drawable.lock_ic_land_circ);
     	mLockIcon = getBitmapFor(R.drawable.lock_ic_lock);
     }
     
     
     
     // ***********
     /**
      * Assuming bitmap is a bounding box around a piece of an arc drawn by two concentric circles
      * (as the background drawable for the portrait circular widget is), and given an x and y coordinate along the
      * drawable, return false if the the radius for the touch points is greater than the touch circle.
      * This is accomplished by shifting the touch onto a cartesian plane with the bootom of the
      * view defined as Y = 0 and the middle of the view defined as X = 0. With the knowledge of
      * the radius of the circle and the touch points a radius can be  
      * 
      * y coordinate of a point on the arc that is between the two concentric
      * circles.  The resulting y combined with the incoming x is a point along the circle in
      * between the two concentric circles.
      * 
      * 
      * For use when in portrait mode
      *
      * 
      * @param innerRadius The radius of the circle that intersects the drawable at the bottom two
      *        corders of the drawable (top two corners in terms of drawing coordinates).
      * @param y The distance along the y axis of the touch point. 
      * @param x The distance along the x axis of the touchpoint.   
      * @param height The height of the view
      * @param width The width of the view
      * @return False if the radius of the touch point is greater than the radius of the touch circle 
      */
     private boolean isYUnderArc(int innerRadius, int y, int x, int height, int width) {
 
     	int CartesianX = width/2; // The x point directly in the middle of the view
     	int CartesianY = height;  // The Y point, at the bottom of the view
     	
     	
     	
     	int YRadiusUnderArc = innerRadius;
     	
     	int CartesianShiftTouchX;
     	int CartesianShiftTouchY; 
     	
     	if(x < CartesianX)
     		CartesianShiftTouchX = CartesianX - x;
     	else
     		 CartesianShiftTouchX = x - CartesianX;
     	
     	if(y < CartesianY)
     		CartesianShiftTouchY = CartesianX - y;
     	else
     		 CartesianShiftTouchY = y - CartesianY;
     	
     	
     	
     	int YTouchRadius = (int) Math.sqrt((CartesianShiftTouchX*CartesianShiftTouchX) + (CartesianShiftTouchY*CartesianShiftTouchY));
     	
     	if(YTouchRadius > YRadiusUnderArc)
     		return false;
     	else 
     		return true;
     	
     	
     	
   
  
     }
     
     /**
      * For use in landscape mode
      */
     private boolean isYWithinCircle(int innerRadius, int y, int x, int height, int width) {
 
     	int YRadiusUnderArc = innerRadius;
       	int CartesianX = width/2; // The x point directly in the middle of the view
     	int CartesianY = height/2;  // The Y point directly in the middle of the view
     	int CartesianShiftTouchX;
     	int CartesianShiftTouchY; 
     	
     	
     	if(x > CartesianX)
     		CartesianShiftTouchX = CartesianX - x;
     	else
     		 CartesianShiftTouchX = x - CartesianX;
     	
     	if(y > CartesianY)
     		CartesianShiftTouchY = CartesianX - y;
     	else
     		 CartesianShiftTouchY = y - CartesianY;
     	//TODO: Add functionality for landscape circle
     	
     	int YTouchRadius = (int) Math.sqrt((CartesianShiftTouchX*CartesianShiftTouchX) + (CartesianShiftTouchY*CartesianShiftTouchY));
     	
     	if(YTouchRadius > YRadiusUnderArc)
     		return false;
     	else 
     		return true;
     }
     
     
     
     // Lock position function
     
     private void setLockXY(int eventX, int eventY){
     	mLockX = eventX;
     	mLockY = eventY;
     	
     	
     }
     
     // ****************** Interface
     
     public interface OnCircularSelectorTriggerListener{
     	
     	static final int ICON_GRABBED_STATE_NONE = 0;
     	static final int ICON_GRABBED_STATE_GRABBED = 1;
     	
     	static final int LOCK_ICON_TRIGGERED = 10;
     	
     	public void OnCircularSelectorGrabbedStateChanged(View v, int GrabState);
     	
     	public void onCircularSelectorTrigger(View v, int Trigger);
 
     	
     }
     
     // *********************** Callbacks
     
     /**
      * Registers a callback to be invoked when the music controls
      * are "triggered" by sliding the view one way or the other
      * or pressing the music control buttons.
      *
      * @param l the OnMusicTriggerListener to attach to this view
      */
     public void setOnCircularSelectorTriggerListener(OnCircularSelectorTriggerListener l) {
     	 if (DBG) log("Setting the listners");
     	this.mCircularTriggerListener = l;
     }
     
     /**
      * Sets the current grabbed state, and dispatches a grabbed state change
      * event to our listener.
      */
     private void setGrabbedState(int newState) {
         if (newState != mGrabbedState) {
             mGrabbedState = newState;
             if (mCircularTriggerListener != null) {
                 mCircularTriggerListener.OnCircularSelectorGrabbedStateChanged(this, mGrabbedState);
             }
         }
     }
     
     
     /**
      * Dispatches a trigger event to our listener.
      */
     private void dispatchTriggerEvent(int whichTrigger) {
     	
     	 if (IDBG) log("Dispatching a trigered event");
         //vibrate(VIBRATE_LONG);
         if (mCircularTriggerListener != null) {
             
         		mCircularTriggerListener.onCircularSelectorTrigger(this, whichTrigger);
             
         }
     }
     
     
     //************************** Misc Function***********************
     private boolean isVertical() {
         return (mOrientation == VERTICAL);
     }
     
     
     private void log(String msg) {
 	    Log.d(TAG, msg);
 	}
 	    
     private Bitmap getBitmapFor(int resId) {
         return BitmapFactory.decodeResource(getContext().getResources(), resId);
     }
     private void reset(){
     	
     	setGrabbedState(OnCircularSelectorTriggerListener.ICON_GRABBED_STATE_NONE);
     	mIsTouchInCircle = false;
     	
     }
     
     
     
 }
