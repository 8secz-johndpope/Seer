 /*
  * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
  * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
  * 
  * For more information on the project, please refer to the this web site:
  * http://www.esdi-humboldt.eu
  * 
  * LICENSE: For information on the license under which this program is 
  * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
  * (c) the HUMBOLDT Consortium, 2007 to 2010.
  */
 package eu.esdihumboldt.cst.iobridge;
 
 import java.lang.reflect.Method;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.Map;
 
 import org.apache.commons.logging.impl.Log4JLogger;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.geotools.feature.FeatureCollection;
 import org.geotools.feature.FeatureCollections;
 import org.geotools.gml3.GMLConfiguration;
 import org.geotools.util.logging.Log4JLoggerFactory;
 import org.geotools.util.logging.Logging;
 import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.opengis.feature.simple.SimpleFeature;
 import org.opengis.feature.simple.SimpleFeatureType;
 
 import eu.esdihumboldt.cst.CstFunction;
 import eu.esdihumboldt.cst.iobridge.IoBridgeFactory.BridgeType;
 import eu.esdihumboldt.cst.iobridge.impl.DefaultCstServiceBridge;
 import eu.esdihumboldt.cst.transformer.service.CstFunctionFactory;
 import eu.esdihumboldt.hale.gmlparser.HaleGMLParser;
 
 /**
  * This class contains tests that test the integration of the different 
  * CST components.
  * 
  * @author Thorsten Reitz
  * @version $Id$
  */
 public class IoBridgeIntegrationTest {
 
 	private static Logger _log = Logger.getLogger(IoBridgeIntegrationTest.class);
 		
 	
 
 	@BeforeClass 
 	public static void initialize(){
 		addCST();
 		// configure logging
 		Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
 		Logger.getLogger(Log4JLogger.class).setLevel(Level.WARN);
 		Logger.getRootLogger().setLevel(Level.WARN);
 	}
 	
 	
 	//@Test
 	public void testCstGetRegisteredTransfomers(){
 		CstFunctionFactory tf = CstFunctionFactory.getInstance();
 		tf.registerCstPackage("eu.esdihumboldt.cst.corefunctions");
 		Map<String, Class<? extends CstFunction>> functions = tf
 				.getRegisteredFunctions();
 		//functions.clear();
 		functions = tf.getRegisteredFunctions();
 		Assert.assertTrue(functions.size() > 2);
 	}
 		
 	//@Test
 	public void testParser() throws Exception{
 		FeatureCollection<SimpleFeatureType, SimpleFeature>  fc = FeatureCollections.newCollection();
 		
 		URL url = new URL("file://"
 				+ getClass()
 						.getResource("./test_gs.xml")
 						.getFile());		
 		HaleGMLParser parser = new HaleGMLParser(new GMLConfiguration());
 		
 		
 		fc = (FeatureCollection<SimpleFeatureType, SimpleFeature>) parser
 				.parse(url.openStream());
 		Assert.assertNotNull(fc);
 		Assert.assertNotNull(fc.features().next().toString());
 	}
 	
 	/*@Test 
 	public void testGenerator(){
 		Assert.assertTrue(true);
 	}*/
 	//@Test
 	public void testTransform() {	
 		CstFunctionFactory tf = CstFunctionFactory.getInstance();
 		tf.registerCstPackage("eu.esdihumboldt.cst.corefunctions");
 		try {
 	
 			URL omlURL = IoBridgeIntegrationTest.class.getResource("test.oml");
 			URL gmlURL = IoBridgeIntegrationTest.class.getResource("test_gs.xml");			
 			URL xsd =  IoBridgeIntegrationTest.class.getResource("test_gs_target.xsd");		
 			String out = IoBridgeIntegrationTest.class.getResource("").toExternalForm() + "out.gml";		
 			DefaultCstServiceBridge csb = (DefaultCstServiceBridge)IoBridgeFactory.getIoBridge(BridgeType.preloaded);
 			System.out.println(xsd.toURI().toString());
 			System.out.println(omlURL.toURI().toString());
 			System.out.println(gmlURL.toURI().toString());
 			
 			
 			String result = csb.transform(
 					xsd.toURI().toString(),
 					omlURL.toURI().toString(), 
 			        gmlURL.toURI().toString(),
					out, null, null);
 			
 			
 			System.out.println(result);
 			
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	}
 	//@Test
 	public void testTransformItaly_INSPIRE() {	
 		CstFunctionFactory tf = CstFunctionFactory.getInstance();
 		tf.registerCstPackage("eu.esdihumboldt.cst.corefunctions");
 		try {
 	
 			URL omlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_INSPIRE/HALE_CST_Italy_INSPIRE.xml.goml");
 			URL gmlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_INSPIRE/SIC/SIC.gml");			
 			URL xsd =  IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_INSPIRE/models/ProtectedSitesFull.xsd");		
 			String out = "src/test/resource/SIC_generated.gml";		
 			DefaultCstServiceBridge csb = (DefaultCstServiceBridge)IoBridgeFactory.getIoBridge(BridgeType.preloaded);
 			System.out.println(xsd.toURI().toString());
 			System.out.println(omlURL.toURI().toString());
 			System.out.println(gmlURL.toURI().toString());
 			
 			
 			String result = csb.transform(
 					xsd.toURI().toString(),
 					omlURL.toURI().toString(), 
 			        gmlURL.toURI().toString(),
					out, null, null);
 			
 			
 			System.out.println(result);
 			
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	}
 	@Test
 	public void testTransformItaly_TC() {	
 		CstFunctionFactory tf = CstFunctionFactory.getInstance();
 		tf.registerCstPackage("eu.esdihumboldt.cst.corefunctions");
 		try {
 	
 			URL omlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_TC/HALE_CST_Italy_TC.xml.goml");
 			URL gmlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_TC/lakes/SPECCHI_ACQUA_07.gml");			
 			URL xsd =  IoBridgeIntegrationTest.class.getResource("./HALE_CST_Italy_TC/models/TCS_final_mdv.xsd");		
 			String out = "src/test/resource/SPECCHI_ACQUA_07_generated.gml";
 			//String out = IoBridgeIntegrationTest.class.getResource("").toExternalForm() + "HALE_CST_Italy_TC/lakes/SPECCHI_ACQUA_07_generated.gml";		
 			DefaultCstServiceBridge csb = (DefaultCstServiceBridge)IoBridgeFactory.getIoBridge(BridgeType.preloaded);
 			System.out.println(xsd.toURI().toString());
 			System.out.println(omlURL.toURI().toString());
 			System.out.println(gmlURL.toURI().toString());
 			
 			
 			String result = csb.transform(
 					xsd.toURI().toString(),
 					omlURL.toURI().toString(), 
 			        gmlURL.toURI().toString(),
					out, null, null);
 			
 			
 			System.out.println(result);
 			
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	}
 	//@Test
 	public void testTransformSpain_HUMBOLDT() {	
 		CstFunctionFactory tf = CstFunctionFactory.getInstance();
 		tf.registerCstPackage("eu.esdihumboldt.cst.corefunctions");
 		try {
 	
 			URL omlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Spain_HUMBOLDT/HALE_CST_Spain_HUMBOLDT.xml.goml");
 			URL gmlURL = IoBridgeIntegrationTest.class.getResource("./HALE_CST_Spain_HUMBOLDT/data/ren_ex.gml");			
 			URL xsd =  IoBridgeIntegrationTest.class.getResource("./HALE_CST_Spain_HUMBOLDT/models/pa_simple_corr.xsd");		
 			String out = "src/test/resource/ren_ex_generated.gml";		
 			DefaultCstServiceBridge csb = (DefaultCstServiceBridge)IoBridgeFactory.getIoBridge(BridgeType.preloaded);
 			System.out.println(xsd.toURI().toString());
 			System.out.println(omlURL.toURI().toString());
 			System.out.println(gmlURL.toURI().toString());
 			
 			
 			String result = csb.transform(
 					xsd.toURI().toString(),
 					omlURL.toURI().toString(), 
 			        gmlURL.toURI().toString(),
					out, null, null);
 			
 			
 			System.out.println(result);
 			
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	}
 	public static void addCST() {
 		Class<?>[] parameters = new Class[]{URL.class};
		URL functions = (new IoBridgeIntegrationTest()).getClass().getResource("corefunctions-1.0.3-SNAPSHOT.jar");		
 		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
 	      Class<?> sysclass = URLClassLoader.class;
 
 	      try {
 	         Method method = sysclass.getDeclaredMethod("addURL", parameters);
 	         method.setAccessible(true);
 	         method.invoke(sysloader, new Object[]{functions});
 	      } catch (Throwable t) {
 	         t.printStackTrace();
 	         //throw new IOException("Error, could not add URL to system classloader");
 	      }
 
 	}
 }
