 //
 // DataManager.java
 //
 
 /*
 VisBio application for visualization of multidimensional
 biological image data. Copyright (C) 2002-2004 Curtis Rueden.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
 package loci.visbio.data;
 
 import java.awt.Component;
 import java.awt.event.KeyEvent;
 import java.io.*;
 import java.net.URL;
 import java.util.Vector;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipInputStream;
 import javax.swing.tree.DefaultMutableTreeNode;
 import loci.visbio.*;
 import loci.visbio.help.HelpManager;
 import loci.visbio.state.SaveException;
 import loci.visbio.state.StateManager;
 import loci.visbio.util.SwingUtil;
 import loci.visbio.util.XMLUtil;
 import org.w3c.dom.Element;
 
 /** DataManager is the manager encapsulating VisBio's data transform logic. */
 public class DataManager extends LogicManager {
 
   // -- Constants --
 
   /** URL prefix for sample datasets. */
   protected static final String SAMPLE_PREFIX =
     "ftp://ftp.loci.wisc.edu/locisoftware/visbio/data/";
 
 
   // -- Control panel --
 
   /** Datasets control panel. */
   protected DataControls dataControls;
 
 
   // -- Other fields --
 
   /** List of registered data transform type classes. */
   protected Vector transformTypes;
 
   /** List of registered data transform type labels. */
   protected Vector transformLabels;
 
 
   // -- Constructor --
 
   /** Constructs a dataset manager. */
   public DataManager(VisBioFrame bio) { super(bio); }
 
 
   // -- DataManager API methods --
 
   /** Adds a data object to the list. */
   public void addData(DataTransform data) {
     dataControls.addData(data);
     bio.generateEvent(this, "add data", true);
   }
 
   /** Removes a data object from the list. */
  public void removeData(DataTransform data) {
    dataControls.removeData(data);
    bio.generateEvent(this, "remove data", true);
   }
 
   /** Gets the root node of the data object tree. */
   public DefaultMutableTreeNode getDataRoot() {
     return dataControls.getDataRoot();
   }
 
   /** Gets the currently selected data object. */
   public DataTransform getSelectedData() {
     return dataControls.getSelectedData();
   }
 
   /** Shows dialog containing controls for the given data object. */
   public void showControls(DataTransform data) {
     dataControls.showControls(data);
   }
 
   /**
    * Registers the given subclass of DataTransform with the data manager,
    * using the given label as a description.
    */
   public void registerDataType(Class c, String label) {
     transformTypes.add(c);
     transformLabels.add(label);
   }
 
   /** Gets list of registered data transform types. */
   public Class[] getRegisteredDataTypes() {
     Class[] types = new Class[transformTypes.size()];
     transformTypes.copyInto(types);
     return types;
   }
 
   /** Gets list of regitered data transform labels. */
   public String[] getRegisteredDataLabels() {
     String[] labels = new String[transformLabels.size()];
     transformLabels.copyInto(labels);
     return labels;
   }
 
   /** Gets data control panel. */
   public DataControls getControlPanel() { return dataControls; }
 
   /** Gets a list of data transforms present in the tree. */
   public Vector getDataList() {
     Vector v = new Vector();
     buildDataList(dataControls.getDataRoot(), v);
     return v;
   }
 
   /** Gets the data transform with the associated ID. */
   public DataTransform getDataById(int id) {
     return getDataById(dataControls.getDataRoot(), id);
   }
 
   /** Imports a dataset. */
   public void importData() { importData(bio); }
 
   /** Imports a dataset, using the given parent component for user dialogs. */
   public void importData(Component parent) {
     DataTransform dt = Dataset.makeTransform(this, null, parent);
     if (dt != null) addData(dt);
   }
 
   /** Exports the selected data object to disk. */
   public void exportData() {
     DataTransform data = dataControls.getSelectedData();
     if (data instanceof ImageTransform) exportData((ImageTransform) data);
   }
 
   /** Exports the given data object to disk. */
   public void exportData(ImageTransform data) {
     dataControls.exportData(data);
   }
 
   /**
    * Loads the given sample dataset. If one with the given name already exists
    * in the samples cache, it is used. Otherwise, it is downloaded from the
    * VisBio site and stored in the cache first.
    */
   public void openSampleData(String name) {
     final String dirName = name;
     final String zipName = name + ".zip";
     final String location = SAMPLE_PREFIX + zipName;
 
     // prepare sample dataset in a separate thread
     new Thread(new Runnable() {
       public void run() {
         // create samples folder if it does not already exist
         File samplesDir = new File("samples");
         if (!samplesDir.exists()) samplesDir.mkdir();
 
         // create samples subdirectory and download data if not already cached
         File dir = new File(samplesDir, dirName);
         if (!dir.exists()) {
           dir.mkdir();
           try {
             bio.setStatus("Downloading " + zipName + "...");
             URL url = new URL(location);
             ZipInputStream in = new ZipInputStream(url.openStream());
             byte[] buf = new byte[8192];
             while (true) {
               ZipEntry entry = in.getNextEntry();
               if (entry == null) break; // eof
               String entryName = entry.getName();
               bio.setStatus("Extracting " + entryName + "...");
               FileOutputStream out = new FileOutputStream(
                 new File(dir, entryName));
               while (true) {
                 int r = in.read(buf);
                 if (r == -1) break; // end of entry
                 out.write(buf, 0, r);
               }
               out.close();
               in.closeEntry();
             }
             in.close();
           }
           catch (IOException exc) {
             System.err.println("Cannot download sample data from " + location);
             exc.printStackTrace();
           }
         }
 
         // create dataset object
         bio.setStatus("Organizing data...");
         File[] files = dir.listFiles();
         String pattern = null;
         for (int i=0; i<files.length; i++) {
           if (files[i].getName().endsWith(".visbio")) continue;
           pattern = FilePattern.findPattern(files[i]);
           if (pattern == null) pattern = files[i].getAbsolutePath();
         }
         if (pattern == null) {
           System.err.println("Error: no files for sample dataset " + dirName);
           return;
         }
 
         FilePattern fp = new FilePattern(pattern);
         int[] lengths = fp.getCount();
 
         // assume first dimension is Time, last is Slice, and others are Other
         int len = lengths.length;
         String[] dims = new String[len + 1];
         dims[0] = "Time";
         for (int i=1; i<len; i++) dims[i] = "Other";
         dims[len] = "Slice";
 
         bio.setStatus("Creating " + dirName + " dataset...");
         addData(new Dataset(dirName, pattern, fp.getFiles(), lengths, dims));
       }
     }).start();
   }
 
 
   // -- LogicManager API methods --
 
   /** Called to notify the logic manager of a VisBio event. */
   public void doEvent(VisBioEvent evt) {
     int eventType = evt.getEventType();
     if (eventType == VisBioEvent.LOGIC_ADDED) {
       LogicManager lm = (LogicManager) evt.getSource();
       if (lm == this) doGUI();
     }
   }
 
   /** Gets the number of tasks required to initialize this logic manager. */
   public int getTasks() { return 4; }
 
 
   // -- Saveable API methods --
 
   /** Writes the current state to the given DOM element ("VisBio"). */
   public void saveState(Element el) throws SaveException {
     Vector v = getDataList();
     int len = v.size();
     Element child = XMLUtil.createChild(el, "DataTransforms");
     for (int i=0; i<len; i++) {
       DataTransform data = (DataTransform) v.elementAt(i);
       data.saveState(child);
     }
   }
 
   /** Restores the current state from the given DOM element ("VisBio"). */
   public void restoreState(Element el) throws SaveException {
     Element child = XMLUtil.getFirstChild(el, "DataTransforms");
     Element[] els = XMLUtil.getChildren(child, null);
     Vector vn = new Vector();
     for (int i=0; i<els.length; i++) {
       // read transform class name
       String className = els[i].getAttribute("class");
       if (className == null) {
         System.err.println("Failed to read transform #" + i + " class");
         continue;
       }
 
       // locate transform class
       Class c = null;
       try { c = Class.forName(className); }
       catch (ClassNotFoundException exc) { }
       if (c == null) {
         System.err.println("Failed to identify transform #" + i +
           " class: " + className);
         continue;
       }
 
       // construct transform
       Object o = null;
       try { o = c.newInstance(); }
       catch (IllegalAccessException exc) { }
       catch (InstantiationException exc) { }
       if (o == null) {
         System.err.println("Failed to instantiate transform #" + i);
         continue;
       }
       if (!(o instanceof DataTransform)) {
         System.err.println("Transform #" + i + " is not valid (" +
           o.getClass().getName() + ")");
         continue;
       }
 
       // restore transform state
       DataTransform data = (DataTransform) o;
       data.restoreState(els[i]);
       vn.add(data);
     }
 
     // restore parent transform references
     int nlen = vn.size();
     for (int i=0; i<nlen; i++) {
       DataTransform data = (DataTransform) vn.elementAt(i);
       String parentId = els[i].getAttribute("parent");
       if (parentId == null || parentId.equals("")) data.parent = null;
       else {
         int pid = -1;
         try { pid = Integer.parseInt(parentId); }
         catch (NumberFormatException exc) { exc.printStackTrace(); }
         // search for transform with matching ID
         for (int j=0; j<nlen; j++) {
           DataTransform dt = (DataTransform) vn.elementAt(j);
           int id = dt.getTransformId();
           if (dt.getTransformId() == pid) data.parent = dt;
         }
         if (data.parent == null) {
           System.err.println("Invalid parent id (" +
             parentId + ") for transform #" + i);
         }
       }
     }
 
     // merge old and new transform lists
     Vector vo = getDataList();
     StateManager.mergeStates(vo, vn);
 
     // add new transforms to tree structure
     for (int i=0; i<nlen; i++) {
       DataTransform data = (DataTransform) vn.elementAt(i);
       if (!vo.contains(data)) addData(data);
     }
 
     // purge old transforms from tree structure
     int olen = vo.size();
     for (int i=0; i<olen; i++) {
       DataTransform data = (DataTransform) vo.elementAt(i);
       if (!vn.contains(data)) removeData(data);
     }
   }
 
 
   // -- Helper methods --
 
   /** Adds data-related GUI components to VisBio. */
   private void doGUI() {
     // control panel
     bio.setSplashStatus("Initializing data logic");
     dataControls = new DataControls(this);
     PanelManager pm = (PanelManager) bio.getManager(PanelManager.class);
     pm.addPanel(dataControls);
 
     // data transform registration
     bio.setSplashStatus(null);
     transformTypes = new Vector();
     transformLabels = new Vector();
     registerDataType(Dataset.class, "Dataset");
     registerDataType(DataSampling.class, "Subsampling");
     registerDataType(ProjectionTransform.class,
       "Maximum intensity projection");
     registerDataType(CollapseTransform.class, "Dimensional collapse");
     registerDataType(SpectralTransform.class, "Spectral mapping");
     registerDataType(ArbitrarySlice.class, "Arbitrary slice");
 
     // menu items
     bio.setSplashStatus(null);
     bio.addMenuItem("File", "Import data...",
       "loci.visbio.data.DataManager.importData", 'i');
     SwingUtil.setMenuShortcut(bio, "File", "Import data...", KeyEvent.VK_O);
     bio.addMenuItem("File", "Export data...",
       "loci.visbio.data.DataManager.exportData", 'e').setEnabled(false);
     bio.addSubMenu("File", "Sample datasets", 'd');
     bio.addMenuItem("Sample datasets", "sdub",
       "loci.visbio.data.DataManager.openSampleData(sdub)", 's');
     bio.addMenuItem("Sample datasets", "TAABA",
       "loci.visbio.data.DataManager.openSampleData(TAABA)", 't');
 
     // help window
     bio.setSplashStatus(null);
     HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
     String s = "File formats";
     hm.addHelpTopic(s, "formats.html");
     hm.addHelpTopic(s + "/Bio-Rad PIC", "biorad.html");
     hm.addHelpTopic(s + "/Olympus Fluoview TIFF", "fluoview.html");
     hm.addHelpTopic(s + "/Metamorph STK", "metamorph.html");
     hm.addHelpTopic(s + "/Openlab LIFF", "openlab.html");
     hm.addHelpTopic(s + "/QuickTime movies", "quicktime.html");
     hm.addHelpTopic(s + "/TIFF stacks", "tiff.html");
     hm.addHelpTopic(s + "/Zeiss LSM", "zeiss_lsm.html");
     hm.addHelpTopic(s + "/Zeiss ZVI (Axiovision)", "zeiss_zvi.html");
     hm.addHelpTopic(s + "/Other formats (JPEG, BMP, etc.)",
       "other_formats.html");
     s = "Data transforms";
     hm.addHelpTopic(s, "data_transforms.html");
     hm.addHelpTopic(s + "/Datasets", "dataset.html");
     hm.addHelpTopic(s + "/Subsamplings", "subsampling.html");
     hm.addHelpTopic(s + "/Maximum intensity projections",
       "max_intensity.html");
     hm.addHelpTopic(s + "/Dimensional collapse transforms", "collapse.html");
     hm.addHelpTopic(s + "/Spectral mappings", "spectral.html");
     hm.addHelpTopic(s + "/Arbitrary slices", "arbitrary_slice.html");
     s = "Control panels/Data panel";
     hm.addHelpTopic(s, "data_panel.html");
     hm.addHelpTopic(s + "/Importing a dataset from disk", "import_data.html");
     hm.addHelpTopic(s + "/Adding a data object", "add_data.html");
     hm.addHelpTopic(s + "/Generating thumbnails", "thumbnails.html");
     hm.addHelpTopic(s + "/Displaying a data object", "display_data.html");
     hm.addHelpTopic(s + "/Exporting data to disk", "export_data.html");
     hm.addHelpTopic(s + "/Uploading to an OME database", "upload_ome.html");
   }
 
   /** Recursively creates a list of data transforms below the given node. */
   private void buildDataList(DefaultMutableTreeNode node, Vector v) {
     Object o = node.getUserObject();
     if (o instanceof DataTransform) v.add(o);
 
     int count = node.getChildCount();
     for (int i=0; i<count; i++) {
       DefaultMutableTreeNode child = (DefaultMutableTreeNode)
         node.getChildAt(i);
       buildDataList(child, v);
     }
   }
 
   /** Recursively searches data tree for a transform with the given ID. */
   private DataTransform getDataById(DefaultMutableTreeNode node, int id) {
     Object o = node.getUserObject();
     if (o instanceof DataTransform) {
       DataTransform data = (DataTransform) o;
       if (data.getTransformId() == id) return data;
     }
     int count = node.getChildCount();
     for (int i=0; i<count; i++) {
       DefaultMutableTreeNode child = (DefaultMutableTreeNode)
         node.getChildAt(i);
       DataTransform data = getDataById(child, id);
       if (data != null) return data;
     }
     return null;
   }
 
 }
