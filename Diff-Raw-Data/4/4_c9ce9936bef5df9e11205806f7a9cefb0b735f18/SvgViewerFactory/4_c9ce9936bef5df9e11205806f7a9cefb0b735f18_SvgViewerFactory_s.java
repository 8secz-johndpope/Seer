 package org.freeplane.plugin.svg;
 
 import java.awt.Dimension;
 import java.net.URI;
 
 import javax.swing.JComponent;
 import javax.swing.SwingUtilities;
 
 import org.apache.batik.swing.JSVGCanvas;
 import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
 import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
 import org.apache.batik.util.SVGConstants;
 import org.freeplane.core.resources.ResourceBundles;
 import org.freeplane.core.resources.ResourceController;
 import org.freeplane.view.swing.addins.filepreview.ExternalResource;
 import org.freeplane.view.swing.addins.filepreview.IViewerFactory;
 import org.freeplane.view.swing.addins.filepreview.ViewerLayoutManager;
 import org.freeplane.view.swing.map.MapView;
 import org.w3c.dom.svg.SVGDocument;
 import org.w3c.dom.svg.SVGLength;
 import org.w3c.dom.svg.SVGSVGElement;
 
 public class SvgViewerFactory implements IViewerFactory {
 
 	private final class ViewerComponent extends JSVGCanvas {
 	    /**
 	     * 
 	     */
 	    private static final long serialVersionUID = 1L;
 	    private Dimension originalSize = null;
 
 	    protected Dimension getOriginalSize() {
         	return new Dimension(originalSize);
         }
 
 		public ViewerComponent(final URI uri) {
 	        super(null, false, false);
 	        setDocumentState(ALWAYS_STATIC);
 	        setSize(1, 1);
 			addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
 	            @Override
                 public void gvtRenderingStarted(GVTTreeRendererEvent e) {
 	                super.gvtRenderingStarted(e);
 					final SVGDocument document = getSVGDocument();
 					final SVGSVGElement rootElement = document.getRootElement();
 					final SVGLength width = rootElement.getWidth().getBaseVal();
 					final SVGLength height = rootElement.getHeight().getBaseVal();
					float defaultWidth = width.getValue();	
					float defaultHeigth = height.getValue();
 					if(defaultWidth == 1f && defaultHeigth == 1f){
 						defaultWidth = ResourceController.getResourceController().getIntProperty("default_external_component_width", 200);
 						defaultHeigth = ResourceController.getResourceController().getIntProperty("default_external_component_height", 200);
 					}
 					originalSize = new Dimension((int)defaultWidth, (int)defaultHeigth );
 					if("".equals(rootElement.getAttributeNS(null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE))){
 						rootElement.setAttributeNS(null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 "+ defaultWidth + " " + defaultHeigth);
 					}
 					setSize(originalSize);
 	                removeGVTTreeRendererListener(this);
                 }
 	        });
 			setURI (uri.toString());
         }
 
 		@Override
 	    public Dimension getPreferredSize() {
 			if(originalSize == null){
 				return new Dimension(1, 1);
 			}
 	    	return super.getPreferredSize();
 	    }
     }
 
 	public boolean accept(URI uri) {
 		return uri.getRawPath().endsWith(".svg");
 	}
 
 	public String getDescription() {
 		return  ResourceBundles.getText("svg");
 	};
 
 	public JComponent createViewer(final ExternalResource resource, final URI uri) {
 		final ViewerComponent canvas = new ViewerComponent(uri);
 		canvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
 
 			public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
             	final float r = resource.getZoom();
             	Dimension preferredSize = canvas.getOriginalSize();
             	preferredSize.width = (int)(Math.rint(preferredSize.width * r));
             	preferredSize.height = (int)(Math.rint(preferredSize.height * r));
             	canvas.setPreferredSize(preferredSize);
             	canvas.setLayout(new ViewerLayoutManager(1f));
             	canvas.revalidate();
             	canvas.removeGVTTreeRendererListener(this);
             }
         });
 		return canvas;
 	}
 
 	public JComponent createViewer(final URI uri, final Dimension preferredSize) {
 		final ViewerComponent canvas = new ViewerComponent(uri);
 		canvas.setPreferredSize(preferredSize);
 		canvas.setSize(preferredSize);
 		canvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
 
 			public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
 				canvas.setMySize(preferredSize);
 				canvas.setSize(preferredSize);
 				canvas.revalidate();
             	canvas.removeGVTTreeRendererListener(this);
             }
         });
 		return canvas;
 	}
 
 	public Dimension getOriginalSize(JComponent viewer) {
 		final ViewerComponent canvas = (ViewerComponent) viewer;
 		return canvas.getOriginalSize();
 	}
 
 	public void setViewerSize(JComponent viewer, Dimension size) {
 		final JSVGCanvas canvas = (JSVGCanvas) viewer;
 		canvas.setMySize(size);
 	}
 }
