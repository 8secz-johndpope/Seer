 /***************************************************************************
  * Copyright (C) 2003-2007 eXo Platform SAS.
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
  ***************************************************************************/
 package org.exoplatform.forum.webui.popup;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.ResourceBundle;
 
 import javax.jcr.ItemExistsException;
 
 import org.exoplatform.container.ExoContainerContext;
 import org.exoplatform.forum.ForumUtils;
 import org.exoplatform.forum.service.Category;
 import org.exoplatform.forum.service.Forum;
 import org.exoplatform.forum.service.ForumService;
 import org.exoplatform.forum.service.Topic;
 import org.exoplatform.forum.service.UserProfile;
 import org.exoplatform.forum.webui.UIForumContainer;
 import org.exoplatform.forum.webui.UIForumDescription;
 import org.exoplatform.forum.webui.UIForumPortlet;
 import org.exoplatform.forum.webui.UITopicContainer;
 import org.exoplatform.forum.webui.UITopicDetail;
 import org.exoplatform.forum.webui.UITopicDetailContainer;
 import org.exoplatform.ks.common.webui.BaseEventListener;
 import org.exoplatform.ks.common.webui.BaseUIForm;
 import org.exoplatform.webui.application.WebuiRequestContext;
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.config.annotation.EventConfig;
 import org.exoplatform.webui.core.UIPopupComponent;
 import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
 import org.exoplatform.webui.event.Event;
 import org.exoplatform.webui.event.EventListener;
 import org.exoplatform.webui.event.Event.Phase;
 /**
  * Created by The eXo Platform SARL
  * Author : Vu Duy Tu
  *					tu.duy@exoplatform.com
  * Aus 15, 2007 2:48:18 PM 
  */
 @ComponentConfig(
 		lifecycle = UIFormLifecycle.class,
 		template = "app:/templates/forum/webui/popup/UIMoveTopicForm.gtmpl",
 		events = {
 			@EventConfig(listeners = UIMoveTopicForm.SaveActionListener.class), 
 			@EventConfig(listeners = UIMoveTopicForm.CancelActionListener.class,phase = Phase.DECODE)
 		}
 )
 public class UIMoveTopicForm extends BaseUIForm implements UIPopupComponent {
 	private ForumService forumService ;
 	private String forumId ;
 	private List<Topic> topics ;
 	private List<Category> categories;
 	private boolean isFormTopic = false ;
 	private boolean isAdmin = false;
 	private UserProfile userProfile ;
 	private String link = "";
 	private String pathTopic = "";
 	
 	public String getLink() {return link;}
 	public void setLink(String link) {this.link = link;}
 	
 	public boolean isAdmin() {
   	return isAdmin;
   }
 	public void setAdmin(boolean isAdmin) {
   	this.isAdmin = isAdmin;
   }
 	
 	public UserProfile getUserProfile() throws Exception {
 	  return this.userProfile ;
   }
 	public void setUserProfile(UserProfile userProfile) throws Exception {
 	  this.userProfile = userProfile ;
 	  if(this.userProfile == null) {
 	  	this.userProfile = this.getAncestorOfType(UIForumPortlet.class).getUserProfile();
 	  }
   }
 	
 	public UIMoveTopicForm() throws Exception {
 		forumService = (ForumService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class) ;
 	}
 	public void activate() throws Exception {}
 	public void deActivate() throws Exception {}
 	
 	public void updateTopic(String forumId, List<Topic> topics, boolean isFormTopic) throws Exception {
 		this.forumId = forumId ;
 		this.topics = topics ;
 		try {
 			this.pathTopic = topics.get(0).getPath();
     } catch (Exception e) {}
 		this.isFormTopic = isFormTopic ;
 		setCategories() ;
 	}
 	
 	private void setCategories() throws Exception {
 		this.categories = new ArrayList<Category>();
 		for (Category category : this.forumService.getCategories()) {
 			if(this.userProfile.getUserRole() == 1) {
 				String []list = category.getUserPrivate() ; 
 				if(list != null && list.length > 0 && !list[0].equals(" ")){
 					if(!ForumUtils.isStringInStrings(list, this.userProfile.getUserId())) {
 						continue ;
 					}
 				}
 			}
 			categories.add(category) ;
 		}
 	}
 	@SuppressWarnings("unused")
 	private List<Category> getCategories() throws Exception {
 		return  this.categories;
 	}
 	
 	@SuppressWarnings("unused")
 	private boolean getSelectCate(String cateId) throws Exception {
 		if(this.topics.get(0).getPath().contains(cateId)) return true ;
 		else return false ;
 	}
 	
 	@SuppressWarnings("unused")
 	private List<Forum> getForums(String categoryId) throws Exception {
 		List<Forum> forums = new ArrayList<Forum>() ;
		for(Forum forum : this.forumService.getForums(categoryId, "")) {
 			if(forum.getId().equalsIgnoreCase(this.forumId)) {
 				if(pathTopic.indexOf(categoryId) >= 0)	continue ;
 			}
 			if(this.userProfile.getUserRole() == 1){
 				if(forum.getModerators().length > 0 && !ForumUtils.isStringInStrings(forum.getModerators(), this.userProfile.getUserId()) || forum.getModerators().length <=0){
 					if(forum.getIsClosed() || forum.getIsLock())continue ; 
 					if(forum.getCreateTopicRole().length > 0 && !ForumUtils.isStringInStrings(forum.getCreateTopicRole(), this.userProfile.getUserId())){
 						continue ;
 					}
 				}
 			}
 			forums.add(forum) ;
 		}
 		return forums ;
 	}
 	
 	static	public class SaveActionListener extends BaseEventListener<UIMoveTopicForm> {
     public void onEvent(Event<UIMoveTopicForm> event, UIMoveTopicForm uiForm, final String forumPath) throws Exception {
 			if(!ForumUtils.isEmpty(forumPath)) {
 				try {
 					WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
 					ResourceBundle res = context.getApplicationResourceBundle() ;
 					// set link
 					String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, "pathId").replaceFirst("private", "public");
 					//
 					uiForm.forumService.moveTopic(uiForm.topics , forumPath, res.getString("UINotificationForm.label.EmailToAuthorMoved"), link) ;
 					UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class) ;
 					forumPortlet.updateUserProfileInfo();
 					forumPortlet.cancelAction() ;
 					if(uiForm.isFormTopic) {
 						UIForumContainer forumContainer = forumPortlet.findFirstComponentOfType(UIForumContainer.class) ;
 						UITopicDetailContainer topicDetailContainer = forumContainer.getChild(UITopicDetailContainer.class) ;
 						forumContainer.setIsRenderChild(false) ;
 						String[] temp = forumPath.split("/") ;
 						UIForumDescription forumDescription = forumContainer.getChild(UIForumDescription.class);
 						forumDescription.setForumIds(temp[temp.length - 2], temp[temp.length - 1]);
 						topicDetailContainer.getChild(UITopicDetail.class).setUpdateTopic(temp[temp.length - 2], temp[temp.length - 1], uiForm.topics.get(0).getId()) ;
 						event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet) ;
 					} else {
 						UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class) ;
 						event.getRequestContext().addUIComponentToUpdateByAjax(topicContainer) ;
 					}
         } catch (ItemExistsException e) {
         	warning("UIImportForm.msg.ObjectIsExist") ;
         	return;
         } catch (Exception e) {
         	warning("UIMoveTopicForm.msg.parent-deleted") ;
         	return;
         }
 			}
 		}
 	}
 	
 	static	public class CancelActionListener extends EventListener<UIMoveTopicForm> {
     public void execute(Event<UIMoveTopicForm> event) throws Exception {
 			UIMoveTopicForm uiForm = event.getSource() ;
 			UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class) ;
 			forumPortlet.cancelAction() ;
 		}
 	}
 }
