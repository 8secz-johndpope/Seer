 package se.chalmers.dryleafsoftware.androidrally.model.gameModel;
 
 import java.beans.PropertyChangeListener;
 import java.beans.PropertyChangeSupport;
 import java.util.ArrayList;
 import java.util.List;
 
 import se.chalmers.dryleafsoftware.androidrally.model.cards.Card;
 import se.chalmers.dryleafsoftware.androidrally.model.cards.Deck;
 import se.chalmers.dryleafsoftware.androidrally.model.cards.Move;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.BoardElement;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.CheckPoint;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.ConveyorBelt;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.GameBoard;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.Gears;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.Laser;
 import se.chalmers.dryleafsoftware.androidrally.model.gameBoard.Wrench;
 import se.chalmers.dryleafsoftware.androidrally.model.robots.Robot;
 
 
 /**
  * This is the mainModel for AndroidRally.
  * 
  *
  */
 public class GameModel {
 	private GameBoard gameBoard;
 	private List<Robot> robots;
 	private Deck deck;
 	private List<String> allMoves = new ArrayList<String>();
 	public static final String ROBOT_WON = "robotWon";
 	public static final String ROBOT_LOST = "robotLost";
 	private int robotsPlaying;
 	private boolean isGameOver;
 
 	private static final String testMap = "yxxxxxxx213xxxxxxx16xxyxx12xxxxx213xxxxx26xx78x16xyxx36x36:37xxxx213xxx32xxxxx58:16xyxxxx26x07xx213xxxxx26xxx38xyxxxx26x36xx14x223x223x223x223x223xxx16xyxxxx26xxxxxxxxxxx18:16xyxxxx26xxxxxxxxxxx28:16xyxxxx26x17xx1x103x103x103x103x103xxxxyxxxx26x36xx113xxxxx26xxx48:16xyxxxxxxx113xxx22xxxxx68:16xyxx5xxxxx113xxxxx26xx88:16xxyxxxxxxx113xxxxxxxxx";
 	/**
 	 * Only for testing!!
  	 * @param nbrOfRobots players + bots
 	 */
 	public GameModel(int nbrOfRobots) {
 		this(nbrOfRobots, testMap);
 	}
 
 
 	/**
 	 * Creates a game board of size 12x16 tiles. Also creates robots based
 	 * on the amount of players. Creates a deck with cards that is shuffled.
 	 * @param nbrOfRobots players + bots
 	 * @param map, a map in String format
 	 */
 	public GameModel(int nbrOfRobots, String map) {
 		gameBoard = new GameBoard(map);
 		isGameOver = false;
 		robots = new ArrayList<Robot>();
 		int[][] startingPositions = gameBoard.getStartingPositions();
 		for (int i = 0; i < nbrOfRobots; i++) {
 			robots.add(new Robot(startingPositions[i][0], startingPositions[i][1]));
 		}
 		robotsPlaying = nbrOfRobots;
 		deck = new Deck();
 	}
 
 	/**
 	 * Give cards to all players/CPU:s.
 	 */
 	public void dealCards() {
 		deck.shuffleDeck();
 		for(Robot robot : robots) {
 			int nbrOfDrawnCards = robot.getHealth();
 			List<Card> drawnCards = new ArrayList<Card>();
 			for (int i = 0; i < nbrOfDrawnCards; i++) {
 				drawnCards.add(deck.drawCard());
 			}
 			robot.addCards(drawnCards);
 		}
 	}
 
 	/**
 	 * A method that make board elements "do things" with robots, i.e.
 	 * move robots that are standing on a conveyor belt and so on.
 	 */
 	private void activateBoardElements() {
 		handleConveyorBelts();
 		handleGears();
 		handleImmobileActions();
 	}
 	
 	private void handleConveyorBelts(){
 		int maxTravelDistance = gameBoard.getMaxConveyorBeltDistance();
 		for (Robot robot : robots) {
 			if(!robot.isDead()){
 				gameBoard.getTile(robot.getX(), robot.getY()).instantAction(robot);
 				if(robot.isDead()){
 					addRobotDeadMove(robot);
 				}
 			}
 		}
 		if(checkGameStatus())return;
 
 		int[][] oldPositions = new int[robots.size()][2];
 		for(int i = 0; i<maxTravelDistance; i++){
 			allMoves.add(";B" + "1" + (maxTravelDistance-i));
 			for(int j = 0; j< robots.size(); j++){
 				if (robots.get(j).isDead()) {
 					continue;
 				}
 				oldPositions[j][0] = robots.get(j).getX();
 				oldPositions[j][1] = robots.get(j).getY();
 				List<BoardElement> boardElements = gameBoard.getTile(robots.get(j).getX(), 
 						robots.get(j).getY()).getBoardElements();
 				if(boardElements != null && boardElements.size() > 0){
 					if(boardElements.get(0) instanceof ConveyorBelt){//ConveyorBelt should always be first
 						if(((ConveyorBelt)boardElements.get(0)).getTravelDistance() >= maxTravelDistance-i){
 							boardElements.get(0).action(robots.get(j));
 							addSimultaneousMove(robots.get(j));
 							if(checkGameStatus())return;
 						}
 					}
 				}
 			}
 			checkConveyorBeltCollides(oldPositions);
 			for(Robot robot : robots){
 				if(!robot.isDead()){
 					gameBoard.getTile(robot.getX(), robot.getY()).instantAction(robot);
 					if(robot.isDead()){
 						addRobotDeadMove(robot);
 					}
 				}
 			}
 			if(checkGameStatus())return;
 		}
 	}
 	
 	/*
 	 * Handles all checkPoints reached and repair/damage done during a round.
 	 */
 	private void handleImmobileActions(){
 		int[] oldCheckPointReached = new int[robots.size()];
 		int[] oldRobotHealth = new int[robots.size()];
 		for (int i = 0; i < robots.size(); i++){
 			oldRobotHealth[i] = robots.get(i).getHealth();
 			oldCheckPointReached[i] = robots.get(i).getLastCheckPoint();
 		}
 		fireAllLasers();
 		if (checkGameStatus())return;
 		
 		for(int i = 0; i < robots.size(); i++){
 			if (robots.get(i).isDead()) {
 				continue;
 			}
 			List<BoardElement> boardElements = gameBoard.getTile(robots.get(i).getX(), 
 					robots.get(i).getY()).getBoardElements();
 			if(boardElements != null && boardElements.size() > 0){
 				for(BoardElement boardelement : boardElements){
 					if(boardelement instanceof CheckPoint || boardelement instanceof Wrench){
 						boardelement.action(robots.get(i));
 					}
 				}
 			}
 		}
 		addDamageToAllMoves(oldRobotHealth);
 		addCheckPointReached(oldCheckPointReached);
 	}
 	
 	private void handleGears(){
 	    allMoves.add(";B4");
 	    for(int i = 0; i<robots.size(); i++){
 	    	if (robots.get(i).isDead()) {
 				continue;
 			}
 	    	List<BoardElement> boardElements = gameBoard.getTile(robots.get(i).getX(), 
     				robots.get(i).getY()).getBoardElements();
 	    	if(boardElements != null && boardElements.size() > 0){
 	    		for(BoardElement boardelement : boardElements){
 		    		if(boardelement instanceof Gears){
 		    			boardelement.action(robots.get(i));
 		    			addSimultaneousMove(robots.get(i));
 		    		}
 		    	}
 	    	}
 	    	
 	    }
 	}
 
 	private void addCheckPointReached(int[] oldCheckPoints){
 		allMoves.add(";B6");
 		for(int i = 0; i<robots.size(); i++){
 			if(!robots.get(i).isDead() && robots.get(i).getLastCheckPoint() != oldCheckPoints[i]){
 				allMoves.add("#" + i + ":" + robots.get(i).getLastCheckPoint());
 			}
 		}
 	}
 	
 	private void addDamageToAllMoves(int[] oldRobotHealth){
 		allMoves.add(";B5");
 		for(int i = 0; i<robots.size(); i++){
 			if(robots.get(i).getHealth() != oldRobotHealth[i]){
 				if(robots.get(i).getHealth() == Robot.STARTING_HEALTH){// if damage has changed and health == starting health -> robot has died
 					allMoves.add("#" + i + ":" + "1" + robots.get(i).getLife() + (Robot.STARTING_HEALTH - robots.get(i).getHealth()));
 				}else{
 					allMoves.add("#" + i + ":" + "0" + + robots.get(i).getLife() + (Robot.STARTING_HEALTH - robots.get(i).getHealth()));
 				}
 			}
 		}
 	}
 
 	/**
 	 * Return the map as a String. With subStrings representing
 	 * a tile with it's boardelements.
 	 * @return the map as a String
 	 */
 	public String getMap(){
 		return gameBoard.getMapAsString();
 	}
 
 	private boolean isRobotHit(int x, int y){
 		for(Robot robot : this.robots){
 			if(!robot.isDead() && robot.getX() == x && robot.getY() == y){
 				robot.damage(1);
 				return true;
 			}
 		}
 		return false;
 	}
 
 	/*
 	 * This method will only give proper answers if the robot moves one step in X-axis or Y-axis, not both.
 	 */
 	private boolean canMove(int x, int y, int direction){
 		if(direction == GameBoard.NORTH){
 			if(y >= 0 && !gameBoard.getTile(x, y).getNorthWall()){
 				return true;
 			}
 		}else if(direction == GameBoard.WEST){
 			if(x>= 0 && !gameBoard.getTile(x, y).getWestWall()){
 				return true;
 			}
 		}else if(direction == GameBoard.SOUTH){
 			if(y <= gameBoard.getHeight()-1 && !gameBoard.getTile(x, y).getSouthWall()){
 				return true;
 			}
 		}else if(direction == GameBoard.EAST){
 			if(x <= gameBoard.getWidth()-1 && !gameBoard.getTile(x, y).getEastWall()){
 				return true;
 			}
 		}
 		return false;
 	}
 
 	/*
 	 * This method will only give proper answers if the robot moves in X-axis or Y-axis, not both.
 	 */
 	private boolean canMove(int oldX, int oldY, int x, int y){
 		if(oldY > y){
 			return canMove(oldX,oldY,GameBoard.NORTH);
 		}else if(oldX < x ){
 			return canMove(oldX,oldY,GameBoard.EAST);
 		}else if(oldY < y ){
 			return canMove(oldX,oldY,GameBoard.SOUTH);
 		}else if(oldX > x ){
 			return canMove(oldX,oldY,GameBoard.WEST);
 		}
 		// This should only happen if the robot is standing still.
 		return true;
 	}
 
 	private void fireRobotLaser(int x, int y, int direction){
 		if(canMove(x, y, direction)) {
 			if (direction == GameBoard.NORTH) {
 				fireLaser(x, y-1, direction);
 			} else if (direction == GameBoard.EAST) {
 				fireLaser(x+1, y, direction);
 			} else if (direction == GameBoard.SOUTH) {
 				fireLaser(x, y+1, direction);
 			} else if (direction == GameBoard.WEST) {
 				fireLaser(x-1, y, direction);
 			}
 		}
 	}
 	
 	private void fireLaser(int x, int y, int direction) {
 		boolean robotIsHit = false;
 		boolean isNoWall = true;
 		if(direction == GameBoard.NORTH){
 			while(y >= 0 && !robotIsHit && isNoWall){
 				robotIsHit = isRobotHit(x, y);
 				isNoWall = canMove(x, y, direction);
 				y--;
 			}
 
 		}else if(direction == GameBoard.EAST){
 			while(x < gameBoard.getWidth() && !robotIsHit && isNoWall){
 				robotIsHit = isRobotHit(x, y);
 				isNoWall = canMove(x, y, direction);
 				x++;
 			}
 		}else if(direction == GameBoard.SOUTH){
 			while(y < gameBoard.getHeight() && !robotIsHit && isNoWall){
 				robotIsHit = isRobotHit(x, y);
 				isNoWall = canMove(x, y, direction);
 				y++;
 			}
 		}else if(direction == GameBoard.WEST){
 			while(x >= 0 && !robotIsHit && isNoWall){
 				robotIsHit = isRobotHit(x, y);
 				isNoWall = canMove(x, y, direction);
 				x--;
 			}
 		}
 	}
 
 
 	/**
 	 * Fires all lasers from both robots and lasers attached to walls.
 	 */
 	public void fireAllLasers() {
 		List<Laser> lasers = gameBoard.getLasers();
 		int x;
 		int y;
 		int direction;
 
 		for (Laser laser : lasers){
 			x = laser.getX();
 			y = laser.getY();
 			direction = laser.getDirection();
 			fireLaser(x, y, direction);
 		}
 		for (Robot robot : robots){
 			if (robot.isDead()) {
 				continue;
 			}
 			x = robot.getX();
 			y = robot.getY();
 			direction = robot.getDirection();
 			fireRobotLaser(x, y, direction);
 		}
 	}
 
 	/*
 	 * This method should only be called after conveyorBelts have moved all robots.
 	 * Size of oldPositions needs to be int[robots.size()][2]
 	 */
 	private void checkConveyorBeltCollides(int[][] oldPositions){
 		int nbrOfMovedRobots = 0;
 		for(int i = 0; i<robots.size(); i++){
 			if(!robots.get(i).isDead() && (robots.get(i).getX() != oldPositions[i][0] || robots.get(i).getY() != oldPositions[i][1])){
 				if(canMove(robots.get(i).getX(), robots.get(i).getY(), oldPositions[i][0], oldPositions[i][1])){
 					addSimultaneousMove(robots.get(i));
 					nbrOfMovedRobots++;
 				}else{
 					robots.get(i).setX(oldPositions[i][0]);
 					robots.get(i).setY(oldPositions[i][1]);
 				}
 			}
 		}
 		
 		List<Robot> handleCollision = new ArrayList<Robot>();
 		for(int i = 0; i<robots.size(); i++){
 			for(int j = 0; j<robots.size(); j++){
 				if(i != j && robots.get(i).getX() == robots.get(j).getX() && 
 						robots.get(i).getY() == robots.get(j).getY()){
 					boolean robotIMove = oldPositions[i][0] != robots.get(i).getX() || oldPositions[i][1] != 
 							robots.get(i).getY();
 					boolean robotJMove = (oldPositions[j][0] != robots.get(j).getX() || 
 							oldPositions[j][1] != robots.get(j).getY());
 					// If both robots have moved to the same position by conveyorBelt, both should move back.
 					if(robotIMove && robotJMove){
 						robots.get(i).setX(oldPositions[i][0]);
 						robots.get(i).setY(oldPositions[i][1]);
 						robots.get(j).setX(oldPositions[j][0]);
 						robots.get(j).setY(oldPositions[j][1]);
 						
 						int allMovesSize = allMoves.size();// The size will change during the loop, but must stay the same
 						// for the code to work.
 						for(int k = 1; k<=nbrOfMovedRobots; k++){
 							if(allMoves.get(allMovesSize - k).contains(i + ":") || 
 									allMoves.get(allMovesSize - k).contains(j + ":")){
 								allMoves.remove(allMovesSize - k);
 							}
 						}
 						nbrOfMovedRobots -= 2;
 					}else{// Push robot
 						if(robotIMove){
 							handleCollision.add(robots.get(i));
 						}else if(robotJMove){
 //							handleCollision.add(robots.get(j));//TODO remove else if if this fixes stackOverflow bug
 						}
 					}
 				}
 			}
 		}
 		for(Robot robot : handleCollision){
 			handleCollision(robot, oldPositions[robots.indexOf(robot)][0], oldPositions[robots.indexOf(robot)][1]);
 		}
 	}
 
 	/*
 	 * Return true if the collision needs to be reversed
 	 */
 	private boolean handleCollision(Robot robot, int oldX, int oldY){
 		// TODO fix stackOverFlow
 		boolean wallCollision = false;
 		if(canMove(oldX, oldY, robot.getX(), robot.getY())){
 			for(Robot r : robots){
 				// Do any robot stand on the same tile as the robot from the parameters.
 				if(!r.isDead() && robot != r && robot.getX() == r.getX() && robot.getY() == r.getY()){
 					// Push other Robot
 					r.setX(r.getX() - (oldX - robot.getX()));
 					r.setY(r.getY() - (oldY - robot.getY()));
 					addSimultaneousMove(r);
 					// Check if other Robot collides
 					if(handleCollision(r, robot.getX(), robot.getY())){// true if r walks into a wall
 						robot.setX(robot.getX() + (oldX - robot.getX()));
 						robot.setY(robot.getY() + (oldY - robot.getY()));
 						allMoves.remove(allMoves.size()-1);// It is always the last move which should be reversed.
 						wallCollision = true;
 					}
 					checkGameStatus();
 					if (!r.isDead()) {
 						gameBoard.getTile(r.getX(), r.getY()).instantAction(r);
 					}
 					if(r.isDead()){
 						addRobotDeadMove(r);
 					}
 				}
 			}
 		}else{// The robot can't walk through a wall
 			robot.setX(oldX);
 			robot.setY(oldY);
 			allMoves.remove(allMoves.size()-1); // It is always the last move which should be reversed.
 			wallCollision = true;
 		}
 		return wallCollision;
 	}
 
 	/**
 	 * Move robots according to the chosen cards.
 	 */
 	public void moveRobots() {
 		allMoves.clear();
 		List<Card[]> currentCards = new ArrayList<Card[]>();
 		for (int i = 0; i < robots.size(); i++) {
 			Card[] chosenCards = robots.get(i).getChosenCards().clone();
 			currentCards.add(chosenCards);
 		}
 		int[][] oldPosition = new int[robots.size()][2];
 
 		for (int i = 0; i < 5; i++) { //loop all 5 cards
 			allMoves.add(";" + "R#" + i);
 			for (int j = 0; j < robots.size(); j++) { //for all robots
 				for (int k = 0; k < robots.size(); k++) {
 					oldPosition[k][0] = robots.get(k).getX();
 					oldPosition[k][1] = robots.get(k).getY();
 				}
 				int highestPriority = -1;
 				int indexOfHighestPriority = -1; //player index in array
 				for (int k = 0; k < currentCards.size(); k++) { //find highest card
 					if (currentCards.get(k)[i] != null //check if card exists and..
 							&&	highestPriority //..is the highest one
 							< currentCards.get(k)[i].getPriority()) {
 						highestPriority = currentCards.get(k)[i].getPriority();
 						indexOfHighestPriority = k;
 					}
 				}
 				if(indexOfHighestPriority == -1){
 					continue;// This will only happen when a robot don't have a card, i.e. it has lost.
 				}
 				//Move the robot that has the highest priority on its card
 				Robot currentRobot = robots.get(indexOfHighestPriority);
 
 				int nbrOfSteps = 1;
 				if (currentCards.get(indexOfHighestPriority)[i] instanceof Move){
 					nbrOfSteps = Math.abs(((Move)currentCards.get(indexOfHighestPriority)[i]
 							).getDistance());
 				}
 				for (int k = 0; k < nbrOfSteps; k++){
 					if (robots.get(indexOfHighestPriority).isDead()) {
 						break; //so that robot doesn't walk more after dying this for-loop
 					}
 					oldPosition[indexOfHighestPriority][0] = robots.get(indexOfHighestPriority).getX();
 					oldPosition[indexOfHighestPriority][1] = robots.get(indexOfHighestPriority).getY();
 					currentCards.get(indexOfHighestPriority)[i]
 							.action(currentRobot);
 					addMove(currentRobot);
 					handleCollision(currentRobot, oldPosition[indexOfHighestPriority][0], 
 							oldPosition[indexOfHighestPriority][1]);
 					if(checkGameStatus())return;
 					if(!currentRobot.isDead()){
 						gameBoard.getTile(currentRobot.getX(), currentRobot.getY())
 						.instantAction(currentRobot);
 					}
 					if(currentRobot.isDead()){
 						addRobotDeadMove(currentRobot);
 					}
 					if (checkGameStatus())return;
 				}
 
 				//Remove the card so it doesn't execute twice
 				currentCards.get(indexOfHighestPriority)[i] = null;
 			}
 			activateBoardElements();
 			if (checkGameStatus())return;
 		}
 
 		allMoves.add(";B7");// B7 = robot respawn actions.
 		for(Robot robot : robots){
 			deck.returnCards(robot.returnCards());
 			if (robot.isDead() && !robot.hasLost()) {
 				resetRobotPosition(robot);
 				robot.setDead(false);
 			}
 		}
 	}
 
 	/**
 	 * 
 	 * @return true if if game is over, else false
 	 */
 	private boolean checkGameStatus(){
 		for(int i = 0; i < robots.size(); i++){
 			if (robotHasReachedLastCheckPoint())return true;
			if(robots.get(i).getHealth() < 0 || !robots.get(i).isDead() && (robots.get(i).getX() < 0 || robots.get(i).getX() >= gameBoard.getWidth() || 
 					robots.get(i).getY() < 0 || robots.get(i).getY() >= gameBoard.getHeight())){
 				robots.get(i).die();
 				if(robots.get(i).hasLost()){
 					--robotsPlaying;
 					if(isGameOver(i))return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	/**
 	 * Check if a player has won.
 	 * @return true if a robot has won, else false
 	 */
 	private boolean robotHasReachedLastCheckPoint() {
 		for (int i = 0; i < robots.size(); i++){
 			if (robots.get(i).getLastCheckPoint() == gameBoard.getNbrOfCheckPoints()) {
 				isGameOver = true;
 				addRobotWon(robots.get(i));
 				return isGameOver;
 			}
 		}
 		return isGameOver;
 	}
 
 	/**
 	 * Check if a robot has lost IF a robot has lost.
 	 * @return true if a player has won, else false
 	 */
 	private boolean isGameOver(int robotID) {
 		checkRobotAlone();
 		if (!isGameOver) {
 			addRobotLost(robots.get(robotID));
 		}
 		return isGameOver;
 	}
 
 	/**
 	 * Checks if robot has won because its alone.
 	 * Sets isGameOver to true if game is over.
 	 */
 	private void checkRobotAlone() {
 		if (robotsPlaying == 1) {
 			for (int j = 0; j < robots.size() ; j++) {
 				if (!robots.get(j).hasLost()) {
 					isGameOver = true;
 					addRobotWon(robots.get(j));
 					return;
 				}
 			}
 		}
 	}
 
 	private void resetRobotPosition(Robot robot){
 		int distanceFromSpawnPoint = 0;
 		while(true){
 			for(int i = robot.getSpawnPointX() - distanceFromSpawnPoint; i<=robot.getSpawnPointX() + distanceFromSpawnPoint; i++){
 				for(int j = robot.getSpawnPointY() - distanceFromSpawnPoint; j<=robot.getSpawnPointY() + distanceFromSpawnPoint; j++){
 					boolean tileOccupied = false;
 					for(Robot r : robots){
 						if((r.getX() == i && r.getY() == j)){
 							tileOccupied = true;
 						}
 					}
 					if(!tileOccupied && i>=0 && i<gameBoard.getWidth() && j>=0 && j<gameBoard.getHeight()){
 						robot.setX(i);
 						robot.setY(j);
 						addRespawnMove(robot);
 						return;
 					}
 				}
 			}
 			distanceFromSpawnPoint++;
 			if(distanceFromSpawnPoint > 2){
 				break;
 			}
 		}
 	}
 	
 	private void addRespawnMove(Robot robot){
 		allMoves.add("#" + robots.indexOf(robot) + ":" + robot.getDirection() + 
 				robot.getXAsString() + robot.getYAsString());
 	}
 	
 	private void addRobotWon(Robot robot) {
 		allMoves.add(";W#" + robots.indexOf(robot));
 	}
 	
 	private void addRobotLost(Robot robot) {
 		allMoves.add(";L#" + robots.indexOf(robot));
 	}
 
 	private void addSimultaneousMove(Robot robot){
 		allMoves.add("#" + robots.indexOf(robot) + ":" + robot.getDirection() + 
 				robot.getXAsString() + robot.getYAsString());
 	}
 
 	private void addMove(Robot robot){
 		allMoves.add(";" + robots.indexOf(robot) + ":" + robot.getDirection() + 
 				robot.getXAsString() + robot.getYAsString() );
 	}
 	
 	private void addRobotDeadMove(Robot robot){
 		allMoves.add(";F#" + robots.indexOf(robot) + ":" + robot.getLife());
 	}
 
 	/**
 	 * Return a String containing all moves during a round.
 	 * @return a String containing all moves during a round.
 	 */
 	public String getAllMoves(){
 		StringBuilder sb = new StringBuilder();
 		for(String string : allMoves){
 			sb.append(string);
 		}
 		// The first character will be a "split character" i.e. ; or #
 		String returnString = sb.substring(1);
 		return returnString;
 	}
 
 	/**
 	 * Returns a list of all robots in the game.
 	 * @return a list of all robots in the game.
 	 */
 	public List<Robot> getRobots(){
 		return robots;
 	}
 
 	public GameBoard getGameBoard() {
 		return gameBoard;
 	}
 
 
 	public boolean isGameOver() {
 		return isGameOver;
 	}
 
 	/**
 	 * Return the amount of robots still in the game. I.e. robots
 	 * who have not lost all their lives.
 	 * @return the amount of robots still in the game.
 	 */
 	public int getRobotsPlaying() {
 		return robotsPlaying;
 	}
 
 }
