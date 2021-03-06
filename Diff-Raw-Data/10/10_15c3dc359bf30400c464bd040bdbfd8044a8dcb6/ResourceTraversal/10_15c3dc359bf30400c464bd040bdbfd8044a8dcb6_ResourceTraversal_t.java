 /*******************************************************************************
  * Copyright (c) 2004, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.resources.mapping;
 
 import java.util.ArrayList;
 import org.eclipse.core.internal.resources.*;
 import org.eclipse.core.resources.*;
 import org.eclipse.core.runtime.CoreException;
 
 /**
  * A resource traversal is simply a set of resources and the depth to which
  * each is to be traversed. A set of traversals is used to describe the
  * resources that constitute a model element.
  * <p>
  * The flags of the traversal indicate which special resources should be
  * included or excluded from the traversal. The flags used are the same as
  * those passed to the <code>IResource#accept(IResourceVisitor, int, int)</code> method.
  * 
  * <p>
  * This class may be instantiated or subclassed by clients.
  * </p>
 
  * @see org.eclipse.core.resources.IResource
  * @since 3.2
  */
 public class ResourceTraversal {
 
 	private final int depth;
 	private final int flags;
 	private final IResource[] resources;
 
 	/**
 	 * Creates a new resource traversal.
 	 * @param resources The resources in the traversal
 	 * @param depth The traversal depth
 	 * @param flags the flags for this traversal. The traversal flags match those
 	 * that are passed to the <code>IResource#accept</code> method.
 	 */
 	public ResourceTraversal(IResource[] resources, int depth, int flags) {
 		if (resources == null)
 			throw new NullPointerException();
 		this.resources = resources;
 		this.depth = depth;
 		this.flags = flags;
 	}
 
 	/**
 	 * Visit the resources of this traversal.
 	 * 
 	 * @param visitor a resource visitor
 	 * @exception CoreException if this method fails. Reasons include:
 	 * <ul>
 	 * <li> A resource in this traversal does not exist.</li>
 	 * <li> The visitor failed with this exception.</li>
 	 * </ul>
 	 */
 	public void accept(IResourceVisitor visitor) throws CoreException {
 		for (int i = 0, imax = resources.length; i < imax; i++)
 			resources[i].accept(visitor, depth, flags);
 	}
 
 	/**
 	 * Return whether the given resource is contained in or
 	 * covered by this traversal.
 	 * @param resource the resource to be tested
 	 * @return whether the resource is contained in this traversal
 	 */
 	public boolean contains(IResource resource) {
 		for (int i = 0; i < resources.length; i++) {
 			IResource member = resources[i];
 			if (contains(member, resource)) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	private boolean contains(IResource resource, IResource child) {
 		if (resource.equals(child))
 			return true;
		if (depth == IResource.DEPTH_ZERO)
			return false;
		if (child.getParent().equals(resource))
 			return true;
		if (depth == IResource.DEPTH_INFINITE)
 			return resource.getFullPath().isPrefixOf(child.getFullPath());
 		return false;
 	}
 
 	/**
 	 * Efficient implementation of {@link #findMarkers(String, boolean)}, not
 	 * available to clients because underlying non-API methods are used that
 	 * may change.
 	 */
 	void doFindMarkers(ArrayList result, String type, boolean includeSubtypes) throws CoreException {
 		MarkerManager markerMan = ((Workspace) ResourcesPlugin.getWorkspace()).getMarkerManager();
 		for (int i = 0; i < resources.length; i++) {
 			Resource resource = (Resource) resources[i];
 			resource.checkAccessible(resource.getFlags(resource.getResourceInfo(false, false)));
 			markerMan.doFindMarkers(resource, result, type, includeSubtypes, depth);
 		}
 	}
 
 	/**
 	 * Returns all markers of the specified type on the resources in this traversal.
 	 * If <code>includeSubtypes</code> is <code>false</code>, only markers 
 	 * whose type exactly matches the given type are returned.  Returns an empty 
 	 * array if there are no matching markers.
 	 *
 	 * @param type the type of marker to consider, or <code>null</code> to indicate all types
 	 * @param includeSubtypes whether or not to consider sub-types of the given type
 	 * @return an array of markers
 	 * @exception CoreException if this method fails. Reasons include:
 	 * <ul>
 	 * <li> A resource in this traversal does not exist.</li>
 	 * </ul>
 	 */
 	public IMarker[] findMarkers(String type, boolean includeSubtypes) throws CoreException {
 		if (resources.length == 0)
 			return new IMarker[0];
 		ArrayList result = new ArrayList();
 		doFindMarkers(result, type, includeSubtypes);
 		return (IMarker[]) result.toArray(new IMarker[result.size()]);
 	}
 
 	/**
 	 * Returns the depth to which the resources should be traversed.
 	 * 
 	 * @return the depth to which the physical resources are to be traversed
 	 * (one of IResource.DEPTH_ZERO, IResource.DEPTH_ONE or
 	 * IResource.DEPTH_INFINITE)
 	 */
 	public int getDepth() {
 		return depth;
 	}
 
 	/**
 	 * Return the flags for this traversal. 
 	 * The flags of the traversal indicate which special resources should be
 	 * included or excluded from the traversal. The flags used are the same as
 	 * those passed to the <code>IResource#accept(IResourceVisitor, int, int)</code> method.
 	 * Clients who traverse the resources manually (i.e. without calling <code>accept</code>)
 	 * should respect the flags when determining which resources are included
 	 * in the traversal.
 	 * 
 	 * @return the flags for this traversal
 	 */
 	public int getFlags() {
 		return flags;
 	}
 
 	/**
 	 * Returns the file system resource(s) for this traversal. The returned
 	 * resources must be contained within the same project and need not exist in
 	 * the local file system. The traversal of the returned resources should be
 	 * done considering the flag returned by getDepth. If a resource returned by
 	 * a traversal is a file, it should always be visited. If a resource of a
 	 * traversal is a folder then files contained in the folder can only be
 	 * visited if the folder is IResource.DEPTH_ONE or IResource.DEPTH_INFINITE.
 	 * Child folders should only be visited if the depth is
 	 * IResource.DEPTH_INFINITE.
 	 * 
 	 * @return The resources in this traversal
 	 */
 	public IResource[] getResources() {
 		return resources;
 	}
 }
