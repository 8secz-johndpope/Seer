 /*
  * Created on 24.9.2003
  *
  */
 package se.idega.idegaweb.commune.accounting.invoice.presentation;
 
 import is.idega.idegaweb.member.presentation.UserSearcher;
 
 import java.rmi.RemoteException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.sql.Date;
 import java.sql.SQLException;
 import java.util.Iterator;
 
 import javax.ejb.CreateException;
 import javax.ejb.EJBException; 
 import javax.ejb.EJBLocalHome;
 import javax.ejb.EJBLocalObject;
 import javax.ejb.FinderException;
 import javax.ejb.RemoveException;
 
 import com.idega.block.school.data.School;
 import com.idega.business.IBOLookup;
 
 import com.idega.data.IDOEntityDefinition;
 import com.idega.data.IDOLookup;
 import com.idega.data.IDOLookupException;
 import com.idega.data.IDOStoreException;
 import com.idega.idegaweb.IWApplicationContext;
 import com.idega.presentation.ExceptionWrapper;
 import com.idega.presentation.IWContext;
 import com.idega.presentation.PresentationObject;
 import com.idega.presentation.Table;
 import com.idega.presentation.text.Link;
 import com.idega.presentation.ui.DropdownMenu;
 import com.idega.presentation.ui.Form;
 import com.idega.presentation.ui.HiddenInput;
 import com.idega.presentation.ui.Parameter;
 import com.idega.presentation.ui.TextInput;
 import com.idega.user.data.User;
 
 
 import se.idega.idegaweb.commune.accounting.invoice.business.RegularInvoiceBusiness;
 import se.idega.idegaweb.commune.accounting.invoice.data.RegularInvoiceEntry;
 import se.idega.idegaweb.commune.accounting.invoice.data.RegularInvoiceEntryHome;
 import se.idega.idegaweb.commune.accounting.presentation.AccountingBlock;
 import se.idega.idegaweb.commune.accounting.presentation.ButtonPanel;
 import se.idega.idegaweb.commune.accounting.presentation.ListTable;
 import se.idega.idegaweb.commune.accounting.presentation.OperationalFieldsMenu;
 import se.idega.idegaweb.commune.accounting.presentation.RegulationSearchPanel;
 import se.idega.idegaweb.commune.accounting.regulations.business.RegulationException;
 import se.idega.idegaweb.commune.accounting.regulations.business.RegulationsBusiness;
 import se.idega.idegaweb.commune.accounting.regulations.business.VATBusiness;
 import se.idega.idegaweb.commune.accounting.regulations.data.Regulation;
 import se.idega.idegaweb.commune.accounting.regulations.data.RegulationSpecType;
 import se.idega.idegaweb.commune.accounting.regulations.data.VATRegulation;
 import se.idega.idegaweb.commune.accounting.school.presentation.PostingBlock;
 
 /**
  * @author Roar
  *
  * To change the template for this generated type comment go to
  * Window>Preferences>Java>Code Generation>Code and Comments
  */
 public class RegularInvoiceEntriesList extends AccountingBlock {
 
 	private String LOCALIZER_PREFIX = "regular_invoice_entries_list.";
 	
 	private static final String KEY_OPERATIONAL_FIELD = "operational_field";
 	private static final String KEY_AMOUNT_PR_MONTH = "amount_pr_month";
 	private static final String KEY_AMOUNT = "amount";
 	private static final String KEY_CANCEL = "cancel";
 	private static final String KEY_DAY_CREATED = "day_created";
 	private static final String KEY_DAY_REGULATED = "day_regulated";
 	private static final String KEY_DOUBLE_ENTRY_ACCOUNT = "double_entry_account";
 	private static final String KEY_FROM = "from";
 	private static final String KEY_INVOICE_PERIODE = "invoice_periode";
 	private static final String KEY_NEW = "new";
 	private static final String KEY_EDIT_TOOLTIP = "edit";	
 	private static final String KEY_DELETE_TOOLTIP = "delete";		
 	private static final String KEY_NOTE = "note";
 
 	private static final String KEY_OWN_POSTING = "own_posting";
 	private static final String KEY_PERIODE = "periode";
 	private static final String KEY_PLACING = "placing";
 	private static final String KEY_REGULATION_TYPE = "regulation_type";
 	private static final String KEY_REMARK = "remark";
 	private static final String KEY_SAVE = "save";
 	private static final String KEY_SEARCH = "search";
 	private static final String KEY_SIGNATURE = "signature";
 	private static final String KEY_SSN = "ssn";	
 	private static final String KEY_TO = "to";
 	private static final String KEY_VAT_PR_MONTH = "vat_pr_month";
 	private static final String KEY_VAT_TYPE = "vattype";
 	private static final String KEY_NAME = "name";	
 	private static final String KEY_PROVIDER = "provider";
 	
 	
 	private static final String PAR_AMOUNT_PR_MONTH = KEY_AMOUNT_PR_MONTH;
 	private static final String PAR_DOUBLE_ENTRY_ACCOUNT  = KEY_DOUBLE_ENTRY_ACCOUNT;
 	private static final String PAR_FROM = KEY_FROM;
 	private static final String PAR_SEEK_FROM = "SEEK_" + KEY_FROM;	
 	private static final String PAR_PLACING = KEY_PLACING;
 	private static final String PAR_REGULATION_TYPE = KEY_REGULATION_TYPE;
 	private static final String PAR_REMARK = KEY_REMARK;
 	private static final String PAR_TO = KEY_TO;
 	private static final String PAR_SEEK_TO = "SEEK_" + KEY_TO;	
 	/** The current user. Used to set user in userSearcher (if not set) */
 	private static final String PAR_USER_SSN = "selected_user_pid"; 
 	private static final String PAR_OWN_POSTING = KEY_OWN_POSTING;	
 	private static final String PAR_VAT_PR_MONTH = KEY_VAT_PR_MONTH;
 	private static final String PAR_VAT_TYPE = KEY_VAT_TYPE;
 	private static final String PAR_PROVIDER = KEY_PROVIDER; 	
 	
 	private static final String PAR_PK = "pk";	
 	private static final String PAR_DELETE_PK = "delete_pk";
 	
 
 
 	private static final int 
 		ACTION_SHOW = 0, 
 		ACTION_NEW = 1, 
 		ACTION_DELETE = 2, 
 		ACTION_CANCEL = 3,
 		ACTION_EDIT = 4, 
 		ACTION_SEARCH_INVOICE = 5,
 		ACTION_SEARCH_REGULATION = 6,
 		ACTION_SAVE = 7,
 		ACTION_CANCEL_NEW_EDIT = 8,
 		ACTION_OPFIELD_DETAILSCREEN = 9,
 		ACTION_OPFIELD_MAINSCREEN = 10;
 			
 	private static final String PAR = "PARAMETER_";
 	private static final String 
 		PAR_NEW = PAR + ACTION_NEW, 
 		PAR_DELETE = PAR + ACTION_DELETE, 
 		PAR_CANCEL = PAR + ACTION_CANCEL,
 		PAR_EDIT = PAR + ACTION_EDIT, 
 		PAR_SEARCH_INVOICE = PAR + ACTION_SEARCH_INVOICE,
 		PAR_SAVE = PAR + ACTION_SAVE,
 		PAR_CANCEL_NEW_EDIT = PAR + ACTION_CANCEL_NEW_EDIT,
 		PAR_OPFIELD_DETAILSCREEN = PAR + ACTION_OPFIELD_DETAILSCREEN,
 		PAR_OPFIELD_MAINSCREEN = PAR + ACTION_OPFIELD_MAINSCREEN;
 
 private UserSearcher _userSearcher = null;
 	
 	public void init(final IWContext iwc) {
 		_userSearcher = getUserSearcherForm(iwc, getUser(iwc));
 		User user = _userSearcher.getUser();
 
 
 		String parFrom = iwc.getParameter(PAR_SEEK_FROM);
 		Date fromDate = parseDate(parFrom);
 
 		String parTo = iwc.getParameter(PAR_SEEK_TO);
 		Date toDate = parseDate(parTo);
 		
 		String errorMessage;
		boolean seekFromEmpty = parFrom == null || (parFrom != null && parFrom.length() == 0);
		boolean seekToEmpty = parTo == null || (parTo != null && parTo.length() == 0);
 		boolean seekFromEmptyOnly = seekFromEmpty && ! seekToEmpty;
 		boolean seekToEmptyOnly = seekToEmpty && ! seekFromEmpty;
 		boolean seekFromFormatError = ! seekFromEmpty && fromDate == null;
 		boolean seekToFormatError = ! seekToEmpty && toDate == null;
 		boolean seekNegativePeriode = toDate != null && fromDate != null && toDate.before(fromDate);
 		errorMessage =  
 			(seekFromFormatError ? localize(LOCALIZER_PREFIX + "date_format_from_error", "From format error") : 
 			(seekToFormatError ? localize(LOCALIZER_PREFIX + "date_format_to_error", "To format error") :
 			(seekFromEmptyOnly ? localize(LOCALIZER_PREFIX + "date_from_missing", "From missing") :
 			(seekToEmptyOnly ? localize(LOCALIZER_PREFIX + "date_to_missing", "To missing") : 
 			(seekNegativePeriode ? localize(LOCALIZER_PREFIX + "negative_periode", "Neagtive periode") :
 				null
 			)))));
 		
 		
 		if (errorMessage != null){
 			handleDefaultAction(iwc, user, fromDate, toDate, errorMessage);
 			return;
 		}		
 		
 		try {
 			int action = parseAction(iwc);
 			switch (action) {
 				case ACTION_SHOW:
 					handleDefaultAction(iwc, user, fromDate, toDate);
 					break;
 				case ACTION_NEW:
 					handleEditAction(iwc, getEmptyEntry(), user);
 					break;
 				case ACTION_DELETE:
 					handleDeleteAction(iwc);
 					handleDefaultAction(iwc, user, fromDate, toDate);
 					break;
 				case ACTION_CANCEL:
 					// TODO implement
 					break;
 				case ACTION_SEARCH_INVOICE:	
 					handleDefaultAction(iwc, user, fromDate, toDate, (seekFromEmpty && seekToEmpty) ? localize(LOCALIZER_PREFIX + "date_empty", "No date present") : null);						
 					break;
 				case ACTION_CANCEL_NEW_EDIT:				
 				case ACTION_OPFIELD_MAINSCREEN:
 					handleDefaultAction(iwc, user, fromDate, toDate);
 					break;
 				case ACTION_EDIT:
 				case ACTION_OPFIELD_DETAILSCREEN:				
 					handleEditAction(iwc, user);
 					break;
 				case ACTION_SEARCH_REGULATION:
 //					handleSearchRegulation(iwc);
 					break;	
 				case ACTION_SAVE:
 					handleSaveAction(iwc, user);
 					break;	
 				
 				default:
 					handleDefaultAction(iwc, user, fromDate, toDate);				
 					
 			}
 		}
 		catch (Exception e) {
 			add(new ExceptionWrapper(e, this));
 		}
 	}
 
 
 	/**
 	 * @param iwc
 	 */
 	private Collection doInvoiceSearch(IWContext iwc, User user, Date from, Date to) {
 		Collection invoices = new ArrayList();		
 		if (user != null && from != null && to != null){
 
 			RegularInvoiceBusiness invoiceBusiness = getRegularInvoiceBusiness(iwc);
 
 			if (user != null){
 				try{
 					invoices = invoiceBusiness.findRegularInvoicesForPeriodeAndUser(from, to, user.getNodeID());
 				}catch(FinderException ex){
 					ex.printStackTrace(); 
 				}catch(IDOLookupException ex){
 					ex.printStackTrace(); 
 				}
 			}
 		}
 
 		return invoices;
 	}
 	
 	private User getUser(IWContext iwc){
 		String userPid = iwc.getParameter(PAR_USER_SSN);
 		User user = null;
 		try{
 			user = getUserBusiness(iwc.getApplicationContext()).getUser(userPid);
 		}catch(FinderException ex){
 			ex.printStackTrace(); 
 		}
 		return user;	
 	}
 
 	/*
 	 * Returns the action constant for the action to perform based 
 	 * on the POST parameters in the specified context.
 	 */
 	private int parseAction(IWContext iwc) {
 		
 		int action = ACTION_SHOW;
 		for (int a = 0; a <= 20; a++){ 
 			if (iwc.isParameterSet(PAR + a)){
 				action = a;
 				break;					
 			}
 		}
 		return action;
 	}	
 
 
 	private void handleDeleteAction(IWContext iwc){
 		RegularInvoiceEntry entry = getRegularInvoiceEntry(iwc.getParameter(PAR_PK));
 		try{
 			entry.delete();
 		} catch(SQLException ex){
 			ex.printStackTrace();
 		}
 	}
 	
 	private RegularInvoiceBusiness getRegularInvoiceBusiness(IWContext iwc){
 		RegularInvoiceBusiness invoiceBusiness = null;
 		try{
 			invoiceBusiness = (RegularInvoiceBusiness)IBOLookup.getServiceInstance(iwc, RegularInvoiceBusiness.class);
 		}catch(RemoteException ex){
 			ex.printStackTrace();	
 			return null;		
 		}		
 		return invoiceBusiness;
 	}
 
 		
 	
 	private void handleSaveAction(IWContext iwc, User user){
 		RegularInvoiceEntry entry = getRegularInvoiceEntry(iwc.getParameter(PAR_PK));
 		
 		if (entry == null){
 			try{
 				entry = getRegularInvoiceEntryHome().create();
 				entry.setCreatedDate(new Date(new java.util.Date().getTime()));
 				entry.setCreatedSign(iwc.getCurrentUser().getName());				
 			}catch(CreateException ex2){
 				ex2.printStackTrace();
 				return;
 			}			
 		}else{
 			entry.setEditDate(new Date(new java.util.Date().getTime()));
 			entry.setEditSign(iwc.getCurrentUser().getName());
 		}
 		
 		entry.setAmount(new Float(iwc.getParameter(PAR_AMOUNT_PR_MONTH)).floatValue());
 		Date from = parseDate(iwc.getParameter(PAR_FROM));
 		Date to = parseDate(iwc.getParameter(PAR_TO));
 		entry.setFrom(from);
 		entry.setTo(to);
 			
 		entry.setNote(iwc.getParameter(PAR_REMARK));
 		entry.setPlacing(iwc.getParameter(PAR_PLACING));
 		entry.setVAT(new Float(iwc.getParameter(PAR_VAT_PR_MONTH)).floatValue());
 		entry.setPlacing(iwc.getParameter(PAR_PLACING));
 		if (iwc.getParameter(PAR_PROVIDER) != null){
 			entry.setSchoolId(new Integer(iwc.getParameter(PAR_PROVIDER)).intValue());
 		}
 		entry.setRegSpecTypeId(new Integer(iwc.getParameter(PAR_REGULATION_TYPE)).intValue());
 		entry.setUser(user);
 		entry.setVatRegulationId(new Integer(iwc.getParameter(PAR_VAT_TYPE)).intValue());
 		entry.setOwnPosting(iwc.getParameter(PAR_OWN_POSTING));
 		entry.setDoublePosting(iwc.getParameter(PAR_DOUBLE_ENTRY_ACCOUNT));
 		
 		if (from != null && to != null){
 			entry.store();		
 			handleDefaultAction(iwc, user, parseDate(iwc.getParameter(PAR_SEEK_FROM)), parseDate(iwc.getParameter(PAR_SEEK_TO)));					
 		} else {
 			handleEditAction(iwc, entry, user, localize(LOCALIZER_PREFIX + "date_format_yymm_warning", "Wrong date format. use: yymm."));	
 		}
 	}
 
 	private RegularInvoiceEntry getRegularInvoiceEntry(String pk) {
 		RegularInvoiceEntryHome home = getRegularInvoiceEntryHome();
 		RegularInvoiceEntry entry = null;
 		if (home != null){
 			try{
 				entry = home.findByPrimaryKey(pk);
 			}catch(FinderException ex){
 				ex.printStackTrace();
 			}
 		}
 		return entry;
 	}
 
 	private RegularInvoiceEntryHome getRegularInvoiceEntryHome() {
 		RegularInvoiceEntryHome home = null;
 		try{
 			home = (RegularInvoiceEntryHome) IDOLookup.getHome(RegularInvoiceEntry.class);
 			
 		}catch(IDOLookupException ex){
 			ex.printStackTrace();			
 		}
 		return home;
 	}
 		
 	private void handleEditAction(IWContext iwc, User user){
 		handleEditAction(iwc, user, null);
 				
 	}
 	private void handleEditAction(IWContext iwc, User user, String errorMessage){
 		RegularInvoiceEntry entry = null;
 				
 		if (errorMessage != null){
 			entry = getNotStoredEntry(iwc);
 		} else {
 			try{
 				entry = getRegularInvoiceEntryHome().findByPrimaryKey(iwc.getParameter(PAR_PK));
 			}catch(FinderException ex){
 				entry = getNotStoredEntry(iwc);
 			}
 		}
 	
 		handleEditAction(iwc, entry, user, errorMessage);
 	}
 
 
 	private void handleEditAction(IWContext iwc, RegularInvoiceEntry entry, User user){
 		handleEditAction(iwc, entry, user, null);
 	}
 
 			
 	private void handleEditAction(IWContext iwc, RegularInvoiceEntry entry, User user, String errorMessage){
 		Collection regTypes = new ArrayList(), vatTypes = new ArrayList();
 		try {
 			regTypes = getRegulationsBusiness(iwc.getApplicationContext()).findAllRegulationSpecTypes();
 			vatTypes = getVATBusiness(iwc.getApplicationContext()).findAllVATRegulations();				
 		} catch (RegulationException e1) {
 			e1.printStackTrace();
 		} catch (RemoteException e1) {
 			e1.printStackTrace();
 		}	
 		
 		Form form = new Form();
 		if (entry != null){
 			form.add(new HiddenInput(PAR_PK, ""+entry.getPrimaryKey()));
 		}
 		if (user != null){
 			form.add(new HiddenInput(PAR_USER_SSN, user.getPersonalID()));	
 		}
 		form.maintainParameter(PAR_SEEK_FROM);
 		form.maintainParameter(PAR_SEEK_TO);
 		
 		form.add(getDetailPanel(iwc, user, entry, regTypes, vatTypes, errorMessage));
 		
 		add(form);
 	}
 
 		
 	private void handleDefaultAction(IWContext iwc, User user, Date fromDate, Date toDate, String errorMessage){
 		add(getEntryListPage(doInvoiceSearch(iwc, user, fromDate, toDate), user, fromDate, toDate, errorMessage));
 	}
 			
 	private void handleDefaultAction(IWContext iwc, User user, Date fromDate, Date toDate){
 		add(getEntryListPage(doInvoiceSearch(iwc, user, fromDate, toDate), user, fromDate, toDate));
 	}
 
 	
 	private Table getEntryListPage(Collection entries, User user, Date fromDate, Date toDate){
 		return getEntryListPage(entries, user, fromDate, toDate, null);
 	}
 	
 	private Table getEntryListPage(Collection entries, User user, Date fromDate, Date toDate, String errorMessage){
 		
 		Table t1 = new Table();
 		
 		t1.setCellpadding(getCellpadding());
 		t1.setCellspacing(getCellspacing());
 
 		t1.add(getOperationalFieldPanel(PAR_OPFIELD_MAINSCREEN, user), 1, 1); 
 		
 		Form form = new Form();			
 		Table t2 = new Table();
 		int row = 1;
 		UserSearcher searcher = _userSearcher;
 		
 		t2.add(searcher, 1, row++);
 		if (user != null){
 			t2.add(getPeriodeForm(fromDate, toDate, errorMessage), 1, row++);
 			t2.add(getInvoiceList(entries, user, fromDate, toDate), 1, row++);
 		
 			ButtonPanel bp = new ButtonPanel(this);
 			bp.addLocalizedButton(PAR_NEW, KEY_NEW, "New");
 			bp.addLocalizedButton(PAR_CANCEL, KEY_CANCEL, "Cancel");
 			t2.add(bp, 1, row++);
 
 			form.add(new HiddenInput(PAR_DELETE_PK, "-1"));
 		}
 		
 		if (user != null){
 			form.add(new HiddenInput(PAR_USER_SSN, user.getPersonalID()));
 		}
 		
 		form.add(t2);		
 		t1.add(form, 1, 2);
 
 		return t1;		
 	}
 	
 
 	private Table getOperationalFieldPanel(String actionCommand, User user) {
 		
 		Table inner = new Table();
 
 		inner.add(getLocalizedLabel(KEY_OPERATIONAL_FIELD, "Huvudverksamhet"), 1, 1);
 		OperationalFieldsMenu ofm = new OperationalFieldsMenu();
 		if (user != null){
 			ofm.setParameter(PAR_USER_SSN, user.getPersonalID());
 		}
 		ofm.maintainParameter(PAR_SEEK_FROM);
 		ofm.maintainParameter(PAR_SEEK_TO);
 	
 		inner.add(ofm, 2, 1);
 		inner.add(new HiddenInput(actionCommand, " ")); //to make it return to the right page
 		return inner;
 	}	
 	
 	private UserSearcher getUserSearcherForm(IWContext iwc, User currentUser){
 	
 		UserSearcher searcher = new UserSearcher();
 		searcher.setPersonalIDLength(15);
 		searcher.setFirstNameLength(25);
 		searcher.setLastNameLength(25);
 		searcher.setShowMiddleNameInSearch(false);
 		searcher.setOwnFormContainer(false);
 		searcher.setUniqueIdentifier("");
 		searcher.setBelongsToParent(true);
 		searcher.setConstrainToUniqueSearch(false);
 		
 		searcher.setTextFontStyle(getTextFontStyle());
 
 		if (iwc.getParameter(PAR_SEEK_FROM) != null){
 			searcher.maintainParameter(new Parameter(PAR_SEEK_FROM, iwc.getParameter(PAR_SEEK_FROM)));		
 		}
 		if (iwc.getParameter(PAR_SEEK_TO) != null){
 			searcher.maintainParameter(new Parameter(PAR_SEEK_TO, iwc.getParameter(PAR_SEEK_TO)));		
 		}
 		
 		searcher.setToFormSubmit(true);	
 		
 		try{
 			searcher.process(iwc);	
 			if (searcher.getUser() == null && ! searcher.isHasManyUsers() && ! searcher.isClearedButtonPushed(iwc)){
 				searcher.setUser(currentUser);
 			}			
 		} catch (FinderException ex){
 			
 			ex.printStackTrace();
 		} catch (RemoteException ex){
 			ex.printStackTrace();			
 		}
 		return searcher;
 	}
 	
 
 	private Table getPeriodeForm(Date fromDate, Date toDate, String errorMessage){
 			
 
 		Table formTable = new Table();
 		formTable.add(getLocalizedLabel("KEY_PERIODE", "Periode"), 1, 2);
 		TextInput from = getTextInput(PAR_SEEK_FROM, "");
 		from.setLength(4);
 		if (fromDate != null){
 			from.setContent(formatDate(fromDate, 4));	
 		}	
 		
 		TextInput to = getTextInput(PAR_SEEK_TO, "");
 		to.setLength(4);
 		if (toDate != null){
 			to.setContent(formatDate(toDate, 4));	
 		}
 
 		if (errorMessage != null){
 			formTable.mergeCells(1, 1, 10, 1);
 			formTable.add(getErrorText(errorMessage), 1, 1);
 		}
 		
 		formTable.add(from, 2, 2);
 		formTable.add(getText(" - "), 2, 2);	
 		formTable.add(to, 2, 2);			
 
 		formTable.add(getLocalizedButton(PAR_SEARCH_INVOICE, KEY_SEARCH, "Search"), 10, 2);
 
 		return formTable;	
 	}	
 	
 	
 	private ListTable getInvoiceList(Collection invoices, User user, Date fromDate, Date toDate) {
 		
 		ListTable list = new ListTable(this, 6);
 		
 		list.setLocalizedHeader(KEY_INVOICE_PERIODE, "Invoice periode", 1);
 		list.setLocalizedHeader(KEY_PLACING, "Placing", 2);
 		list.setLocalizedHeader(KEY_AMOUNT, "Amount", 3);
 		list.setLocalizedHeader(KEY_NOTE, "Note", 4);
 		list.setHeader(" ",  5);
 		list.setHeader(" ",  6);
 		
 		try {
 			if (invoices != null) {
 				Iterator i = invoices.iterator();
 				while (i.hasNext()) {
 					RegularInvoiceEntry entry = (RegularInvoiceEntry) i.next();
 					list.add(getText(""+entry.getFrom() + " - " + entry.getTo()));
 					
 					Link link = getLink(entry.getPlacing(), PAR_PK, "" + entry.getPrimaryKey());
 					link.setParameter(PAR_EDIT, " ");
 					link.setParameter(PAR_SEEK_FROM, formatDate(fromDate, 4));
 					link.setParameter(PAR_SEEK_TO, formatDate(toDate, 4));
 					link.setParameter(PAR_USER_SSN, user.getPersonalID());
 					list.add(link);
 					
 					list.add(getText(""+entry.getAmount()));
 					list.add(getText(entry.getNote()));
 
 					Link edit = new Link(getEditIcon(localize(KEY_EDIT_TOOLTIP, "Edit")));
 					edit.addParameter(PAR_EDIT, " ");
 					edit.addParameter(PAR_PK, entry.getPrimaryKey().toString());
 					edit.setParameter(PAR_SEEK_FROM, formatDate(fromDate, 4));
 					edit.setParameter(PAR_SEEK_TO, formatDate(toDate, 4));					
 					edit.addParameter(PAR_USER_SSN, user.getPersonalID());
 					list.add(edit);
 					
 					Link delete = new Link(getDeleteIcon(localize(KEY_DELETE_TOOLTIP, "Delete")));
 					delete.addParameter(PAR_DELETE, " ");
 					delete.addParameter(PAR_PK, entry.getPrimaryKey().toString());
 					delete.setParameter(PAR_SEEK_FROM, formatDate(fromDate, 4));
 					delete.setParameter(PAR_SEEK_TO, formatDate(toDate, 4));					
 					delete.addParameter(PAR_USER_SSN, user.getPersonalID());
 					delete.setOnClick("return confirm('Confirm deletion');");
 					list.add(delete);
 				}
 			}			
 		} catch (Exception e) {
 			super.add(new ExceptionWrapper(e, this));
 		}	
 		list.setColumnAlignment(3, Table.HORIZONTAL_ALIGN_RIGHT);
 		list.setColumnAlignment(5, Table.HORIZONTAL_ALIGN_RIGHT);
 		return list;
 	}
 	
 	private Table getDetailPanel(IWContext iwc, User user, RegularInvoiceEntry entry, Collection regTypes, Collection vatTypes, String errorMessage){
 				
 		final int EMPTY_ROW_HEIGHT = 8;
 		Table table = new Table();
 		int row = 1;
 		table.mergeCells(1,1,3,1);
 		table.add(getOperationalFieldPanel(PAR_OPFIELD_DETAILSCREEN, user), 1, row++);
 		
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 				
 		addField(table, KEY_SSN, user.getPersonalID(), 1, row);
 		addField(table, KEY_NAME, user.getLastName() + ", " + user.getFirstName(), 3, row++);
 		
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 				
 		RegulationSearchPanel searchPanel = new RegulationSearchPanel(iwc);
 		
 		searchPanel.maintainParameter(new String[]{PAR_USER_SSN, PAR_FROM, PAR_TO, PAR_AMOUNT_PR_MONTH, PAR_SEEK_FROM, PAR_SEEK_TO, PAR_PK});
 		searchPanel.setPlacingIfNull(entry.getPlacing());
 		searchPanel.setSchoolIdIfNull(entry.getSchoolId());
 		
 		searchPanel.setParameter(PAR_EDIT, " ");
 		searchPanel.maintainAllParameters();
 		table.mergeCells(1, row, 10, row);
 		table.add(searchPanel, 1, row++);
 
 		Regulation reg = searchPanel.getRegulation(); 
 		
 		if (reg != null){
 			entry = getNotStoredEntry(iwc, reg, searchPanel.getPosting());
 		}
 
 
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 		
 		if (errorMessage != null){
 			table.add(getErrorText(errorMessage), 1, row++);			
 		}
 		table.add(getLocalizedLabel(KEY_PERIODE, "Periode:"), 1, row);
 		TextInput fromInput = getTextInput(PAR_FROM, KEY_FROM);
 		fromInput.setContent(formatDate(entry.getFrom(), 4));
 		fromInput.setLength(4);
 		table.add(fromInput, 2, row);
 		table.add(getText(" - "), 2, row);
 		TextInput toInput = getTextInput(PAR_TO, KEY_TO);
 		toInput.setContent(formatDate(entry.getTo(), 4));		
 		toInput.setLength(4);
 		table.add(toInput, 2, row++);
 
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 		
 		addField(table, KEY_DAY_CREATED, formatDate(entry.getCreatedDate(), 6), 1, row);
 		addField(table, KEY_SIGNATURE, entry.getCreatedName(), 4, row++);
 		addField(table, KEY_DAY_REGULATED, formatDate(entry.getEditDate(), 6), 1, row);
 		addField(table, KEY_SIGNATURE, entry.getEditName(), 4, row++);
 
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 
 		addFloatField(table, PAR_AMOUNT_PR_MONTH, KEY_AMOUNT_PR_MONTH, ""+ entry.getAmount(), 1, row++);
 		addFloatField(table, PAR_VAT_PR_MONTH, KEY_VAT_PR_MONTH, ""+entry.getVAT(), 1, row++);
 
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 
 		addField(table, PAR_REMARK, KEY_REMARK, entry.getNote(), 1, row++);
 
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 
 		addDropDown(table, PAR_REGULATION_TYPE, KEY_REGULATION_TYPE, regTypes, entry.getRegSpecTypeId(), "getRegSpecType", 1, row++);
 		table.mergeCells(1, row, 10, row);
 		PostingBlock postingBlock = new PostingBlock(entry.getOwnPosting(), entry.getDoublePosting());
 		if (entry.getOwnPosting() == null && entry.getDoublePosting() == null){
 			try{
 				postingBlock.generateStrings(iwc);
 			}catch(Exception ex){
 				ex.printStackTrace();
 			}
 		}
 		table.add(postingBlock, 1, row++);
 		addDropDown(table, PAR_VAT_TYPE, KEY_VAT_TYPE, vatTypes, entry.getVatRegulationId(),  "getDescription", 1, row++);
 		
 		table.setHeight(row++, EMPTY_ROW_HEIGHT);
 				
 		ButtonPanel bp = new ButtonPanel(this);
 		bp.addLocalizedButton(PAR_SAVE, KEY_SAVE, "Save");
 		bp.addLocalizedButton(PAR_CANCEL_NEW_EDIT, KEY_CANCEL, "Delete");
 		table.add(bp, 1, row);
 		
 		return table;
 	}
 
 	private RegularInvoiceEntry getEmptyEntry() {
 		return getNotStoredEntry(null, null, null);		
 	}
 	
 	private RegularInvoiceEntry getNotStoredEntry(IWContext iwc) {
 		return getNotStoredEntry(iwc, null, null);
 	}
 	
 	private RegularInvoiceEntry getNotStoredEntry(IWContext iwc, Regulation reg, String[] posting) {
 		final IWContext _iwc = iwc;
 		final Regulation _reg = reg;
 		final String[] _posting = posting;
 		
 		return new RegularInvoiceEntry() {
 		
 			public Date getFrom() {
 				return _reg != null ? _reg.getPeriodFrom() : getDateValue(PAR_FROM);
 			}
 		
 			public Date getTo() {
 				return _reg != null ? _reg.getPeriodTo() : getDateValue(PAR_TO);
 			}
 		
 			public String getPlacing() {
 				return _reg != null ? _reg.getName() : getValue(PAR_PLACING);
 			}
 		
 			public User getUser() {
 				return null;
 			}
 		
 			public RegulationSpecType getRegSpecType() {
 				return _reg != null ? _reg.getRegSpecType() : null;
 			}
 		
 			public int getRegSpecTypeId() {
 				return _reg != null ? new Integer(""+_reg.getRegSpecType().getPrimaryKey()).intValue() : getIntValue(PAR_REGULATION_TYPE);
 			}
 		
 			public School getSchool() {
 				return null;
 			}
 		
 			public int getSchoolId() {
 				return getIntValue(PAR_PROVIDER);
 			}
 		
 			public String getOwnPosting() {
 				if (_posting != null && _posting.length >= 1){
 					return _posting[0];
 				}
 				return getValue(PAR_OWN_POSTING);
 			}
 		
 			public String getDoublePosting() {
 				if (_posting != null && _posting.length >= 2){
 					return _posting[1];
 				}				
 				return getValue(PAR_DOUBLE_ENTRY_ACCOUNT);
 			}
 		
 			public float getAmount() {
 				return _reg != null ? _reg.getAmount().floatValue() : getFloatValue(PAR_AMOUNT_PR_MONTH);
 			}
 		
 			public float getVAT() {
 				
 				return _reg != null ? _reg.getVATEligible().floatValue() : getFloatValue(PAR_VAT_PR_MONTH);
 			}
 		
 			public VATRegulation getVatRegulation() {
 				return null;
 			}
 		
 			public int getVatRegulationId() {
 				return getIntValue(PAR_VAT_TYPE);
 			}
 		
 			public String getNote() {
 				return getValue(PAR_REMARK);
 			}
 		
 			public Date getCreatedDate() {
 				return null;
 			}
 		
 			public String getCreatedName() {
 				return null;
 			}
 		
 			public Date getEditDate() {
 				return null;
 			}
 		
 			public String getEditName() {
 				return null;
 			}
 			
 			String getValue(String parameter){
 				return _iwc == null || _iwc.getParameter(parameter) == null ? "" : _iwc.getParameter(parameter);
 			}				
 			
 			int getIntValue(String parameter){
 				try {
 					return _iwc == null || _iwc.getParameter(parameter) == null ? 0 : new Integer(_iwc.getParameter(parameter)).intValue();
 				} catch (NumberFormatException ex){
 					return 0;
 				}
 			}
 			
 			float getFloatValue(String parameter){
 				try {
 					return _iwc == null || _iwc.getParameter(parameter) == null ? 0 : new Float(_iwc.getParameter(parameter)).floatValue();
 				} catch (NumberFormatException ex){
 					return 0;
 				}
 			}
 			Date getDateValue(String parameter){
 				return _iwc == null || _iwc.getParameter(parameter) == null ? null : parseDate(_iwc.getParameter(parameter));
 			}
 						
 //dummy implementations - methods not used.
 			public void setFrom(Date from) {}
 			public void setTo(Date to) {}
 			public void setPlacing(String plascint) {}
 			public void setUser(User user) {}
 			public void setRegSpecType(RegulationSpecType regType) {}
 			public void setRegSpecTypeId(int regTypeId) {}
 			public void setSchoolId(int schoolId) {}
 			public void setAmount(float amount) {}
 			public void setVAT(float vat) {}
 			public void setVatRegulation(VATRegulation vatRegulation) {}
 			public void setVatRegulationId(int vatRegId) {}
 			public void setNote(String note) {}
 			public void setOwnPosting(String ownPosting) {}
 			public void setDoublePosting(String doublePosting) {}
 			public void setCreatedDate(Date date) {}
 			public void setCreatedSign(String name) {}
 			public void setEditDate(Date date) {}
 			public void setEditSign(String name) {}
 			public void delete() throws SQLException {}
 			public void store() throws IDOStoreException {}
 			public IDOEntityDefinition getEntityDefinition() {return null;}
 			public EJBLocalHome getEJBLocalHome() throws EJBException {return null;}
 			public Object getPrimaryKey() throws EJBException {return null;}
 			public void remove() throws RemoveException, EJBException {}
 			public boolean isIdentical(EJBLocalObject arg0) throws EJBException {return false;}
 			public int compareTo(Object arg0) {return 0;}
 		};
 	}
 	
 
 
 	/**
 	 * @param table
 	 * @param KEY_REGULATION_TYPE
 	 * @param regulationType
 	 * @param options
 	 * @param PAR_REGULATION_TYPE
 	 * @param i
 	 * @param j
 	 */
 	private Table addDropDown(Table table, String parameter, String key, Collection options, int selected, String method, int col, int row) {
 		DropdownMenu dropDown = getDropdownMenu(parameter, options, method);
 		dropDown.setSelectedElement(selected);
 		return addWidget(table, key, dropDown, col, row);		
 	}
 
 
 	
 	/**
 	 * Adds a label and a TextInput to a table
 	 * @param table
 	 * @param key is used both as localization key for the label and default label value
 	 * @param value
 	 * @param parameter
 	 * @param col
 	 * @param row
 	 * @return
 	 */
 	private Table addField(Table table, String parameter, String key, String value, int col, int row){
 		return addWidget(table, key, getTextInput(parameter, value), col, row);
 	}
 	
 	/**
 	 * Adds a label and a TextInput to a table
 	 * @param table
 	 * @param key is used both as localization key for the label and default label value
 	 * @param value
 	 * @param parameter
 	 * @param col
 	 * @param row
 	 * @return
 	 */
 	private Table addFloatField(Table table, String parameter, String key, String value, int col, int row){
 		TextInput input = getTextInput(parameter, value);
 		input.setAsFloat(localize(LOCALIZER_PREFIX + "float_format_error", "Format-error: Expecting float:" )+ " " + localize(key, "") ); 
 		return addWidget(table, key, input, col, row);
 	}
 		
 
 	/**
 	 * Adds a label and a value to a table
 	 * @param table
 	 * @param key is used both as localization key for the label and default label value
 	 * @param value
 	 * @param col
 	 * @param row
 	 * @return
 	 */
 	private Table addField(Table table, String key, String value, int col, int row){
 		return addWidget(table, key, getText(value), col, row);
 	}	
 	
 	/**
 	 * Adds a label and widget to a table
 	 * @param table
 	 * @param key is used both as localization key for the label and default label value
 	 * @param widget
 	 * @param col
 	 * @param row
 	 * @return
 	 */
 	private Table addWidget(Table table, String key, PresentationObject widget, int col, int row){
 		table.add(getLocalizedLabel(key, key), col, row);
 		table.add(widget, col + 1, row);
 		return table;
 	
 	}		
 	
 	protected RegulationsBusiness getRegulationsBusiness(IWApplicationContext iwc) throws RemoteException {
 		return (RegulationsBusiness) IBOLookup.getServiceInstance(iwc, RegulationsBusiness.class);
 	}	
 	
 	protected VATBusiness getVATBusiness(IWApplicationContext iwc) throws RemoteException {
 		return (VATBusiness) IBOLookup.getServiceInstance(iwc, VATBusiness.class);
 	}		
 	
 	
 
 }
