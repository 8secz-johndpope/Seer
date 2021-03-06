 /***************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 *
 *              ... and it just works.
 *
 ****************************************************/
 /*
  * OverviewComponent.java
  *
  * Created on 5. Maerz 2008, 21:33
  */
 package de.cismet.cismap.commons.gui.overviewwidget;
 
 import com.vividsolutions.jts.geom.Coordinate;
 
 import edu.umd.cs.piccolo.PCamera;
 
 import org.deegree.model.crs.GeoTransformer;
 
 import org.jdom.Element;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 
 import javax.swing.JFrame;
 
 import de.cismet.cismap.commons.BoundingBox;
 import de.cismet.cismap.commons.Crs;
 import de.cismet.cismap.commons.CrsTransformer;
 import de.cismet.cismap.commons.RetrievalServiceLayer;
 import de.cismet.cismap.commons.XBoundingBox;
 import de.cismet.cismap.commons.gui.MappingComponent;
 import de.cismet.cismap.commons.gui.layerwidget.ActiveLayerModel;
 import de.cismet.cismap.commons.interaction.CismapBroker;
 import de.cismet.cismap.commons.raster.wms.simple.SimpleWMS;
 import de.cismet.cismap.commons.raster.wms.simple.SimpleWmsGetMapUrl;
 import de.cismet.cismap.commons.retrieval.RetrievalEvent;
 import de.cismet.cismap.commons.retrieval.RetrievalListener;
 
 import de.cismet.tools.configuration.Configurable;
 import de.cismet.tools.configuration.NoWriteError;
 
 import de.cismet.tools.gui.log4jquickconfig.Log4JQuickConfig;
 
 /**
  * DOCUMENT ME!
  *
  * @author   hell
  * @version  $Revision$, $Date$
  */
 public class OverviewComponent extends javax.swing.JPanel implements Configurable {
 
     //~ Instance fields --------------------------------------------------------
 
     MappingComponent overviewMap = null;
     MappingComponent masterMap = null;
     ActiveLayerModel model = new ActiveLayerModel();
     private final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
     private String url =
         "http://geoportal.wuppertal.de/deegree/wms?&VERSION=1.1.1&REQUEST=GetMap&WIDTH=<cismap:width>&HEIGHT=<cismap:height>&BBOX=<cismap:boundingBox>&SRS=EPSG:31466&FORMAT=image/png&TRANSPARENT=true&BGCOLOR=0xF0F0F0&EXCEPTIONS=application/vnd.ogc.se_xml&LAYERS=R102:stadtplan2007&STYLES=default"; // NOI18N
     SimpleWMS backgroundService = new SimpleWMS(new SimpleWmsGetMapUrl(url));
     Crs srs = new Crs("EPSG:31466", "EPSG:31466", "EPSG:31466", true, true);                                                                                                                                                                                                                              // NOI18N
     XBoundingBox home = new XBoundingBox(2567799, 5670041, 2594650, 5688258, srs.getCode(), srs.isMetric());
 
     //~ Constructors -----------------------------------------------------------
 
     /**
      * Creates new form OverviewComponent.
      */
     public OverviewComponent() {
         initComponents();
         overviewMap = new MappingComponent();
         add(overviewMap, BorderLayout.CENTER);
         overviewMap.setInteractionMode(MappingComponent.OVERVIEW);
         overviewMap.setReadOnly(true);
         overviewMap.setFixedBoundingBox(home);
         model.setSrs(srs);
         model.addHome(home);
         overviewMap.setMappingModel(model);
         revalidate();
     }
 
     //~ Methods ----------------------------------------------------------------
 
     /**
      * DOCUMENT ME!
      */
     public void initBackgroundService() {
         if (log.isDebugEnabled()) {
             log.debug("initBackgroundService"); // NOI18N
         }
         model = new ActiveLayerModel();
 
         overviewMap.setFixedBoundingBox(home);
         model.setSrs(srs);
         model.setDefaultHomeSrs(srs);
         model.addHome(home);
         overviewMap.setMappingModel(model);
        overviewMap.resetWtst();
         model.removeAllLayers();
         model.addLayer((RetrievalServiceLayer)backgroundService);
         backgroundService.addRetrievalListener(new RetrievalListener() {
 
                 @Override
                 public void retrievalStarted(final RetrievalEvent e) {
                 }
                 @Override
                 public void retrievalProgress(final RetrievalEvent e) {
                 }
                 @Override
                 public void retrievalComplete(final RetrievalEvent e) {
                 }
                 @Override
                 public void retrievalAborted(final RetrievalEvent e) {
                 }
                 @Override
                 public void retrievalError(final RetrievalEvent e) {
                 }
             });
     }
 
     /**
      * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
      * content of this method is always regenerated by the Form Editor.
      */
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
         setLayout(new java.awt.BorderLayout());
     } // </editor-fold>//GEN-END:initComponents
     // Variables declaration - do not modify//GEN-BEGIN:variables
     // End of variables declaration//GEN-END:variables
 
     /**
      * DOCUMENT ME!
      *
      * @param  args  DOCUMENT ME!
      */
     public static void main(final String[] args) {
         Log4JQuickConfig.configure4LumbermillOnLocalhost();
         final JFrame f = new JFrame();
         f.setSize(500, 500);
         f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         final OverviewComponent ov = new OverviewComponent();
         f.getContentPane().add(ov);
         f.setVisible(true);
         ov.overviewMap.unlock();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
     public MappingComponent getOverviewMap() {
         return overviewMap;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param  overviewMap  DOCUMENT ME!
      */
     public void setOverviewMap(final MappingComponent overviewMap) {
         this.overviewMap = overviewMap;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return  DOCUMENT ME!
      */
     public MappingComponent getMasterMap() {
         return masterMap;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param  masterMap  DOCUMENT ME!
      */
     public void setMasterMap(final MappingComponent masterMap) {
         this.masterMap = masterMap;
         final Color fill = new Color(255, 0, 0, 100);
         masterMap.getCamera()
                 .addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, new PropertyChangeListener() {
 
                         @Override
                         public void propertyChange(final PropertyChangeEvent evt) {
                             try {
                                 BoundingBox bb = masterMap.getCurrentBoundingBoxFromCamera();
 
                                 if (!CismapBroker.getInstance().getSrs().getCode().equals(srs.getCode())) {
                                     final CrsTransformer transformer = new CrsTransformer(srs.getCode());
                                     bb = transformer.transformBoundingBox(
                                             bb,
                                             CismapBroker.getInstance().getSrs().getCode());
                                 }
 
                                 final double x = bb.getX1() + (bb.getWidth() / 2.0);
                                 final double y = bb.getY2() - (bb.getHeight() / 2.0);
                                 overviewMap.getWtst();
                                 overviewMap.outlineArea(bb, fill);
                                 // overviewMap.crossHairPoint(new Coordinate(2583781, 5682540));
                                 overviewMap.crossHairPoint(new Coordinate(x, y));
                             } catch (Exception e) {
                                 log.error(
                                     "Cannot transform the current boundingbox from "
                                     + CismapBroker.getInstance().getSrs().getCode()
                                     + " to "
                                     + srs.getCode(),
                                     e);
                             }
                         }
                     });
     }
 
     @Override
     public void configure(final Element parent) {
     }
 
     @Override
     public Element getConfiguration() throws NoWriteError {
         return null;
     }
 
     @Override
     public void masterConfigure(final Element parent) {
         try {
             final Element prefs = parent.getChild("cismapOverviewComponentPreferences"); // NOI18N
             try {
                 // the following crs object is incomplete and should only be used within the OverviewComponent
                 final Crs tmp = new Crs();
                 tmp.setCode(prefs.getAttributeValue("srs"));
                 srs = tmp;
             } catch (Exception skip) {
             }
 
             try {
                 // TODO determine, whether the home CRS is metric or not
                 home = new XBoundingBox(prefs.getChild("overviewExtent"), srs.getCode(), true); // NOI18N
             } catch (Exception skip) {
             }
 
             try {
                 final SimpleWMS simpleWMS = new SimpleWMS(prefs.getChild("background").getChild("simpleWms")); // NOI18N
                 backgroundService = simpleWMS;
             } catch (Exception skip) {
             }
             initBackgroundService();
         } catch (Exception e) {
             log.warn("Fehler beim Konfigurieren der OverviewComponent. Fallback=Stadtplan", e);                // NOI18N
             initBackgroundService();
         }
     }
 }
