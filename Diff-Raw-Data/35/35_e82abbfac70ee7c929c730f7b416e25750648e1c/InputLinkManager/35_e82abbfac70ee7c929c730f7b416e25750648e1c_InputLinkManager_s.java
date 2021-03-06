 package tanksoar;
 
import java.util.logging.*;
 
 import simulation.Simulation;
 import sml.Agent;
 import sml.FloatElement;
 import sml.Identifier;
 import sml.IntElement;
 import sml.StringElement;
 import sml.WMElement;
 import utilities.*;
 
 public class InputLinkManager {
 
 	// public for visual agent world
 	public final static String kEnergyID = "energy";
 	public final static String kHealthID = "health";
 	public final static String kMissilesID = "missiles";
 	public final static String kObstacleID = "obstacle";
 	public final static String kOpenID = "open";
 	public final static String kTankID = "tank";
 	
 	private final static String kBlockedID = "blocked";
 	private final static String kClockID = "clock";
 	private final static String kColorID = "color";
 	private final static String kCurrentScoreID = "current-score";
 	private final static String kDirectionID = "direction";
 	private final static String kDistanceID = "distance";
 	private final static String kEnergyRechargerID = "energyrecharger";
 	private final static String kHealthRechargerID = "healthrecharger";
 	private final static String kIncomingID = "incoming";
 	private final static String kMyColorID = "my-color";
 	private final static String kRadarID = "radar";
 	private final static String kRadarDistanceID = "radar-distance";
 	private final static String kRadarSettingID = "radar-setting";
 	private final static String kRadarStatusID = "radar-status";
 	private final static String kRandomID = "random";
 	private final static String kResurrectID = "resurrect";
 	private final static String kRWavesID = "rwaves";
 	private final static String kShieldStatusID = "shield-status";
 	private final static String kSmellID = "smell";
 	private final static String kSoundID = "sound";
 	private final static String kXID = "x";
 	private final static String kYID = "y";
 	
 	private final static String kBackwardID = "backward";
 	private final static String kForwardID = "forward";
 	private final static String kLeftID = "left";
 	private final static String kRightID = "right";
 	private final static String kSilentID = "silent";
 	private final static String kPositionID = "position";
 	private final static String kCenterID = "center";
 	
 	public final static String kEast = "east";
 	public final static String kNorth = "north";
 	public final static String kSouth = "south";
 	public final static String kWest = "west";
 	
 	private final static String kYes = "yes";
 	private final static String kNo = "no";
 	
 	private final static String kOff = "off";
 	private final static String kOn = "on";
 	
 	private final static String kNone = "none";
 
 	private Identifier m_InputLink;
 	private Identifier m_BlockedWME;
 	private StringElement m_BlockedBackwardWME;
 	private StringElement m_BlockedForwardWME;
 	private StringElement m_BlockedLeftWME;
 	private StringElement m_BlockedRightWME;
 	private IntElement m_ClockWME;
 	private Identifier m_CurrentScoreWME;
 	
 	private IntElement m_BlueScore;
 	private IntElement m_RedScore;
 	private IntElement m_YellowScore;
 	private IntElement m_GreenScore;
 	private IntElement m_PurpleScore;
 	private IntElement m_OrangeScore;
 	private IntElement m_BlackScore;
 	
 	private StringElement m_DirectionWME;
 	private IntElement m_EnergyWME;
 	private StringElement m_EnergyRechargerWME;
 	private IntElement m_HealthWME;
 	private StringElement m_HealthRechargerWME;
 	private Identifier m_IncomingWME;
 	private StringElement m_IncomingBackwardWME;
 	private StringElement m_IncomingForwardWME;
 	private StringElement m_IncomingLeftWME;
 	private StringElement m_IncomingRightWME;
 	private IntElement m_MissilesWME;
 	private StringElement m_MyColorWME;
 	private StringElement m_RadarStatusWME;
 	private IntElement m_RadarDistanceWME;
 	private IntElement m_RadarSettingWME;
 	private Identifier m_RadarWME;
 	private FloatElement m_RandomWME;
 	private StringElement m_ResurrectWME;
 	private Identifier m_RWavesWME;
 	private StringElement m_RWavesBackwardWME;
 	private StringElement m_RWavesForwardWME;
 	private StringElement m_RWavesLeftWME;
 	private StringElement m_RWavesRightWME;
 	private StringElement m_ShieldStatusWME;
 	private Identifier m_SmellWME;
 	private StringElement m_SmellColorWME;
 	private IntElement m_SmellDistanceWME;
 	private StringElement m_SmellDistanceStringWME;
 	private StringElement m_SoundWME;
 	private IntElement m_xWME;
 	private IntElement m_yWME;			
 
	private static Logger logger = Logger.getLogger("simulation");
 	
 	private Identifier[][] radarCellIDs = new Identifier[Tank.kRadarWidth][Tank.kRadarHeight];
 	private StringElement[][] radarColors = new StringElement[Tank.kRadarWidth][Tank.kRadarHeight];
 	
 	private Agent m_Agent;
 	private Tank m_Tank;
 	private TankSoarWorld m_World;
 	private boolean m_Reset = true;
 	private int m_ResurrectFrame = 0;
 
 	public InputLinkManager(TankSoarWorld world, Tank tank) {
 		m_Agent = tank.getAgent();
 		m_World = world;
 		m_Tank = tank;
 		m_InputLink = m_Agent.GetInputLink();
 	}
 	
 	private void DestroyWME(WMElement wme) {
 		assert wme != null;
 		m_Agent.DestroyWME(wme);
 	}
 
 	private void Update(StringElement wme, String value) {
 		assert wme != null;
 		assert value != null;
 		m_Agent.Update(wme, value);
 	}
 
 	private void Update(IntElement wme, int value) {
 		assert wme != null;
 		m_Agent.Update(wme, value);
 	}
 
 	private void Update(FloatElement wme, float value) {
 		assert wme != null;
 		m_Agent.Update(wme, value);
 	}
 	
 	private IntElement CreateIntWME(Identifier id, String attribute, int value) {
 		assert id != null;
 		assert attribute != null;
 		return m_Agent.CreateIntWME(id, attribute, value);
 	}
 
 	private StringElement CreateStringWME(Identifier id, String attribute, String value) {
 		assert id != null;
 		assert attribute != null;
 		assert value != null;
 		return m_Agent.CreateStringWME(id, attribute, value);
 	}
 
 	private FloatElement CreateFloatWME(Identifier id, String attribute, float value) {
 		assert id != null;
 		assert attribute != null;
 		return m_Agent.CreateFloatWME(id, attribute, value);
 	}
 
 	public void clear() {
 		if (m_Reset == true) {
 			return;
 		}
 		
 		DestroyWME(m_BlockedWME);
 		m_BlockedWME = null;
 		
 		DestroyWME(m_ClockWME);
 		m_ClockWME = null;
 		
 		DestroyWME(m_CurrentScoreWME);
 		m_CurrentScoreWME = null;
 		m_BlueScore = null;
 		m_RedScore = null;
 		m_YellowScore = null;
 		m_GreenScore = null;
 		m_PurpleScore = null;
 		m_OrangeScore = null;
 		m_BlackScore = null;
 
 		DestroyWME(m_DirectionWME);
 		m_DirectionWME = null;
 		
 		DestroyWME(m_EnergyWME);
 		m_EnergyWME = null;
 		
 		DestroyWME(m_EnergyRechargerWME);
 		m_EnergyRechargerWME = null;
 		
 		DestroyWME(m_HealthWME);
 		m_HealthWME = null;
 		
 		DestroyWME(m_HealthRechargerWME);
 		m_HealthRechargerWME = null;
 		
 		DestroyWME(m_IncomingWME);
 		m_IncomingWME = null;
 		
 		DestroyWME(m_MissilesWME);
 		m_MissilesWME = null;
 		
 		DestroyWME(m_MyColorWME);
 		m_MyColorWME = null;
 		
 		DestroyWME(m_RadarStatusWME);
 		m_RadarStatusWME = null;
 		
 		DestroyWME(m_RadarDistanceWME);
 		m_RadarDistanceWME = null;
 		
 		DestroyWME(m_RadarSettingWME);
 		m_RadarSettingWME = null;
 		
 		if (m_RadarWME != null) {
 			DestroyWME(m_RadarWME);
 			m_RadarWME = null;
 		}
 		DestroyWME(m_RandomWME);
 		m_RandomWME = null;
 		
 		DestroyWME(m_ResurrectWME);
 		m_ResurrectWME = null;
 		
 		DestroyWME(m_RWavesWME);
 		m_RWavesWME = null;
 		
 		DestroyWME(m_ShieldStatusWME);
 		m_ShieldStatusWME = null;
 		
 		DestroyWME(m_SmellWME);
 		m_SmellWME = null;
 		
 		DestroyWME(m_SoundWME);
 		m_SoundWME = null;
 		
 		DestroyWME(m_xWME);
 		m_xWME = null;
 		
 		DestroyWME(m_yWME);
 		m_yWME = null;
 		
 		m_Agent.Commit();
 
 		clearRadar();
 
 		m_Reset = true;
 	}
 	
 	void initScoreWMEs() {
 		if (m_CurrentScoreWME == null) {
 			return;
 		}
 		
 		boolean blueSeen = false;
 		boolean redSeen = false;
 		boolean yellowSeen = false;
 		boolean greenSeen = false;
 		boolean purpleSeen = false;
 		boolean orangeSeen = false;
 		boolean blackSeen = false;
 		
 		Tank[] tanks = m_World.getTanks();
 		for (int i = 0; i < tanks.length; ++i) {
 			if (tanks[i].getColor().equals("blue")) {
 				blueSeen = true;
 				if (m_BlueScore == null) {
 					m_BlueScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "blue", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("red")) {
 				redSeen = true;
 				if (m_RedScore == null) {
 					m_RedScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "red", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("yellow")) {
 				yellowSeen = true;
 				if (m_YellowScore == null) {
 					m_YellowScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "yellow", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("green")) {
 				greenSeen = true;
 				if (m_GreenScore == null) {
 					m_GreenScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "green", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("purple")) {
 				purpleSeen = true;
 				if (m_PurpleScore == null) {
 					m_PurpleScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "purple", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("orange")) {
 				orangeSeen = true;
 				if (m_OrangeScore == null) {
 					m_OrangeScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "orange", tanks[i].getPoints());
 				}
 			} else if (tanks[i].getColor().equals("black")) {
 				blackSeen = true;
 				if (m_BlackScore == null) {
 					m_BlackScore = m_Agent.CreateIntWME(m_CurrentScoreWME, "black", tanks[i].getPoints());
 				}
 			}
 		}
 		
 		if (blueSeen == false) {
 			if (m_BlueScore != null) {
 				DestroyWME(m_BlueScore);
 				m_BlueScore = null;
 			}
 		}
 		if (redSeen == false) {
 			if (m_RedScore != null) {
 				DestroyWME(m_RedScore);
 				m_RedScore = null;
 			}
 		}
 		if (yellowSeen == false) {
 			if (m_YellowScore != null) {
 				DestroyWME(m_YellowScore);
 				m_YellowScore = null;
 			}
 		}
 		if (greenSeen == false) {
 			if (m_GreenScore != null) {
 				DestroyWME(m_GreenScore);
 				m_GreenScore = null;
 			}
 		}
 		if (purpleSeen == false) {
 			if (m_PurpleScore != null) {
 				DestroyWME(m_PurpleScore);
 				m_PurpleScore = null;
 			}
 		}
 		if (orangeSeen == false) {
 			if (m_OrangeScore != null) {
 				DestroyWME(m_OrangeScore);
 				m_OrangeScore = null;
 			}
 		}
 		if (blackSeen == false) {
 			if (m_BlackScore != null) {
 				DestroyWME(m_BlackScore);
 				m_BlackScore = null;
 			}
 		}
 	}
 	
 	void write() {
 		java.awt.Point location = m_Tank.getLocation();
 		TankSoarCell cell = m_World.getCell(location);
 		
 		String energyRecharger = cell.isEnergyRecharger() ? kYes : kNo;
 		String healthRecharger = cell.isHealthRecharger() ? kYes : kNo;
 
 		if (m_Reset) {
 			m_EnergyRechargerWME = CreateStringWME(m_InputLink, kEnergyRechargerID, energyRecharger);
 			m_HealthRechargerWME = CreateStringWME(m_InputLink, kHealthRechargerID, healthRecharger);
 
 			m_xWME = CreateIntWME(m_InputLink, kXID, location.x);
 			m_yWME = CreateIntWME(m_InputLink, kYID, location.y);
 			
 		} else {
 			if (m_Tank.recentlyMoved()) {
 				Update(m_EnergyRechargerWME, energyRecharger);
 				Update(m_HealthRechargerWME, healthRecharger);
 				
 				Update(m_xWME, location.x);
 				Update(m_yWME, location.y);
 			}
 		}
 
 		int currentEnergy = m_Tank.getEnergy();
 		int currentHealth = m_Tank.getHealth();
 		if (m_Reset) {
 			m_EnergyWME = CreateIntWME(m_InputLink, kEnergyID, currentEnergy);
 			m_HealthWME = CreateIntWME(m_InputLink, kHealthID, currentHealth);
 		} else {
 			if (m_EnergyWME.GetValue() != currentEnergy) {
 				Update(m_EnergyWME, currentEnergy);
 			}
 			if (m_HealthWME.GetValue() != currentHealth) {
 				Update(m_HealthWME, currentHealth);
 			}			
 		}
 
 		String shieldStatus = m_Tank.getShieldStatus() ? kOn : kOff;
 		if (m_Reset) {
 			m_ShieldStatusWME = CreateStringWME(m_InputLink, kShieldStatusID, shieldStatus);
 		} else {
 			if (!m_ShieldStatusWME.GetValue().equalsIgnoreCase(shieldStatus)) {
 				Update(m_ShieldStatusWME, shieldStatus);
 			}
 		}
 		
 		String facing = Direction.stringOf[m_Tank.getFacingInt()];
 		if (m_Reset) {
 			m_DirectionWME = CreateStringWME(m_InputLink, kDirectionID, facing);
 		} else {
 			if (!m_DirectionWME.GetValue().equalsIgnoreCase(facing)) {
 				Update(m_DirectionWME, facing);
 			}
 		}
 				
 		int blocked = m_World.getBlockedByLocation(m_Tank);
 		
 		String blockedForward = ((blocked & Direction.indicators[m_Tank.getFacingInt()]) > 0) ? kYes : kNo;
 		String blockedBackward = ((blocked & Direction.indicators[Direction.backwardOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		String blockedLeft = ((blocked & Direction.indicators[Direction.leftOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		String blockedRight = ((blocked & Direction.indicators[Direction.rightOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		
 		if (m_Reset) {
 			m_BlockedWME = m_Agent.CreateIdWME(m_InputLink, kBlockedID);
 			m_BlockedForwardWME = CreateStringWME(m_BlockedWME, kForwardID, blockedForward);
 			m_BlockedBackwardWME = CreateStringWME(m_BlockedWME, kBackwardID, blockedBackward);
 			m_BlockedLeftWME = CreateStringWME(m_BlockedWME, kLeftID, blockedLeft);
 			m_BlockedRightWME = CreateStringWME(m_BlockedWME, kRightID, blockedRight);				
 		} else {
 			if (m_Tank.recentlyMovedOrRotated() || !m_BlockedForwardWME.GetValue().equalsIgnoreCase(blockedForward)) {
 				Update(m_BlockedForwardWME, blockedForward);
 			}
 			if (m_Tank.recentlyMovedOrRotated() || !m_BlockedBackwardWME.GetValue().equalsIgnoreCase(blockedBackward)) {
 				Update(m_BlockedBackwardWME, blockedBackward);
 			}
 			if (m_Tank.recentlyMovedOrRotated() || !m_BlockedLeftWME.GetValue().equalsIgnoreCase(blockedLeft)) {
 				Update(m_BlockedLeftWME, blockedLeft);
 			}
 			if (m_Tank.recentlyMovedOrRotated() || !m_BlockedRightWME.GetValue().equalsIgnoreCase(blockedRight)) {
 				Update(m_BlockedRightWME, blockedRight);
 			}
 		}
 		
 		
 		if (m_Reset) {
 			m_CurrentScoreWME = m_Agent.CreateIdWME(m_InputLink, kCurrentScoreID);
 			initScoreWMEs();
 		} else {
 			Tank[] tanks = m_World.getTanks();
 			for (int i = 0; i < tanks.length; ++i) {
 				if (tanks[i].pointsChanged()) {
 					if (tanks[i].getColor().equals("blue")) {
 						Update(m_BlueScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("red")) {
 						Update(m_RedScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("yellow")) {
 						Update(m_YellowScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("green")) {
 						Update(m_GreenScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("purple")) {
 						Update(m_PurpleScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("orange")) {
 						Update(m_OrangeScore, tanks[i].getPoints());
 					} else if (tanks[i].getColor().equals("black")) {
 						Update(m_BlackScore, tanks[i].getPoints());
 					}
 				}
 			}
 		}
 		
 		int incoming = m_World.getIncomingByLocation(location);
 		String incomingForward = ((incoming & Direction.indicators[m_Tank.getFacingInt()]) > 0) ? kYes : kNo;
 		String incomingBackward = ((incoming & Direction.indicators[Direction.backwardOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		String incomingLeft = ((incoming & Direction.indicators[Direction.leftOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		String incomingRight = ((incoming & Direction.indicators[Direction.rightOf[m_Tank.getFacingInt()]]) > 0) ? kYes : kNo;
 		
 		if (m_Reset) {
 			m_IncomingWME = m_Agent.CreateIdWME(m_InputLink, kIncomingID);
 			m_IncomingBackwardWME = CreateStringWME(m_IncomingWME, kBackwardID, incomingForward);
 			m_IncomingForwardWME = CreateStringWME(m_IncomingWME, kForwardID, incomingBackward);
 			m_IncomingLeftWME = CreateStringWME(m_IncomingWME, kLeftID, incomingLeft);
 			m_IncomingRightWME = CreateStringWME(m_IncomingWME, kRightID, incomingRight);
 			
 		} else {
 			if (!m_IncomingForwardWME.GetValue().equalsIgnoreCase(incomingForward)) {
 				Update(m_IncomingForwardWME, incomingForward);
 			}
 			if (!m_IncomingBackwardWME.GetValue().equalsIgnoreCase(incomingBackward)) {
 				Update(m_IncomingBackwardWME, incomingBackward);
 			}
 			if (!m_IncomingLeftWME.GetValue().equalsIgnoreCase(incomingLeft)) {
 				Update(m_IncomingLeftWME, incomingLeft);
 			}
 			if (!m_IncomingRightWME.GetValue().equalsIgnoreCase(incomingRight)) {
 				Update(m_IncomingRightWME, incomingRight);
 			}
 		}
 
 		// Smell
 		Tank closestTank = m_World.getStinkyTankNear(m_Tank);
 		String closestTankColor = (closestTank == null) ? kNone : closestTank.getColor();
 		int distance = (closestTank == null) ? 0 : m_World.getManhattanDistanceTo(location, closestTank.getLocation());
 		m_Tank.setSmell(distance, distance == 0 ? null : closestTankColor);
 		if (m_Reset) {
 			m_SmellWME = m_Agent.CreateIdWME(m_InputLink, kSmellID);
 			m_SmellColorWME = CreateStringWME(m_SmellWME, kColorID, closestTankColor);
 			if (closestTank == null) {
 				m_SmellDistanceWME = null;
 				m_SmellDistanceStringWME = CreateStringWME(m_SmellWME, kDistanceID, kNone);
 			} else {
 				m_SmellDistanceWME = CreateIntWME(m_SmellWME, kDistanceID, distance);
 				m_SmellDistanceStringWME = null;
 			}
 		} else {
 			if (!m_SmellColorWME.GetValue().equalsIgnoreCase(closestTankColor)) {
 				Update(m_SmellColorWME, closestTankColor);
 			}
 			if (closestTank == null) {
 				if (m_SmellDistanceWME != null) {
 					DestroyWME(m_SmellDistanceWME);
 					m_SmellDistanceWME = null;
 				}
 				if (m_SmellDistanceStringWME == null) {
 					m_SmellDistanceStringWME = CreateStringWME(m_SmellWME, kDistanceID, kNone);
 				}
 			} else {
 				if (m_SmellDistanceWME == null) {
 					m_SmellDistanceWME = CreateIntWME(m_SmellWME, kDistanceID, distance);
 				} else {
 					if (m_SmellDistanceWME.GetValue() != distance) {
 						Update(m_SmellDistanceWME, distance);
 					}
 				}
 				if (m_SmellDistanceStringWME != null) {
 					DestroyWME(m_SmellDistanceStringWME);
 					m_SmellDistanceStringWME = null;
 				}
 			}
 		}
 
 		// Sound
 		// if the closest tank is greater than 7 away, there is no
 		// possibility of hearing anything
 		int sound = 0;
 		if ((closestTank != null) && m_World.getManhattanDistanceTo(closestTank.getLocation(), m_Tank.getLocation()) <= TankSoarWorld.kMaxSmellDistance) {
 			sound = m_World.getSoundNear(m_Tank);
 		}
 		String soundString;
 		if (sound == m_Tank.getFacingInt()) {
 			soundString = kForwardID;
 		} else if (sound == Direction.backwardOf[m_Tank.getFacingInt()]) {
 			soundString = kBackwardID;
 		} else if (sound == Direction.leftOf[m_Tank.getFacingInt()]) {
 			soundString = kLeftID;
 		} else if (sound == Direction.rightOf[m_Tank.getFacingInt()]) {
 			soundString = kRightID;
 		} else {
 			soundString = kSilentID;
 		}
 		if (m_Reset) {
 			m_SoundWME = CreateStringWME(m_InputLink, kSoundID, soundString);			
 		} else {
 			if (!m_SoundWME.GetValue().equalsIgnoreCase(soundString)) {
 				Update(m_SoundWME, soundString);
 			}
 		}
 		
 		// Missiles
 		int missiles = m_Tank.getMissiles();
 		if (m_Reset) {
 			m_MissilesWME = CreateIntWME(m_InputLink, kMissilesID, missiles);
 		} else {
 			if (m_MissilesWME.GetValue() != missiles) {
 				Update(m_MissilesWME, missiles);
 			}
 		}
 		
 		// Color
 		if (m_Reset) {
 			m_MyColorWME = CreateStringWME(m_InputLink, kMyColorID, m_Tank.getColor());
 		}
 		
 		int worldCount = m_World.getWorldCount();
 		if (m_Reset) {
 			m_ClockWME = CreateIntWME(m_InputLink, kClockID, worldCount);
 		} else {
 			Update(m_ClockWME, worldCount);
 		}
 		
 		// Resurrect
 		if (m_Reset) {
 			m_ResurrectFrame = worldCount;
 			m_ResurrectWME = CreateStringWME(m_InputLink, kResurrectID, kYes);
 		} else {
 			if (worldCount != m_ResurrectFrame) {
 				if (!m_ResurrectWME.GetValue().equalsIgnoreCase(kNo)) {
 					Update(m_ResurrectWME, kNo);
 				}
 			}
 		}
 		
 		// Radar
 		String radarStatus = m_Tank.getRadarStatus() ? kOn : kOff;
 		if (m_Reset) {
 			m_RadarStatusWME = CreateStringWME(m_InputLink, kRadarStatusID, radarStatus);
 			if (m_Tank.getRadarStatus()) {
 				m_RadarWME = m_Agent.CreateIdWME(m_InputLink, kRadarID);
 				generateNewRadar();
 			} else {
 				m_RadarWME = null;
 			}
 			m_RadarDistanceWME = CreateIntWME(m_InputLink, kRadarDistanceID, m_Tank.getRadarDistance());
 			m_RadarSettingWME = CreateIntWME(m_InputLink, kRadarSettingID, m_Tank.getRadarSetting());
 			
 		} else {
 			if (!m_RadarStatusWME.GetValue().equalsIgnoreCase(radarStatus)) {
 				Update(m_RadarStatusWME, radarStatus);
 			}
 			if (m_Tank.getRadarStatus()) {
 				if (m_RadarWME == null) {
 					m_RadarWME = m_Agent.CreateIdWME(m_InputLink, kRadarID);
 					generateNewRadar();
 				} else {
 					updateRadar();
 				}
 			} else {
 				if (m_RadarWME != null) {
 					DestroyWME(m_RadarWME);
 					m_RadarWME = null;
 					clearRadar();
 				}
 			}
 			if (m_RadarDistanceWME.GetValue() != m_Tank.getRadarDistance()) {
 				Update(m_RadarDistanceWME, m_Tank.getRadarDistance());
 			}
 			if (m_RadarSettingWME.GetValue() != m_Tank.getRadarSetting()) {
 				Update(m_RadarSettingWME, m_Tank.getRadarSetting());
 			}
 		}
 		
 		// Random
		float random = Simulation.random.nextFloat();
 		if (m_Reset) {
 			m_RandomWME = CreateFloatWME(m_InputLink, kRandomID, random);
 		} else {
 			Update(m_RandomWME, random);
 		}
 
 		// RWaves
 		int rwaves = m_Tank.getRWaves();
 		String rwavesForward = (rwaves & m_Tank.getFacingInt()) > 0 ? kYes : kNo;
 		String rwavesBackward = (rwaves & Direction.indicators[Direction.backwardOf[m_Tank.getFacingInt()]]) > 0 ? kYes : kNo;;
 		String rwavesLeft = (rwaves & Direction.indicators[Direction.leftOf[m_Tank.getFacingInt()]]) > 0 ? kYes : kNo;
 		String rwavesRight = (rwaves & Direction.indicators[Direction.rightOf[m_Tank.getFacingInt()]]) > 0 ? kYes : kNo;
 		
 		if (m_Reset) {
 			m_RWavesWME = m_Agent.CreateIdWME(m_InputLink, kRWavesID);
 			m_RWavesForwardWME = CreateStringWME(m_RWavesWME, kForwardID, rwavesBackward);
 			m_RWavesBackwardWME = CreateStringWME(m_RWavesWME, kBackwardID, rwavesForward);
 			m_RWavesLeftWME = CreateStringWME(m_RWavesWME, kLeftID, rwavesLeft);
 			m_RWavesRightWME = CreateStringWME(m_RWavesWME, kRightID, rwavesRight);
 		} else {
 			if (!m_RWavesForwardWME.GetValue().equalsIgnoreCase(rwavesForward)) {
 				Update(m_RWavesForwardWME, rwavesForward);
 			}
 			if (!m_RWavesBackwardWME.GetValue().equalsIgnoreCase(rwavesBackward)) {
 				Update(m_RWavesBackwardWME, rwavesBackward);
 			}
 			if (!m_RWavesLeftWME.GetValue().equalsIgnoreCase(rwavesLeft)) {
 				Update(m_RWavesLeftWME, rwavesLeft);
 			}
 			if (!m_RWavesRightWME.GetValue().equalsIgnoreCase(rwavesRight)) {
 				Update(m_RWavesRightWME, rwavesRight);
 			}
 			
 		}	
 		
 		m_Reset = false;
 		m_Agent.Commit();
 	}
 	
 	private void generateNewRadar() {
 //		if (logger.isLoggable(Level.FINEST)) logger.finest("generateNewRadar()");
 		TankSoarCell[][] radarCells = m_Tank.getRadarCells();
 		for (int j = 0; j < Tank.kRadarHeight; ++j) {
 			boolean done = false;
 //			String outstring = new String();
 			for (int i = 0; i < Tank.kRadarWidth; ++i) {
 				// Always skip self, this screws up the tanks.
 				if (i == 1 && j == 0) {
 //					outstring += "s";
 					continue;
 				}
 				if (radarCells[i][j] == null) {
 					// if center is null, we're done
 					if (i == 1) {
 //						outstring += "d";
 						done = true;
 						break;
 //					} else {
 //						outstring += ".";
 					}
 				} else {
 					// Create a new WME
 					radarCellIDs[i][j] = m_Agent.CreateIdWME(m_RadarWME, getCellID(radarCells[i][j]));
 					CreateIntWME(radarCellIDs[i][j], kDistanceID, j);
 					CreateStringWME(radarCellIDs[i][j], kPositionID, getPositionID(i));
 					if (radarCells[i][j].containsTank()) {
 						radarColors[i][j] = CreateStringWME(radarCellIDs[i][j], kColorID, radarCells[i][j].getTank().getColor());
 //						outstring += "t";
 //					} else {
 //						outstring += "n";
 					}
 				}
 			}
 //			if (logger.isLoggable(Level.FINEST)) logger.finest(outstring);
 			if (done == true) {
 				break;
 			}
 		}
 	}
 	
 	private void updateRadar() {
 //		if (logger.isLoggable(Level.FINEST)) logger.finest("updateRadar()");
 		TankSoarCell[][] radarCells = m_Tank.getRadarCells();
 		for (int i = 0; i < Tank.kRadarWidth; ++i) {
 //			String outstring = new String();
 			for (int j = 0; j < Tank.kRadarHeight; ++j) {
 				// Always skip self, this screws up the tanks.
 				if (i == 1 && j == 0) {
 //					outstring += "s";
 					continue;
 				}
 				if (radarCells[i][j] == null) {
 					// Unconditionally delete the WME
 					if (radarCellIDs[i][j] != null) {
 //						outstring += "d";
 						DestroyWME(radarCellIDs[i][j]);
 						radarCellIDs[i][j] = null;
 						radarColors[i][j] = null;
 //					} else {
 //						outstring += "-";
 					}
 					
 				} else {
 					
 					if (radarCellIDs[i][j] == null) {
 						// Unconditionally create the WME
 						radarCellIDs[i][j] = m_Agent.CreateIdWME(m_RadarWME, getCellID(radarCells[i][j]));
 						CreateIntWME(radarCellIDs[i][j], kDistanceID, j);
 						CreateStringWME(radarCellIDs[i][j], kPositionID, getPositionID(i));
 						if (radarCells[i][j].containsTank()) {
 							radarColors[i][j] = CreateStringWME(radarCellIDs[i][j], kColorID, radarCells[i][j].getTank().getColor());
 //							outstring += "t";
 //						} else {
 //							outstring += "n";
 						}
 					} else {
 						// Update if relevant change
 						if (m_Tank.recentlyMovedOrRotated() || radarCells[i][j].isModified()) {
 							DestroyWME(radarCellIDs[i][j]);
 							radarCellIDs[i][j] = m_Agent.CreateIdWME(m_RadarWME, getCellID(radarCells[i][j]));
 							CreateIntWME(radarCellIDs[i][j], kDistanceID, j);
 							CreateStringWME(radarCellIDs[i][j], kPositionID, getPositionID(i));
 							if (radarCells[i][j].containsTank()) {
 								// rwaves already set
 								//tank.setRWaves(m_Tank.backward());
 								radarColors[i][j] = CreateStringWME(radarCellIDs[i][j], kColorID, radarCells[i][j].getTank().getColor());
 //								outstring += "U";
 //							} else {
 //								outstring += "u";								
 							}
 //						} else {
 //							outstring += ".";							
 						}
 					}
 				}
 			}
 //			if (logger.isLoggable(Level.FINEST)) logger.finest(outstring);
 		}
 	}
 
 	private void clearRadar() {
 //		logger.finest("clearRadar()");
 		for (int i = 0; i < Tank.kRadarWidth; ++i) {
 			for (int j = 0; j < Tank.kRadarHeight; ++j) {
 				radarCellIDs[i][j] = null;
 				radarColors[i][j] = null;
 			}
 		}
 	}
 	
 	private String getCellID(TankSoarCell cell) {
 		if (cell.containsTank()) {
 			return kTankID;
 		}
 		if (cell.isWall()) {
 			return kObstacleID;
 		}
 		if (cell.isEnergyRecharger()) {
 			return kEnergyID;
 		}
 		if (cell.isHealthRecharger()) {
 			return kHealthID;
 		}
 		if (cell.containsMissilePack()) {
 			return kMissilesID;
 		}
 		return kOpenID;
 	}
 	
 	public String getPositionID(int i) {
 		switch (i) {
 		case Tank.kRadarLeft:
 			return InputLinkManager.kLeftID;
 		default:
 		case Tank.kRadarCenter:
 			return InputLinkManager.kCenterID;
 		case Tank.kRadarRight:
 			return InputLinkManager.kRightID;
 		}
 	}
 }
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
