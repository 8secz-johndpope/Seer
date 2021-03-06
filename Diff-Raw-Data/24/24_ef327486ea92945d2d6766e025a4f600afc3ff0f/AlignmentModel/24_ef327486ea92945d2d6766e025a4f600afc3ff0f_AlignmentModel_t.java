 package nextgen.core.model;
 import java.io.*;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.TreeSet;
 
 import org.apache.log4j.Logger;
 
 import broad.core.datastructures.IntervalTree;
 import broad.core.datastructures.IntervalTree.Node;
 import broad.core.math.EmpiricalDistribution;
 import broad.pda.annotation.BEDFileParser;
 import broad.pda.datastructures.Alignments;
 
 import net.sf.samtools.util.CloseableIterator;
 import nextgen.core.alignment.Alignment;
 import nextgen.core.alignment.AbstractPairedEndAlignment.TranscriptionRead;
 import nextgen.core.annotation.*;
 import nextgen.core.coordinatesystem.CoordinateSpace;
 import nextgen.core.coordinatesystem.GenomicSpace;
 import nextgen.core.feature.GeneWindow;
 import nextgen.core.feature.GenomeWindow;
 import nextgen.core.feature.Window;
 import nextgen.core.general.CloseableFilterIterator;
 import nextgen.core.model.score.WindowScore;
 import nextgen.core.readFilters.SameOrientationFilter;
 import nextgen.core.readFilters.SplicedReadFilter;
 import nextgen.core.readers.PairedEndReader;
 import nextgen.core.writers.PairedEndWriter;
 import nextgen.core.utils.AnnotationUtils;
 import nextgen.core.exception.RuntimeIOException;
 import org.apache.commons.collections15.Predicate;
 
 import nextgen.core.model.score.ScanStatisticScore;
 import nextgen.core.model.score.WindowProcessor;
 import nextgen.core.model.score.CountScore;
 import nextgen.core.model.score.WindowScoreIterator;
 
 /**
  * Serves the purpose of the Generic DataAlignment model and the ContinuousDataAlignmentModel
  * Will populate alignments
  * If a paired end file has unmapped mates, it will be represented by a single end read
  * If both mates are mapped it will be represented as paired end reads
  * Therefore a single model might have both, use flag X to exclude or include only one type
  * This will be a bridge to the analysis modules
  * @author skadri
  */
 
 public class AlignmentModel extends AbstractAnnotationCollection<Alignment> {
 	static Logger logger = Logger.getLogger(AlignmentModel.class.getName());
 	protected CoordinateSpace coordinateSpace;
 	PairedEndReader  reader;
 	String bamFile;
 	boolean hasSize=false;
 	int size;
 	Collection<Predicate<Alignment>> readFilters = new ArrayList<Predicate<Alignment>>();
 	private double globalLength = -99;
 	private double globalCount = -99;
 	private double globalLambda = -99;
 	//private double globalRpkmConstant = -99;
 	private Cache cache;
 	int cacheSize=1000000;
 	private boolean hasGlobalStats = false;
 	private SortedMap<String, Double> refSequenceCounts=new TreeMap<String, Double>();
 
 	/**
 	 * Build with a BAM file
 	 * Populate the alignment collection
 	 * @param bamFile
 	 * @param coordinateSpace 
 	 * @param readFilters 
 	 * @throws IOException 
 	 */
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace, Collection<Predicate<Alignment>> readFilters, boolean readOrCreatePairedEndBam) {
 
 		this(bamFile,coordinateSpace,readFilters,readOrCreatePairedEndBam,TranscriptionRead.UNSTRANDED);				
 	}
 	
 	public AlignmentModel(){
 		
 	}
 	
 	/**
 	 * Populate the alignment collection
 	 * @param bamFile
 	 * @param coordinateSpace 
 	 * @param readFilters 
 	 * @throws IOException 
 	 */
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace, Collection<Predicate<Alignment>> readFilters, boolean readOrCreatePairedEndBam,TranscriptionRead strand) {
 
 		this.bamFile=bamFile;
 		
 		if (readOrCreatePairedEndBam) {
 			this.bamFile = PairedEndReader.getOrCreatePairedEndFile(bamFile,strand);
 			String file = PairedEndReader.getPairedEndFile(bamFile);
 			if (file == null) {
 				file = PairedEndWriter.getDefaultFile(bamFile);
 				PairedEndWriter writer = new PairedEndWriter(new File(this.bamFile), file);
 				writer.convertInputToPairedEnd(strand);
 			}
 			this.bamFile = file;
 		} else {
 			this.bamFile = bamFile;
 		}
 				
 		// Establish the data alignment model like previously
 		this.reader = new PairedEndReader(new File(this.bamFile),strand);
 		
 		//If the passed coordinate space is null then make a Genomic Space
 		if(coordinateSpace==null){
 			//create a new GenomicSpace using the sizes in the BAM File
 			coordinateSpace=new GenomicSpace(reader.getRefSequenceLengths());
 		}
 		// Set the coordinate space
 		this.coordinateSpace=coordinateSpace;
 		
 		// Initialize the cache
 		this.cache=new Cache(this.reader, this.cacheSize);
 		
 		// Initialize the readFilters
 		this.readFilters.addAll(readFilters);
 				
 	}
 	
 	/**
 	 * Populate the alignment collection
 	 * @param bamFile
 	 * @param coordinateSpace 
 	 * @param readFilters 
 	 * @throws IOException 
 	 */
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace, Collection<Predicate<Alignment>> readFilters, boolean readOrCreatePairedEndBam,TranscriptionRead strand,boolean fragment) {
 
 		this.bamFile=bamFile;
 		
 		if (readOrCreatePairedEndBam) {
 			this.bamFile = PairedEndReader.getOrCreatePairedEndFile(bamFile,strand);
 			String file = PairedEndReader.getPairedEndFile(bamFile);
 			if (file == null) {
 				file = PairedEndWriter.getDefaultFile(bamFile);
 				PairedEndWriter writer = new PairedEndWriter(new File(this.bamFile), file);
 				writer.convertInputToPairedEnd(strand);
 			}
 			this.bamFile = file;
 		} else {
 			this.bamFile = bamFile;
 		}
 				
 		// Establish the data alignment model like previously
 		this.reader = new PairedEndReader(new File(this.bamFile),strand,fragment);
 		
 		//If the passed coordinate space is null then make a Genomic Space
 		if(coordinateSpace==null){
 			//create a new GenomicSpace using the sizes in the BAM File
 			coordinateSpace=new GenomicSpace(reader.getRefSequenceLengths());
 		}
 		// Set the coordinate space
 		this.coordinateSpace=coordinateSpace;
 		
 		// Initialize the cache
 		this.cache=new Cache(this.reader, this.cacheSize);
 		
 		// Initialize the readFilters
 		this.readFilters.addAll(readFilters);
 				
 	}
 	
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace, Collection<Predicate<Alignment>> readFilters) {
 		this(bamFile, coordinateSpace, readFilters, true);
 	}
 	
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace, boolean readOrCreatePairedEndBam) {
 		this(bamFile, coordinateSpace, new ArrayList<Predicate<Alignment>>(), readOrCreatePairedEndBam);
 	}
 	
 	public AlignmentModel(String bamFile, CoordinateSpace coordinateSpace) {
 		this(bamFile, coordinateSpace, true);
 	}
 
 	/**
 	 * Check whether the global stats file is valid
 	 * Compare the names of the reference sequences
 	 * @throws IOException
 	 */
 	private boolean validateGlobalStats(Map<String, Double> stats) throws IOException {
 		Collection<String> statsNames = stats.keySet();
 		Collection<String> referenceNames = new ArrayList<String>();
 		referenceNames.addAll(this.coordinateSpace.getReferenceNames());
 		for (String statsName : statsNames) {
 			referenceNames.remove(statsName);
 		}
 		if (referenceNames.size() > 0) {
 			logger.warn("Stats not validated due to missing reference counts.");
 			return false;
 		}
 		
 		if (!statsNames.contains("globalLength") || !statsNames.contains("globalCount") || !statsNames.contains("globalLambda")) {
 			logger.warn("Stats not validated due to missing global stats");
 			return false;
 		}
 		
 		// TODO we might also want to read/write/validate information about the read filters used to calculate global stats
 		return true; 
 	}
 	
 	/**
 	 * Compute global stats and cache to a file for future use, or read from file if already cached
 	 */
 	public void computeGlobalStats() {
 		try {
 			
 			SortedMap<String, Double> stats = null; 
 				
 			// Check if global stats have been stored in a file already
 			String precomputedStats = this.bamFile + getStatFileExtension();
 			
 			boolean loaded = true;
 			
 			File precomputedFile = new File(precomputedStats);
 			if (precomputedFile.exists()) {
 				//logger.info("Reading reference sequence stats from file " + precomputedStats);
 				stats = parseReferenceSequenceStats(precomputedStats);
 				if (!validateGlobalStats(stats)) {
 					logger.warn("Previous saved stats are not valid and will be overwritten.");
 					stats = null;
 				}
 			}
 			
 			// Compute stats
 			if (stats == null) {
 				logger.info("Computing global stats for bam file " + this.bamFile + "...");
 				loaded = false;
 				stats = getReferenceSequenceCounts();
 				
 				this.globalLength = (double) coordinateSpace.getLength();
 				this.globalCount = computeGlobalNumReads();
 				this.globalLambda = this.globalCount / this.globalLength;
 				stats.put("globalLength", this.globalLength);
 				stats.put("globalCount", this.globalCount);
 				stats.put("globalLambda", this.globalLambda);
 				logger.info("Done computing global stats.");
 			}
 
 			this.globalLength = stats.get("globalLength");
 			this.globalCount = stats.get("globalCount");
 			this.globalLambda = stats.get("globalLambda");
 
 			this.hasGlobalStats = true;
 
 			if (!loaded) {
 				// Store global stats in a file
 				logger.info("Writing global stats to file " + precomputedStats + " for future use.");
 				writeReferenceSequenceStats(stats, precomputedStats);
 				logger.info("Done writing global stats to file.");
 			}
 			
 			stats.remove("globalLength");
 			stats.remove("globalCount");
 			stats.remove("globalLambda");
 			this.refSequenceCounts = stats;
 		} catch (IOException e) {
 			throw new RuntimeIOException(e.getMessage());
 		}
 		
 	}
 	
 	
 	private String getStatFileExtension() {
 		return "." + coordinateSpace.getClass().getSimpleName() + "Stats";
 	}
 
 
 	public CloseableIterator<Alignment> getReadIterator() {
 		return new UnpackingIterator(cache.getReads());
 	}
 		
 	public CloseableIterator<Alignment> getReadIterator(Annotation region) {
 		return new UnpackingIterator(cache.getReads(region, false));
 	}
 
 	/**
 	 * Use the coordinate space to decide what is an overlapping read
 	 * @param window
 	 * @param fullyContained
 	 * @return
 	 */
 	public double getCount(Annotation window, boolean fullyContained) {
 		// Check to see if user is requesting a count of the whole chromosome, which we may have stored.
 		// Make sure not to just call this function in getChrLambda!  otherwise infinite loop
 		Annotation refAnnotation = null;
 		try {
 			refAnnotation = coordinateSpace.getReferenceAnnotation(window.getChr());
 		} catch (IllegalArgumentException e) {
 			logger.warn("Coordinate space does not contain reference " + window.getChr() + " ... getCount() returning 0.");
 			return 0.0;
 		}
 		if (refAnnotation != null && window.equals(refAnnotation)) {
 			//logger.info("getting saved chr count");
 			if(!this.hasGlobalStats) computeGlobalStats();
 			return refSequenceCounts.get(window.getChr());
 		} else {
 			CloseableIterator<AlignmentCount> iter=getOverlappingReadCounts(window, fullyContained);
 			double result = getCount(iter);
 			return result;
 		}
 	}
 	
 	/**
 	 * Use the coordinate space to decide what is an overlapping read using strandedness
 	 * @param window
 	 * @param fullyContained
 	 * @return
 	 */
 	public double getCountStranded(Annotation window, boolean fullyContained) {
 		// Check to see if user is requesting a count of the whole chromosome, which we may have stored.
 		// Make sure not to just call this function in getChrLambda!  otherwise infinite loop
 		Annotation refAnnotation = null;
 		try {
 			refAnnotation = coordinateSpace.getReferenceAnnotation(window.getChr());
 		} catch (IllegalArgumentException e) {
 			logger.warn("Coordinate space does not contain reference " + window.getChr() + " ... getCount() returning 0.");
 			return 0.0;
 		}
 		if (refAnnotation != null && window.equals(refAnnotation)) {
 			//logger.info("getting saved chr count");
 			if(!this.hasGlobalStats) computeGlobalStats();
 			return refSequenceCounts.get(window.getChr());
 		} else {
 			CloseableIterator<AlignmentCount> iter=getOverlappingReadCountsStranded(window, fullyContained);
 			double result = getCount(iter);
 			return result;
 		}
 	}
 
 	
 	/**
 	 * Use the coordinate space to decide what is an overlapping read
 	 * Gets the count over the entire genome
 	 * @return
 	 */
 	public double getCount() {
 		CloseableIterator<AlignmentCount> iter=this.cache.getReads();
 		return getCount(iter);
 	}
 	
 	
 	public double getCount(CloseableIterator<AlignmentCount> iter){
 		double counter=0;
 		while(iter.hasNext()){
 			AlignmentCount alignment=iter.next();
 			counter += alignment.getCount();
 		}
 		iter.close();
 		return counter;
 	}
 		
 	private double getCount(CloseableIterator<AlignmentCount> iter, Annotation excludedRegion){
 		double counter=0;
 		while(iter.hasNext()){
 			AlignmentCount alignment=iter.next();
 			if(!alignment.getRead().overlaps(excludedRegion)){
 				counter += alignment.getCount();
 			}
 		}
 		iter.close();
 		return counter;
 	}
 		
 	
 	public double getGlobalLambda() {
 		if(!this.hasGlobalStats){
 			computeGlobalStats();
 		}
 		return this.globalLambda;
 	}
 	
 	
 	public double getGlobalLength() {
 		if (!this.hasGlobalStats) {
 			computeGlobalStats();
 		}
 		return this.globalLength;
 	}
 	
 	
 	/**
 	 * Get total number of reads
 	 * @return Total reads
 	 */
 	private double computeGlobalNumReads() {
 		logger.info("Getting global read count");
 		return this.getCount();
 	}
 	
 	/**
 	 * Get the distribution of all read (or fragment) sizes with respect to a coordinate space
 	 * @param coord The coordinate space
 	 * @param maxSize The maximum read size to consider
 	 * @param numBins The number of bins for histogram
 	 * @return Distribution of read sizes with respect to coordinate space
 	 */
 	public EmpiricalDistribution getReadSizeDistribution(CoordinateSpace coord, int maxSize, int numBins) {
 		CloseableIterator<Alignment> iter = getReadIterator();
 		return calculateReadSizeDistribution(iter, coord, maxSize, numBins);
 	}
 
 
 	/**
 	 * Get the distribution of read (or fragment) sizes in an annotation
 	 * @param region The annotation in which to count read sizes
 	 * @param coord Coordinate space in which to compute read sizes
 	 * @param maxSize The maximum read size to check for
 	 * @param numBins The number of bins for histogram
 	 * @return Distribution of read sizes with respect to coordinate space
 	 * @throws IOException
 	 */
 	public EmpiricalDistribution getReadSizeDistribution(Annotation region, CoordinateSpace coord, int maxSize, int numBins) {
 		CloseableIterator<Alignment> iter = getOverlappingReads(region, true); //TODO Is this what we want?
 		return calculateReadSizeDistribution(iter, coord, maxSize, numBins);
 	}
 
 	private EmpiricalDistribution calculateReadSizeDistribution(CloseableIterator<Alignment> iter, CoordinateSpace coord, int maxSize, int numBins) {
 		int done = 0;
 		EmpiricalDistribution rtrn = new EmpiricalDistribution(numBins, 1, maxSize);
 		while(iter.hasNext()) {
 			Alignment align = iter.next();
 			done++;
 			if(done % 100000 == 0) logger.info("Got " + done + " records.");
 			try {
 				for(Integer size : align.getFragmentSize(coord)) {
 					double doublesize = size.doubleValue();
 					rtrn.add(doublesize);
 				}
 			} catch (NullPointerException e) {
 				// Catch NullPointerException if the fragment is from a chromosome that is not present in the TranscriptomeSpace
 				continue;
 			}
 		}
 		iter.close();
 		return rtrn;
 	}
 
 	
 	/**
 	 * Return the reads that overlap with this region in coordinate space
 	 */
 	private CloseableIterator<AlignmentCount> getOverlappingReadCounts(Annotation region, boolean fullyContained) {
 		//get Alignments over the whole region
 		return this.cache.query(region, fullyContained, this.coordinateSpace);
 	}
 	
 	/**
 	 * Return the reads that overlap with this region in coordinate space
 	 */
 	private CloseableIterator<AlignmentCount> getOverlappingReadCountsStranded(Annotation region, boolean fullyContained) {
 		//get Alignments over the whole region
 		Predicate<Alignment> filter=new SameOrientationFilter(region);
 		return new WrapAlignmentCountIterator(new CloseableFilterIterator<Alignment>(new UnpackingIterator(this.cache.query(region, fullyContained, this.coordinateSpace)), filter));
 	}
 	
 	/**
 	 * Return the reads that overlap with this region in coordinate space
 	 */
 	public CloseableIterator<Alignment> getOverlappingSplicedReads(Annotation region, boolean fullyContained) {
 		//get Alignments over the whole region
 		Predicate<Alignment> filter=new SplicedReadFilter();
 		return new CloseableFilterIterator<Alignment>(new UnpackingIterator(this.cache.query(region, fullyContained, this.coordinateSpace)), filter);
 	}
 
 	/**
 	 * Return the reads that overlap with this region in coordinate space
 	 */
 	public CloseableIterator<Alignment> getOverlappingSplicedReads(String chr) {
 		//get Alignments over the whole region
 		Predicate<Alignment> filter=new SplicedReadFilter();
 		return new CloseableFilterIterator<Alignment>(getOverlappingReads(chr), filter);
 	}
 	
 	public CloseableIterator<Alignment> getOverlappingReads(String chr) {
 		return new UnpackingIterator(getOverlappingReadCounts(chr));
 	}
 
 
 	private CloseableIterator<AlignmentCount> getOverlappingReadCounts(String chr) {
 		//get Alignments over the whole region
 		Annotation region=coordinateSpace.getReferenceAnnotation(chr);
 		return this.cache.query(region, false, this.coordinateSpace);
 	}
 	
 	@Override
 	public int getBasesCovered(Annotation region, boolean fullyContained) {
 		Annotation collapsed=collapse(region, fullyContained);
 		return collapsed.getSize();
 	}
 	
 	
 	@Override
 	public CloseableIterator<Alignment> getPermutedAnnotations(Annotation region) {
 		return new ShuffledIterator(this.getOverlappingReads(region, true), region);
 	}
 		
 
 	/**
 	 * Test whether a given record overlaps a window
 	 * @param record
 	 * @param window The region to use in coordinate space
 	 * @param fullyContained whether to count only reads that are fully contained within the window or overlapping the window
 	 * @return
 	 */
 	private boolean overlapsWindow(Alignment record, Collection<? extends Window> windowCS, boolean fullyContained) {
 		boolean count=false;
 		
 		//BasicAnnotation read=new BasicAnnotation(record.getChr(), record.getFragmentStart(), record.getFragmentEnd());
 		
 		for(Window window: windowCS){
 			if (((!fullyContained && window.overlaps(record)) || (fullyContained && window.contains(record)))) { //TODO I think we should be testing record.overlaps(window)
 			//if ((!fullyContained && record.overlaps(window)) || (fullyContained && window.contains(record))) { //TODO I think we should be testing record.overlaps(window)
 					
 				count = true;
 				break;
 			}
 		}
 		
 		return count;
 	}
 	
 	
 	/**
 	 * Get total number of reads mapping to all reference sequences
 	 * @return Total read count
 	 */
 	public double getGlobalNumReads() {
 		if(!this.hasGlobalStats) computeGlobalStats();
 		return this.globalCount;
 	}
 
 	
 	private boolean isValid(Alignment read) {
 		// TODO replace with Predicates#and
 		
 		for(Predicate<Alignment> filter: this.readFilters){
 			boolean passes=filter.evaluate(read);
 			if(!passes){return false;}
 		}
 		
 		return true;
 	}
 	
 	
 
 	public double getCount(Annotation window) {
 		return getCount(window, false);
 	}
 
 
 	private class WrapAlignmentCountIterator implements CloseableIterator<AlignmentCount>{
 		CloseableIterator<Alignment> iter;
 		
 		WrapAlignmentCountIterator (CloseableIterator<Alignment> iter) {
 			this.iter=iter;
 		}
 
 		@Override
 		public boolean hasNext() {
 			return iter.hasNext();
 		}
 
 		@Override
 		public AlignmentCount next() {
 			Alignment read=iter.next();
 			return new AlignmentCount(read);
 		}
 
 		@Override
 		public void remove() {}
 
 		@Override
 		public void close() {
 			iter.close();
 		}
 	}
 	
 	
 	private class NodeIterator implements CloseableIterator<AlignmentCount>{
 		Iterator<Node<Alignment>> iter;
 		Iterator<Alignment> subIter;
 
 		NodeIterator (Iterator<Node<Alignment>> overlappers) {
 			this.iter=overlappers;
 		}
 
 		@Override
 		public boolean hasNext() {
 			return iter.hasNext();
 		}
 
 		@Override
 		public AlignmentCount next() {
 			Node<Alignment> read=iter.next();
 			return new AlignmentCount(read.getValue(), read.getContainedValues());
 		}
 
 		@Override
 		public void remove() {}
 
 		@Override
 		public void close() {}
 		
 		/*
 		@Override
 		public boolean hasNext() {
 			if(subIter!=null && subIter.hasNext()) {return true;}
 			return iter.hasNext();
 			
 			//return iter.hasNext();
 		}
 
 		@Override
 		public AlignmentCount next() {
 			if(subIter!=null && subIter.hasNext()) {
 				return subIter.next();
 			}
 			else if(hasNext()){
 				Node<Alignment> align=iter.next();
 				subIter=align.getContainedValues().iterator();
 				return subIter.next();
 			}
 			
 			throw new IllegalStateException("Must call hasNext() before next()");
 			
 		//	return iter.next().getValue();
 		}
 
 		@Override
 		public void remove() {}
 
 		@Override
 		public void close() {}*/
 	}
 	
 	private class AlignmentCount{
 		Alignment read;
 		int numReplicates;
 		Collection<Alignment> containedReads;
 
 		AlignmentCount (Alignment read, Collection<Alignment> containedReads) {
 			this.read=read;
 			this.containedReads=containedReads;
 			this.numReplicates=containedReads.size();
 		}
 		
 		public AlignmentCount(Alignment read) {
 			this.read=read;
 			this.numReplicates=1;
 			this.containedReads=new TreeSet<Alignment>();
 			this.containedReads.add(read);
 		}
 
 		double getCount(){
 			return numReplicates*read.getWeight();
 		}
 		
 		int getNumReads(){
 			return this.numReplicates;
 		}
 		
 		Alignment getRead(){
 			return this.read;
 		}
 	}
 	
 	
 	
 	/**
 	 * This class will represent an iterator over the filtered reads
 	 * To make it efficient it will need to precompute the region in coordinate space
 	 * It will also need to know the next element to compute the hasNext and getNext() functions
 	 * @author mguttman
 	 *
 	 */
 	private class FilteredIterator implements CloseableIterator<AlignmentCount>{
 		// JE note:  extending nextgen.core.general.CloseableFilterIterator could clean this up a bit
 		
 		CloseableIterator<AlignmentCount> iter;
 		Collection<? extends Window> regionCS;
 		boolean fullyContained;
 		AlignmentCount next;
 		boolean hasWindow;
 		
 		FilteredIterator (CloseableIterator<AlignmentCount> overlappers, Annotation region, CoordinateSpace cs, boolean fullyContained) {
 			this.hasWindow=true;
 			this.iter=overlappers;
 			this.fullyContained=fullyContained;
 			this.regionCS=cs.getFragment(region);
 			hasNext(); //To initialize
 		}
 		
 		private Collection<? extends Window> filter(Collection<? extends Window> regionCS, Annotation region) {
 			Collection<Window> rtrn=new TreeSet<Window>();
 			
 			//go through each window and make sure it overlaps the region
 			for(Window window: regionCS){
 				if(window.overlaps(region)){rtrn.add(window);}
 			}
 			return rtrn;
 		}
 
 		/**
 		 * This is the constructor for all reads other parms not meaningful
 		 * @param reads
 		 */
 		FilteredIterator (CloseableIterator<AlignmentCount> reads) {
 			this.iter=reads;
 			this.hasWindow=false;
 			//this.fullyContained=fullyContained;
 			//this.regionCS=cs.getFragment(region.getReferenceName(), region.getStart(), region.getEnd());
 			hasNext(); //To initialize
 		}
 		
 		@Override
 		public boolean hasNext() {
 			//we will iterate through until we find an Alignment that passes filter
 			//if we hit the end return false
 			//else save the alignment and return true (cache fact that we have an unreturned element saved)
 			if(next!=null){return true;}
 			else{
 				//test for a next element and cache result
 				while(iter.hasNext()){
 					AlignmentCount alignment=getNext();
 					if(alignment!=null){
 						next=alignment;
 						return true;
 					}
 				}
 				return false;
 			}
 		}
 
 		@Override
 		public AlignmentCount next() {
 			//return next
 			AlignmentCount previousNext=next;
 			next=null;
 			//call hasNext
 			hasNext();
 			return previousNext;
 		}
 
 		private AlignmentCount getNext(){
 			AlignmentCount alignment=iter.next();
 			if(isValid(alignment.getRead())){
 				if(this.hasWindow) {
 					boolean contained=overlapsWindow(alignment.getRead(), regionCS, fullyContained); 
 					if(contained){return alignment;}
 				}
 				else {return alignment;}
 			}
 			return null;
 		}
 		
 		@Override
 		public void remove() {
 			// TODO Auto-generated method stub
 			throw new UnsupportedOperationException();
 		}
 
 		@Override
 		public void close() {
 			iter.close();
 			
 		}
 	}
 	
 	
 	private class ShuffledIterator extends nextgen.core.coordinatesystem.ShuffledIterator<Alignment> {
 		public ShuffledIterator(CloseableIterator<Alignment> itr, Annotation region) {
 			super(itr, coordinateSpace, region);
 		}
 		public Alignment next() {
 			Alignment next = itr.next();
 			Annotation blockedAnnotation = next.getReadAlignmentBlocks(coordinateSpace);
 			coordinateSpace.permuteAnnotation(blockedAnnotation, region);
 			next.moveToCoordinate(blockedAnnotation.getStart());
 			return next;
 		}
 	}
 	
 	
 	private class Cache{
 		String cacheChr = null;
 		int cacheStart = 0;
 		int cacheEnd = 0;
 		boolean fullyContained = false;
 		int cacheSize;
 		IntervalTree<Alignment> cachedTree;
 		
 		PairedEndReader reader;
 		
 		Cache(PairedEndReader reader, int cacheSize){
 			this.reader=reader;
 			this.cacheSize=cacheSize;
 		}
 		
 		//TODO Make sure "fullyContained" works
 		public FilteredIterator query(Annotation window, boolean fullyContained, CoordinateSpace cs) {
 			CloseableIterator<AlignmentCount> iter=query(window, fullyContained);
 			FilteredIterator filtered=new FilteredIterator(iter, window, cs, fullyContained);
 			return filtered;
 		}
 		
 		private CloseableIterator<AlignmentCount> query(Annotation window, boolean fullyContained) {
 			//if larger than the cache size then just return the query directly
 			if(window.getSize()>this.cacheSize){
 				return getReads(window, fullyContained);
 			}
 			//else if doesnt contain the window then update cache and query again
 			else if (!contains(window) || this.fullyContained != fullyContained) {
 				updateCache(window.getReferenceName(), window.getStart(), window.getEnd(), fullyContained);
 			}
 			//pull reads from cache
 			return getReadsFromCache(window);
 		}
 		
 		private CloseableIterator<AlignmentCount> getReadsFromCache(Annotation window) {
 			// TODO:  Can't we just call cachedTree.overlappersValueIterator(window.getStart(), window.getEnd()) and get rid of the NodeIterator?
 			//  it looks like overlappingValueIterator can handle multiple values per node
 			return new NodeIterator(this.cachedTree.overlappers(window.getStart(), window.getEnd()));
 		}
 
 		/**
 		 * Update the cache to have these new positions
 		 * @param chr
 		 * @param start
 		 * @param end
 		 */
 		private void updateCache(String chr, int start, int end, boolean fullyContained) {
 			int newStart=start;
 			int newEnd=end;
 
 			// if window is larger than cache size 
 			// (this will happen in TranscriptomeSpace if a transcript is longer than the cache size)
 			if ((end-start) > this.cacheSize) {
 				newStart=start;
 				newEnd=end;
 			} else {
 				if (this.cacheChr == null || chr.equalsIgnoreCase(this.cacheChr)) { 
 					if (end > this.cacheEnd) {
 						// Set the cache to start and the beginning of the requested sequence
 						newStart = start;
 						newEnd = start + this.cacheSize;
 					} else if (start < this.cacheStart) {
 						// Maybe we're scanning backwards?  So we'll fix the cache to the end of the window
 						newEnd = end;
 						newStart = end - this.cacheSize;
 					}
 				}
 			}
 
 			this.cacheChr=chr;
 			this.cacheStart=newStart;
 			this.cacheEnd=newEnd;
 			this.fullyContained = fullyContained;
 
 			Window update=new GenomeWindow(this.cacheChr, this.cacheStart, this.cacheEnd);
 
 			this.cachedTree=getIntervalTree(update, fullyContained);
 		}
 		
 		private IntervalTree<Alignment> getIntervalTree(Window w, boolean fullyContained) {
 		 	IntervalTree<Alignment> tree=new IntervalTree<Alignment>();
 			CloseableIterator<AlignmentCount> iterReadsOverlappingRegion=getReads(w, fullyContained);
 			
 			while(iterReadsOverlappingRegion.hasNext()){
 				Alignment record=iterReadsOverlappingRegion.next().getRead();
				if (isValid(record)) {
					tree.put(record.getAlignmentStart(), record.getAlignmentEnd(), record);
				}
 				
 				/*Node<Alignment> node=tree.find(record.getAlignmentStart(), record.getAlignmentEnd());
 							
 				if(node!=null){node.incrementCount();}
 				else{tree.put(record.getAlignmentStart(), record.getAlignmentEnd(), record);}*/
 			}
 			
 			iterReadsOverlappingRegion.close();
 			return tree;
 		}
 		
 		
 		private CloseableIterator<AlignmentCount> getReads(Annotation w, boolean fullyContained){
 			return new WrapAlignmentCountIterator(this.reader.query(w, fullyContained));
 		}
 		
 		
 		private CloseableIterator<AlignmentCount> getReads(){
 			return new FilteredIterator(new WrapAlignmentCountIterator(reader.iterator()));
 		}
 		
 		/**
 		 * Does the current cache have the window?
 		 * @param region The window to determine whether its in the cache
 		 */
 		private boolean contains(Annotation region) {
 			if (this.cacheChr==null) {
 				//Not yet initialized so cant be contained in cache
 				return false;
 			}
 			
 			if (this.cacheChr.equalsIgnoreCase(region.getReferenceName())) {
 				if (this.cacheStart <= region.getStart() && this.cacheEnd >= region.getEnd()) { return true; }
 			}
 			
 			return false;
 		}
 		
 		
 	}
 
 	public boolean containsReference(String refName){
 		return this.coordinateSpace.getReferenceNames().contains(refName);
 	}
 	
 	public double getGlobalCount() {
 		if (!this.hasGlobalStats) {
 			computeGlobalStats();
 		}
 		return this.globalCount;
 	}
 
 	@Override
 	public int size() {
 		// WARNING: slow
 		
 		if (!this.hasSize) {
 			computeSize();
 		}
 		return this.size;
 	}
 	
 
 	private void computeSize() {
 		int size=0;
 
 		//Lets iterate overall reads and count
 		CloseableIterator<Alignment> iter=this.reader.iterator();
 		while (iter.hasNext()) {
 			Alignment record=iter.next();
 			if (isValid(record)) {
 				size++;
 			}
 		}
 		iter.close();
 		
 		//set hasSize to true
 		this.hasSize=true;
 		this.size=size;
 	}
 
 
 	@Override
 	public void addFilter(Predicate<Alignment> filter) {
 		this.readFilters.add(filter);
 	}
 	
 	@Override
 	public void addFilters(Collection<Predicate<Alignment>> filters) {
 		for (Predicate<Alignment> filter : filters)
 			addFilter(filter);
 	}
 	
 	
 	/**
 	 * Scan windows over a Collection<GeneWindow> and score
 	 * @param windowSize Window size
 	 * @param overlap Overlap size
 	 * @param processor The window processor
 	 * @return A score iterator over windows
 	 */
 	@Override
 	public <T extends WindowScore> WindowScoreIterator<T> scan(Annotation region, int windowSize, int overlap, WindowProcessor<T> processor) {
 		Iterator<? extends Window> windowIterator = coordinateSpace.getWindowIterator(region, windowSize, overlap);
 		return new WindowScoreIterator<T>(windowIterator, processor, region);
 	}
 	
 	
 	/**
 	 * Return the score for annotation based on the scoring function passed in by WindowProcessor
 	 * @param window Region to score
 	 * @param processor How to score the region
 	 * @return
 	 */
 	public <T extends WindowScore> T getScore(Annotation window, WindowProcessor<T> processor){
 		T score=processor.processWindow(window);
 		return score;
 	}
 	
 	
 	
 	public WindowScoreIterator<CountScore> scan(Annotation region, int windowSize, int overlap) {
 		return scan(region, windowSize, overlap, getCountProcessor());
 	}
 
 
 	public long getRefSequenceLength(String refName) {
 		long length=this.coordinateSpace.getLength(refName);
 		return length;
 	}
 	
 	/**
 	 * Iterate through the whole coordinate space in windows and score them using the provided window processor
 	 * @param windowSize size of the windows to score
 	 */
 	public Iterator<? extends Window> windowIterator(int windowSize, int overlap) {
 		//call the coordinate space window iterator
 		Iterator<? extends Window> windowIterator = this.coordinateSpace.getWindowIterator(windowSize, overlap); 
 		
 		return windowIterator;
 	}
 	
 
 	public double getRefSequenceLambda(String refName) {
 		if (!hasGlobalStats) {
 			computeGlobalStats();
 		}
 		if(!containsReference(refName)) return 0.0;
 		return refSequenceCounts.get(refName) / getRefSequenceLength(refName);
 	}
 	
 	
 	private SortedMap<String, Double> getReferenceSequenceCounts() {
 		SortedMap<String, Double> counts = new TreeMap<String, Double>();
 		for (String refName : coordinateSpace.getReferenceNames()) {
 			counts.put(refName, getReferenceSequenceCount(refName));
 		}
 		return counts;
 	}
 	
 	
 	private double getReferenceSequenceCount(String refName) {
 		Annotation refRegion = coordinateSpace.getReferenceAnnotation(refName);
 		CloseableIterator<AlignmentCount> itr = getOverlappingReadCounts(refRegion, false);
 		double count = getCount(itr);
 		return count;
 	}
 
 	/*
 	private double computeLambda(String chr) {
 		double count=getChromosomeCount(chr);
 		long length=getChrLength(chr);
 		return count/length;
 	}*/
 	
 	
 	public CoordinateSpace getCoordinateSpace() {
 		return this.coordinateSpace;
 		
 	}
 
 
 	@Override
 	public int getBasesCovered(Annotation region) {
 		return getBasesCovered(region, false);
 	}
 	
 	
 	
 	private static SortedMap<String, Double> parseReferenceSequenceStats(String string) throws IOException {
 		SortedMap<String, Double> rtrn=new TreeMap<String, Double>();
 		
 		Collection<String> lines=BEDFileParser.loadList(string);
 		
 		for(String line: lines){
 			String[] tokens=line.split("\t");
 			rtrn.put(tokens[0], new Double(tokens[1]));
 		}
 		
 		return rtrn;
 	}
 	
 	
 	private static void writeReferenceSequenceStats(Map<String, Double> map, String save) throws IOException {
 		BufferedWriter writer=new BufferedWriter(new FileWriter(save));
 		for(String refName: map.keySet()){
 			writer.write(refName+"\t"+map.get(refName)+"\n");
 		}
 		writer.close();
 	}
 
 
 
 
 	/**
 	 * Removes a read filter from the set
 	 * @param readFilter to remove
 	 */
 	public void removeFilter(Predicate<Alignment> readFilter) {
 		readFilters.remove(readFilter);
 	}
 
 
 	/**
 	 * This will count the number of spliced reads with exactly this intron
 	 * @param intron the intron to count
 	 * @return
 	 */
 	public int getIntronCounts(Annotation intron) {
 		int count=0;
 		//Get overlapping spliced reads
 		CloseableIterator<Alignment> iter=getOverlappingSplicedReads(new Alignments(intron.getChr(), intron.getStart()-1, intron.getEnd()+1), false);
 			
 		//count reads with precise intron
 		while(iter.hasNext()){
 			Alignment read=iter.next();
 			Collection<? extends Annotation> introns=read.getSpliceConnections();
 			boolean contains=false;
 			for(Annotation i: introns){
 				//TODO: CHECK
 				if(i.equals(intron, false)){
 					contains=true;
 				}
 			}
 			if(contains){count++;}
 		}
 		//System.err.println(intron.toUCSC()+" Window overlaps record: "+count);
 		iter.close();
 		
 		return count;
 	}
 
 
 	public CloseableIterator<Alignment> getOverlappingReads(Annotation region, boolean fullyContained) {
 		CloseableIterator<AlignmentCount> iter=getOverlappingReadCounts(region, fullyContained);
 		return new UnpackingIterator(iter);
 	}
 	
 
 	/**
 	 * This will unpack the alignment counts into the full alignments that made them
 	 * @author mguttman
 	 *
 	 */
 	private class UnpackingIterator implements CloseableIterator<Alignment>{
 
 		CloseableIterator<AlignmentCount> iter;
 		Iterator<Alignment> currentIter;
 		
 		UnpackingIterator(CloseableIterator<AlignmentCount> iterator){
 			this.iter=iterator;
 			//currentIter=iter.next().containedReads.iterator();
 			hasNext();
 		}
 		
 		@Override
 		public boolean hasNext() {
 			if(currentIter!=null && currentIter.hasNext()){return true;}
 			else if(iter.hasNext()){
 				currentIter=iter.next().containedReads.iterator();
 				return true;
 			}
 			return false;
 		}
 
 		@Override
 		public Alignment next() {
 			return currentIter.next();
 		}
 
 		@Override
 		public void remove() {}
 
 		@Override
 		public void close() {
 			iter.close();
 		}
 		
 	}
 
 
 	@Override
 	public CloseableIterator<Alignment> getOverlappingAnnotations(Annotation region, boolean fullyContained) {
 		return this.getOverlappingReads(region, fullyContained);
 	}
 
 
 	@Override
 	public CloseableIterator<Alignment> iterator() {
 		return this.getReadIterator();
 	}
 
 
 	@Override
 	public double getCountExcludingRegion(Annotation region, Annotation excluded) {
 		CloseableIterator<AlignmentCount> iter=getOverlappingReadCounts(region,false);
 		double result = getCount(iter, excluded);
 		return result;
 	}
 
 
 	public double getSplicedCount(Alignments alignments, boolean fullyContained) {
 		CloseableIterator<Alignment> iter=getOverlappingSplicedReads(alignments, fullyContained);
 		return getCountForAlignment(iter);
 	}
 
 
 	private double getCountForAlignment(CloseableIterator<Alignment> iter) {
 		double counter=0;
 		while(iter.hasNext()){
 			Alignment alignment=iter.next();
 			counter += alignment.getWeight();
 		}
 		iter.close();
 		return counter;
 	}
 	
 	/**
 	 * This function returns an array list of counts for each position along the specified gene
 	 * @param gene
 	 * @return
 	 */
 	public double[] getCountsPerPosition(Gene gene){
 		
 		double[] rtrn = new double[gene.getSize()];
 		WindowScoreIterator<CountScore> scoreIter = scan(gene, 1, 0);
 		int i=0;
 		while(scoreIter.hasNext()) {
 			rtrn[i] = scoreIter.next().getCount();
 		}
 		return rtrn;
 	}
 	
 	
 	
 }
