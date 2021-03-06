 /*
  * $Id$
  */
 package org.xins.util.service;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Random;
 import org.xins.util.MandatoryArgumentChecker;
 
 /**
  * Descriptor for a group of services. Each <code>GroupDescriptor</code> has
  * at least 2 members.
  *
  * @version $Revision$ $Date$
  * @author Ernst de Haan (<a href="mailto:znerd@FreeBSD.org">znerd@FreeBSD.org</a>)
  *
  * @since XINS 0.105
  */
 public final class GroupDescriptor extends Descriptor {
 
    //-------------------------------------------------------------------------
    // Class fields
    //-------------------------------------------------------------------------
 
    /**
     * The identifier of the <em>random</em> group type.
     */
    public static final String RANDOM_TYPE_ID = "random";
 
    /**
     * The identifier of the <em>ordered</em> group type.
     */
    public static final String ORDERED_TYPE_ID = "ordered";
 
    /**
     * The <em>random</em> group type.
     */
    public static final Type RANDOM_TYPE = new Type(RANDOM_TYPE_ID);
 
    /**
     * The <em>ordered</em> group type.
     */
    public static final Type ORDERED_TYPE = new Type(ORDERED_TYPE_ID);
 
    /**
     * Pseudo-random number generator.
     */
    private static final Random RANDOM = new Random();
 
 
    //-------------------------------------------------------------------------
    // Class functions
    //-------------------------------------------------------------------------
 
    /**
     * Gets a group type by identifier.
     *
     * @param identifier
     *    the identifier for the group, cannot be <code>null</code>.
     *
     * @return
     *    the type with the specified identifier, or <code>null</code> if there
     *    is no matching type.
     *
     * @throws IllegalArgumentException
     *    if <code>identifier == null</code>.
     */
    public static Type getType(String identifier)
    throws IllegalArgumentException {
 
       // Check preconditions
       MandatoryArgumentChecker.check("identifier", identifier);
 
       // Match
       if (RANDOM_TYPE_ID.equals(identifier)) {
          return RANDOM_TYPE;
       } else if (ORDERED_TYPE_ID.equals(identifier)) {
          return ORDERED_TYPE;
       } else {
          return null;
       }
    }
 
 
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
 
    /**
     * Constructs a new <code>GroupDescriptor</code>. The members to be
     * included must be passed. The array of members may be empty, but it
     * cannot contain any <code>null</code> elements. It may contain
     * duplicates, though.
     *
     * @param type
     *    the type of group, cannot be <code>null</code>.
     *
     * @param members
     *    list of members of the group, cannot be <code>null</code>.
     *
     * @throws IllegalArgumentException
     *    if <code>type == null
     *          || members == null
     *          || members[<em>n</em>] == null</code>
     *    (where <code>0 &lt;= <em>n</em> &lt; members.length</code>).
     */
   public GroupDescriptor(Type type, Descriptor[] members) {
 
       // Check preconditions
       MandatoryArgumentChecker.check("type", type, "members", members);
       int size = members.length;
       for (int i = 0; i < size; i++) {
          Descriptor d = members[i];
          if (d == null) {
             throw new IllegalArgumentException("members[" + i + "] == null");
          }
       }
 
       // Store members
       _type    = type;
       _members = new Descriptor[size];
       System.arraycopy(members, 0, _members, 0, size);
    }
 
 
    //-------------------------------------------------------------------------
    // Fields
    //-------------------------------------------------------------------------
 
    /**
     * The type of this group. Cannot be <code>null</code>.
     */
    private final Type _type;
 
    /**
     * The members of this group. Cannot be <code>null</code>.
     */
    private final Descriptor[] _members;
 
 
    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------
 
    /**
     * Checks if this service descriptor denotes a group.
     *
     * @return
     *    <code>true</code>, since this descriptor denotes a group.
     */
    public boolean isGroup() {
       return true;
    }
 
    public Iterator iterateServices() {
       if (_type == RANDOM_TYPE) {
          return new RandomIterator();
       } else if (_type == ORDERED_TYPE) {
          return new OrderedIterator();
       } else {
          throw new Error("Unexpected condition: Unknown type: " + _type + '.');
       }
    }
 
    /**
     * Returns the type of this group.
     *
     * @return
     *    the type of this group, not <code>null</code>.
     */
    public Type getType() {
       return _type;
    }
 
    /**
     * Returns the members of this group.
     *
     * @return
     *    the members of this group as a new array, not <code>null</code>.
     */
    public Descriptor[] getMembers() {
       int size = _members.length;
       Descriptor[] array = new Descriptor[size];
       System.arraycopy(_members, 0, array, 0, size);
       return array;
    }
 
 
    //-------------------------------------------------------------------------
    // Inner classes
    //-------------------------------------------------------------------------
 
    /**
     * Type of a group.
     *
     * @version $Revision$ $Date$
     * @author Ernst de Haan (<a href="mailto:znerd@FreeBSD.org">znerd@FreeBSD.org</a>)
     *
     * @since XINS 0.105
     */
    public static final class Type extends Object {
 
       //----------------------------------------------------------------------
       // Constructors
       //----------------------------------------------------------------------
 
       /**
        * Constructs a new <code>Type</code> with the specified description.
        *
        * @param description
        *    the description for this type.
        */
       private Type(String description) {
          _description = description;
       }
 
 
       //----------------------------------------------------------------------
       // Fields
       //----------------------------------------------------------------------
 
       /**
        * The description for this type.
        */
       private final String _description;
 
 
       //----------------------------------------------------------------------
       // Methods
       //----------------------------------------------------------------------
 
       public String toString() {
          return _description;
       }
    }
 
 
    //-------------------------------------------------------------------------
    // Inner classes
    //-------------------------------------------------------------------------
 
    /**
     * Random iterator over the leaf service descriptors contained in this
     * group descriptor. Needed for the implementation of
     * {@link #iterateServices()}.
     *
     * @version $Revision$ $Date$
     * @author Ernst de Haan (<a href="mailto:znerd@FreeBSD.org">znerd@FreeBSD.org</a>)
     *
     * @since XINS 0.105
     */
    private final class RandomIterator
    extends Object
    implements Iterator {
 
       //----------------------------------------------------------------------
       // Constructors
       //----------------------------------------------------------------------
 
       /**
        * Constructs a new <code>RandomIterator</code>.
        */
       private RandomIterator() {
 
          // Copy all members to _remaining
          int size = _members.length;
          _remaining = new ArrayList(size);
          for (int i = 0; i < size; i++) {
             _remaining.add(_members[i]);
          }
 
          // Pick a member randomly
          int index = Math.abs(RANDOM.nextInt() % size);
          Descriptor member = (Descriptor) _remaining.remove(index);
 
          // Initialize the current iterator to link to that member's services
          _currentIterator = member.iterateServices();
       }
 
 
       //----------------------------------------------------------------------
       // Fields
       //----------------------------------------------------------------------
 
       /**
        * The set of remaining descriptors. One is removed from a random index
        * each time {@link #next()} is called.
        *
        * <p>This field will be set to <code>null</code> as soon as there are
        * no more remaining members. Still {@link #_currentIterator} could have
        * more elements.
        */
       private List _remaining;
 
       /**
        * Current iterator of one of the members.
        *
        * <p>This field will be set to <code>null</code> as soon as there are
        * no more remaining services to be iterated over.
        */
       private Iterator _currentIterator;
 
 
       //----------------------------------------------------------------------
       // Methods
       //----------------------------------------------------------------------
 
       public boolean hasNext() {
          return (_currentIterator != null);
       }
 
       public Object next() throws NoSuchElementException {
 
          // Check preconditions
          if (_currentIterator == null) {
             throw new NoSuchElementException();
          }
 
          // Get the next service
          Object o = _currentIterator.next();
 
          // Check if this member/iterator has any more
          if (! _currentIterator.hasNext()) {
 
             // If there are no remaining members, set _currentIterator to null
             if (_remaining == null) {
                _currentIterator = null;
 
             } else {
                // Pick one of the remaining members
                int size = _remaining.size();
                int index = (size == 1) ? 0 : Math.abs(RANDOM.nextInt() % size);
                Descriptor member = (Descriptor) _remaining.remove(index);
                _currentIterator = member.iterateServices();
 
                // If there are now no additional remaining members, set
                // _remaining to null
                if (size == 1) {
                   _remaining = null;
                }
             }
          }
 
          return o;
       }
 
       public void remove() throws UnsupportedOperationException {
          throw new UnsupportedOperationException();
       }
    }
 
    /**
     * Ordered iterator over the leaf service descriptors contained in this
     * group descriptor. Needed for the implementation of
     * {@link #iterateServices()}.
     *
     * @version $Revision$ $Date$
     * @author Ernst de Haan (<a href="mailto:znerd@FreeBSD.org">znerd@FreeBSD.org</a>)
     *
     * @since XINS 0.116
     */
    private final class OrderedIterator
    extends Object
    implements Iterator {
 
       //----------------------------------------------------------------------
       // Constructors
       //----------------------------------------------------------------------
 
       /**
        * Constructs a new <code>OrderedIterator</code>.
        */
       private OrderedIterator() {
 
          // Copy all members to _remaining
          _currentIndex = 0;
 
          // Initialize the current iterator to link to that member's services
          _currentIterator = _members[0].iterateServices();
       }
 
 
       //----------------------------------------------------------------------
       // Fields
       //----------------------------------------------------------------------
 
       /**
        * The current index into the list of members. Will be set to a negative
        * value if there are no more members.
        */
       private int _currentIndex;
 
       /**
        * Current iterator of one of the members.
        *
        * <p>This field will be set to <code>null</code> as soon as there are
        * no more remaining services to be iterated over.
        */
       private Iterator _currentIterator;
 
 
       //----------------------------------------------------------------------
       // Methods
       //----------------------------------------------------------------------
 
       public boolean hasNext() {
          return (_currentIterator != null);
       }
 
       public Object next() throws NoSuchElementException {
 
          // Check preconditions
          if (_currentIterator == null) {
             throw new NoSuchElementException();
          }
 
          // Get the next service
          Object o = _currentIterator.next();
 
          // Check if this member/iterator has any more
          if (! _currentIterator.hasNext()) {
 
             // If there are no remaining members, set _currentIterator to null
             if (_currentIndex < 0) {
                _currentIterator = null;
 
             } else {
                _currentIndex++;
 
                if (_currentIndex < _members.length) {
                   _currentIterator = _members[_currentIndex].iterateServices();
                } else {
                   _currentIndex = -1;
                }
             }
          }
 
          return o;
       }
 
       public void remove() throws UnsupportedOperationException {
          throw new UnsupportedOperationException();
       }
    }
 }
