 /*
  * (c) Copyright 2010-2011 AgileBirds
  *
  * This file is part of OpenFlexo.
  *
  * OpenFlexo is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenFlexo is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OpenFlexo. If not, see <http://www.gnu.org/licenses/>.
  *
  */
 package org.openflexo.foundation.viewpoint.inspector;
 
 import org.openflexo.foundation.ontology.OntologyClass;
 
 /**
  * Represents an inspector entry for an ontology class
  * 
  * @author sylvain
  * 
  */
 public class ClassInspectorEntry extends InspectorEntry {
 
 	private String conceptURI;
 
 	@Override
 	public Class getDefaultDataClass() {
 		return OntologyClass.class;
 	}
 
 	@Override
 	public String getWidgetName() {
 		return "OntologyClassSelector";
 	}
 
 	public String _getConceptURI() {
 		return conceptURI;
 	}
 
 	public void _setConceptURI(String conceptURI) {
 		this.conceptURI = conceptURI;
 	}
 
 	public OntologyClass getConcept() {
		getCalc().loadWhenUnloaded();
 		return getOntologyLibrary().getClass(_getConceptURI());
 	}
 
 	public void setConcept(OntologyClass c) {
 		_setConceptURI(c != null ? c.getURI() : null);
 	}
 
 }
