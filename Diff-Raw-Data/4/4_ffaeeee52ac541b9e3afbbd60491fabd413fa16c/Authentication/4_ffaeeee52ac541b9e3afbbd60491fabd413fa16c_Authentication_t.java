 /*############################################################################
 # Copyright 2010 North Carolina State University                             #
 #                                                                            #
 #   Licensed under the Apache License, Version 2.0 (the "License");          #
 #   you may not use this file except in compliance with the License.         #
 #   You may obtain a copy of the License at                                  #
 #                                                                            #
 #       http://www.apache.org/licenses/LICENSE-2.0                           #
 #                                                                            #
 #   Unless required by applicable law or agreed to in writing, software      #
 #   distributed under the License is distributed on an "AS IS" BASIS,        #
 #   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
 #   See the License for the specific language governing permissions and      #
 #   limitations under the License.                                           #
 ############################################################################*/
 
 package opus.gwt.management.console.client;
 
 import opus.gwt.management.console.client.overlays.UserInformation;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.KeyCodes;
 import com.google.gwt.event.dom.client.KeyPressEvent;
 import com.google.gwt.http.client.URL;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiHandler;
 import com.google.gwt.user.client.Cookies;
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.ui.Button;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.FormPanel;
 import com.google.gwt.user.client.ui.Hidden;
 import com.google.gwt.user.client.ui.Label;
 import com.google.gwt.user.client.ui.PasswordTextBox;
 import com.google.gwt.user.client.ui.TextBox;
 import com.google.gwt.user.client.ui.Widget;
 import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
 
 public class Authentication extends Composite {
 	
 	private static AuthenticationUiBinder uiBinder = GWT.create(AuthenticationUiBinder.class);
 	interface AuthenticationUiBinder extends UiBinder<Widget, Authentication> {}
 	
	private final String loginURL = "/accounts/login/?next=/deployments/";
 	private final String checkLoginURL = "/json/username/?a&callback=";
 
 	private JSVariableHandler JSVarHandler;
 	private ServerCommunicator serverComm;
 	private PanelManager panelManager;
 	private boolean loggedIn;
 	private String username;
 	private boolean firstLoginAttempt;
 	
 	@UiField Hidden csrftoken;
 	@UiField Button loginButton;
 	@UiField TextBox usernameTextBox;
 	@UiField PasswordTextBox passwordTextBox;
 	@UiField FormPanel authenticationForm;
 	@UiField Label errorLabel;
 
 
 	public Authentication(PanelManager panelManager) {
 		initWidget(uiBinder.createAndBindUi(this));
 		this.serverComm = panelManager.getServerCommunicator();
 		this.panelManager = panelManager;
 		JSVarHandler = panelManager.getJSVariableHandler();
 		loggedIn = false;
 		username = "";
 		firstLoginAttempt = true;
 	}
 
 	public void startAuthentication(){
 		setupAuthenticationForm();
 		getUserInfo();
 	}
 	
 	private void setupAuthenticationForm(){
 		authenticationForm.setMethod(FormPanel.METHOD_POST);
 		authenticationForm.setAction(loginURL);
 		authenticationForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
 		      public void onSubmitComplete(SubmitCompleteEvent event) {
 		        getUserInfo();
 		        if( firstLoginAttempt )
 		        	firstLoginAttempt = false;
 		      }
 		 });
 		csrftoken.setValue(Cookies.getCookie("csrftoken")); 
         csrftoken.setName("csrfmiddlewaretoken");
 	}
 	
 	private void getUserInfo(){
 		final String url = URL.encode(JSVarHandler.getDeployerBaseURL() + checkLoginURL);
 		serverComm.getJson(url, serverComm, "handleUserInformation", this);
 	}
 	
 	public void handleUserInformation(UserInformation userInfo){
 		if( userInfo.isAuthenticated() ){
 			username = userInfo.getUsername();
 			loggedIn = true;
 			panelManager.passControl();
 		} else {
 			loggedIn = false;
 			panelManager.showPanel(this);
 			usernameTextBox.setFocus(true);
 			if( !firstLoginAttempt ){
 				errorLabel.setVisible(true);
 				usernameTextBox.setText("");
 				passwordTextBox.setText("");
 			} 
 		}
 	}
 		
 	@UiHandler("loginButton")
 	void onLoginClick(ClickEvent event) {
 		handleLoginButton();
 	}
 
 	@UiHandler("loginButton")
 	void onKeyPressLogin(KeyPressEvent event){
 		if(event.getCharCode() == KeyCodes.KEY_ENTER){
 			loginButton.click();
 		}
 	}
 	
 	@UiHandler("usernameTextBox")
 	void onKeyPressUsername(KeyPressEvent event){
 		if(event.getCharCode() == KeyCodes.KEY_ENTER){
 			loginButton.click();
 		}
 	}
 	
 	@UiHandler("passwordTextBox")
 	void onKeyPressPassword(KeyPressEvent event){
 		if(event.getCharCode() == KeyCodes.KEY_ENTER){
 			loginButton.click();
 		}
 	}
 
 	private void handleLoginButton(){
 		authenticationForm.submit();
 	}
 	
 	public boolean isLoggedIn(){
 		return loggedIn;
 	}
 	
 	public String getUsername(){
 		return username;
 	}
 }
