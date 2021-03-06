 /*
  * $Id$
  *
  * This file is part of McIDAS-V
  *
  * Copyright 2007-2009
  * Space Science and Engineering Center (SSEC)
  * University of Wisconsin - Madison
  * 1225 W. Dayton Street, Madison, WI 53706, USA
  * http://www.ssec.wisc.edu/mcidas
  * 
  * All Rights Reserved
  * 
  * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
  * some McIDAS-V source code is based on IDV and VisAD source code.  
  * 
  * McIDAS-V is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  * 
  * McIDAS-V is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser Public License
  * along with this program.  If not, see http://www.gnu.org/licenses.
  */
 
 package edu.wisc.ssec.mcidasv.control;
 
 import edu.wisc.ssec.mcidasv.data.Test2AddeImageDataSource;
 
 import edu.wisc.ssec.mcidasv.chooser.ImageParameters;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Container;
 import java.awt.Dimension;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.MouseEvent;
 
 import java.util.ArrayList;
 import java.util.Hashtable;
 import java.util.List;
 
 import javax.swing.*;
 import javax.swing.event.*;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 
 import ucar.unidata.data.DataChoice;
 import ucar.unidata.data.DataSelection;
 import ucar.unidata.data.DataSourceImpl;
 import ucar.unidata.data.GeoSelection;
 import ucar.unidata.data.imagery.AddeImageDescriptor;
 
 import ucar.unidata.idv.ControlContext;
 import ucar.unidata.idv.IdvResourceManager;
 
 import ucar.unidata.idv.control.ColorTableWidget;
 import ucar.unidata.idv.control.DisplayControlImpl;
 
 import ucar.unidata.ui.XmlTree;
 
 import ucar.unidata.util.ColorTable;
 import ucar.unidata.util.GuiUtils;
 import ucar.unidata.util.Range;
 
 import ucar.unidata.xml.XmlResourceCollection;
 import ucar.unidata.xml.XmlUtil;
 
 import visad.DateTime;
 import visad.FlatField;
 import visad.meteorology.ImageSequenceImpl;
 
 public class ImagePlanViewControl extends ucar.unidata.idv.control.ImagePlanViewControl {
 
     private static final String TAG_FOLDER = "folder";
     private static final String TAG_DEFAULT = "default";
     private static final String ATTR_NAME = "name";
     private static final String ATTR_SERVER = "server";
     private static final String ATTR_POS = "POS";
     private static final String ATTR_DAY = "DAY";
     private static final String ATTR_TIME = "TIME";
 
     /** save parameter set */
     private JFrame saveWindow;
 
     private static String newFolder;
 
     private XmlTree xmlTree;
 
     /** Command for connecting */
     protected static final String CMD_NEWFOLDER = "cmd.newfolder";
     protected static final String CMD_NEWPARASET = "cmd.newparaset";
 
     /** Install new folder fld */
     private JTextField folderFld;
 
     /** Holds the current save set tree */
     private JPanel treePanel;
 
     /** The user imagedefaults xml root */
     private static Element imageDefaultsRoot;
 
     /** The user imagedefaults xml document */
     private static Document imageDefaultsDocument;
 
     /** Holds the ADDE servers and groups*/
     private static XmlResourceCollection imageDefaults;
 
     private Node lastCat;
     private static Element lastClicked;
 
     private JButton newFolderBtn;
     private JButton newSetBtn;
 
     private String newCompName = "";
 
     /** Shows the status */
     private JLabel statusLabel;
 
     /** Status bar component */
     private JComponent statusComp;
 
     private JPanel contents;
 
     private DataSourceImpl dataSource;
 
     private DataChoice dataChoice;
 
     private FlatField image;
 
     private McIDASVHistogramWrapper histoWrapper;
 
     private DataSelection dataSelection;
 
     public ImagePlanViewControl() {
         super();
         this.imageDefaults = getImageDefaults();
     }
 
 
     /**
      * Get the xml resource collection that defines the image default xml
      *
      * @return Image defaults resources
      */
     protected XmlResourceCollection getImageDefaults() {
         XmlResourceCollection ret = null;
         try {
             ControlContext controlContext = getControlContext();
             if (controlContext != null) {
                 IdvResourceManager irm = controlContext.getResourceManager();
                 ret=irm.getXmlResources( IdvResourceManager.RSC_IMAGEDEFAULTS);
                 if (ret.hasWritableResource()) {
                     imageDefaultsDocument =
                         ret.getWritableDocument("<imagedefaults></imagedefaults>");
                     imageDefaultsRoot =
                         ret.getWritableRoot("<imagedefaults></imagedefaults>");
                 }
             }
         } catch (Exception e) {
             System.out.println("e=" + e);
         }
         return ret;
     }
 
 
     /**
      * Called by doMakeWindow in DisplayControlImpl, which then calls its
      * doMakeMainButtonPanel(), which makes more buttons.
      *
      * @return container of contents
      */
     public Container doMakeContents() {
         try {
             JTabbedPane tab = new MyTabbedPane();
             tab.add("Settings",
                     GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
             tab.add("Histogram", GuiUtils.inset(getHistogramTabComponent(),5));
             //Set this here so we don't get odd crud on the screen
             //When the MyTabbedPane goes to paint itself the first time it
             //will set the tab back to 0
             tab.setSelectedIndex(1);
             GuiUtils.handleHeavyWeightComponentsInTabs(tab);
             ColorTableWidget ctw = getColorTableWidget(getRange());
            ctw.doUseDefault();
             Range range = getRange();
             int lo = (int)range.getMin();
             int hi = (int)range.getMax();
             boolean flag = histoWrapper.modifyRange(lo, hi);
             ((MyTabbedPane)tab).setPopupFlag(!flag);
             histoWrapper.setHigh(hi);
             histoWrapper.setLow(lo);
             return tab;
         } catch (Exception exc) {
             logException("doMakeContents", exc);
         }
         return null;
     }
 
     protected JComponent getHistogramTabComponent() {
         List choices = new ArrayList();
         if (dataChoice == null) {
             dataChoice = getDataChoice();
         }
         choices.add(dataChoice);
         dataSource = getDataSource();
         Hashtable props = dataSource.getProperties();
         histoWrapper = new McIDASVHistogramWrapper("histo", choices, (DisplayControlImpl)this);
         try {
             this.dataSelection = dataChoice.getDataSelection();
             ImageSequenceImpl seq = null;
             if (dataSelection == null) {
                 image = (FlatField)dataSource.getData(dataChoice, null, props);
             } else {
                 GeoSelection gs = dataSelection.getGeoSelection();
                 seq = (ImageSequenceImpl) 
                     dataSource.getData(dataChoice, null, dataSelection, props);
             }
             if (seq != null) {
                 if (seq.getImageCount() > 0) 
                     image = (FlatField)seq.getImage(0);
             }
             histoWrapper.loadData(image);
             double lo = histoWrapper.getLow();
             double hi = histoWrapper.getHigh();
             contrastStretch(lo, hi);
         } catch (Exception e) {
             //System.out.println("Histo e=" + e);
         }
         JComponent histoComp = histoWrapper.doMakeContents();
         JButton resetButton = new JButton("Reset");
         resetButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 resetColorTable();
             }
         });
         JPanel resetPanel =
             GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(resetButton), 4));
         return GuiUtils.centerBottom(histoComp, resetPanel);
     }
 
     protected void contrastStretch(double low, double high) {
         ColorTable ct = getColorTable();
         if (ct != null) {
             Range range = new Range(low, high);
             try {
                 setRange(ct.getName(), range);
             } catch (Exception e) {
                 System.out.println("contrast stretch e=" + e);
             }
         }
     }
 
 
     public void resetColorTable() {
         try {
             histoWrapper.doReset();
         } catch (Exception e) {
             System.out.println("resetColorTable e=" + e);
         }
     }
 
     protected void getSaveMenuItems(List items, boolean forMenuBar) {
         super.getSaveMenuItems(items, forMenuBar);
         items.add(GuiUtils.makeMenuItem("Save Image Parameter Set", this,
             "popupSaveImageParameters"));
         items.add(GuiUtils.makeMenuItem("Save As Local Data Source", this,
             "saveDataToLocalDisk"));
     }
 
     public void saveDataToLocalDisk() {
         getDataSource().saveDataToLocalDisk();
     }
 
     public void popupSaveImageParameters() {
         if (saveWindow == null) {
             showSaveDialog();
             return;
         }
         saveWindow.setVisible(true);
         GuiUtils.toFront(saveWindow);
     }
 
     private void showSaveDialog() {
         if (saveWindow == null) {
             saveWindow = GuiUtils.createFrame("Save Image Parameter Set");
         }
         if (statusComp == null) {
             statusLabel = new JLabel();
             statusComp = GuiUtils.inset(statusLabel, 2);
             statusComp.setBackground(new Color(255, 255, 204));
             statusLabel.setOpaque(true); 
             statusLabel.setBackground(new Color(255, 255, 204));
         }
         JPanel statusPanel = GuiUtils.inset(GuiUtils.top( GuiUtils.vbox(new JLabel(" "),
                 GuiUtils.hbox(GuiUtils.rLabel("Status: "),statusComp),
                 new JLabel(" "))), 6);
         JPanel sPanel = GuiUtils.topCenter(statusPanel,
                GuiUtils.filler());
 
         List newComps = new ArrayList();
         final JTextField newName = new JTextField(20);
         newName.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 setStatus("Click New Folder or New ParameterSet button");
                 newCompName = newName.getText().trim();
             }
         });
         newComps.add(newName);
         newComps.add(GuiUtils.filler());
         newFolderBtn = new JButton("New Folder");
         newFolderBtn.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 newFolder = newName.getText().trim();
                 if (newFolder.equals("")) {
                     newComponentError("folder");
                     return;
                 }
                 Element exists = XmlUtil.findElement(imageDefaultsRoot, "folder", ATTR_NAME, newFolder);
                 if (!(exists == null)) {
                     if (!GuiUtils.askYesNo("Verify Replace Folder",
                         "Do you want to replace the folder " +
                         "\"" + newFolder + "\"?" +
                         "\nNOTE: All parameter sets it contains will be deleted.")) return;
                     imageDefaultsRoot.removeChild(exists);
                 }
                 newName.setText("");
                 Node newEle = makeNewFolder();
                 makeXmlTree();
                 xmlTree.selectElement((Element)newEle);
                 lastCat = newEle;
                 lastClicked = null;
                 newSetBtn.setEnabled(true);
                 setStatus("Please enter a name for the new parameter set");
             }
         });
         newComps.add(newFolderBtn);
         newComps.add(GuiUtils.filler());
         newName.setEnabled(true);
         newFolderBtn.setEnabled(true);
         newSetBtn = new JButton("New Parameter Set");
         newSetBtn.setActionCommand(CMD_NEWPARASET);
         newSetBtn.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 newCompName = newName.getText().trim();
                 if (newCompName.equals("")) {
                     newComponentError("parameter set");
                     return;
                 }
                 newName.setText("");
                 Element newEle = saveParameterSet();
                 if (newEle == null) return;
                 xmlTree.selectElement(newEle);
                 lastClicked = newEle;
             }
         });
         newComps.add(newSetBtn);
         newSetBtn.setEnabled(false);
 
         JPanel newPanel = GuiUtils.top(GuiUtils.left(GuiUtils.hbox(newComps)));
         JPanel topPanel = GuiUtils.topCenter(sPanel, newPanel);
 
         treePanel = new JPanel();
         treePanel.setLayout(new BorderLayout());
         makeXmlTree();
         ActionListener listener = new ActionListener() {
             public void actionPerformed(ActionEvent event) {
                 String cmd = event.getActionCommand();
                 if (cmd.equals(GuiUtils.CMD_CANCEL)) {
                     if (lastClicked != null) {
                         removeNode(lastClicked);
                         lastClicked = null;
                     }
                     saveWindow.setVisible(false);
                     saveWindow = null;
                 } else {
                     saveWindow.setVisible(false);
                     saveWindow = null;
                 }
             }
         };
         JPanel bottom =
             GuiUtils.inset(GuiUtils.makeApplyCancelButtons(listener), 5);
         contents = 
             GuiUtils.topCenterBottom(topPanel, treePanel, bottom);
 
         saveWindow.getContentPane().add(contents);
         saveWindow.pack();
         saveWindow.setLocation(200, 200);
 
         saveWindow.setVisible(true);
         GuiUtils.toFront(saveWindow);
         setStatus("Please select a folder from tree, or create a new folder");
     }
 
     private void newComponentError(String comp) {
         JLabel label = new JLabel("Please enter " + comp +" name");
         JPanel contents = GuiUtils.top(GuiUtils.inset(label, 24));
         GuiUtils.showOkCancelDialog(null, "Make Component Error", contents, null);
     }
 
     private void setStatus(String msg) {
         statusLabel.setText(msg);
         contents.paintImmediately(0,0,contents.getWidth(),
                                         contents.getHeight());
     }
 
     private void removeNode(Element node) {
         if (imageDefaults == null)
             imageDefaults = getImageDefaults();
         Node parent = node.getParentNode();
         parent.removeChild(node);
         makeXmlTree();
         try {
             imageDefaults.writeWritable();
         } catch (Exception e) {
             System.out.println("write error e=" + e);
         }
         imageDefaults.setWritableDocument(imageDefaultsDocument,
             imageDefaultsRoot);
     }
 
 
     private Node makeNewFolder() {
         if (imageDefaults == null)
             imageDefaults = getImageDefaults();
         if (newFolder.equals("")) return null;
         List newChild = new ArrayList();
         Node newEle = imageDefaultsDocument.createElement(TAG_FOLDER);
         lastCat = newEle;
         String[] newAttrs = { ATTR_NAME, newFolder };
         XmlUtil.setAttributes((Element)newEle, newAttrs);
         newChild.add(newEle);
         XmlUtil.addChildren(imageDefaultsRoot, newChild);
         try {
             imageDefaults.writeWritable();
         } catch (Exception e) {
             System.out.println("write error e=" + e);
         }
         imageDefaults.setWritableDocument(imageDefaultsDocument,
             imageDefaultsRoot);
         return newEle;
     }
 
 
     /**
      * Just creates an empty XmlTree
      */
     private void makeXmlTree() {
         if (imageDefaults == null)
             imageDefaults = getImageDefaults();
         xmlTree = new XmlTree(imageDefaultsRoot, true, "") {
             public void doClick(XmlTree theTree, XmlTree.XmlTreeNode node,
                                 Element element) {
                 Element clicked = xmlTree.getSelectedElement();
                 String lastTag = clicked.getTagName();
                 if (lastTag.equals("folder")) {
                     lastCat = clicked;
                     lastClicked = null;
                     setStatus("Please enter a name for the new parameter set");
                     newSetBtn.setEnabled(true);
                 } else {
                     lastCat = clicked.getParentNode();
                     lastClicked = clicked;
                 }
             }
 
             public void doRightClick(XmlTree theTree,
                                      XmlTree.XmlTreeNode node,
                                      Element element, MouseEvent event) {
                 JPopupMenu popup = new JPopupMenu();
                 if (makePopupMenu(theTree, element, popup)) {
                     popup.show((Component) event.getSource(), event.getX(),
                                event.getY());
                 }
             }
         };
         List tagList = new ArrayList();
         tagList.add(TAG_FOLDER);
         tagList.add(TAG_DEFAULT);
         xmlTree.addTagsToProcess(tagList);
         xmlTree.defineLabelAttr(TAG_FOLDER, ATTR_NAME);
         addToContents(GuiUtils.inset(GuiUtils.topCenter(new JPanel(),
                 xmlTree.getScroller()), 5));
         return;
     }
 
     private List getFolders() {
         return XmlUtil.findChildren(imageDefaultsRoot, TAG_FOLDER);
     }
 
 
     private void doDeleteRequest(Node node) {
         if (node == null) return;
         Element ele = (Element)node;
         String tagName = ele.getTagName();
         if (tagName.equals("folder")) {
             if (!GuiUtils.askYesNo("Verify Delete Folder",
                 "Do you want to delete the folder " +
                 "\"" + ele.getAttribute("name") + "\"?" +
                 "\nNOTE: All parameter sets it contains will be deleted.")) return;
             XmlUtil.removeChildren(ele);
         } else if (tagName.equals("default")) {
             if (!GuiUtils.askYesNo("Verify Delete", "Do you want to delete " +
                 "\"" + ele.getAttribute(ATTR_NAME) + "\"?")) return;
         } else { return; }
         removeNode(ele);
     }
     /**
      *  Create and popup a command menu for when the user has clicked on the given xml node.
      *
      *  @param theTree The XmlTree object displaying the current xml document.
      *  @param node The xml node the user clicked on.
      *  @param popup The popup menu to put the menu items in.
      * @return Did we add any items into the menu
      */
     private boolean makePopupMenu(final XmlTree theTree, final Element node,
                                   JPopupMenu popup) {
         theTree.selectElement(node);
         String    tagName = node.getTagName();
         final Element parent = (Element)node.getParentNode();
         boolean   didone  = false;
         JMenuItem mi;
 
         if (tagName.equals("default")) {
             lastClicked = node;
             JMenu moveMenu = new JMenu("Move to");
             List folders = getFolders();
             for (int i=0; i<folders.size(); i++) {
                 final Element newFolder = (Element)folders.get(i);
                 if (!newFolder.isSameNode(parent)) {
                     String name = newFolder.getAttribute(ATTR_NAME);
                     mi = new JMenuItem(name);
                     mi.addActionListener(new ActionListener() {
                         public void actionPerformed(ActionEvent ae) {
                             moveParameterSet(parent, newFolder);
                         }
                     });
                     moveMenu.add(mi);
                 }
             }
             popup.add(moveMenu);
             popup.addSeparator();
             didone = true;
         }
 
         mi = new JMenuItem("Rename...");
         mi.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 doRename(node);
             }
         });
         popup.add(mi);
         didone = true;
 
         mi = new JMenuItem("Delete");
         mi.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 doDeleteRequest(node);
             }
         });
         popup.add(mi);
         didone = true;
 
         return didone;
     }
 
     public void moveParameterSet(Element parent, Element newFolder) {
         if (imageDefaults == null)
             imageDefaults = getImageDefaults();
         if (lastClicked == null) return;
         Node copyNode = lastClicked.cloneNode(true);
         newFolder.appendChild(copyNode);
         parent.removeChild(lastClicked);
         lastCat = newFolder;
         makeXmlTree();
         try {
             imageDefaults.writeWritable();
         } catch (Exception e) {
             System.out.println("write error e=" + e);
         }
         imageDefaults.setWritableDocument(imageDefaultsDocument,
             imageDefaultsRoot);
     }
 
     private void doRename(Element node) {
         if (imageDefaults == null)
             imageDefaults = getImageDefaults();
         if (!node.hasAttribute(ATTR_NAME)) return;
         JLabel label = new JLabel("New name: ");
         JTextField nameFld = new JTextField("", 20);
         JComponent contents = GuiUtils.doLayout(new Component[] {
             GuiUtils.rLabel("New name: "), nameFld, }, 2,
             GuiUtils.WT_N, GuiUtils.WT_N);
         contents = GuiUtils.center(contents);
         contents = GuiUtils.inset(contents, 10);
         if (!GuiUtils.showOkCancelDialog(null, "Rename \"" +
                node.getAttribute("name") + "\"", contents, null)) return;
         String newName = nameFld.getText().trim();
         String tagName = node.getTagName();
         Element root = imageDefaultsRoot;
         if (tagName.equals("default"))
             root = (Element)node.getParentNode();
         Element exists = XmlUtil.findElement(root, tagName, ATTR_NAME, newName);
         if (!(exists == null)) {
            if (!GuiUtils.askYesNo("Name Already Exists",
                "Do you want to replace " + node.getAttribute("name") + " with" +
                "\"" + newName + "\"?")) return;
         }
         node.removeAttribute(ATTR_NAME);
 	node.setAttribute(ATTR_NAME, newName);
 	makeXmlTree();
 	try {
 	    imageDefaults.writeWritable();
 	} catch (Exception e) {
 	    System.out.println("write error e=" + e);
 	}
 	imageDefaults.setWritableDocument(imageDefaultsDocument,
 	    imageDefaultsRoot);
     }
 
     /**
      *  Remove the currently display gui and insert the given one.
      *
      *  @param comp The new gui.
      */
     private void addToContents(JComponent comp) {
 	treePanel.removeAll();
 	comp.setPreferredSize(new Dimension(200, 300));
 	treePanel.add(comp, BorderLayout.CENTER);
 	if (contents != null) {
 	    contents.invalidate();
 	    contents.validate();
 	    contents.repaint();
 	}
     }
 
     private DataSourceImpl getDataSource() {
 	DataSourceImpl ds = null;
 	List dataSources = getDataSources();
         Object dc = dataSources.get(0);
         ds = (DataSourceImpl)dc;
 	return ds;
     }
 
     public Element saveParameterSet() {
 	if (imageDefaults == null)
 	    imageDefaults = getImageDefaults();
 	if (newCompName.equals("")) {
 	    newComponentError("parameter set");
 	    return null;
 	}
 	Element newChild = imageDefaultsDocument.createElement(TAG_DEFAULT);
 	newChild.setAttribute(ATTR_NAME, newCompName);
 
         dataChoice = getDataChoice();
         dataSource = getDataSource();
 	if (!(dataSource.getClass().isInstance(new Test2AddeImageDataSource())))
             return newChild;
         Test2AddeImageDataSource testDataSource = (Test2AddeImageDataSource)dataSource;
         List imageList = testDataSource.getDescriptors(dataChoice, this.dataSelection);
         int numImages = imageList.size();
         List dateTimes = new ArrayList();
         DateTime thisDT = null;
         if (!(imageList == null)) {
             AddeImageDescriptor aid = null;
             for (int imageNo=0; imageNo<numImages; imageNo++) {
                 aid = (AddeImageDescriptor)(imageList.get(imageNo));
                 thisDT = aid.getImageTime();
                 if (!(dateTimes.contains(thisDT))) {
                     if (thisDT != null) dateTimes.add(thisDT);
                 }
             }
             String dateS = "";
             String timeS = "";
             if (!(dateTimes.isEmpty())) {
                 thisDT = (DateTime)dateTimes.get(0);
                 dateS = thisDT.dateString();
                 timeS = thisDT.timeString();
                 if (dateTimes.size() > 1) {
                     for (int img=1; img<dateTimes.size(); img++) {
                         thisDT = (DateTime)dateTimes.get(img);
                         String str = "," + thisDT.dateString();
                         String newString = new String(dateS + str);
                         dateS = newString;
                         str = "," + thisDT.timeString();
                         newString = new String(timeS + str);
                         timeS = newString;
                     }
                  }
              }
              if (aid != null) {
                 String displayUrl = testDataSource.getDisplaySource();
                 ImageParameters ip = new ImageParameters(displayUrl);
                 List props = ip.getProperties();
                 List vals = ip.getValues();
                 String server = ip.getServer();
                 newChild.setAttribute(ATTR_SERVER, server);
                 int num = props.size();
                 if (num > 0) {
                     String attr = "";
                     String val = "";
                     for (int i=0; i<num; i++) {
                         attr = (String)(props.get(i));
                         if (attr.equals(ATTR_POS)) {
                             val = new Integer(numImages - 1).toString();
                         } else if (attr.equals(ATTR_DAY)) {
                             val = dateS;
                         } else if (attr.equals(ATTR_TIME)) {
                             val = timeS;
                         } else {
                             val = (String)(vals.get(i));
                         }
                         newChild.setAttribute(attr, val);
                     }
                 }
             }
         }
         Element parent = xmlTree.getSelectedElement();
         if (parent == null) parent = (Element)lastCat;
         if (parent != null) {
             Element exists = XmlUtil.findElement(parent, "default", ATTR_NAME, newCompName);
             if (!(exists == null)) {
                 JLabel label = new JLabel("Replace \"" + newCompName + "\"?");
                 JPanel contents = GuiUtils.top(GuiUtils.inset(label, newCompName.length()+12));
                 if (!GuiUtils.showOkCancelDialog(null, "Parameter Set Exists", contents, null))
                     return newChild;
                 parent.removeChild(exists);
             }
             parent.appendChild(newChild);
             makeXmlTree();
         }
         try {
             imageDefaults.writeWritable();
         } catch (Exception e) {
             System.out.println("write error e=" + e);
         }
         imageDefaults.setWritableDocument(imageDefaultsDocument,
             imageDefaultsRoot);
         return newChild;
     }
 
 
     /**
      * Class MyTabbedPane handles the visad component in a tab
      *
      *
      * @author IDV Development Team
      * @version $Revision$
      */
     private class MyTabbedPane extends JTabbedPane implements ChangeListener {
         /** Have we been painted */
         boolean painted = false;
         boolean popupFlag = false;
         /**
          * ctor
          */
         public MyTabbedPane() {
             addChangeListener(this);
         }
         /**
          *
          * Handle when the tab has changed. When we move to tab 1 then hide the heavy
          * component. Show it on change to tab 0.
          *
          * @param e The event
          */
         public void stateChanged(ChangeEvent e) {
             if ( !getActive() || !getHaveInitialized()) {
                 return;
             }
             if ((getSelectedIndex() == 1) && popupFlag) {
                 JLabel label = new JLabel("Can't make a histogram");
                 JPanel contents = GuiUtils.top(GuiUtils.inset(label, label.getText().length() + 12));
                 GuiUtils.showOkDialog(null, "Data Unavailable", contents, null);
                 setPopupFlag(false);
             }
         }
 
         private void setPopupFlag(boolean flag) {
             this.popupFlag = flag;
         }
 
         /**
          * The first time we paint toggle the selected index. This seems to get rid of
          * screen crud
          *
          * @param g graphics
          */
         public void paint(java.awt.Graphics g) {
             if ( !painted) {
                 painted = true;
                 setSelectedIndex(1);
                 setSelectedIndex(0);
                 repaint();
             }
             super.paint(g);
         }
     }
 }
