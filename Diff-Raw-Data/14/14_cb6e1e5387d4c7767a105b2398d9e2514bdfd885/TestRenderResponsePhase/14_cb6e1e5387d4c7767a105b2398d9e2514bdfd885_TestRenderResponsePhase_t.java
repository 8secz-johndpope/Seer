 /*
 * $Id: TestRenderResponsePhase.java,v 1.48 2003/04/22 21:04:40 rkitain Exp $
  */
 
 /*
  * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
  * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
  */
 
 // TestRenderResponsePhase.java
 
 package com.sun.faces.lifecycle;
 
 import org.apache.cactus.WebRequest;
 import org.apache.cactus.JspTestCase;
 
 import org.mozilla.util.Assert;
 import org.mozilla.util.ParameterCheck;
 
 import javax.faces.FacesException;
 import javax.faces.FactoryFinder;
 import javax.faces.context.FacesContext;
 import javax.faces.context.FacesContextFactory;
 import javax.faces.lifecycle.Lifecycle;
 import javax.faces.component.UIComponentBase;
 import javax.faces.validator.Validator;
 import javax.faces.component.AttributeDescriptor;
 
 import com.sun.faces.context.FacesContextImpl;
 import com.sun.faces.lifecycle.Phase;
 import com.sun.faces.JspFacesTestCase;
 import com.sun.faces.FileOutputResponseWrapper;
 import com.sun.faces.RIConstants;
 import com.sun.faces.util.Util;
 import com.sun.faces.CompareFiles;
 
 import com.sun.faces.TestBean;
 
 import java.io.IOException;
 
 import java.util.Iterator;
 import java.util.ArrayList;
 
 import javax.faces.tree.Tree;
 import javax.faces.tree.TreeFactory;
 
 import javax.servlet.jsp.PageContext;
 
 /**
  *
  *  <B>TestRenderResponsePhase</B> is a class ...
  *
  * <B>Lifetime And Scope</B> <P>
  *
 * @version $Id: TestRenderResponsePhase.java,v 1.48 2003/04/22 21:04:40 rkitain Exp $
  * 
  * @see	Blah
  * @see	Bloo
  *
  */
 
 public class TestRenderResponsePhase extends JspFacesTestCase
 {
 //
 // Protected Constants
 //
 
 public static final String TEST_URI = "/TestRenderResponsePhase.jsp";
 
 public String getExpectedOutputFilename() {
     return "RenderResponse_correct";
 }
 
 public static final String ignore[] = {
    "<form method=\"post\" action=\"/test/faces/TestRenderResponsePhase.jsp;jsessionid=2933CA6E85C4FDDD0133E9B1D82A022A\" class=\"formClass\"  title=\"basicForm\" accept=\"html,wml\" >",
    "            <input type=\"image\" src=\"duke.gif;jsessionid=2933CA6E85C4FDDD0133E9B1D82A022A\" name=\"pushButton\" disabled >",
    "            <input type=\"image\" src=\"duke.gif;jsessionid=2933CA6E85C4FDDD0133E9B1D82A022A\" name=\"imageOnlyButton\"> ",
    "            <img id=\"graphicImage\" src=\"/test/duke.gif;jsessionid=2933CA6E85C4FDDD0133E9B1D82A022A\" usemap=\"#map1\"  ismap > "
 };
     
 public String [] getLinesToIgnore() {
     return ignore;
 }
 
 public boolean sendResponseToFile() 
 {
     return true;
 }
 
 //
 // Class Variables
 //
 
 //
 // Instance Variables
 //
 
 // Attribute Instance Variables
 
 // Relationship Instance Variables
 
 //
 // Constructors and Initializers    
 //
 
     public TestRenderResponsePhase() {
 	super("TestRenderResponsePhase");
     }
 
     public TestRenderResponsePhase(String name) {
 	super(name);
     }
 
 //
 // Class methods
 //
 
 //
 // General Methods
 //
 
 
 public void beginHtmlBasicRenderKit(WebRequest theRequest)
 {
     theRequest.setURL("localhost:8080", null, null, TEST_URI, null);
 }
 
 public void testHtmlBasicRenderKit()
 {
     System.setProperty(RIConstants.DISABLE_RENDERERS, "");
 
     boolean result = false;
     UIComponentBase root = null;
     String value = null;
     LifecycleImpl lifecycle = new LifecycleImpl();
     Phase renderResponse = new RenderResponsePhase(lifecycle);
     root = new UIComponentBase() {
 	    public String getComponentType() { return "Root"; }
 	};
     root.setComponentId("root");
  
     TreeFactory treeFactory = (TreeFactory)
          FactoryFinder.getFactory(FactoryFinder.TREE_FACTORY);
     Assert.assert_it(treeFactory != null);
     Tree requestTree = treeFactory.getTree(getFacesContext(),
             TEST_URI );
     getFacesContext().setTree(requestTree);
 
     renderResponse.execute(getFacesContext());
     assertTrue(!((FacesContextImpl)getFacesContext()).getRenderResponse() &&
         !((FacesContextImpl)getFacesContext()).getResponseComplete());
 
     assertTrue(verifyExpectedOutput());
 }
 
 } // end of class TestRenderResponsePhase
