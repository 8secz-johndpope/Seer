 package net.sf.picard.analysis;
 
 import net.sf.picard.cmdline.CommandLineProgram;
 import net.sf.picard.cmdline.Option;
 import net.sf.picard.cmdline.StandardOptionDefinitions;
 import net.sf.picard.cmdline.Usage;
 import net.sf.picard.util.CollectionUtil;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 
 /**
  * Class that is designed to instantiate and execute multiple metrics programs that extend
  * SinglePassSamProgram while making only a single pass through the SAM file and supplying
  * each program with the records as it goes.
  *
  * @author Tim Fennell
  */
 public class CollectMultipleMetrics extends CommandLineProgram {
 
     /**
      * This interface allows developers to create Programs to run in addition to the ones defined in the Program enum.
      */
     public static interface ProgramInterface {
         SinglePassSamProgram makeInstance(final String outbase);
     }
 
     public static enum Program implements ProgramInterface {
         CollectAlignmentSummaryMetrics {
             @Override public SinglePassSamProgram makeInstance(final String outbase) {
                 final CollectAlignmentSummaryMetrics program = new CollectAlignmentSummaryMetrics();
                 program.OUTPUT = new File(outbase + ".alignment_summary_metrics");
                 return program;
             }
         },
         CollectInsertSizeMetrics {
             @Override
             public SinglePassSamProgram makeInstance(final String outbase) {
                 final CollectInsertSizeMetrics program = new CollectInsertSizeMetrics();
                 program.OUTPUT         = new File(outbase + ".insert_size_metrics");
                 program.HISTOGRAM_FILE = new File(outbase + ".insert_size_histogram.pdf");
                 return program;
             }
         },
         QualityScoreDistribution {
             public SinglePassSamProgram makeInstance(final String outbase) {
                 final QualityScoreDistribution program = new QualityScoreDistribution();
                 program.OUTPUT       = new File(outbase + ".quality_distribution_metrics");
                 program.CHART_OUTPUT = new File(outbase + ".quality_distribution.pdf");
                 return program;
             }
         },
         MeanQualityByCycle {
             public SinglePassSamProgram makeInstance(final String outbase) {
                 final MeanQualityByCycle program = new MeanQualityByCycle();
                 program.OUTPUT       = new File(outbase + ".quality_by_cycle_metrics");
                 program.CHART_OUTPUT = new File(outbase + ".quality_by_cycle.pdf");
                 return program;
             }
         };
 
     }
 
     @Usage
     public final String USAGE = getStandardUsagePreamble() +
 			"Takes an input BAM and reference sequence and runs one or more Picard " +
             "metrics modules at the same time to cut down on I/O. Currently all programs are run with " +
             "default options and fixed output extesions, but this may become more flexible in future.";
 
     @Option(shortName= StandardOptionDefinitions.INPUT_SHORT_NAME, doc="Input SAM or BAM file.")
     public File INPUT;
 
     @Option(shortName=StandardOptionDefinitions.REFERENCE_SHORT_NAME, doc="Reference sequence fasta.", optional=true)
     public File REFERENCE_SEQUENCE;
 
     @Option(doc="If true (default), then the sort order in the header file will be ignored.",
             shortName = StandardOptionDefinitions.ASSUME_SORTED_SHORT_NAME)
     public boolean ASSUME_SORTED = true;
 
     @Option(doc="Stop after processing N reads, mainly for debugging.")
     public int STOP_AFTER = 0;
 
     @Option(shortName=StandardOptionDefinitions.OUTPUT_SHORT_NAME, doc="Base name of output files.")
     public String OUTPUT;
 
     @Option(doc="List of metrics programs to apply during the pass through the SAM file.")
     public List<Program> PROGRAM = CollectionUtil.makeList(Program.values());
 
     /**
      * Contents of PROGRAM list is transferred to this list during command-line validation, so that an outside
      * developer can invoke this class programmatically and provide alternative Programs to run by calling
      * setProgramsToRun().
      */
     private List<ProgramInterface> programsToRun;
 
     // Stock main method
     public static void main(final String[] args) {
         new CollectMultipleMetrics().instanceMainWithExit(args);
     }
 
     @Override
     protected String[] customCommandLineValidation() {
         programsToRun = new ArrayList<ProgramInterface>(PROGRAM);
         return super.customCommandLineValidation();
     }
 
     /**
      * Use this method when invoking CollectMultipleMetrics programmatically to run programs other than the ones
      * available via enum.  This must be called before doWork().
      */
     public void setProgramsToRun(List<ProgramInterface> programsToRun) {
         this.programsToRun = programsToRun;
     }
 
    @Override protected int doWork() {
         if (OUTPUT.endsWith(".")) {
             OUTPUT = OUTPUT.substring(0, OUTPUT.length()-1);
         }
 
         final List<SinglePassSamProgram> programs = new ArrayList<SinglePassSamProgram>();
        for (Program program : new HashSet<Program>(PROGRAM)) {
             SinglePassSamProgram instance = program.makeInstance(OUTPUT);
 
             // Generally programs should not be accessing these directly but it might make things smoother
             // to just set them anyway
             instance.INPUT = INPUT;
             instance.REFERENCE_SEQUENCE = REFERENCE_SEQUENCE;
 
             instance.setDefaultHeaders(getDefaultHeaders());
 
             programs.add(instance);
         }
 
         SinglePassSamProgram.makeItSo(INPUT, REFERENCE_SEQUENCE, ASSUME_SORTED, STOP_AFTER, programs);
 
         return 0;
     }
 }
