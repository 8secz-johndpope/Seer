 package eaters;
 
 import java.util.logging.*;
 
 import simulation.*;
 import sml.*;
 import utilities.*;
 
 public class Eater extends WorldEntity {
 	private static Logger logger = Logger.getLogger("simulation");
 	
 	public final static int kEaterVision = 2;
 	
 	public final static String kEaterID = "eater";
 	public final static String kDirectionID = "direction";
 	public final static String kNameID = "name";
 	public final static String kScoreID = "score";
 	public final static String kxID = "x";
 	public final static String kyID = "y";
 	public final static String kMyLocationID = "my-location";
 	public final static String kContentID = "content";
 	public final static String kMoveID = "move";
 	public final static String kJumpID = "jump";
 	public final static String kDontEatID = "dont-eat";
 	public final static String kTrue = "true";
 	public final static String kEater = "eater";
	public final static String kRandomID = "random";
 
 	private StringElement m_DirectionWME;
 	private IntElement m_ScoreWME;
 	private IntElement m_xWME;
 	private IntElement m_yWME;
	private FloatElement m_RandomWME;
 	private SoarCell[][] m_Cells = new SoarCell[(kEaterVision * 2 ) + 1][(kEaterVision * 2 ) + 1];
 	private boolean m_Hungry = true;
 	private boolean m_Moved = true;
	private float random = 0;
 
 	class SoarCell {
 		Identifier me;
 		StringElement content;
 
 		Identifier north;
 		Identifier south;
 		Identifier east;
 		Identifier west;
 		
 		boolean iterated = false;
 	}
 	
 	public Eater(Agent agent, String productions, String color, java.awt.Point location) {
 		super(agent, productions, color, location);
 
 		Identifier eater = m_Agent.CreateIdWME(m_Agent.GetInputLink(), kEaterID);
 		
 		m_DirectionWME = m_Agent.CreateStringWME(eater, kDirectionID, Direction.kNorthString);
 		m_Agent.CreateStringWME(eater, kNameID, getName());
 		m_ScoreWME = m_Agent.CreateIntWME(eater, kScoreID, getPoints());
 		m_xWME = m_Agent.CreateIntWME(eater, kxID, getLocation().x);
 		m_yWME = m_Agent.CreateIntWME(eater, kyID, getLocation().y);
 		
 		for (int i = 0; i < m_Cells.length; ++i) {
 			for (int j = 0; j < m_Cells.length; ++j) {
 				m_Cells[i][j] = new SoarCell();
 			}
 		}
 		
 		m_Cells[kEaterVision][kEaterVision].me = m_Agent.CreateIdWME(m_Agent.GetInputLink(), kMyLocationID);
 		createView(kEaterVision, kEaterVision);
 		
		m_RandomWME = m_Agent.CreateFloatWME(m_Agent.GetInputLink(), kRandomID, random);
		
 		m_Agent.Commit();
 	}
 
 	void createView(int x, int y) {
 		if (x >= 0 && x <= 4 && y >=0 && y <= 4 && !m_Cells[x][y].iterated) {
 			m_Cells[x][y].iterated = true;
 			m_Cells[x][y].content = m_Agent.CreateStringWME(m_Cells[x][y].me, kContentID, CellType.EMPTY.toString());
 
 			if (x > 0) {
 				if (m_Cells[x - 1][y].me == null)
 					m_Cells[x - 1][y].me = m_Agent.CreateIdWME(m_Cells[x][y].me,"west");
 				else
 					m_Cells[x][y].west = m_Agent.CreateSharedIdWME(m_Cells[x][y].me,"west",m_Cells[x - 1][y].me);
 			}
 			
 			if (x < 4) {
 				if (m_Cells[x + 1][y].me == null)
 					m_Cells[x + 1][y].me = m_Agent.CreateIdWME(m_Cells[x][y].me,"east");
 				else
 					m_Cells[x][y].east = m_Agent.CreateSharedIdWME(m_Cells[x][y].me,"east",m_Cells[x + 1][y].me);
 			}
 			
 			if (y > 0) {
 				if (m_Cells[x][y - 1].me == null)
 					m_Cells[x][y - 1].me = m_Agent.CreateIdWME(m_Cells[x][y].me,"north");
 				else
 					m_Cells[x][y].north = m_Agent.CreateSharedIdWME(m_Cells[x][y].me,"north",m_Cells[x][y - 1].me);
 			}
 			
 			if (y < 4) {
 				if (m_Cells[x][y + 1].me == null)
 					m_Cells[x][y + 1].me = m_Agent.CreateIdWME(m_Cells[x][y].me,"south");
 				else
 					m_Cells[x][y].south = m_Agent.CreateSharedIdWME(m_Cells[x][y].me,"south",m_Cells[x][y + 1].me);
 			}
 			
 			createView(x - 1,y);
 			createView(x + 1,y);
 			createView(x,y - 1);
 			createView(x,y + 1);
 		}	
 	}
 	
 	public boolean isHungry() {
 		return m_Hungry;
 	}
 	
 	public void updateInput(EatersWorld world) {
 		// Anything you want to log about each eater each frame can go here:
 		//logger.info(getName() + " at " + getLocation() + " score " + getPoints());
 		
 
 		int xView, yView;
 		int worldSize = world.getSize();
 		for (int x = 0; x < m_Cells.length; ++x) {
 			xView = x - Eater.kEaterVision + getLocation().x;
 			if (xView < 0) {
 				continue;
 			} else if (xView >= worldSize) {
 				break;
 			}
 			for (int y = 0; y < m_Cells[x].length; ++y) {
 				yView = y - Eater.kEaterVision + getLocation().y;
 				if (yView < 0) {
 					continue;
 				} else if (yView >= worldSize) {
 					break;
 				}
 				EatersCell cell = world.getCell(xView, yView);
 				String content = null;
 				if (cell.getEater() != null) {
 					content = kEater;
 					
 				} else if (cell.getFood() != null) {
 					content = cell.getFood().name();
 			
 				} else {
 					content = cell.getType().name();
 				}
 				if (m_Moved || !m_Cells[x][y].content.GetValue().equalsIgnoreCase(content)) {
 					m_Agent.Update(m_Cells[x][y].content, content);
 				}
 			}
 		}
 
 		if (m_ScoreWME.GetValue() != getPoints()) {
 			m_Agent.Update(m_ScoreWME, getPoints());
 		}
 		
 		if (!m_DirectionWME.GetValue().equalsIgnoreCase(Direction.stringOf[m_FacingInt])) {
 			m_Agent.Update(m_DirectionWME, Direction.stringOf[m_FacingInt]);
 		}
 
 		if (m_Moved) {
 			m_Agent.Update(m_xWME, getLocation().x);
 			m_Agent.Update(m_yWME, getLocation().y);
 		}
 		
		// Random
		float oldrandom = random;
		do {
			random = Simulation.random.nextFloat();
		} while (random == oldrandom);
		m_Agent.Update(m_RandomWME, random);
		
 		m_Agent.Commit();
 
 		m_Moved = false;
 	}
 	
 	public class MoveInfo {
 		String direction;
 		boolean jump;
 	}
 	
 	public MoveInfo getMove() {
 		if (m_Agent.GetNumberCommands() == 0) {
 			if (logger.isLoggable(Level.FINE)) logger.fine(getName() + " issued no command.");
 			return null;
 		}
 		
 		if (m_Agent.GetNumberCommands() > 1) {
 			if (logger.isLoggable(Level.FINE)) logger.fine(getName() + " issued more than one command, using first.");
 		}
 
 		Identifier commandId = m_Agent.GetCommand(0);
 		String commandName = commandId.GetAttribute();
 
 		MoveInfo move = new MoveInfo();
 		if (commandName.equalsIgnoreCase(kMoveID)) {
 			move.jump = false;
 		} else if (commandName.equalsIgnoreCase(kJumpID)) {
 			move.jump = true;
 		} else {
 			logger.warning("Unknown command: " + commandName);
 			return null;
 		}
 		
 		String donteat = commandId.GetParameterValue(kDontEatID);
 		if (donteat == null) {
 			m_Hungry = true;
 		} else {
 			m_Hungry = donteat.equalsIgnoreCase(kTrue) ? false : true;
 		}
 		
 		move.direction = commandId.GetParameterValue(kDirectionID);
 		if (move.direction != null) {
 			m_FacingInt = Direction.getInt(move.direction);
 			commandId.AddStatusComplete();
 			m_Agent.ClearOutputLinkChanges();
 			m_Agent.Commit();
 			return move;
 		}
 		
 		logger.warning("Improperly formatted command: " + kMoveID);
 		return null;
 	}
 	
 	void setMoved() {
 		m_Moved = true;
 	}
 
 }
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
