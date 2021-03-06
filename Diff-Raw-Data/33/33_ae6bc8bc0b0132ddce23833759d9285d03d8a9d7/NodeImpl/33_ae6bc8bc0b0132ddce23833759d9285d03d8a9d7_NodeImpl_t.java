 /*
  * Copyright (c) 2002-2008 "Neo Technology,"
  *     Network Engine for Objects in Lund AB [http://neotechnology.com]
  *
  * This file is part of Neo4j.
  * 
  * Neo4j is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  * 
  * You should have received a copy of the GNU Affero General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 package org.neo4j.impl.core;
 
 import java.util.Iterator;
 import java.util.List;
 
 import org.neo4j.api.core.Direction;
 import org.neo4j.api.core.Node;
 import org.neo4j.api.core.NotFoundException;
 import org.neo4j.api.core.Relationship;
 import org.neo4j.api.core.RelationshipType;
 import org.neo4j.api.core.ReturnableEvaluator;
 import org.neo4j.api.core.StopEvaluator;
 import org.neo4j.api.core.Traverser;
 import org.neo4j.api.core.Traverser.Order;
 import org.neo4j.impl.transaction.LockType;
 import org.neo4j.impl.util.ArrayIntSet;
 import org.neo4j.impl.util.ArrayMap;
 
 class NodeImpl extends NeoPrimitive implements Node, Comparable<Node>
 {
     private ArrayMap<String,ArrayIntSet> relationshipMap = null;
 
     NodeImpl( int id, NodeManager nodeManager )
     {
         super( id, nodeManager );
     }
 
     // newNode will only be true for NodeManager.createNode
     NodeImpl( int id, boolean newNode, NodeManager nodeManager )
     {
         super( id, newNode, nodeManager );
         if ( newNode )
         {
             relationshipMap = new ArrayMap<String,ArrayIntSet>();
         }
     }
 
     protected void changeProperty( int propertyId, Object value )
     {
         nodeManager.nodeChangeProperty( this, propertyId, value );
     }
 
     protected int addProperty( PropertyIndex index, Object value )
     {
         return nodeManager.nodeAddProperty( this, index, value );
     }
 
     protected void removeProperty( int propertyId )
     {
         nodeManager.nodeRemoveProperty( this, propertyId );
     }
 
     protected RawPropertyData[] loadProperties()
     {
         return nodeManager.loadProperties( this );
     }
 
     public Iterable<Relationship> getRelationships()
     {
         ensureFullRelationships();
         ArrayIntSet relIds = new ArrayIntSet();
         for ( String type : relationshipMap.keySet() )
         {
             ArrayIntSet source = relationshipMap.get( type );
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 relIds.add( relId );
             }
         }
         ArrayMap<String,ArrayIntSet> cowRelationshipAddMap = null;
         cowRelationshipAddMap = nodeManager.getCowRelationshipAddMap( this );
         if ( cowRelationshipAddMap != null )
         {
             for ( String type : cowRelationshipAddMap.keySet() )
             {
                 ArrayIntSet source = cowRelationshipAddMap.get( type );
                 for ( int relId : source.values() )
                 {
                     relIds.add( relId );
                 }
             }
         }
         return new RelationshipArrayIntSetIterator( relIds, this,
             Direction.BOTH, nodeManager );
     }
 
     public Iterable<Relationship> getRelationships( Direction dir )
     {
         if ( dir == Direction.BOTH )
         {
             return getRelationships();
         }
         ensureFullRelationships();
         ArrayIntSet relIds = new ArrayIntSet();
         for ( String type : relationshipMap.keySet() )
         {
             ArrayIntSet source = relationshipMap.get( type );
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 relIds.add( relId );
             }
         }
         ArrayMap<String,ArrayIntSet> cowRelationshipAddMap = 
             nodeManager.getCowRelationshipAddMap( this );
         if ( cowRelationshipAddMap != null )
         {
             for ( String type : cowRelationshipAddMap.keySet() )
             {
                 ArrayIntSet source = cowRelationshipAddMap.get( type );
                 if ( source == null )
                 {
                     continue;
                 }
                 for ( int relId : source.values() )
                 {
                     relIds.add( relId );
                 }
             }
         }
         return new RelationshipArrayIntSetIterator( relIds, this, dir,
             nodeManager );
     }
 
     public Iterable<Relationship> getRelationships( RelationshipType type )
     {
         ensureFullRelationships();
         ArrayIntSet relIds = new ArrayIntSet();
         ArrayIntSet source = relationshipMap.get( type.name() );
         if ( source != null )
         {
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type.name() );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 relIds.add( relId );
             }
         }
         source = nodeManager.getCowRelationshipAddMap( this, type.name() );
         if ( source != null )
         {
             for ( int relId : source.values() )
             {
                 relIds.add( relId );
             }
         }
         return new RelationshipArrayIntSetIterator( relIds, this,
             Direction.BOTH, nodeManager );
     }
 
     public Iterable<Relationship> getRelationships( RelationshipType... types )
     {
         ensureFullRelationships();
         ArrayIntSet relIds = new ArrayIntSet();
         for ( RelationshipType type : types )
         {
             ArrayIntSet source = relationshipMap.get( type.name() );
             if ( source == null )
             {
                 continue;
             }
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type.name() );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 relIds.add( relId );
             }
         }
         for ( RelationshipType type : types )
         {
             ArrayIntSet source = nodeManager.getCowRelationshipAddMap(
                 this, type.name() );
             if ( source == null )
             {
                 continue;
             }
             for ( int relId : source.values() )
             {
                 relIds.add( relId );
             }
         }
         return new RelationshipArrayIntSetIterator( relIds, this,
             Direction.BOTH, nodeManager );
     }
 
     public Relationship getSingleRelationship( RelationshipType type,
         Direction dir )
     {
         Iterator<Relationship> rels = getRelationships( type, dir ).iterator();
         if ( !rels.hasNext() )
         {
             return null;
         }
         Relationship rel = rels.next();
         if ( rels.hasNext() )
         {
             throw new NotFoundException( "More than one relationship[" +
                 type + ", " + dir + "] found for " + this );
         }
         return rel;
     }
 
     public Iterable<Relationship> getRelationships( RelationshipType type,
         Direction dir )
     {
         ensureFullRelationships();
         ArrayIntSet relIds = new ArrayIntSet();
         ArrayIntSet source = relationshipMap.get( type.name() );
         if ( source != null )
         {
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type.name() );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 relIds.add( relId );
             }
         }
         source = nodeManager.getCowRelationshipAddMap( this, type.name() );
         if ( source != null )
         {
             for ( int relId : source.values() )
             {
                 relIds.add( relId );
             }
         }
         return new RelationshipArrayIntSetIterator( relIds, this, dir,
             nodeManager );
     }
     
     public void delete()
     {
         nodeManager.acquireLock( this, LockType.WRITE );
         boolean success = false;
         try
         {
             nodeManager.deleteNode( this );
             success = true;
         }
         finally
         {
             nodeManager.releaseLock( this, LockType.WRITE );
             if ( !success )
             {
                 setRollbackOnly();
             }
         }
     }
 
     /**
      * If object <CODE>node</CODE> is a node, 0 is returned if <CODE>this</CODE>
      * node id equals <CODE>node's</CODE> node id, 1 if <CODE>this</CODE>
      * node id is greater and -1 else.
      * <p>
      * If <CODE>node</CODE> isn't a node a ClassCastException will be thrown.
      * 
      * @param node
      *            the node to compare this node with
      * @return 0 if equal id, 1 if this id is greater else -1
      */
     public int compareTo( Node n )
     {
         long ourId = this.getId(), theirId = n.getId();
 
         if ( ourId < theirId )
         {
             return -1;
         }
         else if ( ourId > theirId )
         {
             return 1;
         }
         else
         {
             return 0;
         }
     }
 
     /**
      * Returns true if object <CODE>o</CODE> is a node with the same id as
      * <CODE>this</CODE>.
      * 
      * @param o
      *            the object to compare
      * @return true if equal, else false
      */
     public boolean equals( Object o )
     {
         // verify type and not null, should use Node inteface
         if ( !(o instanceof Node) )
         {
             return false;
         }
 
         // The equals contract:
         // o reflexive: x.equals(x)
         // o symmetric: x.equals(y) == y.equals(x)
         // o transitive: ( x.equals(y) && y.equals(z) ) == true
         // then x.equals(z) == true
         // o consistent: the nodeId never changes
         return this.getId() == ((Node) o).getId();
 
     }
 
     public int hashCode()
     {
         return id;
     }
 
     /**
      * Returns this node's string representation.
      * 
      * @return the string representation of this node
      */
     public String toString()
     {
         return "NodeImpl#" + this.getId();
     }
 
     // caller is responsible for acquiring lock
     // this method is only called when a relationship is created or
     // a relationship delete is undone or when the full node is loaded
     void addRelationship( RelationshipType type, int relId )
     {
         ArrayIntSet relationshipSet = nodeManager.getCowRelationshipAddMap(
             this, type.name(), true );
         relationshipSet.add( relId );
     }
 
     // caller is responsible for acquiring lock
     // this method is only called when a undo create relationship or
     // a relationship delete is invoked.
     void removeRelationship( RelationshipType type, int relId )
     {
         ArrayIntSet addedSet = nodeManager.getCowRelationshipAddMap( this, 
             type.name() );
         if ( addedSet != null )
         {
             addedSet.remove( relId );
         }
         ArrayIntSet relationshipSet = nodeManager.getCowRelationshipRemoveMap(
             this, type.name(), true );
         relationshipSet.add( relId );
     }
 
     /*Map<String, Integer> internalGetRelationships()
     {
         ensureFullRelationships();
         Map<String, Integer> result = null;
         for ( String type : relationshipMap.keySet() )
         {
             ArrayIntSet source = relationshipMap.get( type );
             ArrayIntSet skip = nodeManager.getCowRelationshipRemoveMap(
                 this, type );
             for ( int relId : source.values() )
             {
                 if ( skip != null && skip.contains( relId ) )
                 {
                     continue;
                 }
                 result = result == null ?
                     new HashMap<String, Integer>() : result;
                 Integer count = result.get( type );
                 count = count == null ? 0 : count;
                 count++;
                 result.put( type, count );
             }
         }
         ArrayMap<String,ArrayIntSet> cowRelationshipAddMap = 
             nodeManager.getCowRelationshipAddMap( this );
         if ( cowRelationshipAddMap != null )
         {
             for ( String type : cowRelationshipAddMap.keySet() )
             {
                 ArrayIntSet source = cowRelationshipAddMap.get( type );
                 if ( source.size() > 0 )
                 {
                     result = result == null ?
                         new HashMap<String, Integer>() : result;
                     Integer count = result.get( type );
                     count = count == null ? 0 : count;
                     count += source.size();
                     result.put( type, count );
                 }
             }
         }
         return result;
     }*/
 
     private boolean ensureFullRelationships()
     {
         if ( relationshipMap == null )
         {
             List<RelationshipImpl> fullRelationshipList = 
                 nodeManager.loadRelationships( this );
             ArrayMap<String,ArrayIntSet> newRelationshipMap = 
                 new ArrayMap<String,ArrayIntSet>();
             for ( Relationship rel : fullRelationshipList )
             {
                 int relId = (int) rel.getId();
                 RelationshipType type = rel.getType();
                 ArrayIntSet relationshipSet = newRelationshipMap.get( 
                     type.name() );
                 if ( relationshipSet == null )
                 {
                     relationshipSet = new ArrayIntSet();
                     newRelationshipMap.put( type.name(), relationshipSet );
                 }
                 relationshipSet.add( relId );
             }
             this.relationshipMap = newRelationshipMap;
             return true;
         }
         return false;
     }
 
     public Relationship createRelationshipTo( Node otherNode,
         RelationshipType type )
     {
         return nodeManager.createRelationship( this, otherNode, type );
     }
 
     public Traverser traverse( Order traversalOrder,
         StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
         RelationshipType relationshipType, Direction direction )
     {
         if ( direction == null )
         {
             throw new IllegalArgumentException( "Null direction" );
         }
         if ( relationshipType == null )
         {
             throw new IllegalArgumentException( "Null relationship type" );
         }
         // rest of parameters will be validated in traverser package
         return nodeManager.createTraverser( traversalOrder, this,
             relationshipType, direction, stopEvaluator, returnableEvaluator );
     }
 
     public Traverser traverse( Order traversalOrder,
         StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
         RelationshipType firstRelationshipType, Direction firstDirection,
         RelationshipType secondRelationshipType, Direction secondDirection )
     {
         if ( firstDirection == null || secondDirection == null )
         {
             throw new IllegalArgumentException( "Null direction, "
                 + "firstDirection=" + firstDirection + "secondDirection="
                 + secondDirection );
         }
         if ( firstRelationshipType == null || secondRelationshipType == null )
         {
             throw new IllegalArgumentException( "Null rel type, " + "first="
                 + firstRelationshipType + "second=" + secondRelationshipType );
         }
         // rest of parameters will be validated in traverser package
         RelationshipType[] types = new RelationshipType[2];
         Direction[] dirs = new Direction[2];
         types[0] = firstRelationshipType;
         types[1] = secondRelationshipType;
         dirs[0] = firstDirection;
         dirs[1] = secondDirection;
         return nodeManager.createTraverser( traversalOrder, this, types, dirs,
             stopEvaluator, returnableEvaluator );
     }
 
     public Traverser traverse( Order traversalOrder,
         StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator,
         Object... relationshipTypesAndDirections )
     {
         int length = relationshipTypesAndDirections.length;
         if ( (length % 2) != 0 || length == 0 )
         {
             throw new IllegalArgumentException( "Variable argument should "
                 + " consist of [RelationshipType,Direction] pairs" );
         }
         int elements = relationshipTypesAndDirections.length / 2;
         RelationshipType[] types = new RelationshipType[elements];
         Direction[] dirs = new Direction[elements];
         int j = 0;
         for ( int i = 0; i < elements; i++ )
         {
             Object relType = relationshipTypesAndDirections[j++];
             if ( !(relType instanceof RelationshipType) )
             {
                 throw new IllegalArgumentException(
                     "Expected RelationshipType at var args pos " + (j - 1)
                         + ", found " + relType );
             }
             types[i] = (RelationshipType) relType;
             Object direction = relationshipTypesAndDirections[j++];
             if ( !(direction instanceof Direction) )
             {
                 throw new IllegalArgumentException(
                     "Expected Direction at var args pos " + (j - 1)
                         + ", found " + direction );
             }
             dirs[i] = (Direction) direction;
         }
         return nodeManager.createTraverser( traversalOrder, this, types, dirs,
             stopEvaluator, returnableEvaluator );
     }
 
     public boolean hasRelationship()
     {
         return getRelationships().iterator().hasNext();
     }
 
     public boolean hasRelationship( RelationshipType... types )
     {
         return getRelationships( types ).iterator().hasNext();
     }
 
     public boolean hasRelationship( Direction dir )
     {
         return getRelationships( dir ).iterator().hasNext();
     }
 
     public boolean hasRelationship( RelationshipType type, Direction dir )
     {
         return getRelationships( type, dir ).iterator().hasNext();
     }
 
     protected void commitRelationshipMaps( 
         ArrayMap<String,ArrayIntSet> cowRelationshipAddMap, 
         ArrayMap<String,ArrayIntSet> cowRelationshipRemoveMap )
     {
         if ( cowRelationshipAddMap != null )
         {
             if ( relationshipMap == null )
             {
                 // we will load full in some other tx
                 return;
             }
             for ( String type : cowRelationshipAddMap.keySet() )
             {
                 ArrayIntSet source = cowRelationshipAddMap.get( type );
                 if ( source.size() == 0 )
                 {
                     continue;
                 }
                ArrayIntSet dest = new ArrayIntSet();
                ArrayIntSet orig = relationshipMap.get( type );
                if ( orig != null )
                 {
                    for ( int relId : orig.values() )
                    {
                        dest.add( relId );
                    }
                 }
                 for ( int relId : source.values() )
                 {
                     dest.add( relId );
                 }
                relationshipMap.put( type, dest );
             }
         }
         if ( cowRelationshipRemoveMap != null && relationshipMap != null )
         {
             for ( String type : cowRelationshipRemoveMap.keySet() )
             {
                 ArrayIntSet source = cowRelationshipRemoveMap.get( type );
                if ( source.size() == 0 )
                 {
                     continue;
                 }
                ArrayIntSet dest = new ArrayIntSet(); 
                ArrayIntSet orig = relationshipMap.get( type );
                if ( orig != null )
                 {
                    for ( int relId : orig.values() )
                    {
                        if ( !source.contains( relId ) )
                        {
                            dest.add( relId );
                        }
                    }
                    relationshipMap.put( type, dest );
                 }
             }
         }
     }
 }
