 package org.eclipse.jdt.internal.debug.ui.snippeteditor;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import java.io.ByteArrayOutputStream;
 import java.io.PrintStream;
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.IncrementalProjectBuilder;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.QualifiedName;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.IDebugEventListener;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.debug.core.ILauncher;
 import org.eclipse.debug.core.model.IBreakpoint;
 import org.eclipse.debug.core.model.IDebugElement;
 import org.eclipse.debug.core.model.IDebugTarget;
 import org.eclipse.debug.core.model.IStackFrame;
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jdt.core.eval.IEvaluationContext;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaObject;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.debug.core.IJavaType;
 import org.eclipse.jdt.debug.core.IJavaValue;
 import org.eclipse.jdt.debug.eval.EvaluationManager;
 import org.eclipse.jdt.debug.eval.IClassFileEvaluationEngine;
 import org.eclipse.jdt.debug.eval.IEvaluationListener;
 import org.eclipse.jdt.debug.eval.IEvaluationResult;
 import org.eclipse.jdt.internal.debug.ui.JDIContentAssistPreference;
 import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
 import org.eclipse.jdt.internal.debug.ui.JDISourceViewer;
 import org.eclipse.jdt.internal.debug.ui.JavaDebugImages;
 import org.eclipse.jdt.internal.debug.ui.display.JavaInspectExpression;
 import org.eclipse.jdt.internal.ui.JavaPlugin;
 import org.eclipse.jdt.internal.ui.JavaStatusConstants;
 import org.eclipse.jdt.internal.ui.text.java.ResultCollector;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.eclipse.jdt.ui.IContextMenuConstants;
 import org.eclipse.jdt.ui.text.JavaTextTools;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.ITextSelection;
 import org.eclipse.jface.text.contentassist.ContentAssistant;
 import org.eclipse.jface.text.contentassist.IContentAssistant;
 import org.eclipse.jface.text.source.ISourceViewer;
 import org.eclipse.jface.text.source.IVerticalRuler;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.swt.custom.BusyIndicator;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.IFileEditorInput;
 import org.eclipse.ui.IViewPart;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.actions.WorkspaceModifyOperation;
 import org.eclipse.ui.dialogs.SaveAsDialog;
 import org.eclipse.ui.part.EditorActionBarContributor;
 import org.eclipse.ui.part.FileEditorInput;
 import org.eclipse.ui.texteditor.AbstractTextEditor;
 import org.eclipse.ui.texteditor.IDocumentProvider;
 import org.eclipse.ui.texteditor.ITextEditorActionConstants;
 import org.eclipse.ui.texteditor.TextOperationAction;
 
 import com.sun.jdi.InvocationException;
 import com.sun.jdi.ObjectReference;
 
 /**
  * An editor for Java snippets.
  */
 public class JavaSnippetEditor extends AbstractTextEditor implements IDebugEventListener, IEvaluationListener {			
 	public static final String PACKAGE_CONTEXT = "SnippetEditor.package"; //$NON-NLS-1$
 	
 	private final static String TAG= "input_element"; //$NON-NLS-1$
 	
 	public final static int RESULT_DISPLAY= 1;
 	public final static int RESULT_RUN= 2;
 	public final static int RESULT_INSPECT= 3;
 	
 	private IJavaProject fJavaProject;
 	private IEvaluationContext fEvaluationContext;
 	private IDebugTarget fVM;
 	private String[] fLaunchedClassPath;
 	private List fSnippetStateListeners;	
 	private int fResultMode; // one of the RESULT_* constants from above
 	private boolean fEvaluating;
 	private IJavaThread fThread;
 	private int fAttempts= 0;
 	
 	private int fSnippetStart;
 	private int fSnippetEnd;
 	
 	private String fPackageHandle= null;
 	
 	private Image fOldTitleImage= null;
 	private IClassFileEvaluationEngine fEngine= null;
 	
 	public JavaSnippetEditor() {
 		super();
 		setDocumentProvider(JDIDebugUIPlugin.getDefault().getSnippetDocumentProvider());
 		setSourceViewerConfiguration(new JavaSnippetViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools(), this));		
 		fSnippetStateListeners= new ArrayList(4);
 		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
 		setEditorContextMenuId("#JavaSnippetEditorContext"); //$NON-NLS-1$
 		setRulerContextMenuId("#JavaSnippetRulerContext"); //$NON-NLS-1$
 	}
 	
 	protected void doSetInput(IEditorInput input) throws CoreException {
 		super.doSetInput(input);
 		fPackageHandle = getPage().getPersistentProperty(new QualifiedName(JDIDebugUIPlugin.getPluginId(), PACKAGE_CONTEXT));
 	}
 		
 	public void dispose() {
 		shutDownVM();
 		fSnippetStateListeners= Collections.EMPTY_LIST;
 		super.dispose();
 	}
 	
 	/**
 	 * Actions for the editor popup menu
 	 * @see AbstractTextEditor#createActions
 	 */
 	protected void createActions() {
 		super.createActions();
 		setAction("Run", new RunAction(this)); //$NON-NLS-1$
 		setAction("ContentAssistProposal", new TextOperationAction(SnippetMessages.getBundle(), "SnippetEditor.ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS));			 //$NON-NLS-2$ //$NON-NLS-1$
 		setAction("OpenOnSelection", new SnippetOpenOnSelectionAction(this));			 //$NON-NLS-1$
 		setAction("OpenHierarchyOnSelection", new SnippetOpenHierarchyOnSelectionAction(this));  //$NON-NLS-1$
 		setAction("Stop", new StopAction(this));  //$NON-NLS-1$
 		setAction("RunIn", new RunInPackageAction(this));  //$NON-NLS-1$
 	} 
 	
 	/**
 	 * @see AbstractTextEditor#editorContextMenuAboutToShow(MenuManager)
 	 */
 	protected void editorContextMenuAboutToShow(IMenuManager menu) {
 		super.editorContextMenuAboutToShow(menu);
 		addGroup(menu, ITextEditorActionConstants.GROUP_EDIT, IContextMenuConstants.GROUP_GENERATE);		
 		addGroup(menu, ITextEditorActionConstants.GROUP_FIND, IContextMenuConstants.GROUP_SEARCH);		
 		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "ContentAssistProposal"); //$NON-NLS-1$
 		addGroup(menu, IContextMenuConstants.GROUP_SEARCH,  IContextMenuConstants.GROUP_SHOW);
 		addAction(menu, IContextMenuConstants.GROUP_SHOW, "OpenOnSelection"); //$NON-NLS-1$
 		addAction(menu, IContextMenuConstants.GROUP_SHOW, "OpenHierarchyOnSelection"); //$NON-NLS-1$
 		addAction(menu, IContextMenuConstants.GROUP_ADDITIONS, "Run"); //$NON-NLS-1$
 		addAction(menu, IContextMenuConstants.GROUP_ADDITIONS, "Stop"); //$NON-NLS-1$
 		addAction(menu, IContextMenuConstants.GROUP_ADDITIONS, "RunIn"); //$NON-NLS-1$
 	}
 
 	public boolean isVMLaunched() {
 		return fVM != null;
 	}
 	
 	public boolean isEvaluating() {
 		return fEvaluating;
 	}
 	
 	public void evalSelection(final int resultMode) {
 		if (!isInJavaProject()) {
 			reportNotInJavaProjectError();
 			return;
 		}
 		if (isEvaluating()) {
 			return;
 		}
 		evaluationStarts();
 
 		fResultMode= resultMode;
 		buildAndLaunch();
 		
 		if (fVM == null) {
 			evaluationEnds();
 			return;
 		}
 		fireEvalStateChanged();
 
 		ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
 		String snippet= selection.getText();
 		fSnippetStart= selection.getOffset();
 		fSnippetEnd= fSnippetStart + selection.getLength();
 		
 		evaluate(snippet);			
 	}	
 	
 	protected void buildAndLaunch() {
 		IJavaProject javaProject= getJavaProject();
 		if (javaProject == null) {
 			return;
 		}
 		boolean build = !javaProject.getProject().getWorkspace().isAutoBuilding()
 			|| !javaProject.hasBuildState();
 		
 		if (build) {
 			IRunnableWithProgress r= new IRunnableWithProgress() {
 				public void run(IProgressMonitor pm) throws InvocationTargetException {
 					try {
 						getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
 					} catch (CoreException e) {
 						throw new InvocationTargetException(e);
 					}
 				}
 			};
 			try {
 				new ProgressMonitorDialog(getShell()).run(true, false, r);		
 			} catch (InterruptedException e) {
 				evaluationEnds();
 				return;
 			} catch (InvocationTargetException e) {
 				evaluationEnds();
 				return;
 			}
 		}
 
 		boolean cpChange= classPathHasChanged();
 		boolean launch= fVM == null || cpChange;
 				
 		if (cpChange) {
 			shutDownVM();
 		}
 	
 		if (launch) {
 			launchVM();
 			fVM= ScrapbookLauncherDelegate.getDefault().getDebugTarget(getPage());
 		}
 	}
 	
 	public void setPackage(String packageHandle) {
 		fPackageHandle= packageHandle;
 		// persist
 		try {
 			getPage().setPersistentProperty(new QualifiedName(JDIDebugUIPlugin.getPluginId(), PACKAGE_CONTEXT), packageHandle);
 		} catch (CoreException e) {
 			ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.packagecontext"), null, e.getStatus()); //$NON-NLS-1$
 		}
 	}
 	
 	public String getPackage() {
 		return fPackageHandle;
 	}
 			
 	protected IEvaluationContext getEvaluationContext() {
 		if (fEvaluationContext == null) {
 			IJavaProject project= getJavaProject();
 			if (project != null) {
 				fEvaluationContext= project.newEvaluationContext();
 			}
 		}
 		if (fEvaluationContext != null && fPackageHandle != null) {		
 			String packageName = JavaCore.create(fPackageHandle).getElementName();	
 			fEvaluationContext.setPackageName(packageName);
 		}
 		return fEvaluationContext;
 	}
 	
 	public IJavaProject getJavaProject() {
 		if (fJavaProject == null) {
 			try {
 				fJavaProject = findJavaProject();
 			} catch (JavaModelException e) {
 				showError(e.getStatus());
 			}
 		}
 		return fJavaProject;
 	}
 	
 	public void shutDownVM() {
 		DebugPlugin.getDefault().removeDebugEventListener(this);
 
 		// The real shut down
 		IDebugTarget target= fVM;
 		if (fVM != null) {
 			try {
 				IBreakpoint bp = ScrapbookLauncherDelegate.getDefault().getMagicBreakpoint(fVM);
 				if (bp != null) {
 					fVM.breakpointRemoved(bp, null);
 				}
 				if (getThread() != null) {
 					getThread().resume();
 				}
 				fVM.terminate();
 			} catch (DebugException e) {
 				ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.shutdown"), null, e.getStatus()); //$NON-NLS-1$
 				return;
 			}
 			vmTerminated();
 			ScrapbookLauncherDelegate.getDefault().cleanup(target);
 		}
 	}
 	
 	/**
 	 * The VM has terminated, update state
 	 */
 	protected void vmTerminated() {
 		DebugPlugin.getDefault().getLaunchManager().removeLaunch(fVM.getLaunch());
 		fVM= null;
 		fThread= null;
 		fEvaluationContext= null;
 		fLaunchedClassPath= null;
 		fEngine= null;
 		fireEvalStateChanged();
 	}
 	
 	public void addSnippetStateChangedListener(ISnippetStateChangedListener listener) {
 		if (!fSnippetStateListeners.contains(listener))
 			fSnippetStateListeners.add(listener);
 	}
 	
 	public void removeSnippetStateChangedListener(ISnippetStateChangedListener listener) {
 		if (fSnippetStateListeners != null)
 			fSnippetStateListeners.remove(listener);
 	}
 
 	public void fireEvalStateChanged() {
 		Runnable r= new Runnable() {
 			public void run() {			
 				List v= new ArrayList(fSnippetStateListeners);
 				for (int i= 0; i < v.size(); i++) {
 					ISnippetStateChangedListener l= (ISnippetStateChangedListener) v.get(i);
 					l.snippetStateChanged(JavaSnippetEditor.this);
 				}
 			}
 		};
 		getShell().getDisplay().asyncExec(r);
 	}
 	
 	protected void evaluate(final String snippet) {
 		if (fAttempts < 200 && getThread() == null) {
 			// wait for our main thread to suspend
 			fAttempts++;
 			try {
 				Thread.sleep(50);
 			} catch (InterruptedException e) {
 			}
 			Runnable r = new Runnable() {
 				public void run() {
 					evaluate(snippet);
 				}
 			};
 			getShell().getDisplay().asyncExec(r);
 			return;
 		}
 		if (getThread() == null) {
 			IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, 
 				SnippetMessages.getString("SnippetEditor.error.nocontext"), null); //$NON-NLS-1$
 			ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.evaluating"), null, status); //$NON-NLS-1$
 			evaluationEnds();
 			return;
 		}
 		try {
 			if (fPackageHandle != null) {
 				String packageName = JavaCore.create(fPackageHandle).getElementName();		
 				getEvaluationEngine().setPackageName(packageName);
 			}
 			getEvaluationEngine().evaluate(snippet,getThread(), this);
 		} catch (DebugException e) {
 			ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.evaluating"), null, e.getStatus()); //$NON-NLS-1$
 			evaluationEnds();
 		}
 	}
 
 	public void evaluationComplete(final IEvaluationResult result) {
 		Runnable r = new Runnable() {
 			public void run() {
				boolean severeProblems= false;
 				if (result.hasProblems()) {
 					IMarker[] problems = result.getProblems();
 					int count= problems.length;
 					if (count == 0) {
 						showException(result.getException());
 					} else {
 						severeProblems= showAllProblems(problems);
 						if (!severeProblems) {
 							//warnings only..check for exception
							if (result.getException() != null) {
								showException(result.getException());
							}
 						}
 					}
 				} 
 				final IJavaValue value= result.getValue();
 				if (value != null && !severeProblems) {
 					switch (fResultMode) {
 					case RESULT_DISPLAY:
 						Runnable r = new Runnable() {
 							public void run() {
 								displayResult(value, result.getThread());
 							}
 						};
 						getSite().getShell().getDisplay().asyncExec(r);
 						break;
 					case RESULT_INSPECT:
 						String snippet= result.getSnippet().trim();
 						int snippetLength= snippet.length();
 						if (snippetLength > 30) {
 							snippet = snippet.substring(0, 15) + SnippetMessages.getString("SnippetEditor.ellipsis") + snippet.substring(snippetLength - 15, snippetLength);  //$NON-NLS-1$
 						}
 						snippet= snippet.replace('\n', ' ');
 						snippet= snippet.replace('\r', ' ');
 						snippet= snippet.replace('\t', ' ');
 						showExpressionView();
 						JavaInspectExpression exp = new JavaInspectExpression(snippet, value);
 						DebugPlugin.getDefault().getExpressionManager().addExpression(exp);
 						break;
 					case RESULT_RUN:
 						// no action
 						break;
 					}
 				}
 				evaluationEnds();
 			}
 		};
 		Control control= getVerticalRuler().getControl();
 		if (!control.isDisposed()) {
 			control.getDisplay().asyncExec(r);
 		}		
 	}
 	
 	/**
 	 * Make the expression view visible or open one
 	 * if required.
 	 */
 	protected void showExpressionView() {
 		IWorkbenchPage page = JDIDebugUIPlugin.getDefault().getActivePage();
 		if (page != null) {
 			IViewPart part = page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
 			if (part == null) {
 				try {
 					page.showView(IDebugUIConstants.ID_EXPRESSION_VIEW);
 				} catch (PartInitException e) {
 					showError(e.getStatus());
 				}
 			} else {
 				page.bringToTop(part);
 			}
 		}
 	}
 		
 	public void codeComplete(ResultCollector collector) throws JavaModelException {
 		IDocument d= getSourceViewer().getDocument();
 		ITextSelection selection= (ITextSelection)getSelectionProvider().getSelection();
 		int start= selection.getOffset();
 		String snippet= d.get();	
 		IEvaluationContext e= getEvaluationContext();
 		if (e != null) 
 			e.codeComplete(snippet, start, collector);
 	}
 		 
 	public IJavaElement[] codeResolve() throws JavaModelException {
 		IDocument d= getSourceViewer().getDocument();
 		ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
 		int start= selection.getOffset();
 		int len= selection.getLength();
 		
 		String snippet= d.get();	
 		IEvaluationContext e= getEvaluationContext();
 		if (e != null) 
 			return e.codeSelect(snippet, start, len);
 		return null;
 	}	
 	public void showError(IStatus status) {
 		evaluationEnds();
 		if (!status.isOK())
 			ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.evaluating2"), null, status); //$NON-NLS-1$
 	}
 	
 	protected void showError(String message) {
 		Status status= new Status(IStatus.ERROR, JDIDebugUIPlugin.getPluginId(), IStatus.ERROR, message, null);
 		showError(status);
 	}
 	
 	public void displayResult(IJavaValue result, IJavaThread thread) {
 		StringBuffer resultString= new StringBuffer();
 		try {
 			IJavaType type = result.getJavaType();
 			if (type != null) {
 				String sig= type.getSignature();
 				if ("V".equals(sig)) { //$NON-NLS-1$
 					resultString.append(SnippetMessages.getString("SnippetEditor.noreturnvalue")); //$NON-NLS-1$
 				} else {
 					if (sig != null) {
 						resultString.append(SnippetMessages.getFormattedString("SnippetEditor.typename", result.getReferenceTypeName())); //$NON-NLS-1$
 					} else {
 						resultString.append(" "); //$NON-NLS-1$
 					}   
 					resultString.append(evaluateToString(result, thread));
 				}
 			} else {
 				resultString.append(result.getValueString());
 			}
 		} catch(DebugException e) {
 			ErrorDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.error.toString"), null, e.getStatus()); //$NON-NLS-1$
 		}
 			
 		try {
 			getSourceViewer().getDocument().replace(fSnippetEnd, 0, resultString.toString());
 		} catch (BadLocationException e) {
 		}
 		
 		selectAndReveal(fSnippetEnd, resultString.length());
 	}
 	
 	/**
 	 * Returns the result of evaluating 'toString' on the given
 	 * value.
 	 * 
 	 * @param value object or primitive data type the 'toString'
 	 *  is required for
 	 * @param thread the thread in which to evaluate 'toString'
 	 * @return the result of evaluating toString
 	 * @exception DebugException if an exception occurs during the
 	 *  evaluation.
 	 */
 	protected String evaluateToString(IJavaValue value, IJavaThread thread) throws DebugException {
 		if (value instanceof IJavaObject) {
 			IJavaValue result = ((IJavaObject)value).sendMessage("toString","()Ljava/lang/String;", null, thread, false); //$NON-NLS-1$ //$NON-NLS-2$
 			return result.getValueString();
 		} else {
 			return value.getValueString();
 		}
 	}	
 	
 	protected boolean showAllProblems(IMarker[] problems) {
 		IDocument document = getSourceViewer().getDocument();
 		String delimiter = document.getLegalLineDelimiters()[0];
 		int insertionPoint = fSnippetStart;
 		try {
 			insertionPoint = document.getLineOffset(document.getLineOfOffset(fSnippetStart));
 		} catch (BadLocationException ble) {
 		}
 		int firstInsertionPoint = insertionPoint;
 		boolean severeProblem=false;
 		for (int i = 0; i < problems.length; i++) {
 			IMarker problem= problems[i];
 			if (problem.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
 				//only show problems that are greater severity than a warning
 				insertionPoint = showOneProblem(document, problem, insertionPoint, delimiter);
 				severeProblem= true;
 			}
 		}
 		if (severeProblem) {
 			selectAndReveal(firstInsertionPoint, insertionPoint - firstInsertionPoint);
 			fSnippetStart = insertionPoint;
 		}
 		return severeProblem;
 	}
 
 	protected int showOneProblem(IDocument document, IMarker problem, int insertionPoint, String delimiter) {
 		String message= SnippetMessages.getString("SnippetEditor.error.unqualified"); //$NON-NLS-1$
 		message= problem.getAttribute(IMarker.MESSAGE, message) + delimiter;
 		try {
 			document.replace(insertionPoint, 0, message);
 		} catch (BadLocationException e) {
 		}
 		return insertionPoint += message.length();
 	}	
 
 	protected void showException(Throwable exception) {
 		if (exception instanceof DebugException) {
 			DebugException de = (DebugException)exception;
 			Throwable t= de.getStatus().getException();
 			if (t != null) {
 				// show underlying exception
 				showUnderlyingException(t);
 				return;
 			}
 		}
 		ByteArrayOutputStream bos= new ByteArrayOutputStream();
 		PrintStream ps= new PrintStream(bos, true);
 		exception.printStackTrace(ps);
 		try {
 			getSourceViewer().getDocument().replace(fSnippetEnd, 0, bos.toString());
 		} catch (BadLocationException e) {
 		}
 		selectAndReveal(fSnippetEnd, bos.size());
 	}
 	
 	protected void showUnderlyingException(Throwable t) {
 		if (t instanceof InvocationException) {
 			InvocationException ie= (InvocationException)t;
 			ObjectReference ref= ie.exception();
 			String eName= ref.referenceType().name();
 			String message= SnippetMessages.getFormattedString("SnippetEditor.exception", eName); //$NON-NLS-1$
 			try {
 				getSourceViewer().getDocument().replace(fSnippetEnd, 0, message);
 			} catch (BadLocationException e) {
 			}
 			selectAndReveal(fSnippetEnd, message.length());
 		} else {
 			showException(t);
 		}
 	}
 	
 	protected IJavaProject findJavaProject() throws JavaModelException {
 		Object input= getEditorInput();
 		if (input instanceof IFileEditorInput) {
 			IFileEditorInput file= (IFileEditorInput)input;
 			IProject p= file.getFile().getProject();
 			try {
 				if (p.getNature(JavaCore.NATURE_ID) != null) {
 					return JavaCore.create(p);
 				}
 			} catch (CoreException ce) {
 				throw new JavaModelException(ce);
 			}
 		}
 		return null;
 	}
 		
 	protected boolean classPathHasChanged() {
 		String[] classpath= getClassPath(getJavaProject());
 		if (fLaunchedClassPath != null && !classPathsEqual(fLaunchedClassPath, classpath)) {
 			MessageDialog.openError(getShell(), SnippetMessages.getString("SnippetEditor.warning"), SnippetMessages.getString("SnippetEditor.warning.cpchange")); //$NON-NLS-2$ //$NON-NLS-1$
 			return true;
 		}
 		return false;
 	}
 	
 	protected boolean classPathsEqual(String[] path1, String[] path2) {
 		if (path1.length != path2.length)
 			return false;
 		for (int i= 0; i < path1.length; i++) {
 			if (!path1[i].equals(path2[i]))
 				return false;
 		}
 		return true;
 	}
 		
 	protected synchronized void evaluationStarts() {
 		if (fThread != null) {
 			try {
 				fThread.resume();
 				fThread = null;
 			} catch (DebugException e) {
 				showException(e);
 				return;
 			}
 		}		
 		fEvaluating = true;
 		fAttempts = 0;
 		setTitleImage();
 		fireEvalStateChanged();
 		showStatus(SnippetMessages.getString("SnippetEditor.evaluating")); //$NON-NLS-1$
 		getSourceViewer().setEditable(false);
 	}
 	
 	/** 
 	 * Sets the tab image to indicate whether in the process of
 	 * evaluating or not.
 	 */
 	protected void setTitleImage() {
 		Image image=null;
 		if (fEvaluating) {
 			fOldTitleImage= getTitleImage();
 			image= JavaDebugImages.get(JavaDebugImages.IMG_OBJS_SNIPPET_EVALUATING);
 		} else {
 			image= fOldTitleImage;
 			fOldTitleImage= null;
 		}
 		if (image != null) {
 			setTitleImage(image);
 		}
 	}
 		
 	protected void evaluationEnds() {
 		fEvaluating= false;
 		setTitleImage();
 		fireEvalStateChanged();
 		showStatus(""); //$NON-NLS-1$
 		getSourceViewer().setEditable(true);
 	}
 	
 	protected void showStatus(String message) {
 		IEditorSite site=(IEditorSite)getSite();
 		EditorActionBarContributor contributor= (EditorActionBarContributor)site.getActionBarContributor();
 		contributor.getActionBars().getStatusLineManager().setMessage(message);
 	}
 	
 	protected String[] getClassPath(IJavaProject project) {
 		try {
 			return JavaRuntime.computeDefaultRuntimeClassPath(project);
 		} catch (CoreException e) {
 			return new String[0];
 		}
 	}
 	
 	protected Shell getShell() {
 		return getSite().getShell();
 	}
 	
 	public void handleDebugEvent(DebugEvent e) {
 		Object source = e.getSource();
 		if (source instanceof IDebugElement) {
 			IDebugElement de = (IDebugElement)source;
 			if (de instanceof IDebugTarget) {
 				if (de.getDebugTarget().equals(fVM)) {
 					if (e.getKind() == DebugEvent.TERMINATE) {
 						Runnable r = new Runnable() {
 							public void run() {
 								vmTerminated();
 							}
 						};
 						getShell().getDisplay().asyncExec(r);
 					}
 				}
 			} else if (de instanceof IJavaThread) {
 				if (e.getKind() == DebugEvent.SUSPEND) {
 					IJavaThread jt = (IJavaThread)de;
 					try {
 						IJavaStackFrame f= (IJavaStackFrame)jt.getTopStackFrame();
 						if (f != null) {
 							IBreakpoint bp = jt.getBreakpoint();
 							if (e.getDetail() == DebugEvent.STEP_END && f.getLineNumber() == 14 && f.getDeclaringTypeName().equals("org.eclipse.jdt.internal.debug.ui.snippeteditor.ScrapbookMain1")) { //$NON-NLS-1$
 								fThread = jt;
 							} else if (e.getDetail() == DebugEvent.BREAKPOINT &&  bp != null && bp.equals(ScrapbookLauncherDelegate.getDefault().getMagicBreakpoint(jt.getDebugTarget()))) {
 								// locate the 'eval' method and step over
 								IStackFrame[] frames = jt.getStackFrames();
 								for (int i = 0; i < frames.length; i++) {
 									IJavaStackFrame frame = (IJavaStackFrame)frames[i];
 									if (frame.getReceivingTypeName().equals("org.eclipse.jdt.internal.debug.ui.snippeteditor.ScrapbookMain1") && frame.getName().equals("eval")) { //$NON-NLS-1$ //$NON-NLS-2$
 										frame.stepOver();
 										break;
 									}
 								}
 							}
 						}
 					} catch (DebugException ex) {
 						JDIDebugUIPlugin.log(ex);
 					}
 				}
 			}
 		}
 	}
 	
 	/**
 	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
 	 */
 	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
 		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
 		return textTools.affectsBehavior(event);
 	}
 	
 	/**
 	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
 	 */
 	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
 		JDISourceViewer isv= (JDISourceViewer) getSourceViewer();
 			if (isv != null) {
 				IContentAssistant assistant= isv.getContentAssistant();
 				if (assistant instanceof ContentAssistant) {
 					JDIContentAssistPreference.changeConfiguration((ContentAssistant) assistant, event);
 				}				
 				
 				super.handlePreferenceStoreChanged(event);
 			}
 	}
 	
 	protected IJavaThread getThread() {
 		return fThread;
 	}
 	
 	protected void launchVM() {
 		DebugPlugin.getDefault().addDebugEventListener(JavaSnippetEditor.this);
 		fLaunchedClassPath = getClassPath(getJavaProject());
 		final ILauncher launcher = ScrapbookLauncherDelegate.getLauncher();
 		Runnable r = new Runnable() {
 			public void run() {
 				launcher.launch(new Object[] {getPage()}, ILaunchManager.DEBUG_MODE);
 			}
 		};
 		BusyIndicator.showWhile(getShell().getDisplay(), r);
 	}
 	
 	protected IFile getPage() {
 		return ((FileEditorInput)getEditorInput()).getFile();
 	}
 	
 	/**
 	 * Updates all selection dependent actions.
 	 */
 	protected void updateSelectionDependentActions() {
 		super.updateSelectionDependentActions();
 		fireEvalStateChanged();
 	}
 	
    /**
     * Terminates existing VM on a rename of the editor
 	* @see WorkbenchPart#setTitle
  	*/
 	protected void setTitle(String title) {
 		if(isVMLaunched()) {
 			shutDownVM();
 		}
 		super.setTitle(title);
 	}
 	
 	/**
 	 * Returns whether this editor has been opened on a resource that
 	 * is in a Java project.
 	 */
 	protected boolean isInJavaProject() {
 		try {
 			return findJavaProject() != null;
 		} catch (JavaModelException jme) {
 		}
 		return false;
 	}
 	
 	/**
 	 * Displays an error dialog indicating that evaluation
 	 * cannot occur outside of a Java Project.
 	 */
 	protected void reportNotInJavaProjectError() {
 		String projectName= null;
 		Object input= getEditorInput();
 		if (input instanceof IFileEditorInput) {
 			IFileEditorInput file= (IFileEditorInput)input;
 			IProject p= file.getFile().getProject();
 			projectName= p.getName();
 		}
 		String message= ""; //$NON-NLS-1$
 		if (projectName != null) {
 			message = projectName + SnippetMessages.getString("JavaSnippetEditor._is_not_a_Java_Project._n_1"); //$NON-NLS-1$
 		}
 		showError(message + SnippetMessages.getString("JavaSnippetEditor.Unable_to_perform_evaluation_outside_of_a_Java_Project_2")); //$NON-NLS-1$
 	}
 	
 	/**
 	 * Asks the user for the workspace path
 	 * of a file resource and saves the document there.
 	 */
 	protected void performSaveAs(IProgressMonitor progressMonitor) {
 		Shell shell= getSite().getShell();
 		SaveAsDialog dialog= new SaveAsDialog(shell);
 		dialog.open();
 		IPath path= dialog.getResult();
 		
 		if (path == null) {
 			if (progressMonitor != null)
 				progressMonitor.setCanceled(true);
 			return;
 		}
 			
 		IWorkspace workspace= ResourcesPlugin.getWorkspace();
 		IFile file= workspace.getRoot().getFile(path);
 		final IEditorInput newInput= new FileEditorInput(file);
 		
 		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
 			public void execute(final IProgressMonitor monitor) throws CoreException {
 				IDocumentProvider dp= getDocumentProvider();
 				dp.saveDocument(monitor, newInput, dp.getDocument(getEditorInput()), true);
 			}
 		};
 		
 		boolean success= false;
 		try {
 			getDocumentProvider().aboutToChange(newInput);
 			new ProgressMonitorDialog(shell).run(false, true, op);
 			success= true;
 		} catch (InterruptedException x) {
 		} catch (InvocationTargetException x) {
 			String title= SnippetMessages.getString("JavaSnippetEditor.Problems_During_Save_As..._3");  //$NON-NLS-1$
 			String msg= SnippetMessages.getString("JavaSnippetEditor.Save_could_not_be_completed.__4") +  x.getTargetException().getMessage(); //$NON-NLS-1$
 			MessageDialog.openError(shell, title, msg);
 		} finally {
 			getDocumentProvider().changed(newInput);
 			if (success) {
 				setInput(newInput);
 			}
 		}
 		
 		if (progressMonitor != null) {
 			progressMonitor.setCanceled(!success);
 		}
 	}
 	
 	/**
 	 * @see IEditorPart
 	 */
 	public boolean isSaveAsAllowed() {
 		return true;
 	}
 	
 	protected IClassFileEvaluationEngine getEvaluationEngine() {
 		if (fEngine == null) {
 			IPath outputLocation =	getJavaProject().getProject().getPluginWorkingLocation(JDIDebugUIPlugin.getDefault().getDescriptor());
 			java.io.File f = new java.io.File(outputLocation.toOSString());
 			fEngine = EvaluationManager.newClassFileEvaluationEngine(getJavaProject(), (IJavaDebugTarget)getThread().getDebugTarget(), f);
 		}
 		return fEngine;
 	}
 	
 	/**
 	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
 	 */
 	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
 		return new JDISourceViewer(parent, ruler, styles);
 	}
 }
