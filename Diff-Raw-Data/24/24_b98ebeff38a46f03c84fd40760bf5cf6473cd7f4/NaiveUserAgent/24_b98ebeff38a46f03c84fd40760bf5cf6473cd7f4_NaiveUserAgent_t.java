 /*
  * NaiveUserAgent.java
  * Copyright (c) 2004, 2005 Torbjrn Gannholm
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
  *
  */
 package org.xhtmlrenderer.swing;
 
 import org.xhtmlrenderer.resource.CSSResource;
 import org.xhtmlrenderer.resource.ImageResource;
 import org.xhtmlrenderer.resource.XMLResource;
 import org.xhtmlrenderer.util.GraphicsUtil;
 import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.Uu;
 
 import javax.imageio.ImageIO;
 import java.awt.*;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 
 
 /**
  * @author Torbjrn Gannholm
  */
 public class NaiveUserAgent implements org.xhtmlrenderer.extend.UserAgentCallback {
 
 
     /**
      * an LRU cache
      */
     private int imageCacheCapacity = 16;
     private java.util.LinkedHashMap imageCache =
             new java.util.LinkedHashMap(imageCacheCapacity, 0.75f, true) {
                 protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
                     return size() > imageCacheCapacity;
                 }
             };
 
     private String baseURL;
 
     /**
      * Creates a new instance of NaiveUserAgent
      */
     public NaiveUserAgent() {
     }
 
     /**
      * Gets a Reader for the resource identified
      *
      * @param uri PARAM
      * @return The stylesheet value
      */
     //TOdO:implement this with nio.
     private InputStream getInputStream(String uri) {
         java.io.InputStream is = null;
         uri = resolveURI(uri);
         try {
             is = new URL(uri).openStream();
         } catch (java.net.MalformedURLException e) {
             XRLog.exception("bad URL given: " + uri, e);
         } catch (java.io.IOException e) {
             XRLog.exception("IO problem for " + uri, e);
         }
         return is;
     }
 
 
     public CSSResource getCSSResource(String uri) {
         return new CSSResource(getInputStream(uri));
     }
 
     public ImageResource getImageResource(String uri) {
         ImageResource ir = null;
		System.out.println("here");
		Uu.p("getImageResource() called on: " + uri);
         uri = resolveURI(uri);
		Uu.p("resolved URI to: " + uri);
         ir = (ImageResource) imageCache.get(uri);
         //TODO: check that cached image is still valid
         if (ir == null) {
             InputStream is = getInputStream(uri);
             if (is != null) {
                 try {
                     Image img = ImageIO.read(is);
                     img = GraphicsUtil.cleanImage(img);
                     ir = new ImageResource(img);
                     imageCache.put(uri, ir);
                 } catch (IOException e) {
                     XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                 }
             }
         }
         if (ir == null) ir = new ImageResource(null);
         return ir;
     }
 
     public XMLResource getXMLResource(String uri) {
         return XMLResource.load(getInputStream(uri));
     }
 
 
     /**
      * Gets the visited attribute of the NaiveUserAgent object
      *
      * @param uri PARAM
      * @return The visited value
      */
     public boolean isVisited(String uri) {
         return false;
     }
 
     public void setBaseURL(String url) {
         baseURL = url;
     }
 
     public String resolveURI(String uri) {
         URL result;
         try {
             result = new URL(uri);
         } catch (MalformedURLException e) {
             try {
                 result = new URL(new URL(baseURL), uri);
             } catch (MalformedURLException e1) {
                 return null;
             }
         }
         return result.toString();
 
     }
 
     public String getBaseURL() {
         return baseURL;
     }
 }
 
 /*
  * $Id$
  *
  * $Log$
 * Revision 1.18  2005/07/31 18:06:14  joshy
 * debugging
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
  * Revision 1.17  2005/06/25 19:27:47  tobega
  * UAC now supplies Resources
  *
  * Revision 1.16  2005/06/25 17:23:35  tobega
  * first refactoring of UAC: ImageResource
  *
  * Revision 1.15  2005/06/21 17:52:10  joshy
  * new hover code
  * removed some debug statements
  * Issue number:
  * Obtained from:
  * Submitted by:
  * Reviewed by:
  *
  * Revision 1.14  2005/06/20 23:45:56  joshy
  * hack to fix the mangled background images on osx
  * Issue number:
  * Obtained from:
  * Submitted by:
  * Reviewed by:
  *
  * Revision 1.13  2005/06/20 17:26:45  joshy
  * debugging for image issues
  * font scale stuff
  *
  * Issue number:
  * Obtained from:
  * Submitted by:
  * Reviewed by:
  *
  * Revision 1.12  2005/06/15 11:57:18  tobega
  * Making Browser a better model application with UserAgentCallback
  *
  * Revision 1.11  2005/06/15 11:53:47  tobega
  * Changed UserAgentCallback to getInputStream instead of getReader. Fixed up some consequences of previous change.
  *
  * Revision 1.10  2005/06/13 06:50:16  tobega
  * Fixed a bug in table content resolution.
  * Various "tweaks" in other stuff.
  *
  * Revision 1.9  2005/06/03 00:29:49  tobega
  * fixed potential bug
  *
  * Revision 1.8  2005/06/01 21:36:44  tobega
  * Got image scaling working, and did some refactoring along the way
  *
  * Revision 1.7  2005/03/28 14:24:22  pdoubleya
  * Remove stack trace on loading images.
  *
  * Revision 1.6  2005/02/02 12:14:01  pdoubleya
  * Clean, format, buffer reader.
  *
  *
  */
 
