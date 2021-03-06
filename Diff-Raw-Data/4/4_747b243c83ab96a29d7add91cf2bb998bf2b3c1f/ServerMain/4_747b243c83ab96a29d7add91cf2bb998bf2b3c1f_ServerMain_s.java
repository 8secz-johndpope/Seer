 /**
  * 
  */
 package pxchat.server;
 
 import java.io.File;
 import java.util.HashMap;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.ErrorHandler;
 import org.xml.sax.SAXException;
 import org.xml.sax.SAXParseException;
 
 import pxchat.util.XMLUtil;
 
 /**
  * @author markus
  *
  */
 public class ServerMain {
 	
 	private static HashMap<String, String> authList = new HashMap<String, String>();
 
 	/**
 	 * @param args
 	 */
 	public static void main(String[] args) {
 		System.out.println("Started pxchat server...");
 
 		File file = new File("xml/server.xml");
 		Document doc = null;
 		try {
 			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
 			factory.setValidating(true);
 			DocumentBuilder builder = factory.newDocumentBuilder();
 
 			builder.setErrorHandler(new ErrorHandler() {
 				
 				@Override
 				public void warning(SAXParseException e) throws SAXException {
 					System.out.println("Warning validating the config file:");
 					e.printStackTrace();
 				}
 				
 				@Override
 				public void fatalError(SAXParseException e) throws SAXException {
 					System.out.println("Fatal error validating the config file:");
 					e.printStackTrace();
 					System.exit(0);
 				}
 				
 				@Override
				public void error(SAXParseException arg0) throws SAXException {
					throw new RuntimeException("oO");
 				}
 			});
 			doc = builder.parse(file);
 			Node node = doc.getDocumentElement();
 
 			Node config = XMLUtil.getChildByName(node, "config");
 			
 			int port = Integer.valueOf(
 					XMLUtil.getAttributeValue(XMLUtil.getChildByName(config, "port"), "number"));
 			
 			System.out.println(port);
 			
 			Node auth = XMLUtil.getChildByName(node, "auth");
 			
 			NodeList list = auth.getChildNodes();
 			if (list != null) {
 				for (int i = 0; i < list.getLength(); i++) {
 					if (list.item(i).getNodeName().equals("user")) {
 						authList.put(XMLUtil.getAttributeValue(list.item(i), "name"),
 								XMLUtil.getAttributeValue(list.item(i), "password"));
 					}
 				}
 			}
 			
 			System.out.println(authList);
 
 		} catch (Exception e) {
 			System.out.println("An error ocurred loading the config file");
 			e.printStackTrace();
 			return;
 		}
 	}
 
 }
