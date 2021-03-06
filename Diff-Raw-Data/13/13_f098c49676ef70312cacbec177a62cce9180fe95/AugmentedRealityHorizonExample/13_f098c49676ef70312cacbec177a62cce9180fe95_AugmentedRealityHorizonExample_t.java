 package org.anddev.andengine.examples;
 
 import org.anddev.andengine.engine.Engine;
 import org.anddev.andengine.engine.camera.Camera;
 import org.anddev.andengine.engine.options.EngineOptions;
 import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
 import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
 import org.anddev.andengine.entity.Scene;
 import org.anddev.andengine.entity.sprite.Sprite;
 import org.anddev.andengine.extension.augmentedreality.BaseAugmentedRealityGameActivity;
 import org.anddev.andengine.opengl.texture.Texture;
 import org.anddev.andengine.opengl.texture.region.TextureRegion;
 import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
 import org.anddev.andengine.sensor.orientation.IOrientationListener;
 import org.anddev.andengine.sensor.orientation.OrientationData;
 import org.anddev.andengine.util.Debug;
 
import android.widget.Toast;

 /**
  * @author Nicolas Gramlich
  * @since 11:54:51 - 03.04.2010
  */
 public class AugmentedRealityHorizonExample extends BaseAugmentedRealityGameActivity implements IOrientationListener {
 	// ===========================================================
 	// Constants
 	// ===========================================================
 
 	private static final int CAMERA_WIDTH = 720;
 	private static final int CAMERA_HEIGHT = 480;
 
 	// ===========================================================
 	// Fields
 	// ===========================================================
 
 	private Camera mCamera;
 	private Texture mTexture;
 	private TextureRegion mFaceTextureRegion;
 	private Sprite mFace;
 
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
		Toast.makeText(this, "If you don't see a sprite moving over the screen, try starting this while already being in Landscape orientation!!", Toast.LENGTH_LONG).show();
 		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
 		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera, false));
 	}
 
 	@Override
 	public void onLoadResources() {
 		this.mTexture = new Texture(64, 32);
 		this.mFaceTextureRegion = TextureRegionFactory.createFromAsset(this.mTexture, this, "gfx/boxface.png", 0, 0);
 
 		this.getEngine().getTextureManager().loadTexture(this.mTexture);
 	}
 
 	@Override
 	public Scene onLoadScene() {
 		//		this.getEngine().registerPreFrameHandler(new FPSCounter());
 
 		final Scene scene = new Scene(1);
 		//		scene.setBackgroundEnabled(false);
 		scene.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.0f);
 
 		final int x = (CAMERA_WIDTH - this.mFaceTextureRegion.getWidth()) / 2;
 		final int y = (CAMERA_HEIGHT - this.mFaceTextureRegion.getHeight()) / 2;
 		mFace = new Sprite(x, y, this.mFaceTextureRegion);
 //		face.addShapeModifier(new MoveModifier(30, 0, CAMERA_WIDTH - face.getWidth(), 0, CAMERA_HEIGHT - face.getHeight()));
 		scene.getTopLayer().addEntity(mFace);
 
 		return scene;
 	}
 
 	@Override
 	public void onLoadComplete() {
 		this.enableOrientationSensor(this);
 	}
 
 	@Override
 	public void onOrientationChanged(final OrientationData pOrientationData) {
 		final float roll = pOrientationData.getRoll();
 		Debug.d("Roll: " + pOrientationData.getRoll());
 		
 		mFace.setPosition(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2 + (roll - 40) * 5);
 	}
 
 	// ===========================================================
 	// Methods
 	// ===========================================================
 
 	// ===========================================================
 	// Inner and Anonymous Classes
 	// ===========================================================
 }
