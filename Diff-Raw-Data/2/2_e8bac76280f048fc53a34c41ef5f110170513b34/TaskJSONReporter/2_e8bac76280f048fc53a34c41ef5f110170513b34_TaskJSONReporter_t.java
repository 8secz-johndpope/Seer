 package net.idea.restnet.c.reporters;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.util.Iterator;
 import java.util.UUID;
 
 import net.idea.restnet.c.ResourceDoc;
 import net.idea.restnet.i.task.ITaskStorage;
 import net.idea.restnet.i.task.Task;
 import net.idea.restnet.i.task.TaskResult;
 
 import org.restlet.Context;
 import org.restlet.Request;
 import org.restlet.data.Reference;
 
 public class TaskJSONReporter<USERID> extends TaskURIReporter<USERID> {
 	/**
 	 * 
 	 */
 	private static final long serialVersionUID = 9136099541989811170L;
 	protected String comma = null;
 	public TaskJSONReporter(ITaskStorage<USERID> storage) {
 		super(storage);
 	}
 	public TaskJSONReporter(ITaskStorage<USERID> storage,Request request,ResourceDoc doc) {
 		super(storage,request,doc);
 	}
 	protected TaskJSONReporter(ITaskStorage<USERID> storage,Reference baseRef,ResourceDoc doc) {
 		super(storage,baseRef,doc);
 	}	
 
	private static String format = "\n{\n\t\"uri\":\"%s\",\n\t\"id\": \"%s\",\n\t\"name\": \"%s\",\n\t\"error\": \"%s\",\n\t\"policyError\": \"%s\",\n\t\"status\": \"%s\",\n\t\"started\": %d,\n\t\"completed\": %d,\n\t\"result\": \"%s\",\n\t\"user\": \"%s\"\n}";
 
 	@Override
 	public void processItem(UUID item, Writer output) {
 		try {
 			if (comma!=null) output.write(comma);
 
 			Task<TaskResult,USERID> task = storage.findTask(item);
 			String uri = task.getUri()==null?null:task.getUri().toString();
 			
 			output.write(String.format(format,
 					uri,
 					item.toString(),
 					task.getName()==null?"":task.getName(),
 					task.getError()==null?"":task.getError(),
 					task.getPolicyError()==null?"":task.getPolicyError(),
 					task.getStatus()==null?"":task.getStatus(),
 					task.getStarted(),
 					task.getTimeCompleted(),
 					task.getUri()==null?"":task.getUri(),
 					task.getUserid()==null?"":task.getUserid()
 					));
 			comma = ",";
 		} catch (IOException x) {
 			Context.getCurrentLogger().severe(x.getMessage());
 		}
 	}	
 	@Override
 	public void footer(Writer output, Iterator<UUID> query) {
 		try {
 			output.write("\n]\n}");
 		} catch (Exception x) {}
 	};
 	@Override
 	public void header(Writer output, Iterator<UUID> query) {
 		try {
 			output.write("{\"task\": [");
 		} catch (Exception x) {}
 		
 	};
 }
