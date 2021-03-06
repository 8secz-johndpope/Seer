 package org.molgenis.generator;
 
 import freemarker.template.Configuration;
 import freemarker.template.Template;
 import freemarker.template.TemplateException;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.molgenis.compute.ComputeJob;
 import org.molgenis.compute.ComputeParameter;
 import org.molgenis.compute.ComputeProtocol;
 import org.molgenis.pheno.ObservationTarget;
 import org.molgenis.protocol.Workflow;
 import org.molgenis.protocol.WorkflowElement;
 import org.molgenis.util.Tuple;
 
 import java.io.*;
 import java.util.*;
 
 /**
  * Created by IntelliJ IDEA.
  * User: georgebyelas
  * Date: 05/04/2012
  * Time: 09:46
  * To change this template use File | Settings | File Templates.
  */
 public class GenericJobGenerator implements JobGenerator
 {
     private static Logger logger = Logger.getLogger(GenericJobGenerator.class);
 
     //template sources
     private String templateGridDownload;
     private String templateGridDownloadExe;
     private String templateGridUpload;
     private String templateGridJDL;
     private String templateGridAfterExecution;
    private String templateGridUploadLog;
 
     private String templateClusterHeader;
     private String templateClusterFooter;
 
     //template filenames
     private String fileTemplateGridDownload = "templ-download-grid.ftl";
     private String fileTemplateGridDownloadExe = "templ-exe-grid.ftl";
     private String fileTemplateGridUpload = "templ-upload-grid.ftl";
    private String fileTemplateGridUploadLog = "templ-upload-grid-log.ftl";
     private String fileTemplateGridJDL = "templ-jdl-grid.ftl";
     private String fileTemplateGridAfterExecution = "templ-after-exe.ftl";
 
     private String fileTemplateClusterHeader = "templ-pbs-header.ftl";
     private String fileTemplateClusterFooter = "templ-pbs-footer.ftl";
 
     private Hashtable<String, GridTransferContainer> pairJobTransfers = null;
 
     private Hashtable<String, String> config;
 
     public Vector<ComputeJob> generateComputeJobsWorksheet(Workflow workflow, List<Tuple> worksheet, String backend)
     {
         Vector<ComputeJob> computeJobs = new Vector<ComputeJob>();
 
         if (backend.equalsIgnoreCase(JobGenerator.GRID))
             pairJobTransfers = new Hashtable<String, GridTransferContainer>();
 
         //because Hashtable does not allow null keys or values
         Hashtable<String, String> values = new Hashtable<String, String>();
 
         //parameters with templates
         Vector<ComputeParameter> complexParameters = new Vector<ComputeParameter>();
 
         //fill hashtable with workflow global parameters only once
         Collection<ComputeParameter> parameters = workflow.getWorkflowComputeParameterCollection();
         Iterator<ComputeParameter> itParameter = parameters.iterator();
         while (itParameter.hasNext())
         {
             ComputeParameter parameter = itParameter.next();
             if (parameter.getDefaultValue() != null)
             {
                 if (parameter.getDefaultValue().contains("${"))
                 {
                     complexParameters.addElement(parameter);
                 }
                 else
                 {
                     values.put(parameter.getName(), parameter.getDefaultValue());
                 }
             }
             else
                 values.put(parameter.getName(), "");
         }
 
         //produce jobs for every worksheet record
         for (int i = 0; i < worksheet.size(); i++)
         {
             //add parameters from worksheet to values
             Tuple tuple = worksheet.get(i);
             List<String> names = tuple.getFields();
 
             String id = "id";
             for (String name : names)
             {
                 String value = tuple.getString(name);
 
                 //to avoid empty worksheet fields
                 if (value == null)
                     break;
 
                 values.put(name, value);
                 id += "_" + value;
             }
 
             //weave complex parameters
             int count = 0;
             while ((complexParameters.size() > 0) && (count < 10))
             {
                 Vector<ComputeParameter> toRemove = new Vector<ComputeParameter>();
                 for (ComputeParameter computeParameter : complexParameters)
                 {
                     String complexValue = weaveFreemarker(computeParameter.getDefaultValue(), values);
                     //                   values.put(computeParameter.getName(), complexValue);
                     //                   complexParameters.remove(computeParameter);
 
                     if (complexValue.contains("${"))
                     {
                         System.out.println(computeParameter.getName() + " -> " + complexValue);
                     }
                     else
                     {
                         values.put(computeParameter.getName(), complexValue);
                         toRemove.add(computeParameter);
                     }
                 }
                 complexParameters.removeAll(toRemove);
                 System.out.println("loop " + count + " removed" + toRemove.size());
                 count++;
             }
             //read all workflow elements
             Collection<WorkflowElement> workflowElements = workflow.getWorkflowWorkflowElementCollection();
             Iterator<WorkflowElement> itr = workflowElements.iterator();
             while (itr.hasNext())
             {
                 WorkflowElement el = itr.next();
                 ComputeProtocol protocol = (ComputeProtocol) el.getProtocol();
                 String template = protocol.getScriptTemplate();
 
                 String jobListing = weaveFreemarker(template, values);
 
                 ComputeJob job = new ComputeJob();
 
                 //TODO review ComputeJob model
 
                 String jobName = config.get(JobGenerator.GENERATION_ID) + "_" +
                         workflow.getName() + "_" +
                         el.getName() + "_" + id;
 
                 job.setName(jobName);
                 job.setProtocol(protocol);
                 job.setComputeScript(jobListing);
                 computeJobs.add(job);
 
                 //fill job transfer container for grid jobs to ensure correct data transfer
                 if (backend.equalsIgnoreCase(JobGenerator.GRID))
                 {
                     GridTransferContainer container = fillContainer(protocol, values);
                     pairJobTransfers.put(job.getName(), container);
                 }
 
                 logger.log(Level.DEBUG, "----------------------------------------------------------------------");
                 logger.log(Level.DEBUG, el.getName());
                 logger.log(Level.DEBUG, jobListing);
                 logger.log(Level.DEBUG, "----------------------------------------------------------------------");
 
             }
         }
         return computeJobs;
     }
 
     private GridTransferContainer fillContainer(ComputeProtocol protocol, Hashtable<String, String> values)
     {
         GridTransferContainer container = new GridTransferContainer();
 
         List<ComputeParameter> inputs = protocol.getInputs();
         for (ComputeParameter input : inputs)
         {
             String name = input.getName();
             String value = values.get(name);
             container.addInput(name, value);
         }
 
         List<ComputeParameter> outputs = protocol.getOutputs();
         for (ComputeParameter output : outputs)
         {
             String name = output.getName();
             String value = values.get(name);
             container.addOutput(name, value);
         }
 
         List<ComputeParameter> exes = protocol.getExes();
         for (ComputeParameter exe : exes)
         {
             String name = exe.getName();
             String value = values.get(name);
             container.addExe(name, value);
         }
 
         List<ComputeParameter> logs = protocol.getLogs();
         for (ComputeParameter log : logs)
         {
             String name = log.getName();
             String value = values.get(name);
             container.addLog(name, value);
         }
 
         return container;
     }
 
     public Vector<ComputeJob> generateComputeJobsDB(Workflow workflow, List<ObservationTarget> worksheet, String backend)
     {
         return null;
     }
 
     public boolean generateActualJobs(Vector<ComputeJob> computeJobs, String backend, Hashtable<String, String> config)
     {
         //read templates
         String templatesDir = config.get(JobGenerator.TEMPLATE_DIR);
 
         if (backend.equalsIgnoreCase(JobGenerator.GRID))
             readTemplatesGrid(templatesDir);
         else if (backend.equalsIgnoreCase(JobGenerator.CLUSTER))
             readTemplatesCluster(templatesDir);
 
         for (ComputeJob computeJob : computeJobs)
         {
             //generate files for selected back-end
             if (backend.equalsIgnoreCase(JobGenerator.GRID))
                 generateActualJobGrid(computeJob, config);
             else if (backend.equalsIgnoreCase(JobGenerator.CLUSTER))
                 generateActualJobCluster(computeJob, config);
 
         }
 
         return true;
     }
 
     private void generateActualJobCluster(ComputeJob computeJob, Hashtable<String, String> config)
     {
 
     }
 
     private void generateActualJobGrid(ComputeJob computeJob, Hashtable<String, String> config)
     {
         //create values hashtable to fill templates
         Hashtable<String, String> values = new Hashtable<String, String>();
 
         values.put("script_name", computeJob.getName());
         values.put("error_log", "err_" + computeJob.getName() + ".log");
         values.put("output_log", "out_" + computeJob.getName() + ".log");
         values.put("script_location", config.get(JobGenerator.BACK_END_DIR));
 
         //create jdl
         String jdlListing = weaveFreemarker(templateGridJDL, values);
 
         //write jdl
         (new File(config.get(JobGenerator.OUTPUT_DIR))).mkdirs();
         writeToFile(config.get(JobGenerator.OUTPUT_DIR) + System.getProperty("file.separator") + computeJob.getName() + ".jdl",
                 jdlListing);
 
         //create shell
         String shellListing = "";
         String initialScript = computeJob.getComputeScript();
         GridTransferContainer container = pairJobTransfers.get(computeJob.getName());
 
         //get log filename
         Hashtable<String, String> logs = container.getLogs();
         Enumeration logValues = logs.elements();
         String logName = (String) logValues.nextElement();
         String justLogName = giveJustName(logName);
 
         //generate downloading section (transfer inputs and executable)
         //and change job listing to execute in the grid
         Hashtable<String, String> inputs = container.getInputs();
         Enumeration actuals = inputs.elements();
         while (actuals.hasMoreElements())
         {
             Hashtable<String, String> local = new Hashtable<String, String>();
 
             String actualName = (String) actuals.nextElement();
             String justName = giveJustName(actualName);
 
             local.put(JobGenerator.LFN_NAME, actualName);
             local.put(JobGenerator.INPUT, justName);
             local.put(JobGenerator.LOG, justLogName);
 
             String inputListing = weaveFreemarker(templateGridDownload, local);
            initialScript = initialScript.replaceAll(actualName, justName);
 
             shellListing += inputListing;
         }
 
         Hashtable<String, String> exes = container.getExes();
         actuals = exes.elements();
         while (actuals.hasMoreElements())
         {
             Hashtable<String, String> local = new Hashtable<String, String>();
 
             String actualName = (String) actuals.nextElement();
             String justName = giveJustName(actualName);
 
             local.put(JobGenerator.LFN_NAME, actualName);
             local.put(JobGenerator.INPUT, justName);
             local.put(JobGenerator.LOG, justLogName);
 
             String inputListing = weaveFreemarker(templateGridDownloadExe, local);
 
            System.out.println("-----------");
            System.out.println(initialScript);
            System.out.println("act " + actualName);
            System.out.println("just " + justName);
            initialScript = initialScript.replaceAll(actualName, justName);
             shellListing += inputListing;
         }
 
         shellListing += initialScript;
 
         //generate uploading section
         //and change job listing to execute in the grid
         Hashtable<String, String> outputs = container.getOutputs();
         actuals = outputs.elements();
         while (actuals.hasMoreElements())
         {
             Hashtable<String, String> local = new Hashtable<String, String>();
 
             String actualName = (String) actuals.nextElement();
             String justName = giveJustName(actualName);
 
             local.put(JobGenerator.LFN_NAME, actualName);
             local.put(JobGenerator.OUTPUT, justName);
             local.put(JobGenerator.LOG, justLogName);
 
             String outputListing = weaveFreemarker(templateGridUpload, local);
            shellListing = shellListing.replaceAll(actualName, justName);
 
             shellListing += outputListing;
         }
 
         //add upload log
         Hashtable<String, String> local = new Hashtable<String, String>();
 
         local.put(JobGenerator.LFN_NAME, logName);
         local.put(JobGenerator.OUTPUT, justLogName);
         local.put(JobGenerator.LOG, justLogName);
 
        String outputListing = weaveFreemarker(templateGridUploadLog, local);
         shellListing += outputListing;
 
         //write shell
         writeToFile(config.get(JobGenerator.OUTPUT_DIR) + System.getProperty("file.separator") + computeJob.getName() + ".sh",
                 shellListing);
    }
 
     private String giveJustName(String actualName)
     {
         int posSlash = actualName.lastIndexOf("/");
         String justName = actualName.substring(posSlash + 1);
         return justName;
     }
 
     private void readTemplatesCluster(String templatesDir)
     {
         try
         {
             templateClusterHeader = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateClusterHeader);
             templateClusterFooter = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateClusterFooter);
         }
         catch (IOException e)
         {
             e.printStackTrace();
         }
     }
 
     private void readTemplatesGrid(String templatesDir)
     {
         try
         {
             templateGridDownload = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridDownload);
             templateGridDownloadExe = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridDownloadExe);
             templateGridUpload = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridUpload);
            templateGridUploadLog = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridUploadLog);
             templateGridJDL = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridJDL);
             templateGridAfterExecution = getFileAsString(templatesDir + System.getProperty("file.separator") + fileTemplateGridAfterExecution);
         }
         catch (IOException e)
         {
             e.printStackTrace();
         }
     }
 
     public void setConfig(Hashtable<String, String> config)
     {
         this.config = config;
     }
 
     public String weaveFreemarker(String strTemplate, Hashtable<String, String> values)
     {
         Template t = null;
         StringWriter out = new StringWriter();
         try
         {
             t = new Template("name", new StringReader(strTemplate), new Configuration());
             t.process(values, out);
         }
         catch (TemplateException e)
         {
             //e.printStackTrace();
         }
         catch (IOException e)
         {
             //e.printStackTrace();
         }
 
         return out.toString();
     }
 
     private final String getFileAsString(String filename) throws IOException
     {
         File file = new File(filename);
 
         if (!file.exists())
         {
             logger.log(Level.ERROR, "template file " + filename + " does not exist");
             System.exit(1);
         }
         final BufferedInputStream bis = new BufferedInputStream(
                 new FileInputStream(file));
         final byte[] bytes = new byte[(int) file.length()];
         bis.read(bytes);
         bis.close();
         return new String(bytes);
     }
 
     public void writeToFile(String outfilename, String script)
     {
         try
         {
             BufferedWriter out = new BufferedWriter(new FileWriter(outfilename));
             out.write(script);
             out.close();
         }
         catch (IOException e)
         {
         }
     }
 
 }
