 /*
  * This software is provided "AS IS" without a warranty of any kind.
  * You use it on your own risk and responsibility!!!
  *
  * This file is shared under BSD v3 license.
  * See readme.txt and BSD3 file for details.
  *
  */
 
 package kendzi.josm.kendzi3d.jogl;
 
 import java.awt.Color;
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.media.opengl.GL2;
 import javax.media.opengl.glu.GLU;
 import javax.media.opengl.glu.GLUquadric;
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 
 import kendzi.jogl.DrawUtil;
 import kendzi.jogl.model.factory.FaceFactory;
 import kendzi.jogl.model.factory.FaceFactory.FaceType;
 import kendzi.jogl.model.factory.MeshFactory;
 import kendzi.jogl.model.factory.ModelFactory;
 import kendzi.jogl.model.geometry.material.AmbientDiffuseComponent;
 import kendzi.jogl.model.geometry.material.Material;
 import kendzi.jogl.model.render.ModelRender;
 import kendzi.josm.kendzi3d.jogl.layer.Layer;
 import kendzi.josm.kendzi3d.jogl.model.Model;
 import kendzi.josm.kendzi3d.jogl.model.Perspective3D;
 import kendzi.josm.kendzi3d.jogl.model.lod.DLODSuport;
 import kendzi.josm.kendzi3d.jogl.model.lod.LOD;
 import kendzi.josm.kendzi3d.jogl.selection.ArrowEditor;
 import kendzi.josm.kendzi3d.jogl.selection.Editor;
 import kendzi.josm.kendzi3d.jogl.selection.Selection;
 import kendzi.math.geometry.ray.Ray3d;
 import kendzi.math.geometry.ray.Ray3dUtil;
 
 import org.apache.log4j.Logger;
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.data.Bounds;
 import org.openstreetmap.josm.data.coor.EastNorth;
 import org.openstreetmap.josm.data.coor.LatLon;
 import org.openstreetmap.josm.data.osm.DataSet;
 import org.openstreetmap.josm.data.osm.DataSource;
 import org.openstreetmap.josm.data.osm.Node;
 import org.openstreetmap.josm.data.osm.Way;
 import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
 import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
 import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
 import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
 import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
 import org.openstreetmap.josm.data.projection.Projection;
 
 public class RenderJOSM implements DataSetListenerAdapter.Listener {
 
     /** Log. */
     private static final Logger log = Logger.getLogger(RenderJOSM.class);
 
     DataSet dataSet = null;
 
     public static double lod1 = 20 * 20;
 
     private static double lod2 = 100 * 100;
 
     private static double lod3 = 500 * 500;
 
     private static double lod4 = 1000 * 1000;
 
 
 
     /**
      * Request for rebuildin models.
      */
     private boolean datasetChanged = true;
 
     /**
      * Perspective used by OpenGl. Transforming coordinates from EastNorth to OpenGl .
      */
     private Perspective3D pers;
 
 
     /**
      * Perspective used by OpenGl. Transforming coordinates from EastNorth to OpenGl .
      */
     private static Perspective3D perspective3D;
 
     public static Perspective3D getPerspective3D() {
         return perspective3D;
     }
 
 
 //    /**
 //     * X Offset of coordinate system used by openGl.
 //     */
 //    private double centerX = 0;
 //
 //    /**
 //     * Y Offset of coordinate system used by openGl.
 //     */
 //    private double centerY = 0;
 
 
     /**
      * Position of sun. XXX
      */
     private float lightPos[] = new float[] { 0.0f, 1.0f, 1.0f, 0f };
 
     /**
      * Model displayed when error happen.
      */
     private kendzi.jogl.model.geometry.Model errorModel;
 
 
     /**
      * Renderer of model.
      */
     private ModelRender modelRender;
 
     /**
      * Center point of world.
      */
     private EastNorth center;
     private List<Layer> layerList = new ArrayList<Layer>();
 
     private Selection selection;
 
     private GLUquadric quadratic;   // Storage For Our Quadratic Objects
     private GLU glu = new GLU();
 
     public Perspective3D getPerspective() {
         return this.pers;
     }
     public void setPerspective(Perspective3D pers) {
         this.pers = pers;
     }
 
     public RenderJOSM() {
 
 
         // FIXME TODO XXX
         DatasetEventManager.getInstance().addDatasetListener(new DataSetListenerAdapter(this),
                 FireMode.IMMEDIATELY);
 
         this.errorModel = createErrorModel();
 
         this.center = new EastNorth(0, 0);
 
         this.pers = new Perspective3D(1, 0, 0);
 
     }
 
     public void init(GL2 gl) {
         //
         this.quadratic = this.glu.gluNewQuadric();
         this.glu.gluQuadricNormals(this.quadratic, GLU.GLU_SMOOTH); // Create Smooth Normals
        // glu.gluQuadricTexture(quadratic, true);
        // glu.gluQuadricTexture(quadratic, true);
     }
 
     public void draw(GL2 gl, Camera camera) {
 
         // _direction_
         gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, this.lightPos, 0);
 
         if (this.datasetChanged) {
             this.datasetChanged = false;
             rebuildData();
         }
 
 //        for (Model r : this.modelList) {
 //            drawModel(r, gl, camera);
 //        }
 
         for (Layer layer : this.layerList ) {
             List<Model> models = layer.getModels();
             for (Model r : models) {
                 drawModel(r, gl, camera);
             }
         }
 
         if (this.modelRender.isDebugging()) {
             drawSelectable(gl);
 
         }
         Selection selection = this.selection;
         if (selection != null) {
             drawEditors(gl, selection);
         }
 
         this.modelRender.setupDefaultMaterial(gl);
     }
     private void drawEditors(GL2 gl, Selection selection) {
         List<Editor> editors = selection.getEditors();
 
         gl.glDisable(GL2.GL_TEXTURE_2D);
        // gl.glEnable(GL2.GL_LIGHTING);
 
         for (Editor editor : editors) {
             if (editor instanceof ArrowEditor) {
                 ArrowEditor ae = (ArrowEditor) editor;
                 Point3d p = ae.getPoint();
                 Vector3d v = ae.getVector();
                 double l = ae.getLength();
 
                 if (ae.isSelect()) {
                     gl.glColor3fv(Color.RED.darker().darker().darker().getRGBComponents(new float[4]), 0);
                 } else {
                     gl.glColor3fv(Color.green.darker().darker().darker().getRGBComponents(new float[4]), 0);
                 }
 
                 gl.glPushMatrix();
                 gl.glTranslated(p.x, p.y, p.z);
 
                DrawUtil.drawDotY(gl, 0.3, 6);
 
                 gl.glPopMatrix();
 
 
                 gl.glPushMatrix();
                 gl.glTranslated(p.x + v.x * l, p.y + v.y * l, p.z + v.z * l);
 
                // DrawUtil.drawDotY(gl, 0.3, 6);
              // Draw A Sphere With A Radius Of 1 And 16 Longitude And 16 Latitude Segments
                 this.glu.gluSphere(this.quadratic, 0.3f, 32, 32);
 //                http://www.felixgers.de/teaching/jogl/gluQuadricPrimitives.html
              // A Cylinder With A Radius Of 0.5 And A Height Of 2
 //                glu.gluCylinder(quadratic, 1.0f, 1.0f, 3.0f, 32, 32);
 
                 gl.glPopMatrix();
             }
         }
     }
     /**
      * @param r
      * @param gl
      * @param camera
      */
     public void drawModel(Model r, GL2 gl, Camera camera) {
         if (r.isError()) {
             drawError(r, gl, camera);
             return;
         }
 
 //            r.isInCamraRange(pCamraX, pCamraY, pCamraRange)
         try {
 
             if (r instanceof DLODSuport) {
                 DLODSuport lodModel = ((DLODSuport) r);
                 LOD pLod = getLods(lodModel, camera);
                 if (!lodModel.isModelBuild(pLod)) {
                     lodModel.buildModel(pLod);
                 }
 
                 lodModel.draw(gl, camera, pLod);
 
             } else {
                     if (!r.isBuildModel()) {
                         r.buildModel();
                     }
 
                     r.draw(gl, camera);
 
             }
 
         } catch (Exception e) {
             e.printStackTrace();
             r.setError(true);
             drawError(r, gl, camera);
         }
     }
 
     private void drawError(Model r, GL2 pGl, Camera camera) {
         double x = r.getX();
         double y = r.getY();
 
         pGl.glPushMatrix();
         pGl.glTranslated(x, 0, -y);
 
         this.modelRender.render(pGl, this.errorModel);
 
         pGl.glPopMatrix();
     }
 
     kendzi.jogl.model.geometry.Model createErrorModel() {
         ModelFactory modelBuilder = ModelFactory.modelBuilder();
 
         Material material = new Material(new AmbientDiffuseComponent(Color.RED, Color.RED));
 //        material.ambientColor = Color.RED;
 //        material.diffuseColor = Color.RED;
 //        material.specularColor = Color.RED;
 //
 //        material.emissive = Color.RED;
 //
 //        material.shininess = (float) 0.5;
 
 //        material.shininess2 = (float) 0.5;
 
         int mat = modelBuilder.addMaterial(material);
 
         MeshFactory mesh = modelBuilder.addMesh("walls");
 
         mesh.materialID = mat;
         mesh.hasTexture = false;
 
         int v1 = mesh.addVertex(new Point3d(0, 0, 0));
         int v2 = mesh.addVertex(new Point3d(0, 1, 1));
         int v3 = mesh.addVertex(new Point3d(1, 1, 0));
         int v4 = mesh.addVertex(new Point3d(0, 1, -1));
         int v5 = mesh.addVertex(new Point3d(-1, 1, 0));
         int v6 = mesh.addVertex(new Point3d(0, 2, 0));
 
         //XXX recalculate normals!
 
         int n1 = mesh.addNormal(new Vector3d(0, 1, 0));
 
         FaceFactory face = mesh.addFace(FaceType.TRIANGLES);
 
         face.addVertIndex(v1);
         face.addVertIndex(v2);
         face.addVertIndex(v3);
 
         face.addVertIndex(v1);
         face.addVertIndex(v3);
         face.addVertIndex(v4);
 
         face.addVertIndex(v1);
         face.addVertIndex(v4);
         face.addVertIndex(v5);
 
         face.addVertIndex(v1);
         face.addVertIndex(v5);
         face.addVertIndex(v2);
 
         face.addVertIndex(v6);
         face.addVertIndex(v3);
         face.addVertIndex(v2);
 
         face.addVertIndex(v6);
         face.addVertIndex(v4);
         face.addVertIndex(v3);
 
         face.addVertIndex(v6);
         face.addVertIndex(v5);
         face.addVertIndex(v4);
 
         face.addVertIndex(v6);
         face.addVertIndex(v2);
         face.addVertIndex(v5);
 
 
         kendzi.jogl.model.geometry.Model model = modelBuilder.toModel();
         model.setUseLight(true);
         model.setUseTexture(true);
 
         return model;
 
     }
 
 
     private LOD getLods(DLODSuport r, Camera camera) {
         return getLods(r.getPoint(), camera.getPoint());
     }
 
 
     public static LOD getLods(Point3d point, Point3d camera) {
 
 //        double lod5 = 1000 * 1000;
 
 //        //XXX temporary
 //        Point3d point = r.getPoint();
 //        Point3d c = camera.getPoint();
         double dx = camera.x - point.x;
         double dy = camera.y - point.y;
         double dz = camera.z - point.z;
 
         double distance = dx * dx  + dy * dy + dz * dz;
 
         if (distance < lod1) {
             return LOD.LOD1;
         } else if (distance < lod2) {
             return LOD.LOD2;
         } else if (distance < lod3) {
             return LOD.LOD3;
         } else if (distance < lod4) {
             return LOD.LOD4;
         }
         return LOD.LOD5;
 
     }
 
     public DataSet getDataSet() {
         return this.dataSet;
 //        return Main.main.getCurrentDataSet();
     }
 
     public void rebuildData() {
         DataSet dataset = getDataSet();
 
         if (dataset == null) {
             return;
         }
 
         //this.modelList.clear();
 
         for (Layer layer : this.layerList ) {
             layer.clear();
         }
 
         for (Node node : dataset.getNodes()) {
 
             if (node.isDeleted()) {
                 continue;
             }
 
             // layers
             for (Layer layer : this.layerList ) {
                 if (layer.getNodeMatcher() != null && layer.getNodeMatcher().match(node)) {
                     layer.addModel(node, this.pers);
                 }
             }
         }
 
         for (Way way : dataset.getWays()) {
 
             if (way.isDeleted()) {
                 continue;
             }
 
             // layers
             for (Layer layer : this.layerList ) {
                 if (layer.getWayMatcher() != null && layer.getWayMatcher().match(way)) {
                     layer.addModel(way, this.pers);
                 }
             }
         }
 
         for (org.openstreetmap.josm.data.osm.Relation relation : dataset.getRelations()) {
 
             if (relation.isDeleted()) {
                 continue;
             }
 
             // layers
             for (Layer layer : this.layerList) {
                 if (layer.getRelationMatcher() != null && layer.getRelationMatcher().match(relation)) {
                     layer.addModel(relation, this.pers);
                 }
             }
         }
     }
 
     private void setupPerspective3D(EastNorth center) {
         Projection proj = Main.getProjection();
 
         LatLon l1 = proj.eastNorth2latlon(center.add(1, 0));
         LatLon l2 = proj.eastNorth2latlon(center.add(-1, 0));
 
         double dist = l1.greatCircleDistance(l2);
 
         double scale = dist / 2d;
 
         this.pers = new Perspective3D(scale, center.getX(), center.getY());
     }
 
 
 
 
     @Override
     public void processDatasetEvent(AbstractDatasetChangedEvent pEvent) {
 
 
         if (this.center == null || pEvent == null) {
         // for tests we change center only on menu button action
             this.center = Main.map.mapView.getCenter();
         }
 
         if (pEvent != null) {
             this.dataSet = pEvent.getDataset();
         }
 
         if (pEvent instanceof DataChangedEvent) {
             AbstractDatasetChangedEvent dce = pEvent;
 
 
 
             try {
             if (Main.main.getCurrentDataSet() != null && !Main.main.getCurrentDataSet().equals(this.dataSet)) {
 //                log.info("TESTTTT !!!!!!!!!!!!!!!!!!!! bad data set ?");
             }
             } catch (Exception e) {
 //                e.printStackTrace();
             }
 
 
             if (this.dataSet != null) {
 
                 Projection proj = Main.getProjection();
 
                 this.center = centerFromDataSet(this.dataSet, proj);
 
             } else {
                 this.center = new EastNorth(0, 0);
             }
         }
 
         setupPerspective3D(this.center);
 
         this.datasetChanged = true;
     }
 
 
 
     public static Bounds boundsFromDataSet(DataSet dataset) {
         // XXX move to util
 
         Bounds bounds = null;
         for (Bounds b : dataset.getDataSourceBounds()) {
             if (bounds == null) {
                 bounds = new Bounds(b);
             } else {
                 bounds.extend(b.getMax());
                 bounds.extend(b.getMin());
             }
         }
 
         return bounds;
 
     }
 
     private EastNorth centerFromDataSet(DataSet dataset, Projection proj) {
 
         double maxX = -Double.MAX_VALUE;
         double minX = Double.MAX_VALUE;
 
         double maxY = -Double.MAX_VALUE;
         double minY = Double.MAX_VALUE;
 
         if (dataset.dataSources.size() > 0) {
             // it is dataset connected with OSM. Get bounds.
             for (DataSource source : dataset.dataSources) {
                 // create area from data bounds
                 LatLon min = source.bounds.getMin();
                 LatLon max = source.bounds.getMax();
 
                 if (minX > min.getX()) {
                     minX = min.getX();
                 }
                 if (minY > min.getY()) {
                     minY = min.getY();
                 }
                 if (maxX < max.getX()) {
                     maxX = max.getX();
                 }
                 if (maxY < max.getY()) {
                     maxY = max.getY();
                 }
             }
         } else {
             // it is newly created dataset not connected with OSM
             for (Node n : dataset.getNodes()) {
              // create area from data bounds
                 LatLon cord = n.getCoor();
 
                 if (cord == null) {
                     continue;
                 }
 
                 if (minX > cord.getX()) {
                     minX = cord.getX();
                 }
                 if (minY > cord.getY()) {
                     minY = cord.getY();
                 }
                 if (maxX < cord.getX()) {
                     maxX = cord.getX();
                 }
                 if (maxY < cord.getY()) {
                     maxY = cord.getY();
                 }
             }
 
         }
 
 
 
         return proj.latlon2eastNorth(new LatLon(
                 (maxY + minY) / 2d,
                 (maxX + minX) / 2d
             ));
     }
     /**
      * @return the modelRender
      */
     public ModelRender getModelRender() {
         return this.modelRender;
     }
     /**
      * @param modelRender the modelRender to set
      */
     public void setModelRender(ModelRender modelRender) {
         this.modelRender = modelRender;
     }
     /**
      * @return the layerList
      */
     public List<Layer> getLayerList() {
         return this.layerList;
     }
     /**
      * @param layerList the layerList to set
      */
     public void setLayerList(List<Layer> layerList) {
         this.layerList = layerList;
     }
 
     public void drawSelectable(GL2 gl) {
 
         gl.glColor3fv(Color.ORANGE.darker().getRGBComponents(new float[4]), 0);
 
         for (Layer layer : this.layerList ) {
             List<Model> models = layer.getModels();
             for (Model r : models) {
                 for (Selection s : r.getSelection()) {
                    // Double intersect = Ray3dUtil.intersect(selectRay, s.getCenter(), s.getRadius());
 
                     gl.glPushMatrix();
 
 
                     Point3d p = s.getCenter();
 
                     double dx = p.x;
                     double dy = p.y;
                     double dz = p.z;
 
                     gl.glLineWidth(1);
                     gl.glTranslated(dx, dy, dz);
 
                     DrawUtil.drawDotOuterY(gl, s.getRadius(), 24);
 
                     gl.glRotated(90d, 1d, 0, 0);
                     DrawUtil.drawDotOuterY(gl, s.getRadius(), 24);
 
 
                     gl.glRotated(90d, 0, 0, 1d);
                     DrawUtil.drawDotOuterY(gl, s.getRadius(), 24);
 
                     gl.glPopMatrix();
                 }
             }
         }
 
     }
 
     public void select(Ray3d selectRay) {
         Selection selection = null;
         double min = Double.MAX_VALUE;
 
         for (Layer layer : this.layerList ) {
             List<Model> models = layer.getModels();
             for (Model r : models) {
                 for (Selection s : r.getSelection()) {
                     Double intersect = Ray3dUtil.intersect(selectRay, s.getCenter(), s.getRadius());
                     if (intersect ==null) {
                         continue;
                     }
                     if (intersect < min) {
                         selection = s;
                         min = intersect;
                     }
                 }
                // drawModel(r, gl, camera);
             }
         }
 
         Selection last = this.selection;
 
         if (selection != null) {
             log.info("selected object: " + selection);
 
             if (last != selection) {
                 selection.select(true);
                 if (last != null) {
                     last.select(false);
                 }
             }
 
         } else {
             log.info("can't find selection");
 
             if (last != null) {
                 last.select(false);
             }
         }
 
         this.selection = selection;
     }
 
     public Editor selectActiveEditor(Ray3d selectRay) {
         Selection selection = this.selection;
         if (selection == null) {
             return null;
         }
         List<Editor> editors = selection.getEditors();
 
         Editor selectedEditor = null;
         double min = Double.MAX_VALUE;
 
 
         for (Editor e : editors) {
             if (e instanceof ArrowEditor) {
                 ArrowEditor ae = (ArrowEditor) e;
 
                 Double intersect = Ray3dUtil.intersect(selectRay, ae.getPoint(), 0.3);
                 if (intersect ==null) {
                     continue;
                 }
                 if (intersect < min) {
                     selectedEditor = e;
                     min = intersect;
                 }
             }
         }
 
         return selectedEditor;
     }
 
 
 
 
 }
