 package com.redhat.qe.jon.clitest.tests;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.net.URL;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.apache.commons.lang3.StringUtils;
 import org.testng.annotations.AfterTest;
 import org.testng.annotations.Optional;
 import org.testng.annotations.Parameters;
 import org.testng.annotations.Test;
 
 import com.redhat.qe.jon.clitest.base.CliTestScript;
 import com.redhat.qe.jon.clitest.tasks.CliTasks;
 import com.redhat.qe.jon.clitest.tasks.CliTasksException;
 
 public class CliTest extends CliTestScript{
 	private static Logger _logger = Logger.getLogger(CliTest.class.getName());
 	public static String cliShLocation;
 	public static String rhqCliJavaHome;
 	public static String rhqTarget;
 	private String cliUsername;
 	private String cliPassword;
 	protected CliTasks cliTasks;
 	
 	private String jsFileName;
 	private static String remoteFileLocation = "/tmp/";
 	
 	public static boolean isVersionSet = false;
 		
 	
 	
 	
 	//@Parameters({"rhq.target","cli.username","cli.password","js.file","cli.args","expected.result","make.failure"})
 	public void loadSetup(@Optional String rhqTarget, @Optional String cliUsername, @Optional String cliPassword, @Optional String makeFailure) throws IOException{
 		if(rhqTarget != null)
 			CliTest.rhqTarget = rhqTarget;
 		if(cliUsername != null)
 			this.cliUsername = cliUsername;
 		if(cliPassword != null)
 			this.cliPassword = cliPassword;
 	}
 	
 	private String getResourceFileName(String path) throws CliTasksException {
 		if (path.startsWith("http://") || path.startsWith("https://")) {
 			try {
 				File file = File.createTempFile("temp", ".js");
 				file.deleteOnExit();
 				cliTasks.runCommand("wget -nv '"+path+"' -O "+file.getAbsolutePath()+" 2>&1");
 				return file.getAbsolutePath();
 			} catch (IOException e) {
 				throw new CliTasksException("Unable to create temporary file", e);
 			}
 			
 		}
 		URL resource = null;
 		if (!path.startsWith("/")) {
 			_logger.fine("Appending / to resource path");
 			resource = CliTest.class.getResource("/"+path);
 			if (resource==null) {
 				_logger.fine("Appending /js-files/ to resource path");
 				resource = CliTest.class.getResource("/js-files/"+path);	
 			}
 			if (resource==null) {
 				throw new RuntimeException("Unable to retrieve either [/"+path+"] or [/js-files/"+path+"] resource on classpath!");
 			}
 		}
 		else {
 			resource = CliTest.class.getResource(path);
 		}
 		if (resource==null) {
 			throw new RuntimeException("Unable to retrieve ["+path+"] resource on classpath!");
 		}
 		return resource.getFile();
 	}
 	
	
 	/**
 	 * parameters resSrc and resDest must have same count of items and are processed together (resSrc[0] will be copied to resDst[0])
 	 * @param rhqTarget
 	 * @param cliUsername
 	 * @param cliPassword
 	 * @param jsFile to be executed
 	 * @param cliArgs arguments passed
 	 * @param expectedResult comma-separated list of messages that are expected as output
 	 * @param makeFilure comma-separated list of messages - if these are found in output, test fails
 	 * @param jsDepends comma-separated list of other JS files that are required/imported by <b>jsFile</b>
 	 * @param resSrc comma-separated list of source paths
 	 * @param resDst comma-separated list of source paths (must be same size as resSrc)
 	 * @throws IOException
 	 * @throws CliTasksException
 	 */
	@Parameters({"rhq.target","cli.username","cli.password","js.file","cli.args","expected.result","make.failure","js.depends","res.src","res.dst"})
	@Test
 	public void runJSfile(@Optional String rhqTarget, @Optional String cliUsername, @Optional String cliPassword, String jsFile, @Optional String cliArgs, @Optional String expectedResult, @Optional String makeFilure,@Optional String jsDepends,@Optional String resSrc, @Optional String resDst) throws IOException, CliTasksException{
 		loadSetup(rhqTarget, cliUsername, cliPassword, makeFilure);
 		cliTasks = CliTasks.getCliTasks();
 
 		// process additional resource files
 		if (resSrc!=null && resDst!=null) {
 			prepareResources(resSrc, resDst);
 		}
 		String jsFilePath = getResourceFileName(jsFile);
 		jsFileName = new File(jsFilePath).getName();
 		// upload JS file to remote host first
 		cliTasks.copyFile(jsFilePath, remoteFileLocation);
 		if (jsDepends!=null) {
 			prepareDependencies(jsFile, jsDepends,jsFilePath);
 		}
 		
 		// autodetect RHQ_CLI_JAVA_HOME if not defined
 		if (StringUtils.trimToNull(rhqCliJavaHome)==null) {
 			rhqCliJavaHome = cliTasks.runCommand("echo $JAVA_HOME").trim();
 			_logger.log(Level.INFO,"Environment variable RHQ_CLI_JAVA_HOME was autodetected using JAVA_HOME variable");
 		}
 		String consoleOutput = null;
 		String command = "export RHQ_CLI_JAVA_HOME="+rhqCliJavaHome+";"+CliTest.cliShLocation+" -s "+CliTest.rhqTarget+" -u "+this.cliUsername+" -p "+this.cliPassword+" -f "+remoteFileLocation+jsFileName;
 		if(cliArgs != null){
 			command +=" "+cliArgs;
 		}
 		// get live output in log file on server
 		command +=" | tee -a /tmp/cli-automation.log";
 		consoleOutput = cliTasks.runCommand(command);
 		if(!isVersionSet){
 			System.setProperty("rhq.build.version", consoleOutput.substring(consoleOutput.indexOf("Remote server version is:")+25, consoleOutput.indexOf("Login successful")).trim());
 			isVersionSet = true;
 			_logger.log(Level.INFO, "RHQ/JON Version: "+System.getProperty("rhq.build.version"));
 		}
 		
 		_logger.log(Level.INFO, consoleOutput);
 		if(makeFilure != null){
 			cliTasks.validateErrorString(consoleOutput , makeFilure);
 		}
 		if(expectedResult != null){
 			cliTasks.validateExpectedResultString(consoleOutput , expectedResult);
 		}
 		
 	}
 
 	private void prepareResources(String resSrc, String resDst)
 			throws CliTasksException, IOException {
 		_logger.info("Processing additional resources...");
 		String[] sources = resSrc.split(",");
 		String[] dests = resDst.split(",");
 		if (sources.length!=dests.length) {
 			throw new CliTasksException("res.src parameter must be same length as res.dst, please update your testng configuration!");
 		}
 		for (int i=0;i<sources.length;i++) {
 			String src = sources[i];
 			File dst = new File(dests[i]);
 			String destDir = dst.getParent();
 			
 			if (destDir==null) {
 				destDir="/tmp";
 			}
 			else if (!dst.isAbsolute()) {
 				destDir="/tmp/"+destDir;
 			}
 							
 			cliTasks.runCommand("mkdir -p "+destDir);
 			if (src.startsWith("http")) {
 				cliTasks.runCommand("wget -nv "+src+" -O "+destDir+"/"+dst.getName()+" 2>&1");
 			}
 			else {
 				URL resource = CliTest.class.getResource(src);
 				if (resource==null) {
 					throw new CliTasksException("Resource file "+src+" does not exist!");
 				}
 				cliTasks.copyFile(resource.getPath(), destDir,dst.getName());
 			}
 		}
 	}
 
 	private void prepareDependencies(String jsFile, String jsDepends, String mainJsFilePath)
 			throws IOException, CliTasksException {
 		int longestDepNameLength=0;
 		Map<String,Integer> lines = new LinkedHashMap<String, Integer>(); 
 		_logger.info("Preparing JS file depenencies ... "+jsDepends);
 		for (String dependency : jsDepends.split(",")) {
 			if (dependency.length()>longestDepNameLength) {
 				longestDepNameLength = dependency.length();
 			}
 			String jsFilePath = getResourceFileName(dependency);
 			lines.put(dependency, getFileLineCount(jsFilePath));
 			cliTasks.copyFile(jsFilePath, remoteFileLocation, "_tmp.js");
 			// as CLI does not support including, we must merge the files manually
 			cliTasks.runCommand("cat "+remoteFileLocation+"_tmp.js >> "+remoteFileLocation+"_deps.js");
 		}
 		cliTasks.runCommand("rm "+remoteFileLocation+"_tmp.js");
 		// finally merge main jsFile		
 		cliTasks.runCommand("cat "+remoteFileLocation+jsFileName+" >> "+remoteFileLocation+"_deps.js && mv "+remoteFileLocation+"_deps.js "+remoteFileLocation+jsFileName);			
 		_logger.info("JS file depenencies ready");
 		_logger.info("Output file has been merged from JS files as follows:");
 		int current = 0;
 		lines.put(jsFile, getFileLineCount(mainJsFilePath));
 		if (jsFile.length()>longestDepNameLength) {
 			longestDepNameLength = jsFile.length();
 		}
 		_logger.info("===========================");
 		for (String dep : lines.keySet()) {
 			_logger.info("JS File: "+dep+createSpaces(longestDepNameLength-dep.length())+" lines: "+current+" - "+(current+lines.get(dep)));
 			current+=lines.get(dep)+1;
 		}
 		_logger.info("===========================");
 	}	
 	
 	@AfterTest
 	public void deleteJSFile(){
 		try {
 			CliTasks.getCliTasks().runCommand("rm -rf '"+remoteFileLocation+jsFileName+"'", 1000*60*3);
 		} catch (CliTasksException ex) {
 			_logger.log(Level.WARNING, "Exception on remote File deletion!, ", ex);
 		}
 	}
 	private String createSpaces(int length) {
 		StringBuilder sb = new StringBuilder();
 		while (length>0) {
 			sb.append(" ");
 			length--;
 		}
 		return sb.toString();
 	}
 	private int getFileLineCount(String path) {
 		BufferedReader reader = null;
 		int lines = 0;
 		try {
 			reader = new BufferedReader(new FileReader(path));
 			while (reader.readLine() != null) lines++;
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		try {
 			reader.close();
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		return lines;
 	}
 	
 }
