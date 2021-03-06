 package edu.unc.genomics.wigmath;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.nio.charset.Charset;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
 import org.apache.log4j.Logger;
 
 import com.beust.jcommander.Parameter;
 
 import edu.unc.genomics.CommandLineTool;
 import edu.unc.genomics.CommandLineToolException;
 import edu.unc.genomics.Contig;
 import edu.unc.genomics.Interval;
 import edu.unc.genomics.WigMathTool;
 import edu.unc.genomics.io.WigFileReader;
 import edu.unc.genomics.io.WigFileException;
 import edu.unc.utils.FloatCorrelation;
 import edu.unc.utils.WigStatistic;
 
 /**
  * Correlate multiple (Big)Wig files
  * @author timpalpant
  *
  */
 public class Correlate extends CommandLineTool {
 
 	private static final Logger log = Logger.getLogger(Correlate.class);
 
 	@Parameter(description = "Input files", required = true)
 	public List<String> inputFiles = new ArrayList<String>();
 	@Parameter(names = {"-w", "--window"}, description = "Window size (bp)")
 	public int windowSize = 100;
 	@Parameter(names = {"-s", "--step"}, description = "Sliding shift step (bp)")
 	public int stepSize = 50;
 	@Parameter(names = {"-m", "--metric"}, description = "Downsampling metric (coverage/total/mean/min/max)")
 	public String metric = "mean";
 	@Parameter(names = {"-t", "--type"}, description = "Correlation metric to use (pearson/spearman)")
 	public String type = "pearson";
 	@Parameter(names = {"-o", "--output"}, description = "Output file")
 	public Path outputFile;
 	
 	private WigStatistic dsm;
 	private Correlation corr;
 	private List<WigFileReader> wigs = new ArrayList<>();
 	private List<String> chromosomes;
 	int[] chrStarts, chrStops, chrLengths, nBins;
 	int totalNumBins = 0;
 	private float[][] correlationMatrix;
 	
 	@Override
 	public void run() throws IOException {
 		if (inputFiles.size() < 2) {
 			throw new CommandLineToolException("Cannot correlate < 2 input files.");
 		}
 		
 		dsm = WigStatistic.fromName(metric);
 		if (dsm == null) {
 			log.error("Unknown downsampling metric: "+metric);
 			throw new CommandLineToolException("Unknown downsampling metric: "+metric+". Options are mean, min, max, coverage, total");
 		} else {
 			log.debug("Using downsampling metric: "+metric);
 		}
 		
 		corr = Correlation.fromName(type);
 		if (corr == null) {
 			log.error("Unknown correlation metric: "+type);
 			throw new CommandLineToolException("Unknown correlation metric: "+type+". Options are pearson, spearman");
 		} else {
 			log.debug("Computing "+type+" correlation");
 		}
 		
 		log.debug("Initializing input files");
 		for (String inputFile : inputFiles) {
 			try {
 				wigs.add(WigFileReader.autodetect(Paths.get(inputFile)));
 			} catch (IOException e) {
 				log.error("IOError initializing input Wig file: " + inputFile);
 				e.printStackTrace();
 				throw new CommandLineToolException(e.getMessage());
 			}
 		}
 		log.debug("Initialized " + wigs.size() + " input files");
 		correlationMatrix = new float[wigs.size()][wigs.size()];
 		// Put ones down the diagonal
 		for (int i = 0; i < correlationMatrix.length; i++) {
 			correlationMatrix[i][i] = 1;
 		}
 		
 		// Get the maximum extent for each chromosome
		chromosomes = new ArrayList<>(WigMathTool.getCommonChromosomes(wigs));
 		chrStarts = new int[chromosomes.size()];
 		Arrays.fill(chrStarts, Integer.MAX_VALUE);
 		chrStops = new int[chromosomes.size()];
 		Arrays.fill(chrStops, Integer.MIN_VALUE);
 		for (int i = 0; i < chromosomes.size(); i++) {
 			String chr = chromosomes.get(i);
 			for (WigFileReader w : wigs) {
 				if (w.getChrStart(chr) < chrStarts[i]) {
 					chrStarts[i] = w.getChrStart(chr);
 				}
 				if (w.getChrStop(chr) > chrStops[i]) {
 					chrStops[i] = w.getChrStop(chr);
 				}
 			}
 		}
 		// Calculate the number of bins for each chromosome
 		chrLengths = new int[chromosomes.size()];
 		nBins = new int[chromosomes.size()];
 		for (int i = 0; i < chromosomes.size(); i++) {
 			chrLengths[i] = chrStops[i] - chrStarts[i] + 1;
 			nBins[i] = chrLengths[i] / stepSize;
 			if (nBins[i]*stepSize != chrLengths[i]) {
 				nBins[i]++;
 			}
 			totalNumBins += nBins[i];
 		}
 		log.debug("Total number of bins for all chromosomes = "+totalNumBins);
 		
 		// Compute the pairwise correlations between all files
 		// only keeping two files in memory at any one time
 		for (int i = 0; i < wigs.size()-1; i++) {
 			log.debug("Loading data from file "+i);
 			float[] binsI = getDataVector(wigs.get(i));
 			
 			for (int j = i+1; j < wigs.size(); j++) {
 				log.debug("Loading data from file "+j);
 				float[] binsJ = getDataVector(wigs.get(j));
 				
 				log.debug("Correlating ("+i+","+j+")");
 				switch (corr) {
 				case PEARSON:
 					correlationMatrix[i][j] = FloatCorrelation.pearson(binsI, binsJ);
 					break;
 				case SPEARMAN:
 					correlationMatrix[i][j] = FloatCorrelation.spearman(binsI, binsJ);
 					break;
 				}
 				
 				// Copy the correlation to the symmetric point in the matrix
 				correlationMatrix[j][i] = correlationMatrix[i][j];
 			}
 		}
 		
 		// Write the correlation matrix to output
 		StringBuilder output = new StringBuilder(type);
 		// Header row
 		for (int i = 0; i < wigs.size(); i++) {
 			output.append("\t"+wigs.get(i).getPath().getFileName());
 		}
 		// Data rows
 		for (int i = 0; i < wigs.size(); i++) {
 			output.append("\n"+wigs.get(i).getPath().getFileName());
 			for (int j = 0; j < wigs.size(); j++) {
 				output.append("\t"+correlationMatrix[i][j]);
 			}
 		}
 		
 		if (outputFile != null) {
 			log.debug("Writing to output file");
 			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
 				writer.write(output.toString());
 			}
 		} else {
 			System.out.println(output.toString());
 		}
 	}
 	
 	/**
 	 * Get a binned data vector from w
 	 * @param w a WigFile
 	 * @return a vector of binned values from w in a consistent order
 	 * @throws IOException 
 	 * @throws WigFileException 
 	 */
 	private float[] getDataVector(WigFileReader w) {
 		float[] values = new float[totalNumBins];
 		int binOffset = 0;
 		SummaryStatistics stats = new SummaryStatistics();
 		for (int i = 0; i < chromosomes.size(); i++) {
 			String chr = chromosomes.get(i);
 			int start = chrStarts[i];
 			int stop = chrStops[i];
 			try {
 				int chunkStart = start;
 				while (chunkStart <= stop) {
 					int chunkStop = Math.min(chunkStart+DEFAULT_CHUNK_SIZE-1, stop);
 					// Take bin-sized chunks
 					chunkStop = (chunkStop-chunkStart)/stepSize;
 					Interval chunk = new Interval(chr, chunkStart, chunkStop);
 					Contig result = w.query(chunk);
 
 					// Aggregate the values from this chunk in the correct bins
 					int binIndex = (chunkStart-start) / stepSize;
 					int binStart, binStop;
 					do {
 						binStart = binIndex*stepSize + 1;
 						binStop = binStart + windowSize - 1;
 						
 						stats.clear();
 						for (int bp = binStart; bp <= binStop; bp++) {
 							float value = result.get(bp);
 							if (!Float.isNaN(value)) {
 								stats.addValue(value);
 							}
 						}
 						
 						switch (dsm) {
 						case COVERAGE:
 							values[binOffset+binIndex] = stats.getN();
 							break;
 						case TOTAL:
 							values[binOffset+binIndex] = (float) stats.getSum();
 							break;
 						case MEAN:
 							values[binOffset+binIndex] = (float) stats.getMean();
 							break;
 						case MIN:
 							values[binOffset+binIndex] = (float) stats.getMin();
 							break;
 						case MAX:
 							values[binOffset+binIndex] = (float) stats.getMax();
 							break;
 						}
 						
 						binIndex++;
 					} while (binStop <= chunkStop);
 					
 					chunkStart = chunkStop + 1;
 				}
 			} catch (WigFileException | IOException e) {
 				log.error("Error getting data from wig file: "+w.getPath());
 				throw new CommandLineToolException(e);
 			}
 			
 			binOffset += nBins[i];
 		}
 		
 		return values;
 	}
 	
 	/**
 	 * @param args
 	 * @throws WigFileException 
 	 * @throws IOException 
 	 */
 	public static void main(String[] args) throws IOException, WigFileException {
 		new Correlate().instanceMain(args);
 	}
 	
 	public enum Correlation {
 		PEARSON("pearson"),
 		SPEARMAN("spearman");
 		
 		private String name;
 		
 		Correlation(final String name) {
 			this.name = name;
 		}
 		
 		public static Correlation fromName(final String name) {
 			for (Correlation c : Correlation.values()) {
 				if (c.getName().equalsIgnoreCase(name)) {
 					return c;
 				}
 			}
 			
 			return null;
 		}
 
 		/**
 		 * @return the name
 		 */
 		public String getName() {
 			return name;
 		}
 	}
 
 }
