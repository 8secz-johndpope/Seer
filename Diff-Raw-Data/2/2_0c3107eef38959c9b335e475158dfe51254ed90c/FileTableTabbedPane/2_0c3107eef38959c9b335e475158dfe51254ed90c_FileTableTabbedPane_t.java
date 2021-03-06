 /*
  * This file is part of muCommander, http://www.mucommander.com
  * Copyright (C) 2002-2012 Maxence Bernard
  *
  * muCommander is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *
  * muCommander is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.mucommander.ui.main.tabs;
 
 import java.awt.Point;
 import java.awt.event.FocusEvent;
 import java.awt.event.FocusListener;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JTabbedPane;
 import javax.swing.SwingUtilities;
 
 import com.mucommander.commons.runtime.JavaVersions;
 import com.mucommander.core.LocationChanger.ChangeFolderThread;
 import com.mucommander.desktop.DesktopManager;
 import com.mucommander.ui.main.FolderPanel;
 import com.mucommander.ui.main.MainFrame;
 import com.mucommander.ui.tabs.TabbedPane;
 
 /**
  * TabbedPane that present the FileTable tabs.
  * 
  * This TabbedPane doesn't contain different FileTable for each tab, instead
  * it use one FileTable instance as a shared object for all tabs. when switching between
  * tabs, the FileTable instance is updated as needed according to the selected tab state.
  * 
  * @author Arik Hadas
  */
 public class FileTableTabbedPane extends TabbedPane<FileTableTab> implements FocusListener {
 
 	/** The FileTable instance presented in each tab */
 	private JComponent fileTableComponent;
 	
 	private MainFrame mainFrame;
 	private FolderPanel folderPanel;
 	private FileTableTabHeaderFactory headersFactory;
 	
 
 	public FileTableTabbedPane(MainFrame mainFrame, FolderPanel folderPanel, JComponent fileTableComponent, FileTableTabHeaderFactory headersFactory) {
 		this.fileTableComponent = fileTableComponent;
 		this.mainFrame = mainFrame;
 		this.folderPanel = folderPanel;
 		this.headersFactory = headersFactory;
 
 		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
 
 		addMouseListener(new MouseAdapter() {
 			
 			public void mouseClicked(MouseEvent e) {
 				final Point clickedPoint = e.getPoint();
 				int selectedTabIndex = indexAtLocation(clickedPoint.x, clickedPoint.y);
 				if (selectedTabIndex != -1) {
 					if (DesktopManager.isRightMouseButton(e)) {
 						setSelectedIndex(selectedTabIndex);
 						
 						// Open the popup menu only after all swing events are finished, to ensure that when the popup menu is shown
 						// and asks for the currently selected tab in the active panel, it'll get the right one
 						SwingUtilities.invokeLater(new Runnable() {
 							
 							public void run() {
 								new FileTableTabPopupMenu(FileTableTabbedPane.this.mainFrame).show(FileTableTabbedPane.this, clickedPoint.x, clickedPoint.y);	
 							}
 						});
 					}
 				}
 			}
 		});
 		
 		addFocusListener(this);
 	}
 	
 	@Override
 	public boolean requestFocusInWindow() {
 		return fileTableComponent.requestFocusInWindow();
 	}
 
 	@Override
 	public void removeTabAt(int index) {
 		super.removeTabAt(index);
 		
 		if (index == 0 && getTabCount() > 0)
 			setComponentAt(0, fileTableComponent); 
 	}
 	
 	/**
 	 * Not in use yet
 	 * 
 	 * @param index
 	 * @param component
 	 */
 	public void setTabHeader(int index, FileTableTabHeader component) {
 		super.setTabComponentAt(index, component);
 	}
 	
 	@Override
 	public void add(FileTableTab tab) {
 		add(tab, getTabCount());
 	}
 
 	@Override
 	public void add(FileTableTab tab, int index) {
 		add(getTabCount() == 0 ? fileTableComponent : new JLabel(), index);
 
 		update(tab, index);
 	}
 	
 	@Override
 	public void setSelectedIndex(int index) {
 		// Allow tabs switching only when no-events-mode is disabled
 		if (!mainFrame.getNoEventsMode()) {
 
 		    super.setSelectedIndex(index);
 			requestFocusInWindow();
 		}
 	}
 
 	@Override
 	public void update(FileTableTab tab, int index) {
 		if (JavaVersions.JAVA_1_5.isCurrentOrLower()) {
 			/*setLockedAt(index, tab.isLocked());
 			setTitleAt(index, tab.getLocation().getName());	*/
 		}
 		else {
 			setTabHeader(index, headersFactory.create(tab));
 		}
 		
 		setToolTipTextAt(index, tab.getLocation().getAbsolutePath());
 		
 		SwingUtilities.invokeLater(new Runnable() {
 
 			public void run() {
				validate();
 			}
 		});
 	}
 
 	@Override
 	public void show(FileTableTab t) {
 		// Set no events mode before trying to change current folder in order to prevent further changes due to (fast) 
 		// tabs switching (tabs switching won't happen during no-events-mode) before current folder change is finished
 		// TODO: change the location of mainFrame.setNoEventsMode(true) call at ChangeFolderThread instead 
 		mainFrame.setNoEventsMode(true);
 		ChangeFolderThread changeFolderThread = folderPanel.tryChangeCurrentFolderEvenOnLockedTab(t.getLocation());
 		// If the operations wasn't started, activate all operations
 		if (changeFolderThread == null)
 			mainFrame.setNoEventsMode(false);
 		
 		validate();
 	}
 
 	//////////////////////////////////
 	// FocusListener implementation //
 	//////////////////////////////////
 	
 	public void focusGained(FocusEvent e) {
 		folderPanel.getTabs().requestFocus();
 	}
 
 	public void focusLost(FocusEvent e) { }
 }
