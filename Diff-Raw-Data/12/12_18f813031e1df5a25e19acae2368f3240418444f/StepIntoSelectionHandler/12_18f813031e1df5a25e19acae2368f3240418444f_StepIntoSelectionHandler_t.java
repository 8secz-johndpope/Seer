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
 package org.eclipse.jdt.internal.debug.ui.actions;
 
 
 import java.text.MessageFormat;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.IDebugEventFilter;
 import org.eclipse.jdt.core.IMethod;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jdt.core.Signature;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
 import org.eclipse.jface.dialogs.ErrorDialog;
 
 /**
  * Handles stepping into a selected method, for a specific thread.
  */
 public class StepIntoSelectionHandler implements IDebugEventFilter {
 	
 	/**
 	 * The method to step into
 	 */
 	private IMethod fMethod;
 	
 	/**
 	 * Resolved signature of the method to step into
 	 */
 	private String fResolvedSignature;
 	
 	/**
 	 * The thread in which to step
 	 */
 	private IJavaThread fThread;
 
 	/**
 	 * The initial stack frame
 	 */
 	private String fOriginalName;
 	private String fOriginalSignature;
 	private String fOriginalTypeName;
 	private int fOriginalStackDepth;
 	
 	/**
 	 * Whether this is the first step into.
 	 */
 	private boolean fFirstStep = true;
 	
 	/**
 	 * The state of step filters before the step.
 	 */
 	private boolean fStepFilterEnabledState;
 	
 	/**
 	 * Empty event set.
 	 */
 	private static final DebugEvent[] fgEmptyEvents = new DebugEvent[0];
 
 	/**
 	 * Constructs a step handler to step into the given method in the given thread
 	 * starting from the given stack frame.
 	 */
 	public StepIntoSelectionHandler(IJavaThread thread, IJavaStackFrame frame, IMethod method) {
 		fMethod = method;
 		fThread = thread;
 		try {
 			fOriginalName = frame.getName();
 			fOriginalSignature = frame.getSignature();
 			fOriginalTypeName = frame.getDeclaringTypeName();
 			if (method.isBinary()) {
 				fResolvedSignature = method.getSignature();
 			} else {
 				fResolvedSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(method.getDeclaringType(), method.getSignature());
 			}
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e);
 		}
 	}
 	
 	/**
 	 * Returns the target thread for the step.
 	 * 
 	 * @return the target thread for the step
 	 */
 	protected IJavaThread getThread() {
 		return fThread;
 	}
 	
 	protected IJavaDebugTarget getDebugTarget() {
 		return (IJavaDebugTarget)getThread().getDebugTarget();
 	}
 	
 	/**
 	 * Returns the method to step into
 	 * 
 	 * @return the method to step into
 	 */
 	protected IMethod getMethod() {
 		return fMethod;
 	}
 	
 	/**
 	 * Returns the resolved signature of the method to step into
 	 * 
 	 * @return the resolved signature of the method to step into
 	 */
 	protected String getSignature() {
 		return fResolvedSignature;
 	}	
 	
 	/**
 	 * @see org.eclipse.debug.core.IDebugEventFilter#filterDebugEvents(org.eclipse.debug.core.DebugEvent)
 	 */
 	public DebugEvent[] filterDebugEvents(DebugEvent[] events) {
 		for (int i = 0; i < events.length; i++) {
 			DebugEvent event = events[i];
 			if (event.getSource() == getThread()) {
 				if (event.getKind() == DebugEvent.RESUME) {
 					if (event.isStepStart()) {
 						if (fFirstStep) {
 							fFirstStep = false;
 							return events;
 						} else {
 							// secondary step - filter the event
 							return fgEmptyEvents;
 						}
 					}
 				} else if (event.getKind() == DebugEvent.SUSPEND) {
 					// if not a step-end then abort (i.e. hit a breakpoint, etc)
 					if (event.getDetail() != DebugEvent.STEP_END) {
 						cleanup();
 						return events;
 					}
 					// if there is more than one suspend event, then abort:
 					//  -> this means that we have hit a suspend event at the same location
 					// that our step ended.
 					if (events.length > 1) {
						for (int j = i + 1; j < events.length; j++) {
 							DebugEvent debugEvent = events[j];
 							if (debugEvent.getKind() == DebugEvent.SUSPEND) {
 								cleanup();
								int filteredLength= events.length - j;
								DebugEvent[] filtered = new DebugEvent[filteredLength];
								System.arraycopy(events, j, filtered, 0, filteredLength);
								return filtered;
 							}
 						}
 					}
 					// compare location to desired location
 					try {
 						final IJavaStackFrame frame = (IJavaStackFrame)getThread().getTopStackFrame();
 						int stackDepth = frame.getThread().getStackFrames().length;
 						String name = null;
 						if (frame.isConstructor()) {
 							name = frame.getDeclaringTypeName();
 							int index = name.lastIndexOf('.');
 							if (index >= 0) {
 								name = name.substring(index + 1);
 							}
 						} else {
 							name = frame.getName();
 						}
 						if (name.equals(getMethod().getElementName()) && frame.getSignature().equals(getSignature())) {
 							// hit
 							cleanup();
 							return events;
 						} else {
 							// step again
 							Runnable r = null;
 							if (stackDepth > fOriginalStackDepth) {
 								r = new Runnable() {
 									public void run() {
 										try {
 											frame.stepReturn();
 										} catch (DebugException e) {
 											JDIDebugUIPlugin.log(e);
 											cleanup();
 											DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(getDebugTarget(), DebugEvent.CHANGE)});
 										}
 									}
 								};								
 							} else if (stackDepth == fOriginalStackDepth){
 								// we should be back in the original stack frame - if not, abort
 								if (!(frame.getSignature().equals(fOriginalSignature) && frame.getName().equals(fOriginalName) && frame.getDeclaringTypeName().equals(fOriginalTypeName))) {
 									missed();
 									return events;
 								}
 								r = new Runnable() {
 									public void run() {
 										try {
 											frame.stepInto();	
 										} catch (DebugException e) {
 											JDIDebugUIPlugin.log(e);
 											cleanup();
 											DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(getDebugTarget(), DebugEvent.CHANGE)});
 										}
 									}
 								};																
 							} else {
 								// we returned from the original frame - never hit the desired method
 								missed();
 								return events;								
 							}
 							DebugPlugin.getDefault().asyncExec(r);
 							// filter the events
 							return fgEmptyEvents;
 						}
 					} catch (CoreException e) {
 						// abort
 						JDIDebugUIPlugin.log(e);
 						cleanup();
 						return events;
 					}
 				} else {
 					// abort
 					cleanup();
 					return events;
 				}
 			} else if (event.getSource() == getThread().getDebugTarget()) {
 				// abort
 				cleanup();
 				return events;
 			}
 		}
 		return events;
 	}
 	
 	/** 
 	 * Called when stepping returned from the original frame without entering the desired method.
 	 */
 	protected void missed() {
 		cleanup();
 		Runnable r = new Runnable() {
 			public void run() {
 				String methodName = null;
 				try {
 					methodName = Signature.toString(getMethod().getSignature(), getMethod().getElementName(), getMethod().getParameterNames(), false, false);
 				} catch (JavaModelException e) {
 					methodName = getMethod().getElementName();
 				}
 				IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), 0, MessageFormat.format(ActionMessages.getString("StepIntoSelectionHandler.Execution_did_not_enter___{0}___before_the_current_method_returned._1"), new String[]{methodName}), null); //$NON-NLS-1$
 				ErrorDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), ActionMessages.getString("StepIntoSelectionHandler.Error_2"), null, status); //$NON-NLS-1$
 			}
 		};
 		JDIDebugUIPlugin.getStandardDisplay().asyncExec(r);		
 	}
 
 	/**
 	 * Performs the step.
 	 */
 	public void step() {
 		// add event filter and turn off step filters
 		DebugPlugin.getDefault().addDebugEventFilter(this);
 		fStepFilterEnabledState = getDebugTarget().isStepFiltersEnabled();
 		getDebugTarget().setStepFiltersEnabled(false);
 		try {
 			fOriginalStackDepth = getThread().getStackFrames().length;
 			getThread().stepInto();
 		} catch (DebugException e) {
 			JDIDebugUIPlugin.log(e);
 			cleanup();
 			DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(getDebugTarget(), DebugEvent.CHANGE)});			
 		}
 	}
 	
 	/**
 	 * Cleans up when the step is complete/aborted.
 	 */
 	protected void cleanup() {
 		DebugPlugin.getDefault().removeDebugEventFilter(this);
 		// restore step filter state
 		getDebugTarget().setStepFiltersEnabled(fStepFilterEnabledState);
 	}
 }
