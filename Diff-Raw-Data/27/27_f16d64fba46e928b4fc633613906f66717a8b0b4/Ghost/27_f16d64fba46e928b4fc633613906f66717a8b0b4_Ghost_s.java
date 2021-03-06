 package code.uci.pacman.objects.controllable;
 
 
 import java.util.Random;
 
 import code.uci.pacman.ai.AI;
 import code.uci.pacman.controllers.WallController;
 import code.uci.pacman.game.Direction;
 import code.uci.pacman.game.GameState;
 import code.uci.pacman.objects.ControllableObject;
 
 
 /**
  * Represents a Ghost character that chases PacMan.  A Ghost can be controlled by human
  * or by an AI as denoted by the boolean isPlayer.  Ghosts can chase PacMan and try to
  * eat him by colliding with him.  If PacMan eats a Power Pellet, scatter mode is 
  * initiated and a ghost can be eaten if touched by PacMan.  
  * @author Team Objects/AI
  * 
  */
 
 public abstract class Ghost extends ControllableObject implements AI {
 
 	private static final int GHOST_WIDTH = 22;
 	private static final int GHOST_HEIGHT = 22;
 	private static final int GHOST_FRAMERATE = 5;
 	private static final int CAGE_POS = 250;
 	
 	private boolean isPlayer;
 
 	/**
 	 * Constructs a ghost with a sprite, initial position, speed, and a boolean denoting if it
 	 * is controlled by a human or by AI.  
 	 * @param imgPath the image/sprite associated with the ghost
 	 * @param x it's initial x coordinate on the level
 	 * @param y it's initial y coordinate on the level
 	 * @param speed the speed at which the ghost moves
 	 * @param isPlayer true if the ghost is controlled by a human; false if it's controlled by AI
 	 */
 	public Ghost(String imgPath, int x, int y, int speed, boolean isPlayer) {
 		super(imgPath, new int[] {0,0}, GHOST_WIDTH, GHOST_HEIGHT, GHOST_FRAMERATE, x, y);
 		super.speed = speed;
 		this.isPlayer = isPlayer;
 	}
 
 	/**
 	 * the point value of this object
 	 */
 	private int scoreValue;
 	private boolean scatter;
 	
 
 	/**
 	 * Tells the game controller that this ghost has been eaten.
 	 */
 	public void eaten(){
 		control.ghostEaten(this);
 	}
 	
 	/**
 	 * Tests if the ghost is in Scatter Mode.
 	 * @return true if the ghost is currently in Scatter Mode.
 	 */
 	public boolean isScattered() {
 		return scatter;
 	}
 	
 	/**
 	 * Tells the ghost to go into Scatter Mode.
 	 */
 	public void scatter(){
 		scatter = true;
 		// TODO change sprite? yes
 	}
 	
 	/**
 	 * Tells the ghost to come out of Scatter Mode.
 	 */
 	public void unScatter() {
 		scatter = false;
 		// TODO change sprite? yes
 	}
 	
 	/***
 	 * Respawns the ghost back within the cage and disables scatter for the ghost.
 	 */
 	public void respawnInCage() {
 		Random r = new Random();
 		int randomOffset = r.nextInt(50);
 		super.position(CAGE_POS + randomOffset, CAGE_POS);
 		this.unScatter();
 	}
 	
 	/**
 	 * Returns true if the ghost is controlled by a player.
 	 * @return true if the ghost is controlled by a human; false if it is controlled by AI 
 	 */
 	public boolean isPlayer(){
 		return isPlayer;
 	}
 
 	/**
 	 * Gets the score of the ghost.
 	 * @return The score of the ghost.
 	 */
 	public int getValue() {
 		return scoreValue;
 	}
 
 	/**
 	 * This changes the sprite based on its direction.
 	 * @param d direction of the ghost.
 	 */
 	@Override
 	//this is for changing the sprite based on direction
 	protected void spriteForDirection(Direction d) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	/**
 	 * 
 	 */
 	public abstract Direction getMove();
 
 	public boolean moveIsAllowed(Direction d)
 	{
 		WallController walls = GameState.getInstance().getWalls();
		if(d == Direction.UP && walls.willCollideAtPos(this,0,-13))
 			return true;
		if(d == Direction.DOWN && walls.willCollideAtPos(this,0,13 + this.height()))
 			return true;
		if(d == Direction.LEFT && walls.willCollideAtPos(this,-10,0))
 			return true;
		if(d == Direction.RIGHT && walls.willCollideAtPos(this,10 + this.width(),0))
 			return true;
 		else
 			return false;
 	}
 
 }
