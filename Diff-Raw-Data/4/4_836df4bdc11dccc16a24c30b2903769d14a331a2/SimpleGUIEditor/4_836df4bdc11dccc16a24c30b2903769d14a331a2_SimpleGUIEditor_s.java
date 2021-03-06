 /**
  * SimpleGUI.java
  *
  * @author Created by Omnicore CodeGuide
  */
 
 package edu.sc.seis.sod.editor;
 import java.io.*;
 import javax.swing.*;
 import org.w3c.dom.*;
 
 import edu.sc.seis.fissuresUtil.exceptionHandler.FilterReporter;
 import edu.sc.seis.fissuresUtil.exceptionHandler.GUIReporter;
 import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
 import edu.sc.seis.fissuresUtil.xml.Writer;
 import java.awt.BorderLayout;
 import java.awt.Dimension;
 import java.awt.FileDialog;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Properties;
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.transform.TransformerException;
 import org.apache.log4j.BasicConfigurator;
 import org.apache.log4j.Logger;
 import org.omg.CORBA.COMM_FAILURE;
 import org.xml.sax.SAXException;
 
 
 
 public class SimpleGUIEditor extends CommandLineEditor {
 
     static {
         List ignoreList = new ArrayList();
         // silently eat CommFailure
         ignoreList.add(COMM_FAILURE.class);
         GlobalExceptionHandler.add(new FilterReporter(new GUIReporter(), ignoreList));
         GlobalExceptionHandler.registerWithAWTThread();
     }
 
     public SimpleGUIEditor(String[] args) throws TransformerException, ParserConfigurationException, IOException, DOMException, SAXException {
         super(args);
         GlobalExceptionHandler.add(new GUIReporter());
         try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {
             //Oh well, go with the default look and feel
         }
         for (int i = 0; i < args.length; i++) {
             if (args[i].equals("-tabs")) {
                 tabs = true;
             }
         }
     }
 
     public void start() {
         frame = new JFrame(frameName);
         JMenuBar menubar = new JMenuBar();
         frame.setJMenuBar(menubar);
         JMenu fileMenu = new JMenu("File");
         menubar.add(fileMenu);
         final JMenuItem saveAs = new JMenuItem("Save As...");
         saveAs.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                         FileDialog fileDialog = new FileDialog(frame);
                         fileDialog.setMode(FileDialog.SAVE);
                         fileDialog.show();
                         String outfiledir = fileDialog.getDirectory();
                         String outfilename = fileDialog.getFile();
                         if (outfilename != null) {
                             File outfile = new File(outfiledir, outfilename);
                             try {
                                 save(outfile);
                             } catch (IOException ex) {
                                 GlobalExceptionHandler.handle("Unable to save to "+outfile, ex);
                             }
                         }
                     }
                 });
         JMenuItem save = new JMenuItem("Save");
         save.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                         if(getConfigFilename().startsWith("jar")){
                             saveAs.doClick();
                         }else{
                             File configFile = new File(getConfigFilename());
                             try {
                                 save(configFile);
                             } catch (IOException ex) {
                                 GlobalExceptionHandler.handle("Unable to save "+configFile, ex);
                             }
                         }
                     }
                 });
         fileMenu.add(save);
         fileMenu.add(saveAs);
         fileMenu.addSeparator();
         JMenuItem load = new JMenuItem("Open");
         fileMenu.add(load);
         load.addActionListener(new ActionListener(){
                     public void actionPerformed(ActionEvent e) {
                         FileDialog fileDialog = new FileDialog(frame, "Load a SOD config file");
                         fileDialog.setDirectory(".");
                         fileDialog.show();
                         String inFile = fileDialog.getFile();
                         if (inFile != null) {
                             try {
                                setConfigFile(inFile);
                                 loadGUI();
                             } catch (Exception ex) {
                                 GlobalExceptionHandler.handle("Unable to open "+inFile, ex);
                             }
                         }
                     }
 
                 });
         JMenuItem loadTutorial = new JMenuItem("Open Tutorial");
         loadTutorial.addActionListener(tutorialLoader);
         fileMenu.add(loadTutorial);
         JMenuItem loadWeed = new JMenuItem("Open WEED");
         loadWeed.addActionListener(new FileLoader(configFileBase + "weed.xml"));
         fileMenu.add(loadWeed);
         fileMenu.addSeparator();
         JMenuItem quit = new JMenuItem("Quit");
         fileMenu.add(quit);
         quit.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent e) {
                         System.exit(0);
                     }
                 });
         frame.getContentPane().setLayout(new BorderLayout());
         loadGUI();
     }
 
     private class FileLoader implements ActionListener{
         public FileLoader(String filename){
             this.filename = filename;
         }
 
         public void actionPerformed(ActionEvent e) {
             try {
                 setConfigFile(filename);
                 loadGUI();
             } catch (Exception ex) {
                 GlobalExceptionHandler.handle(ex);
             }
         }
         private String filename;
     }
 
     public void loadGUI(){
         Document doc = getDocument();
         if(doc == null){
             tutorialLoader.actionPerformed(null);
         }else{
             frame.getContentPane().removeAll();
             if (tabs) {
                 JTabbedPane tabPane = new JTabbedPane();
                 frame.getContentPane().add(tabPane, BorderLayout.CENTER);
                 // put each top level sod element in a panel
                 NodeList list = doc.getDocumentElement().getChildNodes();
                 for (int j = 0; j < list.getLength(); j++) {
                     if (list.item(j) instanceof Element) {
                         Element el = (Element)list.item(j);
                         JComponent panel = null;
                         if(el.getTagName().equals("properties")){
                             try {
                                 panel = propEditor.getGUI(el);
                             } catch (TransformerException e) {
                                 GlobalExceptionHandler.handle(e);
                             }
                         }else{
                             panel = new JPanel(new BorderLayout());
                             Box box = Box.createVerticalBox();
                             NodeList sublist = ((Element)list.item(j)).getChildNodes();
                             JComponent[] subComponents = getCompsForNodeList(sublist);
                             for (int i = 0; i < subComponents.length; i++) {
                                 box.add(subComponents[i]);
                                 box.add(Box.createVerticalStrut(10));
                             }
                             panel.add(box, BorderLayout.NORTH);
                         }
                         String tabName = getDisplayName(el.getTagName());
                         JScrollPane scrollPane = new JScrollPane(panel,
                                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                         scrollPane.getVerticalScrollBar().setBlockIncrement(50);
                         scrollPane.getVerticalScrollBar().setUnitIncrement(10);
                         tabPane.add(EditorUtil.capFirstLetter(tabName),scrollPane);
                     }
                 }
             } else {
                 JComponent comp = getCompForElement(doc.getDocumentElement());
                 Box box = Box.createVerticalBox();
                 box.add(comp);
                 box.add(Box.createGlue());
                 frame.getContentPane().add(new JScrollPane(box, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
             }
         }
 
         frame.pack();
         frame.show();
         frame.addWindowListener(new WindowAdapter() {
                     public void windowClosing(WindowEvent e) {
                         System.exit(0);
                     }
                 });
     }
 
     JComponent[] getCompsForNodeList(NodeList nl){
         List comps = new ArrayList();
         for (int i = 0; i < nl.getLength(); i++) {
             if (nl.item(i) instanceof Element) {
                 comps.add(getCompForElement((Element)nl.item(i)));
             }
         }
         return (JComponent[])comps.toArray(new JComponent[comps.size()]);
     }
 
 
     protected void save(File file) throws FileNotFoundException, IOException {
         FileOutputStream fos = new FileOutputStream(file);
         setConfigFileLocation(file.getAbsolutePath());
         save(fos);
         fos.close();
     }
 
     protected void save(OutputStream out) {
         BufferedWriter buf =
             new BufferedWriter(new OutputStreamWriter(out));
         Writer xmlWriter = new Writer();
         xmlWriter.setOutput(buf);
         xmlWriter.write(getDocument());
     }
 
     JComponent getCompForElement(Element element) {
         return getDefaultCompForElement(element);
     }
 
     JComponent getDefaultCompForElement(Element element) {
         JLabel label = new JLabel(getDisplayName(element.getTagName()));
         Box box = Box.createVerticalBox();
         JComponent comp = getCompForAttributes(element);
         if (comp != null) { box.add(comp); }
 
         NamedNodeMap attrList = element.getAttributes();
         NodeList list = element.getChildNodes();
         // simple case of only 1 child Text node and no attributes
         if (list.getLength() == 1 && list.item(0) instanceof Text && attrList.getLength() == 0) {
             comp = getCompForTextNode((Text)list.item(0));
             if (comp != null) {
                 box = Box.createHorizontalBox();
                 box.add(Box.createHorizontalStrut(10));
                 box.add(label);
                 box.add(new JLabel(" = "));
                 box.add(comp);
                 return box;
             }
         } else {
             for (int i = 0; i < list.getLength(); i++) {
                 if (list.item(i) instanceof Element) {
                     box.add(getCompForElement((Element)list.item(i)));
                 } else if (list.item(i) instanceof Text) {
                     Text text = (Text)list.item(i);
                     comp = getCompForTextNode(text);
                     if (comp != null) {
                         box.add(comp);
                     }
                 }
             }
         }
         return indent(label, box);
     }
 
     JComponent getCompForAttributes(Element element) {
         Box box = Box.createHorizontalBox();
         Box nameCol = Box.createVerticalBox();
         box.add(nameCol);
         Box valCol = Box.createVerticalBox();
         box.add(valCol);
         NamedNodeMap list = element.getAttributes();
         for (int i = 0; i < list.getLength(); i++) {
             if (list.item(i) instanceof Attr) {
                 Attr attr = (Attr)list.item(i);
                 valCol.add(EditorUtil.getLabeledTextField(attr));
             }
         }
         return box;
     }
 
     JComponent getCompForTextNode(Text text) {
         if (text.getNodeValue().trim().equals("")) {
             return null;
         }
         JTextField textField = new JTextField();
         textField.setText(text.getNodeValue().trim());
         TextListener textListen = new TextListener(text);
         textField.getDocument().addDocumentListener(textListen);
         return textField;
     }
 
     /** creates a JPanel with the bottom component slightly indented relative
      to the bottome one. */
     public Box indent(JComponent top, JComponent bottom) {
         Box box = Box.createVerticalBox();
         Box topRow = Box.createHorizontalBox();
         box.add(topRow);
         Box botRow = Box.createHorizontalBox();
         box.add(botRow);
 
         topRow.add(Box.createHorizontalStrut(5));
         topRow.add(top);
         topRow.add(Box.createGlue());
         botRow.add(Box.createRigidArea(new Dimension(15, 10)));
         botRow.add(bottom);
         botRow.add(Box.createGlue());
         return box;
     }
 
     private PropertyEditor propEditor = new PropertyEditor();
 
     public void setTabbed(boolean tabbed){ this.tabs = tabbed; }
 
     public static String getDisplayName(String tagName) {
         return nameProps.getProperty(tagName, tagName);
     }
 
     public static void main(String[] args) throws Exception {
         BasicConfigurator.configure();
         SimpleGUIEditor gui = new SimpleGUIEditor(args);
         gui.start();
     }
 
     private static final String configFileBase = "jar:edu/sc/seis/sod/data/configFiles/";
     public static final String TUTORIAL_LOC = configFileBase + "tutorial.xml";
     private FileLoader tutorialLoader = new FileLoader(TUTORIAL_LOC);
     protected String frameName = "Simple XML Editor GUI";
     private boolean tabs = false;
     private JFrame frame;
     static Properties nameProps = new Properties();
     private static String NAME_PROPS = "edu/sc/seis/sod/editor/names.prop";
     private static Logger logger = Logger.getLogger(SimpleGUIEditor.class);
     static {
         try {
             nameProps.load(SimpleGUIEditor.class.getClassLoader().getResourceAsStream(NAME_PROPS ));
         }catch(IOException e) {
             GlobalExceptionHandler.handle("Error in loading names Prop file",e);
         }
     }
 
 }
 
 
 
 
 
 
 
