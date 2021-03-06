 /**
 Khalid
 */
 package org.sikuli.slides.sikuli;
 import org.jnativehook.GlobalScreen;
 import org.jnativehook.NativeHookException;
 import org.sikuli.api.DesktopScreenRegion;
 import org.sikuli.api.ScreenRegion;
 import org.sikuli.api.visual.Canvas;
 import org.sikuli.api.visual.DesktopCanvas;
 import org.sikuli.slides.listeners.GlobalKeyboardListeners;
 import org.sikuli.slides.listeners.GlobalMouseListeners;
 import org.sikuli.slides.shapes.SlideShape;
 import org.sikuli.slides.utils.Constants;
 import org.sikuli.slides.utils.Constants.DesktopEvent;
 
 public class SlideTutorial {
 	private ScreenRegion targetRegion;
 	private Constants.DesktopEvent desktopEvent;
 	private SlideShape slideShape;
 	
 	public SlideTutorial(ScreenRegion targetRegion, SlideShape slideShape, Constants.DesktopEvent desktopEvent){
 		this.targetRegion=targetRegion;
 		this.slideShape=slideShape;
 		this.desktopEvent=desktopEvent;
 	}
 	
 	private String getActionDisplayName(){
 		if(desktopEvent==DesktopEvent.LEFT_CLICK){
 			return "Click here";
 		}
 		else if(desktopEvent==DesktopEvent.DOUBLE_CLICK){
 			return "Double click here";
 		}
 		else if(desktopEvent==DesktopEvent.RIGHT_CLICK){
 			return "Right click here";
 		}
 		else if(desktopEvent==DesktopEvent.DRAG_N_DROP){
 			if(slideShape.getOrder()==0)
 				return "Drag this";
 			else if(slideShape.getOrder()==1)
 				return "and drop it here";
 		}
 		else if(desktopEvent==DesktopEvent.KEYBOARD_TYPING){
 			return "Type: "+ slideShape.getText()+" here";
 		}
 		return "";
 	}
 	
 	public void performTutorialSlideAction() {
 		Canvas canvas=new DesktopCanvas();
 		canvas.addBox(targetRegion);
 		int x=targetRegion.getBounds().x;
 		int y=targetRegion.getBounds().y;
 		int w=targetRegion.getBounds().width;
 		int h=targetRegion.getBounds().height;
 		ScreenRegion labelRegion=new DesktopScreenRegion(x,y-h,w,h);
 		canvas.addLabel(labelRegion, getActionDisplayName());
         System.out.println("Waiting for the user to pefrom "+this.desktopEvent.toString()+" on the highlighted target.");
 		try {
         	if(!GlobalScreen.isNativeHookRegistered()){
         		GlobalScreen.registerNativeHook();
         	}
             if(this.desktopEvent==DesktopEvent.KEYBOARD_TYPING){
            	// add native keyboard listener
             	GlobalKeyboardListeners globalKeyboardListener=new GlobalKeyboardListeners(targetRegion,slideShape.getText(),desktopEvent);
             	GlobalScreen.getInstance().addNativeKeyListener(globalKeyboardListener);
            	// display canvas around the target until the user performs the appropriate action
             	canvas.displayWhile(globalKeyboardListener);
            	// Remove native keyboard listener
            	GlobalScreen.getInstance().removeNativeKeyListener(globalKeyboardListener);
            	
             }
            else if(this.desktopEvent==DesktopEvent.LEFT_CLICK ||
            		this.desktopEvent==DesktopEvent.RIGHT_CLICK ||
            		this.desktopEvent==DesktopEvent.DOUBLE_CLICK ||
            		this.desktopEvent==DesktopEvent.DRAG_N_DROP){
            	// add native keyboard listener
             	GlobalMouseListeners globalMouseListener=new GlobalMouseListeners(targetRegion,desktopEvent);
             	GlobalScreen.getInstance().addNativeMouseListener(globalMouseListener);
            	// display canvas around the target until the user performs the appropriate action
             	canvas.displayWhile(globalMouseListener);
            	// Remove native mouse listener
            	GlobalScreen.getInstance().removeNativeMouseListener(globalMouseListener);
            }
            else if(this.desktopEvent==DesktopEvent.LAUNCH_BROWSER){
            	//TODO:
            }
            else if(this.desktopEvent==DesktopEvent.EXIST||this.desktopEvent==DesktopEvent.NOT_EXIST){
            	//TODO:
             }
         }
         catch (NativeHookException ex) {
             System.err.println("There was a problem running the tutorial mode.");
             System.err.println(ex.getMessage());
             System.exit(1);
         }
 	}
 }
