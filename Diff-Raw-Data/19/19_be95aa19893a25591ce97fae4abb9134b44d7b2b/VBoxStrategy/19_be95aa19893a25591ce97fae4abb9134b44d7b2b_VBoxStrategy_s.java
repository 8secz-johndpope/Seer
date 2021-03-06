 package org.ourgrid.virt.strategies.vbox;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Random;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.ourgrid.virt.model.ExecutionResult;
 import org.ourgrid.virt.model.VirtualMachine;
 import org.ourgrid.virt.model.VirtualMachineConstants;
 import org.ourgrid.virt.model.VirtualMachineStatus;
 import org.ourgrid.virt.strategies.HypervisorStrategy;
 import org.ourgrid.virt.strategies.HypervisorUtils;
 
 import com.google.gson.JsonArray;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonParser;
 
 public class VBoxStrategy implements HypervisorStrategy {
 
 	private static final String FILE_ERROR = "VBOX_E_FILE_ERROR";
 	private static final String OBJECT_IN_USE = "VBOX_E_OBJECT_IN_USE";
 	private static final String INVALID_ARG = "E_INVALIDARG";
 	private static final String NS_INVALID_ARG = "NS_ERROR_INVALID_ARG";
	
 	private static final String DISK_CONTROLLER_NAME = "Disk Controller";
 	
 	@Override
 	public void create(VirtualMachine virtualMachine) throws Exception {
 		
 		register(virtualMachine);
 		boolean definedSata = define(virtualMachine);
 		
 		if (!definedSata) {
 			return;
 		}
 		
 		String imagePath = virtualMachine.getProperty(
 				VirtualMachineConstants.DISK_IMAGE_PATH);
 		attachDisk(virtualMachine, imagePath);
 	}
 
 	/**
 	 * Throws an exception if the disk could not be attached
 	 * for any reason.
 	 * 
 	 * @param virtualMachine
 	 * @param diskPath
 	 * @return
 	 * @throws Exception
 	 */
 	private void attachDisk(VirtualMachine virtualMachine, String diskPath) 
 			throws Exception {
 		
 		if (!new File(diskPath).exists()) {
 			throw new FileNotFoundException("The image file does not exist: " + diskPath);
 		}
 		
 		ProcessBuilder attachMediaBuilder = getProcessBuilder(
 				"storageattach " + virtualMachine.getName() +  
 				" --storagectl \"" + DISK_CONTROLLER_NAME + "\" --medium \"" + diskPath + 
 				"\" --port 0 --device 0 --type hdd");
 		
 		HypervisorUtils.runAndCheckProcess(attachMediaBuilder);
 	}
 
 	/**
 	 * Returns true if controller with name 'SATA Controller' was defined, 
 	 * or if a controller with this name already exists. Returns false if
 	 * a controller of the type SATA is already defined.
 	 * 
 	 * Throws an exception if the controller could not be defined 
 	 * for any other reason.
 	 * 
 	 * @param virtualMachine
 	 * @return
 	 * @throws Exception
 	 */
 	private boolean define(VirtualMachine virtualMachine)
 			throws Exception {
 		
 		String memory = virtualMachine.getProperty(
 				VirtualMachineConstants.MEMORY);
 		String diskType = virtualMachine.getProperty(
 				VirtualMachineConstants.DISK_TYPE);
 		
 		ProcessBuilder modifyVMBuilder = getProcessBuilder(
 				"modifyvm " + virtualMachine.getName() + " --memory " + memory + 
 				" --acpi on --boot1 disk --vrde off");
 		HypervisorUtils.runAndCheckProcess(modifyVMBuilder);
 		
 		ProcessBuilder createControllerBuilder = getProcessBuilder(
 				"storagectl " + virtualMachine.getName() + " --name \"" + DISK_CONTROLLER_NAME + "\" --add " + diskType);
 		ExecutionResult createControllerResult = HypervisorUtils.runProcess(createControllerBuilder);
 		
 		String stdErr = createControllerResult.getStdErr().toString();
 		
 		if (createControllerResult.getReturnValue() != ExecutionResult.OK) {
 			
 			// Controller with this name already exists
 			if (stdErr.contains(OBJECT_IN_USE)) {
 				return true;
 			}
 			
 			// VM already has a SATA controller with different name
 			if (stdErr.contains(INVALID_ARG) || stdErr.contains(NS_INVALID_ARG)) {
 				return false;
 			}
 			
 			throw new Exception(stdErr);
 		}
 		
 		return true;
 	}
 
 	private void register(VirtualMachine virtualMachine)
 			throws Exception {
 		
 		String os = virtualMachine.getProperty(
 				VirtualMachineConstants.OS_VERSION);
 		
 		ProcessBuilder createProcessBuilder = getProcessBuilder(
 				"createvm --name " + virtualMachine.getName() + " --ostype \"" + os + "\" --register");
 		ExecutionResult createExecutionResult = HypervisorUtils.runProcess(createProcessBuilder);
 		
 		String stdErr = createExecutionResult.getStdErr().toString();
 		
 		if (createExecutionResult.getReturnValue() != ExecutionResult.OK
 				&& !stdErr.contains(FILE_ERROR)) {
 			throw new Exception(stdErr);
 		}
 		
 	}
 	
 	@Override
 	public void start(VirtualMachine virtualMachine) throws Exception {
 		startVirtualMachine(virtualMachine);
 		checkOSStarted(virtualMachine);
 		mountSharedFolders(virtualMachine);
 	}
 
 	private void mountSharedFolders(VirtualMachine virtualMachine) throws Exception {
 		
 		String sharedFolders = virtualMachine.getProperty(
 				VirtualMachineConstants.SHARED_FOLDERS);
 		
 		if (sharedFolders == null) {
 			return;
 		}
 		
 		JsonArray sharedFoldersJson = (JsonArray) new JsonParser().parse(sharedFolders);
 		
 		for (int i = 0; i < sharedFoldersJson.size(); i++) {
 			
 			JsonObject sharedFolderJson = (JsonObject) sharedFoldersJson.get(i);
 			String name = sharedFolderJson.get("name").getAsString();
 			String guestPath = sharedFolderJson.get("guestpath").getAsString();
 			
 			mountSharedFolder(virtualMachine, name, guestPath);
 			
 		}
 	}
 
 	@Override
 	public void mountSharedFolder(VirtualMachine virtualMachine,
 			String name, String guestPath)
 			throws Exception {
 		
 		String password = virtualMachine.getConfiguration().get(
 				VirtualMachineConstants.GUEST_PASSWORD);
 
 		String user = virtualMachine.getConfiguration().get(
 				VirtualMachineConstants.GUEST_USER);
 		
 		if (HypervisorUtils.isWindowsGuest(virtualMachine)) {
 			
 			HypervisorUtils.checkReturnValue(
 					exec(virtualMachine, 
 					"net use " + guestPath + " \\\\vboxsvr\\" + name));
 			
 		} else if (HypervisorUtils.isLinuxGuest(virtualMachine)) {
 			
 			long randId = Math.abs(new Random().nextLong());
 			String mountScriptFileName = "mount" + randId + ".sh";
 			String mountScriptFilePath = "/tmp/" + mountScriptFileName;
 
 			File mountFile = new File(mountScriptFilePath);
 			FileWriter mountFileWriter = new FileWriter(mountFile);
 			mountFileWriter.write(
					"mkdir -p " + guestPath + "; " +
 					"sudo mount -t vboxsf " + name + " " + guestPath + "; " +
 					"rm " + mountScriptFilePath);
 			mountFileWriter.close();
 
 			try {
 
 				ProcessBuilder copyMountScriptBuilder = getProcessBuilder(
 						"guestcontrol " + virtualMachine.getName() +  
 						" copyto \"" + mountFile.getCanonicalPath() + "\" /tmp/" + 
 						" --username " + user + " --password " + password);
 				HypervisorUtils.runAndCheckProcess(copyMountScriptBuilder);
 
 				HypervisorUtils.checkReturnValue(
 						exec(virtualMachine, "/bin/bash -x " + mountScriptFilePath));
 
 			} finally {
 				mountFile.delete();
 			}
 			
 		} else {
 			throw new Exception("Guest OS not supported");
 		}
 	}
 
 	private void startVirtualMachine(VirtualMachine virtualMachine)
 			throws IOException, Exception {
 		
 		if (HypervisorUtils.isWindowsHost()) {
 			ProcessBuilder vbsProcessBuilder = new ProcessBuilder("wscript", 
 					new File(getClass().getResource("start-vm.vbs").getFile()).getCanonicalPath(), virtualMachine.getName());
 			vbsProcessBuilder.directory(new File(
 					System.getenv().get("VBOX_INSTALL_PATH")));
 			ExecutionResult vbsExecutionResult = 
 					HypervisorUtils.runProcess(vbsProcessBuilder);
 			
 			if (vbsExecutionResult.getReturnValue() != ExecutionResult.OK) {
 				throw new Exception("Could not start VM");
 			}
 			
 		} else {
 			ProcessBuilder startProcessBuilder = getProcessBuilder(
 					"startvm " + virtualMachine.getName() + " --type headless");
 			HypervisorUtils.runAndCheckProcess(startProcessBuilder);
 		}
 	}
 
 	private void checkOSStarted(VirtualMachine virtualMachine)
 			throws InterruptedException {
 		while (true) {
 			try {
 				
 				if (HypervisorUtils.isLinuxGuest(virtualMachine)) {
 					ExecutionResult executionResult = exec(
 							virtualMachine, "/bin/echo check-started");
 					HypervisorUtils.checkReturnValue(executionResult);
 				} else if (HypervisorUtils.isWindowsGuest(virtualMachine)) {
 					ExecutionResult executionResult = exec(
 							virtualMachine, "Echo check-started");
 					HypervisorUtils.checkReturnValue(executionResult);
 				} else {
 					throw new Exception("Guest OS not supported");
 				}
 				
 				break;
 			} catch (Exception e) {}
 			
 			Thread.sleep(1000 * 10);
 		}
 	}
 
 	@Override
 	public void stop(VirtualMachine virtualMachine) throws Exception {
 		
 		ProcessBuilder acpiPowerProcessBuilder = getProcessBuilder(
 				"controlvm " + virtualMachine.getName() + " acpipowerbutton");
 		HypervisorUtils.runAndCheckProcess(acpiPowerProcessBuilder);
 		
 		ProcessBuilder stopProcessBuilder = getProcessBuilder(
 				"controlvm " + virtualMachine.getName() + " poweroff");
 		HypervisorUtils.runAndCheckProcess(stopProcessBuilder);
 	}
 	
 	@Override
 	public ExecutionResult exec(VirtualMachine virtualMachine, String command) throws Exception {
 		
 		String user = virtualMachine.getConfiguration().get(
 				VirtualMachineConstants.GUEST_USER);
 		String password = virtualMachine.getConfiguration().get(
 				VirtualMachineConstants.GUEST_PASSWORD);
 		
 		String[] splittedCommand = command.split(" ");
 		
 		StringBuilder cmdBuilder = new StringBuilder("guestcontrol ");
 		cmdBuilder.append("\"").append(virtualMachine.getName()).append("\"");
 		cmdBuilder.append(" exec --image ");
 		cmdBuilder.append("\"").append(splittedCommand[0]).append("\"");
 		cmdBuilder.append(" --username ");
 		cmdBuilder.append(user);
 		cmdBuilder.append(" --password ");
 		cmdBuilder.append(password);
 		cmdBuilder.append(" --wait-exit --wait-stdout");
 		
 		if (splittedCommand.length > 1) {
 			cmdBuilder.append(" --");
 			for (int i = 1; i < splittedCommand.length; i++) {
 				cmdBuilder.append(" ").append(splittedCommand[i]);
 			}
 		}
 		
 		ProcessBuilder stopProcessBuilder = getProcessBuilder(cmdBuilder.toString());
 		return HypervisorUtils.runProcess(stopProcessBuilder);
 	}
 	
 	@Override
 	public void takeSnapshot(String vMName, String snapshotName)
 			throws Exception {
 		ProcessBuilder takeSnapshotProcessBuilder = getProcessBuilder(
 				"snapshot " + vMName + " take " + snapshotName);
 		HypervisorUtils.runAndCheckProcess(takeSnapshotProcessBuilder);
 	}
 	
 	@Override
 	public void restoreSnapshot(String vMName, String snapshotName)
 			throws Exception {
 		ProcessBuilder restoreSnapshotProcessBuilder = getProcessBuilder(
 				"snapshot " + vMName + " restore " + snapshotName);
 		HypervisorUtils.runAndCheckProcess(restoreSnapshotProcessBuilder);
 	}
 	
 	@Override
 	public void destroy(VirtualMachine virtualMachine) throws Exception {
 		ProcessBuilder destroyProcessBuilder = getProcessBuilder(
 				"unregistervm " + virtualMachine.getName() + " --delete");
 		HypervisorUtils.runAndCheckProcess(destroyProcessBuilder);
 	}
 	
 	private static ProcessBuilder getProcessBuilder(
 			String cmd) throws Exception {
 		
 		String vboxManageCmdLine = "VBoxManage --nologo " + cmd;
 		ProcessBuilder processBuilder = null;
 		
 		String vBoxInstallPath = System.getenv().get("VBOX_INSTALL_PATH");
 		
 		if (HypervisorUtils.isWindowsHost()) {
 			if (!new File(vBoxInstallPath + "\\VBoxManage.exe").exists()) {
 				vBoxInstallPath = null;
 			}
 			processBuilder = new ProcessBuilder("cmd", 
 					"/C " + vboxManageCmdLine);
 			
 		} else if (HypervisorUtils.isLinuxHost()) {
 			
 			List<String> matchList = splitCmdLine(vboxManageCmdLine); 
 			processBuilder =  new ProcessBuilder(matchList.toArray(new String[]{}));
 			
 		} else {
 			throw new Exception("Host OS not supported");
 		}
 		
 		if (vBoxInstallPath != null) {
 			processBuilder.directory(new File(vBoxInstallPath));
 		}
 		
 		return processBuilder;
 	}
 
 	private static List<String> splitCmdLine(String vboxManageCmdLine) {
 		List<String> matchList = new ArrayList<String>();
 		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
 		Matcher regexMatcher = regex.matcher(vboxManageCmdLine);
 		while (regexMatcher.find()) {
 		    if (regexMatcher.group(1) != null) {
 		        // Add double-quoted string without the quotes
 		        matchList.add(regexMatcher.group(1));
 		    } else if (regexMatcher.group(2) != null) {
 		        // Add single-quoted string without the quotes
 		        matchList.add(regexMatcher.group(2));
 		    } else {
 		        // Add unquoted word
 		        matchList.add(regexMatcher.group());
 		    }
 		}
 		return matchList;
 	}
 	
 	@Override
 	public void createSharedFolder(VirtualMachine virtualMachine,
 			String shareName, String hostPath) throws Exception {
 		
 		ProcessBuilder versionProcessBuilder = getProcessBuilder(
 				"sharedfolder add \"" + virtualMachine.getName() + "\"" + 
 				" --hostpath \"" + hostPath + "\"" +
 				" --name " + shareName); 
 		HypervisorUtils.runAndCheckProcess(versionProcessBuilder);
 	}
 	
 	@Override
 	public void createSharedFolder(VirtualMachine virtualMachine,
 			String shareName, String hostPath, String guestPath) throws Exception {
 		
 		createSharedFolder(virtualMachine, shareName, hostPath);
 		
 		String sharedFolders = virtualMachine.getProperty(
 				VirtualMachineConstants.SHARED_FOLDERS);
 		JsonArray sharedFoldersArray = null;
 		
 		if (sharedFolders == null) {
 			sharedFoldersArray = new JsonArray();
 		} else {
 			sharedFoldersArray = (JsonArray) new JsonParser().parse(sharedFolders);
 		}
 		
 		JsonObject sfObject = new JsonObject();
 		sfObject.addProperty("name", shareName);
 		sfObject.addProperty("hostpath", hostPath);
 		sfObject.addProperty("guestpath", guestPath);
 		
 		sharedFoldersArray.add(sfObject);
 		
 		virtualMachine.setProperty(
 				VirtualMachineConstants.SHARED_FOLDERS, 
 				sharedFoldersArray.toString());
 	}
 
 	private List<String> list(boolean onlyRunning) throws Exception {
 		
 		ProcessBuilder listBuilder = getProcessBuilder(
 				"list " + (onlyRunning ? "runningvms" : "vms"));
 		ExecutionResult listResult = HypervisorUtils.runProcess(listBuilder);
 		
 		HypervisorUtils.checkReturnValue(listResult);
 		
 		List<String> vmsOutput = listResult.getStdOut();
 		List<String> vms = new LinkedList<String>();
 		
 		for (String vmOutputted : vmsOutput) {
 			vms.add(vmOutputted.substring(
 					vmOutputted.indexOf("\"") + 1, vmOutputted.lastIndexOf("\"")));
 		}
 		
 		return vms;
 	}
 	
 	@Override
 	public List<String> listVMs() throws Exception {
 		return list(false);
 	}
 
 	@Override
 	public boolean isSupported() {
 		try {
 			ProcessBuilder versionProcessBuilder = getProcessBuilder("--version");
 			HypervisorUtils.runAndCheckProcess(versionProcessBuilder);
 			return true;
 		} catch (Exception e) {
 			return false;
 		}
 	}
 
 	@Override
 	public VirtualMachineStatus status(VirtualMachine virtualMachine)
 			throws Exception {
 		
 		if (list(true).contains(virtualMachine.getName())) {
 			return VirtualMachineStatus.RUNNING;
 		}
 		
 		if (list(false).contains(virtualMachine.getName())) {
 			return VirtualMachineStatus.POWERED_OFF;
 		}
 		
 		return VirtualMachineStatus.NOT_REGISTERED;
 	}
 
 	@Override
 	public List<String> listSnapshots(VirtualMachine virtualMachine) throws Exception {
 		ProcessBuilder vmInfoBuilder = getProcessBuilder(
 				"showvminfo \"" + virtualMachine.getName() + "\" --machinereadable");
 		ExecutionResult vmInfoResult = HypervisorUtils.runProcess(vmInfoBuilder);
 		
 		HypervisorUtils.checkReturnValue(vmInfoResult);
 		
 		List<String> vmInfoOutput = vmInfoResult.getStdOut();
 		List<String> snapshots = new LinkedList<String>();
 		
 		for (String vmInfoDetail : vmInfoOutput) {
 			if (vmInfoDetail.trim().startsWith("SnapshotName")) {
 				snapshots.add(vmInfoDetail.substring(
 						vmInfoDetail.indexOf('=') + 2, vmInfoDetail.length() - 1));
 			}
 		}
 		
 		return snapshots;
 	}
 
 	@Override
 	public List<String> listSharedFolders(VirtualMachine virtualMachine) throws Exception {
 		
 		ProcessBuilder vmInfoBuilder = getProcessBuilder(
 				"showvminfo \"" + virtualMachine.getName() + "\" --machinereadable");
 		ExecutionResult vmInfoResult = HypervisorUtils.runProcess(vmInfoBuilder);
 		
 		HypervisorUtils.checkReturnValue(vmInfoResult);
 		
 		List<String> vmInfoOutput = vmInfoResult.getStdOut();
 		List<String> sharedFolders = new LinkedList<String>();
 		
 		for (String vmInfoDetail : vmInfoOutput) {
 			if (vmInfoDetail.trim().startsWith("SharedFolderName")) {
 				sharedFolders.add(vmInfoDetail.substring(
 						vmInfoDetail.indexOf('=') + 2, vmInfoDetail.length() - 1));
 			}
 		}
 		
 		return sharedFolders;
 	}
 
 }
