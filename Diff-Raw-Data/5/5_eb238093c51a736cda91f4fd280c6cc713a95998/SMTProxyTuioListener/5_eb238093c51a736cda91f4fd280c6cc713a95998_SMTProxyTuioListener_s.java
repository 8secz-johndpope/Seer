 package vialab.SMT;
 
 import TUIO.TuioClient;
 import TUIO.TuioCursor;
 import TUIO.TuioListener;
 import TUIO.TuioObject;
 import TUIO.TuioTime;
 
 /**
  * This class exists to all multiple tuio streams to be merged into one, by
  * being a proxy listener, and changing sessionId/cursorId/symbolId to
  * (port*10000+prevId) to give each of them their own space for 10000 ids
  */
 class SMTProxyTuioListener implements TuioListener {
 	int port;
 
 	TuioListener realListener;
 	
 	TuioClient client;
 
 	public SMTProxyTuioListener(int port, TuioListener realListener) {
 		this.port = port;
 		this.realListener = realListener;
 		this.client = new TuioClient(port);
 		this.client.addTuioListener(this);
 	}
 
 	@Override
 	public synchronized void addTuioCursor(TuioCursor tcur) {
 		TuioCursor t = changedSessionIdCursor(tcur, port);
 		realListener.addTuioCursor(t);
 	}
 
 	@Override
 	public synchronized void updateTuioCursor(TuioCursor tcur) {
 		TuioCursor t = changedSessionIdCursor(tcur, port);
		realListener.addTuioCursor(t);
 	}
 
 	@Override
 	public synchronized void removeTuioCursor(TuioCursor tcur) {
 		TuioCursor t = changedSessionIdCursor(tcur, port);
		realListener.addTuioCursor(t);
 	}
 
 	@Override
 	public synchronized void addTuioObject(TuioObject tobj) {
 		TuioObject t = changedSessionIdObject(tobj, port);
 		realListener.addTuioObject(t);
 	}
 
 	@Override
 	public synchronized void updateTuioObject(TuioObject tobj) {
 		TuioObject t = changedSessionIdObject(tobj, port);
 		realListener.updateTuioObject(t);
 	}
 
 	@Override
 	public synchronized void removeTuioObject(TuioObject tobj) {
 		TuioObject t = changedSessionIdObject(tobj, port);
 		realListener.removeTuioObject(t);
 	}
 
 	@Override
 	public void refresh(TuioTime bundleTime) {
 		realListener.refresh(bundleTime);
 	}
 
 	private TuioCursor changedSessionIdCursor(TuioCursor tcur, int port) {
 		return new TouchCursor(port * 10000 + tcur.getSessionID(), port * 10000
 				+ tcur.getCursorID(), tcur);
 	}
 
 	private TuioObject changedSessionIdObject(TuioObject tobj, int port) {
 		return new TouchObject(port * 10000 + tobj.getSessionID(), port * 10000
 				+ tobj.getSymbolID(), tobj);
 	}
 
 	private class TouchObject extends TuioObject {
 		public TouchObject(long si, int sym, TuioObject tobj) {
 			super(tobj.getTuioTime(), si, sym, tobj.getX(), tobj.getY(), tobj.getAngle());
 			startTime = tobj.getStartTime();
 			x_speed = tobj.getXSpeed();
 			y_speed = tobj.getYSpeed();
 			motion_speed = tobj.getMotionSpeed();
 			motion_accel = tobj.getMotionAccel();
 			path = tobj.getPath();
 			state = tobj.getTuioState();
 			rotation_accel = tobj.getRotationAccel();
 			rotation_speed = tobj.getRotationSpeed();
 		}
 	}
 
 	private class TouchCursor extends TuioCursor {
 		public TouchCursor(long si, int ci, TuioCursor tcur) {
 			super(tcur.getTuioTime(), si, ci, tcur.getX(), tcur.getY());
 			startTime = tcur.getStartTime();
 			x_speed = tcur.getXSpeed();
 			y_speed = tcur.getYSpeed();
 			motion_speed = tcur.getMotionSpeed();
 			motion_accel = tcur.getMotionAccel();
 			path = tcur.getPath();
 			state = tcur.getTuioState();
 		}
 	}
 }
