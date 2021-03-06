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
 package org.apache.directory.server.operations.bind;
 
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
 
 import java.lang.reflect.Field;
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.nio.ByteBuffer;
 
 import javax.naming.NamingEnumeration;
 import javax.naming.directory.Attribute;
 import javax.naming.directory.Attributes;
 import javax.naming.directory.DirContext;
 import javax.naming.directory.InitialDirContext;
 
 import org.apache.commons.lang.ArrayUtils;
 import org.apache.commons.net.SocketClient;
 import org.apache.directory.ldap.client.api.LdapConnection;
 import org.apache.directory.ldap.client.api.LdapNetworkConnection;
 import org.apache.directory.server.annotations.CreateKdcServer;
 import org.apache.directory.server.annotations.CreateLdapServer;
 import org.apache.directory.server.annotations.CreateTransport;
 import org.apache.directory.server.annotations.SaslMechanism;
 import org.apache.directory.server.core.annotations.ApplyLdifs;
 import org.apache.directory.server.core.annotations.ContextEntry;
 import org.apache.directory.server.core.annotations.CreateDS;
 import org.apache.directory.server.core.annotations.CreateIndex;
 import org.apache.directory.server.core.annotations.CreatePartition;
 import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
 import org.apache.directory.server.core.integ.FrameworkRunner;
 import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
 import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
 import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
 import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
 import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
 import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
 import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
 import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
 import org.apache.directory.shared.ldap.constants.SchemaConstants;
 import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
 import org.apache.directory.shared.ldap.entry.DefaultEntry;
 import org.apache.directory.shared.ldap.entry.Entry;
 import org.apache.directory.shared.ldap.exception.LdapException;
 import org.apache.directory.shared.ldap.message.BindRequest;
 import org.apache.directory.shared.ldap.message.BindRequestImpl;
 import org.apache.directory.shared.ldap.message.BindResponse;
 import org.apache.directory.shared.ldap.message.LdapEncoder;
 import org.apache.directory.shared.ldap.message.MessageDecoder;
 import org.apache.directory.shared.ldap.message.ModifyRequest;
 import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
 import org.apache.directory.shared.ldap.message.ResultCodeEnum;
 import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
 import org.apache.directory.shared.ldap.name.DN;
 import org.junit.Ignore;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 
 /**
  * An {@link AbstractServerTest} testing SASL authentication.
  * 
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  */
 @RunWith(FrameworkRunner.class)
 @ApplyLdifs(
     {
         // Entry # 1
         "dn: ou=users,dc=example,dc=com",
         "objectClass: organizationalUnit",
         "objectClass: top",
         "ou: users\n",
         // Entry # 2
         "dn: uid=hnelson,ou=users,dc=example,dc=com",
         "objectClass: inetOrgPerson",
         "objectClass: organizationalPerson",
         "objectClass: person",
         "objectClass: krb5principal",
         "objectClass: krb5kdcentry",
         "objectClass: top",
         "uid: hnelson",
         "userPassword: secret",
         "krb5PrincipalName: hnelson@EXAMPLE.COM",
         "krb5KeyVersionNumber: 0",
         "cn: Horatio Nelson",
         "sn: Nelson",
     
         // krbtgt
         "dn: uid=krbtgt,ou=users,dc=example,dc=com",
         "objectClass: inetOrgPerson",
         "objectClass: organizationalPerson",
         "objectClass: person",
         "objectClass: krb5principal",
         "objectClass: krb5kdcentry",
         "objectClass: top",
         "uid: krbtgt",
         "userPassword: secret",
         "krb5PrincipalName: krbtgt/EXAMPLE.COM@EXAMPLE.COM",
         "krb5KeyVersionNumber: 0",
         "cn: KDC Service",
         "sn: Service",
         
         // ldap per host
         "dn: uid=ldap,ou=users,dc=example,dc=com",
         "objectClass: inetOrgPerson",
         "objectClass: organizationalPerson",
         "objectClass: person",
         "objectClass: krb5principal",
         "objectClass: krb5kdcentry",
         "objectClass: top",
         "uid: ldap",
         "userPassword: randall",
         "krb5PrincipalName: ldap/localhost@EXAMPLE.COM",
         "krb5KeyVersionNumber: 0",
         "cn: LDAP Service",
         "sn: Service"
     })
 @CreateDS(allowAnonAccess = false, name = "SaslBindIT-class", partitions =
     { @CreatePartition(name = "example", suffix = "dc=example,dc=com", contextEntry = @ContextEntry(entryLdif = "dn: dc=example,dc=com\n"
         + "dc: example\n" + "objectClass: top\n" + "objectClass: domain\n\n"), indexes =
         { @CreateIndex(attribute = "objectClass"), @CreateIndex(attribute = "dc"), @CreateIndex(attribute = "ou") }) },
 additionalInterceptors = { KeyDerivationInterceptor.class }
 )
 @CreateLdapServer(transports =
     { @CreateTransport(protocol = "LDAP") }, saslHost = "localhost", saslPrincipal="ldap/localhost@EXAMPLE.COM", saslMechanisms =
     { @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
         @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
         @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
         @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
         @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
         @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class) }, extendedOpHandlers =
     { StoredProcedureExtendedOperationHandler.class }, ntlmProvider = BogusNtlmProvider.class)
 @CreateKdcServer ( 
     transports = 
     {
         @CreateTransport( protocol = "UDP", port = 6088 ),
         @CreateTransport( protocol = "TCP", port = 6088 )
     })
 public class SaslBindIT extends AbstractLdapTestUnit
 {
     public SaslBindIT() throws Exception
     {
         // On Windows 7 and Server 2008 the loopback address 127.0.0.1
         // isn't resolved to localhost by default. In that case we need
         // to use the IP address for the service principal.
         String hostName;
         try
         {
             InetAddress loopback = InetAddress.getByName( "127.0.0.1" );
             hostName = loopback.getHostName();
         }
         catch ( UnknownHostException e )
         {
             System.err.println( "Can't find loopback address '127.0.0.1', using hostname 'localhost'" );
             hostName = "localhost";
         }
         String servicePrincipal = "ldap/" + hostName + "@EXAMPLE.COM";
         ldapServer.setSaslPrincipal( servicePrincipal );
 
         ModifyRequest modifyRequest = new ModifyRequestImpl();
         modifyRequest.setName( new DN( "uid=ldap,ou=users,dc=example,dc=com" ) );
         modifyRequest.replace( "userPassword", "randall" );
         modifyRequest.replace( "krb5PrincipalName", servicePrincipal );
         service.getAdminSession().modify( modifyRequest );
     }
 
 
     /**
      * Tests to make sure the server properly returns the supportedSASLMechanisms.
      */
     @Test
     public void testSupportedSASLMechanisms() throws Exception
     {
         // We have to tell the server that it should accept anonymous
         // auth, because we are reading the rootDSE
         ldapServer.getDirectoryService().setAllowAnonymousAccess( true );
 
         // Point on rootDSE
         DirContext context = new InitialDirContext();
 
         Attributes attrs = context.getAttributes( "ldap://localhost:" + ldapServer.getPort(), new String[]
             { "supportedSASLMechanisms" } );
 
         //             Thread.sleep( 10 * 60 * 1000 );
         NamingEnumeration<? extends Attribute> answer = attrs.getAll();
         Attribute result = answer.next();
         assertEquals( 6, result.size() );
         assertTrue( result.contains( SupportedSaslMechanisms.GSSAPI ) );
         assertTrue( result.contains( SupportedSaslMechanisms.DIGEST_MD5 ) );
         assertTrue( result.contains( SupportedSaslMechanisms.CRAM_MD5 ) );
         assertTrue( result.contains( SupportedSaslMechanisms.NTLM ) );
         assertTrue( result.contains( SupportedSaslMechanisms.PLAIN ) );
         assertTrue( result.contains( SupportedSaslMechanisms.GSS_SPNEGO ) );
     }
 
 
     /**
      * Tests to make sure PLAIN-binds works
      */
     @Test
     public void testSaslBindPLAIN() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
         BindRequest bindReq = new BindRequestImpl();
         bindReq.setCredentials( "secret".getBytes() );
         bindReq.setName( userDn );
         bindReq.setSaslMechanism( SupportedSaslMechanisms.PLAIN );
 
         BindResponse resp = connection.bind( bindReq );
         assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
 
         Entry entry = connection.lookup( userDn );
         assertEquals( "hnelson", entry.get( "uid" ).getString() );
 
         connection.close();
     }
 
 
     /**
      * Test a SASL bind with an empty mechanism 
      */
     @Test
     public void testSaslBindNoMech() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
         BindRequest bindReq = new BindRequestImpl();
         bindReq.setCredentials( "secret".getBytes() );
         bindReq.setName( userDn );
         bindReq.setSaslMechanism( "" ); // invalid mechanism
         bindReq.setSimple( false );
 
        try
        {
            connection.bind( bindReq );
            fail();
        }
        catch ( LdapException le )
        {
            //expected
        }

         connection.close();
     }
 
 
     /**
      * Tests to make sure CRAM-MD5 binds below the RootDSE work.
      */
     @Test
     public void testSaslCramMd5Bind() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindCramMd5( userDn.getName(), "secret", null );
         assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
 
         Entry entry = connection.lookup( userDn );
         assertEquals( "hnelson", entry.get( "uid" ).getString() );
 
         connection.close();
     }
 
 
     /**
      * Tests to make sure CRAM-MD5 binds below the RootDSE fail if the password is bad.
      */
     @Test
     public void testSaslCramMd5BindBadPassword() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindCramMd5( userDn.getName(), "badsecret", null );
         assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );
         connection.close();
     }
 
 
     /**
      * Tests to make sure DIGEST-MD5 binds below the RootDSE work.
      */
     @Test
     public void testSaslDigestMd5Bind() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindDigestMd5( userDn.getName(), "secret", null, ldapServer.getSaslRealms()
                 .get( 0 ) );
         assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
 
         Entry entry = connection.lookup( userDn );
         assertEquals( "hnelson", entry.get( "uid" ).getString() );
 
         connection.close();
     }
 
 
     /**
      * GSSAPI test
      */
     @Test
     public void testSaslGssApiBind() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindGssApi( userDn.getName(), "secret", ldapServer.getSaslRealms().get( 0 )
             .toUpperCase(), "localhost", 6088 );
         assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
 
         Entry entry = connection.lookup( userDn );
         assertEquals( "hnelson", entry.get( "uid" ).getString() );
 
         connection.close();
     }
 
     
     /**
      * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the realm is bad.
      */
     @Test
     public void testSaslDigestMd5BindBadRealm() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindDigestMd5( userDn.getName(), "secret", null, "badrealm.com" );
         assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );
 
         connection.close();
     }
 
 
     /**
      * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the password is bad.
      */
     @Test
     public void testSaslDigestMd5BindBadPassword() throws Exception
     {
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
         LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
 
         BindResponse resp = connection.bindDigestMd5( userDn.getName(), "badsecret", null, ldapServer
                 .getSaslRealms().get( 0 ) );
         assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );
 
         connection.close();
     }
 
 
     /**
      * Tests that the plumbing for NTLM bind works.
      */
     @Test
     public void testNtlmBind() throws Exception
     {
         BogusNtlmProvider provider = getNtlmProviderUsingReflection();
 
         NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.NTLM );
         BindResponse type2response = client.bindType1( "type1_test".getBytes() );
         assertEquals( 1, type2response.getMessageId() );
         assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
         assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
 
         BindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
         assertEquals( 2, finalResponse.getMessageId() );
         assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type3_test".getBytes(), provider.getType3Response() ) );
     }
 
 
     /**
      * Tests that the plumbing for NTLM bind works.
      */
     @Test
     public void testGssSpnegoBind() throws Exception
     {
         BogusNtlmProvider provider = new BogusNtlmProvider();
 
         // the provider configured in @CreateLdapServer only sets for the NTLM mechanism
         // but we use the same NtlmMechanismHandler class for GSS_SPNEGO too but this is a separate
         // instance, so we need to set the provider in the NtlmMechanismHandler instance of GSS_SPNEGO mechanism
         NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get(
             SupportedSaslMechanisms.GSS_SPNEGO );
         ntlmHandler.setNtlmProvider( provider );
 
         NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.GSS_SPNEGO );
         BindResponse type2response = client.bindType1( "type1_test".getBytes() );
         assertEquals( 1, type2response.getMessageId() );
         assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type1_test".getBytes(), provider.getType1Response() ) );
         assertTrue( ArrayUtils.isEquals( "challenge".getBytes(), type2response.getServerSaslCreds() ) );
 
         BindResponse finalResponse = client.bindType3( "type3_test".getBytes() );
         assertEquals( 2, finalResponse.getMessageId() );
         assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
         assertTrue( ArrayUtils.isEquals( "type3_test".getBytes(), provider.getType3Response() ) );
     }
 
 
     /**
      * Test for DIRAPI-30 (Sporadic NullPointerException during SASL bind).
      * Tests multiple connect/bind/unbind/disconnect.
      */
     @Ignore("Activate when DIRAPI-30 is solved")
     @Test
     public void testSequentialBinds() throws Exception
     {
         LdapNetworkConnection connection;
         BindResponse resp;
         Entry entry;
         DN userDn = new DN( "uid=hnelson,ou=users,dc=example,dc=com" );
 
         for ( int i = 0; i < 1000; i++ )
         {
             System.out.println( "try " + i );

             // Digest-MD5
             connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
             resp = connection.bindDigestMd5( userDn.getName(), "secret", null, ldapServer.getSaslRealms()
                 .get( 0 ) );
             assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
             entry = connection.lookup( userDn );
             assertEquals( "hnelson", entry.get( "uid" ).getString() );
             connection.close();
 
             // Cram-MD5
             connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
             resp = connection.bindCramMd5( userDn.getName(), "secret", null );
             assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
             entry = connection.lookup( userDn );
             assertEquals( "hnelson", entry.get( "uid" ).getString() );
             connection.close();
 
             // GSSAPI
             connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
             resp = connection.bindGssApi( userDn.getName(), "secret", ldapServer.getSaslRealms().get( 0 )
                 .toUpperCase(), "localhost", 6088 );
             assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
             entry = connection.lookup( userDn );
             assertEquals( "hnelson", entry.get( "uid" ).getString() );
             connection.close();
         }
     }
 
     /**
      * A NTLM client
      */
     class NtlmSaslBindClient extends SocketClient
     {
         private final Logger LOG = LoggerFactory.getLogger( NtlmSaslBindClient.class );
 
         private final String mechanism;
 
 
         NtlmSaslBindClient( String mechanism ) throws Exception
         {
             this.mechanism = mechanism;
             setDefaultPort( ldapServer.getPort() );
             connect( "localhost", ldapServer.getPort() );
             setTcpNoDelay( false );
 
             LOG.debug( "isConnected() = {}", isConnected() );
             LOG.debug( "LocalPort     = {}", getLocalPort() );
             LOG.debug( "LocalAddress  = {}", getLocalAddress() );
             LOG.debug( "RemotePort    = {}", getRemotePort() );
             LOG.debug( "RemoteAddress = {}", getRemoteAddress() );
         }
 
 
         BindResponse bindType1( byte[] type1response ) throws Exception
         {
             if ( !isConnected() )
             {
                 throw new IllegalStateException( "Client is not connected." );
             }
 
             // Setup the bind request
             BindRequestImpl request = new BindRequestImpl( 1 );
             request.setName( new DN( "uid=admin,ou=system" ) );
             request.setSimple( false );
             request.setCredentials( type1response );
             request.setSaslMechanism( mechanism );
             request.setVersion3( true );
 
             // Setup the ASN1 Encoder and Decoder
             MessageDecoder decoder = new MessageDecoder( new BinaryAttributeDetector()
             {
                 public boolean isBinary( String attributeId )
                 {
                     return false;
                 }
             } );
 
             // Send encoded request to server
             LdapEncoder encoder = new LdapEncoder();
             ByteBuffer bb = encoder.encodeMessage( request );
 
             bb.flip();
 
             _output_.write( bb.array() );
             _output_.flush();
 
             while ( _input_.available() <= 0 )
             {
                 Thread.sleep( 100 );
             }
 
             // Retrieve the response back from server to my last request.
             return ( BindResponse ) decoder.decode( null, _input_ );
         }
 
 
         BindResponse bindType3( byte[] type3response ) throws Exception
         {
             if ( !isConnected() )
             {
                 throw new IllegalStateException( "Client is not connected." );
             }
 
             // Setup the bind request
             BindRequestImpl request = new BindRequestImpl( 2 );
             request.setName( new DN( "uid=admin,ou=system" ) );
             request.setSimple( false );
             request.setCredentials( type3response );
             request.setSaslMechanism( mechanism );
             request.setVersion3( true );
 
             // Setup the ASN1 Enoder and Decoder
             MessageDecoder decoder = new MessageDecoder( new BinaryAttributeDetector()
             {
                 public boolean isBinary( String attributeId )
                 {
                     return false;
                 }
             } );
 
             // Send encoded request to server
             LdapEncoder encoder = new LdapEncoder();
             ByteBuffer bb = encoder.encodeMessage( request );
             bb.flip();
 
             _output_.write( bb.array() );
             _output_.flush();
 
             while ( _input_.available() <= 0 )
             {
                 Thread.sleep( 100 );
             }
 
             // Retrieve the response back from server to my last request.
             return ( BindResponse ) decoder.decode( null, _input_ );
         }
     }
 
 
     private BogusNtlmProvider getNtlmProviderUsingReflection()
     {
         BogusNtlmProvider provider = null;
         try
         {
             NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get(
                 SupportedSaslMechanisms.NTLM );
 
             // there is no getter for 'provider' field hence this hack
             Field field = ntlmHandler.getClass().getDeclaredField( "provider" );
             field.setAccessible( true );
             provider = ( BogusNtlmProvider ) field.get( ntlmHandler );
         }
         catch ( Exception e )
         {
             e.printStackTrace();
         }
 
         return provider;
     }
 
     
     ////////////////////////
     protected Entry getPrincipalAttributes( String dn, String sn, String cn, String uid, String userPassword, String principal ) throws LdapException
     {
         Entry entry = new DefaultEntry( new DN( dn ) );
         entry.add( SchemaConstants.OBJECT_CLASS_AT, "person", "inetOrgPerson", "krb5principal", "krb5kdcentry" );
         entry.add( SchemaConstants.CN_AT, cn );
         entry.add( SchemaConstants.SN_AT, sn );
         entry.add( SchemaConstants.UID_AT, uid );
         entry.add( SchemaConstants.USER_PASSWORD_AT, userPassword );
         entry.add( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal );
         entry.add( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, "0" );
 
         return entry;
     }
 
 }
