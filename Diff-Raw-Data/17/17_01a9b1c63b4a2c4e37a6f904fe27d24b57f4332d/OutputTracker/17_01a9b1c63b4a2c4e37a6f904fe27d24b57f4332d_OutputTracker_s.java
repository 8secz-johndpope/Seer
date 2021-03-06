 package org.broadinstitute.sting.gatk.io;
 
 import org.broadinstitute.sting.utils.JVMUtils;
 import org.broadinstitute.sting.utils.StingException;
 import org.broadinstitute.sting.utils.cmdLine.ArgumentSource;
 import org.broadinstitute.sting.utils.sam.SAMFileReaderBuilder;
 import org.broadinstitute.sting.gatk.walkers.Walker;
 import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
 import org.broadinstitute.sting.gatk.io.stubs.OutputStreamStub;
 import org.broadinstitute.sting.gatk.io.stubs.Stub;
 import org.broadinstitute.sting.gatk.io.storage.StorageFactory;
 import org.broadinstitute.sting.gatk.io.storage.Storage;
 import org.broadinstitute.sting.gatk.io.storage.OutputStreamStorage;
 
 import java.io.*;
 import java.lang.reflect.Field;
 import java.util.Map;
 import java.util.HashMap;
 
 /**
  * User: hanna
  * Date: Apr 30, 2009
  * Time: 9:40:09 AM
  * BROAD INSTITUTE SOFTWARE COPYRIGHT NOTICE AND AGREEMENT
  * Software and documentation are copyright 2005 by the Broad Institute.
  * All rights are reserved.
  *
  * Users acknowledge that this software is supplied without any warranty or support.
  * The Broad Institute is not responsible for its use, misuse, or
  * functionality.
  */
 
 /**
  * Manages the output and err streams that are created specifically for walker
  * output.
  */
 public abstract class OutputTracker {
     /**
      * The streams to which walker users should be reading directly.
      */
     protected Map<ArgumentSource, Object> inputs = new HashMap<ArgumentSource,Object>();
 
     /**
      * The streams to which walker users should be writing directly.
      */
     protected Map<Stub,Storage> outputs = new HashMap<Stub,Storage>();
 
     /**
      * Special-purpose stub.  Provides a connection to output streams.
      */
     protected OutputStreamStub outStub = null;
 
     /**
      * Special-purpose stream.  Provides a connection to error streams.
      */
     protected OutputStreamStub errStub = null;
 
     /**
      * Create an object to manage output given filenames for the output and error files.
      * If no files are specified, returns null.
      * @param outFileName Name of the output file.
      * @param errFileName Name of the error file.
      */
     public void initializeCoreIO( String outFileName, String errFileName ) {
         // If the two output streams match and are non-null, initialize them identically.
         // Otherwise, initialize them separately.
         if( outFileName != null && outFileName.equals(errFileName) ) {
             outStub = errStub = new OutputStreamStub(new File(outFileName));
             addOutput(outStub,new OutputStreamStorage(outStub));
         }
         else {
             outStub = (outFileName != null) ? new OutputStreamStub(new File(outFileName))
                                             : new OutputStreamStub(System.out);
             addOutput(outStub,new OutputStreamStorage(outStub));
 
             errStub = (errFileName != null) ? new OutputStreamStub(new File(errFileName))
                                             : new OutputStreamStub(System.err);
            addOutput(errStub,new OutputStreamStorage(outStub));
         }
     }
 
     /**
      * Gets the output storage associated with a given stub.
      * @param stub The stub for which to find / create the right output stream.
      * @param <T> Type of the stream to create.
      * @return Storage object with a facade of type T.
      */
     public abstract <T> T getStorage( Stub<T> stub );
 
     public void prepareWalker( Walker walker ) {
         installStub( walker, "out", new PrintStream(outStub) );
         installStub( walker, "err", new PrintStream(errStub) );
 
         for( Map.Entry<ArgumentSource,Object> io: inputs.entrySet() ) {
             ArgumentSource targetField = io.getKey();
             Object targetValue = io.getValue();
 
             // Ghastly hack: reaches in and finishes building out the SAMFileReader.
             // TODO: Generalize this, and move it to its own initialization step.
             if( targetValue instanceof SAMFileReaderBuilder) {
                 SAMFileReaderBuilder builder = (SAMFileReaderBuilder)targetValue;
                 builder.setValidationStringency(GenomeAnalysisEngine.instance.getArguments().strictnessLevel);
                 targetValue = builder.build();
             }
 
             JVMUtils.setField( targetField.field, walker, targetValue );
         }
     }
 
     /**
      * Provide a mechanism for injecting supplemental streams for external management.
      * @param argumentSource source Class / field into which to inject this stream.
      * @param stub Stream to manage.
      */
     public void addInput( ArgumentSource argumentSource, Object stub ) {
         inputs.put(argumentSource,stub);
     }
 
     /**
      * Provide a mechanism for injecting supplemental streams for external management.
      * @param stub Stream to manage.
      */
     public <T> void addOutput(Stub<T> stub) {
         addOutput(stub,null);
     }
 
     /**
      * Provide a mechanism for injecting supplemental streams for external management.
      * @param stub Stream to manage.
      */
     public <T> void addOutput(Stub<T> stub, Storage<T> storage) {
         stub.register(this);
         outputs.put(stub,storage);        
     }
 
     /**
      * Close down all existing output streams.
      */
     public void close() {
         for( Stub stub: outputs.keySet() ) {
             // If the stream hasn't yet been created, create it so that there's at least an empty file present.
             if( outputs.get(stub) == null )
                 getTargetStream(stub);
 
             // Close down the storage.
             outputs.get(stub).close();
         }
     }
 
     /**
      * Collects the target stream for this data.
      * @param stub The stub for this stream.
      * @param <T> type of stub.
      * @return An instantiated file into which data can be written.
      */
     protected <T> T getTargetStream( Stub<T> stub ) {
         if( !outputs.containsKey(stub) )
             throw new StingException("OutputTracker was not notified that this stub exists: " + stub);
         Storage<T> storage = outputs.get(stub);
         if( storage == null ) {
             storage = StorageFactory.createStorage(stub);
             outputs.put(stub,storage);
         }
         return (T)storage;
     }
 
     /**
      * Install an OutputStreamStub into the given fieldName of the given walker.
      * @param walker Walker into which to inject the field name.
      * @param fieldName Name of the field into which to inject the stub.
      */
     private void installStub( Walker walker, String fieldName, OutputStream outputStream ) {
         Field field = JVMUtils.findField( walker.getClass(), fieldName );
         JVMUtils.setField( field, walker, outputStream );
     }    
 }
