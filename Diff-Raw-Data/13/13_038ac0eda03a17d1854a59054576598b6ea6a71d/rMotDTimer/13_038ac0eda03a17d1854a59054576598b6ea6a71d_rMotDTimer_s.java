 import java.util.Random;
 import java.util.Timer;
 import java.util.TimerTask;
 
 public class rMotDTimer extends TimerTask{
 	String [] Messages;
 	rMotD rMotD;
 	Timer timer;
 	private int progression;
 	Random generator;
 	private static final int random = 1;
 	private static final int sequential = 0;
 	int nextMessage = 0;
 	int delay;
 	// Listname, delay, progression
 	public rMotDTimer(rMotD rMotD, Timer timer, String [] Messages){
 		this.Messages = Messages;
 		this.rMotD = rMotD;
 		this.timer = timer;
 		this.generator = new Random();
 		String [] split =  Messages[0].split(":");
 		String [] options = split[1].split(",");
 		this.progression = new Integer(options[2]);
 		if (progression == random){
 			this.nextMessage =  generator.nextInt(Messages.length);
 		} else if (progression == sequential){
 			this.nextMessage = 1;
 		} else {
 			// Error?
 		}
		this.delay = new Integer(options[1]) * 60;
 	}
 	public rMotDTimer() {
 	}
 	
 	public rMotDTimer clone(){
 		rMotDTimer clone = new rMotDTimer();
 		clone.rMotD = this.rMotD;
 		clone.Messages = this.Messages;
 		clone.nextMessage = this.nextMessage;
 		clone.generator = this.generator;
 		clone.timer = this.timer;
 		clone.delay = this.delay;
 		clone.progression = this.progression;
 		return clone;
 	}
 	
 	public void run() {
 		// parse into groups, next time, 'progression'
 		String toParse = Messages[nextMessage];
 		String [] split =  toParse.split(":");
 		String [] options =  split[1].split(",");
 		String Groups = split[0];
 		try{
 			delay = new Integer(options[1]) * 1000;
 		} catch (NumberFormatException blargh){
 			rMotD.log.info("[rMotD] Invalid timer interval!");
 			return;
 		}
 		String message = etc.combineSplit(2, split, ":");
 
 		// Send message
 		String [] sendToGroups = Groups.split(",");
 		rMotD.sendToGroups(sendToGroups, message, null);
 		
 		// Find next sequence
 		if (progression == random) {
			int prospectiveNext = nextMessage;
			while (prospectiveNext == nextMessage)
				prospectiveNext = generator.nextInt(Messages.length);
			nextMessage = prospectiveNext;
 		}
 		else if (progression == sequential)
 			nextMessage = ( nextMessage + 1 ) % Messages.length;
 		else {
 			// TODO: I am error!
 		}
 		
 		// Shit bricks
 		// Schedule next run
 		rMotDTimer clone = (rMotDTimer) this.clone();
 		timer.schedule(clone, delay);
 	}
 
 }
