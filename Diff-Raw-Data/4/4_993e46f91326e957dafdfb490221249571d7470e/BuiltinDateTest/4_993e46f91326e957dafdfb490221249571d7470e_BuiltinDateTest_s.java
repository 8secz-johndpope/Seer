 package org.dynjs.runtime.builtins.types;
 
 import org.dynjs.runtime.AbstractDynJSTestSupport;
 import org.dynjs.runtime.JSFunction;
 import org.joda.time.DateTime;
 import org.joda.time.DateTimeUtils;
 import org.joda.time.DateTimeZone;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 import static org.fest.assertions.Assertions.assertThat;
 
 public class BuiltinDateTest extends AbstractDynJSTestSupport {
 
     private final long fixedInstant = 1347051329670L;
     private final DateTimeZone defaultTimeZone = DateTimeZone.getDefault();
 
     @Before
     public void setup() {
         DateTimeUtils.setCurrentMillisFixed(fixedInstant);
         DateTimeZone.setDefault(DateTimeZone.forID("Brazil/East"));
     }
 
     @After
     public void tearDown() {
         DateTimeUtils.setCurrentMillisSystem();
         DateTimeZone.setDefault(defaultTimeZone);
     }
 
     @Test
     public void testDate() {
         assertThat(eval("Date"))
                 .isNotNull()
                 .isInstanceOf(JSFunction.class);
     }
 
     @Test
     public void testDateNow() {
         assertThat(eval("Date.now()"))
                 .isNotNull()
                 .isEqualTo(fixedInstant);
     }
 
     @Test
     public void testDateParse() {
         assertThat(eval("Date.parse('2012-09-07T18:00:00.000Z')"))
                 .isEqualTo(DateTime.parse("2012-09-07T18:00:00.000Z").toDateTime(DateTimeZone.UTC).getMillis());
     }
 
     @Test
     public void testDateValueOf() {
         assertThat(eval("new Date().valueOf()")).isEqualTo(fixedInstant);
     }
 
     @Test
     public void testDateToISOString() {
         assertThat(eval("new Date().toISOString()")).isEqualTo("2012-09-07T17:55:29.670-03:00");
     }
 
     @Test
     public void testDateToString() {
         assertThat(eval("new Date().toString()")).isEqualTo("Fri Sep 07 2012 17:55:29 GMT-0300 (BRT)");
     }
 
     @Test
     public void testDateToDateString() {
         assertThat(eval("new Date().toDateString()")).isEqualTo("Fri Sep 07 2012");
     }
 
     @Test
     public void testDateToTimeString() {
         assertThat(eval("new Date().toTimeString()")).isEqualTo("17:55:29 GMT-0300 (BRT)");
     }
 
 }
