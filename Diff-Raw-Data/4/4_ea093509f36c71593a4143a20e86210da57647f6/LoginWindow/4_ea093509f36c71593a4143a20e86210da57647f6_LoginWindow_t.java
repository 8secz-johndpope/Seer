 /*
 * Copyright (C) 2005-2009 University of Deusto
 * All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.
 *
 * This software consists of contributions made by many individuals, 
 * listed below:
 *
 * Author: Pablo Orduña <pablo@ordunya.com>
 *
 */
 
 package es.deusto.weblab.client.lab.ui.themes.es.deusto.weblab.defaultmobile;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
 import com.google.gwt.event.dom.client.KeyCodes;
 import com.google.gwt.event.dom.client.KeyDownEvent;
 import com.google.gwt.event.dom.client.KeyDownHandler;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiHandler;
 import com.google.gwt.user.client.Cookies;
 import com.google.gwt.user.client.ui.Anchor;
 import com.google.gwt.user.client.ui.Button;
 import com.google.gwt.user.client.ui.DecoratedPopupPanel;
 import com.google.gwt.user.client.ui.Label;
 import com.google.gwt.user.client.ui.PasswordTextBox;
 import com.google.gwt.user.client.ui.TextBox;
 import com.google.gwt.user.client.ui.VerticalPanel;
 import com.google.gwt.user.client.ui.Widget;
 
 import es.deusto.weblab.client.WebLabClient;
 import es.deusto.weblab.client.configuration.IConfigurationManager;
 import es.deusto.weblab.client.lab.ui.themes.es.deusto.weblab.defaultweb.i18n.IWebLabDeustoThemeMessages;
 import es.deusto.weblab.client.ui.widgets.WlWaitingLabel;
 
 class LoginWindow extends BaseWindow {
 
 	interface LoginWindowUiBinder extends UiBinder<Widget, LoginWindow> {
 	}
 
 	public interface ILoginWindowCallback {
 		public void onLoginButtonClicked(String username, String password);
 	}
 
 	private final LoginWindowUiBinder uiBinder = GWT.create(LoginWindowUiBinder.class);
 
 	@UiField TextBox usernameTextbox;
 	@UiField PasswordTextBox passwordTextbox;
 	@UiField WlWaitingLabel waitingLabel;
 	
 	@UiField Anchor languages;
	@UiField Anchor classicLink;
 
 	@UiField Label messages;
 
 	@UiField Label usernameErrorLabel;
 	@UiField Label passwordErrorLabel;
 	
 	@UiField Button loginButton;
 
 	private final ILoginWindowCallback callback;
 
 	LoginWindow(IConfigurationManager configurationManager, ILoginWindowCallback callback) {
 		super(configurationManager);
 		
 		this.callback = callback;
 		
 		super.loadWidgets();
 		
 		final Widget wid = this.uiBinder.createAndBindUi(this);
 		
 		this.setupWidgets(wid);
 	}
 
 	private void setupWidgets(final Widget wid) {
 		// If ENTER is pressed, login as if the button had been clicked.
 		final KeyDownHandler keyboardHandler = new KeyDownHandler(){
 			@Override
 			public void onKeyDown(KeyDownEvent event) {
 			    if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
 					LoginWindow.this.onLoginButtonClicked(null);   
 			}
 		};
 		this.usernameTextbox.addKeyDownHandler(keyboardHandler);
 		this.passwordTextbox.addKeyDownHandler(keyboardHandler);
 		
 		final DecoratedPopupPanel simplePopup = new DecoratedPopupPanel(true);
 		final VerticalPanel languageList = new VerticalPanel();
 		for(int i = 0; i < IWebLabDeustoThemeMessages.LANGUAGES.length; ++i){
 			final String curLanguage = IWebLabDeustoThemeMessages.LANGUAGES[i];
 			final String curLanguageCode = IWebLabDeustoThemeMessages.LANGUAGE_CODES[i];
 			final Anchor languageButton = new Anchor(curLanguage);
 			languageButton.addClickHandler(
 					new LanguageButtonClickHandler(curLanguageCode)
 				);
 			languageList.add(languageButton);
 		}		
 		languageList.setSpacing(15);
 		
 		simplePopup.setWidget(languageList);
 		
 		this.languages.addClickHandler(new ClickHandler() {
 			@Override
 			public void onClick(ClickEvent event) {
 	            final Widget source = (Widget) event.getSource();
 	            final int left = source.getAbsoluteLeft() + 10;
 	            final int top = source.getAbsoluteTop() + 10;
 	            simplePopup.setPopupPosition(left, top);
 	            simplePopup.setModal(true);
 				simplePopup.show();
 			}
 		});
 		
		this.classicLink.setHref(WebLabClient.getNewUrl(WebLabClient.MOBILE_URL_PARAM, "false"));
		
 		this.mainPanel.add(wid);
 	}
 
 	private static class LanguageButtonClickHandler implements ClickHandler{
 		private final String languageCode;
 		
 		public LanguageButtonClickHandler(String languageCode){
 			this.languageCode = languageCode;
 		}
 
 		@Override
 		public void onClick(ClickEvent sender) {
 			Cookies.setCookie(WebLabClient.LOCALE_COOKIE, this.languageCode);
 			WebLabClient.refresh(this.languageCode);
 		}
 	}
 	
 	@UiHandler("loginButton")
 	@SuppressWarnings("unused")
 	void onLoginButtonClicked(ClickEvent e) {
 		boolean errors = false;
 		this.hideMessage();
 		errors |= this.checkUsernameTextbox();
 		errors |= this.checkPasswordTextbox();
 		if(!errors){
 			this.waitingLabel.setStyleName(".visible-message");
 			this.waitingLabel.setText(LoginWindow.this.i18nMessages.loggingIn());
 			this.waitingLabel.start();
 			this.loginButton.setEnabled(false);
 			this.callback.onLoginButtonClicked(
 					this.usernameTextbox.getText(), 
 					this.passwordTextbox.getText()
 				);
 		}
 	}
 	
 	private boolean checkUsernameTextbox(){
 		if(this.usernameTextbox.getText().length() == 0){
 			this.showError(this.usernameErrorLabel, this.i18nMessages.thisFieldCantBeEmpty());
 			return true;
 		}
 		
 		this.hideError(this.usernameErrorLabel);
 		return false;
 	}
 	
 	private boolean checkPasswordTextbox() {
 		if(this.passwordTextbox.getText().length() == 0){
 			this.showError(this.passwordErrorLabel, this.i18nMessages.thisFieldCantBeEmpty());
 			return true;
 		}
 		
 		this.hideError(this.passwordErrorLabel);
 		return false;
 	}
 	
 	@Override
 	void showError(String message) {
 		this.showError(this.messages, message);
 	}
 
 	private void showError(Label where, String message) {
 		where.setStyleName(".visible-error"); // TODO: the color doesn't work :-S
 		where.setText(message);
 	}
 
 	private void hideError(Label where) {
 		where.setText("");
 		where.setStyleName(".invisible");
 	}
 
 	private void hideMessage() {
 		this.hideError(this.messages);
 	}
 	
 	@Override
 	void showMessage(String message) {
 		this.messages.setText(message);
 		this.messages.setStyleName(".visible-message");
 	}
 
 	void showWrongLoginOrPassword() {
 		this.messages.setText(this.i18nMessages.invalidUsernameOrPassword());
 		this.waitingLabel.stop();
 		this.waitingLabel.setText("");
 		this.loginButton.setEnabled(true);
 	}
 }
