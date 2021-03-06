 package org.ovirt.engine.core.bll;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.ovirt.engine.core.bll.command.utils.StorageDomainSpaceChecker;
 import org.ovirt.engine.core.bll.job.ExecutionHandler;
 import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
 import org.ovirt.engine.core.common.AuditLogType;
 import org.ovirt.engine.core.common.PermissionSubject;
 import org.ovirt.engine.core.common.VdcObjectType;
 import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
 import org.ovirt.engine.core.common.action.VdcActionType;
 import org.ovirt.engine.core.common.action.VdcReturnValueBase;
 import org.ovirt.engine.core.common.businessentities.ActionGroup;
 import org.ovirt.engine.core.common.businessentities.CopyVolumeType;
 import org.ovirt.engine.core.common.businessentities.DiskImage;
 import org.ovirt.engine.core.common.businessentities.ImageOperation;
 import org.ovirt.engine.core.common.businessentities.ImageStatus;
 import org.ovirt.engine.core.common.businessentities.VM;
 import org.ovirt.engine.core.common.businessentities.VMStatus;
 import org.ovirt.engine.core.common.businessentities.VmDevice;
 import org.ovirt.engine.core.common.businessentities.VmDeviceId;
 import org.ovirt.engine.core.common.businessentities.VmEntityType;
 import org.ovirt.engine.core.common.businessentities.VmTemplate;
 import org.ovirt.engine.core.common.locks.LockingGroup;
 import org.ovirt.engine.core.compat.Guid;
 import org.ovirt.engine.core.compat.NGuid;
 import org.ovirt.engine.core.dal.VdcBllMessages;
 import org.ovirt.engine.core.dal.dbbroker.DbFacade;
 import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
 import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;
 import org.ovirt.engine.core.dao.VmDeviceDAO;
 
 @CustomLogFields({ @CustomLogField("DiskAlias") })
 @NonTransactiveCommandAttribute
 public class MoveOrCopyDiskCommand<T extends MoveOrCopyImageGroupParameters> extends MoveOrCopyImageGroupCommand<T> {
 
     private static final long serialVersionUID = -7219975636530710384L;
     private Map<Guid, String> sharedLockMap;
     private List<PermissionSubject> permsList = null;
     private List<VM> listVms;
 
     public MoveOrCopyDiskCommand(T parameters) {
         super(parameters);
         setQuotaId(getParameters().getQuotaId());
     }
 
     @Override
     protected void setActionMessageParameters() {
         if (getParameters().getOperation() == ImageOperation.Copy) {
             addCanDoActionMessage(VdcBllMessages.VAR__ACTION__COPY);
         } else {
             addCanDoActionMessage(VdcBllMessages.VAR__ACTION__MOVE);
         }
         addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_DISK);
     }
 
     @Override
     protected boolean canDoAction() {
         ArrayList<String> canDoActionMessages = getReturnValue().getCanDoActionMessages();
         return isImageExist()
                 && checkOperationIsCorrect()
                 && canFindVmOrTemplate()
                 && acquireLockInternal()
                 && isImageIsNotLocked()
                 && isSourceAndDestTheSame()
                 && validateSourceStorageDomain(canDoActionMessages)
                 && validateDestStorage(canDoActionMessages)
                 && checkTemplateInDestStorageDomain()
                 && validateSpaceRequirements()
                 && checkImageConfiguration(canDoActionMessages)
                 && checkCanBeMoveInVm() && checkIfNeedToBeOverride();
     }
 
     protected boolean isSourceAndDestTheSame() {
         if (getParameters().getOperation() == ImageOperation.Move
                 && getParameters().getSourceDomainId().equals(getParameters().getStorageDomainId())) {
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_SOURCE_AND_TARGET_SAME);
             return false;
         }
         return true;
     }
 
     protected boolean isImageExist() {
         if (getImage() == null) {
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_NOT_EXIST);
             return false;
         }
         return true;
     }
 
     protected boolean isImageIsNotLocked() {
         if (getImage().getimageStatus() == ImageStatus.LOCKED) {
             if (getParameters().getOperation() == ImageOperation.Move) {
                 addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_IMAGE_IS_LOCKED);
             } else {
                 addCanDoActionMessage(VdcBllMessages.VM_TEMPLATE_IMAGE_IS_LOCKED);
             }
             return false;
         }
         return true;
     }
 
     @Override
     protected boolean validateQuota() {
         // Set default quota id if storage pool enforcement is disabled.
         getParameters().setQuotaId(QuotaHelper.getInstance().getQuotaIdToConsume(getParameters().getQuotaId(),
                 getStoragePool()));
 
         return (QuotaManager.validateStorageQuota(getStorageDomainId().getValue(),
                 getParameters().getQuotaId(),
                 getStoragePool().getQuotaEnforcementType(),
                 getImage().getActualDiskWithSnapshotsSize(),
                 getCommandId(),
                 getReturnValue().getCanDoActionMessages()));
     }
 
     /**
      * The following method will perform a check for correctness of operation
      * It is allow to copy only if it is image that belongs to template and
      * it is allow to move only if it is image of disk
      * @return
      */
     protected boolean checkOperationIsCorrect() {
         boolean retValue = true;
         if (getParameters().getOperation() == ImageOperation.Copy
                 && getImage().getVmEntityType() != VmEntityType.TEMPLATE) {
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_IS_NOT_TEMPLATE_DISK);
             retValue = false;
         }
 
         if (retValue && getParameters().getOperation() == ImageOperation.Move
                 && getImage().getVmEntityType() == VmEntityType.TEMPLATE) {
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_IS_NOT_VM_DISK);
             retValue = false;
         }
         return retValue;
     }
 
     protected boolean validateDestStorage(ArrayList<String> canDoActionMessages) {
         StorageDomainValidator validator = new StorageDomainValidator(getStorageDomain());
         return validator.isDomainExistAndActive(canDoActionMessages)
                 && validator.domainIsValidDestination(canDoActionMessages);
     }
 
     /**
      * Check if destination storage has enough space
      * @return
      */
     protected boolean validateSpaceRequirements() {
         boolean retValue = true;
         if (!isStorageDomainSpaceBelowThresholds()) {
             retValue = false;
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW);
         }
 
         if (retValue) {
             getImage().getSnapshots().addAll(getAllImageSnapshots());
             if (!doesStorageDomainHaveSpaceForRequest(Math.round(getImage().getActualDiskWithSnapshotsSize()))) {
                 addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW);
                 retValue = false;
             }
         }
         return retValue;
     }
 
     protected boolean isStorageDomainSpaceBelowThresholds() {
         return StorageDomainSpaceChecker.isBelowThresholds(getStorageDomain());
     }
 
     protected List<DiskImage> getAllImageSnapshots() {
         return ImagesHandler.getAllImageSnapshots(getImage().getImageId(), getImage().getit_guid());
     }
 
     protected boolean doesStorageDomainHaveSpaceForRequest(long size) {
         return StorageDomainSpaceChecker.hasSpaceForRequest(getStorageDomain(), size);
     }
 
     protected boolean checkIfNeedToBeOverride() {
         if (getParameters().getOperation() == ImageOperation.Copy && !getParameters().getForceOverride()
                 && getImage().getstorage_ids().contains(getStorageDomain().getId())) {
             addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_IMAGE_ALREADY_EXISTS);
             return false;
         }
         return true;
     }
 
     /**
      * Validate a source storage domain of image, when a source storage domain is not provided
      * any of the domains image will be used
      * @param canDoActionMessages
      * @return
      */
     protected boolean validateSourceStorageDomain(ArrayList<String> canDoActionMessages) {
         NGuid sourceDomainId = getParameters().getSourceDomainId();
         if (sourceDomainId == null || Guid.Empty.equals(sourceDomainId)) {
             sourceDomainId = getImage().getstorage_ids().get(0);
             getParameters().setSourceDomainId(sourceDomainId);
         }
         StorageDomainValidator validator =
                 new StorageDomainValidator(getStorageDomainDAO().getForStoragePool(sourceDomainId.getValue(),
                         getImage().getstorage_pool_id()));
         return validator.isDomainExistAndActive(canDoActionMessages);
     }
 
     protected boolean checkImageConfiguration(List<String> canDoActionMessages) {
         return ImagesHandler.CheckImageConfiguration(getStorageDomain().getStorageStaticData(),
                 getImage(),
                 canDoActionMessages);
     }
 
     /**
      * If a disk is attached to VM it can be moved when it is unplugged or at case that disk is plugged
      * vm should be down
      * @return
      */
     protected boolean checkCanBeMoveInVm() {
         List<VM> listVms = getVmsForDiskId();
         int vmCount = 0;
         boolean canMoveDisk = true;
         while (listVms.size() > vmCount && canMoveDisk) {
             VM vm = listVms.get(vmCount++);
             if (VMStatus.Down != vm.getstatus()) {
                 VmDevice vmDevice =
                         getVmDeviceDAO().get(new VmDeviceId(getImage().getId(), vm.getId()));
                 if (vmDevice.getIsPlugged()) {
                     addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_NOT_DOWN);
                     canMoveDisk = false;
                 }
             }
         }
         return canMoveDisk;
     }
 
     /**
      * Cache method to retrieve all the VMs related to image
      * @return List of Vms.
      */
     private List<VM> getVmsForDiskId() {
         if (listVms == null) {
             listVms = getVmDAO().getVmsListForDisk(getImage().getId());
         }
         return listVms;
     }
 
     /**
      * The following method will check, if we can move disk to destination storage domain, when
      * it is based on template
      * @return
      */
     protected boolean checkTemplateInDestStorageDomain() {
         boolean retValue = true;
         if (getParameters().getOperation() == ImageOperation.Move
                 && !ImagesHandler.BlankImageTemplateId.equals(getImage().getit_guid())) {
             DiskImage templateImage = getDiskImageDao().get(getImage().getit_guid());
             if (!templateImage.getstorage_ids().contains(getParameters().getStorageDomainId())) {
                 retValue = false;
                 addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
             }
         }
         return retValue;
     }
 
     protected VmDeviceDAO getVmDeviceDAO() {
         return DbFacade.getInstance().getVmDeviceDAO();
     }
 
     @Override
     protected void executeCommand() {
         overrideParameters();
         VdcReturnValueBase vdcRetValue = Backend.getInstance().runInternalAction(
                 VdcActionType.MoveOrCopyImageGroup,
                 getParameters(),
                 ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
         if (!vdcRetValue.getSucceeded()) {
             setSucceeded(false);
             getReturnValue().setFault(vdcRetValue.getFault());
         } else {
             getReturnValue().getTaskIdList().addAll(vdcRetValue.getInternalTaskIdList());
         }
         setSucceeded(true);
     }
 
     @Override
     public AuditLogType getAuditLogTypeValue() {
         switch (getActionState()) {
         case EXECUTE:
             return getSucceeded() ? (getParameters().getOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_VM_DISK
                     : AuditLogType.USER_COPIED_TEMPLATE_DISK
                     : (getParameters().getOperation() == ImageOperation.Move) ? AuditLogType.USER_FAILED_MOVED_VM_DISK
                             : AuditLogType.USER_FAILED_COPY_TEMPLATE_DISK;
 
         case END_SUCCESS:
             return getSucceeded() ? (getParameters().getOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_VM_DISK_FINISHED_SUCCESS
                     : AuditLogType.USER_COPIED_TEMPLATE_DISK_FINISHED_SUCCESS
                     : (getParameters().getOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_VM_DISK_FINISHED_FAILURE
                             : AuditLogType.USER_COPIED_TEMPLATE_DISK_FINISHED_FAILURE;
 
         default:
             return (getParameters().getOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_VM_DISK_FINISHED_FAILURE
                     : AuditLogType.USER_COPIED_TEMPLATE_DISK_FINISHED_FAILURE;
         }
     }
 
     @Override
     protected void removeQuotaCommandLeftOver() {
         QuotaManager.removeStorageDeltaQuotaCommand(getQuotaId(),
                 getStorageDomainId().getValue(),
                 getStoragePool().getQuotaEnforcementType(),
                 getCommandId());
     }
 
     @Override
     public List<PermissionSubject> getPermissionCheckSubjects() {
         if (permsList == null) {
             permsList = new ArrayList<PermissionSubject>();
 
             DiskImage image = getImage();
            Guid diskId = image == null ? Guid.Empty : image.getimage_group_id();
             permsList.add(new PermissionSubject(diskId, VdcObjectType.Disk, ActionGroup.CONFIGURE_DISK_STORAGE));
 
             addStoragePermissionByQuotaMode(permsList,
                     getParameters().getStoragePoolId(),
                     getParameters().getStorageDomainId());
 
             permsList = QuotaHelper.getInstance().addQuotaPermissionSubject(permsList, getStoragePool(), getQuotaId());
         }
         return permsList;
     }
 
     /**
      * The following method will override a parameters which are not relevant for the MoveOrCopyDiskCommand to the
      * correct values for these scenario in order to be used at parent class
      */
     private void overrideParameters() {
         if (getParameters().getOperation() == ImageOperation.Copy) {
             getParameters().setUseCopyCollapse(true);
             getParameters().setAddImageDomainMapping(true);
         } else {
             getParameters().setUseCopyCollapse(false);
         }
         getParameters().setDestinationImageId(getImageId());
         getParameters().setImageGroupID(getImageGroupId());
         getParameters().setDestImageGroupId(getImageGroupId());
         getParameters().setVolumeFormat(getDiskImage().getvolume_format());
         getParameters().setVolumeType(getDiskImage().getvolume_type());
         getParameters().setCopyVolumeType(CopyVolumeType.SharedVol);
         getParameters().setParentCommand(getActionType());
         getParameters().setParentParemeters(getParameters());
     }
 
     /**
      * The following method will determine if a provided vm/template exists
      * @param retValue
      * @return
      */
     private boolean canFindVmOrTemplate() {
         boolean retValue = true;
         if (getParameters().getOperation() == ImageOperation.Copy) {
             VmTemplate template = getVmTemplateDAO().get(getImage().getvm_guid());
             if (template == null) {
                 addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
                 retValue = false;
             } else {
                 setVmTemplate(template);
                 sharedLockMap = Collections.singletonMap(getImage().getvm_guid(), LockingGroup.TEMPLATE.name());
             }
         } else {
             List<VM> listVms = getVmsForDiskId();
             if (!listVms.isEmpty()) {
                 sharedLockMap = new HashMap<Guid, String>();
                 for (VM vm : listVms) {
                     sharedLockMap.put(vm.getId(), LockingGroup.VM.name());
                 }
             }
         }
         return retValue;
     }
 
     @Override
     protected Map<Guid, String> getExclusiveLocks() {
        return Collections.singletonMap(getParameters().getImageId(), LockingGroup.DISK.name());
     }
 
     @Override
     protected Map<Guid, String> getSharedLocks() {
         return sharedLockMap;
     }
 
     public String getDiskAlias() {
         return getImage().getDiskAlias();
     }
 }
