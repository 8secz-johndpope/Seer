 /**
  * DataCollectionForm.java
  * 
  * This form hold datasets containing results of gaussian fitting
  * Two types of data sets exists: tracks and "global" spotData
  * 
  * Data structure used internally is contained in "MyRowData".
  * Data are currently stored in RAM, but a caching mechanism could be implemented
  * 
  * The form acts as a "workbench".  Various actions, (such as display, color correction
  * jitter correction) are available, some of which may generate new datasets
  * that are stored in this form
  * 
  *
  * Created on Nov 20, 2010, 8:52:50 AM
  */
 
 package edu.valelab.GaussianFit;
 
 import ij.ImagePlus;
 import ij.WindowManager;
 import ij.gui.ImageWindow;
 import ij.measure.ResultsTable;
 import ij.process.ImageProcessor;
 import ij.text.TextPanel;
 import ij.text.TextWindow;
 import java.awt.Component;
 import java.awt.Frame;
 import java.awt.Point;
 import java.awt.event.KeyListener;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.JButton;
 import javax.swing.JTable;
 import javax.swing.UIManager;
 import javax.swing.table.AbstractTableModel;
 import javax.swing.table.TableCellRenderer;
 
 import edu.ucsf.tsf.TaggedSpotsProtos.IntensityUnits;
 import edu.ucsf.tsf.TaggedSpotsProtos.LocationUnits;
 import edu.ucsf.tsf.TaggedSpotsProtos.FitMode;
 import edu.ucsf.tsf.TaggedSpotsProtos.SpotList;
 import edu.ucsf.tsf.TaggedSpotsProtos.Spot;
 
 import ij.gui.StackWindow;
 import ij.gui.YesNoCancelDialog;
 import ij.process.ByteProcessor;
 import ij.process.ShortProcessor;
 import java.awt.Cursor;
 import java.awt.Dimension;
 import java.awt.FileDialog;
 import java.awt.geom.Point2D;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.nio.channels.FileChannel;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.prefs.Preferences;
 import javax.swing.JOptionPane;
 
 import org.jfree.data.xy.XYSeries;
 
 import org.micromanager.MMStudioMainFrame;
 import valelab.LocalWeightedMean;
 
 /**
  *
  * @author Nico Stuurman
  */
 public class DataCollectionForm extends javax.swing.JFrame {
    AbstractTableModel myTableModel_;
    private final String[] columnNames_ = {"Image", "Nr of spots", "2C Reference", "Plot/Render", "Action"};
    private final String[] plotModes_ = {"t-X", "t-Y", "X-Y"};
    private final String[] renderModes_ = {"Points", "Gaussian"};
    private final String[] renderSizes_  = {"1x", "2x", "4x", "8x"};
    public final static String extension_ = ".tsf";
    // TODO: make this user-settable
    private final double MAXMATCHDISTANCE = 1000.0;
    
    // Prefs
    private static final String FRAMEXPOS = "DCXPos";
    private static final String FRAMEYPOS = "DCYPos";
    private static final String FRAMEWIDTH = "DCWidth";
    private static final String FRAMEHEIGHT = "DCHeight";
    private static final String USESIGMA = "DCSigma";
    private static final String SIGMAMIN = "DCSigmaMin";
    private static final String SIGMAMAX = "DCSigmaMax";
    private static final String USEINT = "DCIntensity";
    private static final String INTMIN = "DCIntMin";
    private static final String INTMAX = "DCIntMax";
    
    ArrayList<MyRowData> rowData_;
    double[][][] colorCorrection_; // 2D array (one for each pixel) containing xy coordinates of
                                  // first image (0 and 1) and correction to second image (2 and 3)
    public static DataCollectionForm instance_ = null;
    private static LocalWeightedMean lwm_;
    
    Preferences prefs_;
 
 
    /**
     * Data structure for spotlists
     */
    public class MyRowData {
       public final List<GaussianSpotData> spotList_;
       public final ArrayList<Double> timePoints_;
       public final String name_;
       public final String title_;
       public final String colCorrRef_;
       public final int width_;
       public final int height_;
       public final float pixelSizeNm_;
       public final int shape_;
       public final int halfSize_;
       public final int nrChannels_;
       public final int nrFrames_;
       public final int nrSlices_;
       public final int nrPositions_;
       public final int maxNrSpots_;
       public final boolean isTrack_;
 
 
       public MyRowData(String name,
               String title,
               String colCorrRef,
               int width,
               int height,
               float pixelSizeUm,
               int shape,
               int halfSize, 
               int nrChannels,
               int nrFrames,
               int nrSlices,
               int nrPositions,
               int maxNrSpots, 
               List<GaussianSpotData> spotList,
               ArrayList<Double> timePoints,
               boolean isTrack) {
          name_ = name;
          title_ = title;
          colCorrRef_ = colCorrRef;
          width_ = width;
          height_ = height;
          pixelSizeNm_ = pixelSizeUm;
          spotList_ = spotList;
          shape_ = shape;
          halfSize_ = halfSize;
          nrChannels_ = nrChannels;
          nrFrames_ = nrFrames;
          nrSlices_ = nrSlices;
          nrPositions_ = nrPositions;
          maxNrSpots_ = maxNrSpots;
          timePoints_ = timePoints;
          isTrack_ = isTrack;
       }
    }
 
    /**
     * Implement this class as a singleton
     *
     * @return the form
     */
    public static DataCollectionForm getInstance() {
       if (instance_ == null)
          instance_ =  new DataCollectionForm();
       return instance_;
    }
 
     /** 
      * Creates new form DataCollectionForm
      */
    private DataCollectionForm() {
 
       rowData_ = new ArrayList<MyRowData>();
 
       myTableModel_ = new AbstractTableModel() {
          @Override
           public String getColumnName(int col) {
               return columnNames_[col].toString();
           }
           public int getRowCount() {
              if (rowData_ == null)
                 return 0;
              return rowData_.size();
           }
           public int getColumnCount() { 
              return columnNames_.length;
           }
           public Object getValueAt(int row, int col) {
              if (col == 0 && rowData_ != null)
                 return rowData_.get(row).name_;
              else if (col == 1)
                 return rowData_.get(row).spotList_.size();
              else if (col == 2)
                 return rowData_.get(row).colCorrRef_;
              else
                 return getColumnName(col);
           }
          @Override
           public boolean isCellEditable(int row, int col) {
             return false;
           }
          @Override
           public void setValueAt(Object value, int row, int col) {
               fireTableCellUpdated(row, col);
           }
        };
 
        initComponents();
        referenceName_.setText("");
        jTable1_.addMouseListener(new LocalMouseListener());
        jTable1_.getColumn("Plot/Render").setCellRenderer(new ButtonRenderer());
        jTable1_.getColumn("Action").setCellRenderer(new ButtonRenderer());
        plotComboBox_.setModel(new javax.swing.DefaultComboBoxModel(plotModes_));
        visualizationModel_.setModel(new javax.swing.DefaultComboBoxModel(renderModes_));
        visualizationMagnification_.setModel(new javax.swing.DefaultComboBoxModel(renderSizes_));
        jScrollPane1.setName("Gaussian Spot Fitting Data Sets");
        
        setBackground(MMStudioMainFrame.getInstance().getBackgroundColor());
        MMStudioMainFrame.getInstance().addMMBackgroundListener(this);
        
        if (prefs_ == null)
           prefs_ = Preferences.userNodeForPackage(this.getClass());
        setBounds(prefs_.getInt(FRAMEXPOS, 50), prefs_.getInt(FRAMEYPOS, 100),
              prefs_.getInt(FRAMEWIDTH, 800), prefs_.getInt(FRAMEHEIGHT, 250));
        filterSigmaCheckBox_.setSelected(prefs_.getBoolean(USESIGMA, false));
        sigmaMin_.setText(prefs_.get(SIGMAMIN, "0.0"));
        sigmaMax_.setText(prefs_.get(SIGMAMAX, "20.0"));
        filterIntensityCheckBox_.setSelected(prefs_.getBoolean(USEINT, false));
        intensityMin_.setText(prefs_.get(INTMIN, "0.0"));
        intensityMax_.setText(prefs_.get(INTMAX, "20000"));
              
        setVisible(true);
    }
 
    /**
     * Use a Mouse listener instead of buttons, since buttons in a table are a pain
     */
    private class LocalMouseListener extends MouseAdapter {
       @Override
       public void mouseClicked(MouseEvent me) {
          Point click = new Point(me.getX(), me.getY());
          int column = jTable1_.columnAtPoint(click);
          int row = jTable1_.rowAtPoint(click);
          
          if (column == 3) {
             if (rowData_.get(row).isTrack_)
                plotData(rowData_.get(row), plotComboBox_.getSelectedIndex());
             else {
                int mag = 1 << visualizationMagnification_.getSelectedIndex();
                SpotDataFilter sf = new SpotDataFilter();
                if (filterSigmaCheckBox_.isSelected())
                   sf.setSigma(true, Double.parseDouble(sigmaMin_.getText()), 
                           Double.parseDouble(sigmaMax_.getText()) );
                if (filterIntensityCheckBox_.isSelected())
                   sf.setIntensity(true, Double.parseDouble(intensityMin_.getText()), 
                           Double.parseDouble(intensityMax_.getText()) );
                ImageRenderer.renderData(null, rowData_.get(row), visualizationModel_.getSelectedIndex(),
                        mag, null, sf);
             }
          }
          else if (column == 4) {
             if (rowData_.get(row).isTrack_)
                straightenTrack(rowData_.get(row));
          }
 
       }
    }
 
 
    /**
     * Adds a spot data set to the form
     *
     *
     * @param name
     * @param title
     * @param width
     * @param height
     * @param pixelSizeUm
     * @param shape
     * @param halfSize
     * @param nrChannels
     * @param nrFrames
     * @param nrSlices
     * @param nrPositions
     * @param maxNrSpots
     * @param spotList
     * @param isTrack
     */
    public void addSpotData(
            String name,
            String title,
            String colCorrRef,
            int width,
            int height,
            float pixelSizeUm,
            int shape,
            int halfSize,
            int nrChannels,
            int nrFrames,
            int nrSlices,
            int nrPositions,
            int maxNrSpots, 
            List<GaussianSpotData> spotList,
            ArrayList<Double> timePoints,
            boolean isTrack) {
       MyRowData newRow = new MyRowData(name, title, colCorrRef, width, height, pixelSizeUm, 
               shape, halfSize, nrChannels, nrFrames, nrSlices, nrPositions, 
               maxNrSpots, spotList, timePoints, isTrack);
       rowData_.add(newRow);
       myTableModel_.fireTableRowsInserted(rowData_.size()-1, rowData_.size());
    }
 
 
     /**
      * This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     @SuppressWarnings("unchecked")
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         jScrollPane1 = new javax.swing.JScrollPane();
         jTable1_ = new javax.swing.JTable();
         loadButton = new javax.swing.JButton();
         plotComboBox_ = new javax.swing.JComboBox();
         visualizationMagnification_ = new javax.swing.JComboBox();
         visualizationModel_ = new javax.swing.JComboBox();
         saveButton = new javax.swing.JButton();
         removeButton = new javax.swing.JButton();
         showButton = new javax.swing.JButton();
         c2StandardButton = new javax.swing.JButton();
         pairsButton = new javax.swing.JButton();
         c2CorrectButton = new javax.swing.JButton();
         referenceName_ = new javax.swing.JLabel();
         unjitterButton_ = new javax.swing.JButton();
         jLabel1 = new javax.swing.JLabel();
         filterSigmaCheckBox_ = new javax.swing.JCheckBox();
         filterIntensityCheckBox_ = new javax.swing.JCheckBox();
         sigmaMin_ = new javax.swing.JTextField();
         intensityMin_ = new javax.swing.JTextField();
         sigmaMax_ = new javax.swing.JTextField();
         intensityMax_ = new javax.swing.JTextField();
         jLabel2 = new javax.swing.JLabel();
         jLabel3 = new javax.swing.JLabel();
 
         setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
         setTitle("Gaussian tracking data");
         setMinimumSize(new java.awt.Dimension(450, 80));
         addWindowListener(new java.awt.event.WindowAdapter() {
             public void windowClosing(java.awt.event.WindowEvent evt) {
                 formWindowClosing(evt);
             }
         });
         addComponentListener(new java.awt.event.ComponentAdapter() {
             public void componentResized(java.awt.event.ComponentEvent evt) {
                 formComponentResized(evt);
             }
         });
 
         jTable1_.setModel(myTableModel_);
         jScrollPane1.setViewportView(jTable1_);
 
         loadButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         loadButton.setText("Load");
         loadButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 loadButtonActionPerformed(evt);
             }
         });
 
         plotComboBox_.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         plotComboBox_.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "X-T", "Y-T", "X-Y" }));
 
         visualizationMagnification_.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         visualizationMagnification_.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1x", "2x", "4x", "8x" }));
 
         visualizationModel_.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         visualizationModel_.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Gaussian" }));
 
         saveButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         saveButton.setText("Save");
         saveButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 saveButtonActionPerformed(evt);
             }
         });
 
         removeButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         removeButton.setText("Remove");
         removeButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 removeButtonActionPerformed(evt);
             }
         });
 
         showButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         showButton.setText("Show");
         showButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 showButtonActionPerformed(evt);
             }
         });
 
         c2StandardButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         c2StandardButton.setText("2C Reference");
         c2StandardButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 c2StandardButtonActionPerformed(evt);
             }
         });
 
         pairsButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         pairsButton.setText("Pairs");
         pairsButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 pairsButtonActionPerformed(evt);
             }
         });
 
         c2CorrectButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         c2CorrectButton.setText("2C Correct");
         c2CorrectButton.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 c2CorrectButtonActionPerformed(evt);
             }
         });
 
         referenceName_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         referenceName_.setText("jLabel1");
 
         unjitterButton_.setFont(new java.awt.Font("Lucida Grande", 0, 10));
         unjitterButton_.setText("UnJitter");
         unjitterButton_.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 unjitterButton_ActionPerformed(evt);
             }
         });
 
         jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         jLabel1.setText("Filters:");
 
         filterSigmaCheckBox_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         filterSigmaCheckBox_.setText("Sigma");
         filterSigmaCheckBox_.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 filterSigmaCheckBox_ActionPerformed(evt);
             }
         });
 
         filterIntensityCheckBox_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         filterIntensityCheckBox_.setText("Intensity");
         filterIntensityCheckBox_.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 filterIntensityCheckBox_ActionPerformed(evt);
             }
         });
 
         sigmaMin_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         sigmaMin_.setText("0");
 
         intensityMin_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         intensityMin_.setText("0");
 
         sigmaMax_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         sigmaMax_.setText("0");
 
         intensityMax_.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         intensityMax_.setText("0");
 
         jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         jLabel2.setText("< spot <");
 
         jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 11));
         jLabel3.setText("< spot <");
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 814, Short.MAX_VALUE)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                 .addContainerGap()
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                     .add(c2CorrectButton, 0, 0, Short.MAX_VALUE)
                     .add(c2StandardButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, Short.MAX_VALUE)
                     .add(org.jdesktop.layout.GroupLayout.TRAILING, loadButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(layout.createSequentialGroup()
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                         .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                             .add(layout.createSequentialGroup()
                                 .add(saveButton)
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(removeButton)
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(showButton)
                                 .add(165, 165, 165)
                                 .add(visualizationModel_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(visualizationMagnification_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(plotComboBox_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                             .add(layout.createSequentialGroup()
                                 .add(referenceName_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 234, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                 .add(180, 180, 180)
                                 .add(jLabel1)
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                     .add(layout.createSequentialGroup()
                                         .add(filterIntensityCheckBox_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                         .add(intensityMin_))
                                     .add(layout.createSequentialGroup()
                                         .add(filterSigmaCheckBox_)
                                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                         .add(sigmaMin_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 46, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 12, Short.MAX_VALUE)
                                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                     .add(jLabel2)
                                     .add(jLabel3))
                                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                     .add(sigmaMax_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 46, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                     .add(intensityMax_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 46, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                     .add(layout.createSequentialGroup()
                         .add(93, 93, 93)
                         .add(pairsButton)
                         .add(89, 89, 89)
                         .add(unjitterButton_)))
                 .add(20, 20, 20))
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                 .addContainerGap()
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(loadButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(removeButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(saveButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(showButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(visualizationMagnification_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(visualizationModel_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(plotComboBox_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                     .add(c2StandardButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                     .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                         .add(referenceName_)
                         .add(jLabel1)
                         .add(filterSigmaCheckBox_)
                         .add(sigmaMin_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(sigmaMax_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(jLabel3)))
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                         .add(c2CorrectButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(pairsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(unjitterButton_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                     .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                         .add(filterIntensityCheckBox_)
                         .add(intensityMin_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(intensityMax_, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .add(jLabel2)))
                 .add(8, 8, 8)
                 .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 401, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     /**
      * Loads data saved in TSF format (Tagged Spot File Format)
      *
      * @evt
      */
     private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
       
       FileDialog fd = new FileDialog(this, "Load Spot Data", FileDialog.LOAD);
       fd.setVisible(true);
       String selectedItem = fd.getFile();
       if (selectedItem == null) {
          return;
       } else {
          final File selectedFile = new File( fd.getDirectory() + File.separator +
 		        fd.getFile());
          
           Runnable doWorkRunnable = new Runnable() {
 
              public void run() {
 
                 SpotList psl = null;
                 try {
                    
                    ij.IJ.showStatus("Loading data..");
 
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
 
                    FileInputStream fi = new FileInputStream(selectedFile);
                    DataInputStream di = new DataInputStream(fi);
                    
                    // the new file format has an initial 0, then the offset (in long)
                    // to the position of spotList
                    int magic = di.readInt();
                    if (magic != 0) {
                       // reset and mark do not seem to work on my computer
                       fi.close();
                       fi = new FileInputStream(selectedFile);
                       psl = SpotList.parseDelimitedFrom(fi);
                    } else {
                       // TODO: evaluate after creating code writing this formt
                       long offset = di.readLong();
                       fi.skip(offset);
                       psl = SpotList.parseDelimitedFrom(fi);
                       fi.close();
                       fi = new FileInputStream(selectedFile);
                       fi.skip(12); // size of int + size of long
                    }
                    
                    
                    String name = psl.getName();
                    String title = psl.getName();
                    int width = psl.getNrPixelsX();
                    int height = psl.getNrPixelsY();
                    float pixelSizeUm = psl.getPixelSize();
                    int shape = 1;
                    if (psl.getFitMode() == FitMode.TWOAXIS) {
                       shape = 2;
                    } else if (psl.getFitMode() == FitMode.TWOAXISANDTHETA) {
                       shape = 3;
                    }
                    int halfSize = psl.getBoxSize() / 2;
                    int nrChannels = psl.getNrChannels();
                    int nrFrames = psl.getNrFrames();
                    int nrSlices = psl.getNrSlices();
                    int nrPositions = psl.getNrPos();
                    boolean isTrack = psl.getIsTrack();
                    long expectedSpots = psl.getNrSpots();
                    long esf = expectedSpots / 100;
                    long maxNrSpots = 0;
 
 
                    ArrayList<GaussianSpotData> spotList = new ArrayList<GaussianSpotData>();
                    Spot pSpot;
                    while (fi.available() > 0 && (expectedSpots == 0 || maxNrSpots < expectedSpots) ) {
                       
                       pSpot = Spot.parseDelimitedFrom(fi);
 
                       GaussianSpotData gSpot = new GaussianSpotData((ImageProcessor) null, pSpot.getChannel(),
                               pSpot.getSlice(), pSpot.getFrame(), pSpot.getPos(),
                               pSpot.getMolecule(), pSpot.getXPosition(), pSpot.getYPosition());
                       gSpot.setData(pSpot.getIntensity(), pSpot.getBackground(), pSpot.getX(),
                               pSpot.getY(), pSpot.getWidth(), pSpot.getA(), pSpot.getTheta(),
                               pSpot.getXPrecision());
                       maxNrSpots++;
                       if ( (esf > 0) &&  ( ( maxNrSpots % esf) == 0) )
                          ij.IJ.showProgress((double)maxNrSpots / (double) expectedSpots);
 
                       spotList.add(gSpot);
                    }
 
                    addSpotData(name, title, "", width, height, pixelSizeUm, shape, halfSize,
                            nrChannels, nrFrames, nrSlices, nrPositions, (int) maxNrSpots,
                            spotList, null, isTrack);
 
                 } catch (FileNotFoundException ex) {
                    ij.IJ.error("File not found");
                 } catch (IOException ex) {
                    ij.IJ.error("Error while reading file");
                 } finally {
                    setCursor(Cursor.getDefaultCursor());
                    ij.IJ.showStatus("");
                    ij.IJ.showProgress(1.0);
                 }
              }
           };
           
           (new Thread(doWorkRunnable)).start();
        }
           
     }//GEN-LAST:event_loadButtonActionPerformed
 
     private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        int row = jTable1_.getSelectedRow();
        if (row > -1)
          saveData(rowData_.get(row));
        else
           JOptionPane.showMessageDialog(getInstance(), "Please select a dataset to save");
     }//GEN-LAST:event_saveButtonActionPerformed
 
     private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int rows[] = jTable1_.getSelectedRows();
        if (rows.length > 0) {
           for (int row = rows.length -1; row >= 0; row--) {
              rowData_.remove(rows[row]);
              myTableModel_.fireTableRowsDeleted(rows[row], rows[row]);
           }
        } else
          JOptionPane.showMessageDialog(getInstance(), "No dataset selected");
     }//GEN-LAST:event_removeButtonActionPerformed
 
     private void showButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showButtonActionPerformed
        int row = jTable1_.getSelectedRow();
        if (row > -1)
          showResults(rowData_.get(row));
        else
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset to show");
     }//GEN-LAST:event_showButtonActionPerformed
 
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
       //jScrollPane1.setSize(this.getSize());
       Dimension d = getSize();
       d.height -= 60;
       jScrollPane1.setSize(d);
       jScrollPane1.getViewport().setViewSize(d);
    }//GEN-LAST:event_formComponentResized
 
    /**
     * Use the selected data set as the reference for 2-channel color correction
     * @param evt 
     */
    private void c2StandardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_c2StandardButtonActionPerformed
       int row = jTable1_.getSelectedRow();
       if (row < 0)
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset as color reference");
       else {
          // Get points from both channels in first frame as ArrayLists        
          ArrayList<Point2D.Double> xyPointsCh1 = new ArrayList<Point2D.Double>();
          ArrayList<Point2D.Double> xyPointsCh2 = new ArrayList<Point2D.Double>();
          Iterator it = rowData_.get(row).spotList_.iterator();
          while (it.hasNext()) {
             GaussianSpotData gs = (GaussianSpotData) it.next();
             if (gs.getFrame() == 1) {
                Point2D.Double point = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                if (gs.getChannel() == 1)
                   xyPointsCh1.add(point);
                else if (gs.getChannel() == 2)
                   xyPointsCh2.add(point);
             }
          }
          
          if (xyPointsCh2.size() == 0) {
             JOptionPane.showMessageDialog(getInstance(), "No points found in second channel.  Is this a dual channel dataset?");
             return;
          }
          
          
          // Find matching points in the two ArrayLists
          Iterator it2 = xyPointsCh1.iterator();
          Map points = new HashMap<Point2D.Double,Point2D.Double>();
          NearestPoint2D np = new NearestPoint2D(xyPointsCh2, MAXMATCHDISTANCE);
          
          while (it2.hasNext()) {
             Point2D.Double pCh1 = (Point2D.Double) it2.next();
             Point2D.Double pCh2 = np.findKDWSE(pCh1);
             if (pCh2 != null) {
               points.put(pCh1, pch@);
             }
          }
          lwm_ = new LocalWeightedMean(2, points);
          referenceName_.setText(rowData_.get(row).name_);
          
       }
    }//GEN-LAST:event_c2StandardButtonActionPerformed
 
    
    /**
     * Cycles through the spots of the selected data set and finds the most nearby 
     * spot in channel 2.  It will list this as a pair if the two spots are within
     * MAXMATCHDISTANCE nm of each other.  
     * In addition, it will list the  average distance, and average distance
     * in x and y for each frame.
     * 
     * spots in channel 2
     * that are within MAXMATCHDISTANCE of 
     * 
     * @param evt 
     */
    private void pairsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pairsButtonActionPerformed
       final int row = jTable1_.getSelectedRow();
       if (row < 0) {
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset for the Pair function");
 
          return;
       }
       
       if (row > -1) {
 
          Runnable doWorkRunnable = new Runnable() {
 
             public void run() {
                ResultsTable rt = new ResultsTable();
                rt.reset();
                rt.setPrecision(2);
                ResultsTable rt2 = new ResultsTable();
                rt2.reset();
                rt2.setPrecision(2);
                int width = rowData_.get(row).width_;
                int height = rowData_.get(row).height_;
                double factor = rowData_.get(row).pixelSizeNm_;
                ij.ImageStack  stack = new ij.ImageStack(width, height); 
                
                ImagePlus sp = new ImagePlus("Errors in pairs");
                
                XYSeries xData = new XYSeries("XError");
                XYSeries yData = new XYSeries("YError");
     
                             
                ij.IJ.showStatus("Creating Pairs...");
                
  
                for (int frame = 1; frame <= rowData_.get(row).nrFrames_; frame++) {
                   ij.IJ.showProgress(frame, rowData_.get(row).nrFrames_);
                   ImageProcessor ip = new ShortProcessor(width, height);
                   short pixels[] = new short[width * height];
                   ip.setPixels(pixels);
                   
                   // Get points from both channels in each frame as ArrayLists        
                   ArrayList<Point2D.Double> xyPointsCh1 = new ArrayList<Point2D.Double>();
                   ArrayList<Point2D.Double> xyPointsCh2 = new ArrayList<Point2D.Double>();
                   Iterator it = rowData_.get(row).spotList_.iterator();
                   while (it.hasNext()) {
                      GaussianSpotData gs = (GaussianSpotData) it.next();
                      if (gs.getFrame() == frame) {
                         Point2D.Double point = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                         if (gs.getChannel() == 1) {
                            xyPointsCh1.add(point);
                         } else if (gs.getChannel() == 2) {
                            xyPointsCh2.add(point);
                         }
                      }
                   }
                   
                   if (xyPointsCh2.size() == 0) {
                      JOptionPane.showMessageDialog(getInstance(), "No points found in second channel.  Is this a 2-channel dataset?");
                      return;
                   }
                      
 
                   // Find matching points in the two ArrayLists
                   Iterator it2 = xyPointsCh1.iterator();
                   NearestPoint2D np = new NearestPoint2D(xyPointsCh2, MAXMATCHDISTANCE);
                   
                   
                   ArrayList<Double> distances = new ArrayList<Double>();
                   ArrayList<Double> errorX = new ArrayList<Double>();
                   ArrayList<Double> errorY = new ArrayList<Double>();
 
                   while (it2.hasNext()) {
                      Point2D.Double pCh1 = (Point2D.Double) it2.next();
                      Point2D.Double pCh2 = np.findKDWSE(pCh1); 
                      if (pCh2 != null) {
                         rt.incrementCounter();
                         rt.addValue("X1", pCh1.getX());
                         rt.addValue("Y1", pCh1.getY());
                         rt.addValue("X2", pCh2.getX());
                         rt.addValue("Y2", pCh2.getY());
                         double d2 = NearestPoint2D.distance2(pCh1, pCh2);
                         double d = Math.sqrt(d2);
                         rt.addValue("Distance", d);
                         distances.add(d);
                         
                         ip.putPixel((int) (pCh1.x / factor), (int) (pCh1.y / factor), (int) d);
 
                         double ex = pCh2.getX() - pCh1.getX();
                         //double ex = (pCh1.getX() - pCh2.getX()) * (pCh1.getX() - pCh2.getX());
                         //ex = Math.sqrt(ex);
                         errorX.add(ex);
                         //double ey = (pCh1.getY() - pCh2.getY()) * (pCh1.getY() - pCh2.getY());
                         //ey = Math.sqrt(ey);
                         double ey = pCh2.getY() - pCh1.getY();
                         errorY.add(ey);
 
                      }
                   }
 
 
                   Double avg = listAvg(distances);
                   Double stdDev = listStdDev(distances, avg);
 
                   Double avgX = listAvg(errorX);
                   Double stdDevX = listStdDev(errorX, avgX);
                   Double avgY = listAvg(errorY);
                   Double stdDevY = listStdDev(errorY, avgY);
 
                   rt2.incrementCounter();
                   rt2.addValue("Frame Nr.", frame);
                   rt2.addValue("Avg. distance", avg);
                   rt2.addValue("StdDev distance", stdDev);
                   rt2.addValue("X", avgX);
                   rt2.addValue("StdDev X", stdDevX);
                   rt2.addValue("Y", avgY);
                   rt2.addValue("StdDevY", stdDevY);  
                   
                   stack.addSlice("frame: " + frame, ip);
                   
                   double timePoint = frame;
                   if (rowData_.get(row).timePoints_ != null) {
                      timePoint = rowData_.get(row).timePoints_.get(frame);
                   }
                   xData.add(timePoint, avgX);
                   yData.add(timePoint, avgY);
 
                }
                              
                rt2.show("Summary of Pairs found in " + rowData_.get(row).name_);
                rt.show("Pairs found in " + rowData_.get(row).name_);
                
                String yAxis = "Time (frameNr)";
                if (rowData_.get(row).timePoints_ != null) {
                   yAxis = "Time (s)";
                }
                GaussianUtils.plotData2("Error", xData, yData, yAxis, "Error(nm)", 0, 400);
                
                ij.IJ.showStatus("");
                
                sp.setOpenAsHyperStack(true);
                sp.setStack(stack, 1, 1, rowData_.get(row).nrFrames_);
                sp.setDisplayRange(0, 20);
                //sp.setSlice(1);
                //sp.resetStack();
                
                ImageWindow w = new StackWindow(sp);
 
                w.setImage(sp);
                w.setVisible(true); 
                
             }
          };
          
          (new Thread(doWorkRunnable)).start();
          
       }
    }//GEN-LAST:event_pairsButtonActionPerformed
 
    private static double listAvg (ArrayList<Double> list) {
       double total = 0.0;
       Iterator it = list.iterator();
       while (it.hasNext()) {
          total += (Double) it.next();
       }
       
       return total / list.size();      
    }
    
    
    /**
     * Returns the Standard Deviation as sqrt( 1/(n-1) sum( square(value - avg)) )
     * Feeding in parameter avg is just increase performance
     * 
     * @param list ArrayList<Double> 
     * @param avg average of the list
     * @return standard deviation as defined above
     */
    private static double listStdDev (ArrayList<Double> list, double avg) {
       
       double errorsSquared = 0;
       Iterator it = list.iterator();
       while (it.hasNext()) {
          double error = (Double) it.next() - avg;
          errorsSquared += (error * error);
       }
       return Math.sqrt(errorsSquared / (list.size() - 1) ) ;
    }
    
    
    /**
     * Utility function to calculate 
     * @param list
     * @return 
     */
    private static double listStdDev (ArrayList<Double> list) {
       double avg = listAvg(list);
       
       return listStdDev(list, avg);
 
    }
    
    
    private void c2CorrectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_c2CorrectButtonActionPerformed
       int row = jTable1_.getSelectedRow();
       if (row > -1) {     
          correct2C(rowData_.get(row));
       } else
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset to color correct");
    }//GEN-LAST:event_c2CorrectButtonActionPerformed
 
    private void unjitterButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unjitterButton_ActionPerformed
       int row = jTable1_.getSelectedRow();
       if (row > -1) {     
          unJitter(rowData_.get(row));
       } else
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset to unjitter");
    }//GEN-LAST:event_unjitterButton_ActionPerformed
 
    private void filterSigmaCheckBox_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterSigmaCheckBox_ActionPerformed
       // TODO add your handling code here:
    }//GEN-LAST:event_filterSigmaCheckBox_ActionPerformed
 
    private void filterIntensityCheckBox_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterIntensityCheckBox_ActionPerformed
       // TODO add your handling code here:
    }//GEN-LAST:event_filterIntensityCheckBox_ActionPerformed
 
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        prefs_.putInt(FRAMEXPOS, getX());
        prefs_.putInt(FRAMEYPOS, getY());
        prefs_.putInt(FRAMEWIDTH, getWidth());
        prefs_.putInt(FRAMEHEIGHT, getHeight());
        
        prefs_.putBoolean(USESIGMA, filterSigmaCheckBox_.isSelected());
        prefs_.put(SIGMAMIN, sigmaMin_.getText());
        prefs_.put(SIGMAMAX, sigmaMax_.getText());
        prefs_.putBoolean(USEINT, filterIntensityCheckBox_.isSelected());
        prefs_.put(INTMIN, intensityMin_.getText());
        prefs_.put(INTMAX, intensityMax_.getText());
        
        setVisible(false);
    }//GEN-LAST:event_formWindowClosing
 
 
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton c2CorrectButton;
     private javax.swing.JButton c2StandardButton;
     private javax.swing.JCheckBox filterIntensityCheckBox_;
     private javax.swing.JCheckBox filterSigmaCheckBox_;
     private javax.swing.JTextField intensityMax_;
     private javax.swing.JTextField intensityMin_;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JTable jTable1_;
     private javax.swing.JButton loadButton;
     private javax.swing.JButton pairsButton;
     private javax.swing.JComboBox plotComboBox_;
     private javax.swing.JLabel referenceName_;
     private javax.swing.JButton removeButton;
     private javax.swing.JButton saveButton;
     private javax.swing.JButton showButton;
     private javax.swing.JTextField sigmaMax_;
     private javax.swing.JTextField sigmaMin_;
     private javax.swing.JButton unjitterButton_;
     private javax.swing.JComboBox visualizationMagnification_;
     private javax.swing.JComboBox visualizationModel_;
     // End of variables declaration//GEN-END:variables
 
 
    /**
     * Renders button with appropriate names
     */
    class ButtonRenderer extends JButton implements TableCellRenderer {
 
       public ButtonRenderer() {
          setOpaque(true);
       }
 
       public Component getTableCellRendererComponent(JTable table, Object value,
           boolean isSelected, boolean hasFocus, int row, int column) {
 
          setForeground(table.getForeground());
          setBackground(UIManager.getColor("Button.background"));
 
          if (rowData_.get(row).isTrack_) {
             if (column == 3)
                setText((value == null ? "" : "Plot"));
             else {
                if (column == 4)
                   setText((value == null ? "" : "Straighten"));
                else
                   setText((value == null ? "" : value.toString()));
             }
          } else {
             if (column == 3)
                setText((value == null ? "" : "Render"));
             if (column == 4)
                return null;     
          }
              
          return this;
       }
    }
 
    /**
     * Shows dataset in ImageJ Results Table
     *
     * @rowData
     */
    private void showResults(MyRowData rowData) {
       // Copy data to results table
 
       ResultsTable rt = new ResultsTable();
       rt.reset();
       rt.setPrecision(1);
       int shape = rowData.shape_;
       for (GaussianSpotData gd : rowData.spotList_) {
          if (gd != null) {
             rt.incrementCounter();
             rt.addValue(Terms.FRAME, gd.getFrame());
             rt.addValue(Terms.SLICE, gd.getSlice());
             rt.addValue(Terms.CHANNEL, gd.getChannel());
             rt.addValue(Terms.POSITION, gd.getPosition());
             rt.addValue(Terms.INT, gd.getIntensity());
             rt.addValue(Terms.BACKGROUND, gd.getBackground());
             rt.addValue(Terms.XNM, gd.getXCenter());
             rt.addValue(Terms.YNM, gd.getYCenter());
             rt.addValue(Terms.SIGMA, gd.getSigma());
             if (shape >= 1) {
                rt.addValue(Terms.WIDTH, gd.getWidth());
             }
             if (shape >= 2) {
                rt.addValue(Terms.A, gd.getA());
             }
             if (shape == 3) {
                rt.addValue(Terms.THETA, gd.getTheta());
             }
             rt.addValue(Terms.XPIX, gd.getX());
             rt.addValue(Terms.YPIX, gd.getY());
          }
       }
       
       TextPanel tp;
       TextWindow win;
       
       String name = "Spots from: " + rowData.name_;
       rt.show(name);
       ImagePlus siPlus = ij.WindowManager.getImage(rowData.title_);
       // Attach listener to TextPanel
       Frame frame = WindowManager.getFrame(name);
       if (frame != null && frame instanceof TextWindow && siPlus != null) {
          win = (TextWindow) frame;
          tp = win.getTextPanel();
 
          // TODO: the following does not work, there is some voodoo going on here
          for (MouseListener ms : tp.getMouseListeners()) {
             tp.removeMouseListener(ms);
          }
          for (KeyListener ks : tp.getKeyListeners()) {
             tp.removeKeyListener(ks);
          }
          
          ResultsTableListener myk = new ResultsTableListener(siPlus, rt, win, rowData.halfSize_);
          tp.addKeyListener(myk);
          tp.addMouseListener(myk);
          frame.toFront();
       }
       
    }
 
    /**
     * Save data set in TSF (Tagged Spot File) format
     *
     * @rowData - row with spot data to be saved
     */
    private void saveData(final MyRowData rowData) {
       FileDialog fd = new FileDialog(this, "Save Spot Data", FileDialog.SAVE);
       fd.setVisible(true);
       String selectedItem = fd.getFile();
       if (selectedItem == null) {
          return;
       } else {
          String fn = fd.getFile();
          if (!fn.contains(".")) {
             fn = fn + extension_;
          }
          final File selectedFile = new File(fd.getDirectory() + File.separator + fn);
          if (selectedFile.exists()) {
             // this may be superfluous
             YesNoCancelDialog y = new YesNoCancelDialog(this, "File " + fn + "Exists...", "File exists.  Overwrite?");
             if (y.cancelPressed()) {
                return;
             }
             if (!y.yesPressed()) {
                saveData(rowData);
                return;
             }
          }
          
          
          
          Runnable doWorkRunnable = new Runnable() {
             
             public void run() {
                
                SpotList.Builder tspBuilder = SpotList.newBuilder();
                tspBuilder.setApplicationId(1).
                        setName(rowData.name_).
                        setFilepath(rowData.title_).
                        setNrPixelsX(rowData.width_).
                        setNrPixelsY(rowData.height_).
                        setNrSpots(rowData.spotList_.size()).
                        setPixelSize(160).
                        setBoxSize(rowData.halfSize_ * 2).
                        setNrChannels(rowData.nrChannels_).
                        setNrSlices(rowData.nrSlices_).
                        setIsTrack(rowData.isTrack_).
                        setNrPos(rowData.nrPositions_).
                        setNrFrames(rowData.nrFrames_).
                        setLocationUnits(LocationUnits.NM).
                        setIntensityUnits(IntensityUnits.PHOTONS).
                        setNrSpots(rowData.maxNrSpots_);
                switch (rowData.shape_) {
                   case (1):
                      tspBuilder.setFitMode(FitMode.ONEAXIS);
                      break;
                   case (2):
                      tspBuilder.setFitMode(FitMode.TWOAXIS);
                      break;
                   case (3):
                      tspBuilder.setFitMode(FitMode.TWOAXISANDTHETA);
                      break;
                }
                
                
                SpotList spotList = tspBuilder.build();
                try {
                   setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                   
                   FileOutputStream fo = new FileOutputStream(selectedFile);
                   // write space for magic nr and offset to spotList
                   for (int i = 0; i < 12; i++) {
                      fo.write(0);
                   }
                   
                   
                   
                   int counter = 0;
                   for (GaussianSpotData gd : rowData.spotList_) {
                      
                      if ((counter % 1000) == 0) {                        
                         ij.IJ.showStatus("Saving spotData...");
                         ij.IJ.showProgress(counter, rowData.spotList_.size());
                      }
                      
                      if (gd != null) {
                         Spot.Builder spotBuilder = Spot.newBuilder();
                         // TODO: precede all these calls with check for presence of member
                         // or be OK with default values?
                         spotBuilder.setMolecule(counter).
                                 setFrame(gd.getFrame()).
                                 setChannel(gd.getChannel()).
                                 setPos(gd.getPosition()).
                                 setSlice(gd.getSlice()).
                                 setX((float) gd.getXCenter()).
                                 setY((float) gd.getYCenter()).
                                 setIntensity((float) gd.getIntensity()).
                                 setBackground((float) gd.getBackground()).
                                 setXPosition(gd.getX()).
                                 setYPosition(gd.getY()).
                                 setWidth((float) gd.getWidth()).
                                 setA((float) gd.getA()).
                                 setTheta((float) gd.getTheta()).
                                 setXPrecision((float) gd.getSigma());
                         
                         double width = gd.getWidth();
                         double xPrec = gd.getSigma();
                         
                         Spot spot = spotBuilder.build();
                         // write message size and message
                         spot.writeDelimitedTo(fo);
                         counter++;
                      }
                   }
                   
                   FileChannel fc = fo.getChannel();
                   long offset = fc.position();
                   spotList.writeDelimitedTo(fo);
 
                   // now go back to write offset to the stream
                   fc.position(4);
                   DataOutputStream dos = new DataOutputStream(fo);
                   dos.writeLong(offset - 12);
                   
                   fo.close();
                   
                   ij.IJ.showProgress(1);
                   ij.IJ.showStatus("Finished saving spotData...");
                } catch (IOException ex) {
                   Logger.getLogger(DataCollectionForm.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                   setCursor(Cursor.getDefaultCursor());
                }
             }
          };
          
          (new Thread(doWorkRunnable)).start();
          
       }
       
    }
 
    /**
     * Calculates the axis of motion of a given dataset and normalizes the data
     * to that axis.
     *
     * @rowData
     */
    private void straightenTrack(MyRowData rowData) {
       
       if (rowData.spotList_.size() <= 1) {
          return;
       }
       
       ArrayList<Point2D.Double> xyPoints = new ArrayList<Point2D.Double>();
       Iterator it = rowData.spotList_.iterator();
       while (it.hasNext()) {
          GaussianSpotData gs = (GaussianSpotData) it.next();
          Point2D.Double point = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
          xyPoints.add(point);
       }
 
       // Calculate direction of travel and transform data set along this axis
       ArrayList<Point2D.Double> xyCorrPoints = GaussianUtils.pcaRotate(xyPoints);
       List<GaussianSpotData> transformedResultList =
               Collections.synchronizedList(new ArrayList<GaussianSpotData>());
       
       for (int i = 0; i < xyPoints.size(); i++) {
          GaussianSpotData oriSpot = rowData.spotList_.get(i);
          GaussianSpotData spot = new GaussianSpotData(oriSpot);
          spot.setData(oriSpot.getIntensity(), oriSpot.getBackground(),
                  xyCorrPoints.get(i).getX(), xyCorrPoints.get(i).getY(), oriSpot.getWidth(),
                  oriSpot.getA(), oriSpot.getTheta(), oriSpot.getSigma());
          transformedResultList.add(spot);
       }
 
       // Add transformed data to data overview window
       addSpotData(rowData.name_ + "Straightened", rowData.title_, "", rowData.width_,
               rowData.height_, rowData.pixelSizeNm_, rowData.shape_,
               rowData.halfSize_, rowData.nrChannels_, rowData.nrFrames_,
               rowData.nrSlices_, 1, rowData.maxNrSpots_, transformedResultList,
               rowData.timePoints_, true);
    }
 
    /**
     * Creates a new data set that is corrected for motion blur
     * Correction is performed by projecting a number of images onto a 
     * 2D scattergram and using cross-correlation between them to find
     * the displacement
     * 
     * @param rowData 
     */
    private void unJitter(final MyRowData rowData) {
       final int framesToCombine = 100;
       
       if (rowData.spotList_.size() <= 1) {
          return;
       }
       
       ij.IJ.showStatus("Executing jitter correction");
       
       Runnable doWorkRunnable = new Runnable() {
          
          public void run() {
             
             int mag = 1 << visualizationMagnification_.getSelectedIndex();
             
             
             int width = mag * rowData.width_;
             int height = mag * rowData.height_;
             int size = width * height;
 
             // TODO: what if we should go through nrSlices instead of nrFrames?
             boolean useSlices = false;
             int nrOfTests = rowData.nrFrames_ / framesToCombine;
             if (nrOfTests == 0) {
                useSlices = true;
                nrOfTests = rowData.nrSlices_ / framesToCombine;
                if (rowData.nrSlices_ % framesToCombine > 0) {
                   nrOfTests++;
                }
             } else {
                if (rowData.nrFrames_ % framesToCombine > 0) {
                   nrOfTests++;
                }
             }
 
             // storage of stage movement data
             class StageMovementData {
                
                Point2D.Double pos_;
                Point frameRange_;
                
                StageMovementData(Point2D.Double pos, Point frameRange) {
                   pos_ = pos;
                   frameRange_ = frameRange;
                }
             }
             ArrayList<StageMovementData> stagePos = new ArrayList<StageMovementData>();
             
             try {
                // make imageprocessors for all the images that we will generate
                ImageProcessor[] ip = new ImageProcessor[nrOfTests];
                byte[][] pixels = new byte[nrOfTests][width * height];
                
                for (int i = 0; i < nrOfTests; i++) {
                   ip[i] = new ByteProcessor(width, height);
                   ip[i].setPixels(pixels[i]);
                }
                
                double factor = (double) mag / rowData.pixelSizeNm_;
 
                // make 2D scattergrams of all pixelData
                for (GaussianSpotData spot : rowData.spotList_) {
                   int j = 0;
                   if (useSlices) {
                      j = (spot.getSlice() - 1) / framesToCombine;
                   } else {
                      j = (spot.getFrame() - 1) / framesToCombine;
                   }
                   int x = (int) (factor * spot.getXCenter());
                   int y = (int) (factor * spot.getYCenter());
                   int index = (y * width) + x;
                   if (index < size && index > 0) {
                      if (pixels[j][index] != -1) {
                         pixels[j][index] += 1;
                      }
                   }
                   
                }
                
                JitterDetector jd = new JitterDetector(ip[0]);
                
                Point2D.Double fp = new Point2D.Double(0.0, 0.0);
                Point2D.Double com = new Point2D.Double(0.0, 0.0);
                
                jd.getJitter(ip[0], fp);
                
                for (int i = 1; i < ip.length; i++) {
                   ij.IJ.showStatus("Executing jitter correction..." + i);
                   ij.IJ.showProgress(i, ip.length);
                   jd.getJitter(ip[i], com);
                   double x = (fp.x - com.x) / factor;
                   double y = (fp.y - com.y) / factor;
                   double timePoint = i;
                   if (rowData.timePoints_ != null) {
                      rowData.timePoints_.get(i);
                   }
                   stagePos.add(new StageMovementData(new Point2D.Double(x, y),
                           new Point(i * framesToCombine, ((i + 1) * framesToCombine - 1))));
                   System.out.println("X: " + x + " Y: " + y);
                }
                
             } catch (OutOfMemoryError ex) {
                // not enough memory to allocate all images in one go
                // we need to cycle through all gaussian spots cycle by cycle
 
                double factor = (double) mag / rowData.pixelSizeNm_;
                
                ImageProcessor ipRef = new ByteProcessor(width, height);
                byte[] pixelsRef = new byte[width * height];
                ipRef.setPixels(pixelsRef);
 
 
                // take the first image as reference
                for (GaussianSpotData spot : rowData.spotList_) {
                   int j = 0;
                   if (useSlices) {
                      j = (spot.getSlice() - 1) / framesToCombine;
                   } else {
                      j = (spot.getFrame() - 1) / framesToCombine;
                   }
                   if (j == 0) {
                      int x = (int) (factor * spot.getXCenter());
                      int y = (int) (factor * spot.getYCenter());
                      int index = (y * width) + x;
                      if (index < size && index > 0) {
                         if (pixelsRef[index] != -1) {
                            pixelsRef[index] += 1;
                         }
                      }
                   }
                }
                
                JitterDetector jd = new JitterDetector(ipRef);
                
                Point2D.Double fp = new Point2D.Double(0.0, 0.0);
                jd.getJitter(ipRef, fp);
                
                Point2D.Double com = new Point2D.Double(0.0, 0.0);
                ImageProcessor ipTest = new ByteProcessor(width, height);
                byte[] pixelsTest = new byte[width * height];
                ipTest.setPixels(pixelsTest);
                
                for (int i = 1; i < nrOfTests; i++) {
                   ij.IJ.showStatus("Executing jitter correction..." + i);
                   ij.IJ.showProgress(i, nrOfTests);
                   for (int p = 0; p < size; p++) {
                      ipTest.set(p, 0);
                   }
                   
                   for (GaussianSpotData spot : rowData.spotList_) {
                      int j = 0;
                      if (useSlices) {
                         j = (spot.getSlice() - 1) / framesToCombine;
                      } else {
                         j = (spot.getFrame() - 1) / framesToCombine;
                      }
                      if (j == i) {
                         int x = (int) (factor * spot.getXCenter());
                         int y = (int) (factor * spot.getYCenter());
                         int index = (y * width) + x;
                         if (index < size && index > 0) {
                            if (pixelsTest[index] != -1) {
                               pixelsTest[index] += 1;
                            }
                         }
                      }
                   }
                   
                   jd.getJitter(ipTest, com);
                   double x = (fp.x - com.x) / factor;
                   double y = (fp.y - com.y) / factor;
                   double timePoint = i;
                   if (rowData.timePoints_ != null) {
                      rowData.timePoints_.get(i);
                   }
                   stagePos.add(new StageMovementData(new Point2D.Double(x, y),
                           new Point(i * framesToCombine, ((i + 1) * framesToCombine - 1))));
                   System.out.println("X: " + x + " Y: " + y);
                }
                
             }
             
             try {
                // Assemble stage movement data into a track
                List<GaussianSpotData> stageMovementData = new ArrayList<GaussianSpotData>();
                GaussianSpotData sm = new GaussianSpotData(null, 1, 1, 1, 1, 1, 1, 1);
                sm.setData(0, 0, 0, 0, 0, 0, 0, 0);
                stageMovementData.add(sm);
                for (int i = 0; i < stagePos.size(); i++) {
                   StageMovementData smd = stagePos.get(i);
                   GaussianSpotData s =
                           new GaussianSpotData(null, 1, 1, i + 2, 1, 1, 1, 1);
                   s.setData(0, 0, smd.pos_.x, smd.pos_.y, 0, 0, 0, 0);                  
                   stageMovementData.add(s);
                }
 
                // Add stage movement data to overview window
                // First try to copy the time points
                ArrayList<Double> timePoints = null;
                if (rowData.timePoints_ != null) {
                   timePoints = new ArrayList<Double>();
                   int tp = framesToCombine;
                   while (tp < rowData.timePoints_.size()) {
                      timePoints.add(rowData.timePoints_.get(tp));
                      tp += framesToCombine;
                   }
                }
                
                MyRowData newRow = new MyRowData(rowData.name_ + "-Jitter", rowData.title_,
                        "", rowData.width_,
                        rowData.height_, rowData.pixelSizeNm_, rowData.shape_,
                        rowData.halfSize_, rowData.nrChannels_, stageMovementData.size(),
                        1, 1, stageMovementData.size(), stageMovementData,
                        timePoints, true);
                rowData_.add(newRow);
                
                myTableModel_.fireTableRowsInserted(rowData_.size() - 1, rowData_.size());
                
                
                
                
                ij.IJ.showStatus("Assembling jitter corrected dataset...");
                ij.IJ.showProgress(1);
                
                List<GaussianSpotData> correctedData = new ArrayList<GaussianSpotData>();
                Iterator it = rowData.spotList_.iterator();
                
                int testNr = 0;
                StageMovementData smd = stagePos.get(0);
                int counter = 0;
                while (it.hasNext()) {
                   counter++;
                   GaussianSpotData gs = (GaussianSpotData) it.next();
                   int test = 0;
                   if (useSlices) {
                      test = gs.getSlice();
                   } else {
                      test = gs.getFrame();
                   }
                   if (test != testNr) {
                      testNr = test - 1;
                   }
                   boolean found = false;
                   if (testNr >= smd.frameRange_.x && testNr <= smd.frameRange_.y) {
                      found = true;
                   }
                   if (!found) {
                      for (int i = 0; i < stagePos.size() && !found; i++) {
                         smd = stagePos.get(i);
                         if (testNr >= smd.frameRange_.x && testNr <= smd.frameRange_.y) {
                            found = true;
                         }
                      }
                   }
                   if (found) {
                      Point2D.Double point = new Point2D.Double(gs.getXCenter() - smd.pos_.x,
                              gs.getYCenter() - smd.pos_.y);
                      GaussianSpotData gsn = new GaussianSpotData(gs);
                      gsn.setXCenter(point.x);
                      gsn.setYCenter(point.y);
                      correctedData.add(gsn);
                   } else {
                      correctedData.add(gs);
                   }
                   
                   
                }
 
                // Add transformed data to data overview window
                addSpotData(rowData.name_ + "-Jitter-Correct", rowData.title_, "", rowData.width_,
                        rowData.height_, rowData.pixelSizeNm_, rowData.shape_,
                        rowData.halfSize_, rowData.nrChannels_, rowData.nrFrames_,
                        rowData.nrSlices_, 1, rowData.maxNrSpots_, correctedData,
                        null, false);
                ij.IJ.showStatus("Finished jitter correction");
             } catch (OutOfMemoryError oom) {
               System.gc();
               ij.IJ.error("Out of Memory");
             }
          }
       };
 
       (new Thread(doWorkRunnable)).start();
    }
    
    
    /**
     * Use the 2Channel calibration to create a new, corrected data set
     * 
     * @param rowData 
     */
    private void correct2C(final MyRowData rowData)
    {
       if (rowData.spotList_.size() <= 1) {
          JOptionPane.showMessageDialog(getInstance(), "Please select a dataset to Color correct");
          return;
       }
       if (lwm_ == null) {
          JOptionPane.showMessageDialog(getInstance(), "No calibration data available.  First Calibrate using 2C Reference");
          return;
       }
       
       
       ij.IJ.showStatus("Executing color correction");
       
       Runnable doWorkRunnable = new Runnable() {
 
          public void run() {
 
             List<GaussianSpotData> correctedData =
                     Collections.synchronizedList(new ArrayList<GaussianSpotData>());
             Iterator it = rowData.spotList_.iterator();
             int frameNr = 0;
             while (it.hasNext()) {
                GaussianSpotData gs = (GaussianSpotData) it.next();
                if (gs.getFrame() != frameNr) {
                   frameNr = gs.getFrame();
                   ij.IJ.showStatus("Executing color correction...");
                   ij.IJ.showProgress(frameNr, rowData.nrFrames_);
                }
                if (gs.getChannel() == 1) {
                   Point2D.Double point = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                   try {
                      Point2D.Double corPoint = lwm_.transform(point);
                      GaussianSpotData gsn = new GaussianSpotData(gs);
                      gsn.setXCenter(corPoint.x);
                      gsn.setYCenter(corPoint.y);
                      correctedData.add(gsn);
                   } catch (Exception ex) {
                      ex.printStackTrace();
                   }
                } else if (gs.getChannel() == 2) {
                   correctedData.add(gs);
                }
 
             }
 
             // Add transformed data to data overview window
             addSpotData(rowData.name_ + "Channel-Correct", rowData.title_, 
                     referenceName_.getText(), rowData.width_,
                     rowData.height_, rowData.pixelSizeNm_, rowData.shape_,
                     rowData.halfSize_, rowData.nrChannels_, rowData.nrFrames_,
                     rowData.nrSlices_, 1, rowData.maxNrSpots_, correctedData,
                     null, 
                     false);
          }
       };
 
       (new Thread(doWorkRunnable)).start();
    }
 
 
    /**
     * Plots Tracks using JFreeChart
     *
     * @rowData
     * @plotMode - Index of plotMode in array {"t-X", "t-Y", "X-Y"};
     */
    private void plotData(MyRowData rowData, int plotMode) {
       String title = rowData.name_ + " " + plotModes_[plotMode];
       if (plotMode == 0) {
          XYSeries data = new XYSeries("");
          for (int i = 0; i < rowData.spotList_.size(); i++) {
             GaussianSpotData spot = rowData.spotList_.get(i);
             if (rowData.timePoints_ != null) {
                double timePoint = rowData.timePoints_.get(i);
                data.add(timePoint , spot.getXCenter());
             } else {
                data.add(i, spot.getXCenter());
             }
          }
          String xAxis = "Time (frameNr)";
          if (rowData.timePoints_ != null)
             xAxis = "Time (s)";
          GaussianUtils.plotData(title, data, xAxis, "X(nm)", 0, 400);
       }
       else if (plotComboBox_.getSelectedIndex() == 1) {
          XYSeries data = new XYSeries("");
          for (int i = 0; i < rowData.spotList_.size(); i++) {
             GaussianSpotData spot = rowData.spotList_.get(i);
             if (rowData.timePoints_ != null) {
                double timePoint = rowData.timePoints_.get(i);
                data.add(timePoint, spot.getYCenter());
             } else {
                data.add(i, spot.getYCenter());
             }
          }
          String yAxis = "Time (frameNr)";
          if (rowData.timePoints_ != null) {
             yAxis = "Time (s)";
          }
          GaussianUtils.plotData(title, data, yAxis, "Y(nm)", 0, 400);
       }
       else if (plotComboBox_.getSelectedIndex() == 2) {
          XYSeries data = new XYSeries("", false, true);
          for (int i = 0; i < rowData.spotList_.size(); i++) {
             GaussianSpotData spot = rowData.spotList_.get(i);
             data.add(spot.getXCenter(), spot.getYCenter());
          }
          GaussianUtils.plotData(title, data, "X(nm)", "Y(nm)", 0, 400);
       }
    }
 
    private boolean test(double[][][] imageSpotList, int source, int target, double cutoff) {
       double xtest = imageSpotList[0][source][2] - imageSpotList[1][target][2];
       if (xtest > cutoff || xtest < -cutoff)
          return false;
       double ytest = imageSpotList[0][source][3] - imageSpotList[1][target][3];
       if (ytest > cutoff || ytest < -cutoff)
          return false;
       return true;
    }
 
    private class SpotSortComparator implements Comparator {
 
       // Return the result of comparing the two row arrays
       public int compare(Object o1, Object o2) {
          double[] p1 = (double[]) o1;
          double[] p2 = (double[]) o2;
          if (p1[0] < p2[0]) {
             return -1;
          }
          if (p1[0] > p2[0]) {
             return 1;
          }
          if (p1[0] == p2[0]) {
             if (p1[1] < p2[1]) {
                return -1;
             }
             if (p1[1] > p2[1]) {
                return 1;
             }
          }
          return 0;
       }
    }
 
 }
