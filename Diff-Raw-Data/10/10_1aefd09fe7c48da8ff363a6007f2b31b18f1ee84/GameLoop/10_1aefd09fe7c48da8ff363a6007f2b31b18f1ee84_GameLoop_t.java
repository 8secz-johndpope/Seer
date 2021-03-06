 package eu.lestard.snakefx.core;
 
 import javafx.animation.Animation;
 import javafx.animation.Animation.Status;
 import javafx.animation.KeyFrame;
 import javafx.animation.Timeline;
 import javafx.animation.TimelineBuilder;
 import javafx.beans.property.ReadOnlyBooleanProperty;
 import javafx.beans.value.ChangeListener;
 import javafx.beans.value.ObservableValue;
 import javafx.event.ActionEvent;
 import javafx.event.EventHandler;
 import javafx.util.Duration;
 
 /**
  * This class is the game loop of the game.
  * 
  * @author manuel.mauky
  * 
  */
 public class GameLoop {
 
 	private static final int ONE_SECOND = 1000;
 
 	private int fps = SpeedLevel.SLOW.getFps();
 
 	private Timeline timeline;
 
 	private final Snake snake;
 
 	public GameLoop(final Snake snake) {
 		this.snake = snake;
 
 		ReadOnlyBooleanProperty collisionProperty = snake.collisionProperty();
 		collisionProperty.addListener(new ChangeListener<Boolean>() {
 			@Override
 			public void changed(final ObservableValue<? extends Boolean> arg0,
 					final Boolean oldValue, final Boolean newValue) {
 				if (newValue) {
 					timeline.stop();
 				}
 			}
 		});
 	}
 
 	/**
 	 * Initialize the timeline instance.
 	 */
 	public void init() {
 		timeline = TimelineBuilder.create().cycleCount(Animation.INDEFINITE)
 				.keyFrames(buildKeyFrame()).build();
 	}
 
 	/**
 	 * This method creates a {@link KeyFrame} instance according to the
 	 * configured framerate.
 	 * 
 	 * The KeyFrame handles the movement of the snake.
 	 * 
 	 * @return
 	 */
 	private KeyFrame buildKeyFrame() {
 
 		Duration duration = Duration.millis(ONE_SECOND / fps);
 
 		KeyFrame frame = new KeyFrame(duration,
 				new EventHandler<ActionEvent>() {
 					@Override
 					public void handle(final ActionEvent arg0) {
 						snake.move();
 					}
 				});
 
 		return frame;
 	}
 
 	/**
 	 * Change the FPS of the GameLoop.
 	 * 
 	 * <bold>Hint:</bold> Changing the FPS of the Gameloop doesn't change the
 	 * speed of the Gameloop directly but only sets a new value for the fps
 	 * property.
 	 * 
 	 * To change the speed of the GameLoops timeline instance you have to
 	 * rebuild the timeline instance after you have set the new fps value. Do
 	 * this by calling {@link GameLoop#init()}.
 	 * 
 	 * @param fps
 	 */
 	public void setFps(final int fps) {
 		this.fps = fps;
 	}
 
 	/**
 	 * delegate to the play method of the timeline.
 	 */
 	public void play() {
 		timeline.play();
 	}
 
 	/**
 	 * delegate to the pause method of the timeline.
 	 */
 	public void pause() {
 		timeline.pause();
 	}
 
 	/**
 	 * delegate to the pause method of the timeline.
 	 */
 	public void stop() {
		if (timeline != null) {
			timeline.stop();
		}
 	}
 
 	/**
 	 * delegate to the getStatus method of the timeline.
 	 * 
 	 * @return
 	 */
 	public Status getStatus() {
 		return timeline.getStatus();
 	}
 
 }
