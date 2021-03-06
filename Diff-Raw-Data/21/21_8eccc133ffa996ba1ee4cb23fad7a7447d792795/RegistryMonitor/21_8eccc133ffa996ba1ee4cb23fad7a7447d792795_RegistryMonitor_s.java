 /***************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 *
 *              ... and it just works.
 *
 ****************************************************/
 package Sirius.server.registry.monitor;
 
 import Sirius.util.*;
 
 import Sirius.server.naming.*;
 import Sirius.server.newuser.*;
 import Sirius.server.*;
 import Sirius.server.registry.*;
 import Sirius.server.registry.events.*;
 
 import java.rmi.*;
 import java.rmi.server.*;
 
 import java.util.*;
 
 import java.awt.*;
 import java.awt.event.*;
 
 import javax.swing.*;
 import javax.swing.event.*;
 import javax.swing.table.*;
 
 /**
  * Der RegistryMonitor ist ein Frontend fuer die Registry (Sirius.Registry) Der Monitor stellt die Server, die beim der
  * Registry registriert sind, in einer Tabelle dar. Dabei fragt der Monitor die Registry zyklisch ab, um die
  * Anzeigetabellen aktuell zu halten. Das UpdateIntervall ist einstellbar. Ausserdem ist ein manuelles Update jederzeit
  * m\u00F6glich.
  *
  * @author   Bernd Kiefer, Rene Wendling
  * @version  1.0
  *
  *           <p>letzte Aenderung: 24.10.2000</p>
  */
 public class RegistryMonitor extends JPanel implements Runnable {
 
     //~ Instance fields --------------------------------------------------------
 
     /** Referenz auf den NameServer der Registry.* */
     private NameServer nameServer;
 
     /** Referenz auf den UserServer der Registry.* */
     private UserServer userServer;
 
     /** die IP-Adresse der Registry.* */
     private String registryIP;
 
     /** Die Tabellenueberschriften fuer die Anzeigetabellen Server.* */
     private java.lang.Object[] columnnamesForServer = { "Name", "IP", "Port" };  // NOI18N
 
     /** Die Tabellenueberschriften fuer die Anzeigetabelle User.* */
     private java.lang.Object[] columnnamesForUser = { "ID", "Name", "local Servername", "Usergroup", "Admin" };  // NOI18N
 
     /** umgewandelter Typ: Sirius.Server.Server[] nach java.lang.Object[][] * */
     private Server[] callServer;
 
     /** umgewandelter Typ: Sirius.Server.Server[] nach java.lang.Object[][] * */
     private Server[] localServer;
 
     /** umgewandelter Typ: Sirius.Server.Server[] nach java.lang.Object[][] * */
     private Server[] protocolServer;
 
     /** umgewandelter Typ: Sirius.Server.Server[] nach java.lang.Object[][] * */
     private Server[] translServer;
 
     /** Tabelle die alle aktiven CallServer anzeigt.* */
     private JTable callServerTable;
 
     /** Tabelle die alle aktiven LocalServer anzeigt.* */
     private JTable localServerTable;
 
     /** Tabelle die alle aktiven ProtocolServer anzeigt.* */
     private JTable protocolServerTable;
 
     /** Tabelle, die alle registrierten TranslationServer anzeigt.* */
     // private JTable translServerTable;
 
     /** Tabelle, die alle registrierten User anzeigt.* */
     private JTable userTable;
 
     /** Vector der die User enth\u00E4lt.* */
     private Vector users;
 
     /** zeigt eventuell auftretende Fehlermeldungen an.* */
     private JLabel messageLabel;
 
     /** Wert fuer das UpdateIntervall.* */
     private int updateIntervall = 60;
 
     /** Ueberschrift fuer den CentralServerMonitor.* */
     private String panelHeader;
     // ----------------------------------------------------------------------------------------
 
     //~ Constructors -----------------------------------------------------------
 
     /**
      * Konstruktor.
      *
      * @param  registryIP  args[0] die IP-Adresse des CentralServers *
      */
     public RegistryMonitor(String registryIP) {
         this.registryIP = registryIP;
 
         // MessageLabel setzen
         messageLabel = new JLabel();
 
         // ueberschrift fuer den Monitor
        panelHeader = org.openide.util.NbBundle.getMessage(RegistryMonitor.class, "RegistryMonitor.panelHeader", registryIP);
 
         try {
             // Referenz auf NameServer und UserServer der Registry erzeugen
             nameServer = (NameServer)Naming.lookup("rmi://" + registryIP + "/nameServer");  // NOI18N
             userServer = (UserServer)nameServer; // (UserServer) Naming.lookup("rmi://"+registryIP+"/userServer");
 
             // abfragen der aktiven Server
             callServer = nameServer.getServers(ServerType.CALLSERVER);
             localServer = nameServer.getServers(ServerType.LOCALSERVER);
             protocolServer = nameServer.getServers(ServerType.PROTOCOLSERVER);
             // translServer   = nameServer.getServers(ServerType.TRANSLATIONSERVER);
 
             // abfragen der aktiven User
             users = new Vector(userServer.getUsers());
 
             // Layout initialisieren
             initMainPanel();
 
             // update-Schleife starten
             new Thread(this, "updateThread").start();  // NOI18N
         } catch (Exception e) {
             // wenn CentralServer auf IP-Adresse nicht vorhanden, Panel wird trotzdem initialisiert
             messageLabel.setText(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.messageLabel.text.noregistry", registryIP));  // NOI18N
 
 
             initMainPanel();
 
             // start der update-Schleife
             new Thread(this, "updateThread").start();  // NOI18N
         }
     }
 
     //~ Methods ----------------------------------------------------------------
 
     /**
      * ----------------------------------------------------------------------------------------
      *
      * @param  intervall  DOCUMENT ME!
      */
     public void setUpdateIntervall(int intervall) {
         this.updateIntervall = intervall;
     }
 
     // ----------------------------------------------------------------------------------------
 
     /**
      * fragt die SiriusRegisty/NameServer nach aktuellen Servern ab. *
      */
     private void update() {
         try {
             // Referenz wird neu angelegt, um zu pr\u00FCfen, ob der SiriusRegistry noch laeuft
             nameServer = (NameServer)Naming.lookup("rmi://" + registryIP + "/nameServer");  // NOI18N
 
             // get all Servers
             callServer = nameServer.getServers(ServerType.CALLSERVER);
             localServer = nameServer.getServers(ServerType.LOCALSERVER);
             protocolServer = nameServer.getServers(ServerType.PROTOCOLSERVER);
             // translServer   = nameServer.getServers(ServerType.TRANSLATIONSERVER);
 
             users = new Vector(userServer.getUsers());
         } catch (Exception e) {
             messageLabel.setForeground(Color.red);
             message(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.messageLabel.text.registrydown", registryIP));   // NOI18N
 
         }
     }
 
     // ----------------------------------------------------------------------------------------
 
     /**
      * fragt SiriusRegistry/NameServer nach aktuellen Servern ab und aktualisiert die Tabellen.*
      */
     public void updateTables() {
         update();
         try {
             MonitorTableModel tmLocalServers = (MonitorTableModel)localServerTable.getModel();
             MonitorTableModel tmCallServers = (MonitorTableModel)callServerTable.getModel();
             MonitorTableModel tmProtocolServers = (MonitorTableModel)protocolServerTable.getModel();
             // MonitorTableModel tmTranslServers       = (MonitorTableModel)translServerTable.getModel();
             MonitorTableModel tmUserServers = (MonitorTableModel)userTable.getModel();
 
             tmLocalServers.setDataVector(MonitorTableModel.convertToMatrix(localServer), columnnamesForServer);
             tmCallServers.setDataVector(MonitorTableModel.convertToMatrix(callServer), columnnamesForServer);
             tmProtocolServers.setDataVector(MonitorTableModel.convertToMatrix(protocolServer), columnnamesForServer);
             // tmTranslServers.setDataVector(MonitorTableModel.convertToMatrix(translServer),columnnamesForServer);
             tmUserServers.setDataVector(MonitorTableModel.convertToMatrix(users), columnnamesForUser);
 
             message(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.messageLabel.text.lastupdate", new Date(System.currentTimeMillis()))); // NOI18N
         } catch (IllegalArgumentException e) {
             localServerTable.setModel(new MonitorTableModel());
             callServerTable.setModel(new MonitorTableModel());
             protocolServerTable.setModel(new MonitorTableModel());
             // translServerTable.setModel(new MonitorTableModel());
             userTable.setModel(new MonitorTableModel());
         }
     }
 
     // ----------------------------------------------------------------------------------------
 
     /**
      * Funktion zum initialisieren des Layouts.*
      */
     private void initMainPanel() {
         // es wird versucht die Tabellen zu initialisiern. Ist kein CentralServer vorhanden,
         // k\u00F6nnen die Variablen call-, local- und protocolServer nicht initialisiert werden,
         // Exception wird ausgeworfen. Dann wird ein TableModel ohne Parameter zugewiesen
         try {
             callServerTable = new JTable(
                     new MonitorTableModel(MonitorTableModel.convertToMatrix(callServer), columnnamesForServer));
             localServerTable = new JTable(
                     new MonitorTableModel(MonitorTableModel.convertToMatrix(localServer), columnnamesForServer));
             protocolServerTable = new JTable(
                     new MonitorTableModel(MonitorTableModel.convertToMatrix(protocolServer), columnnamesForServer));
             // translServerTable         = new JTable(new
             // MonitorTableModel(MonitorTableModel.convertToMatrix(translServer),columnnamesForServer));
             userTable = new JTable(new MonitorTableModel(MonitorTableModel.convertToMatrix(users), columnnamesForUser));
         } catch (IllegalArgumentException e) {
             callServerTable = new JTable(new MonitorTableModel());
             localServerTable = new JTable(new MonitorTableModel());
             protocolServerTable = new JTable(new MonitorTableModel());
             // translServerTable   = new JTable (new MonitorTableModel());
             userTable = new JTable(new MonitorTableModel());
         } catch (Exception e) {
             e.printStackTrace();
             System.err.println("initmainPanel");  // NOI18N
         }
 
         // JTabbedPane erzeugen und Tabellen hinzufuegen
         JTabbedPane allServerPane = new JTabbedPane();
         allServerPane.add(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().allServerPane.tab1.title"),  // NOI18N
                 new JScrollPane(localServerTable));
         allServerPane.add(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().allServerPane.tab2.title"),  // NOI18N
                 new JScrollPane(callServerTable));
         // allServerPane.add("TranslationServers", new JScrollPane(translServerTable));
         allServerPane.add(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().allServerPane.tab3.title"),  // NOI18N
                 new JScrollPane(protocolServerTable));
         allServerPane.add(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().allServerPane.tab4.title"),  // NOI18N
                 new JScrollPane(userTable));
 
         // UpdateButton fuer manuelles Update
         JButton updateButton = new JButton(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().updateButton.text"));  // NOI18N
         updateButton.addActionListener(new MonitorUpdateListener(this));
 
         // Panel fuer updateIntervall Einstellungen
         JPanel timePanel = new JPanel();
         ButtonGroup buttonGroup = new ButtonGroup();
         JRadioButton oneMin = (new JRadioButton(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().oneMin.text")));  // NOI18N
         JRadioButton fiveMin = new JRadioButton(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().fiveMin.text"));  // NOI18N
         JRadioButton tenMin = new JRadioButton(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().tenMin.text"));  // NOI18N
         MonitorIntervallListener il = new MonitorIntervallListener(this);
         oneMin.addActionListener(il);
         fiveMin.addActionListener(il);
         tenMin.addActionListener(il);
         oneMin.setSelected(true);
 
         buttonGroup.add(oneMin);
         buttonGroup.add(fiveMin);
         buttonGroup.add(tenMin);
 
         timePanel.add(oneMin);
         timePanel.add(fiveMin);
         timePanel.add(tenMin);
         timePanel.setBorder(BorderFactory.createTitledBorder(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().timePanel.border.title")));  // NOI18N
 
         // MessageLabel und MessagePanel
         JPanel messagePanel = new JPanel();
         messagePanel.setBorder(BorderFactory.createTitledBorder(
                 org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.initMainPanel().messagePanel.border.title")));  // NOI18N
         messagePanel.add(messageLabel);
 
         JPanel buttonAndMessagePanel = new JPanel();
         buttonAndMessagePanel.setLayout(new BorderLayout());
         buttonAndMessagePanel.add(updateButton, BorderLayout.NORTH);
         buttonAndMessagePanel.add(messagePanel, BorderLayout.CENTER);
 
         // HauptPaneleistellungen
         setLayout(new BorderLayout());
         setBorder(BorderFactory.createTitledBorder(panelHeader));
         add(timePanel, BorderLayout.NORTH);
         add(allServerPane, BorderLayout.CENTER);
         add(buttonAndMessagePanel, BorderLayout.SOUTH);
     }
 
     // ----------------------------------------------------------------------------------------
     /**
      * Schleife zum automatischen aktualisieren der Tabellen, Intervall kann ueber Variable updateIntervall gesetzt
      * werden. *
      */
     public void run() {
         while (true) {
             try {
                 Thread t = Thread.currentThread();
                 messageLabel.setForeground(new Color(102, 102, 153));
                 message(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.messageLabel.text.lastupdate", new Date(System.currentTimeMillis())));  // NOI18N
                 t.sleep(updateIntervall * 1000);
                 updateTables();
             } catch (InterruptedException e) {
             }
         }
     }
     /**
      * ----------------------------------------------------------------------------------------
      *
      * @param  message  DOCUMENT ME!
      */
     public void message(String message) {
         messageLabel.setText(message);
     }
 
     // ----------------------------------------------------------------------------------------
 
     /**
      * MainFunktion zum testen des CentralServerMonitor.*
      *
      * @param  args  DOCUMENT ME!
      */
     public static void main(String[] args) {
         // RegistryMonitor monitor = new RegistryMonitor(args[0]);
         RegistryMonitor monitor = new RegistryMonitor("192.168.0.1");   // NOI18N
         JFrame frame = new JFrame(org.openide.util.NbBundle.getMessage(RegistryMonitor.class,"RegistryMonitor.main(String[]).frame.title"));  // NOI18N
 
         frame.getContentPane().add(monitor);
         frame.setSize(400, 400);
         frame.setVisible(true);
     }
 }
