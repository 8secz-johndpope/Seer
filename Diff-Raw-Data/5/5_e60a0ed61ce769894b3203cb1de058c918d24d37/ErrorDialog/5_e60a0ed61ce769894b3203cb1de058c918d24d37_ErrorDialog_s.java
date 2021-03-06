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
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.Insets;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.WindowEvent;
 import java.awt.event.WindowListener;
 import java.io.BufferedReader;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.UnsupportedEncodingException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLConnection;
 import java.net.URLEncoder;
 
 import javax.swing.Box;
 import javax.swing.Icon;
 import javax.swing.JButton;
 import javax.swing.JCheckBox;
 import javax.swing.JDialog;
 import javax.swing.JLabel;
 import javax.swing.JScrollPane;
 import javax.swing.JTextArea;
 import javax.swing.WindowConstants;
 
 import uk.org.ownage.dmdirc.logger.ErrorLevel;
 
 import uk.org.ownage.dmdirc.ui.interfaces.StatusErrorNotifier;
 
 /**
  * The fatal error dialog is used to inform the user that a fatal error has
  * occured.
  * @author  chris
  */
 public final class ErrorDialog extends JDialog implements ActionListener,
 	StatusErrorNotifier, WindowListener {
     
     /**
      * A version number for this class. It should be changed whenever the class
      * structure is changed (or anything else that would prevent serialized
      * objects being unserialized with the new class).
      */
     private static final long serialVersionUID = 1;
     
     /** Size of the large borders in the dialog. */
     private static final int LARGE_BORDER = 10;
     
     /** Size of the small borders in the dialog. */
     private static final int SMALL_BORDER = 5;
     
     /** error level. */
     private ErrorLevel level;
     
     /** icon. */
     private Icon icon;
     
     /** message. */
     private String message;
     
     /** trace. */
     private String[] trace;
     
     /** button. */
     private JButton okButton;
     
     /** label. */
     private JLabel infoLabel;
     
     /** label. */
     private JLabel messageLabel;
     
     /** label. */
     private JButton showMore;
     
     /** checkbox. */
     private JCheckBox sendData;
     
     /** Scroll pane. */
     private JScrollPane scrollPane;
     
     /** stack trace. */
     private JTextArea stacktraceField;
     
     /**
      * Creates a new fatal error dialog.
      * @param parent The parent frame
      * @param modal Whether this dialog is modal or not
      * @param message The message (error info) to be displayed
      */
     public ErrorDialog(final ErrorLevel newLevel, final Icon newIcon,
 	    final String newMessage, final String[] newTrace) {
 	super(MainFrame.getMainFrame(), (newLevel == ErrorLevel.FATAL ? true: false));
 	icon = newIcon;
 	level = newLevel;
 	message = newMessage;
 	trace = new String[newTrace.length];
 	System.arraycopy(newTrace, 0, trace, 0, newTrace.length);
 	initComponents();
 	layoutComponents();
 	setLocationRelativeTo(MainFrame.getMainFrame());
     }
     
     /**
      * Initialises the components for this dialog.
      */
     private void initComponents() {
 	messageLabel = new JLabel();
 	infoLabel = new JLabel();
 	showMore = new JButton();
 	sendData = new JCheckBox();
 	scrollPane = new JScrollPane();
 	stacktraceField = new JTextArea();
 	okButton = new JButton();
 	
 	setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
 	this.addWindowListener(this);
 	setTitle("DMDirc - an error occured");
 	
 	infoLabel.setText("DMDirc has encountered " + level.toSentenceString() + ".");
 	infoLabel.setIcon(icon);
 	
 	messageLabel.setText("<html>Message: <br>" + message + "</html>");
 	
 	showMore.setText("Show trace");
 	showMore.addActionListener(this);
 	
 	stacktraceField.setColumns(20);
 	stacktraceField.setEditable(false);
 	stacktraceField.setRows(5);
 	
 	if (trace.length > 0) {
 	    for (String line : trace) {
 		stacktraceField.append(line + "\n");
 	    }
 	    stacktraceField.setCaretPosition(0);
 	}
 	
 	scrollPane.setViewportView(stacktraceField);
 	scrollPane.setMinimumSize(new Dimension(600, 200));
 	scrollPane.setPreferredSize(new Dimension(600, 200));
 	scrollPane.setVisible(false);
 	
 	sendData.setText("Send error report to developers");
 	sendData.setSelected(true);
 	
 	okButton.setText("OK");
 	okButton.setPreferredSize(new Dimension(100, 25));
 	okButton.addActionListener(this);
     }
     
     /**
      * lays the components out in the dialog
      */
     private void layoutComponents() {
 	final GridBagConstraints constraints = new GridBagConstraints();
 	
 	getContentPane().setLayout(new GridBagLayout());
 	
 	constraints.gridx = 0;
 	constraints.gridy = 0;
 	constraints.gridwidth = 3;
 	constraints.weightx = 1.0;
 	constraints.weighty = 0.0;
 	constraints.fill = GridBagConstraints.HORIZONTAL;
 	constraints.insets = new Insets(LARGE_BORDER, LARGE_BORDER,
 		LARGE_BORDER, LARGE_BORDER);
 	getContentPane().add(infoLabel, constraints);
 	
 	constraints.insets = new Insets(0, LARGE_BORDER, SMALL_BORDER,
 		LARGE_BORDER);
 	constraints.gridy = 1;
 	getContentPane().add(messageLabel, constraints);
 	
 	constraints.insets = new Insets(0, LARGE_BORDER, SMALL_BORDER,
 		LARGE_BORDER);
 	constraints.gridx = 0;
 	constraints.gridy = 2;
 	constraints.gridwidth = 3;
 	getContentPane().add(showMore, constraints);
 	
 	constraints.insets = new Insets(0, LARGE_BORDER, 0, LARGE_BORDER);
 	constraints.gridx = 0;
 	constraints.gridwidth = 3;
 	constraints.weightx = 1.0;
 	constraints.weighty = 1.0;
 	constraints.gridy = 3;
 	constraints.fill = GridBagConstraints.BOTH;
 	getContentPane().add(scrollPane, constraints);
 	
 	constraints.insets = new Insets(0, LARGE_BORDER, SMALL_BORDER,
 		LARGE_BORDER);
 	constraints.gridx = 0;
 	constraints.gridy = 4;
 	constraints.gridwidth = 3;
 	getContentPane().add(sendData, constraints);
 	
 	constraints.weightx = 1.0;
 	constraints.weighty = 0.0;
 	constraints.gridx = 0;
 	constraints.gridy = 5;
 	constraints.gridwidth = 1;
 	constraints.fill = GridBagConstraints.HORIZONTAL;
 	getContentPane().add(Box.createHorizontalGlue(), constraints);
 	
 	constraints.weightx = 0.0;
 	constraints.insets.set(LARGE_BORDER, LARGE_BORDER, LARGE_BORDER,
 		LARGE_BORDER);
 	constraints.gridx = 2;
 	constraints.anchor = GridBagConstraints.EAST;
 	constraints.fill = GridBagConstraints.NONE;
 	getContentPane().add(okButton, constraints);
 	pack();
     }
     
     /**
      * Exits the program. {@inheritDoc}
      */
     public void actionPerformed(final ActionEvent actionEvent) {
 	if (actionEvent.getSource() == showMore) {
 	    if (showMore.getText().equals("Show trace")) {
 		scrollPane.setVisible(true);
 		showMore.setText("Hide trace");
 		this.pack();
 	    } else {
 		scrollPane.setVisible(false);
 		showMore.setText("Show trace");
 		this.pack();
 	    }
 	} else {
 	    if (sendData.isSelected()) {
 		sendData();
 	    }
	    this.setVisible(false);
 	    if (level == ErrorLevel.FATAL) {
 		System.exit(-1);
 	    }
 	}
     }
     
     /** Called when the user clicks on the status notifier. */
     public void clickReceived() {
 	if (level != ErrorLevel.FATAL) {
 	    MainFrame.getMainFrame().getStatusBar().clearError();
 	}
 	this.setVisible(true);
     }
     
     /**
      * Sends an error report.
      */
     private void sendData() {
 	/*URL url;
 	URLConnection urlConn;
 	DataOutputStream printout;
 	BufferedReader input;
 	String line;
 	try {
 	    url = new URL("http://www.dmdirc.com/error.php");
 	    urlConn = url.openConnection();
 	    urlConn.setDoInput(true);
 	    urlConn.setDoOutput(true);
 	    urlConn.setUseCaches(false);
 	    urlConn.setRequestProperty
 		    ("Content-Type", "application/x-www-form-urlencoded");
 	    printout = new DataOutputStream(urlConn.getOutputStream());
 	    String content =
 		    "message=" + URLEncoder.encode(messageLabel.getText(), "UTF-8") +
 		    "&trace=" + URLEncoder.encode(stacktraceField.getText(), "UTF-8");
 	    printout.writeBytes(content);
 	    printout.flush();
 	    printout.close();
 	    input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
 	    while ((line = input.readLine()) != null) {
 		System.out.println(line);
 	    }
 	    input.close();
 	} catch (MalformedURLException ex) {
 	    System.err.println("Malformed URL, unable to send error report.");
 	} catch (UnsupportedEncodingException ex) {
 	    System.err.println("Unsupported exception,  unable to send error report.");
 	} catch (IOException ex) {
 	    System.err.println("IO Error, unable to send error report.");
 	}*/
     }
     
     /** {@inheritDoc} */
     public void windowOpened(WindowEvent e) {
     }
     
     /** {@inheritDoc} */
     public void windowClosing(WindowEvent e) {
 	if (level == ErrorLevel.FATAL) {
 	    System.exit(-1);
 	}
     }
     
     /** {@inheritDoc} */
     public void windowClosed(WindowEvent e) {
     }
     
     /** {@inheritDoc} */
     public void windowIconified(WindowEvent e) {
     }
     
     /** {@inheritDoc} */
     public void windowDeiconified(WindowEvent e) {
     }
     
     /** {@inheritDoc} */
     public void windowActivated(WindowEvent e) {
     }
     
     /** {@inheritDoc} */
     public void windowDeactivated(WindowEvent e) {
     }
 }
