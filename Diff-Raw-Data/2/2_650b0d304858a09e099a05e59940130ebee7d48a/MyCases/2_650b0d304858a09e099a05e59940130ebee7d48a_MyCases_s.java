 /*
  * $Id$
  * Created on Nov 7, 2005
  *
  * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
  *
  * This software is the proprietary information of Idega hf.
  * Use is subject to license terms.
  */
 package is.idega.idegaweb.egov.cases.presentation;
 
 import is.idega.idegaweb.egov.cases.data.CaseCategory;
 import is.idega.idegaweb.egov.cases.data.CaseType;
 import is.idega.idegaweb.egov.cases.data.GeneralCase;
 
 import java.rmi.RemoteException;
 import java.util.Collection;
 
 import javax.ejb.FinderException;
 
 import com.idega.business.IBORuntimeException;
 import com.idega.presentation.IWContext;
 import com.idega.presentation.Layer;
 import com.idega.presentation.text.Heading1;
 import com.idega.presentation.text.Link;
 import com.idega.presentation.text.Text;
 import com.idega.presentation.ui.DropdownMenu;
 import com.idega.presentation.ui.Form;
 import com.idega.presentation.ui.Label;
 import com.idega.presentation.ui.TextArea;
 import com.idega.presentation.ui.util.SelectorUtility;
 import com.idega.user.data.User;
 import com.idega.util.IWTimestamp;
 import com.idega.util.text.TextSoap;
 
 
 public class MyCases extends CasesProcessor {
 
 	private static final String PARAMETER_CASE_CATEGORY_PK = "prm_case_category_pk";
 	private static final String PARAMETER_CASE_TYPE_PK = "prm_case_type_pk";
 	private static final String PARAMETER_REPLY = "prm_reply";
 	private static final String PARAMETER_STATUS = "prm_status";
 
 	protected String getBlockID() {
 		return "myCases";
 	}
 
 	protected Collection getCases(User user) throws RemoteException {
 		return getBusiness().getMyCases(user);
 	}
 	
 	protected void showProcessor(IWContext iwc, Object casePK) throws RemoteException {
 		Form form = new Form();
 		form.setStyleClass("adminForm");
 		form.maintainParameter(PARAMETER_CASE_PK);
 		form.addParameter(PARAMETER_ACTION, "");
 		
 		GeneralCase theCase = null;
 		try {
 			theCase = getBusiness().getGeneralCase(casePK);
 		}
 		catch (FinderException fe) {
 			fe.printStackTrace();
 			throw new IBORuntimeException(fe);
 		}
 		CaseCategory category = theCase.getCaseCategory();
 		CaseType type = theCase.getCaseType();
 		User owner = theCase.getOwner();
 		IWTimestamp created = new IWTimestamp(theCase.getCreated());
 		
 		form.add(getPersonInfo(iwc, owner));
 		
 		Heading1 heading = new Heading1(getResourceBundle().getLocalizedString("case_overview", "Case overview"));
 		heading.setStyleClass("subHeader");
 		heading.setStyleClass("topSubHeader");
 		form.add(heading);
 		
 		Layer section = new Layer(Layer.DIV);
 		section.setStyleClass("formSection");
 		form.add(section);
 		
 		Layer clear = new Layer(Layer.DIV);
 		clear.setStyleClass("Clear");
 
 		SelectorUtility util = new SelectorUtility();
 		DropdownMenu categories = (DropdownMenu) util.getSelectorFromIDOEntities(new DropdownMenu(PARAMETER_CASE_CATEGORY_PK), getBusiness().getCaseCategories(), "getName");
 		categories.keepStatusOnAction(true);
 		categories.setSelectedElement(category.getPrimaryKey().toString());
 		categories.setStyleClass("caseCategoryDropdown");
 		
 		DropdownMenu types = (DropdownMenu) util.getSelectorFromIDOEntities(new DropdownMenu(PARAMETER_CASE_TYPE_PK), getBusiness().getCaseTypes(), "getName");
 		types.keepStatusOnAction(true);
 		types.setSelectedElement(type.getPrimaryKey().toString());
 		types.setStyleClass("caseTypeDropdown");
 		
 		DropdownMenu statuses = new DropdownMenu(PARAMETER_STATUS);
 		statuses.addMenuElement(getBusiness().getCaseStatusPending().getStatus(), getBusiness().getLocalizedCaseStatusDescription(getBusiness().getCaseStatusPending(), iwc.getCurrentLocale()));
 		statuses.addMenuElement(getBusiness().getCaseStatusWaiting().getStatus(), getBusiness().getLocalizedCaseStatusDescription(getBusiness().getCaseStatusWaiting(), iwc.getCurrentLocale()));
 		statuses.addMenuElement(getBusiness().getCaseStatusReady().getStatus(), getBusiness().getLocalizedCaseStatusDescription(getBusiness().getCaseStatusReady(), iwc.getCurrentLocale()));
 		statuses.setSelectedElement(theCase.getStatus());
 		statuses.setStyleClass("caseStatusDropdown");
 		
 		Layer message = new Layer(Layer.SPAN);
 		message.add(new Text(TextSoap.formatText(theCase.getMessage())));
 		
 		Layer createdDate = new Layer(Layer.SPAN);
 		createdDate.add(new Text(created.getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)));
 		
 		TextArea reply = new TextArea(PARAMETER_REPLY);
 		reply.setStyleClass("textarea");
 		reply.keepStatusOnAction(true);
 		
 		Layer element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		Label label = new Label(getResourceBundle().getLocalizedString("case_category", "Case category"), categories);
 		element.add(label);
 		element.add(categories);
 		section.add(element);
 
 		element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		label = new Label(getResourceBundle().getLocalizedString("case_type", "Case type"), types);
 		element.add(label);
 		element.add(types);
 		section.add(element);
 
 		element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		label = new Label();
 		label.setLabel(getResourceBundle().getLocalizedString("created_date", "Created date"));
 		element.add(label);
 		element.add(createdDate);
 		section.add(element);
 
 		element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		element.setStyleClass("informationItem");
 		label = new Label();
 		label.setLabel(getResourceBundle().getLocalizedString("message", "Message"));
 		element.add(label);
 		element.add(message);
 		section.add(element);
 
 		section.add(clear);
 		
		heading = new Heading1(getResourceBundle().getLocalizedString("case_overview", "Case overview"));
 		heading.setStyleClass("subHeader");
 		form.add(heading);
 		
 		section = new Layer(Layer.DIV);
 		section.setStyleClass("formSection");
 		form.add(section);
 		
 		element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		label = new Label(getResourceBundle().getLocalizedString("status", "status"), statuses);
 		element.add(label);
 		element.add(statuses);
 		section.add(element);
 
 		element = new Layer(Layer.DIV);
 		element.setStyleClass("formItem");
 		label = new Label(getResourceBundle().getLocalizedString("reply", "Reply"), reply);
 		element.add(label);
 		element.add(reply);
 		section.add(element);
 
 		section.add(clear);
 
 		Layer bottom = new Layer(Layer.DIV);
 		bottom.setStyleClass("bottom");
 		form.add(bottom);
 
 		Link next = getButtonLink(getResourceBundle().getLocalizedString("process", "Process"));
 		next.setValueOnClick(PARAMETER_ACTION, String.valueOf(ACTION_SAVE));
 		next.setToFormSubmit(form);
 		bottom.add(next);
 
 		Link back = getButtonLink(getResourceBundle().getLocalizedString("back", "Back"));
 		back.setValueOnClick(PARAMETER_ACTION, String.valueOf(ACTION_VIEW));
 		back.setToFormSubmit(form);
 		bottom.add(back);
 
 		add(form);
 	}
 	
 	protected void save(IWContext iwc) throws RemoteException {
 		Object casePK = iwc.getParameter(PARAMETER_CASE_PK);
 		Object caseCategoryPK = iwc.getParameter(PARAMETER_CASE_CATEGORY_PK);
 		Object caseTypePK = iwc.getParameter(PARAMETER_CASE_TYPE_PK);
 		String status = iwc.getParameter(PARAMETER_STATUS);
 		String reply = iwc.getParameter(PARAMETER_REPLY);
 		
 		try {
 			getBusiness().handleCase(casePK, caseCategoryPK, caseTypePK, status, iwc.getCurrentUser(), reply, iwc.getCurrentLocale());
 		}
 		catch (FinderException fe) {
 			fe.printStackTrace();
 		}
 	}
 }
