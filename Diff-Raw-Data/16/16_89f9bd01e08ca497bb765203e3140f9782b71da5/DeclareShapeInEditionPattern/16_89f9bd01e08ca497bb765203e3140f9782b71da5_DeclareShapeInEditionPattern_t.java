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
 package org.openflexo.foundation.viewpoint.action;
 
 import java.util.Hashtable;
 import java.util.Vector;
 import java.util.logging.Logger;
 
 import org.openflexo.fge.ShapeGraphicalRepresentation;
 import org.openflexo.foundation.FlexoEditor;
 import org.openflexo.foundation.FlexoModelObject;
 import org.openflexo.foundation.action.FlexoActionType;
 import org.openflexo.foundation.ontology.FlexoOntology;
 import org.openflexo.foundation.ontology.OntologicDataType;
 import org.openflexo.foundation.ontology.OntologyClass;
 import org.openflexo.foundation.ontology.OntologyDataProperty;
 import org.openflexo.foundation.ontology.OntologyObject;
 import org.openflexo.foundation.ontology.OntologyObjectProperty;
 import org.openflexo.foundation.ontology.OntologyProperty;
 import org.openflexo.foundation.viewpoint.AddIndividual;
 import org.openflexo.foundation.viewpoint.AddShape;
 import org.openflexo.foundation.viewpoint.CheckboxParameter;
 import org.openflexo.foundation.viewpoint.DataPropertyAssertion;
 import org.openflexo.foundation.viewpoint.DeclarePatternRole;
 import org.openflexo.foundation.viewpoint.DropScheme;
 import org.openflexo.foundation.viewpoint.EditionPattern;
 import org.openflexo.foundation.viewpoint.EditionScheme;
 import org.openflexo.foundation.viewpoint.EditionSchemeParameter;
 import org.openflexo.foundation.viewpoint.ExampleDrawingObject;
 import org.openflexo.foundation.viewpoint.ExampleDrawingShape;
 import org.openflexo.foundation.viewpoint.FloatParameter;
 import org.openflexo.foundation.viewpoint.GraphicalElementPatternRole;
 import org.openflexo.foundation.viewpoint.IndividualParameter;
 import org.openflexo.foundation.viewpoint.IndividualPatternRole;
 import org.openflexo.foundation.viewpoint.IntegerParameter;
 import org.openflexo.foundation.viewpoint.ObjectPropertyAssertion;
 import org.openflexo.foundation.viewpoint.ShapePatternRole;
 import org.openflexo.foundation.viewpoint.TextFieldParameter;
 import org.openflexo.foundation.viewpoint.URIParameter;
 import org.openflexo.foundation.viewpoint.ViewPoint.ViewPointBuilder;
 import org.openflexo.foundation.viewpoint.binding.ViewPointDataBinding;
 import org.openflexo.foundation.viewpoint.inspector.CheckboxInspectorEntry;
 import org.openflexo.foundation.viewpoint.inspector.EditionPatternInspector;
 import org.openflexo.foundation.viewpoint.inspector.FloatInspectorEntry;
 import org.openflexo.foundation.viewpoint.inspector.InspectorEntry;
 import org.openflexo.foundation.viewpoint.inspector.IntegerInspectorEntry;
 import org.openflexo.foundation.viewpoint.inspector.TextFieldInspectorEntry;
