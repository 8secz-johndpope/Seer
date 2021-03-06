 /**
  * <p>Title: DistributionHDAO Class>
  * <p>Description:	DistributionHDAO is used to add distribution information into the database using Hibernate.</p>
  * Copyright:    Copyright (c) year
  * Company: Washington University, School of Medicine, St. Louis.
  * @author Aniruddha Phadnis
  * @version 1.00
  * Created on Aug 23, 2005
  */
 
 package edu.wustl.catissuecore.bizlogic;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import net.sf.hibernate.HibernateException;
 import edu.wustl.catissuecore.domain.CellSpecimen;
 import edu.wustl.catissuecore.domain.DistributedItem;
 import edu.wustl.catissuecore.domain.Distribution;
 import edu.wustl.catissuecore.domain.FluidSpecimen;
 import edu.wustl.catissuecore.domain.MolecularSpecimen;
 import edu.wustl.catissuecore.domain.QuantityInCount;
 import edu.wustl.catissuecore.domain.QuantityInGram;
 import edu.wustl.catissuecore.domain.QuantityInMicrogram;
 import edu.wustl.catissuecore.domain.QuantityInMilliliter;
 import edu.wustl.catissuecore.domain.Site;
 import edu.wustl.catissuecore.domain.Specimen;
 import edu.wustl.catissuecore.domain.SpecimenArray;
 import edu.wustl.catissuecore.domain.TissueSpecimen;
 import edu.wustl.catissuecore.domain.User;
 import edu.wustl.catissuecore.util.global.Constants;
 import edu.wustl.catissuecore.util.global.Utility;
 import edu.wustl.common.beans.SessionDataBean;
 import edu.wustl.common.bizlogic.DefaultBizLogic;
 import edu.wustl.common.dao.DAO;
 import edu.wustl.common.dao.DAOFactory;
 import edu.wustl.common.dao.JDBCDAO;
 import edu.wustl.common.domain.AbstractDomainObject;
 import edu.wustl.common.security.SecurityManager;
 import edu.wustl.common.security.exceptions.SMException;
 import edu.wustl.common.security.exceptions.UserNotAuthorizedException;
 import edu.wustl.common.util.dbManager.DAOException;
 import edu.wustl.common.util.global.ApplicationProperties;
 import edu.wustl.common.util.global.Validator;
 import edu.wustl.common.util.logger.Logger;
 
 /**
  * DistributionHDAO is used to add distribution information into the database using Hibernate.
  * @author aniruddha_phadnis
  */
 public class DistributionBizLogic extends DefaultBizLogic
 {
 
 	/**
 	 * Saves the Distribution object in the database.
 	 * @param obj The storageType object to be saved.
 	 * @param session The session in which the object is saved.
 	 * @throws DAOException 
 	 */
 	protected void insert(Object obj, DAO dao, SessionDataBean sessionDataBean)
 			throws DAOException, UserNotAuthorizedException
 	{
 		Distribution dist = (Distribution) obj;
 
 		//Load & set the User
 		Object userObj = dao.retrieve(User.class.getName(), dist.getUser().getId());
 		if (userObj != null)
 		{
 			User user = (User) userObj;
 			dist.setUser(user);
 		}
 
 		//Load & set From Site
 		//		Object siteObj = dao.retrieve(Site.class.getName(), dist.getFromSite().getId());
 		//		if(siteObj!=null)
 		//		{
 		//			Site site = (Site)siteObj;
 		//			dist.setFromSite(site);
 		//		}
 
 		//Load & set the To Site
 		Object siteObj = dao.retrieve(Site.class.getName(), dist.getToSite().getId());
 		if (siteObj != null)
 		{
 			Site site = (Site) siteObj;
 			dist.setToSite(site);
 		}
 
 		dao.insert(dist, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 				Constants.IS_SECURE_UPDATE_TRUE);
 		Collection distributedItemCollection = dist.getDistributedItemCollection();
 
 		if (distributedItemCollection.size() != 0)
 		{
 			insertDistributedItem(dist, dao, sessionDataBean, distributedItemCollection);
 		}
 
 		try
 		{
 			SecurityManager.getInstance(this.getClass()).insertAuthorizationData(null,
 					getProtectionObjects(dist), getDynamicGroups(dist));
 		}
 		catch (SMException e)
 		{
 			throw handleSMException(e);
 		}
 	}
 
 	public boolean isSpecimenArrayDistributed(Long specimenArrayId) throws DAOException
 	{
 		boolean distributed = true;
 		JDBCDAO dao = (JDBCDAO) DAOFactory.getInstance().getDAO(Constants.JDBC_DAO);
 		List list = null;
 		String queryStr = "select array.distribution_id from catissue_specimen_array array where array.identifier ="
 				+ specimenArrayId;
 
 		try
 		{
 			dao.openSession(null);
 			list = dao.executeQuery(queryStr, null, false, null);
 			String distributionId = (String) ((List) list.get(0)).get(0);
 			if (distributionId.equals(""))
 			{
 				distributed = false;
 			}
 			dao.closeSession();
 		}
 		catch (Exception ex)
 		{
 			throw new DAOException(ex.getMessage());
 		}
 		return distributed;
 	}
 
 	private void insertDistributedItem(Distribution dist, DAO dao, SessionDataBean sessionDataBean,
 			Collection distributedItemCollection) throws DAOException, UserNotAuthorizedException
 	{
 
 		Iterator it = distributedItemCollection.iterator();
 
 		while (it.hasNext())
 		{
 			DistributedItem item = (DistributedItem) it.next();
 			//update the available quantity
 			Object specimenObj = dao.retrieve(Specimen.class.getName(), item.getSpecimen().getId());
 			double quantity = item.getQuantity().doubleValue();
 			boolean availability = checkAvailableQty((Specimen) specimenObj, quantity);
 			if (!availability)
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("errors.distribution.quantity"));
 			}
 			else
 			{
 				dao
 						.update(specimenObj, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 								Constants.IS_SECURE_UPDATE_TRUE,
 								Constants.HAS_OBJECT_LEVEL_PRIVILEGE_FALSE);
 			}
 			item.setDistribution(dist);
 			dao.insert(item, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 					Constants.IS_SECURE_UPDATE_TRUE);
 		}
 	}
 
 	private Set getProtectionObjects(AbstractDomainObject obj)
 	{
 		Set protectionObjects = new HashSet();
 
 		Distribution distribution = (Distribution) obj;
 		protectionObjects.add(distribution);
 
 		Iterator distributedItemIterator = distribution.getDistributedItemCollection().iterator();
 		while (distributedItemIterator.hasNext())
 		{
 			DistributedItem distributedItem = (DistributedItem) distributedItemIterator.next();
 			protectionObjects.add(distributedItem.getSpecimen());
 		}
 
 		return protectionObjects;
 	}
 
 	private String[] getDynamicGroups(AbstractDomainObject obj)
 	{
 		Distribution distribution = (Distribution) obj;
 		String[] dynamicGroups = new String[1];
 		dynamicGroups[0] = Constants.getDistributionProtocolPGName(distribution
 				.getDistributionProtocol().getId());
 
 		return dynamicGroups;
 	}
 
 	/**
 	 * Updates the persistent object in the database.
 	 * @param obj The object to be updated.
 	 * @param session The session in which the object is saved.
 	 * @throws DAOException 
 	 * @throws HibernateException Exception thrown during hibernate operations.
 	 */
 	protected void update(DAO dao, Object obj, Object oldObj, SessionDataBean sessionDataBean)
 			throws DAOException, UserNotAuthorizedException
 	{
 		Distribution distribution = (Distribution) obj;
 		dao.update(obj, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 				Constants.IS_SECURE_UPDATE_TRUE, Constants.HAS_OBJECT_LEVEL_PRIVILEGE_FALSE);
 
 		//Audit of Distribution.
 		dao.audit(obj, oldObj, sessionDataBean, Constants.IS_AUDITABLE_TRUE);
 
 		Distribution oldDistribution = (Distribution) oldObj;
 		Collection oldDistributedItemCollection = oldDistribution.getDistributedItemCollection();
 
 		Collection distributedItemCollection = distribution.getDistributedItemCollection();
 		Iterator it = distributedItemCollection.iterator();
 		while (it.hasNext())
 		{
 			DistributedItem item = (DistributedItem) it.next();
 
 			DistributedItem oldItem = (DistributedItem) getCorrespondingOldObject(
 					oldDistributedItemCollection, item.getId());
 			//update the available quantity
 			Object specimenObj = dao.retrieve(Specimen.class.getName(), item.getSpecimen().getId());
 			Double previousQuantity = (Double) item.getPreviousQuantity();
 			double quantity = item.getQuantity().doubleValue();
 			if (previousQuantity != null)
 			{
 				quantity = quantity - previousQuantity.doubleValue();
 			}
 			boolean availability = checkAvailableQty((Specimen) specimenObj, quantity);
 
 			if (!availability)
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("errors.distribution.quantity"));
 			}
 			else
 			{
 				dao
 						.update(specimenObj, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 								Constants.IS_SECURE_UPDATE_TRUE,
 								Constants.HAS_OBJECT_LEVEL_PRIVILEGE_FALSE);
 				//Audit of Specimen.
 				//If a new specimen is distributed.
 				if (oldItem == null)
 				{
 					Object specimenObjPrev = dao.retrieve(Specimen.class.getName(), item
 							.getSpecimen().getId());
 					dao.audit(specimenObj, specimenObjPrev, sessionDataBean,
 							Constants.IS_AUDITABLE_TRUE);
 				}
 				//if a distributed specimen is updated  
 				else
 					dao.audit(specimenObj, oldItem.getSpecimen(), sessionDataBean,
 							Constants.IS_AUDITABLE_TRUE);
 			}
 			item.setDistribution(distribution);
 
 			dao.update(item, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 					Constants.IS_SECURE_UPDATE_TRUE, Constants.HAS_OBJECT_LEVEL_PRIVILEGE_FALSE);
 
 			//Audit of Distributed Item.
 			dao.audit(item, oldItem, sessionDataBean, Constants.IS_AUDITABLE_TRUE);
 		}
 		//Mandar : 04-Apr-06 for updating the removed specimens start
 		updateRemovedSpecimens(distributedItemCollection, oldDistributedItemCollection, dao,
 				sessionDataBean);
 		Logger.out.debug("Update Successful ...04-Apr-06");
 		// Mandar : 04-Apr-06 end
 	}
 
 	private boolean checkAvailableQty(Specimen specimen, double quantity)
 	{
 		if (specimen instanceof TissueSpecimen)
 		{
 			TissueSpecimen tissueSpecimen = (TissueSpecimen) specimen;
 			double availabeQty = Double.parseDouble(tissueSpecimen.getAvailableQuantity()
 					.toString());//tissueSpecimen.getAvailableQuantityInGram().doubleValue();
 			Logger.out.debug("TissueAvailabeQty" + availabeQty);
 			if (quantity > availabeQty)
 				return false;
 			else
 			{
 				availabeQty = availabeQty - quantity;
 				Logger.out.debug("TissueAvailabeQty after deduction" + availabeQty);
 				tissueSpecimen.setAvailableQuantity(new QuantityInGram(availabeQty));//tissueSpecimen.setAvailableQuantityInGram(new Double(availabeQty));
 			}
 		}
 		else if (specimen instanceof CellSpecimen)
 		{
 			CellSpecimen cellSpecimen = (CellSpecimen) specimen;
 			int availabeQty = Integer.parseInt(cellSpecimen.getAvailableQuantity().toString());//cellSpecimen.getAvailableQuantityInCellCount().intValue();
 			if (quantity > availabeQty)
 				return false;
 			else
 			{
 				availabeQty = availabeQty - (int) quantity;
 				cellSpecimen.setAvailableQuantity(new QuantityInCount(availabeQty));//cellSpecimen.setAvailableQuantityInCellCount(new Integer(availabeQty));
 			}
 		}
 		else if (specimen instanceof MolecularSpecimen)
 		{
 			MolecularSpecimen molecularSpecimen = (MolecularSpecimen) specimen;
 			double availabeQty = Double.parseDouble(molecularSpecimen.getAvailableQuantity().getValue()
 					.toString());//molecularSpecimen.getAvailableQuantityInMicrogram().doubleValue();
 			if (quantity > availabeQty)
 				return false;
 			else
 			{
 				availabeQty = availabeQty - quantity;
 				molecularSpecimen.setAvailableQuantity(new QuantityInMicrogram(availabeQty));//molecularSpecimen.setAvailableQuantityInMicrogram(new Double(availabeQty));
 			}
 		}
 		else if (specimen instanceof FluidSpecimen)
 		{
 			FluidSpecimen fluidSpecimen = (FluidSpecimen) specimen;
 			double availabeQty = Double
 					.parseDouble(fluidSpecimen.getAvailableQuantity().toString());//fluidSpecimen.getAvailableQuantityInMilliliter().doubleValue();
 			if (quantity > availabeQty)
 				return false;
 			else
 			{
 				availabeQty = availabeQty - quantity;
 				fluidSpecimen.setAvailableQuantity(new QuantityInMilliliter(availabeQty));//fluidSpecimen.setAvailableQuantityInMilliliter(new Double(availabeQty));
 			}
 		}
 		return true;
 
 	}
 
 	public void disableRelatedObjects(DAO dao, Long distributionProtocolIDArr[])
 			throws DAOException
 	{
 		List listOfSubElement = super.disableObjects(dao, Distribution.class,
 				"distributionProtocol", "CATISSUE_DISTRIBUTION", "DISTRIBUTION_PROTOCOL_ID",
 				distributionProtocolIDArr);
 	}
 
 	/**
 	 * @see edu.wustl.common.bizlogic.IBizLogic#setPrivilege(DAO, String, Class, Long[], Long, String, boolean)
 	 * @param dao
 	 * @param privilegeName
 	 * @param objectIds
 	 * @param userId
 	 * @param roleId
 	 * @param assignToUser
 	 * @throws SMException
 	 * @throws DAOException
 	 */
 	public void assignPrivilegeToRelatedObjectsForDP(DAO dao, String privilegeName,
 			Long[] objectIds, Long userId, String roleId, boolean assignToUser,
 			boolean assignOperation) throws SMException, DAOException
 	{
 		List listOfSubElement = super.getRelatedObjects(dao, Distribution.class,
 				"distributionProtocol", objectIds);
 		Logger.out.debug("Distribution................" + listOfSubElement.size());
 		if (!listOfSubElement.isEmpty())
 		{
 			Logger.out.debug("Distribution Id : ................" + listOfSubElement.get(0));
 			super.setPrivilege(dao, privilegeName, Distribution.class, Utility
 					.toLongArray(listOfSubElement), userId, roleId, assignToUser, assignOperation);
 
 			assignPrivilegeToRelatedObjectsForDistribution(dao, privilegeName, Utility
 					.toLongArray(listOfSubElement), userId, roleId, assignToUser, assignOperation);
 		}
 	}
 
 	public void assignPrivilegeToRelatedObjectsForDistribution(DAO dao, String privilegeName,
 			Long[] objectIds, Long userId, String roleId, boolean assignToUser,
 			boolean assignOperation) throws SMException, DAOException
 	{
 		List listOfSubElement = super.getRelatedObjects(dao, DistributedItem.class, "distribution",
 				objectIds);
 		Logger.out.debug("Distributed Item................" + listOfSubElement.size());
 		if (!listOfSubElement.isEmpty())
 		{
 			Logger.out.debug("Distribution Item Id : ................" + listOfSubElement.get(0));
 			super.setPrivilege(dao, privilegeName, DistributedItem.class, Utility
 					.toLongArray(listOfSubElement), userId, roleId, assignToUser, assignOperation);
 
 			NewSpecimenBizLogic bizLogic = (NewSpecimenBizLogic) BizLogicFactory.getInstance()
 					.getBizLogic(Constants.NEW_SPECIMEN_FORM_ID);
 			bizLogic.assignPrivilegeToRelatedObjectsForDistributedItem(dao, privilegeName, Utility
 					.toLongArray(listOfSubElement), userId, roleId, assignToUser, assignOperation);
 		}
 
 	}
 
 	/**
 	 * Overriding the parent class's method to validate the enumerated attribute values
 	 */
 	protected boolean validate(Object obj, DAO dao, String operation) throws DAOException
 	{
 		Distribution distribution = (Distribution) obj;
 
 		//Added By Ashish
 /*		if (distribution == null)
 			throw new DAOException("domain.object.null.err.msg", new String[]{"Distribution"});
 		Validator validator = new Validator();
 		if (!validator.isValidOption(distribution.getDistributionProtocol().getId().toString()))
 		{
 			Logger.out.debug("dist prot");
 			String message = ApplicationProperties.getValue("distribution.protocol");
 			throw new DAOException("errors.item.required", new String[]{message});
 		}
 
 		if (!validator.isValidOption("" + distribution.getUser().getId()))
 		{
 			String message = ApplicationProperties.getValue("distribution.distributedBy");
 			throw new DAOException("errors.item.required", new String[]{message});
 		}
 
 		//  date validation 
 		String errorKey = validator.validateDate(Utility.parseDateToString(distribution
 				.getTimestamp(), Constants.DATE_PATTERN_MM_DD_YYYY), true);
 		if (errorKey.trim().length() > 0)
 		{
 			String message = ApplicationProperties.getValue("eventparameters.dateofevent");
 			throw new DAOException(errorKey, new String[]{message});
 
 		}
 
 		if (!validator.isValidOption(distribution.getToSite().getId().toString()))
 		{
 			Logger.out.debug("to site");
 			String message = ApplicationProperties.getValue("distribution.toSite");
 			throw new DAOException("errors.item.required", new String[]{message});
 
 		}
 
 		//Validations for Add-More Block
 		Map values = null;
 		int counter = 0;
 		if (distribution.getDistributedItemCollection() != null)
 		{
 			values = new HashMap();
 
 			Iterator it = distribution.getDistributedItemCollection().iterator();
 			int i = 1;
 
 			while (it.hasNext())
 			{
 				String key1 = "DistributedItem:" + i + "_id";
 				String key2 = "DistributedItem:" + i + "_Specimen_id";
 				String key3 = "DistributedItem:" + i + "_quantity";
 				String key9 = "DistributedItem:" + i + "_availableQty";
 				String key10 = "DistributedItem:" + i + "_previousQuantity";
 				String key12 = "DistributedItem:" + i + "_Specimen_barcode";
 				String key13 = "DistributedItem:" + i + "_Specimen_label";
 
 				DistributedItem dItem = (DistributedItem) it.next();
 				Specimen specimen = dItem.getSpecimen();
 
 				Double quantity = dItem.getQuantity();
 				//dItem.setPreviousQty(quantity);
 
 				values.put(key1, Utility.toString(dItem.getId()));
 				values.put(key2, Utility.toString(specimen.getId()));
 				values.put(key3, quantity.toString());
 				//			values.put(key9,getAvailableQty(specimen));
 				values.put(key10, quantity.toString());
 				values.put(key12, specimen.getBarcode());
 				values.put(key13, specimen.getLabel());
 				i++;
 			}
 			counter = distribution.getDistributedItemCollection().size();
 		}
 		if (values.keySet().isEmpty())
 		{
 			String message = ApplicationProperties.getValue("distribution.distributedItem");
 			throw new DAOException("errors.item.required", new String[]{message});
 
 		}
 
 		Iterator it = values.keySet().iterator();
 		while (it.hasNext())
 		{
 			String key = (String) it.next();
 			String value = (String) values.get(key);
 
 			if (key.indexOf("Specimen_id") != -1 && !validator.isValidOption(value))
 			{
 				String message = ApplicationProperties.getValue("itemrecord.specimenId");
 				throw new DAOException("errors.item.required", new String[]{message});
 
 			}
 
 			if (key.indexOf("_quantity") != -1)
 			{
 				if ((validator.isEmpty(value)))
 				{
 					Logger.out.debug("Quantity empty**************");
 					String message = ApplicationProperties.getValue("itemrecord.quantity");
 					throw new DAOException("errors.item.required", new String[]{message});
 
 				}
 				else if (!validator.isDouble(value))
 				{
 					String message = ApplicationProperties.getValue("itemrecord.quantity");
 					throw new DAOException("errors.item.format", new String[]{message});
 
 				}
 			}
 
 		}
 		
 		*/
 		//END
 
 		if (operation.equals(Constants.ADD))
 		{
 			if (!Constants.ACTIVITY_STATUS_ACTIVE.equals(distribution.getActivityStatus()))
 			{
 				throw new DAOException(ApplicationProperties
 						.getValue("activityStatus.active.errMsg"));
 			}
 		}
 		else
 		{
 			if (!Validator.isEnumeratedValue(Constants.ACTIVITY_STATUS_VALUES, distribution
 					.getActivityStatus()))
 			{
 				throw new DAOException(ApplicationProperties.getValue("activityStatus.errMsg"));
 			}
 		}
 
 		return true;
 	}
 
 	//Mandar : 04-Apr-06 : bug id:1545 : - Check for removed specimens
 	private void updateRemovedSpecimens(Collection newDistributedItemCollection,
 			Collection oldDistributedItemCollection, DAO dao, SessionDataBean sessionDataBean)
 			throws DAOException, UserNotAuthorizedException
 	{
 		// iterate through the old collection and find the specimens that are removed.
 		Iterator it = oldDistributedItemCollection.iterator();
 		while (it.hasNext())
 		{
 			DistributedItem item = (DistributedItem) it.next();
 			boolean isPresentInNew = newDistributedItemCollection.contains(item);
 			Logger.out.debug("Old Object in New Collection : " + isPresentInNew);
 			if (!isPresentInNew)
 			{
 				Object specimenObj = dao.retrieve(Specimen.class.getName(), item.getSpecimen()
 						.getId());
 				double quantity = item.getQuantity().doubleValue();
 				updateAvailableQty((Specimen) specimenObj, quantity);
 				dao
 						.update(specimenObj, sessionDataBean, Constants.IS_AUDITABLE_TRUE,
 								Constants.IS_SECURE_UPDATE_TRUE,
 								Constants.HAS_OBJECT_LEVEL_PRIVILEGE_FALSE);
 			}
 
 		}
 		Logger.out.debug("Update Successful ...04-Apr-06");
 	}
 
 	//this method updates the specimen available qty by adding the previously subtracted(during distribution) qty.
 	private void updateAvailableQty(Specimen specimen, double quantity)
 	{
 		if (specimen instanceof TissueSpecimen)
 		{
 			TissueSpecimen tissueSpecimen = (TissueSpecimen) specimen;
 			double availabeQty = Double.parseDouble(tissueSpecimen.getAvailableQuantity()
 					.toString());//tissueSpecimen.getAvailableQuantityInGram().doubleValue();
 			Logger.out.debug("TissueAvailabeQty" + availabeQty);
 			availabeQty = availabeQty + quantity;
 			Logger.out.debug("TissueAvailabeQty after addition" + availabeQty);
 			tissueSpecimen.setAvailableQuantity(new QuantityInGram(availabeQty));//tissueSpecimen.setAvailableQuantityInGram(new Double(availabeQty));
 		}
 		else if (specimen instanceof CellSpecimen)
 		{
 			CellSpecimen cellSpecimen = (CellSpecimen) specimen;
 			int availabeQty = Integer.parseInt(cellSpecimen.getAvailableQuantity().toString());//cellSpecimen.getAvailableQuantityInCellCount().intValue();
 			availabeQty = availabeQty + (int) quantity;
 			cellSpecimen.setAvailableQuantity(new QuantityInCount(availabeQty));//cellSpecimen.setAvailableQuantityInCellCount(new Integer(availabeQty));
 		}
 		else if (specimen instanceof MolecularSpecimen)
 		{
 			MolecularSpecimen molecularSpecimen = (MolecularSpecimen) specimen;
 			double availabeQty = Double.parseDouble(molecularSpecimen.getAvailableQuantity()
 					.toString());//molecularSpecimen.getAvailableQuantityInMicrogram().doubleValue();
 			availabeQty = availabeQty + quantity;
 			molecularSpecimen.setAvailableQuantity(new QuantityInMicrogram(availabeQty));//molecularSpecimen.setAvailableQuantityInMicrogram(new Double(availabeQty));
 		}
 		else if (specimen instanceof FluidSpecimen)
 		{
 			FluidSpecimen fluidSpecimen = (FluidSpecimen) specimen;
 			double availabeQty = Double
 					.parseDouble(fluidSpecimen.getAvailableQuantity().toString());//fluidSpecimen.getAvailableQuantityInMilliliter().doubleValue();
 			availabeQty = availabeQty + quantity;
 			fluidSpecimen.setAvailableQuantity(new QuantityInMilliliter(availabeQty));//fluidSpecimen.setAvailableQuantityInMilliliter(new Double(availabeQty));
 		}
 
 	}
 
 	//Mandar : 04-Apr-06 : end
 
 	public Long getSpecimenId(String barcodeLabel, Integer distributionBasedOn) throws DAOException
 	{
 		String className = Specimen.class.getName();
 		String[] selectColumnName = null;
 		String[] whereColumnName = {"barcode"};
 		String[] whereColumnCondition = {"="};
 		String[] value = {barcodeLabel};
 		Object[] whereColumnValue = new Object[]{value};
 
 		if (distributionBasedOn.intValue() == Constants.LABEL_BASED_DISTRIBUTION)
 		{
 			whereColumnName = new String[]{"label"};
 		}
 
 		Specimen specimen = null;
 		List specimenList = retrieve(className, selectColumnName, whereColumnName,
 				whereColumnCondition, value, null);
 
 		if (specimenList == null || specimenList.isEmpty())
 		{
 			throw new DAOException("errors.distribution.specimenNotFound");
 		}
 		specimen = (Specimen) specimenList.get(0);		
 		if(specimen.getActivityStatus().equals(Constants.ACTIVITY_STATUS_VALUES[2]))
 		{
 			throw new DAOException("errors.distribution.closedSpecimen");
 		}
 
 		return specimen.getId();
 	}
 
 	public Long getSpecimenArrayId(String barcodeLabel, Integer distributionBasedOn)
 			throws DAOException
 	{
 
 		String className = SpecimenArray.class.getName();
 		String[] selectColumnName = null;
		String[] whereColumnName = {Constants.SYSTEM_BARCODE};
 		String[] whereColumnCondition = {"="};
 		String[] value = {barcodeLabel};
 		Object[] whereColumnValue = new Object[]{value};
 
 		if (distributionBasedOn.intValue() == Constants.LABEL_BASED_DISTRIBUTION)
 		{
			whereColumnName = new String[]{Constants.SYSTEM_NAME};
 		}
 		
 		
 
 		SpecimenArray specimenArray = null;
 		List specimenList = retrieve(className, selectColumnName, whereColumnName,
 				whereColumnCondition, value, null);
 
 		if (specimenList == null || specimenList.isEmpty())
 		{
 			throw new DAOException("errors.distribution.specimenArrayNotFound");
 		}
 		specimenArray = (SpecimenArray) specimenList.get(0);
 		return specimenArray.getId();
 	}
 }
