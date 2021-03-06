 package code.uci.pacman.game;
 
 import java.io.IOException;
 
 import code.uci.pacman.controllers.*;
 import code.uci.pacman.gui.*;
 import code.uci.pacman.objects.stationary.*;
 import code.uci.pacman.multiplayer.*;
 import ucigame.*;
 
 import java.util.*;
 
 /**
  * 
  * The main game class for PacMan. This class extends from the UciGame class
  * which is part of the UciGame game framework. Thanks to Professor Frost for
  * making this great framework which made our lives easier. This is the class
  * that should be run to play PacMan.
  * 
  * @author The Game Team
  */
 public class PacManGame extends Ucigame {
 
 	private static final long serialVersionUID = -917916728311505169L;
 	private GameController control;
 	private ScoreBoard scoreBoard;
 	private TopScores topScores;
 	private MainMenu mainMenu;
 	private IntroPlayer introPlayer; // for playing the intro
 	private ScreenMode currentScene; // stores the current scene displayed
 	public static String font = "Dialog.bold";
 	public static int multiplayerType = 1; // 1=server, 2=client
 	public static int gameType = 1; // 1=single, 2 = multi
 
 	// This was added to override the stupid UCI game thing that requires the param twice...
 	// this allows me to put the whole game into a JAR file and not have it bitch at me
 	public static void main(String[] args)
 	{
 		String[] args2 = new String[1];
 		args2[0] = "code.uci.pacman.game.PacManGame";
 		Ucigame.main(args2);
 	}
 
 	/* Initialization */
 	public void setup() {
 		generatePositions(false);
 		// creates the window, sets the title, initialize
 		initializeWindow();
 		// starts the intro for the game
 		showIntroScreen();
 	}
 
 	private void initializeWindow() {
 		// set global game control 
 		control = GameController.setInstance(this);
 		// set framerate
 		this.framerate(20);
 		// set window size
 		window.size(600, 650);
 		// set window title
 		window.title("Pac Man Fever");
 	}
 
 	private void showIntroScreen() {
 		// initialize screens
 		introPlayer = new IntroPlayer();
 		// show intro screen
 		canvas.background(0, 0, 0); 
 		// This part was added so that it could better sync.  Untested.
 		try
 		{
 			System.out.println("LOADING INTRO!");
 			Thread.sleep(3000);
 			showScene(ScreenMode.INTRO);
 			introPlayer.playIntroTheme();
 		}
 		catch(InterruptedException e)
 		{
 			System.out.println("Intro thread interrupted before start!");
 			e.printStackTrace();
 		}
 	}
 
 	private void showMenuScreen() {
 		// stop intro theme
 		introPlayer.stopIntroTheme();
 		// initialize menu
 		mainMenu = new MainMenu();
 		// show menu
 		canvas.background(getImage("images/final/mainMenuBackGroundDim2.png"));
 		mainMenu.startMenuTheme();
 		showScene(ScreenMode.MENU);
 	}
 
 	public void showGameScreen() {
 		// stop menu theme
 		mainMenu.stopMenuTheme();
 		introPlayer.stopIntroTheme();
 		// show game
 		canvas.background(0, 0, 0);
 		control.startGame(); // start the game
 		scoreBoard = new ScoreBoard();
 		showScene(ScreenMode.GAME);
 	}
 
 	public void showScoresScreen() {
 		topScores = new TopScores();
		canvas.background(getImage("images\\final\\topscores.png"));
 		showScene(ScreenMode.SCORES);
 	}
 
 	private void showMpwaitingScreen()
 	{
 		mainMenu.stopMenuTheme();
 		introPlayer.stopIntroTheme();
 		canvas.background(0, 0, 0);
 		showScene(ScreenMode.MPWAITING);
 	}
 	
 	private void startPacManServer()
 	{
 		try
 		{
 			new Server().start();
 		}
 		catch (Exception e){}
 	}
 
 	private void startPacManClient()
 	{
 		try
 		{
 			new Client().start();
 		}
 		catch (Exception e){}
 		showMpwaitingScreen();
 	}
 
 	public void drawMpwaiting() {
 		canvas.clear();
 		canvas.font(PacManGame.font, PacManGame.BOLD, 40, 255, 255, 255);
 		canvas.putText("WAITING FOR SERVER", 100, 300);
 		canvas.font(PacManGame.font, PacManGame.BOLD, 20, 255, 255, 255);
 		canvas.putText("PacMan is currently listening for open games", 80, 340);
 	}
 
 	private void generatePositions(boolean run) {
 		try {
 			if (run) {
 				ItemGenerator.execute();
 			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 
 	/* Painting the Game Scenes */
 
 	/**
 	 * Draws the "Intro" scene
 	 */
 	public void drawIntro() {		
 		introPlayer.draw();
 	}
 
 	/**
 	 * Draws the "Menu" scene
 	 */
 	public void drawMenu() {
 		canvas.clear();
 		mainMenu.draw();
 	}
 
 	/**
 	 * Draws the "Scores" scene
 	 */
 	public void drawScores() {
 		canvas.clear();
 		topScores.draw();
 	}
 
 	/**
 	 * Draws the "Game" scene
 	 */
 	public void drawGame() {
 		canvas.clear();
 		control.nextMove();
 		control.drawState();
 		scoreBoard.draw();
 	}
 
 	/**
 	 * Draws the "Game Over" scene
 	 */
 	public void drawGameover() {
 		canvas.clear();
 		canvas.font(PacManGame.font, PacManGame.BOLD, 40, 255, 255, 255);
 		canvas.putText("GAME OVER", 200, 300);
 		canvas.font(PacManGame.font, PacManGame.BOLD, 20, 255, 255, 255);
 		canvas.putText("Press R to Try Again", 210, 340);
 	}
 
 	/* Timer Handling */
 	public void startFruitTimer() {
 		startTimer("removeFruit", Fruit.SHOW_FRUIT_DURATION);
 	}
 
 	public void removeFruitTimer() {
 		stopTimer("removeFruit");
 		control.hideFruit();
 	}
 
 	public void startScatterTimer() {
 		startTimer("unScatterGhosts", GhostController.SCATTERSECONDS);
 	}
 
 	public void unScatterGhostsTimer() {
 		stopTimer("unScatterGhosts");
 		control.unscatterGhosts();
 	}
 
 	/* Event Input Handling */
 
 	/**
 	 * 
 	 * displays the menu and plays the menu theme.
 	 * 
 	 */
 	public void onClickMenuStart() {
 		if (isShowingScene(ScreenMode.INTRO)) {
 			showMenuScreen();
 		}
 	}
 
 	/**
 	 * 
 	 * starts a single-player game.
 	 * 
 	 */
 	public void onClickSinglePlay() {
 		if (isShowingScene(ScreenMode.MENU)) {
 			PacManGame.gameType = 1;
 			System.out.println("single player click");
 			startPacManServer();
 			showGameScreen();
 		}
 	}
 
 	/**
 	 * 
 	 * goes to the multiplayer menu.
 	 * 
 	 */
 	public void onClickMultiPlay() {
 		if (isShowingScene(ScreenMode.MENU)) {
 			PacManGame.gameType = 2;
 			System.out.println("multi player click");
 			startPacManClient();
 			//showGameScreen();
 			// beginGame();
 		}
 	}
 
 	/**
 	 * 
 	 * shows top scores screen.
 	 * 
 	 */
 	public void onClickTopScores() {
 		if (isShowingScene(ScreenMode.MENU)) {
 			System.out.println("topScore click");
 			showScoresScreen();			
 			// beginGame();
 		}
 	}
 
 	/**
 	 * 
 	 * writes out the high scores, then quits the game.
 	 * 
 	 */
 	public void onClickQuit() {
 		if (isShowingScene(ScreenMode.MENU)) {
 
 			if( PacManGame.gameType == 1 )
 			{
 				// server
 				Server.send(PType.GAMEOVER);
 			}
 			else
 			{
 				Client.send(PType.LEAVE);
 			}
 
 			System.out.println("quit click");
 			System.exit(0);
 			// beginGame();
 		}
 	}
 
 	public void onKeyPressIntro() {
 		if (keyboard.isDown(keyboard.S) && isShowingScene(ScreenMode.INTRO)) {
 			showMenuScreen();
 		}
 		if (keyboard.isDown(keyboard.SPACE) && isShowingScene(ScreenMode.INTRO)) {
 			showMenuScreen();
 		}
 	}
 
 	// This kept messing up the Start Game button, since it would start the game before
 	// the button was pressed.   If you want to skip the intro, just press and hold "s" or the 
 	// spacebar to get to the menu.  If you still want to click to get to the menu, 
 	// click the area where the start game button would be
 	//public void onMousePressed() {
 		//if (isShowingScene(ScreenMode.INTRO)) {
 		//	showMenuScreen();
 		//}
 	//}
 
 	public void onKeyPressGame() {
 		// // Arrow keys and WASD keys move the paddle
 		if( PacManGame.gameType == 1 )
 		{
 			if (keyboard.isDown(keyboard.UP, keyboard.W))
 			{
 				Server.send(Direction.UP);
 				control.setPacManDirection(Direction.UP);
 			}
 			else if (keyboard.isDown(keyboard.DOWN, keyboard.S))
 			{
 				Server.send(Direction.DOWN);
 				control.setPacManDirection(Direction.DOWN);
 			}
 			else if (keyboard.isDown(keyboard.LEFT, keyboard.A))
 			{
 				Server.send(Direction.LEFT);
 				control.setPacManDirection(Direction.LEFT);
 			}
 			else if (keyboard.isDown(keyboard.RIGHT, keyboard.D))
 			{
 				Server.send(Direction.RIGHT);
 				control.setPacManDirection(Direction.RIGHT);
 			}
 		}
 		else
 		{
 			// FOR MUTLIPLAYER
 			String gname = capitalize(Client.getGhostType().name());
 
 			if (keyboard.isDown(keyboard.UP, keyboard.W))
 			{
 				Client.send(Direction.UP);
 				GameState.getInstance().getGhosts().getObjectAt(gname).setDirection(Direction.UP);
 			}
 			else if (keyboard.isDown(keyboard.DOWN, keyboard.S))
 			{
 				Client.send(Direction.DOWN);
 				GameState.getInstance().getGhosts().getObjectAt(gname).setDirection(Direction.DOWN);
 			}
 			else if (keyboard.isDown(keyboard.LEFT, keyboard.A))
 			{
 				Client.send(Direction.LEFT);
 				GameState.getInstance().getGhosts().getObjectAt(gname).setDirection(Direction.LEFT);
 			}
 			else if (keyboard.isDown(keyboard.RIGHT, keyboard.D))
 			{
 				Client.send(Direction.RIGHT);
 				GameState.getInstance().getGhosts().getObjectAt(gname).setDirection(Direction.RIGHT);
 			}
 
 		}
 		if (keyboard.isDown(keyboard.R)) {
 			showGameScreen();
 		}
 	}
 
 	public void onKeyPressGameOver() {
 		if (keyboard.isDown(keyboard.R)) {
 			showGameScreen();
 		}
 	}
 
 	public Sprite makeSpriteFromPath(String string) {
 		return makeSprite(getImage(string));
 	}
 
 	/**
 	 * Displays the specified scene onto the window, while also storing the
 	 * scene for later reference
 	 * 
 	 * @param scene
 	 *            the scene to display
 	 */
 	public void showScene(ScreenMode scene) {
 		this.currentScene = scene;
 		String sceneName = capitalize(scene.toString().toLowerCase());
 		super.startScene(sceneName);
 	}
 
 	/**
 	 * Determines if the scene currently displayed is the one specified.
 	 * 
 	 * @param scene
 	 *            the scene to check
 	 * @return true if the scene specified is currently displayed; false
 	 *         otherwise.
 	 */
 	public boolean isShowingScene(ScreenMode scene) {
 		return currentScene.equals(scene);
 	}
 
 	private String capitalize(String s) {
 		if (s.length() == 0)
 			return s;
 		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
 	}
 }