import org.openflexo.localization.FlexoLocalization;
 import org.openflexo.toolbox.JavaUtils;
 import org.openflexo.toolbox.StringUtils;
 
 public class DeclareShapeInEditionPattern extends DeclareInEditionPattern<DeclareShapeInEditionPattern, ExampleDrawingShape> {
 
 	private static final Logger logger = Logger.getLogger(DeclareShapeInEditionPattern.class.getPackage().getName());
 
 	public static FlexoActionType<DeclareShapeInEditionPattern, ExampleDrawingShape, ExampleDrawingObject> actionType = new FlexoActionType<DeclareShapeInEditionPattern, ExampleDrawingShape, ExampleDrawingObject>(
 			"declare_in_edition_pattern", FlexoActionType.defaultGroup, FlexoActionType.NORMAL_ACTION_TYPE) {
 
 		/**
 		 * Factory method
 		 */
 		@Override
 		public DeclareShapeInEditionPattern makeNewAction(ExampleDrawingShape focusedObject, Vector<ExampleDrawingObject> globalSelection,
 				FlexoEditor editor) {
 			return new DeclareShapeInEditionPattern(focusedObject, globalSelection, editor);
 		}
 
 		@Override
 		public boolean isVisibleForSelection(ExampleDrawingShape shape, Vector<ExampleDrawingObject> globalSelection) {
 			return true;
 		}
 
 		@Override
 		public boolean isEnabledForSelection(ExampleDrawingShape shape, Vector<ExampleDrawingObject> globalSelection) {
 			return shape != null && shape.getViewPoint() != null;
 		}
 
 	};
 
 	static {
 		FlexoModelObject.addActionForClass(DeclareShapeInEditionPattern.actionType, ExampleDrawingShape.class);
 	}
 
 	public static enum NewEditionPatternChoices {
 		MAP_SINGLE_INDIVIDUAL, BLANK_EDITION_PATTERN
 	}
 
 	public NewEditionPatternChoices patternChoice = NewEditionPatternChoices.MAP_SINGLE_INDIVIDUAL;
 
 	private String editionPatternName;
 	private OntologyClass concept;
 	private String individualPatternRoleName;
 
 	public boolean isTopLevel = true;
 	public EditionPattern containerEditionPattern;
 	private String dropSchemeName;
 
 	private EditionPattern newEditionPattern;
 	private Hashtable<ExampleDrawingObjectEntry, GraphicalElementPatternRole> newGraphicalElementPatternRoles;
 
 	public Vector<PropertyEntry> propertyEntries = new Vector<PropertyEntry>();
 
 	DeclareShapeInEditionPattern(ExampleDrawingShape focusedObject, Vector<ExampleDrawingObject> globalSelection, FlexoEditor editor) {
 		super(actionType, focusedObject, globalSelection, editor);
 	}
 
 	@Override
 	protected void doAction(Object context) {
 		logger.info("Declare shape in edition pattern");
 		if (isValid()) {
 			switch (primaryChoice) {
 			case CHOOSE_EXISTING_EDITION_PATTERN:
 				if (getPatternRole() != null) {
 					getPatternRole().updateGraphicalRepresentation(getFocusedObject().getGraphicalRepresentation());
 				}
 				break;
 			case CREATES_EDITION_PATTERN:
 				ViewPointBuilder builder = new ViewPointBuilder(getFocusedObject().getViewPoint());
 				switch (patternChoice) {
 				case MAP_SINGLE_INDIVIDUAL:
 				case BLANK_EDITION_PATTERN:
 
 					// Create new edition pattern
 					newEditionPattern = new EditionPattern(builder);
 					newEditionPattern.setName(getEditionPatternName());
 
 					// Find best URI base candidate
 					PropertyEntry mainPropertyDescriptor = selectBestEntryForURIBaseName();
 
 					// Create individual pattern role
 					IndividualPatternRole individualPatternRole = new IndividualPatternRole(builder);
 					if (patternChoice == NewEditionPatternChoices.MAP_SINGLE_INDIVIDUAL) {
 						individualPatternRole.setPatternRoleName(getIndividualPatternRoleName());
 						individualPatternRole.setOntologicType(getConcept());
 						newEditionPattern.addToPatternRoles(individualPatternRole);
 						newEditionPattern.setPrimaryConceptRole(individualPatternRole);
 					}
 
 					// Create graphical elements pattern role
 
 					newGraphicalElementPatternRoles = new Hashtable<ExampleDrawingObjectEntry, GraphicalElementPatternRole>();
 
 					GraphicalElementPatternRole primaryRepresentationRole = null;
 					for (ExampleDrawingObjectEntry entry : drawingObjectEntries) {
 						if (entry.getSelectThis()) {
 							if (entry.graphicalObject instanceof ExampleDrawingShape) {
 								ShapePatternRole newShapePatternRole = new ShapePatternRole(builder);
 								newShapePatternRole.setPatternRoleName(entry.patternRoleName);
 								if (mainPropertyDescriptor != null && entry.isMainEntry()) {
 									newShapePatternRole.setLabel(new ViewPointDataBinding(getIndividualPatternRoleName() + "."
 											+ mainPropertyDescriptor.property.getName()));
 								} else {
 									newShapePatternRole.setReadOnlyLabel(true);
 									if (StringUtils.isNotEmpty(entry.graphicalObject.getName())) {
 										newShapePatternRole
 												.setLabel(new ViewPointDataBinding("\"" + entry.graphicalObject.getName() + "\""));
 									}
 								}
 								newShapePatternRole.setExampleLabel(((ShapeGraphicalRepresentation) entry.graphicalObject
 										.getGraphicalRepresentation()).getText());
 								// We clone here the GR (fixed unfocusable GR bug)
 								newShapePatternRole.setGraphicalRepresentation(((ShapeGraphicalRepresentation<?>) entry.graphicalObject
 										.getGraphicalRepresentation()).clone());
 								// Forces GR to be displayed in view
 								((ShapeGraphicalRepresentation<?>) newShapePatternRole.getGraphicalRepresentation())
 										.setAllowToLeaveBounds(false);
 								newEditionPattern.addToPatternRoles(newShapePatternRole);
 								if (entry.getParentEntry() != null) {
 									newShapePatternRole.setParentShapePatternRole((ShapePatternRole) newGraphicalElementPatternRoles
 											.get(entry.getParentEntry()));
 								}
 								if (entry.isMainEntry()) {
 									primaryRepresentationRole = newShapePatternRole;
 								}
 								newGraphicalElementPatternRoles.put(entry, newShapePatternRole);
 							}
 						}
 					}
 					newEditionPattern.setPrimaryRepresentationRole(primaryRepresentationRole);
 
 					// Create other individual roles
 					Vector<IndividualPatternRole> otherRoles = new Vector<IndividualPatternRole>();
 					if (patternChoice == NewEditionPatternChoices.MAP_SINGLE_INDIVIDUAL) {
 						for (PropertyEntry e : propertyEntries) {
 							if (e.selectEntry) {
 								if (e.property instanceof OntologyObjectProperty) {
 									OntologyObject range = ((OntologyObjectProperty) e.property).getRange();
 									if (range instanceof OntologyClass) {
 										IndividualPatternRole newPatternRole = new IndividualPatternRole(builder);
 										newPatternRole.setPatternRoleName(e.property.getName());
 										newPatternRole.setOntologicType((OntologyClass) range);
 										newEditionPattern.addToPatternRoles(newPatternRole);
 										otherRoles.add(newPatternRole);
 									}
 								}
 							}
 						}
 					}
 
 					// Create new drop scheme
 					DropScheme newDropScheme = new DropScheme(builder);
 					newDropScheme.setName(getDropSchemeName());
 					newDropScheme.setTopTarget(isTopLevel);
 					if (!isTopLevel) {
 						newDropScheme.setTargetEditionPattern(containerEditionPattern);
 					}
 
 					// Parameters
 					if (patternChoice == NewEditionPatternChoices.MAP_SINGLE_INDIVIDUAL) {
 						Vector<PropertyEntry> candidates = new Vector<PropertyEntry>();
 						for (PropertyEntry e : propertyEntries) {
							if (e != null && e.property != null && e.selectEntry) {
 								EditionSchemeParameter newParameter = null;
 								if (e.property instanceof OntologyDataProperty) {
 									switch (((OntologyDataProperty) e.property).getDataType()) {
 									case Boolean:
 										newParameter = new CheckboxParameter(builder);
 										newParameter.setName(e.property.getName());
 										newParameter.setLabel(e.label);
 										break;
 									case Byte:
 									case Integer:
 									case Long:
 									case Short:
 										newParameter = new IntegerParameter(builder);
 										newParameter.setName(e.property.getName());
 										newParameter.setLabel(e.label);
 										break;
 									case Double:
 									case Float:
 										newParameter = new FloatParameter(builder);
 										newParameter.setName(e.property.getName());
 										newParameter.setLabel(e.label);
 										break;
 									case String:
 										newParameter = new TextFieldParameter(builder);
 										newParameter.setName(e.property.getName());
 										newParameter.setLabel(e.label);
 										break;
 									default:
 										break;
 									}
 								} else if (e.property instanceof OntologyObjectProperty) {
 									OntologyObject range = ((OntologyObjectProperty) e.property).getRange();
 									if (range instanceof OntologyClass) {
 										newParameter = new IndividualParameter(builder);
 										newParameter.setName(e.property.getName());
 										newParameter.setLabel(e.label);
 										((IndividualParameter) newParameter).setConcept((OntologyClass) range);
 									}
 								}
 								if (newParameter != null) {
 									newDropScheme.addToParameters(newParameter);
 								}
 							}
 						}
 
 						URIParameter uriParameter = new URIParameter(builder);
 						uriParameter.setName("uri");
 						uriParameter.setLabel("uri");
 						if (mainPropertyDescriptor != null) {
 							uriParameter.setBaseURI(new ViewPointDataBinding(mainPropertyDescriptor.property.getName()));
 						}
 						newDropScheme.addToParameters(uriParameter);
 
 						// Declare pattern role
 						for (IndividualPatternRole r : otherRoles) {
 							DeclarePatternRole action = new DeclarePatternRole(builder);
 							action.setAssignation(new ViewPointDataBinding(r.getPatternRoleName()));
 							action.setObject(new ViewPointDataBinding("parameters." + r.getName()));
 							newDropScheme.addToActions(action);
 						}
 
 						// Add individual action
 						AddIndividual newAddIndividual = new AddIndividual(builder);
 						newAddIndividual.setAssignation(new ViewPointDataBinding(individualPatternRole.getPatternRoleName()));
 						newAddIndividual.setIndividualName(new ViewPointDataBinding("parameters.uri"));
 						for (PropertyEntry e : propertyEntries) {
 							if (e.selectEntry) {
 								if (e.property instanceof OntologyObjectProperty) {
 									OntologyObject range = ((OntologyObjectProperty) e.property).getRange();
 									if (range instanceof OntologyClass) {
 										ObjectPropertyAssertion propertyAssertion = new ObjectPropertyAssertion(builder);
 										propertyAssertion.setOntologyProperty(e.property);
 										propertyAssertion.setObject(new ViewPointDataBinding("parameters." + e.property.getName()));
 										newAddIndividual.addToObjectAssertions(propertyAssertion);
 									}
 								} else if (e.property instanceof OntologyDataProperty) {
 									DataPropertyAssertion propertyAssertion = new DataPropertyAssertion(builder);
 									propertyAssertion.setOntologyProperty(e.property);
 									propertyAssertion.setValue(new ViewPointDataBinding("parameters." + e.property.getName()));
 									newAddIndividual.addToDataAssertions(propertyAssertion);
 								}
 							}
 						}
 						newDropScheme.addToActions(newAddIndividual);
 					}
 
 					// Add shape/connector actions
 					boolean mainPatternRole = true;
 					for (GraphicalElementPatternRole graphicalElementPatternRole : newGraphicalElementPatternRoles.values()) {
 						if (graphicalElementPatternRole instanceof ShapePatternRole) {
 							// Add shape action
 							AddShape newAddShape = new AddShape(builder);
 							newAddShape.setAssignation(new ViewPointDataBinding(graphicalElementPatternRole.getPatternRoleName()));
 							if (mainPatternRole) {
 								if (isTopLevel) {
 									newAddShape.setContainer(new ViewPointDataBinding(EditionScheme.TOP_LEVEL));
 								} else {
 									newAddShape.setContainer(new ViewPointDataBinding(EditionScheme.TARGET + "."
 											+ containerEditionPattern.getPrimaryRepresentationRole().getPatternRoleName()));
 								}
 							}
 							mainPatternRole = false;
 							newDropScheme.addToActions(newAddShape);
 						}
 					}
 
 					// Add new drop scheme
 					newEditionPattern.addToEditionSchemes(newDropScheme);
 
 					// Add inspector
 					EditionPatternInspector inspector = newEditionPattern.getInspector();
 					inspector.setInspectorTitle(getEditionPatternName());
 					if (patternChoice == NewEditionPatternChoices.MAP_SINGLE_INDIVIDUAL) {
 						for (PropertyEntry e : propertyEntries) {
 							if (e.selectEntry) {
 								if (e.property instanceof OntologyObjectProperty) {
 									OntologyObject range = ((OntologyObjectProperty) e.property).getRange();
 									if (range instanceof OntologyClass) {
 										InspectorEntry newInspectorEntry = null;
 										newInspectorEntry = new TextFieldInspectorEntry(builder);
 										newInspectorEntry.setName(e.property.getName());
 										newInspectorEntry.setLabel(e.label);
 										newInspectorEntry.setIsReadOnly(true);
 										newInspectorEntry.setData(new ViewPointDataBinding(e.property.getName() + ".uriName"));
 										inspector.addToEntries(newInspectorEntry);
 									}
 								} else if (e.property instanceof OntologyDataProperty) {
 									InspectorEntry newInspectorEntry = null;
 									switch (((OntologyDataProperty) e.property).getDataType()) {
 									case Boolean:
 										newInspectorEntry = new CheckboxInspectorEntry(builder);
 										break;
 									case Byte:
 									case Integer:
 									case Long:
 									case Short:
 										newInspectorEntry = new IntegerInspectorEntry(builder);
 										break;
 									case Double:
 									case Float:
 										newInspectorEntry = new FloatInspectorEntry(builder);
 										break;
 									case String:
 										newInspectorEntry = new TextFieldInspectorEntry(builder);
 										break;
 									default:
 										logger.warning("Not handled: " + ((OntologyDataProperty) e.property).getDataType());
 									}
 									if (newInspectorEntry != null) {
 										newInspectorEntry.setName(e.property.getName());
 										newInspectorEntry.setLabel(e.label);
 										newInspectorEntry.setData(new ViewPointDataBinding(getIndividualPatternRoleName() + "."
 												+ e.property.getName()));
 										inspector.addToEntries(newInspectorEntry);
 									}
 								}
 							}
 						}
 					}
 
 					// And add the newly created edition pattern
 					getFocusedObject().getViewPoint().addToEditionPatterns(newEditionPattern);
 
 				default:
 					break;
 				}
 			default:
 				logger.warning("Pattern not implemented");
 			}
 		} else {
 			logger.warning("Focused role is null !");
 		}
 	}
 
 	@Override
 	public boolean isValid() {
 		if (getFocusedObject() == null) {
 			return false;
 		}
 		switch (primaryChoice) {
 		case CHOOSE_EXISTING_EDITION_PATTERN:
 			return getEditionPattern() != null && getPatternRole() != null;
 		case CREATES_EDITION_PATTERN:
 			switch (patternChoice) {
 			case MAP_SINGLE_INDIVIDUAL:
 				return StringUtils.isNotEmpty(getEditionPatternName()) && concept != null
 						&& StringUtils.isNotEmpty(getIndividualPatternRoleName()) && getSelectedEntriesCount() > 0
 						&& (isTopLevel || containerEditionPattern != null) && StringUtils.isNotEmpty(getDropSchemeName());
 			case BLANK_EDITION_PATTERN:
 				return StringUtils.isNotEmpty(getEditionPatternName()) && getSelectedEntriesCount() > 0
 						&& (isTopLevel || containerEditionPattern != null) && StringUtils.isNotEmpty(getDropSchemeName());
 			default:
 				break;
 			}
 		default:
 			return false;
 		}
 	}
 
 	private ShapePatternRole patternRole;
 
 	@Override
 	public ShapePatternRole getPatternRole() {
 		return patternRole;
 	}
 
 	public void setPatternRole(ShapePatternRole patternRole) {
 		this.patternRole = patternRole;
 	}
 
 	@Override
 	public void resetPatternRole() {
 		this.patternRole = null;
 	}
 
 	public OntologyClass getConcept() {
 		return concept;
 	}
 
 	public void setConcept(OntologyClass concept) {
 		this.concept = concept;
 		propertyEntries.clear();
 		FlexoOntology ownerOntology = concept.getFlexoOntology();
 		for (OntologyProperty p : concept.getPropertiesTakingMySelfAsDomain()) {
 			if (p.getFlexoOntology() == ownerOntology) {
 				PropertyEntry newEntry = new PropertyEntry(p);
 				propertyEntries.add(newEntry);
 			}
 		}
 	}
 
 	public String getEditionPatternName() {
 		if (StringUtils.isEmpty(editionPatternName) && concept != null) {
 			return concept.getName();
 		}
 		return editionPatternName;
 	}
 
 	public void setEditionPatternName(String editionPatternName) {
 		this.editionPatternName = editionPatternName;
 	}
 
 	public String getIndividualPatternRoleName() {
 		if (StringUtils.isEmpty(individualPatternRoleName) && concept != null) {
 			return JavaUtils.getVariableName(concept.getName());
 		}
 		return individualPatternRoleName;
 	}
 
 	public void setIndividualPatternRoleName(String individualPatternRoleName) {
 		this.individualPatternRoleName = individualPatternRoleName;
 	}
 
 	/*public String getShapePatternRoleName() {
 		if (StringUtils.isEmpty(shapePatternRoleName)) {
 			return "shape";
 		}
 		return shapePatternRoleName;
 	}
 
 	public void setShapePatternRoleName(String shapePatternRoleName) {
 		this.shapePatternRoleName = shapePatternRoleName;
 	}*/
 
 	public String getDropSchemeName() {
 		if (StringUtils.isEmpty(dropSchemeName)) {
 			return "drop" + (StringUtils.isEmpty(getEditionPatternName()) ? "" : getEditionPatternName())
 					+ (isTopLevel ? "AtTopLevel" : containerEditionPattern != null ? "In" + containerEditionPattern.getName() : "");
 		}
 		return dropSchemeName;
 	}
 
 	public void setDropSchemeName(String dropSchemeName) {
 		this.dropSchemeName = dropSchemeName;
 	}
 
 	public class PropertyEntry {
 
 		public OntologyProperty property;
 		public String label;
 		public boolean selectEntry = false;
 
 		public PropertyEntry(OntologyProperty property) {
 			this.property = property;
			if (property != null) {
				if (StringUtils.isNotEmpty(property.getDescription())) {
					label = property.getDescription();
				} else {
					label = property.getName() + "_of_" + getIndividualPatternRoleName();
				}
 			} else {
				label = FlexoLocalization.localizedForKey("property");
 			}
 		}
 
 		public String getRange() {
 			if (property instanceof OntologyDataProperty) {
 				if (((OntologyDataProperty) property).getDataType() != null) {
 					return ((OntologyDataProperty) property).getDataType().name();
 				}
 				return "";
 			}
 			if (property instanceof OntologyObjectProperty && ((OntologyObjectProperty) property).getRange() != null) {
 				return ((OntologyObjectProperty) property).getRange().getName();
 			}
 			return "";
 		}
 	}
 
 	private PropertyEntry selectBestEntryForURIBaseName() {
 		Vector<PropertyEntry> candidates = new Vector<PropertyEntry>();
 		for (PropertyEntry e : propertyEntries) {
 			if (e.selectEntry && e.property instanceof OntologyDataProperty
 					&& ((OntologyDataProperty) e.property).getDataType() == OntologicDataType.String) {
 				candidates.add(e);
 			}
 		}
 		if (candidates.size() > 0) {
 			return candidates.firstElement();
 		}
 		return null;
 	}
 
 	public PropertyEntry createPropertyEntry() {
 		PropertyEntry newPropertyEntry = new PropertyEntry(null);
 		propertyEntries.add(newPropertyEntry);
 		return newPropertyEntry;
 	}
 
 	public PropertyEntry deletePropertyEntry(PropertyEntry aPropertyEntry) {
 		propertyEntries.remove(aPropertyEntry);
 		return aPropertyEntry;
 	}
 
 	public void selectAllProperties() {
 		for (PropertyEntry e : propertyEntries) {
 			e.selectEntry = true;
 		}
 	}
 
 	public void selectNoneProperties() {
 		for (PropertyEntry e : propertyEntries) {
 			e.selectEntry = false;
 		}
 	}
 
 	@Override
 	public EditionPattern getEditionPattern() {
 		if (primaryChoice == DeclareInEditionPatternChoices.CREATES_EDITION_PATTERN) {
 			return newEditionPattern;
 		}
 		return super.getEditionPattern();
 	};
 
 }
