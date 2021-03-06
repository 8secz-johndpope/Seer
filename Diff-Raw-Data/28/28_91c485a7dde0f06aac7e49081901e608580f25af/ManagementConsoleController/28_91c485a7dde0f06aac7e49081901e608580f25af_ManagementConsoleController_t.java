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
 
 import java.util.HashMap;
 
 import opus.gwt.management.console.client.dashboard.DeleteProjectPanel;
 import opus.gwt.management.console.client.dashboard.ProjectManagerController;
 import opus.gwt.management.console.client.deployer.ProjectDeployerController;
 import opus.gwt.management.console.client.event.AsyncRequestEvent;
 import opus.gwt.management.console.client.event.AuthenticationEvent;
 import opus.gwt.management.console.client.event.AuthenticationEventHandler;
 import opus.gwt.management.console.client.event.GetApplicationsEvent;
 import opus.gwt.management.console.client.event.GetApplicationsEventHandler;
 import opus.gwt.management.console.client.event.GetProjectsEvent;
 import opus.gwt.management.console.client.event.GetProjectsEventHandler;
 import opus.gwt.management.console.client.event.PanelTransitionEvent;
 import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
 import opus.gwt.management.console.client.event.UpdateProjectsEvent;
 import opus.gwt.management.console.client.event.UpdateProjectsEventHandler;
 import opus.gwt.management.console.client.navigation.BreadCrumbsPanel;
 import opus.gwt.management.console.client.navigation.NavigationPanel;
 import opus.gwt.management.console.client.overlays.Application;
 import opus.gwt.management.console.client.overlays.Project;
 import opus.gwt.management.console.client.resources.ManagementConsoleControllerResources.ManagementConsoleControllerStyle;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.event.shared.EventBus;
 import com.google.gwt.uibinder.client.UiBinder;
 import com.google.gwt.uibinder.client.UiField;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.ui.Composite;
 import com.google.gwt.user.client.ui.LayoutPanel;
 import com.google.gwt.user.client.ui.RootLayoutPanel;
 import com.google.gwt.user.client.ui.Widget;
 
 
 public class ManagementConsoleController extends Composite {
 
 	private static ManagementConsoleUiBinder uiBinder = GWT.create(ManagementConsoleUiBinder.class);
 	interface ManagementConsoleUiBinder extends UiBinder<Widget, ManagementConsoleController> {}
 	
 	private EventBus eventBus;
 	private ClientFactory clientFactory;
 	private AuthenticationPanel authenticationPanel;
 	private ProjectDeployerController projectDeployerController;
 	private ProjectManagerController projectManagerController;
 	private IconPanel iconPanel;
 	private DeleteProjectPanel deleteProjectPanel;
 	private JSVariableHandler jsVarHandler;
 	private boolean onStartUp;
 	private String projectName;
 	
 	@UiField LayoutPanel contentLayoutPanel;
 	@UiField NavigationPanel navigationPanel;
 	@UiField BreadCrumbsPanel breadCrumbsPanel;
 	@UiField ManagementConsoleControllerStyle style;
 	
 	public ManagementConsoleController(ClientFactory clientFactory) {
 		initWidget(uiBinder.createAndBindUi(this));
 		RootLayoutPanel.get().setStyleName(style.rootLayoutPanel());
 		onStartUp = true;
 		jsVarHandler = new JSVariableHandler();
 		this.eventBus = clientFactory.getEventBus();
 		this.clientFactory = clientFactory;
 		authenticationPanel = new AuthenticationPanel(clientFactory);
 		iconPanel = new IconPanel(clientFactory);
 		navigationPanel.setEventBus(clientFactory);
 		breadCrumbsPanel.setEventBus(clientFactory);
 		registerHandlers();
 		deleteProjectPanel = new DeleteProjectPanel(clientFactory, projectName);
 		eventBus.fireEvent(new AsyncRequestEvent("handleUser"));
 		eventBus.fireEvent(new AsyncRequestEvent("getApplications"));
 		eventBus.fireEvent(new AsyncRequestEvent("getProjects"));
 	}
 	
 	private void registerHandlers(){
 		eventBus.addHandler(GetApplicationsEvent.TYPE, 
 			new GetApplicationsEventHandler() {
 				public void onGetApplications(GetApplicationsEvent event) {
 					HashMap<String, Application> applications = event.getApplications();
 					clientFactory.setApplications(applications);
 				}
 		});
 		
 		eventBus.addHandler(GetProjectsEvent.TYPE, 
 			new GetProjectsEventHandler(){
 				public void onGetProjects(GetProjectsEvent event) {
 					HashMap<String, Project> projects = event.getProjects();
 					clientFactory.setProjects(projects);
 				}
 		});
 		
 		eventBus.addHandler(AuthenticationEvent.TYPE, 
 			new AuthenticationEventHandler(){
 				public void onAuthentication(AuthenticationEvent event){
 					if( event.isAuthenticated() ){
 						startConsole();
 					} else if ( !event.isAuthenticated() ){
 						showAuthentication();
 					}
 				}
 		});
 		eventBus.addHandler(PanelTransitionEvent.TYPE, 
 			new PanelTransitionEventHandler(){
 				public void onPanelTransition(PanelTransitionEvent event){
 					if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DEPLOY ){
 						showDeployer();
 					} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.PROJECTS ){
 						showIconPanel();
 					} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DASHBOARD ){
 						manageProjects(event.getName());
 					}
 				}
 		});
 		eventBus.addHandler(UpdateProjectsEvent.TYPE, 
 				new UpdateProjectsEventHandler(){
 					public void onUpdateProjects(UpdateProjectsEvent event){
 						if( onStartUp ){
 							onStartUp = false;
 							if( event.getProjects().length() == 0 ){
 								showDeployer();
 							} else {
 								if ( jsVarHandler.getProjectToken() != null) {
 									showDeployer();
 								} else {
 									showIconPanel();
 								}
 							}
 						}
 		}});
 	}
 	
 	private void showAuthentication(){
 		contentLayoutPanel.clear();
 		contentLayoutPanel.add(authenticationPanel);
 		RootLayoutPanel.get().add(authenticationPanel);
 	}
 	
 	private void startConsole(){
 		eventBus.fireEvent(new AsyncRequestEvent("handleProjects"));
 	}
 	
 	private void showDeployer(){
 		RootLayoutPanel.get().clear();
 		RootLayoutPanel.get().add(this);
 		projectDeployerController = new ProjectDeployerController(clientFactory);
 		contentLayoutPanel.clear();
 		contentLayoutPanel.add(projectDeployerController);
 		contentLayoutPanel.setVisible(true);
 	}
 	
 	private void manageProjects(String projectName){
 		RootLayoutPanel.get().clear();
 		RootLayoutPanel.get().add(this);
 		projectManagerController = new ProjectManagerController(clientFactory, projectName);
 		contentLayoutPanel.clear();
 		contentLayoutPanel.add(projectManagerController);
 		contentLayoutPanel.setVisible(true);
 	}
 	
 	private void showIconPanel(){
 		RootLayoutPanel.get().clear();
 		RootLayoutPanel.get().add(this);
 		contentLayoutPanel.clear();
 		contentLayoutPanel.add(iconPanel);
 		contentLayoutPanel.setVisible(true);
 	}
 }
