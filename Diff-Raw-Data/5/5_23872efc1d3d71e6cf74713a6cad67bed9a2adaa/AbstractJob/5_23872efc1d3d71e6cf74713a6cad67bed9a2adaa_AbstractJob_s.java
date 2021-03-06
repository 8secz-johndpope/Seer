 /*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.xwiki.job.internal;
 
 import java.util.Date;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
 
 import javax.inject.Inject;
 
 import org.slf4j.Logger;
 import org.xwiki.component.annotation.InstantiationStrategy;
 import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
 import org.xwiki.component.manager.ComponentManager;
 import org.xwiki.job.Job;
 import org.xwiki.job.JobContext;
 import org.xwiki.job.Request;
 import org.xwiki.job.event.JobFinishedEvent;
 import org.xwiki.job.event.JobStartedEvent;
 import org.xwiki.job.event.status.JobStatus;
 import org.xwiki.job.event.status.JobStatus.State;
 import org.xwiki.job.event.status.PopLevelProgressEvent;
 import org.xwiki.job.event.status.PushLevelProgressEvent;
 import org.xwiki.job.event.status.StepProgressEvent;
 import org.xwiki.logging.LoggerManager;
 import org.xwiki.observation.ObservationManager;
 
 /**
  * Base class for {@link Job} implementations.
  * 
  * @param <R> the request type associated to the job
  * @version $Id$
 * @since 4.0M1
  */
 @InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractJob<R extends Request, S extends AbstractJobStatus<R>> implements Job
 {
     /**
      * Component manager.
      */
     @Inject
     protected ComponentManager componentManager;
 
     /**
      * Used to send extensions installation and upgrade related events.
      */
     @Inject
     protected ObservationManager observationManager;
 
     /**
      * Used to isolate job related log.
      */
     @Inject
     protected LoggerManager loggerManager;
 
     /**
      * Used to store the results of the jobs execution.
      */
     @Inject
     protected JobStatusStorage storage;
 
     /**
      * The logger to log.
      */
     @Inject
     protected Logger logger;
 
     /**
      * Used to set the current context.
      */
     @Inject
     protected JobContext jobContext;
 
     /**
      * The job request.
      */
     protected R request;
 
     /**
      * @see #getStatus()
      */
     protected S status;
 
     /**
      * Main lock guarding all access.
      */
     private final ReentrantLock lock = new ReentrantLock();
 
     /**
      * Condition for waiting takes.
      */
     private final Condition finishedCondition = lock.newCondition();
 
     @Override
     public R getRequest()
     {
         return this.request;
     }
 
     @Override
     public S getStatus()
     {
         return this.status;
     }
 
     @Override
     public void start(Request request)
     {
         this.request = castRequest(request);
         this.status = createNewStatus(this.request);
 
         jobStarting();
 
         Throwable error = null;
         try {
             start();
         } catch (Throwable t) {
             this.logger.error("Exception thrown during job execution", t);
             error = t;
         } finally {
             jobFinished(error);
         }
     }
 
     /**
      * Called when the job is starting.
      */
     protected void jobStarting()
     {
         this.jobContext.pushCurrentJob(this);
 
         this.observationManager.notify(new JobStartedEvent(getRequest().getId(), getType(), request), this);
 
         this.status.setStartDate(new Date());
         this.status.setState(JobStatus.State.RUNNING);
 
         this.status.startListening();
 
         if (getStatus().getRequest().getId() != null) {
             this.logger.info("Starting job of type [{}] with identifier [{}]", getType(), getStatus().getRequest()
                 .getId());
         } else {
             this.logger.info("Starting job of type [{}]", getType());
         }
     }
 
     /**
      * Called when the job is done.
      * 
      * @param exception the exception throw during execution of the job
      */
     protected void jobFinished(Throwable exception)
     {
         this.lock.lock();
 
         try {
             // Give a chance to any listener to do custom action associated to the job
             // TODO: use a JobFinishingEvent instead ?
             this.observationManager.notify(new JobFinishedEvent(getRequest().getId(), getType(), this.request), this,
                 exception);
 
             // Indicate when the job ended
             this.status.setEndDate(new Date());
 
             if (getStatus().getRequest().getId() != null) {
                 this.logger.info("Finished job of type [{}] with identifier [{}]", getType(), getStatus().getRequest()
                     .getId());
             } else {
                 this.logger.info("Finished job of type [{}]", getType());
             }
 
             // Stop updating job status (progress, log, etc.)
             this.status.stopListening();
 
             // Update job state
             this.status.setState(JobStatus.State.FINISHED);
 
             // Release threads waiting for job being done
             this.finishedCondition.signalAll();
 
             // Remove the job from the current jobs context
             this.jobContext.popCurrentJob();
 
             // Store the job status
             try {
                 if (this.request.getId() != null) {
                     this.storage.store(this.status);
                 }
             } catch (Throwable t) {
                 this.logger.warn("Failed to store job status [{}]", this.status, t);
             }
         } finally {
             this.lock.unlock();
         }
     }
 
     /**
      * Should be overridden if R is not Request.
      * 
      * @param request the request
      * @return the request in the proper extended type
      */
     @SuppressWarnings("unchecked")
     protected R castRequest(Request request)
     {
         return (R) request;
     }
 
     /**
      * @param request contains information related to the job to execute
      * @return the status of the job
      */
     protected S createNewStatus(R request)
     {
         return (S) new DefaultJobStatus<R>(request, this.observationManager, this.loggerManager,
             this.jobContext.getCurrentJob() != null);
     }
 
     /**
      * Push new progression level.
      * 
      * @param steps number of steps in this new level
      */
     protected void notifyPushLevelProgress(int steps)
     {
         this.observationManager.notify(new PushLevelProgressEvent(steps), this);
     }
 
     /**
      * Next step.
      */
     protected void notifyStepPropress()
     {
         this.observationManager.notify(new StepProgressEvent(), this);
     }
 
     /**
      * Pop progression level.
      */
     protected void notifyPopLevelProgress()
     {
         this.observationManager.notify(new PopLevelProgressEvent(), this);
     }
 
     /**
      * Should be implemented by {@link Job} implementations.
      * 
      * @throws Exception errors during job execution
      */
     protected abstract void start() throws Exception;
 
     @Override
     public void join() throws InterruptedException
     {
         this.lock.lockInterruptibly();
 
         try {
             if (getStatus() == null || getStatus().getState() != State.FINISHED) {
                 this.finishedCondition.await();
             }
         } finally {
             this.lock.unlock();
         }
     }
 
     @Override
     public boolean join(long time, TimeUnit unit) throws InterruptedException
     {
         this.lock.lockInterruptibly();
 
         try {
             if (getStatus().getState() != State.FINISHED) {
                 return this.finishedCondition.await(time, unit);
             }
         } finally {
             this.lock.unlock();
         }
 
         return true;
     }
 }
