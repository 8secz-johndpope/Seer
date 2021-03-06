 package org.drools.eclipse.editors.completion;
 
 import org.drools.lang.Location;
 
 import junit.framework.TestCase;
 
 /**
  * Test to check the location determination when doing code completion inside
  * rule condtions.
  * 
  * @author <a href="mailto:kris_verlaenen@hotmail.com">kris verlaenen </a>
  *
  */
 public class LocationDeterminatorTest extends TestCase {
 
     public void testColumnOperatorPattern() {
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(    property ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property   ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( name : property ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(name:property ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(    name:property ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( name:property   ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(   name  :  property  ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property1 == \"value\", property2 ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property1 == \"value\", name : property2 ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property1 == \"value\", name:property2 ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property1 == \"value\",   name  :  property2  ").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(    prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( name:prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(name:prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( name : prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(   name  :  prop").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property <= ").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( name : property == ").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("(property==").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property contains ").matches());
     	assertFalse(LocationDeterminator.PATTERN_PATTERN_OPERATOR.matcher("( property1 == \"value\", property2 >= ").matches());
     }
 
     public void testColumnArgumentPattern() {
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property == ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property >= ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("(property== ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("(   property   ==   ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( name : property == ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("(name:property== ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("(  name  :  property  ==  ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property1 == \"value\", property2 == ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property1 == \"value\",property2== ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property1 == \"value\",  property2  ==  ").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("( property == otherProp").matches());
     	assertTrue(LocationDeterminator.PATTERN_PATTERN_COMPARATOR_ARGUMENT.matcher("(property==otherProp").matches());
     }
     
     public void testCheckLHSLocationDetermination() {
         String input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		";
         Location location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class( condition == true ) \n" +
         	"		";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		class: Class( condition == true, condition2 == null ) \n" +
         	"		";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class( condition == true ) \n" +
         	"		Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		class: Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		class:Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** Inside of condition: start */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( na";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("na", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name.subProperty['test'].subsu";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("name.subProperty['test'].subsu", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( condition == true, ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( condition == true, na";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( \n" +
         	"			";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( condition == true, \n" +
         	"			";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( c: condition, \n" +
         	"			";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name : ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name: ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name:";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     
         /** Inside of condition: Operator */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class(property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name : property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class (name:property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class (name:property   ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name1 : property1, name : property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name1 : property1 == \"value\", name : property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name1 : property1 == \"value\",property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name1 : property1, \n" + 
         	"			name : property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         /** Inside of condition: argument */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property== ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name : property <= ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("<=", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name:property != ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("!=", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name1 : property1, property2 == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property2", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class (name:property== ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property == otherPropertyN";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property == \"someth";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property contains ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("contains", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property excludes ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("excludes", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property matches \"prop";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("matches", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property in ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("in", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
        
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property in ('1', '2') ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property in ('1', '2'), ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not in ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("in", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
        
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not in ('1', '2') ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not in ('1', '2'), ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property memberOf ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("memberOf", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
        
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property memberOf collection ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property memberOf collection, ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not memberOf ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("memberOf", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
        
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not memberOf collection ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property not memberOf collection, ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         
         
         /** EXISTS */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists(";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists ( Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists ( name : Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** NOT */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_NOT, location.getType());
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_NOT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not exists ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not exists Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         // TODO        
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not exists Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not exists name : Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		not Class () \n" +
         	"		";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
     
         /** AND */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) &&  ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class () and   ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		name : Class ( name: property ) and ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name: property ) \n" + 
         	"       and ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and name : Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) && name : Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and not Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and exists Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and Class ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and Class ( name ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("name", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and Class ( name == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class ( ) and not ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_NOT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class ( ) and exists ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) and not Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** OR */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) || ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class () or   ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
     
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		name : Class ( name: property ) or ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name: property ) \n" + 
         	"       or ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or name : Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) || name : Cl";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_AND_OR, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or Class ( ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or Class ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or Class ( name ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("name", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( ) or Class ( name == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class ( ) or not ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_NOT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		exists Class ( ) or exists ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION_EXISTS, location.getType());
 
         /** EVAL */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval(";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( myCla";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("myCla", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getMetho";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getMetho", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getMethod(";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getMethod(", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getMethod().get";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getMethod().get", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getMethod(\"someStringWith)))\").get";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getMethod(\"someStringWith)))\").get", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getMethod(\"someStringWith(((\").get";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getMethod(\"someStringWith(((\").get", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( true )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getProperty(name).isTrue() )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getProperty(\"someStringWith(((\").isTrue() )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getProperty((((String) s) )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_EVAL, location.getType());
         assertEquals("param.getProperty((((String) s) )", location.getProperty(Location.LOCATION_EVAL_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( param.getProperty((((String) s))))";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		eval( true ) \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** MULTIPLE RESTRICTIONS */
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 && ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name : property1, property2 > 0 && ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property2", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property1 < 20, property2 > 0 && ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property2", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 && < ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("<", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 && < 10 ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 && < 10, ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 || ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 || \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( name : property1, property2 > 0 || ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property2", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property1 < 20, property2 > 0 || ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property2", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 && < 10 ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 || < 10 ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property == \"test\" || == \"test2\" ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_END, location.getType());
 
         /** FROM */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) fr";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM, location.getType());
         assertEquals("", location.getProperty(Location.LOCATION_FROM_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from myGlob";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM, location.getType());
         assertEquals("myGlob", location.getProperty(Location.LOCATION_FROM_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from myGlobal.get";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM, location.getType());
         assertEquals("myGlobal.get", location.getProperty(Location.LOCATION_FROM_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from myGlobal.getList() \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from getDroolsFunction() \n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** FROM ACCUMULATE */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate(";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total = 0; ), \n" +
         	"			action( total += $cheese.getPrice(); ), \n" +
         	"           result( new Integer( total ) ) \n" +
         	"		) \n" +
         	"		";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_INIT_INSIDE, location.getType());
         assertEquals("", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total = 0; ), \n" +
         	"			action( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_ACTION_INSIDE, location.getType());
         assertEquals(" int total = 0; ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
         assertEquals("", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_ACTION_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total = 0; ), \n" +
         	"			action( total += $cheese.getPrice(); ), \n" +
         	"           result( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_RESULT_INSIDE, location.getType());
         assertEquals(" int total = 0; ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
         assertEquals(" total += $cheese.getPrice(); ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_ACTION_CONTENT));
         assertEquals("", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_RESULT_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total =";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_INIT_INSIDE, location.getType());
         assertEquals("int total =", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total = 0; ), \n" +
         	"			action( total += $ch";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_ACTION_INSIDE, location.getType());
         assertEquals(" int total = 0; ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
         assertEquals("total += $ch", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_ACTION_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == $likes ), \n" +
         	"			init( int total = 0; ), \n" +
         	"			action( total += $cheese.getPrice(); ), \n" +
         	"           result( new Integer( tot";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_ACCUMULATE_RESULT_INSIDE, location.getType());
         assertEquals(" int total = 0; ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_INIT_CONTENT));
         assertEquals(" total += $cheese.getPrice(); ", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_ACTION_CONTENT));
         assertEquals("new Integer( tot", location.getProperty(Location.LOCATION_PROPERTY_FROM_ACCUMULATE_RESULT_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("type", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from accumulate( \n" +
         	"			$cheese : Cheese( type == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("type", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         /** FROM COLLECT */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_COLLECT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect(";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM_COLLECT, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect ( \n" +
         	"			Cheese( type == $likes )" +
         	"		) \n" +
         	"		";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect ( \n" +
         	"			Cheese( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect ( \n" +
         	"			Cheese( type ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("type", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		Class ( property > 0 ) from collect ( \n" +
         	"			Cheese( type == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Cheese", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("type", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         
         /** NESTED FROM */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		ArrayList(size > 50) from collect( Person( disabled == \"yes\", income > 100000 ) ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		ArrayList(size > 50) from collect( Person( disabled == \"yes\", income > 100000 ) from ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		ArrayList(size > 50) from collect( Person( disabled == \"yes\", income > 100000 ) from town.getPersons() )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		ArrayList(size > 50) from accumulate( Person( disabled == \"yes\", income > 100000 ) ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		ArrayList(size > 50) from accumulate( Person( disabled == \"yes\", income > 100000 ) from ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_FROM, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
        	"		ArrayList(size > 50) from accumulate( Person( disabled == \"yes\", $i : income > 100000 ) from town.getPersons(), max( $i ) )";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         /** FORALL */
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		forall ( ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		forall ( " +
         	"           Class ( pr";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("pr", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		forall ( " +
         	"           Class ( property ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_OPERATOR, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		forall ( " +
         	"           Class ( property == ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_ARGUMENT, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
         assertEquals("property", location.getProperty(Location.LOCATION_PROPERTY_PROPERTY_NAME));
         assertEquals("==", location.getProperty(Location.LOCATION_PROPERTY_OPERATOR));
 
         input = 
         	"rule MyRule \n" +
         	"	when \n" +
         	"		forall ( " +
         	"           Class ( property == \"test\")" +
         	"           C";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
     }
     
     public void testCheckRHSLocationDetermination() {
         String input = 
         	"rule MyRule \n" +
         	"	when\n" +
         	"		Class ( )\n" +
         	"   then\n" +
         	"       ";
         Location location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RHS, location.getType());
         assertEquals("", location.getProperty(Location.LOCATION_RHS_CONTENT));
         
         input = 
         	"rule MyRule \n" +
         	"	when\n" +
         	"		Class ( )\n" +
         	"   then\n" +
         	"       assert(null);\n" +
         	"       ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RHS, location.getType());
         assertEquals("assert(null);\n       ", location.getProperty(Location.LOCATION_RHS_CONTENT));
 
         input = 
         	"rule MyRule \n" +
         	"	when\n" +
         	"		Class ( )\n" +
         	"   then\n" +
         	"       meth";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RHS, location.getType());
         assertEquals("meth", location.getProperty(Location.LOCATION_RHS_CONTENT));
     }
         
     public void testCheckRuleHeaderLocationDetermination() {
         String input = 
         	"rule MyRule ";
         Location location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RULE_HEADER, location.getType());
         
         input = 
         	"rule MyRule \n" +
         	"	salience 12 activation-group \"my";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RULE_HEADER, location.getType());
 
         // KRISV: still can't make this work... apparently, ANTLR is trying to recover from
         // the error (unkown token) by deleting the token. I don't know why it continues to 
         // execute actions though, if the EOF is found.
 //        input = 
 //        	"rule \"Hello World\" ruleflow-group \"hello\" s";
 //        location = LocationDeterminator.getLocation(input);
 //        assertEquals(Location.LOCATION_RULE_HEADER, location.getType());
     }
     
     public void testCheckQueryLocationDetermination() {
         String input = 
         	"query MyQuery ";
         Location location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RULE_HEADER, location.getType());
         
         input = 
         	"query \"MyQuery\" ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_RULE_HEADER, location.getType());
         
         input = 
             "query MyQuery() ";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_BEGIN_OF_CONDITION, location.getType());
         
         input = 
         	"query MyQuery \n" +
         	"	Class (";
         location = LocationDeterminator.getLocation(input);
         assertEquals(Location.LOCATION_LHS_INSIDE_CONDITION_START, location.getType());
         assertEquals("Class", location.getProperty(Location.LOCATION_PROPERTY_CLASS_NAME));
     }
 
 }
