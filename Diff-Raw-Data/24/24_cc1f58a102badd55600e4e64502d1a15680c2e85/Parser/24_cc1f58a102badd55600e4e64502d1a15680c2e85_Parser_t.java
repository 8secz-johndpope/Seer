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
 import java.util.List;
 
 import org.eclipse.cdt.core.parser.Backtrack;
 import org.eclipse.cdt.core.parser.EndOfFile;
 import org.eclipse.cdt.core.parser.IParser;
 import org.eclipse.cdt.core.parser.IParserCallback;
 import org.eclipse.cdt.core.parser.IProblemReporter;
 import org.eclipse.cdt.core.parser.IScanner;
 import org.eclipse.cdt.core.parser.ISourceElementRequestor;
 import org.eclipse.cdt.core.parser.IToken;
 import org.eclipse.cdt.core.parser.ITokenDuple;
 import org.eclipse.cdt.core.parser.ITranslationResult;
 import org.eclipse.cdt.core.parser.ParserFactory;
 import org.eclipse.cdt.core.parser.ParserMode;
 import org.eclipse.cdt.core.parser.ScannerException;
 import org.eclipse.cdt.core.parser.ast.AccessVisibility;
 import org.eclipse.cdt.core.parser.ast.ClassKind;
 import org.eclipse.cdt.core.parser.ast.IASTASMDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
 import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTExpression;
 import org.eclipse.cdt.core.parser.ast.IASTFactory;
 import org.eclipse.cdt.core.parser.ast.IASTInitializerClause;
 import org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification;
 import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTScope;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDirective;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier.ClassNameType;
 import org.eclipse.cdt.core.parser.ast.IASTExpression.Kind;
 import org.eclipse.cdt.internal.core.model.Util;
 
 
 /**
  * This is our first implementation of the IParser interface, serving as a parser for
  * ANSI C and C++.
  * 
  * From time to time we will make reference to the ANSI ISO specifications.
  * 
  * @author jcamelon
  */
 public class Parser implements IParser
 {
     private ClassNameType access;
     private static int DEFAULT_OFFSET = -1;
     // sentinel initial value for offsets 
     private int firstErrorOffset = DEFAULT_OFFSET;
     // offset where the first parse error occurred
     private IParserCallback callback;
     // the parser callback that was registered with us
     private ParserMode mode = ParserMode.COMPLETE_PARSE;
     // are we doing the high-level parse, or an in depth parse?
     private boolean parsePassed = true; // did the parse pass?
     private boolean cppNature = true; // true for C++, false for C
     private ISourceElementRequestor requestor = null;
     // new callback mechanism
     private IASTFactory astFactory = null; // ast factory
     
     private IProblemReporter problemReporter = null;
     private ITranslationResult unitResult = null; 
     /**
      * This is the single entry point for setting parsePassed to 
      * false, and also making note what token offset we failed upon. 
      * 
      * @throws EndOfFile
      */
     protected void failParse() throws EndOfFile
     {
         if (firstErrorOffset == DEFAULT_OFFSET)
             firstErrorOffset = LA(1).getOffset();
         parsePassed = false;
     }
     /**
      * This is the standard cosntructor that we expect the Parser to be instantiated 
      * with.  
      * 
      * @param s				IScanner instance that has been initialized to the code input 
      * @param c				IParserCallback instance that will receive callbacks as we parse
      * @param quick			Are we asking for a high level parse or not? 
      */
     public Parser(IScanner s, IParserCallback c, ParserMode m, IProblemReporter problemReporter, ITranslationResult unitResult)
     {
         callback = c;
         scanner = s;
         this.problemReporter = problemReporter;
         this.unitResult = unitResult;
         if (c instanceof ISourceElementRequestor)
             setRequestor((ISourceElementRequestor)c);
         mode = m;
         astFactory = ParserFactory.createASTFactory(m);
         scanner.setMode(m);
         scanner.setCallback(c);
         scanner.setASTFactory(astFactory);
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
         onParseEnd();
         // For the debuglog to take place, you have to call
         // Util.setDebugging(true);
         // Or set debug to true in the core plugin preference 
         Util.debugLog(
             "Parse "
                 + (++parseCount)
                 + ": "
                 + (System.currentTimeMillis() - startTime)
                 + "ms"
                 + (parsePassed ? "" : " - parse failure"));
         return parsePassed;
     }
     
     public void onParseEnd() {
         scanner.onParseEnd();
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
             callback.setParser(this);
         }
         catch (Exception e)
         {
         }
         Object translationUnit = null;
         try
         {
             translationUnit = callback.translationUnitBegin();
         }
         catch (Exception e)
         {
         }
         IASTCompilationUnit compilationUnit =
             astFactory.createCompilationUnit();
         requestor.enterCompilationUnit(compilationUnit);
         IToken lastBacktrack = null;
         IToken checkToken;
         while (true)
         {
             try
             {
                 checkToken = LA(1);
                 declaration(translationUnit, compilationUnit);
                 if (LA(1) == checkToken)
                     errorHandling();
             }
             catch (EndOfFile e)
             {
                 // Good
                 break;
             }
             catch (Backtrack b)
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
                 catch (EndOfFile e)
                 {
                     break;
                 }
             }
             catch (Exception e)
             {
                 // we've done the best we can
             }
         }
         try
         {
             callback.translationUnitEnd(translationUnit);
         }
         catch (Exception e)
         {
         }
         requestor.exitCompilationUnit(compilationUnit);
     }
     /**
      * This function is called whenever we encounter and error that we cannot backtrack out of and we 
      * still wish to try and continue on with the parse to do a best-effort parse for our client. 
      * 
      * @throws EndOfFile  	We can potentially hit EndOfFile here as we are skipping ahead.  
      */
     protected void errorHandling() throws EndOfFile
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
      * @throws Backtrack	request for a backtrack
      */
     protected void usingClause(Object container, IASTScope scope)
         throws Backtrack
     {
         IToken firstToken = consume(IToken.t_using);
         if (LT(1) == IToken.t_namespace)
         {
             Object directive = null;
             try
             {
                 directive = callback.usingDirectiveBegin(container);
             }
             catch (Exception e)
             {
             }
             // using-directive
             consume(IToken.t_namespace);
             // optional :: and nested classes handled in name
             TokenDuple duple = null;
             if (LT(1) == IToken.tIDENTIFIER || LT(1) == IToken.tCOLONCOLON)
             {
                 duple = name();
                 try
                 {
                     callback.usingDirectiveNamespaceId(directive);
                 }
                 catch (Exception e)
                 {
                 }
             }
             else
             {
                 try
                 {
                     callback.usingDirectiveAbort(directive);
                 }
                 catch (Exception e)
                 {
                 }
                 throw backtrack;
             }
             if (LT(1) == IToken.tSEMI)
             {
                 consume(IToken.tSEMI);
                 try
                 {
                     callback.usingDirectiveEnd(directive);
                 }
                 catch (Exception e)
                 {
                 }
                 IASTUsingDirective astUD =
                     astFactory.createUsingDirective(scope, duple);
                 requestor.acceptUsingDirective(astUD);
                 return;
             }
             else
             {
                 try
                 {
                     callback.usingDirectiveAbort(directive);
                 }
                 catch (Exception e)
                 {
                 }
                 throw backtrack;
             }
         }
         else
         {
             Object usingDeclaration = null;
             try
             {
                 usingDeclaration = callback.usingDeclarationBegin(container);
             }
             catch (Exception e)
             {
             }
             boolean typeName = false;
             if (LT(1) == IToken.t_typename)
             {
                 typeName = true;
                 consume(IToken.t_typename);
             }
             TokenDuple name = null;
             if (LT(1) == IToken.tIDENTIFIER || LT(1) == IToken.tCOLONCOLON)
             {
                 //	optional :: and nested classes handled in name
                 name = name();
                 try
                 {
                     callback.usingDeclarationMapping(
                         usingDeclaration,
                         typeName);
                 }
                 catch (Exception e)
                 {
                 }
             }
             else
             {
                 try
                 {
                     callback.usingDeclarationAbort(usingDeclaration);
                 }
                 catch (Exception e)
                 {
                 }
                 throw backtrack;
             }
             if (LT(1) == IToken.tSEMI)
             {
                 consume(IToken.tSEMI);
                 try
                 {
                     callback.usingDeclarationEnd(usingDeclaration);
                 }
                 catch (Exception e)
                 {
                 }
                 IASTUsingDeclaration declaration =
                     astFactory.createUsingDeclaration(scope, typeName, name);
                 requestor.acceptUsingDeclaration(declaration);
             }
             else
             {
                 try
                 {
                     callback.usingDeclarationAbort(usingDeclaration);
                 }
                 catch (Exception e)
                 {
                 }
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
      * @throws Backtrack	request for a backtrack
      */
     protected void linkageSpecification(Object container, IASTScope scope)
         throws Backtrack
     {
         consume(IToken.t_extern);
         if (LT(1) != IToken.tSTRING)
             throw backtrack;
         Object linkageSpec = null;
         IToken spec = consume(IToken.tSTRING);
         try
         {
             linkageSpec =
                 callback.linkageSpecificationBegin(container, spec.getImage());
         }
         catch (Exception e)
         {
         }
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             IASTLinkageSpecification linkage =
                 astFactory.createLinkageSpecification(scope, spec.getImage());
             requestor.enterLinkageSpecification(linkage);
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
                             declaration(linkageSpec, linkage);
                         }
                         catch (Backtrack bt)
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
             consume();
             try
             {
                 callback.linkageSpecificationEnd(linkageSpec);
             }
             catch (Exception e)
             {
             }
             requestor.exitLinkageSpecification(linkage);
         }
         else // single declaration
             {
             IASTLinkageSpecification linkage =
                 astFactory.createLinkageSpecification(scope, spec.getImage());
             requestor.enterLinkageSpecification(linkage);
             declaration(linkageSpec);
             try
             {
                 callback.linkageSpecificationEnd(linkageSpec);
             }
             catch (Exception e)
             {
             }
             requestor.exitLinkageSpecification(linkage);
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
      * @throws Backtrack		request for a backtrack
      */
     protected void templateDeclaration(Object container) throws Backtrack
     {
         IToken firstToken = null;
         if (LT(1) == IToken.t_export)
         {
             firstToken = consume(IToken.t_export);
             consume(IToken.t_template);
         }
         else
             firstToken = consume(IToken.t_template);
         if (LT(1) != IToken.tLT)
         {
             // explicit-instantiation
             Object instantiation = null;
             try
             {
                 instantiation = callback.explicitInstantiationBegin(container);
             }
             catch (Exception e)
             {
             }
             declaration(instantiation);
             try
             {
                 callback.explicitInstantiationEnd(instantiation);
             }
             catch (Exception e)
             {
             }
             return;
         }
         else
         {
             consume(IToken.tLT);
             if (LT(1) == IToken.tGT)
             {
                 consume(IToken.tGT);
                 // explicit-specialization
                 Object specialization = null;
                 try
                 {
                     specialization =
                         callback.explicitSpecializationBegin(container);
                 }
                 catch (Exception e)
                 {
                 }
                 declaration(specialization);
                 try
                 {
                     callback.explicitSpecializationEnd(specialization);
                 }
                 catch (Exception e)
                 {
                 }
                 return;
             }
         }
         Object templateDeclaration = null;
         try
         {
             try
             {
                 templateDeclaration =
                     callback.templateDeclarationBegin(container, firstToken);
             }
             catch (Exception e)
             {
             }
             templateParameterList(templateDeclaration);
             consume(IToken.tGT);
             declaration(templateDeclaration);
             try
             {
                 callback.templateDeclarationEnd(
                     templateDeclaration,
                     (Token)lastToken);
             }
             catch (Exception e)
             {
             }
         }
         catch (Backtrack bt)
         {
             try
             {
                 callback.templateDeclarationAbort(templateDeclaration);
             }
             catch (Exception e)
             {
             }
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
      * @throws Backtrack				request for a backtrack
      */
     protected void templateParameterList(Object templateDeclaration)
         throws Backtrack
     {
         // if we have gotten this far then we have a true template-declaration
         // iterate through the template parameter list
         Object templateParameterList = null;
         try
         {
             templateParameterList =
                 callback.templateParameterListBegin(templateDeclaration);
         }
         catch (Exception e)
         {
         }
         for (;;)
         {
             if (LT(1) == IToken.tGT)
                 return;
             if (LT(1) == IToken.t_class || LT(1) == IToken.t_typename)
             {
                 Object currentTemplateParm = null;
                 try
                 {
                     try
                     {
                         currentTemplateParm =
                             callback.templateTypeParameterBegin(
                                 templateParameterList,
                                 consume());
                     }
                     catch (Exception e)
                     {
                     }
                     if (LT(1) == IToken.tIDENTIFIER) // optional identifier
                     {
                         identifier();
                         try
                         {
                             callback.templateTypeParameterName(
                                 currentTemplateParm);
                         }
                         catch (Exception e)
                         {
                         }
                         if (LT(1) == IToken.tASSIGN) // optional = type-id
                         {
                             consume(IToken.tASSIGN);
                             typeId(); // type-id
                             try
                             {
                                 callback.templateTypeParameterInitialTypeId(
                                     currentTemplateParm);
                             }
                             catch (Exception e)
                             {
                             }
                         }
                     }
                     try
                     {
                         callback.templateTypeParameterEnd(currentTemplateParm);
                     }
                     catch (Exception e)
                     {
                     }
                 }
                 catch (Backtrack bt)
                 {
                     try
                     {
                         callback.templateTypeParameterAbort(
                             currentTemplateParm);
                     }
                     catch (Exception e)
                     {
                     }
                     throw bt;
                 }
             }
             else if (LT(1) == IToken.t_template)
             {
                 IToken kind = consume(IToken.t_template);
                 consume(IToken.tLT);
                 Object newTemplateParm = null;
                 try
                 {
                     newTemplateParm =
                         callback.templateTypeParameterBegin(
                             templateParameterList,
                             kind);
                 }
                 catch (Exception e)
                 {
                 }
                 templateParameterList(newTemplateParm);
                 consume(IToken.tGT);
                 consume(IToken.t_class);
                 if (LT(1) == IToken.tIDENTIFIER) // optional identifier
                 {
                     identifier();
                     try
                     {
                         callback.templateTypeParameterName(newTemplateParm);
                     }
                     catch (Exception e)
                     {
                     }
                     if (LT(1) == IToken.tASSIGN) // optional = type-id
                     {
                         consume(IToken.tASSIGN);
                         typeId();
                         try
                         {
                             callback.templateTypeParameterInitialTypeId(
                                 newTemplateParm);
                         }
                         catch (Exception e)
                         {
                         }
                     }
                 }
                 try
                 {
                     callback.templateTypeParameterEnd(newTemplateParm);
                 }
                 catch (Exception e)
                 {
                 }
             }
             else if (LT(1) == IToken.tCOMMA)
             {
                 consume(IToken.tCOMMA);
                 continue;
             }
             else
             {
                 parameterDeclaration(templateParameterList, null); // this should be something real
             }
         }
     }
     protected void declaration(Object container) throws Backtrack
     {
         declaration(container, null);
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
      * @throws Backtrack	request a backtrack
      */
     protected void declaration(Object container, IASTScope scope)
         throws Backtrack
     {
         switch (LT(1))
         {
             case IToken.t_asm :
                 IToken first = consume(IToken.t_asm);
                 consume(IToken.tLPAREN);
                 String assembly = consume(IToken.tSTRING).getImage();
                 consume(IToken.tRPAREN);
                 IToken last = consume(IToken.tSEMI);
                 IASTASMDefinition asmDefinition =
                     astFactory.createASMDefinition(
                         scope,
                         assembly,
                         first.getOffset(),
                         last.getEndOffset());
                 // if we made it this far, then we have all we need 
                 // do the callback
                 try
                 {
                     callback.asmDefinition(container, assembly);
                 }
                 catch (Exception e)
                 {
                 }
                 requestor.acceptASMDefinition(asmDefinition);
                 return;
             case IToken.t_namespace :
                 namespaceDefinition(container, scope);
                 return;
             case IToken.t_using :
                 usingClause(container, scope);
                 return;
             case IToken.t_export :
             case IToken.t_template :
                 templateDeclaration(container);
                 return;
             case IToken.t_extern :
                 if (LT(2) == IToken.tSTRING)
                 {
                     linkageSpecification(container, scope);
                     return;
                 }
             default :
                 IToken mark = mark();
                 try
                 {
                     simpleDeclaration(container, true, false, scope);
                     // try it first with the original strategy
                 }
                 catch (Backtrack bt)
                 {
                     // did not work 
                     backup(mark);
                     simpleDeclaration(container, false, false, scope);
                     // try it again with the second strategy
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
      * @throws Backtrack	request a backtrack
     
      */
     protected void namespaceDefinition(Object container, IASTScope scope)
         throws Backtrack
     {
         Object namespace = null;
         IToken first = consume(IToken.t_namespace);
         try
         {
             namespace = callback.namespaceDefinitionBegin(container, first);
         }
         catch (Exception e)
         {
         }
         IToken identifier = null;
         // optional name 		
         if (LT(1) == IToken.tIDENTIFIER)
         {
             identifier = identifier();
             try
             {
                 callback.namespaceDefinitionId(namespace);
             }
             catch (Exception e)
             {
             }
         }
         if (LT(1) == IToken.tLBRACE)
         {
             consume();
             IASTNamespaceDefinition namespaceDefinition =
                 astFactory.createNamespaceDefinition(
                     scope,
                     (identifier == null ? "" : identifier.getImage()),
                     first.getOffset(),
                     (identifier == null ? 0 : identifier.getOffset()));
             requestor.enterNamespaceDefinition(namespaceDefinition);
             namepsaceDeclarationLoop : while (LT(1) != IToken.tRBRACE)
             {
                 IToken checkToken = LA(1);
                 switch (LT(1))
                 {
                     case IToken.tRBRACE :
                         //consume(Token.tRBRACE);
                         break namepsaceDeclarationLoop;
                     default :
                         try
                         {
                             declaration(namespace, namespaceDefinition);
                         }
                         catch (Backtrack bt)
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
             IToken last = consume(IToken.tRBRACE);
             try
             {
                 callback.namespaceDefinitionEnd(namespace, (Token)last);
             }
             catch (Exception e)
             {
             }
             namespaceDefinition.setEndingOffset(
                 last.getOffset() + last.getLength());
             requestor.exitNamespaceDefinition(namespaceDefinition);
         }
         else
         {
             try
             {
                 callback.namespaceDefinitionAbort(namespace);
             }
             catch (Exception e)
             {
             }
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
      * @param forKR             Is this for K&R-style parameter declaration (true) or simple declaration (false) 
      * @throws Backtrack		request a backtrack
      */
     protected void simpleDeclaration(
         Object container,
         boolean tryConstructor,
         boolean forKR,
         IASTScope scope)
         throws Backtrack
     {
         Object simpleDecl = null;
         DeclarationWrapper sdw = new DeclarationWrapper(scope);
         try
         {
             simpleDecl = callback.simpleDeclarationBegin(container, LA(1));
         }
         catch (Exception e)
         {
         }
         declSpecifierSeq(simpleDecl, false, tryConstructor, sdw);
         Object declarator = null;
         DeclaratorDuple d = null; 
         if (LT(1) != IToken.tSEMI)
             try
             {
                 d = initDeclarator(simpleDecl, sdw);
                 declarator = d.getObject();
                 while (LT(1) == IToken.tCOMMA)
                 {
                     consume();
                     try
                     {
                         d = initDeclarator(simpleDecl, sdw);
                     }
                     catch (Backtrack b)
                     {
                         throw b;
                     }
                 }
             }
             catch (Backtrack b)
             {
                 // allowed to be empty
             }
         
         boolean done = false; 
         boolean hasFunctionBody = false; 
         switch (LT(1))
         {
             case IToken.tSEMI :
                 consume(IToken.tSEMI);
                 done = true; 
                 break;
             case IToken.tCOLON :
                 if (forKR)
                     throw backtrack;
                 ctorInitializer(declarator, d.getDeclarator());
                 // Falling through on purpose
             case IToken.tLBRACE :
                 if (forKR)
                     throw backtrack;
          
 				d.getDeclarator().hasFunctionBody( true );
 				hasFunctionBody = true; 
                 break;
             default :
                 throw backtrack;
         }
         
         List l = sdw.createAndCallbackASTNodes();
         
 		if( hasFunctionBody )
 		{
 //			if( l.size() != 1 )
 //				requestor.acceptProblem( ParserFactory.createProblem());
 			        
 			Object function = null;
 			try
 			{
 				function = callback.functionBodyBegin(simpleDecl);
 			}
 			catch (Exception e)
 			{
 			}
 			handleFunctionBody(d.getDeclarator());
 			try
 			{
 				callback.functionBodyEnd(function);
 			}
 			catch (Exception e)
 			{
 			}
 		}
         
         try
         {
             callback.simpleDeclarationEnd(simpleDecl, (Token)lastToken);
         }
         catch (Exception e)
         {
         }
     }
     
 	protected void handleFunctionBody(Declarator d) throws Backtrack, EndOfFile {
 		  if (mode == ParserMode.QUICK_PARSE)
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
 		    else
 		    {
 		        functionBody();
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
      * @throws Backtrack	request a backtrack
      */
     protected void ctorInitializer(Object declarator, Declarator d) throws Backtrack
     {
         consume(IToken.tCOLON);
         Object constructorChain = null;
         try
         {
             constructorChain = callback.constructorChainBegin(declarator);
         }
         catch (Exception e)
         {
         }
         try
         {
             for (;;)
             {
                 if (LT(1) == IToken.tLBRACE)
                     break;
                 Object constructorChainElement = null;
                 try
                 {
                     constructorChainElement =
                         callback.constructorChainElementBegin(constructorChain);
                 }
                 catch (Exception e)
                 {
                 }
                 ITokenDuple duple = name();
                 try
                 {
                     callback.constructorChainElementId(constructorChainElement);
                 }
                 catch (Exception e)
                 {
                 }
                 consume(IToken.tLPAREN);
 				IASTExpression expressionList = null;
                 Object expression = null;
                 try
                 {
                     expression = callback.expressionBegin(constructorChainElement);
                 }
                 catch (Exception e)
                 {
                 }
                 expressionList = expression(expression);
                 try
                 {
                     callback.expressionEnd(expressionList);
                 }
                 catch (Exception e)
                 {
                 }
        
                 consume(IToken.tRPAREN);
                 
                 try
                 {
                     callback.constructorChainElementEnd(
                         constructorChainElement);
                 }
                 catch (Exception e)
                 {
                 }
                 
                 d.addConstructorMemberInitializer( astFactory.createConstructorMemberInitializer( duple, expressionList ) );
                 
                 if (LT(1) == IToken.tLBRACE)
                     break;
                 consume(IToken.tCOMMA);
             }
         }
         catch (Backtrack bt)
         {
             try
             {
                 callback.constructorChainAbort(constructorChain);
             }
             catch (Exception e)
             {
             }
             throw backtrack;
         }
         try
         {
             callback.constructorChainEnd(constructorChain);
         }
         catch (Exception e)
         {
         }
     }
     /**
      * This routine parses a parameter declaration 
      * 
      * @param containerObject	The IParserCallback object representing the parameterDeclarationClause owning the parm. 
      * @throws Backtrack		request a backtrack
      */
     protected void parameterDeclaration(Object containerObject, IParameterCollection collection)
         throws Backtrack
     {
         IToken current = LA(1);
         Object parameterDecl = null;
         try
         {
             parameterDecl = callback.parameterDeclarationBegin(containerObject);
         }
         catch (Exception e)
         {
         }
         
         DeclarationWrapper sdw = new DeclarationWrapper( null ); 
         declSpecifierSeq(
             parameterDecl,
             true,
             false,
             sdw
         );
         if (LT(1) != IToken.tSEMI)
             try
             {
                 DeclaratorDuple d = initDeclarator(parameterDecl, sdw );
                 Object declarator = d.getObject();
             }
             catch (Backtrack b)
             {
                 // allowed to be empty
             }
         if (current == LA(1))
             throw backtrack;
         
         if ( collection != null ) 
         	collection.addParameter( sdw );
         	
         try
         {
             callback.parameterDeclarationEnd(parameterDecl);
         }
         catch (Exception e)
         {
         }
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
      * @throws EndOfFile       we could encounter EOF while looking ahead
      */
     private boolean lookAheadForConstructorOrConversion(Flags flags)
         throws EndOfFile
     {
         if (flags.isForParameterDeclaration())
             return false;
         if (LT(2) == IToken.tLPAREN && flags.isForConstructor())
             return true;
         boolean continueProcessing = true;
         // Portions of qualified name
         // ...::secondLastID<template-args>::lastID ...
         int secondLastIDTokenPos = -1;
         int lastIDTokenPos = 1;
         int tokenPos = 2;
         do
         {
             if (LT(tokenPos) == IToken.tLT)
             {
                 // a case for template instantiation, like CFoobar<A,B>::CFoobar
                 tokenPos++;
                 // until we get all the names sorted out
                 int depth = 1;
                 while (depth > 0)
                 {
                     switch (LT(tokenPos++))
                     {
                         case IToken.tGT :
                             --depth;
                             break;
                         case IToken.tLT :
                             ++depth;
                             break;
                     }
                 }
             }
             if (LT(tokenPos) == IToken.tCOLONCOLON)
             {
                 tokenPos++;
                 switch (LT(tokenPos))
                 {
                     case IToken.tCOMPL : // for destructors
                     case IToken.t_operator : // for conversion operators
                         return true;
                     case IToken.tIDENTIFIER :
                         secondLastIDTokenPos = lastIDTokenPos;
                         lastIDTokenPos = tokenPos;
                         tokenPos++;
                         break;
                     default :
                         // Something unexpected after ::
                         return false;
                 }
             }
             else
             {
                 continueProcessing = false;
             }
         }
         while (continueProcessing);
         // for constructors
         if (secondLastIDTokenPos < 0)
             return false;
         String secondLastID = LA(secondLastIDTokenPos).getImage();
         String lastID = LA(lastIDTokenPos).getImage();
         return secondLastID.equals(lastID);
     }
     /**
      * @param flags			input flags that are used to make our decision 
      * @return				whether or not this looks like a a declarator follows
      * @throws EndOfFile	we could encounter EOF while looking ahead
      */
     private boolean lookAheadForDeclarator(Flags flags) throws EndOfFile
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
      * @throws Backtrack		request a backtrack
      */
     protected void declSpecifierSeq(
         Object decl,
         boolean parm,
         boolean tryConstructor,
         DeclarationWrapper sdw)
         throws Backtrack
     {
         Flags flags = new Flags(parm, tryConstructor);
         declSpecifiers : for (;;)
         {
             switch (LT(1))
             {
                 case IToken.t_inline :
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     sdw.setInline(true);
                     break;
                 case IToken.t_auto :
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     sdw.setAuto(true);
                     break;
                 case IToken.t_register :
                     sdw.setRegister(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_static :
                     sdw.setStatic(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_extern :
                     sdw.setExtern(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_mutable :
                     sdw.setMutable(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_virtual :
                     sdw.setVirtual(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_explicit :
                     sdw.setExplicit(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_typedef :
                     sdw.setTypedef(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_friend :
                     sdw.setFriend(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_const :
                     sdw.setConst(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_volatile :
                     sdw.setVolatile(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_signed :
                 case IToken.t_unsigned :
                 case IToken.t_short :
                 case IToken.t_char :
                 case IToken.t_wchar_t :
                 case IToken.t_bool :
                 case IToken.t_int :
                 case IToken.t_long :
                 case IToken.t_float :
                 case IToken.t_double :
                 case IToken.t_void :
                     sdw.setType(LT(1));
                     flags.setEncounteredRawType(true);
                     try
                     {
                         callback.simpleDeclSpecifier(decl, consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_typename :
                     sdw.setTypenamed(true);
                     try
                     {
                         callback.simpleDeclSpecifier(
                             decl,
                             consume(IToken.t_typename));
                     }
                     catch (Exception e)
                     {
                     }
                     IToken first = LA(1);
                     IToken last = null;
                     last = name().getLastToken();
                     if (LT(1) == IToken.t_template)
                     {
                         consume(IToken.t_template);
                         last = templateId();
                         try
                         {
                             callback.nameBegin(first);
                             callback.nameEnd(last);
                         }
                         catch (Exception e)
                         {
                         }
                     }
                     ITokenDuple duple = new TokenDuple(first, last);
                     sdw.setTypeName(duple.toString());
                     try
                     {
                         callback.simpleDeclSpecifierName(decl);
                     }
                     catch (Exception e)
                     {
                     }
                     return;
                 case IToken.tCOLONCOLON :
                     consume(IToken.tCOLONCOLON);
                     // handle nested later:
                 case IToken.tIDENTIFIER :
                     // TODO - Kludgy way to handle constructors/destructors
                     // handle nested later:
                     if (flags.haveEncounteredRawType())
                         return;
                     if (parm && flags.haveEncounteredTypename())
                         return;
                     if (lookAheadForConstructorOrConversion(flags))
                         return;
                     if (lookAheadForDeclarator(flags))
                         return;
                     try
                     {
                         callback.simpleDeclSpecifier(decl, LA(1));
                     }
                     catch (Exception e)
                     {
                     }
                     ITokenDuple d = name();
                     sdw.setTypeName(d.toString());
                     try
                     {
                         callback.simpleDeclSpecifierName(decl);
                     }
                     catch (Exception e)
                     {
                     }
                     flags.setEncounteredTypename(true);
                     break;
                 case IToken.t_class :
                 case IToken.t_struct :
                 case IToken.t_union :
                     if (!parm)
                     {
                         try
                         {
                             classSpecifier(decl, sdw );
                             return;
                         }
                         catch (Backtrack bt)
                         {
                             elaboratedTypeSpecifier(decl, sdw);
                             flags.setEncounteredTypename(true);
                             break;
                         }
                     }
                     else
                     {
                         elaboratedTypeSpecifier(decl, sdw);
                         flags.setEncounteredTypename(true);
                         break;
                     }
                 case IToken.t_enum :
                     if (!parm)
                     {
                         try
                         {
                             enumSpecifier(decl, sdw);
                             break;
                         }
                         catch (Backtrack bt)
                         {
                             // this is an elaborated class specifier
                             elaboratedTypeSpecifier(decl, sdw);
                             flags.setEncounteredTypename(true);
                             break;
                         }
                     }
                     else
                     {
                         elaboratedTypeSpecifier(decl, sdw);
                         flags.setEncounteredTypename(true);
                         break;
                     }
                 default :
                     break declSpecifiers;
             }
         }
     }
     /**
      * Parse an elaborated type specifier.  
      * 
      * @param decl			Declaration which owns the elaborated type 
      * @throws Backtrack	request a backtrack
      */
     protected void elaboratedTypeSpecifier(Object decl, DeclarationWrapper sdw) throws Backtrack
     {
         // this is an elaborated class specifier
         Object elab = null;
         IToken t = consume();
         ClassKind eck = null; 
         
         switch( t.getType() )
         {
         	case Token.t_class:
 				eck = ClassKind.CLASS;
 				break;
         	case Token.t_struct:
 				eck = ClassKind.STRUCT;
 				break;
         	case Token.t_union:
 				eck = ClassKind.UNION;
 				break;        	
         	case Token.t_enum:
         		eck = ClassKind.ENUM;
         		break;
         	default: 
         		break;
         }
         try
         {
             elab = callback.elaboratedTypeSpecifierBegin(decl, t );
         }
         catch (Exception e)
         {
         }
         
         ITokenDuple d = name();
         
         IASTElaboratedTypeSpecifier elaboratedTypeSpec = astFactory.createElaboratedTypeSpecifier( eck, d.toString(), t.getOffset(), 
         		d.getLastToken().getEndOffset() );
         		
         sdw.setTypeSpecifier( elaboratedTypeSpec );
         
         requestor.acceptElaboratedTypeSpecifier( elaboratedTypeSpec );
         
         try
         {
             callback.elaboratedTypeSpecifierName(elab);
             callback.elaboratedTypeSpecifierEnd(elab);
         }
         catch (Exception e)
         {
         }
     }
     /**
      * Consumes template parameters.  
      *
      * @param previousLast	Previous "last" token (returned if nothing was consumed)
      * @return				Last consumed token, or <code>previousLast</code> if nothing was consumed
      * @throws Backtrack	request a backtrack
      */
     private IToken consumeTemplateParameters(IToken previousLast)
         throws Backtrack
     {
         IToken last = previousLast;
         if (LT(1) == IToken.tLT)
         {
             last = consume(IToken.tLT);
             // until we get all the names sorted out
             int depth = 1;
             while (depth > 0)
             {
                 last = consume();
                 switch (last.getType())
                 {
                     case IToken.tGT :
                         --depth;
                         break;
                     case IToken.tLT :
                         ++depth;
                         break;
                 }
             }
         }
         return last;
     }
     /**
      * Parse an identifier.  
      * 
      * @throws Backtrack	request a backtrack
      */
     protected IToken identifier() throws Backtrack
     {
         IToken first = consume(IToken.tIDENTIFIER);
         // throws backtrack if its not that
         try
         {
             callback.nameBegin(first);
             callback.nameEnd(first);
         }
         catch (Exception e)
         {
         }
         return first;
     }
     /**
      * Parses a className.  
      * 
      * class-name: identifier | template-id
      * 
      * @throws Backtrack
      */
     protected ITokenDuple className() throws Backtrack
     {
         if (LT(1) == IToken.tIDENTIFIER)
         {
             if (LT(2) == IToken.tLT)
             {
                 return new TokenDuple(LA(1), templateId());
             }
             else
             {
                 IToken t = identifier();
                 return new TokenDuple(t, t);
             }
         }
         else
             throw backtrack;
     }
     /**
      * Parse a template-id, according to the ANSI C++ spec.  
      * 
      * template-id: template-name < template-argument-list opt >
      * template-name : identifier
      * 
      * @return		the last token that we consumed in a successful parse 
      * 
      * @throws Backtrack	request a backtrack
      */
     protected IToken templateId() throws Backtrack
     {
         IToken first = consume(IToken.tIDENTIFIER);
         IToken last = consumeTemplateParameters(first);
         callback.nameBegin(first);
         callback.nameEnd(last);
         return last;
     }
     /**
      * Parse a name.
      * 
      * name
      * : ("::")? name2 ("::" name2)*
      * 
      * name2
      * : IDENTIFER
      * 
      * @throws Backtrack	request a backtrack
      */
     protected TokenDuple name() throws Backtrack
     {
         IToken first = LA(1);
         IToken last = null;
         IToken mark = mark();
         try
         {
             callback.nameBegin(first);
         }
         catch (Exception e)
         {
         }
         if (LT(1) == IToken.tCOLONCOLON)
             last = consume();
         // TODO - whacky way to deal with destructors, please revisit
         if (LT(1) == IToken.tCOMPL)
             consume();
         switch (LT(1))
         {
             case IToken.tIDENTIFIER :
                 last = consume();
                 last = consumeTemplateParameters(last);
                 break;
             default :
                 backup(mark);
                 throw backtrack;
         }
         while (LT(1) == IToken.tCOLONCOLON)
         {
             last = consume();
             if (LT(1) == IToken.t_template)
                 consume();
             if (LT(1) == IToken.tCOMPL)
                 consume();
             switch (LT(1))
             {
                 case IToken.t_operator :
                     backup(mark);
                     throw backtrack;
                 case IToken.tIDENTIFIER :
                     last = consume();
                     last = consumeTemplateParameters(last);
             }
         }
         try
         {
             callback.nameEnd(last);
         }
         catch (Exception e)
         {
         }
         return new TokenDuple(first, last);
     }
     /**
      * Parse a const-volatile qualifier.  
      * 
      * cvQualifier
      * : "const" | "volatile"
      * 
      * TODO: fix this 
      * @param ptrOp		Pointer Operator that const-volatile applies to. 		  		
      * @return			Returns the same object sent in.
      * @throws Backtrack
      */
     protected Object cvQualifier(Object ptrOp, PointerOperator po) throws Backtrack
     {
         switch (LT(1))
         {
             case IToken.t_const :
 				try
 				{
 					callback.pointerOperatorCVModifier(ptrOp, consume());
 				}
 				catch (Exception e)
 				{
 				}
 				po.setConst(true);
 				return ptrOp;
 
             case IToken.t_volatile :
                 try
                 {
                     callback.pointerOperatorCVModifier(ptrOp, consume());
                 }
                 catch (Exception e)
                 {
                 }
                 po.setVolatile( true );
                 return ptrOp;
             default :
                 throw backtrack;
         }
     }
     /**
      * Parses the initDeclarator construct of the ANSI C++ spec.
      * 
      * initDeclarator
      * : declarator ("=" initializerClause | "(" expressionList ")")?
      * @param owner			IParserCallback object that represents the owner declaration object.  
      * @return				declarator that this parsing produced.  
      * @throws Backtrack	request a backtrack
      */
     protected DeclaratorDuple initDeclarator(Object owner, DeclarationWrapper sdw)
         throws Backtrack
     {
         DeclaratorDuple duple = declarator(owner, sdw, null);
         Object declarator = duple.getObject();
         Declarator d = duple.getDeclarator();
       
         // handle = initializerClause
         if(LT(1) == IToken.tASSIGN )
         {
         	consume( IToken.tASSIGN );
             d.setInitializerClause( initializerClause( declarator ) );
         }           
         else if (LT(1) == IToken.tLPAREN)
         {
         	// initializer in constructor
             consume(IToken.tLPAREN); // EAT IT!
             Object expression = null;
             IASTExpression astExpression = null; 
             try
             {
                 try
                 {
                     expression = callback.expressionBegin(declarator);
                 }
                 catch (Exception e)
                 {
                 }
                 astExpression = expression(expression);
 				consume(IToken.tRPAREN);
                 try
                 {
                     callback.expressionEnd(expression);
                 }
                 catch (Exception e)
                 {
                 }
                 d.setConstructorExpression( astExpression );
             }
             catch (Backtrack b)
             {
                 if (expression != null)
                 {
                     try
                     {
                         callback.expressionAbort(expression);
                     }
                     catch (Exception e)
                     {
                     }
                     throw b;
                 }
             }
         }
         try
         {
             callback.declaratorEnd(declarator);
         }
         catch (Exception e)
         {
         }
         
 		sdw.addDeclarator(d);
         return duple; 
     }
 
     /**
 	 * 
 	 */
 	protected IASTInitializerClause initializerClause(Object declarator) throws Backtrack 
 	{
 		if (LT(1) == IToken.tLBRACE)
 		{
 			//TODO - parse this for real
 			consume(IToken.tLBRACE);
 			if( LT(1) == (IToken.tRBRACE ) )
 			{
 				consume( IToken.tRBRACE );
 				return astFactory.createInitializerClause( IASTInitializerClause.Kind.EMPTY, null, null );
 			}
 			
 			// otherwise it is a list of initializers
 			List initializerClauses = new ArrayList(); 
 			for( ; ; )
 			{
 				IASTInitializerClause clause = initializerClause( declarator ); 
 				initializerClauses.add( clause );
 				if( LT(1) == IToken.tRBRACE ) break;
 				consume( IToken.tCOMMA );
 			}
 			consume( IToken.tRBRACE );
 			return astFactory.createInitializerClause( IASTInitializerClause.Kind.INITIALIZER_LIST, null, initializerClauses );
 		}
 		// try this now instead
 		// assignmentExpression || { initializerList , } || { }
 		Object expression = null;
 		try
 		{
 			try
 			{
 				expression = callback.expressionBegin(declarator);
 			}
 			catch (Exception e)
 			{
 			}
 			
 			IToken marked = mark(); 
 			IASTExpression assignmentExpression  = assignmentExpression(expression);
 			
 			try
 			{
 				callback.expressionEnd(expression);
 			}
 			catch (Exception e)
 			{
 			}
 			
 			return astFactory.createInitializerClause( IASTInitializerClause.Kind.ASSIGNMENT_EXPRESSION, assignmentExpression, null );
 		}
 		catch (Backtrack b)
 		{
 			if (expression != null)
 				try
 				{
 					callback.expressionAbort(expression);
 				}
 				catch (Exception e)
 				{
 				}
 		}
 		
 		
 		
 		throw backtrack;
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
      * @throws Backtrack	request a backtrack
      */
     protected DeclaratorDuple declarator(Object container, DeclarationWrapper sdw, Declarator owningDeclarator) throws Backtrack
     {
 		Object declarator = null;
 		Declarator d = null;
 
 		overallLoop:
         do
         { 
 			declarator = null;
 			d = null;
            
             if( sdw != null ) 
             	d = new Declarator( sdw );
             else if( owningDeclarator != null )
             	d = new Declarator( owningDeclarator );
             	
             try
             {
                 declarator = callback.declaratorBegin(container);
             }
             catch (Exception e)
             {
             }
             for (;;)
             {
                 try
                 {
                     ptrOperator(declarator, d);
                 }
                 catch (Backtrack b)
                 {
                     break;
                 }
             }
             if (LT(1) == IToken.tLPAREN)
             {
                 consume();
                 DeclaratorDuple subDeclarator = declarator(declarator, null, d);
                 consume(IToken.tRPAREN);
                 try
                 {
                     callback.declaratorEnd(subDeclarator.getObject());
                 }
                 catch (Exception e)
                 {
                 }
             }
             else if (LT(1) == IToken.t_operator)
                   operatorId(declarator, d, null);
             else
             {
                 try
                 {
                     ITokenDuple duple = name();
                     d.setName( duple );
                     try
                     {
                         callback.declaratorId(declarator);
                     }
                     catch (Exception e)
                     {
                     }
                     
                 }
                 catch (Backtrack bt)
                 {
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
   							operatorId( declarator, d, start );
   						else
   						{
  							backup( mark );
   							throw backtrack;
   						} 
                     }
                 }
             }
             for (;;)
             {
                 switch (LT(1))
                 {
                     case IToken.tLPAREN :
                         // temporary fix for initializer/function declaration ambiguity
                         if (!LA(2).looksLikeExpression())
                         {
                             // parameterDeclarationClause
                             d.setIsFunction( true );
                             Object clause = null;
                             try
                             {
                                 clause = callback.argumentsBegin(declarator);
                             }
                             catch (Exception e)
                             {
                             }
                             consume();
                             boolean seenParameter = false;
                             parameterDeclarationLoop : for (;;)
                             {
                                 switch (LT(1))
                                 {
                                     case IToken.tRPAREN :
                                         consume();
                                         break parameterDeclarationLoop;
                                     case IToken.tELIPSE :
                                         consume();
                                         break;
                                     case IToken.tCOMMA :
                                         consume();
                                         seenParameter = false;
                                         break;
                                     default :
                                         if (seenParameter)
                                             throw backtrack;
                                         parameterDeclaration(clause, d);
                                         seenParameter = true;
                                 }
                             }
                             try
                             {
                                 callback.argumentsEnd(clause);
                             }
                             catch (Exception e)
                             {
                             }
                             if (LT(1) == IToken.tCOLON)
                             {
  								break overallLoop;
                             }
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
                                 try
                                 {
                                     callback.declaratorThrowsException(
                                         declarator);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                                 consume(); // throw
                                 consume(IToken.tLPAREN); // (
                                 boolean done = false;
                                 ITokenDuple duple = null;  
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
                                         	try {
                                             	duple = typeId();
                                             	exceptionSpecIds.add( duple );
                                         	} catch (Backtrack e) {
 												failParse();
 												Util.debugLog( "Unexpected Token =" + image );
 												consume(); // eat this token anyway
 												continue;
                                         	}
                                             
                                             try {	
                                                 callback
                                                     .declaratorThrowExceptionName(
                                                     declarator);
                                             }
                                             catch (Exception e)
                                             {
                                             }
                                             break;
                                     }
                                 }
                                 if( exceptionSpecIds != null )
                                 	d.setExceptionSpecification( astFactory.createExceptionSpecification( exceptionSpecIds ) );
                             }
                             
                             // check for optional pure virtual							
                             if (LT(1) == IToken.tASSIGN
                                 && LT(2) == IToken.tINTEGER
                                 && LA(2).getImage().equals("0"))
                             {
                                 consume(IToken.tASSIGN);
                                 consume(IToken.tINTEGER);
                                 d.setPureVirtual( true ); 
                                 try
                                 {
                                     callback.declaratorPureVirtual(declarator);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                             }
  
                             if ( afterCVModifier != LA(1) ||  LT(1) == IToken.tSEMI )
                             {
                                 // There were C++-specific clauses after const/volatile modifier
                                 // Then it is a marker for the method
                                 if ( cvModifier != null ) {
 	                                try
 	                                {
 	                                    callback.declaratorCVModifier(
 	                                        declarator,
 	                                        cvModifier);
 	                                }
 	                                catch (Exception e)
 	                                {
 	                                }
 	                                
 	                                if( cvModifier.getType() == IToken.t_const )
 	                                	d.setConst( true );
 									if( cvModifier.getType() == IToken.t_volatile )
 										d.setVolatile( true );
                                 }
 									
 								afterCVModifier = mark();
                                 
                                 // In this case (method) we can't expect K&R parameter declarations,
                                 // but we'll check anyway, for errorhandling
                             }
                             else
                             {
                                 // let's try this modifier as part of K&R parameter declaration
                                 if (cvModifier != null) backup(beforeCVModifier);
                             }
                             
                             if (LT(1) != IToken.tSEMI)
                             {
                                 // try K&R-style parameter declarations
                                 Object oldKRParameterDeclarationClause = null;
                                 try
                                 {
                                     oldKRParameterDeclarationClause =
                                         callback.oldKRParametersBegin(clause);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                                 try
                                 {
                                     do
                                     {
                                    	IASTScope s = null;
                                    	if( sdw != null )
                                    		s = sdw.getScope();
                                    	else
                                    	{
                                    		Declarator i = owningDeclarator; 
                                    		while( i.getOwnerDeclarator() != null )
                                    			i = i.getOwnerDeclarator(); 
                                    		s = i.getOwner().getScope();
                                    	}
                                    	
                                         simpleDeclaration(
                                             oldKRParameterDeclarationClause,
                                             false,
                                             true,
                                            s);
                                     }
                                     while (LT(1) != IToken.tLBRACE);
                                 }
                                 catch (Exception e)
                                 {
                                     // Something is wrong, 
                                     // this is not a proper K&R declaration clause
                                     backup(afterCVModifier);
                                 }
                                 try
                                 {
                                     callback.oldKRParametersEnd(
                                         oldKRParameterDeclarationClause);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                             }
                         }
                         break;
                     case IToken.tLBRACKET :
                         while (LT(1) == IToken.tLBRACKET)
                         {
                             consume(); // eat the '['
                             Object array = null;
                             try
                             {
                                 array =
                                     callback.arrayDeclaratorBegin(declarator);
                             }
                             catch (Exception e)
                             {
                             }
 							IASTExpression exp = null;
                             if (LT(1) != IToken.tRBRACKET)
                             {
                                 Object expression = null;
                                 try
                                 {
                                     expression =
                                         callback.expressionBegin(array);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                                 exp = constantExpression(expression);
                                 try
                                 {
                                     callback.expressionEnd(expression);
                                 }
                                 catch (Exception e)
                                 {
                                 }
                             }
                             consume(IToken.tRBRACKET);
 							IASTArrayModifier arrayMod = astFactory.createArrayModifier( exp ); 
 							d.addArrayModifier( arrayMod );
 
                             try
                             {
                                 callback.arrayDeclaratorEnd(array);
                             }
                             catch (Exception e)
                             {
                             }
                         }
                         continue;
                     case IToken.tCOLON :
                         consume(IToken.tCOLON);
                         Object bitfield = null;
                         try
                         {
                             bitfield = callback.startBitfield(declarator);
                         }
                         catch (Exception e)
                         {
                         }
                         Object expression = null;
                         IASTExpression exp = null; 
                         try
                         {
                             expression = callback.expressionBegin(bitfield);
                         }
                         catch (Exception e)
                         {
                         }
                         exp = constantExpression(expression);
                         try
                         {
                             callback.expressionEnd(expression);
                         }
                         catch (Exception e)
                         {
                         }
                         try
                         {
                             callback.endBitfield(bitfield);
                         }
                         catch (Exception e)
                         {
                         }
                         d.setBitFieldExpression( exp );
                     default :
                         break;
                 }
                 break;
             }
             if (LA(1).getType() == IToken.tIDENTIFIER)
             {
                 try
                 {
                     callback.declaratorAbort(declarator);
                 }
                 catch (Exception e)
                 {
                 }
                 declarator = null;
             }
             else
             {
             	break;
             }
         }
         while (true);
 		if( sdw == null )
 			owningDeclarator.setOwnedDeclarator(d);
 		return new DeclaratorDuple( declarator, d );
         
     }
     
     protected  void operatorId(Object declarator, Declarator d, IToken originalToken)
         throws Backtrack, EndOfFile
     {
           // we know this is an operator
             IToken operatorToken = consume(IToken.t_operator);
             IToken toSend = null;
             if (LA(1).isOperator()
                 || LT(1) == IToken.tLPAREN
                 || LT(1) == IToken.tLBRACKET)
             {
                 if ((LT(1) == IToken.t_new || LT(1) == IToken.t_delete)
                     && LT(2) == IToken.tLBRACKET
                     && LT(3) == IToken.tRBRACKET)
                 {
                     consume();
                     consume(IToken.tLBRACKET);
                     toSend = consume(IToken.tRBRACKET);
                     // vector new and delete operators
                 }
                 else if (
                     LT(1) == IToken.tLPAREN && LT(2) == IToken.tRPAREN)
                 {
                     // operator ()
                     consume(IToken.tLPAREN);
                     toSend = consume(IToken.tRPAREN);
                 }
                 else if (
                     LT(1) == IToken.tLBRACKET && LT(2) == IToken.tRBRACKET)
                 {
                     consume(IToken.tLBRACKET);
                     toSend = consume(IToken.tRBRACKET);
                 }
                 else if (LA(1).isOperator())
                     toSend = consume();
                 else
                     throw backtrack;
             }
             else
             {
                 // must be a conversion function
                 typeId();
                 toSend = lastToken;
                 try
                 {
                     // this ptrOp doesn't belong to the declarator, 
                     // it's just a part of the name
                     ptrOperator(null, d);
                     toSend = lastToken;
                 }
                 catch (Backtrack b)
                 {
                 }
                 // In case we'll need better error recovery 
                 // while( LT(1) != Token.tLPAREN )	{ toSend = consume(); }
             }
             ITokenDuple duple = new TokenDuple( originalToken == null ? operatorToken : originalToken ,toSend ); 
             try
             {
                 callback.nameBegin(originalToken == null ? operatorToken : originalToken );
                 callback.nameEnd(toSend);
             }
             catch (Exception e)
             {
             }
             try
             {
                 callback.declaratorId(declarator);
             }
             catch (Exception e)
             {
             }
 			d.setName( duple ); 
     }
     /**
      * Parse a Pointer Operator.   
      * 
      * ptrOperator
      * : "*" (cvQualifier)*
      * | "&"
      * | ::? nestedNameSpecifier "*" (cvQualifier)*
      * 
      * @param owner 		Declarator that this pointer operator corresponds to.  
      * @throws Backtrack 	request a backtrack
      */
     protected void ptrOperator(Object owner, Declarator d) throws Backtrack
     {
         int t = LT(1);
         Object ptrOp = null;
         
         try
         {
             ptrOp = callback.pointerOperatorBegin(owner);
         }
         catch (Exception e)
         {
         }
         if (t == IToken.tAMPER)
         {
             try
             {
                 callback.pointerOperatorType(ptrOp, consume(IToken.tAMPER));
 				callback.pointerOperatorEnd(ptrOp);
             }
             catch (Exception e)
             {
             }
 			d.addPtrOp( new PointerOperator( PointerOperator.Type.REFERENCE ) );
             return;
         }
         IToken mark = mark();
         IToken tokenType = LA(1);
         boolean hasName = false;
         ITokenDuple nameDuple = null;
         if (t == IToken.tIDENTIFIER || t == IToken.tCOLONCOLON)
         {
             callback.nameBegin(tokenType);
             nameDuple = name();
             callback.nameEnd(lastToken);
             hasName = true;
             t = LT(1);
         }
         if (t == IToken.tSTAR)
         {
         	PointerOperator po = null; 
             if (hasName)
             {
                 try
                 {
                 	 
                     callback.pointerOperatorName(ptrOp);
                 }
                 catch (Exception e)
                 {
                 }
                 // just consume "*", so tokenType is left as "::" or Id
                 po = new PointerOperator( PointerOperator.Type.NAMED );
                 po.setName( nameDuple.toString() );
                 consume(Token.tSTAR);
             }
             else
             {
                 tokenType = consume(Token.tSTAR); // tokenType = "*"
                 po = new PointerOperator( PointerOperator.Type.POINTER );
             }
             
             try
             {
                 callback.pointerOperatorType(ptrOp, tokenType);
             }
             catch (Exception e)
             {
             }
             for (;;)
             {
                 try
                 {
                     ptrOp = cvQualifier(ptrOp, po);
                 }
                 catch (Backtrack b)
                 {
                     // expected at some point
                     break;
                 }
             }
             try
             {
                 callback.pointerOperatorEnd(ptrOp);
             }
             catch (Exception e)
             {
             }
             d.addPtrOp(po);
             return;
         }
         backup(mark);
         try
         {
             callback.pointerOperatorAbort(ptrOp);
         }
         catch (Exception e)
         {
         }
         throw backtrack;
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
      * @throws	Backtrack	request a backtrack
      */
     protected void enumSpecifier(Object owner, DeclarationWrapper sdw) throws Backtrack
     {
         Object enumSpecifier = null;
         IToken mark = mark();
         IToken identifier = null; 
         try
         {
             enumSpecifier =
                 callback.enumSpecifierBegin(owner, consume(IToken.t_enum));
         }
         catch (Exception e)
         {
         }
         if (LT(1) == IToken.tIDENTIFIER)
         {
             identifier = identifier();
             try
             {
                 callback.enumSpecifierId(enumSpecifier);
             }
             catch (Exception e)
             {
             }
         }
         if (LT(1) == IToken.tLBRACE)
         {
         	IASTEnumerationSpecifier enumeration = astFactory.createEnumerationSpecifier( 
         		( ( identifier == null ) ? "" : identifier.getImage()), 
         		mark.getOffset(), 
 				( ( identifier == null ) ? mark.getOffset() : identifier.getOffset()) );
             consume(IToken.tLBRACE);
             while (LT(1) != IToken.tRBRACE)
             {
                 Object defn;
 				IToken enumeratorIdentifier = null;
                 if (LT(1) == IToken.tIDENTIFIER)
                 {
                 	
                     defn = null;
                     try
                     {
                         defn = callback.enumeratorBegin(enumSpecifier);
                     }
                     catch (Exception e)
                     {
                     }
 					enumeratorIdentifier = identifier();
                     try
                     {
                         callback.enumeratorId(defn);
                     }
                     catch (Exception e)
                     {
                     }
                 }
                 else
                 {
                     try
                     {
                         callback.enumSpecifierAbort(enumSpecifier);
                     }
                     catch (Exception e)
                     {
                     }
                     throw backtrack;
                 }
                 if (LT(1) == IToken.tASSIGN)
                 {
                     consume(IToken.tASSIGN);
                     Object expression = null;
                     try
                     {
                         expression = callback.expressionBegin(defn);
                     }
                     catch (Exception e)
                     {
                     }
                     constantExpression(expression);
                     try
                     {
                         callback.expressionEnd(expression);
                     }
                     catch (Exception e)
                     {
                     }
                 }
                 try
                 {
                     callback.enumeratorEnd(defn, lastToken);
                 }
                 catch (Exception e)
                 {
                 }
                 if (LT(1) == IToken.tRBRACE)
                 {
                 	astFactory.addEnumerator( enumeration, enumeratorIdentifier.toString(), enumeratorIdentifier.getOffset(), enumeratorIdentifier.getEndOffset() ); 
                     break;
                 }
                 if (LT(1) != IToken.tCOMMA)
                 {
                     try
                     {
                         callback.enumSpecifierAbort(enumSpecifier);
                     }
                     catch (Exception e)
                     {
                     }
                     throw backtrack;
                 }
 				astFactory.addEnumerator( enumeration, enumeratorIdentifier.toString(), enumeratorIdentifier.getOffset(), enumeratorIdentifier.getEndOffset() ); 
                 consume(IToken.tCOMMA);
             }
             
 			IToken t = consume(IToken.tRBRACE);
             try
             {
             	
                 callback.enumSpecifierEnd(
                     enumSpecifier,
                     t );
             }
             catch (Exception e)
             {
             }
 			enumeration.setEndingOffset( t.getEndOffset() );
 			requestor.acceptEnumerationSpecifier( enumeration );
 			sdw.setTypeSpecifier( enumeration );
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
      * @throws	Backtrack	request a backtrack
      */
     protected void classSpecifier(Object owner, DeclarationWrapper sdw )
         throws Backtrack
     {
         ClassNameType nameType = ClassNameType.IDENTIFIER;
         ClassKind classKind = null;
         AccessVisibility access = AccessVisibility.PUBLIC;
         IToken classKey = null;
         IToken mark = mark();
         // class key
         switch (LT(1))
         {
             case IToken.t_class :
                 classKey = consume();
                 classKind = ClassKind.CLASS;
                 access = AccessVisibility.PRIVATE;
                 break;
             case IToken.t_struct :
                 classKey = consume();
                 classKind = ClassKind.STRUCT;
                 break;
             case IToken.t_union :
                 classKey = consume();
                 classKind = ClassKind.UNION;
                 break;
             default :
                 throw backtrack;
         }
         Object classSpec = null;
         try
         {
             classSpec = callback.classSpecifierBegin(owner, classKey);
         }
         catch (Exception e)
         {
         }
         ITokenDuple duple = null;
         // class name
         if (LT(1) == IToken.tIDENTIFIER)
         {
             duple = className();
             try
             {
                 callback.classSpecifierName(classSpec);
             }
             catch (Exception e)
             {
             }
         }
         if (duple != null && !duple.isIdentifier())
             nameType = ClassNameType.TEMPLATE;
         if (LT(1) != IToken.tCOLON && LT(1) != IToken.tLBRACE)
         {
             // this is not a classSpecification
             try
             {
                 callback.classSpecifierAbort(classSpec);
             }
             catch (Exception e)
             {
             }
             classSpec = null;
             backup(mark);
             throw backtrack;
         }
         IASTClassSpecifier astClassSpecifier =
             astFactory
                 .createClassSpecifier(
                     sdw.getScope(),
                     duple == null ? "" : duple.toString(),
                     classKind,
                     nameType,
                     access,
                     null,            //TODO add TemplateDeclaration here
    					 classKey.getOffset(),
  			       duple == null ? 0 : duple.getFirstToken().getOffset());
  		sdw.setTypeSpecifier(astClassSpecifier);
         // base clause
         if (LT(1) == IToken.tCOLON)
         {
             baseSpecifier(classSpec, astClassSpecifier);
         }
         if (LT(1) == IToken.tLBRACE)
         {
             consume(IToken.tLBRACE);
             requestor.enterClassSpecifier(astClassSpecifier);
             memberDeclarationLoop : while (LT(1) != IToken.tRBRACE)
             {
                 IToken checkToken = LA(1);
                 switch (LT(1))
                 {
                     case IToken.t_public :
                     case IToken.t_protected :
                     case IToken.t_private :
                         try
                         {
                             callback.classMemberVisibility(
                                 classSpec,
                                 consume());
                         }
                         catch (Exception e)
                         {
                         }
                         consume(IToken.tCOLON);
                         break;
                     case IToken.tRBRACE :
                         consume(IToken.tRBRACE);
                         break memberDeclarationLoop;
                     default :
                         try
                         {
                            declaration(classSpec,astClassSpecifier);
                         }
                         catch (Backtrack bt)
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
             IToken lastToken = consume(IToken.tRBRACE);
             try
             {
                 callback.classSpecifierEnd(classSpec, lastToken);
             }
             catch (Exception e)
             {
             }
             astClassSpecifier.setEndingOffset(lastToken.getEndOffset());
             requestor.exitClassSpecifier(astClassSpecifier);
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
      * @throws Backtrack
      */
     protected void baseSpecifier(
         Object classSpecOwner,
         IASTClassSpecifier astClassSpec)
         throws Backtrack
     {
         consume(IToken.tCOLON);
         Object baseSpecifier = null;
         try
         {
             baseSpecifier = callback.baseSpecifierBegin(classSpecOwner);
         }
         catch (Exception e)
         {
         }
         boolean isVirtual = false;
         AccessVisibility visibility = AccessVisibility.PUBLIC;
         ITokenDuple nameDuple = null;
         baseSpecifierLoop : for (;;)
         {
             switch (LT(1))
             {
                 case IToken.t_virtual :
                     consume(IToken.t_virtual);
                     isVirtual = true;
                     try
                     {
                         callback.baseSpecifierVirtual(baseSpecifier, true);
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_public :
                     try
                     {
                         callback.baseSpecifierVisibility(
                             baseSpecifier,
                             consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.t_protected :
                     try
                     {
                         callback.baseSpecifierVisibility(
                             baseSpecifier,
                             consume());
                     }
                     catch (Exception e)
                     {
                     }
                     visibility = AccessVisibility.PROTECTED;
                     break;
                 case IToken.t_private :
                     visibility = AccessVisibility.PRIVATE;
                     try
                     {
                         callback.baseSpecifierVisibility(
                             baseSpecifier,
                             consume());
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.tCOLONCOLON :
                 case IToken.tIDENTIFIER :
                     nameDuple = name();
                     try
                     {
                         callback.baseSpecifierName(baseSpecifier);
                     }
                     catch (Exception e)
                     {
                     }
                     break;
                 case IToken.tCOMMA :
                     try
                     {
                         astFactory.addBaseSpecifier(
                             astClassSpec,
                             isVirtual,
                             visibility,
                             nameDuple.toString());
                         isVirtual = false;
                         visibility = AccessVisibility.PUBLIC;
                         nameDuple = null;
                         callback.baseSpecifierEnd(baseSpecifier);
                         baseSpecifier =
                             callback.baseSpecifierBegin(classSpecOwner);
                     }
                     catch (Exception e)
                     {
                     }
                     consume();
                     continue baseSpecifierLoop;
                 default :
                     break baseSpecifierLoop;
             }
         }
         try
         {
             callback.baseSpecifierEnd(baseSpecifier);
         }
         catch (Exception e)
         {
         }
         astFactory.addBaseSpecifier(
             astClassSpec,
             isVirtual,
             visibility,
             nameDuple.toString());
     }
     /**
      * Parses a function body. 
      * 
      * @throws Backtrack	request a backtrack
      */
     protected void functionBody() throws Backtrack
     {
         compoundStatement();
     }
     /**
      * Parses a statement. 
      * 
      * @throws Backtrack	request a backtrack
      */
     protected void statement() throws Backtrack
     {
         Object expression = null;
         switch (LT(1))
         {
             case IToken.t_case :
                 consume();
                 // TODO regarding this null
                 try
                 {
                     expression = callback.expressionBegin(null);
                 }
                 catch (Exception e)
                 {
                 }
                 constantExpression(expression);
                 try
                 {
                     callback.expressionEnd(expression);
                 }
                 catch (Exception e)
                 {
                 }
                 consume(IToken.tCOLON);
                 statement();
                 return;
             case IToken.t_default :
                 consume();
                 consume(IToken.tCOLON);
                 statement();
                 return;
             case IToken.tLBRACE :
                 compoundStatement();
                 return;
             case IToken.t_if :
                 consume();
                 consume(IToken.tLPAREN);
                 condition();
                 consume(IToken.tRPAREN);
                 statement();
                 if (LT(1) == IToken.t_else)
                 {
                     consume();
                     statement();
                 }
                 return;
             case IToken.t_switch :
                 consume();
                 consume(IToken.tLPAREN);
                 condition();
                 consume(IToken.tRPAREN);
                 statement();
                 return;
             case IToken.t_while :
                 consume();
                 consume(IToken.tLPAREN);
                 condition();
                 consume(IToken.tRPAREN);
                 statement();
                 return;
             case IToken.t_do :
                 consume();
                 statement();
                 consume(IToken.t_while);
                 consume(IToken.tLPAREN);
                 condition();
                 consume(IToken.tRPAREN);
                 return;
             case IToken.t_for :
                 consume();
                 consume(IToken.tLPAREN);
                 forInitStatement();
                 if (LT(1) != IToken.tSEMI)
                     condition();
                 consume(IToken.tSEMI);
                 if (LT(1) != IToken.tRPAREN)
                 {
                     try
                     {
                         expression = callback.expressionBegin(null);
                     }
                     catch (Exception e)
                     {
                     }
                     //TODO get rid of NULL  
                     expression(expression);
                     try
                     {
                         callback.expressionEnd(expression);
                     }
                     catch (Exception e)
                     {
                     }
                 }
                 consume(IToken.tRPAREN);
                 statement();
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
                     try
                     {
                         expression = callback.expressionBegin(null);
                     }
                     catch (Exception e)
                     {
                     }
                     //TODO get rid of NULL  
                     expression(expression);
                     try
                     {
                         callback.expressionEnd(expression);
                     }
                     catch (Exception e)
                     {
                     }
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
                 compoundStatement();
                 while (LT(1) == IToken.t_catch)
                 {
                     consume();
                     consume(IToken.tLPAREN);
                     declaration(null); // was exceptionDeclaration
                     consume(IToken.tRPAREN);
                     compoundStatement();
                 }
                 return;
             case IToken.tSEMI :
                 consume();
                 return;
             default :
                 // can be many things:
                 // label
                 if (LT(1) == IToken.tIDENTIFIER && LT(2) == IToken.tCOLON)
                 {
                     consume();
                     consume();
                     statement();
                     return;
                 }
                 // expressionStatement
                 // Note: the function style cast ambiguity is handled in expression
                 // Since it only happens when we are in a statement
                 try
                 {
                     try
                     {
                         expression = callback.expressionBegin(null);
                     }
                     catch (Exception e)
                     {
                     }
                     //TODO get rid of NULL  
                     expression(expression);
                     try
                     {
                         callback.expressionEnd(expression);
                     }
                     catch (Exception e)
                     {
                     }
                     consume(IToken.tSEMI);
                     return;
                 }
                 catch (Backtrack b)
                 {
                 }
                 // declarationStatement
                 declaration(null);
         }
     }
     /**
      * @throws Backtrack
      */
     protected void condition() throws Backtrack
     {
         // TO DO
     }
     /**
      * @throws Backtrack
      */
     protected void forInitStatement() throws Backtrack
     {
         // TO DO
     }
     /**
      * @throws Backtrack
      */
     protected void compoundStatement() throws Backtrack
     {
         consume(IToken.tLBRACE);
         while (LT(1) != IToken.tRBRACE)
             statement();
         consume();
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression constantExpression(Object expression) throws Backtrack
     {
         return conditionalExpression(expression);
     }
     
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#expression(java.lang.Object)
      */
     public IASTExpression expression(Object expression) throws Backtrack
     {
         IASTExpression assignmentExpression = assignmentExpression(expression);
         while (LT(1) == IToken.tCOMMA)
         {
             IToken t = consume();
             IASTExpression secondExpression = assignmentExpression(expression);
             
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
             assignmentExpression = astFactory.createExpression( IASTExpression.Kind.EXPRESSIONLIST, assignmentExpression, secondExpression, null, "", "", "", null );
         }
         return assignmentExpression; 
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression assignmentExpression(Object expression) throws Backtrack
     {
         if (LT(1) == IToken.t_throw)
         {
             return throwExpression(expression);
         }
         IASTExpression conditionalExpression = conditionalExpression(expression); 
         // if the condition not taken, try assignment operators
         if (conditionalExpression != null && conditionalExpression.getExpressionKind() == IASTExpression.Kind.CONDITIONALEXPRESSION_HARD )
 			return conditionalExpression;
         
         switch (LT(1))
         {
             case IToken.tASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_NORMAL );
             case IToken.tSTARASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_MULT );
             case IToken.tDIVASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_DIV );
             case IToken.tMODASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_MOD );
             case IToken.tPLUSASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_PLUS );
             case IToken.tMINUSASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_MINUS );
             case IToken.tSHIFTRASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_RSHIFT );
             case IToken.tSHIFTLASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_LSHIFT );
             case IToken.tAMPERASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_AND );
             case IToken.tXORASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_XOR );
             case IToken.tBITORASSIGN :
 				return assignmentOperatorExpression(expression, IASTExpression.Kind.ASSIGNMENTEXPRESSION_OR );
         }
 		return conditionalExpression;
         	
     }
     
 	protected IASTExpression assignmentOperatorExpression(Object expression, IASTExpression.Kind kind )
 		throws EndOfFile, Backtrack {
 		IToken t = consume();
 		IASTExpression assignmentExpression = assignmentExpression(expression);
 		try
 		{
 		    callback.expressionOperator(expression, t);
 		}
 		catch (Exception e)
 		{
 		}
 		return astFactory.createExpression( kind, assignmentExpression, null, null, "", "", "", null );
 	}
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression throwExpression(Object expression) throws Backtrack
     {
         consume(IToken.t_throw);
         IASTExpression throwExpression = null; 
         try
         {
             throwExpression = expression(expression);
         }
         catch (Backtrack b)
         {
         }
         return astFactory.createExpression( IASTExpression.Kind.THROWEXPRESSION, throwExpression, null, null, "", "", "", null );
     }
     /**
      * @param expression
      * @return
      * @throws Backtrack
      */
     protected IASTExpression conditionalExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = logicalOrExpression(expression);
         if (LT(1) == IToken.tQUESTION)
         {
             consume();
             IASTExpression secondExpression = expression(expression);
             consume(IToken.tCOLON);
             IASTExpression thirdExpression = assignmentExpression(expression);
             return astFactory.createExpression( IASTExpression.Kind.CONDITIONALEXPRESSION_HARD, firstExpression, secondExpression, thirdExpression, "","","",null);
         }
         else
             return firstExpression;
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression logicalOrExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = logicalAndExpression(expression);
         
         while (LT(1) == IToken.tOR)
         {
             IToken t = consume();
             IASTExpression secondExpression = logicalAndExpression(expression);
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
             firstExpression = astFactory.createExpression( IASTExpression.Kind.LOGICALOREXPRESSION, firstExpression, secondExpression, null, "", "", "", null );  
         }
         return firstExpression;
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression logicalAndExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = inclusiveOrExpression(expression);
         while (LT(1) == IToken.tAND)
         {
             IToken t = consume();
             IASTExpression secondExpression = inclusiveOrExpression(expression);
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
 			firstExpression = astFactory.createExpression( IASTExpression.Kind.LOGICALANDEXPRESSION, firstExpression, secondExpression, null, "", "", "", null );
         }
         return firstExpression;
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression inclusiveOrExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = exclusiveOrExpression(expression);
         while (LT(1) == IToken.tBITOR)
         {
             IToken t = consume();
             IASTExpression secondExpression = exclusiveOrExpression(expression);
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
 			firstExpression = astFactory.createExpression( IASTExpression.Kind.INCLUSIVEOREXPRESSION, firstExpression, secondExpression, null, "", "", "", null );
         }
         return firstExpression; 
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression exclusiveOrExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = andExpression(expression);
         while (LT(1) == IToken.tXOR)
         {
             IToken t = consume();
 			IASTExpression secondExpression = andExpression(expression);
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
 			firstExpression = astFactory.createExpression( IASTExpression.Kind.EXCLUSIVEOREXPRESSION, firstExpression, secondExpression, null, "", "", "", null );
         }
         return firstExpression; 
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression andExpression(Object expression) throws Backtrack
     {
 		IASTExpression firstExpression = equalityExpression(expression);
         while (LT(1) == IToken.tAMPER)
         {
             IToken t = consume();
 			IASTExpression secondExpression = equalityExpression(expression);
             try
             {
                 callback.expressionOperator(expression, t);
             }
             catch (Exception e)
             {
             }
 			firstExpression = astFactory.createExpression( IASTExpression.Kind.ANDEXPRESSION, firstExpression, secondExpression, null, "", "", "", null );
         }
         return firstExpression;
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression equalityExpression(Object expression) throws Backtrack
     {
 		IASTExpression firstExpression = relationalExpression(expression);
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tEQUAL :
 				case IToken.tNOTEQUAL :
                     IToken t = consume();
 					IASTExpression secondExpression = relationalExpression(expression);
                     try
                     {
                         callback.expressionOperator(expression, t);
                     }
                     catch (Exception e)
                     {
                     }
 					firstExpression = astFactory.createExpression( ( t.getType() == IToken.tEQUAL ) ? IASTExpression.Kind.EQUALITY_EQUALS : IASTExpression.Kind.EQUALITY_NOTEQUALS,
 						 firstExpression, secondExpression, null, "", "", "", null );
                     break;
                 default :
                     return firstExpression;
             }
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression relationalExpression(Object expression) throws Backtrack
     {
 		IASTExpression firstExpression = shiftExpression(expression); 
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tGT :
                 case IToken.tLT :
                 case IToken.tLTEQUAL :
                 case IToken.tGTEQUAL :
                     IToken mark = mark();
                     IToken t = consume();
                     IToken next = LA(1);
                     IASTExpression secondExpression = shiftExpression(expression);
                     if (next == LA(1))
                     {
                         // we did not consume anything
                         // this is most likely an error
                         backup(mark);
                         return firstExpression;
                     }
                     else
                     {
                         try
                         {
                             callback.expressionOperator(expression, t);
                         }
                         catch (Exception e)
                         {
                         }
                         
                         IASTExpression.Kind kind = null; 
                         switch( t.getType() )
                         {
 							case IToken.tGT :
 								kind = IASTExpression.Kind.RELATIONAL_GREATERTHAN;
 								break;
 								
 							case IToken.tLT :
 								kind = IASTExpression.Kind.RELATIONAL_LESSTHAN;
 								break;
 
 							case IToken.tLTEQUAL :
 								kind = IASTExpression.Kind.RELATIONAL_LESSTHANEQUALTO;
 								break;
 
 							case IToken.tGTEQUAL :
 								kind = IASTExpression.Kind.RELATIONAL_GREATERTHANEQUALTO;
 								break;
 
                         }
                         
                         firstExpression = astFactory.createExpression( kind, firstExpression, secondExpression, null, "", "", "", null );
                     }
                     break;
                 default :
                     return firstExpression;
             }
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression shiftExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = additiveExpression(expression);
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tSHIFTL :
                 case IToken.tSHIFTR :
                     IToken t = consume();
                     IASTExpression secondExpression = additiveExpression(expression);
                     try
                     {
                         callback.expressionOperator(expression, t);
                     }
                     catch (Exception e)
                     {
                     }
                     firstExpression = astFactory.createExpression( ( ( t.getType() == IToken.tSHIFTL ) ? IASTExpression.Kind.SHIFT_LEFT : IASTExpression.Kind.SHIFT_RIGHT ), 
                       firstExpression, secondExpression, null, "", "", "", null );  
                     break;
                 default :
                     return firstExpression ;
             }
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression additiveExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = multiplicativeExpression(expression);
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tPLUS :
                 case IToken.tMINUS :
                     IToken t = consume();
                     IASTExpression secondExpression = multiplicativeExpression(expression);
                     try
                     {
                         callback.expressionOperator(expression, t);
                     }
                     catch (Exception e)
                     {
                     }
 					firstExpression = astFactory.createExpression( ( ( t.getType() == IToken.tPLUS ) ? IASTExpression.Kind.ADDITIVE_PLUS : IASTExpression.Kind.ADDITIVE_MINUS), 
 						  firstExpression, secondExpression, null, "", "", "", null );  
 
                     break;
                 default :
                     return firstExpression;
             }
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression multiplicativeExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = pmExpression(expression);
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tSTAR :
                 case IToken.tDIV :
                 case IToken.tMOD :
                     IToken t = consume();
                     IASTExpression secondExpression = pmExpression(expression);
                     try
                     {
                         callback.expressionOperator(expression, t);
                     }
                     catch (Exception e)
                     {
                     }
     
 	                    
 					IASTExpression.Kind kind = null; 
 					switch( t.getType() )
 					{
 						case IToken.tSTAR :
 							kind = IASTExpression.Kind.MULTIPLICATIVE_MULTIPLY;
 							break;
 									
 						case IToken.tDIV :
 							kind = IASTExpression.Kind.MULTIPLICATIVE_DIVIDE;
 							break;
 	
 						case IToken.tMOD :
 							kind = IASTExpression.Kind.MULTIPLICATIVE_MODULUS;
 							break;
 	
 					}
 	                        
 					firstExpression = astFactory.createExpression( kind, firstExpression, secondExpression, null, "", "", "", null );
 					break;
                     
                 default :
                     return firstExpression;
             }
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression pmExpression(Object expression) throws Backtrack
     {
         IASTExpression firstExpression = castExpression(expression);
         for (;;)
         {
             switch (LT(1))
             {
                 case IToken.tDOTSTAR :
                 case IToken.tARROWSTAR :
                     IToken t = consume();
                     IASTExpression secondExpression = castExpression(expression);
                     try
                     {
                         callback.expressionOperator(expression, t);
                     }
                     catch (Exception e)
                     {
                     }
 					firstExpression = astFactory.createExpression( ( ( t.getType() == IToken.tDOTSTAR ) ? IASTExpression.Kind.PM_DOTSTAR : IASTExpression.Kind.PM_ARROWSTAR), 
 						  firstExpression, secondExpression, null, "", "", "", null );  
 
                     break;
                 default :
                     return firstExpression;
             }
         }
     }
     /**
      * castExpression
      * : unaryExpression
      * | "(" typeId ")" castExpression
      */
     protected IASTExpression castExpression(Object expression) throws Backtrack
     {
         // TO DO: we need proper symbol checkint to ensure type name
         if (LT(1) == IToken.tLPAREN)
         {
             IToken mark = mark();
             consume();
             ITokenDuple duple = null; 
             // If this isn't a type name, then we shouldn't be here
             try
             {
                 if (LT(1) == IToken.t_const)
                     consume();
                 duple = typeId();
                 while (LT(1) == IToken.tSTAR)
                 {
                     consume(IToken.tSTAR);
                     if (LT(1) == IToken.t_const || LT(1) == IToken.t_volatile)
                         consume();
                 }
                 consume(IToken.tRPAREN);
                 IASTExpression castExpression = castExpression(expression);
                 return astFactory.createExpression( IASTExpression.Kind.CASTEXPRESSION, castExpression, null, null, null, duple.toString(), "", null );
             }
             catch (Backtrack b)
             {
                 backup(mark);
             }
         }
         return unaryExpression(expression);
     }
     /**
      * @throws Backtrack
      */
     protected ITokenDuple typeId() throws Backtrack
     {
 		IToken begin = LA(1);
 		IToken end = null;
 
         try
         {
             ITokenDuple d = name();
             return d;
         }
         catch (Backtrack b)
         {
             simpleMods : for (;;)
             {
                 switch (LT(1))
                 {
                     case IToken.t_short :
                     case IToken.t_unsigned :
                     case IToken.t_long :
                     case IToken.t_const :
                         end = consume();
                         break;
                     case IToken.tAMPER :
                     case IToken.tSTAR :
                     case IToken.tIDENTIFIER :
                         if (end == null)
                             throw backtrack;
                         end = consume();
                         break;
                     case IToken.t_int :
                     case IToken.t_char :
                     case IToken.t_bool :
                     case IToken.t_double :
                     case IToken.t_float :
                     case IToken.t_wchar_t :
                     case IToken.t_void :
                         end = consume();
                     default :
                         break simpleMods;
                 }
             }
             if (end != null)
             {
                 try
                 {
                     callback.nameBegin(begin);
                     callback.nameEnd(end);
                 }
                 catch (Exception e)
                 {
                 }
                 return new TokenDuple( begin, end );
             }
             else if (LT(1) == IToken.t_typename || LT(1) == IToken.t_struct || LT(1) == IToken.t_class || LT(1) == IToken.t_enum || LT(1) == IToken.t_union )
             {
             	consume();
 				ITokenDuple d = name();
 				return new TokenDuple( begin, d.getLastToken() );
             }
             else
                 throw backtrack;
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression deleteExpression(Object expression) throws Backtrack
     {
         if (LT(1) == IToken.tCOLONCOLON)
         {
             // global scope
             consume();
         }
         consume(IToken.t_delete);
         boolean vectored = false; 
         if (LT(1) == IToken.tLBRACKET)
         {
             // array delete
             consume();
             consume(IToken.tRBRACKET);
             vectored = true;
         }
         IASTExpression castExpression = castExpression(expression);
         return astFactory.createExpression( ( vectored ? IASTExpression.Kind.DELETE_VECTORCASTEXPRESSION : IASTExpression.Kind.DELETE_CASTEXPRESSION ), 
         	castExpression, null, null, "", "", "", null ); 
     }
     /**
      * Pazse a new-expression.  
      * 
      * @param expression
      * @throws Backtrack
      * 
      * 
      * newexpression: 	::? new newplacement? newtypeid newinitializer?
      *					::? new newplacement? ( typeid ) newinitializer?
      * newplacement:	( expressionlist )
      * newtypeid:		typespecifierseq newdeclarator?
      * newdeclarator:	ptroperator newdeclarator? | directnewdeclarator
      * directnewdeclarator:		[ expression ]
      *							directnewdeclarator [ constantexpression ]
      * newinitializer:	( expressionlist? )
      */
     protected IASTExpression newExpression(Object expression) throws Backtrack
     {
         if (LT(1) == IToken.tCOLONCOLON)
         {
             // global scope
             consume();
         }
         consume(IToken.t_new);
         
         boolean typeIdInParen = false;
         boolean placementParseFailure = true;
         
         IToken beforeSecondParen = null;
         IToken backtrackMarker = null;
         
         if (LT(1) == IToken.tLPAREN)
         {
             consume(IToken.tLPAREN);
             try
             {
                 // Try to consume placement list
                 // Note: since expressionList and expression are the same...
                 backtrackMarker = mark();
                 expression(expression);
                 consume(IToken.tRPAREN);
                 placementParseFailure = false;
                 if (LT(1) == IToken.tLPAREN)
                 {
                     beforeSecondParen = mark();
                     consume(IToken.tLPAREN);
                     typeIdInParen = true;
                 }
             }
             catch (Backtrack e)
             {
                 backup(backtrackMarker);
             }
             if (placementParseFailure)
             {
                 // CASE: new (typeid-not-looking-as-placement) ...
                 // the first expression in () is not a placement
                 // - then it has to be typeId
                 typeId();
                 consume(IToken.tRPAREN);
             }
             else
             {
                 if (!typeIdInParen)
                 {
                     if (LT(1) == IToken.tLBRACKET)
                     {
                         // CASE: new (typeid-looking-as-placement) [expr]...
                         // the first expression in () has been parsed as a placement;
                         // however, we assume that it was in fact typeId, and this 
                         // new statement creates an array.
                         // Do nothing, fallback to array/initializer processing
                     }
                     else
                     {
                         // CASE: new (placement) typeid ...
                         // the first expression in () is parsed as a placement,
                         // and the next expression doesn't start with '(' or '['
                         // - then it has to be typeId
                         try
                         {
                             backtrackMarker = mark();
                             typeId();
                         }
                         catch (Backtrack e)
                         {
                             // Hmmm, so it wasn't typeId after all... Then it is
                             // CASE: new (typeid-looking-as-placement)
                             backup(backtrackMarker);
                             return null; // TODO fix this
                         }
                     }
                 }
                 else
                 {
                     // Tricky cases: first expression in () is parsed as a placement,
                     // and the next expression starts with '('.
                     // The problem is, the first expression might as well be a typeid
                     try
                     {
                         typeId();
                         consume(IToken.tRPAREN);
                         if (LT(1) == IToken.tLPAREN
                             || LT(1) == IToken.tLBRACKET)
                         {
                             // CASE: new (placement)(typeid)(initializer)
                             // CASE: new (placement)(typeid)[] ...
                             // Great, so far all our assumptions have been correct
                             // Do nothing, fallback to array/initializer processing
                         }
                         else
                         {
                             // CASE: new (placement)(typeid)
                             // CASE: new (typeid-looking-as-placement)(initializer-looking-as-typeid)
                             // Worst-case scenario - this cannot be resolved w/o more semantic information.
                             // Luckily, we don't need to know what was that - we only know that 
                             // new-expression ends here.
                             return null;  // TODO fix this
                         }
                     }
                     catch (Backtrack e)
                     {
                         // CASE: new (typeid-looking-as-placement)(initializer-not-looking-as-typeid)
                         // Fallback to initializer processing
                         backup(beforeSecondParen);
                     }
                 }
             }
         }
         else
         {
             // CASE: new typeid ...
             // new parameters do not start with '('
             // i.e it has to be a plain typeId
             typeId();
         }
         while (LT(1) == IToken.tLBRACKET)
         {
             // array new
             consume();
             assignmentExpression(expression);
             consume(IToken.tRBRACKET);
         }
         // newinitializer
         if (LT(1) == IToken.tLPAREN)
         {
             consume(IToken.tLPAREN);
             if (LT(1) != IToken.tRPAREN)
                 expression(expression);
             consume(IToken.tRPAREN);
         }
         return null; //TODO fix this 
     }
     
     
     protected IASTExpression unaryOperatorCastExpression( Object expression, IASTExpression.Kind kind, IToken consumed ) throws Backtrack
     {
 		IASTExpression castExpression = castExpression(expression);
 		try
 		{
 			callback.expressionOperator(expression, consumed);
 		}
 		catch (Exception e)
 		{
 		}
 		return astFactory.createExpression( kind, castExpression, null, null, "", "", "", null );
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression unaryExpression(Object expression) throws Backtrack
     {
         switch (LT(1))
         {
             case IToken.tSTAR :
 				return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_STAR_CASTEXPRESSION, consume() );
             case IToken.tAMPER :
 		    	return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_AMPSND_CASTEXPRESSION, consume() );
             case IToken.tPLUS :
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_PLUS_CASTEXPRESSION, consume() );
             case IToken.tMINUS :
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_MINUS_CASTEXPRESSION, consume() );
             case IToken.tNOT :
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_NOT_CASTEXPRESSION, consume() );
             case IToken.tCOMPL :
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_TILDE_CASTEXPRESSION, consume() );
             case IToken.tINCR :
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_INCREMENT, consume() );
             case IToken.tDECR :            
 			    return unaryOperatorCastExpression( expression, IASTExpression.Kind.UNARY_DECREMENT, consume() );
                   
             case IToken.t_sizeof :
                 consume(IToken.t_sizeof);
                 IToken mark = LA(1);
                 ITokenDuple d = null;
                 IASTExpression unaryExpression = null;   
                 if (LT(1) == IToken.tLPAREN)
                 {
                     try
                     {
                         consume(IToken.tLPAREN);
                         d = typeId();
                         consume(IToken.tRPAREN);
                     }
                     catch (Backtrack bt)
                     {
                         backup(mark);
                         unaryExpression = unaryExpression(expression);
                     }
                 }
                 else
                 {
                     unaryExpression = unaryExpression(expression);
                 }
                 
                 if( d != null & unaryExpression == null )
                 	return astFactory.createExpression( IASTExpression.Kind.UNARY_SIZEOF_TYPEID, null, null, null, "", d.toString(), "", null );
                 else if( unaryExpression != null && d == null )
 					return astFactory.createExpression( IASTExpression.Kind.UNARY_SIZEOF_UNARYEXPRESSION, unaryExpression, null, null, "", "", "", null );
                 else
                 	throw backtrack;
                 
             case IToken.t_new :
                 return newExpression(expression);
             case IToken.t_delete :
                 return deleteExpression(expression);
             case IToken.tCOLONCOLON :
                 switch (LT(2))
                 {
                     case IToken.t_new :
                         return newExpression(expression);
                     case IToken.t_delete :
                         return deleteExpression(expression);
                     default :
                         return postfixExpression(expression);
                 }
             default :
                 return postfixExpression(expression);
         }
     }
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression postfixExpression(Object expression) throws Backtrack
     {
     	IASTExpression firstExpression = null;
 		boolean isTemplate = false; 
         switch (LT(1))
         {
             case IToken.t_typename :
                 consume(); //TODO: the rest of this 
                 break;
              // simple-type-specifier ( assignment-expression , .. )
             case IToken.t_char :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_CHAR );
 				break;
             case IToken.t_wchar_t :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_WCHART );
 				break;
             case IToken.t_bool :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_BOOL);
 				break;
             case IToken.t_short :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_SHORT);
 				break;
             case IToken.t_int :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_INT);
 				break;
             case IToken.t_long :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_LONG);
 				break;
             case IToken.t_signed :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_SIGNED);
 				break;
             case IToken.t_unsigned :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_UNSIGNED);
 				break;
             case IToken.t_float :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_FLOAT);
 				break;
             case IToken.t_double :
 				firstExpression = simpleTypeConstructorExpression(expression, IASTExpression.Kind.POSTFIX_SIMPLETYPE_DOUBLE);
                 break;
             case IToken.t_dynamic_cast :
 				firstExpression = specialCastExpression(expression, IASTExpression.Kind.POSTFIX_DYNAMIC_CAST );
 				break;
             case IToken.t_static_cast :
 				firstExpression = specialCastExpression(expression, IASTExpression.Kind.POSTFIX_STATIC_CAST );
 				break;
             case IToken.t_reinterpret_cast :
 				firstExpression = specialCastExpression(expression, IASTExpression.Kind.POSTFIX_REINTERPRET_CAST );
 				break;
             case IToken.t_const_cast :
 				firstExpression = specialCastExpression(expression, IASTExpression.Kind.POSTFIX_CONST_CAST );
                 break;
             case IToken.t_typeid :
                 consume();
                 consume(IToken.tLPAREN);
                 boolean isTypeId = true;
                 IASTExpression lhs = null; 
                 ITokenDuple typeId = null; 
                 try
                 {
                     typeId = typeId();
                 }
                 catch (Backtrack b)
                 {
                 	isTypeId = false;
                     lhs = expression(expression);
                 }
                 consume(IToken.tRPAREN);
                 
                 firstExpression = astFactory.createExpression( ( isTypeId ? IASTExpression.Kind.POSTFIX_TYPEID_TYPEID : IASTExpression.Kind.POSTFIX_TYPEID_EXPRESSION ),
                 	  lhs, null, null, "", ( isTypeId ?  typeId.toString() : "" ), "", null );  
                 	
                 break;
             default :
                 firstExpression = primaryExpression(expression);
         }
                 
         IASTExpression secondExpression = null; 
      
      	for( ; ; )
      	{   
 	        switch (LT(1))
 	        {
 	           case IToken.tLBRACKET :
 	              // array access
 	              consume();
 	              secondExpression = expression(expression);
 	              consume(IToken.tRBRACKET);
 	              firstExpression = astFactory.createExpression( IASTExpression.Kind.POSTFIX_SUBSCRIPT, firstExpression, secondExpression, null, "", "", "", null );
 	              break;
 	           case IToken.tLPAREN :
 	              // function call
 	              consume();
 	              secondExpression = expression(expression);
 	              consume(IToken.tRPAREN);
 				  firstExpression = astFactory.createExpression( IASTExpression.Kind.POSTFIX_FUNCTIONCALL, firstExpression, secondExpression, null, "", "", "", null );
 				  break;
 	           case IToken.tINCR :
 				  consume();
 				  firstExpression = astFactory.createExpression( IASTExpression.Kind.POSTFIX_INCREMENT, firstExpression, null, null, "", "", "", null );
 				  break;
 	           case IToken.tDECR :
 	              consume();
 				  firstExpression = astFactory.createExpression( IASTExpression.Kind.POSTFIX_DECREMENT, firstExpression, null, null, "", "", "", null );
 				  break;
 	           case IToken.tDOT :
 				   // member access
 				   consume( IToken.tDOT );
 	 
 				   if( LT(1) == IToken.t_template )
 				   {
 				      consume( IToken.t_template );
 				      isTemplate = true; 
 				   }
 				   secondExpression = primaryExpression(expression);
 				   firstExpression = astFactory.createExpression( ( isTemplate ? IASTExpression.Kind.POSTFIX_DOT_TEMPL_IDEXPRESS : IASTExpression.Kind.POSTFIX_DOT_IDEXPRESSION), 
 				   		firstExpression, secondExpression, null, "", "", "", null ); 			   
 	           	   break;
 	           case IToken.tARROW :
 				   // member access
 				   consume( IToken.tARROW );
 				   if( LT(1) == IToken.t_template )
 				   {
 					  consume( IToken.t_template );
 					  isTemplate = true; 
 				   }
 				   secondExpression = primaryExpression(expression);
 				   firstExpression = astFactory.createExpression( ( isTemplate ? IASTExpression.Kind.POSTFIX_ARROW_TEMPL_IDEXP : IASTExpression.Kind.POSTFIX_ARROW_IDEXPRESSION), 
 						firstExpression, secondExpression, null, "", "", "", null );
 				   break;		   
 	           default :
 	               return firstExpression;
 	         }
    		 }
          
 
     }
 	protected IASTExpression specialCastExpression(Object expression, IASTExpression.Kind kind)
 		throws EndOfFile, Backtrack {
 		consume();
 		consume(IToken.tLT);
 		ITokenDuple duple = typeId();
 		consume(IToken.tGT);
 		consume(IToken.tLPAREN);
 		IASTExpression lhs = expression(expression);
 		consume(IToken.tRPAREN);
 		return astFactory.createExpression( kind, lhs, null, null, "", "", "", null );
 	}
 	
 	protected IASTExpression simpleTypeConstructorExpression(Object expression, Kind type)	throws EndOfFile, Backtrack {
 	   consume();
 	   consume(IToken.tLPAREN);
 	   IASTExpression inside = expression( expression );
 	   //                while (true)
 	   //                {
 	   //                    assignmentExpression(expression);
 	   //                    if (LT(1) == IToken.tRPAREN)
 	   //                        break;
 	   //                    consume(IToken.tCOMMA);
 	   //                }
 	   consume(IToken.tRPAREN);
 	   return astFactory.createExpression( type, inside, null, null, "", "", "", null );  
 	}
     /**
      * @param expression
      * @throws Backtrack
      */
     protected IASTExpression primaryExpression(Object expression) throws Backtrack
     {
 		IToken t = null; 
         switch (LT(1))
         {
             // TO DO: we need more literals...
             case IToken.tINTEGER :
             	t = consume();  
 				try
 				{
 					callback.expressionTerminal(expression, t );
 				}
 				catch (Exception e)
 				{
 				}
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_INTEGER_LITERAL, null, null, null, "", "", t.getImage(), null );
             
             case IToken.tFLOATINGPT :
 				t = consume();
 				try
 				{
 					callback.expressionTerminal(expression, t );
 				}
 				catch (Exception e)
 				{
 				}
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_FLOAT_LITERAL, null, null, null, "", "", t.getImage(), null );
             
             case IToken.tSTRING :
             case IToken.tLSTRING :
 				t = consume();
 				try
 				{
 					callback.expressionTerminal(expression, t );
 				}
 				catch (Exception e)
 				{
 				}
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_INTEGER_LITERAL, null, null, null, "", "", t.getImage(), null );
             
             case IToken.t_false :
             case IToken.t_true :
 				t = consume();
 				try
 				{
 					callback.expressionTerminal(expression, t );
 				}
 				catch (Exception e)
 				{
 				}
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_BOOLEAN_LITERAL, null, null, null, "", "", t.getImage(), null );
             
             case IToken.tCHAR :
 				t = consume();
                 try
                 {
                     callback.expressionTerminal(expression, t );
                 }
                 catch (Exception e)
                 {
                 }
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_CHAR_LITERAL, null, null, null, "", "", t.getImage(), null );
             case IToken.t_this :
                 consume( IToken.t_this );
                 return astFactory.createExpression( IASTExpression.Kind.PRIMARY_THIS, null, null, null, "", "", "", null );
             case IToken.tLPAREN :
                 consume();
                 IASTExpression lhs = expression(expression);
                 consume(IToken.tRPAREN);
                 return astFactory.createExpression( IASTExpression.Kind.PRIMARY_BRACKETED_EXPRESSION, lhs, null, null, "", "", "", null );
 
 			case IToken.tIDENTIFIER :
 			
 				ITokenDuple duple = name(); //TODO should be an ID Expression really 
 				try
 				{
 					callback.expressionName(expression);
 				}
 				catch (Exception e)
 				{
 				}
 				return astFactory.createExpression( IASTExpression.Kind.ID_EXPRESSION, null, null, null, "", "", duple.toString(), null );
 			default :
 				return astFactory.createExpression( IASTExpression.Kind.PRIMARY_EMPTY, null, null, null, "", "", "", null ); 
         }
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
     // the static instance we always use
     private static Backtrack backtrack = new Backtrack();
 
     // the static instance we always use
     public static EndOfFile endOfFile = new EndOfFile();
     // Token management
     private IScanner scanner;
     private IToken currToken, // current token we plan to consume next 
     lastToken; // last token we consumed
     /**
      * Fetches a token from the scanner. 
      * 
      * @return				the next token from the scanner
      * @throws EndOfFile	thrown when the scanner.nextToken() yields no tokens
      */
     private IToken fetchToken() throws EndOfFile
     {
         try
         {
             return scanner.nextToken();
         }
         catch (EndOfFile e)
         {
             throw e;
         }
         catch (ScannerException e)
         {
             //			e.printStackTrace();
             return fetchToken();
         }
     }
     /**
      * Look Ahead in the token list to see what is coming.  
      * 
      * @param i		How far ahead do you wish to peek?
      * @return		the token you wish to observe
      * @throws EndOfFile	if looking ahead encounters EOF, throw EndOfFile 
      */
     protected IToken LA(int i) throws EndOfFile
     {
         if (i < 1)            // can't go backwards
             return null;
         if (currToken == null)
             currToken = fetchToken();
         IToken retToken = currToken;
         for (; i > 1; --i)
         {
             retToken = retToken.getNext();
             if (retToken == null)
                 retToken = fetchToken();
         }
         return retToken;
     }
     /**
      * Look ahead in the token list and return the token type.  
      * 
      * @param i				How far ahead do you wish to peek?
      * @return				The type of that token
      * @throws EndOfFile	if looking ahead encounters EOF, throw EndOfFile
      */
     protected int LT(int i) throws EndOfFile
     {
         return LA(i).getType();
     }
     /**
      * Consume the next token available, regardless of the type.  
      * 
      * @return				The token that was consumed and removed from our buffer.  
      * @throws EndOfFile	If there is no token to consume.  
      */
     protected IToken consume() throws EndOfFile
     {
         if (currToken == null)
             currToken = fetchToken();
         if (currToken != null)
             lastToken = currToken;
         currToken = currToken.getNext();
         return lastToken;
     }
     /**
      * Consume the next token available only if the type is as specified.  
      * 
      * @param type			The type of token that you are expecting.  	
      * @return				the token that was consumed and removed from our buffer. 
      * @throws Backtrack	If LT(1) != type 
      */
     protected IToken consume(int type) throws Backtrack
     {
         if (LT(1) == type)
             return consume();
         else
             throw backtrack;
     }
     /**
      * Mark our place in the buffer so that we could return to it should we have to.  
      * 
      * @return				The current token. 
      * @throws EndOfFile	If there are no more tokens.
      */
     protected IToken mark() throws EndOfFile
     {
         if (currToken == null)
             currToken = fetchToken();
         return currToken;
     }
     /**
      * Rollback to a previous point, reseting the queue of tokens.  
      * 
      * @param mark		The point that we wish to restore to.  
      *  
      */
     protected void backup(IToken mark)
     {
         currToken = (Token)mark;
         lastToken = null; // this is not entirely right ... 
     }
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#isCppNature()
      */
     public boolean isCppNature()
     {
         return cppNature;
     }
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#setCppNature(boolean)
      */
     public void setCppNature(boolean b)
     {
         cppNature = b;
         if (scanner != null)
             scanner.setCppNature(b);
     }
 
     /* (non-Javadoc)
      * @see org.eclipse.cdt.internal.core.parser.IParser#getLastErrorOffset()
      */
     public int getLastErrorOffset()
     {
         return firstErrorOffset;
     }
     
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.parser.IParser#setRequestor(org.eclipse.cdt.core.parser.ISourceElementRequestor)
      */
     public void setRequestor(ISourceElementRequestor r)
     {
         requestor = r;
         if (scanner != null)
             scanner.setRequestor(r);
     }
 }
