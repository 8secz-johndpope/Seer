 /*
  Copyright (c) 2000-2005 University of Washington.  All rights reserved.
 
  Redistribution and use of this distribution in source and binary forms,
  with or without modification, are permitted provided that:
 
    The above copyright notice and this permission notice appear in
    all copies and supporting documentation;
 
    The name, identifiers, and trademarks of the University of Washington
    are not used in advertising or publicity without the express prior
    written permission of the University of Washington;
 
    Recipients acknowledge that this distribution is made available as a
    research courtesy, "as is", potentially with defects, without
    any obligation on the part of the University of Washington to
    provide support, services, or repair;
 
    THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
    IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
    ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
    PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
    WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
    DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
    PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
    NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
    THE USE OR PERFORMANCE OF THIS SOFTWARE.
  */
 /* **********************************************************************
     Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.
 
     Redistribution and use of this distribution in source and binary forms,
     with or without modification, are permitted provided that:
        The above copyright notice and this permission notice appear in all
         copies and supporting documentation;
 
         The name, identifiers, and trademarks of Rensselaer Polytechnic
         Institute are not used in advertising or publicity without the
         express prior written permission of Rensselaer Polytechnic Institute;
 
     DISCLAIMER: The software is distributed" AS IS" without any express or
     implied warranty, including but not limited to, any implied warranties
     of merchantability or fitness for a particular purpose or any warrant)'
     of non-infringement of any current or pending patent rights. The authors
     of the software make no representations about the suitability of this
     software for any particular purpose. The entire risk as to the quality
     and performance of the software is with the user. Should the software
     prove defective, the user assumes the cost of all necessary servicing,
     repair or correction. In particular, neither Rensselaer Polytechnic
     Institute, nor the authors of the software are liable for any indirect,
     special, consequential, or incidental damages related to the software,
     to the maximum extent the law permits.
 */
 
 package org.bedework.webadmin.timezones;
 
 import org.bedework.appcommon.TimeZonesParser;
 import org.bedework.calsvci.CalSvcI;
 import org.bedework.webadmin.PEAbstractAction;
 import org.bedework.webadmin.PEActionForm;
 import org.bedework.webcommon.BwSession;
 
 import java.util.Collection;
 import java.util.Iterator;
 import javax.servlet.http.HttpServletRequest;
 
 import org.apache.struts.upload.FormFile;
 
 /** This action imports the uploaded system timezone definitions.
  *
  * <p>Forwards to:<ul>
  *      <li>"noAccess"     user not authorised.</li>
 *      <li>"error"        some sort of fatal error.</li>
 *      <li>"failed"       for some reason.</li>
  *      <li>"success"      processed.</li>
  * </ul>
  *
  * @author Mike Douglass   douglm@rpi.edu
  */
 public class PEUploadTimezonesAction extends PEAbstractAction {
   /* (non-Javadoc)
    * @see org.bedework.webadmin.PEAbstractAction#doAction(javax.servlet.http.HttpServletRequest, org.bedework.webcommon.BwSession, org.bedework.webadmin.PEActionForm)
    */
   public String doAction(HttpServletRequest request,
                          BwSession sess,
                          PEActionForm form) throws Throwable {
     /** Check access
      */
     if (!form.getAuthorisedUser()) {
       return "noAccess";
     }
 
     FormFile upFile = form.getUploadFile();
 
     if (upFile == null) {
       // Just forget it
       return "success";
     }
 
     TimeZonesParser tzp = new TimeZonesParser(upFile.getInputStream(), debug);
 
    Collection tzis = null;
    try {
      tzis = tzp.getTimeZones();
    } catch (Throwable t) {
      // XXX Could probably do better figuring out what the error is
      form.getErr().emit("org.bedework.error.timezones.readerror");
      return "failed";
    }
 
     CalSvcI svci = form.fetchSvci();
 
     Iterator it = tzis.iterator();
     while (it.hasNext()) {
       TimeZonesParser.TimeZoneInfo tzi = (TimeZonesParser.TimeZoneInfo)it.next();
 
       svci.saveTimeZone(tzi.tzid, tzi.timezone);
     }
 
     form.getMsg().emit("org.bedework.client.message.timezones.imported");
 
     return "success";
   }
 }
 
