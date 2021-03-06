 ///////////////////////////////////////////////////////////////////////////////
 //FILE:          MMStudioMainFrame.java
 //PROJECT:       Micro-Manager
 //SUBSYSTEM:     mmstudio
 //-----------------------------------------------------------------------------
 //AUTHOR:        Nenad Amodaj, nenad@amodaj.com, Jul 18, 2005
 //               Modifications by Arthur Edelstein, Nico Stuurman
 //COPYRIGHT:     University of California, San Francisco, 2006-2010
 //               100X Imaging Inc, www.100ximaging.com, 2008
 //LICENSE:       This file is distributed under the BSD license.
 //               License text is included with the source distribution.
 //               This file is distributed in the hope that it will be useful,
 //               but WITHOUT ANY WARRANTY; without even the implied warranty
 //               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 //               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 //               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 //               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 //CVS:          $Id$
 //
 package org.micromanager;
 
 import ij.IJ;
 import ij.ImageJ;
 import ij.ImagePlus;
 import ij.WindowManager;
 import ij.gui.Line;
 import ij.gui.Roi;
 import ij.process.ImageProcessor;
 import ij.process.ShortProcessor;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.Font;
 import java.awt.Insets;
 import java.awt.Rectangle;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.FocusAdapter;
 import java.awt.event.FocusEvent;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.awt.geom.Point2D;
 
 import java.io.File;
 import java.io.IOException;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.prefs.Preferences;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JCheckBoxMenuItem;
 import javax.swing.JComboBox;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JMenu;
 import javax.swing.JMenuBar;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JTextField;
 import javax.swing.JToggleButton;
 import javax.swing.SpringLayout;
 import javax.swing.SwingConstants;
 import javax.swing.SwingUtilities;
 import javax.swing.UIManager;
 import javax.swing.event.AncestorEvent;
 
 import mmcorej.CMMCore;
 import mmcorej.DeviceType;
 import mmcorej.MMCoreJ;
 import mmcorej.MMEventCallback;
 import mmcorej.Metadata;
 import mmcorej.StrVector;
 
 import org.json.JSONObject;
 import org.micromanager.acquisition.AcquisitionManager;
 import org.micromanager.acquisition.MMImageCache;
 import org.micromanager.api.AcquisitionEngine;
 import org.micromanager.api.Autofocus;
 import org.micromanager.api.DeviceControlGUI;
 import org.micromanager.api.MMPlugin;
 import org.micromanager.api.ScriptInterface;
 import org.micromanager.api.MMListenerInterface;
 import org.micromanager.conf.ConfiguratorDlg;
 import org.micromanager.conf.MMConfigFileException;
 import org.micromanager.conf.MicroscopeModel;
 import org.micromanager.graph.ContrastPanel;
 import org.micromanager.graph.GraphData;
 import org.micromanager.graph.GraphFrame;
 import org.micromanager.navigation.CenterAndDragListener;
 import org.micromanager.navigation.PositionList;
 import org.micromanager.navigation.XYZKeyListener;
 import org.micromanager.navigation.ZWheelListener;
 import org.micromanager.utils.AutofocusManager;
 import org.micromanager.utils.ContrastSettings;
 import org.micromanager.utils.GUIColors;
 import org.micromanager.utils.GUIUtils;
 import org.micromanager.utils.JavaUtils;
 import org.micromanager.utils.MMException;
 import org.micromanager.utils.MMImageWindow;
 import org.micromanager.utils.MMScriptException;
 import org.micromanager.utils.NumberUtils;
 import org.micromanager.utils.TextUtils;
 import org.micromanager.utils.WaitDialog;
 
 
 import bsh.EvalError;
 import bsh.Interpreter;
 
 import com.swtdesigner.SwingResourceManager;
 import ij.gui.ImageCanvas;
 import ij.gui.ImageWindow;
 import ij.process.FloatProcessor;
 import java.awt.Cursor;
 import java.awt.Graphics;
 import java.awt.KeyboardFocusManager;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.nio.ByteBuffer;
 import java.util.Collections;
 import java.util.concurrent.Callable;
 import javax.swing.BorderFactory;
 import javax.swing.JPanel;
 import javax.swing.JSplitPane;
 import javax.swing.event.AncestorListener;
 import mmcorej.TaggedImage;
 import org.micromanager.acquisition.AcquisitionVirtualStack;
 
 import org.micromanager.acquisition.AcquisitionWrapperEngine;
 import org.micromanager.acquisition.MMAcquisition;
 import org.micromanager.acquisition.MetadataPanel;
 import org.micromanager.acquisition.TaggedImageStorageDiskDefault;
 import org.micromanager.acquisition.VirtualAcquisitionDisplay;
 import org.micromanager.utils.ImageFocusListener;
 import org.micromanager.api.Pipeline;
 import org.micromanager.api.TaggedImageStorage;
 import org.micromanager.utils.FileDialogs;
 import org.micromanager.utils.FileDialogs.FileType;
 import org.micromanager.utils.HotKeysDialog;
 import org.micromanager.utils.MMKeyDispatcher;
 import org.micromanager.utils.ReportingUtils;
 
 
 /*
  * Main panel and application class for the MMStudio.
  */
 public class MMStudioMainFrame extends JFrame implements DeviceControlGUI, ScriptInterface {
 
    private static final String MICRO_MANAGER_TITLE = "Micro-Manager 1.4";
    private static final String VERSION = "1.4.4 ";
    private static final long serialVersionUID = 3556500289598574541L;
    private static final String MAIN_FRAME_X = "x";
    private static final String MAIN_FRAME_Y = "y";
    private static final String MAIN_FRAME_WIDTH = "width";
    private static final String MAIN_FRAME_HEIGHT = "height";
    private static final String MAIN_FRAME_DIVIDER_POS = "divider_pos";
    private static final String MAIN_EXPOSURE = "exposure";
    private static final String SYSTEM_CONFIG_FILE = "sysconfig_file";
    private static final String MAIN_STRETCH_CONTRAST = "stretch_contrast";
    private static final String CONTRAST_SETTINGS_8_MIN = "contrast8_MIN";
    private static final String CONTRAST_SETTINGS_8_MAX = "contrast8_MAX";
    private static final String CONTRAST_SETTINGS_16_MIN = "contrast16_MIN";
    private static final String CONTRAST_SETTINGS_16_MAX = "contrast16_MAX";
    private static final String OPEN_ACQ_DIR = "openDataDir";
    private static final String SCRIPT_CORE_OBJECT = "mmc";
    private static final String SCRIPT_ACQENG_OBJECT = "acq";
    private static final String SCRIPT_GUI_OBJECT = "gui";
    private static final String AUTOFOCUS_DEVICE = "autofocus_device";
    private static final String MOUSE_MOVES_STAGE = "mouse_moves_stage";
 
 
    // cfg file saving
    private static final String CFGFILE_ENTRY_BASE = "CFGFileEntry"; // + {0, 1, 2, 3, 4}
    // GUI components
    private JComboBox comboBinning_;
    private JComboBox shutterComboBox_;
    private JTextField textFieldExp_;
    private JLabel labelImageDimensions_;
    private JToggleButton toggleButtonLive_;
    private JCheckBox autoShutterCheckBox_;
    private boolean autoShutterOrg_;
    private boolean shutterOrg_;
    private MMOptions options_;
    private boolean runsAsPlugin_;
    private JCheckBoxMenuItem centerAndDragMenuItem_;
    private JButton buttonSnap_;
    private JButton buttonAutofocus_;
    private JButton buttonAutofocusTools_;
    private JToggleButton toggleButtonShutter_;
    private GUIColors guiColors_;
    private GraphFrame profileWin_;
    private PropertyEditor propertyBrowser_;
    private CalibrationListDlg calibrationListDlg_;
    private AcqControlDlg acqControlWin_;
    private ReportProblemDialog reportProblemDialog_;
    
    private JMenu pluginMenu_;
    private ArrayList<PluginItem> plugins_;
    private List<MMListenerInterface> MMListeners_
            = (List<MMListenerInterface>)
            Collections.synchronizedList(new ArrayList<MMListenerInterface>());
    private List<Component> MMFrames_
            = (List<Component>)
            Collections.synchronizedList(new ArrayList<Component>());
    private AutofocusManager afMgr_;
    private final static String DEFAULT_CONFIG_FILE_NAME = "MMConfig_demo.cfg";
    private ArrayList<String> MRUConfigFiles_;
    private static final int maxMRUCfgs_ = 5;
    private String sysConfigFile_;
    private String startupScriptFile_;
    private String sysStateFile_ = "MMSystemState.cfg";
    private ConfigGroupPad configPad_;
    private ContrastPanel contrastPanel_;
    // Timer interval - image display interval
    private double liveModeInterval_ = 40;
    private Timer liveModeTimer_;
    private LiveModeTimerTask liveModeTimerTask_;
    private GraphData lineProfileData_;
    private Object img_;
    // labels for standard devices
    private String cameraLabel_;
    private String zStageLabel_;
    private String shutterLabel_;
    private String xyStageLabel_;
    // applications settings
    private Preferences mainPrefs_;
    private Preferences systemPrefs_;
    // MMcore
    private CMMCore core_;
    private AcquisitionEngine engine_;
    private PositionList posList_;
    private PositionListDlg posListDlg_;
    private String openAcqDirectory_ = "";
    private boolean running_;
    private boolean liveRunning_ = false;
    private boolean configChanged_ = false;
    private StrVector shutters_ = null;
 
    private JButton saveConfigButton_;
    private ScriptPanel scriptPanel_;
    private org.micromanager.utils.HotKeys hotKeys_;
    //private SplitView splitView_;
    private CenterAndDragListener centerAndDragListener_;
    private ZWheelListener zWheelListener_;
    private XYZKeyListener xyzKeyListener_;
    private AcquisitionManager acqMgr_;
    private static MMImageWindow imageWin_;
    private int snapCount_ = -1;
    private boolean liveModeSuspended_;
    public Font defaultScriptFont_ = null;
    public JLabel citePleaLabel_;
    
    public static FileType MM_CONFIG_FILE
             = new FileType("MM_CONFIG_FILE",
                            "Micro-Manager Config File",
                            "./MyScope.cfg",
                            true, "cfg");
 
    // Our instance
    private static MMStudioMainFrame gui_;
    // Callback
    private CoreEventCallback cb_;
 
    private JMenuBar menuBar_;
    private ConfigPadButtonPanel configPadButtonPanel_;
    private boolean virtual_ = false;
    private final JMenu switchConfigurationMenu_;
    private final MetadataPanel metadataPanel_;
    public static FileType MM_DATA_SET 
            = new FileType("MM_DATA_SET",
                  "Micro-Manager Image Location",
                  System.getProperty("user.home") + "/Untitled",
                  false, (String[]) null);
    private Thread pipelineClassLoadingThread_ = null;
    private Class pipelineClass_ = null;
    private Pipeline acquirePipeline_ = null;
    private final JSplitPane splitPane_;
   private ArrayList<Callable<Boolean>> exitHandlers;
 
    public ImageWindow getImageWin() {
       return imageWin_;
    }
 
    public static MMImageWindow getLiveWin() {
       return imageWin_;
    }
 
    private void doSnapColor() {
       try {
          getPipeline().doSnap();
       } catch (Exception ex) {
          ReportingUtils.logError(ex);
       }
    }
    
    private void doSnapFloat() {
       try {
          // just a test harness
          core_.snapImage();
          byte[] byteImage = (byte[]) core_.getImage();
          int  ii = (int)core_.getImageWidth();
          int   jj = (int)core_.getImageHeight();
          int   npoints = ii*jj;
          float[] floatImage = new float[npoints];
 
          ImagePlus implus = new ImagePlus();
          int iiterator = 0;
          int oiterator = 0;
          for (; oiterator < npoints; ++oiterator) {
             floatImage[oiterator] = Float.intBitsToFloat(((int) byteImage[iiterator+3 ] << 24) + ((int) byteImage[iiterator + 2] << 16) + ((int) byteImage[iiterator + 1] << 8) + (int) byteImage[iiterator]);
             iiterator += 4;
          }
          FloatProcessor fp = new FloatProcessor(ii, jj, floatImage, null);
          implus.setProcessor(fp);
          ImageWindow iwindow = new ImageWindow(implus);
          WindowManager.setCurrentWindow(iwindow);
       } catch (Exception ex) {
          ReportingUtils.showError(ex);
       }
 
 
    }
 
    private void doSnapMonochrome() {
       try {
          Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
          setCursor(waitCursor);
          if (!isImageWindowOpen()) {
             imageWin_ = createImageWindow();
          }
          if (imageWin_ == null) {
             return;
          }
          imageWin_.toFront();
          setIJCal(imageWin_);
          // this is needed to clear the subtite, should be folded into
          // drawInfo
          imageWin_.getGraphics().clearRect(0, 0, imageWin_.getWidth(), 40);
          imageWin_.drawInfo(imageWin_.getGraphics());
          imageWin_.setSubTitle("Snap");
          String expStr = textFieldExp_.getText();
          if (expStr.length() > 0) {
             core_.setExposure(NumberUtils.displayStringToDouble(expStr));
             updateImage();
          } else {
             handleError("Exposure field is empty!");
          }
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
       Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
       setCursor(defaultCursor);
    }
 
     private void initializeHelpMenu() {
         // add help menu item
         final JMenu helpMenu = new JMenu();
         helpMenu.setText("Help");
         menuBar_.add(helpMenu);
         final JMenuItem usersGuideMenuItem = new JMenuItem();
         usersGuideMenuItem.addActionListener(new ActionListener() {
 
             public void actionPerformed(ActionEvent e) {
                 try {
                     ij.plugin.BrowserLauncher.openURL("http://micro-manager.org/documentation.php?object=Userguide");
                 } catch (IOException e1) {
                     ReportingUtils.showError(e1);
                 }
             }
         });
         usersGuideMenuItem.setText("User's Guide...");
         helpMenu.add(usersGuideMenuItem);
         final JMenuItem configGuideMenuItem = new JMenuItem();
         configGuideMenuItem.addActionListener(new ActionListener() {
 
             public void actionPerformed(ActionEvent e) {
                 try {
                     ij.plugin.BrowserLauncher.openURL("http://micro-manager.org/documentation.php?object=Configguide");
                 } catch (IOException e1) {
                     ReportingUtils.showError(e1);
                 }
             }
         });
         configGuideMenuItem.setText("Configuration Guide...");
         helpMenu.add(configGuideMenuItem);
         if (!systemPrefs_.getBoolean(RegistrationDlg.REGISTRATION, false)) {
             final JMenuItem registerMenuItem = new JMenuItem();
             registerMenuItem.addActionListener(new ActionListener() {
 
                 public void actionPerformed(ActionEvent e) {
                     try {
                         RegistrationDlg regDlg = new RegistrationDlg(systemPrefs_);
                         regDlg.setVisible(true);
                     } catch (Exception e1) {
                         ReportingUtils.showError(e1);
                     }
                 }
             });
             registerMenuItem.setText("Register your copy of Micro-Manager...");
             helpMenu.add(registerMenuItem);
         }
         final MMStudioMainFrame thisFrame = this;
         final JMenuItem reportProblemMenuItem = new JMenuItem();
         reportProblemMenuItem.addActionListener(new ActionListener() {
 
             public void actionPerformed(ActionEvent e) {
                 if (null == reportProblemDialog_) {
                     reportProblemDialog_ = new ReportProblemDialog(core_, thisFrame, sysConfigFile_, options_);
                     thisFrame.addMMBackgroundListener(reportProblemDialog_);
                     reportProblemDialog_.setBackground(guiColors_.background.get(options_.displayBackground_));
                 }
                 reportProblemDialog_.setVisible(true);
             }
         });
         reportProblemMenuItem.setText("Report Problem");
         helpMenu.add(reportProblemMenuItem);
         final JMenuItem aboutMenuItem = new JMenuItem();
         aboutMenuItem.addActionListener(new ActionListener() {
 
             public void actionPerformed(ActionEvent e) {
                 MMAboutDlg dlg = new MMAboutDlg();
                 String versionInfo = "MM Studio version: " + VERSION;
                 versionInfo += "\n" + core_.getVersionInfo();
                 versionInfo += "\n" + core_.getAPIVersionInfo();
                 versionInfo += "\nUser: " + core_.getUserId();
                 versionInfo += "\nHost: " + core_.getHostName();
                 dlg.setVersionInfo(versionInfo);
                 dlg.setVisible(true);
             }
         });
         aboutMenuItem.setText("About...");
         helpMenu.add(aboutMenuItem);
         menuBar_.validate();
     }
 
    private void updateSwitchConfigurationMenu() {
       switchConfigurationMenu_.removeAll();
       for (final String configFile : MRUConfigFiles_) {
          if (! configFile.equals(sysConfigFile_)) {
             JMenuItem configMenuItem = new JMenuItem();
             configMenuItem.setText(configFile);
             configMenuItem.addActionListener(new ActionListener() {
                String theConfigFile = configFile;
                public void actionPerformed(ActionEvent e) {
                   sysConfigFile_ = theConfigFile;
                   loadSystemConfiguration();
                   mainPrefs_.put(SYSTEM_CONFIG_FILE, sysConfigFile_);
                }
             });
             switchConfigurationMenu_.add(configMenuItem);
          }
       }
    }
 
    /**
     * Allows MMListeners to register themselves
     */
    public void addMMListener(MMListenerInterface newL) {
       if (MMListeners_.contains(newL))
          return;
       MMListeners_.add(newL);
    }
 
    /**
     * Allows MMListeners to remove themselves
     */
    public void removeMMListener(MMListenerInterface oldL) {
       if (!MMListeners_.contains(oldL))
          return;
       MMListeners_.remove(oldL);
    }
 
    /**
     * Lets JComponents register themselves so that their background can be
     * manipulated
     */
    public void addMMBackgroundListener(Component comp) {
       if (MMFrames_.contains(comp))
          return;
       MMFrames_.add(comp);
    }
 
    /**
     * Lets JComponents remove themselves from the list whose background gets
     * changes
     */
    public void removeMMBackgroundListener(Component comp) {
       if (!MMFrames_.contains(comp))
          return;
       MMFrames_.remove(comp);
    }
 
    public void updateContrast(ImagePlus iplus) {
       contrastPanel_.updateContrast(iplus);
    }
 
    /**
     * Part of ScriptInterface
     * Manipulate acquisition so that it looks like a burst
     */
    public void runBurstAcquisition() throws MMScriptException {
       double interval = engine_.getFrameIntervalMs();
       int nr = engine_.getNumFrames();
       boolean doZStack = engine_.isZSliceSettingEnabled();
       boolean doChannels = engine_.isChannelsSettingEnabled();
       engine_.enableZSliceSetting(false);
       engine_.setFrames(nr, 0);
       engine_.enableChannelsSetting(false);
       try {
          engine_.acquire();
       } catch (MMException e) {
          throw new MMScriptException(e);
       }
       engine_.setFrames(nr, interval);
       engine_.enableZSliceSetting(doZStack);
       engine_.enableChannelsSetting(doChannels);
    }
 
    public void runBurstAcquisition(int nr) throws MMScriptException {
       int originalNr = engine_.getNumFrames();
       double interval = engine_.getFrameIntervalMs();
       engine_.setFrames(nr, 0);
       this.runBurstAcquisition();
       engine_.setFrames(originalNr, interval);
    }
 
    public void runBurstAcquisition(int nr, String name, String root) throws MMScriptException {
       //String originalName = engine_.getDirName();
       String originalRoot = engine_.getRootName();
       engine_.setDirName(name);
       engine_.setRootName(root);
       this.runBurstAcquisition(nr);
       engine_.setRootName(originalRoot);
       //engine_.setDirName(originalDirName);
    }
 
 
    /**
     * @deprecated
     * @throws MMScriptException
     */
    public void startBurstAcquisition() throws MMScriptException {
       runAcquisition();
    }
 
    public boolean isBurstAcquisitionRunning() throws MMScriptException {
       if (engine_ == null)
          return false;
       return engine_.isAcquisitionRunning();
    }
 
    private void startLoadingPipelineClass() {
       Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
       pipelineClassLoadingThread_ = new Thread("Pipeline Class loading thread") {
          @Override
          public void run() {
             try {
                pipelineClass_  = Class.forName("org.micromanager.AcqEngine");
             } catch (Exception ex) {
                ReportingUtils.logError(ex);
                pipelineClass_ = null;
             }
          }
       };
       pipelineClassLoadingThread_.start();
    }
 
  
    
    /**
     * Callback to update GUI when a change happens in the MMCore.
     */
    public class CoreEventCallback extends MMEventCallback {
 
       public CoreEventCallback() {
          super();
       }
 
       @Override
       public void onPropertiesChanged() {
          // TODO: remove test once acquisition engine is fully multithreaded
          if (engine_ != null && engine_.isAcquisitionRunning()) {
             core_.logMessage("Notification from MMCore ignored because acquistion is running!");
          } else {
             updateGUI(true);
             // update all registered listeners
             for (MMListenerInterface mmIntf:MMListeners_) {
                mmIntf.propertiesChangedAlert();
             }
             core_.logMessage("Notification from MMCore!");
          }
       }
 
       @Override
       public void onPropertyChanged(String deviceName, String propName, String propValue) {
          core_.logMessage("Notification for Device: " + deviceName + " Property: " +
                propName + " changed to value: " + propValue);
          // update all registered listeners
          for (MMListenerInterface mmIntf:MMListeners_) {
             mmIntf.propertyChangedAlert(deviceName, propName, propValue);
          }
       }
 
       @Override
       public void onConfigGroupChanged(String groupName, String newConfig) {
          try {
             configPad_.refreshGroup(groupName, newConfig);
          } catch (Exception e) {
          }
       }
 
       @Override
       public void onPixelSizeChanged(double newPixelSizeUm) {
          updatePixSizeUm (newPixelSizeUm);
       }
 
       @Override
       public void onStagePositionChanged(String deviceName, double pos) {
          if (deviceName.equals(zStageLabel_))
             updateZPos(pos);
       }
 
       @Override
       public void onStagePositionChangedRelative(String deviceName, double pos) {
          if (deviceName.equals(zStageLabel_))
             updateZPosRelative(pos);
       }
 
       @Override
       public void onXYStagePositionChanged(String deviceName, double xPos, double yPos) {
          if (deviceName.equals(xyStageLabel_))
             updateXYPos(xPos, yPos);
       }
 
       @Override
       public void onXYStagePositionChangedRelative(String deviceName, double xPos, double yPos) {
          if (deviceName.equals(xyStageLabel_))
             updateXYPosRelative(xPos, yPos);
       }
 
    }
 
    private class PluginItem {
 
       public Class<?> pluginClass = null;
       public String menuItem = "undefined";
       public MMPlugin plugin = null;
       public String className = "";
 
       public void instantiate() {
 
          try {
             if (plugin == null) {
                plugin = (MMPlugin) pluginClass.newInstance();
             }
          } catch (InstantiationException e) {
             ReportingUtils.logError(e);
          } catch (IllegalAccessException e) {
             ReportingUtils.logError(e);
          }
          plugin.setApp(MMStudioMainFrame.this);
       }
    }
 
    /*
     * Simple class used to cache static info
     */
    private class StaticInfo {
 
       public long width_;
       public long height_;
       public long bytesPerPixel_;
       public long imageBitDepth_;
       public double pixSizeUm_;
       public double zPos_;
       public double x_;
       public double y_;
    }
    private StaticInfo staticInfo_ = new StaticInfo();
 
    /**
     * Main procedure for stand alone operation.
     */
    public static void main(String args[]) {
       try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          MMStudioMainFrame frame = new MMStudioMainFrame(false);
          frame.setVisible(true);
          frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
       } catch (Throwable e) {
          ReportingUtils.showError(e, "A java error has caused Micro-Manager to exit.");
          System.exit(1);
       }
    }
 
    public MMStudioMainFrame(boolean pluginStatus) {
       super();
 
       startLoadingPipelineClass();
 
       options_ = new MMOptions();
       try {
          options_.loadSettings();
       } catch (NullPointerException ex) {
          ReportingUtils.logError(ex);
       }
 
       guiColors_ = new GUIColors();
 
       plugins_ = new ArrayList<PluginItem>();
 
       gui_ = this;
 
       runsAsPlugin_ = pluginStatus;
       setIconImage(SwingResourceManager.getImage(MMStudioMainFrame.class,
             "icons/microscope.gif"));
       running_ = true;
 
       acqMgr_ = new AcquisitionManager();
 
       sysConfigFile_ = System.getProperty("user.dir") + "/"
             + DEFAULT_CONFIG_FILE_NAME;
 
       if (options_.startupScript_.length() > 0) {
          startupScriptFile_ = System.getProperty("user.dir") + "/"
                  + options_.startupScript_;
       } else {
          startupScriptFile_ = "";
       }
 
       ReportingUtils.SetContainingFrame(gui_);
 
       // set the location for app preferences
       try {
          mainPrefs_ = Preferences.userNodeForPackage(this.getClass());
       } catch (Exception e) {
          ReportingUtils.logError(e);
       }
       systemPrefs_ = mainPrefs_;
       // check system preferences
       try {
          Preferences p = Preferences.systemNodeForPackage(this.getClass());
          if (null != p) {
             // if we can not write to the systemPrefs, use AppPrefs instead
             if (JavaUtils.backingStoreAvailable(p)) {
                systemPrefs_ = p;
             }
          }
       } catch (Exception e) {
          ReportingUtils.logError(e);
       }
 
       // show registration dialog if not already registered
       // first check user preferences (for legacy compatibility reasons)
       boolean userReg = mainPrefs_.getBoolean(RegistrationDlg.REGISTRATION,
             false) || mainPrefs_.getBoolean(RegistrationDlg.REGISTRATION_NEVER, false);
 
       if (!userReg) {
          boolean systemReg = systemPrefs_.getBoolean(
                RegistrationDlg.REGISTRATION, false) || systemPrefs_.getBoolean(RegistrationDlg.REGISTRATION_NEVER, false);
          if (!systemReg) {
             // prompt for registration info
             RegistrationDlg dlg = new RegistrationDlg(systemPrefs_);
             dlg.setVisible(true);
          }
       }
 
       liveModeTimer_ = new Timer("liveModeTimer");
 
 
       // load application preferences
       // NOTE: only window size and position preferences are loaded,
       // not the settings for the camera and live imaging -
       // attempting to set those automatically on startup may cause problems
       // with the hardware
       int x = mainPrefs_.getInt(MAIN_FRAME_X, 100);
       int y = mainPrefs_.getInt(MAIN_FRAME_Y, 100);
       int width = mainPrefs_.getInt(MAIN_FRAME_WIDTH, 580);
       int height = mainPrefs_.getInt(MAIN_FRAME_HEIGHT, 482);
       boolean stretch = mainPrefs_.getBoolean(MAIN_STRETCH_CONTRAST, true);
       int dividerPos = mainPrefs_.getInt(MAIN_FRAME_DIVIDER_POS, 178);
       openAcqDirectory_ = mainPrefs_.get(OPEN_ACQ_DIR, "");
 
       setBounds(x, y, width, height);
       setExitStrategy(options_.closeOnExit_);
       setTitle(MICRO_MANAGER_TITLE);
       setBackground(guiColors_.background.get((options_.displayBackground_)));
       SpringLayout topLayout = new SpringLayout();
       
       this.setMinimumSize(new Dimension(580,480));
       JPanel topPanel = new JPanel();
       topPanel.setLayout(topLayout);
       topPanel.setMinimumSize(new Dimension(580, 175));
 
       class ListeningJPanel extends JPanel implements AncestorListener {
 
          public void ancestorMoved(AncestorEvent event) {
             //System.out.println("moved!");
          }
 
          public void ancestorRemoved(AncestorEvent event) {}
          public void ancestorAdded(AncestorEvent event) {}
 
       }
 
       ListeningJPanel bottomPanel = new ListeningJPanel();
       bottomPanel.setLayout(topLayout);
       
       splitPane_ = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
               topPanel, bottomPanel);
       splitPane_.setBorder(BorderFactory.createEmptyBorder());
       splitPane_.setDividerLocation(dividerPos);
       splitPane_.setResizeWeight(0.0);
       splitPane_.addAncestorListener(bottomPanel);
       getContentPane().add(splitPane_);
 
 
       // Snap button
       // -----------
       buttonSnap_ = new JButton();
       buttonSnap_.setIconTextGap(6);
       buttonSnap_.setText("Snap");
       buttonSnap_.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class, "/org/micromanager/icons/camera.png"));
       buttonSnap_.setFont(new Font("Arial", Font.PLAIN, 10));
       buttonSnap_.setToolTipText("Snap single image");
       buttonSnap_.setMaximumSize(new Dimension(0, 0));
       buttonSnap_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             doSnap();
          }
       });
       topPanel.add(buttonSnap_);
       topLayout.putConstraint(SpringLayout.SOUTH, buttonSnap_, 25,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, buttonSnap_, 4,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, buttonSnap_, 95,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, buttonSnap_, 7,
             SpringLayout.WEST, topPanel);
 
       // Initialize
       // ----------
 
       // Exposure field
       // ---------------
       final JLabel label_1 = new JLabel();
       label_1.setFont(new Font("Arial", Font.PLAIN, 10));
       label_1.setText("Exposure [ms]");
       topPanel.add(label_1);
       topLayout.putConstraint(SpringLayout.EAST, label_1, 198,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, label_1, 111,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, label_1, 39,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, label_1, 23,
             SpringLayout.NORTH, topPanel);
 
       textFieldExp_ = new JTextField();
       textFieldExp_.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent fe) {
             setExposure();
          }
       });
       textFieldExp_.setFont(new Font("Arial", Font.PLAIN, 10));
       textFieldExp_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             setExposure();
          }
       });
       topPanel.add(textFieldExp_);
       topLayout.putConstraint(SpringLayout.SOUTH, textFieldExp_, 40,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, textFieldExp_, 21,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, textFieldExp_, 276,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, textFieldExp_, 203,
             SpringLayout.WEST, topPanel);
 
       // Live button
       // -----------
       toggleButtonLive_ = new JToggleButton();
       toggleButtonLive_.setMargin(new Insets(2, 2, 2, 2));
       toggleButtonLive_.setIconTextGap(1);
       toggleButtonLive_.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/camera_go.png"));
       toggleButtonLive_.setIconTextGap(6);
       toggleButtonLive_.setToolTipText("Continuous live view");
       toggleButtonLive_.setFont(new Font("Arial", Font.PLAIN, 10));
       toggleButtonLive_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             if (!isLiveModeOn()) {
                // Display interval for Live Mode changes as well
                setLiveModeInterval();
             }
             enableLiveMode(!isLiveModeOn());
          }
       });
 
       toggleButtonLive_.setText("Live");
       topPanel.add(toggleButtonLive_);
       topLayout.putConstraint(SpringLayout.SOUTH, toggleButtonLive_, 47,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, toggleButtonLive_, 26,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, toggleButtonLive_, 95,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, toggleButtonLive_, 7,
             SpringLayout.WEST, topPanel);
 
       // Acquire button
       // -----------
       JButton acquireButton = new JButton();
       acquireButton.setMargin(new Insets(2, 2, 2, 2));
       acquireButton.setIconTextGap(1);
       acquireButton.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/snapAppend.png"));
       acquireButton.setIconTextGap(6);
       acquireButton.setToolTipText("Acquire single frame");
       acquireButton.setFont(new Font("Arial", Font.PLAIN, 10));
       acquireButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             snapAndAddToImage5D(null);
          }
       });
 
       acquireButton.setText("Acquire");
       topPanel.add(acquireButton);
       topLayout.putConstraint(SpringLayout.SOUTH, acquireButton, 69,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, acquireButton, 48,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, acquireButton, 95,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, acquireButton, 7,
             SpringLayout.WEST, topPanel);
 
       // Shutter button
       // --------------
 
       toggleButtonShutter_ = new JToggleButton();
       toggleButtonShutter_.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             toggleShutter();
          }
 
 
       });
       toggleButtonShutter_.setToolTipText("Open/close the shutter");
       toggleButtonShutter_.setIconTextGap(6);
       toggleButtonShutter_.setFont(new Font("Arial", Font.BOLD, 10));
       toggleButtonShutter_.setText("Open");
       topPanel.add(toggleButtonShutter_);
       topLayout.putConstraint(SpringLayout.EAST, toggleButtonShutter_,
             275, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, toggleButtonShutter_,
             203, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, toggleButtonShutter_,
             138 - 21, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, toggleButtonShutter_,
             117 - 21, SpringLayout.NORTH, topPanel);
 
       // Active shutter label
       final JLabel activeShutterLabel = new JLabel();
       activeShutterLabel.setFont(new Font("Arial", Font.PLAIN, 10));
       activeShutterLabel.setText("Shutter");
       topPanel.add(activeShutterLabel);
       topLayout.putConstraint(SpringLayout.SOUTH, activeShutterLabel,
             108 - 22, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, activeShutterLabel,
             95 - 22, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, activeShutterLabel,
             160 - 2, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, activeShutterLabel,
             113 - 2, SpringLayout.WEST, topPanel);
 
       // Active shutter Combo Box
       shutterComboBox_ = new JComboBox();
       shutterComboBox_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent arg0) {
             try {
                if (shutterComboBox_.getSelectedItem() != null) {
                   core_.setShutterDevice((String) shutterComboBox_.getSelectedItem());
                }
             } catch (Exception e) {
                ReportingUtils.showError(e);
             }
             return;
          }
       });
       topPanel.add(shutterComboBox_);
       topLayout.putConstraint(SpringLayout.SOUTH, shutterComboBox_,
             114 - 22, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, shutterComboBox_,
             92 - 22, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, shutterComboBox_, 275,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, shutterComboBox_, 170,
             SpringLayout.WEST, topPanel);
 
       menuBar_ = new JMenuBar();
       setJMenuBar(menuBar_);
 
       final JMenu fileMenu = new JMenu();
       fileMenu.setText("File");
       menuBar_.add(fileMenu);
 
       final JMenuItem openMenuItem = new JMenuItem();
       openMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             new Thread() {
                @Override
                public void run() {
                   openAcquisitionData();
                }
             }.start();
          }
       });
       openMenuItem.setText("Open Acquisition Data...");
       fileMenu.add(openMenuItem);
 
       fileMenu.addSeparator();
 
       final JMenuItem loadState = new JMenuItem();
       loadState.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             loadSystemState();
          }
       });
       loadState.setText("Load System State...");
       fileMenu.add(loadState);
 
       final JMenuItem saveStateAs = new JMenuItem();
       fileMenu.add(saveStateAs);
       saveStateAs.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             saveSystemState();
          }
       });
       saveStateAs.setText("Save System State As...");
 
       fileMenu.addSeparator();
 
       final JMenuItem exitMenuItem = new JMenuItem();
       exitMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             closeSequence();
          }
       });
       fileMenu.add(exitMenuItem);
       exitMenuItem.setText("Exit");
 /*
       final JMenu image5dMenu = new JMenu();
       image5dMenu.setText("Image5D");
       menuBar_.add(image5dMenu);
 
       final JMenuItem closeAllMenuItem = new JMenuItem();
       closeAllMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             WindowManager.closeAllWindows();
          }
       });
       closeAllMenuItem.setText("Close All");
       image5dMenu.add(closeAllMenuItem);
 
       final JMenuItem duplicateMenuItem = new JMenuItem();
       duplicateMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Duplicate_Image5D duplicate = new Duplicate_Image5D();
             duplicate.run("");
          }
       });
       duplicateMenuItem.setText("Duplicate");
       image5dMenu.add(duplicateMenuItem);
 
       final JMenuItem cropMenuItem = new JMenuItem();
       cropMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Crop_Image5D crop = new Crop_Image5D();
             crop.run("");
          }
       });
       cropMenuItem.setText("Crop");
       image5dMenu.add(cropMenuItem);
 
       final JMenuItem makeMontageMenuItem = new JMenuItem();
       makeMontageMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Make_Montage makeMontage = new Make_Montage();
             makeMontage.run("");
          }
       });
       makeMontageMenuItem.setText("Make Montage");
       image5dMenu.add(makeMontageMenuItem);
 
       final JMenuItem zProjectMenuItem = new JMenuItem();
       zProjectMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Z_Project projection = new Z_Project();
             projection.run("");
          }
       });
       zProjectMenuItem.setText("Z Project");
       image5dMenu.add(zProjectMenuItem);
 
       final JMenuItem convertToRgbMenuItem = new JMenuItem();
       convertToRgbMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Image5D_Stack_to_RGB stackToRGB = new Image5D_Stack_to_RGB();
             stackToRGB.run("");
          }
       });
       convertToRgbMenuItem.setText("Copy to RGB Stack(z)");
       image5dMenu.add(convertToRgbMenuItem);
 
       final JMenuItem convertToRgbtMenuItem = new JMenuItem();
       convertToRgbtMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Image5D_Stack_to_RGB_t stackToRGB_t = new Image5D_Stack_to_RGB_t();
             stackToRGB_t.run("");
          }
       });
       convertToRgbtMenuItem.setText("Copy to RGB Stack(t)");
       image5dMenu.add(convertToRgbtMenuItem);
 
       final JMenuItem convertToStackMenuItem = new JMenuItem();
       convertToStackMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Image5D_to_Stack image5DToStack = new Image5D_to_Stack();
             image5DToStack.run("");
          }
       });
       convertToStackMenuItem.setText("Copy to Stack");
       image5dMenu.add(convertToStackMenuItem);
 
       final JMenuItem convertToStacksMenuItem = new JMenuItem();
       convertToStacksMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Image5D_Channels_to_Stacks image5DToStacks = new Image5D_Channels_to_Stacks();
             image5DToStacks.run("");
          }
       });
       convertToStacksMenuItem.setText("Copy to Stacks (channels)");
       image5dMenu.add(convertToStacksMenuItem);
 
       final JMenuItem volumeViewerMenuItem = new JMenuItem();
       volumeViewerMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Image5D_to_VolumeViewer volumeViewer = new Image5D_to_VolumeViewer();
             volumeViewer.run("");
          }
       });
       volumeViewerMenuItem.setText("VolumeViewer");
       image5dMenu.add(volumeViewerMenuItem);
 
       final JMenuItem splitImageMenuItem = new JMenuItem();
       splitImageMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             Split_Image5D splitImage = new Split_Image5D();
             splitImage.run("");
          }
       });
       splitImageMenuItem.setText("SplitView");
       image5dMenu.add(splitImageMenuItem);
 
  */
 
       final JMenu toolsMenu = new JMenu();
       toolsMenu.setText("Tools");
       menuBar_.add(toolsMenu);
 
       final JMenuItem refreshMenuItem = new JMenuItem();
       refreshMenuItem.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class, "icons/arrow_refresh.png"));
       refreshMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             updateGUI(true);
          }
       });
       refreshMenuItem.setText("Refresh GUI");
       toolsMenu.add(refreshMenuItem);
 
       final JMenuItem rebuildGuiMenuItem = new JMenuItem();
       rebuildGuiMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             initializeGUI();
          }
       });
       rebuildGuiMenuItem.setText("Rebuild GUI");
       toolsMenu.add(rebuildGuiMenuItem);
 
       toolsMenu.addSeparator();
 
       final JMenuItem scriptPanelMenuItem = new JMenuItem();
       toolsMenu.add(scriptPanelMenuItem);
       scriptPanelMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             scriptPanel_.setVisible(true);
          }
       });
       scriptPanelMenuItem.setText("Script Panel...");
 
       final JMenuItem hotKeysMenuItem = new JMenuItem();
       toolsMenu.add(hotKeysMenuItem);
       hotKeysMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             HotKeysDialog hk = new HotKeysDialog
                     (guiColors_.background.get((options_.displayBackground_)));
             //hk.setBackground(guiColors_.background.get((options_.displayBackground_)));
          }
       });
       hotKeysMenuItem.setText("Shortcuts...");
 
       final JMenuItem propertyEditorMenuItem = new JMenuItem();
       toolsMenu.add(propertyEditorMenuItem);
       propertyEditorMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             createPropertyEditor();
          }
       });
       propertyEditorMenuItem.setText("Device/Property Browser...");
 
       toolsMenu.addSeparator();
 
       final JMenuItem xyListMenuItem = new JMenuItem();
       xyListMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent arg0) {
             showXYPositionList();
          }
       });
       xyListMenuItem.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class, "icons/application_view_list.png"));
       xyListMenuItem.setText("XY List...");
       toolsMenu.add(xyListMenuItem);
 
       final JMenuItem acquisitionMenuItem = new JMenuItem();
       acquisitionMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             openAcqControlDialog();
          }
       });
       acquisitionMenuItem.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class, "icons/film.png"));
       acquisitionMenuItem.setText("Multi-Dimensional Acquisition...");
       toolsMenu.add(acquisitionMenuItem);
 
       /*
       final JMenuItem splitViewMenuItem = new JMenuItem();
       splitViewMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             splitViewDialog();
          }
       });
       splitViewMenuItem.setText("Split View...");
       toolsMenu.add(splitViewMenuItem);
       */
       
       centerAndDragMenuItem_ = new JCheckBoxMenuItem();
 
       centerAndDragMenuItem_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             if (centerAndDragListener_ == null) {
                centerAndDragListener_ = new CenterAndDragListener(core_, gui_);
             }
             if (!centerAndDragListener_.isRunning()) {
                centerAndDragListener_.start();
                centerAndDragMenuItem_.setSelected(true);
             } else {
                centerAndDragListener_.stop();
                centerAndDragMenuItem_.setSelected(false);
             }
             mainPrefs_.putBoolean(MOUSE_MOVES_STAGE, centerAndDragMenuItem_.isSelected());
          }
       });
 
       centerAndDragMenuItem_.setText("Mouse Moves Stage");
       centerAndDragMenuItem_.setSelected(mainPrefs_.getBoolean(MOUSE_MOVES_STAGE, false));
 
       toolsMenu.add(centerAndDragMenuItem_);
 
       final JMenuItem calibrationMenuItem = new JMenuItem();
       toolsMenu.add(calibrationMenuItem);
       calibrationMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             createCalibrationListDlg();
          }
       });
       calibrationMenuItem.setText("Pixel Size Calibration...");
       toolsMenu.add(calibrationMenuItem);
 
       toolsMenu.addSeparator();
 
       final JMenuItem configuratorMenuItem = new JMenuItem();
       configuratorMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent arg0) {
             try {
                if (configChanged_) {
                   Object[] options = {"Yes", "No"};
                   int n = JOptionPane.showOptionDialog(null,
                         "Save Changed Configuration?", "Micro-Manager",
                         JOptionPane.YES_NO_OPTION,
                         JOptionPane.QUESTION_MESSAGE, null, options,
                         options[0]);
                   if (n == JOptionPane.YES_OPTION) {
                      saveConfigPresets();
                   }
                   configChanged_ = false;
                }
 
                boolean liveRunning = false;
                if (liveRunning_) {
                   liveRunning = liveRunning_;
                   enableLiveMode(false);
                }
 
                // unload all devices before starting configurator
                core_.reset();
                GUIUtils.preventDisplayAdapterChangeExceptions();
 
                // run Configurator
                ConfiguratorDlg configurator = new ConfiguratorDlg(core_,
                      sysConfigFile_);
                configurator.setVisible(true);
                GUIUtils.preventDisplayAdapterChangeExceptions();
 
                // re-initialize the system with the new configuration file
                sysConfigFile_ = configurator.getFileName();
                mainPrefs_.put(SYSTEM_CONFIG_FILE, sysConfigFile_);
                loadSystemConfiguration();
                GUIUtils.preventDisplayAdapterChangeExceptions();
 
                if (liveRunning) {
                   enableLiveMode(liveRunning);
                }
 
             } catch (Exception e) {
                ReportingUtils.showError(e);
                return;
             }
          }
       });
       configuratorMenuItem.setText("Hardware Configuration Wizard...");
       toolsMenu.add(configuratorMenuItem);
 
       final JMenuItem loadSystemConfigMenuItem = new JMenuItem();
       toolsMenu.add(loadSystemConfigMenuItem);
       loadSystemConfigMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             loadConfiguration();
             initializeGUI();
          }
       });
       loadSystemConfigMenuItem.setText("Load Hardware Configuration...");
 
       switchConfigurationMenu_ = new JMenu();
       for (int i=0; i<5; i++)
       {
          JMenuItem configItem = new JMenuItem();
          configItem.setText(Integer.toString(i));
          switchConfigurationMenu_.add(configItem);
          
       }
 
       switchConfigurationMenu_.setText("Switch Hardware Configuration");
       toolsMenu.add(switchConfigurationMenu_);
 
       final JMenuItem saveConfigurationPresetsMenuItem = new JMenuItem();
       saveConfigurationPresetsMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent arg0) {
             saveConfigPresets();
             updateChannelCombos();
          }
       });
       saveConfigurationPresetsMenuItem.setText("Save Configuration Settings...");
       toolsMenu.add(saveConfigurationPresetsMenuItem);
 
       toolsMenu.addSeparator();
 
       final MMStudioMainFrame thisInstance = this;
       final JMenuItem optionsMenuItem = new JMenuItem();
       optionsMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             int oldBufsize = options_.circularBufferSizeMB_;
 
             OptionsDlg dlg = new OptionsDlg(options_, core_, mainPrefs_,
                   thisInstance, sysConfigFile_);
             dlg.setVisible(true);
             // adjust memory footprint if necessary
             if (oldBufsize != options_.circularBufferSizeMB_) {
                try {
                   core_.setCircularBufferMemoryFootprint(options_.circularBufferSizeMB_);
                } catch (Exception exc) {
                   ReportingUtils.showError(exc);
                }
             }
          }
       });
       optionsMenuItem.setText("Options...");
       toolsMenu.add(optionsMenuItem);
 
       final JLabel binningLabel = new JLabel();
       binningLabel.setFont(new Font("Arial", Font.PLAIN, 10));
       binningLabel.setText("Binning");
       topPanel.add(binningLabel);
       topLayout.putConstraint(SpringLayout.SOUTH, binningLabel, 64,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, binningLabel, 43,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, binningLabel, 200 - 1,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, binningLabel, 112 - 1,
             SpringLayout.WEST, topPanel);
 
       labelImageDimensions_ = new JLabel();
       labelImageDimensions_.setFont(new Font("Arial", Font.PLAIN, 10));
       bottomPanel.add(labelImageDimensions_);
       topLayout.putConstraint(SpringLayout.SOUTH, labelImageDimensions_,
             -5, SpringLayout.SOUTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.NORTH, labelImageDimensions_,
             -25, SpringLayout.SOUTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.EAST, labelImageDimensions_,
             -5, SpringLayout.EAST, bottomPanel);
       topLayout.putConstraint(SpringLayout.WEST, labelImageDimensions_,
             5, SpringLayout.WEST, bottomPanel);
 
       comboBinning_ = new JComboBox();
       comboBinning_.setFont(new Font("Arial", Font.PLAIN, 10));
       comboBinning_.setMaximumRowCount(4);
       comboBinning_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             changeBinning();
          }
       });
       topPanel.add(comboBinning_);
       topLayout.putConstraint(SpringLayout.EAST, comboBinning_, 275,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, comboBinning_, 200,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, comboBinning_, 66,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, comboBinning_, 43,
             SpringLayout.NORTH, topPanel);
 
 
 
       final JLabel cameraSettingsLabel = new JLabel();
       cameraSettingsLabel.setFont(new Font("Arial", Font.BOLD, 11));
       cameraSettingsLabel.setText("Camera settings");
       topPanel.add(cameraSettingsLabel);
       topLayout.putConstraint(SpringLayout.EAST, cameraSettingsLabel,
             211, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, cameraSettingsLabel, 6,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, cameraSettingsLabel,
             109, SpringLayout.WEST, topPanel);
 
 
       configPad_ = new ConfigGroupPad();
 
       configPad_.setFont(new Font("", Font.PLAIN, 10));
       topPanel.add(configPad_);
       topLayout.putConstraint(SpringLayout.EAST, configPad_, -4,
             SpringLayout.EAST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, configPad_, 5,
             SpringLayout.EAST, comboBinning_);
       topLayout.putConstraint(SpringLayout.SOUTH, configPad_, -21,
             SpringLayout.SOUTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, configPad_, 21,
             SpringLayout.NORTH, topPanel);
 
       configPadButtonPanel_ = new ConfigPadButtonPanel();
       configPadButtonPanel_.setConfigPad(configPad_);
       configPadButtonPanel_.setGUI(this);
       topPanel.add(configPadButtonPanel_);
       topLayout.putConstraint(SpringLayout.EAST, configPadButtonPanel_, -4,
             SpringLayout.EAST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, configPadButtonPanel_, 5,
             SpringLayout.EAST, comboBinning_);
       topLayout.putConstraint(SpringLayout.SOUTH, configPadButtonPanel_, 0,
             SpringLayout.SOUTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, configPadButtonPanel_, -18,
             SpringLayout.SOUTH, topPanel);
 
 
       final JLabel stateDeviceLabel = new JLabel();
       stateDeviceLabel.setFont(new Font("Arial", Font.BOLD, 11));
       stateDeviceLabel.setText("Configuration settings");
       topPanel.add(stateDeviceLabel);
       topLayout.putConstraint(SpringLayout.SOUTH, stateDeviceLabel, 0,
             SpringLayout.SOUTH, cameraSettingsLabel);
       topLayout.putConstraint(SpringLayout.NORTH, stateDeviceLabel, 0,
             SpringLayout.NORTH, cameraSettingsLabel);
       topLayout.putConstraint(SpringLayout.EAST, stateDeviceLabel, 150,
             SpringLayout.WEST, configPad_);
       topLayout.putConstraint(SpringLayout.WEST, stateDeviceLabel, 0,
             SpringLayout.WEST, configPad_);
 
 
       final JButton buttonAcqSetup = new JButton();
       buttonAcqSetup.setMargin(new Insets(2, 2, 2, 2));
       buttonAcqSetup.setIconTextGap(1);
       buttonAcqSetup.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class, "/org/micromanager/icons/film.png"));
       buttonAcqSetup.setToolTipText("Open Acquistion dialog");
       buttonAcqSetup.setFont(new Font("Arial", Font.PLAIN, 10));
       buttonAcqSetup.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             openAcqControlDialog();
          }
       });
       buttonAcqSetup.setText("Multi-D Acq.");
       topPanel.add(buttonAcqSetup);
       topLayout.putConstraint(SpringLayout.SOUTH, buttonAcqSetup, 91,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, buttonAcqSetup, 70,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, buttonAcqSetup, 95,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, buttonAcqSetup, 7,
             SpringLayout.WEST, topPanel);
 
       autoShutterCheckBox_ = new JCheckBox();
       autoShutterCheckBox_.setFont(new Font("Arial", Font.PLAIN, 10));
       autoShutterCheckBox_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             core_.setAutoShutter(autoShutterCheckBox_.isSelected());
             if (shutterLabel_.length() > 0) {
                try {
                   setShutterButton(core_.getShutterOpen());
                } catch (Exception e1) {
                   ReportingUtils.showError(e1);
                }
             }
             if (autoShutterCheckBox_.isSelected()) {
                toggleButtonShutter_.setEnabled(false);
             } else {
                toggleButtonShutter_.setEnabled(true);
             }
          }
       });
       autoShutterCheckBox_.setIconTextGap(6);
       autoShutterCheckBox_.setHorizontalTextPosition(SwingConstants.LEADING);
       autoShutterCheckBox_.setText("Auto shutter");
       topPanel.add(autoShutterCheckBox_);
       topLayout.putConstraint(SpringLayout.EAST, autoShutterCheckBox_,
             202 - 3, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, autoShutterCheckBox_,
             110 - 3, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, autoShutterCheckBox_,
             141 - 22, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, autoShutterCheckBox_,
             118 - 22, SpringLayout.NORTH, topPanel);
 
     
       final JButton refreshButton = new JButton();
       refreshButton.setMargin(new Insets(2, 2, 2, 2));
       refreshButton.setIconTextGap(1);
       refreshButton.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/arrow_refresh.png"));
       refreshButton.setFont(new Font("Arial", Font.PLAIN, 10));
       refreshButton.setToolTipText("Refresh all GUI controls directly from the hardware");
       refreshButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             updateGUI(true);
          }
       });
       refreshButton.setText("Refresh");
       topPanel.add(refreshButton);
       topLayout.putConstraint(SpringLayout.SOUTH, refreshButton, 113,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, refreshButton, 92,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, refreshButton, 95,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, refreshButton, 7,
             SpringLayout.WEST, topPanel);
 
       JLabel citePleaLabel_ = new JLabel("<html>Please <a href=\"http://micro-manager.org\">cite Micro-Manager</a> so funding will continue!</html>");
       topPanel.add(citePleaLabel_);
       citePleaLabel_.setFont(new Font("Arial", Font.PLAIN, 11));
       topLayout.putConstraint(SpringLayout.SOUTH, citePleaLabel_, 139,
               SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, citePleaLabel_, 119,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, citePleaLabel_, 270,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, citePleaLabel_, 7,
             SpringLayout.WEST, topPanel);
 
       class Pleader extends Thread{
          Pleader(){
             super("pleader");
          }
          @Override
          public void run(){
           try {
                ij.plugin.BrowserLauncher.openURL("https://valelab.ucsf.edu/~MM/MMwiki/index.php/Citing_Micro-Manager");
             } catch (IOException e1) {
                ReportingUtils.showError(e1);
             }
          }
 
       }
       citePleaLabel_.addMouseListener(new MouseAdapter() {
          @Override
           public void mousePressed(MouseEvent e) {
              Pleader p = new Pleader();
              p.start();
           }
       });
 
       // add window listeners
       addWindowListener(new WindowAdapter() {
 
          @Override
          public void windowClosing(WindowEvent e) {
             running_ = false;
             closeSequence();
          }
 
          @Override
          public void windowOpened(WindowEvent e) {
             // -------------------
             // initialize hardware
             // -------------------
             try {
                core_ = new CMMCore();
             } catch(UnsatisfiedLinkError ex) {
                ReportingUtils.showError(ex, "Failed to open libMMCoreJ_wrap.jnilib");
                return;
             }
             ReportingUtils.setCore(core_);
 
             core_.enableDebugLog(options_.debugLogEnabled_);
             core_.logMessage("MM Studio version: " + getVersion());
             core_.logMessage(core_.getVersionInfo());
             core_.logMessage(core_.getAPIVersionInfo());
             core_.logMessage("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
 
             cameraLabel_ = "";
             shutterLabel_ = "";
             zStageLabel_ = "";
             xyStageLabel_ = "";
             engine_ = new AcquisitionWrapperEngine();
 
             // register callback for MMCore notifications, this is a global
             // to avoid garbage collection
             cb_ = new CoreEventCallback();
             core_.registerCallback(cb_);
 
             try {
                core_.setCircularBufferMemoryFootprint(options_.circularBufferSizeMB_);
             } catch (Exception e2) {
                ReportingUtils.showError(e2);
             }
 
             MMStudioMainFrame parent = (MMStudioMainFrame) e.getWindow();
             if (parent != null) {
                engine_.setParentGUI(parent);
             }
 
             loadMRUConfigFiles();
             initializePlugins();
 
             toFront();
             
             if (!options_.doNotAskForConfigFile_) {
                MMIntroDlg introDlg = new MMIntroDlg(VERSION, MRUConfigFiles_);
                introDlg.setConfigFile(sysConfigFile_);
                introDlg.setBackground(guiColors_.background.get((options_.displayBackground_)));
                introDlg.setVisible(true);
                sysConfigFile_ = introDlg.getConfigFile();
             }
             saveMRUConfigFiles();
 
             mainPrefs_.put(SYSTEM_CONFIG_FILE, sysConfigFile_);
 
             paint(MMStudioMainFrame.this.getGraphics());
 
             afMgr_ = new AutofocusManager(core_);
             engine_.setCore(core_, afMgr_);
             posList_ = new PositionList();
             engine_.setPositionList(posList_);
             // load (but do no show) the scriptPanel
             createScriptPanel();
 
             // Create an instance of HotKeys so that they can be read in from prefs
             hotKeys_ = new org.micromanager.utils.HotKeys();
             hotKeys_.loadSettings();
 
             // if an error occurred during config loading, 
             // do not display more errors than needed
             if (!loadSystemConfiguration())
                ReportingUtils.showErrorOn(false);
 
             executeStartupScript();
 
 
             // Create Multi-D window here but do not show it.
             // This window needs to be created in order to properly set the "ChannelGroup"
             // based on the Multi-D parameters
             acqControlWin_ = new AcqControlDlg(engine_, mainPrefs_, MMStudioMainFrame.this);
             addMMBackgroundListener(acqControlWin_);
 
             configPad_.setCore(core_);
             if (parent != null) {
                configPad_.setParentGUI(parent);
             }
 
             configPadButtonPanel_.setCore(core_);
 
             // initialize controls
             initializeGUI();
             initializeHelpMenu();
             
             String afDevice = mainPrefs_.get(AUTOFOCUS_DEVICE, "");
             if (afMgr_.hasDevice(afDevice)) {
                try {
                   afMgr_.selectDevice(afDevice);
                } catch (MMException e1) {
                   // this error should never happen
                   ReportingUtils.showError(e1);
                }
             }
             
             // switch error reporting back on
             ReportingUtils.showErrorOn(true);
          }
 
          private void initializePlugins() {
             pluginMenu_ = new JMenu();
             pluginMenu_.setText("Plugins");
             menuBar_.add(pluginMenu_);
             new Thread("Plugin loading") {
                public void run() {
                   // Needed for loading clojure-based jars:
                   Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                   loadPlugins();
                }
             }.start();
          }
 
         
       });
 
       final JButton setRoiButton = new JButton();
       setRoiButton.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/shape_handles.png"));
       setRoiButton.setFont(new Font("Arial", Font.PLAIN, 10));
       setRoiButton.setToolTipText("Set Region Of Interest to selected rectangle");
       setRoiButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             setROI();
          }
       });
       topPanel.add(setRoiButton);
       topLayout.putConstraint(SpringLayout.EAST, setRoiButton, 37,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, setRoiButton, 7,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, setRoiButton, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, setRoiButton, 154,
             SpringLayout.NORTH, topPanel);
 
       final JButton clearRoiButton = new JButton();
       clearRoiButton.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/arrow_out.png"));
       clearRoiButton.setFont(new Font("Arial", Font.PLAIN, 10));
       clearRoiButton.setToolTipText("Reset Region of Interest to full frame");
       clearRoiButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             clearROI();
          }
       });
       topPanel.add(clearRoiButton);
       topLayout.putConstraint(SpringLayout.EAST, clearRoiButton, 70,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, clearRoiButton, 40,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.SOUTH, clearRoiButton, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, clearRoiButton, 154,
             SpringLayout.NORTH, topPanel);
 
       final JLabel regionOfInterestLabel = new JLabel();
       regionOfInterestLabel.setFont(new Font("Arial", Font.BOLD, 11));
       regionOfInterestLabel.setText("ROI");
       topPanel.add(regionOfInterestLabel);
       topLayout.putConstraint(SpringLayout.SOUTH, regionOfInterestLabel,
             154, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, regionOfInterestLabel,
             140, SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, regionOfInterestLabel,
             71, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, regionOfInterestLabel,
             8, SpringLayout.WEST, topPanel);
 
 
       contrastPanel_ = new ContrastPanel();
       contrastPanel_.setFont(new Font("", Font.PLAIN, 10));
       contrastPanel_.setContrastStretch(stretch);
       contrastPanel_.setBorder(BorderFactory.createEmptyBorder());
       bottomPanel.add(contrastPanel_);
       topLayout.putConstraint(SpringLayout.SOUTH, contrastPanel_, -20,
             SpringLayout.SOUTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.NORTH, contrastPanel_, 0,
             SpringLayout.NORTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.EAST, contrastPanel_, 0,
             SpringLayout.EAST, bottomPanel);
       topLayout.putConstraint(SpringLayout.WEST, contrastPanel_, 0,
             SpringLayout.WEST, bottomPanel);
 
       
       metadataPanel_ = new MetadataPanel();
       metadataPanel_.setVisible(false);
 
       bottomPanel.add(metadataPanel_);
       topLayout.putConstraint(SpringLayout.SOUTH, metadataPanel_, -20,
             SpringLayout.SOUTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.NORTH, metadataPanel_, 0,
             SpringLayout.NORTH, bottomPanel);
       topLayout.putConstraint(SpringLayout.EAST, metadataPanel_, 0,
             SpringLayout.EAST, bottomPanel);
       topLayout.putConstraint(SpringLayout.WEST, metadataPanel_, 0,
             SpringLayout.WEST, bottomPanel);
       metadataPanel_.setBorder(BorderFactory.createEmptyBorder());
 
       GUIUtils.registerImageFocusListener(new ImageFocusListener() {
          public void focusReceived(ImageWindow focusedWindow) {
             if (focusedWindow == null) {
                contrastPanel_.setVisible(true);
                metadataPanel_.setVisible(false);
             } else if (focusedWindow instanceof MMImageWindow) {
                contrastPanel_.setVisible(true);
                metadataPanel_.setVisible(false);
             } else if (focusedWindow.getImagePlus().getStack() instanceof AcquisitionVirtualStack) {
                contrastPanel_.setVisible(false);
                metadataPanel_.setVisible(true);
             }
          }
       });
 
 
       final JLabel regionOfInterestLabel_1 = new JLabel();
       regionOfInterestLabel_1.setFont(new Font("Arial", Font.BOLD, 11));
       regionOfInterestLabel_1.setText("Zoom");
       topPanel.add(regionOfInterestLabel_1);
       topLayout.putConstraint(SpringLayout.SOUTH,
             regionOfInterestLabel_1, 154, SpringLayout.NORTH,
             topPanel);
       topLayout.putConstraint(SpringLayout.NORTH,
             regionOfInterestLabel_1, 140, SpringLayout.NORTH,
             topPanel);
       topLayout.putConstraint(SpringLayout.EAST, regionOfInterestLabel_1,
             139, SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, regionOfInterestLabel_1,
             81, SpringLayout.WEST, topPanel);
 
       final JButton zoomInButton = new JButton();
       zoomInButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             zoomIn();
          }
       });
       zoomInButton.setIcon(SwingResourceManager.getIcon(MMStudioMainFrame.class,
             "/org/micromanager/icons/zoom_in.png"));
       zoomInButton.setToolTipText("Zoom in");
       zoomInButton.setFont(new Font("Arial", Font.PLAIN, 10));
       topPanel.add(zoomInButton);
       topLayout.putConstraint(SpringLayout.SOUTH, zoomInButton, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, zoomInButton, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, zoomInButton, 110,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, zoomInButton, 80,
             SpringLayout.WEST, topPanel);
 
       final JButton zoomOutButton = new JButton();
       zoomOutButton.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             zoomOut();
          }
       });
       zoomOutButton.setIcon(SwingResourceManager.getIcon(MMStudioMainFrame.class,
             "/org/micromanager/icons/zoom_out.png"));
       zoomOutButton.setToolTipText("Zoom out");
       zoomOutButton.setFont(new Font("Arial", Font.PLAIN, 10));
       topPanel.add(zoomOutButton);
       topLayout.putConstraint(SpringLayout.SOUTH, zoomOutButton, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, zoomOutButton, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, zoomOutButton, 143,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, zoomOutButton, 113,
             SpringLayout.WEST, topPanel);
 
       // Profile
       // -------
 
       final JLabel profileLabel_ = new JLabel();
       profileLabel_.setFont(new Font("Arial", Font.BOLD, 11));
       profileLabel_.setText("Profile");
       topPanel.add(profileLabel_);
       topLayout.putConstraint(SpringLayout.SOUTH, profileLabel_, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, profileLabel_, 140,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, profileLabel_, 217,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, profileLabel_, 154,
             SpringLayout.WEST, topPanel);
 
       final JButton buttonProf = new JButton();
       buttonProf.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/chart_curve.png"));
       buttonProf.setFont(new Font("Arial", Font.PLAIN, 10));
       buttonProf.setToolTipText("Open line profile window (requires line selection)");
       buttonProf.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             openLineProfileWindow();
          }
       });
       // buttonProf.setText("Profile");
       topPanel.add(buttonProf);
       topLayout.putConstraint(SpringLayout.SOUTH, buttonProf, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, buttonProf, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, buttonProf, 183,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, buttonProf, 153,
             SpringLayout.WEST, topPanel);
 
       // Autofocus
       // -------
 
       final JLabel autofocusLabel_ = new JLabel();
       autofocusLabel_.setFont(new Font("Arial", Font.BOLD, 11));
       autofocusLabel_.setText("Autofocus");
       topPanel.add(autofocusLabel_);
       topLayout.putConstraint(SpringLayout.SOUTH, autofocusLabel_, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, autofocusLabel_, 140,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, autofocusLabel_, 274,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, autofocusLabel_, 194,
             SpringLayout.WEST, topPanel);
 
       buttonAutofocus_ = new JButton();
       buttonAutofocus_.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/find.png"));
       buttonAutofocus_.setFont(new Font("Arial", Font.PLAIN, 10));
       buttonAutofocus_.setToolTipText("Autofocus now");
       buttonAutofocus_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             if (afMgr_.getDevice() != null) {
                new Thread() {
                   @Override
                   public void run() {
                      try {
                        boolean lmo  = isLiveModeOn();
                       if(lmo)
                            enableLiveMode(false);
                        afMgr_.getDevice().fullFocus();
                        if(lmo)
                            enableLiveMode(true);
                      } catch (MMException ex) {
                         ReportingUtils.logError(ex);
                      }
                   }
                }.start(); // or any other method from Autofocus.java API
             }
          }
       });
       topPanel.add(buttonAutofocus_);
       topLayout.putConstraint(SpringLayout.SOUTH, buttonAutofocus_, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, buttonAutofocus_, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, buttonAutofocus_, 223,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, buttonAutofocus_, 193,
             SpringLayout.WEST, topPanel);
 
       buttonAutofocusTools_ = new JButton();
       buttonAutofocusTools_.setIcon(SwingResourceManager.getIcon(
             MMStudioMainFrame.class,
             "/org/micromanager/icons/wrench_orange.png"));
       buttonAutofocusTools_.setFont(new Font("Arial", Font.PLAIN, 10));
       buttonAutofocusTools_.setToolTipText("Set autofocus options");
       buttonAutofocusTools_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent e) {
             showAutofocusDialog();
          }
       });
       topPanel.add(buttonAutofocusTools_);
       topLayout.putConstraint(SpringLayout.SOUTH, buttonAutofocusTools_, 174,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, buttonAutofocusTools_, 154,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, buttonAutofocusTools_, 256,
             SpringLayout.WEST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, buttonAutofocusTools_, 226,
             SpringLayout.WEST, topPanel);
 
       saveConfigButton_ = new JButton();
       saveConfigButton_.addActionListener(new ActionListener() {
 
          public void actionPerformed(ActionEvent arg0) {
             saveConfigPresets();
          }
       });
       saveConfigButton_.setToolTipText("Save current presets to the configuration file");
       saveConfigButton_.setText("Save");
       saveConfigButton_.setEnabled(false);
       topPanel.add(saveConfigButton_);
       topLayout.putConstraint(SpringLayout.SOUTH, saveConfigButton_, 20,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.NORTH, saveConfigButton_, 2,
             SpringLayout.NORTH, topPanel);
       topLayout.putConstraint(SpringLayout.EAST, saveConfigButton_, -5,
             SpringLayout.EAST, topPanel);
       topLayout.putConstraint(SpringLayout.WEST, saveConfigButton_, -80,
             SpringLayout.EAST, topPanel);
 
       // Add our own keyboard manager that handles Micro-Manager shortcuts
       MMKeyDispatcher mmKD = new MMKeyDispatcher(gui_);
       KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(mmKD);
 
    }
 
    private void handleException(Exception e, String msg) {
       String errText = "Exception occurred: ";
       if (msg.length() > 0) {
          errText += msg + " -- ";
       }
       if (options_.debugLogEnabled_) {
          errText += e.getMessage();
       } else {
          errText += e.toString() + "\n";
          ReportingUtils.showError(e);
       }
       handleError(errText);
    }
 
    private void handleException(Exception e) {
       handleException(e, "");
    }
 
    private void handleError(String message) {
       if (isLiveModeOn()) {
          // Should we always stop live mode on any error?
          enableLiveMode(false);
       }
       JOptionPane.showMessageDialog(this, message);
       core_.logMessage(message);
    }
 
    public void makeActive() {
       toFront();
    }
 
    private void setExposure() {
       try {
          core_.setExposure(NumberUtils.displayStringToDouble(textFieldExp_.getText()));
 
          // Display the new exposure time
          double exposure = core_.getExposure();
          textFieldExp_.setText(NumberUtils.doubleToDisplayString(exposure));
 
          // Interval for Live Mode changes as well
          setLiveModeInterval();
 
       } catch (Exception exp) {
          // Do nothing.
       }
    }
 
    public boolean getConserveRamOption() {
       return options_.conserveRam_;
    }
 
    private void updateTitle() {
       this.setTitle("System: " + sysConfigFile_);
    }
 
    private void updateLineProfile() {
       if (!isImageWindowOpen() || profileWin_ == null
             || !profileWin_.isShowing()) {
          return;
       }
 
       calculateLineProfileData(imageWin_.getImagePlus());
       profileWin_.setData(lineProfileData_);
    }
 
    private void openLineProfileWindow() {
       if (imageWin_ == null || imageWin_.isClosed()) {
          return;
       }
       calculateLineProfileData(imageWin_.getImagePlus());
       if (lineProfileData_ == null) {
          return;
       }
       profileWin_ = new GraphFrame();
       profileWin_.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
       profileWin_.setData(lineProfileData_);
       profileWin_.setAutoScale();
       profileWin_.setTitle("Live line profile");
       profileWin_.setBackground(guiColors_.background.get((options_.displayBackground_)));
       addMMBackgroundListener(profileWin_);
       profileWin_.setVisible(true);
    }
 
    public Rectangle getROI() throws Exception {
       // ROI values are give as x,y,w,h in individual one-member arrays (pointers in C++):
       int[][] a = new int[4][1];
       core_.getROI(a[0], a[1], a[2], a[3]);
       // Return as a single array with x,y,w,h:
       return new Rectangle(a[0][0], a[1][0], a[2][0], a[3][0]);
    }
 
    private void calculateLineProfileData(ImagePlus imp) {
       // generate line profile
       Roi roi = imp.getRoi();
       if (roi == null || !roi.isLine()) {
 
          // if there is no line ROI, create one
          Rectangle r = imp.getProcessor().getRoi();
          int iWidth = r.width;
          int iHeight = r.height;
          int iXROI = r.x;
          int iYROI = r.y;
          if (roi == null) {
             iXROI += iWidth / 2;
             iYROI += iHeight / 2;
          }
 
          roi = new Line(iXROI - iWidth / 4, iYROI - iWidth / 4, iXROI
                + iWidth / 4, iYROI + iHeight / 4);
          imp.setRoi(roi);
          roi = imp.getRoi();
       }
 
       ImageProcessor ip = imp.getProcessor();
       ip.setInterpolate(true);
       Line line = (Line) roi;
 
       if (lineProfileData_ == null) {
          lineProfileData_ = new GraphData();
       }
       lineProfileData_.setData(line.getPixels());
    }
 
    public void setROI(Rectangle r) throws Exception {
       boolean liveRunning = false;
       if (liveRunning_) {
          liveRunning = liveRunning_;
          enableLiveMode(false);
       }
       core_.setROI(r.x, r.y, r.width, r.height);
       updateStaticInfo();
       if (liveRunning) {
          enableLiveMode(true);
       }
 
    }
 
    private void setROI() {
       ImagePlus curImage = WindowManager.getCurrentImage();
       if (curImage == null) {
          return;
       }
 
       Roi roi = curImage.getRoi();
 
       try {
          if (roi == null) {
             // if there is no ROI, create one
             Rectangle r = curImage.getProcessor().getRoi();
             int iWidth = r.width;
             int iHeight = r.height;
             int iXROI = r.x;
             int iYROI = r.y;
             if (roi == null) {
                iWidth /= 2;
                iHeight /= 2;
                iXROI += iWidth / 2;
                iYROI += iHeight / 2;
             }
 
             curImage.setRoi(iXROI, iYROI, iWidth, iHeight);
             roi = curImage.getRoi();
          }
 
          if (roi.getType() != Roi.RECTANGLE) {
             handleError("ROI must be a rectangle.\nUse the ImageJ rectangle tool to draw the ROI.");
             return;
          }
 
          Rectangle r = roi.getBoundingRect();
          // Stop (and restart) live mode if it is running
          setROI(r);
 
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
    }
 
    private void clearROI() {
       try {
          boolean liveRunning = false;
          if (liveRunning_) {
             liveRunning = liveRunning_;
             enableLiveMode(false);
          }
          core_.clearROI();
          updateStaticInfo();
          if (liveRunning) {
             enableLiveMode(true);
          }
 
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
    }
 
    private BooleanLock creatingImageWindow_ = new BooleanLock(false);
    private static long waitForCreateImageWindowTimeout_ = 5000;
 
    private MMImageWindow createImageWindow() {
       if (creatingImageWindow_.isTrue()) {
          try {
             creatingImageWindow_.waitToSetFalse(waitForCreateImageWindowTimeout_);
          } catch (Exception e) {
             ReportingUtils.showError(e);
          }
          return imageWin_;
       }
       creatingImageWindow_.setValue(true);
       MMImageWindow win = imageWin_;
       removeMMBackgroundListener(imageWin_);
       imageWin_ = null;
       try {
          if (win != null) {
             win.saveAttributes();
         //    WindowManager.removeWindow(win);
 
            //    win.close();
 
             win.dispose();
             win = null;
          }
 
          win = new MMImageWindow(core_, this);
 
          core_.logMessage("createImageWin1");
 
          win.setBackground(guiColors_.background.get((options_.displayBackground_)));
          addMMBackgroundListener(win);
          setIJCal(win);
 
          // listeners
          if (centerAndDragListener_ != null
                && centerAndDragListener_.isRunning()) {
             centerAndDragListener_.attach(win.getImagePlus().getWindow());
          }
          if (zWheelListener_ != null && zWheelListener_.isRunning()) {
             zWheelListener_.attach(win.getImagePlus().getWindow());
          }
          if (xyzKeyListener_ != null && xyzKeyListener_.isRunning()) {
             xyzKeyListener_.attach(win.getImagePlus().getWindow());
          }
 
          win.getCanvas().requestFocus();
          imageWin_ = win;
 
       } catch (Exception e) {
          if (win != null) {
             win.saveAttributes();
             WindowManager.removeWindow(win);
             win.dispose();
          }
          ReportingUtils.showError(e);
       }
       creatingImageWindow_.setValue(false);
       return imageWin_;
    }
 
    /**
     * Returns instance of the core uManager object;
     */
    public CMMCore getMMCore() {
       return core_;
    }
 
    /**
     * Returns singleton instance of MMStudioMainFrame
     */
    public static MMStudioMainFrame getInstance() {
       return gui_;
    }
 
 
    public final void setExitStrategy(boolean closeOnExit) {
       if (closeOnExit)
          setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       else
          setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
 
    public void saveConfigPresets() {
       MicroscopeModel model = new MicroscopeModel();
       try {
          model.loadFromFile(sysConfigFile_);
          model.createSetupConfigsFromHardware(core_);
          model.createResolutionsFromHardware(core_);
          File f = FileDialogs.save(this, "Save the configuration file", MM_CONFIG_FILE);
          if (f != null) {
             model.saveToFile(f.getAbsolutePath());
             sysConfigFile_ = f.getAbsolutePath();
             mainPrefs_.put(SYSTEM_CONFIG_FILE, sysConfigFile_);
             configChanged_ = false;
             setConfigSaveButtonStatus(configChanged_);
             updateTitle();
          }
       } catch (MMConfigFileException e) {
          ReportingUtils.showError(e);
       }
    }
 
    protected void setConfigSaveButtonStatus(boolean changed) {
       saveConfigButton_.setEnabled(changed);
    }
 
    public String getAcqDirectory() {
       return openAcqDirectory_;
    }
 
    public void setAcqDirectory(String dir) {
       openAcqDirectory_ = dir;
    }
 
       /**
     * Open an existing acquisition directory and build image5d window.
     *
     */
    public void openAcquisitionData() {
 
       // choose the directory
       // --------------------
       File f = FileDialogs.openDir(this, "Please select an image data set", MM_DATA_SET);
 
       if (f != null) {
          if (f.isDirectory()) {
             openAcqDirectory_ = f.getAbsolutePath();
          } else {
             openAcqDirectory_ = f.getParent();
          }
 
          openAcquisitionData(openAcqDirectory_);
          
       }
    }
 
    public void openAcquisitionData(String dir) {
                String rootDir = new File(dir).getAbsolutePath();
          String name = new File(dir).getName();
          rootDir= rootDir.substring(0, rootDir.length() - (name.length() + 1));
          try {
             acqMgr_.openAcquisition(name, rootDir, true, true, true);
             acqMgr_.getAcquisition(name).initialize();
             acqMgr_.closeAcquisition(name);
          } catch (MMScriptException ex) {
             ReportingUtils.showError(ex);
          }
    }
 
    protected void zoomOut() {
       ImageWindow curWin = WindowManager.getCurrentWindow();
       if (curWin != null) {
          ImageCanvas canvas = curWin.getCanvas();
          Rectangle r = canvas.getBounds();
          canvas.zoomOut(r.width / 2, r.height / 2);
       }
    }
 
    protected void zoomIn() {
       ImageWindow curWin = WindowManager.getCurrentWindow();
       if (curWin != null) {
          ImageCanvas canvas = curWin.getCanvas();
          Rectangle r = canvas.getBounds();
          canvas.zoomIn(r.width / 2, r.height / 2);
       }
    }
 
    protected void changeBinning() {
       try {
          boolean liveRunning = false;
          if (liveRunning_) {
             liveRunning = liveRunning_;
             enableLiveMode(false);
          }
 
          if (isCameraAvailable()) {
             Object item = comboBinning_.getSelectedItem();
             if (item != null) {
                core_.setProperty(cameraLabel_, MMCoreJ.getG_Keyword_Binning(), item.toString());
             }
          }
 
          updateStaticInfo();
 
          if (liveRunning) {
             enableLiveMode(true);
          }
 
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
 
    }
 
    private void createPropertyEditor() {
       if (propertyBrowser_ != null) {
          propertyBrowser_.dispose();
       }
 
       propertyBrowser_ = new PropertyEditor();
       propertyBrowser_.setGui(this);
       propertyBrowser_.setVisible(true);
       propertyBrowser_.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
       propertyBrowser_.setCore(core_);
    }
 
    private void createCalibrationListDlg() {
       if (calibrationListDlg_ != null) {
          calibrationListDlg_.dispose();
       }
 
       calibrationListDlg_ = new CalibrationListDlg(core_);
       calibrationListDlg_.setVisible(true);
       calibrationListDlg_.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
       calibrationListDlg_.setParentGUI(this);
    }
 
    public CalibrationListDlg getCalibrationListDlg() {
       if (calibrationListDlg_ == null) {
          createCalibrationListDlg();
       }
       return calibrationListDlg_;
    }
 
    private void createScriptPanel() {
       if (scriptPanel_ == null) {
          scriptPanel_ = new ScriptPanel(core_, options_, this);
          scriptPanel_.insertScriptingObject(SCRIPT_CORE_OBJECT, core_);
          scriptPanel_.insertScriptingObject(SCRIPT_ACQENG_OBJECT, engine_);
          scriptPanel_.setParentGUI(this);
          scriptPanel_.setBackground(guiColors_.background.get((options_.displayBackground_)));
          addMMBackgroundListener(scriptPanel_);
 
       }
    }
 
    /**
     * Updates Status line in main window from cached values
     */
    private void updateStaticInfoFromCache() {
       String dimText = "Image size: " + staticInfo_.width_ + " X " + staticInfo_.height_ + " X "
             + staticInfo_.bytesPerPixel_ + ", Intensity range: " + staticInfo_.imageBitDepth_ + " bits";
       dimText += ", " + TextUtils.FMT0.format(staticInfo_.pixSizeUm_ * 1000) + "nm/pix";
       if (zStageLabel_.length() > 0) {
          dimText += ", Z=" + TextUtils.FMT2.format(staticInfo_.zPos_) + "um";
       }
       if (xyStageLabel_.length() > 0) {
          dimText += ", XY=(" + TextUtils.FMT2.format(staticInfo_.x_) + "," + TextUtils.FMT2.format(staticInfo_.y_) + ")um";
       }
 
       labelImageDimensions_.setText(dimText);
    }
 
    public void updateXYPos(double x, double y) {
       staticInfo_.x_ = x;
       staticInfo_.y_ = y;
 
       updateStaticInfoFromCache();
    }
 
    public void updateZPos(double z) {
       staticInfo_.zPos_ = z;
 
       updateStaticInfoFromCache();
    }
 
    public void updateXYPosRelative(double x, double y) {
       staticInfo_.x_ += x;
       staticInfo_.y_ += y;
 
       updateStaticInfoFromCache();
    }
 
    public void updateZPosRelative(double z) {
       staticInfo_.zPos_ += z;
 
       updateStaticInfoFromCache();
    }
 
    public void updateXYStagePosition(){
 
       double x[] = new double[1];
       double y[] = new double[1];
       try {
          if (xyStageLabel_.length() > 0) 
             core_.getXYPosition(xyStageLabel_, x, y);
       } catch (Exception e) {
           ReportingUtils.showError(e);
       }
 
       staticInfo_.x_ = x[0];
       staticInfo_.y_ = y[0];
       updateStaticInfoFromCache();
    }
 
    private void updatePixSizeUm (double pixSizeUm) {
       staticInfo_.pixSizeUm_ = pixSizeUm;
 
       updateStaticInfoFromCache();
    }
 
    private void updateStaticInfo() {
       double zPos = 0.0;
       double x[] = new double[1];
       double y[] = new double[1];
 
       try {
          if (zStageLabel_.length() > 0) {
             zPos = core_.getPosition(zStageLabel_);
          }
          if (xyStageLabel_.length() > 0) {
             core_.getXYPosition(xyStageLabel_, x, y);
          }
       } catch (Exception e) {
          handleException(e);
       }
 
       staticInfo_.width_ = core_.getImageWidth();
       staticInfo_.height_ = core_.getImageHeight();
       staticInfo_.bytesPerPixel_ = core_.getBytesPerPixel();
       staticInfo_.imageBitDepth_ = core_.getImageBitDepth();
       staticInfo_.pixSizeUm_ = core_.getPixelSizeUm();
       staticInfo_.zPos_ = zPos;
       staticInfo_.x_ = x[0];
       staticInfo_.y_ = y[0];
 
       updateStaticInfoFromCache();
    }
 
    public void toggleShutter() {
       try {
          if (!toggleButtonShutter_.isEnabled())
             return;
          toggleButtonShutter_.requestFocusInWindow();
          if (toggleButtonShutter_.getText().equals("Open")) {
             setShutterButton(true);
             core_.setShutterOpen(true);
          } else {
             core_.setShutterOpen(false);
             setShutterButton(false);
          }
       } catch (Exception e1) {
          ReportingUtils.showError(e1);
       }
    }
 
    private void setShutterButton(boolean state) {
       if (state) {
          toggleButtonShutter_.setSelected(true);
          toggleButtonShutter_.setText("Close");
       } else {
          toggleButtonShutter_.setSelected(false);
          toggleButtonShutter_.setText("Open");
       }
    }
 
    // //////////////////////////////////////////////////////////////////////////
    // public interface available for scripting access
    // //////////////////////////////////////////////////////////////////////////
    public void snapSingleImage() {
       doSnap();
    }
 
    public Object getPixels() {
       if (imageWin_ != null) {
          return imageWin_.getImagePlus().getProcessor().getPixels();
       }
 
       return null;
    }
 
    public void setPixels(Object obj) {
       if (imageWin_ == null) {
          return;
       }
       imageWin_.getImagePlus().getProcessor().setPixels(obj);
    }
 
    public int getImageHeight() {
       if (imageWin_ != null) {
          return imageWin_.getImagePlus().getHeight();
       }
       return 0;
    }
 
    public int getImageWidth() {
       if (imageWin_ != null) {
          return imageWin_.getImagePlus().getWidth();
       }
       return 0;
    }
 
    public int getImageDepth() {
       if (imageWin_ != null) {
          return imageWin_.getImagePlus().getBitDepth();
       }
       return 0;
    }
 
    public ImageProcessor getImageProcessor() {
       if (imageWin_ == null) {
          return null;
       }
       return imageWin_.getImagePlus().getProcessor();
    }
 
    private boolean isCameraAvailable() {
       return cameraLabel_.length() > 0;
    }
 
    public boolean isImageWindowOpen() {
       boolean ret = imageWin_ != null;
       ret = ret && !imageWin_.isClosed();
       if (ret) {
          try {
             Graphics g = imageWin_.getGraphics();
             if (null != g) {
                int ww = imageWin_.getWidth();
                g.clearRect(0, 0, ww, 40);
                imageWin_.drawInfo(g);
             } else {
                // explicitly clean up if Graphics is null, rather
                // than cleaning up in the exception handler below..
                WindowManager.removeWindow(imageWin_);
                imageWin_.saveAttributes();
                imageWin_.dispose();
                imageWin_ = null;
                ret = false;
             }
 
          } catch (Exception e) {
             WindowManager.removeWindow(imageWin_);
             imageWin_.saveAttributes();
             imageWin_.dispose();
             imageWin_ = null;
             ReportingUtils.showError(e);
             ret = false;
          }
       }
       return ret;
    }
 
    boolean isLiveModeOn() {
       if (core_.getNumberOfComponents() == 1) {
       return liveModeTimerTask_ != null && liveModeTimerTask_.isRunning();
       } else {
          return getPipeline().isLiveRunning();
       }
    }
 
    // Timer task that displays the live image
    class LiveModeTimerTask extends TimerTask {
       public boolean running_ = false;
       private boolean cancelled_ = false;
 
       public synchronized boolean isRunning() {
          return running_;
       }
 
       @Override
       public synchronized boolean cancel() {
          running_ = false;
          return super.cancel();
       }
 
       public void run() {
          Thread.currentThread().setPriority(3);
          running_ = true;
          if (!isImageWindowOpen()) {
             // stop live acquisition if user closed the window
             enableLiveMode(false);
             return;
          }
          if (!isNewImageAvailable()) {
             return;
          }
          try {
             if (core_.getRemainingImageCount() > 0) {
                Object img = core_.getLastImage();
                if (img != img_) {
                   img_ = img;
                   displayImage(img_);
                   Thread.yield();
                }
             }
          } catch (Exception e) {
             ReportingUtils.showError(e);
             return;
          }
 
       }
 
 
    };
 
    public void enableLiveMode(boolean enable) {
       if (core_.getNumberOfComponents() == 1) {
          if (enable) {
             if (isLiveModeOn()) {
                return;
             }
             try {
                if (!isImageWindowOpen() && creatingImageWindow_.isFalse()) {
                   imageWin_ = createImageWindow();
                }
 
                // this is needed to clear the subtitle, should be folded into
                // drawInfo
                imageWin_.getGraphics().clearRect(0, 0, imageWin_.getWidth(),
                        40);
                imageWin_.drawInfo(imageWin_.getGraphics());
                imageWin_.toFront();
 
                // turn off auto shutter and open the shutter
                autoShutterOrg_ = core_.getAutoShutter();
                if (shutterLabel_.length() > 0) {
                   shutterOrg_ = core_.getShutterOpen();
                }
                core_.setAutoShutter(false);
 
                // Hide the autoShutter Checkbox
                autoShutterCheckBox_.setEnabled(false);
 
                shutterLabel_ = core_.getShutterDevice();
                // only open the shutter when we have one and the Auto shutter
                // checkbox was checked
                if ((shutterLabel_.length() > 0) && autoShutterOrg_) {
                   core_.setShutterOpen(true);
                }
                // attach mouse wheel listener to control focus:
                if (zWheelListener_ == null) {
                   zWheelListener_ = new ZWheelListener(core_, this);
                }
                zWheelListener_.start(imageWin_);
 
                // attach key listener to control the stage and focus:
                if (xyzKeyListener_ == null) {
                   xyzKeyListener_ = new XYZKeyListener(core_, this);
                }
                xyzKeyListener_.start(imageWin_);
 
                // Do not display more often than dictated by the exposure time
                setLiveModeInterval();
                core_.startContinuousSequenceAcquisition(0.0);
                liveModeTimerTask_ = new LiveModeTimerTask();
                liveModeTimer_.schedule(liveModeTimerTask_, (long) 0, (long) liveModeInterval_);
                // Only hide the shutter checkbox if we are in autoshuttermode
                buttonSnap_.setEnabled(false);
                if (autoShutterOrg_) {
                   toggleButtonShutter_.setEnabled(false);
                }
                imageWin_.setSubTitle("Live (running)");
                liveRunning_ = true;
             } catch (Exception err) {
                ReportingUtils.showError(err, "Failed to enable live mode.");
 
                if (imageWin_ != null) {
                   imageWin_.saveAttributes();
                   WindowManager.removeWindow(imageWin_);
                   imageWin_.dispose();
                   imageWin_ = null;
                }
             }
          } else {
             if (!isLiveModeOn()) {
                return;
             }
             try {
                liveModeTimerTask_.cancel();
                core_.stopSequenceAcquisition();
 
                if (zWheelListener_ != null) {
                   zWheelListener_.stop();
                }
                if (xyzKeyListener_ != null) {
                   xyzKeyListener_.stop();
                }
 
                // restore auto shutter and close the shutter
                if (shutterLabel_.length() > 0) {
                   core_.setShutterOpen(shutterOrg_);
                }
                core_.setAutoShutter(autoShutterOrg_);
                if (autoShutterOrg_) {
                   toggleButtonShutter_.setEnabled(false);
                } else {
                   toggleButtonShutter_.setEnabled(true);
                }
                liveRunning_ = false;
                buttonSnap_.setEnabled(true);
                autoShutterCheckBox_.setEnabled(true);
                // TODO: add timeout so that we can not hang here
                while (liveModeTimerTask_.isRunning()); // Make sure Timer properly stops.
                // This is here to avoid crashes when changing ROI in live mode
                // with Sensicam
                // Should be removed when underlying problem is dealt with
                Thread.sleep(100);
 
                imageWin_.setSubTitle("Live (stopped)");
                liveModeTimerTask_ = null;
 
             } catch (Exception err) {
                ReportingUtils.showError(err, "Failed to disable live mode.");
                if (imageWin_ != null) {
                   WindowManager.removeWindow(imageWin_);
                   imageWin_.dispose();
                   imageWin_ = null;
                }
             }
          }
       } else {
          buttonSnap_.setEnabled(!enable);
          autoShutterCheckBox_.setEnabled(!enable);
          getPipeline().enableLiveMode(enable);
          liveRunning_ = enable;
       }
 
       toggleButtonLive_.setIcon(liveRunning_ ? SwingResourceManager.getIcon(MMStudioMainFrame.class,
               "/org/micromanager/icons/cancel.png")
               : SwingResourceManager.getIcon(MMStudioMainFrame.class,
               "/org/micromanager/icons/camera_go.png"));
       toggleButtonLive_.setSelected(liveRunning_);
       toggleButtonLive_.setText(liveRunning_ ? "Stop Live" : "Live");
    }
 
    public boolean getLiveMode() {
       return liveRunning_;
    }
 
    public boolean updateImage() {
       try {
          if (isLiveModeOn()) {
                enableLiveMode(false);
                return true; // nothing to do, just show the last image
          }
 
          if (!isImageWindowOpen()) {
             createImageWindow();
          }
 
          core_.snapImage();
          Object img;
          img = core_.getImage();
 
          if (imageWin_.windowNeedsResizing()) {
             createImageWindow();
          }
 
          if (!isCurrentImageFormatSupported()) {
             return false;
          }
 
          imageWin_.newImage(img);
          updateLineProfile();
       } catch (Exception e) {
          ReportingUtils.showError(e);
          return false;
       }
 
       return true;
    }
 
    public boolean displayImage(Object pixels) {
       try {
          if (!isImageWindowOpen() ||  imageWin_.windowNeedsResizing()
                && creatingImageWindow_.isFalse()) {
             createImageWindow();
          }
 
          imageWin_.newImage(pixels);
          updateLineProfile();
       } catch (Exception e) {
          ReportingUtils.logError(e);
          return false;
       }
 
       return true;
    }
 
    public boolean displayImageWithStatusLine(Object pixels, String statusLine) {
       try {
          if (!isImageWindowOpen() || imageWin_.windowNeedsResizing()
                && creatingImageWindow_.isFalse()) {
             createImageWindow();
          }
 
          imageWin_.newImageWithStatusLine(pixels, statusLine);
          updateLineProfile();
       } catch (Exception e) {
          ReportingUtils.logError(e);
          return false;
       }
 
       return true;
    }
 
    public void displayStatusLine(String statusLine) {
       try {
          if (isImageWindowOpen()) {
             imageWin_.displayStatusLine(statusLine);
          }
       } catch (Exception e) {
          ReportingUtils.logError(e);
          return;
       }
    }
 
    private boolean isCurrentImageFormatSupported() {
       boolean ret = false;
       long channels = core_.getNumberOfComponents();
       long bpp = core_.getBytesPerPixel();
 
       if (channels > 1 && channels != 4 && bpp != 1) {
          handleError("Unsupported image format.");
       } else {
          ret = true;
       }
       return ret;
    }
 
    private void doSnap() {
       if (core_.getNumberOfComponents() == 1) {
          if(4==core_.getBytesPerPixel()){
             doSnapFloat();
          }else{
          doSnapMonochrome();
          }
       } else {
          doSnapColor();
       }
    }
 
    public void initializeGUI() {
       try {
 
          // establish device roles
          cameraLabel_ = core_.getCameraDevice();
          shutterLabel_ = core_.getShutterDevice();
          zStageLabel_ = core_.getFocusDevice();
          xyStageLabel_ = core_.getXYStageDevice();
          engine_.setZStageDevice(zStageLabel_);
 
          if (cameraLabel_.length() > 0) {
             ActionListener[] listeners;
 
             // binning combo
             if (comboBinning_.getItemCount() > 0) {
                comboBinning_.removeAllItems();
             }
             StrVector binSizes = core_.getAllowedPropertyValues(
                   cameraLabel_, MMCoreJ.getG_Keyword_Binning());
             listeners = comboBinning_.getActionListeners();
             for (int i = 0; i < listeners.length; i++) {
                comboBinning_.removeActionListener(listeners[i]);
             }
             for (int i = 0; i < binSizes.size(); i++) {
                comboBinning_.addItem(binSizes.get(i));
             }
 
             comboBinning_.setMaximumRowCount((int) binSizes.size());
             if (binSizes.size() == 0) {
                comboBinning_.setEditable(true);
             } else {
                comboBinning_.setEditable(false);
             }
 
             for (int i = 0; i < listeners.length; i++) {
                comboBinning_.addActionListener(listeners[i]);
             }
 
          }
 
          // active shutter combo
          try {
             shutters_ = core_.getLoadedDevicesOfType(DeviceType.ShutterDevice);
          } catch (Exception e) {
             ReportingUtils.logError(e);
          }
 
          if (shutters_ != null) {
             String items[] = new String[(int) shutters_.size()];
             for (int i = 0; i < shutters_.size(); i++) {
                items[i] = shutters_.get(i);
             }
 
             GUIUtils.replaceComboContents(shutterComboBox_, items);
             String activeShutter = core_.getShutterDevice();
             if (activeShutter != null) {
                shutterComboBox_.setSelectedItem(activeShutter);
             } else {
                shutterComboBox_.setSelectedItem("");
             }
          }
 
          // Autofocus
          buttonAutofocusTools_.setEnabled(afMgr_.getDevice() != null);
          buttonAutofocus_.setEnabled(afMgr_.getDevice() != null);
 
          // Rebuild stage list in XY PositinList
          if (posListDlg_ != null) {
             posListDlg_.rebuildAxisList();
          }
 
          // Mouse moves stage
          centerAndDragListener_ = new CenterAndDragListener(core_, gui_);
          if (centerAndDragMenuItem_.isSelected()) {
             centerAndDragListener_.start();
          }
 
          updateGUI(true);
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
    }
 
    public String getVersion() {
       return VERSION;
    }
 
    private void addPluginToMenu(final PluginItem plugin) {
       // add plugin menu items
 
       final JMenuItem newMenuItem = new JMenuItem();
       newMenuItem.addActionListener(new ActionListener() {
 
          public void actionPerformed(final ActionEvent e) {
             ReportingUtils.logMessage("Plugin command: "
                   + e.getActionCommand());
                   plugin.instantiate();
                   plugin.plugin.show();
          }
       });
       newMenuItem.setText(plugin.menuItem);
       pluginMenu_.add(newMenuItem);
       pluginMenu_.validate();
       menuBar_.validate();
    }
 
    public void updateGUI(boolean updateConfigPadStructure) {
 
       try {
          // establish device roles
          cameraLabel_ = core_.getCameraDevice();
          shutterLabel_ = core_.getShutterDevice();
          zStageLabel_ = core_.getFocusDevice();
          xyStageLabel_ = core_.getXYStageDevice();
 
          afMgr_.refresh();
 
          // camera settings
          if (isCameraAvailable()) {
             double exp = core_.getExposure();
             textFieldExp_.setText(NumberUtils.doubleToDisplayString(exp));
             String binSize = core_.getProperty(cameraLabel_, MMCoreJ.getG_Keyword_Binning());
             GUIUtils.setComboSelection(comboBinning_, binSize);
 
             long bitDepth = 8;
             if (imageWin_ != null) {
                long hsz = imageWin_.getRawHistogramSize();
                bitDepth = (long) Math.log(hsz);
             }
 
             bitDepth = core_.getImageBitDepth();
             contrastPanel_.setPixelBitDepth((int) bitDepth, false);
          }
 
          if (liveModeTimerTask_ == null || !liveModeTimerTask_.isRunning()) {
             autoShutterCheckBox_.setSelected(core_.getAutoShutter());
             boolean shutterOpen = core_.getShutterOpen();
             setShutterButton(shutterOpen);
             if (autoShutterCheckBox_.isSelected()) {
                toggleButtonShutter_.setEnabled(false);
             } else {
                toggleButtonShutter_.setEnabled(true);
             }
 
             autoShutterOrg_ = core_.getAutoShutter();
          }
 
          // active shutter combo
          if (shutters_ != null) {
             String activeShutter = core_.getShutterDevice();
             if (activeShutter != null) {
                shutterComboBox_.setSelectedItem(activeShutter);
             } else {
                shutterComboBox_.setSelectedItem("");
             }
          }
 
          // state devices
          if (updateConfigPadStructure && (configPad_ != null)) {
             configPad_.refreshStructure();
          }
 
          // update Channel menus in Multi-dimensional acquisition dialog
          updateChannelCombos();
 
       } catch (Exception e) {
          ReportingUtils.logError(e);
       }
 
       updateStaticInfo();
       updateTitle();
 
    }
 
    public boolean okToAcquire() {
       return !isLiveModeOn();
    }
 
    public void stopAllActivity() {
       enableLiveMode(false);
    }
 
    public void refreshImage() {
       if (imageWin_ != null) {
          imageWin_.getImagePlus().updateAndDraw();
       }
    }
 
    private void cleanupOnClose() {
       // NS: Save config presets if they were changed.
       if (configChanged_) {
          Object[] options = {"Yes", "No"};
          int n = JOptionPane.showOptionDialog(null,
                "Save Changed Configuration?", "Micro-Manager",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
          if (n == JOptionPane.YES_OPTION) {
             saveConfigPresets();
          }
       }
       if (liveModeTimerTask_ != null)
          liveModeTimerTask_.cancel();
 
 
       try{
           if (imageWin_ != null) {
              if (!imageWin_.isClosed())
                 imageWin_.close();
              imageWin_.dispose();
              imageWin_ = null;
           }
       }
       catch( Throwable t){
             ReportingUtils.logError(t, "closing ImageWin_");
         }
 
       if (profileWin_ != null) {
          removeMMBackgroundListener(profileWin_);
          profileWin_.dispose();
       }
 
       if (scriptPanel_ != null) {
          removeMMBackgroundListener(scriptPanel_);
          scriptPanel_.closePanel();
       }
 
       if (propertyBrowser_ != null) {
          removeMMBackgroundListener(propertyBrowser_);
          propertyBrowser_.dispose();
       }
 
       if (acqControlWin_ != null) {
          removeMMBackgroundListener(acqControlWin_);
          acqControlWin_.close();
       }
 
       if (engine_ != null) {
          engine_.shutdown();
       }
 
       if (afMgr_ != null) {
          afMgr_.closeOptionsDialog();
       }
 
       // dispose plugins
       for (int i = 0; i < plugins_.size(); i++) {
          MMPlugin plugin = (MMPlugin) plugins_.get(i).plugin;
          if (plugin != null) {
             plugin.dispose();
          }
       }
 
       try {
          if (core_ != null)
             core_.reset();
       } catch (Exception err) {
          ReportingUtils.showError(err);
       }
    }
 
    public void addExitHandler(Callable<Boolean> exitHandler) {
       exitHandlers.add(exitHandler);
    }
 
    public void removeExitHandler(Callable<Boolean> exitHandler) {
       exitHandlers.remove(exitHandler);
    }
 
    private void saveSettings() {
       Rectangle r = this.getBounds();
 
       mainPrefs_.putInt(MAIN_FRAME_X, r.x);
       mainPrefs_.putInt(MAIN_FRAME_Y, r.y);
       mainPrefs_.putInt(MAIN_FRAME_WIDTH, r.width);
       mainPrefs_.putInt(MAIN_FRAME_HEIGHT, r.height);
       mainPrefs_.putInt(MAIN_FRAME_DIVIDER_POS, this.splitPane_.getDividerLocation());
 
       mainPrefs_.putBoolean(MAIN_STRETCH_CONTRAST, contrastPanel_.isContrastStretch());
 
       mainPrefs_.put(OPEN_ACQ_DIR, openAcqDirectory_);
 
       // save field values from the main window
       // NOTE: automatically restoring these values on startup may cause
       // problems
       mainPrefs_.put(MAIN_EXPOSURE, textFieldExp_.getText());
 
       // NOTE: do not save auto shutter state
 
       if (afMgr_ != null && afMgr_.getDevice() != null) {
          mainPrefs_.put(AUTOFOCUS_DEVICE, afMgr_.getDevice().getDeviceName());
       }
    }
 
    private void loadConfiguration() {
       File f = FileDialogs.openFile(this, "Load a config file",MM_CONFIG_FILE);
       if (f != null) {
          sysConfigFile_ = f.getAbsolutePath();
          configChanged_ = false;
          setConfigSaveButtonStatus(configChanged_);
          mainPrefs_.put(SYSTEM_CONFIG_FILE, sysConfigFile_);
          loadSystemConfiguration();
       }
    }
 
    private void loadSystemState() {
       File f = FileDialogs.openFile(this, "Load a system state file", MM_CONFIG_FILE);
       if (f != null) {
          sysStateFile_ = f.getAbsolutePath();
          try {
             // WaitDialog waitDlg = new
             // WaitDialog("Loading saved state, please wait...");
             // waitDlg.showDialog();
             core_.loadSystemState(sysStateFile_);
             GUIUtils.preventDisplayAdapterChangeExceptions();
             // waitDlg.closeDialog();
             initializeGUI();
          } catch (Exception e) {
             ReportingUtils.showError(e);
             return;
          }
       }
    }
 
    private void saveSystemState() {
       File f = FileDialogs.save(this,
               "Save the system state to a config file", MM_CONFIG_FILE);
 
       if (f != null) {
          sysStateFile_ = f.getAbsolutePath();
 
          try {
             core_.saveSystemState(sysStateFile_);
          } catch (Exception e) {
             ReportingUtils.showError(e);
             return;
          }
       }
    }
 
    public void closeSequence() {
       /* exit handlers can cancel exiting if they return false */
       for (Callable<Boolean> exitHandler:exitHandlers) {
          try {
             if (!exitHandler.call())
                return;
          } catch (Exception ex) {
             ReportingUtils.logError(ex);
          }
       }
 
       if (engine_ != null && engine_.isAcquisitionRunning()) {
          int result = JOptionPane.showConfirmDialog(
                this,
                "Acquisition in progress. Are you sure you want to exit and discard all data?",
                "Micro-Manager", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
 
          if (result == JOptionPane.NO_OPTION) {
             return;
          }
       }
 
       stopAllActivity();
 
       cleanupOnClose();
       saveSettings();
       try {
          configPad_.saveSettings();
          options_.saveSettings();
          hotKeys_.saveSettings();
       } catch (NullPointerException e) {
          this.logError(e);
       }
       dispose();
       if (options_.closeOnExit_) {
          if (!runsAsPlugin_) {
             System.exit(0);
          } else {
             ImageJ ij = IJ.getInstance();
             if (ij != null) {
                ij.quit();
             }
          }
       }
 
    }
 
    public void applyContrastSettings(ContrastSettings contrast8,
          ContrastSettings contrast16) {
       contrastPanel_.applyContrastSettings(contrast8, contrast16);
    }
 
    public ContrastSettings getContrastSettings() {
       return contrastPanel_.getContrastSettings();
    }
 
    public boolean is16bit() {
       if (isImageWindowOpen()
             && imageWin_.getImagePlus().getProcessor() instanceof ShortProcessor) {
          return true;
       }
       return false;
    }
 
    public boolean isRunning() {
       return running_;
    }
 
    /**
     * Executes the beanShell script. This script instance only supports
     * commands directed to the core object.
     */
    private void executeStartupScript() {
       // execute startup script
       File f = new File(startupScriptFile_);
 
       if (startupScriptFile_.length() > 0 && f.exists()) {
          WaitDialog waitDlg = new WaitDialog(
                "Executing startup script, please wait...");
          waitDlg.showDialog();
          Interpreter interp = new Interpreter();
          try {
             // insert core object only
             interp.set(SCRIPT_CORE_OBJECT, core_);
             interp.set(SCRIPT_ACQENG_OBJECT, engine_);
             interp.set(SCRIPT_GUI_OBJECT, this);
 
             // read text file and evaluate
             interp.eval(TextUtils.readTextFile(startupScriptFile_));
          } catch (IOException exc) {
             ReportingUtils.showError(exc, "Unable to read the startup script (" + startupScriptFile_ + ").");
          } catch (EvalError exc) {
             ReportingUtils.showError(exc);
          } finally {
             waitDlg.closeDialog();
          }
       } else {
          if (startupScriptFile_.length() > 0)
             ReportingUtils.logMessage("Startup script file ("+startupScriptFile_+") not present.");
       }
    }
 
    /**
     * Loads sytem configuration from the cfg file.
     */
    private boolean loadSystemConfiguration() {
       boolean result = true;
 
       saveMRUConfigFiles();
 
       final WaitDialog waitDlg = new WaitDialog(
             "Loading system configuration, please wait...");
 
       waitDlg.setAlwaysOnTop(true);
       waitDlg.showDialog();
       this.setEnabled(false);
 
       try {
          if (sysConfigFile_.length() > 0) {
             GUIUtils.preventDisplayAdapterChangeExceptions();
             core_.waitForSystem();
             core_.loadSystemConfiguration(sysConfigFile_);
             GUIUtils.preventDisplayAdapterChangeExceptions();
             waitDlg.closeDialog();
          } else {
              waitDlg.closeDialog();
          }
       } catch (final Exception err) {
          GUIUtils.preventDisplayAdapterChangeExceptions();
          waitDlg.closeDialog();
          ReportingUtils.showError(err);
          result = false;
       }
       this.setEnabled(true);
       this.initializeGUI();
 
       updateSwitchConfigurationMenu();
 
       FileDialogs.storePath(MM_CONFIG_FILE, new File(sysConfigFile_));
 
       return result;
    }
 
    private void saveMRUConfigFiles() {
       if (0 < sysConfigFile_.length()) {
          if (MRUConfigFiles_.contains(sysConfigFile_)) {
             MRUConfigFiles_.remove(sysConfigFile_);
          }
          if (maxMRUCfgs_ <= MRUConfigFiles_.size()) {
             MRUConfigFiles_.remove(maxMRUCfgs_ - 1);
          }
          MRUConfigFiles_.add(0, sysConfigFile_);
          // save the MRU list to the preferences
          for (Integer icfg = 0; icfg < MRUConfigFiles_.size(); ++icfg) {
             String value = "";
             if (null != MRUConfigFiles_.get(icfg)) {
                value = MRUConfigFiles_.get(icfg).toString();
             }
             mainPrefs_.put(CFGFILE_ENTRY_BASE + icfg.toString(), value);
          }
       }
    }
 
    private void loadMRUConfigFiles() {
       sysConfigFile_ = mainPrefs_.get(SYSTEM_CONFIG_FILE, sysConfigFile_);
       // startupScriptFile_ = mainPrefs_.get(STARTUP_SCRIPT_FILE,
       // startupScriptFile_);
       MRUConfigFiles_ = new ArrayList<String>();
       for (Integer icfg = 0; icfg < maxMRUCfgs_; ++icfg) {
          String value = "";
          value = mainPrefs_.get(CFGFILE_ENTRY_BASE + icfg.toString(), value);
          if (0 < value.length()) {
             File ruFile = new File(value);
             if (ruFile.exists()) {
                if (!MRUConfigFiles_.contains(value)) {
                   MRUConfigFiles_.add(value);
                }
             }
          }
       }
       // initialize MRU list from old persistant data containing only SYSTEM_CONFIG_FILE
       if (0 < sysConfigFile_.length()) {
          if (!MRUConfigFiles_.contains(sysConfigFile_)) {
             // in case persistant data is inconsistent
             if (maxMRUCfgs_ <= MRUConfigFiles_.size()) {
                MRUConfigFiles_.remove(maxMRUCfgs_ - 1);
             }
             MRUConfigFiles_.add(0, sysConfigFile_);
          }
       }
    }
 
    /**
     * Opens Acquisition dialog.
     */
    private void openAcqControlDialog() {
       try {
          if (acqControlWin_ == null) {
             acqControlWin_ = new AcqControlDlg(engine_, mainPrefs_, this);
          }
          if (acqControlWin_.isActive()) {
             acqControlWin_.setTopPosition();
          }
 
          acqControlWin_.setVisible(true);
 
          // TODO: this call causes a strange exception the first time the
          // dialog is created
          // something to do with the order in which combo box creation is
          // performed
 
          // acqControlWin_.updateGroupsCombo();
       } catch (Exception exc) {
          ReportingUtils.showError(exc,
                "\nAcquistion window failed to open due to invalid or corrupted settings.\n"
                + "Try resetting registry settings to factory defaults (Menu Tools|Options).");
       }
    }
 
    /**
     * Opens Split View dialog.
     */
   /*
    protected void splitViewDialog() {
       try {
          if (splitView_ == null) {
             splitView_ = new SplitView(core_, this, options_);
          }
          splitView_.setVisible(true);
       } catch (Exception exc) {
          ReportingUtils.showError(exc,
                "\nSplit View Window failed to open due to internal error.");
       }
    }
 */
    /**
     * /** Opens a dialog to record stage positions
     */
    public void showXYPositionList() {
       if (posListDlg_ == null) {
          posListDlg_ = new PositionListDlg(core_, this, posList_, options_);
       }
       posListDlg_.setVisible(true);
    }
 
    private void updateChannelCombos() {
       if (this.acqControlWin_ != null) {
          this.acqControlWin_.updateChannelAndGroupCombo();
       }
    }
 
    public void setConfigChanged(boolean status) {
       configChanged_ = status;
       setConfigSaveButtonStatus(configChanged_);
    }
 
 
    /**
     * Returns the current background color
     * @return
     */
    public Color getBackgroundColor() {
       return guiColors_.background.get((options_.displayBackground_));
    }
 
    /*
     * Changes background color of this window and all other MM windows
     */
    public void setBackgroundStyle(String backgroundType) {
       setBackground(guiColors_.background.get((backgroundType)));
       paint(MMStudioMainFrame.this.getGraphics());
       
       // sets background of all registered Components
       for (Component comp:MMFrames_) {
          if (comp != null)
             comp.setBackground(guiColors_.background.get(backgroundType));
        }
    }
 
    public String getBackgroundStyle() {
       return options_.displayBackground_;
    }
 
    // Set ImageJ pixel calibration
    private void setIJCal(MMImageWindow imageWin) {
       if (imageWin != null) {
          imageWin.setIJCal();
       }
    }
 
    // //////////////////////////////////////////////////////////////////////////
    // Scripting interface
    // //////////////////////////////////////////////////////////////////////////
    private class ExecuteAcq implements Runnable {
 
       public ExecuteAcq() {
       }
 
       public void run() {
          if (acqControlWin_ != null) {
             acqControlWin_.runAcquisition();
          }
       }
    }
 
    private class LoadAcq implements Runnable {
 
       private String filePath_;
 
       public LoadAcq(String path) {
          filePath_ = path;
       }
 
       public void run() {
          // stop current acquisition if any
          engine_.shutdown();
 
          // load protocol
          if (acqControlWin_ != null) {
             acqControlWin_.loadAcqSettingsFromFile(filePath_);
          }
       }
    }
 
    private void testForAbortRequests() throws MMScriptException {
       if (scriptPanel_ != null) {
          if (scriptPanel_.stopRequestPending()) {
             throw new MMScriptException("Script interrupted by the user!");
          }
       }
    }
 
    public void startAcquisition() throws MMScriptException {
       testForAbortRequests();
       SwingUtilities.invokeLater(new ExecuteAcq());
    }
 
    public void runAcquisition() throws MMScriptException {
       testForAbortRequests();
       if (acqControlWin_ != null) {
          acqControlWin_.runAcquisition();
          try {
             while (acqControlWin_.isAcquisitionRunning()) {
                Thread.sleep(50);
             }
          } catch (InterruptedException e) {
             ReportingUtils.showError(e);
          }
       } else {
          throw new MMScriptException(
                "Acquisition window must be open for this command to work.");
       }
    }
 
    public void runAcquisition(String name, String root)
          throws MMScriptException {
       testForAbortRequests();
       if (acqControlWin_ != null) {
          acqControlWin_.runAcquisition(name, root);
          try {
             while (acqControlWin_.isAcquisitionRunning()) {
                Thread.sleep(100);
             }
          } catch (InterruptedException e) {
             ReportingUtils.showError(e);
          }
       } else {
          throw new MMScriptException(
                "Acquisition window must be open for this command to work.");
       }
    }
 
    public void runAcqusition(String name, String root) throws MMScriptException {
       runAcquisition(name, root);
    }
 
    public void loadAcquisition(String path) throws MMScriptException {
       testForAbortRequests();
       SwingUtilities.invokeLater(new LoadAcq(path));
    }
 
    public void setPositionList(PositionList pl) throws MMScriptException {
       testForAbortRequests();
       // use serialization to clone the PositionList object
       posList_ = pl; // PositionList.newInstance(pl);
       SwingUtilities.invokeLater(new Runnable() {
          public void run() {
             if (posListDlg_ != null) {
                posListDlg_.setPositionList(posList_);
                engine_.setPositionList(posList_);
             }
          }
       });
    }
 
    public PositionList getPositionList() throws MMScriptException {
       testForAbortRequests();
       // use serialization to clone the PositionList object
       return posList_; //PositionList.newInstance(posList_);
    }
 
    public void sleep(long ms) throws MMScriptException {
       if (scriptPanel_ != null) {
          if (scriptPanel_.stopRequestPending()) {
             throw new MMScriptException("Script interrupted by the user!");
          }
          scriptPanel_.sleep(ms);
       }
    }
 
    public void openAcquisition(String name, String rootDir) throws MMScriptException {
       openAcquisition(name, rootDir, true);
    }
 
    public void openAcquisition(String name, String rootDir, boolean show) throws MMScriptException {
       //acqMgr_.openAcquisition(name, rootDir, show);
       TaggedImageStorage imageFileManager = new TaggedImageStorageDiskDefault((new File(rootDir, name)).getAbsolutePath());
       MMImageCache cache = new MMImageCache(imageFileManager);
       VirtualAcquisitionDisplay display = new VirtualAcquisitionDisplay(cache, null);
       display.show();
    }
 
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices, int nrPositions) throws MMScriptException {
       this.openAcquisition(name, rootDir, nrFrames, nrChannels, nrSlices,
               nrPositions, true, false);
    }
 
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices) throws MMScriptException {
       openAcquisition(name, rootDir, nrFrames, nrChannels, nrSlices, 0);
    }
    
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices, int nrPositions, boolean show)
          throws MMScriptException {
       this.openAcquisition(name, rootDir, nrFrames, nrChannels, nrSlices, nrPositions, show, false);
    }
 
 
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices, boolean show)
          throws MMScriptException {
       this.openAcquisition(name, rootDir, nrFrames, nrChannels, nrSlices, 0, show, false);
    }   
 
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices, int nrPositions, boolean show, boolean virtual)
          throws MMScriptException {
       acqMgr_.openAcquisition(name, rootDir, show, virtual);
       MMAcquisition acq = acqMgr_.getAcquisition(name);
       acq.setDimensions(nrFrames, nrChannels, nrSlices, nrPositions);
    }
 
    public void openAcquisition(String name, String rootDir, int nrFrames,
          int nrChannels, int nrSlices, boolean show, boolean virtual)
          throws MMScriptException {
       this.openAcquisition(name, rootDir, nrFrames, nrChannels, nrSlices, 0, show, virtual);
    }
 
    private void openAcquisitionSnap(String name, String rootDir, boolean show)
          throws MMScriptException {
       /*
        MMAcquisition acq = acqMgr_.openAcquisitionSnap(name, rootDir, this,
             show);
       acq.setDimensions(0, 1, 1, 1);
       try {
          // acq.getAcqData().setPixelSizeUm(core_.getPixelSizeUm());
          acq.setProperty(SummaryKeys.IMAGE_PIXEL_SIZE_UM, String.valueOf(core_.getPixelSizeUm()));
 
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
        *
        */
    }
 
    public void initializeAcquisition(String name, int width, int height,
          int depth) throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(name);
       acq.setImagePhysicalDimensions(width, height, depth);
       acq.initialize();
    }
 
    public int getAcquisitionImageWidth(String acqName) throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(acqName);
       return acq.getWidth();
    }
 
    public int getAcquisitionImageHeight(String acqName) throws MMScriptException{
       MMAcquisition acq = acqMgr_.getAcquisition(acqName);
       return acq.getHeight();
    }
 
    public int getAcquisitionImageByteDepth(String acqName) throws MMScriptException{
       MMAcquisition acq = acqMgr_.getAcquisition(acqName);
       return acq.getDepth();
    }
 
    public Boolean acquisitionExists(String name) {
       return acqMgr_.acquisitionExists(name);
    }
 
    public void closeAcquisition(String name) throws MMScriptException {
       acqMgr_.closeAcquisition(name);
    }
 
    public void closeAcquisitionImage5D(String title) throws MMScriptException {
       acqMgr_.closeImage5D(title);
    }
 
    /**
     * Since Burst and normal acquisition are now carried out by the same engine,
     * loadBurstAcquistion simply calls loadAcquisition
     * t
     * @param path - path to file specifying acquisition settings
     */
    public void loadBurstAcquisition(String path) throws MMScriptException {
       this.loadAcquisition(path);
    }
 
    public void refreshGUI() {
       updateGUI(true);
    }
 
    public void setAcquisitionProperty(String acqName, String propertyName,
          String value) throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(acqName);
       acq.setProperty(propertyName, value);
    }
 
    public void setAcquisitionSystemState(String acqName, JSONObject md) throws MMScriptException {
       acqMgr_.getAcquisition(acqName).setSystemState(md);
    }
 
    public void setAcquisitionSummary(String acqName, JSONObject md) throws MMScriptException {
       acqMgr_.getAcquisition(acqName).setSummaryProperties(md);
    }
 
    public void setImageProperty(String acqName, int frame, int channel,
          int slice, String propName, String value) throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(acqName);
       acq.setProperty(frame, channel, slice, propName, value);
    }
 
 
    public void snapAndAddImage(String name, int frame, int channel, int slice)
            throws MMScriptException {
       snapAndAddImage(name, frame, channel, slice, 0);
    }
 
    public void snapAndAddImage(String name, int frame, int channel, int slice, int position)
          throws MMScriptException {
 
       Metadata md = new Metadata();
       try {
          Object img;
          if (core_.isSequenceRunning()) {
             img = core_.getLastImage();
             core_.getLastImageMD(0, 0, md);
             //img = core_.getLastTaggedImage();
          } else {
             core_.snapImage();
             img = core_.getImage();
          }
 
          MMAcquisition acq = acqMgr_.getAcquisition(name);
 
          long width = core_.getImageWidth();
          long height = core_.getImageHeight();
          long depth = core_.getBytesPerPixel();
 
          if (!acq.isInitialized()) {
 
             acq.setImagePhysicalDimensions((int) width, (int) height,
                   (int) depth);
             acq.initialize();
          }
 
          acq.insertImage(img, frame, channel, slice, position);
          // Insert exposure in metadata
 //       acq.setProperty(frame, channel, slice, ImagePropertyKeys.EXPOSURE_MS, NumberUtils.doubleToDisplayString(core_.getExposure()));
          // Add pixel size calibration
 
          /*
           double pixSizeUm = core_.getPixelSizeUm();
          if (pixSizeUm > 0) {
             acq.setProperty(frame, channel, slice, ImagePropertyKeys.X_UM, NumberUtils.doubleToDisplayString(pixSizeUm));
             acq.setProperty(frame, channel, slice, ImagePropertyKeys.Y_UM, NumberUtils.doubleToDisplayString(pixSizeUm));
          }
          // generate list with system state
          JSONObject state = Annotator.generateJSONMetadata(core_.getSystemStateCache());
          // and insert into metadata
          acq.setSystemState(frame, channel, slice, state);
           */
 
 
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
 
    }
 
    public void addToSnapSeries(Object img, String acqName) {
       try {
          // boolean liveRunning = liveRunning_;
          // if (liveRunning)
          //    enableLiveMode(false);
          if (acqName == null) {
             acqName = "Snap" + snapCount_;
          }
          Boolean newSnap = false;
 
          core_.setExposure(NumberUtils.displayStringToDouble(textFieldExp_.getText()));
          long width = core_.getImageWidth();
          long height = core_.getImageHeight();
          long depth = core_.getBytesPerPixel();
          //MMAcquisitionSnap acq = null;
 
          if (acqMgr_.hasActiveImage5D(acqName)) {
            // acq = (MMAcquisitionSnap) acqMgr_.getAcquisition(acqName);
            // newSnap = !acq.isCompatibleWithCameraSettings();
             ;
          } else {
             newSnap = true;
          }
 
          if (newSnap) {
             snapCount_++;
             acqName = "Snap" + snapCount_;
             this.openAcquisitionSnap(acqName, null, true); // (dir=null) ->
             // keep in
             // memory; don't
             // save to file.
             initializeAcquisition(acqName, (int) width, (int) height,
                   (int) depth);
 
          }
          setChannelColor(acqName, 0, Color.WHITE);
          setChannelName(acqName, 0, "Snap");
 
 //         acq = (MMAcquisitionSnap) acqMgr_.getAcquisition(acqName);
  //        acq.appendImage(img);
          // add exposure to metadata
 //         acq.setProperty(acq.getFrames() - 1, acq.getChannels() - 1, acq.getSlices() - 1, ImagePropertyKeys.EXPOSURE_MS, NumberUtils.doubleToDisplayString(core_.getExposure()));
          // Add pixel size calibration
          double pixSizeUm = core_.getPixelSizeUm();
          if (pixSizeUm > 0) {
 //            acq.setProperty(acq.getFrames() - 1, acq.getChannels() - 1, acq.getSlices() - 1, ImagePropertyKeys.X_UM, NumberUtils.doubleToDisplayString(pixSizeUm));
 //            acq.setProperty(acq.getFrames() - 1, acq.getChannels() - 1, acq.getSlices() - 1, ImagePropertyKeys.Y_UM, NumberUtils.doubleToDisplayString(pixSizeUm));
          }
          // generate list with system state
 //         JSONObject state = Annotator.generateJSONMetadata(core_.getSystemStateCache());
          // and insert into metadata
 //         acq.setSystemState(acq.getFrames() - 1, acq.getChannels() - 1, acq.getSlices() - 1, state);
 
 
          // closeAcquisition(acqName);
       } catch (Exception e) {
          ReportingUtils.showError(e);
       }
 
    }
 
    public void addImage(String name, Object img, int frame, int channel,
          int slice) throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(name);
       acq.insertImage(img, frame, channel, slice);
    }
 
    public void addImage(String name, TaggedImage taggedImg) throws MMScriptException {
       acqMgr_.getAcquisition(name).insertImage(taggedImg);
    }
 
    public void addImage(String name, TaggedImage taggedImg, boolean updateDisplay) throws MMScriptException {
       acqMgr_.getAcquisition(name).insertImage(taggedImg, updateDisplay);
    }
 
    public void closeAllAcquisitions() {
       acqMgr_.closeAll();
    }
 
    private class ScriptConsoleMessage implements Runnable {
 
       String msg_;
 
       public ScriptConsoleMessage(String text) {
          msg_ = text;
       }
 
       public void run() {
          if (scriptPanel_ != null)
             scriptPanel_.message(msg_);
       }
    }
 
    public void message(String text) throws MMScriptException {
       if (scriptPanel_ != null) {
          if (scriptPanel_.stopRequestPending()) {
             throw new MMScriptException("Script interrupted by the user!");
          }
 
          SwingUtilities.invokeLater(new ScriptConsoleMessage(text));
       }
    }
 
    public void clearMessageWindow() throws MMScriptException {
       if (scriptPanel_ != null) {
          if (scriptPanel_.stopRequestPending()) {
             throw new MMScriptException("Script interrupted by the user!");
          }
          scriptPanel_.clearOutput();
       }
    }
 
    public void clearOutput() throws MMScriptException {
       clearMessageWindow();
    }
 
    public void clear() throws MMScriptException {
       clearMessageWindow();
    }
 
    public void setChannelContrast(String title, int channel, int min, int max)
          throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(title);
       acq.setChannelContrast(channel, min, max);
    }
 
    public void setChannelName(String title, int channel, String name)
          throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(title);
       acq.setChannelName(channel, name);
 
    }
 
    public void setChannelColor(String title, int channel, Color color)
          throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(title);
       acq.setChannelColor(channel, color.getRGB());
    }
 
    public void setContrastBasedOnFrame(String title, int frame, int slice)
          throws MMScriptException {
       MMAcquisition acq = acqMgr_.getAcquisition(title);
       acq.setContrastBasedOnFrame(frame, slice);
    }
 
    public void setStagePosition(double z) throws MMScriptException {
       try {
          core_.setPosition(core_.getFocusDevice(),z);
          core_.waitForDevice(core_.getFocusDevice());
       } catch (Exception e) {
          throw new MMScriptException(e.getMessage());
       }
    }
 
    public void setRelativeStagePosition(double z) throws MMScriptException {
       try {
          core_.setRelativePosition(core_.getFocusDevice(), z);
          core_.waitForDevice(core_.getFocusDevice());
       } catch (Exception e) {
          throw new MMScriptException(e.getMessage());
       }
    }
 
 
    public void setXYStagePosition(double x, double y) throws MMScriptException {
       try {
          core_.setXYPosition(core_.getXYStageDevice(), x, y);
          core_.waitForDevice(core_.getXYStageDevice());
       } catch (Exception e) {
          throw new MMScriptException(e.getMessage());
       }
    }
 
       public void setRelativeXYStagePosition(double x, double y) throws MMScriptException {
       try {
          core_.setRelativeXYPosition(core_.getXYStageDevice(), x, y);
          core_.waitForDevice(core_.getXYStageDevice());
       } catch (Exception e) {
          throw new MMScriptException(e.getMessage());
       }
    }
 
    public Point2D.Double getXYStagePosition() throws MMScriptException {
       String stage = core_.getXYStageDevice();
       if (stage.length() == 0) {
          throw new MMScriptException("XY Stage device is not available");
       }
 
       double x[] = new double[1];
       double y[] = new double[1];
       try {
          core_.getXYPosition(stage, x, y);
          Point2D.Double pt = new Point2D.Double(x[0], y[0]);
          return pt;
       } catch (Exception e) {
          throw new MMScriptException(e.getMessage());
       }
    }
 
    public String getXYStageName() {
       return core_.getXYStageDevice();
    }
 
    public void setXYOrigin(double x, double y) throws MMScriptException {
       String xyStage = core_.getXYStageDevice();
       try {
          core_.setAdapterOriginXY(xyStage, x, y);
       } catch (Exception e) {
          throw new MMScriptException(e);
       }
    }
 
    public AcquisitionEngine getAcquisitionEngine() {
       return engine_;
    }
 
    public String installPlugin(Class<?> cl) {
       String className = cl.getSimpleName();
       String msg = new String(className + " module loaded.");
       try {
          for (PluginItem plugin : plugins_) {
             if (plugin.className.contentEquals(className)) {
                return className + " already loaded.";
             }
          }
 
          PluginItem pi = new PluginItem();
          pi.className = className;
          try {
             // Get this static field from the class implementing MMPlugin.
             pi.menuItem = (String) cl.getDeclaredField("menuName").get(null);
          } catch (SecurityException e) {
             ReportingUtils.logError(e);
             pi.menuItem = className;
          } catch (NoSuchFieldException e) {
             pi.menuItem = className;
             ReportingUtils.logError(className + " fails to implement static String menuName.");
          } catch (IllegalArgumentException e) {
             ReportingUtils.logError(e);
          } catch (IllegalAccessException e) {
             ReportingUtils.logError(e);
          }
 
          if (pi.menuItem == null) {
             pi.menuItem = className;
             //core_.logMessage(className + " fails to implement static String menuName.");
          }
          pi.menuItem = pi.menuItem.replace("_", " ");
          pi.pluginClass = cl;
          plugins_.add(pi);
          final PluginItem pi2 = pi;
          SwingUtilities.invokeLater(
             new Runnable() {
                public void run() {
                   addPluginToMenu(pi2);
                }
             });
 
       } catch (NoClassDefFoundError e) {
          msg = className + " class definition not found.";
          ReportingUtils.logError(e, msg);
 
       }
 
       return msg;
 
    }
 
    public String installPlugin(String className, String menuName) {
       String msg = "installPlugin(String className, String menuName) is deprecated. Use installPlugin(String className) instead.";
       core_.logMessage(msg);
       installPlugin(className);
       return msg;
    }
 
    public String installPlugin(String className) {
       String msg = "";
       try {
          Class clazz = Class.forName(className);
          return installPlugin(clazz);
       } catch (ClassNotFoundException e) {
          msg = className + " plugin not found.";
          ReportingUtils.logError(e, msg);
          return msg;
       }
    }
 
    public String installAutofocusPlugin(String className) {
       try {
          return installAutofocusPlugin(Class.forName(className));
       } catch (ClassNotFoundException e) {
          String msg = "Internal error: AF manager not instantiated.";
          ReportingUtils.logError(e, msg);
          return msg;
       }
    }
 
    public String installAutofocusPlugin(Class<?> autofocus) {
       String msg = new String(autofocus.getSimpleName() + " module loaded.");
       if (afMgr_ != null) {
          try {
             afMgr_.refresh();
          } catch (MMException e) {
             msg = e.getMessage();
             ReportingUtils.logError(e);
          }
          afMgr_.setAFPluginClassName(autofocus.getSimpleName());
       } else {
          msg = "Internal error: AF manager not instantiated.";
       }
       return msg;
    }
 
    public CMMCore getCore() {
       return core_;
    }
 
    public Pipeline getPipeline() {
       try {
          pipelineClassLoadingThread_.join();
          if (acquirePipeline_ == null) {
             acquirePipeline_ = (Pipeline) pipelineClass_.newInstance();
          }
          return acquirePipeline_;
       } catch (Exception e) {
          ReportingUtils.logError(e);
          return null;
       }
    }
 
    public void snapAndAddToImage5D(String acqName) {
       try {
          getPipeline().acquireSingle();
       } catch (Exception ex) {
          ReportingUtils.logError(ex);
       }
    }
 
    public void setAcquisitionEngine(AcquisitionEngine eng) {
       engine_ = eng;
    }
    
    //Returns true if there is a newer image to display that can be get from MMCore
    //Implements "optimistic" approach: returns true even
    //if there was an error while getting the image time stamp
    private boolean isNewImageAvailable() {
       boolean ret = true;
       /* disabled until metadata-related methods in MMCoreJ can handle exceptions
       Metadata md = new Metadata();
       MetadataSingleTag tag = null;
       try
       {
       core_.getLastImageMD(0, 0, md);
       String strTag=MMCoreJ.getG_Keyword_Elapsed_Time_ms();
       tag = md.GetSingleTag(strTag);
       if(tag != null)
       {
       double newFrameTimeStamp = Double.valueOf(tag.GetValue());
       ret = newFrameTimeStamp > lastImageTimeMs_;
       if (ret)
       {
       lastImageTimeMs_ = newFrameTimeStamp;
       }
       }
       }
       catch(Exception e)
       {
       ReportingUtils.logError(e);
       }
        */
       return ret;
    }
 
    ;
 
    public void suspendLiveMode() {
       liveModeSuspended_ = isLiveModeOn();
       enableLiveMode(false);
    }
 
    public void resumeLiveMode() {
       if (liveModeSuspended_) {
          enableLiveMode(true);
       }
    }
 
    public Autofocus getAutofocus() {
       return afMgr_.getDevice();
    }
 
    public void showAutofocusDialog() {
       if (afMgr_.getDevice() != null) {
          afMgr_.showOptionsDialog();
       }
    }
 
    public AutofocusManager getAutofocusManager() {
       return afMgr_;
    }
 
    public void selectConfigGroup(String groupName) {
       configPad_.setGroup(groupName);
    }
 
    private void loadPlugins() {
 
       ArrayList<Class<?>> pluginClasses = new ArrayList<Class<?>>();
       ArrayList<Class<?>> autofocusClasses = new ArrayList<Class<?>>();
       List<Class<?>> classes;
 
       try {
          long t1 = System.currentTimeMillis();
          classes = JavaUtils.findClasses(new File("mmplugins"), 2);
          //System.out.println("findClasses: " + (System.currentTimeMillis() - t1));
          //System.out.println(classes.size());
          for (Class<?> clazz : classes) {
             for (Class<?> iface : clazz.getInterfaces()) {
                //core_.logMessage("interface found: " + iface.getName());
                if (iface == MMPlugin.class) {
                   pluginClasses.add(clazz);
                }
             }
 
          }
 
          classes = JavaUtils.findClasses(new File("mmautofocus"), 2);
          for (Class<?> clazz : classes) {
             for (Class<?> iface : clazz.getInterfaces()) {
                //core_.logMessage("interface found: " + iface.getName());
                if (iface == Autofocus.class) {
                   autofocusClasses.add(clazz);
                }
             }
          }
 
       } catch (ClassNotFoundException e1) {
          ReportingUtils.logError(e1);
       }
 
       for (Class<?> plugin : pluginClasses) {
          try {
             ReportingUtils.logMessage("Attempting to install plugin " + plugin.getName());
             installPlugin(plugin);
          } catch (Exception e) {
             ReportingUtils.logError(e, "Attempted to install the \"" + plugin.getName() + "\" plugin .");
          }
       }
 
       for (Class<?> autofocus : autofocusClasses) {
          try {
             installAutofocusPlugin(autofocus.getName());
          } catch (Exception e) {
             ReportingUtils.logError("Attempted to install the \"" + autofocus.getName() + "\" autofocus plugin.");
          }
       }
 
    }
 
    /**
     *
     */
    private void setLiveModeInterval() {
       double interval = 33.0;
       try {
          if (core_.getExposure() > 33.0) {
             interval = core_.getExposure();
          }
       } catch (Exception ex) {
          ReportingUtils.showError(ex);
       }
 
       liveModeInterval_ = interval;
       //liveModeTimer_.setDelay((int) liveModeInterval_);
       //liveModeTimer_.setInitialDelay(liveModeTimer_.getDelay());
    }
 
    public void logMessage(String msg) {
       ReportingUtils.logMessage(msg);
    }
 
    public void showMessage(String msg) {
       ReportingUtils.showMessage(msg);
    }
 
    public void logError(Exception e, String msg) {
       ReportingUtils.logError(e, msg);
    }
 
    public void logError(Exception e) {
       ReportingUtils.logError(e);
    }
 
    public void logError(String msg) {
       ReportingUtils.logError(msg);
    }
 
    public void showError(Exception e, String msg) {
       ReportingUtils.showError(e, msg);
    }
 
    public void showError(Exception e) {
       ReportingUtils.showError(e);
    }
 
    public void showError(String msg) {
       ReportingUtils.showError(msg);
    }
 }
 
 class BooleanLock extends Object {
 
    private boolean value;
 
    public BooleanLock(boolean initialValue) {
       value = initialValue;
    }
 
    public BooleanLock() {
       this(false);
    }
 
    public synchronized void setValue(boolean newValue) {
       if (newValue != value) {
          value = newValue;
          notifyAll();
       }
    }
 
    public synchronized boolean waitToSetTrue(long msTimeout)
          throws InterruptedException {
 
       boolean success = waitUntilFalse(msTimeout);
       if (success) {
          setValue(true);
       }
 
       return success;
    }
 
    public synchronized boolean waitToSetFalse(long msTimeout)
          throws InterruptedException {
 
       boolean success = waitUntilTrue(msTimeout);
       if (success) {
          setValue(false);
       }
 
       return success;
    }
 
    public synchronized boolean isTrue() {
       return value;
    }
 
    public synchronized boolean isFalse() {
       return !value;
    }
 
    public synchronized boolean waitUntilTrue(long msTimeout)
          throws InterruptedException {
 
       return waitUntilStateIs(true, msTimeout);
    }
 
    public synchronized boolean waitUntilFalse(long msTimeout)
          throws InterruptedException {
 
       return waitUntilStateIs(false, msTimeout);
    }
 
    public synchronized boolean waitUntilStateIs(
          boolean state,
          long msTimeout) throws InterruptedException {
 
       if (msTimeout == 0L) {
          while (value != state) {
             wait();
          }
 
          return true;
       }
 
       long endTime = System.currentTimeMillis() + msTimeout;
       long msRemaining = msTimeout;
 
       while ((value != state) && (msRemaining > 0L)) {
          wait(msRemaining);
          msRemaining = endTime - System.currentTimeMillis();
       }
 
       return (value == state);
    }
 
  
 
 }
 
