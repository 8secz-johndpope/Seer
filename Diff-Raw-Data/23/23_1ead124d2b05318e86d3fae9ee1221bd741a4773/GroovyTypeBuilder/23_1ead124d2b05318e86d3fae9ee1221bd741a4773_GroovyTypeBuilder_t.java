 /***************************************************************
  *  This file is part of the [fleXive](R) framework.
  *
  *  Copyright (c) 1999-2008
  *  UCS - unique computing solutions gmbh (http://www.ucs.at)
  *  All rights reserved
  *
  *  The [fleXive](R) project is free software; you can redistribute
  *  it and/or modify it under the terms of the GNU Lesser General Public
  *  License version 2.1 or higher as published by the Free Software Foundation.
  *
  *  The GNU Lesser General Public License can be found at
  *  http://www.gnu.org/licenses/lgpl.html.
  *  A copy is found in the textfile LGPL.txt and important notices to the
  *  license from the author are found in LICENSE.txt distributed with
  *  these libraries.
  *
  *  This library is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  For further information about UCS - unique computing solutions gmbh,
  *  please see the company website: http://www.ucs.at
  *
  *  For further information about [fleXive](R), please see the
  *  project website: http://www.flexive.org
  *
  *
  *  This copyright notice MUST APPEAR in all copies of the file!
  ***************************************************************/
 package com.flexive.shared.scripting.groovy;
 
 import com.flexive.shared.CacheAdmin;
 import com.flexive.shared.EJBLookup;
 import com.flexive.shared.FxSharedUtils;
 import com.flexive.shared.exceptions.FxApplicationException;
 import com.flexive.shared.exceptions.FxInvalidParameterException;
 import com.flexive.shared.exceptions.FxNotFoundException;
 import com.flexive.shared.security.ACL;
 import com.flexive.shared.security.ACLCategory;
 import com.flexive.shared.structure.*;
 import com.flexive.shared.value.FxString;
 import com.flexive.shared.value.FxValue;
 import com.flexive.shared.value.FxReference;
 import groovy.util.BuilderSupport;
 import org.apache.commons.lang.StringUtils;
 
 import java.util.*;
 import java.io.Serializable;
 
 /**
  * A {@link com.flexive.shared.structure.FxType FxType} groovy builder. By convention,
  * group names start with an uppercase letter, and properties with a lowercase letter.
  * An example, taken from the test cases:
  * <p/>
  * <pre>
  * // create the type "builderTest"
  * new GroovyTypeBuilder().builderTest {
  * // assign the caption property
  * myCaption(assignment: "ROOT/CAPTION")
  * // add some new properties
  * stringPropertyDefault()
  * numberProperty(FxDataType.Number)
  * descriptionProperty(description: new FxString("string property description"))
  * multilineProperty(multiline: true)
  * multilangProperty(multilang: true)
  * uniqueProperty(uniqueMode: UniqueMode.Global)
  * referenceProperty(FxDataType.Reference, referencedType: CacheAdmin.environment.getType("DOCUMENT"))
  * listProperty(FxDataType.SelectMany, referencedList: CacheAdmin.environment.getSelectLists().get(0))
  * <p/>
  * // create a new group
  * MultiGroup(description: new FxString("my group"), multiplicity: FxMultiplicity.MULT_0_N) {
  * // assign a property inside the group
  * nestedCaption(assignment: "ROOT/CAPTION")
  * // create a new property
  * groupNumberProperty(FxDataType.Number)
  * <p/>
  * // nest another group
  * NestedGroup(multiplicity: FxMultiplicity.MULT_1_N) {
  * nestedProperty()
  * }
  * }
  * }
  * </pre>
  * <p>
  * Type node arguments:
  * <table>
  * <tr>
  * <th>description</th>
  * <td>FxString</td>
  * <td>The type description</td>
  * </tr>
  * <tr>
  * <th>acl</th>
  * <td>ACL</td>
  * <td>The type ACL to be used</td>
  * </tr>
  * <tr>
  * <th>useInstancePermissions,<br/>
  * usePropertyPermissions,<br/>
  * useStepPermissions,<br/>
  * useTypePermissions</th>
  * <td>boolean</td>
  * <td>Enable or disable the given type permissions.</td>
  * </tr>
  * <tr>
  * <th>usePermissions</th>
  * <td>boolean</td>
  * <td>If set to false, disables all permissions checks for the type (and contents of this type)</td>
  * </tr>
  * </table>
  * </p>
  *
  * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
  * @version $Rev$
  */
 public class GroovyTypeBuilder extends BuilderSupport implements Serializable {
     private static final long serialVersionUID = -6856824640709225006L;
     private static final Map<Object, Object> EMPTYATTRIBUTES = Collections.unmodifiableMap(new HashMap<Object, Object>());
 
     /**
      * List of keys that are not used for options and are "real" parameters
      * when creating a new property
      */
     private final static String[] PROPERTY_NONOPTION_KEYS = {
             "DATATYPE", "MULTIPLICITY", "NAME", "DESCRIPTION", "LABEL", "HINT", "ACL", "ASSIGNMENT",
             "ALIAS", "DEFAULTMULTIPLICITY", "DEFAULTVALUE", "OVERRIDEMULTILANG", "MULTILANG",
             "OVERRIDEACL", "OVERRIDEMULTIPLICITY", "OVERRIDEINOVERVIEW", "OVERRIDEMAXLENGTH",
             "OVERRIDEMULTILINE", "OVERRIDESEARCHABLE", "OVERRIDEUSEHTMLEDITOR",
             "DEFAULTLANGUAGE", "PARENTGROUPASSIGNMENT", "ENABLED", "USEHTMLEDITOR",
             "FULLTEXTINDEXED", "UNIQUEMODE", "AUTOUNIQUEPROPERTYNAME", "REFERENCEDLIST", "REFERENCEDTYPE"
     };
 
     /**
      * List of keys that are not used for options and are "real" parameters
      * when creating a reused property assignment
      */
     private final static String[] ASSIGNMENT_NONOPTION_KEYS = {
             "DATATYPE", "MULTIPLICITY", "NAME", "DESCRIPTION", "LABEL", "HINT", "ACL", "ASSIGNMENT",
             "ALIAS", "DEFAULTMULTIPLICITY", "DEFAULTVALUE", "OVERRIDEMULTILANG", "MULTILANG",
             "OVERRIDEACL", "OVERRIDEMULTIPLICITY", "OVERRIDEINOVERVIEW", "OVERRIDEMAXLENGTH",
             "OVERRIDEMULTILINE", "OVERRIDESEARCHABLE", "OVERRIDEUSEHTMLEDITOR",
             "DEFAULTLANGUAGE", "PARENTGROUPASSIGNMENT", "ENABLED", "USEHTMLEDITOR",
             "FULLTEXTINDEXED", "UNIQUEMODE", "AUTOUNIQUEPROPERTYNAME", "REFERENCEDLIST", "REFERENCEDTYPE"
     };
 
     /**
      * Check if the given key is a non-property-option key (=not used for property options)
      *
      * @param key the key to check
      * @return if its a non-option key
      */
     private static boolean isNonPropertyOptionKey(String key) {
         String uKey = key.toUpperCase();
         for (String check : PROPERTY_NONOPTION_KEYS)
             if (check.equals(uKey))
                 return true;
         return false;
     }
 
     /**
      * Check if the given key is a non-assignment-option key (=not used for property assignment options)
      *
      * @param key the key to check
      * @return if its a non-option key
      */
     private static boolean isNonAssignmentOptionKey(String key) {
         String uKey = key.toUpperCase();
         for (String check : ASSIGNMENT_NONOPTION_KEYS)
             if (check.equals(uKey))
                 return true;
         return false;
     }
 
     private FxType type;
 
     private static class Node<TElement> implements Serializable {
         private static final long serialVersionUID = 4721651554653493085L;
         protected GroupNode parent;
         protected TElement element;
 
         public Node() {
             this(null);
         }
 
         public Node(TElement element) {
             this.element = element;
         }
 
         public String getXPath() {
             return "";
         }
 
         public String getName() {
             return "ROOT";
         }
 
         public Node getParent() {
             return parent;
         }
 
         public void setParent(GroupNode parent) {
             this.parent = parent;
         }
 
         public TElement getElement() {
             return element;
         }
     }
 
     private static class StructureNode<T extends FxStructureElement> extends Node<T> {
         private static final long serialVersionUID = -2703786157094279644L;
 
         public StructureNode(T element) {
             super(element);
             FxSharedUtils.checkParameterEmpty(element, "element");
         }
 
         @Override
         public String getXPath() {
             return parent == null ? "/" + element.getName() : parent.getXPath() + "/" + element.getName();
         }
 
         @Override
         public String getName() {
             return element.getName();
         }
     }
 
     private static class PropertyNode extends StructureNode<FxPropertyEdit> {
         private static final long serialVersionUID = 7033936953834313748L;
 
         private final long typeId;
         private final String alias;
         private final String parentXPath;
 
         public PropertyNode(FxPropertyEdit element, String alias, long typeId) {
             super(element);
             this.alias = alias;
             this.typeId = typeId;
             parentXPath = null;
         }
 
         public PropertyNode(FxPropertyEdit element, String alias, long typeId, String parentXPath) {
             super(element);
             this.alias = alias;
             this.typeId = typeId;
             this.parentXPath = parentXPath;
         }
 
         @Override
         public void setParent(GroupNode parent) {
             super.setParent(parent);
             try {
                 String xPath = "/"; // root assignments
                 if (getParent() != null) {
                     if (getParent() != null) {
                         if (parent.getAssignment() == null && parentXPath != null)
                             xPath = parentXPath;
                         else
                             xPath = parent.getAssignment().getXPath();
                     }
                 }
                 // attach property
                 EJBLookup.getAssignmentEngine().createProperty(typeId, getElement(), xPath, alias);
             } catch (FxApplicationException e) {
                 throw e.asRuntimeException();
             }
         }
     }
 
     private static class GroupNode extends StructureNode<FxGroupEdit> {
         private static final long serialVersionUID = 4759700316011472966L;
 
         private FxGroupAssignment assignment;
         private final long typeId;
         private final String parentXPath;
         private final String alias;
 
         public GroupNode(FxGroupEdit element, String alias, long typeId) {
             super(element);
             this.typeId = typeId;
             parentXPath = null;
             this.alias = alias;
         }
 
         public GroupNode(FxGroupEdit element, String alias, long typeId, String parentXPath) {
             super(element);
             this.typeId = typeId;
             this.parentXPath = parentXPath;
             this.alias = alias;
         }
 
         public FxGroupAssignment getAssignment() {
             return assignment;
         }
 
         @Override
         public void setParent(GroupNode parent) {
             super.setParent(parent);
             try {
                 String xPath = "/"; // for assignments to the root
                 // retrieve the assignment if the group exists within the current type
                 if (getParent() != null) {
                     if (parent.getAssignment() == null && parentXPath != null)
                         xPath = parentXPath;
                     else
                         xPath = parent.getAssignment().getXPath();
                 }
                 final long id = EJBLookup.getAssignmentEngine().createGroup(typeId, getElement(), xPath, alias);
                 assignment = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id);
 
             } catch (FxApplicationException e) {
                 throw e.asRuntimeException();
             }
         }
     }
 
     private static class PropertyAssignmentNode extends Node<FxPropertyAssignmentEdit> {
         private static final long serialVersionUID = -5173534896365045004L;
 
         private final AttributeMapper am;
 
         public PropertyAssignmentNode(FxPropertyAssignmentEdit element, AttributeMapper am) {
             super(element);
             this.am = am;
         }
 
         @Override
         public void setParent(GroupNode parent) {
             super.setParent(parent);
             try {
                 final String xPath;
                 if (am.isNew) {
                     if (getParent() != null) {
                         if (parent.getAssignment() == null) {// for calls with attributes (existing groups)
                             xPath = am.parentXPath != null ? am.parentXPath + element.getXPath() : parent.parentXPath + element.getXPath();
                             element.setParentGroupAssignment((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(
                                     am.parentXPath != null ? am.parentXPath : parent.parentXPath));
                         } else {
                             xPath = parent.getAssignment().getXPath() + element.getXPath();
                             element.setParentGroupAssignment(parent.getAssignment());
                         }
                         element.setXPath(xPath);
                     }
                     element.setPosition(FxAssignment.POSITION_BOTTOM); //set to the bottom-most position
                 }
                 if (am.assignmentChanges || am.isNew) // only save if changes need to be made or if we have a new assignment
                     EJBLookup.getAssignmentEngine().save(element, am.createSubAssignments);
             } catch (FxApplicationException e) {
                 throw e.asRuntimeException();
             }
         }
     }
 
     private static class GroupAssignmentNode extends Node<FxGroupAssignmentEdit> {
         private static final long serialVersionUID = -6625714158681292375L;
 
         private final AttributeMapper am;
 
         private GroupAssignmentNode(FxGroupAssignmentEdit element, AttributeMapper am) {
             super(element);
             this.am = am;
         }
 
         @Override
         public void setParent(GroupNode parent) {
             super.setParent(parent);
             try {
                 final String xPath;
                 if (am.isNew) {
                     if (getParent() != null) { // root assignments have no parent
                         if (parent.getAssignment() == null) { // for calls with attributes (existing groups)
                             xPath = am.parentXPath != null ? am.parentXPath + element.getXPath() : parent.parentXPath + element.getXPath();
                             element.setParentGroupAssignment((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(
                                     am.parentXPath != null ? am.parentXPath : parent.parentXPath));
                         } else {
                             xPath = parent.getAssignment().getXPath() + element.getXPath();
                             element.setParentGroupAssignment(parent.getAssignment());
                         }
                         element.setXPath(xPath);
                     }
                     element.setPosition(FxAssignment.POSITION_BOTTOM); //set to the bottom-most position
                 }
                 if (am.assignmentChanges || am.isNew) // only save if changes need to be made or if we have a new assignment
                     EJBLookup.getAssignmentEngine().save(element, am.createSubAssignments);
             } catch (FxApplicationException e) {
                 throw e.asRuntimeException();
             }
         }
     }
 
     public GroovyTypeBuilder() {
     }
 
     public GroovyTypeBuilder(FxType type) {
         this.type = type;
     }
 
     public GroovyTypeBuilder(String typeName) {
         this.type = CacheAdmin.getEnvironment().getType(typeName);
     }
 
     public GroovyTypeBuilder(long typeId) {
         this.type = CacheAdmin.getEnvironment().getType(typeId);
     }
 
     @Override
     protected void setParent(Object parent, Object child) {
         if (parent == null) {
             return;
         }
         final Node parentNode = (Node) parent;
         final Node childNode = (Node) child;
 
         if (StringUtils.isBlank(parentNode.getXPath())) {
             if (parent instanceof GroupAssignmentNode) {
                 final FxGroupAssignment ga = (FxGroupAssignment) parentNode.getElement();
                 childNode.setParent(new GroupNode(ga.getGroup().asEditable(), ga.getAlias(), ga.getAssignedType().getId(), ga.getXPath()));
             } else {
                 childNode.setParent(null); // attaching to the root node
             }
             return;
         }
         if (!(parentNode instanceof GroupNode)) {
             throw new FxInvalidParameterException("parent", "ex.scripting.builder.type.parent",
                     childNode.getName(), parentNode.getName()).asRuntimeException();
         }
         childNode.setParent((GroupNode) parentNode);
     }
 
     @Override
     protected Object createNode(Object name) {
         return "call".equals(name) || "doCall".equals(name) ? new Node() : createNode(name, EMPTYATTRIBUTES, null);
     }
 
     @Override
     protected Object createNode(Object name, Object value) {
         return createNode(name, EMPTYATTRIBUTES, value);
     }
 
     @Override
     protected Object createNode(Object name, Map attributes) {
         return createNode(name, attributes, null);
     }
 
     @SuppressWarnings({"unchecked"})
     @Override
     protected Object createNode(Object name, Map attributes, Object value) {
         final String structureName = (String) name;
         if (this.type == null) {
             // AttributeMapper
             final AttributeMapper am = new AttributeMapper(attributes, value, structureName).setStructureAttributes();
             // root node, create type
             try {
                 final FxTypeEdit type = FxTypeEdit.createNew(structureName, am.label, am.acl,
                         am.parentTypeName != null ? CacheAdmin.getEnvironment().getType(am.parentTypeName) : null);
                 am.setTypeAttributes(type); // set type attributes
 
                 final long typeId = EJBLookup.getTypeEngine().save(type);
                 this.type = CacheAdmin.getEnvironment().getType(typeId);
                 return new Node();
             } catch (FxApplicationException e) {
                 throw e.asRuntimeException();
             }
         }
 
         final AttributeMapper am = new AttributeMapper(attributes, value, structureName).setElementAttributes();
         boolean hasParent = this.getCurrent() instanceof GroupAssignmentNode || this.getCurrent() instanceof GroupNode;
         if (hasParent) {// set the parent xPath immed. for assignments and reload the type from the cache
             am.parentXPath = getXPathFromStructureName(structureName, true);
             type = CacheAdmin.getEnvironment().getType(type.getId());
         }
         // name starts lowercase --> create property or property assignment
         if (Character.isLowerCase(structureName.charAt(0))) {
             if (am.assignment != null) { // create a new property assignment
                final FxAssignment fxAssignment = CacheAdmin.getEnvironment().getAssignment(am.assignment.startsWith("/") ? type.getName() + am.assignment : am.assignment);
                 try {
                     return createNewPropertyAssignmentNode(fxAssignment, am);
                 } catch (FxApplicationException e) {
                     throw e.asRuntimeException();
                 }
             } else {
                 try {
                     // existing property assignments
                     if (CacheAdmin.getEnvironment().propertyExistsInType(type.getName(), am.structureName)
                             || structureAssignmentIsInType(structureName, hasParent, false)) {
                         final FxAssignment fxAssignment;
                         am.isNew = false;
                         am.assignmentChanges = attributes.size() > 0;
                         final String xPath;
                         if (hasParent) {
                             if (CacheAdmin.getEnvironment().assignmentExists(getXPathFromStructureName(structureName, false)))
                                 xPath = getXPathFromStructureName(structureName, false);
                             else // try an alias
                                 xPath = getXPathFromAlias(am.structureName, false, true);
 
                         } else { // root
                             if (CacheAdmin.getEnvironment().assignmentExists(type.getName() + "/" + structureName.toUpperCase()))
                                 xPath = type.getName() + "/" + structureName;
                             else // we might have a different alias
                                 xPath = getXPathFromAlias(am.structureName, false, false);
                         }
 
                         if (xPath == null)
                             throw new FxNotFoundException("ex.scripting.builder.structure.notFound", structureName, type.getName()).asRuntimeException();
                         // retrieve the fxAssignment and set the correct alias if applicable
                         fxAssignment = CacheAdmin.getEnvironment().getAssignment(xPath);
                         // also set the correct alias in the attributesmapper
                         if (!attributes.containsKey("alias")) {
                             am.alias = fxAssignment.getAlias();
                         }
                         return createNewPropertyAssignmentNode(fxAssignment, am);
                     }
                     // create a new property
                     final FxPropertyEdit property = FxPropertyEdit.createNew(StringUtils.capitalize(am.elementName),
                             am.label, am.hint, am.multiplicity, am.acl, am.dataType);
                     am.setPropertyAttributes(property); // set property attributes
 
                     if (this.getCurrent() instanceof GroupAssignmentNode) {
                         final GroupAssignmentNode node = (GroupAssignmentNode) getCurrent(); // retrieve the parent
                         return new PropertyNode(property, am.alias, type.getId(), node.getElement().getXPath());
 
                     } else
                         return new PropertyNode(property, am.alias, type.getId());
 
                 } catch (FxApplicationException e) {
                     throw e.asRuntimeException();
                 }
             }
         } else { // name starts uppercase --> group
             if (am.assignment != null) { // create a new group assignment
                 final FxAssignment fxAssignment = CacheAdmin.getEnvironment().getAssignment(am.assignment);
                 try {
                     return createNewGroupAssignmentNode(fxAssignment, am);
                 } catch (FxApplicationException e) {
                     throw e.asRuntimeException();
                 }
             } else {
                 try {
                     // do not create a new group if it already exists - for calls to the builder to change or add existing assignments
                     if (CacheAdmin.getEnvironment().groupExistsInType(type.getName(), am.structureName)
                             || structureAssignmentIsInType(structureName, hasParent, true)) {
                         final FxAssignment fxAssignment;
                         am.isNew = false;
                         am.assignmentChanges = attributes.size() > 0;
                         final String xPath;
                         if (hasParent) {
                             if (CacheAdmin.getEnvironment().assignmentExists(getXPathFromStructureName(structureName, false)))
                                 xPath = getXPathFromStructureName(structureName, false);
                             else // try an alias
                                 xPath = getXPathFromAlias(am.structureName, true, true);
 
                         } else { // root
                             if (CacheAdmin.getEnvironment().assignmentExists(type.getName() + "/" + structureName.toUpperCase()))
                                 xPath = type.getName() + "/" + structureName;
                             else // we might have a different alias
                                 xPath = getXPathFromAlias(am.structureName, true, false);
                         }
 
                         if (xPath == null)
                             throw new FxNotFoundException("ex.scripting.builder.structure.notFound", structureName, type.getName()).asRuntimeException();
                         // retrieve the fxAssignment and set the correct alias if applicable
                         fxAssignment = CacheAdmin.getEnvironment().getAssignment(xPath);
                         // also set the correct alias in the attributesmapper
                         if (!attributes.containsKey("alias")) {
                             am.alias = fxAssignment.getAlias();
                         }
                         return createNewGroupAssignmentNode(fxAssignment, am);
                     }
 
                     FxGroupEdit ge = FxGroupEdit.createNew(am.elementName, am.label, am.hint, am.overrideMultiplicity, // false,
                             am.multiplicity);
                     am.setGroupAttributes(ge); // set group attributes
 
                     if (this.getCurrent() instanceof GroupAssignmentNode) {
                         final GroupAssignmentNode node = (GroupAssignmentNode) getCurrent(); // retrieve the parent
                         return new GroupNode(ge, am.alias, type.getId(), node.getElement().getXPath());
 
                     } else
                         return new GroupNode(ge, am.alias, type.getId());
 
                 } catch (FxApplicationException e) {
                     throw e.asRuntimeException();
                 }
             }
         }
     }
 
     /**
      * Refactoring of common call to "new GroupAssignmentNode"
      *
      * @param fxAssignment the FxAssignment for which the call will be made
      * @param am           an instance of the AttributeMapper
      * @return returns a GroupAssignmentNode ojbect
      * @throws FxApplicationException on errors
      */
     private Object createNewGroupAssignmentNode(FxAssignment fxAssignment, AttributeMapper am)
             throws FxApplicationException {
         if (!(fxAssignment instanceof FxGroupAssignment)) {
             throw new FxInvalidParameterException("assignment", "ex.scripting.builder.assignment.group",
                     am.elementName, am.assignment).asRuntimeException();
         }
         final FxGroupAssignmentEdit ga;
         if (!am.isNew) {
             ga = ((FxGroupAssignment) fxAssignment).asEditable();
             am.setGroupAssignmentAttributes(ga);
             return new GroupAssignmentNode(ga, am);
         }
 
         ga = FxGroupAssignmentEdit.createNew((FxGroupAssignment) fxAssignment, type, am.alias, "/");
         am.setGroupAssignmentAttributes(ga); // set group assignment attributes
 
         return new GroupAssignmentNode(ga, am);
     }
 
     /**
      * Refactoring of common call to "new PropertyAssignmentNode"
      *
      * @param fxAssignment the FxAssignment for which the call will be made
      * @param am           an instance of the AttributeMapper
      * @return returns a GroupAssignmentNode object
      * @throws FxApplicationException on errors
      */
     private Object createNewPropertyAssignmentNode(FxAssignment fxAssignment, AttributeMapper am) throws FxApplicationException {
         if (!(fxAssignment instanceof FxPropertyAssignment)) {
             throw new FxInvalidParameterException("assignment", "ex.scripting.builder.assignment.property",
                     am.elementName, am.assignment).asRuntimeException();
         }
         final FxPropertyAssignmentEdit pa;
         if (!am.isNew) {
             pa = ((FxPropertyAssignment) fxAssignment).asEditable();
             am.setPropertyAssignmentAttributes(pa);
             return new PropertyAssignmentNode(pa, am);
         }
 
         pa = FxPropertyAssignmentEdit.createNew((FxPropertyAssignment) fxAssignment, type, am.alias, "/");
         am.setPropertyAssignmentAttributes(pa); //set property-assignment attributes
 
         return new PropertyAssignmentNode(pa, am);
 
     }
 
     /**
      * Refactoring of finding an assignment from a potential given alias
      *
      * @param structureName    the structure's name
      * @param structureIsGroup set to true if the structure to be examined is a group(assignment)
      * @param hasParent        set to true if the structure t.b. examined has a parent (group) assignment
      * @return returns the XPath, or null if no matching XPath was found
      */
     private String getXPathFromAlias(String structureName, boolean structureIsGroup, boolean hasParent) {
         final FxEnvironment env = CacheAdmin.getEnvironment();
         final String regex1 = structureName.toUpperCase();
         final String regex2 = structureName.toUpperCase() + "\\_\\d+";
         if (hasParent) {
             final FxGroupAssignment parentAssignment = (FxGroupAssignment) env.getAssignment(getXPathFromStructureName(structureName, true));
             if (structureIsGroup) {
                 for (FxGroupAssignment ga : parentAssignment.getAssignedGroups()) {
                     if (ga.getGroup().getName().matches(regex1) || ga.getGroup().getName().matches(regex2))
                         return ga.getXPath();
                 }
             } else {
                 for (FxPropertyAssignment pa : parentAssignment.getAssignedProperties()) {
                     if (pa.getProperty().getName().matches(regex1) || pa.getProperty().getName().matches(regex2))
                         return pa.getXPath();
                 }
             }
         } else {
             if (structureIsGroup) {
                 for (FxGroupAssignment ga : type.getAssignedGroups()) {
                     if (ga.getGroup().getName().matches(regex1) || ga.getGroup().getName().matches(regex2))
                         return ga.getXPath();
                 }
             } else {
                 for (FxPropertyAssignment pa : type.getAssignedProperties()) {
                     if (pa.getProperty().getName().matches(regex1) || pa.getProperty().getName().matches(regex2))
                         return pa.getXPath();
                 }
             }
         }
         return null;
     }
 
     /**
      * Check if the given structurename (element) is assigned to the current type or group
      *
      * @param structureName the element's name
      * @param hasParent     if parent assignments exist
      * @param isGroup       the current element is a group assignment (ignored if the current element has a parent assignment)
      * @return returns true if an assignment can be found, false otherwise
      */
     private boolean structureAssignmentIsInType(String structureName, boolean hasParent, boolean isGroup) {
         if (hasParent) {
             return CacheAdmin.getEnvironment().assignmentExists(getXPathFromStructureName(structureName, false));
         } else { // root assignments
             if (isGroup) {
                 for (FxGroupAssignment ga : type.getAssignedGroups()) {
                     if (ga.getXPath().endsWith(structureName.toUpperCase()))
                         return true;
                 }
             } else {
                 for (FxPropertyAssignment pa : type.getAssignedProperties()) {
                     if (pa.getXPath().endsWith(structureName.toUpperCase()))
                         return true;
                 }
             }
         }
         return false;
     }
 
     /**
      * Returns an XPath from a given structurename, or the current parent assignment's XPath
      *
      * @param structureName the given structurename
      * @param getParent     true if only the parent assignment should be returned
      * @return returns the xPath or null
      */
     private String getXPathFromStructureName(String structureName, boolean getParent) {
         final String xPath;
         if (this.getCurrent() instanceof GroupNode) {
             final GroupNode node = (GroupNode) getCurrent();
             xPath = getParent ? node.getAssignment().getXPath() : node.getAssignment().getXPath() + "/" + structureName.toUpperCase();
             if (!xPath.startsWith(type.getName()))
                 return type.getName() + xPath;
             return xPath;
 
         } else if (this.getCurrent() instanceof GroupAssignmentNode) {
             final GroupAssignmentNode node = (GroupAssignmentNode) getCurrent(); // retrieve the parent
             xPath = getParent ? node.getElement().getXPath() : node.getElement().getXPath() + "/" + structureName.toUpperCase();
             if (!xPath.startsWith(type.getName()))
                 return type.getName() + xPath;
             return xPath;
         }
         return null;
     }
 
     /**
      * This class Maps the given Attributes for the GroovyTypeBuilder to their respective Object representations
      * and provides setters for types, properties (and assignments) and groups (and assignments),
      * and getters for all available type / property / group (and their assignments) attributes
      */
     private class AttributeMapper {
 
         // type and prop/group attributes
         final Map<String, Object> attributes;
         final Object value;
         final String structureName;
         String parentXPath = null;
         String assignment, alias, elementName, parentTypeName;
         LanguageMode languageMode;
         TypeMode typeMode;
         Long historyAge, maxVersions;
         FxDataType dataType;
         FxMultiplicity multiplicity;
         FxString label, hint;
         UniqueMode uniqueMode;
         ACL acl;
         int defaultMultiplicity;
         FxValue defaultValue;
         Boolean useInstancePermissions, useStepPermissions, useTypePermissions, usePropertyPermissions,
                 usePermissions, overrideMultilang, multilang, overrideACL, overrideMultiplicity, overrideInOverview,
                 overrideMaxLength, overrideMultiline, overrideSearchable, overrideHTMLEditor, searchable,
                 inOverview, useHTMLEditor, multiLine, enabled, trackHistory, fullTextIndexed, autoUniquePropertyName;
         Integer maxLength;
         FxType referencedType;
         FxSelectList referencedList;
         long defaultLanguage;
         GroupMode groupMode;
         FxReference icon;
         boolean createSubAssignments = false;
         boolean isNew = true;
         boolean assignmentChanges = false;
 
         /**
          * Construct an AttributeMapper
          *
          * @param attributes    the Map of GroovyTypeBuilder attributes
          * @param value         the (optional) Object value
          * @param structureName the name of the structure (type, prop, group etc).
          */
         AttributeMapper(Map<String, Object> attributes, Object value, String structureName) {
             this.attributes = attributes;
             this.structureName = structureName;
             this.value = value;
         }
 
         /**
          * Retrieves a type's attributes from the attribute map
          *
          * @return Returns the AttributeMapper itself (for chained calls)
          */
         AttributeMapper setStructureAttributes() {
             if (attributes.get("acl") instanceof ACL) {
                 acl = (ACL) FxSharedUtils.get(attributes, "acl", CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()));
             } else if (attributes.get("acl") instanceof String) {
                 final String aclString = (String) FxSharedUtils.get(attributes, "acl", "Default Structure ACL");
                 acl = CacheAdmin.getEnvironment().getACL(aclString);
             } else if (!attributes.containsKey("acl")) { // default
                 acl = CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId());
             }
             if (attributes.containsKey("description")) {
                 label = (FxString) FxSharedUtils.get(attributes, "description", new FxString(structureName));
             } else {
                 label = (FxString) FxSharedUtils.get(attributes, "label", new FxString(structureName));
             }
 
             icon = (FxReference) FxSharedUtils.get(attributes, "icon", null);
             parentTypeName = (String) FxSharedUtils.get(attributes, "parentTypeName", null);
             useInstancePermissions = (Boolean) FxSharedUtils.get(attributes, "useInstancePermissions", true);
             usePropertyPermissions = (Boolean) FxSharedUtils.get(attributes, "usePropertyPermissions", false);
             useStepPermissions = (Boolean) FxSharedUtils.get(attributes, "useStepPermissions", true);
             useTypePermissions = (Boolean) FxSharedUtils.get(attributes, "useTypePermissions", true);
             usePermissions = (Boolean) FxSharedUtils.get(attributes, "usePermissions", null);
             languageMode = (LanguageMode) FxSharedUtils.get(attributes, "languageMode", LanguageMode.Multiple);
             typeMode = (TypeMode) FxSharedUtils.get(attributes, "typeMode", TypeMode.Content);
             trackHistory = (Boolean) FxSharedUtils.get(attributes, "trackHistory", false);
             historyAge = (Long) FxSharedUtils.get(attributes, "historyAge", 1L);
             maxVersions = (Long) FxSharedUtils.get(attributes, "maxVersions", -1L);
 
             return this;
         }
 
         /**
          * Sets the attributes of a given type
          *
          * @param type the FxTypeEdit instance whose attributes will be changed
          */
         void setTypeAttributes(FxTypeEdit type) {
             if (attributes.containsKey("languageMode"))
                 type.setLanguage(languageMode);
             if (attributes.containsKey("typeMode"))
                 type.setMode(typeMode);
             if (attributes.containsKey("trackHistory"))
                 type.setTrackHistory(trackHistory);
             if (attributes.containsKey("maxVersions"))
                 type.setMaxVersions(maxVersions);
             if (attributes.containsKey("useInstancePermissions"))
                 type.setUseInstancePermissions(useInstancePermissions);
             if (attributes.containsKey("usePropertyPermissions"))
                 type.setUsePropertyPermissions(usePropertyPermissions);
             if (attributes.containsKey("useStepPermissions"))
                 type.setUseStepPermissions(useStepPermissions);
             if (attributes.containsKey("useTypePermissions"))
                 type.setUseTypePermissions(useTypePermissions);
             if (attributes.containsKey("usePermissions"))
                 if (usePermissions != null && !usePermissions) {
                     type.setPermissions((byte) 0);
                 }
             if (attributes.containsKey("icon"))
                 type.setIcon(icon);
             if (attributes.containsKey("trackHistory") && trackHistory) {
                 type.setHistoryAge(historyAge);
             }
         }
 
         /**
          * Retrieve the attributes of elements (FxGroups / FxProperties and their assignments)
          *
          * @return returns the AttributeMapper itself (for chained calls)
          */
         AttributeMapper setElementAttributes() {
             dataType = value != null ? (FxDataType) value : (FxDataType) FxSharedUtils.get(attributes, "dataType", FxDataType.String1024);
 
             multiplicity = FxMultiplicity.MULT_0_1; // default
             if (attributes.get("multiplicity") instanceof FxMultiplicity) {
                 multiplicity = (FxMultiplicity) FxSharedUtils.get(attributes, "multiplicity", FxMultiplicity.MULT_0_1);
             } else if (attributes.get("multiplicity") instanceof String) {
                 final String mult = (String) attributes.get("multiplicity");
                 int min = Integer.parseInt(StringUtils.strip(mult.substring(0, mult.indexOf(","))));
                 int max = Integer.parseInt(StringUtils.strip(mult.substring(mult.indexOf(",") + 1, mult.length())));
                 multiplicity = new FxMultiplicity(min, max);
             }
             // elementName will be reassigned to "alias" if no alias is given
             elementName = (String) FxSharedUtils.get(attributes, "name", StringUtils.capitalize(structureName));
             label = (FxString) FxSharedUtils.get(attributes, "label", new FxString(elementName));
 
             if (attributes.containsKey("description") && !attributes.containsKey("label")) // overwrite the label if given
                 label = (FxString) FxSharedUtils.get(attributes, "description", new FxString(elementName));
 
             hint = (FxString) FxSharedUtils.get(attributes, "hint", new FxString(""));
 
             acl = CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()); // default
             if (attributes.get("acl") instanceof ACL) {
                 acl = (ACL) FxSharedUtils.get(attributes, "acl", CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()));
             } else if (attributes.get("acl") instanceof String) {
                 final String aclString = (String) FxSharedUtils.get(attributes, "acl", "Default Structure ACL");
                 acl = CacheAdmin.getEnvironment().getACL(aclString);
             }
 
             if (attributes.get("assignment") instanceof String)
                 assignment = (String) FxSharedUtils.get(attributes, "assignment", null);
             else if (attributes.get("assignment") instanceof FxAssignment) {
                 final FxAssignment a = (FxAssignment) FxSharedUtils.get(attributes, "assignment", null);
                 assignment = a != null ? a.getXPath() : null;
             }
 
             if (assignment != null && attributes.containsKey("createSubAssignments")) {
                 createSubAssignments = (Boolean) FxSharedUtils.get(attributes, "createSubAssignments", false);
             }
 
             if (attributes.get("referencedType") instanceof String) {
                 final String typeName = (String) FxSharedUtils.get(attributes, "referencedType", null);
                 referencedType = CacheAdmin.getEnvironment().getType(typeName);
             } else if (attributes.get("referencedType") instanceof FxType) {
                 referencedType = (FxType) FxSharedUtils.get(attributes, "referencedType", null);
             }
 
             if (attributes.get("referencedList") instanceof String) {
                 final String listName = (String) FxSharedUtils.get(attributes, "referencedList", null);
                 referencedList = CacheAdmin.getEnvironment().getSelectList(listName);
             } else if (attributes.get("referencedList") instanceof FxSelectList) {
                 referencedList = (FxSelectList) attributes.get("referencedList");
             }
 
             groupMode = GroupMode.AnyOf; // default
             if (attributes.get("groupMode") instanceof String) {
                 final String groupModeString = (String) FxSharedUtils.get(attributes, "groupMode", "AnyOf");
                 groupMode = groupModeString.contains("AnyOf") ? GroupMode.AnyOf : GroupMode.OneOf;
             } else if (attributes.get("groupMode") instanceof GroupMode) {
                 groupMode = (GroupMode) FxSharedUtils.get(attributes, "groupMode", GroupMode.AnyOf);
             }
 
             alias = (String) FxSharedUtils.get(attributes, "alias", elementName);
             defaultMultiplicity = (Integer) FxSharedUtils.get(attributes, "defaultMultiplicity", 1);
             defaultValue = (FxValue) FxSharedUtils.get(attributes, "defaultValue", null);
             overrideMultilang = (Boolean) FxSharedUtils.get(attributes, "overrideMultilang", false);
             multilang = (Boolean) FxSharedUtils.get(attributes, "multilang", false);
             overrideACL = (Boolean) FxSharedUtils.get(attributes, "overrideACL", true);
             overrideMultiplicity = (Boolean) FxSharedUtils.get(attributes, "overrideMultiplicity", false);
             overrideInOverview = (Boolean) FxSharedUtils.get(attributes, "overrideInOverview", false);
             overrideMaxLength = (Boolean) FxSharedUtils.get(attributes, "overrideMaxLength", false);
             overrideMultiline = (Boolean) FxSharedUtils.get(attributes, "overrideMultiline", false);
             overrideSearchable = (Boolean) FxSharedUtils.get(attributes, "overrideSearchable", false);
             overrideHTMLEditor = (Boolean) FxSharedUtils.get(attributes, "overrideUseHtmlEditor", false);
             maxLength = (Integer) FxSharedUtils.get(attributes, "maxLength", -1);
             searchable = (Boolean) FxSharedUtils.get(attributes, "searchable", true);
             inOverview = (Boolean) FxSharedUtils.get(attributes, "inOverview", false);
             useHTMLEditor = (Boolean) FxSharedUtils.get(attributes, "useHtmlEditor", false);
             multiLine = (Boolean) FxSharedUtils.get(attributes, "multiline", false);
             defaultLanguage = (Long) FxSharedUtils.get(attributes, "defaultLanguage", 1L);
             enabled = (Boolean) FxSharedUtils.get(attributes, "enabled", true);
 
             fullTextIndexed = (Boolean) FxSharedUtils.get(attributes, "fullTextIndexed", true);
             autoUniquePropertyName = (Boolean) FxSharedUtils.get(attributes, "autoUniquePropertyName", true);
             uniqueMode = (UniqueMode) FxSharedUtils.get(attributes, "uniqueMode", UniqueMode.Type);
 
             return this;
         }
 
         /**
          * Make changes to an existing FxPropertyAssignmentEdit instance
          *
          * @param pa the FxPropertyAssignment t.b. changed
          * @throws FxApplicationException on errors
          */
         void setPropertyAssignmentAttributes(FxPropertyAssignmentEdit pa) throws FxApplicationException {
             if (attributes.containsKey("label"))
                 pa.setLabel(label);
             if (attributes.containsKey("defaultMultiplicity"))
                 pa.setDefaultMultiplicity(defaultMultiplicity);
             if (attributes.containsKey("alias"))
                 pa.setAlias(alias);
             if (attributes.containsKey("hint"))
                 pa.setHint(hint);
             if (attributes.containsKey("acl"))
                 pa.setACL(acl);
             if (attributes.containsKey("defaultValue"))
                 pa.setDefaultValue(defaultValue);
             if (attributes.containsKey("multiplicity"))
                 pa.setMultiplicity(multiplicity);
             if (attributes.containsKey("defaultLanguage"))
                 pa.setDefaultLanguage(defaultLanguage);
             if (attributes.containsKey("enabled"))
                 pa.setEnabled(enabled);
             if (attributes.containsKey("multiline)"))
                 pa.setMultiLine(multiLine);
             if (attributes.containsKey("inOverview"))
                 pa.setInOverview(inOverview);
             if (attributes.containsKey("useHtmlEditor"))
                 pa.setUseHTMLEditor(useHTMLEditor);
             if (attributes.containsKey("maxLength"))
                 pa.setMaxLength(maxLength);
             if (attributes.containsKey("multilang"))
                 pa.setMultiLang(multilang);
 
             // set non-generic property-assignment options
             for (Object oEntry : attributes.entrySet()) {
                 final Map.Entry entry = (Map.Entry) oEntry;
                 final Object key = entry.getKey();
                 if (isNonAssignmentOptionKey((String) key))
                     continue;
                 final Object optionValue = entry.getValue();
                 final String optionKey = ((String) key).toUpperCase();
                 // set generic options
                 if (optionValue instanceof Boolean) {
                     pa.setOption(optionKey, (Boolean) optionValue);
                 } else if (optionValue != null) {
                     pa.setOption(optionKey, optionValue.toString());
                 }
             }
         }
 
         /**
          * Make changes to an FxGroupAssignmentEdit instance
          *
          * @param ga the FxGroupAssignmentEdit instance t.b. changed
          * @throws FxApplicationException on errors
          */
         void setGroupAssignmentAttributes(FxGroupAssignmentEdit ga) throws FxApplicationException {
             ga.setDefaultMultiplicity(defaultMultiplicity);
             if (attributes.containsKey("label"))
                 ga.setLabel(label);
             if (attributes.containsKey("hint"))
                 ga.setHint(hint);
             if (attributes.containsKey("alias"))
                 ga.setAlias(alias);
             if (attributes.containsKey("multiplicity"))
                 ga.setMultiplicity(multiplicity);
             if (attributes.containsKey("enabled"))
                 ga.setEnabled(enabled);
             if (attributes.containsKey("groupMode"))
                 ga.setMode(groupMode);
 
             // set options
             for (Object oEntry : attributes.entrySet()) {
                 final Map.Entry entry = (Map.Entry) oEntry;
                 final Object key = entry.getKey();
                 if (isNonPropertyOptionKey((String) key))
                     continue;
                 final Object optionValue = entry.getValue();
                 final String optionKey = ((String) key).toUpperCase();
 
                 // set generic options
                 if (optionValue instanceof Boolean) {
                     ga.setOption(optionKey, (Boolean) optionValue);
                 } else if (optionValue != null) {
                     ga.setOption(optionKey, optionValue.toString());
                 }
             }
         }
 
         /**
          * Assign property attributes
          *
          * @param property the instance of FxPropertyEdit t.b. changed
          */
         void setPropertyAttributes(FxPropertyEdit property) {
             property.setAutoUniquePropertyName(autoUniquePropertyName);
             property.setAssignmentDefaultMultiplicity(defaultMultiplicity);
             // optional
             if (attributes.containsKey("fullTextIndexed"))
                 property.setFulltextIndexed(fullTextIndexed);
             if (attributes.containsKey("multilang"))
                 property.setMultiLang(multilang);
             if (attributes.containsKey("overrideMultilang"))
                 property.setOverrideMultiLang(overrideMultilang);
             if (attributes.containsKey("overrideACL"))
                 property.setOverrideACL(overrideACL);
             if (attributes.containsKey("overrideMultiplicity"))
                 property.setOverrideMultiplicity(overrideMultiplicity);
             if (attributes.containsKey("overrideInOverview"))
                 property.setOverrideOverview(overrideInOverview);
             if (attributes.containsKey("overrideMultiline"))
                 property.setOverrideMultiLine(overrideMultiline);
             if (attributes.containsKey("overrideSearchable"))
                 property.setOverrideSearchable(overrideSearchable);
             if (attributes.containsKey("overrideUseHtmlEditor"))
                 property.setOverrideHTMLEditor(overrideHTMLEditor);
             if (attributes.containsKey("searchable"))
                 property.setSearchable(searchable);
             if (attributes.containsKey("inOverview"))
                 property.setInOverview(inOverview);
             if (attributes.containsKey("useHtmlEditor"))
                 property.setUseHTMLEditor(useHTMLEditor);
             if (attributes.containsKey("multiline"))
                 property.setMultiLine(multiLine);
             if (attributes.containsKey("maxLength")) {
                 try {
                     property.setMaxLength(maxLength);
                 } catch (FxInvalidParameterException e) {
                     throw e.asRuntimeException();
                 }
             }
             if (attributes.containsKey("overrideMaxLength"))
                 property.setOverrideMaxLength(overrideMaxLength);
 
             if (defaultValue != null)
                 property.setDefaultValue(defaultValue);
             if (attributes.containsKey("uniqueMode"))
                 property.setUniqueMode(uniqueMode);
             if (attributes.containsKey("referencedType"))
                 property.setReferencedType(referencedType);
             if (attributes.containsKey("referencedList"))
                 property.setReferencedList(referencedList);
 
             for (Object oEntry : attributes.entrySet()) {
                 final Map.Entry entry = (Map.Entry) oEntry;
                 final Object key = entry.getKey();
                 if (isNonPropertyOptionKey((String) key))
                     continue;
                 final Object optionValue = entry.getValue();
                 final String optionKey = ((String) key).toUpperCase();
                 // set generic options
                 if (optionValue instanceof Boolean) {
                     property.setOption(optionKey, true, (Boolean) optionValue);
                 } else if (optionValue != null) {
                     property.setOption(optionKey, true, optionValue.toString());
                 }
             }
         }
 
         /**
          * Make changes to an FxGroupEdit instance
          *
          * @param ge the FxGroup t.b. changed
          * @throws FxApplicationException on errors
          */
         void setGroupAttributes(FxGroupEdit ge) throws FxApplicationException {
             ge.setAssignmentDefaultMultiplicity(defaultMultiplicity);
             if (attributes.containsKey("groupMode"))
                 ge.setAssignmentGroupMode(groupMode);
 
             // set options
             for (Object oEntry : attributes.entrySet()) {
                 final Map.Entry entry = (Map.Entry) oEntry;
                 final Object key = entry.getKey();
                 if (isNonPropertyOptionKey((String) key))
                     continue;
                 final Object optionValue = entry.getValue();
                 final String optionKey = ((String) key).toUpperCase();
 
                 // set generic options
                 if (optionValue instanceof Boolean) {
                     ge.setOption(optionKey, true, (Boolean) optionValue);
                 } else if (optionValue != null) {
                     ge.setOption(optionKey, true, optionValue.toString());
                 }
             }
 
         }
     }
 }
