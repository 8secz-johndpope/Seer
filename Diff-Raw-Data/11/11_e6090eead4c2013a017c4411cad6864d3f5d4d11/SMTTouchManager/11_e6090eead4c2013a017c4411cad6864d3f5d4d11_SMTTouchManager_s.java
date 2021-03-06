 package vialab.SMT;
 
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import processing.core.PApplet;
 
 public class SMTTouchManager {
 	private PApplet applet;
 
 	private SMTTuioListener touchListener;
 
 	private SMTZonePicker picker;
 
 	/**
 	 * For each touch ID, gives the object that that touch belongs to. This
 	 * object may be null.
 	 */
 	public Map<Long, Zone> idToTouched = Collections.synchronizedMap(new HashMap<Long, Zone>());
 
 	/**
 	 * The number of touches for each object. The numbers in this map are at
 	 * least 1.
 	 */
 	private Map<Zone, Integer> touchesPerObject = Collections
 			.synchronizedMap(new HashMap<Zone, Integer>());
 
 	/**
 	 * The set of currently active objects.
 	 */
 	private Set<Zone> activeObjects = Collections.synchronizedSet(new HashSet<Zone>());
 
 	private Method touchDown, touchMoved, touchUp;
 
 	// private Method addTouch, removeTouch, updateTouch;
 
 	// private Method addObject, removeObject, updateObject;
 
 	// private Method refresh;
 	
 	static TouchState currentTouchState= new TouchState();
 
 	public SMTTouchManager(SMTTuioListener touchListener, SMTZonePicker picker) {
 		this.touchListener = touchListener;
 		this.picker = picker;
 		this.applet = TouchClient.parent;
 
 		retrieveMethods(TouchClient.parent);
 	}
 
 	/**
 	 * Determines to which objects touch events should be sent, and then sends
 	 * them.
 	 */
 	public void handleTouches() {
 		// while (touchListener.hasMoreTouchStates()) {
 		picker.renderPickBuffer();
 
 		// fetch the previous and current state of all touches
 		// (note that the order of fetching these matters!)
 		// TouchState previousTouchState = touchListener.getCurrentTouchState();
 		// TouchState currentTouchState = touchListener.dequeueTouchState();
 
 		currentTouchState.update(touchListener.getCurrentTuioState());
 
 		// forward events
 		handleTouchesDown(currentTouchState);
 		handleTouchesUp(currentTouchState);
 		handleTouchesMoved(currentTouchState);
 		// }
 	}
 
 	public void reset() {
 		this.activeObjects.clear();
 		this.idToTouched.clear();
 		this.touchesPerObject.clear();
 	}
 
 	private void activate(Zone zone, Touch touch) {
 		activeObjects.add(zone);
 		// zone.assign(touch);
 		// zone.setActive(true);
 	}
 
 	private void deactivate(Zone zone, Long id) {
 		zone.unassign(id);
 		if (!zone.isActive()) {
 			activeObjects.remove(zone);
 		}
 	}
 
 	private void deactivate(Zone zone) {
 		activeObjects.remove(zone);
 		zone.unassignAll();
 		// zone.setActive(false);
 	}
 
 	private void resetActive() {
 		// don't want ConcurrentModificationException to bite us
 		ArrayList<Zone> candidates = new ArrayList<Zone>();
 		for (Zone zone : activeObjects) {
 			if (!touchesPerObject.containsKey(zone)) {
 				candidates.add(zone);
 			}
 		}
 		for (Zone zone : candidates) {
 			deactivate(zone);
 		}
 	}
 
 	public int getTouchesPerObject(Zone zone) {
 		if (touchesPerObject.containsKey(zone)) {
 			return touchesPerObject.get(zone);
 		}
 		else {
 			return 0;
 		}
 	}
 
 	/**
 	 * Handles every touch in the current but not the previous state.
 	 */
 	protected void handleTouchesDown(TouchState currentTouchState) {
 		SMTUtilities.invoke(touchDown, applet);
 		for (Touch touchPoint : currentTouchState) {
 			// now either their is no zone it is mapped to or the zone mapped to
 			// doesn't have it active, so new touchDown to find where to assign
 			// it
 			if (idToTouched.get(touchPoint.getSessionID()) == null
 					|| !idToTouched.get(touchPoint.getSessionID()).activeTouches
 							.containsKey(touchPoint.getSessionID())) {
				touchPoint.isDown=true;
 				// it's a new touch that just went down,
 				// or an old touch that just crossed an object
 				// or an old touch that was unassigned from an object
 				Zone picked = picker.pick(touchPoint);
 
 				// dont fire touchDown when repicking to same zone (both null is
 				// also caught by this), to avoid the auto-unassignAll() which
 				// allows picking to different zones with one touch by default,
 				// from causing touchDown being called every frame when the
 				// touch is down
 				if (picked != idToTouched.get(touchPoint.getSessionID())) {
 					doTouchDown(picked, touchPoint);
 				}
 			}
 		}
 	}
 
 	/**
 	 * Handles every touch in the previous but not in the current state.
 	 */
 	protected void handleTouchesUp(TouchState currentTouchState) {
 		SMTUtilities.invoke(touchUp, applet);
 		ArrayList<Long> idsToRemove = new ArrayList<Long>();
 		for (Long id : idToTouched.keySet()) {
 			if (!currentTouchState.contains(id)) {
 				// the touch existed, but no longer exists, so it went up
 				Zone zone = idToTouched.get(id);
 				idsToRemove.add(id);
				zone.getTouchMap().get(id).isDown=false;
 				doTouchUp(zone, zone.getTouchMap().get(id));
 			}
 		}
 
 		for (Long id : idsToRemove) {
 			idToTouched.remove(id);
 		}
 	}
 
 	/**
 	 * Handles the movement of touches.
 	 */
 	protected void handleTouchesMoved(TouchState currentTouchState) {
 		SMTUtilities.invoke(touchMoved, applet);
 
 		// get the set of all objects currently touched
 		Set<Zone> touchables = new HashSet<Zone>(idToTouched.values());
 
 		// send each of these objects a touch moved event
 		for (Zone zone : touchables) {
 			if (zone != null) {
 				doTouchesMoved(zone, currentTouchState);
 			}
 		}
 	}
 
 	/**
 	 * Called when a touch went down, or when an orphaned touch moves around.
 	 * 
 	 * @param zone
 	 *            may be null
 	 */
 	private void doTouchDown(Zone zone, Touch touchPoint) {
 		idToTouched.put(touchPoint.getSessionID(), zone);
 
 		if (zone != null) {
 			assignTouch(zone, touchPoint);
 			zone.touchDownInvoker();
 		}
 
 		// upon a touch-down anywhere, we set all objects that don't currently
 		// have touches to inactive
 		resetActive();
 	}
 
 	public void assignTouch(Zone zone, Touch touch) {
 		idToTouched.put(touch.getSessionID(), zone);
 		if (!touchesPerObject.containsKey(zone)) {
 			touchesPerObject.put(zone, 0);
 		}
 		touchesPerObject.put(zone, touchesPerObject.get(zone) + 1);
 
 		activate(zone, touch);
 		zone.touchDown(touch);
 	}
 
 	/**
 	 * Called when a touch went up.
 	 * 
 	 * @param zone
 	 *            may be null
 	 */
 	private void doTouchUp(Zone zone, Touch touch) {
 		if (zone != null) {
 			zone.touchUp(touch);
 			deactivate(zone, touch.sessionID);
 
 			touchesPerObject.put(zone, touchesPerObject.get(zone) - 1);
 			if (touchesPerObject.get(zone) == 0) {
 				touchesPerObject.remove(zone);
 			}
 			
 			zone.touchUpInvoker();
 		}
 	}
 
 	private void doTouchesMoved(Zone zone, TouchState currentTouchState) {
 		List<Touch> currentLocalState = localTouchState(currentTouchState, zone);
 		zone.touchesMoved(currentLocalState);
 		zone.touchMovedInvoker();
 	}
 
 	/**
 	 * Returns a new {@link TouchState} that contains only the touches relevant
 	 * to the given object, with their IDs remapped to 0, 1, 2 etc.
 	 * 
 	 * @param zone
 	 *            must not be null
 	 */
 	private List<Touch> localTouchState(TouchState touchState, Zone zone) {
 		List<Touch> localState = new ArrayList<Touch>(touchState.size());
 		for (Touch touchPoint : touchState) {
 			if (idToTouched.get(touchPoint.getSessionID()) == zone) {
 				localState.add(touchPoint);
 			}
 		}
 		return localState;
 	}
 
 	private void retrieveMethods(PApplet parent) {
 		// addTouch = SMTUtilities.getPMethod(parent, "addTouch", new Class[] {
 		// Touch.class });
 		// removeTouch = SMTUtilities.getPMethod(parent, "removeTouch", new
 		// Class[] { Touch.class });
 		// updateTouch = SMTUtilities.getPMethod(parent, "updateTouch", new
 		// Class[] { Touch.class });
 
 		/*
 		 * addObject = SMTUtilities.getPMethod(parent, "addObject", new Class[]
 		 * { TuioObject.class }); removeObject = SMTUtilities.getPMethod(parent,
 		 * "removeObject", new Class[] { TuioObject.class }); updateObject =
 		 * SMTUtilities.getPMethod(parent, "updateObject", new Class[] {
 		 * TuioObject.class });
 		 * 
 		 * refresh = SMTUtilities.getPMethod(parent, "refresh", new Class[] {
 		 * TuioTime.class });
 		 */
 
 		touchDown = SMTUtilities.getPMethod(parent, "touchDown");
 		touchMoved = SMTUtilities.getPMethod(parent, "touchMoved");
 		touchUp = SMTUtilities.getPMethod(parent, "touchUp");
 	}
 }
