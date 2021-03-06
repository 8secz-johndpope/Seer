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
 
 package org.mifos.application.admin.struts.action;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Properties;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.mifos.application.master.MessageLookup;
 import org.mifos.application.master.business.MifosCurrency;
 import org.mifos.application.meeting.util.helpers.WeekDay;
 import org.mifos.application.util.helpers.ActionForwards;
 import org.mifos.application.util.helpers.YesNoFlag;
 import org.mifos.config.AccountingRules;
 import org.mifos.config.ClientRules;
 import org.mifos.config.ConfigLocale;
 import org.mifos.config.FiscalCalendarRules;
 import org.mifos.config.ProcessFlowRules;
 import org.mifos.config.business.service.ConfigurationBusinessService;
 import org.mifos.config.exceptions.ConfigurationException;
 import org.mifos.framework.business.service.BusinessService;
 import org.mifos.framework.exceptions.ServiceException;
 import org.mifos.framework.struts.action.BaseAction;
 import org.mifos.framework.util.helpers.TransactionDemarcate;
 import org.mifos.security.util.ActionSecurity;
 import org.mifos.security.util.SecurityConstants;
 
 public class ViewOrganizationSettingsAction extends BaseAction {
     /** Name of request attribute where organization settings are stored. */
     public static final String ORGANIZATION_SETTINGS = "orgSettings";
 
     private static final String DELIMITER = ", ";
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("viewOrganizationSettingsAction");
         security.allow("get", SecurityConstants.CAN_VIEW_SYSTEM_INFO);
         return security;
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward get(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         Properties orgSettings = new Properties();
 
         orgSettings.putAll(getFiscalRules());
         orgSettings.putAll(getLocaleInfo());
         orgSettings.putAll(getAccountingRules());
         orgSettings.put("currencies", getCurrencies());
         orgSettings.putAll(getClientRules());
         orgSettings.putAll(getProcessFlowRules());
         orgSettings.putAll(getMiscRules(request.getSession()));
 
         request.setAttribute(ORGANIZATION_SETTINGS, orgSettings);
 
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     private Properties getFiscalRules() {
         Properties fiscalRules = new Properties();
 
         fiscalRules.setProperty("workingDays", getWorkingDays());
        fiscalRules
                .setProperty("allowCalDefForNextYear", new FiscalCalendarRules().getDaysForCalendarDefinition().toString());
         fiscalRules.setProperty("startOfWeek", WeekDay.getWeekDay(new FiscalCalendarRules().getStartOfWeek()).getName());
         fiscalRules.setProperty("offDays", getOffDays());
         fiscalRules.setProperty("holidayMeeting", new FiscalCalendarRules().getScheduleTypeForMeetingOnHoliday());
 
         return fiscalRules;
     }
 
     private Properties getLocaleInfo() {
         ConfigLocale configLocale = new ConfigLocale();
         Properties localeInfo = new Properties();
 
         localeInfo.setProperty("localeCountryCode", configLocale.getCountryCode());
         localeInfo.setProperty("localeLanguageCode", configLocale.getLanguageCode());
 
         return localeInfo;
     }
 
     private Properties getAccountingRules() {
         Properties accountingRules = new Properties();
 
         accountingRules.setProperty("maxInterest", AccountingRules.getMaxInterest().toString());
         accountingRules.setProperty("minInterest", AccountingRules.getMinInterest().toString());
         accountingRules.setProperty("digitsBeforeDecimal", AccountingRules.getDigitsBeforeDecimal().toString());
         accountingRules.setProperty("intDigitsAfterDecimal", AccountingRules.getDigitsAfterDecimalForInterest()
                 .toString());
         accountingRules.setProperty("intDigitsBeforeDecimal", AccountingRules.getDigitsBeforeDecimalForInterest()
                 .toString());
         accountingRules.setProperty("interestDays", AccountingRules.getNumberOfInterestDays().toString());
         accountingRules.setProperty("currencyRoundingMode", AccountingRules.getCurrencyRoundingMode().toString());
         accountingRules.setProperty("initialRoundingMode", AccountingRules.getInitialRoundingMode().toString());
         accountingRules.setProperty("finalRoundingMode", AccountingRules.getFinalRoundingMode().toString());
         return accountingRules;
     }
 
     private List<Properties> getCurrencies() {
         List<Properties> currencies = new ArrayList<Properties>();
         Properties currencyRules = new Properties();
 
         for (MifosCurrency currency : AccountingRules.getCurrencies()) {
             currencyRules = new Properties();
             currencyRules.setProperty("code", currency.getCurrencyCode());
             currencyRules.setProperty("digitsAfterDecimal", AccountingRules.getDigitsAfterDecimal(currency).toString());
             currencyRules.setProperty("finalRoundOffMultiple", AccountingRules.getFinalRoundOffMultiple(currency).toString());
             currencyRules.setProperty("initialRoundOffMultiple", AccountingRules.getInitialRoundOffMultiple(currency).toString());
             currencies.add(currencyRules);
         }
 
         return currencies;
     }
 
     private Properties getClientRules() throws ConfigurationException {
         Properties clientRules = new Properties();
 
         clientRules.setProperty("centerHierarchyExists", booleanToYesNo(ClientRules.getCenterHierarchyExists()));
         clientRules.setProperty("loansForGroups", booleanToYesNo(ClientRules.getGroupCanApplyLoans()));
         clientRules.setProperty("clientsOutsideGroups", booleanToYesNo(ClientRules.getClientCanExistOutsideGroup()));
         clientRules.setProperty("nameSequence", StringUtils.join(ClientRules.getNameSequence(), DELIMITER));
         clientRules.setProperty("isAgeCheckEnabled",booleanToYesNo(ClientRules.isAgeCheckEnabled()));
         clientRules.setProperty("maximumAge", String.valueOf(ClientRules.getMaximumAgeForNewClient()));
         clientRules.setProperty("minimumAge", String.valueOf(ClientRules.getMinimumAgeForNewClient()));
         clientRules.setProperty("isFamilyDetailsRequired",booleanToYesNo(ClientRules.isFamilyDetailsRequired()));
         clientRules.setProperty("maximumNumberOfFamilyMembers",String.valueOf(ClientRules.getMaximumNumberOfFamilyMembers()));
         return clientRules;
     }
 
     private Properties getProcessFlowRules() {
         Properties processFlowRules = new Properties();
 
         processFlowRules.setProperty("clientPendingState", booleanToYesNo(ProcessFlowRules
                 .isClientPendingApprovalStateEnabled()));
         processFlowRules.setProperty("groupPendingState", booleanToYesNo(ProcessFlowRules
                 .isGroupPendingApprovalStateEnabled()));
         processFlowRules.setProperty("loanDisbursedState", booleanToYesNo(ProcessFlowRules
                 .isLoanDisbursedToLoanOfficerStateEnabled()));
         processFlowRules.setProperty("loanPendingState", booleanToYesNo(ProcessFlowRules
                 .isLoanPendingApprovalStateEnabled()));
         processFlowRules.setProperty("savingsPendingState", booleanToYesNo(ProcessFlowRules
                 .isSavingsPendingApprovalStateEnabled()));
 
         return processFlowRules;
     }
 
     private Properties getMiscRules(HttpSession httpSession) throws ServiceException {
         Properties misc = new Properties();
 
         Integer timeoutVal = httpSession.getMaxInactiveInterval() / 60;
         misc.setProperty("sessionTimeout", timeoutVal.toString());
 
            // FIXME - #00001 - keithw - Check days in advance usage in CollectionsheetHelper
 //            Integer advanceDaysVal = CollectionSheetHelper.getDaysInAdvance();
             misc.setProperty("collectionSheetAdvanceDays", "1");
 
         misc.setProperty("backDatedTransactions", booleanToYesNo(AccountingRules.isBackDatedTxnAllowed()));
         ConfigurationBusinessService cbs = new ConfigurationBusinessService();
         misc.setProperty("glim", booleanToYesNo(cbs.isGlimEnabled()));
         misc.setProperty("lsim", booleanToYesNo(cbs.isRepaymentIndepOfMeetingEnabled()));
 
         return misc;
     }
 
     private String getWorkingDays() {
         List<WeekDay> workDaysList = new FiscalCalendarRules().getWorkingDays();
         List<String> workDayNames = new ArrayList<String>();
         for (WeekDay workDay : workDaysList) {
             workDayNames.add(workDay.getName());
         }
         return StringUtils.join(workDayNames, DELIMITER);
     }
 
     private String getOffDays() {
         List<Short> offDaysList = new FiscalCalendarRules().getWeekDayOffList();
         List<String> offDayNames = new ArrayList<String>();
         for (Short offDayNum : offDaysList) {
             offDayNames.add(WeekDay.getWeekDay(offDayNum).getName());
         }
         return StringUtils.join(offDayNames, DELIMITER);
     }
 
     private String booleanToYesNo(boolean bool) {
         MessageLookup m = MessageLookup.getInstance();
         if (bool) {
             return m.lookup(YesNoFlag.YES);
         }
 
         return m.lookup(YesNoFlag.NO);
     }
 
     @Override
     protected BusinessService getService() throws ServiceException {
         return null;
     }
 
     @Override
     protected boolean skipActionFormToBusinessObjectConversion(@SuppressWarnings("unused") String method) {
         return true;
     }
 }
