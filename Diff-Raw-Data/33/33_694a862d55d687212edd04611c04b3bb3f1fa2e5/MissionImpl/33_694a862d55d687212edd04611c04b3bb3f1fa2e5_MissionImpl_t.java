 /**
  * 
  */
 package de.beimax.spacealert.mission;
 
 import java.util.ArrayList;
 import java.util.Random;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import de.beimax.spacealert.util.Options;
 
 /**
  * Default Mission Generator
  * @author mkalus
  */
 public class MissionImpl implements Mission {
 	static private Logger logger = Logger.getLogger("MissionLogger");
 	static {
 		// debugging option set?
 		if (Options.getOptions().debug) logger.setLevel(Level.FINEST);
 		else logger.setLevel(Level.WARNING);
 	}
 	
 	/**
 	 * configuration: threat level (8 for std game)
 	 */
 	private int threatLevel = 8;
 	
 	/**
 	 * ...of which 1 level is unconfirmed (for 5 players)
 	 */
 	private int threatUnconfirmed = 1;
 	
 	/**
 	 * ...of which x levels are internal
 	 */
 	private int minInternalThreats = 1;
 	private int maxInternalThreats = 3;
 	private int maxInternalThreatsNumber = 2; // number of internal threats max
 	
 	/**
 	 * minimum and maximum time in which normal threats can occur
 	 */
 	private int minTNormalExternalThreat = 1;
 	private int maxTNormalExternalThreat = 8;
 	
 	/**
 	 * minimum and maximum time in which serious threats can occur
 	 */
 	private int minTSeriousExternalThreat = 2;
 	private int maxTSeriousExternalThreat = 7;
 	
 	/**
 	 * minimum and maximum time in which normal threats can occur
 	 */
 	private int minTNormalInternalThreat = 2;
 	private int maxTNormalInternalThreat = 7;
 	
 	/**
 	 * minimum and maximum time in which serious threats can occur
 	 */
 	private int minTSeriousInternalThreat = 3;
 	private int maxTSeriousInternalThreat = 6;
 	
 	/**
 	 * minimum data operations (either data transfer or incoming data)
 	 */
 	private int[] minDataOperations = {2, 2, 0};
 	private int[] maxDataOperations = {3, 3, 1};
 	
 	/**
 	 * minimum and maximum incoming data by phases
 	 */
 	private int[] minIncomingData = {1, 0, 0};
 	private int[] maxIncomingData = {3, 2, 0};
 	private int minIncomingDataTotal = 2;
 	
 	/**
 	 * minimum and maximum data transfers by phases
 	 */
 	private int[] minDataTransfer = {0, 1, 1};
 	private int[] maxDataTransfer = {1, 2, 1};
 	private int minDataTransferTotal = 3;
 	
 	/**
 	 * minimum and maximum time for white noise
 	 */
 	private int minWhiteNoise = 45;
 	private int maxWhiteNoise = 60;
 	private int minWhiteNoiseTime = 9;
 	private int maxWhiteNoiseTime = 20;
 	
 	/**
 	 * minimum and maximum time for phases
 	 */
 	private int[] minPhaseTime = {205, 180, 140};
 	private int[] maxPhaseTime = {240, 225, 155};
 	
 	/**
 	 * times for first threats to appear
 	 */
 	private int[] minTimeForFirst = { 10, 10 };
 	private int[] maxTimeForFirst = { 20, 40 };
 	
 	/**
 	 * chance for ambush in phases 4/8 in %
 	 */
 	private int[] chanceForAmbush = { 40, 40 };
 	
 	/**
 	 * "middle" threats (2+3/5+6) should appear with % of phase length
 	 */
 	private int threatsWithInPercent = 70;
 	
 	/**
 	 * keeps threats
 	 */
 	private Threat[] threats;
 	
 	/**
 	 * keeps incoming and data transfers
 	 */
 	private int[] incomingData;
 	private int[] dataTransfers;
 	
 	/**
 	 * white noise chunks in seconds (to distribute)
 	 */
 	private WhiteNoise[] whiteNoise;
 
 	/**
 	 * phase times in seconds
 	 */
 	private int[] phaseTimes;
 	
 	/**
 	 * event list
 	 */
 	EventList eventList;
 	
 	/**
 	 * random number generator
 	 */
 	Random generator;
 	
 	/**
 	 * Constructor
 	 */
 	public MissionImpl() {
		// get Options
		Options options = Options.getOptions();
		
		long seed;
		if (options.seed == null) seed = System.nanoTime() + 8682522807148012L;
		else seed = (long) options.seed;
 		// random number generator
		generator = new Random(seed);
		
		// print out seed?
		if (options.printSeed)
			System.out.println("Random number generator seed: " +  seed);
 	}
 	
 	/**
 	 * Return event list of mission
 	 * @return ordered event list of mission
 	 */
 	public EventList getMissionEvents() {
 		return eventList;
 	}
 
 	/**
 	 * Return length of a phase in seconds
 	 * @param phase 1-3
 	 * @return phase length of mission or -1
 	 */
 	public int getMissionPhaseLength(int phase) {
 		if (phase < 1 || phase > phaseTimes.length) return -1;
 		return phaseTimes[phase-1];
 	}
 
 	/**
 	 * Generate new mission
 	 * 
 	 * @return true if mission creation succeeded
 	 */
 	public boolean generateMission() {
 		// generate threats
 		boolean generated;
 		int tries = 100; //maximum number of tries to generate mission
 		do {generated = generateThreats();} while(!generated && tries-- > 0);
 		if (!generated) {
 			logger.warning("Giving up creating threats.");
 			return false; //fail
 		}
 
 		// generate data transfer and incoming data
 		generated = false; tries = 100;
 		do {generated = generateDataOperations();} while(!generated && tries-- > 0);
 		if (!generated) {
 			logger.warning("Giving up creating data operations.");
 			return false; //fail
 		}
 		
 		//generate times
 		generateTimes();
 
 		//generate phases
 		generatePhases();
 		
 		return false;
 	}
 	
 	/**
 	 * "sane" generator method for threats
 	 * @return true if generation was successful
 	 */
 	protected boolean generateThreats() {
 		// number of internal threats
 		int internalThreats = generator.nextInt(maxInternalThreats - minInternalThreats + 1) + minInternalThreats;
 		int externalThreats = threatLevel - internalThreats;
 		
 		logger.fine("Threat Level: " + threatLevel + "; interal = " + internalThreats + ", external = " + externalThreats);
 		
 		// generate number of serious threats
 		int seriousThreats = generator.nextInt(threatLevel / 2 + 1);
 		// if we only have serious threats and normal unconfirmed reports: reduce number of threats by 1
 		if (threatUnconfirmed % 2 == 1 && seriousThreats * 2 == threatLevel)
 			seriousThreats--;
 		int normalThreats =  threatLevel - seriousThreats * 2;
 		
 		logger.fine("Normal Threats: " + normalThreats + "; Serious Threats: " + seriousThreats);
 		
 		// if there are 8 normal threats - check again, if we really want this
 		if (normalThreats == 8 && generator.nextInt(3) != 0) {
 			logger.info("8 normal threats unlikely. Redoing.");
 			return false;
 		}
 
 		// get sums
 		int threatsSum = normalThreats + seriousThreats;
 		
 		// distribute unconfirmed
 		int seriousUnconfirmed = generator.nextInt(threatUnconfirmed / 2 + 1);
 		int normalUnconfirmed = threatUnconfirmed - seriousUnconfirmed * 2;
 		if (normalUnconfirmed > normalThreats) { // adjust, if there are not enough threats
 			normalUnconfirmed -= 2;
 			seriousUnconfirmed++;
 		}
 		else if (seriousUnconfirmed > seriousThreats) { // adjust, if there are not enough serious threats
 			normalUnconfirmed += 2;
 			seriousUnconfirmed--;
 		}
 		logger.fine("Normal unconfirmed Threats: " + normalUnconfirmed + "; Serious unconfirmed Threats: " + seriousUnconfirmed);
 
 		// sane threat distribution onto phase 1 and 2
 		int threatsFirstPhase = threatsSum / 2 + generator.nextInt(3)-1;
 		int threatsSecondPhase = threatsSum - threatsFirstPhase;
 		if (threatsSecondPhase > threatsFirstPhase && threatsSecondPhase - threatsFirstPhase > 1) {
 			threatsSecondPhase--;
 			threatsFirstPhase++;
 		} else if (threatsSecondPhase < threatsFirstPhase && threatsFirstPhase - threatsSecondPhase > 1) {
 			threatsSecondPhase++;
 			threatsFirstPhase--;
 		}
 		
 		logger.fine("Threats 1st phase: " + threatsFirstPhase + "; Threats 2nd phase: " + threatsSecondPhase);
 		
 		// phases
 		ArrayList<Integer> phaseOne = new ArrayList<Integer>(4);
 		for (int i = 1; i <= 4; i++) phaseOne.add(new Integer(i));
 		ArrayList<Integer> phaseTwo = new ArrayList<Integer>(4);
 		for (int i = 5; i <= 8; i++) phaseTwo.add(new Integer(i));
 		
 		// remove random entries from the phases
 		for (int i = 0; i < 4-threatsFirstPhase; i++) {
 			phaseOne.remove(generator.nextInt(phaseOne.size()));
 		}
 		for (int i = 0; i < 4-threatsSecondPhase; i++) {
 			phaseTwo.remove(generator.nextInt(phaseTwo.size()));
 		}
 		
 		// free memory
 		ArrayList<Integer> phases = new ArrayList<Integer>(threatsFirstPhase + threatsSecondPhase);
 		for (int i = 0; i < threatsFirstPhase; i++) phases.add(phaseOne.get(i));
 		for (int i = 0; i < threatsSecondPhase; i++) phases.add(phaseTwo.get(i));
 		phaseOne = null; phaseTwo = null;
 
 		// create threats by level
 		threats = new Threat[8];
 		// counter for maximum internal threats
 		int internalThreatsNumber = 0;
 		//statistics counter to make internal threats likely, too
 		int externalThreatLevelLeft = externalThreats;
 		for (int i = 0; i < threatsSum; i++) {
 			Threat newThreat = new Threat(); // new threat created
 			if (i < seriousThreats) {
 				newThreat.setThreatLevel(Threat.THREAT_LEVEL_SERIOUS);
 				// unconfirmed reports
 				if (seriousUnconfirmed > 0) {
 					seriousUnconfirmed--;
 					newThreat.setConfirmed(false);
 				} else newThreat.setConfirmed(true);
 			}
 			else {
 				newThreat.setThreatLevel(Threat.THREAT_LEVEL_NORMAL);
 				// unconfirmed reports
 				if (normalUnconfirmed > 0) {
 					normalUnconfirmed--;
 					newThreat.setConfirmed(false);
 				} else newThreat.setConfirmed(true);
 			}
 			// internal/external?
 			if (generator.nextInt(threatsSum - i) + 1 <= externalThreatLevelLeft) {
 				if (newThreat.getThreatLevel() == Threat.THREAT_LEVEL_SERIOUS) {
 					if (externalThreatLevelLeft == 1) { // not enough external threat level left => make internal
 						newThreat.setThreatPosition(Threat.THREAT_POSITION_INTERNAL);
 						internalThreatsNumber++;
 					} else { // serious threat level deduction
 						externalThreatLevelLeft -= 2;
 						newThreat.setThreatPosition(Threat.THREAT_POSITION_EXTERNAL);
 					}
 				} else { // normal threat level deduction
 					externalThreatLevelLeft--;
 					newThreat.setThreatPosition(Threat.THREAT_POSITION_EXTERNAL);
 				}
 			} else {
 				newThreat.setThreatPosition(Threat.THREAT_POSITION_INTERNAL);
 				internalThreatsNumber++;
 			}
 			if (internalThreatsNumber > maxInternalThreatsNumber) {
 				logger.info("Too many internal threats. Redoing.");
 				return false;
 			}
 			
 			// define phase
 			int maxCounter = 3; // try three times before giving up
 			boolean found = false;
 			do {
 				int idx = generator.nextInt(phases.size());
 				int phase = phases.get(idx).intValue();
 				if (newThreat.getThreatLevel() == Threat.THREAT_LEVEL_SERIOUS) {
 					if (newThreat.getThreatPosition() == Threat.THREAT_POSITION_EXTERNAL) {
 						if (phase < minTSeriousExternalThreat || phase > maxTSeriousExternalThreat) continue;
 					} else {
 						if (phase < minTSeriousInternalThreat || phase > maxTSeriousInternalThreat) continue;
 					}
 				} else {
 					if (newThreat.getThreatPosition() == Threat.THREAT_POSITION_EXTERNAL) {
 						if (phase < minTNormalExternalThreat|| phase > maxTNormalExternalThreat) continue;
 					} else {
 						if (phase < minTNormalInternalThreat || phase > maxTNormalInternalThreat) continue;
 					}
 				}
 				found = true;
 				newThreat.setTime(phase);
 				phases.remove(idx);
 			} while(!found && maxCounter-- > 0);
 			if (!found) {
 				logger.info("Could not create mission due to phase restrictions. Redoing.");
 				return false;
 			}
 
 			//System.out.println(newThreat);
 			threats[newThreat.getTime()-1] = newThreat;
 		} // for (int i = 0; i < threatsSum; i++) {
 		
 		// now sort mission entries and generate attack sectors
 		int lastSector = -1;
 		for (int i = 0; i < 8; i++) {
 			if (threats[i] != null && threats[i].getThreatPosition() == Threat.THREAT_POSITION_EXTERNAL) {
 				switch(generator.nextInt(3)) {
 				case 0: if (lastSector != Threat.THREAT_SECTOR_BLUE) threats[i].setSector(Threat.THREAT_SECTOR_BLUE);
 						else threats[i].setSector(Threat.THREAT_SECTOR_WHITE); break;
 				case 1: if (lastSector != Threat.THREAT_SECTOR_WHITE) threats[i].setSector(Threat.THREAT_SECTOR_WHITE);
 						else threats[i].setSector(Threat.THREAT_SECTOR_RED); break;
 				case 2: if (lastSector != Threat.THREAT_SECTOR_RED) threats[i].setSector(Threat.THREAT_SECTOR_RED);
 						else threats[i].setSector(Threat.THREAT_SECTOR_BLUE); break;
 				default: System.out.println("No Way!");
 				}
 				lastSector = threats[i].getSector();
 			}
 
 			//if (threats[i] != null) System.out.println(threats[i]);
 		}
 		
 		return true;
 	}
 	
 	/**
 	 * Generate data operations (either data transfer or incoming data)
 	 * @return true, if data creation could be generated
 	 */
 	protected boolean generateDataOperations() {
 		// clear data
 		incomingData = new int[3];
 		dataTransfers = new int[3];
 		
 		int incomingSum = 0;
 		int transferSum = 0;
 
 		// generate stuff by phase
 		for (int i = 0; i < 3; i++) {
 			incomingData[i] = generator.nextInt(maxIncomingData[i] - minIncomingData[i] + 1) + minIncomingData[i];
 			dataTransfers[i] = generator.nextInt(maxDataTransfer[i] - minDataTransfer[i] + 1) + minDataTransfer[i];
 			
 			// check minimums
 			if (incomingData[i] + dataTransfers[i] < minDataOperations[i] ||
 					incomingData[i] + dataTransfers[i] > maxDataOperations[i]) return false;
 			
 			incomingSum += incomingData[i];
 			transferSum += dataTransfers[i];
 		}
 		
 		// check minimums
 		if (incomingSum < minIncomingDataTotal || transferSum < minDataTransferTotal) return false;
 		
 		// debuggin information
 		if (logger.getLevel() == Level.FINE) {
 			for (int i = 0; i < 3; i++) {
 				logger.fine("Phase " + (i+1) + ": Incoming Data = " + incomingData[i] + "; Data Transfers = " + dataTransfers[i]);
 			}
 		}
 		
 		return true;
 	}
 	
 	/**
 	 * simple generation of times for phases, white noise etc.
 	 */
 	protected void generateTimes() {
 		// generate white noise
 		int whiteNoiseTime = generator.nextInt(maxWhiteNoise - minWhiteNoise + 1) + minWhiteNoise;
 		logger.fine("White noise time: " + whiteNoiseTime);
 		
 		// create chunks
 		ArrayList<Integer> whiteNoiseChunks = new ArrayList<Integer>();
 		while (whiteNoiseTime > 0) {
 			// create random chunk
 			int chunk = generator.nextInt(maxWhiteNoiseTime - minWhiteNoiseTime + 1) + minWhiteNoiseTime;
 			// check if there is enough time left
 			if (chunk > whiteNoiseTime) {
 				// hard case: smaller than minimum time
 				if (chunk < minWhiteNoiseTime) {
 					// add to last chunk that fits
 					for (int i = whiteNoiseChunks.size()-1; i >= 0; i--) {
 						int sumChunk = whiteNoiseChunks.get(i) + chunk;
 						// if smaller than maximum time: add to this chunk
 						if (sumChunk <= maxWhiteNoiseTime) {
 							whiteNoiseChunks.set(i, sumChunk);
 							whiteNoiseTime = 0;
 							break;
 						}
 					}
 					// still not zeroed
 					if (whiteNoiseTime > 0) { // add to last element, regardless - quite unlikely though
 						int lastIdx = whiteNoiseChunks.size()-1;
 						whiteNoiseChunks.set(lastIdx, whiteNoiseChunks.get(lastIdx) + chunk);
 						whiteNoiseTime = 0;
 					}
 				} else { // easy case: create smaller rest chunk
 					whiteNoiseChunks.add(whiteNoiseTime);
 					whiteNoiseTime = 0;
 				}
 			} else { // add new chunk
 				whiteNoiseChunks.add(chunk);
 				whiteNoiseTime -= chunk;
 			}
 		}
 		
 		// ok, add chunks to mission
 		whiteNoise = new WhiteNoise[whiteNoiseChunks.size()];
 		for (int i = 0; i < whiteNoiseChunks.size(); i++) whiteNoise[i] = new WhiteNoise(whiteNoiseChunks.get(i));
 		
 		// add mission lengths
 		phaseTimes = new int[3];
 		for (int i = 0; i < 3; i++) {
 			phaseTimes[i] = generator.nextInt(maxPhaseTime[i] - minPhaseTime[i] + 1) + minPhaseTime[i];
 		}
 	}
 	
 	/**
 	 * generate phase stuff from data above
 	 */
 	protected void generatePhases() {
 		logger.info("Data gathered: Generating phases.");
 
 		// create events
 		eventList = new EventList();
 
 		// add fixed events: announcements
 		eventList.addPhaseEvents(phaseTimes[0], phaseTimes[1], phaseTimes[2]);
 		
 		boolean ambushOccured = false;
 		// add threats in first phase
 		int last = 3; // last index to check
 		// ambush handling - is there a phase 4, and it is a normal external threat? ... and chance is taken?
 		if (threats[3] != null && threats[3].getThreatLevel() == Threat.THREAT_LEVEL_NORMAL &&
 				threats[3].getThreatPosition() == Threat.THREAT_POSITION_EXTERNAL && generator.nextInt(100) + 1 < chanceForAmbush[0]) {
 			//...then add an "ambush" threat between 1 minute and 20 secs warnings
 			boolean done = false; // try until it fits
 			do {
 				// TODO: remove hardcoded length here:
 				int ambushTime = generator.nextInt(35) + phaseTimes[0] - 59;
 				logger.info("Ambush in phase 1 at time: " + ambushTime);
 				done = eventList.addEvent(ambushTime, threats[3]);
 			} while (!done);
 			
 			last = 2; // ignore last threat
 			ambushOccured = true; // to disallow two ambushes in one game
 		}
 
 		// to be used further down
 		int[] lastThreatTime = { 0, 0 };
 
 		// add the rest of the threats
 		int currentTime = generator.nextInt(maxTimeForFirst[0] - minTimeForFirst[0] + 1) + minTimeForFirst[0];
 		// threats should appear within this time
 		int lastTime = (int) (phaseTimes[0] * (((float)threatsWithInPercent) / 100));
 		boolean first = true;
 		// look for first threat
 		for (int i = 0; i <= last; i++) {
 			if (threats[i] == null) continue;
 			// first event?
 			if (first) {
 				if (!eventList.addEvent(currentTime, threats[i])) logger.warning("Could not add first event to list - arg!");
 				first = false;
 			} else {
 				boolean done = false; // try until it fits
 				int nextTime = 0;
 				do {
 					// next threat appears
 					nextTime = generator.nextInt((lastTime - currentTime) / 2) + 5;
 					done = eventList.addEvent(currentTime + nextTime, threats[i]);
 				} while (!done);
 				currentTime += nextTime;
 				// save lastThreatTime for data transfers further down
 				if (i < 3) lastThreatTime[0] = currentTime;
 			}
 			// add to time
 			currentTime += threats[i].getLengthInSeconds();
 		}
 
 		// add threats in second phase
 		last = 7; // last index to check
 		// ambush handling - is there a phase 8, and it is a normal external threat? ... and chance is taken?
 		if (!ambushOccured && threats[7] != null && threats[7].getThreatLevel() == Threat.THREAT_LEVEL_NORMAL &&
 				threats[7].getThreatPosition() == Threat.THREAT_POSITION_EXTERNAL && generator.nextInt(100) + 1 < chanceForAmbush[1]) {
 			//...then add an "ambush" threat between 1 minute and 20 secs warnings
 			boolean done = false; // try until it fits
 			do {
 				// TODO: remove hardcoded length here:
 				int ambushTime = generator.nextInt(35) + phaseTimes[0] + phaseTimes[1] - 59;
 				logger.info("Ambush in phase 2 at time: " + ambushTime);
 				done = eventList.addEvent(ambushTime, threats[7]);
 			} while (!done);
 			
 			last = 6; // ignore last threat
 		}
 
 		// add the rest of the threats
 		currentTime = phaseTimes[0] + generator.nextInt(maxTimeForFirst[1] - minTimeForFirst[1] + 1) + minTimeForFirst[1];
 		// threats should appear within this time
 		lastTime = phaseTimes[0] + (int) (phaseTimes[1] * (((float)threatsWithInPercent) / 100));
 		first = true;
 		// look for first threat
 		for (int i = 4; i <= last; i++) {
 			if (threats[i] == null) continue;
 			// first event?
 			if (first) {
 				if (!eventList.addEvent(currentTime, threats[i])) logger.warning("Could not add first event to list in second phase - arg!");
 				first = false;
 			} else {
 				boolean done = false; // try until it fits
 				int nextTime = 0;
 				do {
 					// next element occurs
 					nextTime = generator.nextInt((lastTime - currentTime) / 2) + 5;
 					done = eventList.addEvent(currentTime + nextTime, threats[i]);
 				} while (!done);
 				currentTime += nextTime;
 				// save lastThreatTime for data transfers further down
 				if (i < 7) lastThreatTime[1] = currentTime;
 			}
 			// add to time
 			currentTime += threats[i].getLengthInSeconds();
 		}
 
 		//add data transfers
 		// get start and end times
 		int startTime = 0;
 		int endTime = 0;
 		// special balance: first data transfers in phase 1 and 2 should occur shortly after first threat wave
 		for (int i = 0; i < 2; i++) {
 			startTime = endTime;
 			endTime += phaseTimes[i];
 			if (dataTransfers[i] > 0) { // if there is a data transfer
 				startTime = lastThreatTime[i];
 				boolean done = false; // try until it fits
 				do { // try to add incoming data within 30 seconds of event
 					startTime = generator.nextInt(31) + startTime + 1;
 					done = eventList.addEvent(startTime, new DataTransfer());
 				} while (!done && startTime < endTime);
 				if (done) {
 					// reduce data transfers below
 					dataTransfers[i]--;
 				}
 			}
 		}
 		
 		startTime = 0;
 		endTime = 0;
 		// distribute rest of data transfers and incoming data randomly within the phases
 		for (int i = 0; i < 3; i++) {
 			// recalculate phase times
 			startTime = endTime;
 			endTime += phaseTimes[i];
 			// data transfer first, since these are fairly long
 			for (int j = 0; j < dataTransfers[j]; j++) {
 				boolean done = false; // try until it fits
 				do {
 					// white noise can pretty much occur everywhere
 					int time = generator.nextInt(endTime - startTime) + startTime - 5; // to fend off events after mission ends
 					done = eventList.addEvent(time, new DataTransfer());
 				} while (!done);
 			}
 			// incoming data second
 			for (int j = 0; j < incomingData[j]; j++) {
 				boolean done = false; // try until it fits
 				do {
 					// white noise can pretty much occur everywhere
 					int time = generator.nextInt(endTime - startTime) + startTime - 5; // to fend off events after mission ends
 					done = eventList.addEvent(time, new IncomingData());
 				} while (!done);
 			}
 		}
 
 		//add white noise at random times
 		for (int i = 0; i < whiteNoise.length; i++) {
 			boolean done = false; // try until it fits
 			do {
 				// white noise can pretty much occur everywhere
 				int time = generator.nextInt(phaseTimes[0] + phaseTimes[1] + phaseTimes[2] - 30) + 10;
 				done = eventList.addEvent(time, whiteNoise[i]);
 			} while (!done);
 		}
 	}
 	
 	/**
 	 * Prints list of missions
 	 */
 	@Override
 	public String toString() {
 		return eventList.toString();
 	}
 }
