 /***************************************************************
  *  This file is part of the [fleXive](R) framework.
  *
  *  Copyright (c) 1999-2010
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
 package com.flexive.shared.content;
 
 import com.flexive.shared.CacheAdmin;
 import com.flexive.shared.FxContext;
 import com.flexive.shared.XPathElement;
 import com.flexive.shared.exceptions.*;
 import com.flexive.shared.security.UserTicket;
 import com.flexive.shared.structure.*;
 import com.flexive.shared.value.FxReference;
 import com.flexive.shared.value.FxValue;
 import com.google.common.base.Predicate;
 import com.google.common.collect.Collections2;
 import com.google.common.collect.Lists;
 
 import java.util.*;
 
 /**
  * FxData extension for groups
  *
  * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
  */
 public class FxGroupData extends FxData {
     private static final long serialVersionUID = 133412774300450631L;
     private List<FxData> data;
     private FxValueChangeListener changeListener = null;
 
     public FxGroupData(String xpPrefix, String alias, int index, String xPath, String xPathFull, int[] indices,
                        long assignmentId, FxMultiplicity assignmentMultiplicity, int pos,
                        FxGroupData parent, List<FxData> data, boolean systemInternal) throws FxInvalidParameterException {
         super(xpPrefix, alias, index, xPath, xPathFull, indices, assignmentId, assignmentMultiplicity,
                 pos, parent, systemInternal);
         this.data = data;
     }
 
     /**
      * Copy constructor.
      *
      * @param other     the group data instance to copy
      * @param parent    the new parent
      * @since 3.1.7
      */
     public FxGroupData(FxGroupData other, FxGroupData parent) {
         super(other, parent);
         this.changeListener = other.changeListener;
         this.data = Lists.newArrayListWithCapacity(other.data.size());
         for (FxData org : other.data) {
             data.add(org.copy(this));
         }
         this.changeListener = other.changeListener;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean isProperty() {
         return false;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean isGroup() {
         return true;
     }
 
     /**
      * Get the groups assignment
      *
      * @return FxGroupAssignment
      * @since 3.1
      */
     public FxGroupAssignment getGroupAssignment() {
         return (FxGroupAssignment) getAssignment();
     }
 
     /**
      * Get all child entries for this group
      *
      * @return child entries
      */
     public List<FxData> getChildren() {
         return data;
     }
 
     /**
      * Get all child entries for this group, excluding all internal properties.
      *
      * @return child entries, excluding all internal properties.
      * @since 3.1
      */
     public Collection<FxData> getChildrenWithoutInternal() {
         return Collections2.filter(data, FILTER_REMOVE_INTERNAL);
     }
     
     private static final RemoveInternalFilter FILTER_REMOVE_INTERNAL = new RemoveInternalFilter();
     
     private static class RemoveInternalFilter implements Predicate<FxData> {
         public boolean apply(FxData t) {
             return t != null && !t.isSystemInternal();
         }
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean isEmpty() {
         for (FxData curr : this.getChildren())
             if (!curr.isEmpty())
                 return false;
         return true;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setEmpty() {
         if(this.isRootGroup())
             return; //not possible for the root group (yet)

        // create an empty groups
         FxGroupData empty = (FxGroupData)this.getAssignment().createEmptyData(this.getParent(), this.getIndex(), this.getPos());

        // fix child parents
        for (FxData emptyData : empty.data) {
            emptyData.parent = this;
        }

        // replace group children
         this.data = empty.data;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean isRequiredPropertiesPresent() {
         for (FxData curr : this.getChildren()) {
             if (curr.isRequiredPropertiesPresent()) {
                 return true;
             }
         }
         return this.getAssignmentMultiplicity().isRequired();
 //        return false;
     }
 
     /**
      * Return true if this content has at least one multilingual property set.
      *
      * @return true if this content has at least one multilingual property set.
      * @since 3.1
      */
     public boolean isHasMultiLangProperty() {
         for (FxData fxData : data) {
             if (fxData.isGroup()) {
                 if (((FxGroupData) fxData).isHasMultiLangProperty()) {
                     return true;
                 }
             } else {
                 if (((FxPropertyData) fxData).getPropertyAssignment().isMultiLang()) {
                     return true;
                 }
             }
         }
         return false;
     }
 
     /**
      * Helper to create a virtual root group
      *
      * @param xpPrefix XPath prefix like "FxType name[@pk=..]"
      * @return virtual root group
      * @throws FxInvalidParameterException on errors
      */
     public static FxGroupData createVirtualRootGroup(String xpPrefix) throws FxInvalidParameterException {
         // note: multiplicity and other settings are no longer used by FxGroupData, they are supplied
         // by the virtual root assignment created in FxData#getAssignment
         return new FxGroupData(xpPrefix, "", 1, "/", "/", new int[0], -1, new FxMultiplicity(1, 1), -1, null, new ArrayList<FxData>(), false);
     }
 
     /**
      * Move a child element identified by its alias and multiplicity by delta positions within this group
      * If delta is Integer.MAX_VALUE the data will always be placed at the bottom,
      * Integer.MIN_VALUE will always place it at the top.
      *
      * @param xp    element to move
      * @param delta delta positions to move
      */
     public void moveChild(XPathElement xp, int delta) {
         if (delta == 0 || data.size() < 2)
             return;
         int currPos = -1, newPos;
         FxData child = null;
         List<String> aliases = new ArrayList<String>((int) (data.size() * 0.7));
         int topSysInternalPos = -1;
         for (int i = 0; i < data.size(); i++) {
             child = data.get(i);
             if (child.isSystemInternal() && topSysInternalPos < child.getPos())
                 topSysInternalPos = child.getPos();
             if (!aliases.contains(child.getAlias()))
                 aliases.add(child.getAlias());
             if (child.getXPathElement().equals(xp)) {
                 currPos = i;
                 break;
             }
         }
         if (currPos == -1)
             //noinspection ThrowableInstanceNeverThrown
             throw new FxNotFoundException("ex.xpath.alias.notFound", xp).asRuntimeException();
         if (delta < 0 && topSysInternalPos >= 0 && topSysInternalPos >= (currPos + delta) && !child.isSystemInternal())
             return; //do not position a non-sysinternal element before a sysinternal one to remain consistency in visual editors, etc.
 
         newPos = currPos + delta;
         if (newPos < 0)
             newPos = 0; //move to top
         if (newPos >= data.size())
             newPos = data.size() - 1; //move to bottom
         data.remove(currPos);
         data.add(newPos, child);
         //resync positions and indices
         for (int i = 0; i < data.size(); i++) {
             data.get(i).setPos(i);
             if (aliases.contains(data.get(i).getAlias())) {
                 //make sure to sync the multiplicity for each alias only once
                 data.get(i).compact();
                 aliases.remove(data.get(i).getAlias());
             }
             valueChanged(data.get(i).getXPathFull(), FxValueChangeListener.ChangeType.Move);
         }
 
     }
 
     /**
      * Add a child FxData at the correct position
      *
      * @param child FxData to add
      * @return the child
      */
     public synchronized FxData addChild(FxData child) {
         if (data.contains(child)) //TODO: change to containsChild()?
             return child;
         if (hasChangeListener()) {
             if (child instanceof FxGroupData)
                 ((FxGroupData) child).setChangeListener(this.changeListener, true);
             else {
                 ((FxPropertyData) child).getValue().setChangeListener(this.changeListener);
                 if (!((FxPropertyData) child).getValue().isEmpty())
                     valueChanged(child.getXPathFull(), FxValueChangeListener.ChangeType.Add);
             }
         }
         int pos = (data.size() == 0 ? 0 : child.getPos());
         switch (child.getPos()) {
             case POSITION_TOP:
                 pos = 0;
                 break;
             case POSITION_BOTTOM:
                 pos = data.size();
                 break;
         }
         if (!child.isSystemInternal() && pos < 20 && child.getParent().isRootGroup())
             pos = 20;
         child.setPos(pos);
         //check if the position is taken (if so move the child and successors at the given position one place down)
         for (int i = 0; i < data.size(); i++) {
             if (data.get(i).getPos() < pos)
                 continue;
             if (data.get(i).getPos() > pos) {
                 if (i > 0 && data.get(i - 1).getPos() < pos)
                     data.add(i, child);
                 else
                     data.add((i - 1 < 0 ? 0 : i - 1), child);
                 return child;
             }
             if (data.get(i).getPos() == pos) {
                 //move successors down
                 int lastPos = pos + 1;
                 data.get(i).setPos(lastPos);
                 for (int j = i + 1; j < data.size(); j++) {
                     if (!(data.get(j).getPos() > lastPos))
                         data.get(j).setPos(lastPos + 1);
                     lastPos = data.get(j).getPos();
                 }
                 data.add(i, child);
                 return child;
             }
         }
         data.add(child); //add at bottom
         return child;
     }
 
     /**
      * Add an and FxData entry with the given XPath to this group (must be a direct child of this group, no nesting allowed!).
      * Empty groups consist of empty but preinitialized elements!
      *
      * @param xPath XPath to add to this group (must be a direct child of this group, no nesting allowed!)
      * @param pos   position in same hierarchy level
      * @return the added data element
      */
     public FxData addEmptyChild(String xPath, int pos) {
         FxType type;
         List<FxAssignment> childAssignments;
         xPath = XPathElement.toXPathMult(xPath);
         if (this.isRootGroup()) {
             type = CacheAdmin.getEnvironment().getAssignment(this.getChildren().get(0).getAssignmentId()).getAssignedType();
             childAssignments = type.getConnectedAssignments("/");
         } else {
             type = CacheAdmin.getEnvironment().getAssignment(this.getAssignmentId()).getAssignedType();
             childAssignments = ((FxGroupAssignment) type.getAssignment(this.getXPath())).getAssignments();
         }
         if (childAssignments != null && childAssignments.size() > 0) {
             FxGroupAssignment thisGroup = childAssignments.get(0).getParentGroupAssignment();
             boolean isOneOf = false;
             if (thisGroup != null)
                 isOneOf = thisGroup.getMode() == GroupMode.OneOf;
 
             String xPathNoMult = type.getName().toUpperCase() + XPathElement.stripType(XPathElement.toXPathNoMult(xPath));
             for (FxAssignment as : childAssignments) {
                 if (as.getXPath().equals(xPathNoMult)) {
                     if (isOneOf) {
                         //check if other assignments exist
                         for (FxData child : this.getChildren()) {
                             if (child.getAssignmentId() != as.getId())
                                 throw new FxCreateException("ex.content.xpath.group.oneof", as.getXPath(), this.getXPathFull()).setAffectedXPath(xPath, FxContentExceptionCause.GroupOneOfViolated).asRuntimeException();
                         }
                     }
                     int index = XPathElement.lastElement(xPath).getIndex();
                     if (as.getMultiplicity().isValidMax(index)) {
                         for (FxData child : getChildren()) {
                             if (child.isGroup() && child.getXPathFull().equals(xPath)) {
                                 throw new FxInvalidParameterException("pos", "ex.content.xpath.group.exists", xPath).asRuntimeException();
                             }
                         }
                         return this.addChild(as.createEmptyData(this, index).setPos(pos));
                     } else
                         throw new FxInvalidParameterException("pos", "ex.content.xpath.index.invalid", index, as.getMultiplicity(), this.getXPath()).setAffectedXPath(xPath, FxContentExceptionCause.InvalidIndex).asRuntimeException();
                 }
             }
         }
         throw new FxNotFoundException("ex.content.xpath.add.notFound", xPath).asRuntimeException();
     }
 
 
     /**
      * Apply the multiplicity to XPath and children if its a group
      */
     @Override
     protected void applyIndices() {
         try {
             List<XPathElement> elements = XPathElement.split(this.getXPathFull());
             if (elements.get(elements.size() - 1).getIndex() == this.getIndex())
                 return;
             int pos = elements.size() - 1;
             elements.get(pos).setIndex(this.getIndex());
             setXPathFull(XPathElement.toXPath(elements));
             if (this.getChildren() != null)
                 _changeIndex(this.getChildren(), pos, this.getIndex());
         } catch (FxInvalidParameterException e) {
             throw e.asRuntimeException();
         }
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void setXPathFull(String xpathFull) {
         if (xpathFull.equals(this.XPathFull)) {
             return;
         }
         this.XPathFull = xpCached(xpathFull);
         this.indices = XPathElement.getIndices(this.XPathFull);
     }
 
 
     /**
      * 'Fix' the indices of children after they have been added to reflect the parent groups index in
      * their XPath
      * Don't work in the root of a type
      */
     public void fixChildIndices() {
         try {
             List<XPathElement> elements = XPathElement.split(this.getXPathFull());
             int pos = elements.size() - 1;
             if (this.getChildren() != null)
                 _changeIndex(this.getChildren(), pos, this.getIndex());
         } catch (FxInvalidParameterException e) {
             throw e.asRuntimeException();
         }
     }
 
     /**
      * Recursively change the index for an element in the XPath of all children and their sub groups/properties
      *
      * @param children array of FxData to process
      * @param pos      position of the element to change in the XPath
      * @param index    the index to apply
      * @throws FxInvalidParameterException on errors with XPath composition
      */
     private void _changeIndex(List<FxData> children, int pos, int index) throws FxInvalidParameterException {
         List<XPathElement> elements;
         for (FxData curr : children) {
             // we need to get the XPathFull to have the indices
             curr.setXPathFull(XPathElement.changeIndex(curr.getXPathFull(), pos, index));
             if (curr instanceof FxGroupData)
                 _changeIndex(((FxGroupData) curr).getChildren(), pos, index);
         }
     }
 
     /**
      * Remove all empty entries of this group that are not required
      *
      * @see #removeEmptyEntries(boolean)
      */
     public void removeEmptyEntries() {
         removeEmptyEntries(false);
     }
 
     /**
      * Remove all empty entries of this group
      *
      * @param includeRequired include entries that are required (but empty)?
      */
     public void removeEmptyEntries(boolean includeRequired) {
         // a list to store all entries to remove
         List<FxData> toRemove = new ArrayList<FxData>(50);
         // a map to store all ids and count not removed
         // key : id ; value : amount
         Map<Long, Integer> notRemoved = new HashMap<Long, Integer>();
         // a map to store all removed items to compact them, key = assignmentID ; value = the data with the ID
         Map<Long, FxData> toCompact = new HashMap<Long, FxData>();
         // a linked list with the groups to check, the first element is always removed, and groups in groups are added to the end
         // linked list because it has O(1) at adding to the end and removing from the beginning, while an Arraylist must copy all following elements if the first is removed
         List<FxGroupData> currentGroups = new LinkedList<FxGroupData>();
         currentGroups.add(this);
         // indicates if we need to remove all elements 
         boolean removeAll;
         // while there is any unchecked group
         while (currentGroups.size() > 0) {
             FxGroupData currentGroup= currentGroups.remove(0);
             toRemove.clear();
             removeAll = true;
             // First we need to check if we could remove an element (then add to the toRemove set) else add all other groups to the currentGroups List
             List<FxData> currentData = currentGroup.data;
             for (FxData curr : currentData) {
                 boolean propCheck = false; 
                 if (curr.isProperty()) {
                     FxPropertyData prop = (FxPropertyData) curr;
                     propCheck |= prop.isContainsDefaultValue();
                 }
                 // if we got something to remove, just put it in to the list otherwise set removeAll to false
                 if ((curr.isEmpty() || propCheck)
                         // for groups, don't remove them when they contain required properties
                         && ((curr.isGroup() && !curr.isRequiredPropertiesPresent())
                         || includeRequired || curr.isRemoveable())
                         && !curr.isSystemInternal()) {
                     toRemove.add(curr);
                 } else {
                     removeAll = false;
                     if (curr instanceof FxGroupData) {
                         currentGroups.add((FxGroupData) curr);
                     }
                     if (!curr.getAssignmentMultiplicity().isOptional() && !curr.isSystemInternal()) {
                         long id = curr.getAssignmentId();
                         Integer amount = notRemoved.get(id);
                         if (amount == null) {
                             amount = 0;
                         }
                         amount++;
                         notRemoved.put(id, amount);
                     }
                 }
                 if (!curr.getAssignmentMultiplicity().isOptional()) {
                     removeAll = false;
                 }
             }
             /*if we need to remove all elements of a group and the current group is optional,
               then we don't even need to remove all elements and compact,
               just remove the current group (and clear it) from the parrent and let GC do its job*/
             if (removeAll) {
                 currentData.clear();
                 if (currentGroup.getAssignmentMultiplicity().isOptional()) {
                     FxGroupData currentParent = currentGroup.getParent();
                     if (currentParent != null) {
                         currentParent.data.remove(currentGroup);
                     }
                 }
                 continue;
             }
             toCompact.clear();
             // if we don't need to remove all data, just remove what we need to remove and compact
             for (FxData curr : toRemove) {
                 // only check non optional fields that are removed
                 if (!curr.getAssignmentMultiplicity().isOptional()) {
                     long id = curr.getAssignmentId();
                     Integer amount = notRemoved.get(id);
                     if (amount == null) {
                         amount = 0;
                     }
                     // only remove something if we got at least as many elements as we need
                     if (amount < curr.getAssignmentMultiplicity().getMin()) {
                         amount++;
                         notRemoved.put(id, amount);
 
                         continue;
                     }
                 }
                 currentData.remove(curr);
                 // just store any removed item with its ID as key in a map, and only do this if we don't removed all the elements
                 toCompact.put(curr.getAssignmentId(), curr);
 //                curr.compact();
             }
             // compact after all elements have been removed, and compact multiple data with same ID only once            
             for (FxData curr : toCompact.values()) {
                 curr.compact();
             }
         }
     }
 
     /*
      * Remove all empty entries of this group
      *
      * @param includeRequired include entries that are required (but empty)?
      *
     public void removeEmptyEntriesOLD(boolean includeRequired) {
         for (FxData curr : data) {
             if ((curr.isEmpty() || (curr.isProperty() && ((FxPropertyData) curr).isContainsDefaultValue()))
                     // for groups, don't remove them when they contain required properties
                     && ((curr.isGroup() && !curr.isRequiredPropertiesPresent())
                     || includeRequired || curr.isRemoveable()) && !curr.isSystemInternal()) {
                 data.remove(curr);
                 for (FxData com : data) {
                     if (com.getAssignmentId() == curr.getAssignmentId()) {
                         com.compact();
                         break;
                     }
                 }
                 removeEmptyEntriesOLD(includeRequired);
                 return;
             } else if (curr instanceof FxGroupData) {
                 ((FxGroupData) curr).removeEmptyEntriesOLD(includeRequired);
                 if (((FxGroupData) curr).data.size() == 0)
                     data.remove(curr);
             }
         }
     }*/
 
     /**
      * Synchronize positions closing gaps optionally including sub groups
      *
      * @param includeSubGroups close gaps for subgroups as well?
      */
     public void compactPositions(boolean includeSubGroups) {
         List<FxGroupData> todos = new LinkedList<FxGroupData>();
         int pos = 0;
         for (FxData curr : data) {
             curr.setPos(pos++);
             if (includeSubGroups && curr instanceof FxGroupData)
                 todos.add((FxGroupData) curr);
         }
 
         while (todos.size() > 0) {
             pos = 0;
             for (FxData curr : todos.remove(0).data) {
                 curr.setPos(pos++);
                 if (curr instanceof FxGroupData)
                     todos.add((FxGroupData) curr);
             }
         }
     }
 
     /**
      * Synchronize positions closing gaps optionally including sub groups
      *
      * @param includeSubGroups close gaps for subgroups as well?
      *
     public void compactPositionsOLD(boolean includeSubGroups) {
         int pos = 0;
         for (FxData curr : data) {
             curr.setPos(pos++);
             if (includeSubGroups && curr instanceof FxGroupData)
                 ((FxGroupData) curr).compactPositions(true);
         }
     }*/
 
     /**
      * Get the root group for this group
      *
      * @return root group
      */
     private FxGroupData getRootGroup() {
         FxGroupData root = this;
         while (root.getParent() != null)
             root = root.getParent();
         return root;
     }
 
     /**
      * Is this group the root group?
      *
      * @return if this group is the root group
      */
     public boolean isRootGroup() {
         return this.getAssignmentId() == -1;
     }
 
     /**
      * Get the group denoted by the given XPath
      *
      * @param xPath requested XPath for the group
      * @return FxGroupData
      */
     @SuppressWarnings({"ThrowableInstanceNeverThrown"})
     public FxGroupData getGroup(String xPath) {
         FxGroupData root = getRootGroup();
         if ("/".equals(xPath))
             return root;
         List<XPathElement> elements = XPathElement.split(xPath);
 
         FxGroupData found = null;
         List<FxData> currGroup = root.getChildren();
         for (XPathElement e : elements) {
             found = null;
             for (FxData curr : currGroup) {
                 if (curr instanceof FxGroupData && curr.getXPathElement().equals(e)) {
                     found = (FxGroupData) curr;
                     currGroup = found.getChildren();
                     break;
                 }
             }
             if (found == null)
                 throw new FxNotFoundException("ex.content.xpath.notFound", xPath).asRuntimeException();
         }
         if (found == null)
             throw new FxNotFoundException("ex.content.xpath.notFound", xPath).asRuntimeException();
         return found;
     }
 
     /**
      * Get the FxGroupData entry for the given group assignment.
      *
      * @param assignmentId  the group assignment ID
      * @return FxGroupData  entry for the given assignment
      * @throws FxRuntimeException   if the assignment was not found
      * @since 3.1.4
      */
     public FxGroupData getGroup(long assignmentId) {
         if (getAssignmentId() == assignmentId) {
             return this;
         }
         final FxGroupData result = findGroup(assignmentId);
         if (result != null) {
             return result;
         } else {
             throw new FxNotFoundException(
                     "ex.content.assignment.notFound",
                     CacheAdmin.getEnvironment().getAssignment(assignmentId).getXPath()
             ).asRuntimeException();
         }
     }
 
     private FxGroupData findGroup(long assignmentId) {
         for (FxData child : data) {
             if (child.isGroup()) {
                 if (child.getAssignmentId() == assignmentId) {
                     return (FxGroupData) child;
                 } else {
                     // recurse
                     final FxGroupData result = ((FxGroupData) child).findGroup(assignmentId);
                     if (result != null) {
                         return result;
                     }
                 }
             }
         }
         return null;
     }
 
     /**
      * Return true if the content has at least one group.
      *
      * @return true if the content has at least one group.
      * @since 3.1
      */
     public boolean isHasGroups() {
         for (FxData data : this.data) {
             if (data.isGroup()) {
                 return true;
             }
         }
         return false;
     }
 
     /**
      * Add a property at the given XPath location, removing eventually existing properties.
      * The group for this property has to exist already!
      *
      * @param xPath      requested XPath
      * @param assignment assignment of the property
      * @param value      value
      * @param pos        position
      */
     public void addProperty(String xPath, FxPropertyAssignment assignment, FxValue value, int pos) {
         FxGroupData parentGroup = this;
         List<XPathElement> elements = XPathElement.split(xPath);
         if (elements.size() > 1) {
             String groupXPath = XPathElement.toXPath(elements.subList(0, elements.size() - 1));
             parentGroup = getGroup(groupXPath);
         }
         int index = elements.get(elements.size() - 1).getIndex();
         FxPropertyData data = new FxPropertyData(this.xpPrefix, assignment.getAlias(), index,
                 assignment.getXPath(), xPath, XPathElement.getIndices(xPath), assignment.getId(), assignment.getProperty().getId(),
                 assignment.getMultiplicity(), pos, parentGroup, value, assignment.isSystemInternal(), assignment.getOption(FxStructureOption.OPTION_MAXLENGTH));
 
         FxData check = parentGroup.containsChild(data.getXPathElement());
         if (check != null)
             parentGroup.data.remove(check);
         parentGroup.addChild(data);
         /*boolean added = false;
         for (int i = 0; i < parentGroup.data.size(); i++) {
             if (parentGroup.data.get(i).getPos() > data.getPos()) {
                 parentGroup.data.add(i, data);
                 added = true;
                 break;
             }
         }
         if (!added) //add at end
             parentGroup.data.add(data);*/
 //            parentGroup.replaceChild(data.getXPathElement(), data);
 //        } else { //TODO: check if adding allowed!
 //            parentGroup.data.add(pos, data);
 //        }
     }
 
     /**
      * Check if a child with the same alias and multiplicity that is not empty exists.
      * No elements of subgroups are checked, just <i>direct</i> childs!
      *
      * @param check XPathElement to check
      * @return FxData or <code>null</code>
      */
     public FxData containsChild(XPathElement check) {
         for (FxData curr : getChildren()) {
             if (curr.getXPathElement().equals(check))
                 return curr;
         }
         return null;
     }
 
     public void replaceChild(XPathElement xpath, FxData data) {
         for (int i = 0; i < this.data.size(); i++) {
             if (this.data.get(i).getXPathElement().equals(xpath)) {
                 this.data.set(i, data);
                 valueChanged(data.getXPathFull(), FxValueChangeListener.ChangeType.Update);
                 return;
             }
         }
     }
 
     /**
      * Add a group entry at the given XPath. Existing entries will stay untouched but position adjusted.
      * If parent groups of this group do not exist, they will be created as well.
      *
      * @param xPath             requested XPath
      * @param fxGroupAssignment the assignment of the group
      * @param pos               position
      * @throws com.flexive.shared.exceptions.FxInvalidParameterException on errors
      * @throws com.flexive.shared.exceptions.FxNotFoundException         on errors
      * @throws com.flexive.shared.exceptions.FxCreateException           on errors
      */
     public void addGroup(String xPath, FxGroupAssignment fxGroupAssignment, int pos) throws FxInvalidParameterException, FxNotFoundException, FxCreateException {
         addGroup(xPath, fxGroupAssignment, pos, false);
     }
 
     /**
      * Add a group entry at the given XPath. Existing entries will stay untouched but position adjusted.
      * If parent groups of this group do not exist, they will be created as well.
      *
      * @param xPath             requested XPath
      * @param fxGroupAssignment the assignment of the group
      * @param pos               position
      * @param onlySystemInternal    when true, only system-internal groups or properties are added
      * @throws FxInvalidParameterException on errors
      * @throws FxNotFoundException         on errors
      * @throws FxCreateException           on errors
      *
      * @since 3.1.7
      */
     public void addGroup(String xPath, FxGroupAssignment fxGroupAssignment, int pos, boolean onlySystemInternal) throws FxInvalidParameterException, FxNotFoundException, FxCreateException {
         if (xPath.endsWith("/"))
             xPath = xPath.substring(0, xPath.length() - 1);
         List<XPathElement> xp = XPathElement.split(xPath);
         XPathElement addy = xp.get(xp.size() - 1);
         FxGroupData currGroup = getRootGroup(), tmp;
 //        System.out.println("adding group(s): " + xPath);
         for (XPathElement curr : xp) {
             if ((tmp = (FxGroupData) currGroup.containsChild(curr)) != null) {
                 currGroup = tmp;
             } else {
                 FxGroupAssignment gaNew = (FxGroupAssignment) fxGroupAssignment.getAssignedType().getAssignment(XPathElement.buildXPath(true, currGroup.getXPath(), curr.getAlias()));
                 FxData gdNew = gaNew.createEmptyData(currGroup, curr.getIndex(), gaNew.getPosition(), onlySystemInternal);
                 //TODO: check if adding allowed here!
 //                System.out.println("creating " + curr + " in " + xPath);
                 if (addy.equals(curr)) {
 //                    System.out.println("creating the actual addy group...");
                     gdNew.setPos(pos);
                 }
                 currGroup.addChild(gdNew);
                 currGroup = currGroup.getGroup(gdNew.getXPathFull());
             }
         }
     }
 
     /**
      * Remove the requested child data and compact indices and positions
      *
      * @param data FxData to remove
      */
     @SuppressWarnings({"ThrowableInstanceNeverThrown"})
     public void removeChild(FxData data) {
         if (!data.isRemoveable())
             throw new FxNoAccessException("ex.content.xpath.remove.invalid", data.getXPathFull()).asRuntimeException();
 
         if (!this.data.remove(data)) //was: if (!data.getParent().data.remove(data))
             throw new FxInvalidParameterException("ex.content.xpath.remove.notFound", data.getXPathFull()).asRuntimeException();
         if(hasChangeListener() && !data.isEmpty())
             valueChanged(data.getXPathFull(), FxValueChangeListener.ChangeType.Remove);
         data.compact();
         compactPositions(false);
     }
 
     /**
      * Remove the requested children and compact indices and positions
      *
      * @param dataList list of FxData to remove
      */
     @SuppressWarnings({"ThrowableInstanceNeverThrown"})
     public void removeChildren(List<FxData> dataList) {
         Map<String, FxData> compactCandidates = new HashMap<String, FxData>(dataList.size());
         for (FxData data : dataList) {
             if (!data.isRemoveable())
                 throw new FxNoAccessException("ex.content.xpath.remove.invalid", data.getXPathFull()).asRuntimeException();
 
             if (!this.data.remove(data))
                 throw new FxInvalidParameterException("ex.content.xpath.remove.notFound", data.getXPathFull()).asRuntimeException();
             valueChanged(data.getXPathFull(), FxValueChangeListener.ChangeType.Remove);
             if (!compactCandidates.containsKey(data.getXPath()))
                 compactCandidates.put(data.getXPath(), data);
         }
         for (FxData compactData : compactCandidates.values())
             compactData.compact();
         compactPositions(false);
     }
 
     /**
      * "Explode" this group by adding all creatable assignments at the bottom
      *
      * @param explodeChildGroups recursively explode all <i>existing</i> child groups?
      */
     public void explode(boolean explodeChildGroups) {
         final boolean isOneOf = !this.isRootGroup() && this.getGroupAssignment().getMode() == GroupMode.OneOf;
         for (String xpath : getCreateableChildren(false)) {
             FxData child = addEmptyChild(xpath, POSITION_BOTTOM);
             if (isOneOf) {
                 if (explodeChildGroups && child.isGroup())
                     ((FxGroupData) child).explode(true);
                 return; //can not "explode" any more children
             }
         }
         if (explodeChildGroups) {
             // explode child groups
             for (FxData child : getChildren()) {
                 if (child.isGroup()) {
                     ((FxGroupData) child).explode(true);
                 }
             }
         }
     }
 
     /**
      * Get a list of child FxData instances (as XPath with full indices) that can be created as children for this group.
      * Readonly or no access properties or groups will not be returned!
      *
      * @param includeExisting include entries for children that already exist but with a new (higher) multiplicity?
      * @return List of XPaths
      */
     public List<String> getCreateableChildren(boolean includeExisting) {
         List<String> ret = new ArrayList<String>(20);
         FxType type;
         List<FxAssignment> childAssignments;
         boolean checkOneOf;
         if (this.isRootGroup()) {
             type = CacheAdmin.getEnvironment().getAssignment(this.getChildren().get(0).getAssignmentId()).getAssignedType();
             childAssignments = type.getConnectedAssignments("/");
             checkOneOf = false;
         } else {
             type = CacheAdmin.getEnvironment().getAssignment(this.getAssignmentId()).getAssignedType();
             FxGroupAssignment thisAssignment = ((FxGroupAssignment) type.getAssignment(this.getXPath()));
             childAssignments = thisAssignment.getAssignments();
             checkOneOf = thisAssignment.getMode() == GroupMode.OneOf;
         }
 
         int count;
         for (FxAssignment as : childAssignments) {
             if (!as.isEnabled() || as.isSystemInternal())
                 continue;
             count = 0;
             if (as instanceof FxPropertyAssignment && type.isUsePropertyPermissions()) {
                 UserTicket ticket = FxContext.getUserTicket();
                 long aclId = ((FxPropertyAssignment) as).getACL().getId();
                 //ignore owner in this checks since owner membership does not allow creation
                 if (!ticket.mayReadACL(aclId, 0) || !ticket.mayCreateACL(aclId, 0) || !ticket.mayEditACL(aclId, 0))
                     continue;
             }
             for (FxData _curr : this.getChildren()) {
                 if (_curr.getAssignmentId() == as.getId())
                     count++;
             }
             if (checkOneOf && count > 0) {
                 //one child exists already -> can only use this one
                 ret.clear();
             }
             try {
                 if (as.getMultiplicity().getMax() > count && (includeExisting || (count == 0 && !includeExisting)))
                     ret.add(as.createEmptyData(this, count + 1).getXPathFull());
             } catch (Exception e) {
                 //ignore
             }
             if (checkOneOf && count > 0)
                 return ret; //now we either have another to add of an existing or none -> return
         }
         return ret;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected FxGroupData copy(FxGroupData parent) {
         return new FxGroupData(this, parent);
     }
 
     /**
      * Get a list of all FxReference values in this group and optionally all sub groups
      *
      * @param includeSubGroups collect FxReferences from sub groups as well?
      * @return list of all FxReference values in this group and optionally all sub groups
      */
     protected List<FxReference> getReferences(boolean includeSubGroups) {
         List<FxReference> refs = new ArrayList<FxReference>(20);
         gatherReferences(refs, includeSubGroups);
         return refs;
     }
 
     /**
      * Walk through all data nodes and collect FxReference instances
      *
      * @param refs             list to add references to
      * @param includeSubGroups should sub groups be included?
      */
     private void gatherReferences(List<FxReference> refs, boolean includeSubGroups) {
         for (FxData d : data) {
             if (d instanceof FxGroupData) {
                 if (includeSubGroups)
                     ((FxGroupData) d).gatherReferences(refs, includeSubGroups);
             } else if (d instanceof FxPropertyData) {
                 if (((FxPropertyData) d).getValue() instanceof FxReference)
                     refs.add((FxReference) ((FxPropertyData) d).getValue());
             }
         }
     }
 
     /**
      * Get a list of all FxPropertyData entries that are assigned to propertyId
      *
      * @param propertyId   the property id requested. -1 to return all properties.
      * @param includeEmpty include empty data instances?
      * @return list of all FxPropertyData entries that are assigned to propertyId
      */
     public List<FxPropertyData> getPropertyData(long propertyId, boolean includeEmpty) {
         List<FxPropertyData> res = new ArrayList<FxPropertyData>(5);
         for (FxData d : getChildren()) {
             if (d instanceof FxPropertyData && (propertyId == -1 || ((FxPropertyData) d).getPropertyId() == propertyId)) {
                 if (includeEmpty || !d.isEmpty())
                     res.add((FxPropertyData) d);
             } else if (d instanceof FxGroupData)
                 res.addAll(((FxGroupData) d).getPropertyData(propertyId, includeEmpty));
         }
         return res;
     }
 
     /**
      * Remove all entries that are not system internal form the current group
      *
      * @since 3.1
      */
     public void removeNonInternalData() {
         List<FxData> nonInternal = new ArrayList<FxData>(10);
         for (FxData d : data) {
             if (!d.isSystemInternal())
                 nonInternal.add(d);
         }
         for (FxData d : nonInternal)
             data.remove(d);
         this.compactPositions(true);
     }
 
     /**
      * Is a change listener attached
      *
      * @return a change listener is attached
      * @since 3.1.6
      */
     public boolean hasChangeListener() {
         return changeListener != null;
     }
 
     /**
      * Get thechange listener if attached
      *
      * @return a change listener if attached
      * @since 3.1.6
      */
     public FxValueChangeListener getChangeListener() {
         return changeListener;
     }
 
     /**
      * Set the change listener
      *
      * @param changeListener change listener
      * @param includeSubGroups set the change listener for subgroups as well?
      * @since 3.1.6
      */
     public void setChangeListener(FxValueChangeListener changeListener, boolean includeSubGroups) {
         this.changeListener = changeListener;
         for (FxData child : this.data) {
             if (includeSubGroups && child.isGroup())
                 ((FxGroupData) child).setChangeListener(changeListener, includeSubGroups);
             else
                 ((FxPropertyData) child).getValue().setChangeListener(changeListener);
         }
     }
 
     /**
      * Called internally when a value has changed
      *
      * @param xpath affected xpath
      * @param changeType type of change
      */
     protected void valueChanged(String xpath, FxValueChangeListener.ChangeType changeType) {
         if(this.changeListener != null)
             this.changeListener.onValueChanged(xpath, changeType);
     }
 }
