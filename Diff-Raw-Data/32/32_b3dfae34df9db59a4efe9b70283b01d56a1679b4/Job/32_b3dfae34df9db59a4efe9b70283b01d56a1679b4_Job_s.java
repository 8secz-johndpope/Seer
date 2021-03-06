 package org.daisy.pipeline.job;
 
 import java.util.Properties;
 
 import org.daisy.common.xproc.XProcEngine;
 import org.daisy.common.xproc.XProcPipeline;
 import org.daisy.common.xproc.XProcResult;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.base.Function;
 import com.google.common.eventbus.EventBus;
 
 // TODO: Auto-generated Javadoc
 //TODO check thread safety
 /**
  * The Class Job defines the execution unit.
  */
 public class Job {
 
 	private static final Logger logger = LoggerFactory
 			.getLogger(Job.class);
 
 	public static class  JobBuilder{
 		protected JobContext ctxt;
 		protected EventBus bus;
 		private Job job;
 		public JobBuilder withContext(JobContext ctxt){
 			this.ctxt=ctxt;
 			return this;
 		}
 
 		public JobBuilder withEventBus(EventBus bus){
 			this.bus=bus;
 			return this;
 		}
 		//available for subclasses to override
 		protected Job initJob(){
 			Job job = new Job(this.ctxt,this.bus);
 			return job;
 		}
 		public final Job build(){
 			this.job=this.initJob();
 			this.job.changeStatus(Status.IDLE);
 			return job;
 		}
 		
 		/**
 		 * Builds the job allowing a preprocessing before the status is 
 		 * broadcasted
 		 * @param initialiser
 		 * @return
 		 */
 		public final Job build(Function<Job,Job> initialiser){
 			this.job= initialiser.apply(this.initJob());
 			this.job.changeStatus(Status.IDLE);
 			return job;
 		}
 		public final void initialise(){
 		}
 	}
 
 
 	/**
 	 * The Enum Status.
 	 */
 
 
 	public static enum Status {
 
 		/** The IDLE. */
 		IDLE,
 		/** The RUNNING. */
 		RUNNING,
 		/** The DONE. */
 		DONE,
 		ERROR
 	}
 
 
 	/** The status. */
 	private volatile Status status = Status.IDLE;
 	protected JobContext ctxt;
 	private EventBus eventBus;
 
 	protected Job(JobContext ctxt,EventBus eventBus) {
 		this.ctxt=ctxt;
 		this.eventBus=eventBus;
 	}
 
 	/**
 	 * Gets the id.
 	 *
 	 * @return the id
 	 */
 	public JobId getId() {
 		return this.ctxt.getId();
 	}
 
 	/**
 	 * Gets the status.
 	 *
 	 * @return the status
 	 */
 	public Status getStatus() {
 		synchronized(this.status){
 			return status;
 		}
 	}
 
 	protected void setStatus(Status status){
 		synchronized(this.status){
 			this.status=status;
 		}
 	}
 
 	/**
 	 * @param eventBus the eventBus to set
 	 */
 	public void setEventBus(EventBus eventBus) {
 		this.eventBus = eventBus;
 	}
 
 	/**
 	 * Gets the ctxt for this instance.
 	 *
 	 * @return The ctxt.
 	 */
 	public JobContext getContext() {
 		return this.ctxt;
 	}
 
 	/**
 	 * Gets the ctxt for this instance.
 	 *
 	 * @return The ctxt.
 	 */
 	protected void setContext(JobContext ctxt) {
 		this.ctxt=ctxt;
 	}
 
 	/**
 	 * Gets the x proc output.
 	 *
 	 * @return the x proc output
 	 */
 	final XProcResult getXProcOutput() {
 		return null;
 	}
 
 	private synchronized final void changeStatus(Status to){
 		this.status=to;
 		if (this.eventBus!=null)
 			this.eventBus.post(new StatusMessage.Builder().withJobId(this.getId()).withStatus(this.status).build());
 		else
 			logger.warn("I couldnt broadcast my change of status because"+((this.ctxt==null)? " the context ": " event bus ") + "is null");
 		this.onStatusChanged(to);
 	}
 
 	/**
 	 * Runs the job using the XProcEngine as script loader.
 	 *
 	 * @param engine the engine
 	 */
 	public synchronized final void run(XProcEngine engine) {
 		changeStatus(Status.RUNNING);
 		XProcPipeline pipeline = null;
 		try{
 			pipeline = engine.load(this.ctxt.getScript().getURI());
		}catch (Exception e){
			logger.error("Error while loading the script:"+this.ctxt.getScript().getName());
			throw new RuntimeException(e);
		}
		try{
 			Properties props=new Properties();
 			props.setProperty("JOB_ID", this.ctxt.getId().toString());
 			XProcResult results = pipeline.run(this.ctxt.getInputs(),this.ctxt.getMonitor(),props);
 			this.ctxt.writeResult(results);
 			changeStatus( Status.DONE );
 		}catch(Exception e){
 			logger.error("job finished with error state",e);
 			changeStatus( Status.ERROR);
 		}
 
 	}
 
 	protected void onStatusChanged(Status newStatus){
 		//for subclasses
 	}
 
 }
