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
 
 package net.sf.freecol.client.gui.action;
 
 import java.util.ArrayList;
 import java.util.List;
 import javax.xml.stream.XMLStreamConstants;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import javax.xml.stream.XMLStreamWriter;
 
 import net.sf.freecol.common.model.Specification;
 import net.sf.freecol.common.model.FreeColGameObjectType;
 import net.sf.freecol.common.model.FreeColObject;
 import net.sf.freecol.common.model.TileImprovementType;
 
 /**
  * A storage class for ImprovementActionType used to create ImprovementActions.
  * Filled by Specification.java, utilized by ActionManager.java
  */
 public final class ImprovementActionType extends FreeColGameObjectType {
 
     private char accelerator;
     
     private final List<String> names = new ArrayList<String>();
     private final List<TileImprovementType> impTypes = new ArrayList<TileImprovementType>();
     private final List<String> imageIDs = new ArrayList<String>();
     
     // ------------------------------------------------------------ constructors
     
     public ImprovementActionType(String id, Specification specification) {
         super(id, specification);
     }
 
     // ------------------------------------------------------------ retrieval methods
 
     public char getAccelerator() {
         return accelerator;
     }
 
     public List<String> getNames() {
         return names;
     }
 
     public List<TileImprovementType> getImpTypes() {
         return impTypes;
     }
     
     public List<String> getImageIDs() {
         return imageIDs;
     }
 
     // ------------------------------------------------------------ API methods
 
     public void readFromXML(XMLStreamReader in) throws XMLStreamException {
         setId(in.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE_TAG));
         accelerator = in.getAttributeValue(null, "accelerator").charAt(0);
 
         while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
             names.add(in.getAttributeValue(null, "name"));
             String t = in.getAttributeValue(null, "tileimprovement-type");
             impTypes.add(getSpecification().getTileImprovementType(t));
             imageIDs.add(in.getAttributeValue(null, "image-id"));
             in.nextTag(); // close this element
         }
     }
 
     public void toXML(XMLStreamWriter out) throws XMLStreamException {
         out.writeStartElement("improvementaction-type");
         out.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, getId());
         out.writeAttribute("accelerator", Character.toString(accelerator));
 
         for (int index = 0; index < names.size(); index++) {
             out.writeStartElement("action");
             out.writeAttribute("name", names.get(index));
             out.writeAttribute("tileimprovement-type", impTypes.get(index).getId());
             out.writeAttribute("image-id", imageIDs.get(index));
             out.writeEndElement();
         }
 
         out.writeEndElement();
     }
 
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out);
    }

 }
