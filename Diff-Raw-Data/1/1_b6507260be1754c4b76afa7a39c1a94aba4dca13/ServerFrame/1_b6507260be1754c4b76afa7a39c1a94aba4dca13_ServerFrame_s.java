 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package uk.org.ownage.dmdirc.ui;
 
 import java.awt.Dimension;
 import javax.swing.JScrollBar;
 import uk.org.ownage.dmdirc.Server;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import javax.swing.border.Border;
 import javax.swing.border.EmptyBorder;
 import javax.swing.plaf.basic.BasicInternalFrameUI;
 import uk.org.ownage.dmdirc.commandparser.CommandParser;
 import uk.org.ownage.dmdirc.commandparser.CommandWindow;
 import uk.org.ownage.dmdirc.logger.ErrorLevel;
 import uk.org.ownage.dmdirc.logger.Logger;
 import uk.org.ownage.dmdirc.ui.messages.Formatter;
 import uk.org.ownage.dmdirc.ui.messages.Styliser;
 
 /**
  * The ServerFrame is the MDI window that shows server messages to the user
  * @author chris
  */
 public class ServerFrame extends javax.swing.JInternalFrame implements CommandWindow {
     
     /**
      * The border used when the frame is not maximised
      */
     private Border myborder;
     /**
      * The dimensions of the titlebar of the frame
      **/
     private Dimension titlebarSize;
     /**
      * whether to auto scroll the textarea when adding text
      */
     private boolean autoScroll = true;
     /**
      * holds the scrollbar for the frame
      */
     private JScrollBar scrollBar;
     
     private CommandParser commandParser;
     
     /**
      * Creates a new ServerFrame
      * @param commandParser The command parser to use
      */
     public ServerFrame(CommandParser commandParser) {
         initComponents();
         
         setFrameIcon(MainFrame.getMainFrame().getIcon());
         
         setMaximizable(true);
         setClosable(true);
         setVisible(true);
         setResizable(true);
         
         scrollBar = jScrollPane1.getVerticalScrollBar();
         
         this.commandParser = commandParser;
         
         jTextField1.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent actionEvent) {
                 try {
                     ServerFrame.this.commandParser.parseCommand(ServerFrame.this, jTextField1.getText());
                 } catch (Exception e) {
                     Logger.error(ErrorLevel.ERROR, e);
                 }
                 jTextField1.setText("");
             }
         });
         
         addPropertyChangeListener("maximum", new PropertyChangeListener() {
             public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                 if (propertyChangeEvent.getNewValue().equals(Boolean.TRUE)) {
                     ServerFrame.this.myborder = getBorder();
                     ServerFrame.this.titlebarSize =
                             ((BasicInternalFrameUI)getUI())
                             .getNorthPane().getPreferredSize();
                     
                     ((BasicInternalFrameUI)getUI()).getNorthPane()
                     .setPreferredSize(new Dimension(0,0));
                     setBorder(new EmptyBorder(0,0,0,0));
                     
                     MainFrame.getMainFrame().setMaximised(true);
                 } else {
                     autoScroll = ((scrollBar.getValue() + scrollBar.getVisibleAmount())
                     != scrollBar.getMaximum());
                     if(autoScroll) {
                         jTextPane1.setCaretPosition(jTextPane1.getStyledDocument().getLength());
                     }
                     
                     setBorder(ServerFrame.this.myborder);
                     ((BasicInternalFrameUI)getUI()).getNorthPane()
                     .setPreferredSize(ServerFrame.this.titlebarSize);
                     
                     MainFrame.getMainFrame().setMaximised(false);
                 }
             }
         });
         
     }
     
     /**
      * Adds a line of text to the main text area, and scrolls the text pane
      * down so that it's visible if the scrollbar is already at the bottom
      * @param line text to add
      */
     public void addLine(String line) {
         String ts = Formatter.formatMessage("timestamp", new Date());
         Styliser.addStyledString(jTextPane1.getStyledDocument(), ts+line+"\n");
         
         autoScroll = ((scrollBar.getValue() + scrollBar.getVisibleAmount())
         != scrollBar.getMaximum());
         if(autoScroll) {
             jTextPane1.setCaretPosition(jTextPane1.getStyledDocument().getLength());
         }
     }
     
     /**
      * Formats the arguments using the Formatter, then adds the result to the
      * main text area
      * @param messageType The type of this message
      * @param args The arguments for the message
      */
     public void addLine(String messageType, Object... args) {
         addLine(Formatter.formatMessage(messageType, args));
     }    
     
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
     private void initComponents() {
         jTextField1 = new javax.swing.JTextField();
         jScrollPane1 = new javax.swing.JScrollPane();
         jTextPane1 = new javax.swing.JTextPane();
 
         setTitle("Server Frame");
 
         jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
         jTextPane1.setEditable(false);
         jScrollPane1.setViewportView(jTextPane1);
 
         org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
         getContentPane().setLayout(layout);
         layout.setHorizontalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
         );
         layout.setVerticalGroup(
             layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
             .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                 .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                 .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                 .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
         );
         pack();
     }// </editor-fold>//GEN-END:initComponents
     
     
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JScrollPane jScrollPane1;
     private javax.swing.JTextField jTextField1;
     private javax.swing.JTextPane jTextPane1;
     // End of variables declaration//GEN-END:variables
     
 }
