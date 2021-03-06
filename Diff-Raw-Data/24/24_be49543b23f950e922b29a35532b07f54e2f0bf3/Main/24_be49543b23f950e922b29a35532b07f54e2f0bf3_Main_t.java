 /*
  * Main.java
  *
  * Created on 6. červenec 2007, 15:37
  */
 
 package esmska;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Event;
 import java.awt.SystemColor;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyEvent;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.swing.AbstractAction;
 import javax.swing.AbstractListModel;
 import javax.swing.Action;
 import javax.swing.DefaultCellEditor;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.DefaultListCellRenderer;
 import javax.swing.ImageIcon;
 import javax.swing.InputVerifier;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JList;
 import javax.swing.JTextField;
 import javax.swing.KeyStroke;
 import javax.swing.ListCellRenderer;
 import javax.swing.ListSelectionModel;
 import javax.swing.Timer;
 import javax.swing.ToolTipManager;
 import javax.swing.UIManager;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentEvent.ElementChange;
 import javax.swing.event.DocumentEvent.EventType;
 import javax.swing.event.DocumentListener;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.event.TableModelEvent;
 import javax.swing.event.TableModelListener;
 import javax.swing.event.UndoableEditEvent;
 import javax.swing.event.UndoableEditListener;
 import javax.swing.table.AbstractTableModel;
 import javax.swing.table.DefaultTableCellRenderer;
 import javax.swing.table.TableColumn;
 import javax.swing.text.AbstractDocument;
 import javax.swing.text.AttributeSet;
 import javax.swing.text.BadLocationException;
 import javax.swing.text.Document;
 import javax.swing.text.DocumentFilter;
 import javax.swing.text.Element;
 import javax.swing.text.JTextComponent;
 import javax.swing.text.Style;
 import javax.swing.text.StyleConstants;
 import javax.swing.text.StyleContext;
 import javax.swing.text.StyledDocument;
 import javax.swing.undo.UndoManager;
 import operators.O2;
 import operators.Operator;
 import operators.OperatorEnum;
 import operators.Vodafone;
 import persistence.ConfigBean;
 import persistence.Contact;
 import persistence.ContactsBean;
 import persistence.PersistenceManager;
 
 /** Main form
  *
  * @author  ripper
  */
 public class Main extends javax.swing.JFrame {
     
     private Action quitAction = new QuitAction();
     private SendAction sendAction = new SendAction();
     private Action smsQueuePauseAction = new SMSQueuePauseAction();
     private Action deleteSMSAction = new DeleteSMSAction();
     private Action editSMSAction = new EditSMSAction();
     private Action aboutAction = new AboutAction();
     private Action configAction = new ConfigAction();
     private Action addContactAction = new AddContactAction();
     private Action removeContactAction = new RemoveContactAction();
     private Action smsUpAction = new SMSUpAction();
     private Action smsDownAction = new SMSDownAction();
     private Action smsTextUndoAction;
     private Action smsTextRedoAction;
     private JFrame aboutFrame, configFrame;
     private SMSTextPaneListener smsTextPaneListener = new SMSTextPaneListener();
     private SMSTextPaneDocumentFilter smsTextPaneDocumentFilter;
     
     /** actual queue of sms's */
     private List<SMS> smsQueue = Collections.synchronizedList(new ArrayList<SMS>());
     /** sender of sms */
     private SMSSender smsSender = new SMSSender(smsQueue, this);
     /** timer to send another sms after defined delay */
     private Timer smsDelayTimer = new Timer(1000,new SMSDelayActionListener());
     /** support for undo and redo in sms text pane */
     private UndoManager smsTextUndoManager = new UndoManager();
     /** manager of persistence data */
     PersistenceManager persistenceManager;
     /** program configuration */
     ConfigBean config;
     /** sms contacts */
     ContactsBean contacts;
     
     /** Creates new form Main */
     public Main() {
         initComponents();
         //set tooltip delay
         ToolTipManager.sharedInstance().setInitialDelay(500);
         ToolTipManager.sharedInstance().setDismissDelay(10000);
         //load config
         try {
             persistenceManager = PersistenceManager.getPersistenceManager();
         } catch (IOException ex) {
             ex.printStackTrace();
             printStatusMessage("Nepovedlo se vytvořit adresář s nastavením programu!");
         }
         loadConfig();
         //load contacts
         loadContacts();
         //init custom components
         smsDelayProgressBar.setVisible(false);
         smsDelayTimer.setInitialDelay(0);
         configAction.setEnabled(config != null);
         
         contactTable.setModel(new ContactTableModel());
         TableColumn opColumn = contactTable.getColumnModel().getColumn(2);
         JComboBox comboBox = new JComboBox();
         comboBox.setModel(new DefaultComboBoxModel(OperatorEnum.getAsList().toArray()));
         opColumn.setCellEditor(new DefaultCellEditor(comboBox));
         contactTable.getModel().addTableModelListener(new ContactTableModelListener());
         
     }
     
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
     private void initComponents() {
         mainPanel = new javax.swing.JPanel();
         statusPanel = new javax.swing.JPanel();
         jSeparator1 = new javax.swing.JSeparator();
         statusMessageLabel = new javax.swing.JLabel();
         statusAnimationLabel = new javax.swing.JLabel();
         smsDelayProgressBar = new javax.swing.JProgressBar();
         smsPanel = new javax.swing.JPanel();
         jLabel4 = new javax.swing.JLabel();
         jLabel1 = new javax.swing.JLabel();
         smsNumberTextField = new javax.swing.JTextField();
         jScrollPane1 = new javax.swing.JScrollPane();
         smsTextPane = new javax.swing.JTextPane();
         jLabel5 = new javax.swing.JLabel();
         operatorComboBox = new javax.swing.JComboBox();
         sendButton = new javax.swing.JButton();
         smsCounterLabel = new javax.swing.JLabel();
         jLabel2 = new javax.swing.JLabel();
         nameLabel = new javax.swing.JLabel();
         queuePanel = new javax.swing.JPanel();
         jScrollPane2 = new javax.swing.JScrollPane();
         smsQueueList = new javax.swing.JList();
         pauseButton = new javax.swing.JButton();
         editButton = new javax.swing.JButton();
         deleteButton = new javax.swing.JButton();
         smsUpButton = new javax.swing.JButton();
         smsDownButton = new javax.swing.JButton();
         contactPanel = new javax.swing.JPanel();
         jScrollPane3 = new javax.swing.JScrollPane();
         contactTable = new javax.swing.JTable();
         addContactButton = new javax.swing.JButton();
         removeContactButton = new javax.swing.JButton();
         menuBar = new javax.swing.JMenuBar();
         programMenu = new javax.swing.JMenu();
         configMenuItem = new javax.swing.JMenuItem();
         aboutMenuItem = new javax.swing.JMenuItem();
         exitMenuItem = new javax.swing.JMenuItem();
 
         javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
         mainPanel.setLayout(mainPanelLayout);
         mainPanelLayout.setHorizontalGroup(
             mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGap(0, 311, Short.MAX_VALUE)
         );
         mainPanelLayout.setVerticalGroup(
             mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGap(0, 161, Short.MAX_VALUE)
         );
 
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
         setTitle("Esmska");
         setIconImage(new ImageIcon(getClass().getResource("resources/esmska.png")).getImage());
         addWindowListener(new java.awt.event.WindowAdapter() {
             public void windowClosing(java.awt.event.WindowEvent evt) {
                 formWindowClosing(evt);
             }
         });
 
         statusMessageLabel.setText("V\u00edtejte");
 
         statusAnimationLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/task-idle.png")));
 
         smsDelayProgressBar.setMaximum(15);
         smsDelayProgressBar.setString("Dal\u0161\u00ed sms za: ");
         smsDelayProgressBar.setStringPainted(true);
 
         javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
         statusPanel.setLayout(statusPanelLayout);
         statusPanelLayout.setHorizontalGroup(
             statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
             .addGroup(statusPanelLayout.createSequentialGroup()
                 .addComponent(statusMessageLabel)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 392, Short.MAX_VALUE)
                 .addComponent(smsDelayProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                 .addComponent(statusAnimationLabel))
         );
         statusPanelLayout.setVerticalGroup(
             statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
             .addGroup(statusPanelLayout.createSequentialGroup()
                 .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                     .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                         .addComponent(statusMessageLabel)
                         .addComponent(statusAnimationLabel))
                     .addComponent(smsDelayProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
         );
 
         smsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Zpr\u00e1va"));
         jLabel4.setText("\u010c\u00edslo");
 
         jLabel1.setText("+420");
 
         smsNumberTextField.setInputVerifier(new InputVerifier() {
             public boolean verify(JComponent input) {
                 JTextField tf = (JTextField) input;
                 if (tf.getText().length() != 9
                     && tf.getText().length() != 0)
                 return false;
                 for (Character c : tf.getText().toCharArray()) {
                     if (!Character.isDigit(c))
                     return false;
                 }
                 return true;
             }
         });
         smsNumberTextField.addKeyListener(new java.awt.event.KeyAdapter() {
             public void keyReleased(java.awt.event.KeyEvent evt) {
                 smsNumberTextFieldKeyReleased(evt);
             }
         });
 
         smsTextPane.setBackground(SystemColor.text);
         smsTextPane.getDocument().addDocumentListener(smsTextPaneListener);
         smsTextPaneDocumentFilter = new SMSTextPaneDocumentFilter();
         ((AbstractDocument)smsTextPane.getStyledDocument()).setDocumentFilter(smsTextPaneDocumentFilter);
 
         //set undo and redo actions and bind them
         smsTextUndoManager.setLimit(-1);
         smsTextUndoAction = new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 if (smsTextUndoManager.canUndo()) {
                     smsTextUndoManager.undo();
                     smsTextPaneDocumentFilter.requestUpdate();
                 }
             }
         };
         smsTextRedoAction = new AbstractAction() {
             public void actionPerformed(ActionEvent e) {
                 if (smsTextUndoManager.canRedo()) {
                     smsTextUndoManager.redo();
                     smsTextPaneDocumentFilter.requestUpdate();
                 }
             }
         };
         smsTextPane.getDocument().addUndoableEditListener(new UndoableEditListener() {
             public void undoableEditHappened(UndoableEditEvent e) {
                 if (e.getEdit().getPresentationName().contains("style"))
                 return;
                 smsTextUndoManager.addEdit(e.getEdit());
             }
         });
         smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK),"undo");
         smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK),"redo");
         smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
             KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK),"redo");
     smsTextPane.getActionMap().put("undo",smsTextUndoAction);
     smsTextPane.getActionMap().put("redo",smsTextRedoAction);
     jScrollPane1.setViewportView(smsTextPane);
 
     jLabel5.setText("Text");
 
     operatorComboBox.setModel(new DefaultComboBoxModel(OperatorEnum.getAsList().toArray()));
     operatorComboBox.setRenderer(new OperatorComboBoxRenderer());
     //operatorComboBox.setRenderer(operatorComboBox.getRenderer());
     operatorComboBox.addActionListener(new OperatorComboBoxActionListener());
     operatorComboBox.setSelectedItem(operatorComboBox.getSelectedItem());
 
     sendButton.setAction(sendAction);
 
     smsCounterLabel.setText("0 znak\u016f (0 sms)");
 
     jLabel2.setText("Jm\u00e9no");
 
     nameLabel.setForeground(new java.awt.Color(0, 51, 255));
 
     javax.swing.GroupLayout smsPanelLayout = new javax.swing.GroupLayout(smsPanel);
     smsPanel.setLayout(smsPanelLayout);
     smsPanelLayout.setHorizontalGroup(
         smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(smsPanelLayout.createSequentialGroup()
             .addContainerGap()
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addComponent(jLabel2)
                 .addComponent(jLabel4)
                 .addComponent(jLabel5))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addGroup(smsPanelLayout.createSequentialGroup()
                     .addComponent(jLabel1)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(smsNumberTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(operatorComboBox, 0, 84, Short.MAX_VALUE))
                 .addComponent(nameLabel)
                 .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                 .addGroup(smsPanelLayout.createSequentialGroup()
                     .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(sendButton)))
             .addGap(12, 12, 12))
     );
     smsPanelLayout.setVerticalGroup(
         smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(smsPanelLayout.createSequentialGroup()
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                 .addComponent(jLabel2)
                 .addComponent(nameLabel))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                 .addComponent(jLabel4)
                 .addComponent(jLabel1)
                 .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                 .addComponent(smsNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addComponent(jLabel5)
                 .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addComponent(sendButton)
                 .addComponent(smsCounterLabel))
             .addContainerGap())
     );
 
     queuePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Fronta"));
     smsQueueList.setModel(new SMSQueueListModel());
     smsQueueList.setCellRenderer(new SMSQueueListRenderer());
     smsQueueList.setLayoutOrientation(javax.swing.JList.VERTICAL_WRAP);
     smsQueueList.setVisibleRowCount(-1);
     smsQueueList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
         public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
             smsQueueListValueChanged(evt);
         }
     });
 
     jScrollPane2.setViewportView(smsQueueList);
 
     pauseButton.setAction(smsQueuePauseAction);
     pauseButton.setBorderPainted(false);
     pauseButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     editButton.setAction(editSMSAction);
     editButton.setBorderPainted(false);
     editButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     deleteButton.setAction(deleteSMSAction);
     deleteButton.setBorderPainted(false);
     deleteButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     smsUpButton.setAction(smsUpAction);
     smsUpButton.setBorderPainted(false);
     smsUpButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     smsDownButton.setAction(smsDownAction);
     smsDownButton.setBorderPainted(false);
     smsDownButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     javax.swing.GroupLayout queuePanelLayout = new javax.swing.GroupLayout(queuePanel);
     queuePanel.setLayout(queuePanelLayout);
     queuePanelLayout.setHorizontalGroup(
         queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(queuePanelLayout.createSequentialGroup()
             .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addComponent(smsUpButton)
                 .addComponent(smsDownButton))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addGroup(queuePanelLayout.createSequentialGroup()
                     .addComponent(editButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                 .addComponent(pauseButton))
             .addContainerGap())
     );
 
     queuePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});
 
     queuePanelLayout.setVerticalGroup(
         queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(queuePanelLayout.createSequentialGroup()
             .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)
                     .addComponent(deleteButton, javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.LEADING, queuePanelLayout.createSequentialGroup()
                         .addComponent(editButton)
                         .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                         .addComponent(pauseButton)))
                 .addGroup(queuePanelLayout.createSequentialGroup()
                     .addComponent(smsUpButton)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(smsDownButton)))
             .addContainerGap())
     );
 
     queuePanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});
 
     contactPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Kontakty"));
     contactTable.setAutoCreateRowSorter(true);
     contactTable.setModel(new javax.swing.table.DefaultTableModel(
         new Object [][] {
             {null, null, null},
             {null, null, null},
             {null, null, null},
             {null, null, null}
         },
         new String [] {
             "Title 1", "Title 2", "Title 3"
         }
     ));
     contactTable.getSelectionModel().addListSelectionListener(new ContactTableSelectionListener());
     ((DefaultTableCellRenderer)contactTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
     jScrollPane3.setViewportView(contactTable);
 
     addContactButton.setAction(addContactAction);
     addContactButton.setBorderPainted(false);
     addContactButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     removeContactButton.setAction(removeContactAction);
     removeContactButton.setBorderPainted(false);
     removeContactButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
 
     javax.swing.GroupLayout contactPanelLayout = new javax.swing.GroupLayout(contactPanel);
     contactPanel.setLayout(contactPanelLayout);
     contactPanelLayout.setHorizontalGroup(
         contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, contactPanelLayout.createSequentialGroup()
             .addContainerGap()
             .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                 .addComponent(addContactButton)
                 .addComponent(removeContactButton))
             .addContainerGap())
     );
 
     contactPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addContactButton, removeContactButton});
 
     contactPanelLayout.setVerticalGroup(
         contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(contactPanelLayout.createSequentialGroup()
             .addGroup(contactPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addGroup(contactPanelLayout.createSequentialGroup()
                     .addComponent(addContactButton)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(removeContactButton))
                 .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE))
             .addContainerGap())
     );
 
     contactPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {addContactButton, removeContactButton});
 
     programMenu.setText("Program");
     configMenuItem.setAction(configAction);
     programMenu.add(configMenuItem);
 
     aboutMenuItem.setAction(aboutAction);
     aboutMenuItem.setText("O programu");
     programMenu.add(aboutMenuItem);
 
     exitMenuItem.setAction(quitAction);
     programMenu.add(exitMenuItem);
 
     menuBar.add(programMenu);
 
     setJMenuBar(menuBar);
 
     javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
     getContentPane().setLayout(layout);
     layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addComponent(statusPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
             .addContainerGap()
             .addComponent(smsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                 .addComponent(queuePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                 .addComponent(contactPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
             .addContainerGap())
     );
     layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
             .addContainerGap()
             .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                 .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                     .addComponent(contactPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(queuePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                 .addComponent(smsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
             .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
             .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
     );
     pack();
     }// </editor-fold>//GEN-END:initComponents
     
     private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
         saveConfig();
         saveContacts();
     }//GEN-LAST:event_formWindowClosing
     
     private void smsQueueListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_smsQueueListValueChanged
         //update form components
         if (!evt.getValueIsAdjusting()) {
             int queueSize = smsQueueList.getModel().getSize();
             int selectedItems = smsQueueList.getSelectedIndices().length;
             deleteSMSAction.setEnabled(queueSize != 0 && selectedItems != 0);
             editSMSAction.setEnabled(queueSize != 0 && selectedItems == 1);
             smsUpAction.setEnabled(queueSize != 0 && selectedItems == 1);
             smsDownAction.setEnabled(queueSize != 0 && selectedItems == 1);
         }
     }//GEN-LAST:event_smsQueueListValueChanged
     
     private void smsNumberTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_smsNumberTextFieldKeyReleased
         //clear name label
         nameLabel.setText("");
         
         //color background (number ok/not ok)
         boolean ok = smsNumberTextField.getInputVerifier().verify(smsNumberTextField);
         if (!ok || smsNumberTextField.getText().isEmpty())
             smsNumberTextField.setBackground(Color.RED);
         else
             smsNumberTextField.setBackground(Color.GREEN);
         
         //guess operator
         Operator op = OperatorEnum.getOperator(smsNumberTextField.getText());
         if (op != null) {
             for (int i=0; i<operatorComboBox.getItemCount(); i++) {
                 if (operatorComboBox.getItemAt(i).getClass().equals(op.getClass())) {
                     operatorComboBox.setSelectedIndex(i);
                     break;
                 }
             }
         }
         
         //update send action
         sendAction.updateStatus();
     }//GEN-LAST:event_smsNumberTextFieldKeyReleased
     
     /**
      * @param args the command line arguments
      */
     public static void main(String[] args) {
         //set native L&F
         try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception ex) {
             Logger.getLogger("global").log(Level.SEVERE, "Error setting L&F", ex);
         }
         
         //start main frame
         java.awt.EventQueue.invokeLater(new java.lang.Runnable() {
             public void run() {
                 new esmska.Main().setVisible(true);
             }
         });
     }
     
     /** Prints message to status bar */
     public void printStatusMessage(String message) {
         statusMessageLabel.setText(message);
     }
     
     /** Tells main form whether it should display task busy icon */
     public void setTaskRunning(boolean b) {
         if (b == false)
             statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource("resources/task-idle.png")));
         else
             statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource("resources/task-busy.gif")));
     }
     
     /** Notifies about change in sms queue */
     public void smsProcessed(SMS sms) {
         int index = smsQueue.indexOf(sms);
         if (sms.getStatus() == SMS.Status.SENT_OK) {
             smsQueue.remove(sms);
             ((SMSQueueListModel)smsQueueList.getModel()).fireIntervalRemoved(
                     smsQueueList.getModel(), index, index);
         }
         if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
             ((SMSQueueListModel)smsQueueList.getModel()).fireContentsChanged(
                     smsQueueList.getModel(), index, index);
         }
     }
     
     /** Pauses sms queue */
     public void pauseSMSQueue() {
         smsQueuePauseAction.actionPerformed(null);
     }
     
     /** Forces delay before sending another sms */
     public void setSMSDelay() {
         smsSender.setDelayed(true);
         smsDelayTimer.start();
     }
     
     /** save program configuration */
     private void saveConfig() {
         //save sms queue
         ArrayList<SMS> list = new ArrayList<SMS>();
         for (SMS sms : smsQueue) {
             //erase connection properties
             sms.setErrMsg(null);
             sms.setImage(null);
             sms.setImageCode(null);
             sms.setStatus(null);
             list.add(sms);
         }
         config.setSmsQueue(list);
         
         try {
             persistenceManager.saveConfig();
         } catch (IOException ex) {
             ex.printStackTrace();
             printStatusMessage("Nepodařilo se uložit nastavení!");
         }
     }
     
     /** load program configuration */
     private void loadConfig() {
         try {
             persistenceManager.loadConfig();
         } catch (IOException ex) {
             ex.printStackTrace();
             printStatusMessage("Nepodařilo se načíst konfiguraci!");
             return;
         }
         config = persistenceManager.getConfig();
         
         if (config.isRememberQueue()) { //load sms queue
             if (config.getSmsQueue().size() != 0)
                 pauseSMSQueue();
             smsQueue.addAll(config.getSmsQueue());
             if (config.getSmsQueue().size() != 0)
                 ((SMSQueueListModel)smsQueueList.getModel()).fireIntervalAdded(
                         smsQueueList.getModel(), 0, smsQueue.size()-1);
         }
     }
     
     /** load contacts */
     private void loadContacts() {
         try {
             persistenceManager.loadContacts();
         } catch (IOException ex) {
             ex.printStackTrace();
             printStatusMessage("Nepodařilo se načíst kontakty!");
             return;
         }
         contacts = persistenceManager.getContacs();
         contacts.sortContacts();
     }
     
     /** save contacts */
     private void saveContacts() {
         try {
             persistenceManager.saveContacts();
         } catch (IOException ex) {
             ex.printStackTrace();
             printStatusMessage("Nepodařilo se uložit kontakty!");
         }
     }
     
     /** Show about frame */
     private class AboutAction extends AbstractAction {
         public AboutAction() {
             super("O programu", new ImageIcon(Main.this.getClass().getResource("resources/about-small.png")));
         }
         public void actionPerformed(ActionEvent e) {
             if (aboutFrame == null)
                 aboutFrame = new AboutFrame();
             aboutFrame.setLocationRelativeTo(Main.this);
             aboutFrame.setVisible(true);
         }
     }
     
     /** Show config frame */
     private class ConfigAction extends AbstractAction {
         public ConfigAction() {
             super("Nastavení", new ImageIcon(Main.this.getClass().getResource("resources/config-small.png")));
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             if (configFrame == null)
                 configFrame = new ConfigFrame(config);
             configFrame.setLocationRelativeTo(Main.this);
             configFrame.setVisible(true);
         }
     }
     
     /** Send sms to queue */
     private class SendAction extends AbstractAction {
         public SendAction() {
             super("Poslat", new ImageIcon(Main.this.getClass().getResource("resources/send.png")));
             putValue(MNEMONIC_KEY, KeyEvent.VK_S);
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             //text must be non-empty
             if (smsNumberTextField.getText().isEmpty()) {
                 smsNumberTextField.requestFocusInWindow();
                 return;
             }
             
             SMS sms = new SMS();
             sms.setNumber(smsNumberTextField.getText());
             sms.setText(smsTextPane.getText());
             sms.setOperator((Operator)operatorComboBox.getSelectedItem());
             if (config.isUseSenderID()) { //append signature if requested
                 sms.setSenderNumber(config.getSenderNumber());
                 sms.setSenderName(config.getSenderName());
             }
             sms.setName(nameLabel.getText());
             
             smsQueue.add(sms);
             int index = smsQueue.indexOf(sms);
             ((SMSQueueListModel)smsQueueList.getModel()).fireIntervalAdded(
                     smsQueueList.getModel(), index, index);
             smsSender.announceNewSMS();
             
             smsTextPane.setText(null);
             smsTextUndoManager.discardAllEdits();
             smsTextPane.requestFocusInWindow();
         }
         /** update status according to conditions  */
         public void updateStatus() {
             boolean ok = true;
             // valid number
             if (ok && !smsNumberTextField.getInputVerifier().verify(smsNumberTextField))
                 ok = false;
             // non-empty sms text
             if (ok && smsTextPane.getText().length() == 0)
                 ok = false;
             
             this.setEnabled(ok);
         }
     }
     
     /** Pause/unpause the sms queue */
     private class SMSQueuePauseAction extends AbstractAction {
         private boolean makePause = true;
         public SMSQueuePauseAction() {
             super(null, new ImageIcon(Main.this.getClass().getResource("resources/pause.png")));
             this.putValue(SHORT_DESCRIPTION,"Pozastavit odesílání sms ve frontě");
         }
         public void actionPerformed(ActionEvent e) {
             if (makePause) {
                 smsSender.setPaused(true);
                 this.putValue(LARGE_ICON_KEY, new ImageIcon(Main.this.getClass().getResource("resources/start.png")));
                 this.putValue(SHORT_DESCRIPTION,"Pokračovat v odesílání sms ve frontě");
             } else {
                 smsSender.setPaused(false);
                 this.putValue(LARGE_ICON_KEY,new ImageIcon(Main.this.getClass().getResource("resources/pause.png")));
                 this.putValue(SHORT_DESCRIPTION,"Pozastavit odesílání sms ve frontě");
             }
             makePause = !makePause;
         }
     }
     
     /** Erase sms from queue list */
     private class DeleteSMSAction extends AbstractAction {
         public DeleteSMSAction() {
             super(null, new ImageIcon(Main.this.getClass().getResource("resources/delete.png")));
             this.putValue(SHORT_DESCRIPTION,"Odstranit označené zprávy");
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             Object[] smsArray = smsQueueList.getSelectedValues();
             for (Object o : smsArray) {
                 SMS sms = (SMS) o;
                 int index = smsQueue.indexOf(sms);
                 smsQueue.remove(sms);
                 ((SMSQueueListModel)smsQueueList.getModel()).fireIntervalRemoved(
                         smsQueueList.getModel(), index, index);
             }
         }
     }
     
     /** Quit the program */
     private class QuitAction extends AbstractAction {
         public QuitAction() {
             super("Ukončit", new ImageIcon(Main.this.getClass().getResource("resources/exit-small.png")));
         }
         public void actionPerformed(ActionEvent e) {
             System.exit(0);
         }
     }
     
     /** Edit sms from queue */
     private class EditSMSAction extends AbstractAction {
         public EditSMSAction() {
             super(null, new ImageIcon(Main.this.getClass().getResource("resources/edit.png")));
             this.putValue(SHORT_DESCRIPTION,"Upravit označenou zprávu");
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             SMS sms = (SMS) smsQueueList.getSelectedValue();
             if (sms == null)
                 return;
             smsNumberTextField.setText(sms.getNumber());
             smsTextPane.setText(sms.getText());
             operatorComboBox.setSelectedItem(sms.getOperator());
             nameLabel.setText(sms.getName());
             int index = smsQueue.indexOf(sms);
             smsQueue.remove(sms);
             ((SMSQueueListModel)smsQueueList.getModel()).fireIntervalRemoved(
                     smsQueueList.getModel(), index, index);
             contactTable.clearSelection();
             smsTextPane.requestFocusInWindow();
         }
     }
     
     /** Add contact to contact table */
     private class AddContactAction extends AbstractAction {
         public AddContactAction() {
             super(null,new ImageIcon(Main.this.getClass().getResource("resources/add.png")));
             this.putValue(SHORT_DESCRIPTION,"Přidat nový kontakt");
         }
         public void actionPerformed(ActionEvent e) {
             Contact c = new Contact("Nový","",new Vodafone());
             contacts.getContacts().add(c);
             contacts.sortContacts();
             int contactIndex = contactTable.convertRowIndexToView(contacts.getContacts().indexOf(c));
            ((ContactTableModel)contactTable.getModel()).fireTableRowsInserted(contactIndex,contactIndex);
             contactTable.getSelectionModel().setSelectionInterval(contactIndex,contactIndex);
             //ensure new item is visible
             contactTable.scrollRectToVisible(contactTable.getCellRect(contactIndex,0,true));
             //edit first column
             contactTable.editCellAt(contactIndex,0);
             Component editor = contactTable.getEditorComponent();
             if (editor instanceof JTextComponent) {
                 ((JTextComponent)editor).selectAll();
                 editor.requestFocusInWindow();
             }
         }
     }
     
     /** Remove contact from contact table */
     private class RemoveContactAction extends AbstractAction {
         public RemoveContactAction() {
             super(null,new ImageIcon(Main.this.getClass().getResource("resources/remove.png")));
             this.putValue(SHORT_DESCRIPTION,"Odstranit označené kontakty");
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             //stop cell editing
             if (contactTable.getCellEditor() != null) {
                 contactTable.getCellEditor().stopCellEditing();
             }
             //remove selected rows
             int[] selectedRows = contactTable.getSelectedRows();
            int minIndex = contactTable.getSelectionModel().getMinSelectionIndex();
            int maxIndex = contactTable.getSelectionModel().getMaxSelectionIndex();
             ArrayList<Contact> list = new ArrayList<Contact>();
             //TODO sorting
             for (int i : selectedRows)
                 list.add(contacts.getContacts().get(contactTable.convertRowIndexToModel(i)));
             contacts.getContacts().removeAll(list);
            ((ContactTableModel)contactTable.getModel()).fireTableRowsDeleted(minIndex,maxIndex);
         }
     }
     
     /** move sms up in sms queue */
     private class SMSUpAction extends AbstractAction {
         public SMSUpAction() {
             super(null,new ImageIcon(Main.this.getClass().getResource("resources/up.png")));
             this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě výše");
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             int index = smsQueueList.getSelectedIndex();
             if (index <= 0) //cannot move up first item
                 return;
             synchronized(smsQueue) {
                 SMS sms = smsQueue.get(index);
                 smsQueue.set(index,smsQueue.get(index-1));
                 smsQueue.set(index-1,sms);
             }
             ((SMSQueueListModel)smsQueueList.getModel()).fireContentsChanged(
                     smsQueueList.getModel(), index-1, index);
             smsQueueList.setSelectedIndex(index-1);
             smsQueueList.ensureIndexIsVisible(index-1);
         }
     }
     
     /** move sms down in sms queue */
     private class SMSDownAction extends AbstractAction {
         public SMSDownAction() {
             super(null,new ImageIcon(Main.this.getClass().getResource("resources/down.png")));
             this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě níže");
             this.setEnabled(false);
         }
         public void actionPerformed(ActionEvent e) {
             int index = smsQueueList.getSelectedIndex();
             if (index < 0 || index >= smsQueueList.getModel().getSize() - 1) //cannot move down last item
                 return;
             synchronized(smsQueue) {
                 SMS sms = smsQueue.get(index);
                 smsQueue.set(index,smsQueue.get(index+1));
                 smsQueue.set(index+1,sms);
             }
             ((SMSQueueListModel)smsQueueList.getModel()).fireContentsChanged(
                     smsQueueList.getModel(), index, index+1);
             smsQueueList.setSelectedIndex(index+1);
             smsQueueList.ensureIndexIsVisible(index+1);
         }
     }
     
     /** Model for SMSQueueList */
     private class SMSQueueListModel extends AbstractListModel {
         public Object getElementAt(int index) {
             return smsQueue.get(index);
         }
         public int getSize() {
             return smsQueue.size();
         }
         
         protected void fireIntervalRemoved(Object source, int index0, int index1) {
             super.fireIntervalRemoved(source, index0, index1);
         }
         
         protected void fireIntervalAdded(Object source, int index0, int index1) {
             super.fireIntervalAdded(source, index0, index1);
         }
         
         protected void fireContentsChanged(Object source, int index0, int index1) {
             super.fireContentsChanged(source, index0, index1);
         }
     }
     
     /** Progress bar action listener after sending sms */
     private class SMSDelayActionListener implements ActionListener {
         private final int DELAY = 15;
         private int seconds = 0;
         public void actionPerformed(ActionEvent e) {
             if (seconds <= DELAY) { //still wainting
                 smsDelayProgressBar.setValue(seconds);
                 smsDelayProgressBar.setString("Další sms za: " + (DELAY-seconds) + "s");
                 if (seconds == 0)
                     smsDelayProgressBar.setVisible(true);
                 seconds++;
             } else { //delay finished
                 smsDelayTimer.stop();
                 smsDelayProgressBar.setVisible(false);
                 seconds = 0;
                 smsSender.setDelayed(false);
                 smsSender.announceNewSMS();
             }
         }
     }
     
     /** Renderer for items in queue list */
     private class SMSQueueListRenderer implements ListCellRenderer {
         public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
             Component c = (new DefaultListCellRenderer()).getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
             SMS sms = (SMS)value;
             //problematic sms colored
             if ((sms.getStatus() == SMS.Status.PROBLEMATIC) && !isSelected) {
                 c.setBackground(Color.RED);
             }
             //add operator logo
             if (sms.getOperator() instanceof Vodafone) {
                 ((JLabel)c).setIcon(new ImageIcon(sms.getOperator().getClass().getResource("resources/Vodafone.png")));
             }
             if (sms.getOperator() instanceof O2) {
                 ((JLabel)c).setIcon(new ImageIcon(sms.getOperator().getClass().getResource("resources/O2.png")));
             }
             //set tooltip
             ((JLabel)c).setToolTipText(wrapToHTML(sms.getText()));
             
             return c;
         }
         /** transform string to html with linebreaks */
         private String wrapToHTML(String text) {
             StringBuilder output = new StringBuilder();
             output.append("<html>");
             int from = 0;
             while (from < text.length()) {
                 int to = from + 50;
                 to = text.indexOf(' ',to);
                 if (to < 0)
                     to = text.length();
                 output.append(text.substring(from, to));
                 output.append("<br>");
                 from = to + 1;
             }
             output.append("</html>");
             return output.toString();
         }
     }
     
     /** Renderer for items in operator combo box */
     private class OperatorComboBoxRenderer implements ListCellRenderer {
         public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
             Component c = (new DefaultListCellRenderer()).getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
             Operator operator = (Operator)value;
             if (operator instanceof Vodafone) {
                 ((JLabel)c).setIcon(new ImageIcon(operator.getClass().getResource("resources/Vodafone.png")));
             }
             if (operator instanceof O2) {
                 ((JLabel)c).setIcon(new ImageIcon(operator.getClass().getResource("resources/O2.png")));
             }
             return c;
         }
     }
     
     /** Listener for sms text pane */
     private class SMSTextPaneListener implements DocumentListener {
         /** count number of chars in sms and take action */
         private void countChars(DocumentEvent e) {
             int chars = e.getDocument().getLength();
             Operator op = (Operator)operatorComboBox.getSelectedItem();
             smsCounterLabel.setText(chars + " znaků (" + op.getSMSCount(chars) + " sms)");
             if (chars > op.getMaxChars()) { //chars more than max
                 smsCounterLabel.setForeground(Color.RED);
                 smsCounterLabel.setText(chars + " znaků (nelze odeslat!)");
             } else //chars ok
                 smsCounterLabel.setForeground(SystemColor.textText);
         }
         /** update form components */
         private void updateUI(DocumentEvent e) {
             sendAction.updateStatus();
         }
         public void changedUpdate(DocumentEvent e) {
             countChars(e);
             updateUI(e);
         }
         public void insertUpdate(DocumentEvent e) {
             countChars(e);
             updateUI(e);
         }
         public void removeUpdate(DocumentEvent e) {
             countChars(e);
             updateUI(e);
         }
     }
     
     /** Another operator selected */
     private class OperatorComboBoxActionListener implements ActionListener {
         public void actionPerformed(ActionEvent e) {
             //update text editor listeners
             DocumentEvent event = new DocumentEvent() {
                 public ElementChange getChange(Element elem) {
                     return null;
                 }
                 public Document getDocument() {
                     return smsTextPane.getDocument();
                 }
                 public int getLength() {
                     return 0;
                 }
                 public int getOffset() {
                     return 0;
                 }
                 public EventType getType() {
                     return EventType.INSERT;
                 }
             };
             smsTextPaneListener.insertUpdate(event);
             
             //set size and color filter to text editor
             smsTextPaneDocumentFilter.setMaxChars(((Operator)operatorComboBox.getSelectedItem()).getMaxChars());
             smsTextPaneDocumentFilter.setSmsLength(((Operator)operatorComboBox.getSelectedItem()).getSMSLength());
         }
     }
     
     /** Limit maximum sms length and color it */
     private class SMSTextPaneDocumentFilter extends DocumentFilter {
         private int maxChars;  //max chars in message
         private int smsLength; //length of 1 sms
         private StyledDocument doc;
         private Style regular, highlight;
         private Timer timer = new Timer(500, new ActionListener() { //updating after each event is slow,
             public void actionPerformed(ActionEvent e) {    //therefore there is timer
                 colorDocument(0,doc.getLength());
             }
         });
         public SMSTextPaneDocumentFilter() {
             super();
             timer.setRepeats(false);
             //set styles
             doc = smsTextPane.getStyledDocument();
             Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
             regular = doc.addStyle("regular", def);
             StyleConstants.setForeground(regular,SystemColor.textText);
             highlight = doc.addStyle("highlight", def);
             StyleConstants.setForeground(highlight, Color.BLUE);
         }
         /** color parts of sms */
         private void colorDocument(int from, int length) {
             while (from < length) {
                 int to = ((from / smsLength) + 1) * smsLength - 1;
                 to = to<length-1?to:length-1;
                 doc.setCharacterAttributes(from,to-from+1,getStyle(from),false);
                 from = to + 1;
             }
         }
         /** calculate which style is appropriate for given position */
         private Style getStyle(int offset) {
             if ((offset / smsLength) % 2 == 0) //even sms
                 return regular;
             else
                 return highlight;
         }
         public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
             if ((fb.getDocument().getLength() + text.length() - length) > maxChars) //reached size limit
                 return;
             super.replace(fb, offset, length, text, getStyle(offset));
             if ((offset + (text!=null?text.length():0) != fb.getDocument().getLength()) //not adding to end
             || (text!=null?text.length():1)!=1) //adding more than 1 char
                 timer.restart();
         }
         public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
             if ((fb.getDocument().getLength() + string.length()) > maxChars) //reached size limit
                 return;
             super.insertString(fb, offset, string, attr);
             timer.restart();
         }
         public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
             super.remove(fb, offset, length);
             if (offset != fb.getDocument().getLength()) //not removing from end
                 timer.restart();
         }
         /** request recoloring externally */
         public void requestUpdate() {
             timer.restart();
         }
         public void setMaxChars(int maxChars) {
             this.maxChars = maxChars;
         }
         public void setSmsLength(int smsLength) {
             this.smsLength = smsLength;
         }
         
     }
     
     /** Model for contact table */
     private class ContactTableModel extends AbstractTableModel {
         public int getRowCount() {
             return contacts.getContacts().size();
         }
         public int getColumnCount() {
             return 3;
         }
         public Object getValueAt(int rowIndex, int columnIndex) {
             Contact contact = contacts.getContacts().get(rowIndex);
             switch (columnIndex) {
                 case 0: return contact.getName();
                 case 1: return contact.getNumber();
                 case 2: return contact.getOperator();
                 default: return null;
             }
         }
         public String getColumnName(int column) {
             switch (column) {
                 case 0: return "Jméno";
                 case 1: return "Číslo";
                 case 2: return "Operátor";
                 default: return null;
             }
         }
         public Class<?> getColumnClass(int columnIndex) {
             switch (columnIndex) {
                 case 0: return String.class;
                 case 1: return String.class;
                 case 2: return Object.class;
                 default: return Object.class;
             }
         }
         public boolean isCellEditable(int rowIndex, int columnIndex) {
             switch (columnIndex) {
                 case 0:
                 case 1:
                 case 2: return true;
                 default: return false;
             }
         }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
             Contact contact = contacts.getContacts().get(rowIndex);
             switch (columnIndex) {
                 case 0: contact.setName((String)aValue); break;
                 case 1: contact.setNumber((String)aValue); break;
                 case 2: contact.setOperator((Operator)aValue); break;
             }
             super.fireTableCellUpdated(rowIndex,columnIndex);
         }
         public void fireTableDataChanged() {
             super.fireTableDataChanged();
         }
        public void fireTableRowsInserted(int firstRow, int lastRow) {
            super.fireTableRowsInserted(firstRow, lastRow);
        }
        public void fireTableRowsDeleted(int firstRow, int lastRow) {
            super.fireTableRowsDeleted(firstRow, lastRow);
        }
     }
     
     /** Listener for contact table */
     private class ContactTableSelectionListener implements ListSelectionListener {
         public void valueChanged(ListSelectionEvent e) {
             if (e.getValueIsAdjusting())
                 return;
             ListSelectionModel lsm = (ListSelectionModel)e.getSource();
             // update components
             removeContactAction.setEnabled(!lsm.isSelectionEmpty());
             // fill sms components with current contact
             int i;
             if ((i = lsm.getMinSelectionIndex()) >=0 ) {
                 nameLabel.setText(
                         (String) contactTable.getValueAt(i,contactTable.convertColumnIndexToView(0)));
                 smsNumberTextField.setText(
                         (String)contactTable.getValueAt(i,contactTable.convertColumnIndexToView(1)));
                 operatorComboBox.setSelectedItem(
                         (Operator)contactTable.getValueAt(i,contactTable.convertColumnIndexToView(2)));
                 smsTextPane.requestFocusInWindow();
             }
         }
     }
     
     /** Listener for changes in contact table model */
     private class ContactTableModelListener implements TableModelListener {
         public void tableChanged(TableModelEvent e) {
            //select new or updated row
            if (e.getType() == e.INSERT || e.getType() == e.UPDATE) {
                contactTable.clearSelection();
                contactTable.getSelectionModel().setSelectionInterval(e.getFirstRow(),e.getFirstRow());
            }
         }
     }
     
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JMenuItem aboutMenuItem;
     private javax.swing.JButton addContactButton;
     private javax.swing.JMenuItem configMenuItem;
     private javax.swing.JPanel contactPanel;
     private javax.swing.JTable contactTable;
     private javax.swing.JButton deleteButton;
     private javax.swing.JButton editButton;
     private javax.swing.JMenuItem exitMenuItem;
     private javax.swing.JLabel jLabel1;
     private javax.swing.JLabel jLabel2;
     private javax.swing.JLabel jLabel4;
     private javax.swing.JLabel jLabel5;
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JScrollPane jScrollPane2;
     private javax.swing.JScrollPane jScrollPane3;
     private javax.swing.JSeparator jSeparator1;
     private javax.swing.JPanel mainPanel;
     private javax.swing.JMenuBar menuBar;
     private javax.swing.JLabel nameLabel;
     private javax.swing.JComboBox operatorComboBox;
     private javax.swing.JButton pauseButton;
     private javax.swing.JMenu programMenu;
     private javax.swing.JPanel queuePanel;
     private javax.swing.JButton removeContactButton;
     private javax.swing.JButton sendButton;
     private javax.swing.JLabel smsCounterLabel;
     private javax.swing.JProgressBar smsDelayProgressBar;
     private javax.swing.JButton smsDownButton;
     private javax.swing.JTextField smsNumberTextField;
     private javax.swing.JPanel smsPanel;
     private javax.swing.JList smsQueueList;
     private javax.swing.JTextPane smsTextPane;
     private javax.swing.JButton smsUpButton;
     private javax.swing.JLabel statusAnimationLabel;
     private javax.swing.JLabel statusMessageLabel;
     private javax.swing.JPanel statusPanel;
     // End of variables declaration//GEN-END:variables
 }
