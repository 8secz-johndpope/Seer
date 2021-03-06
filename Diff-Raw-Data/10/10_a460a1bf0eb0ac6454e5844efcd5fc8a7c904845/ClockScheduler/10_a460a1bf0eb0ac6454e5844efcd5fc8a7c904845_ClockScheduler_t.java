 package com.almende.eve.scheduler;
 
 import java.io.Serializable;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.UUID;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.joda.time.DateTime;
 
 import com.almende.eve.agent.Agent;
 import com.almende.eve.agent.AgentHost;
 import com.almende.eve.rpc.RequestParams;
 import com.almende.eve.rpc.annotation.Sender;
 import com.almende.eve.rpc.jsonrpc.JSONRequest;
 import com.almende.eve.rpc.jsonrpc.JSONResponse;
 import com.almende.eve.rpc.jsonrpc.jackson.JOM;
 import com.almende.eve.scheduler.clock.Clock;
 import com.almende.eve.scheduler.clock.RunnableClock;
 
 public class ClockScheduler extends AbstractScheduler implements Runnable {
 	private static final Logger		LOG			= Logger.getLogger("ClockScheduler");
 	private final Agent				myAgent;
 	private final Clock				myClock;
 	private final ClockScheduler	_this		= this;
 	private static final String		TASKLIST	= "_taskList";
 	private static final int 		MAXCOUNT    = 100;
 	
 	public ClockScheduler(Agent myAgent, AgentHost factory) {
 		this.myAgent = myAgent;
 		myClock = new RunnableClock();
 	}
 	
 	@SuppressWarnings("unchecked")
 	public TaskEntry getFirstTask() {
 		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
 				.get(TASKLIST);
 		if (timeline != null && !timeline.isEmpty()) {
 			TaskEntry task = timeline.first();
 			int count = 0;
 			while (task != null && task.isActive() && count < MAXCOUNT) {
 				count++;
 				task = timeline.higher(task);
 			}
 			if (count >= MAXCOUNT) {
 				LOG.warning("Oops: more than 100 tasks active at the same time?!?");
 			}
 			return task;
 		}
 		return null;
 	}
 	
 	public void putTask(TaskEntry task) {
 		putTask(task, false);
 	}
 	
 	@SuppressWarnings("unchecked")
 	public void putTask(TaskEntry task, boolean onlyIfExists) {
 		Set<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent.getState()
 				.get(TASKLIST);
 		Set<TaskEntry> timeline = null;
 		boolean found = false;
 		if (oldTimeline != null) {
 			timeline = new TreeSet<TaskEntry>();
 			TaskEntry[] arr = oldTimeline.toArray(new TaskEntry[0]);
 			for (TaskEntry entry : arr) {
 				if (!entry.getTaskId().equals(task.getTaskId())) {
 					timeline.add(entry);
 				} else {
 					found = true;
 					timeline.add(task);
 				}
 			}
 		}
 		if (!found && !onlyIfExists) {
 			if (timeline == null) {
 				timeline = new TreeSet<TaskEntry>();
 			}
 			timeline.add(task);
 		}
 		if (!myAgent.getState().putIfUnchanged(TASKLIST,
 				(Serializable) timeline, (Serializable) oldTimeline)) {
 			LOG.severe("need to retry putTask...");
 			// recursive retry....
 			putTask(task, onlyIfExists);
 			return;
 		}
 	}
 	
 	@SuppressWarnings("unchecked")
 	@Override
 	public void cancelTask(String id) {
 		TreeSet<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent
 				.getState().get(TASKLIST);
 		TreeSet<TaskEntry> timeline = null;
 		if (oldTimeline != null) {
 			timeline = new TreeSet<TaskEntry>();
 			TaskEntry[] arr = oldTimeline.toArray(new TaskEntry[0]);
 			for (TaskEntry entry : arr) {
 				if (!entry.getTaskId().equals(id)) {
 					timeline.add(entry);
 				}
 			}
 		}
 		if (timeline != null
 				&& !myAgent.getState().putIfUnchanged(TASKLIST, timeline,
 						oldTimeline)) {
 			LOG.severe("need to retry cancelTask...");
 			// recursive retry....
 			cancelTask(id);
 			return;
 		}
 	}
 	
 	public void runTask(final TaskEntry task) {
 		myClock.runInPool(new Runnable() {
 			@Override
 			public void run() {
 				try {
 					if (task.getInterval() <= 0) {
 						// Remove from list
 						cancelTask(task.getTaskId());
 					} else {
 						if (!task.isSequential()) {
 							task.setDue(DateTime.now().plus(task.getInterval()));
 						} else {
 							task.setActive(true);
 						}
 						_this.putTask(task, true);
 						_this.run();
 					}
 					
 					RequestParams params = new RequestParams();
 					String senderUrl = "local://" + myAgent.getId();
 					params.put(Sender.class, senderUrl);
 					
 					//Next call is potentially long duration:
 					JSONResponse resp = myAgent.getAgentHost().receive(
 							myAgent.getId(), task.getRequest(), params);
 					
 					if (task.getInterval() > 0 && task.isSequential()) {
 						task.setDue(DateTime.now().plus(task.getInterval()));
 						task.setActive(false);
 						_this.putTask(task, true);
 						_this.run();
 					}
 					if (resp.getError() != null) {
 						throw resp.getError();
 					}
 				} catch (Exception e) {
 					LOG.log(Level.SEVERE, "Failed to run scheduled task", e);
 				}
 			}
 			
 		});
 	}
 	
 	@Override
 	public String createTask(JSONRequest request, long delay) {
 		return createTask(request, delay, false, false);
 	}
 	
 	@Override
 	public String createTask(JSONRequest request, long delay, boolean interval,
 			boolean sequential) {
 		TaskEntry task = new TaskEntry(DateTime.now().plus(delay), request,
 				(interval ? delay : 0), sequential);
		putTask(task);
 		if (interval || delay <= 0) {
 			runTask(task);
 		} else {
 			run();
 		}
 		return task.getTaskId();
 	}
 	
 	@Override
 	public Set<String> getTasks() {
 		Set<String> result = new HashSet<String>();
		@SuppressWarnings("unchecked")
 		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
 				.get(TASKLIST);
		if (timeline == null || timeline.size() == 0) {
 			return result;
 		}
 		for (TaskEntry entry : timeline) {
 			result.add(entry.getTaskId());
 		}
 		return result;
 	}
 	
 	@Override
 	public void run() {
 		TaskEntry task = getFirstTask();
 		if (task != null) {
 			if (task.getDue().isBeforeNow()) {
 				runTask(task);
 				// recursive call next task
 				run();
 				return;
 			}
 			myClock.requestTrigger(myAgent.getId(), task.getDue(), this);
 		}
 	}
 	
 	@Override
 	public String toString() {
 		@SuppressWarnings("unchecked")
 		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
 				.get(TASKLIST);
 		return (timeline != null) ? timeline.toString() : "[]";
 	}
 }
 
 class TaskEntry implements Comparable<TaskEntry>, Serializable {
 	private static final Logger	LOG					= Logger.getLogger(TaskEntry.class
 															.getCanonicalName());
 	private static final long	serialVersionUID	= -2402975617148459433L;
 	// TODO, make JSONRequest.equals() state something about real equal tasks,
 	// use it as deduplication!
 	private String				taskId;
 	private JSONRequest			request;
 	private DateTime			due;
 	private long				interval			= 0;
 	private boolean				sequential			= true;
 	private boolean				active				= false;
 	
 	public TaskEntry(DateTime due, JSONRequest request, long interval,
 			boolean sequential) {
 		taskId = UUID.randomUUID().toString();
 		this.request = request;
 		this.due = due;
 		this.interval = interval;
 		this.sequential = sequential;
 	}
 	
 	@Override
 	public boolean equals(Object o) {
 		if (this == o) {
 			return true;
 		}
 		if (!(o instanceof TaskEntry)) {
 			return false;
 		}
 		TaskEntry other = (TaskEntry) o;
 		return taskId.equals(other.taskId);
 	}
 	
 	@Override
 	public int hashCode() {
 		return taskId.hashCode();
 	}
 	
 	@Override
 	public int compareTo(TaskEntry o) {
 		if (equals(o)) {
 			return 0;
 		}
 		if (due.equals(o.due)) {
 			return 0;
 		}
 		return due.compareTo(o.due);
 	}
 	
 	public String getTaskId() {
 		return taskId;
 	}
 	
 	public JSONRequest getRequest() {
 		return request;
 	}
 	
 	public String getDueAsString() {
 		return due.toString();
 	}
 	
 	public DateTime getDue() {
 		return due;
 	}
 	
 	public long getInterval() {
 		return interval;
 	}
 	
 	public void setTaskId(String taskId) {
 		this.taskId = taskId;
 	}
 	
 	public void setRequest(JSONRequest request) {
 		this.request = request;
 	}
 	
 	public void setDue(DateTime due) {
 		this.due = due;
 	}
 	
 	public void setInterval(long interval) {
 		this.interval = interval;
 	}
 	
 	public void setSequential(boolean sequential) {
 		this.sequential = sequential;
 	}
 	
 	public void setActive(boolean active) {
 		this.active = active;
 	}
 	
 	public boolean isSequential() {
 		return sequential;
 	}
 	
 	public boolean isActive() {
 		return active;
 	}
 	
 	@Override
 	public String toString() {
 		try {
 			return JOM.getInstance().writeValueAsString(this);
 		} catch (Exception e) {
 			LOG.log(Level.WARNING, "Couldn't use Jackson to print task.", e);
 			return "{\"taskId\":" + taskId + ",\"due\":" + due + "}";
 		}
 	}
 }
