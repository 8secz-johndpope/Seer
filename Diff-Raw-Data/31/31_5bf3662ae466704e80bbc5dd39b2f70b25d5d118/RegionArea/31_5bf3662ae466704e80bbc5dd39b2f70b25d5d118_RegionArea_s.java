 package org.dawb.workbench.plotting.system.swtxy;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.csstudio.swt.xygraph.figures.Axis;
 import org.csstudio.swt.xygraph.figures.PlotArea;
 import org.csstudio.swt.xygraph.figures.Trace;
 import org.csstudio.swt.xygraph.undo.ZoomType;
 import org.dawb.common.ui.plot.region.IRegion.RegionType;
 import org.dawb.common.ui.plot.region.IRegionListener;
 import org.dawb.common.ui.plot.region.RegionEvent;
 import org.dawb.common.ui.plot.trace.IImageTrace.ImageOrigin;
 import org.dawb.common.ui.plot.trace.ITraceListener;
 import org.dawb.common.ui.plot.trace.TraceEvent;
 import org.dawb.workbench.plotting.system.dialog.AddRegionCommand;
 import org.dawb.workbench.plotting.system.swtxy.selection.AbstractSelectionRegion;
 import org.dawb.workbench.plotting.system.swtxy.selection.SelectionRegionFactory;
 import org.eclipse.draw2d.Figure;
 import org.eclipse.draw2d.Graphics;
 import org.eclipse.draw2d.MouseEvent;
 import org.eclipse.draw2d.MouseListener;
 import org.eclipse.draw2d.MouseMotionListener;
 import org.eclipse.draw2d.geometry.PointList;
 import org.eclipse.draw2d.geometry.Rectangle;
 import org.eclipse.jface.viewers.ISelectionProvider;
 import org.eclipse.swt.graphics.PaletteData;
 
