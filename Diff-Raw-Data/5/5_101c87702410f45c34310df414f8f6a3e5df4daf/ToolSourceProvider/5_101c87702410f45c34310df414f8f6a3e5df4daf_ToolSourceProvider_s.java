 /*******************************************************************************
  * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet).
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     Stephan Wahlbrink - initial API and implementation
  *******************************************************************************/
 
 package de.walware.statet.nico.internal.ui;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.expressions.IEvaluationContext;
 import org.eclipse.ui.AbstractSourceProvider;
 import org.eclipse.ui.ISources;
 import org.eclipse.ui.IWindowListener;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PlatformUI;
 
 import de.walware.statet.nico.core.runtime.ToolProcess;
 import de.walware.statet.nico.ui.IToolRegistryListener;
 import de.walware.statet.nico.ui.ToolSessionUIData;
 
 
 public class ToolSourceProvider extends AbstractSourceProvider implements IWindowListener {
 	
 	
 	public static final String ACTIVE_TOOL_NAME = "de.walware.statet.activeTool";
 	
 	
 	private class RegistryListerner implements IToolRegistryListener {
 		
 		private final IWorkbenchWindow fWindow;
 		
 		public RegistryListerner(final IWorkbenchWindow window) {
 			fWindow = window;
 		}
 		
 		public void toolSessionActivated(final ToolSessionUIData info) {
 			if (fActiveWindow == fWindow) {
 				handleActivated(info.getProcess());
 			}
 		}
 		
 		public void toolTerminated(final ToolSessionUIData sessionData) {
 			if (fActiveWindow == fWindow) {
 				handleTerminated(sessionData.getProcess());
 			}
 		}
 	};
 	
 	
 	private final ToolRegistry fRegistry;
 	private final List<RegistryListerner> fCreatedListeners;
 	private IWorkbenchWindow fActiveWindow;
 	private ToolProcess fCurrentTool;
 	
 	
 	public ToolSourceProvider() {
 		fCreatedListeners = new ArrayList<RegistryListerner>();
 		PlatformUI.getWorkbench().addWindowListener(this);
 		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
 			windowOpened(window);
 		};
		fRegistry = NicoUIPlugin.getDefault().getToolRegistry();
 	}
 	
 	
 	public void dispose() {
 		synchronized (fCreatedListeners) {
 			final Iterator<RegistryListerner> iter = fCreatedListeners.iterator();
 			while (iter.hasNext()) {
 				fRegistry.removeListener(iter.next());
 				iter.remove();
 			}
 		}
 	}
 	
 	
 	public void windowOpened(final IWorkbenchWindow window) {
 		final RegistryListerner listener = new RegistryListerner(window);
 		synchronized (fCreatedListeners) {
 			fCreatedListeners.add(listener);
 		}
 		fRegistry.addListener(listener, window.getActivePage());
 	}
 	
 	public void windowClosed(final IWorkbenchWindow window) {
 		synchronized (fCreatedListeners) {
 			final Iterator<RegistryListerner> iter = fCreatedListeners.iterator();
 			while (iter.hasNext()) {
 				if (iter.next().fWindow == window) {
 					iter.remove();
 				}
 			}
 		}
 		fActiveWindow = null;
 	}
 	
 	public void windowActivated(final IWorkbenchWindow window) {
 		fActiveWindow = window;
 		handleActivated(fRegistry.getActiveToolSession(window.getActivePage()).getProcess());
 	}
 	
 	public void windowDeactivated(final IWorkbenchWindow window) {
 	}
 	
 	
 	private void handleActivated(final ToolProcess tool) {
 		synchronized (this) {
 			if (fCurrentTool == tool || (fCurrentTool == null && tool == null)) {
 				return;
 			}
 			fCurrentTool = tool;
 		}
 		if (DEBUG) {
 			System.out.println("[tool source] changed:" + (tool != null ? tool.getLabel() : "-"));
 		}
 		final Object value = (tool != null) ? tool : IEvaluationContext.UNDEFINED_VARIABLE;
 		fireSourceChanged(ISources.WORKBENCH, ACTIVE_TOOL_NAME, value);
 	}
 	
 	private void handleTerminated(final ToolProcess tool) {
 		synchronized (this) {
 			if (fCurrentTool == tool) {
 				if (DEBUG) {
 					System.out.println("[tool source] terminated:" + (tool != null ? tool.getLabel() : "-"));
 				}
 				fireSourceChanged(ISources.WORKBENCH, ACTIVE_TOOL_NAME, tool);
 			}
 		}
 	}
 	
 	public String[] getProvidedSourceNames() {
 		return new String[] { ACTIVE_TOOL_NAME };
 	}
 	
 	public Map getCurrentState() {
 		final Map<String, Object> map = new HashMap<String, Object>();
 		Object tool = null;
 		if (fActiveWindow != null) {
 			tool = fRegistry.getActiveToolSession(fActiveWindow.getActivePage()).getProcess();
 		}
 		if (tool == null) {
 			tool = IEvaluationContext.UNDEFINED_VARIABLE;
 		}
 		map.put(ACTIVE_TOOL_NAME, tool);
 		return map;
 	}
 	
 }
