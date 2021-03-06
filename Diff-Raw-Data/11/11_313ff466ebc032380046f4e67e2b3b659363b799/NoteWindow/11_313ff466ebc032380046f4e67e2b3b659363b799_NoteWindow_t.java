 /*
  * pin 'em up
  * 
  * Copyright (C) 2007 by Mario Koedding
  *
  *
  * pin 'em up is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * pin 'em up is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with pin 'em up; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *
  */
 
 package gui;
 
 import logic.*;
 import javax.swing.*;
 import java.awt.event.*;
 import java.awt.*;
 
 public class NoteWindow extends JDialog implements FocusListener, WindowListener, ActionListener, MouseListener, MouseMotionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
 
    private JScrollPane textPanel;
 
    private JPanel topPanel, mainPanel;
 
    private JTextArea textArea;
 
    private Note parentNote;
 
    private JButton closeButton;
 
    private int dx, dy;
 
    private boolean dragging; // required to make the window movable
 
    private boolean resizeCursor, resizing; // required to make window resizable
    
    private JLabel bgLabel;
    
 
    public NoteWindow(Note pn) {
      super(new JFrame());
       parentNote = pn;
       textPanel = new JScrollPane();
       textPanel.setOpaque(false);
       mainPanel = new JPanel(new BorderLayout());
       mainPanel.setOpaque(false);
       topPanel = new JPanel(new BorderLayout());
       topPanel.setOpaque(false);
       mainPanel.add(textPanel, BorderLayout.CENTER);
       mainPanel.add(topPanel, BorderLayout.NORTH);
       dragging = false;
 
       // create and adjust TextArea
       textArea = new JTextArea(parentNote.getText(), 1, 1);
       textArea.setOpaque(false);
       textArea.setLineWrap(true);
       textArea.setWrapStyleWord(true);
       textArea.setFont(new java.awt.Font("SERIF", 1, parentNote.getFontSize()));
       textArea.addFocusListener(this);
       textArea.setMargin(new Insets(0, 3, 3, 3));
       textPanel.setViewportView(textArea);
       textPanel.getViewport().setOpaque(false);      
       textPanel.setBorder(null);
       mainPanel.add(textPanel, BorderLayout.CENTER);
       
       // adjust and add buttons to the topPanel
       topPanel.addMouseListener(this);
       topPanel.addMouseMotionListener(this);
       topPanel.addFocusListener(this);
       Image img = ResourceLoader.loadImage("resources", "closeicon.gif");
       ImageIcon closeIcon = null;
       closeIcon = new ImageIcon(img);
       closeButton = new JButton(closeIcon);
       closeButton.setBackground(new Color(255,255,255,0));
       
       closeButton.setToolTipText("hide note");
       closeButton.addActionListener(this);
       closeButton.addMouseListener(this);
       closeButton.setFocusable(false);
       closeButton.setBorderPainted(false);
       closeButton.setHorizontalAlignment(SwingConstants.CENTER);
       closeButton.setPreferredSize(new java.awt.Dimension(20, 20));
       closeButton.setMargin(new Insets(4, 0, 0, 3));
       topPanel.add(closeButton, BorderLayout.EAST);
       updateToolTip();
       
       setUndecorated(true);
       setLocation(parentNote.getXPos(),parentNote.getYPos());
       setSize(parentNote.getXSize(),parentNote.getYSize());
 
       // menu and doubleclick
       topPanel.addMouseListener(this);
 
       // rezeize listener
       resizing = false;
       resizeCursor = false;
       textArea.addMouseListener(this);
       textArea.addMouseMotionListener(this);
       
       setAlwaysOnTop(parentNote.isAlwaysOnTop());
       setContentPane(mainPanel);
       setDefaultCloseOperation(DISPOSE_ON_CLOSE);
       addWindowListener(this);
       
       bgLabel = new BackgroundLabel(this);
      
       getLayeredPane().add(bgLabel, new Integer(Integer.MIN_VALUE));
       setVisible(true);
    }
 
    public void focusGained(FocusEvent arg0) {
       // do nothing
    }
 
    public void focusLost(FocusEvent arg0) {
       parentNote.setText(textArea.getText());
       parentNote.setPosition((short)getX(), (short)getY());
       parentNote.setSize((short)getWidth(), (short)getHeight());
       
       // write notes to file after every change
       NoteIO.writeNotesToFile(PinEmUp.getMainApp().getNotes(), PinEmUp.getUserSettings().getNotesFile());
    }
 
    public void windowActivated(WindowEvent arg0) {
       // do nothing
    }
 
    public void windowClosed(WindowEvent arg0) {
       parentNote.setWindow(null);
    }
 
    public void windowClosing(WindowEvent arg0) {
       // do nothing
    }
 
    public void windowDeactivated(WindowEvent arg0) {
       // do nothing
    }
 
    public void windowDeiconified(WindowEvent arg0) {
       // do nothing
    }
 
    public void windowIconified(WindowEvent arg0) {
       // do nothing
    }
 
    public void windowOpened(WindowEvent arg0) {
       // do nothing
    }
 
    public void actionPerformed(ActionEvent e) {
       if (e.getSource() == closeButton) {
          parentNote.hide();
          
          // write notes to file after every change
          NoteIO.writeNotesToFile(PinEmUp.getMainApp().getNotes(), PinEmUp.getUserSettings().getNotesFile());
       }
 
    }
 
    public void mouseClicked(MouseEvent e) {
       if (e.getSource() == topPanel && e.getClickCount() == 2) { // doubleclick on topPanel
          autoSizeY();
       }
    }
    
    private void autoSizeY() {
       setSize(getWidth(),30);
       new Thread(new Runnable() {
          public void run() {
             try {
                Thread.sleep(5); // must wait for new settings (size) to be applied
             } catch (Exception e) {
                // do nothing
             }
             int sizeX = getWidth();
             int sizeY = textArea.getHeight();
             setSize(sizeX,sizeY+20);
          }
       }).start();
    }
 
    public void mouseEntered(MouseEvent e) {
       // do nothing
    }
 
    public void mouseExited(MouseEvent e) {
       // do nothing
 
    }
 
    public void mousePressed(MouseEvent e) {
       checkPopupMenu(e);
       if (e.getSource() == topPanel && e.getButton() == MouseEvent.BUTTON1) {
          // Position on Panel
          dx = e.getXOnScreen() - getX();
          dy = e.getYOnScreen() - getY();
 
          dragging = true;
       }
 
       else if (e.getButton() == MouseEvent.BUTTON1 && resizeCursor
             && e.getSource() == textArea) {
          dx = getX() + getWidth() - e.getXOnScreen();
          dy = getY() + getHeight() - e.getYOnScreen();
          resizing = true;
       }
 
    }
 
    public void mouseReleased(MouseEvent e) {
       checkPopupMenu(e);
 
       if (dragging && e.getButton() == MouseEvent.BUTTON1) {
          dragging = false;
       } else if (resizing && e.getButton() == MouseEvent.BUTTON1) {
          resizing = false;
       } else if (e.getSource() == closeButton) {
          // restore button backgorund if not pressed
          repaint();
       }
    }
 
    private void checkPopupMenu(MouseEvent event) {
       if (event.isPopupTrigger()) {
          RightClickMenu popup = new RightClickMenu(this);
          popup.show(event.getComponent(), event.getX(), event.getY());
       }
    }
 
    public Note getParentNote() {
       return parentNote;
    }
 
    public void mouseDragged(MouseEvent e) {
       if (dragging) {
          setLocation(e.getXOnScreen() - dx, e.getYOnScreen() - dy);
       } else if (resizing) {
          setSize(e.getXOnScreen() - getX() + dx, e.getYOnScreen() - getY() + dy);
       }
    }
    
    public void updateToolTip() {
       topPanel.setToolTipText("Category: " + (parentNote.getCategory()+1) + " " + PinEmUp.getUserSettings().getCategoryNames()[parentNote.getCategory()]);
    }
 
    public void mouseMoved(MouseEvent e) {
       if (e.getSource() == textArea && !resizing) {
          // if in lower right corner, start resizing or change cursor
          if (e.getX() >= textArea.getWidth() - 10 && e.getY() >= textPanel.getHeight() - 10) { // height from panel because of vertical scrolling
             if (!resizeCursor) {
                resizeCursor = true;
                textArea.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
             }
          } else {
             if (resizeCursor) {
                resizeCursor = false;
                textArea.setCursor(new Cursor(Cursor.TEXT_CURSOR));
             }
          }
       }
    }
    
    public void refreshView() {
       textArea.setFont(new java.awt.Font("SERIF", 1, parentNote.getFontSize()));
    }
 }
