 /*
  * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
  * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
  * See Copying
  */
 
 package de.ueller.midlet.gps;
 
 import javax.microedition.lcdui.Graphics;
 import javax.microedition.lcdui.Image;
 
 import de.ueller.gps.data.Legend;
 import de.ueller.gps.data.Configuration;
 import de.ueller.gps.tools.LayoutElement;
 import de.ueller.gpsMid.mapData.Tile;
 import de.ueller.midlet.gps.data.IntPoint;
 import de.ueller.midlet.gps.data.MoreMath;
 import de.ueller.midlet.gps.data.Node;
 import de.ueller.midlet.gps.data.ProjFactory;
 import de.ueller.midlet.gps.data.Projection;
 import de.ueller.midlet.gps.data.Way;
 import de.ueller.midlet.gps.tile.Images;
 import de.ueller.midlet.gps.tile.PaintContext;
 import de.ueller.midlet.gps.tile.WayDescription;
 
 /* This class collects all visible objects to an offline image for later painting.
  * It is run in a low priority to avoid interrupting the GUI.
  */
 public class ImageCollector implements Runnable {
 	private final static Logger logger = Logger.getInstance(ImageCollector.class, 
 			Logger.TRACE);
 
 	private volatile boolean shutdown = false;
 	private volatile boolean suspended = true;
 	private final Tile t[];
 	private Thread processorThread;
 	private ScreenContext nextSc = new ScreenContext();
 
 	private Image[] img = new Image[2];
 	private volatile PaintContext[] pc = new PaintContext[2];
	public static Node mapCenter = new Node();
 	public static volatile long icDuration = 0;
 	byte nextCreate = 1;
 	byte nextPaint = 0;
 
 	int xSize;
 	int ySize;
 	IntPoint newCenter = new IntPoint(0, 0);
 	IntPoint oldCenter = new IntPoint(0, 0);
 	float oldCourse;
 	private volatile boolean needRedraw = false;
 	public static volatile int createImageCount = 0;
 	private final Trace tr;
 	/** additional scale boost for Overview/Filter Map, bigger values load the tiles already when zoomed more out */
 	public static float overviewTileScaleBoost = 1.0f;
 
 	public ImageCollector(Tile[] t, int x, int y, Trace tr, Images i, Legend legend) {
 		super();
 		this.t = t;
 		this.tr = tr;
 		xSize = x;
 		ySize = y;
 		img[0] = Image.createImage(xSize, ySize);
 		img[1] = Image.createImage(xSize, ySize);
 		try {
 			Node n = new Node(2f, 0f);
 			pc[0] = new PaintContext(tr, i);
 			pc[0].legend = legend;
 			pc[0].setP(ProjFactory.getInstance(n, 0, 1500, xSize, ySize));
 			pc[1] = new PaintContext(tr, i);
 			pc[1].legend = legend;
 			pc[1].setP(ProjFactory.getInstance(n, 0, 1500, xSize, ySize));
 
 		} catch (Exception e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		processorThread = new Thread(this, "ImageCollector");
 		processorThread.setPriority(Thread.MIN_PRIORITY);
 		processorThread.start();
 	}
 
 	public void run() {
 		PaintContext createPC = null;
 		final byte MAXCRASHES = 5;
 		byte crash = 0;
 		do {
 		try {
 			while (!shutdown) {
 				if (!needRedraw || suspended) {
 					synchronized (this) {
 						try {
 							wait(30000);
 						} catch (InterruptedException e) {
 							continue; // Recheck condition of the loop
 						}
 					}
 				}
 				//#debug debug
 				logger.debug("Redrawing Map");
 				synchronized (this) {
 					while (pc[nextCreate].state != PaintContext.STATE_READY && !shutdown) {
 						try {
 							// System.out.println("img not ready");
 							wait(1000);
 						} catch (InterruptedException e) {
 						}
 					}
 					if (suspended || shutdown)
 						continue;
 					pc[nextCreate].state = PaintContext.STATE_IN_CREATE;
 				}
 				createPC = pc[nextCreate];				
 				
 				long startTime = System.currentTimeMillis();
 
 				// create PaintContext
 				createPC.xSize = xSize;
 				createPC.ySize = ySize;
 				createPC.center = nextSc.center.clone();
 				mapCenter = nextSc.center.clone();
 				createPC.scale = nextSc.scale;
 				Projection p = ProjFactory.getInstance(createPC.center, 
 						nextSc.course, nextSc.scale, xSize, ySize);
 				createPC.setP(p);
 //				p.inverse(xSize, 0, createPC.screenRU);
 //				p.inverse(0, ySize, createPC.screenLD);
 				// pcCollect.trace = nextSc.trace;
 				// pcCollect.dataReader = nextSc.dataReader;
 				// cleans the screen
 				createPC.g = img[nextCreate].getGraphics();
 				createPC.g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
 				createPC.g.fillRect(0, 0, xSize, ySize);
 //				createPC.g.setColor(0x00FF0000);
 //				createPC.g.drawRect(0, 0, xSize - 1, ySize - 1);
 //				createPC.g.drawRect(20, 20, xSize - 41, ySize - 41);
 				createPC.squareDstToWay = Float.MAX_VALUE;
 				createPC.squareDstToActualRoutableWay = Float.MAX_VALUE;
 				createPC.squareDstWithPenToRoutePath = Float.MAX_VALUE;
 				createPC.squareDstToRoutePath = Float.MAX_VALUE;
 				createPC.dest = nextSc.dest;
 				createPC.course = nextSc.course;
 				// System.out.println("create " + pcCollect);
 				
 				Way.setupDirectionalPenalty(createPC, tr.speed, tr.gpsRecenter);
 
 
 				float boost = Configuration.getMaxDetailBoostMultiplier();
 				
 				/*
 				 * layers containing highlighted path segments
 				 */
 				createPC.hlLayers = 0;
 				
 				// highlighted path only on top if gpsCentered
 				createPC.highlightedPathOnTop = tr.gpsRecenter;
 				
 				/**
 				 * At the moment we don't really have proper layer support
 				 * in the data yet, so only split it into Area, Way and Node
 				 * layers
 				 */
 				byte layersToRender[] = { Tile.LAYER_AREA, 1 | Tile.LAYER_AREA , 2 | Tile.LAYER_AREA,
 						3 | Tile.LAYER_AREA, 4 | Tile.LAYER_AREA,  0, 1, 2, 3, 4,
 						0 | Tile.LAYER_HIGHLIGHT, 1 | Tile.LAYER_HIGHLIGHT,
 						2 | Tile.LAYER_HIGHLIGHT, 3 | Tile.LAYER_HIGHLIGHT,
 						Tile.LAYER_NODE };
 				
 				/**
 				 * Draw each layer separately to enforce paint ordering:
 				 *
 				 * Go through the entire tile tree multiple times
 				 * to get the drawing order correct.
 				 * 
 				 * The first 5 layers correspond to drawing areas with the osm
 				 * layer tag of  (< -1, -1, 0, 1, >1),
 				 * then next 5 layers are drawing streets with
 				 * osm layer tag (< -1, -1, 0, 1, >1).
 				 * 
 				 * Then we draw the highlighted streets
 				 * and finally we draw the POI layer.
 				 * 
 				 * So e. g. layer 7 corresponds to all streets that
 				 * have no osm layer tag or layer = 0.
 				 */
 				for (byte layer = 0; layer < layersToRender.length; layer++) {
 					// render only highlight layers which actually have highlighted path segments
 					if (
 						(layersToRender[layer] & Tile.LAYER_HIGHLIGHT) > 0
 						&& layersToRender[layer] != Tile.LAYER_NODE
 					) {
 						/**
 						 * as we do two passes for each way layer when gps recentered - one for the ways and one for the route line on top,
 						 * we can use in the second pass the determined route path connection / idx
 						 * to highlight the route line in the correct / prior route line color.
 						 * when not gps recentered, this info will be by one image obsolete however
 						 */ 
 						if (layersToRender[layer] == (0 | Tile.LAYER_HIGHLIGHT)) {
 							RouteInstructions.dstToRoutePath = createPC.getDstFromSquareDst(createPC.squareDstToRoutePath);
 							if (RouteInstructions.dstToRoutePath != Integer.MAX_VALUE) {
 								RouteInstructions.routePathConnection = createPC.routePathConnection;
 								RouteInstructions.pathIdxInRoutePathConnection = createPC.pathIdxInRoutePathConnection;
 							}
 						}
 						byte relLayer = (byte)(((int)layersToRender[layer]) & 0x0000000F);
 						if ( (createPC.hlLayers & (1 << relLayer)) == 0) {
 							continue;
 						}
 					}
 					byte minTile = Legend.scaleToTile((int)(createPC.scale / (boost * overviewTileScaleBoost) ));
 					if ((minTile >= 3) && (t[3] != null)) {
 						t[3].paint(createPC, layersToRender[layer]);
 						Thread.yield();
 					}
 					if ((minTile >= 2) && (t[2] != null)) {
 						t[2].paint(createPC, layersToRender[layer]);
 						Thread.yield();
 					}
 					if ((minTile >= 1) && (t[1] != null)) {
 						t[1].paint(createPC, layersToRender[layer]);
 						Thread.yield();
 					}
 					if (t[0] != null) {
 						t[0].paint(createPC, layersToRender[layer]);
 					}
 					/**
 					 * Drawing waypoints
 					 */
 					if (t[5] != null) {
 						t[5].paint(createPC, layersToRender[layer]);
 					}
 					if (suspended) {
 						// Don't continue rendering if suspended
 						createPC.state = PaintContext.STATE_READY;
 						break;
 					}
 				}
 				/**
 				 * Drawing debuginfo for routing
 				 */
 				if (!suspended && t[4] != null 
 						&& (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS) 
 								|| Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS))) {
 					t[4].paint(createPC, (byte) 0);
 				}
 
 				icDuration = System.currentTimeMillis() - startTime;
 				//#mdebug
 				logger.info("Painting map took " + icDuration + " ms");
 				//#enddebug
 				createPC.state = PaintContext.STATE_READY;
 				if (!shutdown) {
 					newCollected();
 				}
 				createImageCount++;				
 				needRedraw = false;
 				tr.cleanup();
 				// System.out.println("create ready");
 				//System.gc();
 			}
 		} catch (OutOfMemoryError oome) {
 			if (createPC != null) {
 				createPC.state = PaintContext.STATE_READY;
 			}
 		   String recoverZoomedIn = "";
 		   crash++;
 		   if(tr.scale > 10000 && crash < MAXCRASHES) {
 		    tr.scale /= 1.5f;
 		    recoverZoomedIn = " Zooming in to recover.";
 		   }   
 		   logger.fatal("ImageCollector ran out of memory: " + oome.getMessage() + recoverZoomedIn);
 		} catch (Exception e) {
 			crash++;
 			logger.exception("ImageCollector thread crashed unexpectedly with error ", e);
 		}
 		if(crash >= MAXCRASHES) {
 		   logger.fatal("ImageCollector crashed too often. Aborting.");
 		}
 		} while (!shutdown && crash <MAXCRASHES);
 		processorThread = null;
 		synchronized (this) {
 			notifyAll();
 		}
 	}
 	
 	public void suspend() {
 		suspended = true;
 	}
 
 	public void resume() {
 		suspended = false;
 	}
 	
 	public synchronized void stop() {
 		shutdown = true;
 		notifyAll();
 		try {
 			while ((processorThread != null) && (processorThread.isAlive())) {
 				wait(1000);
 			}
 		} catch (InterruptedException e) {
 			//Nothing to do
 		}
 	}
 	
 	public void restart() {
 		processorThread = new Thread(this, "ImageCollector");
 		processorThread.setPriority(Thread.MIN_PRIORITY);
 		processorThread.start();
 	}
 
 	/** copy the last created image to the real screen
 	 *  but with the last collected position and direction in the center
 	 */
 	public Node paint(PaintContext screenPc) {
 		PaintContext paintPC;
 //		System.out.println("paint this: " + screenPc);
 //		System.out.println("paint image: " + pc[nextPaint]);
 		if (suspended) {
 			return new Node(0, 0);
 		}
 		
 //		nextSc = screenPc.cloneToScreenContext();
 		nextSc.center = screenPc.center.clone();
 		nextSc.course = screenPc.course;
 		nextSc.scale = screenPc.scale;
 		nextSc.dest = screenPc.dest;
 		nextSc.xSize = screenPc.xSize;
 		nextSc.ySize = screenPc.ySize;
 		Projection p = ProjFactory.getInstance(nextSc.center, 
 				nextSc.course, nextSc.scale, nextSc.xSize, nextSc.ySize);
 		nextSc.setP(p);
 		screenPc.setP(p);
 		
 		synchronized (this) {
 			if (pc[nextPaint].state != PaintContext.STATE_READY) {
 				logger.error("ImageCollector was trying to draw a non ready PaintContext " + pc[nextPaint].state);
 				return new Node(0, 0);
 			}
 			paintPC = pc[nextPaint];
 			paintPC.state = PaintContext.STATE_IN_PAINT;
 		}
 
 		// return center of the map image drawn to the caller
 		Node getDrawnCenter = paintPC.center.clone();
 
 		p.forward(paintPC.center, oldCenter);
 		screenPc.g.drawImage(img[nextPaint], 
 				oldCenter.x, oldCenter.y,
 				Graphics.VCENTER | Graphics.HCENTER); 
 		//Test if the new center is in the middle of the screen, in which 
 		//case we don't need to redraw, as nothing has changed. 
 		if (oldCenter.x != nextSc.xSize / 2 || oldCenter.y != nextSc.ySize / 2 || paintPC.course != nextSc.course ) { 
 			//The center of the screen has moved, so need 
 			//to redraw the map image  
 			needRedraw = true; 
 		} 
 
 		String name = null;
 		Way wayForName = null;
 		/** used to check for pixel distances because checking for meters from converted pixels
 		 *  requires to be exactly on the pixel when zoomed out far
 		 */
 		final int SQUARE_MAXPIXELS = 5 * 5;
 		// Tolerance of 15 pixels converted to meters
 		float pixDest = 15 / paintPC.ppm;
 		if (pixDest < 15) {
 			pixDest = 15;
 		}
 		if (paintPC.trace.gpsRecenter) {
 			// Show closest routable way name if map is gpscentered and we are closer 
 			// than SQUARE_MAXPIXELS or 30 m (including penalty) to it.
 			// If the routable way is too far away, we try the closest way.
 			if (paintPC.squareDstToActualRoutableWay < SQUARE_MAXPIXELS 
 				|| paintPC.getDstFromSquareDst(paintPC.squareDstToActualRoutableWay) < 30
 			) {
 				wayForName = paintPC.actualRoutableWay;
 			} else if (paintPC.squareDstToWay < SQUARE_MAXPIXELS
 					|| paintPC.getDstFromSquareDst(paintPC.squareDstToWay) < 30
 			) {
 				wayForName = paintPC.actualWay;
 			}
 		} else if (paintPC.getDstFromSquareDst(paintPC.squareDstToWay) <= pixDest) {
 			// If not gpscentered show closest way name if it's no more than 15 pixels away.
 			wayForName = paintPC.actualWay;
 		}
 		/*
 		 * As we are double buffering pc, nothing should be writing to paintPC
 		 * therefore it should be safe to access the volatile variable actualWay 
 		 */
 		if (paintPC.actualWay != null) {
 			screenPc.actualWay = paintPC.actualWay;
 			screenPc.actualSingleTile = paintPC.actualSingleTile;
 		}
 		if (wayForName != null) {		
 			int nummaxspeed;
 			String maxspeed = "";
 			String winter = "";
 			if (wayForName.getMaxSpeed() != 0) {
 				nummaxspeed = wayForName.getMaxSpeed();
 				if (Configuration.getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER) && (wayForName.getMaxSpeedWinter() > 0)) {
 					nummaxspeed = wayForName.getMaxSpeedWinter();
 					winter = " W";
 				}
 				if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
 					maxspeed=" SL:" + nummaxspeed + winter;
 				} else {
 					//Round up at this point, as the the previouse two conversions
 					//were rounded down already. (Seems to work better for speed limits of
 					//20mph and 30mph)
 					maxspeed=" SL:" + ((int)(nummaxspeed / 1.609344f) + 1) + winter;
 				}
 			}
 
 			if (wayForName.nameIdx != -1) {
 				name = screenPc.trace.getName(wayForName.nameIdx);
 			} else {
 				WayDescription wayDesc = Legend.getWayDescription(wayForName.type);
 				name = "(unnamed " + wayDesc.description + ")";
 			}
 			if (name == null) {
 				name = maxspeed;
 			} else {
 				name = name + maxspeed;
 			}
 		}
 		// use the nearest routable way for the the speed limit detection if it's 
 		// closer than 30 m or SQUARE_MAXPIXELS including penalty
 		if (paintPC.squareDstToActualRoutableWay < SQUARE_MAXPIXELS 
 				|| paintPC.getDstFromSquareDst(paintPC.squareDstToActualRoutableWay) < 30) {
 			tr.actualSpeedLimitWay = paintPC.actualRoutableWay;
 		} else {
 			tr.actualSpeedLimitWay = null;			
 		}
 
 		boolean showLatLon = Configuration.getCfgBitState(Configuration.CFGBIT_SHOWLATLON);
 		
 		LayoutElement e = Trace.tl.ele[TraceLayout.WAYNAME];
 		if (showLatLon) {
 			e.setText("lat: " + Float.toString(paintPC.center.radlat * MoreMath.FAC_RADTODEC) +
 					  " lon: " + Float.toString(paintPC.center.radlon * MoreMath.FAC_RADTODEC)
 			);
 		} else {
 			if (name != null && name.length() > 0) {
 				e.setText(name);
 			} else {
 				e.setText(" "); 
 			}
 		}
 
 		if (paintPC.scale != screenPc.scale) {
 			needRedraw = true;
 		}
 		synchronized (this) {
 			paintPC.state = PaintContext.STATE_READY;
 			if (needRedraw) {
 				notifyAll();
 			} else {
 				//System.out.println("No need to redraw after painting");
 			}
 		}
 		return getDrawnCenter;
 	}
 
 	private synchronized void newCollected() {
 		while ((pc[nextPaint].state != PaintContext.STATE_READY) || (pc[nextCreate].state != PaintContext.STATE_READY)) {
 			try {
 				wait(1000);
 			} catch (InterruptedException e) {
 			}
 		}
 		nextPaint = nextCreate;
 		nextCreate = (byte) ((nextCreate + 1) % 2);
 		tr.requestRedraw();
 	}
 
 	/**
 	 * Inform the ImageCollector that new vector data is available
 	 * and it's time to create a new image.
 	 */
 	public synchronized void newDataReady() {
 		needRedraw = true;
 		notify();
 	}
 	
 	public Projection getCurrentProjection(){
 		return pc[nextPaint].getP();
 	}
 	
 }
