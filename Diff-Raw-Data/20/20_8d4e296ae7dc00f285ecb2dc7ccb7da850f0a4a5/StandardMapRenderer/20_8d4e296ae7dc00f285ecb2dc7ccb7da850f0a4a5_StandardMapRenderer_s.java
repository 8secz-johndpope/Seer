 package mapwriter.map;
 
 import java.awt.Point;
 
 import mapwriter.Mw;
 import mapwriter.Render;
 import mapwriter.map.mapmode.MapMode;
 
 import org.lwjgl.opengl.GL11;
 
 public class StandardMapRenderer implements MapRenderer {
 	private Mw mw;
 	private MapMode mapMode;
 	private MapView mapView;
 	public Point.Double playerArrowScreenPos = new Point.Double(0, 0);
 	
 	public StandardMapRenderer(Mw mw, MapMode mapMode, MapView mapView) {
 		this.mw = mw;
 		this.mapMode = mapMode;
 		this.mapView = mapView;
 	}
 	
 	public void close() {
 	}
 	
 	public void update() {
 		this.mapMode.setScreenRes();
 		this.mapView.setMapWH(this.mapMode);
 		this.mapView.setTextureSize(this.mw.mapTexture.textureSize);
 		this.mw.mapTexture.requestView(this.mapView, this.mw.executor, this.mw.regionManager);
 	}
 	
 	public void draw() {
 		int regionZoomLevel = Math.max(0, this.mapView.getZoomLevel());
 		int dimension = this.mapView.getDimension();
 		
		boolean drawTexture = ((regionZoomLevel == this.mw.mapTexture.loadedZoomLevel) && (dimension == this.mw.mapTexture.loadedDimension));
		
 		double tSize = (double) (this.mw.mapTexture.textureSize << regionZoomLevel);
 		
 		// if the texture UV coordinates do not line up with the texture pixels then the texture
 		// will look blurry when it is rendered.
 		// to fix this we round the texture coordinates to the nearest pixel boundary.
 		// this is unnecessary when zoomed in as the texture will be upscaled and look blurry
 		// anyway, so it is disabled in this case.
 		// also the rounding causes the map to noticeably (and unpleasantly) 'snap' to texture
 		// pixel boundaries when zoomed in.
 		
 		double u, v, w, h;
 		if (this.mapView.getZoomLevel() >= 0) {
 			u = Math.round(this.mapView.getMinX() % tSize) / tSize;
 			v = Math.round(this.mapView.getMinZ() % tSize) / tSize;
 			w = Math.round(this.mapView.getWidth()) / tSize;
 			h = Math.round(this.mapView.getHeight()) / tSize;
 		} else {
 			u = (this.mapView.getMinX() % tSize) / tSize;
 			v = (this.mapView.getMinZ() % tSize) / tSize;
 			w = this.mapView.getWidth() / tSize;
 			h = this.mapView.getHeight() / tSize;
 		}
 		
 		GL11.glPushMatrix();
 		GL11.glLoadIdentity();
 		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
 		if (this.mapMode.rotate) {
 			GL11.glRotatef((float) this.mw.mapRotationDegrees, 0.0f, 0.0f, 1.0f);
 		}
 		if (this.mapMode.circular) {
 			Render.setCircularStencil(0, 0, this.mapMode.h / 2.0);
 		}
 		
		if (drawTexture) {
 			Render.setColourWithAlphaPercent(0xffffff, this.mapMode.alphaPercent);
 			this.mw.mapTexture.bind();
 			Render.drawTexturedRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h,
 					u, v, u + w, v + h);
 		} else {
 			Render.setColourWithAlphaPercent(0x000000, this.mapMode.alphaPercent);
 			Render.drawRect(this.mapMode.x, this.mapMode.y, this.mapMode.w, this.mapMode.h);
 		}
 		
 		if (this.mapMode.circular) {
 			Render.disableStencil();
 		}
 		
 		drawBorder(this.mapMode);
 		
 		// draw markers
 		this.mw.markerManager.drawMarkers(this.mapMode, this.mapView);
 		
 		// draw player trail
 		if (this.mw.playerTrail.enabled) {
 			this.mw.playerTrail.draw(this.mapMode, this.mapView);
 		}
 		
 		// the position of the player within the map
 		Point.Double arrow = this.getPlayerArrowPos();
 		
 		// draw player arrow
 		Render.setColour(this.mapMode.playerArrowColour);
 		Render.drawArrow(arrow.x, arrow.y, this.mw.playerHeading, this.mapMode.playerArrowSize);
 		
 		GL11.glLoadIdentity();
 		GL11.glTranslatef((float) this.mapMode.xTranslation, (float) this.mapMode.yTranslation, -2000.0f);
 		
 		drawCoords(this.mw, this.mapMode);
 		
 		// some shader mods seem to need depth testing re-enabled
 		GL11.glEnable(GL11.GL_DEPTH_TEST);
 		GL11.glPopMatrix();
 	}
 	
 	public static void drawCoords(Mw mw, MapMode mapMode) {
 		// draw coordinates
 		if (mw.coordsEnabled && mapMode.coordsEnabled) {
 			Render.drawCentredString(mapMode.textX, mapMode.textY, mapMode.textColour,
 					"%d %d %d", mw.playerXInt, mw.playerYInt, mw.playerZInt);
 		}
 	}
 	
 	public static void drawBorder(MapMode mapMode) {
 		if ((mapMode.borderWidth > 0) && (mapMode.borderColour != 0)) {
 			Render.setColour(mapMode.borderColour);
 			if (mapMode.circular) {
 				Render.drawCircleBorder(0.0, 0.0, mapMode.h / 2.0, mapMode.borderWidth);
 			} else {
 				Render.drawRectBorder(mapMode.x, mapMode.y, mapMode.w, mapMode.h, mapMode.borderWidth);
 			}
 		}
 	}
 	
 	public Point.Double getPlayerArrowPos() {
 		double scale = this.mapView.getDimensionScaling(this.mw.playerDimension);
 		Point.Double p = this.mapMode.getClampedScreenXY(this.mapView, this.mw.playerX * scale, this.mw.playerZ * scale);
 		this.playerArrowScreenPos.setLocation(p.x + this.mapMode.xTranslation, p.y + this.mapMode.yTranslation);
 		return p;
 	}
 }
 
