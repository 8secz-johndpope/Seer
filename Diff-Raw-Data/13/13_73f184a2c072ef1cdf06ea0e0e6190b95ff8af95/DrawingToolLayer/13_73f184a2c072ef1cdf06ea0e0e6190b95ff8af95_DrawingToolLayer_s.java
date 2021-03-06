 // **********************************************************************
 // 
 // <copyright>
 // 
 //  BBN Technologies, a Verizon Company
 //  10 Moulton Street
 //  Cambridge, MA 02138
 //  (617) 873-8000
 // 
 //  Copyright (C) BBNT Solutions LLC. All rights reserved.
 // 
 // </copyright>
 // **********************************************************************
 // 
 // $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/DrawingToolLayer.java,v $
 // $RCSfile: DrawingToolLayer.java,v $
// $Revision: 1.11 $
// $Date: 2003/03/07 15:06:07 $
 // $Author: dietrick $
 // 
 // **********************************************************************
 
 
 package com.bbn.openmap.layer;
 
 import com.bbn.openmap.Layer;
 import com.bbn.openmap.event.*;
 import com.bbn.openmap.layer.util.LayerUtils;
 import com.bbn.openmap.omGraphics.*;
 import com.bbn.openmap.proj.*;
 import com.bbn.openmap.tools.drawing.DrawingTool;
 import com.bbn.openmap.tools.drawing.OMDrawingTool;
 import com.bbn.openmap.tools.drawing.DrawingToolRequestor;
 import com.bbn.openmap.util.*;
 
 import java.awt.*;
 import java.awt.event.*;
 import java.util.*;
 import java.util.Properties;
 import javax.swing.*;
 
 /**
  * This layer can receive graphics from the OMDrawingToolLauncher, and
  * also sent it's graphics to the OMDrawingTool for editing. <P>
  *
  * The projectionChanged() and paint() methods are taken care of in
  * the OMGraphicHandlerLayer superclass.
  */
 public class DrawingToolLayer extends OMGraphicHandlerLayer 
     implements MapMouseListener, DrawingToolRequestor {
 
     /** Get a handle on the DrawingTool. */
     protected OMDrawingTool drawingTool;
 
     /** For callbacks on editing... */
     protected final DrawingToolRequestor layer = this;
 
     /**
      * A flag to provide a tooltip over OMGraphics to click to edit.
      */
     protected boolean showHints = true;
 
     public final static String ShowHintsProperty = "showHints";
 
     protected boolean DTL_DEBUG = false;
 
     public DrawingToolLayer() {
 	setAddToBeanContext(true);
 	DTL_DEBUG = Debug.debugging("dtl");
     }
 
     public void setProperties(String prefix, Properties props) {
 	super.setProperties(prefix,props);
 
 	String realPrefix = PropUtils.getScopedPropertyPrefix(prefix);
 	showHints = LayerUtils.booleanFromProperties(props, realPrefix + ShowHintsProperty, showHints);
     }
     
     /**
      * Overriding the OMGraphicHandlerMethod, creating a list if it's null.
      */
     public OMGraphicList getList() {
 	OMGraphicList list = super.getList();
 	if (list == null) {
 	    list = new OMGraphicList();
 	    super.setList(list);
 	}
 	return list;
     }
 	
     public OMDrawingTool getDrawingTool() {
 	return drawingTool;
     }
 
     public void setDrawingTool(OMDrawingTool dt) {
 	drawingTool = dt;
     }
 
     /**
      * DrawingToolRequestor method.
      */
     public void drawingComplete(OMGraphic omg, OMAction action) {
 
 	if (DTL_DEBUG) {
 	    String cname = omg.getClass().getName();
	    int lastPeriod = cname.indexOf('.');
 	    if (lastPeriod != -1) {
		cname = cname.substring(lastPeriod);
 	    }
 	    Debug.output("DrawingToolLayer: DrawingTool complete for " +
			 cname + " " + action);
 	}
 	// First thing, release the proxy MapMouseMode, if there is one.
 	releaseProxyMouseMode();
 
 	getList(); // create a list if there isn't one.
 	doAction(omg, action);
 	repaint();
     }
 
     /**
      * If the DrawingToolLayer is using a hidden OMDrawingTool,
      * release the proxy lock on the active MapMouseMode.
      */
     public void releaseProxyMouseMode() {
 	MapMouseMode pmmm = getProxyMouseMode();
 	OMDrawingTool dt = getDrawingTool();
 	if (pmmm != null && dt != null) {
 	    if (pmmm.isProxyFor(dt.getMouseMode())) {
 		if (DTL_DEBUG) {
 		    Debug.output("DTL: releasing proxy on " + pmmm.getID());
 		}
 
 		pmmm.releaseProxy();
 		setProxyMouseMode(null);
 		fireRequestInfoLine(""); // hidden drawing tool put up coordinates, clean up.
 	    }
 
 	    if (dt.isActivated()) {
 		dt.deactivate();
 	    }
 	}
     }
 
     /**
      * Called by findAndInit(Iterator) so subclasses can find
      * objects, too.
      */
     public void findAndInit(Object someObj) {
 	if (someObj instanceof OMDrawingTool) {
 	    Debug.message("dtl", "DrawingToolLayer: found a drawing tool");
 	    setDrawingTool((OMDrawingTool)someObj);
 	}
     }
     
     /**
      * BeanContextMembershipListener method.  Called when a new object
      * is removed from the BeanContext of this object.
      */
     public void findAndUndo(Object someObj) {
 	if (someObj instanceof DrawingTool) {
 	    if (getDrawingTool() == (DrawingTool)someObj) {
 		setDrawingTool(null);
 	    }
 	}
     }
 
     /**
      * Note: A layer interested in receiving amouse events should
      * implement this function .  Otherwise, return the default, which
      * is null.
      */
     public synchronized MapMouseListener getMapMouseListener() {
 	return this;
     }
 
     /**
      * Return a list of the modes that are interesting to the
      * MapMouseListener.  You MUST override this with the modes you're
      * interested in.
      */
     public String[] getMouseModeServiceList() {
 	String[] services = {SelectMouseMode.modeID};
 	return services;
     }
     
     ////////////////////////
     // Mouse Listener events
     ////////////////////////
     
     /**
      * Invoked when a mouse button has been pressed on a component.
      * @param e MouseEvent
      * @return false
      */
     public boolean mousePressed(MouseEvent e) { 
 	boolean ret = false;
 	OMGraphic omgr = ((OMGraphicList)getList()).findClosest(e.getX(), e.getY(), 4);
 
 	if (omgr != null && shouldEdit(omgr)) {
 	    OMDrawingTool dt = getDrawingTool();
 	    if (dt != null) {
 		dt.setBehaviorMask(OMDrawingTool.QUICK_CHANGE_BEHAVIOR_MASK);
 
 		MapMouseMode omdtmm = dt.getMouseMode();
 		if (!omdtmm.isVisible()) {
 		    dt.setBehaviorMask(OMDrawingTool.PASSIVE_MOUSE_EVENT_BEHAVIOR_MASK |
 				       OMDrawingTool.QUICK_CHANGE_BEHAVIOR_MASK);
 		}
 
 		if (dt.edit(omgr, layer, e) != null) {
 		    // OK, means we're editing - let's lock up the MouseMode
 		    if (e instanceof MapMouseEvent) {
 
 			// Check to see if the DrawingToolMouseMode wants to 
 			// be invisible.  If it does, ask the current
 			// active MouseMode to be the proxy for it...
 			if (!omdtmm.isVisible()) {
 			    MapMouseMode mmm = ((MapMouseEvent)e).getMapMouseMode();
 			    if (mmm.actAsProxyFor(omdtmm)) {
 				if (DTL_DEBUG) {
 				    Debug.output("DTL: Setting " + mmm.getID() + " as proxy for drawing tool");
 				}
 				setProxyMouseMode(mmm);
 			    } else {
 				// WHOA, couldn't get proxy lock - bail
 				if (DTL_DEBUG) {
 				    Debug.output("DTL: couldn't get proxy lock on " + mmm.getID() + " deactivating internal drawing tool");
 				}
 				dt.deactivate();
 			    }
 			} else {
 			    if (DTL_DEBUG) {
 				Debug.output("DTL: OMDTMM wants to be visible");
 			    }
 			}
 		    } else {
 			if (DTL_DEBUG) {
 			    Debug.output("DTL: MouseEvent not a MapMouseEvent");
 			}
 		    }
 
 		    fireHideToolTip(e);
 		    ret = true;
 		}
 	    }
 	}
 	return ret;
     }
     
     protected MapMouseMode proxyMMM = null;
 
     /**
      * Set the ProxyMouseMode for the internal drawing tool, if there
      * is one.  Can be null.  Used to reset the mouse mode when
      * drawing's complete.
      */
     protected synchronized void setProxyMouseMode(MapMouseMode mmm) {
 	proxyMMM = mmm;
     }
 
     /**
      * Get the ProxyMouseMode for the internal drawing tool, if there
      * is one.  May be null.  Used to reset the mouse mode when
      * drawing's complete.
      */
     protected synchronized MapMouseMode getProxyMouseMode() {
 	return proxyMMM;
     }
 
     /**
      * Invoked when a mouse button has been released on a component.
      * @param e MouseEvent
      * @return false
      */
     public boolean mouseReleased(MouseEvent e) {      
 	return false;
     }
 
     /**
      * Get the behavior mask used for the drawing tool when an
      * OMGraphic is clicked on, and editing starts.  Controls how the
      * drawing tool will behave, with respect to its GUI, etc.
      */
     public int getDrawingToolEditBehaviorMask() {
 	return OMDrawingTool.DEFAULT_BEHAVIOR_MASK;
     }
     
     /**
      * Invoked when the mouse has been clicked on a component.
      * @param e MouseEvent
      * @return false
      */
     public boolean mouseClicked(MouseEvent e) { 
 	return false;
     }
     
     /**
      * Invoked when the mouse enters a component.
      * @param e MouseEvent
      */
     public void mouseEntered(MouseEvent e) {
 	return;
     }
     
     /**
      * Invoked when the mouse exits a component.
      * @param e MouseEvent
      */
     public void mouseExited(MouseEvent e) {
 	return;
     }
     
     ///////////////////////////////
     // Mouse Motion Listener events
     ///////////////////////////////
     
     /**
      * Invoked when a mouse button is pressed on a component and then 
      * dragged.  The listener will receive these events if it
      * @param e MouseEvent
      * @return false
      */
     public boolean mouseDragged(MouseEvent e) {      
 	return false;
     }
 
     protected OMGraphic lastSelected = null;
     protected String lastToolTip = null;
 
     /**
      * Invoked when the mouse button has been moved on a component
      * (with no buttons down).
      * @param e MouseEvent
      * @return false
      */
     public boolean mouseMoved(MouseEvent e) {  
 	OMGraphic omgr = ((OMGraphicList)getList()).findClosest(e.getX(),e.getY(),4.0f);
 	boolean ret = false;
 
 	if (omgr != null) {
 //  	    fireRequestInfoLine("Click to edit graphic");
 	    if (showHints) {
 		if (omgr != lastSelected) {
 		    lastToolTip = getToolTipForOMGraphic(omgr);
 		}
 
 		if (lastToolTip != null) {
 		    fireRequestToolTip(e, lastToolTip);
 		    ret = true;
 		} else {
 		    fireHideToolTip(e);
 		}
 	    }
 	} else {
 // 	    fireRequestInfoLine("");
 	    if (showHints) {
 		fireHideToolTip(e);
 	    }
 	    lastToolTip = null;
 	}
 
 	lastSelected = omgr;
 
 	return ret;
     }
     
     /**
      * A method called from within different MapMouseListener methods
      * to check whether an OMGraphic *should* be edited if the
      * OMDrawingTool is able to edit it.  Can be used by subclasses to
      * delineate between OMGraphics that are non-relocatable versus
      * those that can be moved.  This method should work together with
      * the getToolTipForOMGraphic() method so that OMGraphics that
      * shouldn't be edited don't provide tooltips that suggest that
      * they can be.
      *
      * By default, this method always
      * returns true because the DrawingToolLayer always thinks the
      * OMGraphic should be edited.
      */
     public boolean shouldEdit(OMGraphic omgr) {
 	return true;
     }
 
     /**
      * Called by default in the MouseMoved method, in order to fire a
      * ToolTip for a particular OMGraphic.  Return a String if you
      * want a ToolTip displayed, null if you don't.  By default,
      * returns 'Click to Edit' if the drawing tool can edit the
      * object.  You can override and change, and also return different
      * String for different OMGraphics.
      */
     protected String getToolTipForOMGraphic(OMGraphic omgr) {
 	OMDrawingTool dt = getDrawingTool();
 	if (shouldEdit(omgr) && dt.canEdit(omgr.getClass()) && !dt.isActivated()) {
 	    return "Click to Edit";
 	} else {
 	    return null;
 	}
     }
 
     /**
      * Handle a mouse cursor moving without the button being pressed.
      * Another layer has consumed the event.
      */
     public void mouseMoved() {}
 
     public Component getGUI() {
 
 	JPanel box = PaletteHelper.createVerticalPanel("Save Layer Graphics");
 	box.setLayout(new java.awt.GridLayout(0, 1));
 	JButton button = new JButton("Save As Shape File");
 	button.addActionListener(new ActionListener() {
 		public void actionPerformed(ActionEvent event) {
 		    com.bbn.openmap.dataAccess.shape.EsriShapeExport ese = 
 			new com.bbn.openmap.dataAccess.shape.EsriShapeExport(getList(), getProjection(), null);
 		    ese.export();
 		}
 	    });
 	box.add(button);
 
 	return box;
     }
 
     /**
      * A flag to provide a tooltip over OMGraphics to click to edit.
      */
     public void setShowHints(boolean show) {
 	showHints = show;
     }
 
     public boolean getShowHints() {
 	return showHints;
     }
 }
 
