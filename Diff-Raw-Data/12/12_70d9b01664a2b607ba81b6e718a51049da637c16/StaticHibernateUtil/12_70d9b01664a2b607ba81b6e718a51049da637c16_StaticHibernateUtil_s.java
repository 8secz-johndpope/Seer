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
 
 package org.mifos.framework.hibernate.helper;
 
 import org.hibernate.Session;
 import org.hibernate.SessionFactory;
 import org.hibernate.Transaction;
 import org.mifos.framework.components.audit.util.helpers.AuditInterceptor;
 import org.mifos.framework.exceptions.HibernateProcessException;
 import org.mifos.framework.exceptions.HibernateStartUpException;
 
 public class StaticHibernateUtil {
 
     private static HibernateUtil hibernateUtil;
 
     private static boolean commitFalg = true;
 
     public static void setHibernateUtil(HibernateUtil hibernateUtil) {
         StaticHibernateUtil.hibernateUtil = hibernateUtil;
     }
 
     /**
      * This method must be called before using Hibernate!
      */
     public static void initialize() throws HibernateStartUpException {
         hibernateUtil = new HibernateUtil();
     }
 
<<<<<<< HEAD

=======
>>>>>>> ae162ff61f7cc628821155182f314af445c83423

     public static SessionFactory getSessionFactory() {
         return hibernateUtil.getSessionFactory();
     }
 
     public static Session getSessionTL() {
         return hibernateUtil.getSessionTL();
     }
 
     public static AuditInterceptor getInterceptor() {
         return hibernateUtil.getInterceptor();
     }
 
     public static Transaction startTransaction() {
         return hibernateUtil.startTransaction();
     }
 
 
     public static void closeSession() {
         hibernateUtil.closeSession();
     }
 
     public static void flushSession() {
         hibernateUtil.flushSession();
     }
 
     public static void flushAndCloseSession() {
         hibernateUtil.flushAndCloseSession();
     }
 
     public static void flushAndClearSession() {
         hibernateUtil.flushAndClearSession();
     }
 
     public static void commitTransaction() {
         if (commitFalg) {
             hibernateUtil.commitTransaction();
         }
     }
 
     public static void enableCommits() {
         commitFalg = true;
 
     }
 
     public static void disableCommits() {
         commitFalg = false;
 
     }
 
     public static void rollbackTransaction() {
         hibernateUtil.rollbackTransaction();
     }
 
 }
