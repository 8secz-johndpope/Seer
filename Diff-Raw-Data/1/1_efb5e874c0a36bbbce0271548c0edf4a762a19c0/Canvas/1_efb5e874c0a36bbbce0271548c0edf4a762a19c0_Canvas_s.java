 package me.teaisaweso.client;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.event.dom.client.MouseDownEvent;
 import com.google.gwt.event.dom.client.MouseMoveEvent;
 import com.google.gwt.event.dom.client.MouseUpEvent;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiHandler;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
 
 /**
  * Class used to handle clicks on canvas.
  * 
  * @author ed/ben
  * 
  * @mouseDown stores the first point the user dragged from
  * @mouseMove continually updates the end coordinates
  * @mouseUp when this is called the last set of coordinates are 
  * used as the end coordinates.
  * Finally the canvas calls for the options panel from the
  * graphemeui class.
  */
 public class Canvas extends Composite{
 
 	private static UiBinderCanvas uiBinder = GWT
 			.create(UiBinderCanvas.class);
 
 	interface UiBinderCanvas extends UiBinder<Widget, Canvas> {
 	}
 	
 	public Graphemeui parent;
 	public int x1, x2, y1, y2;
 	@UiField
 	public CanvasWrapper canvasPanel;	
 
 	public Canvas(Graphemeui parent) {
 		initWidget(uiBinder.createAndBindUi(this));
 		this.parent = parent;
 	}
 	
 	@UiHandler("canvasPanel")
 	void onMouseDown(MouseDownEvent e){
 		x1 = e.getX();
 		y1 = e.getY();
 	}
 	
 	@UiHandler("canvasPanel")
 	void onMouseMove(MouseMoveEvent e){
 		x2 = e.getX();
 		y2 = e.getX();
 	}
 	
 	@UiHandler("canvasPanel")
 	void onMouseUp(MouseUpEvent e){
 		if(parent.tools.getTool() == 1){
 			parent.initOptions(x1, y1, x2, y2);
 		}
 	}
 }
