 /*
  *  Licensed to the Apache Software Foundation (ASF) under one
  *  or more contributor license agreements.  See the NOTICE file
  *  distributed with this work for additional information
  *  regarding copyright ownership.  The ASF licenses this file
  *  to you under the Apache License, Version 2.0 (the
  *  "License"); you may not use this file except in compliance
  *  with the License.  You may obtain a copy of the License at
  *  
  *    http://www.apache.org/licenses/LICENSE-2.0
  *  
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an
  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *  KIND, either express or implied.  See the License for the
  *  specific language governing permissions and limitations
  *  under the License. 
  *  
  */
 package org.apache.directory.server.kerberos.shared.service;
 
 
 import javax.security.auth.kerberos.KerberosPrincipal;
 
 import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
 import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
 import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
 import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
 import org.apache.mina.handler.chain.IoHandlerCommand;
 
 
 /**
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$, $Date$
  */
 public abstract class GetPrincipalStoreEntry implements IoHandlerCommand
 {
     private String contextKey = "context";
 
 
     /**
      * Get a PrincipalStoreEntry given a principal.  The ErrorType is used to indicate
      * whether any resulting error pertains to a server or client.
      *
      * @param principal
      * @param store
      * @param errorType
      * @return The PrincipalStoreEntry
      * @throws Exception
      */
     public PrincipalStoreEntry getEntry( KerberosPrincipal principal, PrincipalStore store, ErrorType errorType )
         throws Exception
     {
         PrincipalStoreEntry entry = null;
 
         try
         {
             entry = store.getPrincipal( principal );
         }
         catch ( Exception e )
         {
             throw new KerberosException( errorType, e );
         }
 
        if ( entry == null )
         {
             throw new KerberosException( errorType );
         }
 
        if ( entry.getKeyMap() == null || entry.getKeyMap().isEmpty() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_NULL_KEY );
        }

         return entry;
     }
 
 
     protected String getContextKey()
     {
         return ( this.contextKey );
     }
 }
