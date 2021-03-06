 package org.vaadin.addon.leaflet;
 
 import java.lang.reflect.Method;
 
import org.peimari.gleaflet.client.DragEndListener;
import org.peimari.gleaflet.client.Event;
import org.vaadin.addon.leaflet.client.vaadin.ClickServerRpc;
 import org.vaadin.addon.leaflet.client.vaadin.DragEndServerRpc;
 import org.vaadin.addon.leaflet.client.vaadin.LeafletMarkerClientRpc;
 import org.vaadin.addon.leaflet.client.vaadin.LeafletMarkerState;
 import org.vaadin.addon.leaflet.client.vaadin.PopupState;
 import org.vaadin.addon.leaflet.shared.Point;
 
 import com.vaadin.util.ReflectTools;
 
 /**
  * Prototype. This might be technically easier to implement as an extension with
  * Leaflet, but I'm trying this way to pave way for OL integration (which has
  * vectorlayer, extensions cannot have children -> needs to be
  * componentcontainer)
  * 
  */
 public class LMarker extends AbstractLeafletLayer {
 	
 	public static class DragEndEvent extends Event {
 
 		public DragEndEvent(LMarker source) {
 			super(source);
 		}
 		
 	}
 	
 	public interface DragEndListener {
 		Method METHOD = ReflectTools.findMethod(DragEndListener.class, "dragEnd", DragEndEvent.class);
 
 		public void dragEnd(DragEndEvent event);
 	}
 
     @Override
     protected LeafletMarkerState getState() {
         return (LeafletMarkerState) super.getState();
     }
 
     public LMarker(double lat, double lon) {
     	this();
         getState().point = new Point(lat, lon);
     }
 
     public LMarker() {
 		registerRpc(new DragEndServerRpc() {
 			
 			@Override
 			public void dragEnd(Point point) {
 				setPoint(point);
 				fireEvent(new DragEndEvent(LMarker.this));
 			}
 		});
 
     }
 
     public LMarker(Point point) {
         getState().point = point;
     }
 
     public void setPoint(Point p) {
         getState().point = p;
     }
 
     public Point getPoint() {
         return getState().point;
     }
 
     public void setIconSize(Point point) {
         getState().iconSize = point;
     }
 
     public void setIconAnchor(Point point) {
         getState().iconAnchor = point;
     }
 
     public void setTitle(String title) {
         getState().title = title;
     }
 
     public void setDivIcon(String divIcon) {
         getState().divIcon = divIcon;
     }
 
     public void setPopup(String popup) {
         getState().popup = popup;
     }
 
     public void setPopupState(PopupState popupState){
         getState().popupState = popupState;
     }
 
     public void openPopup() {
         getRpcProxy(LeafletMarkerClientRpc.class).openPopup();
     }
 
     public void closePopup() {
         getRpcProxy(LeafletMarkerClientRpc.class).closePopup();
     }
     
 	public void addDragEndListener(DragEndListener listener) {
 		addListener("dragend", DragEndEvent.class, listener,
 				DragEndListener.METHOD);
 	}
 	
 	public void removeDragEndListener(DragEndListener listener) {
 		removeListener("dragend", DragEndEvent.class, listener);
 	}
 
 
 }
