 package org.cytoscape.task.internal.table;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.cytoscape.io.read.CyTableReader;
 import org.cytoscape.model.CyColumn;
 import org.cytoscape.model.CyEdge;
 import org.cytoscape.model.CyIdentifiable;
 import org.cytoscape.model.CyNetwork;
 import org.cytoscape.model.CyNetworkManager;
 import org.cytoscape.model.CyNode;
 import org.cytoscape.model.CyTable;
 import org.cytoscape.work.AbstractTask;
 import org.cytoscape.work.ProvidesTitle;
 import org.cytoscape.work.TaskMonitor;
 import org.cytoscape.work.Tunable;
 import org.cytoscape.work.util.ListMultipleSelection;
 import org.cytoscape.work.util.ListSingleSelection;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public final class MapTableToNetworkTablesTask extends AbstractTask {
 	
 	private static enum TableType {
 		NODE_ATTR("Node Attributes", CyNode.class), EDGE_ATTR("Edge Attributes", CyEdge.class), NETWORK_ATTR("Network Attributes", CyNetwork.class);
 
 		private final String name;
 		private final  Class<? extends CyIdentifiable> type;
 		
 		private TableType(final String name, Class<? extends CyIdentifiable> type) {
 			this.name = name;
 			this.type = type;
 		}
 		
 		public Class<? extends CyIdentifiable> getType(){
 			return this.type;
 		}
 		
 		@Override public String toString() {
 			return name;
 		}
 	};
 	
 	private static Logger logger = LoggerFactory.getLogger(MapTableToNetworkTablesTask.class);
	private static String NO_NETWORKS = "No Networks Found";
 	private final CyNetworkManager networkManager;
 	private final CyTable globalTable;
 	private final  CyTableReader reader;
 	private final boolean byReader;
 	private Map<String, CyNetwork> name2NetworkMap;
 	
	@Tunable(description = "Apply to Selected Networks Only",groups="Select Tables", dependsOn="noNetworks=false")
 	public boolean selectedNetworksOnly = false;
 	
 	@Tunable(description = "Network List",groups="Select Tables",dependsOn="selectedNetworksOnly=true")
 	public ListMultipleSelection<String> networkList;
 	
 	@Tunable(description = "Import Data To:")
 	public ListSingleSelection<TableType> dataTypeOptions;
 
 	@ProvidesTitle
 	public String getTitle() {
 		return "Import Data ";
 	}
 
	
 	public MapTableToNetworkTablesTask(final CyNetworkManager networkManager, final CyTableReader reader){
 		this.reader = reader;
 		globalTable = null;
 		this.byReader = true;
 		this.networkManager = networkManager;
 		this.name2NetworkMap = new HashMap<String, CyNetwork>();
 		initTunable(networkManager);
 	}
 	
 	public MapTableToNetworkTablesTask(final CyNetworkManager networkManager, final CyTable globalTable){
 		this.networkManager = networkManager;
 		this.globalTable = globalTable;
 		this.byReader = false;
 		this.reader = null;
 		this.name2NetworkMap = new HashMap<String, CyNetwork>();
 		initTunable(networkManager);
 	}
 	
 	private void initTunable(CyNetworkManager networkManage){
 		final List<TableType> options = new ArrayList<TableType>();
 		for(TableType type: TableType.values())
 			options.add(type);
 		dataTypeOptions = new ListSingleSelection<TableType>(options);
 		dataTypeOptions.setSelectedValue(TableType.NODE_ATTR);
 		
 		for(CyNetwork net: networkManage.getNetworkSet()){
 			String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
 			name2NetworkMap.put(netName, net);
 		}
 		List<String> names = new ArrayList<String>();
 		names.addAll(name2NetworkMap.keySet());
		if(names.isEmpty())
			networkList = new ListMultipleSelection<String>(NO_NETWORKS);
		else
			networkList = new ListMultipleSelection<String>(names);
 			
 	}
 	
 	
 	public void run(TaskMonitor taskMonitor) throws Exception {	
 		TableType tableType = dataTypeOptions.getSelectedValue();
 
 		List<CyNetwork> networks = new ArrayList<CyNetwork>();
 		
 		if (!selectedNetworksOnly)
 			networks.addAll(networkManager.getNetworkSet());
		else{
			if(!networkList.getSelectedValues().get(0).equals(NO_NETWORKS))
				for(String netName: networkList.getSelectedValues())
					networks.add(name2NetworkMap.get(netName));
 
		}
 		for (CyNetwork network: networks){
 			CyTable targetTable = getTable(network, tableType);
 			if (targetTable != null){
 				if(byReader){
 					if (reader.getTables() != null && reader.getTables().length >0){
 						for(CyTable sourceTable : reader.getTables())
 							mapTable(targetTable, sourceTable);
 					}
 				}else
 					mapTable(targetTable, globalTable);
 			}
 		}
 		if(!selectedNetworksOnly){ //if table should be mapped to all of the tables
 			//add each mapped table to the list for mapping to networks going to be added later
 			if(byReader){
 				if (reader.getTables() != null && reader.getTables().length >0)
 					for(CyTable sourceTable : reader.getTables())
 						UpdateAddedNetworkAttributes.addMappingToList(sourceTable, tableType.getType());
 			}else
 				UpdateAddedNetworkAttributes.addMappingToList(globalTable, tableType.getType());
 		}
 	}
 
 
 	
 	private CyTable getTable(CyNetwork network, TableType tableType){
 		if (tableType == TableType.NODE_ATTR)
 			return network.getDefaultNodeTable();
 		if (tableType == TableType.EDGE_ATTR)
 			return network.getDefaultEdgeTable();
 		if (tableType == TableType.NETWORK_ATTR)
 			return network.getDefaultNetworkTable();
 		logger.warn("The selected table type is not valie. \nTable needs to be one of these types: " +TableType.NODE_ATTR +", " + TableType.EDGE_ATTR + "or "+ TableType.NETWORK_ATTR +".");
 		return null;
 	}
 	
 	private void mapTable(final CyTable localTable, final CyTable globalTable) {
 		if (globalTable.getPrimaryKey().getType() != String.class)
 			throw new IllegalStateException("Local table's primary key should be type String!");
 
 		final CyColumn trgCol = localTable.getColumn(CyNetwork.NAME);
 		if (trgCol != null)
 			localTable.addVirtualColumns(globalTable, CyNetwork.NAME, false);
 		else
 			logger.warn("Name column in the target table was not found!");
 	}
 }
