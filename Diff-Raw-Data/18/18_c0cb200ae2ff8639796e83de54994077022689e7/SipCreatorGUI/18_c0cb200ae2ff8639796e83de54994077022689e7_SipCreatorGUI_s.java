 /*
  * Copyright 2007 EDL FOUNDATION
  *
  *  Licensed under the EUPL, Version 1.0 or? as soon they
  *  will be approved by the European Commission - subsequent
  *  versions of the EUPL (the "Licence");
  *  you may not use this work except in compliance with the
  *  Licence.
  *  You may obtain a copy of the Licence at:
  *
  *  http://ec.europa.eu/idabc/eupl
  *
  *  Unless required by applicable law or agreed to in
  *  writing, software distributed under the Licence is
  *  distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  *  express or implied.
  *  See the Licence for the specific language governing
  *  permissions and limitations under the Licence.
  */
 
 package eu.europeana.sip.gui;
 
 import eu.delving.core.metadata.MetadataModel;
 import eu.delving.core.metadata.MetadataModelImpl;
 import eu.europeana.sip.core.RecordValidationException;
 import eu.europeana.sip.model.FileSet;
 import eu.europeana.sip.model.SipModel;
 import eu.europeana.sip.model.UserNotifier;
 import org.apache.log4j.Logger;
 
 import javax.swing.JFrame;
 import javax.swing.JMenuBar;
 import javax.swing.JOptionPane;
 import javax.swing.JTabbedPane;
 import javax.swing.SwingUtilities;
 import java.awt.BorderLayout;
 import java.awt.EventQueue;
 import java.awt.Toolkit;
 import java.io.IOException;
 
 /**
  * The main GUI class for the sip creator
  *
  * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
  * @author Serkan Demirel <serkan@blackbuilt.nl>
  */
 
 public class SipCreatorGUI extends JFrame {
     private Logger log = Logger.getLogger(getClass());
     private SipModel sipModel;
 
    public SipCreatorGUI(Class<?> beanClass, String serverUrl) {
         super("SIP Creator");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         this.sipModel = new SipModel(loadMetadataModel(), new PopupExceptionHandler(), serverUrl);
         JTabbedPane tabs = new JTabbedPane();
         tabs.addTab("Analysis", new AnalysisPanel(sipModel));
         tabs.addTab("Mapping", new MappingPanel(sipModel));
         tabs.addTab("Refinement", new RefinementPanel(sipModel));
         tabs.addTab("Normalization", new NormalizationPanel(sipModel));
         if (serverUrl != null) {
             tabs.addTab("Repository", new DataSetPanel(sipModel));
         }
         getContentPane().add(tabs, BorderLayout.CENTER);
         setJMenuBar(createMenuBar());
 //        setSize(1024, 768);
         setSize(Toolkit.getDefaultToolkit().getScreenSize());
     }
 
     private JMenuBar createMenuBar() {
         JMenuBar bar = new JMenuBar();
         FileMenu fileMenu = new FileMenu(this, sipModel, new FileMenu.SelectListener() {
             @Override
             public boolean select(FileSet fileSet) {
                 if (!fileSet.isValid()) {
                     return false;
                 }
                 else {
                     fileSet.setExceptionHandler(new PopupExceptionHandler());
                     sipModel.setFileSet(fileSet);
                     setTitle(String.format("SIP Creator - %s", fileSet.getAbsolutePath()));
                     return true;
                 }
             }
         });
         bar.add(fileMenu);
         MappingTemplateMenu mappingTemplateMenu = new MappingTemplateMenu(this, sipModel);
         bar.add(mappingTemplateMenu);
         return bar;
     }
 
     private MetadataModel loadMetadataModel() {
         try {
             MetadataModelImpl metadataModel = new MetadataModelImpl();
            metadataModel.setRecordDefinitionResource("/record-definition.xml");
             return metadataModel;
         }
         catch (IOException e) {
             e.printStackTrace();
             System.exit(1);
             return null;
         }
     }
 
     private class PopupExceptionHandler implements UserNotifier {
 
         @Override
         public void tellUser(final String message, final Exception exception) {
             SwingUtilities.invokeLater(new Runnable() {
                 @Override
                 public void run() {
                     String html = String.format("<html><h3>%s</h3><p>%s</p></html>", message, exception.getMessage());
                     if (exception instanceof RecordValidationException) {
                         RecordValidationException rve = (RecordValidationException) exception;
                         StringBuilder problemHtml = new StringBuilder(String.format("<html><h3>%s</h3><ul>", message));
                         for (String problem : rve.getProblems()) {
                             problemHtml.append(String.format("<li>%s</li>", problem));
                         }
                         problemHtml.append("</ul></html>");
                         html = problemHtml.toString();
                     }
                     JOptionPane.showMessageDialog(SipCreatorGUI.this, html);
                 }
             });
             if (exception != null) {
                 log.warn(message, exception);
             }
             else {
                 log.warn(message);
             }
         }
 
         @Override
         public void tellUser(String message) {
             tellUser(message, null);
         }
     }
 
     public static void main(final String[] args) throws ClassNotFoundException {
        if (args.length != 2) {
            throw new RuntimeException("SipCreatorGUI gets two parameters <bean-class-name> <server-url>");
        }
        final Class<?> beanClass = Class.forName(args[0]);
        final String serverUrl = args[1];
         EventQueue.invokeLater(new Runnable() {
             public void run() {
                SipCreatorGUI sipCreatorGUI = new SipCreatorGUI(beanClass, serverUrl);
                 sipCreatorGUI.setVisible(true);
             }
         });
     }
 }
