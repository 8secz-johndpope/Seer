 package fi.csc.microarray.databeans.fs;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.net.URL;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipFile;
 import java.util.zip.ZipOutputStream;
 
 import fi.csc.microarray.client.ClientApplication;
 import fi.csc.microarray.client.dialog.ChipsterDialog.DetailsVisibility;
 import fi.csc.microarray.client.dialog.DialogInfo.Severity;
 import fi.csc.microarray.client.operation.Operation;
 import fi.csc.microarray.client.operation.OperationCategory;
 import fi.csc.microarray.client.operation.OperationDefinition;
 import fi.csc.microarray.client.operation.Operation.DataBinding;
 import fi.csc.microarray.client.operation.parameter.Parameter;
 import fi.csc.microarray.databeans.DataBean;
 import fi.csc.microarray.databeans.DataFolder;
 import fi.csc.microarray.databeans.DataItem;
 import fi.csc.microarray.databeans.DataBean.Link;
 import fi.csc.microarray.exception.MicroarrayException;
 import fi.csc.microarray.util.IOUtils;
 
 
 /**
  * Saves and loads contents of a DataManager. Snapshotting is based on journaling 
  * paradigm, but due to pragmatical reasons both writing and reading are 2-pass 
  * operations, not fitting the paradigm exactly. 
  * 
  * @author Aleksi Kallio
  *
  */
 public class FSSnapshottingSession {
 
 	private final int DATA_BLOCK_SIZE = 2048;
 
 	private static final String METADATA_FILENAME = "snapshot_metadata.txt";
 	public final static String SNAPSHOT_EXTENSION = "cs";
 	private final int SNAPSHOT_VERSION = 2;
 	
 	private static final String ROOT_FOLDER_ID = "0";
 	
 	private FSDataManager manager;
 	private ClientApplication application;
 
 	private int itemIdCounter = 0;
 	private HashMap<Integer, DataItem> itemIdMap = new HashMap<Integer, DataItem>();
 	private HashMap<DataItem, Integer> reversedItemIdMap = new HashMap<DataItem, Integer>();
 
 	private int operationIdCounter = 0;
 	private HashMap<Integer, Operation> operationIdMap = new HashMap<Integer, Operation>();
 	private HashMap<Operation, Integer> reversedOperationIdMap = new HashMap<Operation, Integer>();
 
 	public FSSnapshottingSession(FSDataManager manager, ClientApplication application) {
 		this.manager = manager;
 		this.application = application;
 	}
 	
 	private ZipOutputStream cpZipOutputStream = null;
 
 	public void saveSnapshot(File snapshot) throws IOException {
 																					
 		FileOutputStream fos = new FileOutputStream(snapshot);			
 		cpZipOutputStream = new ZipOutputStream(fos);
 		boolean createdSuccesfully = false;
 		
 		try {
 
 			cpZipOutputStream.setLevel(1); // quite slow with bigger values														
 
 			// write data and gather metadata simultanously
 			StringBuffer metadata = new StringBuffer("");
 			metadata.append("VERSION " + SNAPSHOT_VERSION + "\n");
 
 			// generate all ids
			generateIdsRecursively((FSDataFolder)manager.getRootFolder());
 
 			// 1st pass, write most metadata
			saveRecursively((FSDataFolder)manager.getRootFolder(), cpZipOutputStream, metadata);
 
 			// 2nd pass for links (if written in one pass, input dependent operation parameters break when reading)
			saveLinksRecursively((FSDataFolder)manager.getRootFolder(), metadata);
 
 			writeFile(cpZipOutputStream, METADATA_FILENAME, 
 					new ByteArrayInputStream(metadata.toString().getBytes()));
 
 			cpZipOutputStream.finish();
 			cpZipOutputStream.close();
 			createdSuccesfully = true;
 			
 		} catch (RuntimeException e) {
 			// createdSuccesfully is false, so file will be deleted in finally block
 			throw e;
 			
 		} catch (IOException e) {
 			// createdSuccesfully is false, so file will be deleted in finally block
 			throw e;
 
 		} finally {
 			IOUtils.closeIfPossible(cpZipOutputStream); // called twice for normal execution, not a problem
 			if (!createdSuccesfully) {
 				snapshot.delete(); // do not leave bad session files hanging around
 			}
 		}
 	}
 	
 	private void writeFile(ZipOutputStream out, String name, InputStream in) throws IOException {
 			
 		int byteCount;
 		ZipEntry cpZipEntry = new ZipEntry(name);
 		cpZipOutputStream.putNextEntry(cpZipEntry );
 
 		byte[] b = new byte[DATA_BLOCK_SIZE];
 
 		while ( (byteCount = in.read(b, 0, DATA_BLOCK_SIZE)) != -1 ) {
 			cpZipOutputStream.write(b, 0, byteCount);
 		}
 
 		cpZipOutputStream.closeEntry() ;							
 	}
 	
	private int generateIdsRecursively(FSDataFolder folder) throws IOException {
 		
 		int dataCount = 0;
 		
 		generateId(folder);
 		
 		for (DataItem data : folder.getChildren()) {
			if (data instanceof FSDataFolder) {
				int recDataCount = generateIdsRecursively((FSDataFolder)data);
 				dataCount += recDataCount;
 				
 			} else {
 				generateId((FSDataBean)data);
 				dataCount++;
 			}
 		}
 
 		return dataCount;
 	}
 			
	private void saveRecursively(FSDataFolder folder, ZipOutputStream cpZipOutputStream, StringBuffer metadata) throws IOException {
 		
 		String folderId = fetchId(folder);
 		saveDataFolderMetadata(folder, folderId, metadata);
 		
 		for (DataItem data : folder.getChildren()) {
			if (data instanceof FSDataFolder) {
				saveRecursively((FSDataFolder)data, cpZipOutputStream, metadata);
 				
 			} else {
 				FSDataBean bean = (FSDataBean)data;
 				saveDataBeanMetadata(bean, folderId, metadata);
 
 				writeFile(cpZipOutputStream, bean.getContentFile().getName(),  
 						new FileInputStream(bean.getContentFile()));
 
 			}
 		}
 	}
 
 
	private void saveDataFolderMetadata(FSDataFolder folder, String folderId, StringBuffer metadata) {
 		metadata.append("DATAFOLDER " + folderId + "\n");
 		saveDataItemMetadata(folder, folderId, metadata);
 	}	
 	
 	private void saveDataBeanMetadata(FSDataBean bean, String folderId, StringBuffer metadata) {
 		String beanId = fetchId(bean);
 		metadata.append("DATABEAN " + beanId + " " + bean.getContentFile().getName() + "\n");
 		
 		if (bean.getOperation() != null) {
 			Operation operation = bean.getOperation();
 			String operId;
 			
 			// write operation or lookup already written
 			if (!operationIdMap.containsValue(operation) ) {
 				
 				operId = generateId(operation);
 				metadata.append("OPERATION " + operId + " " + operation.getCategoryName() + "/" + operation.getName() + "\n");
 
 				// some parameters need inputs at loading time => write these first
 				if (operation.getBindings() != null) {
 					String beanIds = "";
 					for (DataBinding binding : operation.getBindings()) {
 						beanIds += fetchId(binding.getData()) + " ";
 					}
 					metadata.append("INPUTS " + operId + " " + beanIds + "\n");
 				}
 
 				for (Parameter parameter : operation.getParameters()) {
 					metadata.append("OPERATION_PARAMETER " + operId + " " +  parameter.getName() + " " + parameter.getValue() + "\n");
 				}
 
 				// will be written in the 2nd pass
 //				for (Link type : Link.values()) {
 //					for (DataBean target : bean.getLinkTargets(type)) {
 //						String targetId = fetchId(target);				
 //						metadata.append("LINK " + type.name() + " " + beanId + " " + targetId + "\n");
 //					}
 //				}		
 
 			} else {
 				operId = reversedOperationIdMap.get(operation).toString();
 			}
 			
 			metadata.append("OUTPUT " + operId + " " + beanId + "\n");
 			
 		}
 		
 		if (bean.getNotes() != null) {
 			// remove newlines from notes, they break loading
 			metadata.append("NOTES " + beanId + " " + bean.getNotes().replace('\n', ' ') + "\n");
 		}
 		
 		if (bean.getUrl() != null) {
 			metadata.append("CACHED_URL " + beanId + " " + bean.getUrl() + "\n");			
 		}
 		
 		saveDataItemMetadata(bean, beanId, metadata);
 	}
 
 	private void saveDataItemMetadata(DataItem data, String folderId, StringBuffer metadata) {
 		metadata.append("NAME " + folderId + " " + data.getName() + "\n");
 		if (data.getParent() != null) {
 			metadata.append("CHILD " + folderId + " " + fetchId(data.getParent()) + "\n");
 		}
 	}
 
 
 	public List<DataItem> loadFromSnapshot(File snapshot, DataFolder parentFolder) throws IOException, MicroarrayException {				
 		
 		ZipFile zipFile = new ZipFile(snapshot);		
 		ZipEntry entry = null;
 		Map<String,ZipEntry> entryMap = new HashMap<String,ZipEntry>();
 		Enumeration<? extends ZipEntry> entries = zipFile.entries();
 		
 		while (entries.hasMoreElements()){
 			entry = (ZipEntry)entries.nextElement();			
 			entryMap.put(entry.getName(), entry);			
 		}
 		
 		LinkedList<DataItem> newItems = new LinkedList<DataItem>();
 		LinkedList<String> delayedProcessing = new LinkedList<String>();
 		BufferedReader metadataIn = null;
 		
 		try {
 			
 			// load metadata and data
 			metadataIn = new BufferedReader(new InputStreamReader(
 					zipFile.getInputStream(entryMap.get(METADATA_FILENAME))));
 
 			String firstLine = metadataIn.readLine(); 
 			String supportedVersionLine = "VERSION " + SNAPSHOT_VERSION;
 			if (!firstLine.contains(supportedVersionLine)) {
 				throw new RuntimeException("unsupported stored session format: should be \"" + supportedVersionLine + "\", but was \"" + firstLine.replace("\n", "") + "\"");
 			}
 
 			// 1st pass
 			for (String line = metadataIn.readLine(); line != null; line = metadataIn.readLine()) {
 
 				if (line.startsWith("DATAFOLDER ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					DataFolder folder = manager.createFolder("");
 					newItems.add(folder);
 					mapId(id, folder);
 					
 				} else if (line.startsWith("DATABEAN ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					ZipEntry beanEntry = entryMap.get(split[2]);
 					InputStream inputStream = zipFile.getInputStream(beanEntry);
 					DataBean bean = manager.createDataBean("<empty>", inputStream);
 					
 					newItems.add(bean);
 					mapId(id, bean);
 					
 				} else if (line.startsWith("NAME ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					String name = afterNthSpace(line, 2);
 					DataItem item = fetchItem(id);
 					item.setName(name);
 					if (item instanceof DataBean) {
 						// update content type now that we have the real filename available (this is needed!)
 						DataBean bean = (DataBean)item;
 						bean.setContentType(manager.guessContentType(name));
 					}
 					
 				} else if (line.startsWith("OPERATION ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					String[] opData = afterNthSpace(line, 2).split("/");	
 					OperationDefinition od = application.locateOperationDefinition(opData[0], opData[1]);
 					Operation op = null;
 					if (od == null) {
 						// create local operation definition object
 						od = new OperationDefinition(opData[1], new OperationCategory(opData[0]), "", false);
 						
 						// warn if it was a real operation
 						if (!OperationCategory.isPseudoCategory(od.getCategory())) {
 							String message = "The session you opened contains a dataset which has been derived using an analysis tool which has been removed or renamed.\n\n" +
 							"The dataset contents have not changed and you can use them as before, but the obsolete operation will not be usable in workflows.";
 							String details = "Analysis tool: " + od.getCategoryName() + " / " + od.getName() + "\n";
 							warnAboutObsoleteContent(message, details, null);
 						}
 					}
 					op = new Operation(od, new DataBean[] { /* empty inputs currently */});
 					mapId(id, op);
 
 				} else if (line.startsWith("OPERATION_PARAMETER ")) {
 					delayedProcessing.add(line); // process after derivation links are in place
 
 				} else if (line.startsWith("OUTPUT ")) {
 					String[] split = line.split(" ");
 					String operId = split[1];
 					Operation operation = fetchOperation(operId);
 					String beanId = split[2];
 					DataBean bean = (DataBean)fetchItem(beanId);
 					bean.setOperation(operation);
 
 				} else if (line.startsWith("INPUTS ")) {
 					delayedProcessing.add(line); // process after derivation links are in place
 
 				} else if (line.startsWith("CHILD ")) {
 					String[] split = line.split(" ");
 					String childId = split[1];
 					String parentId = split[2];
 					
 					DataFolder parent;
 					if (parentId.equals(ROOT_FOLDER_ID)) {
 						parent = manager.getRootFolder();
 					} else {
 						parent = (DataFolder)fetchItem(parentId);
 					}
 					
 					DataItem child = fetchItem(childId);
 					parent.addChild(child);
 					
 				} else if (line.startsWith("NOTES ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					String notes = afterNthSpace(line, 2);
 					DataBean item = (DataBean)fetchItem(id);
 					item.setNotes(notes);
 
 				} else if (line.startsWith("CACHED_URL ")) {
 					String[] split = line.split(" ");
 					String id = split[1];
 					String url = split[2];
 					FSDataBean bean = (FSDataBean)fetchItem(id);
 					bean.setContentChanged(false);
 					bean.setUrl(new URL(url));
 					
 				} else if (line.startsWith("LINK ")) {
 					String[] split = line.split(" ");
 					Link link= Link.valueOf(split[1]);
 					String fromId = split[2];
 					String toId = split[3];
 					DataBean from = (DataBean)fetchItem(fromId);
 					DataBean to = (DataBean)fetchItem(toId);
 					
 					// to be compatible with older session files that have duplicate links
 					// check for duplicity here
 					boolean exists = false;
 					for (DataBean target : from.getLinkTargets(link)) {
 						if (target == to) {
 							// this link already exists, do not add it again
 							exists = true;
 							break;
 						}
 					}
 
 					if (!exists) {
 						from.addLink(link, to);
 					}
 
 				} else {
 					throw new RuntimeException("metadata error in " + snapshot.getCanonicalPath() + ": line could not be processed \"" + line + "\"");
 				}				
 			}
 			
 		} finally {
 			if (metadataIn != null) {
 				metadataIn.close();
 			}
 		}
 		
 		// 2nd pass
 		for (String line : delayedProcessing) {
 			if (line.startsWith("OPERATION_PARAMETER ")) {
 				String[] split = line.split(" ", 4); // split to (max.) 4 pieces, i.e., do no skip trailing whitespace (happens when paramValue is empty)  
 				String operId = split[1];
 				String paramName = split[2];
 				String paramValue = split[3];
 				Operation operation = fetchOperation(operId);
 				Parameter parameter = operation.getParameter(paramName);
 				if (parameter != null) {
 					try {
 						parameter.parseValue(paramValue);
 					} catch (IllegalArgumentException e) {
 						String message = "The session you opened contains a dataset with a parameter that references to an another dataset that was removed." +
 						"Typically this happens when you break the connection between phenodata and datasets that it describes. " +
 						"The dataset contents have not changed and you can use them as before, but the obsolete parameter has been removed from the history information of the dataset " +						
 						"and will not be saved in further sessions or workflows.";
 						String details = "Analysis tool: " + operation.getCategoryName() + " / " + operation.getName() + "\nParameter with obsolete reference: " + paramName;
 						warnAboutObsoleteContent(message, details, "");						
 					}
 				} else {
 					String message = "The session you opened contains a dataset which has been derived using an analysis tool with a parameter which has been removed or renamed.\n\n" +
 					"The dataset contents have not changed and you can use them as before, but the obsolete parameter has been removed from the history information of the dataset " +						
 					"and will not be saved in further sessions or workflows.";
 					String details = "Analysis tool: " + operation.getCategoryName() + " / " + operation.getName() + "\nObsolete parameter: " + paramName;
 					String dataName = null;
 					if (operation.getBindings() != null) {
 						if (operation.getBindings().size() == 1) {
 							dataName = operation.getBindings().get(0).getData().getName();
 						}
 					}
 					warnAboutObsoleteContent(message, details, dataName);
 				}
 				
 			} else if (line.startsWith("INPUTS ")) {
 				String[] split = line.split(" ");
 				String operId = split[1];
 				Operation operation = fetchOperation(operId);
 
 				LinkedList<DataBean> inputs = new LinkedList<DataBean>();
 				for (int i = 2; i < split.length; i++) {
 					String beanId = split[i];
 					DataBean bean = (DataBean)fetchItem(beanId);
 					if (bean.queryFeatures("/phenodata/").exists()) {
 						continue; // skip phenodata, it is bound automatically
 					}
 					inputs.add(bean);
 				}
 				operation.bindInputs(inputs.toArray(new DataBean[] {}));
 
 			} else {
 				throw new RuntimeException("internal error, cannot parse: " + line);
 			}
 		}
 		
 		return newItems;		
 	}
 
 	
	private void saveLinksRecursively(FSDataFolder folder, StringBuffer metadata) {
 		
 		for (DataItem data : folder.getChildren()) {
 			
			if (data instanceof FSDataFolder) {
				saveLinksRecursively((FSDataFolder)data, metadata);
 				
 			} else {
 				DataBean bean = (DataBean)data; 
 				for (Link type : Link.values()) {
 					for (DataBean target : bean.getLinkTargets(type)) {
 						String beanId = fetchId(bean);
 						String targetId = fetchId(target);				
 						metadata.append("LINK " + type.name() + " " + beanId + " " + targetId + "\n");
 					}
 				}		
 			}
 		}		
 	}
 
 	private void warnAboutObsoleteContent(String message, String details, String dataName) {
 		String title = "Obsolete content in the session";
 		String inputDataDesc = dataName != null ? ("When loading dataset " + dataName + ":\n") : "";
 		String completeDetails = inputDataDesc + details; 
 		application.showDialog(title, message, completeDetails, Severity.INFO, true, DetailsVisibility.DETAILS_ALWAYS_VISIBLE);
 	}
 
 	private static String afterNthSpace(String line, int nth) {
 		int from = 0;
 		for (int i = 0; i < nth; i++) {
 			from = line.indexOf(" ", from + 1);
 		}
 		return line.substring(from + 1);
 	}
 	
 	private void mapId(String id, DataItem item) {
 		Integer iid = Integer.parseInt(id);
 		itemIdMap.put(iid, item);
 		reversedItemIdMap.put(item, iid);		
 	}
 
 	private void mapId(String id, Operation operation) {
 		Integer iid = Integer.parseInt(id);
 		operationIdMap.put(iid, operation);
 		reversedOperationIdMap.put(operation, iid);		
 	}
 
 	private void generateId(DataItem data) {
 		Integer id = itemIdCounter++;
 		itemIdMap.put(id, data);
 		reversedItemIdMap.put(data, id);
 	}
 
 	private String generateId(Operation operation) {
 		Integer id = operationIdCounter++;
 		operationIdMap.put(id, operation);
 		reversedOperationIdMap.put(operation, id);
 		return id.toString();
 	}
 
 	private DataItem fetchItem(String id) {
 		Integer iid = Integer.parseInt(id);
 		return itemIdMap.get(iid);		
 	}
 
 	private Operation fetchOperation(String id) {
 		Integer iid = Integer.parseInt(id);
 		return operationIdMap.get(iid);		
 	}
 
 	private String fetchId(DataItem item) {
 		return reversedItemIdMap.get(item).toString();
 	}
 
 }
