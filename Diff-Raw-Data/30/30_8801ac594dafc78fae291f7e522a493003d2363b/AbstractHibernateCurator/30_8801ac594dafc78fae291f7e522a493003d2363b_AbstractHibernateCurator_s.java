 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.model;
 
 import java.io.Serializable;
 import java.util.List;
 
 import javax.persistence.EntityManager;
 
 import org.hibernate.Session;
 
 import com.google.inject.Inject;
 import com.google.inject.Provider;
 import com.wideplay.warp.persist.Transactional;
 
 public abstract class AbstractHibernateCurator<E extends Persisted> {
     @Inject protected Provider<EntityManager> entityManager;
     private final Class<E> entityType;
 
     protected AbstractHibernateCurator(Class<E> entityType) {
         //entityType = (Class<E>) ((ParameterizedType)
         //getClass().getGenericSuperclass()).getActualTypeArguments()[0];
         this.entityType = entityType;
     }
 
     public E find(Serializable id) {
         return id == null ? null : get(entityType, id);
     }
 
     @Transactional
     public E create(E entity) {
         save(entity);
         flush();
         return entity;
     }
     
     @SuppressWarnings("unchecked")
     @Transactional()
     public List<E> findAll() {
         return (List<E>) currentSession().createCriteria(entityType).list();
     }
     
     @Transactional
     public void delete(E entity) {
         E toDelete = find(entity.getId());
         currentSession().delete(toDelete);
     }
     
     @Transactional
     public E merge(E entity) {
         return getEntityManager().merge(entity);
     }
 
     protected final <T> T get(Class<T> clazz, Serializable id) {
         return clazz.cast(currentSession().get(clazz, id));
     }
 
     @Transactional
     protected final void save(E anObject) {
         getEntityManager().persist(anObject);
     }
 
     protected final void flush() {
         getEntityManager().flush();
     }
 
     protected Session currentSession() {
         return (Session) entityManager.get().getDelegate();
     }
 
     protected EntityManager getEntityManager() {
         return entityManager.get();
     }
 }
