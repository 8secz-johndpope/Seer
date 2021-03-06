 /*
   * JBoss, Home of Professional Open Source
   * Copyright 2005, JBoss Inc., and individual contributors as indicated
   * by the @authors tag. See the copyright.txt in the distribution for a
   * full listing of individual contributors.
   *
   * This is free software; you can redistribute it and/or modify it
   * under the terms of the GNU Lesser General Public License as
   * published by the Free Software Foundation; either version 2.1 of
   * the License, or (at your option) any later version.
   *
   * This software is distributed in the hope that it will be useful,
   * but WITHOUT ANY WARRANTY; without even the implied warranty of
   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   * Lesser General Public License for more details.
   *
   * You should have received a copy of the GNU Lesser General Public
   * License along with this software; if not, write to the Free
   * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
   * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
   */
 package org.jboss.test.messaging.jms.message;
 
 import java.io.Serializable;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.HashSet;
 
 import javax.naming.InitialContext;
 import javax.jms.Destination;
 import javax.jms.Connection;
 import javax.jms.Session;
 import javax.jms.MessageProducer;
 import javax.jms.MessageConsumer;
 import javax.jms.DeliveryMode;
 import javax.jms.Message;
 import javax.jms.ConnectionFactory;
 import javax.jms.MessageNotWriteableException;
 import javax.jms.MessageFormatException;
 import javax.jms.JMSException;
 import javax.jms.BytesMessage;
 import javax.jms.MapMessage;
 import javax.jms.ObjectMessage;
 import javax.jms.StreamMessage;
 import javax.jms.TextMessage;
 import javax.jms.MessageEOFException;
 
 import org.jboss.jms.destination.JBossQueue;
 import org.jboss.jms.message.JBossMessage;
 import org.jboss.jms.message.JBossBytesMessage;
 import org.jboss.jms.message.JBossMapMessage;
 import org.jboss.jms.message.JBossObjectMessage;
 import org.jboss.jms.message.JBossStreamMessage;
 import org.jboss.jms.message.JBossTextMessage;
 import org.jboss.jms.message.MessageProxy;
 import org.jboss.test.messaging.MessagingTestCase;
 import org.jboss.test.messaging.tools.ServerManagement;
 
 /**
  * Base class for all tests concerning message headers, properties, etc.
  *
  * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
  * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
  * @version <tt>$Revision$</tt>
  *
  * $Id$
  */
 public class MessageTest extends MessagingTestCase
 {
    // Constants -----------------------------------------------------
 
    // Static --------------------------------------------------------
 
    /**
     * Loads the message header fields with significant values.
     */
    public static void configureMessage(JBossMessage m) throws JMSException
    {
       //m.setJMSMessageID("messageID777");
       m.setMessageId(123456);
       m.setJMSTimestamp(123456789l);
       m.setJMSCorrelationID("correlationID777");
       m.setJMSReplyTo(new JBossQueue("ReplyToQueue"));
       m.setJMSDestination(new JBossQueue("DestinationQueue"));
       m.setJMSDeliveryMode(DeliveryMode.PERSISTENT);      
       m.setJMSExpiration(987654321l);
       m.setJMSPriority(9);
       m.setBooleanProperty("booleanProperty", true);
       m.setByteProperty("byteProperty", (byte)2);
       m.setShortProperty("shortProperty", (short)3);
       m.setIntProperty("intProperty", 4);
       m.setLongProperty("longProperty", 5l);
       m.setFloatProperty("floatProperty", 6);
       m.setDoubleProperty("doubleProperty", 7);
       m.setStringProperty("stringPoperty", "someString");
    }
 
    /**
     * Makes sure two physically different message are equivalent: they have identical JMS fields and
     * body.
     */
    public static void ensureEquivalent(Message m1, JBossMessage m2) throws JMSException
    {
       assertTrue(m1 != m2);
       
       //Can't compare message id since not set until send
 
       assertEquals(m1.getJMSTimestamp(), m2.getJMSTimestamp());
 
       byte[] corrIDBytes = null;
       String corrIDString = null;
 
       try
       {
          corrIDBytes = m1.getJMSCorrelationIDAsBytes();
       }
       catch(JMSException e)
       {
          // correlation ID specified as String
          corrIDString = m1.getJMSCorrelationID();
       }
 
       if (corrIDBytes != null)
       {
          assertTrue(Arrays.equals(corrIDBytes, m2.getJMSCorrelationIDAsBytes()));
       }
       else if (corrIDString != null)
       {
          assertEquals(corrIDString, m2.getJMSCorrelationID());
       }
       else
       {
          // no correlation id
 
          try
          {
             byte[] corrID2 = m2.getJMSCorrelationIDAsBytes();
             assertNull(corrID2);
          }
          catch(JMSException e)
          {
             // correlatin ID specified as String
             String corrID2 = m2.getJMSCorrelationID();
             assertNull(corrID2);
          }
       }
       assertEquals(m1.getJMSReplyTo(), m2.getJMSReplyTo());
       assertEquals(m1.getJMSDestination(), m2.getJMSDestination());
       assertEquals(m1.getJMSDeliveryMode(), m2.getJMSDeliveryMode());
       //We don't check redelivered since this is always dealt with on the proxy
       assertEquals(m1.getJMSType(), m2.getJMSType());
       assertEquals(m1.getJMSExpiration(), m2.getJMSExpiration());
       assertEquals(m1.getJMSPriority(), m2.getJMSPriority());
 
       int m1PropertyCount = 0, m2PropertyCount = 0;
       for(Enumeration p = m1.getPropertyNames(); p.hasMoreElements(); m1PropertyCount++)
       {
          p.nextElement();
       }
       for(Enumeration p = m2.getPropertyNames(); p.hasMoreElements(); m2PropertyCount++)
       {
          p.nextElement();
       }
 
       assertEquals(m1PropertyCount, m2PropertyCount);
 
       for(Enumeration props = m1.getPropertyNames(); props.hasMoreElements(); )
       {
          boolean found = false;
 
          String name = (String)props.nextElement();
 
          boolean booleanProperty = false;
          try
          {
             booleanProperty = m1.getBooleanProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a boolean
          }
 
          if (found)
          {
             assertEquals(booleanProperty, m2.getBooleanProperty(name));
             continue;
          }
 
          byte byteProperty = 0;
          try
          {
             byteProperty = m1.getByteProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a byte
          }
 
          if (found)
          {
             assertEquals(byteProperty, m2.getByteProperty(name));
             continue;
          }
 
          short shortProperty = 0;
          try
          {
             shortProperty = m1.getShortProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a short
          }
 
          if (found)
          {
             assertEquals(shortProperty, m2.getShortProperty(name));
             continue;
          }
 
 
          int intProperty = 0;
          try
          {
             intProperty = m1.getIntProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a int
          }
 
          if (found)
          {
             assertEquals(intProperty, m2.getIntProperty(name));
             continue;
          }
 
 
          long longProperty = 0;
          try
          {
             longProperty = m1.getLongProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a long
          }
 
          if (found)
          {
             assertEquals(longProperty, m2.getLongProperty(name));
             continue;
          }
 
 
          float floatProperty = 0;
          try
          {
             floatProperty = m1.getFloatProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a float
          }
 
          if (found)
          {
             assertTrue(floatProperty == m2.getFloatProperty(name));
             continue;
          }
 
          double doubleProperty = 0;
          try
          {
             doubleProperty = m1.getDoubleProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a double
          }
 
          if (found)
          {
             assertTrue(doubleProperty == m2.getDoubleProperty(name));
             continue;
          }
 
          String stringProperty = null;
          try
          {
             stringProperty = m1.getStringProperty(name);
             found = true;
          }
          catch(JMSException e)
          {
             // not a String
          }
 
          if (found)
          {
             assertEquals(stringProperty, m2.getStringProperty(name));
             continue;
          }
 
 
          fail("Cannot identify property " + name);
       }
    }
 
    public static void ensureEquivalent(BytesMessage m1, JBossBytesMessage m2) throws JMSException
    {
       ensureEquivalent((Message)m1, m2);
 
       long len = m1.getBodyLength();
       for(int i = 0; i < len; i++)
       {
          assertEquals(m1.readByte(), m2.readByte());
       }
 
       try
       {
          m1.readByte();
          fail("should throw MessageEOFException");
       }
       catch(MessageEOFException e)
       {
          // OK
       }
 
       try
       {
          m2.readByte();
          fail("should throw MessageEOFException");
       }
       catch(MessageEOFException e)
       {
          // OK
       }
    }
 
    public static void ensureEquivalent(MapMessage m1, JBossMapMessage m2) throws JMSException
    {
       ensureEquivalent((Message)m1, m2);
 
       for(Enumeration e = m1.getMapNames(); e.hasMoreElements(); )
       {
          String name = (String)e.nextElement();
          assertEquals(m1.getObject(name), m2.getObject(name));
       }
 
       for(Enumeration e = m2.getMapNames(); e.hasMoreElements(); )
       {
          String name = (String)e.nextElement();
          assertEquals(m2.getObject(name), m1.getObject(name));
       }
    }
 
    public static void ensureEquivalent(ObjectMessage m1, JBossObjectMessage m2) throws JMSException
    {
       ensureEquivalent((Message)m1, m2);
       assertEquals(m1.getObject(), m2.getObject());
    }
 
    public static void ensureEquivalent(StreamMessage m1, JBossStreamMessage m2) throws JMSException
    {
       ensureEquivalent((Message)m1, m2);
 
       m1.reset();
       m2.reset();
       boolean m1eof = false, m2eof = false;
       while(true)
       {
          byte b1, b2;
          try
          {
             b1 = m1.readByte();
          }
          catch(MessageEOFException e)
          {
             m1eof = true;
             break;
          }
 
          try
          {
             b2 = m2.readByte();
          }
          catch(MessageEOFException e)
          {
             m2eof = true;
             break;
          }
 
          assertEquals(b1, b2);
       }
 
 
       if (m1eof)
       {
          try
          {
             m2.readByte();
             fail("should throw MessageEOFException");
          }
          catch(MessageEOFException e)
          {
             // OK
          }
       }
 
       if (m2eof)
       {
          try
          {
             m1.readByte();
             fail("should throw MessageEOFException");
          }
          catch(MessageEOFException e)
          {
             // OK
          }
       }
    }
    
    public static void ensureEquivalent(TextMessage m1, JBossTextMessage m2) throws JMSException
    {
       ensureEquivalent((Message)m1, m2);
       assertEquals(m1.getText(), m2.getText());
    }
 
    // Attributes ----------------------------------------------------
 
    protected Destination queue;
    protected Destination topic;
    protected Connection producerConnection, consumerConnection;
    protected Session queueProducerSession, queueConsumerSession;
    protected MessageProducer queueProducer;
    protected MessageConsumer queueConsumer;
    protected Session topicProducerSession, topicConsumerSession;
    protected MessageProducer topicProducer;
    protected MessageConsumer topicConsumer;
    
    
    // Constructors --------------------------------------------------
 
    public MessageTest(String name)
    {
       super(name);
    }
 
    // Public --------------------------------------------------------
 
    public void setUp() throws Exception
    {
       super.setUp();
 
       ServerManagement.start("all");
             
       ServerManagement.undeployQueue("Queue");
       ServerManagement.deployQueue("Queue");
                  
       ServerManagement.undeployTopic("Topic");
       ServerManagement.deployTopic("Topic");
 
       InitialContext ic = new InitialContext(ServerManagement.getJNDIEnvironment());
       ConnectionFactory cf = (ConnectionFactory)ic.lookup("/ConnectionFactory");
       queue = (Destination)ic.lookup("/queue/Queue");
       topic = (Destination)ic.lookup("/topic/Topic");
       
       drainDestination(cf, queue);
       
       producerConnection = cf.createConnection();
       consumerConnection = cf.createConnection();
 
       queueProducerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
       queueConsumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
       queueProducer = queueProducerSession.createProducer(queue);
       queueConsumer = queueConsumerSession.createConsumer(queue);
       
       topicProducerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
       topicConsumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
       topicProducer = topicProducerSession.createProducer(topic);
       topicConsumer = topicConsumerSession.createConsumer(topic);
 
       consumerConnection.start();
    }
 
    public void tearDown() throws Exception
    {
       producerConnection.close();
       consumerConnection.close();
 
       ServerManagement.undeployQueue("Queue");
       
 
       super.tearDown();
    }
    
    public void messageOrderTestQueue() throws Exception
    {
       final int NUM_MESSAGES = 10;
       
       queueProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = queueProducerSession.createMessage();
          m.setIntProperty("count", i);
          queueProducer.send(m);
       }
       
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = queueConsumer.receive(3000);
          assertNotNull(m);
          int count = m.getIntProperty("count");
          assertEquals(i, count);
       }
       
       queueProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = queueProducerSession.createMessage();
          m.setIntProperty("count2", i);
          queueProducer.send(m);
       }
       
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = queueConsumer.receive(3000);
          assertNotNull(m);
          int count = m.getIntProperty("count2");
          assertEquals(i, count);
       }
    }
    
    public void messageOrderTestTopic() throws Exception
    {
       final int NUM_MESSAGES = 10;
       
       topicProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = topicProducerSession.createMessage();
          m.setIntProperty("count", i);
          topicProducer.send(m);
       }
       
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = topicConsumer.receive(3000);
          assertNotNull(m);
          int count = m.getIntProperty("count");
          assertEquals(i, count);
       }
       
       topicProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = topicProducerSession.createMessage();
          m.setIntProperty("count2", i);
          topicProducer.send(m);
       }
       
       for (int i = 0; i < NUM_MESSAGES; i++)
       {
          Message m = topicConsumer.receive(3000);
          assertNotNull(m);
          int count = m.getIntProperty("count2");
          assertEquals(i, count);
       }
    }
    
 
    public void testProperties() throws Exception
    {
       Message m1 = queueProducerSession.createMessage();
 
 
       //Some arbitrary values
       boolean myBool = true;
       byte myByte = 13;
       short myShort = 15321;
       int myInt = 0x71ab6c80;
       long myLong = 0x20bf1e3fb6fa31dfL;
       float myFloat = Float.MAX_VALUE - 23465;
       double myDouble = Double.MAX_VALUE - 72387633;
       String myString = "abcdef&^*&!^ghijkl";
 
       m1.setBooleanProperty("myBool", myBool);
       m1.setByteProperty("myByte", myByte);
       m1.setShortProperty("myShort", myShort);
       m1.setIntProperty("myInt", myInt);
       m1.setLongProperty("myLong", myLong);
       m1.setFloatProperty("myFloat", myFloat);
       m1.setDoubleProperty("myDouble", myDouble);
       m1.setStringProperty("myString", myString);
 
       m1.setObjectProperty("myBool", new Boolean(myBool));
       m1.setObjectProperty("myByte", new Byte(myByte));
       m1.setObjectProperty("myShort", new Short(myShort));
       m1.setObjectProperty("myInt", new Integer(myInt));
       m1.setObjectProperty("myLong", new Long(myLong));
       m1.setObjectProperty("myFloat", new Float(myFloat));
       m1.setObjectProperty("myDouble", new Double(myDouble));
       m1.setObjectProperty("myString", myString);
 
       try
       {
          m1.setObjectProperty("myIllegal", new Object());
          fail();
       }
       catch (javax.jms.MessageFormatException e)
       {}
 
 
       queueProducer.send(queue, m1);
 
       Message m2 = queueConsumer.receive(2000);
 
       assertNotNull(m2);
 
       assertEquals(myBool, m2.getBooleanProperty("myBool"));
       assertEquals(myByte, m2.getByteProperty("myByte"));
       assertEquals(myShort, m2.getShortProperty("myShort"));
       assertEquals(myInt, m2.getIntProperty("myInt"));
       assertEquals(myLong, m2.getLongProperty("myLong"));
       assertEquals(myFloat, m2.getFloatProperty("myFloat"), 0);
       assertEquals(myDouble, m2.getDoubleProperty("myDouble"), 0);
       assertEquals(myString, m2.getStringProperty("myString"));
 
 
       //Properties should now be read-only
       try
       {
          m2.setBooleanProperty("myBool", myBool);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setByteProperty("myByte", myByte);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setShortProperty("myShort", myShort);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setIntProperty("myInt", myInt);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setLongProperty("myLong", myLong);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setFloatProperty("myFloat", myFloat);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setDoubleProperty("myDouble", myDouble);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       try
       {
          m2.setStringProperty("myString", myString);
          fail();
       }
       catch (MessageNotWriteableException e) {}
 
       assertTrue(m2.propertyExists("myBool"));
       assertTrue(m2.propertyExists("myByte"));
       assertTrue(m2.propertyExists("myShort"));
       assertTrue(m2.propertyExists("myInt"));
       assertTrue(m2.propertyExists("myLong"));
       assertTrue(m2.propertyExists("myFloat"));
       assertTrue(m2.propertyExists("myDouble"));
       assertTrue(m2.propertyExists("myString"));
 
       assertFalse(m2.propertyExists("sausages"));
 
       HashSet propNames = new HashSet();
       Enumeration en = m2.getPropertyNames();
       while (en.hasMoreElements())
       {
          String propName = (String)en.nextElement();
          propNames.add(propName);
       }
 
       assertEquals(8, propNames.size());
 
       assertTrue(propNames.contains("myBool"));
       assertTrue(propNames.contains("myByte"));
       assertTrue(propNames.contains("myShort"));
       assertTrue(propNames.contains("myInt"));
       assertTrue(propNames.contains("myLong"));
       assertTrue(propNames.contains("myFloat"));
       assertTrue(propNames.contains("myDouble"));
       assertTrue(propNames.contains("myString"));
 
 
       // Check property conversions
 
       //Boolean property can be read as String but not anything else
 
       assertEquals(String.valueOf(myBool), m2.getStringProperty("myBool"));
 
       try
       {
          m2.getByteProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getShortProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getIntProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getLongProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getDoubleProperty("myBool");
          fail();
       } catch (MessageFormatException e) {}
 
 
       // byte property can be read as short, int, long or String
 
       assertEquals((short)myByte, m2.getShortProperty("myByte"));
       assertEquals((int)myByte, m2.getIntProperty("myByte"));
       assertEquals((long)myByte, m2.getLongProperty("myByte"));
       assertEquals(String.valueOf(myByte), m2.getStringProperty("myByte"));
 
       try
       {
          m2.getBooleanProperty("myByte");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myByte");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getDoubleProperty("myByte");
          fail();
       } catch (MessageFormatException e) {}
 
 
       // short property can be read as int, long or String
 
       assertEquals((int)myShort, m2.getIntProperty("myShort"));
       assertEquals((long)myShort, m2.getLongProperty("myShort"));
       assertEquals(String.valueOf(myShort), m2.getStringProperty("myShort"));
 
       try
       {
          m2.getByteProperty("myShort");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getBooleanProperty("myShort");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myShort");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getDoubleProperty("myShort");
          fail();
       } catch (MessageFormatException e) {}
 
       // int property can be read as long or String
 
       assertEquals((long)myInt, m2.getLongProperty("myInt"));
       assertEquals(String.valueOf(myInt), m2.getStringProperty("myInt"));
 
       try
       {
          m2.getShortProperty("myInt");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getByteProperty("myInt");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getBooleanProperty("myInt");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myInt");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getDoubleProperty("myInt");
          fail();
       } catch (MessageFormatException e) {}
 
 
       // long property can be read as String
 
       assertEquals(String.valueOf(myLong), m2.getStringProperty("myLong"));
 
       try
       {
          m2.getIntProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getShortProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getByteProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getBooleanProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getDoubleProperty("myLong");
          fail();
       } catch (MessageFormatException e) {}
 
 
       // float property can be read as double or String
 
       assertEquals(String.valueOf(myFloat), m2.getStringProperty("myFloat"));
       assertEquals((double)myFloat, m2.getDoubleProperty("myFloat"), 0);
 
       try
       {
          m2.getIntProperty("myFloat");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getShortProperty("myFloat");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getLongProperty("myFloat");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getByteProperty("myFloat");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getBooleanProperty("myFloat");
          fail();
       } catch (MessageFormatException e) {}
 
 
 
       // double property can be read as String
 
       assertEquals(String.valueOf(myDouble), m2.getStringProperty("myDouble"));
 
 
       try
       {
          m2.getFloatProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getIntProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getShortProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getByteProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getBooleanProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       try
       {
          m2.getFloatProperty("myDouble");
          fail();
       } catch (MessageFormatException e) {}
 
       m2.clearProperties();
 
       Enumeration en2 = m2.getPropertyNames();
       assertFalse(en2.hasMoreElements());
 
 
 
 
       // Test String -> Numeric and bool conversions
       Message m3 = queueProducerSession.createMessage();
 
       m3.setStringProperty("myBool", String.valueOf(myBool));
       m3.setStringProperty("myByte", String.valueOf(myByte));
       m3.setStringProperty("myShort", String.valueOf(myShort));
       m3.setStringProperty("myInt", String.valueOf(myInt));
       m3.setStringProperty("myLong", String.valueOf(myLong));
       m3.setStringProperty("myFloat", String.valueOf(myFloat));
       m3.setStringProperty("myDouble", String.valueOf(myDouble));
       m3.setStringProperty("myIllegal", "xyz123");
 
       assertEquals(myBool, m3.getBooleanProperty("myBool"));
       assertEquals(myByte, m3.getByteProperty("myByte"));
       assertEquals(myShort, m3.getShortProperty("myShort"));
       assertEquals(myInt, m3.getIntProperty("myInt"));
       assertEquals(myLong, m3.getLongProperty("myLong"));
       assertEquals(myFloat, m3.getFloatProperty("myFloat"), 0);
       assertEquals(myDouble, m3.getDoubleProperty("myDouble"), 0);
 
       m3.getBooleanProperty("myIllegal");
 
       try
       {
          m3.getByteProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
       try
       {
          m3.getShortProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
       try
       {
          m3.getIntProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
       try
       {
          m3.getLongProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
       try
       {
          m3.getFloatProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
       try
       {
          m3.getDoubleProperty("myIllegal");
          fail();
       }
       catch (NumberFormatException e) {}
    }
 
 
 
    public void testSendReceiveForeignMessage() throws JMSException
    {
       
       log.trace("Starting da test");
       
       SimpleJMSMessage foreignMessage = new SimpleJMSMessage();
       
       foreignMessage.setStringProperty("animal", "aardvark");
       
       //foreign messages don't have to be serializable
       assertFalse(foreignMessage instanceof Serializable);
       
       log.trace("Sending message");
       
       queueProducer.send(foreignMessage);
       
       log.trace("Sent message");
 
       Message m2 = queueConsumer.receive(3000);
       log.trace("The message is " + m2);
       
       assertNotNull(m2);
       
       assertEquals("aardvark", m2.getStringProperty("animal"));
       
       log.trace("Received message");
 
       log.trace("Done that test");
    }
 
    public void testCopyOnJBossMessage() throws JMSException
    {
       JBossMessage jbossMessage = ((MessageProxy)queueProducerSession.createMessage()).getMessage();
 
       configureMessage(jbossMessage);
 
       JBossMessage copy = new JBossMessage(jbossMessage, 0);
 
       ensureEquivalent(jbossMessage, copy);
    }
 
 
    public void testCopyOnForeignMessage() throws JMSException
    {
       Message foreignMessage = new SimpleJMSMessage();
 
       JBossMessage copy = new JBossMessage(foreignMessage, 0);
 
       ensureEquivalent(foreignMessage, copy);
    }
    
 
    public void testCopyOnJBossBytesMessage() throws JMSException
    {
       JBossBytesMessage jbossBytesMessage = (JBossBytesMessage)(((MessageProxy)queueProducerSession.
          createBytesMessage()).getMessage());
 
       for(int i = 0; i < 20; i++)
       {
          jbossBytesMessage.writeByte((byte)i);
       }
 
       
       jbossBytesMessage.reset();
       JBossBytesMessage copy = new JBossBytesMessage(jbossBytesMessage);
 
       copy.reset();
 
       ensureEquivalent(jbossBytesMessage, copy);
    }
 
 
    public void testCopyOnForeignBytesMessage() throws JMSException
    {
       BytesMessage foreignBytesMessage = new SimpleJMSBytesMessage();
       for(int i = 0; i < 20; i++)
       {
          foreignBytesMessage.writeByte((byte)i);
       }
 
       JBossBytesMessage copy = new JBossBytesMessage(foreignBytesMessage, 0);
 
       foreignBytesMessage.reset();
       copy.reset();
 
       ensureEquivalent(foreignBytesMessage, copy);
    }
 
    public void testCopyOnJBossMapMessage() throws JMSException
    {
       JBossMapMessage jbossMapMessage = (JBossMapMessage)(((MessageProxy)queueProducerSession.
          createMapMessage()).getMessage());
       
       jbossMapMessage.setInt("int", 1);
       jbossMapMessage.setString("string", "test");
 
       JBossMapMessage copy = new JBossMapMessage((JBossMapMessage)jbossMapMessage);
 
       ensureEquivalent(jbossMapMessage, copy);
    }
 
 
    public void testCopyOnForeignMapMessage() throws JMSException
    {
       MapMessage foreignMapMessage = new SimpleJMSMapMessage();
       foreignMapMessage.setInt("int", 1);
       foreignMapMessage.setString("string", "test");
 
       JBossMapMessage copy = new JBossMapMessage(foreignMapMessage, 0);
 
       ensureEquivalent(foreignMapMessage, copy);
    }
 
 
    public void testCopyOnJBossObjectMessage() throws JMSException
    {
       JBossObjectMessage jbossObjectMessage = (JBossObjectMessage)
          (((MessageProxy)queueProducerSession.createObjectMessage()).getMessage());
       
       JBossObjectMessage copy = new JBossObjectMessage(jbossObjectMessage);
 
       ensureEquivalent(jbossObjectMessage, copy);
    }
 
 
    public void testCopyOnForeignObjectMessage() throws JMSException
    {
       ObjectMessage foreignObjectMessage = new SimpleJMSObjectMessage();
 
       JBossObjectMessage copy = new JBossObjectMessage(foreignObjectMessage, 0);
 
       ensureEquivalent(foreignObjectMessage, copy);
    }
 
 
    public void testCopyOnJBossStreamMessage() throws JMSException
    {
       JBossStreamMessage jbossStreamMessage = (JBossStreamMessage)
          (((MessageProxy)queueProducerSession.createStreamMessage()).getMessage());
       
       jbossStreamMessage.writeByte((byte)1);
       jbossStreamMessage.writeByte((byte)2);
       jbossStreamMessage.writeByte((byte)3);
 
       JBossStreamMessage copy = new JBossStreamMessage((JBossStreamMessage)jbossStreamMessage);
 
       ensureEquivalent(jbossStreamMessage, copy);
    }
 
 
    public void testCopyOnForeignStreamMessage() throws JMSException
    {
       StreamMessage foreignStreamMessage = new SimpleJMSStreamMessage();
       foreignStreamMessage.writeByte((byte)1);
       foreignStreamMessage.writeByte((byte)2);
       foreignStreamMessage.writeByte((byte)3);
 
       JBossStreamMessage copy = new JBossStreamMessage(foreignStreamMessage, 0);
 
       ensureEquivalent(foreignStreamMessage, copy);
    }
 
 
    public void testCopyOnJBossTextMessage() throws JMSException
    {
       JBossTextMessage jbossTextMessage = (JBossTextMessage)
          (((MessageProxy)queueProducerSession.createTextMessage()).getMessage());
       
       JBossTextMessage copy = new JBossTextMessage(jbossTextMessage);
 
       ensureEquivalent(jbossTextMessage, copy);
    }
 
 
    public void testCopyOnForeignTextMessage() throws JMSException
    {
       TextMessage foreignTextMessage = new SimpleJMSTextMessage();
 
       JBossTextMessage copy = new JBossTextMessage(foreignTextMessage, 0);
 
       ensureEquivalent(foreignTextMessage, copy);
    }
    
    public void testForeignJMSDestination() throws JMSException
    {
       Message message = queueProducerSession.createMessage();
       
       Destination foreignDestination = new ForeignDestination();
       
       message.setJMSDestination(foreignDestination);
       
       assertSame(foreignDestination, message.getJMSDestination());
       
       queueProducer.send(message);
       
       assertSame(queue, message.getJMSDestination());
       
       Message receivedMessage = queueConsumer.receive(100L);
       
       ensureEquivalent(receivedMessage, ((MessageProxy) message).getMessage());
    }
    
    public void testForeignJMSReplyTo() throws JMSException
    {
       JBossMessage jbossMessage = ((MessageProxy) queueProducerSession.createTextMessage()).getMessage();
       
       Destination foreignDestination = new ForeignDestination();
       
       jbossMessage.setJMSReplyTo(foreignDestination);
       
       queueProducer.send(jbossMessage);
       
       Message receivedMessage = queueConsumer.receive(100L);
 
       ensureEquivalent(receivedMessage, jbossMessage);
    }
    
    public void testCopyForeignDestinationAndReplyTo() throws JMSException
    {
       Message foreignMessage = new SimpleJMSMessage();
       foreignMessage.setJMSDestination(new ForeignDestination());
       foreignMessage.setJMSReplyTo(new ForeignDestination());
 
       JBossMessage copy = new JBossMessage(foreignMessage, 0);
 
       ensureEquivalent(foreignMessage, copy);
    }
 
    // Package protected ---------------------------------------------
 
    // Protected -----------------------------------------------------
 
    // Private -------------------------------------------------------
 
    // Inner classes -------------------------------------------------
 
    private static class ForeignDestination implements Destination, Serializable
    {
      // A ForeignDestination equals any other ForeignDestination, for simplicity
      public boolean equals(Object obj)
      {
         return obj instanceof ForeignDestination;
      }
      
      public int hashCode()
      {
         return 157;
      }
    }
    
 }
