 package main.taskexecutor.core;
 
 import android.os.*;
 import android.util.*;
 import java.util.*;
 import java.util.concurrent.*;
 import main.taskexecutor.callbacks.*;
 import main.taskexecutor.classes.*;
 
 import main.taskexecutor.classes.Log;
 
 /**
  * @author Noah Seidman
  */
 public class TaskExecutor{
             Handler                   mHandler                   = new Handler(Looper.getMainLooper());
             TaskCompletedCallback     mTaskCompletedCallback     = null;
             ConditionVariable         mLock                      = new ConditionVariable(true);
	    int                       mInterruptTasksAfter       = -1;
 	    Vector<Pair>              mPendingCompletedTasks     = new Vector<Pair>();
     private boolean                   mPause                     = false;  
     private Vector<Task>              mQueue                     = new Vector<Task>();
     private ServiceExecutorCallback   mServiceHelperCallback     = null;
     private ThreadPoolExecutor        mTaskExecutor              = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
 
     public TaskExecutor(ServiceExecutorCallback serviceHelperCallback){
 	mServiceHelperCallback = serviceHelperCallback;
     }
 
     /**
      * @param pool
      * By default Tasks are executed serially. You can execute Tasks
      * concurrently if you'd like, but please consider the implications on
      * finessing your Tasks to accommodate configurationChanges if your
      * implementation is configured as such.
      */
     public void poolThreads(boolean pool){
 	if (pool){
 	    mTaskExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
 	} else{
 	    mTaskExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
 	}
     }
 
     /**
      * @return Return if the queue's execution is currently paused.
      */
     public boolean isPaused(){
 	return mPause;
     }
     
     /**
      * @param task
      * Provide a Task to be added to the queue pending execution.
      * @param taskCompletedCallback
      * Provide an interface to callback when the Task has completed execution,
      * you may supply null, but if you have allowFiness() enabled a callback
      * will assigned at that time.
      */
     public void addTaskToQueue(Task task){
 	task.setTaskExecutor(this);
 	mQueue.add(task);
 	queueModified();
     }
 
     /**
      * @param task
      * Provide an existing Task to remove from the queue. You can use
      * findTaskForTag to locate a particular Task. This will not stop a Task
      * from executing if you've already called executeQueue().
      */
     public void removeTaskFromQueue(Task task){
 	mQueue.remove(task);
 	queueModified();
     }
 
     /**
      * Execute all tasks in the queue that haven't been executed already.
      */
     public void executeQueue(){
 	Log.d(TaskExecutor.class.getName(), "Execute " + mQueue.size() + " Tasks");
 	for (int i = 0; i < mQueue.size(); i++){
 	    if (!mTaskExecutor.getQueue().contains(mQueue.get(i))){
 		Future<?> future = mTaskExecutor.submit(mQueue.get(i));
 		setInterruptorIfActive(future);
 	    }
 	}
     }
     
     /**
      * This method directly executes the Task, bypassing the queue. Useful for the 
      * abstract TaskLoader as no callback is needed for the method params. No callback 
      * will be posted to the Activity as the TaskLoader manages the ui callback!
      * @param task
      * 
      */
     public void executeTask(Task task){
 	Future<?> future = mTaskExecutor.submit(task);
 	setInterruptorIfActive(future);
     }
     
     /**
      * @param seconds
      * -1 disables this feature, and it's disabled by default. Set this value 
      * to milliseconds of time. After that amount of time elapses your Task 
      * will be interrupted. If it has yet to execute it will not execute. If the 
      * Task has already begun executing it will be interrupted, which may prove 
      * useful if the Task has a blocking operation like network communication.
      */
    public void setInterruptTaskAfter(int interruptTasksAfter){
	mInterruptTasksAfter = interruptTasksAfter;
     }
     
     private void setInterruptorIfActive(final Future<?> future){
	if (mInterruptTasksAfter != -1){
 	    mHandler.postDelayed(new Runnable(){
 		@Override
 		public void run(){
 		    future.cancel(true);
 		}
	    }, mInterruptTasksAfter);
 	}
     }
 
     /**
      * Block the current running Task prior to the hard callback until
      * finessTasks() is called.
      * @param finessMode 
      * Should Tasks pause waiting for the callback to be re-assigned 
      * in an onResume()?
      */
     public void restrain(boolean finessMode){
 	// Clear the callback to prevent leaks.
 	mTaskCompletedCallback = null;
 	if (finessMode){
 	    mPause = true;
 	    mLock.close();
 	}
     }
 
     /**
      * Resume Task execution from a restrained state.
      * @param callCompleteCallback
      * Provide the taskCompleteCallback so your Tasks can report back to the
      * activity.
      */
     public void finess(TaskCompletedCallback taskCompletedCallback){
 	//Set the callback, finess() will always be called essential on
 	//or after an Activity's onResume().
 	mTaskCompletedCallback = taskCompletedCallback;
 	for (Pair<Bundle, Exception> pair : mPendingCompletedTasks)
 	    mTaskCompletedCallback.onTaskComplete(pair.first, pair.second);
 	mPendingCompletedTasks.clear();
 	mPause = false;
 	mLock.open();
     }
 
     /**
      * @param TAG
      * Provide the TAG of the Task you want to find.
      * @return The Task for the specified TAG. Null is returned if no Task is
      * found. This is useful is you want to specifically set a callback for a
      * particular Task that is queued.
      */
     public Task findTaskForTag(String TAG){
 	for (Task task : mQueue) {
 	    if (task.getTag().equals(TAG))
 		return task;
 	}
 	return null;
     }
 
     /**
      * @return return a count of items currently in the queue.
      */
     public int getQueueCount(){
 	return mQueue.size();
     }
 
     /**
      * @return A reference to the existing Task queue.
      */
     public Vector<Task> getQueue(){
 	return mQueue;
     }
 
     /**
      * @param queue
      * Set the Task queue. Typically used when restoring the TaskExecutor for
      * the persisted instance on disk.
      */
     public void setQueue(Vector<Task> queue){
 	mQueue = queue;
 	queueModified();
     }
 
     /**
      * Clear all items from the queue. If tasks are currently being executed
      * this will not prevent tasks from being executed.
      */
     public void clearQueue(){
 	mQueue.clear();
 	queueModified();
     }
 
     private void queueModified(){
 	mServiceHelperCallback.queueModified();
     }
 }
