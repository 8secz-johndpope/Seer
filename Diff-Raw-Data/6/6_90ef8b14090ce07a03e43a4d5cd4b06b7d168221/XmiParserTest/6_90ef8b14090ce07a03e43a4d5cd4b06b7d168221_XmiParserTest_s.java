 package org.pentaho.metadata;
 
 import java.io.ByteArrayInputStream;
 import java.io.FileInputStream;
 import java.io.StringReader;
 import java.util.HashMap;
 import java.util.Set;
 import java.util.TreeSet;
 
 import org.junit.Assert;
 import org.junit.Test;
 import org.openide.util.io.ReaderInputStream;
 import org.pentaho.di.core.database.DatabaseMeta;
 import org.pentaho.metadata.model.Domain;
 import org.pentaho.metadata.model.SqlDataSource;
 import org.pentaho.metadata.model.SqlPhysicalModel;
 import org.pentaho.metadata.util.SerializationService;
 import org.pentaho.metadata.util.ThinModelConverter;
 import org.pentaho.metadata.util.XmiParser;
 
 import com.thoughtworks.xstream.XStream;
 import com.thoughtworks.xstream.converters.Converter;
 import com.thoughtworks.xstream.converters.MarshallingContext;
 import com.thoughtworks.xstream.converters.UnmarshallingContext;
 import com.thoughtworks.xstream.io.HierarchicalStreamReader;
 import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
 import com.thoughtworks.xstream.io.xml.DomDriver;
 
 @SuppressWarnings("nls")
 public class XmiParserTest {
   
   @Test
   public void testXmiParser() throws Exception {
     Domain domain = new XmiParser().parseXmi(new FileInputStream("samples/steelwheels.xmi"));
     Assert.assertEquals(6, domain.getConcepts().size());
     Assert.assertEquals(1, domain.getPhysicalModels().size());
     Assert.assertEquals(3, domain.getLogicalModels().size());
     
     Assert.assertEquals(2, domain.getLogicalModels().get(0).getLogicalTables().size());
     Assert.assertEquals(8, domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().size());
     Assert.assertEquals("BC_EMPLOYEES_JOBTITLE", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getId());
     Assert.assertEquals(1, domain.getLogicalModels().get(0).getLogicalRelationships().size());
     
     Assert.assertEquals("JOBTITLE", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getId());
     Assert.assertEquals("PT_EMPLOYEES", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getId());
     Assert.assertNotNull(domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getPhysicalModel());
     
     Assert.assertEquals(2, domain.getLogicalModels().get(0).getCategories().size());
     Assert.assertEquals(9, domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().size());
     Assert.assertEquals("BC_OFFICES_TERRITORY", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getId());
     Assert.assertEquals("TERRITORY", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getPhysicalColumn().getId());
     Assert.assertEquals("PT_OFFICES", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getId());
     
     // verify that inheritance is working
     Assert.assertEquals("$#,###.##", domain.findLogicalModel("BV_ORDERS").findCategory("CAT_ORDERS").findLogicalColumn("BC_ORDERDETAILS_TOTAL").getProperty("mask"));
     
     
   }
   
   @Test
   public void testXmiGenerator() throws Exception {
     // String str = new XmiParser().generateXmi(new Domain());
     // System.out.println(str);
     XmiParser parser = new XmiParser();
     Domain domain = parser.parseXmi(new FileInputStream("samples/steelwheels.xmi"));
     
     String xmi = parser.generateXmi(domain);
     
     ByteArrayInputStream is = new ByteArrayInputStream(xmi.getBytes());
     Domain domain2 = parser.parseXmi(is);
     
     SerializationService serializer = new SerializationService();
    
     String xml1 = serializeWithOrderedHashmaps(domain);
     String xml2 = serializeWithOrderedHashmaps(domain2);
 
     // note: this does not verify security objects at this time
     Assert.assertEquals(xml1, xml2);
   }
   
   public String serializeWithOrderedHashmaps(Domain domain) {
     XStream xstream = new XStream(new DomDriver());
     xstream.registerConverter(new Converter() {
 
       public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
         // TODO Auto-generated method stub
         writer.startNode("hashmap");
         HashMap unknownMap = (HashMap)source;
         if (unknownMap.size() > 0) {
           if (unknownMap.keySet().iterator().next() instanceof String) { 
             HashMap<String, Object> map = (HashMap<String, Object>)source;
             Set<String> ordered = new TreeSet<String>(map.keySet());
             for (String key : ordered) {
               writer.startNode("entry");
               writer.addAttribute("key", key);
               Object obj = map.get(key);
               if (obj == null) {
                 System.out.println("NULL OBJ FOR " + key);
               } else {
                 context.convertAnother(map.get(key));
               }
               writer.endNode();
             }
           }/* else {
             
             System.out.println(unknownMap.keySet().iterator().next());
           }*/
         }
         writer.endNode();
       }
 
       public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
         // TODO Auto-generated method stub
         return null;
       }
 
       public boolean canConvert(Class type) {
         return type.getName().equals("java.util.HashMap");
       }
       
     });
     return xstream.toXML(domain);
 
   }
   
   @Test
   public void testXmiLegacyConceptProperties() throws Exception {
     XmiParser parser = new XmiParser();
     Domain domain = parser.parseXmi(new FileInputStream("test-res/all_concept_properties.xmi"));
     Assert.assertEquals(2, domain.getConcepts().size());
     Assert.assertEquals(1, domain.getPhysicalModels().size());
     Assert.assertEquals(1, domain.getLogicalModels().size());
     
     Assert.assertEquals("http://localhost:8080/pentaho/ServiceAction", domain.getChildProperty("LEGACY_EVENT_SECURITY_SERVICE_URL"));
     
     Assert.assertEquals(1, domain.getLogicalModels().get(0).getLogicalTables().size());
     Assert.assertEquals(29, domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().size());
     Assert.assertEquals("BC_CUSTOMER_CUSTOMER_ID", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getId());
     Assert.assertEquals(0, domain.getLogicalModels().get(0).getLogicalRelationships().size());
     
     Assert.assertEquals("customer_id", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getId());
     Assert.assertEquals("PT_CUSTOMER", domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getId());
     Assert.assertNotNull(domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getPhysicalModel());
     
     Assert.assertEquals(1, domain.getLogicalModels().get(0).getCategories().size());
     Assert.assertEquals(29, domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().size());
     Assert.assertEquals("BC_CUSTOMER_FULLNAME", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getId());
     Assert.assertEquals("fullname", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getPhysicalColumn().getId());
     Assert.assertEquals("PT_CUSTOMER", domain.getLogicalModels().get(0).getCategories().get(0).getLogicalColumns().get(0).getPhysicalColumn().getPhysicalTable().getId());
     
     String xmi = parser.generateXmi(domain);
 
     Domain domain2 = parser.parseXmi(new ReaderInputStream(new StringReader(xmi)));
     SqlDataSource ds = ((SqlPhysicalModel)domain.getPhysicalModels().get(0)).getDatasource();
     SqlDataSource ds2 = ((SqlPhysicalModel)domain2.getPhysicalModels().get(0)).getDatasource();
 
     Assert.assertEquals("http://localhost:8080/pentaho/ServiceAction", domain2.getChildProperty("LEGACY_EVENT_SECURITY_SERVICE_URL"));
 
     
     Assert.assertEquals("foodmart", ds.getDatabaseName());
     Assert.assertEquals(ds.getDatabaseName(), ds2.getDatabaseName());
 
     Assert.assertEquals("MYSQL", ds.getDialectType());
     Assert.assertEquals(ds.getDialectType(), ds2.getDialectType());
 
     Assert.assertEquals("NATIVE", ds.getType().toString());
     Assert.assertEquals(ds.getType(), ds2.getType());
     
     Assert.assertEquals("localhost", ds.getHostname());
     Assert.assertEquals(ds.getHostname(), ds2.getHostname());
 
     Assert.assertEquals("3306", ds.getPort());
     Assert.assertEquals(ds.getPort(), ds2.getPort());
     
     Assert.assertEquals("foodmart", ds.getUsername());
     Assert.assertEquals(ds.getUsername(), ds2.getUsername());
 
     Assert.assertEquals("foodmart", ds.getPassword());
     Assert.assertEquals(ds.getPassword(), ds2.getPassword());
 
     Assert.assertEquals(9, ds.getAttributes().size());
     Assert.assertEquals(ds.getAttributes().size(), ds2.getAttributes().size());
     
     Assert.assertEquals("Y", ds2.getAttributes().get("QUOTE_ALL_FIELDS"));
     
     // test DatabaseMeta conversion
     DatabaseMeta meta = ThinModelConverter.convertToLegacy("test", ds);
 
     Assert.assertEquals("test", meta.getName());
     Assert.assertEquals("MYSQL", meta.getDatabaseTypeDesc());
     Assert.assertEquals("Native", meta.getAccessTypeDesc());
     Assert.assertEquals("localhost", meta.getHostname());
     Assert.assertEquals("3306", meta.getDatabasePortNumberString());
     Assert.assertEquals("foodmart", meta.getDatabaseName());
     Assert.assertEquals("foodmart", meta.getUsername());
     Assert.assertEquals("foodmart", meta.getPassword());
     Assert.assertTrue(meta.isQuoteAllFields());
     
     
     
     // Verify that RowLevelSecurity is in the xmi
     Assert.assertTrue(xmi.indexOf("&lt;row-level-security type=&quot;global&quot;&gt;&lt;formula&gt;&lt;![CDATA[TRUE()]]&gt;&lt;/formula&gt;&lt;entries&gt;&lt;/entries&gt;&lt;/row-level-security&gt;") >= 0);
     
     // Verify that the SqlDatasource is to and from successfully
   }
 
 }
