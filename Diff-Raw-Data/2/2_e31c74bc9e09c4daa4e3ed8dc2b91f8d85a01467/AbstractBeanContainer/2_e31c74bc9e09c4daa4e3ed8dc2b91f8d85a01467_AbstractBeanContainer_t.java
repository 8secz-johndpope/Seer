 /*
 @ITMillApache2LicenseForJavaFiles@
  */
 package com.vaadin.data.util;
 
 import java.beans.PropertyDescriptor;
 import java.io.IOException;
 import java.io.Serializable;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import com.vaadin.data.Container.Filterable;
 import com.vaadin.data.Container.Sortable;
 import com.vaadin.data.Item;
 import com.vaadin.data.Property;
 import com.vaadin.data.Property.ValueChangeEvent;
 import com.vaadin.data.Property.ValueChangeListener;
 import com.vaadin.data.Property.ValueChangeNotifier;
 
 /**
  * An abstract base class for in-memory containers for JavaBeans.
  * 
  * <p>
  * The properties of the container are determined automatically by introspecting
  * the used JavaBean class. Only beans of the same type can be added to the
  * container.
  * </p>
  * 
  * <p>
  * Subclasses should implement adding items to the container, typically calling
  * the protected methods {@link #addItem(Object, Object)},
  * {@link #addItemAfter(Object, Object, Object)} and
  * {@link #addItemAt(int, Object, Object)}, and if necessary,
  * {@link #internalAddAt(int, Object, Object)} and
  * {@link #internalIndexOf(Object)}.
  * </p>
  * 
  * <p>
  * It is not possible to add additional properties to the container and nested
  * bean properties are not supported.
  * </p>
  * 
  * @param <IDTYPE>
  *            The type of the item identifier
  * @param <BEANTYPE>
  *            The type of the Bean
  * 
  * @since 6.5
  */
 public abstract class AbstractBeanContainer<IDTYPE, BEANTYPE> extends
         AbstractInMemoryContainer<IDTYPE, String, BeanItem<BEANTYPE>> implements
         Filterable, Sortable, ValueChangeListener {
 
     /**
      * Resolver that maps beans to their (item) identifiers, removing the need
      * to explicitly specify item identifiers when there is no need to customize
      * this.
      * 
      * Note that beans can also be added with an explicit id even if a resolver
      * has been set.
      * 
      * @param <IDTYPE>
      * @param <BEANTYPE>
      * 
      * @since 6.5
      */
     public static interface BeanIdResolver<IDTYPE, BEANTYPE> extends
             Serializable {
         /**
          * Return the item identifier for a bean.
          * 
          * @param bean
          * @return
          */
         public IDTYPE getIdForBean(BEANTYPE bean);
     }
 
     /**
      * A item identifier resolver that returns the value of a bean property.
      * 
      * The bean must have a getter for the property, and the getter must return
      * an object of type IDTYPE.
      */
     protected class PropertyBasedBeanIdResolver implements
             BeanIdResolver<IDTYPE, BEANTYPE> {
 
         private final Object propertyId;
         private transient Method getMethod;
 
         public PropertyBasedBeanIdResolver(Object propertyId) {
             if (propertyId == null) {
                 throw new IllegalArgumentException(
                         "Property identifier must not be null");
             }
             this.propertyId = propertyId;
         }
 
         private Method getGetter() throws IllegalStateException {
             if (getMethod == null) {
                 if (!model.containsKey(propertyId)) {
                     throw new IllegalStateException("Property " + propertyId
                             + " not found");
                 }
                 getMethod = model.get(propertyId).getReadMethod();
             }
             return getMethod;
         }
 
         @SuppressWarnings("unchecked")
         public IDTYPE getIdForBean(BEANTYPE bean)
                 throws IllegalArgumentException {
             try {
                 return (IDTYPE) getGetter().invoke(bean);
             } catch (IllegalAccessException e) {
                 throw new IllegalArgumentException(e);
             } catch (InvocationTargetException e) {
                 throw new IllegalArgumentException(e);
             }
         }
 
     }
 
     /**
      * The resolver that finds the item ID for a bean, or null not to use
      * automatic resolving.
      * 
      * Methods that add a bean without specifying an ID must not be called if no
      * resolver has been set.
      */
     private BeanIdResolver<IDTYPE, BEANTYPE> beanIdResolver = null;
 
     /**
      * The item sorter which is used for sorting the container.
      */
     private ItemSorter itemSorter = new DefaultItemSorter();
 
     /**
      * Filters currently applied to the container.
      */
     private Set<Filter> filters = new HashSet<Filter>();
 
     /**
      * Maps all item ids in the container (including filtered) to their
      * corresponding BeanItem.
      */
     private final Map<IDTYPE, BeanItem<BEANTYPE>> itemIdToItem = new HashMap<IDTYPE, BeanItem<BEANTYPE>>();
 
     /**
      * The type of the beans in the container.
      */
     private final Class<? super BEANTYPE> type;
 
     /**
      * A description of the properties found in beans of type {@link #type}.
      * Determines the property ids that are present in the container.
      */
     private transient LinkedHashMap<String, PropertyDescriptor> model;
 
     /**
      * Constructs a {@code AbstractBeanContainer} for beans of the given type.
      * 
      * @param type
      *            the type of the beans that will be added to the container.
      * @throws IllegalArgumentException
      *             If {@code type} is null
      */
     protected AbstractBeanContainer(Class<? super BEANTYPE> type) {
         super(new ListSet<IDTYPE>());
         setFilteredItemIds(new ListSet<IDTYPE>());
         if (type == null) {
             throw new IllegalArgumentException(
                     "The bean type passed to AbstractBeanContainer must not be null");
         }
         this.type = type;
         model = BeanItem.getPropertyDescriptors(type);
     }
 
     /**
      * A special deserialization method that resolves {@link #model} is needed
      * as PropertyDescriptor is not {@link Serializable}.
      */
     private void readObject(java.io.ObjectInputStream in) throws IOException,
             ClassNotFoundException {
         in.defaultReadObject();
         model = BeanItem.getPropertyDescriptors(type);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#getType(java.lang.Object)
      */
     public Class<?> getType(Object propertyId) {
         return model.get(propertyId).getPropertyType();
     }
 
     /**
      * Create a BeanItem for a bean using pre-parsed bean metadata (based on
      * {@link #getBeanType()}).
      * 
      * @param bean
      * @return
      */
     protected BeanItem<BEANTYPE> createBeanItem(BEANTYPE bean) {
         return new BeanItem<BEANTYPE>(bean, model);
     }
 
     /**
      * Returns the type of beans this Container can contain.
      * 
      * This comes from the bean type constructor parameter, and bean metadata
      * (including container properties) is based on this.
      * 
      * @return
      */
     public Class<? super BEANTYPE> getBeanType() {
         return type;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#getContainerPropertyIds()
      */
     public Collection<String> getContainerPropertyIds() {
         return model.keySet();
     }
 
     /**
      * Unsupported operation. Properties are determined by the introspecting the
      * bean class.
      */
     public boolean addContainerProperty(Object propertyId, Class<?> type,
             Object defaultValue) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Unsupported operation. Properties are determined by the introspecting the
      * bean class.
      */
     public boolean removeContainerProperty(Object propertyId)
             throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#removeAllItems()
      */
     public boolean removeAllItems() {
         internalRemoveAllItems();
 
         // detach listeners from all Items
         for (Item item : itemIdToItem.values()) {
             removeAllValueChangeListeners(item);
         }
         itemIdToItem.clear();
 
         fireItemSetChange();
 
         return true;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#getItem(java.lang.Object)
      */
     @Override
     public BeanItem<BEANTYPE> getItem(Object itemId) {
         return itemIdToItem.get(itemId);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#getItemIds()
      */
     @Override
     @SuppressWarnings("unchecked")
     public Collection<IDTYPE> getItemIds() {
         return (Collection<IDTYPE>) super.getItemIds();
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#getContainerProperty(java.lang.Object,
      * java.lang.Object)
      */
     public Property getContainerProperty(Object itemId, Object propertyId) {
         Item item = getItem(itemId);
         if (item == null) {
             return null;
         }
         return item.getItemProperty(propertyId);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container#removeItem(java.lang.Object)
      */
     public boolean removeItem(Object itemId) {
         Item item = getItem(itemId);
 
         if (internalRemoveItem(itemId)) {
             // detach listeners from Item
             removeAllValueChangeListeners(item);
 
             // remove item
             itemIdToItem.remove(itemId);
 
             fireItemSetChange();
 
             return true;
         } else {
             return false;
         }
     }
 
     /**
      * Re-filter the container when one of the monitored properties changes.
      */
     public void valueChange(ValueChangeEvent event) {
         // if a property that is used in a filter is changed, refresh filtering
         filterAll();
     }
 
     /**
      * Filter the view to recreate the visible item list from the unfiltered
      * items, and send a notification if the set of visible items changed in any
      * way.
      */
     @SuppressWarnings("unchecked")
     protected void filterAll() {
         // avoid notification if the filtering had no effect
         List<IDTYPE> originalItems = getFilteredItemIds();
         // it is somewhat inefficient to do a (shallow) clone() every time
         setFilteredItemIds((List<IDTYPE>) ((ListSet<IDTYPE>) allItemIds)
                 .clone());
         for (Filter f : filters) {
             filter(f);
         }
         // check if exactly the same items are there after filtering to avoid
         // unnecessary notifications
         // this may be slow in some cases as it uses BEANTYPE.equals()
         if (!originalItems.equals(getFilteredItemIds())) {
             fireItemSetChange();
         }
     }
 
     /**
      * Returns an unmodifiable collection of filters in use by the container.
      * 
      * @return
      */
     protected Set<Filter> getFilters() {
         return Collections.unmodifiableSet(filters);
     }
 
     /**
      * Remove (from the filtered list) any items that do not match the given
      * filter.
      * 
      * @param f
      *            The filter used to determine if items should be removed
      */
     protected void filter(Filter f) {
         Iterator<IDTYPE> iterator = getFilteredItemIds().iterator();
         while (iterator.hasNext()) {
             IDTYPE itemId = iterator.next();
             if (!f.passesFilter(getItem(itemId))) {
                 iterator.remove();
             }
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * com.vaadin.data.Container.Filterable#addContainerFilter(java.lang.Object,
      * java.lang.String, boolean, boolean)
      */
     @SuppressWarnings("unchecked")
     public void addContainerFilter(Object propertyId, String filterString,
             boolean ignoreCase, boolean onlyMatchPrefix) {
         if (filters.isEmpty()) {
             setFilteredItemIds((List<IDTYPE>) ((ListSet<IDTYPE>) allItemIds)
                     .clone());
         }
         // listen to change events to be able to update filtering
         for (Item item : itemIdToItem.values()) {
             addValueChangeListener(item, propertyId);
         }
         Filter f = new Filter(propertyId, filterString, ignoreCase,
                 onlyMatchPrefix);
         filter(f);
         filters.add(f);
         fireItemSetChange();
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container.Filterable#removeAllContainerFilters()
      */
     public void removeAllContainerFilters() {
         if (!filters.isEmpty()) {
             filters = new HashSet<Filter>();
             // stop listening to change events for any property
             for (Item item : itemIdToItem.values()) {
                 removeAllValueChangeListeners(item);
             }
             filterAll();
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * com.vaadin.data.Container.Filterable#removeContainerFilters(java.lang
      * .Object)
      */
     public void removeContainerFilters(Object propertyId) {
         if (!filters.isEmpty()) {
             boolean filteringChanged = false;
             for (Iterator<Filter> iterator = filters.iterator(); iterator
                     .hasNext();) {
                 Filter f = iterator.next();
                 if (f.propertyId.equals(propertyId)) {
                     iterator.remove();
                     filteringChanged = true;
                 }
             }
             if (filteringChanged) {
                 // stop listening to change events for the property
                 for (Item item : itemIdToItem.values()) {
                     removeValueChangeListener(item, propertyId);
                 }
                 filterAll();
             }
         }
     }
 
     /**
      * Make this container listen to the given property provided it notifies
      * when its value changes.
      * 
      * @param item
      *            The {@link Item} that contains the property
      * @param propertyId
      *            The id of the property
      */
     private void addValueChangeListener(Item item, Object propertyId) {
         Property property = item.getItemProperty(propertyId);
         if (property instanceof ValueChangeNotifier) {
             // avoid multiple notifications for the same property if
             // multiple filters are in use
             ValueChangeNotifier notifier = (ValueChangeNotifier) property;
             notifier.removeListener(this);
             notifier.addListener(this);
         }
     }
 
     /**
      * Remove this container as a listener for the given property.
      * 
      * @param item
      *            The {@link Item} that contains the property
      * @param propertyId
      *            The id of the property
      */
     private void removeValueChangeListener(Item item, Object propertyId) {
         Property property = item.getItemProperty(propertyId);
         if (property instanceof ValueChangeNotifier) {
             ((ValueChangeNotifier) property).removeListener(this);
         }
     }
 
     /**
      * Remove this contains as a listener for all the properties in the given
      * {@link Item}.
      * 
      * @param item
      *            The {@link Item} that contains the properties
      */
     private void removeAllValueChangeListeners(Item item) {
         for (Object propertyId : item.getItemPropertyIds()) {
             removeValueChangeListener(item, propertyId);
         }
     }
 
     /**
      * Unsupported operation. Use other methods to add items.
      */
     public Object addItemAfter(Object previousItemId)
             throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Unsupported operation. Beans should be added through
      * {@code addItemAt(int, ...)}.
      */
     public Object addItemAt(int index) throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container.Sortable#getSortableContainerPropertyIds()
      */
     public Collection<Object> getSortableContainerPropertyIds() {
         LinkedList<Object> sortables = new LinkedList<Object>();
         for (Object propertyId : getContainerPropertyIds()) {
             Class<?> propertyType = getType(propertyId);
             if (Comparable.class.isAssignableFrom(propertyType)
                     || propertyType.isPrimitive()) {
                 sortables.add(propertyId);
             }
         }
         return sortables;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see com.vaadin.data.Container.Sortable#sort(java.lang.Object[],
      * boolean[])
      */
     public void sort(Object[] propertyId, boolean[] ascending) {
         itemSorter.setSortProperties(this, propertyId, ascending);
 
         doSort();
 
         // notifies if anything changes in the filtered list, including order
         filterAll();
     }
 
     /**
      * Perform the sorting of the data structures in the container. This is
      * invoked when the <code>itemSorter</code> has been prepared for the sort
      * operation. Typically this method calls
      * <code>Collections.sort(aCollection, getItemSorter())</code> on all arrays
      * (containing item ids) that need to be sorted.
      * 
      */
     protected void doSort() {
         Collections.sort(allItemIds, getItemSorter());
     }
 
     /**
      * Returns the ItemSorter that is used for sorting the container.
      * 
      * @see #setItemSorter(ItemSorter)
      * 
      * @return The ItemSorter that is used for sorting the container
      */
     public ItemSorter getItemSorter() {
         return itemSorter;
     }
 
     /**
      * Sets the ItemSorter that is used for sorting the container. The
      * {@link ItemSorter#compare(Object, Object)} method is called to compare
      * two beans (item ids).
      * 
      * @param itemSorter
      *            The ItemSorter to use when sorting the container
      */
     public void setItemSorter(ItemSorter itemSorter) {
         this.itemSorter = itemSorter;
     }
 
     /**
      * Unsupported operation. See subclasses of {@link AbstractBeanContainer}
      * for the correct way to add items.
      */
     public Object addItem() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Adds the bean to all internal data structures at the given position.
      * Fails if the bean is already in the container or is not assignable to the
      * correct type. Returns a new BeanItem if the bean was added successfully.
      * 
      * <p>
      * Caller should call {@link #filterAll()} after calling this method to
      * ensure the filtered list is updated.
      * </p>
      * 
      * For internal use by subclasses only. This API is experimental and subject
      * to change.
      * 
      * @param position
      *            The position at which the bean should be inserted in the
      *            unfiltered collection of items
      * @param itemId
      *            The item identifier for the bean to insert
      * @param bean
      *            The bean to insert
      * 
      * @return BeanItem<BEANTYPE> if the bean was added successfully, null
      *         otherwise
      */
     protected BeanItem<BEANTYPE> internalAddAt(int position, IDTYPE itemId,
             BEANTYPE bean) {
         if (bean == null) {
             return null;
         }
         // Make sure that the item has not been added previously
        if (allItemIds.contains(itemId)) {
             return null;
         }
 
         if (!getBeanType().isAssignableFrom(bean.getClass())) {
             return null;
         }
 
         // "filteredList" will be updated in filterAll() which should be invoked
         // by the caller after calling this method.
         allItemIds.add(position, itemId);
         BeanItem<BEANTYPE> beanItem = createBeanItem(bean);
         itemIdToItem.put(itemId, beanItem);
 
         // add listeners to be able to update filtering on property
         // changes
         for (Filter filter : getFilters()) {
             // addValueChangeListener avoids adding duplicates
             addValueChangeListener(beanItem, filter.propertyId);
         }
 
         return beanItem;
     }
 
     /**
      * Returns the index of an item within the unfiltered collection of items.
      * 
      * For internal use by subclasses only. This API is experimental and subject
      * to change.
      * 
      * @param itemId
      * @return
      */
     protected int internalIndexOf(IDTYPE itemId) {
         return allItemIds.indexOf(itemId);
     }
 
     /**
      * Adds a bean at the given index of the internal (unfiltered) list.
      * <p>
      * The item is also added in the visible part of the list if it passes the
      * filters.
      * </p>
      * 
      * @param index
      *            Internal index to add the new item.
      * @param newItemId
      *            Id of the new item to be added.
      * @param bean
      *            bean to be added
      * @return Returns new item or null if the operation fails.
      */
     private BeanItem<BEANTYPE> addItemAtInternalIndex(int index,
             IDTYPE newItemId, BEANTYPE bean) {
         BeanItem<BEANTYPE> beanItem = internalAddAt(index, newItemId, bean);
         if (beanItem != null) {
             filterAll();
         }
 
         return beanItem;
     }
 
     /**
      * Adds the bean to the Container.
      * 
      * @see com.vaadin.data.Container#addItem(Object)
      */
     protected BeanItem<BEANTYPE> addItem(IDTYPE itemId, BEANTYPE bean) {
         if (size() > 0) {
             // add immediately after last visible item
             int lastIndex = internalIndexOf(lastItemId());
             return addItemAtInternalIndex(lastIndex + 1, itemId, bean);
         } else {
             return addItemAtInternalIndex(0, itemId, bean);
         }
     }
 
     /**
      * Adds the bean after the given bean.
      * 
      * @see com.vaadin.data.Container.Ordered#addItemAfter(Object, Object)
      */
     protected BeanItem<BEANTYPE> addItemAfter(IDTYPE previousItemId,
             IDTYPE newItemId, BEANTYPE bean) {
         // only add if the previous item is visible
         if (previousItemId == null) {
             return addItemAtInternalIndex(0, newItemId, bean);
         } else if (containsId(previousItemId)) {
             return addItemAtInternalIndex(internalIndexOf(previousItemId) + 1,
                     newItemId, bean);
         } else {
             return null;
         }
     }
 
     /**
      * Adds a new bean at the given index.
      * 
      * The bean is used both as the item contents and as the item identifier.
      * 
      * @param index
      *            Index at which the bean should be added.
      * @param newItemId
      *            The item id for the bean to add to the container.
      * @param bean
      *            The bean to add to the container.
      * 
      * @return Returns the new BeanItem or null if the operation fails.
      */
     protected BeanItem<BEANTYPE> addItemAt(int index, IDTYPE newItemId,
             BEANTYPE bean) {
         if (index < 0 || index > size()) {
             return null;
         } else if (index == 0) {
             // add before any item, visible or not
             return addItemAtInternalIndex(0, newItemId, bean);
         } else {
             // if index==size(), adds immediately after last visible item
             return addItemAfter(getIdByIndex(index - 1), newItemId, bean);
         }
     }
 
     /**
      * Adds a bean to the container using the bean item id resolver to find its
      * identifier.
      * 
      * A bean id resolver must be set before calling this method.
      * 
      * @see #addItem(Object, Object)
      * 
      * @param bean
      *            the bean to add
      * @return BeanItem<BEANTYPE> item added or null
      * @throws IllegalStateException
      *             if no bean identifier resolver has been set
      * @throws IllegalArgumentException
      *             if an identifier cannot be resolved for the bean
      */
     protected BeanItem<BEANTYPE> addBean(BEANTYPE bean)
             throws IllegalStateException, IllegalArgumentException {
         if (beanIdResolver == null) {
             throw new IllegalStateException(
                     "Bean item identifier resolver is required.");
         }
         if (bean == null) {
             return null;
         }
         IDTYPE itemId = beanIdResolver.getIdForBean(bean);
         if (itemId == null) {
             throw new IllegalArgumentException(
                     "Resolved identifier for a bean must not be null");
         }
         return addItem(itemId, bean);
     }
 
     /**
      * Adds a bean to the container after a specified item identifier, using the
      * bean item id resolver to find its identifier.
      * 
      * A bean id resolver must be set before calling this method.
      * 
      * @see #addItemAfter(Object, Object, Object)
      * 
      * @param previousItemId
      *            the identifier of the bean after which this bean should be
      *            added, null to add to the beginning
      * @param bean
      *            the bean to add
      * @return BeanItem<BEANTYPE> item added or null
      * @throws IllegalStateException
      *             if no bean identifier resolver has been set
      * @throws IllegalArgumentException
      *             if an identifier cannot be resolved for the bean
      */
     protected BeanItem<BEANTYPE> addBeanAfter(IDTYPE previousItemId,
             BEANTYPE bean) throws IllegalStateException,
             IllegalArgumentException {
         if (beanIdResolver == null) {
             throw new IllegalStateException(
                     "Bean item identifier resolver is required.");
         }
         if (bean == null) {
             return null;
         }
         IDTYPE itemId = beanIdResolver.getIdForBean(bean);
         if (itemId == null) {
             throw new IllegalArgumentException(
                     "Resolved identifier for a bean must not be null");
         }
         return addItemAfter(previousItemId, itemId, bean);
     }
 
     /**
      * Adds a bean at a specified (filtered view) position in the container
      * using the bean item id resolver to find its identifier.
      * 
      * A bean id resolver must be set before calling this method.
      * 
      * @see #addItemAfter(Object, Object, Object)
      * 
      * @param index
      *            the index (in the filtered view) at which to add the item
      * @param bean
      *            the bean to add
      * @return BeanItem<BEANTYPE> item added or null
      * @throws IllegalStateException
      *             if no bean identifier resolver has been set
      * @throws IllegalArgumentException
      *             if an identifier cannot be resolved for the bean
      */
     protected BeanItem<BEANTYPE> addBeanAt(int index, BEANTYPE bean)
             throws IllegalStateException, IllegalArgumentException {
         if (beanIdResolver == null) {
             throw new IllegalStateException(
                     "Bean item identifier resolver is required.");
         }
         if (bean == null) {
             return null;
         }
         IDTYPE itemId = beanIdResolver.getIdForBean(bean);
         if (itemId == null) {
             throw new IllegalArgumentException(
                     "Resolved identifier for a bean must not be null");
         }
         return addItemAt(index, itemId, bean);
     }
 
     /**
      * Adds all the beans from a {@link Collection} in one operation using the
      * bean item identifier resolver. More efficient than adding them one by
      * one.
      * 
      * A bean id resolver must be set before calling this method.
      * 
      * @param collection
      *            The collection of beans to add. Must not be null.
      * @throws IllegalStateException
      *             if no bean identifier resolver has been set
      */
     protected void addAll(Collection<? extends BEANTYPE> collection)
             throws IllegalStateException {
         if (beanIdResolver == null) {
             throw new IllegalStateException(
                     "Bean item identifier resolver is required.");
         }
 
         int idx = internalIndexOf(lastItemId()) + 1;
         for (BEANTYPE bean : collection) {
             IDTYPE itemId = beanIdResolver.getIdForBean(bean);
             if (internalAddAt(idx, itemId, bean) != null) {
                 idx++;
             }
         }
 
         // Filter the contents when all items have been added
         filterAll();
     }
 
     /**
      * Sets the resolver that finds the item id for a bean, or null not to use
      * automatic resolving.
      * 
      * Methods that add a bean without specifying an id must not be called if no
      * resolver has been set.
      * 
      * Note that methods taking an explicit id can be used whether a resolver
      * has been defined or not.
      * 
      * @param beanIdResolver
      *            to use or null to disable automatic id resolution
      */
     protected void setBeanIdResolver(
             BeanIdResolver<IDTYPE, BEANTYPE> beanIdResolver) {
         this.beanIdResolver = beanIdResolver;
     }
 
     /**
      * Returns the resolver that finds the item ID for a bean.
      * 
      * @return resolver used or null if automatic item id resolving is disabled
      */
     public BeanIdResolver<IDTYPE, BEANTYPE> getBeanIdResolver() {
         return beanIdResolver;
     }
 
     /**
      * Create an item identifier resolver using a named bean property.
      * 
      * @param propertyId
      *            property identifier, which must map to a getter in BEANTYPE
      * @return created resolver
      */
     protected BeanIdResolver<IDTYPE, BEANTYPE> createBeanPropertyResolver(
             Object propertyId) {
         return new PropertyBasedBeanIdResolver(propertyId);
     }
 
 }
