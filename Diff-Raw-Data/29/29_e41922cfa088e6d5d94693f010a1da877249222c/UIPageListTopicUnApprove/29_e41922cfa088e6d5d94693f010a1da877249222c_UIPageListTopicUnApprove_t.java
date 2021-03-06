 /*
  * Copyright (C) 2003-2008 eXo Platform SAS.
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Affero General Public License
  * as published by the Free Software Foundation; either version 3
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, see<http://www.gnu.org/licenses/>.
  */
 package org.exoplatform.forum.webui.popup;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.exoplatform.container.PortalContainer;
 import org.exoplatform.forum.ForumSessionUtils;
 import org.exoplatform.forum.service.ForumService;
 import org.exoplatform.forum.service.Topic;
 import org.exoplatform.forum.service.UserProfile;
 import org.exoplatform.forum.webui.UIForumKeepStickPageIterator;
 import org.exoplatform.forum.webui.UIForumPortlet;
 import org.exoplatform.forum.webui.UITopicContainer;
 import org.exoplatform.services.jcr.ext.common.SessionProvider;
 import org.exoplatform.web.application.ApplicationMessage;
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.config.annotation.EventConfig;
 import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
 import org.exoplatform.webui.event.Event;
 import org.exoplatform.webui.event.EventListener;
 import org.exoplatform.webui.event.Event.Phase;
 import org.exoplatform.webui.exception.MessageException;
 import org.exoplatform.webui.form.UIFormCheckBoxInput;
 
 /**
  * Created by The eXo Platform SAS
  * Author : Vu Duy Tu
  *					tu.duy@exoplatform.com
  * 05-03-2008	
  */
 
 @ComponentConfig(
 		lifecycle = UIFormLifecycle.class,
 		template =	"app:/templates/forum/webui/popup/UIPageListTopicUnApprove.gtmpl",
 		events = {
 				@EventConfig(listeners = UIPageListTopicUnApprove.OpenTopicActionListener.class ),
 				@EventConfig(listeners = UIPageListTopicUnApprove.ApproveTopicActionListener.class ),
 				@EventConfig(listeners = UIForumKeepStickPageIterator.GoPageActionListener.class),
 				@EventConfig(listeners = UIPageListTopicUnApprove.CancelActionListener.class,phase = Phase.DECODE )
 		}
 )
 public class UIPageListTopicUnApprove extends UIForumKeepStickPageIterator implements UIPopupComponent {
 	private ForumService forumService ;
 	private String categoryId, forumId ;
 	private List<Topic> allTopics ;
 	public UIPageListTopicUnApprove() throws Exception {
 		forumService = (ForumService)PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class) ;
 		this.setActions(new String[]{"ApproveTopic","Cancel"});
 	}
 	public void activate() throws Exception {	}
 	public void deActivate() throws Exception {	}
 	
 	@SuppressWarnings("unused")
 	private UserProfile getUserProfile() throws Exception {
 		return this.getAncestorOfType(UIForumPortlet.class).getUserProfile() ;
 	}
 	
 	public void setUpdateContainer(String categoryId, String forumId) {
 		this.categoryId = categoryId ; this.forumId = forumId ;
 	}
 	
 	@SuppressWarnings({ "unchecked", "unused" })
 	private List<Topic> getTopicsUnApprove() throws Exception {
 		pageList	= forumService.getPageTopic(ForumSessionUtils.getSystemProvider(), this.categoryId, this.forumId, "@exo:isApproved='false'", "") ;
 		pageList.setPageSize(6) ;
		maxPage = pageList.getAvailablePage();
 		List<Topic> topics = pageList.getPage(pageSelect);
		System.out.println("\n\nmaxPage: " + maxPage);
		System.out.println("\n\nSelect1: " + pageSelect);
 		pageSelect = pageList.getCurrentPage();
		System.out.println("\n\nSelect2: " + pageSelect);
 		if(topics == null) topics = new ArrayList<Topic>(); 
 		for (Topic topic : topics) {
 			if(getUIFormCheckBoxInput(topic.getId()) != null) {
 				getUIFormCheckBoxInput(topic.getId()).setChecked(false) ;
 			}else {
 				addUIFormInput(new UIFormCheckBoxInput(topic.getId(), topic.getId(), false) );
 			}
 		}
 		this.allTopics = pageList.getPage(0) ;
 		return topics ;
 	}
 	
 	private Topic getTopic(String topicId) throws Exception {
 		List<Topic> listTopic = this.allTopics ;
 		for (Topic topic : listTopic) {
 			if(topic.getId().equals(topicId)) return topic ;
 		}
 		return null ;
 	}
 	
 	
 	static	public class OpenTopicActionListener extends EventListener<UIPageListTopicUnApprove> {
 		public void execute(Event<UIPageListTopicUnApprove> event) throws Exception {
 		}
 	}
 
 	
 	static	public class ApproveTopicActionListener extends EventListener<UIPageListTopicUnApprove> {
 		public void execute(Event<UIPageListTopicUnApprove> event) throws Exception {
 			UIPageListTopicUnApprove topicUnApprove = event.getSource() ;
 			List<Topic> listTopic = new ArrayList<Topic>() ;
 			for(String topicId : topicUnApprove.getIdSelected()) {
 				Topic topic = topicUnApprove.getTopic(topicId) ;
 				if (topic != null) {
 					topic.setIsApproved(true);
 					listTopic.add(topic) ;
 				}
 			}
 			if(!listTopic.isEmpty()) {
 				SessionProvider sProvider = ForumSessionUtils.getSystemProvider() ;
 				try {
 					topicUnApprove.forumService.modifyTopic(sProvider, listTopic, 3) ;
 				} finally {
 					sProvider.close();
 				}
 			} else {
 				Object[] args = { };
 				throw new MessageException(new ApplicationMessage("UIPageListTopicUnApprove.sms.notCheck", args, ApplicationMessage.WARNING)) ;
 			}
 			if(listTopic.size() == topicUnApprove.allTopics.size()) {
 				UIForumPortlet forumPortlet = topicUnApprove.getAncestorOfType(UIForumPortlet.class) ;
 				forumPortlet.cancelAction() ;
 				UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(topicContainer) ;
 			}else{
 				event.getRequestContext().addUIComponentToUpdateByAjax(topicUnApprove.getParent()) ;
 			}
 		}
 	}
 	
 	static	public class CancelActionListener extends EventListener<UIPageListTopicUnApprove> {
 		public void execute(Event<UIPageListTopicUnApprove> event) throws Exception {
 			UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class) ;
 			forumPortlet.cancelAction() ;
 			UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class) ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(topicContainer) ;
 		}
 	}
 }
