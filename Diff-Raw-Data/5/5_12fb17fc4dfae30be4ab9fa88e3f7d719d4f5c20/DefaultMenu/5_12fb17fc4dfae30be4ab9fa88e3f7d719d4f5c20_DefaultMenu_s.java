 package org.wyona.yanel.servlet.menu.impl;
 
 import org.wyona.yanel.core.Resource;
 import org.wyona.yanel.core.api.attributes.VersionableV2;
 import org.wyona.yanel.core.api.attributes.WorkflowableV1;
 import org.wyona.yanel.core.attributes.versionable.RevisionInformation;
 import org.wyona.yanel.core.map.Map;
 import org.wyona.yanel.core.util.ResourceAttributeHelper;
 import org.wyona.yanel.core.workflow.Transition;
 import org.wyona.yanel.core.workflow.Workflow;
 import org.wyona.yanel.core.workflow.WorkflowHelper;
 
 import org.wyona.yanel.servlet.menu.Menu;
 import org.wyona.yanel.servlet.menu.impl.RevisionInformationMenuItem;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 
 import java.io.IOException;
 
import org.apache.log4j.Category;
 
 /**
  *
  */
 public class DefaultMenu extends Menu {
 
    private static Category log = Category.getInstance(DefaultMenu.class);
 
     /**
      * Get toolbar menus
      */
     public  String getMenus(Resource resource, HttpServletRequest request, Map map, String reservedPrefix) throws ServletException, IOException, Exception {
         return getFileMenu(resource) + getEditMenu(resource);
     }
 
     /**
      * Get generic edit menu
      */
     public String getEditMenu(Resource resource) throws Exception {
         StringBuilder sb = new StringBuilder();
         sb.append("<ul><li>");
         sb.append("<div id=\"yaneltoolbar_menutitle\">Edit</div>");
         sb.append("<ul>");
 
         String backToRealm = org.wyona.yanel.core.util.PathUtil.backToRealm(resource.getPath());
         sb.append("<li class=\"haschild\">Open with&#160;&#160;&#160;");
         sb.append("<ul><li>Source editor</li>");
         sb.append("<li class=\"haschild\">WYSIWYG editor&#160;&#160;&#160;");
         sb.append("<ul>");
         if (ResourceAttributeHelper.hasAttributeImplemented(resource, "Modifiable", "2")) {
             sb.append("<li><a href=\"" + backToRealm + "usecases/xinha.html?edit-path=" + resource.getPath() + "\">Edit page with Xinha&#160;&#160;&#160;</a></li>");
             sb.append("<li><a href=\"" + backToRealm + "usecases/tinymce.html?edit-path=" + resource.getPath() + "\">Edit page with tinyMCE&#160;&#160;&#160;</a></li>");
         } else {
             sb.append("<li><a>Edit page with Xinha&#160;&#160;&#160;</a></li>");
             sb.append("<li>Edit page with tinyMCE&#160;&#160;&#160;</li>");
         }
         sb.append("<li><a href=\"http://www.yulup.org\">Edit page with Yulup&#160;&#160;&#160;</a></li>");
         sb.append("</ul>");
         sb.append("</li>");
         sb.append("</ul>");
         sb.append("</li>");
 
         sb.append("</ul>");
         sb.append("</li></ul>");
 
         return sb.toString();
     }
 
     /**
      * Get generic file menu
      */
     public String getFileMenu(Resource resource) throws Exception {
         StringBuffer sb = new StringBuffer();
         sb.append("<ul><li>");
         sb.append("<div id=\"yaneltoolbar_menutitle\">File</div>");
         sb.append("<ul>");
         sb.append("<li><a href=\"create-new-page.html\">Create new page</a></li>");
         if (ResourceAttributeHelper.hasAttributeImplemented(resource, "Workflowable", "1")) {
             Workflow wf = WorkflowHelper.getWorkflow(resource);
             if (wf != null) {
                 sb.append("<li class=\"haschild\">Revisions and Workflow&#160;&#160;&#160;<ul>");
                 if (ResourceAttributeHelper.hasAttributeImplemented(resource, "Versionable", "2")) {
                     RevisionInformation[] revisions = ((VersionableV2)resource).getRevisions();
                     if (revisions != null && revisions.length > 0) {
                         for (int i = revisions.length - 1; i >= 0; i--) {
                             // TODO: Add revision date and workflow state and as children the transitions
                             String wfState = ((WorkflowableV1)resource).getWorkflowState(revisions[i].getName());
                             if (wfState == null) wfState = wf.getInitialState();
                             Transition[] transitions = wf.getLeavingTransitions(wfState);
                             if (transitions.length > 0) {
                                 sb.append("<li class=\"haschild\">");
                             } else {
                                 sb.append("<li>");
                             }
                             sb.append("R: " + revisions[i].getName() + " ("+revisions[i].getDate()+"), WS: " + wfState + " (" + ((WorkflowableV1)resource).getWorkflowDate(revisions[i].getName()) + ")&#160;&#160;&#160;");
                             if (transitions.length > 0) {
                                 sb.append("<ul>");
                                 for (int j = 0; j < transitions.length; j++) {
                                     sb.append("<li><a href=\"?yanel.resource.workflow.transition=" + transitions[j].getID() + "&amp;yanel.resource.revision=" + revisions[i].getName() + "\">Transition: " + transitions[j].getDescription("en") + "</a></li>");
                                 }
                                 sb.append("</ul>");
                             }
                             sb.append("</li>");
                         }
                     } else {
                         log.warn("Has no revisions!");
                     }
                 } else {
                     log.warn("Does not implement interface VersionableV2!");
                 }
                 sb.append("</ul></li>");
             } else {
                 sb.append("<li>Workflowable, but no Workflow associated with resource yet!</li>");
             }
         }
         if (ResourceAttributeHelper.hasAttributeImplemented(resource, "Versionable", "2")) {
             RevisionInformation[] revisions = ((VersionableV2) resource).getRevisions();
             if (revisions !=  null && revisions.length > 0) {
                 sb.append("<li class=\"haschild\">Revisions&#160;&#160;&#160;<ul>");
                 for (int i = revisions.length -1; i >= 0; i--) {
                     boolean mostRecent = false;
                     boolean oldestRevision = false;
                     if (i == revisions.length - 1) mostRecent = true;
                     if (i == 0) oldestRevision = true;
                     sb.append((new RevisionInformationMenuItem(resource,
                                                                revisions[i],
                                                                resource.getRequestedLanguage())).toHTML(mostRecent, oldestRevision));
                 }
                 sb.append("</ul></li>");
             }
         } else {
             log.info("This resource does not implement interface VersionableV2!");
         }
         if (ResourceAttributeHelper.hasAttributeImplemented(resource, "Modifiable", "2")) {
             sb.append("<li><a href=\"?yanel.resource.usecase=delete\">Delete this page</a></li>");
         }
         sb.append("</ul>");
         sb.append("</li></ul>");
 
         return sb.toString();
     }
 }
