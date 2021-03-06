 /*
  * PrintingWidget.java
  *
  * Created on 10. Juli 2006, 17:55
  */
 package de.cismet.cismap.commons.gui.printing;
 
 import de.cismet.cismap.commons.BoundingBox;
 import de.cismet.cismap.commons.Debug;
 import de.cismet.cismap.commons.ServiceLayer;
 import de.cismet.cismap.commons.featureservice.AbstractFeatureService;
 import de.cismet.cismap.commons.gui.MappingComponent;
 import de.cismet.cismap.commons.gui.piccolo.eventlistener.PrintingFrameListener;
 import de.cismet.cismap.commons.retrieval.RetrievalEvent;
 import de.cismet.cismap.commons.retrieval.RetrievalListener;
 import de.cismet.cismap.commons.retrieval.RetrievalService;
 import de.cismet.tools.CismetThreadPool;
 import de.cismet.tools.gui.Static2DTools;
 import de.cismet.tools.gui.StaticSwingTools;
 import de.cismet.tools.gui.imagetooltip.ImageToolTip;
 import java.awt.AlphaComposite;
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Composite;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.lang.reflect.Constructor;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.TreeMap;
 import java.util.logging.Level;
 import javax.swing.ImageIcon;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JToolTip;
 import javax.swing.text.BadLocationException;
 import javax.swing.text.Style;
 import javax.swing.text.StyleConstants;
 import javax.swing.text.StyledDocument;
 import net.sf.jasperreports.engine.JasperExportManager;
 import net.sf.jasperreports.engine.JasperFillManager;
 import net.sf.jasperreports.engine.JasperPrint;
 import net.sf.jasperreports.engine.JasperPrintManager;
 import net.sf.jasperreports.engine.JasperReport;
 import net.sf.jasperreports.engine.util.JRLoader;
 import net.sf.jasperreports.view.JRViewer;
 import org.jdesktop.swingx.JXErrorPane;
 import org.jdesktop.swingx.error.ErrorInfo;
 
 /**
  *
  * @author  thorsten.hell@cismet.de
  */
 public class PrintingWidget extends javax.swing.JDialog implements RetrievalListener
 {
 
   private final static boolean DEBUG = Debug.DEBUG;
   private final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
   private MappingComponent mappingComponent = null;
   private String interactionModeAfterPrinting = "";//NOI18N
   private BufferedImage map;
   private TreeMap<Integer, RetrievalService> services;
   private TreeMap<Integer, Object> results;
   private TreeMap<Integer, Object> erroneous;
   private AbstractPrintingInscriber inscriber = null;
   private ImageIcon errorImage = new javax.swing.ImageIcon(getClass().getResource("/de/cismet/cismap/commons/gui/res/error.png"));//NOI18N
   public static final String TIP = "TIP";//NOI18N
   public static final String INFO = "INFO";//NOI18N
   public static final String SUCCESS = "SUCCESS";//NOI18N
   public static final String EXPERT = "EXPERT";//NOI18N
   public static final String WARN = "WARN";//NOI18N
   public static final String ERROR = "ERROR";//NOI18N
   public static final String ERROR_REASON = "ERROR_REASON";//NOI18N
   private Style styleTip, styleSuccess, styleInfo, styleExpert, styleWarn, styleError, styleErrorReason;
   private HashMap<String, Style> styles = new HashMap<String, Style>();
   PDFCreatingWaitDialog pdfWait;
   private int imageWidth;
   private int imageHeight;
 
   /** Creates new form PrintingWidget */
   public PrintingWidget(final boolean modal, final MappingComponent mappingComponent)
   {
     super(StaticSwingTools.getParentFrame(mappingComponent), modal);
     Runnable r = new Runnable()
     {
 
       @Override
       public void run()
       {
         pdfWait = new PDFCreatingWaitDialog(StaticSwingTools.getParentFrame(mappingComponent), true);
       }
     };
     CismetThreadPool.execute(r);
     this.mappingComponent = mappingComponent;
     initComponents();
     panDesc.setBackground(new Color(216, 228, 248));
     getRootPane().setDefaultButton(cmdOk);
     txpLoadingStatus.setBackground(this.getBackground());
     prbLoading.setForeground(panDesc.getBackground());
     styleTip = txpLoadingStatus.addStyle(TIP, null);
     StyleConstants.setForeground(styleTip, Color.blue);
     StyleConstants.setFontSize(styleTip, 10);
     styles.put(TIP, styleTip);
     styleSuccess = txpLoadingStatus.addStyle(SUCCESS, null);
     StyleConstants.setForeground(styleSuccess, Color.green.darker());
     StyleConstants.setFontSize(styleSuccess, 10);
 
     styles.put(SUCCESS, styleSuccess);
     styleInfo = txpLoadingStatus.addStyle(INFO, null);
     StyleConstants.setForeground(styleInfo, Color.DARK_GRAY);
     StyleConstants.setFontSize(styleInfo, 10);
     styles.put(INFO, styleInfo);
     styleExpert = txpLoadingStatus.addStyle(EXPERT, null);
     StyleConstants.setForeground(styleExpert, Color.gray);
     StyleConstants.setFontSize(styleExpert, 10);
     styles.put(EXPERT, styleExpert);
     styleWarn = txpLoadingStatus.addStyle(WARN, null);
     StyleConstants.setForeground(styleWarn, Color.orange.darker());
     StyleConstants.setFontSize(styleWarn, 10);
     styles.put(WARN, styleWarn);
     styleError = txpLoadingStatus.addStyle(ERROR, null);
     StyleConstants.setForeground(styleError, Color.red);
     StyleConstants.setFontSize(styleError, 10);
     StyleConstants.setBold(styleError, true);
     styles.put(ERROR, styleError);
     styleErrorReason = txpLoadingStatus.addStyle(ERROR_REASON, null);
     StyleConstants.setForeground(styleErrorReason, Color.red);
     StyleConstants.setFontSize(styleErrorReason, 10);
     styles.put(ERROR_REASON, styleErrorReason);
 
     StaticSwingTools.setNiftyScrollBars(scpLoadingStatus);
     //txpLoadingStatus.setContentType("text/html");
     }
 
   public PrintingWidget cloneWithNewParent(boolean modal, MappingComponent mappingComponent)
   {
     PrintingWidget newWidget = new PrintingWidget(modal, mappingComponent);
     return newWidget;
   }
 
   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         lbl1 = new javax.swing.JLabel();
         txt1 = new javax.swing.JTextField();
         lbl2 = new javax.swing.JLabel();
         txt2 = new javax.swing.JTextField();
         panDesc = new javax.swing.JPanel();
         jLabel1 = new javax.swing.JLabel();
         jSeparator2 = new javax.swing.JSeparator();
         jLabel2 = new javax.swing.JLabel();
         jLabel3 = new javax.swing.JLabel();
         jLabel4 = new javax.swing.JLabel();
         jLabel5 = new javax.swing.JLabel();
         jSeparator3 = new javax.swing.JSeparator();
         cmdOk = new javax.swing.JButton();
         cmdCancel = new javax.swing.JButton();
         panLoadAndInscribe = new javax.swing.JPanel();
         jLabel6 = new javax.swing.JLabel();
         jSeparator1 = new javax.swing.JSeparator();
         jSeparator4 = new javax.swing.JSeparator();
         panInscribe = new javax.swing.JPanel();
         panProgress = new javax.swing.JPanel();
         scpLoadingStatus = new javax.swing.JScrollPane();
         txpLoadingStatus = new javax.swing.JTextPane();
         prbLoading = new javax.swing.JProgressBar();
         cmdBack = new javax.swing.JButton();
 
         lbl1.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.lbl1.text")); // NOI18N
 
         txt1.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.txt1.text")); // NOI18N
 
         lbl2.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.lbl2.text")); // NOI18N
 
         txt2.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.txt2.text")); // NOI18N
 
         setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
         setTitle(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.title")); // NOI18N
         addComponentListener(new java.awt.event.ComponentAdapter() {
             public void componentShown(java.awt.event.ComponentEvent evt) {
                 formComponentShown(evt);
             }
         });
 
         panDesc.setBackground(java.awt.SystemColor.inactiveCaptionText);
 
         jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
         jLabel1.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.jLabel1.text")); // NOI18N
 
         jLabel2.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.jLabel2.text")); // NOI18N
 
         jLabel3.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.jLabel3.text")); // NOI18N
 
         jLabel4.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.jLabel4.text")); // NOI18N
 
         jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/cismet/cismap/commons/gui/res/frameprint.png"))); // NOI18N
 
         org.jdesktop.layout.GroupLayout panDescLayout = new org.jdesktop.layout.GroupLayout(panDesc);
         panDesc.setLayout(panDescLayout);
         panDescLayout.setHorizontalGroup(
             panDescLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jSeparator3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, panDescLayout.createSequentialGroup()
                 .addContainerGap(146, Short.MAX_VALUE)
                 .add(jLabel5)
                 .addContainerGap())
             .add(panDescLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(panDescLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(panDescLayout.createSequentialGroup()
                         .add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                         .addContainerGap())
                     .add(panDescLayout.createSequentialGroup()
                         .add(panDescLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                             .add(jLabel1)
                             .add(jLabel2)
                             .add(jLabel3)
                             .add(jLabel4))
                         .add(83, 83, 83))))
         );
         panDescLayout.setVerticalGroup(
             panDescLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(panDescLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(jLabel1)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jLabel2)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jLabel3)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jLabel4)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 58, Short.MAX_VALUE)
                 .add(jLabel5)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
 
         cmdOk.setMnemonic('O');
         cmdOk.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdOk.text")); // NOI18N
         cmdOk.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmdOkActionPerformed(evt);
             }
         });
 
         cmdCancel.setMnemonic('A');
         cmdCancel.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdCancel.text")); // NOI18N
         cmdCancel.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmdCancelActionPerformed(evt);
             }
         });
 
         jLabel6.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
         jLabel6.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.jLabel6.text")); // NOI18N
 
         panInscribe.setLayout(new java.awt.BorderLayout());
 
         txpLoadingStatus.setBackground(java.awt.SystemColor.control);
         txpLoadingStatus.setEditable(false);
         scpLoadingStatus.setViewportView(txpLoadingStatus);
 
         prbLoading.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
         prbLoading.setBorderPainted(false);
 
         org.jdesktop.layout.GroupLayout panProgressLayout = new org.jdesktop.layout.GroupLayout(panProgress);
         panProgress.setLayout(panProgressLayout);
         panProgressLayout.setHorizontalGroup(
             panProgressLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(prbLoading, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
             .add(scpLoadingStatus, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
         );
         panProgressLayout.setVerticalGroup(
             panProgressLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, panProgressLayout.createSequentialGroup()
                 .add(scpLoadingStatus, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                 .add(4, 4, 4)
                 .add(prbLoading, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
 
         org.jdesktop.layout.GroupLayout panLoadAndInscribeLayout = new org.jdesktop.layout.GroupLayout(panLoadAndInscribe);
         panLoadAndInscribe.setLayout(panLoadAndInscribeLayout);
         panLoadAndInscribeLayout.setHorizontalGroup(
             panLoadAndInscribeLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(panLoadAndInscribeLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(jLabel6)
                 .add(148, 148, 148))
             .add(jSeparator4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)
             .add(panLoadAndInscribeLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE))
             .add(panLoadAndInscribeLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(panProgress, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addContainerGap())
             .add(panLoadAndInscribeLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(panInscribe, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                 .addContainerGap())
         );
         panLoadAndInscribeLayout.setVerticalGroup(
             panLoadAndInscribeLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(panLoadAndInscribeLayout.createSequentialGroup()
                 .addContainerGap()
                 .add(jLabel6)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(panInscribe, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(panProgress, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
 
         cmdBack.setText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdBack.text")); // NOI18N
         cmdBack.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 cmdBackActionPerformed(evt);
             }
         });
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(layout.createSequentialGroup()
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                     .add(layout.createSequentialGroup()
                         .add(panDesc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                         .add(panLoadAndInscribe, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                     .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                         .add(cmdCancel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 125, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                         .add(cmdBack, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 125, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                         .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                         .add(cmdOk, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 126, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                 .addContainerGap())
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                     .add(panLoadAndInscribe, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .add(panDesc, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(cmdOk)
                     .add(cmdCancel)
                     .add(cmdBack))
                 .addContainerGap())
         );
 
         pack();
     }// </editor-fold>//GEN-END:initComponents
 
     private void cmdBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdBackActionPerformed
       dispose();
     }//GEN-LAST:event_cmdBackActionPerformed
 
   public void startLoading()
   {
     if(DEBUG)log.debug("startLoading()");//NOI18N
     txpLoadingStatus.setText("");//NOI18N
     try
     {
       Class c = Class.forName(mappingComponent.getPrintingSettingsDialog().getSelectedTemplate().getClassName());
       Constructor constructor = c.getConstructor();
       inscriber = (AbstractPrintingInscriber) constructor.newInstance();
     } catch (Exception e)
     {
       log.error("Error while loading the print template", e);//NOI18N
     }
     panInscribe.removeAll();
     panInscribe.add(inscriber, BorderLayout.CENTER);
 
     cmdOk.setEnabled(false);
     Template t = mappingComponent.getPrintingSettingsDialog().getSelectedTemplate();
 
     Resolution r = mappingComponent.getPrintingSettingsDialog().getSelectedResolution();
     addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.startLoading().msg", new Object[] {r.getResolution()}), EXPERT);//NOI18N
     BoundingBox bb = ((PrintingFrameListener) mappingComponent.getInputListener(MappingComponent.PRINTING_AREA_SELECTION)).getPrintingBoundingBox();
     imageWidth = (int) ((double) t.getMapWidth() / (double) PrintingFrameListener.DEFAULT_JAVA_RESOLUTION_IN_DPI * (double) r.getResolution());
     imageHeight = (int) ((double) t.getMapHeight() / (double) PrintingFrameListener.DEFAULT_JAVA_RESOLUTION_IN_DPI * (double) r.getResolution());
     if (DEBUG)
     {
       log.debug("image size: " + imageWidth + "x" + imageHeight);//NOI18N
     }
     if (DEBUG)
     {
       log.debug("BoundingBox:" + bb);//NOI18N
     }
     map = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
     map.getGraphics().setColor(Color.white);
     map.getGraphics().fillRect(0, 0, imageWidth, imageHeight);
     //map=Static2DTools.toCompatibleImage(map);
     services = new TreeMap<Integer, RetrievalService>();
     results = new TreeMap<Integer, Object>();
     erroneous = new TreeMap<Integer, Object>();
     mappingComponent.queryServicesIndependentFromMap(imageWidth, imageHeight, bb, this);
     prbLoading.setIndeterminate(true);
   }
 
     private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
     }//GEN-LAST:event_formComponentShown
 
     private void cmdCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdCancelActionPerformed
       mappingComponent.setInteractionMode(interactionModeAfterPrinting);
       mappingComponent.getPrintingFrameLayer().removeAllChildren();
       dispose();
     }//GEN-LAST:event_cmdCancelActionPerformed
 
     private void cmdOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdOkActionPerformed
       Runnable r = new Runnable()
       {
         @Override
         public void run()
         {
           Action a = mappingComponent.getPrintingSettingsDialog().getSelectedAction();
           if (a.getId().equalsIgnoreCase(Action.PDF))
           {
             java.awt.EventQueue.invokeLater(new Runnable()
             {
               @Override
               public void run()
               {
                 pdfWait.setLocationRelativeTo(PrintingWidget.this);
                 pdfWait.setVisible(true);
               }
             });
           }
           Template t = mappingComponent.getPrintingSettingsDialog().getSelectedTemplate();
           Scale s = mappingComponent.getPrintingSettingsDialog().getSelectedScale();
           mappingComponent.getPrintingFrameLayer().removeAllChildren();
           mappingComponent.setInteractionMode(interactionModeAfterPrinting);
           if (DEBUG)
           {
             log.debug("interactionModeAfterPrinting:" + interactionModeAfterPrinting);//NOI18N
           }
 
           try
           {
             HashMap param = new HashMap();
             param.put(t.getMapPlaceholder(), map);
             String scaleDenomString = "" + s.getDenominator();//NOI18N
             if (scaleDenomString.equals("0") || scaleDenomString.equals("-1"))//NOI18N
             {
               int sd = (int) (((PrintingFrameListener) mappingComponent.getInputListener(MappingComponent.PRINTING_AREA_SELECTION)).getScaleDenominator() + 0.5d); //+0.5=Runden
               scaleDenomString = "" + sd;//NOI18N
             }
             param.put(t.getScaleDemoninatorPlaceholder(), scaleDenomString);
             param.putAll(inscriber.getValues());
             if (DEBUG)
             {
               log.debug("Parameter:" + param);//NOI18N
             }
             //JasperReport jasperReport=(JasperReport)JRLoader.loadObject(new FileInputStream("c:\\Map1.jasper"));
 
             JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream(t.getFile()));
             //JasperReport jasperReport=JasperCompileManager.compileReport("c:\\Map1.jrxml");
             final JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, param);
             //JasperManager.printReportToPdfFile(jasperPrint, "c:\\\\SampleReport.pdf");
             //JasperViewer.viewReport(jasperPrint);
 
             if (a.getId().equalsIgnoreCase(Action.PRINTPREVIEW))
             {
               JRViewer aViewer = new JRViewer(jasperPrint);
               JFrame aFrame = new JFrame(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdOKActionPerformed(ActionEvent).aFrame.title"));//NOI18N
               aFrame.getContentPane().add(aViewer);
               java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
               aFrame.setSize(screenSize.width / 2, screenSize.height / 2);
               java.awt.Insets insets = aFrame.getInsets();
               aFrame.setSize(aFrame.getWidth() + insets.left + insets.right, aFrame.getHeight() + insets.top + insets.bottom + 20);
               aFrame.setLocation((screenSize.width - aFrame.getWidth()) / 2, (screenSize.height - aFrame.getHeight()) / 2);
               aFrame.setVisible(true);
             } else if (a.getId().equalsIgnoreCase(Action.PDF))
             {
               String home = System.getProperty("user.home");//NOI18N
               String fs = System.getProperty("file.separator");//NOI18N
 
               String file = home + fs + "cismap.pdf"; //TODO//NOI18N
               File f = new File(file);
               file = file.replaceAll("\\\\", "/");//NOI18N
               file = file.replaceAll(" ", "%20");//NOI18N
               //String file="cismap.pdf"; //MesseHotfix
 
               JasperExportManager.exportReportToPdfFile(jasperPrint, f.toString());
 
               log.info("try to open pdf:" + file);//NOI18N
               de.cismet.tools.BrowserLauncher.openURL("file:///" + file);//NOI18N
 //                        try {
 //                            if (Desktop.isDesktopSupported()) {
 //                                Desktop desktop =Desktop.getDesktop();
 //                                //desktop.browse(new URI("file:///"+file));
 //                                desktop.open(f);
 //                            }
 //                            else {
 //                                log.fatal("Desktop not supported");
 //                            }
 //                        } catch (Throwable tt) {
 //                            log.fatal("Konnte PDF nicht oeffnen",tt);
 //                        }
               java.awt.EventQueue.invokeLater(new Runnable()
               {
 
                 @Override
                 public void run()
                 {
                   pdfWait.dispose();
                 }
               });
             } else if (a.getId().equalsIgnoreCase(Action.PRINT))
             {
               JasperPrintManager.printReport(jasperPrint, true);
             }
           } catch (Throwable tt)
           {
             log.error("Error during Jaspern", tt);//NOI18N
 
             ErrorInfo ei = new ErrorInfo(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdOKActionPerformed(ActionEvent).ErrorInfo.title"), //NOI18N
                     org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.cmdOKActionPerformed(ActionEvent).ErrorInfo.message"), //NOI18N
                     null, null, tt, Level.ALL, null);
             JXErrorPane.showDialog(PrintingWidget.this, ei);
 //                    JXErrorDialog.showDialog(, "Fehler beim Drucken", "Beim Erzeugen des Ausdruckes ist ein Fehler aufgetreten.\nStellen Sie sicher das das PDF aus dem letzen Druckvorgang\ngeschlossen oder unter anderem Namen abgespeichert\nwurde.", tt);
 
             if (pdfWait.isVisible())
             {
               pdfWait.dispose();
             }
           }
         }
       };
       CismetThreadPool.execute(r);
       dispose();
     }//GEN-LAST:event_cmdOkActionPerformed
 
   /**
    * @param args the command line arguments
    */
   public static void main(String args[])
   {
     java.awt.EventQueue.invokeLater(new Runnable()
     {
 
       @Override
       public void run()
       {
         //new PrintingWidget(new javax.swing.JFrame(), true).setVisible(true);
       }
     });
   }
 
   public String getInteractionModeAfterPrinting()
   {
     return interactionModeAfterPrinting;
   }
 
   public void setInteractionModeAfterPrinting(String interactionModeAfterPrinting)
   {
     this.interactionModeAfterPrinting = interactionModeAfterPrinting;
   }
 
   @Override
   public void retrievalStarted(RetrievalEvent e)
   {
     if (DEBUG)
     {
       log.debug("retrievalStarted" + e.getRetrievalService());//NOI18N
     }
 
     if(e.isInitialisationEvent())
     {
       log.error(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalStarted ignored, initialisation event");//NOI18N
       return;
     }
 
     addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalStarted(RetrievalEvent).msg", new Object[] {e.getRetrievalService()}), INFO);//NOI18N
     if (e.getRetrievalService() instanceof ServiceLayer)
     {
       int num = ((ServiceLayer) e.getRetrievalService()).getLayerPosition();
       services.put(num, e.getRetrievalService());
     }
   }
 
   @Override
   public void retrievalProgress(RetrievalEvent e)
   {
     if (DEBUG)
     {
       log.debug(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrieval progress: " + e.getPercentageDone());//NOI18N
     }
 
     if(e.isInitialisationEvent())
     {
       log.error(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalProgress ignored, initialisation event");//NOI18N
       return;
     }
   }
 
   @Override
   public void retrievalError(RetrievalEvent e)
   {
     log.error(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalError");//NOI18N
 
     if(e.isInitialisationEvent())
     {
       log.error(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalError ignored, initialisation event");//NOI18N
       return;
     }
 
     addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalError(RetrievalEvent).msg1", new Object[] {e.getRetrievalService()}), ERROR);//NOI18N
     addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalError(RetrievalEvent).msg2"), ERROR_REASON);//NOI18N
     retrievalComplete(e);
 //        if (e.getRetrievalService() instanceof  ServiceLayer) {
 //            int num=((ServiceLayer)e.getRetrievalService()).getLayerPosition();
 //            erroneous.put(num,e);
 //        }
 //        for (Object error:e.getErrors()) {
 //            addMessageToProgressPane(error.getClass().toString(),ERROR_REASON);
 //            addMessageToProgressPane(error.toString(),ERROR_REASON);
 //        }
 //        log.error("retrievalError"+e.getRetrievalService());
     }
 
   @Override
   public void retrievalComplete(RetrievalEvent e)
   {
     if (log.isInfoEnabled()) {
       log.info(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalComplete");//NOI18N
     }
     if(e.isInitialisationEvent())
     {
       log.error(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalComplete ignored, initialisation event");//NOI18N
       return;
     }
 
     if (e.getRetrievalService() instanceof ServiceLayer)
     {
       int num = ((ServiceLayer) e.getRetrievalService()).getLayerPosition();
       if (!e.isHasErrors())
       {
         results.put(num, e.getRetrievedObject());
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg", new Object[] {e.getRetrievalService()}), SUCCESS);//NOI18N
       } else
       {
         erroneous.put(num, e);
         if (e.getRetrievedObject() instanceof Image)
         {
           //Image scaled=Static2DTools.scaleImage((Image)e.getRetrievedObject(),0.7);
           Image i = Static2DTools.removeUnusedBorder((Image) e.getRetrievedObject(), 5, 0.7);
           addIconToProgressPane(errorImage, i);
           addMessageToProgressPane( org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg2", new Object[] {e.getRetrievalService()}), ERROR_REASON);//NOI18N
         }
       }
     }
 
     if (results.size() + erroneous.size() == services.size())
     {
       if (results.size() == services.size())
       {
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg6"), SUCCESS);//NOI18N
       } else if (erroneous.size() == services.size())
       {
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg7"), WARN);//NOI18N
       } else
       {
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg8"), WARN);//NOI18N
       }
 
       for (Integer i : results.keySet())
       {
         Graphics2D g2d = (Graphics2D) map.getGraphics();
         //Transparency
         RetrievalService rs = services.get(i);
 
         float transparency = 0f;
         if (rs instanceof ServiceLayer)
         {
           transparency = ((ServiceLayer) rs).getTranslucency();
         }
         Composite alphaComp = AlphaComposite.getInstance(
                 AlphaComposite.SRC_OVER, transparency);
         g2d.setComposite(alphaComp);
         Object o = results.get(i);
 
         log.info("processing results of type '" + o.getClass().getSimpleName() + "' from service #" + i + " '" + rs + "' ("+rs.getClass().getSimpleName()+")");//NOI18N
         if (o instanceof Image)
         {
           log.debug("service '" + rs + "' returned an image, must be a raster service");//NOI18N
           Image image2add = (Image) o;
           g2d.drawImage(image2add, 0, 0, null);
         } 
         else if (Collection.class.isAssignableFrom(o.getClass()))
         {
           Collection featureCollection = (Collection) o;
           if (DEBUG)
           {
             log.debug("service '" + rs + "' returned a collection, must be a feature service ("+featureCollection.size()+" features retrieved)");//NOI18N
           }
           Image image2add = mappingComponent.getImageOfFeatures(featureCollection, imageWidth, imageHeight);
           g2d.drawImage(image2add, 0, 0, null);
         }
         else
         {
           log.error("unknown results retrieved: "+o.getClass().getSimpleName());//NOI18N
         }
       }
 
       //Add Existing Features as TopLevelLayer
      if(mappingComponent.isFeatureCollectionVisible()){
       try
       {
         Graphics2D g2d = (Graphics2D) map.getGraphics();
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg3"), INFO);//NOI18N
 
         //Transparency
         float transparency = 0f;
         transparency = mappingComponent.getFeatureLayer().getTransparency();
         Composite alphaComp = AlphaComposite.getInstance(
                 AlphaComposite.SRC_OVER, transparency);
         g2d.setComposite(alphaComp);
         Resolution r = mappingComponent.getPrintingSettingsDialog().getSelectedResolution();
         Template t = mappingComponent.getPrintingSettingsDialog().getSelectedTemplate();
         imageWidth = (int) ((double) t.getMapWidth() / (double) PrintingFrameListener.DEFAULT_JAVA_RESOLUTION_IN_DPI * (double) r.getResolution());
         imageHeight = (int) ((double) t.getMapHeight() / (double) PrintingFrameListener.DEFAULT_JAVA_RESOLUTION_IN_DPI * (double) r.getResolution());
         Image image2add = mappingComponent.getFeatureImage(imageWidth, imageHeight);
         g2d.drawImage(image2add, 0, 0, null);
 
       } catch (Throwable t)
       {
         log.error("Error while adding local features to the map", t);//NOI18N
       }
      } else {
         final String localFeaturesNotAddedMessage = org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg9");
         addMessageToProgressPane(localFeaturesNotAddedMessage, INFO);//NOI18N
         log.debug(localFeaturesNotAddedMessage);
      }
 
       if (erroneous.size() < results.size())
       {
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg4"), SUCCESS);//NOI18N
       } else
       {
         addMessageToProgressPane(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.retrievalComplete(RetrievalEvent).msg5"), INFO);//NOI18N
       }
 
       if (DEBUG)
       {
         log.debug("ALLE FERTIG");//NOI18N
         log.debug("results:" + results);//NOI18N
         log.debug("services:" + services);//NOI18N
       }
 
       prbLoading.setIndeterminate(false);
       prbLoading.setValue(100);
       cmdOk.setEnabled(true);
     }
   }
 
   @Override
   public void retrievalAborted(RetrievalEvent e)
   {
     log.warn(e.getRetrievalService() + "[" + e.getRequestIdentifier() + "]: retrievalAborted");//NOI18N
   }
 
   private void addIconToProgressPane(final ImageIcon icon, final Image tooltipImage)
   {
     final JLabel label = new JLabel()
     {
 
       @Override
       public JToolTip createToolTip()
       {
         if (tooltipImage != null)
         {
           return new ImageToolTip(tooltipImage);
         } else
         {
           return super.createToolTip();
         }
       }
     };
     synchronized (this)
     {
       java.awt.EventQueue.invokeLater(new Runnable()
       {
 
         @Override
         public void run()
         {
           StyledDocument doc = (StyledDocument) txpLoadingStatus.getDocument();
           Style style = doc.addStyle("Icon", null);//NOI18N
           label.setIcon(icon);
           label.setText(" ");//NOI18N
           //label.setVerticalAlignment(SwingConstants.TOP);
           label.setAlignmentY(0.8f);
           label.setToolTipText(org.openide.util.NbBundle.getMessage(PrintingWidget.class, "PrintingWidget.addIconToProgressPane(ImageIcon,Image).label.setToolTipText"));//NOI18N
           StyleConstants.setComponent(style, label);
           try
           {
             doc.insertString(doc.getLength(), "ico", style);//NOI18N
           } catch (BadLocationException ble)
           {
             log.error("Error in addIconToProgressPane", ble);//NOI18N
           }
         }
       });
     }
   }
 
   private void addMessageToProgressPane(final String msg, final String reason)
   {
     synchronized (this)
     {
       java.awt.EventQueue.invokeLater(new Runnable()
       {
 
         @Override
         public void run()
         {
           try
           {
             txpLoadingStatus.getStyledDocument().insertString(txpLoadingStatus.getStyledDocument().getLength(), msg + "\n", styles.get(reason));//NOI18N
           } catch (BadLocationException ble)
           {
             log.error("error during Insert", ble);//NOI18N
           }
         }
       });
     }
 //        // txpLoadingStatus.setCaretPosition(txpLoadingStatus.getText().length());
     }
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton cmdBack;
     private javax.swing.JButton cmdCancel;
     private javax.swing.JButton cmdOk;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel3;
     private javax.swing.JLabel jLabel4;
     private javax.swing.JLabel jLabel5;
     private javax.swing.JLabel jLabel6;
     private javax.swing.JSeparator jSeparator1;
     private javax.swing.JSeparator jSeparator2;
     private javax.swing.JSeparator jSeparator3;
     private javax.swing.JSeparator jSeparator4;
     private javax.swing.JLabel lbl1;
     private javax.swing.JLabel lbl2;
     private javax.swing.JPanel panDesc;
     private javax.swing.JPanel panInscribe;
     private javax.swing.JPanel panLoadAndInscribe;
     private javax.swing.JPanel panProgress;
     private javax.swing.JProgressBar prbLoading;
     private javax.swing.JScrollPane scpLoadingStatus;
     private javax.swing.JTextPane txpLoadingStatus;
     private javax.swing.JTextField txt1;
     private javax.swing.JTextField txt2;
     // End of variables declaration//GEN-END:variables
 }
