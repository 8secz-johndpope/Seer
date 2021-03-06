 package ox.stackgame.ui;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 
 import javax.swing.JButton;
 import javax.swing.JPanel;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 
 import ox.stackgame.stackmachine.StackMachineListenerAdapter;
 import ox.stackgame.stackmachine.exceptions.NotHaltingException;
 import ox.stackgame.stackmachine.exceptions.StackRuntimeException;
 
 @SuppressWarnings("serial")
 public class ButtonUI extends JPanel {
 
     private final StateManager sm;
     private Mode oldMode = null;
     private final RunMode runMode;
     @SuppressWarnings("unused") // tui actually is used further down.
     private final ProgramTextUI tui;
 
     private final JButton lexButton = new JButton("Check");
     private final JButton step1Button = new JButton("Step");
     private final JButton stepAllButton = new JButton("Step All");
     private final JButton runAllButton = new JButton("Run All");
     private final JButton pauseButton = new JButton("Pause");
     private final JButton resetButton = new JButton("Reset");
 
     public void updateButtons() {
         updateButtons(false);
     }
     
     public void updateButtons(boolean textDirty) {
         boolean td = textDirty;
         boolean rm = sm.getActiveMode() == runMode;
         lexButton.setEnabled(td);
         step1Button.setEnabled((rm || !td) && runMode.machine.isRunning());
         if (rm && runMode.timerRunning()) {
             stepAllButton.setText("Pause");
             stepAllButton.setEnabled(true);
         } else if ((rm || !td) && runMode.machine.isRunning()) {
             stepAllButton.setText("Step All");
             stepAllButton.setEnabled(true);
         } else {
             stepAllButton.setText("Step All");
             stepAllButton.setEnabled(false);
         }
 
         runAllButton.setEnabled((rm || !td) && runMode.machine.isRunning() && !runMode.timerRunning());
         resetButton.setEnabled(rm && !runMode.timerRunning());
 
         // eui.clearErrors();
     }
 
     public ButtonUI(final StateManager sm, final ProgramTextUI tui, final RunMode runMode, final ErrorUI eui) {
 
         this.runMode = runMode;
         this.sm = sm;
         this.tui = tui;
 
         this.setSize(new Dimension(80, 300));
         this.setBackground(ApplicationFrame.caBlue);
 
         // create lex button
         lexButton.setPreferredSize(new Dimension(ApplicationFrame.CENTER_PANEL_WIDTH / 6, ApplicationFrame.BUTTONUI_HEIGHT - 5));
         this.add(lexButton);
 
         // create step1 button
         step1Button.setForeground(new Color(0, 133, 200));
         step1Button.setPreferredSize(new Dimension(ApplicationFrame.CENTER_PANEL_WIDTH / 6, ApplicationFrame.BUTTONUI_HEIGHT - 5));
         this.add(step1Button);
 
         // create stepAll Button
         // allows the user to see each command animated until the machine halts.
 
         stepAllButton.setForeground(new Color(0, 133, 200));
         stepAllButton.setPreferredSize(new Dimension(ApplicationFrame.CENTER_PANEL_WIDTH / 6 + 5, ApplicationFrame.BUTTONUI_HEIGHT - 5));
         this.add(stepAllButton);
 
         // create runAll button
         runAllButton.setForeground(new Color(0, 133, 200));
         runAllButton.setPreferredSize(new Dimension(ApplicationFrame.CENTER_PANEL_WIDTH / 6, ApplicationFrame.BUTTONUI_HEIGHT - 5));
         this.add(runAllButton);
 
         // pauseButton
         pauseButton.setForeground(new Color(0, 133, 200));
         // this.add(pauseButton);
 
         // create reset button
         resetButton.setPreferredSize(new Dimension(ApplicationFrame.CENTER_PANEL_WIDTH / 6, ApplicationFrame.BUTTONUI_HEIGHT - 5));
         this.add(resetButton);
 
         // initialise the buttons
         updateButtons();
 
         // button logic
         tui.document.addDocumentListener(new DocumentListener() {
             public void changedUpdate(DocumentEvent arg0) {
                 updateButtons(true);
             }
 
             public void insertUpdate(DocumentEvent arg0) {
                 updateButtons(true);
             }
 
             public void removeUpdate(DocumentEvent arg0) {
                 updateButtons(true);
             }
         });
 
         lexButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // feed text through lexer
                 // update modeManager.stackMachine
                 System.out.println("Lexed Text input");
                 sm.stackMachine.loadInstructions(tui.getProgram());
                 updateButtons();
             }
         });
         step1Button.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // when not in runMode already, feed the text through the lexer,
                 // switch to RunMode (storing the current mode), pc:=0, call
                 // 'step'
                 if (sm.getActiveMode() != runMode) {
                     oldMode = sm.getActiveMode();
                     // switch to RunMode
                     sm.setActiveMode(runMode);
                 }
                 // call step
                 try {
                     sm.stackMachine.step();
                 } catch (StackRuntimeException e) {
                     eui.displayError(e.getMessage());
                 }
 
                 updateButtons();
             }
         });
 
         runAllButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // for a dirty textarea, lex the text, switch to RunMode
                 // (storing the current mode), call runAll on the stackMachine.
                 // when clicked in RunMode, call runAll
                 // disable this button
 
                 if (sm.getActiveMode() != runMode) {
                     oldMode = sm.getActiveMode();
                     // switch to RunMode
                     sm.setActiveMode(runMode);
                 }
                 try {
                     sm.stackMachine.runAll();
                 } catch (StackRuntimeException e) {
                     eui.displayError(e.getMessage());
                     //e.printStackTrace();
                 } catch (NotHaltingException e) {
                     eui.displayError(e.getMessage());
                     //e.printStackTrace();
                 }
 
                 updateButtons();
             }
         });
 
         pauseButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // disabled in designMode.
                 // enabled if machine is being animated
                 // when clicked, stop the stepAll timer from calling step
                 // enable stepAllButton, enable runAllButton
 
                 assert (sm.getActiveMode() == runMode);
 
                 runMode.pause();
                 updateButtons();
             }
         });
 
         stepAllButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // for a dirty textarea, lex the text, switch to RunMode
                 // (storing the current mode), create a timer, call step on the
                 // timer until machine terminates.
                 // when clicked in runMode, just do the timer stuff
                 // disable this button
                 // enable pausebutton
                 
                 // WARNING: If you read this method, you WILL vomit on your own face.
                 // Feel free to change this (Please God change it)
 
                 if (stepAllButton.getText() == "Pause") {
                     runMode.pause();
                     updateButtons();
                 } else {
                     if (sm.getActiveMode() != runMode) {
                         oldMode = sm.getActiveMode();
                         // switch to RunMode
                         sm.setActiveMode(runMode);
                     }
 
                     runMode.run(new ActionListener() {
                         public void actionPerformed(ActionEvent e) {
                             updateButtons(); // not being called when this
                                              // finishes.
                         }
                     });
                 }
             }
         });
 
         resetButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent arg0) {
                 // (should not be enabled in DesignMode)
                 // switch back to old mode, call reset on the stackmachine
                 sm.setActiveMode(oldMode);
                 oldMode = null;
                 sm.stackMachine.reset();
                 // resetButton.setEnabled(false);
                 //
                 // lexButton.setEnabled(true);
                 updateButtons();
                
                eui.clearErrors();
             }
         });
 
         // do button enabling/disabling
         sm.stackMachine.addListener(new StackMachineListenerAdapter() {
             public void programCounterChanged(int line) {
                 updateButtons();
             }
         });
 
     }
 }
