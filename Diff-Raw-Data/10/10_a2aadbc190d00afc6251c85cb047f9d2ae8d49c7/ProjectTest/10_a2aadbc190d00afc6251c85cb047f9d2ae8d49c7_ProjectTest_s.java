 /********************************************************************************
  * CruiseControl, a Continuous Integration Toolkit
  * Copyright (c) 2001-2003, ThoughtWorks, Inc.
  * 651 W Washington Ave. Suite 500
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
 package net.sourceforge.cruisecontrol;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 
 import junit.framework.TestCase;
 import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
 
 import org.apache.log4j.BasicConfigurator;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.jdom.Element;
 
 public class ProjectTest extends TestCase {
     private static final Logger LOG = Logger.getLogger(ProjectTest.class);
 
     private Project _project;
     private static final long ONE_MINUTE = 60 * 1000;
 
     public ProjectTest(String name) {
         super(name);
 
         // Turn off logging
         BasicConfigurator.configure();
         LOG.getLoggerRepository().setThreshold(Level.OFF);
     }
 
     protected void setUp() {
         _project = new Project();
     }
 
     public void testBuild() throws Exception {
         assertEquals(
             "Default value of config file doesn't match",
             "config.xml",
             _project.getConfigFileName());
 
         Date now = new Date();
         MockModificationSet modSet = new MockModificationSet();
         modSet.setTimeOfCheck(now);
         MockSchedule sched = new MockSchedule();
         _project.setLabel("1.2.2");
         _project.setName("myproject");
         _project.setSchedule(sched);
         _project.setLogDir("test-results");
         _project.setLogXmlEncoding("ISO-8859-1");
         _project.addAuxiliaryLogFile("_auxLog1.xml");
         _project.addAuxiliaryLogFile("_auxLogs");
         _project.setLabelIncrementer(new DefaultLabelIncrementer());
         _project.setModificationSet(modSet);
         writeFile("_auxLog1.xml", "<one/>");
         File auxLogsDirectory = new File("_auxLogs");
         auxLogsDirectory.mkdir();
         writeFile(
             "_auxLogs/_auxLog2.xml",
             "<testsuite><properties><property/></properties><testcase/></testsuite>");
         writeFile("_auxLogs/_auxLog3.xml", "<testsuite/>");
 
         _project.build();
 
         assertTrue(_project.isLastBuildSuccessful());
 
         String expected =
             "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><cruisecontrol>"
                + "<modifications /><info><property name=\"projectname\""
                 + "value=\"myproject\" /><property name=\"lastbuild\" value=\""
                 + _project.getFormatedTime(now)
                 + "\" /><property name=\"lastsuccessfulbuild\" value=\""
                 + _project.getLastSuccessfulBuild()
                 + "\" /><property name=\"builddate\" value=\""
                 + new SimpleDateFormat(DateFormatFactory.getFormat()).format(now)
                + "\" /><property name=\"label\" value=\"1.2.2\" /><property"
                 + "name=\"interval\" value=\"0\" /><property name=\""
                 + "lastbuildsuccessful\" value=\"true\" /><property name=\"logfile\" value=\""
                 + File.separator
                 + "log"
                 + _project.getFormatedTime(now)
                + "L1.2.2.xml\" /></info><build /><one /><testsuite><testcase"
                 + "/></testsuite><testsuite /></cruisecontrol>";
         assertEquals(expected, readFileToString(_project.getLogFileName()));
         assertEquals(
             "Didn't increment the label",
             "1.2.3",
             _project.getLabel().intern());
 
         //look for sourcecontrol properties
         java.util.Map props = sched.getBuildProperties();
         assertNotNull("Build properties were null.", props);
         assertEquals("Should be 4 build properties.", 4, props.size());
         assertTrue(
             "filemodified not found.",
             props.containsKey("filemodified"));
         assertTrue("fileremoved not found.", props.containsKey("fileremoved"));
     }
 
     public void testBadLabel() {
         try {
             _project.validateLabel("build_0", new DefaultLabelIncrementer());
             fail("Expected exception due to bad label");
         } catch (CruiseControlException expected) {
 
         }
     }
 
     public void testSetLastBuild() throws CruiseControlException {
         String lastBuild = "20000101120000";
 
         _project.setLastBuild(lastBuild);
 
         assertEquals(lastBuild, _project.getLastBuild());
     }
 
     public void testNullLastBuild() throws CruiseControlException {
         try {
             _project.setLastBuild(null);
             fail("Expected an IllegalArgumentException for a null last build");
         } catch (IllegalArgumentException e) {
         }
     }
 
     public void testBadLastBuild() {
         try {
             _project.setLastBuild("af32455432");
             fail("Expected a CruiseControlException for a bad last build");
         } catch (CruiseControlException e) {
         }
     }
 
     public void testGetModifications() throws CruiseControlException {
         MockModificationSet modSet = new MockModificationSet();
         Element modifications = modSet.getModifications(null);
         _project.setModificationSet(modSet);
 
         modSet.setModified(true);
         assertEquals(modifications, _project.getModifications());
         modSet.setModified(false);
         assertEquals(null, _project.getModifications());
 
         _project.setBuildForced(true);
         assertEquals(modifications, _project.getModifications());
         assertEquals(null, _project.getModifications());
 
         _project.setBuildForced(false);
         assertEquals(null, _project.getModifications());
 
         // TODO: need tests for when lastBuildSuccessful = false
     }
 
     public void testCheckOnlySinceLastBuild() throws CruiseControlException {
 
         _project.setLastBuild("20030218010101");
         _project.setLastSuccessfulBuild("20030218010101");
         assertEquals(false, _project.checkOnlySinceLastBuild());
 
         _project.setLastBuild("20030218020202");
         assertEquals(false, _project.checkOnlySinceLastBuild());
 
         _project.setBuildAfterFailed(false);
         assertEquals(true, _project.checkOnlySinceLastBuild());
 
         _project.setLastBuild("20030218010102");
         assertEquals(false, _project.checkOnlySinceLastBuild());
 
         _project.setLastBuild("20020101010101");
         assertEquals(false, _project.checkOnlySinceLastBuild());
     }
 
     public void testWaitIfPaused() throws InterruptedException {
         MockProject project = new MockProject() {
             public void run() {
                 loop();
             }
             void checkWait() throws InterruptedException {
                 waitIfPaused();
             }
         };
 
         new Thread(project).start();
 
         int firstLoopCount = project.getLoopCount();
         Thread.sleep(100);
         int secondLoopCount = project.getLoopCount();
         assertTrue(
             "loop counts are different when not paused",
             firstLoopCount != secondLoopCount);
 
         project.setPaused(true);
         Thread.sleep(100);
         firstLoopCount = project.getLoopCount();
         Thread.sleep(100);
         secondLoopCount = project.getLoopCount();
         assertTrue(
             "loop counts are the same when paused",
             firstLoopCount == secondLoopCount);
 
         project.setPaused(false);
         Thread.sleep(100);
         int lastLoopCount = project.getLoopCount();
         assertTrue(
             "loop count increased after pause ended",
             lastLoopCount > secondLoopCount);
 
         project.stopLooping();
     }
 
     public void testWaitForNextBuild() throws InterruptedException {
         MockProject project = new MockProject() {
             public void run() {
                 loop();
             }
             void checkWait() throws InterruptedException {
                 waitForNextBuild();
             }
         };
         project.setSleepMillis(1000);
         project.setSchedule(new MockSchedule());
         new Thread(project).start();
 
         Thread.sleep(100);
         assertEquals(1, project.getLoopCount());
 
         Thread.sleep(100);
         assertEquals(1, project.getLoopCount());
 
         project.forceBuild();
         Thread.sleep(100);
         assertEquals(2, project.getLoopCount());
 
         project.stopLooping();
     }
 
     public void testWaitForBuildToFinish() throws InterruptedException {
         MockProject project = new MockProject() {
             public void run() {
                 loop();
             }
             void checkWait() throws InterruptedException {
                 waitForBuildToFinish();
             }
         };
 
         new Thread(project).start();
 
         Thread.sleep(100);
         assertEquals(1, project.getLoopCount());
 
         Thread.sleep(100);
         assertEquals(1, project.getLoopCount());
 
         project.buildFinished();
         Thread.sleep(100);
         assertEquals(2, project.getLoopCount());
 
         project.stopLooping();
     }
 
     public void testFormatTime() {
         long fiveSeconds = 5 * 1000;
         long oneHour = 60 * ONE_MINUTE;
         long oneHourFiftyNineMinutes = 2 * oneHour - ONE_MINUTE;
 
         String seconds = "5 seconds";
         String hoursMinutesSeconds = "1 hours 59 minutes 5 seconds";
         String negativeTime = "-1 hours -59 minutes -5 seconds";
 
         assertEquals(seconds, Project.formatTime(fiveSeconds));
         assertEquals(
             hoursMinutesSeconds,
             Project.formatTime(oneHourFiftyNineMinutes + fiveSeconds));
         assertEquals(
             negativeTime,
             Project.formatTime(-1 * (oneHourFiftyNineMinutes + fiveSeconds)));
     }
 
     private String readFileToString(String filename) throws IOException {
         BufferedReader reader = new BufferedReader(new FileReader(filename));
         StringBuffer result = new StringBuffer();
 
         String s = reader.readLine();
         while (s != null) {
             result.append(s.trim());
             s = reader.readLine();
         }
         reader.close();
 
         return result.toString();
     }
 
     private void writeFile(String fileName, String contents)
         throws IOException {
 
         FileWriter fw = new FileWriter(fileName);
         fw.write(contents);
         fw.close();
     }
 
 }
