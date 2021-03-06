 // Copyright (c) 2009 The Chromium Authors. All rights reserved.
 // Use of this source code is governed by a BSD-style license that can be
 // found in the LICENSE file.
 
 package org.chromium.debug.core.model;
 
 import java.io.IOException;
 import java.util.Collection;
 
 import org.chromium.debug.core.ChromiumDebugPlugin;
 import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
 import org.chromium.sdk.Breakpoint;
 import org.chromium.sdk.Browser;
 import org.chromium.sdk.BrowserTab;
 import org.chromium.sdk.DebugContext;
 import org.chromium.sdk.DebugEventListener;
 import org.chromium.sdk.ExceptionData;
 import org.chromium.sdk.JsStackFrame;
 import org.chromium.sdk.Script;
 import org.chromium.sdk.BrowserTab.BreakpointCallback;
 import org.chromium.sdk.BrowserTab.ScriptsCallback;
 import org.chromium.sdk.DebugContext.State;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarkerDelta;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.IBreakpointManager;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.model.IBreakpoint;
 import org.eclipse.debug.core.model.IDebugTarget;
 import org.eclipse.debug.core.model.IMemoryBlock;
 import org.eclipse.debug.core.model.IProcess;
 import org.eclipse.debug.core.model.IThread;
 
 /**
  * An IDebugTarget implementation for remote JavaScript debugging.
  * Can debug any target that supports the ChromeDevTools protocol.
  */
 public class DebugTargetImpl extends DebugElementImpl implements IDebugTarget, DebugEventListener {
 
   private static final IThread[] EMPTY_THREADS = new IThread[0];
 
   private static final long OPERATION_TIMEOUT_MS = 15000L;
 
   private final ILaunch launch;
 
   private final JavascriptThread[] threads;
 
   private final BrowserTab targetTab;
 
   private final ResourceManager resourceManager;
 
   private IProject debugProject;
 
   private DebugContext debugContext;
 
   private boolean isSuspended = false;
 
   private boolean isDisconnected = false;
 
   public DebugTargetImpl(ILaunch launch, Browser browser, TabSelector selector,
       String projectName, Runnable attachCallback, IProgressMonitor monitor) throws CoreException {
     super(null);
     monitor.beginTask("", 2); //$NON-NLS-1$
     try {
       BrowserTab[] tabs;
       try {
         tabs = browser.getTabs();
       } catch (IOException e) {
         monitor.done();
         throw newCoreException("Failed to get tabs for debugging", e); //$NON-NLS-1$
       } catch (IllegalStateException e) {
         monitor.done();
         throw newCoreException("Another Chromium JavaScript Debug Launch is in progress", e); //$NON-NLS-1$
       }
       this.launch = launch;
       this.threads = new JavascriptThread[] { new JavascriptThread(this) };
       this.targetTab = selector.selectTab(tabs);
       this.resourceManager = new ResourceManager();
       if (targetTab != null) {
         monitor.worked(1);
         if (targetTab.attach(this)) {
           onAttach(projectName, monitor, attachCallback);
           return;
         }
         monitor.done();
         // Could not attach. Log warning...
         ChromiumDebugPlugin.logWarning("Could not attach to a browser tab"); //$NON-NLS-1$
         // ... and fall through.
       }
       // Attachment cancelled
       setDisconnected(true);
       fireTerminateEvent();
     } catch (CoreException e) {
       monitor.done();
       throw e;
     } catch (RuntimeException e) {
       monitor.done();
       throw e;
     }
   }
 
   private void onAttach(String projectName, final IProgressMonitor monitor,
       final Runnable attachCallback) {
     this.debugProject = ChromiumDebugPluginUtil.createEmptyProject(projectName);
     DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
     try {
       reloadScriptsAndResume(attachCallback);
     } finally {
       monitor.done();
     }
   }
 
   private void reloadScriptsAndResume(final Runnable attachCallback) {
     reloadScripts(true, new Runnable() {
       public void run() {
         try {
           if (attachCallback != null) {
             attachCallback.run();
           }
         } finally {
           fireCreationEvent();
           resumed();
         }
       }
     });
   }
 
   private void reloadScripts(boolean isSync, final Runnable runnable) {
     Runnable command = new Runnable() {
       public void run() {
         targetTab.getScripts(new ScriptsCallback() {
           public void failure(String errorMessage) {
             ChromiumDebugPlugin.logError(errorMessage);
           }
 
           public void success(Collection<Script> scripts) {
             if (!targetTab.isAttached()) {
               return;
             }
             for (Script script : scripts) {
              if (script != null && !getResourceManager().scriptHasResource(script)) {
                IFile file = ChromiumDebugPluginUtil.createFile(debugProject, script.getName());
                getResourceManager().putScript(script, file);
                 if (script.hasSource()) {
                   try {
                    ChromiumDebugPluginUtil.writeFile(file, script.getSource());
                   } catch (CoreException e) {
                     ChromiumDebugPlugin.log(e);
                   }
                 }
               }
             }
             if (runnable != null) {
               runnable.run();
             }
           }
         });
       }
     };
     if (isSync) {
       command.run();
       return;
     }
     Thread t = new Thread(command);
     t.setDaemon(true);
     t.start();
     try {
       t.join(OPERATION_TIMEOUT_MS);
     } catch (InterruptedException e) {
       ChromiumDebugPlugin.log(e);
     }
   }
 
   public String getName() throws DebugException {
     return Messages.DebugTargetImpl_TargetName;
   }
 
   public IProcess getProcess() {
     return null;
   }
 
   public BrowserTab getTargetTab() {
     return targetTab;
   }
 
   public IThread[] getThreads() throws DebugException {
     return isDisconnected()
         ? EMPTY_THREADS
         : threads;
   }
 
   public boolean hasThreads() throws DebugException {
     return getThreads().length > 0;
   }
 
   public boolean supportsBreakpoint(IBreakpoint breakpoint) {
     return ChromiumDebugPlugin.DEBUG_MODEL_ID.equals(breakpoint.getModelIdentifier()) &&
         !isDisconnected();
   }
 
   @Override
   public DebugTargetImpl getDebugTarget() {
     return this;
   }
 
   @Override
   public ILaunch getLaunch() {
     return launch;
   }
 
   @Override
   public String getModelIdentifier() {
     return ChromiumDebugPlugin.DEBUG_MODEL_ID;
   }
 
   public boolean canTerminate() {
     return false;
   }
 
   public boolean isTerminated() {
     return false;
   }
 
   public void terminate() throws DebugException {
   }
 
   public boolean canResume() {
     return !isDisconnected() && isSuspended();
   }
 
   public boolean canSuspend() {
     // Immediate thread suspension is not supported by V8
     // (as it does not make sense.)
     return false;
   }
 
   public synchronized boolean isSuspended() {
     return isSuspended;
   }
 
   private synchronized void setSuspended(boolean isSuspended) {
     this.isSuspended = isSuspended;
   }
 
   public void suspended(int detail) {
     setSuspended(true);
     getThread().reset();
     fireSuspendEvent(detail);
   }
 
   public void resume() throws DebugException {
     debugContext.continueVm(null, 1, null);
     // Let's pretend Chromium does respond to the "continue" request immediately
     resumed(DebugEvent.CLIENT_REQUEST);
   }
 
   public void resumed(int detail) {
     fireResumeEvent(detail);
   }
 
   public void suspend() throws DebugException {
     // Immediate thread suspension is not supported by V8
     // (as it does not make sense.)
   }
 
   public boolean canDisconnect() {
     return !isDisconnected();
   }
 
   public void disconnect() throws DebugException {
     if (!canDisconnect()) {
       return;
     }
     removeAllBreakpoints();
     if (!targetTab.detach()) {
       ChromiumDebugPlugin.logWarning(Messages.DebugTargetImpl_BadResultWhileDisconnecting);
     }
     // This is a duplicated call to disconnected().
     // The primary one comes from V8DebuggerToolHandler#onDebuggerDetached
     // but we want to make sure the target becomes disconnected even if
     // there is a browser failure and it does not respond.
     disconnected();
   }
 
   public synchronized boolean isDisconnected() {
     return isDisconnected;
   }
 
   public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
     return null;
   }
 
   public boolean supportsStorageRetrieval() {
     return false;
   }
 
   public IProject getDebugProject() {
     return debugProject;
   }
 
   /**
    * Fires a debug event
    *
    * @param event to be fired
    */
   public void fireEvent(DebugEvent event) {
     DebugPlugin debugPlugin = DebugPlugin.getDefault();
     if (debugPlugin != null) {
       debugPlugin.fireDebugEventSet(new DebugEvent[] { event });
     }
   }
 
   public void fireEventForThread(int kind, int detail) {
     try {
       IThread[] threads = getThreads();
       if (threads.length > 0) {
         fireEvent(new DebugEvent(threads[0], kind, detail));
       }
     } catch (DebugException e) {
       // Actually, this is not thrown in out getThreads()
       return;
     }
     if (threads.length == 0) {
       return;
     }
   }
 
   public void fireCreationEvent() {
     setDisconnected(false);
     fireEventForThread(DebugEvent.CREATE, DebugEvent.UNSPECIFIED);
   }
 
   private synchronized void setDisconnected(boolean value) {
     isDisconnected = value;
   }
 
   public void fireResumeEvent(int detail) {
     setSuspended(false);
     fireEventForThread(DebugEvent.RESUME, detail);
     fireEvent(new DebugEvent(this, DebugEvent.RESUME, detail));
   }
 
   public void fireSuspendEvent(int detail) {
     setSuspended(true);
     fireEventForThread(DebugEvent.SUSPEND, detail);
     fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, detail));
   }
 
   public void fireTerminateEvent() {
     fireEventForThread(DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED);
     fireEvent(new DebugEvent(this, DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
     fireEvent(new DebugEvent(getLaunch(), DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED));
   }
 
   public void breakpointAdded(IBreakpoint breakpoint) {
     if (!supportsBreakpoint(breakpoint)) {
       return;
     }
     try {
       if (breakpoint.isEnabled()) {
         // Class cast is ensured by the supportsBreakpoint implementation
         final ChromiumLineBreakpoint lineBreakpoint = (ChromiumLineBreakpoint) breakpoint;
         Script script = getResourceManager().getScript(
             (IFile) breakpoint.getMarker().getResource());
         if (script == null) {
           // Might be a script from a different debug target
           return;
         }
         BreakpointCallback callback = new BreakpointCallback() {
           public void success(Breakpoint breakpoint) {
             lineBreakpoint.setBreakpoint(breakpoint);
           }
 
           public void failure(String errorMessage) {
             ChromiumDebugPlugin.logError(errorMessage);
           }
         };
         // ILineBreakpoint lines are 1-based while V8 lines are 0-based
         int line = (lineBreakpoint.getLineNumber() - 1) + script.getLineOffset();
         if (script.getName() != null) {
           getTargetTab().setBreakpoint(Breakpoint.Type.SCRIPT_NAME,
               script.getName(),
               line,
               Breakpoint.NO_VALUE,
               breakpoint.isEnabled(),
               lineBreakpoint.getCondition(),
               lineBreakpoint.getIgnoreCount(),
               callback);
         } else {
           getTargetTab().setBreakpoint(Breakpoint.Type.SCRIPT_ID,
               String.valueOf(script.getId()),
               line,
               Breakpoint.NO_VALUE,
               breakpoint.isEnabled(),
               lineBreakpoint.getCondition(),
               lineBreakpoint.getIgnoreCount(),
               callback);
         }
       }
     } catch (CoreException e) {
       ChromiumDebugPlugin.log(e);
     }
   }
 
   public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
     if (!supportsBreakpoint(breakpoint)) {
       return;
     }
     // Class cast is ensured by the supportsBreakpoint implementation
     ((ChromiumLineBreakpoint) breakpoint).changed();
   }
 
   public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
     if (!supportsBreakpoint(breakpoint)) {
       return;
     }
     try {
       if (breakpoint.isEnabled()) {
         // Class cast is ensured by the supportsBreakpoint implementation
         ((ChromiumLineBreakpoint) breakpoint).clear();
       }
     } catch (CoreException e) {
       ChromiumDebugPlugin.log(e);
     }
   }
 
   @SuppressWarnings("unchecked")
   @Override
   public Object getAdapter(Class adapter) {
     if (ILaunch.class.equals(adapter)) {
       return this.launch;
     }
     return super.getAdapter(adapter);
   }
 
   public ResourceManager getResourceManager() {
     return resourceManager;
   }
 
   public JavascriptThread getThread() {
     return isDisconnected()
         ? null
         : threads[0];
   }
 
   private static void breakpointsHit(Collection<Breakpoint> breakpointsHit) {
     if (breakpointsHit.isEmpty()) {
       return;
     }
     IBreakpoint[] breakpoints =
         DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
             ChromiumDebugPlugin.DEBUG_MODEL_ID);
     for (IBreakpoint breakpoint : breakpoints) {
       ChromiumLineBreakpoint jsBreakpoint = (ChromiumLineBreakpoint) breakpoint;
       if (breakpointsHit.contains(jsBreakpoint.getBrowserBreakpoint())) {
         jsBreakpoint.setIgnoreCount(-1); // reset ignore count as we've hit it
       }
     }
   }
 
   public void resumed() {
     resumed(DebugEvent.CLIENT_REQUEST);
   }
 
   public void suspended(DebugContext context) {
     this.debugContext = context;
     breakpointsHit(context.getBreakpointsHit());
     if (context.getState() == State.EXCEPTION) {
       ExceptionData exceptionData = context.getExceptionData();
       JsStackFrame topFrame = context.getStackFrames()[0];
       Script script = topFrame.getScript();
       ChromiumDebugPlugin.logError(
           Messages.DebugTargetImpl_LogExceptionFormat,
           exceptionData.isUncaught()
               ? Messages.DebugTargetImpl_Uncaught
               : Messages.DebugTargetImpl_Caught,
           exceptionData.getExceptionText(),
           script != null ? script.getName() : "<unknown>", //$NON-NLS-1$
           topFrame.getLineNumber(),
           trim(exceptionData.getSourceText(), 80));
       suspended(DebugEvent.BREAKPOINT);
       return;
     }
     final boolean hasBreakpointsHit = !context.getBreakpointsHit().isEmpty();
     reloadScripts(false, new Runnable() {
       public void run() {
         suspended(hasBreakpointsHit
             ? DebugEvent.BREAKPOINT
             : DebugEvent.STEP_END);
       }
     });
   }
 
   private static String trim(String text, int maxLength) {
     if (text == null || text.length() <= maxLength) {
       return text;
     }
     return text.substring(0, maxLength - 3) + "..."; //$NON-NLS-1$
   }
 
   public void disconnected() {
     if (!isDisconnected()) {
       setDisconnected(true);
       DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
       fireTerminateEvent();
     }
   }
 
   public DebugContext getDebugContext() {
     return debugContext;
   }
 
   public void navigated(String newUrl) {
     if (newUrl == null) {
       fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.STATE));
     } else {
       reloadScriptsAndResume(new Runnable(){
         public void run() {
           fireEvent(new DebugEvent(DebugTargetImpl.this, DebugEvent.CHANGE, DebugEvent.STATE));
         }
       });
     }
   }
 
   public void closed() {
     navigated(null);
   }
 
   private static CoreException newCoreException(String message, Throwable cause) {
     return new CoreException(
         new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, cause));
   }
 
   private void removeAllBreakpoints() {
     IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
     IBreakpoint[] breakpoints =
         breakpointManager.getBreakpoints(ChromiumDebugPlugin.DEBUG_MODEL_ID);
     for (IBreakpoint bp : breakpoints) {
       ChromiumLineBreakpoint clb = (ChromiumLineBreakpoint) bp;
       if (clb.getBrowserBreakpoint() != null &&
           clb.getBrowserBreakpoint().getId() != Breakpoint.INVALID_ID) {
         clb.getBrowserBreakpoint().clear(null);
       }
     }
     try {
       breakpointManager.removeBreakpoints(breakpoints, true);
     } catch (CoreException e) {
       ChromiumDebugPlugin.log(e);
     }
   }
 }
