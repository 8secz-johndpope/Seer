 /********************************************************************************
  * CruiseControl, a Continuous Integration Toolkit
  * Copyright (c) 2001-2003, ThoughtWorks, Inc.
  * 651 W Washington Ave. Suite 600
  * Chicago, IL 60661 USA
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *     + Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *
  *     + Redistributions in binary form must reproduce the above
  *       copyright notice, this list of conditions and the following
  *       disclaimer in the documentation and/or other materials provided
  *       with the distribution.
  *
  *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
  *       names of its contributors may be used to endorse or promote
  *       products derived from this software without specific prior
  *       written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ********************************************************************************/
 package net.sourceforge.cruisecontrol.publishers;
 
 import junit.framework.TestCase;
 import net.sourceforge.cruisecontrol.util.XMLLogHelper;
 import net.sourceforge.cruisecontrol.CruiseControlException;
 import net.sourceforge.cruisecontrol.Modification;
 import net.sourceforge.cruisecontrol.PluginXMLHelper;
 import net.sourceforge.cruisecontrol.ProjectXMLHelper;
 import net.sourceforge.cruisecontrol.publishers.email.DropLetterEmailAddressMapper;
 import net.sourceforge.cruisecontrol.publishers.email.PropertiesMapper;
 import net.sourceforge.cruisecontrol.testutil.TestUtil;
 
 import java.io.StringReader;
 import java.io.File;
 import java.io.FileOutputStream;
 
 import java.util.Iterator;
 import java.util.Properties;
 import java.util.Set;
 import java.util.TreeSet;
 
 import javax.mail.internet.AddressException;
 import javax.mail.internet.InternetAddress;
 
 // import org.apache.oro.io.GlobFilenameFilter;
 import org.apache.log4j.BasicConfigurator;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.jdom.input.SAXBuilder;
 import org.jdom.Element;
 
 public class EmailPublisherTest extends TestCase {
 
     private XMLLogHelper successLogHelper;
     private XMLLogHelper fixedLogHelper;
     private XMLLogHelper failureLogHelper;
     private XMLLogHelper firstFailureLogHelper;
     private EmailPublisher emailPublisher;
     private EmailPublisher noAlertsEmailPublisher;
     private File tmpFile;
     
     public EmailPublisherTest(String name) {
         super(name);
 
         // Turn off logging
         BasicConfigurator.configure();
         Logger.getLogger(this.getClass()).getLoggerRepository().setThreshold(Level.OFF);
     }
 
     protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
         Element cruisecontrolElement = TestUtil.createElement(success, lastBuildSuccess);
 
         return new XMLLogHelper(cruisecontrolElement);
     }
 
     public void setUp() throws Exception {
         PropertiesMapper propertiesMapper = new PropertiesMapper();
         // create a temp file to test propertiesmapper
         Properties props = new Properties();
         tmpFile = File.createTempFile("cruise", "Test");
         props.setProperty("always1", "always1");
         FileOutputStream fos = new FileOutputStream(tmpFile);
         props.store(fos, null);
         fos.close();
         tmpFile.deleteOnExit();
 
         String xml = generateXML(true);
         emailPublisher = initPublisher(propertiesMapper, xml);
         emailPublisher.setMailHost("mailhost");
         emailPublisher.setReturnAddress("returnaddress");
 
         xml = generateXML(false);
         noAlertsEmailPublisher = initPublisher(propertiesMapper, xml);
         
         successLogHelper = createLogHelper(true, true);
         failureLogHelper = createLogHelper(false, false);
         fixedLogHelper = createLogHelper(true, false);
         firstFailureLogHelper = createLogHelper(false, true); 
     }
 
     
 
     protected EmailPublisher initPublisher(PropertiesMapper propMapper,
                                   String xml)
                                   throws Exception {
         
         SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
         
         Element emailPublisherElement = builder.build(new StringReader(xml)).getRootElement();
         PluginXMLHelper xmlHelper = new PluginXMLHelper(new ProjectXMLHelper());
 
         EmailPublisher ePublisher =
             (MockEmailPublisher) xmlHelper.configure(
                 emailPublisherElement,
                 Class.forName("net.sourceforge.cruisecontrol.publishers.MockEmailPublisher"),
                 false);
 
         ePublisher.add(new DropLetterEmailAddressMapper());        
         propMapper.setFile(tmpFile.getPath());
         ePublisher.add(propMapper);
         
         return ePublisher;
     }
     
     protected String generateXML(boolean includeAlerts) {
         StringBuffer xml = new StringBuffer();
         xml.append("<email defaultsuffix=\"@host.com\">");
         xml.append("<always address=\"always1\"/>");
         xml.append("<always address=\"always2@host.com\"/>");
         xml.append("<always address=\"dropletteruser1\"/>");
         xml.append("<failure address=\"failure1\"/>");
         xml.append("<failure address=\"failure2@host.com\" reportWhenFixed=\"true\"/>");
         xml.append("<success address='success1' />");
         xml.append("<success address='success2@host.com' />");
         xml.append("<map alias=\"user3\" address=\"user3@host2.com\"/>");
         if (includeAlerts) {
             //xml.append("<alert file=\".*\" address=\"anyFileMod@host.com\" />");
             xml.append("<alert fileRegExpr=\"filename1\" address=\"filename1@host.com\" />");
             xml.append("<alert fileRegExpr=\"basedir/subdirectory2/.*\" address=\"subdir2@host.com\" />");
             xml.append("<alert fileRegExpr=\"basedir/subdirectory3/filename3\" address=\"filename3@host.com\" />");
             xml.append("<alert fileRegExpr=\"basedir/subdirectory5/.*\" address=\"basedirSubdirectory5@host.com\" />");
             xml.append("<alert fileRegExpr=\"\" address=\"\" />");
         }
         xml.append("</email>");
         
         return xml.toString();
     }
 
     public void testValidate() {
         EmailPublisher publisher = new MockEmailPublisher();
         try {
             publisher.validate();
             fail("EmailPublisher should throw exceptions when required fields are not set.");
         } catch (CruiseControlException e) {
         }
 
         publisher.setMailHost("mailhost");
         publisher.setReturnAddress("returnaddress");
 
         try {
             publisher.validate();
         } catch (CruiseControlException e) {
             fail("EmailPublisher should not throw exceptions when required fields are set.");
         }
     }
 
     public void testShouldSend() throws Exception {
         //build not necessary, spam while broken=true
         emailPublisher.setSpamWhileBroken(true);
         emailPublisher.setReportSuccess("success");
         assertEquals(true, emailPublisher.shouldSend(successLogHelper));
         assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(true, emailPublisher.shouldSend(failureLogHelper));
 
         emailPublisher.setReportSuccess("fixes");
         assertEquals(false, emailPublisher.shouldSend(successLogHelper));
         assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(true, emailPublisher.shouldSend(failureLogHelper));
 
         emailPublisher.setReportSuccess("never");
         assertEquals(false, emailPublisher.shouldSend(successLogHelper));
         assertEquals(false, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(true, emailPublisher.shouldSend(failureLogHelper));
 
         emailPublisher.setSpamWhileBroken(false);
         emailPublisher.setReportSuccess("success");
         assertEquals(true, emailPublisher.shouldSend(successLogHelper));
         assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
         assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));
 
         emailPublisher.setReportSuccess("fixes");
         assertEquals(false, emailPublisher.shouldSend(successLogHelper));
         assertEquals(true, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
         assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));
 
         emailPublisher.setReportSuccess("never");
         assertEquals(false, emailPublisher.shouldSend(successLogHelper));
         assertEquals(false, emailPublisher.shouldSend(fixedLogHelper));
         assertEquals(false, emailPublisher.shouldSend(failureLogHelper));
         assertEquals(true, emailPublisher.shouldSend(firstFailureLogHelper));
 
     }
 
     public void testCreateSubject() throws Exception {
         emailPublisher.setReportSuccess("always");
         assertEquals(
             "someproject somelabel Build Successful",
             emailPublisher.createSubject(successLogHelper));
         emailPublisher.setReportSuccess("fixes");
         assertEquals(
             "someproject somelabel Build Fixed",
             emailPublisher.createSubject(fixedLogHelper));
 
         assertEquals("someproject Build Failed",
                 emailPublisher.createSubject(failureLogHelper));
 
         emailPublisher.setSubjectPrefix("[CC]");
         emailPublisher.setReportSuccess("always");
         assertEquals(
             "[CC] someproject somelabel Build Successful",
             emailPublisher.createSubject(successLogHelper));
         emailPublisher.setReportSuccess("fixes");
         assertEquals(
             "[CC] someproject somelabel Build Fixed",
             emailPublisher.createSubject(fixedLogHelper));
 
         assertEquals(
             "[CC] someproject Build Failed",
             emailPublisher.createSubject(failureLogHelper));
 
         //Anytime it is a "fixed" build, the subject should read "fixed".
         emailPublisher.setReportSuccess("always");
         assertEquals(
             "[CC] someproject somelabel Build Fixed",
             emailPublisher.createSubject(fixedLogHelper));
 
         emailPublisher.setReportSuccess("failures");
         assertEquals(
             "[CC] someproject somelabel Build Fixed",
             emailPublisher.createSubject(fixedLogHelper));
 
     }
 
     public void testCreateUserList() throws Exception {
         assertEquals(
                 "always1@host.com,always2@host.com,ropletteruser1@host.com,"
                 + "success1@host.com,success2@host.com,"
                 + "user1@host.com,user2@host.com,user3@host2.com",
             emailPublisher.createUserList(successLogHelper));
         assertEquals(
             "always1@host.com,always2@host.com,failure1@host.com,"
                 + "failure2@host.com,ropletteruser1@host.com,user1@host.com,user2@host.com,user3@host2.com",
             emailPublisher.createUserList(failureLogHelper));
         assertEquals(
             "always1@host.com,always2@host.com,"
                 + "failure2@host.com,ropletteruser1@host.com,"
                 + "success1@host.com,success2@host.com,"
                 + "user1@host.com,user2@host.com,user3@host2.com",
             emailPublisher.createUserList(fixedLogHelper));
 
         emailPublisher.setSkipUsers(true);
         assertEquals(
                 "always1@host.com,always2@host.com,ropletteruser1@host.com,success1@host.com,success2@host.com",
             emailPublisher.createUserList(successLogHelper));
         assertEquals(
                 "always1@host.com,always2@host.com,failure1@host.com,failure2@host.com,ropletteruser1@host.com",
             emailPublisher.createUserList(failureLogHelper));
 
         emailPublisher.setSkipUsers(false);
 
         assertEquals(
                 "always1@host.com,always2@host.com,failure2@host.com,"
                 + "ropletteruser1@host.com,success1@host.com,success2@host.com,"
                 + "user1@host.com,user2@host.com,user3@host2.com",
             emailPublisher.createUserList(fixedLogHelper));
     }
 
     public void testGetFromAddress() throws AddressException {
         String returnAddress = "me@you.com";
         String returnName = "Me you Me";
         emailPublisher.setReturnAddress(returnAddress);
         emailPublisher.setReturnName(returnName);
         InternetAddress fromAddress = emailPublisher.getFromAddress();
         assertEquals(returnAddress, fromAddress.getAddress());
         assertEquals(returnName, fromAddress.getPersonal());
     }
 
     public void testSendMail() throws Exception {
         assertFalse(emailPublisher.sendMail(null, "subject", "message", false));
         assertFalse(emailPublisher.sendMail(" ", "subject", "message", false));
     }
     
 
     public void testCreateUserSet() throws Exception {
         emailPublisher.setReportSuccess("success");
         Set userSet = emailPublisher.createUserSet(successLogHelper);
         assertNotNull(userSet);
         assertTrue(userSet.contains("always1"));
         assertTrue(userSet.contains("always2@host.com"));
         assertTrue(userSet.contains("ropletteruser1"));
         assertTrue(userSet.contains("success1"));
         assertTrue(userSet.contains("user1"));
         assertTrue(userSet.contains("user2"));
         assertTrue(userSet.contains("user3@host2.com"));
     }
     
     /**
      * The following unit test ensures TestUtil.createModsElement
      * creates a full XMLLogHelper object
      * 
      * @throws Exception
      */
     public void testCreateModsElement() throws Exception {
         Set modSet = successLogHelper.getModifications();
         Modification mod = null;
         Iterator modIter = modSet.iterator();
         
         while (modIter.hasNext()) {
             mod = (Modification) modIter.next();
             assertNotNull("getFileName should not return null", mod.getFileName());
             assertNotNull("getFullPath should not return null", mod.getFullPath());
             
             if ("filename1".equalsIgnoreCase(mod.getFileName())) {
                 assertNull(mod.getFolderName());
             }
         }
     }
     
     public void testCreateAlertUserSet() throws Exception {
         emailPublisher.validate();
         Set alertUsers = emailPublisher.createAlertUserSet(successLogHelper);
         //assertTrue(alertUsers.contains("anyFileMod@host.com"));
         assertTrue(alertUsers.contains("filename1@host.com"));
         assertTrue(alertUsers.contains("filename3@host.com"));
         assertFalse(alertUsers.contains(""));
 //        assertTrue(alertUsers.contains("subdir2@host.com"));
 //        assertEquals(3, alertUsers.size());
         assertEquals(2, alertUsers.size());
         
         alertUsers = noAlertsEmailPublisher.createAlertUserSet(failureLogHelper); 
         assertEquals(0, alertUsers.size());
     }
     
     public void testCreateEmailString() {
         Set emailSet = new TreeSet();
         emailSet.add("always1@host.com");
         emailSet.add("always2@host.com");
         emailSet.add("always1@host.com");
         emailSet.add("always3@host.com");
  
         assertEquals("always1@host.com,always2@host.com,always3@host.com", 
                 emailPublisher.createEmailString(emailSet));
     }
 }
