 /*******************************************************************************
  * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Wind River Systems - initial API and implementation
  *******************************************************************************/
 package org.eclipse.tm.internal.tcf.debug.ui.model;
 
 import java.io.IOException;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.IExpressionManager;
 import org.eclipse.debug.core.IExpressionsListener;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILaunchConfiguration;
 import org.eclipse.debug.core.commands.IDisconnectHandler;
 import org.eclipse.debug.core.commands.IResumeHandler;
 import org.eclipse.debug.core.commands.IStepIntoHandler;
 import org.eclipse.debug.core.commands.IStepOverHandler;
 import org.eclipse.debug.core.commands.IStepReturnHandler;
 import org.eclipse.debug.core.commands.ISuspendHandler;
 import org.eclipse.debug.core.commands.ITerminateHandler;
 import org.eclipse.debug.core.model.IDebugModelProvider;
 import org.eclipse.debug.core.model.IExpression;
 import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
 import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
 import org.eclipse.debug.core.model.ISourceLocator;
 import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicyFactory;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputProvider;
 import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
 import org.eclipse.debug.ui.DebugUITools;
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.debug.ui.IDebugView;
 import org.eclipse.debug.ui.ISourcePresentation;
 import org.eclipse.debug.ui.contexts.ISuspendTrigger;
 import org.eclipse.debug.ui.contexts.ISuspendTriggerListener;
 import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditorInput;
 import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.StructuredViewer;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.BusyIndicator;
 import org.eclipse.swt.graphics.Device;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.MessageBox;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.tm.internal.tcf.debug.actions.TCFAction;
 import org.eclipse.tm.internal.tcf.debug.model.ITCFConstants;
 import org.eclipse.tm.internal.tcf.debug.model.TCFContextState;
 import org.eclipse.tm.internal.tcf.debug.model.TCFLaunch;
 import org.eclipse.tm.internal.tcf.debug.model.TCFSourceRef;
 import org.eclipse.tm.internal.tcf.debug.ui.Activator;
 import org.eclipse.tm.internal.tcf.debug.ui.ImageCache;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.BackIntoCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.BackOverCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.BackResumeCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.BackReturnCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.DisconnectCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.ResumeCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.StepIntoCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.StepOverCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.StepReturnCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.SuspendCommand;
 import org.eclipse.tm.internal.tcf.debug.ui.commands.TerminateCommand;
 import org.eclipse.tm.tcf.core.Command;
 import org.eclipse.tm.tcf.protocol.IChannel;
 import org.eclipse.tm.tcf.protocol.IErrorReport;
 import org.eclipse.tm.tcf.protocol.Protocol;
 import org.eclipse.tm.tcf.services.IDisassembly;
 import org.eclipse.tm.tcf.services.ILineNumbers;
 import org.eclipse.tm.tcf.services.IMemory;
 import org.eclipse.tm.tcf.services.IMemoryMap;
 import org.eclipse.tm.tcf.services.IProcesses;
 import org.eclipse.tm.tcf.services.IRegisters;
 import org.eclipse.tm.tcf.services.IRunControl;
 import org.eclipse.tm.tcf.services.ISymbols;
 import org.eclipse.tm.tcf.util.TCFDataCache;
 import org.eclipse.tm.tcf.util.TCFTask;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IPersistableElement;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.console.ConsolePlugin;
 import org.eclipse.ui.console.IConsole;
 import org.eclipse.ui.console.IConsoleConstants;
 import org.eclipse.ui.console.IConsoleManager;
 import org.eclipse.ui.console.IConsoleView;
 import org.eclipse.ui.console.IOConsole;
 import org.eclipse.ui.console.IOConsoleInputStream;
 import org.eclipse.ui.console.IOConsoleOutputStream;
 import org.eclipse.ui.texteditor.IDocumentProvider;
 import org.eclipse.ui.texteditor.ITextEditor;
 
 /**
  * TCFModel represents remote target state as it is known to host.
  * The main job of the model is caching remote data,
  * keeping the cache in a coherent state,
  * and feeding UI with up-to-date data.
  */
 public class TCFModel implements IElementContentProvider, IElementLabelProvider, IViewerInputProvider,
         IModelProxyFactory, IColumnPresentationFactory, ISourceDisplay, ISuspendTrigger {
 
     /**
      * A dummy editor input to open the disassembly view as editor.
      */
     public static class DisassemblyEditorInput implements IEditorInput {
         final static String EDITOR_ID = "org.eclipse.cdt.dsf.ui.disassembly";
         final static DisassemblyEditorInput INSTANCE = new DisassemblyEditorInput();
 
         @SuppressWarnings("rawtypes")
         public Object getAdapter(Class adapter) {
             return null;
         }
 
         public boolean exists() {
             return false;
         }
 
         public ImageDescriptor getImageDescriptor() {
             return null;
         }
 
         public String getName() {
             return "Disassembly";
         }
 
         public IPersistableElement getPersistable() {
             return null;
         }
 
         public String getToolTipText() {
             return "Disassembly";
         }
     }
 
     private final TCFLaunch launch;
     private final Display display;
     private final IExpressionManager expr_manager;
 
     private final List<ISuspendTriggerListener> suspend_trigger_listeners =
         new LinkedList<ISuspendTriggerListener>();
 
     private static int display_source_generation;
     private int suspend_trigger_generation;
 
     private final Map<String,String> action_results = new HashMap<String,String>();
     private final Map<IPresentationContext,Map<TCFNode,Integer>> action_deltas =
         new HashMap<IPresentationContext,Map<TCFNode,Integer>>();
     private final HashMap<String,TCFAction> active_actions = new HashMap<String,TCFAction>();
 
     private final Map<IPresentationContext,TCFModelProxy> model_proxies =
         new HashMap<IPresentationContext,TCFModelProxy>();
 
     private final Map<String,TCFNode> id2node = new HashMap<String,TCFNode>();
 
     private final Map<Class<?>,Object> adapters = new HashMap<Class<?>,Object>();
 
     private final Map<String,IMemoryBlockRetrievalExtension> mem_retrieval =
         new HashMap<String,IMemoryBlockRetrievalExtension>();
 
     private final Map<String,String> cast_to_type_map =
         new HashMap<String,String>();
 
     private class Console {
         final IOConsole console;
         final Map<Integer,IOConsoleOutputStream> out;
 
         Console(final IOConsole console) {
             this.console = console;
             out = new HashMap<Integer,IOConsoleOutputStream>();
             Thread t = new Thread() {
                 public void run() {
                     try {
                         IOConsoleInputStream inp = console.getInputStream();
                         final byte[] buf = new byte[0x100];
                         for (;;) {
                             int len = inp.read(buf);
                             if (len < 0) break;
                             // TODO: Eclipse Console view has a bad habit of replacing CR with CR/LF
                             if (len == 2 && buf[0] == '\r' && buf[1] == '\n') len = 1;
                             final int n = len;
                             Protocol.invokeAndWait(new Runnable() {
                                 public void run() {
                                     try {
                                         launch.writeProcessInputStream(buf, 0, n);
                                     }
                                     catch (Exception x) {
                                         onProcessStreamError(null, 0, x, 0);
                                     }
                                 }
                             });
                         }
                     }
                     catch (Throwable x) {
                         Activator.log("Cannot read console input", x);
                     }
                 }
             };
             t.setName("TCF Launch Console Input");
             t.start();
         }
 
         void close() {
             for (IOConsoleOutputStream stream : out.values()) {
                 try {
                     stream.close();
                 }
                 catch (IOException x) {
                     Activator.log("Cannot close console stream", x);
                 }
             }
             try {
                 console.getInputStream().close();
             }
             catch (IOException x) {
                 Activator.log("Cannot close console stream", x);
             }
         }
     }
 
     private Console console;
 
     private static final Map<ILaunchConfiguration,IEditorInput> editor_not_found =
         new HashMap<ILaunchConfiguration,IEditorInput>();
 
     private final IModelSelectionPolicyFactory model_selection_factory = new IModelSelectionPolicyFactory() {
 
         public IModelSelectionPolicy createModelSelectionPolicyAdapter(
                 Object element, IPresentationContext context) {
             return selection_policy;
         }
     };
 
     private final IModelSelectionPolicy selection_policy;
 
     private IChannel channel;
     private TCFNodeLaunch launch_node;
     private boolean disposed;
 
     private final IMemory.MemoryListener mem_listener = new IMemory.MemoryListener() {
 
         public void contextAdded(IMemory.MemoryContext[] contexts) {
             for (IMemory.MemoryContext ctx : contexts) {
                 String id = ctx.getParentID();
                 if (id == null) {
                     launch_node.onContextAdded(ctx);
                 }
                 else {
                     TCFNode node = getNode(id);
                     if (node instanceof TCFNodeExecContext) {
                         ((TCFNodeExecContext)node).onContextAdded(ctx);
                     }
                 }
             }
         }
 
         public void contextChanged(IMemory.MemoryContext[] contexts) {
             for (IMemory.MemoryContext ctx : contexts) {
                 TCFNode node = getNode(ctx.getID());
                 if (node instanceof TCFNodeExecContext) {
                     ((TCFNodeExecContext)node).onContextChanged(ctx);
                 }
             }
         }
 
         public void contextRemoved(final String[] context_ids) {
             onContextRemoved(context_ids);
         }
 
         public void memoryChanged(String context_id, Number[] addr, long[] size) {
             TCFNode node = getNode(context_id);
             if (node instanceof TCFNodeExecContext) {
                 ((TCFNodeExecContext)node).onMemoryChanged(addr, size);
             }
         }
     };
 
     private final IRunControl.RunControlListener run_listener = new IRunControl.RunControlListener() {
 
         public void containerResumed(String[] context_ids) {
             for (String id : context_ids) {
                 TCFNode node = getNode(id);
                 if (node instanceof TCFNodeExecContext) {
                     ((TCFNodeExecContext)node).onContainerResumed();
                 }
             }
         }
 
         public void containerSuspended(String context, String pc, String reason,
                 Map<String,Object> params, String[] suspended_ids) {
             int action_cnt = 0;
             for (String id : suspended_ids) {
                 TCFNode node = getNode(id);
                 action_results.remove(id);
                 if (active_actions.get(id) != null) {
                    setDebugViewSelection(node, reason);
                     action_cnt++;
                 }
                 if (node instanceof TCFNodeExecContext) {
                     ((TCFNodeExecContext)node).onContainerSuspended();
                 }
             }
             TCFNode node = getNode(context);
             if (node instanceof TCFNodeExecContext) {
                 ((TCFNodeExecContext)node).onContextSuspended(pc, reason, params);
             }
             launch_node.onAnyContextSuspendedOrChanged();
             if (action_cnt == 0) setDebugViewSelection(node, reason);
             action_results.remove(context);
         }
 
         public void contextAdded(IRunControl.RunControlContext[] contexts) {
             for (IRunControl.RunControlContext ctx : contexts) {
                 String id = ctx.getParentID();
                 if (id == null) {
                     launch_node.onContextAdded(ctx);
                 }
                 else {
                     TCFNode node = getNode(id);
                     if (node instanceof TCFNodeExecContext) {
                         ((TCFNodeExecContext)node).onContextAdded(ctx);
                     }
                 }
             }
         }
 
         public void contextChanged(IRunControl.RunControlContext[] contexts) {
             for (IRunControl.RunControlContext ctx : contexts) {
                 TCFNode node = getNode(ctx.getID());
                 if (node instanceof TCFNodeExecContext) {
                     ((TCFNodeExecContext)node).onContextChanged(ctx);
                 }
             }
             launch_node.onAnyContextSuspendedOrChanged();
         }
 
         public void contextException(String context, String msg) {
             TCFNode node = getNode(context);
             if (node instanceof TCFNodeExecContext) {
                 ((TCFNodeExecContext)node).onContextException(msg);
             }
         }
 
         public void contextRemoved(final String[] context_ids) {
             onContextRemoved(context_ids);
         }
 
         public void contextResumed(final String context) {
             TCFNode node = getNode(context);
             if (node instanceof TCFNodeExecContext) {
                 ((TCFNodeExecContext)node).onContextResumed();
             }
             display.asyncExec(new Runnable() {
                 public void run() {
                     Activator.getAnnotationManager().removeStackFrameAnnotation(TCFModel.this, context);
                 }
             });
         }
 
         public void contextSuspended(final String context, String pc, String reason, Map<String,Object> params) {
             TCFNode node = getNode(context);
             action_results.remove(context);
             if (node instanceof TCFNodeExecContext) {
                 TCFNodeExecContext exe = (TCFNodeExecContext)node;
                 exe.onContextSuspended(pc, reason, params);
             }
             launch_node.onAnyContextSuspendedOrChanged();
             setDebugViewSelection(node, reason);
         }
     };
 
     private final IMemoryMap.MemoryMapListener mmap_listenr = new IMemoryMap.MemoryMapListener() {
 
         public void changed(String context) {
             TCFNode node = getNode(context);
             if (node instanceof TCFNodeExecContext) {
                 TCFNodeExecContext exe = (TCFNodeExecContext)node;
                 exe.onMemoryMapChanged();
             }
             display.asyncExec(new Runnable() {
                 public void run() {
                     if (PlatformUI.isWorkbenchRunning()) {
                         for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                             IWorkbenchPage page = window.getActivePage();
                             if (page != null) displaySource(null, page, true);
                         }
                     }
                 }
             });
         }
     };
 
     private final IRegisters.RegistersListener reg_listener = new IRegisters.RegistersListener() {
 
         public void contextChanged() {
             for (TCFNode node : id2node.values()) {
                 if (node instanceof TCFNodeExecContext) {
                     ((TCFNodeExecContext)node).onRegistersChanged();
                 }
             }
         }
 
         public void registerChanged(String context) {
             TCFNode node = getNode(context);
             if (node instanceof TCFNodeRegister) {
                 ((TCFNodeRegister)node).onValueChanged();
             }
         }
     };
 
     private final IProcesses.ProcessesListener prs_listener = new IProcesses.ProcessesListener() {
 
         public void exited(String process_id, int exit_code) {
             IProcesses.ProcessContext prs = launch.getProcessContext();
             if (prs != null && process_id.equals(prs.getID())) onLastContextRemoved();
         }
     };
 
     private final IExpressionsListener expressions_listener = new IExpressionsListener() {
 
         int generation;
 
         public void expressionsAdded(IExpression[] expressions) {
             expressionsRemoved(expressions);
         }
 
         public void expressionsChanged(IExpression[] expressions) {
             expressionsRemoved(expressions);
         }
 
         public void expressionsRemoved(IExpression[] expressions) {
             final int g = ++generation;
             Protocol.invokeLater(new Runnable() {
                 public void run() {
                     if (g != generation) return;
                     for (TCFNode n : id2node.values()) {
                         if (n instanceof TCFNodeExecContext) {
                             ((TCFNodeExecContext)n).onExpressionAddedOrRemoved();
                         }
                     }
                     for (TCFModelProxy p : model_proxies.values()) {
                         String id = p.getPresentationContext().getId();
                         if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(id)) {
                             Object o = p.getInput();
                             if (o instanceof TCFNode) {
                                 TCFNode n = (TCFNode)o;
                                 if (n.model == TCFModel.this) p.addDelta(n, IModelDelta.CONTENT);
                             }
                         }
                     }
                 }
             });
         }
     };
 
     private final TCFLaunch.ActionsListener actions_listener = new TCFLaunch.ActionsListener() {
 
         public void onContextActionStart(TCFAction action) {
             active_actions.put(action.getContextID(), action);
         }
 
         public void onContextActionResult(String id, String reason) {
             if (reason == null) action_results.remove(id);
             else action_results.put(id, reason);
             TCFNode node = id2node.get(id);
             if (node != null) {
                 for (IPresentationContext ctx : model_proxies.keySet()) {
                     addActionsDoneDelta(node, ctx, IModelDelta.STATE);
                 }
             }
         }
 
         public void onContextActionDone(TCFAction action) {
             active_actions.remove(action.getContextID());
             for (IPresentationContext ctx : action_deltas.keySet()) {
                 Map<TCFNode,Integer> deltas = action_deltas.get(ctx);
                 TCFModelProxy proxy = model_proxies.get(ctx);
                 if (proxy == null) continue;
                 for (TCFNode node : deltas.keySet()) {
                     proxy.addDelta(node, deltas.get(node));
                 }
             }
             action_deltas.clear();
             for (TCFModelProxy p : model_proxies.values()) p.post();
         }
     };
 
     private final IDebugModelProvider debug_model_provider = new IDebugModelProvider() {
         public String[] getModelIdentifiers() {
             return new String[] { ITCFConstants.ID_TCF_DEBUG_MODEL };
         }
     };
 
     private volatile boolean instruction_stepping_enabled;
 
     TCFModel(TCFLaunch launch) {
         this.launch = launch;
         display = PlatformUI.getWorkbench().getDisplay();
         selection_policy = new TCFModelSelectionPolicy(this);
         adapters.put(ILaunch.class, launch);
         adapters.put(IModelSelectionPolicy.class, selection_policy);
         adapters.put(IModelSelectionPolicyFactory.class, model_selection_factory);
         adapters.put(IDebugModelProvider.class, debug_model_provider);
         adapters.put(ISuspendHandler.class, new SuspendCommand(this));
         adapters.put(IResumeHandler.class, new ResumeCommand(this));
         adapters.put(BackResumeCommand.class, new BackResumeCommand(this));
         adapters.put(ITerminateHandler.class, new TerminateCommand(this));
         adapters.put(IDisconnectHandler.class, new DisconnectCommand(this));
         adapters.put(IStepIntoHandler.class, new StepIntoCommand(this));
         adapters.put(IStepOverHandler.class, new StepOverCommand(this));
         adapters.put(IStepReturnHandler.class, new StepReturnCommand(this));
         adapters.put(BackIntoCommand.class, new BackIntoCommand(this));
         adapters.put(BackOverCommand.class, new BackOverCommand(this));
         adapters.put(BackReturnCommand.class, new BackReturnCommand(this));
         expr_manager = DebugPlugin.getDefault().getExpressionManager();
         expr_manager.addExpressionListener(expressions_listener);
         launch.addActionsListener(actions_listener);
     }
 
     /**
      * Add an adapter for given type.
      *
      * @param adapterType  the type the adapter implements
      * @param adapter  the adapter implementing <code>adapterType</code>
      */
     public void setAdapter(Class<?> adapterType, Object adapter) {
         synchronized (adapters) {
             assert adapterType.isInstance(adapter);
             adapters.put(adapterType, adapter);
         }
     }
 
     @SuppressWarnings("rawtypes")
     public Object getAdapter(final Class adapter, final TCFNode node) {
         synchronized (adapters) {
             Object o = adapters.get(adapter);
             if (o != null) return o;
         }
         if (adapter == IMemoryBlockRetrieval.class || adapter == IMemoryBlockRetrievalExtension.class) {
             return new TCFTask<Object>() {
                 public void run() {
                     Object o = null;
                     TCFDataCache<TCFNodeExecContext> cache = searchMemoryContext(node);
                     if (cache != null) {
                         if (!cache.validate(this)) return;
                         if (cache.getData() != null) {
                             TCFNodeExecContext ctx = cache.getData();
                             o = mem_retrieval.get(ctx.getID());
                             if (o == null) {
                                 TCFMemoryBlockRetrieval m = new TCFMemoryBlockRetrieval(ctx);
                                 mem_retrieval.put(ctx.getID(), m);
                                 o = m;
                             }
                         }
                     }
                     assert o == null || adapter.isInstance(o);
                     done(o);
                 }
             }.getE();
         }
         return null;
     }
 
     void onConnected() {
         assert Protocol.isDispatchThread();
         assert launch_node == null;
         channel = launch.getChannel();
         launch_node = new TCFNodeLaunch(this);
         IMemory mem = launch.getService(IMemory.class);
         if (mem != null) mem.addListener(mem_listener);
         IRunControl run = launch.getService(IRunControl.class);
         if (run != null) run.addListener(run_listener);
         IMemoryMap mmap = launch.getService(IMemoryMap.class);
         if (mmap != null) mmap.addListener(mmap_listenr);
         IRegisters reg = launch.getService(IRegisters.class);
         if (reg != null) reg.addListener(reg_listener);
         IProcesses prs = launch.getService(IProcesses.class);
         if (prs != null) prs.addListener(prs_listener);
         launchChanged();
     }
 
     void onDisconnected() {
         assert Protocol.isDispatchThread();
         if (launch_node != null) {
             launch_node.dispose();
             launch_node = null;
         }
         refreshLaunchView();
         assert id2node.size() == 0;
     }
 
     void onProcessOutput(String process_id, final int stream_id, byte[] data) {
         try {
             IProcesses.ProcessContext prs = launch.getProcessContext();
             if (prs == null || !process_id.equals(prs.getID())) return;
             if (console == null) {
                 final IOConsole c = new IOConsole("TCF " + process_id, null,
                         ImageCache.getImageDescriptor(ImageCache.IMG_TCF), "UTF-8", true);
                 display.asyncExec(new Runnable() {
                     public void run() {
                         try {
                             IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
                             manager.addConsoles(new IConsole[]{ c });
                             IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                             if (w == null) return;
                             IWorkbenchPage page = w.getActivePage();
                             if (page == null) return;
                             IConsoleView view = (IConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
                             view.display(c);
                         }
                         catch (Throwable x) {
                             Activator.log("Cannot open console view", x);
                         }
                     }
                 });
                 console = new Console(c);
             }
             IOConsoleOutputStream stream = console.out.get(stream_id);
             if (stream == null) {
                 final IOConsoleOutputStream s = stream = console.console.newOutputStream();
                 display.asyncExec(new Runnable() {
                     public void run() {
                         try {
                             int color_id = SWT.COLOR_BLACK;
                             switch (stream_id) {
                             case 1: color_id = SWT.COLOR_RED; break;
                             case 2: color_id = SWT.COLOR_BLUE; break;
                             case 3: color_id = SWT.COLOR_GREEN; break;
                             }
                             s.setColor(display.getSystemColor(color_id));
                         }
                         catch (Throwable x) {
                             Activator.log("Cannot open console view", x);
                         }
                     }
                 });
                 console.out.put(stream_id, stream);
             }
             stream.write(data, 0, data.length);
         }
         catch (Throwable x) {
             Activator.log("Cannot write to console", x);
         }
     }
 
     void onProcessStreamError(String process_id, int stream_id, Exception x, int lost_size) {
         if (channel != null && channel.getState() == IChannel.STATE_CLOSED) return;
         StringBuffer bf = new StringBuffer();
         bf.append("Debugger console IO error");
         if (process_id != null) {
             bf.append(". Process ID ");
             bf.append(process_id);
         }
         bf.append(". Stream ");
         bf.append(stream_id);
         if (lost_size > 0) {
             bf.append(". Lost data size ");
             bf.append(lost_size);
         }
         Activator.log(bf.toString(), x);
     }
 
     TCFAction getActiveAction(String id) {
         return active_actions.get(id);
     }
 
     void addActionsDoneDelta(TCFNode node, IPresentationContext ctx, int flags) {
         Map<TCFNode,Integer> deltas = action_deltas.get(ctx);
         if (deltas == null) action_deltas.put(ctx, deltas = new HashMap<TCFNode,Integer>());
         Integer delta = deltas.get(node);
         if (delta != null) {
             deltas.put(node, delta.intValue() | flags);
         }
         else {
             deltas.put(node, flags);
         }
     }
 
     String getContextActionResult(String id) {
         return action_results.get(id);
     }
 
     void onProxyInstalled(TCFModelProxy mp) {
         model_proxies.put(mp.getPresentationContext(), mp);
     }
 
     void onProxyDisposed(TCFModelProxy mp) {
         IPresentationContext ctx = mp.getPresentationContext();
         assert model_proxies.get(ctx) == mp;
         model_proxies.remove(ctx);
     }
 
     private void onContextRemoved(final String[] context_ids) {
         boolean close_channel = false;
         for (String id : context_ids) {
             TCFNode node = getNode(id);
             if (node instanceof TCFNodeExecContext) {
                 ((TCFNodeExecContext)node).onContextRemoved();
                 if (node.parent == launch_node) close_channel = true;
             }
             action_results.remove(id);
         }
         if (close_channel) {
             // Close debug session if the last context is removed:
             onLastContextRemoved();
         }
         display.asyncExec(new Runnable() {
             public void run() {
                 for (String id : context_ids) {
                     Activator.getAnnotationManager().removeStackFrameAnnotation(TCFModel.this, id);
                 }
             }
         });
     }
 
     private void onLastContextRemoved() {
         Protocol.invokeLater(1000, new Runnable() {
             public void run() {
                 if (launch_node == null) return;
                 if (launch_node.isDisposed()) return;
                 TCFChildrenExecContext children = launch_node.getChildren();
                 if (!children.validate(this)) return;
                 if (children.size() != 0) return;
                 launch.onLastContextRemoved();
             }
         });
     }
 
     void launchChanged() {
         if (launch_node != null) {
             for (TCFModelProxy p : model_proxies.values()) {
                 String id = p.getPresentationContext().getId();
                 if (IDebugUIConstants.ID_DEBUG_VIEW.equals(id)) {
                     p.addDelta(launch_node, IModelDelta.STATE | IModelDelta.CONTENT);
                 }
             }
         }
         else {
             refreshLaunchView();
         }
     }
 
     Collection<TCFModelProxy> getModelProxies() {
         return model_proxies.values();
     }
 
     void dispose() {
         launch.removeActionsListener(actions_listener);
         expr_manager.removeExpressionListener(expressions_listener);
         if (console != null) {
             display.asyncExec(new Runnable() {
                 public void run() {
                     console.close();
                     IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
                     manager.removeConsoles(new IOConsole[]{ console.console });
                 }
             });
         }
     }
 
     void addNode(String id, TCFNode node) {
         assert id != null;
         assert Protocol.isDispatchThread();
         assert id2node.get(id) == null;
         assert !node.isDisposed();
         id2node.put(id, node);
     }
 
     void removeNode(String id) {
         assert id != null;
         assert Protocol.isDispatchThread();
         id2node.remove(id);
         mem_retrieval.remove(id);
     }
 
     IExpressionManager getExpressionManager() {
         return expr_manager;
     }
 
     public Display getDisplay() {
         return display;
     }
 
     public TCFLaunch getLaunch() {
         return launch;
     }
 
     public IChannel getChannel() {
         return channel;
     }
 
     public TCFNodeLaunch getRootNode() {
         return launch_node;
     }
 
     public TCFNode getNode(String id) {
         if (id == null) return null;
         if (id.equals("")) return launch_node;
         assert Protocol.isDispatchThread();
         return id2node.get(id);
     }
 
     public String getCastToType(String id) {
         return cast_to_type_map.get(id);
     }
 
     public void setCastToType(String id, String type) {
         if (type != null && type.trim().length() == 0) type = null;
         if (type == null) cast_to_type_map.remove(id);
         else cast_to_type_map.put(id, type);
         TCFNode node = id2node.get(id);
         if (node instanceof ICastToType) {
             ((ICastToType)node).onCastToTypeChanged();
         }
     }
 
     public TCFDataCache<ISymbols.Symbol> getSymbolInfoCache(final String sym_id) {
         if (sym_id == null) return null;
         TCFNodeSymbol n = (TCFNodeSymbol)getNode(sym_id);
         if (n == null) n = new TCFNodeSymbol(launch_node, sym_id);
         return n.getContext();
     }
 
     public TCFDataCache<String[]> getSymbolChildrenCache(final String sym_id) {
         if (sym_id == null) return null;
         TCFNodeSymbol n = (TCFNodeSymbol)getNode(sym_id);
         if (n == null) n = new TCFNodeSymbol(launch_node, sym_id);
         return n.getChildren();
     }
 
     /**
      * Search memory context that owns the object represented by given node.
      * @return data cache item that holds the memory context node.
      */
     public TCFDataCache<TCFNodeExecContext> searchMemoryContext(final TCFNode node) {
         TCFNode n = node;
         while (n != null && !n.disposed) {
             if (n instanceof TCFNodeExecContext) return ((TCFNodeExecContext)n).getMemoryNode();
             n = n.parent;
         }
         return null;
     }
 
     public void update(IChildrenCountUpdate[] updates) {
         for (int i = 0; i < updates.length; i++) {
             Object o = updates[i].getElement();
             if (o instanceof TCFLaunch) {
                 if (launch_node != null) {
                     launch_node.update(updates[i]);
                 }
                 else {
                     updates[i].setChildCount(0);
                     updates[i].done();
                 }
             }
             else {
                 ((TCFNode)o).update(updates[i]);
             }
         }
     }
 
     public void update(IChildrenUpdate[] updates) {
         for (int i = 0; i < updates.length; i++) {
             Object o = updates[i].getElement();
             if (o instanceof TCFLaunch) {
                 if (launch_node != null) {
                     launch_node.update(updates[i]);
                 }
                 else {
                     updates[i].done();
                 }
             }
             else {
                 ((TCFNode)o).update(updates[i]);
             }
         }
     }
 
     public void update(IHasChildrenUpdate[] updates) {
         for (int i = 0; i < updates.length; i++) {
             Object o = updates[i].getElement();
             if (o instanceof TCFLaunch) {
                 if (launch_node != null) {
                     launch_node.update(updates[i]);
                 }
                 else {
                     updates[i].setHasChilren(false);
                     updates[i].done();
                 }
             }
             else {
                 ((TCFNode)o).update(updates[i]);
             }
         }
     }
 
     public void update(ILabelUpdate[] updates) {
         for (int i = 0; i < updates.length; i++) {
             Object o = updates[i].getElement();
             // Launch label is provided by TCFLaunchLabelProvider class.
             assert !(o instanceof TCFLaunch);
             ((TCFNode)o).update(updates[i]);
         }
     }
 
     public void update(IViewerInputUpdate update) {
         if (IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(update.getPresentationContext().getId())) {
             // Current implementation does not support flexible hierarchy for breakpoints
             IViewerInputProvider p = (IViewerInputProvider)launch.getAdapter(IViewerInputProvider.class);
             if (p != null) {
                 p.update(update);
                 return;
             }
         }
         Object o = update.getElement();
         if (o instanceof TCFLaunch) {
             update.setInputElement(o);
             update.done();
         }
         else {
             ((TCFNode)o).update(update);
         }
     }
 
     public IModelProxy createModelProxy(Object element, IPresentationContext context) {
         return new TCFModelProxy(this);
     }
 
     public IColumnPresentation createColumnPresentation(IPresentationContext context, Object element) {
         String id = getColumnPresentationId(context, element);
         if (id == null) return null;
         if (id.equals(TCFColumnPresentationRegister.PRESENTATION_ID)) return new TCFColumnPresentationRegister();
         if (id.equals(TCFColumnPresentationExpression.PRESENTATION_ID)) return new TCFColumnPresentationExpression();
         return null;
     }
 
     public String getColumnPresentationId(IPresentationContext context, Object element) {
         if (IDebugUIConstants.ID_REGISTER_VIEW.equals(context.getId())) {
             return TCFColumnPresentationRegister.PRESENTATION_ID;
         }
         if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(context.getId())) {
             return TCFColumnPresentationExpression.PRESENTATION_ID;
         }
         if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(context.getId())) {
             return TCFColumnPresentationExpression.PRESENTATION_ID;
         }
         return null;
     }
 
     public void setDebugViewSelection(TCFNode node, String reason) {
         assert Protocol.isDispatchThread();
         if (node == null) return;
         if (node.disposed) return;
         runSuspendTrigger(node);
         if (reason == null) return;
         if (reason.equals(IRunControl.REASON_USER_REQUEST)) return;
         for (TCFModelProxy proxy : model_proxies.values()) {
             if (proxy.getPresentationContext().getId().equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
                 proxy.setSelection(node);
                 if (reason.equals(IRunControl.REASON_STEP)) continue;
                 if (reason.equals(IRunControl.REASON_CONTAINER)) continue;
                 proxy.expand(node);
             }
         }
     }
 
     /**
      * Reveal source code associated with given model element.
      * The method is part of ISourceDisplay interface.
      * The method is normally called from SourceLookupService.
      */
     public void displaySource(Object model_element, final IWorkbenchPage page, boolean forceSourceLookup) {
         final int cnt = ++display_source_generation;
         /* Because of racing in Eclipse Debug infrastructure, 'model_element' value can be invalid.
          * As a workaround, get current debug view selection.
          */
         if (page != null) {
             ISelection context = DebugUITools.getDebugContextManager().getContextService(page.getWorkbenchWindow()).getActiveContext();
             if (context instanceof IStructuredSelection) {
                 IStructuredSelection selection = (IStructuredSelection)context;
                 model_element = selection.isEmpty() ? null : selection.getFirstElement();
             }
         }
         final Object element = model_element;
         Protocol.invokeLater(new Runnable() {
             public void run() {
                 if (cnt != display_source_generation) return;
                 TCFNodeExecContext exec_ctx = null;
                 TCFNodeStackFrame stack_frame = null;
                 if (!disposed && channel.getState() == IChannel.STATE_OPEN) {
                     // TODO: to reduce flicker, delay displaySource() requests for a context that has active run control action
                     if (element instanceof TCFNodeExecContext) {
                         exec_ctx = (TCFNodeExecContext)element;
                         if (!exec_ctx.disposed) {
                             TCFDataCache<TCFContextState> state_cache = exec_ctx.getState();
                             if (!state_cache.validate(this)) return;
                             TCFContextState state_data = state_cache.getData();
                             if (state_data != null && state_data.is_suspended) {
                                 TCFChildrenStackTrace stack_trace = exec_ctx.getStackTrace();
                                 if (!stack_trace.validate(this)) return;
                                 stack_frame = stack_trace.getTopFrame();
                             }
                         }
                     }
                     else if (element instanceof TCFNodeStackFrame) {
                         TCFNodeStackFrame f = (TCFNodeStackFrame)element;
                         exec_ctx = (TCFNodeExecContext)f.parent;
                         if (!f.disposed && !exec_ctx.disposed) {
                             TCFDataCache<TCFContextState> state_cache = exec_ctx.getState();
                             if (!state_cache.validate(this)) return;
                             TCFContextState state_data = state_cache.getData();
                             if (state_data != null && state_data.is_suspended) stack_frame = f;
                         }
                     }
                 }
                 String ctx_id = null;
                 boolean top_frame = false;
                 ILineNumbers.CodeArea area = null;
                 if (stack_frame != null) {
                     TCFDataCache<TCFSourceRef> line_info = stack_frame.getLineInfo();
                     if (!line_info.validate(this)) return;
                     Throwable error = line_info.getError();
                     TCFSourceRef src_ref = line_info.getData();
                     if (error == null && src_ref != null) error = src_ref.error;
                     if (error != null) Activator.log("Error retrieving source mapping for a stack frame", error);
                     if (src_ref != null) area = src_ref.area;
                     top_frame = stack_frame.getFrameNo() == 0;
                     ctx_id = stack_frame.parent.id;
                 }
                 displaySource(cnt, page, ctx_id, top_frame, area);
             }
         });
     }
 
     private void displaySource(final int cnt, final IWorkbenchPage page,
             final String exe_id, final boolean top_frame, final ILineNumbers.CodeArea area) {
         final boolean disassembly_available = channel.getRemoteService(IDisassembly.class) != null;
         display.asyncExec(new Runnable() {
             public void run() {
                 if (cnt != display_source_generation) return;
                 String editor_id = null;
                 IEditorInput editor_input = null;
                 int line = 0;
                 if (area != null) {
                     ISourceLocator locator = getLaunch().getSourceLocator();
                     Object source_element = null;
                     if (locator instanceof ISourceLookupDirector) {
                         source_element = ((ISourceLookupDirector)locator).getSourceElement(area);
                     }
                     if (source_element != null) {
                         ISourcePresentation presentation = TCFModelPresentation.getDefault();
                         if (presentation != null) {
                             editor_input = presentation.getEditorInput(source_element);
                         }
                         if (editor_input != null) {
                             editor_id = presentation.getEditorId(editor_input, source_element);
                         }
                         line = area.start_line;
                     }
                 }
                 if (exe_id != null && disassembly_available &&
                         (editor_input == null || editor_id == null || instruction_stepping_enabled) &&
                         PlatformUI.getWorkbench().getEditorRegistry().findEditor(
                                 DisassemblyEditorInput.EDITOR_ID) != null) {
                     editor_id = DisassemblyEditorInput.EDITOR_ID;
                     editor_input = DisassemblyEditorInput.INSTANCE;
                 }
                 if (area != null && (editor_input == null || editor_id == null)) {
                     ILaunchConfiguration cfg = launch.getLaunchConfiguration();
                     editor_id = IDebugUIConstants.ID_COMMON_SOURCE_NOT_FOUND_EDITOR;
                     editor_input = editor_not_found.get(cfg);
                     if (editor_input == null) {
                         editor_input = new CommonSourceNotFoundEditorInput(cfg);
                         editor_not_found.put(cfg, editor_input);
                     }
                 }
                 if (cnt != display_source_generation) return;
                 ITextEditor text_editor = null;
                 if (page != null && editor_input != null && editor_id != null) {
                     IEditorPart editor = openEditor(editor_input, editor_id, page);
                     if (editor instanceof ITextEditor) {
                         text_editor = (ITextEditor)editor;
                     }
                     else {
                         text_editor = (ITextEditor)editor.getAdapter(ITextEditor.class);
                     }
                 }
                 IRegion region = null;
                 if (text_editor != null) {
                     region = getLineInformation(text_editor, line);
                     if (region != null) text_editor.selectAndReveal(region.getOffset(), 0);
                 }
                 Activator.getAnnotationManager().addStackFrameAnnotation(TCFModel.this,
                         exe_id, top_frame, page, text_editor, region);
             }
         });
     }
 
     /*
      * Refresh Launch View.
      * Normally the view is updated by sending deltas through model proxy.
      * This method is used only when launch is not yet connected or already disconnected.
      */
     private void refreshLaunchView() {
         // TODO: there should be a better way to refresh Launch View
         final Throwable error = launch.getError();
         if (error != null) launch.setError(null);
         synchronized (Device.class) {
             if (display.isDisposed()) return;
             display.asyncExec(new Runnable() {
                 public void run() {
                     IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
                     if (windows == null) return;
                     for (IWorkbenchWindow window : windows) {
                         IDebugView view = (IDebugView)window.getActivePage().findView(IDebugUIConstants.ID_DEBUG_VIEW);
                         if (view != null) ((StructuredViewer)view.getViewer()).refresh(launch);
                     }
                 }
             });
             if (error != null) showMessageBox("TCF Launch Error", error);
         }
     }
 
     /**
      * Show error message box in active workbench window.
      * @param title - message box title.
      * @param error - error to be shown.
      */
     public void showMessageBox(final String title, final Throwable error) {
         display.asyncExec(new Runnable() {
             public void run() {
                 Shell shell = display.getActiveShell();
                 if (shell == null) {
                     Shell[] shells = display.getShells();
                     HashSet<Shell> set = new HashSet<Shell>();
                     for (Shell s : shells) set.add(s);
                     for (Shell s : shells) {
                         if (s.getParent() != null) set.remove(s.getParent().getShell());
                     }
                     for (Shell s : shells) shell = s;
                 }
                 MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                 mb.setText(title);
                 mb.setMessage(getErrorMessage(error, true));
                 mb.open();
             }
         });
     }
 
     /**
      * Create human readable error message from a Throwable object.
      * @param error - a Throwable object.
      * @param multiline - true if multi-line text is allowed.
      * @return
      */
     public static String getErrorMessage(Throwable error, boolean multiline) {
         StringBuffer buf = new StringBuffer();
         while (error != null) {
             String msg = null;
             if (!multiline && error instanceof IErrorReport) {
                 msg = Command.toErrorString(((IErrorReport)error).getAttributes());
             }
             else {
                 msg = error.getLocalizedMessage();
             }
             if (msg == null || msg.length() == 0) msg = error.getClass().getName();
             buf.append(msg);
             error = error.getCause();
             if (error != null) {
                 char ch = buf.charAt(buf.length() - 1);
                 if (multiline && ch != '\n') {
                     buf.append('\n');
                 }
                 else if (ch != '.' && ch != ';') {
                     buf.append(';');
                 }
                 buf.append("Caused by:");
                 buf.append(multiline ? '\n' : ' ');
             }
         }
         if (buf.length() > 0) {
             char ch = buf.charAt(buf.length() - 1);
             if (multiline && ch != '\n') {
                 buf.append('\n');
             }
         }
         return buf.toString();
     }
 
     /*
      * Open an editor for given editor input.
      * @param input - IEditorInput representing a source file to be shown in the editor
      * @param id - editor type ID
      * @param page - workbench page that will contain the editor
      * @return - IEditorPart if the editor was opened successfully, or null otherwise.
      */
     private IEditorPart openEditor(final IEditorInput input, final String id, final IWorkbenchPage page) {
         final IEditorPart[] editor = new IEditorPart[]{ null };
         Runnable r = new Runnable() {
             public void run() {
                 if (!page.getWorkbenchWindow().getWorkbench().isClosing()) {
                     try {
                         editor[0] = page.openEditor(input, id, false, IWorkbenchPage.MATCH_ID|IWorkbenchPage.MATCH_INPUT);
                     }
                     catch (PartInitException e) {
                         Activator.log("Cannot open editor", e);
                     }
                 }
             }
         };
         BusyIndicator.showWhile(display, r);
         return editor[0];
     }
 
     /*
      * Returns the line information for the given line in the given editor
      */
     private IRegion getLineInformation(ITextEditor editor, int lineNumber) {
         IDocumentProvider provider = editor.getDocumentProvider();
         IEditorInput input = editor.getEditorInput();
         try {
             provider.connect(input);
         }
         catch (CoreException e) {
             return null;
         }
         try {
             IDocument document = provider.getDocument(input);
             if (document != null)
                 return document.getLineInformation(lineNumber - 1);
         }
         catch (BadLocationException e) {
         }
         finally {
             provider.disconnect(input);
         }
         return null;
     }
 
     public synchronized void addSuspendTriggerListener(ISuspendTriggerListener listener) {
         suspend_trigger_listeners.add(listener);
     }
 
     public synchronized void removeSuspendTriggerListener(ISuspendTriggerListener listener) {
         suspend_trigger_listeners.remove(listener);
     }
 
     private synchronized void runSuspendTrigger(final TCFNode node) {
         final int generation = ++suspend_trigger_generation;
         final ISuspendTriggerListener[] listeners = suspend_trigger_listeners.toArray(
                 new ISuspendTriggerListener[suspend_trigger_listeners.size()]);
         if (listeners.length == 0) return;
         display.asyncExec(new Runnable() {
             public void run() {
                 synchronized (TCFModel.this) {
                     if (generation != suspend_trigger_generation) return;
                 }
                 for (final ISuspendTriggerListener listener : listeners) {
                     try {
                         listener.suspended(launch, node);
                     }
                     catch (Throwable x) {
                         Activator.log(x);
                     }
                 }
             }
         });
     }
 
     /**
      * Set whether instruction stepping mode should be enabled or not.
      *
      * @param enabled
      */
     public void setInstructionSteppingEnabled(boolean enabled) {
         instruction_stepping_enabled = enabled;
     }
 
     /**
      * @return whether instruction stepping is enabled
      */
     public boolean isInstructionSteppingEnabled() {
         return instruction_stepping_enabled;
     }
 }
