 /**
  * Copyright 2011 Green Energy Corp.
  * 
  * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
  * contributor license agreements. See the NOTICE file distributed with this
  * work for additional information regarding copyright ownership. Green Energy
  * Corp licenses this file to you under the GNU Affero General Public License
  * Version 3.0 (the "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  * 
  * http://www.gnu.org/licenses/agpl.html
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package org.totalgrid.reef.integration;
 
 import org.junit.Test;
 import org.totalgrid.reef.japi.ReefServiceException;
 import org.totalgrid.reef.japi.client.SubscriptionCreationListener;
 import org.totalgrid.reef.japi.request.*;
 import org.totalgrid.reef.integration.helpers.BlockingQueue;
 import org.totalgrid.reef.integration.helpers.ReefConnectionTestBase;
 import org.totalgrid.reef.integration.helpers.MockSubscriptionEventAcceptor;
 import org.totalgrid.reef.japi.client.Subscription;
 import org.totalgrid.reef.japi.client.SubscriptionResult;
 import org.totalgrid.reef.proto.Alarms;
 import org.totalgrid.reef.proto.Alarms.Alarm;
 import org.totalgrid.reef.proto.Events;
 import org.totalgrid.reef.proto.Model;
 import org.totalgrid.reef.proto.Utils;
 
 import java.util.LinkedList;
 import java.util.List;
 
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertNotSame;
 import static org.junit.Assert.assertTrue;
 
 public class TestEventService extends ReefConnectionTestBase
 {
     @Test
     public void validateLongEventConfigStrings() throws ReefServiceException
     {
         EventConfigService configService = helpers;
         EventCreationService es = helpers;
 
         StringBuilder sb = new StringBuilder();
         for ( int i = 0; i < 1000; i++ )
             sb.append( "a" );
         String longString = sb.toString();
 
         Alarms.EventConfig config = configService.setEventConfigAsEvent( "Test.EventSuperLong", 1, longString );
         assertEquals( longString, config.getResource() );
 
         Events.Event event = es.publishEvent( "Test.EventSuperLong", "Tests", new LinkedList<Utils.Attribute>() );
         assertEquals( longString, event.getRendered() );
         assertTrue( event.hasUid() );
         assertNotSame( 0, event.getUid().length() );
 
         assertTrue( event.hasTime() );
         assertTrue( event.getTime() > 0 );
 
        configService.deleteEventConfig(config);
     }
 
     @Test
     public void prepareEvents() throws ReefServiceException
     {
         EventCreationService es = helpers;
         EntityService entityService = helpers;
         EventConfigService configService = helpers;
 
         // make an event type for our test events
         configService.setEventConfigAsEvent( "Test.Event", 1, "Event" );
 
         Model.Entity entity = entityService.getEntityByName( "StaticSubstation.Line02.Current" );
 
         // populate some events
         for ( int i = 0; i < 15; i++ )
         {
             Events.Event e = es.publishEvent( "Test.Event", "Tests", entity.getUuid() );
             assertTrue( e.hasUid() );
             assertNotSame( 0, e.getUid().length() );
 
             assertTrue( e.hasTime() );
             assertTrue( e.getTime() > 0 );
         }
     }
 
     @Test
     public void getRecentEvents() throws ReefServiceException
     {
         EventService es = helpers;
         List<Events.Event> events = es.getRecentEvents( 10 );
         assertEquals( events.size(), 10 );
     }
 
     @Test
     public void subscribeEvents() throws ReefServiceException, InterruptedException
     {
 
         MockSubscriptionEventAcceptor<Events.Event> mock = new MockSubscriptionEventAcceptor<Events.Event>( true );
 
         EventService es = helpers;
 
         SubscriptionResult<List<Events.Event>, Events.Event> events = es.subscribeToRecentEvents( 10 );
         assertEquals( events.getResult().size(), 10 );
 
         EventCreationService pub = helpers;
 
         pub.publishEvent( "Test.Event", "Tests", getUUID( "StaticSubstation.Line02.Current" ) );
 
         events.getSubscription().start( mock );
 
         mock.pop( 1000 );
 
 
     }
 
     private Model.ReefUUID getUUID( String name ) throws ReefServiceException
     {
         EntityService es = helpers;
         Model.Entity e = es.getEntityByName( name );
         return e.getUuid();
     }
 
     @Test
     public void prepareAlarms() throws ReefServiceException
     {
 
         // make an event type for our test alarms
         EventConfigService configService = helpers;
         configService.setEventConfigAsAlarm( "Test.Alarm", 1, "Alarm", true );
 
         EventCreationService es = helpers;
 
         // populate some alarms
         for ( int i = 0; i < 5; i++ )
         {
             es.publishEvent( "Test.Alarm", "Tests", getUUID( "StaticSubstation.Line02.Current" ) );
         }
     }
 
     @Test
     public void subscribeAlarms() throws ReefServiceException, InterruptedException
     {
 
         MockSubscriptionEventAcceptor<Alarm> mock = new MockSubscriptionEventAcceptor<Alarm>( true );
 
         AlarmService as = helpers;
 
         SubscriptionResult<List<Alarm>, Alarm> result = as.subscribeToActiveAlarms( 2 );
         List<Alarm> events = result.getResult();
         assertEquals( events.size(), 2 );
 
         EventCreationService pub = helpers;
 
         pub.publishEvent( "Test.Alarm", "Tests", getUUID( "StaticSubstation.Line02.Current" ) );
 
         result.getSubscription().start( mock );
         mock.pop( 1000 );
     }
 
     @Test
     public void subscriptionCreationCallback() throws ReefServiceException, InterruptedException
     {
 
         final BlockingQueue<Subscription<?>> callback = new BlockingQueue<Subscription<?>>();
 
         EventService es = helpers;
 
         es.addSubscriptionCreationListener( new SubscriptionCreationListener() {
             @Override
             public void onSubscriptionCreated( Subscription<?> sub )
             {
                 callback.push( sub );
             }
         } );
 
         SubscriptionResult<List<Events.Event>, Events.Event> result = es.subscribeToRecentEvents( 1 );
         List<Events.Event> events = result.getResult();
         assertEquals( events.size(), 1 );
 
         assertEquals( callback.pop( 1000 ), result.getSubscription() );
     }
 
 
     @Test
     public void cleanupEventConfigs() throws ReefServiceException
     {
         EventConfigService configService = helpers;
        configService.deleteEventConfig(configService.getEventConfiguration("Test.Event"));
        configService.deleteEventConfig(configService.getEventConfiguration("Test.Alarm"));
     }
 
 }
