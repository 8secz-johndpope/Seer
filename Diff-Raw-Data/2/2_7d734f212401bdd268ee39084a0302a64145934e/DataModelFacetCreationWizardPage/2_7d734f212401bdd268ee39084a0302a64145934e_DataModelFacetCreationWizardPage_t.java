 /*******************************************************************************
  * Copyright (c) 2003, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.wst.web.ui.internal.wizards;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 import java.util.SortedSet;
 
 import org.eclipse.jface.dialogs.IDialogSettings;
 import org.eclipse.osgi.util.NLS;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Group;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
 import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
 import org.eclipse.wst.common.frameworks.datamodel.DataModelPropertyDescriptor;
 import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
 import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
 import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizardPage;
 import org.eclipse.wst.common.frameworks.internal.operations.IProjectCreationPropertiesNew;
 import org.eclipse.wst.common.frameworks.internal.ui.NewProjectGroup;
 import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
 import org.eclipse.wst.common.project.facet.core.IPreset;
 import org.eclipse.wst.common.project.facet.core.IProjectFacet;
 import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
 import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
 import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
 import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
 import org.eclipse.wst.common.project.facet.core.events.IProjectFacetsChangedEvent;
 import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
 import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
 import org.eclipse.wst.common.project.facet.core.util.AbstractFilter;
 import org.eclipse.wst.common.project.facet.core.util.FilterEvent;
 import org.eclipse.wst.common.project.facet.core.util.IFilter;
 import org.eclipse.wst.common.project.facet.ui.ModifyFacetedProjectWizard;
 import org.eclipse.wst.common.project.facet.ui.PresetSelectionPanel;
 import org.eclipse.wst.project.facet.ProductManager;
 import org.eclipse.wst.server.ui.ServerUIUtil;
 import org.eclipse.wst.web.internal.ResourceHandler;
 
 public class DataModelFacetCreationWizardPage extends DataModelWizardPage implements IFacetProjectCreationDataModelProperties {
 
 	private static final String NULL_RUNTIME = "NULL_RUNTIME"; //$NON-NLS-1$
 	private static final String MRU_RUNTIME_STORE = "MRU_RUNTIME_STORE"; //$NON-NLS-1$
 	
 	protected IProjectFacet primaryProjectFacet = null;
 	protected Combo primaryVersionCombo = null;
 	
 	protected Set<IProjectFacetVersion> getFacetConfiguration( final IProjectFacetVersion primaryFacetVersion )
 	{
 	    final Set<IProjectFacetVersion> config = new HashSet<IProjectFacetVersion>();
 	    
 	    for( IProjectFacet fixedFacet : this.fpjwc.getFixedProjectFacets() )
 	    {
 	        if( fixedFacet == primaryFacetVersion.getProjectFacet() )
 	        {
 	            config.add( primaryFacetVersion );
 	        }
 	        else
 	        {
 	            config.add( this.fpjwc.getHighestAvailableVersion( fixedFacet ) );
 	        }
 	    }
 	    
 	    return config;
 	}
 	
 	private static final String[] VALIDATION_PROPERTIES = 
 	{
 	    IProjectCreationPropertiesNew.PROJECT_NAME, 
 	    IProjectCreationPropertiesNew.PROJECT_LOCATION, 
 	    FACET_RUNTIME,
 	    FACETED_PROJECT_WORKING_COPY
 	};
 	
 	protected static GridData gdhfill() {
 		return new GridData(GridData.FILL_HORIZONTAL);
 	}
     
     protected static GridData hspan( final GridData gd,
                                      final int span ) 
     {
         gd.horizontalSpan = span;
         return gd;
     }
 
 	protected Composite createTopLevelComposite(Composite parent) {
 		Composite top = new Composite(parent, SWT.NONE);
 		PlatformUI.getWorkbench().getHelpSystem().setHelp(top, getInfopopID());
 		top.setLayout(new GridLayout());
 		top.setLayoutData(new GridData(GridData.FILL_BOTH));
 		createProjectGroup(top);
 		createServerTargetComposite(top);
 		createPrimaryFacetComposite(top);
         createPresetPanel(top);
         return top;
 	}
 
 	protected void createPrimaryFacetComposite(Composite top) {
 		primaryProjectFacet = ProjectFacetsManager.getProjectFacet( getModuleTypeID() );
 		if (primaryProjectFacet.getVersions().size()  <= 1){
 			//there is no need to create this section if there is only one
 			//facet version to choose from (e.g. utility and static web)
 			return;
 		}
 		
 		final Group group = new Group( top, SWT.NONE );
         group.setLayoutData( gdhfill() );
         group.setLayout( new GridLayout( 1, false ) );
         group.setText( Messages.bind( Messages.FACET_VERSION, new Object [] {primaryProjectFacet.getLabel()}));
 		
         primaryVersionCombo = new Combo( group, SWT.BORDER | SWT.READ_ONLY );
         primaryVersionCombo.setLayoutData( gdhfill() );
         updatePrimaryVersions();
         
         primaryVersionCombo.addSelectionListener
         (
             new SelectionAdapter()
             {
                 @Override
                 public void widgetSelected( final SelectionEvent e )
                 {
                     handlePrimaryFacetVersionSelectedEvent();
                 }
             }
         );
         
         fpjwc.addListener(new IFacetedProjectListener() {
 			public void handleEvent(IFacetedProjectEvent event) {
 				if(event.getType() == IFacetedProjectEvent.Type.PROJECT_FACETS_CHANGED){
 					//this block is to update the combo when the underlying facet version changes
 					IProjectFacetsChangedEvent actionEvent = (IProjectFacetsChangedEvent)event;
 					Set<IProjectFacetVersion> changedVersions = actionEvent.getFacetsWithChangedVersions();
 					
 					boolean foundComboVersion = false;
 					for(Iterator <IProjectFacetVersion> iterator = changedVersions.iterator(); iterator.hasNext() && !foundComboVersion;){
 						IProjectFacetVersion next = iterator.next();
 						if(next.getProjectFacet().equals(primaryProjectFacet)){
 							foundComboVersion = true;
 							final IProjectFacetVersion selectedVersion = next;
 							Display.getDefault().syncExec(new Runnable(){
 								public void run() {
 									String selectedText = primaryVersionCombo.getItem(primaryVersionCombo.getSelectionIndex());
 									if(!selectedText.equals(selectedVersion.getVersionString())){
 										String [] items = primaryVersionCombo.getItems();
 										int selectedVersionIndex = -1;
 										for(int i=0;i<items.length && selectedVersionIndex == -1; i++){
 											if(items[i].equals(selectedVersion.getVersionString())){
 												selectedVersionIndex = i;
 												primaryVersionCombo.select(selectedVersionIndex);
 											}
 										}
 									}	
 								}
 							});
 						}
 					}
 				} else if(event.getType() == IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED){
 					//this block updates the items in the combo when the runtime changes
 					Display.getDefault().syncExec(new Runnable(){
 						public void run() {
 							updatePrimaryVersions();
 						}
 					});
 				}
 			}
         	
         }, IFacetedProjectEvent.Type.PROJECT_FACETS_CHANGED, IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED);
 	}
 	
 	protected IProjectFacet getPrimaryFacet()
 	{
 	    return this.primaryProjectFacet;
 	}
 	
 	protected IProjectFacetVersion getPrimaryFacetVersion()
 	{
 	    IProjectFacetVersion fv = null;
 	    
 	    if( this.primaryProjectFacet.getVersions().size() > 1 )
 	    {
             final int selectedIndex = this.primaryVersionCombo.getSelectionIndex();
     
             if( selectedIndex != -1 )
             {
                 final String fvstr = this.primaryVersionCombo.getItem( selectedIndex );
                 fv = this.primaryProjectFacet.getVersion( fvstr );
             }
 	    }
 	    else
 	    {
 	        fv = this.primaryProjectFacet.getDefaultVersion();
 	    }
         
         return fv;
 	}
 	
 	protected void handlePrimaryFacetVersionSelectedEvent()
 	{
 	    final IProjectFacetVersion fv = getPrimaryFacetVersion();
 
         if( fv != null )
         {
             final Set<IProjectFacetVersion> facets = getFacetConfiguration( fv );
             this.fpjwc.setProjectFacets( facets );
         }
 	}
 
 	protected void updatePrimaryVersions(){
 		IProjectFacetVersion selectedVersion = fpjwc.getProjectFacetVersion(primaryProjectFacet);
 		SortedSet<IProjectFacetVersion> initialVersions = fpjwc.getAvailableVersions(primaryProjectFacet);
         String [] items = new String[initialVersions.size()];
         int i=0;
         int selectedVersionIndex = -1;
         for(Iterator <IProjectFacetVersion> iterator = initialVersions.iterator(); iterator.hasNext(); i++){
         	items[i] = iterator.next().getVersionString();
         	if(selectedVersionIndex == -1 && items[i].equals(selectedVersion.getVersionString())){
         		selectedVersionIndex = i;
         	}
         }
         primaryVersionCombo.clearSelection();
         primaryVersionCombo.setItems(items);
         primaryVersionCombo.select(selectedVersionIndex);
 	}
 	
 	public static class Messages extends NLS {
 		private static final String BUNDLE_NAME = "org.eclipse.wst.web.ui.internal.wizards.facetcreationpagemessages"; //$NON-NLS-1$
 
 		public static String FACET_VERSION;
 		
 		static {
 			// initialize resource bundle
 			NLS.initializeMessages(BUNDLE_NAME, Messages.class);
 		}
 
 		private Messages() {
 		}
 	}
 	
 	protected void createPresetPanel(Composite top) {
 		final IFacetedProjectWorkingCopy fpjwc
             = ( (ModifyFacetedProjectWizard) getWizard() ).getFacetedProjectWorkingCopy();
 		
 		final IFilter<IPreset> filter = new AbstractFilter<IPreset>()
 		{
 		    {
 		        fpjwc.addListener
 		        (
 		            new IFacetedProjectListener()
 		            {
                         public void handleEvent( final IFacetedProjectEvent event )
                         {
                             handleProjectFacetsChangedEvent( (IProjectFacetsChangedEvent) event );
                         }
 		            }, 
 		            IFacetedProjectEvent.Type.PROJECT_FACETS_CHANGED 
 		        );
 		    }
 		    
             public boolean check( final IPreset preset )
             {
                 final IProjectFacetVersion primaryFacetVersion = getPrimaryFacetVersion();
                 return preset.getProjectFacets().contains( primaryFacetVersion );
             }
             
             private void handleProjectFacetsChangedEvent( final IProjectFacetsChangedEvent event )
             {
                 for( IProjectFacetVersion fv : event.getFacetsWithChangedVersions() )
                 {
                     if( fv.getProjectFacet() == getPrimaryFacet() )
                     {
                         final IFilterEvent<IPreset> filterEvent
                             = new FilterEvent<IPreset>( this, IFilterEvent.Type.FILTER_CHANGED );
                         
                         notifyListeners( filterEvent );
                     }
                 }
             }
 		};
 
         final PresetSelectionPanel ppanel = new PresetSelectionPanel( top, fpjwc, filter );
         
         ppanel.setLayoutData( gdhfill() );
 	}
 	
 	public static boolean launchNewRuntimeWizard(Shell shell, IDataModel model) {
 		return launchNewRuntimeWizard(shell, model, null);
 	}
 	
 	public static boolean launchNewRuntimeWizard(Shell shell, final IDataModel model, String serverTypeID) 
 	{
 	    if( model == null )
 	    {
 	        return false;
 	    }
 	    
 		final DataModelPropertyDescriptor[] preAdditionDescriptors = model.getValidPropertyDescriptors(FACET_RUNTIME);
 		
 		final boolean[] keepWaiting = { true };
 		
 		final IDataModelListener listener = new IDataModelListener()
 		{
             public void propertyChanged( final DataModelEvent event )
             {
                 if( event.getPropertyName().equals( FACET_RUNTIME ) &&
                     event.getFlag() == DataModelEvent.VALID_VALUES_CHG )
                 {
                     synchronized( keepWaiting )
                     {
                         keepWaiting[ 0 ] = false;
                         keepWaiting.notify();
                     }
                     
                     model.removeListener( this );
                 }
             }
 		};
 		
 		model.addListener( listener );
 		
 		boolean isOK = ServerUIUtil.showNewRuntimeWizard(shell, serverTypeID, null);
 		
 		if( isOK ) 
 		{
 		    // Do the rest of the processing in a separate thread. Since we are going to block
 		    // and wait, doing this on the UI thread can cause hangs.
 		    
 		    final Thread newRuntimeSelectionThread = new Thread()
 		    {
 		        public void run()
 		        {
         		    // Causes the list of runtimes held by the RuntimeManager to be refreshed and 
         		    // triggers events to listeners on that list.
         		    
         		    RuntimeManager.getRuntimes();
         		    
         		    // Wait until the list of valid values has updated to include the new runtime.
         		    
         		    synchronized( keepWaiting )
         		    {
         		        while( keepWaiting[ 0 ] == true )
         		        {
         		            try
         		            {
         		                keepWaiting.wait();
         		            }
         		            catch( InterruptedException e ) {}
         		        }
         		    }
         		    
         		    // Select the new runtime.
         		    
         			DataModelPropertyDescriptor[] postAdditionDescriptors = model.getValidPropertyDescriptors(FACET_RUNTIME);
         			Object[] preAddition = new Object[preAdditionDescriptors.length];
         			for (int i = 0; i < preAddition.length; i++) {
         				preAddition[i] = preAdditionDescriptors[i].getPropertyValue();
         			}
         			Object[] postAddition = new Object[postAdditionDescriptors.length];
         			for (int i = 0; i < postAddition.length; i++) {
         				postAddition[i] = postAdditionDescriptors[i].getPropertyValue();
         			}
         			Object newAddition = getNewObject(preAddition, postAddition);
         
         			if (newAddition != null) // can this ever be null?
         				model.setProperty(FACET_RUNTIME, newAddition);
 		        }
 		    };
 		    
 		    newRuntimeSelectionThread.start();
 		    
 		    return true;
 		}
 		else
 		{
 		    model.removeListener( listener );
 		    return false;
 		}
 	}
 	
 	public boolean internalLaunchNewRuntimeWizard(Shell shell, IDataModel model) {
 		return launchNewRuntimeWizard(shell, model, getModuleTypeID());
 	}
 	
 	protected String getModuleTypeID() {
 		return null;
 	}
 	
 	protected Combo serverTargetCombo;
 	protected NewProjectGroup projectNameGroup;
 	private final IFacetedProjectWorkingCopy fpjwc;
 	private final IFacetedProjectListener fpjwcListener;
 
 	public DataModelFacetCreationWizardPage(IDataModel dataModel, String pageName) 
 	{
 		super(dataModel, pageName);
 		
         this.fpjwc = (IFacetedProjectWorkingCopy) this.model.getProperty( FACETED_PROJECT_WORKING_COPY );
         
         this.fpjwcListener = new IFacetedProjectListener()
         {
             public void handleEvent( final IFacetedProjectEvent event )
             {
                 validatePage();
             }
         };
         
         this.fpjwc.addListener( this.fpjwcListener, IFacetedProjectEvent.Type.VALIDATION_PROBLEMS_CHANGED );
 	}
 
 	protected void createServerTargetComposite(Composite parent) {
         Group group = new Group(parent, SWT.NONE);
         group.setText(ResourceHandler.TargetRuntime);
         group.setLayoutData(gdhfill());
         group.setLayout(new GridLayout(2, false));
 		serverTargetCombo = new Combo(group, SWT.BORDER | SWT.READ_ONLY);
 		serverTargetCombo.setLayoutData(gdhfill());
 		Button newServerTargetButton = new Button(group, SWT.NONE);
 		newServerTargetButton.setText(ResourceHandler.NewDotDotDot);
 		newServerTargetButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				if (!internalLaunchNewRuntimeWizard(getShell(), model)) {
 					//Bugzilla 135288
 					//setErrorMessage(ResourceHandler.InvalidServerTarget);
 				}
 			}
 		});
 		Control[] deps = new Control[]{newServerTargetButton};
 		synchHelper.synchCombo(serverTargetCombo, FACET_RUNTIME, deps);
 		if (serverTargetCombo.getSelectionIndex() == -1 && serverTargetCombo.getVisibleItemCount() != 0)
 			serverTargetCombo.select(0);
 	}
 
 	protected void createProjectGroup(Composite parent) {
 		IDataModel nestedProjectDM = model.getNestedModel(NESTED_PROJECT_DM);
 		nestedProjectDM.addListener(this);
 		projectNameGroup = new NewProjectGroup(parent, nestedProjectDM);
 	}
 
 	protected String[] getValidationPropertyNames() 
 	{
 	    return VALIDATION_PROPERTIES;
 	}
 
 	public void dispose() {
 		super.dispose();
 		if (projectNameGroup != null)
 			projectNameGroup.dispose();
 		
 		this.fpjwc.removeListener( this.fpjwcListener );
 	}
 
 	public void storeDefaultSettings() {
 		IDialogSettings settings = getDialogSettings();
 		DataModelFacetCreationWizardPage.saveRuntimeSettings(settings, model);
 	}
 
 	public void restoreDefaultSettings() {
 		IDialogSettings settings = getDialogSettings();
 		DataModelFacetCreationWizardPage.restoreRuntimeSettings(settings, model);
 	}
 	
 	public static void saveRuntimeSettings(IDialogSettings settings, IDataModel model){
 		if (settings != null) {
 			String[] mruRuntimeArray = settings.getArray(MRU_RUNTIME_STORE);
 			List mruRuntimes = new ArrayList();
 			if(mruRuntimeArray != null)
 				mruRuntimes.addAll(Arrays.asList(mruRuntimeArray));
 			
 			IRuntime runtime = (IRuntime) model.getProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME);
 			String runtimeName = runtime == null ? NULL_RUNTIME : runtime.getName();
 			
 			if (mruRuntimes.contains(runtimeName)) {
 				mruRuntimes.remove(runtimeName);
 			}
 			mruRuntimes.add(0, runtimeName);
 			while (mruRuntimes.size() > 5) {
 				mruRuntimes.remove(5);
 			}
 			mruRuntimeArray = new String[mruRuntimes.size()];
 			for (int i = 0; i < mruRuntimeArray.length; i++) {
 				mruRuntimeArray[i] = (String) mruRuntimes.get(i);
 			}
 			settings.put(MRU_RUNTIME_STORE, mruRuntimeArray);
 		}
 	}
 	
 	public static void restoreRuntimeSettings(IDialogSettings settings, IDataModel model){
 		if (settings != null) {
 			if (!model.isPropertySet(IFacetProjectCreationDataModelProperties.FACET_RUNTIME)) {
 				boolean runtimeSet = false;
 				String[] mruRuntimeArray = settings.getArray(MRU_RUNTIME_STORE);
 				DataModelPropertyDescriptor[] descriptors = model.getValidPropertyDescriptors(IFacetProjectCreationDataModelProperties.FACET_RUNTIME);
 				List mruRuntimes = new ArrayList();
 				if (mruRuntimeArray == null) {
 					List defRuntimes = ProductManager.getDefaultRuntimes();
 					for (Iterator iter = defRuntimes.iterator(); iter.hasNext();)
 						mruRuntimes.add(((IRuntime) iter.next()).getName());
 				} else {
 					mruRuntimes.addAll(Arrays.asList(mruRuntimeArray));
 				}
 				if (!mruRuntimes.isEmpty()) {
 					for (int i = 0; i < mruRuntimes.size() && !runtimeSet; i++) {
 						for (int j = 0; j < descriptors.length-1 && !runtimeSet; j++) {
 							if (mruRuntimes.get(i).equals(descriptors[j].getPropertyDescription())) {
 								model.setProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME, descriptors[j].getPropertyValue());
 								runtimeSet = true;
 							}
 						}
 						if(!runtimeSet && mruRuntimes.get(i).equals(NULL_RUNTIME)){
 							model.setProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME, descriptors[descriptors.length -1].getPropertyValue());
 							runtimeSet = true;
 						}
 					}
 				}
 				if (!runtimeSet && descriptors.length > 0) {
 					model.setProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME, descriptors[0].getPropertyValue());
 				}
 			}
 		}
 	}
 	
 
 	/**
 	 * Find first newObject that is not in the oldObjects array (using "==").
 	 * 
 	 * @param oldObjects
 	 * @param newObjects
 	 * @return first newObject not found in oldObjects, or <code>null</code> if all found.
 	 * 
 	 * @since 1.0.0
 	 */
 	private static Object getNewObject(Object[] oldObjects, Object[] newObjects) {
 		if (oldObjects != null && newObjects != null && oldObjects.length < newObjects.length) {
 			for (int i = 0; i < newObjects.length; i++) {
 				boolean found = false;
 				Object object = newObjects[i];
 				for (int j = 0; j < oldObjects.length; j++) {
 					if (oldObjects[j] == object) {
 						found = true;
 						break;
 					}
 				}
 				if (!found)
 					return object;
 			}
 		}
 		if (oldObjects == null && newObjects != null && newObjects.length == 1)
 			return newObjects[0];
 		return null;
 	}
 	
 }
