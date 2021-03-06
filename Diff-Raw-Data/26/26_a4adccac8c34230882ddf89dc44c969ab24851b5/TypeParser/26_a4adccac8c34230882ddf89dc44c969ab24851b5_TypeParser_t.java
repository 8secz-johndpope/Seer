 /*******************************************************************************
  * Copyright (c) 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corp. - Rational Software - initial implementation
  *     QNX Software Systems - adapted for type search
  *******************************************************************************/
 package org.eclipse.cdt.internal.core.browser.cache;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 
 import org.eclipse.cdt.core.CCorePlugin;
 import org.eclipse.cdt.core.browser.ITypeInfo;
 import org.eclipse.cdt.core.browser.ITypeReference;
 import org.eclipse.cdt.core.browser.ITypeSearchScope;
 import org.eclipse.cdt.core.browser.IWorkingCopyProvider;
 import org.eclipse.cdt.core.browser.PathUtil;
 import org.eclipse.cdt.core.browser.QualifiedTypeName;
 import org.eclipse.cdt.core.browser.TypeInfo;
 import org.eclipse.cdt.core.browser.TypeReference;
 import org.eclipse.cdt.core.browser.TypeSearchScope;
 import org.eclipse.cdt.core.model.CoreModel;
 import org.eclipse.cdt.core.model.ICElement;
 import org.eclipse.cdt.core.model.ICProject;
 import org.eclipse.cdt.core.model.ITranslationUnit;
 import org.eclipse.cdt.core.model.IWorkingCopy;
 import org.eclipse.cdt.core.parser.CodeReader;
 import org.eclipse.cdt.core.parser.DefaultProblemHandler;
 import org.eclipse.cdt.core.parser.IParser;
 import org.eclipse.cdt.core.parser.IProblem;
 import org.eclipse.cdt.core.parser.IScanner;
 import org.eclipse.cdt.core.parser.IScannerInfo;
 import org.eclipse.cdt.core.parser.IScannerInfoProvider;
 import org.eclipse.cdt.core.parser.ISourceElementCallbackDelegate;
 import org.eclipse.cdt.core.parser.ISourceElementRequestor;
 import org.eclipse.cdt.core.parser.ParseError;
 import org.eclipse.cdt.core.parser.ParserFactory;
 import org.eclipse.cdt.core.parser.ParserFactoryError;
 import org.eclipse.cdt.core.parser.ParserLanguage;
 import org.eclipse.cdt.core.parser.ParserMode;
 import org.eclipse.cdt.core.parser.ParserTimeOut;
 import org.eclipse.cdt.core.parser.ParserUtil;
 import org.eclipse.cdt.core.parser.ScannerInfo;
 import org.eclipse.cdt.core.parser.ast.ASTAccessVisibility;
 import org.eclipse.cdt.core.parser.ast.ASTClassKind;
 import org.eclipse.cdt.core.parser.ast.ASTNotImplementedException;
 import org.eclipse.cdt.core.parser.ast.IASTASMDefinition;
 import org.eclipse.cdt.core.parser.ast.IASTAbstractTypeSpecifierDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTBaseSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTClassReference;
 import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
 import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
 import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
 import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationReference;
 import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
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
 import org.eclipse.cdt.core.parser.ast.IASTOffsetableNamedElement;
 import org.eclipse.cdt.core.parser.ast.IASTParameterReference;
 import org.eclipse.cdt.core.parser.ast.IASTQualifiedNameElement;
 import org.eclipse.cdt.core.parser.ast.IASTReference;
 import org.eclipse.cdt.core.parser.ast.IASTScope;
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
 import org.eclipse.cdt.internal.core.browser.util.SimpleStack;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.OperationCanceledException;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.SubProgressMonitor;
 
 public class TypeParser implements ISourceElementRequestor {
 
 	private ITypeCache fTypeCache;
 	private ITypeSearchScope fScope;
 	private IProject fProject;
 	private IWorkingCopyProvider fWorkingCopyProvider;
 	private IProgressMonitor fProgressMonitor;
 	ISourceElementCallbackDelegate fLastDeclaration;
 	private final SimpleStack fScopeStack = new SimpleStack();
 	private final SimpleStack fResourceStack = new SimpleStack();
 	private ITypeInfo fTypeToFind;
 	private ITypeInfo fSuperTypeToFind;
 	private Set fProcessedTypes = new HashSet();
 	private boolean fFoundType;
 	IParser fParser = null;
 	ParserTimeOut fTimeoutThread = null;
 	
 	public TypeParser(ITypeCache typeCache, IWorkingCopyProvider provider) {
 		fTypeCache = typeCache;
 		fWorkingCopyProvider = provider;
 		
 		fTimeoutThread = new ParserTimeOut("TypeParser TimeOut Thread");  //$NON-NLS-1$
 		fTimeoutThread.setThreadPriority(Thread.MAX_PRIORITY);
 	}
 
 	public void parseTypes(TypeSearchScope scope, IProgressMonitor monitor) throws InterruptedException {
 		if (monitor == null)
 			monitor = new NullProgressMonitor();
 
 		if (monitor.isCanceled())
 			throw new InterruptedException();
 
 		fScope = new TypeSearchScope(scope);
 		Map workingCopyMap = null;
 		if (fWorkingCopyProvider != null) {
 			IWorkingCopy[] workingCopies = fWorkingCopyProvider.getWorkingCopies();
 			if (workingCopies != null && workingCopies.length > 0) {
 				workingCopyMap = new HashMap(workingCopies.length);
 				for (int i = 0; i < workingCopies.length; ++i) {
 					IWorkingCopy workingCopy = workingCopies[i];
 					IPath wcPath = workingCopy.getOriginalElement().getPath();
 					if (fScope.encloses(wcPath)) {
 //						// always flush working copies from cache?
 //						fTypeCache.flush(wcPath);
 						fScope.add(wcPath, false, null);
 						workingCopyMap.put(wcPath, workingCopy);
 					}
 				}
 			}
 		}
 		
 		fProject = fTypeCache.getProject();
 		IPath[] searchPaths = fTypeCache.getPaths(fScope);
 		Collection workingCopyPaths = new HashSet();
 		if (workingCopyMap != null) {
 			collectWorkingCopiesInProject(workingCopyMap, fProject, workingCopyPaths);
 			//TODO what about working copies outside the workspace?
 		}
 		
 		monitor.beginTask("", searchPaths.length + workingCopyPaths.size()); //$NON-NLS-1$
 		try {
 			for (Iterator pathIter = workingCopyPaths.iterator(); pathIter.hasNext(); ) {
 				IPath path = (IPath) pathIter.next();
 				parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 			}
 			for (int i = 0; i < searchPaths.length; ++i) {
 				IPath path = searchPaths[i];
 				if (!workingCopyPaths.contains(path)) {
 					parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 				} else {
 					monitor.worked(1);
 				}
 			}
 		} finally {
 			monitor.done();
 		}
 	}
 
 	public boolean findType(ITypeInfo info, IProgressMonitor monitor) throws InterruptedException {
 		if (monitor == null)
 			monitor = new NullProgressMonitor();
 
 		if (monitor.isCanceled())
 			throw new InterruptedException();
 		
 		fScope = new TypeSearchScope();
 		ITypeReference[] refs = info.getReferences();
 		if (refs == null || refs.length == 0)
 			return false;	// no source references
 		
 		fScope.add(refs[0].getPath(), false, null);
 //		for (int i = 0; i < refs.length; ++i) {
 //			ITypeReference location = refs[i];
 //			IPath path = location.getPath();
 //			fScope.add(path, false, null);
 //		}
 		
 		Map workingCopyMap = null;
 		if (fWorkingCopyProvider != null) {
 			IWorkingCopy[] workingCopies = fWorkingCopyProvider.getWorkingCopies();
 			if (workingCopies != null && workingCopies.length > 0) {
 				workingCopyMap = new HashMap(workingCopies.length);
 				for (int i = 0; i < workingCopies.length; ++i) {
 					IWorkingCopy workingCopy = workingCopies[i];
 					IPath wcPath = workingCopy.getOriginalElement().getPath();
 					if (fScope.encloses(wcPath)) {
 //						// always flush working copies from cache?
 //						fTypeCache.flush(wcPath);
 						fScope.add(wcPath, false, null);
 						workingCopyMap.put(wcPath, workingCopy);
 					}
 				}
 			}
 		}
 		
 		fProject = fTypeCache.getProject();
 		IPath[] searchPaths = fTypeCache.getPaths(fScope);
 		Collection workingCopyPaths = new HashSet();
 		if (workingCopyMap != null) {
 			collectWorkingCopiesInProject(workingCopyMap, fProject, workingCopyPaths);
 			//TODO what about working copies outside the workspace?
 		}
 		
 		monitor.beginTask("", searchPaths.length + workingCopyPaths.size()); //$NON-NLS-1$
 		try {
 			fTypeToFind = info;
 			fFoundType = false;
 			for (Iterator pathIter = workingCopyPaths.iterator(); pathIter.hasNext(); ) {
 				IPath path = (IPath) pathIter.next();
 				parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 				if (fFoundType)
 					return true;
 			}
 			for (int i = 0; i < searchPaths.length; ++i) {
 				IPath path = searchPaths[i];
 				if (!workingCopyPaths.contains(path)) {
 					parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 				} else {
 					monitor.worked(1);
 				}
 
 				if (fFoundType)
 					return true;
 			}
 		} finally {
 			fTypeToFind = null;
 			fFoundType = false;
 			monitor.done();
 		}
 		return false;
 	}
 
 	public boolean findSubTypes(ITypeInfo info, IProgressMonitor monitor) throws InterruptedException {
 		if (monitor == null)
 			monitor = new NullProgressMonitor();
 
 		if (monitor.isCanceled())
 			throw new InterruptedException();
 		
 		fScope = new TypeSearchScope();
 		ITypeReference[] refs = info.getDerivedReferences();
 		if (refs == null || refs.length == 0)
 			return false;	// no source references
 		
 		for (int i = 0; i < refs.length; ++i) {
 			ITypeReference location = refs[i];
 			IPath path = location.getPath();
 			fScope.add(path, false, null);
 		}
 
 		Map workingCopyMap = null;
 		if (fWorkingCopyProvider != null) {
 			IWorkingCopy[] workingCopies = fWorkingCopyProvider.getWorkingCopies();
 			if (workingCopies != null && workingCopies.length > 0) {
 				workingCopyMap = new HashMap(workingCopies.length);
 				for (int i = 0; i < workingCopies.length; ++i) {
 					IWorkingCopy workingCopy = workingCopies[i];
 					IPath wcPath = workingCopy.getOriginalElement().getPath();
 					if (fScope.encloses(wcPath)) {
 //						// always flush working copies from cache?
 //						fTypeCache.flush(wcPath);
 						fScope.add(wcPath, false, null);
 						workingCopyMap.put(wcPath, workingCopy);
 					}
 				}
 			}
 		}
 		
 		fProject = fTypeCache.getProject();
 		IPath[] searchPaths = fTypeCache.getPaths(fScope);
 		Collection workingCopyPaths = new HashSet();
 		if (workingCopyMap != null) {
 			collectWorkingCopiesInProject(workingCopyMap, fProject, workingCopyPaths);
 			//TODO what about working copies outside the workspace?
 		}
 		
 		monitor.beginTask("", searchPaths.length + workingCopyPaths.size()); //$NON-NLS-1$
 		try {
 			fTypeToFind = null;
 			fSuperTypeToFind = info;
 			fFoundType = false;
 			for (Iterator pathIter = workingCopyPaths.iterator(); pathIter.hasNext(); ) {
 				IPath path = (IPath) pathIter.next();
 				parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 			}
 			for (int i = 0; i < searchPaths.length; ++i) {
 				IPath path = searchPaths[i];
 				if (!workingCopyPaths.contains(path)) {
 					parseSource(path, fProject, workingCopyMap, new SubProgressMonitor(monitor, 1));
 				} else {
 					monitor.worked(1);
 				}
 			}
 		} finally {
 			fTypeToFind = null;
 			fFoundType = false;
 			monitor.done();
 		}
 		return false;
 	}
 
 	private void collectWorkingCopiesInProject(Map workingCopyMap, IProject project, Collection workingCopyPaths) {
 		for (Iterator mapIter = workingCopyMap.entrySet().iterator(); mapIter.hasNext(); ) {
 			Map.Entry entry = (Map.Entry) mapIter.next();
 			IPath path = (IPath) entry.getKey();
 			IWorkingCopy copy = (IWorkingCopy) entry.getValue();
 			
 			ICProject cProject = copy.getCProject();
 			if (cProject != null && cProject.getProject().equals(project)) {
 				workingCopyPaths.add(path);
 			}
 		}
 	}
 	
 	private void parseSource(IPath path, IProject project, Map workingCopyMap, IProgressMonitor progressMonitor) throws InterruptedException {
 		if (progressMonitor.isCanceled())
 			throw new InterruptedException();
 	
 		// count how many types were indexed for this path
 		TypeSearchScope pathScope = new TypeSearchScope();
 		pathScope.add(path, false, project);
 		int typeCount = fTypeCache.getTypes(pathScope).length;
 		
 		progressMonitor.beginTask("", typeCount); //$NON-NLS-1$
 		try {
 			IWorkingCopy workingCopy = null;
 			if (workingCopyMap != null) {
 				workingCopy = (IWorkingCopy) workingCopyMap.get(path);
 			}
 
 			ParserLanguage language = getLanguage(project, workingCopy);
 			if (language == null) {
 				return;	// not C or C++
 			}
 
 			CodeReader reader = null;
 			Object stackObject = null;
 			
 			IResource resource = null;
 			if (workingCopy != null) {
 				reader = createWorkingCopyReader(workingCopy);
 				resource = workingCopy.getResource();
 				if (resource != null) {
 					path = resource.getLocation();
 				}
 				stackObject = workingCopy;
 			} else {
 				IWorkspace workspace = CCorePlugin.getWorkspace();
 				if (workspace != null) {
 					IWorkspaceRoot wsRoot = workspace.getRoot();
 					if (wsRoot != null) {
 						resource = wsRoot.findMember(path, true);
 					}
 				}
 				if (resource != null) {
 					reader = createResourceReader(resource);
 					path = resource.getLocation();
 					stackObject = resource;
 				} else {
 					reader = createFileReader(path);
 					stackObject = path;
 				}
 			}
 
 			if (reader != null) {
 				fResourceStack.clear();
 				fScopeStack.clear();
 				fResourceStack.push(stackObject);
 				parseContents(path, resource, project, reader, language, progressMonitor);
 				fResourceStack.pop();
 			}
 		} finally {
 			progressMonitor.done();
 		}
 	}
 	
 	private ParserLanguage getLanguage(IProject project, IWorkingCopy workingCopy) {
 		ParserLanguage projectLanguage = null;
 		if (project != null) {
 			if (CoreModel.hasCCNature(project)) {
 				projectLanguage = ParserLanguage.CPP;
 			} else if (CoreModel.hasCNature(project)) {
 				projectLanguage = ParserLanguage.C;
 			}
 		}
 
 		if (workingCopy != null) {
 			ParserLanguage workingCopyLanguage = null;
 			ITranslationUnit unit = workingCopy.getTranslationUnit();
 			if (unit != null) {
 				if (unit.isCLanguage()) {
 					workingCopyLanguage = ParserLanguage.C;
 				} else if (unit.isCXXLanguage()) {
 					workingCopyLanguage = ParserLanguage.CPP;
 				}
 			}
 			if (workingCopyLanguage != null) {
 				if (projectLanguage == null) {
 					return workingCopyLanguage;
 				} else if (projectLanguage.equals(ParserLanguage.CPP)) {
 					// if project is CPP then working copy must be CPP
 					return projectLanguage;
 				} else {
 					return workingCopyLanguage;
 				}
 			}
 		}
 		return projectLanguage;
 	}
 
 	private CodeReader createWorkingCopyReader(IWorkingCopy workingCopy) {
 		CodeReader reader = null;
 		IResource resource = workingCopy.getResource();
 		if (resource != null && resource.isAccessible()) {
 			char[] contents = workingCopy.getContents();
 			if (contents != null)
 				reader = new CodeReader(resource.getLocation().toOSString(), contents);
 		}
 		return reader;
 	}
 
 	private CodeReader createResourceReader(IResource resource) {
 		CodeReader reader = null;
 		if (resource.isAccessible() && resource instanceof IFile) {
 			IFile file = (IFile) resource;
 			InputStream contents = null;
 			try {
 				contents = file.getContents();
 				if (contents != null)
 					reader = new CodeReader(resource.getLocation().toOSString(), file.getCharset(), contents);
 			} catch (CoreException ex) {
 				ex.printStackTrace();
 			} catch (IOException e) {
 			} finally {
 				if (contents != null) {
 					try {
 						contents.close();
 					} catch (IOException io) {
 						// ignore
 					}
 				}
 			}
 		}
 		return reader;
 	}
 
 	private CodeReader createFileReader(IPath path) {
 		CodeReader reader = null;
 		try {
 			reader = new CodeReader(path.toOSString());
 		} catch (IOException ex) {
 			ex.printStackTrace();
 		}
 		return reader;
 	}
 
 	private void parseContents(IPath realPath, IResource resource, IProject project, CodeReader reader, ParserLanguage language, IProgressMonitor progressMonitor) throws InterruptedException {
 		IScannerInfo scanInfo = null;
 
 		if (project != null) {
 			//TODO temporary workaround to catch managed build exceptions
 			try {
 				IScannerInfoProvider provider = CCorePlugin.getDefault().getScannerInfoProvider(project);
 				if (provider != null) {
 					IScannerInfo buildScanInfo = provider.getScannerInformation(resource != null ? resource : project);
 					if (buildScanInfo != null)
 						scanInfo = new ScannerInfo(buildScanInfo.getDefinedSymbols(), buildScanInfo.getIncludePaths());
 				}
 			} catch (Exception ex) {
 				ex.printStackTrace();
 			}
 		}
 		if (scanInfo == null)
 			scanInfo = new ScannerInfo();
 
 		try {
 			fProgressMonitor = progressMonitor;
 			IScanner scanner = ParserFactory.createScanner(reader, scanInfo,
 					ParserMode.STRUCTURAL_PARSE, language, this, ParserUtil.getScannerLogService(), null);
 			fParser = ParserFactory.createParser(scanner, this, ParserMode.STRUCTURAL_PARSE, language, ParserUtil.getParserLogService());
 			
 			// start timer
 			int timeout = getParserTimeout();
 			if (timeout > 0) {
 				fTimeoutThread.setTimeout(timeout);
 				fTimeoutThread.setParser(fParser);
 				while (!fTimeoutThread.isReadyToRun()){
 					try {
 						Thread.sleep(20);
 					} catch (InterruptedException e) {
 						// TODO Auto-generated catch block
 						e.printStackTrace();
 					}
 				}
 				fTimeoutThread.startTimer();
 			}
 			
 			fParser.parse();
 		} catch (ParserFactoryError e) {
 			CCorePlugin.log(e);
 		} catch (ParseError e) {
 			// no need to log
 			// CCorePlugin.log(e);
 		} catch (OperationCanceledException e) {
 			throw new InterruptedException();
 		} catch (Exception e) {
 			CCorePlugin.log(e);
 		} finally {
 			// stop timer
 			fTimeoutThread.stopTimer();
 			fTimeoutThread.setParser(null);
 			fProgressMonitor = null;
 			fParser = null;
 		}
 	}
 
 	public boolean acceptProblem(IProblem problem) {
 		return DefaultProblemHandler.ruleOnProblem(problem, ParserMode.COMPLETE_PARSE);
 	}
 	public void acceptUsingDirective(IASTUsingDirective usageDirective) {}
 	public void acceptUsingDeclaration(IASTUsingDeclaration usageDeclaration) {}
 	public void acceptASMDefinition(IASTASMDefinition asmDefinition) {}
 	public void acceptAbstractTypeSpecDeclaration(IASTAbstractTypeSpecifierDeclaration abstractDeclaration) {}
 	public void enterTemplateDeclaration(IASTTemplateDeclaration declaration) {}
 	public void enterTemplateSpecialization(IASTTemplateSpecialization specialization) {}
 	public void enterTemplateInstantiation(IASTTemplateInstantiation instantiation) {}
 	public void exitTemplateDeclaration(IASTTemplateDeclaration declaration) {}
 	public void exitTemplateSpecialization(IASTTemplateSpecialization specialization) {}
 	public void exitTemplateExplicitInstantiation(IASTTemplateInstantiation instantiation) {}
 	public void acceptParameterReference(IASTParameterReference reference) {}
 	public void acceptTemplateParameterReference(IASTTemplateParameterReference reference) {}
 	public void acceptTypedefReference(IASTTypedefReference reference) {}
 	public void acceptEnumeratorReference(IASTEnumeratorReference reference) {}
 	public void acceptClassReference(IASTClassReference reference) {}
 	public void acceptNamespaceReference(IASTNamespaceReference reference) {}
 	public void acceptVariableReference(IASTVariableReference reference) {}
 	public void acceptFieldReference(IASTFieldReference reference) {}
 	public void acceptEnumerationReference(IASTEnumerationReference reference) {}
 	public void acceptFunctionReference(IASTFunctionReference reference) {}
 	public void acceptMethodReference(IASTMethodReference reference) {}
 	public void acceptField(IASTField field) {}
 	public void acceptMacro(IASTMacro macro) {}
 	public void acceptVariable(IASTVariable variable) {}
 	public void acceptFunctionDeclaration(IASTFunction function) {}
 	public void acceptMethodDeclaration(IASTMethod method) {}
 	public void enterCodeBlock(IASTCodeScope scope) {}
 	public void exitCodeBlock(IASTCodeScope scope) {}
 	public void acceptFriendDeclaration(IASTDeclaration declaration) {}
 
 	public void enterLinkageSpecification(IASTLinkageSpecification linkageSpec) {
 		pushScope(linkageSpec);
 	}
 
 	public void exitLinkageSpecification(IASTLinkageSpecification linkageSpec) {
 		popScope();
 	}
 
 	public void enterCompilationUnit(IASTCompilationUnit compilationUnit) {
 		pushScope(compilationUnit);
 	}
 
 	public void exitCompilationUnit(IASTCompilationUnit compilationUnit) {
 		popScope();
 	}
 
 	public void enterFunctionBody(IASTFunction function) {
 		pushScope(function);
 	}
 
 	public void exitFunctionBody(IASTFunction function) {
 		popScope();
 	}
 
 	public void enterMethodBody(IASTMethod method) {
 		pushScope(method);
 	}
 
 	public void exitMethodBody(IASTMethod method) {
 		popScope();
 	}
 
 	public void enterNamespaceDefinition(IASTNamespaceDefinition namespaceDefinition) {
 		fLastDeclaration = namespaceDefinition;
 		acceptType(namespaceDefinition);
 		pushScope(namespaceDefinition);
 	}
 
 	public void exitNamespaceDefinition(IASTNamespaceDefinition namespaceDefinition) {
 		popScope();
 	}
 
 	public void enterClassSpecifier(IASTClassSpecifier classSpecification) {
 		fLastDeclaration = classSpecification;
 		acceptType(classSpecification);
 		pushScope(classSpecification);
 	}
 
 	public void exitClassSpecifier(IASTClassSpecifier classSpecification) {
 		popScope();
 	}
 
 	private void pushScope(IASTScope scope) {
 		if (fProgressMonitor.isCanceled())
 			throw new OperationCanceledException();
 		fScopeStack.push(scope);
 	}
 
 	private IASTScope popScope() {
 		if (fProgressMonitor.isCanceled())
 			throw new OperationCanceledException();
 		return (IASTScope) fScopeStack.pop();
 	}
 
 	public void acceptTypedefDeclaration(IASTTypedefDeclaration typedef) {
 		fLastDeclaration = typedef;
 		acceptType(typedef);
 	}
 
 	public void acceptEnumerationSpecifier(IASTEnumerationSpecifier enumeration) {
 		fLastDeclaration = enumeration;
 		acceptType(enumeration);
 	}
 
 	public void acceptElaboratedForewardDeclaration(IASTElaboratedTypeSpecifier elaboratedType) {
 	//	acceptType(elaboratedType);
 	}
 
 	public void enterInclusion(IASTInclusion inclusion) {
 		if (fProgressMonitor.isCanceled())
 			throw new OperationCanceledException();
 
 		String includePath = inclusion.getFullFileName();
 		IPath path = new Path(includePath);
 		path = PathUtil.getWorkspaceRelativePath(path);
 
 		IResource resource = null;
 		IWorkspace workspace = CCorePlugin.getWorkspace();
 		if (workspace != null) {
 			IWorkspaceRoot wsRoot = workspace.getRoot();
 			if (wsRoot != null) {
 				resource = wsRoot.findMember(path, true);
 			}
 		}
 		//TODO do inclusions get parsed as working copies?
 		
 		Object stackObject = path;
 		if (resource != null)
 			stackObject = resource;
 		
 		fResourceStack.push(stackObject);
 	}
 
 	public void exitInclusion(IASTInclusion inclusion) {
 		if (fProgressMonitor.isCanceled())
 			throw new OperationCanceledException();
 		fResourceStack.pop();
 	}
 	
 	private class NodeTypeInfo {
 		int type;
 		String name;
 		String[] enclosingNames;
 		int offset;
 		int end;
 		IASTOffsetableNamedElement offsetable;
 		
 		void init() {
 			type = 0;
 			name = null;
 			enclosingNames = null;
 			offset = 0;
 			end = 0;
 			offsetable = null;
 		}
 		
 		boolean parseNodeForTypeInfo(ISourceElementCallbackDelegate node) {
 			init();
 			
 			if (node instanceof IASTReference) {
 				IASTReference reference = (IASTReference) node;
 				offset = reference.getOffset();
 				end = offset + reference.getName().length();
 			} else if (node instanceof IASTOffsetableNamedElement) {
 				offsetable = (IASTOffsetableNamedElement) node;
 				offset = offsetable.getNameOffset() != 0 ? offsetable.getNameOffset() : offsetable.getStartingOffset();
 				end = offsetable.getNameEndOffset();
 				if (end == 0) {
 					end = offset + offsetable.getName().length();
 				}
 			}
 			
 			if (node instanceof IASTReference)
 				node = fLastDeclaration;
 			if (node instanceof IASTReference) {
 				offsetable = (IASTOffsetableNamedElement) ((IASTReference) node).getReferencedElement();
 				name = ((IASTReference) node).getName();
 			} else if (node instanceof IASTOffsetableNamedElement) {
 				offsetable = (IASTOffsetableNamedElement) node;
 				name = offsetable.getName();
 			} else {
 				return false;
 			}
 			
 			// skip unnamed structs
 			if (name == null || name.length() == 0)
 				return false;
 
 			// skip unused types
 			type = getElementType(offsetable);
 			if (type == 0) {
 				return false;
 			}
 			
 			// collect enclosing names
 			if (offsetable instanceof IASTQualifiedNameElement) {
 				String[] names = ((IASTQualifiedNameElement) offsetable).getFullyQualifiedName();
 				if (names != null && names.length > 1) {
 					enclosingNames = new String[names.length - 1];
 					System.arraycopy(names, 0, enclosingNames, 0, names.length - 1);
 				}
 			}
 			
 			return true;
 		}
 
 	}
 
 	private void acceptType(ISourceElementCallbackDelegate node) {
 		if (fProgressMonitor.isCanceled())
 			throw new OperationCanceledException();
 
 		// skip local declarations
 		IASTScope currentScope = (IASTScope) fScopeStack.top();
 		if (currentScope instanceof IASTFunction || currentScope instanceof IASTMethod) {
 			return;
 		}
 		
 		NodeTypeInfo nodeInfo = new NodeTypeInfo();
 		if (nodeInfo.parseNodeForTypeInfo(node)) {
 			TypeReference originalLocation = null;
 			Object originalRef = fResourceStack.bottom();
 			if (originalRef instanceof IWorkingCopy) {
 				IWorkingCopy workingCopy = (IWorkingCopy) originalRef;
 				originalLocation = new TypeReference(workingCopy, fProject);
 			} else if (originalRef instanceof IResource) {
 				IResource resource = (IResource) originalRef;
 				originalLocation = new TypeReference(resource, fProject);
 			} else if (originalRef instanceof IPath) {
 				IPath path = PathUtil.getProjectRelativePath((IPath)originalRef, fProject);
 				originalLocation = new TypeReference(path, fProject);
 			}
 			
 			TypeReference resolvedLocation = null;
 			Object resolvedRef = fResourceStack.top();
 			if (resolvedRef instanceof IWorkingCopy) {
 				IWorkingCopy workingCopy = (IWorkingCopy) resolvedRef;
 				resolvedLocation = new TypeReference(workingCopy, fProject, nodeInfo.offset, nodeInfo.end - nodeInfo.offset);
 			} else if (resolvedRef instanceof IResource) {
 				IResource resource = (IResource) resolvedRef;
 				resolvedLocation = new TypeReference(resource, fProject, nodeInfo.offset, nodeInfo.end - nodeInfo.offset);
 			} else if (resolvedRef instanceof IPath) {
 				IPath path = PathUtil.getProjectRelativePath((IPath)resolvedRef, fProject);
 				resolvedLocation = new TypeReference(path, fProject, nodeInfo.offset, nodeInfo.end - nodeInfo.offset);
 			}
 			
 			if (fTypeToFind != null) {
 				if ((fTypeToFind.getCElementType() == nodeInfo.type || fTypeToFind.isUndefinedType()) && nodeInfo.name.equals(fTypeToFind.getName())) {
 					QualifiedTypeName qualifiedName = new QualifiedTypeName(nodeInfo.name, nodeInfo.enclosingNames);
 					if (qualifiedName.equals(fTypeToFind.getQualifiedTypeName())) {
 						// add types to cache
 						ITypeInfo newType = addType(nodeInfo.type, qualifiedName, originalLocation, resolvedLocation);
 						if (newType != null && node instanceof IASTClassSpecifier) {
 							addSuperClasses(newType, (IASTClassSpecifier)node, originalLocation, fProcessedTypes);
 						}
 						fProgressMonitor.worked(1);
 
 						fFoundType = true;
 						// terminate the parser
 						fParser.cancel();
 					}
 				}
 			} else {
 				// add types to cache
 				QualifiedTypeName qualifiedName = new QualifiedTypeName(nodeInfo.name, nodeInfo.enclosingNames);
 				ITypeInfo newType = addType(nodeInfo.type, qualifiedName, originalLocation, resolvedLocation);
 				if (newType != null && node instanceof IASTClassSpecifier) {
 					addSuperClasses(newType, (IASTClassSpecifier)node, originalLocation, fProcessedTypes);
 				}
 				fProgressMonitor.worked(1);
 			}
 		}
 	}
 
 	int getElementType(IASTOffsetableNamedElement offsetable) {
 		if (offsetable instanceof IASTClassSpecifier || offsetable instanceof IASTElaboratedTypeSpecifier) {
 			ASTClassKind kind = null;
 			if (offsetable instanceof IASTClassSpecifier) {
 				kind = ((IASTClassSpecifier) offsetable).getClassKind();
 			} else {
 				kind = ((IASTElaboratedTypeSpecifier) offsetable).getClassKind();
 			}
 			if (kind == ASTClassKind.CLASS) {
 				return ICElement.C_CLASS;
 			} else if (kind == ASTClassKind.STRUCT) {
 				return ICElement.C_STRUCT;
 			} else if (kind == ASTClassKind.UNION) {
 				return ICElement.C_UNION;
 			}
 		} else if (offsetable instanceof IASTNamespaceDefinition) {
 			return ICElement.C_NAMESPACE;
 		} else if (offsetable instanceof IASTEnumerationSpecifier) {
 			return ICElement.C_ENUMERATION;
 		} else if (offsetable instanceof IASTTypedefDeclaration) {
 			return ICElement.C_TYPEDEF;
 		}
 		return 0;
 	}
 	
 	private void addSuperClasses(ITypeInfo type, IASTClassSpecifier classSpec, TypeReference location, Set processedClasses) {
 		Iterator baseIter = classSpec.getBaseClauses();
 		if (baseIter != null) {
 			while (baseIter.hasNext()) {
 				IASTBaseSpecifier baseSpec = (IASTBaseSpecifier) baseIter.next();
 				try {
 					ASTAccessVisibility baseAccess = baseSpec.getAccess();
 					
 					IASTClassSpecifier parentClass = null;
 					IASTTypeSpecifier parentSpec = baseSpec.getParentClassSpecifier();
 					if (parentSpec instanceof IASTClassSpecifier)
 						parentClass = (IASTClassSpecifier) parentSpec;
 					
 					if (parentClass != null) {
 						NodeTypeInfo nodeInfo = new NodeTypeInfo();
 						if (nodeInfo.parseNodeForTypeInfo(parentClass)) {
 							// add type to cache
 							ITypeInfo superType = addSuperType(type, nodeInfo.type, nodeInfo.name, nodeInfo.enclosingNames, location, baseAccess, baseSpec.isVirtual());
 
 							// recursively process super super classes
 							if (!processedClasses.contains(parentClass)) {
 								processedClasses.add(parentClass);
 								addSuperClasses(superType, parentClass, location, processedClasses);
 							}
 						}
 					}
 				} catch (ASTNotImplementedException e) {
 				}
 			}
 		}
 	}
 
 	private ITypeInfo addType(int type, QualifiedTypeName qualifiedName, TypeReference originalLocation, TypeReference resolvedLocation) {
 		ITypeInfo info = fTypeCache.getType(type, qualifiedName);
 		if (info == null || info.isUndefinedType()) {
 			// add new type to cache
 			if (info != null) {
 				info.setCElementType(type);
 			} else {
 				info = new TypeInfo(type, qualifiedName);
 				fTypeCache.insert(info);
 			}
 			
 			info.addReference(originalLocation);
 		}
 		
 		info.addReference(resolvedLocation);
 		
 		return info;
 	}
 
 	private ITypeInfo addSuperType(ITypeInfo addToType, int type, String name, String[] enclosingNames, TypeReference location, ASTAccessVisibility access, boolean isVirtual) {
 		QualifiedTypeName qualifiedName = new QualifiedTypeName(name, enclosingNames);
 		ITypeInfo superType = fTypeCache.getType(type, qualifiedName);
 		if (superType == null || superType.isUndefinedType()) {
 			if (superType != null) {
 				// merge with existing type
 				superType.setCElementType(type);
 			} else {
 				// add new type to cache
 				superType = new TypeInfo(type, qualifiedName);
 				fTypeCache.insert(superType);
 			}
 		}
 		superType.addDerivedReference(location);
 		
 		if (fSuperTypeToFind != null && fSuperTypeToFind.equals(superType)) {
 			//TODO don't need to do anything here?
 		}
 		fTypeCache.addSupertype(addToType, superType, access, isVirtual);
 		fTypeCache.addSubtype(superType, addToType);
 		return superType;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.cdt.core.parser.ISourceElementRequestor#createReader(java.lang.String)
 	 */
 	public CodeReader createReader(String finalPath, Iterator workingCopies) {
 		return ParserUtil.createReader(finalPath, workingCopies);
 	}
    
    private static final int DEFAULT_PARSER_TIMEOUT = 30;
 	
 	public int getParserTimeout() {
        //TODO we shouldn't be trying to access the indexer prefs
        //TODO clean this up
        int timeout = 0;
        try {
            // here we just reuse the indexer timeout
            String str = CCorePlugin.getDefault().getPluginPreferences().getString("CDT_INDEXER_TIMEOUT"); //$NON-NLS-1$
            if (str != null && str.length() > 0) {
                int val = Integer.valueOf(str).intValue();
                if (val > 0) {
                    timeout = val;
                }
            }
        } catch (NumberFormatException e) {
            // do nothing
        }

        if (timeout == 0) {
            timeout = DEFAULT_PARSER_TIMEOUT;
        }
        return timeout;
 	}
 	
 }
