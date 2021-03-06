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
 * $Id: GetSupportedDataTypes.java,v 1.2 2008-03-11 02:28:33 veiming Exp $
  *
  * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
  */
 
 package com.sun.identity.cli.realm;
 
 import com.iplanet.sso.SSOToken;
 import com.iplanet.sso.SSOException;
 import com.sun.identity.cli.AuthenticatedCommand;
 import com.sun.identity.cli.CLIException;
 import com.sun.identity.cli.ExitCodes;
 import com.sun.identity.cli.IArgument;
 import com.sun.identity.cli.IOutput;
 import com.sun.identity.cli.LogWriter;
 import com.sun.identity.cli.RequestContext;
 import com.sun.identity.idm.IdConstants;
 import com.sun.identity.sm.SMSException;
 import com.sun.identity.sm.ServiceSchema;
 import com.sun.identity.sm.ServiceSchemaManager;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.logging.Level;
 
 /**
  * This command gets the supported data type.
  */
 public class GetSupportedDataTypes extends AuthenticatedCommand {
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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String[] params = {realm};
         writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_SUPPORTED_DATA_TYPES", params);
 
         try {
             ServiceSchemaManager scm = new ServiceSchemaManager(
                 IdConstants.REPO_SERVICE, adminSSOToken);
             ServiceSchema orgSchema = scm.getOrganizationSchema();
             Set names = orgSchema.getSubSchemaNames();
             
             if ((names != null) && !names.isEmpty()) {
                 for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                     String name = (String)iter.next();
                     outputWriter.printlnMessage(name);
                 }
             } else {
                 outputWriter.printlnMessage(getResourceString(
                     "get-supported-no-supported-datatype"));
             }
 
             writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_GET_SUPPORTED_DATA_TYPES", params);
         } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
             debugError("GetSupportedDataTypes.handleRequest", e);
             writeLog(LogWriter.LOG_ERROR, Level.INFO,
                 "FAILED_GET_SUPPORTED_DATA_TYPES", args);
             throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
         } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
             debugError("GetSupportedDataTypes.handleRequest", e);
             writeLog(LogWriter.LOG_ERROR, Level.INFO,
                 "FAILED_GET_SUPPORTED_DATA_TYPES", args);
             throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
         }
     }
 }
