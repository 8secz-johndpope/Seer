 /******************************************************************************* 
  * Copyright (c) 2011 Red Hat, Inc. 
  * Distributed under license by Red Hat, Inc. All rights reserved. 
  * This program is made available under the terms of the 
  * Eclipse Public License v1.0 which accompanies this distribution, 
  * and is available at http://www.eclipse.org/legal/epl-v10.html 
  * 
  * Contributors: 
  * Red Hat, Inc. - initial API and implementation 
  ******************************************************************************/ 
 package org.jboss.ide.eclipse.as.rse.ui;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.wizard.WizardDialog;
 import org.eclipse.rse.core.RSECorePlugin;
 import org.eclipse.rse.core.events.ISystemModelChangeEvent;
 import org.eclipse.rse.core.events.ISystemModelChangeListener;
 import org.eclipse.rse.core.model.IHost;
 import org.eclipse.rse.files.ui.dialogs.SystemRemoteFileDialog;
 import org.eclipse.rse.services.files.IHostFile;
 import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
 import org.eclipse.rse.ui.wizards.newconnection.RSEMainNewConnectionWizard;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.FormLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.wst.server.core.IModule;
 import org.eclipse.wst.server.core.IRuntime;
 import org.eclipse.wst.server.core.IServer;
 import org.eclipse.wst.server.core.IServerWorkingCopy;
 import org.jboss.ide.eclipse.as.core.publishers.PublishUtil;
 import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
 import org.jboss.ide.eclipse.as.core.server.IJBossServerPublishMethodType;
 import org.jboss.ide.eclipse.as.core.server.IJBossServerRuntime;
 import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
 import org.jboss.ide.eclipse.as.core.util.DeploymentPreferenceLoader;
 import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
 import org.jboss.ide.eclipse.as.core.util.ServerConverter;
 import org.jboss.ide.eclipse.as.rse.core.RSEPublishMethod;
 import org.jboss.ide.eclipse.as.rse.core.RSEUtils;
 import org.jboss.ide.eclipse.as.ui.UIUtil;
 import org.jboss.ide.eclipse.as.ui.editor.DeploymentModuleOptionCompositeAssistant;
 import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI;
 import org.jboss.ide.eclipse.as.ui.editor.ModuleDeploymentPage;
