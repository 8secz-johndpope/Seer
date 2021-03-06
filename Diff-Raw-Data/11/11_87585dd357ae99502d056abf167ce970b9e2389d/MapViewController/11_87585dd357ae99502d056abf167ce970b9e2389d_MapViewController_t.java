 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
  *
  *  This file is modified by Dimitry Polivaev in 2008.
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.view.swing.map;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Font;
 import java.awt.Graphics;
 import java.awt.Rectangle;
 import java.awt.image.BufferedImage;
 import java.awt.image.RenderedImage;
 import java.io.File;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 
 import org.apache.commons.lang.StringUtils;
 import org.freeplane.core.frame.IMapSelectionListener;
 import org.freeplane.core.frame.IMapViewChangeListener;
 import org.freeplane.core.frame.IMapViewManager;
 import org.freeplane.core.modecontroller.IMapSelection;
 import org.freeplane.core.modecontroller.ModeController;
 import org.freeplane.core.model.MapModel;
 import org.freeplane.core.model.NodeModel;
 
 /**
  * Manages the list of MapViews. As this task is very complex, I exported it
  * from Controller to this class to keep Controller simple. The information
  * exchange between controller and this class is managed by observer pattern
  * (the controller observes changes to the map mapViews here).
  */
 public class MapViewController implements IMapViewManager {
 	static private class MapViewChangeObserverCompound {
 		final private HashSet<IMapSelectionListener> mapListeners = new HashSet();
 		final private HashSet<IMapViewChangeListener> viewListeners = new HashSet();
 
 		void addListener(final IMapSelectionListener listener) {
 			mapListeners.add(listener);
 		}
 
 		void addListener(final IMapViewChangeListener listener) {
 			viewListeners.add(listener);
 		}
 
 		void afterMapChange(final MapView oldMap, final MapView newMap) {
 			final MapModel oldModel = getModel(oldMap);
 			final MapModel newModel = getModel(newMap);
 			for (final Iterator<IMapSelectionListener> iter = mapListeners.iterator(); iter.hasNext();) {
 				final IMapSelectionListener observer = iter.next();
 				if(oldModel != newModel){
 				observer.afterMapChange(oldModel, newModel);
 				}
 			}
 			for (final Iterator<IMapViewChangeListener> iter = viewListeners.iterator(); iter.hasNext();) {
 				final IMapViewChangeListener observer = iter.next();
 				observer.afterViewChange(oldMap, newMap);
 			}
 		}
 
 		void afterMapClose(final MapView pOldMap) {
 			for (final Iterator<IMapSelectionListener> iter = mapListeners.iterator(); iter.hasNext();) {
 				final IMapSelectionListener observer = iter.next();
 				observer.afterMapClose(getModel(pOldMap));
 			}
 			for (final Iterator<IMapViewChangeListener> iter = viewListeners.iterator(); iter.hasNext();) {
 				final IMapViewChangeListener observer = iter.next();
 				observer.afterViewClose(pOldMap);
 			}
 		}
 
 		void beforeMapChange(final MapView oldMap, final MapView newMap) {
 			for (final Iterator<IMapSelectionListener> iter = mapListeners.iterator(); iter.hasNext();) {
 				final IMapSelectionListener observer = iter.next();
 				observer.beforeMapChange(getModel(oldMap), getModel(newMap));
 			}
 			for (final Iterator<IMapViewChangeListener> iter = viewListeners.iterator(); iter.hasNext();) {
 				final IMapViewChangeListener observer = iter.next();
 				observer.beforeViewChange(oldMap, newMap);
 			}
 		}
 
 		private MapModel getModel(final MapView view) {
 			return view == null ? null : view.getModel();
 		}
 
 		boolean isMapChangeAllowed(final MapModel oldMap, final MapModel newMap) {
 			boolean returnValue = true;
 			for (final Iterator iter = new Vector(mapListeners).iterator(); iter.hasNext();) {
 				final IMapSelectionListener observer = (IMapSelectionListener) iter.next();
 				returnValue = observer.isMapChangeAllowed(oldMap, newMap);
 				if (!returnValue) {
 					break;
 				}
 			}
 			return returnValue;
 		}
 
 		void mapViewCreated(final MapView mapView) {
 			for (final Iterator<IMapViewChangeListener> iter = viewListeners.iterator(); iter.hasNext();) {
 				final IMapViewChangeListener observer = iter.next();
 				observer.afterViewCreated(mapView);
 			}
 		}
 
 		void removeListener(final IMapSelectionListener listener) {
 			mapListeners.remove(listener);
 		}
 
 		void removeListener(final IMapViewChangeListener listener) {
 			viewListeners.remove(listener);
 		}
 	}
 
 	private String lastModeName;
 	MapViewChangeObserverCompound listener = new MapViewChangeObserverCompound();
 	/** reference to the current mapmapView; null is allowed, too. */
 	private MapView mapView;
 	/**
 	 * A vector of MapView instances. They are ordered according to their screen
 	 * order.
 	 */
 	final private Vector mapViewVector = new Vector();
 
 	/**
 	 * Reference to the current mode as the mapView may be null.
 	 */
 	public MapViewController() {
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#addMapChangeListener(org.freeplane.core.frame.IMapChangeListener)
 	 */
 	public void addMapChangeListener(final IMapSelectionListener pListener) {
 		listener.addListener(pListener);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#addMapViewChangeListener(org.freeplane.core.frame.IMapViewChangeListener)
 	 */
 	public void addMapViewChangeListener(final IMapViewChangeListener pListener) {
 		listener.addListener(pListener);
 	}
 
 	private void addToOrChangeInMapViews(final String key, final MapView newOrChangedMapView) {
 		String extension = "";
 		int count = 1;
 		final List mapKeys = getMapKeys();
 		while (mapKeys.contains(key + extension)) {
 			extension = "<" + (++count) + ">";
 		}
 		newOrChangedMapView.setName((key + extension));
 		newOrChangedMapView.setName((key + extension));
 		if (!mapViewVector.contains(newOrChangedMapView)) {
 			mapViewVector.add(newOrChangedMapView);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#changeToMapView(org.freeplane.view.swing.map.MapView)
 	 */
 	public boolean changeToMapView(final Component newMapViewComponent) {
 		final MapView newMapView = (MapView) newMapViewComponent;
 		final MapView oldMapView = mapView;
 		if (!listener.isMapChangeAllowed(getModel(oldMapView), getModel(newMapView))) {
 			return false;
 		}
 		listener.beforeMapChange(oldMapView, newMapView);
 		mapView = newMapView;
 		if (mapView != null) {
 			lastModeName = mapView.getModeController().getModeName();
 		}
 		listener.afterMapChange(oldMapView, newMapView);
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#changeToMapView(java.lang.String)
 	 */
 	public boolean changeToMapView(final String mapViewDisplayName) {
 		MapView mapViewCandidate = null;
 		for (final Iterator iterator = mapViewVector.iterator(); iterator.hasNext();) {
 			final MapView mapMod = (MapView) iterator.next();
 			if (StringUtils.equals(mapViewDisplayName, mapMod.getName())) {
 				mapViewCandidate = mapMod;
 				break;
 			}
 		}
 		if (mapViewCandidate == null) {
 			throw new IllegalArgumentException("Map mapView " + mapViewDisplayName + " not found.");
 		}
 		return changeToMapView(mapViewCandidate);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#changeToMode(java.lang.String)
 	 */
 	public boolean changeToMode(final String modeName) {
 		if (modeName.equals(lastModeName)) {
 			return true;
 		}
 		MapView mapViewCandidate = null;
 		for (final Iterator iterator = mapViewVector.iterator(); iterator.hasNext();) {
 			final MapView mapMod = (MapView) iterator.next();
 			if (modeName.equals(mapMod.getModeController().getModeName())) {
 				mapViewCandidate = mapMod;
 				break;
 			}
 		}
 		final boolean changed = changeToMapView(mapViewCandidate);
 		if (changed) {
 			lastModeName = modeName;
 		}
 		return changed;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#checkIfFileIsAlreadyOpened(java.net.URL)
 	 */
 	public String checkIfFileIsAlreadyOpened(final URL urlToCheck) throws MalformedURLException {
 		for (final Iterator iter = mapViewVector.iterator(); iter.hasNext();) {
 			final MapView mapView = (MapView) iter.next();
 			if (getModel(mapView) != null) {
 				final URL mapViewUrl = getModel(mapView).getURL();
 				if (sameFile(urlToCheck, mapViewUrl)) {
 					return mapView.getName();
 				}
 			}
 		}
 		return null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#close(boolean)
 	 */
 	public boolean close(final boolean force) {
 		final MapView mapView = getMapView();
 		final boolean closingNotCancelled = mapView.getModeController().getMapController().close(force);
 		if (!closingNotCancelled) {
 			return false;
 		}
 		int index = mapViewVector.indexOf(mapView);
 		mapViewVector.remove(mapView);
 		if (mapViewVector.isEmpty()) {
 			/* Keep the current running mode */
 			changeToMapView((MapView) null);
 		}
 		else {
 			if (index >= mapViewVector.size() || index < 0) {
 				index = mapViewVector.size() - 1;
 			}
 			changeToMapView(((MapView) mapViewVector.get(index)));
 		}
 		listener.afterMapClose(mapView);
 		return true;
 	}
 
 	public String createHtmlMap() {
 		final MapModel model = getModel();
 		final ClickableImageCreator creator = new ClickableImageCreator(model.getRootNode(),
 			getMapView().getModeController(), "FM$1FM");
 		return creator.generateHtml();
 	}
 
 	public RenderedImage createImage() {
 		final MapView view = getMapView();
 		if (view == null) {
 			return null;
 		}
 		(view).preparePrinting();
 		final Rectangle innerBounds = (view).getInnerBounds();
 		BufferedImage myImage = (BufferedImage) (view).createImage(view.getWidth(), view.getHeight());
 		final Graphics g = myImage.getGraphics();
 		g.clipRect(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height);
 		(view).print(g);
 		myImage = myImage.getSubimage(innerBounds.x, innerBounds.y, innerBounds.width, innerBounds.height);
 		return myImage;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getBackgroundColor(org.freeplane.core.model.NodeModel)
 	 */
 	public Color getBackgroundColor(final NodeModel node) {
 		final MapView mapView = getMapView();
 		if (mapView == null) {
 			return null;
 		}
 		final NodeView nodeView = mapView.getNodeView(node);
 		if (nodeView == null) {
 			return null;
 		}
 		return nodeView.getTextBackground();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getComponent(org.freeplane.core.model.NodeModel)
 	 */
 	public Component getComponent(final NodeModel node) {
		final NodeView nodeView = mapView.getNodeView(node);
		return nodeView != null ? nodeView.getMainView() : null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getFont(org.freeplane.core.model.NodeModel)
 	 */
 	public Font getFont(final NodeModel node) {
 		final MapView mapView = getMapView();
 		if (mapView == null) {
 			return null;
 		}
 		final NodeView nodeView = mapView.getNodeView(node);
 		if (nodeView == null) {
 			return null;
 		}
 		return nodeView.getMainView().getFont();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getMapKeys()
 	 */
 	public List getMapKeys() {
 		final LinkedList returnValue = new LinkedList();
 		for (final Iterator iterator = mapViewVector.iterator(); iterator.hasNext();) {
 			final MapView mapView = (MapView) iterator.next();
 			returnValue.add(mapView.getName());
 		}
 		return Collections.unmodifiableList(returnValue);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getMaps()
 	 */
 	@Deprecated
 	public Map<String, MapModel> getMaps() {
 		final HashMap<String, MapModel> returnValue = new HashMap();
 		for (final Iterator iterator = mapViewVector.iterator(); iterator.hasNext();) {
 			final MapView mapView = (MapView) iterator.next();
 			returnValue.put(mapView.getName(), getModel(mapView));
 		}
 		return Collections.unmodifiableMap(returnValue);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getMapSelection()
 	 */
 	public IMapSelection getMapSelection() {
 		final MapView mapView = getMapView();
 		return mapView == null ? null : mapView.getMapSelection();
 	}
 
 	public MapView getMapView() {
 		return mapView;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getMapViewComponent()
 	 */
 	public Component getMapViewComponent() {
 		return getMapView();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getMapViewVector()
 	 */
 	public List getMapViewVector() {
 		return Collections.unmodifiableList(mapViewVector);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getModel()
 	 */
 	public MapModel getModel() {
 		final MapView mapView = getMapView();
 		return mapView == null ? null : getModel(mapView);
 	}
 
 	private MapModel getModel(final MapView mapView) {
 		return mapView == null ? null : mapView.getModel();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getSelectedComponent()
 	 */
 	public Component getSelectedComponent() {
 		final MapView mapView = getMapView();
 		return mapView == null ? null : mapView.getSelected();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getTextColor(org.freeplane.core.model.NodeModel)
 	 */
 	public Color getTextColor(final NodeModel node) {
 		final MapView mapView = getMapView();
 		if (mapView == null) {
 			return null;
 		}
 		final NodeView nodeView = mapView.getNodeView(node);
 		if (nodeView == null) {
 			return null;
 		}
 		return nodeView.getTextColor();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getViewNumber()
 	 */
 	public int getViewNumber() {
 		return mapViewVector.size();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#getZoom()
 	 */
 	public float getZoom() {
 		return getMapView().getZoom();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#newMapView(org.freeplane.core.model.MapModel, org.freeplane.core.modecontroller.ModeController)
 	 */
 	public void newMapView(final MapModel map, final ModeController modeController) {
 		final MapView mapView = new MapView(map, modeController);
 		addToOrChangeInMapViews(mapView.getName(), mapView);
 		listener.mapViewCreated(mapView);
 		changeToMapView(mapView);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#nextMapView()
 	 */
 	public void nextMapView() {
 		int index;
 		final int size = mapViewVector.size();
 		if (getMapView() != null) {
 			index = mapViewVector.indexOf(getMapView());
 		}
 		else {
 			index = size - 1;
 		}
 		if (index + 1 < size && index >= 0) {
 			changeToMapView(((MapView) mapViewVector.get(index + 1)));
 		}
 		else if (size > 0) {
 			changeToMapView(((MapView) mapViewVector.get(0)));
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#previousMapView()
 	 */
 	public void previousMapView() {
 		int index;
 		final int size = mapViewVector.size();
 		if (getMapView() != null) {
 			index = mapViewVector.indexOf(getMapView());
 		}
 		else {
 			index = 0;
 		}
 		if (index > 0) {
 			changeToMapView(((MapView) mapViewVector.get(index - 1)));
 		}
 		else {
 			if (size > 0) {
 				changeToMapView(((MapView) mapViewVector.get(size - 1)));
 			}
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#removeIMapViewChangeListener(org.freeplane.core.frame.IMapChangeListener)
 	 */
 	public void removeMapChangeListener(final IMapSelectionListener pListener) {
 		listener.removeListener(pListener);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#removeMapViewChangeListener(org.freeplane.core.frame.IMapViewChangeListener)
 	 */
 	public void removeMapViewChangeListener(final IMapViewChangeListener pListener) {
 		listener.removeListener(pListener);
 	}
 
 	private boolean sameFile(final URL urlToCheck, final URL mapViewUrl) {
 		if (mapViewUrl == null) {
 			return false;
 		}
 		if (urlToCheck.getProtocol().equals("file") && mapViewUrl.getProtocol().equals("file")) {
 			return (new File(urlToCheck.getFile())).equals(new File(mapViewUrl.getFile()));
 		}
 		return urlToCheck.sameFile(mapViewUrl);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#scrollNodeToVisible(org.freeplane.core.model.NodeModel)
 	 */
 	public void scrollNodeToVisible(final NodeModel node) {
 		final NodeView nodeView = mapView.getNodeView(node);
 		if (nodeView != null) {
 			mapView.scrollNodeToVisible(nodeView);
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#setZoom(float)
 	 */
 	public void setZoom(final float zoom) {
 		getMapView().setZoom(zoom);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#tryToChangeToMapView(java.lang.String)
 	 */
 	public boolean tryToChangeToMapView(final String mapView) {
 		if (mapView != null && getMapKeys().contains(mapView)) {
 			changeToMapView(mapView);
 			return true;
 		}
 		else {
 			return false;
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#updateMapView()
 	 */
 	public void updateMapView() {
 		mapView.getRoot().updateAll();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.freeplane.core.frame.IMapViewController#updateMapViewName()
 	 */
 	public void updateMapViewName() {
 		MapView r = getMapView();
 		final String name = r.getModel().getTitle();
         r.setName(name);
 		addToOrChangeInMapViews(getMapView().getName(), getMapView());
 		changeToMapView(getMapView());
 	}
 
 	public ModeController getModeController(Component mapView) {
 		return ((MapView)mapView).getModeController();
     }
 
 	public MapModel getModel(Component mapView) {
 		return ((MapView)mapView).getModel();
     }
 }
