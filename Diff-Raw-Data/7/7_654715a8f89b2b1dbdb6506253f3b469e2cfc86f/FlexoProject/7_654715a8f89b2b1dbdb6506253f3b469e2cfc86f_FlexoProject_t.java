 /*
  * (c) Copyright 2010-2011 AgileBirds
  *
  * This file is part of OpenFlexo.
  *
  * OpenFlexo is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenFlexo is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OpenFlexo. If not, see <http://www.gnu.org/licenses/>.
  *
  */
 package org.openflexo.foundation.rm;
 
 import java.awt.Image;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.UnsupportedEncodingException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.StringTokenizer;
 import java.util.TreeMap;
 import java.util.Vector;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Pattern;
 import java.util.zip.Deflater;
 
 import javax.imageio.ImageIO;
 import javax.naming.InvalidNameException;
 import javax.swing.ImageIcon;
 
 import org.openflexo.foundation.CodeType;
 import org.openflexo.foundation.DataModification;
 import org.openflexo.foundation.DocType;
 import org.openflexo.foundation.DocType.DefaultDocType;
 import org.openflexo.foundation.FlexoEditor;
 import org.openflexo.foundation.FlexoEditor.FlexoEditorFactory;
 import org.openflexo.foundation.FlexoModelObject;
 import org.openflexo.foundation.FlexoObject;
 import org.openflexo.foundation.FlexoObserver;
 import org.openflexo.foundation.Inspectors;
 import org.openflexo.foundation.TemporaryFlexoModelObject;
 import org.openflexo.foundation.bindings.AbstractBinding;
 import org.openflexo.foundation.bindings.AbstractBinding.AbstractBindingStringConverter;
 import org.openflexo.foundation.bindings.BindingAssignment;
 import org.openflexo.foundation.bindings.BindingAssignment.BindingAssignmentStringConverter;
 import org.openflexo.foundation.bindings.BindingDefinition.BindingDefinitionType;
 import org.openflexo.foundation.bindings.BindingExpression.BindingExpressionStringConverter;
 import org.openflexo.foundation.bindings.BindingValue.BindingValueStringConverter;
 import org.openflexo.foundation.bindings.StaticBinding.StaticBindingStringConverter;
 import org.openflexo.foundation.bindings.TranstypedBinding.TranstypedBindingStringConverter;
 import org.openflexo.foundation.bindings.WKFBindingDefinition;
 import org.openflexo.foundation.bindings.WidgetBindingDefinition;
 import org.openflexo.foundation.cg.CGFile;
 import org.openflexo.foundation.cg.CannotRemoveLastDocType;
 import org.openflexo.foundation.cg.DocTypeAdded;
 import org.openflexo.foundation.cg.DocTypeRemoved;
 import org.openflexo.foundation.cg.DuplicateCodeRepositoryNameException;
 import org.openflexo.foundation.cg.DuplicateDocTypeException;
 import org.openflexo.foundation.cg.GeneratedCode;
 import org.openflexo.foundation.cg.GeneratedDoc;
 import org.openflexo.foundation.dkv.DKVModel;
 import org.openflexo.foundation.dkv.DKVValidationModel;
 import org.openflexo.foundation.dm.DMModel;
 import org.openflexo.foundation.dm.DMType;
 import org.openflexo.foundation.dm.DMValidationModel;
 import org.openflexo.foundation.dm.JarClassLoader;
 import org.openflexo.foundation.gen.FlexoProcessImageNotificationCenter;
 import org.openflexo.foundation.gen.ScreenshotGenerator;
 import org.openflexo.foundation.ie.IEOperationComponent;
 import org.openflexo.foundation.ie.IEPopupComponent;
 import org.openflexo.foundation.ie.IEReusableComponent;
 import org.openflexo.foundation.ie.IETabComponent;
 import org.openflexo.foundation.ie.IEValidationModel;
 import org.openflexo.foundation.ie.IEWOComponent;
 import org.openflexo.foundation.ie.OperationComponentInstance;
 import org.openflexo.foundation.ie.action.ImportImage;
 import org.openflexo.foundation.ie.cl.ComponentDefinition;
 import org.openflexo.foundation.ie.cl.FlexoComponentLibrary;
 import org.openflexo.foundation.ie.cl.OperationComponentDefinition;
 import org.openflexo.foundation.ie.cl.TabComponentDefinition;
 import org.openflexo.foundation.ie.dm.StyleSheetFolderChanged;
 import org.openflexo.foundation.ie.menu.FlexoNavigationMenu;
 import org.openflexo.foundation.ie.palette.FlexoIEBIRTPalette;
 import org.openflexo.foundation.ie.palette.FlexoIEBasicPalette;
 import org.openflexo.foundation.ie.palette.FlexoIECustomImagePalette;
 import org.openflexo.foundation.ie.palette.FlexoIECustomImagePalette.FlexoIECustomImage;
 import org.openflexo.foundation.ie.palette.FlexoIECustomWidgetPalette;
 import org.openflexo.foundation.ie.palette.FlexoIEImagePalette;
 import org.openflexo.foundation.ie.palette.FlexoIEImagePalette.FlexoIEImage;
 import org.openflexo.foundation.ie.util.DateFormatType;
 import org.openflexo.foundation.ie.widget.IEWidget;
 import org.openflexo.foundation.ontology.EditionPatternInstance;
 import org.openflexo.foundation.ontology.EditionPatternReference;
 import org.openflexo.foundation.ontology.EditionPatternReference.ConceptActorReference;
 import org.openflexo.foundation.ontology.OntologyObject;
 import org.openflexo.foundation.ontology.ProjectOWLOntology;
 import org.openflexo.foundation.ontology.ProjectOntology;
 import org.openflexo.foundation.ontology.ProjectOntologyLibrary;
 import org.openflexo.foundation.resource.FlexoResourceCenterService;
 import org.openflexo.foundation.resource.ResourceData;
 import org.openflexo.foundation.rm.FlexoResource.DependencyAlgorithmScheme;
 import org.openflexo.foundation.rm.cg.CGRepositoryFileResource;
 import org.openflexo.foundation.sg.GeneratedSources;
 import org.openflexo.foundation.stats.ProjectStatistics;
 import org.openflexo.foundation.toc.TOCData;
 import org.openflexo.foundation.toc.TOCDataBinding;
 import org.openflexo.foundation.toc.TOCRepository;
 import org.openflexo.foundation.utils.FlexoCSS;
 import org.openflexo.foundation.utils.FlexoModelObjectReference;
 import org.openflexo.foundation.utils.FlexoObjectIDManager;
 import org.openflexo.foundation.utils.FlexoProgress;
 import org.openflexo.foundation.utils.FlexoProjectFile;
 import org.openflexo.foundation.utils.ProjectLoadingCancelledException;
 import org.openflexo.foundation.utils.ProjectLoadingHandler;
 import org.openflexo.foundation.validation.CompoundIssue;
 import org.openflexo.foundation.validation.FixProposal;
 import org.openflexo.foundation.validation.FlexoImportedObjectValidationModel;
 import org.openflexo.foundation.validation.FlexoProjectValidationModel;
 import org.openflexo.foundation.validation.InformationIssue;
 import org.openflexo.foundation.validation.Validable;
 import org.openflexo.foundation.validation.ValidationError;
 import org.openflexo.foundation.validation.ValidationIssue;
 import org.openflexo.foundation.validation.ValidationModel;
 import org.openflexo.foundation.validation.ValidationReport;
 import org.openflexo.foundation.validation.ValidationRule;
 import org.openflexo.foundation.view.ViewLibrary;
 import org.openflexo.foundation.viewpoint.EditionPattern;
 import org.openflexo.foundation.viewpoint.EditionPattern.EditionPatternConverter;
 import org.openflexo.foundation.viewpoint.PatternRole;
 import org.openflexo.foundation.wkf.FlexoImportedProcessLibrary;
 import org.openflexo.foundation.wkf.FlexoProcess;
 import org.openflexo.foundation.wkf.FlexoWorkflow;
 import org.openflexo.foundation.wkf.Role;
 import org.openflexo.foundation.wkf.RoleList;
 import org.openflexo.foundation.wkf.Status;
 import org.openflexo.foundation.wkf.WKFArtefact;
 import org.openflexo.foundation.wkf.WKFObject;
 import org.openflexo.foundation.wkf.WKFValidationModel;
 import org.openflexo.foundation.wkf.dm.WKFAttributeDataModification;
 import org.openflexo.foundation.wkf.edge.FlexoPostCondition;
 import org.openflexo.foundation.wkf.node.AbstractActivityNode;
 import org.openflexo.foundation.wkf.node.OperationNode;
 import org.openflexo.foundation.ws.FlexoWSLibrary;
 import org.openflexo.foundation.xml.FlexoXMLMappings;
 import org.openflexo.inspector.InspectableObject;
 import org.openflexo.kvc.KVCObject;
 import org.openflexo.localization.FlexoLocalization;
 import org.openflexo.module.external.IModuleLoader;
 import org.openflexo.toolbox.FileCst;
 import org.openflexo.toolbox.FileResource;
 import org.openflexo.toolbox.FileUtils;
 import org.openflexo.toolbox.FlexoVersion;
 import org.openflexo.toolbox.ToolBox;
 import org.openflexo.toolbox.WRLocator;
 import org.openflexo.toolbox.ZipUtils;
 import org.openflexo.xmlcode.StringConvertable;
 import org.openflexo.xmlcode.StringEncoder;
 import org.openflexo.xmlcode.StringEncoder.Converter;
 import org.openflexo.xmlcode.XMLMapping;
 
 /**
  * This class represents a Flexo project. A FlexoProject is composed of FlexoResources which are generally located in a project directory. A
  * FlexoProject is the entry point to navigate through the whole data of the project.
  * {@link org.openflexo.foundation.FlexoProject#getFlexoWorkflow()} give an access to all data related to processes.
  * {@link org.openflexo.foundation.FlexoProject#getDataModel()} give an access to all data related to the data model.
  * {@link org.openflexo.foundation.FlexoProject#getDKVModel()} give an access to all data related to the key values lists (defined in
  * Interface Editor). {@link org.openflexo.foundation.FlexoProject#getFlexoComponentLibrary()} give an access to all data related to the
  * screens. {@link org.openflexo.foundation.FlexoProject#getFlexoNavigationMenu()} give an access to all data related to the navigation
  * menus (defined in Interface Editor). {@link org.openflexo.foundation.FlexoProject#getFlexoWSLibrary()} give an access to all data related
  * to the WebServices imported in this project.
  * 
  * @author sguerin
  */
 public class FlexoProject extends FlexoModelObject implements XMLStorageResourceData, InspectableObject, Validable,
 		Iterable<FlexoResource<? extends FlexoResourceData>>, ResourceData<FlexoProject> {
 
 	private static final String REVISION = "revision";
 	private static final String VERSION = "version";
 	private static final String FRAMEWORKS_DIRECTORY = "Frameworks";
 	private static final String HTML_DIRECTORY = "HTML";
 	private static final String DOCX_DIRECTORY = "Docx";
 	private static final String LATEX_DIRECTORY = "Latex";
 	public static final String PROCESS_SNAPSHOT_LOCAL_DIRECTORY = "ProcessSnapshotLocal";
 	private static final String PROCESS_SNAPSHOT_IMPORTED_DIRECTORY = "ProcessSnapshotImported";
 	private static final String PROJECT_DOCUMENTATION_CSS_FILE = "FlexoDocumentationMasterStyle.css";
 
 	public static final File INITIAL_IMAGES_DIR = new FileResource("Config/InitialImages");
 
 	private boolean computeDiff = true;
 
 	private boolean timestampsHaveBeenLoaded = false;
 
 	private FlexoResourceManager resourceManagerInstance;
 
 	private FlexoXMLMappings xmlMappings;
 
 	private FlexoResourceCenterService resourceCenterService;
 
 	private FlexoObjectIDManager objectIDManager;
 
 	private List<FlexoModelObjectReference> objectReferences = new ArrayList<FlexoModelObjectReference>();
 	/**
 	 * These variable are here to replace old static references.
 	 */
 	private final Map<IEWidget, Hashtable<String, WidgetBindingDefinition>> widgetBindingDefinitions = new Hashtable<IEWidget, Hashtable<String, WidgetBindingDefinition>>();
 	private final Map<WKFObject, Hashtable<String, WKFBindingDefinition>> wkfObjectBindingDefinitions = new Hashtable<WKFObject, Hashtable<String, WKFBindingDefinition>>();
 	private List<FlexoModelObject> allRegisteredObjects;
 	private List<FlexoModelObject> _recentlyCreatedObjects;
 	private final Hashtable<String, Status> globalStatus = new Hashtable<String, Status>();
 	protected AbstractBindingStringConverter<AbstractBinding> abstractBindingConverter = new AbstractBinding.AbstractBindingStringConverter<AbstractBinding>(
 			AbstractBinding.class, this);
 	protected BindingValueStringConverter bindingValueConverter = new BindingValueStringConverter(abstractBindingConverter);
 	protected ImageFileConverter imageFileConverter = new ImageFileConverter();
 	protected BindingExpressionStringConverter bindingExpressionConverter = new BindingExpressionStringConverter();
 	protected StaticBindingStringConverter staticBindingConverter = new StaticBindingStringConverter();
 	protected TranstypedBindingStringConverter transtypedBindingConverter = new TranstypedBindingStringConverter(abstractBindingConverter);
 	protected BindingAssignmentStringConverter bindingAssignmentConverter = new BindingAssignment.BindingAssignmentStringConverter(this);
 	protected FlexoModelObjectReferenceConverter objectReferenceConverter = new FlexoModelObjectReferenceConverter();
 
 	private boolean lastUniqueIDHasBeenSet = false;
 	private long lastID = Integer.MIN_VALUE;
 
 	protected static final Logger logger = Logger.getLogger(FlexoProject.class.getPackage().getName());
 
 	// " | ? * [ ] / < > = { } & % # ~ \ _
 	public static final String BAD_CHARACTERS_REG_EXP = "[\"|\\?\\*\\[\\]/<>:{}&%#~\\\\_]";
 
 	public static final Pattern BAD_CHARACTERS_PATTERN = Pattern.compile(BAD_CHARACTERS_REG_EXP);
 
 	protected transient FlexoRMResource _resource;
 
 	protected String projectName;
 
 	protected File projectDirectory;
 
 	private FlexoCSS cssSheet;
 
 	private CodeType targetType;
 
 	private List<File> filesToDelete;
 
 	private String projectDescription = FlexoLocalization.localizedForKey("no_description");
 
 	private long firstOperationFlexoID = -1;
 
 	private OperationNode firstOperation;
 
 	private List<DocType> docTypes;
 
 	private ProjectStatistics statistics = null;
 
 	private String version = "1.0";
 
 	private long revision = 0;
 
 	/**
 	 * Hashtable where all the known resources are stored, with associated key which is a String identifying the resource
 	 * (resourceIdentifier)
 	 */
 	protected transient ResourceHashtable resources;
 
 	private FlexoProjectValidationModel projectValidationModel;
 	private FlexoImportedObjectValidationModel importedObjectValidationModel;
 
 	private static int ID = 0;
 
 	private final int id;
 
 	private Date creationDate;
 	private String creationUserId;
 	private String projectURI;
 	private String projectVersionURI;
 	private String docTypesAsString;
 
 	private boolean holdObjectRegistration = false;
 
 	private ProjectLoadingHandler loadingHandler;
 
 	public int getID() {
 		return id;
 	}
 
 	private List<FlexoEditor> editors;
 
 	private final StringEncoder stringEncoder;
 
 	private FlexoIEBasicPalette basicPalette;
 	private FlexoIEImagePalette imagePalette;
 	private FlexoIECustomWidgetPalette customWidgetPalette;
 	private FlexoIECustomImagePalette customImagePalette;
 	private FlexoIEBIRTPalette birtPalette;
 
 	private IModuleLoader moduleLoader;
 
 	private class ResourceHashtable extends TreeMap<String, FlexoResource<? extends FlexoResourceData>> {
 		public ResourceHashtable() {
 			super();
 		}
 
 		public ResourceHashtable(Map<String, FlexoResource<? extends FlexoResourceData>> ht) {
 			super(ht);
 		}
 
 		protected void restoreKeys() {
 			List<FlexoResource<? extends FlexoResourceData>> v = new ArrayList<FlexoResource<? extends FlexoResourceData>>();
 			Iterator<String> i = keySet().iterator();
 			while (i.hasNext()) {
 				String key = i.next();
 				v.add(get(key));
 			}
 			clear();
 			for (FlexoResource<? extends FlexoResourceData> r : v) {
 				put(r.getResourceIdentifier(), r);
 			}
 		}
 
 	}
 
 	public static interface FlexoProjectReferenceLoader {
 
 		public void loadProjects(List<FlexoProjectReference> references) throws ProjectLoadingCancelledException;
 	}
 
 	protected class FlexoModelObjectReferenceConverter extends Converter<FlexoModelObjectReference> {
 
 		public FlexoModelObjectReferenceConverter() {
 			super(FlexoModelObjectReference.class);
 		}
 
 		@Override
 		public FlexoModelObjectReference<FlexoModelObject> convertFromString(String value) {
 			return new FlexoModelObjectReference<FlexoModelObject>(FlexoProject.this, value);
 		}
 
 		@Override
 		public String convertToString(FlexoModelObjectReference value) {
 			return value.getStringRepresentation();
 		}
 
 	}
 
 	protected class FlexoProjectStringEncoder extends StringEncoder {
 		@Override
 		public void _initialize() {
 			super._initialize();
 			FlexoObject.initialize(this);
 			_addConverter(bindingValueConverter);
 			_addConverter(bindingExpressionConverter);
 			_addConverter(abstractBindingConverter);
 			_addConverter(staticBindingConverter);
 			_addConverter(bindingAssignmentConverter);
 			_addConverter(objectReferenceConverter);
 			_addConverter(imageFileConverter);
 			_addConverter(TOCDataBinding.CONVERTER);
 		}
 	}
 
 	/**
 	 *
 	 */
 	private FlexoProject() {
 		super(null);
 		xmlMappings = new FlexoXMLMappings();
 		stringEncoder = new FlexoProjectStringEncoder();
 		stringEncoder._initialize();
 		// Just to be sure, we initialize them here
 		if (allRegisteredObjects == null) {
 			allRegisteredObjects = new Vector<FlexoModelObject>();
 		}
 		if (_recentlyCreatedObjects == null) {
 			_recentlyCreatedObjects = new Vector<FlexoModelObject>();
 		}
 		editors = new Vector<FlexoEditor>();
 		synchronized (FlexoProject.class) {
 			id = ID++;
 		}
 		logger.info("Create new project, ID=" + id);
 		_externalRepositories = new Vector<ProjectExternalRepository>();
 		repositoriesCache = new Hashtable<String, ProjectExternalRepository>();
 		filesToDelete = new Vector<File>();
 		resources = new ResourceHashtable();
 		docTypes = new Vector<DocType>();
 	}
 
 	/**
 	 * Constructor used for XML Serialization: never try to instanciate project from this constructor
 	 * 
 	 * @param builder
 	 */
 	public FlexoProject(FlexoProjectBuilder builder) {
 		this();
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Deserialization for FlexoProject started");
 		}
 		builder.project = this;
 		setResourceCenter(builder.resourceCenterService);
 		setProjectDirectory(builder.projectDirectory);
 		loadingHandler = builder.loadingHandler;
 		initializeDeserialization(builder);
 	}
 
 	protected FlexoProject(File aProjectDirectory) {
 		this();
 		setProjectDirectory(aProjectDirectory);
 		projectName = aProjectDirectory.getName().replaceAll(BAD_CHARACTERS_REG_EXP, " ");
 		if (projectName.endsWith(".prj")) {
 			projectName = projectName.substring(0, projectName.length() - 4);
 		} else {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Project directory does not end with '.prj'");
 			}
 		}
 	}
 
 	public CodeType getTargetType() {
 		if (targetType == null) {
 			targetType = CodeType.PROTOTYPE;
 		}
 		return targetType;
 	}
 
 	public void setTargetType(CodeType targetType) {
 		CodeType old = this.targetType;
 		if (old != targetType) {
 			this.targetType = targetType;
 			setChanged();
 			notifyObservers(new DataModification("targetType", old, targetType));
 			_ieValidationModel = null;
 			_dmValidationModel = null;
 			_wkfValidationModel = null;
 			_dkvValidationModel = null;
 			projectValidationModel = null;
 		}
 	}
 
 	public FlexoRMResource getFlexoRMResource() {
 		return _resource;
 	}
 
 	@Override
 	public FlexoRMResource getFlexoResource() {
 		return _resource;
 	}
 
 	@Override
 	public FlexoRMResource getFlexoXMLFileResource() {
 		return _resource;
 	}
 
 	@Override
 	public void setFlexoResource(FlexoResource resource) throws DuplicateResourceException {
 		_resource = (FlexoRMResource) resource;
 		// registerResource(_resource);
 	}
 
 	@Override
 	public FlexoProject getProject() {
 		return this;
 	}
 
 	/**
 	 * Returns reference to the main object in which this XML-serializable object is contained relating to storing scheme: here it's the
 	 * project itself
 	 * 
 	 * @return this
 	 */
 	@Override
 	public XMLStorageResourceData getXMLResourceData() {
 		return this;
 	}
 
 	/**
 	 * Overrides setChanged
 	 * 
 	 * @see org.openflexo.foundation.FlexoXMLSerializableObject#setChanged()
 	 */
 	@Override
 	public void setChanged() {
 		super.setChanged();
 	}
 
 	/**
 	 * Save this project using ResourceManager scheme Additionnaly save all known resources related to this project
 	 * 
 	 * Overrides
 	 * 
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 */
 	@Override
 	public void save() throws SaveResourceException {
 		save(null);
 	}
 
 	public void save(FlexoProgress progress) throws SaveResourceException {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Saving project...");
 		}
 		saveModifiedResources(progress, true);
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Saving project... DONE");
 		}
 		FlexoProcessImageNotificationCenter.getInstance().notifyNewImage();
 	}
 
 	/**
 	 * Zip the current projects into the given zip file <code>zipFile</code>. If <code>lighten</code> is set to true, the project will be
 	 * lighten, i.e., jars will be emptied and screenshots will be removed.
 	 * 
 	 * @param zipFile
 	 *            the file on which to zip the project
 	 * @param lightenProject
 	 *            wheter the zipped project should be lighten of unrequired resources or not.
 	 * @throws SaveResourceException
 	 */
 	public void saveAsZipFile(File zipFile, FlexoProgress progress, boolean lightenProject, boolean copyCVSFiles)
 			throws SaveResourceException {
 		try {
 			FileUtils.createNewFile(zipFile);
 			File tempProjectDirectory = FileUtils.createTempDirectory(getProjectName().length() > 2 ? getProjectName() : "FlexoProject-"
 					+ getProjectName(), "");
 			tempProjectDirectory = new File(tempProjectDirectory, getProjectDirectory().getName());
 			saveAs(tempProjectDirectory, progress, null, false, copyCVSFiles);
 			if (lightenProject) {
 				replaceBigJarsWithEmtpyJars(progress, tempProjectDirectory);
 				removeScreenshots(progress, tempProjectDirectory);
 				removeBackupFiles(progress, tempProjectDirectory);
 			}
 			if (progress != null) {
 				progress.setProgress(FlexoLocalization.localizedForKey("zipping_project"));
 			}
 			ZipUtils.makeZip(zipFile, tempProjectDirectory, progress, null, lightenProject ? Deflater.BEST_COMPRESSION
 					: Deflater.DEFAULT_COMPRESSION);
 		} catch (IOException e) {
 			e.printStackTrace();
 			throw new SaveResourceException(null) {
 
 			};
 		}
 	}
 
 	private void removeBackupFiles(FlexoProgress progress, File tempProjectDirectory) {
 		List<File> tildes = FileUtils.listFilesRecursively(tempProjectDirectory, new FilenameFilter() {
 			@Override
 			public boolean accept(File dir, String name) {
 				return name.endsWith("~");
 			}
 		});
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("removing_backups"));
 			progress.resetSecondaryProgress(tildes.size());
 		}
 		for (File tilde : tildes) {
 			if (progress != null) {
 				progress.setSecondaryProgress(FlexoLocalization.localizedForKey("removing") + " " + tilde.getName());
 			}
 			tilde.delete();
 		}
 	}
 
 	private void removeScreenshots(FlexoProgress progress, File tempProjectDirectory) {
 		File docDir = new File(tempProjectDirectory, ProjectRestructuration.GENERATED_DOC_DIR);
 		File[] pngs = docDir.listFiles(new FilenameFilter() {
 			@Override
 			public boolean accept(File dir, String name) {
 				return name.toLowerCase().endsWith(ScreenshotResource.DOTTED_SCREENSHOT_EXTENSION);
 			}
 		});
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("removing_screenshots"));
 			progress.resetSecondaryProgress(pngs.length);
 		}
 		for (File png : pngs) {
 			if (progress != null) {
 				progress.setSecondaryProgress(FlexoLocalization.localizedForKey("removing") + " " + png.getName());
 			}
 			png.delete();
 		}
 	}
 
 	private void replaceBigJarsWithEmtpyJars(FlexoProgress progress, File tempProjectDirectory) throws IOException {
 		File dmDir = new File(tempProjectDirectory, ProjectRestructuration.DATA_MODEL_DIR);
 		File[] knownJars = new FileResource("Library/JarLibraries").listFiles(FileUtils.JARFileNameFilter);
 		File[] jars = dmDir.listFiles(FileUtils.JARFileNameFilter);
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("compressing_data"));
 			progress.resetSecondaryProgress(jars.length);
 		}
 		for (int i = 0; i < jars.length; i++) {
 			File jar = jars[i];
 			if (progress != null) {
 				progress.setSecondaryProgress(FlexoLocalization.localizedForKey("data_model"));
 			}
 			boolean isKnown = false;
 			for (File kJar : knownJars) {
 				if (kJar.getName().equals(jar.getName())) {
 					isKnown = true;
 					break;
 				}
 			}
 			if (!isKnown) {
 				continue;
 			}
 			if (jar.isFile() && jar.length() > 4096) {
 				ZipUtils.createEmptyZip(jar);
 			}
 		}
 	}
 
 	public static void restoreJarsIfNeeded(File aProjectDirectory) throws IOException {
 		File dmDir = new File(aProjectDirectory, ProjectRestructuration.DATA_MODEL_DIR);
 		if (!dmDir.exists()) {
 			return;
 		}
 		File[] knownJars = new FileResource("Library/JarLibraries").listFiles(FileUtils.JARFileNameFilter);
 		File[] jars = dmDir.listFiles(FileUtils.JARFileNameFilter);
 
 		for (File knownJar : knownJars) {
 			for (File jar : jars) {
 				if (jar.getName().equals(knownJar.getName())) {
 					if (jar.length() < knownJar.length() && jar.length() < 4096) {
 						if (logger.isLoggable(Level.INFO)) {
 							logger.info("Restoring " + jar.getName());
 						}
 						FileUtils.copyFileToFile(knownJar, jar);
 						jar.setLastModified(knownJar.lastModified());
 					}
 				}
 			}
 		}
 
 	}
 
 	public synchronized void saveAs(File newProjectDirectory, FlexoProgress progress, FlexoVersion releaseVersion,
 			boolean useNewDirectoryFromNow, boolean copyCVSFiles) throws SaveResourceException {
 		File oldProjectDirectory = getProjectDirectory();
 		boolean directoriesAreDifferent = true;
 
 		if (newProjectDirectory == null) {
 			if (logger.isLoggable(Level.SEVERE)) {
 				logger.severe("Invoked 'Save As' with a null new project directory");
 			}
 			return;
 		}
 		if (oldProjectDirectory != null) {
 			try {
 				directoriesAreDifferent = !oldProjectDirectory.getCanonicalFile().equals(newProjectDirectory.getCanonicalFile());
 			} catch (IOException e) {
 				e.printStackTrace();
 				throw new SaveResourceException(null) {
 
 				};
 			}
 		}
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Save project as " + newProjectDirectory.getAbsolutePath());
 		}
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("saving_project_as_") + newProjectDirectory.getAbsolutePath());
 			progress.resetSecondaryProgress(3);
 			progress.setSecondaryProgress(FlexoLocalization.localizedForKey("emptying directory"));
 		}
 		if (progress != null) {
 			progress.setSecondaryProgress(FlexoLocalization.localizedForKey("creating_new_location"));
 		}
 		newProjectDirectory.mkdirs();
 		resourceManagerInstance.stopResourcePeriodicChecking();
 		Map<FlexoFileResource<? extends FlexoResourceData>, Date> dateBackup = new HashMap<FlexoFileResource<? extends FlexoResourceData>, Date>();
 		if (!useNewDirectoryFromNow) {
 			for (FlexoFileResource<? extends FlexoResourceData> fileResource : getFileResources()) {
 				dateBackup.put(fileResource, fileResource.getDiskLastModifiedDate());
 			}
 		}
 		try {
 			if (directoriesAreDifferent) {
 				try {
 					if (logger.isLoggable(Level.FINE)) {
 						logger.fine("Copying all files to new location");
 					}
 					if (progress != null) {
 						progress.setSecondaryProgress(FlexoLocalization.localizedForKey("copying_files_to_new_location"));
 					}
 					if (copyCVSFiles) {
 						FileUtils.copyContentDirToDirIncludingCVSFiles(getProjectDirectory(), newProjectDirectory);
 					} else {
 						FileUtils.copyContentDirToDir(getProjectDirectory(), newProjectDirectory);
 					}
 					if (logger.isLoggable(Level.FINE)) {
 						logger.fine("Copy terminated succesfully");
 					}
 				} catch (IOException e) {
 					e.printStackTrace();
 					throw new SaveResourcePermissionDeniedException(null);
 				}
 			}
 			setProjectDirectory(newProjectDirectory, useNewDirectoryFromNow);
 			saveModifiedResources(progress, useNewDirectoryFromNow);
 
 			// Use given version if any
 			if (releaseVersion != null) {
 				getFlexoDMResource().revertToReleaseVersion(releaseVersion);
 				for (FlexoXMLStorageResource<? extends XMLStorageResourceData> r : getXMLStorageResources()) {
 					if (r != getFlexoDMResource() && r != getFlexoResource()) {
 						r.revertToReleaseVersion(releaseVersion);
 					}
 				}
 				getFlexoResource().revertToReleaseVersion(releaseVersion);
 				// Don' forget .version file
 				writeDotVersion(releaseVersion);
 			}
 			if (useNewDirectoryFromNow) {
 				for (FlexoFileResource<? extends FlexoResourceData> fileResource : getFileResources()) {
 					fileResource.hasWrittenOnDisk(null);// We reset the known dates
 				}
 			} else {
 				for (FlexoFileResource<? extends FlexoResourceData> fileResource : getFileResources()) {
 					Date date = dateBackup.get(fileResource);
 					if (date == null) {
 						date = FileUtils.getDiskLastModifiedDate(fileResource.getFile());
 					}
 					fileResource._setLastWrittenOnDisk(date);// We set the dates back to what they were
 					// so even if somebody has modified something during
 				}
 			}
 		} finally {
 			if (!useNewDirectoryFromNow) {
 				setProjectDirectory(oldProjectDirectory, false);
 			}
 			resourceManagerInstance.startResourcePeriodicChecking();
 		}
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Save as ... DONE");
 		}
 	}
 
 	/**
 	 * @param resourcesToSave
 	 * @param progress
 	 * @throws SaveResourceException
 	 */
 	public synchronized void saveStorageResources(List<FlexoStorageResource<? extends StorageResourceData>> resourcesToSave,
 			FlexoProgress progress) throws SaveResourceException {
 		for (FlexoStorageResource<? extends StorageResourceData> data : resourcesToSave) {
 			if (progress != null) {
 				progress.setSecondaryProgress(FlexoLocalization.localizedForKey("saving_resource_") + data.getName());
 			}
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine("Saving " + data.getResourceIdentifier() + " file: " + data.getFileName());
 			}
 			data.saveResourceData(true);
 		}
 	}
 
 	protected void writeDotVersion() {
 		writeDotVersion(FlexoXMLMappings.latestRelease());
 	}
 
 	private void writeDotVersion(FlexoVersion version) {
 		FileOutputStream fos = null;
 		File f = new File(projectDirectory, ".version");
 		try {
 			fos = new FileOutputStream(f);
 			fos.write(version.toString(true).getBytes("UTF-8"));
 		} catch (UnsupportedEncodingException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		} finally {
 			try {
 				if (fos != null) {
 					fos.flush();
 				}
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 			try {
 				if (fos != null) {
 					fos.close();
 				}
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 		}
 
 	}
 
 	/**
 	 * Save this project using ResourceManager scheme Additionnaly save all known resources related to this project
 	 * 
 	 * Overrides
 	 * 
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 */
 	public synchronized void saveModifiedResources(FlexoProgress progress) throws SaveResourceException {
 		saveModifiedResources(progress, true);
 	}
 
 	/**
 	 * Save this project using ResourceManager scheme Additionally save all known resources related to this project
 	 * 
 	 * Overrides
 	 * 
 	 * @param clearModifiedStatus
 	 *            TODO
 	 * 
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 */
 	public synchronized void saveModifiedResources(FlexoProgress progress, boolean clearModifiedStatus) throws SaveResourceException {
 		if (logger.isLoggable(Level.FINE)) {
 			logger.fine("Saving modified resources of project...");
 		}
 		List<FlexoStorageResource<? extends StorageResourceData>> unsaved = getUnsavedStorageResources(true);
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("saving_modified_resources"));
 			progress.resetSecondaryProgress(unsaved.size() + 1);
 		}
 		boolean resourceSaved = false;
 		try {
 			for (FlexoStorageResource<? extends StorageResourceData> data : unsaved) {
 				if (data == getFlexoRMResource()) {
 					continue;
 				}
 				if (progress != null) {
 					progress.setSecondaryProgress(FlexoLocalization.localizedForKey("saving_resource_") + data.getName());
 				}
 				if (logger.isLoggable(Level.FINE)) {
 					logger.fine("Saving " + data.getResourceIdentifier() + " file: " + data.getFileName());
 				}
 				data.saveResourceData(clearModifiedStatus);
 				resourceSaved = true;
 			}
 		} finally {
 			if (resourceSaved || getFlexoRMResource().isModified()) {
 				revision++;
 				// If at least one resource has been saved, let's try to save the RM so that the lastID is also saved, avoiding possible
 				// duplication of flexoID's.
 				writeDotVersion();
 				// We save RM at the end so that all dates are always up-to-date and we also save the lastID which may have changed!
 				getFlexoRMResource().saveResourceData(clearModifiedStatus);
 			}
 		}
 	}
 
 	/**
 	 * Save this project using ResourceManager scheme Additionally save all known resources related to this project
 	 * 
 	 * Overrides
 	 * 
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 * @see org.openflexo.foundation.rm.FlexoResourceData#save()
 	 */
 	public void saveLoadedResources() throws SaveResourceException {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Saving all loaded resources for project...");
 		}
 		List<FlexoStorageResource<? extends StorageResourceData>> loaded = getLoadedStorageResources();
 		for (FlexoStorageResource<? extends StorageResourceData> r : loaded) {
 			r.saveResourceData();
 		}
 		writeDotVersion();
 	}
 
 	/*
 	 * GPO: The 2 following methods have a synchronized attributes to prevent 2 saves from deadlocking each-other. When the
 	 * saveModifiedResources is invoked, this also sends notification to Frames which will attempt to clear their modified status. To do so,
 	 * they call the second method. This methods invokes the isModified() method which is also synchronized. Therefore two threads trying to
 	 * perform a save of the project could end up in a deadlock
 	 */
 
 	/**
 	 * Build and return a vector of modified and unsaved file resources
 	 * 
 	 * @return a Vector of FlexoStorageResource
 	 */
 	public synchronized List<FlexoStorageResource<? extends StorageResourceData>> getUnsavedStorageResources() {
 		return getUnsavedStorageResources(true);
 	}
 
 	/**
 	 * Build and return a vector of modified and unsaved file resources
 	 * 
 	 * @param sortResources
 	 *            wheter the resources needed to be sorted by dependancy order or not. Sorting by dependancy is quite expensive.
 	 * 
 	 * @return a Vector of FlexoStorageResource
 	 */
 	public synchronized Vector<FlexoStorageResource<? extends StorageResourceData>> getUnsavedStorageResources(boolean sortResources) {
 		Vector<FlexoStorageResource<? extends StorageResourceData>> returned = new Vector<FlexoStorageResource<? extends StorageResourceData>>();
 		for (FlexoStorageResource<? extends StorageResourceData> resource : getStorageResources()) {
 			if (resource.needsSaving()) {
 				returned.add(resource);
 				if (logger.isLoggable(Level.FINE)) {
 					logger.fine("Resource " + resource.getResourceIdentifier() + " must be saved");
 				}
 			} else {
 				if (logger.isLoggable(Level.FINE)) {
 					logger.fine("Resource " + resource.getResourceIdentifier() + " doesn't require saving");
 				}
 			}
 		}
 		if (sortResources) {
 			DependencyAlgorithmScheme scheme = _dependancyScheme;
 			// Pessimistic dependency scheme is cheaper and is not intended for this situation
 			setDependancyScheme(DependencyAlgorithmScheme.Pessimistic);
 			FlexoResource.sortResourcesWithDependancies(returned);
 			setDependancyScheme(scheme);
 		}
 		return returned;
 	}
 
 	/**
 	 * Build and return a vector of loaded storage resources
 	 * 
 	 * @return a Vector of FlexoStorageResource
 	 */
 	public synchronized Vector<FlexoStorageResource<? extends StorageResourceData>> getLoadedStorageResources() {
 		Vector<FlexoStorageResource<? extends StorageResourceData>> returned = new Vector<FlexoStorageResource<? extends StorageResourceData>>();
 		for (Entry<String, FlexoResource<? extends FlexoResourceData>> e : resources.entrySet()) {
 			FlexoResource<? extends FlexoResourceData> resource = e.getValue();
 			if (resource instanceof FlexoStorageResource) {
 				if (((FlexoStorageResource<? extends StorageResourceData>) resource).isLoaded()) {
 					returned.add((FlexoStorageResource<? extends StorageResourceData>) resource);
 				}
 			}
 		}
 		return returned;
 	}
 
 	/**
 	 * Close this project Don't save anything
 	 * 
 	 * Overrides
 	 * 
 	 */
 	public void close() {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Closing project...");
 		}
 		if (resourceManagerInstance != null) {
 			resourceManagerInstance.stopResourcePeriodicChecking();
 		}
 		if (getGeneratedCodeResource(false) != null && getGeneratedCodeResource(false).isLoaded()) {
 			getGeneratedCode().setFactory(null);
 		}
 		if (getGeneratedDocResource(false) != null && getGeneratedDocResource(false).isLoaded()) {
 			getGeneratedDoc().setFactory(null);
 		}
 		getDataModel().close();
 		_resource = null;
 		resources = null;
 		resourceCenterService = null;
 		allRegisteredObjects.clear();
 		globalStatus.clear();
 		// resources.clear();
 		widgetBindingDefinitions.clear();
 		jarClassLoader = null;
 		notifyObservers(new ProjectClosedNotification(this));
 		deleteObservers();
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Closing project... DONE");
 		}
 
 	}
 
 	// =============================================================
 	// =================== Resources management ====================
 	// =============================================================
 
 	public Map<String, FlexoResource<? extends FlexoResourceData>> getSerializationResources() {
 		// logger.info("Attention on se tape la duplication de la hashtable des resources !!!!!");
 		Map<String, FlexoResource<? extends FlexoResourceData>> returned = new ResourceHashtable(getResources());
 		for (Entry<String, FlexoResource<? extends FlexoResourceData>> e : getResources().entrySet()) {
 			if (!e.getValue().isToBeSerialized()) {
 				returned.remove(e.getKey());
 			}
 		}
 		return returned;
 	}
 
 	public void setSerializationResources(Map<String, FlexoResource<? extends FlexoResourceData>> res) {
 		setResources(res);
 	}
 
 	public void setSerializationResourceForKey(FlexoResource<? extends FlexoResourceData> resource, String resourceIdentifier) {
 		setResourceForKey(resource, resourceIdentifier);
 	}
 
 	public void removeSerializationResourceWithKey(String resourceIdentifier) {
 		removeResourceWithKey(resourceIdentifier);
 	}
 
 	/**
 	 * Looks for a FlexoFileResource with an identical file. The lookup is done without taking the case into account to make sure that the
 	 * project always stays cross-platform compatible (Windows and some MacOS file systems are not case sensitive)
 	 * 
 	 * @param file
 	 * @return the resource matching the file.
 	 */
 	public FlexoFileResource<? extends FlexoResourceData> resourceForFile(File file) {
 		return resourceForFileName(new FlexoProjectFile(file, this));
 	}
 
 	/**
 	 * Looks for a file with an identical string representation. This compare is case insensitive because the project must stay
 	 * cross-platform compatible
 	 * 
 	 * @param aFile
 	 */
 	public FlexoFileResource<? extends FlexoResourceData> resourceForFileName(FlexoProjectFile aFile) {
 		for (FlexoResource<? extends FlexoResourceData> res : this) {
 			if (res instanceof FlexoFileResource) {
 				FlexoFileResource<? extends FlexoResourceData> flexoFileResource = (FlexoFileResource<? extends FlexoResourceData>) res;
 				if (flexoFileResource.getResourceFile() != null) {
 					String s1 = flexoFileResource.getResourceFile().getStringRepresentation();
 					String s2 = aFile.getStringRepresentation();
 					if (s1.equalsIgnoreCase(s2)) {
 						return flexoFileResource;
 					}
 				}
 			}
 		}
 		return null;
 	}
 
 	public Map<String, FlexoResource<? extends FlexoResourceData>> getResources() {
 		return resources;
 	}
 
 	public void setResources(Map<String, FlexoResource<? extends FlexoResourceData>> res) {
 		// Transtype to ResourceHashtable
 		this.resources = new ResourceHashtable(res);
 	}
 
 	@Override
 	public synchronized Iterator<FlexoResource<? extends FlexoResourceData>> iterator() {
 		return new ArrayList<FlexoResource<? extends FlexoResourceData>>(resources.values()).iterator();
 	}
 
 	public FlexoResource<? extends FlexoResourceData> resourceForKey(String fullQualifiedResourceIdentifier) {
 		FlexoResource<? extends FlexoResourceData> flexoResource = resources.get(fullQualifiedResourceIdentifier);
 		if (flexoResource != null) {
 			if (logger.isLoggable(Level.FINER)) {
 				logger.finer("Retrieving resource " + fullQualifiedResourceIdentifier);
 			}
 			return flexoResource;
 		}
 		// if (logger.isLoggable(Level.WARNING)) logger.warning ("Could not
 		// retrieve resource "+fullQualifiedResourceIdentifier);
 		return null;
 	}
 
 	public synchronized void setResourceForKey(FlexoResource<? extends FlexoResourceData> resource, String resourceIdentifier) {
 		if (logger.isLoggable(Level.FINE)) {
 			logger.fine("Registering resource " + resourceIdentifier + " with object " + resource);
 		}
 		resources.put(resourceIdentifier, resource);
 		setChanged();
 		notifyObservers(new ResourceAdded(resource));
 	}
 
 	public synchronized void removeResourceWithKey(String resourceIdentifier) {
 		if (resources.get(resourceIdentifier) != null) {
 			FlexoResource<? extends FlexoResourceData> resource = resources.get(resourceIdentifier);
 			resources.remove(resourceIdentifier);
 			setChanged();
 			notifyObservers(new ResourceRemoved(resource));
 		}
 	}
 
 	public synchronized void registerResource(FlexoResource<? extends FlexoResourceData> resource) throws DuplicateResourceException {
 		if (resourceForKey(resource.getResourceIdentifier()) != null) {
 			throw new DuplicateResourceException(resource);
 		}
 		setResourceForKey(resource, resource.getResourceIdentifier());
 		if (resource instanceof FlexoFileResource && getFlexoRMResource() != null) {
 			resource.addToSynchronizedResources(getFlexoRMResource());
 		}
 	}
 
 	public synchronized void renameResource(FlexoResource<? extends FlexoResourceData> resource, String newName)
 			throws DuplicateResourceException {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("renameResource() " + resource.getResourceIdentifier() + " with " + newName);
 		}
 		if (isRegistered(resource)) {
 			String oldResourceIdentifier = registeredKeyForResource(resource);
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine("resource is registered");
 			}
 			if (isRegistered(resource.getResourceIdentifierForNewName(newName))
 					&& resources.get(resource.getResourceIdentifierForNewName(newName)) != resource) {
 				throw new DuplicateResourceException(resource.getResourceIdentifierForNewName(newName));
 			} else {
 				if (logger.isLoggable(Level.FINE)) {
 					logger.finer("remove resource " + resource.getResourceIdentifier());
 				}
 				removeResourceWithKey(oldResourceIdentifier);
 				resource.setName(newName);
 				if (logger.isLoggable(Level.FINE)) {
 					logger.finer("register resource " + resource.getResourceIdentifier());
 				}
 				registerResource(resource);
 			}
 		} else {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Could not rename resource " + resource.getResourceIdentifier() + " to " + newName
 						+ " because this resource is not registered !");
 			}
 		}
 	}
 
 	public synchronized void repairKeyForResource(FlexoResource<FlexoResourceData> resource) {
 		try {
 			String actualKey = registeredKeyForResource(resource);
 			if (actualKey == null) {
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Registering " + resource);
 				}
 				registerResource(resource);
 			} else if (!actualKey.equals(resource.getResourceIdentifier())) {
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Fixing key for resource " + resource);
 				}
 				removeResourceWithKey(actualKey);
 				registerResource(resource);
 			} else {
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Actual key of " + resource + " is correct.");
 				}
 			}
 		} catch (DuplicateResourceException e) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Cannot fix the resource " + resource + " because another one is already registered with the same key.");
 			}
 		}
 	}
 
 	public boolean isRegistered(FlexoResource<? extends FlexoResourceData> resource) {
 		return registeredKeyForResource(resource) != null;
 	}
 
 	public synchronized String registeredKeyForResource(FlexoResource<? extends FlexoResourceData> resource) {
 		for (Entry<String, FlexoResource<? extends FlexoResourceData>> r : resources.entrySet()) {
 			if (r.getValue() == resource) {
 				return r.getKey();
 			}
 		}
 		return null;
 	}
 
 	public boolean isRegistered(String resourceIdentifier) {
 		return resources.get(resourceIdentifier) != null;
 	}
 
 	public synchronized void removeResource(FlexoResource<? extends FlexoResourceData> resource) {
 		String identifier = resource.getResourceIdentifier();
 		if (resources.get(identifier) == null) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Incorrect identifier in resources Hashtable, trying to find it anyway");
 			}
 			identifier = null;
 			for (Entry<String, FlexoResource<? extends FlexoResourceData>> r : resources.entrySet()) {
 				{
 					if (r.getValue() == resource) {
 						identifier = r.getKey();
 						break;
 					}
 				}
 			}
 		}
 		if (identifier != null) {
 			removeResourceWithKey(identifier);
 			for (FlexoResource<FlexoResourceData> res : new ArrayList<FlexoResource<FlexoResourceData>>(resource.getAlteredResources())) {
 				res.removeFromDependentResources(resource);
 			}
 			for (FlexoResource<FlexoResourceData> res : new ArrayList<FlexoResource<FlexoResourceData>>(resource.getDependentResources())) {
 				res.removeFromAlteredResources(resource);
 			}
 			for (FlexoResource<FlexoResourceData> res : new ArrayList<FlexoResource<FlexoResourceData>>(resource.getSynchronizedResources())) {
 				res.removeFromSynchronizedResources(resource);
 			}
 		} else {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Could not remove resource " + resource.getResourceIdentifier()
 						+ " because this resource is not registered !");
 			}
 		}
 	}
 
 	// ==========================================================================
 	// ====================== Access to Resources ==============================
 	// ==========================================================================
 
 	public FlexoResource<? extends FlexoResourceData> resourceForKey(ResourceType resourceType, String resourceName) {
 		if (logger.isLoggable(Level.FINE)) {
 			if (logger.isLoggable(Level.FINER)) {
 				logger.finer("Looking for " + resourceType.getName() + "." + resourceName);
 			}
 			if (logger.isLoggable(Level.FINEST)) {
 				for (Entry<String, FlexoResource<? extends FlexoResourceData>> e : resources.entrySet()) {
 					FlexoResource<? extends FlexoResourceData> temp = e.getValue();
 					logger.finest("Having " + temp + "/" + temp.hashCode());
 				}
 			}
 			if (logger.isLoggable(Level.FINER)) {
 				logger.finer("Returning " + resourceForKey(resourceType.getName() + "." + resourceName));
 			}
 		}
 		return resourceForKey(resourceType.getName() + "." + resourceName);
 	}
 
 	public FlexoWorkflowResource getFlexoWorkflowResource() {
 		return getFlexoWorkflowResource(true);
 	}
 
 	public FlexoWorkflowResource getFlexoWorkflowResource(boolean create) {
 		FlexoWorkflowResource wkfResource = (FlexoWorkflowResource) resourceForKey(ResourceType.WORKFLOW, getProjectName());
 		if (wkfResource == null && create) {
 			FlexoWorkflow.createNewWorkflow(this);
 			return getFlexoWorkflowResource(false);
 		}
 		return wkfResource;
 	}
 
 	public FlexoComponentLibraryResource getFlexoComponentLibraryResource() {
 		return getFlexoComponentLibraryResource(true);
 	}
 
 	public FlexoComponentLibraryResource getFlexoComponentLibraryResource(boolean create) {
 		FlexoComponentLibraryResource returned = (FlexoComponentLibraryResource) resourceForKey(ResourceType.COMPONENT_LIBRARY,
 				getProjectName());
 		if (returned == null && create) {
			if (logger.isLoggable(Level.INFO)) {
				logger.info("Create ComponentLibrary");
			}
 			FlexoComponentLibrary.createNewComponentLibrary(this);
 			return getFlexoComponentLibraryResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoOEShemaLibraryResource getFlexoShemaLibraryResource() {
 		return getFlexoShemaLibraryResource(true);
 	}
 
 	public FlexoOEShemaLibraryResource getFlexoShemaLibraryResource(boolean createIfNotExist) {
 		FlexoOEShemaLibraryResource returned = (FlexoOEShemaLibraryResource) resourceForKey(ResourceType.OE_SHEMA_LIBRARY, getProjectName());
 		if (returned == null && createIfNotExist) {
 			ViewLibrary.createNewShemaLibrary(this);
 			return getFlexoShemaLibraryResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoNavigationMenuResource getFlexoNavigationMenuResource() {
 		return getFlexoNavigationMenuResource(true);
 	}
 
 	public FlexoNavigationMenuResource getFlexoNavigationMenuResource(boolean create) {
 		FlexoNavigationMenuResource returned = (FlexoNavigationMenuResource) resourceForKey(ResourceType.NAVIGATION_MENU, getProjectName());
 		if (returned == null && create) {
 			FlexoNavigationMenu.createNewFlexoNavigationMenu(this);
 			return getFlexoNavigationMenuResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoDMResource getFlexoDMResource() {
 		return getFlexoDMResource(true);
 	}
 
 	public FlexoDMResource getFlexoDMResource(boolean create) {
 		FlexoDMResource returned = (FlexoDMResource) resourceForKey(ResourceType.DATA_MODEL, getProjectName());
 		if (returned == null && !dataModelIsBuilding) {
 			DMModel.createNewDMModel(this);
 			return getFlexoDMResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoDKVResource getFlexoDKVResource() {
 		return getFlexoDKVResource(true);
 	}
 
 	public FlexoDKVResource getFlexoDKVResource(boolean create) {
 		FlexoDKVResource returned = (FlexoDKVResource) resourceForKey(ResourceType.DKV_MODEL, getProjectName());
 		if (returned == null && create) {
 			DKVModel.createNewDKVModel(this);
 			return getFlexoDKVResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoWSLibraryResource getFlexoWSLibraryResource() {
 		return getFlexoWSLibraryResource(true);
 	}
 
 	public FlexoWSLibraryResource getFlexoWSLibraryResource(boolean create) {
 		FlexoWSLibraryResource returned = (FlexoWSLibraryResource) resourceForKey(ResourceType.WS_LIBRARY, getProjectName());
 		if (returned == null && create) {
 			FlexoWSLibrary.createNewWSLibrary(this);
 			return getFlexoWSLibraryResource(false);
 		}
 		return returned;
 	}
 
 	public FlexoProcessResource getFlexoProcessResource(String processName) {
 		return (FlexoProcessResource) resourceForKey(ResourceType.PROCESS, processName);
 	}
 
 	public FlexoOperationComponentResource getFlexoOperationComponentResource(String componentName) {
 		return (FlexoOperationComponentResource) resourceForKey(ResourceType.OPERATION_COMPONENT, componentName);
 	}
 
 	public FlexoTabComponentResource getFlexoTabComponentResource(String componentName) {
 		return (FlexoTabComponentResource) resourceForKey(ResourceType.TAB_COMPONENT, componentName);
 	}
 
 	public FlexoReusableComponentResource getFlexoReusableComponentResource(String componentName) {
 		return (FlexoReusableComponentResource) resourceForKey(ResourceType.REUSABLE_COMPONENT, componentName);
 	}
 
 	public FlexoPopupComponentResource getFlexoPopupComponentResource(String componentName) {
 		return (FlexoPopupComponentResource) resourceForKey(ResourceType.POPUP_COMPONENT, componentName);
 	}
 
 	public FlexoMonitoringScreenResource getFlexoMonitoringScreenResource(String componentName) {
 		return (FlexoMonitoringScreenResource) resourceForKey(ResourceType.MONITORING_SCREEN, componentName);
 	}
 
 	public FlexoOEShemaResource getShemaResource(String shemaName) {
 		return (FlexoOEShemaResource) resourceForKey(ResourceType.OE_SHEMA, shemaName);
 	}
 
 	public ImplementationModelResource getImplementationModelResource(String modelName) {
 		return (ImplementationModelResource) resourceForKey(ResourceType.IMPLEMENTATION_MODEL, modelName);
 	}
 
 	public CustomInspectorsResource getCustomInspectorsResource() {
 		return (CustomInspectorsResource) resourceForKey(ResourceType.CUSTOM_INSPECTORS, getProjectName());
 	}
 
 	public Vector<CustomTemplatesResource> getCustomTemplatesResources() {
 		Vector<CustomTemplatesResource> returned = new Vector<CustomTemplatesResource>();
 		for (FlexoResource<? extends FlexoResourceData> resource : this) {
 			if (resource.getResourceType() == ResourceType.CUSTOM_TEMPLATES) {
 				returned.add((CustomTemplatesResource) resource);
 			}
 		}
 		return returned;
 	}
 
 	public ProjectExternalRepository createExternalRepositoryWithKey(String identifier) throws DuplicateCodeRepositoryNameException {
 		for (ProjectExternalRepository rep : _externalRepositories) {
 			if (rep.getIdentifier().equals(identifier)) {
 				throw new DuplicateCodeRepositoryNameException(null, identifier);
 			}
 		}
 		ProjectExternalRepository returned = new ProjectExternalRepository(this, identifier);
 		addToExternalRepositories(returned);
 		return returned;
 	}
 
 	public String getNextExternalRepositoryIdentifier(String base) {
 		String attempt = base;
 		int i = 0;
 		while (getExternalRepositoryWithKey(attempt) != null) {
 			i++;
 			attempt = base + i;
 		}
 		return attempt;
 	}
 
 	public ProjectExternalRepository getExternalRepositoryWithKey(String identifier) {
 		for (ProjectExternalRepository rep : _externalRepositories) {
 			if (rep.getIdentifier().equals(identifier)) {
 				return rep;
 			}
 		}
 		return null;
 	}
 
 	public ProjectExternalRepository getExternalRepositoryWithDirectory(File directory) {
 		if (directory == null) {
 			return null;
 		}
 		for (ProjectExternalRepository rep : _externalRepositories) {
 			if (rep.getDirectory() != null && rep.getDirectory().equals(directory)) {
 				return rep;
 			}
 		}
 		return null;
 	}
 
 	public ProjectExternalRepository setDirectoryForRepositoryName(String identifier, File directory) {
 		ProjectExternalRepository returned = getExternalRepositoryWithKey(identifier);
 		if (returned == null) {
 			returned = new ProjectExternalRepository(this, identifier);
 			addToExternalRepositories(returned);
 		}
 		if (returned.getDirectory() == null || !returned.getDirectory().equals(directory)) {
 			returned.setDirectory(directory);
 			setChanged();
 			notifyObservers(new ExternalRepositorySet(returned));
 		}
 		return returned;
 	}
 
 	private List<ProjectExternalRepository> _externalRepositories;
 	private final Map<String, ProjectExternalRepository> repositoriesCache;
 
 	public List<ProjectExternalRepository> getExternalRepositories() {
 		return _externalRepositories;
 	}
 
 	public void setExternalRepositories(List<ProjectExternalRepository> externalRepositories) {
 		_externalRepositories = externalRepositories;
 		repositoriesCache.clear();
 		if (externalRepositories != null) {
 			for (ProjectExternalRepository projectExternalRepository : externalRepositories) {
 				repositoriesCache.put(projectExternalRepository.getIdentifier(), projectExternalRepository);
 			}
 		}
 	}
 
 	public void addToExternalRepositories(ProjectExternalRepository anExternalRepository) {
 		_externalRepositories.add(anExternalRepository);
 		repositoriesCache.put(anExternalRepository.getIdentifier(), anExternalRepository);
 		setChanged();
 	}
 
 	public void removeFromExternalRepositories(ProjectExternalRepository anExternalRepository) {
 		_externalRepositories.remove(anExternalRepository);
 		repositoriesCache.remove(anExternalRepository.getIdentifier());
 		setChanged();
 	}
 
 	public FlexoEOModelResource getEOModelResource(String eoModelName) {
 		return (FlexoEOModelResource) resourceForKey(ResourceType.EOMODEL, eoModelName);
 	}
 
 	public FlexoJarResource getJarResource(String jarName) {
 		return (FlexoJarResource) resourceForKey(ResourceType.JAR, jarName);
 	}
 
 	public FlexoCSSResource getDocumentationCssResource() {
 		FlexoCSSResource documentationCssResource = (FlexoCSSResource) resourceForKey(ResourceType.CSS_FILE, PROJECT_DOCUMENTATION_CSS_FILE);
 		if (documentationCssResource == null) {
 			File documentationCssFile = new File(getProjectDirectory(), "Resources/" + PROJECT_DOCUMENTATION_CSS_FILE);
 			try {
 				FileUtils.copyFileToFile(new FileResource("Resources/" + PROJECT_DOCUMENTATION_CSS_FILE), documentationCssFile);
 				documentationCssResource = new FlexoCSSResource(this);
 				documentationCssResource.setResourceFile(new FlexoProjectFile(documentationCssFile, this));
 				registerResource(documentationCssResource);
 			} catch (InvalidFileNameException e) {
 				logger.log(Level.SEVERE, "Oups cannot load resource documentation css !", e);
 				documentationCssResource = null;
 			} catch (DuplicateResourceException e) {
 				logger.log(Level.SEVERE, "Oups cannot load resource documentation css !", e);
 				documentationCssResource = null;
 			} catch (IOException e) {
 				logger.log(Level.SEVERE, "Oups cannot load resource documentation css !", e);
 				documentationCssResource = null;
 			}
 		}
 
 		return documentationCssResource;
 	}
 
 	public FlexoEOModelResource getEOModelResource(FlexoProjectFile eoModelFile) {
 		return getEOModelResource(eoModelFile.getFile().getName());
 	}
 
 	public FlexoJarResource getJarResource(FlexoProjectFile jarFile) {
 		return getJarResource(jarFile.getFile().getName());
 	}
 
 	public ScreenshotResource getScreenshotResource(FlexoModelObject object) {
 		return getScreenshotResource(object, false);
 	}
 
 	public ScreenshotResource getScreenshotResource(FlexoModelObject object, boolean createIfNotExist) {
 		ScreenshotResource resource = (ScreenshotResource) resourceForKey(ResourceType.SCREENSHOT,
 				ScreenshotGenerator.getScreenshotName(object));
 		if (resource != null && resource.getModelObject() == null) {
 			resource = null;
 		}
 		if (resource == null && createIfNotExist) {
 			resource = ScreenshotResource.createNewScreenshotForObject(object);
 		}
 		return resource;
 	}
 
 	// ==========================================================================
 	// =========================== Access to Data ==============================
 	// ==========================================================================
 
 	public String getGlobalComponentPrefix() {
 		return getFlexoComponentLibrary().getRootFolder().getComponentPrefix();
 	}
 
 	public FlexoComponentLibrary getFlexoComponentLibrary() {
 		return getFlexoComponentLibrary(true);
 	}
 
 	public FlexoComponentLibrary getFlexoComponentLibrary(boolean create) {
 		if (getFlexoComponentLibraryResource(create) != null) {
 			return getFlexoComponentLibraryResource(create).getResourceData();
 		}
 		return null;
 	}
 
 	@Deprecated
 	public ViewLibrary getShemaLibrary() {
 		return getShemaLibrary(true);
 	}
 
 	public ViewLibrary getViewLibrary() {
 		return getShemaLibrary();
 	}
 
 	public ViewLibrary getShemaLibrary(boolean createIfNotExist) {
 		if (getFlexoShemaLibraryResource(createIfNotExist) == null) {
 			if (createIfNotExist) {
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Create ShemaLibrary");
 				}
 				ViewLibrary.createNewShemaLibrary(this);
 			} else {
 				return null;
 			}
 		}
 		return getFlexoShemaLibraryResource(createIfNotExist).getResourceData();
 	}
 
 	public GeneratedCode getGeneratedCode() {
 		if (getGeneratedCodeResource(true) == null) {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Create GeneratedCode");
 			}
 			GeneratedCode.createNewGeneratedCode(this);
 		}
 		return getGeneratedCodeResource(false).getResourceData();
 	}
 
 	public GeneratedCodeResource getGeneratedCodeResource() {
 		return getGeneratedCodeResource(true);
 	}
 
 	public GeneratedCodeResource getGeneratedCodeResource(boolean createIfNotExists) {
 		GeneratedCodeResource returned = (GeneratedCodeResource) resourceForKey(ResourceType.GENERATED_CODE, getProjectName());
 		if (returned == null && createIfNotExists) {
 			GeneratedCode.createNewGeneratedCode(this);
 			return getGeneratedCodeResource(false);
 		}
 		return returned;
 	}
 
 	public GeneratedSources getGeneratedSources() {
 		if (getGeneratedSourcesResource(true) == null) {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Create GeneratedSources");
 			}
 			GeneratedSources.createNewGeneratedSources(this);
 		}
 		return getGeneratedSourcesResource(false).getResourceData();
 	}
 
 	public GeneratedSourcesResource getGeneratedSourcesResource() {
 		return getGeneratedSourcesResource(true);
 	}
 
 	public GeneratedSourcesResource getGeneratedSourcesResource(boolean createIfNotExists) {
 		GeneratedSourcesResource returned = (GeneratedSourcesResource) resourceForKey(ResourceType.GENERATED_SOURCES, getProjectName());
 		if (returned == null && createIfNotExists) {
 			GeneratedSources.createNewGeneratedSources(this);
 			return getGeneratedSourcesResource(false);
 		}
 		return returned;
 	}
 
 	public GeneratedDoc getGeneratedDoc() {
 		if (getGeneratedDocResource() == null) {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Create GeneratedDoc");
 			}
 			GeneratedDoc.createNewGeneratedDoc(this);
 		}
 		return getGeneratedDocResource(false).getResourceData();
 	}
 
 	public GeneratedDocResource getGeneratedDocResource() {
 		return getGeneratedDocResource(true);
 	}
 
 	public GeneratedDocResource getGeneratedDocResource(boolean createIfNotExists) {
 		GeneratedDocResource returned = (GeneratedDocResource) resourceForKey(ResourceType.GENERATED_DOC, getProjectName());
 		if (returned == null && createIfNotExists) {
 			GeneratedDoc.createNewGeneratedDoc(this);
 			return getGeneratedDocResource(false);
 		}
 		return returned;
 	}
 
 	public TOCData getTOCData() {
 		if (getTOCResource() == null) {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Create TOC");
 			}
 			TOCData.createNewTOCData(this);
 		}
 		return getTOCResource().getResourceData();
 	}
 
 	public FlexoTOCResource getTOCResource() {
 		FlexoTOCResource returned = (FlexoTOCResource) resourceForKey(ResourceType.TOC, getProjectName());
 		if (returned == null) {
 			TOCData.createNewTOCData(this);
 			return getTOCResource();
 		}
 		return returned;
 	}
 
 	public FlexoNavigationMenu getFlexoNavigationMenu() {
 		return getFlexoNavigationMenu(true);
 	}
 
 	public FlexoNavigationMenu getFlexoNavigationMenu(boolean create) {
 		if (getFlexoNavigationMenuResource(create) != null) {
 			return getFlexoNavigationMenuResource().getResourceData();
 		}
 		return null;
 	}
 
 	public FlexoWSLibrary getFlexoWSLibrary() {
 		return getFlexoWSLibrary(true);
 	}
 
 	public FlexoWSLibrary getFlexoWSLibrary(boolean create) {
 		if (getFlexoWSLibraryResource(create) != null) {
 			return getFlexoWSLibraryResource().getWSLibrary();
 		}
 		return null;
 	}
 
 	private DMModel buildingDataModel = null;
 	private boolean dataModelIsBuilding = false;
 	private JarClassLoader jarClassLoader;
 
 	public DMModel getDataModel() {
 		return getDataModel(true);
 	}
 
 	public DMModel getDataModel(boolean create) {
 		if (getBuildingDataModel() != null) {
 			return getBuildingDataModel();
 		}
 		if (getFlexoDMResource(create) != null) {
 			return getFlexoDMResource().getResourceData();
 		}
 		return null;
 	}
 
 	public DMModel getBuildingDataModel() {
 		return buildingDataModel;
 	}
 
 	public void setBuildingDataModel(DMModel buildingDataModel) {
 		this.buildingDataModel = buildingDataModel;
 	}
 
 	public JarClassLoader getJarClassLoader() {
 		if (jarClassLoader == null) {
 			jarClassLoader = new JarClassLoader(Arrays.asList(new FlexoProjectFile(this, ProjectRestructuration.DATA_MODEL_DIR).getFile()));
 		}
 		return jarClassLoader;
 	}
 
 	public void setJarClassLoader(JarClassLoader jarClassLoader) {
 		this.jarClassLoader = jarClassLoader;
 	}
 
 	public DKVModel getDKVModel() {
 		return getDKVModel(true);
 	}
 
 	public DKVModel getDKVModel(boolean create) {
 		if (getFlexoDKVResource(create) != null) {
 			return getFlexoDKVResource().getResourceData();
 		} else {
 			return null;
 		}
 	}
 
 	public FlexoWorkflow getWorkflow() {
 		return getFlexoWorkflow();
 	}
 
 	public FlexoWorkflow getFlexoWorkflow() {
 		return getFlexoWorkflow(true);
 	}
 
 	public FlexoWorkflow getFlexoWorkflow(boolean create) {
 		if (getFlexoWorkflowResource(create) != null) {
 			return getFlexoWorkflowResource(create).getResourceData();
 		}
 		return null;
 	}
 
 	public FlexoProcess getRootFlexoProcess() {
 		return getFlexoWorkflow().getRootFlexoProcess();
 	}
 
 	public FlexoProcess getRootProcess() {
 		return getRootFlexoProcess();
 	}
 
 	/**
 	 * Returns the default CSS for the whole project. This CSS is used by the interface editor to display component. You can never return
 	 * null, it would cause NPE's all over the place.
 	 * 
 	 * @return the default CSS for this project.
 	 */
 	public FlexoCSS getCssSheet() {
 		if (cssSheet == null) {
 			cssSheet = FlexoCSS.CONTENTO;
 		}
 		return cssSheet;
 	}
 
 	public void setCssSheet(FlexoCSS css) {
 		FlexoCSS old = this.cssSheet;
 		this.cssSheet = css;
 		setChanged();
 		notifyObservers(new StyleSheetFolderChanged(old, css));
 	}
 
 	/**
 	 * @return a Vector with all FlexoProcess.
 	 */
 	public Vector<FlexoProcess> getAllFlexoProcesses() {
 		if (getFlexoWorkflow() != null) {
 			return getFlexoWorkflow().getAllFlexoProcesses();
 		}
 		return null;
 	}
 
 	/**
 	 * @return a Vector with all local FlexoProcess.
 	 */
 	public Vector<FlexoProcess> getAllLocalFlexoProcesses() {
 		if (getFlexoWorkflow(false) != null) {
 			return getFlexoWorkflow().getAllLocalFlexoProcesses();
 		}
 		return null;
 	}
 
 	public FlexoProcess getLocalFlexoProcess(String processName) {
 		return getFlexoWorkflow().getLocalFlexoProcessWithName(processName);
 	}
 
 	public IEOperationComponent getOperationComponent(String componentName) {
 		return getFlexoOperationComponentResource(componentName).getIEOperationComponent();
 	}
 
 	public IETabComponent getTabComponent(String componentName) {
 		return getFlexoTabComponentResource(componentName).getTabComponent();
 	}
 
 	public IEReusableComponent getSingleWidgetComponent(String componentName) {
 		return getFlexoReusableComponentResource(componentName).getWOComponent();
 	}
 
 	public IEPopupComponent getPopupComponent(String componentName) {
 		return getFlexoPopupComponentResource(componentName).getIEPopupComponent();
 	}
 
 	public File getCustomInspectorsDirectory() {
 		if (getCustomInspectorsResource() != null) {
 			return getCustomInspectorsResource().getResourceDirectory();
 		} else {
 			return null;
 		}
 	}
 
 	// ==========================================================================
 	// ======================= New project procedure
 	// ============================
 	// ==========================================================================
 
 	public static FlexoEditor newProject(File rmFile, File aProjectDirectory, FlexoEditorFactory editorFactory, FlexoProgress progress,
 			FlexoResourceCenterService resourceCenterService) {
 		// aProjectDirectory = aProjectDirectory.getCanonicalFile();
 		FlexoProject project = new FlexoProject(aProjectDirectory);
 		project.setResourceCenter(resourceCenterService);
 		FlexoEditor editor = editorFactory.makeFlexoEditor(project);
 		project.setLastUniqueID(0);
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Building project: " + aProjectDirectory.getAbsolutePath());
 		}
 
 		FlexoProjectFile projectRMFile = new FlexoProjectFile(rmFile, project);
 		FlexoRMResource rmResource = new FlexoRMResource(project, projectRMFile);
 
 		try {
 			project.setTimestampsHaveBeenLoaded(true);
 			project.setCreationUserId(FlexoModelObject.getCurrentUserIdentifier());
 			project.setCreationDate(new Date());
 			project.setFlexoResource(rmResource);
 			project.registerResource(rmResource);
 			// project.dataModelIsBuilding = true;
 			// DMModel.createNewDMModel(project);
 			// project.dataModelIsBuilding = false;
 			// FlexoComponentLibrary.createNewComponentLibrary(project);
 			// FlexoWorkflow.createNewWorkflow(project);
 			// FlexoNavigationMenu.createNewFlexoNavigationMenu(project);
 			// DKVModel.createNewDKVModel(project);
 			TOCData tocData = project.getTOCData();
 			TOCRepository repository = TOCRepository.createTOCRepositoryForDocType(tocData, project.getDocTypes().get(0));
 			repository.setTitle(project.getName());
 			tocData.addToRepositories(repository);
 			// GeneratedDoc.createNewGeneratedDoc(project);
 			project.initJavaFormatter();
 			importInitialImages(project, editor);
 			// Eventually log the result
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine("Getting this RM File:\n" + project.getFlexoRMResource().getResourceXMLRepresentation());
 			}
 			try {
 				// This needs to be called to ensure the consistency of the project
 				project.setGenerateSnapshot(false);
 				project.save(progress);
 				project.setGenerateSnapshot(true);
 			} catch (SaveResourceException e) {
 				if (logger.isLoggable(Level.SEVERE)) {
 					logger.severe("Could not save all resources for project: " + project.getProjectName() + " located in "
 							+ project.getProjectDirectory().getAbsolutePath());
 				}
 			}
 		} catch (Exception e) {
 			// Warns about the exception
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Exception raised: " + e.getClass().getName() + ". See console for details.");
 			}
 			e.printStackTrace();
 		}
 
 		return editor;
 	}
 
 	boolean generateSnapshot = true;
 
 	private void setGenerateSnapshot(boolean b) {
 		generateSnapshot = b;
 	}
 
 	public boolean isGenerateSnapshot() {
 		return generateSnapshot;
 	}
 
 	public static void importInitialImages(FlexoProject project, FlexoEditor editor) {
 
 		if (!INITIAL_IMAGES_DIR.exists()) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Could not find intial images.");
 			}
 			return;
 		}
 		File[] files = INITIAL_IMAGES_DIR.listFiles(new java.io.FileFilter() {
 
 			@Override
 			public boolean accept(File pathname) {
 				String lower = pathname.getName().toLowerCase();
 				return lower.endsWith(".jpg") || lower.endsWith(".gif") || lower.endsWith(".png");
 			}
 		});
 		if (files == null) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Initial images dir return null array.");
 			}
 			return;
 		}
 		Arrays.sort(files);
 		for (File file : files) {
 			ImportImage importImage = ImportImage.actionType.makeNewAction(project, null, editor);
 			importImage.setFileToImport(file);
 			/*
 			 * if (file.getName().matches("[0-9]+_.*")) { importImage.setImageName(file.getName().substring(file.getName().indexOf('_')+1));
 			 * }
 			 */
 			importImage.doAction();
 		}
 	}
 
 	public File getProjectDirectory() {
 		return projectDirectory;
 	}
 
 	public File getIECustomPaletteDirectory() {
 		if (projectDirectory != null) {
 			if (projectDirectory.exists()) {
 				File reply = new File(projectDirectory, "IEPalette");
 				if (reply.exists()) {
 					return reply;
 				}
 				reply.mkdir();
 				return reply;
 			}
 		}
 		return null;
 	}
 
 	public List<FlexoWebServerFileResource> getSpecificImageResources() {
 		return getResourcesOfClass(FlexoWebServerFileResource.class);
 	}
 
 	public List<File> getSpecificImages() {
 		List<File> reply = new Vector<File>();
 		for (FlexoWebServerFileResource wsr : getSpecificImageResources()) {
 			reply.add(wsr.getFile());
 		}
 		return reply;
 	}
 
 	public void setProjectDirectory(File aProjectDirectory) {
 		setProjectDirectory(aProjectDirectory, true);
 	}
 
 	public void setProjectDirectory(File aProjectDirectory, boolean notify) {
 		projectDirectory = aProjectDirectory;
 		clearCachedFiles();
 		if (notify) {
 			setChanged();
 			notifyObservers(new DataModification("projectDirectory", null, projectDirectory));
 		}
 	}
 
 	/**
 	 *
 	 */
 	public void clearCachedFiles() {
 		for (FlexoFileResource<? extends FlexoResourceData> r : getFileResources()) {
 			if (r.getResourceFile() != null) {
 				r.getResourceFile().clearCachedFile();
 			}
 		}
 	}
 
 	public String getProjectName() {
 		return projectName;
 	}
 
 	@Override
 	public String getName() {
 		return getProjectName();
 	}
 
 	public void setProjectName(String aName) throws InvalidNameException {
 		if (!BAD_CHARACTERS_PATTERN.matcher(aName).find()) {
 			projectName = aName;
 			resources.restoreKeys();
 		} else {
 			throw new InvalidNameException();
 		}
 	}
 
 	public String getProjectDescription() {
 		return projectDescription;
 	}
 
 	public void setProjectDescription(String aDescription) {
 		projectDescription = aDescription;
 		setChanged();
 	}
 
 	@Override
 	public XMLMapping getXMLMapping() {
 		return xmlMappings.getRMMapping();
 	}
 
 	public FlexoXMLMappings getXmlMappings() {
 		return xmlMappings;
 	}
 
 	public void setXmlMappings(FlexoXMLMappings xmlMappings) {
 		this.xmlMappings = xmlMappings;
 	}
 
 	public String getPrefix() {
 		String prefix = null;
 		if (getProjectName().length() > 2) {
 			prefix = getProjectName().substring(0, 3).toUpperCase();
 		} else {
 			prefix = getProjectName().toUpperCase();
 		}
 		return ToolBox.getJavaName(prefix, true, false);
 	}
 
 	private DateFormatType _dateFormat = DateFormatType.EUDEFAULT;
 
 	public DateFormatType getProjectDateFormat() {
 		if (_dateFormat == null) {
 			_dateFormat = DateFormatType.EUDEFAULT;
 		}
 		return _dateFormat;
 	}
 
 	public void setProjectDateFormat(DateFormatType value) {
 		if (value != null) {
 			_dateFormat = value;
 		}
 	}
 
 	// ==========================================================================
 	// ============================= InspectableObject
 	// ==========================
 	// ==========================================================================
 
 	@Override
 	public String getInspectorName() {
 		return Inspectors.COMMON.PROJECT_INSPECTOR;
 	}
 
 	// ==========================================================================
 	// ============================= Validation
 	// =================================
 	// ==========================================================================
 
 	private WKFValidationModel _wkfValidationModel;
 
 	private IEValidationModel _ieValidationModel;
 
 	private DKVValidationModel _dkvValidationModel;
 
 	private DMValidationModel _dmValidationModel;
 
 	public ValidationModel getWKFValidationModel() {
 		if (_wkfValidationModel == null) {
 			_wkfValidationModel = new WKFValidationModel(this);
 		}
 		return _wkfValidationModel;
 	}
 
 	public ValidationModel getIEValidationModel() {
 		if (_ieValidationModel == null) {
 			_ieValidationModel = new IEValidationModel(this);
 		}
 		return _ieValidationModel;
 	}
 
 	public ValidationModel getDKVValidationModel() {
 		if (_dkvValidationModel == null) {
 			_dkvValidationModel = new DKVValidationModel(this);
 		}
 		return _dkvValidationModel;
 	}
 
 	public ValidationModel getDMValidationModel() {
 		if (_dmValidationModel == null) {
 			_dmValidationModel = new DMValidationModel(this);
 		}
 		return _dmValidationModel;
 	}
 
 	public ValidationModel getProjectValidationModel() {
 		if (projectValidationModel == null) {
 			projectValidationModel = new FlexoProjectValidationModel(this);
 		}
 		return projectValidationModel;
 	}
 
 	public ValidationModel getImportedObjectValidationModel() {
 		if (importedObjectValidationModel == null) {
 			importedObjectValidationModel = new FlexoImportedObjectValidationModel(this);
 		}
 		return importedObjectValidationModel;
 	}
 
 	/**
 	 * Rebuild resource dependencies for project
 	 */
 	public void rebuildDependencies() {
 		rebuildDependencies(null);
 	}
 
 	/**
 	 * Rebuild resource dependencies for project
 	 */
 	public void rebuildDependencies(FlexoProgress progress) {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Rebuild resource dependancies for project " + getProjectName());
 		}
 		if (progress != null) {
 			progress.setProgress(FlexoLocalization.localizedForKey("cleaning_dependancies"));
 		}
 		for (FlexoResource<? extends FlexoResourceData> resource : this) {
 			// Clear dependencies for that resource
 			resource.clearDependencies();
 		}
 		for (FlexoResource<? extends FlexoResourceData> resource : this) {
 			if (progress != null) {
 				progress.setProgress(FlexoLocalization.localizedForKey("building_dependancies_for_resource") + " "
 						+ resource.getResourceIdentifier());
 			}
 			// Rebuild dependencies for that resource
 			resource.rebuildDependancies();
 		}
 		setChanged();
 	}
 
 	private void propagateResourceStatusChangeToAlteredResource(AlteredResources resources, FlexoResource origin,
 			Collection<FlexoResource<FlexoResourceData>> notified) {
 		for (FlexoResource<FlexoResourceData> res : resources.getResources()) {
 			if (notified.contains(res)) {
 				continue;
 			}
 			res.notifyDependantResourceChange(origin);
 			notified.add(res);
 			propagateResourceStatusChangeToAlteredResource(res.getAlteredResources(), origin, notified);
 		}
 	}
 
 	public void notifyResourceStatusChanged(FlexoResource<? extends FlexoResourceData> resource) {
 		for (FlexoResource<? extends FlexoResourceData> res : this) {
 			res.getDependentResources().update();
 			res.getAlteredResources().update();
 			res.getSynchronizedResources().update();
 		}
 		if (resource != null) {
 			propagateResourceStatusChangeToAlteredResource(resource.getAlteredResources(), resource,
 					new HashSet<FlexoResource<FlexoResourceData>>());
 		}
 		boolean isChanging = hasChanged();
 		setChanged(false);
 		notifyObservers(new ResourceStatusModification(null, null));
 		if (isChanging) {
 			setChanged(false);
 		}
 	}
 
 	public void notifyResourceChanged(FlexoResource resource) {
 		setChanged();
 		notifyObservers(new FlexoResourceChange(resource));
 	}
 
 	private boolean _backwardSynchronizationHasBeenPerformed = false;
 
 	public void notifyResourceHasBeenBackwardSynchronized(FlexoResource resource) {
 		notifyResourceChanged(resource);
 		_backwardSynchronizationHasBeenPerformed = true;
 	}
 
 	public boolean hasBackwardSynchronizationBeenPerformed() {
 		return _backwardSynchronizationHasBeenPerformed;
 	}
 
 	@Override
 	public synchronized void clearIsModified(boolean clearLastMemoryUpdate) {
 		// logger.info("Project clearIsModified()");
 		if (isDeserializing() && hasBackwardSynchronizationBeenPerformed()) {
 			return;
 		}
 		super.clearIsModified(clearLastMemoryUpdate);
 		_backwardSynchronizationHasBeenPerformed = false;
 	}
 
 	/*
 	 * public synchronized void setIsModified() { super.setIsModified(); notifyObservers(); }
 	 */
 
 	/*
 	 * public synchronized void clearIsModified() { super.clearIsModified(); notifyObservers(); }
 	 */
 
 	@Override
 	public String getFullyQualifiedName() {
 		return "PROJECT." + getProjectName();
 	}
 
 	/**
 	 * Don't use this method to get a new ID. Use getNewUniqueID instead
 	 * 
 	 * @return Returns the lastUniqueID.
 	 */
 	public long getLastUniqueID() {
 		if (getLastID() < 1) {
 			setLastID(1);
 		}
 		return getLastID();
 
 	}
 
 	/**
 	 * @param lastUniqueID
 	 *            The lastUniqueID to set.
 	 */
 	public void setLastUniqueID(long lastUniqueID) {
 		setLastID(lastUniqueID);
 	}
 
 	public Vector<OperationComponentDefinition> getAllInstanciatedOperationComponentDefinition() {
 		Vector<OperationComponentDefinition> reply = new Vector<OperationComponentDefinition>();
 		for (OperationComponentInstance inst : getProject().getFlexoWorkflow().getAllComponentInstances()) {
 			OperationComponentDefinition def = inst.getOperationComponentDefinition();
 			if (reply.contains(def)) {
 				continue;
 			}
 			reply.add(def);
 		}
 		return reply;
 	}
 
 	public Vector<TabComponentDefinition> getAllInstanciatedTabComponentDefinition() {
 		Vector<TabComponentDefinition> reply = new Vector<TabComponentDefinition>();
 
 		/*
 		 * for (OperationComponentDefinition opcd : getAllInstanciatedOperationComponentDefinition()) {
 		 * opcd.getWOComponent().getAllTabComponents(reply); } for (PopupComponentDefinition popcd :
 		 * getFlexoComponentLibrary().getPopupsComponentList()) { popcd.getWOComponent().getAllTabComponents(reply); }
 		 */
 		for (TabComponentDefinition tcd : getFlexoComponentLibrary().getTabComponentList()) {
 			if (tcd.getComponentInstances().size() > 0) {
 				reply.add(tcd);
 			}
 		}
 		return reply;
 	}
 
 	public FlexoProjectFile getJavaFormatterSettings() {
 		if (!new FlexoProjectFile(this, "FlexoJavaFormatSettings.xml").getFile().exists()) {
 			initJavaFormatter();
 		}
 		return new FlexoProjectFile(this, "FlexoJavaFormatSettings.xml");
 	}
 
 	/**
 	 *
 	 */
 	protected void initJavaFormatter() {
 		FileResource file = new FileResource("Config/FlexoJavaFormatSettings.xml");
 		FlexoProjectFile prjFile = new FlexoProjectFile(this, "FlexoJavaFormatSettings.xml");
 		try {
 			FileUtils.copyFileToFile(file, prjFile.getFile());
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 
 	public Date getCreationDate() {
 		return creationDate;
 	}
 
 	public void setCreationDate(Date creationDate) {
 		this.creationDate = creationDate;
 	}
 
 	public String getCreationDateAsString() {
 		if (getCreationDate() != null) {
 			return new SimpleDateFormat("dd/MM HH:mm:ss").format(getCreationDate());
 		}
 		return FlexoLocalization.localizedForKey("unknown");
 	}
 
 	public String getCreationUserId() {
 		return creationUserId;
 	}
 
 	public void setCreationUserId(String creationUserId) {
 		this.creationUserId = creationUserId;
 	}
 
 	public int getResourcesCount() {
 		return resources.size();
 	}
 
 	public void setResourcesCount(int resourcesCount) {
 		// logger.info("setResourcesCount with "+resourcesCount+" isDeserializing()="+isDeserializing());
 		if (isDeserializing()) {
 			((FlexoProjectBuilder) getBuilder()).initResourcesCount(resourcesCount);
 		}
 	}
 
 	public Enumeration<FlexoProcess> getSortedProcesses() {
 		return getFlexoWorkflow().getSortedProcesses();
 	}
 
 	/**
 	 * Ensure that all .prj contains required .cvsignore files
 	 * 
 	 */
 	public static void cvsIgnorize(File projectDirectory) {
 		File mainCVSIgnoreFile = new File(projectDirectory, ".cvsignore");
 		if (!mainCVSIgnoreFile.exists()) {
 			try {
 				FileUtils.saveToFile(mainCVSIgnoreFile, "*~\n" + ".#*\n" + "*.rmxml.ts\n" + "temp?.xml\n" + "*.ini\n" + "*.cvsrepository\n"
 						+ "*.bak\n" + "*.autosave\n");
 			} catch (IOException e) {
 				logger.warning("Could not create file " + mainCVSIgnoreFile + ": " + e.getMessage());
 				e.printStackTrace();
 			}
 		}
 
 		cvsIgnorizeDir(projectDirectory);
 
 	}
 
 	private static void cvsIgnorizeDir(File aDirectory) {
 		File cvsIgnoreFile = new File(aDirectory, ".cvsignore");
 		if (!cvsIgnoreFile.exists()) {
 			try {
 				FileUtils.saveToFile(cvsIgnoreFile, "*~\n" + ".#*\n" + "temp?.xml\n" + "*.rmxml.ts\n" + "*.history\n" + "PB.project\n"
 						+ "*.xcodeproj\n" + "*.autosave\n");
 			} catch (IOException e) {
 				logger.warning("Could not create file " + cvsIgnoreFile + ": " + e.getMessage());
 				e.printStackTrace();
 			}
 		}
 
 		File[] allDirs = aDirectory.listFiles(new java.io.FileFilter() {
 			@Override
 			public boolean accept(File pathname) {
 				return pathname.isDirectory() && !pathname.getName().equals("CVS") && !pathname.getName().equals(".history")
 						&& !pathname.getName().equals(".wo.LAST_ACCEPTED") && !pathname.getName().equals(".wo.LAST_GENERATED");
 			}
 		});
 
 		for (File f : allDirs) {
 			cvsIgnorizeDir(f);
 		}
 	}
 
 	/**
 	 * Remove all CVS directories
 	 */
 	public static void removeCVSDirs(File projectDirectory) {
 		removeCVSDirs(projectDirectory, true);
 	}
 
 	/**
 	 * Remove all CVS directories
 	 */
 	public static void removeCVSDirs(File aDirectory, boolean recurse) {
 		File cvsDir = new File(aDirectory, "CVS");
 		if (cvsDir.exists()) {
 			FileUtils.recursiveDeleteFile(cvsDir);
 		}
 		if (recurse) {
 			File[] allDirs = aDirectory.listFiles(new java.io.FileFilter() {
 				@Override
 				public boolean accept(File pathname) {
 					return pathname.isDirectory() && !pathname.getName().equals("CVS");
 				}
 			});
 			for (File f : allDirs) {
 				removeCVSDirs(f);
 			}
 		}
 	}
 
 	/**
 	 * Search (deeply) CVS directories Return true if at least one CVS dir was found
 	 */
 	public static boolean searchCVSDirs(File aDirectory) {
 		File cvsDir = new File(aDirectory, "CVS");
 		if (cvsDir.exists()) {
 			return true;
 		}
 		File[] allDirs = aDirectory.listFiles(new java.io.FileFilter() {
 			@Override
 			public boolean accept(File pathname) {
 				return pathname.isDirectory() && !pathname.getName().equals("CVS");
 			}
 		});
 		for (File f : allDirs) {
 			if (searchCVSDirs(f)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	public List<FlexoEditor> getEditors() {
 		return editors;
 	}
 
 	public void setEditors(List<FlexoEditor> editors) {
 		this.editors = editors;
 	}
 
 	public void addToEditors(FlexoEditor editor) {
 		editors.add(editor);
 	}
 
 	public void removeFromEditors(FlexoEditor editor) {
 		editors.remove(editor);
 	}
 
 	public void notifyObjectChanged(FlexoModelObject object) {
 		for (FlexoEditor ed : editors) {
 			ed.notifyObjectChanged(object);
 		}
 	}
 
 	public void notifyObjectDeleted(FlexoModelObject object) {
 		for (FlexoEditor ed : editors) {
 			ed.notifyObjectDeleted(object);
 		}
 	}
 
 	public void notifyObjectCreated(FlexoModelObject object) {
 		for (FlexoEditor ed : editors) {
 			ed.notifyObjectCreated(object);
 		}
 	}
 
 	public ProjectStatistics getStatistics() {
 		if (statistics == null) {
 			statistics = new ProjectStatistics(this);
 		}
 		return statistics;
 	}
 
 	public ProjectLoadingHandler getLoadingHandler() {
 		return loadingHandler;
 	}
 
 	public List<DocType> getDocTypes() {
 		if (docTypes.size() == 0) {
 			try {
 				for (DefaultDocType defaultDocType : DefaultDocType.values()) {
 					addToDocTypes(new DocType(defaultDocType.name(), this), false);
 				}
 			} catch (DuplicateDocTypeException e3) {
 				// Cannot happen, so we leave the stacktrace (if it happens, it
 				// means that someone miscoded something)
 				e3.printStackTrace();
 			}
 		}
 		if (docTypesAsString != null) {
 			StringTokenizer st = new StringTokenizer(docTypesAsString, ",", false);
 			while (st.hasMoreElements()) {
 				String s = (String) st.nextElement();
 				try {
 					addToDocTypes(new DocType(s, this), false);
 				} catch (DuplicateDocTypeException e) {
 					// Can happen but has no impact so we just ignore it
 				}
 			}
 			docTypesAsString = null;
 		}
 		return docTypes;
 	}
 
 	public void setDocTypes(List<DocType> docTypes) {
 		this.docTypes = docTypes;
 	}
 
 	public String getDocTypesAsString() {
 		if (getDocTypes().size() > 0) {
 			boolean isFirst = true;
 			StringBuilder sb = new StringBuilder();
 			for (DocType dt : docTypes) {
 				sb.append(dt.getName());
 				if (!isFirst) {
 					sb.append(',');
 				}
 				isFirst = false;
 			}
 			return sb.toString();
 		} else {
 			return null;
 		}
 	}
 
 	public void setDocTypesAsString(String docTypes) {
 		this.docTypesAsString = docTypes;
 	}
 
 	public void addToDocTypes(DocType docType) throws DuplicateDocTypeException {
 		addToDocTypes(docType, true);
 	}
 
 	private void addToDocTypes(DocType docType, boolean notify) throws DuplicateDocTypeException {
 		for (DocType dt : docTypes) {
 			if (dt.getName().equalsIgnoreCase(docType.getName())) {
 				throw new DuplicateDocTypeException(dt);
 			}
 		}
 		docTypes.add(docType);
 		if (notify) {
 			setChanged();
 			notifyObservers(new DocTypeAdded(docType));
 		}
 	}
 
 	public void removeFromDocTypes(DocType docType) throws CannotRemoveLastDocType {
 		if (docTypes.size() > 1) {
 			docTypes.remove(docType);
 			setChanged();
 			notifyObservers(new DocTypeRemoved(docType));
 		} else {
 			throw new CannotRemoveLastDocType();
 		}
 	}
 
 	public DocType getDocTypeNamed(String name) {
 		for (DocType dt : getDocTypes()) {
 			if (dt.getName().equals(name)) {
 				return dt;
 			}
 		}
 		return null;
 	}
 
 	public boolean getTimestampsHaveBeenLoaded() {
 		return timestampsHaveBeenLoaded;
 	}
 
 	public void setTimestampsHaveBeenLoaded(boolean timestampsHaveBeenLoaded) {
 		this.timestampsHaveBeenLoaded = timestampsHaveBeenLoaded;
 	}
 
 	/**
 	 * The following methods are inherited from the past and are here to replace static non-final references. From now on it is strictly
 	 * forbidden to use static non-final variables because FlexoServer can load several projects at the same time.
 	 * 
 	 */
 	public WidgetBindingDefinition getWidgetBindingDefinition(IEWidget widget, String name, Class<?> typeClass,
 			BindingDefinitionType bindingType, boolean mandatory) {
 		Hashtable<String, WidgetBindingDefinition> bindingsForWidget = widgetBindingDefinitions.get(widget);
 		if (bindingsForWidget == null) {
 			bindingsForWidget = new Hashtable<String, WidgetBindingDefinition>();
 			widgetBindingDefinitions.put(widget, bindingsForWidget);
 		}
 		WidgetBindingDefinition returned = bindingsForWidget.get(name);
 		DMType type = typeClass == null ? DMType.makeObjectDMType(widget.getProject()) : DMType.makeResolvedDMType(typeClass,
 				widget.getProject());
 
 		// DMEntity entityType = type==null?null:widget.getProject().getDataModel().getDMEntity(type,true);
 		if (returned == null || returned.getType() == null || !returned.getType().equals(type)) {
 			returned = new WidgetBindingDefinition(name, type, widget, bindingType, mandatory);
 			bindingsForWidget.put(name, returned);
 		}
 		return returned;
 	}
 
 	public WKFBindingDefinition getWKFObjectBindingDefinition(WKFObject wkfObject, String name, DMType type,
 			BindingDefinitionType bindingType, boolean mandatory) {
 		Hashtable<String, WKFBindingDefinition> bindingsForObject = wkfObjectBindingDefinitions.get(wkfObject);
 		if (bindingsForObject == null) {
 			bindingsForObject = new Hashtable<String, WKFBindingDefinition>();
 			wkfObjectBindingDefinitions.put(wkfObject, bindingsForObject);
 		}
 		WKFBindingDefinition returned = bindingsForObject.get(name);
 		if (returned == null || returned.getType() == null || returned.getType().equals(type)) {
 			returned = new WKFBindingDefinition(name, type, wkfObject, bindingType, mandatory);
 			bindingsForObject.put(name, returned);
 		}
 		return returned;
 	}
 
 	public Hashtable<String, Status> getGlobalStatus() {
 		return globalStatus;
 	}
 
 	public boolean lastUniqueIDHasBeenSet() {
 		return lastUniqueIDHasBeenSet;
 	}
 
 	public long getNewFlexoID() {
 		if (lastID < 0) {
 			return -1;
 		}
 		return ++lastID;
 	}
 
 	/**
 	 * @return Returns the lastUniqueID.
 	 */
 	public long getLastID() {
 		if (lastUniqueIDHasBeenSet && lastID < 0) {
 			lastID = 0;
 		}
 		return lastID;
 	}
 
 	/**
 	 * @param lastUniqueID
 	 *            The lastUniqueID to set.
 	 */
 	public void setLastID(long lastUniqueID) {
 		lastID = lastUniqueID;
 		lastUniqueIDHasBeenSet = true;
 	}
 
 	public boolean getLastUniqueIDHasBeenSet() {
 		return lastUniqueIDHasBeenSet;
 	}
 
 	public void register(FlexoModelObject object) {
 		if (!holdObjectRegistration && !(object instanceof TemporaryFlexoModelObject)) {
 			if (allRegisteredObjects == null) {
 				allRegisteredObjects = new Vector<FlexoModelObject>();
 			}
 			allRegisteredObjects.add(object);
 			if (_recentlyCreatedObjects == null) {
 				_recentlyCreatedObjects = new Vector<FlexoModelObject>();
 			}
 			_recentlyCreatedObjects.add(object);
 		}
 	}
 
 	public void unregister(FlexoModelObject object) {
 		if (!(object instanceof TemporaryFlexoModelObject)) {
 			allRegisteredObjects.remove(object);
 			_recentlyCreatedObjects.remove(object);
 		}
 	}
 
 	public FlexoModelObject findObject(String objectUID, long objectFlexoID) {
 		if (allRegisteredObjects == null) {
 			return null;
 		}
 		for (FlexoModelObject temp : allRegisteredObjects) {
 			if (temp.getFlexoID() == objectFlexoID && temp.getUserIdentifier().equals(objectUID)) {
 				// logger.info("Try to find "+objectUID+"_"+objectFlexoID+" : SUCCEEDED");
 				return temp;
 			}
 		}
 		// logger.info("Try to find "+objectUID+"_"+objectFlexoID+" : FAILED");
 		return null;
 	}
 
 	public void clearRecentlyCreatedObjects() {
 		_recentlyCreatedObjects.clear();
 	}
 
 	public void notifyRecentlyCreatedObjects() {
 		for (FlexoModelObject o : _recentlyCreatedObjects) {
 			if (o.getProject() == null) {
 				logger.severe("PROGRAMATION ISSUE : a FlexoModelObject of class " + o.getClass()
 						+ " don't have a project at notification time !");
 			}
 			if (o.getProject() == null || o.getProject() == this) {
 				if (!o.isDeleted()) {
 					notifyObjectCreated(o);
 				}
 			}
 		}
 	}
 
 	public List<FlexoModelObject> getAllRegisteredObjects() {
 		return allRegisteredObjects;
 	}
 
 	public DependencyAlgorithmScheme getDependancyScheme() {
 		return _dependancyScheme;
 	}
 
 	public void setDependancyScheme(DependencyAlgorithmScheme scheme) {
 		_dependancyScheme = scheme;
 	}
 
 	public static boolean getIsLoadingAProject() {
 		return false;
 	}
 
 	public FlexoResourceManager getResourceManagerInstance() {
 		return resourceManagerInstance;
 	}
 
 	public void setResourceManagerInstance(FlexoResourceManager resourceManagerInstance) {
 		this.resourceManagerInstance = resourceManagerInstance;
 	}
 
 	/**
 	 * @return
 	 */
 	public AbstractBindingStringConverter<AbstractBinding> getAbstractBindingConverter() {
 		return abstractBindingConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	public BindingValueStringConverter getBindingValueConverter() {
 		return bindingValueConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	public BindingExpressionStringConverter getBindingExpressionConverter() {
 		return bindingExpressionConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	public StaticBindingStringConverter getStaticBindingConverter() {
 		return staticBindingConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	public TranstypedBindingStringConverter getTranstypedBindingStringConverter() {
 		return transtypedBindingConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	public BindingAssignmentStringConverter getBindingAssignementConverter() {
 		return bindingAssignmentConverter;
 	}
 
 	/**
 	 * @return
 	 */
 	/*
 	 * public EditionPatternConverter getEditionPatternConverter() { return editionPatternConverter; }
 	 */
 
 	@Override
 	public StringEncoder getStringEncoder() {
 		return stringEncoder;
 	}
 
 	/**
 	 * @param resource
 	 * @param b
 	 * @throws SaveResourceException
 	 */
 	public synchronized void saveResourceData(FlexoStorageResource resource, boolean clearIsModified) throws SaveResourceException {
 		resource.saveResourceData(clearIsModified);
 	}
 
 	public FlexoWebServerFileResource getWebServerResource(String name) {
 		return (FlexoWebServerFileResource) resourceForKey(ResourceType.WEBSERVER, name);
 	}
 
 	@SuppressWarnings("unchecked")
 	public synchronized <T extends FlexoResource<? extends FlexoResourceData>> List<T> getResourcesOfClass(Class<T> resourceClass) {
 		List<T> reply = new Vector<T>();
 		for (FlexoResource<? extends FlexoResourceData> item : this) {
 			if (resourceClass.isInstance(item)) {
 				reply.add((T) item);
 			}
 		}
 		return reply;
 	}
 
 	public List<CGRepositoryFileResource<?, ?, ?>> getCGRepositoryResources() {
 		return getResourcesOfClass(CGRepositoryFileResource.class);
 	}
 
 	public List<FlexoXMLStorageResource<? extends XMLStorageResourceData>> getXMLStorageResources() {
 		return getResourcesOfClass(FlexoXMLStorageResource.class);
 	}
 
 	public List<FlexoStorageResource<? extends StorageResourceData>> getStorageResources() {
 		return getResourcesOfClass(FlexoStorageResource.class);
 	}
 
 	public List<FlexoFileResource<? extends FlexoResourceData>> getFileResources() {
 		return getResourcesOfClass(FlexoFileResource.class);
 	}
 
 	/**
 	 * Overrides finalize
 	 * 
 	 * @see java.lang.Object#finalize()
 	 */
 	@Override
 	protected void finalize() throws Throwable {
 		System.err.println("##########################################################\n" + "# Project finalization" + getID() + "\n"
 				+ "##########################################################");
 		super.finalize();
 	}
 
 	public String getUnusedImageName(String name) {
 		String attempt = name;
 		int i = 1;
 		while (resourceForFile(new File(getImportedImagesDir(), attempt)) != null) {
 			if (name.indexOf('.') > -1) {
 				attempt = name.substring(0, name.lastIndexOf('.')) + "-" + i + name.substring(name.lastIndexOf('.')).toLowerCase();
 			}
 			i++;
 		}
 		return attempt;
 	}
 
 	public FlexoModelObjectReferenceConverter getObjectReferenceConverter() {
 		return objectReferenceConverter;
 	}
 
 	public void setObjectReferenceConverter(FlexoModelObjectReferenceConverter objectReferenceConverter) {
 		this.objectReferenceConverter = objectReferenceConverter;
 	}
 
 	public FlexoIEBasicPalette getBasicPalette() {
 		if (basicPalette == null) {
 			basicPalette = new FlexoIEBasicPalette(this);
 		}
 		return basicPalette;
 	}
 
 	public FlexoIEImagePalette getImagePalette() {
 		if (imagePalette == null) {
 			imagePalette = new FlexoIEImagePalette(this);
 		}
 		return imagePalette;
 	}
 
 	public FlexoIECustomWidgetPalette getCustomWidgetPalette() {
 		if (customWidgetPalette == null) {
 			customWidgetPalette = new FlexoIECustomWidgetPalette(this);
 		}
 		return customWidgetPalette;
 	}
 
 	public FlexoIECustomImagePalette getCustomImagePalette() {
 		if (customImagePalette == null) {
 			customImagePalette = new FlexoIECustomImagePalette(this);
 		}
 		return customImagePalette;
 	}
 
 	private final Vector<ImageFile> availableImageFiles = new Vector<ImageFile>();
 
 	public void resetAvailableImages() {
 		availableImageFiles.clear();
 	}
 
 	private ImageFile defaultImage;
 
 	public ImageFile getDefaultImageFile() {
 		return defaultImage;
 	}
 
 	public Vector<ImageFile> getAvailableImageFiles() {
 		if (availableImageFiles.size() == 0) {
 			for (FlexoIEImage image : getImagePalette().getWidgets()) {
 				ImageFile imageFile = new ImageFile(image.getScreenshotFile(null));
 				if (image.getScreenshotFile(null).equals(WRLocator.AGILE_BIRDS_LOGO)) {
 					defaultImage = imageFile;
 				}
 				availableImageFiles.add(imageFile);
 			}
 			Vector<ImageFile> temp = new Vector<ImageFile>();
 			for (FlexoIECustomImage image : getCustomImagePalette().getWidgets()) {
 				temp.add(new ImageFile(image.getScreenshotFile(null)));
 			}
 			Collections.sort(temp, new Comparator<ImageFile>() {
 				@Override
 				public int compare(ImageFile o1, ImageFile o2) {
 					return o1.getImageFile().compareTo(o2.getImageFile());
 				}
 			});
 			availableImageFiles.addAll(temp);
 		}
 		return availableImageFiles;
 	}
 
 	/*
 	 * public ImageFile getImageFileForName(String imageName) { ImageFile file = null; if (imageName != null) { File f =
 	 * WRLocator.locate(getProjectDirectory(), imageName, getCssSheet() == null ? FlexoCSS.CONTENTO.getName() : getCssSheet().getName()); if
 	 * (f==null || !f.exists()) {
 	 * 
 	 * f = WRLocator.DENALI_LOGO; imageName = f.getName(); } file = new ImageFile(f); } return file; }
 	 */private class ImageFileConverter extends Converter<ImageFile> {
 		public ImageFileConverter() {
 			super(ImageFile.class);
 		}
 
 		@Override
 		public ImageFile convertFromString(String value) {
 			ImageFile file = null;
 			Vector<ImageFile> v = getAvailableImageFiles();
 			for (int i = 0; i < v.size(); i++) {
 				file = v.get(i);
 				if (file.equals(value)) {
 					break;
 				}
 			}
 			if (value != null && value.startsWith("/ImportedImages/")) {
 				return convertFromString(value.substring("/ImportedImages/".length()));
 			}
 			if (file == null) {
 				if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Could not find '" + value + "' replacing with Denali logo");
 				}
 				return getDefaultImageFile();
 			}
 			return file;
 		}
 
 		@Override
 		public String convertToString(ImageFile value) {
 			return value.getImageName();
 		}
 	}
 
 	public class ImageFile extends KVCObject implements StringConvertable<ImageFile> {
 		private static final String HAS_FILE_EXTENSION_REGEXP = ".+\\.[a-zA-Z0-9]{3}$";
 
 		private String imageName;
 
 		public ImageFile(String imageName) {
 			this.imageName = imageName;
 		}
 
 		public ImageFile(File imageFile) {
 			this(imageNameForFile(imageFile));
 		}
 
 		public String getImageName() {
 			return imageName;
 		}
 
 		public String getBeautifiedImageName() {
 			if (imageName == null) {
 				return null;
 			}
 			String name = imageName;
 			if (name.startsWith("_Button_")) {
 				if (name.matches(HAS_FILE_EXTENSION_REGEXP)) {
 					name = name.substring("_Button_".length(), name.length() - 4);
 				} else {
 					name = name.substring("_Button_".length());
 				}
 			} else if (name.startsWith("Icon_")) {
 				if (name.matches(HAS_FILE_EXTENSION_REGEXP)) {
 					name = name.substring("Icon_".length(), name.length() - 4);
 				} else {
 					name = name.substring("Icon_".length());
 				}
 			} else if (name.startsWith("_Icon_")) {
 				if (name.matches(HAS_FILE_EXTENSION_REGEXP)) {
 					name = name.substring("_Icon_".length(), name.length() - 4);
 				} else {
 					name = name.substring("_Icon_".length());
 				}
 			}
 			return name.replace('_', ' ');
 		}
 
 		public File getImageFile() {
 			File f = WRLocator.locate(getProjectDirectory(), imageName, getCssSheet() == null ? FlexoCSS.CONTENTO.getName() : getCssSheet()
 					.getName());
 			if (f == null || !f.exists()) {
 				if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Could not find '" + imageName + "' replacing with Denali logo");
 				}
 				f = WRLocator.AGILE_BIRDS_LOGO;
 				imageName = f.getName();
 			}
 			return f;
 		}
 
 		public boolean exists() {
 			return getImageFile() != null && getImageFile().exists();
 		}
 
 		/**
 		 * Overrides equals
 		 * 
 		 * @see java.lang.Object#equals(java.lang.Object)
 		 */
 		@Override
 		public boolean equals(Object obj) {
 			if (imageName != null && obj instanceof ImageFile) {
 				return ((ImageFile) obj).getImageName().equals(getImageName());
 			} else if (imageName != null && obj instanceof String) {
 				return imageName.equals(obj);
 			}
 			return super.equals(obj);
 		}
 
 		@Override
 		public Converter<? extends ImageFile> getConverter() {
 			return imageFileConverter;
 		}
 
 		public boolean isImported() {
 			return getImageFile().getParentFile().equals(getImportedImagesDir());
 		}
 
 		/**
 		 * @param b
 		 */
 		public File createButton(File output) {
 			File file = getImageFile();
 			if (file == null) {
 				return null;
 			}
 			if (!output.exists()) {
 				try {
 					output.createNewFile();
 				} catch (IOException e1) {
 					e1.printStackTrace();
 					return output;
 				}
 			}
 			OutputStream out;
 			try {
 				out = new FileOutputStream(output);
 			} catch (FileNotFoundException e1) {
 				e1.printStackTrace();
 				return output;
 			}
 			try {
 				ImageIcon icon = new ImageIcon(file.getAbsolutePath());
 				Image i = icon.getImage();
 				BufferedImage bi = ImageIO.read(file);
 				BufferedImage image = new BufferedImage(bi.getWidth(null), bi.getHeight(null), BufferedImage.TYPE_INT_RGB);
 				image.createGraphics().drawImage(i, 0, 0, bi.getWidth(null), bi.getHeight(null), null);
 				ImageIO.write(image, "jpg", out);
 				return output;
 			} catch (IOException e) {
 				e.printStackTrace();
 				return null;
 			} finally {
 				try {
 					out.close();
 				} catch (IOException e) {
 					e.printStackTrace();
 				}
 			}
 		}
 	}
 
 	public String imageNameForFile(File f) {
 		if (f == null || f.getParentFile() == null) {
 			return null;
 		}
 		if (f.getName().startsWith("Contento_")) {
 			return ToolBox.replaceStringByStringInString("Contento", "", f.getName());
 		}
 		if (f.getName().startsWith("Flexo_")) {
 			return ToolBox.replaceStringByStringInString("Flexo", "", f.getName());
 		}
 		if (f.getName().startsWith("Omniscio_")) {
 			return ToolBox.replaceStringByStringInString("Omniscio", "", f.getName());
 		}
 		if (f.getParentFile().getName().equals("Images") || f.getParentFile().equals(getImportedImagesDir())) {
 			return f.getName();
 		}
 		return f.getParentFile().getName() + "/" + f.getName();
 	}
 
 	public FlexoIEBIRTPalette getBIRTPalette() {
 		if (birtPalette == null) {
 			birtPalette = new FlexoIEBIRTPalette(this);
 		}
 		return birtPalette;
 	}
 
 	public boolean isHoldingProjectRegistration() {
 		return holdObjectRegistration;
 	}
 
 	public void holdObjectRegistration() {
 		holdObjectRegistration = true;
 	}
 
 	public void unholdObjectRegistration() {
 		holdObjectRegistration = false;
 	}
 
 	private boolean _rebuildDependanciesIsRequired = false;
 
 	public void setRebuildDependanciesIsRequired() {
 		_rebuildDependanciesIsRequired = true;
 	}
 
 	public boolean rebuildDependanciesIsRequired() {
 		return _rebuildDependanciesIsRequired;
 	}
 
 	public void addToFilesToDelete(File f) {
 		filesToDelete.add(f);
 	}
 
 	public void removeFromFilesToDelete(File f) {
 		filesToDelete.remove(f);
 	}
 
 	public void deleteFilesToBeDeleted() {
 		for (File f : filesToDelete) {
 			try {
 				if (FileUtils.recursiveDeleteFile(f)) {
 					if (logger.isLoggable(Level.INFO)) {
 						logger.info("Successfully deleted " + f.getAbsolutePath());
 						// filesToDelete.remove(f);
 					}
 				} else if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Could not delete " + f.getAbsolutePath());
 				}
 			} catch (RuntimeException e) {
 				e.printStackTrace();
 				if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Could not delete " + f.getAbsolutePath());
 				}
 			}
 		}
 		filesToDelete.clear();
 	}
 
 	/**
 	 * Overrides getClassNameKey
 	 * 
 	 * @see org.openflexo.foundation.FlexoModelObject#getClassNameKey()
 	 */
 	@Override
 	public String getClassNameKey() {
 		return "flexo_project";
 	}
 
 	public List<File> getFilesToDelete() {
 		return filesToDelete;
 	}
 
 	public void setFilesToDelete(List<File> filesToDel) {
 		this.filesToDelete = filesToDel;
 	}
 
 	public OperationNode getFirstOperation() {
 		if (getRootFlexoProcess() != null) {
 			if (firstOperation == null && firstOperationFlexoID > -1) {
 				firstOperation = getRootFlexoProcess().getOperationNodeWithFlexoID(firstOperationFlexoID);
 			}
 			if (firstOperation == null) {
 				List<OperationNode> v = getRootFlexoProcess().getAllOperationNodesWithComponent();
 				if (v.size() > 0) {
 					firstOperation = v.get(0);
 					setChanged();
 					notifyObservers(new WKFAttributeDataModification("firstOperation", null, firstOperation));
 
 				}
 			}
 		}
 		return firstOperation;
 	}
 
 	public void setFirstOperation(OperationNode firstOp) {
 		OperationNode old = this.firstOperation;
 		this.firstOperation = firstOp;
 		setChanged();
 		notifyObservers(new WKFAttributeDataModification("firstOperation", old, firstOp));
 	}
 
 	public long getFirstOperationFlexoID() {
 		if (firstOperation != null) {
 			return getFirstOperation().getFlexoID();
 		} else {
 			return -1;
 		}
 	}
 
 	public void setFirstOperationFlexoID(long firstOpFlexoID) {
 		this.firstOperationFlexoID = firstOpFlexoID;
 	}
 
 	/**
 	 * @deprecated : use getImportedImagesDir
 	 * @return
 	 */
 	@Deprecated
 	public File getSpecificButtonDirectory() {
 		if (_specificButtonDir == null) {
 			_specificButtonDir = new File(getProjectDirectory(), "specificButtons");
 			if (!_specificButtonDir.exists()) {
 				_specificButtonDir.mkdirs();
 			}
 		}
 		return _specificButtonDir;
 	}
 
 	/**
 	 * @deprecated : use _importedImagesDir
 	 */
 	@Deprecated
 	private File _specificButtonDir;
 
 	public File getImportedImagesDir() {
 		if (_importedImagesDir == null) {
 			_importedImagesDir = new File(getProjectDirectory(), FileCst.IMPORTED_IMAGE_DIR_NAME);
 			if (!_importedImagesDir.exists()) {
 				_importedImagesDir.mkdirs();
 				FlexoWebServerFileResource.importSpecificButtonsIntoResources(this);
 			}
 		}
 		return _importedImagesDir;
 	}
 
 	private File _importedImagesDir;
 
 	private DependencyAlgorithmScheme _dependancyScheme = DependencyAlgorithmScheme.Optimistic;
 
 	public File getFrameworksToEmbedDirectory() {
 		return getProjectDirectoryWithName(FRAMEWORKS_DIRECTORY);
 	}
 
 	public File getHTMLToEmbedDirectory() {
 		return getProjectDirectoryWithName(HTML_DIRECTORY);
 	}
 
 	public File getDocxToEmbedDirectory() {
 		return getProjectDirectoryWithName(DOCX_DIRECTORY);
 	}
 
 	public File getLatexToEmbedDirectory() {
 		return getProjectDirectoryWithName(LATEX_DIRECTORY);
 	}
 
 	public File getProcessSnapshotImportedDirectory() {
 		return getProjectDirectoryWithName(PROCESS_SNAPSHOT_IMPORTED_DIRECTORY);
 	}
 
 	public File getProcessSnapshotLocalDirectory() {
 		return getProjectDirectoryWithName(PROCESS_SNAPSHOT_LOCAL_DIRECTORY);
 	}
 
 	/**
 	 * @return
 	 */
 	private File getProjectDirectoryWithName(String path) {
 		File file = new File(getProjectDirectory(), path);
 		if (file.exists()) {
 			return file;
 		} else {
 			final String loweredPath = path.toLowerCase();
 			File[] files = getProjectDirectory().listFiles(new FilenameFilter() {
 				@Override
 				public boolean accept(File dir, String name) {
 					return name.toLowerCase().indexOf(loweredPath) > -1;
 				}
 			});
 			for (int i = 0; i < files.length; i++) {
 				File file2 = files[i];
 				if (file2.isDirectory()) {
 					return file2;
 				}
 			}
 			// If we get here, it means that there are no Frameworks dir, so let's create the default one
 			file.mkdir();
 			return file;
 		}
 	}
 
 	public File[] listFrameworksToEmbed() {
 		final File f = getFrameworksToEmbedDirectory();
 		if (f != null && f.exists()) {
 			return f.listFiles(new FilenameFilter() {
 
 				@Override
 				public boolean accept(File dir, String name) {
 					return f.equals(dir) && name.endsWith(".framework");
 				}
 
 			});
 		} else {
 			return new File[0];
 		}
 	}
 
 	/**
 	 * Overrides getAllEmbeddedValidableObjects
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#getAllEmbeddedValidableObjects()
 	 */
 	@Override
 	public Vector<Validable> getAllEmbeddedValidableObjects() {
 		Vector<Validable> v = new Vector<Validable>();
 		v.add(this);
 		for (FlexoStorageResource<? extends StorageResourceData> r : getLoadedStorageResources()) {
 			if (r.getResourceData() instanceof Validable && r.getResourceData() != this) {
 				v.addAll(((Validable) r.getResourceData()).getAllEmbeddedValidableObjects());
 			}
 		}
 		return v;
 	}
 
 	/**
 	 * Overrides getDefaultValidationModel
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#getDefaultValidationModel()
 	 */
 	@Override
 	public ValidationModel getDefaultValidationModel() {
 		return getProjectValidationModel();
 	}
 
 	/**
 	 * Overrides isValid
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#isValid()
 	 */
 	@Override
 	public boolean isValid() {
 		return isValid(getDefaultValidationModel());
 	}
 
 	/**
 	 * Overrides isValid
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#isValid(org.openflexo.foundation.validation.ValidationModel)
 	 */
 	@Override
 	public boolean isValid(ValidationModel validationModel) {
 		return validationModel.isValid(this);
 	}
 
 	/**
 	 * Overrides validate
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#validate()
 	 */
 	@Override
 	public ValidationReport validate() {
 		return validate(getDefaultValidationModel());
 	}
 
 	/**
 	 * Overrides validate
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#validate(org.openflexo.foundation.validation.ValidationModel)
 	 */
 	@Override
 	public synchronized ValidationReport validate(ValidationModel validationModel) {
 		return validationModel.validate(this);
 	}
 
 	/**
 	 * Overrides validate
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#validate(org.openflexo.foundation.validation.ValidationReport)
 	 */
 	@Override
 	public void validate(ValidationReport report) {
 		validate(report, getDefaultValidationModel());
 	}
 
 	/**
 	 * Overrides validate
 	 * 
 	 * @see org.openflexo.foundation.validation.Validable#validate(org.openflexo.foundation.validation.ValidationReport,
 	 *      org.openflexo.foundation.validation.ValidationModel)
 	 */
 	@Override
 	public synchronized void validate(ValidationReport report, ValidationModel validationModel) {
 		validationModel.validate(this, report);
 	}
 
 	public static class FlexoIDMustBeUnique extends ValidationRule<FlexoIDMustBeUnique, FlexoProject> {
 
 		/**
 		 * @param objectType
 		 * @param ruleName
 		 */
 		public FlexoIDMustBeUnique() {
 			super(FlexoProject.class, "flexo_id_must_be_unique");
 		}
 
 		/**
 		 * Overrides applyValidation
 		 * 
 		 * @see org.openflexo.foundation.validation.ValidationRule#applyValidation(org.openflexo.foundation.validation.Validable)
 		 */
 		@Override
 		public ValidationIssue<FlexoIDMustBeUnique, FlexoProject> applyValidation(FlexoProject object) {
 			List<FlexoModelObject> badObjects = object.getObjectIDManager().checkProject(true);
 			if (badObjects.size() > 0) {
 				DuplicateObjectIDIssue issues = new DuplicateObjectIDIssue(object);
 				for (FlexoModelObject obj : badObjects) {
 					issues.addToContainedIssues(new InformationIssue<FlexoIDMustBeUnique, FlexoProject>(object,
 							"identifier_of_($object.fullyQualifiedName)_was_duplicated_and_reset_to_($object.flexoID)"));
 				}
 				return issues;
 			} else {
 				return new InformationIssue<FlexoIDMustBeUnique, FlexoProject>(object, "no_duplicated_identifiers_found");
 			}
 		}
 
 		public static class DuplicateObjectIDIssue extends CompoundIssue<FlexoIDMustBeUnique, FlexoProject> {
 
 			public DuplicateObjectIDIssue(FlexoProject anObject) {
 				super(anObject);
 			}
 
 		}
 	}
 
 	public static class AllResourcesMustBeDefinedInProject extends ValidationRule<AllResourcesMustBeDefinedInProject, FlexoProject> {
 
 		/**
 		 * @param objectType
 		 * @param ruleName
 		 */
 		public AllResourcesMustBeDefinedInProject() {
 			super(FlexoProject.class, "all_resources_must_be_defined_in_project");
 		}
 
 		/**
 		 * Overrides applyValidation
 		 * 
 		 * @see org.openflexo.foundation.validation.ValidationRule#applyValidation(org.openflexo.foundation.validation.Validable)
 		 */
 		@Override
 		public ValidationIssue<AllResourcesMustBeDefinedInProject, FlexoProject> applyValidation(FlexoProject p) {
 			p.checkResourceIntegrity();
 			boolean ok = true;
 			for (FlexoResource<? extends FlexoResourceData> r : p) {
 				for (FlexoResource<FlexoResourceData> dr : r.getDependentResources()) {
 					if (p.getResources().get(dr.getResourceIdentifier()) == null) {
 						if (logger.isLoggable(Level.INFO)) {
 							logger.info("Found a dependant resource not in project: " + dr.getResourceIdentifier());
 						}
 						ok = false;
 					}
 				}
 				for (FlexoResource<FlexoResourceData> sync : r.getSynchronizedResources()) {
 					if (p.getResources().get(sync.getResourceIdentifier()) == null) {
 						if (logger.isLoggable(Level.INFO)) {
 							logger.info("Found a synchronized resource not in project: " + sync.getResourceIdentifier());
 						}
 						ok = false;
 					}
 				}
 				for (FlexoResource<FlexoResourceData> alt : r.getAlteredResources()) {
 					if (p.getResources().get(alt.getResourceIdentifier()) == null) {
 						if (logger.isLoggable(Level.INFO)) {
 							logger.info("Found an altered resource not in project: " + alt.getResourceIdentifier());
 						}
 						ok = false;
 					}
 				}
 			}
 			if (ok) {
 				return new InformationIssue<AllResourcesMustBeDefinedInProject, FlexoProject>(p, "no_dereferenced_resources_found");
 			} else {
 				return new ValidationError<AllResourcesMustBeDefinedInProject, FlexoProject>(this, p,
 						"dependant_altered_synchronized_resources_exists_but_not_in_project", new RemoveUnexistentResources());
 			}
 		}
 
 		public class RemoveUnexistentResources extends FixProposal<AllResourcesMustBeDefinedInProject, FlexoProject> {
 			/**
 			  *
 			  */
 			public RemoveUnexistentResources() {
 				super("remove_unexistent_resources");
 			}
 
 			/**
 			 * Overrides fixAction
 			 * 
 			 * @see org.openflexo.foundation.validation.FixProposal#fixAction()
 			 */
 			@Override
 			protected void fixAction() {
 				FlexoProject p = getObject();
 				for (FlexoResource<? extends FlexoResourceData> r : p) {
 					Iterator<FlexoResource<FlexoResourceData>> i = r.getDependentResources().iterator();
 					while (i.hasNext()) {
 						FlexoResource<FlexoResourceData> dr = i.next();
 						if (p.getResources().get(dr.getResourceIdentifier()) == null) {
 							i.remove();
 						}
 					}
 					i = r.getSynchronizedResources().iterator();
 					while (i.hasNext()) {
 						FlexoResource<FlexoResourceData> dr = i.next();
 						if (p.getResources().get(dr.getResourceIdentifier()) == null) {
 							i.remove();
 						}
 					}
 					i = r.getAlteredResources().iterator();
 					while (i.hasNext()) {
 						FlexoResource<FlexoResourceData> dr = i.next();
 						if (p.getResources().get(dr.getResourceIdentifier()) == null) {
 							i.remove();
 						}
 					}
 				}
 			}
 		}
 	}
 
 	public static class NameOfResourceMustBeKeyOfHashtableEntry extends
 			ValidationRule<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject> {
 
 		/**
 		 * @param objectType
 		 * @param ruleName
 		 */
 		public NameOfResourceMustBeKeyOfHashtableEntry() {
 			super(FlexoProject.class, "name_of_resource_must_be_key_of_hashtable_entry");
 		}
 
 		/**
 		 * Overrides applyValidation
 		 * 
 		 * @see org.openflexo.foundation.validation.ValidationRule#applyValidation(org.openflexo.foundation.validation.Validable)
 		 */
 		@Override
 		public ValidationIssue<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject> applyValidation(FlexoProject project) {
 			ValidationError<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject> issues = null;
 			for (Entry<String, FlexoResource<? extends FlexoResourceData>> e : project.getResources().entrySet()) {
 				FlexoResource<? extends FlexoResourceData> resource = e.getValue();
 				if (!e.getKey().equals(resource.getResourceIdentifier())) {
 					issues = new ValidationError<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject>(this, project,
 							"name_of_resource_must_be_key_of_hashtable_entry", new RestoreResourceKeys());
 					break;
 				}
 			}
 			if (issues != null) {
 				return issues;
 			} else {
 				return new InformationIssue<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject>(project,
 						"no_inconsistant_resource_name_found");
 			}
 		}
 
 		public class RestoreResourceKeys extends FixProposal<NameOfResourceMustBeKeyOfHashtableEntry, FlexoProject> {
 
 			/**
 			 * @param aMessage
 			 */
 			public RestoreResourceKeys() {
 				super("restore_resource_keys");
 			}
 
 			/**
 			 * Overrides fixAction
 			 * 
 			 * @see org.openflexo.foundation.validation.FixProposal#fixAction()
 			 */
 			@Override
 			protected void fixAction() {
 				getProject().resources.restoreKeys();
 			}
 
 		}
 	}
 
 	public static class RebuildDependancies extends ValidationRule<RebuildDependancies, FlexoProject> {
 
 		public RebuildDependancies() {
 			super(FlexoProject.class, "rebuild_dependancies");
 		}
 
 		@Override
 		public ValidationIssue<RebuildDependancies, FlexoProject> applyValidation(FlexoProject object) {
 			object.rebuildDependencies();
 			return new InformationIssue<RebuildDependancies, FlexoProject>(object, "resource_dependancies_have_been_rebuilt");
 		}
 	}
 
 	public static class ResourceCanNotDeeplyDependOfItself extends ValidationRule<ResourceCanNotDeeplyDependOfItself, FlexoProject> {
 
 		/**
 		 * @param objectType
 		 * @param ruleName
 		 */
 		public ResourceCanNotDeeplyDependOfItself() {
 			super(FlexoProject.class, "resource_cannot_deeply_depend_of_itself");
 		}
 
 		/**
 		 * Overrides applyValidation
 		 * 
 		 * @see org.openflexo.foundation.validation.ValidationRule#applyValidation(org.openflexo.foundation.validation.Validable)
 		 */
 		@Override
 		public ValidationIssue<ResourceCanNotDeeplyDependOfItself, FlexoProject> applyValidation(FlexoProject project) {
 			CompoundIssue<ResourceCanNotDeeplyDependOfItself, FlexoProject> issues = null;
 			for (FlexoResource<? extends FlexoResourceData> resource : project) {
 				if (resource.deeplyDependsOfItSelf()) {
 					if (issues == null) {
 						issues = new CompoundIssue<ResourceCanNotDeeplyDependOfItself, FlexoProject>(project);
 					}
 					issues.addToContainedIssues(new ValidationError<ResourceCanNotDeeplyDependOfItself, FlexoProject>(this, project,
 							"resource_cannot_deeply_depend_of_itself", new BreakCycleDependances(resource)));
 				}
 			}
 			if (issues != null) {
 				return issues;
 			} else {
 				return new InformationIssue<ResourceCanNotDeeplyDependOfItself, FlexoProject>(project,
 						"no_inconsistant_resource_name_found");
 			}
 		}
 
 		public class BreakCycleDependances extends FixProposal<ResourceCanNotDeeplyDependOfItself, FlexoProject> {
 			private final FlexoResource resource;
 
 			/**
 			 * @param aMessage
 			 */
 			public BreakCycleDependances(FlexoResource aResource) {
 				super("break_cycle_dependances");
 				resource = aResource;
 			}
 
 			/**
 			 * Overrides fixAction
 			 * 
 			 * @see org.openflexo.foundation.validation.FixProposal#fixAction()
 			 */
 			@Override
 			protected void fixAction() {
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Implement me");
 				}
 			}
 
 		}
 	}
 
 	/**
 	 * @author gpolet
 	 * 
 	 */
 	public static class GeneratedResourcesMustHaveCGFile extends ValidationRule<GeneratedResourcesMustHaveCGFile, FlexoProject> {
 
 		/**
 		 * @param objectType
 		 * @param ruleName
 		 */
 		public GeneratedResourcesMustHaveCGFile() {
 			super(FlexoProject.class, "generated_resources_must_have_CGFile");
 		}
 
 		/**
 		 * Overrides applyValidation
 		 * 
 		 * @see org.openflexo.foundation.validation.ValidationRule#applyValidation(org.openflexo.foundation.validation.Validable)
 		 */
 		@Override
 		public ValidationIssue<GeneratedResourcesMustHaveCGFile, FlexoProject> applyValidation(FlexoProject project) {
 			if (project.getGeneratedCodeResource(false) != null && !project.getGeneratedCodeResource(false).isLoaded()
 					|| project.getGeneratedDocResource(false) != null && !project.getGeneratedDocResource(false).isLoaded()) {
 				return null;// If the generated code or the generated doc resource is not loaded, then CGFiles have not yet been associated
 				// with their resource!
 			}
 			for (Entry<String, FlexoResource<? extends FlexoResourceData>> e : project.getResources().entrySet()) {
 				FlexoResource<? extends FlexoResourceData> resource = e.getValue();
 				if (resource instanceof CGRepositoryFileResource) {
 					CGRepositoryFileResource cgr = (CGRepositoryFileResource) resource;
 					if (cgr.getCGFile() == null) {
 						return new ValidationError<GeneratedResourcesMustHaveCGFile, FlexoProject>(this, project,
 								"some_generated_resources_dont_have_a_file", new DeleteGeneratedResourceWithoutFiles());
 					}
 				}
 			}
 			return null;
 		}
 
 		public class DeleteGeneratedResourceWithoutFiles extends FixProposal<GeneratedResourcesMustHaveCGFile, FlexoProject> {
 
 			public DeleteGeneratedResourceWithoutFiles() {
 				super("fix_invalid_generated_resource");
 			}
 
 			@Override
 			protected void fixAction() {
 				FlexoProject project = getObject();
 				for (CGRepositoryFileResource<?, ?, ?> r : project.getCGRepositoryResources()) {
 					if (r.getCGFile() == null) {
 						r.delete(false);
 					}
 				}
 			}
 		}
 
 	}
 
 	public static class ComponentInstancesMustDefineAComponent extends ValidationRule<ComponentInstancesMustDefineAComponent, FlexoProject> {
 
 		public class FixComponentInstances extends FixProposal<ComponentInstancesMustDefineAComponent, FlexoProject> {
 
 			public FixComponentInstances() {
 				super("fix_invalid_component_instances");
 			}
 
 			@Override
 			protected void fixAction() {
 				FlexoProject project = getObject();
 				for (ComponentDefinition cd : project.getFlexoComponentLibrary().getAllComponentList()) {
 					IEWOComponent wo = cd.getWOComponent();
 					wo.getRootSequence().removeInvalidComponentInstances();
 				}
 			}
 		}
 
 		public ComponentInstancesMustDefineAComponent() {
 			super(FlexoProject.class, "component_instance_must_define_a_component");
 
 		}
 
 		@Override
 		public ValidationIssue<ComponentInstancesMustDefineAComponent, FlexoProject> applyValidation(FlexoProject object) {
 			FlexoProject project = object;
 			Enumeration<ComponentDefinition> en = project.getFlexoComponentLibrary().getAllComponentList().elements();
 			while (en.hasMoreElements()) {
 				ComponentDefinition cd = en.nextElement();
 				IEWOComponent wo = cd.getWOComponent();
 				if (!wo.getRootSequence().areComponentInstancesValid()) {
 					return new ValidationError<ComponentInstancesMustDefineAComponent, FlexoProject>(this, project,
 							"there_are_some_invalid_component_instances", new FixComponentInstances());
 				}
 			}
 			return null;
 		}
 
 	}
 
 	public List<ValidationReport> checkModelConsistency(CodeType generationTarget) {
 		return checkModelConsistency(null, null, null, null, generationTarget);
 	}
 
 	public void checkResourceIntegrity() {
 		List<FlexoResource<? extends FlexoResourceData>> v = new ArrayList<FlexoResource<? extends FlexoResourceData>>(getResources()
 				.values());
 		FlexoResource.sortResourcesWithDependancies(v);
 		List<FlexoResource<? extends FlexoResourceData>> resourcesToDelete = new ArrayList<FlexoResource<? extends FlexoResourceData>>();
 		for (FlexoResource<? extends FlexoResourceData> resource : v) {
 			if (!resource.checkIntegrity()) {
 				resourcesToDelete.add(resource);
 			}
 		}
 		if (resourcesToDelete.size() > 0) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Found " + resourcesToDelete.size() + " resource that are no more acceptable.");
 			}
 			for (FlexoResource<? extends FlexoResourceData> resource : resourcesToDelete) {
 				if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Deleting " + resource.getFullyQualifiedName());
 				}
 				if (resource instanceof FlexoFileResource) {
 					((FlexoFileResource<? extends FlexoResourceData>) resource).delete(false); // Let's be cautious here.
 				} else {
 					resource.delete();
 				}
 			}
 		}
 	}
 
 	public List<ValidationReport> checkModelConsistency(FlexoObserver ieValidationObserver, FlexoObserver wkfValidationObserver,
 			FlexoObserver dmValidationObserver, FlexoObserver dkvValidationObserver, CodeType generationTarget) {
 		List<ValidationReport> reply = new Vector<ValidationReport>();
 		if (getFlexoComponentLibrary(false) != null) {
 			// We validate the component library model
 			IEValidationModel ieValidationModel = new IEValidationModel(this, generationTarget);
 			if (ieValidationObserver != null) {
 				ieValidationModel.addObserver(ieValidationObserver);
 			}
 
 			ValidationReport report = getProject().getFlexoComponentLibrary().validate(ieValidationModel);
 			if (ieValidationObserver != null) {
 				ieValidationModel.deleteObserver(ieValidationObserver);
 			}
 			reply.add(report);
 		}
 
 		if (getFlexoWorkflow(false) != null) {
 			// We validate the workflow model
 			WKFValidationModel wkfValidationModel = new WKFValidationModel(getProject(), generationTarget);
 			if (wkfValidationObserver != null) {
 				wkfValidationModel.addObserver(wkfValidationObserver);
 			}
 			ValidationReport report = getProject().getFlexoWorkflow().validate(wkfValidationModel);
 			if (wkfValidationObserver != null) {
 				wkfValidationModel.deleteObserver(wkfValidationObserver);
 			}
 			reply.add(report);
 		}
 
 		if (getDKVModel(false) != null) {
 			// We validate the dkv model
 			DKVValidationModel dkvValidationModel = new DKVValidationModel(getProject(), generationTarget);
 			if (dkvValidationObserver != null) {
 				dkvValidationModel.addObserver(dkvValidationObserver);
 			}
 			ValidationReport report = getProject().getDKVModel().validate(dkvValidationModel);
 			if (dkvValidationObserver != null) {
 				dkvValidationModel.deleteObserver(dkvValidationObserver);
 			}
 			reply.add(report);
 		}
 
 		if (getDataModel(false) != null) {
 			DMValidationModel dmValidationModel = new DMValidationModel(getProject(), generationTarget);
 			if (dmValidationObserver != null) {
 				dmValidationModel.addObserver(dmValidationObserver);
 			}
 			ValidationReport report = getProject().getDataModel().validate(dmValidationModel);
 			if (dmValidationObserver != null) {
 				dmValidationModel.deleteObserver(dmValidationObserver);
 			}
 			reply.add(report);
 		}
 		return reply;
 	}
 
 	public static final String ONTOLOGY_URI = "http://www.agilebirds.com/projects";
 	public static final String RESOURCES = "resources";
 
 	public String getProjectVersionURI() {
 		return projectVersionURI;
 	}
 
 	public void setProjectVersionURI(String projectVersionURI) {
 		this.projectVersionURI = projectVersionURI;
 	}
 
 	public String getProjectURI() {
 		return getURI();
 	}
 
 	public void setProjectURI(String projectURI) {
 		this.projectURI = projectURI;
 	}
 
 	@Override
 	public String getURI() {
 		if (projectURI == null && !isDeserializing()) {
 			Date currentDate = new Date();
 			projectURI = ONTOLOGY_URI + "/" + (1900 + currentDate.getYear()) + "/" + (currentDate.getMonth() + 1) + "/" + getProjectName()
 					+ "_" + System.currentTimeMillis();
 			// projectURI= ONTOLOGY_URI+"/data/prj_"+getPrefix()+"_"+System.currentTimeMillis();
 		}
 		return projectURI;
 	}
 
 	@Override
 	public String toString() {
 		return "PROJECT-" + getName() + " ID=" + getID();
 	}
 
 	public static void cleanUpActionizer() {
 		// FlexoModelObject
 		FlexoModelObject.addFlexoPropertyActionizer = null;
 		FlexoModelObject.deleteFlexoPropertyActionizer = null;
 		FlexoModelObject.sortFlexoPropertiesActionizer = null;
 
 		// CGFile
 		CGFile.editCustomTemplateActionizer = null;
 		CGFile.redefineTemplateActionizer = null;
 		CGFile.showTemplateActionizer = null;
 
 		// FlexoProcess
 		FlexoProcess.addMetricsActionizer = null;
 		FlexoProcess.addStatusActionizer = null;
 		FlexoProcess.deleteActionizer = null;
 		FlexoProcess.deleteMetricsActionizer = null;
 
 		// FlexoWorkflow
 		FlexoWorkflow.addActivityMetricsDefinitionActionizer = null;
 		FlexoWorkflow.addArtefactMetricsDefinitionActionizer = null;
 		FlexoWorkflow.addEdgeMetricsDefinitionActionizer = null;
 		FlexoWorkflow.addOperationMetricsDefinitionActionizer = null;
 		FlexoWorkflow.addProcessMetricsDefinitionActionizer = null;
 		FlexoWorkflow.deleteMetricsDefinitionActionizer = null;
 
 		// Role
 		Role.addParentRoleActionizer = null;
 
 		// RoleList
 		RoleList.addRoleActionizer = null;
 		RoleList.deleteRoleActionizer = null;
 
 		// WKFArtefact
 		WKFArtefact.addMetricsActionizer = null;
 		WKFArtefact.deleteMetricsActionizer = null;
 
 		// FlexoPostCondition
 		FlexoPostCondition.addMetricsActionizer = null;
 		FlexoPostCondition.deleteMetricsActionizer = null;
 
 		// AbstractActivityNode
 		AbstractActivityNode.addAccountableRoleActionizer = null;
 		AbstractActivityNode.addConsultedRoleActionizer = null;
 		AbstractActivityNode.addInformedRoleActionizer = null;
 		AbstractActivityNode.addMetricsActionizer = null;
 		AbstractActivityNode.addResponsibleRoleActionizer = null;
 
 		AbstractActivityNode.deleteMetricsActionizer = null;
 
 		AbstractActivityNode.removeFromAccountableRoleActionizer = null;
 		AbstractActivityNode.removeFromConsultedRoleActionizer = null;
 		AbstractActivityNode.removeFromInformedRoleActionizer = null;
 		AbstractActivityNode.removeFromResponsibleRoleActionizer = null;
 
 		// OperationNode
 		OperationNode.addMetricsActionizer = null;
 		OperationNode.deleteMetricsActionizer = null;
 	}
 
 	public FlexoPamelaResource<ProjectData> getProjectDataResource() {
 		return getProjectDataResource(false);
 	}
 
 	public FlexoPamelaResource<ProjectData> getProjectDataResource(boolean createIfNotExist) {
 		FlexoPamelaResource<ProjectData> returned = (FlexoPamelaResource<ProjectData>) resourceForKey(ResourceType.PROJECT_DATA,
 				getProjectName());
 		if (returned == null && createIfNotExist) {
 			File xml = ProjectRestructuration.getExpectedProjectDataFile(this);
 			FlexoProjectFile dataFile = new FlexoProjectFile(xml, this);
 
 			try {
 				returned = new FlexoPamelaResource<ProjectData>(this, ResourceType.PROJECT_DATA, ProjectData.class, dataFile);
 				registerResource(returned);
 			} catch (Exception e1) {
 				// Warns about the exception
 				if (logger.isLoggable(Level.WARNING)) {
 					logger.warning("Exception raised: " + e1.getClass().getName() + ". See console for details.");
 				}
 				e1.printStackTrace();
 			}
 
 		}
 		return returned;
 	}
 
 	public ProjectData getProjectData() {
 		return getProjectData(false);
 	}
 
 	public ProjectData getProjectData(boolean createIfNotExist) {
 		FlexoPamelaResource<ProjectData> projectDataResource = getProjectDataResource(createIfNotExist);
 		if (projectDataResource != null) {
 			return projectDataResource.getResourceData();
 		} else {
 			return null;
 		}
 	}
 
 	public FlexoStorageResource<? extends ProjectOntology> getFlexoProjectOntologyResource() {
 		return getFlexoProjectOntologyResource(true);
 	}
 
 	private ProjectOntology createProjectOntology() {
 		// Temporary hack to select the type of the project ontology
 		// To be updated with model slots
 		return ProjectOWLOntology.createNewProjectOntology(this);
 		// return ProjectXSOntology.createNewProjectOntology(this);
 	}
 
 	@SuppressWarnings("unchecked")
 	public FlexoStorageResource<? extends ProjectOntology> getFlexoProjectOntologyResource(boolean createIfNotExist) {
 		FlexoStorageResource<ProjectOntology> returned = (FlexoStorageResource<ProjectOntology>) resourceForKey(
 				ResourceType.PROJECT_ONTOLOGY, getProjectName());
 		if (returned == null && createIfNotExist) {
 			return createProjectOntology().getFlexoResource();
 		}
 		return returned;
 	}
 
 	public ProjectOntology getProjectOntology() {
 		return getProjectOntology(true);
 	}
 
 	public ProjectOntology getProjectOntology(boolean createIfNotExist) {
 		FlexoStorageResource<? extends ProjectOntology> resource = getFlexoProjectOntologyResource(createIfNotExist);
 		if (resource == null) {
 			return null;
 		}
 		return resource.getResourceData();
 	}
 
 	private ProjectOntologyLibrary ontologyLibrary = null;
 
 	public ProjectOntologyLibrary getProjectOntologyLibrary() {
 		return getProjectOntologyLibrary(true);
 	}
 
 	public ProjectOntologyLibrary getProjectOntologyLibrary(boolean createIfNotExist) {
 		if (ontologyLibrary == null) {
 			if (createIfNotExist) {
 				logger.info("resource center: " + getResourceCenter());
 				ontologyLibrary = new ProjectOntologyLibrary(getResourceCenter().getOpenFlexoResourceCenter(), this);
 				// ontologyLibrary.getFlexoConceptOntology().loadWhenUnloaded();
 				// ontologyLibrary.init();
 			} else {
 				return null;
 			}
 		}
 		return ontologyLibrary;
 	}
 
 	/*
 	 * private CalcLibrary calcLibrary = null;
 	 * 
 	 * public CalcLibrary getCalcLibrary() { return getCalcLibrary(true); }
 	 * 
 	 * public CalcLibrary getCalcLibrary(boolean createIfNotExist) { if (calcLibrary == null) { if (createIfNotExist) calcLibrary = new
 	 * CalcLibrary(getOntologyLibrary()); else return null; } return calcLibrary; }
 	 */
 
 	public FlexoImportedProcessLibrary getImportedProcessLibrary() {
 		if (getFlexoWorkflow(false) == null) {
 			return null;
 		}
 		return getWorkflow().getImportedProcessLibrary();
 	}
 
 	public RoleList getImportedRoleList() {
 		return getWorkflow().getImportedRoleList();
 	}
 
 	public boolean getIsLocalized() {
 		return getDKVModel(false) != null && getDKVModel().getLanguages().size() > 1;
 	}
 
 	private Map<String, Map<Long, EditionPatternInstance>> _editionPatternInstances;
 
 	public EditionPatternInstance makeNewEditionPatternInstance(EditionPattern pattern) {
 		EditionPatternInstance returned = new EditionPatternInstance(pattern, this);
 		if (_editionPatternInstances == null) {
 			_editionPatternInstances = new Hashtable<String, Map<Long, EditionPatternInstance>>();
 		}
 		Map<Long, EditionPatternInstance> hash = _editionPatternInstances.get(pattern.getName());
 		if (hash == null) {
 			hash = new Hashtable<Long, EditionPatternInstance>();
 			_editionPatternInstances.put(pattern.getName(), hash);
 		}
 		hash.put(returned.getInstanceId(), returned);
 		return returned;
 	}
 
 	public EditionPatternInstance getEditionPatternInstance(EditionPatternReference reference) {
 		if (reference == null) {
 			return null;
 		}
 		if (reference.getEditionPattern() == null) {
 			logger.warning("Found a reference to a null EP, please investigate");
 			return null;
 		}
 		if (_editionPatternInstances == null) {
 			_editionPatternInstances = new Hashtable<String, Map<Long, EditionPatternInstance>>();
 		}
 		Map<Long, EditionPatternInstance> hash = _editionPatternInstances.get(reference.getEditionPattern().getName());
 		if (hash == null) {
 			hash = new Hashtable<Long, EditionPatternInstance>();
 			_editionPatternInstances.put(reference.getEditionPattern().getName(), hash);
 		}
 		EditionPatternInstance returned = hash.get(reference.getInstanceId());
 		if (returned == null) {
 			returned = new EditionPatternInstance(reference);
 			hash.put(reference.getInstanceId(), returned);
 		}
 		return returned;
 	}
 
 	public FlexoObjectIDManager getObjectIDManager() {
 		if (objectIDManager == null) {
 			objectIDManager = new FlexoObjectIDManager(this);
 		}
 		return objectIDManager;
 	}
 
 	public FlexoResourceCenterService getResourceCenter() {
 		if (resourceCenterService == null) {
 
 		}
 		// logger.info("return resourceCenter " + resourceCenter + " for project " + Integer.toHexString(hashCode()));
 		return resourceCenterService;
 	}
 
 	public void setResourceCenter(FlexoResourceCenterService resourceCenterService) {
 		if (resourceCenterService != null) {
 			if (resourceCenterService == this.resourceCenterService) {
 				logger.warning("Resource center is already set and the same as this new attempt. I will simply ignore the call.");
 				return;
 			}
 			logger.info(">>>>>>>>>>>>>>>>> setResourceCenter " + resourceCenterService + " for project " + Integer.toHexString(hashCode()));
 			if (this.resourceCenterService != null) {
 				logger.warning("Changing resource center on project " + getProjectName() + ". This is likely to cause problems.");
 			}
 			this.resourceCenterService = resourceCenterService;
 			EditionPatternConverter editionPatternConverter = new EditionPatternConverter(
 					resourceCenterService.getOpenFlexoResourceCenter());
 			getStringEncoder()._addConverter(editionPatternConverter);
 		} else {
 			getResourceCenter();
 			logger.warning("#@!#@!#@!#@! An attempt to set a null resource center was made. I will print a stacktrace to let you know where it came from but I am not setting the RC to null!\n"
 					+ "I will try to find one.");
 			new Exception("Attempt to set a null resource center on project " + getProjectName()).printStackTrace();
 		}
 	}
 
 	public boolean isComputeDiff() {
 		return computeDiff;
 	}
 
 	public void setComputeDiff(boolean computeDiff) {
 		this.computeDiff = computeDiff;
 	}
 
 	/**
 	 * This method is called while deserialising EditionPatternReference instances Because this storage is distributed, we have to build
 	 * partial knowledge, as resources are being loaded.
 	 * 
 	 * @param conceptURI
 	 * @param actorReference
 	 */
 	public void _addToPendingEditionPatternReferences(String conceptURI, ConceptActorReference actorReference) {
 		logger.fine("Registering concept " + conceptURI + " as pending pattern object reference: " + actorReference);
 		List<ConceptActorReference> values = pendingEditionPatternReferences.get(conceptURI);
 		if (values == null) {
 			values = new Vector<ConceptActorReference>();
 			pendingEditionPatternReferences.put(conceptURI, values);
 		}
 		values.add(actorReference);
 	}
 
 	private Map<String, List<ConceptActorReference>> pendingEditionPatternReferences = new Hashtable<String, List<ConceptActorReference>>();
 
 	public void _retrievePendingEditionPatternReferences(OntologyObject object) {
 		List<ConceptActorReference> values = pendingEditionPatternReferences.get(object.getURI());
 		if (values == null) {
 			// No pending EditionPattern references for object
 			return;
 		} else {
 			List<ConceptActorReference> clonedValues = new ArrayList<EditionPatternReference.ConceptActorReference>(values);
 			for (ConceptActorReference actorReference : clonedValues) {
 				EditionPatternInstance instance = actorReference.getPatternReference().getEditionPatternInstance();
 				if (instance == null) {
 					logger.warning("Found null EditionPatternInstance, please investigate");
 				} else if (actorReference.getPatternReference() == null) {
 					logger.warning("Found null actorReference.getPatternReference(), please investigate");
 				} else if (actorReference.getPatternReference().getEditionPattern() == null) {
 					logger.warning("Found null actorReference.getPatternReference().getEditionPattern(), please investigate");
 				} else {
 					PatternRole pr = actorReference.getPatternReference().getEditionPattern().getPatternRole(actorReference.patternRole);
 					logger.fine("Retrieve Edition Pattern Instance " + instance + " for " + object + " role=" + pr);
 					object.registerEditionPatternReference(instance, pr);
 				}
 			}
 			values.clear();
 		}
 	}
 
 	public void resolvePendingEditionPatternReferences() {
 		ArrayList<String> allKeys = new ArrayList<String>(pendingEditionPatternReferences.keySet());
 		for (String conceptURI : allKeys) {
 			OntologyObject oo = getProjectOntology().getOntologyObject(conceptURI);
 			if (oo != null) {
 				_retrievePendingEditionPatternReferences(oo);
 			}
 		}
 	}
 
 	public IModuleLoader getModuleLoader() {
 		return moduleLoader;
 	}
 
 	public void setModuleLoader(IModuleLoader moduleLoader) {
 		this.moduleLoader = moduleLoader;
 	}
 
 	public String getVersion() {
 		return version;
 	}
 
 	public void setVersion(String version) {
 		if (version != null) {
 			version = version.replace("~", "");
 		}
 		String old = this.version;
 		this.version = version;
 		setChanged();
 		notifyObservers(new DataModification(VERSION, old, version));
 	}
 
 	public long getRevision() {
 		return revision;
 	}
 
 	public void setRevision(long revision) {
 		long old = this.revision;
 		this.revision = revision;
 		setChanged();
 		notifyObservers(new DataModification(REVISION, old, revision));
 	}
 
 	public String canImportProject(FlexoProject project) {
 		if (getProjectData() == null) {
 			return null;
 		} else {
 			return getProjectData().canImportProject(project);
 		}
 	}
 
 	public boolean hasImportedProjects() {
 		return getProjectData() != null && getProjectData().getImportedProjects().size() > 0;
 	}
 
 	public List<FlexoModelObjectReference> getObjectReferences() {
 		return objectReferences;
 	}
 
 	public void addToObjectReferences(FlexoModelObjectReference<?> objectReference) {
 		objectReferences.add(objectReference);
 	}
 
 	public void removeObjectReferences(FlexoModelObjectReference<?> objectReference) {
 		objectReferences.remove(objectReference);
 	}
 
 }
