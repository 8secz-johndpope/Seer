 /*
  *    GeoTools - The Open Source Java GIS Toolkit
  *    http://geotools.org
  *
  *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
  *
  *    This library is free software; you can redistribute it and/or
  *    modify it under the terms of the GNU Lesser General Public
  *    License as published by the Free Software Foundation;
  *    version 2.1 of the License.
  *
  *    This library is distributed in the hope that it will be useful,
  *    but WITHOUT ANY WARRANTY; without even the implied warranty of
  *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *    Lesser General Public License for more details.
  */
 
 package org.geotools.data.complex;
 
 import java.io.IOException;
 import java.net.URL;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Map.Entry;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import junit.framework.TestCase;
 
 import org.geotools.data.DataAccess;
 import org.geotools.data.DataAccessFinder;
 import org.geotools.data.FeatureSource;
 import org.geotools.data.complex.config.ComplexDataStoreConfigurator;
 import org.geotools.data.complex.config.ComplexDataStoreDTO;
 import org.geotools.data.complex.config.EmfAppSchemaReader;
 import org.geotools.data.complex.config.XMLConfigDigester;
 import org.geotools.data.complex.filter.XPath.StepList;
 import org.geotools.feature.FeatureCollection;
 import org.geotools.feature.NameImpl;
 import org.geotools.feature.Types;
 import org.geotools.filter.FilterFactoryImplNamespaceAware;
 import org.geotools.xlink.XLINK;
 import org.opengis.feature.Attribute;
 import org.opengis.feature.ComplexAttribute;
 import org.opengis.feature.Feature;
 import org.opengis.feature.type.AttributeDescriptor;
 import org.opengis.feature.type.AttributeType;
 import org.opengis.feature.type.ComplexType;
 import org.opengis.feature.type.FeatureType;
 import org.opengis.feature.type.Name;
 import org.opengis.filter.Filter;
 import org.opengis.filter.FilterFactory2;
 import org.opengis.filter.expression.Expression;
 import org.opengis.filter.expression.Function;
 import org.opengis.filter.expression.Literal;
 import org.opengis.filter.expression.PropertyName;
 import org.xml.sax.helpers.NamespaceSupport;
 
 /**
  * DOCUMENT ME!
  * 
  * @author Gabriel Roldan, Axios Engineering
  * @version $Id$
  * @source $URL:
  *         http://svn.geotools.org/geotools/branches/2.4.x/modules/unsupported/community-schemas/community-schema-ds/src/test/java/org/geotools/data/complex/BoreholeTest.java $
  * @since 2.4
  */
 public class BoreholeTest extends TestCase {
     private static final Logger LOGGER = org.geotools.util.logging.Logging
             .getLogger(BoreholeTest.class.getPackage().getName());
 
     private static final String XMMLNS = "http://www.opengis.net/xmml";
 
     private static final String SANS = "http://www.seegrid.csiro.au/xml/sampling";
 
     private static final String OMNS = "http://www.opengis.net/om";
 
     private static final String SWENS = "http://www.opengis.net/swe/0.0";
 
     private static final String GMLNS = "http://www.opengis.net/gml";
 
     private static final String GEONS = "http://www.seegrid.csiro.au/xml/geometry";
 
     final String schemaBase = "/test-data/";
 
     final Name typeName = new NameImpl(XMMLNS, "Borehole");
 
     EmfAppSchemaReader reader;
 
     private FeatureSource source;
 
     /**
      * DOCUMENT ME!
      * 
      * @throws Exception
      *                 DOCUMENT ME!
      */
     protected void setUp() throws Exception {
         super.setUp();
         reader = EmfAppSchemaReader.newInstance();
         // Logging.GEOTOOLS.forceMonolineConsoleOutput(Level.FINEST);
     }
 
     /**
      * DOCUMENT ME!
      * 
      * @throws Exception
      *                 DOCUMENT ME!
      */
     protected void tearDown() throws Exception {
         super.tearDown();
     }
 
     /**
      * 
      * @param location
      *                schema location path discoverable through getClass().getResource()
      */
     private void loadSchema(String location) throws IOException {
         // load needed GML types directly from the gml schemas
         URL schemaLocation = getClass().getResource(location);
         assertNotNull(location, schemaLocation);
         reader.parse(schemaLocation);
     }
 
     /**
      * Tests if the schema-to-FM parsing code developed for complex datastore configuration loading
      * can parse the GeoSciML types
      * 
      * @throws Exception
      */
     public void testParseBoreholeSchema() throws Exception {
         /*
          * not found types and elements:
          */
 
         // load geosciml schema
         try {
             loadSchema(schemaBase + "commonSchemas/XMML/1/borehole.xsd");
         } catch (Exception e) {
             e.printStackTrace();
             throw e;
         }
 
         Map typeRegistry = reader.getTypeRegistry();
 
         Name typeName = Types.typeName(XMMLNS, "BoreholeType");
         ComplexType borehole = (ComplexType) typeRegistry.get(typeName);
         assertNotNull(borehole);
         assertTrue(borehole instanceof FeatureType);
 
         AttributeType superType = borehole.getSuper();
         assertNotNull(superType);
         Name superTypeName = Types.typeName(SANS, "ProfileType");
         assertEquals(superTypeName, superType.getName());
         assertTrue(superType instanceof FeatureType);
 
         // ensure all needed types were parsed and aren't just empty proxies
         Collection properties = borehole.getDescriptors();
         assertEquals(16, properties.size());
         Map expectedNamesAndTypes = new HashMap();
         // from gml:AbstractFeatureType
         expectedNamesAndTypes.put(name(GMLNS, "metaDataProperty"), typeName(GMLNS,
                 "MetaDataPropertyType"));
         expectedNamesAndTypes.put(name(GMLNS, "description"), typeName(GMLNS, "StringOrRefType"));
         expectedNamesAndTypes.put(name(GMLNS, "name"), typeName(GMLNS, "CodeType"));
         expectedNamesAndTypes.put(name(GMLNS, "boundedBy"), typeName(GMLNS, "BoundingShapeType"));
         expectedNamesAndTypes.put(name(GMLNS, "location"), typeName(GMLNS, "LocationPropertyType"));
         // from sa:ProfileType
         expectedNamesAndTypes.put(name(SANS, "begin"), typeName(GMLNS, "PointPropertyType"));
         expectedNamesAndTypes.put(name(SANS, "end"), typeName(GMLNS, "PointPropertyType"));
         expectedNamesAndTypes.put(name(SANS, "length"), typeName(SWENS, "RelativeMeasureType"));
         expectedNamesAndTypes.put(name(SANS, "shape"), typeName(GEONS, "Shape1DPropertyType"));
         // sa:SamplingFeatureType
         expectedNamesAndTypes.put(name(SANS, "member"), typeName(SANS,
                 "SamplingFeaturePropertyType"));
         expectedNamesAndTypes.put(name(SANS, "surveyDetails"), typeName(SANS,
                 "SurveyProcedurePropertyType"));
         expectedNamesAndTypes.put(name(SANS, "associatedSpecimen"), typeName(SANS,
                 "SpecimenPropertyType"));
         expectedNamesAndTypes.put(name(SANS, "relatedObservation"), typeName(OMNS,
                 "AbstractObservationPropertyType"));
         // from xmml:BoreholeType
         expectedNamesAndTypes.put(name(XMMLNS, "drillMethod"), typeName(XMMLNS, "drillCode"));
         expectedNamesAndTypes.put(name(XMMLNS, "collarDiameter"), typeName(GMLNS, "MeasureType"));
         expectedNamesAndTypes.put(name(XMMLNS, "log"), typeName(XMMLNS, "LogPropertyType"));
 
         for (Iterator it = expectedNamesAndTypes.entrySet().iterator(); it.hasNext();) {
             Map.Entry entry = (Entry) it.next();
             Name dName = (Name) entry.getKey();
             Name tName = (Name) entry.getValue();
 
             AttributeDescriptor d = (AttributeDescriptor) Types.descriptor(borehole, dName);
             assertNotNull("Descriptor not found: " + dName, d);
             AttributeType type;
             try {
                 type = (AttributeType) d.getType();
             } catch (Exception e) {
                 LOGGER.log(Level.SEVERE, "type not parsed for "
                         + ((AttributeDescriptor) d).getName(), e);
                 throw e;
             }
             assertNotNull(type);
             assertNotNull(type.getName());
             assertNotNull(type.getBinding());
             if (tName != null) {
                 assertEquals(tName, type.getName());
             }
         }
 
         Name tcl = Types.typeName(SWENS, "TypedCategoryListType");
         AttributeType typedCategoryListType = (AttributeType) typeRegistry.get(tcl);
         assertNotNull(typedCategoryListType);
         assertFalse(typedCategoryListType instanceof ComplexType);
     }
 
     private Name typeName(String ns, String localName) {
         return Types.typeName(ns, localName);
     }
 
     private Object name(String ns, String localName) {
         return new NameImpl(ns, localName);
     }
 
     public void testLoadMappingsConfig() throws Exception {
         XMLConfigDigester reader = new XMLConfigDigester();
         URL url = getClass().getResource(schemaBase + "BoreholeTest_properties.xml");
 
         ComplexDataStoreDTO config = reader.parse(url);
 
         Set mappings = ComplexDataStoreConfigurator.buildMappings(config);
 
         assertNotNull(mappings);
         assertEquals(1, mappings.size());
 
         FeatureTypeMapping mapping = (FeatureTypeMapping) mappings.iterator().next();
 
         AttributeDescriptor targetFeature = mapping.getTargetFeature();
         assertNotNull(targetFeature);
         assertNotNull(targetFeature.getType());
         assertEquals(XMMLNS, targetFeature.getName().getNamespaceURI());
         assertEquals("Borehole", targetFeature.getName().getLocalPart());
 
         source = mapping.getSource();
         assertNotNull(source);
         FeatureType schema = source.getSchema();
         String typeName = schema.getName().toString();
         assertEquals("test-data:boreholes_denormalized", typeName);
 
         List attributeMappings = mapping.getAttributeMappings();
         assertEquals(24, attributeMappings.size());
 
         AttributeMapping attMapping = (AttributeMapping) attributeMappings.get(0);
         assertNotNull(attMapping);
         StepList targetXPath = attMapping.getTargetXPath();
         assertNotNull(targetXPath);
         assertEquals("xmml:Borehole", targetXPath.toString());
 
         Expression idExpression = attMapping.getIdentifierExpression();
         assertNotNull(idExpression);
         assertTrue(idExpression instanceof Function);
         Function idFunction = (Function) idExpression;
         assertEquals("strConcat", idFunction.getName());
         assertTrue(idFunction.getParameters().get(0) instanceof Literal);
         assertTrue(idFunction.getParameters().get(1) instanceof PropertyName);
 
         assertEquals(Expression.NIL, attMapping.getSourceExpression());
     }
 
     public void testGetDataStore() throws Exception {
         DataAccess<FeatureType, Feature> mappingDataStore = getDataStore();
         assertNotNull(mappingDataStore);
         assertNotNull(mappingDataStore.getSchema(typeName));
     }
 
     public void testDataStore() throws Exception {
         DataAccess<FeatureType, Feature> mappingDataStore = getDataStore();
         FeatureSource<FeatureType, Feature> fSource = (FeatureSource<FeatureType, Feature>) mappingDataStore
                 .getFeatureSource(typeName);
 
         // make a getFeatures request with a nested properties filter.
         // note that the expected result count is set to 65 since that's the
         // number
         // of results I get from a sql select on min_time_d = 'carnian'
         final int EXPECTED_RESULT_COUNT = 20;
 
         FeatureCollection<FeatureType, Feature> features = (FeatureCollection<FeatureType, Feature>) fSource
                 .getFeatures();
 
         int resultCount = getCount(features);
         String msg = "be sure difference in result count is not due to different dataset."
                 + " Query used should be min_time_d = 'carnian'";
         assertEquals(msg, EXPECTED_RESULT_COUNT, resultCount);
 
         Feature feature;
         int count = 0;
         Iterator it = features.iterator();
         for (; it.hasNext();) {
             feature = (Feature) it.next();
             count++;
         }
         features.close(it);
         assertEquals(EXPECTED_RESULT_COUNT, count);
     }
 
     public void testQueryXlinkProperty() throws Exception {
         final DataAccess<FeatureType, Feature> mappingDataStore = getDataStore();
         final FeatureSource<FeatureType, Feature> fSource = (FeatureSource<FeatureType, Feature>) mappingDataStore
                 .getFeatureSource(typeName);
         final String queryProperty = "sa:shape/geo:LineByVector/geo:origin/@xlink:href";
        final String queryLiteral = "#bh.176909a.start";
 
         NamespaceSupport namespaces = new NamespaceSupport();
         namespaces.declarePrefix("sa", SANS);
         namespaces.declarePrefix("geo", GEONS);
         namespaces.declarePrefix("xlink", XLINK.NAMESPACE);
 
         final FilterFactory2 ff = new FilterFactoryImplNamespaceAware(namespaces);
         final PropertyName propertyName = ff.property(queryProperty);
         final Literal literal = ff.literal(queryLiteral);
 
         final Filter filter = ff.equals(propertyName, literal);
 
         FeatureCollection<FeatureType, Feature> features = (FeatureCollection<FeatureType, Feature>) fSource
                 .getFeatures(filter);
 
         // did the query work?
         int resultCount = getCount(features);
        assertEquals(1, resultCount);
 
         // the datastore performed the query by unmapping the client property
         // to its corresponding source expression, as defined in the AttributeMapping
         // clientProperties.
         // Now the Filter is able to evaluate the property name?
 
         // TODO: not sure why AttributePropertyHandler is not catching up at expression
         // evaluation
         // Feature feature = (Feature)features.iterator().next();
         // String obtainedValue = (String) propertyName.evaluate(feature);
         // assertNotNull(obtainedValue);
         // assertEquals(queryLiteral, obtainedValue);
     }
 
     /**
      * Grab a feature and traverse it in deep as an encoder might do
      * 
      * @throws Exception
      */
     public void testTraverseDeep() throws Exception {
         final DataAccess<FeatureType, Feature> mappingDataStore = getDataStore();
         final FeatureSource<FeatureType, Feature> fSource = (FeatureSource<FeatureType, Feature>) mappingDataStore
                 .getFeatureSource(typeName);
         final String queryProperty = "sa:shape/geo:LineByVector/geo:origin/@xlink:href";
        final String queryLiteral = "#bh.176909a.start";
 
         NamespaceSupport namespaces = new NamespaceSupport();
         namespaces.declarePrefix("sa", SANS);
         namespaces.declarePrefix("geo", GEONS);
         namespaces.declarePrefix("xlink", XLINK.NAMESPACE);
 
         FeatureCollection<FeatureType, Feature> features = (FeatureCollection) fSource
                 .getFeatures();
         Feature f = (Feature) features.iterator().next();
         traverse(f);
     }
 
     private void traverse(Attribute f) {
         Object value;
         value = f.getValue();
         if (f instanceof ComplexAttribute) {
             Collection values = (Collection) value;
             for (Iterator it = values.iterator(); it.hasNext();) {
                 Attribute att = (Attribute) it.next();
                 assertNotNull(att);
                 traverse(att);
             }
         }
     }
 
     private DataAccess<FeatureType, Feature> getDataStore() throws IOException {
         final Map dsParams = new HashMap();
         final URL url = getClass().getResource(schemaBase + "BoreholeTest_properties.xml");
         dsParams.put("dbtype", "complex");
         dsParams.put("url", url.toExternalForm());
 
         DataAccess<FeatureType, Feature> mappingDataStore = DataAccessFinder.getDataStore(dsParams);
         assertNotNull(mappingDataStore);
         return mappingDataStore;
     }
 
     private int getCount(FeatureCollection features) {
         Iterator iterator = features.iterator();
         int count = 0;
         try {
             while (iterator.hasNext()) {
                 iterator.next();
                 count++;
             }
         } finally {
             features.close(iterator);
         }
         return count;
     }
 
 }
