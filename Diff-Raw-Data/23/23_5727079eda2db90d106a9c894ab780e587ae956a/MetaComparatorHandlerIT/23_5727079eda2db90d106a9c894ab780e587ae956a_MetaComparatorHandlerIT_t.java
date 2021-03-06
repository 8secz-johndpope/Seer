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
 package org.apache.directory.server.core.schema;
 
 
 import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
 import static org.junit.Assert.fail;
 
 import java.io.ByteArrayOutputStream;
 import java.io.InputStream;
 
 import javax.naming.NamingException;
 import javax.naming.directory.Attribute;
 import javax.naming.directory.Attributes;
 import javax.naming.directory.BasicAttribute;
 import javax.naming.directory.BasicAttributes;
 import javax.naming.directory.DirContext;
 import javax.naming.directory.ModificationItem;
 
 import org.apache.directory.server.core.DirectoryService;
 import org.apache.directory.server.core.integ.CiRunner;
 import org.apache.directory.server.core.integ.Level;
 import org.apache.directory.server.core.integ.annotations.CleanupLevel;
 import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
 import org.apache.directory.shared.ldap.constants.SchemaConstants;
 import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
 import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
 import org.apache.directory.shared.ldap.message.ResultCodeEnum;
 import org.apache.directory.shared.ldap.name.LdapDN;
 import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.comparators.BooleanComparator;
 import org.apache.directory.shared.ldap.schema.comparators.StringComparator;
 import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;
 import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
 import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
 import org.junit.Ignore;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 
 
 /**
  * A test case which tests the addition of various schema elements
  * to the ldap server.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
 @RunWith ( CiRunner.class )
 @CleanupLevel( Level.CLASS )
 public class MetaComparatorHandlerIT
 {
     private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
     private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";
 
 
     public static DirectoryService service;
 
     
     /**
      * Gets relative DN to ou=schema.
      *
      * @param schemaName the name of the schema
      * @return the dn to the ou underwhich comparators are found for a schmea
      * @throws Exception if there are dn construction issues
      */
     private LdapDN getComparatorContainer( String schemaName ) throws Exception
     {
         return new LdapDN( "ou=comparators,cn=" + schemaName );
     }
 
 
     private static ComparatorRegistry getComparatorRegistry()
     {
         return service.getRegistries().getComparatorRegistry();
     }
     
 
     private static MatchingRuleRegistry getMatchingRuleRegistry()
     {
         return service.getRegistries().getMatchingRuleRegistry();
     }
 
 
     private static OidRegistry getOidRegistry()
     {
         return service.getRegistries().getOidRegistry();
     }
 
 
     // ----------------------------------------------------------------------
     // Test all core methods with normal operational pathways
     // ----------------------------------------------------------------------
 
 
     @Test
     public void testAddComparator() throws Exception
     {
         Attributes attrs = new BasicAttributes( true );
         Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
         oc.add( MetaSchemaConstants.META_TOP_OC );
         oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
         attrs.put( oc );
         attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
         attrs.put( MetaSchemaConstants.M_OID_AT, OID );
         attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         getSchemaContext( service ).createSubcontext( dn, attrs );
         
         assertTrue( getComparatorRegistry().contains( OID ) );
         assertEquals( getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
         Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
         assertEquals( clazz, StringComparator.class );
     }
     
 
     @Test
     public void testAddComparatorWithByteCode() throws Exception
     {
         InputStream in = getClass().getResourceAsStream( "DummyComparator.bytecode" );
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         
         while ( in.available() > 0 )
         {
             out.write( in.read() );
         }
         
         Attributes attrs = new BasicAttributes( true );
         Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
         oc.add( MetaSchemaConstants.META_TOP_OC );
         oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
         attrs.put( oc );
         attrs.put( MetaSchemaConstants.M_FQCN_AT, "org.apache.directory.shared.ldap.schema.comparators.DummyComparator" );
         attrs.put( MetaSchemaConstants.M_BYTECODE_AT, out.toByteArray() );
         attrs.put( MetaSchemaConstants.M_OID_AT, OID );
         attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         getSchemaContext( service ).createSubcontext( dn, attrs );
         
         assertTrue( getComparatorRegistry().contains( OID ) );
         assertEquals( getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
         Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
         assertEquals( clazz.getName(), "org.apache.directory.shared.ldap.schema.comparators.DummyComparator" );
     }
     
 
     @Test
     public void testDeleteComparator() throws Exception
     {
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         testAddComparator();
         
         getSchemaContext( service ).destroySubcontext( dn );
 
         assertFalse( "comparator should be removed from the registry after being deleted", 
             getComparatorRegistry().contains( OID ) );
         
         try
         {
             getComparatorRegistry().lookup( OID );
             fail( "comparator lookup should fail after deleting the comparator" );
         }
         catch( NamingException e )
         {
         }
     }
 
 
     @Test
     public void testRenameComparator() throws Exception
     {
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         testAddComparator();
         
         LdapDN newdn = getComparatorContainer( "apachemeta" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
         getSchemaContext( service ).rename( dn, newdn );
 
         assertFalse( "old comparator OID should be removed from the registry after being renamed", 
             getComparatorRegistry().contains( OID ) );
         
         try
         {
             getComparatorRegistry().lookup( OID );
             fail( "comparator lookup should fail after deleting the comparator" );
         }
         catch( NamingException e )
         {
         }
 
         assertTrue( getComparatorRegistry().contains( NEW_OID ) );
         Class<?> clazz = getComparatorRegistry().lookup( NEW_OID ).getClass();
         assertEquals( clazz, StringComparator.class );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparator() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN newdn = getComparatorContainer( "apache" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         getSchemaContext( service ).rename( dn, newdn );
 
         assertTrue( "comparator OID should still be present", 
             getComparatorRegistry().contains( OID ) );
         
         assertEquals( "comparator schema should be set to apache not apachemeta", 
             getComparatorRegistry().getSchemaName( OID ), "apache" );
 
         Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
         assertEquals( clazz, StringComparator.class );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparatorAndChangeRdn() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN newdn = getComparatorContainer( "apache" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
         
         getSchemaContext( service ).rename( dn, newdn );
 
         assertFalse( "old comparator OID should NOT be present", 
             getComparatorRegistry().contains( OID ) );
         
         assertTrue( "new comparator OID should be present", 
             getComparatorRegistry().contains( NEW_OID ) );
         
         assertEquals( "comparator with new oid should have schema set to apache NOT apachemeta", 
             getComparatorRegistry().getSchemaName( NEW_OID ), "apache" );
 
         Class<?> clazz = getComparatorRegistry().lookup( NEW_OID ).getClass();
         assertEquals( clazz, StringComparator.class );
     }
 
 
     @Test
     public void testModifyComparatorWithModificationItems() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( MetaSchemaConstants.M_FQCN_AT, BooleanComparator.class.getName() );
         mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
         getSchemaContext( service ).modifyAttributes( dn, mods );
 
         assertTrue( "comparator OID should still be present", 
             getComparatorRegistry().contains( OID ) );
         
         assertEquals( "comparator schema should be set to apachemeta", 
             getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
 
         Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BooleanComparator.class );
     }
 
 
     @Test
     public void testModifyComparatorWithAttributes() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         Attributes mods = new BasicAttributes( true );
        mods.put( MetaSchemaConstants.M_FQCN_AT, BooleanComparator.class.getName() );
         getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );
 
         assertTrue( "comparator OID should still be present", 
             getComparatorRegistry().contains( OID ) );
         
         assertEquals( "comparator schema should be set to apachemeta", 
             getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
 
         Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BooleanComparator.class );
     }
     
 
     // ----------------------------------------------------------------------
     // Test move, rename, and delete when a MR exists and uses the Comparator
     // ----------------------------------------------------------------------
 
     
     @Test
     public void testDeleteComparatorWhenInUse() throws Exception
     {
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         testAddComparator();
         getMatchingRuleRegistry().register( new DummyMR() );
         
         try
         {
             getSchemaContext( service ).destroySubcontext( dn );
             fail( "should not be able to delete a comparator in use" );
         }
         catch( LdapOperationNotSupportedException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
         }
 
         assertTrue( "comparator should still be in the registry after delete failure", 
             getComparatorRegistry().contains( OID ) );
         getMatchingRuleRegistry().unregister( OID );
         getOidRegistry().unregister( OID );
     }
     
     
     @Test
     @Ignore
     public void testMoveComparatorWhenInUse() throws Exception
     {
         testAddComparator();
         getMatchingRuleRegistry().register( new DummyMR() );
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN newdn = getComparatorContainer( "apache" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         try
         {
             getSchemaContext( service ).rename( dn, newdn );
             fail( "should not be able to move a comparator in use" );
         }
         catch( LdapOperationNotSupportedException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
         }
 
         assertTrue( "comparator should still be in the registry after move failure", 
             getComparatorRegistry().contains( OID ) );
         getMatchingRuleRegistry().unregister( OID );
         getOidRegistry().unregister( OID );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparatorAndChangeRdnWhenInUse() throws Exception
     {
         testAddComparator();
         getMatchingRuleRegistry().register( new DummyMR() );
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN newdn = getComparatorContainer( "apache" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
         
         try
         {
             getSchemaContext( service ).rename( dn, newdn );
             fail( "should not be able to move a comparator in use" );
         }
         catch( LdapOperationNotSupportedException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
         }
 
         assertTrue( "comparator should still be in the registry after move failure", 
             getComparatorRegistry().contains( OID ) );
         getMatchingRuleRegistry().unregister( OID );
         getOidRegistry().unregister( OID );
     }
 
     
     @Test
     public void testRenameComparatorWhenInUse() throws Exception
     {
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         testAddComparator();
         getMatchingRuleRegistry().register( new DummyMR() );
         
         LdapDN newdn = getComparatorContainer( "apachemeta" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
         
         try
         {
             getSchemaContext( service ).rename( dn, newdn );
             fail( "should not be able to rename a comparator in use" );
         }
         catch( LdapOperationNotSupportedException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
         }
 
         assertTrue( "comparator should still be in the registry after rename failure", 
             getComparatorRegistry().contains( OID ) );
         getMatchingRuleRegistry().unregister( OID );
         getOidRegistry().unregister( OID );
     }
 
 
     // ----------------------------------------------------------------------
     // Let's try some freaky stuff
     // ----------------------------------------------------------------------
 
 
     @Test
     @Ignore
     public void testMoveComparatorToTop() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN top = new LdapDN();
         top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         try
         {
             getSchemaContext( service ).rename( dn, top );
             fail( "should not be able to move a comparator up to ou=schema" );
         }
         catch( LdapInvalidNameException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
         }
 
         assertTrue( "comparator should still be in the registry after move failure", 
             getComparatorRegistry().contains( OID ) );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparatorToNormalizers() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         LdapDN newdn = new LdapDN( "ou=normalizers,cn=apachemeta" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         try
         {
             getSchemaContext( service ).rename( dn, newdn );
             fail( "should not be able to move a comparator up to normalizers container" );
         }
         catch( LdapInvalidNameException e ) 
         {
             assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
         }
 
         assertTrue( "comparator should still be in the registry after move failure", 
             getComparatorRegistry().contains( OID ) );
     }
     
     
     @Test
     public void testAddComparatorToDisabledSchema() throws Exception
     {
         Attributes attrs = new BasicAttributes( true );
         Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
         oc.add( MetaSchemaConstants.META_TOP_OC );
         oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
         attrs.put( oc );
         attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
         attrs.put( MetaSchemaConstants.M_OID_AT, OID );
         attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
         
         // nis is by default inactive
         LdapDN dn = getComparatorContainer( "nis" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         getSchemaContext( service ).createSubcontext( dn, attrs );
         
         assertFalse( "adding new comparator to disabled schema should not register it into the registries", 
             getComparatorRegistry().contains( OID ) );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparatorToDisabledSchema() throws Exception
     {
         testAddComparator();
         
         LdapDN dn = getComparatorContainer( "apachemeta" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         // nis is inactive by default
         LdapDN newdn = getComparatorContainer( "nis" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         getSchemaContext( service ).rename( dn, newdn );
 
         assertFalse( "comparator OID should no longer be present", 
             getComparatorRegistry().contains( OID ) );
     }
 
 
     @Test
     @Ignore
     public void testMoveComparatorToEnabledSchema() throws Exception
     {
         testAddComparatorToDisabledSchema();
         
         // nis is inactive by default
         LdapDN dn = getComparatorContainer( "nis" );
         dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
 
         assertFalse( "comparator OID should NOT be present when added to disabled nis schema", 
             getComparatorRegistry().contains( OID ) );
 
         LdapDN newdn = getComparatorContainer( "apachemeta" );
         newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
         
         getSchemaContext( service ).rename( dn, newdn );
 
         assertTrue( "comparator OID should be present when moved to enabled schema", 
             getComparatorRegistry().contains( OID ) );
         
         assertEquals( "comparator should be in apachemeta schema after move", 
             getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
     }
 
 
     class DummyMR extends MatchingRule
     {
         public DummyMR()
         {
             super( OID );
             addName( "dummy" );
         }
 
         private static final long serialVersionUID = 1L;
     }
 }
