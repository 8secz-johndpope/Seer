 package ox.stackgame.ui;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.Font;
 import java.util.*;
 
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.border.EmptyBorder;
 
 import ox.stackgame.challenge.AbstractChallenge;
 import ox.stackgame.stackmachine.IntStackValue;
 import ox.stackgame.stackmachine.StackMachine;
 import ox.stackgame.stackmachine.StackMachineListener;
 import ox.stackgame.stackmachine.StackMachineListenerAdapter;
 import ox.stackgame.stackmachine.instructions.Instruction;
 
 /**
  * Allow the user to select a challenge from a predefined list. Listen to the
  * machine during RunMode, evaluating it against the current challenge success
  * criteria and displaying either success or failure messages as appropriate.
  * Also constrains execution to the challenge's specified maxInstructions,
  * stackSize and allowedInstructions.
  * 
  * @author danfox
  * @author rgossiaux
  * 
  */
 public class ChallengeUI extends JPanel {
 
     /**
      * currentChallenge stores the active challenge. This can be null; denoting
      * the user has not yet selected a challenge.
      */
     protected AbstractChallenge currChallenge = null;
     protected JLabel descLabel;
     private StackMachine machine;
    private final StateManager stateManager;
 
     private StackMachineListener l = new StackMachineListenerAdapter() {
         public void programCounterChanged(int line) {
            if (stateManager.getActiveMode() instanceof ChallengeMode && currChallenge != null){
                 // when the machine terminates, evaluate machine against challenge's hasSucceeded() function
                 if (machine.isRunning()==false) {
                     String message = currChallenge.hasSucceeded(machine);
                     System.out.println(message=="" ? "Congratulations" : message);
                     // TODO display message
                 }
             }
         }
     };
     
     public void switchTo(int i){
         assert 0 <= i && i<ChallengeMode.challengeList.size();
         System.out.println("Switching to challenge "+i);
         this.currChallenge=ChallengeMode.challengeList.get(i);
     }
 
     public ChallengeUI(StateManager m) {
         m.stackMachine.addListener(l);
         this.machine = m.stackMachine;
        this.stateManager = m;
 
         m.registerModeActivationVisitor(modeActivationVisitor);
         m.registerModeDeactivationVisitor(modeDeactivationVisitor);
 
         this.setLayout(new FlowLayout());
         this.setBackground(Color.white);
         this.setSize(new Dimension(250, ApplicationFrame.h));
         this.setBorder(new EmptyBorder(15, 15, 15, 15));
         JLabel l = new JLabel("Challenge");
         l.setFont(new Font("Helvetica Neue", Font.BOLD, 23));
         l.setForeground(new Color(66, 66, 66));
         this.add(l);
         descLabel = new JLabel();
         this.add(descLabel);
         //this.setVisible(false);
     }
     
     private void checkMachineInstructions(StackMachine m){
         if (currChallenge != null) {
             
             System.out.println(new Instruction("const", new IntStackValue(1)).equals(new Instruction("const", new IntStackValue(1))));
             
             System.out.println("Checking machine against challenge's instructionSet");
             Map<Instruction, Integer> used = new HashMap<Instruction, Integer>();
             // go through whole program, ensure that
             for (Instruction i : m.getInstructions()) {
                 Integer numUsed = used.get(i);
                 Integer allowedInstances = currChallenge.instructionSet.get(i);
                 if(numUsed == null)
                     used.put(i, 1);
                 else
                     used.put(i,numUsed+1);
                 
                 
                 // allowedInstances = -1 denotes an infinite allowance
                 if (allowedInstances == null
                         || (allowedInstances != null
                                 && allowedInstances != -1 && numUsed > allowedInstances)) {
                     // exceeded allowed exceptions.
                     throw new IllegalArgumentException("Program doesn't conform to instructionSet: "+ i.name);
                     // TODO handle this better; perhaps give offending instruction?
                 }
             }
         }
     }
 
     private ModeVisitor modeActivationVisitor = new ModeVisitor() {
 
         /**
          * When switching to RunMode, check the program against allowedInstructions.
          */
         public void visit(RunMode m) {
            if (stateManager.getActiveMode() instanceof ChallengeMode)
                ChallengeUI.this.checkMachineInstructions(machine);
         }
 
         // this mode is activated!
         public void visit(ChallengeMode m) {
             descLabel.setText(currChallenge.description);
            ChallengeUI.this.setVisible(true);
         }
 
         public void visit(FreeDesignMode m) {
         }
     };
 
     // code to be executed when a mode is deactivated
     private ModeVisitor modeDeactivationVisitor = new ModeVisitor() {
         public void visit(RunMode m) {
         }
 
         // this mode is deactivated
         public void visit(ChallengeMode m) {
            ChallengeUI.this.setVisible(false);
         }
 
         public void visit(FreeDesignMode m) {
         }
     };
 
 }
