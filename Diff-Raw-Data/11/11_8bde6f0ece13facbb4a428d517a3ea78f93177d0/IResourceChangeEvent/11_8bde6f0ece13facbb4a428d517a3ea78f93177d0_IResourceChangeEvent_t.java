 /*******************************************************************************
  * Copyright (c) 2000, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.resources;
 
 /**
  * Resource change events describe changes to resources.
  * <p>
  * There are currently five different types of resource change events:
  * <ul>
  *   <li>
  *    After-the-fact batch reports of arbitrary creations, 
  *    deletions and modifications to one or more resources expressed
  *    as a hierarchical resource delta. Event type is
  *    <code>PRE_BUILD</code>, and <code>getDelta</code> returns
  *    the hierarchical delta. The resource delta is rooted at the 
  *    workspace root.  These events are broadcast to interested parties immediately 
  *    before the first build of any kind in a workspace modifying operation.  If
  *    autobuilding is not enabled, these events still occur at times when autobuild
  *    would have occurred. The workspace is open for change during notification of 
  *    these events. The delta reported in this event cycle is identical across
  *    all listeners registered for this type of event.
  *    Resource changes attempted during a <code>PRE_BUILD</code> callback
  *    <b>must</b> be done in the thread doing the notification.
  *   </li>
  *   <li>
  *    After-the-fact batch reports of arbitrary creations, 
  *    deletions and modifications to one or more resources expressed
  *    as a hierarchical resource delta. Event type is
  *    <code>POST_BUILD</code>, and <code>getDelta</code> returns
  *    the hierarchical delta. The resource delta is rooted at the 
  *    workspace root.  These events are broadcast to interested parties at the
  *    end of every workspace operation in which a build of any kind occurred.
  *    If autobuilding is not enabled, these events still occur at times when autobuild
  *    would have occurred. The workspace is open for change during notification of 
  *    these events. The delta reported in this event cycle is identical across
  *    all listeners registered for this type of event.
  *    Resource changes attempted during a <code>POST_BUILD</code> callback
  *    <b>must</b> be done in the thread doing the notification.
  *   </li>
  *   <li>
  *    After-the-fact batch reports of arbitrary creations, 
  *    deletions and modifications to one or more resources expressed
  *    as a hierarchical resource delta. Event type is
  *    <code>POST_CHANGE</code>, and <code>getDelta</code> returns
  *    the hierarchical delta. The resource delta is rooted at the 
  *    workspace root.  These events are broadcast to interested parties after
  *    a set of resource changes and happen whether or not auto-building is enabled.  
  *    The workspace is closed for change during notification of these events.
  *    The delta reported in this event cycle is identical across all listeners registered for 
  *    this type of event.
  *   </li>
  *   <li>
  *    Before-the-fact reports of the impending closure of a single
  *    project. Event type is <code>PRE_CLOSE</code>, 
  *    and <code>getResource</code> returns the project being closed.
  *    The workspace is closed for change during  notification of these events.
  *   </li>
  *   <li>
  *    Before-the-fact reports of the impending deletion of a single
  *    project. Event type is <code>PRE_DELETE</code>, 
  *    and <code>getResource</code> returns the project being deleted.
  *    The workspace is closed for change during  notification of these events.
  *   </li>
  * </ul>
  * <p>
  * In order to handle additional event types that may be introduced
  * in future releases of the platform, clients should do not write code
  * that presumes the set of event types is closed.
  * </p>
  * <p>
  * This interface is not intended to be implemented by clients.
  * </p>
  */
 public interface IResourceChangeEvent {
 	/**
 	 * Event type constant (bit mask) indicating an after-the-fact 
 	 * report of creations, deletions, and modifications
 	 * to one or more resources expressed as a hierarchical
 	 * resource delta as returned by <code>getDelta</code>.
 	 * See class comments for further details.
 	 *
 	 * @see #getType()
 	 * @see #getDelta()
 	 */
 	public static final int POST_CHANGE = 1;
 
 	/**
 	 * Event type constant (bit mask) indicating a before-the-fact 
 	 * report of the impending closure of a single
 	 * project as returned by <code>getResource</code>.
 	 * See class comments for further details.
 	 *
 	 * @see #getType()
 	 * @see #getResource()
 	 */
 	public static final int PRE_CLOSE = 2;
 
 	/**
 	 * Event type constant (bit mask) indicating a before-the-fact 
 	 * report of the impending deletion of a single
 	 * project as returned by <code>getResource</code>.
 	 * See class comments for further details.
 	 *
 	 * @see #getType()
 	 * @see #getResource()
 	 */
 	public static final int PRE_DELETE = 4;
 
 	/**
 	 * @deprecated This event type has been renamed to
 	 * <code>PRE_BUILD</code>
 	 */
 	public static final int PRE_AUTO_BUILD = 8;
 
 	/**
 	 * Event type constant (bit mask) indicating an after-the-fact 
 	 * report of creations, deletions, and modifications
 	 * to one or more resources expressed as a hierarchical
 	 * resource delta as returned by <code>getDelta</code>.
 	 * See class comments for further details.
 	 *
 	 * @see #getType()
 	 * @see #getResource()
 	 * @since 3.0
 	 */
 	public static final int PRE_BUILD = 8;
 
 	/**
 	 * @deprecated This event type has been renamed to
 	 * <code>POST_BUILD</code>
 	 */
 	public static final int POST_AUTO_BUILD = 16;
 
 	/**
 	 * Event type constant (bit mask) indicating an after-the-fact 
 	 * report of creations, deletions, and modifications
 	 * to one or more resources expressed as a hierarchical
 	 * resource delta as returned by <code>getDelta</code>.
 	 * See class comments for further details.
 	 *
 	 * @see #getType()
 	 * @see #getResource()
 	 * @since 3.0
 	 */
 	public static final int POST_BUILD = 16;
 
 	/**
 	 * Returns all marker deltas of the specified type that are associated
 	 * with resource deltas for this event. If <code>includeSubtypes</code>
 	 * is <code>false</code>, only marker deltas whose type exactly matches 
 	 * the given type are returned.  Returns an empty array if there 
 	 * are no matching marker deltas.
 	 * <p>
 	 * Calling this method is equivalent to walking the entire resource
 	 * delta for this event, and collecting all marker deltas of a given type.
 	 * The speed of this method will be proportional to the number of changed
 	 * markers, regardless of the size of the resource delta tree.
 	 * </p>
 	 * @param type the type of marker to consider, or <code>null</code> to indicate all types
 	 * @param includeSubtypes whether or not to consider subtypes of the given type
 	 * @return an array of marker deltas
 	 * @since 2.0
 	 */
 	public IMarkerDelta[] findMarkerDeltas(String type, boolean includeSubtypes);
 
 	/**
 	 * Returns a resource delta, rooted at the workspace, describing the set
 	 * of changes that happened to resources in the workspace. 
 	 * Returns <code>null</code> if not applicable to this type of event.
 	 *
 	 * @return the resource delta, or <code>null</code> if not
 	 *   applicable
 	 */
 	public IResourceDelta getDelta();
 
 	/**
	 * Returns the resource in question or <code>null</code>
	 * if not applicable to this type of event. 
	 * <p>
	 * If the event is a <code>PRE_CLOSE</code> or 
	 * <code>PRE_DELETE</code> event then the resource 
	 * will be the affected project. Otherwise the resource will 
	 * be <code>null</code>.
	 * </p>
 	 * @return the resource, or <code>null</code> if not applicable
 	 */
 	public IResource getResource();
 
 	/**
 	 * Returns an object identifying the source of this event.
 	 *
 	 * @return an object identifying the source of this event 
 	 * @see java.util.EventObject
 	 */
 	public Object getSource();
 
 	/**
 	 * Returns the type of event being reported.
 	 *
 	 * @return one of the event type constants
 	 * @see #POST_CHANGE
 	 * @see #POST_BUILD
 	 * @see #PRE_BUILD
 	 * @see #PRE_CLOSE
 	 * @see #PRE_DELETE
 	 */
 	public int getType();
 }
