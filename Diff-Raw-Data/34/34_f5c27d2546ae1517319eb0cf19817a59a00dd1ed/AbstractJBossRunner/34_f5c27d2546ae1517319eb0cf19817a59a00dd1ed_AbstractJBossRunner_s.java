 package pl.softwaremill.common.uitest.jboss;
 
 import org.testng.annotations.AfterSuite;
 import org.testng.annotations.BeforeSuite;
 import pl.softwaremill.common.uitest.selenium.ServerPoperties;
 
 import java.io.IOException;
 import java.util.Scanner;
 
 /**
  *
  * @author maciek
  * @author Pawel Wrzeszcz
  */
 public abstract class AbstractJBossRunner {
 
     private String serverHome;
     private String serverPort;
     private String configuration;
     private boolean running;
     private int portset;
     
     Process jbossProcess;
 
 	private final static SysoutLog log = new SysoutLog();
 
 	private static final String STARTED_LOG_MESSAGE = "Started in";
 
 	private static final int DEPLOYMENT_TIMEOUT = 5 * 60 * 1000; // 5 minutes
 	private boolean deploymentComplete = false;
 
 	protected abstract ServerPoperties getServerProperties();
 
 	protected abstract Deployment[] getDeployments();
 
 	@BeforeSuite
     public void start() throws Exception {
 		loadProperties();
 		scheduleTimeout();
 		undeploy(); // Clean old deployments
 		startServerIfNeeded();
 		deploy();
     }
 
 	@AfterSuite(alwaysRun = true)
 	public void shutdown() throws Exception {
     	undeploy();
         if (!running) {
 			log.info("Stopping JBoss server");
 			shutdownServer();
 			log.info("JBoss Server stopped");
         }
     }
 
 	private void loadProperties() {
 		ServerPoperties serverPoperties = getServerProperties();
 
 		this.serverHome = serverPoperties.getServerHome();
 		this.serverPort = "8"+serverPoperties.getPortset()+"80";
 		this.configuration = serverPoperties.getConfiguration();
 		this.running =serverPoperties.isRunning();
 		this.portset = serverPoperties.getPortset();
 	}
 
 	private void startServerIfNeeded() throws Exception {
         if (!running) {
 			log.info("Starting JBoss server");
 			startServer();
 			log.info("JBoss started");
 		}
     }
 
     protected void startServer() throws Exception {
        jbossProcess = Runtime.getRuntime().exec(new String[]{serverHome + "/bin/run.sh", "-c", configuration, "-Djboss.service.binding.set=ports-0"+portset});
 		waitFor(jbossProcess, STARTED_LOG_MESSAGE);
 	}
 
 	private void waitFor(Process process, String message) {
 		log.info("Waiting for message: [" + message + "]");
 		Scanner scanner = new Scanner(process.getInputStream()).useDelimiter(message);
 		scanner.next();
 	}
 
 	private Process getTailProcess() throws IOException {
		return Runtime.getRuntime().exec(new String[]{"tail", "-f", serverHome + "/server/" + configuration + "/log/server.log"});
 	}
 
 	private void shutdownServer() throws IOException, InterruptedException {
		Process shutdownProcess = Runtime.getRuntime().exec(new String[]{serverHome + "/bin/shutdown.sh", "-s", "localhost:1"+portset+"99", "-S"});
 		shutdownProcess.waitFor();
 	}
 
 	private void deploy() throws Exception {
 		for (Deployment deployment : getDeployments()) {
 			deployment.deploy(getDeployDir());
 			waitFor(getTailProcess(), deployment.getWaitForMessage());
 		}
 		deploymentComplete = true;
 	}
 
 	private void undeploy() throws Exception {
 		for (Deployment deployment : getDeployments()) {
 			deployment.undeploy(getDeployDir());
 		}
 	}
 
 	public String getDeployDir() {
 		return serverHome + "/server/" + configuration + "/deploy/";
 	}
 
 	private static class SysoutLog {
 		public void info(String msg) {
 			System.out.println("--- " + msg);
 		}
 	}
 
 	private void scheduleTimeout() {
 		new Thread(
 			new Runnable() {
 
 				@Override
 				public void run() {
 					try {
 						Thread.sleep(DEPLOYMENT_TIMEOUT);
 						if (!deploymentComplete) {
 							System.out.println("Timeout, shutting down JBoss");
 							shutdown();
 							System.exit(1);
 						}
 					} catch (InterruptedException e) {
 						// do nothing
 					} catch (Exception e) {
 						throw new RuntimeException(e);
 					}
 				}
 			}
 		).start();
 	}
 
 }
