 /*******************************************************************************
  * Copyright (c) 2008 Code 9 and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Code 9 - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.internal.p2.metadata.mirror;
 
 import org.eclipse.osgi.service.resolver.VersionRange;
 
 public class VersionRangedName {
 	private final String id;
 	private final VersionRange range;
 
 	/**
 	 * Creates and returns a new version ranged id from the given spec.  The spec should be
 	 * id/version range.
 	 * @param spec the spec for the version ranged id to create
 	 * @return the parsed versioned id
 	 */
 	public static VersionRangedName parse(String spec) {
 		String[] segments = MirrorApplication.getArrayArgsFromString(spec, "/"); //$NON-NLS-1$
 		return new VersionRangedName(segments[0], segments.length == 1 ? null : segments[1]);
 	}
 
 	public VersionRangedName(String id, String rangeSpec) {
 		this(id, new VersionRange(rangeSpec));
 	}
 
 	public VersionRangedName(String id, VersionRange range) {
 		if (id == null)
			throw new IllegalArgumentException("id cannot be null"); //$NON-NLS-1$
 		this.id = id;
 		this.range = range != null ? range : new VersionRange(null);
 	}
 
 	public String getId() {
 		return id;
 	}
 
 	public VersionRange getVersionRange() {
 		return range;
 	}
 
 	public String toString() {
 		return id + "/" + (range == null ? "0.0.0" : range.toString()); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 }
