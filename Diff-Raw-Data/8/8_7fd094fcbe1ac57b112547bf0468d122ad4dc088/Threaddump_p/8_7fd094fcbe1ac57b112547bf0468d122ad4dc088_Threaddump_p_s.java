 // Threaddump_p.java 
 // -----------------------
 // part of YACY
 // (C) by Michael Peter Christen; mc@anomic.de
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2004
 //
 // This File is contributed by Alexander Fieger
 //
 // $LastChangedDate: 2008-01-22 12:51:43 +0100 (Di, 22 Jan 2008) $
 // $LastChangedRevision: 4374 $
 // $LastChangedBy: low012 $
 //
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 //
 // Using this software in any meaning (reading, learning, copying, compiling,
 // running) means that you agree that the Author(s) is (are) not responsible
 // for cost, loss of data or any harm that may be caused directly or indirectly
 // by usage of this softare or this documentation. The usage of this software
 // is on your own risk. The installation and usage (starting/running) of this
 // software may allow other people or application to access your computer and
 // any attached devices and is highly dependent on the configuration of the
 // software which must be done by the user of the software; the author(s) is
 // (are) also not responsible for proper configuration and usage of the
 // software, even if provoked by documentation provided together with
 // the software.
 //
 // Any changes to this file according to the GPL as documented in the file
 // gpl.txt aside this file in the shipment you received can be done to the
 // lines that follows this copyright notice here, but changes must not be
 // done inside the copyright notive above. A re-distribution must contain
 // the intact and unchanged copyright notice.
 // Contributions and changes to the program code must be marked as such.
 
 // You must compile this file with
 // javac -classpath .:../Classes Blacklist_p.java
 // if the shell's current path is HTROOT
 
 import de.anomic.http.httpHeader;
 import de.anomic.plasma.plasmaSwitchboard;
 import de.anomic.server.serverObjects;
 import de.anomic.server.serverSwitch;
 import de.anomic.yacy.yacyVersion;
 
 import java.util.Map;
 import java.lang.StringBuffer;
import java.util.Iterator;
 import java.util.Date;
 
 public class Threaddump_p {
 
 	private static serverObjects prop;
 	private static plasmaSwitchboard sb;
 
     public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch env) {
 
    	
     	prop = new serverObjects();
     	sb = (plasmaSwitchboard) env;
     	StringBuffer buffer = new StringBuffer();
     	
     	
     	if (post != null && post.containsKey("createThreaddump")) {
     	    prop.put("dump", "1");
         	// Thread dump
         	Map<Thread,StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
         	Date dt = new Date();
         	String versionstring = yacyVersion.combined2prettyVersion(sb.getConfig("version","0.1"));
         	
         	buffer.append("************* Start Thread Dump " + dt + " *******************").append("<br />");
             buffer.append("<br /> YaCy Version: " + versionstring + "<br />");
         	buffer.append("Total Memory = " + (Runtime.getRuntime().totalMemory())).append("<br />");
         	buffer.append("Used Memory = " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())).append("<br />");
         	buffer.append("Free Memory = " + (Runtime.getRuntime().freeMemory())).append("<br />");
         	buffer.append(" --- --- --- --- <br />");
         	for (Thread thread: stackTraces.keySet()) {
         	    StackTraceElement[] stackTraceElement = (StackTraceElement[]) stackTraces.get(thread);
         	    buffer.append("Thread= " + thread.getName() + " " + (thread.isDaemon()?"daemon":"") + " id=" + thread.getId() + " " + thread.getState()).append("<br />");
         	    for(int i = 0; i <= (stackTraceElement.length -1); i++)	{
         	        buffer.append(stackTraceElement[i]).append("<br />");
         	    }
         	    buffer.append("<br />");
         	}
         	buffer.append("************* End Thread Dump " + dt + " *******************").append("<br />");       	
         
         	prop.put("dump_content", buffer.toString());
     	} else {
     	    prop.put("dump", "0");
     	}
     	
        	return prop;    // return from serverObjects respond()
     }    
     
 
 }
