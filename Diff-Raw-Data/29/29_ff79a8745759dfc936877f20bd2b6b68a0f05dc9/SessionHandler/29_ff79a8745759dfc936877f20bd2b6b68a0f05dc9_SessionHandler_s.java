 package org.cytoscape.internal;
 
 /*
  * #%L
  * Cytoscape Swing Application Impl (swing-application-impl)
  * $Id:$
  * $HeadURL:$
  * %%
  * Copyright (C) 2006 - 2013 The Cytoscape Consortium
  * %%
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as 
  * published by the Free Software Foundation, either version 2.1 of the 
  * License, or (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Lesser Public License for more details.
  * 
  * You should have received a copy of the GNU General Lesser Public 
  * License along with this program.  If not, see
  * <http://www.gnu.org/licenses/lgpl-2.1.html>.
  * #L%
  */
 
 import java.io.File;
 import java.math.BigInteger;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import javax.swing.JInternalFrame;
 import javax.swing.JOptionPane;
 import javax.swing.SwingUtilities;
 
 import org.cytoscape.application.events.CyShutdownEvent;
 import org.cytoscape.application.events.CyShutdownListener;
 import org.cytoscape.application.swing.CytoPanel;
 import org.cytoscape.application.swing.CytoPanelName;
 import org.cytoscape.application.swing.CytoPanelState;
 import org.cytoscape.internal.io.SessionIO;
 import org.cytoscape.internal.io.networklist.Network;
 import org.cytoscape.internal.io.networklist.NetworkList;
 import org.cytoscape.internal.io.sessionstate.Cytopanel;
 import org.cytoscape.internal.io.sessionstate.Cytopanels;
 import org.cytoscape.internal.io.sessionstate.NetworkFrame;
 import org.cytoscape.internal.io.sessionstate.NetworkFrames;
 import org.cytoscape.internal.io.sessionstate.SessionState;
 import org.cytoscape.internal.view.CytoscapeDesktop;
 import org.cytoscape.internal.view.NetworkPanel;
 import org.cytoscape.internal.view.NetworkViewManager;
 import org.cytoscape.model.CyNetwork;
 import org.cytoscape.model.CyNetworkManager;
 import org.cytoscape.session.CySession;
 import org.cytoscape.session.CySessionManager;
 import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
 import org.cytoscape.session.events.SessionAboutToBeSavedListener;
 import org.cytoscape.session.events.SessionLoadedEvent;
 import org.cytoscape.session.events.SessionLoadedListener;
 import org.cytoscape.task.write.SaveSessionAsTaskFactory;
 import org.cytoscape.util.swing.FileChooserFilter;
 import org.cytoscape.util.swing.FileUtil;
 import org.cytoscape.view.model.CyNetworkView;
 import org.cytoscape.work.SynchronousTaskManager;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class SessionHandler implements CyShutdownListener, SessionLoadedListener, SessionAboutToBeSavedListener {
 
 	private static final String APP_NAME = "org.cytoscape.swing-application";
 	private static final String SESSION_STATE_FILENAME = "session_state.xml";
 	private static final String NETWORK_LIST_FILENAME = "network_list.xml";
 	
 	private final CytoscapeDesktop desktop;
 	private final CyNetworkManager netMgr;
 	private final NetworkViewManager netViewMgr;
 	private final SynchronousTaskManager<?> syncTaskMgr;
 	private final SaveSessionAsTaskFactory saveTaskFactory;
 	private final SessionIO sessionIO;
 	private final CySessionManager sessionManager;
 	private final FileUtil fileUtil;
 	private final NetworkPanel netPanel;
 	private final Map<String, CytoPanelName> CYTOPANEL_NAMES = new LinkedHashMap<String, CytoPanelName>();
 	
 	private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
 	
 	public SessionHandler(final CytoscapeDesktop desktop,
 						  final CyNetworkManager netMgr,
 						  final NetworkViewManager netViewMgr,
 						  final SynchronousTaskManager<?> syncTaskMgr,
 						  final SaveSessionAsTaskFactory saveTaskFactory,
 						  final SessionIO sessionIO,
 						  final CySessionManager sessionManager,
 						  final FileUtil fileUtil,
 						  final NetworkPanel netPanel) {
 		this.desktop = desktop;
 		this.netMgr = netMgr;
 		this.netViewMgr = netViewMgr;
 		this.syncTaskMgr = syncTaskMgr;
 		this.saveTaskFactory = saveTaskFactory;
 		this.sessionIO = sessionIO;
 		this.sessionManager = sessionManager;
 		this.fileUtil = fileUtil;
 		this.netPanel = netPanel;
 		
 		CYTOPANEL_NAMES.put("CytoPanel1", CytoPanelName.WEST);
 		CYTOPANEL_NAMES.put("CytoPanel2", CytoPanelName.SOUTH);
 		CYTOPANEL_NAMES.put("CytoPanel3", CytoPanelName.EAST);
 	}
 
 	@Override
 	public void handleEvent(final CyShutdownEvent e) {
 		// If there are no networks, just quit.
		if (netMgr.getNetworkSet().size() == 0) 
 			return;
 
 		// Ask user whether to save current session or not.
 		final String msg = "Do you want to save your session?";
 		final String header = "Save Networks Before Quitting?";
 		final Object[] options = { "Yes, save and quit", "No, just quit", "Cancel" };
 		final int n = JOptionPane.showOptionDialog(desktop.getJFrame(), msg, header,
 		                                     JOptionPane.YES_NO_OPTION,
 		                                     JOptionPane.QUESTION_MESSAGE, 
 											 null, options, options[0]);
 
 		if (n == JOptionPane.NO_OPTION) {
 			return;
 		} else if (n == JOptionPane.YES_OPTION) {
 			final String sessionFileName = sessionManager.getCurrentSessionFileName();
 			final File file;
 			
 			if (sessionFileName == null || sessionFileName.isEmpty() ){
 				FileChooserFilter filter = new FileChooserFilter("Session File" , "cys");
 				List<FileChooserFilter> filterCollection = new ArrayList<FileChooserFilter>(1);
 				filterCollection.add(filter);
 				file = fileUtil.getFile(desktop, "Save Session File", FileUtil.SAVE, filterCollection );
 			} else {
 				file = new File(sessionFileName);
 			}
 			
 			if (file == null){ //just check the file again in case the file chooser dialoge task is canceled.
 				e.abortShutdown("User canceled the shutdown request.");
 				return;
 			}
 			
 			syncTaskMgr.execute(saveTaskFactory.createTaskIterator(file));
 			return;
 		} else {
 			e.abortShutdown("User canceled the shutdown request.");
 			return; 
 		}
 	}
 
 	@Override
 	public void handleEvent(final SessionAboutToBeSavedEvent e) {
 		// Do not use invokeLater() here.  It breaks session file.
 		final File f1 = saveSessionState(e);
 		final File f2 = saveNetworkList(e);
 		
 		final List<File> files = new ArrayList<File>();
 		if (f1 != null) files.add(f1);
 		if (f2 != null) files.add(f2);
 		
 		// Add it to the apps list
 		try {
 			if (!files.isEmpty())
 				e.addAppFiles(APP_NAME, files);
 		} catch (Exception ex) {
 			logger.error("Error adding app files to be saved in the session.", ex);
 		}
 	}
 	
 	@Override
 	public void handleEvent(final SessionLoadedEvent e) {
 		final CySession sess = e.getLoadedSession();
 
 		if (sess == null)
 			return;
 		
 		SwingUtilities.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				postLoading(sess);
 			}
 		});
 	}
 	
 	private File saveSessionState(final SessionAboutToBeSavedEvent e) {
 		final SessionState sessState = new SessionState();
 
 		// Network Frames
 		final NetworkFrames netFrames = new NetworkFrames();
 		sessState.setNetworkFrames(netFrames);
 
 		final JInternalFrame[] internalFrames = netViewMgr.getDesktopPane().getAllFrames();
 
 		for (JInternalFrame iframe : internalFrames) {
 			final CyNetworkView view = netViewMgr.getNetworkView(iframe);
 
 			if (view == null) {
 				logger.error("Cannot save position of network frame \"" + iframe.getTitle()
 						+ "\": Network View is null.");
 				continue;
 			}
 
 			final NetworkFrame nf = new NetworkFrame();
 			nf.setNetworkViewID(view.getSUID().toString());
 			nf.setX(BigInteger.valueOf(iframe.getX()));
 			nf.setY(BigInteger.valueOf(iframe.getY()));
 
 			netFrames.getNetworkFrame().add(nf);
 		}
 
 		// CytoPanels States
 		final Cytopanels cytopanels = new Cytopanels();
 		sessState.setCytopanels(cytopanels);
 
 		for (Map.Entry<String, CytoPanelName> entry : CYTOPANEL_NAMES.entrySet()) {
 			final CytoPanel p = desktop.getCytoPanel(entry.getValue());
 
 			final Cytopanel cytopanel = new Cytopanel();
 			cytopanel.setId(entry.getKey());
 			cytopanel.setPanelState(p.getState().toString());
 			cytopanel.setSelectedPanel(Integer.toString(p.getSelectedIndex()));
 
 			cytopanels.getCytopanel().add(cytopanel);
 		}
 
 		// Create temp file
 		File tmpFile = new File(System.getProperty("java.io.tmpdir"), SESSION_STATE_FILENAME);
 		tmpFile.deleteOnExit();
 
 		// Write to the file
 		sessionIO.write(sessState, tmpFile);
 
 		return tmpFile;
 	}
 	
 	private File saveNetworkList(final SessionAboutToBeSavedEvent e) {
 		final Map<Long, Integer> netOrder = netPanel.getNetworkListOrder();
 		
 		// Create the JAXB objects
 		final NetworkList netList = new NetworkList();
 		
 		for (final Entry<Long, Integer> entry : netOrder.entrySet()) {
 			final Long suid = entry.getKey();
 			final Integer order = entry.getValue();
 			
 			if (order != null) {
 				final Network n = new Network();
 				n.setId(suid);
 				n.setOrder(order);
 				netList.getNetwork().add(n);
 			}
 		}
 		
 		// Create temp file
 		File tmpFile = new File(System.getProperty("java.io.tmpdir"), NETWORK_LIST_FILENAME);
 		tmpFile.deleteOnExit();
 
 		// Write to the file
 		sessionIO.write(netList, tmpFile);
 		
 		return tmpFile;
 	}
 	
 	private final void postLoading(final CySession sess) {
 		final Map<String, List<File>> filesMap = sess.getAppFileListMap();
 
 		if (filesMap != null) {
 			final List<File> files = filesMap.get(APP_NAME);
 
 			if (files != null) {
 				SessionState sessState = null;
 				NetworkList netList = null;
 				
 				for (File f : files) {
 					if (f.getName().endsWith(SESSION_STATE_FILENAME))
 						sessState = sessionIO.read(f, SessionState.class);
 					else if (f.getName().endsWith(NETWORK_LIST_FILENAME))
 						netList = sessionIO.read(f, NetworkList.class);
 				}
 				
 				if (sessState != null) {
 					setNetworkFrameLocations(sessState.getNetworkFrames(), sess);
 					setCytoPanelStates(sessState.getCytopanels());
 				}
 				
 				if (netList != null) {
 					setNetworkListOrder(netList.getNetwork(), sess);
 				}
 			}
 		}
 	}
 
 	/**
 	 * Restore each network frame's location.
 	 * @param frames
 	 */
 	private void setNetworkFrameLocations(final NetworkFrames frames, final CySession sess) {
 		if (frames != null) {
 			final List<NetworkFrame> framesList = frames.getNetworkFrame();
 			
 			for (NetworkFrame nf : framesList) {
 				final String oldIdStr = nf.getNetworkViewID(); // ID in the original session--it's probably different now
 				CyNetworkView view = null;
 				
 				// Try to convert the old ID to Long--only works if the loaded session is a 3.0 format
 				try {
 					final Long oldSuid = Long.valueOf(oldIdStr);
 					view = sess.getObject(oldSuid, CyNetworkView.class);
 				} catch (NumberFormatException nfe) {
 					logger.debug("The old network view id is not a number: " + oldIdStr);
 					view = sess.getObject(oldIdStr, CyNetworkView.class);
 				}
 				
 				if (view != null) {
 					final JInternalFrame iframe = netViewMgr.getInternalFrame(view);
 					
 					if (iframe != null) {
 						iframe.moveToBack(); // In order to restore its z-index
 						
 						if (nf.getX() != null && nf.getY() != null) {
 							int x = nf.getX().intValue();
 							int y = nf.getY().intValue();
 							iframe.setLocation(x, y);
 						}
 					}
 				} else {
 					logger.warn("Cannot restore network frame's position: Network View not found for former ID \""
 							+ oldIdStr + "\".");
 				}
 			}
 		}
 	}
 	
 	/**
 	 * Restore the states of the CytoPanels.
 	 * @param cytopanels
 	 */
 	private void setCytoPanelStates(final Cytopanels cytopanels) {
 		if (cytopanels != null) {
 			final List<Cytopanel> cytopanelsList = cytopanels.getCytopanel();
 
 			for (Cytopanel cytopanel : cytopanelsList) {
 				String id = cytopanel.getId();
 				final CytoPanelName panelName = CYTOPANEL_NAMES.get(id);
 
 				if (panelName != null) {
 					final CytoPanel p = desktop.getCytoPanel(panelName);
 
 					try {
 						p.setState(CytoPanelState.valueOf(cytopanel.getPanelState().toUpperCase().trim()));
 					} catch (Exception ex) {
 						logger.error("Cannot restore the state of panel \"" + panelName.getTitle() + "\"", ex);
 					}
 
 					try {
 						p.setSelectedIndex(Integer.parseInt(cytopanel.getSelectedPanel()));
 					} catch (Exception ex) {
 						logger.error("Cannot restore the selected index of panel \"" + panelName.getTitle() + "\"", ex);
 					}
 				}
 			}
 		}
 	}
 	
 	private void setNetworkListOrder(final List<Network> networks, final CySession sess) {
 		final Map<Long, Integer> order = new HashMap<Long, Integer>();
 		
 		for (final Network n : networks) {
 			final CyNetwork net = sess.getObject(n.getId(), CyNetwork.class); // in order to retrieve the new SUID
 			
 			if (net != null)
 				order.put(net.getSUID(), n.getOrder());
 		}
 		
 		netPanel.setNetworkListOrder(order);
 	}
 }
