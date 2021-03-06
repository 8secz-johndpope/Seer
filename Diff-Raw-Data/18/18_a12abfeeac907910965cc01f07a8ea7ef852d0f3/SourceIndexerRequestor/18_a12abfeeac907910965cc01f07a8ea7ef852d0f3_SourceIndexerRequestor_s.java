 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.cdt.internal.core.search.indexing;
 
 /**
 * @author bgheorgh
 */
 
 import java.io.Reader;
 import java.util.LinkedList;
 
 import org.eclipse.cdt.core.CCorePlugin;
 import org.eclipse.cdt.core.model.ICModelMarker;
 import org.eclipse.cdt.core.parser.DefaultProblemHandler;
 import org.eclipse.cdt.core.parser.IProblem;
 import org.eclipse.cdt.core.parser.ISourceElementRequestor;
 import org.eclipse.cdt.core.parser.ParserMode;
 import org.eclipse.cdt.core.parser.ParserUtil;
 import org.eclipse.cdt.core.parser.ast.IASTASMDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTAbstractTypeSpecifierDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTClassReference;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
 import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
 import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationReference;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerator;
 import org.eclipse.cdt.core.parser.ast.IASTEnumeratorReference;
 import org.eclipse.cdt.core.parser.ast.IASTField;
 import org.eclipse.cdt.core.parser.ast.IASTFieldReference;
 import org.eclipse.cdt.core.parser.ast.IASTFunction;
 import org.eclipse.cdt.core.parser.ast.IASTFunctionReference;
 import org.eclipse.cdt.core.parser.ast.IASTInclusion;
 import org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification;
 import org.eclipse.cdt.core.parser.ast.IASTMacro;
 import org.eclipse.cdt.core.parser.ast.IASTMethod;
 import org.eclipse.cdt.core.parser.ast.IASTMethodReference;
 import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTNamespaceReference;
 import org.eclipse.cdt.core.parser.ast.IASTParameterDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTParameterReference;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateInstantiation;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateParameterReference;
 import org.eclipse.cdt.core.parser.ast.IASTTemplateSpecialization;
 import org.eclipse.cdt.core.parser.ast.IASTTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTTypedefDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTTypedefReference;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTUsingDirective;
 import org.eclipse.cdt.core.parser.ast.IASTVariable;
 import org.eclipse.cdt.core.parser.ast.IASTVariableReference;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 
 /**
  * @author bgheorgh
  *
  * To change the template for this generated type comment go to
  * Window>Preferences>Java>Code Generation>Code and Comments
  */
 public class SourceIndexerRequestor implements ISourceElementRequestor, IIndexConstants {
 	
 	SourceIndexer indexer;
 	IFile resourceFile;
 
 	char[] packageName;
 	char[][] enclosingTypeNames = new char[5][];
 	int depth = 0;
 	int methodDepth = 0;
 	
 	private IASTInclusion currentInclude = null;
 	private LinkedList includeStack = new LinkedList();
 	
 	public SourceIndexerRequestor(SourceIndexer indexer, IFile resourceFile) {
 		super();
 		this.indexer = indexer;
 		this.resourceFile = resourceFile;
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptProblem(org.eclipse.cdt.core.parser.IProblem)
 	 */
 	public boolean acceptProblem(IProblem problem) {
 	
		  IASTInclusion include = peekInclude();
 		  IFile tempFile = resourceFile;
 		  int lineNumber = problem.getSourceLineNumber();
 		  
 		  //If we are in an include file, get the include file
 		  if (include != null){
 			
 			 IPath newPath = new Path(include.getFullFileName());
 			 IPath problemPath = new Path(new String(problem.getOriginatingFileName()));
 			 
 		
 				 tempFile = CCorePlugin.getWorkspace().getRoot().getFileForLocation(newPath);
 				 //Needed for external files
 				 if (tempFile == null)
 				 	tempFile = resourceFile;
 			 
 			 if (!newPath.equals(problemPath)){
 				 lineNumber = include.getStartingLine();
 			 }
 		  }
 		  
 	    addMarkers(tempFile,problem, lineNumber); 
	      
 		return DefaultProblemHandler.ruleOnProblem( problem, ParserMode.COMPLETE_PARSE );
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptMacro(org.eclipse.cdt.core.parser.ast.IASTMacro)
 	 */
 	public void acceptMacro(IASTMacro macro) {
 		// TODO Auto-generated method stub
 		indexer.addMacro(macro);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptVariable(org.eclipse.cdt.core.parser.ast.IASTVariable)
 	 */
 	public void acceptVariable(IASTVariable variable) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptVariable");
 		indexer.addVariable(variable);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptFunctionDeclaration(org.eclipse.cdt.core.parser.ast.IASTFunction)
 	 */
 	public void acceptFunctionDeclaration(IASTFunction function) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptFunctionDeclaration");
 		indexer.addFunctionDeclaration(function);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptUsingDirective(org.eclipse.cdt.core.parser.ast.IASTUsingDirective)
 	 */
 	public void acceptUsingDirective(IASTUsingDirective usageDirective) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptUsingDirective");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptUsingDeclaration(org.eclipse.cdt.core.parser.ast.IASTUsingDeclaration)
 	 */
 	public void acceptUsingDeclaration(IASTUsingDeclaration usageDeclaration) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptUsingDeclaration");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptASMDefinition(org.eclipse.cdt.core.parser.ast.IASTASMDefinition)
 	 */
 	public void acceptASMDefinition(IASTASMDefinition asmDefinition) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptASMDefinition");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptTypedef(org.eclipse.cdt.core.parser.ast.IASTTypedef)
 	 */
 	public void acceptTypedefDeclaration(IASTTypedefDeclaration typedef) {
 		// TODO Auto-generated method stub
 		indexer.addTypedefDeclaration(typedef);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptEnumerationSpecifier(org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier)
 	 */
 	public void acceptEnumerationSpecifier(IASTEnumerationSpecifier enumeration) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptEnumSpecifier");
 		indexer.addEnumerationSpecifier(enumeration);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterFunctionBody(org.eclipse.cdt.core.parser.ast.IASTFunction)
 	 */
 	public void enterFunctionBody(IASTFunction function) {
 		// TODO Auto-generated method stub
 		indexer.addFunctionDeclaration(function);
 		//indexer.addFunctionDefinition();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitFunctionBody(org.eclipse.cdt.core.parser.ast.IASTFunction)
 	 */
 	public void exitFunctionBody(IASTFunction function) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitFunctionBody");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterCompilationUnit(org.eclipse.cdt.core.parser.ast.IASTCompilationUnit)
 	 */
 	public void enterCompilationUnit(IASTCompilationUnit compilationUnit) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterCompilationUnit");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterInclusion(org.eclipse.cdt.core.parser.ast.IASTInclusion)
 	 */
 	public void enterInclusion(IASTInclusion inclusion) {
 		// TODO Auto-generated method stub
		IPath newPath = new Path(inclusion.getFullFileName());
 		IFile tempFile = CCorePlugin.getWorkspace().getRoot().getFileForLocation(newPath);
 		if (tempFile !=null){
 			removeMarkers(tempFile);
 		}
 		else{
 		 //File is out of workspace
 		 
		}
 		
 		IASTInclusion parent = peekInclude();
 		indexer.addInclude(inclusion, parent);
 		//Push on stack
 		pushInclude(inclusion);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterNamespaceDefinition(org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition)
 	 */
 	public void enterNamespaceDefinition(IASTNamespaceDefinition namespaceDefinition) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterNamespaceDefinition");
 		indexer.addNamespaceDefinition(namespaceDefinition);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterClassSpecifier(org.eclipse.cdt.core.parser.ast.IASTClassSpecifier)
 	 */
 	public void enterClassSpecifier(IASTClassSpecifier classSpecification) {
 		// TODO Auto-generated method stub
 		
 		//System.out.println("New class spec: " + classSpecification.getName());
 		indexer.addClassSpecifier(classSpecification);
 		//System.out.println("enterClassSpecifier");
 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterLinkageSpecification(org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification)
 	 */
 	public void enterLinkageSpecification(IASTLinkageSpecification linkageSpec) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterLinkageSpecification");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterTemplateDeclaration(org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration)
 	 */
 	public void enterTemplateDeclaration(IASTTemplateDeclaration declaration) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterTemplateDeclaration");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterTemplateSpecialization(org.eclipse.cdt.core.parser.ast.IASTTemplateSpecialization)
 	 */
 	public void enterTemplateSpecialization(IASTTemplateSpecialization specialization) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterTemplateSpecialization");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterTemplateExplicitInstantiation(org.eclipse.cdt.core.parser.ast.IASTTemplateInstantiation)
 	 */
 	public void enterTemplateInstantiation(IASTTemplateInstantiation instantiation) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterTemplateExplicitInstantiation");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptMethodDeclaration(org.eclipse.cdt.core.parser.ast.IASTMethod)
 	 */
 	public void acceptMethodDeclaration(IASTMethod method) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptMethodDeclaration");
 		indexer.addMethodDeclaration(method);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterMethodBody(org.eclipse.cdt.core.parser.ast.IASTMethod)
 	 */
 	public void enterMethodBody(IASTMethod method) {
 		// TODO Auto-generated method stub
 		//System.out.println("enterMethodBody " + method.getName());
 		indexer.addMethodDeclaration(method);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitMethodBody(org.eclipse.cdt.core.parser.ast.IASTMethod)
 	 */
 	public void exitMethodBody(IASTMethod method) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitMethodBody");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptField(org.eclipse.cdt.core.parser.ast.IASTField)
 	 */
 	public void acceptField(IASTField field) {
 		// TODO Auto-generated method stub
 	  // System.out.println("acceptField");
 	   indexer.addFieldDeclaration(field);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptClassReference(org.eclipse.cdt.core.parser.ast.IASTClassSpecifier, int)
 	 */
 	public void acceptClassReference(IASTClassReference reference) {
 		// TODO Auto-generated method stub
 		//System.out.println("acceptClassReference");
 		if (reference.getReferencedElement() instanceof IASTClassSpecifier)
 			indexer.addClassReference((IASTClassSpecifier)reference.getReferencedElement());
 		else if (reference.getReferencedElement() instanceof IASTElaboratedTypeSpecifier)
 		{
 		    indexer.addClassReference((IASTTypeSpecifier) reference.getReferencedElement());
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitTemplateDeclaration(org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration)
 	 */
 	public void exitTemplateDeclaration(IASTTemplateDeclaration declaration) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitTemplateDeclaration");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitTemplateSpecialization(org.eclipse.cdt.core.parser.ast.IASTTemplateSpecialization)
 	 */
 	public void exitTemplateSpecialization(IASTTemplateSpecialization specialization) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitTemplateSpecialization");
 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitTemplateExplicitInstantiation(org.eclipse.cdt.core.parser.ast.IASTTemplateInstantiation)
 	 */
 	public void exitTemplateExplicitInstantiation(IASTTemplateInstantiation instantiation) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitTemplateExplicitInstantiation");
 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitLinkageSpecification(org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification)
 	 */
 	public void exitLinkageSpecification(IASTLinkageSpecification linkageSpec) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitLinkageSpecification");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitClassSpecifier(org.eclipse.cdt.core.parser.ast.IASTClassSpecifier)
 	 */
 	public void exitClassSpecifier(IASTClassSpecifier classSpecification) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitClassSpecifier");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitNamespaceDefinition(org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition)
 	 */
 	public void exitNamespaceDefinition(IASTNamespaceDefinition namespaceDefinition) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitNamespaceDefinition");
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitInclusion(org.eclipse.cdt.core.parser.ast.IASTInclusion)
 	 */
 	public void exitInclusion(IASTInclusion inclusion) {
 		// TODO Auto-generated method stub
 		popInclude();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitCompilationUnit(org.eclipse.cdt.core.parser.ast.IASTCompilationUnit)
 	 */
 	public void exitCompilationUnit(IASTCompilationUnit compilationUnit) {
 		// TODO Auto-generated method stub
 		//System.out.println("exitCompilationUnit");
 
 }
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptAbstractTypeSpecDeclaration(org.eclipse.cdt.core.parser.ast.IASTAbstractTypeSpecifierDeclaration)
 	 */
 	public void acceptAbstractTypeSpecDeclaration(IASTAbstractTypeSpecifierDeclaration abstractDeclaration) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptTypedefReference(org.eclipse.cdt.core.parser.ast.IASTTypedefReference)
 	 */
 	public void acceptTypedefReference(IASTTypedefReference reference) {
 		// TODO Auto-generated method stub
 		if( reference.getReferencedElement() instanceof IASTTypedefDeclaration )
 			indexer.addTypedefReference( (IASTTypedefDeclaration) reference.getReferencedElement() );
 		
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptNamespaceReference(org.eclipse.cdt.core.parser.ast.IASTNamespaceReference)
 	 */
 	public void acceptNamespaceReference(IASTNamespaceReference reference) {
 		// TODO Auto-generated method stub
 		if (reference.getReferencedElement() instanceof IASTNamespaceDefinition)
 		indexer.addNamespaceReference((IASTNamespaceDefinition)reference.getReferencedElement());	
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptEnumerationReference(org.eclipse.cdt.core.parser.ast.IASTEnumerationReference)
 	 */
 	public void acceptEnumerationReference(IASTEnumerationReference reference) {
 		// TODO Auto-generated method stub
 		if (reference.getReferencedElement() instanceof IASTEnumerationSpecifier)
 		  indexer.addEnumerationReference((IASTEnumerationSpecifier) reference.getReferencedElement());
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptVariableReference(org.eclipse.cdt.core.parser.ast.IASTVariableReference)
 	 */
 	public void acceptVariableReference(IASTVariableReference reference) {
 		// TODO Auto-generated method stub
 		if (reference.getReferencedElement() instanceof IASTVariable)
 			indexer.addVariableReference((IASTVariable)reference.getReferencedElement());
 	
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptFunctionReference(org.eclipse.cdt.core.parser.ast.IASTFunctionReference)
 	 */
 	public void acceptFunctionReference(IASTFunctionReference reference) {
 		if (reference.getReferencedElement() instanceof IASTFunction)
 			indexer.addFunctionReference((IASTFunction) reference.getReferencedElement());
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptFieldReference(org.eclipse.cdt.core.parser.ast.IASTFieldReference)
 	 */
 	public void acceptFieldReference(IASTFieldReference reference) {
 		if (reference.getReferencedElement() instanceof IASTField)
 		  indexer.addFieldReference((IASTField) reference.getReferencedElement());
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptMethodReference(org.eclipse.cdt.core.parser.ast.IASTMethodReference)
 	 */
 	public void acceptMethodReference(IASTMethodReference reference) {
 		if (reference.getReferencedElement() instanceof IASTMethod)
 		 indexer.addMethodReference((IASTMethod) reference.getReferencedElement());
 	}
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptElaboratedForewardDeclaration(org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier)
      */
     public void acceptElaboratedForewardDeclaration(IASTElaboratedTypeSpecifier elaboratedType){
         indexer.addElaboratedForwardDeclaration(elaboratedType);       
     }
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#enterCodeBlock(org.eclipse.cdt.core.parser.ast.IASTScope)
 	 */
 	public void enterCodeBlock(IASTCodeScope scope) {
 		// TODO Auto-generated method stub
 		
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#exitCodeBlock(org.eclipse.cdt.core.parser.ast.IASTScope)
 	 */
 	public void exitCodeBlock(IASTCodeScope scope) {
 		// TODO Auto-generated method stub
 		
 	}
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptEnumeratorReference(org.eclipse.cdt.core.parser.ast.IASTEnumerationReference)
      */
     public void acceptEnumeratorReference(IASTEnumeratorReference reference)
     {
      	if( reference.getReferencedElement() instanceof IASTEnumerator )
      		indexer.addEnumeratorReference( (IASTEnumerator)reference.getReferencedElement() );
         
     }
     /* (non-Javadoc)
      * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#acceptParameterReference(org.eclipse.cdt.internal.core.parser.ast.complete.ASTParameterReference)
      */
     public void acceptParameterReference(IASTParameterReference reference)
     {
         if( reference.getReferencedElement() instanceof IASTParameterDeclaration )
         	indexer.addParameterReference( (IASTParameterDeclaration) reference.getReferencedElement() );
         
     }
     
     public void acceptTemplateParameterReference( IASTTemplateParameterReference reference ){
     	if( reference.getReferencedElement() instanceof IASTTemplateParameterReference ){
     		//TODO
     	}
     }
     
 	private void pushInclude( IASTInclusion inclusion ){
 		includeStack.addFirst( currentInclude );
 		currentInclude = inclusion;
 	}
 	
 	private IASTInclusion popInclude(){
 		IASTInclusion oldInclude = currentInclude;
 		currentInclude = (includeStack.size() > 0 ) ? (IASTInclusion) includeStack.removeFirst() : null;
 		return oldInclude;
 	}
 	
 	private IASTInclusion peekInclude(){
 		return currentInclude;
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#createReader(java.lang.String)
 	 */
 	public Reader createReader(String finalPath) {
 		return ParserUtil.createReader(finalPath);
 	}
 
 	/**
 	 * 
 	 */
 	public void removeMarkers(IFile resource) {
 		 int depth = IResource.DEPTH_INFINITE;
 	   try {
 	   	  IMarker[] markers = resource.findMarkers(ICModelMarker.INDEXER_MARKER,true,depth);
 	   	  if (markers.length > 0){
 	      resource.deleteMarkers(ICModelMarker.INDEXER_MARKER, true, depth); 
 	   	  }
 	   	  
 	   } catch (CoreException e) {
 	      // something went wrong
 	   }
 
 	}
 	
 	private void addMarkers(IFile tempFile, IProblem problem, int lineNumber){
 		 try {
 	      	IMarker[] markers = tempFile.findMarkers(ICModelMarker.INDEXER_MARKER, true,IResource.DEPTH_INFINITE);
 	      	
 	      	boolean newProblem = true;
 	      	
 	      	if (markers.length > 0){
 	      		IMarker tempMarker = null;
 	      		Integer tempInt = null;	
 	      		String tempMsgString = null;
 	      		
 	      		for (int i=0; i<markers.length; i++){
 	      			tempMarker = markers[i];
 	      			tempInt = (Integer) tempMarker.getAttribute(IMarker.LINE_NUMBER);
 	      			tempMsgString = (String) tempMarker.getAttribute(IMarker.MESSAGE);
 	      			if (tempInt.intValue()==problem.getSourceLineNumber() &&
 	      				tempMsgString.equals(problem.getMessage())){
 	      				newProblem = false;
 	      				break;
 	      			}
 	      		}
 	      	}
 	      	
 	      	if (newProblem){
 		        IMarker marker = tempFile.createMarker(ICModelMarker.INDEXER_MARKER);
 		 		
 				marker.setAttribute(IMarker.LOCATION, problem.getSourceLineNumber());
 				marker.setAttribute(IMarker.MESSAGE, /*"Resource File: " + resourceFile.getName() + " - " +*/ problem.getMessage());
 				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
 				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
 				marker.setAttribute(IMarker.CHAR_START,-1);
 				marker.setAttribute(IMarker.CHAR_END, -1);
 	      	}
 			
 	      } catch (CoreException e) {
 	         // You need to handle the cases where attribute value is rejected
 	      }
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#parserTimeout()
 	 */
 	public boolean parserTimeout() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 	
 }
