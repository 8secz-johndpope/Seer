 /**
  *  MicroEmulator
  *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
  *
  *  It is licensed under the following two licenses as alternatives:
  *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
  *    2. Apache License (the "AL") Version 2.0
  *
  *  You may not use this file except in compliance with at least one of
  *  the above two licenses.
  *
  *  You may obtain a copy of the LGPL at
  *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
  *
  *  You may obtain a copy of the AL at
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the LGPL or the AL for the specific language governing permissions and
  *  limitations.
  *
  *  @version $Id$
  */
 
 package org.microemu.android.device.ui;
 
 import javax.microedition.lcdui.Canvas;
 import javax.microedition.lcdui.Graphics;
 import javax.microedition.lcdui.game.GameCanvas;
 
 import org.microemu.MIDletAccess;
 import org.microemu.MIDletBridge;
 import org.microemu.android.MicroEmulatorActivity;
 import org.microemu.android.device.AndroidDeviceDisplay;
 import org.microemu.android.device.AndroidDisplayGraphics;
 import org.microemu.android.device.AndroidInputMethod;
 import org.microemu.app.ui.DisplayRepaintListener;
 import org.microemu.device.Device;
 import org.microemu.device.DeviceFactory;
 import org.microemu.device.ui.CanvasUI;
 
 import android.content.Context;
 import android.graphics.Bitmap;
 import android.graphics.Rect;
 import android.os.Handler;
 import android.view.MotionEvent;
 import android.view.SurfaceHolder;
 import android.view.SurfaceView;
 import android.view.View;
 import android.view.SurfaceHolder.Callback;
 
 public class AndroidCanvasUI extends AndroidDisplayableUI implements CanvasUI {
     
     public AndroidCanvasUI(final MicroEmulatorActivity activity, Canvas canvas) {
         super(activity, canvas, false);
         
         activity.post(new Runnable() {
             public void run() {
                 view = new CanvasView(activity);
             }
         });
     }
     
     public View getView() {
         return view;
     }
     
     @Override
     public void hideNotify()
     {
         ((AndroidDeviceDisplay) activity.getEmulatorContext().getDeviceDisplay()).removeDisplayRepaintListener((DisplayRepaintListener) view);
         
         super.hideNotify();
     }
 
     @Override
     public void showNotify()
     {
         super.showNotify();
         
         ((AndroidDeviceDisplay) activity.getEmulatorContext().getDeviceDisplay()).addDisplayRepaintListener((DisplayRepaintListener) view);
     }   
     
 	public Graphics getGraphics() {
 		return ((CanvasView) view).graphics;
 	}
     
     //
     // CanvasUI
     //
     
     public class CanvasView extends SurfaceView implements DisplayRepaintListener {
         
         private final static int FIRST_DRAG_SENSITIVITY_X = 5;
         
         private final static int FIRST_DRAG_SENSITIVITY_Y = 5;
         
         private Bitmap bitmap;
             
         private android.graphics.Canvas bitmapCanvas;
         
         private boolean surfaceCreated;
         
         private Callback callback;
         
         private int pressedX = -FIRST_DRAG_SENSITIVITY_X;
         
         private int pressedY = -FIRST_DRAG_SENSITIVITY_Y;
         
         private AndroidDisplayGraphics graphics = new AndroidDisplayGraphics();
         
         public CanvasView(Context context) {
             super(context);
             
             setFocusable(true);
             setFocusableInTouchMode(true);
             
             initGraphics(((Canvas) displayable).getWidth(), ((Canvas) displayable).getHeight());
             
             surfaceCreated = false;
             callback = new Callback() {
 
                 public void surfaceCreated(SurfaceHolder holder) {
                 	synchronized (CanvasView.this) {
                     	surfaceCreated = true;
                     	CanvasView.this.notify();
                 	}
                 }
 
                 public void surfaceDestroyed(SurfaceHolder holder) {
                 	surfaceCreated = false;
                 }
                 
                 public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                     initGraphics(width, height);
                     ((Canvas) displayable).repaint(0, 0, width, height);
                 }
 
             };
             getHolder().addCallback(callback);
         }
 
         public void initGraphics(int width, int height) {
             AndroidCanvasUI.this.view = view;
             bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
             bitmapCanvas = new android.graphics.Canvas(bitmap);
             graphics.reset(bitmapCanvas);
         }
         
         public AndroidDisplayGraphics getGraphics() {
             return graphics;
         }
         
         public void waitForSurfaceCreated() {
         	synchronized (CanvasView.this) {
         		if (!surfaceCreated) {
 					try {
 						CanvasView.this.wait();
 					} catch (InterruptedException e) {
 					}
         		}
         	}
         }
         
         //
         // View
         //
         
         @Override
         public void onDraw(android.graphics.Canvas androidCanvas) {
             MIDletAccess ma = MIDletBridge.getMIDletAccess();
             if (ma == null) {
                 return;
             }
             
             graphics.reset(androidCanvas);
             graphics.setClip(0, 0, view.getWidth(), view.getHeight());
             ma.getDisplayAccess().paint(graphics);
         }   
         
         @Override
         public boolean onTouchEvent(MotionEvent event) {
             Device device = DeviceFactory.getDevice();
             AndroidInputMethod inputMethod = (AndroidInputMethod) device.getInputMethod();
             int x = (int) event.getX();
             int y = (int) event.getY();
             switch (event.getAction()) {
             case MotionEvent.ACTION_DOWN :
                 inputMethod.pointerPressed(x, y);
                 pressedX = x;
                 pressedY = y;
                 break;
             case MotionEvent.ACTION_UP :
                 inputMethod.pointerReleased(x, y);
                 break;
             case MotionEvent.ACTION_MOVE :
                 if (x > (pressedX - FIRST_DRAG_SENSITIVITY_X) &&  x < (pressedX + FIRST_DRAG_SENSITIVITY_X)
                         && y > (pressedY - FIRST_DRAG_SENSITIVITY_Y) &&  y < (pressedY + FIRST_DRAG_SENSITIVITY_Y)) {
                 } else {
                     pressedX = -FIRST_DRAG_SENSITIVITY_X;
                     pressedY = -FIRST_DRAG_SENSITIVITY_Y;
                     inputMethod.pointerDragged(x, y);
                 }
                 break;
             default:
                 return false;
             }
             
             return true;
         }
 
         @Override
         public Handler getHandler() {
             return super.getHandler();
         }       
 
         //
         // DisplayRepaintListener
         //
 
         public void repaintInvoked(Object repaintObject)
         {
             if (!(((Canvas) displayable) instanceof GameCanvas)) {
                 onDraw(bitmapCanvas);
             }
             SurfaceHolder holder = getHolder();
             android.graphics.Canvas canvas = holder.lockCanvas((Rect) repaintObject);
            canvas.drawBitmap(bitmap, 0, 0, null);
            holder.unlockCanvasAndPost(canvas);
         }
         
     }
 
 }
