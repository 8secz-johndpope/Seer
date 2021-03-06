 /* JNativeHook: Global keyboard and mouse hooking for Java.
  * Copyright (C) 2006-2012 Alexander Barker.  All Rights Received.
  * 
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.jnativehook.example;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.ItemSelectable;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.ItemEvent;
 import java.awt.event.ItemListener;
 import java.awt.event.KeyEvent;
 import java.awt.event.WindowEvent;
 import java.awt.event.WindowListener;
 import javax.swing.BorderFactory;
 import javax.swing.JCheckBox;
 import javax.swing.JFrame;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTextArea;
 import javax.swing.WindowConstants;
 import javax.swing.border.EtchedBorder;
 import org.jnativehook.GlobalScreen;
 import org.jnativehook.NativeHookException;
 import org.jnativehook.NativeInputEvent;
 import org.jnativehook.keyboard.NativeKeyEvent;
 import org.jnativehook.keyboard.NativeKeyListener;
 import org.jnativehook.mouse.NativeMouseEvent;
 import org.jnativehook.mouse.NativeMouseInputListener;
 import org.jnativehook.mouse.NativeMouseWheelEvent;
 import org.jnativehook.mouse.NativeMouseWheelListener;
 
 /**
  * A demonstration of how to use the JNativeHook library.
  * 
  * @author	Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
  * @version	1.0
  * @since	1.0
  * 
  * @see GlobalScreen
  * @see NativeKeyListener
  */
 public class NativeHookDemo extends JFrame implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener, ActionListener, WindowListener, ItemListener {
 	/** The Constant serialVersionUID. */
 	private static final long serialVersionUID = -5076634313730799059L;
 	
 	/** Checkbox's for event delivery options. */
 	private JCheckBox chkKeyboard, chkButton, chkMotion, chkWheel;
 	
 	/** The text area to display event info. */
 	private JTextArea txtEventInfo;
 	
 	/**
 	 * Instantiates a new native hook demo.
 	 */
 	public NativeHookDemo() {
 		//Setup the main window.
 		setTitle("JNativeHook Demo");
 		setLayout(new BorderLayout());
 		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
 		setSize(600, 300);
 		addWindowListener(this);
 		
 		//Create options panel
 		JPanel grpOptions = new JPanel();
 		grpOptions.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
 		grpOptions.setLayout(new FlowLayout(FlowLayout.LEADING));
 		add(grpOptions, BorderLayout.NORTH);
 		
 		//Create keyboard checkbox
 		chkKeyboard = new JCheckBox("Keyboard Events");
 		chkKeyboard.setMnemonic(KeyEvent.VK_K);
 		chkKeyboard.addItemListener(this);
 		chkKeyboard.setSelected(true);
 		grpOptions.add(chkKeyboard);
 		
 		//Create button checkbox
 		chkButton = new JCheckBox("Button Events");
 		chkButton.setMnemonic(KeyEvent.VK_B);
 		chkButton.addItemListener(this);
 		chkButton.setSelected(true);
 		grpOptions.add(chkButton);
 		
 		//Create motion checkbox
 		chkMotion = new JCheckBox("Motion Events");
 		chkMotion.setMnemonic(KeyEvent.VK_M);
 		chkMotion.addItemListener(this);
 		chkMotion.setSelected(true);
 		grpOptions.add(chkMotion);
 
 		//Create wheel checkbox
 		chkWheel = new JCheckBox("Wheel Events");
 		chkWheel.setMnemonic(KeyEvent.VK_W);
 		chkWheel.addItemListener(this);
 		chkWheel.setSelected(true);
 		grpOptions.add(chkWheel);
 		
 		//Create feedback area
 		txtEventInfo = new JTextArea();
 		txtEventInfo.setEditable(false);
 		txtEventInfo.setBackground(new Color(0xFF, 0xFF, 0xFF));
 		txtEventInfo.setForeground(new Color(0x00, 0x00, 0x00));
 		txtEventInfo.setText("");
 		
 		JScrollPane scrollPane = new JScrollPane(txtEventInfo);
 		scrollPane.setPreferredSize(new Dimension(375, 125));
 		add(scrollPane, BorderLayout.CENTER);
 		
 		setVisible(true);
 	}
 	
 
 	/**
 	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
 	 */
 	public void itemStateChanged(ItemEvent e) {
 		ItemSelectable item = e.getItemSelectable();
 		
         if (item == chkKeyboard) {
         	//Keyboard checkbox was changed, adjust listeners accordingly
         	if (e.getStateChange() == ItemEvent.SELECTED) {
         		GlobalScreen.getInstance().addNativeKeyListener(this);
         	}
         	else {
         		GlobalScreen.getInstance().removeNativeKeyListener(this);
         	}
 		}
 		else if (item == chkButton) {
 			//Button checkbox was changed, adjust listeners accordingly
         	if (e.getStateChange() == ItemEvent.SELECTED) {
         		GlobalScreen.getInstance().addNativeMouseListener(this);
         	}
         	else {
         		GlobalScreen.getInstance().removeNativeMouseListener(this);
         	}
 		}
 		else if (item == chkMotion) {
 			//Motion checkbox was changed, adjust listeners accordingly
 			if (e.getStateChange() == ItemEvent.SELECTED) {
 				GlobalScreen.getInstance().addNativeMouseMotionListener(this);
 			}
 			else {
 				GlobalScreen.getInstance().removeNativeMouseMotionListener(this);
 			}
 		}
 		else if (item == chkWheel) {
 			//Motion checkbox was changed, adjust listeners accordingly
 			if (e.getStateChange() == ItemEvent.SELECTED) {
 				GlobalScreen.getInstance().addNativeMouseWheelListener(this);
 			}
 			else {
 				GlobalScreen.getInstance().removeNativeMouseWheelListener(this);
 			}
 		}
 	}
 	
 	/**
 	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
 	 */
 	public void actionPerformed(ActionEvent e) {
 		//Clear the text components.
 		txtEventInfo.setText("");
 		
 		//Return the focus to the window.
 		this.requestFocusInWindow();	
 	}
 	
 	/**
 	 * @see org.jnativehook.keyboard.NativeKeyListener#keyPressed(org.jnativehook.keyboard.NativeKeyEvent)
 	 */
 	public void keyPressed(NativeKeyEvent e) {
 		displayEventInfo(e);
 	}
 	
 	/**
 	 * @see org.jnativehook.keyboard.NativeKeyListener#keyReleased(org.jnativehook.keyboard.NativeKeyEvent)
 	 */
 	public void keyReleased(NativeKeyEvent e) {
 		displayEventInfo(e);
 	}
 	
 	/**
 	 * @see org.jnativehook.mouse.NativeMouseListener#mousePressed(org.jnativehook.mouse.NativeMouseEvent)
 	 */
 	public void mousePressed(NativeMouseEvent e) {
 		displayEventInfo(e);
 	}
 	
 	/**
 	 * @see org.jnativehook.mouse.NativeMouseListener#mouseReleased(org.jnativehook.mouse.NativeMouseEvent)
 	 */
 	public void mouseReleased(NativeMouseEvent e) {
 		displayEventInfo(e);
 	}
 	
 	/**
 	 * @see org.jnativehook.mouse.NativeMouseMotionListener#mouseMoved(org.jnativehook.mouse.NativeMouseEvent)
 	 */
 	public void mouseMoved(NativeMouseEvent e) {
 		displayEventInfo(e);
 	}
 
 	/**
 	 * @see org.jnativehook.mouse.NativeMouseMotionListener#mouseDragged(org.jnativehook.mouse.NativeMouseEvent)
 	 */
 	public void mouseDragged(NativeMouseEvent e) {
 		displayEventInfo(e);
 	}
 
 	/**
 	 * @see org.jnativehook.mouse.NativeMouseWheelListener#mouseWheelMoved(org.jnativehook.mouse.NativeMouseWheelEvent)
 	 */
 	public void mouseWheelMoved(NativeMouseWheelEvent e) {
 		displayEventInfo(e);
 	}
 	
 	/**
 	 * Write the <code>NativeInputEvent</code> to the text window.
 	 *
 	 * @param e The native key event.
 	 */
 	private void displayEventInfo(NativeInputEvent e) {
 		txtEventInfo.append("\n" + e.paramString());
 		txtEventInfo.setCaretPosition(txtEventInfo.getDocument().getLength());
 	}
 
 	/**
 	 * Unimplemented
 	 * 
 	 * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
 	 */
 	public void windowActivated(WindowEvent e) { /* Do Nothing */ }
 	
 	/**
 	 * Unimplemented
 	 *
 	 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
 	 */
 	public void windowClosing(WindowEvent e) { /* Do Nothing */ }
 	
 	/**
 	 * Unimplemented
 	 *
 	 * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
 	 */
 	public void windowDeactivated(WindowEvent e) { /* Do Nothing */ }
 	
 	/**
 	 * Unimplemented
 	 *
 	 * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
 	 */
 	public void windowDeiconified(WindowEvent e) { /* Do Nothing */ }
 	
 	/**
 	 * Unimplemented
 	 *
 	 * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
 	 */
 	public void windowIconified(WindowEvent e) { /* Do Nothing */ }
 
 	/**
 	 * Write the auto repeat rate and delay to the text window along with any 
 	 * errors that may have occurred.
 	 *
 	 * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
 	 */
 	public void windowOpened(WindowEvent e) {

 		try {
 			txtEventInfo.setText("Auto Repate Rate: " + System.getProperty("jnativehook.autoRepeatRate"));
 			txtEventInfo.append("\n" + "Auto Repate Delay: " + System.getProperty("jnativehook.autoRepeatDelay"));
 			txtEventInfo.append("\n" + "Double Click Time: " + System.getProperty("jnativehook.multiClickInterval"));
 			txtEventInfo.append("\n" + "Pointer Sensitivity: " + System.getProperty("jnativehook.pointerSensitivity"));
 			txtEventInfo.append("\n" + "Pointer Acceleration Multiplier: " + System.getProperty("jnativehook.pointerAccelerationMultiplier"));
 			txtEventInfo.append("\n" + "Pointer Acceleration Threshold: " + System.getProperty("jnativehook.pointerAccelerationThreshold"));
 
 			//Initialze native hook.  This is done on window open because the
 			//listener requires the txtEventInfo object to be constructed.
 			//Note: This function is not AWT Thread Safe!
 			GlobalScreen.getInstance().registerNativeHook();
 		}
 		catch (Exception ex) {
			txtEventInfo.append("\n" + "Error: " + ex.toString());
 		}
 		
 		txtEventInfo.setCaretPosition(txtEventInfo.getDocument().getLength());
 	}
 	
 	/**
 	 * Finalize and exit the program.
 	 * 
 	 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
 	 */
 	public void windowClosed(WindowEvent e) {
     	//Clean up the native hook.
    		try {
    			//Note: This function is not AWT Thread Safe!
 			GlobalScreen.getInstance().unregisterNativeHook();
 		}
 		catch (NativeHookException ex) {
 			ex.printStackTrace();
 		}
 
 		System.runFinalization();
 		System.exit(0);
 	}
     
 	/**
 	 * The demo project entry point.
 	 *
 	 * @param args unused.
 	 */
 	public static void main(String[] args) {
 		new NativeHookDemo();
 	}
 }
