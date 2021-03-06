 package vialab.SMT;
 
 import java.awt.event.KeyEvent;
 
 import processing.core.PFont;
 
 public class TextZone extends Zone {
 
 	private class WordZone extends Zone {
 
 		public String word;
 		private boolean selected = false;
 
 		public WordZone(int x, int y, int width, int height) {
 			super(x, y, width, height);
 			this.word = "";
 			TextZone.this.add(this);
 			this.setDirect(true);
 		}
 
 		@Override
		public void touchUp(Touch touch) {
			if (this.getTouches().isEmpty()) {
				selected = !selected;
			}
			super.touchUp(touch);
 		}
 
 		@Override
 		public void draw() {
 			super.beginDraw();
 			fill(255);
 			if(selected){
 				fill(255,0,0);
 			}
 			rect(0, 0, width, height);
 			fill(0);
 			text(this.word, 0, 5, width, height);
 			super.endDraw();
 			super.draw();
 		}
 
 	}
 
 	public String text = "";
 
 	private WordZone currentWordZone;
 
 	private PFont font = applet.createFont("Arial", 16);
 
 	public TextZone(int x, int y, int width, int height) {
 		this(null, x, y, width, height, false);
 	}
 
 	public TextZone(int x, int y, int width, int height, boolean keysRecievedFromApplet) {
 		this(null, x, y, width, height, keysRecievedFromApplet);
 	}
 
 	public TextZone(String name, int x, int y, int width, int height) {
 		this(name, x, y, width, height, false);
 	}
 
 	public TextZone(String name, int x, int y, int width, int height, boolean keysRecievedFromApplet) {
 		super(name, x, y, width, height);
 		this.currentWordZone = new WordZone(0, 0, 1, 20);
 		if (keysRecievedFromApplet) {
 			applet.registerKeyEvent(this);
 		}
 
 	}
 
 	@Override
 	public void draw() {
 		super.beginDraw();
 		fill(255);
 		rect(0, 0, width, height);
 		super.endDraw();
 		super.draw();
 	}
 
 	@Override
 	public void keyTyped(KeyEvent e) {
 		textFont(font);
 		if (e.getKeyChar() == ' ') {
 			this.currentWordZone = new WordZone(currentWordZone.x
 					+ (int) textWidth(currentWordZone.word + e.getKeyChar()), currentWordZone.y, 1, 20);
 		}
 		else if (e.getKeyChar() == '\t') {
 			this.currentWordZone = new WordZone(currentWordZone.x
 					+ (int) textWidth(currentWordZone.word + e.getKeyChar()), currentWordZone.y, 1, 20);
 		}
 		else if (e.getKeyChar() == '\n') {
 			this.currentWordZone = new WordZone(0, currentWordZone.y + 20, 1, 20);
 		}
 		else {
 			this.currentWordZone.word += e.getKeyChar();
 			this.currentWordZone.setData(currentWordZone.x, currentWordZone.y,
 					(int) textWidth(currentWordZone.word), currentWordZone.height);
 		}
 		super.keyTyped(e);
 	}
 
 	public void keyEvent(KeyEvent event) {
 		switch (event.getID()) {
 		case KeyEvent.KEY_RELEASED:
 			keyReleased(event);
 			break;
 		case KeyEvent.KEY_TYPED:
 			keyTyped(event);
 			break;
 		case KeyEvent.KEY_PRESSED:
 			keyPressed(event);
 			break;
 		}
 	}
 }
