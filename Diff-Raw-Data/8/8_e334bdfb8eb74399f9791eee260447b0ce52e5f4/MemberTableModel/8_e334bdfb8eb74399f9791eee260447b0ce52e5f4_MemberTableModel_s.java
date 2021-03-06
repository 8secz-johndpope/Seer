 package org.openstreetmap.josm.gui.dialogs.relation;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.Vector;
 import java.util.concurrent.CopyOnWriteArrayList;
 
 import javax.swing.DefaultListSelectionModel;
 import javax.swing.ListSelectionModel;
 import javax.swing.table.AbstractTableModel;
 import javax.swing.event.TableModelListener;
 import javax.swing.event.TableModelEvent;
 
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.data.osm.Node;
 import org.openstreetmap.josm.data.osm.OsmPrimitive;
 import org.openstreetmap.josm.data.osm.Relation;
 import org.openstreetmap.josm.data.osm.RelationMember;
 import org.openstreetmap.josm.data.osm.Way;
 import org.openstreetmap.josm.gui.layer.OsmDataLayer;
 
 public class MemberTableModel extends AbstractTableModel implements TableModelListener {
 
     /**
      * data of the table model: The list of members and the cached WayConnectionType of each member.
      **/
     private ArrayList<RelationMember> members;
     private ArrayList<WayConnectionType> connectionType = null;
     
     private DefaultListSelectionModel listSelectionModel;
     private CopyOnWriteArrayList<IMemberModelListener> listeners;
     private OsmDataLayer layer;
 
     /**
      * constructor
      */
     public MemberTableModel(OsmDataLayer layer) {
         members = new ArrayList<RelationMember>();
         listeners = new CopyOnWriteArrayList<IMemberModelListener>();
         this.layer = layer;
         addTableModelListener(this);
     }
 
     public void addMemberModelListener(IMemberModelListener listener) {
         synchronized (listeners) {
             if (listener != null && !listeners.contains(listener)) {
                 listeners.add(listener);
             }
         }
     }
 
     public void removeMemberModelListener(IMemberModelListener listener) {
         synchronized (listeners) {
             if (listener != null && listeners.contains(listener)) {
                 listeners.remove(listener);
             }
         }
     }
 
     protected void fireMakeMemberVisible(int index) {
         synchronized (listeners) {
             for (IMemberModelListener listener : listeners) {
                 listener.makeMemberVisible(index);
             }
         }
     }
 
     public void populate(Relation relation) {
         members.clear();
         if (relation != null) {
             // make sure we work with clones of the relation members
             // in the model.
             members.addAll(new Relation(relation).getMembers());
         }
         fireTableDataChanged();
     }
 
     public int getColumnCount() {
         return 3;
     }
 
     public int getRowCount() {
         return members.size();
     }
 
     public Object getValueAt(int rowIndex, int columnIndex) {
         switch (columnIndex) {
             case 0:
                 return members.get(rowIndex).getRole();
             case 1:
                 return members.get(rowIndex).getMember();
             case 2:
                 return wayConnection(rowIndex);
         }
         // should not happen
         return null;
     }
 
     @Override
     public boolean isCellEditable(int rowIndex, int columnIndex) {
         return columnIndex == 0;
     }
 
     public OsmPrimitive getReferredPrimitive(int idx) {
         return members.get(idx).getMember();
     }
 
     public void moveUp(int[] selectedRows) {
         if (!canMoveUp(selectedRows))
             return;
 
         for (int row : selectedRows) {
             RelationMember member1 = members.get(row);
             RelationMember member2 = members.get(row - 1);
             members.set(row, member2);
             members.set(row - 1, member1);
         }
         fireTableDataChanged();
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int row : selectedRows) {
             row--;
             getSelectionModel().addSelectionInterval(row, row);
         }
         getSelectionModel().setValueIsAdjusting(false);
         fireMakeMemberVisible(selectedRows[0] - 1);
     }
 
     public void moveDown(int[] selectedRows) {
         if (!canMoveDown(selectedRows))
             return;
 
         for (int i = selectedRows.length - 1; i >= 0; i--) {
             int row = selectedRows[i];
             RelationMember member1 = members.get(row);
             RelationMember member2 = members.get(row + 1);
             members.set(row, member2);
             members.set(row + 1, member1);
         }
         fireTableDataChanged();
         getSelectionModel();
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int row : selectedRows) {
             row++;
             getSelectionModel().addSelectionInterval(row, row);
         }
         getSelectionModel().setValueIsAdjusting(false);
         fireMakeMemberVisible(selectedRows[0] + 1);
     }
 
     public void remove(int[] selectedRows) {
         if (!canRemove(selectedRows))
             return;
         int offset = 0;
         for (int row : selectedRows) {
             row -= offset;
             members.remove(row);
             offset++;
         }
         fireTableDataChanged();
     }
 
     public boolean canMoveUp(int[] rows) {
         if (rows == null || rows.length == 0)
             return false;
         Arrays.sort(rows);
         return rows[0] > 0 && members.size() > 0;
     }
 
     public boolean canMoveDown(int[] rows) {
         if (rows == null || rows.length == 0)
             return false;
         Arrays.sort(rows);
         return members.size() > 0 && rows[rows.length - 1] < members.size() - 1;
     }
 
     public boolean canRemove(int[] rows) {
         if (rows == null || rows.length == 0)
             return false;
         return true;
     }
 
     public DefaultListSelectionModel getSelectionModel() {
         if (listSelectionModel == null) {
             listSelectionModel = new DefaultListSelectionModel();
             listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         }
         return listSelectionModel;
     }
     public void removeMembersReferringTo(List<? extends OsmPrimitive> primitives) {
         if (primitives == null)
             return;
         Iterator<RelationMember> it = members.iterator();
         while (it.hasNext()) {
             RelationMember member = it.next();
             if (primitives.contains(member.getMember())) {
                 it.remove();
             }
         }
         fireTableDataChanged();
     }
 
     public void applyToRelation(Relation relation) {
         relation.setMembers(members);
     }
 
     public boolean hasSameMembersAs(Relation relation) {
         if (relation == null)
             return false;
         if (relation.getMembersCount() != members.size())
             return false;
         for (int i = 0; i < relation.getMembersCount(); i++) {
             if (!relation.getMember(i).equals(members.get(i)))
                 return false;
         }
         return true;
     }
 
     public boolean hasIncompleteMembers() {
         for (RelationMember member : members) {
             if (member.getMember().incomplete)
                 return true;
         }
         return false;
     }
 
     protected List<Integer> getSelectedIndices() {
         ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
         for (int i = 0; i < members.size(); i++) {
             if (getSelectionModel().isSelectedIndex(i)) {
                 selectedIndices.add(i);
             }
         }
         return selectedIndices;
     }
 
     public void addMembersAtBeginning(List<OsmPrimitive> primitives) {
         if (primitives == null)
             return;
         for (OsmPrimitive primitive : primitives) {
             RelationMember member = new RelationMember("", primitive);
             members.add(0, member);
         }
         fireTableDataChanged();
         getSelectionModel().clearSelection();
         for (int i = 0; i < primitives.size(); i++) {
             getSelectionModel().addSelectionInterval(i, i);
         }
         fireMakeMemberVisible(0);
     }
 
     public void addMembersAtEnd(List<? extends OsmPrimitive> primitives) {
         if (primitives == null)
             return;
 
         for (OsmPrimitive primitive : primitives) {
             RelationMember member = new RelationMember("", primitive);
             members.add(member);
         }
         fireTableDataChanged();
         getSelectionModel().clearSelection();
         for (int i = 0; i < primitives.size(); i++) {
             getSelectionModel().addSelectionInterval(members.size() - 1 - i, members.size() - 1 - i);
         }
         fireMakeMemberVisible(members.size() - 1);
     }
 
     public void addMembersBeforeIdx(List<? extends OsmPrimitive> primitives, int idx) {
         if (primitives == null)
             return;
 
         for (OsmPrimitive primitive : primitives) {
             RelationMember member = new RelationMember("", primitive);
             members.add(idx, member);
         }
         fireTableDataChanged();
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int i = 0; i < primitives.size(); i++) {
             getSelectionModel().addSelectionInterval(idx + i, idx + i);
         }
         getSelectionModel().setValueIsAdjusting(false);
         fireMakeMemberVisible(idx);
     }
 
     public void addMembersAfterIdx(List<? extends OsmPrimitive> primitives, int idx) {
         if (primitives == null)
             return;
         int j = 1;
         for (OsmPrimitive primitive : primitives) {
             RelationMember member = new RelationMember("", primitive);
             members.add(idx + j, member);
             j++;
         }
         fireTableDataChanged();
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int i = 0; i < primitives.size(); i++) {
             getSelectionModel().addSelectionInterval(idx + 1 + i, idx + 1 + i);
         }
         getSelectionModel().setValueIsAdjusting(false);
         fireMakeMemberVisible(idx + 1);
     }
 
     /**
      * Replies the number of members which refer to a particular primitive
      *
      * @param primitive the primitive
      * @return the number of members which refer to a particular primitive
      */
     public int getNumMembersWithPrimitive(OsmPrimitive primitive) {
         int count = 0;
         for (RelationMember member : members) {
             if (member.getMember().equals(primitive)) {
                 count++;
             }
         }
         return count;
     }
 
     /**
      * updates the role of the members given by the indices in <code>idx</code>
      *
      * @param idx the array of indices
      * @param role the new role
      */
     public void updateRole(int[] idx, String role) {
         if (idx == null || idx.length == 0)
             return;
         for (int row : idx) {
             RelationMember oldMember = members.get(row);
             RelationMember newMember = new RelationMember(role, oldMember.getMember());
             members.remove(row);
             members.add(row, newMember);
         }
         fireTableDataChanged();
         for (int row : idx) {
             getSelectionModel().addSelectionInterval(row, row);
         }
     }
 
     /**
      * Get the currently selected relation members
      *
      * @return a collection with the currently selected relation members
      */
     public Collection<RelationMember> getSelectedMembers() {
         ArrayList<RelationMember> selectedMembers = new ArrayList<RelationMember>();
         for (int i : getSelectedIndices()) {
             selectedMembers.add(members.get(i));
         }
         return selectedMembers;
     }
 
     /**
      * Replies the set of selected referers. Never null, but may be empty.
      *
      * @return the set of selected referers
      */
     public Set<OsmPrimitive> getSelectedChildPrimitives() {
         HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
         for (RelationMember m: getSelectedMembers()) {
             ret.add(m.getMember());
         }
         return ret;
     }
 
     /**
      * Replies the set of selected referers. Never null, but may be empty.
      *
      * @return the set of selected referers
      */
     public Set<OsmPrimitive> getChildPrimitives(Collection<? extends OsmPrimitive> referenceSet) {
         HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
         if (referenceSet == null) return null;
         for (RelationMember m: members) {
             if (referenceSet.contains(m.getMember())) {
                 ret.add(m.getMember());
             }
         }
         return ret;
     }
 
 
     /**
      * Replies true, if the selected {@see OsmPrimitive}s in the layer belonging
      * to this model are in sync with the selected referers in this model.
      *
      * @return
      */
     public boolean selectionsAreInSync() {
         HashSet<OsmPrimitive> s1 = new HashSet<OsmPrimitive>(getSelectedChildPrimitives());
         if (s1.size() != layer.data.getSelected().size()) return false;
         s1.removeAll(layer.data.getSelected());
         return s1.isEmpty();
     }
     /**
      * Selects the members in the collection selectedMembers
      *
      * @param selectedMembers the collection of selected members
      */
     public void setSelectedMembers(Collection<RelationMember> selectedMembers) {
         if (selectedMembers == null || selectedMembers.isEmpty())
             return;
 
         // lookup the indices for the respective members
         //
         ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
         for (RelationMember member : selectedMembers) {
             for (int idx = 0; idx < members.size(); idx ++) {
                 if (members.get(idx).equals(member)) {
                     if (!selectedIndices.contains(idx)) {
                         selectedIndices.add(idx);
                     }
                 }
             }
         }
 
         // select the members
         //
         Collections.sort(selectedIndices);
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int row : selectedIndices) {
             getSelectionModel().addSelectionInterval(row, row);
         }
         getSelectionModel().setValueIsAdjusting(false);
 
         // make the first selected member visible
         //
         if (selectedIndices.size() > 0) {
             fireMakeMemberVisible(selectedIndices.get(0));
         }
     }
 
     /**
      * Replies true if the index-th relation members referrs
      * to an editable relation, i.e. a relation which is not
      * incomplete.
      *
      * @param index the index
      * @return true, if the index-th relation members referrs
      * to an editable relation, i.e. a relation which is not
      * incomplete
      */
     public boolean isEditableRelation(int index) {
         if (index < 0 || index >= members.size())
             return false;
         RelationMember member = members.get(index);
         if (!member.isRelation())
             return false;
         Relation r = member.getRelation();
         return !r.incomplete;
     }
 
     /**
      * Replies true if there is at least one relation member in this model
      * which refers to at least on the primitives in <code>primitives</code>.
      *
      * @param primitives the collection of primitives
      * @return true if there is at least one relation member in this model
      * which refers to at least on the primitives in <code>primitives</code>; false
      * otherwise
      */
     public boolean hasMembersReferringTo(Collection<OsmPrimitive> primitives) {
         if (primitives == null || primitives.isEmpty())
             return false;
         HashSet<OsmPrimitive> referrers = new HashSet<OsmPrimitive>();
         for(RelationMember member : members) {
             referrers.add(member.getMember());
         }
         Iterator<OsmPrimitive> it = primitives.iterator();
         while(it.hasNext()) {
             OsmPrimitive referred = it.next();
             if (referrers.contains(referred))
                 return true;
         }
         return false;
     }
 
     /**
      * Selects all mebers which refer to {@see OsmPrimitive}s in the collections
      * <code>primitmives</code>. Does nothing is primitives is null.
      *
      * @param primitives the collection of primitives
      */
     public void selectMembersReferringTo(Collection<? extends OsmPrimitive> primitives) {
         if (primitives == null) return;
         getSelectionModel().setValueIsAdjusting(true);
         getSelectionModel().clearSelection();
         for (int i=0; i< members.size();i++) {
             RelationMember m = members.get(i);
             if (primitives.contains(m.getMember())) {
                 this.getSelectionModel().addSelectionInterval(i,i);
             }
         }
         getSelectionModel().setValueIsAdjusting(false);
         if (getSelectedIndices().size() > 0) {
             fireMakeMemberVisible(getSelectedIndices().get(0));
         }
     }
 
     /**
      * Replies true if the layer this model belongs to is equal to the active
      * layer
      *
      * @return true if the layer this model belongs to is equal to the active
      * layer
      */
     protected boolean isActiveLayer() {
         if (Main.map == null || Main.map.mapView == null) return false;
         return Main.map.mapView.getActiveLayer() == layer;
     }
 
     /**
      * get a node we can link against when sorting members
      * @param element the element we want to link against
      * @param linked_element already linked against element
      * @return the unlinked node if element is a way, the node itself if element is a node, null otherwise
      */
     private static Node getUnusedNode(RelationMember element, RelationMember linked_element)
     {
         Node result = null;
 
         if (element.isWay()) {
             Way w = element.getWay();
             if (linked_element.isWay()) {
                 Way x = linked_element.getWay();
                 if ((w.firstNode() == x.firstNode()) || (w.firstNode() == x.lastNode())) {
                     result = w.lastNode();
                 } else {
                     result = w.firstNode();
                 }
             } else if (linked_element.isNode()) {
                 Node m = linked_element.getNode();
                 if (w.firstNode() == m) {
                     result = w.lastNode();
                 } else {
                     result = w.firstNode();
                 }
             }
         } else if (element.isNode()) {
             Node n = element.getNode();
             result = n;
         }
 
         return result;
     }
 
     void sort() {
         RelationNodeMap map = new RelationNodeMap(members);
         Vector<LinkedList<Integer>> segments;
         LinkedList<Integer> segment;
         Node startSearchNode;
         Node endSearchNode;
         boolean something_done;
 
         /*
          * sort any 2 or more connected elements together may be slow with many unconnected members
          */
 
         if (map.isEmpty())
             // empty relation or incomplete members
             return;
         segments = new Vector<LinkedList<Integer>>();
 
         while (!map.isEmpty()) {
             // find an element for the next segment
             // try first element in relation if we just started
             // otherwise, or if first element is another relation, just fetch some element from the
             // map
             Integer next;
             if ((segments.size() == 0) && map.remove(0, members.get(0))) {
                 next = 0;
             } else {
                 next = map.pop();
                 if (next == null) {
                     break;
                 }
             }
 
             segment = new LinkedList<Integer>();
             segment.add(next);
             segments.add(segment);
 
             do {
                 something_done = false;
                 startSearchNode = null;
                 endSearchNode = null;
                 if (segment.size() == 1) {
                     // only one element in segment, so try to link against each linkable node of element
                     RelationMember m = members.get(segment.getFirst());
                     if (m.isWay()) {
                         Way w = m.getWay();
                         endSearchNode = w.lastNode();
                         if (w.lastNode() != w.firstNode())
                         {
                             startSearchNode = w.firstNode();
                         }
                     } else if (m.isNode()) {
                         Node n = m.getNode();
                         endSearchNode = n;
                     }
                 } else {
                     // add unused node of first element and unused node of last element
                     // start with the first element (compared to next element)
                     startSearchNode = getUnusedNode(members.get(segment.getFirst()), members.get(segment.get(1)));
 
                     // now the same for the last element (compared to previous element)
                     endSearchNode = getUnusedNode(members.get(segment.getLast()), members.get(segment.get(segment.size() - 2)));
                 }
 
                 // let's see if we can find connected elements for endSearchNode and startSearchNode
                 if (startSearchNode != null) {
                     Integer m2 = map.find(startSearchNode, segment.getFirst());
                     if (m2 != null) {
                         segment.add(0, m2);
                         map.remove(m2, members.get(m2));
                         something_done = true;
                     }
                 }
                 if (endSearchNode != null) {
                     Integer m2 = map.find(endSearchNode, segment.getLast());
                     if (m2 != null) {
                         segment.add(segment.size(), m2);
                         map.remove(m2, members.get(m2));
                         something_done = true;
                     }
                 }
             } while (something_done);
 
         }
 
         if (segments.size() > 0) {
             // append map.remaining() to segments list (as a single segment)
             segment = new LinkedList<Integer>();
             segment.addAll(map.getRemaining());
             segments.add(segment);
 
             // now we need to actually re-order the relation members
             ArrayList<RelationMember> newmembers = new ArrayList<RelationMember>();
             for (LinkedList<Integer> segment2 : segments) {
                 for (Integer p : segment2) {
                     newmembers.add(members.get(p));
                 }
             }
             members.clear();
             members.addAll(newmembers);
 
             fireTableDataChanged();
         }
     }
 
 /**
  * Determines the direction of way k with reference to the way ref_i.
  * The direction of way ref_i is ref_direction.
  *
  * ref_i is usually the predecessor of k.
  *
  * direction:
  * Let the relation be a route of oneway streets, and someone travels them in the given order.
  * Direction is 1 for if it is legel and -1 if it is illegal to do so for the given way.
  *
  * If the two ways are not properly linked the return value is 0.
  **/
     private int determineDirection(int ref_i,int ref_direction, int k) {
         if (ref_i < 0 || k < 0 || ref_i >= members.size() || k >= members.size()) {
             return 0;
         }
         if (ref_direction == 0) {
             return 0;
         }
         
         RelationMember m_ref = members.get(ref_i);
         RelationMember m = members.get(k);
         Way way_ref = null;
         Way way = null;
 
         if (m_ref.isWay()) {
             way_ref = m_ref.getWay();
         }
         if (m.isWay()) {
             way = m.getWay();
         }
         
         if (way_ref == null || way == null) {
             return 0;
         }
         
         Node nRef = ref_direction > 0 ? way_ref.lastNode() : way_ref.firstNode();
         if (nRef == null) {
             return 0;
         }
         
         if (nRef == way.firstNode()) {
             return 1;
         }
         if (nRef == way.lastNode()) {
             return -1;
         }
         return 0;
     }
 
     private WayConnectionType wayConnection(int i) {
         if (connectionType == null) {
             updateLinks();
         }
         return connectionType.get(i);
     }
 
     public void tableChanged(TableModelEvent e) {
         connectionType = null;
     }
 
     public void updateLinks() {
         connectionType = null;
         ArrayList<WayConnectionType> con = new ArrayList<WayConnectionType>();
 
         for (int i=0; i<members.size(); ++i) con.add(null);
 
         int firstGroupIdx=0;
         boolean resetFirstGoupIdx=false;
 
         for (int i=0; i<members.size(); ++i) {
             if (resetFirstGoupIdx) {
                 firstGroupIdx = i;
                 resetFirstGoupIdx = false;
             }
 
             RelationMember m = members.get(i);
             if (! m.isWay()) {
                 con.set(i, new WayConnectionType());
                 resetFirstGoupIdx = true;
                 continue;
             }
 
             Way w = m.getWay();
             if (w == null || w.incomplete) {
                 con.set(i, new WayConnectionType());
                 resetFirstGoupIdx = true;
                 continue;
             }
 
             boolean linkPrev = (i != firstGroupIdx);
             boolean linkNext;
             int dir;
             if (linkPrev) {
                 dir = determineDirection(i-1, con.get(i-1).direction, i);
                 linkNext = (determineDirection(i, dir, i+1) != 0);
             }
             else {
                 dir = determineDirection(i, +1, i+1) != 0 ? +1 : 0;
                 if (dir == 0) {
                     dir = determineDirection(i, -1, i+1) != 0 ? -1 : 0;
                 }
                 linkNext = (dir != 0);
             }
 
             con.set(i, new WayConnectionType(linkPrev, linkNext, dir));
 
             if (! linkNext) {
                 boolean loop;
                 if (i == firstGroupIdx) {
                     loop = determineDirection(i, 1, i) == 1;
                 } else {
                     loop = determineDirection(i, dir, firstGroupIdx) == con.get(firstGroupIdx).direction;
                 }
                 if (loop) {
                     for (int j=firstGroupIdx; j <= i; ++j) {
                         con.get(j).isLoop = true;
                     }
                 }
                 resetFirstGoupIdx = true;
             }
         }
         connectionType = con;
     }
 }
