 /*******************************************************************************
  * JBoss, Home of Professional Open Source
  * Copyright 2010, Red Hat, Inc. and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
  *******************************************************************************/
 package org.richfaces.tests.metamer.ftest.richPanelMenuItem;
 
 import static org.jboss.test.selenium.utils.URLUtils.buildUrl;
 import static org.richfaces.PanelMenuMode.ajax;
 import static org.richfaces.PanelMenuMode.client;
 import static org.richfaces.PanelMenuMode.server;
 
 import java.net.URL;
 
 import org.richfaces.tests.metamer.ftest.AbstractMetamerTest;
 import org.richfaces.tests.metamer.ftest.annotations.Inject;
 import org.richfaces.tests.metamer.ftest.annotations.RegressionTest;
 import org.richfaces.tests.metamer.ftest.annotations.Use;
 import org.richfaces.tests.metamer.ftest.model.PanelMenu;
 import org.testng.annotations.Test;
 
 /**
  * @author <a href="mailto:lfryc@redhat.com">Lukas Fryc</a>
  * @version $Revision$
  */
 @RegressionTest("https://issues.jboss.org/browse/RF-10486")
 public class TestPanelMenuItemClientSideHandlers extends AbstractMetamerTest {
 
     PanelMenuItemAttributes attributes = new PanelMenuItemAttributes();
     PanelMenu menu = new PanelMenu(pjq("div.rf-pm[id$=panelMenu]"));
     PanelMenu.Item item = menu.getGroup(1).getItem(2);
 
     @Inject
     @Use(empty = true)
     String event;
     String[] ajaxEvents = new String[] { "beforeselect", "begin", "beforedomupdate", "select", "complete" };
     String[] clientEvents = new String[] { "beforeselect", "select" };
     String[] serverEvents = new String[] { "select" };
 
     @Override
     public URL getTestUrl() {
         return buildUrl(contextPath, "faces/components/richPanelMenuItem/simple.xhtml");
     }
 
     @Test
     @Use(field = "event", value = "ajaxEvents")
     public void testClientSideEvent() {
         attributes.setMode(ajax);
         menu.setItemMode(ajax);
         super.testRequestEventsBefore(event);
         item.select();
         super.testRequestEventsAfter(event);
     }
 
     @Test
     public void testClientSideEventsOrderClient() {
         attributes.setMode(client);
         menu.setItemMode(client);
         super.testRequestEventsBefore(clientEvents);
         item.select();
         super.testRequestEventsAfter(clientEvents);
     }
 
     @Test
     public void testClientSideEventsOrderAjax() {
         attributes.setMode(ajax);
         menu.setItemMode(ajax);
         super.testRequestEventsBefore(ajaxEvents);
         item.select();
         super.testRequestEventsAfter(ajaxEvents);
     }
 
     @Test
     public void testClientSideEventsOrderServer() {
         attributes.setMode(server);
         menu.setItemMode(server);
         super.testRequestEventsBefore(serverEvents);
         item.select();
         super.testRequestEventsAfter(serverEvents);
     }
 }
