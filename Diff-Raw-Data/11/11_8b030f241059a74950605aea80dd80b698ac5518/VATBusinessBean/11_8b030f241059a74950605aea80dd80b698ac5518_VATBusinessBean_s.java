 /*
 * $Id: VATBusinessBean.java,v 1.5 2003/08/25 15:32:45 anders Exp $
  *
  * Copyright (C) 2003 Agura IT. All Rights Reserved.
  *
  * This software is the proprietary information of Agura IT AB.
  * Use is subject to license terms.
  *
  */
 package se.idega.idegaweb.commune.accounting.regulations.business;
 
 import java.util.Collection;
 import java.sql.Date;
 import java.rmi.RemoteException;
 import javax.ejb.FinderException;
 import javax.ejb.CreateException;
 import javax.ejb.RemoveException;
 
 import se.idega.idegaweb.commune.accounting.regulations.data.VATRegulationHome;
 import se.idega.idegaweb.commune.accounting.regulations.data.VATRegulation;
 
 /** 
  * Business logic for VAT values and regulations.
  * <p>
 * Last modified: $Date: 2003/08/25 15:32:45 $ by $Author: anders $
  *
  * @author Anders Lindman
 * @version $Revision: 1.5 $
  */
 public class VATBusinessBean extends com.idega.business.IBOServiceBean implements VATBusiness  {
 
 	private final static String KP = "vat_error."; // key prefix 
 
 	public final static String KEY_DATE_FORMAT = KP + "date_format";
 	public final static String KEY_PERIOD_VALUES = KP + "period_value";
 	public final static String KEY_FROM_DATE_MISSING = KP + "from_date_missing";
 	public final static String KEY_TO_DATE_MISSING = KP + "to_date_missing";
 	public final static String KEY_DESCRIPTION_MISSING = KP + "description_missing";
 	public final static String KEY_VAT_PERCENT_MISSING = KP + "vat_percent_missing";
 	public final static String KEY_VAT_PERCENT_VALUE = KP + "vat_percent_value";
 	public final static String KEY_PAYMENT_FLOW_TYPE_MISSING = KP + "payment_flow_type_missing";
 	public final static String KEY_PROVIDER_TYPE_MISSING = KP + "provider_type_missing";
 	public final static String KEY_CANNOT_SAVE_VAT_REGULATION = KP + "cannot_save_vat_regulation";
 	public final static String KEY_CANNOT_DELETE_VAT_REGULATION = KP + "cannot_delete_vat_regulation";
 	public final static String KEY_CANNOT_FIND_VAT_REGULATION = KP + "cannot_find_vat_regulation";
 
 	public final static String DEFAULT_DATE_FORMAT = "Datum mste anges p formen MM, MMDD, eller MMDD.";
 	public final static String DEFAULT_PERIOD_VALUES = "Skperiodens startdatum mste vara mindre eller lika med slutdatum.";
 	public final static String DEFAULT_FROM_DATE_MISSING = "Periodens startdatum mste fyllas i.";
 	public final static String DEFAULT_TO_DATE_MISSING = "Periodens slutdatum mste fyllas i.";
 	public final static String DEFAULT_DESCRIPTION_MISSING = "Benmning av momssatsen mste fyllas i.";
 	public final static String DEFAULT_VAT_PERCENT_MISSING = "Procentsats mste fyllas i.";
 	public final static String DEFAULT_VAT_PERCENT_VALUE = "Procentsatsen mste vara mellan 0 och 100.";
 	public final static String DEFAULT_PAYMENT_FLOW_TYPE_MISSING = "Strm mste vljas.";
 	public final static String DEFAULT_PROVIDER_TYPE_MISSING = "Anordnartyp mste vljas.";
 	public final static String DEFAULT_CANNOT_SAVE_VAT_REGULATION = "Momssatsen kunde inte sparas p grund av tekniskt fel.";
 	public final static String DEFAULT_CANNOT_DELETE_VAT_REGULATION = "Momssatsen kunde inte tas bort p grund av tekniskt fel.";
 	public final static String DEFAULT_CANNOT_FIND_VAT_REGULATION = "Kan ej hitta momssatsen.";
 
 	/**
 	 * Return VAT regulation home. 
 	 */	
 	protected VATRegulationHome getVATRegulationHome() throws RemoteException {
 		return (VATRegulationHome) com.idega.data.IDOLookup.getHome(VATRegulation.class);
 	}	
 	
 	/**
 	 * Finds all VAT regulations.
 	 * @return collection of VAT regulation objects
 	 * @see se.idega.idegaweb.commune.accounting.regulations.data.VATRegulation 
 	 */
 	public Collection findAllVATRegulations() {
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 			return home.findAll();				
 		} catch (RemoteException e) {
 			return null;
 		} catch (FinderException e) {
 			return null;
 		}
 	}	
 	
 	/**
 	 * Finds all VAT regulations for the specified period.
 	 * The string values are used for exception handling only.
 	 * @param periodFrom the start of the period
 	 * @param periodTo the end of the period
 	 * @param periodFromString the unparsed from date
 	 * @param periodToString the unparsed to date
 	 * @return collection of VAT regulation objects
 	 * @see se.idega.idegaweb.commune.accounting.regulations.data.VATRegulation
 	 * @throws VATException if invalid period parameters
 	 */
 	public Collection findVATRegulations(
 			Date periodFrom,
 			Date periodTo,
 			String periodFromString,
 			String periodToString) throws VATException {
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 
 			String s = periodFromString.trim();
 			if (s != null) {
 				if (!s.equals("")) {
 					if (periodFrom == null) {
 						throw new VATException(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
 					}
 				}
 			}
 
 			s = periodToString.trim();
 			if (s != null) {
 				if (!s.equals("")) {
 					if (periodTo == null) {
 						throw new VATException(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
 					}
 				}
 			}
 			
 			if ((periodFrom != null) && (periodTo != null)) {
 				if (periodFrom.getTime() > periodTo.getTime()) {
 					throw new VATException(KEY_PERIOD_VALUES, DEFAULT_PERIOD_VALUES);
 				}
 			}
 
 			return home.findByPeriod(periodFrom, periodTo);
 			
 		} catch (RemoteException e) {
 			return null;
 		} catch (FinderException e) {
 			return null;
 		}
 	}	
 	
 	/**
 	 * Saves a VAT regulation object.
 	 * Creates a new persistent object if nescessary.
 	 * @parame id the VAT regulation id
 	 * @param periodFrom the start of the period
 	 * @param periodTo the end of the period
 	 * @param periodFromString the unparsed from date
 	 * @param periodToString the unparsed to date
	 * @param descriptionString the description of the VAT regulation
 	 * @param vatPercentString the VAT percent value
 	 * @param paymentFlowTypeIdString the payment flow type id
 	 * @param providerTypeIdString the provider type id
 	 * @throws VATException if invalid parameters
 	 */
 	public void saveVATRegulation(
 			int id,
 			Date periodFrom,
 			Date periodTo,
 			String periodFromString,
 			String periodToString,
 			String description,
 			String vatPercentString,
 			String paymentFlowTypeIdString,
 			String providerTypeIdString) throws VATException {
 				
 		// Period from
 		String s = periodFromString.trim();
 		if (s.equals("")) {
 			throw new VATException(KEY_FROM_DATE_MISSING, DEFAULT_FROM_DATE_MISSING);
 		} else {
 			if (periodFrom == null) {
 				throw new VATException(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
 			}
 		}
 		
 		// Period to
 		s = periodToString.trim();
 		if (s.equals("")) {
 			throw new VATException(KEY_TO_DATE_MISSING, DEFAULT_TO_DATE_MISSING);
 		} else {
 			if (periodTo == null) {
 				throw new VATException(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT);
 			}
 		}
 
 		// Description
 		s = description.trim();
 		if (s.equals("")) {
 			throw new VATException(KEY_DESCRIPTION_MISSING, DEFAULT_DESCRIPTION_MISSING);
 		} else {
 			description = s;
 		}
 		
 		// VAT percent
 		s = vatPercentString.trim();
 		int vatPercent = 0;
 		if (s.equals("")) { 
 			throw new VATException(KEY_VAT_PERCENT_MISSING, DEFAULT_VAT_PERCENT_MISSING);
 		}
 		try {
 			vatPercent = Integer.parseInt(s); 
 		} catch (NumberFormatException e) {
 			throw new VATException(KEY_VAT_PERCENT_VALUE, DEFAULT_VAT_PERCENT_VALUE);
 		}
 		if ((vatPercent < 0) || (vatPercent > 100)) {
 			throw new VATException(KEY_VAT_PERCENT_VALUE, DEFAULT_VAT_PERCENT_VALUE);
 		}
 
 		// Payment flow type
 		s = paymentFlowTypeIdString;
 		if (s.equals("0")) {
 			throw new VATException(KEY_PAYMENT_FLOW_TYPE_MISSING, DEFAULT_PAYMENT_FLOW_TYPE_MISSING);
 		}
 		int paymentFlowTypeId = Integer.parseInt(s);
 
 		// Provider type
 		s = providerTypeIdString;
 		if (s.equals("0")) {
 			throw new VATException(KEY_PROVIDER_TYPE_MISSING, DEFAULT_PROVIDER_TYPE_MISSING);
 		}
 		int providerTypeId = Integer.parseInt(s);
 		
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 			VATRegulation vr = null;
 			try {
 				vr = home.findByPrimaryKey(new Integer(id));
 			} catch (FinderException e) {
 				vr = home.create();
 			}
 			vr.setPeriodFrom(periodFrom);
 			vr.setPeriodTo(periodTo);
 			vr.setDescription(description);
 			vr.setVATPercent(vatPercent);
 			vr.setPaymentFlowTypeId(paymentFlowTypeId);
 			vr.setProviderTypeId(providerTypeId);
 			vr.store();
 		} catch (RemoteException e) { 
 			throw new VATException(KEY_CANNOT_SAVE_VAT_REGULATION, DEFAULT_CANNOT_SAVE_VAT_REGULATION);
 		} catch (CreateException e) { 
 			throw new VATException(KEY_CANNOT_SAVE_VAT_REGULATION, DEFAULT_CANNOT_SAVE_VAT_REGULATION);
 		}		
 	}
 	
 	/**
 	 * Deletes the VAT regulation objects with the specified ids.
 	 * @param vatRegulationIds the array of VAT regulation ids
 	 * @throws VATException if a VAT regulation could not be deleted
 	 */ 
 	public void deleteVATRegulations(String[] vatRegulationIds) throws VATException {
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 			for (int i = 0; i < vatRegulationIds.length; i++) {
 				int id = Integer.parseInt(vatRegulationIds[i]);
 				VATRegulation vr = home.findByPrimaryKey(new Integer(id));
 				vr.remove();
 			}
 		} catch (RemoteException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		} catch (FinderException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		} catch (RemoveException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		}		
 	}
 	
 	/**
 	 * Deletes the VAT regulation object with the specified id.
 	 * @param id the VAT regulation id
 	 * @throws VATException if the VAT regulation could not be deleted
 	 */ 
 	public void deleteVATRegulation(int id) throws VATException {
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 			VATRegulation vr = home.findByPrimaryKey(new Integer(id));
 			vr.remove();
 		} catch (RemoteException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		} catch (FinderException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		} catch (RemoveException e) { 
 			throw new VATException(KEY_CANNOT_DELETE_VAT_REGULATION, DEFAULT_CANNOT_DELETE_VAT_REGULATION);
 		}		
 	}
 	
 	/**
 	 * Returns the VAT regulation with the specified id.
 	 * @param id the VAT regulation id
 	 * @throws VATException if VAT regulation not found
 	 */
 	public VATRegulation getVATRegulation(int id) throws VATException {
 		VATRegulation vr = null;
 		try {
 			VATRegulationHome home = getVATRegulationHome();
 			vr = home.findByPrimaryKey(new Integer(id));
 		} catch (RemoteException e) { 
 			throw new VATException(KEY_CANNOT_FIND_VAT_REGULATION, DEFAULT_CANNOT_FIND_VAT_REGULATION);
 		} catch (FinderException e) { 
 			throw new VATException(KEY_CANNOT_FIND_VAT_REGULATION, DEFAULT_CANNOT_FIND_VAT_REGULATION);
 		}
 		
 		return vr;		
 	}
 }
