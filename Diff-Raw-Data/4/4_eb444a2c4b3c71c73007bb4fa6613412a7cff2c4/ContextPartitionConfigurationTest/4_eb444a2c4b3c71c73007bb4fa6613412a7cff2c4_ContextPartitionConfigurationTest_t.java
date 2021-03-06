 
 /*
  *   Copyright 2004 The Apache Software Foundation
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
 package org.apache.ldap.server.configuration;
 
 
 import java.util.Hashtable;
 
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NameNotFoundException;
 import javax.naming.directory.Attributes;
 import javax.naming.directory.BasicAttributes;
 
 import junit.framework.Assert;
 
 import org.apache.ldap.server.AbstractAdminTestCase;
 import org.apache.ldap.server.jndi.CoreContextFactory;
 import org.apache.ldap.server.partition.impl.btree.jdbm.JdbmContextPartition;
 
 
 /**
 * Tests {@link AddContextPartitionConfiguration} and
 * {@link RemoveContextPartitionConfiguration} works correctly.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
 public class ContextPartitionConfigurationTest extends AbstractAdminTestCase
 {
     public ContextPartitionConfigurationTest()
     {
     }
 
     public void testAddAndRemove() throws Exception
     {
         MutableContextPartitionConfiguration partitionCfg =
             new MutableContextPartitionConfiguration();
         partitionCfg.setName( "removable" );
         partitionCfg.setSuffix( "ou=removable" );
         Attributes ctxEntry = new BasicAttributes();
         ctxEntry.put( "objectClass", "top" );
         ctxEntry.put( "ou", "removable" );
         partitionCfg.setContextEntry( ctxEntry );
         partitionCfg.setContextPartition( new JdbmContextPartition() );
         
         // Test AddContextPartition
         AddContextPartitionConfiguration addCfg =
             new AddContextPartitionConfiguration( partitionCfg );
         
         Hashtable env = new Hashtable();
         env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
         env.putAll( addCfg.toJndiEnvironment() );
         
         Context ctx = new InitialContext( env );
         Assert.assertNotNull( ctx.lookup( "ou=removable" ) );
         
         // Test removeContextPartition
         RemoveContextPartitionConfiguration removeCfg =
             new RemoveContextPartitionConfiguration( "ou=removable" );
         env.putAll( removeCfg.toJndiEnvironment() );
         
         ctx = new InitialContext( env );
         try
         {
             ctx.lookup( "ou=removable" );
             Assert.fail( "NameNotFoundException should be thrown." );
         }
         catch( NameNotFoundException e )
         {
             // Partition is removed.
         }
     }
 }
