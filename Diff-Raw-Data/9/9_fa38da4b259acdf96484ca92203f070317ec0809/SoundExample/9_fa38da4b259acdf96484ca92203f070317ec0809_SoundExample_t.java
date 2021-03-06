 package org.anddev.andengine.examples;
 
 import java.io.IOException;
 
 import org.anddev.andengine.audio.sound.Sound;
 import org.anddev.andengine.audio.sound.SoundFactory;
 import org.anddev.andengine.engine.Engine;
 import org.anddev.andengine.engine.camera.Camera;
 import org.anddev.andengine.engine.options.EngineOptions;
 import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
 import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
 import org.anddev.andengine.entity.FPSCounter;
 import org.anddev.andengine.entity.Scene;
 import org.anddev.andengine.entity.sprite.Sprite;
 import org.anddev.andengine.input.touch.IOnAreaTouchListener;
 import org.anddev.andengine.input.touch.ITouchArea;
 import org.anddev.andengine.opengl.texture.Texture;
 import org.anddev.andengine.opengl.texture.region.TextureRegion;
 import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
 import org.anddev.andengine.util.Debug;
 
 import android.view.MotionEvent;
 import android.widget.Toast;
 
 /**
  * @author Nicolas Gramlich
 * @since 11:54:51 - 11.06.2010
  */
 public class SoundExample extends BaseExampleGameActivity {
 	// ===========================================================
 	// Constants
 	// ===========================================================
 
 	private static final int CAMERA_WIDTH = 720;
 	private static final int CAMERA_HEIGHT = 480;
 
 	// ===========================================================
 	// Fields
 	// ===========================================================
 
 	private Texture mTexture;
 	private TextureRegion mTankTextureRegion;
 
 	private Sound mExplosionSound;
 
 	// ===========================================================
 	// Constructors
 	// ===========================================================
 
 	// ===========================================================
 	// Getter & Setter
 	// ===========================================================
 
 	// ===========================================================
 	// Methods for/from SuperClass/Interfaces
 	// ===========================================================
 
 	@Override
 	public Engine onLoadEngine() {
 		Toast.makeText(this, "Touch the tank to hear an explosion sound.", Toast.LENGTH_LONG).show();
 		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
 		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera, true));
 	}
 
 	@Override
 	public void onLoadResources() {
 		this.mTexture = new Texture(128, 256);
 		TextureRegionFactory.setAssetBasePath("gfx/");
 		this.mTankTextureRegion = TextureRegionFactory.createFromAsset(this.mTexture, this, "tank.png", 0, 0);
 
 		SoundFactory.setAssetBasePath("mfx/");
 		try {
 			this.mExplosionSound = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "explosion.ogg");
 		} catch (final IOException e) {
 			Debug.e("Error", e);
 		}
 
 		this.getEngine().getTextureManager().loadTexture(this.mTexture);
 	}
 
 	@Override
 	public Scene onLoadScene() {
 		this.getEngine().registerPreFrameHandler(new FPSCounter());
 
 		final Scene scene = new Scene(1);
 		scene.setBackgroundColor(0.09804f, 0.6274f, 0.8784f);
 
 		final int x = (CAMERA_WIDTH - this.mTankTextureRegion.getWidth()) / 2;
 		final int y = (CAMERA_HEIGHT - this.mTankTextureRegion.getHeight()) / 2;
 		final Sprite tank = new Sprite(x, y, this.mTankTextureRegion);
 		scene.getTopLayer().addEntity(tank);
 
 		scene.registerTouchArea(tank);
 		scene.setOnAreaTouchListener(new IOnAreaTouchListener() {
 			@Override
 			public boolean onAreaTouched(final ITouchArea pTouchArea, final MotionEvent pSceneMotionEvent) {
 				if(pSceneMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
 					SoundExample.this.mExplosionSound.play();
 				}
 				return true;
 			}
 		});
 
 		return scene;
 	}
 
 	@Override
 	public void onLoadComplete() {
 
 	}
 
 	// ===========================================================
 	// Methods
 	// ===========================================================
 
 	// ===========================================================
 	// Inner and Anonymous Classes
 	// ===========================================================
 }
