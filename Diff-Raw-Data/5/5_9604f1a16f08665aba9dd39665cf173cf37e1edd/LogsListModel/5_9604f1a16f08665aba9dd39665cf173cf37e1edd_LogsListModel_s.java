 package artnetremote.gui.models;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.logging.Handler;
 import java.util.logging.LogRecord;
 import java.util.logging.Logger;
 
 import javax.swing.AbstractListModel;
 
 /**
  * A {@code ListModel} that wrap one or more {@code java.util.Logger}.
  * 
  * @author jeremie
  * @author Alexandre COLLIGNON
  */
 public class LogsListModel extends AbstractListModel {
 
 	private static final long serialVersionUID = -3782927318483496410L;
 
 	/** list of logs **/
 	private List<String> logs;
 	
 	/**
	 * Contructs a {@code LogsListModel} that wraps the 
 	 * {@code java.util.Logger} given in argument.
 	 * 
 	 * @param logger {@code java.util.Logger} to wrap.
 	 */
 	public LogsListModel(Logger logger) {
 		logs = new ArrayList<String>();
 	}
 	
 	/**
 	 * Adds a {@code java.util.Logger} to wrap.
 	 * 
 	 * @param logger {@code java.util.Logger} to wrap
 	 */
 	public void addLogger(final Logger logger) {
 		logger.addHandler(new Handler() {
 			
 			@Override
 			public void publish(LogRecord record) {
 				addLogLine("[" + logger.getName() + "] " + record.getMessage());
 			}
 			
 			@Override
 			public void flush() {
 				// nothing
 			}
 			
 			@Override
 			public void close() throws SecurityException {
 				logs.add(logger.getName() + " is closing");
 			}
 		});
 	}
 	
 	/**
	 * Adds a Log Line in the list.
 	 * 
 	 * @param logLine the log line to add
 	 */
 	private void addLogLine(String logLine){
 		logs.add(logLine);
 		this.fireIntervalAdded(this, logs.size()-1, logs.size()-1);
 	}
 	
 	/**
 	 * Removes all log lines of the list.
 	 */
 	public void clearLogsList(){
 		int lastSize = logs.size();
 		if(lastSize>0){
 			logs.clear();
 			this.fireIntervalRemoved(this, 0, lastSize-1);			
 		}
 	}
 	
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public Object getElementAt(int index) {
 		return logs.get(index);
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public int getSize() {
 		return logs.size();
 	}
 }
