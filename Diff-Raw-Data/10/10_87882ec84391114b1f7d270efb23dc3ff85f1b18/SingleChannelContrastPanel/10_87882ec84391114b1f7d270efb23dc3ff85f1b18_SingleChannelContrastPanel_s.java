 ///////////////////////////////////////////////////////////////////////////////
 //FILE:          ContrastPanel.java
 //PROJECT:       Micro-Manager
 //SUBSYSTEM:     mmstudio
 //-----------------------------------------------------------------------------
 //
 // AUTHOR:       Nenad Amodaj, nenad@amodaj.com, October 29, 2006
 //
 // COPYRIGHT:    University of California, San Francisco, 2006
 //
 // LICENSE:      This file is distributed under the BSD license.
 //               License text is included with the source distribution.
 //
 //               This file is distributed in the hope that it will be useful,
 //               but WITHOUT ANY WARRANTY; without even the implied warranty
 //               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 //
 //               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 //               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 //               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 //
 // CVS:          $Id: ContrastPanel.java 8480 2012-01-11 16:54:23Z henry $
 //
 package org.micromanager.graph;
 
 import ij.ImagePlus;
 import ij.WindowManager;
 import ij.process.ImageProcessor;
 import ij.process.LUT;
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.Font;
 import java.awt.Graphics;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.beans.PropertyChangeListener;
 import java.beans.PropertyChangeEvent;
 import java.text.NumberFormat;
 import java.text.ParseException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.prefs.Preferences;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JComboBox;
 import javax.swing.JFormattedTextField;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.JSpinner;
 import javax.swing.SpinnerModel;
 import javax.swing.SpinnerNumberModel;
 import javax.swing.SpringLayout;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 import org.json.JSONException;
 import org.micromanager.MMStudioMainFrame;
 import org.micromanager.acquisition.MMAcquisition;
 import org.micromanager.acquisition.MetadataPanel;
 import org.micromanager.acquisition.VirtualAcquisitionDisplay;
 import org.micromanager.api.ContrastPanel;
 import org.micromanager.api.ImageCache;
 import org.micromanager.graph.HistogramPanel.CursorListener;
 import org.micromanager.utils.ContrastSettings;
 import org.micromanager.utils.HistogramUtils;
 import org.micromanager.utils.MDUtils;
 import org.micromanager.utils.MMScriptException;
 import org.micromanager.utils.ReportingUtils;
 import org.micromanager.utils.NumberUtils;
 
 /**
  * Slider and histogram panel for adjusting contrast and brightness.
  * 
  */
 public class SingleChannelContrastPanel extends JPanel implements 
         ContrastPanel, PropertyChangeListener, CursorListener {
    private static final double SLOW_HIST_UPDATE_TIME_MS = 2000;
    
    private static final String PREF_AUTOSTRETCH = "sc_stretch_contrast";
    private static final String PREF_REJECT_OUTLIERS = "sc_reject_outliers";
    private static final String PREF_REJECT_FRACTION = "sc_reject_fraction";
    private static final String PREF_LOG_HIST = "sc_log_hist";
    private static final String PREF_SLOW_HIST = "sc_slow_hist";
    
    private static final int SLOW_HIST_UPDATE_INTERVAL_MS = 1000;
    private long slowHistLastUpdateTime_;
    
 	private static final long serialVersionUID = 1L;
 	private JComboBox modeComboBox_;
 	private HistogramPanel histogramPanel_;
 	private JLabel maxLabel_;
 	private JLabel minLabel_;
    private JLabel meanLabel_;
    private JLabel stdDevLabel_;
 	private SpringLayout springLayout;
    private JFormattedTextField gammaValue_;
    private NumberFormat numberFormat_;
    private double gamma_ = 1.0;
 	private int histMax_;
    private int maxIntensity_;
    private double mean_;
    private double stdDev_;
    private double pixelMin_ = 0.0;
    private double pixelMax_ = 255.0;
 	private int binSize_ = 1;
 	private static final int HIST_BINS = 256;
 	ContrastSettings cs8bit_;
 	ContrastSettings cs16bit_;
 	private JCheckBox autostretchCheckBox_;
 	private JCheckBox rejectOutliersCheckBox_;
    private JCheckBox slowHistCheckbox_;
 	private boolean logScale_ = false;
 	private JCheckBox logHistCheckBox_;
    private boolean autostretch_;
    private double contrastMin_;
    private double contrastMax_;
 	private double minAfterRejectingOutliers_;
 	private double maxAfterRejectingOutliers_;
    JSpinner rejectOutliersPercentSpinner_;
    private double fractionToReject_;
    JLabel percentOutliersLabel_;
    private int numFramesForSlowHist_;
    private MetadataPanel mdPanel_;
    private Preferences prefs_;
 
 
 
 	public SingleChannelContrastPanel(MetadataPanel md) {
 		super();
       
       mdPanel_ = md;
 
       numFramesForSlowHist_ = (int) (SLOW_HIST_UPDATE_TIME_MS / 33.0 );
       prefs_ = Preferences.userNodeForPackage(this.getClass());
       
       
       boolean autostretch = prefs_.getBoolean(PREF_AUTOSTRETCH, false);
       boolean reject = prefs_.getBoolean(PREF_REJECT_OUTLIERS, false);
       boolean slowHist = prefs_.getBoolean(PREF_SLOW_HIST, false);
       boolean logHist = prefs_.getBoolean(PREF_LOG_HIST, false);
       fractionToReject_ = prefs_.getDouble(PREF_REJECT_FRACTION, 0.02);
       
       init(autostretch, reject, slowHist, logHist);
       if (!autostretch) {
          rejectOutliersCheckBox_.setEnabled(false);
          rejectOutliersPercentSpinner_.setEnabled(false);
       }
    }
    
    private void saveSettings() {
       prefs_.putBoolean(PREF_AUTOSTRETCH, autostretchCheckBox_.isSelected());
       prefs_.putBoolean(PREF_LOG_HIST, logHistCheckBox_.isSelected());
       prefs_.putBoolean(PREF_REJECT_OUTLIERS, rejectOutliersCheckBox_.isSelected());
       prefs_.putBoolean(PREF_SLOW_HIST, slowHistCheckbox_.isSelected());
       prefs_.putDouble(PREF_REJECT_FRACTION, 0.01*((Double) rejectOutliersPercentSpinner_.getValue()));
    }
    
    private void init(boolean autostretch, boolean reject, boolean slowHist, boolean logHist) {
 		setFont(new Font("", Font.PLAIN, 10));
 		springLayout = new SpringLayout();
 		setLayout(springLayout);
 
       numberFormat_ = NumberFormat.getNumberInstance();
 
 		final JButton fullScaleButton_ = new JButton();
 		fullScaleButton_.addActionListener(new ActionListener() {
 			public void actionPerformed(final ActionEvent e) {
 				fullButtonAction();
 			}
 		});
 		fullScaleButton_.setFont(new Font("Arial", Font.PLAIN, 10));
 		fullScaleButton_
 				.setToolTipText("Set display levels to full pixel range");
 		fullScaleButton_.setText("Full");
 		add(fullScaleButton_);
 		springLayout.putConstraint(SpringLayout.EAST, fullScaleButton_, 80,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, fullScaleButton_, 5,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, fullScaleButton_, 25,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, fullScaleButton_, 5,
 				SpringLayout.NORTH, this);
 
 		final JButton autoScaleButton = new JButton();
 		autoScaleButton.addActionListener(new ActionListener() {
 			public void actionPerformed(final ActionEvent e) {
 				autoButtonAction();
 			}
 		});
 		autoScaleButton.setFont(new Font("Arial", Font.PLAIN, 10));
 		autoScaleButton
 				.setToolTipText("Set display levels to maximum contrast");
 		autoScaleButton.setText("Auto");
 		add(autoScaleButton);
 		springLayout.putConstraint(SpringLayout.EAST, autoScaleButton, 80,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, autoScaleButton, 5,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, autoScaleButton, 46,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, autoScaleButton, 26,
 				SpringLayout.NORTH, this);
 
 		minLabel_ = new JLabel();
 		minLabel_.setFont(new Font("", Font.PLAIN, 10));
 		add(minLabel_);
 		springLayout.putConstraint(SpringLayout.EAST, minLabel_, 95,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, minLabel_, 45,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, minLabel_, 98,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, minLabel_, 84,
 				SpringLayout.NORTH, this);
 
       maxLabel_ = new JLabel();
 		maxLabel_.setFont(new Font("", Font.PLAIN, 10));
 		add(maxLabel_);
 		springLayout.putConstraint(SpringLayout.EAST, maxLabel_, 95,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, maxLabel_, 45,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, maxLabel_, 114,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, maxLabel_, 100,
 				SpringLayout.NORTH, this);
 
 		JLabel minLabel = new JLabel();
 		minLabel.setFont(new Font("", Font.PLAIN, 10));
 		minLabel.setText("Min");
 		add(minLabel);
 		springLayout.putConstraint(SpringLayout.SOUTH, minLabel, 98,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, minLabel, 84,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.EAST, minLabel, 30,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, minLabel, 5,
 				SpringLayout.WEST, this);
 
 		JLabel maxLabel = new JLabel();
 		maxLabel.setFont(new Font("", Font.PLAIN, 10));
 		maxLabel.setText("Max");
 		add(maxLabel);
 		springLayout.putConstraint(SpringLayout.SOUTH, maxLabel, 114,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, maxLabel, 100,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.EAST, maxLabel, 30,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, maxLabel, 5,
 				SpringLayout.WEST, this);
 
       JLabel avgLabel = new JLabel();
       avgLabel.setFont(new Font("", Font.PLAIN, 10));
       avgLabel.setText("Avg");
       add(avgLabel);
       springLayout.putConstraint(SpringLayout.EAST, avgLabel, 42, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.WEST, avgLabel, 5, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.SOUTH, avgLabel, 130, SpringLayout.NORTH, this);
       springLayout.putConstraint(SpringLayout.NORTH, avgLabel, 116, SpringLayout.NORTH, this);
 
       meanLabel_ = new JLabel();                                              
       meanLabel_.setFont(new Font("", Font.PLAIN, 10));                       
       add(meanLabel_);                                                        
       springLayout.putConstraint(SpringLayout.EAST, meanLabel_, 95, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.WEST, meanLabel_, 45, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.SOUTH, meanLabel_, 130, SpringLayout.NORTH, this);
       springLayout.putConstraint(SpringLayout.NORTH, meanLabel_, 116, SpringLayout.NORTH, this);
                                                                              
       JLabel varLabel = new JLabel();
       varLabel.setFont(new Font("", Font.PLAIN, 10));
       varLabel.setText("Std Dev");
       add(varLabel);
       springLayout.putConstraint(SpringLayout.SOUTH, varLabel, 146, SpringLayout.NORTH, this);
       springLayout.putConstraint(SpringLayout.NORTH, varLabel, 132, SpringLayout.NORTH, this);
       springLayout.putConstraint(SpringLayout.EAST, varLabel, 42, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.WEST, varLabel, 5, SpringLayout.WEST, this);
 
       stdDevLabel_ = new JLabel();                                              
       stdDevLabel_.setFont(new Font("", Font.PLAIN, 10));                       
       add(stdDevLabel_);
       springLayout.putConstraint(SpringLayout.EAST, stdDevLabel_, 95, SpringLayout.WEST, this); 
       springLayout.putConstraint(SpringLayout.WEST, stdDevLabel_, 45, SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.SOUTH, stdDevLabel_, 146, SpringLayout.NORTH, this);
       springLayout.putConstraint(SpringLayout.NORTH, stdDevLabel_, 132, SpringLayout.NORTH, this);
 
       JLabel gammaLabel = new JLabel();
       gammaLabel.setFont(new Font("Arial", Font.PLAIN, 10));
       gammaLabel.setPreferredSize(new Dimension(40, 20));
       gammaLabel.setText("Gamma");
       add(gammaLabel);
 		springLayout.putConstraint(SpringLayout.WEST, gammaLabel, 5,
 				SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.NORTH, gammaLabel, 250,
 				SpringLayout.NORTH, this);
 
       gammaValue_ = new JFormattedTextField(numberFormat_);
       gammaValue_.setFont(new Font("Arial", Font.PLAIN, 10));
       gammaValue_.setValue(gamma_);
       gammaValue_.addPropertyChangeListener("value", this);
       gammaValue_.setPreferredSize(new Dimension(35, 20));
       gammaValue_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             gammaBoxAction();
          }});
 
       add(gammaValue_);
 		springLayout.putConstraint(SpringLayout.WEST, gammaValue_, 45,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.EAST, gammaValue_, 95,
 				SpringLayout.WEST, this);
   		springLayout.putConstraint(SpringLayout.NORTH, gammaValue_, 0,
 				SpringLayout.NORTH, gammaLabel);
 
 		histogramPanel_ = new HistogramPanel() {
           public void paint(Graphics g) {
            super.paint(g);
            //For drawing max label
            g.setColor(Color.black);
            g.setFont(new Font("Lucida Grande", 0, 10));          
            String label = ""+histMax_;
            g.drawString(label, this.getSize().width - 7*label.length(), this.getSize().height );
         } };
 		histogramPanel_.setMargins(8, 10);
       histogramPanel_.setTraceStyle(true, Color.white);
 		histogramPanel_.setTextVisible(false);
 		histogramPanel_.setGridVisible(false);
       
       histogramPanel_.addCursorListener(this);
 
 
 		add(histogramPanel_);
 		springLayout.putConstraint(SpringLayout.EAST, histogramPanel_, -5,
 				SpringLayout.EAST, this);
 		springLayout.putConstraint(SpringLayout.WEST, histogramPanel_, 95,
 				SpringLayout.WEST, this);
 
 		springLayout.putConstraint(SpringLayout.SOUTH, histogramPanel_, -6,
 				SpringLayout.SOUTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, histogramPanel_, 0,
 				SpringLayout.NORTH, fullScaleButton_);
 
 		autostretchCheckBox_ = new JCheckBox();
       autostretchCheckBox_.setSelected(autostretch);
       autostretch_ = autostretch;
 		autostretchCheckBox_.setFont(new Font("", Font.PLAIN, 10));
 		autostretchCheckBox_.setText("Auto-stretch");
 		autostretchCheckBox_.addChangeListener(new ChangeListener() {
 			public void stateChanged(ChangeEvent ce) {
             autostretchCheckboxAction();
 			};
 		});
 		add(autostretchCheckBox_);
 
 		springLayout.putConstraint(SpringLayout.EAST, autostretchCheckBox_, 5,
 				SpringLayout.WEST, histogramPanel_);
 		springLayout.putConstraint(SpringLayout.WEST, autostretchCheckBox_, 0,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, autostretchCheckBox_, 205,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, autostretchCheckBox_, 180,
 				SpringLayout.NORTH, this);
 
 
 	   rejectOutliersCheckBox_ = new JCheckBox();
 		rejectOutliersCheckBox_.setFont(new Font("", Font.PLAIN, 10));
 		rejectOutliersCheckBox_.setText("");
       rejectOutliersCheckBox_.setSelected(reject);
 		rejectOutliersCheckBox_.addChangeListener(new ChangeListener() {
 			public void stateChanged(ChangeEvent ce) {
             rejectOutliersAction();
 			};
 		});
 		add(rejectOutliersCheckBox_);
 
 		springLayout.putConstraint(SpringLayout.EAST, rejectOutliersCheckBox_, 30,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, rejectOutliersCheckBox_, 0,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, rejectOutliersCheckBox_, 230,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, rejectOutliersCheckBox_, 210,
 				SpringLayout.NORTH, this);
 
 
       SpinnerModel smodel = new SpinnerNumberModel(100*fractionToReject_,0,100,.1);
       rejectOutliersPercentSpinner_ = new JSpinner();
       rejectOutliersPercentSpinner_.setModel(smodel);
       Dimension sd = rejectOutliersPercentSpinner_.getSize();
       rejectOutliersPercentSpinner_.setFont(new Font("Arial", Font.PLAIN, 9));
       // user sees the fraction as percent
       add(rejectOutliersPercentSpinner_);
       rejectOutliersPercentSpinner_.setEnabled(reject);
       rejectOutliersPercentSpinner_.setToolTipText("% pixels dropped or saturated to reject");
       smodel.addChangeListener(new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            percentOutliersAction();
          }});
 
 
 		springLayout.putConstraint(SpringLayout.EAST, rejectOutliersPercentSpinner_, 90,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, rejectOutliersPercentSpinner_, 35,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, rejectOutliersPercentSpinner_, 230,
 				SpringLayout.NORTH, this);
 		springLayout.putConstraint(SpringLayout.NORTH, rejectOutliersPercentSpinner_, 210,
 				SpringLayout.NORTH, this);
 
       percentOutliersLabel_ = new JLabel();
       percentOutliersLabel_.setEnabled(reject);
       percentOutliersLabel_.setFont(new Font("Arial", Font.PLAIN, 10));
       percentOutliersLabel_.setText("% outliers to ignore");
       add(percentOutliersLabel_);
 		springLayout.putConstraint(SpringLayout.WEST, percentOutliersLabel_, 5,
 				SpringLayout.WEST, this);
       springLayout.putConstraint(SpringLayout.NORTH, percentOutliersLabel_, 230,
 				SpringLayout.NORTH, this);
  		springLayout.putConstraint(SpringLayout.EAST, percentOutliersLabel_, 5,
 				SpringLayout.WEST, histogramPanel_);
 
 
 		modeComboBox_ = new JComboBox();
 		modeComboBox_.setFont(new Font("", Font.PLAIN, 10));
 		modeComboBox_.addActionListener(new ActionListener() {
 			public void actionPerformed(final ActionEvent e) {
 				pixelTypeAction();
 			}});
 		modeComboBox_.setModel(new DefaultComboBoxModel(new String[] {
 				"camera", "8bit", "10bit", "12bit", "14bit", "16bit" }));
 		add(modeComboBox_);
 		springLayout.putConstraint(SpringLayout.EAST, modeComboBox_, 0,
 				SpringLayout.EAST, maxLabel_);
 		springLayout.putConstraint(SpringLayout.WEST, modeComboBox_, 0,
 				SpringLayout.WEST, minLabel);
 		springLayout.putConstraint(SpringLayout.SOUTH, modeComboBox_, 27,
 				SpringLayout.SOUTH, varLabel);
 		springLayout.putConstraint(SpringLayout.NORTH, modeComboBox_, 5,
 				SpringLayout.SOUTH, varLabel);
 
 		logHistCheckBox_ = new JCheckBox();
       logHistCheckBox_.setSelected(logHist);
       logScale_ = logHist;
 		logHistCheckBox_.setFont(new Font("", Font.PLAIN, 10));
 		logHistCheckBox_.addActionListener(new ActionListener() {
 			public void actionPerformed(final ActionEvent e) {
 				logHistAction();
          }});
       slowHistCheckbox_ = new JCheckBox();
 		slowHistCheckbox_.setFont(new Font("", Font.PLAIN, 10));
 		slowHistCheckbox_.setText("Slow hist.");
 		slowHistCheckbox_.addChangeListener(new ChangeListener() {
 			public void stateChanged(ChangeEvent ce) {
             slowHistAction();
          };});
      slowHistCheckbox_.setSelected(slowHist);
 
       
 		logHistCheckBox_.setText("Log hist.");
 		add(logHistCheckBox_);
 		springLayout.putConstraint(SpringLayout.SOUTH, logHistCheckBox_, -20,
 				SpringLayout.NORTH, minLabel);
 		springLayout.putConstraint(SpringLayout.NORTH, logHistCheckBox_, 0,
 				SpringLayout.SOUTH, autoScaleButton);
 		springLayout.putConstraint(SpringLayout.EAST, logHistCheckBox_, 74,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, logHistCheckBox_, 1,
 				SpringLayout.WEST, this);
 
       
    
 		add(slowHistCheckbox_);
 		springLayout.putConstraint(SpringLayout.EAST, slowHistCheckbox_, 84,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.WEST, slowHistCheckbox_, 1,
 				SpringLayout.WEST, this);
 		springLayout.putConstraint(SpringLayout.SOUTH, slowHistCheckbox_, 0,
 				SpringLayout.NORTH, minLabel);
 		springLayout.putConstraint(SpringLayout.NORTH, slowHistCheckbox_, -20,
 				SpringLayout.NORTH, minLabel);
       	   
    }
    
    private void autoButtonAction() {
       autostretch();
       mdPanel_.drawWithoutUpdate();
    }
 
    private void fullButtonAction() {
       setFullScale();
       mdPanel_.drawWithoutUpdate();
    }
 
    private void slowHistAction() {    
       saveSettings();
    }
 
    private void logHistAction() {
       if (logHistCheckBox_.isSelected()) 
          logScale_ = true;
       else 
          logScale_ = false;
       ImagePlus img = WindowManager.getCurrentImage();
       if (img!= null)
          calcAndDisplayHistAndStats(img,true);
       saveSettings();
    }
 
    private void pixelTypeAction() {
       setHistMaxAndBinSize();
       ImagePlus img = WindowManager.getCurrentImage(); 
       if (img!= null)
          calcAndDisplayHistAndStats(img,true);
    }
 
    private void rejectOutliersAction() {
       rejectOutliersPercentSpinner_.setEnabled(rejectOutliersCheckBox_.isSelected());
       percentOutliersLabel_.setEnabled(rejectOutliersCheckBox_.isSelected());
       calcAndDisplayHistAndStats(WindowManager.getCurrentImage(),true);
       autoButtonAction();
       saveSettings();
    }
 
    private void percentOutliersAction() {
       fractionToReject_ = ((Double) rejectOutliersPercentSpinner_.getValue());
       calcAndDisplayHistAndStats(WindowManager.getCurrentImage(),true);
       autoButtonAction();
       saveSettings();
    }
    
    private void gammaBoxAction() {
       gamma_ = (Double) gammaValue_.getValue();
       mdPanel_.drawWithoutUpdate();
    }
    
    private void autostretchCheckboxAction() {
       rejectOutliersCheckBox_.setEnabled(autostretchCheckBox_.isSelected());
       boolean rejectControlsEnabled = autostretchCheckBox_.isSelected() && rejectOutliersCheckBox_.isSelected();
       percentOutliersLabel_.setEnabled(rejectControlsEnabled);
       rejectOutliersPercentSpinner_.setEnabled(rejectControlsEnabled);
       if (autostretchCheckBox_.isSelected()) {
          autostretch_ = true;
          autoButtonAction();
       } else {
          autostretch_ = false;
       }
       saveSettings();
    }
 
    public void applyLUTToImage(ImagePlus img, ImageCache cache) {
       if (img == null)
          return;
       ImageProcessor ip = img.getProcessor();
       if (ip == null)
          return;
 
       double maxValue = 255.0;
       byte[] r = new byte[256];
       byte[] g = new byte[256];
       byte[] b = new byte[256];
       for (int i = 0; i < 256; i++) {
          double val = Math.pow((double) i / maxValue, gamma_) * (double) maxValue;
          r[i] = (byte) val;
          g[i] = (byte) val;
          b[i] = (byte) val;
       }
       //apply gamma and contrast to image
       ip.setColorModel( new LUT(8, 256, r, g, b));    //doesnt explicitly redraw
       ip.setMinAndMax(contrastMin_, contrastMax_);   //doesnt explicitly redraw
       
       //store contrast settings
       cache.storeChannelDisplaySettings(0,(int)contrastMin_, (int)contrastMax_, gamma_);
       
       updateHistogram();
       gammaValue_.setValue(gamma_);
    }
    
    public void saveDisplaySettings(ImageCache cache) {
       cache.storeChannelDisplaySettings(0,(int)contrastMin_, (int)contrastMax_, gamma_);
    }
   	 
    private void updateHistogram() {
       histogramPanel_.setCursors(contrastMin_ / binSize_, contrastMax_ / binSize_, gamma_);
 		histogramPanel_.repaint();
    }
    
 	private void setHistMaxAndBinSize() {
 		switch (modeComboBox_.getSelectedIndex()-1) {        
       case -1:
          histMax_ = maxIntensity_;
          break;
       case 0: 
 			histMax_ = 255;
 			break;
 		case 1:
 			histMax_ = 1023;
 			break;
 		case 2:
 			histMax_ = 4095;
 			break;
 		case 3:
 			histMax_ = 16383;
 			break;
 		case 4:
 			histMax_ = 65535;
 			break;
 		default:
 			break;
 		}
 		binSize_ = (histMax_ + 1) / HIST_BINS;
       updateHistogram();
    }
 
    // only used for Gamma
    public void propertyChange(PropertyChangeEvent e) {
       try { 
          gamma_ = (double) NumberUtils.displayStringToDouble(numberFormat_.format(gammaValue_.getValue()));
       } catch (ParseException p) {
          ReportingUtils.logError(p, "ContrastPanel, Function propertyChange");
       }
       ImagePlus ip = WindowManager.getCurrentImage();
       if (ip != null) 
             mdPanel_.drawWithoutUpdate();
    }
 
    //Calculates autostretch, doesnt apply or redraw
 	public void autostretch() {        
          contrastMin_ = pixelMin_;
          contrastMax_ = pixelMax_;
 
 			if(rejectOutliersCheckBox_.isSelected()){
 				if( contrastMin_ < minAfterRejectingOutliers_  ){
                if( 0 < minAfterRejectingOutliers_){
                   contrastMin_ =  minAfterRejectingOutliers_;
                }
 				}
 				if( maxAfterRejectingOutliers_ < contrastMax_){
                   contrastMax_ = maxAfterRejectingOutliers_;
 				}
 			}
 	}
 
 	private void setFullScale() {
       setHistMaxAndBinSize();
       autostretchCheckBox_.setSelected(false);
       contrastMin_ = 0;
       contrastMax_ = histMax_;    
 	}
    
    private void loadContrastSettings(ImageCache cache) {
       contrastMax_ = cache.getChannelMax(0);
       if (contrastMax_ < 0)
          contrastMax_ = maxIntensity_;
       contrastMin_ = cache.getChannelMin(0);
       gamma_ = cache.getChannelGamma(0);
    }
    
    public void imageChanged(ImagePlus img, ImageCache cache, boolean drawHist) {
       boolean update = true;
       if (slowHistCheckbox_.isSelected()) {
          long time = System.currentTimeMillis();
          if (time - slowHistLastUpdateTime_ < SLOW_HIST_UPDATE_INTERVAL_MS) 
             update = false;
          else 
             slowHistLastUpdateTime_ = time;
       }
       if (update) {
          calcAndDisplayHistAndStats(img, drawHist);
          if (autostretch_) 
             autostretch();
          applyLUTToImage(img, cache);
       }
    }
    
    public void displayChanged(ImagePlus img, ImageCache cache) {
       try {
          VirtualAcquisitionDisplay vad = VirtualAcquisitionDisplay.getDisplay(WindowManager.getCurrentImage());
          int bitDepth = MDUtils.getBitDepth(vad.getSummaryMetadata());
          maxIntensity_ = (int) (Math.pow(2, bitDepth) - 1);
       } catch (JSONException ex) {
          ReportingUtils.logError("BitDepth not in summary metadata");
          maxIntensity_ = (int) (Math.pow(2, 16)-1);
       }
 
       setHistMaxAndBinSize();
       calcAndDisplayHistAndStats(img,true);
       if (autostretchCheckBox_.isSelected())
          autostretch();
       else 
          loadContrastSettings(cache);
      mdPanel_.drawWithoutUpdate(img);
    }
    
    public void calcAndDisplayHistAndStats(ImagePlus img, boolean drawHist) {
      if (img != null) {
          int[] rawHistogram = img.getProcessor().getHistogram();
          int imgWidth = img.getWidth();
          int imgHeight = img.getHeight();
          if (rejectOutliersCheckBox_.isSelected()) {
             // todo handle negative values
             maxAfterRejectingOutliers_ = rawHistogram.length;
             // specified percent of pixels are ignored in the automatic contrast setting
             int totalPoints = imgHeight * imgWidth;
             fractionToReject_ = 0.01 * (Double) rejectOutliersPercentSpinner_.getValue();
             HistogramUtils hu = new HistogramUtils(rawHistogram, totalPoints, fractionToReject_);
             minAfterRejectingOutliers_ = hu.getMinAfterRejectingOutliers();
             maxAfterRejectingOutliers_ = hu.getMaxAfterRejectingOutliers();
          }
          GraphData histogramData = new GraphData();
          
          pixelMin_ = -1;
          pixelMax_ = 0;
          mean_ = 0;
          
          int numBins = Math.min(rawHistogram.length / binSize_, HIST_BINS);
          int[] histogram = new int[HIST_BINS];
          int total = 0;
          for (int i = 0; i < numBins; i++) {
             histogram[i] = 0;
             for (int j = 0; j < binSize_; j++) {
                int rawHistIndex = i * binSize_ + j;
                int rawHistVal = rawHistogram[rawHistIndex];
                histogram[i] += rawHistVal;
                if (rawHistVal > 0) {
                   pixelMax_ = rawHistIndex;
                   if (pixelMin_ == -1) {
                      pixelMin_ = rawHistIndex;
                   }
                   mean_ += rawHistIndex * rawHistVal;
                }
             }
             total += histogram[i];
             if (logScale_) 
                histogram[i] = histogram[i] > 0 ? (int) (1000 * Math.log(histogram[i])) : 0;
          }
          mean_ /= imgWidth*imgHeight;
          if (pixelMin_ == pixelMax_) 
             if (pixelMin_ == 0) 
                pixelMax_++;
             else 
                pixelMin_--;
 
          // work around what is apparently a bug in ImageJ
          if (total == 0) {
             if (img.getProcessor().getMin() == 0) {
                histogram[0] = imgWidth * imgHeight;
             } else {
                histogram[numBins - 1] = imgWidth * imgHeight;
             }
          }
         
          if (drawHist) {
             stdDev_ = 0;
             for (int i = 0; i < rawHistogram.length; i++) {
                for (int j = 0; j < rawHistogram[i]; j++) {
                   stdDev_ += (i - mean_) * (i - mean_);
                }
             }
             stdDev_ = Math.sqrt(stdDev_ / (imgWidth * imgHeight));
             //Draw histogram and stats
             histogramData.setData(histogram);
             histogramPanel_.setData(histogramData);
             histogramPanel_.setAutoScale();
 
             maxLabel_.setText(NumberUtils.intToDisplayString((int) pixelMax_));
             minLabel_.setText(NumberUtils.intToDisplayString((int) pixelMin_));
             meanLabel_.setText(NumberUtils.intToDisplayString((int) mean_));
             stdDevLabel_.setText(NumberUtils.doubleToDisplayString(stdDev_));
 
             histogramPanel_.repaint();
          }
       }
    }
    
    public void setChannelContrast(int channelIndex, int min, int max, double gamma) {
       if (channelIndex != 0)
          return;
       contrastMax_ = max;
       contrastMin_ = min;
       gamma_ = gamma;
    }
 
    public void onLeftCursor(double pos) {
       if (autostretch_)
          autostretchCheckBox_.setSelected(false);
 
       contrastMin_ = Math.max(0, pos) * binSize_;
       if (contrastMax_ < contrastMin_)
          contrastMax_ = contrastMin_;
       mdPanel_.drawWithoutUpdate();
 
    }
 
    public void onRightCursor(double pos) {
       if (autostretch_)
          autostretchCheckBox_.setSelected(false);
       
       contrastMax_ = Math.min(255, pos) * binSize_;
       if (contrastMin_ > contrastMax_)
          contrastMin_ = contrastMax_;
       mdPanel_.drawWithoutUpdate();
    }
 
    public void onGammaCurve(double gamma) {
       if (gamma != 0) {
          if (gamma > 0.9 & gamma < 1.1) 
             gamma_ = 1;
          else 
             gamma_ = gamma;
          gammaValue_.setValue(gamma_);
          mdPanel_.drawWithoutUpdate();
       }
    }
  
    public void setupChannelControls(ImageCache cache) {
    }
 }
