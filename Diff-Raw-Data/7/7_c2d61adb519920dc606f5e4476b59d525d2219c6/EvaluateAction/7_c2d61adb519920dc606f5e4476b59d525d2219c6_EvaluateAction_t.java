 package org.eclipse.jdt.internal.debug.ui.actions;
 
 /**********************************************************************
 Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 This file is made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html
 **********************************************************************/
 
 
 import java.text.MessageFormat;
 import java.util.Iterator;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.model.ISourceLocator;
 import org.eclipse.debug.core.model.IStackFrame;
 import org.eclipse.debug.core.model.IValue;
 import org.eclipse.debug.ui.DebugUITools;
 import org.eclipse.debug.ui.IDebugModelPresentation;
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.debug.ui.IDebugView;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.debug.core.IJavaArray;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaObject;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.debug.core.IJavaValue;
 import org.eclipse.jdt.debug.core.IJavaVariable;
 import org.eclipse.jdt.debug.core.JDIDebugModel;
 import org.eclipse.jdt.debug.eval.IEvaluationEngine;
 import org.eclipse.jdt.debug.eval.IEvaluationListener;
 import org.eclipse.jdt.debug.eval.IEvaluationResult;
 import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;
 import org.eclipse.jdt.internal.debug.ui.EvaluationContextManager;
 import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
 import org.eclipse.jdt.internal.debug.ui.display.DataDisplay;
 import org.eclipse.jdt.internal.debug.ui.display.IDataDisplay;
 import org.eclipse.jdt.internal.debug.ui.display.JavaInspectExpression;
 import org.eclipse.jdt.internal.debug.ui.snippeteditor.ISnippetStateChangedListener;
 import org.eclipse.jdt.internal.debug.ui.snippeteditor.JavaSnippetEditor;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.text.ITextSelection;
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionProvider;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IEditorActionDelegate;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IObjectActionDelegate;
 import org.eclipse.ui.IPartListener;
 import org.eclipse.ui.IViewActionDelegate;
 import org.eclipse.ui.IViewPart;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.IWorkbenchWindowActionDelegate;
 import org.eclipse.ui.PartInitException;
 
 import com.sun.jdi.InvocationException;
 import com.sun.jdi.ObjectReference;
 
 
 /**
  * Action to do simple code evaluation. The evaluation
  * is done in the UI thread and the expression and result are
  * displayed using the IDataDisplay.
  */
 public abstract class EvaluateAction implements IEvaluationListener, IWorkbenchWindowActionDelegate, IObjectActionDelegate, IEditorActionDelegate, IPartListener, IViewActionDelegate, ISnippetStateChangedListener {
 
 	private IAction fAction;
 	private IWorkbenchPart fTargetPart;
 	private IWorkbenchWindow fWindow;
 	private Object fSelection;
 	
 	/**
 	 * Is the action waiting for an evaluation.
 	 */
 	private boolean fEvaluating;
 	
 	/**
 	 * The new target part to use with the evaluation completes.
 	 */
 	private IWorkbenchPart fNewTargetPart= null;
 	
 	/**
 	 * Used to resolve editor input for selected stack frame
 	 */
 	private IDebugModelPresentation fPresentation;
 			
 	public EvaluateAction() {
 		super();
 	}
 	
 	/**
 	 * Returns the 'object' context for this evaluation,
 	 * or <code>null</code> if none. If the evaluation is being performed
 	 * in the context of the variables view/inspector. Then
 	 * perform the evaluation in the context of the
 	 * selected value.
 	 * 
 	 * @return Java object or <code>null</code>
 	 */
 	protected IJavaObject getObjectContext() {
 		IWorkbenchPage page= JDIDebugUIPlugin.getActivePage();
 		if (page != null) {
 			IWorkbenchPart activePart= page.getActivePart();
 			if (activePart != null) {
 				IDebugView a = (IDebugView)activePart.getAdapter(IDebugView.class);
 				if (a != null) {
 					if (a.getViewer() != null) {
 						ISelection s = a.getViewer().getSelection();
 						if (s instanceof IStructuredSelection) {
 							IStructuredSelection structuredSelection = (IStructuredSelection)s;
 							if (structuredSelection.size() == 1) {
 								Object selection= structuredSelection.getFirstElement();
 								if (selection instanceof IJavaVariable) {
 									IJavaVariable var = (IJavaVariable)selection;
 									// if 'this' is selected, use stack frame context
 									try {
 										if (!var.getName().equals("this")) { //$NON-NLS-1$
 											IValue value= var.getValue();
 											if (value instanceof IJavaObject && !(value instanceof IJavaArray)) {
 												return (IJavaObject)value;
 											}
 										} 
 									} catch (DebugException e) {
 										JDIDebugUIPlugin.log(e);
 									}
 								} else if (selection instanceof JavaInspectExpression) {
 									IValue value= ((JavaInspectExpression)selection).getValue();
 									if (value instanceof IJavaObject && !(value instanceof IJavaArray)) {
 										return (IJavaObject)value;
 									}
 								}
 							}
 						}
 					}
 				}
 			}
 		}
 		return null;		
 	}
 	
 	/**
 	 * Finds the currently selected stack frame in the UI.
 	 * Stack frames from a scrapbook launch are ignored.
 	 */
 	protected IJavaStackFrame getStackFrameContext() {
 		IWorkbenchPart part = getTargetPart();
 		IJavaStackFrame frame = null;
 		if (part == null) {
 			frame = EvaluationContextManager.getEvaluationContext(getWindow());
 		} else {
 			frame = EvaluationContextManager.getEvaluationContext(part);
 		}		
 		return frame;
 	}
 	
 	/**
 	 * @see IEvaluationListener#evaluationComplete(IEvaluationResult)
 	 */
 	public void evaluationComplete(final IEvaluationResult result) {
 		// if plug-in has shutdown, ignore - see bug# 8693
 		if (JDIDebugUIPlugin.getDefault() == null) {
 			return;
 		}
 		
 		final IJavaValue value= result.getValue();
 		if (result.hasErrors() || value != null) {
 			final Display display= JDIDebugUIPlugin.getStandardDisplay();
 			if (display.isDisposed()) {
 				return;
 			}
 			if (result.hasErrors()) {
 				display.asyncExec(new Runnable() {
 					public void run() {
 						if (display.isDisposed()) {
 							return;
 						}
 						reportErrors(result);
 						evaluationCleanup();
 					}
 				});
 			} else if (value != null) {
 				displayResult(result);
 			}
 		}
 	}
 	
 	protected void evaluationCleanup() {
 		setEvaluating(false);
 		setTargetPart(fNewTargetPart);
 	}
 	/**
 	 * Display the given evaluation result.
 	 */
 	abstract protected void displayResult(IEvaluationResult result);	
 	
 	protected void run() {		
 		// eval in context of object or stack frame
 		IJavaObject object = getObjectContext();		
 		IJavaStackFrame stackFrame= getStackFrameContext();
 		if (stackFrame == null) {
 			reportError(ActionMessages.getString("Evaluate.error.message.stack_frame_context")); //$NON-NLS-1$
 			return;
 		}
 		
		// check for nested evaluation
		IJavaThread thread = (IJavaThread)stackFrame.getThread();
		if (thread.isPerformingEvaluation()) {
			reportError(ActionMessages.getString("EvaluateAction.Cannot_perform_nested_evaluations._1")); //$NON-NLS-1$
			return;
		}
		
 		setNewTargetPart(getTargetPart());
 		if (stackFrame.isSuspended()) {
 			IJavaElement javaElement= getJavaElement(stackFrame);
 			if (javaElement != null) {
 				IJavaProject project = javaElement.getJavaProject();
 				IEvaluationEngine engine = null;
 				try {
 					Object selection= getSelectedObject();
 					if (!(selection instanceof String)) {
 						return;
 					}
 					String expression= (String)selection;
 					
 					engine = JDIDebugUIPlugin.getDefault().getEvaluationEngine(project, (IJavaDebugTarget)stackFrame.getDebugTarget());
 					setEvaluating(true);
 					if (object == null) {
 						engine.evaluate(expression, stackFrame, this, DebugEvent.EVALUATION, true);
 					} else {
 						engine.evaluate(expression, object, (IJavaThread)stackFrame.getThread(), this, DebugEvent.EVALUATION, true);
 					}
 					return;
 				} catch (CoreException e) {
 					reportError(getExceptionMessage(e));
 				}
 			} else {
 				reportError(ActionMessages.getString("Evaluate.error.message.src_context")); //$NON-NLS-1$
 			}
 		} else {
 			// thread not suspended
 			reportError(ActionMessages.getString("EvaluateAction.Thread_not_suspended_-_unable_to_perform_evaluation._1")); //$NON-NLS-1$
 		}
 		evaluationCleanup();
 	}
 		
 	protected IJavaElement getJavaElement(IStackFrame stackFrame) {
 		
 		// Get the corresponding element.
 		ILaunch launch = stackFrame.getLaunch();
 		if (launch == null) {
 			return null;
 		}
 		ISourceLocator locator= launch.getSourceLocator();
 		if (locator == null)
 			return null;
 		
 		Object sourceElement = locator.getSourceElement(stackFrame);
 		if (sourceElement instanceof IJavaElement) {
 			return (IJavaElement) sourceElement;
 		}			
 		return null;
 	}
 	
 	/**
 	 * Updates the enabled state of the action that this is a
 	 * delegate for.
 	 */
 	protected void update() {
 		IAction action= getAction();
 		if (action != null) {
 			resolveSelectedObject();
 		}
 	}
 	
 	/**
 	 * Resolves the selected object in the target part, or <code>null</code>
 	 * if there is no selection.
 	 */
 	protected void resolveSelectedObject() {
 		Object selectedObject= null;
 		ISelection selection= getTargetSelection();
 		if (selection instanceof ITextSelection) {
 			String text= ((ITextSelection)selection).getText();
 			if (textHasContent(text)) {
 				selectedObject= text;
 			}
 		} else if (selection instanceof IStructuredSelection) {
 			if (!selection.isEmpty()) {
 				if (getTargetPart().getSite().getId().equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
 					//work on the editor selection
 					setTargetPart(getTargetPart().getSite().getPage().getActiveEditor());
 					selection= getTargetSelection();
 					if (selection instanceof ITextSelection) {
 						String text= ((ITextSelection)selection).getText();
 						if (textHasContent(text)) {
 							selectedObject= text;
 						}
 					}
 				} else {
 					IStructuredSelection ss= (IStructuredSelection)selection;
 					Iterator elements = ss.iterator();
 					while (elements.hasNext()) {
 						if (!(elements.next() instanceof IJavaVariable)) {
 							setSelectedObject(null);
 							return;
 						}
 					}
 					selectedObject= ss;
 				}			
 			}
 		}
 		setSelectedObject(selectedObject);
 	}
 	
 	protected ISelection getTargetSelection() {
 		IWorkbenchPart part = getTargetPart();
 		if (part != null) {
 			ISelectionProvider provider = part.getSite().getSelectionProvider();
 			if (provider != null) {
 				return provider.getSelection();
 			}
 		}
 		return null;
 	}
 	
 	/**
 	 * Resolve an editor input from the source element of the stack frame
 	 * argument, and return whether it's equal to the editor input for the
 	 * editor that owns this action.
 	 */
 	protected boolean compareToEditorInput(IStackFrame stackFrame) {
 		ILaunch launch = stackFrame.getLaunch();
 		if (launch == null) {
 			return false;
 		}
 		ISourceLocator locator= launch.getSourceLocator();
 		if (locator == null) {
 			return false;
 		}
 		Object sourceElement = locator.getSourceElement(stackFrame);
 		if (sourceElement == null) {
 			return false;
 		}
 		IEditorInput sfEditorInput= getDebugModelPresentation().getEditorInput(sourceElement);
 		if (getTargetPart() instanceof IEditorPart) {
 			return ((IEditorPart)getTargetPart()).getEditorInput().equals(sfEditorInput);
 		}
 		return false;
 	}
 	
 	protected Shell getShell() {
 		if (getTargetPart() != null) {
 			return getTargetPart().getSite().getShell();
 		} else {
 			return JDIDebugUIPlugin.getActiveWorkbenchShell();
 		}
 	}
 	
 	protected IDataDisplay getDataDisplay() {
 		IDataDisplay display= getDirectDataDisplay();
 		if (display != null) {
 			return display;
 		}
 		IWorkbenchPage page= JDIDebugUIPlugin.getActivePage();
 		if (page != null) {
 			IWorkbenchPart activePart= page.getActivePart();
 			if (activePart != null) {
 				IViewPart view = page.findView(IJavaDebugUIConstants.ID_DISPLAY_VIEW);;
 				if (view == null) {
 					try {
 						view= page.showView(IJavaDebugUIConstants.ID_DISPLAY_VIEW);
 					} catch (PartInitException e) {
 						JDIDebugUIPlugin.errorDialog(ActionMessages.getString("EvaluateAction.Cannot_open_Display_view"), e); //$NON-NLS-1$
 					} finally {
 						page.activate(activePart);
 					}
 				}
 				if (view != null) {
 					page.bringToTop(view);
 					return (IDataDisplay)view.getAdapter(IDataDisplay.class);
 				}			
 			}
 		}
 		
 		return null;		
 	}	
 	
 	protected IDataDisplay getDirectDataDisplay() {
 		IWorkbenchPart part= getTargetPart();
 		if (part != null) {
 			IDataDisplay display= (IDataDisplay)part.getAdapter(IDataDisplay.class);
 //			if (display == null) {
 //				ITextViewer viewer = (ITextViewer)part.getAdapter(ITextViewer.class);
 //				if (viewer != null) {
 //					display= new DataDisplay(viewer);
 //				}
 //			}
 			if (display != null) {
 				IWorkbenchPage page= JDIDebugUIPlugin.getActivePage();
 				if (page != null) {
 					IWorkbenchPart activePart= page.getActivePart();
 					if (activePart != null) {
 						if (activePart != part) {
 							page.activate(part);
 						}
 					}
 				}
 				return display;
 			}
 		}
 		IWorkbenchPage page= JDIDebugUIPlugin.getActivePage();
 		if (page != null) {
 			IWorkbenchPart activePart= page.getActivePart();
 			if (activePart != null) {
 				IDataDisplay display= (IDataDisplay)activePart.getAdapter(IDataDisplay.class);
 				if (display != null) {
 					return display;
 				}	
 				ITextViewer viewer = (ITextViewer)activePart.getAdapter(ITextViewer.class);
 				if (viewer != null) {
 					return new DataDisplay(viewer);
 				}
 			}
 		}
 		return null;
 	}
 	protected boolean textHasContent(String text) {
 		if (text != null) {
 			int length= text.length();
 			if (length > 0) {
 				for (int i= 0; i < length; i++) {
 					if (Character.isLetterOrDigit(text.charAt(i))) {
 						return true;
 					}
 				}
 			}
 		}
 		return false;
 	}
 	
 	/**
 	 * Displays a failed evaluation message in the data display.
 	 */
 	protected void reportErrors(IEvaluationResult result) {
 		String message= getErrorMessage(result);
 		reportError(message);
 	}
 	
 	protected void reportError(String message) {
 		IDataDisplay dataDisplay= getDirectDataDisplay();
 		if (dataDisplay != null) {
 			if (message.length() != 0) {
 				dataDisplay.displayExpressionValue(MessageFormat.format(ActionMessages.getString("EvaluateAction.(evaluation_failed)_Reason"), new String[] {format(message)})); //$NON-NLS-1$
 			} else {
 				dataDisplay.displayExpressionValue(ActionMessages.getString("EvaluateAction.(evaluation_failed)_1")); //$NON-NLS-1$
 			}
 		} else {
 			Status status= new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, message, null);
 			ErrorDialog.openError(getShell(), ActionMessages.getString("Evaluate.error.title.eval_problems"), null, status); //$NON-NLS-1$
 		}
 	}
 	
 	private String format(String message) {
 		StringBuffer result= new StringBuffer();
 		int index= 0, pos;
 		while ((pos= message.indexOf('\n', index)) != -1) {
 			result.append("\t\t").append(message.substring(index, index= pos + 1)); //$NON-NLS-1$
 		}
 		if (index < message.length()) {
 			result.append("\t\t").append(message.substring(index)); //$NON-NLS-1$
 		}
 		return result.toString();
 	}
 	
 	protected String getExceptionMessage(Throwable exception) {
 		if (exception instanceof CoreException) {
 			CoreException ce = (CoreException)exception;
 			Throwable throwable= ce.getStatus().getException();
 			if (throwable instanceof com.sun.jdi.InvocationException) {
 				return getInvocationExceptionMessage((com.sun.jdi.InvocationException)throwable);
 			} else if (throwable instanceof CoreException) {
 				// Traverse nested CoreExceptions
 				return getExceptionMessage(throwable);
 			}
 			return ce.getStatus().getMessage();
 		}
 		String message= MessageFormat.format(ActionMessages.getString("Evaluate.error.message.direct_exception"), new Object[] { exception.getClass() }); //$NON-NLS-1$
 		if (exception.getMessage() != null) {
 			message= MessageFormat.format(ActionMessages.getString("Evaluate.error.message.exception.pattern"), new Object[] { message, exception.getMessage() }); //$NON-NLS-1$
 		}
 		return message;
 	}
 
 	/**
 	 * Returns a message for the exception wrapped in an invocation exception
 	 */
 	protected String getInvocationExceptionMessage(com.sun.jdi.InvocationException exception) {
 			InvocationException ie= (InvocationException) exception;
 			ObjectReference ref= ie.exception();
 			return MessageFormat.format(ActionMessages.getString("Evaluate.error.message.wrapped_exception"), new Object[] { ref.referenceType().name() }); //$NON-NLS-1$
 	}
 	
 	protected String getErrorMessage(IEvaluationResult result) {
 		String[] errors= result.getErrorMessages();
 		if (errors.length == 0) {
 			return getExceptionMessage(result.getException());
 		} else {
 			return getErrorMessage(errors);
 		}
 	}
 	
 	protected String getErrorMessage(String[] errors) {
 		String message= ""; //$NON-NLS-1$
 		for (int i= 0; i < errors.length; i++) {
 			String msg= errors[i];
 			if (i == 0) {
 				message= msg;
 			} else {
 				message= MessageFormat.format(ActionMessages.getString("Evaluate.error.problem_append_pattern"), new Object[] { message, msg }); //$NON-NLS-1$
 			}
 		}
 		return message;
 	}
 	
 	/**
 	 * @see IActionDelegate#run(IAction)
 	 */
 	public void run(IAction action) {
 		update();
 		run();
 	}
 
 	/**
 	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
 	 */
 	public void selectionChanged(IAction action, ISelection selection) { 
 		setAction(action);
 	}	
 
 	/**
 	 * @see IWorkbenchWindowActionDelegate#dispose()
 	 */
 	public void dispose() {
 		disposeDebugModelPresentation();
 		IWorkbenchWindow win = getWindow();
 		if (win != null) {
 			win.getPartService().removePartListener(this);
 		}
 	}
 
 	/**
 	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
 	 */
 	public void init(IWorkbenchWindow window) {
 		setWindow(window);
 		IWorkbenchPage page= window.getActivePage();
 		if (page != null) {
 			setTargetPart(page.getActivePart());
 		}
 		window.getPartService().addPartListener(this);
 		update();
 	}
 
 	protected IAction getAction() {
 		return fAction;
 	}
 
 	protected void setAction(IAction action) {
 		fAction = action;
 	}
 	
 	/**
 	 * Returns a debug model presentation (creating one
 	 * if necessary).
 	 * 
 	 * @return debug model presentation
 	 */
 	protected IDebugModelPresentation getDebugModelPresentation() {
 		if (fPresentation == null) {
 			fPresentation = DebugUITools.newDebugModelPresentation(JDIDebugModel.getPluginIdentifier());
 		}
 		return fPresentation;
 	}
 	
 	/** 
 	 * Disposes this action's debug model presentation, if
 	 * one was created.
 	 */
 	protected void disposeDebugModelPresentation() {
 		if (fPresentation != null) {
 			fPresentation.dispose();
 		}
 	}
 
 	/**
 	 * @see IEditorActionDelegate#setActiveEditor(IAction, IEditorPart)
 	 */
 	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
 		setAction(action);
 		setTargetPart(targetEditor);
 	}
 
 	/**
 	 * @see IPartListener#partActivated(IWorkbenchPart)
 	 */
 	public void partActivated(IWorkbenchPart part) {
 		setTargetPart(part);
 	}
 
 	/**
 	 * @see IPartListener#partBroughtToTop(IWorkbenchPart)
 	 */
 	public void partBroughtToTop(IWorkbenchPart part) {
 	}
 
 	/**
 	 * @see IPartListener#partClosed(IWorkbenchPart)
 	 */
 	public void partClosed(IWorkbenchPart part) {
 		if (part == getTargetPart()) {
 			setTargetPart(null);
 		}
 		if (part == getNewTargetPart()) {
 			setNewTargetPart(null);
 		}
 	}
 
 	/**
 	 * @see IPartListener#partDeactivated(IWorkbenchPart)
 	 */
 	public void partDeactivated(IWorkbenchPart part) {
 	}
 
 	/**
 	 * @see IPartListener#partOpened(IWorkbenchPart)
 	 */
 	public void partOpened(IWorkbenchPart part) {
 	}
 	
 	/**
 	 * @see IViewActionDelegate#init(IViewPart)
 	 */
 	public void init(IViewPart view) {
 		setTargetPart(view);
 	}
 
 	protected IWorkbenchPart getTargetPart() {
 		return fTargetPart;
 	}
 
 	protected void setTargetPart(IWorkbenchPart part) {
 		if (isEvaluating()) {
 			//do not want to change the target part while evaluating
 			//see bug 8334
 			setNewTargetPart(part);
 		} else {
 			if (getTargetPart() instanceof JavaSnippetEditor) {
 				((JavaSnippetEditor)getTargetPart()).removeSnippetStateChangedListener(this);
 			}
 			fTargetPart= part;
 			if (part instanceof JavaSnippetEditor) {
 				((JavaSnippetEditor)part).addSnippetStateChangedListener(this);
 			}
 		}
 	}
 
 	protected IWorkbenchWindow getWindow() {
 		return fWindow;
 	}
 
 	protected void setWindow(IWorkbenchWindow window) {
 		fWindow = window;
 	}
 	
 	/**
 	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
 	 */
 	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
 		setAction(action);
 		setTargetPart(targetPart);
 		update();
 	}
 	
 	protected Object getSelectedObject() {
 		return fSelection;
 	}
 	
 	protected void setSelectedObject(Object selection) {
 		fSelection = selection;
 	}
 	
 	/**
 	 * @see ISnippetStateChangedListener#snippetStateChanged(JavaSnippetEditor)
 	 */
 	public void snippetStateChanged(JavaSnippetEditor editor) {
 		if (editor != null && !editor.isEvaluating()) {
 			update();
 		} else {
 			getAction().setEnabled(false);
 		}
 	}
 
 	protected IWorkbenchPart getNewTargetPart() {
 		return fNewTargetPart;
 	}
 
 	protected void setNewTargetPart(IWorkbenchPart newTargetPart) {
 		fNewTargetPart = newTargetPart;
 	}
 	
 	protected boolean isEvaluating() {
 		return fEvaluating;
 	}
 
 	protected void setEvaluating(boolean evaluating) {
 		fEvaluating = evaluating;
 	}
 }
