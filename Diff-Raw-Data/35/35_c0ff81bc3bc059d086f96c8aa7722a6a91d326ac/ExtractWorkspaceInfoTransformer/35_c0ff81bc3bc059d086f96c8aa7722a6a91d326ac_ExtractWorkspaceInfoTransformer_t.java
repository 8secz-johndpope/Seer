 package org.iplantc.iptol.server;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Stack;
 
 import org.iplantc.treedata.model.File;
 import org.iplantc.treedata.model.Folder;
 import org.iplantc.treedata.model.Workspace;
 import org.mule.api.transformer.TransformerException;
 import org.mule.config.i18n.MessageFactory;
 import org.mule.transformer.AbstractTransformer;
 
 public class ExtractWorkspaceInfoTransformer extends AbstractTransformer {
 
 	@Override
 	protected Object doTransform(Object source, String arg1)
 			throws TransformerException {
 		Workspace workspace;
 		if (source instanceof Workspace) {
 			workspace = (Workspace)source;
 		} else {
 			throw new TransformerException(
 				MessageFactory.createStaticMessage(
 					"Received object that was not a File or list of Files"));
 		}		
 		
 		WorkspaceInfo workspaceInfo = new WorkspaceInfo();
		FolderInfo rootFolder = folderInfoFromFolder(workspace.getRootFolder());
 		workspaceInfo.setId(workspace.getId());
 		workspaceInfo.setRootFolder(rootFolder);
 		
 		Stack<Folder> folderStack = new Stack<Folder>();
 		Map<Folder, FolderInfo> folderToFolderInfo = new HashMap<Folder, FolderInfo>();
 		folderToFolderInfo.put(workspace.getRootFolder(), rootFolder);
 		folderStack.push(workspace.getRootFolder());
 		while (!folderStack.empty()) {
 			Folder folder = folderStack.pop();
 			for (Folder subfolder: folder.getSubfolders()) {
 				folderStack.push(subfolder);
				FolderInfo folderInfo = folderInfoFromFolder(subfolder);
 				folderToFolderInfo.get(folder).getSubfolders().add(folderInfo);
 				folderToFolderInfo.put(subfolder, folderInfo);
 			}
 			for (File file: folder.getFiles()) {
				FileInfo fileInfo = fileInfoFromFile(file);
 				folderToFolderInfo.get(folder).getFiles().add(fileInfo);
 			}
 		}
 		
 		return workspaceInfo;
 	}
 
	private FileInfo fileInfoFromFile(File file) {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setId(file.getId());
		fileInfo.setName(file.getName());
		fileInfo.setUploaded(file.getUploaded() == null ? 
				"" : file.getUploaded().toString());
		fileInfo.setType(file.getType() == null ? 
				"" : file.getType().getDescription());
		return fileInfo;
	}

	private FolderInfo folderInfoFromFolder(Folder folder) {
		FolderInfo folderInfo = new FolderInfo();
		folderInfo.setId(folder.getId());
		folderInfo.setLabel(folder.getLabel());
		return folderInfo;
	}

 }
