 /*
  *    Copyright 2010 University of Toronto
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
 
 package savant.view.swing;
 
 import savant.file.FileFormat;
 import com.jidesoft.action.CommandBar;
 import com.jidesoft.docking.DockableFrame;
 import com.jidesoft.swing.JideButton;
 import com.jidesoft.swing.JideMenu;
 
 import savant.controller.DrawModeController;
 import savant.controller.RangeController;
 import savant.controller.event.drawmode.DrawModeChangedEvent;
 import savant.util.DrawingInstructions;
 import savant.util.Mode;
 import savant.util.Range;
 import savant.view.swing.continuous.ContinuousTrackRenderer;
 import savant.view.swing.interval.BAMCoverageViewTrack;
 import savant.view.swing.interval.BAMTrackRenderer;
 import savant.view.swing.interval.BEDTrackRenderer;
 import savant.view.swing.interval.IntervalTrackRenderer;
 import savant.view.swing.point.PointTrackRenderer;
 import savant.view.swing.sequence.SequenceTrackRenderer;
 
 import javax.swing.*;
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.image.BufferedImage;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import savant.controller.ReferenceController;
 import savant.settings.ColourSettings;
 import savant.view.swing.interval.BAMViewTrack;
 
 /**
  *
  * @author mfiume
  */
 public class Frame {
 
     private boolean isHidden = false;
     private GraphPane graphPane;
     private JLayeredPane frameLandscape;
     private List<ViewTrack> tracks;
     private boolean isLocked;
     private Range currentRange;
 
     private JLayeredPane jlp;
 
    //public CommandBar commandBar;
    public JMenuBar commandBar;
     public CommandBar commandBarHidden;
     private JPanel arcLegend;
     //private boolean legend = false;
     private List<JCheckBoxMenuItem> visItems;
     private JideButton arcButton;
     //private JMenu intervalMenu;
     private JideButton intervalButton;
 
     private boolean commandBarActive = true;
     private DockableFrame parent;
     private String name;
 
     public JScrollPane scrollPane;
     //private boolean tempCommandBar = true;
 
     public boolean isHidden() { return this.isHidden; }
     public void setHidden(boolean isHidden) { this.isHidden = isHidden; }
 
     public GraphPane getGraphPane() { return this.graphPane; }
     public JComponent getFrameLandscape() { return this.frameLandscape; }
     public List<ViewTrack> getTracks() { return this.tracks; }
 
     public boolean isOpen() { return getGraphPane() != null; }
 
     //public Frame(JComponent frameLandscape) { this((JLayeredPane)frameLandscape, null, null); }
 
     //public Frame(JComponent frameLandscape, List<ViewTrack> tracks, String name) { this((JLayeredPane)frameLandscape, tracks, new ArrayList<TrackRenderer>(), name); }
 
     //public Frame(JLayeredPane frameLandscape, List<ViewTrack> tracks, List<TrackRenderer> renderers, String name)
 
 
     public Frame(List<ViewTrack> tracks, String name) {this(tracks, new ArrayList<TrackRenderer>(), name); }
 
     public Frame(List<ViewTrack> tracks, List<TrackRenderer> renderers, String name)
     {
 
         this.name = name;
 
         //INIT LEGEND PANEL
         arcLegend = new JPanel();
         arcLegend.setVisible(false);
 
         isLocked = false;
         this.tracks = new ArrayList<ViewTrack>();
         this.frameLandscape = new JLayeredPane();
         initGraph();
 
         //scrollpane
         scrollPane = new JScrollPane();
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setWheelScrollingEnabled(false);
         scrollPane.setBorder(null);
 
         //hide commandBar while scrolling
         MouseListener ml = new MouseListener(){
             public void mouseClicked(MouseEvent e) {}
             public void mousePressed(MouseEvent e) {
                 if(parent.isActive())
                     tempHideCommands();
             }
             public void mouseReleased(MouseEvent e) {
                 if(parent.isActive())
                     tempShowCommands();
             }
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         };
         scrollPane.getVerticalScrollBar().addMouseListener(ml);
         JScrollBar vsb = scrollPane.getVerticalScrollBar();
         for(int i = 0; i < vsb.getComponentCount(); i++){
             vsb.getComponent(i).addMouseListener(ml);
         }
 
 
         //add graphPane -> jlp -> scrollPane
         jlp = new JLayeredPane();
         jlp.setLayout(new GridBagLayout());
         GridBagConstraints gbc= new GridBagConstraints();
         gbc.fill = GridBagConstraints.BOTH;
         gbc.weightx = 1.0;
         gbc.weighty = 1.0;
         gbc.gridx = 0;
         gbc.gridy = 0;
         jlp.add(this.graphPane, gbc, 0);
 
 
         //scrollPane.getViewport().add(this.graphPane);
         scrollPane.getViewport().add(jlp);
 
 
 
         if (!tracks.isEmpty()) {
             int i=0;
             Iterator<ViewTrack> it = tracks.iterator();
             while ( it.hasNext()) {
                 ViewTrack track = it.next();
                 // FIXME:
                 track.setFrame(this);
                 TrackRenderer renderer=null;
                 if (!renderers.isEmpty()) {
                     renderer = renderers.get(i++);
                 }
                 addTrack(track, renderer);
 
                 //CREATE LEGEND PANEL
                 if(track.getDataType().toString().equals("INTERVAL_BAM")){
                     arcLegend = track.getTrackRenderers().get(0).arcLegendPaint();
                 }
             }
         }
         //frameLandscape.setLayout(new BorderLayout());
         //frameLandscape.add(getGraphPane());
 
         //COMMAND BAR
        commandBar = new JMenuBar();

        //THE FOLLOWING BLOCK ONLY APPLIED TO JIDE COMMANDBAR
        //commandBar = new CommandBar();
        //commandBar.setStretch(false);
        //commandBar.setPaintBackground(false);
        //commandBar.setChevronAlwaysVisible(false);
        
         commandBar.setOpaque(true);
        
         JMenu optionsMenu = createOptionsMenu();
         //JMenu infoMenu = createInfoMenu();
         //JideButton lockButton = createLockButton();
         JideButton hideButton = createHideButton();
         //JideButton colorButton = createColorButton();
         //commandBar.add(infoMenu);
         //commandBar.add(lockButton);
         //commandBar.add(colorButton);
         commandBar.add(optionsMenu);
         if(this.tracks.get(0).getDrawModes().size() > 0){
             JMenu displayMenu = createDisplayMenu();
             commandBar.add(displayMenu);
         }
          if (this.tracks.get(0).getDataType() == FileFormat.INTERVAL_BAM) {
             arcButton = createArcButton();
             commandBar.add(arcButton);
             arcButton.setVisible(false);
 
             intervalButton = createIntervalButton();
             commandBar.add(intervalButton);
             intervalButton.setVisible(false);
             String drawMode = this.getTracks().get(0).getDrawMode().getName();
             if(drawMode.equals("STANDARD") || drawMode.equals("VARIANTS")){
                 intervalButton.setVisible(true);
             }
 
             //intervalMenu = createIntervalMenu();
             //commandBar.add(intervalMenu);
             //intervalMenu.setVisible(false);
             //String drawMode = this.getTracks().get(0).getDrawMode().getName();
             //if(drawMode.equals("STANDARD") || drawMode.equals("VARIANTS")){
             //    intervalMenu.setVisible(true);
             //}
         }
         commandBar.add(new JSeparator(SwingConstants.VERTICAL));
         commandBar.add(hideButton);
         commandBar.setVisible(false);
 
         //COMMAND BAR HIDDEN
         //commandBarHidden.setVisible(false);
         commandBarHidden = new CommandBar();
         //commandBarHidden.setVisible(false);
         commandBarHidden.setOpaque(true);
         commandBarHidden.setChevronAlwaysVisible(false);
         JideButton showButton = createShowButton();
         commandBarHidden.add(showButton);
         commandBarHidden.setVisible(false);
 
         //GRID FRAMEWORK AND COMPONENT ADDING...
         frameLandscape.setLayout(new GridBagLayout());
         GridBagConstraints c = new GridBagConstraints();
         JLabel l;
         c.fill = GridBagConstraints.HORIZONTAL;
 
         //force commandBars to be full size
         JPanel contain1 = new JPanel();
         contain1.setOpaque(false);
         contain1.setLayout(new BorderLayout());
         contain1.setCursor(new Cursor(Cursor.HAND_CURSOR));
         contain1.add(commandBar, BorderLayout.WEST);
         JPanel contain2 = new JPanel();
         contain2.setOpaque(false);
         contain2.setLayout(new BorderLayout());
         contain2.setCursor(new Cursor(Cursor.HAND_CURSOR));
         contain2.add(commandBarHidden, BorderLayout.WEST);
 
         //add commandBar/hidden to top left
         c.weightx = 0;
         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridx = 0;
         c.gridy = 0;
         frameLandscape.add(contain1, c, 5);
         frameLandscape.add(contain2, c, 6);
         //commandBarHidden.setVisible(true);
 
         //filler to extend commandBars
         JPanel a = new JPanel();
         a.setCursor(new Cursor(Cursor.HAND_CURSOR));
         a.setMinimumSize(new Dimension(400,30));
         a.setOpaque(false);
         c.weightx = 0;
         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridx = 0;
         c.gridy = 1;
         frameLandscape.add(a, c, 5);
 
         //add filler to top middle
         l = new JLabel();
         //l.setOpaque(false);
         c.fill = GridBagConstraints.HORIZONTAL;
         c.weightx = 1.0;
         c.gridx = 1;
         c.gridy = 0;
         frameLandscape.add(l, c);
 
         //add arcLegend to bottom right
         c.fill = GridBagConstraints.HORIZONTAL;
         c.weightx = 0;
         c.weighty = 0;
         c.gridx = 2;
         c.gridy = 1;
         c.anchor = GridBagConstraints.NORTHEAST;
         frameLandscape.add(arcLegend, c, 6);
 
         //add graphPane to all cells
         c.fill = GridBagConstraints.BOTH;
         c.weightx = 1.0;
         c.weighty = 1.0;
         c.gridx = 0;
         c.gridy = 0;
         c.gridwidth = 3;
         c.gridheight = 2;
         frameLandscape.add(scrollPane, c, 0);
 
         frameLandscape.setLayer(commandBar, (Integer) JLayeredPane.PALETTE_LAYER);
         //frameLandscape.setLayer(getGraphPane(), (Integer) JLayeredPane.DEFAULT_LAYER);
         frameLandscape.setLayer(scrollPane, (Integer) JLayeredPane.DEFAULT_LAYER);
     }
 
     public void setActiveFrame(){
         if(commandBarActive) commandBar.setVisible(true);
         else commandBarHidden.setVisible(true);
     }
 
     public void setInactiveFrame(){
         commandBarHidden.setVisible(false);
         commandBar.setVisible(false);
     }
 
     public void resetLayers(){
         Frame f = this;
         if(f.getTracks().get(0).getDrawModes().size() > 0 && f.getTracks().get(0).getDrawMode().getName().equals("MATE_PAIRS")){
             f.arcLegend.setVisible(true);
         } else {
             f.arcLegend.setVisible(false);
         }
 
         ((JLayeredPane) this.getFrameLandscape()).moveToBack(this.getGraphPane());
     }
 
     private void tempHideCommands(){
         commandBar.setVisible(false);
         commandBarHidden.setVisible(false);
     }
 
     private void tempShowCommands(){
         if(commandBarActive){
             commandBar.setVisible(true);
             commandBarHidden.setVisible(false);
         } else {
             commandBarHidden.setVisible(true);
             commandBar.setVisible(false);
         }
     }
 
     /**
      * Create the button to hide the commandBar
      */
     private JideButton createHideButton() {
         JideButton button = new JideButton();
         button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/arrow_left.png")));
         button.setToolTipText("Hide this toolbar");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 commandBar.setVisible(false);
                 commandBarHidden.setVisible(true);
                 commandBarActive = false;
                 ((JideButton)commandBarHidden.getComponent(commandBarHidden.getComponentCount()-1)).setFocusPainted(false);
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create the button to show the commandBar
      */
     private JideButton createShowButton() {
         JideButton button = new JideButton();
         button.setLayout(new BorderLayout());
         JLabel l1 = new JLabel("Settings");
         l1.setOpaque(false);
         button.add(l1, BorderLayout.WEST);
         JLabel l2 = new JLabel(new javax.swing.ImageIcon(getClass().getResource("/savant/images/arrow_right.png")));
         button.add(l2, BorderLayout.EAST);
         button.setToolTipText("Show the toolbar");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 commandBar.setVisible(true);
                 commandBarHidden.setVisible(false);
                 commandBarActive = true;
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create lock button for commandBar
      */
     private JideButton createColorButton() {
         //TODO: This is temporary until there is an options menu
         JideButton button = new JideButton("Colour Settings  ");
         button.setToolTipText("Change the colour scheme for this track");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 tracks.get(0).captureColorParameters();
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create options menu for commandBar
      */
     private JMenu createOptionsMenu() {
         JCheckBoxMenuItem item;
        //JMenu menu = new JideMenu("Settings");
        JMenu menu = new JMenu("Settings");
         item = new JCheckBoxMenuItem("Lock Track");
         item.addActionListener(new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 graphPane.switchLocked();
             }
         });
         menu.add(item);
 
         JMenuItem item1;
         item1 = new JMenuItem("Colour Settings...");
         item1.addActionListener(new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 tracks.get(0).captureColorParameters();
             }
         });
         menu.add(item1);
         return menu;
     }
 
     /**
      * Create lock button for commandBar
      */
     private JideButton createLockButton() {
         //TODO: This is temporary until there is an options menu
         JideButton button = new JideButton("Lock Track  ");
         button.setToolTipText("Prevent range changes on this track");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 JideButton button = new JideButton();
                 for(int i = 0; i < commandBar.getComponentCount(); i++){
                     if(commandBar.getComponent(i).getClass() == JideButton.class){
                         button = (JideButton)commandBar.getComponent(i);
                         if(button.getText().equals("Lock Track  ") || button.getText().equals("Unlock Track  ")) break;
                     }
                 }
                 if(button.getText().equals("Lock Track  ")){
                     button.setText("Unlock Track  ");
                     button.setToolTipText("Allow range changes on this track");
                 } else {
                     button.setText("Lock Track  ");
                     button.setToolTipText("Prevent range changes on this track");
                 }
                 graphPane.switchLocked();
                 resetLayers();
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create info menu for commandBar
      */
     private JMenu createInfoMenu() {
         JMenu menu = new JideMenu("Info");
         JMenuItem item = new JMenuItem("Track Info...");
         item.addActionListener(new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 //TODO
             }
         });
         menu.add(item);
 
         return menu;
     }
 
     /**
      * Create the button to show the arc params dialog
      */
     private JideButton createArcButton() {
         JideButton button = new JideButton("Arc Options");
         button.setToolTipText("Change mate pair parameters");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 final BAMViewTrack innerTrack = (BAMViewTrack)tracks.get(0);
                 graphPane.getBAMParams(innerTrack);
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create interval button for commandBar
      */
     private JideButton createIntervalButton() {
         JideButton button = new JideButton("Interval Options");
         button.setToolTipText("Change interval display parameters");
         button.addMouseListener(new MouseListener() {
             public void mouseClicked(MouseEvent e) {
                 tracks.get(0).captureIntervalParameters();
             }
             public void mousePressed(MouseEvent e) {}
             public void mouseReleased(MouseEvent e) {}
             public void mouseEntered(MouseEvent e) {}
             public void mouseExited(MouseEvent e) {}
         });
         button.setFocusPainted(false);
         return button;
     }
 
     /**
      * Create the menu for interval options
      */
     /*private JMenu createIntervalMenu() {
         JMenu menu = new JideMenu("Interval Options");
         final JCheckBoxMenuItem dynamic = new JCheckBoxMenuItem("Dynamic Height");
         final JCheckBoxMenuItem fixed = new JCheckBoxMenuItem("Fixed Height");
 
         dynamic.addActionListener(new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 for(int i = 0; i < graphPane.getTrackRenderers().size(); i++){
                     graphPane.getTrackRenderers().get(i).setIntervalMode("dynamic");
                 }
                 dynamic.setState(true);
                 fixed.setState(false);
                 graphPane.setRenderRequired();
                 graphPane.repaint();
             }
         });
 
         fixed.addActionListener(new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 for(int i = 0; i < graphPane.getTrackRenderers().size(); i++){
                     graphPane.getTrackRenderers().get(i).setIntervalMode("fixed");
                 }
                 fixed.setState(true);
                 dynamic.setState(false);
                 graphPane.setRenderRequired();
                 graphPane.repaint();
             }
         });
 
         dynamic.setState(true);
         menu.add(dynamic);
         menu.add(fixed);
         return menu;
     }*/
 
     /**
      * Create display menu for commandBar
      */
     private JMenu createDisplayMenu() {
         JMenu menu = new JMenu("Display Mode");//new JideMenu("Display Mode");
 
         //Arc params (if bam file)
         /*if(this.tracks.get(0).getDataType() == FileFormat.INTERVAL_BAM){
             JMenuItem arcParam = new JMenuItem();
             arcParam.setText("Change Arc Parameters...");
             menu.add(arcParam);
             arcParam.addActionListener(new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     final BAMViewTrack innerTrack = (BAMViewTrack)tracks.get(0);
                     graphPane.getBAMParams(innerTrack);
                 }
             });
             JSeparator jSeparator1 = new JSeparator();
             menu.add(jSeparator1);
         }*/
 
         //display modes
         List<Mode> drawModes = this.tracks.get(0).getDrawModes();
         visItems = new ArrayList<JCheckBoxMenuItem>();
         for(int i = 0; i < drawModes.size(); i++){
             final JCheckBoxMenuItem item = new JCheckBoxMenuItem(drawModes.get(i).getName());
             item.addActionListener(new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     if(item.getState()){
                         for(int j = 0; j < visItems.size(); j++){
                             visItems.get(j).setState(false);
                             if(item.getText().equals(tracks.get(0).getDrawModes().get(j).getName())){
                                 DrawModeController.getInstance().switchMode(tracks.get(0), tracks.get(0).getDrawModes().get(j));
                             }
                         }
                     }
                     item.setState(true);
                 }
             });
             if(drawModes.get(i) == this.tracks.get(0).getDrawMode()){
                 item.setState(true);
             }
             visItems.add(item);
             menu.add(item);
         }
         return menu;
     }
 
     /**
      * Add a track to the list of tracks in
      * this frame
      * @param track The track to add
      * @param renderer to add for this track; if null, add default renderer for data type
      */
     public void addTrack(ViewTrack track, TrackRenderer renderer) {
         tracks.add(track);
 
         if (renderer == null) {
             switch(track.getDataType()) {
                 case POINT_GENERIC:
                     renderer = new PointTrackRenderer();
                     break;
                 case INTERVAL_GENERIC:
                     renderer = new IntervalTrackRenderer();
                     break;
                 case CONTINUOUS_GENERIC:
                     renderer = new ContinuousTrackRenderer();
                     break;
                 case INTERVAL_BAM:
                     renderer = new BAMTrackRenderer();
                     break;
                 case INTERVAL_BED:
                     renderer = new BEDTrackRenderer();
                     break;
                 case SEQUENCE_FASTA:
                     renderer = new SequenceTrackRenderer();
                     break;
             }
         }
         renderer.setURI(track.getURI());
         renderer.getDrawingInstructions().addInstruction(
                 DrawingInstructions.InstructionName.TRACK_DATA_TYPE, track.getDataType());
         track.addTrackRenderer(renderer);
         GraphPane graphPane = getGraphPane();
         graphPane.addTrackRenderer(renderer);
         graphPane.addTrack(track);
     }
 
     public void addTrack(ViewTrack track) {
         addTrack(track, null);
     }
 
     public void redrawTracksInRange() throws Exception {
         drawTracksInRange(ReferenceController.getInstance().getReferenceName(), currentRange);
     }
 
     /**
      * // TODO: comment
      * @param range
      */
     public void drawTracksInRange(String reference, Range range)
     {
         if (!isLocked()) { currentRange = range; }
         if (this.graphPane.isLocked()) { return; }
 
         if (this.tracks.size() > 0) {
 
             this.graphPane.setXRange(currentRange);
 
             try {
 
                 for (ViewTrack track : tracks) {
                     track.prepareForRendering(reference, range);
                 }
                 this.graphPane.repaint();
 
             } catch (Throwable throwable) {
                 throwable.printStackTrace();
                 JOptionPane.showMessageDialog(graphPane, throwable.getMessage());
             }
         }
         this.resetLayers();
     }
 
     private GraphPane getNewZedGraphControl() {
         GraphPane zgc = new GraphPane(this);
 
         // TODO: set properties
 
         return zgc;
     }
 
     // TODO: what is locking for?
     public void lockRange(Range r) { setLocked(true, r); }
     public void unlockRange() { setLocked(false, null); }
     public void setLocked(boolean b, Range r) {
         this.isLocked = b;
         this.currentRange = r;
     }
 
     public boolean isLocked() { return this.isLocked; }
 
     private void initGraph() {
         graphPane = getNewZedGraphControl();
         graphPane.setBackground(ColourSettings.colorFrameBackground);
     }
 
     // FIXME: this is a horrible kludge
     public void drawModeChanged(DrawModeChangedEvent evt) {
 
         ViewTrack viewTrack = evt.getViewTrack();
 
 //        if (getTracks().contains(viewTrack)) {
         boolean reRender = true;
         if (viewTrack.getDataType() == FileFormat.INTERVAL_BAM) {
             if (evt.getMode().getName().equals("MATE_PAIRS")) {
                 reRender = true;
                 setCoverageEnabled(false);
                 this.arcButton.setVisible(true);
                 this.intervalButton.setVisible(false);
             }else {
                 if(evt.getMode().getName().equals("STANDARD") || evt.getMode().getName().equals("VARIANTS")){
                     this.intervalButton.setVisible(true);
                 } else {
                     this.intervalButton.setVisible(false);
                 }
                 setCoverageEnabled(true);
                 reRender = true;
                 this.arcButton.setVisible(false);
             }
         }
         if (reRender) {
             drawTracksInRange(ReferenceController.getInstance().getReferenceName(), RangeController.getInstance().getRange());
         }
 //        }
     }
 
     private void setCoverageEnabled(boolean enabled) {
 
         for (ViewTrack track: getTracks()) {
             if (track instanceof BAMCoverageViewTrack) {
                 ((BAMCoverageViewTrack) track).setEnabled(enabled);
             }
         }
     }
 
     /**
      * Create a new panel to draw on.
      */
     public JPanel getLayerToDraw(){
         JPanel p = new JPanel();
         p.setOpaque(false);
         GridBagConstraints c = new GridBagConstraints();
         c.fill = GridBagConstraints.BOTH;
         c.weightx = 1.0;
         c.weighty = 1.0;
         c.gridx = 0;
         c.gridy = 0;
         c.gridwidth = 3;
         c.gridheight = 2;
         jlp.add(p,c,2);
         jlp.setLayer(p, 50);
         return p;
     }
 
     /**
      * Export this frame as an image.
      */
     public BufferedImage frameToImage(){
         BufferedImage bufferedImage = new BufferedImage(getGraphPane().getWidth(), getGraphPane().getHeight(), BufferedImage.TYPE_INT_RGB);
         Graphics2D g = bufferedImage.createGraphics();
         this.getGraphPane().render(g);
         g.setColor(Color.black);
         g.setFont(new Font(null, Font.BOLD, 13));
         g.drawString(this.getTracks().get(0).getName(), 2, 15);
         return bufferedImage;
     }
 
     /**
      * Give reference to DockableFrame container.
      */
     public void setDockableFrame(DockableFrame df){
         this.parent = df;
     }
 
     public String getName(){
         return this.name;
     }
 }
