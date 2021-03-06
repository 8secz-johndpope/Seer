 package util;
 
 import graphics.render.Graphics;
 import gui.GUI;
 import gui.menu.MainMenu;
 
 import org.lwjgl.input.Mouse;
 import org.lwjgl.opengl.Display;
 
 import physics.Physics;
 import util.debug.Debug;
 import util.helper.DisplayHelper;
 import util.manager.KeyboardManager;
 import util.manager.MouseManager;
 
 // Rule number 1: Tell everyone about Spaceout (ask them for ideas! We need ideas!).
 // Rule number 2: Comment everything motherfucker.
 
 /**
  * Initializes and runs the game.
  * 
  * @author TranquilMarmot
  */
 public class Runner {
 	/** what version of Spaceout is this? */
	public static final String VERSION = "0.0.51";
 
 	/** prevents updates but still renders the scene */
 	public static boolean paused = false;
 	/** keeps the pause button from repeatedly pausing and unpausing */
 	private boolean pauseDown = false;
 
 	/** if either of this is true, it means it's time to shut down ASAP */
 	public static boolean done = false;
 
 	/** the keyboard and mouse handlers that need to be updated every frame */
 	public static KeyboardManager keyboard = new KeyboardManager();
 	public static MouseManager mouse = new MouseManager();
 
 	public static void main(String[] args) {
 		// Instantiate a runner, otherwise everything would have to be static
 		Runner run = new Runner();
 		run.run();
 	}
 
 	/**
 	 * Runs the game
 	 */
 	public void run() {
 		// initialize everything
 		init();
 		try {
 			// keep going until the done flag is up or a window close is
 			// requested
 			while (!done && !DisplayHelper.closeRequested) {
 				// check for window resizes
 				DisplayHelper.resizeWindow();
 				// update misc stuff (keyboard, mouse, etc.)
 				update();
 				// render the scene
 				Graphics.renderAndUpdateEntities();
 				// update the display (this swaps the buffers)
 				Display.update();
 				Display.sync(DisplayHelper.targetFPS);
 			}
 			shutdown();
 		} catch (Exception e) {
 			// if an exception is caught, destroy the display and the frame
 			shutdown();
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Initialize OpenGL, variables, etc.
 	 */
 	private void init() {
 		DisplayHelper.createWindow();
 		Graphics.initGL();
 
 		/*
 		 * NOTE: Most of the initializing is done on the main menu, see
 		 * MainMenu.java
 		 */
 		MainMenu mainMenu = new MainMenu();
 		GUI.addBuffer.add(mainMenu);
 	}
 
 	/**
 	 * Updates everything
 	 */
 	private void update() {
 		// System.out.println("delta: " + Debug.getDelta());
 		// update the mouse and keyboard handlers
 		mouse.update();
 		keyboard.update();
 
 		/* BEGIN PAUSE LOGIC */
 		// if pauseDown is true, it means that the pause button is being
 		// held,
 		// so it avoids repeatedly flipping paused when the key is held
 		if (KeyboardManager.pause && !pauseDown) {
 			paused = !paused;
 			pauseDown = true;
 		}
 
 		if (!KeyboardManager.pause) {
 			pauseDown = false;
 		}
 
 		// release the mouse if the game's paused or the console is on or the menu is up
 		if (!paused && !Debug.consoleOn && !GUI.menuUp)
 			Mouse.setGrabbed(true);
 		else
 			Mouse.setGrabbed(false);
 		/* END PAUSE LOGIC */
 
 		DisplayHelper.doFullscreenLogic();
 		
 		if(!paused && Physics.dynamicsWorld != null)
 			Physics.update();
 	}
 
 	/**
 	 * To be called when the game is quit
 	 */
 	private void shutdown() {
 		Display.destroy();
 		DisplayHelper.frame.dispose();
 	}
 }
