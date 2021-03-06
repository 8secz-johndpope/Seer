 package eu.semaine.gui.monitor;
 
 import java.awt.Frame;
 
 import javax.swing.JDialog;
 import javax.swing.JScrollPane;
 import javax.swing.JTextPane;
 
public class InfoDialog extends JDialog
 {
 	JTextPane textPane;
 	
 	public InfoDialog(String title)
 	{
		super((Frame)null, title);
 		setSize(400, 400);
         java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
         setLocation((screenSize.width-getWidth())/2,(screenSize.height-getHeight())/2);
 		textPane = new JTextPane();
 		textPane.setEditable(false);
 		getContentPane().add(new JScrollPane(textPane));
 	}
 
 	public void setText(String text)
 	{
 		textPane.setText(text);
		textPane.setCaretPosition(text.length());
 	}
 }
