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
 package org.exoplatform.wiki.webui;
 
 import java.util.Arrays;
 
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
 import org.exoplatform.wiki.webui.core.UIWikiContainer;
 
 /**
  * Created by The eXo Platform SAS
  * Author : Lai Trung Hieu
  *          hieu.lai@exoplatform.com
  * 7 Dec 2010  
  */
 @ComponentConfig(
                  lifecycle = UIApplicationLifecycle.class,
                  template = "app:/templates/wiki/webui/UIWikiMiddleArea.gtmpl"
                )
 public class UIWikiMiddleArea extends UIWikiContainer {
 
   public UIWikiMiddleArea() throws Exception {    
     super();
     this.accept_Modes = Arrays.asList(new WikiMode[] { WikiMode.VIEW, WikiMode.EDITPAGE,
         WikiMode.ADDPAGE, WikiMode.ADVANCEDSEARCH, WikiMode.SHOWHISTORY, WikiMode.PAGE_NOT_FOUND,
        WikiMode.DELETECONFIRM, WikiMode.VIEWREVISION });
     // TODO Auto-generated constructor stub
     addChild(UIWikiNavigationContainer.class, null, null);
     addChild(UIWikiPageContainer.class, null, null);    
   }
 
 }
