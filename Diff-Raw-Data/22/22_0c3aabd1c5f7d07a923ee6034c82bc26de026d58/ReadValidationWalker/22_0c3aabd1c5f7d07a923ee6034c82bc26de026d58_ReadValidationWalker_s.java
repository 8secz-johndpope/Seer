 package org.broadinstitute.sting.playground.gatk.walkers;
 
 import org.broadinstitute.sting.gatk.walkers.Walker;
 import org.broadinstitute.sting.gatk.walkers.ReadWalker;
 import org.broadinstitute.sting.utils.cmdLine.Argument;
 import org.broadinstitute.sting.utils.Utils;
 import org.broadinstitute.sting.utils.StingException;
 import net.sf.samtools.SAMRecord;
 import net.sf.samtools.SAMFileWriter;
 import net.sf.samtools.SAMReadGroupRecord;
 import net.sf.samtools.SAMFileHeader;
 
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.List;
 import java.util.ArrayList;
 import java.math.BigInteger;
 
 
 /*
  * Copyright (c) 2009 The Broad Institute
  *
  * Permission is hereby granted, free of charge, to any person
  * obtaining a copy of this software and associated documentation
  * files (the "Software"), to deal in the Software without
  * restriction, including without limitation the rights to use,
  * copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the
  * Software is furnished to do so, subject to the following
  * conditions:
  *
  * The above copyright notice and this permission notice shall be
  * included in all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
  * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
  * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
  * OTHER DEALINGS IN THE SOFTWARE.
  */
 
 /**
  * 
  * @author aaron 
  * 
  * Class ReadValidationWalker
  *
  * A descriptions should go here. Blame aaron if it's missing.
  */
 public class ReadValidationWalker extends ReadWalker<SAMRecord, SAMRecord> {
 
     // our MD5 sum
     private MessageDigest m;
 
     // private list of md5sums
     private final List<String> list = new ArrayList<String>();
 
     /**
      * The initialize function.
      */
     public void initialize() {
         try {
             m = MessageDigest.getInstance("MD5");
         } catch (NoSuchAlgorithmException e) {
             throw new StingException("Unable to get the MD5 algorithm. Get a more eXtreme version of JAVA!@!@!!");
         }
     }
 
     /**
      * The reads filter function.
      * @param ref the reference bases that correspond to our read, if a reference was provided
      * @param read the read itself, as a SAMRecord
      * @return true if the read passes the filter, false if it doesn't
      */
     public boolean filter(char[] ref, SAMRecord read) {
         return true;
 	}
 
     /**
      * The reads map function.
      * @param ref the reference bases that correspond to our read, if a reference was provided
      * @param read the read itself, as a SAMRecord
      * @return the read itself
      */
     public SAMRecord map( char[] ref, SAMRecord read ) {
         return read;
     }
 
     /**
      * reduceInit is called once before any calls to the map function.  We use it here to setup the output
      * bam file, if it was specified on the command line
      * @return SAMFileWriter, set to the BAM output file if the command line option was set, null otherwise
      */
     public SAMRecord reduceInit() {
         return null;
     }
 
     /**
      * given a read and a output location, reduce by emitting the read
      * @param read the read itself
      * @param output the output source
      * @return the SAMFileWriter, so that the next reduce can emit to the same source
      */
     public SAMRecord reduce( SAMRecord read, SAMRecord output ) {
         if (output == null)
             return read;
         if ((read.getReferenceIndex() == output.getReferenceIndex()) && (read.getAlignmentStart() < output.getAlignmentStart())) {
            System.err.println("saw the read " + read.getReadName() + " duplicated,  old alignment = " + output.getAlignmentStart());
         }
         else if (read.getReferenceIndex() != output.getReferenceIndex()){
            System.err.println("Switching Chromo");   
         }
         return read;
     }
 
 
     /**
      * when we're done traversing, close the reads file
      * @param output the SAMFileWriter we've used in the reduce phase
      */
     public void onTraversalDone( SAMFileWriter output ) {
         if (output != null) {
             output.close();
         }
     }
 }
