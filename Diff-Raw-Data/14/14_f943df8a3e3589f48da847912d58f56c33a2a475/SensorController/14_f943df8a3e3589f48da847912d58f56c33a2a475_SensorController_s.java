 package battlecode.common;
 
 public interface SensorController extends ComponentController {
 
     public GameObject senseObjectAtLocation(MapLocation loc, RobotLevel height) throws GameActionException;
 
     /**
      * Sense objects of type <code>type</code> that are within this sensor's range.
      */
     public <T extends GameObject> T[] senseNearbyGameObjects(Class<T> type);
 
     /**
      * Sense the location of the object <code>o</code>
      */
     public MapLocation senseLocationOf(GameObject o) throws GameActionException;
 
     /**
      * Sense the RobotInfo for the robot <code>r</code>.
      */
     public RobotInfo senseRobotInfo(Robot r) throws GameActionException;
 
     public MineInfo senseMineInfo(Mine r) throws GameActionException;
 
     public boolean canSenseObject(GameObject o);
 
     public boolean canSenseSquare(MapLocation loc);
 
 	/**
 	 * Senses the income of the recycler robot {@code r}' s team.
 	 *
 	 * @throws GameActionException if {@code r} is not within range
 	 * @throws GameActionException if {@code r} does not have a RECYCLER component
 	 */
     public double seneseIncome(Robot r) throws GameActionException;
 }
