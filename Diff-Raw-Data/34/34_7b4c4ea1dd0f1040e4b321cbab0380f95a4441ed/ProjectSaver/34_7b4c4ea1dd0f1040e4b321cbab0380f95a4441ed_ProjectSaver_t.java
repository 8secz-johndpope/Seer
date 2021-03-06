 package org.caleydo.core.serialize;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Marshaller;
 
 import org.caleydo.core.data.collection.set.LoadDataParameters;
 import org.caleydo.core.data.graph.tree.Tree;
 import org.caleydo.core.data.graph.tree.TreePorter;
 import org.caleydo.core.data.selection.ContentVAType;
 import org.caleydo.core.data.selection.ContentVirtualArray;
 import org.caleydo.core.data.selection.StorageVAType;
 import org.caleydo.core.data.selection.StorageVirtualArray;
 import org.caleydo.core.data.selection.VirtualArray;
 import org.caleydo.core.manager.GeneralManager;
 import org.caleydo.core.manager.datadomain.ADataDomain;
 import org.caleydo.core.manager.datadomain.ASetBasedDataDomain;
 import org.caleydo.core.manager.datadomain.DataDomainManager;
 import org.caleydo.core.manager.datadomain.IDataDomain;
 import org.caleydo.core.manager.view.ViewManager;
 import org.caleydo.core.util.clusterer.ClusterNode;
 import org.caleydo.core.util.logging.Logger;
 import org.caleydo.core.util.system.FileOperations;
 import org.caleydo.core.view.IView;
 import org.caleydo.core.view.opengl.canvas.AGLView;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Status;
 import org.osgi.framework.Bundle;
 
 /**
  * Serializes the current state of the application into a directory or file.
  * 
  * @author Alexander Lex
  * @author Werner Puff
  */
 public class ProjectSaver {
 
 	/** full path to directory to temporarily store the projects file before zipping */
 	public static final String TEMP_PROJECT_DIR_NAME = GeneralManager.CALEYDO_HOME_PATH + "tempSave"
 		+ File.separator;
 
 	/** full path to directory of the recently open project */
 	public static final String RECENT_PROJECT_DIR_NAME = GeneralManager.CALEYDO_HOME_PATH + "recent_project"
 		+ File.separator;
 
 	/** file name of the set-data-file in project-folders */
 	public static final String SET_DATA_FILE_NAME = "data.csv";
 
 	/** file name of the datadomain-file in project-folders */
 	public static final String DATA_DOMAIN_FILE_NAME = "datadomain.xml";
 
 	/** File name of file where list of plugins are to be stored */
 	public static final String PLUG_IN_LIST_FILE_NAME = "plugins.xml";
 
 	/** file name of the view-file in project-folders */
 	public static final String VIEWS_FILE_NAME = "views.xml";
 
 	/** file name of the gene-cluster-file in project-folders */
 	public static final String GENE_TREE_FILE_NAME = "gene_cluster.xml";
 
 	/** file name of the experiment-cluster-file in project-folders */
 	public static final String EXP_TREE_FILE_NAME = "experiment_cluster.xml";
 
 	/**
 	 * Saves the project into a specified zip-archive.
 	 * 
 	 * @param fileName
 	 *            name of the file to save the project in.
 	 */
 	public void save(String fileName) {
 		ZipUtils zipUtils = new ZipUtils();
 		prepareDirectory(TEMP_PROJECT_DIR_NAME);
 		savePluginData(TEMP_PROJECT_DIR_NAME);
 		saveProjectData(TEMP_PROJECT_DIR_NAME);
 		saveViewData(TEMP_PROJECT_DIR_NAME);
 		zipUtils.zipDirectory(TEMP_PROJECT_DIR_NAME, fileName);
 
 		zipUtils.deleteDirectory(TEMP_PROJECT_DIR_NAME);
 	}
 
 	/**
 	 * Saves the project to the directory for the recent project
 	 */
 	public void saveRecentProject() {
		// ZipUtils zipUtils = new ZipUtils();
		// zipUtils.deleteDirectory(RECENT_PROJECT_DIR_NAME);
 		prepareDirectory(RECENT_PROJECT_DIR_NAME);
 		savePluginData(RECENT_PROJECT_DIR_NAME);
 		saveProjectData(RECENT_PROJECT_DIR_NAME);
 
 		// remove saveViewData() for LAZY_VIEW_LOADING
 		saveViewData(RECENT_PROJECT_DIR_NAME);
 	}
 
 	private void prepareDirectory(String dirName) {
 		if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
 			dirName += File.separator;
 		}
 
 		File tempDirFile = new File(dirName);
 		tempDirFile.mkdir();
 	}
 
 	/**
 	 * Save which plug-ins were loaded
 	 * 
 	 * @param dirName
 	 */
 	private void savePluginData(String dirName) {
 		PlugInList plugInList = new PlugInList();
 
 		for (Bundle bundle : Platform.getBundle("org.caleydo.core").getBundleContext().getBundles()) {
 			if (bundle.getSymbolicName().contains("org.caleydo") && bundle.getState() == Bundle.ACTIVE)
 				plugInList.plugIns.add(bundle.getSymbolicName());
 		}
 
 		File pluginFile = new File(dirName + PLUG_IN_LIST_FILE_NAME);
 		try {
 			JAXBContext context = JAXBContext.newInstance(PlugInList.class);
 			Marshaller marshaller = context.createMarshaller();
 			marshaller.marshal(plugInList, pluginFile);
 		}
 		catch (JAXBException ex) {
 			Logger.log(new Status(Status.ERROR, this.toString(), "Could not serialize plug-in names: "
 				+ plugInList.toString(), ex));
 			ex.printStackTrace();
 		}
 
 	}
 
 	/**
 	 * Saves the project to the directory with the given name. The directory is created before saving.
 	 * 
 	 * @param dirName
 	 *            directory to save the project-files into
 	 */
 	private void saveProjectData(String dirName) {
 
 		saveDataDomains(dirName);
 	}
 
 	private void saveDataDomains(String dirName) {
 
 		SerializationManager serializationManager = GeneralManager.get().getSerializationManager();
 		JAXBContext projectContext = serializationManager.getProjectContext();
 
 		try {
 			Marshaller marshaller = projectContext.createMarshaller();
 
 			File dataDomainFile = new File(dirName + DATA_DOMAIN_FILE_NAME);
 
 			ArrayList<ADataDomain> dataDomains = new ArrayList<ADataDomain>();
 			for (IDataDomain dataDomain : DataDomainManager.get().getDataDomains()) {
 				dataDomains.add((ADataDomain) dataDomain);
 			}
 			DataDomainList dataDomainList = new DataDomainList();
 			dataDomainList.setDataDomains(dataDomains);
 
 			marshaller.marshal(dataDomainList, dataDomainFile);
 
 			for (IDataDomain dataDomain : DataDomainManager.get().getDataDomains()) {
 
 				if (dataDomain instanceof ASetBasedDataDomain) {
 
 					LoadDataParameters parameters = dataDomain.getLoadDataParameters();
 					try {
 						FileOperations.writeInputStreamToFile(dirName + SET_DATA_FILE_NAME, GeneralManager
 							.get().getResourceLoader().getResource(parameters.getFileName()));
 					}
 					catch (FileNotFoundException e) {
 						throw new IllegalStateException("Error saving project file", e);
 					}
 
 					ASetBasedDataDomain setBasedDataDomain = (ASetBasedDataDomain) dataDomain;
 
 					for (ContentVAType type : ContentVAType.getRegisteredVATypes()) {
 						saveContentVA(marshaller, dirName, setBasedDataDomain, type);
 					}
 
 					for (StorageVAType type : StorageVAType.getRegisteredVATypes()) {
 						saveStorageVA(marshaller, dirName, setBasedDataDomain, type);
 					}
 					TreePorter treePorter = new TreePorter();
 					Tree<ClusterNode> geneTree =
 						setBasedDataDomain.getSet().getContentData(ContentVAType.CONTENT).getContentTree();
 					if (geneTree != null) {
 						treePorter.exportTree(dirName + GENE_TREE_FILE_NAME, geneTree);
 					}
 
 					treePorter = new TreePorter();
 					Tree<ClusterNode> expTree =
 						setBasedDataDomain.getSet().getStorageData(StorageVAType.STORAGE).getStorageTree();
 					if (expTree != null) {
 						treePorter.exportTree(dirName + EXP_TREE_FILE_NAME, expTree);
 					}
 				}
 			}
 		}
 		catch (JAXBException ex) {
 			throw new RuntimeException("Error saving project files (xml serialization)", ex);
 		}
 		catch (IOException ex) {
 			throw new RuntimeException("Error saving project files (file access)", ex);
 		}
 	}
 
 	/**
 	 * Saves all the view's serialized forms to the given directory. The directory must exist.
 	 * 
 	 * @param dirName
 	 *            name of the directory to save the views to.
 	 */
 	private void saveViewData(String dirName) {
 		SerializationManager serializationManager = GeneralManager.get().getSerializationManager();
 		JAXBContext projectContext = serializationManager.getProjectContext();
 
 		try {
 			Marshaller marshaller = projectContext.createMarshaller();
 			ViewList storeViews = createStoreViewList();
 			File viewFile = new File(dirName + VIEWS_FILE_NAME);
 			marshaller.marshal(storeViews, viewFile);
 		}
 		catch (JAXBException ex) {
 			throw new RuntimeException("Error saving view files (xml serialization)", ex);
 		}
 	}
 
 	/**
 	 * Creates a {@link ViewList} of all views registered in the central {@link ViewManager}.
 	 * 
 	 * @return {@link ViewList} to storing the view's state.
 	 */
 	private ViewList createStoreViewList() {
 		ArrayList<String> storeViews = new ArrayList<String>();
 
 		ViewManager viewManager = GeneralManager.get().getViewGLCanvasManager();
 
 		Collection<AGLView> glViews = viewManager.getAllGLViews();
 		for (AGLView glView : glViews) {
 			if (!glView.isRenderedRemote()) {
 				ASerializedView serView = glView.getSerializableRepresentation();
 				if (!(serView instanceof SerializedDummyView)) {
 					storeViews.add(serView.getViewType());
 				}
 			}
 		}
 
 		Collection<IView> swtViews = viewManager.getAllItems();
 		for (IView swtView : swtViews) {
 			ASerializedView serView = swtView.getSerializableRepresentation();
 			if (!(serView instanceof SerializedDummyView)) {
 				storeViews.add(serView.getViewType());
 			}
 		}
 
 		ViewList viewList = new ViewList();
 		viewList.setViews(storeViews);
 
 		return viewList;
 	}
 
 	/**
 	 * Saves the {@link VirtualArray} of the given type. The filename is created from the type.
 	 * 
 	 * @param dir
 	 *            directory to save the {@link VirtualArray} in.
 	 * @param useCase
 	 *            {@link IDataDomain} to retrieve the {@link VirtualArray} from.
 	 * @param type
 	 *            type of the virtual array within the given {@link IDataDomain}.
 	 */
 	private void saveContentVA(Marshaller marshaller, String dir, ASetBasedDataDomain dataDomain,
 		ContentVAType type) throws JAXBException {
 		String fileName = dir + "va_" + type.toString() + ".xml";
 		ContentVirtualArray va = (ContentVirtualArray) dataDomain.getContentVA(type);
 		marshaller.marshal(va, new File(fileName));
 	}
 
 	private void saveStorageVA(Marshaller marshaller, String dir, ASetBasedDataDomain dataDomain,
 		StorageVAType type) throws JAXBException {
 		String fileName = dir + "va_" + type.toString() + ".xml";
 		StorageVirtualArray va = (StorageVirtualArray) dataDomain.getStorageVA(type);
 		marshaller.marshal(va, new File(fileName));
 	}
 
 }
 
 // String geneTreePath = tempDirectory + "/bgene_tree.xml";
 
 // ISet set = GeneralManager.get().getUseCase().getSet();
 
 // SetExporter exporter = new SetExporter();
 // exporter.export(set, exportedData, EWhichViewToExport.WHOLE_DATA);
 //
 // exporter.exportTrees(set, tempDirectory);
