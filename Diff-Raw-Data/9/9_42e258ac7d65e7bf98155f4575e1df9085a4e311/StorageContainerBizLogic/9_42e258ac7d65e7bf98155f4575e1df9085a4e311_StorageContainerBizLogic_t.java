 /**
  * <p>Title: StorageContainerHDAO Class>
  * <p>Description:	StorageContainerHDAO is used to add Storage Container information into the database using Hibernate.</p>
  * Copyright:    Copyright (c) year
  * Company: Washington University, School of Medicine, St. Louis.
  * @author Aniruddha Phadnis
  * @version 1.00
  * Created on Jul 23, 2005
  */
 
 package edu.wustl.catissuecore.bizlogic;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.Vector;
 
 import edu.wustl.catissuecore.domain.CollectionProtocol;
 import edu.wustl.catissuecore.domain.Container;
 import edu.wustl.catissuecore.domain.Site;
 import edu.wustl.catissuecore.domain.Specimen;
 import edu.wustl.catissuecore.domain.SpecimenArray;
 import edu.wustl.catissuecore.domain.SpecimenArrayType;
 import edu.wustl.catissuecore.domain.StorageContainer;
 import edu.wustl.catissuecore.domain.StorageType;
 import edu.wustl.catissuecore.util.global.Constants;
 import edu.wustl.catissuecore.util.global.Utility;
 import edu.wustl.common.beans.NameValueBean;
 import edu.wustl.common.beans.SessionDataBean;
 import edu.wustl.common.bizlogic.DefaultBizLogic;
 import edu.wustl.common.dao.AbstractDAO;
 import edu.wustl.common.dao.DAO;
 import edu.wustl.common.dao.DAOFactory;
 import edu.wustl.common.dao.JDBCDAO;
 import edu.wustl.common.domain.AbstractDomainObject;
 import edu.wustl.common.security.SecurityManager;
 import edu.wustl.common.security.exceptions.SMException;
 import edu.wustl.common.security.exceptions.UserNotAuthorizedException;
 import edu.wustl.common.tree.StorageContainerTreeNode;
 import edu.wustl.common.tree.TreeDataInterface;
 import edu.wustl.common.tree.TreeNode;
 import edu.wustl.common.tree.TreeNodeImpl;
 import edu.wustl.common.util.Permissions;
 import edu.wustl.common.util.dbManager.DAOException;
 import edu.wustl.common.util.global.ApplicationProperties;
 import edu.wustl.common.util.global.Validator;
 import edu.wustl.common.util.logger.Logger;
 
 /**
  * StorageContainerHDAO is used to add Storage Container information into the
  * database using Hibernate.
  * @author aniruddha_phadnis
  */
 public class StorageContainerBizLogic extends DefaultBizLogic implements TreeDataInterface
 {
 
 	/**
 	 * Saves the storageContainer object in the database.
 	 * @param obj The storageType object to be saved.
 	 * @param session The session in which the object is saved.
 	 * @throws DAOException
 	 */
 	protected void insert(Object obj, DAO dao, SessionDataBean sessionDataBean)
 			throws DAOException, UserNotAuthorizedException
 	{
 		StorageContainer container = (StorageContainer) obj;
 		container.setActivityStatus(Constants.ACTIVITY_STATUS_ACTIVE);
 
 		//Setting the Parent Container if applicable
 		int posOneCapacity = 1, posTwoCapacity = 1;
 		int positionDimensionOne = Constants.STORAGE_CONTAINER_FIRST_ROW, positionDimensionTwo = Constants.STORAGE_CONTAINER_FIRST_COLUMN;
 		boolean fullStatus[][] = null;
 
 		if (container.getSimilarContainerMap() == null)
 		{
 			int noOfContainers = container.getNoOfContainers().intValue();
 
 			if (container.getParent() != null)
 			{
 				List list = dao.retrieve(StorageContainer.class.getName(), "id", container
 						.getParent().getId());
 
 				if (list.size() != 0)
 				{
 					StorageContainer pc = (StorageContainer) list.get(0);
 
 					// check for closed ParentContainer
 					checkStatus(dao, pc, "Parent Container");
 
 					int totalCapacity = pc.getCapacity().getOneDimensionCapacity().intValue()
 							* pc.getCapacity().getTwoDimensionCapacity().intValue();
 					if ((noOfContainers + pc.getChildren().size()) > totalCapacity)
 					{
 						throw new DAOException(ApplicationProperties
 								.getValue("errors.storageContainer.overflow"));
 					}
 					else
 					{
 
 						//Check if position specified is within the parent
 						// container's
 						//capacity  
 						if (false == validatePosition(pc, container))
 						{
 							throw new DAOException(ApplicationProperties
 									.getValue("errors.storageContainer.dimensionOverflow"));
 						}
 
 						// check for availability of position
 						boolean canUse = isContainerAvailableForPositions(dao, container);
 
 						if (!canUse)
 						{
 							throw new DAOException(ApplicationProperties
 									.getValue("errors.storageContainer.inUse"));
 						}
 
 						container.setParent(pc);
 
 						// check for closed ParentSite
 						checkStatus(dao, pc.getSite(), "Parent Site");
 
 						container.setSite(pc.getSite());
 
 						posOneCapacity = pc.getCapacity().getOneDimensionCapacity().intValue();
 						posTwoCapacity = pc.getCapacity().getTwoDimensionCapacity().intValue();
 
 						fullStatus = getStorageContainerFullStatus(dao, container.getParent()
 								.getId());
 						positionDimensionOne = container.getPositionDimensionOne().intValue();
 						positionDimensionTwo = container.getPositionDimensionTwo().intValue();
 					}
 				}
 				else
 				{
 					throw new DAOException(ApplicationProperties
 							.getValue("errors.storageContainerExist"));
 				}
 			}
 			else
 			{
 				loadSite(dao, container);
 			}
 
 			loadStorageType(dao, container);
 
 			for (int i = 0; i < noOfContainers; i++)
 			{
 				StorageContainer cont = new StorageContainer(container);
 				if (cont.getParent() != null)
 				{
 					cont.setPositionDimensionOne(new Integer(positionDimensionOne));
 					cont.setPositionDimensionTwo(new Integer(positionDimensionTwo));
 				}
 
 				Logger.out.debug("Collection protocol size:"
 						+ container.getCollectionProtocolCollection().size());
 
 				dao.insert(cont.getCapacity(), sessionDataBean, true, true);
 				dao.insert(cont, sessionDataBean, true, true);
 
 				//Used for showing the success message after insert and using it
 				// for edit.
 				container.setId(cont.getId());
 
 				if (container.getParent() != null)
 				{
 					Logger.out.debug("In if: ");
 					do
 					{
 						if (positionDimensionTwo == posTwoCapacity)
 						{
 							if (positionDimensionOne == posOneCapacity)
 								positionDimensionOne = Constants.STORAGE_CONTAINER_FIRST_ROW;
 							else
 								positionDimensionOne = (positionDimensionOne + 1)
 										% (posOneCapacity + 1);
 
 							positionDimensionTwo = Constants.STORAGE_CONTAINER_FIRST_COLUMN;
 						}
 						else
 						{
 							positionDimensionTwo = positionDimensionTwo + 1;
 						}
 
 						Logger.out.debug("positionDimensionTwo: " + positionDimensionTwo);
 						Logger.out.debug("positionDimensionOne: " + positionDimensionOne);
 					}
 					while (fullStatus[positionDimensionOne][positionDimensionTwo] != false);
 				}
 
 				//Inserting authorization data
 				Set protectionObjects = new HashSet();
 				protectionObjects.add(cont);
 				try
 				{
 					SecurityManager.getInstance(this.getClass()).insertAuthorizationData(null,
 							protectionObjects, getDynamicGroups(cont));
 				}
 				catch (SMException e)
 				{
 					throw handleSMException(e);
 				}
 			}
 		}
 		else
 		// if similarContainerMap is not null
 		{
 			int noOfContainers = container.getNoOfContainers().intValue();
 			Map simMap = container.getSimilarContainerMap();
 			// --- common values for all similar containers ---
 			loadStorageType(dao, container);
 			Logger.out.debug("cont.getCollectionProtocolCollection().size()  "
 					+ container.getCollectionProtocolCollection().size());
 			Logger.out.debug("container.getParentContainer() site id -->>()<<-- "
 					+ container.getParent()); //.getSite().getId()
 			Logger.out.debug("Container siteId " + container.getSite());
 			Logger.out.info(simMap);
 			int checkButton = Integer.parseInt((String) simMap.get("checkedButton"));
 			//int checkButton = 1;
 
 			for (int i = 1; i <= noOfContainers; i++)
 			{
 				String simContPrefix = "simCont:" + i + "_";
 				String contName = (String) simMap.get(simContPrefix + "name");
 				String barcode = (String) simMap.get(simContPrefix + "barcode");
 				if (barcode != null && barcode.equals("")) // this is done because barcode is empty string set by struts
 				{ // but barcode in DB is unique but can be null.
 					barcode = null;
 				}
 				StorageContainer cont = new StorageContainer(container);
 				if (checkButton == 1) // site
 				{
 					String siteId = (String) simMap.get(simContPrefix + "siteId");
 					Site site = new Site();
 					site.setId(new Long(siteId));
 					cont.setSite(site);
 					loadSite(dao, cont); // <<----
 
 				}
 				else
 				// parentContainer
 				{
 					String parentId = (String) simMap.get(simContPrefix + "parentContainerId");
 					String posOne = (String) simMap.get(simContPrefix + "positionDimensionOne");
 					String posTwo = (String) simMap.get(simContPrefix + "positionDimensionTwo");
 
 					StorageContainer parentContainer = new StorageContainer();
 					parentContainer.setId(new Long(parentId));
 					parentContainer.setPositionDimensionOne(new Integer(posOne));
 					parentContainer.setPositionDimensionTwo(new Integer(posTwo));
 					cont.setParent(parentContainer); // <<----
 					
 					
 					//chk for positions 
 					// check for availability of position
 					boolean canUse = isContainerAvailableForPositions(dao, parentContainer);
 
 					if (!canUse)
 					{
 						throw new DAOException(ApplicationProperties
 								.getValue("errors.storageContainer.inUse"));
 					}
 
 
 					// Have to set Site object for parentContainer
 					loadSite(dao, parentContainer); // 17-07-2006
 					loadSiteFromContainerId(dao, parentContainer);
 					cont.setPositionDimensionOne(new Integer(posOne));
 					cont.setPositionDimensionTwo(new Integer(posTwo));
 					cont.setSite(parentContainer.getSite()); // 16-07-2006 chetan
 					Logger.out.debug("^^>> " + parentContainer.getSite());
 				}
 				//StorageContainer cont = new StorageContainer();
 				cont.setName(contName); // <<----
 				cont.setBarcode(barcode); // <<----     		
 
 				Logger.out.debug(cont.getParent() + " <<<<---- parentContainer");
 				Logger.out.debug("cont.getCollectionProtocol().size() "
 						+ cont.getCollectionProtocolCollection().size());
 				cont.setActivityStatus("Active");
 				dao.insert(cont.getCapacity(), sessionDataBean, true, true);
 				dao.insert(cont, sessionDataBean, true, true);
 
 				container.setId(cont.getId());
 
 				//        		Inserting authorization data
 				Set protectionObjects = new HashSet();
 				protectionObjects.add(cont);
 				try
 				{
 					SecurityManager.getInstance(this.getClass()).insertAuthorizationData(null,
 							protectionObjects, getDynamicGroups(cont));
 				}
 				catch (SMException e)
 				{
 					throw handleSMException(e);
 				}
 			}
 
 		}
 	}
 
 	//	public Set getProtectionObjects(AbstractDomainObject obj)
 	//    {
 	//        Set protectionObjects = new HashSet();
 	//        
 	//        StorageContainer storageContainer = (StorageContainer) obj;
 	//        protectionObjects.add(storageContainer);
 	//        
 	//        Iterator storageContainerIterator = storageContainer.getChildrenContainerCollection().iterator();
 	//        while (storageContainerIterator.hasNext())
 	//        {
 	//            StorageContainer childContainer = (StorageContainer) storageContainerIterator.next();
 	//            protectionObjects.add(childContainer);
 	//        }
 	//        
 	//        Logger.out.debug(protectionObjects.toString());
 	//        return protectionObjects;
 	//    }
 
 	// This method sets the collection Storage Types.
 	private String[] getDynamicGroups(AbstractDomainObject obj) throws SMException
 	{
 		String[] dynamicGroups = null;
 		StorageContainer storageContainer = (StorageContainer) obj;
 
 		if (storageContainer.getParent() != null)
 		{
 			dynamicGroups = SecurityManager.getInstance(this.getClass()).getProtectionGroupByName(
 					storageContainer.getParent());
 		}
 		else
 		{
 			dynamicGroups = SecurityManager.getInstance(this.getClass()).getProtectionGroupByName(
 					storageContainer.getSite());
 		}
 		return dynamicGroups;
 	}
 
 	/**
 	 * Updates the persistent object in the database.
 	 * 
 	 * @param obj The object to be updated.
 	 * @param session The session in which the object is saved.
 	 * @throws DAOException
 	 */
 	protected void update(DAO dao, Object obj, Object oldObj, SessionDataBean sessionDataBean)
 			throws DAOException, UserNotAuthorizedException
 	{
 		StorageContainer container = (StorageContainer) obj;
 		StorageContainer oldContainer = (StorageContainer) oldObj;
 		Logger.out.debug("container.isParentChanged() : " + container.isParentChanged());
 
 		if (container.isParentChanged())
 		{
 			if (container.getParent() != null)
 			{
 				//Check whether continer is moved to one of its sub container.
 				if (isUnderSubContainer(container, container.getParent().getId()))
 				{
 					throw new DAOException(ApplicationProperties
 							.getValue("errors.container.under.subcontainer"));
 				}
 				Logger.out.debug("Loading ParentContainer: " + container.getParent().getId());
 
 				StorageContainer pc = (StorageContainer) dao.retrieve(StorageContainer.class
 						.getName(), container.getParent().getId());
 
 				/* Check if position specified is within the parent container's capacity*/
 				if (false == validatePosition(pc, container))
 				{
 					throw new DAOException(ApplicationProperties
 							.getValue("errors.storageContainer.dimensionOverflow"));
 				}
 
 				// Mandar : code added for validation bug id 666. 24-11-2005  start
 				boolean canUse = isContainerAvailableForPositions(dao, container);
 				Logger.out.debug("canUse : " + canUse);
 				if (!canUse)
 				{
 					throw new DAOException(ApplicationProperties
 							.getValue("errors.storageContainer.inUse"));
 				}
 				// Mandar : code added for validation bug id 666. 24-11-2005 end
 
 				//check for closed ParentContainer
 				checkStatus(dao, pc, "Parent Container");
 
 				container.setParent(pc);
 				//check for closed Site
 				checkStatus(dao, pc.getSite(), "Parent Site");
 
 				container.setSite(pc.getSite());
 			}
 		}
 		// Mandar : code added for validation 25-11-05-----------
 		else
 		// if parent container is not changed only the position is changed.
 		{
 			if (container.isPositionChanged())
 			{
 				// -----------------
 				String sourceObjectName = StorageContainer.class.getName();
 				String[] selectColumnName = {"id", "capacity.oneDimensionCapacity",
 						"capacity.twoDimensionCapacity"};
 				String[] whereColumnName = {"id"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 				String[] whereColumnCondition = {"="};
 				Object[] whereColumnValue = {container.getParent().getId()};
 				String joinCondition = null;
 
 				List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 						whereColumnCondition, whereColumnValue, joinCondition);
 
 				if (!list.isEmpty())
 				{
 					Object[] obj1 = (Object[]) list.get(0);
 					Logger.out.debug("**************PC obj::::::: --------------- " + obj1);
 					Logger.out.debug((Long) obj1[0]);
 					Logger.out.debug((Integer) obj1[1]);
 					Logger.out.debug((Integer) obj1[2]);
 
 					Integer pcCapacityOne = (Integer) obj1[1];
 					Integer pcCapacityTwo = (Integer) obj1[2];
 
 					if (!validatePosition(pcCapacityOne.intValue(), pcCapacityTwo.intValue(),
 							container))
 					{
 						throw new DAOException(ApplicationProperties
 								.getValue("errors.storageContainer.dimensionOverflow"));
 					}
 				}
 				else
 				{
 
 				}
 				// -----------------
 				//            	StorageContainer pc = (StorageContainer) dao.retrieve(StorageContainer.class.getName(), container.getParentContainer().getId());
 
 				//        		if(!validatePosition(container.getParentContainer().getStorageContainerCapacity().getOneDimensionCapacity().intValue(),
 				//          			container.getParentContainer().getStorageContainerCapacity().getTwoDimensionCapacity().intValue(),
 				//					container))
 				//	           	 /*Check if position specified is within the parent container's capacity*/
 				////	           	if(!validatePosition(pc,container))
 				//	           	{
 				//	           		throw new DAOException(ApplicationProperties.getValue("errors.storageContainer.dimensionOverflow"));
 				//	           	}
 				//
 				/**
 				 * Only if parentContainerID, positionOne or positionTwo is changed
 				 *  check for availability of position
 				 */
 				Logger.out.debug("oldContainer.getParentContainer().getId().longValue() : "
 						+ oldContainer.getParent().getId().longValue());
 				Logger.out.debug("container.getParentContainer().getId().longValue() : "
 						+ container.getParent().getId().longValue());
 				Logger.out.debug("oldContainer.getPositionDimensionOne().intValue() : "
 						+ oldContainer.getPositionDimensionOne().intValue());
 				Logger.out.debug("container.getPositionDimensionOne().intValue() : "
 						+ container.getPositionDimensionOne().intValue());
 				Logger.out.debug("oldContainer.getPositionDimensionTwo().intValue() : "
 						+ oldContainer.getPositionDimensionTwo().intValue());
 				Logger.out.debug("container.getPositionDimensionTwo().intValue() : "
 						+ container.getPositionDimensionTwo().intValue());
 
 				if (oldContainer.getPositionDimensionOne().intValue() != container
 						.getPositionDimensionOne().intValue()
 						|| oldContainer.getPositionDimensionTwo().intValue() != container
 								.getPositionDimensionTwo().intValue())
 				{
 					boolean canUse = isContainerAvailableForPositions(dao, container);
 					Logger.out.debug("canUse : " + canUse);
 					if (!canUse)
 					{
 						throw new DAOException(ApplicationProperties
 								.getValue("errors.storageContainer.inUse"));
 					}
 				}
 
 			}
 		}
 
 		// Mandar : --------- end  25-11-05 -----------------		
 
 		//Check whether size has been reduced
 		//Sri: fix for bug #355 (Storage capacity: Reducing capacity should be
 		// handled)
 
 		Integer oldContainerDimOne = oldContainer.getCapacity().getOneDimensionCapacity();
 		Integer oldContainerDimTwo = oldContainer.getCapacity().getTwoDimensionCapacity();
 		Integer newContainerDimOne = container.getCapacity().getOneDimensionCapacity();
 		Integer newContainerDimTwo = container.getCapacity().getTwoDimensionCapacity();
 
 		// If any size of reduced, throw error
 		if ((oldContainerDimOne.compareTo(newContainerDimOne) > 0)
 				|| (oldContainerDimTwo.compareTo(newContainerDimTwo) > 0))
 		{
 			throw new DAOException(ApplicationProperties
 					.getValue("errors.storageContainer.cannotReduce"));
 		}
 
 		//Check for closed Site
 		if ((container.getSite() != null) && (oldContainer.getSite() != null))
 		{
 			if ((container.getSite().getId() != null) && (oldContainer.getSite().getId() != null))
 			{
 				if ((!container.getSite().getId().equals(oldContainer.getSite().getId())))
 				{
 					checkStatus(dao, container.getSite(), "Site");
 				}
 			}
 		}
 		setSiteForSubContainers(container, container.getSite());
 
 		boolean restrictionsCanChange = isContainerEmpty(dao, container);
 		Logger.out.info("--------------container Available :" + restrictionsCanChange);
 		if (!restrictionsCanChange)
 		{
 
 			boolean restrictionsChanged = checkForRestrictionsChanged(container, oldContainer);
 			Logger.out.info("---------------restriction changed -:" + restrictionsChanged);
 			if (restrictionsChanged)
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("errros.storageContainer.restrictionCannotChanged"));
 			}
 
 		}
 
 		dao.update(container, sessionDataBean, true, true, false);
 
 		//Audit of update of storage container.
 		dao.audit(obj, oldObj, sessionDataBean, true);
 		dao.audit(container.getCapacity(), oldContainer.getCapacity(), sessionDataBean, true);
 
 		Logger.out.debug("container.getActivityStatus() " + container.getActivityStatus());
 		if (container.getActivityStatus().equals(Constants.ACTIVITY_STATUS_DISABLED))
 		{
 			Long containerIDArr[] = {container.getId()};
 			if (isContainerAvailableForDisabled(dao, containerIDArr))
 			{
 				setDisableToSubContainer(container);
 				Logger.out.debug("container.getActivityStatus() " + container.getActivityStatus());
 				/*Long containerIDArr[] = {container.getId()};*/
 
 				disableSubStorageContainer(dao, sessionDataBean, containerIDArr);
 				container.setParent(null);
 				container.setPositionDimensionOne(null);
 				container.setPositionDimensionTwo(null);
 				dao.update(container, sessionDataBean, true, true, false);
 			}
 			else
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("errors.container.contains.specimen"));	
 			}
 		}
 	}
 
 	public boolean isContainerFull(String containerId) throws DAOException
 	{
 		boolean availablePositions[][] = getAvailablePositions(containerId);
 
 		int dimX = availablePositions.length;
 		for (int x = 1; x < dimX; x++)
 		{
 			int dimY = availablePositions[x].length;
 			for (int y = 1; y < dimY; y++)
 			{
 				if (availablePositions[x][y] == true)
 					return false;
 			}
 		}
 		return true;
 
 	}
 
 	private boolean checkForRestrictionsChanged(StorageContainer newContainer,
 			StorageContainer oldContainer)
 	{
 		int flag = 0;
 		Collection cpCollNew = newContainer.getCollectionProtocolCollection();
 		Collection cpCollOld = oldContainer.getCollectionProtocolCollection();
 
 		Collection storTypeCollNew = newContainer.getHoldsStorageTypeCollection();
 		Collection storTypeCollOld = oldContainer.getHoldsStorageTypeCollection();
 
 		Collection spClassCollNew = newContainer.getHoldsSpecimenClassCollection();
 		Collection spClassCollOld = oldContainer.getHoldsSpecimenClassCollection();
 
 		Collection spArrayTypeCollNew = newContainer.getHoldsSpArrayTypeCollection();
 		Collection spArrayTypeCollOld = oldContainer.getHoldsSpArrayTypeCollection();
 
 		if (cpCollNew.size() != cpCollOld.size())
 			return true;
 
 		Iterator itrNew = cpCollNew.iterator();
 
 		while (itrNew.hasNext())
 		{
 			flag = 0;
 			CollectionProtocol cpNew = (CollectionProtocol) itrNew.next();
 			Iterator itrOld = cpCollOld.iterator();
 			while (itrOld.hasNext())
 			{
 				CollectionProtocol cpOld = (CollectionProtocol) itrOld.next();
 				if (cpOld.getId().longValue() == cpNew.getId().longValue())
 				{
 					flag = 1;
 					break;
 				}
 			}
 			if (flag != 1)
 				return true;
 		}
 
 		if (storTypeCollNew.size() != storTypeCollOld.size())
 			return true;
 
 		itrNew = storTypeCollNew.iterator();
 
 		while (itrNew.hasNext())
 		{
 			flag = 0;
 			StorageType storNew = (StorageType) itrNew.next();
 			Iterator itrOld = storTypeCollOld.iterator();
 			while (itrOld.hasNext())
 			{
 				StorageType storOld = (StorageType) itrOld.next();
 	
 				if (storNew.getId().longValue() == storOld.getId().longValue())
 				{
 					flag = 1;
 					break;
 				}
 				if (flag != 1)
 					return true;
 			}
 		}
 
 		if (spClassCollNew.size() != spClassCollOld.size())
 			return true;
 
 		itrNew = spClassCollNew.iterator();
 
 		while (itrNew.hasNext())
 		{
 			flag = 0;
 			String specimenNew = (String) itrNew.next();
 			Iterator itrOld = spClassCollOld.iterator();
 			while (itrOld.hasNext())
 			{
 				String specimenOld = (String) itrOld.next();
 				if (specimenNew.equals(specimenOld))
 				{
 					flag = 1;
 					break;
 				}
 			}
 			if (flag != 1)
 				return true;
 		}
 
 		if (spArrayTypeCollNew.size() != spArrayTypeCollOld.size())
 			return true;
 
 		itrNew = spArrayTypeCollNew.iterator();
 
 		while (itrNew.hasNext())
 		{
 			flag = 0;
 			SpecimenArrayType spArrayTypeNew = (SpecimenArrayType) itrNew.next();
 			Iterator itrOld = spArrayTypeCollOld.iterator();
 			while (itrOld.hasNext())
 			{
 				SpecimenArrayType spArrayTypeOld = (SpecimenArrayType) itrOld.next();
 				
 				if (spArrayTypeNew.getId().longValue() == spArrayTypeOld.getId().longValue())
 				{
 					flag = 1;
 					break;
 				}
 			}
 			if (flag != 1)
 				return true;
 		}
 
 		return false;
 	}
 
 	protected void setPrivilege(DAO dao, String privilegeName, Class objectType, Long[] objectIds,
 			Long userId, String roleId, boolean assignToUser, boolean assignOperation)
 			throws SMException, DAOException
 	{
 		Logger.out.debug(" privilegeName:" + privilegeName + " objectType:" + objectType
 				+ " objectIds:" + edu.wustl.common.util.Utility.getArrayString(objectIds)
 				+ " userId:" + userId + " roleId:" + roleId + " assignToUser:" + assignToUser);
 
 		
 		// Aarti: Bug#1199 - We should be able to deassign 
 		// privilege on child even though user has privilege on the parent.
 		// Thus commenting the check for privileges on parent.
 //		if (assignOperation == Constants.PRIVILEGE_DEASSIGN)
 //		{
 //			isDeAssignable(dao, privilegeName, objectIds, userId, roleId, assignToUser);
 //		}
 
 		super.setPrivilege(dao, privilegeName, objectType, objectIds, userId, roleId, assignToUser,
 				assignOperation);
 
 		assignPrivilegeToSubStorageContainer(dao, privilegeName, objectIds, userId, roleId,
 				assignToUser, assignOperation);
 	}
 
 	/**
 	 * Checks whether the user/role has privilege on the parent (Container/Site). If the user has privilege
 	 * an exception is thrown stating to deassign the privilege of parent first. 
 	 * @param dao The dao object to get the related objects down the hierarchy. 
 	 * @param objectIds The objects ids of containerwhose parent is to be checked.
 	 * @param privilegeName The privilege name.
 	 * @param userId The user identifier.
 	 * @param roleId The roleId in case privilege is assigned/deassigned to a role.
 	 * @param assignToUser boolean which determines whether privilege is assigned/deassigned to a user or role. 
 	 * @throws DAOException
 	 * @throws SMException
 	 */
 	private void isDeAssignable(DAO dao, String privilegeName, Long[] objectIds, Long userId,
 			String roleId, boolean assignToUser) throws DAOException, SMException
 	{
 		// Aarti: Bug#2364 - Error while assigning privileges since attribute parentContainer changed to parent
 		String[] selectColumnNames = {"parent.id", "site.id"};
 		String[] whereColumnNames = {"id"};
 		List listOfSubElement = super.getRelatedObjects(dao, StorageContainer.class,
 				selectColumnNames, whereColumnNames, objectIds);
 
 		Logger.out.debug("Related Objects>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
 				+ listOfSubElement.size());
 
 		String userName = new String();
 		if (assignToUser == true)
 		{
 			userName = SecurityManager.getInstance(StorageContainerBizLogic.class).getUserById(
 					userId.toString()).getLoginName();
 		}
 
 		Iterator iterator = listOfSubElement.iterator();
 		while (iterator.hasNext())
 		{
 			Object[] row = (Object[]) iterator.next();
 
 			//Parent storage container identifier.
 			Object containerObject = (Object) row[0];
 			String className = StorageContainer.class.getName();
 
 			// Parent storage container identifier is null, the parent is a site..
 			if ((row[0] == null) || (row[0].equals("")))
 			{
 				containerObject = row[1];
 				className = Site.class.getName();
 			}
 
 			Logger.out.debug("Container Object After ********************** : " + containerObject
 					+ "row[1] : " + row[1]);
 
 			boolean permission = false;
 			//Check the permission on the parent container or site.
 			if (assignToUser == true)//If the privilege is assigned/deassigned to a user.
 			{
 				permission = SecurityManager.getInstance(StorageContainerBizLogic.class)
 						.checkPermission(userName, className, containerObject.toString(),
 								privilegeName);
 			}
 			else
 			//If the privilege is assigned/deassigned to a user group.
 			{
 				permission = SecurityManager.getInstance(StorageContainerBizLogic.class)
 						.checkPermission(roleId, className, containerObject.toString());
 			}
 
 			//If the parent is a Site.
 			if (permission == true && row[0] == null)
 			{
 				throw new DAOException(
 						"Error : First de-assign privilege of the Parent Site with system identifier "
 								+ row[1].toString());
 			}
 			else if (permission == true && row[0] != null)//If the parent is a storage container. 
 			{
 				throw new DAOException(
 						"Error : First de-assign privilege of the Parent Container with system identifier "
 								+ row[0].toString());
 			}
 		}
 	}
 
 	/**
 	 * Assigns the privilege to all the sub-containers down the hierarchy.
 	 * @param dao The dao object to get the related objects down the hierarchy. 
 	 * @param privilegeName The privilege name.
 	 * @param storageContainerIDArr The storage container id array.
 	 * @param userId The user identifier.
 	 * @param roleId The roleId in case privilege is assigned/deassigned to a role.
 	 * @param assignToUser boolean which determines whether privilege is assigned/deassigned to a user or role. 
 	 * @param assignOperation boolean which determines assign/deassign.
 	 * @throws SMException
 	 * @throws DAOException
 	 */
 	private void assignPrivilegeToSubStorageContainer(DAO dao, String privilegeName,
 			Long[] storageContainerIDArr, Long userId, String roleId, boolean assignToUser,
 			boolean assignOperation) throws SMException, DAOException
 	{
 		// Aarti: Bug#2364 - Error while assigning privileges since attribute parentContainer changed to parent
 		//Get list of sub container identifiers.
 		List listOfSubStorageContainerId = super.getRelatedObjects(dao, StorageContainer.class,
 				"parent", storageContainerIDArr);
 
 		if (listOfSubStorageContainerId.isEmpty())
 			return;
 
 		super.setPrivilege(dao, privilegeName, StorageContainer.class, Utility
 				.toLongArray(listOfSubStorageContainerId), userId, roleId, assignToUser,
 				assignOperation);
 
 		assignPrivilegeToSubStorageContainer(dao, privilegeName, Utility
 				.toLongArray(listOfSubStorageContainerId), userId, roleId, assignToUser,
 				assignOperation);
 	}
 
 	/**
 	 * @param dao
 	 * @param objectIds
 	 * @param assignToUser
 	 * @param roleId
 	 * @throws DAOException
 	 * @throws SMException
 	 */
 	public void assignPrivilegeToRelatedObjectsForSite(DAO dao, String privilegeName,
 			Long[] objectIds, Long userId, String roleId, boolean assignToUser,
 			boolean assignOperation) throws SMException, DAOException
 	{
 		List listOfSubElement = super.getRelatedObjects(dao, StorageContainer.class, "site",
 				objectIds);
 
 		if (!listOfSubElement.isEmpty())
 		{
 			super.setPrivilege(dao, privilegeName, StorageContainer.class, Utility
 					.toLongArray(listOfSubElement), userId, roleId, assignToUser, assignOperation);
 		}
 	}
 
 	// This method sets the Storage Type & Site (if applicable) of this
 	// container.
 	private void loadSite(DAO dao, StorageContainer container) throws DAOException
 	{
 		//Setting the site if applicable
 		if (container.getSite() != null)
 		{
 			Object siteObj = dao.retrieve(Site.class.getName(), container.getSite().getId());
 			if (siteObj != null)
 			{
 				Site site = (Site) siteObj;
 
 				// check for closed site
 				checkStatus(dao, site, "Site");
 
 				container.setSite(site);
 				setSiteForSubContainers(container, site);
 			}
 		}
 	}
 
 	private void loadStorageType(DAO dao, StorageContainer container) throws DAOException
 	{
 		//Setting the Storage Type
 		Object storageTypeObj = dao.retrieve(StorageType.class.getName(), container
 				.getStorageType().getId());
 		if (storageTypeObj != null)
 		{
 			StorageType type = (StorageType) storageTypeObj;
 			container.setStorageType(type);
 		}
 	}
 
 	private void setSiteForSubContainers(StorageContainer storageContainer, Site site)
 	{
 		if (storageContainer != null)
 		{
 			Logger.out.debug("site() " + site.getId());
 			Logger.out.debug("storageContainer.getChildrenContainerCollection() "
 					+ storageContainer.getChildren().size());
 
 			Iterator iterator = storageContainer.getChildren().iterator();
 			while (iterator.hasNext())
 			{
 				StorageContainer container = (StorageContainer) iterator.next();
 				container.setSite(site);
 				setSiteForSubContainers(container, site);
 			}
 		}
 	}
 
 	private boolean isUnderSubContainer(StorageContainer storageContainer, Long parentContainerID)
 	{
 		if (storageContainer != null)
 		{
 			Iterator iterator = storageContainer.getChildren().iterator();
 			while (iterator.hasNext())
 			{
 				StorageContainer container = (StorageContainer) iterator.next();
 				//Logger.out.debug("SUB CONTINER container
 				// "+parentContainerID.longValue()+"
 				// "+container.getId().longValue()+"
 				// "+(parentContainerID.longValue()==container.getId().longValue()));
 				if (parentContainerID.longValue() == container.getId().longValue())
 					return true;
 				if (isUnderSubContainer(container, parentContainerID))
 					return true;
 			}
 		}
 		return false;
 	}
 
 	//  TODO TO BE REMOVED
 	private void setDisableToSubContainer(StorageContainer storageContainer)
 	{
 		if (storageContainer != null)
 		{
 			Iterator iterator = storageContainer.getChildren().iterator();
 			while (iterator.hasNext())
 			{
 				StorageContainer container = (StorageContainer) iterator.next();
 				container.setActivityStatus(Constants.ACTIVITY_STATUS_DISABLED);
 				/* whenever container is disabled free it's used positions */
 				container.setParent(null);
 				container.setPositionDimensionOne(null);
 				container.setPositionDimensionTwo(null);
 
 				setDisableToSubContainer(container);
 			}
 		}
 	}
 
 	public long getNextContainerNumber() throws DAOException
 	{
 		String sourceObjectName = "CATISSUE_STORAGE_CONTAINER";
 		String[] selectColumnName = {"max(IDENTIFIER) as MAX_NAME"};
 		AbstractDAO dao = DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 
 		dao.openSession(null);
 
 		List list = dao.retrieve(sourceObjectName, selectColumnName);
 
 		dao.closeSession();
 
 		if (!list.isEmpty())
 		{
 			List columnList = (List) list.get(0);
 			if (!columnList.isEmpty())
 			{
 				String str = (String) columnList.get(0);
 				if (!str.equals(""))
 				{
 					long no = Long.parseLong(str);
 					return no + 1;
 				}
 			}
 		}
 
 		return 1;
 	}
 
 	public String getContainerName(String siteName, String typeName, String operation, long Id)
 			throws DAOException
 	{
 		String containerName = "";
 		if (typeName != null && siteName != null && !typeName.equals("") && !siteName.equals(""))
 		{
 			if (operation.equals(Constants.ADD))
 			{
 				containerName = siteName + "_" + typeName + "_"
 						+ String.valueOf(getNextContainerNumber());
 			}
 			else
 			{
 				containerName = siteName + "_" + typeName + "_" + String.valueOf(Id);
 			}
 
 		}
 		return containerName;
 	}
 
 	public int getNextContainerNumber(long parentID, long typeID, boolean isInSite)
 			throws DAOException
 	{
 		String sourceObjectName = "CATISSUE_STORAGE_CONTAINER";
 		String[] selectColumnName = {"max(IDENTIFIER) as MAX_NAME"};
 		String[] whereColumnName = {"STORAGE_TYPE_ID", "PARENT_CONTAINER_ID"};
 		String[] whereColumnCondition = {"=", "="};
 		Object[] whereColumnValue = {Long.toString(typeID), Long.toString(parentID)};
 
 		if (isInSite)
 		{
 			whereColumnName = new String[3];
 			whereColumnName[0] = "STORAGE_TYPE_ID";
 			whereColumnName[1] = "SITE_ID";
 			whereColumnName[2] = "PARENT_CONTAINER_ID";
 
 			whereColumnValue = new Object[3];
 			whereColumnValue[0] = Long.toString(typeID);
 			whereColumnValue[1] = Long.toString(parentID);
 			whereColumnValue[2] = "null";
 
 			whereColumnCondition = new String[3];
 			whereColumnCondition[0] = "=";
 			whereColumnCondition[1] = "=";
 			whereColumnCondition[2] = "is";
 		}
 		String joinCondition = Constants.AND_JOIN_CONDITION;
 
 		AbstractDAO dao = DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 
 		dao.openSession(null);
 
 		List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 				whereColumnCondition, whereColumnValue, joinCondition);
 
 		dao.closeSession();
 
 		if (!list.isEmpty())
 		{
 			List columnList = (List) list.get(0);
 			if (!columnList.isEmpty())
 			{
 				String str = (String) columnList.get(0);
 				Logger.out.info("str---------------:" + str);
 				if (!str.equals(""))
 				{
 					int no = Integer.parseInt(str);
 					return no + 1;
 				}
 			}
 		}
 
 		return 1;
 	}
 
 	private boolean isContainerEmpty(DAO dao, StorageContainer container) throws DAOException
 	{
 
 		//Retrieving all the occupied positions by child containers
 		String sourceObjectName = StorageContainer.class.getName();
 		String[] selectColumnName = {"positionDimensionOne", "positionDimensionTwo"};
 		String[] whereColumnName = {"parent"};
 		String[] whereColumnCondition = {"="};
 		Object[] whereColumnValue = {container.getId()};
 
 		List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 				whereColumnCondition, whereColumnValue, null);
 
 		if (!list.isEmpty())
 		{
 			return false;
 		}
 		else
 		{
 			//			Retrieving all the occupied positions by specimens
 			sourceObjectName = Specimen.class.getName();
 			whereColumnName[0] = "storageContainer";
 
 			list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 					whereColumnCondition, whereColumnValue, null);
 
 			if (!list.isEmpty())
 			{
 				return false;
 			}
 			else
 			{
 				//				Retrieving all the occupied positions by specimens array type
 				sourceObjectName = SpecimenArray.class.getName();
 				whereColumnName[0] = "storageContainer";
 
 				list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 						whereColumnCondition, whereColumnValue, null);
 
 				if (!list.isEmpty())
 				{
 					return false;
 				}
 
 			}
 
 		}
 
 		return true;
 
 	}
 
 	/**
 	 * Returns the data for generation of storage container tree view.
 	 * @return the vector of tree nodes for the storage containers. 
 	 */
 	public Vector getTreeViewData() throws DAOException
 	{
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		dao.openSession(null);
 
 		//		String queryStr = " SELECT t8.IDENTIFIER, t8.CONTAINER_NAME, t5.TYPE, t8.SITE_ID, "
 		//				+ " t4.TYPE, t8.PARENT_IDENTIFIER, "
 		//				+ " t8.PARENT_CONTAINER_NAME, t8.PARENT_CONTAINER_TYPE "
 		//				+ " FROM (SELECT t7.IDENTIFIER, t7.CONTAINER_NAME, t7.SITE_ID, "
 		//				+ " t7.STORAGE_TYPE_ID, t7.PARENT_IDENTIFIER, "
 		//				+ " t7.PARENT_CONTAINER_NAME, t6.TYPE AS PARENT_CONTAINER_TYPE FROM "
 		//				+ " (select t1.IDENTIFIER AS IDENTIFIER, t1.CONTAINER_NAME AS CONTAINER_NAME, "
 		//				+ " t1.SITE_ID AS SITE_ID, t1.STORAGE_TYPE_ID AS STORAGE_TYPE_ID, "
 		//				+ " t2.IDENTIFIER AS PARENT_IDENTIFIER, t2.CONTAINER_NAME AS PARENT_CONTAINER_NAME, "
 		//				+ " t2.STORAGE_TYPE_ID AS PARENT_STORAGE_TYPE_ID "
 		//				+ " from CATISSUE_STORAGE_CONTAINER t1 LEFT OUTER JOIN CATISSUE_STORAGE_CONTAINER t2 "
 		//				+ " on t1.PARENT_CONTAINER_ID = t2.IDENTIFIER) AS t7 LEFT OUTER JOIN CATISSUE_STORAGE_TYPE t6 "
 		//				+ " on t7.PARENT_STORAGE_TYPE_ID = t6.IDENTIFIER) AS t8, "
 		//				+ " CATISSUE_SITE t4, CATISSUE_STORAGE_TYPE t5 "
 		//				+ " WHERE t8.SITE_ID = t4.IDENTIFIER " + " AND t8.STORAGE_TYPE_ID = t5.IDENTIFIER ";
 
 		String queryStr = "SELECT "
 				+ " t8.IDENTIFIER, t8.CONTAINER_NAME, t5.NAME, t8.SITE_ID, t4.TYPE, t8.PARENT_IDENTIFIER, "
 				+ " t8.PARENT_CONTAINER_NAME, t8.PARENT_CONTAINER_TYPE "
 				+ " FROM ( "
 				+ " 	SELECT "
 				+ " 	  t7.IDENTIFIER, t7.CONTAINER_NAME, t7.SITE_ID, t7.STORAGE_TYPE_ID, t7.PARENT_IDENTIFIER, "
 				+ " 	  t7.PARENT_CONTAINER_NAME, t6.NAME AS PARENT_CONTAINER_TYPE "
 				+ " 	  FROM "
 				+ " 	  ( "
 				+ " 	  select "
 				+ " 	  t1.IDENTIFIER AS IDENTIFIER, t1.NAME AS CONTAINER_NAME, t11.SITE_ID AS SITE_ID, "
 				+ " 	  t11.STORAGE_TYPE_ID AS STORAGE_TYPE_ID, t2.IDENTIFIER AS PARENT_IDENTIFIER, "
 				+ " 	  t2.NAME AS PARENT_CONTAINER_NAME, t22.STORAGE_TYPE_ID AS PARENT_STORAGE_TYPE_ID"
 				+ " 	  from "
 				+ " 	      CATISSUE_STORAGE_CONTAINER t11, CATISSUE_STORAGE_CONTAINER t22, "
 				+ " 	      CATISSUE_CONTAINER t1 LEFT OUTER JOIN CATISSUE_CONTAINER t2 "
 				+ " 	      on t1.PARENT_CONTAINER_ID = t2.IDENTIFIER "
 				+ " 	      where "
 				+ " 		t1.identifier = t11.identifier and  t2.identifier = t22.identifier "
 				+ " 	  ) "
 				+ " 	  t7 LEFT OUTER JOIN CATISSUE_CONTAINER_TYPE t6 on "
 				+ " 	  t7.PARENT_STORAGE_TYPE_ID = t6.IDENTIFIER "
 				+ " ) "
 				+ " t8, CATISSUE_SITE t4, CATISSUE_CONTAINER_TYPE t5 WHERE t8.SITE_ID = t4.IDENTIFIER "
 				+ " AND t8.STORAGE_TYPE_ID = t5.IDENTIFIER ";
 
 		Logger.out.debug("Storage Container query......................" + queryStr);
 		List list = null;
 
 		try
 		{
 			list = dao.executeQuery(queryStr, null, false, null);
 			printRecords(list);
 		}
 		catch (Exception ex)
 		{
 			throw new DAOException(ex.getMessage());
 		}
 
 		dao.closeSession();
 
 		return getTreeNodeList(list);
 	}
 
 	/**
 	 * Returns the vector of tree node for the storage container list.
 	 * @param resultList the storage container list.
 	 * @return the vector of tree node for the storage container list.
 	 * @throws DAOException
 	 */
 	public Vector getTreeNodeList(List resultList) throws DAOException
 	{
 		Map containerRelationMap = new HashMap();
 
 		// Vector of Tree Nodes for all the storage containers.
 		Vector treeNodeVector = new Vector();
 		Vector finalNodeVector = new Vector();
 
 		if (resultList.isEmpty() == false)
 		{
 			Iterator iterator = resultList.iterator();
 
 			while (iterator.hasNext())
 			{
 				List rowList = (List) iterator.next();
 				if ((String) rowList.get(5) != "") //if parent container is not null
 				{
 					List childIds = new ArrayList();
 
 					// Create the relationship map for parent container id and the child container ids.
 					// Check if the parent container already has an entry in the Map and get it.
 					if (containerRelationMap.containsKey(Long.valueOf((String) rowList.get(5))))
 					{
 						childIds = (List) containerRelationMap.get(Long.valueOf((String) rowList
 								.get(5)));
 					}
 
 					// Put the container in the child container list of the parent container
 					// and update the Map. 
 					childIds.add(Long.valueOf((String) rowList.get(0)));
 					containerRelationMap.put(Long.valueOf((String) rowList.get(5)), childIds);
 
 					//Mandar : code for tooltip for the container
 					String toolTip = getToolTipData((String) rowList.get(0));
 
 					// Create the tree node for the child node.
 					TreeNode treeNodeImpl = new StorageContainerTreeNode(Long
 							.valueOf((String) rowList.get(0)), (String) rowList.get(1),
 							(String) rowList.get(1), toolTip);
 
 					// Add the tree node in the Vector if it is not present.
 					if (treeNodeVector.contains(treeNodeImpl) == false)
 					{
 						treeNodeVector.add(treeNodeImpl);
 					}
 
 					// Create the tree node for the parent node and add it in the vector if not present.
 					toolTip = getToolTipData((String) rowList.get(5));
 					treeNodeImpl = new StorageContainerTreeNode(Long.valueOf((String) rowList
 							.get(5)), (String) rowList.get(6), (String) rowList.get(6), toolTip);
 					if (treeNodeVector.contains(treeNodeImpl) == false)
 					{
 						treeNodeVector.add(treeNodeImpl);
 					}
 				}
 			}
 			printVectorMap(treeNodeVector, containerRelationMap);
 
 			finalNodeVector = createHierarchy(containerRelationMap, treeNodeVector);
 		}
 
 		return finalNodeVector;
 	}
 
 	/**
 	 * Creates the hierarchy of the tree nodes of the container according to the container relationship map.
 	 * @param containerRelationMap the container relationship map.
 	 * @param treeNodeVector the vector of tree nodes.
 	 * @return the hierarchy of the tree nodes of the container according to the container relationship map.
 	 * @throws DAOException
 	 */
 	private Vector createHierarchy(Map containerRelationMap, Vector treeNodeVector)
 			throws DAOException
 	{
 
 		//Get the ket set of the parent containers.
 		Set keySet = containerRelationMap.keySet();
 		Iterator iterator = keySet.iterator();
 
 		while (iterator.hasNext())
 		{
 			//Get the parent container id and create the tree node. 
 			Long parentId = (Long) iterator.next();
 			StorageContainerTreeNode parentTreeNodeImpl = new StorageContainerTreeNode(parentId,
 					null, null);
 			parentTreeNodeImpl = (StorageContainerTreeNode) treeNodeVector.get(treeNodeVector
 					.indexOf(parentTreeNodeImpl));
 
 			//Get the child container ids and create the tree nodes.
 			List childNodeList = (List) containerRelationMap.get(parentId);
 			Iterator childIterator = childNodeList.iterator();
 			while (childIterator.hasNext())
 			{
 				Long childId = (Long) childIterator.next();
 				StorageContainerTreeNode childTreeNodeImpl = new StorageContainerTreeNode(childId,
 						null, null);
 				childTreeNodeImpl = (StorageContainerTreeNode) treeNodeVector.get(treeNodeVector
 						.indexOf(childTreeNodeImpl));
 
 				// Set the relationship between the parent and child tree nodes.
 				childTreeNodeImpl.setParentNode(parentTreeNodeImpl);
 				parentTreeNodeImpl.getChildNodes().add(childTreeNodeImpl);
 			}
 		}
 
 		//Get the container whose tree node has parent null 
 		//and get its site tree node and set it as its child.
 		Vector parentNodeVector = new Vector();
 		iterator = treeNodeVector.iterator();
 		System.out.println("\nNodes without Parent\n");
 		while (iterator.hasNext())
 		{
 			StorageContainerTreeNode treeNodeImpl = (StorageContainerTreeNode) iterator.next();
 
 			if (treeNodeImpl.getParentNode() == null)
 			{
 				System.out.print("\n" + treeNodeImpl);
 				TreeNodeImpl siteNode = getSiteTreeNode(treeNodeImpl.getIdentifier());
 				System.out.print("\tSiteNodecreated: " + siteNode);
 				if (parentNodeVector.contains(siteNode))
 				{
 					siteNode = (TreeNodeImpl) parentNodeVector.get(parentNodeVector
 							.indexOf(siteNode));
 					System.out.print("SiteNode Found");
 				}
 				System.out.print("\tSiteNodeSet: " + siteNode);
 				treeNodeImpl.setParentNode(siteNode);
 				siteNode.getChildNodes().add(treeNodeImpl);
 				parentNodeVector.add(siteNode);
 			}
 		}
 
 		//Get the containers under site.
 		Vector containersUnderSite = getContainersUnderSite();
 		containersUnderSite.removeAll(parentNodeVector);
 		parentNodeVector.addAll(containersUnderSite);
 
 		return parentNodeVector;
 	}
 
 	private Vector getContainersUnderSite() throws DAOException
 	{
 		//		String sql = " SELECT sc.IDENTIFIER, sc.CONTAINER_NAME, scType.TYPE, site.IDENTIFIER, site.NAME, site.TYPE "
 		//				+ " from catissue_storage_container sc, catissue_site site, catissue_storage_type scType "
 		//				+ " where sc.SITE_ID = site.IDENTIFIER AND sc.STORAGE_TYPE_ID = scType.IDENTIFIER "
 		//				+ " and sc.PARENT_CONTAINER_ID is NULL";
 
 		String sql = " SELECT sc.IDENTIFIER, cn.NAME, scType.NAME, site.IDENTIFIER, site.NAME, site.TYPE "
 				+ " from catissue_storage_container sc, catissue_site site, catissue_container_type scType, "
 				+ " catissue_container cn  "
 				+ " where sc.SITE_ID = site.IDENTIFIER AND sc.STORAGE_TYPE_ID = scType.IDENTIFIER "
 				+ " and sc.IDENTIFIER = cn.IDENTIFIER " + " and cn.PARENT_CONTAINER_ID is NULL ";
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		List resultList = new ArrayList();
 		Vector containerNodeVector = new Vector();
 
 		try
 		{
 			dao.openSession(null);
 			resultList = dao.executeQuery(sql, null, false, null);
 			dao.closeSession();
 			System.out.println("\nIn getContainersUnderSite()\n ");
 			printRecords(resultList);
 		}
 		catch (Exception daoExp)
 		{
 			throw new DAOException(daoExp.getMessage(), daoExp);
 		}
 
 		Iterator iterator = resultList.iterator();
 		while (iterator.hasNext())
 		{
 			List rowList = (List) iterator.next();
 			StorageContainerTreeNode containerNode = new StorageContainerTreeNode(Long
 					.valueOf((String) rowList.get(0)), (String) rowList.get(1), (String) rowList
 					.get(1));
 			StorageContainerTreeNode siteNode = new StorageContainerTreeNode(Long
 					.valueOf((String) rowList.get(3)), (String) rowList.get(4), (String) rowList
 					.get(4));
 
			if (containerNodeVector.contains(siteNode))
			{
				siteNode = (StorageContainerTreeNode) containerNodeVector.get(containerNodeVector.indexOf(siteNode));
			}
			else
				containerNodeVector.add(siteNode);
 			containerNode.setParentNode(siteNode);
 			siteNode.getChildNodes().add(containerNode);
 		}
 
 		return containerNodeVector;
 	}
 
 	/**
 	 * Returns the site tree node of the container with the given identifier.
 	 * @param identifier the identifier of the container.
 	 * @return the site tree node of the container with the given identifier.
 	 * @throws DAOException
 	 */
 	private TreeNodeImpl getSiteTreeNode(Long identifier) throws DAOException
 	{
 		String sql = "SELECT site.IDENTIFIER, site.NAME, site.TYPE "
 				+ " from catissue_storage_container sc, catissue_site site "
 				+ " where sc.SITE_ID = site.IDENTIFIER AND sc.IDENTIFIER = "
 				+ identifier.longValue();
 
 		Logger.out.debug("Site Query........................." + sql);
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		List resultList = new ArrayList();
 
 		try
 		{
 			dao.openSession(null);
 			resultList = dao.executeQuery(sql, null, false, null);
 			dao.closeSession();
 		}
 		catch (Exception daoExp)
 		{
 			throw new DAOException(daoExp.getMessage(), daoExp);
 		}
 
 		TreeNodeImpl siteTreeNode = null;
 		if (resultList.isEmpty() == false)
 		{
 			List siteRecord = (List) resultList.get(0);
 			siteTreeNode = new StorageContainerTreeNode(Long.valueOf((String) siteRecord.get(0)),
 					(String) siteRecord.get(1), (String) siteRecord.get(1));
 		}
 
 		return siteTreeNode;
 	}
 
 	public boolean[][] getStorageContainerFullStatus(DAO dao, Long id) throws DAOException
 	{
 		List list = dao.retrieve(StorageContainer.class.getName(), "id", id);
 		boolean[][] fullStatus = null;
 
 		if (!list.isEmpty())
 		{
 			StorageContainer storageContainer = (StorageContainer) list.get(0);
 			Integer oneDimensionCapacity = storageContainer.getCapacity().getOneDimensionCapacity();
 			Integer twoDimensionCapacity = storageContainer.getCapacity().getTwoDimensionCapacity();
 			fullStatus = new boolean[oneDimensionCapacity.intValue() + 1][twoDimensionCapacity
 					.intValue() + 1];
 
 			if (storageContainer.getChildren() != null)
 			{
 				Iterator iterator = storageContainer.getChildren().iterator();
 				Logger.out.debug("storageContainer.getChildrenContainerCollection().size(): "
 						+ storageContainer.getChildren().size());
 				while (iterator.hasNext())
 				{
 					StorageContainer childStorageContainer = (StorageContainer) iterator.next();
 					Integer positionDimensionOne = childStorageContainer.getPositionDimensionOne();
 					Integer positionDimensionTwo = childStorageContainer.getPositionDimensionTwo();
 					Logger.out.debug("positionDimensionOne : " + positionDimensionOne.intValue());
 					Logger.out.debug("positionDimensionTwo : " + positionDimensionTwo.intValue());
 					fullStatus[positionDimensionOne.intValue()][positionDimensionTwo.intValue()] = true;
 				}
 			}
 		}
 		return fullStatus;
 	}
 
 	private void disableSubStorageContainer(DAO dao, SessionDataBean sessionDataBean, Long storageContainerIDArr[])
 			throws DAOException,UserNotAuthorizedException
 	{
 		List listOfSpecimenIDs = getRelatedObjects(dao, Specimen.class, "storageContainer",
 				storageContainerIDArr);
 		
 		if (!listOfSpecimenIDs.isEmpty())
 		{
 			throw new DAOException(ApplicationProperties
 					.getValue("errors.container.contains.specimen"));
 		}
 
 		List listOfSubStorageContainerId = super.disableObjects(dao, Container.class, "parent",
 				"CATISSUE_CONTAINER", "PARENT_CONTAINER_ID", storageContainerIDArr);
 
 		if (listOfSubStorageContainerId.isEmpty())
 		{
 			return;
 		}
 		else
 		{
 			Iterator itr = listOfSubStorageContainerId.iterator();
 			while(itr.hasNext())
 			{
 				Long contId = (Long)itr.next();
 				String sourceObjectName = StorageContainer.class.getName();
 				String whereColumnName = "id"; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 				Object whereColumnValue = contId;
 				
 				List containerList = retrieve(sourceObjectName,whereColumnName,whereColumnValue);
 				if(!containerList.isEmpty())
 				{
 					StorageContainer cont = (StorageContainer)containerList.get(0);
 					cont.setParent(null);
 					cont.setPositionDimensionOne(null);
 					cont.setPositionDimensionTwo(null);
 					dao.update(cont,sessionDataBean,true,true,false);
 				}
 				
 			}
 		}
 
 		disableSubStorageContainer(dao, sessionDataBean,Utility.toLongArray(listOfSubStorageContainerId));
 	}
 
 	// Checks for whether the user is trying to use a container without privilege to use it
 	// This is needed since now users can enter the values in the edit box
 	private boolean validateContainerAccess(StorageContainer container,
 			SessionDataBean sessionDataBean) throws SMException
 	{
 		Logger.out.debug("validateContainerAccess..................");
 		String userName = sessionDataBean.getUserName();
 		if (!SecurityManager.getInstance(this.getClass()).isAuthorized(userName,
 				StorageContainer.class.getName() + "_" + container.getId(), Permissions.USE))
 		{
 			return false;
 		}
 
 		else
 			return true;
 	}
 
 	// Checks for whether the user is trying to place the container in a
 	// position
 	// outside the range of parent container
 	// This is needed since now users can enter the values in the edit box
 	private boolean validatePosition(StorageContainer parent, StorageContainer current)
 	{
 		int posOneCapacity = parent.getCapacity().getOneDimensionCapacity().intValue();
 		int posTwoCapacity = parent.getCapacity().getTwoDimensionCapacity().intValue();
 
 		int positionDimensionOne = current.getPositionDimensionOne().intValue();
 		int positionDimensionTwo = current.getPositionDimensionTwo().intValue();
 
 		Logger.out.debug("validatePosition C : " + positionDimensionOne + " : "
 				+ positionDimensionTwo);
 		Logger.out.debug("validatePosition P : " + posOneCapacity + " : " + posTwoCapacity);
 
 		if ((positionDimensionOne > posOneCapacity) || (positionDimensionTwo > posTwoCapacity))
 		{
 			Logger.out.debug("validatePosition false");
 			return false;
 		}
 		Logger.out.debug("validatePosition true");
 		return true;
 	}
 
 	private boolean validatePosition(int posOneCapacity, int posTwoCapacity,
 			StorageContainer current)
 	{
 		int positionDimensionOne = current.getPositionDimensionOne().intValue();
 		int positionDimensionTwo = current.getPositionDimensionTwo().intValue();
 
 		Logger.out.debug("validatePosition C : " + positionDimensionOne + " : "
 				+ positionDimensionTwo);
 		Logger.out.debug("validatePosition P : " + posOneCapacity + " : " + posTwoCapacity);
 
 		if ((positionDimensionOne > posOneCapacity) || (positionDimensionTwo > posTwoCapacity))
 		{
 			Logger.out.debug("validatePosition false");
 			return false;
 		}
 		Logger.out.debug("validatePosition true");
 		return true;
 	}
 
 	private boolean isContainerAvailableForDisabled(DAO dao, Long[] containerIds)
 	{
 		List containerList = new ArrayList();
 		if (containerIds.length != 0)
 		{
 			try
 			{
 				String sourceObjectName = Specimen.class.getName();
 				String[] selectColumnName = {"id"};
 				String[] whereColumnName1 = {"storageContainer.id"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 				String[] whereColumnCondition1 = {"in"};
 				Object[] whereColumnValue1 = {containerIds};
 				String joinCondition = Constants.AND_JOIN_CONDITION;
 
 				List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName1,
 						whereColumnCondition1, whereColumnValue1, joinCondition);
 				// check if Specimen exists with the given storageContainer information
 				if (list.size() != 0)
 				{
 					Object obj = list.get(0);
 					return false;
 				}
 				else
 				{
 					sourceObjectName = SpecimenArray.class.getName();
 					list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName1,
 							whereColumnCondition1, whereColumnValue1, joinCondition);
 					// check if Specimen exists with the given storageContainer information
 					if (list.size() != 0)
 					{
 						return false;
 					}
 					else
 					{
 						sourceObjectName = StorageContainer.class.getName();
 						String[] whereColumnName = {"parent.id"};
 						containerList = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 								whereColumnCondition1, whereColumnValue1, joinCondition);
 						
 
 					}
 
 				}
 
 			}
 			catch (Exception e)
 			{
 				Logger.out.debug("Error in isContainerAvailable : " + e);
 				return false;
 			}
 		}
 		else
 		{
 			return true;
 		}
 
 		return isContainerAvailableForDisabled(dao, Utility.toLongArray(containerList));
 	}
 
 	// -- to check if storageContainer is available or used
 	private boolean isContainerAvailableForPositions(DAO dao, StorageContainer current)
 	{
 		try
 		{
 			String sourceObjectName = StorageContainer.class.getName();
 			String[] selectColumnName = {"id"};
 			String[] whereColumnName = {"positionDimensionOne", "positionDimensionTwo", "parent"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 			String[] whereColumnCondition = {"=", "=", "="};
 			Object[] whereColumnValue = {current.getPositionDimensionOne(),
 					current.getPositionDimensionTwo(), current.getParent()};
 			String joinCondition = Constants.AND_JOIN_CONDITION;
 
 			List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 					whereColumnCondition, whereColumnValue, joinCondition);
 			Logger.out.debug("current.getParentContainer() :" + current.getParent());
 			// check if StorageContainer exists with the given storageContainer information
 			if (list.size() != 0)
 			{
 				Object obj = list.get(0);
 				Logger.out.debug("**********IN isContainerAvailable : obj::::::: --------- " + obj);
 				return false;
 			}
 			else
 			{
 				sourceObjectName = Specimen.class.getName();
 				String[] whereColumnName1 = {"positionDimensionOne", "positionDimensionTwo",
 						"storageContainer.id"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 				String[] whereColumnCondition1 = {"=", "=", "="};
 				Object[] whereColumnValue1 = {current.getPositionDimensionOne(),
 						current.getPositionDimensionTwo(), current.getParent().getId()};
 
 				list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName1,
 						whereColumnCondition1, whereColumnValue1, joinCondition);
 				// check if Specimen exists with the given storageContainer information
 				if (list.size() != 0)
 				{
 					Object obj = list.get(0);
 					Logger.out
 							.debug("**************IN isPositionAvailable : obj::::::: --------------- "
 									+ obj);
 					return false;
 				}
 				else
 				{
 					sourceObjectName = SpecimenArray.class.getName();
 
 					list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName1,
 							whereColumnCondition1, whereColumnValue1, joinCondition);
 					// check if Specimen exists with the given storageContainer information
 					if (list.size() != 0)
 					{
 						Object obj = list.get(0);
 						Logger.out
 								.debug("**************IN isPositionAvailable : obj::::::: --------------- "
 										+ obj);
 						return false;
 					}
 				}
 
 			}
 
 			return true;
 		}
 		catch (Exception e)
 		{
 			Logger.out.debug("Error in isContainerAvailable : " + e);
 			return false;
 		}
 	}
 
 	//	 Will check only for valid range of the StorageContainer
 	protected boolean validatePosition(StorageContainer storageContainer, String posOne,
 			String posTwo)
 	{
 		try
 		{
 			Logger.out.debug("storageContainer.getPositionDimensionOne() : "
 					+ storageContainer.getPositionDimensionOne());
 			Logger.out.debug("storageContainer.getPositionDimensionTwo() : "
 					+ storageContainer.getPositionDimensionTwo());
 			int positionDimensionOne = (storageContainer.getPositionDimensionOne() != null
 					? storageContainer.getPositionDimensionOne().intValue()
 					: -1);
 			int positionDimensionTwo = (storageContainer.getPositionDimensionTwo() != null
 					? storageContainer.getPositionDimensionTwo().intValue()
 					: -1);
 			if (((positionDimensionOne) < Integer.parseInt(posOne))
 					|| ((positionDimensionTwo) < Integer.parseInt(posTwo)))
 			{
 				return false;
 			}
 			return true;
 		}
 		catch (Exception e)
 		{
 			Logger.out.debug("Error in validatePosition : " + e);
 			return false;
 		}
 	}
 
 	//  Will check only for Position is used or not.
 	protected boolean isPositionAvailable(DAO dao, StorageContainer storageContainer,
 			String posOne, String posTwo)
 	{
 		try
 		{
 			String sourceObjectName = Specimen.class.getName();
 			String[] selectColumnName = {"id"};
 			String[] whereColumnName = {"positionDimensionOne", "positionDimensionTwo",
 					"storageContainer.id"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 			String[] whereColumnCondition = {"=", "=", "="};
 			Object[] whereColumnValue = {posOne, posTwo, storageContainer.getId()};
 			String joinCondition = Constants.AND_JOIN_CONDITION;
 
 			List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 					whereColumnCondition, whereColumnValue, joinCondition);
 			Logger.out.debug("storageContainer.getId() :" + storageContainer.getId());
 			// check if Specimen exists with the given storageContainer information
 			if (list.size() != 0)
 			{
 				Object obj = list.get(0);
 				Logger.out
 						.debug("**************IN isPositionAvailable : obj::::::: --------------- "
 								+ obj);
 				//            	Logger.out.debug((Long)obj[0] );
 				//            	Logger.out.debug((Integer)obj[1]);
 				//            	Logger.out.debug((Integer )obj[2]);
 
 				return false;
 			}
 			else
 			{
 				sourceObjectName = StorageContainer.class.getName();
 				String[] whereColumnName1 = {"positionDimensionOne", "positionDimensionTwo",
 						"parent"}; //"storageContainer."+Constants.SYSTEM_IDENTIFIER
 				String[] whereColumnCondition1 = {"=", "=", "="};
 				Object[] whereColumnValue1 = {posOne, posTwo, storageContainer.getId()};
 
 				list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName1,
 						whereColumnCondition1, whereColumnValue1, joinCondition);
 				Logger.out.debug("storageContainer.getId() :" + storageContainer.getId());
 				// check if Specimen exists with the given storageContainer information
 				if (list.size() != 0)
 				{
 					Object obj = list.get(0);
 					Logger.out
 							.debug("**********IN isPositionAvailable : obj::::: --------- " + obj);
 					return false;
 				}
 				else
 				{
 					sourceObjectName = SpecimenArray.class.getName();
 					String[] whereColumnName2 = {"positionDimensionOne", "positionDimensionTwo",
 							"storageContainer.id"}; 
 					String[] whereColumnCondition2 = {"=", "=", "="};
 					Object[] whereColumnValue2 = {posOne, posTwo, storageContainer.getId()};
 
 					list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName2,
 							whereColumnCondition2, whereColumnValue2, joinCondition);
 					Logger.out.debug("storageContainer.getId() :" + storageContainer.getId());
 					// check if Specimen exists with the given storageContainer information
 					if (list.size() != 0)
 					{
 						Object obj = list.get(0);
 						Logger.out
 								.debug("**********IN isPositionAvailable : obj::::: --------- " + obj);
 						return false;
 					}
 				}
 			}
 			return true;
 		}
 		catch (Exception e)
 		{
 			Logger.out.debug("Error in isPositionAvailable : " + e);
 			return false;
 		}
 	}
 
 	//	 -- storage container validation for specimen
 	public void checkContainer(DAO dao, String storageContainerID, String positionOne,
 			String positionTwo, SessionDataBean sessionDataBean) throws DAOException, SMException
 	{
 		//        List list = dao.retrieve(StorageContainer.class.getName(),
 		//                "id",storageContainerID  );
 
 		String sourceObjectName = StorageContainer.class.getName();
 		String[] selectColumnName = {Constants.SYSTEM_IDENTIFIER, "capacity.oneDimensionCapacity",
 				"capacity.twoDimensionCapacity"};
 		String[] whereColumnName = {Constants.SYSTEM_IDENTIFIER};
 		String[] whereColumnCondition = {"="};
 		Object[] whereColumnValue = {storageContainerID};
 		String joinCondition = Constants.AND_JOIN_CONDITION;
 
 		List list = dao.retrieve(sourceObjectName, selectColumnName, whereColumnName,
 				whereColumnCondition, whereColumnValue, joinCondition);
 
 		// check if StorageContainer exists with the given ID
 		if (list.size() != 0)
 		{
 			Object[] obj = (Object[]) list.get(0);
 			Logger.out.debug("**********SC found for given ID ****obj::::::: --------------- "
 					+ obj);
 			Logger.out.debug((Long) obj[0]);
 			Logger.out.debug((Integer) obj[1]);
 			Logger.out.debug((Integer) obj[2]);
 
 			StorageContainer pc = new StorageContainer();
 			pc.setId((Long) obj[0]);
 
 			if (obj[1] != null)
 				pc.setPositionDimensionOne((Integer) obj[1]);
 			if (obj[2] != null)
 				pc.setPositionDimensionTwo((Integer) obj[2]);
 
 			//check if user has privilege to use the container
 			boolean hasAccess = validateContainerAccess(pc, sessionDataBean);
 			Logger.out.debug("hasAccess..............." + hasAccess);
 			if (!hasAccess)
 			{
 				throw new DAOException(ApplicationProperties.getValue("access.use.object.denied"));
 			}
 			// check for closed Container
 			checkStatus(dao, pc, "Storage Container");
 
 			// check for valid position
 			boolean isValidPosition = validatePosition(pc, positionOne, positionTwo);
 			Logger.out.debug("isValidPosition : " + isValidPosition);
 			if (isValidPosition) //	if position is valid 
 			{
 				boolean canUsePosition = isPositionAvailable(dao, pc, positionOne, positionTwo);
 				Logger.out.debug("canUsePosition : " + canUsePosition);
 				if (canUsePosition) // position empty. can be used 
 				{
 
 				}
 				else
 				// position already in use
 				{
 					throw new DAOException(ApplicationProperties
 							.getValue("errors.storageContainer.inUse"));
 				}
 			}
 			else
 			// position is invalid
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("errors.storageContainer.dimensionOverflow"));
 			}
 		}
 		else
 		//	storageContainer does not exist
 		{
 			throw new DAOException(ApplicationProperties.getValue("errors.storageContainerExist"));
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see edu.wustl.catissuecore.bizlogic.TreeDataInterface#getTreeViewData(edu.wustl.common.beans.SessionDataBean, java.util.Map)
 	 */
 	public Vector getTreeViewData(SessionDataBean sessionData, Map map, List list)
 			throws DAOException
 	{
 		return null;
 	}
 
 	/**
 	 * Overriding the parent class's method to validate the enumerated attribute values
 	 */
 	protected boolean validate(Object obj, DAO dao, String operation) throws DAOException
 	{
 		StorageContainer container = (StorageContainer) obj;
 		
 		//Added by Ashish
 		/*
 		String message = "";
 		if (container == null)
 			throw new DAOException("domain.object.null.err.msg", new String[]{"Storage Container"});
 		Validator validator = new Validator();
 		if (container.getStorageType().getId() == -1)
 		{
 			message = ApplicationProperties.getValue("storageContainer.type");
 			throw new DAOException("errors.item.required", new String[]{message});
 			
 		}
 		
 
 		if (container.getSite().getId() == -1 && container.getNoOfContainers() == 1)
 		{
 			message = ApplicationProperties.getValue("storageContainer.site");
 			throw new DAOException("errors.item.required", new String[]{message});
 			
 		}
 		 if (container.getNoOfContainers() == 1)
 		{
 			if (!validator.isNumeric(String.valueOf(container.getPositionDimensionOne()), 1)
 					|| !validator.isNumeric(String.valueOf(container.getPositionDimensionTwo()), 1)
 					|| !validator.isNumeric(String.valueOf(container.getParent().getId()), 1))
 			{
 				message = ApplicationProperties.getValue("storageContainer.parentContainer");
 				throw new DAOException("errors.item.format", new String[]{message});
 				
 			}
 			long longValue = Long.parseLong(String.valueOf(container.getNoOfContainers()));
             if (longValue <= 0)
             {
             	message = ApplicationProperties.getValue("storageContainer.noOfContainers");
 				throw new DAOException("errors.item.format", new String[]{message});            	
             }
 			
 		}			
 		
 		//validations for Container name
 		if (validator.isEmpty(container.getName()) && container.getNoOfContainers() == 1)
 		{
 			message = ApplicationProperties.getValue("storageContainer.name");
 			throw new DAOException("errors.item.required", new String[]{message});			
 		}
 
 		//validation for collection protocol
 		if (container.getCollectionProtocolCollection().size() > 1)
 		{
 			Iterator collectionProtocolIterator = container.getCollectionProtocolCollection().iterator();
 			while(collectionProtocolIterator.hasNext())
 			{
 				if (((Long)collectionProtocolIterator.next()).longValue() == -1)
 				{
 					message = ApplicationProperties.getValue("storageContainer.collectionProtocolTitle");
 					throw new DAOException("errors.item.format", new String[]{message});					
 				}
 			}
 		}
 
 		//validation for holds storage type
 		if (container.getHoldsStorageTypeCollection() != null && container.getHoldsStorageTypeCollection().size() > 1)
 		{
 			Iterator iter = container.getHoldsStorageTypeCollection().iterator();
 			while(iter.hasNext())
 			{
 				if (((StorageType)iter.next()).getId() == 1)
 				{
 					message = ApplicationProperties.getValue("storageContainer.containerType");
 					throw new DAOException("errors.item.format", new String[]{message});
 					
 				}
 			}
 		}
 		
 		//validation for holds specimen class
 		//new chnage checkValidSelectionForAny(holdsSpecimenClassTypeIds, "storageContainer.specimenType",
 		//		errors);
 
 		
 		// validations for temperature
 		if (!validator.isEmpty(container.getTempratureInCentigrade().toString())
 				&& (!validator.isDouble(container.getTempratureInCentigrade().toString(), false)))
 		{
 			message = ApplicationProperties.getValue("storageContainer.temperature");
 			throw new DAOException("errors.item.format", new String[]{message});
 			
 		}
 
 		//VALIDATIONS FOR DIMENSION 1.
 		if (validator.isEmpty(String.valueOf(container.getPositionDimensionOne())))
 		{
 			message = ApplicationProperties.getValue("storageContainer.oneDimension");
 			throw new DAOException("errors.item.required", new String[]{message});
 			
 		}
 		else
 		{
 			if (!validator.isNumeric(String.valueOf(container.getPositionDimensionOne())))
 			{
 				message = ApplicationProperties.getValue("storageContainer.oneDimension");
 				throw new DAOException("errors.item.format", new String[]{message});
 				
 			}
 		}
 
 		//Validations for dimension 2
 		if (!validator.isEmpty(String.valueOf(container.getPositionDimensionTwo()))
 				&& (!validator.isNumeric(String.valueOf(container.getPositionDimensionTwo()))))
 		{
 			message = ApplicationProperties.getValue("storageContainer.twoDimension");
 			throw new DAOException("errors.item.format", new String[]{message});
 			
 		}
 
 //		if (container.getNoOfContainers() > 1 && this.getSimilarContainersMap().size()>0)
 //		{
 //			for (int i = 1; i <= this.noOfContainers; i++)
 //			{
 //				String iBarcode = (String) this.getSimilarContainerMapValue("simCont:" + i + "_barcode"); //simCont:1_barcode
 //				if (iBarcode != null && iBarcode.equals("")) // this is done because barcode is empty string set by struts
 //				{ // but barcode in DB is unique but can be null.
 //					this.setSimilarContainerMapValue("simCont:" + i + "_barcode", null);
 //				}
 //				
 //				int checkedButtonStatus = Integer.parseInt((String) getSimilarContainerMapValue("checkedButton"));
 //				String siteId = (String) getSimilarContainerMapValue("simCont:" + i + "_siteId");
 //				if (checkedButtonStatus == 2 || siteId == null)
 //				{
 //					String parentContId = (String) getSimilarContainerMapValue("simCont:" + i
 //							+ "_parentContainerId");
 //					String positionDimensionOne = (String) getSimilarContainerMapValue("simCont:" + i
 //							+ "_positionDimensionOne");
 //					String positionDimensionTwo = (String) getSimilarContainerMapValue("simCont:" + i
 //							+ "_positionDimensionTwo");
 //					
 //					if (parentContId.equals("-1") || positionDimensionOne.equals("-1")
 //							|| positionDimensionTwo.equals("-1"))
 //					{
 //						message = ApplicationProperties.getValue("storageContainer.location");
 //						throw new DAOException("errors.item.required", new String[]{message});
 //						
 //					}
 //				}
 //				else
 //				{
 //					if (siteId.equals("-1"))
 //					{
 //						message = ApplicationProperties.getValue("storageContainer.site");
 //						throw new DAOException("errors.item.required", new String[]{message});
 //						
 //					}
 //				}
 //			}
 //		}
 		*/
 		//End
 		if (operation.equals(Constants.ADD))
 		{
 			if (!Constants.ACTIVITY_STATUS_ACTIVE.equals(container.getActivityStatus()))
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("activityStatus.active.errMsg"));
 			}
 
 			if (container.isFull().booleanValue())
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("storageContainer.isContainerFull.errMsg"));
 			}
 		}
 		else
 		{
 			if (!Validator.isEnumeratedValue(Constants.ACTIVITY_STATUS_VALUES, container
 					.getActivityStatus()))
 			{
 				throw new DAOException(ApplicationProperties.getValue("activityStatus.errMsg"));
 			}
 		}
 
 		return true;
 	}
 
 	// TODO Write the proper business logic to return an appropriate list of containers.
 	public List getStorageContainerList() throws DAOException
 	{
 		String sourceObjectName = StorageContainer.class.getName();
 		String[] displayNameFields = {Constants.SYSTEM_IDENTIFIER};
 		String valueField = Constants.SYSTEM_IDENTIFIER;
 
 		List list = getList(sourceObjectName, displayNameFields, valueField, true);
 		return list;
 	}
 
 	public List getCollectionProtocolList() throws DAOException
 	{
 		String sourceObjectName = CollectionProtocol.class.getName();
 		List returnList = new ArrayList();
 		NameValueBean nvb1 = new NameValueBean("--Any--", "-1");
 		returnList.add(nvb1);
 		List list = retrieve(sourceObjectName);
 		Iterator itr = list.iterator();
 		while (itr.hasNext())
 		{
 			CollectionProtocol collectionProtocol = (CollectionProtocol) itr.next();
 			NameValueBean nvb = new NameValueBean(collectionProtocol.getTitle(), collectionProtocol);
 			returnList.add(nvb);
 		}
 		return returnList;
 	}
 
 	/**
 	 * This functions returns a double dimensional boolean array which tells the availablity of 
 	 * storage positions of particular container. True - Available. False - Not Available.
 	 * @param container The container.
 	 * @return Returns a double dimensional boolean array of position availablity.
 	 * @throws DAOException
 	 */
 	public boolean[][] getAvailablePositions(StorageContainer container) throws DAOException
 	{
 		final int dimX = container.getCapacity().getOneDimensionCapacity().intValue() + 1;
 		final int dimY = container.getCapacity().getTwoDimensionCapacity().intValue() + 1;
 		boolean[][] positions = new boolean[dimX][dimY];
 
 		//Initializing the array
 		for (int i = 0; i < dimX; i++)
 		{
 			for (int j = 0; j < dimY; j++)
 			{
 				positions[i][j] = true;
 			}
 		}
 
 		//Retrieving all the occupied positions by child containers
 		String sourceObjectName = StorageContainer.class.getName();
 		String[] selectColumnName = {"positionDimensionOne", "positionDimensionTwo"};
 		String[] whereColumnName = {"parent"};
 		String[] whereColumnCondition = {"="};
 		Object[] whereColumnValue = {container.getId()};
 
 		List list = retrieve(sourceObjectName, selectColumnName, whereColumnName,
 				whereColumnCondition, whereColumnValue, null);
 
 		if (!list.isEmpty())
 		{
 			int x, y;
 
 			for (int i = 0; i < list.size(); i++)
 			{
 				Object[] object = (Object[]) list.get(i);
 				x = Integer.parseInt(object[0].toString());
 				y = Integer.parseInt(object[1].toString());
 
 				positions[x][y] = false;
 			}
 		}
 
 		//Retrieving all the occupied positions by specimens
 		sourceObjectName = Specimen.class.getName();
 		whereColumnName[0] = "storageContainer";
 
 		list = retrieve(sourceObjectName, selectColumnName, whereColumnName, whereColumnCondition,
 				whereColumnValue, null);
 
 		if (!list.isEmpty())
 		{
 			int x, y;
 
 			for (int i = 0; i < list.size(); i++)
 			{
 				Object[] object = (Object[]) list.get(i);
 				x = Integer.parseInt(object[0].toString());
 				y = Integer.parseInt(object[1].toString());
 
 				positions[x][y] = false;
 			}
 		}
 
 		//Retrieving all the occupied positions by specimens array type
 		sourceObjectName = SpecimenArray.class.getName();
 		whereColumnName[0] = "storageContainer";
 
 		list = retrieve(sourceObjectName, selectColumnName, whereColumnName, whereColumnCondition,
 				whereColumnValue, null);
 
 		if (!list.isEmpty())
 		{
 			int x, y;
 
 			for (int i = 0; i < list.size(); i++)
 			{
 				Object[] object = (Object[]) list.get(i);
 				x = Integer.parseInt(object[0].toString());
 				y = Integer.parseInt(object[1].toString());
 
 				positions[x][y] = false;
 			}
 		}
 
 		return positions;
 	}
 
 	/**
 	 * This functions returns a double dimensional boolean array which tells the availablity of 
 	 * storage positions of particular container. True - Available. False - Not Available. 
 	 * @param containerId The container identifier.
 	 * @return Returns a double dimensional boolean array of position availablity.
 	 * @throws DAOException
 	 */
 	public boolean[][] getAvailablePositions(String containerId) throws DAOException
 	{
 		List list = retrieve(StorageContainer.class.getName(), Constants.SYSTEM_IDENTIFIER,
 				new Long(containerId));
 
 		if (list != null)
 		{
 			StorageContainer container = (StorageContainer) list.get(0);
 			return getAvailablePositions(container);
 		}
 		else
 		{
 			return new boolean[0][0];
 		}
 	}
 
 	/**
 	 * This functions returns a map of available rows vs. available columns.
 	 * @param container The container.
 	 * @return Returns a map of available rows vs. available columns.
 	 * @throws DAOException
 	 */
 
 	public Map getAvailablePositionMap(StorageContainer container,int aliquotCount) throws DAOException
 	{
 		Map map = new TreeMap();
 		int count = 0;
 		if (!container.isFull().booleanValue())
 		{
 			boolean[][] availablePosistions = getAvailablePositions(container);
 
 			for (int x = 1; x < availablePosistions.length; x++)
 			{
 
 				List list = new ArrayList();
 
 				for (int y = 1; y < availablePosistions[x].length; y++)
 				{
 					if (availablePosistions[x][y])
 					{
 						list.add(new NameValueBean(new Integer(y), new Integer(y)));
 						count++;
 					}
 				}
 
 				if (!list.isEmpty())
 				{
 					Integer xObj = new Integer(x);
 					NameValueBean nvb = new NameValueBean(xObj, xObj);
 					map.put(nvb, list);
 
 				}
 			}
 		}
 		//Logger.out.info("Map :"+map);
 		if(count<aliquotCount) {
 			return new TreeMap();
 		}
 		return map;
 	}
 
 	/**
 	 * This functions returns a map of available rows vs. available columns.
 	 * @param containerId The container identifier.
 	 * @return Returns a map of available rows vs. available columns.
 	 * @throws DAOException
 	 */
 	public Map getAvailablePositionMap(String containerId,int aliquotCount) throws DAOException
 	{
 		List list = retrieve(StorageContainer.class.getName(), Constants.SYSTEM_IDENTIFIER,
 				new Long(containerId));
 
 		if (list != null)
 		{
 			StorageContainer container = (StorageContainer) list.get(0);
 			return getAvailablePositionMap(container,aliquotCount);
 		}
 		else
 		{
 			return new TreeMap();
 		}
 	}
 
 	/**
 	 * This functions returns a map of allocated containers vs. their respective free locations.
 	 * @return Returns a map of allocated containers vs. their respective free locations.
 	 * @throws DAOException
 	 */
 	public Map getAllocatedContainerMap() throws DAOException
 	{
 		/*
 		 A code snippet inside the commented block should actually be replaced by the
 		 code to get the allocated containers of specific collection protocol
 		 */
 
 		List list = retrieve(StorageContainer.class.getName());
 		Map containerMap = new TreeMap();
 		//Logger.out.info("===================== list size:"+list.size());
 		Iterator itr = list.iterator();
 		while (itr.hasNext())
 		{
 			StorageContainer container = (StorageContainer) itr.next();
 			//Logger.out.info("+++++++++++++++++++++++++++:"+container.getName()+"++++++++++:"+container.getId());
 			Map positionMap = getAvailablePositionMap(container.getId().toString(),0);
 
 			if (!positionMap.isEmpty())
 			{
 				//Logger.out.info("---------"+container.getName()+"------"+container.getId());
 				NameValueBean nvb = new NameValueBean(container.getName(), container.getId());
 				containerMap.put(nvb, positionMap);
 
 			}
 		}
 
 		return containerMap;
 	}
 
 	private void loadSiteFromContainerId(DAO dao, StorageContainer container) throws DAOException
 	{
 		if (container != null)
 		{
 			Long sysId = container.getId();
 			List siteIdList = dao.retrieve(StorageContainer.class.getName(),
 					Constants.SYSTEM_IDENTIFIER, sysId);
 			System.out.println("siteIdList " + siteIdList);
 			StorageContainer sc = (StorageContainer) siteIdList.get(0);
 			System.out.println("siteId " + sc.getSite().getId());
 			container.setSite(sc.getSite());
 		}
 	}
 
 	public List getAllocatedContaienrMapForContainer(long type_id) throws DAOException
 	{
 		List mapSiteList = new ArrayList();
 		//		List list = retrieve(StorageContainer.class.getName());
 		Map containerMap = new TreeMap();
 		List siteList = new ArrayList();
 		siteList.add(new NameValueBean("---", "---"));
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		dao.openSession(null);
 
 		String queryStr = "SELECT t1.IDENTIFIER, t1.NAME, t2.NAME FROM CATISSUE_CONTAINER t1, CATISSUE_SITE t2 ,CATISSUE_STORAGE_CONTAINER t3 WHERE "
 				+ "t1.IDENTIFIER IN (SELECT t4.STORAGE_CONTAINER_ID FROM CATISSUE_ST_CONT_ST_TYPE_REL t4 "
 				+ "WHERE t4.STORAGE_TYPE_ID = '"
 				+ type_id
 				+ "' OR t4.STORAGE_TYPE_ID='1') and t1.IDENTIFIER = t3.IDENTIFIER and t2.IDENTIFIER=t3.SITE_ID AND "
 				+ "t1.ACTIVITY_STATUS='"+Constants.ACTIVITY_STATUS_ACTIVE+"'";
 
 		Logger.out.debug("Storage Container query......................" + queryStr);
 		List list = new ArrayList();
 
 		try
 		{
 			list = dao.executeQuery(queryStr, null, false, null);
 		}
 		catch (Exception ex)
 		{
 			throw new DAOException(ex.getMessage());
 		}
 
 		dao.closeSession();
 		Logger.out.info("Size of list:" + list.size());
 
 		Iterator itr = list.iterator();
 		while (itr.hasNext())
 		{
 			List list1 = (List) itr.next();
 			String Id = (String) list1.get(0);
 			String Name = (String) list1.get(1);
 			String siteName = (String) list1.get(2);
 			Map positionMap = getAvailablePositionMap(Id,0);
 			if (!positionMap.isEmpty())
 			{
 				NameValueBean nvb = new NameValueBean(Name, Id);
 				containerMap.put(nvb, positionMap);
 				siteList.add(new NameValueBean(siteName, Id));
 			}
 		}
 
 		mapSiteList.add(containerMap);
 		mapSiteList.add(siteList);
 		return mapSiteList;
 
 	}
 
 	/* temp function end */
 
 	public Map getAllocatedContaienrMapForSpecimen(long cpId, String specimenClass,int aliquotCount)
 			throws DAOException
 	{
 		Map containerMap = new TreeMap();
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		dao.openSession(null);
 
 		String queryStr = "(SELECT t1.IDENTIFIER, t1.NAME FROM CATISSUE_CONTAINER t1 WHERE "
 				+ "t1.IDENTIFIER IN (SELECT t2.STORAGE_CONTAINER_ID FROM CATISSUE_ST_CONT_COLL_PROT_REL t2 "
 				+ "WHERE t2.COLLECTION_PROTOCOL_ID = '"
 				+ cpId
 				+ "') and t1.IDENTIFIER IN "
 				+ "(SELECT t3.STORAGE_CONTAINER_ID FROM CATISSUE_STOR_CONT_SPEC_CLASS t3 WHERE "
 				+ "t3.SPECIMEN_CLASS = '"
 				+ specimenClass
 				+ "') AND t1.ACTIVITY_STATUS='Active') UNION "
 				+ "(SELECT t4.IDENTIFIER, t4.NAME FROM CATISSUE_CONTAINER t4 WHERE "
 				+ "t4.IDENTIFIER NOT IN (SELECT t5.STORAGE_CONTAINER_ID FROM CATISSUE_ST_CONT_COLL_PROT_REL t5) "
 				+ " and t4.IDENTIFIER IN "
 				+ "(SELECT t6.STORAGE_CONTAINER_ID FROM CATISSUE_STOR_CONT_SPEC_CLASS t6 WHERE "
 				+ "t6.SPECIMEN_CLASS = '" + specimenClass + "') AND t4.ACTIVITY_STATUS='Active')";
 
 		Logger.out.debug("Storage Container query......................" + queryStr);
 		List list = new ArrayList();
 
 		try
 		{
 			list = dao.executeQuery(queryStr, null, false, null);
 		}
 		catch (Exception ex)
 		{
 			throw new DAOException(ex.getMessage());
 		}
 
 		dao.closeSession();
 		Logger.out.info("Size of list:" + list.size());
 		Iterator itr = list.iterator();
 		while (itr.hasNext())
 		{
 			List list1 = (List) itr.next();
 			String Id = (String) list1.get(0);
 			String Name = (String) list1.get(1);
 			Map positionMap = getAvailablePositionMap(Id,aliquotCount);
 			if (!positionMap.isEmpty())
 			{
 				NameValueBean nvb = new NameValueBean(Name, Id);
 				containerMap.put(nvb, positionMap);
 
 			}
 		}
 		Logger.out.info("Size of containerMap:" + containerMap.size());
 		return containerMap;
 
 	}
 
 	/**
 	 * Gets allocated container map for specimen array.
 	 * @param specimen_array_type_id specimen array type id
 	 * @param noOfAliqoutes No. of aliquotes
 	 * @return container map 
 	 * @throws DAOException -- throws DAO Exception
 	 * @see edu.wustl.common.dao.JDBCDAOImpl
 	 */
 	public Map getAllocatedContaienrMapForSpecimenArray(long specimen_array_type_id,int noOfAliqoutes) throws DAOException
 	{
 		Map containerMap = new TreeMap();
 		
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		dao.openSession(null);
 		String includeAllIdQueryStr =  " OR t4.SPECIMEN_ARRAY_TYPE_ID = '" + Constants.ARRAY_TYPE_ALL_ID + "'";
 		
 		if (!(new Validator().isValidOption(String.valueOf(specimen_array_type_id))))
 		{
 			includeAllIdQueryStr ="";
 		}
 		String queryStr = "select t1.IDENTIFIER,t1.name from CATISSUE_CONTAINER t1,CATISSUE_STORAGE_CONTAINER t2 " +
 						  "where t1.IDENTIFIER IN (select t4.STORAGE_CONTAINER_ID from CATISSUE_CONT_HOLDS_SPARRTYPE t4 " +
 						  "where t4.SPECIMEN_ARRAY_TYPE_ID = '" + specimen_array_type_id + "'" + includeAllIdQueryStr + ") and t1.IDENTIFIER = t2.IDENTIFIER";
 		
 		Logger.out.debug("SPECIMEN ARRAY QUERY ......................" + queryStr);
 		List list = new ArrayList();
 
 		try
 		{
 			list = dao.executeQuery(queryStr, null, false, null);
 		}
 		catch (Exception ex)
 		{
 			throw new DAOException(ex.getMessage());
 		}
 
 		dao.closeSession();
 		Logger.out.info("Size of list:"+list.size());
 
 		Iterator itr = list.iterator();
 		while (itr.hasNext())
 		{
 			List list1 = (List)itr.next();
 			String Id = (String)list1.get(0);
 			String Name = (String)list1.get(1);
 			Map positionMap = getAvailablePositionMap(Id,noOfAliqoutes);
 			if (!positionMap.isEmpty())
 			{
 				NameValueBean nvb = new NameValueBean(Name,Id);
 				containerMap.put(nvb, positionMap);
 			}
 		}
 		return containerMap;
 	}
 	
 	//--------------Code for Map Mandar: 04-Sep-06 start
 	//Mandar : 29Aug06 : for StorageContainerMap
 	/**
 	 * @param id Identifier of the StorageContainer related to which the collectionProtocol titles are to be retrieved. 
 	 * @return List of collectionProtocol title.
 	 * @throws DAOException
 	 */
 	public List getCollectionProtocolList(String id) throws DAOException
 	{
 
 		// Query to return titles of collection protocol related to given storagecontainer. 29-Aug-06 Mandar.
 		String sql = " SELECT SP.TITLE TITLE FROM CATISSUE_SPECIMEN_PROTOCOL SP, CATISSUE_ST_CONT_COLL_PROT_REL SC "
 				+ " WHERE SP.IDENTIFIER = SC.COLLECTION_PROTOCOL_ID AND SC.STORAGE_CONTAINER_ID = "
 				+ id;
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		List resultList = new ArrayList();
 		try
 		{
 			dao.openSession(null);
 			resultList = dao.executeQuery(sql, null, false, null);
 			dao.closeSession();
 		}
 		catch (Exception daoExp)
 		{
 			throw new DAOException(daoExp.getMessage(), daoExp);
 		}
 
 		Iterator iterator = resultList.iterator();
 		System.out.println("\nCollectionProtocol :");
 		List returnList = new ArrayList();
 		while (iterator.hasNext())
 		{
 			List list = (List) iterator.next();
 			String data = (String) list.get(0);
 			returnList.add(data);
 			System.out.println(data);
 		}
 
 		if (returnList.isEmpty())
 		{
 			returnList.add(new String(Constants.ALL));
 		}
 		return returnList;
 	}
 
 	/**
 	 * @param id Identifier of the StorageContainer related to which the collectionProtocol titles are to be retrieved. 
 	 * @return List of collectionProtocol title.
 	 * @throws DAOException
 	 */
 	public List getSpecimenClassList(String id) throws DAOException
 	{
 
 		// Query to return specimen classes related to given storagecontainer. 29-Aug-06 Mandar.
 		String sql = " SELECT SP.SPECIMEN_CLASS CLASS FROM CATISSUE_STOR_CONT_SPEC_CLASS SP "
 				+ "WHERE SP.STORAGE_CONTAINER_ID = " + id;
 
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		List resultList = new ArrayList();
 		try
 		{
 			dao.openSession(null);
 			resultList = dao.executeQuery(sql, null, false, null);
 			dao.closeSession();
 		}
 		catch (Exception daoExp)
 		{
 			throw new DAOException(daoExp.getMessage(), daoExp);
 		}
 
 		Iterator iterator = resultList.iterator();
 		System.out.println("\nSpecimenClass :");
 		List returnList = new ArrayList();
 		while (iterator.hasNext())
 		{
 			List list = (List) iterator.next();
 			for (int cnt = 0; cnt < list.size(); cnt++)
 			{
 				String data = (String) list.get(cnt);
 				returnList.add(data);
 				System.out.println(data);
 			}
 		}
 		if (returnList.isEmpty())
 		{
 			returnList.add(new String(Constants.ALL));
 		}
 		return returnList;
 	}
 
 	//prints results returned from DAO executeQuery  To comment after debug 
 	private void printRecords(List list)
 	{
 		if (list != null)
 		{
 			if (!list.isEmpty())
 			{
 				System.out.println("OuterList Size : " + list.size());
 				for (int i = 0; i < list.size(); i++)
 				{
 					List innerList = (List) list.get(i);
 					System.out.println("\nInnerList Size : " + innerList.size() + "\n");
 					String s = "";
 					for (int j = 0; j < innerList.size(); j++)
 					{
 						String s1 = (String) innerList.get(j);
 						s = s + " | " + s1;
 					}
 					System.out.print(s);
 				}
 			}
 		}
 	}
 
 	// Method to print the relationMap and treeNode vector. To delete after debug
 	private void printVectorMap(Vector v, Map m)
 	{
 		System.out.println("\n");
 		System.out.println("\nVector Data\n");
 		Iterator itr = v.iterator();
 		while (itr.hasNext())
 		{
 			TreeNodeImpl obj = (TreeNodeImpl) itr.next();
 			System.out.println(obj);
 		}
 		System.out.println("\n-------------------\n");
 		System.out.println("\nMap\n");
 		Iterator key = m.keySet().iterator();
 		while (key.hasNext())
 		{
 			Long k = (Long) key.next();
 			Object val = m.get(k);
 			System.out.println(k + " : " + val.toString());
 		}
 	}
 
 	//Method to fetch ToolTipData for a given Container
 	private String getToolTipData(String containerID) throws DAOException
 	{
 		String toolTipData = "";
 
 		List specimenClassList = getSpecimenClassList(containerID);
 
 		String classData = "SpecimenClass";
 		for (int counter = 0; counter < specimenClassList.size(); counter++)
 		{
 			String data = (String) specimenClassList.get(counter);
 			classData = classData + " | " + data;
 		}
 
 		List collectionProtocolList = getCollectionProtocolList(containerID);
 
 		String protocolData = "CollectionProtocol";
 		for (int cnt = 0; cnt < collectionProtocolList.size(); cnt++)
 		{
 			String data = (String) collectionProtocolList.get(cnt);
 			protocolData = protocolData + " | " + data;
 		}
 
 		toolTipData = protocolData + "\n" + classData;
 		System.out.println(toolTipData);
 
 		return toolTipData;
 	}
 
 	//--------------Code for Map Mandar: 04-Sep-06 end	
 
 }
