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
 package org.apache.directory.server.core.authz.support;
 
 
 import org.apache.directory.server.core.CoreSession;
 import org.apache.directory.server.core.DefaultCoreSession;
 import org.apache.directory.server.core.DefaultDirectoryService;
 import org.apache.directory.server.core.DirectoryService;
 import org.apache.directory.server.core.OperationManager;
 import org.apache.directory.server.core.ReferralHandlingMode;
 import org.apache.directory.server.core.authn.LdapPrincipal;
 import org.apache.directory.server.core.changelog.ChangeLog;
 import org.apache.directory.server.core.cursor.Cursor;
 import org.apache.directory.server.core.cursor.CursorIterator;
 import org.apache.directory.server.core.entry.ClonedServerEntry;
 import org.apache.directory.server.core.entry.DefaultServerEntry;
 import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.event.EventService;
 import org.apache.directory.server.core.filtering.EntryFilteringCursor;
 import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
 import org.apache.directory.server.core.interceptor.Interceptor;
 import org.apache.directory.server.core.interceptor.InterceptorChain;
 import org.apache.directory.server.core.interceptor.context.AddOperationContext;
 import org.apache.directory.server.core.interceptor.context.BindOperationContext;
 import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
 import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
 import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
 import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
 import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
 import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
 import org.apache.directory.server.core.interceptor.context.ListOperationContext;
 import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
 import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
 import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
 import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
 import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
 import org.apache.directory.server.core.interceptor.context.OperationContext;
 import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
 import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
 import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
 import org.apache.directory.server.core.partition.Partition;
 import org.apache.directory.server.core.partition.PartitionNexus;
 import org.apache.directory.server.core.schema.SchemaOperationControl;
 import org.apache.directory.server.core.schema.SchemaService;
 import org.apache.directory.server.schema.registries.Registries;
 import org.apache.directory.shared.ldap.NotImplementedException;
 import org.apache.directory.shared.ldap.aci.ACITuple;
 import org.apache.directory.shared.ldap.aci.MicroOperation;
 import org.apache.directory.shared.ldap.aci.ProtectedItem;
 import org.apache.directory.shared.ldap.aci.UserClass;
 import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
 import org.apache.directory.shared.ldap.entry.Modification;
 import org.apache.directory.shared.ldap.ldif.LdifEntry;
 import org.apache.directory.shared.ldap.name.LdapDN;
 
 import javax.naming.NamingException;
 import javax.naming.ldap.Control;
 import javax.naming.ldap.LdapContext;
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Set;
 
 import org.junit.Test;
 import org.junit.BeforeClass;
 import static org.junit.Assert.assertEquals;
 
 
 /**
  * Tests {@link MaxImmSubFilter}.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$, $Date$
  */
 public class MaxImmSubFilterTest
 {
     private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
     private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
     private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );
 
     private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );
 
     private static final LdapDN ROOTDSE_NAME = new LdapDN();
     private static LdapDN ENTRY_NAME;
     private static Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
     private static ServerEntry ENTRY;
     
     /** A reference to the directory service */
     private static DirectoryService service;
 
     
     @BeforeClass public static void setup() throws NamingException
     {
         service = new DefaultDirectoryService();
 
         ENTRY_NAME = new LdapDN( "ou=test, ou=system" );
         PROTECTED_ITEMS.add( new ProtectedItem.MaxImmSub( 2 ) );
         ENTRY = new DefaultServerEntry( service.getRegistries(), ENTRY_NAME );
     }
 
 
     @Test public void testWrongScope() throws Exception
     {
         MaxImmSubFilter filter = new MaxImmSubFilter();
         Collection<ACITuple> tuples = new ArrayList<ACITuple>();
         tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
             EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );
 
         tuples = Collections.unmodifiableCollection( tuples );
 
         assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null, null,
             null, ENTRY_NAME, null, null, ENTRY, null, null ) );
 
         assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
             null, null, ENTRY_NAME, null, null, ENTRY, null, null ) );
     }
 
 
     @Test public void testRootDSE() throws Exception
     {
         MaxImmSubFilter filter = new MaxImmSubFilter();
 
         Collection<ACITuple> tuples = new ArrayList<ACITuple>();
         tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
             EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );
 
         tuples = Collections.unmodifiableCollection( tuples );
 
         assertEquals( tuples, filter.filter( null, tuples, OperationScope.ENTRY, null, null, null, null, null,
             ROOTDSE_NAME, null, null, ENTRY, null, null ) );
     }
 
 
     @Test public void testZeroTuple() throws Exception
     {
         MaxImmSubFilter filter = new MaxImmSubFilter();
 
         assertEquals( 0, filter.filter( null, EMPTY_ACI_TUPLE_COLLECTION, OperationScope.ENTRY, null, null, null, null, null,
             ENTRY_NAME, null, null, ENTRY, null, null ).size() );
     }
 
 
     @Test public void testDenialTuple() throws Exception
     {
         MaxImmSubFilter filter = new MaxImmSubFilter();
         Collection<ACITuple> tuples = new ArrayList<ACITuple>();
         tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
             PROTECTED_ITEMS, EMPTY_MICRO_OPERATION_SET, false, 0 ) );
 
         tuples = Collections.unmodifiableCollection( tuples );
 
         assertEquals( tuples, filter.filter( null, tuples, OperationScope.ENTRY, null, null, null, null, null,
             ENTRY_NAME, null, null, ENTRY, null, null ) );
     }
 
 
     @Test public void testGrantTuple() throws Exception
     {
         MaxImmSubFilter filter = new MaxImmSubFilter();
         Collection<ACITuple> tuples = new ArrayList<ACITuple>();
         tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
             PROTECTED_ITEMS, EMPTY_MICRO_OPERATION_SET, true, 0 ) );
 
         assertEquals( 1, filter.filter( null, tuples, OperationScope.ENTRY, new MockOperation( 1 ), null, null, null,
             null, ENTRY_NAME, null, null, ENTRY, null, null ).size() );
 
         assertEquals( 0, filter.filter( null, tuples, OperationScope.ENTRY, new MockOperation( 3 ), null, null, null,
             null, ENTRY_NAME, null, null, ENTRY, null, null ).size() );
     }
 
     
     class MockOperation implements OperationContext
     {
         final int count;
         final CoreSession session; 
 
 
         public MockOperation( int count ) throws Exception 
         {
             this.count = count;
             this.session = new DefaultCoreSession( new LdapPrincipal( new LdapDN(), AuthenticationLevel.STRONG ), 
                 new MockDirectoryService( count ) );
         }
 
 
         public EntryFilteringCursor search( SearchOperationContext opContext )
             throws NamingException
         {
             return new BaseEntryFilteringCursor( new BogusCursor( count ), opContext );
         }
 
 
         public EntryFilteringCursor search( SearchOperationContext opContext, Collection<String> bypass ) throws NamingException
         {
             return new BaseEntryFilteringCursor( new BogusCursor( count ), opContext );
         }
 
 
         public void addRequestControl( Control requestControl )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void addRequestControls( Control[] requestControls )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void addResponseControl( Control responseControl )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public Collection<String> getByPassed()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public LdapDN getDn()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public String getName()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public Control getRequestControl( String numericOid )
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public Control getResponseControl( String numericOid )
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public int getResponseControlCount()
         {
             // TODO Auto-generated method stub
             return 0;
         }
 
 
         public Control[] getResponseControls()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public CoreSession getSession()
         {
             return session;
         }
 
 
         public boolean hasBypass()
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean hasRequestControl( String numericOid )
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean hasRequestControls()
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean hasResponseControl( String numericOid )
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean hasResponseControls()
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean isBypassed( String interceptorName )
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public boolean isCollateralOperation()
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public ClonedServerEntry lookup( LdapDN dn, Collection<String> bypass ) throws Exception
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws Exception
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public LookupOperationContext newLookupContext( LdapDN dn )
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public void setByPassed( Collection<String> byPassed )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void setCollateralOperation( boolean collateralOperation )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void setDn( LdapDN dn )
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public LdapPrincipal getEffectivePrincipal()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public OperationContext getFirstOperation()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public OperationContext getLastOperation()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public OperationContext getNextOperation()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public OperationContext getPreviousOperation()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public boolean isFirstOperation()
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public void add( ServerEntry entry, Collection<String> bypass ) throws Exception
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void delete( LdapDN dn, Collection<String> bypass ) throws Exception
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public void modify( LdapDN dn, List<Modification> mods, Collection<String> bypass ) throws Exception
         {
             // TODO Auto-generated method stub
             
         }
 
 
         public boolean hasEntry( LdapDN dn, Collection<String> byPass ) throws Exception
         {
             // TODO Auto-generated method stub
             return false;
         }
 
 
         public ReferralHandlingMode getReferralHandlingMode()
         {
             // TODO Auto-generated method stub
             return null;
         }
 
 
         public void setReferralHandlingMode( ReferralHandlingMode referralHandlingMode )
         {
             // TODO Auto-generated method stub
             
         }
     }
 
     class MockDirectoryService implements DirectoryService
     {
         int count;
         
         
         public MockDirectoryService( int count )
         {
             this.count = count;
         }
         
         public Hashtable<String, Object> getEnvironment()
         {
             return null;
         }
 
 
         public void setEnvironment( Hashtable<String, Object> environment )
         {
         }
 
 
         public long revert( long revision ) throws NamingException
         {
             return 0;
         }
 
 
         public long revert() throws NamingException
         {
             return 0;
         }
 
 
         public PartitionNexus getPartitionNexus()
         {
             return null;
         }
 
 
         public InterceptorChain getInterceptorChain()
         {
             return null;
         }
 
 
         public void addPartition( Partition partition ) throws NamingException
         {
         }
 
 
         public void removePartition( Partition partition ) throws NamingException
         {
         }
 
 
         public Registries getRegistries()
         {
             return null;
         }
 
 
         public void setRegistries( Registries registries )
         {
         }
 
 
         public SchemaService getSchemaService()
         {
             return null;
         }
 
 
         public void setSchemaService( SchemaService schemaService )
         {
 
         }
 
 
         public SchemaOperationControl getSchemaManager()
         {
             return null;
         }
 
 
         public void setSchemaManager( SchemaOperationControl schemaManager )
         {
         }
 
 
         public void startup() throws NamingException
         {
         }
 
 
         public void shutdown() throws NamingException
         {
         }
 
 
         public void sync() throws NamingException
         {
         }
 
 
         public boolean isStarted()
         {
             return true;
         }
 
 
         public LdapContext getJndiContext() throws NamingException
         {
             return null;
         }
 
 
         public DirectoryService getDirectoryService()
         {
             return null;
         }
 
 
         public void setInstanceId( String instanceId )
         {
 
         }
 
 
         public String getInstanceId()
         {
             return null;
         }
 
 
         public Set<? extends Partition> getPartitions()
         {
             return null;
         }
 
 
         public void setPartitions( Set<? extends Partition> partitions )
         {
         }
 
 
         public boolean isAccessControlEnabled()
         {
             return false;
         }
 
 
         public void setAccessControlEnabled( boolean accessControlEnabled )
         {
         }
 
 
         public boolean isAllowAnonymousAccess()
         {
             return false;
         }
 
 
         public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
         {
 
         }
 
 
         public List<Interceptor> getInterceptors()
         {
             return null;
         }
 
 
         public void setInterceptors( List<Interceptor> interceptors )
         {
 
         }
 
 
         public List<LdifEntry> getTestEntries()
         {
             return null;
         }
 
 
         public void setTestEntries( List<? extends LdifEntry> testEntries )
         {
         }
 
 
         public File getWorkingDirectory()
         {
             return null;
         }
 
 
         public void setWorkingDirectory( File workingDirectory )
         {
         }
 
 
         public void validate()
         {
         }
 
 
         public void setShutdownHookEnabled( boolean shutdownHookEnabled )
         {
 
         }
 
 
         public boolean isShutdownHookEnabled()
         {
             return false;
         }
 
 
         public void setExitVmOnShutdown( boolean exitVmOnShutdown )
         {
 
         }
 
 
         public boolean isExitVmOnShutdown()
         {
             return false;
         }
 
 
         public void setMaxSizeLimit( int maxSizeLimit )
         {
 
         }
 
 
         public int getMaxSizeLimit()
         {
             return 0;
         }
 
 
         public void setMaxTimeLimit( int maxTimeLimit )
         {
 
         }
 
 
         public int getMaxTimeLimit()
         {
             return 0;
         }
 
 
         public void setSystemPartition( Partition systemPartition )
         {
 
         }
 
 
         public Partition getSystemPartition()
         {
             return null;
         }
 
 
         public boolean isDenormalizeOpAttrsEnabled()
         {
             return false;
         }
 
 
         public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
         {
 
         }
         
         public void setChangeLog( ChangeLog changeLog )
         {
             
         }
         
         public ChangeLog getChangeLog()
         {
             return null;
         }
 
 
         public ServerEntry newEntry( LdapDN dn ) throws NamingException
         {
             return null;
         }
         
         public ServerEntry newEntry( String ldif, String dn )
         {
             return null;
         }
 
 
         public OperationManager getOperationManager()
         {
             return new MockOperationManager( count );
         }
 
 
         public CoreSession getSession() throws Exception
         {
             return null;
         }
 
 
         public CoreSession getSession( LdapPrincipal principal ) throws Exception
         {
             return null;
         }
 
 
         public CoreSession getSession( LdapDN principalDn, byte[] credentials ) throws Exception
         {
             return null;
         }
 
         
         public CoreSession getSession( LdapDN principalDn, byte[] credentials, String saslMechanism, String saslAuthId )
             throws Exception
         {
             return null;
         }
 
         public CoreSession getAdminSession() throws Exception
         {
             // TODO Auto-generated method stub
             return null;
         }

        public EventService getEventService()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void setEventService( EventService eventService )
        {
            // TODO Auto-generated method stub
            
        }
     }
 
     
     class MockOperationManager implements OperationManager
     {
         int count;
         
         public MockOperationManager( int count )
         {
             this.count = count;
         }
         
         public void add( AddOperationContext opContext ) throws Exception
         {
         }
 
         
         public void bind( BindOperationContext opContext ) throws Exception
         {
         }
 
         
         public boolean compare( CompareOperationContext opContext ) throws Exception
         {
             return false;
         }
 
 
         public void delete( DeleteOperationContext opContext ) throws Exception
         {
         }
 
         public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public boolean hasEntry( EntryOperationContext opContext ) throws Exception
         {
             return false;
         }
 
         public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public Iterator<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
         {
             return null;
         }
 
         public void modify( ModifyOperationContext opContext ) throws Exception
         {
         }
 
         public void move( MoveOperationContext opContext ) throws Exception
         {
         }
 
         public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
         {
         }
 
         public void rename( RenameOperationContext opContext ) throws Exception
         {
         }
 
         public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
         {
             return new BaseEntryFilteringCursor( new BogusCursor( count ), opContext );
         }
 
 
         public void unbind( UnbindOperationContext opContext ) throws Exception
         {
         }
     }
     
 
     class BogusCursor implements Cursor<ServerEntry>
     {
         final int count;
         int ii;
 
 
         public BogusCursor(int count)
         {
             this.count = count;
         }
 
 
         public boolean available() 
         {
             return ii < count;
         }
 
 
         public void close() throws NamingException
         {
             ii = count;
         }
 
 
         public boolean hasMoreElements()
         {
             return ii < count;
         }
 
 
         public Object nextElement()
         {
             if ( ii >= count )
             {
                 throw new NoSuchElementException();
             }
 
             ii++;
             
             return new Object();
         }
 
 
         public void after( ServerEntry element ) throws Exception
         {
         }
 
 
         public void afterLast() throws Exception
         {
         }
 
 
         public void before( ServerEntry element ) throws Exception
         {
             throw new NotImplementedException();
         }
 
 
         public void beforeFirst() throws Exception
         {
             ii = -1;
         }
 
 
         public boolean first() throws Exception
         {
             ii = 0;
             return ii < count;
         }
 
 
         public ServerEntry get() throws Exception
         {
             return new DefaultServerEntry( service.getRegistries() );
         }
 
 
         public boolean isClosed() throws Exception
         {
             return false;
         }
 
 
         public boolean isElementReused()
         {
             return false;
         }
 
 
         public boolean last() throws Exception
         {
             ii = count;
             return true;
         }
 
 
         public boolean next() 
         {
             if ( ii >= count )
             {
                 return false;
             }
 
             ii++;
             
             return true;
         }
 
 
         public boolean previous() throws Exception
         {
             if ( ii < 0 )
             {
                 return false;
             }
             
             ii--;
             return true;
         }
 
 
         public Iterator<ServerEntry> iterator()
         {
             return new CursorIterator<ServerEntry>( this );
         }
     }
 }
