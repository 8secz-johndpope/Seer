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
 
 package opus.gwt.management.console.client.dashboard;
 
 import java.util.HashMap;
 
 import opus.gwt.management.console.client.ClientFactory;
 import opus.gwt.management.console.client.JSVariableHandler;
 import opus.gwt.management.console.client.event.PanelTransitionEvent;
 import opus.gwt.management.console.client.overlays.Application;
 import opus.gwt.management.console.client.overlays.Project;
 import opus.gwt.management.console.client.resources.ManagementConsoleControllerResources.ManagementConsoleControllerStyle;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.JsArrayString;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
 import com.google.gwt.event.dom.client.MouseOutEvent;
 import com.google.gwt.event.dom.client.MouseOutHandler;
 import com.google.gwt.event.dom.client.MouseOverEvent;
 import com.google.gwt.event.dom.client.MouseOverHandler;
 import com.google.gwt.event.shared.EventBus;
 import com.google.gwt.http.client.Request;
 import com.google.gwt.http.client.RequestBuilder;
 import com.google.gwt.http.client.RequestCallback;
 import com.google.gwt.http.client.RequestException;
 import com.google.gwt.http.client.Response;
 import com.google.gwt.http.client.URL;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.uibinder.client.UiHandler;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.ui.Button;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.FlexTable;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.FocusPanel;
 import com.google.gwt.user.client.ui.FormPanel;
 import com.google.gwt.user.client.ui.Image;
 import com.google.gwt.user.client.ui.Label;
 import com.google.gwt.user.client.ui.Widget;
 
 public class DashboardPanel extends Composite {
 
 	private static DashboardUiBinder uiBinder = GWT.create(DashboardUiBinder.class);
 	interface DashboardUiBinder extends UiBinder<Widget, DashboardPanel> {}
 	
 	private EventBus eventBus;
 	private ClientFactory clientFactory;
 	private HashMap<String, Application> applications;
 	private JSVariableHandler JSVarHandler;
 	private String projectName;
 	private boolean active;
 	
 	@UiField FlowPanel applicationsFlowPanel;
 	@UiField Button settingsButton;
 	@UiField ManagementConsoleControllerStyle manager;
 	@UiField Label projectLabel;
 	@UiField Button activeButton;
 	@UiField Button deleteButton;
 	@UiField FlexTable formContainer;
 	@UiField FormPanel optionsForm;
 	
 	public DashboardPanel(ClientFactory clientFactory, String projectName) {
 		initWidget(uiBinder.createAndBindUi(this));
 		this.eventBus = clientFactory.getEventBus();
 		this.clientFactory = clientFactory;
 		this.JSVarHandler = clientFactory.getJSVariableHandler();
 		this.applications = clientFactory.getApplications();
 		this.projectName = projectName;
 		projectLabel.setText(projectName);
 		activeButton.setText("");
 		handleProjectInformation(projectName);
 	}
 	
 	@UiHandler("settingsButton")
 	void onSettingsButtonClick(ClickEvent event){
 		eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.SETTINGS));
 	}
 	
 	@UiHandler("activeButton")
 	void onDeactivateButtonClick(ClickEvent event) {
 		setProjectStatus();
 	}
 	
 	@UiHandler("deleteButton")
 	void onDeleteButtonClick(ClickEvent event) {
 		eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.DELETE));
 	}
 
 	public void handleProjectInformation(String projectName){
 		final Project project = clientFactory.getProjects().get(projectName);
 		HashMap<String, Application> applicationsMap = clientFactory.getApplications();
 		JsArrayString applicationsArray = project.getApps();
 		active = project.isActive();
 		
 		if(active) {
 			activeButton.setText("Deactivate project");
 		} else {
 			activeButton.setText("Activate project");
 		}
 		
 		for(int i = 0; i < applicationsArray.length() - 1; i++) {
 			
 			final Application app = applicationsMap.get(applicationsArray.get(i));
 			final FlowPanel application = new FlowPanel();
 			final FocusPanel applicationLabel = new FocusPanel();
 			
 			final Label appName = new Label(app.getName());
 			final Label httpLabel = new Label("HTTP");
 			final Label httpsLabel = new Label("HTTPS");
 			final Label settingsLabel = new Label("Settings");
 			
 			Image appIcon = new Image();
 			
 			if(app.getIconURL().split("//").length < 2) {
 				appIcon = new Image(JSVarHandler.getCommunityBaseURL() + app.getIconURL());
 			} else {
 				appIcon = new Image(app.getIconURL());
 			}
 			
 			appIcon.setSize("64px", "64px");
 			
 			application.add(appIcon);
 			application.add(appName);
 			application.add(httpLabel);
 			application.add(httpsLabel);
 			application.add(settingsLabel);
 			application.setStyleName(manager.appIcon());
 			
 			applicationLabel.add(application);
 
 			applicationLabel.addMouseOverHandler(new MouseOverHandler() {
 				public void onMouseOver(MouseOverEvent event){
 					applicationLabel.setStyleName(manager.appIconActive());
 					appName.addStyleName(manager.text());
 					httpLabel.addStyleName(manager.link());
 					httpsLabel.addStyleName(manager.link());
 					settingsLabel.addStyleName(manager.link());
 				}
 			});
 			applicationLabel.addMouseOutHandler(new MouseOutHandler() {
 				public void onMouseOut(MouseOutEvent event){
 					applicationLabel.setStyleName(manager.appIcon());
 					appName.removeStyleName(manager.text());
 					httpLabel.removeStyleName(manager.link());
 					httpsLabel.removeStyleName(manager.link());
 					settingsLabel.removeStyleName(manager.link());
 				}
 			});
 			
 			httpLabel.addClickHandler(new ClickHandler() {
 				public void onClick(ClickEvent event) {
 					Window.Location.assign(project.getURLS().get(0) + app.getAppName());
 				}
 			});
 			
 			httpsLabel.addClickHandler(new ClickHandler() {
 				public void onClick(ClickEvent event) {
 					Window.Location.assign(project.getURLS().get(1) + app.getAppName());
 				}
 			});
 			
 			settingsLabel.addClickHandler(new ClickHandler() {
 				public void onClick(ClickEvent event) {
 					Window.alert("Clicked settings link");
 				}
 			});
 			
 			applicationsFlowPanel.add(applicationLabel);
 		}
 
 	}
 	
 	private void setProjectStatus() {
 		StringBuffer formBuilder = new StringBuffer();
 		formBuilder.append("csrfmiddlewaretoken=");
 		formBuilder.append( URL.encodeQueryString(JSVarHandler.getCSRFTokenURL()));
 		
		formBuilder.append("&active=false");
 		
 		Window.alert(formBuilder.toString());
 		
	    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/deployments/" + projectName + "/");
 		builder.setHeader("Content-type", "application/x-www-form-urlencoded");
 		
 		try {
 			Request request = builder.sendRequest(formBuilder.toString(), new RequestCallback() {
 				@Override
 				public void onResponseReceived(Request request, Response response) {
 					Window.alert("Success!\n" + response.getText());
 				}
 
 				@Override
 				public void onError(Request request, Throwable exception) {
 					Window.alert("Error!");
 				}
 			});
 		} catch (RequestException e) {
 			e.printStackTrace();
 		}
 	}
 }
