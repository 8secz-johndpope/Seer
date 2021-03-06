 /*
  * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
  * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
  */
 
 /**
 * $Id: TestRenderers_3.java,v 1.23 2003/09/09 19:04:00 jvisvanathan Exp $
  *
  * (C) Copyright International Business Machines Corp., 2001,2002
  * The source code for this program is not published or otherwise
  * divested of its trade secrets, irrespective of what has been
  * deposited with the U. S. Copyright Office.   
  */
 
 // TestRenderers_3.java
 
 package com.sun.faces.renderkit.html_basic;
 import java.io.IOException;
 
 import javax.faces.FactoryFinder;
 import javax.faces.application.Application;
 import javax.faces.application.ApplicationFactory;
 import javax.faces.component.UICommand;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UISelectItems;
 import javax.faces.component.UISelectMany;
 import javax.faces.component.UISelectOne;
 import javax.faces.component.UIInput;
 import javax.faces.component.NamingContainer;
 import javax.faces.component.base.UICommandBase;
 import javax.faces.component.base.UISelectManyBase;
 import javax.faces.component.base.UISelectItemsBase;
import javax.faces.component.UIViewRoot;
 import javax.faces.component.base.UISelectOneBase;
 import javax.faces.component.base.UIInputBase;
 import javax.faces.component.base.UIViewRootBase;
 import javax.faces.context.FacesContextFactory;
 import javax.faces.convert.Converter;
 import javax.faces.convert.NumberConverter;
 import javax.faces.model.SelectItem;
 
 import java.text.DateFormat;
 import java.text.NumberFormat;
 import java.util.Date;
 
 import com.sun.faces.renderkit.html_basic.HiddenRenderer;
 import org.apache.cactus.WebRequest;
 
 import com.sun.faces.JspFacesTestCase;
 
 /**
  *
  *  Test encode and decode methods in Renderer classes.
  *
  * <B>Lifetime And Scope</B> <P>
  *
 * @version $Id: TestRenderers_3.java,v 1.23 2003/09/09 19:04:00 jvisvanathan Exp $
  * 
  *
  */
 
 public class TestRenderers_3 extends JspFacesTestCase {
     //
     // Instance Variables
     //
     private Application application;
 
     //
     // Protected Constants
     //
     public static String DATE_STR = "Jan 12, 1952";
     
     public static String NUMBER_STR = "47%";
    
     public boolean sendWriterToFile() {
         return true;
     }
 
     public String getExpectedOutputFilename() {
         return "CorrectRenderersResponse_3";
     }
 
     //
     // Class Variables
     //
 
     //
     // Instance Variables
     //
     private FacesContextFactory facesContextFactory = null;
 
     // Attribute Instance Variables
     // Relationship Instance Variables
     //
     // Constructors and Initializers    
     //
 
     public TestRenderers_3() {
         super("TestRenderers_3");
     }
     public TestRenderers_3(String name) {
         super(name);
     }
 
     //
     // Class methods
     //
 
     //
     // Methods from TestCase
     //
     public void setUp() {
         super.setUp();
         ApplicationFactory aFactory = 
 	    (ApplicationFactory)FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
         application = aFactory.getApplication();
 	UIViewRootBase xmlTree = new UIViewRootBase();
 	xmlTree.setViewId("viewId");
 	xmlTree.getChildren().add(new UICommandBase());
         getFacesContext().setViewRoot(xmlTree);
         assertTrue(null != getFacesContext().getResponseWriter());
     }
 
     public void beginRenderers(WebRequest theRequest) {
         theRequest.addParameter("myMenu", "Blue");
         theRequest.addParameter("myListbox", "Blue");
         theRequest.addParameter("myCheckboxlist", "Blue");
         theRequest.addParameter("myOnemenu", "Blue");
         // parameters to test hidden renderer
         theRequest.addParameter("myNumberHidden", NUMBER_STR);
         theRequest.addParameter("myInputDateHidden", DATE_STR);
 
     }
 
     public void testRenderers() {
 
         try {
             // create a dummy root for the tree.
            UIViewRoot root = getFacesContext().getViewRoot();
             root.setId("root");
 
             testSelectManyMenuRenderer(root);
             testSelectManyListboxRenderer(root);
             testSelectManyCheckboxListRenderer(root);
             testSelectOneMenuRenderer(root);
             testHiddenRenderer(root);
             assertTrue(verifyExpectedOutput());
         }
         catch (Throwable t) {
             t.printStackTrace();
             assertTrue(false);
             return;
         }
     }
 
     public void testSelectManyListboxRenderer(UIComponent root)
         throws IOException {
         System.out.println("Testing SelectManyListboxRenderer");
         UISelectMany selectMany = new UISelectManyBase();
         UISelectItems uiSelectItems = new UISelectItemsBase();
         selectMany.setValue(null);
         selectMany.setId("myListbox");
         SelectItem item1 = new SelectItem("Red", "Red", null);
         SelectItem item2 = new SelectItem("Blue", "Blue", null);
         SelectItem item3 = new SelectItem("Green", "Green", null);
         SelectItem item4 = new SelectItem("Yellow", "Yellow", null);
         SelectItem[] selectItems = { item1, item2, item3, item4 };
 	Object selectedValues[] = null;
         uiSelectItems.setValue(selectItems);
        uiSelectItems.setId("manyListitems");
         selectMany.getChildren().add(uiSelectItems);
         root.getChildren().add(selectMany);
 
         ListboxRenderer selectManyListboxRenderer =
             new ListboxRenderer();
 
         // test decode method
         System.out.println("    Testing decode method... ");
         selectManyListboxRenderer.decode(getFacesContext(), selectMany);
 	assertTrue(null != (selectedValues = selectMany.getSelectedValues()));
 	assertTrue(1 == selectedValues.length);
         assertTrue(((String)selectedValues[0]).equals("Blue"));
 
         // test encode method
 
         System.out.println("    Testing encode method... ");
         selectManyListboxRenderer.encodeBegin(getFacesContext(), selectMany);
         selectManyListboxRenderer.encodeEnd(getFacesContext(), selectMany);
         getFacesContext().getResponseWriter().writeText("\n", null);
         getFacesContext().getResponseWriter().flush();
 
     }
 
     public void testSelectManyCheckboxListRenderer(UIComponent root)
         throws IOException {
         System.out.println("Testing SelectManyCheckboxListRenderer");
         UISelectMany selectMany = new UISelectManyBase();
         UISelectItems uiSelectItems = new UISelectItemsBase();
         selectMany.setValue(null);
         selectMany.setId("myCheckboxlist");
         SelectItem item1 = new SelectItem("Red", "Red", null);
         SelectItem item2 = new SelectItem("Blue", "Blue", null);
         SelectItem item3 = new SelectItem("Green", "Green", null);
         SelectItem item4 = new SelectItem("Yellow", "Yellow", null);
         SelectItem[] selectItems = { item1, item2, item3, item4 };
 	Object selectedValues[] = null;
         uiSelectItems.setValue(selectItems);
         selectMany.getChildren().add(uiSelectItems);
         root.getChildren().add(selectMany);
 
         SelectManyCheckboxListRenderer selectManyCheckboxListRenderer =
             new SelectManyCheckboxListRenderer();
 
         // test decode method
 
         System.out.println("    Testing decode method... ");
         selectManyCheckboxListRenderer.decode(getFacesContext(), selectMany);
 	assertTrue(null != (selectedValues = selectMany.getSelectedValues()));
 	assertTrue(1 == selectedValues.length);
         assertTrue(((String)selectedValues[0]).equals("Blue"));
 
 
         // test encode method
         System.out.println("    Testing encode method... ");
         selectManyCheckboxListRenderer.encodeBegin(
             getFacesContext(),
             selectMany);
         selectManyCheckboxListRenderer.encodeEnd(getFacesContext(), 
 						 selectMany);
         getFacesContext().getResponseWriter().writeText("\n", null);
         getFacesContext().getResponseWriter().flush();
 
     }
 
     public void testSelectManyMenuRenderer(UIComponent root)
         throws IOException {
         System.out.println("Testing SelectManyMenuRenderer");
         UISelectMany selectMany = new UISelectManyBase();
         UISelectItems uiSelectItems = new UISelectItemsBase();
         selectMany.setValue(null);
         selectMany.setId("myMenu");
         SelectItem item1 = new SelectItem("Red", "Red", null);
         SelectItem item2 = new SelectItem("Blue", "Blue", null);
         SelectItem item3 = new SelectItem("Green", "Green", null);
         SelectItem item4 = new SelectItem("Yellow", "Yellow", null);
         SelectItem[] selectItems = { item1, item2, item3, item4 };
 	Object selectedValues[] = null;
         uiSelectItems.setValue(selectItems);
        uiSelectItems.setId("manyMenuitems");
         selectMany.getChildren().add(uiSelectItems);
         root.getChildren().add(selectMany);
 
         MenuRenderer selectManyMenuRenderer =
             new MenuRenderer();
 
         // test decode method
         System.out.println("    Testing decode method... ");
         selectManyMenuRenderer.decode(getFacesContext(), selectMany);
 	assertTrue(null != (selectedValues = selectMany.getSelectedValues()));
 	assertTrue(1 == selectedValues.length);
         assertTrue(((String)selectedValues[0]).equals("Blue"));
 
         // test encode method
         System.out.println("    Testing encode method... ");
         selectManyMenuRenderer.encodeBegin(getFacesContext(), selectMany);
         selectManyMenuRenderer.encodeEnd(getFacesContext(), selectMany);
         getFacesContext().getResponseWriter().writeText("\n", null);
         getFacesContext().getResponseWriter().flush();
 
     }
 
     public void testSelectOneMenuRenderer(UIComponent root)
         throws IOException {
         System.out.println("Testing SelectOneMenuRenderer");
         UISelectOne selectOne = new UISelectOneBase();
         UISelectItems uiSelectItems = new UISelectItemsBase();
         selectOne.setValue(null);
         selectOne.setId("myOnemenu");
         SelectItem item1 = new SelectItem("Red", "Red", null);
         SelectItem item2 = new SelectItem("Blue", "Blue", null);
         SelectItem item3 = new SelectItem("Green", "Green", null);
         SelectItem item4 = new SelectItem("Yellow", "Yellow", null);
         SelectItem[] selectItems = { item1, item2, item3, item4 };
         String selectedValue = null;
         uiSelectItems.setValue(selectItems);
        uiSelectItems.setId("manySelectOneitems");
         selectOne.getChildren().add(uiSelectItems);
         root.getChildren().add(selectOne);
 
         MenuRenderer selectOneMenuRenderer =
             new MenuRenderer();
 
         // test decode method
         System.out.println("    Testing decode method... ");
         selectOneMenuRenderer.decode(getFacesContext(), selectOne); 
         selectedValue = (String)selectOne.getValue();
         assertTrue(selectedValue.equals("Blue"));
 
         // test encode method
         System.out.println("    Testing encode method... ");
         selectOneMenuRenderer.encodeBegin(getFacesContext(), selectOne);
         selectOneMenuRenderer.encodeEnd(getFacesContext(), selectOne);
         getFacesContext().getResponseWriter().writeText("\n", null);
         getFacesContext().getResponseWriter().flush();
 
     }
     
     public void testHiddenRenderer(UIComponent root) throws IOException {
         System.out.println("Testing Input_DateRenderer");
         UIInput input1 = new UIInputBase();
         input1.setValue(null);
         input1.setId("myInputDateHidden");
         Converter converter = application.createConverter("DateTime");
         input1.setConverter(converter);
 	input1.setAttribute("dateStyle", "medium");
         root.getChildren().add(input1);
         HiddenRenderer hiddenRenderer = new HiddenRenderer();
         
         DateFormat dateformatter = 
 	    DateFormat.getDateInstance(DateFormat.MEDIUM,
 				       getFacesContext().getLocale());
         
         // test hidden renderer with converter set to date
         // test decode method
 	System.out.println("    Testing decode method...");
         hiddenRenderer.decode(getFacesContext(), input1);
 	Date date = (Date) input1.getValue();
 	assertTrue(null != date);
 	assertTrue(DATE_STR.equals(dateformatter.format(date)));
         
         // test encode method
         System.out.println("    Testing encode method...");
         hiddenRenderer.encodeBegin(getFacesContext(), input1);
         hiddenRenderer.encodeEnd(getFacesContext(), input1);
         getFacesContext().getResponseWriter().flush();
         
         // test hidden renderer with converter set to number
         UIInput input2 = new UIInputBase();
         input2.setValue(null);
         input2.setId("myNumberHidden");
         converter = application.createConverter("Number");
 	((NumberConverter)converter).setType("percent");
         input2.setConverter(converter);
         root.getChildren().add(input2);
 
 	NumberFormat numberformatter = 
 	    NumberFormat.getPercentInstance(getFacesContext().getLocale());
         // test decode method
         System.out.println("    Testing decode method...");
         hiddenRenderer.decode(getFacesContext(), input2);
 	Number number = (Number) input2.getValue();
 	assertTrue(null != number);
 	System.out.println("NUMBER_STR:"+NUMBER_STR);
 	System.out.println("NUMBERFORMATTER:"+numberformatter.format(number));
 	assertTrue(NUMBER_STR.equals(numberformatter.format(number)));
    
         // test encode method
         System.out.println("    Testing encode method...");
         hiddenRenderer.encodeBegin(getFacesContext(), input2);
         hiddenRenderer.encodeEnd(getFacesContext(), input2);
         getFacesContext().getResponseWriter().flush();
        
     }
 } // end of class TestRenderers_3
