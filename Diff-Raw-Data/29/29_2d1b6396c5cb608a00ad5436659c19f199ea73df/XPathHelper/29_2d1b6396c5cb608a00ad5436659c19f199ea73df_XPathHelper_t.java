 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2009 The Weblounge Team
  *  http://weblounge.o2it.ch
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software Foundation
  *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
 
 package ch.o2it.weblounge.common.impl.util.xml;
 
import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpressionException;
 import javax.xml.xpath.XPathFactory;
 
 /**
  * Utility class to handle and simplify XPath queries.
  */
 public class XPathHelper {
 
   /** Logging facility */
   protected final static Logger log_ = LoggerFactory.getLogger(XPathHelper.class);
 
   /**
    * Returns the query result or <code>null</code>.
    * <p>
    * <b>Note:</b> This signature creates a new <code>XPath</code> processor on
    * every call, which is probably fine for testing but not favorable when it
    * comes to production use, since creating an <code>XPath</code> processor is
    * resource intensive.
    * 
    * @param node
    *          the context node
    * @param xpath
    *          the xpath expression
    * 
    * @return the selected string or <code>null</code> if the query didn't yield
    *         a result
    */
   public static String valueOf(Node node, String xpathExpression) {
     XPath xpath = XPathFactory.newInstance().newXPath();
     return valueOf(node, xpathExpression, null, xpath);
   }
 
   /**
    * Returns the query result or <code>null</code>.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @param processor
    *          the xpath engine
    * 
    * @return the selected string or <code>null</code> if the query didn't yield
    *         a result
    */
   public static String valueOf(Node node, String xpathExpression, XPath processor) {
     return valueOf(node, xpathExpression, null, processor);
   }
 
   /**
    * Returns the query result or <code>null</code>.
    * <p>
    * <b>Note:</b> This signature creates a new <code>XPath</code> processor on
    * every call, which is probably fine for testing but not favorable when it
    * comes to production use, since creating an <code>XPath</code> processor is
    * resource intensive.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @param defaultValue
    *          the default value
    * 
    * @return the selected string or <code>defaultValue</code> if the query
    *         didn't yield a result
    */
   public static String valueOf(Node node, String xpathExpression,
       String defaultValue) {
     XPath xpath = XPathFactory.newInstance().newXPath();
     return valueOf(node, xpathExpression, defaultValue, xpath);
   }
 
   /**
    * Returns the query result or <code>null</code>.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @param defaultValue
    *          the default value
    * @param processor
    *          the xpath engine
    * 
    * @return the selected string or <code>defaultValue</code> if the query
    *         didn't yield a result
    */
   public static String valueOf(Node node, String xpathExpression,
       String defaultValue, XPath processor) {
 
     if (node == null || processor == null)
       return null;
 
     try {
      String value = StringUtils.trimToNull(processor.evaluate(xpathExpression, node));
       return (value != null) ? value : defaultValue;
     } catch (XPathExpressionException e) {
       log_.warn("Error when selecting '{}' from {}", new Object[] {
           xpathExpression,
           node,
           e });
       return null;
     }
   }
 
   /**
    * Returns the query result as a <code>Node</code> or <code>null</code> if the
    * xpath expression doesn't yield a resulting node.
    * <p>
    * <b>Note:</b> This signature creates a new <code>XPath</code> processor on
    * every call, which is probably fine for testing but not favorable when it
    * comes to production use, since creating an <code>XPath</code> processor is
    * resource intensive.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @return the selected node
    */
   public static Node select(Node node, String xpathExpression) {
     XPath xpath = XPathFactory.newInstance().newXPath();
     return select(node, xpathExpression, xpath);
   }
 
   /**
    * Returns the query result as a <code>Node</code> or <code>null</code> if the
    * xpath expression doesn't yield a resulting node.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @param processor
    *          the xpath processor
    * @return the selected node
    */
   public static Node select(Node node, String xpath, XPath processor) {
     if (node == null || processor == null) {
       return null;
     }
     try {
       return (Node) processor.evaluate(xpath, node, XPathConstants.NODE);
     } catch (XPathExpressionException e) {
       log_.warn("Error when selecting '{}' from {}", new Object[] {
           xpath,
           node,
           e });
       return null;
     }
   }
 
   /**
    * Returns the query result as a <code>NodeList</code> or <code>null</code> if
    * the xpath expression doesn't yield a result set.
    * <p>
    * <b>Note:</b> This signature creates a new <code>XPath</code> processor on
    * every call, which is probably fine for testing but not favorable when it
    * comes to production use, since creating an <code>XPath</code> processor is
    * resource intensive.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @return the selected node
    */
   public static NodeList selectList(Node node, String xpathExpression) {
     XPath xpath = XPathFactory.newInstance().newXPath();
     return selectList(node, xpathExpression, xpath);
   }
 
   /**
    * Returns the query result as a <code>NodeList</code> or <code>null</code> if
    * the xpath expression doesn't yield a result set.
    * 
    * @param node
    *          the context node
    * @param xpathExpression
    *          the xpath expression
    * @param processor
    *          the xpath processor
    * @return the selected node
    */
   public static NodeList selectList(Node node, String xpath, XPath processor) {
     if (node == null || processor == null) {
       return null;
     }
     try {
       return (NodeList) processor.evaluate(xpath, node, XPathConstants.NODESET);
     } catch (XPathExpressionException e) {
       log_.warn("Error when selecting '{}' from {}", new Object[] {
           xpath,
           node,
           e });
       return null;
     }
   }
 
 }
