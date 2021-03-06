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
 
 package org.mifos.accounts.struts.action;
 
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.mifos.accounts.acceptedpaymenttype.persistence.AcceptedPaymentTypePersistence;
 import org.mifos.accounts.api.AccountPaymentParametersDto;
 import org.mifos.accounts.api.AccountReferenceDto;
 import org.mifos.accounts.api.AccountService;
 import org.mifos.accounts.api.PaymentTypeDto;
 import org.mifos.accounts.api.StandardAccountService;
 import org.mifos.accounts.api.UserReferenceDto;
 import org.mifos.accounts.loan.business.service.LoanBusinessService;
 import org.mifos.accounts.loan.persistance.LoanPersistence;
 import org.mifos.accounts.persistence.AccountPersistence;
 import org.mifos.accounts.savings.persistence.GenericDaoHibernate;
 import org.mifos.accounts.servicefacade.AccountPaymentDto;
 import org.mifos.accounts.servicefacade.AccountServiceFacade;
 import org.mifos.accounts.servicefacade.AccountTypeDto;
 import org.mifos.accounts.servicefacade.WebTierAccountServiceFacade;
 import org.mifos.accounts.struts.actionforms.AccountApplyPaymentActionForm;
 import org.mifos.accounts.util.helpers.AccountConstants;
 import org.mifos.application.admin.servicefacade.InvalidDateException;
 import org.mifos.application.master.util.helpers.MasterConstants;
 import org.mifos.application.util.helpers.ActionForwards;
 import org.mifos.core.MifosRuntimeException;
 import org.mifos.customers.exceptions.CustomerException;
 import org.mifos.customers.persistence.CustomerDaoHibernate;
 import org.mifos.framework.business.service.BusinessService;
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
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.math.BigDecimal;
 import java.util.List;
 
 public class AccountApplyPaymentAction extends BaseAction {
     private AccountServiceFacade accountServiceFacade = new WebTierAccountServiceFacade();
     private AccountService accountService = null;
     private AccountPersistence accountPersistence = new AccountPersistence();
     private List<PaymentTypeDto> loanPaymentTypeDtos;
     private List<PaymentTypeDto> feePaymentTypeDtos;
 
     public AccountApplyPaymentAction() throws Exception {
         accountService = new StandardAccountService(accountPersistence, new LoanPersistence(), new AcceptedPaymentTypePersistence(), null, new CustomerDaoHibernate(new GenericDaoHibernate()), new LoanBusinessService());
         loanPaymentTypeDtos = accountService.getLoanPaymentTypes();
         feePaymentTypeDtos = accountService.getFeePaymentTypes();
     }
 
     public AccountService getAccountService() {
         return accountService;
     }
 
     @Override
     protected BusinessService getService() throws ServiceException {
         return null;
     }
 
     @Override
     protected boolean skipActionFormToBusinessObjectConversion(String method) {
         return true;
     }
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("applyPaymentAction");
         security.allow("load", SecurityConstants.VIEW);
         security.allow("preview", SecurityConstants.VIEW);
         security.allow("previous", SecurityConstants.VIEW);
         security.allow("applyPayment", SecurityConstants.VIEW);
         return security;
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         UserContext userContext = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
         AccountApplyPaymentActionForm actionForm = (AccountApplyPaymentActionForm) form;
         clearActionForm(actionForm);
         actionForm.setTransactionDate(DateUtils.makeDateAsSentFromBrowser());
 
         final AccountReferenceDto accountReferenceDto = new AccountReferenceDto(Integer.valueOf(actionForm.getAccountId()));
         AccountPaymentDto accountPaymentDto = accountServiceFacade.getAccountPaymentInformation(
                 Integer.valueOf(accountReferenceDto.getAccountId()), request.getParameter(Constants.INPUT), userContext.getLocaleId(),
                 new UserReferenceDto(userContext.getId()));
 
         SessionUtils.setAttribute(Constants.ACCOUNT_VERSION, accountPaymentDto.getVersion(), request);
         SessionUtils.setAttribute(Constants.ACCOUNT_TYPE, accountPaymentDto.getAccountType().name(), request);
         SessionUtils.setAttribute(Constants.ACCOUNT_ID, Integer.valueOf(actionForm.getAccountId()), request);
 
         SessionUtils.setCollectionAttribute(MasterConstants.PAYMENT_TYPE,
                 accountPaymentDto.getPaymentTypeList(), request);
 
         actionForm.setAmount(accountPaymentDto.getTotalPaymentDue().toString());
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.preview_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previous(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.previous_success.toString());
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     @CloseSession
     public ActionForward applyPayment(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         UserContext userContext = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
         AccountApplyPaymentActionForm actionForm = (AccountApplyPaymentActionForm) form;
         Integer accountId = Integer.valueOf(actionForm.getAccountId());
         String paymentType = request.getParameter(Constants.INPUT);
         UserReferenceDto userReferenceDto = new UserReferenceDto(userContext.getId());
         AccountPaymentDto accountPaymentDto = accountServiceFacade.getAccountPaymentInformation(accountId, paymentType, userContext.getLocaleId(), userReferenceDto);
 
         validateAccountPayment(accountPaymentDto, accountId, request, userContext);
 
         PaymentTypeDto paymentTypeDto;
         String amount = "0";
         if (accountPaymentDto.getAccountType().equals(AccountTypeDto.LOAN_ACCOUNT)) {
             amount = actionForm.getAmount();
             paymentTypeDto = getLoanPaymentTypeDtoForId(Short.valueOf(actionForm.getPaymentTypeId()));
         } else {
             amount = accountPaymentDto.getTotalPaymentDue().toString();
             paymentTypeDto = getFeePaymentTypeDtoForId(Short.valueOf(actionForm.getPaymentTypeId()));
         }
 
         AccountPaymentParametersDto accountPaymentParametersDto = new AccountPaymentParametersDto(
                 userReferenceDto, new AccountReferenceDto(accountId), new BigDecimal(amount), actionForm.getTrxnDateAsLocalDate(),
                paymentTypeDto, AccountConstants.NO_COMMENT, actionForm.getReceiptDateAsLocalDate(), actionForm.getReceiptId(), null);
 
         getAccountService().makePayment(accountPaymentParametersDto);
 
         return mapping.findForward(getForward(((AccountApplyPaymentActionForm) form).getInput()));
 
     }
 
     private void validateAccountPayment(AccountPaymentDto accountPaymentDto, Integer accountId, HttpServletRequest request, UserContext userContext) throws Exception {
         checkVersion(request, accountPaymentDto.getVersion());
         checkPermission(userContext, accountId);
     }
 
     private void checkVersion(HttpServletRequest request, int accountVersion) throws Exception {
         Integer savedAccountVersion = (Integer) SessionUtils.getAttribute(Constants.ACCOUNT_VERSION, request);
         checkVersionMismatch(savedAccountVersion, accountVersion);
     }
 
     private void checkPermission(UserContext userContext, Integer accountId) throws ServiceException, CustomerException {
         if (!accountServiceFacade.isPaymentPermitted(userContext, accountId)) {
             throw new CustomerException(SecurityConstants.KEY_ACTIVITY_NOT_ALLOWED);
         }
     }
 
     private PaymentTypeDto getLoanPaymentTypeDtoForId(short id) {
         for (PaymentTypeDto paymentTypeDto : loanPaymentTypeDtos) {
             if (paymentTypeDto.getValue() == id) {
                 return paymentTypeDto;
             }
         }
         throw new MifosRuntimeException("Expected loan PaymentTypeDto not found for id: " + id);
     }
 
     private PaymentTypeDto getFeePaymentTypeDtoForId(short id) {
         for (PaymentTypeDto paymentTypeDto : feePaymentTypeDtos) {
             if (paymentTypeDto.getValue() == id) {
                 return paymentTypeDto;
             }
         }
         throw new MifosRuntimeException("Expected fee PaymentTypeDto not found for id: " + id);
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward cancel(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         return mapping.findForward(getForward(((AccountApplyPaymentActionForm) form).getInput()));
     }
 
     private void clearActionForm(AccountApplyPaymentActionForm actionForm) throws InvalidDateException {
         actionForm.setReceiptDate(null);
         actionForm.setReceiptId(null);
         actionForm.setPaymentTypeId(null);
     }
 
     private String getForward(String input) {
         if (input.equals(Constants.LOAN)) {
             return ActionForwards.loan_detail_page.toString();
         }
 
         return "applyPayment_success";
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
         String method = (String) request.getAttribute("methodCalled");
         String forward = null;
         if (method != null) {
             forward = method + "_failure";
         }
         return mapping.findForward(forward);
     }
 
     public AccountPersistence getAccountPersistence() {
         return accountPersistence;
     }
 }
