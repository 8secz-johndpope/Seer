 /**
  *  Copyright 2007-2008 University Of Southern California
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 
 package edu.isi.pegasus.planner.code.generator;
 
 import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;
 import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
 
 import edu.isi.pegasus.planner.code.CodeGeneratorException;
 
 import edu.isi.pegasus.common.logging.LogManager;
 import edu.isi.pegasus.planner.code.GridStart;
 import edu.isi.pegasus.planner.code.GridStartFactory;
 
 import edu.isi.pegasus.planner.code.POSTScript;
 
 import org.griphyn.cPlanner.classes.ADag;
 import org.griphyn.cPlanner.classes.SubInfo;
 import org.griphyn.cPlanner.classes.PegasusBag;
 
 import org.griphyn.cPlanner.common.DefaultStreamGobblerCallback;
 import org.griphyn.cPlanner.common.StreamGobbler;
 import org.griphyn.cPlanner.common.StreamGobblerCallback;
 
 import org.griphyn.cPlanner.namespace.Condor;
 import org.griphyn.cPlanner.namespace.Dagman;
 
 import org.griphyn.cPlanner.partitioner.graph.Adapter;
 import org.griphyn.cPlanner.partitioner.graph.Graph;
 import org.griphyn.cPlanner.partitioner.graph.GraphNode;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 
 import java.util.Collection;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * This code generator generates a shell script in the submit directory.
  * The shell script can be executed on the submit host to run the workflow
  * locally.
  *
  * @author Karan Vahi
  * @version $Revision$
  */
 public class Shell extends Abstract {
 
     /**
      * The prefix for events associated with job in jobstate.log file
      */
     public static final String JOBSTATE_JOB_PREFIX = "JOB";
     
     
     /**
      * The prefix for events associated with POST_SCRIPT in jobstate.log file
      */
     public static final String JOBSTATE_POST_SCRIPT_PREFIX = "POST_SCRIPT";
     
     
     /**
      * The prefix for events associated with job in jobstate.log file
      */
     public static final String JOBSTATE_PRE_SCRIPT_PREFIX = "PRE_SCRIPT";
     
 
     /**
      * The handle to the output file that is being written to.
      */
     private PrintWriter mWriteHandle;
 
     
     /**
      * Handle to the Site Store.
      */
     private SiteStore mSiteStore;
 
     /**
      * The handle to the GridStart Factory.
      */
     protected GridStartFactory mGridStartFactory;
 
     /**
      * A boolean indicating whether grid start has been initialized or not.
      */
     protected boolean mInitializeGridStart;
 
     
     /**
      * The default constructor.
      */
     public Shell( ){
         super();
         mInitializeGridStart = true;
         mGridStartFactory = new GridStartFactory();
     }
 
     /**
      * Initializes the Code Generator implementation.
      *
      *  @param bag   the bag of initialization objects.
      *
      * @throws CodeGeneratorException in case of any error occuring code generation.
      */
     public void initialize( PegasusBag bag ) throws CodeGeneratorException{
         super.initialize( bag );
         mLogger = bag.getLogger();
 
         //create the base directory recovery
         File wdir = new File(mSubmitFileDir);
         wdir.mkdirs();
 
         //get the handle to pool file
         mSiteStore = bag.getHandleToSiteStore();
 
     }
 
     /**
      * Generates the code for the concrete workflow in the GRMS input format.
      * The GRMS input format is xml based. One XML file is generated per
      * workflow.
      *
      * @param dag  the concrete workflow.
      *
      * @return handle to the GRMS output file.
      *
      * @throws CodeGeneratorException in case of any error occuring code generation.
      */
     public Collection<File> generateCode( ADag dag ) throws CodeGeneratorException{
         String opFileName = this.getPathToShellScript( dag ) ;
 
 
         initializeWriteHandle( opFileName );
         Collection result = new ArrayList( 1 );
         result.add( new File( opFileName ) );
         
 
         //write out the script header
         writeString(this.getScriptHeader( mSubmitFileDir ) );
 
         //we first need to convert internally into graph format
         Graph workflow =    Adapter.convert( dag );
 
         //traverse the workflow in topological sort order
         for( Iterator<GraphNode> it = workflow.topologicalSortIterator(); it.hasNext(); ){
             GraphNode node = it.next();
             SubInfo job = (SubInfo)node.getContent();
             generateCode( dag, job );
         }
 
         //write out the footer
         writeString(this.getScriptFooter());
         mWriteHandle.close();
 
         //set the XBit on the generated shell script
         setXBitOnFile( opFileName );
 
         //write out the braindump file
         this.writeOutBraindump( dag );
 
 
         return result;
     }
 
     /**
      * Generates the code for a single job in the input format of the workflow
      * executor being used.
      *
      * @param dag    the dag of which the job is a part of.
      * @param job    the <code>SubInfo</code> object holding the information about
      *               that particular job.
      *
      * @throws CodeGeneratorException in case of any error occuring code generation.
      */
     public void generateCode( ADag dag, SubInfo job ) throws CodeGeneratorException{
         mLogger.log( "Generating code for job " + job.getID() , LogManager.DEBUG_MESSAGE_LEVEL );
 
 
         //sanity check
         if( !job.getSiteHandle().equals( "local" ) ){
             throw new CodeGeneratorException( "Shell Code generator only works for jobs scheduled to site local" );
         }
 
         //initialize GridStart if required.
         if ( mInitializeGridStart ){
             mGridStartFactory.initialize( mBag, dag );
             mInitializeGridStart = false;
         }
 
         //determine the work directory for the job
         String execDir = getExecutionDirectory( job );
         
         //for local jobs we need initialdir
         //instead of remote_initialdir
         job.condorVariables.construct("initialdir", execDir );
         job.condorVariables.construct( "universe", "local" );
 
         SiteCatalogEntry site = mSiteStore.lookup( job.getSiteHandle() );
         String gridStartPath = site.getKickstartPath();
         GridStart gridStart = mGridStartFactory.loadGridStart( job , gridStartPath );
 
         //enable the job
         if( !gridStart.enable( job,false ) ){
             String msg = "Job " +  job.getName() + " cannot be enabled by " +
                          gridStart.shortDescribe() + " to run at " +
                          job.getSiteHandle();
             mLogger.log( msg, LogManager.FATAL_MESSAGE_LEVEL );
             throw new CodeGeneratorException( msg );
         }
         
         //apply the appropriate POSTScript
         POSTScript ps       = mGridStartFactory.loadPOSTScript( job, gridStart );
         boolean constructed = ps.construct( job, Dagman.POST_SCRIPT_KEY );
 
         //generate call to executeJob
        writeString( generateCallToExecuteJob( job, execDir, this.mSubmitFileDir ) );
         if( constructed ){
             //execute postscript and check for exitcode
             writeString( generateCallToExecutePostScript( job, mSubmitFileDir ) );
             writeString( generateCallToCheckExitcode( job, JOBSTATE_POST_SCRIPT_PREFIX ) );
         }
         else{
             //no postscript generated
             //generate the call to check_exitcode
             //check_exitcode  test1 JOB $?
             writeString( generateCallToCheckExitcode( job, JOBSTATE_JOB_PREFIX ) );
         }
         writeString( "" );
     }   
     
     /**
      * Returns a Map containing additional braindump entries that are specific
      * to a Code Generator
      * 
      * @param workflow  the executable workflow
      * 
      * @return Map
      */
     public  Map<String, String> getAdditionalBraindumpEntries( ADag workflow ) {
         Map entries = new HashMap();
         entries.put( Braindump.GENERATOR_TYPE_KEY, "shell" );
         entries.put( "script", this.getPathToShellScript( workflow ) );
         
         return entries;
     }
     
 
     /**
      * Generates a call to check_exitcode function that is used
      * 
      * @param job      the associated job
      * @param prefix   the prefix for the jobstate.log events
      * 
      * @return the call to execute job function.
      */
     protected String generateCallToCheckExitcode( SubInfo job,
                                                   String prefix ){
         StringBuffer sb = new StringBuffer();
             sb.append( "check_exitcode" ).append( " " ).
                append(  job.getID() ).append( " " ).
                append(  prefix ).append( " " ).
                append(  "$?" );
             
         return sb.toString();
     }
     
     /**
      * Generates a call to execute_post_script function , that is used to launch
      * a job from the shell script.
      * 
      * @param job        the job to be launched
      * @param directory  the directory in which the job needs to be launched.
      * 
      * @return the call to execute job function.
      */
     protected String generateCallToExecutePostScript( SubInfo job,
                                                       String directory ){
         StringBuffer sb = new StringBuffer();
         
         //gridstart modules right now store the executable
         //and arguments as condor profiles. Should be fixed.
         //This setting should happen only in Condor Generator
         String executable = (String) job.dagmanVariables.get( Dagman.POST_SCRIPT_KEY );
         StringBuffer args = new StringBuffer();
         args.append( (String)job.dagmanVariables.get( Dagman.POST_SCRIPT_ARGUMENTS_KEY ) ).
              append( " " ).append( (String)job.dagmanVariables.get( Dagman.OUTPUT_KEY)  ); 
         
         String arguments = args.toString();
 
         //generate the call to execute job function
         //execute_job $jobstate test1 /tmp /bin/echo "Karan Vahi" "stdin file" "k=v" "g=m"
         sb.append( "execute_post_script" ).append( " " ).
            append( job.getID() ).append( " " ).//the job id
            append( directory ).append( " " ).    //the directory in which we want the job to execute
            append( executable ).append( " " ). //the executable to be invoked
            append( "\"" ).append( arguments ).append( "\"" ).append( " " );//the arguments
 
 
         //handle stdin
         sb.append( "\"\"" );
         sb.append( " " );
 
         
         //add the environment variables
         
         return sb.toString();
     }
     
     /**
      * Generates a call to execute_job function , that is used to launch
      * a job from the shell script.
      * 
     * @param job               the job to be launched
     * @param scratchDirectory  the workflow specific execution directory created during running of the workflow
     * @param submitDirectory   the submit directory of the workflow
      * 
      * @return the call to execute job function.
      */
     protected String generateCallToExecuteJob( SubInfo job,
                                               String scratchDirectory,
                                               String submitDirectory ){
         StringBuffer sb = new StringBuffer();
         
         //gridstart modules right now store the executable
         //and arguments as condor profiles. Should be fixed.
         //This setting should happen only in Condor Generator
         String executable = (String) job.condorVariables.get( "executable" );
         String arguments = (String)job.condorVariables.get( Condor.ARGUMENTS_KEY );
         arguments = ( arguments == null ) ? "" : arguments;
 
        String directory = job.runInWorkDirectory() ? scratchDirectory : submitDirectory;

         //generate the call to execute job function
         //execute_job $jobstate test1 /tmp /bin/echo "Karan Vahi" "stdin file" "k=v" "g=m"
         sb.append( "execute_job" ).append( " " ).
            append( job.getID() ).append( " " ).//the job id
            append( directory ).append( " " ).    //the directory in which we want the job to execute
            append( executable ).append( " " ). //the executable to be invoked
            append( "\"" ).append( arguments ).append( "\"" ).append( " " );//the arguments
 
 
         //handle stdin for jobs
         String stdin = job.getStdIn();
         if( stdin == null || stdin.length() == 0 ){
             sb.append( "\"\"" );
         }
         else{
             if( stdin.startsWith( File.separator ) ){
                 sb.append( stdin );
             }
             else{
                 sb.append( this.mSubmitFileDir ).append( File.separator ).append( stdin );
             }
         }
         sb.append( " " );
 
         
         //add the environment variables
         for( Iterator it = job.envVariables.getProfileKeyIterator(); it.hasNext(); ){
             String key = (String)it.next();
             sb.append( "\"" ).
                append( key ).append( "=" ).append( job.envVariables.get( key ) ).
                append( "\"" ).append( " " );
         }
 
         return sb.toString();
     }
     
  
     /**
      * Returns the header for the generated shell script. The header contains
      * the code block that sources the common plan script from $PEGASUS_HOME/bin
      * and initializes the jobstate.log file.
      *
      * @param submitDirectory       the submit directory for the workflow.
      *
      * @return the script header
      */
     protected String getScriptHeader( String submitDirectory ){
 
         StringBuffer sb = new StringBuffer();
         sb.append( "#!/bin/bash" ).append( "\n" ).
            append( "#" ).append( "\n" ).
            append( "# executes the workflow in shell mode " ).append( "\n" ).
            append( "#" ).append( "\n" ).
            append( "\n");
 
         //check for PEGASUS_HOME
         sb.append( "if [ \"X${PEGASUS_HOME}\" = \"X\" ]; then" ).append( "\n" ).
            append( "   echo \"ERROR: Set your PEGASUS_HOME variable\" 1>&2").append( "\n" ).
            append( "   exit 1" ).append( "\n" ).
            append( "fi" ).append( "\n" ).
            append( "\n" );
 
 
         //check for common shell script before sourcing
         sb.append( "if [ ! -e ${PEGASUS_HOME}/bin/common-sh-plan.sh ];then" ).append( "\n" ).
            append( "   echo \"Unable to find common-sh-plan.sh file.\"" ).append( "\n" ).
            append( "   echo   \"You need to use Pegasus Version 3.0 or higher\"").append( "\n" ).
            append( "   exit 1 " ).append( "\n" ).
            append( "fi" ).append( "\n" );
         
         //source the common shell script
         sb.append( ".  ${PEGASUS_HOME}/bin/common-sh-plan.sh" ).append( "\n" ).
            append( "" ).append( "\n" );
 
         sb.append( "PEGASUS_SUBMIT_DIR" ).append( "=" ).append( submitDirectory ).append( "\n" );
 
 
         sb.append( "#initialize jobstate.log file" ).append( "\n" ).
            append( "JOBSTATE_LOG=jobstate.log" ).append( "\n" ).
            append( "touch $JOBSTATE_LOG" ).append( "\n" ).
            append( "echo \"INTERNAL *** SHELL_SCRIPT_STARTED ***\" >> $JOBSTATE_LOG" ).append( "\n" );
 
         return sb.toString();
 
     }
 
     /**
      * Returns the footer for the generated shell script.
      *
      * @return the script footer.
      */
     protected String getScriptFooter(){
 
         StringBuffer sb = new StringBuffer();
         sb.append( "echo \"INTERNAL *** SHELL_SCRIPT_FINISHED 0 ***\" >> $JOBSTATE_LOG" );
 
         return sb.toString();
 
     }
 
     /**
      * Returns path to the shell script that is generated
      * 
      * @param dag  the workflow 
      * @return path
      */
     protected String getPathToShellScript(ADag dag) {
         StringBuffer script = new StringBuffer();
         script.append( this.mSubmitFileDir ).append( File.separator ).
                append( dag.dagInfo.nameOfADag ).append( ".sh" );
         return script.toString();
     }
 
     /**
      * It initializes the write handle to the output file.
      *
      * @param filename  the name of the file to which you want the write handle.
      */
     private void initializeWriteHandle(String filename) throws CodeGeneratorException{
         try {
             File f = new File( filename );
             mWriteHandle = new PrintWriter(new FileWriter( f ));
             mLogger.log("Writing to file " + filename , LogManager.DEBUG_MESSAGE_LEVEL);
         }
         catch (Exception e) {
             throw new CodeGeneratorException( "Unable to initialize file handle for shell script ", e );
             
         }
 
         
     }
 
     /**
      * Writes a string to the associated write handle with the class
      *
      * @param st  the string to be written.
      */
     protected void writeString(String st){
         //try{
             //write the xml header
             mWriteHandle.println(st);
         /*}
         catch(IOException ex){
             System.out.println("Error while writing to xml " + ex.getMessage());
         }*/
     }
 
     /**
      * Returns the directory in which a job should be executed.
      *
      * @param job  the job.
      *
      * @return  the directory
      */
     protected String getExecutionDirectory(SubInfo job) {
        String execSiteWorkDir = mSiteStore.getWorkDirectory(job);
        String workdir = (String) job.globusRSL.removeKey("directory"); // returns old value
        workdir = (workdir == null)?execSiteWorkDir:workdir;
 
        return workdir;
     }
 
     /**
      * Sets the xbit on the file.
      *
      * @param file   the file for which the xbit is to be set
      *
      * @return boolean indicating whether xbit was set or not.
      */
     protected boolean  setXBitOnFile( String file ) {
         boolean result = false;
 
         //do some sanity checks on the source and the destination
         File f = new File( file );
         if( !f.exists() || !f.canRead()){
             mLogger.log("The file does not exist " + file,
                         LogManager.ERROR_MESSAGE_LEVEL);
             return result;
         }
 
         try{
             //set the callback and run the grep command
             Runtime r = Runtime.getRuntime();
             String command = "chmod +x " + file;
             mLogger.log("Setting xbit " + command,
                         LogManager.DEBUG_MESSAGE_LEVEL);
             Process p = r.exec(command);
 
             //the default gobbler callback always log to debug level
             StreamGobblerCallback callback =
                new DefaultStreamGobblerCallback(LogManager.DEBUG_MESSAGE_LEVEL);
             //spawn off the gobblers with the already initialized default callback
             StreamGobbler ips =
                 new StreamGobbler(p.getInputStream(), callback);
             StreamGobbler eps =
                 new StreamGobbler(p.getErrorStream(), callback);
 
             ips.start();
             eps.start();
 
             //wait for the threads to finish off
             ips.join();
             eps.join();
 
             //get the status
             int status = p.waitFor();
             if( status != 0){
                 mLogger.log("Command " + command + " exited with status " + status,
                             LogManager.DEBUG_MESSAGE_LEVEL);
                 return result;
             }
             result = true;
         }
         catch(IOException ioe){
             mLogger.log("IOException while creating symbolic links ", ioe,
                         LogManager.ERROR_MESSAGE_LEVEL);
         }
         catch( InterruptedException ie){
             //ignore
         }
         return result;
     }
 
 
 }
