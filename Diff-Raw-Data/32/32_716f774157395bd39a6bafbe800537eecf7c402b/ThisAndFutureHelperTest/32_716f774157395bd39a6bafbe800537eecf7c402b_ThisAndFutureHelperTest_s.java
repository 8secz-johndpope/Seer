 /*
  * Copyright 2007 Open Source Applications Foundation
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.osaf.cosmo.model.util;
 
 import java.io.FileInputStream;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.Set;
 
 import junit.framework.Assert;
 import junit.framework.TestCase;
 import net.fortuna.ical4j.data.CalendarBuilder;
 import net.fortuna.ical4j.model.Calendar;
 import net.fortuna.ical4j.model.Component;
 import net.fortuna.ical4j.model.ComponentList;
 import net.fortuna.ical4j.model.Date;
 import net.fortuna.ical4j.model.DateTime;
 import net.fortuna.ical4j.model.Recur;
 import net.fortuna.ical4j.model.TimeZone;
 import net.fortuna.ical4j.model.TimeZoneRegistry;
 import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
 import net.fortuna.ical4j.model.component.VEvent;
 
 import org.osaf.cosmo.model.EventExceptionStamp;
 import org.osaf.cosmo.model.EventStamp;
 import org.osaf.cosmo.model.ModificationUid;
 import org.osaf.cosmo.model.NoteItem;
 
 /**
  * Test EventStamp
  */
 public class ThisAndFutureHelperTest extends TestCase {
    
     
     protected String baseDir = "src/test/unit/resources/testdata/thisandfuture/";
     private static final TimeZoneRegistry TIMEZONE_REGISTRY =
         TimeZoneRegistryFactory.getInstance().createRegistry();
 
     
     public void testBreakFloatingSeries() throws Exception {
         Calendar cal1 = getCalendar("thisandfuturetest_floating.ics");
         Calendar cal2 = getCalendar("thisandfuturetest_floating_changed.ics");
         
         NoteItem oldSeries = createEvent("oldmaster", cal1);
         NoteItem newSeries = createEvent("newmaster", cal2);
         
         Date lastRecurrenceId = new DateTime("20070808T081500");
         
         ThisAndFutureHelper helper = new ThisAndFutureHelper();
         
         Set<NoteItem> results = 
             helper.breakRecurringEvent(oldSeries, newSeries, lastRecurrenceId);
         
         EventStamp eventStamp = EventStamp.getStamp(oldSeries);
         Recur recur = eventStamp.getRecurrenceRules().get(0);
         
        Assert.assertEquals(new DateTime("20070808T081459"), recur.getUntil());
         
         Assert.assertEquals(6, results.size());
         
         assertContains("oldmaster:20070808T081500", results, false);
         assertContains("oldmaster:20070809T081500", results, false);
         assertContains("oldmaster:20070810T081500", results, false);
         assertContains("newmaster:20070808T081500", results, true);
         assertContains("newmaster:20070809T081500", results, true);
         assertContains("newmaster:20070810T081500", results, true);
     }
     
     public void testBreakFloatingSeriesWithTimeShift() throws Exception {
         Calendar cal1 = getCalendar("thisandfuturetest_floating.ics");
         Calendar cal2 = getCalendar("thisandfuturetest_floating_changed_timeshift.ics");
         
         NoteItem oldSeries = createEvent("oldmaster", cal1);
         NoteItem newSeries = createEvent("newmaster", cal2);
         
         Date lastRecurrenceId = new DateTime("20070808T081500");
         
         ThisAndFutureHelper helper = new ThisAndFutureHelper();
         
         Set<NoteItem> results = 
             helper.breakRecurringEvent(oldSeries, newSeries, lastRecurrenceId);
         
         EventStamp eventStamp = EventStamp.getStamp(oldSeries);
         Recur recur = eventStamp.getRecurrenceRules().get(0);
         
        Assert.assertEquals(new DateTime("20070808T081459"), recur.getUntil());
         
         Assert.assertEquals(6, results.size());
         
         assertContains("oldmaster:20070808T081500", results, false);
         assertContains("oldmaster:20070809T081500", results, false);
         assertContains("oldmaster:20070810T081500", results, false);
         assertContains("newmaster:20070808T101500", results, true);
         assertContains("newmaster:20070809T101500", results, true);
         assertContains("newmaster:20070810T101500", results, true);
     }
     
     public void testBreakTimeZoneSeries() throws Exception {
         Calendar cal1 = getCalendar("thisandfuturetest_timezone.ics");
         Calendar cal2 = getCalendar("thisandfuturetest_timezone_changed.ics");
         
         NoteItem oldSeries = createEvent("oldmaster", cal1);
         NoteItem newSeries = createEvent("newmaster", cal2);
         
         TimeZone tz = TIMEZONE_REGISTRY.getTimeZone("America/Chicago");
        
         Date lastRecurrenceId = new DateTime("20070809T084500", tz);
         
         ThisAndFutureHelper helper = new ThisAndFutureHelper();
         
         Set<NoteItem> results = 
             helper.breakRecurringEvent(oldSeries, newSeries, lastRecurrenceId);
         
         EventStamp eventStamp = EventStamp.getStamp(oldSeries);
         Recur recur = eventStamp.getRecurrenceRules().get(0);
         
        Assert.assertEquals(new DateTime("20070809T084459", tz), recur.getUntil());
         
         Assert.assertEquals(4, results.size());
         
         assertContains("oldmaster:20070809T134500Z", results, false);
         assertContains("oldmaster:20070816T134500Z", results, false);
        
         assertContains("newmaster:20070809T134500Z", results, true);
         assertContains("newmaster:20070816T134500Z", results, true);
         
     }
     
     public void testBreakAllDaySeries() throws Exception {
         Calendar cal1 = getCalendar("thisandfuturetest_allday.ics");
         Calendar cal2 = getCalendar("thisandfuturetest_allday_changed.ics");
         
         NoteItem oldSeries = createEvent("oldmaster", cal1);
         NoteItem newSeries = createEvent("newmaster", cal2);
         
         Date lastRecurrenceId = new Date("20070808");
         
         ThisAndFutureHelper helper = new ThisAndFutureHelper();
         
         Set<NoteItem> results = 
             helper.breakRecurringEvent(oldSeries, newSeries, lastRecurrenceId);
         
         EventStamp eventStamp = EventStamp.getStamp(oldSeries);
         Recur recur = eventStamp.getRecurrenceRules().get(0);
         
         Assert.assertEquals(new Date("20070807"), recur.getUntil());
         
         Assert.assertEquals(4, results.size());
         
         assertContains("oldmaster:20070809", results, false);
         assertContains("oldmaster:20070810", results, false);
         
         assertContains("newmaster:20070809", results, true);
         assertContains("newmaster:20070810", results, true);
     }
     
     protected void assertContains(String uid, Collection<NoteItem> notes, boolean isActive) {
         for(NoteItem note: notes)
             if(note.getUid().equals(uid) && note.getIsActive()==isActive)
                     return;
             
         Assert.fail(uid + " not in collection");
     }
     
     protected Calendar getCalendar(String name) throws Exception {
         CalendarBuilder cb = new CalendarBuilder();
         FileInputStream fis = new FileInputStream(baseDir + name);
         Calendar calendar = cb.build(fis);
         return calendar;
     }
     
     protected NoteItem createEvent(String uid, Calendar calendar) {
         NoteItem master = new NoteItem();
         master.setUid(uid);
         EventStamp es = new EventStamp(master);
         master.addStamp(es);
         
         ComponentList vevents = calendar.getComponents().getComponents(
                 Component.VEVENT);
         
         ArrayList<VEvent> exceptions = new ArrayList<VEvent>();
         
         // get list of exceptions (VEVENT with RECURRENCEID)
         for (Iterator<VEvent> i = vevents.iterator(); i.hasNext();) {
             VEvent event = i.next();
             if (event.getRecurrenceId() != null) {
                 exceptions.add(event);
                 NoteItem mod = new NoteItem();
                 mod.setUid(new ModificationUid(master,event.getRecurrenceId().getDate()).toString());
                 mod.setModifies(master);
                 master.getModifications().add(mod);
                 EventExceptionStamp ees = new EventExceptionStamp(mod);
                 mod.addStamp(ees);
                 ees.createCalendar();
                 ees.setRecurrenceId(event.getRecurrenceId().getDate());
             } 
         }
         
         
         for(VEvent ex: exceptions)
             calendar.getComponents().remove(ex);
         
         es.setEventCalendar(calendar);
         
         return master;
     }
     
     
 }
