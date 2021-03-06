 package com.idega.block.trade.stockroom.data;
 
 import java.rmi.RemoteException;
 import java.sql.SQLException;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Vector;
 import javax.ejb.CreateException;
 import javax.ejb.EJBException;
 import javax.ejb.FinderException;
 import com.idega.block.trade.data.CreditCardInformation;
 import com.idega.block.trade.stockroom.business.SupplierManagerBusiness;
 import com.idega.block.trade.stockroom.business.SupplierManagerBusinessBean;
 import com.idega.business.IBOLookup;
 import com.idega.core.contact.data.Email;
 import com.idega.core.contact.data.Phone;
 import com.idega.core.location.data.Address;
 import com.idega.data.EntityFinder;
 import com.idega.data.IDOAddRelationshipException;
 import com.idega.data.IDOException;
 import com.idega.data.IDOLookup;
 import com.idega.data.IDOQuery;
 import com.idega.data.IDORelationshipException;
 import com.idega.data.IDORemoveRelationshipException;
 import com.idega.data.query.Column;
 import com.idega.data.query.MatchCriteria;
 import com.idega.data.query.Order;
 import com.idega.data.query.SelectQuery;
 import com.idega.data.query.Table;
 import com.idega.data.query.WildCardColumn;
 import com.idega.presentation.IWContext;
 import com.idega.user.data.Group;
 
 /**
  * Title: IW Trade Description: Copyright: Copyright (c) 2001 Company: idega.is
  * 
  * @author 2000 - idega team -<br>
  *             <a href="mailto:gummi@idega.is">Gumundur gst Smundsson </a> <br>
  *             <a href="mailto:gimmi@idega.is">Grmur Jnsson </a>
  * @version 1.0
  */
 
 public class SupplierBMPBean extends com.idega.data.GenericEntity implements Supplier{
 
 	private String newName;
 	private static String COLUMN_SUPPLIER_MANAGER_ID = "SUPPLIER_MANAGER_ID";
 
 	public SupplierBMPBean() {
 		super();
 	}
 
 	public SupplierBMPBean(int id) throws SQLException {
 		super(id);
 	}
 
 	public void initializeAttributes() {
 		addAttribute(getIDColumnName());
 		addAttribute(getColumnNameName(), "Name", true, true, String.class);
 		addAttribute(getColumnNameDescription(), "Lsing", true, true, String.class, 500);
 		addAttribute(getColumnNameGroupID(), "Hpur", true, true, Integer.class, "many_to_one", SupplierStaffGroup.class);
 		addAttribute(getColumnNameIsValid(), " notkun", true, true, Boolean.class);
 		addAttribute(COLUMN_SUPPLIER_MANAGER_ID, "supplier manager", true, true, Integer.class, MANY_TO_ONE, Group.class);
 		/* can this be removed */
 		addAttribute(getColumnNameTPosMerchantID(), "Viskiptamannanumer", true, true, Integer.class);
 
 		this.addManyToManyRelationShip(Address.class, "SR_SUPPLIER_IC_ADDRESS");
 		this.addManyToManyRelationShip(Phone.class, "SR_SUPPLIER_IC_PHONE");
 		this.addManyToManyRelationShip(Email.class, "SR_SUPPLIER_IC_EMAIL");
 
 		this.addManyToManyRelationShip(ProductCategory.class, "SR_SUPPLIER_PRODUCT_CATEGORY");
 		this.addManyToManyRelationShip(Reseller.class);
 		this.addManyToManyRelationShip(CreditCardInformation.class, "SR_SUPPLIER_CC_INFORMATION");
 
 		addIndex("IDX_SUPP_1", new String[]{getIDColumnName(), getColumnNameIsValid()});
 		addIndex("IDX_SUPP_2", new String[]{getColumnNameIsValid()});
		addIndex("IDX_SUPP_4", new String[]{COLUMN_SUPPLIER_MANAGER_ID, getColumnNameIsValid()});
 	}
 
 	public void insertStartData() throws Exception {
 	}
 
 	public void setDefaultValues() {
 		setIsValid(true);
 	}
 
 	public static String getSupplierTableName() {
 		return "SR_SUPPLIER";
 	}
 
 	public static String getColumnNameName() {
 		return "NAME";
 	}
 
 	public static String getColumnNameDescription() {
 		return "DESCRIPTION";
 	}
 
 	public static String getColumnNameGroupID() {
 		return "IC_GROUP_ID";
 	}
 
 	public static String getColumnNameIsValid() {
 		return "IS_VALID";
 	}
 
 	public static String getColumnNameTPosMerchantID() {
 		return "TPOS_MERCHANT_ID";
 	}
 
 	public String getEntityName() {
 		return getSupplierTableName();
 	}
 
 	public String getName() {
 		return getStringColumnValue(getColumnNameName());
 	}
 
 	public void setName(String name) {
 		newName = name;
 	}
 
 	public String getDescription() {
 		return getStringColumnValue(getColumnNameDescription());
 	}
 
 	public void setDescription(String description) {
 		setColumn(getColumnNameDescription(), description);
 	}
 
 	public void setGroupId(int id) {
 		setColumn(getColumnNameGroupID(), id);
 	}
 
 	public int getGroupId() {
 		return getIntColumnValue(getColumnNameGroupID());
 	}
 
 	public void setIsValid(boolean isValid) {
 		setColumn(getColumnNameIsValid(), isValid);
 	}
 
 	public boolean getIsValid() {
 		return getBooleanColumnValue(getColumnNameIsValid());
 	}
 
 	public int getSupplierManagerID() {
 		return getIntColumnValue(COLUMN_SUPPLIER_MANAGER_ID);
 	}
 
 	public Group getSupplierManager() {
 		return (Group) getColumnValue(COLUMN_SUPPLIER_MANAGER_ID);
 	}
 	
 	public void setSupplierManager(Group group) {
 		setColumn(COLUMN_SUPPLIER_MANAGER_ID, group);
 	}
  	
 	public void setSupplierManagerPK(Object pk) {
 		setColumn(COLUMN_SUPPLIER_MANAGER_ID, pk);
 	}
 	
 	public Address getAddress() throws SQLException {
 		Address address = null;
 		List addr = getAddresses();
 		if (addr != null) {
 			address = (Address) addr.get(addr.size() - 1);
 		}
 		return address;
 	}
 
 	public List getAddresses() throws SQLException {
 		return EntityFinder.findRelated(this, com.idega.core.location.data.AddressBMPBean.getStaticInstance(Address.class));
 	}
 
 	public List getPhones() throws SQLException {
 		return EntityFinder.findRelated(this, com.idega.core.contact.data.PhoneBMPBean.getStaticInstance(Phone.class));
 	}
 
 	public List getHomePhone() throws SQLException {
 		return getPhones(com.idega.core.contact.data.PhoneBMPBean.getHomeNumberID());
 	}
 
 	public List getFaxPhone() throws SQLException {
 		return getPhones(com.idega.core.contact.data.PhoneBMPBean.getFaxNumberID());
 	}
 
 	public List getWorkPhone() throws SQLException {
 		return getPhones(com.idega.core.contact.data.PhoneBMPBean.getWorkNumberID());
 	}
 
 	public List getMobilePhone() throws SQLException {
 		return getPhones(com.idega.core.contact.data.PhoneBMPBean.getMobileNumberID());
 	}
 
 	public List getPhones(int PhoneTypeId) throws SQLException {
 		Vector phones = new Vector();
 		List allPhones = getPhones();
 		if (allPhones != null) {
 			Phone temp = null;
 			for (int i = 0; i < allPhones.size(); i++) {
 				temp = (Phone) allPhones.get(i);
 				if (temp.getPhoneTypeId() == PhoneTypeId) {
 					phones.add(temp);
 				}
 			}
 		}
 		return phones;
 	}
 
 	public Email getEmail() throws SQLException {
 		Email email = null;
 		List emails = getEmails();
 		if (emails != null) {
 			email = (Email) emails.get(emails.size() - 1);
 		}
 		return email;
 	}
 
 	public List getEmails() throws SQLException {
 		return EntityFinder.findRelated(this, com.idega.core.contact.data.EmailBMPBean.getStaticInstance(Email.class));
 	}
 
 	/**
 	 * @deprecated Replaced with findAll( supplierManager );
 	 */
 	public static Supplier[] getValidSuppliers() throws SQLException {
 		try {
 			throw new Exception("ERRRROR : Using a wrong method : getValidSuppliers(), should be findAll ( supplierManager )    !!!!");
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return (Supplier[]) com.idega.block.trade.stockroom.data.SupplierBMPBean.getStaticInstance(Supplier.class).findAllByColumnOrdered(com.idega.block.trade.stockroom.data.SupplierBMPBean.getColumnNameIsValid(), "Y", com.idega.block.trade.stockroom.data.SupplierBMPBean.getColumnNameName());
 	}
 	
 	public Collection ejbFindAll(Group supplierManager) throws FinderException {
 		Table table = new Table(this);
 		Column isValid = new Column(table, this.getColumnNameIsValid());
 		Column suppMan = new Column(table, COLUMN_SUPPLIER_MANAGER_ID);
 		Column name = new Column(table, getColumnNameName());
 		Order order = new Order(name, true);
 		
 		SelectQuery query = new SelectQuery(table);
 		query.addColumn(new WildCardColumn(table));
 		query.addCriteria(new MatchCriteria(isValid, MatchCriteria.EQUALS, true));
 		query.addCriteria(new MatchCriteria(suppMan, MatchCriteria.EQUALS, supplierManager.getPrimaryKey()));
 		query.addOrder(order);
 		
 		return this.idoFindPKsByQuery(query);
 		
 //		return this.idoFindAllIDsByColumnOrderedBySQL(this.getColumnNameIsValid(), "'Y'", getColumnNameName());
 	}
 
 	public void update() throws SQLException {
 		if (newName != null) {
 			try {
 				SupplierManagerBusiness sm = (SupplierManagerBusiness) IBOLookup.getServiceInstance(IWContext.getInstance(), SupplierManagerBusiness.class);
 				Group pGroup = sm.getPermissionGroup(this);
 				pGroup.setName(newName + "_" + this.getID() + SupplierManagerBusinessBean.permissionGroupNameExtention);
 				pGroup.store();
 				SupplierStaffGroup sGroup = sm.getSupplierStaffGroup(this);
 				sGroup.setName(newName + "_" + this.getID());
 				sGroup.store();
 				setColumn(getColumnNameName(), newName);
 				newName = null;
 			} catch (Exception e) {
 				e.printStackTrace();
 				throw new SQLException(e.getMessage());
 			}
 		}
 		super.update();
 	}
 
 	public void insert() throws SQLException {
 		if (newName != null) {
 			setColumn(getColumnNameName(), newName);
 		}
 		super.insert();
 	}
 
 	public int getTPosMerchantId() {
 		return getIntColumnValue(getColumnNameTPosMerchantID());
 	}
 
 	public Settings getSettings() throws FinderException, RemoteException, CreateException {
 		Collection coll = null;
 		try {
 			coll = this.idoGetRelatedEntities(Settings.class);
 		}
 		catch (IDOException ido) {
 			//throw new CreateException(ido.getMessage());
 		}
 		SettingsHome shome = (SettingsHome) IDOLookup.getHome(Settings.class);
 		if (coll.size() == 1) {
 			Iterator iter = coll.iterator();
 			return (Settings) iter.next();
 		}
 		else if (coll.size() > 1) {
 			/** @todo fixa vitlaus gogn... setja removeFrom thegar thad virkar */
 			debug("Settings data wrong for Supplier " + getID());
 			Iterator iter = coll.iterator();
 			Settings set;
 			while (iter.hasNext()) {
 				set = (Settings) iter.next();
 				if (!iter.hasNext()) { return set; }
 			}
 			return null;
 		}
 		else {
 			return shome.create(this);
 		}
 	}
 
 	public void setCreditCardInformation(Collection pks) throws IDORemoveRelationshipException, IDOAddRelationshipException, EJBException {
 		if (pks != null) {
 			Iterator iter = pks.iterator();
 			Object obj;
 			while (iter.hasNext()) {
 				obj = iter.next();
 				try {
 					if (obj instanceof CreditCardInformation) {
 						addCreditCardInformation((CreditCardInformation) obj);
 					}
 					else {
 						addCreditCardInformationPK(obj);
 					}
 				}
 				catch (Exception e) {
 					log("SupplierBMPBean : error adding cc info, probably primaryKey error : " + e.getMessage());
 				}
 			}
 		}
 	}
 
 	public void addCreditCardInformationPK(Object pk) throws IDOAddRelationshipException {
 		this.idoAddTo(CreditCardInformation.class, pk);
 	}
 
 	public void addCreditCardInformation(CreditCardInformation info) throws IDOAddRelationshipException, EJBException {
 		if (info != null) {
 			addCreditCardInformationPK(info.getPrimaryKey());
 		}
 	}
 
 	public Collection getCreditCardInformation() throws IDORelationshipException {
 		return this.idoGetRelatedEntities(CreditCardInformation.class);
 	}
 
 	public Collection getProductCategories() throws IDORelationshipException {
 		return this.idoGetRelatedEntities(ProductCategory.class);
 	}
 
 	public Collection ejbFindWithTPosMerchant() throws FinderException {
 		IDOQuery query = this.idoQuery();
 		query.appendSelectAllFrom(this).appendWhereEqualsQuoted(getColumnNameIsValid(), "Y").appendAnd().append("(").append(getColumnNameTPosMerchantID()).append(" is not null").appendOr().appendEquals(getColumnNameTPosMerchantID(), -1).append(")").appendOrderBy(getColumnNameName());
 
 		return this.idoFindPKsByQuery(query);
 	}
 	
 	public Collection ejbFindAllByGroupID(int groupID) throws FinderException {
 		Table table = new Table(this);
 		Column groupColumn = new Column(table, getColumnNameGroupID());
 		Column validColumn = new Column(table, getColumnNameIsValid());
 		SelectQuery query = new SelectQuery(table);
 		query.addColumn(new WildCardColumn(table));
 		query.addCriteria(new MatchCriteria(groupColumn, MatchCriteria.EQUALS, groupID));
 		query.addCriteria(new MatchCriteria(validColumn, MatchCriteria.EQUALS, true));
 		return this.idoFindPKsByQuery(query);
 	}
 }
 
