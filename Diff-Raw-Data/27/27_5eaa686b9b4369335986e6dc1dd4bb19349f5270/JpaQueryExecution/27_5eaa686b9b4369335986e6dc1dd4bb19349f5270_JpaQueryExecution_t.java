 /*
  * Copyright 2008-2011 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.springframework.data.jpa.repository.query;
 
 import java.util.Collections;
 import java.util.List;
 
 import javax.persistence.EntityManager;
 import javax.persistence.NoResultException;
 import javax.persistence.Query;
 import javax.persistence.TypedQuery;
 
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.PageImpl;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.repository.query.ParameterAccessor;
 import org.springframework.data.repository.query.Parameters;
 import org.springframework.data.repository.query.ParametersParameterAccessor;
 import org.springframework.data.repository.query.QueryMethod;
 import org.springframework.util.Assert;
 
 /**
  * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a {@link QueryMethod}
  * a {@link AbstractStringBasedJpaQuery} can be executed in various flavours.
  * 
  * @author Oliver Gierke
  */
 public abstract class JpaQueryExecution {
 
 	/**
 	 * Executes the given {@link AbstractStringBasedJpaQuery} with the given {@link ParameterBinder}.
 	 * 
 	 * @param query
 	 * @param binder
 	 * @return
 	 */
 	public Object execute(AbstractJpaQuery query, Object[] values) {
 
 		Assert.notNull(query);
 		Assert.notNull(values);
 
 		try {
 			return doExecute(query, values);
 		} catch (NoResultException e) {
 			return null;
 		}
 	}
 
 	/**
 	 * Method to implement {@link AbstractStringBasedJpaQuery} executions by single enum values.
 	 * 
 	 * @param query
 	 * @param binder
 	 * @return
 	 */
 	protected abstract Object doExecute(AbstractJpaQuery query, Object[] values);
 
 	/**
 	 * Executes the {@link AbstractStringBasedJpaQuery} to return a simple collection of entities.
 	 */
 	static class CollectionExecution extends JpaQueryExecution {
 
 		@Override
 		protected Object doExecute(AbstractJpaQuery query, Object[] values) {
 			return query.createQuery(values).getResultList();
 		}
 	}
 
 	/**
 	 * Executes the {@link AbstractStringBasedJpaQuery} to return a {@link Page} of entities.
 	 */
 	static class PagedExecution extends JpaQueryExecution {
 
 		private final Parameters parameters;
 
 		public PagedExecution(Parameters parameters) {
 
 			this.parameters = parameters;
 		}
 
 		@Override
 		@SuppressWarnings("unchecked")
 		protected Object doExecute(AbstractJpaQuery repositoryQuery, Object[] values) {
 
 			// Execute query to compute total
 			TypedQuery<Long> projection = repositoryQuery.createCountQuery(values);
 			List<Long> totals = projection.getResultList();
 			Long total = totals.size() == 1 ? totals.get(0) : totals.size();
 
 			Query query = repositoryQuery.createQuery(values);
 			ParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
 			Pageable pageable = accessor.getPageable();
 
			List<Object> content = pageable == null || total > pageable.getOffset() ? query.getResultList() : Collections
					.emptyList();
 
 			return new PageImpl<Object>(content, pageable, total);
 		}
 	}
 
 	/**
 	 * Executes a {@link AbstractStringBasedJpaQuery} to return a single entity.
 	 */
 	static class SingleEntityExecution extends JpaQueryExecution {
 
 		@Override
 		protected Object doExecute(AbstractJpaQuery query, Object[] values) {
 
 			return query.createQuery(values).getSingleResult();
 		}
 	}
 
 	/**
 	 * Executes a modifying query such as an update, insert or delete.
 	 */
 	static class ModifyingExecution extends JpaQueryExecution {
 
 		private final EntityManager em;
 
 		/**
 		 * Creates an execution that automatically clears the given {@link EntityManager} after execution if the given
 		 * {@link EntityManager} is not {@literal null}.
 		 * 
 		 * @param em
 		 */
 		public ModifyingExecution(JpaQueryMethod method, EntityManager em) {
 
 			Class<?> returnType = method.getReturnType();
 
 			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
 			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);
 
 			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");
 
 			this.em = em;
 		}
 
 		@Override
 		protected Object doExecute(AbstractJpaQuery query, Object[] values) {
 
 			int result = query.createQuery(values).executeUpdate();
 
 			if (em != null) {
 				em.clear();
 			}
 
 			return result;
 		}
 	}
 }
