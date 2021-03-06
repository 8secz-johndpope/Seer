 /*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
 package org.infinispan.test.fwk;
 
 import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
 import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
 import org.infinispan.util.LegacyKeySupportSystemProperties;
 
 import javax.transaction.TransactionManager;
 import javax.transaction.UserTransaction;
 
 /**
  * A simple abstraction for transaction manager interaction
  *
  * @author Jason T. Greene
  */
 public class TransactionSetup {
    private interface Operations {
       UserTransaction getUserTransaction();
 
       String getLookup();
 
       void cleanup();
 
       TransactionManager getManager();
    }
 
   public static final String JTA = LegacyKeySupportSystemProperties.getProperty("infinispan.test.jta.tm", "infinispan.tm");
    public static final String JBOSS_TM = "jbosstm";
 
    private static Operations operations;
 
    static {
       init();
    }
 
    private static void init() {
       String property = JTA;
       if (JBOSS_TM.equalsIgnoreCase(property)) {
          final String lookup = JBossStandaloneJTAManagerLookup.class.getName();
          final JBossStandaloneJTAManagerLookup instance = new JBossStandaloneJTAManagerLookup();
          operations = new Operations() {
             public UserTransaction getUserTransaction() {
                try {
                   return instance.getUserTransaction();
                }
                catch (Exception e) {
                   throw new RuntimeException(e);
                }
             }
 
             public void cleanup() {
             }
 
             public String getLookup() {
                return lookup;
             }
 
             public TransactionManager getManager() {
                try {
                   return instance.getTransactionManager();
                }
                catch (Exception e) {
                   throw new RuntimeException(e);
                }
             }
          };
       } else {
          final String lookup = DummyTransactionManagerLookup.class.getName();
          final DummyTransactionManagerLookup instance = new DummyTransactionManagerLookup();
          operations = new Operations() {
             public UserTransaction getUserTransaction() {
                return instance.getUserTransaction();
             }
 
             public void cleanup() {
                instance.cleanup();
             }
 
             public String getLookup() {
                return lookup;
             }
 
             public TransactionManager getManager() {
                try {
                   return instance.getTransactionManager();
                }
                catch (Exception e) {
                   throw new RuntimeException(e);
                }
 
             }
          };
       }
    }
 
    public static TransactionManager getManager() {
       return operations.getManager();
    }
 
    public static String getManagerLookup() {
       return operations.getLookup();
    }
 
    public static UserTransaction getUserTransaction() {
       return operations.getUserTransaction();
    }
 
    public static void cleanup() {
       operations.cleanup();
    }
 }
