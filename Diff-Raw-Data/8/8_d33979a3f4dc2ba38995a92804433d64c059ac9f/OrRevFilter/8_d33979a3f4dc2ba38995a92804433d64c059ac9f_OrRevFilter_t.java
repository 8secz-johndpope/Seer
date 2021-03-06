 /*
  *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
  *
  *  This library is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU General Public
  *  License, version 2, as published by the Free Software Foundation.
  *
  *  This library is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *  General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public
  *  License along with this library; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
  */
 package org.spearce.jgit.revwalk.filter;
 
 import java.io.IOException;
 import java.util.Collection;
 
 import org.spearce.jgit.errors.IncorrectObjectTypeException;
 import org.spearce.jgit.errors.MissingObjectException;
 import org.spearce.jgit.revwalk.RevCommit;
 import org.spearce.jgit.revwalk.RevWalk;
 
 /**
  * Includes a commit if any subfilters include the same commit.
  * <p>
  * Classic shortcut behavior is used, so evaluation of the
  * {@link RevFilter#include(RevWalk, RevCommit)} method stops as soon as a true
  * result is obtained. Applications can improve filtering performance by placing
  * faster filters that are more likely to accept a result earlier in the list.
  */
 public abstract class OrRevFilter extends RevFilter {
 	/**
 	 * Create a filter with two filters, one of which must match.
	 * 
 	 * @param a
 	 *            first filter to test.
 	 * @param b
 	 *            second filter to test.
 	 * @return a filter that must match at least one input filter.
 	 */
 	public static RevFilter create(final RevFilter a, final RevFilter b) {
 		if (a == ALL || b == ALL)
 			return ALL;
 		return new Binary(a, b);
 	}
 
 	/**
 	 * Create a filter around many filters, one of which must match.
	 * 
 	 * @param list
 	 *            list of filters to match against. Must contain at least 2
 	 *            filters.
 	 * @return a filter that must match at least one input filter.
 	 */
 	public static RevFilter create(final RevFilter[] list) {
 		if (list.length == 2)
 			return create(list[0], list[1]);
 		if (list.length < 2)
 			throw new IllegalArgumentException("At least two filters needed.");
 		final RevFilter[] subfilters = new RevFilter[list.length];
 		System.arraycopy(list, 0, subfilters, 0, list.length);
 		return new List(subfilters);
 	}
 
 	/**
 	 * Create a filter around many filters, one of which must match.
	 * 
 	 * @param list
 	 *            list of filters to match against. Must contain at least 2
 	 *            filters.
 	 * @return a filter that must match at least one input filter.
 	 */
 	public static RevFilter create(final Collection<RevFilter> list) {
 		if (list.size() < 2)
 			throw new IllegalArgumentException("At least two filters needed.");
 		final RevFilter[] subfilters = new RevFilter[list.size()];
 		list.toArray(subfilters);
 		if (subfilters.length == 2)
 			return create(subfilters[0], subfilters[1]);
 		return new List(subfilters);
 	}
 
 	private static class Binary extends OrRevFilter {
 		private final RevFilter a;
 
 		private final RevFilter b;
 
 		Binary(final RevFilter one, final RevFilter two) {
 			a = one;
 			b = two;
 		}
 
 		@Override
 		public boolean include(final RevWalk walker, final RevCommit c)
 				throws MissingObjectException, IncorrectObjectTypeException,
 				IOException {
 			return a.include(walker, c) || b.include(walker, c);
 		}
 
 		@Override
 		public RevFilter clone() {
 			return new Binary(a.clone(), b.clone());
 		}
 
 		@Override
 		public String toString() {
 			return "(" + a.toString() + " OR " + b.toString() + ")";
 		}
 	}
 
 	private static class List extends OrRevFilter {
 		private final RevFilter[] subfilters;
 
 		List(final RevFilter[] list) {
 			subfilters = list;
 		}
 
 		@Override
 		public boolean include(final RevWalk walker, final RevCommit c)
 				throws MissingObjectException, IncorrectObjectTypeException,
 				IOException {
 			for (final RevFilter f : subfilters) {
 				if (f.include(walker, c))
 					return true;
 			}
 			return false;
 		}
 
 		@Override
 		public RevFilter clone() {
 			final RevFilter[] s = new RevFilter[subfilters.length];
 			for (int i = 0; i < s.length; i++)
 				s[i] = subfilters[i].clone();
 			return new List(s);
 		}
 
 		@Override
 		public String toString() {
 			final StringBuffer r = new StringBuffer();
 			r.append("(");
 			for (int i = 0; i < subfilters.length; i++) {
 				if (i > 0)
 					r.append(" OR ");
 				r.append(subfilters[i].toString());
 			}
 			r.append(")");
 			return r.toString();
 		}
 	}
 }
