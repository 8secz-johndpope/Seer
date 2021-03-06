 package org.ovirt.engine.ui.uicommonweb.models.userportal;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.ovirt.engine.core.common.VdcActionUtils;
 import org.ovirt.engine.core.common.action.AddVmFromScratchParameters;
 import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
 import org.ovirt.engine.core.common.action.AddVmTemplateParameters;
 import org.ovirt.engine.core.common.action.ChangeDiskCommandParameters;
 import org.ovirt.engine.core.common.action.ChangeVMClusterParameters;
 import org.ovirt.engine.core.common.action.RemoveVmParameters;
 import org.ovirt.engine.core.common.action.RunVmOnceParams;
 import org.ovirt.engine.core.common.action.VdcActionType;
 import org.ovirt.engine.core.common.action.VmManagementParametersBase;
 import org.ovirt.engine.core.common.businessentities.DiskImage;
 import org.ovirt.engine.core.common.businessentities.DisplayType;
 import org.ovirt.engine.core.common.businessentities.MigrationSupport;
 import org.ovirt.engine.core.common.businessentities.UsbPolicy;
 import org.ovirt.engine.core.common.businessentities.VDS;
 import org.ovirt.engine.core.common.businessentities.VDSGroup;
 import org.ovirt.engine.core.common.businessentities.VM;
 import org.ovirt.engine.core.common.businessentities.VMStatus;
 import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
 import org.ovirt.engine.core.common.businessentities.VmOsType;
 import org.ovirt.engine.core.common.businessentities.VmTemplate;
 import org.ovirt.engine.core.common.businessentities.VmType;
 import org.ovirt.engine.core.common.businessentities.VolumeType;
 import org.ovirt.engine.core.common.businessentities.storage_domains;
 import org.ovirt.engine.core.common.businessentities.storage_pool;
 import org.ovirt.engine.core.common.businessentities.vm_pools;
 import org.ovirt.engine.core.common.queries.GetAllVmPoolsAttachedToUserParameters;
 import org.ovirt.engine.core.common.queries.GetUserVmsByUserIdAndGroupsParameters;
 import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
 import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
 import org.ovirt.engine.core.common.queries.VdcQueryType;
 import org.ovirt.engine.core.compat.Event;
 import org.ovirt.engine.core.compat.EventArgs;
 import org.ovirt.engine.core.compat.EventDefinition;
 import org.ovirt.engine.core.compat.Guid;
 import org.ovirt.engine.core.compat.NGuid;
 import org.ovirt.engine.core.compat.ObservableCollection;
 import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
 import org.ovirt.engine.core.compat.StringHelper;
 import org.ovirt.engine.core.compat.Version;
 import org.ovirt.engine.ui.frontend.AsyncQuery;
 import org.ovirt.engine.ui.frontend.Frontend;
 import org.ovirt.engine.ui.frontend.INewAsyncCallback;
 import org.ovirt.engine.ui.uicommonweb.Cloner;
 import org.ovirt.engine.ui.uicommonweb.DataProvider;
 import org.ovirt.engine.ui.uicommonweb.Linq;
 import org.ovirt.engine.ui.uicommonweb.UICommand;
 import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
 import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
 import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
 import org.ovirt.engine.ui.uicommonweb.models.ListModel;
 import org.ovirt.engine.ui.uicommonweb.models.Model;
 import org.ovirt.engine.ui.uicommonweb.models.configure.UserPortalPermissionListModel;
 import org.ovirt.engine.ui.uicommonweb.models.pools.PoolDiskListModel;
 import org.ovirt.engine.ui.uicommonweb.models.pools.PoolGeneralModel;
 import org.ovirt.engine.ui.uicommonweb.models.pools.PoolInterfaceListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.BootSequenceModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.ConsoleModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.NewTemplateVmModelBehavior;
 import org.ovirt.engine.ui.uicommonweb.models.vms.RdpConsoleModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.RunOnceModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.SpiceConsoleModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalExistingVmModelBehavior;
 import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalNewVmModelBehavior;
 import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalVmEventListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalVmSnapshotListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VmDiskListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VmInterfaceListModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VmMonitorModel;
 import org.ovirt.engine.ui.uicommonweb.models.vms.VncConsoleModel;
 import org.ovirt.engine.ui.uicompat.ConstantsManager;
 import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
 import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
 
 public class UserPortalListModel extends IUserPortalListModel implements IVmPoolResolutionService
 {
 
     public static EventDefinition SearchCompletedEventDefinition;
     private Event privateSearchCompletedEvent;
 
     public Event getSearchCompletedEvent()
     {
         return privateSearchCompletedEvent;
     }
 
     private void setSearchCompletedEvent(Event value)
     {
         privateSearchCompletedEvent = value;
     }
 
     private UICommand privateNewDesktopCommand;
 
     public UICommand getNewDesktopCommand()
     {
         return privateNewDesktopCommand;
     }
 
     private void setNewDesktopCommand(UICommand value)
     {
         privateNewDesktopCommand = value;
     }
 
     private UICommand privateNewServerCommand;
 
     public UICommand getNewServerCommand()
     {
         return privateNewServerCommand;
     }
 
     private void setNewServerCommand(UICommand value)
     {
         privateNewServerCommand = value;
     }
 
     private UICommand privateEditCommand;
 
     public UICommand getEditCommand()
     {
         return privateEditCommand;
     }
 
     private void setEditCommand(UICommand value)
     {
         privateEditCommand = value;
     }
 
     private UICommand privateRemoveCommand;
 
     public UICommand getRemoveCommand()
     {
         return privateRemoveCommand;
     }
 
     private void setRemoveCommand(UICommand value)
     {
         privateRemoveCommand = value;
     }
 
     private UICommand privateSaveCommand;
 
     public UICommand getSaveCommand()
     {
         return privateSaveCommand;
     }
 
     private void setSaveCommand(UICommand value)
     {
         privateSaveCommand = value;
     }
 
     private UICommand privateRunOnceCommand;
 
     public UICommand getRunOnceCommand()
     {
         return privateRunOnceCommand;
     }
 
     private void setRunOnceCommand(UICommand value)
     {
         privateRunOnceCommand = value;
     }
 
     private UICommand privateChangeCdCommand;
 
     public UICommand getChangeCdCommand()
     {
         return privateChangeCdCommand;
     }
 
     private void setChangeCdCommand(UICommand value)
     {
         privateChangeCdCommand = value;
     }
 
     private UICommand privateNewTemplateCommand;
 
     public UICommand getNewTemplateCommand()
     {
         return privateNewTemplateCommand;
     }
 
     private void setNewTemplateCommand(UICommand value)
     {
         privateNewTemplateCommand = value;
     }
 
     private EntityModel vmGeneralModel;
     private ListModel vmSnapshotListModel;
     private EntityModel vmMonitorModel;
     private ListModel vmDiskListModel;
     private ListModel vmInterfaceListModel;
     private ListModel permissionListModel;
     private ListModel vmEventListModel;
     private ListModel vmAppListModel;
     private EntityModel poolGeneralModel;
     private ListModel poolDiskListModel;
     private ListModel poolInterfaceListModel;
     private ArrayList<VM> privatevms;
 
     public ArrayList<VM> getvms()
     {
         return privatevms;
     }
 
     public void setvms(ArrayList<VM> value)
     {
         privatevms = value;
     }
 
     private ArrayList<vm_pools> privatepools;
 
     public ArrayList<vm_pools> getpools()
     {
         return privatepools;
     }
 
     public void setpools(ArrayList<vm_pools> value)
     {
         privatepools = value;
     }
 
     private VM privatetempVm;
 
     public VM gettempVm()
     {
         return privatetempVm;
     }
 
     public void settempVm(VM value)
     {
         privatetempVm = value;
     }
 
     private storage_domains privatestorageDomain;
 
     public storage_domains getstorageDomain()
     {
         return privatestorageDomain;
     }
 
     public void setstorageDomain(storage_domains value)
     {
         privatestorageDomain = value;
     }
 
     private HashMap<Version, ArrayList<String>> CustomPropertiesKeysList;
 
     public HashMap<Version, ArrayList<String>> getCustomPropertiesKeysList() {
         return CustomPropertiesKeysList;
     }
 
     public void setCustomPropertiesKeysList(HashMap<Version, ArrayList<String>> customPropertiesKeysList) {
         CustomPropertiesKeysList = customPropertiesKeysList;
     }
 
     private final HashMap<Guid, ArrayList<ConsoleModel>> cachedConsoleModels;
 
     static
     {
         SearchCompletedEventDefinition = new EventDefinition("SearchCompleted", UserPortalListModel.class); //$NON-NLS-1$
     }
 
     public UserPortalListModel()
     {
         setSearchCompletedEvent(new Event(SearchCompletedEventDefinition));
 
         cachedConsoleModels = new HashMap<Guid, ArrayList<ConsoleModel>>();
 
         setNewDesktopCommand(new UICommand("NewDesktop", this)); //$NON-NLS-1$
         setNewServerCommand(new UICommand("NewServer", this)); //$NON-NLS-1$
         setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
         setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
         setSaveCommand(new UICommand("Save", this)); //$NON-NLS-1$
         setRunOnceCommand(new UICommand("RunOnce", this)); //$NON-NLS-1$
         setChangeCdCommand(new UICommand("ChangeCD", this)); //$NON-NLS-1$
         setNewTemplateCommand(new UICommand("NewTemplate", this)); //$NON-NLS-1$
 
         UpdateActionAvailability();
 
         AsyncQuery _asyncQuery = new AsyncQuery();
         _asyncQuery.setModel(this);
         _asyncQuery.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model, Object result)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model;
                 if (result != null)
                 {
                     userPortalListModel.setCustomPropertiesKeysList(new HashMap<Version, ArrayList<String>>());
                     HashMap<Version, String> dictionary = (HashMap<Version, String>) result;
                     for (Map.Entry<Version, String> keyValuePair : dictionary.entrySet())
                     {
                         userPortalListModel.CustomPropertiesKeysList.put(keyValuePair.getKey(),
                                 new ArrayList<String>());
                         for (String s : keyValuePair.getValue().split("[;]", -1)) //$NON-NLS-1$
                         {
                             userPortalListModel.CustomPropertiesKeysList.get(keyValuePair.getKey()).add(s);
                         }
                     }
                 }
 
             }
         };
         if (getCustomPropertiesKeysList() == null) {
             AsyncDataProvider.GetCustomPropertiesList(_asyncQuery);
         }
     }
 
     @Override
     public void setItems(Iterable value)
     {
         if (items != value)
         {
             ItemsChanging(value, items);
             items = value;
             getItemsChangedEvent().raise(this, EventArgs.Empty);
             OnPropertyChanged(new PropertyChangedEventArgs("Items")); //$NON-NLS-1$
         }
     }
 
     @Override
     protected void SyncSearch()
     {
         super.SyncSearch();
 
         AsyncQuery _asyncQuery = new AsyncQuery();
         _asyncQuery.setModel(this);
         _asyncQuery.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model, Object ReturnValue)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model;
                 userPortalListModel.setvms((ArrayList<VM>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
                 userPortalListModel.OnVmAndPoolLoad();
             }
         };
 
         Frontend.RunQuery(VdcQueryType.GetUserVmsByUserIdAndGroups,
                 new GetUserVmsByUserIdAndGroupsParameters(Frontend.getLoggedInUser().getUserId()),
                 _asyncQuery);
 
         AsyncQuery _asyncQuery1 = new AsyncQuery();
         _asyncQuery1.setModel(this);
         _asyncQuery1.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model, Object ReturnValue)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model;
                 userPortalListModel.setpools((ArrayList<vm_pools>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
                 userPortalListModel.OnVmAndPoolLoad();
             }
         };
 
         Frontend.RunQuery(VdcQueryType.GetAllVmPoolsAttachedToUser,
                 new GetAllVmPoolsAttachedToUserParameters(Frontend.getLoggedInUser().getUserId()),
                 _asyncQuery1);
 
     }
 
     @Override
     protected void AsyncSearch()
     {
         super.AsyncSearch();
         SyncSearch();
     }
 
     @Override
     protected void InitDetailModels()
     {
         super.InitDetailModels();
 
         vmGeneralModel = new VmGeneralModel();
         vmGeneralModel.setIsAvailable(false);
 
         vmSnapshotListModel = new UserPortalVmSnapshotListModel();
         vmSnapshotListModel.setIsAvailable(false);
 
         vmMonitorModel = new VmMonitorModel();
         vmMonitorModel.setIsAvailable(false);
 
         vmDiskListModel = new VmDiskListModel();
         vmDiskListModel.setIsAvailable(false);
 
         vmInterfaceListModel = new VmInterfaceListModel();
         vmInterfaceListModel.setIsAvailable(false);
 
         permissionListModel = new UserPortalPermissionListModel();
         permissionListModel.setIsAvailable(false);
 
         vmEventListModel = new UserPortalVmEventListModel();
         vmEventListModel.setIsAvailable(false);
 
         vmAppListModel = new VmAppListModel();
         vmAppListModel.setIsAvailable(false);
 
         poolGeneralModel = new PoolGeneralModel();
         poolGeneralModel.setIsAvailable(false);
 
         poolDiskListModel = new PoolDiskListModel();
         poolDiskListModel.setIsAvailable(false);
 
         poolInterfaceListModel = new PoolInterfaceListModel();
         poolInterfaceListModel.setIsAvailable(false);
 
         ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
         list.add(vmGeneralModel);
         list.add(poolGeneralModel);
         list.add(vmInterfaceListModel);
         list.add(poolInterfaceListModel);
         list.add(vmDiskListModel);
         list.add(poolDiskListModel);
         list.add(vmSnapshotListModel);
         list.add(permissionListModel);
         list.add(vmEventListModel);
         list.add(vmAppListModel);
         list.add(vmMonitorModel);
 
         setDetailModels(list);
 
         permissionListModel.setIsAvailable(true);
         vmEventListModel.setIsAvailable(true);
         vmAppListModel.setIsAvailable(true);
     }
 
     @Override
     protected Object ProvideDetailModelEntity(Object selectedItem)
     {
         // Each item in this list model is not a business entity,
         // therefore select an Entity property to provide it to
         // the detail models.
 
         EntityModel model = (EntityModel) selectedItem;
         if (model == null)
         {
             return null;
         }
 
         return model.getEntity();
     }
 
     @Override
     protected void UpdateDetailsAvailability()
     {
         super.UpdateDetailsAvailability();
 
         UserPortalItemModel item = (UserPortalItemModel) getSelectedItem();
 
         vmGeneralModel.setIsAvailable(item != null && !item.getIsPool());
         vmSnapshotListModel.setIsAvailable(item != null && !item.getIsPool());
         vmMonitorModel.setIsAvailable(item != null && !item.getIsPool());
         vmDiskListModel.setIsAvailable(item != null && !item.getIsPool());
         vmInterfaceListModel.setIsAvailable(item != null && !item.getIsPool());
         vmEventListModel.setIsAvailable(item != null && !item.getIsPool());
 
         poolGeneralModel.setIsAvailable(item != null && item.getIsPool());
         poolDiskListModel.setIsAvailable(item != null && item.getIsPool());
         poolInterfaceListModel.setIsAvailable(item != null && item.getIsPool());
     }
 
     @Override
     public void ExecuteCommand(UICommand command)
     {
         super.ExecuteCommand(command);
 
         if (command == getNewDesktopCommand())
         {
             NewDesktop();
         }
         if (command == getNewServerCommand())
         {
             NewServer();
         }
         else if (command == getEditCommand())
         {
             Edit();
         }
         else if (command == getRemoveCommand())
         {
             remove();
         }
         else if (command == getSaveCommand())
         {
             OnSave();
         }
         else if (command == getRunOnceCommand())
         {
             RunOnce();
         }
         else if (command == getChangeCdCommand())
         {
             ChangeCD();
         }
         else if (command == getNewTemplateCommand())
         {
             NewTemplate();
         }
         else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
         {
             Cancel();
         }
         else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
         {
             OnRemove();
         }
         else if (StringHelper.stringsEqual(command.getName(), "OnRunOnce")) //$NON-NLS-1$
         {
             OnRunOnce();
         }
         else if (StringHelper.stringsEqual(command.getName(), "OnChangeCD")) //$NON-NLS-1$
         {
             OnChangeCD();
         }
         else if (StringHelper.stringsEqual(command.getName(), "OnNewTemplate")) //$NON-NLS-1$
         {
             OnNewTemplate();
         }
         else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
         {
             OnSave();
         }
         else if (command.getName().equals("closeVncInfo")) { //$NON-NLS-1$
             setWindow(null);
         }
     }
 
     private void NewTemplate()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null)
         {
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
         UnitVmModel windowModel = new UnitVmModel(new NewTemplateVmModelBehavior(vm));
         setWindow(windowModel);
         windowModel.setTitle(ConstantsManager.getInstance().getConstants().newTemplateTitle());
         windowModel.setHashName("new_template"); //$NON-NLS-1$
         windowModel.setIsNew(true);
         windowModel.setVmType(vm.getvm_type());
         windowModel.Initialize(null);
 
         UICommand tempVar = new UICommand("OnNewTemplate", this); //$NON-NLS-1$
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar.setIsDefault(true);
         windowModel.getCommands().add(tempVar);
         UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar2.setIsCancel(true);
         windowModel.getCommands().add(tempVar2);
     }
 
     private void OnNewTemplate()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null)
         {
             Cancel();
             return;
         }
 
         UnitVmModel model = (UnitVmModel) getWindow();
 
         if (!model.Validate())
         {
             model.setIsValid(false);
         }
         else
         {
             model.StartProgress(null);
             String name = (String) model.getName().getEntity();
 
             // Check name unicitate.
             AsyncDataProvider.IsTemplateNameUnique(new AsyncQuery(this,
                     new INewAsyncCallback() {
                         @Override
                         public void OnSuccess(Object target, Object returnValue) {
 
                             UserPortalListModel userPortalListModel = (UserPortalListModel) target;
                             boolean isNameUnique = (Boolean) returnValue;
                             if (!isNameUnique)
                             {
 
                                 UnitVmModel vmModel = (UnitVmModel) userPortalListModel.getWindow();
                                 vmModel.getName().getInvalidityReasons().clear();
                                 vmModel
                                         .getName()
                                         .getInvalidityReasons()
                                         .add(ConstantsManager.getInstance()
                                                 .getConstants()
                                                 .nameMustBeUniqueInvalidReason());
                                 vmModel.getName().setIsValid(false);
                                 vmModel.setIsValid(false);
                                 stopProgress(target);
                             }
                             else
                             {
                                 userPortalListModel.PostNameUniqueCheck(userPortalListModel);
                             }
 
                         }
                     }),
                     name);
         }
     }
 
     public void PostNameUniqueCheck(UserPortalListModel userPortalListModel)
     {
         UnitVmModel model = (UnitVmModel) userPortalListModel.getWindow();
         UserPortalItemModel selectedItem = (UserPortalItemModel) userPortalListModel.getSelectedItem();
         VM vm = (VM) selectedItem.getEntity();
 
         VM tempVar = new VM();
         tempVar.setId(vm.getId());
         tempVar.setvm_type(model.getVmType());
         tempVar.setvm_os((VmOsType) model.getOSType().getSelectedItem());
         tempVar.setnum_of_monitors((Integer) model.getNumOfMonitors().getSelectedItem());
         tempVar.setAllowConsoleReconnect((Boolean) model.getAllowConsoleReconnect().getEntity());
         tempVar.setvm_domain(model.getDomain().getIsAvailable() ? (String) model.getDomain().getSelectedItem() : ""); //$NON-NLS-1$
         tempVar.setvm_mem_size_mb((Integer) model.getMemSize().getEntity());
         tempVar.setMinAllocatedMem((Integer) model.getMinAllocatedMemory().getEntity());
         tempVar.setvds_group_id(((VDSGroup) model.getCluster().getSelectedItem()).getId());
         tempVar.settime_zone(model.getTimeZone().getIsAvailable() && model.getTimeZone().getSelectedItem() != null ? ((Map.Entry<String, String>) model.getTimeZone()
                 .getSelectedItem()).getKey()
                 : ""); //$NON-NLS-1$
         tempVar.setnum_of_sockets((Integer) model.getNumOfSockets().getEntity());
         tempVar.setcpu_per_socket((Integer) model.getTotalCPUCores().getEntity()
                 / (Integer) model.getNumOfSockets().getEntity());
         tempVar.setusb_policy((UsbPolicy) model.getUsbPolicy().getSelectedItem());
         tempVar.setis_auto_suspend(false);
         tempVar.setis_stateless((Boolean) model.getIsStateless().getEntity());
         tempVar.setdefault_boot_sequence(model.getBootSequence());
         tempVar.setauto_startup((Boolean) model.getIsHighlyAvailable().getEntity());
         tempVar.setiso_path(model.getCdImage().getIsChangable() ? (String) model.getCdImage().getSelectedItem() : ""); //$NON-NLS-1$
         tempVar.setinitrd_url(vm.getinitrd_url());
         tempVar.setkernel_url(vm.getkernel_url());
         tempVar.setkernel_params(vm.getkernel_params());
         VM newvm = tempVar;
 
         EntityModel displayProtocolSelectedItem = (EntityModel) model.getDisplayProtocol().getSelectedItem();
         newvm.setdefault_display_type((DisplayType) displayProtocolSelectedItem.getEntity());
 
         EntityModel prioritySelectedItem = (EntityModel) model.getPriority().getSelectedItem();
         newvm.setpriority((Integer) prioritySelectedItem.getEntity());
 
         AddVmTemplateParameters addVmTemplateParameters =
                 new AddVmTemplateParameters(newvm,
                         (String) model.getName().getEntity(),
                         (String) model.getDescription().getEntity());
         addVmTemplateParameters.setDestinationStorageDomainId(((storage_domains) model.getStorageDomain()
                 .getSelectedItem()).getId());
         addVmTemplateParameters.setPublicUse((Boolean) model.getIsTemplatePublic().getEntity());
 
         Frontend.RunAction(VdcActionType.AddVmTemplate, addVmTemplateParameters,
                 new IFrontendActionAsyncCallback() {
                     @Override
                     public void Executed(FrontendActionAsyncResult result) {
                         stopProgress(result.getState());
                         Cancel();
                     }
                 }, this);
     }
 
     @Override
     protected void OnSelectedItemChanged()
     {
         super.OnSelectedItemChanged();
         UpdateActionAvailability();
     }
 
     private void RunOnce()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null || selectedItem.getEntity() == null)
         {
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
 
         RunOnceModel model = new RunOnceModel();
         setWindow(model);
         model.setTitle(ConstantsManager.getInstance().getConstants().runVirtualMachinesTitle());
         model.setHashName("run_virtual_machine"); //$NON-NLS-1$
         model.getAttachIso().setEntity(false);
         model.getAttachFloppy().setEntity(false);
         model.getRunAsStateless().setEntity(vm.getis_stateless());
         model.getRunAndPause().setEntity(false);
         model.setHwAcceleration(true);
 
         fillIsoList(vm);
         fillFloppyImages(vm);
 
         // passing Kernel parameters
         model.getKernel_parameters().setEntity(vm.getkernel_params());
         model.getKernel_path().setEntity(vm.getkernel_url());
         model.getInitrd_path().setEntity(vm.getinitrd_url());
 
         model.getCustomProperties().setEntity(vm.getCustomProperties());
 
         model.setIsLinux_Unassign_UnknownOS(DataProvider.IsLinuxOsType(vm.getvm_os())
                 || vm.getvm_os() == VmOsType.Unassigned || vm.getvm_os() == VmOsType.Other);
 
         model.getIsLinuxOptionsAvailable().setEntity(model.getIsLinux_Unassign_UnknownOS());
         model.setIsWindowsOS(DataProvider.IsWindowsOsType(vm.getvm_os()));
         model.getIsVmFirstRun().setEntity(!vm.getis_initialized());
         model.getSysPrepDomainName().setSelectedItem(vm.getvm_domain());
 
         // Update Domain list
         AsyncDataProvider.GetDomainList(new AsyncQuery(model,
                 new INewAsyncCallback() {
                     @Override
                     public void OnSuccess(Object target, Object returnValue1) {
 
                         RunOnceModel runOnceModel = (RunOnceModel) target;
                         List<String> domains = (List<String>) returnValue1;
                         String oldDomain = (String) runOnceModel.getSysPrepDomainName().getSelectedItem();
                         if (oldDomain != null && !oldDomain.equals("") && !domains.contains(oldDomain)) //$NON-NLS-1$
                         {
                             domains.add(0, oldDomain);
                         }
                         runOnceModel.getSysPrepDomainName().setItems(domains);
                         runOnceModel.getSysPrepDomainName().setSelectedItem((oldDomain != null) ? oldDomain
                                 : Linq.FirstOrDefault(domains));
 
                     }
                 }),
                 true);
 
         // Display protocols.
         EntityModel tempVar = new EntityModel();
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().VNCTitle());
         tempVar.setEntity(DisplayType.vnc);
         EntityModel vncProtocol = tempVar;
 
         EntityModel tempVar2 = new EntityModel();
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().spiceTitle());
         tempVar2.setEntity(DisplayType.qxl);
         EntityModel qxlProtocol = tempVar2;
 
         ArrayList<EntityModel> items = new ArrayList<EntityModel>();
         items.add(vncProtocol);
         items.add(qxlProtocol);
         model.getDisplayProtocol().setItems(items);
         model.getDisplayProtocol().setSelectedItem(vm.getdefault_display_type() == DisplayType.vnc ? vncProtocol
                 : qxlProtocol);
 
         model.setCustomPropertiesKeysList(getCustomPropertiesKeysList()
                 .get(vm.getvds_group_compatibility_version()));
 
         // Boot sequence.
         AsyncQuery _asyncQuery2 = new AsyncQuery();
         _asyncQuery2.setModel(this);
 
         _asyncQuery2.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model3, Object ReturnValue)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model3;
                 boolean hasNics =
                         ((ArrayList<VmNetworkInterface>) ((VdcQueryReturnValue) ReturnValue).getReturnValue()).size() > 0;
 
                 if (!hasNics)
                 {
                     BootSequenceModel bootSequenceModel =
                             ((RunOnceModel) userPortalListModel.getWindow()).getBootSequence();
                     bootSequenceModel.getNetworkOption().setIsChangable(false);
                     bootSequenceModel.getNetworkOption()
                             .getChangeProhibitionReasons()
                             .add("Virtual Machine must have at least one network interface defined to boot from network."); //$NON-NLS-1$
                 }
             }
         };
 
         Frontend.RunQuery(VdcQueryType.GetVmInterfacesByVmId, new GetVmByVmIdParameters(vm.getId()), _asyncQuery2);
 
         UICommand tempVar3 = new UICommand("OnRunOnce", this); //$NON-NLS-1$
         tempVar3.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar3.setIsDefault(true);
         model.getCommands().add(tempVar3);
         UICommand tempVar4 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar4.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar4.setIsCancel(true);
         model.getCommands().add(tempVar4);
     }
 
     protected void fillIsoList(VM vm) {
         AsyncQuery getIsoImagesQuery = new AsyncQuery();
         getIsoImagesQuery.setModel(this);
 
         getIsoImagesQuery.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model1, Object result)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model1;
                 RunOnceModel runOnceModel = (RunOnceModel) userPortalListModel.getWindow();
                 List<String> images = (List<String>) ((VdcQueryReturnValue) result).getReturnValue();
                 runOnceModel.getIsoImage().setItems(images);
 
                 if (runOnceModel.getIsoImage().getIsChangable()
                         && runOnceModel.getIsoImage().getSelectedItem() == null)
                 {
                     runOnceModel.getIsoImage().setSelectedItem(Linq.FirstOrDefault(images));
                 }
             }
         };
 
         AsyncDataProvider.GetIrsImageList(getIsoImagesQuery, vm.getstorage_pool_id());
     }
 
     protected void fillFloppyImages(VM vm) {
         AsyncQuery getFloppyQuery = new AsyncQuery();
         getFloppyQuery.setModel(this);
 
         getFloppyQuery.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model2, Object result)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model2;
                 UserPortalItemModel userPortalItemModel =
                         (UserPortalItemModel) userPortalListModel.getSelectedItem();
                 RunOnceModel runOnceModel = (RunOnceModel) userPortalListModel.getWindow();
                 VM selectedVM = (VM) userPortalItemModel.getEntity();
                 List<String> images = (List<String>) ((VdcQueryReturnValue) result).getReturnValue();
 
                 if (DataProvider.IsWindowsOsType(selectedVM.getvm_os()))
                 {
                     // Add a pseudo floppy disk image used for Windows' sysprep.
                     if (!selectedVM.getis_initialized())
                     {
                         images.add(0, "[sysprep]"); //$NON-NLS-1$
                         runOnceModel.getAttachFloppy().setEntity(true);
                     }
                     else
                     {
                         images.add("[sysprep]"); //$NON-NLS-1$
                     }
                 }
                 runOnceModel.getFloppyImage().setItems(images);
 
                 if (runOnceModel.getFloppyImage().getIsChangable()
                         && runOnceModel.getFloppyImage().getSelectedItem() == null)
                 {
                     runOnceModel.getFloppyImage().setSelectedItem(Linq.FirstOrDefault(images));
                 }
             }
         };
 
         AsyncDataProvider.GetFloppyImageList(getFloppyQuery, vm.getstorage_pool_id());
     }
 
     private void OnRunOnce()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null || selectedItem.getEntity() == null)
         {
             Cancel();
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
 
         RunOnceModel model = (RunOnceModel) getWindow();
 
         if (!model.Validate())
         {
             return;
         }
 
         getWindow().StartProgress(null);
         BootSequenceModel bootSequenceModel = model.getBootSequence();
 
         RunVmOnceParams tempVar = new RunVmOnceParams();
         tempVar.setVmId(vm.getId());
         tempVar.setBootSequence(bootSequenceModel.getSequence());
         tempVar.setDiskPath((Boolean) model.getAttachIso().getEntity() ? (String) model.getIsoImage().getSelectedItem()
                 : ""); //$NON-NLS-1$
         tempVar.setFloppyPath(model.getFloppyImagePath());
         tempVar.setKvmEnable(model.getHwAcceleration());
         tempVar.setRunAndPause((Boolean) model.getRunAndPause().getEntity());
         tempVar.setAcpiEnable(true);
         tempVar.setRunAsStateless((Boolean) model.getRunAsStateless().getEntity());
         tempVar.setReinitialize(model.getReinitialize());
         tempVar.setCustomProperties((String) model.getCustomProperties().getEntity());
         RunVmOnceParams param = tempVar;
 
         // kernel params
         if (model.getKernel_path().getEntity() != null)
         {
             param.setkernel_url((String) model.getKernel_path().getEntity());
         }
         if (model.getKernel_parameters().getEntity() != null)
         {
             param.setkernel_params((String) model.getKernel_parameters().getEntity());
         }
         if (model.getInitrd_path().getEntity() != null)
         {
             param.setinitrd_url((String) model.getInitrd_path().getEntity());
         }
 
         // Sysprep params
         if (model.getSysPrepDomainName().getSelectedItem() != null)
         {
             param.setSysPrepDomainName((String) model.getSysPrepDomainName().getSelectedItem());
         }
         if (model.getSysPrepUserName().getEntity() != null)
         {
             param.setSysPrepUserName((String) model.getSysPrepUserName().getEntity());
         }
         if (model.getSysPrepPassword().getEntity() != null)
         {
             param.setSysPrepPassword((String) model.getSysPrepPassword().getEntity());
         }
 
         EntityModel displayProtocolSelectedItem = (EntityModel) model.getDisplayProtocol().getSelectedItem();
         param.setUseVnc((DisplayType) displayProtocolSelectedItem.getEntity() == DisplayType.vnc);
 
         Frontend.RunAction(VdcActionType.RunVmOnce, param,
                 new IFrontendActionAsyncCallback() {
                     @Override
                     public void Executed(FrontendActionAsyncResult result) {
                         stopProgress(result.getState());
                         Cancel();
                     }
                 }, this);
 
     }
 
     private void UpdateActionAvailability()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
 
         getEditCommand().setIsExecutionAllowed(selectedItem != null && !selectedItem.getIsPool());
 
         getRemoveCommand().setIsExecutionAllowed(selectedItem != null
                 && !selectedItem.getIsPool()
                 && VdcActionUtils.CanExecute(new ArrayList<VM>(Arrays.asList(new VM[] { (VM) selectedItem.getEntity() })),
                         VM.class,
                         VdcActionType.RemoveVm));
 
         getRunOnceCommand().setIsExecutionAllowed(selectedItem != null
                 && !selectedItem.getIsPool()
                 && VdcActionUtils.CanExecute(new ArrayList<VM>(Arrays.asList(new VM[] { (VM) selectedItem.getEntity() })),
                         VM.class,
                         VdcActionType.RunVmOnce));
 
         getChangeCdCommand().setIsExecutionAllowed(selectedItem != null
                 && !selectedItem.getIsPool()
                 && VdcActionUtils.CanExecute(new ArrayList<VM>(Arrays.asList(new VM[] { (VM) selectedItem.getEntity() })),
                         VM.class,
                         VdcActionType.ChangeDisk));
 
         getNewTemplateCommand().setIsExecutionAllowed(selectedItem != null
                 && !selectedItem.getIsPool()
                 && VdcActionUtils.CanExecute(new ArrayList<VM>(Arrays.asList(new VM[] { (VM) selectedItem.getEntity() })),
                         VM.class,
                         VdcActionType.AddVmTemplate));
     }
 
     private void NewDesktop()
     {
         NewInternal(VmType.Desktop);
     }
 
     private void NewServer()
     {
         NewInternal(VmType.Server);
     }
 
     private void NewInternal(VmType vmType)
     {
         UnitVmModel model = new UnitVmModel(new UserPortalNewVmModelBehavior());
         setWindow(model);
         model.setTitle(ConstantsManager.getInstance()
                 .getMessages()
                 .newVmTitle(vmType == VmType.Server ? ConstantsManager.getInstance().getConstants().serverVmType()
                         : ConstantsManager.getInstance().getConstants().desktopVmType()));
         model.setHashName(model.getTitle().toLowerCase().replace(' ', '_'));
         model.setIsNew(true);
         model.setVmType(vmType);
         model.setCustomPropertiesKeysList(CustomPropertiesKeysList);
 
         model.Initialize(null);
 
         // Ensures that the default provisioning is "Clone" for a new server and "Thin" for a new desktop.
         boolean selectValue = model.getVmType() == VmType.Server;
         model.getProvisioning().setEntity(selectValue);
 
         UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar.setIsDefault(true);
         model.getCommands().add(tempVar);
         UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar2.setIsCancel(true);
         model.getCommands().add(tempVar2);
     }
 
     private void Edit()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null)
         {
             return;
         }
 
         if (getWindow() != null)
         {
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
 
         UnitVmModel model = new UnitVmModel(new UserPortalExistingVmModelBehavior(vm));
 
         model.setTitle(ConstantsManager.getInstance()
                 .getMessages()
                 .editVmTitle(vm.getvm_type() == VmType.Server ? ConstantsManager.getInstance()
                         .getConstants()
                         .serverVmType()
                         : ConstantsManager.getInstance().getConstants().desktopVmType()));
         model.setHashName(model.getTitle().toLowerCase().replace(' ', '_'));
         model.setVmType(vm.getvm_type());
         model.setCustomPropertiesKeysList(CustomPropertiesKeysList);
 
         setWindow(model);
 
         model.Initialize(null);
 
         UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar.setIsDefault(true);
         model.getCommands().add(tempVar);
         UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar2.setIsCancel(true);
         model.getCommands().add(tempVar2);
 
     }
 
     private void remove()
     {
         if (getConfirmWindow() != null)
         {
             return;
         }
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         VM vm = (VM) selectedItem.getEntity();
 
         ConfirmationModel confirmModel = new ConfirmationModel();
         setConfirmWindow(confirmModel);
 
         confirmModel.setTitle(ConstantsManager.getInstance().getConstants().removeVirtualMachineTitle());
         confirmModel.setHashName("remove_virtual_machine"); //$NON-NLS-1$
         confirmModel.setMessage(ConstantsManager.getInstance().getConstants().virtualMachineMsg());
 
         ArrayList<String> list = new ArrayList<String>();
         list.add(vm.getvm_name());
         confirmModel.setItems(list);
 
         UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar.setIsDefault(true);
         getConfirmWindow().getCommands().add(tempVar);
         UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar2.setIsCancel(true);
         getConfirmWindow().getCommands().add(tempVar2);
     }
 
     private void OnRemove()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         VM vm = (VM) selectedItem.getEntity();
 
         getConfirmWindow().StartProgress(null);
 
         Frontend.RunAction(VdcActionType.RemoveVm, new RemoveVmParameters(vm.getId(), false),
                 new IFrontendActionAsyncCallback() {
                     @Override
                     public void Executed(FrontendActionAsyncResult result) {
                         ConfirmationModel model =
                                 (ConfirmationModel) ((UserPortalListModel) result.getState()).getConfirmWindow();
                         model.StopProgress();
                         Cancel();
                     }
                 },
                 this);
 
     }
 
     private void ChangeCD()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null || selectedItem.getEntity() == null)
         {
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
 
         AttachCdModel model = new AttachCdModel();
         setWindow(model);
         model.setTitle(ConstantsManager.getInstance().getConstants().changeCDTitle());
         model.setHashName("change_cd"); //$NON-NLS-1$
 
         ArrayList<String> defaultImages =
                 new ArrayList<String>(Arrays.asList(new String[] { "No CDs" })); //$NON-NLS-1$
         model.getIsoImage().setItems(defaultImages);
         model.getIsoImage().setSelectedItem(Linq.FirstOrDefault(defaultImages));
 
         AsyncQuery getImagesQuery = new AsyncQuery();
         getImagesQuery.setModel(this);
 
         getImagesQuery.asyncCallback = new INewAsyncCallback() {
             @Override
             public void OnSuccess(Object model1, Object result)
             {
                 UserPortalListModel userPortalListModel = (UserPortalListModel) model1;
                 AttachCdModel _attachCdModel = (AttachCdModel) userPortalListModel.getWindow();
                List<String> images = (List<String>) result;
                 if (images.size() > 0)
                 {
                     images.add(0, ConsoleModel.EjectLabel);
                     _attachCdModel.getIsoImage().setItems(images);
                 }
                 if (_attachCdModel.getIsoImage().getIsChangable())
                 {
                     _attachCdModel.getIsoImage().setSelectedItem(Linq.FirstOrDefault(images));
                 }
             }
         };
 
         AsyncDataProvider.GetIrsImageList(getImagesQuery, vm.getstorage_pool_id());
 
         UICommand tempVar = new UICommand("OnChangeCD", this); //$NON-NLS-1$
         tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
         tempVar.setIsDefault(true);
         model.getCommands().add(tempVar);
         UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
         tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
         tempVar2.setIsCancel(true);
         model.getCommands().add(tempVar2);
     }
 
     private void OnChangeCD()
     {
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (selectedItem == null || selectedItem.getEntity() == null)
         {
             Cancel();
             return;
         }
 
         VM vm = (VM) selectedItem.getEntity();
         AttachCdModel model = (AttachCdModel) getWindow();
         model.StartProgress(null);
         String isoName =
                 (StringHelper.stringsEqual(model.getIsoImage().getSelectedItem().toString(), ConsoleModel.EjectLabel)) ? "" //$NON-NLS-1$
                         : model.getIsoImage().getSelectedItem().toString();
 
         Frontend.RunAction(VdcActionType.ChangeDisk, new ChangeDiskCommandParameters(vm.getId(), isoName),
                 new IFrontendActionAsyncCallback() {
                     @Override
                     public void Executed(FrontendActionAsyncResult result) {
                         stopProgress(result.getState());
                         Cancel();
                     }
                 }, this);
     }
 
     private void OnSave()
     {
 
         final UnitVmModel model = (UnitVmModel) getWindow();
         UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
         if (!model.getIsNew() && selectedItem.getEntity() == null)
         {
             Cancel();
             return;
         }
 
         settempVm(model.getIsNew() ? new VM() : (VM) Cloner.clone(selectedItem.getEntity()));
 
         if (!model.Validate())
         {
             return;
         }
 
         model.StartProgress(null);
         // Check name uniqueness.
         AsyncDataProvider.IsVmNameUnique(new AsyncQuery(this,
                 new INewAsyncCallback() {
                     @Override
                     public void OnSuccess(Object target, Object returnValue) {
 
                         UserPortalListModel userPortalListModel = (UserPortalListModel) target;
                         stopProgress(target);
                         boolean isNameUnique = (Boolean) returnValue;
                         String newName = (String) model.getName().getEntity();
                         String currentName = userPortalListModel.gettempVm().getvm_name();
                         if (!isNameUnique && newName.compareToIgnoreCase(currentName) != 0)
                         {
                             UnitVmModel unitModel = (UnitVmModel) userPortalListModel.getWindow();
                             unitModel.getName().getInvalidityReasons().clear();
                             unitModel
                                     .getName()
                                     .getInvalidityReasons()
                                     .add(ConstantsManager.getInstance().getConstants().nameMustBeUniqueInvalidReason());
                             unitModel.getName().setIsValid(false);
                             unitModel.setIsValid(false);
                             unitModel.setIsGeneralTabValid(false);
                         }
                         else
                         {
                             userPortalListModel.PostVmNameUniqueCheck(userPortalListModel);
                         }
 
                     }
                 }),
                 (String) model.getName().getEntity());
     }
 
     private void stopProgress(Object target) {
         if (target instanceof UserPortalListModel) {
             Model window = ((UserPortalListModel) target).getWindow();
             if (window != null) {
                 window.StopProgress();
             }
         }
     }
 
     public void PostVmNameUniqueCheck(UserPortalListModel userPortalListModel)
     {
 
         UnitVmModel model = (UnitVmModel) getWindow();
         UserPortalItemModel selectedItem = (UserPortalItemModel) userPortalListModel.getSelectedItem();
         String name = (String) model.getName().getEntity();
 
         // Save changes.
         VmTemplate template = (VmTemplate) model.getTemplate().getSelectedItem();
 
         gettempVm().setvm_type(model.getVmType());
         gettempVm().setvmt_guid(template.getId());
         gettempVm().setvm_name(name);
         gettempVm().setvm_os((VmOsType) model.getOSType().getSelectedItem());
         gettempVm().setnum_of_monitors((Integer) model.getNumOfMonitors().getSelectedItem());
         gettempVm().setAllowConsoleReconnect((Boolean) model.getAllowConsoleReconnect().getEntity());
         gettempVm().setvm_description((String) model.getDescription().getEntity());
         gettempVm().setvm_domain(model.getDomain().getIsAvailable() ? (String) model.getDomain()
                 .getSelectedItem() : ""); //$NON-NLS-1$
         gettempVm().setvm_mem_size_mb((Integer) model.getMemSize().getEntity());
         gettempVm().setMinAllocatedMem((Integer) model.getMinAllocatedMemory().getEntity());
         Guid newClusterID = ((VDSGroup) model.getCluster().getSelectedItem()).getId();
         gettempVm().setvds_group_id(newClusterID);
         gettempVm().settime_zone((model.getTimeZone().getIsAvailable() && model.getTimeZone()
                 .getSelectedItem() != null) ? ((Map.Entry<String, String>) model.getTimeZone()
                 .getSelectedItem()).getKey() : ""); //$NON-NLS-1$
         gettempVm().setnum_of_sockets((Integer) model.getNumOfSockets().getEntity());
         gettempVm().setcpu_per_socket((Integer) model.getTotalCPUCores().getEntity()
                 / (Integer) model.getNumOfSockets().getEntity());
         gettempVm().setusb_policy((UsbPolicy) model.getUsbPolicy().getSelectedItem());
         gettempVm().setis_auto_suspend(false);
         gettempVm().setis_stateless((Boolean) model.getIsStateless().getEntity());
         gettempVm().setdefault_boot_sequence(model.getBootSequence());
         gettempVm().setiso_path(model.getCdImage().getIsChangable() ? (String) model.getCdImage()
                 .getSelectedItem() : ""); //$NON-NLS-1$
         gettempVm().setauto_startup((Boolean) model.getIsHighlyAvailable().getEntity());
 
         gettempVm().setinitrd_url((String) model.getInitrd_path().getEntity());
         gettempVm().setkernel_url((String) model.getKernel_path().getEntity());
         gettempVm().setkernel_params((String) model.getKernel_parameters().getEntity());
 
         gettempVm().setCustomProperties((String) model.getCustomProperties().getEntity());
 
         EntityModel displayProtocolSelectedItem = (EntityModel) model.getDisplayProtocol().getSelectedItem();
         gettempVm().setdefault_display_type((DisplayType) displayProtocolSelectedItem.getEntity());
 
         EntityModel prioritySelectedItem = (EntityModel) model.getPriority().getSelectedItem();
         gettempVm().setpriority((Integer) prioritySelectedItem.getEntity());
 
         VDS defaultHost = (VDS) model.getDefaultHost().getSelectedItem();
         if ((Boolean) model.getIsAutoAssign().getEntity())
         {
             gettempVm().setdedicated_vm_for_vds(null);
         }
         else
         {
             gettempVm().setdedicated_vm_for_vds(defaultHost.getId());
         }
 
         gettempVm().setMigrationSupport(MigrationSupport.MIGRATABLE);
         if ((Boolean) model.getRunVMOnSpecificHost().getEntity())
         {
             gettempVm().setMigrationSupport(MigrationSupport.PINNED_TO_HOST);
         }
         else if ((Boolean) model.getDontMigrateVM().getEntity())
         {
             gettempVm().setMigrationSupport(MigrationSupport.IMPLICITLY_NON_MIGRATABLE);
         }
 
         boolean cancel = true;
         if (model.getIsNew())
         {
             if (gettempVm().getvmt_guid().equals(NGuid.Empty))
             {
                 AddVmFromScratchParameters parameters =
                         new AddVmFromScratchParameters(gettempVm(),
                                 new ArrayList<DiskImage>(),
                                 NGuid.Empty);
                 parameters.setMakeCreatorExplicitOwner(true);
 
                 Frontend.RunAction(VdcActionType.AddVmFromScratch, parameters,
                         new IFrontendActionAsyncCallback() {
                             @Override
                             public void Executed(FrontendActionAsyncResult result) {
                                 stopProgress(result.getState());
                             }
                         }, this);
             }
             else
             {
                 setstorageDomain((storage_domains) model.getStorageDomain().getSelectedItem());
 
                 if ((Boolean) model.getProvisioning().getEntity())
                 {
                     AsyncQuery _asyncQuery = new AsyncQuery();
                     _asyncQuery.setModel(this);
                     _asyncQuery.asyncCallback = new INewAsyncCallback() {
                         @Override
                         public void OnSuccess(Object model, Object result)
                         {
                             UserPortalListModel userPortalListModel1 = (UserPortalListModel) model;
                             ArrayList<DiskImage> templateDisks = (ArrayList<DiskImage>) result;
                             for (DiskImage templateDisk : templateDisks)
                             {
                                 DiskModel disk = null;
                                 for (DiskModel a : ((UnitVmModel) userPortalListModel1.getWindow()).getDisks())
                                 {
                                     if (templateDisk.getId().equals(a.getDisk().getId()))
                                     {
                                         disk = a;
                                         break;
                                     }
                                 }
 
                                 if (disk != null) {
                                     templateDisk.setvolume_type((VolumeType) disk.getVolumeType().getSelectedItem());
                                     templateDisk.setvolume_format(DataProvider.GetDiskVolumeFormat((VolumeType) disk.getVolumeType()
                                             .getSelectedItem(),
                                             getstorageDomain().getstorage_type()));
                                 }
                             }
 
                             HashMap<Guid, DiskImage> dict =
                                     new HashMap<Guid, DiskImage>();
                             for (DiskImage a : templateDisks)
                             {
                                 dict.put(a.getImageId(), a);
                             }
 
                             AddVmFromTemplateParameters parameters =
                                     new AddVmFromTemplateParameters(gettempVm(), dict, getstorageDomain().getId());
                             parameters.setMakeCreatorExplicitOwner(true);
 
                             Frontend.RunAction(VdcActionType.AddVmFromTemplate, parameters,
                                     new IFrontendActionAsyncCallback() {
                                         @Override
                                         public void Executed(FrontendActionAsyncResult a) {
                                             stopProgress(a.getState());
                                         }
                                     }, this);
 
                             userPortalListModel1.Cancel();
 
                         }
                     };
                     AsyncDataProvider.GetTemplateDiskList(_asyncQuery, template.getId());
                     cancel = false;
                 }
                 else
                 {
                     VmManagementParametersBase parameters = new VmManagementParametersBase(gettempVm());
                     parameters.setStorageDomainId(getstorageDomain().getId());
                     parameters.setMakeCreatorExplicitOwner(true);
 
                     Frontend.RunAction(VdcActionType.AddVm, parameters,
                             new IFrontendActionAsyncCallback() {
                                 @Override
                                 public void Executed(FrontendActionAsyncResult result) {
                                     stopProgress(result.getState());
                                 }
                             }, this);
                 }
             }
         }
         else
         {
             Guid oldClusterID = ((VM) selectedItem.getEntity()).getvds_group_id();
             if (oldClusterID.equals(newClusterID) == false)
             {
                 Frontend.RunAction(VdcActionType.ChangeVMCluster, new ChangeVMClusterParameters(newClusterID,
                         gettempVm().getId()),
                         new IFrontendActionAsyncCallback() {
                             @Override
                             public void Executed(FrontendActionAsyncResult result) {
 
                                 Frontend.RunAction(VdcActionType.UpdateVm, new VmManagementParametersBase(gettempVm()),
                                         new IFrontendActionAsyncCallback() {
                                             @Override
                                             public void Executed(FrontendActionAsyncResult a) {
                                                 stopProgress(a.getState());
                                             }
                                         }, this);
 
                             }
                         }, this);
             }
             else
             {
                 Frontend.RunAction(VdcActionType.UpdateVm, new VmManagementParametersBase(gettempVm()),
                         new IFrontendActionAsyncCallback() {
                             @Override
                             public void Executed(FrontendActionAsyncResult a) {
                                 stopProgress(a.getState());
                             }
                         }, this);
             }
         }
 
         if (cancel)
         {
             Cancel();
         }
     }
 
     private void Cancel()
     {
         Frontend.Unsubscribe();
         setWindow(null);
         setConfirmWindow(null);
     }
 
     private void VmModel_DataCenter_ItemsChanged()
     {
         UnitVmModel model = (UnitVmModel) getWindow();
         storage_pool dataCenter = null;
         for (Object item : model.getDataCenter().getItems())
         {
             storage_pool a = (storage_pool) item;
 
             if (model.getIsNew())
             {
                 dataCenter = a;
                 break;
             }
             else
             {
                 UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
                 VM vm = (VM) selectedItem.getEntity();
 
                 if (a.getId().equals(vm.getstorage_pool_id()))
                 {
                     dataCenter = a;
                     break;
                 }
             }
         }
 
         if (!model.getIsNew() && dataCenter == null)
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
             AsyncQuery _asyncQuery = new AsyncQuery();
             _asyncQuery.setModel(this);
             _asyncQuery.asyncCallback = new INewAsyncCallback() {
                 @Override
                 public void OnSuccess(Object model, Object result)
                 {
                     UserPortalListModel userPortalListModel = (UserPortalListModel) model;
                     ArrayList<storage_pool> list =
                             new ArrayList<storage_pool>(Arrays.asList(new storage_pool[] { (storage_pool) result }));
                     UnitVmModel unitModel = (UnitVmModel) userPortalListModel.getWindow();
                     unitModel.getDataCenter().setItems(list);
                     unitModel.getDataCenter().setSelectedItem(Linq.FirstOrDefault(list));
 
                 }
             };
             AsyncDataProvider.GetDataCenterById(_asyncQuery, vm.getstorage_pool_id());
         }
         else
         {
             model.getDataCenter().setSelectedItem(dataCenter);
         }
     }
 
     private void VmModel_Cluster_ItemsChanged()
     {
 
         UnitVmModel model = (UnitVmModel) getWindow();
         if (!model.getIsNew())
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
             VDSGroup cluster = null;
 
             for (Object item : model.getCluster().getItems())
             {
                 VDSGroup a = (VDSGroup) item;
                 if (a.getId().equals(vm.getvds_group_id()))
                 {
                     cluster = a;
                     break;
                 }
             }
             model.getCluster().setSelectedItem(cluster);
 
             model.getCluster().setIsChangable(vm.getstatus() == VMStatus.Down);
         }
     }
 
     private void VmModel_DefaultHost_ItemsChanged()
     {
         UnitVmModel model = (UnitVmModel) getWindow();
         if (!model.getIsNew())
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
             VDS host = null;
 
             for (Object item : model.getDefaultHost().getItems())
             {
                 VDS a = (VDS) item;
                 if (a.getId().equals(((vm.getdedicated_vm_for_vds() != null) ? vm.getdedicated_vm_for_vds()
                         : NGuid.Empty)))
                 {
                     host = a;
                     break;
                 }
             }
             if (host == null)
             {
                 model.getIsAutoAssign().setEntity(true);
             }
             else
             {
                 model.getDefaultHost().setSelectedItem(host);
                 model.getIsAutoAssign().setEntity(false);
             }
         }
     }
 
     private void VmModel_DisplayProtocol_ItemsChanged()
     {
         UnitVmModel model = (UnitVmModel) getWindow();
         if (!model.getIsNew())
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
             EntityModel displayType = null;
 
             for (Object item : model.getDisplayProtocol().getItems())
             {
                 EntityModel a = (EntityModel) item;
                 DisplayType dt = (DisplayType) a.getEntity();
                 if (dt == vm.getdefault_display_type())
                 {
                     displayType = a;
                     break;
                 }
             }
             model.getDisplayProtocol().setSelectedItem(displayType);
         }
     }
 
     private void VmModel_Priority_ItemsChanged()
     {
         UnitVmModel model = (UnitVmModel) getWindow();
         if (!model.getIsNew())
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
             AsyncQuery _asyncQuery = new AsyncQuery();
             _asyncQuery.setModel(this);
             _asyncQuery.asyncCallback = new INewAsyncCallback() {
                 @Override
                 public void OnSuccess(Object model, Object result)
                 {
                     UserPortalListModel userPortalListModel = (UserPortalListModel) model;
                     int roundPriority = (Integer) result;
                     EntityModel priority = null;
 
                     for (Object item : ((UnitVmModel) userPortalListModel.getWindow()).getPriority().getItems())
                     {
                         EntityModel a = (EntityModel) item;
                         int p = (Integer) a.getEntity();
                         if (p == roundPriority)
                         {
                             priority = a;
                             break;
                         }
                     }
                     ((UnitVmModel) userPortalListModel.getWindow()).getPriority().setSelectedItem(priority);
 
                 }
             };
             AsyncDataProvider.GetRoundedPriority(_asyncQuery, vm.getpriority());
         }
     }
 
     private void VmModel_TimeZone_ItemsChanged()
     {
         UnitVmModel model = (UnitVmModel) getWindow();
         if (!model.getIsNew())
         {
             UserPortalItemModel selectedItem = (UserPortalItemModel) getSelectedItem();
             VM vm = (VM) selectedItem.getEntity();
 
             if (!StringHelper.isNullOrEmpty(vm.gettime_zone()))
             {
                 model.getTimeZone().setSelectedItem(Linq.FirstOrDefault(model.getTimeZone().getItems(),
                         new Linq.TimeZonePredicate(vm.gettime_zone())));
             }
         }
     }
 
     @Override
     public void eventRaised(Event ev, Object sender, EventArgs args)
     {
         super.eventRaised(ev, sender, args);
 
         UnitVmModel model = (UnitVmModel) getWindow();
         if (ev.equals(ItemsChangedEventDefinition) && sender == model.getDataCenter())
         {
             VmModel_DataCenter_ItemsChanged();
         }
         else if (ev.equals(ItemsChangedEventDefinition) && sender == model.getCluster())
         {
             VmModel_Cluster_ItemsChanged();
         }
         else if (ev.equals(ItemsChangedEventDefinition) && sender == model.getDefaultHost())
         {
             VmModel_DefaultHost_ItemsChanged();
         }
         else if (ev.equals(ItemsChangedEventDefinition) && sender == model.getDisplayProtocol())
         {
             VmModel_DisplayProtocol_ItemsChanged();
         }
         else if (ev.equals(ItemsChangedEventDefinition) && sender == model.getPriority())
         {
             VmModel_Priority_ItemsChanged();
         }
         else if (ev.equals(ItemsChangedEventDefinition) && sender == model.getTimeZone())
         {
             VmModel_TimeZone_ItemsChanged();
         }
     }
 
     @Override
     public void OnVmAndPoolLoad()
     {
         if (getvms() != null && getpools() != null)
         {
             // Complete search.
 
             // Remove pools that has provided VMs.
             ArrayList<vm_pools> filteredPools = new ArrayList<vm_pools>();
             poolMap = new HashMap<Guid, vm_pools>();
 
             for (vm_pools pool : getpools())
             {
                 // Add pool to map.
                 poolMap.put(pool.getvm_pool_id(), pool);
 
                 boolean found = false;
                 for (VM vm : getvms())
                 {
                     if (vm.getVmPoolId() != null && vm.getVmPoolId().equals(pool.getvm_pool_id()))
                     {
                         found = true;
                         break;
                     }
                 }
 
                 if (!found)
                 {
                     filteredPools.add(pool);
                 }
             }
 
             // Merge VMs and Pools, and create item models.
             List all = Linq.Concat(getvms(), filteredPools);
             Linq.Sort(all, new Linq.VmAndPoolByNameComparer());
 
             ArrayList<Model> items = new ArrayList<Model>();
             for (Object item : all)
             {
                 UserPortalItemModel model = new UserPortalItemModel(this);
                 model.setEntity(item);
                 items.add(model);
 
                 UpdateConsoleModel(model);
             }
 
             // In userportal 'Extended View': Set 'CanConnectAutomatically' to true if there's one and only one up VM.
             setCanConnectAutomatically(GetUpVms(items).size() == 1
                     && GetUpVms(items).get(0).getDefaultConsole().getConnectCommand().getIsExecutionAllowed());
 
             setItems(items);
 
             setvms(null);
             setpools(null);
 
             getSearchCompletedEvent().raise(this, EventArgs.Empty);
         }
     }
 
     private void UpdateConsoleModel(UserPortalItemModel item)
     {
         if (item.getEntity() != null)
         {
             Object tempVar = item.getEntity();
             VM vm = (VM) ((tempVar instanceof VM) ? tempVar : null);
             if (vm == null)
             {
                 return;
             }
 
             // Caching console model if needed
             if (!cachedConsoleModels.containsKey(vm.getId()))
             {
                 SpiceConsoleModel spiceConsoleModel = new SpiceConsoleModel();
                 spiceConsoleModel.getErrorEvent().addListener(this);
                 VncConsoleModel vncConsoleModel = new VncConsoleModel();
                 vncConsoleModel.setModel(this);
                 RdpConsoleModel rdpConsoleModel = new RdpConsoleModel();
 
                 cachedConsoleModels.put(vm.getId(),
                         new ArrayList<ConsoleModel>(Arrays.asList(new ConsoleModel[] {
                                 spiceConsoleModel, vncConsoleModel, rdpConsoleModel })));
             }
 
             // Getting cached console model
             ArrayList<ConsoleModel> cachedModels = cachedConsoleModels.get(vm.getId());
             for (ConsoleModel cachedModel : cachedModels)
             {
                 cachedModel.setEntity(null);
                 cachedModel.setEntity(vm);
             }
 
             // Set default console by vm's display type
             item.setDefaultConsole(vm.getdisplay_type() == DisplayType.vnc ? cachedModels.get(1) : cachedModels.get(0));
 
             // Adjust item's default console for userportal 'Extended View'
             item.getDefaultConsole().setForceVmStatusUp(false);
 
             // Update additional console
             if (DataProvider.IsWindowsOsType(vm.getvm_os()))
             {
                 item.setAdditionalConsole(cachedModels.get(2));
                 item.setHasAdditionalConsole(true);
             }
             else
             {
                 item.setAdditionalConsole(null);
                 item.setHasAdditionalConsole(false);
             }
         }
     }
 
     @Override
     protected String getListName() {
         return "UserPortalListModel"; //$NON-NLS-1$
     }
 }
