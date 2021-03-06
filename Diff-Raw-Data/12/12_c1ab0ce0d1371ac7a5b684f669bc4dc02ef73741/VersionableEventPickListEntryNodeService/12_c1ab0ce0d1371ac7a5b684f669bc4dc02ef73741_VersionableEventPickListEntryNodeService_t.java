 package org.lexevs.dao.database.service.valuesets;
 
 import org.LexGrid.LexBIG.Exceptions.LBException;
 import org.LexGrid.LexBIG.Exceptions.LBParameterException;
 import org.LexGrid.LexBIG.Exceptions.LBRevisionException;
 import org.LexGrid.commonTypes.Property;
 import org.LexGrid.valueSets.PickListEntryNode;
 import org.LexGrid.versions.EntryState;
 import org.LexGrid.versions.types.ChangeType;
 import org.lexevs.dao.database.access.valuesets.PickListEntryNodeDao;
 import org.lexevs.dao.database.access.valuesets.VSPropertyDao.ReferenceType;
 import org.lexevs.dao.database.service.AbstractDatabaseService;
 
 public class VersionableEventPickListEntryNodeService extends
 		AbstractDatabaseService implements PickListEntryNodeService {
 
 	private VSPropertyService vsPropertyService = null;
 
 	@Override
 	public void insertPickListEntryNode(String pickListId,
 			PickListEntryNode pickListEntryNode) {
 
 		String pickListDefUId = this.getDaoManager()
 				.getCurrentPickListDefinitionDao()
 				.getPickListGuidFromPickListId(pickListId);
 
 		this.getDaoManager().getCurrentPickListEntryNodeDao()
 				.insertPickListEntry(pickListDefUId, pickListEntryNode);
 	}
 
 	@Override
 	public void removePickListEntryNode(String pickListId,
 			PickListEntryNode pickListEntryNode) {
 
 		PickListEntryNodeDao pickListEntryNodeDao = this.getDaoManager()
 				.getCurrentPickListEntryNodeDao();
 
 		String pickListEntryNodeUId = pickListEntryNodeDao
 				.getPickListEntryNodeUId(pickListId, pickListEntryNode
 						.getPickListEntryId());
 
 		/* 1. Delete all entry state details of PL entry. */
 		this.getDaoManager().getCurrentVsEntryStateDao()
 				.deleteAllEntryStatesOfPLEntryNodeByUId(pickListEntryNodeUId);
 
 		/* 2. Delete all pick list entry node properties. */
 		this.getDaoManager().getCurrentVsPropertyDao()
 				.deleteAllPickListEntryNodeProperties(pickListEntryNodeUId);
 
 		/* 3. Delete all PL entry multi attributes. */
 		pickListEntryNodeDao
 				.removeAllPickListEntryNodeMultiAttributes(pickListEntryNodeUId);
 
 		/* 4. Delete PL entry node. */
 		pickListEntryNodeDao.deletePLEntryNodeByUId(pickListEntryNodeUId);
 	}
 
 	@Override
 	public void updatePickListEntryNode(String pickListId,
 			PickListEntryNode pickListEntryNode) throws LBException {
 
 		String pickListEntryId = pickListEntryNode.getPickListEntryId();
 
 		PickListEntryNodeDao pickListEntryNodeDao = this.getDaoManager()
 				.getCurrentPickListEntryNodeDao();
 
 		String pickListEntryNodeUId = pickListEntryNodeDao
 				.getPickListEntryNodeUId(pickListId, pickListEntryId);
 
 		String prevEntryStateUId = pickListEntryNodeDao
 				.insertHistoryPickListEntryNode(pickListEntryNodeUId);
 
 		String entryStateUId = pickListEntryNodeDao
 				.updatePickListEntryNode(pickListEntryNodeUId,
 						pickListEntryNode);
 
 		this.getDaoManager().getCurrentVsEntryStateDao().insertEntryState(
 				entryStateUId, pickListEntryNodeUId,
 				ReferenceType.PICKLISTENTRY.name(), prevEntryStateUId,
 				pickListEntryNode.getEntryState());
 
 		this.insertDependentChanges(pickListId, pickListEntryNode);
 	}
 
 	@Override
 	public void updateVersionableAttributes(String pickListId,
 			PickListEntryNode pickListEntryNode) throws LBException {
 
 		String pickListEntryId = pickListEntryNode.getPickListEntryId();
 
 		PickListEntryNodeDao pickListEntryNodeDao = this.getDaoManager()
 				.getCurrentPickListEntryNodeDao();
 
 		String pickListEntryNodeUId = pickListEntryNodeDao
 				.getPickListEntryNodeUId(pickListId, pickListEntryId);
 
 		String prevEntryStateUId = pickListEntryNodeDao
 				.insertHistoryPickListEntryNode(pickListEntryNodeUId);
 
 		String entryStateUId = pickListEntryNodeDao
 				.updateVersionableAttributes(pickListEntryNodeUId,
 						pickListEntryNode);
 
 		this.getDaoManager().getCurrentVsEntryStateDao().insertEntryState(
 				entryStateUId, pickListEntryNodeUId,
 				ReferenceType.PICKLISTENTRY.name(), prevEntryStateUId,
 				pickListEntryNode.getEntryState());
 
 		this.insertDependentChanges(pickListId, pickListEntryNode);
 	}
 
 	@Override
 	public void insertDependentChanges(String pickListId,
 			PickListEntryNode pickListEntryNode) throws LBException {
 
 		String pickListEntryId = pickListEntryNode.getPickListEntryId();
 
 		/* 1. Insert EntryState entry. */
 		
 		if (pickListEntryNode.getEntryState().getChangeType() == ChangeType.DEPENDENT) {
 
 			doAddPLEntryDependentEntry(pickListId, pickListEntryNode);
 		}
 
 		/* 2. Revise dependent pickList definition entry properties. */
 		if (pickListEntryNode.getProperties() != null) {
 
 			Property[] propertyList = pickListEntryNode.getProperties()
 					.getProperty();
 
 			for (int i = 0; i < propertyList.length; i++) {
 				vsPropertyService.revisePickListEntryNodeProperty(pickListId,
 						pickListEntryId, propertyList[i]);
 			}
 		}
 	}
 
 	@Override
 	public void revise(String pickListId, PickListEntryNode pickListEntryNode)
 			throws LBException {
 
 		if (validRevision(pickListId, pickListEntryNode)) {
 
 			ChangeType changeType = pickListEntryNode.getEntryState()
 					.getChangeType();
 
 			if (changeType == ChangeType.NEW) {
 
 				this.insertPickListEntryNode(pickListId, pickListEntryNode);
 			} else if (changeType == ChangeType.REMOVE) {
 
 				this.removePickListEntryNode(pickListId, pickListEntryNode);
 			} else if (changeType == ChangeType.MODIFY) {
 
 				this.updatePickListEntryNode(pickListId, pickListEntryNode);
 			} else if (changeType == ChangeType.DEPENDENT) {
 
 				this.insertDependentChanges(pickListId, pickListEntryNode);
 			} else if (changeType == ChangeType.VERSIONABLE) {
 
 				this.updateVersionableAttributes(pickListId, pickListEntryNode);
 			}
 		}
 	}
 
 	@Override
 	public PickListEntryNode resolvePickListEntryNodeByRevision(
 			String pickListId, String plEntryId, String revisionId)
 			throws LBRevisionException {
 		PickListEntryNodeDao pickListEntryNodeDao = this.getDaoManager()
 				.getCurrentPickListEntryNodeDao();
 
 		return pickListEntryNodeDao.resolvePLEntryNodeByRevision(pickListId,
 				plEntryId, revisionId);
 	}
 	
 	/**
 	 * @return the vsPropertyService
 	 */
 	public VSPropertyService getVsPropertyService() {
 		return vsPropertyService;
 	}
 
 	/**
 	 * @param vsPropertyService
 	 *            the vsPropertyService to set
 	 */
 	public void setVsPropertyService(VSPropertyService vsPropertyService) {
 		this.vsPropertyService = vsPropertyService;
 	}
 
 	private boolean validRevision(String pickListId,
 			PickListEntryNode pickListEntryNode) throws LBException {
 		
 		if (pickListEntryNode == null)
 			throw new LBParameterException("pickListEntryNode is null.");
 	
 		EntryState entryState = pickListEntryNode.getEntryState();
 	
 		if (entryState == null) {
 			throw new LBRevisionException("EntryState can't be null.");
 		}
 	
 		ChangeType changeType = entryState.getChangeType();
 	
 		PickListEntryNodeDao pickListEntryDao = this.getDaoManager().getCurrentPickListEntryNodeDao();
 		
 		String pickListEntryNodeUId = pickListEntryDao.getPickListEntryNodeUId(
 				pickListId, pickListEntryNode.getPickListEntryId());
 	
 		if (changeType == ChangeType.NEW) {
 			if (entryState.getPrevRevision() != null) {
 				throw new LBRevisionException(
 						"Changes of type NEW are not allowed to have previous revisions.");
 			}
 	
 			if (pickListEntryNodeUId != null) {
 				throw new LBRevisionException(
 						"The picklist entry node being added already exist.");
 			}
 	
 		} else {
 	
 			if (pickListEntryNodeUId == null) {
 				throw new LBRevisionException(
 						"The picklist entry node being revised doesn't exist.");
 			}
 	
 			String pickListEntryNodeLatestRevisionId = pickListEntryDao
 					.getLatestRevision(pickListEntryNodeUId);
 	
 			String currentRevision = entryState.getContainingRevision();
 			String prevRevision = entryState.getPrevRevision();
 			
 			if (entryState.getPrevRevision() == null
 					&& pickListEntryNodeLatestRevisionId != null
 					&& !pickListEntryNodeLatestRevisionId.equals(currentRevision)) {
 				throw new LBRevisionException(
 						"All changes of type other than NEW should have previous revisions.");
 			} else if (pickListEntryNodeLatestRevisionId != null
 					&& !pickListEntryNodeLatestRevisionId.equals(currentRevision)
 					&& !pickListEntryNodeLatestRevisionId.equals(prevRevision)) {
 				throw new LBRevisionException(
 						"Revision source is not in sync with the database revisions. "
 								+ "Previous revision id does not match with the latest revision id of the picklist entry node."
 								+ "Please update the authoring instance with all the revisions and regenerate the source.");
 			}
 		}
 		
 		return true;
 	}
 	
 	private void doAddPLEntryDependentEntry(String pickListId,
 			PickListEntryNode pickListEntryNode) {
 	
 		String pickListEntryId = pickListEntryNode.getPickListEntryId();
 	
 		PickListEntryNodeDao pickListEntryNodeDao = this.getDaoManager()
 				.getCurrentPickListEntryNodeDao();
 	
 		String pickListEntryNodeUId = pickListEntryNodeDao
 				.getPickListEntryNodeUId(pickListId, pickListEntryId);
 	
 		String prevEntryStateUId = pickListEntryNodeDao
 				.getPickListEntryStateUId(pickListEntryNodeUId);
 	
 		pickListEntryNodeDao.createEntryStateIfAbsent(prevEntryStateUId,
 				pickListEntryNodeUId);
 	
 		String entryStateUId = this.getDaoManager().getCurrentVsEntryStateDao()
 				.insertEntryState(pickListEntryNodeUId,
 						ReferenceType.PICKLISTENTRY.name(), prevEntryStateUId,
 						pickListEntryNode.getEntryState());
 	
 		pickListEntryNodeDao.updateEntryStateUId(pickListEntryNodeUId,
 				entryStateUId);
 	}
 }
