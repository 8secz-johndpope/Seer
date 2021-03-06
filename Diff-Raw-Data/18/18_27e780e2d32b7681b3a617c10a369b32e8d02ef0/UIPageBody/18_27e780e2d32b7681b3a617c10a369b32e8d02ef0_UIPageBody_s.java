 /**
  * Copyright (C) 2009 eXo Platform SAS.
  * 
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  * 
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.exoplatform.portal.webui.page;
 
 import org.exoplatform.container.ExoContainer;
 import org.exoplatform.portal.config.UserPortalConfigService;
 import org.exoplatform.portal.config.model.Page;
 import org.exoplatform.portal.config.model.PageBody;
 import org.exoplatform.portal.config.model.PageNode;
 import org.exoplatform.portal.webui.portal.UIPortal;
 import org.exoplatform.portal.webui.portal.UIPortalComponent;
 import org.exoplatform.portal.webui.util.PortalDataMapper;
 import org.exoplatform.portal.webui.util.Util;
 import org.exoplatform.portal.webui.workspace.UIPortalApplication;
 import org.exoplatform.web.application.ApplicationMessage;
 import org.exoplatform.webui.application.WebuiRequestContext;
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.core.UIComponent;
 import org.exoplatform.webui.core.UIComponentDecorator;
 
 /**
  * May 19, 2006
  */
 @ComponentConfig(template = "system:/groovy/portal/webui/page/UIPageBody.gtmpl")
 public class UIPageBody extends UIComponentDecorator
 {
 
    private UIPortalComponent maximizedUIComponent;
 
    private String storageId;
 
    @SuppressWarnings("unused")
    public UIPageBody(PageBody model) throws Exception
    {
       setId("UIPageBody");
    }
 
    public String getStorageId()
    {
       return storageId;
    }
 
    public void setStorageId(String storageId)
    {
       this.storageId = storageId;
    }
 
    public UIPageBody() throws Exception
    {
       setId("UIPageBody");
    }
 
    @SuppressWarnings("unused")
    public void init(PageBody model) throws Exception
    {
       setId("UIPageBody");
    }
 
    public void setPageBody(PageNode pageNode, UIPortal uiPortal) throws Exception
    {
       WebuiRequestContext context = Util.getPortalRequestContext();
       ExoContainer appContainer = context.getApplication().getApplicationServiceContainer();
       UserPortalConfigService userPortalConfigService =
          (UserPortalConfigService)appContainer.getComponentInstanceOfType(UserPortalConfigService.class);
       Page page = null;
       UIPage uiPage;
       
       String pageReference = null;
       
       if (pageNode != null)
       {
          pageReference = pageNode.getPageReference();
          try
          {
             if (pageReference != null)
             {
                page = userPortalConfigService.getPage(pageReference, context.getRemoteUser());
             }
          }
          catch (Exception e)
          {
             UIPortalApplication uiApp = getAncestorOfType(UIPortalApplication.class);
             uiApp.addMessage(new ApplicationMessage(e.getMessage(), new Object[]{}));
          }
       }
       
       uiPortal.setMaximizedUIComponent(null);
       
       uiPage = getUIPage(pageReference, page, uiPortal, context);
       
       if (uiPage.isShowMaxWindow())
       {
          uiPortal.setMaximizedUIComponent(uiPage);
       }
       else
       {
          UIComponent maximizedComponent = uiPortal.getMaximizedUIComponent();
          if (maximizedComponent != null && maximizedComponent instanceof UIPage)
          {
             uiPortal.setMaximizedUIComponent(null);
          }
          maximizedComponent = this.getMaximizedUIComponent();
          if (maximizedComponent != null && maximizedComponent instanceof UIPage)
          {
             this.setMaximizedUIComponent(null);
          }
       }
       setUIComponent(uiPage);
    }
 
    /**
     * Return cached UIPage or a newly built UIPage
     * 
     * @param pageReference
     * @param page
     * @param uiPortal
     * @return
     */
    private UIPage getUIPage(String pageReference, Page page, UIPortal uiPortal, WebuiRequestContext context)
       throws Exception
    {
       UIPage uiPage = uiPortal.getUIPage(pageReference);
       if (uiPage != null)
       {
          return uiPage;
       }
       
       if(page == null)
       {
          return null;
       }
 
       if (Page.DESKTOP_PAGE.equals(page.getFactoryId()))
       {
          uiPage = createUIComponent(context, UIDesktopPage.class, null, null);
       }
       else
       {
          uiPage = createUIComponent(context, UIPage.class, null, null);
       }
       PortalDataMapper.toUIPage(uiPage, page);
 
       return uiPage;
    }
    
    public void renderChildren() throws Exception
    {
       uicomponent_.processRender((WebuiRequestContext)WebuiRequestContext.getCurrentInstance());
    }
 
    public void processRender(WebuiRequestContext context) throws Exception
    {
       if (maximizedUIComponent != null && Util.getUIPortalApplication().getModeState() % 2 == 0)
       {
          maximizedUIComponent.processRender((WebuiRequestContext)WebuiRequestContext.getCurrentInstance());
          return;
       }
       if (uicomponent_ == null)
       {
          setPageBody(Util.getUIPortal().getSelectedNode(), Util.getUIPortal());
       }
       super.processRender(context);
    }
 
    public UIPortalComponent getMaximizedUIComponent()
    {
       return maximizedUIComponent;
    }
 
    public void setMaximizedUIComponent(UIPortalComponent uiMaximizedComponent)
    {
       this.maximizedUIComponent = uiMaximizedComponent;
    }
 
 }
