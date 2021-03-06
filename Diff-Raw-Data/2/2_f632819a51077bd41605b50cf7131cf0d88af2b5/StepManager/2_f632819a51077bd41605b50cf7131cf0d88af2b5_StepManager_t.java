 package de.sub.goobi.Persistence.apache;
 
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 
 import org.apache.log4j.Logger;
 
 public class StepManager {
 
 	private static final Logger logger = Logger.getLogger(MySQLHelper.class);
 
 	public static StepObject getStepById(int stepId) {
 		StepObject so = null;
 		try {
 			so = MySQLHelper.getStepByStepId(stepId);
 		} catch (SQLException e) {
 			logger.error("Cannot not load step with id " + stepId, e);
 		}
 
 		return so;
 	}
 
 	public static List<StepObject> getStepsForProcess(int processId) {
 		List<StepObject> answer = new ArrayList<StepObject>();
 
 		try {
 			answer = MySQLHelper.getStepsForProcess(processId);
 		} catch (SQLException e) {
 			logger.error("Cannot not load process with id " + processId, e);
 		}
 
 		return answer;
 	}
 
 	public static void updateStep(StepObject step) {
 		
 		try {
 			MySQLHelper.getInstance().updateStep(step);
 		} catch (SQLException e) {
 			logger.error("Cannot not save step with id " + step.getId(), e);
 		}
 		
 	}
 
 	public static void addHistory(Date myDate, double order, String value, int type, int processId) {
 		try {
 			MySQLHelper.getInstance().addHistory( myDate,  order,  value,  type,  processId);
 		} catch (SQLException e) {
 			logger.error("Cannot not save history event", e);
 		}
 	}
 
 	public static List<String> loadScripts(int id) {
 		try {
 			return MySQLHelper.getScriptsForStep( id);
 		} catch (SQLException e) {
			logger.error("Cannot not load scripts for step with id " + id, e);
 		}
 		return new ArrayList<String>();
 	}
 }
