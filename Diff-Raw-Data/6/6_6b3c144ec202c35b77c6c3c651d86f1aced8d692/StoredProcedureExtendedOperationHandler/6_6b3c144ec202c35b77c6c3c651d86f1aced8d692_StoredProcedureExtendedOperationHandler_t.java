 /*
  *   Copyright 2006 The Apache Software Foundation
  *
  *   Licensed under the Apache License, Version 2.0 (the "License");
  *   you may not use this file except in compliance with the License.
  *   You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *   Unless required by applicable law or agreed to in writing, software
  *   distributed under the License is distributed on an "AS IS" BASIS,
  *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *   See the License for the specific language governing permissions and
  *   limitations under the License.
  *
  */
 
 
 package org.apache.directory.server.ldap.support.extended;
 
 
 import java.nio.ByteBuffer;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Set;
 
 import javax.naming.ldap.Control;
 import javax.naming.ldap.LdapContext;
 
 import org.apache.directory.server.ldap.ExtendedOperationHandler;
 import org.apache.directory.server.ldap.LdapProtocolProvider;
 import org.apache.directory.server.ldap.SessionRegistry;
 import org.apache.directory.shared.asn1.ber.Asn1Decoder;
 import org.apache.directory.shared.asn1.ber.IAsn1Container;
 import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
 import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureContainer;
 import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureDecoder;
 import org.apache.directory.shared.ldap.message.ExtendedRequest;
 import org.apache.directory.shared.ldap.message.extended.StoredProcedureRequest;
 import org.apache.directory.shared.ldap.message.extended.StoredProcedureResponse;
 import org.apache.mina.common.IoSession;
 
 
 /**
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$ $Date$
  */
 public class StoredProcedureExtendedOperationHandler implements ExtendedOperationHandler
 {
     public void handleExtendedOperation( IoSession session, SessionRegistry registry, ExtendedRequest req ) throws Exception
     {
         Control[] connCtls = ( Control[] ) req.getControls().values().toArray( new Control[ req.getControls().size() ] );
         LdapContext serverLdapContext = ( LdapContext ) registry.getLdapContext( session, connCtls, false);
         StoredProcedure spBean = decodeBean( req.getPayload() );
         
         LanguageSpecificStoredProceureExtendedOperationHandler handler = null;
         
         /**
          * TODO This part may be replaced by a better handler determiner.
          */
         if ( spBean.getLanguage().equalsIgnoreCase( "Java" ) )
         {
             handler = new JavaStoredProcedureExtendedOperationHandler();
             handler.handleStoredProcedureExtendedOperation( serverLdapContext, spBean );
         }
        
        /**
         * TODO: Temporarily added this response to make things work.
         * We need to send back the SP result btw.
         */
        session.write( req.getResultResponse() );
     }
     
     private StoredProcedure decodeBean( byte[] payload )
     {
         Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();
         ByteBuffer stream = ByteBuffer.wrap( payload );
         IAsn1Container storedProcedureContainer = new StoredProcedureContainer();
 
         try
         {
             storedProcedureDecoder.decode( stream, storedProcedureContainer );
         }
         catch ( Exception de )
         {
             de.printStackTrace();
         }
 
         StoredProcedure spBean = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();
         
         return spBean;
     }
 
     
     public String getOid()
     {
         return StoredProcedureRequest.EXTENSION_OID;
     }
 
 
     private static final Set EXTENSION_OIDS;
     static
     {
         Set s = new HashSet();
         s.add( StoredProcedureRequest.EXTENSION_OID );
         s.add( StoredProcedureResponse.EXTENSION_OID );
         EXTENSION_OIDS = Collections.unmodifiableSet( s );
     }
     
     
     public Set getExtensionOids()
     {
         return EXTENSION_OIDS;
     }
 
     
 	public void setLdapProvider(LdapProtocolProvider provider) 
     {
 	}
 }
