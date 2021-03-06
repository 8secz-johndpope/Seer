 /***************************************************************************
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
  ***************************************************************************/
 package org.exoplatform.forum.webui.popup;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.exoplatform.container.ExoContainerContext;
 import org.exoplatform.container.PortalContainer;
 import org.exoplatform.forum.ForumUtils;
 import org.exoplatform.forum.service.BBCode;
 import org.exoplatform.forum.service.ForumAdministration;
 import org.exoplatform.forum.service.ForumPageList;
 import org.exoplatform.forum.service.ForumService;
 import org.exoplatform.forum.service.JCRPageList;
 import org.exoplatform.forum.service.PruneSetting;
 import org.exoplatform.forum.service.TopicType;
 import org.exoplatform.forum.webui.UICategory;
import org.exoplatform.forum.webui.UIForumContainer;
 import org.exoplatform.forum.webui.UIForumPageIterator;
 import org.exoplatform.forum.webui.UIForumPortlet;
 import org.exoplatform.forum.webui.UITopicContainer;
 import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
 import org.exoplatform.web.application.ApplicationMessage;
 import org.exoplatform.webui.config.annotation.ComponentConfig;
 import org.exoplatform.webui.config.annotation.EventConfig;
 import org.exoplatform.webui.core.UIApplication;
 import org.exoplatform.webui.core.UIPopupWindow;
 import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
 import org.exoplatform.webui.core.model.SelectItemOption;
 import org.exoplatform.webui.event.Event;
 import org.exoplatform.webui.event.EventListener;
 import org.exoplatform.webui.event.Event.Phase;
 import org.exoplatform.webui.form.UIForm;
 import org.exoplatform.webui.form.UIFormCheckBoxInput;
 import org.exoplatform.webui.form.UIFormInputWithActions;
 import org.exoplatform.webui.form.UIFormRadioBoxInput;
 import org.exoplatform.webui.form.UIFormSelectBox;
 import org.exoplatform.webui.form.UIFormStringInput;
 import org.exoplatform.webui.form.UIFormTextAreaInput;
 import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
 import org.exoplatform.webui.form.validator.PositiveNumberFormatValidator;
 import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;
 
 /**
  * Created by The eXo Platform SAS
  * Author : Vu Duy Tu
  *					tu.duy@exoplatform.com
  * May 5, 2008 - 9:01:20 AM	
  */
 @ComponentConfig(
 		lifecycle = UIFormLifecycle.class,
 		template = "app:/templates/forum/webui/popup/UIForumAdministrationForm.gtmpl",
 		events = {
 			@EventConfig(listeners = UIForumAdministrationForm.SaveActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.AddIpActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.PostsActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.UnBanActionListener.class, confirm= "UIForumAdministrationForm.msg.confirm-delete-ipban"), 
 			@EventConfig(listeners = UIForumAdministrationForm.GetDefaultMailActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.AddNewBBCodeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.EditBBCodeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.DeleteBBCodeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.AddTopicTypeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.EditTopicTypeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.DeleteTopicTypeActionListener.class), 
 			@EventConfig(listeners = UIForumAdministrationForm.CancelActionListener.class, phase=Phase.DECODE),
 			@EventConfig(listeners = UIForumAdministrationForm.SelectTabActionListener.class, phase=Phase.DECODE),
 			@EventConfig(listeners = UIForumAdministrationForm.PruneSettingActionListener.class),
 			@EventConfig(listeners = UIForumAdministrationForm.RunPruneActionListener.class),
 			@EventConfig(listeners = UIForumAdministrationForm.ActivatePruneActionListener.class)
 		}
 )
 public class UIForumAdministrationForm extends UIForm implements UIPopupComponent {
 	private ForumService forumService ;
 	private ForumAdministration administration ;
 	private int id = 0 ;
 	private boolean isRenderListTopic = false ;
 	public static final String FIELD_FORUMSORT_TAB = "forumSortTab" ;
 	public static final String FIELD_CENSOREDKEYWORD_TAB = "forumCensorTab" ;
 	public static final String FIELD_ACTIVETOPIC_TAB = "activeTopicTab" ;
 	public static final String FIELD_NOTIFYEMAIL_TAB = "notifyEmailTab" ;
 	public static final String FIELD_AUTOPRUNE_TAB = "autoPruneTab" ;
 	public static final String FIELD_TOPICTYPEMANAGER_TAB = "topicTypeManagerTab" ;
 	public static final String FIELD_BBCODE_TAB = "bbcodesTab" ;
 	public static final String IP_BAN_TAB = "ipBanTab" ;
 	
 	public static final String NEW_IP_BAN_INPUT1 = "newIpBan1";
 	public static final String NEW_IP_BAN_INPUT2 = "newIpBan2";
 	public static final String NEW_IP_BAN_INPUT3 = "newIpBan3";
 	public static final String NEW_IP_BAN_INPUT4 = "newIpBan4";
 	public static final String SEARCH_IP_BAN = "searchIpBan";
 	public static final String FIELD_FORUMSORTBY_INPUT = "forumSortBy" ;
 	public static final String FIELD_FORUMSORTBYTYPE_INPUT = "forumSortByType" ;
 	public static final String FIELD_TOPICSORTBY_INPUT = "topicSortBy" ;
 	public static final String FIELD_TOPICSORTBYTYPE_INPUT = "topicSortByType" ;
 	
 	public static final String FIELD_CENSOREDKEYWORD_TEXTAREA = "censorKeyword" ;
 	
 	public static final String FIELD_ENABLEHEADERSUBJECT_CHECKBOX = "enableHeaderSubject" ;
 	public static final String FIELD_HEADERSUBJECT_INPUT = "headerSubject" ;
 	public static final String FIELD_NOTIFYEMAIL_TEXTAREA = "notifyEmail" ;
 	public static final String FIELD_NOTIFYEMAILMOVED_TEXTAREA = "notifyEmailMoved" ;
 	
 	public static final String FIELD_ACTIVEABOUT_INPUT = "activeAbout" ;
 	public static final String FIELD_SETACTIVE_INPUT = "setActive" ;
 	public static final String BAN_IP_PAGE_ITERATOR = "IpBanPageIterator" ;
 	@SuppressWarnings("unchecked")
   private JCRPageList pageList ;
 	private List<String> listIpBan = new ArrayList<String>();
 	private List<BBCode> listBBCode = new ArrayList<BBCode>();
 	private List<TopicType> listTT = new ArrayList<TopicType>();
 	List<PruneSetting> listPruneSetting = new ArrayList<PruneSetting>();
 	private UIForumPageIterator pageIterator ;
 	private String notifyEmail_ = "";
 	private String notifyMove_ = "";
 	public UIForumAdministrationForm() throws Exception {
 		forumService = (ForumService)PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class) ;
 		addChild(UIListTopicOld.class, null, null) ;
 		this.setActions(new String[]{"Save", "Cancel"}) ;
 		pageIterator = addChild(UIForumPageIterator.class, null, BAN_IP_PAGE_ITERATOR);
 	}
 	
   public void setInit() throws Exception{
   	getPruneSettings();
   	
 		this.administration = forumService.getForumAdministration();
 		UIFormInputWithActions forumSortTab = new UIFormInputWithActions(FIELD_FORUMSORT_TAB) ;
 		UIFormInputWithActions forumCensorTab = new UIFormInputWithActions(FIELD_CENSOREDKEYWORD_TAB) ;
 		UIFormInputWithActions notifyEmailTab = new UIFormInputWithActions(FIELD_NOTIFYEMAIL_TAB);
 		UIFormInputWithActions ipBanTab = new UIFormInputWithActions(IP_BAN_TAB);
 		UIFormInputWithActions bbcodeTab = new UIFormInputWithActions(FIELD_BBCODE_TAB);
 		UIFormInputWithActions autoPruneTab = new UIFormInputWithActions(FIELD_AUTOPRUNE_TAB);
 		UIFormInputWithActions topicTypeManagerTag = new UIFormInputWithActions(FIELD_TOPICTYPEMANAGER_TAB);
 		
 		String []idLables = new String[]{"forumOrder", "isLock", "createdDate",
 																"modifiedDate",	"topicCount", "postCount"}; 
 		List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>() ;
 		ls.add(new SelectItemOption<String>(this.getLabel("forumName"), "name")) ;
 		for (String string : idLables) {
 			ls.add(new SelectItemOption<String>(this.getLabel(string), string)) ;
 		}
 		UIFormSelectBox forumSortBy = new UIFormSelectBox(FIELD_FORUMSORTBY_INPUT, FIELD_FORUMSORTBY_INPUT, ls);
 		forumSortBy.setValue(administration.getForumSortBy()) ;
 		
 		ls = new ArrayList<SelectItemOption<String>>() ;
 		ls.add(new SelectItemOption<String>(this.getLabel("ascending"), "ascending")) ;
 		ls.add(new SelectItemOption<String>(this.getLabel("descending"), "descending")) ;
 		UIFormSelectBox forumSortByType = new UIFormSelectBox(FIELD_FORUMSORTBYTYPE_INPUT, FIELD_FORUMSORTBYTYPE_INPUT, ls);
 		forumSortByType.setValue(administration.getForumSortByType()) ;
 		
 		idLables = new String[]{"isLock", "createdDate", "modifiedDate", 
 				"lastPostDate", "postCount", "viewCount", "numberAttachments"}; 
 		ls = new ArrayList<SelectItemOption<String>>() ;
 		ls.add(new SelectItemOption<String>(this.getLabel("threadName"), "name")) ;
 		for (String string : idLables) {
 			ls.add(new SelectItemOption<String>(this.getLabel(string), string)) ;
 		}
 		
 		UIFormSelectBox topicSortBy = new UIFormSelectBox(FIELD_TOPICSORTBY_INPUT, FIELD_TOPICSORTBY_INPUT, ls);
 		topicSortBy.setValue(administration.getTopicSortBy()) ;
 		
 		ls = new ArrayList<SelectItemOption<String>>() ;
 		ls.add(new SelectItemOption<String>(this.getLabel("ascending"), "ascending")) ;
 		ls.add(new SelectItemOption<String>(this.getLabel("descending"), "descending")) ;
 		UIFormSelectBox topicSortByType = new UIFormSelectBox(FIELD_TOPICSORTBYTYPE_INPUT, FIELD_TOPICSORTBYTYPE_INPUT, ls);
 		topicSortByType.setValue(administration.getTopicSortByType()) ;
 		
 		UIFormTextAreaInput censorKeyword = new UIFormTextAreaInput(FIELD_CENSOREDKEYWORD_TEXTAREA, FIELD_CENSOREDKEYWORD_TEXTAREA, null);
 		censorKeyword.setValue(administration.getCensoredKeyword()) ;
 		
 		UIFormStringInput activeAbout = new UIFormStringInput(FIELD_ACTIVEABOUT_INPUT, FIELD_ACTIVEABOUT_INPUT, null);
 		activeAbout.setValue("0");
 		activeAbout.addValidator(PositiveNumberFormatValidator.class);
 		List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
 		options.add( new SelectItemOption<String>("true", "true") ) ;
 		options.add( new SelectItemOption<String>("false", "false") ) ;
 		UIFormRadioBoxInput setActive = new UIFormRadioBoxInput(FIELD_SETACTIVE_INPUT, FIELD_SETACTIVE_INPUT, options);
 		setActive.setValue("false") ;
 		
 		String value = administration.getNotifyEmailContent();
 		if(ForumUtils.isEmpty(value)) value = this.getLabel("notifyEmailContentDefault");
 		UIFormWYSIWYGInput notifyEmail = new UIFormWYSIWYGInput(FIELD_NOTIFYEMAIL_TEXTAREA, FIELD_NOTIFYEMAIL_TEXTAREA, "");
 		notifyEmail.setValue(value); this.notifyEmail_ = value;
 		value = administration.getNotifyEmailMoved();
 		if(ForumUtils.isEmpty(value)) value = this.getLabel("EmailToAuthorMoved");
 		UIFormWYSIWYGInput notifyEmailMoved = new UIFormWYSIWYGInput(FIELD_NOTIFYEMAILMOVED_TEXTAREA, FIELD_NOTIFYEMAILMOVED_TEXTAREA, "");
 		notifyEmailMoved.setValue(value); this.notifyMove_ = value;
 		
 		UIFormCheckBoxInput<Boolean> enableHeaderSubject = new UIFormCheckBoxInput<Boolean>(FIELD_ENABLEHEADERSUBJECT_CHECKBOX, FIELD_ENABLEHEADERSUBJECT_CHECKBOX, false);
 		enableHeaderSubject.setChecked(administration.getEnableHeaderSubject());
 		UIFormStringInput headerSubject = new UIFormStringInput(FIELD_HEADERSUBJECT_INPUT, FIELD_HEADERSUBJECT_INPUT, null);
 		String headerSubject_ = administration.getHeaderSubject(); 
 		if(ForumUtils.isEmpty(headerSubject_)) headerSubject_ = this.getLabel("notifyEmailHeaderSubjectDefault");
 		headerSubject.setValue(headerSubject_);
 		//headerSubject.setEditable(administration.getEnableHeaderSubject());
 		
 		setListBBcode();
     
 		for (BBCode bbc : listBBCode) {
 			UIFormCheckBoxInput<Boolean>isActiveBBcode = new UIFormCheckBoxInput<Boolean>(bbc.getId(), bbc.getId(), false);
     	isActiveBBcode.setChecked(bbc.isActive());
       bbcodeTab.addChild(isActiveBBcode);
     }
 		
 		forumSortTab.addUIFormInput(forumSortBy) ;
 		forumSortTab.addUIFormInput(forumSortByType) ;
 		forumSortTab.addUIFormInput(topicSortBy) ;
 		forumSortTab.addUIFormInput(topicSortByType) ;
 		
 		notifyEmailTab.addUIFormInput(enableHeaderSubject);
 		notifyEmailTab.addUIFormInput(headerSubject);
 		notifyEmailTab.addUIFormInput(notifyEmail) ;
 		notifyEmailTab.addUIFormInput(notifyEmailMoved) ;
 		
 		forumCensorTab.addUIFormInput(censorKeyword) ;
 		
 		addUIFormInput(activeAbout);
 		addUIFormInput(setActive);
 		
 		addUIFormInput(forumSortTab) ;
 		addUIFormInput(forumCensorTab) ;
 		addUIFormInput(notifyEmailTab) ;
 		addUIFormInput(bbcodeTab) ;
 		addUIFormInput(autoPruneTab) ;
 		addUIFormInput(topicTypeManagerTag) ;
 		if(ForumUtils.enableIPLogging()){
 			ipBanTab.addUIFormInput(new UIFormStringInput(SEARCH_IP_BAN, null));
 			ipBanTab.addUIFormInput((new UIFormStringInput(NEW_IP_BAN_INPUT1, null)).setMaxLength(3));
 			ipBanTab.addUIFormInput((new UIFormStringInput(NEW_IP_BAN_INPUT2, null)).setMaxLength(3));
 			ipBanTab.addUIFormInput((new UIFormStringInput(NEW_IP_BAN_INPUT3, null)).setMaxLength(3));
 			ipBanTab.addUIFormInput((new UIFormStringInput(NEW_IP_BAN_INPUT4, null)).setMaxLength(3));
 			addUIFormInput(ipBanTab);
 		}
 		
 		
 		List<ActionData> actions = new ArrayList<ActionData>() ;
 		ActionData ad = new ActionData() ;
 		ad.setActionListener("GetDefaultMail") ;
 		ad.setActionParameter(FIELD_NOTIFYEMAIL_TEXTAREA) ;
 		ad.setCssIconClass("Refresh") ;
 		ad.setActionName("TitleResetMail");
 		actions.add(ad) ;
 		notifyEmailTab.setActionField(FIELD_NOTIFYEMAIL_TEXTAREA, actions);
 		
 		actions = new ArrayList<ActionData>() ;
 		ad = new ActionData() ;
 		ad.setActionListener("GetDefaultMail") ;
 		ad.setActionParameter(FIELD_NOTIFYEMAILMOVED_TEXTAREA) ;
 		ad.setCssIconClass("Refresh") ;
 		ad.setActionName("TitleResetMail");
 		actions.add(ad) ;
 		notifyEmailTab.setActionField(FIELD_NOTIFYEMAILMOVED_TEXTAREA, actions);
 	}
 	
 	public void setListBBcode() throws Exception {
 		listBBCode = new ArrayList<BBCode>();
 		try {
 			listBBCode.addAll(forumService.getAllBBCode());
     } catch (Exception e) {
 	    e.printStackTrace();
     }
 	}
 	
 	private List<PruneSetting> getPruneSettings() throws Exception {
 		listPruneSetting = new ArrayList<PruneSetting>();
 		try {
 			listPruneSetting.addAll(forumService.getAllPruneSetting());
     } catch (Exception e) {
     }
 		return listPruneSetting;
 	}
 	
 	private PruneSetting getPruneSetting(String pruneId) throws Exception {
 		for (PruneSetting prune : listPruneSetting) {
 	    if(prune.getId().equals(pruneId)) return prune;
     }
 		return new PruneSetting();
 	}
 	
 	@SuppressWarnings("unused")
   private List<TopicType> getTopicTypes() throws Exception {
 		listTT = new ArrayList<TopicType>();
 		listTT.addAll(forumService.getTopicTypes());
 		return listTT;
 	}
 	
 	private TopicType getTopicType(String topicTId) throws Exception {
 		for (TopicType topicT : listTT) {
 	    if(topicT.getId().equals(topicTId)) return topicT;
     }
 		return new TopicType();
 	}
 	
 	@SuppressWarnings("unused")
   private List<BBCode> getListBBcode() throws Exception{
 		return listBBCode;
 	}
 	
 	private BBCode getBBCode(String bbcId) {
 		for (BBCode bbCode : listBBCode) {
 	    if(bbCode.getId().equals(bbcId)) return bbCode;
     }
 		return new BBCode();
 	}
 	
 	@SuppressWarnings({ "unused", "unchecked" })
 	private List<String> getListIpBan() throws Exception{
 		listIpBan = new ArrayList<String>();
 		try {
 			listIpBan.addAll(forumService.getBanList());
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		pageList = new ForumPageList(8, listIpBan.size());
 		pageList.setPageSize(8);
 		pageIterator = this.getChild(UIForumPageIterator.class);
 		pageIterator.updatePageList(pageList);
 		List<String>list = new ArrayList<String>();
 		list.addAll(this.pageList.getPageList(pageIterator.getPageSelected(), listIpBan)) ;
 		pageIterator.setSelectPage(pageList.getCurrentPage());
 		try {
 			if(pageList.getAvailablePage() <= 1) pageIterator.setRendered(false);
 			else  pageIterator.setRendered(true);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return list;
 	}
 	
 	public boolean isRenderListTopic() {
 		return isRenderListTopic;
 	}
 
 	public void setRenderListTopic(boolean isRenderListTopic) {
 		this.isRenderListTopic = isRenderListTopic;
 	}
 	public void activate() throws Exception {}
 	public void deActivate() throws Exception {}
 	
 	@SuppressWarnings("unused")
 	private boolean getIsSelected(int id) {
 		if(this.id == id) return true ;
 		return false ;
 	}
 	
 	private String checkIpAddress(String[] ipAdd){
 		String ip = "";
 		try{
 			int[] ips = new int[4];
 			for(int t = 0; t < ipAdd.length; t ++){
 				if(t>0) ip += ".";
 				ip += ipAdd[t];
 				ips[t] = Integer.parseInt(ipAdd[t]);
 			}
 			for(int i = 0; i < 4; i ++){
 				if(ips[i] < 0 || ips[i] > 255) return null;
 			}
 			if(ips[0] == 255 && ips[1] == 255 && ips[2] == 255 && ips[3] == 255) return null;
 			return ip;
 		} catch (Exception e){
 			return null;
 		}
 	}
 	
 	static	public class SaveActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm administrationForm = event.getSource() ;
 			UIFormInputWithActions forumSortTab = administrationForm.getChildById(FIELD_FORUMSORT_TAB) ;
 			UIFormInputWithActions forumCensor = administrationForm.getChildById(FIELD_CENSOREDKEYWORD_TAB) ;
 			UIFormInputWithActions notifyEmailTab = administrationForm.getChildById(FIELD_NOTIFYEMAIL_TAB) ;
 			String forumSortBy = forumSortTab.getUIFormSelectBox(FIELD_FORUMSORTBY_INPUT).getValue() ;
 			String forumSortByType = forumSortTab.getUIFormSelectBox(FIELD_FORUMSORTBYTYPE_INPUT).getValue() ;
 			String topicSortBy = forumSortTab.getUIFormSelectBox(FIELD_TOPICSORTBY_INPUT).getValue() ;
 			String topicSortByType = forumSortTab.getUIFormSelectBox(FIELD_TOPICSORTBYTYPE_INPUT).getValue() ;
 			String censoredKeyword = forumCensor.getUIFormTextAreaInput(FIELD_CENSOREDKEYWORD_TEXTAREA).getValue() ;
 			censoredKeyword = ForumUtils.removeSpaceInString(censoredKeyword);
 			if(!ForumUtils.isEmpty(censoredKeyword)) {
 				censoredKeyword = censoredKeyword.toLowerCase();
 			}
 			boolean enableHeaderSubject = (Boolean)notifyEmailTab.getUIFormCheckBoxInput(FIELD_ENABLEHEADERSUBJECT_CHECKBOX).getValue();
 			String headerSubject = notifyEmailTab.getUIStringInput(FIELD_HEADERSUBJECT_INPUT).getValue();
 			String notifyEmail = ((UIFormWYSIWYGInput)notifyEmailTab.getChildById(FIELD_NOTIFYEMAIL_TEXTAREA)).getValue() ;
 			String notifyEmailMoved = ((UIFormWYSIWYGInput)notifyEmailTab.getChildById(FIELD_NOTIFYEMAILMOVED_TEXTAREA)).getValue() ;
 			UIForumPortlet forumPortlet = administrationForm.getAncestorOfType(UIForumPortlet.class) ;
 			if(notifyEmail == null || notifyEmail.replaceAll("<p>", "").replaceAll("</p>", "").replaceAll("&nbsp;", "").trim().length() < 1){
 				UIApplication uiApplication = administrationForm.getAncestorOfType(UIApplication.class) ;
 				uiApplication.addMessage(new ApplicationMessage("UIForumAdministrationForm.msg.mailContentInvalid", new String[]{administrationForm.getLabel(FIELD_NOTIFYEMAIL_TEXTAREA)}, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages()) ;
 				return;
 			}
 			if(notifyEmailMoved == null || notifyEmailMoved.replaceAll("<p>", "").replaceAll("</p>", "").replaceAll("&nbsp;", "").trim().length() < 1){
 				UIApplication uiApplication = administrationForm.getAncestorOfType(UIApplication.class) ;
 				uiApplication.addMessage(new ApplicationMessage("UIForumAdministrationForm.msg.mailContentInvalid", new String[]{administrationForm.getLabel(FIELD_NOTIFYEMAILMOVED_TEXTAREA)}, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages()) ;
 				return;
 			}
 			ForumAdministration forumAdministration = new ForumAdministration() ;
 			forumAdministration.setForumSortBy(forumSortBy) ;
 			forumAdministration.setForumSortByType(forumSortByType) ;
 			forumAdministration.setTopicSortBy(topicSortBy) ;
 			forumAdministration.setTopicSortByType(topicSortByType) ;
 			forumAdministration.setCensoredKeyword(censoredKeyword) ;
 			forumAdministration.setEnableHeaderSubject(enableHeaderSubject) ;
 			forumAdministration.setHeaderSubject(headerSubject);
 			forumAdministration.setNotifyEmailContent(notifyEmail) ;
 			forumAdministration.setNotifyEmailMoved(notifyEmailMoved);
 			try {
 				administrationForm.forumService.saveForumAdministration(forumAdministration) ;
 				if(!forumSortBy.equals(administrationForm.administration.getForumSortBy()) || !forumSortByType.equals(administrationForm.administration.getForumSortByType())){
 					forumPortlet.findFirstComponentOfType(UICategory.class).setIsEditForum(true);
 				}
 			} catch (Exception e) {
 			}
 			UIFormInputWithActions bbcodeTab = administrationForm.getChildById(FIELD_BBCODE_TAB) ;
 //			
 			List<BBCode> bbCodes = new ArrayList<BBCode>();
 			boolean inactiveAll = true;
 			for (BBCode bbc : administrationForm.listBBCode) {
 				boolean isActive = true;
 				try {
 					isActive = (Boolean)bbcodeTab.getUIFormCheckBoxInput(bbc.getId()).getValue();
         } catch (Exception e) {
         }
 				if(bbc.isActive() != isActive){
 					bbc.setActive(isActive);
 					bbCodes.add(bbc);
 				}
 				if(isActive) inactiveAll = false;
       }
 			if(administrationForm.listBBCode.size() > 0 && inactiveAll){
 				UIApplication uiApplication = administrationForm.getAncestorOfType(UIApplication.class) ;
 				uiApplication.addMessage(new ApplicationMessage("UIForumAdministrationForm.msg.inactiveAllBBCode", new String[]{administrationForm.getLabel(FIELD_NOTIFYEMAILMOVED_TEXTAREA)}, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages()) ;
 				return;
 			}
 			if(!bbCodes.isEmpty()){
 				try {
 					administrationForm.forumService.saveBBCode(bbCodes);
 	      } catch (Exception e) {
 	      }
 	      forumPortlet.findFirstComponentOfType(UITopicDetail.class).setIsGetSv(true);
 			}
 			forumPortlet.cancelAction() ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet) ;
 		}
 	}
 	
 	static	public class RunPruneActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource() ;
 			String pruneId = event.getRequestContext().getRequestParameter(OBJECTID)	;
 			PruneSetting pruneSetting = uiForm.getPruneSetting(pruneId);
 			if(pruneSetting.getInActiveDay() == 0) {
 				UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
 				uiApp.addMessage(new ApplicationMessage("UIForumAdministrationForm.sms.not-set-activeDay", new Object[]{}, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
 				return;
 			}else {
 				UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 				UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 				UIRunPruneForm pruneForm = popupAction.activate(UIRunPruneForm.class, 200) ;
 				pruneForm.setPruneSetting(pruneSetting);
 				pruneForm.setId("RunPruneForm");
 				event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 			}
 		}
 	}
 	
 	static	public class GetDefaultMailActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String id = event.getRequestContext().getRequestParameter(OBJECTID)	;
 			if(id.equals(FIELD_NOTIFYEMAIL_TEXTAREA)) {
 				UIFormWYSIWYGInput areaInput = ((UIFormInputWithActions)uiForm.getChildById(FIELD_NOTIFYEMAIL_TAB)).
 																																					getChildById(FIELD_NOTIFYEMAIL_TEXTAREA);
 				areaInput.setValue(uiForm.getLabel("notifyEmailContentDefault"));
 			} else {
 				UIFormWYSIWYGInput areaInput = ((UIFormInputWithActions)uiForm.getChildById(FIELD_NOTIFYEMAIL_TAB)).
 				getChildById(FIELD_NOTIFYEMAILMOVED_TEXTAREA);
 				areaInput.setValue(uiForm.getLabel("EmailToAuthorMoved"));
 			}
 			event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
 		}
 	}
 	
 	static	public class SelectTabActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			String id = event.getRequestContext().getRequestParameter(OBJECTID)	;
 			UIForumAdministrationForm uiForm = event.getSource();
 			int temp = uiForm.id;
 			uiForm.id = Integer.parseInt(id);
 			UIFormInputWithActions notifyEmailTab = uiForm.getChildById(FIELD_NOTIFYEMAIL_TAB) ;
 			UIFormWYSIWYGInput notifyEmailForm = notifyEmailTab.getChildById(FIELD_NOTIFYEMAIL_TEXTAREA) ;
 			UIFormWYSIWYGInput notifyMoveForm = notifyEmailTab.getChildById(FIELD_NOTIFYEMAILMOVED_TEXTAREA);
 			if(uiForm.id == 2) {
 				notifyEmailForm.setValue(uiForm.notifyEmail_);
 				notifyMoveForm.setValue(uiForm.notifyMove_);
 			} else if(temp == 2){
 				uiForm.notifyEmail_ = notifyEmailForm.getValue();
 				uiForm.notifyMove_ = notifyMoveForm.getValue();
 			}
 			if(uiForm.id == 3){
 				UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
 	      popupWindow.setWindowSize(650, 450) ;
 	      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow) ;
 			} else {
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
 			}
 		}
 	}
 
 	static	public class AddNewBBCodeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIAddBBCodeForm bbcForm = popupAction.activate(UIAddBBCodeForm.class, 670) ;
 			bbcForm.setId("AddBBCodeForm") ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 
 	static	public class EditBBCodeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String bbcId = event.getRequestContext().getRequestParameter(OBJECTID);
 			BBCode bbCode = uiForm.getBBCode(bbcId);
 			UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIAddBBCodeForm bbcForm = popupAction.activate(UIAddBBCodeForm.class, 670) ;
 			bbcForm.setEditBBcode(bbCode);
 			bbcForm.setId("EditBBCodeForm") ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 	
 	static	public class DeleteBBCodeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String bbcId = event.getRequestContext().getRequestParameter(OBJECTID);
 			uiForm.forumService.removeBBCode(bbcId);
 			uiForm.setListBBcode();
 			event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
 		}
 	}
 
 	static	public class AddTopicTypeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIAddTopicTypeForm topicTypeForm = popupAction.activate(UIAddTopicTypeForm.class,700);
 			topicTypeForm.setId("AddTopicTypeForm");
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 	
 	static	public class EditTopicTypeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String topicTId = event.getRequestContext().getRequestParameter(OBJECTID);
 			TopicType topicType = uiForm.getTopicType(topicTId);
 			UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIAddTopicTypeForm topicTypeForm = popupAction.activate(UIAddTopicTypeForm.class,700);
 			topicTypeForm.setId("EditTopicTypeForm");
 			topicTypeForm.setTopicType(topicType);
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 	
 	static	public class DeleteTopicTypeActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String topicTypeId = event.getRequestContext().getRequestParameter(OBJECTID);
 			uiForm.forumService.removeTopicType(topicTypeId);
			UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
			UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class);
 			topicContainer.setTopicType(topicTypeId);
 			event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
			if(forumPortlet.getChild(UIForumContainer.class).isRendered() && !forumPortlet.findFirstComponentOfType(UITopicDetailContainer.class).isRendered()){
				event.getRequestContext().addUIComponentToUpdateByAjax(topicContainer) ;
			}
 		}
 	}
 	
 	static	public class CancelActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class) ;
 			forumPortlet.cancelAction() ;
 		}
 	}
 
 	static	public class ActivatePruneActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			String pruneId = event.getRequestContext().getRequestParameter(OBJECTID);
 			UIForumAdministrationForm uiForm = event.getSource();
 			PruneSetting pruneSetting = uiForm.getPruneSetting(pruneId);
 			if(pruneSetting.getInActiveDay() == 0) {
 				UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 				UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 				UIAutoPruneSettingForm pruneSettingForm = popupAction.activate(UIAutoPruneSettingForm.class, 525) ;
 				pruneSettingForm.setPruneSetting(pruneSetting);
 				pruneSettingForm.setId("AutoPruneSettingForm") ;
 				pruneSettingForm.setActivate(true);
 				event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 			} else {
 				pruneSetting.setActive(!pruneSetting.isActive());
 				uiForm.forumService.savePruneSetting(pruneSetting);
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
 			}
 		}
 	}
 	
 	static	public class PruneSettingActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm uiForm = event.getSource();
 			String pruneId = event.getRequestContext().getRequestParameter(OBJECTID);
 			UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIAutoPruneSettingForm pruneSettingForm = popupAction.activate(UIAutoPruneSettingForm.class, 525) ;
 			PruneSetting pruneSetting = uiForm.getPruneSetting(pruneId);
 			pruneSettingForm.setPruneSetting(pruneSetting);
 			pruneSettingForm.setId("AutoPruneSettingForm") ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 	
 	static	public class AddIpActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm administrationForm = event.getSource();
 			UIFormInputWithActions inputWithActions = (UIFormInputWithActions)administrationForm.getChildById(IP_BAN_TAB);
 			String[] ip = new String[]{((UIFormStringInput)inputWithActions.getChildById(NEW_IP_BAN_INPUT1)).getValue(),
 																	((UIFormStringInput)inputWithActions.getChildById(NEW_IP_BAN_INPUT2)).getValue(),
 																	((UIFormStringInput)inputWithActions.getChildById(NEW_IP_BAN_INPUT3)).getValue(),
 																	((UIFormStringInput)inputWithActions.getChildById(NEW_IP_BAN_INPUT4)).getValue(),
 																	};
 			for(int i = 1; i <= 4; i ++){
 				((UIFormStringInput)inputWithActions.getChildById("newIpBan" + i)).setValue("");
 			}
 			UIForumAdministrationForm uiForm = event.getSource();
 			UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
 			String ipAdd = uiForm.checkIpAddress(ip);
 			if(ipAdd == null){
 				uiApp.addMessage(new ApplicationMessage("UIForumAdministrationForm.sms.ipInvalid", null, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
 				return ;
 			} 
 			ForumService fservice = (ForumService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class) ;
 			if(!fservice.addBanIP(ipAdd)){
 				uiApp.addMessage(new ApplicationMessage("UIForumAdministrationForm.sms.ipBanFalse", new Object[]{ipAdd}, ApplicationMessage.WARNING)) ;
 				event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
 				return;
 			}
 			event.getRequestContext().addUIComponentToUpdateByAjax(administrationForm) ;
 		}
 	}
 	
 	static	public class PostsActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm forumAdministrationForm = event.getSource();
 			String ip = event.getRequestContext().getRequestParameter(OBJECTID)	;
 			UIPopupContainer popupContainer = forumAdministrationForm.getAncestorOfType(UIPopupContainer.class);
 			UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class).setRendered(true) ;
 			UIPageListPostByIP viewPostedByUser = popupAction.activate(UIPageListPostByIP.class, 650) ;
 			viewPostedByUser.setIp(ip) ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
 		}
 	}
 	
 	static	public class UnBanActionListener extends EventListener<UIForumAdministrationForm> {
 		public void execute(Event<UIForumAdministrationForm> event) throws Exception {
 			UIForumAdministrationForm administrationForm = event.getSource();
 			String ip = event.getRequestContext().getRequestParameter(OBJECTID)	;
 			ForumService fservice = (ForumService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class) ;
 			fservice.removeBan(ip) ;
 			event.getRequestContext().addUIComponentToUpdateByAjax(administrationForm) ;
 		}
 	}
 }
