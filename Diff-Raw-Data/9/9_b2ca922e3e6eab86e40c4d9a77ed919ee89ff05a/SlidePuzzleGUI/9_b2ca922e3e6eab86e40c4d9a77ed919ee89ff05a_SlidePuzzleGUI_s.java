 package view;
 
 
 import java.awt.*;
 import model.Puzzle;
 import javax.swing.*;
 import java.awt.event.*;
 import controller.StatusAction;
 import controller.StartAction;
 import javax.swing.JSlider;
 import controller.TimerAction;
 
 
 
 
 public class SlidePuzzleGUI extends JPanel {
 
     private GraphicsPanel    _puzzleGraphics;
     private Puzzle puzzle;
     private StatusAction nga;
     private StartAction ngl;
     private TimerAction timer;
     /** Creates new form NewJPanel */
 
     public SlidePuzzleGUI(Puzzle p, StatusAction gma, StartAction ngl, TimerAction timer) {
         this.puzzle = p;
         this.nga = gma;
         this.ngl = ngl;
         this.timer = timer;
 
         //--- Create a button.  Add a listener to it.
         JButton statusButton = new JButton("Play/Pause");
         JButton newGame = new JButton("Reiniciar");
         newGame.addActionListener(this.ngl);
         statusButton.addActionListener(this.nga);
 
         JLabel label = new JLabel("10");
         JSlider slider = new JSlider (JSlider.HORIZONTAL, 0, 10, 5);
         timer.setTimerAtr(slider, label);
         slider.addChangeListener(timer);
 
 
         //--- Create control panel
         JPanel controlPanel = new JPanel();
         controlPanel.setLayout(new FlowLayout());
         controlPanel.add(statusButton, java.awt.BorderLayout.NORTH);
         controlPanel.add(newGame, java.awt.BorderLayout.NORTH);
         controlPanel.add(slider, java.awt.BorderLayout.CENTER);
         controlPanel.add(label, java.awt.BorderLayout.SOUTH);
 
 
         //--- Create graphics panel
         _puzzleGraphics = new GraphicsPanel(p);
 
         //--- Set the layout and add the components
         this.setLayout(new BorderLayout());
         this.add(controlPanel, BorderLayout.NORTH);
         this.add(_puzzleGraphics, BorderLayout.CENTER);
     }//end constructor
     
      public void mousePressed(MouseEvent e) {
             this.repaint();  // Show any updates to model.
         }//end mousePressed
 
      public void setPuzzle(Puzzle p){
          this.puzzle = p;
          _puzzleGraphics.repaint();
      }
 
 
     
     //////////////////////////////////////////////// class GraphicsPanel
     // This is defined inside the outer class so that
     // it can use the outer class instance variables.
   public class GraphicsPanel extends JPanel implements MouseListener {
 
         private static final int CELL_SIZE = 100; // Pixels
         private Font _biggerFont;
 
 
         //================================================== constructor
         public GraphicsPanel(Puzzle p) {
             puzzle = p;
             _biggerFont = new Font("Helvetica", Font.BOLD, CELL_SIZE/4);
             this.setPreferredSize(new Dimension(CELL_SIZE * p.getCol(), CELL_SIZE*p.getRow()));
             this.setBackground(Color.white);
             this.addMouseListener(this);  // Listen own mouse events.
         }//end constructor
 
 
         //=======================================x method paintComponent
         @Override
         public void paintComponent(Graphics g) {
             super.paintComponent(g);
             for (int r=0; r<puzzle.getRow(); r++) {
                 for (int c=0; c<puzzle.getCol(); c++) {
                     int x = c * CELL_SIZE;
                     int y = r * CELL_SIZE;
                     char text = puzzle.toString().charAt(r * puzzle.getCol() + c);
                     if (true) {
                         g.setColor(new Color(51, 102, 255));
                         g.fillRect(x+70, y+2, CELL_SIZE-4, CELL_SIZE-4);
                         g.setColor(new Color(244, 244, 251));
                         g.setFont(_biggerFont);
                         g.drawString("" + text, x+105, y-15+(3*CELL_SIZE)/4);
                     }
                 }
             }
         }//end paintComponent
 
 
         //========================================== ignore these events
         public void mouseClicked (MouseEvent e) {}
         public void mouseReleased(MouseEvent e) {}
         public void mouseEntered (MouseEvent e) {}
         public void mouseExited  (MouseEvent e) {}
         public void mousePressed(MouseEvent e) {}
 
 }//end GraphicGUI
 
 
     ////////////////////////////////////////// inner class StatusAction
   
 
 }//end class SlidePuzzleGUI
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
 
 
     /*
      *
     @SuppressWarnings("unchecked")
 
     // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
     private void initComponents() {
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
         this.setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(0, 400, Short.MAX_VALUE)
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(0, 300, Short.MAX_VALUE)
         );
     }// </editor-fold>//GEN-END:initComponents
 
 */
     // Variables declaration - do not modify//GEN-BEGIN:variables
     // End of variables declaration//GEN-END:variables
 
 
