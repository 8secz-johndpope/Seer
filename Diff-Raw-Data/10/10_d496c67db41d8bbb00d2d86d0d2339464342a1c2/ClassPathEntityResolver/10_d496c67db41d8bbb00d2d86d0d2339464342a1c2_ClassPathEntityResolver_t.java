 /* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
    modeler, Finit element mesher, Plugin architecture.
  
 	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
  
 	This library is free software; you can redistribute it and/or
 	modify it under the terms of the GNU Lesser General Public
 	License as published by the Free Software Foundation; either
 	version 2.1 of the License, or (at your option) any later version.
  
 	This library is distributed in the hope that it will be useful,
 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 	Lesser General Public License for more details.
  
 	You should have received a copy of the GNU Lesser General Public
 	License along with this library; if not, write to the Free Software
 	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package org.jcae.mesh.xmldata;
 
 import java.io.IOException;
import java.io.StringReader;
 import java.net.*;
 import org.apache.log4j.Logger;
 import org.xml.sax.*;
 import java.io.*;
 /**
  * @author Jerome Robert
  */
 public class ClassPathEntityResolver implements EntityResolver
 {
 	private static Logger logger=Logger.getLogger(ClassPathEntityResolver.class);
 	
 	public InputSource resolveEntity(String publicId, String systemId)
 		throws SAXException, IOException
 	{
 		try
 		{
 			URI uri=new URI(systemId);
 			if(uri.getScheme().equals("classpath"))
 			{				
 				String path=uri.getPath();
 				//remove leading "/"
 				path=path.substring(1);
 				logger.debug("resolve "+systemId+" from CLASSPATH at "+path);
 				InputStream in= ClassLoader.getSystemResourceAsStream(path);
				if(in==null)
				{
					System.err.println("WARNING: "+systemId+" not found");
					return new InputSource(new StringReader(""));
				}
				else
					return new InputSource(in);				
 			}
 			else return null;
 		}
 		catch(URISyntaxException ex)
 		{
 			ex.printStackTrace();
 			return null;
 		}
 	}	
 }
