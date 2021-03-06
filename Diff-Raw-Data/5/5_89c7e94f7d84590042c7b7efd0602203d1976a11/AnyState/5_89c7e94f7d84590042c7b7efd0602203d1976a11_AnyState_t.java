 /*
  * @(#)$Id$
  *
  * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
  * 
  * This software is the proprietary information of Sun Microsystems, Inc.  
  * Use is subject to license terms.
  * 
  */
 package com.sun.msv.reader.xmlschema;
 
 import com.sun.msv.grammar.Expression;
 import com.sun.msv.grammar.ReferenceExp;
 import com.sun.msv.grammar.NameClass;
 import com.sun.msv.grammar.NotNameClass;
 import com.sun.msv.grammar.NamespaceNameClass;
 import com.sun.msv.grammar.ChoiceNameClass;
 import com.sun.msv.grammar.AnyNameClass;
 import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.trex.DifferenceNameClass;
 import com.sun.msv.grammar.xmlschema.ElementDeclExp;
 import com.sun.msv.grammar.xmlschema.LaxDefaultNameClass;
 import com.sun.msv.grammar.xmlschema.XMLSchemaSchema;
 import com.sun.msv.reader.ExpressionWithoutChildState;
 import java.util.StringTokenizer;
 import java.util.Iterator;
 
 /**
  * base implementation of AnyAttributeState and AnyElementState.
  * 
  * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
  */
 public abstract class AnyState extends ExpressionWithoutChildState {
 
 	protected final Expression makeExpression() {
 		return createExpression(
 			startTag.getDefaultedAttribute("namespace","##any"),
 			startTag.getDefaultedAttribute("processContents","strict") );
 	}
 	
 	protected abstract Expression createExpression( String namespace, String process );
 	
 	/**
 	 * processes 'namepsace' attribute and gets corresponding NameClass object.
 	 */
 	protected NameClass getNameClass( String namespace, XMLSchemaSchema currentSchema ) {
 		// we have to get currentSchema through parameter because
 		// this method is also used while back-patching, and 
 		// reader.currentSchema points to the invalid schema in that case.
 		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
 		namespace = namespace.trim();
 		
 		if( namespace.equals("##any") )
 			return AnyNameClass.theInstance;
 		
 		if( namespace.equals("##other") )
 			return new NotNameClass( new NamespaceNameClass(currentSchema.targetNamespace) );
 		
 		NameClass choices=null;
 		
 		StringTokenizer tokens = new StringTokenizer(namespace);
 		while( tokens.hasMoreTokens() ) {
 			String token = tokens.nextToken();
 			
 			NameClass nc;
 			if( token.equals("##targetNamespace") )
 				nc = new NamespaceNameClass(currentSchema.targetNamespace);
 			else
 			if( token.equals("##local") )
 				nc = new NamespaceNameClass("");
 			else
 				nc = new NamespaceNameClass(token);
 			
 			if( choices==null )		choices = nc;
 			else					choices = new ChoiceNameClass(choices,nc);
 		}
 		
 		if( choices==null ) {
 			// no item was found.
 			reader.reportError( reader.ERR_BAD_ATTRIBUTE_VALUE, "namespace", namespace );
 			return AnyNameClass.theInstance;
 		}
 		
 		return choices;
 	}
 	
 					
 	protected NameClass createLaxNameClass( NameClass allowedNc, XMLSchemaReader.RefResolver res ) {
 		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
 		LaxDefaultNameClass laxNc = new LaxDefaultNameClass();
 				
 		Iterator itr = reader.grammar.schemata.values().iterator();
 		while( itr.hasNext() ) {
 			XMLSchemaSchema schema = (XMLSchemaSchema)itr.next();
 			if(allowedNc.accepts( schema.targetNamespace, NameClass.LOCALNAME_WILDCARD )) {
 				ReferenceExp[] refs = res.get(schema).getAll();
 				for( int i=0; i<refs.length; i++ ) {
 					ElementDeclExp decl = (ElementDeclExp)refs[i];
 					NameClass elementName = decl.self.getNameClass();
 							
 					if(!(elementName instanceof SimpleNameClass ))
 						// assertion failed.
 						// XML Schema's element declaration is always simple name.
 						throw new Error();
 					SimpleNameClass snc = (SimpleNameClass)elementName;
 							
 					laxNc.addAllowedName(snc.namespaceURI,snc.localName);
 				}
 			}
 		}
 
		// laxNc - names in namespaces that are not allowed.
		return new DifferenceNameClass( laxNc, new NotNameClass(allowedNc) );
 	}
 }
