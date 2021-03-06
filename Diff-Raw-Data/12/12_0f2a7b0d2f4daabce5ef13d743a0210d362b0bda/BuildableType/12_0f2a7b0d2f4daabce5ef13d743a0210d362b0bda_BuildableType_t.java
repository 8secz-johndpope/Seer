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
 
 package net.sf.freecol.common.model;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 
 import net.sf.freecol.client.gui.i18n.Messages;
 import net.sf.freecol.common.Specification;
 import net.sf.freecol.common.util.Utils;
 
 /**
  * Contains information on buildable types.
  */
 public class BuildableType extends FreeColGameObjectType {
 
     public static final int UNDEFINED = Integer.MIN_VALUE;
 
     public static final BuildableType NOTHING = new BuildableType("model.buildableType.nothing");
     
     /**
      * The minimum population that a Colony needs in order to build
      * this type.
      */
     private int populationRequired = 1;
 
     /**
      * A list of AbstractGoods required to build this type.
      */
    private List<AbstractGoods> goodsRequired = null;
     
     /**
      * Stores the abilities required by this Type.
      */
     private final HashMap<String, Boolean> requiredAbilities = new HashMap<String, Boolean>();
     
     public BuildableType() {
         // empty constructor, class is abstract except for BuildableType.NOTHING
     }
 
     private BuildableType(String id) {
         setId(id);
     }
 
     public String getGoodsRequiredAsString() {
         ArrayList<String> result = new ArrayList<String>();
         if (goodsRequired != null) {
             for (AbstractGoods goods : goodsRequired) {
                 result.add(Messages.message("model.goods.goodsAmount",
                                             "%amount%", String.valueOf(goods.getAmount()),
                                             "%goods%", goods.getType().getName()));
             }
         }
        String resultAsStr = Utils.join(", ", result);
        
        // has  no goods required
        if(resultAsStr == null){
        	resultAsStr = "";
        }
        
        return resultAsStr;
     }
 
     /**
      * Get the <code>GoodsRequired</code> value.
      *
      * @return a <code>List<AbstractGoods></code> value
      */
     public final List<AbstractGoods> getGoodsRequired() {
         return goodsRequired;
     }
 
     /**
      * Set the <code>GoodsRequired</code> value.
      *
      * @param newGoodsRequired The new GoodsRequired value.
      */
     public final void setGoodsRequired(final List<AbstractGoods> newGoodsRequired) {
         this.goodsRequired = newGoodsRequired;
     }
 
     /**
      * Get the <code>PopulationRequired</code> value.
      *
      * @return an <code>int</code> value
      */
     public int getPopulationRequired() {
         return populationRequired;
     }
 
     /**
      * Set the <code>PopulationRequired</code> value.
      *
      * @param newPopulationRequired The new PopulationRequired value.
      */
     public void setPopulationRequired(final int newPopulationRequired) {
         this.populationRequired = newPopulationRequired;
     }
 
     /**
      * Returns the abilities required by this Type.
      *
      * @return the abilities required by this Type.
      */
     public Map<String, Boolean> getAbilitiesRequired() {
         return requiredAbilities;
     }
 
     protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
         // the class is basically abstract, except for BuildableType.NOTHING
     }
 
     protected void readFromXML(XMLStreamReader in, Specification specification) throws XMLStreamException {
         // the class is basically abstract, except for BuildableType.NOTHING
     }
 
 }
