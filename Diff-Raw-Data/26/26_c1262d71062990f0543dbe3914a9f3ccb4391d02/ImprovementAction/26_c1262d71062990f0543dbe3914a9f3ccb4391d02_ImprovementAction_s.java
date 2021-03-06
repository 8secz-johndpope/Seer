 package net.sf.freecol.client.gui.action;
 
 import java.awt.event.ActionEvent;
 import java.util.List;
 import java.util.logging.Logger;
 
 import javax.swing.KeyStroke;
 
 import net.sf.freecol.client.FreeColClient;
 import net.sf.freecol.client.gui.GUI;
 import net.sf.freecol.client.gui.ImageLibrary;
 import net.sf.freecol.client.gui.i18n.Messages;
 import net.sf.freecol.common.model.Tile;
 import net.sf.freecol.common.model.TileImprovementType;
 import net.sf.freecol.common.model.Unit;
 
 /**
  * An action for using the active unit to plow/clear a forest.
  */
 public class ImprovementAction extends MapboardAction {
     @SuppressWarnings("unused")
     private static final Logger logger = Logger.getLogger(ImprovementAction.class.getName());
 
     public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
 
     public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
 
     public static final String REVISION = "$Revision: 2442 $";
 
     public ImprovementActionType iaType;
     
     int actionID;
     /**
      * Creates this action.
      * 
      * @param freeColClient The main controller object for the client.
      */
     public ImprovementAction(FreeColClient freeColClient, ImprovementActionType iaType) {
         super(freeColClient, iaType.names.get(0), null, KeyStroke.getKeyStroke(iaType.accelerator, 0));
         this.iaType = iaType;
        actionID = 0;
        updateValues(actionID);
     }
 
     /**
      * Updates this action to one of the possible actions for this ImprovementAction.
      * 
      * @param p <code>true</code> if this action should be "clear forest".
      */
     private void updateValues(int newActionID) {
         if (actionID == newActionID) {
             return;
         }
         actionID = newActionID;
 
         putValue(BUTTON_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                  iaType.imageIDs.get(actionID), 0));
         putValue(BUTTON_ROLLOVER_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                  iaType.imageIDs.get(actionID), 1));
         putValue(BUTTON_PRESSED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                  iaType.imageIDs.get(actionID), 2));
         putValue(BUTTON_DISABLED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                  iaType.imageIDs.get(actionID), 3));
         putValue(NAME, Messages.message(iaType.names.get(actionID)));
 
     }
 
     /**
      * Updates the "enabled"-status with the value returned by
      * {@link #shouldBeEnabled} and updates the name of the action.
      */
     public void update() {
         super.update();
 
         GUI gui = getFreeColClient().getGUI();
         if (gui != null) {
             Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
             if (enabled && selectedOne != null && selectedOne.getTile() != null) {
                 Tile tile = selectedOne.getTile();
                 int newActionID = 0;
                 for (TileImprovementType impType : iaType.impTypes) {
                     if (!impType.isTileTypeAllowed(tile.getType())) {
                         continue;
                     }
                     if (!impType.isWorkerAllowed(selectedOne)) {
                         continue;
                     }
                     if (tile.findTileImprovementType(impType) != null) {
                         continue;
                     }
                     newActionID = iaType.impTypes.indexOf(impType);
                     break;
                 }
                 updateValues(newActionID);
             } else {
                 updateValues(0);
             }
         }
     }
 
     /**
      * Checks if this action should be enabled.
      * 
      * @return <code>false</code> if there is no active unit or if the unit
      *         cannot plow/clear forest.
      */
     protected boolean shouldBeEnabled() {
         if (!super.shouldBeEnabled()) {
             return false;
         }
 
         GUI gui = getFreeColClient().getGUI();
         if (gui == null)
             return false;
 
         Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
         if (selectedOne == null || !selectedOne.checkSetState(Unit.IMPROVING))
             return false;
 
         Tile tile = selectedOne.getTile();
         if (tile == null)
             return false;
         
         // Check if there is an ImprovementType that can be performed by this unit on this tile
         for (TileImprovementType impType : iaType.impTypes) {
             if (!impType.isTileTypeAllowed(tile.getType())) {
                 continue;
             }
             if (!impType.isWorkerAllowed(selectedOne)) {
                 continue;
             }
             if (tile.findTileImprovementType(impType) != null) {
                 // Already has this improvement
                 continue;
             }
             return true;
         }
         // Since nothing suitable was found, disable this ImprovementAction.
         return false;
     }
 
     /**
      * Returns the id of this <code>Option</code>.
      * 
      * @return "ImprovementAction"
      */
     public String getId() {
         return iaType.ID;
     }
 
     /**
      * Applies this action.
      * 
      * @param e The <code>ActionEvent</code>.
      */
     public void actionPerformed(ActionEvent e) {
         getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.IMPROVING);
     }
 }
