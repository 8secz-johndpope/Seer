 /*******************************************************************************
  * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
  * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html.
  ******************************************************************************/
 package de.tuilmenau.ics.fog.ui.eclipse.editors;
 
 import java.io.FileNotFoundException;
 import java.text.Collator;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.Locale;
 import java.util.Observable;
 import java.util.Observer;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.jface.layout.TableColumnLayout;
 import org.eclipse.jface.viewers.ColumnWeightData;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.ScrolledComposite;
 import org.eclipse.swt.custom.StyleRange;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.MenuDetectEvent;
 import org.eclipse.swt.events.MenuDetectListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.MenuItem;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.TableItem;
 import org.eclipse.swt.widgets.ToolBar;
 import org.eclipse.swt.widgets.ToolItem;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.EditorPart;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 
 import de.tuilmenau.ics.fog.IEvent;
 import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
 import de.tuilmenau.ics.fog.eclipse.utils.Resources;
 import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
 import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
 import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
 import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
 import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
 import de.tuilmenau.ics.fog.routing.hierarchical.RoutingTable;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.Cluster;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterMember;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannel;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannelPacketMetaData;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.CoordinatorAsClusterMember;
 import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;
 import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
 import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
 import de.tuilmenau.ics.fog.ui.Logging;
 
 
 /**
  * The HRM viewer, which depicts all information from an HRM controller.
  * 
  */
 public class HRMViewer extends EditorPart implements Observer, Runnable, IEvent
 {
 	private static boolean HRM_VIEWER_DEBUGGING = HRMConfig.DebugOutput.GUI_SHOW_VIEWER_STEPS;
 	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_CLUSTERING_CONTROLS = true;
 	private static boolean HRM_VIEWER_SHOW_SINGLE_ENTITY_ELECTION_CONTROLS = true;
 	private static boolean HRM_VIEWER_SHOW_ALWAYS_ALL_CLUSTERS = true;
 	
 	private static final boolean GUI_SHOW_COLORED_BACKGROUND_FOR_CONTROL_ENTITIES = true;
 
 	private HRMController mHRMController = null;
     private Composite mShell = null;
     private Display mDisplay = null;
 
     private Composite mContainerRoutingTable = null;
 	private Composite mContainerHRMID2L2ADDRTable = null;
 	private Composite mGlobalContainer = null;
     private Composite mContainer = null;
     private Composite mToolBtnContainer = null;
     private Composite mToolChlBoxContainer = null;
     private ScrolledComposite mScroller = null;
     
     private Button mBtnPriorityLog = null;
     private Button mBtnClusteringLog = null;
     private Button mBtnClusterMembersLog = null;
     private Button mBtnHRMIDLog = null;
     private Button mBtnHRGLog = null;
     private Button mBtnSuperiorCoordinatorsLog = null;
     private Button mBtnUsedClusterAddressesLog = null;
     private Button mBtnTopologyReports = null;
     private Button mBtnShareRoutes = null;
     
     private Button mBtnClusterMembers = null;
     private Button mBtnCoordClusterMembers = null;
     private Button mBtnCoordAnnounce = null;
     
     private Table mTableRoutingTable = null;
     
     private int mGuiCounter = 0;
 
     private boolean mNextGUIUpdateResetsOnlyRoutingTable = false;
     
 	private boolean mShowClusterMembers = false;
 	private boolean mShowCoordinatorAsClusterMembers = false;
 
 	/**
 	 * Stores the time stamp of the last GUI update
 	 */
 	private Double mTimestampLastGUIUpdate =  new Double(0);
 
     /**
      * Stores the simulation time for the next GUI update.
      */
     private double mTimeNextGUIUpdate = 0;
     
 	/**
 	 * Stores the ID of the HRM plug-in
 	 */
 	private static final String PLUGIN_ID = "de.tuilmenau.ics.fog.routing.hrm";
 	
 	/**
 	 * Stores the path to the HRM icons
 	 */
 	private static final String PATH_ICONS = "/icons/";
 
 	/**
 	 * Reference pointer to ourself
 	 */
 	private HRMViewer mHRMViewer = this;
 	
 	public HRMViewer()
 	{
 		
 	}
 	
 	private GridData createGridData(boolean grabSpace, int colSpan)
 	{
 		GridData gridData = new GridData();
 		gridData.horizontalAlignment = SWT.FILL;
 		gridData.grabExcessHorizontalSpace = grabSpace;
 		gridData.horizontalSpan = colSpan;
 		return gridData;
 	}
 
 	private void createButtons(Composite pParent)
 	{
 		if(mGuiCounter == 1){
 			mToolBtnContainer = new Composite(pParent, SWT.NONE);
 	
 			GridLayout tLayout1 = new GridLayout(7, false);
 			mToolBtnContainer.setLayout(tLayout1);
 			mToolBtnContainer.setLayoutData(createGridData(true, 1));
 			
 			mToolChlBoxContainer = new Composite(pParent, SWT.NONE);
 			
 			GridLayout tLayout2 = new GridLayout(5, false);
 			mToolChlBoxContainer.setLayout(tLayout2);
 			mToolChlBoxContainer.setLayoutData(createGridData(true, 1));
 		}
 		
 		/**
 		 * Push buttons
 		 */
 		// **** show priority update log ****
 		if(mGuiCounter == 1){
 			mBtnPriorityLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnPriorityLog.setText("Show priority update events");
 			mBtnPriorityLog.setLayoutData(createGridData(false, 1));
 			mBtnPriorityLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Logging.log(this, "Connectivity priority updates: " + mHRMController.getGUIDescriptionConnectivityPriorityUpdates());
 					Logging.log(this, "Hierarchy priority updates: " + mHRMController.getGUIDescriptionHierarchyPriorityUpdates());
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 		// **** show update cluster log ****
 		if(mGuiCounter == 1){
 			mBtnClusteringLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnClusteringLog.setText("Show clustering events");
 			mBtnClusteringLog.setLayoutData(createGridData(false, 1));
 			mBtnClusteringLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Logging.log(this, "Clustering updates: " + mHRMController.getGUIDescriptionClusterUpdates());
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 		// **** show active ClusterMember update log ****
 		if(mGuiCounter == 1){
 			mBtnClusterMembersLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnClusterMembersLog.setText("Show active ClusterMember events");
 			mBtnClusterMembersLog.setLayoutData(createGridData(false, 1));
 			mBtnClusterMembersLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Logging.log(this, "Active ClusterMember updates: " + ((String)mHRMController.getGUIDescriptionNodeElectionStateChanges()));
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 		// **** show HRMID update log ****
 		if(mGuiCounter == 1){
 			mBtnHRMIDLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnHRMIDLog.setText("Show HRMID events");
 			mBtnHRMIDLog.setLayoutData(createGridData(false, 1));
 			mBtnHRMIDLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Logging.log(this, "HRMID updates: " + mHRMController.getGUIDescriptionHRMIDChanges());
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 		// **** show HRG update log ****
 		if(mGuiCounter == 1){
 			mBtnHRGLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnHRGLog.setText("Show HRG events");
 			mBtnHRGLog.setLayoutData(createGridData(false, 1));
 			mBtnHRGLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Logging.log(this, "HRG updates: " + mHRMController.getGUIDescriptionHRGChanges());
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}		
 		// **** show superior coordinators ****
 		if(mGuiCounter == 1){
 			mBtnSuperiorCoordinatorsLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnSuperiorCoordinatorsLog.setText("Show superior coordinators");
 			mBtnSuperiorCoordinatorsLog.setLayoutData(createGridData(false, 1));
 			mBtnSuperiorCoordinatorsLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {					
 					LinkedList<ClusterName> tSuperiorCoordinators = mHRMController.getAllSuperiorCoordinators();
 					Logging.log(this, "Superior coordinators:");
 					int i = 0;
 					for(ClusterName tSuperiorCoordinator : tSuperiorCoordinators){
 						Logging.log(this, "    ..[" + i + "]: Coordinator" + tSuperiorCoordinator.getGUICoordinatorID() + "@" + tSuperiorCoordinator.getHierarchyLevel().getValue() + "(Cluster" + tSuperiorCoordinator.getGUIClusterID() + ")");
 					}					
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 		// **** show superior coordinators ****
 		if(mGuiCounter == 1){
 			mBtnUsedClusterAddressesLog = new Button(mToolBtnContainer, SWT.PUSH);
 			mBtnUsedClusterAddressesLog.setText("Show used addresses");
 			mBtnUsedClusterAddressesLog.setLayoutData(createGridData(false, 1));
 			mBtnUsedClusterAddressesLog.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {					
 					Logging.log(this, "Used cluster addresses: " + mHRMController.getGUIDEscriptionUsedAddresses());
 				}
 				public String toString()
 				{
 					return mHRMViewer.toString();
 				}
 			});
 		}
 
 		/**
 		 * Check buttons
 		 */
 		// **** hide/show cluster members ****
 		if(mGuiCounter == 1){
 			mBtnClusterMembers = new Button(mToolChlBoxContainer, SWT.CHECK);
 		}
 		mBtnClusterMembers.setText("ClusterMembers");
 		if(mShowClusterMembers){
 			mBtnClusterMembers.setSelection(true);
 		}else{
 			mBtnClusterMembers.setSelection(false);
 		}
 		if(mGuiCounter == 1){
 			mBtnClusterMembers.setLayoutData(createGridData(false, 1));
 			mBtnClusterMembers.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					mShowClusterMembers = !mShowClusterMembers;
 					startGUIUpdateTimer();
 				}
 			});
 		}
 		// **** hide/show coordinators-as-cluster-members ****
 		if(mGuiCounter == 1){
 			mBtnCoordClusterMembers = new Button(mToolChlBoxContainer, SWT.CHECK);
 		}
 		mBtnCoordClusterMembers.setText("CoordinatorAsClusterMembers");
 		if(mShowCoordinatorAsClusterMembers){
 			mBtnCoordClusterMembers.setSelection(true);
 		}else{
 			mBtnCoordClusterMembers.setSelection(false);
 		}
 		if(mGuiCounter == 1){
 			mBtnCoordClusterMembers.setLayoutData(createGridData(false, 1));
 			mBtnCoordClusterMembers.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					mShowCoordinatorAsClusterMembers = !mShowCoordinatorAsClusterMembers;
 					startGUIUpdateTimer();
 				}
 			});
 		}
 		// **** deactivate/activate coordinator announcements ****
 		if(mGuiCounter == 1){
 			mBtnCoordAnnounce = new Button(mToolChlBoxContainer, SWT.CHECK);
 		}
 		mBtnCoordAnnounce.setText("Coordinator announcements");
 		if (Coordinator.GUI_USER_CTRL_COORDINATOR_ANNOUNCEMENTS){
 			mBtnCoordAnnounce.setSelection(true);
 		}else{
 			mBtnCoordAnnounce.setSelection(false);
 		}
 		if(mGuiCounter == 1){
 			mBtnCoordAnnounce.setLayoutData(createGridData(false, 1));
 			mBtnCoordAnnounce.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					Coordinator.GUI_USER_CTRL_COORDINATOR_ANNOUNCEMENTS = !Coordinator.GUI_USER_CTRL_COORDINATOR_ANNOUNCEMENTS;
 				}
 			});
 		}
 		// **** deactivate/activate topology reports ****
 		if(mGuiCounter == 1){
 			mBtnTopologyReports = new Button(mToolChlBoxContainer, SWT.CHECK);
 		}
 		mBtnTopologyReports.setText("Topology reports");
 		if (HRMController.GUI_USER_CTRL_REPORT_TOPOLOGY){
 			mBtnTopologyReports.setSelection(true);
 		}else{
 			mBtnTopologyReports.setSelection(false);
 		}
 		if(mGuiCounter == 1){
 			mBtnTopologyReports.setLayoutData(createGridData(false, 1));
 			mBtnTopologyReports.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					HRMController.GUI_USER_CTRL_REPORT_TOPOLOGY = !HRMController.GUI_USER_CTRL_REPORT_TOPOLOGY;
 				}
 			});
 		}
 		// **** deactivate/activate share routes ****
 		if(mGuiCounter == 1){
 			mBtnShareRoutes = new Button(mToolChlBoxContainer, SWT.CHECK);
 		}
 		mBtnShareRoutes.setText("Share routes");
 		if (HRMController.GUI_USER_CTRL_SHARE_ROUTES){
 			mBtnShareRoutes.setSelection(true);
 		}else{
 			mBtnShareRoutes.setSelection(false);
 		}
 		if(mGuiCounter == 1){
 			mBtnShareRoutes.setLayoutData(createGridData(false, 1));
 			mBtnShareRoutes.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent pEvent) {
 					HRMController.GUI_USER_CTRL_SHARE_ROUTES = !HRMController.GUI_USER_CTRL_SHARE_ROUTES;
 				}
 			});
 		}
 	}
 	
 	/**
 	 * Resets all parts of the EditorPart
 	 */
 	private void destroyPartControl()
 	{
 		mContainer.dispose();
 		
 		//HINT: don't dispose the mScroller object here, this would lead to GUI display problems
 		
 		mShell.redraw();
 	}
 
 	/**
 	 * Creates all needed parts of the EditorPart.
 	 * 
 	 * @param pParent the parent shell
 	 */
 	@Override
 	public void createPartControl(Composite pParent)
 	{
 		mGuiCounter++;
 		
 		mShell = pParent;
 		mDisplay = pParent.getDisplay();
 
 		if (mGuiCounter == 1){
 			mGlobalContainer = new Composite(pParent, SWT.NONE);//NO_BACKGROUND);
 		    GridLayout gridLayout = new GridLayout(1, false);
 		    mGlobalContainer.setLayout(gridLayout);
 		}
 	    
 		if (mGuiCounter == 1){
 			mScroller = new ScrolledComposite(mGlobalContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
 			GridData tScrollerLayoutData = new GridData();
 			tScrollerLayoutData.horizontalAlignment = SWT.FILL;
 			tScrollerLayoutData.verticalAlignment = SWT.FILL;
 			tScrollerLayoutData.grabExcessVerticalSpace = true;
 			tScrollerLayoutData.grabExcessHorizontalSpace = true;
 			tScrollerLayoutData.horizontalSpan = 1;
 			mScroller.setExpandHorizontal(true);
 			mScroller.setLayout(new GridLayout());
 			mScroller.setLayoutData(tScrollerLayoutData);
 			// fix the mouse wheel function
 			mScroller.addListener(SWT.Activate, new Listener() {
 			    public void handleEvent(Event e) {
 			    	mScroller.setFocus();
 			    }
 			});
 			// fix the vertical scrolling speed
 			mScroller.getVerticalBar().setIncrement(mScroller.getVerticalBar().getIncrement() * 20 /* 20 times faster */);
 		}
 
 		mContainer = new Composite(mScroller, SWT.NONE);
 		mContainer.setLayout(new GridLayout(1, false));
 		mContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
 		mScroller.setContent(mContainer);
 
 		/**
 		 * Clusters, coordinators, ...
 		 */
 		if (HRM_VIEWER_DEBUGGING){
 			Logging.log(this, "Found clusters: " + mHRMController.getAllClusters().size());
 			Logging.log(this, "Found coordinators: " + mHRMController.getAllCoordinators().size());
 		}
 
 		/**
 		 * GUI part 0: list clusters
 		 */
 		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
 			for (Cluster tCluster: mHRMController.getAllClusters(i)) {
 				// show only those cluster which also have a coordinator
 				if((HRM_VIEWER_SHOW_ALWAYS_ALL_CLUSTERS) || (tCluster.hasLocalCoordinator())){
 					// print info. about cluster
 					printClusterMember(mContainer, tCluster);
 				}
 			}
 		}
 
 		/**
 		 * GUI part: list cluster members
 		 */
 		if (mShowClusterMembers){
 			for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
 				for(ClusterMember tClusterMemeber : mHRMController.getAllClusterMembers(i)){
 					if (!(tClusterMemeber instanceof Cluster)){
 						// print info. about cluster
 						printClusterMember(mContainer, tClusterMemeber);
 					}
 				}
 			}
 		}
 		
 		/**
 		 * GUI part: list coordinator as cluster members
 		 */
 		if(mShowCoordinatorAsClusterMembers){
 			for(CoordinatorAsClusterMember tCoordinatorAsClusterMember : mHRMController.getAllCoordinatorAsClusterMembers()){
 				// print info. about cluster
 				printClusterMember(mContainer, tCoordinatorAsClusterMember);
 			}
 		}
 		/**
 		 * GUI part 1: list coordinators
 		 */
 		for(int i = 0; i < HRMConfig.Hierarchy.HEIGHT; i++){
 			for (Coordinator tCoordinator: mHRMController.getAllCoordinators(i)) {
 				// print info. about cluster
 				printCoordinator(mContainer, tCoordinator);
 			}
 		}
 
 		/**
 		 * GUI part 2: table of known mappings from HRMID to L2Addresses
 		 */
 		if (HRM_VIEWER_DEBUGGING){
 			Logging.log(this, "Printing HRMID-2-L2Address mapping...");
 		}
 		// create the headline
 		StyledText tSignaturesLabel4 = new StyledText(mContainer, SWT.BORDER);
 		String tHRMIDsStr = "";
 		LinkedList<HRMID> tListHRMIDs = mHRMController.getHRMIDs();
 		for(HRMID tHRMID : tListHRMIDs){
 			if(tHRMIDsStr != ""){
 				tHRMIDsStr += ", ";
 			}
 			tHRMIDsStr += tHRMID.toString();
 		}
 		tSignaturesLabel4.setText("Mappings from HRMID to L2Address (local HRMIDs: " + tHRMIDsStr + ")");
 		tSignaturesLabel4.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
 		tSignaturesLabel4.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
 	    StyleRange style4 = new StyleRange();
 	    style4.start = 0;
 	    style4.length = tSignaturesLabel4.getText().length();
 	    style4.fontStyle = SWT.BOLD;
 	    tSignaturesLabel4.setStyleRange(style4);
 	    
 	    // create the GUI container
 	    mContainerHRMID2L2ADDRTable = new Composite(mContainer, SWT.NONE);
 	    GridData tLayoutDataMappingTable = new GridData(SWT.FILL, SWT.FILL, true, true);
 	    tLayoutDataMappingTable.horizontalSpan = 1;
 	    mContainerHRMID2L2ADDRTable.setLayoutData(tLayoutDataMappingTable); 
 	    
 	    // create the table
 		final Table tTableMappingTable = new Table(mContainerHRMID2L2ADDRTable, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
 		tTableMappingTable.setHeaderVisible(true);
 		tTableMappingTable.setLinesVisible(true);
 		
 		// create the columns and define the texts for the header row
 		// col. 0
 		TableColumn tTableHRMID = new TableColumn(tTableMappingTable, SWT.NONE, 0);
 		tTableHRMID.setText("HRMID");
 		// col. 1
 		TableColumn tTableL2Addr = new TableColumn(tTableMappingTable, SWT.NONE, 1);
 		tTableL2Addr.setText("L2 address");
 		
 		HashMap<HRMID, L2Address> tHRMIDToL2AddressMapping = mHRMController.getHRS().getHRMIDToL2AddressMapping();
 		if (HRM_VIEWER_DEBUGGING){
 			Logging.log(this, "Found " + tHRMIDToL2AddressMapping.keySet().size() + " HRMID-to-L2Address mappings");
 		}
 
 		if ((tHRMIDToL2AddressMapping != null) && (!tHRMIDToL2AddressMapping.isEmpty())) {
 			int tRowNumber = 0;
 			for(HRMID tAddr : tHRMIDToL2AddressMapping.keySet()) {
 				L2Address tL2Addr = tHRMIDToL2AddressMapping.get(tAddr);
 				
 				// create the table row
 				TableItem tTableRow = new TableItem(tTableMappingTable, SWT.NONE, tRowNumber);
 				
 				/**
 				 * Column 0: the neighbor name
 				 */
 				tTableRow.setText(0, tAddr.toString());
 
 				/**
 				 * Column 1: route 
 				 */
 				tTableRow.setText(1, tL2Addr.toString());
 
 				tRowNumber++;
 			}
 		}
 		
 		TableColumn[] columns4 = tTableMappingTable.getColumns();
 		for (int k = 0; k < columns4.length; k++){
 			columns4[k].pack();
 		}
 		tTableMappingTable.setLayoutData(new GridData(GridData.FILL_BOTH));
 
 		// create the container layout
 		TableColumnLayout tLayoutMappingTable = new TableColumnLayout();
 		mContainerHRMID2L2ADDRTable.setLayout(tLayoutMappingTable);
 		// assign each column a layout weight
 		tLayoutMappingTable.setColumnData(tTableHRMID, new ColumnWeightData(3));
 		tLayoutMappingTable.setColumnData(tTableL2Addr, new ColumnWeightData(3));
 
 		/**
 		 * GUI part 3: HRM routing table of the node
 		 */
 		createRoutingTable(mContainer);
 		
 		/**
 		 * Tool buttons
 		 */
 		createButtons(mGlobalContainer);
 		
 		// arrange the GUI content in order to full the entire space
 		if (mGuiCounter == 1){
 			mScroller.setSize(mScroller.computeSize(SWT.DEFAULT, SWT.DEFAULT));
 		}
         mContainer.setSize(mContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         mContainerRoutingTable.setSize(mContainerRoutingTable.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         mContainerHRMID2L2ADDRTable.setSize(mContainerHRMID2L2ADDRTable.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         //mToolBtnContainer.setSize(mToolBtnContainer.computeSize(SWT.DEFAULT, 20));
 	}
 
 	/**
 	 * Draws the routing table view
 	 * 
 	 * @param pParent the parent GUI container
 	 */
 	private void createRoutingTable(Composite pParent)
 	{
 		if (HRM_VIEWER_DEBUGGING){
 			Logging.log(this, "Printing HRM routing table...");
 		}
 		// create the headline
 		StyledText tSignaturesLabel2 = new StyledText(pParent, SWT.BORDER);
 		tSignaturesLabel2.setText("HRM Routing Table");
 		tSignaturesLabel2.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
 		tSignaturesLabel2.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
 	    StyleRange style3 = new StyleRange();
 	    style3.start = 0;
 	    style3.length = tSignaturesLabel2.getText().length();
 	    style3.fontStyle = SWT.BOLD;
 	    tSignaturesLabel2.setStyleRange(style3);
 	    
 	    // create the GUI container
 	    mContainerRoutingTable = new Composite(pParent, SWT.NONE);
 	    GridData tLayoutDataRoutingTable = new GridData(SWT.FILL, SWT.FILL, true, true);
 	    tLayoutDataRoutingTable.horizontalSpan = 1;
 	    mContainerRoutingTable.setLayoutData(tLayoutDataRoutingTable); 
 
 	    // create the table
 		mTableRoutingTable = new Table(mContainerRoutingTable, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
 		mTableRoutingTable.setHeaderVisible(true);
 			mTableRoutingTable.setLinesVisible(true);
 		
 		// create the columns and define the texts for the header row
 		// col. 0
 		TableColumn tTableColDest = new TableColumn(mTableRoutingTable, SWT.NONE, 0);
 		tTableColDest.setText("Dest.");
 		// col. 1
 		TableColumn tTableColNext = new TableColumn(mTableRoutingTable, SWT.NONE, 1);
 		tTableColNext.setText("Next hop");
 		// col. 2
 		TableColumn tTableColHops = new TableColumn(mTableRoutingTable, SWT.NONE, 2);
 		tTableColHops.setText("Hops");
 		// col. 3
 		TableColumn tTableColUtil = new TableColumn(mTableRoutingTable, SWT.NONE, 3);
 		tTableColUtil.setText("Util. [%]");
 		// col. 4
 		TableColumn tTableColDelay = new TableColumn(mTableRoutingTable, SWT.NONE, 4);
 		tTableColDelay.setText("MinDelay [ms]");
 		// col. 5
 		TableColumn tTableColDR = new TableColumn(mTableRoutingTable, SWT.NONE, 5);
 		tTableColDR.setText("MaxDR [Kb/s]");
 		// col. 6
 		TableColumn tTableColLoop = new TableColumn(mTableRoutingTable, SWT.NONE, 6);
 		tTableColLoop.setText("Loopback?");
 		// col. 7
 		TableColumn tTableColDirectNeighbor = new TableColumn(mTableRoutingTable, SWT.NONE, 7);
 		tTableColDirectNeighbor.setText("Route to neighbor");
 		// col. 8
 		TableColumn tTableColSource = new TableColumn(mTableRoutingTable, SWT.NONE, 8);
 		tTableColSource.setText("Source");
 		// col. 9
 		TableColumn tTableColNextL2 = new TableColumn(mTableRoutingTable, SWT.NONE, 9);
 		tTableColNextL2.setText("NextL2");
 		// col. 10
 		TableColumn tTableColTimeout = new TableColumn(mTableRoutingTable, SWT.NONE, 10);
 		tTableColTimeout.setText("Timeout");
 		
 		updateRoutingTable();
 		
 		TableColumn[] columns2 = mTableRoutingTable.getColumns();
 		for (int k = 0; k < columns2.length; k++){
 			columns2[k].pack();
 		}
 		mTableRoutingTable.setLayoutData(new GridData(GridData.FILL_BOTH));//SWT.FILL, SWT.TOP, true, true, 1, 1));
 
 		// create the container layout
 		TableColumnLayout tLayoutRoutingTable2 = new TableColumnLayout();
 		mContainerRoutingTable.setLayout(tLayoutRoutingTable2);
 		// assign each column a layout weight
 		tLayoutRoutingTable2.setColumnData(tTableColDest, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColNext, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColHops, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColUtil, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColDelay, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColDR, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColLoop, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColDirectNeighbor, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColSource, new ColumnWeightData(1));
 		tLayoutRoutingTable2.setColumnData(tTableColNextL2, new ColumnWeightData(3));
 		tLayoutRoutingTable2.setColumnData(tTableColTimeout, new ColumnWeightData(1));
 		
 		/**
 		 * Add a listener to allow re-sorting of the table based on the destination per table row
 		 */
 		tTableColDest.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event e) {
 				// sort column 2
 		        TableItem[] tAllRows = mTableRoutingTable.getItems();
 		        Collator collator = Collator.getInstance(Locale.getDefault());
 		        
 		        for (int i = 1; i < tAllRows.length; i++) {
 		        	String value1 = tAllRows[i].getText(1);
 		          
 		        	for (int j = 0; j < i; j++) {
 		        		String value2 = tAllRows[j].getText(1);
 		            
 		        		if (collator.compare(value1, value2) < 0) {
 							// copy table row data
 							String[] tRowData = { tAllRows[i].getText(0), tAllRows[i].getText(1) };
 							  
 							// delete table row "i"
 							tAllRows[i].dispose();
 							  
 							// create new table row
 							TableItem tRow = new TableItem(mTableRoutingTable, SWT.NONE, j);
 							tRow.setText(tRowData);
 							  
 							// update data of table rows
 							tAllRows = mTableRoutingTable.getItems();
 							  
 							break;
 		        		}
 		        	}
 		        }
 			}
 	    });
 	}
 
 	/**
 	 * Draws GUI elements for depicting coordinator information.
 	 * 
 	 * @param pParent the parent GUI container
 	 * @param pCoordinator selected coordinator 
 	 */
 	private void printCoordinator(Composite pParent, Coordinator pCoordinator)
 	{
 		if (HRM_VIEWER_DEBUGGING)
 			Logging.log(this, "Printing coordinator \"" + pCoordinator.toString() +"\"");
 
 		/**
 		 * GUI part 0: name of the coordinator 
 		 */
 		printNAME(pParent, pCoordinator);
 
 		/**
 		 * GUI part 1: tool box 
 		 */
 		if(pCoordinator != null) {
 			ToolBar tToolbar = new ToolBar(pParent, SWT.NONE);
 
 			if (HRM_VIEWER_SHOW_SINGLE_ENTITY_CLUSTERING_CONTROLS){
 				if(!pCoordinator.getHierarchyLevel().isHighest()) {
 					if ((pCoordinator.getCluster().getElector() != null) && (pCoordinator.getCluster().getElector().isWinner()) && (!pCoordinator.isClustered())){
 					    ToolItem toolItem3 = new ToolItem(tToolbar, SWT.PUSH);
 					    toolItem3.setText("[Create cluster]");
 					    toolItem3.addListener(SWT.Selection, new ListenerClusterHierarchy(this, pCoordinator));
 					}
 				}
 			}
 
 		    ToolItem toolItem4 = new ToolItem(tToolbar, SWT.PUSH);
 		    toolItem4.setText("[Create all level " + pCoordinator.getHierarchyLevel().getValue() + " clusters]");
 		    toolItem4.addListener(SWT.Selection, new ListenerClusterHierarchyLevel(this, pCoordinator));
 		    
 		    tToolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
 		}
 		
 		/**
 		 * GUI part 2: table about comm. channels 
 		 */
 		if(!pCoordinator.getHierarchyLevel().isHighest()){
 			printComChannels(pParent, pCoordinator);
 		}
 			
 		Label separator = new Label (pParent, SWT.SEPARATOR | SWT.HORIZONTAL);
 		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
 		separator.setVisible(true);
 		
 	}
 	
 	private void printComChannels(Composite pParent, final ControlEntity pControlEntity)
 	{
 	    // create the container
 		Composite tContainerComChannels = new Composite(pParent, SWT.NONE);
 	    GridData tLayoutDataMappingTable = new GridData(SWT.FILL, SWT.FILL, true, true);
 	    tLayoutDataMappingTable.horizontalSpan = 1;
 	    tContainerComChannels.setLayoutData(tLayoutDataMappingTable); 
 
 		final Table tTable = new Table(tContainerComChannels, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
 		
 		/**
 		 * The table header
 		 */
 		TableColumn tColumnPeer = new TableColumn(tTable, SWT.NONE, 0);
 		tColumnPeer.setText("Peer");
 		
 		TableColumn tColumnPeerNode = new TableColumn(tTable, SWT.NONE, 1);
 		tColumnPeerNode.setText("Peer Node");
 		
 		TableColumn tColumnActiveLink = new TableColumn(tTable, SWT.NONE, 2);
 		tColumnActiveLink.setText("Active Link");
 		
 		TableColumn tColumnPeerPriority = new TableColumn(tTable, SWT.NONE, 3);
 		tColumnPeerPriority.setText("Peer Priority");
 		
 		TableColumn tColumnPeerEP = new TableColumn(tTable, SWT.NONE, 4);
 		tColumnPeerEP.setText("Peer End Point");
 		
 		TableColumn tColumnRoute = new TableColumn(tTable, SWT.NONE, 5);
 		tColumnRoute.setText("Route to peer");
 		
 		TableColumn tColumnPeerHRMID = new TableColumn(tTable, SWT.NONE, 6);
 		tColumnPeerHRMID.setText("Peer HRMID");
 
 		TableColumn tColumnDirection = new TableColumn(tTable, SWT.NONE, 7);
 		tColumnDirection.setText("Connected");
 
 		TableColumn tColumnSendPackets = new TableColumn(tTable, SWT.NONE, 8);
 		tColumnSendPackets.setText("Sent packets");
 
 		TableColumn tColumnReceivedPackets = new TableColumn(tTable, SWT.NONE, 9);
 		tColumnReceivedPackets.setText("Received packets");
 
 		TableColumn tColumnState = new TableColumn(tTable, SWT.NONE, 10);
 		tColumnState.setText("State");
 
 		TableColumn tColumnParentHRMID = new TableColumn(tTable, SWT.NONE, 11);
 		tColumnParentHRMID.setText("Parent HRMID");
 
 		TableColumn tColumnQueue = new TableColumn(tTable, SWT.NONE, 12);
 		tColumnQueue.setText("Queue");
 
 		tTable.setHeaderVisible(true);
 		tTable.setLinesVisible(true);
 		
 		/**
 		 * The table content
 		 */
 		LinkedList<ComChannel> tComChannels = pControlEntity.getComChannels();
 		if(pControlEntity instanceof Coordinator){
 			Coordinator tCoordinator = (Coordinator)pControlEntity;
 			
 			tComChannels = tCoordinator.getClusterMembershipComChannels();
 		}
 		
 		int j = 0;		
 		if (HRM_VIEWER_DEBUGGING)
 			Logging.log(this, "Amount of known comm. channels is " + tComChannels.size());
 		for(ComChannel tComChannel : tComChannels) {
 			if (HRM_VIEWER_DEBUGGING)
 				Logging.log(this, "Updating table item number " + j);
 			
 			// table row
 			TableItem tRow = null;
 			
 			// get reference to already existing table row
 			if (tTable.getItemCount() > j) {
 				tRow = tTable.getItem(j);
 			}				
 			
 			// create a table row if necessary
 			if (tRow == null){
 				tRow = new TableItem(tTable, SWT.NONE, j);
 			}
 			
 			/**
 			 * Column 0: peer
 			 */
 			String tIsSuperiorCoordinatorChannel = "";
 			String tIsActiveCluster = "";
 			if(pControlEntity instanceof Coordinator){
 				Coordinator tCoordinator = (Coordinator)pControlEntity;
 				if(tComChannel == tCoordinator.superiorCoordinatorComChannel()){
 					tIsSuperiorCoordinatorChannel = "*";
 				}
 				CoordinatorAsClusterMember tCoordinatorAsClusterMember =  (CoordinatorAsClusterMember) tComChannel.getParent();
 				if(tCoordinatorAsClusterMember.isActiveCluster()){
 					tIsActiveCluster = "+";
 				}
 			}
 			tRow.setText(0, tIsSuperiorCoordinatorChannel + tIsActiveCluster + ((tComChannel.getPeer() != null) ? tComChannel.getPeer().toString() : tComChannel.getRemoteClusterName().toString()));
 
 			/**
 			 * Column 1: peer node
 			 */
 			tRow.setText(1, (tComChannel.getPeerL2Address() != null ? tComChannel.getPeerL2Address().toString() : "??"));
 
 			/**
 			 * Column 2: active link 
 			 */
 			tRow.setText(2, tComChannel.isLinkActive() ? "yes" : "no");
 			
 			/**
 			 * Column 3:  
 			 */
 			tRow.setText(3, Float.toString(tComChannel.getPeerPriority().getValue()));
 			
 			/**
 			 * Column 4:  
 			 */
 			if (tComChannel.getRemoteClusterName() != null){
 				if ((pControlEntity.getHierarchyLevel().isHigherLevel()) && (pControlEntity instanceof Cluster)){
 					ClusterName tRemoteClusterName = tComChannel.getRemoteClusterName();
 					int tPeerLevel = tRemoteClusterName.getHierarchyLevel().getValue() - 1 /* the connected remote entity is always one level below because it is reported with an increment */;
 					tRow.setText(4, "Coordinator" + tRemoteClusterName.getGUICoordinatorID() + "@" + (tPeerLevel) + "(Cluster" + tRemoteClusterName.getGUIClusterID() + "@" + tPeerLevel + ")");
 				}else{
 					tRow.setText(4, tComChannel.getRemoteClusterName().toString());
 				}
 			}else{
 				tRow.setText(4, "??");
 			}
 			
 			/**
 			 * Column 5:  
 			 */
 			if (tComChannel.getRouteToPeer() != null){
 				tRow.setText(5, tComChannel.getRouteToPeer().toString());
 			}else{
 				tRow.setText(5, "??");
 			}
 			
 			/**
 			 * Column 6:  
 			 */
 			HRMID tPeerHRMID = tComChannel.getPeerHRMID();
 			tRow.setText(6, (tPeerHRMID != null ? tPeerHRMID.toString() : "undef."));
 
 			/**
 			 * Column 7:  
 			 */
 			tRow.setText(7, tComChannel.getDirection().toString());
 
 			/**
 			 * Column 8:  
 			 */
 			tRow.setText(8, Integer.toString(tComChannel.countSentPackets()));
 
 			/**
 			 * Column 9:  
 			 */
 			tRow.setText(9, Integer.toString(tComChannel.countReceivedPackets()));
 
 			/**
 			 * Column 10:  
 			 */
 			tRow.setText(10, tComChannel.getState().toString());
 
 			/**
 			 * Column 11:  
 			 */
 			HRMID tParentHRMID = tComChannel.getParent().getHRMID();
 			tRow.setText(11, (tParentHRMID != null ? tParentHRMID.toString() : "undef."));
 
 			/**
 			 * Column 12:  
 			 */
 			tRow.setText(12, Integer.toString(tComChannel.getPacketQueue().size()));
 
 			j++;
 		}
 		
 		TableColumn[] cols = tTable.getColumns();
 		for(int k=0; k < cols.length; k++) cols[k].pack();
 		tTable.setLayoutData(new GridData(GridData.FILL_BOTH));
 		
 		// create the container layout
 		TableColumnLayout tLayoutMappingTable = new TableColumnLayout();
 		tContainerComChannels.setLayout(tLayoutMappingTable);
 		// assign each column a layout weight
 		tLayoutMappingTable.setColumnData(tColumnPeer, new ColumnWeightData(3));
 		tLayoutMappingTable.setColumnData(tColumnPeerNode, new ColumnWeightData(3));
 		tLayoutMappingTable.setColumnData(tColumnActiveLink, new ColumnWeightData(1));
 		tLayoutMappingTable.setColumnData(tColumnPeerPriority, new ColumnWeightData(1));
 		tLayoutMappingTable.setColumnData(tColumnPeerEP, new ColumnWeightData(3));
 		tLayoutMappingTable.setColumnData(tColumnRoute, new ColumnWeightData(3));
 		tLayoutMappingTable.setColumnData(tColumnPeerHRMID, new ColumnWeightData(1));
 		tLayoutMappingTable.setColumnData(tColumnDirection, new ColumnWeightData(1));
 		tLayoutMappingTable.setColumnData(tColumnSendPackets, new ColumnWeightData(1));		
 		tLayoutMappingTable.setColumnData(tColumnReceivedPackets, new ColumnWeightData(1));		
 		tLayoutMappingTable.setColumnData(tColumnState, new ColumnWeightData(1));		
 		tLayoutMappingTable.setColumnData(tColumnParentHRMID, new ColumnWeightData(1));		
 		tLayoutMappingTable.setColumnData(tColumnQueue, new ColumnWeightData(1));		
 		
 		/**
 		 * The table context menu
 		 */
 		final LinkedList<ComChannel> tfComChannels = tComChannels;
 		tTable.addMenuDetectListener(new MenuDetectListener()
 		{
 			@Override
 			public void menuDetected(MenuDetectEvent pEvent)
 			{
 				final int tSelectedIndex = tTable.getSelectionIndex();
 				// was there a row selected?
 				if (tSelectedIndex != -1){
 					// identify which row was clicked.
 					TableItem tSelectedRow = tTable.getItem(tSelectedIndex);
 					tSelectedRow.getData();
 				
 					//Logging.log(this, "Context menu for comm. channels of entity: " + pControlEntity + ", index: " + tSelectedIndex + ", row data: " + tSelectedRow);
 					
 					/**
 					 * Create the context menu
 					 */
 					Menu tMenu = new Menu(tTable);
 					MenuItem tMenuItem = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem.setText("Show packet I/O");
 					tMenuItem.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showPackets(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showPackets(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem1 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem1.setText("Show link activation events");
 					tMenuItem1.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showLinkActivationEvents(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showLinkActivationEvents(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem2 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem2.setText("Show session");
 					tMenuItem2.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showSession(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showSession(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem3 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem3.setText("Show peer HRMIDs");
 					tMenuItem3.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showPeerHRMIDs(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showPeerHRMIDs(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem4 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem4.setText("Show assigned peer HRMIDs");
 					tMenuItem4.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showAssignedPeerHRMIDs(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showAssignedPeerHRMIDs(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem5 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem5.setText("Show packet queue");
 					tMenuItem5.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showPacketQueue(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showPacketQueue(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem6 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem6.setText("Show shared routing table");
 					tMenuItem6.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showSharedRoutes(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showSharedRoutes(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					MenuItem tMenuItem7 = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem7.setText("Show reported routing table");
 					tMenuItem7.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showReportedRoutes(tfComChannels.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showReportedRoutes(tfComChannels.get(tSelectedIndex));
 						}
 					});
 					
 					tTable.setMenu(tMenu);
 				}
 			}
 		});
 	}
 		
 	private void showAssignedPeerHRMIDs(ComChannel pComChannel)
 	{
 		Logging.log(this, "Assigned peer HRMIDs of: " + pComChannel);
 		int i = 0;
 		for (HRMID tHRMID : pComChannel.getAssignedPeerHRMIDs()){
 			Logging.log(this, "    ..[" + i + "]: " + tHRMID);
 			i++;
 		}
 	}
 	
 	private void showPacketQueue(ComChannel pComChannel)
 	{
 		Logging.log(this, "Remaining packets in queue of: " + pComChannel);
 		int i = 0;
 		for (SignalingMessageHrm tPacket : pComChannel.getPacketQueue()){
 			Logging.log(this, "    ..[" + i + "]: " + tPacket);
 			i++;
 		}
 	}
 
 	private void showSharedRoutes(ComChannel pComChannel)
 	{
 		Logging.log(this, "Shared routing table received via: " + pComChannel);
 		int i = 0;
 		for (RoutingEntry tEntry : pComChannel.getSharedRoutingTable()){
 			Logging.log(this, "    ..[" + i + "]: " + tEntry);
 			Logging.log(this, "    ..cause:");
 			int j = 0;
 			for (String tCauseString : tEntry.getCause()){
 				Logging.trace(this, "      ..[" + j + "]: " + tCauseString);
 				j++;
 			}		
 			i++;
 		}
 	}
 
 	private void showReportedRoutes(ComChannel pComChannel)
 	{
 		Logging.log(this, "Reported routing table received via: " + pComChannel);
 		int i = 0;
 		for (RoutingEntry tEntry : pComChannel.getReportedRoutingTable()){
 			Logging.log(this, "    ..[" + i + "]: " + tEntry);
 			Logging.log(this, "    ..cause:");
 			int j = 0;
 			for (String tCauseString : tEntry.getCause()){
 				Logging.trace(this, "      ..[" + j + "]: " + tCauseString);
 				j++;
 			}		
 			i++;
 		}
 	}
 
 	private void showPeerHRMIDs(ComChannel pComChannel)
 	{
 		Logging.log(this, "Peer HRMIDs of: " + pComChannel);
 		int i = 0;
 		for (HRMID tHRMID : pComChannel.getPeerHRMIDs()){
 			Logging.log(this, "    ..[" + i + "]: " + tHRMID);
 			i++;
 		}
 	}
 	
 	private void showSession(ComChannel pComChannel)
 	{
 		Logging.log(this, "Session for: " + pComChannel);
 		Logging.log(this, "     ..session: " + pComChannel.getParentComSession());
 		LinkedList<ComChannel> tChannels = pComChannel.getParentComSession().getAllComChannels();
 		for(ComChannel tComChannel : tChannels){
 			Logging.log(this, "       ..channel: " + tComChannel);
 		}
 		tChannels = pComChannel.getParentComSession().getAllFormerChannels();
 		for(ComChannel tComChannel : tChannels){
 			Logging.log(this, "       ..former channel: " + tComChannel);
 		}
 	}
 	
 	private void showPackets(ComChannel pComChannel)
 	{
 		Logging.log(this, "Packet I/O for: " + pComChannel);
 		LinkedList<ComChannelPacketMetaData> tPacketsMetaData = pComChannel.getSeenPackets();
 		if(tPacketsMetaData != null){
 			int i = 0;
 			for (ComChannelPacketMetaData tPacketMetaData: tPacketsMetaData){
 				if(tPacketMetaData.getPacket() instanceof AnnounceCoordinator){
 					AnnounceCoordinator tAnnounceCoordinatorPacket = (AnnounceCoordinator)tPacketMetaData.getPacket();
 	
 					Logging.log(this, "     ..[" + i + "] (" + (tPacketMetaData.wasSent() ? "S" : "R") + " @ " + tPacketMetaData.getTimetstamp() + "): " + tPacketMetaData.getPacket() + ", passed clusters: " + tAnnounceCoordinatorPacket.getGUIPassedClusters()+ ", passed nodes: " + tAnnounceCoordinatorPacket.getPassedNodes());
 				}else{
 					Logging.log(this, "     ..[" + i + "] (" + (tPacketMetaData.wasSent() ? "S" : "R") + " @ " + tPacketMetaData.getTimetstamp() + "): " + tPacketMetaData.getPacket());
 				}
 				i++;
 			}
 		}		
 	}
 	
 	private void showLinkActivationEvents(ComChannel pComChannel)
 	{
 		Logging.log(this, "Link activation events for: " + pComChannel + pComChannel.getDescriptionLinkActivation());
 	}
 
 	private void showRoutingEntryCause(RoutingEntry pRoutingEntry)
 	{
 		Logging.log(this, "Cause for: " + pRoutingEntry);
 		int i = 0;
 		for (String tCauseString : pRoutingEntry.getCause()){
 			Logging.trace(this, "    ..[" + i + "]: " + tCauseString);
 			i++;
 		}		
 	}
 
 	private void printNAME(Composite pParent, ControlEntity pEntity)
 	{
 		Composite tContainerName = new Composite(pParent, SWT.NONE);
 		GridLayout tLayout1 = new GridLayout(2, false);
 		tContainerName.setLayout(tLayout1);
 
 		/**
 		 * name part 0: front image
 		 */
 		if(pEntity instanceof Coordinator){
 			String tImagePath = "";
 			try {
 				tImagePath = Resources.locateInPlugin(PLUGIN_ID, PATH_ICONS, "Coordinator_Level" + Integer.toString(pEntity.getHierarchyLevel().getValue()) + ".gif");
 			} catch (FileNotFoundException e) {
 				//ignore
 			}
 			//Logging.log(this, "Loading front image: " + tImagePath);		
 			Image tFrontImage = new Image(mDisplay, tImagePath);
 			Label tFronImageLabel = new Label(tContainerName, SWT.NONE);
 			tFronImageLabel.setSize(16, 20);
 			tFronImageLabel.setImage(tFrontImage);
 
 			tFronImageLabel.setLayoutData(createGridData(true, 1));
 		}
 		if(pEntity instanceof Cluster){
 			String tImagePath = "";
 			try {
 				tImagePath = Resources.locateInPlugin(PLUGIN_ID, PATH_ICONS, "Cluster_Level" + Integer.toString(pEntity.getHierarchyLevel().getValue()) + ".png");
 			} catch (FileNotFoundException e) {
 				//ignore
 			}
 			//Logging.log(this, "Loading front image: " + tImagePath);		
 			Image tFrontImage = new Image(mDisplay, tImagePath);
 			Label tFronImageLabel = new Label(tContainerName, SWT.NONE);
 			tFronImageLabel.setSize(45, 20);
 			tFronImageLabel.setImage(tFrontImage);
 
 			tFronImageLabel.setLayoutData(createGridData(true, 1));
 		}
 
 		/**
 		 * name part 1: the name
 		 */
 		StyledText tClusterLabel = new StyledText(tContainerName, SWT.BORDER);;
 		tClusterLabel.setForeground(new Color(mShell.getDisplay(), 0, 0, 0));
 		boolean tClusterHeadWithoutCoordinator = false;
 		boolean tClusterMemberOfInactiveCluster = false;
 		/**
 		 * BACKGROUND COLOR
 		 */
 		if (GUI_SHOW_COLORED_BACKGROUND_FOR_CONTROL_ENTITIES){
 			boolean tBackgroundSet = false;
 			if (pEntity instanceof Cluster){
 				Cluster tCluster =(Cluster) pEntity;
 				if ((tCluster.getElector() != null) && (tCluster.getElector().finished())){
 					if(tCluster.hasLocalCoordinator()){
 						tClusterLabel.setBackground(new Color(mShell.getDisplay(), 111, 222, 111));
 					}else{
 						tClusterHeadWithoutCoordinator = true;
 						tClusterLabel.setBackground(new Color(mShell.getDisplay(), 111, 222, 222));
 					}
 					tBackgroundSet = true;
 				}
 			}else{
 				if(pEntity instanceof ClusterMember){
 					ClusterMember tClusterMember = (ClusterMember)pEntity;
 					
 					tClusterMemberOfInactiveCluster = !tClusterMember.isActiveCluster();
 
 					if(tClusterMemberOfInactiveCluster){
 						tClusterLabel.setBackground(new Color(mShell.getDisplay(), 111, 222, 222));
 					}else{
 						tClusterLabel.setBackground(new Color(mShell.getDisplay(), 151, 222, 151));
 					}
 
 					tBackgroundSet = true;
 				}
 			}
 			if (pEntity instanceof Coordinator){
 				Coordinator tCoordinator =(Coordinator) pEntity;
 				if ((tCoordinator.isClustered()) || (tCoordinator.getHierarchyLevel().isHighest())){
 					tClusterLabel.setBackground(new Color(mShell.getDisplay(), 111, 222, 111));
 					tBackgroundSet = true;
 				}
 			}
 			if (!tBackgroundSet){
 				tClusterLabel.setBackground(new Color(mShell.getDisplay(), 222, 111, 111));
 			}
 		}else{
 			tClusterLabel.setBackground(new Color(mShell.getDisplay(), 222, 222, 222));
 		}
 		/**
 		 * TEXT
 		 */
 		String tFormerHRMIDs = (pEntity.getDescriptionFormerHRMIDs() != "" ? " (FormerHRMIDs: " + pEntity.getDescriptionFormerHRMIDs() + ")" : "");
 		String tNetworkInterface = "";
 		String tL0HRMID = "";
 		if (pEntity instanceof ClusterMember){
 			ClusterMember tClusterMember = (ClusterMember) pEntity;
 			tNetworkInterface = (tClusterMember.getBaseHierarchyLevelNetworkInterface() != null ? "  (" + tClusterMember.getBaseHierarchyLevelNetworkInterface().toString() + ")": "");
 			
 			if(tClusterMember.getL0HRMID() != null){
 				tL0HRMID = "  (L0-HRMID: " + tClusterMember.getL0HRMID().toString() + ")";
 			}
 		}
 		
 		if (pEntity instanceof Cluster){
 			Cluster tCluster = (Cluster) pEntity;
 			boolean tClusterCanBeActive = tCluster.getElector().isAllowedToWin();
 			tClusterLabel.setText(pEntity.toString() + "  Priority=" + pEntity.getPriority().getValue() + "  UniqueID=" + tCluster.getClusterID() + " Election=" + tCluster.getElector().getElectionStateStr() + (tClusterHeadWithoutCoordinator ? " (inactive cluster)" : "") + (!tClusterCanBeActive ? " [ZOMBIE]" : "") + (tCluster.getDescriptionFormerGUICoordinatorIDs() != "" ? " (Former Coordinators=" + tCluster.getDescriptionFormerGUICoordinatorIDs() + ")" : "") + tFormerHRMIDs + tNetworkInterface + tL0HRMID);
 		}else{
 			if(pEntity instanceof Coordinator){
 				Coordinator tCoordinator = (Coordinator)pEntity;
 				
 				tClusterLabel.setText(pEntity.toString() + "  Priority=" + pEntity.getPriority().getValue() + tFormerHRMIDs + " Announces=" + tCoordinator.countAnnounces() + " AddressBroadcasts=" + tCoordinator.getCluster().countAddressBroadcasts());
 			}else{
 				tClusterLabel.setText(pEntity.toString() + "  Priority=" + pEntity.getPriority().getValue() + tFormerHRMIDs + (tClusterMemberOfInactiveCluster ? "   (inactive cluster)" : "") + tNetworkInterface + tL0HRMID);
 			}
 		}
 		/**
 		 * STYLE/LAYOUT
 		 */
 	    StyleRange style1 = new StyleRange();
 	    style1.start = 0;
 	    style1.length = tClusterLabel.getText().length();
 	    style1.fontStyle = SWT.BOLD;
 	    tClusterLabel.setStyleRange(style1);
 		if((pEntity instanceof Coordinator) || (pEntity instanceof Cluster)){
 			tClusterLabel.setLayoutData(createGridData(true, 1));
 		}else{
 			tClusterLabel.setLayoutData(createGridData(true, 2));
 		}
 
 	}
 	
 	/**
 	 * Draws GUI elements for depicting cluster information.
 	 * 
 	 * @param pCluster selected cluster 
 	 */
 	private void printClusterMember(Composite pParent, ClusterMember pClusterMember)
 	{
 		// on which hierarchy level are we?
 		int tHierarchyLevel = pClusterMember.getHierarchyLevel().getValue();
 
 		if (HRM_VIEWER_DEBUGGING)
 			Logging.log(this, "Printing cluster (member) \"" + pClusterMember.toString() +"\"");
 
 		/**
 		 * GUI part 0: name of the cluster 
 		 */
 		printNAME(pParent, pClusterMember);
 		
 		/**
 		 * GUI part 1: tool box 
 		 */
 		if(pClusterMember != null) {
 			ToolBar tToolbar = new ToolBar(pParent, SWT.NONE);
 
 			if (HRM_VIEWER_SHOW_SINGLE_ENTITY_ELECTION_CONTROLS){
 				if ((pClusterMember.getElector() != null) && (!pClusterMember.getElector().finished())){
 					ToolItem toolItem1 = new ToolItem(tToolbar, SWT.PUSH);
 				    toolItem1.setText("[Elect coordinator]");
 				    toolItem1.addListener(SWT.Selection, new ListenerElectCoordinator(this, pClusterMember));
 				}
 			}
 
 			ToolItem toolItem2 = new ToolItem(tToolbar, SWT.PUSH);
 		    toolItem2.setText("[Elect all level " + tHierarchyLevel + " coordinators]");
 		    toolItem2.addListener(SWT.Selection, new ListenerElectHierarchyLevelCoordinators(this, pClusterMember));
 		    
 		    tToolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
 		}
 		
 		/**
 		 * GUI part 2: table about comm. channels 
 		 */
 		printComChannels(pParent, pClusterMember);
 	
 		Label separator = new Label (pParent, SWT.SEPARATOR | SWT.HORIZONTAL);
 		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
 		separator.setVisible(true);
 	}
 
 	@SuppressWarnings("deprecation")
 	@Override
 	public void init(IEditorSite pSite, IEditorInput pInput) throws PartInitException
 	{
 		setSite(pSite);
 		setInput(pInput);
 		
 		// get selected object to show in editor
 		Object tInputObject;
 		if(pInput instanceof EditorInput) {
 			tInputObject = ((EditorInput) pInput).getObj();
 		} else {
 			tInputObject = null;
 		}
 		if (HRM_VIEWER_DEBUGGING)
 			Logging.log(this, "Initiating HRM viewer " + tInputObject + " (class=" + tInputObject.getClass() +")");
 		
 		if(tInputObject != null) {
 			// update title of editor
 			setTitle(tInputObject.toString());
 
 			if(tInputObject instanceof HRMController) {
 				mHRMController = (HRMController) tInputObject;				
 			} else {
 				throw new PartInitException("Invalid input object " +tInputObject +". Bus expected.");
 			}
 			
 			// update name of editor part
 			setPartName(toString());
 			
 		} else {
 			throw new PartInitException("No input for editor.");
 		}
 		
 		// register this GUI at the corresponding HRMController
 		if (mHRMController != null){
 			mHRMController.registerGUI(this);
 		}
 	}
 	
 	/**
 	 * overloaded dispose() function for unregistering from the HRMController instance
 	 */
 	public void dispose()
 	{
 		// unregister this GUI at the corresponding HRMController
 		if (mHRMController != null){
 			mHRMController.unregisterGUI(this);
 		}
 		
 		// call the original implementation
 		super.dispose();
 	}
 
 	@Override
 	public void doSave(IProgressMonitor arg0)
 	{
 	}
 
 	@Override
 	public void doSaveAs()
 	{
 	}
 
 	@Override
 	public boolean isDirty()
 	{
 		return false;
 	}
 
 	@Override
 	public boolean isSaveAsAllowed()
 	{
 		return false;
 	}
 
 	@Override
 	public void setFocus()
 	{
 	}
 
 	@Override
 	public Object getAdapter(Class required)
 	{
 		if(getClass().equals(required)) return this;
 		
 		Object res = super.getAdapter(required);
 		
 		if(res == null) {
 			res = Platform.getAdapterManager().getAdapter(this, required);
 			
 			if(res == null)	res = Platform.getAdapterManager().getAdapter(mHRMController, required);
 		}
 		
 		return res;
 	}
 
 	/**
 	 * Thread main function, which is used if an asynchronous GUI update is needed.
 	 * In this case, the GUI update has to be delayed in order to do it within the main GUI thread.
 	 */
 	@Override
 	public void run()
 	{
 		resetGUI();
 	}
 
 	/**
 	 * Destroys the content of the routing table
 	 */
 	private void updateRoutingTable()
 	{
		int tOldSelectedIndex = mTableRoutingTable.getSelectionIndex();

 	    mTableRoutingTable.removeAll();
 	    
 		RoutingTable tRoutingTable = mHRMController.getHRS().getRoutingTable();
 		if (HRM_VIEWER_DEBUGGING){
 			Logging.log(this, "Found " + tRoutingTable.size() + " entries in the local routing table");
 		}
 			
 	    Color tColLoop = new Color(mDisplay, 210, 210, 250);
 	    Color tColNeighbor = new Color(mDisplay, 210, 250, 210);
 	    Color tColGeneral = new Color(mDisplay, 250, 210, 210);
 	    
 		if ((tRoutingTable != null) && (!tRoutingTable.isEmpty())) {
 			int tRowNumber = 0;
 			for(RoutingEntry tEntry : tRoutingTable) {
 				if ((HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES) || (tEntry.getDest() == null) || (!tEntry.getDest().isRelativeAddress())){
 					// create the table row
 					TableItem tTableRow = new TableItem(mTableRoutingTable, SWT.NONE, tRowNumber);
 					
 					/**
 					 * Column 0: destination
 					 */
 					tTableRow.setText(0, tEntry.getDest() != null ? tEntry.getDest().toString() : "undef.");
 	
 					/**
 					 * Column 1: next hop 
 					 */
 					if (tEntry.getNextHop() != null) {
 						tTableRow.setText(1, tEntry.getNextHop().toString());
 					}else{
 						tTableRow.setText(1, "??");
 					}
 					
 					/**
 					 * Column 2: hop costs
 					 */
 					if (tEntry.getHopCount() != RoutingEntry.NO_HOP_COSTS){
 						tTableRow.setText(2, Integer.toString(tEntry.getHopCount()));
 					}else{
 						tTableRow.setText(2, "none");
 					}
 					
 					/**
 					 * Column 3:  utilization
 					 */
 					if (tEntry.getUtilization() != RoutingEntry.NO_UTILIZATION){
 						tTableRow.setText(3,  Float.toString(tEntry.getUtilization() * 100));
 					}else{
 						tTableRow.setText(3, "N/A");
 					}
 					
 					/**
 					 * Column 4: min. delay
 					 */
 					if (tEntry.getMinDelay() != RoutingEntry.NO_DELAY){					
 						tTableRow.setText(4, Long.toString(tEntry.getMinDelay()));
 					}else{
 						tTableRow.setText(4, "none");
 					}
 					
 					/**
 					 * Column 5: max. data rate
 					 */
 					if (tEntry.getMaxDataRate() != RoutingEntry.INFINITE_DATARATE){
 						tTableRow.setText(5, Long.toString(tEntry.getMaxDataRate()));				
 					}else{
 						tTableRow.setText(5, "inf.");
 					}
 					
 					/**
 					 * Column 6: loopback?
 					 */
 					if (tEntry.isLocalLoop()){
 						tTableRow.setText(6, "yes");				
 					}else{
 						tTableRow.setText(6, "no");
 					}
 	
 					/**
 					 * Column 7: direct neighbor?
 					 */
 					if (tEntry.isRouteToDirectNeighbor()){
 						tTableRow.setText(7, "yes");				
 					}else{
 						tTableRow.setText(7, "no");
 					}
 	
 					/**
 					 * Column 8: source 
 					 */
 					if (tEntry.getSource() != null) {
 						tTableRow.setText(8, tEntry.getSource().toString());
 					}else{
 						tTableRow.setText(8, "??");
 					}
 
 					/**
 					 * Column 9: next hop L2Address
 					 */
 					if (tEntry.getNextHopL2Address() != null) {
 						tTableRow.setText(9, tEntry.getNextHopL2Address().toString());
 					}else{
 						tTableRow.setText(9, "??");
 					}
 
 					/**
 					 * Column 10: timeout
 					 */
 					if (tEntry.getTimeout() > 0) {
 						tTableRow.setText(10, Double.toString(tEntry.getTimeout()));
 					}else{
 						tTableRow.setText(10, "none");
 					}
 
 					/**
 					 * Cells coloring
 					 */
 					for(int i = 0; i < 11; i++){
 						if(tEntry.isLocalLoop()){
 							tTableRow.setBackground(i, tColLoop);
 						}else if (tEntry.isRouteToDirectNeighbor()){
 							tTableRow.setBackground(i, tColNeighbor);
 						}else{
 							tTableRow.setBackground(i, tColGeneral);
 						}
 					}
 					tRowNumber++;
 				}
 			}
			mTableRoutingTable.setItemCount(tRowNumber);
 		}
 		
		if(tOldSelectedIndex > 0){
			mTableRoutingTable.select(tOldSelectedIndex);
		}
			
 		/**
 		 * The table context menu
 		 */
 		final RoutingTable tfRoutingTable = tRoutingTable;
 		mTableRoutingTable.addMenuDetectListener(new MenuDetectListener()
 		{
 			@Override
 			public void menuDetected(MenuDetectEvent pEvent)
 			{
 				final int tSelectedIndex = mTableRoutingTable.getSelectionIndex();
 				// was there a row selected?
 				if (tSelectedIndex != -1){
 					// identify which row was clicked.
 					TableItem tSelectedRow = mTableRoutingTable.getItem(tSelectedIndex);
 					tSelectedRow.getData();
 				
 					//Logging.log(this, "Context menu for comm. channels of entity: " + pControlEntity + ", index: " + tSelectedIndex + ", row data: " + tSelectedRow);
 					
 					/**
 					 * Create the context menu
 					 */
 					Menu tMenu = new Menu(mTableRoutingTable);
 					MenuItem tMenuItem = new MenuItem(tMenu, SWT.NONE);
 					tMenuItem.setText("Show cause for this entry");
 					tMenuItem.addSelectionListener(new SelectionListener() {
 						public void widgetDefaultSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Default selected: " + pEvent);
 							showRoutingEntryCause(tfRoutingTable.get(tSelectedIndex));
 						}
 						public void widgetSelected(SelectionEvent pEvent)
 						{
 							//Logging.log(this, "Widget selected: " + pEvent);
 							showRoutingEntryCause(tfRoutingTable.get(tSelectedIndex));
 						}
 					});
 
 					mTableRoutingTable.setMenu(tMenu);
 				}
 			}
 		});
 	}
 
 	/**
 	 * Resets the routing table and updates it
 	 */
 	private synchronized void redrawRoutingTable()
 	{
 		updateRoutingTable();
 		
 		mNextGUIUpdateResetsOnlyRoutingTable = false;
 	}
 	
 	private synchronized void redrawGUI()
 	{
 		if(!mScroller.isDisposed()){
 			Point tOldScrollPosition = mScroller.getOrigin();
 			
 			destroyPartControl();
 			
 			createPartControl(mShell);
 			
 			mScroller.setOrigin(tOldScrollPosition);
 		}else{
 			Logging.warn(this, "Scroller is already disposed");
 		}
 	}
 	
 	/**
 	 * Resets the GUI and updates everything in this EditorPart
 	 */
 	private void resetGUI()
 	{
 		// store the time of this (last) GUI update
 		mTimestampLastGUIUpdate = mHRMController.getSimulationTime();
 
 		// reset stored GUI update time
 		mTimeNextGUIUpdate = 0;
 
 		if(!mDisplay.isDisposed()) {
 			if(Thread.currentThread() != mDisplay.getThread()) {
 				//switches to different thread
 				mDisplay.asyncExec(this);
 			} else {
 				if (mNextGUIUpdateResetsOnlyRoutingTable){
 					redrawRoutingTable();
 				}else{
 					redrawGUI();
 				}
 			}
 		}
 	}
 	
 	/**
 	 * Function for receiving notifications about changes in the corresponding HRMController instance
 	 */
 	@Override
 	public void update(Observable pSource, Object pReason)
 	{
 		if (HRMConfig.DebugOutput.GUI_SHOW_NOTIFICATIONS){
 			if(pReason instanceof RoutingEntry){
 				RoutingEntry tEntry = (RoutingEntry)pReason;
 				Logging.log(this, "Got notification from " + pSource + " because of:");
 				Logging.log(this, "   ..entry: \"" + tEntry + "\"");
 				Logging.log(this, "   ..cause: " + tEntry.getCause());
 			}else{
 				Logging.log(this, "Got notification from " + pSource + " because of \"" + pReason + "\"");
 			}
 		}
 
 		if(pReason instanceof RoutingEntry){
 			startRoutingTableUpdateTimer();
 		}else{
 			startGUIUpdateTimer();
 		}
 	}
 	
 	private synchronized void startRoutingTableUpdateTimer()
 	{
 		if (HRMConfig.DebugOutput.GUI_SHOW_NOTIFICATIONS){
 			Logging.log(this, "Got a routing table update");
 		}
 		
 		mNextGUIUpdateResetsOnlyRoutingTable = true;
 		
 		// trigger GUI update
 		startGUIUpdateTimer();
 	}
 	
 	/**
 	 * Starts the timer for the "update GUI" event.
 	 * If the timer is already started nothing is done.
 	 */
 	private synchronized void startGUIUpdateTimer()
 	{
 		// is a GUI update already planned?
 		if (mTimeNextGUIUpdate == 0){
 			// when was the last GUI update? is the time period okay for a new update? -> determine a timeout for a new GUI update
 			double tNow = mHRMController.getSimulationTime();
 			double tTimeout = mTimestampLastGUIUpdate.longValue() + HRMConfig.DebugOutput.GUI_NODE_DISPLAY_UPDATE_INTERVAL;
 
 			if ((mTimestampLastGUIUpdate.doubleValue() == 0) || (tNow > tTimeout)){
 				mTimeNextGUIUpdate = tNow;
 
 				// register next trigger
 				mHRMController.getAS().getTimeBase().scheduleIn(0, this);
 			}else{
 				mTimeNextGUIUpdate = tTimeout;
 			
 				// register next trigger
 				mHRMController.getAS().getTimeBase().scheduleIn(tTimeout - tNow, this);
 			}
 
 		}else{
 			// timer is already started, we ignore the repeated request
 		}
 	}
 	
 	/**
 	 * This function is called when the event is fired by the main event system.
 	 */
 	@Override
 	public void fire()
 	{
 		// reset stored GUI update time
 		mTimeNextGUIUpdate = 0;
 		
 		// trigger GUI update
 		resetGUI();
 	}
 
 	/**
 	 * Returns a descriptive string about this object
 	 * 
 	 * @return the descriptive string
 	 */
 	public String toString()
 	{		
 		return "HRM viewer" + (mHRMController != null ? "@" + mHRMController.getNodeGUIName() : "");
 	}
 	
 	/**
 	 * Listener for electing coordinator for this cluster.
 	 */
 	private class ListenerElectCoordinator implements Listener
 	{
 		private ClusterMember ClusterMember = null;
 		private HRMViewer mHRMViewer = null;
 		
 		private ListenerElectCoordinator(HRMViewer pHRMViewer, ClusterMember pClusterMember)
 		{
 			super();
 			ClusterMember = pClusterMember;
 			mHRMViewer = pHRMViewer;
 		}
 		
 		@Override
 		public void handleEvent(Event event)
 		{
 			if (HRM_VIEWER_DEBUGGING){
 				Logging.log(this, "GUI-TRIGGER: Starting election for " + ClusterMember);
 			}
 			
 			// start the election for the selected cluster
 			ClusterMember.getElector().startElection();
 		}
 	
 		public String toString()
 		{
 			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
 		}
 	}
 
 	/**
 	 * Listener for electing coordinators for all clusters on this hierarchy level. 
 	 */
 	private class ListenerElectHierarchyLevelCoordinators implements Listener
 	{
 		private ClusterMember mClusterMember = null;
 		private HRMViewer mHRMViewer = null;
 
 		private ListenerElectHierarchyLevelCoordinators(HRMViewer pHRMViewer, ClusterMember pClusterMember)
 		{
 			super();
 			mClusterMember = pClusterMember;
 			mHRMViewer = pHRMViewer;
 		}
 		
 		@Override
 		public void handleEvent(Event event)
 		{
 			// get the hierarchy level of the selected cluster
 			HierarchyLevel tLocalClusterLevel = mClusterMember.getHierarchyLevel();
 			
 			// iterate over all HRMControllers
 			for(HRMController tHRMController : HRMController.getALLHRMControllers()) {
 				// iterate over all clusters from the current HRMController
 				for (Cluster tCluster: tHRMController.getAllClusters())
 				{
 					// check the hierarchy of the found cluster
 					if (tLocalClusterLevel.equals(tCluster.getHierarchyLevel())){
 						if (HRM_VIEWER_DEBUGGING){
 							Logging.log(this, "GUI-TRIGGER: Starting election for " + tCluster);
 						}
 						
 						// start the election for the found cluster
 						tCluster.getElector().startElection();
 					}
 				}
 			}
 		}
 		
 		public String toString()
 		{
 			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
 		}
 	}
 	
 	/**
 	 * Listener for clustering the network on a defined hierarchy level. 
 	 */
 	private class ListenerClusterHierarchyLevel implements Listener
 	{
 		private Coordinator mCoordinator = null;
 		private HRMViewer mHRMViewer = null;
 		
 		private ListenerClusterHierarchyLevel(HRMViewer pHRMViewer, Coordinator pCoordinator)
 		{
 			super();
 			mCoordinator = pCoordinator;
 			mHRMViewer = pHRMViewer;
 		}
 		
 		@Override
 		public void handleEvent(Event event)
 		{
 			// get the hierarchy level of the selected cluster
 			HierarchyLevel tLocalClusterLevel = mCoordinator.getHierarchyLevel();
 			
 			// check if we are at the max. hierarchy height
 			if(!tLocalClusterLevel.isHighest()) {
 
 				// iterate over all HRMControllers
 				for(HRMController tHRMController : HRMController.getALLHRMControllers()) {
 					tHRMController.cluster(null, new HierarchyLevel(this, tLocalClusterLevel.getValue() + 1));
 				}
 			}else{
 				Logging.err(this, "Maximum hierarchy height " + (tLocalClusterLevel.getValue()) + " is already reached.");
 			}
 		}
 		
 		public String toString()
 		{
 			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
 		}
 	}
 	
 	/**
 	 * Listener for clustering the network, including the current cluster's coordinator and its siblings. 
 	 */
 	private class ListenerClusterHierarchy implements Listener
 	{
 		private Coordinator mCoordinator = null;
 		private HRMViewer mHRMViewer = null;
 		
 		private ListenerClusterHierarchy(HRMViewer pHRMViewer, Coordinator pCoordinator)
 		{
 			super();
 			mCoordinator = pCoordinator;
 			mHRMViewer = pHRMViewer;
 		}
 		
 		@Override
 		public void handleEvent(Event event)
 		{
 			// check if we are at the max. hierarchy height
 			if(!mCoordinator.getHierarchyLevel().isHighest()) {
 				if (mCoordinator != null){
 					if (HRM_VIEWER_DEBUGGING){
 						Logging.log(this, "GUI-TRIGGER: Starting clustering for coordinator " + mCoordinator);
 					}
 					
 					// start the clustering of the selected cluster's coordinator and its neighbors
 					mHRMController.cluster(null, new HierarchyLevel(this, mCoordinator.getHierarchyLevel().getValue() + 1));
 				}else{
 					Logging.err(this, "Coordinator is invalid, skipping clustering request");
 				}
 			}else{
 				Logging.err(this, "Maximum hierarchy height " + (mCoordinator.getHierarchyLevel().getValue()) + " is already reached.");
 			}
 		}		
 		
 		public String toString()
 		{
 			return mHRMViewer.toString() + "@" + getClass().getSimpleName(); 
 		}
 	}	
 }
