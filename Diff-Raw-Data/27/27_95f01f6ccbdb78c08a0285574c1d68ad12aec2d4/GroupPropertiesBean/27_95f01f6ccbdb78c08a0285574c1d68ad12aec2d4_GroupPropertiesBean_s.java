 package com.idega.user.bean;
 
 public class GroupPropertiesBean extends PropertiesBean {
 	
 	private boolean showName = true;
 	private boolean showHomePage = true;
 	private boolean showShortName = false;
 	private boolean showPhone = true;
 	private boolean showFax = true;
 	private boolean showEmptyFields = false;
 	
 	public GroupPropertiesBean() {
		super(true, true);
 	}
 	
 	public GroupPropertiesBean(PropertiesBean bean, String instanceId) {
		super(bean, true, true);
 		this.setInstanceId(instanceId);
 	}
 	
 	public boolean isShowEmptyFields() {
 		return showEmptyFields;
 	}
 	public void setShowEmptyFields(boolean showEmptyFields) {
 		this.showEmptyFields = showEmptyFields;
 	}
 	public boolean isShowFax() {
 		return showFax;
 	}
 	public void setShowFax(boolean showFax) {
 		this.showFax = showFax;
 	}
 	public boolean isShowHomePage() {
 		return showHomePage;
 	}
 	public void setShowHomePage(boolean showHomePage) {
 		this.showHomePage = showHomePage;
 	}
 	public boolean isShowName() {
 		return showName;
 	}
 	public void setShowName(boolean showName) {
 		this.showName = showName;
 	}
 	public boolean isShowPhone() {
 		return showPhone;
 	}
 	public void setShowPhone(boolean showPhone) {
 		this.showPhone = showPhone;
 	}
 	public boolean isShowShortName() {
 		return showShortName;
 	}
 	public void setShowShortName(boolean showShortName) {
 		this.showShortName = showShortName;
 	}
 }
