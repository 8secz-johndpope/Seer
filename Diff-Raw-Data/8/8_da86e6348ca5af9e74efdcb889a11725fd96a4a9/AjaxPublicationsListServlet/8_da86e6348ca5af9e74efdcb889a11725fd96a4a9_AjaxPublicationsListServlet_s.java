 /**
  * Copyright (C) 2000 - 2009 Silverpeas
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * As a special exception to the terms and conditions of version 3.0 of
  * the GPL, you may redistribute this Program in connection with Free/Libre
  * Open Source Software ("FLOSS") applications as described in Silverpeas's
  * FLOSS exception.  You should have recieved a copy of the text describing
  * the FLOSS exception, and it is also available here:
  * "http://repository.silverpeas.com/legal/licensing"
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package com.stratelia.webactiv.kmelia.servlets;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import com.silverpeas.util.EncodeHelper;
 import com.silverpeas.util.ForeignPK;
 import com.silverpeas.util.StringUtil;
 import com.stratelia.silverpeas.peasCore.URLManager;
 import com.stratelia.silverpeas.silvertrace.SilverTrace;
 import com.stratelia.silverpeas.util.ResourcesWrapper;
 import com.stratelia.silverpeas.versioning.model.Document;
 import com.stratelia.silverpeas.versioning.model.DocumentVersion;
 import com.stratelia.silverpeas.versioning.util.VersioningUtil;
 import com.stratelia.webactiv.beans.admin.UserDetail;
 import com.stratelia.webactiv.kmelia.control.KmeliaSessionController;
 import com.stratelia.webactiv.kmelia.control.ejb.KmeliaHelper;
 import com.stratelia.webactiv.kmelia.model.TopicDetail;
 import com.stratelia.webactiv.kmelia.model.UserPublication;
 import com.stratelia.webactiv.util.FileRepositoryManager;
 import com.stratelia.webactiv.util.FileServerUtils;
 import com.stratelia.webactiv.util.GeneralPropertiesManager;
 import com.stratelia.webactiv.util.ResourceLocator;
 import com.stratelia.webactiv.util.attachment.control.AttachmentController;
 import com.stratelia.webactiv.util.attachment.ejb.AttachmentPK;
 import com.stratelia.webactiv.util.attachment.model.AttachmentDetail;
 import com.stratelia.webactiv.util.publication.model.PublicationDetail;
 import com.stratelia.webactiv.util.publication.model.PublicationPK;
 import com.stratelia.webactiv.util.viewGenerator.html.GraphicElementFactory;
 import com.stratelia.webactiv.util.viewGenerator.html.board.Board;
 import com.stratelia.webactiv.util.viewGenerator.html.pagination.Pagination;
 
 /**
  * @author ehugonnet
  */
 public class AjaxPublicationsListServlet extends HttpServlet {
 
   private static final long serialVersionUID = 1L;
 
   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse res)
       throws ServletException, IOException {
     doPost(req, res);
   }
 
   @Override
   public void doPost(HttpServletRequest req, HttpServletResponse res)
       throws ServletException, IOException {
     HttpSession session = req.getSession(true);
 
     String componentId = req.getParameter("ComponentId");
     String nodeId = req.getParameter("Id");
 
     KmeliaSessionController kmeliaSC = (KmeliaSessionController) session
         .getAttribute("Silverpeas_" + "kmelia" + "_" + componentId);
     GraphicElementFactory gef = (GraphicElementFactory) session
         .getAttribute("SessionGraphicElementFactory");
     String context = GeneralPropertiesManager.getGeneralResourceLocator()
         .getString("ApplicationURL");
 
     if (kmeliaSC != null) {
 
       if (StringUtil.isDefined(nodeId)) {
         kmeliaSC.getTopic(nodeId, true);
         session.setAttribute("Silverpeas_DragAndDrop_TopicId", nodeId); // used by drag n drop
       }
 
       ResourcesWrapper resources = new ResourcesWrapper(
           kmeliaSC.getMultilang(), kmeliaSC.getIcon(), kmeliaSC.getSettings(),
           kmeliaSC.getLanguage());
 
       String index = req.getParameter("Index");
       String sort = req.getParameter("Sort");
       String sToLink = req.getParameter("ToLink");
       String sToValidate = req.getParameter("ToValidate");
       String sToPortlet = req.getParameter("ToPortlet");
       String pubIdToHighlight = req.getParameter("PubIdToHighLight");
 
       boolean toLink = (isDefined(sToLink) && "1".equals(sToLink));
       boolean toValidate = (isDefined(sToValidate) && "1".equals(sToValidate));
       boolean toPortlet = (isDefined(sToPortlet) && "1".equals(sToPortlet));
 
       if (toLink) {
         processPublicationsToLink(kmeliaSC, req);
       }
 
       if (isDefined(index)) {
         kmeliaSC.setIndexOfFirstPubToDisplay(index);
       }
       if (isDefined(sort)) {
         kmeliaSC.setSortValue(sort);
       }
 
       sort = kmeliaSC.getSortValue();
 
       SilverTrace.info("kmelia", "AjaxPublicationsListServlet.doPost",
           "root.MSG_GEN_PARAM_VALUE", "Request parameters = "
           + req.getQueryString());
 
       TopicDetail currentTopic = null;
       boolean sortAllowed = true;
       boolean linksAllowed = true;
       boolean checkboxAllowed = false;
       List<String> selectedIds = new ArrayList<String>();
       List<UserPublication> publications = null;
       boolean subTopics = false;
       String role = kmeliaSC.getProfile();
       if (toLink) {
         currentTopic = kmeliaSC.getSessionTopicToLink();
         sortAllowed = false;
         linksAllowed = false;
         checkboxAllowed = true;
         selectedIds = kmeliaSC.getPublicationsToLink();
 
         PublicationPK currentPubPK = kmeliaSC.getSessionPubliOrClone()
             .getPublication().getPublicationDetail().getPK();
         publications = currentTopic.getValidPublications(currentPubPK);
       } else if (toPortlet) {
         sortAllowed = false;
         currentTopic = kmeliaSC.getSessionTopic();
         publications = kmeliaSC.getSessionPublicationsList();
         role = "user";
       } else if (toValidate) {
         kmeliaSC.orderPubsToValidate(Integer.parseInt(sort));
         publications = kmeliaSC.getSessionPublicationsList();
       } else {
         currentTopic = kmeliaSC.getSessionTopic();
         publications = kmeliaSC.getSessionPublicationsList();
       }
 
       if (KmeliaHelper.isToolbox(componentId)) {
         String profile = kmeliaSC.getUserTopicProfile(currentTopic.getNodePK()
             .getId());
         if ("user".equals(profile))
           linksAllowed = false;
       }
 
       if (currentTopic != null)
         subTopics = currentTopic.getNodeDetail().getChildrenNumber() > 0;
 
       res.setContentType("text/xml");
       res.setHeader("charset", "UTF-8");
 
       Writer writer = res.getWriter();
       /*
        * writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        * writer.write("<ajax-response>");
        * writer.write("<response type=\"element\" id=\""+elementId+"\">");
        */
       if (kmeliaSC.isRightsOnTopicsEnabled() && kmeliaSC.isUserComponentAdmin()
           && !kmeliaSC.isCurrentTopicAvailable()) {
         Board board = gef.getBoard();
         writer.write(board.printBefore());
         writer
             .write("<table width=\"100%\" border=\"0\" cellspacing=\"0\" align=\"center\">");
         writer.write("<tr>");
         writer.write("<td>"
             + EncodeHelper.escapeXml(resources
             .getString("GML.ForbiddenAccessContent")) + "</td>");
         writer.write("</tr>");
         writer.write("</table>");
         writer.write(board.printAfter());
       } else if (currentTopic != null && "0".equals(currentTopic.getNodePK().getId()) &&
           kmeliaSC.getNbPublicationsOnRoot() != 0 && kmeliaSC.isTreeStructure()) {
         List<UserPublication> publicationsToDisplay = new ArrayList<UserPublication>();
         Iterator<UserPublication> iterator = currentTopic.getPublicationDetails().iterator();
         UserPublication userPub;
         while (iterator.hasNext()) {
           userPub = iterator.next();
          if (!kmeliaSC.isPublicationDeleted(userPub.getPublication().getPK().getId()))
             publicationsToDisplay.add(userPub);
         }
         displayLastPublications(publicationsToDisplay, kmeliaSC, resources, gef, writer);
       } else {
         displayPublications(publications, subTopics, sortAllowed, linksAllowed, checkboxAllowed,
             kmeliaSC, role, gef, context, resources, selectedIds, pubIdToHighlight, writer);
       }
       /*
        * writer.write("</response>"); writer.write("</ajax-response>");
        */
     }
   }
 
   void displayPublications(List<UserPublication> allPubs, boolean subtopicsExist,
       boolean sortAllowed, boolean linksAllowed, boolean checkboxAllowed,
       KmeliaSessionController kmeliaScc, String profile,
       GraphicElementFactory gef, String context, ResourcesWrapper resources,
       List<String> selectedIds, String pubIdToHighlight, Writer out) throws IOException {
     PublicationDetail pub;
     UserPublication userPub;
     UserDetail user;
     String vignette_url;
     String publicationSrc = resources.getIcon("kmelia.publication");
     String fullStarSrc = resources.getIcon("kmelia.fullStar");
     String emptyStarSrc = resources.getIcon("kmelia.emptyStar");
     ResourceLocator publicationSettings = new ResourceLocator(
         "com.stratelia.webactiv.util.publication.publicationSettings",
         kmeliaScc.getLanguage());
     boolean displayLinks = URLManager.displayUniversalLinks();
     boolean showImportance = resources.getSetting("showImportance", true);
     boolean showNoPublisMessage = resources.getSetting("showNoPublisMessage",
         true);
     String language = kmeliaScc.getCurrentLanguage();
     String name = null;
     String description = null;
 
     String currentUserId = kmeliaScc.getUserDetail().getId();
     String currentTopicId = "0";
     if (kmeliaScc.getSessionTopic() != null)
       currentTopicId = kmeliaScc.getSessionTopic().getNodePK().getId();
 
     int nbPubsPerPage = kmeliaScc.getNbPublicationsPerPage();
     int firstDisplayedItemIndex = kmeliaScc.getIndexOfFirstPubToDisplay();
     int nbPubs = allPubs.size();
 
     Board board = gef.getBoard();
     Pagination pagination = gef.getPagination(nbPubs, nbPubsPerPage,
         firstDisplayedItemIndex);
     List<UserPublication> pubs = allPubs.subList(pagination.getFirstItemIndex(), pagination
         .getLastItemIndex());
 
     // if (subtopicsExist)
     // out.write("<BR/>");
 
     out.write("<Form name=\"publicationsForm\">");
 
     if (pubs.size() > 0) {
       out.write(board.printBefore());
       out
           .write("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\">");
       displayPublicationsListHeader(nbPubs, sortAllowed, pagination, resources,
           out);
 
       String linkIcon = resources.getIcon("kmelia.link");
       String pubColor = "";
       String pubState = "";
       String highlightClassBegin = "";
       String highlightClassEnd = "";
       for (int p = 0; p < pubs.size(); p++) {
         userPub = pubs.get(p);
         pub = userPub.getPublication();
         user = userPub.getOwner();
         name = pub.getName(language);
         description = pub.getDescription(language);
 
         pubColor = "";
         pubState = "";
         highlightClassBegin = "";
         highlightClassEnd = "";
 
         if (StringUtil.isDefined(pubIdToHighlight)
             && pubIdToHighlight.equals(pub.getPK().getId())) {
           highlightClassBegin = "<span class=\"highlight\">";
           highlightClassEnd = "</span>";
         }
 
         out.write("<!-- Publication Body -->");
 
         if (pub.getStatus() != null && pub.getStatus().equals("Valid")) {
           if (pub.haveGotClone() && "Clone".equals(pub.getCloneStatus())
               && !"user".equals(profile)) {
             pubColor = "blue";
             pubState = resources.getString("kmelia.UpdateInProgress");
           } else if ("Draft".equals(pub.getCloneStatus())) {
             if (currentUserId.equals(user.getId())) {
               pubColor = "gray";
               pubState = resources.getString("PubStateDraft");
             }
           } else if ("ToValidate".equals(pub.getCloneStatus())) {
             if (profile.equals("admin") || profile.equals("publisher")
                 || currentUserId.equals(user.getId())) {
               pubColor = "red";
               pubState = resources.getString("kmelia.PubStateToValidate");
             }
           } else {
             if (pub.isNotYetVisible()) {
               pubState = resources.getString("kmelia.VisibleFrom") + " "
                   + resources.getOutputDateAndHour(pub.getBeginDateAndHour());
             } else if (pub.isNoMoreVisible()) {
               pubState = resources.getString("kmelia.VisibleTo") + " "
                   + resources.getOutputDateAndHour(pub.getEndDateAndHour());
             }
             if (!pub.isVisible())
               pubColor = "gray";
           }
         } else {
           if (pub.getStatus() != null
               && pub.getStatus().equals(PublicationDetail.DRAFT)) {
             // en mode brouillon, si on est en co-rédaction et si on autorise
             // le
             // mode brouillon visible par tous,
             // les publication en mode brouillon sont visibles par tous sauf les
             // lecteurs
             // sinon, seules les publications brouillons de l'utilisateur sont
             // visibles
             if (currentUserId.equals(pub.getUpdaterId())
                 || ((kmeliaScc.isCoWritingEnable() && kmeliaScc
                 .isDraftVisibleWithCoWriting()) && !profile.equals("user"))) {
               pubColor = "gray";
               pubState = resources.getString("PubStateDraft");
             }
           } else {
             if (profile.equals("admin") || profile.equals("publisher")
                 || currentUserId.equals(pub.getUpdaterId())
                 || (!profile.equals("user") && kmeliaScc.isCoWritingEnable())) {
               // si on est en co-rédaction, on affiche toutes les publications
               // à
               // valider (sauf pour les lecteurs)
               pubColor = "red";
               if ("UnValidate".equalsIgnoreCase(pub.getStatus()))
                 pubState = resources.getString("kmelia.PubStateUnvalidate");
               else
                 pubState = resources.getString("kmelia.PubStateToValidate");
             }
           }
         }
 
         if (!pub.getPK().getInstanceId().equals(kmeliaScc.getComponentId())) {
           pubState = resources.getString("kmelia.Shortcut");
         }
 
         out.write("<tr class=\"intfdcolor4\">");
         if (checkboxAllowed) {
           String checked = "";
           if (selectedIds != null && selectedIds.contains(pub.getPK().getId()))
             checked = "checked=\"checked\"";
           out.write("<td valign=\"middle\" align=\"center\" width=\"80\">");
           out.write("<input type=\"checkbox\" name=\"C1\" value=\""
               + pub.getPK().getId() + "\" " + checked + "/>");
           out.write("</td>");
         } else {
           if (pub.getImage() != null
               && Boolean.valueOf(resources.getSetting("isVignetteVisible"))) {
             out.write("<td valign=\"top\" align=\"right\" width=\"80\">");
             String height = resources.getSetting("vignetteHeight");
             String width = resources.getSetting("vignetteWidth");
             vignette_url = EncodeHelper.escapeXml(FileServerUtils.getUrl(pub
                 .getPK().getSpace(), pub.getPK().getComponentName(),
                 "vignette", pub.getImage(), pub.getImageMimeType(),
                 publicationSettings.getString("imagesSubDirectory")));
             out.write("<IMG SRC=\"" + vignette_url + "\""
                 + ((height == null) ? "" : " HEIGHT=\"" + height + "\"")
                 + ((width == null) ? "" : " WIDTH=\"" + width + "\"")
                 + "/>&#160;");
             out.write("</td>");
           } else {
             out.write("<td valign=\"top\" align=\"right\">");
             out.write("<img src=\"" + resources.getIcon("kmelia.1px") + "\"/>");
             out.write("</td>");
           }
         }
 
         out.write("<td valign=\"top\" colspan=\"4\">");
         out
             .write("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr valign=\"middle\">");
         out
             .write("<td width=\"1\" valign=\"top\">&#8226;&#160;</td><td><a name=\""
             + pub.getPK().getId() + "\"></a>");
         if (linksAllowed)
           out.write("<font color=\"" + pubColor
               + "\"><a href=\"javascript:onClick=publicationGoTo('"
               + pub.getPK().getId() + "')\"><b>" + highlightClassBegin
               + EncodeHelper.escapeXml(name) + highlightClassEnd
               + "</b></a></font>");
         else
           out.write("<font color=\"" + pubColor + "\"><b>"
               + highlightClassBegin + EncodeHelper.escapeXml(name)
               + highlightClassEnd + "</b></font>");
 
         out.write("&#160;");
         // if ("Draft".equals(pub.getStatus()))
         if (pubState.length() > 0)
           out.write("(" + EncodeHelper.escapeXml(pubState) + ")");
         else if (showImportance)
           out.write("<nobr>"
               + displayImportance(new Integer(pub.getImportance()).intValue(),
               5, fullStarSrc, emptyStarSrc, out) + "</nobr>");
         out.write("</td>");
         out.write("</tr>");
         out
             .write("<tr><td width=\"1\">&#160;</td><td colspan=\"2\"><font color=\""
             + pubColor + "\">");
         if (kmeliaScc.showUserNameInList())
           out.write(getUserName(userPub, kmeliaScc) + " - ");
         out.write(resources.getOutputDate(pub.getUpdateDate()));
         if (kmeliaScc.isAuthorUsed() && pub.getAuthor() != null
             && !pub.getAuthor().equals(""))
           out.write("&#160;-&#160;(" + resources.getString("GML.author")
               + ":&#160;" + EncodeHelper.escapeXml(pub.getAuthor()) + ")");
         if (displayLinks) {
           String link = null;
           if (!pub.getPK().getInstanceId().equals(kmeliaScc.getComponentId()))
             link = URLManager.getSimpleURL(URLManager.URL_PUBLI, pub.getPK()
                 .getId(), kmeliaScc.getComponentId());
           else
             link = URLManager.getSimpleURL(URLManager.URL_PUBLI, pub.getPK()
                 .getId());
           out.write(" - <a href=\""
               + link
               + "\"><img src=\""
               + linkIcon
               + "\" border=\"0\" align=\"absmiddle\" alt=\""
               + EncodeHelper.escapeXml(resources
               .getString("kmelia.CopyPublicationLink"))
               + "\" title=\""
               + EncodeHelper.escapeXml(resources
               .getString("kmelia.CopyPublicationLink")) + "\"/></a>");
         }
 
         if (StringUtil.isDefined(description) && !description.equals(name)) {
           out.write("<br/>");
           out.write(EncodeHelper.javaStringToHtmlParagraphe(EncodeHelper
               .escapeXml(pub.getDescription(language))));
         }
         out.write("</font>");
         out.write("<BR/>");
         if (KmeliaHelper.isToolbox(kmeliaScc.getComponentId()) || kmeliaScc.attachmentsInPubList()) {
           if (kmeliaScc.isVersionControlled()) {
             out.write(displayVersioning(pub, out, resources));
           } else {
             out.write(displayAttachments(pub, currentUserId, currentTopicId, out, resources));
           }
         }
         out.write("<BR/>");
         out.write("</td></tr></table>");
         out.write("</td>");
         out.write("</tr>");
 
       } // End while
       if (nbPubs > nbPubsPerPage) {
         out
             .write("<tr class=\"intfdcolor4\"><td colspan=\"5\">&#160;</td></tr>");
         out.write("<tr valign=\"middle\" class=\"intfdcolor\">");
         out.write("<td colspan=\"5\" align=\"center\">");
         out.write(pagination.printIndex("doPagination"));
         out.write("</td>");
         out.write("</tr>");
       }
       out.write("</table>");
       out.write(board.printAfter());
     } // End if
     else if (showNoPublisMessage
         && (kmeliaScc.getNbPublicationsOnRoot() != 0 || !currentTopicId
         .equals("0"))) {
       out.write(board.printBefore());
       out
           .write("<table width=\"100%\" border=\"0\" cellspacing=\"0\" align=\"center\">");
       out.write("<tr valign=\"middle\" class=\"intfdcolor\">");
       out.write("<td width=\"80\"><img src=\"" + publicationSrc
           + "\" border=\"0\"/></td>");
       out.write("<td align=\"left\"><b>"
           + resources.getString("GML.publications") + "</b></td></tr>");
       out.write("<tr class=\"intfdcolor4\"><td colspan=\"2\">&#160;</td></tr>");
       out.write("<tr>");
       out.write("<td>&#160;</td>");
       out.write("<td>"
           + EncodeHelper.escapeXml(kmeliaScc.getString("PubAucune")) + "</td>");
       out.write("</tr>");
       out.write("</table>");
       out.write(board.printAfter());
     }
     out.write("</Form>");
   }
 
   void displaySortingListBox(ResourcesWrapper resources, Writer out)
       throws IOException {
     out
         .write("<select name=\"sortBy\" onChange=\"javascript:sortGoTo(this.selectedIndex);\">");
     out.write("<option selected=\"selected\">"
         + EncodeHelper.escapeXml(resources.getString("SortBy")) + "</option>");
     out.write("<option>-------------------------------</option>");
     out.write("<option value=\"1\">"
         + EncodeHelper.escapeXml(resources.getString("DateAsc")) + "</option>");
     out
         .write("<option value=\"2\">"
         + EncodeHelper.escapeXml(resources.getString("DateDesc"))
         + "</option>");
     out.write("<option value=\"0\">"
         + EncodeHelper.escapeXml(resources.getString("PubAuteur"))
         + "</option>");
     if (!"no".equals(resources.getSetting("showImportance")))
       out.write("<option value=\"3\">"
           + EncodeHelper.escapeXml(resources.getString("PubImportance"))
           + "</option>");
     out
         .write("<option value=\"4\">"
         + EncodeHelper.escapeXml(resources.getString("PubTitre"))
         + "</option>");
     out.write("</select>");
     out.write("&#160;");
   }
 
   void displayPublicationsListHeader(int nbPubs, boolean sortAllowed,
       Pagination pagination, ResourcesWrapper resources, Writer out)
       throws IOException {
     String publicationSrc = resources.getIcon("kmelia.publication");
     out.write("<tr valign=\"middle\" class=\"intfdcolor\">");
     out.write("<td><img src=\"" + publicationSrc + "\" border=\"0\"/></td>");
     out.write("<td align=\"left\" class=\"ArrayNavigation\">");
     out.write(pagination.printCounter());
     if (nbPubs > 1)
       out
           .write(EncodeHelper
           .escapeXml(resources.getString("GML.publications")));
     else
       out.write(EncodeHelper.escapeXml(resources.getString("GML.publication")));
     out.write("</td>");
 
     out.write("<td align=\"right\">");
     if (sortAllowed) {
       displaySortingListBox(resources, out);
     } else {
       out.write("&#160;");
     }
     out.write("</td>");
     out.write("</tr>");
     out.write("<tr class=\"intfdcolor4\"><td colspan=\"6\">&#160;</td></tr>");
   }
 
   String getUserName(UserPublication userPub, KmeliaSessionController kmeliaScc) {
     UserDetail user = userPub.getOwner(); // contains creator
     PublicationDetail pub = userPub.getPublication();
     String updaterId = pub.getUpdaterId();
     UserDetail updater = null;
     if (updaterId != null && updaterId.length() > 0)
       updater = kmeliaScc.getUserDetail(updaterId);
     if (updater == null)
       updater = user;
 
     String userName = "";
     if (updater != null
         && (updater.getFirstName().length() > 0 || updater.getLastName()
         .length() > 0))
       userName = updater.getDisplayedName();
     else
       userName = kmeliaScc.getString("kmelia.UnknownUser");
 
     return EncodeHelper.escapeXml(userName);
   }
 
   String displayImportance(int importance, int maxImportance, String fullStar,
       String emptyStar, Writer out) throws IOException {
     String stars = "";
 
     // display full Stars
     for (int i = 0; i < importance; i++) {
       stars += "<img src=\"" + fullStar + "\" align=\"absmiddle\"/>";
     }
     // display empty stars
     for (int i = importance + 1; i <= 5; i++) {
       stars += "<img src=\"" + emptyStar + "\" align=\"absmiddle\"/>";
     }
     return stars;
   }
 
   private boolean isDefined(String param) {
     return (param != null && param.length() > 0 && !param.equals("null"));
   }
 
   private void processPublicationsToLink(KmeliaSessionController kmelia,
       HttpServletRequest request) {
     String selectedPubIds = request.getParameter("SelectedIds");
     String notSelectedPubIds = request.getParameter("NotSelectedIds");
 
     List<String> publicationsToLink = kmelia.getPublicationsToLink();
 
     StringTokenizer tokenizer = new StringTokenizer(selectedPubIds, ",");
     String pubId = null;
     while (tokenizer.hasMoreTokens()) {
       pubId = tokenizer.nextToken();
       if (!publicationsToLink.contains(pubId))
         publicationsToLink.add(pubId);
     }
 
     tokenizer = new StringTokenizer(notSelectedPubIds, ",");
     while (tokenizer.hasMoreTokens()) {
       pubId = tokenizer.nextToken();
       publicationsToLink.remove(pubId);
     }
   }
 
   @SuppressWarnings("unchecked")
   private String displayVersioning(PublicationDetail pubDetail, Writer out,
       ResourcesWrapper resources) throws IOException {
     VersioningUtil versioning = new VersioningUtil();
     ForeignPK foreignPK = new ForeignPK(pubDetail.getPK());
     List<Document> documents = versioning.getDocuments(foreignPK);
     Iterator<Document> iterator = documents.iterator();
     StringBuilder result = new StringBuilder();
     String url = "";
     String title = "";
     String info = "";
     String icon;
     String logicalName;
     String size;
     String downloadTime;
     Date creationDate;
     String permalink = null;
     boolean oneFile = false;
     while (iterator.hasNext()) {
       Document document = iterator.next();
       DocumentVersion version = versioning.getLastPublicVersion(document.getPk());
       if (version != null) {
         if (result.length() == 0) {
           result.append("<table border=\"0\">");
           oneFile = true;
         }
 
         title = document.getName() + " v" + version.getMajorNumber();
         info = document.getDescription();
         icon = versioning.getDocumentVersionIconPath(version.getPhysicalName());
         logicalName = version.getLogicalName();
         size = FileRepositoryManager.formatFileSize(version.getSize());
         downloadTime = versioning.getDownloadEstimation(version.getSize());
         creationDate = version.getCreationDate();
         permalink = URLManager.getSimpleURL(URLManager.URL_DOCUMENT, document.getPk().getId());
         url =
             versioning.getDocumentVersionURL(document.getPk().getInstanceId(), logicalName,
             document.getPk().getId(), version.getPk().getId());
 
         result.append(displayFile(url, title, info, icon, logicalName, size, downloadTime,
             creationDate, permalink, out, resources));
       }
     }
     if (oneFile) {
       result.append("</table>");
     }
     return result.toString();
   }
 
   private String displayAttachments(PublicationDetail pubDetail, String userId, String nodeId,
       Writer out, ResourcesWrapper resources) throws IOException {
     SilverTrace.info("kmelia", "AjaxPublicationsListServlet.displayAttachments()",
         "root.MSG_GEN_ENTER_METHOD", "pubId = " + pubDetail.getPK().getId());
     StringBuilder result = new StringBuilder();
 
     // construction d'une AttachmentPK (c'est une foreignKey) � partir de la publication
     AttachmentPK foreignKey =
         new AttachmentPK(pubDetail.getPK().getId(), pubDetail.getPK().getInstanceId());
 
     Collection<AttachmentDetail> attachmentList =
         AttachmentController.searchAttachmentByPKAndContext(foreignKey, "Images");
     Iterator<AttachmentDetail> iterator = attachmentList.iterator();
     if (iterator.hasNext()) {
       result.append("<table border=\"0\">");
       AttachmentDetail attachmentDetail = null;
       String url = "";
       String title = "";
       String info = "";
       String icon;
       String logicalName;
       String id;
       String size;
       String downloadTime;
       Date creationDate;
       String permalink = null;
       while (iterator.hasNext()) {
         attachmentDetail = iterator.next();
         url = attachmentDetail.getAttachmentURLToMemorize(userId, nodeId);
         title = attachmentDetail.getTitle();
         info = attachmentDetail.getInfo();
         icon = attachmentDetail.getAttachmentIcon();
         logicalName = attachmentDetail.getLogicalName();
         id = attachmentDetail.getPK().getId();
         size = attachmentDetail.getAttachmentFileSize();
         downloadTime = attachmentDetail.getAttachmentDownloadEstimation();
         creationDate = attachmentDetail.getCreationDate();
         if (!attachmentDetail.isAttachmentLinked()) {
           permalink = URLManager.getSimpleURL(URLManager.URL_FILE, id);
         }
 
         result.append(displayFile(url, title, info, icon, logicalName, size, downloadTime,
             creationDate, permalink, out, resources));
       }
       result.append("</table>");
     }
     SilverTrace.info("kmelia", "JSPattachmentUtils.displayAttachments()",
         "root.MSG_GEN_EXIT_METHOD", "result = " + result.toString());
     return result.toString();
   }
 
   String displayFile(String url, String title, String info, String icon, String logicalName,
       String size, String downloadTime, Date creationDate, String permalink, Writer out,
       ResourcesWrapper resources) throws IOException {
     SilverTrace.info("kmelia", "AjaxPublicationsListServlet.displayFile()",
         "root.MSG_GEN_ENTER_METHOD");
     StringBuilder result = new StringBuilder();
 
     String link = "<A href=\"" + EncodeHelper.escapeXml(url) + "\" target=\"_blank\">";
     result.append("<TR><TD valign=\"top\">");
     // Add doc type icon
     result.append(link).append("<IMG src=\"").append(icon).append(
         "\" border=\"0\" align=\"absmiddle\"/></A>&#160;</TD>");
     result.append("<TD valign=\"top\">").append(link);
     if (title == null || title.length() == 0)
       result.append(EncodeHelper.escapeXml(logicalName));
     else
       result.append(EncodeHelper.escapeXml(title));
     result.append("</A>");
 
     if (StringUtil.isDefined(permalink)) {
       result.append("&#160;<a href=\"").append(EncodeHelper.escapeXml(permalink)).append(
           "\" target=\"_blank\"><img src=\"").append(resources.getIcon("kmelia.link")).append(
           "\" border=\"0\" valign=\"absmiddle\" alt=\"").append(EncodeHelper.escapeXml(
           resources.getString("toolbox.CopyFileLink"))).append("\" title=\"").append(
           EncodeHelper.escapeXml(resources.getString("toolbox.CopyFileLink"))).append("\"/></a>");
     }
 
     result.append("<br/>");
 
     result.append("<i>");
     if (StringUtil.isDefined(title) && !"no".equals(resources.getSetting("showTitle")))
       result.append(EncodeHelper.escapeXml(logicalName)).append(" / ");
 
     // Add file size
     if (!"no".equals(resources.getSetting("showFileSize")))
       result.append(EncodeHelper.escapeXml(size));
 
     // and download estimation
     if (!"no".equals(resources.getSetting("showDownloadEstimation")))
       result.append(" / ").append(EncodeHelper.escapeXml(downloadTime)).append(" / ").append(
           resources.getOutputDate(creationDate));
 
     result.append("</i>");
 
     // Add info
     if (StringUtil.isDefined(info) && !"no".equals(resources.getSetting("showInfo")))
       result.append("<BR/>").append(
           EncodeHelper.javaStringToHtmlParagraphe(EncodeHelper.escapeXml(info)));
 
     result.append("</TD></TR>");
 
     SilverTrace.info("kmelia", "JSPattachmentUtils.displayFile()", "root.MSG_GEN_EXIT_METHOD");
     return result.toString();
   }
 
   private void displayLastPublications(List<UserPublication> pubs,
       KmeliaSessionController kmeliaScc, ResourcesWrapper resources, GraphicElementFactory gef,
       Writer writer) throws IOException {
 
     boolean displayLinks = URLManager.displayUniversalLinks();
     PublicationDetail pub;
     UserPublication userPub;
     String language = kmeliaScc.getCurrentLanguage();
 
     Iterator<UserPublication> iterator = pubs.iterator();
 
     Board board = gef.getBoard();
     writer.write(board.printBefore());
     writer.write("<table border=\"0\" width=\"98%\" align=\"center\">");
     writer.write("<tr>");
     writer.write("<td width=\"40\" align=\"left\"><img src=\"" +
         resources.getIcon("kmelia.publication") + "\" border=0></td>");
     writer.write("<td align=\"left\" width=\"100%\"><b>" + kmeliaScc.getString("PublicationsLast") +
         "</b></td>");
     writer.write("</tr>");
     if (iterator.hasNext()) {
       writer.write("<tr><td colspan=\"2\">&nbsp;</td></tr>");
       writer.write("<!-- Publications Header End -->");
       writer.write("<tr><td colspan=\"2\"><table border=\"0\" width=\"100%\">");
       int j = 1;
       int nbCol = new Integer(resources.getSetting("HomeNbCols")).intValue();
       if (pubs.size() < nbCol)
         nbCol = pubs.size();
       String width = new Integer(100 / nbCol).toString();
       boolean endRaw = false;
       String linkIcon = resources.getIcon("kmelia.link");
       String shortcut = null;
       while (iterator.hasNext()) {
         if (j == 1) {
           writer.write("<tr>\n");
           writer.write("<td valign=\"top\">&nbsp;</td>\n");
           endRaw = false;
         }
         if (j <= nbCol) {
           userPub = iterator.next();
           pub = userPub.getPublication();
 
           if (!pub.getPK().getInstanceId().equals(kmeliaScc.getComponentId()))
             shortcut = " (" + resources.getString("kmelia.Shortcut") + ")";
           else
             shortcut = "";
 
           writer.write("<!-- Publication Body -->");
           writer.write("<td valign=\"top\" width=\"100\">&#149; </td>");
           writer.write("<td valign=\"top\" width=\"" + width + "%\">");
           writer.write("<p><b><a href=\"javascript:onClick=publicationGoToFromMain('" +
               pub.getPK().getId() + "')\">" +
               EncodeHelper.javaStringToHtmlString(pub.getName(language)) + "</a>" + shortcut +
               "</b><br/>");
 
           if (kmeliaScc.showUserNameInList())
             writer.write(getUserName(userPub, kmeliaScc) + " - ");
           writer.write(resources.getOutputDate(pub.getUpdateDate()));
           if (displayLinks) {
             String link = URLManager.getSimpleURL(URLManager.URL_PUBLI, pub.getPK().getId());
             writer.write(" - <a href=\"" + link + "\"><img src=\"" + linkIcon +
                 "\" border=\"0\" align=\"absmiddle\" alt=\"" +
                 resources.getString("kmelia.CopyPublicationLink") + "\" title=\"" +
                 resources.getString("kmelia.CopyPublicationLink") + "\"></a>");
           }
           writer.write("<br>");
           writer.write(EncodeHelper.javaStringToHtmlParagraphe(pub.getDescription(language)) +
               "<BR><BR></p>");
           writer.write("</td>");
           writer.write("<!-- Publication Body End -->");
           j++;
         }
         if (j > nbCol) {
           writer.write("\t</tr>");
           endRaw = true;
           j = 1;
         }
       }
       if (!endRaw) {
         int nbTd = nbCol - j + 1;
         int k = 1;
         while (k <= nbTd) {
           writer.write("<td colspan=\"3\" valign=\"top\">&nbsp;</td>\n");
           k++;
         }
         writer.write("</tr>\n");
       }
       writer.write("</td></tr></table>");
     } // End if
     else {
       writer.write("<tr>");
       writer.write("<td>&nbsp;</td>");
       writer.write("<td>" + kmeliaScc.getString("PubAucune") + "</td>");
       writer.write("</tr>");
     }
     writer.write("</td>");
     writer.write("</tr>");
     writer.write("</table>");
     writer.write(board.printAfter());
   }
 }
