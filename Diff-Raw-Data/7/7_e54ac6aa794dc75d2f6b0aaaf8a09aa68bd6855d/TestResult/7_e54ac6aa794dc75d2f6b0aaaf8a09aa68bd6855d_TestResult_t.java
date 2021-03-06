 package hudson.tasks.junit;
 
 import hudson.model.AbstractBuild;
 import hudson.util.IOException2;
 import hudson.*;
 import org.apache.tools.ant.DirectoryScanner;
 import org.dom4j.DocumentException;
 import org.kohsuke.stapler.StaplerRequest;
 import org.kohsuke.stapler.StaplerResponse;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 /**
  * Root of all the test results for one build.
  *
  * @author Kohsuke Kawaguchi
  */
 public final class TestResult extends MetaTabulatedResult {
     /**
      * List of all {@link SuiteResult}s in this test.
      * This is the core data structure to be persisted in the disk.
      */
     private final List<SuiteResult> suites = new ArrayList<SuiteResult>();
 
     /**
      * {@link #suites} keyed by their names for faster lookup.
      */
     private transient Map<String,SuiteResult> suitesByName;
 
     /**
      * Results tabulated by package.
      */
     private transient Map<String,PackageResult> byPackages;
 
     // set during the freeze phase
     private transient TestResultAction parent;
 
     /**
      * Number of all tests.
      */
     private transient int totalTests;
     /**
      * Number of failed/error tests.
      */
     private transient List<CaseResult> failedTests;
 
     /**
      * Creates an empty result.
      */
     TestResult() {
     }
 
     /**
      * Collect reports from the given {@link DirectoryScanner}, while
      * filtering out all files that were created before the given tiem.
      */
     public TestResult(long buildTime, DirectoryScanner results) throws IOException {
         String[] includedFiles = results.getIncludedFiles();
         File baseDir = results.getBasedir();
 
         boolean parsed=false;
 
         for (String value : includedFiles) {
             File reportFile = new File(baseDir, value);
             // only count files that were actually updated during this build
             if(buildTime <= reportFile.lastModified()) {
                 parse(reportFile);
                 parsed = true;
             }
         }
 
         if(!parsed) {
            long localTime = System.currentTimeMillis();
            if(localTime < buildTime)
                throw new AbortException(
                    "Clock on this slave is out of sync with the master, and therefore I" +
                    "can't figure out what test results are new and what are old." +
                    "Please keep the slave clock in sync with the master.");

             File f = new File(baseDir,includedFiles[0]);
             throw new AbortException(
                 String.format(
                 "Test reports were found but none of them are new. Did tests run? "+
                 "For example, %s is %s old\n", f,
                 Util.getTimeSpanString(buildTime-f.lastModified())));
         }
     }
 
     /**
      * Parses an additional report file.
      */
     public void parse(File reportFile) throws IOException {
         try {
             suites.add(new SuiteResult(reportFile));
         } catch (DocumentException e) {
             if(!reportFile.getPath().endsWith(".xml"))
                 throw new IOException2("Failed to read "+reportFile+"\n"+
                     "Is this really a JUnit report file? Your configuration must be matching too many files",e);
             else
                 throw new IOException2("Failed to read "+reportFile,e);
         }
     }
 
     public String getDisplayName() {
         return "Test Result";
     }
 
     public AbstractBuild<?,?> getOwner() {
         return parent.owner;
     }
 
     @Override
     public TestResult getPreviousResult() {
         TestResultAction p = parent.getPreviousResult();
         if(p!=null)
             return p.getResult();
         else
             return null;
     }
 
     public String getTitle() {
         return "Test Result";
     }
 
     public String getChildTitle() {
         return "Package";
     }
 
     @Override
     public int getPassCount() {
         return totalTests-getFailCount();
     }
 
     @Override
     public int getFailCount() {
         return failedTests.size();
     }
 
     @Override
     public List<CaseResult> getFailedTests() {
         return failedTests;
     }
 
     @Override
     public Collection<PackageResult> getChildren() {
         return byPackages.values();
     }
 
     public PackageResult getDynamic(String packageName, StaplerRequest req, StaplerResponse rsp) {
         return byPackage(packageName);
     }
 
     public PackageResult byPackage(String packageName) {
         return byPackages.get(packageName);
     }
 
     public SuiteResult getSuite(String name) {
         return suitesByName.get(name);
     }
 
     /**
      * Builds up the transient part of the data structure
      * from results {@link #parse(File) parsed} so far.
      *
      * <p>
      * After the data is frozen, more files can be parsed
      * and then freeze can be called again.
      */
     public void freeze(TestResultAction parent) {
         this.parent = parent;
         if(suitesByName==null) {
             // freeze for the first time
             suitesByName = new HashMap<String,SuiteResult>();
             totalTests = 0;
             failedTests = new ArrayList<CaseResult>();
             byPackages = new TreeMap<String,PackageResult>();
         }
 
         for (SuiteResult s : suites) {
             if(!s.freeze(this))
                 continue;
 
             suitesByName.put(s.getName(),s);
 
             totalTests += s.getCases().size();
             for(CaseResult cr : s.getCases()) {
                 if(!cr.isPassed())
                     failedTests.add(cr);
 
                 String pkg = cr.getPackageName();
                 PackageResult pr = byPackage(pkg);
                 if(pr==null)
                     byPackages.put(pkg,pr=new PackageResult(this,pkg));
                 pr.add(cr);
             }
         }
 
         Collections.sort(failedTests,CaseResult.BY_AGE);
 
         for (PackageResult pr : byPackages.values())
             pr.freeze();
     }
 }
