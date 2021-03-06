 /* ********************************************************************
     Licensed to Jasig under one or more contributor license
     agreements. See the NOTICE file distributed with this work
     for additional information regarding copyright ownership.
     Jasig licenses this file to you under the Apache License,
     Version 2.0 (the "License"); you may not use this file
     except in compliance with the License. You may obtain a
     copy of the License at:
 
     http://www.apache.org/licenses/LICENSE-2.0
 
     Unless required by applicable law or agreed to in writing,
     software distributed under the License is distributed on
     an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     KIND, either express or implied. See the License for the
     specific language governing permissions and limitations
     under the License.
 */
 package org.bedework.caldav.server;
 
 import org.bedework.caldav.server.CaldavBwNode.PropertyTagXrdEntry;
 import org.bedework.caldav.server.calquery.CalData;
 import org.bedework.caldav.server.calquery.FreeBusyQuery;
 import org.bedework.caldav.server.filter.FilterHandler;
 import org.bedework.caldav.server.get.FreeBusyGetHandler;
 import org.bedework.caldav.server.get.GetHandler;
 import org.bedework.caldav.server.get.IscheduleGetHandler;
 import org.bedework.caldav.server.get.WebcalGetHandler;
 import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
 import org.bedework.caldav.server.sysinterface.RetrievalMode;
 import org.bedework.caldav.server.sysinterface.SysIntf;
 import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
 import org.bedework.caldav.server.sysinterface.SysIntf.SynchReportData;
 import org.bedework.caldav.server.sysinterface.SysIntf.SynchReportData.SynchReportDataItem;
 
 import edu.rpi.cct.webdav.servlet.common.AccessUtil;
 import edu.rpi.cct.webdav.servlet.common.Headers;
 import edu.rpi.cct.webdav.servlet.common.Headers.IfHeaders;
 import edu.rpi.cct.webdav.servlet.common.MethodBase.MethodInfo;
 import edu.rpi.cct.webdav.servlet.common.WebdavServlet;
 import edu.rpi.cct.webdav.servlet.common.WebdavUtils;
 import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
 import edu.rpi.cct.webdav.servlet.shared.WdEntity;
 import edu.rpi.cct.webdav.servlet.shared.WdSynchReport;
 import edu.rpi.cct.webdav.servlet.shared.WdSynchReport.WdSynchReportItem;
 import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
 import edu.rpi.cct.webdav.servlet.shared.WebdavException;
 import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
 import edu.rpi.cct.webdav.servlet.shared.WebdavNotFound;
 import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
 import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
 import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
 import edu.rpi.cct.webdav.servlet.shared.WebdavPrincipalNode;
 import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
 import edu.rpi.cct.webdav.servlet.shared.WebdavServerError;
 import edu.rpi.cct.webdav.servlet.shared.WebdavUnauthorized;
 import edu.rpi.cct.webdav.servlet.shared.WebdavUnsupportedMediaType;
 import edu.rpi.cmt.access.AccessException;
 import edu.rpi.cmt.access.AccessPrincipal;
 import edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb;
 import edu.rpi.cmt.access.Ace;
 import edu.rpi.cmt.access.AceWho;
 import edu.rpi.cmt.access.Acl;
 import edu.rpi.cmt.access.PrivilegeDefs;
 import edu.rpi.cmt.access.WhoDefs;
 import edu.rpi.sss.util.Util;
 import edu.rpi.sss.util.xml.XmlEmit;
 import edu.rpi.sss.util.xml.XmlEmit.NameSpace;
 import edu.rpi.sss.util.xml.XmlUtil;
 import edu.rpi.sss.util.xml.tagdefs.AppleIcalTags;
 import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
 import edu.rpi.sss.util.xml.tagdefs.CalWSXrdDefs;
 import edu.rpi.sss.util.xml.tagdefs.CaldavDefs;
 import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
 import edu.rpi.sss.util.xml.tagdefs.WebdavTags;
 import edu.rpi.sss.util.xml.tagdefs.XcalTags;
 import edu.rpi.sss.util.xml.tagdefs.XrdTags;
 import edu.rpi.sss.util.xml.tagdefs.XsiTags;
 
 import org.oasis_open.docs.ns.xri.xrd_1.AnyURI;
 import org.oasis_open.docs.ns.xri.xrd_1.LinkType;
 import org.oasis_open.docs.ns.xri.xrd_1.XRDType;
 import org.oasis_open.docs.ws_calendar.ns.soap.GetPropertiesBasePropertyType;
 import org.w3c.dom.Element;
 
 import ietf.params.xml.ns.caldav.FilterType;
 import ietf.params.xml.ns.icalendar_2.IcalendarType;
 
 import java.io.CharArrayReader;
 import java.io.InputStream;
 import java.io.Reader;
 import java.io.Serializable;
 import java.net.URI;
 import java.net.URLDecoder;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeSet;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.Marshaller;
 import javax.xml.namespace.QName;
 
 /** This class implements a namespace interface for the webdav abstract
  * servlet. One of these interfaces is associated with each current session.
  *
  * <p>As a first pass we'll define webdav urls as starting with <br/>
  * /user/user-name/calendar-name/<br/>
  *
  * <p>uri resolution should be made part of the core calendar allowing all
  * such distinctions to be removed from this code.
  *
  * <p>The part following the above prefix probably determines exactly what is
  * delivered. We may want the entire calendar (or what we show by default) or
  * a single event from the calendar
  *
  *   @author Mike Douglass   douglm @ rpi.edu
  */
 public class CaldavBWIntf extends WebdavNsIntf {
   /** Namespace prefix based on the request url.
    */
   private String namespacePrefix;
 
   private AccessUtil accessUtil;
 
   /** Namespace based on the request url.
    */
   @SuppressWarnings("unused")
   private String namespace;
 
   SysIntf sysi;
 
   /* true if this is a CalWS server */
   private boolean calWs;
 
   /* The bedework end of the synch service. This is a web
     service called by the synch engine to get information out of
     bedework and to update events and status.
 
     This path should probably be restricted to a given host only.
 
     Coming up on a separate port might help to lock it down.
    */
   private String synchWsURI;
 
   /* ====================================================================
    *                     Interface methods
    * ==================================================================== */
 
   /** Called before any other method is called to allow initialization to
    * take place at the first or subsequent requests
    *
    * @param servlet
    * @param req
    * @param methods    HashMap   table of method info
    * @param dumpContent
    * @throws WebdavException
    */
   @Override
   public void init(final WebdavServlet servlet,
                    final HttpServletRequest req,
                    final HashMap<String, MethodInfo> methods,
                    final boolean dumpContent) throws WebdavException {
     try {
       // Needed before any other initialization
       calWs = Boolean.parseBoolean(servlet.getInitParameter("calws"));
       synchWsURI = servlet.getInitParameter("synchWsURI");
       sysi = getSysi(servlet.getInitParameter("sysintfImpl"));
 
       super.init(servlet, req, methods, dumpContent);
 
       namespacePrefix = WebdavUtils.getUrlPrefix(req);
       namespace = namespacePrefix + "/schema";
 
       sysi.init(req, account, false, calWs);
 
       accessUtil = new AccessUtil(namespacePrefix, xml,
                                   new CalDavAccessXmlCb(sysi));
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /** See if we can reauthenticate. Use for real-time service which needs to
    * authenticate as a particular principal.
    *
    * @param req
    * @param account
    * @param service - true if this is a service call - e.g. iSchedule -
    *                rather than a real user.
    * @throws WebdavException
    */
   public void reAuth(final HttpServletRequest req,
                      final String account,
                      final boolean service) throws WebdavException {
     try {
       if (sysi != null) {
         try {
           sysi.close();
         } catch (Throwable t) {
           throw new WebdavException(t);
         }
       } else {
         sysi = getSysi(servlet.getInitParameter("sysintfImpl"));
       }
 
       this.account = account;
 
       sysi.init(req, account, service, calWs);
 
       accessUtil = new AccessUtil(namespacePrefix, xml,
                                   new CalDavAccessXmlCb(sysi));
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /**
    * @return boolean
    */
   public boolean getCalWS() {
     return calWs;
   }
 
   /** Get the synch web service uri - null for no service
    *
    * @return String
    */
   public String getSynchWsURI() {
     return synchWsURI;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getDavHeader(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public String getDavHeader(final WebdavNsNode node) throws WebdavException {
     if (account == null) {
       return super.getDavHeader(node) + ", calendar-access";
     }
 
     return super.getDavHeader(node) +
         ", calendar-access" +
         ", calendar-schedule" +
         ", calendar-auto-schedule" +
         ", calendar-default-alarms" +
         ", calendarserver-sharing";
   }
 
   @Override
   public void emitError(final QName errorTag, final String extra,
                         final XmlEmit xml) throws Throwable {
     if (errorTag.equals(CaldavTags.noUidConflict)) {
       xml.openTag(errorTag);
       if (extra != null) {
         xml.property(WebdavTags.href, sysi.getUrlHandler().prefix(extra));
       }
       xml.closeTag(errorTag);
     } else {
       super.emitError(errorTag, extra, xml);
     }
   }
 
   /**
    */
   private static class CalDavAccessXmlCb implements AccessXmlCb, Serializable {
     private SysIntf sysi;
 
     private QName errorTag;
     private String errorMsg;
 
     CalDavAccessXmlCb(final SysIntf sysi) {
       this.sysi = sysi;
     }
 
     /* (non-Javadoc)
      * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#makeHref(java.lang.String, int)
      */
     @Override
     public String makeHref(final String id, final int whoType) throws AccessException {
       try {
         return sysi.makeHref(id, whoType);
       } catch (Throwable t) {
         throw new AccessException(t);
       }
     }
 
     @Override
     public AccessPrincipal getPrincipal() throws AccessException {
       try {
         return sysi.getPrincipal();
       } catch (Throwable t) {
         throw new AccessException(t);
       }
     }
 
     @Override
     public AccessPrincipal getPrincipal(final String href) throws AccessException {
       try {
         return sysi.getPrincipal(sysi.getUrlHandler().unprefix(href));
       } catch (Throwable t) {
         throw new AccessException(t);
       }
     }
 
     /* (non-Javadoc)
      * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#setErrorTag(edu.rpi.sss.util.xml.QName)
      */
     @Override
     public void setErrorTag(final QName tag) throws AccessException {
       errorTag = tag;
     }
 
     /* (non-Javadoc)
      * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#getErrorTag()
      */
     @Override
     public QName getErrorTag() throws AccessException {
       return errorTag;
     }
 
     /* (non-Javadoc)
      * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#setErrorMsg(java.lang.String)
      */
     @Override
     public void setErrorMsg(final String val) throws AccessException {
       errorMsg = val;
     }
 
     /* (non-Javadoc)
      * @see edu.rpi.cmt.access.AccessXmlUtil.AccessXmlCb#getErrorMsg()
      */
     @Override
     public String getErrorMsg() throws AccessException {
       return errorMsg;
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getAccessUtil()
    */
   @Override
   public AccessUtil getAccessUtil() throws WebdavException {
     return accessUtil;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#canPut(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public boolean canPut(final WebdavNsNode node) throws WebdavException {
     CalDAVEvent ev = null;
 
     if (node instanceof CaldavComponentNode) {
       CaldavComponentNode comp = (CaldavComponentNode)node;
       ev = comp.getEvent();
     } else if (!(node instanceof CaldavResourceNode)) {
       return false;
     }
 
     if (ev != null) {
       return sysi.checkAccess(ev,
                               PrivilegeDefs.privWriteContent,
                               true).getAccessAllowed();
     } else {
       return sysi.checkAccess(node.getCollection(true), // deref - we're trying to put into the target
                               PrivilegeDefs.privBind, true).getAccessAllowed();
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getDirectoryBrowsingDisallowed()
    */
   @Override
   public boolean getDirectoryBrowsingDisallowed() throws WebdavException {
     return sysi.getSystemProperties().getDirectoryBrowsingDisallowed();
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#rollback()
    */
   @Override
   public void rollback() {
     sysi.rollback();
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#close()
    */
   @Override
   public void close() throws WebdavException {
     sysi.close();
   }
 
   /**
    * @return SysIntf
    */
   public SysIntf getSysi() {
     return sysi;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getSupportedLocks()
    */
   @Override
   public String getSupportedLocks() {
     return null; // No locks
     /*
      return  "<DAV:lockentry>" +
              "  <DAV:lockscope>" +
              "    <DAV:exclusive/>" +
              "  </DAV:lockscope>" +
              "  <DAV:locktype><DAV:write/></DAV:locktype>" +
              "</DAV:lockentry>";
              */
   }
 
   @Override
   public boolean getAccessControl() {
     return true;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#addNamespace(edu.rpi.sss.util.xml.XmlEmit)
    */
   @Override
   public void addNamespace(final XmlEmit xml) throws WebdavException {
     try {
       if (calWs) {
         xml.addNs(new NameSpace(CalWSXrdDefs.namespace, "CalWS"), true);
         xml.addNs(new NameSpace(XsiTags.namespace, "xsi"), false);
         xml.addNs(new NameSpace(XrdTags.namespace, "xrd"), false);
 
         // Need these for the time being
         xml.addNs(new NameSpace(WebdavTags.namespace, "DAV"), false);
         xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), false);
 
         return;
       }
 
       super.addNamespace(xml);
 
       xml.addNs(new NameSpace(CaldavDefs.caldavNamespace, "C"), true);
       xml.addNs(new NameSpace(AppleIcalTags.appleIcalNamespace, "AI"), false);
       xml.addNs(new NameSpace(CaldavDefs.icalNamespace, "ical"), false);
       xml.addNs(new NameSpace(AppleServerTags.appleCaldavNamespace, "CS"), false);
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getNode(java.lang.String, int, int)
    */
   @Override
   public WebdavNsNode getNode(final String uri,
                               final int existance,
                               final int nodeType) throws WebdavException {
     return getNodeInt(uri, existance, nodeType, null, null, null);
   }
 
   @Override
   public void putNode(final WebdavNsNode node)
       throws WebdavException {
   }
 
   @Override
   public void delete(final WebdavNsNode node) throws WebdavException {
     try {
       if (node instanceof CaldavResourceNode) {
         CaldavResourceNode rnode = (CaldavResourceNode)node;
 
         sysi.deleteFile(rnode.getResource());
       } else if (node instanceof CaldavComponentNode) {
         CaldavComponentNode cnode = (CaldavComponentNode)node;
 
         CalDAVEvent ev = cnode.getEvent();
 
         if (ev != null) {
           if (debug) {
             trace("About to delete event " + ev);
           }
           sysi.deleteEvent(ev, CalDavHeaders.scheduleReply(getRequest()));
         } else {
           if (debug) {
             trace("No event object available");
           }
         }
       } else {
         if (!(node instanceof CaldavCalNode)) {
           throw new WebdavUnauthorized();
         }
 
         CaldavCalNode cnode = (CaldavCalNode)node;
 
         CalDAVCollection col = (CalDAVCollection)cnode.getCollection(false); // Don't deref for delete
 
         sysi.deleteCollection(col);
       }
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getChildren(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public Collection<WebdavNsNode> getChildren(final WebdavNsNode node) throws WebdavException {
     try {
       ArrayList<WebdavNsNode> al = new ArrayList<WebdavNsNode>();
 
       if (!node.isCollection()) {
         // Don't think we should have been called
         return al;
       }
 
       if (debug) {
         debugMsg("About to get children for " + node.getUri());
       }
 
       Collection<? extends WdEntity> children = node.getChildren();
 
       if (children == null) {
         // Perhaps no access
         return al;
       }
 
       String uri = node.getUri();
       CalDAVCollection parent = (CalDAVCollection)node.getCollection(false);  // don't deref
 
       for (WdEntity wde: children) {
         CalDAVCollection col = null;
         CalDAVResource r = null;
         CalDAVEvent ev = null;
 
         String name = wde.getName();
         int nodeType;
 
         if (wde instanceof CalDAVCollection) {
           col = (CalDAVCollection)wde;
 
           nodeType = WebdavNsIntf.nodeTypeCollection;
           if (debug) {
             debugMsg("Found child " + col);
           }
         } else if (wde instanceof CalDAVResource) {
           col = parent;
           r = (CalDAVResource)wde;
 
           nodeType = WebdavNsIntf.nodeTypeEntity;
         } else if (wde instanceof CalDAVEvent) {
           col = parent;
           ev = (CalDAVEvent)wde;
 
           nodeType = WebdavNsIntf.nodeTypeEntity;
         } else {
           throw new WebdavException("Unexpected return type");
         }
 
         al.add(getNodeInt(Util.buildPath(true, uri, "/", name),
                           WebdavNsIntf.existanceDoesExist,
                           nodeType, col, ev, r));
       }
 
       return al;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   @Override
   public WebdavNsNode getParent(final WebdavNsNode node)
       throws WebdavException {
     return null;
   }
 
   @Override
   public boolean prefetch(final HttpServletRequest req,
                           final HttpServletResponse resp,
                           final WebdavNsNode node) throws WebdavException {
     if (!super.prefetch(req, resp, node)) {
       return false;
     }
 
     if (calWs) {
       return true;
     }
 
     if (!(node instanceof CaldavComponentNode)) {
       return true;
     }
 
     CaldavComponentNode cnode = (CaldavComponentNode)node;
 
     if (!cnode.getEvent().getOrganizerSchedulingObject() &&
         !cnode.getEvent().getAttendeeSchedulingObject()) {
       return true;
     }
 
     // Add the schedule tag header
 
     resp.setHeader("Schedule-Tag", cnode.getStagValue());
 
     return true;
   }
 
   @Override
   public Content getContent(final HttpServletRequest req,
                             final HttpServletResponse resp,
                             final String contentType,
                             final WebdavNsNode node) throws WebdavException {
     try {
       String ctype = contentType;
 
       if (ctype == null) {
         String accept = req.getHeader("ACCEPT");
 
         if (accept != null) {
           ctype = accept.trim();
         }
       }
 
       if (node.isCollection()) {
         if ((ctype == null) || (ctype.indexOf("text/html") >= 0)) {
           if (getDirectoryBrowsingDisallowed()) {
             throw new WebdavException(HttpServletResponse.SC_FORBIDDEN);
           }
 
           Content c = new Content();
 
           String content = generateHtml(req, node);
           c.rdr = new CharArrayReader(content.toCharArray());
           c.contentType = "text/html";
           c.contentLength = content.getBytes().length;
 
           return c;
         }
       }
 
       /* ===================  Try for XRD fetch ======================= */
 
       if (calWs && (ctype != null) &&
           "application/xrd+xml".equals(ctype)) {
         return doXrd(req, resp, (CaldavBwNode)node);
       }
 
       /* ===================  Try for calendar fetch ======================= */
 
       if (node.isCollection() && (ctype != null) &&
           "text/calendar".equals(ctype)) {
         GetHandler handler = new WebcalGetHandler(this);
         RequestPars pars = new RequestPars(req, this, getResourceUri(req));
 
         pars.webcalGetAccept = true;
 
         handler.process(req, resp, pars);
 
         Content c = new Content();
 
         c.written = true; // set content to say it's done
 
         return c;
       }
 
       if (node.isCollection()) {
         return null;
       }
 
       if (!node.getAllowsGet()) {
         return null;
       }
 
       if ((ctype == null) ||
           (!ctype.equals("text/calendar") &&
            !ctype.equals(XcalTags.mimetype))) {
         ctype = sysi.getDefaultContentType();
       }
 
       resp.setContentType(ctype + "; charset=UTF-8");
 
       Content c = new Content();
       c.written = true;
 
       c.contentType = node.writeContent(null, resp.getWriter(), ctype);
 
       if (c.contentType.indexOf(';') < 0) {
         // No charset
         c.contentType += "; charset=UTF-8";
       }
 
       return c;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getBinaryContent(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public Content getBinaryContent(final WebdavNsNode node) throws WebdavException {
     try {
       if (!node.getAllowsGet()) {
         return null;
       }
 
       if (!(node instanceof CaldavResourceNode)) {
         throw new WebdavException("Unexpected node type");
       }
 
       CaldavResourceNode bwnode = (CaldavResourceNode)node;
 
       Content c = new Content();
 
       c.stream = bwnode.getContentStream();
       c.contentType = node.getContentType();
       c.contentLength = node.getContentLen();
 
       return c;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#putContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, java.lang.String[], java.io.Reader, edu.rpi.cct.webdav.servlet.common.Headers.IfHeaders)
    */
   @Override
   public PutContentResult putContent(final HttpServletRequest req,
                                      final HttpServletResponse resp,
                                      final WebdavNsNode node,
                                      final String[] contentTypePars,
                                      final Reader contentRdr,
                                      final IfHeaders ifHeaders) throws WebdavException {
     try {
       PutContentResult pcr = new PutContentResult();
       pcr.node = node;
 
       if (node instanceof CaldavResourceNode) {
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       CaldavComponentNode bwnode = (CaldavComponentNode)node;
       CalDAVCollection col = (CalDAVCollection)node.getCollection(true); // deref - put into target
 
       boolean calContent = false;
       if ((contentTypePars != null) && (contentTypePars.length > 0)) {
         calContent = contentTypePars[0].equals("text/calendar") ||
                      contentTypePars[0].equals("application/calendar+xml");
       }
 
       if ((col.getCalType() != CalDAVCollection.calTypeCalendarCollection) ||
           !calContent) {
         throw new WebdavForbidden(CaldavTags.supportedCalendarData);
       }
 
       /** We can only put a single resource - that resource will be an ics file
        * containing freebusy information or an event or todo and possible overrides.
        */
 
       pcr.created = putEvent(req, resp, bwnode,
                              contentRdr,
                              contentTypePars[0],
                              ifHeaders);
 
       return pcr;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   @Override
   public PutContentResult putBinaryContent(final HttpServletRequest req,
                                            final WebdavNsNode node,
                                            final String[] contentTypePars,
                                            final InputStream contentStream,
                                            final IfHeaders ifHeaders) throws WebdavException {
     try {
       PutContentResult pcr = new PutContentResult();
       pcr.node = node;
 
       if (!(node instanceof CaldavResourceNode)) {
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       CaldavResourceNode bwnode = (CaldavResourceNode)node;
       CalDAVCollection col = (CalDAVCollection)node.getCollection(true);
 
       if ((col == null) ||
           (col.getCalType() == CalDAVCollection.calTypeCalendarCollection)) {
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       CalDAVResource r = bwnode.getResource();
 
       if (r.isNew()) {
         ifHeaders.create = true;
       }
 
       String contentType = null;
 
       if ((contentTypePars != null) && (contentTypePars.length > 0)) {
         for (String c: contentTypePars) {
           if (c == null) {
             continue;
           }
 
           if (contentType != null) {
             contentType += ";" + c;
           } else {
             contentType = c;
           }
         }
       }
 
       r.setContentType(contentType);
       r.setBinaryContent(contentStream);
 
       if (ifHeaders.create) {
         sysi.putFile(col, r);
       } else {
         sysi.updateFile(r, true);
       }
       return pcr;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   private boolean putEvent(final HttpServletRequest req,
                            final HttpServletResponse resp,
                            final CaldavComponentNode bwnode,
                            final Reader contentRdr,
                            final String contentType,
                            final IfHeaders ifHeaders) throws WebdavException {
     String ifStag = Headers.ifScheduleTagMatch(req);
     boolean noInvites = req.getHeader("Bw-NoInvites") != null; // based on header?
 
     //BwEvent ev = evinfo.getEvent();
     String entityName = bwnode.getEntityName();
 
     CalDAVCollection col = (CalDAVCollection)bwnode.getCollection(true);
     boolean created = false;
 
     SysiIcalendar cal = sysi.fromIcal(col, contentRdr, contentType,
                                       IcalResultType.OneComponent,
                                       ifStag != null); // mergeAttendees
     if (cal.getMethod() != null) {
       throw new WebdavForbidden(CaldavTags.validCalendarObjectResource,
                                 "No method on PUT");
     }
 
     CalDAVEvent ev = (CalDAVEvent)cal.iterator().next();
 
     ev.setParentPath(col.getPath());
 
     if (entityName == null) {
       entityName = ev.getUid() + ".ics";
       bwnode.setEntityName(entityName);
     }
 
     if (debug) {
       debugMsg("putContent: intf has event with name " + entityName +
                " and summary " + ev.getSummary() +
                " new event = " + ev.isNew());
     }
 
     if (ev.isNew()) {
       created = true;
       ev.setName(entityName);
 
       /* Collection<BwEventProxy>failedOverrides = */
       sysi.addEvent(ev, noInvites, true);
 
       bwnode.setEvent(ev);
     } else if (ifHeaders.create) {
       /* Resource already exists */
 
       throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
     } else {
       if (!entityName.equals(ev.getName())) {
         /* Probably specifying a different uid */
         throw new WebdavForbidden(CaldavTags.noUidConflict);
       }
 
       if ((ifHeaders.ifEtag != null) &&
           (!ifHeaders.ifEtag.equals(bwnode.getPrevEtagValue(true)))) {
         if (debug) {
           debugMsg("putContent: etag mismatch if=" + ifHeaders.ifEtag +
                    "prev=" + bwnode.getPrevEtagValue(true));
         }
         rollback();
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       if ((ifStag != null) &&
           (!ifStag.equals(bwnode.getPrevStagValue()))) {
         if (debug) {
           debugMsg("putContent: stag mismatch if=" + ifStag +
                    "prev=" + bwnode.getPrevStagValue());
         }
         rollback();
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       if (debug) {
         debugMsg("putContent: update event " + ev);
       }
       sysi.updateEvent(ev);
 
       bwnode.setEvent(ev);
     }
 
     if (ev.getOrganizerSchedulingObject() ||
         ev.getAttendeeSchedulingObject()) {
       resp.setHeader("Schedule-Tag", ev.getScheduleTag());
     }
 
     return created;
   }
 
   /**
    * @param resp
    * @param bwnode
    * @param ical
    * @param create
    * @param noInvites
    * @param ifStag
    * @param ifEtag
    * @return true for OK
    * @throws WebdavException
    */
   public boolean putEvent(final HttpServletResponse resp,
                           final CaldavComponentNode bwnode,
                           final IcalendarType ical,
                           final boolean create,
                           final boolean noInvites,
                           final String ifStag,
                           final String ifEtag) throws WebdavException {
     String entityName = bwnode.getEntityName();
 
     CalDAVCollection col = (CalDAVCollection)bwnode.getCollection(true); // deref
     boolean created = false;
 
     SysiIcalendar cal = sysi.fromIcal(col, ical,
                                       IcalResultType.OneComponent);
     if (cal.getMethod() != null) {
       throw new WebdavForbidden(CaldavTags.validCalendarObjectResource,
                                 "No method on PUT");
     }
 
     CalDAVEvent ev = (CalDAVEvent)cal.iterator().next();
 
     ev.setParentPath(col.getPath());
 
     if (entityName == null) {
       entityName = ev.getUid() + ".ics";
       bwnode.setEntityName(entityName);
     }
 
     if (debug) {
       debugMsg("putContent: intf has event with name " + entityName +
                " and summary " + ev.getSummary() +
                " new event = " + ev.isNew());
     }
 
     if (ev.isNew()) {
       created = true;
       ev.setName(entityName);
 
       /* Collection<BwEventProxy>failedOverrides = */
       sysi.addEvent(ev, noInvites, true);
 
       bwnode.setEvent(ev);
     } else if (create) {
       /* Resource already exists */
 
       throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
     } else {
       if (!entityName.equals(ev.getName())) {
         /* Probably specifying a different uid */
         throw new WebdavForbidden(CaldavTags.noUidConflict);
       }
 
       if ((ifEtag != null) &&
           (!ifEtag.equals(bwnode.getPrevEtagValue(true)))) {
         if (debug) {
           debugMsg("putContent: etag mismatch if=" + ifEtag +
                    "prev=" + bwnode.getPrevEtagValue(true));
         }
         rollback();
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       if ((ifStag != null) &&
           (!ifStag.equals(bwnode.getPrevStagValue()))) {
         if (debug) {
           debugMsg("putContent: stag mismatch if=" + ifStag +
                    "prev=" + bwnode.getPrevStagValue());
         }
         rollback();
         throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED);
       }
 
       if (debug) {
         debugMsg("putContent: update event " + ev);
       }
       sysi.updateEvent(ev);
     }
 
     if (ev.getOrganizerSchedulingObject() ||
         ev.getAttendeeSchedulingObject()) {
       resp.setHeader("Schedule-Tag", ev.getScheduleTag());
     }
 
     return created;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#create(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public void create(final WebdavNsNode node) throws WebdavException {
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#createAlias(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public void createAlias(final WebdavNsNode alias) throws WebdavException {
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#acceptMkcolContent(javax.servlet.http.HttpServletRequest)
    */
   @Override
   public void acceptMkcolContent(final HttpServletRequest req) throws WebdavException {
     throw new WebdavUnsupportedMediaType();
   }
 
   /** Create an empty collection at the given location.
    *
    * <pre>
    *  201 (Created) - The calendar collection resource was created in its entirety.
    *  403 (Forbidden) - This indicates at least one of two conditions: 1) the
    *          server does not allow the creation of calendar collections at the
    *          given location in its namespace, or 2) the parent collection of the
    *          Request-URI exists but cannot accept members.
    *  405 (Method Not Allowed) - MKCALENDAR can only be executed on a null resource.
    *  409 (Conflict) - A collection cannot be made at the Request-URI until one
    *          or more intermediate collections have been created.
    *  415 (Unsupported Media Type)- The server does not support the request type
    *          of the body.
    *  507 (Insufficient Storage) - The resource does not have sufficient space
    *          to record the state of the resource after the execution of this method.
    *
    * @param req       HttpServletRequest
    * @param node             node to create
    * @throws WebdavException
    */
   @Override
   public void makeCollection(final HttpServletRequest req,
                              final HttpServletResponse resp,
                              final WebdavNsNode node) throws WebdavException {
     try {
       if (!(node instanceof CaldavCalNode)) {
         throw new WebdavBadRequest("Not a valid node object " +
                                    node.getClass().getName());
       }
 
       CaldavCalNode bwnode = (CaldavCalNode)node;
 
       /* The uri should have an entity name representing the new collection
        * and a collection object representing the parent.
        *
        * A namepart of null means that the path already exists
        */
 
       CalDAVCollection newCol = (CalDAVCollection)bwnode.getCollection(false); // No deref?
 
       CalDAVCollection parent = getSysi().getCollection(newCol.getParentPath());
       if (parent.getCalType() == CalDAVCollection.calTypeCalendarCollection) {
         throw new WebdavForbidden(CaldavTags.calendarCollectionLocationOk);
       }
 
       if (newCol.getName() == null) {
         throw new WebdavForbidden("Forbidden: Null name");
       }
 
       resp.setStatus(sysi.makeCollection(newCol));
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   @Override
   public void copyMove(final HttpServletRequest req,
                        final HttpServletResponse resp,
                        final WebdavNsNode from,
                        final WebdavNsNode to,
                        final boolean copy,
                        final boolean overwrite,
                        final int depth) throws WebdavException {
     if (from instanceof CaldavCalNode) {
       copyMoveCollection(resp, (CaldavCalNode)from,
                          to, copy, overwrite, depth);
 
       return;
     }
 
     // Copy entity or resource
     if ((depth != Headers.depthNone) && (depth != 0)) {
       throw new WebdavBadRequest();
     }
 
     if (from instanceof CaldavComponentNode) {
       copyMoveComponent(resp, (CaldavComponentNode)from,
                         to, copy, overwrite);
       return;
     }
 
     if (from instanceof CaldavResourceNode) {
       copyMoveResource(resp, (CaldavResourceNode)from,
                        to, copy, overwrite);
       return;
     }
 
     throw new WebdavBadRequest();
   }
 
   private Content doXrd(final HttpServletRequest req,
                         final HttpServletResponse resp,
                         final CaldavBwNode node) throws WebdavException {
     resp.setContentType("application/xrd+xml; charset=UTF-8");
 
     try {
       XRDType xrd = getXRD(node);
 
       JAXBContext jc = JAXBContext.newInstance(xrd.getClass().getPackage().getName());
 
       Marshaller m = jc.createMarshaller();
       m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
       m.marshal(xrd, resp.getOutputStream());
 
       Content c = new Content();
 
       c.written = true; // set content to say it's done
 
       return c;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /**
    * @param node
    * @param props
    * @throws WebdavException
    */
   public void getCalWSProperties(final CaldavBwNode node,
                                  final List<GetPropertiesBasePropertyType> props) throws WebdavException {
     for (PropertyTagEntry pte: node.getCalWSSoapNames()) {
       if (pte.inPropAll) {
         node.generateCalWsProperty(props, pte.tag, this, true);
       }
     }
   }
 
   /**
    * @param node
    * @return the XRD object for the node
    * @throws WebdavException
    */
   public XRDType getXRD(final CaldavBwNode node) throws WebdavException {
     try {
       XRDType xrd = new XRDType();
 
       AnyURI uri = new AnyURI();
 
       uri.setValue(node.getUrlValue());
       xrd.setSubject(uri);
 
       for (PropertyTagXrdEntry pxe: node.getXrdNames()) {
         if (pxe.inPropAll) {
           node.generateXrdProperties(xrd.getAliasOrPropertyOrLink(),
                                      pxe.xrdName, this, true);
         }
       }
 
       if (node.isCollection()) {
         // Provide link info for each child collection
 
         for (WebdavNsNode child: getChildren(node)) {
           CaldavBwNode cn = (CaldavBwNode)child;
 
           LinkType l = new LinkType();
           l.setRel(CalWSXrdDefs.childCollection);
           l.setHref(cn.getUrlValue());
 
           for (PropertyTagXrdEntry pxe: node.getXrdNames()) {
             if (pxe.inLink) {
               cn.generateXrdProperties(l.getTitleOrPropertyOrAny(),
                                        pxe.xrdName, this, true);
             }
           }
         }
       }
 
       return xrd;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   private void copyMoveCollection(final HttpServletResponse resp,
                                   final CaldavCalNode from,
                                   final WebdavNsNode to,
                                   final boolean copy,
                                   final boolean overwrite,
                                   final int depth) throws WebdavException {
     if (!(to instanceof CaldavCalNode)) {
       throw new WebdavBadRequest();
     }
 
     // Copy folder
     if ((depth != Headers.depthNone) && (depth != Headers.depthInfinity)) {
       throw new WebdavBadRequest();
     }
 
     CaldavCalNode fromCalNode = from;
     CaldavCalNode toCalNode = (CaldavCalNode)to;
 
     if (toCalNode.getExists() && !overwrite) {
       resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
 
       return;
     }
 
     /* XXX This is NOT rename - we really move or copy.
      * For the moment we'll deref both - but I think there are alias issues here
      */
     CalDAVCollection fromCol = (CalDAVCollection)fromCalNode.getCollection(true);
     CalDAVCollection toCol = (CalDAVCollection)toCalNode.getCollection(true);
 
     if ((fromCol == null) || (toCol == null)) {
       // What do we do here?
       resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
 
       return;
     }
 
     getSysi().copyMove(fromCol, toCol, copy, overwrite);
     if (toCalNode.getExists()) {
       resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
     } else {
       resp.setStatus(HttpServletResponse.SC_CREATED);
       Headers.makeLocation(resp, getLocation(to));
     }
   }
 
   private void copyMoveComponent(final HttpServletResponse resp,
                                  final CaldavComponentNode from,
                                  final WebdavNsNode to,
                                  final boolean copy,
                                  final boolean overwrite) throws WebdavException {
     if (!(to instanceof CaldavComponentNode)) {
       throw new WebdavBadRequest();
     }
 
     CaldavComponentNode toNode = (CaldavComponentNode)to;
 
     if (toNode.getExists() && !overwrite) {
       resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
 
       return;
     }
 
     /* deref - copy/move into targetted collection */
     CalDAVCollection toCol = (CalDAVCollection)toNode.getCollection(true);
 
     if (!getSysi().copyMove(from.getEvent(),
                             toCol, toNode.getEntityName(), copy,
                             overwrite)) {
       resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
     } else {
       resp.setStatus(HttpServletResponse.SC_CREATED);
       Headers.makeLocation(resp, getLocation(to));
     }
   }
 
   private void copyMoveResource(final HttpServletResponse resp,
                                final CaldavResourceNode from,
                                final WebdavNsNode to,
                                final boolean copy,
                                final boolean overwrite) throws WebdavException {
     if (!(to instanceof CaldavResourceNode)) {
       throw new WebdavForbidden(CaldavTags.supportedCalendarData);
     }
 
     CaldavResourceNode toNode = (CaldavResourceNode)to;
 
     if (toNode.getExists() && !overwrite) {
       resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
 
       return;
     }
 
     if (!getSysi().copyMoveFile(from.getResource(),
                                 toNode.getPath(), toNode.getEntityName(), copy,
                             overwrite)) {
       resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
     } else {
       resp.setStatus(HttpServletResponse.SC_CREATED);
       Headers.makeLocation(resp, getLocation(to));
     }
   }
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#specialUri(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String)
    */
   @Override
   public boolean specialUri(final HttpServletRequest req,
                             final HttpServletResponse resp,
                             final String resourceUri) throws WebdavException {
     RequestPars pars = new RequestPars(req, this, resourceUri);
     GetHandler handler = null;
 
     if (pars.iSchedule) {
       handler = new IscheduleGetHandler(this);
     } else if (pars.freeBusy) {
       handler = new FreeBusyGetHandler(this);
     } else if (pars.webcal) {
       handler = new WebcalGetHandler(this);
     }
 
     if (handler == null) {
       return false;
     }
 
     handler.process(req, resp, pars);
 
     return true;
   }
 
   @Override
   public WdSynchReport getSynchReport(final String path,
                                       final String token,
                                       final int limit,
                                       final boolean recurse) throws WebdavException {
     SynchReportData srd = getSysi().getSyncReport(path, token, limit, recurse);
 
     if (srd == null) {
       return null;
     }
 
     WdSynchReport wsr = new WdSynchReport();
 
     wsr.token = srd.token;
     wsr.truncated = srd.truncated;
     wsr.items = new ArrayList<WdSynchReportItem>();
 
     Map<String, WebdavNsNode> parents = new HashMap<String, WebdavNsNode>();
 
     for (SynchReportDataItem srdi: srd.items) {
       int nodeType;
       CalDAVCollection col = null;
       CalDAVResource r = null;
       CalDAVEvent ev = null;
       WdSynchReportItem wri = null;
       WebdavNsNode parent = null; // Need for non-collection
       String name;
       boolean canSync;
       boolean collection = false;
 
       if (srdi.getCol() == null) {
         parent = parents.get(srdi.getVpath());
         if (parent == null) {
           parent = getNode(srdi.getVpath(),
                            WebdavNsIntf.existanceMust,
                            WebdavNsIntf.nodeTypeCollection);
 
           parents.put(srdi.getVpath(), parent);
         }
 
         col = (CalDAVCollection)parent.getCollection(false);
         canSync = true;
 
         if (srdi.getEntity() != null) {
           nodeType = WebdavNsIntf.nodeTypeEntity;
           ev = srdi.getEntity();
           name = ev.getName();
         } else if (srdi.getResource() != null) {
           nodeType = WebdavNsIntf.nodeTypeEntity;
           r = srdi.getResource();
           name = r.getName();
         } else {
           throw new WebdavException("Unexpected return type");
         }
       } else {
         nodeType = WebdavNsIntf.nodeTypeCollection;
         col = srdi.getCol();
         name = col.getName();
         canSync = srdi.getCanSync();
         collection = true;
       }
 
       wri = new WdSynchReportItem(getNodeInt(Util.buildPath(collection,
                                                             srdi.getVpath(),
                                                             "/", name),
                                              WebdavNsIntf.existanceDoesExist,
                                              nodeType, col, ev, r),
                                              srdi.getToken(),
                                              canSync);
 
       wsr.items.add(wri);
     }
 
     return wsr;
   }
 
   @Override
   public String getSyncToken(final String path) throws WebdavException{
     String url = sysi.getUrlHandler().unprefix(fixPath(path));
 
     CalDAVCollection col = getSysi().getCollection(url);
 
     if (col == null) {
       throw new WebdavException(HttpServletResponse.SC_PRECONDITION_FAILED,
                                 "Bad If header - unknown resource");
     }
 
     return getSysi().getSyncToken(col);
   }
 
   /* ====================================================================
    *                  Access methods
    * ==================================================================== */
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getGroups(java.lang.String, java.lang.String)
    */
   @Override
   public Collection<WebdavNsNode> getGroups(final String resourceUri,
                                             final String principalUrl)
           throws WebdavException {
     Collection<WebdavNsNode> res = new ArrayList<WebdavNsNode>();
 
     Collection<String> hrefs = getSysi().getGroups(resourceUri, principalUrl);
     for (String href: hrefs) {
       if (href.endsWith("/")) {
         href = href.substring(0, href.length());
       }
 
       res.add(new CaldavUserNode(new CaldavURI(getSysi().getPrincipal(href)),
                                  getSysi(), null));
     }
 
     return res;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipalCollectionSet(java.lang.String)
    */
   @Override
   public Collection<String> getPrincipalCollectionSet(final String resourceUri)
          throws WebdavException {
     ArrayList<String> al = new ArrayList<String>();
 
     for (String s: getSysi().getPrincipalCollectionSet(resourceUri)) {
       al.add(sysi.getUrlHandler().prefix(s));
     }
 
     return al;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getPrincipals(java.lang.String, edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch)
    */
   @Override
   public Collection<WebdavPrincipalNode> getPrincipals(final String resourceUri,
                                                 final PrincipalPropertySearch pps)
           throws WebdavException {
     ArrayList<WebdavPrincipalNode> pnodes = new ArrayList<WebdavPrincipalNode>();
 
     for (CalPrincipalInfo cui: sysi.getPrincipals(resourceUri, pps)) {
       pnodes.add(new CaldavUserNode(new CaldavURI(cui.principal),
                                     getSysi(), cui));
     }
 
     return pnodes;
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#makeUserHref(java.lang.String)
    */
   @Override
   public String makeUserHref(final String id) throws WebdavException {
     return getSysi().makeHref(id, WhoDefs.whoTypeUser);
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#updateAccess(edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf.AclInfo)
    */
   @Override
   public void updateAccess(final AclInfo info) throws WebdavException {
     CaldavBwNode node = (CaldavBwNode)getNode(info.what,
                                               WebdavNsIntf.existanceMust,
                                               WebdavNsIntf.nodeTypeUnknown);
     updateAccess(info, node);
   }
 
   /**
    * @param info
    * @param node
    * @throws WebdavException
    */
   public void updateAccess(final AclInfo info,
                            final CaldavBwNode node) throws WebdavException {
     try {
       // May need a real principal hierarchy
       if (node instanceof CaldavCalNode) {
         // XXX to dref or not deref?
         sysi.updateAccess((CalDAVCollection)node.getCollection(false), info.acl);
       } else if (node instanceof CaldavComponentNode) {
         sysi.updateAccess(((CaldavComponentNode)node).getEvent(),
                           info.acl);
       } else {
         throw new WebdavException(HttpServletResponse.SC_NOT_IMPLEMENTED);
       }
     } catch (WebdavException wi) {
       throw wi;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   @Override
   public void emitAcl(final WebdavNsNode node) throws WebdavException {
     Acl acl = null;
 
     try {
       if (node.isCollection()) {
         acl = node.getCurrentAccess().getAcl();
       } else if (node instanceof CaldavComponentNode) {
         acl = ((CaldavComponentNode)node).getCurrentAccess().getAcl();
       }
 
       if (acl != null) {
         accessUtil.emitAcl(acl, true);
       }
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#getAclPrincipalInfo(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode)
    */
   @Override
   public Collection<String> getAclPrincipalInfo(final WebdavNsNode node) throws WebdavException {
     try {
       TreeSet<String> hrefs = new TreeSet<String>();
 
       for (Ace ace: node.getCurrentAccess().getAcl().getAces()) {
         AceWho who = ace.getWho();
 
         if (who.getWhoType() == WhoDefs.whoTypeUser) {
           hrefs.add(accessUtil.makeUserHref(who.getWho()));
         } else if (who.getWhoType() == WhoDefs.whoTypeGroup) {
           hrefs.add(accessUtil.makeGroupHref(who.getWho()));
         }
       }
 
       return hrefs;
     } catch (AccessException ae) {
       if (debug) {
         error(ae);
       }
       throw new WebdavServerError();
     }
   }
 
   /* ====================================================================
    *                Property value methods
    * ==================================================================== */
 
   /** Override this to create namespace specific property objects.
    *
    * @param propnode
    * @return WebdavProperty
    * @throws WebdavException
    */
   @Override
   public WebdavProperty makeProp(final Element propnode) throws WebdavException {
     if (!XmlUtil.nodeMatches(propnode, CaldavTags.calendarData)) {
       return super.makeProp(propnode);
     }
 
     /* Handle the calendar-data element */
 
     CalData caldata = new CalData(new QName(propnode.getNamespaceURI(),
                                             propnode.getLocalName()));
     caldata.parse(propnode);
 
     return caldata;
   }
 
   /** Properties we can process */
   private static final QName[] knownProperties = {
     CaldavTags.calendarData,
     CaldavTags.calendarTimezone,
     //  CaldavTags.maxAttendeesPerInstance,
     //  CaldavTags.maxDateTime,
     //  CaldavTags.maxInstances,
     CaldavTags.maxResourceSize,
     //  CaldavTags.minDateTime,
   };
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#knownProperty(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, edu.rpi.cct.webdav.servlet.shared.WebdavProperty)
    */
   @Override
   public boolean knownProperty(final WebdavNsNode node,
                                final WebdavProperty pr) {
     QName tag = pr.getTag();
 
     if (node.knownProperty(tag)) {
       return true;
     }
 
     for (int i = 0; i < knownProperties.length; i++) {
       if (tag.equals(knownProperties[i])) {
         return true;
       }
     }
 
     /* Try the node for a value */
 
     return super.knownProperty(node, pr);
   }
 
   /* (non-Javadoc)
    * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf#generatePropValue(edu.rpi.cct.webdav.servlet.shared.WebdavNsNode, edu.rpi.cct.webdav.servlet.shared.WebdavProperty, boolean)
    */
   @Override
   public boolean generatePropValue(final WebdavNsNode node,
                                    WebdavProperty pr,
                                    final boolean allProp) throws WebdavException {
     QName tag = pr.getTag();
     String ns = tag.getNamespaceURI();
 
     try {
       /* Deal with anything but webdav properties */
       if (ns.equals(WebdavTags.namespace)) {
         // Not ours
         return super.generatePropValue(node, pr, allProp);
       }
 
       if (tag.equals(CaldavTags.calendarData)) {
         // pr may be a CalendarData object - if not it's probably allprops
         if (!(pr instanceof CalData)) {
           pr = new CalData(tag);
         }
 
         CalData caldata = (CalData)pr;
 
         if (debug) {
           trace("do CalendarData for " + node.getUri());
         }
 
         int status = HttpServletResponse.SC_OK;
         try {
           /* Output the (transformed) node.
            */
 
           xml.openTagNoNewline(CaldavTags.calendarData);
           caldata.process(node, xml);
 
           return true;
         } catch (WebdavException wde) {
           status = wde.getStatusCode();
           if (debug && (status != HttpServletResponse.SC_NOT_FOUND)) {
             error(wde);
           }
           return false;
         } finally {
           xml.closeTagNoblanks(CaldavTags.calendarData);
         }
       }
 
       if (tag.equals(CaldavTags.maxAttendeesPerInstance)) {
         return false;
       }
 
       if (tag.equals(CaldavTags.maxDateTime)) {
         return false;
       }
 
       if (tag.equals(CaldavTags.maxInstances)) {
         return false;
       }
 
       if (tag.equals(CaldavTags.maxResourceSize)) {
         /* e.g.
          * <C:max-resource-size
          *    xmlns:C="urn:ietf:params:xml:ns:caldav">102400</C:max-resource-size>
          */
         xml.property(tag, String.valueOf(sysi.getSystemProperties().getMaxUserEntitySize()));
         return true;
       }
 
       if (tag.equals(CaldavTags.minDateTime)) {
         return false;
       }
 
       return node.generatePropertyValue(tag, this, allProp);
     } catch (WebdavException wie) {
       throw wie;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* ====================================================================
    *                         Caldav methods
    * ==================================================================== */
 
   /** Use the given query to return a collection of nodes. An exception will
    * be raised if the entire query fails for some reason (access, etc). An
    * empty collection will be returned if no objects match.
    *
    * @param wdnode    WebdavNsNode defining root of search
    * @param retrieveList   If non-null limit required fields.
    * @param retrieveRecur  How we retrieve recurring events
    * @param fltr      Filter object defining search
    * @return Collection of result nodes (empty for no result)
    * @throws WebdavException
    */
   public Collection<WebdavNsNode> query(final WebdavNsNode wdnode,
                                         final List<String> retrieveList,
                                         final RetrievalMode retrieveRecur,
                                         final FilterType fltr) throws WebdavException {
     CaldavBwNode node = (CaldavBwNode)wdnode;
 
     FilterHandler fh = new FilterHandler(fltr);
     Collection<CalDAVEvent> events = fh.query(node,
                                              retrieveList, retrieveRecur);
 
     /* We now need to build a node for each of the events in the collection.
        For each event we first determine what calendar it's in. We then take the
        incoming uri, strip any calendar names off it and append the calendar
        name and event name to create the new uri.
 
        If there is no calendar name for the event we just give it the default.
      */
 
     Collection<WebdavNsNode> evnodes = new ArrayList<WebdavNsNode>();
 
     if (events == null) {
       return evnodes;
     }
 
     try {
       for (CalDAVEvent ev: events) {
         CalDAVCollection col = getSysi().getCollection(ev.getParentPath());
         String uri = col.getPath();
 
         /* If no name was assigned use the guid */
         String evName = ev.getName();
         if (evName == null) {
           evName = ev.getUid() + ".ics";
         }
 
         String evuri = Util.buildPath(false, uri, "/", evName);
 
         CaldavComponentNode evnode = (CaldavComponentNode)getNodeInt(evuri,
                                                    WebdavNsIntf.existanceDoesExist,
                                                    WebdavNsIntf.nodeTypeEntity,
                                                    col, ev, null);
 
         evnodes.add(evnode);
       }
 
       return fh.postFilter(evnodes);
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       error(t);
       throw new WebdavServerError();
     }
   }
 
   /** The node represents a calendar resource for which we must get free-busy
    * information.
    *
    * @param cnode  CaldavCalNode
    * @param freeBusy
    * @param depth
    * @throws WebdavException
    */
   public void getFreeBusy(final CaldavCalNode cnode,
                           final FreeBusyQuery freeBusy,
                           final int depth) throws WebdavException {
     try {
       /* We need to deref as the freebusy comes from the target */
       CalDAVCollection c = (CalDAVCollection)cnode.getCollection(true);
 
       if (c == null) {
         // XXX - exception?
         return;
       }
 
       cnode.setFreeBusy(freeBusy.getFreeBusy(sysi, c,
                                              depth));
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /* ====================================================================
    *                         Private methods
    * ==================================================================== */
 
   private SysIntf getSysi(final String className) throws WebdavException {
     try {
       Object o = Class.forName(className).newInstance();
 
       if (o == null) {
         throw new WebdavException("Class " + className + " not found");
       }
 
       if (!SysIntf.class.isInstance(o)) {
         throw new WebdavException("Class " + className +
                                   " is not a subclass of " +
                                   SysIntf.class.getName());
       }
 
       return (SysIntf)o;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   private WebdavNsNode getNodeInt(final String uri,
                                   final int existance,
                                   final int nodeType,
                                   final CalDAVCollection col,
                                   final CalDAVEvent ev,
                                   final CalDAVResource r) throws WebdavException {
     if (debug) {
       debugMsg("About to get node for " + uri);
     }
 
     if (uri == null)  {
       return null;
     }
 
     try {
       CaldavURI wi = findURI(uri, existance, nodeType, col, ev, r);
 
       if (wi == null) {
         return null;
       }
 
       WebdavNsNode nd = null;
       AccessPrincipal ap = wi.getPrincipal();
 
       if (ap != null) {
         if (ap.getKind() == WhoDefs.whoTypeUser) {
           nd = new CaldavUserNode(wi, sysi, sysi.getCalPrincipalInfo(ap));
         } else if (ap.getKind() == WhoDefs.whoTypeGroup) {
           nd = new CaldavGroupNode(wi, sysi, sysi.getCalPrincipalInfo(ap));
         }
       } else if (wi.isCollection()) {
         nd = new CaldavCalNode(wi, sysi);
       } else if (wi.isResource()) {
         nd = new CaldavResourceNode(wi, sysi);
       } else {
         nd = new CaldavComponentNode(wi, sysi);
       }
 
       return nd;
     } catch (WebdavNotFound wnf) {
       throw wnf;
     } catch (WebdavException we) {
       throw we;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   /** Find the named item by following down the path from the root.
    * This requires the names at each level to be unique (and present)
    *
    * I don't think the name.now has to have an ics suffix. Draft 7 goes as
    * far as saying it may have ".ics" or ".ifb"
    *
    * For the moment enforce one or the other
    *
    * <p>Uri is at least /user/user-id or <br/>
    *    /public
    * <br/>followed by one or more calendar path elements possibly followed by an
    * entity name.
    *
    * @param uri        String uri - just the path part
    * @param existance        Say's something about the state of existance
    * @param nodeType         Say's something about the type of node
    * @param collection        Supplied CalDAVCollection object if we already have it.
    * @param ev
    * @param rsrc
    * @return CaldavURI object representing the uri
    * @throws WebdavException
    */
   private CaldavURI findURI(String uri,
                             final int existance,
                             final int nodeType,
                             final CalDAVCollection collection,
                             CalDAVEvent ev,
                             CalDAVResource rsrc) throws WebdavException {
     try {
       if ((nodeType == WebdavNsIntf.nodeTypeUnknown) &&
           (existance != WebdavNsIntf.existanceMust)) {
         // We assume an unknown type must exist
         throw new WebdavServerError();
       }
 
       uri = normalizeUri(uri);
 
       if (!uri.startsWith("/")) {
         return null;
       }
 
       CaldavURI curi = null;
       /* Look for it in the map * /
        * This is stateless so we probably never find it.
       CaldavURI curi = getUriPath(uri);
       if (curi != null) {
         if (debug) {
           debugMsg("reuse uri - " + curi.getPath() +
                    "\" entityName=\"" + curi.getEntityName() + "\"");
         }
         return curi;
       }*/
 
       boolean isPrincipal = sysi.isPrincipal(uri);
 
       if ((nodeType == WebdavNsIntf.nodeTypePrincipal) && !isPrincipal) {
         throw new WebdavNotFound(uri);
       }
 
       if (isPrincipal) {
         AccessPrincipal p = getSysi().getPrincipal(uri);
 
         if (p == null) {
           throw new WebdavNotFound(uri);
         }
 
         return new CaldavURI(p);
       }
 
       if (existance == WebdavNsIntf.existanceDoesExist) {
         // Provided with calendar and entity if needed.
         String name = null;
         if (ev != null) {
           name = ev.getName();
           curi = new CaldavURI(collection, ev, name, true, false);
         } else if (rsrc != null) {
           curi = new CaldavURI(collection, rsrc, true);
         } else {
           curi = new CaldavURI(collection, ev, name, true, false);
         }
         //putUriPath(curi);
 
         return curi;
       }
 
       if ((nodeType == WebdavNsIntf.nodeTypeCollection) ||
           (nodeType == WebdavNsIntf.nodeTypeUnknown)) {
         // For unknown we try the full path first as a calendar.
         String coluri = Util.buildPath(true, uri);
         if (debug) {
           debugMsg("search for collection uri \"" + coluri + "\"");
         }
         CalDAVCollection col = sysi.getCollection(coluri);
 
         if (col == null) {
           if ((nodeType == WebdavNsIntf.nodeTypeCollection) &&
               (existance != WebdavNsIntf.existanceNot) &&
               (existance != WebdavNsIntf.existanceMay)) {
             /* We asked for a collection and it doesn't exist */
             throw new WebdavNotFound(uri);
           }
 
           // We'll try as an entity for unknown
         } else {
          if (existance == WebdavNsIntf.existanceNot) {
             throw new WebdavForbidden(WebdavTags.resourceMustBeNull);
           }
 
           if (debug) {
             debugMsg("create collection uri - cal=\"" + col.getPath() + "\"");
           }
 
           curi = new CaldavURI(col, true);
           //putUriPath(curi);
 
           return curi;
         }
       }
 
       // Entity or unknown
       String parentPath;
       String entityName = null;
 
       if (calWs && (existance == WebdavNsIntf.existanceNot)) {
         /* If we are trying to create we POST to the collection - there is no
            entity name */
         parentPath = uri;
       } else {
         /* Split name into parent path and entity name part */
         SplitResult split = splitUri(uri);
 
         if (split.name == null) {
           // No name part
           throw new WebdavNotFound(uri);
         }
 
         parentPath = split.path;
         entityName = split.name;
       }
 
       /* Look for the parent */
       CalDAVCollection col = sysi.getCollection(parentPath);
 
       if (col == null) {
         if (nodeType == WebdavNsIntf.nodeTypeCollection) {
           // Trying to create calendar/collection with no parent
           throw new WebdavException(HttpServletResponse.SC_CONFLICT);
         }
 
         throw new WebdavNotFound(uri);
       }
 
       if (nodeType == WebdavNsIntf.nodeTypeCollection) {
         // Trying to create calendar/collection
         CalDAVCollection newCol = getSysi().newCollectionObject(false,
                                                                 col.getPath());
         newCol.setName(entityName);
         newCol.setPath(Util.buildPath(true, col.getPath(), "/", newCol.getName()));
 
         curi = new CaldavURI(newCol, false);
 
         return curi;
       }
 
       int ctype = col.getCalType();
       if ((ctype == CalDAVCollection.calTypeCalendarCollection) ||
           (ctype == CalDAVCollection.calTypeInbox) ||
           (ctype == CalDAVCollection.calTypeOutbox)) {
         if (entityName != null) {
           if (debug) {
             debugMsg("find event(s) - cal=\"" + col.getPath() + "\" name=\"" +
                      entityName + "\"");
           }
 
           ev = sysi.getEvent(col, entityName, null);
 
           if ((existance == existanceMust) && (ev == null)) {
             throw new WebdavNotFound(uri);
           }
         }
 
         curi = new CaldavURI(col, ev, entityName, ev != null,
                              entityName == null);
       } else {
         if (entityName != null) {
           if (debug) {
             debugMsg("find resource - cal=\"" + col.getPath() + "\" name=\"" +
                      entityName + "\"");
           }
 
           /* Look for a resource */
           rsrc = sysi.getFile(col, entityName);
 
           if ((existance == existanceMust) && (rsrc == null)) {
             throw new WebdavNotFound(uri);
           }
         }
 
         boolean exists = rsrc != null;
 
         if (!exists) {
           rsrc = getSysi().newResourceObject(col.getPath());
           rsrc.setName(entityName);
         }
 
         curi = new CaldavURI(col, rsrc, exists);
       }
 
       //putUriPath(curi);
 
       return curi;
     } catch (WebdavNotFound wnf) {
       throw wnf;
     } catch (WebdavException wde) {
       throw wde;
     } catch (Throwable t) {
       throw new WebdavException(t);
     }
   }
 
   private static class SplitResult {
     String path;
     String name;
 
     SplitResult(final String path, final String name) {
       this.path = path;
       this.name = name;
     }
   }
 
   /* Split the uri so that result.path is the path up to the name part result.name
    *
    */
   private SplitResult splitUri(final String uri) throws WebdavException {
     // Remove any trailing slash = it makes things easier.
     String noslUri = Util.buildPath(false, uri);
 
     int pos = noslUri.lastIndexOf("/");
     if (pos < 0) {
       // bad uri
       throw new WebdavBadRequest("Invalid uri: " + uri);
     }
 
     String path;
     String name = null;
 
     if (pos == 0) {
       path = Util.buildPath(true, uri);
     } else {
       path = noslUri.substring(0, pos);
       name = noslUri.substring(pos + 1);
     }
 
     return new SplitResult(Util.buildPath(true, path), name);
   }
 
   private String normalizeUri(String uri) throws WebdavException {
     /*Remove all "." and ".." components */
     try {
       uri = new URI(null, null, uri, null).toString();
 
       uri = new URI(URLEncoder.encode(uri, "UTF-8")).normalize().getPath();
 
       uri = URLDecoder.decode(uri, "UTF-8");
 
       //if ((uri.length() > 1) && uri.endsWith("/")) {
       //  uri = uri.substring(0, uri.length() - 1);
       //}
 
       if (debug) {
         debugMsg("Normalized uri=" + uri);
       }
 
       return uri;
     } catch (Throwable t) {
       if (debug) {
         error(t);
       }
       throw new WebdavBadRequest("Bad uri: " + uri);
     }
   }
 
   /*
   private CaldavURI getUriPath(String path) {
     return uriMap.get(path);
   }
 
   private void putUriPath(CaldavURI wi) {
     uriMap.put(wi.getPath(), wi);
   }
   */
 }
