 package models;
 
 import javax.persistence.Entity;
 
 import play.db.jpa.Model;
 
 @Entity
 public class Options extends Model{
 	public static final Integer ALIVE_DEFAULT = 60;
	public static final Integer SNOOZE_DEFAULT = 12;
 	public String email;
 	public Boolean enableWarningNotification;
 	public Boolean enableLowTempWarning;
 	public Double  lowTempThreshold;
 	public Boolean enableHighTempWarning;
 	public Double  highTempThreshold = 100.0; //set as default if none provided.
 	public Boolean enablePlantedWarnings;
 	public Integer remoteAliveCheckMins;
	public Integer snoozeActiveWarnings_hours = SNOOZE_DEFAULT;  //if an active warning exists how many hours to snooze it before sending out a new warning Alert.
 
	public Options(String email, Boolean enableWarningNotification, Boolean enableLowTempWarning, Double  lowTempThreshold, Boolean enableHighTempWarning, Double highTempThreshold, Boolean enablePlantedWarnings, Integer remoteAliveCheckMins, Integer snoozeActiveWarnings_hours) {
 		this.email = email;
 		this.enableWarningNotification = (enableWarningNotification == null)? false:enableWarningNotification;
 		
 		this.enableLowTempWarning = (enableLowTempWarning == null)? false:enableLowTempWarning;
 		this.enableHighTempWarning = (enableHighTempWarning == null)? false:enableHighTempWarning;
 		
 		this.lowTempThreshold = (lowTempThreshold == null)? 0.0: lowTempThreshold;
 		
 		this.enablePlantedWarnings = (enablePlantedWarnings == null)? false:enablePlantedWarnings;
 		this.highTempThreshold = (highTempThreshold == null)? 0.0: highTempThreshold;
 		
 		this.remoteAliveCheckMins = (remoteAliveCheckMins == null)?ALIVE_DEFAULT:remoteAliveCheckMins;
		this.snoozeActiveWarnings_hours = (snoozeActiveWarnings_hours == null)?SNOOZE_DEFAULT:snoozeActiveWarnings_hours; 
 	}
 	
 	@Override
 	public String toString() {
 		StringBuilder sb = new StringBuilder("[SensorData]");
     	sb.append(" email=").append(email);
     	sb.append(" enableWarningNotification=").append(enableWarningNotification);
     	sb.append(" enableLowTempWarning=").append(enableLowTempWarning);
     	sb.append(" lowTempThreshold=").append(lowTempThreshold);
     	sb.append(" enableHighTempWarning=").append(enableHighTempWarning);
     	sb.append(" highTempThreshold=").append(highTempThreshold);
     	sb.append(" enablePlantedWarnings=").append(enablePlantedWarnings);
     	sb.append(" remoteAliveCheckMins=").append(remoteAliveCheckMins);
     	return sb.toString();
 	}
 }
