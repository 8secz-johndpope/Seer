 /*
  [The "BSD licence"]
  Copyright (c) 2003-2005 Terence Parr
  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. The name of the author may not be used to endorse or promote products
     derived from this software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 package org.antlr.stringtemplate.language;
 
 import org.antlr.stringtemplate.*;
 
 import java.io.IOException;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.util.*;
 import java.lang.reflect.Method;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.Field;
 
 import antlr.collections.AST;
 import antlr.RecognitionException;
 
 /** A single string template expression enclosed in $...; separator=...$
  *  parsed into an AST chunk to be evaluated.
  */
 public class ASTExpr extends Expr {
     public static final String DEFAULT_ATTRIBUTE_NAME = "it";
     public static final String DEFAULT_ATTRIBUTE_NAME_DEPRECATED = "attr";
     public static final String DEFAULT_INDEX_VARIABLE_NAME = "i";
 
     /** How to refer to the surrounding template object;
      *  this is attribute name to use.
     public static final String SELF_ATTR = "self";
      */
 
     /** When lint mode is on, this attribute contains a list of reflection
      *  objects that tell you about the attributes set by the user of the
      *  template.
      */
     public static final String REFLECTION_ATTRIBUTES = "attributes";
 
 	AST exprTree = null;
 
     /** store separator etc... */
     Map options = null;
 
     public ASTExpr(StringTemplate enclosingTemplate, AST exprTree, Map options) {
 		super(enclosingTemplate);
         this.exprTree = exprTree;
         this.options = options;
     }
 
 	/** Return the tree interpreted when this template is written out. */
 	public AST getAST() {
 		return exprTree;
 	}
 
     /** To write out the value of an ASTExpr, invoke the evaluator in eval.g
      *  to walk the tree writing out the values.  For efficiency, don't
      *  compute a bunch of strings and then pack them together.  Write out directly.
      */
     public int write(StringTemplate self, StringTemplateWriter out) throws IOException {
         if ( exprTree==null || self==null || out==null ) {
             return 0;
         }
         out.pushIndentation(getIndentation());
         //System.out.println("evaluating tree: "+exprTree.toStringList());
         ActionEvaluator eval =
                 new ActionEvaluator(self,this,out);
 		int n = 0;
         try {
             n = eval.action(exprTree); // eval and write out tree
         }
         catch (RecognitionException re) {
             self.error("can't evaluate tree: "+exprTree.toStringList(), re);
         }
         out.popIndentation();
 		return n;
     }
 
     // HELP ROUTINES CALLED BY EVALUATOR TREE WALKER
 
     public Object applyListOfAlternatingTemplates(StringTemplate self,
                                                   Object attributeValue,
                                                   List templatesToApply)
     {
         if ( attributeValue==null || templatesToApply==null || templatesToApply.size()==0 ) {
             return null; // do not apply if missing templates or empty value
         }
         StringTemplate embedded = null;
         Map argumentContext = null;
 
         // normalize collections and such to use iterators
         // anything iteratable can be used for "APPLY"
 		attributeValue = convertAnythingIteratableToIterator(attributeValue);
 
         if ( attributeValue instanceof Iterator ) {
             List resultVector = new ArrayList();
             Iterator iter = (Iterator)attributeValue;
             int i = 0;
             while ( iter.hasNext() ) {
                 Object ithValue = iter.next();
                 if ( ithValue==null ) {
                     // weird...a null value in the list; ignore
                     continue;
                 }
                 int templateIndex = i % templatesToApply.size(); // rotate through
                 embedded = (StringTemplate)templatesToApply.get(templateIndex);
                 // template to apply is an actual StringTemplate (created in
                 // eval.g), but that is used as the examplar.  We must create
                 // a new instance of the embedded template to apply each time
                 // to get new attribute sets etc...
                 StringTemplateAST args = embedded.getArgumentsAST();
                 embedded = embedded.getInstanceOf(); // make new instance
                 embedded.setEnclosingInstance(self);
                 embedded.setArgumentsAST(args);
                 argumentContext = new HashMap();
                 argumentContext.put(DEFAULT_ATTRIBUTE_NAME, ithValue);
                 argumentContext.put(DEFAULT_ATTRIBUTE_NAME_DEPRECATED, ithValue);
                 argumentContext.put(DEFAULT_INDEX_VARIABLE_NAME, new Integer(i+1));
                 embedded.setArgumentContext(argumentContext);
                 evaluateArguments(embedded);
                 /*
 				System.err.println("i="+i+": applyTemplate("+embedded.getName()+
                         ", args="+argumentContext+
                         " to attribute value "+ithValue);
                 */
 				resultVector.add(embedded);
                 i++;
             }
 			if ( resultVector.size()==0 ) {
 				resultVector = null;
 			}
 			return resultVector;
         }
         else {
             /*
             System.out.println("setting attribute "+DEFAULT_ATTRIBUTE_NAME+" in arg context of "+
             embedded.getName()+
             " to "+attributeValue);
             */
             embedded = (StringTemplate)templatesToApply.get(0);
             argumentContext = new HashMap();
             argumentContext.put(DEFAULT_ATTRIBUTE_NAME, attributeValue);
             argumentContext.put(DEFAULT_ATTRIBUTE_NAME_DEPRECATED, attributeValue);
             argumentContext.put(DEFAULT_INDEX_VARIABLE_NAME, new Integer(1));
             embedded.setArgumentContext(argumentContext);
             evaluateArguments(embedded);
             return embedded;
         }
     }
 
     /** Return o.getPropertyName() given o and propertyName.  If o is
      *  a stringtemplate then access it's attributes looking for propertyName
      *  instead (don't check any of the enclosing scopes; look directly into
      *  that object).  Also try isXXX() for booleans.  Allow HashMap,
      *  Hashtable as special case (grab value for key).
      */
     public Object getObjectProperty(StringTemplate self, Object o, String propertyName) {
         if ( o==null || propertyName==null ) {
             return null;
         }
         Class c = o.getClass();
         Object value = null;
 
         // Special case: our automatically created Aggregates via
         // attribute name: "{obj.{prop1,prop2}}"
         if ( c==StringTemplate.Aggregate.class ) {
             value = ((StringTemplate.Aggregate)o).get(propertyName);
         }
 
         // Special case: if it's a template, pull property from
         // it's attribute table.
         // TODO: TJP just asked himself why we can't do inherited attr here?
         else if ( c==StringTemplate.class ) {
             Map attributes = ((StringTemplate)o).getAttributes();
             if ( attributes!=null ) {
                 value = attributes.get(propertyName);
             }
         }
 
         // Special case: if it's a HashMap, Hashtable then pull using
         // key not the property method.  Do NOT allow general Map interface
         // as people could pass in their database masquerading as a Map.
         else if ( isValidMapInstance(c) ) {
             Map map = (Map)o;
             value = map.get(propertyName);
         }
 
         else {
             // use getPropertyName() lookup
             String methodSuffix = Character.toUpperCase(propertyName.charAt(0))+
                         propertyName.substring(1,propertyName.length());
             String methodName = "get"+methodSuffix;
             Method m = null;
             try {
                 m = c.getMethod(methodName, null);
             }
             catch (NoSuchMethodException nsme) {
                 methodName = "is"+methodSuffix;
                 try {
                     m = c.getMethod(methodName, null);
                 }
                 catch (NoSuchMethodException nsme2) {
 					// try for a visible field
 					try {
 						Field f = c.getField(propertyName);
 						try {
 							value = f.get(o);
 							return value;
 						}
 						catch (IllegalAccessException iae) {
							self.error("Can't access property "+propertyName+" using method get/is"+methodSuffix+
 									   " or direct field access from "+c.getName()+" instance", iae);
 						}
 					}
 					catch (NoSuchFieldException nsfe) {
						throw new NoSuchElementException("Class "+c.getName()+" has no such attribute: "+propertyName+
														 " in template context "+self.getEnclosingInstanceStackString());
 					}
 				}
 			}
 			try {
 				value = m.invoke(o,null);
 			}
 			catch (Exception e) {
 				self.error("Can't get property "+propertyName+" using method get/is"+methodSuffix+
 						   " from "+c.getName()+" instance", e);
 			}
 		}
 
 		// take care of array properties...convert to a List so we can
 		// apply templates to the elements etc...
 		value = convertArrayToList(value);
         return value;
     }
 
 	/** Normally StringTemplate tests presence or absence of attributes
 	 *  for adherence to my principles of separation, but some people
 	 *  disagree and want to change.
 	 *
 	 *  For 2.0, if the object is a boolean, do something special. $if(boolean)$
 	 *  will actually test the value.  Now, this breaks my rules of entanglement
 	 *  listed in my paper, but it truly surprises programmers to have booleans
 	 *  always true.  Further, the key to isolating logic in the model is avoiding
 	 *  operators (for which you need attribute values).  But, no operator is
 	 *  needed to use boolean values.  Well, actually I guess "!" (not) is
 	 *  an operator.  Regardless, for practical reasons, I'm going to technically
 	 *  violate my rules as I currently have them defined.  Perhaps for a future
 	 *  version of the paper I will refine the rules.
 	 *
 	 *  Post 2.1, I added a check for non-null Iterators, Collections, ...
 	 *  with size==0 to return false. TJP 5/1/2005
 	 */
 	public boolean testAttributeTrue(Object a) {
 		if ( a==null ) {
 			return false;
 		}
 		if ( a instanceof Boolean ) {
 			return ((Boolean)a).booleanValue();
 		}
 		if ( a instanceof Collection ) {
 			return ((Collection)a).size()>0;
 		}
 		if ( a instanceof Map ) {
 			return ((Map)a).size()>0;
 		}
 		if ( a instanceof Iterator ) {
 			return ((Iterator)a).hasNext();
 		}
 		return true; // any other non-null object, return true--it's present
 	}
 
     /** For strings or other objects, catenate and return.
      */
     public Object add(Object a, Object b) {
         if ( a==null ) { // a null value means don't do cat, just return other value
             return b;
         }
         else if ( b==null ) {
             return a;
         }
         return a.toString() + b.toString();
     }
 
     /** Call a string template with args and return result.  Do not convert
      *  to a string yet.  It may need attributes that will be available after
      *  this is inserted into another template.
      */
     public StringTemplate getTemplateInclude(StringTemplate enclosing,
                                          String templateName,
                                          StringTemplateAST argumentsAST)
     {
         StringTemplateGroup group = enclosing.getGroup();
         StringTemplate embedded = group.getEmbeddedInstanceOf(enclosing, templateName);
         if ( embedded==null ) {
             enclosing.error("cannot make embedded instance of "+templateName+
                     " in template "+enclosing.getName());
             return null;
         }
         embedded.setArgumentsAST(argumentsAST);
         evaluateArguments(embedded);
         return embedded;
     }
 
     /** How to spit out an object.  If it's not a StringTemplate nor a
      *  List, just do o.toString().  If it's a StringTemplate,
      *  do o.write(out).  If it's a Vector, do a write(out,
      *  o.elementAt(i)) for all elements.  Note that if you do
      *  something weird like set the values of a multivalued tag
      *  to be vectors, it will effectively flatten it.
      *
      *  If self is an embedded template, you might have specified
      *  a separator arg; used when is a vector.
      */
     public int writeAttribute(StringTemplate self, Object o, StringTemplateWriter out) {
         Object separator = null;
         if ( options!=null ) {
             separator = options.get("separator");
         }
         return write(self,o,out,separator);
     }
 
 	protected int write(StringTemplate self,
 						Object o,
 						StringTemplateWriter out,
 						Object separator)
     {
         if ( o==null ) {
             return 0;
         }
 		int n = 0;
         try {
             if ( o instanceof StringTemplate ) {
                 StringTemplate stToWrite = (StringTemplate)o;
                 stToWrite.setEnclosingInstance(self);
                 // if self is found up the enclosing instance chain, then
                 // infinite recursion
                 if ( StringTemplate.inLintMode() &&
                      StringTemplate.isRecursiveEnclosingInstance(stToWrite) )
                 {
                     // throw exception since sometimes eval keeps going
                     // even after I ignore this write of o.
                     throw new IllegalStateException("infinite recursion to "+
                             stToWrite.getTemplateDeclaratorString()+" referenced in "+
                             stToWrite.getEnclosingInstance().getTemplateDeclaratorString()+
                             "; stack trace:\n"+stToWrite.getEnclosingInstanceStackTrace());
                 }
                 else {
                     n = stToWrite.write(out);
                 }
                 return n;
             }
             // normalize anything iteratable to iterator
 			o = convertAnythingIteratableToIterator(o);
 			if ( o instanceof Iterator ) {
 				Iterator iter = (Iterator)o;
                 String separatorString = null;
                 if ( separator!=null ) {
                     separatorString = computeSeparator(self, out, separator);
                 }
                 int i = 0;
                 while ( iter.hasNext() ) {
                     i++;
                     Object iterValue = iter.next();
                     int charWrittenForValue =
 						write(self, iterValue, out, separator);
 					n += charWrittenForValue;
                     if ( iter.hasNext() ) {
 						boolean valueIsPureConditional = false;
 						if ( iterValue instanceof StringTemplate ) {
 							StringTemplate iterValueST = (StringTemplate)iterValue;
 							List chunks = (List)iterValueST.getChunks();
 							Expr firstChunk = (Expr)chunks.get(0);
 							valueIsPureConditional =
 								firstChunk instanceof ConditionalExpr &&
 								((ConditionalExpr)firstChunk).getElseSubtemplate()==null;
 						}
 						boolean emptyIteratedValue =
 							valueIsPureConditional && charWrittenForValue==0;
                         if ( !emptyIteratedValue && separator!=null ) {
 							n += out.write(separatorString);
                         }
                     }
                 }
             }
             else {
                 n = out.write( o.toString() );
 				return n;
             }
         }
         catch (IOException io) {
             self.error("problem writing object: "+o, io);
         }
 		return n;
     }
 
 	/** A separator is normally just a string literal, but is still an AST that
      *  we must evaluate.  The separator can be any expression such as a template
      *  include or string cat expression etc...
      */
     protected String computeSeparator(StringTemplate self,
 									  StringTemplateWriter out,
 									  Object separator)
 	{
         if ( separator==null ) {
             return null;
         }
         if ( separator instanceof StringTemplateAST ) {
             StringTemplateAST separatorTree = (StringTemplateAST)separator;
             // must evaluate, writing to a string so we can hand on to it
             ASTExpr e = new ASTExpr(getEnclosingTemplate(),separatorTree,null);
             StringWriter buf = new StringWriter();
 			// create a new instance of whatever StringTemplateWriter
 			// implementation they are using.  Default is AutoIndentWriter.
 			// Defalut behavior is to indent but without
             // any prior indents surrounding this attribute expression
 			StringTemplateWriter sw = null;
 			Class writerClass = out.getClass();
 			try {
 				Constructor ctor =
 						writerClass.getConstructor(new Class[] {Writer.class});
 				sw = (StringTemplateWriter)ctor.newInstance(new Object[] {buf});
 			}
 			catch (Exception exc) {
 				// default new AutoIndentWriter(buf)
 				self.error("cannot make implementation of StringTemplateWriter",exc);
 				sw = new AutoIndentWriter(buf);
 			}
 
 			try {
 				e.write(self,sw);
             }
             catch (IOException ioe) {
                 self.error("can't evaluate separator expression", ioe);
             }
             return buf.toString();
         }
         else {
             // just in case we expand in the future and it's something else
             return separator.toString();
         }
     }
 
     protected void evaluateArguments(StringTemplate self) {
         StringTemplateAST argumentsAST = self.getArgumentsAST();
         if ( argumentsAST==null || argumentsAST.getFirstChild()==null ) {
             // return immediately if missing tree or no actual args
             return;
         }
         ActionEvaluator eval =
                 new ActionEvaluator(self,this,null);
         try {
             // using any initial argument context (such as when obj is set),
             // evaluate the arg list like bold(item=obj).  Since we pass
             // in any existing arg context, that context gets filled with
             // new values.  With bold(item=obj), context becomes:
             // {[obj=...],[item=...]}.
             Map ac = eval.argList(argumentsAST, self.getArgumentContext());
             self.setArgumentContext(ac);
         }
         catch (RecognitionException re) {
             self.error("can't evaluate tree: "+argumentsAST.toStringList(), re);
         }
     }
 
 	/** Do a standard conversion of array attributes to Lists.
 	 */
 	public static Object convertArrayToList(Object value) {
 		if ( value instanceof Object[] ) {
 			value = Arrays.asList((Object[])value);
 		}
 		else if ( value instanceof int[] ) {
 			int[] list = (int[])value;
 			List v = new ArrayList(list.length);
 			for (int i = 0; i < list.length; i++) {
 				int elem = list[i];
 				v.add(new Integer(elem));
 			}
 			value = v;
 		}
 		else if ( value instanceof long[] ) {
 			long[] list = (long[])value;
 			List v = new ArrayList(list.length);
 			for (int i = 0; i < list.length; i++) {
 				long elem = list[i];
 				v.add(new Long(elem));
 			}
 			value = v;
 		}
 		else if ( value instanceof float[] ) {
 			float[] list = (float[])value;
 			List v = new ArrayList(list.length);
 			for (int i = 0; i < list.length; i++) {
 				float elem = list[i];
 				v.add(new Float(elem));
 			}
 			value = v;
 		}
 
 		else if ( value instanceof double[] ) {
 			double[] list = (double[])value;
 			List v = new ArrayList(list.length);
 			for (int i = 0; i < list.length; i++) {
 				double elem = list[i];
 				v.add(new Double(elem));
 			}
 			value = v;
 		}
 		return value;
 	}
 
 	private static Object convertAnythingIteratableToIterator(Object o) {
 		Iterator iter = null;
 		if ( o instanceof Collection ) {
 			iter = ((Collection)o).iterator();
 		}
 		else if ( o instanceof Map ) {
 			iter = ((Map)o).values().iterator();
 		}
 		else if ( o instanceof Iterator ) {
 			iter = (Iterator)o;
 		}
 		if ( iter==null ) {
 			return o;
 		}
 		return iter;
 	}
 
     public static boolean isValidMapInstance(Class type) {
         return type==HashMap.class || type==Hashtable.class;
     }
 
     /** A property can be declared as Map, but the instance must be
      *  isValidMapInstance().
      */
     public static boolean isValidReturnTypeMapInstance(Class type) {
         return isValidMapInstance(type) || type==Map.class;
     }
 }
