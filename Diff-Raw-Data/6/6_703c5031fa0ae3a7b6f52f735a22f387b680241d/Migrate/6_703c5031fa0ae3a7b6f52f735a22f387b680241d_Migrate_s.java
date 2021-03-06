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
 * $Id: Migrate.java,v 1.1 2008-01-24 00:32:28 bina Exp $
  *
  * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
  */
 
 import com.sun.identity.upgrade.MigrateTasks;
 import com.sun.identity.upgrade.UpgradeException;
 import com.sun.identity.upgrade.UpgradeUtils;
 
 public class Migrate implements MigrateTasks {
 
     static final String SCHEMA_FILE = "AgentService.xml";
 
     /**
      * Adds new service schema AgentService.
      *
      * @return true if service creation was successful else false.
      */
     public boolean migrateService() {
         boolean isSuccess = false;
         try {
             String fileName = UpgradeUtils.getNewServiceNamePath(SCHEMA_FILE);
             UpgradeUtils.createService(fileName);
             return true;
         } catch (UpgradeException e) {
         // log error
         }
         return true;
     }
 
     /**
      * Post Migration operations.
      *
      * @return true if successful else error.
      */
     public boolean postMigrateTask() {
         return true;
     }
 
     /**
      * Pre Migration operations.
      *
      * @return true if successful else error.
      */
     public boolean preMigrateTask() {
         return true;
     }
 }
