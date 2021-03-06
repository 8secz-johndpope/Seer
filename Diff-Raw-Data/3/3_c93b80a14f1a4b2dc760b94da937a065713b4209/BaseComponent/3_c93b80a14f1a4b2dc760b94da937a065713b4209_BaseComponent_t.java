 package battlecode.world;
 
 import battlecode.common.ComponentClass;
 import battlecode.common.ComponentController;
 import battlecode.common.ComponentType;
 import battlecode.common.Direction;
 import battlecode.common.GameActionException;
 import battlecode.common.GameObject;
 import battlecode.common.MapLocation;
 import battlecode.common.Robot;
 import battlecode.common.RobotLevel;
 import battlecode.engine.signal.Signal;
 
 import static battlecode.common.GameActionExceptionType.*;
 
 import com.google.common.base.Predicate;
 
 public class BaseComponent extends ControllerShared implements ComponentController {
 
     protected ComponentType type;
     protected int roundsUntilIdle;
     protected RobotControllerImpl rc;
 
     public boolean isActive() {
         return roundsUntilIdle > 0;
     }
 
     public int roundsUntilIdle() {
         return roundsUntilIdle;
     }
 
     public void processBeginningOfTurn() {
     }
 
     public void processEndOfTurn() {
         if (roundsUntilIdle > 0)
             roundsUntilIdle--;
     }
 
     public void activate() {
         activate(type.delay);
     }
 
     public void activate(int rounds) {
         if (roundsUntilIdle < rounds)
             roundsUntilIdle = rounds;
     }
 
     public void activate(Signal action) {
         activate();
         robot.addAction(action);
     }
 
     public void activate(Signal action, int rounds) {
         activate(rounds);
         robot.addAction(action);
     }
 
     public ComponentType type() {
         return type;
     }
 
     public ComponentClass componentClass() {
         return type.componentClass;
     }
 
     public RobotControllerImpl getRC() {
         return rc;
     }
 
     /*
     public void unequip() {
     // add unequip signal
     }
      */
     protected Predicate<InternalObject> objectWithinRangePredicate() {
         return new Predicate<InternalObject>() {
 
             public boolean apply(InternalObject o) {
                 return checkWithinRange(o);
             }
         };
     }
 
     protected Predicate<MapLocation> locWithinRangePredicate() {
         return new Predicate<MapLocation>() {
 
             public boolean apply(MapLocation o) {
                 return checkWithinRange(o);
             }
         };
     }
 
     // This was written under the assumption that components could be
     // unequipped.  It can be removed once all calls to it have been
     // removed.
     @Deprecated
     protected void assertEquipped() {
         //if(component.getController()!=this)
         //	throw new IllegalStateException("You no longer control this component.");
     }
 
     public boolean withinRange(MapLocation loc) {
         assertEquipped();
         return checkWithinRange(loc);
     }
 
     protected boolean checkWithinRange(MapLocation loc) {
         if (getLocation().distanceSquaredTo(loc) > type.range)
             return false;
         return GameWorld.inAngleRange(getLocation(), getDirection(),
                 loc, type.cosHalfAngle);
     }
 
     protected boolean checkWithinRange(InternalObject obj) {
         if (!obj.exists())
             return false;
         if (obj.container() == robot)
             return true;
         return checkWithinRange(obj.getLocation());
     }
 
     protected void assertInactive() throws GameActionException {
         if (roundsUntilIdle() > 0)
             throw new GameActionException(ALREADY_ACTIVE, "This component is already active.");
     }
 
     protected void assertValidDirection(Direction d) {
         assertNotNull(d);
         if (d == Direction.NONE || d == Direction.OMNI)
             throw new IllegalArgumentException("You cannot move in the direction NONE or OMNI.");
     }
 
     protected void assertWithinRange(MapLocation loc) throws GameActionException {
         if (!checkWithinRange(loc))
             outOfRange();
     }
 
     protected void assertWithinRange(InternalObject obj) throws GameActionException {
        if (!checkWithinRange(obj))
			outOfRange();
     }
 
     protected InternalRobot alliedRobotAt(MapLocation loc, RobotLevel height) throws GameActionException {
         InternalRobot ir = gameWorld.getRobot(loc, height);
         if (ir == null || ir.getTeam() != robot.getTeam())
             throw new GameActionException(NO_ROBOT_THERE, "There is no allied robot there.");
         return ir;
     }
 
     protected void spendResources(double amount) throws GameActionException {
         if (!gameWorld.spendResources(robot.getTeam(), amount))
             throw new GameActionException(NOT_ENOUGH_RESOURCES, "You do not have enough resources for that.");
     }
 
     public MapLocation getLocation() {
         return rc.getLocation();
     }
 
     public Direction getDirection() {
         return rc.getDirection();
     }
 
     protected BaseComponent(ComponentType type, InternalRobot robot) {
         super(robot.getGameWorld(), robot);
         this.type = type;
         rc = robot.getRC();
     }
 }
