 /*
  * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are
  * met:
  * 
  * - Redistribution of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  * 
  * - Redistribution in binary form must reproduce the above copyright
  *   notice, this list of conditions and the following disclaimer in the
  *   documentation and/or other materials provided with the distribution.
  * 
  * Neither the name of Sun Microsystems, Inc. or the names of
  * contributors may be used to endorse or promote products derived from
  * this software without specific prior written permission.
  * 
  * This software is provided "AS IS," without a warranty of any kind. ALL
  * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
  * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
  * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
  * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
  * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
  * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
  * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
  * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
  * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
  * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
  * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  * 
  * You acknowledge that this software is not designed or intended for use
  * in the design, construction, operation or maintenance of any nuclear
  * facility.
  * 
  * Sun gratefully acknowledges that this software was originally authored
  * and developed by Kenneth Bradley Russell and Christopher John Kline.
  */
 
 package com.sun.opengl.util.j2d;
 
 import java.awt.AlphaComposite;
 import java.awt.Color;
 import java.awt.Font;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.Point;
 import java.awt.RenderingHints;
 import java.awt.font.*;
 import java.awt.geom.*;
 import java.util.*;
 
 import javax.media.opengl.*;
 import javax.media.opengl.glu.*;
 import com.sun.opengl.impl.packrect.*;
 
 // For debugging purposes
 import java.awt.EventQueue;
 import java.awt.Frame;
 import java.awt.event.*;
 import com.sun.opengl.impl.*;
 import com.sun.opengl.util.*;
 
 /** Renders bitmapped Java 2D text into an OpenGL window with high
     performance, full Unicode support, and a simple API. Performs
     appropriate caching of text rendering results in an OpenGL texture
     internally to avoid repeated font rasterization. The caching is
     completely automatic, does not require any user intervention, and
     has no visible controls in the public API. <P>
 
     Using the {@link TextRenderer TextRenderer} is simple. Add a
     "<code>TextRenderer renderer;</code>" field to your {@link
     GLEventListener GLEventListener}. In your {@link
     GLEventListener#init init} method, add:
 
 <PRE>
     renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 </PRE>
 
     <P> In the {@link GLEventListener#display display} method of your
     {@link GLEventListener GLEventListener}, add:
 <PRE>
     renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
     // optionally set the color
     renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
     renderer.draw("Text to draw", xPosition, yPosition);
     // ... more draw commands, color changes, etc.
     renderer.endRendering();
 </PRE>
 
     Internally, the renderer uses a rectangle packing algorithm to
     pack multiple full Strings' rendering results (which are variable
     size) onto a larger OpenGL texture. The internal backing store is
     maintained using a {@link TextureRenderer TextureRenderer}. A
     least recently used (LRU) algorithm is used to discard previously
     rendered strings; the specific algorithm is undefined, but is
     currently implemented by flushing unused Strings' rendering
     results every few hundred rendering cycles, where a rendering
     cycle is defined as a pair of calls to {@link #beginRendering
     beginRendering} / {@link #endRendering endRendering}.
 */
 
 public class TextRenderer {
   private static final boolean DEBUG = Debug.debug("TextRenderer");
 
   private Font font;
   private boolean antialiased;
   private boolean useFractionalMetrics;
 
   private RectanglePacker packer;
   private boolean haveMaxSize;
   private TextureRenderer cachedBackingStore;
   private Graphics2D cachedGraphics;
   private FontRenderContext cachedFontRenderContext;
   private Map/*<String,Rect>*/ stringLocations = new HashMap/*<String,Rect>*/();
   private static final Color TRANSPARENT_BLACK = new Color(0.0f, 0.0f, 0.0f, 0.0f);
 
   // Support tokenization of space-separated words
   // NOTE: not exposing this at the present time as we aren't
   // producing identical (or even vaguely similar) rendering results;
   // may ultimately yield more efficient use of the backing store, but
   // also seems to have performance issues due to rendering more quads
   private boolean splitAtSpaces;
   private int spaceWidth = -1;
   private List/*<String>*/ tokenizationResults = new ArrayList/*<String>*/();
 
   // Every certain number of render cycles, flush the strings which
   // haven't been used recently
   private static final int CYCLES_PER_FLUSH = 100;
   private int numRenderCycles;
   // The amount of vertical dead space on the backing store before we
   // force a compaction
   private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;
 
   // Current text color
   private float r = 1.0f;
   private float g = 1.0f;
   private float b = 1.0f;
   private float a = 1.0f;
 
   // Data associated with each rectangle of text
   static class TextData {
     private String str;    // Back-pointer to String this TextData describes
     // The following must be defined and used VERY precisely. This is
     // the offset from the upper-left corner of this rectangle (Java
     // 2D coordinate system) at which the string must be rasterized in
     // order to fit within the rectangle -- the leftmost point of the
     // baseline.
     private Point origin;
     private boolean used;  // Whether this text was used recently
 
     TextData(String str, Point origin) {
       this.str = str;
       this.origin = origin;
     }
 
     String string()  { return str;    }
     Point origin()   { return origin; }
     boolean used()   { return used;   }
     void markUsed()  { used = true;   }
     void clearUsed() { used = false;  }
   }
 
   // Debugging purposes only
   private boolean debugged;
 
   /** Creates a new TextRenderer with the given font, using no
       antialiasing or fractional metrics. Equivalent to
       <code>TextRenderer(font, false, false)</code>.
 
       @param font the font to render with
   */      
   public TextRenderer(Font font) {
     this(font, false, false);
   }
 
   /** Creates a new TextRenderer with the given Font and specified
       font properties. The <code>antialiased</code> and
       <code>useFractionalMetrics</code> flags provide control over the
       same properties at the Java 2D level.
 
       @param font the font to render with
       @param antialiased whether to use antialiased fonts
       @param useFractionalMetrics whether to use fractional font
         metrics at the Java 2D level
   */
   public TextRenderer(Font font,
                       boolean antialiased,
                       boolean useFractionalMetrics) {
     this.font = font;
     this.antialiased = antialiased;
     this.useFractionalMetrics = useFractionalMetrics;
 
     // FIXME: consider adjusting the size based on font size
     // (it will already automatically resize if necessary)
     packer = new RectanglePacker(new Manager(), 256, 256);
   }
 
   /** Returns the bounding rectangle of the given String, assuming it
       was rendered at the origin. The coordinate system of the
       returned rectangle is Java 2D's, with increasing Y coordinates
       in the downward direction. The relative coordinate (0, 0) in the
       returned rectangle corresponds to the baseline of the leftmost
       character of the rendered string, in similar fashion to the
       results returned by, for example, {@link
       GlyphVector#getVisualBounds}. Most applications will use only
       the width and height of the returned Rectangle for the purposes
       of centering or justifying the String. It is not specified which
       Java 2D bounds ({@link GlyphVector#getVisualBounds
       getVisualBounds}, {@link GlyphVector#getPixelBounds
       getPixelBounds}, etc.) the returned bounds correspond to,
       although every effort is made to ensure an accurate bound. */
   public Rectangle2D getBounds(String str) {
     // FIXME: this doesn't hit the cache if tokenization is enabled --
     // needs more work
     // Prefer a more optimized approach
     Rect r = null;
     if ((r = (Rect) stringLocations.get(str)) != null) {
       TextData data = (TextData) r.getUserData();
       // Reconstitute the Java 2D results based on the cached values
       return new Rectangle2D.Double(-data.origin().x,
                                     -data.origin().y,
                                     r.w(), r.h());
     }
 
     FontRenderContext frc = getFontRenderContext();
     GlyphVector gv = font.createGlyphVector(frc, str);
     // Must return a Rectangle compatible with the layout algorithm --
     // must be idempotent
     return normalize(gv.getPixelBounds(frc, 0, 0));
   }
 
   /** Returns the Font this renderer is using. */
   public Font getFont() {
     return font;
   }
 
   /** Returns a FontRenderContext which can be used for external
       text-related size computations. This object should be considered
       transient and may become invalidated between {@link
       #beginRendering beginRendering} / {@link #endRendering
       endRendering} pairs. */
   public FontRenderContext getFontRenderContext() {
     if (cachedFontRenderContext == null) {
       cachedFontRenderContext = getGraphics2D().getFontRenderContext();
     }
     return cachedFontRenderContext;
   }
 
   /** Begins rendering with this {@link TextRenderer TextRenderer}
       into the current OpenGL drawable, pushing the projection and
       modelview matrices and some state bits and setting up a
       two-dimensional orthographic projection with (0, 0) as the
       lower-left coordinate and (width, height) as the upper-right
       coordinate. Binds and enables the internal OpenGL texture
       object, sets the texture environment mode to GL_MODULATE, and
       changes the current color to the last color set with this
       TextRenderer via {@link #setColor setColor}.
 
       @param width the width of the current on-screen OpenGL drawable
       @param height the height of the current on-screen OpenGL drawable
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void beginRendering(int width, int height) throws GLException {
     beginRendering(true, width, height);
   }
 
   /** Begins rendering of 2D text in 3D with this {@link TextRenderer
       TextRenderer} into the current OpenGL drawable. Assumes the end
       user is responsible for setting up the modelview and projection
       matrices, and will render text using the {@link #draw3D draw3D}
       method. This method pushes some OpenGL state bits, binds and
       enables the internal OpenGL texture object, sets the texture
       environment mode to GL_MODULATE, and changes the current color
       to the last color set with this TextRenderer via {@link
       #setColor setColor}.
 
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void begin3DRendering() throws GLException {
     beginRendering(false, 0, 0);
   }
 
   private float[] compArray;
   /** Changes the current color of this TextRenderer to the supplied
       one. The default color is opaque white.
 
       @param color the new color to use for rendering text
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void setColor(Color color) throws GLException {
     // Get color's RGBA components as floats in the range [0,1].
     if (compArray == null) {
       compArray = new float[4];
     }
     color.getRGBComponents(compArray);
     setColor(compArray[0], compArray[1], compArray[2], compArray[3]);
   }
 
   /** Changes the current color of this TextRenderer to the supplied
       one, where each component ranges from 0.0f - 1.0f. The alpha
       component, if used, does not need to be premultiplied into the
       color channels as described in the documentation for {@link
      com.sun.opengl.util.texture.Texture Texture}, although
      premultiplied colors are used internally. The default color is
      opaque white.
 
       @param r the red component of the new color
       @param g the green component of the new color
       @param b the blue component of the new color
       @param alpha the alpha component of the new color, 0.0f =
         completely transparent, 1.0f = completely opaque
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void setColor(float r, float g, float b, float a) throws GLException {
     GL gl = GLU.getCurrentGL();
     this.r = r * a;
     this.g = g * a;
     this.b = b * a;
     this.a = a;
 
     gl.glColor4f(this.r, this.g, this.b, this.a);
   }
 
   /** Draws the supplied String at the desired location using the
       renderer's current color. The baseline of the leftmost character
       is at position (x, y) specified in OpenGL coordinates, where the
       origin is at the lower-left of the drawable and the Y coordinate
       increases in the upward direction.
 
       @param str the string to draw
       @param x the x coordinate at which to draw
       @param y the y coordinate at which to draw
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void draw(String str, int x, int y) throws GLException {
     draw3D(str, x, y, 0, 1);
   }
 
   /** Draws the supplied String at the desired 3D location using the
       renderer's current color. The baseline of the leftmost character
       is placed at position (x, y, z) in the current coordinate system.
 
       @param str the string to draw
       @param x the x coordinate at which to draw
       @param y the y coordinate at which to draw
       @param z the z coordinate at which to draw
       @param scaleFactor a uniform scale factor applied to the width and height of the drawn rectangle
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void draw3D(String str,
                      float x, float y, float z,
                      float scaleFactor) {
     // Split up the string into space-separated pieces
     tokenize(str);
     int xOffset = 0;
     for (Iterator iter = tokenizationResults.iterator(); iter.hasNext(); ) {
       String curStr = (String) iter.next();
       if (curStr != null) {
         // Look up the string on the backing store
         Rect rect = (Rect) stringLocations.get(curStr);
         if (rect == null) {
           // Rasterize this string and place it on the backing store
           Graphics2D g = getGraphics2D();
           FontRenderContext frc = getFontRenderContext();
           GlyphVector gv = font.createGlyphVector(frc, curStr);
           Rectangle2D bbox = normalize(gv.getPixelBounds(frc, 0, 0));
           Point origin = new Point((int) -bbox.getMinX(),
                                    (int) -bbox.getMinY());
           rect = new Rect(0, 0,
                           (int) bbox.getWidth(),
                           (int) bbox.getHeight(),
                           new TextData(curStr, origin));
           packer.add(rect);
           stringLocations.put(curStr, rect);
           // Re-fetch the Graphics2D in case the addition of the rectangle
           // caused the old backing store to be thrown away
           g = getGraphics2D();
           // OK, should now have an (x, y) for this rectangle; rasterize
           // the String
           // FIXME: need to verify that this causes the String to be
           // rasterized fully into the bounding rectangle
           int strx = rect.x() + origin.x;
           int stry = rect.y() + origin.y;
           // Clear out the area we're going to draw into
           g.setColor(TRANSPARENT_BLACK);
           g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
           g.setColor(Color.WHITE);
           // Draw the string
           g.drawString(curStr, strx, stry);
           // Sync to the OpenGL texture
           getBackingStore().sync(rect.x(), rect.y(), rect.w(), rect.h());
         }
 
         // OK, now draw the portion of the backing store to the screen
         TextureRenderer renderer = getBackingStore();
         // NOTE that the rectangles managed by the packer have their
         // origin at the upper-left but the TextureRenderer's origin is
         // at its lower left!!!
         TextData data = (TextData) rect.getUserData();
         data.markUsed();
 
         // Align the leftmost point of the baseline to the (x, y, z) coordinate requested
         renderer.draw3DRect(x - scaleFactor * (data.origin().x + xOffset),
                             y - scaleFactor * ((rect.h() - data.origin().y)),
                             z,
                             rect.x(),
                             renderer.getHeight() - rect.y() - rect.h(),
                             rect.w(), rect.h(),
                             scaleFactor);
         xOffset += rect.w() * scaleFactor;
       }
       xOffset += getSpaceWidth() * scaleFactor;
     }
   }
 
   /** Ends a render cycle with this {@link TextRenderer TextRenderer}.
       Restores the projection and modelview matrices as well as
       several OpenGL state bits. Should be paired with {@link
       #beginRendering beginRendering}.
 
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void endRendering() throws GLException {
     endRendering(true);
   }
 
   /** Ends a 3D render cycle with this {@link TextRenderer TextRenderer}.
       Restores several OpenGL state bits. Should be paired with {@link
       #begin3DRendering begin3DRendering}.
 
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void end3DRendering() throws GLException {
     endRendering(false);
   }
 
   /** Disposes of all resources this TextRenderer is using. It is not
       valid to use the TextRenderer after this method is called.
 
       @throws GLException If an OpenGL context is not current when this method is called
   */
   public void dispose() throws GLException {
     packer.dispose();
     packer = null;
     cachedBackingStore = null;
     cachedGraphics = null;
     cachedFontRenderContext = null;
   }
   
   //----------------------------------------------------------------------
   // Internals only below this point
   //
 
   private static Rectangle2D normalize(Rectangle2D src) {
     // Give ourselves a one-pixel boundary around each string in order
     // to prevent bleeding of nearby Strings due to the fact that we
     // use linear filtering
     return new Rectangle2D.Double((int) Math.floor(src.getMinX() - 1),
                                   (int) Math.floor(src.getMinY() - 1),
                                   (int) Math.ceil(src.getWidth() + 2),
                                   (int) Math.ceil(src.getHeight()) + 2);
   }
 
   private TextureRenderer getBackingStore() {
     TextureRenderer renderer = (TextureRenderer) packer.getBackingStore();
     if (renderer != cachedBackingStore) {
       // Backing store changed since last time; discard any cached Graphics2D
       if (cachedGraphics != null) {
         cachedGraphics.dispose();
         cachedGraphics = null;
         cachedFontRenderContext = null;
       }
       cachedBackingStore = renderer;
     }
     return cachedBackingStore;
   }
 
   private Graphics2D getGraphics2D() {
     TextureRenderer renderer = getBackingStore();
     if (cachedGraphics == null) {
       cachedGraphics = renderer.createGraphics();
       // Set up composite, font and rendering hints
       cachedGraphics.setComposite(AlphaComposite.Src);
       cachedGraphics.setColor(Color.WHITE);
       cachedGraphics.setFont(font);
       cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                       (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                                                    : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
       cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                       (useFractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                                             : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
     }
     return cachedGraphics;
   }
 
   private void beginRendering(boolean ortho, int width, int height) {
     if (DEBUG && !debugged) {
       debug();
     }
 
     if (ortho) {
       getBackingStore().beginOrthoRendering(width, height);
     } else {
       getBackingStore().begin3DRendering();
     }
     GL gl = GLU.getCurrentGL();
 
     if (!haveMaxSize) {
       // Query OpenGL for the maximum texture size and set it in the
       // RectanglePacker to keep it from expanding too large
       int[] sz = new int[1];
       gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, sz, 0);
       packer.setMaxSize(sz[0], sz[0]);
       haveMaxSize = true;
     }
 
     // Change texture environment mode to MODULATE
     gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
     // Change text color to last saved
     gl.glColor4f(r, g, b, a);
   }
 
   private void endRendering(boolean ortho) throws GLException {
     if (ortho) {
       getBackingStore().endOrthoRendering();
     } else {
       getBackingStore().end3DRendering();
     }
     if (++numRenderCycles >= CYCLES_PER_FLUSH) {
       numRenderCycles = 0;
       if (DEBUG) {
         System.err.println("Clearing unused entries in endRendering()");
       }
       clearUnusedEntries();
     }
   }
 
   private int getSpaceWidth() {
     if (spaceWidth < 0) {
       Graphics2D g = getGraphics2D();
       FontRenderContext frc = getFontRenderContext();
       GlyphVector gv = font.createGlyphVector(frc, " ");
       Rectangle2D bbox = gv.getLogicalBounds();
       spaceWidth = (int) bbox.getWidth();
     }
     return spaceWidth;
   }
 
   private void tokenize(String str) {
     // Avoid lots of little allocations per render
     tokenizationResults.clear();
     if (!splitAtSpaces) {
       tokenizationResults.add(str);
     } else {
       int startChar = 0;
       char c = (char) 0;
       int len = str.length();
       int i = 0;
       while (i < len) {
         if (str.charAt(i) == ' ') {
           // Terminate any substring
           if (startChar < i) {
             tokenizationResults.add(str.substring(startChar, i));
           } else {
             tokenizationResults.add(null);
           }
           startChar = i + 1;
         }
         ++i;
       }
       // Add on any remaining (all?) characters
       if (startChar == 0) {
         tokenizationResults.add(str);
       } else if (startChar < len) {
         tokenizationResults.add(str.substring(startChar, len));
       }
     }
   }
 
   private void clearUnusedEntries() {
     final List/*<Rect>*/ deadRects = new ArrayList/*<Rect>*/();
     // Iterate through the contents of the backing store, removing
     // text strings that haven't been used recently
     packer.visit(new RectVisitor() {
         public void visit(Rect rect) {
           TextData data = (TextData) rect.getUserData();
           if (data.used()) {
             data.clearUsed();
           } else {
             deadRects.add(rect);
           }
         }
       });
     for (Iterator iter = deadRects.iterator(); iter.hasNext(); ) {
       Rect r = (Rect) iter.next();
       packer.remove(r);
       stringLocations.remove(((TextData) r.getUserData()).string());
 
       if (DEBUG) {
         Graphics2D g = getGraphics2D();
         g.setColor(TRANSPARENT_BLACK);
         g.fillRect(r.x(), r.y(), r.w(), r.h());
         g.setColor(Color.WHITE);
       }
     }
 
     // If we removed dead rectangles this cycle, try to do a compaction
     float frag = packer.verticalFragmentationRatio();
     if (!deadRects.isEmpty() && frag > MAX_VERTICAL_FRAGMENTATION) {
       if (DEBUG) {
         System.err.println("Compacting TextRenderer backing store due to vertical fragmentation " + frag);
       }
       packer.compact();
     }
 
     if (DEBUG) {
       getBackingStore().sync(0, 0, getBackingStore().getWidth(), getBackingStore().getHeight());
     }
   }
 
   class Manager implements BackingStoreManager {
     private Graphics2D g;
 
     public Object allocateBackingStore(int w, int h) {
       // FIXME: should consider checking Font's attributes to see
       // whether we're likely to need to support a full RGBA backing
       // store (i.e., non-default Paint, foreground color, etc.), but
       // for now, let's just be more efficient
       TextureRenderer renderer = TextureRenderer.createAlphaOnlyRenderer(w, h);
       if (DEBUG) {
         System.err.println(" TextRenderer allocating backing store " + w + " x " + h);
       }
       return renderer;
     }
 
     public void deleteBackingStore(Object backingStore) {
       ((TextureRenderer) backingStore).dispose();
     }
 
     public boolean preExpand(Rect cause, int attemptNumber) {
       // Only try this one time; clear out potentially obsolete entries
 
       // NOTE: this heuristic and the fact that it clears the used bit
       // of all entries seems to cause cycling of entries in some
       // situations, where the backing store becomes small compared to
       // the amount of text on the screen (see the TextFlow demo) and
       // the entries continually cycle in and out of the backing
       // store, decreasing performance. If we added a little age
       // information to the entries, and only cleared out entries
       // above a certain age, this behavior would be eliminated.
       // However, it seems the system usually stabilizes itself, so
       // for now we'll just keep things simple. Note that if we don't
       // clear the used bit here, the backing store tends to increase
       // very quickly to its maximum size, at least with the TextFlow
       // demo when the text is being continually re-laid out.
       if (attemptNumber == 0) {
         if (DEBUG) {
           System.err.println("Clearing unused entries in preExpand(): attempt number " + attemptNumber);
         }
         clearUnusedEntries();
         return true;
       }
 
       return false;
     }
 
     public void additionFailed(Rect cause, int attemptNumber) {
       // Heavy hammer -- might consider doing something different
       packer.clear();
       stringLocations.clear();
 
       if (DEBUG) {
         System.err.println(" *** Cleared all text because addition failed ***");
       }
     }
 
     public void beginMovement(Object oldBackingStore, Object newBackingStore) {
       TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
       g = newRenderer.createGraphics();
     }
 
     public void move(Object oldBackingStore,
                      Rect   oldLocation,
                      Object newBackingStore,
                      Rect   newLocation) {
       TextureRenderer oldRenderer = (TextureRenderer) oldBackingStore;
       TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
 
       if (oldRenderer == newRenderer) {
         // Movement on the same backing store -- easy case
         g.copyArea(oldLocation.x(), oldLocation.y(),
                    oldLocation.w(), oldLocation.h(),
                    newLocation.x() - oldLocation.x(),
                    newLocation.y() - oldLocation.y());
       } else {
         // Need to draw from the old renderer's image into the new one
         Image img = oldRenderer.getImage();
         g.drawImage(img,
                     newLocation.x(), newLocation.y(),
                     newLocation.x() + newLocation.w(), newLocation.y() + newLocation.h(),
                     oldLocation.x(), oldLocation.y(),
                     oldLocation.x() + oldLocation.w(), oldLocation.y() + oldLocation.h(),
                     null);
       }
     }
 
     public void endMovement(Object oldBackingStore, Object newBackingStore) {
       g.dispose();
       // Sync the whole surface
       TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
       newRenderer.sync(0, 0, newRenderer.getWidth(), newRenderer.getHeight());
     }
   }
 
   //----------------------------------------------------------------------
   // Debugging functionality
   //
 
   private void debug() {
     Frame dbgFrame = new Frame("TextRenderer Debug Output");
     GLCanvas dbgCanvas = new GLCanvas(new GLCapabilities(), null, GLContext.getCurrent(), null);
     dbgCanvas.addGLEventListener(new DebugListener(dbgFrame));
     dbgFrame.add(dbgCanvas);
     final FPSAnimator anim = new FPSAnimator(dbgCanvas, 10);
     dbgFrame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
           // Run this on another thread than the AWT event queue to
           // make sure the call to Animator.stop() completes before
           // exiting
           new Thread(new Runnable() {
               public void run() {
                 anim.stop();
               }
             }).start();
         }
       });
     dbgFrame.setSize(256, 256);
     dbgFrame.setVisible(true);
     anim.start();
     debugged = true;
   }
 
   class DebugListener implements GLEventListener {
     private GLU glu = new GLU();
     private Frame frame;
 
     DebugListener(Frame frame) {
       this.frame = frame;
     }
 
     public void display(GLAutoDrawable drawable) {
       GL gl = drawable.getGL();
       gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
       TextureRenderer rend = getBackingStore();
       final int w = rend.getWidth();
       final int h = rend.getHeight();
       rend.beginOrthoRendering(w, h);
       rend.drawOrthoRect(0, 0);
       rend.endOrthoRendering();
       if (frame.getWidth() != w ||
           frame.getHeight() != h) {
         EventQueue.invokeLater(new Runnable() {
             public void run() {
               frame.setSize(w, h);
             }
           });
       }
     }
 
     // Unused methods
     public void init(GLAutoDrawable drawable) {}
     public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
     public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
   }
 }
