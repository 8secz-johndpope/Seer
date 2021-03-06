 package org.drools.integrationtests;
 
 /*
  * Copyright 2005 JBoss Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.ObjectInput;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutput;
 import java.io.ObjectOutputStream;
 import java.io.Reader;
 import java.io.StringReader;
 import java.math.BigDecimal;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import junit.framework.Assert;
 import junit.framework.TestCase;
 
 import org.acme.insurance.Driver;
 import org.acme.insurance.Policy;
 import org.drools.Address;
 import org.drools.Cell;
 import org.drools.Cheese;
 import org.drools.Cheesery;
 import org.drools.Child;
 import org.drools.FactHandle;
 import org.drools.FirstClass;
 import org.drools.FromTestClass;
 import org.drools.Guess;
 import org.drools.IndexedNumber;
 import org.drools.InsertedObject;
 import org.drools.Order;
 import org.drools.OrderItem;
 import org.drools.Person;
 import org.drools.PersonInterface;
 import org.drools.PersonWithEquals;
 import org.drools.Primitives;
 import org.drools.QueryResults;
 import org.drools.RandomNumber;
 import org.drools.RuleBase;
 import org.drools.RuleBaseConfiguration;
 import org.drools.RuleBaseFactory;
 import org.drools.SecondClass;
 import org.drools.Sensor;
 import org.drools.SpecialString;
 import org.drools.State;
 import org.drools.StatefulSession;
 import org.drools.StatelessSession;
 import org.drools.TestParam;
 import org.drools.WorkingMemory;
 import org.drools.Cheesery.Maturity;
 import org.drools.base.ClassObjectFilter;
 import org.drools.common.AbstractWorkingMemory;
 import org.drools.compiler.DrlParser;
 import org.drools.compiler.DroolsError;
 import org.drools.compiler.DroolsParserException;
 import org.drools.compiler.PackageBuilder;
 import org.drools.compiler.PackageBuilderConfiguration;
 import org.drools.compiler.ParserError;
 import org.drools.compiler.RuleError;
 import org.drools.compiler.PackageBuilder.PackageMergeException;
 import org.drools.event.ActivationCancelledEvent;
 import org.drools.event.ActivationCreatedEvent;
 import org.drools.event.AfterActivationFiredEvent;
 import org.drools.event.AgendaEventListener;
 import org.drools.event.AgendaGroupPoppedEvent;
 import org.drools.event.AgendaGroupPushedEvent;
 import org.drools.event.BeforeActivationFiredEvent;
 import org.drools.event.DefaultWorkingMemoryEventListener;
 import org.drools.event.ObjectInsertedEvent;
 import org.drools.event.ObjectRetractedEvent;
 import org.drools.event.ObjectUpdatedEvent;
 import org.drools.event.WorkingMemoryEventListener;
 import org.drools.facttemplates.Fact;
 import org.drools.facttemplates.FactTemplate;
 import org.drools.integrationtests.helloworld.Message;
 import org.drools.lang.DrlDumper;
 import org.drools.lang.descr.AttributeDescr;
 import org.drools.lang.descr.PackageDescr;
 import org.drools.lang.descr.RuleDescr;
 import org.drools.rule.InvalidRulePackage;
 import org.drools.rule.Package;
 import org.drools.rule.Rule;
 import org.drools.rule.builder.dialect.java.JavaDialectConfiguration;
 import org.drools.spi.Activation;
 import org.drools.spi.ConsequenceExceptionHandler;
 import org.drools.xml.XmlDumper;
 
 /** Run all the tests with the ReteOO engine implementation */
 public class MiscTest extends TestCase {
 
     protected RuleBase getRuleBase() throws Exception {
 
         return RuleBaseFactory.newRuleBase( RuleBase.RETEOO,
                                             null );
     }
 
     protected RuleBase getRuleBase(final RuleBaseConfiguration config) throws Exception {
 
         return RuleBaseFactory.newRuleBase( RuleBase.RETEOO,
                                             config );
     }
 
     public void testGlobals() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "globals_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         workingMemory.setGlobal( "string",
                                  "stilton" );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( new Integer( 5 ),
                       list.get( 0 ) );
     }
 
     public void testFieldBiningsAndEvalSharing() throws Exception {
         final String drl = "test_FieldBindingsAndEvalSharing.drl";
         evalSharingTest( drl );
     }
 
     public void testFieldBiningsAndPredicateSharing() throws Exception {
         final String drl = "test_FieldBindingsAndPredicateSharing.drl";
         evalSharingTest( drl );
     }
 
     private void evalSharingTest(final String drl) throws DroolsParserException,
                                                   IOException,
                                                   Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( drl ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory wm = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         wm.setGlobal( "list",
                       list );
 
         final TestParam tp1 = new TestParam();
         tp1.setValue2( "boo" );
         wm.insert( tp1 );
 
         wm.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
     }
 
     public void testFactBindings() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FactBindings.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         // add the package to a rulebase
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List events = new ArrayList();
         final WorkingMemoryEventListener listener = new DefaultWorkingMemoryEventListener() {
             public void objectUpdated(ObjectUpdatedEvent event) {
                 events.add( event );
             }
         };
 
         workingMemory.addEventListener( listener );
 
         final Person bigCheese = new Person( "big cheese" );
         final Cheese cheddar = new Cheese( "cheddar",
                                            15 );
         bigCheese.setCheese( cheddar );
 
         final FactHandle bigCheeseHandle = workingMemory.insert( bigCheese );
         final FactHandle cheddarHandle = workingMemory.insert( cheddar );
         workingMemory.fireAllRules();
 
         ObjectUpdatedEvent event = (ObjectUpdatedEvent) events.get( 0 );
         assertSame( cheddarHandle,
                     event.getFactHandle() );
         assertSame( cheddar,
                     event.getOldObject() );
         assertSame( cheddar,
                     event.getObject() );
 
         event = (ObjectUpdatedEvent) events.get( 1 );
         assertSame( bigCheeseHandle,
                     event.getFactHandle() );
         assertSame( bigCheese,
                     event.getOldObject() );
         assertSame( bigCheese,
                     event.getObject() );
     }
 
     public void testNullHandling() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NullHandling.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         // add the package to a rulebase
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
         final Cheese nullCheese = new Cheese( null,
                                               2 );
         workingMemory.insert( nullCheese );
 
         final Person notNullPerson = new Person( "shoes butt back" );
         notNullPerson.setBigDecimal( new BigDecimal( "42.42" ) );
 
         workingMemory.insert( notNullPerson );
 
         final Person nullPerson = new Person( "whee" );
         nullPerson.setBigDecimal( null );
 
         workingMemory.insert( nullPerson );
 
         workingMemory.fireAllRules();
         System.out.println( list.get( 0 ) );
         assertEquals( 3,
                       list.size() );
 
     }
 
     public void testEmptyPattern() throws Exception {
         // pre build the package
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EmptyPattern.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         // add the package to a rulebase
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( new Integer( 5 ),
                       list.get( 0 ) );
     }
 
     private RuleBase loadRuleBase(final Reader reader) throws IOException,
                                                       DroolsParserException,
                                                       Exception {
         final DrlParser parser = new DrlParser();
         final PackageDescr packageDescr = parser.parse( reader );
         if ( parser.hasErrors() ) {
             System.err.println(parser.getErrors());
             Assert.fail( "Error messages in parser, need to sort this our (or else collect error messages)" );
         }
         // pre build the package
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackage( packageDescr );
         final Package pkg = builder.getPackage();
 
         // add the package to a rulebase
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         // load up the rulebase
         return ruleBase;
     }
 
     public void testExplicitAnd() throws Exception {
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_ExplicitAnd.drl" ) );
         final RuleBase ruleBase = loadRuleBase( reader );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
         workingMemory.insert( new Message( "hola" ) );
 
         workingMemory.fireAllRules();
         assertEquals( 0,
                       list.size() );
 
         workingMemory.insert( new Cheese( "brie",
                                           33 ) );
 
         workingMemory.fireAllRules();
         assertEquals( 1,
                       list.size() );
     }
 
     public void testHelloWorld() throws Exception {
 
         // read in the source
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "HelloWorld.drl" ) );
         final RuleBase ruleBase = loadRuleBase( reader );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         // go !
         final Message message = new Message( "hola" );
         message.addToList( "hello" );
         message.setNumber( 42 );
 
         workingMemory.insert( message );
         workingMemory.insert( "boo" );
         workingMemory.fireAllRules();
         assertTrue( message.isFired() );
         assertEquals( message,
                       list.get( 0 ) );
 
     }
 
     public void testMVELSoundex() throws Exception {
 
         // read in the source
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "MVEL_soundex.drl" ) );
         final RuleBase ruleBase = loadRuleBase( reader );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         Cheese c = new Cheese("fubar", 2);
 
 
         workingMemory.insert( c );
         workingMemory.fireAllRules();
         assertEquals(42, c.getPrice());
     }
 
     public void testLiteral() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "literal_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( "stilton",
                       list.get( 0 ) );
     }
 
     public void testLiteralWithBoolean() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "literal_with_boolean.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final PersonInterface bill = new Person( "bill",
                                                  null,
                                                  12 );
         bill.setAlive( true );
         workingMemory.insert( bill );
         workingMemory.fireAllRules();
 
         assertEquals( bill,
                       list.get( 0 ) );
     }
 
     public void testFactTemplate() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FactTemplate.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final FactTemplate cheese = pkg.getFactTemplate( "Cheese" );
         final Fact stilton = cheese.createFact( 0 );
         stilton.setFieldValue( "name",
                                "stilton" );
         stilton.setFieldValue( "price",
                                new Integer( 100 ) );
         workingMemory.insert( stilton );
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
         assertEquals( stilton,
                       list.get( 0 ) );
         final Fact fact = (Fact) list.get( 0 );
         assertSame( stilton,
                     fact );
         assertEquals( new Integer( 200 ),
                       fact.getFieldValue( "price" ) );
 
     }
 
     public void testPropertyChangeSupport() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_PropertyChange.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final State state = new State( "initial" );
         workingMemory.insert( state,
                               true );
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         state.setFlag( true );
         assertEquals( 1,
                       list.size() );
 
         workingMemory.fireAllRules();
         assertEquals( 2,
                       list.size() );
 
         state.setState( "finished" );
         workingMemory.fireAllRules();
         assertEquals( 3,
                       list.size() );
 
     }
 
     public void testBigDecimal() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "big_decimal_and_comparable.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final PersonInterface bill = new Person( "bill",
                                                  null,
                                                  12 );
         bill.setBigDecimal( new BigDecimal( "42" ) );
 
         final PersonInterface ben = new Person( "ben",
                                                 null,
                                                 13 );
         ben.setBigDecimal( new BigDecimal( "43" ) );
 
         workingMemory.insert( bill );
         workingMemory.insert( ben );
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
     }
 
     public void testCell() throws Exception {
         final Cell cell1 = new Cell( 9 );
         final Cell cell = new Cell( 0 );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "evalmodify.drl" ) ) );
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
 
         final WorkingMemory memory = ruleBase.newStatefulSession();
         memory.insert( cell1 );
         memory.insert( cell );
         memory.fireAllRules();
         assertEquals( 9,
                       cell.getValue() );
     }
 
     public void testNesting() throws Exception {
         Person p = new Person();
         p.setName( "Michael" );
 
         Address add1 = new Address();
         add1.setStreet( "High" );
 
         Address add2 = new Address();
         add2.setStreet( "Low" );
 
         List l = new ArrayList();
         l.add( add1 );
         l.add( add2 );
 
         p.setAddresses( l );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "nested_fields.drl" ) ) );
 
         assertFalse( builder.getErrors().toString(),
                      builder.hasErrors() );
 
         DrlParser parser = new DrlParser();
         PackageDescr desc = parser.parse( new InputStreamReader( getClass().getResourceAsStream( "nested_fields.drl" ) ) );
         List packageAttrs = desc.getAttributes();
         assertEquals( 1,
                       desc.getRules().size() );
         assertEquals( 1,
                       packageAttrs.size() );
 
         RuleDescr rule = (RuleDescr) desc.getRules().get( 0 );
         List ruleAttrs = rule.getAttributes();
         assertEquals( 1,
                       ruleAttrs.size() );
 
         assertEquals( "mvel",
                       ((AttributeDescr) ruleAttrs.get( 0 )).getValue() );
         assertEquals( "dialect",
                       ((AttributeDescr) ruleAttrs.get( 0 )).getName() );
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
 
         final WorkingMemory memory = ruleBase.newStatefulSession();
 
         memory.insert( p );
         memory.fireAllRules();
 
     }
 
     public void testOr() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "or_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese cheddar = new Cheese( "cheddar",
                                            5 );
         final FactHandle h = workingMemory.insert( cheddar );
 
         workingMemory.fireAllRules();
 
         // just one added
         assertEquals( "got cheese",
                       list.get( 0 ) );
         assertEquals( 1,
                       list.size() );
 
         workingMemory.retract( h );
         workingMemory.fireAllRules();
 
         // still just one
         assertEquals( 1,
                       list.size() );
 
         workingMemory.insert( new Cheese( "stilton",
                                           5 ) );
         workingMemory.fireAllRules();
 
         // now have one more
         assertEquals( 2,
                       list.size() );
 
     }
 
     public void testQuery() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "simple_query_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese stilton = new Cheese( "stinky",
                                            5 );
         workingMemory.insert( stilton );
         final QueryResults results = workingMemory.getQueryResults( "simple query" );
         assertEquals( 1,
                       results.size() );
     }
 
     public void testEval() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "eval_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "five",
                                  new Integer( 5 ) );
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
         workingMemory.fireAllRules();
 
         assertEquals( stilton,
                       list.get( 0 ) );
     }
 
     public void testJaninoEval() throws Exception {
         final PackageBuilderConfiguration config = new PackageBuilderConfiguration();
         JavaDialectConfiguration javaConf = (JavaDialectConfiguration) config.getDialectConfiguration( "java" );
         javaConf.setCompiler( JavaDialectConfiguration.JANINO );
 
         final PackageBuilder builder = new PackageBuilder( config );
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "eval_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "five",
                                  new Integer( 5 ) );
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
         workingMemory.fireAllRules();
 
         assertEquals( stilton,
                       list.get( 0 ) );
     }
 
     public void testEvalMore() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "eval_rule_test_more.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Person foo = new Person( "foo" );
         workingMemory.insert( foo );
         workingMemory.fireAllRules();
 
         assertEquals( foo,
                       list.get( 0 ) );
     }
 
     public void testReturnValue() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "returnvalue_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "two",
                                  new Integer( 2 ) );
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final PersonInterface peter = new Person( "peter",
                                                   null,
                                                   12 );
         workingMemory.insert( peter );
         final PersonInterface jane = new Person( "jane",
                                                  null,
                                                  10 );
         workingMemory.insert( jane );
 
         workingMemory.fireAllRules();
 
         assertEquals( jane,
                       list.get( 0 ) );
         assertEquals( peter,
                       list.get( 1 ) );
     }
 
     public void testPredicate() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "predicate_rule_test.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "two",
                                  new Integer( 2 ) );
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final PersonInterface peter = new Person( "peter",
                                                   null,
                                                   12 );
         workingMemory.insert( peter );
         final PersonInterface jane = new Person( "jane",
                                                  null,
                                                  10 );
         workingMemory.insert( jane );
 
         workingMemory.fireAllRules();
 
         assertEquals( jane,
                       list.get( 0 ) );
         assertEquals( peter,
                       list.get( 1 ) );
     }
 
     public void testNullBehaviour() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "null_behaviour.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final PersonInterface p1 = new Person( "michael",
                                                "food",
                                                40 );
         final PersonInterface p2 = new Person( null,
                                                "drink",
                                                30 );
         workingMemory.insert( p1 );
         workingMemory.insert( p2 );
 
         workingMemory.fireAllRules();
 
     }
 
     public void testNullConstraint() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "null_constraint.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List foo = new ArrayList();
         workingMemory.setGlobal( "messages",
                                  foo );
 
         final PersonInterface p1 = new Person( null,
                                                "food",
                                                40 );
         final Primitives p2 = new Primitives();
         p2.setArrayAttribute( null );
 
         workingMemory.insert( p1 );
         workingMemory.insert( p2 );
 
         workingMemory.fireAllRules();
         assertEquals( 2,
                       foo.size() );
 
     }
 
     public void testImportFunctions() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ImportFunctions.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final Cheese cheese = new Cheese( "stilton",
                                           15 );
         workingMemory.insert( cheese );
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
         workingMemory.fireAllRules();
 
         assertEquals( 4,
                       list.size() );
 
         assertEquals( "rule1",
                       list.get( 0 ) );
         assertEquals( "rule2",
                       list.get( 1 ) );
         assertEquals( "rule3",
                       list.get( 2 ) );
         assertEquals( "rule4",
                       list.get( 3 ) );
     }
 
     public void testBasicFrom() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_From.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List list1 = new ArrayList();
         workingMemory.setGlobal( "list1",
                                  list1 );
         final List list2 = new ArrayList();
         workingMemory.setGlobal( "list2",
                                  list2 );
         final List list3 = new ArrayList();
         workingMemory.setGlobal( "list3",
                                  list3 );
 
         final Cheesery cheesery = new Cheesery();
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         final Cheese cheddar = new Cheese( "cheddar",
                                            15 );
         cheesery.addCheese( stilton );
         cheesery.addCheese( cheddar );
         workingMemory.setGlobal( "cheesery",
                                  cheesery );
         workingMemory.insert( cheesery );
 
         Person p = new Person( "stilton" );
         workingMemory.insert( p );
 
         workingMemory.fireAllRules();
 
         // from using a global
         assertEquals( 2,
                       list1.size() );
         assertEquals( cheddar,
                       list1.get( 0 ) );
         assertEquals( stilton,
                       list1.get( 1 ) );
 
         // from using a declaration
         assertEquals( 2,
                       list2.size() );
         assertEquals( cheddar,
                       list2.get( 0 ) );
         assertEquals( stilton,
                       list2.get( 1 ) );
 
         // from using a declaration
         assertEquals( 1,
                       list3.size() );
         assertEquals( stilton,
                       list3.get( 0 ) );
     }
 
     public void testFromWithParams() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FromWithParams.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List list = new ArrayList();
         final Object globalObject = new Object();
         workingMemory.setGlobal( "list",
                                  list );
         workingMemory.setGlobal( "testObject",
                                  new FromTestClass() );
         workingMemory.setGlobal( "globalObject",
                                  globalObject );
 
         final Person bob = new Person( "bob" );
         workingMemory.insert( bob );
 
         workingMemory.fireAllRules();
 
         assertEquals( 6,
                       list.size() );
 
         final List array = (List) list.get( 0 );
         assertEquals( 3,
                       array.size() );
         final Person p = (Person) array.get( 0 );
         assertSame( p,
                     bob );
 
         assertEquals( new Integer( 42 ),
                       array.get( 1 ) );
 
         final List nested = (List) array.get( 2 );
         assertEquals( "x",
                       nested.get( 0 ) );
         assertEquals( "y",
                       nested.get( 1 ) );
 
         final Map map = (Map) list.get( 1 );
         assertEquals( 2,
                       map.keySet().size() );
 
         assertTrue( map.keySet().contains( bob ) );
         assertSame( globalObject,
                     map.get( bob ) );
 
         assertTrue( map.keySet().contains( "key1" ) );
         final Map nestedMap = (Map) map.get( "key1" );
         assertEquals( 1,
                       nestedMap.keySet().size() );
         assertTrue( nestedMap.keySet().contains( "key2" ) );
         assertEquals( "value2",
                       nestedMap.get( "key2" ) );
 
         assertEquals( new Integer( 42 ),
                       list.get( 2 ) );
         assertEquals( "literal",
                       list.get( 3 ) );
         assertSame( bob,
                     list.get( 4 ) );
         assertSame( globalObject,
                     list.get( 5 ) );
     }
 
     public void testFromWithNewConstructor() throws Exception {
         DrlParser parser = new DrlParser();
         PackageDescr descr = parser.parse( new InputStreamReader( getClass().getResourceAsStream( "test_FromWithNewConstructor.drl" ) ) );
         PackageBuilder builder = new PackageBuilder();
         builder.addPackage( descr );
         Package pkg = builder.getPackage();
         pkg.checkValidity();
     }
 
     public void testWithInvalidRule() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "invalid_rule.drl" ) ) );
         final Package pkg = builder.getPackage();
         // Mark: please check if the conseqeuence/should/shouldn't be built
         // Rule badBoy = pkg.getRules()[0];
         // assertFalse(badBoy.isValid());
 
         RuntimeException runtime = null;
         // this should ralph all over the place.
         final RuleBase ruleBase = getRuleBase();
         try {
             ruleBase.addPackage( pkg );
             fail( "Should have thrown an exception as the rule is NOT VALID." );
         } catch ( final RuntimeException e ) {
             assertNotNull( e.getMessage() );
             runtime = e;
         }
         assertTrue( builder.getErrors().getErrors().length > 0 );
 
         final String pretty = builder.getErrors().toString();
         assertFalse( pretty.equals( "" ) );
         assertEquals( pretty,
                       runtime.getMessage() );
 
     }
 
     public void testErrorLineNumbers() throws Exception {
         // this test aims to test semantic errors
         // parser errors are another test case
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "errors_in_rule.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final DroolsError err = builder.getErrors().getErrors()[0];
         final RuleError ruleErr = (RuleError) err;
         assertNotNull( ruleErr.getDescr() );
         assertTrue( ruleErr.getLine() != -1 );
 
         final DroolsError errs[] = builder.getErrors().getErrors();
 
         assertEquals( 3,
                       builder.getErrors().getErrors().length );
 
         // check that its getting it from the ruleDescr
         assertEquals( ruleErr.getLine(),
                       ruleErr.getDescr().getLine() );
         // check the absolute error line number (there are more).
         assertEquals( 11,
                       ruleErr.getLine() );
 
         // now check the RHS, not being too specific yet, as long as it has the
         // rules line number, not zero
         final RuleError rhs = (RuleError) builder.getErrors().getErrors()[2];
         assertTrue( rhs.getLine() > 7 ); // not being too specific - may need to
         // change this when we rework the error
         // reporting
 
     }
 
     public void testErrorsParser() throws Exception {
         final DrlParser parser = new DrlParser();
         assertEquals( 0,
                       parser.getErrors().size() );
         parser.parse( new InputStreamReader( getClass().getResourceAsStream( "errors_parser_multiple.drl" ) ) );
         assertTrue( parser.hasErrors() );
         assertTrue( parser.getErrors().size() > 0 );
         assertTrue( parser.getErrors().get( 0 ) instanceof ParserError );
         final ParserError first = ((ParserError) parser.getErrors().get( 0 ));
         assertTrue( first.getMessage() != null );
         assertFalse( first.getMessage().equals( "" ) );
     }
 
     public void testFunction() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FunctionInConsequence.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( new Integer( 5 ),
                       list.get( 0 ) );
     }
 
     public void testAssertRetract() throws Exception {
         // postponed while I sort out KnowledgeHelperFixer
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "assert_retract.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final PersonInterface person = new Person( "michael",
                                                    "cheese" );
         person.setStatus( "start" );
         workingMemory.insert( person );
         workingMemory.fireAllRules();
 
         assertEquals( 5,
                       list.size() );
         assertTrue( list.contains( "first" ) );
         assertTrue( list.contains( "second" ) );
         assertTrue( list.contains( "third" ) );
         assertTrue( list.contains( "fourth" ) );
         assertTrue( list.contains( "fifth" ) );
 
     }
 
     public void testPredicateAsFirstPattern() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "predicate_as_first_pattern.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese mussarela = new Cheese( "Mussarela",
                                              35 );
         workingMemory.insert( mussarela );
         final Cheese provolone = new Cheese( "Provolone",
                                              20 );
         workingMemory.insert( provolone );
 
         workingMemory.fireAllRules();
 
         Assert.assertEquals( "The rule is being incorrectly fired",
                              35,
                              mussarela.getPrice() );
         Assert.assertEquals( "Rule is incorrectly being fired",
                              20,
                              provolone.getPrice() );
     }
 
     public void testConsequenceException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ConsequenceException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
         workingMemory.insert( brie );
 
         try {
             workingMemory.fireAllRules();
             fail( "Should throw an Exception from the Consequence" );
         } catch ( final Exception e ) {
             assertEquals( "this should throw an exception",
                           e.getCause().getMessage() );
         }
     }
 
     public void testCustomConsequenceException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ConsequenceException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         RuleBaseConfiguration conf = new RuleBaseConfiguration();
         CustomConsequenceExceptionHandler handler = new CustomConsequenceExceptionHandler();
         conf.setConsequenceExceptionHandler( handler );
 
         final RuleBase ruleBase = getRuleBase(conf);
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertTrue( handler.isCalled() );
     }
 
     public static class CustomConsequenceExceptionHandler implements ConsequenceExceptionHandler {
 
         private boolean called;
 
         public void handleException(Activation activation,
                                     WorkingMemory workingMemory,
                                     Exception exception) {
             this.called = true;
         }
 
         public boolean isCalled() {
             return this.called;
         }
 
     }
 
     public void testFunctionException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FunctionException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
         workingMemory.insert( brie );
 
         try {
             workingMemory.fireAllRules();
             fail( "Should throw an Exception from the Function" );
         } catch ( final Exception e ) {
             assertEquals( "this should throw an exception",
                           e.getCause().getMessage() );
         }
     }
 
     public void testEvalException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EvalException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
 
         try {
             workingMemory.insert( brie );
             workingMemory.fireAllRules();
             fail( "Should throw an Exception from the Eval" );
         } catch ( final Exception e ) {
             assertEquals( "this should throw an exception",
                           e.getCause().getMessage() );
         }
     }
 
     public void testPredicateException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_PredicateException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
 
         try {
             workingMemory.insert( brie );
             workingMemory.fireAllRules();
             fail( "Should throw an Exception from the Predicate" );
         } catch ( final Exception e ) {
             assertEquals( "this should throw an exception",
                           e.getCause().getMessage() );
         }
     }
 
     public void testReturnValueException() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ReturnValueException.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
 
         try {
             workingMemory.insert( brie );
             workingMemory.fireAllRules();
             fail( "Should throw an Exception from the ReturnValue" );
         } catch ( final Exception e ) {
             assertTrue( e.getCause().getMessage().endsWith( "this should throw an exception" ) );
         }
     }
 
     public void testMultiRestrictionFieldConstraint() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_MultiRestrictionFieldConstraint.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list1 = new ArrayList();
         workingMemory.setGlobal( "list1",
                                  list1 );
         final List list2 = new ArrayList();
         workingMemory.setGlobal( "list2",
                                  list2 );
         final List list3 = new ArrayList();
         workingMemory.setGlobal( "list3",
                                  list3 );
         final List list4 = new ArrayList();
         workingMemory.setGlobal( "list4",
                                  list4 );
 
         final Person youngChili1 = new Person( "young chili1" );
         youngChili1.setAge( 12 );
         youngChili1.setHair( "blue" );
         final Person youngChili2 = new Person( "young chili2" );
         youngChili2.setAge( 25 );
         youngChili2.setHair( "purple" );
 
         final Person chili1 = new Person( "chili1" );
         chili1.setAge( 35 );
         chili1.setHair( "red" );
 
         final Person chili2 = new Person( "chili2" );
         chili2.setAge( 38 );
         chili2.setHair( "indigigo" );
 
         final Person oldChili1 = new Person( "old chili2" );
         oldChili1.setAge( 45 );
         oldChili1.setHair( "green" );
 
         final Person oldChili2 = new Person( "old chili2" );
         oldChili2.setAge( 48 );
         oldChili2.setHair( "blue" );
 
         workingMemory.insert( youngChili1 );
         workingMemory.insert( youngChili2 );
         workingMemory.insert( chili1 );
         workingMemory.insert( chili2 );
         workingMemory.insert( oldChili1 );
         workingMemory.insert( oldChili2 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list1.size() );
         assertTrue( list1.contains( chili1 ) );
 
         assertEquals( 2,
                       list2.size() );
         assertTrue( list2.contains( chili1 ) );
         assertTrue( list2.contains( chili2 ) );
 
         assertEquals( 2,
                       list3.size() );
         assertTrue( list3.contains( youngChili1 ) );
         assertTrue( list3.contains( youngChili2 ) );
 
         assertEquals( 2,
                       list4.size() );
         assertTrue( list4.contains( youngChili1 ) );
         assertTrue( list4.contains( chili1 ) );
     }
 
     public void testDumpers() throws Exception {
         final DrlParser parser = new DrlParser();
         final PackageDescr pkg = parser.parse( new InputStreamReader( getClass().getResourceAsStream( "test_Dumpers.drl" ) ) );
 
         PackageBuilder builder = new PackageBuilder();
         builder.addPackage( pkg );
 
         RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
         WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese brie = new Cheese( "brie",
                                         12 );
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertEquals( 3,
                       list.size() );
         assertEquals( "3 1",
                       list.get( 0 ) );
         assertEquals( "MAIN",
                       list.get( 1 ) );
         assertEquals( "1 1",
                       list.get( 2 ) );
 
         final DrlDumper drlDumper = new DrlDumper();
         final String drlResult = drlDumper.dump( pkg );
         builder = new PackageBuilder();
         builder.addPackageFromDrl( new StringReader( drlResult ) );
 
         ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
         workingMemory = ruleBase.newStatefulSession();
 
         list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertEquals( 3,
                       list.size() );
         assertEquals( "3 1",
                       list.get( 0 ) );
         assertEquals( "MAIN",
                       list.get( 1 ) );
         assertEquals( "1 1",
                       list.get( 2 ) );
 
         final XmlDumper xmlDumper = new XmlDumper();
         final String xmlResult = xmlDumper.dump( pkg );
 
         // System.out.println( xmlResult );
 
         builder = new PackageBuilder();
         builder.addPackageFromXml( new StringReader( xmlResult ) );
 
         ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
         workingMemory = ruleBase.newStatefulSession();
 
         list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertEquals( 3,
                       list.size() );
         assertEquals( "3 1",
                       list.get( 0 ) );
         assertEquals( "MAIN",
                       list.get( 1 ) );
         assertEquals( "1 1",
                       list.get( 2 ) );
     }
 
     public void testContainsCheese() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ContainsCheese.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         workingMemory.insert( stilton );
         final Cheese brie = new Cheese( "brie",
                                         10 );
         workingMemory.insert( brie );
 
         final Cheesery cheesery = new Cheesery();
         cheesery.getCheeses().add( stilton );
         workingMemory.insert( cheesery );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( stilton,
                       list.get( 0 ) );
         assertEquals( brie,
                       list.get( 1 ) );
     }
 
     public void testStaticFieldReference() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_StaticField.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheesery cheesery1 = new Cheesery();
         cheesery1.setStatus( Cheesery.SELLING_CHEESE );
         cheesery1.setMaturity( Maturity.OLD );
         workingMemory.insert( cheesery1 );
 
         final Cheesery cheesery2 = new Cheesery();
         cheesery2.setStatus( Cheesery.MAKING_CHEESE );
         cheesery2.setMaturity( Maturity.YOUNG );
         workingMemory.insert( cheesery2 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( cheesery1,
                       list.get( 0 ) );
         assertEquals( cheesery2,
                       list.get( 1 ) );
     }
 
     public void testDuplicateRuleNames() throws Exception {
         PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DuplicateRuleName1.drl" ) ) );
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
 
         builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DuplicateRuleName2.drl" ) ) );
         ruleBase.addPackage( builder.getPackage() );
 
         // @todo: this is from JBRULES-394 - maybe we should test more stuff
         // here?
 
     }
 
     public void testNullValuesIndexing() throws Exception {
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_NullValuesIndexing.drl" ) );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( reader );
         final Package pkg1 = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg1 );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         // Adding person with null name and likes attributes
         final PersonInterface bob = new Person( null,
                                                 null );
         bob.setStatus( "P1" );
         final PersonInterface pete = new Person( null,
                                                  null );
         bob.setStatus( "P2" );
         workingMemory.insert( bob );
         workingMemory.insert( pete );
 
         workingMemory.fireAllRules();
 
         Assert.assertEquals( "Indexing with null values is not working correctly.",
                              "OK",
                              bob.getStatus() );
         Assert.assertEquals( "Indexing with null values is not working correctly.",
                              "OK",
                              pete.getStatus() );
 
     }
 
     public void testSerializable() throws Exception {
 
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable.drl" ) );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( reader );
         final Package pkg = builder.getPackage();
 
         assertEquals( 0,
                       builder.getErrors().getErrors().length );
 
         RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();
 
         ruleBase.addPackage( pkg );
 
         Map map = new HashMap();
         map.put( "x",
                  ruleBase );
         final byte[] ast = serializeOut( map );
         map = (Map) serializeIn( ast );
         ruleBase = (RuleBase) map.get( "x" );
         final Rule[] rules = ruleBase.getPackages()[0].getRules();
         assertEquals( 4,
                       rules.length );
 
         assertEquals( "match Person 1",
                       rules[0].getName() );
         assertEquals( "match Person 2",
                       rules[1].getName() );
         assertEquals( "match Person 3",
                       rules[2].getName() );
         assertEquals( "match Integer",
                       rules[3].getName() );
 
         WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "list",
                                  new ArrayList() );
 
         final Person bob = new Person( "bob" );
         workingMemory.insert( bob );
 
         final byte[] wm = serializeOut( workingMemory );
 
         workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );
 
         assertEquals( 1,
                       IteratorToList.convert( workingMemory.iterateObjects() ).size() );
         assertEquals( bob,
                       IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );
 
         assertEquals( 2,
                       workingMemory.getAgenda().agendaSize() );
 
         workingMemory.fireAllRules();
 
         final List list = (List) workingMemory.getGlobal( "list" );
 
         assertEquals( 3,
                       list.size() );
         // because of agenda-groups
         assertEquals( new Integer( 4 ),
                       list.get( 0 ) );
 
         assertEquals( 2,
                       IteratorToList.convert( workingMemory.iterateObjects() ).size() );
         assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( bob ) );
         assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( new Person( "help" ) ) );
     }
 
     public void testEmptyRule() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EmptyRule.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         workingMemory.fireAllRules();
 
         assertTrue( list.contains( "fired1" ) );
         assertTrue( list.contains( "fired2" ) );
     }
 
     public void testjustEval() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NoPatterns.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         workingMemory.fireAllRules();
 
         assertTrue( list.contains( "fired1" ) );
         assertTrue( list.contains( "fired3" ) );
     }
 
     public void testOrWithBinding() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_OrWithBindings.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final Person hola = new Person( "hola" );
         workingMemory.insert( hola );
 
         workingMemory.fireAllRules();
 
         assertEquals( 0,
                       list.size() );
         workingMemory.insert( new State( "x" ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
         assertTrue( list.contains( hola ) );
 
     }
 
     protected Object serializeIn(final byte[] bytes) throws IOException,
                                                     ClassNotFoundException {
         final ObjectInput in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
         final Object obj = in.readObject();
         in.close();
         return obj;
     }
 
     protected byte[] serializeOut(final Object obj) throws IOException {
         // Serialize to a byte array
         final ByteArrayOutputStream bos = new ByteArrayOutputStream();
         final ObjectOutput out = new ObjectOutputStream( bos );
         out.writeObject( obj );
         out.close();
 
         // Get the bytes of the serialized object
         final byte[] bytes = bos.toByteArray();
         return bytes;
     }
 
     public void testJoinNodeModifyObject() throws Exception {
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_JoinNodeModifyObject.drl" ) );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( reader );
         final Package pkg1 = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg1 );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List orderedFacts = new ArrayList();
         final List errors = new ArrayList();
 
         workingMemory.setGlobal( "orderedNumbers",
                                  orderedFacts );
         workingMemory.setGlobal( "errors",
                                  errors );
 
         final int MAX = 5;
         for ( int i = 1; i <= MAX; i++ ) {
             final IndexedNumber n = new IndexedNumber( i,
                                                        MAX - i + 1 );
             workingMemory.insert( n );
         }
         workingMemory.fireAllRules();
 
         Assert.assertTrue( "Processing generated errors: " + errors.toString(),
                            errors.isEmpty() );
 
         for ( int i = 1; i <= MAX; i++ ) {
             final IndexedNumber n = (IndexedNumber) orderedFacts.get( i - 1 );
             Assert.assertEquals( "Fact is out of order",
                                  i,
                                  n.getIndex() );
         }
     }
 
     public void testQuery2() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Query.drl" ) ) );
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         workingMemory.fireAllRules();
 
         final QueryResults results = workingMemory.getQueryResults( "assertedobjquery" );
         assertEquals( 1,
                       results.size() );
         assertEquals( new InsertedObject( "value1" ),
                       results.get( 0 ).get( 0 ) );
     }
 
     public void testQueryWithParams() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_QueryWithParams.drl" ) ) );
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( builder.getPackage() );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         workingMemory.fireAllRules();
 
         QueryResults results = workingMemory.getQueryResults( "assertedobjquery",
                                                               new String[]{"value1"} );
         assertEquals( 1,
                       results.size() );
         assertEquals( new InsertedObject( "value1" ),
                       results.get( 0 ).get( 0 ) );
 
         results = workingMemory.getQueryResults( "assertedobjquery",
                                                  new String[]{"value3"} );
         assertEquals( 0,
                       results.size() );
 
         results = workingMemory.getQueryResults( "assertedobjquery2",
                                                  new String[]{null, "value2"} );
         assertEquals( 1,
                       results.size() );
         assertEquals( new InsertedObject( "value2" ),
                       results.get( 0 ).get( 0 ) );
 
         results = workingMemory.getQueryResults( "assertedobjquery2",
                                                  new String[]{"value3", "value2"} );
         assertEquals( 1,
                       results.size() );
         assertEquals( new InsertedObject( "value2" ),
                       results.get( 0 ).get( 0 ) );
     }
 
     public void testTwoQuerries() throws Exception {
         // @see JBRULES-410 More than one Query definition causes an incorrect
         // Rete network to be built.
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_TwoQuerries.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Cheese stilton = new Cheese( "stinky",
                                            5 );
         workingMemory.insert( stilton );
         final Person per1 = new Person( "stinker",
                                         "smelly feet",
                                         70 );
         final Person per2 = new Person( "skunky",
                                         "smelly armpits",
                                         40 );
 
         workingMemory.insert( per1 );
         workingMemory.insert( per2 );
 
         QueryResults results = workingMemory.getQueryResults( "find stinky cheeses" );
         assertEquals( 1,
                       results.size() );
 
         results = workingMemory.getQueryResults( "find pensioners" );
         assertEquals( 1,
                       results.size() );
     }
 
     public void testInsurancePricingExample() throws Exception {
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "insurance_pricing_example.drl" ) );
         final RuleBase ruleBase = loadRuleBase( reader );
         final WorkingMemory wm = ruleBase.newStatefulSession();
 
         // now create some test data
         final Driver driver = new Driver();
         final Policy policy = new Policy();
 
         wm.insert( driver );
         wm.insert( policy );
 
         wm.fireAllRules();
 
         assertEquals( 120,
                       policy.getBasePrice() );
     }
 
     public void testLLR() throws Exception {
 
         // read in the source
         final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_JoinNodeModifyTuple.drl" ) );
         final RuleBase ruleBase = loadRuleBase( reader );
 
         final WorkingMemory wm = ruleBase.newStatefulSession();
 
         // 1st time
         org.drools.Target tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Anna" );
         tgt.setLat( new Float( 60.26544f ) );
         tgt.setLon( new Float( 28.952137f ) );
         tgt.setCourse( new Float( 145.0f ) );
         tgt.setSpeed( new Float( 12.0f ) );
         tgt.setTime( new Float( 1.8666667f ) );
         wm.insert( tgt );
 
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Maria" );
         tgt.setLat( new Float( 60.236874f ) );
         tgt.setLon( new Float( 28.992579f ) );
         tgt.setCourse( new Float( 325.0f ) );
         tgt.setSpeed( new Float( 8.0f ) );
         tgt.setTime( new Float( 1.8666667f ) );
         wm.insert( tgt );
 
         wm.fireAllRules();
 
         // 2nd time
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Anna" );
         tgt.setLat( new Float( 60.265343f ) );
         tgt.setLon( new Float( 28.952267f ) );
         tgt.setCourse( new Float( 145.0f ) );
         tgt.setSpeed( new Float( 12.0f ) );
         tgt.setTime( new Float( 1.9f ) );
         wm.insert( tgt );
 
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Maria" );
         tgt.setLat( new Float( 60.236935f ) );
         tgt.setLon( new Float( 28.992493f ) );
         tgt.setCourse( new Float( 325.0f ) );
         tgt.setSpeed( new Float( 8.0f ) );
         tgt.setTime( new Float( 1.9f ) );
         wm.insert( tgt );
 
         wm.fireAllRules();
 
         // 3d time
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Anna" );
         tgt.setLat( new Float( 60.26525f ) );
         tgt.setLon( new Float( 28.952396f ) );
         tgt.setCourse( new Float( 145.0f ) );
         tgt.setSpeed( new Float( 12.0f ) );
         tgt.setTime( new Float( 1.9333333f ) );
         wm.insert( tgt );
 
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Maria" );
         tgt.setLat( new Float( 60.236996f ) );
         tgt.setLon( new Float( 28.992405f ) );
         tgt.setCourse( new Float( 325.0f ) );
         tgt.setSpeed( new Float( 8.0f ) );
         tgt.setTime( new Float( 1.9333333f ) );
         wm.insert( tgt );
 
         wm.fireAllRules();
 
         // 4th time
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Anna" );
         tgt.setLat( new Float( 60.265163f ) );
         tgt.setLon( new Float( 28.952526f ) );
         tgt.setCourse( new Float( 145.0f ) );
         tgt.setSpeed( new Float( 12.0f ) );
         tgt.setTime( new Float( 1.9666667f ) );
         wm.insert( tgt );
 
         tgt = new org.drools.Target();
         tgt.setLabel( "Santa-Maria" );
         tgt.setLat( new Float( 60.237057f ) );
         tgt.setLon( new Float( 28.99232f ) );
         tgt.setCourse( new Float( 325.0f ) );
         tgt.setSpeed( new Float( 8.0f ) );
         tgt.setTime( new Float( 1.9666667f ) );
         wm.insert( tgt );
 
         wm.fireAllRules();
     }
 
     public void testDoubleQueryWithExists() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DoubleQueryWithExists.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final Person p1 = new Person( "p1",
                                       "stilton",
                                       20 );
         p1.setStatus( "europe" );
         final FactHandle c1FactHandle = workingMemory.insert( p1 );
         final Person p2 = new Person( "p2",
                                       "stilton",
                                       30 );
         p2.setStatus( "europe" );
         final FactHandle c2FactHandle = workingMemory.insert( p2 );
         final Person p3 = new Person( "p3",
                                       "stilton",
                                       40 );
         p3.setStatus( "europe" );
         final FactHandle c3FactHandle = workingMemory.insert( p3 );
         workingMemory.fireAllRules();
 
         QueryResults queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 2,
                       queryResults.size() );
 
         // europe=[ 1, 2 ], america=[ 3 ]
         p3.setStatus( "america" );
         workingMemory.update( c3FactHandle,
                               p3 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 1,
                       queryResults.size() );
 
         // europe=[ 1 ], america=[ 2, 3 ]
         p2.setStatus( "america" );
         workingMemory.update( c2FactHandle,
                               p2 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 1,
                       queryResults.size() );
 
         // europe=[ ], america=[ 1, 2, 3 ]
         p1.setStatus( "america" );
         workingMemory.update( c1FactHandle,
                               p1 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 2,
                       queryResults.size() );
 
         // europe=[ 2 ], america=[ 1, 3 ]
         p2.setStatus( "europe" );
         workingMemory.update( c2FactHandle,
                               p2 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 1,
                       queryResults.size() );
 
         // europe=[ 1, 2 ], america=[ 3 ]
         p1.setStatus( "europe" );
         workingMemory.update( c1FactHandle,
                               p1 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 1,
                       queryResults.size() );
 
         // europe=[ 1, 2, 3 ], america=[ ]
         p3.setStatus( "europe" );
         workingMemory.update( c3FactHandle,
                               p3 );
         workingMemory.fireAllRules();
         queryResults = workingMemory.getQueryResults( "2 persons with the same status" );
         assertEquals( 2,
                       queryResults.size() );
     }
 
     public void testFunctionWithPrimitives() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_FunctionWithPrimitives.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( new Integer( 10 ),
                       list.get( 0 ) );
     }
 
     public void testReturnValueAndGlobal() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ReturnValueAndGlobal.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List matchlist = new ArrayList();
         workingMemory.setGlobal( "matchingList",
                                  matchlist );
 
         final List nonmatchlist = new ArrayList();
         workingMemory.setGlobal( "nonMatchingList",
                                  nonmatchlist );
 
         workingMemory.setGlobal( "cheeseType",
                                  "stilton" );
 
         final Cheese stilton1 = new Cheese( "stilton",
                                             5 );
         final Cheese stilton2 = new Cheese( "stilton",
                                             7 );
         final Cheese brie = new Cheese( "brie",
                                         4 );
         workingMemory.insert( stilton1 );
         workingMemory.insert( stilton2 );
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       matchlist.size() );
         assertEquals( 1,
                       nonmatchlist.size() );
     }
 
     public void testDeclaringAndUsingBindsInSamePattern() throws Exception {
         final RuleBaseConfiguration config = new RuleBaseConfiguration();
         config.setRemoveIdentities( true );
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DeclaringAndUsingBindsInSamePattern.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase( config );
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List sensors = new ArrayList();
 
         workingMemory.setGlobal( "sensors",
                                  sensors );
 
         final Sensor sensor1 = new Sensor( 100,
                                            150 );
         workingMemory.insert( sensor1 );
         workingMemory.fireAllRules();
         assertEquals( 0,
                       sensors.size() );
 
         final Sensor sensor2 = new Sensor( 200,
                                            150 );
         workingMemory.insert( sensor2 );
         workingMemory.fireAllRules();
         assertEquals( 3,
                       sensors.size() );
     }
 
     public void testMissingImports() {
         try {
             final PackageBuilder builder = new PackageBuilder();
             builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_missing_import.drl" ) ) );
             final Package pkg = builder.getPackage();
 
             final RuleBase ruleBase = getRuleBase();
             ruleBase.addPackage( pkg );
 
             Assert.fail( "Should have thrown an InvalidRulePackage" );
         } catch ( final InvalidRulePackage e ) {
             // everything fine
         } catch ( final Exception e ) {
             e.printStackTrace();
             Assert.fail( "Should have thrown an InvalidRulePackage Exception instead of " + e.getMessage() );
         }
     }
 
     public void testNestedConditionalElements() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NestedConditionalElements.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final State state = new State( "SP" );
         workingMemory.insert( state );
 
         final Person bob = new Person( "Bob" );
         bob.setStatus( state.getState() );
         bob.setLikes( "stilton" );
         workingMemory.insert( bob );
 
         workingMemory.fireAllRules();
 
         assertEquals( 0,
                       list.size() );
 
         workingMemory.insert( new Cheese( bob.getLikes(),
                                           10 ) );
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
     }
 
     public void testDeclarationUsage() throws Exception {
 
         try {
             final PackageBuilder builder = new PackageBuilder();
             builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DeclarationUsage.drl" ) ) );
             final Package pkg = builder.getPackage();
 
             final RuleBase ruleBase = getRuleBase();
             ruleBase.addPackage( pkg );
 
             fail( "Should have trown an exception" );
         } catch ( final InvalidRulePackage e ) {
             // success ... correct exception thrown
         } catch ( final Exception e ) {
             e.printStackTrace();
             fail( "Wrong exception raised: " + e.getMessage() );
         }
     }
 
     public void testUnbalancedTrees() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_UnbalancedTrees.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         final WorkingMemory wm = ruleBase.newStatefulSession();
 
         wm.insert( new Cheese( "a",
                                10 ) );
         wm.insert( new Cheese( "b",
                                10 ) );
         wm.insert( new Cheese( "c",
                                10 ) );
         wm.insert( new Cheese( "d",
                                10 ) );
         final Cheese e = new Cheese( "e",
                                      10 );
         wm.insert( e );
 
         wm.fireAllRules();
 
         Assert.assertEquals( "Rule should have fired twice, seting the price to 30",
                              30,
                              e.getPrice() );
         // success
     }
 
     public void testImportConflict() throws Exception {
         final RuleBase ruleBase = getRuleBase();
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ImportConflict.drl" ) ) );
         final Package pkg = builder.getPackage();
         ruleBase.addPackage( pkg );
     }
 
     public void testPrimitiveArray() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_primitiveArray.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List result = new ArrayList();
         workingMemory.setGlobal( "result",
                                  result );
 
         final Primitives p1 = new Primitives();
         p1.setPrimitiveArrayAttribute( new int[]{1, 2, 3} );
         p1.setArrayAttribute( new String[]{"a", "b"} );
 
         workingMemory.insert( p1 );
 
         workingMemory.fireAllRules();
         assertEquals( 3,
                       result.size() );
         assertEquals( 3,
                       ((Integer) result.get( 0 )).intValue() );
         assertEquals( 2,
                       ((Integer) result.get( 1 )).intValue() );
         assertEquals( 3,
                       ((Integer) result.get( 2 )).intValue() );
 
     }
 
     public void testEmptyIdentifier() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_emptyIdentifier.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final List result = new ArrayList();
         workingMemory.setGlobal( "results",
                                  result );
 
         final Person person = new Person( "bob" );
         final Cheese cheese = new Cheese( "brie",
                                           10 );
 
         workingMemory.insert( person );
         workingMemory.insert( cheese );
 
         workingMemory.fireAllRules();
         assertEquals( 4,
                       result.size() );
     }
 
     public void testDuplicateVariableBinding() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_duplicateVariableBinding.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
         final Map result = new HashMap();
         workingMemory.setGlobal( "results",
                                  result );
 
         final Cheese stilton = new Cheese( "stilton",
                                            20 );
         final Cheese brie = new Cheese( "brie",
                                         10 );
 
         workingMemory.insert( stilton );
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
         assertEquals( 5,
                       result.size() );
         assertEquals( stilton.getPrice(),
                       ((Integer) result.get( stilton.getType() )).intValue() );
         assertEquals( brie.getPrice(),
                       ((Integer) result.get( brie.getType() )).intValue() );
 
         assertEquals( stilton.getPrice(),
                       ((Integer) result.get( stilton )).intValue() );
         assertEquals( brie.getPrice(),
                       ((Integer) result.get( brie )).intValue() );
 
         assertEquals( stilton.getPrice(),
                       ((Integer) result.get( "test3" + stilton.getType() )).intValue() );
 
         workingMemory.insert( new Person( "bob",
                                           brie.getType() ) );
         workingMemory.fireAllRules();
 
         assertEquals( 6,
                       result.size() );
         assertEquals( brie.getPrice(),
                       ((Integer) result.get( "test3" + brie.getType() )).intValue() );
     }
 
     public void testDuplicateVariableBindingError() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_duplicateVariableBindingError.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         assertFalse( pkg.isValid() );
         System.out.println( pkg.getErrorSummary() );
         assertEquals( 6,
                       pkg.getErrorSummary().split( "\n" ).length );
     }
 
     public void testShadowProxyInHirarchies() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ShadowProxyInHirarchies.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.insert( new Child( "gp" ) );
 
         workingMemory.fireAllRules();
     }
 
     public void testSelfReference() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_SelfReference.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final Order order = new Order( 10, "Bob" );
         final OrderItem item1 = new OrderItem( order,
                                                1 );
         final OrderItem item2 = new OrderItem( order,
                                                2 );
         final OrderItem anotherItem1 = new OrderItem( null,
                                                       3 );
         final OrderItem anotherItem2 = new OrderItem( null,
                                                       4 );
         workingMemory.insert( order );
         workingMemory.insert( item1 );
         workingMemory.insert( item2 );
         workingMemory.insert( anotherItem1 );
         workingMemory.insert( anotherItem2 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       results.size() );
         assertTrue( results.contains( item1 ) );
         assertTrue( results.contains( item2 ) );
     }
 
     public void testNumberComparisons() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NumberComparisons.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         // asserting the sensor object
         final RandomNumber rn = new RandomNumber();
         rn.setValue( 10 );
         workingMemory.insert( rn );
 
         final Guess guess = new Guess();
         guess.setValue( new Integer( 5 ) );
 
         final FactHandle handle = workingMemory.insert( guess );
 
         workingMemory.fireAllRules();
 
         // HIGHER
         assertEquals( 1,
                       list.size() );
         assertEquals( "HIGHER",
                       list.get( 0 ) );
 
         guess.setValue( new Integer( 15 ) );
         workingMemory.update( handle,
                               guess );
 
         workingMemory.fireAllRules();
 
         // LOWER
         assertEquals( 2,
                       list.size() );
         assertEquals( "LOWER",
                       list.get( 1 ) );
 
         guess.setValue( new Integer( 10 ) );
         workingMemory.update( handle,
                               guess );
 
         workingMemory.fireAllRules();
 
         // CORRECT
         assertEquals( 3,
                       list.size() );
         assertEquals( "CORRECT",
                       list.get( 2 ) );
 
     }
 
     public void testSkipModify() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_skipModify.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final Cheese cheese = new Cheese( "brie",
                                           10 );
         final FactHandle handle = workingMemory.insert( cheese );
 
         final Person bob = new Person( "bob",
                                        "stilton" );
         workingMemory.insert( bob );
 
         cheese.setType( "stilton" );
         workingMemory.update( handle,
                               cheese );
         workingMemory.fireAllRules();
         assertEquals( 2,
                       results.size() );
     }
 
     public void testEventModel() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EventModel.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory wm = ruleBase.newStatefulSession();
 
         final List agendaList = new ArrayList();
         final AgendaEventListener agendaEventListener = new AgendaEventListener() {
 
             public void activationCancelled(ActivationCancelledEvent event,
                                             WorkingMemory workingMemory) {
                 agendaList.add( event );
 
             }
 
             public void activationCreated(ActivationCreatedEvent event,
                                           WorkingMemory workingMemory) {
                 agendaList.add( event );
             }
 
             public void afterActivationFired(AfterActivationFiredEvent event,
                                              WorkingMemory workingMemory) {
                 agendaList.add( event );
             }
 
             public void agendaGroupPopped(AgendaGroupPoppedEvent event,
                                           WorkingMemory workingMemory) {
                 agendaList.add( event );
             }
 
             public void agendaGroupPushed(AgendaGroupPushedEvent event,
                                           WorkingMemory workingMemory) {
                 agendaList.add( event );
             }
 
             public void beforeActivationFired(BeforeActivationFiredEvent event,
                                               WorkingMemory workingMemory) {
                 agendaList.add( event );
             }
 
         };
 
         final List wmList = new ArrayList();
         final WorkingMemoryEventListener workingMemoryListener = new WorkingMemoryEventListener() {
 
             public void objectInserted(ObjectInsertedEvent event) {
                 wmList.add( event );
             }
 
             public void objectUpdated(ObjectUpdatedEvent event) {
                 wmList.add( event );
             }
 
             public void objectRetracted(ObjectRetractedEvent event) {
                 wmList.add( event );
             }
 
         };
 
         wm.addEventListener( workingMemoryListener );
 
         final Cheese stilton = new Cheese( "stilton",
                                            15 );
         final Cheese cheddar = new Cheese( "cheddar",
                                            17 );
 
         final FactHandle stiltonHandle = wm.insert( stilton );
 
         final ObjectInsertedEvent oae = (ObjectInsertedEvent) wmList.get( 0 );
         assertSame( stiltonHandle,
                     oae.getFactHandle() );
 
         wm.update( stiltonHandle,
                    stilton );
         final ObjectUpdatedEvent ome = (ObjectUpdatedEvent) wmList.get( 1 );
         assertSame( stiltonHandle,
                     ome.getFactHandle() );
 
         wm.retract( stiltonHandle );
         final ObjectRetractedEvent ore = (ObjectRetractedEvent) wmList.get( 2 );
         assertSame( stiltonHandle,
                     ore.getFactHandle() );
 
         wm.insert( cheddar );
     }
 
     public void testImplicitDeclarations() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_implicitDeclarations.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
         workingMemory.setGlobal( "factor",
                                  new Double( 1.2 ) );
 
         final Cheese cheese = new Cheese( "stilton",
                                           10 );
         workingMemory.insert( cheese );
 
         workingMemory.fireAllRules();
         assertEquals( 1,
                       results.size() );
     }
 
     public void testCastingInsideEvals() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_castsInsideEval.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.setGlobal( "value",
                                  new Integer( 20 ) );
 
         workingMemory.fireAllRules();
     }
 
     public void testMemberOfAndNotMemberOf() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_memberOf.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         final Cheese muzzarela = new Cheese( "muzzarela",
                                              10 );
         final Cheese brie = new Cheese( "brie",
                                         15 );
         workingMemory.insert( stilton );
         workingMemory.insert( muzzarela );
 
         final Cheesery cheesery = new Cheesery();
         cheesery.getCheeses().add( stilton.getType() );
         cheesery.getCheeses().add( brie.getType() );
         workingMemory.insert( cheesery );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( stilton,
                       list.get( 0 ) );
         assertEquals( muzzarela,
                       list.get( 1 ) );
     }
 
     public void testContainsInArray() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_contains_in_array.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Primitives p = new Primitives();
         p.setStringArray( new String[]{"test1", "test3"} );
         workingMemory.insert( p );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( "ok1",
                       list.get( 0 ) );
         assertEquals( "ok2",
                       list.get( 1 ) );
     }
 
     public void testCollectNodeSharing() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_collectNodeSharing.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         workingMemory.insert( new Cheese( "stilton",
                                           10 ) );
         workingMemory.insert( new Cheese( "brie",
                                           15 ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         assertEquals( 2,
                       ((List) list.get( 0 )).size() );
     }
 
     public void testNodeSharingNotExists() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_nodeSharingNotExists.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         assertEquals( "rule1",
                       list.get( 0 ) );
 
         workingMemory.insert( new Cheese( "stilton",
                                           10 ) );
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( "rule2",
                       list.get( 1 ) );
 
     }
 
     public void testNullBinding() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_nullBindings.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         workingMemory.insert( new Person( "bob" ) );
         workingMemory.insert( new Person( null ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         assertEquals( "OK",
                       list.get( 0 ) );
 
     }
 
     public void testModifyRetractWithFunction() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RetractModifyWithFunction.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final AbstractWorkingMemory workingMemory = (AbstractWorkingMemory) ruleBase.newStatefulSession();
 
         final Cheese stilton = new Cheese( "stilton",
                                            7 );
         final Cheese muzzarella = new Cheese( "muzzarella",
                                               9 );
         final int sum = stilton.getPrice() + muzzarella.getPrice();
         final FactHandle stiltonHandle = workingMemory.insert( stilton );
         final FactHandle muzzarellaHandle = workingMemory.insert( muzzarella );
 
         workingMemory.fireAllRules();
 
         assertEquals( sum,
                       stilton.getPrice() );
         assertEquals( 1,
                       workingMemory.getFactHandleMap().size() );
         assertNotNull( workingMemory.getObject( stiltonHandle ) );
         assertNotNull( workingMemory.getFactHandle( stilton ) );
 
         assertNull( workingMemory.getObject( muzzarellaHandle ) );
         assertNull( workingMemory.getFactHandle( muzzarella ) );
 
     }
 
     public void testConstraintConnectors() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ConstraintConnectors.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final Person youngChili1 = new Person( "young chili1" );
         youngChili1.setAge( 12 );
         youngChili1.setHair( "blue" );
         final Person youngChili2 = new Person( "young chili2" );
         youngChili2.setAge( 25 );
         youngChili2.setHair( "purple" );
 
         final Person chili1 = new Person( "chili1" );
         chili1.setAge( 35 );
         chili1.setHair( "red" );
 
         final Person chili2 = new Person( "chili2" );
         chili2.setAge( 38 );
         chili2.setHair( "indigigo" );
 
         final Person oldChili1 = new Person( "old chili2" );
         oldChili1.setAge( 45 );
         oldChili1.setHair( "green" );
 
         final Person oldChili2 = new Person( "old chili2" );
         oldChili2.setAge( 48 );
         oldChili2.setHair( "blue" );
 
         final Person veryold = new Person( "very old" );
         veryold.setAge( 99 );
         veryold.setHair( "gray" );
 
         workingMemory.insert( youngChili1 );
         workingMemory.insert( youngChili2 );
         workingMemory.insert( chili1 );
         workingMemory.insert( chili2 );
         workingMemory.insert( oldChili1 );
         workingMemory.insert( oldChili2 );
         workingMemory.insert( veryold );
 
         workingMemory.fireAllRules();
 
         assertEquals( 4,
                       results.size() );
         assertEquals( chili1,
                       results.get( 0 ) );
         assertEquals( oldChili1,
                       results.get( 1 ) );
         assertEquals( youngChili1,
                       results.get( 2 ) );
         assertEquals( veryold,
                       results.get( 3 ) );
 
     }
 
     public void testMatchesNotMatchesCheese() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_MatchesNotMatches.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "list",
                                  list );
 
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         final Cheese stilton2 = new Cheese( "stilton2",
                                             12 );
         final Cheese brie = new Cheese( "brie",
                                         10 );
         final Cheese brie2 = new Cheese( "brie2",
                                          10 );
         final Cheese muzzarella = new Cheese( "muzzarella",
                                               10 );
         final Cheese muzzarella2 = new Cheese( "muzzarella2",
                                                10 );
         workingMemory.insert( stilton );
         workingMemory.insert( stilton2 );
         workingMemory.insert( brie );
         workingMemory.insert( brie2 );
         workingMemory.insert( muzzarella );
         workingMemory.insert( muzzarella2 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       list.size() );
 
         assertEquals( stilton,
                       list.get( 0 ) );
         assertEquals( brie,
                       list.get( 1 ) );
     }
 
     public void testAutomaticBindings() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_AutoBindings.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final Person bob = new Person( "bob",
                                        "stilton" );
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         workingMemory.insert( bob );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         assertEquals( bob,
                       list.get( 0 ) );
     }
 
     public void testAutomaticBindingsErrors() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_AutoBindingsErrors.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         assertNotNull( pkg.getErrorSummary() );
     }
 
     public void testQualifiedFieldReference() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_QualifiedFieldReference.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final Person bob = new Person( "bob",
                                        "stilton" );
         final Cheese stilton = new Cheese( "stilton",
                                            12 );
         workingMemory.insert( bob );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
 
         assertEquals( bob,
                       list.get( 0 ) );
     }
 
     public void testEvalRewrite() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EvalRewrite.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final Order order1 = new Order( 10, "Bob" );
         final OrderItem item11 = new OrderItem( order1,
                                                 1 );
         final OrderItem item12 = new OrderItem( order1,
                                                 2 );
         order1.addItem( item11 );
         order1.addItem( item12 );
         final Order order2 = new Order( 11, "Bob" );
         final OrderItem item21 = new OrderItem( order2,
                                                 1 );
         final OrderItem item22 = new OrderItem( order2,
                                                 2 );
         order2.addItem( item21 );
         order2.addItem( item22 );
         final Order order3 = new Order( 12, "Bob" );
         final OrderItem item31 = new OrderItem( order3,
                                                 1 );
         final OrderItem item32 = new OrderItem( order3,
                                                 2 );
         order3.addItem( item31 );
         order3.addItem( item32 );
         final Order order4 = new Order( 13, "Bob" );
         final OrderItem item41 = new OrderItem( order4,
                                                 1 );
         final OrderItem item42 = new OrderItem( order4,
                                                 2 );
         order4.addItem( item41 );
         order4.addItem( item42 );
         workingMemory.insert( order1 );
         workingMemory.insert( item11 );
         workingMemory.insert( item12 );
         workingMemory.insert( order2 );
         workingMemory.insert( item21 );
         workingMemory.insert( item22 );
         workingMemory.insert( order3 );
         workingMemory.insert( item31 );
         workingMemory.insert( item32 );
         workingMemory.insert( order4 );
         workingMemory.insert( item41 );
         workingMemory.insert( item42 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 5,
                       list.size() );
         assertTrue( list.contains( item11 ) );
         assertTrue( list.contains( item12 ) );
         assertTrue( list.contains( item22 ) );
         assertTrue( list.contains( order3 ) );
         assertTrue( list.contains( order4 ) );
 
     }
 
     public void testMapAccess() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_MapAccess.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         Map map = new HashMap();
         map.put( "name",
                  "Edson" );
         map.put( "surname",
                  "Tirelli" );
         map.put( "age",
                  "28" );
 
         workingMemory.insert( map );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
         assertTrue( list.contains( map ) );
 
     }
 
     public void testHalt() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_halt.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Integer( 0 ) );
         workingMemory.fireAllRules();
 
         assertEquals( 10,
                       results.size() );
         for ( int i = 0; i < 10; i++ ) {
             assertEquals( new Integer( i ),
                           results.get( i ) );
         }
     }
 
     public void testFireLimit() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_fireLimit.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Integer( 0 ) );
         workingMemory.fireAllRules();
 
         assertEquals( 20,
                       results.size() );
         for ( int i = 0; i < 20; i++ ) {
             assertEquals( new Integer( i ),
                           results.get( i ) );
         }
         results.clear();
 
         workingMemory.insert( new Integer( 0 ) );
         workingMemory.fireAllRules( 10 );
 
         assertEquals( 10,
                       results.size() );
         for ( int i = 0; i < 10; i++ ) {
             assertEquals( new Integer( i ),
                           results.get( i ) );
         }
         results.clear();
 
         workingMemory.insert( new Integer( 0 ) );
         workingMemory.fireAllRules( -1 );
 
         assertEquals( 20,
                       results.size() );
         for ( int i = 0; i < 20; i++ ) {
             assertEquals( new Integer( i ),
                           results.get( i ) );
         }
         results.clear();
 
     }
 
     public void testGlobals2() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_globalsAsConstraints.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final List cheeseTypes = new ArrayList();
         workingMemory.setGlobal( "cheeseTypes",
                                  cheeseTypes );
         cheeseTypes.add( "stilton" );
         cheeseTypes.add( "muzzarela" );
 
         final Cheese stilton = new Cheese( "stilton",
                                            5 );
         workingMemory.insert( stilton );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       results.size() );
         assertEquals( "memberOf",
                       results.get( 0 ) );
 
         final Cheese brie = new Cheese( "brie",
                                         5 );
         workingMemory.insert( brie );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       results.size() );
         assertEquals( "not memberOf",
                       results.get( 1 ) );
     }
 
     public void testEqualitySupport() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_equalitySupport.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         RuleBaseConfiguration conf = new RuleBaseConfiguration();
         conf.setAssertBehaviour( RuleBaseConfiguration.AssertBehaviour.EQUALITY );
         final RuleBase ruleBase = getRuleBase( conf );
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         PersonWithEquals person = new PersonWithEquals( "bob",
                                                         30 );
 
         workingMemory.insert( person );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       results.size() );
         assertEquals( "mark",
                       results.get( 0 ) );
 
     }
 
     public void testCharComparisons() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_charComparisons.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         Primitives p1 = new Primitives();
         p1.setCharPrimitive( 'a' );
         p1.setStringAttribute( "b" );
         Primitives p2 = new Primitives();
         p2.setCharPrimitive( 'b' );
         p2.setStringAttribute( "a" );
 
         workingMemory.insert( p1 );
         workingMemory.insert( p2 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 3,
                       results.size() );
         assertEquals( "1",
                       results.get( 0 ) );
         assertEquals( "2",
                       results.get( 1 ) );
         assertEquals( "3",
                       results.get( 2 ) );
 
     }
 
     public void testAlphaNodeSharing() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_alphaNodeSharing.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBaseConfiguration conf = new RuleBaseConfiguration();
         conf.setShareAlphaNodes( false );
         final RuleBase ruleBase = getRuleBase( conf );
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         Person p1 = new Person( "bob",
                                 5 );
         workingMemory.insert( p1 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       results.size() );
         assertEquals( "1",
                       results.get( 0 ) );
         assertEquals( "2",
                       results.get( 1 ) );
 
     }
 
     public void testFunctionCallingFunction() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_functionCallingFunction.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       list.size() );
     }
 
     public void testSelfReference2() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_SelfReference2.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Cheese() );
 
         workingMemory.fireAllRules();
 
         assertEquals( 0,
                       results.size() );
     }
 
     public void testMergingDifferentPackages() throws Exception {
         // using the same builder
         try {
             final PackageBuilder builder = new PackageBuilder();
             builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes1.drl" ) ) );
             builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes2.drl" ) ) );
             fail( "Can't merge packages with different names " );
         } catch ( PackageMergeException e ) {
             // success
         } catch ( RuntimeException e ) {
             e.printStackTrace();
             fail( "unexpected exception: " + e.getMessage() );
         }
     }
 
     public void testMergingDifferentPackages2() throws Exception {
         // using different builders
         try {
             final PackageBuilder builder1 = new PackageBuilder();
             builder1.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes1.drl" ) ) );
             final Package pkg1 = builder1.getPackage();
 
             assertEquals( 1,
                           pkg1.getRules().length );
 
             final PackageBuilder builder2 = new PackageBuilder();
             builder2.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes2.drl" ) ) );
             final Package pkg2 = builder2.getPackage();
 
             assertEquals( 1,
                           pkg2.getRules().length );
 
             final RuleBase ruleBase = getRuleBase();
             ruleBase.addPackage( pkg1 );
             ruleBase.addPackage( pkg2 );
             final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
             final List results = new ArrayList();
             workingMemory.setGlobal( "results",
                                      results );
 
             workingMemory.insert( new Cheese( "stilton",
                                               10 ) );
             workingMemory.insert( new Cheese( "brie",
                                               5 ) );
 
             workingMemory.fireAllRules();
 
             assertEquals( results.toString(),
                           2,
                           results.size() );
             assertTrue( results.contains( "p1.r1" ) );
             assertTrue( results.contains( "p2.r1" ) );
 
         } catch ( PackageMergeException e ) {
             fail( "Should not raise exception when merging different packages into the same rulebase: " + e.getMessage() );
         } catch ( Exception e ) {
             e.printStackTrace();
             fail( "unexpected exception: " + e.getMessage() );
         }
     }
 
     public void testRuleReplacement() throws Exception {
         // test rule replacement
         try {
             final PackageBuilder builder1 = new PackageBuilder();
             builder1.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes1.drl" ) ) );
             builder1.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_RuleNameClashes3.drl" ) ) );
             final Package pkg1 = builder1.getPackage();
 
             assertEquals( 1,
                           pkg1.getRules().length );
 
             final RuleBase ruleBase = getRuleBase();
             ruleBase.addPackage( pkg1 );
             final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
             final List results = new ArrayList();
             workingMemory.setGlobal( "results",
                                      results );
 
             workingMemory.insert( new Cheese( "stilton",
                                               10 ) );
             workingMemory.insert( new Cheese( "brie",
                                               5 ) );
 
             workingMemory.fireAllRules();
 
             assertEquals( results.toString(),
                           0,
                           results.size() );
 
             workingMemory.insert( new Cheese( "muzzarella",
                                               7 ) );
 
             workingMemory.fireAllRules();
 
             assertEquals( results.toString(),
                           1,
                           results.size() );
             assertTrue( results.contains( "p1.r3" ) );
 
         } catch ( PackageMergeException e ) {
             fail( "Should not raise exception when merging different packages into the same rulebase: " + e.getMessage() );
         } catch ( Exception e ) {
             e.printStackTrace();
             fail( "unexpected exception: " + e.getMessage() );
         }
     }
 
     public void testOutOfMemory() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_OutOfMemory.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.insert( new Cheese( "stilton",
                                           1 ) );
 
         workingMemory.fireAllRules( 3000000 );
 
         // just for profiling
         //Thread.currentThread().wait();
     }
 
     public void testBindingsOnConnectiveExpressions() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_bindings.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Cheese( "stilton",
                                           15 ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       results.size() );
         assertEquals( "stilton",
                       results.get( 0 ) );
         assertEquals( new Integer( 15 ),
                       results.get( 1 ) );
     }
 
     public void testMultipleFroms() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_multipleFroms.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final Cheesery cheesery = new Cheesery();
         cheesery.addCheese( new Cheese( "stilton",
                                         15 ) );
         cheesery.addCheese( new Cheese( "brie",
                                         10 ) );
 
         workingMemory.setGlobal( "cheesery",
                                  cheesery );
 
         workingMemory.fireAllRules();
 
         assertEquals( 2,
                       results.size() );
         assertEquals( 2,
                       ((List) results.get( 0 )).size() );
         assertEquals( 2,
                       ((List) results.get( 1 )).size() );
     }
 
     public void testNullHashing() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NullHashing.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Cheese( "stilton",
                                           15 ) );
         workingMemory.insert( new Cheese( "",
                                           10 ) );
         workingMemory.insert( new Cheese( null,
                                           8 ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 3,
                       results.size() );
     }
 
     public void testDefaultBetaConstrains() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_DefaultBetaConstraint.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
         final FirstClass first = new FirstClass( "1",
                                                  "2",
                                                  "3",
                                                  "4",
                                                  "5" );
         final FactHandle handle = workingMemory.insert( first );
         workingMemory.fireAllRules();
         assertEquals( 1,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 0 ) );
 
         workingMemory.insert( new SecondClass() );
         workingMemory.update( handle,
                               first );
         workingMemory.fireAllRules();
         assertEquals( 2,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 1 ) );
 
         workingMemory.update( handle,
                               first );
         workingMemory.insert( new SecondClass( null,
                                                "2",
                                                "3",
                                                "4",
                                                "5" ) );
         workingMemory.fireAllRules();
         assertEquals( 3,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 2 ) );
 
         workingMemory.update( handle,
                               first );
         workingMemory.insert( new SecondClass( "1",
                                                null,
                                                "3",
                                                "4",
                                                "5" ) );
         workingMemory.fireAllRules();
         assertEquals( 4,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 3 ) );
 
         workingMemory.update( handle,
                               first );
         workingMemory.insert( new SecondClass( "1",
                                                "2",
                                                null,
                                                "4",
                                                "5" ) );
         workingMemory.fireAllRules();
         assertEquals( 5,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 4 ) );
 
         workingMemory.update( handle,
                               first );
         workingMemory.insert( new SecondClass( "1",
                                                "2",
                                                "3",
                                                null,
                                                "5" ) );
         workingMemory.fireAllRules();
         assertEquals( 6,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 5 ) );
 
         workingMemory.update( handle,
                               first );
         workingMemory.insert( new SecondClass( "1",
                                                "2",
                                                "3",
                                                "4",
                                                null ) );
         workingMemory.fireAllRules();
         assertEquals( 7,
                       results.size() );
         assertEquals( "NOT",
                       results.get( 6 ) );
 
         workingMemory.insert( new SecondClass( "1",
                                                "2",
                                                "3",
                                                "4",
                                                "5" ) );
         workingMemory.update( handle,
                               first );
         workingMemory.fireAllRules();
         assertEquals( 8,
                       results.size() );
         assertEquals( "EQUALS",
                       results.get( 7 ) );
 
     }
 
     public void testBooleanWrapper() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_BooleanWrapper.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         Primitives p1 = new Primitives();
         workingMemory.insert( p1 );
         workingMemory.fireAllRules();
         assertEquals( 0,
                       results.size() );
 
         Primitives p2 = new Primitives();
         p2.setBooleanWrapper( Boolean.FALSE );
         workingMemory.insert( p2 );
         workingMemory.fireAllRules();
         assertEquals( 0,
                       results.size() );
 
         Primitives p3 = new Primitives();
         p3.setBooleanWrapper( Boolean.TRUE );
         workingMemory.insert( p3 );
         workingMemory.fireAllRules();
         assertEquals( 1,
                       results.size() );
 
     }
 
     public void testCrossProductRemovingIdentityEquals() throws Exception {
         PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( MiscTest.class.getResourceAsStream( "test_CrossProductRemovingIdentityEquals.drl" ) ) );
 
         RuleBaseConfiguration conf = new RuleBaseConfiguration();
         conf.setShadowProxy( true );
         RuleBase rb = RuleBaseFactory.newRuleBase( conf );
         rb.addPackage( builder.getPackage() );
         StatefulSession session = rb.newStatefulSession();
 
         List list1 = new ArrayList();
         List list2 = new ArrayList();
 
         session.setGlobal( "list1",
                            list1 );
         session.setGlobal( "list2",
                            list2 );
 
         SpecialString first42 = new SpecialString( "42" );
         SpecialString second43 = new SpecialString( "42" );
         SpecialString world = new SpecialString( "World" );
         session.insert( world );
         session.insert( first42 );
         session.insert( second43 );
 
         //System.out.println( "Firing rules ..." );
 
         session.fireAllRules();
 
         assertEquals( 6,
                       list1.size() );
         assertEquals( 6,
                       list2.size() );
 
         assertEquals( second43,
                       list1.get( 0 ) );
         assertEquals( first42,
                       list1.get( 1 ) );
         assertEquals( world,
                       list1.get( 2 ) );
         assertEquals( second43,
                       list1.get( 3 ) );
         assertEquals( first42,
                       list1.get( 4 ) );
         assertEquals( world,
                       list1.get( 5 ) );
 
         assertEquals( first42,
                       list2.get( 0 ) );
         assertEquals( second43,
                       list2.get( 1 ) );
         assertEquals( second43,
                       list2.get( 2 ) );
         assertEquals( world,
                       list2.get( 3 ) );
         assertEquals( world,
                       list2.get( 4 ) );
         assertEquals( first42,
                       list2.get( 5 ) );
     }
 
     public void testIterateObjects() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_IterateObjects.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Cheese( "stilton",
                                           10 ) );
 
         workingMemory.fireAllRules();
 
         Iterator events = workingMemory.iterateObjects( new ClassObjectFilter( PersonInterface.class ) );
 
         assertTrue( events.hasNext() );
         assertEquals( 1,
                       results.size() );
         assertEquals( results.get( 0 ),
                       events.next() );
     }
 
     public void testNotInStatelessSession() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_NotInStatelessSession.drl" )) );
         final Package pkg = builder.getPackage();
 
         RuleBaseConfiguration conf = new RuleBaseConfiguration();
         conf.setSequential( true );
         final RuleBase ruleBase = getRuleBase(conf);
         ruleBase.addPackage( pkg );
 
         StatelessSession session = ruleBase.newStatelessSession();
         List list = new ArrayList();
         session.setGlobal( "list", list );
         session.execute( "not integer" );
         assertEquals("not integer", list.get( 0 ) );
     }
 
     public void testDynamicallyAddInitialFactRule() throws Exception {
         PackageBuilder builder = new PackageBuilder();
         String rule = "package org.drools.test\n global java.util.List list\n rule xxx\n when\n i:Integer()\nthen\n list.add(i);\nend";
         builder.addPackageFromDrl( new StringReader( rule ) );
         Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
 
         StatefulSession session = ruleBase.newStatefulSession();
         List list = new ArrayList();
         session.setGlobal( "list", list );
 
         session.insert( new Integer( 5) );
         session.fireAllRules();
 
         assertEquals( new Integer(5), list.get( 0 ) );
 
         builder = new PackageBuilder();
         rule = "package org.drools.test\n global java.util.List list\n rule xxx\n when\nthen\n list.add(\"x\");\nend";
         builder.addPackageFromDrl( new StringReader( rule ) );
         pkg = builder.getPackage();
 
         // Make sure that this rule is fired as the Package is updated, it also tests that InitialFactImpl is still in the network
         // even though the first rule didn't use it.
         ruleBase.addPackage( pkg );
 
         assertEquals( "x", list.get( 1 ) );
 
     }
 
     // FIXME
     public void FIXMEtestEvalRewriteWithSpecialOperators() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_EvalRewriteWithSpecialOperators.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List list = new ArrayList();
         workingMemory.setGlobal( "results",
                                  list );
 
         final Order order1 = new Order( 10, "Bob" );
         final OrderItem item11 = new OrderItem( order1, 1 );
         final OrderItem item12 = new OrderItem( order1, 2 );
         order1.addItem( item11 );
         order1.addItem( item12 );
         final Order order2 = new Order( 11, "Bob" );
         final OrderItem item21 = new OrderItem( order2, 1 );
         final OrderItem item22 = new OrderItem( order2, 2 );
         order2.addItem( item21 );
         order2.addItem( item22 );
         final Order order3 = new Order( 12, "Bob" );
         final OrderItem item31 = new OrderItem( order3, 1 );
         final OrderItem item32 = new OrderItem( order3, 2 );
         final OrderItem item33 = new OrderItem( order3, 3 );
         order3.addItem( item31 );
         order3.addItem( item32 );
         order3.addItem( item33 );
         final Order order4 = new Order( 13, "Bob" );
         final OrderItem item41 = new OrderItem( order4, 1 );
         final OrderItem item42 = new OrderItem( order4, 2 );
         order4.addItem( item41 );
         order4.addItem( item42 );
         final Order order5 = new Order( 14, "Mark" );
         final OrderItem item51 = new OrderItem( order5, 1 );
         final OrderItem item52 = new OrderItem( order5, 2 );
         order5.addItem( item51 );
         order5.addItem( item52 );
         workingMemory.insert( order1 );
         workingMemory.insert( item11 );
         workingMemory.insert( item12 );
         workingMemory.insert( order2 );
         workingMemory.insert( item21 );
         workingMemory.insert( item22 );
         workingMemory.insert( order3 );
         workingMemory.insert( item31 );
         workingMemory.insert( item32 );
         workingMemory.insert( item33 );
         workingMemory.insert( order4 );
         workingMemory.insert( item41 );
         workingMemory.insert( item42 );
         workingMemory.insert( order5 );
         workingMemory.insert( item51 );
         workingMemory.insert( item52 );
 
         workingMemory.fireAllRules();
 
         assertEquals( 9,
                       list.size() );
         int index=0;
         assertEquals( item11, list.get( index++ ) );
         assertEquals( item12, list.get( index++ ) );
         assertEquals( item21, list.get( index++ ) );
         assertEquals( item22, list.get( index++ ) );
         assertEquals( item31, list.get( index++ ) );
         assertEquals( item33, list.get( index++ ) );
         assertEquals( item41, list.get( index++ ) );
         assertEquals( order5, list.get( index++ ) );
         assertEquals( order5, list.get( index++ ) );
 
 
     }
 
     public void testImportColision() throws Exception {
 
         final PackageBuilder builder = new PackageBuilder();
         final PackageBuilder builder2 = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "nested1.drl" ) ) );
         builder2.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "nested2.drl" ) ) );
         final Package pkg = builder.getPackage();
         final Package pkg2 = builder2.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         ruleBase.addPackage( pkg2 );
 
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         workingMemory.insert( new FirstClass() );
         workingMemory.insert( new SecondClass() );
         workingMemory.insert( new FirstClass.AlternativeKey() );
         workingMemory.insert( new SecondClass.AlternativeKey() );
 
         workingMemory.fireAllRules();
     }
 
     public void testAutovivificationOfVariableRestrictions() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_AutoVivificationVR.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         workingMemory.insert( new Cheese( "stilton",
                                           10,
                                           8 ) );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       results.size() );
     }
 
     public void testShadowProxyOnCollections() throws Exception {
         final PackageBuilder builder = new PackageBuilder();
         builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_ShadowProxyOnCollections.drl" ) ) );
         final Package pkg = builder.getPackage();
 
         final RuleBase ruleBase = getRuleBase();
         ruleBase.addPackage( pkg );
         final WorkingMemory workingMemory = ruleBase.newStatefulSession();
 
         final List results = new ArrayList();
         workingMemory.setGlobal( "results",
                                  results );
 
         final Cheesery cheesery = new Cheesery();
         workingMemory.insert( cheesery );
 
         workingMemory.fireAllRules();
 
         assertEquals( 1,
                       results.size() );
         assertEquals( 1,
                       cheesery.getCheeses().size() );
         assertEquals( results.get( 0 ),
                       cheesery.getCheeses().get( 0 ) );
     }
 
 }
