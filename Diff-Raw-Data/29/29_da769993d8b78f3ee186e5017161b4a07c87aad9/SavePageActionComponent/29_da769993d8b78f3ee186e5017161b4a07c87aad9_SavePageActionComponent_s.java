 /*
  * Copyright (C) 2003-2010 eXo Platform SAS.
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Affero General Public License
  * as published by the Free Software Foundation; either version 3
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, see<http://www.gnu.org/licenses/>.
  */
 package org.exoplatform.wiki.webui.control.action;
 
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.List;
 
 import org.exoplatform.container.PortalContainer;
 import org.exoplatform.portal.webui.util.Util;
 import org.exoplatform.services.jcr.datamodel.IllegalNameException;
 import org.exoplatform.services.log.ExoLogger;
 import org.exoplatform.services.log.Log;
 import org.exoplatform.web.application.ApplicationMessage;
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.config.annotation.EventConfig;
 import org.exoplatform.webui.core.UIApplication;
 import org.exoplatform.webui.core.UIComponent;
 import org.exoplatform.webui.event.Event;
 import org.exoplatform.webui.event.Event.Phase;
 import org.exoplatform.webui.ext.filter.UIExtensionFilter;
 import org.exoplatform.webui.ext.filter.UIExtensionFilters;
 import org.exoplatform.webui.form.UIFormSelectBox;
 import org.exoplatform.webui.form.UIFormStringInput;
 import org.exoplatform.webui.form.UIFormTextAreaInput;
 import org.exoplatform.wiki.commons.NameValidator;
 import org.exoplatform.wiki.commons.Utils;
 import org.exoplatform.wiki.mow.api.Page;
 import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
 import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
 import org.exoplatform.wiki.rendering.RenderingService;
 import org.exoplatform.wiki.resolver.TitleResolver;
 import org.exoplatform.wiki.service.WikiPageParams;
 import org.exoplatform.wiki.service.WikiService;
 import org.exoplatform.wiki.webui.EditMode;
 import org.exoplatform.wiki.webui.UIWikiPageControlArea;
 import org.exoplatform.wiki.webui.UIWikiPageEditForm;
 import org.exoplatform.wiki.webui.UIWikiPageTitleControlArea;
 import org.exoplatform.wiki.webui.UIWikiPortlet;
 import org.exoplatform.wiki.webui.UIWikiRichTextArea;
 import org.exoplatform.wiki.webui.WikiMode;
 import org.exoplatform.wiki.webui.control.filter.IsEditAddModeFilter;
 import org.exoplatform.wiki.webui.control.filter.IsEditAddPageModeFilter;
 import org.exoplatform.wiki.webui.control.listener.UIPageToolBarActionListener;
 import org.xwiki.rendering.syntax.Syntax;
 
 /**
  * Created by The eXo Platform SAS
  * Author : viet nguyen
  *          viet.nguyen@exoplatform.com
  * Apr 26, 2010  
  */
 @ComponentConfig(
   template = "app:/templates/wiki/webui/control/action/SavePageActionComponent.gtmpl",
   events = {
     @EventConfig(listeners = SavePageActionComponent.SavePageActionListener.class, phase = Phase.DECODE)
   }
 )
 public class SavePageActionComponent extends UIComponent {
 
   public static final String                   ACTION   = "SavePage";  
   
   private static final Log log = ExoLogger.getLogger("wiki:SavePageActionComponent");
   
   private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
       new IsEditAddModeFilter(), new IsEditAddPageModeFilter() });
 
   @UIExtensionFilters
   public List<UIExtensionFilter> getFilters() {
     return FILTERS;
   }  
 
   private boolean isNewMode() {
     return (WikiMode.ADDPAGE.equals(getAncestorOfType(UIWikiPortlet.class).getWikiMode()));
   }
   
   private String getPageTitleInputId() {
     return UIWikiPageTitleControlArea.FIELD_TITLEINPUT;
   }
   
   private String getActionLink() throws Exception {
     return Utils.createFormActionLink(this, ACTION, ACTION);
   }  
   
   public static class SavePageActionListener extends
                                             UIPageToolBarActionListener<SavePageActionComponent> {
     @Override
     protected void processEvent(Event<SavePageActionComponent> event) throws Exception {
       WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
       UIWikiPortlet wikiPortlet = event.getSource().getAncestorOfType(UIWikiPortlet.class);
       WikiPageParams pageParams = Utils.getCurrentWikiPageParams();
       UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
       UIWikiPageTitleControlArea pageTitleControlForm = wikiPortlet.findComponentById(UIWikiPageControlArea.TITLE_CONTROL);
       UIWikiPageEditForm pageEditForm = wikiPortlet.findFirstComponentOfType(UIWikiPageEditForm.class);
       UIWikiRichTextArea wikiRichTextArea = pageEditForm.getChild(UIWikiRichTextArea.class);
       UIFormStringInput titleInput = pageEditForm.getChild(UIWikiPageTitleControlArea.class)
                                                  .getUIStringInput();      
       UIFormTextAreaInput markupInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_CONTENT);
       UIFormStringInput commentInput = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_COMMENT);
       UIFormSelectBox syntaxTypeSelectBox = pageEditForm.findComponentById(UIWikiPageEditForm.FIELD_SYNTAX);
       RenderingService renderingService = (RenderingService) PortalContainer.getComponent(RenderingService.class);
       Page page = Utils.getCurrentWikiPage();
       Utils.setUpWikiContext(wikiPortlet);
       try {
         NameValidator.validate(titleInput.getValue());
       } catch (IllegalNameException ex) {
         String msg = ex.getMessage();
         ApplicationMessage appMsg = new ApplicationMessage("WikiPageNameValidator.msg.EmptyTitle",
                                                            null,
                                                            ApplicationMessage.WARNING);
         if (msg != null) {
           Object[] arg = { msg };
           appMsg = new ApplicationMessage("WikiPageNameValidator.msg.Invalid-char",
                                           arg,
                                           ApplicationMessage.WARNING);
         }
         uiApp.addMessage(appMsg);
         event.getRequestContext().setProcessRender(true);
       }
       if (event.getRequestContext().getProcessRender()) {
         event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
         Utils.redirect(pageParams, wikiPortlet.getWikiMode());
         return;
       }
 
       String title = titleInput.getValue().trim();
       if (wikiRichTextArea.isRendered()) {
         String htmlContent = wikiRichTextArea.getUIFormTextAreaInput().getValue();
         String markupContent = renderingService.render(htmlContent,
                                                        Syntax.XHTML_1_0.toIdString(),
                                                        syntaxTypeSelectBox.getValue(),
                                                        false);
         markupInput.setValue(markupContent);
       }
       String markup = (markupInput.getValue() == null) ? "" : markupInput.getValue();
       markup = markup.trim();
       String syntaxId = syntaxTypeSelectBox.getValue();
       
      String pageId = TitleResolver.getId(title, false);
      if (!pageId.equals(page.getName()) && wikiService.isExisting(pageParams.getType(),
                                                                   pageParams.getOwner(),
                                                                   pageId)) {
        // if page title is not changed or duplicated with existed page's after edited.
         if (log.isDebugEnabled()) log.debug("The title '" + title + "' is already existing!");
         uiApp.addMessage(new ApplicationMessage("SavePageAction.msg.warning-page-title-already-exist",
                                                 null,
                                                 ApplicationMessage.WARNING));
         event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
         Utils.redirect(pageParams, wikiPortlet.getWikiMode());
         return;
       }
       
       try {
         if (wikiPortlet.getWikiMode() == WikiMode.EDITPAGE) {
           if (wikiPortlet.getEditMode() == EditMode.SECTION) {
            pageId = page.getName();
             title = page.getTitle();
             markup = renderingService.updateContentOfSection(page.getContent().getText(),
                                                              page.getSyntax(),
                                                              wikiPortlet.getSectionIndex(),
                                                              markup);
           }
          if (!page.getName().equals(pageId)) {
             wikiService.renamePage(pageParams.getType(),
                                    pageParams.getOwner(),
                                    page.getName(),
                                   pageId,
                                    title);
           }
           Object minorAtt = event.getRequestContext().getAttribute(MinorEditActionComponent.ACTION);
           if (minorAtt != null) {
             ((PageImpl) page).setMinorEdit(Boolean.parseBoolean(minorAtt.toString()));
           }
 
           page.setComment(commentInput.getValue());
           page.setSyntax(syntaxId);
           pageTitleControlForm.getUIFormInputInfo().setValue(title);
           pageParams.setPageId(page.getName());
           ((PageImpl) page).setURL(Utils.getURLFromParams(pageParams));          
           page.getContent().setText(markup);
 
           if (!pageEditForm.getTitle().equals(title)) {
             page.setTitle(title);
             ((PageImpl) page).checkin();
             ((PageImpl) page).checkout();
            pageParams.setPageId(pageId);
           } else {
             ((PageImpl) page).checkin();
             ((PageImpl) page).checkout();
           }
         } else if (wikiPortlet.getWikiMode() == WikiMode.ADDPAGE) {
           String sessionId = Util.getPortalRequestContext().getRequest().getSession(false).getId();
           Page draftPage = wikiService.getExsitedOrNewDraftPageById(null, null, sessionId);
           Collection<AttachmentImpl> attachs = ((PageImpl) draftPage).getAttachments();
 
           Page subPage = wikiService.createPage(pageParams.getType(),
                                                 pageParams.getOwner(),
                                                 title,
                                                 page.getName());
           pageParams.setPageId(page.getName());
           ((PageImpl) page).setURL(Utils.getURLFromParams(pageParams));
           subPage.getContent().setText(markup);
           subPage.setSyntax(syntaxId);
           ((PageImpl) subPage).getAttachments().addAll(attachs);
           ((PageImpl) subPage).checkin();
           ((PageImpl) subPage).checkout();
          pageParams.setPageId(pageId);
           ((PageImpl) draftPage).remove();
           return;
         }
       } catch (Exception e) {
         log.error("An exception happens when saving the page with title:" + title, e);
         uiApp.addMessage(new ApplicationMessage("UIPageToolBar.msg.Exception",
                                                 null,
                                                 ApplicationMessage.ERROR));
         event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
       } finally {
         wikiPortlet.changeMode(WikiMode.VIEW);
         Utils.redirect(pageParams, WikiMode.VIEW);
         super.processEvent(event);
       }
     }
   }
 
 }
