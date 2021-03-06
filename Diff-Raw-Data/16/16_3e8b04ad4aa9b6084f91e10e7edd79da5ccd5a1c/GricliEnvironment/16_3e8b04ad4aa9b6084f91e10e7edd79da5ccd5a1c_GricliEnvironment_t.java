 package grisu.gricli;
 
 import grisu.control.ServiceInterface;
 import grisu.frontend.model.job.JobObject;
 import grisu.jcommons.constants.Constants;
 import grisu.model.GrisuRegistry;
 import grisu.model.GrisuRegistryManager;
 import grisu.model.dto.GridFile;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.log4j.Logger;
 
 public class GricliEnvironment {
 	
 	static final Logger myLogger = Logger.getLogger(GricliEnvironment.class.getName());
 
 	static class DateValidator extends Validator {
 		@Override
 		public String validate(String var, String value) throws GricliSetValueException {
 			int ivalue = 0;
 			try {
 				ivalue = Integer.parseInt(value);
 			} catch (NumberFormatException ex){
 				Pattern date = Pattern.compile("([0-9]+d)?([0-9]+h)?([0-9]+m)?");
 				Matcher m = date.matcher(value);
 				if (!m.matches()){
 					throw new GricliSetValueException(var,value,"not a valid date format");
 				}
 				String days = m.group(1);
 				String hours = m.group(2);
 				String minutes = m.group(3);
 
 				days = (days == null)?"0":days.replace("d","");
 				hours = (hours == null)?"0":hours.replace("h","");
 				minutes = (minutes == null)?"0":minutes.replace("m","");
 
 				try {
 					ivalue = (Integer.parseInt(days) * 1440) +
 							(Integer.parseInt(hours) * 60) +
 							Integer.parseInt(minutes);
 
 				} catch (NumberFormatException ex2){
 					throw new GricliSetValueException(var,value,"not valid date format");
 				}
 
 			}
 
 			if (ivalue < 0){
 				throw new GricliSetValueException(var,value, "must be positive");
 			}
 
 			return "" + ivalue;
 		}
 	}
 
 	static class DirValidator extends Validator {
 		@Override
 		public String validate(String var,String value) throws GricliSetValueException{
 			try {
 				//expand path for checking
 				String resultValue = StringUtils.replace(
 						value, "~", System.getProperty("user.home"));
 				File dir = new File(resultValue);
 
 				if (!dir.isAbsolute()) {
 					dir = new File(System.getProperty("user.dir"), value);
 					resultValue = dir.getAbsolutePath();
 				}
 				//check path
 				if (!dir.exists()) {
 					throw new GricliSetValueException(var,
 							dir.getCanonicalPath(), "directory does not exist");
 				}
 
 				System.setProperty("user.dir", resultValue);
 
 				//summarize path for display
 				resultValue = StringUtils.replace(
 						dir.getCanonicalPath(),
 						System.getProperty("user.home"), "~");
 				return resultValue;
 			} catch (IOException ex) {
 				throw new GricliSetValueException(var, value, ex.getMessage());
 			}
 		}
 	}
 
 	static class NullValidator extends Validator {
 
 		@Override
 		public String validate(String var,String value) throws GricliSetValueException{
 			if ("null".equals(value)){
 				return null;
 			}
 			return value;
 		}
 	}
 
 	static class PositiveIntValidator extends Validator {
 		@Override
 		public String validate(String var,String value) throws GricliSetValueException{
 			try {
 				int ivalue = Integer.parseInt(value);
 				if (ivalue < 0) {
 					throw new GricliSetValueException(var, value,
 							"cannot be negative");
 				}
 				return value;
 
 			} catch (NumberFormatException ex) {
 				throw new GricliSetValueException(var, value,
 						"must be a number");
 			}
 		}
 	}
 	static class SetValidator extends Validator {
 
 		private final String[] values;
 
 		public SetValidator(String[] values){
 			this.values = values;
 		}
 
 		@Override
 		public String validate(String var,String value) throws GricliSetValueException{
 			for (String pvalue: values){
 				if (pvalue.equals(value)){
 					return value;
 				}
 			}
 			throw new GricliSetValueException(var, value," has to be one of " + StringUtils.join(values, " "));
 		}
 	}
 	static class Validator {
 		public String validate(String var,String value) throws GricliSetValueException
 		{return value;}
 	}
 
 	private ServiceInterface si;
 	private GrisuRegistry reg;
 	private String siUrl;
 
 
 	private final HashMap<String, List<String>> globalLists = new HashMap<String, List<String>>();
 
 	private boolean quiet = false;
 
 
 	private final HashMap<String,String> environment = new HashMap<String,String>();
 
 	private static HashMap<String,Validator> validators = new HashMap<String,Validator>();
 
 	static {
 		validators.put("email", new Validator());
 		validators.put("email_on_start", new SetValidator(new String[] {"true","false"}));
 		validators.put("email_on_finish", new SetValidator(new String[] {"true","false"}));
 		validators.put("prompt", new Validator());
 		validators.put("dir", new DirValidator());
 		validators.put("group", new Validator());
 		validators.put("host", new Validator());
 		validators.put("gdir", new Validator());
 		validators.put("memory", new PositiveIntValidator());
 		validators.put("cpus", new PositiveIntValidator());
 		validators.put("walltime", new DateValidator());
		validators.put("jobtype", new SetValidator(new String[] {"single","mpi","smp"}));
 		validators.put("description", new Validator());
 		validators.put("version", new Validator());
 		validators.put("debug", new SetValidator(new String[] {"true","false"}));
 		validators.put("jobname", new Validator());
 		validators.put("application", new NullValidator());
 		validators.put("outputfile", new NullValidator());
 		validators.put("queue", new Validator());
 	}
 
 	public static Set<String> getVariables(){
 		HashSet<String> result = new HashSet<String>();
 		result.addAll(validators.keySet());
 
 		return result;
 	}
 
 	public GricliEnvironment() {
 
 		environment.put("queue", Constants.NO_SUBMISSION_LOCATION_INDICATOR_STRING);
 		environment.put("version", Constants.NO_VERSION_INDICATOR_STRING);
 		environment.put("walltime", "10");
 		environment.put("jobname", "gricli");
 		environment.put("cpus","1");
 		environment.put("jobtype","single");
 		environment.put("memory","2048");
 		environment.put("group","/nz");
 		environment.put("dir",System.getProperty("user.dir"));
 		environment.put("gdir","/");
 		environment.put("application", null);
 		environment.put("queue",null);
 		environment.put("debug","false");
 		environment.put("prompt","gricli> ");
 		environment.put("outputfile",null);
 		environment.put("description", "gricli job");
 
 		globalLists.put("files", new LinkedList<String>());
 	}
 
 	public void add(String globalList, String value)
 			throws GricliRuntimeException {
 		List<String> list = globalLists.get(globalList);
 		if (list == null){
 			throw new GricliRuntimeException("list " + globalList + " does not exist.");
 		}
 		list.add(value);
 
 	}
 
 	public void clear(String list) throws GricliRuntimeException {
 		try {
 			globalLists.get(list).clear();
 		}
 		catch (NullPointerException ex){
 			throw new GricliRuntimeException("list " + ((list!=null)?list:"null") + " does not exist");
 		}
 	}
 
 	public String get(String global) {
 		return environment.get(global);
 	}
 
 	public Set<String> getGlobalNames() {
 		return new HashSet<String>(validators.keySet());
 	}
 
 	public GrisuRegistry getGrisuRegistry() {
 		return this.reg;
 	}
 
 	public JobObject getJob() throws LoginRequiredException{
 
 		ServiceInterface si = getServiceInterface();
 		final JobObject job = new JobObject(si);
 		job.setJobname(get("jobname"));
 		String app = get("application");
 		if (app == null){
 			job.setApplication(Constants.GENERIC_APPLICATION_NAME);
 		}
 		else {
 			job.setApplication(app);
 			job.setApplicationVersion(get("version"));
 		}
 
 		job.setCpus(Integer.parseInt(get("cpus")));
 		job.setEmail_address(get("email"));
 
 		if ("true".equals(get("email_on_start"))){
 			job.setEmail_on_job_start(true);
 		}
 
 		if ("true".equals(get("email_on_finish"))){
 			job.setEmail_on_job_finish(true);
 		}
 
 		job.setDescription(get("description"));
 
 		job.setWalltimeInSeconds(Integer.parseInt(get("walltime")) * 60);
 		job.setMemory(Long.parseLong(get("memory")) * 1024 * 1024);
 		job.setSubmissionLocation(get("queue"));
 
 		boolean isMpi = "mpi".equals(get("jobtype"));
 		job.setForce_mpi(isMpi);
		boolean isSmp = "smp".equals(get("jobtype"));
		if (isSmp){
			job.setForce_single(true);
 			job.setHostCount(1);
 		}
 
 		// attach input files
 		List<String> files = getList("files");;
 		for (String file : files) {
 			System.out.println("adding input file "
 					+ new GridFile(file).getUrl());
 			job.addInputFileUrl(new GridFile(file).getUrl());
 		}
 
 		return job;
 	}
 
 	public List<String> getList(String globalList) {
 		return globalLists.get(globalList);
 	}
 
 	public ServiceInterface getServiceInterface() throws LoginRequiredException {
 		if (si == null) {
 			throw new LoginRequiredException();
 		}
 		return si;
 	}
 
 	public String getServiceInterfaceUrl() {
 		return siUrl;
 	}
 
 	public void printError(String message){
 		myLogger.info("gricli-audit-error username=" + System.getProperty("user.name") + "command=" + message );
 		System.err.println(message);
 	}
 
 	public void printMessage(String message){
 		if (!quiet){
 			System.out.println(message);
 		}
 
 		PrintStream out = null;
 
 		try {
 			String output = get("outputfile");
 			if (output != null){
 				File outputfile = new File(output);
 				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputfile, true)));
 				out.println(message);
 			}
 		}
 		catch (IOException ex){
 			printError(ex.getMessage());
 		}
 		finally {
 			if (out != null){
 				out.close();
 			}
 		}
 	}
 
 	public void put(String global, String value) throws GricliRuntimeException {
 		Validator v = validators.get(global);
 		if (v != null) {
 			String result = v.validate(global, value);
 			environment.put(global, result);
 		} else {
 			throw new GricliRuntimeException(global
 					+ " global variable does not exist");
 		}
 	}
 
 
 	public void quiet(boolean q){
 		this.quiet = q;
 	}
 	public void setServiceInterface(ServiceInterface si) {
 		this.si = si;
 		this.reg = GrisuRegistryManager.getDefault(si);
 	}
 
 	public void setServiceInterfaceUrl(String siUrl) {
 		this.siUrl = siUrl;
 	}
 
 }
