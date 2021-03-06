 package gui;
 
 import java.awt.Color;
 import java.awt.Font;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.RenderingHints;
 import java.awt.event.InputEvent;
 import java.awt.event.KeyEvent;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.io.BufferedWriter;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.LinkedList;
 import java.util.concurrent.locks.ReentrantLock;
 
 import javax.swing.*;
 import javax.swing.text.*;
 
 import os.Pipe;
 import os.Init;
 import job.Job;
 import job.Shell;
 
 /**
  * gui/Console.java 
  * <br><br>
  * Draws gui for Console (Shell).
  * 
  * 
  * @author Lukáš Hain
  * 
  * @team <i>OutOfMemory</i> for KIV/OS 2013
  * @teamLeader Radek Petruška radekp25@students.zcu.cz
  * 
  */
 public class Console extends Job{
 
 	final private static String prompt = "$ ";
 
 	private boolean isRestricted = false;
 
 	private JFrame frame;
 	private JTextArea console;
 	private int offset = 0;
 	private ConsoleLock cl = new ConsoleLock(true, prompt);
 	private LinkedList<String> history = new LinkedList<String>();
 
 	private ActionMap restrictedActionMap = new ActionMap();
 	private InputMap restrictedInputMap = new InputMap();
 	private ActionMap normalActionMap = new ActionMap();
 	private InputMap normalInputMap = new InputMap();
 
 	private Thread stdErrThread;
 	private Thread swingWorker;
 	private Thread stdInThread;
 
 	private Shell shell;
 	/** Lock for sync between shell and console while using setUnrestricted(). */
 	ReentrantLock writingLock;
 	
 	private final Fortune fortune = new Fortune(0);
 	
 	PrintWriter log;
 
 
 	/**
 	 * Classic Constructor of Job.
 	 * 
 	 * @param PID
 	 * @param stdERR
 	 */
 	public Console(Integer PID, Pipe stdERR) {
 		super(PID, stdERR);
 		
 		this.writingLock = new ReentrantLock();
 
 		consoleInit();
 		this.frame = new JFrame();
 		this.frame.setTitle("OutOfMemoryOS Console, PID: " + this.PID);
 		this.frame.getContentPane().add(new JScrollPane(getConsole()));
 
 		try{
 			
 			this.frame.setIconImage(new ImageIcon
					(this.getClass().getResource("/gui/ico/icon645.png")).getImage());
 		} catch (Exception e) {
 			System.err.println("Console init() icon not found.");
 		}
 		
 		 
 
 		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
 		this.frame.addWindowListener(new WindowAdapter() {
 			
 			@Override
 			public void windowClosing(WindowEvent e) {
 				Console.this.closeTheConsole(true);
 			}
 		});
 		this.frame.setSize(800, 600);
 		this.frame.setVisible(true);
 		this.frame.setLocation(FramePosition.getPointConsole(this.frame));
 
 		this.console.setCaretPosition(this.console.getText().length());
 
 		startUpdate();
 	}
 	
 	
 	/**
 	 * Call this method when you want to close the console.
 	 */
 	public void closeTheConsole(boolean closeShell){
 		
 		if(this.log != null){
 			this.log.close();
 			this.log = null;
 		}
 		
 		if(this.stdErrThread.isInterrupted()){
 			System.err.println("Closing terminal MULTIPLE TIMES!");
 			return;
 		}
 	
 		this.stdErrThread.interrupt();
 		this.swingWorker.interrupt();
 		this.stdInThread.interrupt();
 	
 		
 		this.frame.dispose();
 	
 		if(!closeShell){
 			Init.terminalClosed(this);
 		}
 
 		if(closeShell){
 			try {
 				Init.killJob(this.shell.PID);
 			} catch (InterruptedException e) {
 				System.err.println("INTERUPTED WHILE KILLING SHELL in CONSOLE.");
 				e.printStackTrace();
 			}
 			//this.shell.closeShell();
 		}
 			
 		
 	}
 
 
 	public static String getPrompt() {
 		return prompt;
 	}
 
 	JTextArea getConsole() {
 		return this.console;
 	}
 
 	public void setShell(Shell shell) {
 		boolean wasShellNull = this.shell == null;
 		this.shell = shell;
 		this.frame.setTitle("console" + this.PID + ", shell: " + shell.PID);
 		if(wasShellNull){
 			print(this.fortune.getRandomJoke());
 			firstLine();
 		}
 	}
 
 	public Shell getShell() {
 		return this.shell;
 	}
 
 	/**
 	 * 
 	 * @return number of entries in history
 	 */
 	protected int getHistorySize() {
 		return this.history.size();
 	}
 
 	/**
 	 * Repaints terminal every 500 miliseconds because of blinking caret.
 	 */
 	private void startUpdate() {
 		this.swingWorker = new Thread(){
 
 			@Override
 			public void run(){
 				while(true){
 					try {
 						Thread.sleep(500);
 						getConsole().repaint();
 					} catch (InterruptedException e) {
 						return;
 					}                        
 				}
 			}
 
 		};
 
 		this.swingWorker.start();
 
 	}
 
 
 	/**
 	 * Initialize and set text area to input commands.
 	 */
 	private void consoleInit() {
 
 		this.console = new JTextArea(){
 			
 			@Override
 			public void paintComponent(Graphics g) {
 
 				try{
 					
 					Graphics2D graphics2d = (Graphics2D) g;
 					graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 					super.paintComponent(g);
 				} catch(NullPointerException e){
 					System.err.println("CONSOLE_INIT() repaint NULL POINTER EXCEPTION!");
 				}
 			}
 		};
 
 		try {
 			this.log = new PrintWriter(new BufferedWriter(new FileWriter(Init.home + Init.FS + "history.txt"), 256));
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 
 		//************* STATIC SETTINGS ***************************                
 		FancyCaret fc = new FancyCaret();
 		fc.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
 		this.console.setCaret(fc);
 		this.console.setLineWrap(true);
 		this.console.setWrapStyleWord(true);
 		((AbstractDocument)this.console.getDocument()).setDocumentFilter(this.cl);
 
 		this.history.add("");
 
 
 		ConsoleActions ca = new ConsoleActions(this);
 
 		//======================= ENTER ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
 		this.console.getActionMap().put("enter", ca.sendCommand);
 
 
 		//======================= CTRL + C ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "end");
 		this.console.getActionMap().put("end", ca.end);
 		
 		
 		//======================= CTRL + L ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK), "clean");
 		this.console.getActionMap().put("clean", ca.clean);
 		
 		
 		//======================= CTRL + PLUS ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK), "larger");
 		this.console.getActionMap().put("larger", ca.makeFontLarger);
 		
 		
 		//======================= CTRL + MINUS ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK), "smaller");
 		this.console.getActionMap().put("smaller", ca.makeFontSmaller);
 
 				
 		//======================= UP KEY ====================================================================");
 		this.console.getInputMap().put(KeyStroke.getKeyStroke("UP"), "moreHistory");
 		this.console.getActionMap().put("moreHistory", ca.moreHistory);
 
 
 		//======================= DOWN KEY ====================================================================
 		this.console.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "lessHistory");
 		this.console.getActionMap().put("lessHistory", ca.lessHistory);
 
 
 		this.console.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));		
 		this.console.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
 
 
 		this.normalActionMap = this.console.getActionMap();
 		this.normalInputMap = this.console.getInputMap();
 
 		//======================= SETTING RESTRICTED MAPS ==================================================
 		this.restrictedInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "end");
 		this.restrictedActionMap.put("end", ca.end);
 		//==================================================================================================
 
 		//************* DYNAMIC SETTINGS ***************************
 		setConsleBackground(Color.BLACK);
 		setConsoleFontColor(new Color(0, 204, 0));
 		setConsoleFontType(FontLoader.getConsoleFont());
 
 
 		this.console.setHighlighter(null);
 
 		while(this.console.getMouseListeners().length > 0){
 			this.console.removeMouseListener(this.console.getMouseListeners()[0]);
 		}
 
 		while(this.console.getMouseMotionListeners().length > 0){
 			this.console.removeMouseMotionListener(this.console.getMouseMotionListeners()[0]);
 		}
 	}
 
 
 	public void setConsoleFontType(Font f){
 		this.getConsole().setFont(f);
 	}
 	
 	public void setConsoleFontSize(int size){
 		if(size < 1){
 			return;
 		}
 		Font f = this.console.getFont();
 		this.console.setFont(f.deriveFont((float) size));
 	}
 
 
 	public void setConsoleFontColor(Color c){
 		this.getConsole().setForeground(c);
 	}
 
 
 	public void setConsleBackground(Color c){
 		this.getConsole().setBackground(c);
 	}
 
 
 	/**
 	 * Prints new line and sets carret position there.
 	 */
 	private void newLine(){
 		String temp = this.shell.getCurrentDirectory() + prompt;
 		print("\n" + temp);
 		this.offset = this.console.getText().lastIndexOf(prompt);
 		this.console.setCaretPosition(this.console.getText().length());
 		resetUserInput();
 	}
 
 	private void firstLine(){
 		String temp = this.shell.getCurrentDirectory() + prompt;
 		print(temp);
 		this.offset = this.console.getText().lastIndexOf(prompt);
 		this.console.setCaretPosition(this.console.getText().length());
 		resetUserInput();
 	}
 
 
 	protected void changeToHistory(int value) {
 		if(value < this.history.size()){
 			changeLastLineText(this.history.get(value));
 			this.cl.setUserInput(this.history.get(value));
 			this.shell.resetCommandString();
 		} 
 	}
 
 	/**
 	 * This method replaces anything what is behind last prompt with text in parameter. 
 	 * 
 	 * @param text which shoud replace current content
 	 */
 	private void changeLastLineText(String text){
 		try {
 			this.cl.setUseDocumentFilter(false);
 			this.console.setText(this.console.getText(0, this.offset + prompt.length()) + text);
 			this.cl.setUseDocumentFilter(true);
 		} catch (BadLocationException e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Prints string
 	 * @param string what to print
 	 */
 	public void print(String string) {
 		this.console.append(string);
 		this.console.validate();
 	}
 
 	/**
 	 * This will delete all text from console.
 	 */
 	protected void clean() {
 		this.cl.setUseDocumentFilter(false);
 		this.console.setText("");
 		this.cl.setUseDocumentFilter(true);
 		firstLine();
 	}
 
 	/**
 	 * Returns last command entered into console (without current path and prompt).
 	 * @return last command
 	 */
 	private String getLastCommand(){
 		String pom = this.cl.getUserInput();
 		resetUserInput();
 		return pom;
 	}
 
 	/**
 	 * Adds valid string (must have length 1 or greater and also 
 	 * must be different from last added string) to history of commands.
 	 * 
 	 * @param str command which will be added to history
 	 */
 	public void addToHistory(String str) {
 		if((this.history.size() > 1 && this.history.get(1).compareTo(str) != 0) || this.history.size() <= 1){
 			this.history.add(1, str);
 			this.log.println(this.shell.getCurrentDirectory() + " " + str);
 			this.log.flush();
 		} 
 	}
 
 	@Override
 	public String getManual() {
 		return ("This man should not be accesible at all. Use man terminal. ");
 	}
 
 	/**
 	 * This method prevents user from any editing of console. The only action
 	 * user can do after this method is CTRL + C for killing the process. This method 
 	 * should be used immediately after ENTER key was pressed.
 	 * <br>
 	 * <br>
 	 * <br>
 	 * For unlocking info @see setUnrestricted()
 	 * <br>
 	 * <br>
 	 * <b>IMPORTANT: This method is quite time consuming so please use it with this
 	 * fact in mind.</b>
 	 */
 	protected void setRestricted(){
 		if(!this.isRestricted){
 			this.console.setEditable(false);
 			this.cl.setUseDocumentFilter(false);
 			this.console.setActionMap(this.restrictedActionMap);
 			this.console.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this.restrictedInputMap);
 			this.console.setInputMap(JComponent.WHEN_FOCUSED, this.restrictedInputMap);
 
 			this.isRestricted = true;
 		}
 	}
 
 	/**
 	 * This method clears the restriction that setRestricted() method enabled.
 	 * Console can be unlocked under these circumstances:
 	 * <br>
 	 * <li>Process was running in fourground and already printed his output.</li>
 	 * <li>Process was started and is running in background.</li>
 	 * <br>
 	 * <br>
 	 * <b>IMPORTANT: This method is quite time consuming so please use it with this
 	 * fact in mind.</b>
 	 * <br>
 	 * <br>
 	 * @param newLine when true
 	 */
 	public void setUnrestricted(boolean newLine){
 		if(this.isRestricted){
 			this.writingLock.lock();
 				
 			this.console.setInputMap(JComponent.WHEN_FOCUSED, this.normalInputMap);
 			this.console.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this.normalInputMap);
 			this.console.setActionMap(this.normalActionMap);
 			this.cl.setUseDocumentFilter(true);
 			
 			if(newLine){
 				newLine();
 			}
 			this.console.setEditable(true);
 
 			this.isRestricted = false;			
 			
 			this.writingLock.unlock();
 			
 			
 			return;
 		}
 		if(newLine)
 			newLine();
 	}
 	
 	@Override
 	public void live() {
 		Init.terminalOpened(this);
 		super.live();
 	}
 	
 	@Override
 	public void SIG_KILL(){
 		super.SIG_KILL();
 		closeTheConsole(true);
 	}
 
 	@Override
 	protected void getJobDone() throws InterruptedException {
 		
 		/** Read stdErr pipe **/
 		this.stdErrThread = new Thread(){
 
 			@Override
 			public void run(){
 				char[] chars = new char[BUF_SIZE];
 				int i = 0;
 				while(i > -1){
 					try{
 						
 						i = Console.this.getStdErr().getData(chars, Console.this.writingLock);
 						if(i != -1){
 							print(String.copyValueOf(chars, 0, i));
 						}
 						
 						Console.this.writingLock.unlock();
 						
 						} catch (IOException e) {
 							print("Console read error");
 						}catch (InterruptedException e) {
 						return;
 					}
 				}
 			}};
 		this.stdErrThread.start();
 
 		
 		/** Read stdIn pipe **/
 		this.stdInThread = new Thread(new Runnable() {
 
 			public void run() {
 				char[] chars = new char[BUF_SIZE];
 				int i = 0;
 				while(i > -1){
 					try {
 
 						i = Console.this.getStdIn().getData(chars, Console.this.writingLock);
 
 						if(i != -1){
 							print(String.copyValueOf(chars, 0, i));
 						}
 						
 						Console.this.writingLock.unlock();
 						
 					} catch (IOException e) {
 						print("Console read error");
 					} catch (InterruptedException e) {
 						return;
 					}
 				}			
 			}
 
 		});
 
 		this.stdInThread.start();
 
 		/** Waits for threads in this console to do something */
 		this.stdErrThread.join();
 		this.stdInThread.join();
 		this.swingWorker.join();
 		
 		System.out.println("CONSOLE " + this.PID + " ENDED NORMALLY.");
 	}
 
 	/**
 	 * Push last command into pipe.
 	 */
 	public void consolePush(){
 		try {
 			String str = getLastCommand();
 			if(str.length() > 0){
 				setRestricted();
 				print("\n");
 			}
 			pushData(str.toCharArray(), 0, str.toCharArray().length);
 			pushData(new char[]{'\n'}, 0, 1);//let shell know there is the end
 
 		} catch (IOException e) {
 			e.printStackTrace();
 		} catch (InterruptedException e) {
 			System.err.println("ConsolePush() interrupted.");
 		}
 	}
 
 	protected void resetUserInput() {
 		this.cl.setUserInput("");		
 	}
 
 	protected void makeFontSmaller() {
 		setConsoleFontSize(this.console.getFont().getSize() - 1);
 	}
 	
 	protected void makeFontLarger() {
 		setConsoleFontSize(this.console.getFont().getSize() + 1);	
 	}
 	
 	
 }
