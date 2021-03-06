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
 
 import static org.mifos.framework.util.helpers.DateUtils.getUserLocaleDate;
 
 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.joda.time.LocalDate;
 import org.mifos.accounts.acceptedpaymenttype.business.service.AcceptedPaymentTypeService;
 import org.mifos.accounts.acceptedpaymenttype.persistence.AcceptedPaymentTypePersistence;
 import org.mifos.accounts.api.AccountPaymentParametersDto;
 import org.mifos.accounts.api.AccountReferenceDto;
 import org.mifos.accounts.api.AccountService;
 import org.mifos.accounts.api.StandardAccountService;
 import org.mifos.accounts.api.UserReferenceDto;
 import org.mifos.accounts.exceptions.AccountException;
 import org.mifos.accounts.loan.persistance.LoanPersistence;
 import org.mifos.accounts.loan.struts.actionforms.LoanDisbursementActionForm;
 import org.mifos.accounts.loan.util.helpers.LoanConstants;
 import org.mifos.accounts.loan.util.helpers.LoanDisbursalDto;
 import org.mifos.accounts.persistence.AccountPersistence;
 import org.mifos.accounts.savings.persistence.GenericDaoHibernate;
 import org.mifos.application.master.business.PaymentTypeEntity;
 import org.mifos.application.master.util.helpers.MasterConstants;
 import org.mifos.application.master.util.helpers.PaymentTypes;
 import org.mifos.config.AccountingRules;
 import org.mifos.config.AccountingRulesConstants;
 import org.mifos.config.persistence.ConfigurationPersistence;
 import org.mifos.core.MifosRuntimeException;
 import org.mifos.customers.personnel.persistence.PersonnelDaoHibernate;
 import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.logger.MifosLogger;
 import org.mifos.framework.exceptions.PageExpiredException;
 import org.mifos.framework.exceptions.ServiceException;
 import org.mifos.framework.struts.action.BaseAction;
 import org.mifos.framework.util.helpers.CloseSession;
 import org.mifos.framework.util.helpers.Constants;
 import org.mifos.framework.util.helpers.DateUtils;
 import org.mifos.framework.util.helpers.SessionUtils;
 import org.mifos.framework.util.helpers.TransactionDemarcate;
 import org.mifos.security.util.ActionSecurity;
 import org.mifos.security.util.SecurityConstants;
 import org.mifos.security.util.UserContext;
 
 public class LoanDisbursementAction extends BaseAction {
    private static final MifosLogger logger = MifosLogManager.getLogger(LoggerConstants.ACCOUNTSLOGGER);
 
     private AccountService accountService = null;
 
     public AccountService getAccountService() {
         return accountService;
     }
 
     public LoanDisbursementAction() {
 
         accountService = new StandardAccountService(new AccountPersistence(), new LoanPersistence(),
                 new AcceptedPaymentTypePersistence(), new PersonnelDaoHibernate(new GenericDaoHibernate()));
     }
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("loanDisbursementAction");
         security.allow("load", SecurityConstants.LOAN_CAN_DISBURSE_LOAN);
         security.allow("preview", SecurityConstants.VIEW);
         security.allow("previous", SecurityConstants.VIEW);
         security.allow("update", SecurityConstants.VIEW);
         return security;
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward load(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             final HttpServletResponse response) throws Exception {
 
         setIsRepaymentScheduleEnabled(request);
         setIsBackdatedTransactionAllowed(request);
 
         LoanDisbursementActionForm loanDisbursementActionForm = (LoanDisbursementActionForm) form;
         loanDisbursementActionForm.clear();
         loanDisbursementActionForm.setAmountCannotBeZero(false);
 
         Integer loanAccountId = Integer.valueOf(loanDisbursementActionForm.getAccountId());
         loanServiceFacade.checkIfProductsOfferingCanCoexist(loanAccountId);
 
         LoanDisbursalDto loanDisbursalDto = loanServiceFacade.getLoanDisbursalDto(loanAccountId);
 
         SessionUtils.setAttribute(LoanConstants.PROPOSED_DISBURSAL_DATE, loanDisbursalDto.getProposedDate(), request);
         loanDisbursementActionForm.setTransactionDate(getUserLocaleDate(getUserContext(request).getPreferredLocale(),
                 loanDisbursalDto.getProposedDate()));
 
         UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
         SessionUtils.setCollectionAttribute(MasterConstants.PAYMENT_TYPE, getAcceptedPaymentTypes(uc.getLocaleId()),
                 request);
         loanDisbursementActionForm.setAmount(loanDisbursalDto.getAmountPaidAtDisbursement().toString());
         loanDisbursementActionForm.setLoanAmount(loanDisbursalDto.getLoanAmount().toString());
         if (AccountingRules.isMultiCurrencyEnabled()) {
             loanDisbursementActionForm.setCurrencyId(loanDisbursalDto.getLoanAmount().getCurrency().getCurrencyId());
         }
 
         return mapping.findForward(Constants.LOAD_SUCCESS);
     }
 
     private static List<PaymentTypeEntity> getAcceptedPaymentTypes(final Short localeId) throws Exception {
         return AcceptedPaymentTypeService.getAcceptedPaymentTypes(localeId);
     }
 
     private void setIsBackdatedTransactionAllowed(final HttpServletRequest request) throws PageExpiredException {
         SessionUtils.setAttribute(AccountingRulesConstants.BACKDATED_TRANSACTIONS_ALLOWED, AccountingRules
                 .isBackDatedTxnAllowed(), request);
     }
 
     private void setIsRepaymentScheduleEnabled(final HttpServletRequest request) throws PageExpiredException {
         SessionUtils.setAttribute(LoanConstants.REPAYMENT_SCHEDULES_INDEPENDENT_OF_MEETING_IS_ENABLED,
                 new ConfigurationPersistence().isRepaymentIndepOfMeetingEnabled() ? 1 : 0, request);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward preview(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             final HttpServletResponse response) throws Exception {
 
         return mapping.findForward(Constants.PREVIEW_SUCCESS);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previous(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             final HttpServletResponse response) throws Exception {
         return mapping.findForward(Constants.PREVIOUS_SUCCESS);
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     @CloseSession
     public ActionForward update(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             final HttpServletResponse response) throws Exception {
 
         LoanDisbursementActionForm actionForm = (LoanDisbursementActionForm) form;
 
         UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
         Date trxnDate = getDateFromString(actionForm.getTransactionDate(), uc.getPreferredLocale());
         trxnDate = DateUtils.getDateWithoutTimeStamp(trxnDate.getTime());
         Date receiptDate = getDateFromString(actionForm.getReceiptDate(), uc.getPreferredLocale());
 
         Integer loanAccountId = Integer.valueOf(actionForm.getAccountId());
         if (!loanServiceFacade.isTrxnDateValid(loanAccountId, trxnDate)) {
             throw new AccountException("errors.invalidTxndate");
         }
 
         String modeOfPayment = actionForm.getPaymentModeOfPayment();
         Short modeOfPaymentId = StringUtils.isEmpty(modeOfPayment) ? PaymentTypes.CASH.getValue() : Short
                 .valueOf(modeOfPayment);
         try {
             final List<AccountPaymentParametersDto> payment = new ArrayList<AccountPaymentParametersDto>();
            final org.mifos.accounts.api.PaymentTypeDto paymentType = getLoanDisbursementTypeDtoForId(Short
                     .valueOf(modeOfPaymentId));
             final String comment = "";
             final BigDecimal disbursalAmount = new BigDecimal(actionForm.getLoanAmount());
             payment.add(new AccountPaymentParametersDto(new UserReferenceDto(uc.getId()), new AccountReferenceDto(
                     loanAccountId), disbursalAmount, new LocalDate(trxnDate), paymentType, comment,
                     new LocalDate(receiptDate), actionForm.getReceiptId()));
             getAccountService().disburseLoans(payment);
 
         } catch (Exception e) {
             if (e.getMessage().startsWith("errors.")) {
                 throw e;
             }
            String msg = "errors.cannotDisburseLoan.because.disburseFailed";
            logger.error(msg, e);
            throw new AccountException(msg);
         }
 
         return mapping.findForward(Constants.UPDATE_SUCCESS);
     }
 
    private org.mifos.accounts.api.PaymentTypeDto getLoanDisbursementTypeDtoForId(short id) throws Exception {
        for (org.mifos.accounts.api.PaymentTypeDto paymentTypeDto : getAccountService().getLoanDisbursementTypes()) {
             if (paymentTypeDto.getValue() == id) {
                 return paymentTypeDto;
             }
         }
         throw new MifosRuntimeException("Expected loan PaymentTypeDto not found for id: " + id);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validate(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             final HttpServletResponse response) throws Exception {
         String method = (String) request.getAttribute("methodCalled");
         String forward = null;
         if (method != null) {
             forward = method + "_failure";
         }
         return mapping.findForward(forward);
     }
 
     @Override
     protected BusinessService getService() throws ServiceException {
         return null;
     }
 
     @Override
     protected boolean skipActionFormToBusinessObjectConversion(final String method) {
         return true;
     }
 
     @Override
     protected boolean isNewBizRequired(final HttpServletRequest request) throws ServiceException {
         return false;
     }
 
 }
