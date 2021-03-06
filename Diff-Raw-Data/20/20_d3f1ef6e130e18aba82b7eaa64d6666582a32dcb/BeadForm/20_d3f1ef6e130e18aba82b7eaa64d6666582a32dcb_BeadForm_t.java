 /** jbead - http://www.brunoldsoftware.ch
     Copyright (C) 2001-2012  Damian Brunold
 
     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.
 
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package ch.jbead;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.AdjustmentEvent;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.awt.print.PageFormat;
 import java.awt.print.PrinterJob;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.ResourceBundle;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import javax.swing.Action;
 import javax.swing.ButtonGroup;
 import javax.swing.JButton;
 import javax.swing.JCheckBoxMenuItem;
 import javax.swing.JColorChooser;
 import javax.swing.JFileChooser;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JMenu;
 import javax.swing.JMenuBar;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JRadioButtonMenuItem;
 import javax.swing.JScrollBar;
 import javax.swing.JToggleButton;
 import javax.swing.JToolBar;
 
 import ch.jbead.action.EditArrangeAction;
 import ch.jbead.action.EditDeleteLineAction;
 import ch.jbead.action.EditInsertLineAction;
 import ch.jbead.action.EditRedoAction;
 import ch.jbead.action.EditUndoAction;
 import ch.jbead.action.FileExitAction;
 import ch.jbead.action.FileMRUAction;
 import ch.jbead.action.FileNewAction;
 import ch.jbead.action.FileOpenAction;
 import ch.jbead.action.FilePrintAction;
 import ch.jbead.action.FilePrintSetupAction;
 import ch.jbead.action.FileSaveAction;
 import ch.jbead.action.FileSaveAsAction;
 import ch.jbead.action.ToolFillAction;
 import ch.jbead.action.ToolPencilAction;
 import ch.jbead.action.ToolPipetteAction;
 import ch.jbead.action.ToolSelectAction;
 import ch.jbead.action.ViewCorrectedAction;
 import ch.jbead.action.ViewDraftAction;
 import ch.jbead.action.ViewReportAction;
 import ch.jbead.action.ViewSimulationAction;
 import ch.jbead.action.ViewZoomInAction;
 import ch.jbead.action.ViewZoomNormalAction;
 import ch.jbead.action.ViewZoomOutAction;
 import ch.jbead.dialog.AboutBox;
 import ch.jbead.dialog.CopyForm;
 import ch.jbead.dialog.PatternWidthForm;
 
 /**
  *
  */
 public class BeadForm extends JFrame implements Localization, ModelListener {
 
     private static final long serialVersionUID = 1L;
 
     private static final int SHIFTING_INTERVAL = 150;
     private static final int UPDATE_INTERVAL = 500;
 
     private ResourceBundle bundle = ResourceBundle.getBundle("jbead");
 
     private Model model = new Model(this);
     private Selection selection = new Selection();
 
     private boolean dragging;
 
     private List<File> mru = new ArrayList<File>();
 
     private JToolBar toolbar = new JToolBar();
 
     private ButtonGroup colorsGroup = new ButtonGroup();
     private List<ColorButton> colors = new ArrayList<ColorButton>();
 
     private JScrollBar scrollbar = new JScrollBar(JScrollBar.VERTICAL);
 
     private DraftPanel draft = new DraftPanel(model, selection, this);
     private CorrectedPanel corrected = new CorrectedPanel(model, selection, this);
     private SimulationPanel simulation = new SimulationPanel(model, selection, this);
     private ReportPanel report = new ReportPanel(model, selection, this);
 
     private JLabel laDraft = new JLabel("draft");
     private JLabel laCorrected = new JLabel("corrected");
     private JLabel laSimulation = new JLabel("simulation");
     private JLabel laReport = new JLabel("report");
 
     private JMenuItem viewDraft;
     private JMenuItem viewCorrected;
     private JMenuItem viewSimulation;
     private JMenuItem viewReport;
 
     private JMenu menuPattern = new JMenu("pattern");
     private JMenuItem patternWidth = new JMenuItem("width");
 
     private JMenu menuInfo = new JMenu("?");
     private JMenuItem infoAbout = new JMenuItem("about jbead");
 
     private JButton sbRotateleft;
     private JButton sbRotateright;
 
     private ButtonGroup toolsGroup = new ButtonGroup();
     private JRadioButtonMenuItem toolPencil;
     private JRadioButtonMenuItem toolSelect;
     private JRadioButtonMenuItem toolFill;
     private JRadioButtonMenuItem toolPipette;
 
     private ButtonGroup sbToolsGroup = new ButtonGroup();
     private JToggleButton sbToolPencil;
     private JToggleButton sbToolSelect;
     private JToggleButton sbToolFill;
     private JToggleButton sbToolPipette;
 
     private PageFormat pageFormat;
 
     private JPanel main = new JPanel();
     private JLabel statusbar = new JLabel("X");
 
     private Map<String, Action> actions = new HashMap<String, Action>();
 
     private Timer updateTimer;
 
     private Timer shiftTimer;
 
     ActionListener colorActionListener = new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
             colorClick(e);
         }
     };
 
     MouseAdapter colorMouseAdapter = new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
             super.mouseClicked(e);
             if (e.getClickCount() == 2) {
                 colorDblClick(e.getSource());
             }
         }
 
     };
 
     public BeadForm() {
         super("jbead");
         createGUI();
         model.addListener(this);
         model.clear();
         selection.clear();
         setColorIcons();
         loadMRU();
         updateMRU();
         updateScrollbar();
         initCloseHandler();
 
         // persist settings?
         viewDraft.setSelected(true);
         viewCorrected.setSelected(true);
         viewSimulation.setSelected(true);
         viewReport.setSelected(true);
 
         setIconImage(ImageFactory.getImage("jbead-16"));
 
         // TODO persist location and size in settings
         setSize(1024, 700);
         setLocation(100, 35);
 
         // TODO persist the pageFormat in Settings?
         pageFormat = PrinterJob.getPrinterJob().defaultPage();
         pageFormat.setOrientation(PageFormat.LANDSCAPE);
 
         selection.addListener(draft);
 
         updateTimer = new Timer("updateTimer", true);
         updateTimer.schedule(new TimerTask() {
             @Override
             public void run() {
                 updateHandler();
             }
         }, UPDATE_INTERVAL, UPDATE_INTERVAL);
     }
 
     private void initCloseHandler() {
         setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
         addWindowListener(new WindowAdapter() {
             @Override
             public void windowClosing(WindowEvent e) {
                 if (canTerminateApp()) {
                     System.exit(0);
                 }
             }
         });
     }
 
     public void registerAction(String name, Action action) {
         actions.put(name, action);
     }
 
     @Override
     public ResourceBundle getBundle() {
         return bundle;
     }
 
     @Override
     public String getString(String key) {
         return getBundle().getString(key);
     }
 
     public Action getAction(String name) {
         return actions.get(name);
     }
 
     private void createGUI() {
         createMenu();
         setLayout(new BorderLayout());
         add(toolbar, BorderLayout.NORTH);
         add(main, BorderLayout.CENTER);
         add(statusbar, BorderLayout.SOUTH);
         createToolbar();
         createMainGUI();
     }
 
     private void createMenu() {
         JMenuBar menubar = new JMenuBar();
         menubar.add(createFileMenu());
         menubar.add(createEditMenu());
         menubar.add(createViewMenu());
         menubar.add(createToolMenu());
         menubar.add(createPatternMenu());
         menubar.add(createInfoMenu());
         setJMenuBar(menubar);
     }
 
     private JMenu createFileMenu() {
         JMenu menuFile = new JMenu(bundle.getString("action.file"));
         menuFile.add(new FileNewAction(this));
         menuFile.add(new FileOpenAction(this));
         menuFile.add(new FileSaveAction(this));
         menuFile.add(new FileSaveAsAction(this));
         menuFile.addSeparator();
         menuFile.add(new FilePrintAction(this));
         menuFile.add(new FilePrintSetupAction(this));
         menuFile.addSeparator();
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 0)));
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 1)));
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 2)));
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 3)));
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 4)));
         menuFile.add(new MRUMenuItem(new FileMRUAction(this, 5)));
         menuFile.addSeparator(); // TODO what if no mru files are there?
         menuFile.add(new FileExitAction(this));
         return menuFile;
     }
 
     private JMenu createEditMenu() {
         JMenu menuEdit = new JMenu(bundle.getString("action.edit"));
         menuEdit.add(new EditUndoAction(this));
         menuEdit.add(new EditRedoAction(this));
         menuEdit.add(new EditArrangeAction(this));
         JMenu menuEditLine = new JMenu(bundle.getString("action.edit.line"));
         menuEdit.add(menuEditLine);
         menuEditLine.add(new EditInsertLineAction(this));
         menuEditLine.add(new EditDeleteLineAction(this));
         return menuEdit;
     }
 
     private JMenu createViewMenu() {
         JMenu menuView = new JMenu(bundle.getString("action.view"));
         menuView.add(viewDraft = new JCheckBoxMenuItem(new ViewDraftAction(this)));
         menuView.add(viewCorrected = new JCheckBoxMenuItem(new ViewCorrectedAction(this)));
         menuView.add(viewSimulation = new JCheckBoxMenuItem(new ViewSimulationAction(this)));
         menuView.add(viewReport = new JCheckBoxMenuItem(new ViewReportAction(this)));
         menuView.addSeparator();
         menuView.add(new ViewZoomInAction(this));
         menuView.add(new ViewZoomNormalAction(this));
         menuView.add(new ViewZoomOutAction(this));
         return menuView;
     }
 
     private JMenu createToolMenu() {
         JMenu menuTool = new JMenu(bundle.getString("action.tool"));
         menuTool.add(toolPencil = new JRadioButtonMenuItem(new ToolPencilAction(this)));
         menuTool.add(toolSelect = new JRadioButtonMenuItem(new ToolSelectAction(this)));
         menuTool.add(toolFill = new JRadioButtonMenuItem(new ToolFillAction(this)));
         menuTool.add(toolPipette = new JRadioButtonMenuItem(new ToolPipetteAction(this)));
         toolsGroup.add(toolPencil);
         toolsGroup.add(toolSelect);
         toolsGroup.add(toolFill);
         toolsGroup.add(toolPipette);
         return menuTool;
     }
 
     private JMenu createPatternMenu() {
         menuPattern.add(patternWidth);
         patternWidth.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 patternWidthClick();
             }
         });
         return menuPattern;
     }
 
     private JMenu createInfoMenu() {
         menuInfo.add(infoAbout);
         infoAbout.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 new AboutBox(BeadForm.this).setVisible(true);
             }
         });
         return menuInfo;
     }
 
     private void createToolbar() {
         toolbar.add(getAction("file.new"));
         toolbar.add(getAction("file.open"));
         toolbar.add(getAction("file.save"));
         toolbar.add(getAction("file.print"));
         toolbar.add(getAction("edit.undo"));
         toolbar.add(getAction("edit.redo"));
         toolbar.add(sbRotateleft = new JButton(ImageFactory.getIcon("sb_prev")));
         sbRotateleft.addMouseListener(new MouseAdapter() {
             @Override
             public void mousePressed(MouseEvent e) {
                 super.mousePressed(e);
                 sbRotateleftMouseDown(e);
             }
             @Override
             public void mouseReleased(MouseEvent e) {
                 super.mouseReleased(e);
                 sbRotateleftMouseUp(e);
             }
         });
         toolbar.add(sbRotateright = new JButton(ImageFactory.getIcon("sb_next")));
         sbRotateright.addMouseListener(new MouseAdapter() {
             @Override
             public void mousePressed(MouseEvent e) {
                 super.mousePressed(e);
                 sbRotaterightMouseDown(e);
             }
             @Override
             public void mouseReleased(MouseEvent e) {
                 super.mouseReleased(e);
                 sbRotaterightMouseUp(e);
             }
         });
         toolbar.add(getAction("edit.arrange"));
 
         toolbar.addSeparator();
 
         toolbar.add(sbToolPencil = new JToggleButton(getAction("tool.pencil")));
         toolbar.add(sbToolSelect = new JToggleButton(getAction("tool.select")));
         toolbar.add(sbToolFill = new JToggleButton(getAction("tool.fill")));
         toolbar.add(sbToolPipette = new JToggleButton(getAction("tool.pipette")));
 
         sbToolPencil.setSelected(true);
 
         sbToolsGroup.add(sbToolPencil);
         sbToolsGroup.add(sbToolSelect);
         sbToolsGroup.add(sbToolFill);
         sbToolsGroup.add(sbToolPipette);
 
         toolbar.addSeparator();
 
         for (int i = 0; i < Model.COLOR_COUNT; i++) {
             toolbar.add(createColorButton(i));
         }
         colors.get(1).setSelected(true);
     }
 
     private ColorButton createColorButton(int index) {
         ColorButton button = new ColorButton(new ColorIcon(model, (byte) index));
         button.addActionListener(colorActionListener);
         button.addMouseListener(colorMouseAdapter);
         colorsGroup.add(button);
         colors.add(button);
         return button;
     }
 
     private void createMainGUI() {
         main.setLayout(new GridBagLayout());
 
         GridBagConstraints c = new GridBagConstraints();
         c.gridx = 0;
         c.gridy = 0;
         c.weightx = 1;
         c.weighty = 1;
         c.fill = GridBagConstraints.BOTH;
         main.add(draft, c);
 
         c = new GridBagConstraints();
         c.gridx = 0;
         c.gridy = 1;
         main.add(laDraft, c);
 
         c = new GridBagConstraints();
         c.gridx = 1;
         c.gridy = 0;
         c.weightx = 1;
         c.weighty = 1;
         c.fill = GridBagConstraints.BOTH;
         main.add(corrected, c);
 
         c = new GridBagConstraints();
         c.gridx = 1;
         c.gridy = 1;
         main.add(laCorrected, c);
 
         c = new GridBagConstraints();
         c.gridx = 2;
         c.gridy = 0;
         c.weightx = 1;
         c.weighty = 1;
         c.fill = GridBagConstraints.BOTH;
         main.add(simulation, c);
 
         c = new GridBagConstraints();
         c.gridx = 2;
         c.gridy = 1;
         main.add(laSimulation, c);
 
         c = new GridBagConstraints();
         c.gridx = 3;
         c.gridy = 0;
         c.weightx = 5;
         c.weighty = 1;
         c.fill = GridBagConstraints.BOTH;
         main.add(report, c);
 
         c = new GridBagConstraints();
         c.gridx = 3;
         c.gridy = 1;
         main.add(laReport, c);
 
         c = new GridBagConstraints();
         c.gridx = 4;
         c.gridy = 0;
         c.fill = GridBagConstraints.VERTICAL;
         main.add(scrollbar, c);
     }
 
     private void setColorIcons() {
         for (byte i = 0; i < Model.COLOR_COUNT; i++) {
             colors.get(i).setIcon(new ColorIcon(model, i));
         }
     }
 
     private void updateScrollbar() {
         int h = draft.getHeight() / model.getGridy();
         assert (h < model.getHeight());
         scrollbar.setMinimum(0);
         scrollbar.setMaximum(model.getHeight() - h);
         if (scrollbar.getMaximum() < 0) scrollbar.setMaximum(0);
         scrollbar.setUnitIncrement(h);
         scrollbar.setBlockIncrement(h);
         scrollbar.setValue(scrollbar.getMaximum() - scrollbar.getBlockIncrement() - model.getScroll());
     }
 
     public void fileNewClick() {
         // ask whether to save modified document
         if (model.isModified()) {
             int answer = JOptionPane.showConfirmDialog(this, getString("savechanges"));
             if (answer == JOptionPane.CANCEL_OPTION) return;
             if (answer == JOptionPane.YES_OPTION) {
                 fileSaveClick();
             }
         }
 
         // delete all
         selection.clear();
         model.clear();
         colors.get(1).setSelected(true);
         setColorIcons();
         updateScrollbar();
     }
 
     public void loadFile(File file, boolean addtomru) {
         // ask whether to save modified document
         if (model.isModified()) {
             int answer = JOptionPane.showConfirmDialog(this, getString("savechanges"));
             if (answer == JOptionPane.CANCEL_OPTION) return;
             if (answer == JOptionPane.YES_OPTION) {
                 fileSaveClick();
             }
         }
 
         // Datei laden
         try {
             JBeadInputStream in = new JBeadInputStream(new FileInputStream(file));
             try {
                 String strid = in.read(13);
                 if (!strid.equals("DB-BEAD/01:\r\n")) {
                     JOptionPane.showMessageDialog(this, getString("invalidformat"));
                     return;
                 }
                 selection.clear();
                 model.clear();
                 model.load(in);
                 viewDraft.setSelected(in.readBool());
                 viewCorrected.setSelected(in.readBool());
                 viewSimulation.setSelected(in.readBool());
                 colors.get(model.getColorIndex()).setSelected(true);
                 updateScrollbar();
             } finally {
                 in.close();
             }
         } catch (Exception e) {
             JOptionPane.showMessageDialog(this, getString("load.failed").replace("{1}", file.getPath()).replace("{2}", e.getMessage()));
             model.clear();
         }
         model.setSaved();
         model.setModified(false);
         model.setRepeatDirty();
         model.setFile(file);
         if (addtomru) addToMRU(file);
     }
 
     public void fileOpenClick() {
         JFileChooser dialog = new JFileChooser();
         dialog.setCurrentDirectory(model.getCurrentDirectory());
         dialog.setAcceptAllFileFilterUsed(true);
         dialog.setFileFilter(new DbbFileFilter());
         if (dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
             loadFile(dialog.getSelectedFile(), true);
         }
     }
 
     public void fileSaveClick() {
         if (model.isSaved()) {
             // Einfach abspeichern...
             try {
                 JBeadOutputStream out = new JBeadOutputStream(new FileOutputStream(model.getFile()));
                 try {
                     out.write("DB-BEAD/01:\r\n");
                     model.save(out);
                     out.writeBool(viewDraft.isSelected());
                     out.writeBool(viewCorrected.isSelected());
                     out.writeBool(viewSimulation.isSelected());
                     // report flag is not saved?!
                     model.setModified(false);
                 } finally {
                     out.close();
                 }
             } catch (IOException e) {
                 // xxx
             }
         } else {
             fileSaveasClick();
         }
     }
 
     public void fileSaveasClick() {
         JFileChooser dialog = new JFileChooser();
         dialog.setCurrentDirectory(model.getCurrentDirectory());
         dialog.setAcceptAllFileFilterUsed(true);
         dialog.setFileFilter(new DbbFileFilter());
         if (dialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
             if (dialog.getSelectedFile().exists()) {
                 String msg = getString("fileexists");
                 msg = msg.replace("{1}", dialog.getSelectedFile().getName());
                 if (JOptionPane.showConfirmDialog(this, msg, "Overwrite", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                     return;
                 }
             }
             File file = dialog.getSelectedFile();
             if (file.getName().indexOf('.') == -1) {
                 file = new File(file.getParentFile(), file.getName() + ".dbb");
             }
             model.setFile(file);
             model.setSaved();
             fileSaveClick();
             addToMRU(model.getFile());
         }
     }
 
     public void filePrintClick(boolean showDialog) {
         new DesignPrinter(model, this, pageFormat, draft.isVisible(), corrected.isVisible(), simulation.isVisible(), report.isVisible()).print(showDialog);
     }
 
     public void filePrintersetupClick() {
         PrinterJob pj = PrinterJob.getPrinterJob();
         pageFormat = pj.pageDialog(pj.defaultPage());
     }
 
     public void fileExitClick() {
         if (model.isModified()) {
             int r = JOptionPane.showConfirmDialog(this, getString("savechanges"));
             if (r == JOptionPane.CANCEL_OPTION) return;
             if (r == JOptionPane.OK_OPTION) fileSaveClick();
         }
         // TODO maybe need to save settings?
         System.exit(0);
     }
 
     public void patternWidthClick() {
         int old = model.getWidth();
         PatternWidthForm form = new PatternWidthForm(this);
         form.setPatternWidth(model.getWidth());
         form.setVisible(true);
         if (form.isOK()) {
             selection.clear();
             // TODO move this to model
             model.snapshot();
             model.setWidth(form.getPatternWidth());
             if (!model.isModified()) {
                 model.setModified(old != model.getWidth());
             }
             model.setRepeatDirty();
         }
     }
 
     private void draftLinePreview() {
         if (!sbToolPencil.isSelected()) return;
         if (!selection.isActive()) return;
         draft.linePreview(selection.getOrigin(), selection.getLineDest());
     }
 
     private void drawPrepress() {
         if (sbToolPencil.isSelected()) {
             draft.drawPrepress(selection.getOrigin());
         }
     }
 
     public void draftMouseDown(MouseEvent event) {
         if (dragging) return;
         Point pt = new Point(event.getX(), event.getY());
         if (event.getButton() == MouseEvent.BUTTON1 && draft.mouseToField(pt)) {
             dragging = true;
             selection.init(pt);
             drawPrepress();
             draftLinePreview();
         }
     }
 
     public void draftMouseMove(MouseEvent event) {
         Point pt = new Point(event.getX(), event.getY());
         if (dragging && draft.mouseToField(pt)) {
             draftLinePreview();
             selection.update(pt);
             draftLinePreview();
         }
     }
 
     public void draftMouseUp(MouseEvent event) {
         Point pt = new Point(event.getX(), event.getY());
         if (dragging && draft.mouseToField(pt)) {
             draftLinePreview();
             selection.update(pt);
             dragging = false;
             if (sbToolPencil.isSelected()) {
                 if (!selection.isActive()) {
                     setPoint(selection.getOrigin());
                 } else {
                     drawLine(selection.getOrigin(), selection.getLineDest());
                 }
             } else if (sbToolFill.isSelected()) {
                 fillLine(selection.getOrigin());
             } else if (sbToolPipette.isSelected()) {
                 selectColorFrom(selection.getOrigin());
             } else if (sbToolSelect.isSelected()) {
                 if (!selection.isActive()) {
                     setPoint(selection.getOrigin());
                 }
             }
         }
     }
 
     private void selectColorFrom(Point pt) {
         byte colorIndex = model.get(pt.scrolled(model.getScroll()));
         colors.get(colorIndex).setSelected(true);
     }
 
     private void drawLine(Point begin, Point end) {
         model.snapshot();
         model.drawLine(begin, end);
     }
 
     private void fillLine(Point pt) {
         model.snapshot();
         model.fillLine(pt);
     }
 
     private void setPoint(Point pt) {
         model.snapshot();
         model.setPoint(pt);
     }
 
     public void editUndoClick() {
         model.undo();
     }
 
     public void editRedoClick() {
         model.redo();
     }
 
     public void viewZoomInClick() {
         model.zoomIn();
         updateScrollbar();
     }
 
     public void viewZoomNormalClick() {
         if (model.isNormalZoom()) return;
         model.zoomNormal();
         updateScrollbar();
     }
 
     public void viewZoomOutClick() {
         model.zoomOut();
         updateScrollbar();
     }
 
     public void viewDraftClick() {
         draft.setVisible(viewDraft.isSelected());
         laDraft.setVisible(draft.isVisible());
     }
 
     public void viewCorrectedClick() {
         corrected.setVisible(viewCorrected.isSelected());
         laCorrected.setVisible(corrected.isVisible());
     }
 
     public void viewSimulationClick() {
         simulation.setVisible(viewSimulation.isSelected());
         laSimulation.setVisible(simulation.isVisible());
     }
 
     public void viewReportClick() {
         report.setVisible(viewReport.isSelected());
         laReport.setVisible(report.isVisible());
     }
 
     public void formKeyUp(KeyEvent event) {
         int Key = event.getKeyCode();
         if (Key == KeyEvent.VK_F5) {
             repaint();
         } else if (event.getKeyChar() >= '0' && event.getKeyChar() <= '9') {
             model.setColorIndex((byte) (event.getKeyChar() - '0'));
             colors.get(model.getColorIndex()).setSelected(true);
         } else if (Key == KeyEvent.VK_SPACE) {
             getAction("tool.pencil").putValue("SELECT", true);
 //            sbToolPoint.setSelected(true);
 //            toolPoint.setSelected(true);
         } else if (Key == KeyEvent.VK_ESCAPE && shiftTimer != null) {
             shiftTimer.cancel();
             shiftTimer = null;
         }
     }
 
     private void rotateLeft() {
         model.shiftLeft();
     }
 
     private void rotateRight() {
         model.shiftRight();
     }
 
     // TODO split this for every color toolbar button
     public void colorClick(ActionEvent event) {
         ColorButton sender = (ColorButton) event.getSource();
         model.setColorIndex(sender.getColorIndex());
     }
 
     // TODO split this for every color toolbar button
     public void colorDblClick(Object sender) {
         ColorButton colorButton = (ColorButton) sender;
         byte c = colorButton.getColorIndex();
         Color color = JColorChooser.showDialog(this, "choose color", model.getColor(c));
         if (color == null) return;
         model.snapshot();
         model.setColor(c, color);
         setColorIcons();
     }
 
     // TODO handle out parameter
     private void scrollbarScroll(AdjustmentEvent event) {
         int oldscroll = model.getScroll();
         // if (ScrollPos > scrollbar.Max - scrollbar.PageSize) ScrollPos =
         // scrollbar.Max - scrollbar.PageSize;
         model.setScroll(scrollbar.getMaximum() - scrollbar.getBlockIncrement() - scrollbar.getValue());
     }
 
     private void updateHandler() {
         getAction("edit.arrange").setEnabled(selection.isActive());
         getAction("edit.undo").setEnabled(model.canUndo());
         getAction("edit.redo").setEnabled(model.canRedo());
 
         if (model.isRepeatDirty()) {
             model.updateRepeat();
         }
         model.prepareSnapshot();
     }
 
     public void toolPencilClick() {
         selection.clear();
     }
 
     public void toolSelectClick() {
         selection.clear();
     }
 
     public void toolFillClick() {
         selection.clear();
     }
 
     public void toolPipetteClick() {
         selection.clear();
     }
 
     public void correctedMouseUp(MouseEvent event) {
         if (event.getButton() == MouseEvent.BUTTON1) {
             model.snapshot();
             corrected.togglePoint(new Point(event.getX(), event.getY()));
         }
     }
 
     public void simulationMouseUp(MouseEvent event) {
         if (event.getButton() == MouseEvent.BUTTON1) {
             model.snapshot();
             simulation.togglePoint(new Point(event.getX(), event.getY()));
         }
     }
 
     public void infoAboutClick() {
         new AboutBox(this).setVisible(true);
     }
 
     private void sbRotaterightMouseDown(MouseEvent event) {
         rotateRight();
         if (shiftTimer != null) shiftTimer.cancel();
         shiftTimer = new Timer("shiftTimer");
         shiftTimer.scheduleAtFixedRate(new TimerTask() {
             @Override
             public void run() {
                 rotateRight();
             }
         }, SHIFTING_INTERVAL, SHIFTING_INTERVAL);
     }
 
     private void sbRotaterightMouseUp(MouseEvent event) {
         shiftTimer.cancel();
         shiftTimer = null;
     }
 
     private void sbRotateleftMouseDown(MouseEvent event) {
         rotateLeft();
         if (shiftTimer != null) shiftTimer.cancel();
         shiftTimer = new Timer("shiftTimer");
         shiftTimer.scheduleAtFixedRate(new TimerTask() {
             @Override
             public void run() {
                 rotateLeft();
             }
         }, SHIFTING_INTERVAL, SHIFTING_INTERVAL);
     }
 
     private void sbRotateleftMouseUp(MouseEvent event) {
         shiftTimer.cancel();
         shiftTimer = null;
     }
 
     public void formKeyDown(KeyEvent event) {
         if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
             rotateRight();
         } else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
             rotateLeft();
         }
     }
 
     private boolean canTerminateApp() {
         if (model.isModified()) {
             int r = JOptionPane.showConfirmDialog(this, getString("savechanges"));
             if (r == JOptionPane.CANCEL_OPTION) {
                 return false;
             }
             if (r == JOptionPane.OK_OPTION) fileSaveClick();
         }
         return true;
     }
 
     public void editArrangeClick() {
         CopyForm copyform = new CopyForm(this);
         copyform.setVisible(true);
         if (copyform.isOK()) {
             int copies = copyform.getCopies();
             int offset = copyform.getOffset(model.getWidth());
             model.arrangeSelection(selection, copies, offset);
         }
     }
 
     public void editInsertLineClick() {
         model.snapshot();
         model.insertLine();
     }
 
     public void editDeleteLineClick() {
         model.snapshot();
         model.deleteLine();
     }
 
     public void updateTitle() {
         String c = getString("title");
         if (model.isSaved()) {
             c = c.replace("{1}", model.getFile().getName());
         } else {
             c = c.replace("{1}", getString("unnamed"));
         }
         if (model.isModified()) {
             c += "*";
         }
         setTitle(c);
     }
 
     private void addToMRU(File file) {
         if (file.getPath() == "") return;
         if (mru.contains(file)) {
             pullToTop(file);
         } else {
             addToTop(file);
         }
         updateMRU();
         saveMRU();
     }
 
     private void pullToTop(File file) {
         mru.remove(file);
         mru.add(0, file);
     }
 
     private void addToTop(File file) {
         mru.add(0, file);
         if (mru.size() > 6) {
             mru.remove(mru.size() - 1);
         }
     }
 
     private void updateMRU() {
         for (int i = 0; i < mru.size(); i++) {
             getAction("file.mru" + i).putValue(Action.NAME, getMRUDisplayName(i));
         }
         // TODO maybe have to set visibility of separator after last mru menu item
     }
 
     private String getMRUDisplayName(int index) {
         String curdir = model.getCurrentDirectory().getAbsolutePath();
         String path = mru.get(index).getAbsolutePath();
         if (path.startsWith(curdir)) {
             return path.substring(curdir.length() + 1);
         } else {
             return path;
         }
     }
 
     public void loadMRUFile(int index) {
         loadFile(mru.get(index), true);
     }
 
     private void saveMRU() {
         Settings settings = new Settings();
         settings.SetCategory("mru");
         for (int i = 0; i < mru.size(); i++) {
            settings.SaveString("mru" + i, mru.get(i).getAbsolutePath());
         }
     }
 
     private void loadMRU() {
         Settings settings = new Settings();
         settings.SetCategory("mru");
         mru.clear();
         for (int i = 0; i < 6; i++) {
             addMRU(settings.LoadString("mru" + i));
         }
     }
 
     private void addMRU(String path) {
         if (path.isEmpty()) return;
         mru.add(new File(path));
     }
 
     public static void main(String[] args) {
         new BeadForm().setVisible(true);
     }
 
     @Override
     public void pointChanged(Point pt) {
         updateTitle();
     }
 
     @Override
     public void modelChanged() {
         setColorIcons();
         updateTitle();
     }
 
     @Override
     public void colorChanged(byte colorIndex) {
         colors.get(colorIndex).setIcon(new ColorIcon(model, colorIndex));
         updateTitle();
     }
 
     @Override
     public void scrollChanged(int scroll) {
         updateTitle();
     }
 
     @Override
     public void shiftChanged(int shift) {
         updateTitle();
     }
 
     @Override
     public void zoomChanged(int gridx, int gridy) {
         updateTitle();
     }
 
     @Override
     public void repeatChanged(int repeat, int colorRepeat) {
         updateTitle();
     }
 
 }
