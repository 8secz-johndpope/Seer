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
 
 package net.sf.freecol.common.model;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.logging.Logger;
 
 import javax.xml.stream.XMLStreamConstants;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import javax.xml.stream.XMLStreamWriter;
 
 import org.w3c.dom.Element;
 
 /**
  * The <code>UnitLocation</code> is a place where a <code>Unit</code>
  * can be put. The UnitLocation can not store any other Locatables,
  * such as {@link Goods}, or {@link TileItem}s.
  *
  * @see Locatable
  */
 public abstract class UnitLocation extends FreeColGameObject implements Location {
 
     private static final Logger logger = Logger.getLogger(UnitLocation.class.getName());
 
     public static enum NoAddReason {
         /**
          * No reason why Locatable can not be added.
          */
         NONE,
         /**
          * Locatable can not be added because it has the wrong
          * type. E.g. a {@link Building} can not be added to a
          * {@link Unit}.
          */
         WRONG_TYPE,
         /**
          * Locatable can not be added because the Location is already
          * full.
          */
         CAPACITY_EXCEEDED,
         /**
          * Locatable can not be added because the Location is
          * occupied by objects belonging to another player.
          */
         OCCUPIED_BY_ENEMY,
         /**
          * Locatable can not be added because the Location belongs
          * to another player and does not admit foreign objects.
          */
         OWNED_BY_ENEMY
     }
 
     /**
      * The Units present in this Location.
      */
     private final List<Unit> units = new ArrayList<Unit>();
 
 
     protected UnitLocation() {
         // empty constructor
     }
 
     /**
      * Creates a new <code>UnitLocation</code> instance.
      *
      * @param game a <code>Game</code> value
      */
     public UnitLocation(Game game) {
         super(game);
     }
 
     /**
      * Creates a new <code>UnitLocation</code> instance.
      *
      * @param game a <code>Game</code> value
      * @param in a <code>XMLStreamReader</code> value
      * @exception XMLStreamException if an error occurs
      */
     public UnitLocation(Game game, XMLStreamReader in) throws XMLStreamException {
         super(game, in);
     }
 
     /**
      * Creates a new <code>UnitLocation</code> instance.
      *
      * @param game a <code>Game</code> value
      * @param e an <code>Element</code> value
      */
     public UnitLocation(Game game, Element e) {
         super(game, e);
         readFromXMLElement(e);
     }
 
     /**
      * Creates a new <code>UnitLocation</code> instance.
      *
      * @param game a <code>Game</code> value
      * @param id a <code>String</code> value
      */
     public UnitLocation(Game game, String id) {
         super(game, id);
     }
 
     /**
      * Returns the maximum number of <code>Units</code> this Location
      * can hold.
      *
      * @return Integer.MAX_VALUE
      */
     public int getUnitCapacity() {
         return Integer.MAX_VALUE;
     }
 
     /**
      * Returns the name of this location.
      *
      * @return The name of this location.
      */
     public StringTemplate getLocationName() {
         return StringTemplate.key(getId());
     }
 
     /**
      * Returns the name of this location for a particular player.
      *
      * @param player The <code>Player</code> to return the name for.
      * @return The name of this location.
      */
     public StringTemplate getLocationNameFor(Player player) {
         return getLocationName();
     }
 
     /**
      * Adds a <code>Locatable</code> to this Location.
      *
      * @param locatable
      *            The <code>Locatable</code> to add to this Location.
      */
     public void add(Locatable locatable) {
         if (locatable instanceof Unit) {
             Unit unit = (Unit) locatable;
             if (contains(unit)) {
                 return;
             } else if (canAdd(unit)) {
                 units.add(unit);
             }
         } else if (locatable instanceof Goods) {
             locatable.setLocation(null);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public boolean canAdd(Locatable locatable) {
         return getNoAddReason(locatable) == NoAddReason.NONE;
     }
 
     /**
      * Removes a <code>Locatable</code> from this Location.
      *
      * @param locatable
      *            The <code>Locatable</code> to remove from this Location.
      */
     public void remove(Locatable locatable) {
         if (locatable instanceof Unit) {
             units.remove((Unit) locatable);
         }
     }
 
     /**
      * Checks if this <code>Location</code> contains the specified
      * <code>Locatable</code>.
      *
      * @param locatable
      *            The <code>Locatable</code> to test the presence of.
      * @return
      *            <ul>
      *            <li><i>true</i> if the specified <code>Locatable</code> is
      *            on this <code>Location</code> and
      *            <li><i>false</i> otherwise.
      *            </ul>
      */
     public boolean contains(Locatable locatable) {
         return units != null && units.contains(locatable);
     }
 
     /**
      * Returns the reason why a given <code>Locatable</code> can not
      * be added to this Location.
      *
      * @param locatable a <code>Locatable</code> value
      * @return a <code>NoAddReason</code> value
      */
     public NoAddReason getNoAddReason(Locatable locatable) {
         if (locatable instanceof Unit) {
             if (units == null || units.size() >= getUnitCapacity()) {
                 return NoAddReason.CAPACITY_EXCEEDED;
             } else {
                 Unit unit = (Unit) locatable;
                 if (!units.isEmpty()
                     && units.get(0).getOwner() != unit.getOwner()) {
                     return NoAddReason.OCCUPIED_BY_ENEMY;
                 } else {
                     return NoAddReason.NONE;
                 }
             }
         } else {
             return NoAddReason.WRONG_TYPE;
         }
     }
 
 
     /**
      * Returns <code>true</code> if this Location admits the given
      * <code>Ownable</code>. By default, this is the case if the
      * Location and the Ownable have the same owner, or if at least
      * one of the owners is <code>null</code>.
      *
      * @param ownable an <code>Ownable</code> value
      * @return a <code>boolean</code> value
      */
     /*
     public boolean admitsOwnable(Ownable ownable) {
         return (owner == null
                 || ownable.getOwner() == null
                 || owner == ownable.getOwner());
     }
     */
 
     /**
      * Returns the amount of Units at this Location.
      *
      * @return The amount of Units at this Location.
      */
     public int getUnitCount() {
         return units.size();
     }
 
     /**
      * Returns a list containing all the Units present at this Location.
      *
      * @return a list containing the Units present at this location.
      */
     public List<Unit> getUnitList() {
         return units;
     }
 
     /**
      * Gets a <code>Iterator</code> of every <code>Unit</code> directly
      * located on this <code>Location</code>.
      *
      * @return The <code>Iterator</code>.
      */
     public Iterator<Unit> getUnitIterator() {
         return getUnitList().iterator();
     }
 
     /**
      * Returns the <code>Tile</code> where this <code>Location</code>
      * is located, or <code>null</code> if it is not located on a Tile.
      *
      * @return a <code>Tile</code> value
      */
     public Tile getTile() {
         return null;
     }
 
     /**
      * Returns the <code>Colony</code> this <code>Location</code> is
      * located in, or <code>null</code> if it is not located in a colony.
      *
      * @return A <code>Colony</code>
      */
     public Colony getColony() {
         return null;
     }
 
     /**
      * Returns the <code>Settlement</code> this <code>Location</code>
      * is located in, or <code>null</code> if it is not located in any
      * settlement.
      *
      * @return a <code>Settlement</code> value
      */
     public Settlement getSettlement() {
         return null;
     }
 
     /**
      * Gets the <code>GoodsContainer</code> this <code>Location</code>
      * use for storing it's goods, or <code>null</code> if the
      * <code>Location</code> cannot store any goods.
      *
      * @return A <code>GoodsContainer</code> value
      */
     public GoodsContainer getGoodsContainer() {
         return null;
     }
 
     /**
      * {@inheritDoc}
      */
     protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
         writeChildren(out, null, true, true);
     }
 
     /**
      * {@inheritDoc}
      */
     protected void writeChildren(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
         throws XMLStreamException {
         if (!getUnitList().isEmpty()) {
             for (Unit unit : getUnitList()) {
                 unit.toXML(out, player, showAll, toSavedGame);
             }
          }
     }
 
     /**
      * {@inheritDoc}
      */
     protected void readChildren(XMLStreamReader in) throws XMLStreamException {
         units.clear();
         while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
             if (Unit.getXMLElementTagName().equals(in.getLocalName())) {
                 Unit unit = updateFreeColGameObject(in, Unit.class);
                 if (!units.contains(unit)) {
                     units.add(unit);
                 }
             } else {
                 logger.warning("Found unknown child element '" + in.getLocalName() + "'.");
             }
         }
     }
 
}
