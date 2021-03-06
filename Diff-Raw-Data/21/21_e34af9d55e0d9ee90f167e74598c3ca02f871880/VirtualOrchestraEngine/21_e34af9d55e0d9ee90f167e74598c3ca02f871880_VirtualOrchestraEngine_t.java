 package de.uniba.wiai.dsg.betsy.virtual.host.engines;
 
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import betsy.data.Process;
 import betsy.data.engines.orchestra.OrchestraEngine;
 import de.uniba.wiai.dsg.betsy.Configuration;
 import de.uniba.wiai.dsg.betsy.virtual.host.VirtualBoxController;
 import de.uniba.wiai.dsg.betsy.virtual.host.VirtualEngine;
 import de.uniba.wiai.dsg.betsy.virtual.host.VirtualEnginePackageBuilder;
 import de.uniba.wiai.dsg.betsy.virtual.host.utils.ServiceAddress;
 
 public class VirtualOrchestraEngine extends VirtualEngine {
 
 	private final OrchestraEngine defaultEngine;
 	private Configuration config = Configuration.getInstance();
 
 	public VirtualOrchestraEngine(VirtualBoxController vbc) {
 		super(vbc);
 		this.defaultEngine = new OrchestraEngine();
 		this.defaultEngine.setPackageBuilder(new VirtualEnginePackageBuilder(
 				getName()));
 	}
 
 	@Override
 	public String getName() {
 		return "orchestra_v";
 	}
 
 	@Override
 	public List<ServiceAddress> getRequiredAddresses() {
 		List<ServiceAddress> saList = new LinkedList<>();
 		saList.add(new ServiceAddress("http", "localhost", "/orchestra", 8080));
 		return saList;
 	}
 
 	@Override
 	public Set<Integer> getRequiredPorts() {
 		Set<Integer> portList = new HashSet<>();
 		portList.add(8080);
 		return portList;
 	}
 
 	@Override
 	public Integer getEndpointPort() {
 		return 8080;
 	}
 
 	@Override
 	public String getEndpointPath(Process process) {
 		return "/orchestra/" + process.getBpelFileNameWithoutExtension()
 				+ "TestInterface";
 	}
 
 	@Override
 	public void buildArchives(Process process) {
 		// use default engine's operations
 		defaultEngine.buildArchives(process);
 	}
 
 	@Override
 	public String getXsltPath() {
 		return "src/main/xslt/" + defaultEngine.getName();
 	}
 
 	@Override
 	public void onPostDeployment() {
 		// not required. deploy is in sync and does not return before engine is
 		// deployed
 	}
 
 	@Override
 	public void onPostDeployment(Process process) {
 		// not required. deploy is in sync and does not return before process is
 		// deployed
 	}
 
 	@Override
 	public String getVMLogfileDir() {
 		return config.getValueAsString(
 				"virtualisation.engines.orchestra_v.logfileDir",
 				"/var/lib/tomcat7/logs");
 	}
 
 	@Override
 	public String getVMDeploymentDir() {
		return config.getValueAsString(
				"virtualisation.engines.orchestra_v.deploymentDir",
				"/usr/share/tomcat7/webapps/orchestra/WEB-INF/processes");
	}

	@Override
	public String getTargetPackageExtension() {
		return "zip";
 	}
 }
