 package com.mojang.mojam;
 
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyEvent;
 import java.awt.event.KeyListener;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.PipedInputStream;
 import java.io.PipedOutputStream;
 import java.io.PrintStream;
 import java.util.ArrayList;
 
 import javax.script.ScriptException;
 import javax.swing.JFrame;
 import javax.swing.JOptionPane;
 import javax.swing.JScrollPane;
 import javax.swing.JSplitPane;
 import javax.swing.JTextArea;
 import javax.swing.JTextField;
 import javax.swing.Timer;
 
 import com.mojang.mojam.Keys.Key;
 
 public class Console implements Runnable, KeyListener
 {
 	JTextArea displayPane;
 	BufferedReader reader;
 	ArrayList<String> commands = new ArrayList<String>();
 	String command = "";
 	public static JTextArea textArea = new JTextArea();
 	public static JScrollPane scrollPane = new JScrollPane(textArea);
 	public static JTextField textField = new JTextField();
 	static int scroll = 0;
 
 	Console()
 	{
 
 	}
 
 	private Console(JTextArea displayPane, PipedOutputStream pos)
 	{
 		this.displayPane = displayPane;
 
 		try
 		{
 			PipedInputStream pis = new PipedInputStream(pos);
 			reader = new BufferedReader(new InputStreamReader(pis));
 		}
 		catch (IOException e)
 		{
 		}
 	}
 
 	public void run()
 	{
 		String line = null;
 
 		try
 		{
 			while((line = reader.readLine()) != null)
 			{
 				//              displayPane.replaceSelection( line + "\n" );
 				displayPane.append(line + "\n");
 				displayPane.setCaretPosition(displayPane.getDocument().getLength());
 			}
 
 			System.err.println("im here");
 		}
 		catch (IOException ioe)
 		{
 			JOptionPane.showMessageDialog(null, "Error redirecting output : " + ioe.getMessage());
 		}
 	}
 
 	public static void redirectOutput(JTextArea displayPane)
 	{
 		Console.redirectOut(displayPane);
 		Console.redirectErr(displayPane);
 	}
 
 	public static void redirectOut(JTextArea displayPane)
 	{
 		PipedOutputStream pos = new PipedOutputStream();
 		System.setOut(new PrintStream(pos, true));
 
 		Console console = new Console(displayPane, pos);
 		new Thread(console).start();
 	}
 
 	public static void redirectErr(JTextArea displayPane)
 	{
 		PipedOutputStream pos = new PipedOutputStream();
 		System.setErr(new PrintStream(pos, true));
 
 		Console console = new Console(displayPane, pos);
 		new Thread(console).start();
 	}
 
 	public static void main(String[] args)
 	{
 		textField.addKeyListener(new Console());
 		scrollPane.setMinimumSize(new Dimension(30, 400));
 
 		JFrame frame = new JFrame("Console Window");
 		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 		JSplitPane panes = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scrollPane, textField);
 		panes.setDividerLocation(530);
 		frame.getContentPane().add(panes);
 		//frame.
 		//frame.getContentPane().add(scrollPane);
 		//frame.getContentPane().add(textField);
 		frame.setSize(300, 600);
 		frame.setVisible(true);
 
		Console.redirectOutput(textArea);
 		final int i = 0;
 
 		Timer timer = new Timer(1000, new ActionListener()
 		{
 			public void actionPerformed(ActionEvent e)
 			{
 				//System.out.println( new java.util.Date().toString() );
 				//System.err.println( System.currentTimeMillis() );
 			}
 		});
 		timer.start();
 	}
 
 	@Override
 	public void keyPressed(KeyEvent event)
 	{
 		try
 		{
 			Snatch.console("echo Command: "+command);
 			if(event.getKeyCode() == KeyEvent.VK_ENTER)
 			{
 				//command = textField.getText();
 				Snatch.console(textField.getText());
 				commands.add(textField.getText());
 				System.out.println("Console: " + textField.getText());
 				//command = "";
 				textField.setText("");
 				scroll = commands.size();
 				return;
 			}
 			if(event.getKeyCode() == KeyEvent.VK_UP && scroll > 0)
 			{
 				scroll--;
 				textField.setText(commands.get(scroll));
 				return;
 			}
 			if(event.getKeyCode() == KeyEvent.VK_DOWN && scroll < commands.size() - 1)
 			{
 				scroll++;
 				textField.setText(commands.get(scroll));
 				return;
 			}
 			if(event.getKeyCode() == KeyEvent.VK_DOWN && scroll < commands.size())
 			{
 				textField.setText(command);
 				return;
 			}
 			if(event.getKeyCode() != KeyEvent.VK_DOWN && event.getKeyCode() != KeyEvent.VK_UP)
 			{
 				command = textField.getText();
 			}
 			
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 		}
 	}
 
 	@Override
 	public void keyReleased(KeyEvent arg0)
 	{
 
 	}
 
 	@Override
 	public void keyTyped(KeyEvent arg0)
 	{
 	}
 
 }
