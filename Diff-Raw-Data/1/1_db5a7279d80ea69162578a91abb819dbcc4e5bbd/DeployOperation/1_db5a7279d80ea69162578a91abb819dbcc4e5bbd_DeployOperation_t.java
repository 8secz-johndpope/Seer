 package betsy.virtual.server.logic;
 
 import betsy.data.engines.activebpel.ActiveBpelDeployer;
 import betsy.data.engines.bpelg.BpelgDeployer;
 import betsy.data.engines.ode.OdeDeployer;
 import betsy.data.engines.openesb.OpenEsbCLI;
 import betsy.data.engines.openesb.OpenEsbDeployer;
 import betsy.data.engines.orchestra.OrchestraDeployer;
 import betsy.data.engines.petalsesb.PetalsEsbDeployer;
 import betsy.virtual.common.exceptions.ChecksumException;
 import betsy.virtual.common.exceptions.CommunicationException;
 import betsy.virtual.common.exceptions.ConnectionException;
 import betsy.virtual.common.exceptions.DeployException;
 import betsy.virtual.common.messages.deploy.DeployRequest;
 import betsy.virtual.common.messages.deploy.DeployResponse;
 import org.apache.log4j.Logger;
 
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 
 public class DeployOperation {
 
     private final static Logger log = Logger.getLogger(DeployOperation.class);
 
     public static DeployResponse deployOperation(DeployRequest request) throws DeployException, ChecksumException, ConnectionException {
         if (request.getFileMessage().isInvalid()) {
             throw new ChecksumException("Given data is invalid");
         }
         log.info("Received file is valid");
 
         Path pathToPackageFile = saveFileToDisk(request);
 
         log.info("Calling deployer for engine " + request.getEngineName());
         Path deploymentDirPath = Paths.get(request.getDeploymentDir());
         Path engineLogfileDirPath = Paths.get(request.getEngineLogfileDir());
         if ("petalsesb_v".equals(request.getEngineName())) {
             PetalsEsbDeployer deployer = new PetalsEsbDeployer();
             deployer.setDeploymentDirPath(deploymentDirPath);
             deployer.setProcessName(request.getProcessName());
             deployer.setLogFilePath(engineLogfileDirPath);
             deployer.setPackageFilePath(pathToPackageFile);
             deployer.setTimeoutInSeconds(request.getDeployTimeout());
             deployer.deploy();
         } else if ("ode_v".equals(request.getEngineName())) {
             OdeDeployer deployer = new OdeDeployer();
             deployer.setDeploymentDirPath(deploymentDirPath);
             deployer.setProcessName(request.getProcessName());
             deployer.setLogFilePath(engineLogfileDirPath);
             deployer.setPackageFilePath(pathToPackageFile);
             deployer.setTimeoutInSeconds(request.getDeployTimeout());
             deployer.deploy();
         } else if ("bpelg_v".equals(request.getEngineName())) {
             BpelgDeployer deployer = new BpelgDeployer();
             deployer.setDeploymentDirPath(deploymentDirPath);
             deployer.setProcessName(request.getProcessName());
             deployer.setLogFilePath(engineLogfileDirPath);
             deployer.setPackageFilePath(pathToPackageFile);
             deployer.setTimeoutInSeconds(request.getDeployTimeout());
             deployer.deploy();
         } else if ("active_bpel_v".equals(request.getEngineName())) {
             ActiveBpelDeployer deployer = new ActiveBpelDeployer();
             deployer.setDeploymentDirPath(deploymentDirPath);
             deployer.setProcessName(request.getProcessName());
             deployer.setLogFilePath(engineLogfileDirPath);
             deployer.setPackageFilePath(pathToPackageFile);
             deployer.setTimeoutInSeconds(request.getDeployTimeout());
             deployer.deploy();
         } else if ("orchestra_v".equals(request.getEngineName())) {
             OrchestraDeployer deployer = new OrchestraDeployer();
             deployer.setOrchestraHome(deploymentDirPath);
             deployer.setPackageFilePath(pathToPackageFile);
            deployer.setAntBinFolder(Paths.get("TODO"));
             deployer.deploy();
         } else if ("openesb_v".equals(request.getEngineName())) {
             OpenEsbCLI cli = new OpenEsbCLI();
             cli.setGlassfishHome(Paths.get("/opt/openesb/glassfish"));
 
             OpenEsbDeployer deployer = new OpenEsbDeployer();
             deployer.setTmpFolder(Paths.get("/opt/betsy"));
             deployer.setProcessName(request.getProcessName());
             deployer.setPackageFilePath(pathToPackageFile);
             deployer.setCli(cli);
             deployer.deploy();
         } else {
             throw new CommunicationException("invalid engine given");
         }
 
         log.info("Deployment successful -> sending response");
         return new DeployResponse();
     }
 
     public static Path saveFileToDisk(DeployRequest operation) throws DeployException {
         // write data to temp dir first
         Path tmpDeployFile = Paths.get("tmp").resolve(operation.getFileMessage().getFilename());
         try {
             Files.write(tmpDeployFile, operation.getFileMessage().getData());
             log.info("Received file is stored to disk");
             return tmpDeployFile.toAbsolutePath();
         } catch (IOException e) {
             throw new DeployException("Couldn't write the container data to "
                     + "the local disk:", e);
         }
     }
 
 }