import org.jboss.ide.eclipse.as.ui.editor.ServerModeSection;
 import org.jboss.ide.eclipse.as.ui.editor.ServerModeSectionComposite.ChangeServerPropertyCommand;
 import org.jboss.tools.as.wst.server.ui.xpl.ExploreActionProvider;
 
 public class RSEDeploymentPreferenceUI implements IDeploymentTypeUI {
 	static {
 		DeploymentModuleOptionCompositeAssistant.browseBehaviorMap.put("rse", new DeploymentModuleOptionCompositeAssistant.IBrowseBehavior() {
 			public String openBrowseDialog(ModuleDeploymentPage page, String original) {
 				String current = page.getServer().getAttribute(RSEUtils.RSE_SERVER_HOST, (String)null);
 				IHost h = findHost(current, null);
 				return browseClicked4(new Shell(), h);
 			}
 		});
 		ExploreActionProvider.exploreBehaviorMap.put("rse", new ExploreActionProvider.IExploreBehavior() {
 			public void openExplorer(IServer server, IModule[] module) {
 				IDeployableServer ds = ServerConverter.getDeployableServer(server);
 				IPath remoteFolder = new Path(RSEUtils.getDeployRootFolder(ds));
 				IJBossServerPublishMethodType type = DeploymentPreferenceLoader.getCurrentDeploymentMethodType(server);
 				RSEPublishMethod method = (RSEPublishMethod)type.createPublishMethod();
 				method.setBehaviour(ServerConverter.getDeployableServerBehavior(server));
 				if( module != null ) {
 					remoteFolder = PublishUtil.getDeployPath(method, module, ds);
 				}
 				try {
 					method.getFileService();
 					method.ensureConnection(new NullProgressMonitor());
 					IHostFile file = method.getFileService().getFile(remoteFolder.removeLastSegments(1).toOSString(), remoteFolder.lastSegment(), new NullProgressMonitor());
 					String path = file.getAbsolutePath();
 					
 					IRemoteFile rf = method.getFileServiceSubSystem().getRemoteFileObject(path, null);
 					
 					SystemShowInTableAction act = new SystemShowInTableAction(Display.getDefault().getActiveShell()); 
 					act.setSelectedObject(rf);
 					act.run();
 				} catch(Exception e) {
 					e.printStackTrace();
 				}
 			}
 		});
 	}
 	
 	public RSEDeploymentPreferenceUI() {
 		// Do nothing
 	}
 
	@Override @Deprecated
	public void fillComposite(Composite parent, ServerModeSection modeSection) {
		return;
	}

 	@Override 
 	public void fillComposite(Composite parent, IServerModeUICallback callback) {
 		parent.setLayout(new FillLayout());
 		RSEDeploymentPreferenceComposite composite = null;
 		
 		IServerWorkingCopy cServer = callback.getServer();
 		JBossServer jbs = cServer.getOriginal() == null ? 
 				ServerConverter.getJBossServer(cServer) :
 					ServerConverter.getJBossServer(cServer.getOriginal());
 		if( jbs != null && !cServer.getServerType().getId().equals(IJBossToolingConstants.SERVER_AS_70) ) {
 			composite = new JBossRSEDeploymentPrefComposite(parent, SWT.NONE, callback);
 		} else if( cServer.getServerType().getId().equals(IJBossToolingConstants.SERVER_AS_70) ){
 			composite = new JBoss7RSEDeploymentPrefComposite(parent, SWT.NONE, callback);
 			// TODO add for jboss 7
 		} else {
 			composite = new DeployOnlyRSEPrefComposite(parent, SWT.NONE, callback);
 		}
 		//return composite;
 	}
 	
 	public static abstract class RSEDeploymentPreferenceComposite extends Composite implements PropertyChangeListener {
 		protected IServerModeUICallback callback;
 		protected CustomSystemHostCombo combo;
 		protected ModifyListener comboMListener;
 		private boolean updatingFromModelChange = false;
 		public RSEDeploymentPreferenceComposite(Composite parent, int style, IServerModeUICallback callback) {
 			super(parent, style);
 			this.callback = callback;
 			setLayout(new FormLayout());
 			Composite child = new Composite(this, SWT.None);
 			child.setLayoutData(UIUtil.createFormData2(0, 0, null, 0, 0, 5, 100, 0));
 			child.setLayout(new GridLayout());
 			String current = discoverCurrentHost(callback);
 			combo = new CustomSystemHostCombo(child, SWT.NULL, current, "files"); //$NON-NLS-1$
 			comboMListener = new ModifyListener() {
 				public void modifyText(ModifyEvent e) {
 					rseHostChanged();
 				}
 			};
 			combo.getCombo().addModifyListener(comboMListener);
 			
 			createRSEWidgets(child);
 		}
 		
 		protected abstract void createRSEWidgets(Composite child);
 		
 		protected IDeployableServer getServer() {
 			return (IDeployableServer) callback.getServer().loadAdapter(
 					IDeployableServer.class, new NullProgressMonitor());
 		}
 
 		protected String browseClicked3(Shell shell) {
 			return browseClicked4(getShell(), combo.getHost());
 		}
 		
 		protected IJBossServerRuntime getRuntime() {
 			IRuntime rt = callback.getRuntime();
 			if( rt == null ) return null;
 			return (IJBossServerRuntime)rt.loadAdapter(IJBossServerRuntime.class, null);
 		}
 
 		
 		
 		protected void showMessageDialog(String title, IStatus s, Shell shell) {
 			ErrorDialog d = new ErrorDialog(shell, title, null, s, IStatus.INFO | IStatus.ERROR);
 			d.open();
 		}
 		
 		
 		protected String discoverCurrentHost(IServerModeUICallback callback) {
 			String current = callback.getServer().getAttribute(RSEUtils.RSE_SERVER_HOST, (String)null);
 			if( current == null ) {
 				String serverHost = callback.getServer().getHost().toLowerCase();
 				IHost[] hosts = RSECorePlugin.getTheSystemRegistry().getHostsBySubSystemConfigurationCategory("files");
 				String name, hostName;
 				for( int i = 0; i < hosts.length; i++ ) {
 					name = hosts[i].getName();
 					hostName = hosts[i].getHostName();
 					if( hostName.toLowerCase().equals(serverHost)) {
 						callback.getServer().setAttribute(RSEUtils.RSE_SERVER_HOST, name);
 						return hosts[i].getName();
 					}
 				}
 			}
 			return current;
 		}
 		
 		@Override
 		public void dispose () {
 			super.dispose();
 			callback.getServer().removePropertyChangeListener(this);
 		}
 
 		public void propertyChange(PropertyChangeEvent evt) {
 			updatingFromModelChange = true;
 			propertyChangeBody(evt);
 			updatingFromModelChange = false;
 		}
 		
 		protected boolean isUpdatingFromModelChange() {
 			return updatingFromModelChange;
 		}
 		
 		protected void propertyChangeBody(PropertyChangeEvent evt) {
 			if( evt.getPropertyName().equals(RSEUtils.RSE_SERVER_HOST)) {
 				combo.setHostName(evt.getNewValue().toString());
 			}
 		}
 		
 		protected void updateTextIfChanges(Text control, String newValue) {
 			if(!control.getText().equals(newValue)) {
 				control.setText(newValue);
 			}
 		}
 
 		
 		protected void rseHostChanged() {
 			if( !updatingFromModelChange ) {
 				String hostName = combo.getHost() == null ? null : combo.getHost().getAliasName();
 				String oldVal = callback.getServer().getAttribute(RSEUtils.RSE_SERVER_HOST, (String)null);
 				if( !hostName.equals(oldVal) && !updatingFromModelChange) {
 					callback.execute(new ChangeServerPropertyCommand(
 							callback.getServer(), RSEUtils.RSE_SERVER_HOST, hostName, "localhost", 
 							RSEUIMessages.CHANGE_RSE_HOST));
 					callback.execute(new ChangeServerPropertyCommand(
 							callback.getServer(), "hostname", combo.getHost().getHostName(), 
 							RSEUIMessages.CHANGE_HOSTNAME));
 				}
 			}
 		}
 
 		public class CustomSystemHostCombo extends Composite implements ModifyListener, ISystemModelChangeListener {
 			private String fileSubSystem;
 			private Combo combo;
 			private Button newHost;
 			private IHost currentHost;
 			private String currentHostName;
 			private IHost[] hosts;
 			private String[] hostsAsStrings;
 			public CustomSystemHostCombo(Composite parent, int style, String initialHostName, String fileSubSystem) {
 				super(parent, style);
 				this.fileSubSystem = fileSubSystem;
 				this.currentHostName = initialHostName;
 				this.hosts = RSECorePlugin.getTheSystemRegistry().getHostsBySubSystemConfigurationCategory(fileSubSystem);
 				this.currentHost = findHost(initialHostName);
 				RSECorePlugin.getTheSystemRegistry().addSystemModelChangeListener(this);			
 				
 				// Where I belong in the parent
 				GridData data = new GridData();
 				// horizontal clues
 				data.horizontalAlignment = GridData.FILL;
 			    data.grabExcessHorizontalSpace = true;        
 		        data.widthHint =  200;
 				// vertical clues
 				data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING; //GridData.CENTER;
 			    data.grabExcessVerticalSpace = false; // true;        
 				this.setLayoutData(data);
 	
 				// What's inside me
 				setLayout(new FormLayout());
 				Label l = new Label(this, SWT.NONE);
 				l.setText("Host");
 				newHost = new Button(this, SWT.NONE);
 				newHost.setText("New Host...");
 				newHost.setLayoutData(UIUtil.createFormData2(0, 0, null, 0, null, 0, 100, -5));
 				newHost.addSelectionListener(new SelectionListener() {
 					public void widgetSelected(SelectionEvent e) {
 						newHostClicked();
 					}
 					public void widgetDefaultSelected(SelectionEvent e) {
 					}
 				});
 				
 				combo = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
 				l.setLayoutData(UIUtil.createFormData2(0, 5, null, 0, 0, 0, null, 0));
 				combo.setLayoutData(UIUtil.createFormData2(0, 0, null, 0, l, 5, newHost, -5));
 				refreshConnections();
 				combo.addModifyListener(this);
 			}
 			
 			protected void newHostClicked() {
 				RSEMainNewConnectionWizard newConnWizard = new RSEMainNewConnectionWizard();
 				WizardDialog d = new WizardDialog(getShell(), newConnWizard);
 				d.open();
 			}
 			
 			public IHost findHost(String name) {
 				return RSEDeploymentPreferenceUI.findHost(name, hosts);
 			}
 
 			public Combo getCombo() {
 				return combo;
 			}
 			
 			public IHost getHost() {
 				return currentHost;
 			}
 			
 			public String getHostName() {
 				return currentHostName;
 			}
 			
 			public void setHostName(String name) {
 				this.currentHostName = name;
 				this.currentHost = findHost(currentHostName);
 				if( currentHost == null )
 					combo.clearSelection();
 				else {
 					String[] items = combo.getItems();
 					for( int i = 0; i < items.length; i++ ) {
 						if( items[i].equals(currentHost.getAliasName())) {
 							combo.select(i);
 							return;
 						}
 					}
 				}
 			}
 			
 			public void refreshConnections() {
 				hosts = RSECorePlugin.getTheSystemRegistry().getHostsBySubSystemConfigurationCategory(fileSubSystem);
 				hostsAsStrings = new String[hosts.length];
 				int currentHostIndex = -1;
 				for( int i = 0; i < hosts.length; i++ ) {
 					hostsAsStrings[i] = hosts[i].getAliasName();
 					if( currentHostIndex == -1 && currentHostName != null 
 							&& hostsAsStrings[i].equals(currentHostName)) {
 						currentHostIndex = i;
 					}
 				}
 				
 				// refill the combo thingie
 				combo.setItems(hostsAsStrings);
 				if( currentHostIndex != -1 ) // set the current host
 					combo.select(currentHostIndex);
 				else
 					combo.clearSelection();
 			}
 	
 			@Override
 			public void modifyText(ModifyEvent e) {
 				int index = combo.getSelectionIndex();
 				if( index != -1 ) {
 					String s = combo.getItem(index);
 					for( int i = 0; i < hosts.length; i++ ) {
 						if( hosts[i].getAliasName().equals(s)) {
 							currentHost = hosts[i];
 							currentHostName = currentHost.getAliasName();
 							return;
 						}
 					}
 				}
 			}
 			public void systemModelResourceChanged(ISystemModelChangeEvent event) {
 				if( combo.isDisposed())
 					return;
 				Display.getDefault().asyncExec(new Runnable(){
 					public void run() {
 						combo.removeModifyListener(comboMListener);
 						refreshConnections();
 						combo.addModifyListener(comboMListener);
 					}
 				});
 			}
 			@Override
 			public void dispose () {
 				super.dispose();
 				RSECorePlugin.getTheSystemRegistry().removeSystemModelChangeListener(this);			
 			}
 		}
 	}
 	
 	
 	public static IHost findHost(String name, IHost[] hosts) {
 		if( hosts == null )
 			hosts = RSECorePlugin.getTheSystemRegistry().getHostsBySubSystemConfigurationCategory("files");
 		for( int i = 0; i < hosts.length; i++ ) {
 			if( hosts[i].getAliasName().equals(name))
 				return hosts[i];
 		}
 		return null;
 	}
 
 	public static String browseClicked4(Shell s, IHost host) {
 		SystemRemoteFileDialog d = new SystemRemoteFileDialog(
 				s, RSEUIMessages.BROWSE_REMOTE_SYSTEM, host);
 		if( d.open() == Dialog.OK) {
 			Object o = d.getOutputObject();
 			if( o instanceof IRemoteFile ) {
 				String path = ((IRemoteFile)o).getAbsolutePath();
 				return path;
 			}
 		}
 		return null;
 	}
 }
