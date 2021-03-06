 /*******************************************************************************
  * Copyright (c) 2003, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.runtime.jobs;
 
 import org.eclipse.core.internal.jobs.InternalJob;
 import org.eclipse.core.runtime.*;
 
 /**
  * Jobs are units of runnable work that can be scheduled to be run with the job
  * manager.  Once a job has completed, it can be scheduled to run again (jobs are
  * reusable).
  * <p>
  * Jobs have a state that indicates what they are currently doing.  When constructed,
  * jobs start with a state value of <code>NONE</code>.  When a job is scheduled
  * to be run, it moves into the <code>WAITING</code> state.  When a job starts
  * running, it moves into the <code>RUNNING</code> state.  When execution finishes
  * (either normally or through cancelation), the state changes back to 
  * <code>NONE</code>.  
  * </p><p>
  * A job can also be in the <code>SLEEPING</code> state.  This happens if a user
  * calls Job.sleep() on a waiting job, or if a job is scheduled to run after a specified
  * delay.  Only jobs in the <code>WAITING</code> state can be put to sleep.  
  * Sleeping jobs can be woken at any time using Job.wakeUp(), which will put the
  * job back into the <code>WAITING</code> state.
  * </p><p>
  * Jobs can be assigned a priority that is used as a hint about how the job should
  * be scheduled.  There is no guarantee that jobs of one priority will be run before
  * all jobs of lower priority.  The javadoc for the various priority constants provide
  * more detail about what each priority means.  By default, jobs start in the 
  * <code>LONG</code> priority class.
  * 
  * @see IJobManager
  * @since 3.0
  */
 public abstract class Job extends InternalJob implements IAdaptable {
 
 	/**
 	 * Job status return value that is used to indicate asynchronous job completion.
 	 * @see Job#run(IProgressMonitor)
 	 * @see Job#done(IStatus)
 	 */
 	public static final IStatus ASYNC_FINISH = new Status(IStatus.OK, Platform.PI_RUNTIME, 1, "", null);//$NON-NLS-1$
 
 	/* Job priorities */
 	/** 
 	 * Job priority constant (value 10) for interactive jobs.
 	 * Interactive jobs generally have priority over all other jobs.
 	 * Interactive jobs should be either fast running or very low on CPU
 	 * usage to avoid blocking other interactive jobs from running.
 	 * 
 	 * @see #getPriority()
 	 * @see #setPriority(int)
 	 * @see #run(IProgressMonitor)
 	 */
 	public static final int INTERACTIVE = 10;
 	/** 
 	 * Job priority constant (value 20) for short background jobs.
 	 * Short background jobs are jobs that typically complete within a second,
 	 * but may take longer in some cases.  Short jobs are given priority
 	 * over all other jobs except interactive jobs.
 	 * 
 	 * @see #getPriority()
 	 * @see #setPriority(int)
 	 * @see #run(IProgressMonitor)
 	 */
 	public static final int SHORT = 20;
 	/** 
 	 * Job priority constant (value 30) for long-running background jobs.
 	 * 
 	 * @see #getPriority()
 	 * @see #setPriority(int)
 	 * @see #run(IProgressMonitor)
 	 */
 	public static final int LONG = 30;
 	/** 
 	 * Job priority constant (value 40) for build jobs.  Build jobs are
 	 * generally run after all other background jobs complete.
 	 * 
 	 * @see #getPriority()
 	 * @see #setPriority(int)
 	 * @see #run(IProgressMonitor)
 	 */
 	public static final int BUILD = 40;
 
 	/** 
 	 * Job priority constant (value 50) for decoration jobs.
 	 * Decoration jobs have lowest priority.  Decoration jobs generally
 	 * compute extra information that the user may be interested in seeing
 	 * but is generally not waiting for.
 	 * 
 	 * @see #getPriority()
 	 * @see #setPriority(int)
 	 * @see #run(IProgressMonitor)
 	 */
 	public static final int DECORATE = 50;
 	/** 
 	 * Job state code (value 0) indicating that a job is not 
 	 * currently sleeping, waiting, or running (i.e., the job manager doesn't know 
 	 * anything about the job). 
 	 * 
 	 * @see #getState()
 	 */
 	public static final int NONE = 0;
 	/** 
 	 * Job state code (value 1) indicating that a job is sleeping.
 	 * 
 	 * @see #run(IProgressMonitor)
 	 * @see #getState()
 	 */
 	public static final int SLEEPING = 0x01;
 	/** 
 	 * Job state code (value 2) indicating that a job is waiting to be run.
 	 * 
 	 * @see #getState()
 	 */
 	public static final int WAITING = 0x02;
 	/** 
 	 * Job state code (value 4) indicating that a job is currently running
 	 * 
 	 * @see #getState()
 	 */
 	public static final int RUNNING = 0x04;
 
 	/**
 	 * Creates a new job with the specified name.  The job name is a human-readable
 	 * value that is displayed to users.  The name does not need to be unique, but it
 	 * must not be <code>null</code>.
 	 * 
 	 * @param name the name of the job.
 	 */
 	public Job(String name) {
 		super(name);
 	}
 
 	/**
 	 * Registers a job listener with this job
 	 * Has no effect if an identical listener is already registered.
 	 * 
 	 * @param listener the listener to be added.
 	 */
 	public final void addJobChangeListener(IJobChangeListener listener) {
 		super.addJobChangeListener(listener);
 	}
 
 	/**
 	 * Returns whether this job belongs to the given family.  Job families are
 	 * represented as objects that are not interpreted or specified in any way
 	 * by the job manager.  Thus, a job can choose to belong to any number of
 	 * families.
 	 * <p>
 	 * Clients may override this method.  This default implementation always returns
 	 * <code>false</code>.  Overriding implementations must return <code>false</code>
 	 * for families they do not recognize.
 	 * </p>
 	 * 
 	 * @param family the job family identifier
 	 * @return <code>true</code> if this job belongs to the given family, and 
 	 * <code>false</code> otherwise.
 	 */
 	public boolean belongsTo(Object family) {
 		return false;
 	}
 
 	/**
 	 * Stops the job.  If the job is currently waiting,
 	 * it will be removed from the queue.  If the job is sleeping,
 	 * it will be discarded without having a chance to resume and its sleeping state
 	 * will be cleared.  If the job is currently executing, it will be asked to
 	 * stop but there is no guarantee that it will do so.
 	 * 
 	 * @return <code>false</code> if the job is currently running (and thus may not
 	 * respond to cancelation), and <code>true</code> in all other cases.
 	 */
 	public final boolean cancel() {
 		return super.cancel();
 	}
 
 	/**
 	 * Jobs that complete their execution asynchronously must indicate when they
 	 * are finished by calling this method.  This method must not be called by
 	 * a job that has not indicated that it is executing asynchronously.
 	 * <p>
 	 * This method must not be called from within the scope of a job's <code>run</code>
 	 * method.  Jobs should normally indicate completion by returning an appropriate
 	 * status from the <code>run</code> method.  Jobs that return a status of
 	 * <code>ASYNC_FINISH</code> from their run method must later call 
 	 * <code>done</code> to indicate completion.
 	 * 
 	 * @param result a status object indicating the result of the job's execution.
 	 * @see #ASYNC_FINISH
 	 * @see #run(IProgressMonitor)
 	 */
 	public final void done(IStatus result) {
 		super.done(result);
 	}
 
 	/**
 	 * Returns the human readable name of this job.  The name is never 
 	 * <code>null</code>.
 	 * 
 	 * @return the name of this job
 	 */
 	public final String getName() {
 		return super.getName();
 	}
 
 	/**
 	 * Returns the priority of this job.  The priority is used as a hint when the job
 	 * is scheduled to be run.
 	 * 
 	 * @return the priority of the job.  One of INTERACTIVE, SHORT, LONG, BUILD, 
 	 * 	or DECORATE.
 	 */
 	public final int getPriority() {
 		return super.getPriority();
 	}
 
 	/**
 	 * Returns the value of the property of this job identified by the given key, 
 	 * or <code>null</code> if this job has no such property.
 	 *
 	 * @param key the name of the property
 	 * @return the value of the property, 
 	 *     or <code>null</code> if this job has no such property
 	 * @see #setProperty(QualifiedName, Object)
 	 */
 	public final Object getProperty(QualifiedName key) {
 		return super.getProperty(key);
 	}
 
 	/**
 	 * Returns the result of this job's last run.
 	 * 
 	 * @return the result of this job's last run, or <code>null</code> if this
 	 * job has never finished running.
 	 */
 	public final IStatus getResult() {
 		return super.getResult();
 	}
 
 	/**
 	 * Returns the scheduling rule for this job.  Returns <code>null</code> if this job has no
 	 * scheduling rule.
 	 * 
 	 * @return the scheduling rule for this job, or <code>null</code>.
 	 * @see ISchedulingRule
 	 * @see #setRule(ISchedulingRule)
 	 */
 	public final ISchedulingRule getRule() {
 		return super.getRule();
 	}
 
 	/**
 	 * Returns the state of the job. Result will be one of:
 	 * <ul>
 	 * <li><code>Job.RUNNING</code> - if the job is currently running.</li>
 	 * <li><code>Job.WAITING</code> - if the job is waiting to be run.</li>
 	 * <li><code>Job.SLEEPING</code> - if the job is sleeping.</li>
 	 * <li><code>Job.NONE</code> - in all other cases.</li>
 	 * </ul>
 	 * <p>
 	 * Note that job state is inherently volatile, and in most cases clients 
 	 * cannot rely on the result of this method being valid by the time the 
 	 * result is obtained.  For example, if <tt>getState</tt> returns 
 	 * <tt>RUNNING</tt>,  the job may have actually completed by the 
 	 * time the <tt>getState</tt> method returns.  All clients can infer from 
 	 * invoking this method is that  the job was recently in the returned state.
 	 * 
 	 * @return the job state
 	 */
 	public final int getState() {
 		return super.getState();
 	}
 
 	/**
 	 * Returns the thread that this job is currently running in.
 	 * 
 	 * @return the thread this job is running in, or <code>null</code>
 	 * if this job is not running or the thread is unknown.
 	 */
 	public final Thread getThread() {
 		return super.getThread();
 	}
 
 	/**
 	 * Returns whether this job is blocking another non-system job from 
 	 * starting due to a conflicting scheduling rule.  Returns <code>false</code> 
 	 * if this job is not running, or is not blocking any other job.
 	 * 
 	 * @return <code>true</code> if this job is blocking a waiting non-system
 	 * job, and <code>false</code> otherwise.
 	 * @see #getRule()
 	 * @see #isSystem()
 	 */
 	public final boolean isBlocking() {
 		return super.isBlocking();
 	}
 
 	/**
 	 * Returns whether this job is a system job.  System jobs are typically not 
 	 * revealed to users in any UI presentation of jobs.  Other than their UI presentation,
 	 * system jobs act exactly like other jobs.  If this value is not explicitly set, jobs
 	 * are treated as non-system jobs.  The default value is <code>false</code>.
 	 * 
 	 * @return <code>true</code> if this job is a system job, and
 	 * <code>false</code> otherwise.
 	 * @see #setSystem(boolean)
 	 */
 	public final boolean isSystem() {
 		return super.isSystem();
 	}
 
 	/**
 	 * Returns whether this job has been directly initiated by a UI end user. 
 	 * These jobs may be presented differently in the UI.  The default value
 	 * is <code>false</code>.
 	 * 
 	 * @return <code>true</code> if this job is a user-initiated job, and
 	 * <code>false</code> otherwise.
 	 * @see #setUser(boolean)
 	 */
 	public final boolean isUser() {
 		return super.isUser();
 	}
 
 	/**
 	 * Waits until this job is finished.  This method will block the calling thread until the 
 	 * job has finished executing, or until this thread has been interrupted.  If the job 
 	 * has not been scheduled, this method returns immediately.
 	 * <p>
 	 * If this method is called on a job that reschedules itself from within the 
 	 * <tt>run</tt> method, the join will return at the end of the first execution.
 	 * In other words, join will return the first time this job exits the
 	 * <tt>RUNNING</tt> state, or as soon as this job enters the <tt>NONE</tt> state.
 	 * </p><p>
 	 * Note that there is a deadlock risk when using join.  If the calling thread owns
 	 * a lock or object monitor that the joined thread is waiting for, deadlock 
 	 * will occur.
 	 * </p>
 	 * 
 	 * @exception InterruptedException if this thread is interrupted while waiting
 	 * @see ILock
 	 */
 	public final void join() throws InterruptedException {
 		super.join();
 	}
 
 	/**
 	 * Removes a job listener from this job.
 	 * Has no effect if an identical listener is not already registered.
 	 * 
 	 * @param listener the listener to be removed
 	 */
 	public final void removeJobChangeListener(IJobChangeListener listener) {
 		super.removeJobChangeListener(listener);
 	}
 
 	/**
 	 * Executes this job.  Returns the result of the execution.
 	 * <p>
 	 * The provided monitor can be used to report progress and respond to 
 	 * cancellation.  If the progress monitor has been canceled, the job
 	 * should finish its execution at the earliest convenience. 
 	 * <p>
 	 * This method must not be called directly by clients.  Clients should call
 	 * <code>schedule</code>, which will in turn cause this method to be called.
 	 * <p>
 	 * Jobs can optionally finish their execution asynchronously (in another thread) by 
 	 * returning a result status of <code>Job.ASYNC_FINISH</code>.  Jobs that finish
 	 * asynchronously <b>must</b> specify the execution thread by calling
 	 * <code>setThread</code>, and must indicate when they are finished by calling
 	 * the method <code>done</code>.
 	 * 
 	 * @param monitor the monitor to be used for reporting progress and
 	 * responding to cancelation. The monitor is never <code>null</code>
 	 * @return resulting status of the run. The result must not be <code>null</code>
 	 * @see #ASYNC_FINISH
 	 * @see #done(IStatus)
 	 */
 	protected abstract IStatus run(IProgressMonitor monitor);
 
 	/**
 	 * Schedules this job to be run.  The job is added to a queue of waiting
 	 * jobs, and will be run when it arrives at the beginning of the queue.
 	 * <p>
 	 * This is a convenience method, fully equivalent to 
 	 * <code>schedule(0L)</code>.
 	 * </p>
 	 */
 	public final void schedule() {
 		super.schedule(0L);
 	}
 
 	/**
 	 * Schedules this job to be run after a specified delay.  After the specified delay,
 	 * the job is added to a queue of waiting jobs, and will be run when it arrives at the 
 	 * beginning of the queue.	
 	 * <p>
 	 * If this job is currently running, it will be rescheduled with the specified
 	 * delay as soon as it finishes.  If this method is called multiple times
 	 * while the job is running, the job will still only be rescheduled once,
 	 * with the most recent delay value that was provided.
	 * <p>
	 * Scheduling a job that is waiting or sleeping has no effect
 	 * 
 	 * @param delay a time delay in milliseconds before the job should run
 	 */
 	public final void schedule(long delay) {
 		super.schedule(delay);
 	}
 
 	/**
 	 * Changes the name of this job.  The job name is a human-readable
 	 * value that is displayed to users.  The name does not need to be unique, but it
 	 * must not be <code>null</code>.
 	 * 
 	 * @param name the name of the job.
 	 */
 	public final void setName(String name) {
 		super.setName(name);
 	}
 
 	/**
 	 * Sets the priority of the job.  This will not affect the execution of
 	 * a running job, but it will affect how the job is scheduled while
 	 * it is waiting to be run.
 	 * 
 	 * @param priority the new job priority.  One of
 	 * INTERACTIVE, SHORT, LONG, BUILD, or DECORATE.
 	 */
 	public final void setPriority(int priority) {
 		super.setPriority(priority);
 	}
 
 	/**
 	 * Associates this job with a progress group.  Progress feedback 
 	 * on this job's next execution will be displayed together with other
 	 * jobs in that group. The provided monitor must be a monitor 
 	 * created by the method <tt>IJobManager.createProgressGroup</tt>
 	 * and must have at least <code>ticks</code> units of available work.
 	 * <p>
 	 * The progress group must be set before the job is scheduled.
 	 * The group will be used only for a single invocation of the job's 
 	 * <tt>run</tt> method, after which any association of this job to the 
 	 * group will be lost.
 	 * 
 	 * @see IJobManager#createProgressGroup()
 	 * @param group The progress group to use for this job
 	 * @param ticks the number of work ticks allocated from the
 	 *    parent monitor, or IProgressMonitor.UNKNOWN
 	 */
 	public final void setProgressGroup(IProgressMonitor group, int ticks) {
 		super.setProgressGroup(group, ticks);
 	}
 
 	/**
 	 * Sets the value of the property of this job identified
 	 * by the given key. If the supplied value is <code>null</code>,
 	 * the property is removed from this resource. 
 	 * <p>
 	 * Properties are intended to be used as a caching mechanism
 	 * by ISV plug-ins. They allow key-object associations to be stored with
 	 * a job instance.  These key-value associations are maintained in 
 	 * memory (at all times), and the information is never discarded automatically.
 	 * </p><p>
 	 * The qualifier part of the property name must be the unique identifier
 	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
 	 * </p>
 	 *
 	 * @param key the qualified name of the property
 	 * @param value the value of the property, 
 	 *     or <code>null</code> if the property is to be removed
 	 * @see #getProperty(QualifiedName)
 	 */
 	public void setProperty(QualifiedName key, Object value) {
 		super.setProperty(key, value);
 	}
 
 	/**
 	 * Sets the scheduling rule to be used when scheduling this job.  This method
 	 * must be called before the job is scheduled.
 	 * 
 	 * @param rule the new scheduling rule, or <code>null</code> if the job
 	 * should have no scheduling rule
 	 * @see #getRule()
 	 */
 	public final void setRule(ISchedulingRule rule) {
 		super.setRule(rule);
 	}
 
 	/**
 	 * Sets whether or not this job is a system job.  System jobs are typically not 
 	 * revealed to users in any UI presentation of jobs.  Other than their UI presentation,
 	 * system jobs act exactly like other jobs.  If this value is not explicitly set, jobs
 	 * are treated as non-system jobs. This method must be called before the job 
 	 * is scheduled.
 	 * 
 	 * @param value <code>true</code> if this job should be a system job, and
 	 * <code>false</code> otherwise.
 	 * @see #isSystem()
 	 */
 	public final void setSystem(boolean value) {
 		super.setSystem(value);
 	}
 
 	/**
 	 * Sets whether or not this job has been directly initiated by a UI end user. 
 	 * These jobs may be presented differently in the UI. This method must be 
 	 * called before the job is scheduled.
 	 * 
 	 * @param value <code>true</code> if this job is a user-initiated job, and
 	 * <code>false</code> otherwise.
 	 * @see #isUser()
 	 */
 	public final void setUser(boolean value) {
 		super.setUser(value);
 	}
 
 	/**
 	 * Sets the thread that this job is currently running in, or <code>null</code>
 	 * if this job is not running or the thread is unknown.
 	 * <p>
 	 * Jobs that use the <code>Job.ASYNC_FINISH</code> return code should tell 
 	 * the job what thread it is running in.  This is used to prevent deadlocks.
 	 * 
 	 * @param thread the thread that this job is running in.
 	 * 
 	 * @see #ASYNC_FINISH
 	 * @see #run(IProgressMonitor)
 	 */
 	public final void setThread(Thread thread) {
 		super.setThread(thread);
 	}
 
 	/**
 	 * Returns whether this job should be run.
 	 * If <code>false</code> is returned, this job will be discarded by the job manager
 	 * without running.
 	 * <p>
 	 * This method is called immediately prior to calling the job's
 	 * run method, so it can be used for last minute pre-condition checking before
 	 * a job is run. This method must not attempt to schedule or change the
 	 * state of any other job.
 	 * </p><p>
 	 * Clients may override this method.  This default implementation always returns
 	 * <code>true</code>.
 	 * </p>
 	 * 
 	 * @return <code>true</code> if this job should be run 
 	 *   and <code>false</code> otherwise
 	 */
 	public boolean shouldRun() {
 		return true;
 	}
 
 	/**
 	 * Returns whether this job should be scheduled.
 	 * If <code>false</code> is returned, this job will be discarded by the job manager
 	 * without being added to the queue.
 	 * <p>
 	 * This method is called immediately prior to adding the job to the waiting job
 	 * queue.,so it can be used for last minute pre-condition checking before
 	 * a job is scheduled.
 	 * </p><p>
 	 * Clients may override this method.  This default implementation always returns
 	 * <code>true</code>.
 	 * </p>
 	 * 
 	 * @return <code>true</code> if the job manager should schedule this job
 	 *   and <code>false</code> otherwise
 	 */
 	public boolean shouldSchedule() {
 		return true;
 	}
 
 	/**
 	 * Requests that this job be suspended.  If the job is currently waiting to be run, it 
 	 * will be removed from the queue move into the <code>SLEEPING</code> state.
 	 * The job will remain asleep until either resumed or canceled.  If this job is not
 	 * currently waiting to be run, this method has no effect.
 	 * <p>
 	 * Sleeping jobs can be resumed using <code>wakeUp</code>.
 	 * 
 	 * @return <code>false</code> if the job is currently running (and thus cannot
 	 * be put to sleep), and <code>true</code> in all other cases
 	 * @see #wakeUp()
 	 */
 	public final boolean sleep() {
 		return super.sleep();
 	}
 
 	/**
 	 * Puts this job immediately into the <code>WAITING</code> state so that it is 
 	 * eligible for immediate execution. If this job is not currently sleeping, 
 	 * the request is ignored.
 	 * <p>
 	 * This is a convenience method, fully equivalent to 
 	 * <code>wakeUp(0L)</code>.
 	 * </p>
 	 * @see #sleep()
 	 */
 	public final void wakeUp() {
 		super.wakeUp(0L);
 	}
 
 	/**
 	 * Puts this job back into the <code>WAITING</code> state after
 	 * the specified delay. This is equivalent to canceling the sleeping job and
 	 * rescheduling with the given delay.  If this job is not currently sleeping, 
 	 * the request  is ignored.
 	 * 
 	 * @param delay the number of milliseconds to delay
 	 * @see #sleep()
 	 */
 	public final void wakeUp(long delay) {
 		super.wakeUp(delay);
 	}
 }
