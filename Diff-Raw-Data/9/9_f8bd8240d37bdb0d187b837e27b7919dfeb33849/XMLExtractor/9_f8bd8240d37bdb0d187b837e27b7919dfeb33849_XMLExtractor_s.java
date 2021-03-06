 package org.atl.engine.extractors.xml;
 
 import org.atl.engine.extractors.Extractor;
 import org.atl.engine.vm.nativelib.ASMCollection;
 import org.atl.engine.vm.nativelib.ASMModel;
 import org.atl.engine.vm.nativelib.ASMModelElement;
 import org.atl.engine.vm.nativelib.ASMString;
 
 import java.io.BufferedOutputStream;
 import java.io.OutputStream;
 import java.io.PrintStream;
 
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * @author Frdric Jouault
  */
 public class XMLExtractor implements Extractor {
 
 	/* New Extractor Interface. */
 	
 	private static Map parameterTypes = Collections.EMPTY_MAP;
 
 	public Map getParameterTypes() {
 		return parameterTypes;
 	}
 
 	public void extract(ASMModel source, OutputStream target, Map params) {
 		extract(null, source, target);
 	}
 
 	/* Old Extractor Interface. */
 	
 	public String getPrefix() {
 		return "xml";
 	}
 
 	public void extract(ASMModel format, ASMModel extent, OutputStream out) {
 		PrintStream out2 = new PrintStream(new BufferedOutputStream(out));
 		out2.println("<?xml version = '1.0' encoding = 'ISO-8859-1' ?>");
 
 		write((ASMModelElement)extent.getElementsByType("Root").iterator().next(), extent, out2, "");
 
 		out2.close();
 	}
 
 	/* Serializer. */
 	
 	private void write(ASMModelElement o, ASMModel extent, PrintStream out, String indent) {
 		String oTypeName = getString(o.getMetaobject(), "name");
 		if(oTypeName.equals("Element") || oTypeName.equals("Root")) {
 			String name = getString(o, "name");
 			out.print(indent + "<" + name);
 
 			ASMCollection children = (ASMCollection)o.get(null, "children");
 			boolean hasElements = false;
 			boolean hasTexts = false;
 			for(Iterator i = children.iterator() ; i.hasNext() ; ) {
 				ASMModelElement c = (ASMModelElement)i.next();
 				String typeName = getString(c.getMetaobject(), "name");
 				if(typeName.equals("Attribute")) {
 					out.print(" " + getString(c, "name") + " = \'" + convertText(getString(c, "value"), true) + "\'");
 				} else if(typeName.equals("Element")) {
 					hasElements = true;
 				} else if(typeName.equals("Text")) {
 					hasTexts = true;
 				}
 			}
 			if(hasElements) {
 				out.println(">");
 
 				for(Iterator i = children.iterator() ; i.hasNext() ; ) {
 					ASMModelElement c = (ASMModelElement)i.next();
 					String typeName = getString(c.getMetaobject(), "name");
 					if(typeName.equals("Element")) {
 						write(c, extent, out, indent + "  ");
 					} else if(typeName.equals("Text")) {
 						out.print(convertText(getString(c, "value"), false));
 					}
 				}
 
 				out.println(indent + "</" + name + ">");
 			} else if(hasTexts) {
 				out.print(">");
 
 				for(Iterator i = children.iterator() ; i.hasNext() ; ) {
 					ASMModelElement c = (ASMModelElement)i.next();
 					String typeName = getString(c.getMetaobject(), "name");
 					if(typeName.equals("Text")) {
 						out.print(convertText(getString(c, "value"), false));
 					}
 				}
 
 				out.println("</" + name + ">");
 			} else {
 				out.println("/>");
 			}
 		}
 	}
 
 	private String getString(ASMModelElement ame, String name) {
		return ((ASMString)ame.get(null, name)).getSymbol();
 	}
 
 	private String convertText(String in, boolean inAttr) {
 		String ret = in.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
 		
 		if(inAttr) {
 			ret = ret.replaceAll("\n", "&#10;").replaceAll("\t", "&#9;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;");
 		}
 		
 		return ret;
 	}
 }
 
