 /*
  * Copyright 2010 Martin Grotzke
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
 package com.esotericsoftware.kryo;
 
 import static com.esotericsoftware.kryo.TestClasses.createPerson;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Currency;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.IdentityHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.apache.commons.lang.mutable.MutableInt;
 import org.testng.Assert;
 import org.testng.annotations.BeforeTest;
 import org.testng.annotations.DataProvider;
 import org.testng.annotations.Test;
 
 import com.esotericsoftware.kryo.TestClasses.ClassWithoutDefaultConstructor;
 import com.esotericsoftware.kryo.TestClasses.Container;
 import com.esotericsoftware.kryo.TestClasses.CounterHolder;
 import com.esotericsoftware.kryo.TestClasses.CounterHolderArray;
 import com.esotericsoftware.kryo.TestClasses.Email;
 import com.esotericsoftware.kryo.TestClasses.HashMapWithIntConstructorOnly;
 import com.esotericsoftware.kryo.TestClasses.Holder;
 import com.esotericsoftware.kryo.TestClasses.HolderArray;
 import com.esotericsoftware.kryo.TestClasses.HolderList;
 import com.esotericsoftware.kryo.TestClasses.MyContainer;
 import com.esotericsoftware.kryo.TestClasses.Person;
 import com.esotericsoftware.kryo.TestClasses.SomeInterface;
 import com.esotericsoftware.kryo.TestClasses.Person.Gender;
 
 /**
 * Test for {@link JavolutionTranscoder}
  *
  * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
  */
 public class KryoTest {
     
     private Kryo _kryo;
 
     @BeforeTest
     protected void beforeTest() {
         _kryo = new Kryo();
         _kryo.setAllowUnregisteredClasses( true );
         _kryo.setSerializer( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer( _kryo ) );
     }
 
     @Test( enabled = true )
     public void testStringBuffer() throws Exception {
         final StringBuffer stringBuffer = new StringBuffer( "<string\n&buffer/>" );
         final StringBuffer deserialized = deserialize( serialize( stringBuffer ), StringBuffer.class );
         assertDeepEquals( deserialized, stringBuffer );
     }
 
     @Test( enabled = true )
     public void testStringBuilder() throws Exception {
         final StringBuilder stringBuilder = new StringBuilder( "<string\n&buffer/>" );
         final StringBuilder deserialized = deserialize( serialize( stringBuilder ), StringBuilder.class );
         assertDeepEquals( deserialized, stringBuilder );
     }
     
     @Test( enabled = true )
     public void testMapWithIntConstructorOnly() throws Exception {
         final HashMapWithIntConstructorOnly map = new HashMapWithIntConstructorOnly( 5 );
         final HashMapWithIntConstructorOnly deserialized =
                 deserialize( serialize( map ), HashMapWithIntConstructorOnly.class );
         assertDeepEquals( deserialized, map );
 
     }
     
     @Test( enabled = true )
     public void testCurrency() throws Exception {
         final Currency currency = Currency.getInstance( "EUR" );
         final Currency deserialized =
                 deserialize( serialize( currency ), Currency.class );
         assertDeepEquals( deserialized, currency );
 
         // Check that the transient field defaultFractionDigits is initialized correctly
         Assert.assertEquals( deserialized.getCurrencyCode(), currency.getCurrencyCode() );
         Assert.assertEquals( deserialized.getDefaultFractionDigits(), currency.getDefaultFractionDigits() );
     }
     
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true )
     public void testJavaUtilCollectionsUnmodifiableList() throws Exception {
         final List<String> unmodifiableList = Collections.unmodifiableList( new ArrayList<String>( Arrays.asList( "foo", "bar" ) ) );
         final List<String> deserialized = deserialize( serialize( unmodifiableList ), List.class );
         assertDeepEquals( deserialized, unmodifiableList );
     }
     
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true )
     public void testJavaUtilCollectionsUnmodifiableMap() throws Exception {
         final HashMap<String, String> m = new HashMap<String, String>();
         m.put( "foo", "bar" );
         final Map<String, String> unmodifiableMap = Collections.unmodifiableMap( m );
         final Map<String, String> deserialized = deserialize( serialize( unmodifiableMap ), Map.class );
         assertDeepEquals( deserialized, unmodifiableMap );
     }
     
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true )
     public void testJavaUtilCollectionsEmptyList() throws Exception {
         final List<String> emptyList = Collections.<String>emptyList();
         final List<String> deserialized = deserialize( serialize( emptyList ), List.class );
         assertDeepEquals( deserialized, emptyList );
     }
     
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true )
     public void testJavaUtilCollectionsEmptyMap() throws Exception {
         final Map<String, String> emptyMap = Collections.<String, String>emptyMap();
         final Map<String, String> deserialized = deserialize( serialize( emptyMap ), Map.class );
         assertDeepEquals( deserialized, emptyMap );
     }
     
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true )
     public void testJavaUtilArraysAsList() throws Exception {
         final Holder<List<String>> asListHolder = new Holder( Arrays.asList( "foo", "bar" ) );
         final Holder<List<String>> deserialized = deserialize( serialize( asListHolder ), Holder.class );
         assertDeepEquals( deserialized, asListHolder );
     }
 
     @Test( enabled = true )
     public void testJdkProxy() throws Exception {
         final SomeInterface bean = TestClasses.createProxy();
         final SomeInterface deserialized = deserialize( serialize( bean ), SomeInterface.class );
         assertDeepEquals( deserialized, bean );
     }
 
     @Test( enabled = true )
     public void testInnerClass() throws Exception {
         final Container container = TestClasses.createContainer( "some content" );
         final Container deserialized = deserialize( serialize( container ), Container.class );
         assertDeepEquals( deserialized, container );
     }
 
     @Test( enabled = true )
     public <T> void testSharedObjectIdentity_CounterHolder() throws Exception {
 
         final AtomicInteger sharedObject = new AtomicInteger( 42 );
         final CounterHolder holder1 = new CounterHolder( sharedObject );
         final CounterHolder holder2 = new CounterHolder( sharedObject );
         final CounterHolderArray holderHolder = new CounterHolderArray( holder1, holder2 );
         
         final CounterHolderArray deserialized = deserialize( serialize( holderHolder ), CounterHolderArray.class );
         assertDeepEquals( deserialized, holderHolder );
         Assert.assertTrue( deserialized.holders[0].item == deserialized.holders[1].item );
 
     }
 
     @DataProvider( name = "sharedObjectIdentityProvider" )
     protected Object[][] createSharedObjectIdentityProviderData() {
         return new Object[][] {
                 { AtomicInteger.class.getSimpleName(), new AtomicInteger( 42 ) },
                 { Email.class.getSimpleName(), new Email( "foo bar", "foo.bar@example.com" ) } };
     }
 
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true, dataProvider = "sharedObjectIdentityProvider" )
     public <T> void testSharedObjectIdentityWithArray( final String name, final T sharedObject ) throws Exception {
         final Holder<T> holder1 = new Holder<T>( sharedObject );
         final Holder<T> holder2 = new Holder<T>( sharedObject );
         final HolderArray<T> holderHolder = new HolderArray<T>( holder1, holder2 );
         
         final HolderArray<T> deserialized = deserialize( serialize( holderHolder ), HolderArray.class );
         assertDeepEquals( deserialized, holderHolder );
         Assert.assertTrue( deserialized.holders[0].item == deserialized.holders[1].item );
     }
 
     @SuppressWarnings( "unchecked" )
     @Test( enabled = true, dataProvider = "sharedObjectIdentityProvider" )
     public <T> void testSharedObjectIdentity( final String name, final T sharedObject ) throws Exception {
         final Holder<T> holder1 = new Holder<T>( sharedObject );
         final Holder<T> holder2 = new Holder<T>( sharedObject );
         final HolderList<T> holderHolder = new HolderList<T>( new ArrayList<Holder<T>>( Arrays.asList( holder1, holder2 ) ) );
         
         final HolderList<T> deserialized = deserialize( serialize( holderHolder ), HolderList.class );
         assertDeepEquals( deserialized, holderHolder );
         Assert.assertTrue( deserialized.holders.get( 0 ).item == deserialized.holders.get( 1 ).item );
     }
 
     @DataProvider( name = "typesAsSessionAttributesProvider" )
     protected Object[][] createTypesAsSessionAttributesData() {
         return new Object[][] { { int.class, 42 },
                 { long.class, 42 },
                 { Boolean.class, Boolean.TRUE },
                 { String.class, "42" },
                 { StringBuilder.class, new StringBuilder( "42" ) },
                 { StringBuffer.class, new StringBuffer( "42" ) },
                 { Class.class, String.class },
                 { Long.class, new Long( 42 ) },
                 { Integer.class, new Integer( 42 ) },
                 { Character.class, new Character( 'c' ) },
                 { Byte.class, new Byte( "b".getBytes()[0] ) },
                 { Double.class, new Double( 42d ) },
                 { Float.class, new Float( 42f ) },
                 { Short.class, new Short( (short) 42 ) },
                 { BigDecimal.class, new BigDecimal( 42 ) },
                 { AtomicInteger.class, new AtomicInteger( 42 ) },
                 { AtomicLong.class, new AtomicLong( 42 ) },
                 { MutableInt.class, new MutableInt( 42 ) },
                 { Integer[].class, new Integer[] { 42 } },
                 { Date.class, new Date( System.currentTimeMillis() - 10000 ) },
                 { Calendar.class, Calendar.getInstance() },
                 { Currency.class, Currency.getInstance( "EUR" ) },
                 { ArrayList.class, new ArrayList<String>( Arrays.asList( "foo" ) ) },
                 { int[].class, new int[] { 1, 2 } },
                 { long[].class, new long[] { 1, 2 } },
                 { short[].class, new short[] { 1, 2 } },
                 { float[].class, new float[] { 1, 2 } },
                 { double[].class, new double[] { 1, 2 } },
                 { int[].class, new int[] { 1, 2 } },
                 { byte[].class, "42".getBytes() },
                 { char[].class, "42".toCharArray() },
                 { String[].class, new String[] { "23", "42" } },
                 { Person[].class, new Person[] { createPerson( "foo bar", Gender.MALE, 42 ) } } };
     }
 
     @Test( enabled = true, dataProvider = "typesAsSessionAttributesProvider" )
     public <T> void testTypesAsSessionAttributes( final Class<T> type, final T instance ) throws Exception {
         final T deserialized = deserialize( serialize( instance ), type );
         assertDeepEquals( deserialized, instance );
     }
 
     @Test( enabled = true )
     public void testTypesInContainerClass() throws Exception {
         final MyContainer myContainer = new MyContainer();
         final MyContainer deserialized = deserialize( serialize( myContainer ), MyContainer.class );
         assertDeepEquals( deserialized, myContainer );
     }
 
     @Test( enabled = true )
     public void testClassWithoutDefaultConstructor() throws Exception {
         final ClassWithoutDefaultConstructor obj = TestClasses.createClassWithoutDefaultConstructor( "foo" );
         final ClassWithoutDefaultConstructor deserialized = deserialize( serialize( obj ), ClassWithoutDefaultConstructor.class );
         assertDeepEquals( deserialized, obj );
     }
 
     @Test( enabled = true )
     public void testPrivateClass() throws Exception {
         final Holder<?> holder = new Holder<Object>( TestClasses.createPrivateClass( "foo" ) );
         final Holder<?> deserialized = deserialize( serialize( holder ), Holder.class );
         assertDeepEquals( deserialized, holder );
     }
 
     @Test( enabled = true )
     public void testCollections() throws Exception {
         final EntityWithCollections obj = new EntityWithCollections();
         final EntityWithCollections deserialized = deserialize( serialize( obj ), EntityWithCollections.class );
         assertDeepEquals( deserialized, obj );
     }
 
     @Test( enabled = true )
     public void testCyclicDependencies() throws Exception {
         final Person p1 = createPerson( "foo bar", Gender.MALE, 42, "foo.bar@example.org", "foo.bar@example.com" );
         final Person p2 = createPerson( "bar baz", Gender.FEMALE, 42, "bar.baz@example.org", "bar.baz@example.com" );
         p1.addFriend( p2 );
         p2.addFriend( p1 );
 
         final Person deserialized = deserialize( serialize( p1 ), Person.class );
         assertDeepEquals( deserialized, p1 );
     }
 
     public static class EntityWithCollections {
         private final String[] _bars;
         private final List<String> _foos;
         private final Map<String, Integer> _bazens;
 
         public EntityWithCollections() {
             _bars = new String[] { "foo", "bar" };
             _foos = new ArrayList<String>( Arrays.asList( "foo", "bar" ) );
             _bazens = new HashMap<String, Integer>();
             _bazens.put( "foo", 1 );
             _bazens.put( "bar", 2 );
         }
 
         @Override
         public int hashCode() {
             final int prime = 31;
             int result = 1;
             result = prime * result + Arrays.hashCode( _bars );
             result = prime * result + ( ( _bazens == null )
                 ? 0
                 : _bazens.hashCode() );
             result = prime * result + ( ( _foos == null )
                 ? 0
                 : _foos.hashCode() );
             return result;
         }
 
         @Override
         public boolean equals( final Object obj ) {
             if ( this == obj ) {
                 return true;
             }
             if ( obj == null ) {
                 return false;
             }
             if ( getClass() != obj.getClass() ) {
                 return false;
             }
             final EntityWithCollections other = (EntityWithCollections) obj;
             if ( !Arrays.equals( _bars, other._bars ) ) {
                 return false;
             }
             if ( _bazens == null ) {
                 if ( other._bazens != null ) {
                     return false;
                 }
             } else if ( !_bazens.equals( other._bazens ) ) {
                 return false;
             }
             if ( _foos == null ) {
                 if ( other._foos != null ) {
                     return false;
                 }
             } else if ( !_foos.equals( other._foos ) ) {
                 return false;
             }
             return true;
         }
     }
     
     private void assertDeepEquals( final Object one, final Object another ) throws Exception {
         assertDeepEquals( one, another, new IdentityHashMap<Object, Object>() );
     }
 
     private void assertDeepEquals( final Object one, final Object another, final Map<Object, Object> alreadyChecked )
         throws Exception {
         if ( one == another ) {
             return;
         }
         if ( one == null && another != null || one != null && another == null ) {
             Assert.fail( "One of both is null: " + one + ", " + another );
         }
         if ( alreadyChecked.containsKey( one ) ) {
             return;
         }
         alreadyChecked.put( one, another );
 
         Assert.assertEquals( one.getClass(), another.getClass() );
         if ( one.getClass().isPrimitive() || one instanceof String || one instanceof Character || one instanceof Boolean ) {
             Assert.assertEquals( one, another );
             return;
         }
 
         if ( Map.class.isAssignableFrom( one.getClass() ) ) {
             final Map<?, ?> m1 = (Map<?, ?>) one;
             final Map<?, ?> m2 = (Map<?, ?>) another;
             Assert.assertEquals( m1.size(), m2.size() );
             for ( final Map.Entry<?, ?> entry : m1.entrySet() ) {
                 assertDeepEquals( entry.getValue(), m2.get( entry.getKey() ) );
             }
             return;
         }
 
         if ( Number.class.isAssignableFrom( one.getClass() ) ) {
             Assert.assertEquals( ( (Number) one ).longValue(), ( (Number) another ).longValue() );
             return;
         }
 
         if ( one instanceof Currency ) {
             // Check that the transient field defaultFractionDigits is initialized correctly (that was issue #34)
             final Currency currency1 = ( Currency) one;
             final Currency currency2 = ( Currency) another;
             Assert.assertEquals( currency1.getCurrencyCode(), currency2.getCurrencyCode() );
             Assert.assertEquals( currency1.getDefaultFractionDigits(), currency2.getDefaultFractionDigits() );
         }
 
         Class<? extends Object> clazz = one.getClass();
         while ( clazz != null ) {
             assertEqualDeclaredFields( clazz, one, another, alreadyChecked );
             clazz = clazz.getSuperclass();
         }
 
     }
 
     private void assertEqualDeclaredFields( final Class<? extends Object> clazz, final Object one, final Object another,
             final Map<Object, Object> alreadyChecked ) throws Exception, IllegalAccessException {
         for ( final Field field : clazz.getDeclaredFields() ) {
             field.setAccessible( true );
             if ( !Modifier.isTransient( field.getModifiers() ) ) {
                 assertDeepEquals( field.get( one ), field.get( another ), alreadyChecked );
             }
         }
     }
 
     protected byte[] serialize( final Object o ) {
         if ( o == null ) {
             throw new NullPointerException( "Can't serialize null" );
         }
         return new ObjectBuffer(_kryo).writeObject( o );
         
     }
 
     protected <T> T deserialize( final byte[] in, final Class<T> clazz ) {
         return new ObjectBuffer( _kryo ).readObject( in, clazz );
     }
 
 }
