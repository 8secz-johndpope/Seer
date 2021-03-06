 /**
  * <p>Title: DistributionForm Class</p>
  * <p>Description:  This Class handles the Distribution..
  * <p> It extends the EventParametersForm class.    
  * Copyright:    Copyright (c) 2005
  * Company: Washington University, School of Medicine, St. Louis.
  * @author Jyoti Singh
  * @version 1.00
  * Created on Aug 10, 2005
  */
 
 package edu.wustl.catissuecore.actionForm;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 
 import org.apache.struts.action.ActionError;
 import org.apache.struts.action.ActionErrors;
 import org.apache.struts.action.ActionMapping;
 
 import edu.wustl.catissuecore.domain.AbstractDomainObject;
 import edu.wustl.catissuecore.domain.CellSpecimen;
 import edu.wustl.catissuecore.domain.DistributedItem;
 import edu.wustl.catissuecore.domain.Distribution;
 import edu.wustl.catissuecore.domain.FluidSpecimen;
 import edu.wustl.catissuecore.domain.MolecularSpecimen;
 import edu.wustl.catissuecore.domain.Specimen;
 import edu.wustl.catissuecore.domain.TissueSpecimen;
 import edu.wustl.catissuecore.util.global.ApplicationProperties;
 import edu.wustl.catissuecore.util.global.Constants;
 import edu.wustl.catissuecore.util.global.Utility;
 import edu.wustl.catissuecore.util.global.Validator;
 import edu.wustl.common.util.logger.Logger;
 
 /**
   *
  * Description:  This Class handles the Distribution..
  */
 public class DistributionForm extends SpecimenEventParametersForm
 {
 	
 	private String fromSite;
 	private String toSite;
 	
 	private int counter=1;
 	private String distributionProtocolId;
 	private boolean idChange = false;
 	private int rowNo=0;
 	
 	
 	
 	/**
 	 * Map to handle values of all Events
 	 */
 	protected Map values = new HashMap();
 	
 	public int getFormId()
 	{
 		return Constants.DISTRIBUTION_FORM_ID;
 	}
 
 	public void setAllValues(AbstractDomainObject abstractDomain)
 	{
 		super.setAllValues(abstractDomain);
 		Logger.out.debug("setAllValues of DistributionForm"); 
 		Distribution distributionObject = (Distribution)abstractDomain ;
 		this.distributionProtocolId = String.valueOf(distributionObject.getDistributionProtocol().getSystemIdentifier());
 		this.fromSite = String.valueOf(distributionObject.getFromSite().getSystemIdentifier());
 		this.toSite = String.valueOf(distributionObject.getToSite().getSystemIdentifier());
 		this.activityStatus = Utility.toString(distributionObject.getActivityStatus());
 		Logger.out.debug("this.activityStatus "+this.activityStatus);
 		Collection distributedItemCollection = distributionObject.getDistributedItemCollection();
 		
 		if(distributedItemCollection != null)
 		{
 			values = new HashMap();
 			
 			Iterator it = distributedItemCollection.iterator();
 			int i=1;
 			
 			while(it.hasNext())
 			{
 				
 				String key1 = "DistributedItem:"+i+"_systemIdentifier";
 				String key2 = "DistributedItem:"+i+"_Specimen_systemIdentifier";
 				String key3 = "DistributedItem:"+i+"_quantity";
 				String key4 = "DistributedItem:"+i+"_unitSpan";
 				String key5 = "DistributedItem:"+i+"_tissueSite";
 				String key6 = "DistributedItem:"+i+"_tissueSide";
 				String key7 = "DistributedItem:"+i+"_pathologicalStatus";
 				String key8 = "DistributedItem:"+i+"_Specimen_className";	
 				String key9 = "DistributedItem:"+i+"_availableQty";
 				String key10 = "DistributedItem:"+i+"_previousQuantity";
 				
 				DistributedItem dItem = (DistributedItem)it.next();
 				Specimen specimen =dItem.getSpecimen();
 				String unit= getUnitSpan(specimen);
 				
 				Double quantity = dItem.getQuantity();
 				//dItem.setPreviousQty(quantity);
 
 				values.put(key1,dItem.getSystemIdentifier());
 				values.put(key2,specimen.getSystemIdentifier());
 				values.put(key3,quantity);
 				values.put(key4,unit);
 				values.put(key5,specimen.getSpecimenCharacteristics().getTissueSite());
 				values.put(key6,specimen.getSpecimenCharacteristics().getTissueSide());
 				values.put(key7,specimen.getSpecimenCharacteristics().getPathologicalStatus());
 				values.put(key8,specimen.getClassName());
 				values.put(key9,getAvailableQty(specimen));
 				values.put(key10,quantity);
 				
 				i++;
 			}
 			Logger.out.debug("Display Map Values"+values); 
 			counter = distributedItemCollection.size();
 		}
 		
 		//At least one row should be displayed in ADD MORE therefore
 		if(counter == 0)
 			counter = 1;
 	}
 	
 	
 	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) 
 	{
 		ActionErrors errors = super.validate(mapping, request);
 		Validator validator = new Validator();
 		Logger.out.debug("Inside validate function");
 		if(!validator.isValidOption(distributionProtocolId))
 		{
 			Logger.out.debug("dist prot");
 			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.item.required",ApplicationProperties.getValue("distribution.protocol")));
 		}
 		
 		if(!validator.isValidOption(fromSite))
 		{
 			Logger.out.debug("from site");
 			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.item.required",ApplicationProperties.getValue("distribution.fromSite")));
 		}
 		
 		if(!validator.isValidOption(toSite))
 		{
 			Logger.out.debug("to site");
 			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.item.required",ApplicationProperties.getValue("distribution.toSite")));
 		}
 		
 		//Validations for Add-More Block
         
         try
 		{
         	if (this.values.keySet().isEmpty())
 			{
 				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.one.item.required",ApplicationProperties.getValue("distribution.distributedItem")));
 			}
         	
 			Iterator it = this.values.keySet().iterator();
 			while (it.hasNext())
 			{
 				String key = (String)it.next();
 				String value = (String)values.get(key);
 				
 				if(key.indexOf("Specimen_systemIdentifier")!=-1 && !validator.isValidOption( value))
 				{
 					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.distribution.missing",ApplicationProperties.getValue("itemrecord.specimenId")));
 				}
 				
 				
 				/*if(key.indexOf("Specimen_className")!=-1 && !validator.isValidOption( value))
 				{
 					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.distribution.missing",ApplicationProperties.getValue("distribution.specimenType")));
 				}
 				if(key.indexOf("quantity")!=-1)
 				{
 					String classKey = key.substring(0,key.lastIndexOf("_") );
 					classKey = classKey + "_specimenClass";
 					String classValue = (String)getValue(classKey );
 					if (classValue.trim().equals("Cell"))
 					{*/
         		if(key.indexOf("_quantity")!=-1  && (validator.isEmpty(value) ))
         		{
         			Logger.out.debug("Quantity empty**************");
         			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.distribution.missing",ApplicationProperties.getValue("itemrecord.quantity")));
         		}
 					
 				/*else
 				{
 					if(key.indexOf("_quantity")!=-1  && !(validator.isEmpty(value) && !validator.isNumeric(value)))
         			{
 						Logger.out.debug("Quantity invalid**************");
 						errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("errors.item.format",ApplicationProperties.getValue("itemrecord.quantity")));
         			}
 				}*/
 				//}  if  quantity
 			}
 		}
 		catch (Exception excp)
 		{
 	    	// use of logger as per bug 79
 	    	Logger.out.error(excp.getMessage(),excp); 
 			errors = new ActionErrors();
 		}
 		return errors;
 	}
 	/**
 	 * @return Returns the distributionProtocolId.
 	 */
 	public String getDistributionProtocolId()
 	{
 		return distributionProtocolId;
 	}
 	/**
 	 * @param distributionProtocolId The distributionProtocolId to set.
 	 */
 	public void setDistributionProtocolId(String distributionProtocolId)
 	{
 		this.distributionProtocolId = distributionProtocolId;
 	}
 	
 	/**
 	 * @return fromSite
 	 */ 
 	public String getFromSite() {
 		return fromSite;
 	}
 
 	/**
 	 * @param fromSite
 	 */
 	public void setFromSite(String fromSite) {
 		this.fromSite = fromSite;
 	}
 
 	/**
 	 * @return
 	 */
 	public int getCounter()
 	{
 		return counter;
 	}
 	
 	/**
 	 * @param counter The counter to set.
 	 */
 	public void setCounter(int counter)
 	{
 		this.counter = counter;
 	}
 	public String getToSite() {
 		return toSite;
 	}
 
 	/**
 	 * @param toSite
 	 */
 	public void setToSite(String toSite) {
 		this.toSite = toSite;
 	}
 	
 	/**
 		 * Associates the specified object with the specified key in the map.
 		 * @param key the key to which the object is mapped.
 		 * @param value the object which is mapped.
 		 */
 		public void setValue(String key, Object value)
 		{
 			values.put(key, value);
 		}
 
 		/**
 		 * Returns the object to which this map maps the specified key.
 		 * 
 		 * @param key the required key.
 		 * @return the object to which this map maps the specified key.
 		 */
 		public Object getValue(String key)
 		{
 			return values.get(key);
 		}		
 
 		/**
 		 * @param values The values to set.
 		 */
 		public void setValues(Map values)
 		{
 			this.values = values;
 		}
 
 	/**
 	 * @return
 	 */
 	public Map getValues() {
 		return values;
 	}
 
 	protected void reset()
     {
 //        super.reset();
 //        this.distributionProtocolId = null;
 //        this.fromSite = null;
 //        this.toSite = null;
 //        this.counter =1;
 		  
     }
 	/**
 	 * @return Returns the idChange.
 	 */
 	public boolean isIdChange() {
 		return idChange;
 	}
 	/**
 	 * @param idChange The idChange to set.
 	 */
 	public void setIdChange(boolean idChange) {
 		this.idChange = idChange;
 	}
 	/**
 	 * @return Returns the rowNo.
 	 */
 	public int getRowNo() {
 		return rowNo;
 	}
 	/**
 	 * @param rowNo The rowNo to set.
 	 */
 	public void setRowNo(int rowNo) {
 		this.rowNo = rowNo;
 	}
 	public static String getUnitSpan(Specimen specimen)
 	{
 		
 		if(specimen instanceof TissueSpecimen)
 		{
 			return Constants.UNIT_GM;
 			
 		}
 		else if(specimen instanceof CellSpecimen)
 		{
 			return Constants.UNIT_CC;
 			
 		}
 		else if(specimen instanceof MolecularSpecimen)
 		{
 			return Constants.UNIT_MG;
 			
 		}
 		else if(specimen instanceof FluidSpecimen)
 		{
 			return Constants.UNIT_ML;
 		}
 		return null;
 	}
 	public Object getAvailableQty(Specimen specimen)
 	{
 		if(specimen instanceof TissueSpecimen)
 		{
 			
 			TissueSpecimen tissueSpecimen = (TissueSpecimen) specimen;
 			Logger.out.debug("tissueSpecimenAvailableQuantityInGram "+tissueSpecimen.getAvailableQuantityInGram());
 			return tissueSpecimen.getAvailableQuantityInGram();
 			
 		}
 		else if(specimen instanceof CellSpecimen)
 		{
 			CellSpecimen cellSpecimen = (CellSpecimen) specimen;
 			return cellSpecimen.getAvailableQuantityInCellCount();
 			
 		}
 		else if(specimen instanceof MolecularSpecimen)
 		{
 			MolecularSpecimen molecularSpecimen = (MolecularSpecimen) specimen;
 			return molecularSpecimen.getAvailableQuantityInMicrogram();
 			
 		}
 		else if(specimen instanceof FluidSpecimen)
 		{
 			FluidSpecimen fluidSpecimen = (FluidSpecimen) specimen;
 			return fluidSpecimen.getAvailableQuantityInMilliliter();
 		}
 		return null;
 	}
 	
 	
 }
