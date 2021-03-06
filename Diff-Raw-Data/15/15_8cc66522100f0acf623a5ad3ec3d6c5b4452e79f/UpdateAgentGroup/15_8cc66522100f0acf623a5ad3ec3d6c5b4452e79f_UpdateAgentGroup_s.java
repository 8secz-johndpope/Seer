 /* The contents of this file are subject to the terms
  * of the Common Development and Distribution License
  * (the License). You may not use this file except in
  * compliance with the License.
  *
  * You can obtain a copy of the License at
  * https://opensso.dev.java.net/public/CDDLv1.0.html or
  * opensso/legal/CDDLv1.0.txt
  * See the License for the specific language governing
  * permission and limitations under the License.
  *
  * When distributing Covered Code, include this CDDL
  * Header Notice in each file and include the License file
  * at opensso/legal/CDDLv1.0.txt.
  * If applicable, add the following below the CDDL Header,
  * with the fields enclosed by brackets [] replaced by
  * your own identifying information:
  * "Portions Copyrighted [year] [name of copyright owner]"
  *
 * $Id: UpdateAgentGroup.java,v 1.2 2008-01-02 18:01:51 veiming Exp $
  *
  * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
  */
 
 package com.sun.identity.cli.agentconfig;
 
 import com.iplanet.sso.SSOToken;
 import com.iplanet.sso.SSOException;
 import com.sun.identity.cli.AttributeValues;
 import com.sun.identity.cli.AuthenticatedCommand;
 import com.sun.identity.cli.CLIException;
 import com.sun.identity.cli.ExitCodes;
 import com.sun.identity.cli.IArgument;
 import com.sun.identity.cli.IOutput;
 import com.sun.identity.cli.LogWriter;
 import com.sun.identity.cli.RequestContext;
 import com.sun.identity.common.configuration.AgentConfiguration;
 import com.sun.identity.idm.IdRepoException;
 import com.sun.identity.sm.SMSException;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 
 /**
  * This command sets the attribute values of an agent group.
  */
 public class UpdateAgentGroup extends AuthenticatedCommand {
     /**
      * Services a Commandline Request.
      *
      * @param rc Request Context.
      * @throw CLIException if the request cannot serviced.
      */
     public void handleRequest(RequestContext rc) 
         throws CLIException {
         super.handleRequest(rc);
         ldapLogin();
 
         SSOToken adminSSOToken = getAdminSSOToken();
         IOutput outputWriter = getOutputWriter();
         String realm = "/";
         String agentGroupName = getStringOptionValue(
             IArgument.AGENT_GROUP_NAME);
         String datafile = getStringOptionValue(IArgument.DATA_FILE);
         boolean bSet = isOptionSet(IArgument.AGENT_SET_ATTR_VALUE);
         List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);
 
         if ((datafile == null) && (attrValues == null)) {
             throw new CLIException(
                 getResourceString("missing-attributevalues"),
                 ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
         }
 
         Map attributeValues = AttributeValues.parse(getCommandManager(),
             datafile, attrValues);
         String[] params = {realm, agentGroupName};
 
         try {
             writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                 "ATTEMPT_UPDATE_AGENT_GROUP", params);
             AgentConfiguration.updateAgentGroup(adminSSOToken, agentGroupName, 
                 attributeValues, bSet);
             outputWriter.printlnMessage(getResourceString(
                "update-agent-succeeded"));
             writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                 "SUCCEED_UPDATE_AGENT_GROUP", params);
         } catch (IdRepoException e) {
             String[] args = {realm, agentGroupName, e.getMessage()};
             debugError("UpdateAgent.handleRequest", e);
             writeLog(LogWriter.LOG_ERROR, Level.INFO, 
                 "FAILED_UPDATE_AGENT_GROUP", args);
             throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
         } catch (SMSException e) {
             String[] args = {realm, agentGroupName, e.getMessage()};
             debugError("UpdateAgent.handleRequest", e);
             writeLog(LogWriter.LOG_ERROR, Level.INFO, 
                 "FAILED_UPDATE_AGENT_GROUP", args);
             throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
         } catch (SSOException e) {
             String[] args = {realm, agentGroupName, e.getMessage()};
             debugError("UpdateAgent.handleRequest", e);
             writeLog(LogWriter.LOG_ERROR, Level.INFO, 
                 "FAILED_UPDATE_AGENT_GROUP", args);
             throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
         }
     }
 }
