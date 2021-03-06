 package com.sun.tools.xjc.reader.xmlschema;
 
 import com.sun.tools.xjc.model.CClassInfo;
 import com.sun.tools.xjc.model.CPropertyInfo;
 import com.sun.tools.xjc.model.CTypeInfo;
 import com.sun.tools.xjc.model.TypeUse;
 import com.sun.tools.xjc.reader.Ring;
 import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIProperty;
 import com.sun.xml.xsom.XSAnnotation;
 import com.sun.xml.xsom.XSComponent;
 import com.sun.xml.xsom.XSFacet;
 import com.sun.xml.xsom.XSIdentityConstraint;
 import com.sun.xml.xsom.XSNotation;
 import com.sun.xml.xsom.XSSchema;
 import com.sun.xml.xsom.XSSimpleType;
 import com.sun.xml.xsom.XSXPath;
 import com.sun.xml.xsom.visitor.XSVisitor;
 
 /**
  * @author Kohsuke Kawaguchi
  */
 abstract class ColorBinder extends BindingComponent implements XSVisitor {
     protected final BGMBuilder builder = Ring.get(BGMBuilder.class);
     protected final ClassSelector selector = getClassSelector();
 
     protected final CClassInfo getCurrentBean() {
         return selector.getCurrentBean();
     }
     protected final XSComponent getCurrentRoot() {
         return selector.getCurrentRoot();
     }
 
     protected CTypeInfo boundToType(XSComponent sc) {
         if(sc==selector.getCurrentRoot())    return null;
         return selector.bindToType(sc);
     }
 
 
 
 
     protected final void createSimpleTypeProperty(XSSimpleType type,String propName) {
         BIProperty prop = BIProperty.getCustomization(type);
 
         SimpleTypeBuilder stb = Ring.get(SimpleTypeBuilder.class);
        TypeUse use = type.apply(stb.composer);

        CPropertyInfo p = prop.createValueProperty(propName,false,type,use);
         getCurrentBean().addProperty(p);
     }
 
 
 
 
 
     public final void annotation(XSAnnotation xsAnnotation) {
         throw new IllegalStateException();
     }
 
     public final void schema(XSSchema xsSchema) {
         throw new IllegalStateException();
     }
 
     public final void facet(XSFacet xsFacet) {
         throw new IllegalStateException();
     }
 
     public final void notation(XSNotation xsNotation) {
         throw new IllegalStateException();
     }
 
     public final void identityConstraint(XSIdentityConstraint xsIdentityConstraint) {
         throw new IllegalStateException();
     }
 
     public final void xpath(XSXPath xsxPath) {
         throw new IllegalStateException();
     }
 }
