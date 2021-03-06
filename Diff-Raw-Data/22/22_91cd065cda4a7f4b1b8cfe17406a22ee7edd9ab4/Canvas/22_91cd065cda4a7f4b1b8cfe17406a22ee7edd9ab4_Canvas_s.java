 /* Copyright (c) 2006-2008 Indiana University Research and Technology Corporation.
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * - Redistributions of source code must retain the above copyright notice, this
  *  list of conditions and the following disclaimer.
  *
  * - Redistributions in binary form must reproduce the above copyright notice,
  *  this list of conditions and the following disclaimer in the documentation
  *  and/or other materials provided with the distribution.
  *
  * - Neither the Indiana University nor the names of its contributors may be used
  *  to endorse or promote products derived from this software without specific
  *  prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package stencil.adapters.java2D;
 
 import java.awt.Color;
 import java.awt.Graphics;
 import java.awt.image.BufferedImage;
 import java.awt.Rectangle;
 import java.awt.geom.Rectangle2D;
 import java.awt.geom.AffineTransform;
 import java.util.List;
 
 import javax.swing.JComponent;
 
 import stencil.adapters.java2D.data.glyphs.Point;
 import stencil.adapters.java2D.data.Table;
 import stencil.adapters.java2D.util.GenerationTracker;
 import stencil.adapters.java2D.util.Painter;
 import stencil.display.CanvasTuple;
 import stencil.parser.tree.Layer;
 
 
 /**Some of this is derived from Prefuse's display and related objects.*/
 
 public final class Canvas extends JComponent {	
 	final Painter painter;	
 	BufferedImage buffer;
 	AffineTransform viewTransform;
 	private final GenerationTracker tracker;
 	private Rectangle2D contentBounds;
 	final Table<? extends Point>[] layers;
 	
 	public Canvas(List<Layer> layers) {
 		this.setBackground((Color) CanvasTuple.CanvasAttribute.BACKGROUND_COLOR.getDefaultValue());
 		
 		this.layers = new Table[layers.size()];
 		for (int i=0;i< layers.size();i++) {
 			this.layers[i] = (Table) layers.get(i).getDisplayLayer();
 		}
 		this.painter = new Painter(this.layers, this);
 		painter.start();
 
 		setDoubleBuffered(false);	//TODO: Use the BufferStrategy instead of manually double buffering
 		setOpaque(true);
 		tracker = new GenerationTracker(this.layers);
 		
 		viewTransform = new  AffineTransform();
 	}
 	
 	public void dispose() {painter.signalStop();}
 		
 	public void paintComponent(Graphics g) {g.drawImage(buffer, 0, 0, null);}
 	
 	public void setBackBuffer(BufferedImage i) {this.buffer = i;}
 	
 	public Rectangle getContentDimension() {
 		Rectangle2D bounds =contentBounds;
 		if (contentBounds == null || tracker.changed()) {
 			bounds = new Rectangle2D.Double(0,0,0,0);
 			
 			for (Table<? extends Point> t: layers) {
 				tracker.fixGeneration(t);
 				for (Point p: t) {
 					bounds.add(p.getBounds());
 				}
 			}
 		} 
 		zoomFit(bounds); //HACK: remove when there is user-controlled zoom/pan.  Right now, it just makes things fit.
 		return bounds.getBounds();
 	}	
 
 	public void stopPainter() {painter.signalStop();}
 	
 	private void zoomFit(Rectangle2D bounds) {
 		Rectangle view = this.getBounds();
 		double scale = getScale(bounds, view);
 		viewTransform.setToTranslation(center(bounds.getMinX(),bounds.getWidth(), view.getWidth()),
 										center(bounds.getMinY(), bounds.getHeight(), view.getHeight()));
 		viewTransform.scale(scale, scale);
 	}
 	
 	private double center(double itemP, double itemD, double frameD) {
 		return -itemP + (frameD/2.0 - itemD/2.0);
 	}
 	
 	private double getScale(Rectangle2D item, Rectangle2D frame) {
 		double scaleX = (frame.getWidth())/item.getWidth();
 		double scaleY = (frame.getHeight())/item.getHeight();
 		return Math.min(scaleX,scaleY);
 	}
 	
 	public AffineTransform getViewTransform() {return viewTransform;}
 }
