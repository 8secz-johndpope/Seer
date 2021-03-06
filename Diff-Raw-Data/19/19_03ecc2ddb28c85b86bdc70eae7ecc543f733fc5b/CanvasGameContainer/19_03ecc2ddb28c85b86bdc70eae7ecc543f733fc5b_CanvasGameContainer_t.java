 package org.newdawn.slick;
 
 import java.awt.Canvas;
 
 import org.lwjgl.LWJGLException;
 import org.lwjgl.opengl.Display;
 import org.newdawn.slick.util.Log;
 
 /**
  * A game container that displays the game on an AWT Canvas.
  * 
  * @author kevin
  */
 public class CanvasGameContainer extends Canvas {
 	/** The actual container implementation */
 	protected Container container;
 
 	/** The game being held in this container */
 	protected Game game;
 
 	/** True if a reinit is required */
 	protected boolean reinit = false;
 
 	/** True if we've already created the AWTInputAdapter */
 	protected boolean createdAdapter = false;
 
 	/**
 	 * Create a new panel
 	 * 
	 * @param game The game being held
	 * @throws SlickException Indicates a failure during creation of the container
 	 */
	public CanvasGameContainer(Game game) throws SlickException {
 		super();
 
 		this.game = game;
 		
 		requestFocus();
 		setIgnoreRepaint(true);
 		setSize(500,500);
		
		container = new Container(game);
 	}
 
 	/**
 	 * Start the game container rendering
 	 * 
 	 * @throws SlickException Indicates a failure during game execution
 	 */
 	public void start() throws SlickException {
 		try {
 			Display.setParent(this);
 		} catch (LWJGLException e) {
 			throw new SlickException("Failed to setParent of canvas", e);
 		}
 		container.start();
 	}
 	
 	/**
 	 * Dispose the container and any resources it holds
 	 */
 	public void dispose() {
 	}
 
 	/**
 	 * Get the GameContainer providing this canvas
 	 * 
 	 * @return The game container providing this canvas
 	 */
 	public GameContainer getContainer() {
 		return container;
 	}
 
 	/**
 	 * A game container to provide the canvas context
 	 * 
 	 * @author kevin
 	 */
 	private class Container extends AppGameContainer {
 		/**
 		 * Create a new container wrapped round the game
 		 * 
 		 * @param game
 		 *            The game to be held in this container
 		 * @throws SlickException Indicates a failure to initialise
 		 */
 		public Container(Game game) throws SlickException {
 			super(game, CanvasGameContainer.this.getWidth(), CanvasGameContainer.this.getHeight(), false);
 
 			width = CanvasGameContainer.this.getWidth();
 			height = CanvasGameContainer.this.getHeight();
 		}
 
 		/**
 		 * Updated the FPS counter
 		 */
 		protected void updateFPS() {
 			super.updateFPS();
 			
 			if ((width != CanvasGameContainer.this.getWidth()) ||
 			   (height != CanvasGameContainer.this.getHeight())) {
 
 				try {
 					setDisplayMode(CanvasGameContainer.this.getWidth(), 
 								   CanvasGameContainer.this.getHeight(), false);
 				} catch (SlickException e) {
 					Log.error(e);
 				}
 			}
 		}
 
 		/**
 		 * @see org.newdawn.slick.GameContainer#running()
 		 */
 		protected boolean running() {
 			return CanvasGameContainer.this.isDisplayable();
 		}
 	}
 }
