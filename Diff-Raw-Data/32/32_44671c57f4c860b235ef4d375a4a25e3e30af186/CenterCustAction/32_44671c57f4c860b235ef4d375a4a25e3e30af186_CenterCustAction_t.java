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
 
 package org.mifos.customers.center.struts.action;
 
 import org.apache.struts.action.ActionErrors;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.joda.time.DateTime;
 import org.mifos.application.meeting.business.MeetingBO;
 import org.mifos.application.questionnaire.struts.DefaultQuestionnaireServiceFacadeLocator;
 import org.mifos.application.questionnaire.struts.QuestionnaireFlowAdapter;
 import org.mifos.application.questionnaire.struts.QuestionnaireServiceFacadeLocator;
 import org.mifos.application.servicefacade.CenterCreation;
 import org.mifos.application.servicefacade.CenterDto;
 import org.mifos.application.servicefacade.CenterFormCreationDto;
 import org.mifos.application.servicefacade.CenterUpdate;
 import org.mifos.application.servicefacade.CustomerDetailsDto;
 import org.mifos.application.servicefacade.CustomerSearch;
 import org.mifos.application.util.helpers.ActionForwards;
 import org.mifos.application.util.helpers.Methods;
 import org.mifos.customers.business.CustomerCustomFieldEntity;
 import org.mifos.customers.center.business.CenterBO;
 import org.mifos.customers.center.business.service.CenterInformationDto;
 import org.mifos.customers.center.struts.actionforms.CenterCustActionForm;
 import org.mifos.customers.struts.action.CustAction;
 import org.mifos.customers.util.helpers.CustomerConstants;
 import org.mifos.dto.screen.OnlyBranchOfficeHierarchyDto;
 import org.mifos.framework.exceptions.PageExpiredException;
 import org.mifos.framework.util.helpers.CloseSession;
 import org.mifos.framework.util.helpers.Constants;
 import org.mifos.framework.util.helpers.SessionUtils;
 import org.mifos.framework.util.helpers.TransactionDemarcate;
 import org.mifos.platform.questionnaire.service.QuestionGroupInstanceDetail;
 import org.mifos.platform.questionnaire.service.QuestionnaireServiceFacade;
 import org.mifos.security.util.ActionSecurity;
 import org.mifos.security.util.SecurityConstants;
 import org.mifos.security.util.UserContext;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 import java.util.List;
 
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.METHODCALLED;
 
 public class CenterCustAction extends CustAction {
 
     private QuestionnaireServiceFacadeLocator questionnaireServiceFacadeLocator = new DefaultQuestionnaireServiceFacadeLocator();
 
     private QuestionnaireFlowAdapter createCenterQuestionnaire = new QuestionnaireFlowAdapter("Create", "Center", 
             ActionForwards.preview_success, "custSearchAction.do?method=loadMainSearch", questionnaireServiceFacadeLocator
         );
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("centerCustAction");
         security.allow("chooseOffice", SecurityConstants.CENTER_CREATE_NEW_CENTER);
         security.allow("load", SecurityConstants.CENTER_CREATE_NEW_CENTER);
         security.allow("loadMeeting", SecurityConstants.MEETING_CREATE_CENTER_MEETING);
         security.allow("previous", SecurityConstants.VIEW);
         security.allow("preview", SecurityConstants.VIEW);
         security.allow("create", SecurityConstants.CENTER_CREATE_NEW_CENTER);
         security.allow("manage", SecurityConstants.CENTER_MODIFY_CENTER_INFORMATION_AND_CHANGE_CENTER_STATUS);
         security.allow("editPrevious", SecurityConstants.VIEW);
         security.allow("editPreview", SecurityConstants.VIEW);
         security.allow("update", SecurityConstants.CENTER_MODIFY_CENTER_INFORMATION_AND_CHANGE_CENTER_STATUS);
 
         security.allow("get", SecurityConstants.VIEW);
         security.allow("loadSearch", SecurityConstants.VIEW);
         security.allow("search", SecurityConstants.VIEW);
         security.allow("loadChangeLog", SecurityConstants.VIEW);
         security.allow("cancelChangeLog", SecurityConstants.VIEW);
 
         security.allow("loadTransferSearch", SecurityConstants.VIEW);
         security.allow("searchTransfer", SecurityConstants.VIEW);
         security.allow("captureQuestionResponses", SecurityConstants.VIEW);
         security.allow("editQuestionResponses", SecurityConstants.VIEW);
         return security;
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward chooseOffice(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         UserContext userContext = getUserContext(request);
 
         OnlyBranchOfficeHierarchyDto officeHierarchy = customerServiceFacade
                 .retrieveBranchOnlyOfficeHierarchy(userContext);
 
         SessionUtils.setAttribute(OnlyBranchOfficeHierarchyDto.IDENTIFIER, officeHierarchy, request);
        SessionUtils.setAttribute(CustomerConstants.URL_MAP, null, request.getSession(false));
 
         return mapping.findForward(ActionForwards.chooseOffice_success.toString());
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         actionForm.clearActionFormFields();
         SessionUtils.removeAttribute(CustomerConstants.CUSTOMER_MEETING, request);
 
         UserContext userContext = getUserContext(request);
         CenterCreation centerCreationDto = new CenterCreation(actionForm.getOfficeIdValue(), userContext.getId(),
                 userContext.getLevelId(), userContext.getPreferredLocale());
 
         CenterFormCreationDto centerFormCreation = this.customerServiceFacade.retrieveCenterFormCreationData(
                 centerCreationDto, userContext);
 
         SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, centerFormCreation
                 .getCustomFieldViews(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.LOAN_OFFICER_LIST, centerFormCreation
                 .getActiveLoanOfficersForBranch(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.ADDITIONAL_FEES_LIST, centerFormCreation
                 .getAdditionalFees(), request);
         actionForm.setCustomFields(centerFormCreation.getCustomFieldViews());
         actionForm.setDefaultFees(centerFormCreation.getDefaultFees());
 
         DateTime today = new DateTime().toDateMidnight().toDateTime();
         actionForm.setMfiJoiningDate(today.getDayOfMonth(), today.getMonthOfYear(), today.getYear());
 
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward loadMeeting(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             @SuppressWarnings("unused") HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         // NOTE - forwards to MeetingAction.load and MeetingAction.create to save meeting schedule
         return mapping.findForward(ActionForwards.loadMeeting_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         // NOTE - pulls information from session scope variables and from actionform in session scope
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         return createCenterQuestionnaire.fetchAppliedQuestions(
                 mapping, actionForm, request, ActionForwards.preview_success);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previous(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             @SuppressWarnings("unused") HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.previous_success.toString());
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         MeetingBO meeting = (MeetingBO) SessionUtils.getAttribute(CustomerConstants.CUSTOMER_MEETING, request);
         UserContext userContext = getUserContext(request);
 
         List<CustomerCustomFieldEntity> customerCustomFields = CustomerCustomFieldEntity.fromDto(actionForm.getCustomFields(), null);
         CustomerDetailsDto centerDetails = this.customerServiceFacade.createNewCenter(actionForm, meeting, userContext, customerCustomFields);
         createCenterQuestionnaire.saveResponses(request, actionForm, centerDetails.getId());
 
         actionForm.setCustomerId(centerDetails.getId().toString());
         actionForm.setGlobalCustNum(centerDetails.getGlobalCustNum());
 
         return mapping.findForward(ActionForwards.create_success.toString());
     }
 
     // NOTE edit center details
     @TransactionDemarcate(joinToken = true)
     public ActionForward manage(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
 
         actionForm.clearActionFormFields();
         CenterBO center = (CenterBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
         final Integer centerId = center.getCustomerId();
         UserContext userContext = getUserContext(request);
 
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, null, request);
 
         CenterDto centerDto = this.customerServiceFacade.retrieveCenterDetailsForUpdate(centerId, userContext);
 
         actionForm.setLoanOfficerId(centerDto.getLoanOfficerIdAsString());
         actionForm.setCustomerId(centerDto.getCustomerIdAsString());
         actionForm.setGlobalCustNum(centerDto.getGlobalCustNum());
         actionForm.setExternalId(centerDto.getExternalId());
         actionForm.setMfiJoiningDate(centerDto.getMfiJoiningDateAsString());
         actionForm.setMfiJoiningDate(centerDto.getMfiJoiningDate().getDayOfMonth(), centerDto.getMfiJoiningDate()
                 .getMonthOfYear(), centerDto.getMfiJoiningDate().getYear());
         actionForm.setAddress(centerDto.getAddress());
         actionForm.setCustomerPositions(centerDto.getCustomerPositionViews());
         actionForm.setCustomFields(centerDto.getCustomFieldViews());
 
         SessionUtils.setAttribute(Constants.BUSINESS_KEY, centerDto.getCenter(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.LOAN_OFFICER_LIST, centerDto
                 .getActiveLoanOfficersForBranch(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, centerDto.getCustomFieldViews(),
                 request);
         SessionUtils.setCollectionAttribute(CustomerConstants.POSITIONS, centerDto.getCustomerPositionViews(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.CLIENT_LIST, centerDto.getClientList(), request);
 
         return mapping.findForward(ActionForwards.manage_success.toString());
     }
 
     // NOTE - manage->preview
     @TransactionDemarcate(joinToken = true)
     public ActionForward editPreview(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             @SuppressWarnings("unused") HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.editpreview_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editPrevious(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             @SuppressWarnings("unused") HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.editprevious_success.toString());
     }
 
     // NOTE - manage->preview->update
     @CloseSession
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         CenterBO centerFromSession = (CenterBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         UserContext userContext = getUserContext(request);
 
         CenterUpdate centerUpdate = new CenterUpdate(centerFromSession.getCustomerId(), centerFromSession
                 .getVersionNo(), actionForm.getLoanOfficerIdValue(), actionForm.getExternalId(), actionForm
                 .getMfiJoiningDate(), actionForm.getAddress(), actionForm.getCustomFields(), actionForm.getCustomerPositions());
 
         this.customerServiceFacade.updateCenter(userContext, centerUpdate);
 
         return mapping.findForward(ActionForwards.update_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validate(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
             HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         String method = (String) request.getAttribute("methodCalled");
         return mapping.findForward(method + "_failure");
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward cancel(ActionMapping mapping, ActionForm form,
             @SuppressWarnings("unused") HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) {
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         ActionForwards forward = null;
 
         if (actionForm.getInput().equals(Methods.create.toString())) {
             forward = ActionForwards.cancel_success;
         } else if (actionForm.getInput().equals(Methods.manage.toString())) {
             forward = ActionForwards.editcancel_success;
         }
 
         return mapping.findForward(forward.toString());
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward get(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         // John W - UserContext passed because some status' need to be looked up for internationalisation
         CenterInformationDto centerInformationDto = this.centerDetailsServiceFacade.getCenterInformationDto(
                 ((CenterCustActionForm) form).getGlobalCustNum(), getUserContext(request));
         SessionUtils.removeThenSetAttribute("centerInformationDto", centerInformationDto, request);
 
         // John W - 'BusinessKey' attribute used by breadcrumb but is not in associated jsp
         CenterBO centerBO = (CenterBO) this.customerDao.findCustomerById(centerInformationDto.getCenterDisplay().getCustomerId());
         SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, centerBO, request);
         setCurrentPageUrl(request, centerBO);
         setQuestionGroupInstances(request, centerBO);
 
         return mapping.findForward(ActionForwards.get_success.toString());
     }
 
     private void setQuestionGroupInstances(HttpServletRequest request, CenterBO centerBO) throws PageExpiredException {
         QuestionnaireServiceFacade questionnaireServiceFacade = questionnaireServiceFacadeLocator.getService(request);
         if (questionnaireServiceFacade != null) {
             setQuestionGroupInstances(questionnaireServiceFacade, request, centerBO.getCustomerId());
         }
     }
 
     // Intentionally made public to aid testing !
     public void setQuestionGroupInstances(QuestionnaireServiceFacade questionnaireServiceFacade, HttpServletRequest request, Integer customerId) throws PageExpiredException {
         List<QuestionGroupInstanceDetail> instanceDetails = questionnaireServiceFacade.getQuestionGroupInstances(customerId, "View", "Center");
         SessionUtils.setCollectionAttribute("questionGroupInstances", instanceDetails, request);
     }
 
     private void setCurrentPageUrl(HttpServletRequest request, CenterBO centerBO) throws PageExpiredException, UnsupportedEncodingException {
         SessionUtils.removeThenSetAttribute("currentPageUrl", constructCurrentPageUrl(request, centerBO), request);
     }
 
     private String constructCurrentPageUrl(HttpServletRequest request, CenterBO centerBO) throws UnsupportedEncodingException {
         String officerId = request.getParameter("recordOfficeId");
         String loanOfficerId = request.getParameter("recordLoanOfficerId");
         String url = String.format("centerCustAction.do?globalCustNum=%s&recordOfficeId=%s&recordLoanOfficerId=%s",
                 centerBO.getGlobalCustNum(), officerId, loanOfficerId);
         return URLEncoder.encode(url, "UTF-8");
     }
 
     @TransactionDemarcate(conditionToken = true)
     public ActionForward loadSearch(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         cleanSearchResults(request, (CenterCustActionForm) form);
         return mapping.findForward(ActionForwards.loadSearch_success.toString());
     }
 
     @TransactionDemarcate(conditionToken = true)
     public ActionForward loadTransferSearch(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         cleanSearchResults(request, (CenterCustActionForm) form);
         return mapping.findForward(ActionForwards.loadTransferSearch_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward searchTransfer(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         cleanUpSearch(request);
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         String searchString = actionForm.getSearchString();
         UserContext userContext = getUserContext(request);
 
         CustomerSearch searchResult = this.customerServiceFacade.search(searchString, userContext);
 
         addSeachValues(searchString, searchResult.getOfficeId(), searchResult.getOfficeName(), request);
         SessionUtils.setQueryResultAttribute(Constants.SEARCH_RESULTS, searchResult.getSearchResult(), request);
         return mapping.findForward(ActionForwards.transferSearch_success.toString());
     }
 
     /**
      * invoked when searching for centers from group creation screen
      */
     @Override
     @TransactionDemarcate(joinToken = true)
     public ActionForward search(ActionMapping mapping, ActionForm form, HttpServletRequest request,
             HttpServletResponse response) throws Exception {
 
         ActionForward actionForward = super.search(mapping, form, request, response);
         CenterCustActionForm actionForm = (CenterCustActionForm) form;
         String searchString = actionForm.getSearchString();
         UserContext userContext = getUserContext(request);
 
         CustomerSearch searchResult = this.customerServiceFacade.search(searchString, userContext);
 
         addSeachValues(searchString, searchResult.getOfficeId(), searchResult.getOfficeName(), request);
         SessionUtils.setQueryResultAttribute(Constants.SEARCH_RESULTS, searchResult.getSearchResult(), request);
         return actionForward;
     }
 
     private void cleanSearchResults(HttpServletRequest request, CenterCustActionForm actionForm)
             throws PageExpiredException {
         actionForm.setSearchString(null);
         cleanUpSearch(request);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward captureQuestionResponses(
             final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
             @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "captureQuestionResponses");
         ActionErrors errors = createCenterQuestionnaire.validateResponses(request, (CenterCustActionForm) form);
         if (errors != null && !errors.isEmpty()) {
             addErrors(request, errors);
             return mapping.findForward(ActionForwards.captureQuestionResponses.toString());
         }
         return createCenterQuestionnaire.rejoinFlow(mapping);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editQuestionResponses(
             final ActionMapping mapping, final ActionForm form,
             final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "editQuestionResponses");
         return createCenterQuestionnaire.editResponses(mapping, request, (CenterCustActionForm) form);
     }
 }
