 /*
  * Created on 3.5.2004
  */
 package se.idega.idegaweb.commune.school.music.presentation;
 
 import java.rmi.RemoteException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import javax.ejb.CreateException;
 import javax.ejb.FinderException;
 import se.idega.idegaweb.commune.care.business.CareBusiness;
 import se.idega.idegaweb.commune.school.music.business.InstrumentComparator;
 import se.idega.idegaweb.commune.school.music.business.NoDepartmentFoundException;
 import se.idega.idegaweb.commune.school.music.business.NoInstrumentFoundException;
 import se.idega.idegaweb.commune.school.music.business.NoLessonTypeFoundException;
 import se.idega.idegaweb.commune.school.music.data.MusicSchoolChoice;
 import se.idega.idegaweb.commune.school.music.event.MusicSchoolEventListener;
 import com.idega.block.navigation.presentation.UserHomeLink;
 import com.idega.block.school.data.School;
 import com.idega.block.school.data.SchoolSeason;
 import com.idega.block.school.data.SchoolStudyPath;
 import com.idega.block.school.data.SchoolType;
 import com.idega.block.school.data.SchoolYear;
 import com.idega.business.IBOLookup;
 import com.idega.business.IBOLookupException;
 import com.idega.business.IBORuntimeException;
 import com.idega.core.contact.data.Email;
 import com.idega.core.contact.data.Phone;
 import com.idega.core.location.data.Address;
 import com.idega.core.location.data.PostalCode;
 import com.idega.data.IDOCreateException;
 import com.idega.data.IDORelationshipException;
 import com.idega.presentation.ExceptionWrapper;
 import com.idega.presentation.IWContext;
 import com.idega.presentation.Table;
 import com.idega.presentation.text.Break;
 import com.idega.presentation.text.Text;
 import com.idega.presentation.ui.BackButton;
 import com.idega.presentation.ui.DropdownMenu;
 import com.idega.presentation.ui.Form;
 import com.idega.presentation.ui.RadioButton;
 import com.idega.presentation.ui.SubmitButton;
 import com.idega.presentation.ui.TextArea;
 import com.idega.presentation.ui.TextInput;
 import com.idega.presentation.ui.util.SelectorUtility;
 import com.idega.user.business.NoEmailFoundException;
 import com.idega.user.business.NoPhoneFoundException;
 import com.idega.user.data.User;
 import com.idega.util.Age;
 import com.idega.util.PersonalIDFormatter;
 
 /**
  * @author laddi
  */
 public class MusicSchoolApplication extends MusicSchoolBlock {
 	
 	private static final int ACTION_OVERVIEW = 0;
 	private static final int ACTION_PHASE_1 = 1;
 	private static final int ACTION_PHASE_2 = 2;
 	private static final int ACTION_PHASE_3 = 3;
 	private static final int ACTION_PHASE_4 = 4;
 	private static final int ACTION_PHASE_5 = 5;
 	private static final int ACTION_VERIFY = 6;
 	private static final int ACTION_SAVE = 7;
 	
 	private static final String EXTRA_PREFIX = "extra_";
 	
 	private static final String PARAMETER_ACTION = "prm_action";
 	private static final String PARAMETER_BACK = "prm_back";
 
 	private static final String PARAMETER_SCHOOLS = "prm_school";
 	private static final String PARAMETER_SEASON = "prm_season";
 	private static final String PARAMETER_DEPARTMENT = "prm_department";
 	private static final String PARAMETER_INSTRUMENTS = "prm_instruments";
 	private static final String PARAMETER_LESSON_TYPE = "prm_lesson_type";
 	private static final String PARAMETER_OTHER_INSTRUMENT = "prm_other_instrument";
 
 	private static final String PARAMETER_TEACHER_REQUEST = "prm_teacher_request";
 	private static final String PARAMETER_MESSAGE = "prm_message";
 	private static final String PARAMETER_PREVIOUS_STUDIES = "prm_previoues_studies";
 	private static final String PARAMETER_ELEMENTARY_SCHOOL = "prm_elementary_school";
 	private static final String PARAMETER_CURRENT_YEAR = "prm_current_year";
 	private static final String PARAMETER_CURRENT_INSTRUMENT = "prm_current_instrument";
 	private static final String PARAMETER_PAYMENT_METHOD = "prm_payment_method";
 
 	private static final String PARAMETER_HOME_PHONE = "prm_home_phone";
 	private static final String PARAMETER_MOBILE_PHONE = "prm_mobile_phone";
 	private static final String PARAMETER_EMAIL = "prm_email";
 	
 	private static final String PARAMETER_HAS_EXTRA_APPLICATIONS = "prm_has_extra_applications";
 
 	/* (non-Javadoc)
 	 * @see se.idega.idegaweb.commune.school.music.presentation.MusicSchoolBlock#init(com.idega.presentation.IWContext)
 	 */
 	public void init(IWContext iwc) throws Exception {
 		if (getSession().getChild() != null) {
 			switch (parseAction(iwc)) {
 				case ACTION_OVERVIEW:
 					showOverview(iwc);
 					break;
 				case ACTION_PHASE_1:
 					showPhaseOne(iwc);
 					break;
 				case ACTION_PHASE_2:
 					updateUserInfo(iwc);
 					showPhaseTwo(iwc);
 					break;
 				case ACTION_PHASE_3:
 					showPhaseThree(iwc);
 					break;
 				case ACTION_PHASE_4:
 					showPhaseFour(iwc);
 					break;
 				case ACTION_PHASE_5:
 					showPhaseFive(iwc);
 					break;
 				case ACTION_VERIFY:
 					verifyApplication(iwc);
 					break;
 				case ACTION_SAVE:
 					saveChoices(iwc);
 					break;
 			}
 		}
 		else {
 			add(getErrorText(localize("application.no_user_found", "No user found...")));
 		}
 	}
 	
 	private void showOverview(IWContext iwc) throws RemoteException {
 		SchoolSeason season = null;
 		try {
 			season = getCareBusiness(iwc).getCurrentSeason();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			add(getErrorText(localize("no_season_found", "No season found...")));
 			return;
 		}
 		
 		if (getBusiness().hasApplication(getSession().getChild(), season)) {
 			viewApplication(iwc, getBusiness().hasGrantedApplication(getSession().getChild(), season));
 		}
 		else {
 			showPhaseOne(iwc);
 		}
 	}
 	
 	private void showPhaseOne(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_DEPARTMENT);
 		form.maintainParameter(PARAMETER_LESSON_TYPE);
 		form.maintainParameter(PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_HAS_EXTRA_APPLICATIONS);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_MESSAGE);
 		form.maintainParameter(PARAMETER_PREVIOUS_STUDIES);
 		form.maintainParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		
 		Table table = new Table();
 		table.setColumns(2);
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setBorder(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 
 		table.mergeCells(1, row, 2, row);
 		table.add(getPhasesTable(1, 6), 1, row++);
 		table.setHeight(row++, 12);
 		
 		table.mergeCells(1, row, 2, row);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.applicant", "Applicant")), 1, row++);
 		
 		User child = getSession().getChild();
 		Address address = getUserBusiness().getUsersMainAddress(child);
 		Phone phone = null;
 		try {
 			phone = getUserBusiness().getUsersHomePhone(child);
 		}
 		catch (NoPhoneFoundException npfe) {
 			phone = null;
 		}
 		Phone mobile = null;
 		try {
 			mobile = getUserBusiness().getUsersMobilePhone(child);
 		}
 		catch (NoPhoneFoundException npfe) {
 			mobile = null;
 		}
 		Email email = null;
 		try {
 			email = getUserBusiness().getUsersMainEmail(child);
 		}
 		catch (NoEmailFoundException nefe) {
 			email = null;
 		}
 		
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("name", "Name")), 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("personal_id", "Personal ID")), 2, row++);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(getTextInput("name", child.getName(), true), 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(getTextInput("personalID", PersonalIDFormatter.format(child.getPersonalID(), iwc.getCurrentLocale()), true), 2, row++);
 		
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("address", "Address")), 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("zip_code", "Zip code")), 2, row++);
 		TextInput addr = getTextInput("address", null, true);
 		TextInput zip = getTextInput("zipCode", null, true);
 		if (address != null) {
 			addr.setContent(address.getStreetAddress());
 			PostalCode code = address.getPostalCode();
 			if (code != null) {
 				zip.setContent(code.getPostalAddress());
 			}
 		}
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(addr, 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(zip, 2, row++);
 
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("home_phone", "Home phone")), 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("mobile_phone", "Mobile phone")), 2, row++);
 		TextInput homePhone = getTextInput(PARAMETER_HOME_PHONE, null, false);
 		homePhone.keepStatusOnAction(true);
 		TextInput mobilePhone = getTextInput(PARAMETER_MOBILE_PHONE, null, false);
 		mobilePhone.keepStatusOnAction(true);
 		if (phone != null) {
 			homePhone.setContent(phone.getNumber());
 		}
 		if (mobile != null) {
 			mobilePhone.setContent(mobile.getNumber());
 		}
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(homePhone, 1, row);
 		table.setStyleClass(2, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(mobilePhone, 2, row++);
 
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("email", "E-mail")), 1, row++);
 		TextInput mail = getTextInput(PARAMETER_EMAIL, null, false);
 		mail.keepStatusOnAction(true);
 		if (email != null) {
 			mail.setContent(email.getEmailAddress());
 		}
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(mail, 1, row++);
 
 		table.setHeight(row++, 18);
 		
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("next", "Next"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_2)));
 		
 		table.mergeCells(1, row, table.getColumns(), row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_phase_1"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 
 		add(form);
 	}
 	
 	private void showPhaseTwo(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_HAS_EXTRA_APPLICATIONS);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_MESSAGE);
 		form.maintainParameter(PARAMETER_PREVIOUS_STUDIES);
 		form.maintainParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		form.addParameter(PARAMETER_BACK, "-1");
 		
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 		
 		table.add(getPhasesTable(2, 6), 1, row++);
 		table.setHeight(row++, 12);
 
 		table.add(getPersonInfoTable(iwc, getSession().getChild()), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.add(getInformationTable(localize("phase_two_information", "Information text for phase two...")), 1, row++);
 		table.setHeight(row++, 6);
 
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.primary_application", "Primary application")), 1, row++);
 		
 		table.add(getChoiceTable(iwc, false), 1, row++);
 		table.setHeight(row++, 18);
 
 		SubmitButton previous = (SubmitButton) getButton(new SubmitButton(localize("previous", "Previous"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_1)));
 		previous.setValueOnClick(PARAMETER_BACK, "0");
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("next", "Next"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_3)));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_phase_2"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 		next.setOnSubmitFunction("checkApplication", getSubmitConfirmScript(""));
 
 		add(form);
 	}
 	
 	private Table getChoiceTable(IWContext iwc, boolean extraApplications) throws RemoteException {
 		Table choiceTable = new Table();
 		choiceTable.setCellpadding(0);
 		choiceTable.setCellspacing(0);
 		choiceTable.setColumns(2);
 		choiceTable.setWidth(Table.HUNDRED_PERCENT);
 		int iRow = 1;
 		
 		SchoolSeason season = null;
 		try {
 			season = getCareBusiness(iwc).getCurrentSeason();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			choiceTable.add(getErrorText(localize("no_season_found", "No season found...")));
 			return choiceTable;
 		}
 		
 		List instruments = null;
 		try {
 			instruments = new ArrayList(getInstruments());
 		}
 		catch (NoInstrumentFoundException nife) {
 			log(nife);
 			choiceTable.add(getErrorText(localize("no_instruments_found", "No instruments found...")));
 			return choiceTable;
 		}
 		Collections.sort(instruments, new InstrumentComparator(getResourceBundle()));
 		
 		Collection departments = null;
 		try {
 			departments = getDepartments();
 		}
 		catch (NoDepartmentFoundException ndfe) {
 			log(ndfe);
 			choiceTable.add(getErrorText(localize("no_departments_found", "No departments found...")));
 			return choiceTable;
 		}
 		
 		Collection lessonTypes = null;
 		try {
 			lessonTypes = getLessonTypes();
 		}
 		catch (NoLessonTypeFoundException ndfe) {
 			log(ndfe);
 			choiceTable.add(getErrorText(localize("no_departments_found", "No departments found...")));
 			return choiceTable;
 		}
 		
 		Collection schools = null;
 		try {
 			schools = getBusiness().findAllMusicSchools();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			choiceTable.add(getErrorText(localize("no_schools_found", "No schools found...")));
 			return choiceTable;
 		}
 		
 		if (extraApplications) {
 			Collection selectedSchools = new ArrayList();
 			for (int i = 0; i < 3; i++) {
 				if (iwc.isParameterSet(PARAMETER_SCHOOLS + "_" + (i+1))) {
 					try {
 						selectedSchools.add(getBusiness().findMusicSchool(iwc.getParameter(PARAMETER_SCHOOLS + "_" + (i+1))));
 					}
 					catch (FinderException fe) {
 						log(fe);
 					}
 				}
 			}
 			schools.removeAll(selectedSchools);
 		}
 		
 		Collection chosenInstruments = null;
 		Object chosenSchool1 = null;
 		Object chosenSchool2 = null;
 		Object chosenSchool3 = null;
 		Object chosenDepartment = null;
 		Object chosenLessonType = null;
 		String chosenTeacher = null;
 		String otherInstrument = null;
 		
 		Collection choices = null;
 		try {
 			choices = getBusiness().findChoicesByChildAndSeason(getSession().getChild(), season, extraApplications);
 			Iterator iter = choices.iterator();
 			boolean initialValuesSet = false;
 			int choiceNumber = 1;
 			while (iter.hasNext()) {
 				MusicSchoolChoice choice = (MusicSchoolChoice) iter.next();
 				choiceNumber = choice.getChoiceNumber();
 				if (!initialValuesSet) {
 					try {
 						chosenInstruments = choice.getStudyPaths();
 					}
 					catch (IDORelationshipException ire) {
 						log(ire);
 						break;
 					}
 					chosenDepartment = choice.getSchoolYearPK();
 					chosenLessonType = choice.getSchoolTypePK();
 					chosenTeacher = choice.getTeacherRequest();
 					otherInstrument = choice.getOtherInstrument();
 					initialValuesSet = true;
 				}
 				if (choiceNumber == 1) {
 					chosenSchool1 = choice.getSchoolPK();
 				}
 				else if (choiceNumber == 2) {
 					chosenSchool2 = choice.getSchoolPK();
 				}
 				else if (choiceNumber == 3) {
 					chosenSchool3 = choice.getSchoolPK();
 				}
 				choiceNumber++;
 			}
 		}
 		catch (FinderException fe) {
 			//Nothing found...
 		}
 		
 		SelectorUtility util = new SelectorUtility();
 		DropdownMenu instrumentsDrop1 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_INSTRUMENTS + "_1", extraApplications), null), instruments, "getLocalizedKey", getResourceBundle());
 		instrumentsDrop1.addMenuElementFirst("-1", localize("select_instrument", "Select instrument"));
 		instrumentsDrop1.keepStatusOnAction(true);
 		DropdownMenu instrumentsDrop2 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_INSTRUMENTS + "_2", extraApplications), null), instruments, "getLocalizedKey", getResourceBundle());
 		instrumentsDrop2.addMenuElementFirst("-1", localize("select_instrument", "Select instrument"));
 		instrumentsDrop2.keepStatusOnAction(true);
 		DropdownMenu instrumentsDrop3 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_INSTRUMENTS + "_3", extraApplications), null), instruments, "getLocalizedKey", getResourceBundle());
 		instrumentsDrop3.addMenuElementFirst("-1", localize("select_instrument", "Select instrument"));
 		instrumentsDrop3.keepStatusOnAction(true);
 		if (chosenInstruments != null) {
 			int index = 1;
 			Iterator iter = chosenInstruments.iterator();
 			while (iter.hasNext()) {
 				SchoolStudyPath instrument = (SchoolStudyPath) iter.next();
 				if (index == 1) {
 					instrumentsDrop1.setSelectedElement(instrument.getPrimaryKey().toString());
 				}
 				else if (index == 2) {
 					instrumentsDrop2.setSelectedElement(instrument.getPrimaryKey().toString());
 				}
 				else if (index == 3) {
 					instrumentsDrop3.setSelectedElement(instrument.getPrimaryKey().toString());
 				}
 				index++;
 			}
 		}
 		
 		DropdownMenu school1 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_SCHOOLS + "_1", extraApplications), chosenSchool1), schools, "getSchoolName");
 		school1.addMenuElementFirst("-1", localize("select_school", "Select school"));
 		school1.keepStatusOnAction(true);
 		
 		DropdownMenu school2 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_SCHOOLS + "_2", extraApplications), chosenSchool2), schools, "getSchoolName");
 		school2.addMenuElementFirst("-1", localize("select_school", "Select school"));
 		school2.keepStatusOnAction(true);
 		
 		DropdownMenu school3 = (DropdownMenu) util.getSelectorFromIDOEntities(getDropdown(getParameterName(PARAMETER_SCHOOLS + "_3", extraApplications), chosenSchool3), schools, "getSchoolName");
 		school3.addMenuElementFirst("-1", localize("select_school", "Select school"));
 		school3.keepStatusOnAction(true);
 		
 		DropdownMenu departmentDrop = getDropdown(getParameterName(PARAMETER_DEPARTMENT, extraApplications), chosenDepartment);
 		departmentDrop.addMenuElementFirst("-1", localize("select_department", "Select department"));
 		departmentDrop.keepStatusOnAction(true);
 		Iterator iter = departments.iterator();
 		while (iter.hasNext()) {
 			SchoolYear year = (SchoolYear) iter.next();
 			if (year.isSelectable()) {
 				departmentDrop.addMenuElement(year.getPrimaryKey().toString(), localize(year.getLocalizedKey(), year.getSchoolYearName()));
 			}
 		}
 
 		DropdownMenu lessonTypeDrop = getDropdown(getParameterName(PARAMETER_LESSON_TYPE, extraApplications), chosenLessonType);
 		lessonTypeDrop.addMenuElementFirst("-1", localize("select_lesson_type", "Select lesson type"));
 		lessonTypeDrop.keepStatusOnAction(true);
 		iter = lessonTypes.iterator();
 		while (iter.hasNext()) {
 			SchoolType type = (SchoolType) iter.next();
 			lessonTypeDrop.addMenuElement(type.getPrimaryKey().toString(), localize(type.getLocalizationKey(), type.getSchoolTypeName()));
 		}
 		
 		TextInput otherInstrumentInput = getTextInput(getParameterName(PARAMETER_OTHER_INSTRUMENT, extraApplications), otherInstrument);
 		otherInstrumentInput.keepStatusOnAction(true);
 		
 		TextInput teacherRequestInput = getTextInput(getParameterName(PARAMETER_TEACHER_REQUEST, extraApplications), chosenTeacher);
 		teacherRequestInput.keepStatusOnAction(true);
 
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("first_school", "First choice")), 1, iRow);
 		choiceTable.add(new Break(), 1, iRow);
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(school1, 1, iRow);
 
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("first_instrument", "First instrument")), 2, iRow);
 		choiceTable.add(new Break(), 2, iRow);
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(instrumentsDrop1, 2, iRow++);
 		
 		choiceTable.setHeight(iRow++, 3);
 
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("second_school", "Second choice")), 1, iRow);
 		choiceTable.add(new Break(), 1, iRow);
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(school2, 1, iRow);
 
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("second_instrument", "Second instrument")), 2, iRow);
 		choiceTable.add(new Break(), 2, iRow);
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(instrumentsDrop2, 2, iRow++);
 
 		choiceTable.setHeight(iRow++, 3);
 
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("third_school", "Third school")), 1, iRow);
 		choiceTable.add(new Break(), 1, iRow);
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(school3, 1, iRow);
 
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("third_instrument", "Third instrument")), 2, iRow);
 		choiceTable.add(new Break(), 2, iRow);
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(instrumentsDrop3, 2, iRow++);
 
 		choiceTable.setHeight(iRow++, 3);
 
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("other_instrument", "Other instrument")), 2, iRow);
 		choiceTable.add(new Break(), 2, iRow);
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(otherInstrumentInput, 2, iRow++);
 		
 		choiceTable.setHeight(iRow++, 18);
 		
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("department", "Department")), 1, iRow);
 		choiceTable.add(new Break(), 1, iRow);
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(departmentDrop, 1, iRow);
 
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("lesson_type", "Lesson type")), 2, iRow);
 		choiceTable.add(new Break(), 2, iRow);
 		choiceTable.setStyleClass(2, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(lessonTypeDrop, 2, iRow++);
 
 		choiceTable.setHeight(iRow++, 3);
 
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_TEXT_CELL));
 		choiceTable.add(getText(localize("teacher_request", "Teacher request")), 1, iRow);
 		choiceTable.add(new Break(), 1, iRow);
 		choiceTable.setStyleClass(1, iRow, getStyleName(STYLENAME_INPUT_CELL));
 		choiceTable.add(teacherRequestInput, 1, iRow++);
 		
 		return choiceTable;
 	}
 	
 	private Table getPhasesTable(int phase, int totalPhases) {
 		Table table = new Table(2, 1);
 		table.setCellpadding(getCellpadding());
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		table.setAlignment(2, 1, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setBottomCellBorder(1, 1, 1, "#D7D7D7", "solid");
 		table.setBottomCellBorder(2, 1, 1, "#D7D7D7", "solid");
 		
 		table.add(getHeader(localize("music_school_application", "Music school application")), 1, 1);
 		
 		StringBuffer buffer = new StringBuffer();
 		buffer.append(localize("phase", "Phase")).append(" ").append(phase).append(" ").append(localize("of", "of")).append(" ").append(totalPhases);
 		table.add(getHeader(buffer.toString()), 2, 1);
 		
 		return table;
 	}
 	
 	private Table getInformationTable(String information) {
 		Table table = new Table(1, 1);
 		table.setCellpadding(getCellpadding());
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		table.setBottomCellBorder(1, 1, 1, "#D7D7D7", "solid");
 		table.setCellpaddingBottom(1, 1, 6);
 		
 		table.add(getText(information), 1, 1);
 		
 		return table;
 	}
 	
 	private String getParameterName(String parameterName, boolean extraApplications) {
 		if (extraApplications) {
 			return EXTRA_PREFIX + parameterName;
 		}
 		return parameterName;
 	}
 	
 	private void showPhaseThree(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_DEPARTMENT);
 		form.maintainParameter(PARAMETER_LESSON_TYPE);
 		form.maintainParameter(PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_MESSAGE);
 		form.maintainParameter(PARAMETER_PREVIOUS_STUDIES);
 		form.maintainParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		
 		SchoolSeason season = null;
 		try {
 			season = getCareBusiness(iwc).getCurrentSeason();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			add(getErrorText(localize("no_season_found", "No season found...")));
 			return;
 		}
 		
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 
 		table.add(getPhasesTable(3, 6), 1, row++);
 		table.setHeight(row++, 12);
 
 		table.add(getPersonInfoTable(iwc, getSession().getChild()), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.add(getInformationTable(localize("extra_application_information_text", "If you want to make an application to another school for another instrument select 'Yes' below and click 'Next'.")), 1, row++);
 		table.setHeight(row++, 6);
 		
 		Table choiceTable = new Table(3, 2);
 		choiceTable.setColumnAlignment(2, Table.HORIZONTAL_ALIGN_CENTER);
 		choiceTable.setColumnAlignment(3, Table.HORIZONTAL_ALIGN_CENTER);
 
 		RadioButton yes = this.getRadioButton(PARAMETER_HAS_EXTRA_APPLICATIONS, Boolean.TRUE.toString());
 		RadioButton no = this.getRadioButton(PARAMETER_HAS_EXTRA_APPLICATIONS, Boolean.FALSE.toString());
 		if (getBusiness().hasExtraApplication(getSession().getChild(), season)) {
 			yes.setSelected(true);
 		}
 		else {
 			no.setSelected(true);
 		}
 		yes.keepStatusOnAction(true);
 		no.keepStatusOnAction(true);
 		
 		choiceTable.add(getSmallHeader(localize("apply_for_extra_school", "Apply for another school") + ":"), 1, 2);
 		choiceTable.add(getSmallHeader(localize("yes", "Yes")), 2, 1);
 		choiceTable.add(getSmallHeader(localize("no", "No")), 3, 1);
 		choiceTable.add(yes, 2, 2);
 		choiceTable.add(no, 3, 2);
 		
 		table.add(choiceTable, 1, row++);
 		table.setHeight(row++, 18);
 		
 		SubmitButton previous = (SubmitButton) getButton(new SubmitButton(localize("previous", "Previous"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_2)));
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("next", "Next"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_4)));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_phase_3"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 
 		add(form);
 	}
 	
 	private void showPhaseFour(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_DEPARTMENT);
 		form.maintainParameter(PARAMETER_LESSON_TYPE);
 		form.maintainParameter(PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_MESSAGE);
 		form.maintainParameter(PARAMETER_PREVIOUS_STUDIES);
 		form.maintainParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		form.addParameter(PARAMETER_HAS_EXTRA_APPLICATIONS, "true");
 		form.addParameter(PARAMETER_BACK, "-1");
 		
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 		
 		table.add(getPhasesTable(4, 6), 1, row++);
 		table.setHeight(row++, 12);
 
 		table.add(getPersonInfoTable(iwc, getSession().getChild()), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.add(getInformationTable(localize("phase_four_information", "Information text for phase four...")), 1, row++);
 		table.setHeight(row++, 6);
 
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.secondary_applications", "Secondary application")), 1, row++);
 		
 		table.add(getChoiceTable(iwc, true), 1, row++);
 		table.setHeight(row++, 18);
 
 		SubmitButton previous = (SubmitButton) getButton(new SubmitButton(localize("previous", "Previous"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_3)));
 		previous.setValueOnClick(PARAMETER_BACK, "0");
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("next", "Next"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_5)));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_phase_4"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 		next.setOnSubmitFunction("checkApplication", getSubmitConfirmScript(EXTRA_PREFIX));
 
 		add(form);
 	}
 	
 	private void showPhaseFive(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_DEPARTMENT);
 		form.maintainParameter(PARAMETER_LESSON_TYPE);
 		form.maintainParameter(PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_HAS_EXTRA_APPLICATIONS);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 		
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 
 		User user = getSession().getChild();
 		Age age = new Age(user.getDateOfBirth());
 		
 		SchoolSeason season = null;
 		try {
 			season = getCareBusiness(iwc).getCurrentSeason();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			add(getErrorText(localize("no_season_found", "No season found...")));
 			return;
 		}
 		
 		String chosenElementarySchool = null;
 		String chosenPreviousStudies = null;
 		String chosenMessage = null;
 		
 		List choices = null;
 		try {
 			choices = new ArrayList(getBusiness().findChoicesByChildAndSeason(getSession().getChild(), season, false));
 			if (!choices.isEmpty()) {
 				MusicSchoolChoice choice = (MusicSchoolChoice) choices.get(0);
 				chosenElementarySchool = choice.getElementarySchool();
 				chosenPreviousStudies = choice.getPreviousStudies();
 				chosenMessage = choice.getMessage();
 			}
 		}
 		catch (FinderException fe) {
 			//Nothing found...
 		}
 
 		table.add(getPhasesTable(5, 6), 1, row++);
 		table.setHeight(row++, 12);
 
 		table.add(getPersonInfoTable(iwc, user), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.add(getInformationTable(localize("phase_five_information", "Information text for phase five...")), 1, row++);
 		table.setHeight(row++, 6);
 
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.other_information", "Other information")), 1, row++);
 		
 		if (age.getYears() < 16) {
 			TextInput elementarySchool = getTextInput(PARAMETER_ELEMENTARY_SCHOOL, chosenElementarySchool);
 			elementarySchool.keepStatusOnAction(true);
 			table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 			table.add(getText(localize("elementary_school", "Elementary school")), 1, row++);
 			table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 			table.add(elementarySchool, 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		TextArea previousStudies = getTextArea(PARAMETER_PREVIOUS_STUDIES, chosenPreviousStudies);
 		previousStudies.setHeight("50");
 		previousStudies.keepStatusOnAction(true);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("previous_studies", "Previous studies")), 1, row++);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(previousStudies, 1, row);
 
 		table.setHeight(row++, 18);
 
 		TextArea message = getTextArea(PARAMETER_MESSAGE, chosenMessage);
 		message.setHeight("50");
 		message.keepStatusOnAction(true);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_TEXT_CELL));
 		table.add(getText(localize("message", "message")), 1, row++);
 		table.setStyleClass(1, row, getStyleName(STYLENAME_INPUT_CELL));
 		table.add(message, 1, row++);
 		
 		table.setHeight(row++, 18);
 
 		SubmitButton previous = (SubmitButton) getButton(new SubmitButton(localize("previous", "Previous"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_4)));
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("next", "Next"), PARAMETER_ACTION, String.valueOf(ACTION_VERIFY)));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_phase_5"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 
 		add(form);
 	}
 	
 	private void verifyApplication(IWContext iwc) throws RemoteException {
 		Form form = new Form();
 		form.setEventListener(MusicSchoolEventListener.class);
 		form.maintainParameter(PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(PARAMETER_SEASON);
 		form.maintainParameter(PARAMETER_DEPARTMENT);
 		form.maintainParameter(PARAMETER_LESSON_TYPE);
 		form.maintainParameter(PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_HAS_EXTRA_APPLICATIONS);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 		form.maintainParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 		form.maintainParameter(PARAMETER_MESSAGE);
 		form.maintainParameter(PARAMETER_PREVIOUS_STUDIES);
 		form.maintainParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		form.addParameter(PARAMETER_ACTION, String.valueOf(ACTION_SAVE));
 
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 
 		table.add(getPhasesTable(6, 6), 1, row++);
 		table.setHeight(row++, 12);
 
 		table.add(getPersonInfoTable(iwc, getSession().getChild()), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.verify_application", "Verify application")), 1, row++);
 		
 		Table verifyTable = new Table();
 		verifyTable.setCellpadding(getCellpadding());
 		verifyTable.setCellspacing(getCellspacing());
 		verifyTable.setColumns(2);
 		table.add(verifyTable, 1, row++);
 		int iRow = 1;
 		
 		String school1 = iwc.getParameter(PARAMETER_SCHOOLS + "_1");
 		String school2 = iwc.getParameter(PARAMETER_SCHOOLS + "_2");
 		String school3 = iwc.getParameter(PARAMETER_SCHOOLS + "_3");
 		
 		String instrument1 = iwc.getParameter(PARAMETER_INSTRUMENTS + "_1");
 		String instrument2 = iwc.getParameter(PARAMETER_INSTRUMENTS + "_2");
 		String instrument3 = iwc.getParameter(PARAMETER_INSTRUMENTS + "_3");
 		
 		String department = iwc.getParameter(PARAMETER_DEPARTMENT);
 		String lessonType = iwc.getParameter(PARAMETER_LESSON_TYPE);
 		
 		String teacherRequest = iwc.getParameter(PARAMETER_TEACHER_REQUEST);
 		String otherInstrument = iwc.getParameter(PARAMETER_OTHER_INSTRUMENT);
 		
 		String elementarySchool = iwc.getParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		String previousStudies = iwc.getParameter(PARAMETER_PREVIOUS_STUDIES);
 		String message = iwc.getParameter(PARAMETER_MESSAGE);
 		
 		verifyTable.mergeCells(1, iRow, 2, iRow);
 		verifyTable.add(getHeader(localize("primary_application", "Primary application")), 1, iRow++);
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (school1 != null) {
 			School school = getSchoolBusiness().getSchool(school1);
 			if (school != null) {
 				verifyTable.add(getSmallHeader(localize("first_school", "First school")), 1, iRow);
 				verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 			}
 		}
 		
 		if (school2 != null) {
 			School school = getSchoolBusiness().getSchool(school2);
 			if (school != null) {
 				verifyTable.add(getSmallHeader(localize("second_school", "Second school")), 1, iRow);
 				verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 			}
 		}
 		
 		if (school3 != null) {
 			School school = getSchoolBusiness().getSchool(school3);
 			if (school != null) {
 				verifyTable.add(getSmallHeader(localize("third_school", "Third school")), 1, iRow);
 				verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 			}
 		}
 		
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (instrument1 != null) {
 			SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument1);
 			if (instrument != null) {
 				verifyTable.add(getSmallHeader(localize("first_instrument", "First instrument")), 1, iRow);
 				verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 			}
 		}
 		
 		if (instrument2 != null) {
 			SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument2);
 			if (instrument != null) {
 				verifyTable.add(getSmallHeader(localize("second_instrument", "Second instrument")), 1, iRow);
 				verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 			}
 		}
 		
 		if (instrument3 != null) {
 			SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument3);
 			if (instrument != null) {
 				verifyTable.add(getSmallHeader(localize("third_instrument", "Third instrument")), 1, iRow);
 				verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 			}
 		}
 		
 		if (otherInstrument != null && otherInstrument.length() > 0) {
 			verifyTable.add(getSmallHeader(localize("other_instrument", "Other instrument")), 1, iRow);
 			verifyTable.add(getText(otherInstrument), 2, iRow++);
 		}
 		
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (department != null) {
 			SchoolYear year = getSchoolBusiness().getSchoolYear(department);
 			if (year != null) {
 				verifyTable.add(getSmallHeader(localize("department", "Department")), 1, iRow);
 				verifyTable.add(getText(localize(year.getLocalizedKey(), year.getSchoolYearName())), 2, iRow++);
 			}
 		}
 		
 		if (lessonType != null) {
 			SchoolType type = getSchoolBusiness().getSchoolType(lessonType);
 			if (type != null) {
 				verifyTable.add(getSmallHeader(localize("lesson_type", "Lesson type")), 1, iRow);
 				verifyTable.add(getText(localize(type.getLocalizationKey(), type.getSchoolTypeName())), 2, iRow++);
 			}
 		}
 		
 		if (teacherRequest != null && teacherRequest.length() > 0) {
 			verifyTable.add(getSmallHeader(localize("teacher_request", "Teacher request")), 1, iRow);
 			verifyTable.add(getText(teacherRequest), 2, iRow++);
 		}
 		
 		verifyTable.setHeight(iRow++, 12);
 		
 		boolean hasExtraApplications = new Boolean(iwc.getParameter(PARAMETER_HAS_EXTRA_APPLICATIONS)).booleanValue();
 		if (hasExtraApplications) {
 			verifyTable.mergeCells(1, iRow, 2, iRow);
 			verifyTable.add(getHeader(localize("secondary_application", "Secondary application")), 1, iRow++);
 			verifyTable.setHeight(iRow++, 6);
 			
 			school1 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_1");
 			school2 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_2");
 			school3 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_3");
 			
 			instrument1 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_1");
 			instrument2 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_2");
 			instrument3 = iwc.getParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_3");
 			
 			department = iwc.getParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 			lessonType = iwc.getParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 			
 			teacherRequest = iwc.getParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 			otherInstrument = iwc.getParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 			
 			if (school1 != null) {
 				School school = getSchoolBusiness().getSchool(school1);
 				if (school != null) {
 					verifyTable.add(getSmallHeader(localize("first_school", "First school")), 1, iRow);
 					verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 				}
 			}
 			
 			if (school2 != null) {
 				School school = getSchoolBusiness().getSchool(school2);
 				if (school != null) {
 					verifyTable.add(getSmallHeader(localize("second_school", "Second school")), 1, iRow);
 					verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 				}
 			}
 			
 			if (school3 != null) {
 				School school = getSchoolBusiness().getSchool(school3);
 				if (school != null) {
 					verifyTable.add(getSmallHeader(localize("third_school", "Third school")), 1, iRow);
 					verifyTable.add(getText(school.getSchoolName()), 2, iRow++);
 				}
 			}
 			
 			verifyTable.setHeight(iRow++, 6);
 			
 			if (instrument1 != null) {
 				SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument1);
 				if (instrument != null) {
 					verifyTable.add(getSmallHeader(localize("first_instrument", "First instrument")), 1, iRow);
 					verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 				}
 			}
 			
 			if (instrument2 != null) {
 				SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument2);
 				if (instrument != null) {
 					verifyTable.add(getSmallHeader(localize("second_instrument", "Second instrument")), 1, iRow);
 					verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 				}
 			}
 			
 			if (instrument3 != null) {
 				SchoolStudyPath instrument = getSchoolBusiness().getSchoolStudyPath(instrument3);
 				if (instrument != null) {
 					verifyTable.add(getSmallHeader(localize("third_instrument", "Third instrument")), 1, iRow);
 					verifyTable.add(getText(localize(instrument.getLocalizedKey(), instrument.getDescription())), 2, iRow++);
 				}
 			}
 			
 			if (otherInstrument != null && otherInstrument.length() > 0) {
 				verifyTable.add(getSmallHeader(localize("other_instrument", "Other instrument")), 1, iRow);
 				verifyTable.add(getText(otherInstrument), 2, iRow++);
 			}
 			
 			verifyTable.setHeight(iRow++, 6);
 			
 			if (department != null) {
 				SchoolYear year = getSchoolBusiness().getSchoolYear(department);
 				if (year != null) {
 					verifyTable.add(getSmallHeader(localize("department", "Department")), 1, iRow);
 					verifyTable.add(getText(localize(year.getLocalizedKey(), year.getSchoolYearName())), 2, iRow++);
 				}
 			}
 			
 			if (lessonType != null) {
 				SchoolType type = getSchoolBusiness().getSchoolType(lessonType);
 				if (type != null) {
 					verifyTable.add(getSmallHeader(localize("lesson_type", "Lesson type")), 1, iRow);
 					verifyTable.add(getText(localize(type.getLocalizationKey(), type.getSchoolTypeName())), 2, iRow++);
 				}
 			}
 			
 			if (teacherRequest != null && teacherRequest.length() > 0) {
 				verifyTable.add(getSmallHeader(localize("teacher_request", "Teacher request")), 1, iRow);
 				verifyTable.add(getText(teacherRequest), 2, iRow++);
 			}
 			
 		}
 		
 		table.setHeight(row++, 12);
 		table.add(getHeader(localize("other_info", "Other information")), 1, row++);
 		table.setHeight(row++, 6);
 		
 		if (elementarySchool != null && elementarySchool.length() > 0) {
 			table.add(getSmallHeader(localize("elementary_school", "Elementary school")), 1, row++);
 			table.add(getText(elementarySchool), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		if (previousStudies != null && previousStudies.length() > 0) {
 			table.add(getSmallHeader(localize("previous_studies", "Previous studies")), 1, row++);
 			table.add(getText(previousStudies), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		if (message != null && message.length() > 0) {
 			table.add(getSmallHeader(localize("message", "Message")), 1, row++);
 			table.add(getText(message), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		table.setHeight(row++, 18);
 
 		BackButton previous = (BackButton) getButton(new BackButton(localize("previous", "Previous")));
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("save", "Save")));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(next, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		table.add(getHelpButton("help_music_school_application_verify"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 		form.setToDisableOnSubmit(next, true);
 
 		add(form);
 	}
 	
 	private void viewApplication(IWContext iwc, boolean hasGrantedApplication) throws RemoteException {
 		Form form = new Form();
 
 		SchoolSeason season = null;
 		try {
 			season = getCareBusiness(iwc).getCurrentSeason();
 		}
 		catch (FinderException fe) {
 			log(fe);
 			add(getErrorText(localize("no_season_found", "No season found...")));
 			return;
 		}
 
 		Collection chosenInstruments = null;
 		School chosenSchool1 = null;
 		School chosenSchool2 = null;
 		School chosenSchool3 = null;
 		SchoolStudyPath instrument1 = null;
 		SchoolStudyPath instrument2 = null;
 		SchoolStudyPath instrument3 = null;
 		School extraChosenSchool1 = null;
 		School extraChosenSchool2 = null;
 		School extraChosenSchool3 = null;
 		SchoolStudyPath extraInstrument1 = null;
 		SchoolStudyPath extraInstrument2 = null;
 		SchoolStudyPath extraInstrument3 = null;
 		SchoolYear chosenDepartment = null;
 		SchoolType chosenLessonType = null;
 		String chosenTeacher = null;
 		String otherInstrument = null;
 		SchoolYear extraChosenDepartment = null;
 		SchoolType extraChosenLessonType = null;
 		String extraChosenTeacher = null;
 		String extraOtherInstrument = null;
 		String elementarySchool = null;
 		String previousStudies = null;
 		String message = null;
 		
 		
 		Collection choices = null;
 		try {
 			choices = getBusiness().findChoicesByChildAndSeason(getSession().getChild(), season, false);
 			Iterator iter = choices.iterator();
 			boolean initialValuesSet = false;
 			int choiceNumber = 1;
 			while (iter.hasNext()) {
 				MusicSchoolChoice choice = (MusicSchoolChoice) iter.next();
 				choiceNumber = choice.getChoiceNumber();
 				if (!initialValuesSet) {
 					try {
 						chosenInstruments = choice.getStudyPaths();
 					}
 					catch (IDORelationshipException ire) {
 						log(ire);
 						break;
 					}
 					chosenDepartment = choice.getSchoolYear();
 					chosenLessonType = choice.getSchoolType();
 					chosenTeacher = choice.getTeacherRequest();
 					otherInstrument = choice.getOtherInstrument();
 					elementarySchool = choice.getElementarySchool();
 					previousStudies = choice.getPreviousStudies();
 					message = choice.getMessage();
 					initialValuesSet = true;
 				}
 				if (choiceNumber == 1) {
 					chosenSchool1 = choice.getSchool();
 				}
 				else if (choiceNumber == 2) {
 					chosenSchool2 = choice.getSchool();
 				}
 				else if (choiceNumber == 3) {
 					chosenSchool3 = choice.getSchool();
 				}
 				choiceNumber++;
 			}
 		}
 		catch (FinderException fe) {
 			//Nothing found...
 		}
 		
 		if (chosenInstruments != null) {
 			int index = 1;
 			Iterator iter = chosenInstruments.iterator();
 			while (iter.hasNext()) {
 				SchoolStudyPath instrument = (SchoolStudyPath) iter.next();
 				if (index == 1) {
 					instrument1 = instrument;
 				}
 				else if (index == 2) {
 					instrument2 = instrument;
 				}
 				else if (index == 3) {
 					instrument3 = instrument;
 				}
 				index++;
 			}
 		}
 
 		try {
			choices = getBusiness().findChoicesByChildAndSeason(getSession().getChild(), season, false);
 			Iterator iter = choices.iterator();
 			boolean initialValuesSet = false;
 			int choiceNumber = 1;
 			while (iter.hasNext()) {
 				MusicSchoolChoice choice = (MusicSchoolChoice) iter.next();
 				if (!initialValuesSet) {
 					try {
 						chosenInstruments = choice.getStudyPaths();
 					}
 					catch (IDORelationshipException ire) {
 						log(ire);
 						break;
 					}
 					extraChosenDepartment = choice.getSchoolYear();
 					extraChosenLessonType = choice.getSchoolType();
 					extraChosenTeacher = choice.getTeacherRequest();
 					extraOtherInstrument = choice.getOtherInstrument();
 					initialValuesSet = true;
 				}
 				choiceNumber = choice.getChoiceNumber();
 				if (choiceNumber == 1) {
 					extraChosenSchool1 = choice.getSchool();
 				}
 				else if (choiceNumber == 2) {
 					extraChosenSchool2 = choice.getSchool();
 				}
 				else if (choiceNumber == 3) {
 					extraChosenSchool3 = choice.getSchool();
 				}
 				choiceNumber++;
 			}
 		}
 		catch (FinderException fe) {
 			//Nothing found...
 		}
 
 		if (chosenInstruments != null) {
 			int index = 1;
 			Iterator iter = chosenInstruments.iterator();
 			while (iter.hasNext()) {
 				SchoolStudyPath instrument = (SchoolStudyPath) iter.next();
 				if (index == 1) {
 					extraInstrument1 = instrument;
 				}
 				else if (index == 2) {
 					extraInstrument2 = instrument;
 				}
 				else if (index == 3) {
 					extraInstrument3 = instrument;
 				}
 				index++;
 			}
 		}
 
 		Table table = new Table();
 		table.setCellpadding(0);
 		table.setCellspacing(0);
 		table.setWidth(Table.HUNDRED_PERCENT);
 		form.add(table);
 		int row = 1;
 
 		table.add(getPersonInfoTable(iwc, getSession().getChild()), 1, row++);
 		table.setHeight(row++, 6);
 		
 		table.setStyleClass(1, row, getStyleName(STYLENAME_HEADING_CELL));
 		table.add(getHeader(localize("application.view_application", "View application")), 1, row++);
 		
 		Table verifyTable = new Table();
 		verifyTable.setCellpadding(getCellpadding());
 		verifyTable.setCellspacing(getCellspacing());
 		verifyTable.setColumns(2);
 		table.add(verifyTable, 1, row++);
 		int iRow = 1;
 				
 		verifyTable.mergeCells(1, iRow, 2, iRow);
 		verifyTable.add(getHeader(localize("primary_application", "Primary application")), 1, iRow++);
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (chosenSchool1 != null) {
 			verifyTable.add(getSmallHeader(localize("first_school", "First school")), 1, iRow);
 			verifyTable.add(getText(chosenSchool1.getSchoolName()), 2, iRow++);
 		}
 		
 		if (chosenSchool2 != null) {
 			verifyTable.add(getSmallHeader(localize("second_school", "Second school")), 1, iRow);
 			verifyTable.add(getText(chosenSchool2.getSchoolName()), 2, iRow++);
 		}
 		
 		if (chosenSchool3 != null) {
 			verifyTable.add(getSmallHeader(localize("third_school", "Third school")), 1, iRow);
 			verifyTable.add(getText(chosenSchool3.getSchoolName()), 2, iRow++);
 		}
 		
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (instrument1 != null) {
 			verifyTable.add(getSmallHeader(localize("first_instrument", "First instrument")), 1, iRow);
 			verifyTable.add(getText(localize(instrument1.getLocalizedKey(), instrument1.getDescription())), 2, iRow++);
 		}
 		
 		if (instrument2 != null) {
 			verifyTable.add(getSmallHeader(localize("second_instrument", "Second instrument")), 1, iRow);
 			verifyTable.add(getText(localize(instrument2.getLocalizedKey(), instrument2.getDescription())), 2, iRow++);
 		}
 		
 		if (instrument3 != null) {
 			verifyTable.add(getSmallHeader(localize("third_instrument", "Third instrument")), 1, iRow);
 			verifyTable.add(getText(localize(instrument3.getLocalizedKey(), instrument3.getDescription())), 2, iRow++);
 		}
 		
 		if (otherInstrument != null && otherInstrument.length() > 0) {
 			verifyTable.add(getSmallHeader(localize("other_instrument", "Other instrument")), 1, iRow);
 			verifyTable.add(getText(otherInstrument), 2, iRow++);
 		}
 		
 		verifyTable.setHeight(iRow++, 6);
 		
 		if (chosenDepartment != null) {
 			verifyTable.add(getSmallHeader(localize("department", "Department")), 1, iRow);
 			verifyTable.add(getText(localize(chosenDepartment.getLocalizedKey(), chosenDepartment.getSchoolYearName())), 2, iRow++);
 		}
 		
 		if (chosenLessonType != null) {
 			verifyTable.add(getSmallHeader(localize("lesson_type", "Lesson type")), 1, iRow);
 			verifyTable.add(getText(localize(chosenLessonType.getLocalizationKey(), chosenLessonType.getSchoolTypeName())), 2, iRow++);
 		}
 		
 		if (chosenTeacher != null && chosenTeacher.length() > 0) {
 			verifyTable.add(getSmallHeader(localize("teacher_request", "Teacher request")), 1, iRow);
 			verifyTable.add(getText(chosenTeacher), 2, iRow++);
 		}
 		
 		verifyTable.setHeight(iRow++, 12);
 		
 		if (extraChosenSchool1 != null) {
 			verifyTable.mergeCells(1, iRow, 2, iRow);
 			verifyTable.add(getHeader(localize("secondary_application", "Secondary application")), 1, iRow++);
 			verifyTable.setHeight(iRow++, 6);
 			
 			if (extraChosenSchool1 != null) {
 				verifyTable.add(getSmallHeader(localize("first_school", "First school")), 1, iRow);
 				verifyTable.add(getText(extraChosenSchool1.getSchoolName()), 2, iRow++);
 			}
 			
 			if (extraChosenSchool2 != null) {
 				verifyTable.add(getSmallHeader(localize("second_school", "Second school")), 1, iRow);
 				verifyTable.add(getText(extraChosenSchool2.getSchoolName()), 2, iRow++);
 			}
 			
 			if (extraChosenSchool3 != null) {
 				verifyTable.add(getSmallHeader(localize("third_school", "Third school")), 1, iRow);
 				verifyTable.add(getText(extraChosenSchool3.getSchoolName()), 2, iRow++);
 			}
 			
 			verifyTable.setHeight(iRow++, 6);
 			
 			if (extraInstrument1 != null) {
 				verifyTable.add(getSmallHeader(localize("first_instrument", "First instrument")), 1, iRow);
 				verifyTable.add(getText(localize(extraInstrument1.getLocalizedKey(), extraInstrument1.getDescription())), 2, iRow++);
 			}
 			
 			if (extraInstrument2 != null) {
 				verifyTable.add(getSmallHeader(localize("second_instrument", "Second instrument")), 1, iRow);
 				verifyTable.add(getText(localize(extraInstrument2.getLocalizedKey(), extraInstrument2.getDescription())), 2, iRow++);
 			}
 			
 			if (extraInstrument3 != null) {
 				verifyTable.add(getSmallHeader(localize("third_instrument", "Third instrument")), 1, iRow);
 				verifyTable.add(getText(localize(extraInstrument3.getLocalizedKey(), extraInstrument3.getDescription())), 2, iRow++);
 			}
 			
 			if (extraOtherInstrument != null && extraOtherInstrument.length() > 0) {
 				verifyTable.add(getSmallHeader(localize("other_instrument", "Other instrument")), 1, iRow);
 				verifyTable.add(getText(extraOtherInstrument), 2, iRow++);
 			}
 			
 			verifyTable.setHeight(iRow++, 6);
 			
 			if (extraChosenDepartment != null) {
 				verifyTable.add(getSmallHeader(localize("department", "Department")), 1, iRow);
 				verifyTable.add(getText(localize(extraChosenDepartment.getLocalizedKey(), extraChosenDepartment.getSchoolYearName())), 2, iRow++);
 			}
 			
 			if (extraChosenLessonType != null) {
 				verifyTable.add(getSmallHeader(localize("lesson_type", "Lesson type")), 1, iRow);
 				verifyTable.add(getText(localize(extraChosenLessonType.getLocalizationKey(), extraChosenLessonType.getSchoolTypeName())), 2, iRow++);
 			}
 			
 			if (extraChosenTeacher != null && extraChosenTeacher.length() > 0) {
 				verifyTable.add(getSmallHeader(localize("teacher_request", "Teacher request")), 1, iRow);
 				verifyTable.add(getText(extraChosenTeacher), 2, iRow++);
 			}
 			
 		}
 		
 		table.setHeight(row++, 12);
 		table.add(getHeader(localize("other_info", "Other information")), 1, row++);
 		table.setHeight(row++, 6);
 		
 		if (elementarySchool != null && elementarySchool.length() > 0) {
 			table.add(getSmallHeader(localize("elementary_school", "Elementary school")), 1, row++);
 			table.add(getText(elementarySchool), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		if (previousStudies != null && previousStudies.length() > 0) {
 			table.add(getSmallHeader(localize("previous_studies", "Previous studies")), 1, row++);
 			table.add(getText(previousStudies), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		if (message != null && message.length() > 0) {
 			table.add(getSmallHeader(localize("message", "Message")), 1, row++);
 			table.add(getText(message), 1, row++);
 			table.setHeight(row++, 3);
 		}
 		
 		table.setHeight(row++, 18);
 		
 		if (hasGrantedApplication) {
 			table.add(getErrorText(localize("student_already_has_placement", "Student already has a granted placement for the school season")), 1, row++);
 			table.setHeight(row++, 18);
 		}
 
 		BackButton previous = (BackButton) getButton(new BackButton(localize("previous", "Previous")));
 		SubmitButton next = (SubmitButton) getButton(new SubmitButton(localize("edit", "Edit"), PARAMETER_ACTION, String.valueOf(ACTION_PHASE_1)));
 		
 		table.add(previous, 1, row);
 		table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		if (!hasGrantedApplication) {
 			table.add(next, 1, row);
 			table.add(getSmallText(Text.NON_BREAKING_SPACE), 1, row);
 		}
 		table.add(getHelpButton("help_music_school_application_verify"), 1, row);
 		table.setAlignment(1, row, Table.HORIZONTAL_ALIGN_RIGHT);
 		table.setCellpaddingRight(1, row, 12);
 		form.setToDisableOnSubmit(next, true);
 
 		add(form);
 	}
 	
 	private String getSubmitConfirmScript(String prefix) {
 		StringBuffer buffer = new StringBuffer();
 		buffer.append("function checkApplication() {").append("\n\t");
 		buffer.append("\n\t var back = ").append("findObj('").append(PARAMETER_BACK).append("').value;");
 		buffer.append("\n\t if (back == '0') return true;");
 		buffer.append("\n\n\t var dropOne = ").append("findObj('").append(prefix + PARAMETER_SCHOOLS + "_1").append("');");
 		buffer.append("\n\t var dropTwo = ").append("findObj('").append(prefix + PARAMETER_SCHOOLS + "_2").append("');");
 		buffer.append("\n\t var dropThree = ").append("findObj('").append(prefix + PARAMETER_SCHOOLS + "_3").append("');");
 		buffer.append("\n\t var dropDepartment = ").append("findObj('").append(prefix + PARAMETER_DEPARTMENT).append("');");
 		buffer.append("\n\t var dropLessonTypes = ").append("findObj('").append(prefix + PARAMETER_LESSON_TYPE).append("');");
 		buffer.append("\n\t var dropInstrumentOne = ").append("findObj('").append(prefix + PARAMETER_INSTRUMENTS + "_1").append("');");
 		buffer.append("\n\t var dropInstrumentTwo = ").append("findObj('").append(prefix + PARAMETER_INSTRUMENTS + "_2").append("');");
 		buffer.append("\n\t var dropInstrumentThree = ").append("findObj('").append(prefix + PARAMETER_INSTRUMENTS + "_3").append("');");
 
 		buffer.append("\n\t var one = 0;");
 		buffer.append("\n\t var two = 0;");
 		buffer.append("\n\t var three = 0;");
 		buffer.append("\n\t var department = -1;");
 		buffer.append("\n\t var lessonType = -1;");
 		buffer.append("\n\t var length = 0;");
 		buffer.append("\n\t var instrumentOne = 0;");
 		buffer.append("\n\t var instrumentTwo = 0;");
 		buffer.append("\n\t var instrumentThree = 0;");
 
 		buffer.append("\n\n\t if (dropOne.selectedIndex > 0) {\n\t\t one = dropOne.options[dropOne.selectedIndex].value;\n\t\t if (one > 0) length++;\n\t }");
 		buffer.append("\n\t if (dropTwo.selectedIndex > 0) {\n\t\t two = dropTwo.options[dropTwo.selectedIndex].value;\n\t\t if (two > 0) length++;\n\t }");
 		buffer.append("\n\t if (dropThree.selectedIndex > 0) {\n\t\t three = dropThree.options[dropThree.selectedIndex].value;\n\t\t if (three > 0) length++;\n\t }");
 		buffer.append("\n\t if (dropDepartment.selectedIndex > 0) {\n\t\t department = dropDepartment.options[dropDepartment.selectedIndex].value;\n\t }");
 		buffer.append("\n\t if (dropLessonTypes.selectedIndex > 0) {\n\t\t lessonType = dropLessonTypes.options[dropLessonTypes.selectedIndex].value;\n\t }");
 		buffer.append("\n\t if (dropInstrumentOne.selectedIndex > 0) {\n\t\t instrumentOne = dropInstrumentOne.options[dropInstrumentOne.selectedIndex].value;\n\t }");
 		buffer.append("\n\t if (dropInstrumentTwo.selectedIndex > 0) {\n\t\t instrumentTwo = dropInstrumentTwo.options[dropInstrumentTwo.selectedIndex].value;\n\t }");
 		buffer.append("\n\t if (dropInstrumentThree.selectedIndex > 0) {\n\t\t instrumentThree = dropInstrumentThree.options[dropInstrumentThree.selectedIndex].value;\n\t }");
 
 		buffer.append("\n\t if(length > 0){");
 		buffer.append("\n\t\t if(one > 0 && (one == two || one == three)){");
 		String message = localize("must_not_be_the_same", "Please do not choose the same provider more than once.");
 		buffer.append("\n\t\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t\t return false;");
 		buffer.append("\n\t\t }");
 		buffer.append("\n\t\t if(two > 0 && (two == one || two == three)){");
 		buffer.append("\n\t\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t\t return false;");
 		buffer.append("\n\t\t }");
 		buffer.append("\n\t\t if(three > 0 && (three == one || three == two )){");
 		buffer.append("\n\t\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t\t return false;");
 		buffer.append("\n\t\t }");
 
 		buffer.append("\n\t }");
 		buffer.append("\n\t else {");
 		message = localize("must_fill_out_one", "Please fill out the first choice.");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		message = localize("less_than_three_chosen", "You have chosen less than three choices.  An offer can not be guaranteed within three months.");
 
 		message = localize("must_fill_out_department", "Please fill out department.");
 		buffer.append("\n\t if(department < 0){");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		
 		message = localize("must_fill_out_lesson_type", "Please fill out lesson type.");
 		buffer.append("\n\t if(lessonType < 0){");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		
 		buffer.append("\n\t if(instrumentOne > 0 && (instrumentOne == instrumentTwo || instrumentOne == instrumentThree)){");
 		message = localize("instrument_must_not_be_the_same", "Please do not choose the same instrument more than once.");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		buffer.append("\n\t if(instrumentTwo > 0 && (instrumentTwo == instrumentOne || instrumentTwo == instrumentThree)){");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		buffer.append("\n\t if(instrumentThree > 0 && (instrumentThree == instrumentOne || instrumentThree == instrumentTwo )){");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 		message = localize("must_fill_out_instrument", "Please select at least one instrument.");
 		buffer.append("\n\t if(instrumentOne == 0 && instrumentTwo == 0 && instrumentThree == 0){");
 		buffer.append("\n\t\t alert('").append(message).append("');");
 		buffer.append("\n\t\t return false;");
 		buffer.append("\n\t }");
 
 		buffer.append("\n\t document.body.style.cursor = 'wait'");
 		buffer.append("\n\t return true;");
 		buffer.append("\n}\n");
 
 		return buffer.toString();
 	}
 	
 	private void updateUserInfo(IWContext iwc) {
 		String homePhone = iwc.getParameter(PARAMETER_HOME_PHONE);
 		String mobilePhone = iwc.getParameter(PARAMETER_MOBILE_PHONE);
 		String email = iwc.getParameter(PARAMETER_EMAIL);
 
 		try {
 			if (homePhone != null) {
 				getUserBusiness().updateUserHomePhone(getSession().getChild(), homePhone);
 			}
 			if (mobilePhone != null) {
 				getUserBusiness().updateUserMobilePhone(getSession().getChild(), mobilePhone);
 			}
 			if (email != null) {
 				try {
 					getUserBusiness().updateUserMail(getSession().getChild(), email);
 				}
 				catch (CreateException ce) {
 					log(ce);
 				}
 			}
 		}
 		catch (RemoteException re) {
 			throw new IBORuntimeException(re);
 		}
 	}
 	
 	private void saveChoices(IWContext iwc) {
 		Collection schoolPKs = new ArrayList();
 		for (int i = 0; i < 3; i++) {
 			if (iwc.isParameterSet(PARAMETER_SCHOOLS + "_" + (i+1))) {
 				String schoolPK = iwc.getParameter(PARAMETER_SCHOOLS + "_" + (i+1));
 				if (!schoolPK.equals("-1")) {
 					schoolPKs.add(schoolPK);
 				}
 			}
 		}
 		Collection instrumentPKs = new ArrayList();
 		for (int i = 0; i < 3; i++) {
 			if (iwc.isParameterSet(PARAMETER_INSTRUMENTS + "_" + (i+1))) {
 				String instrumentPK = iwc.getParameter(PARAMETER_INSTRUMENTS + "_" + (i+1));
 				if (!instrumentPK.equals("-1")) {
 					instrumentPKs.add(instrumentPK);
 				}
 			}
 		}
 		
 		String season = iwc.getParameter(PARAMETER_SEASON);
 		String department = iwc.getParameter(PARAMETER_DEPARTMENT);
 		String lessonType = iwc.getParameter(PARAMETER_LESSON_TYPE);
 		String teacherRequest = iwc.getParameter(PARAMETER_TEACHER_REQUEST);
 		String message = iwc.getParameter(PARAMETER_MESSAGE);
 		String previousStudies = iwc.getParameter(PARAMETER_PREVIOUS_STUDIES);
 		String elementarySchool = iwc.getParameter(PARAMETER_ELEMENTARY_SCHOOL);
 		String otherInstrument = iwc.getParameter(PARAMETER_OTHER_INSTRUMENT);
 		
 		String currentYear = null;
 		if (iwc.isParameterSet(PARAMETER_CURRENT_YEAR)) {
 			currentYear = iwc.getParameter(PARAMETER_CURRENT_YEAR);
 		}
 		String currentInstrument = null;
 		if (iwc.isParameterSet(PARAMETER_CURRENT_INSTRUMENT)) {
 			currentInstrument = iwc.getParameter(PARAMETER_CURRENT_INSTRUMENT);
 		}
 		int paymentMethod = -1;
 		if (iwc.isParameterSet(PARAMETER_PAYMENT_METHOD)) {
 			paymentMethod = Integer.parseInt(iwc.getParameter(PARAMETER_PAYMENT_METHOD));
 		}
 		
 		try {
 			boolean success = getBusiness().saveChoices(iwc.getCurrentUser(), getSession().getChild(), schoolPKs, season, department, lessonType, instrumentPKs, otherInstrument, teacherRequest, message, currentYear, currentInstrument, previousStudies, elementarySchool, paymentMethod, false);
 			
 			if (iwc.isParameterSet(PARAMETER_HAS_EXTRA_APPLICATIONS)) {
 				schoolPKs = new ArrayList();
 				for (int i = 0; i < 3; i++) {
 					if (iwc.isParameterSet(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_" + (i+1))) {
 						schoolPKs.add(iwc.getParameter(EXTRA_PREFIX + PARAMETER_SCHOOLS + "_" + (i+1)));
 					}
 				}
 				instrumentPKs = new ArrayList();
 				for (int i = 0; i < 3; i++) {
 					if (iwc.isParameterSet(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_" + (i+1))) {
 						instrumentPKs.add(iwc.getParameter(EXTRA_PREFIX + PARAMETER_INSTRUMENTS + "_" + (i+1)));
 					}
 				}
 
 				department = iwc.getParameter(EXTRA_PREFIX + PARAMETER_DEPARTMENT);
 				lessonType = iwc.getParameter(EXTRA_PREFIX + PARAMETER_LESSON_TYPE);
 				teacherRequest = iwc.getParameter(EXTRA_PREFIX + PARAMETER_TEACHER_REQUEST);
 				otherInstrument = iwc.getParameter(EXTRA_PREFIX + PARAMETER_OTHER_INSTRUMENT);
 
 				success = getBusiness().saveChoices(iwc.getCurrentUser(), getSession().getChild(), schoolPKs, season, department, lessonType, instrumentPKs, otherInstrument, teacherRequest, message, currentYear, currentInstrument, previousStudies, elementarySchool, paymentMethod, true);
 			}
 			
 			if (success) {
 				if (getResponsePage() != null) {
 					iwc.forwardToIBPage(getParentPage(), getResponsePage());
 				}
 				else {
 					Table table = new Table();
 					table.add(getErrorText(localize("submit_success", "Submit success...")), 1, 1);
 					table.add(new UserHomeLink(), 1, 2);
 				}
 			}
 			else {
 				Table table = new Table();
 				table.add(getErrorText(localize("submit_failed", "Submit failed...")), 1, 1);
 				table.add(new UserHomeLink(), 1, 2);
 			}
 		}
 		catch (RemoteException re) {
 			re.printStackTrace();
 		}
 		catch (IDOCreateException ice) {
 			add(new ExceptionWrapper(ice));
 		}
 	}
 	
 	private int parseAction(IWContext iwc) {
 		int action = ACTION_OVERVIEW;
 		if (iwc.isParameterSet(PARAMETER_ACTION)) {
 			action = Integer.parseInt(iwc.getParameter(PARAMETER_ACTION));
 		}
 		
 		if (action == ACTION_PHASE_4 && iwc.isParameterSet(PARAMETER_HAS_EXTRA_APPLICATIONS)) {
 			boolean showExtraApplicationsForm = new Boolean(iwc.getParameter(PARAMETER_HAS_EXTRA_APPLICATIONS)).booleanValue();
 			if (!showExtraApplicationsForm) {
 				action = ACTION_PHASE_5;
 			}
 		}
 		
 		return action;
 	}
 
 	private CareBusiness getCareBusiness(IWContext iwc) {
 		try {
 			return (CareBusiness) IBOLookup.getServiceInstance(iwc, CareBusiness.class);	
 		}
 		catch (IBOLookupException ile) {
 			throw new IBORuntimeException(ile);
 		}
 	}
 	
 	public Map getStyleNames() {
 		Map map = new HashMap();
 		map.put(STYLENAME_HEADER_CELL, "");
 		map.put(STYLENAME_HEADING_CELL, "");
 		map.put(STYLENAME_TEXT_CELL, "");
 		map.put(STYLENAME_INPUT_CELL, "");
 		map.put(STYLENAME_INFORMATION_CELL, "");
 		
 		return map;
 	}
 }
