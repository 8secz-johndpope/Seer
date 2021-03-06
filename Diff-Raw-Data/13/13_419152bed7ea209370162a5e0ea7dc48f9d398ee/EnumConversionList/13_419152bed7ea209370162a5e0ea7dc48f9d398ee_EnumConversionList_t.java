 /*******************************************************************************
  * Copyright (c) 2011 Vrije Universiteit Brussel.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Dennis Wagelaar, Vrije Universiteit Brussel - initial API and
  *         implementation and/or initial documentation
  *******************************************************************************/
 package org.eclipse.m2m.atl.emftvm.util;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.NoSuchElementException;
 
 import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EEnumLiteral;
 
 /**
  * Converts {@link Enumerator}s to {@link EnumLiteral}s.
  * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
  */
 public class EnumConversionList extends LazyList<Object> {
 
 	/**
 	 * {@link Iterator} for {@link EnumConversionList}.
 	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
 	 */
 	public class EnumConversionIterator extends CachingIterator {
 
 		/**
 		 * Creates a new {@link EnumConversionIterator}.
 		 */
 		public EnumConversionIterator() {
 			super(dataSource.iterator());
 		}
 
 		/**
 		 * {@inheritDoc}
 		 */
 		@Override
 		public Object next() {
 			final Object next = convert(inner.next());
 			updateCache(next);
 			return next;
 		}
 	}
 
 	/**
 	 * Creates a new {@link EnumConversionList} around <code>dataSource</code>.
 	 * @param dataSource the list to wrap
 	 */
 	public EnumConversionList(List<Object> dataSource) {
 		super(dataSource);
 	}
 
 	/**
 	 * Performs the element conversion.
 	 * @param object the object to convert
 	 * @return the converted object
 	 */
 	protected final Object convert(final Object object) {
		if (object instanceof Enumerator && !(object instanceof EEnumLiteral)) {
 			return new EnumLiteral(object.toString());
 		}
 		return object;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public Iterator<Object> iterator() {
 		if (dataSource == null) {
 			return Collections.unmodifiableCollection(cache).iterator();
 		}
 		return new EnumConversionIterator(); // extends CachingIterator
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public int size() {
 		if (dataSource == null) {
 			return cache.size();
 		}
 		return ((Collection<Object>)dataSource).size();
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public Object get(final int index) {
 		if (index < cache.size()) {
 			return ((List<Object>)cache).get(index);
 		}
 		return convert(((List<Object>)dataSource).get(index));
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	public Object last() {
 		final int size = size();
 		if (size < 1) {
 			throw new NoSuchElementException();
 		}
 		if (dataSource == null) {
 			return ((List<Object>)cache).get(size - 1);
 		}
 		return convert(((List<Object>)dataSource).get(size - 1));
 	}
 
 	/**
 	 * Forces cache completion.
 	 * @return this list
 	 */
 	public EnumConversionList cache() {
 		synchronized (cache) {
 			if (dataSource != null) {
 				cache.clear();
 				for (Object o : dataSource) {
 					cache.add(convert(o));
 				}
 				assert cache.size() == ((List<?>)dataSource).size();
 				dataSource = null;
 			}
 		}
 		return this;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 */
 	@Override
 	protected void createCache() {
 		if (dataSource == null) {
 			this.cache = Collections.emptyList(); // dataSource == null; cache complete
 			this.occurrences = Collections.emptyMap();
 		} else {
 			this.cache = new ArrayList<Object>(((List<Object>)dataSource).size());
 		}
 		assert this.cache instanceof List<?>;
 	}
 }