import persistence.antlr.debug.TraceListener;

 public class RegionArea extends PlotArea {
 
 	protected ISelectionProvider selectionProvider;
 	private final Map<String,AbstractSelectionRegion>     regions;
 	private final Map<String,ImageTrace> imageTraces;
 	
 	private Collection<IRegionListener>     regionListeners;
 	private Collection<ITraceListener>      imageTraceListeners;
 
 	private AbstractSelectionRegion regionBeingAdded;
 	private PointList regionPoints;
 	
 	public RegionArea(XYRegionGraph xyGraph) {
 		super(xyGraph);
 		this.regions     = new LinkedHashMap<String,AbstractSelectionRegion>();
 		this.imageTraces = new LinkedHashMap<String,ImageTrace>();
 	}
 		
 
 	public void addRegion(final AbstractSelectionRegion region){
 		addRegion(region, true);
 	}
 
 	private void addRegion(final AbstractSelectionRegion region, boolean fireListeners){
 		regions.put(region.getName(), region);
 		region.setXyGraph(xyGraph);
 		region.createContents(this);
 		region.setSelectionProvider(selectionProvider);
 		if (fireListeners) fireRegionAdded(new RegionEvent(region));
 		clearRegionTool();
 		revalidate();
 	}
 
 	public boolean removeRegion(final AbstractSelectionRegion region){
 	    final AbstractSelectionRegion gone = regions.remove(region.getName());
 		if (gone!=null){
 			region.remove(); // Clears up children (you can live without this
 			fireRegionRemoved(new RegionEvent(region));
 			revalidate();
 		}
 		return gone!=null;
 	}
 	
 	public void clearRegions() {
 		clearRegionsInternal();
 		revalidate();
 	}
 	
     protected void clearRegionsInternal() {
 		clearRegionTool();
 		if (regions==null) return;
 		for (AbstractSelectionRegion region : regions.values()) {
 			if (!region.isUserRegion()) continue;
 			region.remove();
 			fireRegionRemoved(new RegionEvent(region));
 		}
 		regions.clear();		
 	}
 	
 
 	public ImageTrace createImageTrace(String name, Axis xAxis, Axis yAxis) {
 
         if (imageTraces.containsKey(name)) throw new RuntimeException("There is an image called '"+name+"' already plotted!");
         
 		final ImageTrace trace = new ImageTrace(name, xAxis, yAxis);
 		
 		fireImageTraceCreated(new TraceEvent(trace));
 		
 		return trace;
 	}
 
 	/**Add a trace to the plot area.
 	 * @param trace the trace to be added.
 	 */
 	public void addImageTrace(final ImageTrace trace){
 		imageTraces.put(trace.getName(), trace);
 		add(trace);
 		revalidate();
 		
 		fireImageTraceAdded(new TraceEvent(trace));
 	}
 	
 	public boolean removeImageTrace(final ImageTrace trace){
 	    final ImageTrace gone = imageTraces.remove(trace.getName());
 		if (gone!=null){
 			trace.remove();
 			fireImageTraceRemoved(new TraceEvent(trace));
 			revalidate();
 		}
 		return gone!=null;
 	}
 
 	public void clearImageTraces() {
 		if (imageTraces==null) return;
 		for (ImageTrace trace : imageTraces.values()) {
 			trace.remove();
 			fireImageTraceRemoved(new TraceEvent(trace));
 		}
 		imageTraces.clear();
 		revalidate();
 	}
 
 	
 	@Override
 	protected void layout() {
 	    final Rectangle clientArea = getClientArea();
 		for(ImageTrace trace : imageTraces.values()){
 			if(trace != null && trace.isVisible())
 				//Shrink will make the trace has no intersection with axes,
 				//which will make it only repaints the trace area.
 				trace.setBounds(clientArea);//.getCopy().shrink(1, 1));				
 		}		
         super.layout();
 	}
 		
 	@Override
 	protected void paintClientArea(final Graphics graphics) {
 		super.paintClientArea(graphics);
 
		if (regionBeingAdded!=null && regionPoints!=null) {
 			regionBeingAdded.paintBeforeAdded(graphics, regionPoints, getBounds());
 		}
 	}
 
     private RegionMouseListener regionListener;
 	
 	public AbstractSelectionRegion createRegion(String name, Axis x, Axis y, RegionType regionType, boolean startingWithMouseEvent) throws Exception {
 
 		if (getRegionMap()!=null) {
 			if (getRegionMap().containsKey(name)) throw new Exception("The region '"+name+"' already exists.");
 		}
 		
 		AbstractSelectionRegion region = SelectionRegionFactory.createSelectionRegion(name, x, y, regionType);
 		if (startingWithMouseEvent) {
 			xyGraph.setZoomType(ZoomType.NONE);
 		    setCursor(region.getRegionCursor());
 		    regionBeingAdded = region;
 		    
 		    // Mouse listener for region bounds
 		    regionListener = new RegionMouseListener();
 		    addMouseListener(regionListener);
 		    addMouseMotionListener(regionListener);
 		}
 		
 		fireRegionCreated(new RegionEvent(region));
         return region;
 	}
 
 	public void disposeRegion(AbstractSelectionRegion region) {
 		removeRegion(region);
 		setCursor(null);
 		clearRegionTool();
 	}
 	
 	public void setZoomType(final ZoomType zoomType) {
 		clearRegionTool();
         super.setZoomType(zoomType);
 	}
 	
 	protected void clearRegionTool() {
 		if (regionListener!=null) {
 		    removeMouseListener(regionListener);
 		    removeMouseMotionListener(regionListener);
 		    regionListener = null;
 		    setCursor(null);
 		}
 	}
 
 	/**
 	 * 
 	 * @param l
 	 */
 	public boolean addRegionListener(final IRegionListener l) {
 		if (regionListeners == null) regionListeners = new HashSet<IRegionListener>(7);
 		return regionListeners.add(l);
 	}
 	
 	/**
 	 * 
 	 * @param l
 	 */
 	public boolean removeRegionListener(final IRegionListener l) {
 		if (regionListeners == null) return true;
 		return regionListeners.remove(l);
 	}
 
 	protected void fireRegionCreated(RegionEvent evt) {
 		if (regionListeners==null) return;
 		for (IRegionListener l : regionListeners) l.regionCreated(evt);
 	}
 	
 
 	protected void fireRegionAdded(RegionEvent evt) {
 		if (regionListeners==null) return;
 		for (IRegionListener l : regionListeners) l.regionAdded(evt);
 	}
 	
 	protected void fireRegionRemoved(RegionEvent evt) {
 		if (regionListeners==null) return;
 		for (IRegionListener l : regionListeners) l.regionRemoved(evt);
 	}
 	
 	/**
 	 * 
 	 * @param l
 	 */
 	public boolean addImageTraceListener(final ITraceListener l) {
 		if (imageTraceListeners == null) imageTraceListeners = new HashSet<ITraceListener>(7);
 		return imageTraceListeners.add(l);
 	}
 	
 	/**
 	 * 
 	 * @param l
 	 */
 	public boolean removeImageTraceListener(final ITraceListener l) {
 		if (imageTraceListeners == null) return true;
 		return imageTraceListeners.remove(l);
 	}
 
 	
 	protected void fireImageTraceCreated(TraceEvent evt) {
 		if (imageTraceListeners==null) return;
 		for (ITraceListener l : imageTraceListeners) l.traceCreated(evt);
 	}
 	
 
 	protected void fireImageTraceAdded(TraceEvent evt) {
 		if (imageTraceListeners==null) return;
 		for (ITraceListener l : imageTraceListeners) l.traceAdded(evt);
 	}
 	
 	protected void fireImageTraceRemoved(TraceEvent evt) {
 		if (regionListeners==null) return;
 		for (ITraceListener l : imageTraceListeners) l.traceRemoved(evt);
 	}
 
 	
 	public Map<String, AbstractSelectionRegion> getRegionMap() {
 		return regions;
 	}
 	public List<AbstractSelectionRegion> getRegions() {
 		final Collection<AbstractSelectionRegion> vals = regions.values();
 		return new ArrayList<AbstractSelectionRegion>(vals);
 	}
 	
 //	private Image rawImage;
 	
 //	@Override
 //	protected void paintClientArea(final Graphics graphics) {
 	
 // TODO
 //		if (rawImage==null) {
 //			rawImage = new Image(Display.getCurrent(), "C:/tmp/ESRF_Pilatus_Data.png");
 //		}
 //		
 //		final Rectangle bounds = getBounds();
 //		final Image scaled = new Image(Display.getCurrent(),
 //				rawImage.getImageData().scaledTo(bounds.width,bounds.height));
 //		graphics.drawImage(scaled, new Point(0,0));
 //
 //		super.paintClientArea(graphics);
 //
 //	}
 
 	public Collection<String> getRegionNames() {
 		return regions.keySet();
 	}
 
 
 	public void setSelectionProvider(ISelectionProvider provider) {
 		this.selectionProvider = provider;
 	}
 
 
 	public AbstractSelectionRegion getRegion(String name) {
 		if (regions==null) return null;
 		return regions.get(name);
 	}
 
 	class RegionMouseListener extends MouseMotionListener.Stub implements MouseListener {
		private int drag;
 		public RegionMouseListener() {
 			regionPoints = new PointList(2);
 		}
 
 		@Override
 		public void mousePressed(MouseEvent me) {
 			if (me.button == 3) {
 				releaseMouse();
 			} else {
 				regionPoints.addPoint(me.getLocation());
 				regionPoints.addPoint(me.getLocation());
 				drag = regionPoints.size() - 1;
 			}
 			me.consume();
 			repaint();
 		}
 
 		@Override
 		public void mouseReleased(MouseEvent me) {
			if (!regionBeingAdded.useMultipleMousePresses()) {
				releaseMouse();
 			} else {
				
 			}
 			me.consume();
 			repaint();
 		}
 
 		@Override
 		public void mouseDoubleClicked(MouseEvent me) {
 		}
 
 		@Override
 		public void mouseDragged(final MouseEvent me) {
			regionPoints.setPoint(me.getLocation(), drag);
 			me.consume();
 			repaint();
 		}
 
 		@Override
 		public void mouseExited(final MouseEvent me) {
 			// mouseReleased(me);
 		}
 
 		private void releaseMouse() {
 			removeMouseListener(this);
 			removeMouseMotionListener(this);
 			if (regionListener == this) {
 				regionListener = null;
 			} else {
 				clearRegionTool(); // Actually something has gone wrong if this happens.
 			}
 			setCursor(null);
 
 			RegionArea.this.addRegion(regionBeingAdded, false);
 			((XYRegionGraph) xyGraph).getOperationsManager().addCommand(new AddRegionCommand((XYRegionGraph) xyGraph,
 							regionBeingAdded));
 
 			regionBeingAdded.setLocalBounds(regionPoints, getBounds());
 
 			fireRegionAdded(new RegionEvent(regionBeingAdded));
 
 			RegionArea.this.regionBeingAdded = null;
 			regionPoints = null;
 		}
 	}
 
 	protected Map<String,ImageTrace> getImageTraces() {
 		return this.imageTraces;
 	}
 
   
 	/**
 	 * Must call in UI thread safe way.
 	 */
 	public void clearTraces() {
 		
 		final List<Trace> traceList = getTraceList();
 		if (traceList!=null) {
 			for (Trace trace : traceList) {
 				remove(trace);
 				if (trace instanceof LineTrace) ((LineTrace)trace).dispose();
 			}
 			traceList.clear();
 	    }
 		
 		if (imageTraces!=null) {
 			final Collection<ImageTrace> its = new HashSet<ImageTrace>(imageTraces.values());
 			for (ImageTrace trace : its) {
 				final ImageTrace gone = imageTraces.remove(trace.getName());
 				if (gone!=null){
 					trace.remove();
 					fireImageTraceRemoved(new TraceEvent(trace));
 				}
 			}
 
 			imageTraces.clear();
 
 		}
 
 	}
 
 
 	public void setPaletteData(PaletteData data) {
 		if (imageTraces!=null) for (ImageTrace trace : imageTraces.values()) {
 			trace.setPaletteData(data);
 		}
 	}
 
 
 	public void setImageOrigin(ImageOrigin origin) {
 		if (imageTraces!=null) for (ImageTrace trace : imageTraces.values()) {
 			trace.setImageOrigin(origin);
 		}
 	}
 
 
 	public Figure getImageTrace() {
 		if (imageTraces!=null && imageTraces.size()>0) return imageTraces.values().iterator().next();
 		return null;
 	}
 
 
 	public void dispose() {
 		
 		clearTraces();
 		clearRegionsInternal();
 		if (regionListeners!=null)     regionListeners.clear();
 		if (imageTraceListeners!=null) imageTraceListeners.clear();
 		if (regions!=null)             regions.clear();
 		if (imageTraces!=null)         imageTraces.clear();
 	}
 
 }
