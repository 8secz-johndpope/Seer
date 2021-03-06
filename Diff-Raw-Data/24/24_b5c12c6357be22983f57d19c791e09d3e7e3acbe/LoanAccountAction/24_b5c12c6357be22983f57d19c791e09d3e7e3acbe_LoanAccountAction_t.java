 /*
  * Copyright (c) 2005-2010 Grameen Foundation USA
  * All rights reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * permissions and limitations under the License.
  *
  * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
  * explanation of the license and how it is applied.
  */
 
 package org.mifos.accounts.loan.struts.action;
 
 import static org.apache.commons.lang.StringUtils.isNotEmpty;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.ADDITIONAL_FEES_LIST;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.ADMINISTRATIVE_DOCUMENT_IS_ENABLED;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.CLIENT_LIST;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.CUSTOM_FIELDS;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOANACCOUNTOWNER;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOANFUNDS;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOANOFFERING;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOAN_ACCOUNT_OWNER_IS_A_GROUP;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOAN_ALL_ACTIVITY_VIEW;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.LOAN_INDIVIDUAL_MONITORING_IS_ENABLED;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.METHODCALLED;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.MIN_DAYS_BETWEEN_DISBURSAL_AND_FIRST_REPAYMENT_DAY;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.NEXTMEETING_DATE;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.PERSPECTIVE_VALUE_REDO_LOAN;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.PROPOSED_DISBURSAL_DATE;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.RECURRENCEID;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.RECURRENCENAME;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.STATUS_HISTORY;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.TOTAL_AMOUNT_OVERDUE;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.VIEWINSTALLMENTDETAILS_SUCCESS;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.VIEW_OVERDUE_INSTALLMENT_DETAILS;
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.VIEW_UPCOMING_INSTALLMENT_DETAILS;
 import static org.mifos.accounts.loan.util.helpers.RequestConstants.PERSPECTIVE;
 import static org.mifos.framework.util.helpers.Constants.BUSINESS_KEY;
 
 import java.io.Serializable;
 import java.io.UnsupportedEncodingException;
 import java.math.BigDecimal;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Locale;
 import java.util.Set;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.collections.CollectionUtils;
 import org.apache.commons.collections.Predicate;
 import org.apache.commons.lang.StringUtils;
 import org.apache.struts.action.ActionErrors;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.apache.struts.action.ActionMessage;
 import org.joda.time.DateTime;
 import org.joda.time.LocalDate;
 import org.mifos.accounts.business.AccountCustomFieldEntity;
 import org.mifos.accounts.business.AccountStatusChangeHistoryEntity;
 import org.mifos.accounts.business.service.AccountBusinessService;
 import org.mifos.accounts.exceptions.AccountException;
 import org.mifos.accounts.fees.business.FeeDto;
 import org.mifos.accounts.fund.business.FundBO;
 import org.mifos.accounts.loan.business.LoanActivityDto;
 import org.mifos.accounts.loan.business.LoanBO;
 import org.mifos.accounts.loan.business.MaxMinInterestRate;
 import org.mifos.accounts.loan.business.service.LoanBusinessService;
 import org.mifos.accounts.loan.business.service.LoanInformationDto;
 import org.mifos.accounts.loan.business.service.LoanScheduleGenerationDto;
 import org.mifos.accounts.loan.business.service.OriginalScheduleInfoDto;
 import org.mifos.accounts.loan.persistance.LoanDaoHibernate;
 import org.mifos.accounts.loan.struts.actionforms.LoanAccountActionForm;
 import org.mifos.accounts.loan.util.InstallmentAndCashflowComparisionUtility;
 import org.mifos.accounts.loan.util.helpers.LoanConstants;
 import org.mifos.accounts.loan.util.helpers.RepaymentScheduleInstallment;
 import org.mifos.accounts.productdefinition.business.LoanAmountOption;
 import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
 import org.mifos.accounts.productdefinition.business.LoanOfferingFundEntity;
 import org.mifos.accounts.productdefinition.business.LoanOfferingInstallmentRange;
 import org.mifos.accounts.productdefinition.business.VariableInstallmentDetailsBO;
 import org.mifos.accounts.productdefinition.business.service.LoanPrdBusinessService;
 import org.mifos.accounts.productdefinition.business.service.LoanProductService;
 import org.mifos.accounts.productdefinition.persistence.LoanProductDao;
 import org.mifos.accounts.savings.persistence.GenericDaoHibernate;
 import org.mifos.accounts.struts.action.AccountAppAction;
 import org.mifos.accounts.util.helpers.AccountConstants;
 import org.mifos.application.admin.servicefacade.InvalidDateException;
 import org.mifos.application.cashflow.struts.CashFlowAdaptor;
 import org.mifos.application.cashflow.struts.CashFlowCaptor;
 import org.mifos.application.cashflow.struts.DefaultCashFlowServiceLocator;
 import org.mifos.application.master.MessageLookup;
 import org.mifos.application.master.business.BusinessActivityEntity;
 import org.mifos.application.master.business.CustomFieldDefinitionEntity;
 import org.mifos.application.master.business.CustomFieldType;
 import org.mifos.application.master.business.CustomValueDto;
 import org.mifos.application.master.business.CustomValueListElementDto;
 import org.mifos.application.master.business.service.MasterDataService;
 import org.mifos.application.master.persistence.MasterPersistence;
 import org.mifos.application.master.util.helpers.MasterConstants;
 import org.mifos.application.meeting.business.MeetingBO;
 import org.mifos.application.meeting.business.MeetingDetailsEntity;
 import org.mifos.application.meeting.exceptions.MeetingException;
 import org.mifos.application.meeting.util.helpers.MeetingConstants;
 import org.mifos.application.meeting.util.helpers.MeetingType;
 import org.mifos.application.meeting.util.helpers.RankOfDay;
 import org.mifos.application.meeting.util.helpers.RecurrenceType;
 import org.mifos.application.meeting.util.helpers.WeekDay;
 import org.mifos.application.questionnaire.struts.DefaultQuestionnaireServiceFacadeLocator;
 import org.mifos.application.questionnaire.struts.QuestionnaireAction;
 import org.mifos.application.questionnaire.struts.QuestionnaireFlowAdapter;
 import org.mifos.application.questionnaire.struts.QuestionnaireServiceFacadeLocator;
 import org.mifos.application.servicefacade.LoanCreationLoanScheduleDetailsDto;
 import org.mifos.application.util.helpers.ActionForwards;
 import org.mifos.application.util.helpers.EntityType;
 import org.mifos.application.util.helpers.Methods;
 import org.mifos.config.AccountingRules;
 import org.mifos.config.FiscalCalendarRules;
 import org.mifos.config.business.service.ConfigurationBusinessService;
 import org.mifos.config.persistence.ConfigurationPersistence;
 import org.mifos.customers.business.CustomerBO;
 import org.mifos.customers.business.service.CustomerBusinessService;
 import org.mifos.customers.client.business.ClientBO;
 import org.mifos.customers.client.business.service.ClientBusinessService;
 import org.mifos.customers.util.helpers.CustomerConstants;
 import org.mifos.dto.domain.CustomFieldDto;
 import org.mifos.dto.domain.CustomerDetailDto;
 import org.mifos.dto.domain.LoanAccountDetailsDto;
 import org.mifos.dto.domain.MeetingDto;
 import org.mifos.dto.domain.ValueListElement;
 import org.mifos.dto.screen.LoanAccountInfoDto;
 import org.mifos.dto.screen.LoanAccountMeetingDto;
 import org.mifos.dto.screen.LoanCreationGlimDto;
 import org.mifos.dto.screen.LoanCreationLoanDetailsDto;
 import org.mifos.dto.screen.LoanCreationPreviewDto;
 import org.mifos.dto.screen.LoanCreationProductDetailsDto;
 import org.mifos.dto.screen.LoanCreationResultDto;
 import org.mifos.dto.screen.LoanRepaymentScheduleInstallmentDto;
 import org.mifos.framework.business.util.helpers.MethodNameConstants;
 import org.mifos.framework.exceptions.ApplicationException;
 import org.mifos.framework.exceptions.PageExpiredException;
 import org.mifos.framework.exceptions.PersistenceException;
 import org.mifos.framework.exceptions.ServiceException;
 import org.mifos.framework.util.helpers.Constants;
 import org.mifos.framework.util.helpers.DateUtils;
 import org.mifos.framework.util.helpers.Money;
 import org.mifos.framework.util.helpers.SessionUtils;
 import org.mifos.framework.util.helpers.TransactionDemarcate;
 import org.mifos.platform.cashflow.ui.model.CashFlowForm;
 import org.mifos.platform.questionnaire.service.QuestionGroupInstanceDetail;
 import org.mifos.platform.questionnaire.service.QuestionnaireServiceFacade;
 import org.mifos.platform.validations.ErrorEntry;
 import org.mifos.platform.validations.Errors;
 import org.mifos.reports.admindocuments.persistence.AdminDocAccStateMixPersistence;
 import org.mifos.reports.admindocuments.persistence.AdminDocumentPersistence;
 import org.mifos.reports.admindocuments.util.helpers.AdminDocumentsContants;
 import org.mifos.security.util.ActionSecurity;
 import org.mifos.security.util.SecurityConstants;
 import org.mifos.security.util.UserContext;
 
 /**
  * Creation and management of loan accounts.
  * <p/>
  * The "repayment day" form fields provided by the frontend and manipulated in the form by this class are somewhat
  * confusing. Here's an attempt to add some clarity.
  * <p/>
  * <h3>required for both weekly and monthly recurrence</h3>
  * <ul>
  * <li>frequency
  * <ul>
  * <li>ie: "weekly", "monthly" (corresponds to values in {@link RecurrenceType} ).</li>
  * </ul>
  * </li>
  * </ul>
  * <p/>
  * <h3>required for monthly recurrence</h3>
  * <ul>
  * <li>monthType=1 : "12th day of every 1 month"
  * <ul>
  * <li>monthDay
  * <ul>
  * <li>Xst/Xnd/Xth of every month</li>
  * </ul>
  * </li>
  * <li>dayRecurMonth
  * <ul>
  * <li>of every X months</li>
  * </ul>
  * </li>
  * </ul>
  * </li>
  * <li>monthType=2 : "First Monday of every 1 month"
  * <ul>
  * <li>monthRank
  * <ul>
  * <li>First, Second, etc. (ordinal)</li>
  * </ul>
  * </li>
  * <li>monthWeek
  * <ul>
  * <li><strong>day</strong>, ie, Monday, Tuesday, etc. of the week that repayments should be made. Really.</li>
  * </ul>
  * </li>
  * <li>recurMonth
  * <ul>
  * <li>every X months</li>
  * </ul>
  * </li>
  * </ul>
  * </li>
  * </ul>
  * <p/>
  * <h3>required for weekly recurrence</h3>
  * <ul>
  * <li>(only one kind of weekly schedule)
  * <ul>
  * <li>weekDay
  * <ul>
  * <li>day of the week repayments should be made</li>
  * </ul>
  * </li>
  * <li>recurWeek
  * <ul>
  * <li>every X weeks</li>
  * </ul>
  * </li>
  * </ul>
  * </li>
  * </ul>
  */
 public class LoanAccountAction extends AccountAppAction implements QuestionnaireAction {
 
     private final LoanBusinessService loanBusinessService;
     private final LoanPrdBusinessService loanPrdBusinessService;
     private final ClientBusinessService clientBusinessService;
     private final MasterDataService masterDataService;
     private final ConfigurationPersistence configurationPersistence;
     private final ConfigurationBusinessService configService;
     private final GlimLoanUpdater glimLoanUpdater;
 
     public static final String CUSTOMER_ID = "customerId";
     public static final String ACCOUNT_ID = "accountId";
     public static final String GLOBAL_ACCOUNT_NUM = "globalAccountNum";
 
     private QuestionnaireServiceFacadeLocator questionnaireServiceFacadeLocator;
     private QuestionGroupFilterForLoan questionGroupFilter;
     private QuestionnaireFlowAdapter createLoanQuestionnaire;
     private CashFlowAdaptor cashFlowAdaptor =
             new CashFlowAdaptor(ActionForwards.capture_cash_flow.toString(), new DefaultCashFlowServiceLocator());
 
     private static final String SHOW_PREVIEW = "loanAccountAction.do?method=showPreview";
     private static final String CUSTOMER_SEARCH_URL = "custSearchAction.do?method=loadMainSearch";
 
     public LoanAccountAction() {
         this(new ConfigurationBusinessService(), new LoanBusinessService(), new GlimLoanUpdater(),
                 new LoanPrdBusinessService(), new ClientBusinessService(), new MasterDataService(),
                 new ConfigurationPersistence(), new AccountBusinessService());
     }
 
     public LoanAccountAction(final ConfigurationBusinessService configService,
                              final LoanBusinessService loanBusinessService, final GlimLoanUpdater glimLoanUpdater,
                              final LoanPrdBusinessService loanPrdBusinessService, final ClientBusinessService clientBusinessService,
                              final MasterDataService masterDataService, final ConfigurationPersistence configurationPersistence,
                              final AccountBusinessService accountBusinessService) {
         super(accountBusinessService);
 
         this.configService = configService;
         this.loanBusinessService = loanBusinessService;
         this.glimLoanUpdater = glimLoanUpdater;
         this.loanPrdBusinessService = loanPrdBusinessService;
         this.clientBusinessService = clientBusinessService;
         this.masterDataService = masterDataService;
         this.configurationPersistence = configurationPersistence;
         this.questionGroupFilter = new QuestionGroupFilterForLoan();
         this.questionnaireServiceFacadeLocator = new DefaultQuestionnaireServiceFacadeLocator();
         this.createLoanQuestionnaire = new QuestionnaireFlowAdapter("Create", "Loan", ActionForwards.schedulePreview_success,
                 "custSearchAction.do?method=loadMainSearch", questionnaireServiceFacadeLocator, questionGroupFilter);
     }
 
     /**
      * @deprecated - test only
      */
     @Deprecated
     LoanAccountAction(final LoanBusinessService loanBusinessService, final ConfigurationBusinessService configService,
                       final GlimLoanUpdater glimLoanUpdater) {
         this(configService, loanBusinessService, glimLoanUpdater);
     }
 
     /**
      * test only constructor
      */
     @Deprecated
     private LoanAccountAction(final ConfigurationBusinessService configService,
                               final LoanBusinessService loanBusinessService, final GlimLoanUpdater glimLoanUpdater) {
         this(configService, loanBusinessService, glimLoanUpdater, new LoanPrdBusinessService(),
                 new ClientBusinessService(), new MasterDataService(), new ConfigurationPersistence(),
                 new AccountBusinessService());
     }
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("loanAccountAction");
         security.allow("getAllActivity", SecurityConstants.VIEW);
         security.allow("get", SecurityConstants.VIEW);
         security.allow("getLoanRepaymentSchedule", SecurityConstants.VIEW);
         security.allow("viewOriginalSchedule", SecurityConstants.VIEW);
         security.allow("viewStatusHistory", SecurityConstants.VIEW);
         security.allow("manage", SecurityConstants.LOAN_UPDATE_LOAN);
         security.allow("managePreview", SecurityConstants.VIEW);
         security.allow("managePrevious", SecurityConstants.VIEW);
         security.allow("cancel", SecurityConstants.VIEW);
         security.allow("update", SecurityConstants.LOAN_UPDATE_LOAN);
 
         security.allow("getPrdOfferings", SecurityConstants.VIEW);
         security.allow("load", SecurityConstants.VIEW);
         security.allow("schedulePreview", SecurityConstants.VIEW);
         security.allow("preview", SecurityConstants.VIEW);
         security.allow("previous", SecurityConstants.VIEW);
         security.allow("create", SecurityConstants.VIEW);
 
         security.allow("loadChangeLog", SecurityConstants.VIEW);
         security.allow("cancelChangeLog", SecurityConstants.VIEW);
         security.allow("waiveChargeDue", SecurityConstants.VIEW);
         security.allow("forwardWaiveCharge", SecurityConstants.VIEW);
         security.allow("waiveChargeOverDue", SecurityConstants.VIEW);
         security.allow("redoLoanBegin", SecurityConstants.CAN_REDO_LOAN_DISPURSAL);
         security.allow("captureQuestionResponses", SecurityConstants.VIEW);
         security.allow("editQuestionResponses", SecurityConstants.VIEW);
         security.allow("validateInstallments", SecurityConstants.VIEW);
         security.allow("showPreview", SecurityConstants.VIEW);
         return security;
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward getPrdOfferings(final ActionMapping mapping, final ActionForm form,
                                          final HttpServletRequest request,
                                          @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         final LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
 
         Integer customerId = loanActionForm.getCustomerIdValue();
         LoanCreationProductDetailsDto loanCreationProductDetailsDto = this.loanAccountServiceFacade.retrieveGetProductDetailsForLoanAccountCreation(customerId);
 
         storeCollectionOnSessionForUseInJspPage(request, LoanConstants.LOANPRDOFFERINGS, loanCreationProductDetailsDto.getLoanProductDtos());
         storeObjectOnSessionForUseInJspPage(request, LoanConstants.LOANACCOUNTOWNER, loanCreationProductDetailsDto.getCustomerDetailDto());
         storeObjectOnSessionForUseInJspPage(request, LoanConstants.PROPOSED_DISBURSAL_DATE, loanCreationProductDetailsDto.getNextMeetingDate());
 
         storeRedoLoanSettingOnRequestForUseInJspIfPerspectiveParamaterOnQueryString(request);
 
         if (loanCreationProductDetailsDto.isGlimEnabled()) {
             setGlimEnabledSessionAttributes(request, loanCreationProductDetailsDto.isGroup());
             request.setAttribute(METHODCALLED, "getPrdOfferings");
 
             if (loanCreationProductDetailsDto.isGroup()) {
                 loanActionForm.setClientDetails(loanCreationProductDetailsDto.getClientDetails());
 
                 LoanCreationGlimDto loanCreationGlimDto = loanCreationProductDetailsDto.getLoanCreationGlimDto();
                 storeCollectionOnSessionForUseInJspPage(request, MasterConstants.BUSINESS_ACTIVITIES, loanCreationGlimDto.getLoanPurposes());
 
                 CustomerBO customer = this.customerDao.findCustomerById(customerId);
                 final List<ClientBO> activeClientsOfGroup = this.customerDao.findActiveClientsUnderGroup(customer);
 
                 storeCollectionOnSessionForUseInJspPage(request, LoanConstants.CLIENT_LIST, activeClientsOfGroup);
                 storeObjectOnSessionForUseInJspPage(request, "clientListSize", activeClientsOfGroup.size());
             }
         }
 
         handleRepaymentsIndependentOfMeetingIfConfigured(request, loanActionForm, loanCreationProductDetailsDto.getRecurMonth());
 
         return mapping.findForward(ActionForwards.getPrdOfferigs_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward load(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                               @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
         loanActionForm.clearDetailsForLoan();
 
         Integer customerId = loanActionForm.getCustomerIdValue();
         Short productId = loanActionForm.getPrdOfferingIdValue();
 
         LoanCreationLoanDetailsDto loanCreationDetailsDto = this.loanAccountServiceFacade.retrieveLoanDetailsForLoanAccountCreation(customerId, productId);
 
         MeetingDto meetingDetail = loanCreationDetailsDto.getLoanOfferingMeetingDetail();
         RecurrenceType loanOfferingRecurrence = RecurrenceType.fromInt(meetingDetail.getMeetingDetailsDto().getRecurrenceTypeId().shortValue());
 
         if (loanCreationDetailsDto.isRepaymentIndependentOfMeetingEnabled()) {
             loanActionForm.setMonthDay("");
             loanActionForm.setMonthWeek("0");
             loanActionForm.setMonthRank("0");
 
             if (RecurrenceType.MONTHLY.equals(loanOfferingRecurrence)) {
                 // 2 is signaled as the schedule is monthly on jsp page (Monthradio button is clicked)
                 loanActionForm.setFrequency("2");
                 loanActionForm.setRecurMonth(meetingDetail.getMeetingDetailsDto().getEvery().toString());
                 loanActionForm.setDayRecurMonth(meetingDetail.getMeetingDetailsDto().getEvery().toString());
 
                 Integer weekOfMonth = meetingDetail.getMeetingDetailsDto().getRecurrenceDetails().getWeekOfMonth();
                 if (weekOfMonth != null && weekOfMonth > 0) {
                     // 2 is signaled as the day of week is chosen on jsp page. For ex,
                     // First Monday of every 2 months
                     loanActionForm.setMonthType("2");
                     loanActionForm.setMonthRank(weekOfMonth.toString());
                     loanActionForm.setMonthWeek(meetingDetail.getMeetingDetailsDto().getRecurrenceDetails().getDayOfWeek().toString());
 
                 } else {
                     // 1 is signaled as the day of month is chosen on jsp page. For ex,
                     // 12 th day of every 1 month
                     loanActionForm.setMonthType("1");
                     loanActionForm.setMonthDay(meetingDetail.getMeetingDetailsDto().getRecurrenceDetails().getDayNumber().toString());
                 }
             } else {
                 MeetingDto customerMeeting = loanCreationDetailsDto.getCustomerMeetingDetail();
                 // 1 is signaled as the schedule is weekly on jsp page. Week radio
                 // button is clicked
                 loanActionForm.setFrequency("1");
                 loanActionForm.setRecurWeek(customerMeeting.getMeetingDetailsDto().getEvery().toString());
                 loanActionForm.setWeekDay(customerMeeting.getMeetingDetailsDto().getRecurrenceDetails().getDayOfWeek().toString());
             }
         }
 
         LoanOfferingBO loanProduct = this.loanProductDao.findById(productId.intValue());
         CustomerBO customer = this.customerDao.findCustomerById(customerId);
         LoanAmountOption eligibleLoanAmount = loanProduct.eligibleLoanAmount(customer.getMaxLoanAmount(loanProduct),
                 customer.getMaxLoanCycleForProduct(loanProduct));
 
         loanActionForm.setLoanAmountRange(eligibleLoanAmount);
 
         LoanOfferingInstallmentRange eligibleNoOfInstall = loanProduct.eligibleNoOfInstall(customer.getMaxLoanAmount(loanProduct), customer.getMaxLoanCycleForProduct(loanProduct));
 
         String loanAmount = getDoubleStringForMoney(eligibleLoanAmount.getDefaultLoanAmount(), loanProduct.getCurrency());
         loanActionForm.setLoanAmount(loanAmount);
         loanActionForm.setMaxInterestRate(loanProduct.getMaxInterestRate());
         loanActionForm.setMinInterestRate(loanProduct.getMinInterestRate());
 
         loanActionForm.setInstallmentRange(eligibleNoOfInstall);
         loanActionForm.setNoOfInstallments(getStringValue(eligibleNoOfInstall.getDefaultNoOfInstall()));
         loanActionForm.setInterestRate(getDoubleStringForInterest(loanProduct.getDefInterestRate()));
         loanActionForm.setIntDedDisbursement(getStringValue(loanProduct.isIntDedDisbursement()));
         loanActionForm.setGracePeriodDuration(getStringValue(loanProduct.getGracePeriodDuration()));
         loanActionForm.setDisbursementDate(DateUtils.getUserLocaleDate(getUserContext(request).getPreferredLocale(),
                 SessionUtils.getAttribute(PROPOSED_DISBURSAL_DATE, request).toString()));
 
         if (isRedoOperation(request.getParameter(PERSPECTIVE))) {
             loanActionForm.setDisbursementDate("");
         }
 
         loanActionForm.setCustomFields(new ArrayList<CustomFieldDto>());
         UserContext userContext = getUserContext(request);
         List<FeeDto> additionalFees = new ArrayList<FeeDto>();
         List<FeeDto> defaultFees = new ArrayList<FeeDto>();
 
         new LoanProductService(new LoanPrdBusinessService()).getDefaultAndAdditionalFees(productId, userContext, defaultFees, additionalFees);
 
         LoanOfferingBO loanOffering = new LoanPrdBusinessService().getLoanOffering(productId, userContext.getLocaleId());
 
         if (AccountingRules.isMultiCurrencyEnabled()) {
             defaultFees = getFilteredFeesByCurrency(defaultFees, loanOffering.getCurrency().getCurrencyId());
             additionalFees = getFilteredFeesByCurrency(additionalFees, loanOffering.getCurrency().getCurrencyId());
         }
 
         loanActionForm.setDefaultFees(defaultFees);
 
         CustomValueDto customValueDto = new MasterPersistence().getLookUpEntity(MasterConstants.COLLATERAL_TYPES, userContext.getLocaleId());
         List<CustomValueListElementDto> collateralTypes = customValueDto.getCustomValueListElements();
 
         SessionUtils.setCollectionAttribute(LoanConstants.CUSTOM_FIELDS, new ArrayList<CustomFieldDefinitionEntity>(), request);
         SessionUtils.setCollectionAttribute(ADDITIONAL_FEES_LIST, additionalFees, request);
         SessionUtils.setCollectionAttribute(MasterConstants.COLLATERAL_TYPES, collateralTypes, request);
         SessionUtils.setCollectionAttribute(MasterConstants.BUSINESS_ACTIVITIES, loanCreationDetailsDto.getLoanPurposes(), request);
 
         Short recurrenceType = loanOfferingRecurrence.getValue();
         SessionUtils.setAttribute(RECURRENCEID, recurrenceType, request);
         request.setAttribute(RECURRENCEID, recurrenceType);
 
         SessionUtils.removeAttribute(LOANOFFERING, request);
         SessionUtils.setAttribute(LOANOFFERING, loanProduct, request);
         SessionUtils.setCollectionAttribute(LOANFUNDS, getFunds(loanOffering), request);
 
         storeRedoLoanSettingOnRequestForUseInJspIfPerspectiveParamaterOnQueryString(request);
         setVariableInstallmentDetailsOnForm(loanProduct, loanActionForm);
 
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     private List<FundBO> getFunds(final LoanOfferingBO loanOffering) {
         List<FundBO> funds = new ArrayList<FundBO>();
         if (loanOffering.getLoanOfferingFunds() != null && loanOffering.getLoanOfferingFunds().size() > 0) {
             for (LoanOfferingFundEntity loanOfferingFund : loanOffering.getLoanOfferingFunds()) {
                 funds.add(loanOfferingFund.getFund());
             }
         }
         return funds;
     }
 
     private List<FeeDto> getFilteredFeesByCurrency(List<FeeDto> defaultFees, Short currencyId) {
         List<FeeDto> filteredFees = new ArrayList<FeeDto>();
         for (FeeDto feeDto : defaultFees) {
             if (feeDto.isValidForCurrency(currencyId)) {
                 filteredFees.add(feeDto);
             }
         }
         return filteredFees;
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validateInstallments(final ActionMapping mapping, final ActionForm form,
                                               final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response)
             throws Exception {
 
         LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
 
         boolean validateInstallmentsPassed = validateInstallments(request, loanActionForm);
 
         boolean cashflowBounded = bindCashflowIfPresent(request, loanActionForm);
 
         ActionForwards forward = ActionForwards.validateInstallments_failure;
         if (validateInstallmentsPassed && cashflowBounded) {
             forward = validateCashFlowForWarningAndErrors(loanActionForm, request, loanActionForm, ActionForwards.validateInstallments_success, ActionForwards.validateInstallments_failure);
 
         }
         return mapping.findForward(forward.name());
     }
 
 
     private ActionErrors validateCashFlowForInstallmentsForWarnings(HttpServletRequest request, LoanAccountActionForm loanActionForm) throws Exception {
         return getActionErrors(loanServiceFacade.validateCashFlowForInstallmentsForWarnings(loanActionForm, getUserContext(request).getLocaleId()));
     }
 
 
     private ActionErrors validateCashflowAndInstallmentDates(List<RepaymentScheduleInstallment> installments, CashFlowForm cashFlowForm, Double repaymentCapacity) {
         return getActionErrors(loanServiceFacade.validateCashFlowForInstallments(installments, cashFlowForm, repaymentCapacity));
     }
 
     private boolean validateInstallments(HttpServletRequest request, LoanAccountActionForm loanActionForm) throws Exception {
         boolean result = true;
         UserContext userContext = getUserContext(request);
         LoanOfferingBO loanOffering = getLoanOffering(loanActionForm.getPrdOfferingIdValue(), userContext.getLocaleId());
         if (loanOffering.isVariableInstallmentsAllowed()) {
             List<RepaymentScheduleInstallment> installments = loanActionForm.getInstallments();
             VariableInstallmentDetailsBO variableInstallmentDetails = loanOffering.getVariableInstallmentDetails();
             java.sql.Date disbursementDate = loanActionForm.getDisbursementDateValue(userContext.getPreferredLocale());
             Errors errors = loanServiceFacade.validateInputInstallments(disbursementDate, variableInstallmentDetails, installments, loanActionForm.getCustomerIdValue());
             ActionErrors actionErrors = getActionErrors(errors);
             actionErrors.add(validateCashflowAndInstallmentDates(installments, loanActionForm.getCashFlowForm(), loanOffering.getRepaymentCapacity()));
             if (actionErrors.isEmpty()) {
                 loanBusinessService.applyDailyInterestRates(
                         new LoanScheduleGenerationDto(disbursementDate, loanActionForm.getLoanAmountValue(),
                                 loanActionForm.getInterestDoubleValue(), installments));
                 // TODO need to figure out a way to avoid putting 'installments' onto session - required for mifostabletag in schedulePreview.jsp
                 setInstallmentsOnSession(request, loanActionForm);
                 actionErrors = getActionErrors(loanServiceFacade.validateInstallmentSchedule(installments, variableInstallmentDetails));
                 if (!actionErrors.isEmpty()) {
                     addErrors(request, actionErrors);
                     result = false;
                 }
             } else {
                 addErrors(request, actionErrors);
                 result = false;
             }
         }
         return result;
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward showPreview(final ActionMapping mapping, final ActionForm form,
                                      final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "showPreview");
 
         ActionForward forwardAfterCashflowBinding = cashFlowAdaptor.bindCashFlow((CashFlowCaptor) form,
                 ActionForwards.schedulePreview_success.toString(), request.getSession(), mapping);
 
         bindCashflowIfPresent(request, form);
 
         return forwardAfterCashflowBinding;
     }
 
     private boolean bindCashflowIfPresent(final HttpServletRequest request, final ActionForm form) throws Exception {
         boolean cashflowBound = false;
         UserContext userContext = getUserContext(request);
         LoanAccountActionForm loanForm = (LoanAccountActionForm) form;
         LoanOfferingBO loanOffering = getLoanOffering(loanForm.getPrdOfferingIdValue(), userContext.getLocaleId());
 
         if (loanOffering != null && loanOffering.isCashFlowCheckEnabled()) {
             InstallmentAndCashflowComparisionUtility cashflowUtility = new InstallmentAndCashflowComparisionUtility(
                     loanForm.getInstallments(),
                     loanForm.getCashFlowForm().getMonthlyCashFlows(),
                     loanForm.getLoanAmountAsBigDecimal(),
                     loanForm.getDisbursementDateValue(userContext.getPreferredLocale()));
 
             loanForm.setCashflowDataHtmlBeans(cashflowUtility.mapToCashflowDataHtmlBeans());
             cashflowBound = true;
         }
         return cashflowBound;
     }
 
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward schedulePreview(final ActionMapping mapping, final ActionForm form,
                                          final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response)
             throws Exception {
 
         LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
         UserContext userContext = getUserContext(request);
         Short productId = loanActionForm.getPrdOfferingIdValue();
         LoanOfferingBO loanOffering = getLoanOffering(productId, userContext.getLocaleId());
         setVariableInstallmentDetailsOnForm(loanOffering, loanActionForm);
         DateTime disbursementDate = getDisbursementDate(loanActionForm, userContext.getPreferredLocale());
         CustomerDetailDto oldCustomer = getCustomer(request);
 
         Short fundId = loanActionForm.getLoanOfferingFundValue();
 //        FundBO fund = getFund(request, fundId);
 
         LoanCreationLoanScheduleDetailsDto loanScheduleDetailsDto;
         try {
             if (isRedoOperation(request.getParameter(PERSPECTIVE))) {
                 loanScheduleDetailsDto = loanServiceFacade.retrieveScheduleDetailsForRedoLoan(oldCustomer.getCustomerId(), disbursementDate,
                         fundId, loanActionForm);
 
                 loanActionForm.initializeTransactionFields(loanScheduleDetailsDto.getPaymentDataBeans());
             } else {
                 loanScheduleDetailsDto = loanServiceFacade.retrieveScheduleDetailsForLoanCreation(oldCustomer.getCustomerId(), disbursementDate,
                         fundId, loanActionForm);
                 loanActionForm.initializeInstallments(loanScheduleDetailsDto.getInstallments());
             }
         } finally {
             setPerspectiveOnRequest(request);
         }
 
         setAttributesForSchedulePreview(request, loanActionForm, disbursementDate, loanScheduleDetailsDto);
         questionGroupFilter.setLoanOfferingBO(loanOffering);
         BigDecimal loanAmount = loanActionForm.getLoanAmountAsBigDecimal();
         ActionForward pageAfterQuestionnaire = getPageAfterQuestionnaire(mapping, request, loanOffering, loanScheduleDetailsDto, cashFlowAdaptor, loanAmount);
         return createLoanQuestionnaire.fetchAppliedQuestions(mapping, loanActionForm, request, ActionForwards.valueOf(pageAfterQuestionnaire.getName()));
     }
 
     private void setAttributesForSchedulePreview(HttpServletRequest request, LoanAccountActionForm loanActionForm, DateTime disbursementDate, LoanCreationLoanScheduleDetailsDto loanScheduleDetailsDto) throws PageExpiredException {
         setGlimOnSession(request, loanActionForm, loanScheduleDetailsDto);
         SessionUtils.setAttribute(CustomerConstants.PENDING_APPROVAL_DEFINED, loanScheduleDetailsDto.isLoanPendingApprovalDefined(), request);
         SessionUtils.setAttribute(CustomerConstants.DISBURSEMENT_DATE, disbursementDate, request);
         SessionUtils.setAttribute(CustomerConstants.LOAN_AMOUNT, loanActionForm.getLoanAmount(), request);
         // TODO need to figure out a way to avoid putting 'installments' onto session - required for mifostabletag in schedulePreview.jsp
         setInstallmentsOnSession(request, loanActionForm);
     }
 
     // Intentionally made 'public' to aid testing
     // CashFlowAdaptor passed to aid testing
 
     public ActionForward getPageAfterQuestionnaire(ActionMapping mapping, HttpServletRequest request,
                                                    LoanOfferingBO loanOffering,
                                                    LoanCreationLoanScheduleDetailsDto loanScheduleDetailsDto,
                                                    CashFlowAdaptor cashFlowAdaptor, BigDecimal loanAmount) {
         if (loanOffering.isCashFlowCheckEnabled()) {
             return cashFlowAdaptor.renderCashFlow(
                     loanScheduleDetailsDto.firstInstallmentDueDate(),
                     loanScheduleDetailsDto.lastInstallmentDueDate(),
                     SHOW_PREVIEW, CUSTOMER_SEARCH_URL, mapping, request, loanOffering, loanAmount);
         }
         return mapping.findForward(ActionForwards.schedulePreview_success.toString());
     }
 
     private DateTime getDisbursementDate(LoanAccountActionForm loanActionForm, Locale locale) throws InvalidDateException {
         return new DateTime(loanActionForm.getDisbursementDateValue(locale));
     }
 
     private void setGlimOnSession(HttpServletRequest request, LoanAccountActionForm loanActionForm,
                                   LoanCreationLoanScheduleDetailsDto loanScheduleDetailsDto) throws PageExpiredException {
         if (loanScheduleDetailsDto.isGlimApplicable()) {
             setGlimEnabledSessionAttributes(request, loanScheduleDetailsDto.isGroup());
             loanActionForm.setLoanAmount(Double.toString(loanScheduleDetailsDto.getGlimLoanAmount()));
         }
     }
 
     private void setPerspectiveOnRequest(HttpServletRequest request) {
         String perspective = request.getParameter(PERSPECTIVE);
         if (perspective != null) {
             request.setAttribute(PERSPECTIVE, request.getParameter(PERSPECTIVE));
         }
     }
 
     private CustomerDetailDto getCustomer(HttpServletRequest request) throws PageExpiredException {
         return (CustomerDetailDto) SessionUtils.getAttribute(LOANACCOUNTOWNER, request);
     }
 
     private void setVariableInstallmentDetailsOnForm(LoanOfferingBO loanOffering, LoanAccountActionForm loanActionForm) {
         boolean variableInstallmentsAllowed = loanOffering.isVariableInstallmentsAllowed();
         loanActionForm.setVariableInstallmentsAllowed(variableInstallmentsAllowed);
         if (variableInstallmentsAllowed) {
             VariableInstallmentDetailsBO variableInstallmentDetails = loanOffering.getVariableInstallmentDetails();
             loanActionForm.setMinimumGapInDays(variableInstallmentDetails.getMinGapInDays());
             loanActionForm.setMaximumGapInDays(variableInstallmentDetails.getMaxGapInDays());
             loanActionForm.setMinInstallmentAmount(variableInstallmentDetails.getMinInstallmentAmount());
         }
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward preview(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                                  @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         LoanAccountActionForm loanAccountForm = (LoanAccountActionForm) form;
         String perspective = loanAccountForm.getPerspective();
         if (perspective != null) {
             Integer customerId = loanAccountForm.getCustomerIdValue();
 
             if (perspective.equals(PERSPECTIVE_VALUE_REDO_LOAN)) {
                 UserContext userContext = getUserContext(request);
                 DateTime disbursementDate = getDisbursementDate(loanAccountForm, userContext.getPreferredLocale());
                 LoanBO loan = loanServiceFacade.previewLoanRedoDetails(customerId, loanAccountForm, disbursementDate);
 
                 String loanDisbursementDate = DateUtils.getUserLocaleDate(null, disbursementDate.toDate());
                 SessionUtils.setAttribute("loanDisbursementDate", loanDisbursementDate, request);
                 SessionUtils.setAttribute(Constants.BUSINESS_KEY, loan, request);
             }
 
             List<LoanAccountDetailsDto> accountDetails = loanAccountForm.getClientDetails();
             List<String> selectedClientIds = loanAccountForm.getClients();
 
             LoanCreationPreviewDto loanPreviewDto = loanAccountServiceFacade.previewLoanCreationDetails(customerId, accountDetails, selectedClientIds);
 
             List<BusinessActivityEntity> businessActEntity = retrieveLoanPurposesFromSession(request);
             for (LoanAccountDetailsDto loanAccountDetailsView : loanPreviewDto.getLoanAccountDetailsView()) {
                 String businessActName = null;
                 for (ValueListElement busact : businessActEntity) {
                     if (busact.getId().toString().equals(loanAccountDetailsView.getBusinessActivity())) {
                         businessActName = busact.getName();
                     }
                 }
                 loanAccountDetailsView.setBusinessActivityName((StringUtils.isNotBlank(businessActName) ? businessActName : "-").toString());
             }
 
             if (loanPreviewDto.isGlimEnabled()) {
                 SessionUtils.setAttribute(LOAN_INDIVIDUAL_MONITORING_IS_ENABLED, 1, request);
 
                 if (loanPreviewDto.isGroup()) {
                     SessionUtils.setAttribute(LOAN_ACCOUNT_OWNER_IS_A_GROUP, LoanConstants.LOAN_ACCOUNT_OWNER_IS_GROUP_YES, request);
                     SessionUtils.setCollectionAttribute("loanAccountDetailsView", loanPreviewDto.getLoanAccountDetailsView(), request);
                 }
             }
             request.setAttribute(PERSPECTIVE, perspective);
         }
         // TODO need to figure out a way to avoid putting 'installments' onto session - required for mifostabletag in createloanpreview.jsp
         setInstallmentsOnSession(request, loanAccountForm);
         ActionForwards forward = validateInstallmentsAndCashFlow(form, request, loanAccountForm);
         return mapping.findForward(forward.name());
     }
 
     private ActionForwards validateInstallmentsAndCashFlow(ActionForm form, HttpServletRequest request,
                                                            LoanAccountActionForm loanAccountForm) throws Exception {
         ActionForwards forward = validateInstallments(request, loanAccountForm) ?
                 ActionForwards.preview_success :
                 ActionForwards.preview_failure;
 
         boolean cashFlowBounded = bindCashflowIfPresent(request, loanAccountForm);
         //----to display errors on second page
         if (forward.equals(ActionForwards.preview_success) && cashFlowBounded) {
             forward = validateCashFlowForWarningAndErrors(form, request, loanAccountForm, ActionForwards.preview_success, ActionForwards.preview_failure);
         }
         return forward;
     }
 
     private ActionForwards validateCashFlowForWarningAndErrors(ActionForm form, HttpServletRequest request, LoanAccountActionForm loanAccountForm, ActionForwards preview_success, ActionForwards failure) throws Exception {
         validateCashFlowForWarning(form, request);
         ActionForwards forward = validateCashFlowAndInstallmentDatesForErrors(loanAccountForm, request) ?
                 preview_success :
                 failure;
         return forward;
     }
 
     private boolean validateCashFlowAndInstallmentDatesForErrors(LoanAccountActionForm loanAccountForm,  HttpServletRequest request) throws Exception {
         UserContext userContext = getUserContext(request);
         LoanOfferingBO loanOffering = getLoanOffering(loanAccountForm.getPrdOfferingIdValue(), userContext.getLocaleId());
         return addErrorAndReturnResult(request, validateCashflowAndInstallmentDates(loanAccountForm.getInstallments(),
                 loanAccountForm.getCashFlowForm(), loanOffering.getRepaymentCapacity()));
     }
 
     private boolean validateCashFlowForWarning(ActionForm form, HttpServletRequest request) throws Exception {
         return addErrorAndReturnResult(request, validateCashFlowForInstallmentsForWarnings(request, (LoanAccountActionForm) form));
     }
 
     private boolean addErrorAndReturnResult(HttpServletRequest request, ActionErrors actionErrors) {
         boolean isEmpty = actionErrors.isEmpty();
         if (!isEmpty) {
             addErrors(request, actionErrors);
         }
         return isEmpty;
     }
 
     private void setInstallmentsOnSession(HttpServletRequest request, LoanAccountActionForm loanAccountForm) throws PageExpiredException {
         SessionUtils.setCollectionAttribute(LoanConstants.INSTALLMENTS, loanAccountForm.getInstallments(), request);
     }
 
     @SuppressWarnings("unchecked")
     private List<BusinessActivityEntity> retrieveLoanPurposesFromSession(final HttpServletRequest request)
             throws PageExpiredException {
         return (List<BusinessActivityEntity>) SessionUtils.getAttribute(MasterConstants.BUSINESS_ACTIVITIES, request);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward getInstallmentDetails(final ActionMapping mapping,
                                                @SuppressWarnings("unused") final ActionForm form, final HttpServletRequest request,
                                                @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         Integer accountId = Integer.valueOf(request.getParameter(ACCOUNT_ID));
 
         LoanInstallmentDetailsDto loanInstallmentDetailsDto = this.loanServiceFacade
                 .retrieveInstallmentDetails(accountId);
 
         SessionUtils.setAttribute(VIEW_UPCOMING_INSTALLMENT_DETAILS, loanInstallmentDetailsDto
                 .getUpcomingInstallmentDetails(), request);
         SessionUtils.setAttribute(VIEW_OVERDUE_INSTALLMENT_DETAILS, loanInstallmentDetailsDto
                 .getOverDueInstallmentDetails(), request);
         SessionUtils.setAttribute(TOTAL_AMOUNT_OVERDUE, loanInstallmentDetailsDto.getTotalAmountDue(), request);
         SessionUtils.setAttribute(NEXTMEETING_DATE, loanInstallmentDetailsDto.getNextMeetingDate(), request);
 
         return mapping.findForward(VIEWINSTALLMENTDETAILS_SUCCESS);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward getAllActivity(final ActionMapping mapping, @SuppressWarnings("unused") final ActionForm form,
                                         final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response)
             throws Exception {
 
         String globalAccountNum = request.getParameter(GLOBAL_ACCOUNT_NUM);
 
         List<LoanActivityDto> allLoanAccountActivities = this.loanServiceFacade
                 .retrieveAllLoanAccountActivities(globalAccountNum);
 
         SessionUtils.setCollectionAttribute(LOAN_ALL_ACTIVITY_VIEW, allLoanAccountActivities, request);
         return mapping.findForward(MethodNameConstants.GETALLACTIVITY_SUCCESS);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward forwardWaiveCharge(final ActionMapping mapping,
                                             @SuppressWarnings("unused") final ActionForm form, final HttpServletRequest request,
                                             @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         return mapping.findForward("waive" + request.getParameter("type") + "Charges_Success");
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward get(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                              final HttpServletResponse response) throws Exception {
         LoanAccountActionForm loanAccountActionForm = (LoanAccountActionForm) form;
         loanAccountActionForm.clearDetailsForLoan();
         String globalAccountNum = request.getParameter(GLOBAL_ACCOUNT_NUM);
         UserContext userContext = getUserContext(request);
         LoanInformationDto loanInformationDto = this.loanServiceFacade.getLoanInformationDto(globalAccountNum, userContext);
 
         final String accountStateNameLocalised = MessageLookup.getInstance().lookup(
                 loanInformationDto.getAccountStateName(), userContext);
         SessionUtils.removeThenSetAttribute("accountStateNameLocalised", accountStateNameLocalised, request);
         final String gracePeriodTypeNameLocalised = MessageLookup.getInstance().lookup(
                 loanInformationDto.getGracePeriodTypeName(), userContext);
         SessionUtils.removeThenSetAttribute("gracePeriodTypeNameLocalised", gracePeriodTypeNameLocalised, request);
         final String interestTypeNameLocalised = MessageLookup.getInstance().lookup(
                 loanInformationDto.getInterestTypeName(), userContext);
         SessionUtils.removeThenSetAttribute("interestTypeNameLocalised", interestTypeNameLocalised, request);
         final Set<String> accountFlagStateEntityNamesLocalised = new HashSet<String>();
         for (String name : loanInformationDto.getAccountFlagNames()) {
             accountFlagStateEntityNamesLocalised.add(MessageLookup.getInstance().lookup(name, userContext));
         }
         SessionUtils.setCollectionAttribute("accountFlagNamesLocalised", accountFlagStateEntityNamesLocalised, request);
 
         String customerId = request.getParameter(CUSTOMER_ID);
         SessionUtils.removeAttribute(BUSINESS_KEY, request);
 
         Integer loanIndividualMonitoringIsEnabled = configurationPersistence.getConfigurationKeyValueInteger(
                 LOAN_INDIVIDUAL_MONITORING_IS_ENABLED).getValue();
 
         if (null != loanIndividualMonitoringIsEnabled && loanIndividualMonitoringIsEnabled.intValue() != 0) {
             SessionUtils.setAttribute(LOAN_INDIVIDUAL_MONITORING_IS_ENABLED, loanIndividualMonitoringIsEnabled
                     .intValue(), request);
         }
         setBusinessActivitiesIntoSession(request);
 
         if (null != loanIndividualMonitoringIsEnabled && 0 != loanIndividualMonitoringIsEnabled.intValue()
                 && loanInformationDto.isGroup()) {
 
             List<BusinessActivityEntity> businessActEntity = (List<BusinessActivityEntity>) SessionUtils
                     .getAttribute("BusinessActivities", request);
             SessionUtils.setCollectionAttribute("loanAccountDetailsView",
                     loanServiceFacade.getLoanAccountDetailsViewList(loanInformationDto,
                             businessActEntity, clientBusinessService), request);
         }
         loadCustomFieldDefinitions(request);
         loadMasterData(request);
         SessionUtils.setAttribute(AccountConstants.LAST_PAYMENT_ACTION, loanBusinessService
                 .getLastPaymentAction(loanInformationDto.getAccountId()), request);
         SessionUtils.removeThenSetAttribute("loanInformationDto", loanInformationDto, request);
 
         request.setAttribute(CustomerConstants.SURVEY_KEY, loanInformationDto.getAccountSurveys());
         request.setAttribute(CustomerConstants.SURVEY_COUNT, loanInformationDto.getActiveSurveys());
         request.setAttribute(AccountConstants.SURVEY_KEY, loanInformationDto.getAccountSurveys());
 
         Integer administrativeDocumentsIsEnabled = configurationPersistence.getConfigurationKeyValueInteger(
                 ADMINISTRATIVE_DOCUMENT_IS_ENABLED).getValue();
 
         if (null != administrativeDocumentsIsEnabled && administrativeDocumentsIsEnabled.intValue() == 1) {
             SessionUtils.setCollectionAttribute(AdminDocumentsContants.ADMINISTRATIVEDOCUMENTSLIST,
                     new AdminDocumentPersistence().getAllAdminDocuments(), request);
 
             SessionUtils.setCollectionAttribute(AdminDocumentsContants.ADMINISTRATIVEDOCUMENTSACCSTATEMIXLIST,
                     new AdminDocAccStateMixPersistence().getAllMixedAdminDocuments(), request);
 
         }
 
         // John W - temporarily put back because needed in applychargeaction - update
         LoanBO loan = new LoanDaoHibernate(new GenericDaoHibernate()).findById(loanInformationDto.getAccountId());
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, loan, request);
         setCurrentPageUrl(request, loan);
         setQuestionGroupInstances(request, loan);
         List<RepaymentScheduleInstallment> installments = loan.toRepaymentScheduleDto(userContext.getPreferredLocale());
         loanAccountActionForm.initializeInstallments(installments);
         return mapping.findForward(ActionForwards.get_success.toString());
     }
 
     private void setQuestionGroupInstances(HttpServletRequest request, LoanBO loanBO) throws PageExpiredException {
         QuestionnaireServiceFacade questionnaireServiceFacade = questionnaireServiceFacadeLocator.getService(request);
         if (questionnaireServiceFacade == null) {
             return;
         }
         setQuestionGroupInstances(questionnaireServiceFacade, request, loanBO.getAccountId());
     }
 
     // Intentionally made public to aid testing !
 
     public void setQuestionGroupInstances(QuestionnaireServiceFacade questionnaireServiceFacade, HttpServletRequest request, Integer loanAccountId) throws PageExpiredException {
         List<QuestionGroupInstanceDetail> instanceDetails = questionnaireServiceFacade.getQuestionGroupInstances(loanAccountId, "View", "Loan");
         SessionUtils.setCollectionAttribute("questionGroupInstances", instanceDetails, request);
     }
 
     private void setCurrentPageUrl(HttpServletRequest request, LoanBO loanBO) throws PageExpiredException, UnsupportedEncodingException {
         SessionUtils.removeThenSetAttribute("currentPageUrl", constructCurrentPageUrl(request, loanBO), request);
     }
 
     private String constructCurrentPageUrl(HttpServletRequest request, LoanBO loanBO) throws UnsupportedEncodingException {
         String globalAccountNum = request.getParameter("globalAccountNum");
         String officerId = request.getParameter("recordOfficeId");
         String loanOfficerId = request.getParameter("recordLoanOfficerId");
         String url = String.format("loanAccountAction.do?globalAccountNum=%s&customerId=%s&recordOfficeId=%s&recordLoanOfficerId=%s",
                 globalAccountNum, Integer.toString(loanBO.getAccountId()), officerId, loanOfficerId);
         return URLEncoder.encode(url, "UTF-8");
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward getLoanRepaymentSchedule(final ActionMapping mapping,
                                                   @SuppressWarnings("unused") final ActionForm form, final HttpServletRequest request,
                                                   @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         LoanAccountActionForm loanAccountActionForm = (LoanAccountActionForm) form;
         UserContext userContext = getUserContext(request);
         Integer loanId = Integer.valueOf(request.getParameter(ACCOUNT_ID));
         Locale locale = userContext.getPreferredLocale();
         Date viewDate = loanAccountActionForm.getScheduleViewDateValue(locale);
         LoanBO loan = this.loanServiceFacade.retrieveLoanRepaymentSchedule(userContext, loanId, viewDate);
 
         OriginalScheduleInfoDto originalSchedule = this.loanServiceFacade.retrieveOriginalLoanSchedule(loanId, locale);
 
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, loan, request);
         SessionUtils.setAttribute(Constants.ORIGINAL_SCHEDULE_AVAILABLE, !originalSchedule.getOriginalLoanScheduleInstallment().isEmpty(), request);
         SessionUtils.setAttribute(Constants.VIEW_DATE, viewDate, request);
 
         return mapping.findForward(ActionForwards.getLoanRepaymentSchedule.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward viewOriginalSchedule(final ActionMapping mapping,
                                                   @SuppressWarnings("unused") final ActionForm form, final HttpServletRequest request,
                                                   @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         UserContext userContext = getUserContext(request);
         Integer loanId = Integer.valueOf(request.getParameter(ACCOUNT_ID));
 
         OriginalScheduleInfoDto dto = loanServiceFacade.retrieveOriginalLoanSchedule(loanId, userContext.getPreferredLocale());
         SessionUtils.setAttribute(CustomerConstants.DISBURSEMENT_DATE, dto.getDisbursementDate(), request);
         SessionUtils.setAttribute(CustomerConstants.LOAN_AMOUNT, dto.getLoanAmount(), request);
         SessionUtils.setCollectionAttribute(LoanConstants.ORIGINAL_INSTALLMENTS,
                 dto.getOriginalLoanScheduleInstallment(), request);
         return mapping.findForward(ActionForwards.viewOriginalSchedule.name());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward viewStatusHistory(final ActionMapping mapping,
                                            @SuppressWarnings("unused") final ActionForm form, final HttpServletRequest request,
                                            @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         UserContext userContext = getUserContext(request);
         String globalAccountNum = request.getParameter(GLOBAL_ACCOUNT_NUM);
         List<AccountStatusChangeHistoryEntity> accStatusChangeHistory = this.loanServiceFacade
                 .retrieveLoanAccountStatusChangeHistory(userContext, globalAccountNum);
 
         SessionUtils.setCollectionAttribute(STATUS_HISTORY, accStatusChangeHistory, request);
 
         return mapping.findForward(ActionForwards.viewStatusHistory.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validate(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                                   @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         LoanAccountActionForm loanAccountForm = (LoanAccountActionForm) form;
         String perspective = loanAccountForm.getPerspective();
         if (perspective != null) {
             request.setAttribute(PERSPECTIVE, perspective);
         }
         ActionForwards actionForward = null;
         String method = (String) request.getAttribute(METHODCALLED);
         if (method.equals(Methods.getPrdOfferings.toString())) {
             actionForward = ActionForwards.getPrdOfferigs_failure;
         } else if (method.equals(Methods.load.toString())) {
             actionForward = ActionForwards.getPrdOfferigs_success;
         } else if (method.equals(Methods.schedulePreview.toString())) {
             actionForward = ActionForwards.load_success;
         } else if (method.equals(Methods.managePreview.toString())) {
             actionForward = ActionForwards.managepreview_failure;
         } else if (method.equals(Methods.preview.toString())) {
             actionForward = ActionForwards.preview_failure;
         }
         return mapping.findForward(actionForward.toString());
     }
 
     private void setMonthlySchedule(final LoanAccountActionForm loanActionForm,
                                     final MeetingDetailsEntity meetingDetails) {
         // 2 is signaled as the schedule is monthly on jsp page (Monthradio
         // button is clicked)
         loanActionForm.setFrequency("2");
         loanActionForm.setRecurMonth(meetingDetails.getRecurAfter().toString());
         loanActionForm.setDayRecurMonth(meetingDetails.getRecurAfter().toString());
         if (meetingDetails.getWeekRank() != null) {
             // 2 is signaled as the day of week is chosen on jsp page. For ex,
             // First Monday of every 2 months
             loanActionForm.setMonthType("2");
             loanActionForm.setMonthRank(meetingDetails.getWeekRank().getValue().toString());
             loanActionForm.setMonthWeek(meetingDetails.getWeekDay().getValue().toString());
 
         } else {
             // 1 is signaled as the day of month is chosen on jsp page. For ex,
             // 12 th day of every 1 month
             loanActionForm.setMonthType("1");
             loanActionForm.setMonthDay(meetingDetails.getDayNumber().toString());
         }
     }
 
     private void setWeeklySchedule(final LoanAccountActionForm loanActionForm, final MeetingDetailsEntity meetingDetail) {
         // 1 is signaled as the schedule is weekly on jsp page. Week radio
         // button is clicked
         loanActionForm.setFrequency("1");
         loanActionForm.setRecurWeek(meetingDetail.getRecurAfter().toString());
         loanActionForm.setWeekDay(meetingDetail.getWeekDay().getValue().toString());
     }
 
     public ActionForward redoLoanBegin(final ActionMapping mapping, @SuppressWarnings("unused") final ActionForm form,
                                        @SuppressWarnings("unused") final HttpServletRequest request,
                                        @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.beginRedoLoanDisbursal_success.toString());
     }
 
     /**
      * Resolve repayment start date according to given disbursement date
      * <p/>
      * The resulting date equates to the disbursement date plus MIN_DAYS_BETWEEN_DISBURSAL_AND_FIRST_REPAYMENT_DAY: e.g.
      * If disbursement date is 18 June 2008, and MIN_DAYS_BETWEEN_DISBURSAL_AND_FIRST_REPAYMENT_DAY is 1 then the
      * repayment start date would be 19 June 2008
      *
      * @return Date repaymentStartDate
      * @throws PersistenceException
      */
     private Date resolveRepaymentStartDate(final Date disbursementDate) {
         int minDaysInterval = configurationPersistence.getConfigurationKeyValueInteger(
                 MIN_DAYS_BETWEEN_DISBURSAL_AND_FIRST_REPAYMENT_DAY).getValue();
 
         final GregorianCalendar repaymentStartDate = new GregorianCalendar();
         repaymentStartDate.setTime(disbursementDate);
         repaymentStartDate.add(Calendar.DAY_OF_WEEK, minDaysInterval);
         return repaymentStartDate.getTime();
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previous(final ActionMapping mapping, @SuppressWarnings("unused") final ActionForm form,
                                   @SuppressWarnings("unused") final HttpServletRequest request,
                                   @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward create(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                                 @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
         String perspective = loanActionForm.getPerspective();
         if (perspective != null) {
             request.setAttribute(PERSPECTIVE, perspective);
         }
 
         UserContext userContext = getUserContext(request);
         List<LoanAccountDetailsDto> loanAccountDetailsList = getLoanAccountDetailsFromSession(request);
         DateTime disbursementDate = getDisbursementDate(loanActionForm, userContext.getPreferredLocale());
         Integer customerId = (getCustomer(request)).getCustomerId();
 
         LoanCreationResultDto loanCreationResultDto;
         if (isRedoOperation(perspective)) {
             loanCreationResultDto = this.loanServiceFacade.redoLoan(userContext, customerId, disbursementDate, loanActionForm);
             LoanBO newlyCreatedLoan = this.loanDao.findByGlobalAccountNum(loanCreationResultDto.getGlobalAccountNum());
             SessionUtils.setAttribute(Constants.BUSINESS_KEY, newlyCreatedLoan, request);
         } else {
             Short fundId = loanActionForm.getLoanOfferingFundValue();
 
             LoanAccountMeetingDto loanAccountMeetingDto = new LoanAccountMeetingDto();
             if (StringUtils.isNotBlank(loanActionForm.getRecurrenceId())) {
                 loanAccountMeetingDto.setRecurrenceId(Short.valueOf(loanActionForm.getRecurrenceId()));
             }
             if (StringUtils.isNotBlank(loanActionForm.getWeekDay())) {
                 loanAccountMeetingDto.setWeekDay(Short.valueOf(loanActionForm.getWeekDay()));
             }
             if (StringUtils.isNotBlank(loanActionForm.getRecurWeek())) {
                 loanAccountMeetingDto.setEveryWeek(Short.valueOf(loanActionForm.getRecurWeek()));
             }
 
             loanAccountMeetingDto.setMonthType(loanActionForm.getMonthType());
             if (StringUtils.isNotBlank(loanActionForm.getMonthDay())) {
                 loanAccountMeetingDto.setDayOfMonth(Short.valueOf(loanActionForm.getMonthDay()));
             }
             if (StringUtils.isNotBlank(loanActionForm.getDayRecurMonth())) {
                 loanAccountMeetingDto.setDayRecurMonth(Short.valueOf(loanActionForm.getDayRecurMonth()));
             }
 
             if (StringUtils.isNotBlank(loanActionForm.getMonthWeek())) {
                 loanAccountMeetingDto.setWeekOfMonth(Short.valueOf(loanActionForm.getMonthWeek()));
             }
             if (StringUtils.isNotBlank(loanActionForm.getRecurMonth())) {
                 loanAccountMeetingDto.setEveryMonth(Short.valueOf(loanActionForm.getRecurMonth()));
             }
             if (StringUtils.isNotBlank(loanActionForm.getMonthRank())) {
                 loanAccountMeetingDto.setMonthRank(Short.valueOf(loanActionForm.getMonthRank()));
             }
 
             LoanAccountInfoDto loanAccountInfo = new LoanAccountInfoDto();
             loanAccountInfo.setCustomerId(customerId);
             loanAccountInfo.setFundId(fundId);
             loanAccountInfo.setDisbursementDate(disbursementDate.toLocalDate());
 
             loanAccountInfo.setProductId(loanActionForm.getPrdOfferingIdValue());
             loanAccountInfo.setLoanAmount(loanActionForm.getLoanAmount());
             loanAccountInfo.setInterestDeductedAtDisbursement(loanActionForm.isInterestDedAtDisbValue());
             loanAccountInfo.setInterest(loanActionForm.getInterestDoubleValue());
             loanAccountInfo.setGracePeriod(loanActionForm.getGracePeriodDurationValue());
             loanAccountInfo.setMaxLoanAmount(loanActionForm.getMaxLoanAmount());
             loanAccountInfo.setMinLoanAmount(loanActionForm.getMinLoanAmount());
             loanAccountInfo.setNumOfInstallments(loanActionForm.getNoOfInstallmentsValue());
             loanAccountInfo.setMaxNumOfInstallments(loanActionForm.getMaxNoInstallmentsValue());
             loanAccountInfo.setMinNumOfInstallments(loanActionForm.getMinNoInstallmentsValue());
             loanAccountInfo.setExternalId(loanActionForm.getExternalId());
             loanAccountInfo.setSelectedLoanPurpose(loanActionForm.getBusinessActivityIdValue());
             loanAccountInfo.setSelectedCollateralType(loanActionForm.getCollateralTypeIdValue());
             loanAccountInfo.setAccountState(loanActionForm.getState().getValue());
 
 
             List<LoanRepaymentScheduleInstallmentDto> loanRepayments = new ArrayList<LoanRepaymentScheduleInstallmentDto>();
             List<RepaymentScheduleInstallment> installments = loanActionForm.getInstallments();
             for (RepaymentScheduleInstallment installment : installments) {
                 LoanRepaymentScheduleInstallmentDto repaymentInstallment = new LoanRepaymentScheduleInstallmentDto(installment.getInstallment(), installment.getPrincipal().toString(), installment.getInterest().toString(), new LocalDate(installment.getDueDateValue()));
                 loanRepayments.add(repaymentInstallment);
             }
 
             loanCreationResultDto = this.loanAccountServiceFacade.createLoan(loanAccountMeetingDto, loanAccountInfo, loanRepayments);
 
             createLoanQuestionnaire.saveResponses(request, loanActionForm, loanCreationResultDto.getAccountId());
         }
         cashFlowAdaptor.save((CashFlowCaptor) form, request);
 
         if (loanCreationResultDto.isGlimApplicable()) {
             boolean isRepaymentIndepOfMeetingEnabled = configService.isRepaymentIndepOfMeetingEnabled();
             for (LoanAccountDetailsDto loanAccountDetail : loanAccountDetailsList) {
                 LoanBO newlyCreatedLoan = this.loanDao.findByGlobalAccountNum(loanCreationResultDto.getGlobalAccountNum());
                newlyCreatedLoan.updateDetails(userContext);
                 createIndividualLoanAccount(loanActionForm, newlyCreatedLoan, isRepaymentIndepOfMeetingEnabled,
                                             loanAccountDetail, isRedoOperation(perspective));
             }
         }
 
         loanActionForm.setAccountId(loanCreationResultDto.getAccountId().toString());
         request.setAttribute(GLOBAL_ACCOUNT_NUM, loanCreationResultDto.getGlobalAccountNum());
 
         LoanBO newlyCreatedLoan = this.loanDao.findByGlobalAccountNum(loanCreationResultDto.getGlobalAccountNum());
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, newlyCreatedLoan, request);
 
         // NOTE: needed for link creation
         CustomerBO customer = this.customerDao.findCustomerById(customerId);
         request.setAttribute("customer", customer);
 
         return mapping.findForward(ActionForwards.create_success.toString());
     }
 
     @SuppressWarnings("unchecked")
     private List<LoanAccountDetailsDto> getLoanAccountDetailsFromSession(final HttpServletRequest request)
             throws PageExpiredException {
         return (List<LoanAccountDetailsDto>) SessionUtils.getAttribute("loanAccountDetailsView", request);
     }
 
     private boolean isRedoOperation(final String perspective) {
         return PERSPECTIVE_VALUE_REDO_LOAN.equals(perspective);
     }
 
     // TODO: merge this with GlimLoanUpdater.createIndividualLoan. But note that
     // this method
     // depends on findSystemId which expects globalCustNum. The other one
     // expects actual client id
     // This is because the LoanAccountActionForm.java is implemented such that
     // client id becomes globalcustnum
     // somewhere during the Create Account flow
 
     private void createIndividualLoanAccount(final LoanAccountActionForm loanActionForm, final LoanBO loan,
                                              final boolean isRepaymentIndepOfMeetingEnabled, final LoanAccountDetailsDto loanAccountDetail,
                                              final boolean isRedoOperation) throws AccountException, ServiceException {
         LoanBO individualLoan;
         if (isRedoOperation) {
             individualLoan = LoanBO.redoIndividualLoan(loan.getUserContext(), loan.getLoanOffering(),
                     new CustomerBusinessService().findBySystemId(loanAccountDetail.getClientId()), loanActionForm
                             .getState(), new Money(loan.getCurrency(), loanAccountDetail.getLoanAmount().toString()),
                     loan.getNoOfInstallments(), loan.getDisbursementDate(), false, isRepaymentIndepOfMeetingEnabled,
                     loan.getInterestRate(), loan.getGracePeriodDuration(), loan.getFund(), new ArrayList<FeeDto>(),
                     new ArrayList<CustomFieldDto>());
 
         } else {
             individualLoan = LoanBO.createIndividualLoan(loan.getUserContext(), loan.getLoanOffering(),
                     new CustomerBusinessService().findBySystemId(loanAccountDetail.getClientId()), loanActionForm
                             .getState(), new Money(loan.getCurrency(), loanAccountDetail.getLoanAmount().toString()),
                     loan.getNoOfInstallments(), loan.getDisbursementDate(), false, isRepaymentIndepOfMeetingEnabled,
                     loan.getInterestRate(), loan.getGracePeriodDuration(), loan.getFund(), new ArrayList<FeeDto>(),
                     new ArrayList<CustomFieldDto>(), false);
         }
 
         individualLoan.setParentAccount(loan);
 
         if (!StringUtils.isBlank(loanAccountDetail.getBusinessActivity())) {
             individualLoan.setBusinessActivityId(Integer.valueOf(loanAccountDetail.getBusinessActivity()));
         }
 
         individualLoan.save();
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward manage(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                                 @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         LoanAccountActionForm loanActionForm = (LoanAccountActionForm) form;
         String globalAccountNum = request.getParameter(GLOBAL_ACCOUNT_NUM);
         CustomerBO customer = getCustomerFromRequest(request);
 
         if (isGlimEnabled()) {
             populateGlimAttributes(request, loanActionForm, globalAccountNum, customer);
         }
 
         String recurMonth = customer.getCustomerMeeting().getMeeting().getMeetingDetails().getRecurAfter().toString();
         handleRepaymentsIndependentOfMeetingIfConfigured(request, loanActionForm, recurMonth);
         LoanBO loanBO = new LoanDaoHibernate(new GenericDaoHibernate()).findByGlobalAccountNum(globalAccountNum);
 
         UserContext userContext = getUserContext(request);
         loanBO.setUserContext(userContext);
         SessionUtils.setAttribute(PROPOSED_DISBURSAL_DATE, loanBO.getDisbursementDate(), request);
         SessionUtils.removeAttribute(LOANOFFERING, request);
         LoanOfferingBO loanOffering = getLoanOffering(loanBO.getLoanOffering().getPrdOfferingId(), userContext.getLocaleId());
         loanActionForm.setInstallmentRange(loanBO.getMaxMinNoOfInstall());
         loanActionForm.setLoanAmountRange(loanBO.getMaxMinLoanAmount());
         MaxMinInterestRate interestRateRange = loanBO.getMaxMinInterestRate();
         loanActionForm.setMaxInterestRate(interestRateRange.getMaxLoanAmount());
         loanActionForm.setMinInterestRate(interestRateRange.getMinLoanAmount());
         loanActionForm.setExternalId(loanBO.getExternalId());
         if (null != loanBO.getFund()) {
             loanActionForm.setLoanOfferingFund(loanBO.getFund().getFundId().toString());
         }
         if (configService.isRepaymentIndepOfMeetingEnabled()) {
             MeetingDetailsEntity meetingDetail = loanBO.getLoanMeeting().getMeetingDetails();
             loanActionForm.setMonthDay("");
             loanActionForm.setMonthWeek("0");
             loanActionForm.setMonthRank("0");
 
             if (meetingDetail.getRecurrenceTypeEnum() == RecurrenceType.MONTHLY) {
                 setMonthlySchedule(loanActionForm, meetingDetail);
             } else {
                 setWeeklySchedule(loanActionForm, meetingDetail);
             }
         }
         SessionUtils.setAttribute(LOANOFFERING, loanOffering, request);
         loadUpdateMasterData(request);
 
         SessionUtils.setAttribute(RECURRENCEID, loanBO.getLoanMeeting().getMeetingDetails().getRecurrenceTypeEnum()
                 .getValue(), request);
         SessionUtils.setAttribute(RECURRENCENAME, loanBO.getLoanMeeting().getMeetingDetails().getRecurrenceType()
                 .getRecurrenceName(), request);
         SessionUtils.setCollectionAttribute(LOANFUNDS, getFunds(loanOffering), request);
         setRequestAttributesForEditPage(request, loanBO);
 
         setFormAttributes(loanBO, form, request);
         return mapping.findForward(ActionForwards.manage_success.toString());
     }
 
     private LoanBO getLoanBO(final HttpServletRequest request) throws PageExpiredException, ServiceException {
         LoanBO loanBOInSession = (LoanBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
         return loanBusinessService.getAccount(loanBOInSession.getAccountId());
     }
 
     private void setRequestAttributesForEditPage(final HttpServletRequest request, final LoanBO loanBO)
             throws ApplicationException {
         request.setAttribute("accountState", loanBO.getState());
         request.setAttribute(MasterConstants.COLLATERAL_TYPES, new MasterPersistence().getLookUpEntity(
                 MasterConstants.COLLATERAL_TYPES, getUserContext(request).getLocaleId()).getCustomValueListElements());
         request.setAttribute("collateralTypeId", loanBO.getCollateralTypeId());
     }
 
     private CustomerBO getCustomerFromRequest(final HttpServletRequest request) throws ServiceException {
         String customerId = request.getParameter(CUSTOMER_ID);
         if (isNotEmpty(customerId)) {
             return getCustomer(Integer.valueOf(customerId));
         }
         return null;
     }
 
     private void populateGlimAttributes(final HttpServletRequest request, final LoanAccountActionForm loanActionForm,
                                         final String globalAccountNum, final CustomerBO customer) throws PageExpiredException, ServiceException {
         GlimSessionAttributes glimSessionAttributes = getGlimSpecificPropertiesToSet(loanActionForm, globalAccountNum,
                 customer, getBusinessActivitiesFromDatabase(request));
         glimSessionAttributes.putIntoSession(request);
     }
 
     GlimSessionAttributes getGlimSpecificPropertiesToSet(final LoanAccountActionForm loanActionForm,
                                                          final String globalAccountNum, final CustomerBO customer, final List<ValueListElement> businessActivities)
             throws ServiceException {
         if (configService.isGlimEnabled() && customer.isGroup()) {
             List<LoanBO> individualLoans = loanBusinessService.getAllChildrenForParentGlobalAccountNum(globalAccountNum);
 
             List<ClientBO> activeClientsUnderGroup = this.customerDao.findActiveClientsUnderGroup(customer);
             List<LoanAccountDetailsDto> clientDetails = populateClientDetailsFromLoan(activeClientsUnderGroup, individualLoans, businessActivities);
             loanActionForm.setClientDetails(clientDetails);
             loanActionForm.setClients(fetchClientIdsWithMatchingLoans(individualLoans, clientDetails));
 
             return new GlimSessionAttributes(LoanConstants.GLIM_ENABLED_VALUE, activeClientsUnderGroup, LoanConstants.LOAN_ACCOUNT_OWNER_IS_GROUP_YES);
         }
 
         return new GlimSessionAttributes(LoanConstants.GLIM_DISABLED_VALUE);
     }
 
     private List<String> fetchClientIdsWithMatchingLoans(final List<LoanBO> individualLoans,
                                                          final List<LoanAccountDetailsDto> clientDetails) {
         List<String> clientIds = new ArrayList<String>();
         for (final LoanAccountDetailsDto clientDetail : clientDetails) {
             LoanBO loanMatchingClientDetail = (LoanBO) CollectionUtils.find(individualLoans, new Predicate() {
                 public boolean evaluate(final Object object) {
                     return ((LoanBO) object).getCustomer().getCustomerId().toString()
                             .equals(clientDetail.getClientId());
                 }
             });
             if (loanMatchingClientDetail != null) {
                 clientIds.add(clientDetail.getClientId());
             } else {
                 clientIds.add("");
             }
         }
         return clientIds;
     }
 
     List<LoanAccountDetailsDto> populateClientDetailsFromLoan(final List<ClientBO> activeClientsUnderGroup,
                                                               final List<LoanBO> individualLoans, final List<ValueListElement> businessActivities) {
         List<LoanAccountDetailsDto> clientDetails = new ArrayList<LoanAccountDetailsDto>();
         for (final ClientBO client : activeClientsUnderGroup) {
             LoanAccountDetailsDto clientDetail = new LoanAccountDetailsDto();
             clientDetail.setClientId(getStringValue(client.getCustomerId()));
             clientDetail.setClientName(client.getDisplayName());
             LoanBO loanAccount = (LoanBO) CollectionUtils.find(individualLoans, new Predicate() {
                 public boolean evaluate(final Object object) {
                     return client.getCustomerId().equals(((LoanBO) object).getCustomer().getCustomerId());
                 }
 
             });
             if (loanAccount != null) {
                 final Integer businessActivityId = loanAccount.getBusinessActivityId();
                 if (businessActivityId != null) {
                     clientDetail.setBusinessActivity(Integer.toString(businessActivityId));
 
                     ValueListElement businessActivityElement = (ValueListElement) CollectionUtils.find(
                             businessActivities, new Predicate() {
 
                                 public boolean evaluate(final Object object) {
                                     return ((ValueListElement) object).getId().equals(businessActivityId);
                                 }
 
                             });
                     if (businessActivityElement != null) {
                         clientDetail.setBusinessActivityName(businessActivityElement.getName());
                     }
                 }
 
                 clientDetail.setLoanAmount(loanAccount.getLoanAmount() != null ? loanAccount.getLoanAmount().toString()
                         : "0.0");
             }
             clientDetails.add(clientDetail);
         }
         return clientDetails;
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward managePrevious(final ActionMapping mapping, @SuppressWarnings("unused") final ActionForm form,
                                         final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response)
             throws Exception {
         setRequestAttributesForEditPage(request, getLoanBO(request));
         return mapping.findForward(ActionForwards.manageprevious_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward managePreview(final ActionMapping mapping, final ActionForm form,
                                        final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response)
             throws Exception {
         LoanAccountActionForm loanAccountForm = (LoanAccountActionForm) form;
         Short localeId = getUserContext(request).getLocaleId();
         if (isGlimEnabled()) {
             performGlimSpecificOnManagePreview(request, loanAccountForm, localeId);
         }
         if (null != getFund(loanAccountForm)) {
             request.setAttribute("sourceOfFunds", getFund(loanAccountForm).getFundName());
         }
 
         resetBusinessActivity(request, localeId, (LoanAccountActionForm) form);
         return mapping.findForward(ActionForwards.managepreview_success.toString());
     }
 
     private FundBO getFund(LoanAccountActionForm loanAccountActionForm) {
         FundBO fund = null;
         if (!StringUtils.isBlank(loanAccountActionForm.getLoanOfferingFund())) {
             Short fundId = loanAccountActionForm.getLoanOfferingFundValue();
             if (fundId != 0) {
                 fund = this.fundDao.findById(fundId);
             }
         }
         return fund;
     }
 
     private void performGlimSpecificOnManagePreview(final HttpServletRequest request,
                                                     final LoanAccountActionForm loanAccountForm, final Short localeId) throws ServiceException,
             PageExpiredException {
         CustomerBO customer = getCustomer(loanAccountForm.getCustomerIdValue());
         setGlimEnabledSessionAttributes(request, customer.isGroup());
         if (customer.isGroup()) {
             List<LoanAccountDetailsDto> loanAccountDetailsView = populateDetailsForSelectedClients(localeId,
                     loanAccountForm.getClientDetails(), loanAccountForm.getClients());
             SessionUtils.setCollectionAttribute("loanAccountDetailsView", loanAccountDetailsView, request);
         }
     }
 
     private List<LoanAccountDetailsDto> populateDetailsForSelectedClients(final Short localeId,
                                                                           final List<LoanAccountDetailsDto> clientDetails, final List<String> selectedClients)
             throws ServiceException {
         List<LoanAccountDetailsDto> loanAccountDetailsView = new ArrayList<LoanAccountDetailsDto>();
         for (final String clientId : selectedClients) {
             if (StringUtils.isNotEmpty(clientId)) {
                 LoanAccountDetailsDto matchingClientDetail = (LoanAccountDetailsDto) CollectionUtils.find(
                         clientDetails, new Predicate() {
                             public boolean evaluate(final Object object) {
                                 return ((LoanAccountDetailsDto) object).getClientId().equals(clientId);
                             }
                         });
 
                 if (matchingClientDetail != null) {
                     setGovernmentIdAndPurpose(matchingClientDetail, localeId);
                     loanAccountDetailsView.add(matchingClientDetail);
                 }
             }
         }
         return loanAccountDetailsView;
     }
 
     private void setGovernmentIdAndPurpose(final LoanAccountDetailsDto clientDetail, final Short localeId)
             throws ServiceException {
         clientDetail.setBusinessActivityName(findBusinessActivityName(clientDetail.getBusinessActivity(), localeId));
         clientDetail.setGovermentId(findGovernmentId(getIntegerValue(clientDetail.getClientId())));
     }
 
     private void resetBusinessActivity(final HttpServletRequest request, final Short localeId,
                                        final LoanAccountActionForm loanAccountActionForm) throws PageExpiredException, Exception {
         SessionUtils.removeAttribute(MasterConstants.BUSINESS_ACTIVITIE_NAME, request);
         if (loanAccountActionForm.getBusinessActivityIdValue() != null) {
             SessionUtils.setAttribute(MasterConstants.BUSINESS_ACTIVITIE_NAME, getNameForBusinessActivityEntity(
                     loanAccountActionForm.getBusinessActivityIdValue(), localeId), request);
         }
     }
 
     private String findGovernmentId(final Integer clientId) throws ServiceException {
         ClientBO client = clientBusinessService.getClient(clientId);
         String governmentId = client.getGovernmentId();
         return StringUtils.isBlank(governmentId) ? "-" : governmentId;
     }
 
     private String findBusinessActivityName(final String businessActivity, final Short localeId)
             throws ServiceException {
         List<ValueListElement> businessActEntity = masterDataService.retrieveMasterEntities(
                 MasterConstants.LOAN_PURPOSES, localeId);
         for (ValueListElement busact : businessActEntity) {
 
             if (busact.getId().toString().equals(businessActivity)) {
                 return busact.getName();
             }
         }
         return null;
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward cancel(final ActionMapping mapping, @SuppressWarnings("unused") final ActionForm form,
                                 @SuppressWarnings("unused") final HttpServletRequest request,
                                 @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.loan_detail_page.toString());
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward update(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
                                 @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
 
         LoanBO loanBOInSession = (LoanBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
         LoanBO loanBO = loanBusinessService.findBySystemId(loanBOInSession.getGlobalAccountNum());
         checkVersionMismatch(loanBOInSession.getVersionNo(), loanBO.getVersionNo());
         loanBO.setVersionNo(loanBOInSession.getVersionNo());
         loanBO.setUserContext(getUserContext(request));
         setInitialObjectForAuditLogging(loanBO);
 
         LoanAccountActionForm loanAccountActionForm = (LoanAccountActionForm) form;
 
         CustomerBO customer = loanBO.getCustomer();
         MeetingBO newMeetingForRepaymentDay = null;
         boolean isRepaymentIndepOfMeetingEnabled = configService.isRepaymentIndepOfMeetingEnabled();
 
         if (isRepaymentIndepOfMeetingEnabled) {
             newMeetingForRepaymentDay = this.createNewMeetingForRepaymentDay(request, loanAccountActionForm, customer);
         }
 
         loanBO.setExternalId(loanAccountActionForm.getExternalId());
         loanBO.updateLoan(loanAccountActionForm.isInterestDedAtDisbValue(), new Money(loanBO.getCurrency(),
                 loanAccountActionForm.getLoanAmount()), loanAccountActionForm.getInterestDoubleValue(),
                 loanAccountActionForm.getNoOfInstallmentsValue(), loanAccountActionForm
                         .getDisbursementDateValue(getUserContext(request).getPreferredLocale()), loanAccountActionForm
                         .getGracePeriodDurationValue(), loanAccountActionForm.getBusinessActivityIdValue(),
                 loanAccountActionForm.getCollateralNote(), loanAccountActionForm.getCollateralTypeIdValue(),
                 loanAccountActionForm.getCustomFields(), isRepaymentIndepOfMeetingEnabled, newMeetingForRepaymentDay,
                 getFund(loanAccountActionForm));
 
         if (configService.isGlimEnabled() && customer.isGroup()) {
             List<LoanAccountDetailsDto> loanAccountDetailsList = getLoanAccountDetailsFromSession(request);
             List<LoanBO> individualLoans = loanBusinessService.findIndividualLoans(Integer.valueOf(
                     loanBO.getAccountId()).toString());
             handleIndividualLoans(loanBO, loanAccountActionForm, isRepaymentIndepOfMeetingEnabled,
                     loanAccountDetailsList, individualLoans);
             request.setAttribute(CUSTOMER_ID, loanBO.getCustomer().getCustomerId().toString());
         }
 
         loanBOInSession = null;
         SessionUtils.removeAttribute(Constants.BUSINESS_KEY, request);
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, loanBO, request);
 
         return mapping.findForward(ActionForwards.update_success.toString());
     }
 
     void handleIndividualLoans(final LoanBO loanBO, final LoanAccountActionForm loanAccountActionForm,
                                final boolean isRepaymentIndepOfMeetingEnabled, final List<LoanAccountDetailsDto> loanAccountDetailsList,
                                final List<LoanBO> individualLoans) throws AccountException, ServiceException {
         List<Integer> foundLoans = new ArrayList<Integer>();
         for (final LoanAccountDetailsDto loanAccountDetail : loanAccountDetailsList) {
             Predicate predicate = new Predicate() {
 
                 public boolean evaluate(final Object object) {
                     return ((LoanBO) object).getCustomer().getCustomerId().toString().equals(
                             loanAccountDetail.getClientId());
                 }
 
             };
             LoanBO individualLoan = (LoanBO) CollectionUtils.find(individualLoans, predicate);
             if (individualLoan == null) {
                 glimLoanUpdater.createIndividualLoan(loanAccountActionForm, loanBO, isRepaymentIndepOfMeetingEnabled,
                         loanAccountDetail);
             } else {
                 foundLoans.add(individualLoan.getAccountId());
                 glimLoanUpdater.updateIndividualLoan(loanAccountDetail, individualLoan);
             }
         }
         for (LoanBO loan : individualLoans) {
             if (!foundLoans.contains(loan.getAccountId())) {
                 glimLoanUpdater.delete(loan);
             }
         }
     }
 
     /**
      * Create new meeting for the repayment day.
      * <p/>
      * Depending on the recurrence id (WEEKLY or MONTHLY) a MeetingBO will be created and returned
      *
      * @throws InvalidDateException
      */
     private MeetingBO createNewMeetingForRepaymentDay(final HttpServletRequest request,
                                                       final LoanAccountActionForm loanAccountActionForm, final CustomerBO customer) throws MeetingException,
             InvalidDateException {
         MeetingBO newMeetingForRepaymentDay = null;
         Short recurrenceId = Short.valueOf(loanAccountActionForm.getRecurrenceId());
         final Date repaymentStartDate = this.resolveRepaymentStartDate(loanAccountActionForm
                 .getDisbursementDateValue(getUserContext(request).getPreferredLocale()));
         if (RecurrenceType.WEEKLY.getValue().equals(recurrenceId)) {
             newMeetingForRepaymentDay = new MeetingBO(WeekDay.getWeekDay(Short.valueOf(loanAccountActionForm
                     .getWeekDay())), Short.valueOf(loanAccountActionForm.getRecurWeek()), repaymentStartDate,
                     MeetingType.LOAN_INSTALLMENT, customer.getCustomerMeeting().getMeeting().getMeetingPlace());
         } else if (RecurrenceType.MONTHLY.getValue().equals(recurrenceId)) {
             if (loanAccountActionForm.getMonthType().equals("1")) {
                 newMeetingForRepaymentDay = new MeetingBO(Short.valueOf(loanAccountActionForm.getMonthDay()), Short
                         .valueOf(loanAccountActionForm.getDayRecurMonth()), repaymentStartDate,
                         MeetingType.LOAN_INSTALLMENT, customer.getCustomerMeeting().getMeeting().getMeetingPlace());
             } else {
                 newMeetingForRepaymentDay = new MeetingBO(Short.valueOf(loanAccountActionForm.getMonthWeek()), Short
                         .valueOf(loanAccountActionForm.getRecurMonth()), repaymentStartDate,
                         MeetingType.LOAN_INSTALLMENT, customer.getCustomerMeeting().getMeeting().getMeetingPlace(),
                         Short.valueOf(loanAccountActionForm.getMonthRank()));
             }
         }
         return newMeetingForRepaymentDay;
     }
 
     private LoanOfferingBO getLoanOffering(Short loanOfferingId, short localeId) throws Exception {
         return loanPrdBusinessService.getLoanOffering(loanOfferingId, localeId);
     }
 
     private void loadMasterData(final HttpServletRequest request) throws Exception {
         // Retrieve and set into the session all collateral types from the
         // lookup_value_locale table associated with the current user context
         // locale
         SessionUtils.setCollectionAttribute(MasterConstants.COLLATERAL_TYPES, new MasterPersistence().getLookUpEntity(
                 MasterConstants.COLLATERAL_TYPES, getUserContext(request).getLocaleId()).getCustomValueListElements(),
                 request);
 
         setBusinessActivitiesIntoSession(request);
     }
 
     private String getNameForBusinessActivityEntity(final Integer entityId, final Short localeId) throws Exception {
         if (entityId != null) {
             return masterDataService.retrieveMasterEntities(entityId, localeId);
         }
         return "";
     }
 
     @SuppressWarnings("unchecked")
     private FundBO getFund(final HttpServletRequest request, final Short fundId) throws PageExpiredException {
         List<FundBO> funds = (List<FundBO>) SessionUtils.getAttribute(LOANFUNDS, request);
         for (FundBO fund : funds) {
             if (fund.getFundId().equals(fundId)) {
                 return fund;
             }
         }
         return null;
     }
 
     private void setFormAttributes(final LoanBO loan, final ActionForm form, final HttpServletRequest request)
             throws Exception {
         LoanAccountActionForm loanAccountActionForm = (LoanAccountActionForm) form;
         loanAccountActionForm.setStateSelected(getStringValue(loan.getAccountState().getId()));
         loanAccountActionForm.setLoanAmount(getStringValue(loan.getLoanAmount()));
 
         java.util.Date proposedDisbursement = (Date) SessionUtils.getAttribute(PROPOSED_DISBURSAL_DATE, request);
         loanAccountActionForm.setDisbursementDate(DateUtils.getUserLocaleDate(getUserContext(request)
                 .getPreferredLocale(), DateUtils.toDatabaseFormat(proposedDisbursement)));
 
         loanAccountActionForm.setIntDedDisbursement(loan.isInterestDeductedAtDisbursement() ? "1" : "0");
         loanAccountActionForm.setBusinessActivityId(getStringValue(loan.getBusinessActivityId()));
         if (loan.getCollateralTypeId() != null) {
             loanAccountActionForm.setCollateralTypeId(getStringValue(loan.getCollateralTypeId()));
         }
         loanAccountActionForm.setCollateralNote(loan.getCollateralNote());
         loanAccountActionForm.setInterestRate(getDoubleStringForInterest(loan.getInterestRate()));
         loanAccountActionForm.setNoOfInstallments(getStringValue(loan.getNoOfInstallments()));
         loanAccountActionForm.setGracePeriodDuration(getStringValue(loan.getGracePeriodDuration()));
         loanAccountActionForm.setCustomFields(createCustomFieldViews(loan.getAccountCustomFields(), request));
 
         loanAccountActionForm.setOriginalDisbursementDate(new java.sql.Date(loan.getDisbursementDate().getTime()));
     }
 
     private void loadCustomFieldDefinitions(final HttpServletRequest request) throws Exception {
         SessionUtils.setCollectionAttribute(CUSTOM_FIELDS, getAccountBusinessService().retrieveCustomFieldsDefinition(
                 EntityType.LOAN), request);
     }
 
     @SuppressWarnings("unchecked")
     private List<CustomFieldDto> createCustomFieldViews(final Set<AccountCustomFieldEntity> customFieldEntities,
                                                         final HttpServletRequest request) throws ApplicationException {
         List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();
 
         List<CustomFieldDefinitionEntity> customFieldDefs = (List<CustomFieldDefinitionEntity>) SessionUtils
                 .getAttribute(CUSTOM_FIELDS, request);
         Locale locale = getUserContext(request).getPreferredLocale();
         for (CustomFieldDefinitionEntity customFieldDef : customFieldDefs) {
             for (AccountCustomFieldEntity customFieldEntity : customFieldEntities) {
                 if (customFieldDef.getFieldId().equals(customFieldEntity.getFieldId())) {
                     if (customFieldDef.getFieldType().equals(CustomFieldType.DATE.getValue())) {
                         customFields.add(new CustomFieldDto(customFieldEntity.getFieldId(), DateUtils
                                 .getUserLocaleDate(locale, customFieldEntity.getFieldValue()), customFieldDef
                                 .getFieldType()));
                     } else {
                         customFields.add(new CustomFieldDto(customFieldEntity.getFieldId(), customFieldEntity
                                 .getFieldValue(), customFieldDef.getFieldType()));
                     }
                 }
             }
         }
         return customFields;
     }
 
     private void loadUpdateMasterData(final HttpServletRequest request) throws Exception {
         loadMasterData(request);
         loadCustomFieldDefinitions(request);
     }
 
     static class GlimSessionAttributes {
 
         private final Integer isGlimEnabled;
         private final List<ClientBO> clients;
         private final String loanAccountOwnerIsGroup;
 
         GlimSessionAttributes(final int isGlimEnabled, final List<ClientBO> clients,
                               final String loanAccountOwnerIsGroup) {
             this.isGlimEnabled = isGlimEnabled;
             this.clients = clients;
             this.loanAccountOwnerIsGroup = loanAccountOwnerIsGroup;
         }
 
         @SuppressWarnings("unchecked")
         GlimSessionAttributes(final int isGlimEnabled) {
             this(isGlimEnabled, Collections.EMPTY_LIST, "");
         }
 
         void putIntoSession(final HttpServletRequest request) throws PageExpiredException {
             SessionUtils.setAttribute(LOAN_INDIVIDUAL_MONITORING_IS_ENABLED, isGlimEnabled, request);
             SessionUtils.setCollectionAttribute(CLIENT_LIST, clients, request);
             SessionUtils.setAttribute(LOAN_ACCOUNT_OWNER_IS_A_GROUP, loanAccountOwnerIsGroup, request);
         }
 
         @Override
         public int hashCode() {
             final int PRIME = 31;
             int result = 1;
             result = PRIME * result + (clients == null ? 0 : clients.hashCode());
             result = PRIME * result + (isGlimEnabled == null ? 0 : isGlimEnabled.hashCode());
             result = PRIME * result + (loanAccountOwnerIsGroup == null ? 0 : loanAccountOwnerIsGroup.hashCode());
             return result;
         }
 
         @Override
         public boolean equals(final Object obj) {
             if (this == obj) {
                 return true;
             }
             if (obj == null) {
                 return false;
             }
             if (getClass() != obj.getClass()) {
                 return false;
             }
             final GlimSessionAttributes other = (GlimSessionAttributes) obj;
             if (clients == null) {
                 if (other.clients != null) {
                     return false;
                 }
             } else if (!clients.equals(other.clients)) {
                 return false;
             }
             if (isGlimEnabled == null) {
                 if (other.isGlimEnabled != null) {
                     return false;
                 }
             } else if (!isGlimEnabled.equals(other.isGlimEnabled)) {
                 return false;
             }
             if (loanAccountOwnerIsGroup == null) {
                 if (other.loanAccountOwnerIsGroup != null) {
                     return false;
                 }
             } else if (!loanAccountOwnerIsGroup.equals(other.loanAccountOwnerIsGroup)) {
                 return false;
             }
             return true;
         }
 
         @Override
         public String toString() {
             return "isGlimEnabled:" + isGlimEnabled + " loanAccountOwnerIsAGroup:" + loanAccountOwnerIsGroup
                     + " clients:" + clients;
         }
 
     }
 
     private void storeObjectOnSessionForUseInJspPage(final HttpServletRequest request, final String objectKey,
                                                      final Serializable value) throws PageExpiredException {
         SessionUtils.setAttribute(objectKey, value, request);
     }
 
     private void storeCollectionOnSessionForUseInJspPage(final HttpServletRequest request, final String collectionKey,
                                                          final Collection<? extends Serializable> collectionValue) throws PageExpiredException {
         SessionUtils.setCollectionAttribute(collectionKey, collectionValue, request);
     }
 
     private void storeRedoLoanSettingOnRequestForUseInJspIfPerspectiveParamaterOnQueryString(
             final HttpServletRequest request) {
         if (request.getParameter(PERSPECTIVE) != null) {
             request.setAttribute(PERSPECTIVE, request.getParameter(PERSPECTIVE));
         }
     }
 
     private boolean isGlimEnabled() {
         return new ConfigurationBusinessService().isGlimEnabled();
     }
 
     private void setGlimEnabledSessionAttributes(final HttpServletRequest request, final boolean isGroup)
             throws PageExpiredException {
         storeObjectOnSessionForUseInJspPage(request, LoanConstants.LOAN_INDIVIDUAL_MONITORING_IS_ENABLED,
                 LoanConstants.GLIM_ENABLED_VALUE);
         if (isGroup) {
             storeObjectOnSessionForUseInJspPage(request, LoanConstants.LOAN_ACCOUNT_OWNER_IS_A_GROUP,
                     LoanConstants.LOAN_ACCOUNT_OWNER_IS_GROUP_YES);
         }
     }
 
     private void handleRepaymentsIndependentOfMeetingIfConfigured(final HttpServletRequest request,
                                                                   final LoanAccountActionForm loanActionForm, final String recurMonth) throws PageExpiredException, Exception {
 
         if (configService.isRepaymentIndepOfMeetingEnabled()) {
 
             storeObjectOnSessionForUseInJspPage(request, LoanConstants.REPAYMENT_SCHEDULES_INDEPENDENT_OF_MEETING_IS_ENABLED, Integer.valueOf(1));
             storeObjectOnSessionForUseInJspPage(request, LoanConstants.LOANACCOUNTOWNERISACLIENT, LoanConstants.LOAN_ACCOUNT_OWNER_IS_GROUP_YES);
 
             storeCollectionOnSessionForUseInJspPage(request, MeetingConstants.WEEKDAYSLIST, new FiscalCalendarRules().getWorkingDays());
             storeCollectionOnSessionForUseInJspPage(request, MeetingConstants.WEEKRANKLIST, RankOfDay.getRankOfDayList());
 
             loanActionForm.setRecurMonth(recurMonth);
         }
     }
 
     /**
      * @deprecated {@link LoanProductDao#findAllLoanPurposes()}
      */
     @Deprecated
     private void setBusinessActivitiesIntoSession(final HttpServletRequest request) throws PageExpiredException,
             ServiceException {
         SessionUtils.setCollectionAttribute(MasterConstants.BUSINESS_ACTIVITIES,
                 getBusinessActivitiesFromDatabase(request), request);
     }
 
     /**
      * use method from getPrdOfferings
      */
     @Deprecated
     private List<ValueListElement> getBusinessActivitiesFromDatabase(final HttpServletRequest request)
             throws ServiceException {
         return masterDataService.retrieveMasterEntities(MasterConstants.LOAN_PURPOSES, getUserContext(request)
                 .getLocaleId());
     }
 
     @TransactionDemarcate(joinToken = true)
     @Override
     public ActionForward captureQuestionResponses(
             final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "captureQuestionResponses");
         ActionErrors errors = createLoanQuestionnaire.validateResponses(request, (LoanAccountActionForm) form);
         if (errors != null && !errors.isEmpty()) {
             addErrors(request, errors);
             return mapping.findForward(ActionForwards.captureQuestionResponses.toString());
         }
         ActionForward join = createLoanQuestionnaire.rejoinFlow(mapping);
         return join;
     }
 
     @TransactionDemarcate(joinToken = true)
     @Override
     public ActionForward editQuestionResponses(
             final ActionMapping mapping, final ActionForm form,
             final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "editQuestionResponses");
         return createLoanQuestionnaire.editResponses(mapping, request, (LoanAccountActionForm) form);
     }
 
     private ActionErrors getActionErrors(Errors errors) {
         ActionErrors actionErrors = new ActionErrors();
         if (errors.hasErrors()) {
             for (ErrorEntry errorEntry : errors.getErrorEntries()) {
                 ActionMessage actionMessage;
                 if (errorEntry.hasErrorArgs()) {
                     actionMessage = new ActionMessage(errorEntry.getErrorCode(), errorEntry.getArgs().toArray());
                 } else {
                     actionMessage = new ActionMessage(errorEntry.getErrorCode(), errorEntry.getFieldName());
                 }
                 actionErrors.add(errorEntry.getErrorCode(), actionMessage);
             }
         }
         return actionErrors;
     }
 
 }
