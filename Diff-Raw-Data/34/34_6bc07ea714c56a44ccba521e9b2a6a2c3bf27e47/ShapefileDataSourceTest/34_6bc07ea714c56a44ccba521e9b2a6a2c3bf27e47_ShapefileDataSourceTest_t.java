 /*
  * ShapefileDataSourceTest.java
  * JUnit based test
  *
  * Created on March 4, 2002, 4:00 PM
  */
 
 package org.geotools.data.shapefile;
 
 import org.geotools.data.*;
 import org.geotools.feature.*;
 import com.vividsolutions.jts.geom.*;
 import java.io.*;
 import java.net.*;
 import java.util.*;
 
 import junit.framework.*;
 import org.geotools.data.shapefile.*;
 
 /**
  * @author Ian Schneider
  * @author jamesm
  */
 public class ShapefileDataSourceTest extends TestCaseSupport {
   
   final static String STATE_POP = "statepop.shp";
   final static String STREAM = "stream.shp";
   
   public ShapefileDataSourceTest(java.lang.String testName) {
     super(testName);
   }
   
   public static void main(java.lang.String[] args) throws Exception {
     junit.textui.TestRunner.run(suite(ShapefileDataSourceTest.class));
   }
   
   private FeatureCollection loadFeatures(String resource,Query q) throws Exception {
     if (q == null) q = new DefaultQuery();
     URL url = getTestResource(resource);
     ShapefileDataSource s = new ShapefileDataSource(url);
     return s.getFeatures(q);
   }
   
   
   public void testLoad() throws Exception {
     loadFeatures(STATE_POP,null);
   }
   
   public void testSchema() throws Exception {
     URL url = getTestResource(STATE_POP);
     ShapefileDataSource s = new ShapefileDataSource(url);
     FeatureType schema = s.getSchema();
     AttributeType[] types = schema.getAttributeTypes();
     assertEquals("Number of Attributes",253,types.length);
   }
   
   public void testEnvelope() throws Exception {
     FeatureCollection features = loadFeatures(STATE_POP, null);
     ShapefileDataSource s = new ShapefileDataSource(getTestResource(STATE_POP));
     assertEquals(features.getBounds(), s.getBounds());
   }
   
   public void testLoadAndVerify() throws Exception {
     FeatureCollection features = loadFeatures(STATE_POP,null);
     
     assertEquals("Number of Features loaded",49,features.size());
     
     FeatureType schema = firstFeature(features).getFeatureType();
     assertNotNull(schema.getDefaultGeometry());
     assertEquals("Number of Attributes",253,schema.getAttributeTypes().length);
     assertEquals("Value of statename is wrong",firstFeature(features).getAttribute("STATE_NAME"),"Illinois");
     assertEquals("Value of land area is wrong",((Double)firstFeature(features).getAttribute("LAND_KM")).doubleValue(),143986.61,0.001);
   }
   
   public void testMetaData() throws Exception {
     ShapefileDataSource s = new ShapefileDataSource(getTestResource(STATE_POP));
     DataSourceMetaData meta = s.getMetaData();
     assertTrue(meta.hasFastBbox());
     assertTrue(meta.supportsSetFeatures());
   }
   
   public void testQuerySubset() throws Exception {
     DefaultQuery qi = new DefaultQuery();
     qi.setPropertyNames(new String[] {"STATE_NAME"});
     FeatureCollection features = loadFeatures(STATE_POP,qi);
     
     assertEquals("Number of Features loaded",49,features.size());
     FeatureType schema = firstFeature(features).getFeatureType();
     
     assertEquals("Number of Attributes",1,schema.getAttributeTypes().length);
   }
   
   public void testQuerying() throws Exception {
     URL url = getTestResource(STREAM);
     ShapefileDataSource s = new ShapefileDataSource(url);
     FeatureType schema = s.getSchema();
     AttributeType[] types = schema.getAttributeTypes();
     for (int i = 0, ii = types.length; i < ii; i++) {
       DefaultQuery q = new DefaultQuery();
       q.setPropertyNames(new String[] {types[i].getName()});
       FeatureCollection fc = s.getFeatures(q);
       assertEquals("Number of Features",280,fc.size());
       assertEquals("Number of Attributes",1,firstFeature(fc).getNumberOfAttributes());
       FeatureType type = firstFeature(fc).getFeatureType();
       assertEquals("Attribute Name",type.getAttributeType(0).getName(),types[i].getName());
       assertEquals("Attribute Type",type.getAttributeType(0).getType(),types[i].getType());
       if (i % 5 == 0) System.out.print(".");
     }
   }
   
   public void testAttributesWriting() throws Exception {
     FeatureTypeFactory factory = FeatureTypeFactory.newInstance("junk");
     factory.addType(AttributeTypeFactory.newAttributeType("a",Byte.class));
     factory.addType(AttributeTypeFactory.newAttributeType("b",Short.class));
     factory.addType(AttributeTypeFactory.newAttributeType("c",Double.class));
     factory.addType(AttributeTypeFactory.newAttributeType("d",Float.class));
     factory.addType(AttributeTypeFactory.newAttributeType("e",String.class));
     factory.addType(AttributeTypeFactory.newAttributeType("f",Date.class));
     factory.addType(AttributeTypeFactory.newAttributeType("g",Boolean.class));
     factory.addType(AttributeTypeFactory.newAttributeType("h",Geometry.class));
     factory.addType(AttributeTypeFactory.newAttributeType("i",Number.class));
     FeatureType type = factory.getFeatureType();
     FeatureCollection features = FeatureCollections.newCollection();
     for (int i = 0, ii = 20; i < ii; i++) {
       features.add(type.create(new Object[] {
         new Byte( (byte) i ),
         new Short( (short) i),
         new Double( i ),
         new Float( i ),
         new String( i + " " ),
         new Date( i ),
         new Boolean( true ),
         new Point(new Coordinate(0,0), new PrecisionModel(),0),
         new Integer(22)
       }));
     }
     
     URL parent = getTestResource("");
     File data = new File(parent.getFile());
     if (!data.exists())
       throw new Exception("Couldn't setup temp file");
     File tmpFile = new File(data, "tmp.shp");
     tmpFile.createNewFile();
     
     ShapefileDataSource s = new ShapefileDataSource(tmpFile.toURL());
     s.setFeatures(features);
     
     tmpFile.delete();
   }
   
   public void testGeometriesWriting() throws Exception {
     
     
     String[] wktResources = new String[] {
       "point",
       "multipoint",
       "line",
       "multiline",
       "polygon",
       "multipolygon"
     };
     
     PrecisionModel pm = new PrecisionModel();
     for (int i = 0; i < wktResources.length; i++) {
       Geometry geom = readGeometry(wktResources[i]);
       String testName = wktResources[i];
       try {
         
        runWriteReadTest(geom,false);
         make3D(geom);
         testName += "3d";
        runWriteReadTest(geom,true);
       } catch (Throwable e) {
         throw new Exception("Error in " + testName,e); 
       }
       
     }
     
   }
   
   private void make3D(Geometry g) {
     Coordinate[] c = g.getCoordinates();
     for (int i = 0, ii = c.length; i < ii; i++) {
       c[i].z = 42 + i;
     }
   }
   
  private void runWriteReadTest(Geometry geom,boolean d3) throws Exception {
     // make features
     FeatureTypeFactory factory = FeatureTypeFactory.newInstance("junk");
     factory.addType(AttributeTypeFactory.newAttributeType("a",Geometry.class));
     FeatureType type = factory.getFeatureType();
     FeatureCollection features = FeatureCollections.newCollection();
     for (int i = 0, ii = 20; i < ii; i++) {
       
       features.add(type.create(new Object[] {
         geom.clone()
       }));
     }
     
     // set up file
     File tmpFile = getTempFile();
     
     // write features
     ShapefileDataSource s = new ShapefileDataSource(tmpFile.toURL());
     s.setFeatures(features);
     
     // read features
     s = new ShapefileDataSource(tmpFile.toURL());
     FeatureCollection fc = s.getFeatures();
     FeatureIterator fci = fc.features();
     
     // verify
     while (fci.hasNext()) {
       Feature f = fci.next();
       Geometry fromShape = f.getDefaultGeometry();
       
       if (fromShape instanceof GeometryCollection) {
         if ( ! (geom instanceof GeometryCollection) ) {
           fromShape = ((GeometryCollection)fromShape).getGeometryN(0);
         }
       }
       try {
         Coordinate[] c1 = geom.getCoordinates();
         Coordinate[] c2 = fromShape.getCoordinates();
         for (int cc = 0, ccc = c1.length; cc < ccc; cc++) {
            //System.out.println(c1[cc] + "," + c2[cc]);
          if (d3)
            assertTrue(c1[cc].equals3D(c2[cc]));
          else
            assertTrue(c1[cc].equals2D(c2[cc]));
         }
       } catch (Throwable t) {
         fail("Bogus : " + Arrays.asList(geom.getCoordinates()) + " : " + Arrays.asList(fromShape.getCoordinates()));
       }
       
       
     }
     
     tmpFile.delete();
   }
   
 }
