 package com.sun.tools.xjc.reader.xmlschema;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.xml.bind.annotation.XmlAnyElement;
 import javax.xml.namespace.QName;
 
 import com.sun.tools.xjc.reader.gbind.Choice;
 import com.sun.tools.xjc.reader.gbind.Element;
 import com.sun.tools.xjc.reader.gbind.Expression;
 import com.sun.tools.xjc.reader.gbind.OneOrMore;
 import com.sun.tools.xjc.reader.gbind.Sequence;
 import com.sun.xml.xsom.XSElementDecl;
 import com.sun.xml.xsom.XSModelGroup;
 import com.sun.xml.xsom.XSModelGroupDecl;
 import com.sun.xml.xsom.XSParticle;
 import com.sun.xml.xsom.XSWildcard;
 import com.sun.xml.xsom.visitor.XSTermFunction;
 
 /**
  * Visits {@link XSParticle} and creates a corresponding {@link Expression} tree.
  * @author Kohsuke Kawaguchi
  */
 public final class ExpressionBuilder implements XSTermFunction<Expression> {
 
     public static Expression createTree(XSParticle p) {
         return new ExpressionBuilder().particle(p);
     }
 
     private ExpressionBuilder() {}
 
     /**
      * Wildcard instance needs to be consolidated to one,
      * and this is such instance (if any.)
      */
     private GWildcardElement wildcard = null;
 
     private final Map<QName,GElementImpl> decls = new HashMap<QName,GElementImpl>();
 
     private XSParticle current;
 
     /**
      * We can only have one {@link XmlAnyElement} property,
      * so all the wildcards need to be treated as one node.
      */
     public Expression wildcard(XSWildcard wc) {
         if(wildcard==null)
             wildcard = new GWildcardElement();
         wildcard.particles.add(current);
         return wildcard;
     }
 
     public Expression modelGroupDecl(XSModelGroupDecl decl) {
         return modelGroup(decl.getModelGroup());
     }
 
     public Expression modelGroup(XSModelGroup group) {
         XSModelGroup.Compositor comp = group.getCompositor();
         if(comp==XSModelGroup.CHOICE) {
            Expression e = null;
             for (XSParticle p : group.getChildren()) {
                 if(e==null)     e = particle(p);
                 else            e = new Choice(e,particle(p));
             }
             return e;
         } else {
            Expression e = null;
             for (XSParticle p : group.getChildren()) {
                 if(e==null)     e = particle(p);
                 else            e = new Sequence(e,particle(p));
             }
             return e;
         }
     }
 
     public Element elementDecl(XSElementDecl decl) {
         QName n = new QName(decl.getTargetNamespace(),decl.getName());
 
         GElementImpl e = decls.get(n);
         if(e==null)
             decls.put(n,e=new GElementImpl(n,decl));
 
         e.particles.add(current);
         assert current.getTerm()==decl;
 
         return e;
     }
 
     public Expression particle(XSParticle p) {
         current = p;
         Expression e = p.getTerm().apply(this);
 
         if(p.isRepeated())
             e = new OneOrMore(e);
 
         if(p.getMinOccurs()==0)
             e = new Choice(e,Expression.EPSILON);
 
         return e;
     }
 
 }
