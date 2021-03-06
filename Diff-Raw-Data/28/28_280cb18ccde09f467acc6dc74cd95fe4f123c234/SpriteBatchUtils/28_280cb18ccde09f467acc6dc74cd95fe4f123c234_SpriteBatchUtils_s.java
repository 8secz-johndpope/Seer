 package com.gemserk.commons.gdx.graphics;
 
 import com.badlogic.gdx.graphics.g2d.BitmapFont;
 import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
 import com.badlogic.gdx.graphics.g2d.Sprite;
 import com.badlogic.gdx.graphics.g2d.SpriteBatch;
 
 public class SpriteBatchUtils {
 
 	/**
	 * Used to define how to center the text box over the (x,y) coordinates, not for internal text alignment, for that BitmapFont has HAlignment.
	 */
	public static enum Align {
		Center, Left, Right
	};

	/**
 	 * Draws a text centered in the specified coordinates
 	 */
 	public static void drawCentered(SpriteBatch spriteBatch, BitmapFont font, String text, float x, float y) {
 		TextBounds bounds = font.getBounds(text);
 		font.draw(spriteBatch, text, x - bounds.width * 0.5f, y + bounds.height * 0.5f);
 	}
 
 	/**
 	 * Draws a multi line text centered.
 	 */
 	public static void drawMultilineTextCentered(SpriteBatch spriteBatch, BitmapFont font, String text, float x, float y) {
 		drawMultilineText(spriteBatch, font, text, x, y, 0.5f, 0.5f);
 	}
 
 	/**
 	 * Draws a multi line text in the specified coordinates (x,y). It will use cx and cy to center the text over the coordinates (x,y).
 	 * 
 	 * @param spriteBatch
 	 * @param font
 	 * @param text
 	 * @param x
 	 * @param y
 	 * @param cx
 	 *            A value between 0 and 1 to center the text over the horizontal axis.
 	 * @param cy
 	 *            A value between 0 and 1 to center the text over the vertical axis.
 	 */
 	public static void drawMultilineText(SpriteBatch spriteBatch, BitmapFont font, String text, float x, float y, float cx, float cy) {
 		TextBounds bounds = font.getMultiLineBounds(text);
		font.drawMultiLine(spriteBatch, text, x + bounds.width * cx, y + bounds.height * cy);
 	}
 
 	/**
 	 * Draws a Sprite centered.
 	 */
 	public static void drawCentered(SpriteBatch spriteBatch, Sprite sprite, float x, float y, float w, float h, float angle) {
 		sprite.setSize(w, h);
 		sprite.setOrigin(w * 0.5f, h * 0.5f);
 		sprite.setPosition(x - sprite.getOriginX(), y - sprite.getOriginY());
 		sprite.setRotation(angle);
 		sprite.draw(spriteBatch);
 	}
 
 	/**
 	 * Draws a Sprite centered.
 	 */
 	public static void drawCentered(SpriteBatch spriteBatch, Sprite sprite, float x, float y, float angle) {
 		drawCentered(spriteBatch, sprite, x, y, sprite.getWidth(), sprite.getHeight(), angle);
 	}
 
 	/**
 	 * If text width exceeds viewportWidth * limit, it returns the corresponding scale to make the textWidht be equal to viewportWidth * limit.
 	 */
 	public static float calculateScaleForText(float viewportWidth, float textWidth, float limit) {
 		if (textWidth < viewportWidth * limit)
 			return 1f;
 		return viewportWidth / textWidth * limit;
 	}
 
 }
