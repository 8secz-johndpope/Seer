 package org.hackystat.sensor.ant.junit;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.Unmarshaller;
 import javax.xml.datatype.XMLGregorianCalendar;
 
 import org.apache.tools.ant.BuildException;
 import org.apache.tools.ant.DirectoryScanner;
 import org.apache.tools.ant.Project;
 import org.apache.tools.ant.Task;
 import org.apache.tools.ant.types.FileSet;
 import org.hackystat.sensor.ant.junit.resource.jaxb.Error;
 import org.hackystat.sensor.ant.junit.resource.jaxb.Failure;
 import org.hackystat.sensor.ant.junit.resource.jaxb.Testcase;
 import org.hackystat.sensor.ant.junit.resource.jaxb.Testsuite;
 import org.hackystat.sensor.ant.util.LongTimeConverter;
 import org.hackystat.sensorshell.SensorProperties;
 import org.hackystat.sensorshell.SensorPropertiesException;
 import org.hackystat.sensorshell.SensorShell;
 import org.hackystat.sensorshell.usermap.SensorShellMap;
 import org.hackystat.sensorshell.usermap.SensorShellMapException;
 import org.hackystat.utilities.tstamp.TstampSet;
 
 /**
  * Implements an Ant task that parses the XML files generated by JUnit and sends the test case
  * results to the Hackystat server.
  * 
  * @author Philip Johnson, Hongbing Kou, Joy Agustin, Julie Ann Sakuda, Aaron A. Kagawa
  * @version $Id: JUnitSensor.java,v 1.1.1.1 2005/10/20 23:56:58 johnson Exp $
  */
 public class JUnitSensor extends Task {
 
   /** The list of all XML filesets generated by the junit task. */
   private ArrayList<FileSet> filesets;
 
   /** Whether or not to print out messages during JUnit send. */
   private boolean verbose = false;
 
   /** The sensor shell instance used by this sensor. */
   private SensorShell shell;
 
   /** Specifies the runtime of the sensor. All issues sent will have the same runtime. */
   private long runtime = 0;
   
   /** Root of the source path, e.g. C:/svn/hackystat/hackySensor_JUnit. */
   private String sourcePath;
 
   /** Sensor properties to be used with the sensor. */
   private SensorProperties sensorProps;
 
   /** Timestamp set that will guarantee uniqueness of JUnit timestamps. */
   private TstampSet tstampSet;
 
   /** Tool in UserMap to use. */
   private String tool;
 
   /** Tool account in the UserMap to use. */
   private String toolAccount;
 
   /** Initialize a new instance of a JUnitSensor. */
   public JUnitSensor() {
     this.filesets = new ArrayList<FileSet>();
     this.tstampSet = new TstampSet();
     this.runtime = new Date().getTime();
   }
   
   /**
    * Initialize a new instance of a JUnitSensor, passing the host email, and password directly. This
    * supports testing. Note that when this constructor is called, offline data recovery by the
    * sensor is disabled.
    * 
    * @param host The hackystat host URL.
    * @param email The Hackystat email to use.
    * @param password The Hackystat password to use.
    */
   public JUnitSensor(String host, String email, String password) {
     this.filesets = new ArrayList<FileSet>();
     this.sensorProps = new SensorProperties(host, email, password);
     this.shell = new SensorShell(this.sensorProps, false, "test", false);
     this.tstampSet = new TstampSet();
     this.runtime = new Date().getTime();
   }
 
   /**
    * Set the verbose attribute to "on", "true", or "yes" to enable trace messages while the JUnit
    * sensor is running.
    * 
    * @param mode The new verbose value: should be "on", "true", or "yes" to enable.
    */
   public void setVerbose(String mode) {
     this.verbose = Project.toBoolean(mode);
   }
 
   /**
    * Sets source path of the root.
    * 
    * @param sourcePath Source file path.
    */
   public void setSourcePath(String sourcePath) {
     File sourcePathFile = new File(sourcePath);
     try {
       this.sourcePath = sourcePathFile.getCanonicalPath();
     }
     catch (IOException e) {
       // Invalid path
       this.sourcePath = null;
     }
   }
 
   /**
    * Allows the user to specify the tool in the UserMap that should be used when sending data. Note
    * that setting the tool will only have an effect if the tool account is also specified. Otherwise
    * it will be ignored and the values in v8.sensor.properties will be used.
    * 
    * @param tool The tool containing the tool account to be used when sending data.
    */
   public void setUserMapTool(String tool) {
     this.tool = tool;
   }
 
   /**
    * Allows the user to specify the tool account in the UserMap under the given tool to use when
    * sending data. Note that setting the tool account will only have an effect if the tool is also
    * specified. Otherwise the tool account will be ignored and v8.sensor.properties file values will
    * be used.
    * 
    * @param toolAccount The tool account in the UserMap to use when sending data.
    */
   public void setUserMapToolAccount(String toolAccount) {
     this.toolAccount = toolAccount;
   }
 
   /**
    * Parses the JUnit XML files and sends the resulting JUnit test case results to the hackystat
    * server. This method is invoked automatically by Ant.
    * 
    * @throws BuildException If there is an error.
    */
   @Override
   public void execute() throws BuildException {
     setupSensorShell();
 
     int numberOfTests = 0;
 
     Date startTime = new Date();
     try {
       // Get the file names from the FileSet directives.
       ArrayList<File> files = getFiles();
 
       // Iterate though each file, extract the JUnit data, send to sensorshell.
       for (Iterator<File> i = files.iterator(); i.hasNext();) {
         // get full path of next file to process
         String junitXmlFile = i.next().getPath();
         if (this.verbose) {
           System.out.println("Processing file: " + junitXmlFile);
         }
         numberOfTests += processJunitXmlFile(junitXmlFile);
       }
 
       if (send() > 0) {
         Date endTime = new Date();
         long elapsedTime = (endTime.getTime() - startTime.getTime()) / 1000;
         if (isUsingUserMap()) {
           // no sensorProps exists because we used the sensorshell map
           System.out.println("Hackystat data on " + numberOfTests + " JUnit tests sent to "
               + "host stored in UserMap with tool '" + this.tool + "' and tool account '"
               + this.toolAccount + "' (" + elapsedTime + " secs.)");
         }
         else {
           System.out.println("Hackystat data on " + numberOfTests + " JUnit tests sent to "
               + this.sensorProps.getHackystatHost() + " (" + elapsedTime + " secs.)");
         }
       }
       else if (numberOfTests == 0) {
         System.out.println("No data to send.");
       }
       else {
         System.out.println("Failed to send Hackystat JUnit test data.");
       }
     }
     catch (Exception e) {
       throw new BuildException("Errors occurred while processing the junit report file " + e);
     }
     finally { // After send-out, close the sensor shell.
       this.shell.quit();
     }
   }
 
   /**
    * Sets up the sensorshell instance to use either based on the given tool & tool account or from
    * the sensor.properties file. DO NOT call this method in the constructor. The optional properties
    * tool and tool account do not get set until after the constructor is done.
    */
   private void setupSensorShell() {
     if (isUsingUserMap()) {
       try {
         SensorShellMap map = new SensorShellMap(this.tool);
         this.shell = map.getUserShell(this.toolAccount);
       }
       catch (SensorShellMapException e) {
         throw new BuildException(e.getMessage(), e);
       }
     }
     // sanity check to make sure the prop and shell haven't already been set by the
     // constructor that takes in the email, password, and host
     else if (this.sensorProps == null && this.shell == null) {
       // use the sensor.properties file
       try {
         this.sensorProps = new SensorProperties();
         this.shell = new SensorShell(this.sensorProps, false, "JUnit");
       }
       catch (SensorPropertiesException e) {
         System.out.println(e.getMessage());
         System.out.println("Exiting...");
         throw new BuildException(e.getMessage(), e);
       }
 
       if (!this.sensorProps.isFileAvailable()) {
         System.out.println("Could not find sensor.properties file. ");
         System.out.println("Expected in: " + this.sensorProps.getAbsolutePath());
       }
     }
   }
 
   /**
    * Gets whether or not this sensor instance is using a mapping in the UserMap.
    * 
    * @return Returns true of the tool and tool account are set, otherwise false.
    */
   private boolean isUsingUserMap() {
     return (this.tool != null && this.toolAccount != null);
   }
 
   /**
    * Sends any accumulated data in the SensorShell to the server.
    * 
    * @return Returns the number of SensorData instances sent to the server.
    */
   public int send() {
     return this.shell.send();
   }
 
   /**
    * Parses a JUnit XML file and sends the JUnitEntry instances to the shell.
    * 
    * @param fileNameString The XML file name to be processed.
    * @exception BuildException if any error.
    * @return The number of test cases in this XML file.
    */
   public int processJunitXmlFile(String fileNameString) throws BuildException {
     XMLGregorianCalendar runtimeGregorian = LongTimeConverter.convertLongToGregorian(this.runtime);
     File xmlFile = new File(fileNameString);
 
     try {
       JAXBContext context = JAXBContext
           .newInstance(org.hackystat.sensor.ant.junit.resource.jaxb.ObjectFactory.class);
       Unmarshaller unmarshaller = context.createUnmarshaller();
 
       // One JUnit test suite per file
       Testsuite suite = (Testsuite) unmarshaller.unmarshal(xmlFile);
 
       String testClassName = suite.getName();
       // The start time for all entries will be approximated by the XML file's last mod time.
       // The shell will ensure that it's unique by tweaking the millisecond field.
       long startTime = xmlFile.lastModified();
 
       List<Testcase> testcases = suite.getTestcase();
       for (Testcase testcase : testcases) {
         // Test case name
         String testCaseName = testcase.getName();
 
         // Get the stop time
         double elapsedTime = testcase.getTime();
         long elapsedTimeMillis = (long) (elapsedTime * 1000);
 
         // Make a list of error strings.
         // This should always be a list of zero or one elements.
         List<String> stringErrorList = new ArrayList<String>();
         Error error = testcase.getError();
         if (error != null) {
           stringErrorList.add(error.getMessage());
         }
 
         // Make a list of failure strings.
         // This should always be a list of zero or one elements.
         List<String> stringFailureList = new ArrayList<String>();
         Failure failure = testcase.getFailure();
         if (failure != null) {
           stringFailureList.add(failure.getMessage());
         }
 
         String result = "pass";
        if (!stringErrorList.isEmpty() || !stringFailureList.isEmpty()) {
           result = "fail";
         }
 
         String name = testClassName + "." + testCaseName;
         // Alter startTime to guarantee uniqueness.
         long uniqueTstamp = this.tstampSet.getUniqueTstamp(startTime);
 
         // Get altered start time as XMLGregorianCalendar
         XMLGregorianCalendar startTimeGregorian = LongTimeConverter
             .convertLongToGregorian(uniqueTstamp);
 
         Map<String, String> keyValMap = new HashMap<String, String>();
         keyValMap.put("Tool", "JUnit");
         keyValMap.put("SensorDataType", "UnitTest");
         keyValMap.put("DevEvent-Type", "Test");
 
         // Required
         keyValMap.put("Runtime", runtimeGregorian.toString());
         keyValMap.put("Timestamp", startTimeGregorian.toString());
         keyValMap.put("Name", name);
         keyValMap.put("Resource", testCaseToPath(testClassName));
         keyValMap.put("Result", result);
 
         // Optional
         keyValMap.put("ElapsedTime", Long.toString(elapsedTimeMillis));
         keyValMap.put("TestName", testClassName);
         keyValMap.put("TestCaseName", testCaseName);
 
         if (!stringFailureList.isEmpty()) {
           keyValMap.put("FailureString", stringFailureList.get(0));
         }
 
         if (!stringErrorList.isEmpty()) {
           keyValMap.put("ErrorString", stringErrorList.get(0));
         }
 
         this.shell.add(keyValMap); // add data to sensorshell
       }
       return testcases.size();
     }
     catch (Exception e) {
       throw new BuildException("Failed to process " + fileNameString + "   " + e);
     }
   }
 
   /**
    * Makes of file name path from source path directory and fully-qualified test case name.
    * 
    * @param testCaseName Dot delimited test case name.
    * @return Source of test case name.
    */
   private String testCaseToPath(String testCaseName) {
     String path = this.sourcePath == null ? "" : this.sourcePath;
     if (path.length() > 0 && !path.endsWith("/")) {
       path += File.separator;
     }
 
     // Replace dot delimiters with slash.
     StringBuffer subPath = new StringBuffer();
     String[] fragments = testCaseName.split("\\.");
     for (int i = 0; i < fragments.length; i++) {
       subPath.append(fragments[i]);
       if (i < fragments.length - 1) {
         subPath.append(File.separator);
       }
     }
     // JUnit sensor is applicable on java file only
     subPath.append(".java");
     return path + subPath;
   }
 
   /**
    * Add a fileset which contains the junit report xml file to be processed. Invoked automatically
    * by Ant.
    * 
    * @param fs The new fileset of xml results.
    */
   public void addFileSet(FileSet fs) {
     this.filesets.add(fs);
   }
 
   /**
    * Returns all of the files in the fileset.
    * 
    * @return All files in the fileset.
    */
   private ArrayList<File> getFiles() {
     ArrayList<File> fileList = new ArrayList<File>();
     final int size = this.filesets.size();
     for (int i = 0; i < size; i++) {
       FileSet fs = this.filesets.get(i);
       DirectoryScanner ds = fs.getDirectoryScanner(getProject());
       ds.scan();
       String[] f = ds.getIncludedFiles();
 
       for (int j = 0; j < f.length; j++) {
         String pathname = f[j];
         File file = new File(ds.getBasedir(), pathname);
         file = getProject().resolveFile(file.getPath());
         fileList.add(file);
       }
     }
     return fileList;
   }
 }
