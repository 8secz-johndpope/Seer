 package com.tuohy.worldwindvr;
 
 import java.awt.Color;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
 import gov.nasa.worldwind.geom.Angle;
 import gov.nasa.worldwind.geom.LatLon;
 import gov.nasa.worldwind.geom.Position;
 import gov.nasa.worldwind.layers.RenderableLayer;
 import gov.nasa.worldwind.render.BasicShapeAttributes;
 import gov.nasa.worldwind.render.Material;
 import gov.nasa.worldwind.render.SurfacePolyline;
 import gov.nasa.worldwindx.examples.ApplicationTemplate;
 
 import com.tuohy.worldwindvr.input.SampleGeographicLocation;
 import com.tuohy.worldwindvr.input.WorldWindVRKeyboardListener;
 import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;
 
 public class PrecacheRobot {
 
 	private final boolean DEBUG_MODE_ON = true;
 	RenderableLayer debugLayer = new RenderableLayer();
 	SurfacePolyline debugLine = new SurfacePolyline();
 	List<LatLon> debugLocations = Collections.synchronizedList(new ArrayList<LatLon>());
 
 	//final double dtheta = 3.142/50; // pi/50 -> 100 updates per revolution
 	// rem: 6378.137 km/earth radian. We want ~1km per revolution, so 1/100th of a km per update
 	final double height = 1000; // meters above ground
 	final double radiansPer100m = 1.0 / 63781.37;
 	final double stepSize = 5; // meters
 	WorldWindVR vrFrame;
 	WorldWindVRKeyboardListener vrKey;
 	OculusStereoSceneController sceneController;
 	/**
 	 * @param args
 	 */
 	public PrecacheRobot(WorldWindVRKeyboardListener vrKey, WorldWindVR vrFrame) {
 		this.vrFrame = vrFrame;
 		this.vrKey = vrKey;
 
 		if(DEBUG_MODE_ON){
 			ApplicationTemplate.insertBeforePlacenames(vrFrame.getWwd(), debugLayer);
 			BasicShapeAttributes attr = new BasicShapeAttributes();
 			attr.setOutlineWidth(3);
 			attr.setOutlineMaterial(new Material(Color.CYAN));
 			attr.setDrawOutline(true);
 			debugLine.setAttributes(attr);
 			debugLine.setLocations(debugLocations);
 			debugLayer.addRenderable(debugLine);
 		}
 	}
 
 	class CameraLocation {
 		Angle latitude;
 		Angle longitude;
 		double elevation;
 
 		public Position toPosition() {
 			return new Position(this.latitude, this.longitude, this.elevation);
 		}
 
 		public LatLon toLatLon() {
 			return new LatLon(this.latitude, this.longitude);
 		}
 
 		public CameraLocation(LatLon latLon, double cameraHeightAtFocusElevation) {
 			this.latitude = latLon.getLatitude();
 			this.longitude = latLon.getLongitude();
 			this.elevation = vrFrame.wwd.getView().getGlobe().getElevation(this.latitude, this.longitude) + height;
 			vrFrame.view.setEyePosition(this.toPosition());
 		}
 
 		public void move(Angle bearing, Angle distance) {
 			LatLon newLatLon = LatLon.greatCircleEndPosition(this.toLatLon(),bearing,distance);
 			this.update(newLatLon);
 			vrFrame.view.setEyePosition(this.toPosition());
 		}
 
 		public void update(LatLon location) {
 			this.latitude = location.getLatitude();
 			this.longitude = location.getLongitude();
 			vrFrame.view.setEyePosition(this.toPosition());
 		}
 
 	}
 
 	class PrecacheTravelTask extends TimerTask {
 		LatLon focus;
 		CameraLocation cam;
		double curRadius; //meters
 		Angle theta;
 		Angle dtheta;
 		double cameraHeightAtFocusElevation;
 		// reminder: dtheta = step size / radius
 
 		PrecacheTravelTask(LatLon focus) {
 			this.focus = focus;
			curRadius = 1;
 			theta = Angle.ZERO;
			dtheta = Angle.fromRadians(stepSize / 100*curRadius);
 			cameraHeightAtFocusElevation = vrFrame.wwd.getView().getGlobe().getElevation(focus.getLatitude(), focus.getLongitude()) + height;
 			cam = new CameraLocation(LatLon.greatCircleEndPosition(focus, Angle.ZERO, Angle.fromRadians(radiansPer100m)),cameraHeightAtFocusElevation);			
 		}
 
 		public void run() {
 			theta = theta.add(dtheta);
 			if (theta.radians > (Math.PI * 2)) {
 				theta = Angle.ZERO;
				curRadius += 1;
				dtheta = Angle.fromRadians(stepSize / 100*curRadius);
 			}			
			LatLon latlon = LatLon.greatCircleEndPosition(focus,theta,Angle.fromRadians(curRadius*radiansPer100m));
 			cam = new CameraLocation(latlon,cameraHeightAtFocusElevation);
 
			//upates a surface line with the new position, this line will show the entire camera path
 			if(DEBUG_MODE_ON){
 				debugLocations.add(latlon);
 				
 				//only update the line every 20 iterations or so to save CPU
 				if(debugLocations.size()%10==0){
 					debugLine.setLocations(debugLocations);
 				}
 			}
 		}
 	}
 	public static void test() {
 		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});
 	}
 	public static void main(String[] args) {
 		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});
 
 
 	}
 	public void start() {
 		Timer pulse = new Timer();
 		//Grand Canyon
 //		pulse.schedule(new PrecacheTravelTask(LatLon.fromDegrees(36.19529915228048,-111.7481440380943)), 0, 50);
 
 		//Atlanta - easier for debugging
 		pulse.schedule(new PrecacheTravelTask(LatLon.fromDegrees(33.755,-84.39)), 0, 50);
 		// TODO Auto-generated method stub
 
 	}
 }
