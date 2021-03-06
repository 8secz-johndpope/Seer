 /* *************************************************************************
  
  IT Mill Toolkit 
 
  Development of Browser User Interfaces Made Easy
 
  Copyright (C) 2000-2006 IT Mill Ltd
  
  *************************************************************************
 
  This product is distributed under commercial license that can be found
  from the product package on license.pdf. Use of this product might 
  require purchasing a commercial license from IT Mill Ltd. For guidelines 
  on usage, see licensing-guidelines.html
 
  *************************************************************************
  
  For more information, contact:
  
  IT Mill Ltd                           phone: +358 2 4802 7180
  Ruukinkatu 2-4                        fax:   +358 2 4802 7181
  20540, Turku                          email:  info@itmill.com
  Finland                               company www: www.itmill.com
  
  Primary source for information and releases: www.itmill.com
 
  ********************************************************************** */
 
 package com.itmill.toolkit.ui;
 
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.Set;
 import java.util.Stack;
 import java.util.StringTokenizer;
 
 import com.itmill.toolkit.data.Container;
 import com.itmill.toolkit.data.util.ContainerHierarchicalWrapper;
 import com.itmill.toolkit.event.Action;
 import com.itmill.toolkit.terminal.KeyMapper;
 import com.itmill.toolkit.terminal.PaintException;
 import com.itmill.toolkit.terminal.PaintTarget;
 import com.itmill.toolkit.terminal.Resource;
 
 /**
  * MenuTree component. MenuTree can be used to select an item (or multiple
  * items) from a hierarchical set of items.
  * 
  * @author IT Mill Ltd.
  * @version
  * @VERSION@
  * @since 3.0
  */
 public class Tree extends AbstractSelect implements Container.Hierarchical,
 		Action.Container {
 
 	/* Static members ***************************************************** */
 
 	private static final Method EXPAND_METHOD;
 
 	private static final Method COLLAPSE_METHOD;
 
 	static {
 		try {
 			EXPAND_METHOD = ExpandListener.class.getDeclaredMethod(
 					"nodeExpand", new Class[] { ExpandEvent.class });
 			COLLAPSE_METHOD = CollapseListener.class.getDeclaredMethod(
 					"nodeCollapse", new Class[] { CollapseEvent.class });
 		} catch (java.lang.NoSuchMethodException e) {
 			// This should never happen
 			e.printStackTrace();
 			throw new java.lang.RuntimeException(
 					"Internal error, please report");
 		}
 	}
 
 	/* Private members **************************************************** */
 
 	/**
 	 * Set of expanded nodes.
 	 */
 	private final HashSet expanded = new HashSet();
 
 	/**
 	 * List of action handlers.
 	 */
 	private LinkedList actionHandlers = null;
 
 	/**
 	 * Action mapper.
 	 */
 	private KeyMapper actionMapper = null;
 
 	/**
 	 * Is the tree selectable .
 	 */
 	private boolean selectable = true;
 
 	/**
 	 * Flag to indicate sub-tree loading
 	 */
 	private boolean partialUpdate = false;
 
 	/**
 	 * Holds a itemId which was recently expanded
 	 */
 	private Object expandedItemId;
 
 	/**
 	 * a flag which indicates initial paint. After this flag set true partial
 	 * updates are allowed.
 	 */
 	private boolean initialPaint = true;
 
 	/* Tree constructors ************************************************** */
 
 	/**
 	 * Creates a new empty tree.
 	 */
 	public Tree() {
 	}
 
 	/**
 	 * Creates a new empty tree with caption.
 	 * 
 	 * @param caption
 	 */
 	public Tree(String caption) {
 		setCaption(caption);
 	}
 
 	/**
 	 * Creates a new tree with caption and connect it to a Container.
 	 * 
 	 * @param caption
 	 * @param dataSource
 	 */
 	public Tree(String caption, Container dataSource) {
 		setCaption(caption);
 		setContainerDataSource(dataSource);
 	}
 
 	/* Expanding and collapsing ******************************************* */
 
 	/**
 	 * Check is an item is expanded
 	 * 
 	 * @param itemId
 	 *            the item id.
 	 * @return true iff the item is expanded.
 	 */
 	public boolean isExpanded(Object itemId) {
 		return this.expanded.contains(itemId);
 	}
 
 	/**
 	 * Expands an item.
 	 * 
 	 * @param itemId
 	 *            the item id.
 	 * @return True iff the expand operation succeeded
 	 */
 	public boolean expandItem(Object itemId) {
 
 		// Succeeds if the node is already expanded
 		if (isExpanded(itemId)) {
 			return true;
 		}
 
 		// Nodes that can not have children are not expandable
 		if (!areChildrenAllowed(itemId)) {
 			return false;
 		}
 
 		// Expands
 		this.expanded.add(itemId);
 
 		this.expandedItemId = itemId;
 		if (this.initialPaint) {
 			requestRepaint();
 		} else {
 			requestPartialRepaint();
 		}
 		fireExpandEvent(itemId);
 
 		return true;
 	}
 
 	public void requestRepaint() {
 		super.requestRepaint();
 		this.partialUpdate = false;
 	}
 
 	private void requestPartialRepaint() {
 		super.requestRepaint();
 		this.partialUpdate = true;
 	}
 
 	/**
 	 * Expands the items recursively
 	 * 
 	 * Expands all the children recursively starting from an item. Operation
 	 * succeeds only if all expandable items are expanded.
 	 * 
 	 * @param startItemId
 	 * @return True iff the expand operation succeeded
 	 */
 	public boolean expandItemsRecursively(Object startItemId) {
 
 		boolean result = true;
 
 		// Initial stack
 		Stack todo = new Stack();
 		todo.add(startItemId);
 
 		// Expands recursively
 		while (!todo.isEmpty()) {
 			Object id = todo.pop();
 			if (areChildrenAllowed(id) && !expandItem(id)) {
 				result = false;
 			}
 			if (hasChildren(id)) {
 				todo.addAll(getChildren(id));
 			}
 		}
 
 		return result;
 	}
 
 	/**
 	 * Collapses an item.
 	 * 
 	 * @param itemId
 	 *            the item id.
 	 * @return True iff the collapse operation succeeded
 	 */
 	public boolean collapseItem(Object itemId) {
 
 		// Succeeds if the node is already collapsed
 		if (!isExpanded(itemId)) {
 			return true;
 		}
 
 		// Collapse
 		this.expanded.remove(itemId);
 		requestRepaint();
 		fireCollapseEvent(itemId);
 
 		return true;
 	}
 
 	/**
 	 * Collapses the items recursively.
 	 * 
 	 * Collapse all the children recursively starting from an item. Operation
 	 * succeeds only if all expandable items are collapsed.
 	 * 
 	 * @param startItemId
 	 * @return True iff the collapse operation succeeded
 	 */
 	public boolean collapseItemsRecursively(Object startItemId) {
 
 		boolean result = true;
 
 		// Initial stack
 		Stack todo = new Stack();
 		todo.add(startItemId);
 
 		// Collapse recursively
 		while (!todo.isEmpty()) {
 			Object id = todo.pop();
 			if (areChildrenAllowed(id) && !collapseItem(id)) {
 				result = false;
 			}
 			if (hasChildren(id)) {
 				todo.addAll(getChildren(id));
 			}
 		}
 
 		return result;
 	}
 
 	/**
 	 * Getter for property selectable.
 	 * 
 	 * <p>
 	 * The tree is selectable by default.
 	 * </p>
 	 * 
 	 * @return the Value of property selectable.
 	 */
 	public boolean isSelectable() {
 		return this.selectable;
 	}
 
 	/**
 	 * Setter for property selectable.
 	 * 
 	 * <p>
 	 * The tree is selectable by default.
 	 * </p>
 	 * 
 	 * @param selectable
 	 *            the New value of property selectable.
 	 */
 	public void setSelectable(boolean selectable) {
 		if (this.selectable != selectable) {
 			this.selectable = selectable;
 			requestRepaint();
 		}
 	}
 
 	/* Component API ****************************************************** */
 
 	/**
 	 * Gets the UIDL tag corresponding to the component.
 	 * 
 	 * @see com.itmill.toolkit.ui.AbstractComponent#getTag()
 	 */
 	public String getTag() {
 		return "tree";
 	}
 
 	/**
 	 * Called when one or more variables handled by the implementing class are
 	 * changed.
 	 * 
 	 * @see com.itmill.toolkit.terminal.VariableOwner#changeVariables(Object
 	 *      source, Map variables)
 	 */
 	public void changeVariables(Object source, Map variables) {
 
 		if (!isSelectable() && variables.containsKey("selected")) {
 			// Not-selectable is a special case, AbstractSelect does not support
 			// TODO could be optimized.
 			variables = new HashMap(variables);
 			variables.remove("selected");
 		}
 
 		// Collapses the nodes
 		if (variables.containsKey("collapse")) {
 			String[] keys = (String[]) variables.get("collapse");
 			for (int i = 0; i < keys.length; i++) {
 				Object id = this.itemIdMapper.get(keys[i]);
 				if (id != null && isExpanded(id)) {
 					this.expanded.remove(id);
 					fireCollapseEvent(id);
 				}
 			}
 		}
 
 		// Expands the nodes
 		if (variables.containsKey("expand")) {
 			String[] keys = (String[]) variables.get("expand");
 			for (int i = 0; i < keys.length; i++) {
 				Object id = this.itemIdMapper.get(keys[i]);
 				if (id != null) {
 					expandItem(id);
 				}
 			}
 		}
 
 		// Selections are handled by the select component
 		super.changeVariables(source, variables);
 
 		// Actions
 		if (variables.containsKey("action")) {
 
 			StringTokenizer st = new StringTokenizer((String) variables
 					.get("action"), ",");
 			if (st.countTokens() == 2) {
 				Object itemId = this.itemIdMapper.get(st.nextToken());
 				Action action = (Action) this.actionMapper.get(st.nextToken());
 				if (action != null && containsId(itemId)
 						&& this.actionHandlers != null) {
 					for (Iterator i = this.actionHandlers.iterator(); i
 							.hasNext();) {
 						((Action.Handler) i.next()).handleAction(action, this,
 								itemId);
 					}
 				}
 			}
 		}
 	}
 
 	/**
 	 * Paints any needed component-specific things to the given UIDL stream.
 	 * 
 	 * @see com.itmill.toolkit.ui.AbstractComponent#paintContent(PaintTarget)
 	 */
 	public void paintContent(PaintTarget target) throws PaintException {
 		this.initialPaint = false;
 
 		if (this.partialUpdate) {
 			target.addAttribute("partialUpdate", true);
 			target.addAttribute("rootKey", this.itemIdMapper
 					.key(this.expandedItemId));
 		} else {
 
 			// Focus control id
 			if (getFocusableId() > 0) {
 				target.addAttribute("focusid", getFocusableId());
 			}
 
 			// The tab ordering number
 			if (getTabIndex() > 0) {
 				target.addAttribute("tabindex", getTabIndex());
 			}
 
 			// Paint tree attributes
 			if (isSelectable()) {
 				target.addAttribute("selectmode", (isMultiSelect() ? "multi"
 						: "single"));
 			} else {
 				target.addAttribute("selectmode", "none");
 			}
 			if (isNewItemsAllowed()) {
 				target.addAttribute("allownewitem", true);
 			}
 
			if (isNullSelectionAllowed()) {
				target.addAttribute("nullselect", true);
			}

 		}
 
 		// Initialize variables
 		Set actionSet = new LinkedHashSet();
 		String[] selectedKeys;
 		if (isMultiSelect()) {
 			selectedKeys = new String[((Set) getValue()).size()];
 		} else {
 			selectedKeys = new String[(getValue() == null ? 0 : 1)];
 		}
 		int keyIndex = 0;
 		LinkedList expandedKeys = new LinkedList();
 
 		// Iterates through hierarchical tree using a stack of iterators
 		Stack iteratorStack = new Stack();
 		Collection ids;
 		if (this.partialUpdate) {
 			ids = getChildren(this.expandedItemId);
 		} else {
 			ids = rootItemIds();
 		}
 
 		if (ids != null) {
 			iteratorStack.push(ids.iterator());
 		}
 
 		while (!iteratorStack.isEmpty()) {
 
 			// Gets the iterator for current tree level
 			Iterator i = (Iterator) iteratorStack.peek();
 
 			// If the level is finished, back to previous tree level
 			if (!i.hasNext()) {
 
 				// Removes used iterator from the stack
 				iteratorStack.pop();
 
 				// Closes node
 				if (!iteratorStack.isEmpty()) {
 					target.endTag("node");
 				}
 			}
 
 			// Adds the item on current level
 			else {
 				Object itemId = i.next();
 
 				// Starts the item / node
 				boolean isNode = areChildrenAllowed(itemId)
 						&& hasChildren(itemId);
 				if (isNode) {
 					target.startTag("node");
 				} else {
 					target.startTag("leaf");
 				}
 
 				// Adds the attributes
 				target.addAttribute("caption", getItemCaption(itemId));
 				Resource icon = getItemIcon(itemId);
 				if (icon != null) {
 					target.addAttribute("icon", getItemIcon(itemId));
 				}
 				String key = this.itemIdMapper.key(itemId);
 				target.addAttribute("key", key);
 				if (isSelected(itemId)) {
 					target.addAttribute("selected", true);
 					selectedKeys[keyIndex++] = key;
 				}
 				if (areChildrenAllowed(itemId) && isExpanded(itemId)) {
 					target.addAttribute("expanded", true);
 					expandedKeys.add(key);
 				}
 
 				// Actions
 				if (this.actionHandlers != null) {
 					ArrayList keys = new ArrayList();
 					for (Iterator ahi = this.actionHandlers.iterator(); ahi
 							.hasNext();) {
 						Action[] aa = ((Action.Handler) ahi.next()).getActions(
 								itemId, this);
 						if (aa != null) {
 							for (int ai = 0; ai < aa.length; ai++) {
 								String akey = this.actionMapper.key(aa[ai]);
 								actionSet.add(aa[ai]);
 								keys.add(akey);
 							}
 						}
 					}
 					target.addAttribute("al", keys.toArray());
 				}
 
 				// Adds the children if expanded, or close the tag
 				if (isExpanded(itemId) && hasChildren(itemId)
 						&& areChildrenAllowed(itemId)) {
 					iteratorStack.push(getChildren(itemId).iterator());
 				} else {
 					if (isNode) {
 						target.endTag("node");
 					} else {
 						target.endTag("leaf");
 					}
 				}
 			}
 		}
 
 		// Actions
 		if (!actionSet.isEmpty()) {
 			target.addVariable(this, "action", "");
 			target.startTag("actions");
 			for (Iterator i = actionSet.iterator(); i.hasNext();) {
 				Action a = (Action) i.next();
 				target.startTag("action");
 				if (a.getCaption() != null) {
 					target.addAttribute("caption", a.getCaption());
 				}
 				if (a.getIcon() != null) {
 					target.addAttribute("icon", a.getIcon());
 				}
 				target.addAttribute("key", this.actionMapper.key(a));
 				target.endTag("action");
 			}
 			target.endTag("actions");
 		}
 
 		if (this.partialUpdate) {
 			this.partialUpdate = false;
 		} else {
 			// Selected
 			target.addVariable(this, "selected", selectedKeys);
 
 			// Expand and collapse
 			target.addVariable(this, "expand", new String[] {});
 			target.addVariable(this, "collapse", new String[] {});
 
 			// New items
 			target.addVariable(this, "newitem", new String[] {});
 		}
 	}
 
 	/* Container.Hierarchical API ***************************************** */
 
 	/**
 	 * Tests if the Item with given ID can have any children.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#areChildrenAllowed(Object)
 	 */
 	public boolean areChildrenAllowed(Object itemId) {
 		return ((Container.Hierarchical) this.items).areChildrenAllowed(itemId);
 	}
 
 	/**
 	 * Gets the IDs of all Items that are children of the specified Item.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#getChildren(Object)
 	 */
 	public Collection getChildren(Object itemId) {
 		return ((Container.Hierarchical) this.items).getChildren(itemId);
 	}
 
 	/**
 	 * Gets the ID of the parent Item of the specified Item.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#getParent(Object)
 	 */
 	public Object getParent(Object itemId) {
 		return ((Container.Hierarchical) this.items).getParent(itemId);
 	}
 
 	/**
 	 * Tests if the Item specified with <code>itemId</code> has any child
 	 * Items, that is, is it a leaf Item.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#hasChildren(Object)
 	 */
 	public boolean hasChildren(Object itemId) {
 		return ((Container.Hierarchical) this.items).hasChildren(itemId);
 	}
 
 	/**
 	 * Tests if the Item specified with <code>itemId</code> is a root Item.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#isRoot(Object)
 	 */
 	public boolean isRoot(Object itemId) {
 		return ((Container.Hierarchical) this.items).isRoot(itemId);
 	}
 
 	/**
 	 * Gets the IDs of all Items in the container that don't have a parent.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#rootItemIds()
 	 */
 	public Collection rootItemIds() {
 		return ((Container.Hierarchical) this.items).rootItemIds();
 	}
 
 	/**
 	 * Sets the given Item's capability to have children.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#setChildrenAllowed(Object,
 	 *      boolean)
 	 */
 	public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) {
 		boolean success = ((Container.Hierarchical) this.items)
 				.setChildrenAllowed(itemId, areChildrenAllowed);
 		if (success) {
 			fireValueChange(false);
 		}
 		return success;
 	}
 
 	/**
 	 * Sets the parent of an Item.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Hierarchical#setParent(Object,
 	 *      Object)
 	 */
 	public boolean setParent(Object itemId, Object newParentId) {
 		boolean success = ((Container.Hierarchical) this.items).setParent(
 				itemId, newParentId);
 		if (success) {
 			requestRepaint();
 		}
 		return success;
 	}
 
 	/* Overriding select behavior******************************************** */
 
 	/**
 	 * Sets the Container that serves as the data source of the viewer.
 	 * 
 	 * @see com.itmill.toolkit.data.Container.Viewer#setContainerDataSource(Container)
 	 */
 	public void setContainerDataSource(Container newDataSource) {
 
 		// Assure that the data source is ordered by making unordered
 		// containers ordered by wrapping them
 		if (Container.Hierarchical.class.isAssignableFrom(newDataSource
 				.getClass())) {
 			super.setContainerDataSource(newDataSource);
 		} else {
 			super.setContainerDataSource(new ContainerHierarchicalWrapper(
 					newDataSource));
 		}
 	}
 
 	/* Expand event and listener ****************************************** */
 
 	/**
 	 * Event to fired when a node is expanded. ExapandEvent is fired when a node
 	 * is to be expanded. it can me used to dynamically fill the sub-nodes of
 	 * the node.
 	 * 
 	 * @author IT Mill Ltd.
 	 * @version
 	 * @VERSION@
 	 * @since 3.0
 	 */
 	public class ExpandEvent extends Component.Event {
 
 		/**
 		 * Serial generated by eclipse.
 		 */
 		private static final long serialVersionUID = 3832624001804481075L;
 
 		private final Object expandedItemId;
 
 		/**
 		 * New instance of options change event
 		 * 
 		 * @param source
 		 *            the Source of the event.
 		 * @param expandedItemId
 		 */
 		public ExpandEvent(Component source, Object expandedItemId) {
 			super(source);
 			this.expandedItemId = expandedItemId;
 		}
 
 		/**
 		 * Node where the event occurred.
 		 * 
 		 * @return the Source of the event.
 		 */
 		public Object getItemId() {
 			return this.expandedItemId;
 		}
 	}
 
 	/**
 	 * Expand event listener.
 	 * 
 	 * @author IT Mill Ltd.
 	 * @version
 	 * @VERSION@
 	 * @since 3.0
 	 */
 	public interface ExpandListener {
 
 		/**
 		 * A node has been expanded.
 		 * 
 		 * @param event
 		 *            the Expand event.
 		 */
 		public void nodeExpand(ExpandEvent event);
 	}
 
 	/**
 	 * Adds the expand listener.
 	 * 
 	 * @param listener
 	 *            the Listener to be added.
 	 */
 	public void addListener(ExpandListener listener) {
 		addListener(ExpandEvent.class, listener, EXPAND_METHOD);
 	}
 
 	/**
 	 * Removes the expand listener.
 	 * 
 	 * @param listener
 	 *            the Listener to be removed.
 	 */
 	public void removeListener(ExpandListener listener) {
 		removeListener(ExpandEvent.class, listener, EXPAND_METHOD);
 	}
 
 	/**
 	 * Emits the expand event.
 	 * 
 	 * @param itemId
 	 *            the item id.
 	 */
 	protected void fireExpandEvent(Object itemId) {
 		fireEvent(new ExpandEvent(this, itemId));
 	}
 
 	/* Collapse event ****************************************** */
 
 	/**
 	 * Collapse event
 	 * 
 	 * @author IT Mill Ltd.
 	 * @version
 	 * @VERSION@
 	 * @since 3.0
 	 */
 	public class CollapseEvent extends Component.Event {
 
 		/**
 		 * Serial generated by eclipse.
 		 */
 		private static final long serialVersionUID = 3257009834783290160L;
 
 		private final Object collapsedItemId;
 
 		/**
 		 * New instance of options change event.
 		 * 
 		 * @param source
 		 *            the Source of the event.
 		 * @param collapsedItemId
 		 */
 		public CollapseEvent(Component source, Object collapsedItemId) {
 			super(source);
 			this.collapsedItemId = collapsedItemId;
 		}
 
 		/**
 		 * Gets tge Collapsed Item id.
 		 * 
 		 * @return the collapsed item id.
 		 */
 		public Object getItemId() {
 			return this.collapsedItemId;
 		}
 	}
 
 	/**
 	 * Collapse event listener.
 	 * 
 	 * @author IT Mill Ltd.
 	 * @version
 	 * @VERSION@
 	 * @since 3.0
 	 */
 	public interface CollapseListener {
 
 		/**
 		 * A node has been collapsed.
 		 * 
 		 * @param event
 		 *            the Collapse event.
 		 */
 		public void nodeCollapse(CollapseEvent event);
 	}
 
 	/**
 	 * Adds the collapse listener.
 	 * 
 	 * @param listener
 	 *            the Listener to be added.
 	 */
 	public void addListener(CollapseListener listener) {
 		addListener(CollapseEvent.class, listener, COLLAPSE_METHOD);
 	}
 
 	/**
 	 * Removes the collapse listener.
 	 * 
 	 * @param listener
 	 *            the Listener to be removed.
 	 */
 	public void removeListener(CollapseListener listener) {
 		removeListener(CollapseEvent.class, listener, COLLAPSE_METHOD);
 	}
 
 	/**
 	 * Emits collapse event.
 	 * 
 	 * @param itemId
 	 *            the item id.
 	 */
 	protected void fireCollapseEvent(Object itemId) {
 		fireEvent(new CollapseEvent(this, itemId));
 	}
 
 	/* Action container *************************************************** */
 
 	/**
 	 * Adds an action handler.
 	 * 
 	 * @see com.itmill.toolkit.event.Action.Container#addActionHandler(Action.Handler)
 	 */
 	public void addActionHandler(Action.Handler actionHandler) {
 
 		if (actionHandler != null) {
 
 			if (this.actionHandlers == null) {
 				this.actionHandlers = new LinkedList();
 				this.actionMapper = new KeyMapper();
 			}
 
 			if (!this.actionHandlers.contains(actionHandler)) {
 				this.actionHandlers.add(actionHandler);
 				requestRepaint();
 			}
 		}
 	}
 
 	/**
 	 * Removes an action handler.
 	 * 
 	 * @see com.itmill.toolkit.event.Action.Container#removeActionHandler(Action.Handler)
 	 */
 	public void removeActionHandler(Action.Handler actionHandler) {
 
 		if (this.actionHandlers != null
 				&& this.actionHandlers.contains(actionHandler)) {
 
 			this.actionHandlers.remove(actionHandler);
 
 			if (this.actionHandlers.isEmpty()) {
 				this.actionHandlers = null;
 				this.actionMapper = null;
 			}
 
 			requestRepaint();
 		}
 	}
 
 	/**
 	 * Gets the visible item ids.
 	 * 
 	 * @see com.itmill.toolkit.ui.Select#getVisibleItemIds()
 	 */
 	public Collection getVisibleItemIds() {
 
 		LinkedList visible = new LinkedList();
 
 		// Iterates trough hierarchical tree using a stack of iterators
 		Stack iteratorStack = new Stack();
 		Collection ids = rootItemIds();
 		if (ids != null) {
 			iteratorStack.push(ids.iterator());
 		}
 		while (!iteratorStack.isEmpty()) {
 
 			// Gets the iterator for current tree level
 			Iterator i = (Iterator) iteratorStack.peek();
 
 			// If the level is finished, back to previous tree level
 			if (!i.hasNext()) {
 
 				// Removes used iterator from the stack
 				iteratorStack.pop();
 			}
 
 			// Adds the item on current level
 			else {
 				Object itemId = i.next();
 
 				visible.add(itemId);
 
 				// Adds children if expanded, or close the tag
 				if (isExpanded(itemId) && hasChildren(itemId)) {
 					iteratorStack.push(getChildren(itemId).iterator());
 				}
 			}
 		}
 
 		return visible;
 	}
 
 	/**
 	 * Adding new items is not supported.
 	 * 
 	 * @throws UnsupportedOperationException
 	 *             if set to true.
 	 * @see com.itmill.toolkit.ui.Select#setNewItemsAllowed(boolean)
 	 */
 	public void setNewItemsAllowed(boolean allowNewOptions)
 			throws UnsupportedOperationException {
 		if (allowNewOptions) {
 			throw new UnsupportedOperationException();
 		}
 	}
 
 	/**
 	 * Focusing to this component is not supported.
 	 * 
 	 * @throws UnsupportedOperationException
 	 *             if invoked.
 	 * @see com.itmill.toolkit.ui.AbstractField#focus()
 	 */
 	public void focus() throws UnsupportedOperationException {
 		throw new UnsupportedOperationException();
 	}
 
 	/**
 	 * Tree does not support lazy options loading mode. Setting this true will
 	 * throw UnsupportedOperationException.
 	 * 
 	 * @see com.itmill.toolkit.ui.Select#setLazyLoading(boolean)
 	 */
 	public void setLazyLoading(boolean useLazyLoading) {
 		if (useLazyLoading) {
 			throw new UnsupportedOperationException(
 					"Lazy options loading is not supported by Tree.");
 		}
 	}
 
 }
