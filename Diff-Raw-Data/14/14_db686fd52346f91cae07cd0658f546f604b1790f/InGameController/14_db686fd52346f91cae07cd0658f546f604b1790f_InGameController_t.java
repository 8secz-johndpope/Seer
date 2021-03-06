 package controller;
 
 
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 
 import org.newdawn.slick.Color;
 import org.newdawn.slick.GameContainer;
 import org.newdawn.slick.Graphics;
 import org.newdawn.slick.Image;
 import org.newdawn.slick.Input;
 import org.newdawn.slick.KeyListener;
 import org.newdawn.slick.SlickException;
 import org.newdawn.slick.state.BasicGameState;
 import org.newdawn.slick.state.StateBasedGame;
 import org.newdawn.slick.tiled.TiledMap;
 
 import utils.BlockMapUtils;
 import view.BlockMapView;
 import view.InGameView;
 import view.SpikesView;
 import model.BlockMap;
 import model.Game;
 import model.InGame;
 import model.Item;
 
 public class InGameController extends BasicGameState {
 	private InGame inGame;
 	private InGameView inGameView;
 	private CharacterController characterController;
 	private PlayerController playerController;
 	private WorldController worldController;
 	private StatusBarController statusBarController;
 	private BlockMapController blockMapController;
 	private ArrayList <CandyMonsterController> candyMonsterController;
 	private ArrayList <ItemController> itemController;
 	private ArrayList <SpikesController> spikeController;
 	private Item lastHeldItem;
 	private int itemsDelivered;
 	private StateBasedGame sbg;
 	private GameController gameController;
 	
 	//should be based on the frame update (delta or something like that)
 	private float timeStep = 1.0f / 60.0f;
 	private int velocityIterations = 6;
 	private int positionIterations = 2;
 	
 	public InGameController(GameController gameController) {
 		this.gameController = gameController;
 		
 	}
 
 	
 	@Override
 	public void init(GameContainer gc, StateBasedGame sbg)
 			throws SlickException {
 		this.sbg = sbg;
 		this.candyMonsterController = new ArrayList<CandyMonsterController>();
 		this.itemController = new ArrayList<ItemController>();
 		this.spikeController = new ArrayList<SpikesController>();
 		//TODO ladda in filer
 		 this.blockMapController = new BlockMapController(new TiledMap(BlockMapUtils.getTmxFile(1)));
 		 /*Create candy monster and its items*/
 		 for(int i = 0; i < blockMapController.getCandyMonsterMap().getBlockList().size(); i++){
 			 this.candyMonsterController.add(new CandyMonsterController(this, i)); 
 			 this.itemController.add(new ItemController(this, i));
 		 }
 		 
 		 this.worldController = new WorldController(this);
 		 /*Create spikes*/
 		 for(int i = 0; i < blockMapController.getSpikesMap().getBlockList().size(); i++){
 			 this.spikeController.add(new SpikesController(this, i));
 		 }
 		 this.statusBarController = new StatusBarController(this);
 		 this.characterController = new CharacterController(this);
 		 this.playerController = new PlayerController(characterController, this);
 		 this.inGame = new InGame();
 		 
 		 //temporarily store the SpikesViews in a list
 		 ArrayList<SpikesView> tmpSpikesViewList = new ArrayList();
 		 for(SpikesController spikesController : spikeController) {
 			 tmpSpikesViewList.add(spikesController.getSpikesView());
 		 }
 		 
 		 
 		 this.inGameView = new InGameView(inGame, worldController.getWorldView(), statusBarController.getStatusBarView(), 
 				 characterController.getCharacterView(), tmpSpikesViewList);
 		 itemsDelivered = 0;
 
 	}
 
 	@Override
 	public void render(GameContainer gc, StateBasedGame sbg, Graphics g)
 			throws SlickException {
 		this.inGameView.render(gc, sbg, g);
 	}
 	
 	@Override
 	public void update(GameContainer gc, StateBasedGame sbg, int delta)
 			throws SlickException {
 		//change the time for the game and the character
 		this.inGame.setTime(this.inGame.getTime()-(delta/1000f));
 		this.characterController.getCharacter().setTimeSinceHit(this.characterController.getCharacter().getTimeSinceHit() + delta/1000f);
 		//update the timeBar
 		this.statusBarController.getStatusBarView().updateTimeBar(this.inGame.getLevelTime(), this.inGame.getTime());
 		//check if the game is over
 		checkGameOverConditions();
 		//check key presses
 		characterController.keyPressedUpdate(gc);
 		//simulate the JBox2D world TODO timeStep --> delta
 		if(delta > 0) {
 			this.timeStep = (float) delta / 1000f * 4; //4 is for getting a good speed
 		}
 		worldController.getWorldView().getjBox2DWorld().step(timeStep, velocityIterations, positionIterations);
 		worldController.updateSlickShape();
 		worldController.updateItemShape(worldController.getItemViewList(), characterController.getCharacterView());
 		characterController.getCharacter().setX((int)characterController.getCharacterView().getSlickShape().getX());
 		characterController.getCharacter().setY((int)characterController.getCharacterView().getSlickShape().getY());
 		
 	}
 
 	@Override
 	public void keyPressed (int key, char c) {
 		if (key == Input.KEY_DOWN) {
 			if (characterController.findItemToPickUp()!= null && !characterController.isHoldingItem()) {
 				characterController.getCharacterView().setColor(Color.pink);
 				characterController.getCharacter().pickUpItem(characterController.findItemToPickUp());
 			} else if (characterController.isHoldingItem() && 
 					characterController.getCharacterView().getCharacterBody().getLinearVelocity().y == 0) {
 				lastHeldItem = characterController.getCharacter().getHeldItem();	
 				characterController.getCharacter().dropDownItem(characterController.getCharacter().getHeldItem());
 				this.itemController.get(lastHeldItem.CANDY_NUMBER).uppdateItemShape();
 				candyMonsterController.get(lastHeldItem.CANDY_NUMBER).isDroppedOnMonster(lastHeldItem);
 			}
 		}
		if (key == Input.KEY_UP) {
			characterController.tryToJumpCharacter();
		}
 		if (key == Input.KEY_ESCAPE){
 			//Set previous state to the state you where in before pressing ESC
 			PauseController.setPreviousState(Game.IN_GAME); 
 			sbg.enterState(Game.PAUSE_MENU);
 		}
 	}
 	
 	/**
 	 * checks if the game is done by checking the lives on the character 
 	 * and the items left in the world.
 	 * 
 	 */
 	public void checkGameOverConditions() {
 		if (this.itemController.size() == itemsDelivered) {
 			System.out.println("No more items to pick up, level cleared!");
 			this.playerController.getPlayer().setScore((int)this.inGame.getTime(), this.itemsDelivered);
 			sbg.enterState(Game.END_OF_LEVEL);
 		} else if (this.characterController.getCharacter().getLife() == 0 || this.inGame.getTime() <= 0) {
 			System.out.println("No more lives, you are dead!");
 			sbg.enterState(Game.END_OF_LEVEL);
 		}
 	}
 	
 	@Override
 	public int getID() {
 		return Game.IN_GAME;
 	}
 	
 	public int getItemsDelivered() {
 		return itemsDelivered;
 	}
 
 
 	public void setItemsDelivered(int itemsDelivered) {
 		this.itemsDelivered = itemsDelivered;
 	}
 
 
 	public CharacterController getCharacterController() {
 		return characterController;
 	}
 
 	public WorldController getWorldController() {
 		return worldController;
 	}
 	
 	public BlockMapController getBlockMapController() {
 		return blockMapController;
 	}
 
 
 	public ArrayList<CandyMonsterController> getCandyMonsterController() {
 		return candyMonsterController;
 	}
 
 
 	public ArrayList<ItemController> getItemController() {
 		return itemController;
 	}
 
 
 	public ArrayList<SpikesController> getSpikesController() {
 		return spikeController;
 	}
 
 
 	public PlayerController getPlayerController() {
 		return playerController;
 	}
 
 }
