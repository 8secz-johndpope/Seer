 package io.seqware.cli;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.PrintStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.SortedSet;
 import java.util.TreeSet;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import net.sourceforge.seqware.pipeline.runner.PluginRunner;
 
 /*
  * TODO:
  * - update seqware script to pull jar from proper location
  * - apply user-role filtering of available commands
  * - add descriptions to fields of create
  * - add copying helpers (e.g., S3)
  * - probably need to add developer functionality for controlled launching of bundle workflow
  */
 public class Main {
 
   private static String dl(List<String> tokens, String delim) {
     if (tokens.isEmpty()) {
       return "";
     } else {
       StringBuilder sb = new StringBuilder(tokens.get(0));
       for (int i = 1; i < tokens.size(); i++) {
         sb.append(delim);
         sb.append(tokens.get(i));
       }
       return sb.toString();
     }
   }
 
   private static String cdl(List<String> tokens) {
     return dl(tokens, ",");
   }
 
   private static void out(String format, Object... args) {
     System.out.println(String.format(format, args));
   }
 
   private static void err(String format, Object... args) {
     System.err.println(String.format(format, args));
   }
 
   private static void kill(String format, Object... args) {
     err(format, args);
     System.exit(1);
   }
 
   private static void invalid(String cmd) {
     kill("seqware: '%s' is not a seqware command. See 'seqware --help'.", cmd);
   }
 
   private static void invalid(String cmd, String sub) {
     kill("seqware: '%s %s' is not a seqware command. See 'seqware %s --help'.", cmd, sub, cmd);
   }
 
   private static void extras(List<String> args, String curCommand) {
     if (args.size() > 0) {
       kill("seqware: unexpected arguments to '%s': %s", curCommand, dl(args, " "));
     }
   }
 
   private static boolean flag(List<String> args, String flag) {
     boolean found = false;
     for (int i = 0; i < args.size(); i++) {
       if (flag.equals(args.get(i))) {
         if (found) {
           kill("seqware: multiple instances of '%s'.", flag);
         } else {
           found = true;
           args.remove(i);
         }
       }
     }
     return found;
   }
 
   private static List<String> optVals(List<String> args, String key) {
     List<String> vals = new ArrayList<String>();
 
     for (int i = 0; i < args.size();) {
       String s = args.get(i);
       if (key.equals(s)) {
         args.remove(i);
         if (i < args.size()) {
           vals.add(args.remove(i));
         } else {
           kill("seqware: missing required argument to '%s'.", key);
         }
       } else {
         i++;
       }
     }
 
     return vals;
   }
 
   private static String optVal(List<String> args, String key, String defaultVal) {
     String val = defaultVal;
 
     List<String> vals = optVals(args, key);
     if (vals.size() == 1) {
       val = vals.get(0);
     } else if (vals.size() > 1) {
       kill("seqware: multiple instances of '%s'.", key);
     }
 
     return val;
   }
 
   private static String reqVal(List<String> args, String key) {
     String val = optVal(args, key, null);
 
     if (val == null) {
       kill("seqware: missing required flag '%s'.", key);
     }
 
     return val;
   }
 
   private static boolean isHelp(List<String> args, boolean valOnEmpty) {
     if (args.isEmpty())
       return valOnEmpty;
 
     String first = args.get(0);
     return first.equals("-h") || first.equals("--help");
   }
 
   private static boolean isDev() {
     return Boolean.parseBoolean(System.getenv("SEQWARE_DEV"));
   }
 
   private static boolean isAdmin() {
     return Boolean.parseBoolean(System.getenv("SEQWARE_ADMIN"));
   }
 
   private static boolean isDaemon() {
     return Boolean.parseBoolean(System.getenv("SEQWARE_DAEMON"));
   }
 
   private static boolean isSuperUser() {
     return isDev() || isAdmin() || isDaemon();
   }
 
   public static final AtomicBoolean DEBUG = new AtomicBoolean(false);
 
   private static void run(String... args) {
     if (DEBUG.get()) {
       for (int i = 0; i < args.length; i++) {
         if (args[i].contains(" ")) {
           args[i] = "'" + args[i] + "'";
         }
       }
       out("PluginRunner.main: %s", dl(Arrays.asList(args), " "));
     } else {
       PluginRunner.main(args);
     }
   }
 
   private static void run(List<String> runnerArgs) {
     run(runnerArgs.toArray(new String[runnerArgs.size()]));
   }
 
   // COMMANDS:
 
   private static final SortedSet<String> ANNO_OBJS = new TreeSet<String>(Arrays.asList("experiment", "file", "ius",
                                                                                        "lane", "processing", "sample",
                                                                                        "sequencer-run", "study",
                                                                                        "workflow", "workflow-run"));
 
   private static void annotateHelp() {
     out("");
     out("Usage: seqware annotate [--help]");
     out("       seqware annotate <object> --accession <swid> --key <key> --val <value>");
     out("       seqware annotate <object> --accession <swid> --skip");
     out("       seqware annotate <object> --csv <file>");
     out("");
     out("Objects:");
     for (String obj : ANNO_OBJS) {
       out("  " + obj);
     }
     out("");
     out("Parameters:");
     out("  --csv <file>        Bulk annotation from CSV file of: accession, key, value.");
     out("  --accession <swid>  The SWID of the object to annotate");
     out("  --key <key>         The identifier of the annotation");
     out("  --skip              Sets the skip attribute flag on the object.");
     out("  --val <value>       The value of the annotation.");
     out("");
   }
 
   private static void annotate(List<String> args) {
     if (isHelp(args, true)) {
       annotateHelp();
     } else {
       String obj = args.remove(0);
       if (!ANNO_OBJS.contains(obj)) {
         kill("seqware: '%s' is not a valid object type.  See 'seqware annotate --help'.", obj);
       } else {
         if (isHelp(args, true)) {
           annotateHelp();
         } else {
           String swid = optVal(args, "--accession", null);
           String key = optVal(args, "--key", null);
           String val = optVal(args, "--val", null);
           boolean skip = flag(args, "--skip");
           String csv = optVal(args, "--csv", null);
 
           extras(args, "annotate " + obj);
 
           if (swid != null && key != null && val != null & skip == false && csv == null) {
             String idFlag = "--" + obj + "-accession";
             run("--plugin", "net.sourceforge.seqware.pipeline.plugins.AttributeAnnotator", "--", idFlag, swid, "--key",
                 key, "--value", val);
           } else if (swid != null && key == null && val == null & skip == true && csv == null) {
             String idFlag = "--" + obj + "-accession";
             run("--plugin", "net.sourceforge.seqware.pipeline.plugins.AttributeAnnotator", "--", idFlag, swid,
                 "--skip", "true");
           } else if (swid == null && key == null && val == null & skip == false && csv != null) {
             run("--plugin", "net.sourceforge.seqware.pipeline.plugins.AttributeAnnotator", "--", "--file", csv);
           } else {
             kill("seqware: invalid set of parameters to 'seqware annotate'. See 'seqware annotate --help'.");
           }
         }
       }
     }
   }
 
   private static void bundleValidate(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle validate [--help]");
       out("       seqware bundle validate --dir <bundle-dir>");
       out("");
       out("Parameters:");
       out("  --dir <bundle-dir>  The root directory of the bundle");
       out("");
     } else {
       String dir = reqVal(args, "--dir");
 
       extras(args, "bundle validate");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--validate", "--bundle", dir);
     }
   }
 
   private static void bundleInstall(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle list [--help]");
       out("       seqware bundle list --zip <bundle-zip>");
       out("");
       out("Parameters:");
       out("  --zip <bundle-zip>  The zip file of the bundle");
       out("");
     } else {
       String zip = reqVal(args, "--zip");
 
       extras(args, "bundle launch");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--install", "--bundle", zip);
     }
   }
 
   private static void bundleLaunch(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle launch [--help]");
       out("       seqware bundle launch --dir <bundle-dir>");
       out("");
       out("Parameters:");
       out("  --dir <bundle-dir>  The root directory of the bundle");
       out("");
     } else {
       String dir = reqVal(args, "--dir");
 
       extras(args, "bundle launch");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--test", "--bundle", dir);
     }
   }
 
   private static void bundleList(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle list [--help]");
       out("       seqware bundle list --dir <bundle-dir>");
       out("");
       out("Parameters:");
       out("  --dir <bundle-dir>  The root directory of the bundle");
       out("");
     } else {
       String dir = reqVal(args, "--dir");
 
       extras(args, "bundle list");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--list", "--bundle", dir);
     }
   }
 
   private static void bundlePackage(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle package [--help]");
       out("       seqware bundle package --dir <bundle-dir>");
       out("");
       out("Parameters:");
       out("  --dir <bundle-dir>  The root directory of the bundle");
       out("");
     } else {
       String dir = new File(reqVal(args, "--dir")).getAbsolutePath();
 
       extras(args, "bundle package");
 
       String outdir = new File("").getAbsolutePath();
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--path-to-package", dir,
           "--bundle", outdir);
     }
   }
 
   private static void bundle(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware bundle [--help]");
       out("       seqware bundle <sub-command> [--help]");
       out("");
       out("Sub-commands:");
       out("  install   Inform the Seqware system of the availability of a bundle");
       out("  launch    Launch a workflow from within a bundle directory");
       out("  list      List workflows within a bundle directory");
       out("  package   Package a bundle directory into a zip file");
       out("  validate  Validate that the bundle directory is structured correctly");
       out("");
     } else {
       String cmd = args.remove(0);
       if ("install".equals(cmd)) {
         bundleInstall(args);
       } else if ("launch".equals(cmd)) {
         bundleLaunch(args);
       } else if ("list".equals(cmd)) {
         bundleList(args);
       } else if ("package".equals(cmd)) {
         bundlePackage(args);
       } else if ("validate".equals(cmd)) {
         bundleValidate(args);
       } else {
         invalid("bundle", cmd);
       }
     }
   }
 
   private static void runCreateTable(List<String> args, String table, String... cols) {
     if (flag(args, "--interactive")) {
       extras(args, "create " + table.replace('_', '-'));
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.Metadata", "--", "--table", table, "--create",
           "--interactive");
     } else {
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.Metadata");
       runnerArgs.add("--");
       runnerArgs.add("--table");
       runnerArgs.add(table);
       runnerArgs.add("--create");
 
       for (int i = 0; i < cols.length; i++) {
         runnerArgs.add("--field");
         String key = "--" + cols[i].replace('_', '-');
         String arg = String.format("%s::%s", cols[i], reqVal(args, key));
         runnerArgs.add(arg);
       }
 
       extras(args, "create " + table.replace('_', '-'));
 
       run(runnerArgs);
     }
   }
 
   private static void createExperiment(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create experiment [--help]");
       out("       seqware create experiment --interactive");
       out("       seqware create experiment <fields>");
       out("");
       out("Note: It is strongly recommended that the '--interactive' mode be used when");
       out("      possible, since some columns have a dynamic set of allowable values.");
       out("");
       out("Required fields:");
       out("  --description <val>");
       out("  --platform-id <val>      Dynamic-valued column");
       out("  --study-accession <val>");
       out("  --title <val>");
       out("");
     } else {
       runCreateTable(args, "experiment", "description", "platform_id", "study_accession", "title");
     }
   }
 
   private static void createFile(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create file [--help]");
       out("       seqware create file --interactive");
       out("       seqware create file <fields>");
       out("");
       out("Required fields:");
       out("  --file <val>");
       out("  --meta-type <val>");
       out("  --parent-accession <val>");
       out("");
       out("Optional fields:");
       out("  --description <val>");
       out("  --type <val>");
       out("");
     } else {
       String file = reqVal(args, "--file");
       String meta = reqVal(args, "--meta-type");
       String parentId = reqVal(args, "--parent-accession");
       String type = optVal(args, "--type", "");
       String description = optVal(args, "--description", "");
 
       extras(args, "create file");
 
       String concat = String.format("%s::%s::%s::%s", type, meta, file, description);
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.ModuleRunner", "--", "--metadata-parent-accession",
           parentId, "--module", "net.sourceforge.seqware.pipeline.modules.GenericMetadataSaver", "--",
           "--gms-output-file", concat, "--gms-algorithm", "ManualProvisionFile");
     }
   }
 
   private static void createIus(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create ius [--help]");
       out("       seqware create ius --interactive");
       out("       seqware create ius <fields>");
       out("");
       out("Required fields:");
       out("  --barcode <val>");
       out("  --description <val>");
       out("  --lane-accession <val>");
       out("  --name <val>");
       out("  --sample-accession <val>");
       out("  --skip <val>");
       out("");
     } else {
       runCreateTable(args, "ius", "barcode", "description", "lane_accession", "name", "sample_accession", "skip");
     }
   }
 
   private static void createLane(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create lane [--help]");
       out("       seqware create lane --interactive");
       out("       seqware create lane <fields>");
       out("");
       out("Note: It is strongly recommended that the '--interactive' mode be used when");
       out("      possible, since some columns have a dynamic set of allowable values.");
       out("");
       out("Required fields:");
       out("  --cycle-descriptor <val>");
       out("  --description <val>");
       out("  --lane-number <val>");
       out("  --library-selection-accession <val>  Dynamic-valued field");
       out("  --library-source-accession <val>     Dynamic-valued field");
       out("  --library-strategy-accession <val>   Dynamic-valued field");
       out("  --name <val>");
       out("  --sequencer-run-accession <val>");
       out("  --skip <val>");
       out("  --study-type-accession <val>         Dynamic-valued field");
       out("");
     } else {
       runCreateTable(args, "lane", "cycle_descriptor", "description", "lane_number", "library_selection_accession",
                      "library_source_accession", "library_strategy_accession", "name", "sequencer_run_accession",
                      "skip", "study_type_accession");
     }
   }
 
   private static void createSample(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create sample [--help]");
       out("       seqware create sample --interactive");
       out("       seqware create sample <fields>");
       out("");
       out("Note: It is strongly recommended that the '--interactive' mode be used when");
       out("      possible, since some columns have a dynamic set of allowable values.");
       out("");
       out("Required fields:");
       out("  --description <val>");
       out("  --experiment-accession <val>");
       out("  --organism-id <val>           Dynamic-valued field");
       out("  --title <val>");
       out("");
     } else {
       runCreateTable(args, "sample", "description", "experiment_accession", "organism_id", "title");
     }
   }
 
   private static void createSequencerRun(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create sequencer-run [--help]");
       out("       seqware create sequencer-run --interactive");
       out("       seqware create sequencer-run <fields>");
       out("");
       out("Note: It is strongly recommended that the '--interactive' mode be used when");
       out("      possible, since some columns have a dynamic set of allowable values.");
       out("");
       out("Required fields:");
       out("  --description <val>");
       out("  --file-path <val>");
       out("  --name <val>");
       out("  --paired-end <val>");
       out("  --platform-accession <val>  Dynamic-valued field");
       out("  --skip <val>");
       out("");
     } else {
       runCreateTable(args, "sequencer_run", "description", "file_path", "name", "paired_end", "platform_accession",
                      "skip");
     }
   }
 
   private static void createStudy(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create study [--help]");
       out("       seqware create study --interactive");
       out("       seqware create study <fields>");
       out("");
       out("Note: It is strongly recommended that the '--interactive' mode be used when");
       out("      possible, since some columns have a dynamic set of allowable values.");
       out("");
       out("Required fields:");
       out("  --accession <val>");
       out("  --center-name <val>");
       out("  --center-project-name <val>");
       out("  --description <val>");
       out("  --study-type <val>           Dynamic-valued field");
       out("  --title <val>");
       out("");
     } else {
       runCreateTable(args, "study", "accession", "center_name", "center_project_name", "description", "study_type",
                      "title");
     }
   }
 
   private static void create(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware create [--help]");
       out("       seqware create <object> [--help]");
       out("");
       out("Objects:");
       out("  experiment");
       out("  file");
       out("  ius");
       out("  lane");
       out("  sample");
       out("  sequencer-run");
       out("  study");
       out("");
     } else {
       String obj = args.remove(0);
       if ("experiment".equals(obj)) {
         createExperiment(args);
       } else if ("file".equals(obj)) {
         createFile(args);
       } else if ("ius".equals(obj)) {
         createIus(args);
       } else if ("lane".equals(obj)) {
         createLane(args);
       } else if ("sample".equals(obj)) {
         createSample(args);
       } else if ("sequencer-run".equals(obj)) {
         createSequencerRun(args);
       } else if ("study".equals(obj)) {
         createStudy(args);
       } else {
         kill("seqware: '%s' is not a valid object type.  See 'seqware create --help'.", obj);
       }
     }
   }
 
   private static void filesReport(List<String> args) {
     if (isHelp(args, false)) {
       out("");
       out("Usage: seqware files report --help");
       out("       seqware files report <params>");
       out("");
       out("Optional parameters:");
       out("  --out <file>        The name of the output file");
       out("  --study <title>     Limit files to the specified study title");
       out("");
     } else {
       String study = optVal(args, "--study", null);
       String file = optVal(args, "--out", null);
 
       extras(args, "files report");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.SymLinkFileReporter");
       runnerArgs.add("--");
       runnerArgs.add("--no-links");
 
       if (study != null) {
         runnerArgs.add("--study");
         runnerArgs.add(study);
       }
       if (file != null) {
         runnerArgs.add("--output-filename");
         runnerArgs.add(file);
       }
 
       run(runnerArgs);
     }
   }
 
   private static void files(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware files --help");
       out("       seqware files <sub-command> [--help]");
       out("");
       out("Sub-commands:");
       out("  report          A report of the provenance of output files");
       out("");
     } else {
       String cmd = args.remove(0);
       if ("report".equals(cmd)) {
         filesReport(args);
       } else {
         invalid("files", cmd);
       }
     }
   }
 
   private static void workflowIni(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow ini --help");
       out("       seqware workflow ini <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>  The SWID of the workflow");
       out("");
       out("Optional parameters:");
       out("  --out <file>        Where to write the file (defaults to 'workflow.ini')");
       out("");
     } else {
       String id = reqVal(args, "--accession");
       String outfile = optVal(args, "--out", "workflow.ini");
 
       extras(args, "workflow ini");
 
       PrintStream origOut = System.out;
       PrintStream temp = null;
       try {
         temp = new PrintStream(outfile);
         System.setOut(temp);
         run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--list-workflow-params",
             "--workflow-accession", id);
         out("Created '%s'.", outfile);
       } catch (FileNotFoundException e) {
         kill("seqware: cannot write to '%s'.", outfile);
       } finally {
         System.setOut(origOut);
         if (temp != null) {
           temp.close();
         }
       }
     }
   }
 
   private static void workflowList(List<String> args) {
     if (isHelp(args, false)) {
       out("");
       out("Usage: seqware workflow list --help");
       out("       seqware workflow list");
       out("");
     } else {
       extras(args, "workflow list");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.BundleManager", "--", "--list-installed",
           "--human-expanded");
     }
   }
 
   private static void workflowReport(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow report --help");
       out("       seqware workflow report <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>  The SWID of the workflow");
       out("");
       out("Optional parameters:");
       out("  --out <file>        The name of the report file");
       out("  --tsv               Emit a tab-separated values report");
       out("  --when <date>       The date or date-range of runs to include");
       out("                      If omitted, all runs included");
       out("                      Dates are in the form YYYY-MM-DD");
       out("                      Date ranges are in the form YYYY-MM-DD:YYYY-MM-DD");
       out("");
     } else {
       String swid = reqVal(args, "--accession");
       String when = optVal(args, "--when", null);
       String out = optVal(args, "--out", null);
       boolean tsv = flag(args, "--tsv");
 
       extras(args, "workflow report");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.WorkflowRunReporter");
       runnerArgs.add("--");
       runnerArgs.add("--workflow-accession");
       runnerArgs.add(swid);
       if (when != null) {
         runnerArgs.add("--time-period");
         runnerArgs.add(when);
       }
       if (out != null) {
         runnerArgs.add("--output-filename");
         runnerArgs.add(out);
       } else {
         runnerArgs.add("--stdout");
       }
       if (!tsv) {
         runnerArgs.add("--human");
       }
 
       run(runnerArgs);
     }
   }
 
   private static void workflowSchedule(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow schedule [--help]");
       out("       seqware workflow schedule <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>         The SWID of the workflow to be run");
       out("");
       out("Optional parameters:");
       out("  --engine <type>            The engine that will process the workflow run");
       out("                             May be one of: 'oozie' or 'oozie-sge'");
       out("  --ini <ini-file>           An ini file to configure the workflow run ");
       out("                             Repeat this parameter to provide multiple files");
       out("  --host <host>              The host on which to launch the workflow run");
       out("  --parent-accession <swid>  The SWID of a parent to the workflow run");
       out("                             Repeat this parameter to provide multiple parents");
       out("");
     } else {
       String wfId = reqVal(args, "--accession");
       String engine = optVal(args, "--engine", null);
       List<String> iniFiles = optVals(args, "--ini");
       String host = optVal(args, "--host", null);
       List<String> parentIds = optVals(args, "--parent-accession");
 
       extras(args, "workflow schedule");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher");
       runnerArgs.add("--");
       runnerArgs.add("--schedule");
       runnerArgs.add("--workflow-accession");
       runnerArgs.add(wfId);
       if (engine != null) {
         runnerArgs.add("--workflow-engine");
         runnerArgs.add(engine);
       }
       if (!iniFiles.isEmpty()) {
         runnerArgs.add("--ini-files");
         runnerArgs.add(cdl(iniFiles));
       }
       if (!parentIds.isEmpty()) {
         runnerArgs.add("--parent-accessions");
         runnerArgs.add(cdl(parentIds));
       }
       if (host != null) {
         runnerArgs.add("--host");
         runnerArgs.add(host);
       }
 
       run(runnerArgs);
     }
   }
 
   private static void workflowRunReport(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow-run report --help");
       out("       seqware workflow-run report <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>  The SWID of the workflow run");
       out("");
       out("Optional parameters:");
       out("  --out <file>        The name of the report file");
       out("  --tsv               Emit a tab-separated values report");
       out("");
     } else {
       String swid = reqVal(args, "--accession");
       String out = optVal(args, "--out", null);
       boolean tsv = flag(args, "--tsv");
 
       extras(args, "workflow-run report");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.WorkflowRunReporter");
       runnerArgs.add("--");
       runnerArgs.add("--workflow-run-accession");
       runnerArgs.add(swid);
       if (out != null) {
         runnerArgs.add("--output-filename");
         runnerArgs.add(out);
       } else {
         runnerArgs.add("--stdout");
       }
       if (!tsv) {
         runnerArgs.add("--human");
       }
 
       run(runnerArgs);
     }
   }
 
   private static void workflow(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow [--help]");
       out("       seqware workflow <sub-command> [--help]");
       out("");
       out("Sub-commands:");
       out("  ini               Generate an ini file for a workflow");
       out("  list              List all installed workflows");
       out("  report            List the details of all runs of a given workflow");
       out("  schedule          Schedule a workflow to be run");
       out("");
     } else {
       String cmd = args.remove(0);
       if ("ini".equals(cmd)) {
         workflowIni(args);
       } else if ("list".equals(cmd)) {
         workflowList(args);
       } else if ("report".equals(cmd)) {
         workflowReport(args);
       } else if ("schedule".equals(cmd)) {
         workflowSchedule(args);
       } else {
         invalid("workflow", cmd);
       }
     }
   }
 
   private static void workflowRunLaunchScheduled(List<String> args) {
     if (isHelp(args, false)) {
       out("");
       out("Usage: seqware workflow-run launch-scheduled --help");
       out("       seqware workflow-run launch-scheduled");
       out("");
     } else {
       extras(args, "workflow-run launch-scheduled");
 
       run("--plugin", "net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher", "--", "--launch-scheduled");
     }
   }
 
   private static void workflowRunPropagateStatuses(List<String> args) {
     if (isHelp(args, false)) {
       out("");
       out("Usage: seqware workflow-run propagate-statuses --help");
       out("       seqware workflow-run propagate-statuses <params>");
       out("");
       out("Optional arameters:");
       out("  --threads <num>  The number of concurrent worker threads (default 1)");
     } else {
       String threads = optVal(args, "--threads", null);
 
       extras(args, "workflow-run propagate-statuses");
 
       if (threads == null) {
         run("--plugin", "net.sourceforge.seqware.pipeline.plugins.WorkflowStatusChecker");
       } else {
         run("--plugin", "net.sourceforge.seqware.pipeline.plugins.WorkflowStatusChecker", "--",
             "--threads-in-thread-pool", threads);
       }
     }
   }
 
   private static void workflowRunStderr(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow-run stderr --help");
       out("       seqware workflow-run stderr <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>  The SWID of the workflow run");
       out("");
       out("Optional parameters:");
       out("  --out <file>        The name of the file to write the stderr");
       out("");
     } else {
       String swid = reqVal(args, "--accession");
       String out = optVal(args, "--out", null);
 
       extras(args, "workflow-run stderr");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.WorkflowRunReporter");
       runnerArgs.add("--");
       runnerArgs.add("--workflow-run-accession");
       runnerArgs.add(swid);
       if (out != null) {
         runnerArgs.add("--output-filename");
         runnerArgs.add(out);
       }
       runnerArgs.add("--wr-stderr");
       run(runnerArgs);
     }
   }
 
   private static void workflowRunStdout(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow-run stdout --help");
       out("       seqware workflow-run stdout <params>");
       out("");
       out("Required parameters:");
       out("  --accession <swid>  The SWID of the workflow run");
       out("");
       out("Optional parameters:");
       out("  --out <file>        The name of the file to write the stdout");
       out("");
     } else {
       String swid = reqVal(args, "--accession");
       String out = optVal(args, "--out", null);
 
       extras(args, "workflow-run stdout");
 
       List<String> runnerArgs = new ArrayList<String>();
       runnerArgs.add("--plugin");
       runnerArgs.add("net.sourceforge.seqware.pipeline.plugins.WorkflowRunReporter");
       runnerArgs.add("--");
       runnerArgs.add("--workflow-run-accession");
       runnerArgs.add(swid);
       if (out != null) {
         runnerArgs.add("--output-filename");
         runnerArgs.add(out);
       }
       runnerArgs.add("--wr-stdout");
       run(runnerArgs);
     }
   }
 
   private static void workflowRun(List<String> args) {
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware workflow-run --help");
       out("       seqware workflow-run <sub-command> [--help]");
       out("");
       out("Sub-commands:");
       out("  launch-scheduled    Launch scheduled workflow runs");
       out("  propagate-statuses  Propagate workflow engine statuses to seqware meta DB");
       out("  stderr              Obtain the stderr output of the run");
       out("  stdout              Obtain the stdout output of the run");
       out("  report              The details of a given workflow-run");
       out("");
     } else {
       String cmd = args.remove(0);
       if ("launch-scheduled".equals(cmd)) {
         workflowRunLaunchScheduled(args);
       } else if ("propagate-statuses".equals(cmd)) {
         workflowRunPropagateStatuses(args);
       } else if ("stderr".equals(cmd)) {
         workflowRunStderr(args);
       } else if ("stdout".equals(cmd)) {
         workflowRunStdout(args);
       } else if ("report".equals(cmd)) {
         workflowRunReport(args);
       } else {
        invalid("workflow", cmd);
       }
     }
   }
 
   public static void main(String[] argv) {
     List<String> args = new ArrayList<String>(Arrays.asList(argv));
     if (flag(args, "--debug")) {
       DEBUG.set(true);
     }
 
     if (isHelp(args, true)) {
       out("");
       out("Usage: seqware [<flag>]");
       out("       seqware <command> [--help]");
       out("");
       out("Commands:");
       out("  annotate      Add arbitrary key/value pairs to seqware objects");
       out("  bundle        Interact with a workflow bundle");
       out("  create        Create new seqware objects (e.g., study)");
       out("  files         Extract information about workflow output files");
       out("  workflow      Interact with workflows");
       out("  workflow-run  Interact with workflow runs");
       out("");
       out("flags:");
       out("  --help        Print help out");
       // handled in seqware script:
       out("  --version     Print Seqware's version");
       out("");
     } else {
       String cmd = args.remove(0);
       if ("-v".equals(cmd) || "--version".equals(cmd)) {
         kill("seqware: version information is provided by the wrapper script.");
       } else if ("annotate".equals(cmd)) {
         annotate(args);
       } else if ("bundle".equals(cmd)) {
         bundle(args);
       } else if ("create".equals(cmd)) {
         create(args);
       } else if ("files".equals(cmd)) {
         files(args);
       } else if ("workflow".equals(cmd)) {
         workflow(args);
       } else if ("workflow-run".equals(cmd)) {
         workflowRun(args);
       } else {
         invalid(cmd);
       }
     }
   }
 
 }
