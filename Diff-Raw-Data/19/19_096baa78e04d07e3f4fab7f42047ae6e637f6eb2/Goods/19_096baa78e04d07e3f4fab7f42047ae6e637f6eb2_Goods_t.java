 
 package net.sf.freecol.common.model;
 
 import java.util.logging.Logger;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 

 /**
 * Represents a locatable goods of a specified type and amount.
 */
 public final class Goods implements Locatable {
     public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
     public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
     public static final String  REVISION = "$Revision$";
 
     private static Logger logger = Logger.getLogger(Goods.class.getName());
 
     public static final int FOOD = 0,
                             SUGAR = 1,
                             TOBACCO = 2,
                             COTTON = 3,
                             FURS = 4,
                             LUMBER = 5,
                             ORE = 6,
                             SILVER = 7,
                             HORSES = 8,
                             RUM = 9,
                             CIGARS = 10,
                             CLOTH = 11,
                             COATS = 12,
                             TRADE_GOODS = 13,
                             TOOLS = 14,
                             MUSKETS = 15;
 
     public static final int NUMBER_OF_TYPES = 16;
 
     // Unstorable goods:
     public static final int FISH = 16, // Stored as food.
                             BELLS = 17,
                             CROSSES = 18,
                             HAMMERS = 19;
 
     private Game game;
     private Location location;
     private int type;
     private int amount;
 
 
 
     /**
     * Creates a new <code>Goods</code>.
     *
     * @param game The <code>Game</code> in which this object belongs
     * @param location The location of the goods,
     * @param type The type of the goods.
     * @param amount The amount of the goods.
     */
     public Goods(Game game, Location location, int type, int amount) {
         if (game == null) {
             throw new NullPointerException();
         }
 
         if (location == null) {
             throw new NullPointerException();
         }
 
         this.game = game;
         this.location = location;
         this.type = type;
         this.amount = amount;
     }
 
     
     public Goods(Game game, Element element) {
         this.game = game;
         readFromXMLElement(element);
     }
 
     /**
      * Returns a textual representation of the Good of type <code>type<code>.
      * @param type  The type of good to return
      * @return
      *
      * TODO - needs to be completed
      */
     public static String getName(int type) {
         switch(type) {
             case FOOD: return "Food";
             case SUGAR: return "Sugar";
             case TOBACCO: return "Tobacco";
             case COTTON: return "Cotton";
             case FURS: return "Furs";
             case LUMBER: return "Lumber";
             case ORE:return "Ore";
             case SILVER: return "Silver";
             case HORSES: return "Horses";
             case RUM: return "Rum";
             case CIGARS: return "Cigars";
             case CLOTH: return "Cloth";
             case COATS: return "Coats";
             case TRADE_GOODS: return "Trade Goods";
             case TOOLS: return "Tools";
             case MUSKETS: return "Muskets";
             case FISH: return "Fish";// = 16, // Stored as food.
             case BELLS: return "Bells";
             case CROSSES: return "Crosses";
             case HAMMERS: return "Hammers";
             default: return "Unknown";
         }
     }
 
 
     /**
     * Sets the location of the goods.
     * @param location The new location of the goods,
     */
     public void setLocation(Location location) {
         if ((this.location != null)) {
             this.location.remove(this);
         }
 
         if (location != null) {
             location.add(this);
         }
 
         this.location = location;
     }
 
 
     /**
     * Gets the location of this goods.
     * @return The location.
     */
     public Location getLocation() {
         return location;
     }
 
 
     /**
     * Gets the amount of space this <code>Goods</code> take.
     * @return The amount.
     */
     public int getTakeSpace() {
         return 1;
     }
 
     /**
     * Gets the value <code>amount</code>.
     * @return The current value of amount.
     */
     public int getAmount() {
         return amount;
     }
 
     /**
     * Sets the value <code>amount</code>.
     * @param a The new value for amount.
     */
     public void setAmount(int a) {
         amount = a;
     }
 
     /**
     * Gets the value <code>type</code>. Note that type of goods should NEVER change.
     * @return The current value of type.
     */
     public int getType() {
         return type;
     }
     
 
     /**
     * If the amount of goods is greater than the source can supply,
     * then this method adjusts the amount to the maximum amount possible.
     */
     public void adjustAmount() {
         int maxAmount = location.getGoodsContainer().getGoodsCount(getType());
         setAmount((getAmount() > maxAmount) ? maxAmount : getAmount());
     }
 
 
     /**
     * Prepares the <code>Goods</code> for a new turn.
     */
     public void newTurn() {
     
     }
 
     /**
     * Loads the cargo onto a carrier that is on the same tile.
     *
     * @param carrier The carrier this unit shall embark.
     * @exception IllegalStateException If the carrier is on another tile than this unit.
     */
     public void loadOnto(Unit carrier) {
         if (getLocation() == null) {
             throw new IllegalStateException("The goods need to be taken from a place, but 'location == null'.");
         } else if ((getLocation().getTile() == carrier.getTile())) {
            if (carrier.getLocation() instanceof Europe && (carrier.getState() == Unit.TO_EUROPE || carrier.getState() == Unit.TO_AMERICA)) {
                throw new IllegalStateException("Unloading cargo from a ship that is not in port in Europe.");
            }

             setLocation(carrier);
         } else {
             throw new IllegalStateException("It is not allowed to load cargo onto a ship on another tile.");
         }
     }
 
 
     /**
     * Unload the goods from the ship. This method should only be invoked if the ship is in a harbour.
     *
     * @exception IllegalStateException If not in harbour.
     * @exception ClassCastException If not located on a ship.
     */
     public void unload() {
         Location l = ((Unit) getLocation()).getLocation();
 
         logger.info("Unloading cargo from a ship.");
         if (l instanceof Europe) {
            if ((((Unit) getLocation()).getState() == Unit.TO_EUROPE || ((Unit) getLocation()).getState() == Unit.TO_AMERICA)) {
                throw new IllegalStateException("Unloading cargo from a ship that is not in port in Europe.");
            }

             setLocation(l);
         } else if (l.getTile().getSettlement() != null && l.getTile().getSettlement() instanceof Colony) {
             setLocation(l.getTile().getSettlement());
         } else {
             throw new IllegalStateException("Goods may only leave a ship while in a harbour.");
         }
     }    
     
     
     /**
     * Gets the game object this <code>Goods</code> belongs to.
     * @return The <code>Game</code>.
     */
     public Game getGame() {
         return game;
     }
     
 
     /**
     * Removes all references to this object.
     */
     /*public void dispose() {
         if (location != null) {
             location.remove(this);
         }
 
         super.dispose();
     }*/
 
 
     /**
     * Makes an XML-representation of this object.
     *
     * @param document The document to use when creating new componenets.
     * @return The DOM-element ("Document Object Model") made to represent this "Goods".
     */
     public Element toXMLElement(Player player, Document document) {
         Element goodsElement = document.createElement(getXMLElementTagName());
 
         goodsElement.setAttribute("type", Integer.toString(type));
         goodsElement.setAttribute("amount", Integer.toString(amount));
 
         if (location != null) {
             goodsElement.setAttribute("location", location.getID());
         } else {
             logger.warning("Creating an XML-element for a 'Goods' without a 'Location'.");
         }
 
         return goodsElement;
     }
 
 
     /**
     * Initializes this object from an XML-representation of this object.
     * @param goodsElement The DOM-element ("Document Object Model") made to represent this "Goods".
     */
     public void readFromXMLElement(Element goodsElement) {
         type = Integer.parseInt(goodsElement.getAttribute("type"));
         amount = Integer.parseInt(goodsElement.getAttribute("amount"));
 
         if (goodsElement.hasAttribute("location")) {
             location = (Location) getGame().getFreeColGameObject(goodsElement.getAttribute("location"));
         }
     }
 
 
     /**
     * Gets the tag name of the root element representing this object.
     * @return "goods".
     */
     public static String getXMLElementTagName() {
         return "goods";
     }
 
 }
