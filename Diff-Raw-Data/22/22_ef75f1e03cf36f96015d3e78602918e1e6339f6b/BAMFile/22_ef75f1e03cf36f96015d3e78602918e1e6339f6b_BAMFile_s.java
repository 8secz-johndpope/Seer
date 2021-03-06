 package edu.unc.genomics.io;
 
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 
 import net.sf.samtools.BAMIndex;
 import net.sf.samtools.BAMIndexMetaData;
 import net.sf.samtools.SAMFileReader;
 import net.sf.samtools.SAMRecordIterator;
 import net.sf.samtools.SAMSequenceDictionary;
 import net.sf.samtools.SAMSequenceRecord;
 
 import edu.unc.genomics.SAMEntry;
 import edu.unc.genomics.util.Samtools;
 
 /**
  * A binary BAM file. Will passively index if an index is not available and random
  * queries are attempted.
  * 
  * @author timpalpant
  *
  */
 public class BAMFile extends IntervalFile<SAMEntry> {
 	
 	private static final Logger log = Logger.getLogger(BAMFile.class);
 	
 	private SAMFileReader reader;
 	private Path index;
 	private SAMRecordIterator it;
 	private boolean allowUnmappedReads = false;
 
 	public BAMFile(Path p) {
 		super(p);
 		
 		// Automatically index BAM files that do not have an index
 		reader = new SAMFileReader(p.toFile());
 		if (!reader.hasIndex()) {
			index = p.resolveSibling(p.getFileName()+".bai");
 			Samtools.indexBAMFile(p, index);
 			
 			// Now that we have an index, reset the reader
			reader = new SAMFileReader(p.toFile());
 			// and ensure that we now have an index
 			if (!reader.hasIndex()) {
 				throw new IntervalFileFormatException("Error indexing BAM file: "+p);
 			}
 		}
 		
 		// Turn off memory mapping to avoid BufferUnderRun exceptions
 		reader.enableIndexMemoryMapping(false);
 		// Turn on index caching
 		reader.enableIndexCaching(true);
 	}
 	
 	public BAMFile(Path p, boolean allowUnmappedReads) {
 		this(p);
 		this.allowUnmappedReads = allowUnmappedReads;
 	}
 
 	@Override
 	public void close() throws IOException {
 		reader.close();
		
		// Delete the index if we silently created it to enable querying
		if (index != null) {
			Files.deleteIfExists(index);
		}
 	}
 
 	@Override
 	public int count() {
 		int count = 0;
 		BAMIndex index = reader.getIndex();
     int nRefs = reader.getFileHeader().getSequenceDictionary().size();
     for (int i = 0; i < nRefs; i++) {
     	BAMIndexMetaData data = index.getMetaData(i);
     	count += data.getAlignedRecordCount();
     	count += data.getUnalignedRecordCount();
     }
     return count;
 	}
 
 	@Override
 	public Set<String> chromosomes() {
 		Set<String> chromosomes = new HashSet<String>();
 		SAMSequenceDictionary dict = reader.getFileHeader().getSequenceDictionary();
 		for (SAMSequenceRecord seqRec : dict.getSequences()) {
 			chromosomes.add(seqRec.getSequenceName());
 		}
 		return chromosomes;
 	}
 	
 	@Override
 	public Iterator<SAMEntry> iterator() {
 		// Close any previous iterators since SAM-JDK only allows one at a time
 		if (it != null) {
 			it.close();
 		}
 
 		it = reader.iterator();
 		return new SAMEntryIterator(it, allowUnmappedReads);
 	}
 
 	@Override
 	public Iterator<SAMEntry> query(String chr, int start, int stop) {
 		// Close any previous iterators since SAM-JDK only allows one at a time
 		if (it != null) {
 			it.close();
 		}
 
 		it = reader.query(chr, start, stop, false);
 		return new SAMEntryIterator(it, allowUnmappedReads);
 	}
 
 	/**
 	 * @return the allowUnmappedReads
 	 */
 	public boolean doesAllowUnmappedReads() {
 		return allowUnmappedReads;
 	}
 
 	/**
 	 * @param allowUnmappedReads the allowUnmappedReads to set
 	 */
 	public void setAllowUnmappedReads(boolean allowUnmappedReads) {
 		this.allowUnmappedReads = allowUnmappedReads;
 	}
 	
 }
