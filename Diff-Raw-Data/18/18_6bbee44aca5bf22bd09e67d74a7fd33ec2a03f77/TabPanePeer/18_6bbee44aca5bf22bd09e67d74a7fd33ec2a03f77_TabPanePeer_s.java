 /* 
  * This file is part of the Echo Extras Project.
  * Copyright (C) 2005-2007 NextApp, Inc.
  *
  * Version: MPL 1.1/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * Alternatively, the contents of this file may be used under the terms of
  * either the GNU General Public License Version 2 or later (the "GPL"), or
  * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the MPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the MPL, the GPL or the LGPL.
  */
 
 package nextapp.echo.extras.webcontainer.sync.component;
 
 import nextapp.echo.app.Component;
 import nextapp.echo.app.update.ClientUpdateManager;
 import nextapp.echo.app.util.Context;
 import nextapp.echo.extras.app.TabPane;
 import nextapp.echo.extras.webcontainer.service.CommonService;
 import nextapp.echo.webcontainer.AbstractComponentSynchronizePeer;
 import nextapp.echo.webcontainer.ComponentSynchronizePeer;
 import nextapp.echo.webcontainer.LazyRenderContainer;
 import nextapp.echo.webcontainer.ServerMessage;
 import nextapp.echo.webcontainer.Service;
 import nextapp.echo.webcontainer.UserInstance;
 import nextapp.echo.webcontainer.WebContainerServlet;
 import nextapp.echo.webcontainer.service.JavaScriptService;
 
 /**
  * Synchronization peer for <code>TabPane</code>s.
  * 
  * @author n.beekman
  */
 public class TabPanePeer extends AbstractComponentSynchronizePeer implements LazyRenderContainer {
 
     private static final String PROPERTY_ACTIVE_TAB = "activeTab";
     private static final String PROPERTY_CLOSE_TAB = "closeTab";
 
     /**
      * Component property to enabled/disable lazy rendering of child tabs.
      * Default value is interpreted to be true.
      */
     public static final String PROPERTY_LAZY_RENDER_ENABLED = "lazyRenderEnabled";
 
     private static final Service TAB_PANE_SERVICE = JavaScriptService.forResources("EchoExtras.TabPane",
             new String[] {  "/nextapp/echo/extras/webcontainer/resource/js/Application.TabPane.js",  
                             "/nextapp/echo/extras/webcontainer/resource/js/Render.TabPane.js"});
 
     static {
         WebContainerServlet.getServiceRegistry().add(TAB_PANE_SERVICE);
     }
 
     public TabPanePeer() {
         addOutputProperty(PROPERTY_ACTIVE_TAB);
     }
     
     /**
      * @see nextapp.echo.webcontainer.ComponentSynchronizePeer#getComponentClass()
      */
     public Class getComponentClass() {
         return TabPane.class;
     }
 
     public String getClientComponentType() {
         return "ExtrasApp.TabPane";
     }
     
     /**
      * @see nextapp.echo.webcontainer.ComponentSynchronizePeer#init(nextapp.echo.app.util.Context)
      */
     public void init(Context context) {
         ServerMessage serverMessage = (ServerMessage) context.get(ServerMessage.class);
         serverMessage.addLibrary(CommonService.INSTANCE.getId());
         serverMessage.addLibrary(TAB_PANE_SERVICE.getId());
     }
 
     /**
      * @see ComponentSynchronizePeer#getPropertyClass(String)
      */
     public Class getPropertyClass(String propertyName) {
         if (PROPERTY_ACTIVE_TAB.equals(propertyName)) {
             return String.class;
         } else if (PROPERTY_CLOSE_TAB.equals(propertyName)) {
             return String.class;
         }
         return super.getPropertyClass(propertyName);
     }
 
     /**
      * @see ComponentSynchronizePeer#getOutputProperty(Context, Component, String, int)
      */
     public Object getOutputProperty(Context context, Component component, String propertyName, int propertyIndex) {
         if (PROPERTY_ACTIVE_TAB.equals(propertyName)) {
             TabPane tabPane = (TabPane) component;
             int componentCount = tabPane.getVisibleComponentCount();
             if (componentCount == 0) {
                 return null;
             }
             Component activeTab;
             int activeTabIndex = tabPane.getActiveTabIndex();
             if (activeTabIndex == -1) {
                 activeTab = tabPane.getVisibleComponent(0);
             } else if (activeTabIndex < componentCount) {
                 activeTab = tabPane.getVisibleComponent(activeTabIndex);
             } else {
                 activeTab = tabPane.getVisibleComponent(componentCount - 1);
             }
             return UserInstance.getElementId(activeTab);
         } else {
             return super.getOutputProperty(context, component, propertyName, propertyIndex);
         }
     }
     
     /**
      * @see ComponentSynchronizePeer#storeInputProperty(Context, Component, String, int, Object)
      */
     public void storeInputProperty(Context context, Component component, String propertyName, int index, Object newValue) {
         if (PROPERTY_ACTIVE_TAB.equals(propertyName) || PROPERTY_CLOSE_TAB.equals(propertyName)) {
             Component[] children = component.getVisibleComponents();
             for (int i = 0; i < children.length; ++i) {
                 if (UserInstance.getElementId(children[i]).equals(newValue)) {
                     ClientUpdateManager clientUpdateManager = (ClientUpdateManager) context.get(ClientUpdateManager.class);
                     if (PROPERTY_ACTIVE_TAB.equals(propertyName)) {
                         clientUpdateManager.setComponentProperty(component, TabPane.INPUT_TAB_INDEX, new Integer(i));
                     } else if (PROPERTY_CLOSE_TAB.equals(propertyName)) {
                         clientUpdateManager.setComponentProperty(component, TabPane.INPUT_TAB_CLOSE, new Integer(i));
                     }
                     return;
                 }
             }
         }
     }
     
     /**
      * @see LazyRenderContainer#isRendered(Context, Component, Component)
      */
     public boolean isRendered(Context context, Component component, Component child) {
         // FIXME implement lazy behavior
         return true;
     }
 }
