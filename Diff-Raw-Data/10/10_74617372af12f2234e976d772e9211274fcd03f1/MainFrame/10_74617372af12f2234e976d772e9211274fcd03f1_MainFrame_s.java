 /*
   JSmooth: a VM wrapper toolkit for Windows
   Copyright (C) 2003 Rodrigo Reyes <reyes@charabia.net>
  
   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.
  
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
  
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  
  */
 
 package net.charabia.jsmoothgen.application.gui;
 
 import net.charabia.jsmoothgen.application.*;
 import net.charabia.jsmoothgen.application.gui.util.*;
 import net.charabia.jsmoothgen.skeleton.*;
 import java.io.*;
 import java.util.*;
 import javax.swing.*;
 import java.util.prefs.*;
 import java.awt.*;
 
 public class MainFrame extends javax.swing.JFrame implements MainController
 {
     private StaticWizard m_wizard;
     private SkeletonList m_skelList;
     private File m_projectFile = null;
 	
     final static public String VERSION = "@{VERSION}@";
     final static public String RELEASEINFO = "@{RELEASEINFO}@";
     private RecentFileMenu m_recentMenuManager;
     /** Creates new form MainFrame */
     public MainFrame()
     {
 	super();
 
 	Splash splash = new Splash(this, "/icons/splash.png", false);
 	splash.setVersion(VERSION);
 	splash.show();
 	m_skelList = new SkeletonList(new File("skeletons"));
 		
 	initComponents();
 	m_recentMenuManager = new RecentFileMenu(m_recentMenu, 5, 
 						 MainFrame.class, 
 						 new RecentFileMenu.Action() {
 						     public void action(String path)
 						     {
 							 openDirect(new File(path));
 						     }
 						 });
 		
 	m_wizard = new StaticWizard();
 	m_wizard.setMainController(this);
 	m_centralPane.add(m_wizard);
 		
 	m_projectFileChooser.addChoosableFileFilter(new SimpleFileFilter("jsmooth", "JSmooth Project Files"));
 
 	loadWindowSettings();
 	show();
 
 	splash.dispose();
     }
 	
     /** This method is called from within the constructor to
      * initialize the form.
      * WARNING: Do NOT modify this code. The content of this method is
      * always regenerated by the Form Editor.
      */
     private void initComponents()//GEN-BEGIN:initComponents
     {
         javax.swing.JMenu jMenu1;
 
         m_projectFileChooser = new javax.swing.JFileChooser();
         jToolBar1 = new javax.swing.JToolBar();
         m_buttonNew = new javax.swing.JButton();
         m_buttonOpen = new javax.swing.JButton();
         m_buttonSave = new javax.swing.JButton();
         m_buttonSaveAs = new javax.swing.JButton();
         jSeparator4 = new javax.swing.JSeparator();
         m_buttonCompile = new javax.swing.JButton();
         m_buttonRunExe = new javax.swing.JButton();
         jSeparator5 = new javax.swing.JSeparator();
         jButton1 = new javax.swing.JButton();
         jSeparator6 = new javax.swing.JSeparator();
         jSeparator7 = new javax.swing.JSeparator();
         jSeparator8 = new javax.swing.JSeparator();
         jPanel1 = new javax.swing.JPanel();
         jSeparator1 = new javax.swing.JSeparator();
         jTextField1 = new javax.swing.JTextField();
         m_centralPane = new javax.swing.JPanel();
         jMenuBar1 = new javax.swing.JMenuBar();
         jMenu1 = new javax.swing.JMenu();
         m_menuNew = new javax.swing.JMenuItem();
         jSeparator2 = new javax.swing.JSeparator();
         m_menuLoad = new javax.swing.JMenuItem();
         m_menuSave = new javax.swing.JMenuItem();
         m_menuSaveAs = new javax.swing.JMenuItem();
         jSeparator3 = new javax.swing.JSeparator();
         m_recentMenu = new javax.swing.JMenu();
         jSeparator9 = new javax.swing.JSeparator();
         m_menuExit = new javax.swing.JMenuItem();
         m_menuProject = new javax.swing.JMenu();
         m_menuCompile = new javax.swing.JMenuItem();
         m_menuRunExe = new javax.swing.JMenuItem();
         jMenu2 = new javax.swing.JMenu();
         m_menuAbout = new javax.swing.JMenuItem();
 
        setTitle("JSmooth");
         addWindowListener(new java.awt.event.WindowAdapter()
         {
             public void windowClosing(java.awt.event.WindowEvent evt)
             {
                 exitForm(evt);
             }
         });
 
         m_buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_new.png")));
         m_buttonNew.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonNewActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonNew);
 
         m_buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_open.png")));
         m_buttonOpen.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonOpenActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonOpen);
 
         m_buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_save.png")));
         m_buttonSave.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonSaveActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonSave);
 
         m_buttonSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_save_as.png")));
         m_buttonSaveAs.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonSaveAsActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonSaveAs);
 
         jToolBar1.add(jSeparator4);
 
         m_buttonCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_autopilot-24.png")));
         m_buttonCompile.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonCompileActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonCompile);
 
         m_buttonRunExe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_next.png")));
         m_buttonRunExe.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 buttonRunExeActionPerformed(evt);
             }
         });
 
         jToolBar1.add(m_buttonRunExe);
 
         jToolBar1.add(jSeparator5);
 
         jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_about.png")));
         jButton1.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuAboutActionPerformed(evt);
             }
         });
 
         jToolBar1.add(jButton1);
 
         jToolBar1.add(jSeparator6);
 
         jToolBar1.add(jSeparator7);
 
         jToolBar1.add(jSeparator8);
 
         getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);
 
         jPanel1.setLayout(new java.awt.BorderLayout());
 
         jSeparator1.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 1, 2, 1)), new javax.swing.border.EtchedBorder()));
         jPanel1.add(jSeparator1, java.awt.BorderLayout.NORTH);
 
         jTextField1.setEditable(false);
         jTextField1.setHorizontalAlignment(javax.swing.JTextField.LEFT);
         jTextField1.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(1, 1, 1, 1)));
         jTextField1.setFocusable(false);
         jPanel1.add(jTextField1, java.awt.BorderLayout.CENTER);
 
         getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
 
         m_centralPane.setLayout(new java.awt.GridLayout(1, 1));
 
         getContentPane().add(m_centralPane, java.awt.BorderLayout.CENTER);
 
         jMenu1.setText("File");
         m_menuNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
         m_menuNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_new-16.png")));
         m_menuNew.setText("New");
         m_menuNew.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuNewActionPerformed(evt);
             }
         });
 
         jMenu1.add(m_menuNew);
 
         jMenu1.add(jSeparator2);
 
         m_menuLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
         m_menuLoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_open-16.png")));
         m_menuLoad.setText("Open project...");
         m_menuLoad.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuLoadActionPerformed(evt);
             }
         });
 
         jMenu1.add(m_menuLoad);
 
         m_menuSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
         m_menuSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_save-16.png")));
         m_menuSave.setText("Save");
         m_menuSave.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuSaveActionPerformed(evt);
             }
         });
 
         jMenu1.add(m_menuSave);
 
         m_menuSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_save_as-16.png")));
         m_menuSaveAs.setText("Save as...");
         m_menuSaveAs.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuSaveAsActionPerformed(evt);
             }
         });
 
         jMenu1.add(m_menuSaveAs);
 
         jMenu1.add(jSeparator3);
 
         m_recentMenu.setText("Recent Files");
         jMenu1.add(m_recentMenu);
 
         jMenu1.add(jSeparator9);
 
         m_menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_exit-16.png")));
         m_menuExit.setText("Exit");
         m_menuExit.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuExitActionPerformed(evt);
             }
         });
 
         jMenu1.add(m_menuExit);
 
         jMenuBar1.add(jMenu1);
 
         m_menuProject.setText("Project");
         m_menuCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_autopilot-16.png")));
         m_menuCompile.setText("Create Exe");
         m_menuCompile.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuCompileActionPerformed(evt);
             }
         });
 
         m_menuProject.add(m_menuCompile);
 
         m_menuRunExe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_next-16.png")));
         m_menuRunExe.setText("Run Exe");
         m_menuRunExe.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuRunExeActionPerformed(evt);
             }
         });
 
         m_menuProject.add(m_menuRunExe);
 
         jMenuBar1.add(m_menuProject);
 
         jMenu2.setText("Help");
         m_menuAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/stock_about-16.png")));
         m_menuAbout.setText("About");
         m_menuAbout.addActionListener(new java.awt.event.ActionListener()
         {
             public void actionPerformed(java.awt.event.ActionEvent evt)
             {
                 menuAboutActionPerformed(evt);
             }
         });
 
         jMenu2.add(m_menuAbout);
 
         jMenuBar1.add(jMenu2);
 
         setJMenuBar(jMenuBar1);
 
         java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
         setBounds((screenSize.width-578)/2, (screenSize.height-462)/2, 578, 462);
     }//GEN-END:initComponents
 
     private void menuExitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuExitActionPerformed
     {//GEN-HEADEREND:event_menuExitActionPerformed
 	// Add your handling code here:
 	exitForm(null);
     }//GEN-LAST:event_menuExitActionPerformed
 
     private void buttonNewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonNewActionPerformed
     {//GEN-HEADEREND:event_buttonNewActionPerformed
 	// Add your handling code here:
 	JSmoothModelBean model = new JSmoothModelBean();
 	setTitle("JSmooth " + VERSION + ": New project");
 	m_wizard.setModel(null, model);
 	m_projectFile = null;
     }//GEN-LAST:event_buttonNewActionPerformed
 
     private void buttonRunExeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonRunExeActionPerformed
     {//GEN-HEADEREND:event_buttonRunExeActionPerformed
 	// Add your handling code here:
 	runexe();
     }//GEN-LAST:event_buttonRunExeActionPerformed
 	
     private void menuRunExeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuRunExeActionPerformed
     {//GEN-HEADEREND:event_menuRunExeActionPerformed
 	// Add your handling code here:
 	runexe();
     }//GEN-LAST:event_menuRunExeActionPerformed
 	
     private void menuCompileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuCompileActionPerformed
     {//GEN-HEADEREND:event_menuCompileActionPerformed
 	// Add your handling code here:
 	compile();
     }//GEN-LAST:event_menuCompileActionPerformed
 	
     private void menuAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuAboutActionPerformed
     {//GEN-HEADEREND:event_menuAboutActionPerformed
 	// Add your handling code here:
 	AboutBox ab = new AboutBox(this, true);
 	ab.setVersion(VERSION + " (" + RELEASEINFO + ")");
 	ab.show();
     }//GEN-LAST:event_menuAboutActionPerformed
 	
     private void buttonSaveAsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonSaveAsActionPerformed
     {//GEN-HEADEREND:event_buttonSaveAsActionPerformed
 	// Add your handling code here:
 	save(true);
     }//GEN-LAST:event_buttonSaveAsActionPerformed
 	
     private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonSaveActionPerformed
     {//GEN-HEADEREND:event_buttonSaveActionPerformed
 	// Add your handling code here:
 	save(false);
     }//GEN-LAST:event_buttonSaveActionPerformed
 	
     private void buttonOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonOpenActionPerformed
     {//GEN-HEADEREND:event_buttonOpenActionPerformed
 	// Add your handling code here:
 	open();
     }//GEN-LAST:event_buttonOpenActionPerformed
 	
     private void buttonCompileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_buttonCompileActionPerformed
     {//GEN-HEADEREND:event_buttonCompileActionPerformed
 	// Add your handling code here:
 	compile();
     }//GEN-LAST:event_buttonCompileActionPerformed
 	
 	
     public boolean compile()
     {	
 	m_wizard.updateModel();
 		
 	if (m_projectFile ==  null)
 	    {
 		JOptionPane.showMessageDialog(this, "Save the project first!");
 		return false;			
 	    }
 		
 	m_wizard.getModel().normalizePaths(m_projectFile.getParentFile());		
 	
 	JSmoothModelBean model = m_wizard.getModel();
 	if (model.getSkeletonName() == null)
 	    {
 		String msg = "The skeleton is not specified!";
 		JOptionPane.showMessageDialog(this, msg);
 		return false;
 	    }
 	SkeletonBean skel = m_skelList.getSkeleton(model.getSkeletonName());
 	if (skel == null)
 	    {
 		String msg = "The skeleton is not registered!";
 		JOptionPane.showMessageDialog(this, msg);
 		return false;			
 	    }
 	File skelroot = m_skelList.getDirectory(skel);
 	File basedir = m_projectFile.getParentFile();
 	File exedir = basedir;
 
 	try
 	    {
		File out = new File(exedir, model.getExecutableName());
 		System.out.println("out = "+ out.getAbsolutePath());
 		ExeCompiler compiler = new ExeCompiler();
 		ExeCompiler.CompilerRunner runner = compiler.getRunnable(skelroot, skel, basedir, model, out);
 
 		CompilationDialog dia = new CompilationDialog(this, true);
 		dia.setCompiler(compiler);
 		dia.compile(runner);
 		System.out.println("FINISH !!");			
 		return dia.getResult();
 
 		//			boolean res = compiler.compile(skelroot, skel, model, out);
 		//			compiler.compileAsync(skelroot, skel, model, out);
 		//			if (res == false)
 		//			{
 		//				String msg = "<html>There are errors in the compilation process:<p><ol>";
 		//				Vector errs = compiler.getErrors();
 		//				for (Iterator i=errs.iterator(); i.hasNext(); )
 		//				{
 		//					msg += "<li>" + i.next().toString() + "<br>";
 		//				}
 		//				msg += "</ol></html>";
 		//				JOptionPane.showMessageDialog(this, msg);
 		//				return false;
 		//			}
 	    } catch (Exception exc)
 		{
 		    String msg = "Not all the parameters have been specified.\nCompilation aborted.";
 		    JOptionPane.showMessageDialog(this, msg);
 		    return false;
 		}
     }
 	
     public void runexe()
     {
 	m_wizard.updateModel();
 	JSmoothModelBean model = m_wizard.getModel();
 		
 	try
 	    {
 		File basedir = m_projectFile.getParentFile();
 		File f = new File(basedir, model.getExecutableName());
 		String[] cmd = new String[]{
 		    f.getAbsolutePath()
 		};
 			
 		System.out.println("RUNNING " + cmd[0] + " @ " + basedir);
 			
 		CommandRunner.run(cmd, f.getParentFile());
 
 	    } catch (Exception exc)
 		{
 		    //exc.printStackTrace();
 		}
     }
 	
     private void menuLoadActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuLoadActionPerformed
     {//GEN-HEADEREND:event_menuLoadActionPerformed
 	// Add your handling code here:
 	open();
     }//GEN-LAST:event_menuLoadActionPerformed
 	
     private void menuSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuSaveActionPerformed
     {//GEN-HEADEREND:event_menuSaveActionPerformed
 	// Add your handling code here:
 	save(false);
     }//GEN-LAST:event_menuSaveActionPerformed
 	
     private void menuSaveAsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuSaveAsActionPerformed
     {//GEN-HEADEREND:event_menuSaveAsActionPerformed
 	// Add your handling code here:
 	save(true);
     }//GEN-LAST:event_menuSaveAsActionPerformed
 	
     private String getSuffix(java.io.File f)
     {
 	String fstr = f.getAbsolutePath();
 	int lastDot = fstr.lastIndexOf('.');
 	if ((lastDot >= 0) && ((lastDot+1) < fstr.length()))
 	    {
 		return fstr.substring(lastDot+1);
 	    }
 	return "";
     }
     public void open()
     {
 	if (m_projectFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
 	    {
 		if (openDirect(m_projectFileChooser.getSelectedFile()))
 		    m_recentMenuManager.add(m_projectFileChooser.getSelectedFile().getAbsolutePath());
 	    }
     }
 	
     public boolean openDirect(File path)
     {
 	this.setTitle("JSmooth " + VERSION + ": " + path.toString());
 	m_projectFile = path;
 	try
 	    {
 		JSmoothModelBean model = JSmoothModelPersistency.load(m_projectFile);
 		File basedir = m_projectFile.getParentFile();
 		m_wizard.setModel(basedir, model);
 		return true;
 	    } catch (IOException iox)
 		{
 		    iox.printStackTrace();
 		    return false;
 		}
     }
 	
     public void save(boolean forceNewFile)
     {
 	m_wizard.updateModel();
 		
 	if (forceNewFile || (m_projectFile == null))
 	    {
 		if (m_projectFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
 		    {
 			if (m_projectFile != null)
 			    m_wizard.getModel().normalizePaths(m_projectFile.getParentFile(), false);
 
 			m_projectFile = m_projectFileChooser.getSelectedFile();
 			String suf = getSuffix(m_projectFile);
 			if ("jsmooth".equalsIgnoreCase(suf) == false)
 			    {
 				m_projectFile = new File(m_projectFile.toString() + ".jsmooth");
 			    }
 			
 			m_wizard.getModel().normalizePaths(m_projectFile.getParentFile(), true);
 		    }
 		else
 		    {
 			return;
 		    }
 		this.setTitle("JSmooth " +VERSION + ": " + m_projectFile.toString());
 	    }
 	try
 	    {
 		String[]res = m_wizard.getModel().normalizePaths(m_projectFile.getParentFile());
 			
 		Preferences prefs = Preferences.systemNodeForPackage(this.getClass());
                 String prefname = "filesNotRelativeWarningDontDisplay";
 		if ((res != null) && (prefs.getBoolean(prefname, false) == false))
 		    {
                         WarningNotRelativeFilesDialog wnrfd = new WarningNotRelativeFilesDialog(this, true);
                         wnrfd.setErrors(res);
                         wnrfd.show();
                         
                         if (wnrfd.dontDisplayAnymore() == true)
 			    {
 				prefs.putBoolean(prefname, true);
 			    }
 		    }
 			
 		JSmoothModelPersistency.save(m_projectFile, m_wizard.getModel());
 	    } catch (IOException iox)
 		{
 		    iox.printStackTrace();
 		}
 
 	JSmoothModelBean model = m_wizard.getModel();
 	File basedir = m_projectFile.getParentFile();
 	m_wizard.setModel(basedir, model);
 
 	m_recentMenuManager.add(m_projectFile.getAbsolutePath());
     }
 	
     private void menuNewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuNewActionPerformed
     {//GEN-HEADEREND:event_menuNewActionPerformed
 	// Add your handling code here:
 	JSmoothModelBean model = new JSmoothModelBean();
 	m_wizard.setModel(null, model);
 	m_projectFile = null;
     }//GEN-LAST:event_menuNewActionPerformed
 	
     /** Exit the Application */
     private void exitForm(java.awt.event.WindowEvent evt)//GEN-FIRST:event_exitForm
     {
 	m_recentMenuManager.savePrefs();
 	saveWindowSettings();
 	System.exit(0);
     }//GEN-LAST:event_exitForm
 	
     /**
      * @param args the command line arguments
      */
     public static void main(String args[])
     {
 	try {
 	    UIManager.setLookAndFeel(
 				     UIManager.getSystemLookAndFeelClassName());
 	} catch (Exception e) { }
 	
 	new MainFrame().show();
     }
 	
     public void setStateText(String text)
     {
     }
 	
     public SkeletonList getSkeletonList()
     {
 	return m_skelList;
     }
 
     public void saveWindowSettings()
     {
 	Preferences prefs = Preferences.systemNodeForPackage(this.getClass());
 	System.out.println("prefs: " + prefs);
 	prefs.putInt("window-x", (int)this.getLocation().getX());
 	prefs.putInt("window-y", (int)this.getLocation().getY());
 
 	prefs.putInt("window-width", (int)this.getWidth());
 	prefs.putInt("window-height", (int)this.getHeight());
 	
 	Rectangle r = this.getMaximizedBounds();
 	if (r != null)
 	    {
 		prefs.putInt("window-maximized-x", (int)r.getX());
 		prefs.putInt("window-maximized-y", (int)r.getY());
 		prefs.putInt("window-maximized-width", (int)r.getWidth());
 		prefs.putInt("window-maximized-height", (int)r.getHeight());
 	    }
 
 	prefs.putInt("window-state", this.getExtendedState());
     }
 
     public void loadWindowSettings()
     {
 	Preferences prefs = Preferences.systemNodeForPackage(this.getClass());
 
 	this.setExtendedState(prefs.getInt("window-state", Frame.NORMAL));
 
 	if (prefs.getInt("window-x", -1) > 0)
 	    {
 		this.setLocation(prefs.getInt("window-x", 10), prefs.getInt("window-y", 10));
 		this.setSize(prefs.getInt("window-width", 500), prefs.getInt("window-height", 400));
 		
 		if (prefs.getInt("window-maximized-x", -1) > 0)
 		    {
 			Rectangle maxb = new Rectangle();
 			maxb.setLocation(prefs.getInt("window-maximized-x", 10), prefs.getInt("window-maximized-y", 10));
 			maxb.setSize(prefs.getInt("window-maximized-width", 500), prefs.getInt("window-maximized-height", 400));
 		    }
 	    }
     }
     
     // Variables declaration - do not modify//GEN-BEGIN:variables
     private javax.swing.JButton jButton1;
     private javax.swing.JMenu jMenu2;
     private javax.swing.JMenuBar jMenuBar1;
     private javax.swing.JPanel jPanel1;
     private javax.swing.JSeparator jSeparator1;
     private javax.swing.JSeparator jSeparator2;
     private javax.swing.JSeparator jSeparator3;
     private javax.swing.JSeparator jSeparator4;
     private javax.swing.JSeparator jSeparator5;
     private javax.swing.JSeparator jSeparator6;
     private javax.swing.JSeparator jSeparator7;
     private javax.swing.JSeparator jSeparator8;
     private javax.swing.JSeparator jSeparator9;
     private javax.swing.JTextField jTextField1;
     private javax.swing.JToolBar jToolBar1;
     private javax.swing.JButton m_buttonCompile;
     private javax.swing.JButton m_buttonNew;
     private javax.swing.JButton m_buttonOpen;
     private javax.swing.JButton m_buttonRunExe;
     private javax.swing.JButton m_buttonSave;
     private javax.swing.JButton m_buttonSaveAs;
     private javax.swing.JPanel m_centralPane;
     private javax.swing.JMenuItem m_menuAbout;
     private javax.swing.JMenuItem m_menuCompile;
     private javax.swing.JMenuItem m_menuExit;
     private javax.swing.JMenuItem m_menuLoad;
     private javax.swing.JMenuItem m_menuNew;
     private javax.swing.JMenu m_menuProject;
     private javax.swing.JMenuItem m_menuRunExe;
     private javax.swing.JMenuItem m_menuSave;
     private javax.swing.JMenuItem m_menuSaveAs;
     private javax.swing.JFileChooser m_projectFileChooser;
     private javax.swing.JMenu m_recentMenu;
     // End of variables declaration//GEN-END:variables
 	
 }
