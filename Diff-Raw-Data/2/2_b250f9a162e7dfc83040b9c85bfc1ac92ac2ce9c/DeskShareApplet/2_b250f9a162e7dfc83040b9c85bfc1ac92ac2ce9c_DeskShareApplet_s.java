 /**
 * BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
 * 
 * Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
 *
 */
 package org.bigbluebutton.deskshare.client;
 
 import javax.imageio.ImageIO;
 import javax.swing.JApplet;
 import javax.swing.JFrame;
 import javax.swing.JOptionPane;
 import java.io.IOException;
 import java.net.URL;
 import java.security.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.awt.Image;
 
 public class DeskShareApplet extends JApplet implements ClientListener {
 	public static final String NAME = "DESKSHAREAPPLET: ";
 	
 	private static final long serialVersionUID = 1L;
 
 	String hostValue = "localhost";
     Integer portValue = new Integer(9123);
     String roomValue = "85115";
     Integer cWidthValue = new Integer(800);
     Integer cHeightValue = new Integer(600);
     Integer sWidthValue = new Integer(800);
     Integer sHeightValue = new Integer(600);   
     Boolean qualityValue = false;
     Boolean aspectRatioValue = false;
     Integer xValue = new Integer(0);
     Integer yValue = new Integer(0);
     Boolean tunnelValue = true;
     Boolean fullScreenValue = false;
     Boolean useSVC2Value = false;
     DeskshareClient client;
     Image icon;
     
     public boolean isSharing = false;
     private volatile boolean clientStarted = false;
     private static final String JAVA_VERSION_PATTERN = "1.7.0_([0-9]+)";
     private final int MIN_JRE_VERSION = 45;
     private final static String VERSION_ERROR_MSG = "Desktop sharing requires Java 7 update 45 (or later) to run.";
     
     private class DestroyJob implements PrivilegedExceptionAction {
        public Object run() throws Exception {
 		System.out.println("Desktop Sharing Applet Destroy");
 		if (clientStarted) {
 			client.stop();	
 		}
                	return null;
        }
     }
     
     @Override
 	public void init() {		
     	System.out.println("Desktop Sharing Applet Initializing");
     	
 		hostValue = getParameter("IP");
 		String port = getParameter("PORT");
 		if (port != null) portValue = Integer.parseInt(port);
 		roomValue = getParameter("ROOM");
 
 		String captureFullScreen = getParameter("FULL_SCREEN");
 		if (captureFullScreen != null) fullScreenValue = Boolean.parseBoolean(captureFullScreen);
 		
 		String useSVC2 = getParameter("SVC2");
 		if (useSVC2 != null) useSVC2Value = Boolean.parseBoolean(useSVC2);
 
 		String tunnel = getParameter("HTTP_TUNNEL");
 		if (tunnel != null) tunnelValue = Boolean.parseBoolean(tunnel);
 		try {
 			URL url = new URL(getCodeBase(), "bbb.gif");
 			icon = ImageIO.read(url);
 		} catch (IOException e) {
 		}
 	}
 	 
 	private String getJavaVersionRuntime() {
 		return System.getProperty("java.version");
 	}
 
     /**
      * Create the GUI and show it.  For thread safety,
      * this method should be invoked from the
      * event-dispatching thread.
      */
     private void createAndShowGUI(final String warning) {
 		JOptionPane.showMessageDialog(null,
 				warning,
		    "Java Version Check",
 		    JOptionPane.ERROR_MESSAGE);
 		stop();
     }
     
 	private void displayJavaWarning(final String warning) {	
 		//Schedule a job for the event-dispatching thread:
         //creating and showing this application's GUI.
         javax.swing.SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 createAndShowGUI(warning);
             }
         });
 	}
 		
 	@Override
 	public void start() {		 	
 		System.out.println("Desktop Sharing Applet Starting");
 		super.start();
 		
 		System.out.println("**** JAVA VERSION = [" + getJavaVersionRuntime() + "]");
 		
 		Pattern p = Pattern.compile(JAVA_VERSION_PATTERN);
 		Matcher matcher = p.matcher(getJavaVersionRuntime());
 		if (matcher.matches()) {
 			int jreVersion = Integer.valueOf(matcher.group(1).trim()).intValue();
 			if (jreVersion < MIN_JRE_VERSION) {
 				displayJavaWarning(VERSION_ERROR_MSG);
 			} else {
 				allowDesktopSharing();
 			}
 		} else {
 			displayJavaWarning(VERSION_ERROR_MSG);
 		}
 	}
 	
 	private void allowDesktopSharing() {
 		client = new DeskshareClient.NewBuilder().host(hostValue).port(portValue)
 				.room(roomValue).captureWidth(cWidthValue)
 				.captureHeight(cHeightValue).scaleWidth(sWidthValue).scaleHeight(sHeightValue)
 				.quality(qualityValue).autoScale(0.8)
 				.x(xValue).y(yValue).fullScreen(fullScreenValue).useSVC2(useSVC2Value)
 				.httpTunnel(tunnelValue).trayIcon(icon).enableTrayIconActions(false).build();
 		client.addClientListener(this);
 		
 		clientStarted = true;
 		
 		client.start();		
 	}
 
 	
 	@Override
 	public void destroy() {
 		/* We make this a privileged job.
 		* The privileges of the javascript code  are 'anded' with the 
                 * java privs. Sometimes (depending on jre version, browser, etc.)
                 * javascript will not have the privs to do some of the operations 
                 * required for destroy, particularly network related activities,
 		* but java does. So we make sure here that we run only considering
                 * java privs, not javascript's. This should be 'security safe', since
                 * we are only shutting things down.
 		*/ 
                 try {
  			AccessController.doPrivileged( this.new DestroyJob() );
                 } catch ( PrivilegedActionException e) {
 			System.out.println("Exception during Desktop Sharing Applet Stopping"+e.toString());
 			UncheckedExceptions.spit((Exception) e.getException());
 		}
 		super.destroy();
 	}
 
 	@Override
 	public void stop() {
 		System.out.println("Desktop Sharing Applet Stopping");
 		if (clientStarted) {
 			client.stop();	
 		}
 		
 		super.stop();
 	}
 	
 	public void onClientStop(ExitCode reason) {
 		// determine if client is disconnected _PTS_272_
 		if ( ExitCode.CONNECTION_TO_DESKSHARE_SERVER_DROPPED == reason ){
 			JFrame pframe = new JFrame("Desktop Sharing Disconneted");
 			if ( null != pframe ){
 				client.disconnected();
 				JOptionPane.showMessageDialog(pframe,
 					"Disconnected. Reason: Lost connection to the server." + reason ,
 					"Disconnected" ,JOptionPane.ERROR_MESSAGE );
 			}else{
 				System.out.println("Desktop sharing allocate memory failed.");
 			}
 		}else{
 			client.stop();
 		}	
 	}
 	
 }
