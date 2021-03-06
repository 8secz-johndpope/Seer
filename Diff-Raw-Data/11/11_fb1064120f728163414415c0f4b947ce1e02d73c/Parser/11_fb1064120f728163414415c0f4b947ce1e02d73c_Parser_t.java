 /**********************************************************************
  * Copyright (c) 2002,2003 Rational Software Corporation and others.
  * All rights reserved.   This program and the accompanying materials
  * are made available under the terms of the Common Public License v0.5
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v05.html
  * 
  * Contributors: 
  * Rational Software - Initial API and implementation
 ***********************************************************************/
 package org.eclipse.cdt.internal.core.parser;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.cdt.core.parser.BacktrackException;
 import org.eclipse.cdt.core.parser.EndOfFileException;
 import org.eclipse.cdt.core.parser.IParser;
 import org.eclipse.cdt.core.parser.IParserLogService;
 import org.eclipse.cdt.core.parser.IScanner;
 import org.eclipse.cdt.core.parser.ISourceElementRequestor;
 import org.eclipse.cdt.core.parser.IToken;
 import org.eclipse.cdt.core.parser.ITokenDuple;
 import org.eclipse.cdt.core.parser.OffsetLimitReachedException;
 import org.eclipse.cdt.core.parser.ParseError;
 import org.eclipse.cdt.core.parser.ParserLanguage;
 import org.eclipse.cdt.core.parser.ast.ASTAccessVisibility;
 import org.eclipse.cdt.core.parser.ast.ASTClassKind;
 import org.eclipse.cdt.core.parser.ast.ASTSemanticException;
 import org.eclipse.cdt.core.parser.ast.IASTASMDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
 import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
 import org.eclipse.cdt.core.parser.ast.IASTCompletionNode;
 import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTDesignator;
 import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTExpression;
 import org.eclipse.cdt.core.parser.ast.IASTInitializerClause;
 import org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification;
 import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTNode;
 import org.eclipse.cdt.core.parser.ast.IASTOffsetableElement;
 import org.eclipse.cdt.core.parser.ast.IASTScope;
 import org.eclipse.cdt.core.parser.ast.IASTSimpleTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTTemplate;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateInstantiation;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateParameter;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateSpecialization;
 import org.eclipse.cdt.core.parser.ast.IASTTypeId;
 import org.eclipse.cdt.core.parser.ast.IASTTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDirective;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier.ClassNameType;
 import org.eclipse.cdt.core.parser.ast.IASTCompletionNode.CompletionKind;
 import org.eclipse.cdt.internal.core.parser.token.KeywordSets;
 import org.eclipse.cdt.internal.core.parser.token.TokenDuple;
 import org.eclipse.cdt.internal.core.parser.token.KeywordSets.Key;
 
 /**
  * This is our first implementation of the IParser interface, serving as a parser for
  * ANSI C and C++.
  * 
  * From time to time we will make reference to the ANSI ISO specifications.
  * 
  * @author jcamelon
  */
 public abstract class Parser extends ExpressionParser implements IParser
 {
     private static final List EMPTY_LIST = new ArrayList();
     protected ISourceElementRequestor requestor = null;
     
     
     
     /**
      * This is the standard cosntructor that we expect the Parser to be instantiated 
      * with.  
      * 
      * @param s				IScanner instance that has been initialized to the code input 
      * @param c				IParserCallback instance that will receive callbacks as we parse
      * @param quick			Are we asking for a high level parse or not? 
      */
     public Parser(
         IScanner scanner,
         ISourceElementRequestor callback,
         ParserLanguage language,
         IParserLogService log )
     {
     	super( scanner, language, log );
     	requestor = callback;
     }
     
     // counter that keeps track of the number of times Parser.parse() is called
     private static int parseCount = 0;
     
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#parse()
      */
     public boolean parse()
     {
         long startTime = System.currentTimeMillis();
         translationUnit();
         // For the debuglog to take place, you have to call
         // Util.setDebugging(true);
         // Or set debug to true in the core plugin preference 
         log.traceLog(
             "Parse "
                 + (++parseCount)
                 + ": "
                 + (System.currentTimeMillis() - startTime)
                 + "ms"
                 + (parsePassed ? "" : " - parse failure") );
         return parsePassed;
     }
         
     
     /**
      * This is the top-level entry point into the ANSI C++ grammar.  
      * 
      * translationUnit  : (declaration)*
      */
     protected void translationUnit()
     {
  
         try
         {
             compilationUnit = astFactory.createCompilationUnit();
         }
         catch (Exception e2)
         {
             return;
         }
 
		compilationUnit.enterScope( requestor );
		try {
			setCompletionValues(compilationUnit, CompletionKind.VARIABLE_TYPE, Key.DECLARATION );
		} catch (EndOfFileException e1) {
			compilationUnit.exitScope( requestor );
			return;
		}
		
         IToken lastBacktrack = null;
         IToken checkToken = null;
         while (true)
         {
             try
             {
                 checkToken = LA(1);
                 declaration(compilationUnit, null, null);
                 if (LA(1) == checkToken)
                     errorHandling();
             }
             catch (EndOfFileException e)
             {
                 // Good
                 break;
             }
             catch (BacktrackException b)
             {
                 try
                 {
                     // Mark as failure and try to reach a recovery point
                     failParse();
                     if (lastBacktrack != null && lastBacktrack == LA(1))
                     {
                         // we haven't progressed from the last backtrack
                         // try and find tne next definition
                         errorHandling();
                     }
                     else
                     {
                         // start again from here
                         lastBacktrack = LA(1);
                     }
                 }
                 catch (EndOfFileException e)
                 {
                     break;
                 }
             }
             catch (Exception e)
             {
 				failParse();
                 break;
             }
         }
         compilationUnit.exitScope( requestor );
     }
     /**
      * This function is called whenever we encounter and error that we cannot backtrack out of and we 
      * still wish to try and continue on with the parse to do a best-effort parse for our client. 
      * 
      * @throws EndOfFileException  	We can potentially hit EndOfFile here as we are skipping ahead.  
      */
     protected void errorHandling() throws EndOfFileException
     {
         failParse();
         consume();
         int depth = 0;
         while (!((LT(1) == IToken.tSEMI && depth == 0)
             || (LT(1) == IToken.tRBRACE && depth == 1)))
         {
             switch (LT(1))
             {
                 case IToken.tLBRACE :
                     ++depth;
                     break;
                 case IToken.tRBRACE :
                     --depth;
                     break;
             }
             consume();
         }
         // eat the SEMI/RBRACE as well
         consume();
     }
     /**
      * The merger of using-declaration and using-directive in ANSI C++ grammar.  
      * 
      * using-declaration:
      *	using typename? ::? nested-name-specifier unqualified-id ;
      *	using :: unqualified-id ;
      * using-directive:
      *  using namespace ::? nested-name-specifier? namespace-name ;
      * 
      * @param container		Callback object representing the scope these definitions fall into. 
      * @throws BacktrackException	request for a backtrack
      */
     protected void usingClause(IASTScope scope)
         throws EndOfFileException, BacktrackException
     {
         IToken firstToken = consume(IToken.t_using);
         setCompletionValues(scope, CompletionKind.TYPE_REFERENCE, Key.POST_USING );
         
         if (LT(1) == IToken.t_namespace)
         {
             // using-directive
             consume(IToken.t_namespace);
             
             setCompletionValues(scope, CompletionKind.NAMESPACE_REFERENCE, Key.EMPTY );
             // optional :: and nested classes handled in name
             TokenDuple duple = null;
             if (LT(1) == IToken.tIDENTIFIER || LT(1) == IToken.tCOLONCOLON)
                 duple = name(scope, CompletionKind.NAMESPACE_REFERENCE);
             else
                 throw backtrack;
             if (LT(1) == IToken.tSEMI)
             {
                 IToken last = consume(IToken.tSEMI);
                 IASTUsingDirective astUD = null; 
                 
                 try
                 {
                     astUD = astFactory.createUsingDirective(scope, duple, firstToken.getOffset(), firstToken.getLineNumber(), last.getEndOffset(), last.getLineNumber());
                 }
                 catch (Exception e1)
                 {
                     throw backtrack;
                 }
                 astUD.acceptElement(requestor);
                 return;
             }
             else
             {
                 throw backtrack;
             }
         }
         else
         {
             boolean typeName = false;
             setCompletionValues(scope, CompletionKind.TYPE_REFERENCE, Key.POST_USING );
             
             if (LT(1) == IToken.t_typename)
             {
                 typeName = true;
                 consume(IToken.t_typename);
                 
             }
 
             setCompletionValues(scope, CompletionKind.TYPE_REFERENCE, Key.NAMESPACE_ONLY );
             TokenDuple name = null;
             if (LT(1) == IToken.tIDENTIFIER || LT(1) == IToken.tCOLONCOLON)
             {
                 //	optional :: and nested classes handled in name
                 name = name(scope, CompletionKind.TYPE_REFERENCE);
             }
             else
             {
                 throw backtrack;
             }
             if (LT(1) == IToken.tSEMI)
             {
                 IToken last = consume(IToken.tSEMI);
                 IASTUsingDeclaration declaration = null;
                 try
                 {
                     declaration =
                         astFactory.createUsingDeclaration(
                             scope,
                             typeName,
                             name,
                             firstToken.getOffset(),
                             firstToken.getLineNumber(), last.getEndOffset(), last.getLineNumber());
                 }
                 catch (Exception e1)
                 {
                     throw backtrack;
                 }
                 declaration.acceptElement( requestor );
                 setCompletionValues(scope, getCompletionKindForDeclaration(scope, null), Key.DECLARATION );
             }
             else
             {
                 throw backtrack;
             }
         }
     }
     /**
      * Implements Linkage specification in the ANSI C++ grammar. 
      * 
      * linkageSpecification
      * : extern "string literal" declaration
      * | extern "string literal" { declaration-seq } 
      * 
      * @param container Callback object representing the scope these definitions fall into.
      * @throws BacktrackException	request for a backtrack
      */
     protected void linkageSpecification(IASTScope scope)
         throws EndOfFileException, BacktrackException
     {
         IToken firstToken = consume(IToken.t_extern);
         if (LT(1) != IToken.tSTRING)
             throw backtrack;
         IToken spec = consume(IToken.tSTRING);
   
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             IASTLinkageSpecification linkage;
             try
             {
                 linkage =
                     astFactory.createLinkageSpecification(
                         scope,
                         spec.getImage(),
                         firstToken.getOffset(), firstToken.getLineNumber());
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
             
             linkage.enterScope( requestor );    
             linkageDeclarationLoop : while (LT(1) != IToken.tRBRACE)
             {
                 IToken checkToken = LA(1);
                 switch (LT(1))
                 {
                     case IToken.tRBRACE :
                         consume(IToken.tRBRACE);
                         break linkageDeclarationLoop;
                     default :
                         try
                         {
                             declaration(linkage, null, null);
                         }
                         catch (BacktrackException bt)
                         {
                             failParse();
                             if (checkToken == LA(1))
                                 errorHandling();
                         }
                 }
                 if (checkToken == LA(1))
                     errorHandling();
             }
             // consume the }
             IToken lastToken = consume();
             linkage.setEndingOffsetAndLineNumber(lastToken.getEndOffset(), lastToken.getLineNumber());
             linkage.exitScope( requestor );
         }
         else // single declaration
             {
             IASTLinkageSpecification linkage;
             try
             {
                 linkage =
                     astFactory.createLinkageSpecification(
                         scope,
                         spec.getImage(),
                         firstToken.getOffset(), firstToken.getLineNumber());
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
 			linkage.enterScope( requestor );
             declaration(linkage, null, null);
 			linkage.exitScope( requestor );
         }
     }
     /**
      * 
      * Represents the emalgamation of template declarations, template instantiations and 
      * specializations in the ANSI C++ grammar.  
      * 
      * template-declaration:	export? template < template-parameter-list > declaration
      * explicit-instantiation:	template declaration
      * explicit-specialization:	template <> declaration
      *  
      * @param container			Callback object representing the scope these definitions fall into.
      * @throws BacktrackException		request for a backtrack
      */
     protected void templateDeclaration(IASTScope scope)
         throws EndOfFileException, BacktrackException
     {
         IToken firstToken = null;
         boolean exported = false; 
         if (LT(1) == IToken.t_export)
         {
         	exported = true;
             firstToken = consume(IToken.t_export);
             consume(IToken.t_template);
         }
         else
             firstToken = consume(IToken.t_template);
         if (LT(1) != IToken.tLT)
         {
             // explicit-instantiation
             IASTTemplateInstantiation templateInstantiation;
             try
             {
                 templateInstantiation =
                     astFactory.createTemplateInstantiation(
                         scope,
                         firstToken.getOffset(), firstToken.getLineNumber());
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
             templateInstantiation.enterScope( requestor );
             declaration(scope, templateInstantiation, null);
             templateInstantiation.setEndingOffsetAndLineNumber(lastToken.getEndOffset(), lastToken.getLineNumber());
 			templateInstantiation.exitScope( requestor );
  
             return;
         }
         else
         {
             consume(IToken.tLT);
             if (LT(1) == IToken.tGT)
             {
                 consume(IToken.tGT);
                 // explicit-specialization
                 
                 IASTTemplateSpecialization templateSpecialization;
                 try
                 {
                     templateSpecialization =
                         astFactory.createTemplateSpecialization(
                             scope,
                             firstToken.getOffset(), firstToken.getLineNumber());
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
 				templateSpecialization.enterScope(requestor);
                 declaration(scope, templateSpecialization, null);
                 templateSpecialization.setEndingOffsetAndLineNumber(
                     lastToken.getEndOffset(), lastToken.getLineNumber());
                 templateSpecialization.exitScope(requestor);
                 return;
             }
         }
         
         try
         {
             List parms = templateParameterList(scope);
             consume(IToken.tGT);
             IASTTemplateDeclaration templateDecl;
             try
             {
                 templateDecl =
                     astFactory.createTemplateDeclaration(
                         scope,
                         parms,
                         exported,
                         firstToken.getOffset(), firstToken.getLineNumber());
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
             templateDecl.enterScope( requestor );
             declaration(scope, templateDecl, null );
 			templateDecl.setEndingOffsetAndLineNumber(
 				lastToken.getEndOffset(), lastToken.getLineNumber() );
 			templateDecl.exitScope( requestor );
             
         }
         catch (BacktrackException bt)
         {
             throw bt;
         }
     }
     /**
      * 
      * 
      * 
     	 * template-parameter-list:	template-parameter
      *							template-parameter-list , template-parameter
      * template-parameter:		type-parameter
      *							parameter-declaration
      * type-parameter:			class identifier?
      *							class identifier? = type-id
      * 							typename identifier?
      * 							typename identifier? = type-id
      *							template < template-parameter-list > class identifier?
      *							template < template-parameter-list > class identifier? = id-expression
      * template-id:				template-name < template-argument-list?>
      * template-name:			identifier
      * template-argument-list:	template-argument
      *							template-argument-list , template-argument
      * template-argument:		assignment-expression
      *							type-id
      *							id-expression
      *
      * @param templateDeclaration		Callback's templateDeclaration which serves as a scope to this list.  
      * @throws BacktrackException				request for a backtrack
      */
     protected List templateParameterList(IASTScope scope)
         throws BacktrackException, EndOfFileException
     {
         // if we have gotten this far then we have a true template-declaration
         // iterate through the template parameter list
         List returnValue = new ArrayList();
  
         for (;;)
         {
             if (LT(1) == IToken.tGT)
                 return returnValue;
             if (LT(1) == IToken.t_class || LT(1) == IToken.t_typename)
             {
                 IASTTemplateParameter.ParamKind kind =
                     (consume().getType() == IToken.t_class)
                         ? IASTTemplateParameter.ParamKind.CLASS
                         : IASTTemplateParameter.ParamKind.TYPENAME;
 				
 				IToken id = null;
 				IASTTypeId typeId = null;
                 try
                 {
                     if (LT(1) == IToken.tIDENTIFIER) // optional identifier
                     {
                         id = identifier();
                         
                         if (LT(1) == IToken.tASSIGN) // optional = type-id
                         {
                             consume(IToken.tASSIGN);
                             typeId = typeId(scope, false); // type-id
                         }
                     }
 
                 }
                 catch (BacktrackException bt)
                 {
                     throw bt;
                 }
 				try
                 {
                     returnValue.add(
                     	astFactory.createTemplateParameter(
                     		kind,
                     		( id == null )? "" : id.getImage(),
                     		(typeId == null) ? null : typeId.getTypeOrClassName(),
                     		null,
                     		null));
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
 
             }
             else if (LT(1) == IToken.t_template)
             {
                 consume(IToken.t_template);
                 consume(IToken.tLT);
 
                 List subResult = templateParameterList(scope);
                 consume(IToken.tGT);
                 consume(IToken.t_class);
                 IToken optionalId = null;
                 IASTTypeId optionalTypeId = null;
                 if (LT(1) == IToken.tIDENTIFIER) // optional identifier
                 {
                     optionalId = identifier();
    
                     if (LT(1) == IToken.tASSIGN) // optional = type-id
                     {
                         consume(IToken.tASSIGN);
                         optionalTypeId = typeId(scope, false);
     
                     }
                 }
  
                 try
                 {
                     returnValue.add(
                         astFactory.createTemplateParameter(
                             IASTTemplateParameter.ParamKind.TEMPLATE_LIST,
                             ( optionalId == null )? "" : optionalId.getImage(),
                             ( optionalTypeId == null )  ? "" : optionalTypeId.toString(),
                             null,
                             subResult));
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
             }
             else if (LT(1) == IToken.tCOMMA)
             {
                 consume(IToken.tCOMMA);
                 continue;
             }
             else
             {
                 ParameterCollection c = new ParameterCollection();
                 parameterDeclaration(c, scope);
                 DeclarationWrapper wrapper =
                     (DeclarationWrapper)c.getParameters().get(0);
                 Declarator declarator =
                     (Declarator)wrapper.getDeclarators().next();
                 try
                 {
                     returnValue.add(
                         astFactory.createTemplateParameter(
                             IASTTemplateParameter.ParamKind.PARAMETER,
                             null,
                             null,
                             astFactory.createParameterDeclaration(
                                 wrapper.isConst(),
                                 wrapper.isVolatile(),
                                 wrapper.getTypeSpecifier(),
                                 declarator.getPointerOperators(),
                                 declarator.getArrayModifiers(),
                                 null, null, declarator.getName() == null
                                                 ? ""
                                                 : declarator.getName(), declarator.getInitializerClause(), wrapper.getStartingOffset(), wrapper.getStartingLine(), declarator.getNameStartOffset(), declarator.getNameEndOffset(), declarator.getNameLine(), wrapper.getEndOffset(), wrapper.getEndLine()),
                             null));
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
             }
         }
     }
     /**
      * The most abstract construct within a translationUnit : a declaration.  
      * 
      * declaration
      * : {"asm"} asmDefinition
      * | {"namespace"} namespaceDefinition
      * | {"using"} usingDeclaration
      * | {"export"|"template"} templateDeclaration
      * | {"extern"} linkageSpecification
      * | simpleDeclaration
      * 
      * Notes:
      * - folded in blockDeclaration
      * - merged alternatives that required same LA
      *   - functionDefinition into simpleDeclaration
      *   - namespaceAliasDefinition into namespaceDefinition
      *   - usingDirective into usingDeclaration
      *   - explicitInstantiation and explicitSpecialization into
      *       templateDeclaration
      * 
      * @param container		IParserCallback object which serves as the owner scope for this declaration.  
      * @throws BacktrackException	request a backtrack
      */
     protected void declaration(
         IASTScope scope,
         IASTTemplate ownerTemplate, CompletionKind overideKind)
         throws EndOfFileException, BacktrackException
     {
     	IASTCompletionNode.CompletionKind kind = getCompletionKindForDeclaration(scope, overideKind);
     	setCompletionValues(scope, kind, Key.DECLARATION );
 
     	switch (LT(1))
         {
             case IToken.t_asm :
                 IToken first = consume(IToken.t_asm);
                 setCompletionValues( scope, CompletionKind.NO_SUCH_KIND, Key.EMPTY );
                 consume(IToken.tLPAREN);
                 String assembly = consume(IToken.tSTRING).getImage();
                 consume(IToken.tRPAREN);
                 IToken last = consume(IToken.tSEMI);
                 
                 IASTASMDefinition asmDefinition;
                 try
                 {
                     asmDefinition =
                         astFactory.createASMDefinition(
                             scope,
                             assembly,
                             first.getOffset(),
                             first.getLineNumber(), last.getEndOffset(), last.getLineNumber());
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
                 // if we made it this far, then we have all we need 
                 // do the callback
  				asmDefinition.acceptElement(requestor);
  				setCompletionValues(scope, kind, Key.DECLARATION );
                 return;
             case IToken.t_namespace :
                 namespaceDefinition(scope);
                 return;
             case IToken.t_using :
                 usingClause(scope);
                 return;
             case IToken.t_export :
             case IToken.t_template :
                 templateDeclaration(scope);
                 return;
             case IToken.t_extern :
                 if (LT(2) == IToken.tSTRING)
                 {
                     linkageSpecification(scope);
                     return;
                 }
             default :
                 simpleDeclarationStrategyUnion(scope, ownerTemplate, overideKind );
         }
     	setCompletionValues(scope, kind, Key.DECLARATION );
     	checkEndOfFile();
     }
     
     /**
 	 * @param scope
 	 * @return
 	 */
 	protected IASTCompletionNode.CompletionKind getCompletionKindForDeclaration(IASTScope scope, CompletionKind overide) {
 		return null;
 	}
 	
 	protected void simpleDeclarationStrategyUnion(
         IASTScope scope,
         IASTTemplate ownerTemplate, CompletionKind overide)
         throws EndOfFileException, BacktrackException
     {
         IToken mark = mark();
         
         try
         {
             simpleDeclaration(
                 SimpleDeclarationStrategy.TRY_CONSTRUCTOR,
                 scope,
                 ownerTemplate, overide, false);
             // try it first with the original strategy
         }
         catch (BacktrackException bt)
         {
             // did not work 
             backup(mark);
             
             try
             {  
             	simpleDeclaration(
                 	SimpleDeclarationStrategy.TRY_FUNCTION,
 	                scope,
     	            ownerTemplate, overide, false);
             }
             catch( BacktrackException bt2 )
             {
             	backup( mark ); 
 
 				try
 				{
 					simpleDeclaration(
 						SimpleDeclarationStrategy.TRY_VARIABLE,
 						scope,
 						ownerTemplate, overide, false);
 				}
 				catch( BacktrackException b3 )
 				{
 					backup( mark );
 					throw b3;
 				}
             }
         }
     }
     /**
      *  Serves as the namespace declaration portion of the ANSI C++ grammar.  
      * 
      * 	namespace-definition:
      *		namespace identifier { namespace-body } | namespace { namespace-body }
      *	 namespace-body:
      *		declaration-seq?
      * @param container		IParserCallback object which serves as the owner scope for this declaration.  
      * @throws BacktrackException	request a backtrack
     
      */
     protected void namespaceDefinition(IASTScope scope)
         throws BacktrackException, EndOfFileException
     {
         IToken first = consume(IToken.t_namespace);
  
         IASTCompletionNode.CompletionKind kind = getCompletionKindForDeclaration(scope, null);
         
         setCompletionValues(scope,CompletionKind.USER_SPECIFIED_NAME, Key.EMPTY );
         IToken identifier = null;
         // optional name 		
         if (LT(1) == IToken.tIDENTIFIER)
             identifier = identifier();
         
         if (LT(1) == IToken.tLBRACE)
         {
             consume();
             IASTNamespaceDefinition namespaceDefinition = null;
             try
             {
                 namespaceDefinition = 
                     astFactory.createNamespaceDefinition(
                         scope,
                         (identifier == null ? "" : identifier.getImage()),
                         first.getOffset(),
                         first.getLineNumber(), 
                         (identifier == null ? first.getOffset() : identifier.getOffset()), 
 						(identifier == null ? first.getEndOffset() : identifier.getEndOffset() ),  
 						(identifier == null ? first.getLineNumber() : identifier.getLineNumber() ));
             }
             catch (Exception e1)
             {
                 throw backtrack;
             }
             namespaceDefinition.enterScope( requestor );
             setCompletionValues(scope,CompletionKind.VARIABLE_TYPE, Key.DECLARATION );
             namespaceDeclarationLoop : while (LT(1) != IToken.tRBRACE)
             {
                 IToken checkToken = LA(1);
                 switch (LT(1))
                 {
                     case IToken.tRBRACE :
                         //consume(Token.tRBRACE);
                         break namespaceDeclarationLoop;
                     default :
                         try
                         {
                             declaration(namespaceDefinition, null, null);
                         }
                         catch (BacktrackException bt)
                         {
                             failParse();
                             if (checkToken == LA(1))
                                 errorHandling();
                         }
                 }
                 if (checkToken == LA(1))
                     errorHandling();
             }
             setCompletionValues(scope, CompletionKind.NO_SUCH_KIND,Key.EMPTY );
             // consume the }
             IToken last = consume(IToken.tRBRACE);
  
             namespaceDefinition.setEndingOffsetAndLineNumber(
                 last.getOffset() + last.getLength(), last.getLineNumber());
             setCompletionValues(scope, kind, Key.DECLARATION );
             namespaceDefinition.exitScope( requestor );
         }
         else if( LT(1) == IToken.tASSIGN )
         {
         	setCompletionValues(scope, CompletionKind.NO_SUCH_KIND,Key.EMPTY);
         	consume( IToken.tASSIGN );
         	
 			if( identifier == null )
 				throw backtrack;
 
         	ITokenDuple duple = name(scope, CompletionKind.NAMESPACE_REFERENCE);
         	consume( IToken.tSEMI );
         	setCompletionValues(scope, kind, Key.DECLARATION );
         	try
             {
                 astFactory.createNamespaceAlias( 
                 	scope, identifier.getImage(), duple, first.getOffset(), 
                 	first.getLineNumber(), identifier.getOffset(), identifier.getEndOffset(), identifier.getLineNumber(), duple.getLastToken().getEndOffset(), duple.getLastToken().getLineNumber() );
             }
             catch (Exception e1)
             {
                 throw backtrack;
             }
         }
         else
         {
             throw backtrack;
         }
     }
     /**
      * Serves as the catch-all for all complicated declarations, including function-definitions.  
      * 
      * simpleDeclaration
      * : (declSpecifier)* (initDeclarator ("," initDeclarator)*)? 
      *     (";" | { functionBody }
      * 
      * Notes:
      * - append functionDefinition stuff to end of this rule
      * 
      * To do:
      * - work in functionTryBlock
      * 
      * @param container			IParserCallback object which serves as the owner scope for this declaration.
      * @param tryConstructor	true == take strategy1 (constructor ) : false == take strategy 2 ( pointer to function) 
      * @throws BacktrackException		request a backtrack
      */
     protected void simpleDeclaration(
         SimpleDeclarationStrategy strategy,
         IASTScope scope,
         IASTTemplate ownerTemplate, CompletionKind overideKind, boolean fromCatchHandler)
         throws BacktrackException, EndOfFileException
     {
     	IToken firstToken = LA(1);
     	if( firstToken.getType()  == IToken.tLBRACE ) throw backtrack;
         DeclarationWrapper sdw =
             new DeclarationWrapper(scope, firstToken.getOffset(), firstToken.getLineNumber(), ownerTemplate);
         firstToken = null; // necessary for scalability
 
         setCompletionValues( scope, getCompletionKindForDeclaration(scope, overideKind), Key.DECL_SPECIFIER_SEQUENCE );
         declSpecifierSeq(sdw, false, strategy == SimpleDeclarationStrategy.TRY_CONSTRUCTOR );
         if (sdw.getTypeSpecifier() == null && sdw.getSimpleType() != IASTSimpleTypeSpecifier.Type.UNSPECIFIED )
             try
             {
                 sdw.setTypeSpecifier(
                     astFactory.createSimpleTypeSpecifier(
                         scope,
                         sdw.getSimpleType(),
                         sdw.getName(),
                         sdw.isShort(),
                         sdw.isLong(),
                         sdw.isSigned(),
                         sdw.isUnsigned(), sdw.isTypeNamed(), sdw.isComplex(), sdw.isImaginary()));
             }
             catch (Exception e1)
             {
                 throw backtrack;
             }
         
         Declarator declarator = null;
         if (LT(1) != IToken.tSEMI)
         {
             declarator = initDeclarator(sdw, strategy);
                 
             while (LT(1) == IToken.tCOMMA)
             {
                 consume();
                 initDeclarator(sdw, strategy);
             }
         }
 
         boolean hasFunctionBody = false;
         boolean hasFunctionTryBlock = false;
         boolean consumedSemi = false;
         
         switch (LT(1))
         {
             case IToken.tSEMI :
                 consume(IToken.tSEMI);
                 consumedSemi = true;
                 break;
             case IToken.t_try : 
             	consume( IToken.t_try );
             	if( LT(1) == IToken.tCOLON )
             		ctorInitializer( declarator );
         		hasFunctionTryBlock = true;
         		declarator.setFunctionTryBlock( true );    	
             	break;       	
             case IToken.tCOLON :
                 ctorInitializer(declarator);
                 break;
             case IToken.tLBRACE: 
             	break;
             case IToken.tRPAREN:
             	if( ! fromCatchHandler )
             		throw backtrack;
             	break;
             default: 
             	throw backtrack;
         }
         
         if( ! consumedSemi )
 		{        
 	        if( LT(1) == IToken.tLBRACE )
 	        {
 	            declarator.setHasFunctionBody(true);
 	            hasFunctionBody = true;
 	        }
 	        
 	        if( fromCatchHandler )
 	        	return;
 	        
 	        if( hasFunctionTryBlock && ! hasFunctionBody )
 	        	throw backtrack;
 		}
 		        
         List l = null; 
         try
         {
             l = sdw.createASTNodes(astFactory);
         }
         catch (ASTSemanticException e)
         {
 			throw backtrack;
         }
         Iterator i = l.iterator();
         if (hasFunctionBody && l.size() != 1)
         {
             throw backtrack; //TODO Should be an IProblem
         }
         if (i.hasNext()) // no need to do this unless we have a declarator
         {
             if (!hasFunctionBody)
             {
                 while (i.hasNext())
                 {
                     IASTDeclaration declaration = (IASTDeclaration)i.next();
                     ((IASTOffsetableElement)declaration).setEndingOffsetAndLineNumber(
                         lastToken.getEndOffset(), lastToken.getLineNumber());
                     declaration.acceptElement( requestor );
                 }
             }
             else
             {
                 IASTDeclaration declaration = (IASTDeclaration)i.next();
                 declaration.enterScope( requestor );
    			
    				if ( !( declaration instanceof IASTScope ) ) 
    					throw backtrack;
  
 				handleFunctionBody((IASTScope)declaration );
 				((IASTOffsetableElement)declaration).setEndingOffsetAndLineNumber(
 					lastToken.getEndOffset(), lastToken.getLineNumber());
   
   				declaration.exitScope( requestor );
   				
   				if( hasFunctionTryBlock )
 					catchHandlerSequence( scope );
   				
             }
         }
         else
         {
             try
             {
                 astFactory
                     .createTypeSpecDeclaration(
                         sdw.getScope(),
                         sdw.getTypeSpecifier(),
                         ownerTemplate,
                         sdw.getStartingOffset(),
                         sdw.getStartingLine(), lastToken.getEndOffset(), lastToken.getLineNumber())
                     .acceptElement(requestor);
             }
             catch (Exception e1)
             {
                 throw backtrack;
             }
         }
         
     }
     protected abstract void handleFunctionBody(IASTScope scope) throws BacktrackException, EndOfFileException;
 
     protected void skipOverCompoundStatement() throws BacktrackException, EndOfFileException
     {
         // speed up the parser by skiping the body
         // simply look for matching brace and return
         consume(IToken.tLBRACE);
         int depth = 1;
         while (depth > 0)
         {
             switch (consume().getType())
             {
                 case IToken.tRBRACE :
                     --depth;
                     break;
                 case IToken.tLBRACE :
                     ++depth;
                     break;
             }
         }
     }
     /**
      * This method parses a constructor chain 
      * ctorinitializer:	 : meminitializerlist
      * meminitializerlist: meminitializer | meminitializer , meminitializerlist
      * meminitializer: meminitializerid | ( expressionlist? ) 
      * meminitializerid:	::? nestednamespecifier?
      * 						classname
      * 						identifier
      * @param declarator	IParserCallback object that represents the declarator (constructor) that owns this initializer
      * @throws BacktrackException	request a backtrack
      */
     protected void ctorInitializer(Declarator d )
         throws EndOfFileException, BacktrackException
     {
         consume(IToken.tCOLON);
         IASTScope scope = d.getDeclarationWrapper().getScope();
         try
         {
             for (;;)
             {
                 if (LT(1) == IToken.tLBRACE)
                     break;
 
                 
                 ITokenDuple duple = name(scope, CompletionKind.SINGLE_NAME_REFERENCE );
 
                 consume(IToken.tLPAREN);
                 IASTExpression expressionList = null;
 
                 expressionList = expression(d.getDeclarationWrapper().getScope());
 
                 consume(IToken.tRPAREN);
 
                 try
                 {
                     d.addConstructorMemberInitializer(
                         astFactory.createConstructorMemberInitializer(
                             d.getDeclarationWrapper().getScope(),
                             duple, expressionList));
                 }
                 catch (Exception e1)
                 {
                     throw backtrack;
                 }
                 if (LT(1) == IToken.tLBRACE)
                     break;
                 consume(IToken.tCOMMA);
             }
         }
         catch (BacktrackException bt)
         {
  
             throw backtrack;
         }
 
     }
     /**
      * This routine parses a parameter declaration 
      * 
      * @param containerObject	The IParserCallback object representing the parameterDeclarationClause owning the parm. 
      * @throws BacktrackException		request a backtrack
      */
     protected void parameterDeclaration(
         IParameterCollection collection, IASTScope scope)
         throws BacktrackException, EndOfFileException
     {
         IToken current = LA(1);
         
         DeclarationWrapper sdw =
             new DeclarationWrapper(scope, current.getOffset(), current.getLineNumber(), null);
         declSpecifierSeq(sdw, true, false);
         if (sdw.getTypeSpecifier() == null
             && sdw.getSimpleType()
                 != IASTSimpleTypeSpecifier.Type.UNSPECIFIED)
             try
             {
                 sdw.setTypeSpecifier(
                     astFactory.createSimpleTypeSpecifier(
                         scope,
                         sdw.getSimpleType(),
                         sdw.getName(),
                         sdw.isShort(),
                         sdw.isLong(),
                         sdw.isSigned(),
                         sdw.isUnsigned(), sdw.isTypeNamed(), sdw.isComplex(), sdw.isImaginary()));
             }
             catch (ASTSemanticException e)
             {
                 throw backtrack;
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
         
         setCompletionValues(scope,CompletionKind.USER_SPECIFIED_NAME,Key.EMPTY );     
         if (LT(1) != IToken.tSEMI)
            initDeclarator(sdw, SimpleDeclarationStrategy.TRY_FUNCTION );
  
  		if( lastToken != null )
  			sdw.setEndingOffsetAndLineNumber( lastToken.getEndOffset(), lastToken.getLineNumber() );
  			
         if (current == LA(1))
             throw backtrack;
         collection.addParameter(sdw);
     }
     /**
      * This class represents the state and strategy for parsing declarationSpecifierSequences
      */
     private class Flags
     {
         private boolean encounteredTypename = false;
         // have we encountered a typeName yet?
         private boolean encounteredRawType = false;
         // have we encountered a raw type yet?
         private final boolean parm;
         // is this for a simpleDeclaration or parameterDeclaration?
         private final boolean constructor;
         // are we attempting the constructor strategy?
         public Flags(boolean parm, boolean c)
         {
             this.parm = parm;
             constructor = c;
         }
         /**
          * @return	true if we have encountered a simple type up to this point, false otherwise
          */
         public boolean haveEncounteredRawType()
         {
             return encounteredRawType;
         }
         /**
          * @return  true if we have encountered a typename up to this point, false otherwise
          */
         public boolean haveEncounteredTypename()
         {
             return encounteredTypename;
         }
         /**
          * @param b - set to true if we encounter a raw type (int, short, etc.)
          */
         public void setEncounteredRawType(boolean b)
         {
             encounteredRawType = b;
         }
         /**
          * @param b - set to true if we encounter a typename
          */
         public void setEncounteredTypename(boolean b)
         {
             encounteredTypename = b;
         }
         /**
          * @return true if we are parsing for a ParameterDeclaration
          */
         public boolean isForParameterDeclaration()
         {
             return parm;
         }
         /**
          * @return whether or not we are attempting the constructor strategy or not 
          */
         public boolean isForConstructor()
         {
             return constructor;
         }
     }
     /**
      * @param flags            input flags that are used to make our decision 
      * @return                 whether or not this looks like a constructor (true or false)
      * @throws EndOfFileException       we could encounter EOF while looking ahead
      */
     private boolean lookAheadForConstructorOrConversion(Flags flags, DeclarationWrapper sdw )
         throws EndOfFileException
     {
         if (flags.isForParameterDeclaration())
             return false;
         if (LT(2) == IToken.tLPAREN && flags.isForConstructor())
             return true;
         
         IToken mark = mark(); 
         Declarator d = new Declarator( sdw );
         try
         {
             consumeTemplatedOperatorName( d );
         }
         catch (BacktrackException e)
         {
             backup( mark ); 
             return false;
         }
         
         ITokenDuple duple = d.getNameDuple(); 
        	if( duple == null )
        	{
        		backup( mark ); 
        		return false; 
        	} 
        	
        	int lastColon = duple.findLastTokenType(IToken.tCOLON);
        	if( lastColon == -1  ) 
        	{
        		int lt1 = LT(1);
        		backup( mark );
        		return flags.isForConstructor() && (lt1 == IToken.tLPAREN);
        	} 
        	
        	IToken className = null;
        	int index = lastColon - 1;
         if( duple.getToken( index ).getType() == IToken.tGT )
        	{
        		int depth = -1; 
        		while( depth == -1 )
        		{
        			if( duple.getToken( --index ).getType() == IToken.tLT )
        				++depth;
        		}
        		className = duple.getToken( index );
        	}
        	
        	boolean result = className.getImage().equals( duple.getLastToken());
        	backup( mark );
        	return result;
     }
     /**
      * @param flags			input flags that are used to make our decision 
      * @return				whether or not this looks like a a declarator follows
      * @throws EndOfFileException	we could encounter EOF while looking ahead
      */
     private boolean lookAheadForDeclarator(Flags flags) throws EndOfFileException
     {
         return flags.haveEncounteredTypename()
             && ((LT(2) != IToken.tIDENTIFIER
                 || (LT(3) != IToken.tLPAREN && LT(3) != IToken.tASSIGN))
                 && !LA(2).isPointer());
     }
     /**
      * This function parses a declaration specifier sequence, as according to the ANSI C++ spec. 
      * 
      * declSpecifier
      * : "auto" | "register" | "static" | "extern" | "mutable"
      * | "inline" | "virtual" | "explicit"
      * | "char" | "wchar_t" | "bool" | "short" | "int" | "long"
      * | "signed" | "unsigned" | "float" | "double" | "void"
      * | "const" | "volatile"
      * | "friend" | "typedef"
      * | ("typename")? name
      * | {"class"|"struct"|"union"} classSpecifier
      * | {"enum"} enumSpecifier
      * 
      * Notes:
      * - folded in storageClassSpecifier, typeSpecifier, functionSpecifier
      * - folded elaboratedTypeSpecifier into classSpecifier and enumSpecifier
      * - find template names in name
      * 
      * @param decl				IParserCallback object representing the declaration that owns this specifier sequence
      * @param parm				Is this for a parameter declaration (true) or simple declaration (false)
      * @param tryConstructor	true for constructor, false for pointer to function strategy
      * @throws BacktrackException		request a backtrack
      */
     protected void declSpecifierSeq(
         DeclarationWrapper sdw,
         boolean parm,
         boolean tryConstructor )
         throws BacktrackException, EndOfFileException
     {
         Flags flags = new Flags(parm, tryConstructor);
         IToken typeNameBegin = null;
         IToken typeNameEnd = null;
         declSpecifiers : for (;;)
         {
             switch (LT(1))
             {
                 case IToken.t_inline :
                 	consume(); 
                     sdw.setInline(true);
                     break;
                 case IToken.t_auto :
 					consume(); 
                     sdw.setAuto(true);
                     break;
                 case IToken.t_register :
                     sdw.setRegister(true);
 					consume(); 
     	            break;
                 case IToken.t_static :
                     sdw.setStatic(true);
 					consume(); 
     		        break;
                 case IToken.t_extern :
                     sdw.setExtern(true);
 					consume(); 
                     break;
                 case IToken.t_mutable :
                     sdw.setMutable(true);
 					consume(); 
                     break;
                 case IToken.t_virtual :
                     sdw.setVirtual(true);
 					consume(); 
                     break;
                 case IToken.t_explicit :
                     sdw.setExplicit(true);
 					consume(); 
                     break;
                 case IToken.t_typedef :
                     sdw.setTypedef(true);
 					consume(); 
                     break;
                 case IToken.t_friend :
                     sdw.setFriend(true);
 					consume(); 
                     break;
                 case IToken.t_const :
                     sdw.setConst(true);
 					consume(); 
                     break;
                 case IToken.t_volatile :
                     sdw.setVolatile(true);
 					consume(); 
                     break;
                 case IToken.t_signed :
                     sdw.setSigned(true);
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
 					sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.INT);
                     break;
                 case IToken.t_unsigned :
                     sdw.setUnsigned(true);
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
 					sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.INT);
                     break;
                 case IToken.t_short :
                     sdw.setShort(true);
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
 					sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.INT);
                     break;
                 case IToken.t_long :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
 					sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.INT);
                     sdw.setLong(true);
                     break;
                 case IToken.t__Complex :
 					consume( IToken.t__Complex );
 					if (typeNameBegin == null)
 						typeNameBegin = LA(1);
 					typeNameEnd = LA(1);
 					sdw.setComplex( true );
 					break;
 				case IToken.t__Imaginary :
 					consume( IToken.t__Imaginary );
 					if (typeNameBegin == null)
 						typeNameBegin = LA(1);
 					typeNameEnd = LA(1);
 					sdw.setImaginary( true );
 					break;                
                 case IToken.t_char :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.CHAR);
                     break;
                 case IToken.t_wchar_t :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(
                         IASTSimpleTypeSpecifier.Type.WCHAR_T);
                     break;
                 case IToken.t_bool :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.BOOL);
                     break;
                 case IToken.t__Bool: 
 					if (typeNameBegin == null)
 						typeNameBegin = LA(1);
 					typeNameEnd = LA(1);
 					flags.setEncounteredRawType(true);
 					consume();
 					sdw.setSimpleType(IASTSimpleTypeSpecifier.Type._BOOL);
 					break;                
                 case IToken.t_int :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.INT);
                     break;					
                 case IToken.t_float :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.FLOAT);
                     break;
                 case IToken.t_double :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(
                         IASTSimpleTypeSpecifier.Type.DOUBLE);
                     break;
                 case IToken.t_void :
                     if (typeNameBegin == null)
                         typeNameBegin = LA(1);
                     typeNameEnd = LA(1);
                     flags.setEncounteredRawType(true);
 					consume();
                     sdw.setSimpleType(IASTSimpleTypeSpecifier.Type.VOID);
                     break;
                 case IToken.t_typename :
                     sdw.setTypenamed(true);
                     consume(IToken.t_typename ); 
                     IToken first = LA(1);
                     IToken last = null;
                     last = name(sdw.getScope(), CompletionKind.TYPE_REFERENCE).getLastToken();
                     if (LT(1) == IToken.t_template)
                     {
                         consume(IToken.t_template);
                         last = templateId(sdw.getScope(), CompletionKind.SINGLE_NAME_REFERENCE );
                     }
                     ITokenDuple duple = new TokenDuple(first, last);
                     sdw.setTypeName(duple);
       
                     break;
                 case IToken.tCOLONCOLON :
                     consume(IToken.tCOLONCOLON);
                 case IToken.tIDENTIFIER :
                     // TODO - Kludgy way to handle constructors/destructors
                     if (flags.haveEncounteredRawType())
                     {
                         if (typeNameBegin != null)
                             sdw.setTypeName(
                                 new TokenDuple(typeNameBegin, typeNameEnd));
                         return;
                     }
                     if (parm && flags.haveEncounteredTypename())
                     {
                         if (typeNameBegin != null)
                             sdw.setTypeName(
                                 new TokenDuple(typeNameBegin, typeNameEnd));
                         return;
                     }
                     if (lookAheadForConstructorOrConversion(flags, sdw))
                     {
                         if (typeNameBegin != null)
                             sdw.setTypeName(
                                 new TokenDuple(typeNameBegin, typeNameEnd));
                         return;
                     }
                     if (lookAheadForDeclarator(flags))
                     {
                         if (typeNameBegin != null)
                             sdw.setTypeName(
                                 new TokenDuple(typeNameBegin, typeNameEnd));
  
                         return;
                     }
  
                     ITokenDuple d = name(sdw.getScope(), CompletionKind.TYPE_REFERENCE );
                     sdw.setTypeName(d);
                     sdw.setSimpleType( IASTSimpleTypeSpecifier.Type.CLASS_OR_TYPENAME ); 
                     flags.setEncounteredTypename(true);
                     break;
                 case IToken.t_class :
                 case IToken.t_struct :
                 case IToken.t_union :
                     try
                     {
                         classSpecifier(sdw);
 						flags.setEncounteredTypename(true);
                         break;
                     }
                     catch (BacktrackException bt)
                     {
                         elaboratedTypeSpecifier(sdw);
                         flags.setEncounteredTypename(true);
                         break;
                     }
                 case IToken.t_enum :
                     try
                     {
                         enumSpecifier(sdw);
    					    flags.setEncounteredTypename(true);
                         break;
                     }
                     catch (BacktrackException bt)
                     {
                         // this is an elaborated class specifier
                         elaboratedTypeSpecifier(sdw);
                         flags.setEncounteredTypename(true);
                         break;
                     }
                 default :
                     break declSpecifiers;
             }
         }
         if (typeNameBegin != null)
             sdw.setTypeName(new TokenDuple(typeNameBegin, typeNameEnd));
     }
     /**
      * Parse an elaborated type specifier.  
      * 
      * @param decl			Declaration which owns the elaborated type 
      * @throws BacktrackException	request a backtrack
      */
     protected void elaboratedTypeSpecifier(DeclarationWrapper sdw)
         throws BacktrackException, EndOfFileException
     {
         // this is an elaborated class specifier
         IToken t = consume();
         ASTClassKind eck = null;
         switch (t.getType())
         {
             case IToken.t_class :
                 eck = ASTClassKind.CLASS;
                 break;
             case IToken.t_struct :
                 eck = ASTClassKind.STRUCT;
                 break;
             case IToken.t_union :
                 eck = ASTClassKind.UNION;
                 break;
             case IToken.t_enum :
                 eck = ASTClassKind.ENUM;
                 break;
             default :
                 break;
         }
  
         ITokenDuple d = name(sdw.getScope(), CompletionKind.TYPE_REFERENCE);
 		IASTTypeSpecifier elaboratedTypeSpec = null;
 		final boolean isForewardDecl = ( LT(1) == IToken.tSEMI );
 		
         try
         {
             elaboratedTypeSpec =
                 astFactory.createElaboratedTypeSpecifier(
                     sdw.getScope(),
                     eck,
                     d,
                     t.getOffset(),
                     t.getLineNumber(), 
                     d.getLastToken().getEndOffset(), d.getLastToken().getLineNumber(), isForewardDecl, sdw.isFriend() );
         }
         catch (ASTSemanticException e)
         {
 			failParse();
 			throw backtrack;
         } catch (Exception e)
         {
             throw backtrack;
         }
         sdw.setTypeSpecifier(elaboratedTypeSpec);
         
         if( isForewardDecl )
         	((IASTElaboratedTypeSpecifier)elaboratedTypeSpec).acceptElement( requestor );
     }
     /**
      * Parse an identifier.  
      * 
      * @throws BacktrackException	request a backtrack
      */
     protected IToken identifier() throws EndOfFileException, BacktrackException
     {
         IToken first = consume(IToken.tIDENTIFIER); // throws backtrack if its not that
         return first;
     }
     /**
      * Parses a className.  
      * 
      * class-name: identifier | template-id
      * 
      * @throws BacktrackException
      */
     protected ITokenDuple className(IASTScope scope) throws EndOfFileException, BacktrackException
     {
 		ITokenDuple duple = name(scope, CompletionKind.USER_SPECIFIED_NAME );
 		IToken last = duple.getLastToken(); 
         if (LT(1) == IToken.tLT) {
 			last = consumeTemplateParameters(duple.getLastToken());
         }
         
 		return new TokenDuple(duple.getFirstToken(), last);
     }
     
     /**
      * Parses the initDeclarator construct of the ANSI C++ spec.
      * 
      * initDeclarator
      * : declarator ("=" initializerClause | "(" expressionList ")")?
      * @param owner			IParserCallback object that represents the owner declaration object.  
      * @return				declarator that this parsing produced.  
      * @throws BacktrackException	request a backtrack
      */
     protected Declarator initDeclarator(
         DeclarationWrapper sdw, SimpleDeclarationStrategy strategy )
         throws EndOfFileException, BacktrackException
     {
         Declarator d = declarator(sdw, sdw.getScope(), strategy );
         if( language == ParserLanguage.CPP )
         	optionalCPPInitializer(d);
         else if( language == ParserLanguage.C )
         	optionalCInitializer(d);
         sdw.addDeclarator(d);
         return d;
     }
     
     protected void optionalCPPInitializer(Declarator d)
         throws EndOfFileException, BacktrackException
     {
         // handle initializer
         final IASTScope scope = d.getDeclarationWrapper().getScope();
 		if (LT(1) == IToken.tASSIGN)
         {
             consume(IToken.tASSIGN);
             IASTInitializerClause clause = initializerClause(scope);
 			d.setInitializerClause(clause);
         }
         else if (LT(1) == IToken.tLPAREN )
         {
         	IToken mark = mark(); 
             // initializer in constructor
             try
             {
                 consume(IToken.tLPAREN); // EAT IT!
                 IASTExpression astExpression = null;
                 astExpression = expression(scope);
                 consume(IToken.tRPAREN);
                 d.setConstructorExpression(astExpression);
             } catch( BacktrackException bt )
             {
             	backup( mark ); 
             	throw bt;
             }
         }
     }
     
     protected void optionalCInitializer( Declarator d ) throws EndOfFileException, BacktrackException
     {
     	if( LT(1) == IToken.tASSIGN )
     	{
     		consume( IToken.tASSIGN );
     		d.setInitializerClause( cInitializerClause(d.getDeclarationWrapper().getScope(), EMPTY_LIST ) );
     	}
     }
     /**
      * @param scope
      * @return
      */
     protected IASTInitializerClause cInitializerClause(
         IASTScope scope,
         List designators)
         throws EndOfFileException, BacktrackException
     {    	
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             List initializerList = new ArrayList();
             for (;;)
             {
                 // required at least one initializer list
                 // get designator list
                 List newDesignators = designatorList(scope);
                 if( newDesignators.size() != 0 )
                 	consume( IToken.tASSIGN );
                 IASTInitializerClause initializer =
                     cInitializerClause(scope, newDesignators );
                 initializerList.add(initializer);
                 // can end with just a '}'
                 if (LT(1) == IToken.tRBRACE)
                     break;
                 // can end with ", }"
                 if (LT(1) == IToken.tCOMMA)
                     consume(IToken.tCOMMA);
                 if (LT(1) == IToken.tRBRACE)
                     break;
                 // otherwise, its another initializer in the list
             }
             // consume the closing brace
             consume(IToken.tRBRACE);
             return astFactory.createInitializerClause(
                 scope,
                 (
 				( designators.size() == 0 ) ? 
 					IASTInitializerClause.Kind.INITIALIZER_LIST : 
 					IASTInitializerClause.Kind.DESIGNATED_INITIALIZER_LIST ),
                 null, initializerList, designators );
         }
         // if we get this far, it means that we have not yet succeeded
         // try this now instead
         // assignmentExpression 
         try
         {
             IASTExpression assignmentExpression = assignmentExpression(scope);
             try
             {
                 return astFactory.createInitializerClause(
                     scope,
                     (
 				( designators.size() == 0 ) ? 
 					IASTInitializerClause.Kind.ASSIGNMENT_EXPRESSION : 
 					IASTInitializerClause.Kind.DESIGNATED_ASSIGNMENT_EXPRESSION ),
                     assignmentExpression, null, designators );
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
         }
         catch (BacktrackException b)
         {
             // do nothing
         }
         throw backtrack;
     }
     /**
      * 
      */
     protected IASTInitializerClause initializerClause(IASTScope scope)
         throws EndOfFileException, BacktrackException
     {
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             if (LT(1) == (IToken.tRBRACE))
             {
                 consume(IToken.tRBRACE);
                 try
                 {
                     return astFactory.createInitializerClause(
                         scope,
                         IASTInitializerClause.Kind.EMPTY,
                         null, null, EMPTY_LIST );
                 }
                 catch (Exception e)
                 {
                     throw backtrack;
                 }
             }
             
             // otherwise it is a list of initializer clauses
             List initializerClauses = new ArrayList();
             for (;;)
             {
                 IASTInitializerClause clause = initializerClause(scope);
                 initializerClauses.add(clause);
                 if (LT(1) == IToken.tRBRACE)
                     break;
                 consume(IToken.tCOMMA);
             }
             consume(IToken.tRBRACE);
             try
             {
                 return astFactory.createInitializerClause(
                     scope,
                     IASTInitializerClause.Kind.INITIALIZER_LIST,
                     null, initializerClauses, EMPTY_LIST );
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
         }
         
         // if we get this far, it means that we did not 
         // try this now instead
         // assignmentExpression 
         try
         {
             IASTExpression assignmentExpression =
                 assignmentExpression(scope);
    
             try
             {
                 return astFactory.createInitializerClause(
                     scope,
                     IASTInitializerClause.Kind.ASSIGNMENT_EXPRESSION,
                     assignmentExpression, null, EMPTY_LIST );
             }
             catch (Exception e)
             {
                 throw backtrack;
             }
         }
         catch (BacktrackException b)
         {
 			// do nothing
         }
         catch ( EndOfFileException eof )
         {
 
         }
         throw backtrack;
     }
     
     protected List designatorList(IASTScope scope) throws EndOfFileException, BacktrackException
     {
         List designatorList = new ArrayList();
         // designated initializers for C
         
     	if( LT(1) == IToken.tDOT || LT(1) == IToken.tLBRACKET )
     	{
     
     		while( LT(1) == IToken.tDOT || LT(1) == IToken.tLBRACKET )
     		{
     			IToken id = null; 
     			IASTExpression constantExpression = null;
     			IASTDesignator.DesignatorKind kind = null;
     			
     			if( LT(1) == IToken.tDOT )
     			{
     				consume( IToken.tDOT );
     				id = identifier();
     				kind = IASTDesignator.DesignatorKind.FIELD;
     			}
     			else if( LT(1) == IToken.tLBRACKET )
     			{
     				consume( IToken.tLBRACKET );
     				constantExpression = expression( scope );
     				consume( IToken.tRBRACKET );
 					kind = IASTDesignator.DesignatorKind.SUBSCRIPT; 	
     			}
     			
     			IASTDesignator d = 
     				astFactory.createDesignator( kind, constantExpression, id );
     			designatorList.add( d );
     				
     		}
     	}
 		return designatorList;
     }
     /**
      * Parse a declarator, as according to the ANSI C++ specification. 
      * 
      * declarator
      * : (ptrOperator)* directDeclarator
      * 
      * directDeclarator
      * : declaratorId
      * | directDeclarator "(" parameterDeclarationClause ")" (cvQualifier)*
      *     (exceptionSpecification)*
      * | directDeclarator "[" (constantExpression)? "]"
      * | "(" declarator")"
      * | directDeclarator "(" parameterDeclarationClause ")" (oldKRParameterDeclaration)*
      * 
      * declaratorId
      * : name
      * 
     	 * @param container		IParserCallback object that represents the owner declaration.  
      * @return				declarator that this parsing produced.
      * @throws BacktrackException	request a backtrack
      */
     protected Declarator declarator(
         IDeclaratorOwner owner, IASTScope scope, SimpleDeclarationStrategy strategy )
         throws EndOfFileException, BacktrackException
     {
         Declarator d = null;
         DeclarationWrapper sdw = owner.getDeclarationWrapper();
         overallLoop : do
         {
             d = new Declarator(owner);
  
             consumePointerOperators(d);
  
             if (LT(1) == IToken.tLPAREN)
             {
                 consume();
                 declarator(d, scope, strategy );
                 consume(IToken.tRPAREN);
             }
             else
 	            consumeTemplatedOperatorName(d);
             
             for (;;)
             {
                 switch (LT(1))
                 {
                     case IToken.tLPAREN :
                     	
                     	boolean failed = false;
                         // temporary fix for initializer/function declaration ambiguity
                         if ( queryLookaheadCapability(2) && !LA(2).looksLikeExpression() && strategy != SimpleDeclarationStrategy.TRY_VARIABLE  )
                         {
                         	if(  LT(2) == IToken.tIDENTIFIER )
                         	{
 								IToken newMark = mark();
 								consume( IToken.tLPAREN );
 
 	                        	try
 	                        	{
 	                        		try
                                     {
                                         if( ! astFactory.queryIsTypeName( scope, name(scope, CompletionKind.TYPE_REFERENCE ) ) )
                                         	failed = true;
                                     }
                                     catch (Exception e)
                                     {
                                         throw backtrack;
                                     }
 	                        	} catch( BacktrackException b )
 	                        	{ 
 	                        		failed = true; 
 	                        	}
 	                        	
 								backup( newMark );
                         	}
                         }
 						if( ( queryLookaheadCapability(2) && !LA(2).looksLikeExpression() && strategy != SimpleDeclarationStrategy.TRY_VARIABLE && !failed) || ! queryLookaheadCapability(3) )
 						{  									
                             // parameterDeclarationClause
                             d.setIsFunction(true);
 							// TODO need to create a temporary scope object here 
                             consume(IToken.tLPAREN);
                             setCompletionValues( scope, CompletionKind.ARGUMENT_TYPE, Key.DECL_SPECIFIER_SEQUENCE );
                             boolean seenParameter = false;
                             parameterDeclarationLoop : for (;;)
                             {
                                 switch (LT(1))
                                 {
                                     case IToken.tRPAREN :
                                         consume();
                                         setCompletionValues( scope, CompletionKind.NO_SUCH_KIND, KeywordSets.Key.FUNCTION_MODIFIER );
                                         break parameterDeclarationLoop;
                                     case IToken.tELLIPSIS :
                                         consume();
                                         d.setIsVarArgs( true );
                                         break;
                                     case IToken.tCOMMA :
                                         consume();
                                         setCompletionValues( scope, CompletionKind.ARGUMENT_TYPE, Key.DECL_SPECIFIER_SEQUENCE );            
                                         seenParameter = false;
                                         break;
                                     default :
                                         if (seenParameter)
                                             throw backtrack;
                                         parameterDeclaration(d, scope);
                                         seenParameter = true;
                                 }
                             }
 						}
 
                         if (LT(1) == IToken.tCOLON || LT(1) == IToken.t_try )
                             break overallLoop;
                         
                         IToken beforeCVModifier = mark();
                         IToken cvModifier = null;
                         IToken afterCVModifier = beforeCVModifier;
                         // const-volatile
                         // 2 options: either this is a marker for the method,
                         // or it might be the beginning of old K&R style parameter declaration, see
                         //      void getenv(name) const char * name; {}
                         // This will be determined further below
                         if (LT(1) == IToken.t_const
                             || LT(1) == IToken.t_volatile)
                         {
                             cvModifier = consume();
                             afterCVModifier = mark();
                         }
                         //check for throws clause here 
                         List exceptionSpecIds = null;
                         if (LT(1) == IToken.t_throw)
                         {
                             exceptionSpecIds = new ArrayList();
                             consume(); // throw
                             consume(IToken.tLPAREN); // (
                             boolean done = false;
                             IASTTypeId duple = null;
                             while (!done)
                             {
                                 switch (LT(1))
                                 {
                                     case IToken.tRPAREN :
                                         consume();
                                         done = true;
                                         break;
                                     case IToken.tCOMMA :
                                         consume();
                                         break;
                                     default :
                                         String image = LA(1).getImage();
                                         try
                                         {
                                             duple = typeId(scope, false);
                                             exceptionSpecIds.add(duple);
                                         }
                                         catch (BacktrackException e)
                                         {
                                             failParse();
                                             log.traceLog(
                                                 "Unexpected Token ="
                                                     + image );
                                             consume();
                                             // eat this token anyway
                                             continue;
                                         }
                                         break;
                                 }
                             }
                             if (exceptionSpecIds != null)
                                 try
                                 {
                                     d.setExceptionSpecification(
                                         astFactory
                                             .createExceptionSpecification(
                                             d.getDeclarationWrapper().getScope(), exceptionSpecIds));
                                 }
                                 catch (ASTSemanticException e)
                                 {
                                     failParse();
                                     throw backtrack;
                                 } catch (Exception e)
                                 {
                                     throw backtrack;
                                 }
                         }
                         // check for optional pure virtual							
                         if (LT(1) == IToken.tASSIGN
                             && LT(2) == IToken.tINTEGER
                             && LA(2).getImage().equals("0"))
                         {
                             consume(IToken.tASSIGN);
                             consume(IToken.tINTEGER);
                             d.setPureVirtual(true);
                         }
                         if (afterCVModifier != LA(1)
                             || LT(1) == IToken.tSEMI)
                         {
                             // There were C++-specific clauses after const/volatile modifier
                             // Then it is a marker for the method
                             if (cvModifier != null)
                             {
            
                                 if (cvModifier.getType() == IToken.t_const)
                                     d.setConst(true);
                                 if (cvModifier.getType()
                                     == IToken.t_volatile)
                                     d.setVolatile(true);
                             }
                             afterCVModifier = mark();
                             // In this case (method) we can't expect K&R parameter declarations,
                             // but we'll check anyway, for errorhandling
                         }
                         break;
                     case IToken.tLBRACKET :
                         consumeArrayModifiers(d, sdw.getScope());
                         continue;
                     case IToken.tCOLON :
                         consume(IToken.tCOLON);
                         IASTExpression exp = null;
                         exp = constantExpression(scope);
                         d.setBitFieldExpression(exp);
                     default :
                         break;
                 }
                 break;
             }
             if (LA(1).getType() != IToken.tIDENTIFIER)
                 break;
 
         }
         while (true);
         if (d.getOwner() instanceof IDeclarator)
              ((Declarator)d.getOwner()).setOwnedDeclarator(d);
         return d;
     }
     protected void consumeTemplatedOperatorName(Declarator d)
         throws EndOfFileException, BacktrackException
     {
         if (LT(1) == IToken.t_operator)
             operatorId(d, null);
         else
         {
             try
             {
                 ITokenDuple duple = name(d.getDeclarationWrapper().getScope(), CompletionKind.SINGLE_NAME_REFERENCE );
                 d.setName(duple);
         
             }
             catch (BacktrackException bt)
             {
                 Declarator d1 = d;
                 Declarator d11 = d1;
                 IToken start = null;
                 IToken mark = mark();
                 if (LT(1) == IToken.tCOLONCOLON
                     || LT(1) == IToken.tIDENTIFIER)
                 {
                     start = consume();
                     IToken end = null;
                     if (start.getType() == IToken.tIDENTIFIER)
                         end = consumeTemplateParameters(end);
                         while (LT(1) == IToken.tCOLONCOLON
                             || LT(1) == IToken.tIDENTIFIER)
                         {
                             end = consume();
                             if (end.getType() == IToken.tIDENTIFIER)
                                 end = consumeTemplateParameters(end);
                         }
                     if (LT(1) == IToken.t_operator)
                         operatorId(d11, start);
                     else
                     {
                         backup(mark);
                         throw backtrack;
                     }
                 }
             }
         }
     }
     /**
      * Parse an enumeration specifier, as according to the ANSI specs in C & C++.  
      * 
      * enumSpecifier:
      * 		"enum" (name)? "{" (enumerator-list) "}"
      * enumerator-list:
      * 	enumerator-definition
      *	enumerator-list , enumerator-definition
      * enumerator-definition:
      * 	enumerator
      *  enumerator = constant-expression
      * enumerator: identifier 
      * 
      * @param	owner		IParserCallback object that represents the declaration that owns this type specifier. 
      * @throws	BacktrackException	request a backtrack
      */
     protected void enumSpecifier(DeclarationWrapper sdw)
         throws BacktrackException, EndOfFileException
     {
         IToken mark = mark();
         IToken identifier = null;
         consume( IToken.t_enum );
         if (LT(1) == IToken.tIDENTIFIER)
         {
             identifier = identifier();
         }
         if (LT(1) == IToken.tLBRACE)
         {
             IASTEnumerationSpecifier enumeration = null;
             try
             {
                 enumeration = astFactory.createEnumerationSpecifier(
                         sdw.getScope(),
                         ((identifier == null) ? "" : identifier.getImage()),
                         mark.getOffset(), 
                         mark.getLineNumber(), 
                          ((identifier == null)
                             ? mark.getOffset()
                             : identifier.getOffset()), 
 							((identifier == null)? mark.getEndOffset() : identifier.getEndOffset()), 
 							((identifier == null)? mark.getLineNumber() : identifier.getLineNumber())
                 );
             }
             catch (ASTSemanticException e)
             {
 				failParse();
 				throw backtrack;               
             } catch (Exception e)
             {
                 throw backtrack;
             }
             consume(IToken.tLBRACE);
             while (LT(1) != IToken.tRBRACE)
             {
                 IToken enumeratorIdentifier = null;
                 if (LT(1) == IToken.tIDENTIFIER)
                 {
                     enumeratorIdentifier = identifier();
                 }
                 else
                 {
                     throw backtrack;
                 }
                 IASTExpression initialValue = null;
                 if (LT(1) == IToken.tASSIGN)
                 {
                     consume(IToken.tASSIGN);
                     initialValue = constantExpression(sdw.getScope());
                 }
   
                 if (LT(1) == IToken.tRBRACE)
                 {
                     try
                     {
                         astFactory.addEnumerator(
                             enumeration,
                             enumeratorIdentifier.getImage(),
                             enumeratorIdentifier.getOffset(),
 							enumeratorIdentifier.getLineNumber(),
                             enumeratorIdentifier.getOffset(), enumeratorIdentifier.getEndOffset(), 
                             enumeratorIdentifier.getLineNumber(), lastToken.getEndOffset(), lastToken.getLineNumber(), initialValue);
                     }
                     catch (ASTSemanticException e1)
                     {
 						failParse();
 						throw backtrack;                   
                     } catch (Exception e)
                     {
                         throw backtrack;
                     }
                     break;
                 }
                 if (LT(1) != IToken.tCOMMA)
                 {
                     throw backtrack;
                 }
                 try
                 {
                     astFactory.addEnumerator(
                         enumeration,
                         enumeratorIdentifier.getImage(),
                         enumeratorIdentifier.getOffset(),
 						enumeratorIdentifier.getLineNumber(),
 						enumeratorIdentifier.getOffset(), enumeratorIdentifier.getEndOffset(), enumeratorIdentifier.getLineNumber(), lastToken.getEndOffset(), lastToken.getLineNumber(), initialValue);
                 }
                 catch (ASTSemanticException e1)
                 {
 					failParse();
 					throw backtrack; 
                 } catch (Exception e)
                 {
                     throw backtrack;
                 }
                 consume(IToken.tCOMMA);
             }
             IToken t = consume(IToken.tRBRACE);
             enumeration.setEndingOffsetAndLineNumber(t.getEndOffset(), t.getLineNumber());
             enumeration.acceptElement( requestor );
             sdw.setTypeSpecifier(enumeration);
         }
         else
         {
             // enumSpecifierAbort
             backup(mark);
             throw backtrack;
         }
     }
     /**
      * Parse a class/struct/union definition. 
      * 
      * classSpecifier
      * : classKey name (baseClause)? "{" (memberSpecification)* "}"
      * 
      * @param	owner		IParserCallback object that represents the declaration that owns this classSpecifier
      * @throws	BacktrackException	request a backtrack
      */
     protected void classSpecifier(DeclarationWrapper sdw)
         throws BacktrackException, EndOfFileException
     {
         ClassNameType nameType = ClassNameType.IDENTIFIER;
         ASTClassKind classKind = null;
         ASTAccessVisibility access = ASTAccessVisibility.PUBLIC;
         IToken classKey = null;
         IToken mark = mark();
         
 		// class key
         switch (LT(1))
         {
             case IToken.t_class :
                 classKey = consume();
                 classKind = ASTClassKind.CLASS;
                 access = ASTAccessVisibility.PRIVATE;
                 break;
             case IToken.t_struct :
                 classKey = consume();
                 classKind = ASTClassKind.STRUCT;
                 break;
             case IToken.t_union :
                 classKey = consume();
                 classKind = ASTClassKind.UNION;
                 break;
             default :
                 throw backtrack;
         }
 
         
         ITokenDuple duple = null;
         
         setCompletionValues(sdw.getScope(), CompletionKind.USER_SPECIFIED_NAME, Key.EMPTY );
         // class name
         if (LT(1) == IToken.tIDENTIFIER)
             duple = className(sdw.getScope());
         if (duple != null && !duple.isIdentifier())
             nameType = ClassNameType.TEMPLATE;
         if (LT(1) != IToken.tCOLON && LT(1) != IToken.tLBRACE)
         {
             backup(mark);
             throw backtrack;
         }
         IASTClassSpecifier astClassSpecifier = null;
         
         try
         {
             astClassSpecifier = 
                 astFactory
                     .createClassSpecifier(
                         sdw.getScope(),
                         duple, 
                         classKind,
                         nameType,
                         access,
                         classKey.getOffset(),
             			classKey.getLineNumber(), 
 						duple == null ?  classKey.getOffset() : duple.getFirstToken().getOffset(), 
 						duple == null ?  classKey.getEndOffset() : duple.getFirstToken().getEndOffset(), 
 						duple == null ?  classKey.getLineNumber() : duple.getFirstToken().getLineNumber() );
         }
         catch (ASTSemanticException e)
         {
 			failParse();
 			throw backtrack;
         } catch (Exception e)
         {
             throw backtrack;
         }
         sdw.setTypeSpecifier(astClassSpecifier);
         // base clause
         if (LT(1) == IToken.tCOLON)
         {
             baseSpecifier(astClassSpecifier);
         }
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             setCompletionValues(astClassSpecifier, CompletionKind.FIELD_TYPE, Key.DECLARATION );
             astClassSpecifier.enterScope( requestor );
             memberDeclarationLoop : while (LT(1) != IToken.tRBRACE)
             {
                 IToken checkToken = LA(1);
                 switch (LT(1))
                 {
                     case IToken.t_public :
 						consume(); 
 						consume(IToken.tCOLON);
 						astClassSpecifier.setCurrentVisibility( ASTAccessVisibility.PUBLIC );
 						break;                    
                     case IToken.t_protected :
 						consume(); 
 						consume(IToken.tCOLON);
 					astClassSpecifier.setCurrentVisibility( ASTAccessVisibility.PROTECTED);
 						break;
 
                     case IToken.t_private :
                     	consume(); 
                         consume(IToken.tCOLON);
 						astClassSpecifier.setCurrentVisibility( ASTAccessVisibility.PRIVATE);
                         break;
                     case IToken.tRBRACE :
                         consume(IToken.tRBRACE);
                         break memberDeclarationLoop;
                     default :
                         try
                         {
                             declaration(astClassSpecifier, null, null);
                         }
                         catch (BacktrackException bt)
                         {
                             failParse();
                             if (checkToken == LA(1))
                                 errorHandling();
                         }
                 }
                 if (checkToken == LA(1))
                     errorHandling();
             }
             // consume the }
             IToken lt = consume(IToken.tRBRACE);
             astClassSpecifier.setEndingOffsetAndLineNumber(lt.getEndOffset(), lt.getLineNumber());
             
             try
             {
                 astFactory.signalEndOfClassSpecifier( astClassSpecifier );
             }
             catch (Exception e1)
             {
                 throw backtrack;
             }
             
             astClassSpecifier.exitScope( requestor );
             
             
         }
     }
     /**
      * Parse the subclass-baseclauses for a class specification.  
      * 
      * baseclause:	: basespecifierlist
      * basespecifierlist: 	basespecifier
      * 						basespecifierlist, basespecifier
      * basespecifier:	::? nestednamespecifier? classname
      * 					virtual accessspecifier? ::? nestednamespecifier? classname
      * 					accessspecifier virtual? ::? nestednamespecifier? classname
      * accessspecifier:	private | protected | public
      * @param classSpecOwner
      * @throws BacktrackException
      */
     protected void baseSpecifier(
         IASTClassSpecifier astClassSpec)
         throws EndOfFileException, BacktrackException
     {
         consume(IToken.tCOLON);
         
         setCompletionValues(astClassSpec.getOwnerScope(), CompletionKind.CLASS_REFERENCE, KeywordSets.Key.BASE_SPECIFIER );
         boolean isVirtual = false;
         ASTAccessVisibility visibility = ASTAccessVisibility.PUBLIC;
         ITokenDuple nameDuple = null;
         
         baseSpecifierLoop : for (;;)
         {
             switch (LT(1))
             {
                 case IToken.t_virtual :
                     consume(IToken.t_virtual);
                     isVirtual = true;
                     break;
                 case IToken.t_public :
                 	consume(); 
                     break;
                 case IToken.t_protected :
 					consume();
 				    visibility = ASTAccessVisibility.PROTECTED;
                     break;
                 case IToken.t_private :
                     visibility = ASTAccessVisibility.PRIVATE;
 					consume();
            			break;
                 case IToken.tCOLONCOLON :
                 case IToken.tIDENTIFIER :
                     nameDuple = name(astClassSpec, CompletionKind.CLASS_REFERENCE );
                     break;
                 case IToken.tCOMMA :
                     try
                     {
                         astFactory.addBaseSpecifier(
                             astClassSpec,
                             isVirtual,
                             visibility,
                             nameDuple );
                     }
                     catch (ASTSemanticException e)
                     {
 						failParse();
 						throw backtrack;
                     } catch (Exception e)
                     {
                         throw backtrack;
                     }
                     isVirtual = false;
                     visibility = ASTAccessVisibility.PUBLIC;
                     nameDuple = null;                        
                     consume();
                     continue baseSpecifierLoop;
                 default :
                     break baseSpecifierLoop;
             }
         }
 
         try
         {
             astFactory.addBaseSpecifier(
                 astClassSpec,
                 isVirtual,
                 visibility,
                 nameDuple );
         }
         catch (ASTSemanticException e)
         {
 			failParse();
 			throw backtrack;
         } catch (Exception e)
         {
             throw backtrack;
         }
     }
     /**
      * Parses a function body. 
      * 
      * @throws BacktrackException	request a backtrack
      */
     protected void functionBody( IASTScope scope ) throws EndOfFileException, BacktrackException
     {
         compoundStatement( scope, false );
     }
     /**
      * Parses a statement. 
      * 
      * @throws BacktrackException	request a backtrack
      */
     protected void statement(IASTCodeScope scope) throws EndOfFileException, BacktrackException
     {
     	setCompletionValues(scope, CompletionKind.SINGLE_NAME_REFERENCE, Key.STATEMENT);
     	
         switch (LT(1))
         {
             case IToken.t_case :
                 consume(IToken.t_case);
                 IASTExpression constant_expression = constantExpression(scope);
 				constant_expression.acceptElement(requestor);
                 consume(IToken.tCOLON);
                 statement(scope);
                 return;
             case IToken.t_default :
                 consume(IToken.t_default);
                 consume(IToken.tCOLON);
                 statement(scope);
                 return;
             case IToken.tLBRACE :
                 compoundStatement(scope, true);
                 return;
             case IToken.t_if :
                 consume( IToken.t_if );
                 consume(IToken.tLPAREN);
                 condition( scope );
                 consume(IToken.tRPAREN);
                 if( LT(1) != IToken.tLBRACE )
                     singleStatementScope(scope);
                 else
                 	statement( scope );
                 if (LT(1) == IToken.t_else)
                 {
                     consume( IToken.t_else );
                     if( LT(1) != IToken.tLBRACE )
 						singleStatementScope(scope);
                     else
                     	statement( scope );
                 }
                 return;
             case IToken.t_switch :
                 consume();
                 consume(IToken.tLPAREN);
                 condition(scope);
                 consume(IToken.tRPAREN);
                 statement(scope);
                 return;
             case IToken.t_while :
                 consume(IToken.t_while);
                 consume(IToken.tLPAREN);
                 condition(scope);
                 consume(IToken.tRPAREN);
                 if( LT(1) != IToken.tLBRACE )
 					singleStatementScope(scope);
                 else
                 	statement(scope);
                 return;
             case IToken.t_do :
                 consume(IToken.t_do);
 				if( LT(1) != IToken.tLBRACE )
 					singleStatementScope(scope);
 				else
 					statement(scope);
                 consume(IToken.t_while);
                 consume(IToken.tLPAREN);
                 condition(scope);
                 consume(IToken.tRPAREN);
                 return;
             case IToken.t_for :
                 consume();
                 consume(IToken.tLPAREN);
                 forInitStatement(scope);
                 if (LT(1) != IToken.tSEMI)
                     condition(scope);
                 consume(IToken.tSEMI);
                 if (LT(1) != IToken.tRPAREN)
                 {  
                     IASTExpression finalExpression = expression(scope);
                     finalExpression.acceptElement(requestor);
                 }
                 consume(IToken.tRPAREN);
                 statement(scope);
                 return;
             case IToken.t_break :
                 consume();
                 consume(IToken.tSEMI);
                 return;
             case IToken.t_continue :
                 consume();
                 consume(IToken.tSEMI);
                 return;
             case IToken.t_return :
                 consume();
                 if (LT(1) != IToken.tSEMI)
                 {
                     IASTExpression retVal = expression(scope);
                     retVal.acceptElement(requestor);
                 }
                 consume(IToken.tSEMI);
                 return;
             case IToken.t_goto :
                 consume();
                 consume(IToken.tIDENTIFIER);
                 consume(IToken.tSEMI);
                 return;
             case IToken.t_try :
                 consume();
                 compoundStatement(scope,true);
                 catchHandlerSequence(scope);
                 return;
             case IToken.tSEMI :
                 consume();
                 return;
             default :
                 // can be many things:
                 // label
             	
             	try
 				{
 	                if (LT(1) == IToken.tIDENTIFIER && LT(2) == IToken.tCOLON)
 	                {
 	                    consume(IToken.tIDENTIFIER);
 	                    consume(IToken.tCOLON);
 	                    statement(scope);
 	                    return;
 	                }
             	}catch( OffsetLimitReachedException olre )
 				{
             		// ok
             	}
                 // expressionStatement
                 // Note: the function style cast ambiguity is handled in expression
                 // Since it only happens when we are in a statement
                 IToken mark = mark();
                 try
                 {
                     IASTExpression thisExpression = expression(scope);
                    	consume(IToken.tSEMI);
                     thisExpression.acceptElement( requestor );
                     return;
                 }
                 catch (BacktrackException b)
                 {
                 	backup( mark );
                 }
                 catch( OffsetLimitReachedException olre )
 				{
                 	backup(mark);
                 }
 
                 // declarationStatement
                 declaration(scope, null, null);
         }        
 
     }
     protected void catchHandlerSequence(IASTScope scope)
 	throws EndOfFileException, BacktrackException {
     	if( LT(1) != IToken.t_catch )
     		throw backtrack; // error, need at least one of these
     	while (LT(1) == IToken.t_catch)
     	{
     		consume(IToken.t_catch);
     		consume(IToken.tLPAREN);
     		if( LT(1) == IToken.tELLIPSIS )
     			consume( IToken.tELLIPSIS );
     		else 
     			simpleDeclaration( SimpleDeclarationStrategy.TRY_VARIABLE, scope, null, CompletionKind.EXCEPTION_REFERENCE, true); // was exceptionDeclaration
     		consume(IToken.tRPAREN);
     		
     		catchBlockCompoundStatement(scope);
     	}
     }
     
     
     protected abstract void catchBlockCompoundStatement(IASTScope scope) throws BacktrackException, EndOfFileException; 
     
 	protected void singleStatementScope(IASTScope scope) throws EndOfFileException, BacktrackException
     {
         IASTCodeScope newScope;
         try
         {
             newScope = astFactory.createNewCodeBlock(scope);
         }
         catch (Exception e)
         {
             throw backtrack;
         }
         newScope.enterScope( requestor );
         try
         {
 			statement( newScope );
         }
         finally
         {
 			newScope.exitScope( requestor );
         }
     }
 
     /**
      * @throws BacktrackException
      */
     protected void condition( IASTScope scope ) throws BacktrackException, EndOfFileException
     {
         IASTExpression someExpression = expression( scope );
         someExpression.acceptElement(requestor);
         //TODO type-specifier-seq declarator = assignment expression 
     }
     
     /**
      * @throws BacktrackException
      */
     protected void forInitStatement( IASTScope scope ) throws BacktrackException, EndOfFileException
     {
     	try
     	{
         	simpleDeclarationStrategyUnion(scope,null, null);
     	}
     	catch( BacktrackException bt )
     	{
     		try
     		{
     			IASTExpression e = expression( scope );
     			e.acceptElement(requestor);
     		}
     		catch( BacktrackException b )
     		{
     			failParse(); 
     			throw b;
     		}
     	}
         
     }
     /**
      * @throws BacktrackException
      */
     protected void compoundStatement( IASTScope scope, boolean createNewScope ) throws EndOfFileException, BacktrackException
     {
         consume(IToken.tLBRACE);
         
 		IASTCodeScope newScope = null;
         if( createNewScope )
         {
         	try
             {
                 newScope = astFactory.createNewCodeBlock(scope);
             }
             catch (Exception e)
             {
                 throw backtrack;
             }        
         	newScope.enterScope( requestor );
         }
         IToken checkToken = null;
         setCompletionValues( 
         		(createNewScope ? newScope : scope ), 
 				CompletionKind.SINGLE_NAME_REFERENCE, 
         		KeywordSets.Key.STATEMENT );
         
         while (LT(1) != IToken.tRBRACE)
         {
         	checkToken = LA(1);
         	try
         	{
             	statement((IASTCodeScope) (createNewScope ? newScope : scope) );
         	}
         	catch( BacktrackException b )
         	{
         		failParse(); 
         		if( LA(1) == checkToken )
         			errorHandling();
         	}
         	setCompletionValues(((createNewScope ? newScope : scope )), CompletionKind.SINGLE_NAME_REFERENCE, 
         			KeywordSets.Key.STATEMENT );
         }
         
         consume(IToken.tRBRACE);
         
         if( createNewScope )
         	newScope.exitScope( requestor );
     }
     
     /**
      * @throws Exception
      */
     protected void varName() throws Exception
     {
         if (LT(1) == IToken.tCOLONCOLON)
             consume();
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tIDENTIFIER :
                     consume();
                     //if (isTemplateArgs()) {
                     //	rTemplateArgs();
                     //}
                     if (LT(1) == IToken.tCOLONCOLON)
                     {
                         switch (LT(2))
                         {
                             case IToken.tIDENTIFIER :
                             case IToken.tCOMPL :
                             case IToken.t_operator :
                                 consume();
                                 break;
                             default :
                                 return;
                         }
                     }
                     else
                         return;
                     break;
                 case IToken.tCOMPL :
                     consume();
                     consume(IToken.tIDENTIFIER);
                     return;
                 case IToken.t_operator :
                     consume();
                     //rOperatorName();
                     return;
                 default :
                     throw backtrack;
             }
         }
     }
 
     protected IASTCompilationUnit compilationUnit;
     
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#getLanguage()
      */
     public ParserLanguage getLanguage()
     {
         return language;
     }
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#setLanguage(Language)
      */
     public void setLanguage( ParserLanguage l )
     {
         language = l;
     }
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#getLastErrorOffset()
      */
     public int getLastErrorOffset()
     {
         return firstErrorOffset;
     }
     
     protected void setCompletionToken( IToken token )
 	{
     }
     
     protected IToken getCompletionToken()
 	{
     	return null;
     }
     
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.IParser#parse(int, int)
 	 */
 	public IASTNode parse(int startingOffset, int endingOffset)
 		throws ParseError {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.IParser#parse(int)
 	 */
 	public IASTCompletionNode parse(int offset) throws ParseError {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.internal.core.parser.ExpressionParser#setupASTFactory(org.eclipse.cdt.core.parser.IScanner, org.eclipse.cdt.core.parser.ParserLanguage)
 	 */
 	protected void setupASTFactory(IScanner scanner, ParserLanguage language) {
 	}
 
 }
