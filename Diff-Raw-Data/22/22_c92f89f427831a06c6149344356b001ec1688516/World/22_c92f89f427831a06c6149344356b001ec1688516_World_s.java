 /*
  * Copyright (C) 2012 JPII and contributors
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.jpii.navalbattle.pavo;
 
 import java.awt.*;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.Serializable;
 
 import maximusvladimir.dagen.Rand;
 
 import com.jpii.navalbattle.data.Constants;
 import com.jpii.navalbattle.io.Interactable;
 import com.jpii.navalbattle.pavo.grid.EntityManager;
 import com.jpii.navalbattle.pavo.io.PavoImage;
 import com.jpii.navalbattle.renderer.weather.WeatherManager;
 
 
 public class World extends Renderable implements Interactable {
 	WorldGen gen;
 	Chunk[] chunks;
 	PavoImage buffer;
 	boolean needsNewRender = false;
 	boolean[] generated;
 	boolean bufferLock = false;
 	WorldSize ws;
 	int width = 16;
 	int height = 8;
 	int lww = 800;
 	int lwh = 600;
 	EntityManager em;
 	TimeManager time = new TimeManager();
 	WeatherManager wm;
 	int sx = 0, anisx = 0, anisy = 0,sy = 0;
 	PavoImage noise;
 	int zlevel;
 	public World() {
 		ws = WorldSize.WORLD_LARGE;
 		em = new EntityManager(this);
 		width = PavoHelper.getGameWidth(getWorldSize());
 		height = PavoHelper.getGameHeight(getWorldSize());
 		chunks = new Chunk[(width)*(height)];
 		for (int x = 0;x < width; x++) {
 			for (int z = 0; z < height; z++) {
 				int i = z*width+x;
 				chunks[i] = new Chunk(this);
 				chunks[i].setX(x);
 				chunks[i].setZ(z);
 			}
 		}
 		generated = new boolean[chunks.length];
 		buffer = new PavoImage(Game.Settings.currentWidth,Game.Settings.currentHeight,BufferedImage.TYPE_3BYTE_BGR);
 		makeNoise();
 		wm = new WeatherManager();
 	}
 	public void setEntityManager(EntityManager em) {
 		this.em = em;
 	}
 	public Chunk getChunk(int index) {
 		return chunks[index];
 	}
 	public Chunk getChunk(int x, int z) {
 		return getChunk(z*PavoHelper.getGameWidth(getWorldSize())+x);
 	}
 	public void setZoomLevel(int level) {
 		zlevel = level;
 	}
 	public int getZoomLevel() {
 		return zlevel;
 	}
 	public void makeNoise(){
 		noise = new PavoImage(Game.Settings.currentWidth,Game.Settings.currentHeight,BufferedImage.TYPE_3BYTE_BGR);
 		Rand ras = new Rand(Game.Settings.seed+22);
 		Graphics gs2 = noise.getGraphics(); // Q and D
 		for (int x = 0; x < Game.Settings.currentWidth; x+= 2) {
 			for (int y = 0; y < Game.Settings.currentHeight; y+=2) {
 				int rgb = ras.nextInt(127);
 				gs2.setColor(new Color(rgb,rgb,rgb));
 				gs2.fillRect(x,y,2,2);
 			}
 		}
 		gs2.dispose();
 	}
 	private void runLocLock(int x, int y) {
 		int cx = 0;
 		int cy = 0;
 		if (x != 0)
 			cx = x - sx;
 		if (y != 0)
 			cy = y - sy;
 		getWeather().applyFix(cx,cy);
 	}
 	public void setLoc(int x, int y) {
 		if (sx != x || sy != y)
 			chunkrender = true;
 		runLocLock(x,y);
 		sx = x;
 		sy = y;
 	}
 	public void setLocX(int x) {
 		if (sx != x)
 			chunkrender = true;
 		runLocLock(x,0);
 		sx = x;
 	}
 	public void setLocY(int y) {
 		if (sy != y)
 			chunkrender = true;
 		runLocLock(0,y);
 		sy = y;
 	}
 	public void setWorldGen(WorldGen wg) {
 		gen = wg;
 	}
 	public WorldGen getWorldGen() {
 		return gen;
 	}
 	long smallTicks = 0;
 	long localTicks = 0;
 	long startFPS = 0;
 	public void update() {
 		smallTicks += 100;
 		localTicks++;
 		time.update();
 		wm.update();
 		em.update(localTicks);//smallTicks);
 	}
 	public WeatherManager getWeather() {
 		return wm;
 	}
 	public boolean hasMoreChunks() {
 		for (int c = 0; c < chunks.length; c++) {
 			if (!generated[c])
 				return true;
 		}
 		Game.Settings.isFinishedGenerating = true;
 		return false;
 	}
 	public void genNextChunk() {
 		for (int c = 0; c < chunks.length; c++) {
 			Chunk chunk = chunks[c];
 			Game.getStats().SmKdn02nOaP(c*2);
 			while (chunk.isLocked()) {
 				
 			}
 			chunk.lock();
 			if (!generated[c]){
 				//System.out.println("Chunk at " + c + " generated.");
 				chunk.render();
 				generated[c] = true;
 				needsNewRender = true;
 				//break;
 			}
 			chunkrender = true;
 			chunk.unlock();
 			chunks[c] = chunk;
 		}
 	}
 	boolean chunkrender = false;
 	public boolean needsReChunkRender() {
 		return chunkrender;
 	}
 	/**
 	 * This method should be called sparsingly (which means DO NOT OVER USE). This method is multithreaded, so it puts no stress on the calling thread.
 	 * This method is not actually deprecated, but it is called so to ensure that the above message is read.
 	 * @deprecated
 	 */
 	public void forceRender() {
 		chunkrender = true;
 	}
 	public synchronized void render() {
 		if (!needsReChunkRender())
 			return;
 		long waitStart = System.currentTimeMillis();
 		while (bufferLock) {
 			
 		}
 		long endWait = System.currentTimeMillis() - waitStart;
 		bufferLock = true;
 		long startDraw = System.currentTimeMillis();
 		if (lww != Game.Settings.currentWidth || lwh != Game.Settings.currentHeight) {
 			buffer = null;
 			buffer = new PavoImage(Game.Settings.currentWidth,Game.Settings.currentHeight,BufferedImage.TYPE_3BYTE_BGR);
 			lww = Game.Settings.currentWidth;
 			lwh = Game.Settings.currentHeight;
 			makeNoise();
 		}
 		int liveChunks = 0;
 		Graphics2D g = PavoHelper.createGraphics(buffer);
 		//g.drawIm
 		g.drawImage(noise, 0, 0, null);
 		int startsyncx = (-sx) / 100;
 		int startsyncz = (-sy) / 100;
 		int syncwidth = (Game.Settings.currentWidth/100)+2;
 		int syncheight = (Game.Settings.currentHeight/100)+2;
 		for (int x = startsyncx; x < syncwidth+startsyncx; x++) {
 			for (int z = startsyncz; z < syncheight+startsyncz; z++) {
 				int index = z*width+x;
 				if (index >= 0 && index < chunks.length) {
 					Chunk chunk = chunks[index];
 					if (PavoHelper.isChunkVisibleOnScreen(this, chunk)) {
 						//while (chunk.isLocked()) { }
 						//chunk.lock();
 						if (!chunk.isGenerated()) {
 							int rgb = Game.Settings.rand.nextInt(255);
 							if (Game.Settings.rand.nextBoolean())
 								g.setColor(new Color(6,rgb,13));
 							else
 								g.setColor(new Color(6,13,rgb));
 							g.fillRect(x*100,z*100,100,100);
 						}
 						else if (x-2 == width || z-2 == height) {
 							g.fillRect(sx+(x*100),sy+(z*100), 303, 303);
 						}
 						else {
 							if (chunk.needsBufferWrite())
 								chunk.writeBuffer();
 							if (chunk.getBuffer() != null)
 								g.drawImage(chunk.getBuffer(), sx+(x*100),sy+(z*100),null);
 						}
 						//chunk.unlock();
 						liveChunks++;
 					}
 				}
 			}
 		}
 		g.dispose();
 		chunkrender = false;
 		long endDraw = System.currentTimeMillis() - startDraw;
 		Game.getStats().SmKAk10(endDraw);
 		Game.getStats().SmoOa01kwL(liveChunks);
 		Game.getStats().Smw2e33AK(endWait);
 		bufferLock = false;
 	}
 	public PavoImage getBuffer() {
 		return buffer;
 	}
 	public int getScreenX() {
 		return sx;
 	}
 	public int getScreenY() {
 		return sy;
 	}
 	public void moveScreenTo(int x, int y) {
 		anisx = x;
 		anisy = y;
 	}
 	public WorldSize getWorldSize() {
 		return ws;
 	}
 	public EntityManager getEntityManager() {
 		return em;
 	}
 	public TimeManager getTimeManager() {
 		return time;
 	}
 	public void save(String path) {
 		File file = new File(path);
 		boolean throwError = false;
 		try {
 			if (!file.exists()) {
 				if (!file.createNewFile()) {
 					throwError = true;
 				}
 			}
 		}
 		catch (Error err) {
 		}
 		catch (Exception ex) {
 		}
 		if (throwError)
 			throw new java.lang.IllegalArgumentException("Unable to save file. See store for details");
 	}
 	public void load(String path) {
 	}
 	public void peekElements() {
 		
 	}
 }
