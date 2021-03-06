 
 package org.cytoscape.io.webservice.biomart;
 
 import java.util.Properties;
 
 import org.cytoscape.application.CyApplicationManager;
 import org.cytoscape.application.swing.CySwingApplication;
 import org.cytoscape.io.webservice.biomart.rest.BiomartRestClient;
 import org.cytoscape.io.webservice.biomart.ui.BiomartAttrMappingPanel;
 import org.cytoscape.model.CyNetworkManager;
 import org.cytoscape.model.CyTableFactory;
 import org.cytoscape.model.CyTableManager;
 import org.cytoscape.service.util.AbstractCyActivator;
 import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;
 import org.cytoscape.work.swing.DialogTaskManager;
 import org.osgi.framework.BundleContext;
 
 
 public class CyActivator extends AbstractCyActivator {
 	public CyActivator() {
 		super();
 	}
 
 	public void start(BundleContext bc) {
 
 		// Import services
 		CySwingApplication cySwingApplicationServiceRef = getService(bc,CySwingApplication.class);
 		DialogTaskManager taskManagerServiceRef = getService(bc,DialogTaskManager.class);
 		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
 		CyTableManager cyTableManagerServiceRef = getService(bc,CyTableManager.class);
 		CyApplicationManager cyApplicationManagerServiceRef = getService(bc,CyApplicationManager.class);
 		CyTableFactory cyTableFactoryServiceRef = getService(bc,CyTableFactory.class);
 		MapTableToNetworkTablesTaskFactory mapNetworkAttrTFServiceRef = getService(bc,MapTableToNetworkTablesTaskFactory.class);
		
 		
 		// Export services
 		BiomartRestClient biomartRestClient = new BiomartRestClient("http://www.biomart.org/biomart/martservice");
		BiomartAttrMappingPanel biomartAttrMappingPanel = new BiomartAttrMappingPanel(taskManagerServiceRef,cyApplicationManagerServiceRef,cyTableManagerServiceRef,cyNetworkManagerServiceRef);
 		
 		BiomartClient biomartClient = new BiomartClient("BioMart Client","REST version of BioMart Web Service Client.",biomartRestClient,cyTableFactoryServiceRef,cySwingApplicationServiceRef,cyTableManagerServiceRef, biomartAttrMappingPanel, mapNetworkAttrTFServiceRef);
 		biomartAttrMappingPanel.setClient(biomartClient);
 		
 		registerAllServices(bc,biomartAttrMappingPanel, new Properties());
 		registerAllServices(bc,biomartClient, new Properties());
 	}
 }
 
