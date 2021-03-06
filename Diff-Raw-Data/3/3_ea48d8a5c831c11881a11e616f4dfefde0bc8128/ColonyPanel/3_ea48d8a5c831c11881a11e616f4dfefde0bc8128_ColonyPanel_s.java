 /**
  *  Copyright (C) 2002-2011  The FreeCol Team
  *
  *  This file is part of FreeCol.
  *
  *  FreeCol is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  FreeCol is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package net.sf.freecol.client.gui.panel;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Container;
 import java.awt.Dimension;
 import java.awt.FlowLayout;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Insets;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.dnd.Autoscroll;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.logging.Logger;
 
 import javax.swing.BorderFactory;
 import javax.swing.ComponentInputMap;
 import javax.swing.ImageIcon;
 import javax.swing.InputMap;
 import javax.swing.JButton;
 import javax.swing.JComboBox;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JToolTip;
 import javax.swing.JViewport;
 import javax.swing.KeyStroke;
 import javax.swing.ScrollPaneConstants;
 import javax.swing.SwingUtilities;
 import javax.swing.border.BevelBorder;
 
 import net.miginfocom.swing.MigLayout;
 
 import net.sf.freecol.FreeCol;
 import net.sf.freecol.client.ClientOptions;
 import net.sf.freecol.client.gui.Canvas;
 import net.sf.freecol.client.gui.GUI;
 import net.sf.freecol.client.gui.i18n.Messages;
 import net.sf.freecol.client.gui.panel.ChoiceItem;
 import net.sf.freecol.common.model.AbstractGoods;
 import net.sf.freecol.common.model.BuildableType;
 import net.sf.freecol.common.model.Building;
 import net.sf.freecol.common.model.Colony;
 import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
 import net.sf.freecol.common.model.ColonyTile;
 import net.sf.freecol.common.model.FreeColGameObject;
 import net.sf.freecol.common.model.Goods;
 import net.sf.freecol.common.model.GoodsContainer;
 import net.sf.freecol.common.model.GoodsType;
 import net.sf.freecol.common.model.IndianSettlement;
 import net.sf.freecol.common.model.Map.Direction;
 import net.sf.freecol.common.model.ModelMessage;
 import net.sf.freecol.common.model.Player;
 import net.sf.freecol.common.model.ProductionInfo;
 import net.sf.freecol.common.model.Settlement;
 import net.sf.freecol.common.model.StringTemplate;
 import net.sf.freecol.common.model.Tile;
 import net.sf.freecol.common.model.TileType;
 import net.sf.freecol.common.model.TradeRoute;
 import net.sf.freecol.common.model.TypeCountMap;
 import net.sf.freecol.common.model.Unit;
 import net.sf.freecol.common.networking.Message;
 
 
 /**
  * This is a panel for the Colony display. It shows the units that are working
  * in the colony, the buildings and much more.
  */
 public final class ColonyPanel extends FreeColPanel
     implements ActionListener, PropertyChangeListener {
 
     private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());
 
     /**
      * The height of the area in which autoscrolling should happen.
      */
     public static final int SCROLL_AREA_HEIGHT = 40;
 
     /**
      * The speed of the scrolling.
      */
     public static final int SCROLL_SPEED = 40;
 
     private static final int EXIT = 0,
         BUILDQUEUE = 1,
         UNLOAD = 2,
         WAREHOUSE = 4,
         FILL = 5,
         SETGOODS = 6;
 
     private final JPanel netProductionPanel = new JPanel();
     private final PopulationPanel populationPanel = new PopulationPanel();
 
     private final JComboBox nameBox;
 
     private final OutsideColonyPanel outsideColonyPanel;
 
     private final InPortPanel inPortPanel;
 
     private final ColonyCargoPanel cargoPanel;
 
     private final WarehousePanel warehousePanel;
 
     private final TilePanel tilePanel;
 
     private final BuildingsPanel buildingsPanel;
 
     private final ConstructionPanel constructionPanel;
 
     private final DefaultTransferHandler defaultTransferHandler;
 
     private final MouseListener pressListener;
 
     private final MouseListener releaseListener;
 
     private Colony colony;
 
     private java.util.Map<Object, ProductionInfo> productionMap;
 
     private TypeCountMap<GoodsType> netProduction;
 
     private UnitLabel selectedUnitLabel;
 
     private JButton exitButton = new JButton(Messages.message("close"));
 
     private JButton unloadButton = new JButton(Messages.message("unload"));
 
     private JButton fillButton = new JButton(Messages.message("fill"));
 
     private JButton warehouseButton = new JButton(Messages.message("warehouseDialog.name"));
 
     private JButton buildQueueButton = new JButton(Messages.message("colonyPanel.buildQueue"));
 
     private JButton setGoodsButton = (FreeCol.isInDebugMode())
         ? new JButton("Set Goods") : null;
 
     /**
      * The saved size of this panel.
      */
     private static Dimension savedSize = null;
 
 
     /**
      * The constructor for the panel.
      *
      * @param parent The parent of this panel
      */
     public ColonyPanel(final Canvas parent, Colony colony) {
         super(parent);
 
         setFocusCycleRoot(true);
 
         // Use ESCAPE for closing the ColonyPanel:
         InputMap closeInputMap = new ComponentInputMap(exitButton);
         closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
         closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
         SwingUtilities.replaceUIInputMap(exitButton, JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);
 
         InputMap unloadInputMap = new ComponentInputMap(unloadButton);
         unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false), "pressed");
         unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true), "released");
         SwingUtilities.replaceUIInputMap(unloadButton, JComponent.WHEN_IN_FOCUSED_WINDOW, unloadInputMap);
 
         InputMap fillInputMap = new ComponentInputMap(fillButton);
         fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false), "pressed");
         fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true), "released");
         SwingUtilities.replaceUIInputMap(fillButton, JComponent.WHEN_IN_FOCUSED_WINDOW, fillInputMap);
 
         netProductionPanel.setOpaque(false);
 
         constructionPanel = new ConstructionPanel(parent, colony, true);
         constructionPanel.setOpaque(true);
 
         outsideColonyPanel = new OutsideColonyPanel();
 
         inPortPanel = new InPortPanel();
 
         warehousePanel = new WarehousePanel(this);
 
         tilePanel = new TilePanel(this);
 
         buildingsPanel = new BuildingsPanel(this);
 
         cargoPanel = new ColonyCargoPanel(parent);
         cargoPanel.setParentPanel(this);
 
         defaultTransferHandler = new DefaultTransferHandler(parent, this);
         pressListener = new DragListener(this);
         releaseListener = new DropListener();
 
         JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel,
                                                           ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                           ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         outsideColonyScroll.getVerticalScrollBar().setUnitIncrement( 16 );
         JScrollPane inPortScroll = new JScrollPane(inPortPanel);
         inPortScroll.getVerticalScrollBar().setUnitIncrement( 16 );
         JScrollPane cargoScroll = new JScrollPane(cargoPanel,
                                                   ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
         JScrollPane warehouseScroll = new JScrollPane(warehousePanel,
                                                       ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
         JScrollPane tilesScroll = new JScrollPane(tilePanel,
                                                   ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
         JScrollPane buildingsScroll = new JScrollPane(buildingsPanel,
                                                       ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         buildingsScroll.getVerticalScrollBar().setUnitIncrement( 16 );
 
         // Make the colony label
         nameBox = new JComboBox();
         nameBox.setFont(smallHeaderFont);
         List<Colony> settlements = colony.getOwner().getColonies();
         sortColonies(settlements);
         for (Colony aColony : settlements) {
             nameBox.addItem(aColony);
         }
         nameBox.setSelectedItem(colony);
         nameBox.addActionListener(new ActionListener() {
                 public void actionPerformed(ActionEvent event) {
                     initialize((Colony) nameBox.getSelectedItem());
                 }
             });
 
         buildingsScroll.setAutoscrolls(true);
         buildingsScroll.getViewport().setOpaque(false);
         buildingsPanel.setOpaque(false);
 
         /** Borders */
         tilesScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
         buildingsScroll.setBorder(BorderFactory.createEtchedBorder());
         warehouseScroll.setBorder(BorderFactory.createEtchedBorder());
         cargoScroll.setBorder(BorderFactory.createEtchedBorder());
         inPortScroll.setBorder(BorderFactory.createEtchedBorder());
         outsideColonyScroll.setBorder(BorderFactory.createEtchedBorder());
 
         exitButton.setActionCommand(String.valueOf(EXIT));
         enterPressesWhenFocused(exitButton);
         exitButton.addActionListener(this);
 
         unloadButton.setActionCommand(String.valueOf(UNLOAD));
         enterPressesWhenFocused(unloadButton);
         unloadButton.addActionListener(this);
 
         fillButton.setActionCommand(String.valueOf(FILL));
         enterPressesWhenFocused(fillButton);
         fillButton.addActionListener(this);
 
         warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));
         enterPressesWhenFocused(warehouseButton);
         warehouseButton.addActionListener(this);
 
         buildQueueButton.setActionCommand(String.valueOf(BUILDQUEUE));
         enterPressesWhenFocused(buildQueueButton);
         buildQueueButton.addActionListener(this);
 
         if (setGoodsButton != null) {
             setGoodsButton.setActionCommand(String.valueOf(SETGOODS));
             enterPressesWhenFocused(setGoodsButton);
             setGoodsButton.addActionListener(this);
         }
 
         selectedUnitLabel = null;
 
         // See the message of Ulf Onnen for more information about the presence
         // of this fake mouse listener.
         addMouseListener(new MouseAdapter() {});
 
         setLayout(new MigLayout("fill, wrap 2, insets 2", "[390!][fill]",
                                 "[][]0[]0[][growprio 200,shrinkprio 10][growprio 150,shrinkprio 50]"));
 
         add(nameBox, "height 48:, grow");
         add(netProductionPanel, "growx");
         add(tilesScroll, "width 390!, height 200!, top");
         add(buildingsScroll, "span 1 3, grow");
         add(populationPanel, "grow");
         add(constructionPanel, "grow, top");
         add(inPortScroll, "span, split 3, grow, sg, height 60:121:");
         add(cargoScroll, "grow, sg, height 60:121:");
         add(outsideColonyScroll, "grow, sg, height 60:121:");
         add(warehouseScroll, "span, height 40:60:, growx");
         add(unloadButton, "span, split "
             + Integer.toString((setGoodsButton == null) ? 5 : 6)
             + ", align center");
         add(fillButton);
         add(warehouseButton);
         add(buildQueueButton);
         if (setGoodsButton != null) add(setGoodsButton);
         add(exitButton);
 
         initialize(colony);
         if (savedSize != null) {
             setPreferredSize(savedSize);
         }
     }
 
     @Override
     public void requestFocus() {
         exitButton.requestFocus();
     }
 
     /**
      * Get the <code>SavedSize</code> value.
      *
      * @return a <code>Dimension</code> value
      */
     public final Dimension getSavedSize() {
         return savedSize;
     }
 
     /**
      * Set the <code>SavedSize</code> value.
      *
      * @param newSavedSize The new SavedSize value.
      */
     public final void setSavedSize(final Dimension newSavedSize) {
         ColonyPanel.savedSize = newSavedSize;
     }
 
     /**
      * Returns a pointer to the <code>CargoPanel</code>-object in use.
      *
      * @return The <code>CargoPanel</code>.
      */
     public final CargoPanel getCargoPanel() {
         return cargoPanel;
     }
 
     /**
      * Returns a pointer to the <code>WarehousePanel</code>-object in use.
      *
      * @return The <code>WarehousePanel</code>.
      */
     public final WarehousePanel getWarehousePanel() {
         return warehousePanel;
     }
 
     /**
      * Returns a pointer to the <code>TilePanel</code>-object in use.
      *
      * @return The <code>TilePanel</code>.
      */
     public final TilePanel getTilePanel() {
         return tilePanel;
     }
 
     /**
      * Returns a pointer to the <code>Colony</code>-pointer in use.
      *
      * @return The <code>Colony</code>.
      */
     public synchronized final Colony getColony() {
         return colony;
     }
 
     /**
      * Set the current colony.
      *
      * @param colony The new colony value.
      */
     private synchronized void setColony(Colony colony) {
         removePropertyChangeListeners();
         this.colony = colony;
         addPropertyChangeListeners();
         editable = colony.getOwner() == getMyPlayer();
     }
 
     public void updateConstructionPanel() {
         constructionPanel.update();
     }
 
     public void updateInPortPanel() {
         inPortPanel.initialize();
     }
 
     public void updateWarehousePanel() {
         warehousePanel.update();
     }
 
     public void updateOutsideColonyPanel() {
         outsideColonyPanel.initialize();
     }
 
     public void updateTilePanel() {
         tilePanel.initialize();
     }
 
     public void updateProductionPanel() {
         netProductionPanel.removeAll();
         productionMap = colony.getProductionAndConsumption();
         netProduction = colony.getNetProduction(productionMap);
 
         for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
             int amount = netProduction.getCount(goodsType);
             if (amount != 0) {
                 netProductionPanel.add(new ProductionLabel(goodsType, amount, getCanvas()));
             }
         }
 
         netProductionPanel.revalidate();
     }
 
     private void sortBuildings(List<Building> buildings) {
         Collections.sort(buildings);
     }
 
     private void sortColonies(List<Colony> colonies) {
         Collections.sort(colonies, getClient().getClientOptions()
                          .getColonyComparator());
     }
 
     /**
      * Analyzes an event and calls the right external methods to take
      * care of the user's request.
      *
      * @param event The incoming action event
      */
     public void actionPerformed(ActionEvent event) {
         Canvas canvas = getCanvas();
         Colony colony = getColony();
         String command = event.getActionCommand();
         try {
             switch (Integer.valueOf(command).intValue()) {
             case EXIT:
                 closeColonyPanel();
                 break;
             case UNLOAD:
                 unload();
                 break;
             case WAREHOUSE:
                 if (canvas.showFreeColDialog(new WarehouseDialog(canvas,
                                                                  colony))) {
                     updateWarehousePanel();
                 }
                 break;
             case BUILDQUEUE:
                 canvas.showSubPanel(new BuildQueuePanel(colony, canvas));
                 updateConstructionPanel();
                 break;
             case FILL:
                 fill();
                 break;
             case SETGOODS:
                 debugSetGoods(canvas, colony);
                 break;
             default:
                 logger.warning("Invalid action");
                 break;
             }
         } catch (NumberFormatException e) {
             logger.warning("Invalid action number: " + command);
         }
     }
 
     /**
      * Interactive debug-mode change of goods amount in a colony.
      *
      * @param canvas The <code>Canvas</code> to use.
      * @param colony The <code>Colony</code> to set goods amounts in.
      */
     private void debugSetGoods(Canvas canvas, Colony colony) {
         List<ChoiceItem<GoodsType>> gtl
             = new ArrayList<ChoiceItem<GoodsType>>();
         for (GoodsType t : getSpecification().getGoodsTypeList()) {
             gtl.add(new ChoiceItem<GoodsType>(Messages.message(t.toString() + ".name"),
                                               t));
         }
         GoodsType goodsType = canvas.showChoiceDialog(null, "Select Goods Type",
                                                       "Cancel", gtl);
         if (goodsType == null) return;
         String amount = canvas.showInputDialog(null, "Select Goods Amount",
                 Integer.toString(colony.getGoodsCount(goodsType)),
                 "ok", "cancel", true);
         if (amount == null) return;
         int a;
         try {
             a = Integer.parseInt(amount);
         } catch (NumberFormatException nfe) {
             return;
         }
         GoodsContainer cgc = colony.getGoodsContainer();
         GoodsContainer sgc = (GoodsContainer) getClient().getFreeColServer()
             .getGame().getFreeColGameObject(cgc.getId());
         sgc.setAmount(goodsType, a);
         cgc = new GoodsContainer(getGame(), colony,
                 sgc.toXMLElement(Message.createNewDocument()));
         colony.setGoodsContainer(cgc);
         updateConstructionPanel();
         updateProductionPanel();
         updateWarehousePanel();
     }
 
     /**
      * Unloads all goods and units from the carrier currently selected.
      */
     private void unload() {
         Unit unit = getSelectedUnit();
         if (unit != null && unit.isCarrier()) {
             for (Goods goods : unit.getGoodsContainer().getGoods()) {
                 getController().unloadCargo(goods, false);
             }
             for (Unit u : new ArrayList<Unit>(unit.getUnitList())) {
                 getController().leaveShip(u);
             }
             cargoPanel.initialize();
             outsideColonyPanel.initialize();
         }
         unloadButton.setEnabled(false);
         fillButton.setEnabled(false);
     }
 
     /**
      * Fill goods from the carrier currently selected to capacity.
      */
     private void fill() {
         Unit unit = getSelectedUnit();
         if (unit != null && unit.isCarrier()) {
             for (Goods goods : unit.getGoodsContainer().getGoods()) {
                 int space = GoodsContainer.CARGO_SIZE - goods.getAmount();
                 int count = getColony().getGoodsCount(goods.getType());
                 if (space > 0 && count > 0) {
                     Goods newGoods = new Goods(goods.getGame(), getColony(),
                                                goods.getType(),
                                                Math.min(space, count));
                     getController().loadCargo(newGoods, unit);
                 }
             }
         }
     }
 
     /**
      * Enables the unload and fill buttons if the currently selected unit is a
      * carrier with some cargo.
      */
     private void updateCarrierButtons() {
         unloadButton.setEnabled(false);
         fillButton.setEnabled(false);
         if (isEditable() && selectedUnitLabel != null) {
             Unit unit = selectedUnitLabel.getUnit();
             if (unit != null && unit.isCarrier()
                 && unit.getSpaceLeft() < unit.getType().getSpace()) {
                 unloadButton.setEnabled(true);
                 for (Goods goods : unit.getGoodsList()) {
                     if (getColony().getGoodsCount(goods.getType()) > 0) {
                         fillButton.setEnabled(true);
                         break;
                     }
                 }
             }
         }
     }
 
     /**
      * Returns the currently select unit.
      *
      * @return The currently select unit.
      */
     public Unit getSelectedUnit() {
         return (selectedUnitLabel == null) ? null
             : selectedUnitLabel.getUnit();
     }
 
     /**
      * Selects a unit that is potentially located somewhere in port.
      *
      * @param unit The <code>Unit</code> to select.
      */
     public void setSelectedUnit(Unit unit) {
         UnitLabel unitLabel = null;
 
         if (unit != null) {
             Component[] components = inPortPanel.getComponents();
             for (int i = 0; i < components.length; i++) {
                 if (components[i] instanceof UnitLabel
                     && ((UnitLabel) components[i]).getUnit() == unit) {
                     unitLabel = (UnitLabel) components[i];
                     break;
                 }
             }
         }
 
         setSelectedUnitLabel(unitLabel);
     }
 
     /**
      * Returns the currently select unit label.
      *
      * @return The currently select unit label.
      */
     public UnitLabel getSelectedUnitLabel() {
         return selectedUnitLabel;
     }
 
     /**
      * Selects a unit that is located somewhere on this panel.
      *
      * @param unitLabel The <code>UnitLabel</code> for the unit that
      *     is being selected.
      */
     public void setSelectedUnitLabel(UnitLabel unitLabel) {
         if (selectedUnitLabel != unitLabel) {
             if (selectedUnitLabel != null) {
                 selectedUnitLabel.setSelected(false);
                 selectedUnitLabel.getUnit().removePropertyChangeListener(this);
             }
             selectedUnitLabel = unitLabel;
             if (unitLabel == null) {
                 cargoPanel.setCarrier(null);
             } else {
                 cargoPanel.setCarrier(unitLabel.getUnit());
                 unitLabel.setSelected(true);
                 unitLabel.getUnit().addPropertyChangeListener(this);
             }
         }
         updateCarrierButtons();
         inPortPanel.revalidate();
         inPortPanel.repaint();
     }
 
     /**
      * Initialize the data on the window. This is the same as calling:
      * <code>initialize(colony, game, null)</code>.
      *
      * @param colony The <code>Colony</code> to be displayed.
      */
     private void initialize(Colony colony) {
         setColony(colony);
         productionMap = colony.getProductionAndConsumption();
         netProduction = colony.getNetProduction(productionMap);
 
         // Set listeners and transfer handlers
         outsideColonyPanel.removeMouseListener(releaseListener);
         inPortPanel.removeMouseListener(releaseListener);
         cargoPanel.removeMouseListener(releaseListener);
         warehousePanel.removeMouseListener(releaseListener);
         if (isEditable()) {
             outsideColonyPanel.setTransferHandler(defaultTransferHandler);
             inPortPanel.setTransferHandler(defaultTransferHandler);
             cargoPanel.setTransferHandler(defaultTransferHandler);
             warehousePanel.setTransferHandler(defaultTransferHandler);
 
             outsideColonyPanel.addMouseListener(releaseListener);
             inPortPanel.addMouseListener(releaseListener);
             cargoPanel.addMouseListener(releaseListener);
             warehousePanel.addMouseListener(releaseListener);
         } else {
             outsideColonyPanel.setTransferHandler(null);
             inPortPanel.setTransferHandler(null);
             cargoPanel.setTransferHandler(null);
             warehousePanel.setTransferHandler(null);
         }
 
         // Enable/disable widgets
         unloadButton.setEnabled(isEditable());
         fillButton.setEnabled(isEditable());
         warehouseButton.setEnabled(isEditable());
         nameBox.setEnabled(isEditable());
 
         // update all the subpanels
         cargoPanel.setCarrier(null);
 
         inPortPanel.initialize();
         warehousePanel.initialize();
         buildingsPanel.initialize();
         tilePanel.initialize();
 
         updateProductionPanel();
         populationPanel.update();
 
         constructionPanel.setColony(colony);
         outsideColonyPanel.setColony(colony);
     }
 
     /**
      * Closes the <code>ColonyPanel</code>.
      */
     public void closeColonyPanel() {
         Canvas canvas = getCanvas();
         if (getColony().getUnitCount() == 0) {
             if (canvas.showConfirmDialog("abandonColony.text",
                                          "abandonColony.yes",
                                          "abandonColony.no")) {
                 canvas.remove(this);
                 getController().abandonColony(getColony());
             }
         } else {
             BuildableType buildable = getColony().getCurrentlyBuilding();
             if (buildable != null
                 && buildable.getPopulationRequired() > getColony().getUnitCount()
                 && !canvas.showConfirmDialog(null, StringTemplate.template("colonyPanel.reducePopulation")
                                              .addName("%colony%", getColony().getName())
                                              .addAmount("%number%", buildable.getPopulationRequired())
                                              .add("%buildable%", buildable.getNameKey()),
                                              "ok", "cancel")) {
                 return;
             }
             canvas.remove(this);
 
             // remove property listeners
             removePropertyChangeListeners();
             if (getSelectedUnit() != null) {
                 getSelectedUnit().removePropertyChangeListener(this);
             }
             buildingsPanel.cleanup();
             warehousePanel.cleanup();
             tilePanel.cleanup();
             constructionPanel.removePropertyChangeListeners();
             cargoPanel.setCarrier(null);
             outsideColonyPanel.cleanup();
 
             if (getGame().getCurrentPlayer() == getMyPlayer()) {
                 getController().nextModelMessage();
                 Unit activeUnit = canvas.getGUI().getActiveUnit();
                 if (activeUnit == null || activeUnit.getTile() == null || activeUnit.getMovesLeft() <= 0
                     || (!(activeUnit.getLocation() instanceof Tile) && !(activeUnit.isOnCarrier()))) {
                     canvas.getGUI().setActiveUnit(null);
                     getController().nextActiveUnit();
                 }
             }
             getClient().getGUI().restartBlinking();
         }
     }
 
     /**
      * Add property change listeners needed by this ColonyPanel.
      */
     private void addPropertyChangeListeners() {
         Colony colony = getColony();
         if (colony != null) {
             colony.addPropertyChangeListener(this);
             colony.getGoodsContainer().addPropertyChangeListener(this);
             colony.getTile().addPropertyChangeListener(this);
         }
     }
 
     /**
      * Remove the property change listeners of ColonyPanel.
      */
     private void removePropertyChangeListeners() {
         Colony colony = getColony();
         if (colony != null) {
             colony.removePropertyChangeListener(this);
             colony.getGoodsContainer().removePropertyChangeListener(this);
             colony.getTile().removePropertyChangeListener(this);
         }
     }
 
     /**
      * Handle a property change event sent to this ColonyPanel.
      *
      * @param event The <code>PropertyChangeEvent</code> to handle.
      */
     public void propertyChange(PropertyChangeEvent event) {
         if (!isShowing() || getColony() == null) {
             return;
         }
         String property = event.getPropertyName();
         logger.finest(getColony().getName() + " change " + property
                       + ": " + event.getOldValue()
                       + " -> " + event.getNewValue());
 
         if (property == null) {
             logger.warning("Null property change");
         } else if (Unit.CARGO_CHANGE.equals(property)) {
             updateInPortPanel();
         } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
             populationPanel.update();
             updateProductionPanel(); // food production changes
         } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
             ModelMessage msg = getColony().checkForGovMgtChangeMessage();
             if (msg != null) {
                 getCanvas().showInformationMessage(msg);
             }
             populationPanel.update();
         } else if (ColonyChangeEvent.UNIT_TYPE_CHANGE.toString().equals(property)) {
             FreeColGameObject object = (FreeColGameObject) event.getSource();
             String oldType = (String) event.getOldValue();
             String newType = (String) event.getNewValue();
             getCanvas().showInformationMessage(object,
                                                "model.colony.unitChange",
                                                "%oldType%", oldType,
                                                "%newType%", newType);
             updateTilePanel();
         } else if (ColonyTile.UNIT_CHANGE.toString().equals(property)) {
             // Note: ColonyTile.UNIT_CHANGE.equals(Building.UNIT_CHANGE)
             updateTilePanel();
             updateProductionPanel();
         } else if (property.startsWith("model.goods.")) {
             // Changes to warehouse goods count may affect building production
             // which requires a view update.
             updateProductionPanel();
             updateWarehousePanel();
             buildingsPanel.update();
             updateConstructionPanel();
         } else if (Tile.UNIT_CHANGE.equals(property)) {
             updateOutsideColonyPanel();
             updateInPortPanel();
         } else {
             logger.warning("Unknown property change event: "
                            + event.getPropertyName());
         }
     }
 
 
     /**
      * This panel shows the content of a carrier in the colony
      */
     public final class ColonyCargoPanel extends CargoPanel {
 
         public ColonyCargoPanel(Canvas canvas) {
             super(canvas, true);
             //setLayout(new MigLayout("wrap 4, fill"));
         }
 
         @Override
         public void update() {
             super.update();
             // May have un/loaded cargo, "Unload" could have changed validity
             updateCarrierButtons();
         }
 
         @Override
         public String getUIClassID() {
             return "CargoPanelUI";
         }
     }
 
     /**
      * This panel is a list of the colony's buildings.
      */
     public final class BuildingsPanel extends JPanel {
 
         private final ColonyPanel colonyPanel;
 
 
         /**
          * Creates this BuildingsPanel.
          *
          * @param colonyPanel The panel that holds this BuildingsPanel.
          */
         public BuildingsPanel(ColonyPanel colonyPanel) {
             setLayout(new MigLayout("fill, wrap 4, gap 0:10:10:push"));
             this.colonyPanel = colonyPanel;
         }
 
         /**
          * Initializes the <code>BuildingsPanel</code> by loading/displaying
          * the buildings of the colony.
          */
         public void initialize() {
             update();
         }
 
         public void cleanup() {
             removePropertyChangeListeners();
         }
 
         public void update() {
             removePropertyChangeListeners();
             removeAll();
 
             MouseAdapter mouseAdapter = new MouseAdapter() {
                     public void mousePressed(MouseEvent e) {
                         getCanvas().showSubPanel(new BuildQueuePanel(getColony(), getCanvas()));
                     }
                 };
             ASingleBuildingPanel aSingleBuildingPanel;
 
             List<Building> buildings = getColony().getBuildings();
             sortBuildings(buildings);
             for (Building building : buildings) {
                 aSingleBuildingPanel = new ASingleBuildingPanel(building);
                 if (colonyPanel.isEditable()) {
                     aSingleBuildingPanel.addMouseListener(releaseListener);
                     aSingleBuildingPanel.setTransferHandler(defaultTransferHandler);
                 }
                 aSingleBuildingPanel.setOpaque(false);
                 aSingleBuildingPanel.addMouseListener(mouseAdapter);
                 add(aSingleBuildingPanel);
             }
         }
 
         private void removePropertyChangeListeners() {
             for (Component component : getComponents()) {
                 if (component instanceof ASingleBuildingPanel) {
                     ((ASingleBuildingPanel) component).removePropertyChangeListeners();
                 }
             }
         }
 
         @Override
         public String getUIClassID() {
             return "BuildingsPanelUI";
         }
 
 
         /**
          * This panel is a single line (one building) in the
          * <code>BuildingsPanel</code>.
          */
         public final class ASingleBuildingPanel extends BuildingPanel implements Autoscroll {
 
             /**
              * Creates this ASingleBuildingPanel.
              *
              * @param building The building to display information from.
              */
             public ASingleBuildingPanel(Building building) {
                 super(building, productionMap.get(building), getCanvas());
             }
 
             public void autoscroll(Point p) {
                 JViewport vp = (JViewport) colonyPanel.buildingsPanel.getParent();
                 if (getLocation().y + p.y - vp.getViewPosition().y < SCROLL_AREA_HEIGHT) {
                     vp.setViewPosition(new Point(vp.getViewPosition().x,
                                                  Math.max(vp.getViewPosition().y - SCROLL_SPEED, 0)));
                 } else if (getLocation().y + p.y - vp.getViewPosition().y >= vp.getHeight() - SCROLL_AREA_HEIGHT) {
                     vp.setViewPosition(new Point(vp.getViewPosition().x,
                                                  Math.min(vp.getViewPosition().y + SCROLL_SPEED,
                                                           colonyPanel.buildingsPanel.getHeight()
                                                           - vp.getHeight())));
                 }
             }
 
             public Insets getAutoscrollInsets() {
                 Rectangle r = getBounds();
                 return new Insets(r.x, r.y, r.width, r.height);
             }
 
 
             public void initialize() {
                 super.initialize();
                 if (colonyPanel.isEditable()) {
                     for (UnitLabel unitLabel : getUnitLabels()) {
                         unitLabel.setTransferHandler(defaultTransferHandler);
                         unitLabel.addMouseListener(pressListener);
                     }
                 }
             }
 
             /**
              * Adds a component to this ASingleBuildingPanel and makes
              * sure that the unit that the component represents gets
              * modified so that it will be located in the colony.
              *
              * @param comp The component to add to this ColonistsPanel.
              * @param editState Must be set to 'true' if the state of the
              *            component that is added (which should be a dropped
              *            component representing a Unit) should be changed so
              *            that the underlying unit will be located in the
              *            colony.
              * @return The component argument.
              */
             public Component add(Component comp, boolean editState) {
                 Container oldParent = comp.getParent();
 
                 if (editState) {
                     if (comp instanceof UnitLabel) {
                         Unit unit = ((UnitLabel) comp).getUnit();
 
                         if (getBuilding().canAdd(unit)) {
                             //oldParent.remove(comp);
                             getController().work(unit, getBuilding());
                         } else {
                             return null;
                         }
                     } else {
                         logger.warning("An invalid component got dropped on this BuildingsPanel.");
                         return null;
                     }
                 }
                 initialize();
                 return null;
             }
         }
     }
 
     public final class PopulationPanel extends JPanel {
 
         private final JLabel rebelShield = new JLabel();
         private final JLabel rebelLabel = new JLabel();
         private final JLabel bonusLabel = new JLabel();
         private final JLabel royalistLabel = new JLabel();
         private final JLabel royalistShield = new JLabel();
         private final JLabel rebelMemberLabel = new JLabel();
         private final JLabel popLabel = new JLabel();
         private final JLabel royalistMemberLabel = new JLabel();
 
         public PopulationPanel() {
             setOpaque(false);
             setToolTipText(" ");
             setLayout(new MigLayout("wrap 5, fill, insets 0",
                                     "[][]:push[center]:push[right][]"));
             add(rebelShield, "bottom");
             add(rebelLabel, "split 2, flowy");
             add(rebelMemberLabel);
             add(popLabel, "split 2, flowy");
             add(bonusLabel);
             add(royalistLabel, "split 2, flowy");
             add(royalistMemberLabel);
             add(royalistShield, "bottom");
         }
 
         public JToolTip createToolTip() {
             return new RebelToolTip(getColony(), netProduction, getCanvas());
         }
 
         public void update() {
             int population = getColony().getUnitCount();
             int members = getColony().getMembers();
             int rebels = getColony().getSoL();
             String rebelNumber = Messages.message("colonyPanel.rebelLabel", "%number%",
                                                   Integer.toString(members));
             String royalistNumber = Messages.message("colonyPanel.royalistLabel", "%number%",
                                                      Integer.toString(population - members));
 
             popLabel.setText(Messages.message("colonyPanel.populationLabel", "%number%",
                                               Integer.toString(population)));
             rebelLabel.setText(rebelNumber);
             rebelMemberLabel.setText(Integer.toString(rebels) + "%");
             bonusLabel.setText(Messages.message("colonyPanel.bonusLabel", "%number%",
                                                 Integer.toString(getColony().getProductionBonus())));
             royalistLabel.setText(royalistNumber);
             royalistMemberLabel.setText(Integer.toString(getColony().getTory()) + "%");
             rebelShield.setIcon(new ImageIcon(getLibrary().getCoatOfArmsImage(getColony().getOwner().getNation(), 0.5)));
             royalistShield.setIcon(new ImageIcon(getLibrary().getCoatOfArmsImage(getColony().getOwner().getNation()
                                                                                  .getRefNation(), 0.5)));
             revalidate();
             repaint();
         }
 
         @Override
         public String getUIClassID() {
             return "PopulationPanelUI";
         }
     };
 
     /**
      * A panel that holds UnitLabels that represent Units that are standing in
      * front of a colony.
      */
     public final class OutsideColonyPanel extends JPanel
         implements PropertyChangeListener {
 
         private Colony colony;
 
         public OutsideColonyPanel() {
             super(new MigLayout("wrap 4, fill"));
             setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                        Messages.message("outsideColony")));
         }
 
         public Colony getColony() {
             return colony;
         }
 
         public void setColony(Colony newColony) {
             removePropertyChangeListeners();
             this.colony = newColony;
             addPropertyChangeListeners();
             initialize();
         }
 
         public void initialize() {
             removeAll();
             if (getColony() == null) return;
 
             Tile colonyTile = getColony().getTile();
             for (Unit unit : colonyTile.getUnitList()) {
                 // we only deal with land, non-carrier units here
                 if (unit.isNaval() || unit.isCarrier()) {
                     continue;
                 }
 
                 UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
                 if (isEditable()) {
                     unitLabel.setTransferHandler(defaultTransferHandler);
                     unitLabel.addMouseListener(pressListener);
                 }
                 add(unitLabel, false);
             }
             revalidate();
             repaint();
         }
 
         public void cleanup() {
             removePropertyChangeListeners();
         }
 
         /**
          * Adds a component to this OutsideColonyPanel and makes sure
          * that the unit that the component represents gets modified
          * so that it will be located in the colony.
          *
          * @param comp The component to add to this ColonistsPanel.
          * @param editState Must be set to 'true' if the state of the component
          *            that is added (which should be a dropped component
          *            representing a Unit) should be changed so that the
          *            underlying unit will be located in the colony.
          * @return The component argument.
          */
         public Component add(Component comp, boolean editState) {
             Container oldParent = comp.getParent();
             if (editState) {
                 if (comp instanceof UnitLabel) {
                     UnitLabel unitLabel = ((UnitLabel) comp);
                     Unit unit = unitLabel.getUnit();
 
                     if (!unit.isOnCarrier()) {
                         getController().putOutsideColony(unit);
                     }
 
                     if (unit.getColony() == null) {
                         closeColonyPanel();
                         return null;
                     } else if (!(unit.getLocation() instanceof Tile) && !unit.isOnCarrier()) {
                         return null;
                     }
 
                     oldParent.remove(comp);
                     initialize();
                     return comp;
                 } else {
                     logger.warning("An invalid component got dropped on this ColonistsPanel.");
                     return null;
                 }
             } else {
                 ((UnitLabel) comp).setSmall(false);
                 Component c = add(comp);
                 return c;
             }
         }
 
         private void addPropertyChangeListeners() {
             Colony colony = getColony();
             if (colony != null) {
                 colony.getTile().addPropertyChangeListener(Tile.UNIT_CHANGE,
                                                            this);
             }
         }
 
         private void removePropertyChangeListeners() {
             Colony colony = getColony();
             if (colony != null) {
                 colony.getTile().removePropertyChangeListener(Tile.UNIT_CHANGE,
                                                               this);
             }
         }
 
         public void propertyChange(PropertyChangeEvent event) {
             String property = event.getPropertyName();
             logger.finest("Outside " + getColony().getId()
                           + " change " + property
                           + ": " + event.getOldValue()
                           + " -> " + event.getNewValue());
             initialize();
         }
 
         @Override
         public String getUIClassID() {
             return "OutsideColonyPanelUI";
         }
     }
 
     /**
      * A panel that holds UnitsLabels that represent naval Units that are
      * waiting in the port of the colony.
      */
     public final class InPortPanel extends JPanel {
 
         public InPortPanel() {
             super(new MigLayout("wrap 3, fill"));
             setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                        Messages.message("inPort")));
         }
 
         public void initialize() {
             removeAll();
             if (getColony() == null) return;
 
             UnitLabel lastCarrier = null;
             UnitLabel prevCarrier = null;
             for (Unit unit : getColony().getTile().getUnitList()) {
                 if (!unit.isCarrier()) continue;
 
                 UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
                 TradeRoute tradeRoute = unit.getTradeRoute();
                 if (tradeRoute != null) {
                     unitLabel.setDescriptionLabel(Messages.message(Messages.getLabel(unit))
                                                   + " (" + tradeRoute.getName() + ")");
                 }
                 if (isEditable()) {
                     unitLabel.setTransferHandler(defaultTransferHandler);
                     unitLabel.addMouseListener(pressListener);
                 }
                 add(unitLabel);
 
                 lastCarrier = unitLabel;
                 if (getSelectedUnit() == unit) prevCarrier = unitLabel;
             }
 
             // Keep the previous selected unit if possible, otherwise default
             // on the last carrier.
             setSelectedUnitLabel((prevCarrier != null) ? prevCarrier
                                  : (lastCarrier != null) ? lastCarrier
                                  : null);
             // No revalidate+repaint as this is done in setSelectedUnitLabel
         }
 
         @Override
         public String getUIClassID() {
             return "InPortPanelUI";
         }
     }
 
     /**
      * A panel that holds goods that represent cargo that is inside
      * the Colony.
      */
     public final class WarehousePanel extends JPanel
         implements PropertyChangeListener {
 
         private final ColonyPanel colonyPanel;
 
         /**
          * Creates this WarehousePanel.
          *
          * @param colonyPanel The panel that holds this WarehousePanel.
          */
         public WarehousePanel(ColonyPanel colonyPanel) {
             this.colonyPanel = colonyPanel;
             setLayout(new MigLayout("fill, gap push"));
         }
 
         /**
          * Initialize this WarehousePanel.
          */
         public void initialize() {
             addPropertyChangeListeners();
             update();
             revalidate();
             repaint();
         }
 
         /**
          * Clean up this WarehousePanel.
          */
         public void cleanup() {
             removePropertyChangeListeners();
         }
 
         /**
          * Update this WarehousePanel.
          */
         private void update() {
             final int threshold = getClient().getClientOptions()
                 .getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS);
             removeAll();
             for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                 Goods goods = getColony().getGoodsContainer().getGoods(goodsType);
                 if (goodsType.isStorable() && goods.getAmount() >= threshold) {
                     GoodsLabel goodsLabel = new GoodsLabel(goods, getCanvas());
                     if (colonyPanel.isEditable()) {
                         goodsLabel.setTransferHandler(defaultTransferHandler);
                         goodsLabel.addMouseListener(pressListener);
                     }
                     add(goodsLabel, false);
                 }
             }
             revalidate();
             repaint();
         }
 
         /**
          * Adds a component to this WarehousePanel and makes sure that
          * the unit or good that the component represents gets
          * modified so that it is on board the currently selected
          * ship.
          *
          * @param comp The component to add to this WarehousePanel.
          * @param editState Must be set to 'true' if the state of the
          *            component that is added (which should be a
          *            dropped component representing a Unit or good)
          *            should be changed so that the underlying unit or
          *            goods are on board the currently selected ship.
          * @return The component argument.
          */
         public Component add(Component comp, boolean editState) {
             if (editState) {
                 if (!(comp instanceof GoodsLabel)) {
                     logger.warning("Invalid component dropped on this WarehousePanel.");
                     return null;
                 }
                 comp.getParent().remove(comp);
                 ((GoodsLabel) comp).setSmall(false);
                 return comp;
             }
 
             Component c = add(comp);
             return c;
         }
 
         private void addPropertyChangeListeners() {
             Colony colony = getColony();
             if (colony != null) {
                 colony.getGoodsContainer().addPropertyChangeListener(this);
             }
         }
 
         private void removePropertyChangeListeners() {
             Colony colony = getColony();
             if (colony != null) {
                 colony.getGoodsContainer().removePropertyChangeListener(this);
             }
         }
 
         public void propertyChange(PropertyChangeEvent event) {
             logger.finest(getColony().getName() + "-warehouse change "
                           + event.getPropertyName()
                           + ": " + event.getOldValue()
                           + " -> " + event.getNewValue());
             update();
         }
 
         @Override
         public String getUIClassID() {
             return "WarehousePanelUI";
         }
     }
 
 
     /**
      * A panel that displays the tiles in the immediate area around the colony.
      */
     public final class TilePanel extends FreeColPanel {
 
         private final ColonyPanel colonyPanel;
         private Tile[][] tiles = new Tile[3][3];
 
         /**
          * Creates this TilePanel.
          *
          * @param colonyPanel The panel that holds this TilePanel.
          */
         public TilePanel(ColonyPanel colonyPanel) {
             super(colonyPanel.getCanvas());
             this.colonyPanel = colonyPanel;
             setBackground(Color.BLACK);
             setBorder(null);
             setLayout(null);
         }
 
         @Override
         public void paintComponent(Graphics g) {
             GUI colonyTileGUI = getCanvas().getColonyTileGUI();
 
             g.setColor(Color.black);
             g.fillRect(0, 0, getWidth(), getHeight());
 
             TileType tileType = getColony().getTile().getType();
             int tileWidth = getLibrary().getTerrainImageWidth(tileType) / 2;
             int tileHeight = getLibrary().getTerrainImageHeight(tileType) / 2;
             if (getColony() != null) {
                 for (int x = 0; x < 3; x++) {
                     for (int y = 0; y < 3; y++) {
                         if (tiles[x][y] != null) {
                             int xx = ((2 - x) + y) * tileWidth;
                             int yy = (x + y) * tileHeight;
                             g.translate(xx, yy);
                             colonyTileGUI.displayColonyTile((Graphics2D) g, tiles[x][y], getColony());
                             g.translate(-xx, -yy);
                         }
                     }
                 }
             }
         }
 
         public void initialize() {
             removePropertyChangeListeners();
             removeAll();
             Tile tile = getColony().getTile();
             tiles[0][0] = tile.getNeighbourOrNull(Direction.N);
             tiles[0][1] = tile.getNeighbourOrNull(Direction.NE);
             tiles[0][2] = tile.getNeighbourOrNull(Direction.E);
             tiles[1][0] = tile.getNeighbourOrNull(Direction.NW);
             tiles[1][1] = tile;
             tiles[1][2] = tile.getNeighbourOrNull(Direction.SE);
             tiles[2][0] = tile.getNeighbourOrNull(Direction.W);
             tiles[2][1] = tile.getNeighbourOrNull(Direction.SW);
             tiles[2][2] = tile.getNeighbourOrNull(Direction.S);
 
             int layer = 2;
 
             for (int x = 0; x < 3; x++) {
                 for (int y = 0; y < 3; y++) {
                     if (tiles[x][y] != null) {
                         ColonyTile colonyTile = getColony().getColonyTile(tiles[x][y]);
                         ASingleTilePanel p = new ASingleTilePanel(colonyTile, x, y);
                         add(p, new Integer(layer));
                         layer++;
                     }
                 }
             }
         }
 
         public void cleanup() {
             removePropertyChangeListeners();
         }
 
         private void removePropertyChangeListeners() {
             for (Component component : getComponents()) {
                 if (component instanceof ASingleTilePanel) {
                     ((ASingleTilePanel) component).removePropertyChangeListeners();
                 }
             }
         }
 
 
         /**
          * Panel for visualizing a <code>ColonyTile</code>. The component
          * itself is not visible, however the content of the component is (i.e.
          * the people working and the production)
          */
         public final class ASingleTilePanel extends JPanel implements PropertyChangeListener {
 
             private ColonyTile colonyTile;
 
             public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                 setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                 this.colonyTile = colonyTile;
 
                 addPropertyChangeListeners();
 
                 setOpaque(false);
                 TileType tileType = colonyTile.getTile().getType();
                 // Size and position:
                 setSize(getLibrary().getTerrainImageWidth(tileType), getLibrary().getTerrainImageHeight(tileType));
                 setLocation(((2 - x) + y) * getLibrary().getTerrainImageWidth(tileType) / 2,
                             (x + y) * getLibrary().getTerrainImage(tileType, 0, 0).getHeight(null) / 2);
                 initialize();
             }
 
             /**
              * Initialized the center of the colony panel tile. The one
              * containing the city.
              *
              */
             private void initializeAsCenterTile() {
 
                 setLayout(new MigLayout("wrap 1, center"));
 
                 for (AbstractGoods goods : productionMap.get(colonyTile).getProduction()) {
                     add(new ProductionLabel(goods, getCanvas()), "center");
                 }
             }
 
             /**
              * Updates the description label The description label is a tooltip
              * with the terrain type, road and plow indicator if any
              *
              * If a unit is on it update the tooltip of it instead
              */
             private void updateDescriptionLabel(UnitLabel unit, boolean toAdd) {
                 String tileDescription = Messages.message(this.colonyTile.getLabel());
 
                 if (unit == null) {
                     setToolTipText(tileDescription);
                 } else {
                     String unitDescription = Messages.message(Messages.getLabel(unit.getUnit()));
                     if (toAdd) {
                         unitDescription = tileDescription + " [" + unitDescription + "]";
                     }
                     unit.setDescriptionLabel(unitDescription);
                 }
             }
 
             /**
              * Checks if this <code>JComponent</code> contains the given
              * coordinate.
              */
             @Override
             public boolean contains(int px, int py) {
                 int w = getWidth();
                 int h = getHeight();
                 int dx = Math.abs(w/2 - px);
                 int dy = Math.abs(h/2 - py);
                 return (dx + w * dy / h) <= w/2;
             }
 
             private void initialize() {
 
                 removeAll();
                 UnitLabel unitLabel = null;
                 if (colonyTile.getUnit() != null) {
                     Unit unit = colonyTile.getUnit();
                     unitLabel = new UnitLabel(unit, getCanvas());
                     if (colonyPanel.isEditable()) {
                         unitLabel.setTransferHandler(defaultTransferHandler);
                         unitLabel.addMouseListener(pressListener);
                     }
                     super.add(unitLabel);
 
                 }
                 updateDescriptionLabel(unitLabel, true);
 
                 if (colonyTile.isColonyCenterTile()) {
                     initializeAsCenterTile();
                 }
 
                 if (colonyPanel.isEditable()) {
                     setTransferHandler(defaultTransferHandler);
                     addMouseListener(releaseListener);
                 }
 
                 revalidate();
                 repaint();
             }
 
             /**
              * Adds a component to this CargoPanel and makes sure that the unit
              * or good that the component represents gets modified so that it is
              * on board the currently selected ship.
              *
              * @param comp The component to add to this CargoPanel.
              * @param editState Must be set to 'true' if the state of the
              *            component that is added (which should be a dropped
              *            component representing a Unit or good) should be
              *            changed so that the underlying unit or goods are on
              *            board the currently selected ship.
              * @return The component argument.
              */
             public Component add(Component comp, boolean editState) {
                 Canvas canvas = getCanvas();
                 Container oldParent = comp.getParent();
                 if (editState) {
                     if (comp instanceof UnitLabel) {
                         Unit unit = ((UnitLabel) comp).getUnit();
                         Tile tile = colonyTile.getWorkTile();
                         Player player = unit.getOwner();
 
                         if (colonyTile.canAdd(unit)) {
                             if (tile.getOwner() != player
                                 || tile.getOwningSettlement() != getColony()) {
                                 String name = getColony().getName();
                                 if (getController().claimLand(tile, getColony(), 0)) {
                                     logger.info("Colony " + name
                                         + " claims tile " + tile.toString()
                                         + " with unit " + unit.getId());
                                 } else {
                                     logger.warning("Colony " + name
                                         + " could not claim " + tile.toString()
                                         + " with unit " + unit.getId());
                                     return null;
                                 }
                             }
                             oldParent.remove(comp);
 
                             GoodsType workType = unit.getWorkType();
                             getController().work(unit, colonyTile);
                             // check whether worktype is suitable
                            if (workType != unit.getWorkType()) {
                                 getController().changeWorkType(unit, workType);
                             }
 
                             ((UnitLabel) comp).setSmall(false);
 
                             if (getClient().getClientOptions().getBoolean(ClientOptions.SHOW_NOT_BEST_TILE)) {
                                 ColonyTile bestTile = getColony().getVacantColonyTileFor(unit, false, workType);
                                 if (bestTile != null && colonyTile != bestTile
                                     && (colonyTile.getProductionOf(unit, workType)
                                         < bestTile.getProductionOf(unit, workType))) {
                                     StringTemplate template = StringTemplate.template("colonyPanel.notBestTile")
                                         .addStringTemplate("%unit%", Messages.getLabel(unit))
                                         .add("%goods%", workType.getNameKey())
                                         .addStringTemplate("%tile%", bestTile.getLabel());
                                     canvas.showInformationMessage(template);
                                 }
                             }
                         } else {
                             // could not add the unit on the tile
                             Tile workTile = colonyTile.getWorkTile();
                             Settlement s = workTile.getOwningSettlement();
 
                             if (s != null && s != getColony()) {
                                 if (s.getOwner() == player) {
                                     // Its one of ours
                                     canvas.errorMessage("tileTakenSelf");
                                 } else if (s.getOwner().isEuropean()) {
                                     // occupied by a foreign european colony
                                     canvas.errorMessage("tileTakenEuro");
                                 } else if (s instanceof IndianSettlement) {
                                     // occupied by an indian settlement
                                     getController().claimLand(workTile,
                                                               getColony(), 0);
                                     //canvas.errorMessage("tileTakenInd");
                                 }
                             } else {
                                 if (colonyTile.getUnitCount() > 0) {
                                     ; // Tile is already occupied
                                 } else if (!workTile.isLand()) { // no docks
                                     canvas.errorMessage("tileNeedsDocks");
                                 } else if (workTile.hasLostCityRumour()) {
                                     canvas.errorMessage("tileHasRumour");
                                 }
                             }
                             return null;
                         }
                     } else {
                         logger.warning("An invalid component got dropped on this ASingleTilePanel.");
                         return null;
                     }
                 }
 
                 /*
                  * At this point, the panel has already been updated
                  * via the property change listener.
                  *
                  removeAll();
                  Component c = super.add(comp);
                  refresh();
                 */
                 return comp;
             }
 
             public void addPropertyChangeListeners() {
                 colonyTile.addPropertyChangeListener(this);
             }
 
             public void removePropertyChangeListeners() {
                 colonyTile.removePropertyChangeListener(this);
             }
 
             public void propertyChange(PropertyChangeEvent event) {
                 String property = event.getPropertyName();
                 logger.finest(colonyTile.getId() + " change " + property
                               + ": " + event.getOldValue()
                               + " -> " + event.getNewValue());
                 initialize();
             }
         }
     }
 }
