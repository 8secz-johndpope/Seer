 package com.cdm.view;
 
 import com.badlogic.gdx.Gdx;
 import com.badlogic.gdx.audio.Sound;
 import com.badlogic.gdx.graphics.GL10;
 import com.badlogic.gdx.graphics.OrthographicCamera;
 import com.badlogic.gdx.graphics.Texture;
 import com.badlogic.gdx.graphics.g2d.SpriteBatch;
 import com.badlogic.gdx.graphics.g2d.TextureRegion;
 import com.cdm.SString;
 import com.cdm.Settings;
 import com.cdm.gui.Button;
 import com.cdm.gui.IButtonPressed;
 import com.cdm.gui.IUnitTypeSelected;
 import com.cdm.gui.UnitTypeButton;
 import com.cdm.gui.WidgetContainer;
 import com.cdm.view.Position.RefSystem;
 import com.cdm.view.elements.Elements;
 import com.cdm.view.elements.Level;
 import com.cdm.view.elements.Unit;
 import com.cdm.view.elements.Unit.UnitType;
 
 public class LevelScreen extends Screen implements IUnitTypeSelected,
 		IButtonPressed {
 	public SpriteBatch spriteBatch = new SpriteBatch();
 	public static TextureRegion bg;
 	private Renderer renderer = new Renderer();
 	private Level level;
 	private WidgetContainer gui = new WidgetContainer();
 	private Unit dragElement = null;
 	private LevelDisplays display = new LevelDisplays();
 	private boolean rendering = false;
 	private OrthographicCamera cam;
 	Position dragPosition = new Position(0, 0, RefSystem.Screen);
 	Position oldDragPosition = new Position(0, 0, RefSystem.Screen);
 	Sound sound;
 
 	public LevelScreen() {
 
 		level = new Level(10, 6, 3);
 		display.setLevel(level);
 		bg = load("data/bg_stars2.png", 128, 128);
 
 		createUnitButtons();
 
 		Button sizeButton = new Button(300, 400, 30);
 		sizeButton.setButtonName(SString.SIZE_BUTTON);
 		sizeButton.setPressedListener(this);
 		gui.add(sizeButton);
 		createCam();
 	}
 
 	public void dispose() {
 		renderer.dispose();
 	}
 
 	private void createUnitButtons() {
 		float pos = 40;
 		UnitTypeButton tb;
 		for (UnitType t : new UnitType[] { UnitType.CANNON, UnitType.STUNNER,
 				UnitType.ROCKET_THROWER }) {
 			tb = new UnitTypeButton((int) pos, 400, 30, t, level);
 			tb.setListener(this);
 			tb.setCost(t.getCost());
 			gui.add(tb);
 
 			pos += 80;
 		}
 	}
 
 	private Long oldMicros = 0L;
 
 	@Override
 	public synchronized void render(float delta) {
 		if (rendering)
 			return;
 		rendering = true;
 		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
 		long millis = System.currentTimeMillis();
 		long micro = System.nanoTime() / 1000 + millis * 1000;
 		if (oldMicros > 0) {
 			delta = (micro - oldMicros) * 0.000001f;
 		}
 		oldMicros = micro;
 		delta += mywait(delta);
 
 		if (false) {
 			System.out.print("FPS:");
 			System.out.println(1.0f / delta);
 		}
 
 		spriteBatch.begin();
 
 		for (int x = 0; x < 16; x++)
 			for (int y = 0; y < 16; y++)
 				draw(bg, x * 128, y * 128);
 
 		spriteBatch.end();
 
 		drawLineBased(delta);
 		display.draw(renderer);
 		rendering = false;
 
 	}
 
 	private void createCam() {
 		cam = new OrthographicCamera(Gdx.graphics.getWidth(),
 				Gdx.graphics.getHeight());
		cam.position.set(Gdx.graphics.getWidth() / 2,
 				Gdx.graphics.getHeight() / 2, 0);
 		cam.update();
 
 	}
 
 	private void drawLineBased(float delta) {
 		if (delta > 0) {
 			level.move(delta);
 		}
 		cam.apply(Gdx.gl10);
 
 		// Gdx.gl10.glScalef(1, -1, 0);
 		// Gdx.gl10.glTranslatef(0, -Gdx.graphics.getHeight(), 0);
 
 		level.draw(renderer);
 		gui.addTime(delta);
 		gui.draw(renderer);
 
 		if (dragElement != null) {
 			dragElement.draw(renderer);
 		}
 
 	}
 
 	static final int CIRCLE_VERTICES = 10;
 
 	private float mywait(float delta) {
 		if (true)
 			return 0;
 		try {
 			Integer ms = (int) (delta * 1000);
 			// ~ 50 fps
 			// TODO: sleep shorter, if rendering does need more time
 			Long millis0 = System.currentTimeMillis();
 			Long micros0 = System.nanoTime() / 1000;
 
 			int wait = 15 - ms;
 			if (wait > 5) {
 				Thread.sleep(wait);
 				// System.out.println(wait);
 			}
 			Long millis1 = System.currentTimeMillis();
 			Long micros1 = System.nanoTime() / 1000;
 			return ((millis1 - millis0) * 1000 + (micros1 - micros0)) / 1000000.0f;
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 		return 0;
 	}
 
 	public void draw(TextureRegion region, int x, int y) {
 		int width = region.getRegionWidth();
 		if (width < 0)
 			width = -width;
 		spriteBatch.draw(region, x, y, width, -region.getRegionHeight());
 	}
 
 	public static TextureRegion load(String name, int width, int height) {
 		Texture texture = new Texture(Gdx.files.internal(name));
 		TextureRegion region = new TextureRegion(texture, 0, 0, width, height);
 		region.flip(false, true);
 		return region;
 	}
 
 	public boolean touchDown(int x, int y, int pointer, int button) {
 		if (level.gameover())
 			return false;
 		y = Gdx.graphics.getHeight() - y;
 		if (gui.opaque(x, y)) {
 			gui.touchDown(x, y, pointer, button);
 			return true;
 		} else {
 			dragPosition.set(x, y, RefSystem.Screen);
 			oldDragPosition.set(x, y, RefSystem.Screen);
 		}
 		return false;
 	}
 
 	public boolean touchUp(int x, int y, int pointer, int button) {
 		if (level.gameover())
 			return false;
 		y = Gdx.graphics.getHeight() - y;
 		if (gui.opaque(x, y)) {
 			gui.touchUp(x, y, pointer, button);
 			return true;
 		}
 		stopDragging();
 		level.stopHover();
 		return false;
 	}
 
 	private void stopDragging() {
 		if (level.gameover())
 			return;
 		if (dragElement != null) {
 			level.add(dragElement);
 		}
 		dragElement = null;
 	}
 
 	public boolean touchDragged(int x, int y, int pointer) {
 		if (level.gameover())
 			return false;
 		y = Gdx.graphics.getHeight() - y;
 
 		dragPosition.set(x, y, RefSystem.Screen);
 		if (dragElement != null) {
 			dragElement.setPosition(dragPosition);
 			level.hover(dragPosition);
 		} else {
 			int dx = (int)(dragPosition.x - oldDragPosition.x);
 			int dy = (int)(dragPosition.y - oldDragPosition.y);
 			System.out.println("DX " + dx + " DY" + dy);
 			oldDragPosition.set(dragPosition);
 		}
 		return false;
 	}
 
 	@Override
 	public boolean touchMoved(int x, int y) {
 		y = Gdx.graphics.getHeight() - y;
 		return false;
 	}
 
 	@Override
 	public boolean scrolled(int amount) {
 		return false;
 	}
 
 	@Override
 	public void unitTypeSelected(UnitType type, Position screenPos, int cost) {
 		dragElement = Elements.getElementBy(type, screenPos);
 		dragElement.setCost(cost);
 
 	}
 
 	@Override
 	public void buttonPressed(SString buttonName) {
 		if (buttonName.equals(SString.SIZE_BUTTON)) {
 			sound = Gdx.audio.newSound(Gdx.files.internal("data/zoom.ogg"));
 			sound.play();
 			if (Settings.getCellWidth() == 32) {
 				Settings.setScale(64);
 			} else
 				Settings.setScale(32);
 		}
 	}
 }
