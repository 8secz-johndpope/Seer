 /*
  *  soapUI, copyright (C) 2004-2008 eviware.com 
  *
  *  soapUI is free software; you can redistribute it and/or modify it under the 
  *  terms of version 2.1 of the GNU Lesser General Public License as published by 
  *  the Free Software Foundation.
  *
  *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
  *  See the GNU Lesser General Public License for more details at gnu.org.
  */
 
 package com.eviware.x.impl.swing;
 
 import java.awt.Component;
 import java.io.File;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.swing.JFileChooser;
 
 import com.eviware.soapui.support.ExtensionFileFilter;
 import com.eviware.x.dialogs.XFileDialogs;
 
 /**
  * 
  * @author Lars
  */
 public class SwingFileDialogs implements XFileDialogs
 {
    private static Component parent;
    private static Map<Object, JFileChooser> choosers = new HashMap<Object, JFileChooser>();
   
    public SwingFileDialogs(Component parent)
    {
       SwingFileDialogs.parent = parent;
    }
    
    public static synchronized JFileChooser getChooser(Object action)
    {
    	action = null;
       JFileChooser chooser = choosers.get(action);
       if( chooser == null )
       {
          chooser = new JFileChooser();
          choosers.put(action, chooser);
       }
       
       chooser.resetChoosableFileFilters();
 
       return chooser;
    }
    
    public static Component getParent()
    {
       return parent;
    }
    
    public File saveAs(Object action, String title)
    {
       return saveAs(action, title, null, null, null);
    }
    
    public File saveAs(Object action, String title, String extension, String fileType, File defaultFile)
    {
       JFileChooser chooser = getChooser(action);
       chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
       chooser.setDialogTitle(title);
       chooser.setAcceptAllFileFilterUsed( true );
 
       if(extension != null && fileType != null)
          chooser.setFileFilter( new ExtensionFileFilter( extension, fileType ));
       
       if(defaultFile != null)
          chooser.setSelectedFile(defaultFile);
       
       if (chooser.showSaveDialog(getParent()) != JFileChooser.APPROVE_OPTION)
          return null;
          
       return chooser.getSelectedFile();
    }
    
    public File open(Object action, String title, String extension, String fileType, String current)
    {
    	return openFile( action, title, extension, fileType, current );
    }
    
    public static File openFile(Object action, String title, String extension, String fileType, String current)
    {
       JFileChooser chooser = getChooser(action);
      chooser.setFileSelectionMode( JFileChooser.FILES_ONLY);
       chooser.setDialogTitle(title);
       chooser.setAcceptAllFileFilterUsed( true );
       if( current != null )
       {
       	File file = new File( current);
       	if( file.isDirectory())
       		chooser.setCurrentDirectory(file);
       	else
       		chooser.setSelectedFile( file );
       }
       
       if(extension != null && fileType != null)
          chooser.setFileFilter( new ExtensionFileFilter( extension, fileType ));
       
       if (chooser.showOpenDialog(getParent()) != JFileChooser.APPROVE_OPTION)
          return null;
          
       return chooser.getSelectedFile();      
    }
    
    public File openXML(Object action, String title)
    {
       return open(action, title, ".xml", "XML Files (*.xml)", null);
    }
    
    public File openDirectory(Object action, String title, File defaultDirectory)
    {
       JFileChooser chooser = getChooser(action);
       chooser.setDialogTitle(title);
       chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
 
       if(defaultDirectory != null)
          chooser.setCurrentDirectory( defaultDirectory );
       
       if (chooser.showOpenDialog(getParent()) != JFileChooser.APPROVE_OPTION)
          return null;
          
       return chooser.getSelectedFile();
    }
 }
