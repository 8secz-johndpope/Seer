 package com.pi.editor.gui.map;
 
 import java.awt.Color;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.event.MouseMotionListener;
 import java.io.File;
 
 import javax.swing.JFileChooser;
 import javax.swing.JOptionPane;
 
 import com.pi.common.contants.SectorConstants;
 import com.pi.common.contants.TileConstants;
 import com.pi.common.contants.TileFlags;
 import com.pi.common.database.GraphicsObject;
 import com.pi.common.database.Sector;
 import com.pi.common.database.Tile;
 import com.pi.common.database.Tile.TileLayer;
 import com.pi.common.database.io.DatabaseIO;
 import com.pi.editor.Paths;
 import com.pi.graphics.device.IGraphics;
 import com.pi.gui.GUIKit;
 import com.pi.gui.PIButton;
 import com.pi.gui.PICheckbox;
 import com.pi.gui.PIComponent;
 import com.pi.gui.PIContainer;
 import com.pi.gui.PIScrollBar;
 import com.pi.gui.PIScrollBar.ScrollBarListener;
 import com.pi.gui.PIScrollBar.ScrollEvent;
 import com.pi.gui.PIStyle.StyleType;
 
 public class MapEditorObject extends PIContainer implements ScrollBarListener,
 	MapInfoRenderer {
     private static int[] TILESETS = new int[] { 2 };
 
     public static void init() {
 	String s = JOptionPane
 		.showInputDialog("List tileset image IDs, comma seperated values\n(From "
 			+ Paths.getGraphicsDirectory().getAbsolutePath() + ")");
 	String[] dat = s.split(",");
 	TILESETS = new int[dat.length];
 	for (int i = 0; i < dat.length; i++) {
 	    TILESETS[i] = Integer.valueOf(dat[i]);
 	}
     }
 
     private PIContainer tileSelector;
     private PIComponent graphicsData;
     private PIScrollBar tilesetSelector;
     private PIScrollBar verticalTile;
     private PIScrollBar horizontalTile;
     private PIScrollBar tileLayerSelector;
 
     private PIButton save, load, newS;
 
     private PICheckbox directionBlockMode;
 
     private int tileset = TILESETS[0];
 
     private TileSelectionHandler tileSelectionHandler = new TileSelectionHandler();
 
     private int currentTileOffX, currentTileOffY;
 
     private MapViewerObject viewer;
 
     public MapEditorObject(MapViewerObject viewer) {
 	setLocation(500, 0);
 	setSize(500, 500);
 
 	this.viewer = viewer;
 
 	tileSelector = new PIContainer();
 	tileSelector.setLocation(0, 0);
 	tileSelector.setSize(300, 500);
 	tileSelector.setStyle(StyleType.Normal, GUIKit.containerNormal.clone());
 
 	graphicsData = new PIComponent() {
 	    @Override
 	    public void render(IGraphics g) {
 		super.render(g);
 		if (isVisible && g.getImageWidth(tileset) > 0) {
 		    g.setColor(Color.BLACK);
 		    g.fillRect(getAbsoluteX(), getAbsoluteY(), getWidth(),
 			    getHeight());
 		    currentTileOffX = (int) ((1f - horizontalTile
 			    .getScrollAmount()) * (g.getImageWidth(tileset) - getWidth()));
 		    currentTileOffY = (int) (verticalTile.getScrollAmount() * (g
 			    .getImageHeight(tileset) - getHeight()));
 		    g.drawImage(tileset, getAbsoluteX(), getAbsoluteY(),
 			    currentTileOffX, currentTileOffY, Math.min(
 				    g.getImageWidth(tileset) - currentTileOffX,
 				    getWidth()),
 			    Math.min(g.getImageHeight(tileset)
 				    - currentTileOffY, getHeight()));
 
 		    int destX = (Math.min(tileX, dragTileX) * TileConstants.TILE_WIDTH)
 			    - currentTileOffX + getAbsoluteX();
 		    int destY = (Math.min(tileY, dragTileY) * TileConstants.TILE_HEIGHT)
 			    - currentTileOffY + getAbsoluteY();
 
 		    int destX2 = (Math.max(tileX, dragTileX) * TileConstants.TILE_WIDTH)
 			    - currentTileOffX + getAbsoluteX();
 		    int destY2 = (Math.max(tileY, dragTileY) * TileConstants.TILE_HEIGHT)
 			    - currentTileOffY + getAbsoluteY();
 		    g.setColor(Color.WHITE.darker());
 		    g.drawRect(destX, destY, (destX2 - destX)
 			    + TileConstants.TILE_WIDTH - 1, (destY2 - destY)
 			    + TileConstants.TILE_HEIGHT - 1);
 
 		    if (tileAX >= 0) {
 			destX = (tileAX * TileConstants.TILE_WIDTH)
 				- currentTileOffX + getAbsoluteX();
 			destY = (tileAY * TileConstants.TILE_HEIGHT)
 				- currentTileOffY + getAbsoluteY();
 
 			destX2 = (dragTileAX * TileConstants.TILE_WIDTH)
 				- currentTileOffX + getAbsoluteX();
 			destY2 = (dragTileAY * TileConstants.TILE_HEIGHT)
 				- currentTileOffY + getAbsoluteY();
 			g.setColor(Color.WHITE);
 			g.drawRect(destX, destY, (destX2 - destX)
 				+ TileConstants.TILE_WIDTH - 1,
 				(destY2 - destY) + TileConstants.TILE_HEIGHT
 					- 1);
 		    }
 		}
 	    }
 	};
 	graphicsData.setLocation(0, 0);
 	graphicsData.setSize(tileSelector.getWidth() - 25,
 		tileSelector.getHeight() - 75);
 	graphicsData.addMouseMotionListener(tileSelectionHandler);
 	graphicsData.addMouseListener(tileSelectionHandler);
 
 	horizontalTile = new PIScrollBar(true);
 	horizontalTile.setLocation(0, tileSelector.getHeight() - 75);
 	horizontalTile.setSize(tileSelector.getWidth() - 25, 25);
 
 	verticalTile = new PIScrollBar(false);
 	verticalTile.setLocation(tileSelector.getWidth() - 25, 0);
 	verticalTile.setSize(25, tileSelector.getHeight() - 75);
 
 	tilesetSelector = new PIScrollBar(true);
 	tilesetSelector.setLocation(0, tileSelector.getHeight() - 50);
 	tilesetSelector.setSize(tileSelector.getWidth() - 25, 25);
 	tilesetSelector.setStep(1f / ((float) TILESETS.length));
 
 	tileLayerSelector = new PIScrollBar(true);
 	tileLayerSelector.setLocation(0, tileSelector.getHeight() - 25);
 	tileLayerSelector.setSize(tileSelector.getWidth() - 25, 25);
 	tileLayerSelector.setStep(1f / ((float) TileLayer.values().length));
 	tileLayerSelector.addScrollBarListener(this);
 	tileLayerSelector.setStyle(StyleType.Normal,
 		tileLayerSelector.getStyle(StyleType.Normal));
 	tileLayerSelector.getStyle(StyleType.Normal).foreground = Color.WHITE;
 	tileLayerSelector
 		.setOverlay((currentTileLayer = TileLayer.values()[Math
 			.round(tileLayerSelector.getScrollAmount()
 				* (TileLayer.MAX_VALUE.ordinal() - 1))]).name());
 
 	tileSelector.add(graphicsData);
 	tileSelector.add(horizontalTile);
 	tileSelector.add(verticalTile);
 	tileSelector.add(tilesetSelector);
 	tileSelector.add(tileLayerSelector);
 
 	add(tileSelector);
 
 	load = new PIButton();
 	load.setContent("Load");
 	load.setLocation(325, 0);
 	load.setSize(50, 25);
 	load.addMouseListener(tileSelectionHandler);
 
 	save = new PIButton();
 	save.setLocation(325, 40);
 	save.setContent("Save");
 	save.setSize(50, 25);
 	save.addMouseListener(tileSelectionHandler);
 
 	newS = new PIButton();
 	newS.setLocation(325, 80);
 	newS.setContent("New");
 	newS.setSize(50, 25);
 	newS.addMouseListener(tileSelectionHandler);
 
 	directionBlockMode = new PICheckbox();
 	directionBlockMode.setContent("Directional Blocking");
 	directionBlockMode.setLocation(325, 120);
 	directionBlockMode.setSize(125, 25);
 	directionBlockMode.setChecked(false);
 	directionBlockMode.addMouseListener(new MouseAdapter() {
 	    @Override
 	    public void mouseClicked(MouseEvent e) {
 		tileSelector.setVisible(!directionBlockMode.isChecked());
 	    }
 	});
 
 	add(load);
 	add(save);
 	add(newS);
 	add(directionBlockMode);
 
 	compile();
 
 	viewer.infoRender = this;
     }
 
     @Override
     public void onScroll(ScrollEvent e) {
 	if (e.getSource() == tilesetSelector) {
 	    int nTiles = Math.round(e.getScrollPosition() * TILESETS.length);
 	    if (nTiles != tileset) {
 		horizontalTile.setScrollAmount(0);
 		verticalTile.setScrollAmount(0);
 		tileset = nTiles;
 		tileX = 0;
 		tileY = 0;
 		dragTileX = 0;
 		dragTileY = 0;
 		tileAX = -1;
 	    }
 	} else if (e.getSource() == tileLayerSelector) {
 	    tileLayerSelector
 		    .setOverlay((currentTileLayer = TileLayer.values()[Math
 			    .round(tileLayerSelector.getScrollAmount()
 				    * (TileLayer.MAX_VALUE.ordinal() - 1))])
 			    .name());
 	}
     }
 
     public int tileX, tileY, dragTileX, dragTileY;
     public int tileAX = -1, tileAY, dragTileAX, dragTileAY;
     private boolean mouseDown = false;
     private TileLayer currentTileLayer = TileLayer.GROUND;
 
     private class TileSelectionHandler implements MouseListener,
 	    MouseMotionListener {
 	@Override
 	public void mouseClicked(MouseEvent e) {
 	    if (e.getSource() == save) {
 		JFileChooser fc = new JFileChooser();
 		int returnVal = fc.showSaveDialog(null);
 
 		if (returnVal == JFileChooser.APPROVE_OPTION) {
 		    File file = fc.getSelectedFile();
 		    try {
 			DatabaseIO.write(file, viewer.getSector());
 		    } catch (Exception ex) {
 			JOptionPane.showMessageDialog(null, ex.toString());
 		    }
 		}
 	    } else if (e.getSource() == load) {
 		JFileChooser fc = new JFileChooser();
 		int returnVal = fc.showOpenDialog(null);
 
 		if (returnVal == JFileChooser.APPROVE_OPTION) {
 		    File file = fc.getSelectedFile();
 		    try {
 			viewer.setSector((Sector) DatabaseIO.read(file,
 				Sector.class));
 		    } catch (Exception ex) {
 			JOptionPane.showMessageDialog(null, ex.toString());
 		    }
 		}
 	    } else if (e.getSource() == newS) {
 		viewer.setSector(new Sector());
 	    }
 	}
 
 	@Override
 	public void mousePressed(MouseEvent e) {
 	    mouseDown = true;
 	}
 
 	@Override
 	public void mouseReleased(MouseEvent e) {
 	    if (mouseDown) {
 		tileAX = Math.min(tileX, dragTileX);
 		tileAY = Math.min(tileY, dragTileY);
 		dragTileAX = Math.max(tileX, dragTileX);
 		dragTileAY = Math.max(tileY, dragTileY);
 		mouseDown = false;
 	    }
 	}
 
 	@Override
 	public void mouseEntered(MouseEvent e) {
 
 	}
 
 	@Override
 	public void mouseExited(MouseEvent e) {
 
 	}
 
 	@Override
 	public void mouseDragged(MouseEvent e) {
 	    dragTileX = (e.getX() + currentTileOffX) / TileConstants.TILE_WIDTH;
 	    dragTileY = (e.getY() + currentTileOffY)
 		    / TileConstants.TILE_HEIGHT;
 	}
 
 	@Override
 	public void mouseMoved(MouseEvent e) {
 	    tileX = (e.getX() + currentTileOffX) / TileConstants.TILE_WIDTH;
 	    tileY = (e.getY() + currentTileOffY) / TileConstants.TILE_HEIGHT;
 	    dragTileX = tileX;
 	    dragTileY = tileY;
 	}
 
     }
 
     private static final int hoffset = 2;
     private static final int voffset = 4;
 
     @Override
     public void renderMapTile(IGraphics g, int baseX, int baseY, Tile tile) {
 	if (directionBlockMode.isChecked()) {
 	    g.drawText(
 		    "<",
 		    baseX + hoffset,
 		    baseY - voffset + (TileConstants.TILE_HEIGHT / 3),
 		    GUIKit.defaultStyle.font,
 		    ((tile.getFlags() & TileFlags.WALL_WEST) == TileFlags.WALL_WEST) ? Color.RED
 			    : Color.GREEN);
 	    g.drawText(
 		    ">",
 		    baseX + hoffset + 2 * (TileConstants.TILE_WIDTH / 3),
 		    baseY - voffset + (TileConstants.TILE_HEIGHT / 3),
 		    GUIKit.defaultStyle.font,
 		    ((tile.getFlags() & TileFlags.WALL_EAST) == TileFlags.WALL_EAST) ? Color.RED
 			    : Color.GREEN);
 
 	    g.drawText(
 		    "/\\",
 		    baseX + hoffset + (TileConstants.TILE_WIDTH / 3),
 		    baseY - voffset,
 		    GUIKit.defaultStyle.font,
 		    ((tile.getFlags() & TileFlags.WALL_NORTH) == TileFlags.WALL_NORTH) ? Color.RED
 			    : Color.GREEN);
 	    g.drawText(
 		    "\\/",
 		    baseX + hoffset + (TileConstants.TILE_WIDTH / 3),
 		    baseY - voffset + (2 * (TileConstants.TILE_HEIGHT / 3)),
 		    GUIKit.defaultStyle.font,
 		    ((tile.getFlags() & TileFlags.WALL_SOUTH) == TileFlags.WALL_SOUTH) ? Color.RED
 			    : Color.GREEN);
 
 	    g.setColor((tile.getFlags() & TileFlags.BLOCKED) == TileFlags.BLOCKED ? Color.RED
 		    : Color.GREEN);
 	    g.fillRect(baseX + (TileConstants.TILE_WIDTH / 3) + 2, baseY
 		    + (TileConstants.TILE_HEIGHT / 3) + 2,
 		    (TileConstants.TILE_WIDTH / 3) - 4,
 		    (TileConstants.TILE_HEIGHT / 3) - 4);
 
 	    g.setColor(Color.BLUE);
 	    g.drawRect(baseX, baseY, TileConstants.TILE_WIDTH - 1,
 		    TileConstants.TILE_HEIGHT - 1);
 	}
     }
 
     @Override
     public void onMapClick(Sector s, int tileX, int tileY, int internalX,
 	    int internalY) {
 	if (s != null) {
 	    if (!directionBlockMode.isChecked()) {
 		if (tileAX >= 0) {
 		    int tileWidth = dragTileAX - tileAX;
 		    int tileHeight = dragTileAY - tileAY;
 		    for (int x = tileX; x <= Math.min(tileX + tileWidth,
			    SectorConstants.SECTOR_WIDTH); x++) {
 			for (int y = tileY; y <= Math.min(tileY + tileHeight,
				SectorConstants.SECTOR_HEIGHT); y++) {
 			    s.getLocalTile(x, y)
 				    .setLayer(
 					    currentTileLayer,
 					    new GraphicsObject(
 						    tileset,
 						    ((x - tileX) + tileAX)
 							    * TileConstants.TILE_WIDTH,
 						    ((y - tileY) + tileAY)
 							    * TileConstants.TILE_HEIGHT,
 						    TileConstants.TILE_WIDTH,
 						    TileConstants.TILE_HEIGHT));
 			}
 		    }
 		}
 	    } else {
 		int flags = s.getLocalTile(tileX, tileY).getFlags();
 		if (internalX < (TileConstants.TILE_WIDTH / 3)) {
 		    if (internalY > (TileConstants.TILE_HEIGHT / 3)
 			    && internalY < 2 * (TileConstants.TILE_HEIGHT / 3)) {
 			if ((flags & TileFlags.WALL_WEST) == TileFlags.WALL_WEST)
 			    flags &= (~TileFlags.WALL_WEST);
 			else
 			    flags |= TileFlags.WALL_WEST;
 		    }
 		} else if (internalX > 2 * (TileConstants.TILE_WIDTH / 3)) {
 		    if (internalY > (TileConstants.TILE_HEIGHT / 3)
 			    && internalY < 2 * (TileConstants.TILE_HEIGHT / 3)) {
 			if ((flags & TileFlags.WALL_EAST) == TileFlags.WALL_EAST)
 			    flags &= (~TileFlags.WALL_EAST);
 			else
 			    flags |= TileFlags.WALL_EAST;
 		    }
 		} else {
 		    if (internalY < (TileConstants.TILE_HEIGHT / 3)) {
 			if ((flags & TileFlags.WALL_NORTH) == TileFlags.WALL_NORTH)
 			    flags &= (~TileFlags.WALL_NORTH);
 			else
 			    flags |= TileFlags.WALL_NORTH;
 		    } else if (internalY > 2 * (TileConstants.TILE_HEIGHT / 3)) {
 			if ((flags & TileFlags.WALL_SOUTH) == TileFlags.WALL_SOUTH)
 			    flags &= (~TileFlags.WALL_SOUTH);
 			else
 			    flags |= TileFlags.WALL_SOUTH;
 		    } else {
 			if ((flags & TileFlags.BLOCKED) == TileFlags.BLOCKED) {
 			    flags = 0;
 			    if (tileX > 0) {
 				s.getLocalTile(tileX - 1, tileY).removeFlag(
 					TileFlags.WALL_EAST);
 			    }
 			    if (tileY > 0) {
 				s.getLocalTile(tileX, tileY - 1).removeFlag(
 					TileFlags.WALL_SOUTH);
 			    }
 			    if (tileX < SectorConstants.SECTOR_WIDTH - 1) {
 				s.getLocalTile(tileX + 1, tileY).removeFlag(
 					TileFlags.WALL_WEST);
 			    }
 			    if (tileY < SectorConstants.SECTOR_HEIGHT - 1) {
 				s.getLocalTile(tileX, tileY - 1).removeFlag(
 					TileFlags.WALL_NORTH);
 			    }
 			} else {
 			    flags |= TileFlags.BLOCKED;
 			    if (tileX > 0) {
 				s.getLocalTile(tileX - 1, tileY).applyFlag(
 					TileFlags.WALL_EAST);
 			    }
 			    if (tileY > 0) {
 				s.getLocalTile(tileX, tileY - 1).applyFlag(
 					TileFlags.WALL_SOUTH);
 			    }
 			    if (tileX < SectorConstants.SECTOR_WIDTH - 1) {
 				s.getLocalTile(tileX + 1, tileY).applyFlag(
 					TileFlags.WALL_WEST);
 			    }
 			    if (tileY < SectorConstants.SECTOR_HEIGHT - 1) {
 				s.getLocalTile(tileX, tileY + 1).applyFlag(
 					TileFlags.WALL_NORTH);
 			    }
 			}
 		    }
 		}
 		s.getLocalTile(tileX, tileY).setFlags(flags);
 	    }
 	}
     }
 
     @Override
     public void onMapDrag(Sector s, int tileX, int tileY, int internalX,
 	    int internalY) {
 	onMapClick(s, tileX, tileY, internalX, internalY);
     }
 }
