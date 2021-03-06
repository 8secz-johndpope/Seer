 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.gpmldiff;
 
 import javax.swing.*;
 import java.awt.*;
 import java.net.URL;
 import java.io.File;
 import org.pathvisio.util.FileUtils;
 import org.pathvisio.Engine;
 import org.pathvisio.debug.Logger;
 
 public class AppletMain extends JApplet
 {
 	GpmlDiffWindow panel;
 	
 	void openUrl(int pwyType, String param)
 	{
 		try
 		{
 			URL url = new URL (param);
 		
 			String protocol = url.getProtocol();
 			File f = null;
 			if(protocol.equals("file"))
 			{
 				f = new File(url.getFile());
 				panel.setFile (pwyType, f);
 			}
 			else
 			{
 				f = File.createTempFile("urlPathway", "." + Engine.PATHWAY_FILE_EXTENSION);
 				FileUtils.downloadFile(url, f);
 				panel.setFile (pwyType, f);
 			}
 		}
 		catch(Exception e)
 		{
 			Logger.log.error ("Exception While downloading url: " + param, e);
		}		
 	}
 	
 	public void init()
 	{
 		panel = new GpmlDiffWindow(this);
 		setContentPane (panel);
 
		openUrl (GpmlDiffWindow.PWY_OLD, getParameter ("old"));
		openUrl (GpmlDiffWindow.PWY_NEW, getParameter ("new"));
 	}
 	
 }
