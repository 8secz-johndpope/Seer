 /**
  *  Copyright (C) 2002-2007  The FreeCol Team
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
 
 package net.sf.freecol.server.control;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.EnumMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import net.sf.freecol.FreeCol;
 import net.sf.freecol.common.model.AbstractUnit;
 import net.sf.freecol.common.model.Colony;
 import net.sf.freecol.common.model.CombatModel;
 import net.sf.freecol.common.model.EquipmentType;
 import net.sf.freecol.common.model.FoundingFather;
 import net.sf.freecol.common.model.IndianSettlement;
 import net.sf.freecol.common.model.ModelController;
 import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
 import net.sf.freecol.common.model.GameOptions;
 import net.sf.freecol.common.model.Goods;
 import net.sf.freecol.common.model.GoodsType;
 import net.sf.freecol.common.model.Map;
 import net.sf.freecol.common.model.Map.Direction;
 import net.sf.freecol.common.model.Market;
 import net.sf.freecol.common.model.Modifier;
 import net.sf.freecol.common.model.Monarch;
 import net.sf.freecol.common.model.Monarch.MonarchAction;
 import net.sf.freecol.common.model.Player;
 import net.sf.freecol.common.model.Player.PlayerType;
 import net.sf.freecol.common.model.Player.Stance;
 import net.sf.freecol.common.model.Settlement;
 import net.sf.freecol.common.model.Tile;
 import net.sf.freecol.common.model.Unit;
 import net.sf.freecol.common.model.Unit.UnitState;
 import net.sf.freecol.common.model.UnitType;
 import net.sf.freecol.common.model.Map.Position;
 import net.sf.freecol.common.networking.Message;
 import net.sf.freecol.server.FreeColServer;
 import net.sf.freecol.server.model.ServerPlayer;
 
 import org.w3c.dom.Element;
 
 /**
  * TODO: write class comment.
  */
 public final class InGameController extends Controller {
 
     private static Logger logger = Logger.getLogger(InGameController.class.getName());
 
     protected static EquipmentType muskets = FreeCol.getSpecification().getEquipmentType("model.equipment.muskets");
     protected static EquipmentType horses = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
 
     public int debugOnlyAITurns = 0;
 
     /**
      * The constructor to use.
      * 
      * @param freeColServer The main server object.
      */
     public InGameController(FreeColServer freeColServer) {
         super(freeColServer);
     }
 
     /**
      * Ends the turn of the given player.
      * 
      * @param player The player to end the turn of.
      */
     public void endTurn(ServerPlayer player) {
         /* BEGIN FIX
          * 
          * TODO: Remove this temporary fix for bug:
          *       [ 1709196 ] Waiting for next turn (inifinite wait)
          *       
          *       This fix can be removed when FIFO ordering of
          *       of network messages is working correctly.
          *       (scheduled to be fixed as part of release 0.8.0)
          */
         try {
             Thread.sleep(100);
         } catch (InterruptedException e) {}
         // END FIX
         
         FreeColServer freeColServer = getFreeColServer();
         ServerPlayer oldPlayer = (ServerPlayer) getGame().getCurrentPlayer();
         
         if (oldPlayer != player) {
             logger.warning("It is not " + player.getName() + "'s turn!");
             throw new IllegalArgumentException("It is not " + player.getName() + "'s turn!");
         }
         
         player.clearModelMessages();
         freeColServer.getModelController().clearTaskRegister();
 
         Player winner = checkForWinner();
         if (winner != null && (!freeColServer.isSingleplayer() || !winner.isAI())) {
             Element gameEndedElement = Message.createNewRootElement("gameEnded");
             gameEndedElement.setAttribute("winner", winner.getId());
             freeColServer.getServer().sendToAll(gameEndedElement, null);
             return;
         }
         
         ServerPlayer newPlayer = (ServerPlayer) nextPlayer();
         
         if (newPlayer != null 
             && !newPlayer.isAI()
             && (!newPlayer.isConnected() || debugOnlyAITurns > 0)) {
             endTurn(newPlayer);
             return;
         }
     }
     
     /**
      * Sets a new current player and notifies the clients.
      * @return The new current player.
      */
     private Player nextPlayer() {
         final FreeColServer freeColServer = getFreeColServer();
         
         if (!isHumanPlayersLeft()) {
             getGame().setCurrentPlayer(null);
             return null;
         }
         
         if (getGame().isNextPlayerInNewTurn()) {
             getGame().newTurn();
             if (debugOnlyAITurns > 0) {
                 debugOnlyAITurns--;
             }
             Element newTurnElement = Message.createNewRootElement("newTurn");
             freeColServer.getServer().sendToAll(newTurnElement, null);
         }
         
         ServerPlayer newPlayer = (ServerPlayer) getGame().getNextPlayer();
         getGame().setCurrentPlayer(newPlayer);
         if (newPlayer == null) {
             getGame().setCurrentPlayer(null);
             return null;
         }
         
         synchronized (newPlayer) {
             if (checkForDeath(newPlayer)) {
                 newPlayer.setDead(true);
                 Element setDeadElement = Message.createNewRootElement("setDead");
                 setDeadElement.setAttribute("player", newPlayer.getId());
                 freeColServer.getServer().sendToAll(setDeadElement, null);
                 return nextPlayer();
             }
         }
         
         if (newPlayer.isEuropean()) {
 
             try {        
                 Market market = newPlayer.getMarket();
                 // make random change to the market
                 List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
                 GoodsType typeToRemove = goodsTypes.get(getPseudoRandom().nextInt(goodsTypes.size()));
                 if (typeToRemove.isStorable()) {
                     int amountToRemove = getPseudoRandom().nextInt(21);
                     market.remove(typeToRemove, amountToRemove);
                     Element updateElement = Message.createNewRootElement("marketElement");
                     updateElement.setAttribute("type", typeToRemove.getId());
                     updateElement.setAttribute("amount", String.valueOf(-amountToRemove));
                     newPlayer.getConnection().send(updateElement);
                 }
             } catch (IOException e) {
                 logger.warning("Could not send message to: " + newPlayer.getName() +
                                " with connection " + newPlayer.getConnection());
             }
 
             if (newPlayer.getCurrentFather() == null && newPlayer.getSettlements().size() > 0) {
                 chooseFoundingFather(newPlayer);
             }
             if (newPlayer.getMonarch() != null) {
                 monarchAction(newPlayer);
             }
             bombardEnemyShips(newPlayer);
         }
         else if (newPlayer.isIndian()) {
             
             for (IndianSettlement indianSettlement: newPlayer.getIndianSettlements()) {
                 if (indianSettlement.checkForNewMissionnaryConvert()) {
                     // an Indian brave gets converted by missionary
                     Unit missionary = indianSettlement.getMissionary();
                     ServerPlayer european = (ServerPlayer) missionary.getOwner();
                     // search for a nearby colony
                     Tile settlementTile = indianSettlement.getTile();
                     Tile targetTile = null;
                     Iterator<Position> ffi = getGame().getMap().getFloodFillIterator(settlementTile.getPosition());
                     while (ffi.hasNext()) {
                         Tile t = getGame().getMap().getTile(ffi.next());
                         if (settlementTile.getDistanceTo(t) > IndianSettlement.MAX_CONVERT_DISTANCE) {
                             break;
                         }
                         if (t.getSettlement() != null && t.getSettlement().getOwner() == european) {
                             targetTile = t;
                             break;
                         }
                     }
         
                     if (targetTile != null) {
                         
                         List<UnitType> converts = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.convert");
                         if (converts.size() > 0) {
                             // perform the conversion from brave to convert in the server
                             Unit brave = indianSettlement.getUnitIterator().next();
                             String nationId = brave.getOwner().getNationID();
                             brave.dispose();
                             ModelController modelController = getGame().getModelController();
                             int random = modelController.getRandom(indianSettlement.getId() + "getNewConvertType", converts.size());
                             UnitType unitType = converts.get(random);
                             Unit unit = modelController.createUnit(indianSettlement.getId() + "newTurn100missionary", targetTile,
                                     european, unitType);
                             // and send update information to the client
                             try {
                                 Element updateElement = Message.createNewRootElement("newConvert");
                                 updateElement.setAttribute("nation", nationId);
                                 updateElement.setAttribute("colonyTile", targetTile.getId());
                                 updateElement.appendChild(unit.toXMLElement(updateElement.getOwnerDocument()));
                                 european.getConnection().send(updateElement);
                                 logger.info("New convert created for " + european.getName() + " with ID=" + unit.getId());
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + european.getName());
                             }
                         }
                     }
                 }
             }
         }
         
         Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
         setCurrentPlayerElement.setAttribute("player", newPlayer.getId());
         freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);
         
         return newPlayer;
     }
     
     private boolean isHumanPlayersLeft() {
         for (Player player : getFreeColServer().getGame().getPlayers()) {
             if (!player.isDead() && !player.isAI() && ((ServerPlayer) player).isConnected()) {
                 return true;
             }
         }
         return false;
     }
 
     private void chooseFoundingFather(ServerPlayer player) {
         final ServerPlayer nextPlayer = player;
        Thread t = new Thread(FreeCol.CLIENT_THREAD+"FoundingFather-thread") {
                 public void run() {
                     List<FoundingFather> randomFoundingFathers = getRandomFoundingFathers(nextPlayer);
                     boolean atLeastOneChoice = false;
                     Element chooseFoundingFatherElement = Message.createNewRootElement("chooseFoundingFather");
                     for (FoundingFather father : randomFoundingFathers) {
                         chooseFoundingFatherElement.setAttribute(father.getType().toString(),
                                                                  father.getId());
                         atLeastOneChoice = true;
                     }
                     if (!atLeastOneChoice) {
                         nextPlayer.setCurrentFather(null);
                     } else {
                         try {
                             Element reply = nextPlayer.getConnection().ask(chooseFoundingFatherElement);
                             FoundingFather father = FreeCol.getSpecification().
                                 getFoundingFather(reply.getAttribute("foundingFather"));
                             if (!randomFoundingFathers.contains(father)) {
                                 throw new IllegalArgumentException();
                             }
                             nextPlayer.setCurrentFather(father);
                         } catch (IOException e) {
                             logger.warning("Could not send message to: " + nextPlayer.getName());
                         }
                     }
                 }
             };
         t.start();
     }
 
     /**
      * 
      * Returns a List of FoundingFathers, not including the founding
      * fathers the player already has, one of each type, or null if no
      * FoundingFather of that type is available.
      * 
      * @param player The <code>Player</code> that should pick a founding
      *            father from this list.
      */
     private List<FoundingFather> getRandomFoundingFathers(Player player) {
         int age = getGame().getTurn().getAge();
         List<FoundingFather> randomFoundingFathers = new ArrayList<FoundingFather>();
         EnumMap<FoundingFatherType, Integer> weightSums = new
             EnumMap<FoundingFatherType, Integer>(FoundingFatherType.class);
         for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
             if (!player.hasFather(father) && father.isAvailableTo(player)) {
                 Integer weightSum = weightSums.get(father.getType());
                 if (weightSum == null) {
                     weightSum = new Integer(0);
                 }
                 weightSums.put(father.getType(), weightSum + father.getWeight(age));
             }
         }
         for (java.util.Map.Entry<FoundingFatherType, Integer> entry : weightSums.entrySet()) {
             if (entry.getValue() != 0) {
                 int r = getPseudoRandom().nextInt(entry.getValue()) + 1;
                 int weightSum = 0;
                 for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                     if (!player.hasFather(father) && father.getType() == entry.getKey()) {
                         weightSum += father.getWeight(age);
                         if (weightSum >= r) {
                             randomFoundingFathers.add(father);
                             break;
                         }
                     }
                 }
             }
         }
         return randomFoundingFathers;
     }
 
     /**
      * Checks if anybody has won the game and returns that player.
      * 
      * @return The <code>Player</code> who have won the game or <i>null</i>
      *         if the game is not finished.
      */
     public Player checkForWinner() {
         List<Player> players = getGame().getPlayers();
         GameOptions go = getGame().getGameOptions();
         if (go.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
             for (Player player : players) {
                 if (!player.isAI() && player.getPlayerType() == PlayerType.INDEPENDENT) {
                     return player;
                 }
             }
         }
         if (go.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)) {
             Player winner = null;
             for (Player player : players) {
                 if (!player.isDead() && player.isEuropean() && !player.isREF()) {
                     if (winner != null) {
                         // There is more than one european player alive:
                         winner = null;
                         break;
                     } else {
                         winner = player;
                     }
                 }
             }
             if (winner != null) {
                 return winner;
             }
         }
         if (go.getBoolean(GameOptions.VICTORY_DEFEAT_HUMANS)) {
             Player winner = null;
             for (Player player : players) {
                 if (!player.isDead() && !player.isAI()) {
                     if (winner != null) {
                         // There is more than one human player alive:
                         winner = null;
                         break;
                     } else {
                         winner = player;
                     }
                 }
             }
             if (winner != null) {
                 return winner;
             }
         }
         return null;
     }
 
     /**
      * Checks if the given player has died.
      * 
      * @param player The <code>Player</code>.
      * @return <i>true</i> if this player should die.
      */
     private boolean checkForDeath(Player player) {
         /*
          * Die if: (No colonies or units on map)
          *         && ((After 20 turns) || (Cannot get a unit from Europe))
          */
     	
         if (player.isREF()) {
             /*
              * The REF never dies. I can grant independence to
              * dominions, see: AIPlayer.checkForREFDefeat
              */
             return false;
         }
 
         // Quick check to avoid long processing time:
         if (!player.getSettlements().isEmpty()) {
             return false;
         }
         
         boolean hasCarrier = false;
         List<Unit> unitList = player.getUnits();
         if (!unitList.isEmpty()) {
             for(int i=0; i < unitList.size(); i++){
             	Unit unit = unitList.get(i);
             	
             	// Can found new colony
             	if(unit.isColonist()){
             		logger.info("Unit " + unit.getId() + " can found colony");
             		return false;
             	}
             	
             	// Can capture units/goods
             	if(unit.isOffensiveUnit()){
             		logger.info("Unit " + unit.getId() + " has offense");
             		return false;
             	}
             	
             	// Is carrying units and/or goods
             	if(unit.getGoodsCount() > 0){
             		logger.info("Unit " + unit.getId() + " has goods");
             		return false;
             	}
             	if(unit.getUnitCount()>0){
             		logger.info("Unit " + unit.getId() + " has units");
             		return false;
             	}
             	
             	if(unit.isNaval() && unit.isCarrier())
             		hasCarrier = true;
             }
         }
         
         /*
          * At this point we know the player does not have any valid units or
          * settlements on the map.
          */
         
         /*
          *  No Europe, no reenforcements
          */
         if (!player.isEuropean() || player.getEurope() == null) {
             return true;
         }
         
         // After 20 turns, no presence in New World means endgame
         if (getGame().getTurn().getNumber() > 20) {
             return true;
         }
         
         /*
          * Check if player has colonists and carrier to transport them to New World
          */
         boolean hasColonistsWaiting = false;
         
         Iterator<Unit> unitIterator = player.getEurope().getUnitIterator();
     	while (unitIterator.hasNext()) {
             Unit unit = unitIterator.next();
             if (unit.isCarrier()) {
                 /*
                  * The carrier has units 
                  *or goods that can be sold
                  */
                 if(unit.getGoodsCount() > 0){
                     return false;
                 }
     			
                 hasCarrier = true;
                 continue;
             }
             if (unit.isColonist()){
                 hasColonistsWaiting = true;
                 continue;
             }
     	}
         
         int goldNeeded = 0;
         
         /*
     	 * No carrier, check if has gold to buy one
     	 */
     	if(!hasCarrier){
             /*
              * Find the cheapest naval unit
              */
     		
             Iterator<UnitType> navalUnits = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.navalUnit").iterator();
     		
             int lowerPrice = player.getEurope().getUnitPrice(navalUnits.next());
     		
             while(navalUnits.hasNext()){
                 UnitType unit = navalUnits.next();
                 if(player.getEurope().getUnitPrice(unit) < lowerPrice){
                     lowerPrice = player.getEurope().getUnitPrice(unit);
                 }
             }
     		
             goldNeeded += lowerPrice;
     		
             if(goldNeeded > player.getGold()){
                 return true;
             }
     	}
     	
     	/*
     	 * No colonists, check if has gold to train 
     	 *or recruit one
     	 */
     	
     	if(!hasColonistsWaiting){
             int goldToRecruit =  player.getEurope().getRecruitPrice();
     		
             /*
              * Find the cheapest colonist, either by recruiting or training
              */
     		
             Iterator<UnitType> trainedUnits = FreeCol.getSpecification().getUnitTypesTrainedInEurope().iterator();
     		
             int goldToTrain = Integer.MAX_VALUE;
     		
             while(trainedUnits.hasNext()){
                 UnitType unit = trainedUnits.next();
     			
                 if(!unit.hasAbility("model.ability.foundColony")){
                     continue;
                 }
     			
                 if(player.getEurope().getUnitPrice(unit) < goldToTrain){
                     goldToTrain = player.getEurope().getUnitPrice(unit);
                 }
             }
     		    		
             goldNeeded += Math.min(goldToTrain, goldToRecruit);
     		
             if(goldNeeded > player.getGold()){
                 return true;
             }
     	}
     	
     	/*
     	 * Has carrier and colonists waiting, or
     	 *enough gold to buy them
     	 */
     	return false;
     }
 
     /**
      * Checks for monarch actions.
      * 
      * @param player The server player.
      */
     private void monarchAction(ServerPlayer player) {
         final ServerPlayer nextPlayer = player;
         Thread t = new Thread("monarchAction") {
                 public void run() {
                     try {
                         Monarch monarch = nextPlayer.getMonarch();
                         MonarchAction action = monarch.getAction();
                         Element monarchActionElement = Message.createNewRootElement("monarchAction");
                         monarchActionElement.setAttribute("action", String.valueOf(action));
                         switch (action) {
                         case RAISE_TAX:
                             int oldTax = nextPlayer.getTax();
                             int newTax = monarch.getNewTax(MonarchAction.RAISE_TAX);
                             if (newTax > 100) {
                                 logger.warning("Tax rate exceeds 100 percent.");
                                 return;
                             }
                             Goods goods = nextPlayer.getMostValuableGoods();
                             if (goods == null) {
                                 return;
                             }
                             monarchActionElement.setAttribute("amount", String.valueOf(newTax));
                             monarchActionElement.setAttribute("goods", goods.getName());
                             monarchActionElement.setAttribute("force", String.valueOf(false));
                             try {
                                 nextPlayer.setTax(newTax); // to avoid cheating
                                 Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                                 boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                             
                                 if (!accepted) {
                                     Colony colony = (Colony) goods.getLocation();
                                     if (colony.getGoodsCount(goods.getType()) >= goods.getAmount()) {
                                         nextPlayer.setTax(oldTax); // player hasn't accepted, restoring tax
                                         Element removeGoodsElement = Message.createNewRootElement("removeGoods");
                                         colony.removeGoods(goods);
                                         nextPlayer.setArrears(goods);
                                         colony.getFeatureContainer().addModifier(Modifier
                                             .createTeaPartyModifier(getGame().getTurn()));
                                         removeGoodsElement.appendChild(goods.toXMLElement(nextPlayer, removeGoodsElement
                                                                                           .getOwnerDocument()));
                                         nextPlayer.getConnection().send(removeGoodsElement);
                                     } else {
                                         // player has cheated and removed goods from colony, don't restore tax
                                         monarchActionElement.setAttribute("force", String.valueOf(true));
                                         nextPlayer.getConnection().send(monarchActionElement);
                                     }
                                 }
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                             }
                             break;
                         case LOWER_TAX:
                             int taxLowered = monarch.getNewTax(MonarchAction.LOWER_TAX);
                             if (taxLowered < 0) {
                                 logger.warning("Tax rate less than 0 percent.");
                                 return;
                             }
                             monarchActionElement.setAttribute("amount", String.valueOf(taxLowered));
                             try {
                                 nextPlayer.setTax(taxLowered); // to avoid cheating
                                 nextPlayer.getConnection().send(monarchActionElement); 
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                             }
                             break;
                         case ADD_TO_REF:
                             List<AbstractUnit> unitsToAdd = monarch.addToREF();
                             monarch.addToREF(unitsToAdd);
                             Element additionElement = monarchActionElement.getOwnerDocument().createElement("addition");
                             for (AbstractUnit unit : unitsToAdd) {
                                 additionElement.appendChild(unit.toXMLElement(additionElement.getOwnerDocument()));
                             }
                             monarchActionElement.appendChild(additionElement);
                             try {
                                 nextPlayer.getConnection().send(monarchActionElement);
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                             }
                             break;
                         case DECLARE_WAR:
                             Player enemy = monarch.declareWar();
                             if (enemy == null) {
                                 // this should not happen
                                 logger.warning("Declared war on nobody.");
                                 return;
                             }
                             nextPlayer.changeRelationWithPlayer(enemy, Stance.WAR);
                             monarchActionElement.setAttribute("enemy", enemy.getId());
                             try {
                                 nextPlayer.getConnection().send(monarchActionElement);
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                             }
                             break;
                             /** TODO: restore
                                 case Monarch.SUPPORT_LAND:
                                 int[] additions = monarch.supportLand();
                                 createUnits(additions, monarchActionElement, nextPlayer);
                                 try {
                                 nextPlayer.getConnection().send(monarchActionElement);
                                 } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                                 }
                                 break;
                                 case Monarch.SUPPORT_SEA:
                                 // TODO: make this generic
                                 UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.frigate");
                                 newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer, unitType, UnitState.ACTIVE);
                                 //nextPlayer.getEurope().add(newUnit);
                                 monarchActionElement.appendChild(newUnit.toXMLElement(nextPlayer, monarchActionElement
                                 .getOwnerDocument()));
                                 try {
                                 nextPlayer.getConnection().send(monarchActionElement);
                                 } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                                 }
                                 break;
                             */
                         case OFFER_MERCENARIES:
                             Element mercenaryElement = monarchActionElement.getOwnerDocument().createElement("mercenaries");
                             List<AbstractUnit> units = monarch.getMercenaries();
                             int price = monarch.getPrice(units, true);
                             monarchActionElement.setAttribute("price", String.valueOf(price));
                             for (AbstractUnit unit : units) {
                                 mercenaryElement.appendChild(unit.toXMLElement(monarchActionElement.getOwnerDocument()));
                             }
                             monarchActionElement.appendChild(mercenaryElement);
                             try {
                                 Element reply = nextPlayer.getConnection().ask(monarchActionElement);
                                 boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                                 if (accepted) {
                                     Element updateElement = Message.createNewRootElement("monarchAction");
                                     updateElement.setAttribute("action", String.valueOf(MonarchAction.ADD_UNITS));
                                     nextPlayer.modifyGold(-price);
                                     createUnits(units, updateElement, nextPlayer);
                                     nextPlayer.getConnection().send(updateElement);
                                 }
                             } catch (IOException e) {
                                 logger.warning("Could not send message to: " + nextPlayer.getName());
                             }
                             break;
                         }
                     } catch (Exception e) {
                         logger.log(Level.WARNING, "Monarch action failed!", e);
                     }
                 }
             };
         t.start();
     }
 
     private void createUnits(List<AbstractUnit> units, Element element, ServerPlayer nextPlayer) {
         EquipmentType[] soldier = new EquipmentType[] { muskets };
         EquipmentType[] dragoon = new EquipmentType[] { horses, muskets };
         for (AbstractUnit unit : units) {
             EquipmentType[] equipment = EquipmentType.NO_EQUIPMENT;
             for (int count = 0; count < unit.getNumber(); count++) {
                 switch(unit.getRole()) {
                 case SOLDIER:
                     equipment = soldier;
                     break;
                 case DRAGOON:
                     equipment = dragoon;
                     break;
                 default:
                 }
                 Unit newUnit = new Unit(getGame(), nextPlayer.getEurope(), nextPlayer,
                                         unit.getUnitType(), UnitState.ACTIVE, equipment);
                 //nextPlayer.getEurope().add(newUnit);
                 if (element != null) {
                     element.appendChild(newUnit.toXMLElement(nextPlayer, element.getOwnerDocument()));
                 }
             }
         }
     }
     
     public boolean createMission(IndianSettlement settlement,Unit missionary){
     	settlement.setMissionary(missionary);
     	
     	//TODO: make possibility of indians refusing the mission 
     	return true;
     }
 
     private void bombardEnemyShips(ServerPlayer currentPlayer) {
         logger.finest("Entering method bombardEnemyShips.");
         Map map = getFreeColServer().getGame().getMap();
         CombatModel combatModel = getFreeColServer().getGame().getCombatModel();
         for (Settlement settlement : currentPlayer.getSettlements()) {
             Colony colony = (Colony) settlement;
             logger.finest("Colony is " + colony.getName());
             if (colony.hasAbility("model.ability.bombardShips") && !colony.isLandLocked()) {
                 logger.finest("Colony has harbour and fort.");
                 Position colonyPosition = colony.getTile().getPosition();
                 for (Direction direction : Direction.values()) {
                     Tile tile = map.getTile(Map.getAdjacent(colonyPosition, direction));
                     if (!tile.isLand()) {
                         Iterator<Unit> unitIterator = tile.getUnitIterator();
                         while (unitIterator.hasNext()) {
                             Unit unit = unitIterator.next();
                             float attackPower = combatModel.getOffencePower(colony, unit);
                             Player player = unit.getOwner();
                             if (player != currentPlayer
                                 && (currentPlayer.getStance(player) == Stance.WAR ||
                                     unit.hasAbility("model.ability.piracy"))) {
                                 logger.finest("Found enemy unit " + unit.getOwner().getNationAsString() + " "
                                               + unit.getName());
                                 // generate bombardment result
                                 CombatModel.CombatResult result = combatModel.generateAttackResult(colony, unit);
 
                                 // Inform the players (other then the player
                                 // attacking) about the attack:
                                 int plunderGold = -1;
                                 Iterator<Player> enemyPlayerIterator = getFreeColServer().getGame().getPlayerIterator();
                                 while (enemyPlayerIterator.hasNext()) {
                                     ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
 
                                     if (// currentPlayer.equals(enemyPlayer) ||
                                         enemyPlayer.getConnection() == null) {
                                         continue;
                                     }
 
                                     Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
                                     if (unit.isVisibleTo(enemyPlayer)) {
                                         opponentAttackElement.setAttribute("direction", direction.toString());
                                         opponentAttackElement.setAttribute("result", result.toString());
                                         opponentAttackElement .setAttribute("plunderGold",
                                                                             Integer.toString(plunderGold));
                                         opponentAttackElement.setAttribute("colony", colony.getId());
                                         opponentAttackElement.setAttribute("defender", unit.getId());
 
                                         if (!enemyPlayer.canSee(colony.getTile())) {
                                             opponentAttackElement.setAttribute("update", "tile");
                                             enemyPlayer.setExplored(colony.getTile());
                                             opponentAttackElement.appendChild(colony.getTile().toXMLElement(
                                                                                                             enemyPlayer, opponentAttackElement.getOwnerDocument()));
                                         }
 
                                         try {
                                             enemyPlayer.getConnection().send(opponentAttackElement);
                                         } catch (IOException e) {
                                             logger.warning("Could not send message to: " + enemyPlayer.getName()
                                                            + " with connection " + enemyPlayer.getConnection());
                                         }
                                     }
                                 }
 
                                 // Create the reply for the attacking player:
                                 /*
                                  * Element bombardElement =
                                  * Message.createNewRootElement("bombardResult");
                                  * bombardElement.setAttribute("result",
                                  * Integer.toString(result));
                                  * bombardElement.setAttribute("colony",
                                  * colony.getId());
                                  * 
                                  * if (!unit.isVisibleTo(player)) {
                                  * bombardElement.appendChild(unit.toXMLElement(player,
                                  * bombardElement.getOwnerDocument())); }
                                  * colony.bombard(unit, result); try {
                                  * currentPlayer.getConnection().send(bombardElement); }
                                  * catch (IOException e) { logger.warning("Could
                                  * not send message to: " +
                                  * currentPlayer.getName() + " with connection " +
                                  * currentPlayer.getConnection()); }
                                  */
                             }
                         }
                     }
                 }
             }
         }
     }
 }
