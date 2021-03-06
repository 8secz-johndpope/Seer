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
 
 package org.mifos.security.authentication;
 
 import java.io.IOException;
 import java.util.Random;
 import java.util.ResourceBundle;
 
 import javax.servlet.FilterChain;
 import javax.servlet.ServletException;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import org.mifos.application.admin.system.ShutdownManager;
 import org.mifos.application.servicefacade.LoginActivityDto;
 import org.mifos.application.servicefacade.LegacyLoginServiceFacade;
 import org.mifos.core.MifosRuntimeException;
 import org.mifos.framework.components.batchjobs.MifosBatchJob;
 import org.mifos.framework.exceptions.ApplicationException;
 import org.mifos.framework.util.DateTimeService;
 import org.mifos.framework.util.helpers.Constants;
 import org.mifos.framework.util.helpers.FilePaths;
 import org.mifos.framework.util.helpers.Flow;
 import org.mifos.framework.util.helpers.FlowManager;
 import org.mifos.framework.util.helpers.ServletUtils;
 import org.mifos.security.login.util.helpers.LoginConstants;
 import org.mifos.security.util.UserContext;
 import org.springframework.security.authentication.AuthenticationServiceException;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.AuthenticationException;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 
 /**
  * I am a custom authentication filter implementation of {@link UsernamePasswordAuthenticationFilter}.
  *
  * A custom filter is needed as in the legacy authentication process, certain things were set in the session
  * that are used by the legacy views (jsp pages). When struts and jsp is completely removed, we can revert back to using
  * the out of the box authentication filter that is created using the spring security namespace.
  */
 public class MifosLegacyUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
 
     final LegacyLoginServiceFacade loginServiceFacade;
 
     public MifosLegacyUsernamePasswordAuthenticationFilter(final LegacyLoginServiceFacade loginServiceFacade) {
         super();
         this.loginServiceFacade = loginServiceFacade;
     }
 
     @Override
     public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
             ServletException {
 
         HttpServletRequest request = (HttpServletRequest) req;
         HttpServletResponse response = (HttpServletResponse) res;
         AuthenticationException denied = null;
 
         boolean allowAuthenticationToContinue = true;
         if (MifosBatchJob.isBatchJobRunningThatRequiresExclusiveAccess()) {
             allowAuthenticationToContinue = false;
 
             request.getSession(false).invalidate();
             ResourceBundle resources = ResourceBundle.getBundle(FilePaths.LOGIN_UI_PROPERTY_FILE);
             String errorMessage = resources.getString(LoginConstants.BATCH_JOB_RUNNING);
             denied = new AuthenticationServiceException(errorMessage);
         }
 
         ShutdownManager shutdownManager = (ShutdownManager) ServletUtils.getGlobal(request, ShutdownManager.class.getName());
         if (shutdownManager.isShutdownDone()) {
             allowAuthenticationToContinue = false;
             request.getSession(false).invalidate();
             ResourceBundle resources = ResourceBundle.getBundle(FilePaths.LOGIN_UI_PROPERTY_FILE);
             String errorMessage = resources.getString(LoginConstants.SHUTDOWN);
             denied = new AuthenticationServiceException(errorMessage);
         }
 
         if (shutdownManager.isInShutdownCountdownNotificationThreshold()) {
             request.setAttribute("shutdownIsImminent", true);
         }
 
         if (allowAuthenticationToContinue) {
             super.doFilter(request, response, chain);
         } else {
             unsuccessfulAuthentication(request, response, denied);
         }
     }
 
     @Override
     protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
             Authentication authResult) throws IOException, ServletException {
 
         final String username = obtainUsername(request);
         request.setAttribute("username", username);
         final String password = obtainPassword(request);
 
         handleLegacySuccessfulAuthentication(request, username, password);
 
         super.successfulAuthentication(request, response, authResult);
     }
 
     private void handleLegacySuccessfulAuthentication(HttpServletRequest request, final String username,
             final String password) {
         try {
             FlowManager flowManager = new FlowManager();
             String flowKey = String.valueOf(new DateTimeService().getCurrentDateTime().getMillis());
             flowManager.addFLow(flowKey, new Flow(), this.getFilterName());

             request.setAttribute(Constants.CURRENTFLOWKEY, flowKey);
 
             request.getSession(false).setAttribute(Constants.FLOWMANAGER, flowManager);
             request.getSession(false).setAttribute(Constants.RANDOMNUM, new Random().nextLong());
 
             LoginActivityDto loginActivity = loginServiceFacade.login(username, password);
 
             request.getSession(false).setAttribute(Constants.ACTIVITYCONTEXT, loginActivity.getActivityContext());
             request.setAttribute("activityDto", loginActivity);
 
             UserContext userContext = loginActivity.getUserContext();
             request.setAttribute(Constants.USERCONTEXT, userContext);
             request.getSession(false).setAttribute(Constants.USERCONTEXT, userContext);
 
             if (loginActivity.isPasswordChanged()) {
                 HttpSession hs = request.getSession(false);
                 hs.setAttribute(Constants.USERCONTEXT, userContext);
                 hs.setAttribute("org.apache.struts.action.LOCALE", userContext.getCurrentLocale());
             } else {
                 flowManager.addObjectToFlow(flowKey, Constants.TEMPUSERCONTEXT, userContext);
             }
 
             Short passwordChanged = loginActivity.getPasswordChangedFlag();
             if (null != passwordChanged && LoginConstants.PASSWORDCHANGEDFLAG.equals(passwordChanged)) {
                 flowManager.removeFlow((String) request.getAttribute(Constants.CURRENTFLOWKEY));
                 request.setAttribute(Constants.CURRENTFLOWKEY, null);
             }
         } catch (ApplicationException e1) {
             throw new MifosRuntimeException(e1);
         }
     }
 }
