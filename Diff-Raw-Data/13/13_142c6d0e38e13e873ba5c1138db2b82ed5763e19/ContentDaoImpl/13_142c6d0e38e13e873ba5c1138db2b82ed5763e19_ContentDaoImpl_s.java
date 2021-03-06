 /*
  * Copyright 2006 Open Source Applications Foundation
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.osaf.cosmo.dao.hibernate;
 
 import java.util.Date;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.hibernate.FlushMode;
 import org.hibernate.Hibernate;
 import org.hibernate.HibernateException;
 import org.hibernate.Query;
 import org.hibernate.validator.InvalidStateException;
 import org.osaf.cosmo.dao.ContentDao;
 import org.osaf.cosmo.model.CollectionItem;
 import org.osaf.cosmo.model.ContentItem;
 import org.osaf.cosmo.model.ICalendarItem;
 import org.osaf.cosmo.model.IcalUidInUseException;
 import org.osaf.cosmo.model.Item;
 import org.osaf.cosmo.model.NoteItem;
 import org.osaf.cosmo.model.hibernate.HibItem;
 import org.osaf.cosmo.model.hibernate.HibItemTombstone;
 
 /**
  * Implementation of ContentDao using hibernate persistence objects
  * 
  */
 public class ContentDaoImpl extends ItemDaoImpl implements ContentDao {
 
     private static final Log log = LogFactory.getLog(ContentDaoImpl.class);
    
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#createCollection(org.osaf.cosmo.model.CollectionItem,
      *      org.osaf.cosmo.model.CollectionItem)
      */
     public CollectionItem createCollection(CollectionItem parent,
             CollectionItem collection) {
 
         if(parent==null)
             throw new IllegalArgumentException("parent cannot be null");
         
         if (collection == null)
             throw new IllegalArgumentException("collection cannot be null");
 
         if (collection.getOwner() == null)
             throw new IllegalArgumentException("collection must have owner");
         
         if (getBaseModelObject(collection).getId()!=-1)
             throw new IllegalArgumentException("invalid collection id (expected -1)");
         
         
         try {
             // verify uid not in use
             checkForDuplicateUid(collection);
             
             setBaseItemProps(collection);
             collection.getParents().add(parent);
             
             getSession().save(collection);
             getSession().flush();
             
             return collection;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
     
     
     /* (non-Javadoc)
      * @see org.osaf.cosmo.dao.ContentDao#updateCollection(org.osaf.cosmo.model.CollectionItem, java.util.Set)
      */
     public CollectionItem updateCollection(CollectionItem collection, Set<ContentItem> children) {
         
         try {
             updateCollectionInternal(collection);
             
             // Either create, update, or delete each item
             for (ContentItem item : children) {
                 
                 // create item
                 if(getBaseModelObject(item).getId()==-1) {
                     createContentInternal(collection, item);
                 }
                 // delete item
                 else if(item.getIsActive()==false) {
                     // If item is a note modification, only remove the item
                     // if its parent is not also being removed.  This is because
                     // when a master item is removed, all its modifications are
                     // removed.
                     if(item instanceof NoteItem) {
                         NoteItem note = (NoteItem) item;
                         if(note.getModifies()!=null && note.getModifies().getIsActive()==false)
                             continue;
                     }
                     removeItemFromCollectionInternal(item, collection);
                 }
                 // update item
                 else {
                     if(!item.getParents().contains(collection)) {
                         addItemToCollectionInternal(item, collection);
                     }
                     updateContentInternal(item);
                 }
             }
             
             getSession().flush();
             
             // clear the session to improve subsequent flushes
             getSession().clear();
             
             // load collection to get it back into the session
             getSession().load(collection, getBaseModelObject(collection).getId());
             
             return collection;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
 
 
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#createContent(org.osaf.cosmo.model.CollectionItem,
      *      org.osaf.cosmo.model.ContentItem)
      */
     public ContentItem createContent(CollectionItem parent, ContentItem content) {
         
         try {
             createContentInternal(parent, content);
             getSession().flush();
             return content;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
     
     
     /* (non-Javadoc)
      * @see org.osaf.cosmo.dao.ContentDao#createContent(java.util.Set, org.osaf.cosmo.model.ContentItem)
      */
     public ContentItem createContent(Set<CollectionItem> parents, ContentItem content) {
 
         try {
             createContentInternal(parents, content);
             getSession().flush();
             return content;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
     
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#findCollectionByUid(java.lang.String)
      */
     public CollectionItem findCollectionByUid(String uid) {
         try {
             Query hibQuery = getSession()
                     .getNamedQuery("collectionItem.by.uid").setParameter("uid",uid);
             hibQuery.setFlushMode(FlushMode.MANUAL);
             return (CollectionItem) hibQuery.uniqueResult();
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#findCollectionByPath(java.lang.String)
      */
     public CollectionItem findCollectionByPath(String path) {
         try {
             Item item = getItemPathTranslator().findItemByPath(path);
             if (item == null || !(item instanceof CollectionItem) )
                 return null;
 
             return (CollectionItem) item;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#findContentByPath(java.lang.String)
      */
     public ContentItem findContentByPath(String path) {
         try {
             Item item = getItemPathTranslator().findItemByPath(path);
             if (item == null || !(item instanceof ContentItem) )
                 return null;
 
             return (ContentItem) item;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } 
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#findContentByUid(java.lang.String)
      */
     public ContentItem findContentByUid(String uid) {
         try {
             Query hibQuery = getSession().getNamedQuery("contentItem.by.uid")
                     .setParameter("uid", uid);
             hibQuery.setFlushMode(FlushMode.MANUAL);
             return (ContentItem) hibQuery.uniqueResult();
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
 
     public CollectionItem updateCollectionTimestamp(CollectionItem collection) {
         try {
             if(!getSession().contains(collection))
                 collection = (CollectionItem) getSession().merge(collection);
             collection.updateTimestamp();
             getSession().flush();
             return collection;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
     
     
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#updateCollection(org.osaf.cosmo.model.CollectionItem)
      */
     public CollectionItem updateCollection(CollectionItem collection) {
         try {
             
             updateCollectionInternal(collection);
             getSession().flush();
             
             return collection;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#updateContent(org.osaf.cosmo.model.ContentItem)
      */
     public ContentItem updateContent(ContentItem content) {
         try {     
             updateContentInternal(content);
             getSession().flush();
             return content;
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         } catch (InvalidStateException ise) {
             logInvalidStateException(ise);
             throw ise;
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#removeCollection(org.osaf.cosmo.model.CollectionItem)
      */
     public void removeCollection(CollectionItem collection) {
         
         if(collection==null)
             throw new IllegalArgumentException("collection cannot be null");
         
         try {
             getSession().refresh(collection);
             removeCollectionRecursive(collection);
             getSession().flush();
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.osaf.cosmo.dao.ContentDao#removeContent(org.osaf.cosmo.model.ContentItem)
      */
     public void removeContent(ContentItem content) {
         
         if(content==null)
             throw new IllegalArgumentException("content cannot be null");
         
         try {
             getSession().refresh(content);
             removeContentRecursive(content);
             getSession().flush();
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
     
     
     /* (non-Javadoc)
      * @see org.osaf.cosmo.dao.ContentDao#loadChildren(org.osaf.cosmo.model.CollectionItem, java.util.Date)
      */
     public Set<ContentItem> loadChildren(CollectionItem collection, Date timestamp) {
         try {
             Set<ContentItem> children = new HashSet<ContentItem>();
             Query query = null;
 
             // use custom HQL query that will eager fetch all associations
             if (timestamp == null)
                 query = getSession().getNamedQuery("contentItem.by.parent")
                         .setParameter("parent", collection);
             else
                 query = getSession().getNamedQuery("contentItem.by.parent.timestamp")
                         .setParameter("parent", collection).setParameter(
                                 "timestamp", timestamp);
             
             query.setFlushMode(FlushMode.MANUAL);
             List results = query.list();
             for (Iterator it = results.iterator(); it.hasNext();) {
                 ContentItem content = (ContentItem) it.next();
                 initializeItem(content);
                 children.add(content);
             }
 
             return children;
             
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
     }
 
 
     @Override
     public void initializeItem(Item item) {
         super.initializeItem(item);
         
         // Initialize master NoteItem if applicable
         try {
            if(item instanceof NoteItem) {
                NoteItem note = (NoteItem) item;
                if(note.getModifies()!=null) {
                    Hibernate.initialize(note.getModifies());
                    initializeItem(note.getModifies());
                }
            }
         } catch (HibernateException e) {
             throw convertHibernateAccessException(e);
         }
         
     }
 
     @Override
     public void removeItem(Item item) {
         if(item instanceof ContentItem)
             removeContent((ContentItem) item);
         else if(item instanceof CollectionItem)
             removeCollection((CollectionItem) item);
         else
             super.removeItem(item);
     }
     
     
     @Override
     public void removeItemByPath(String path) {
         Item item = this.findItemByPath(path);
         if(item instanceof ContentItem)
             removeContent((ContentItem) item);
         else if(item instanceof CollectionItem)
             removeCollection((CollectionItem) item);
         else
             super.removeItem(item);
     }
 
     @Override
     public void removeItemByUid(String uid) {
         Item item = this.findItemByUid(uid);
         if(item instanceof ContentItem)
             removeContent((ContentItem) item);
         else if(item instanceof CollectionItem)
             removeCollection((CollectionItem) item);
         else
             super.removeItem(item);
     }
     
 
     /**
      * Initializes the DAO, sanity checking required properties and defaulting
      * optional properties.
      */
     public void init() {
         super.init();
     }
 
     private void removeContentRecursive(ContentItem content) {
         removeContentCommon(content);
         
         // Remove modifications
         if(content instanceof NoteItem) {
             NoteItem note = (NoteItem) content;
             if(note.getModifies()!=null) {
                 // remove mod from master's collection
                 note.getModifies().removeModification(note);
                 note.getModifies().updateTimestamp();
             } else {  
                 // mods will be removed by Hibernate cascading rules, but we
                 // need to add tombstones for mods
                 for(NoteItem mod: note.getModifications())
                     removeContentCommon(mod);
             }
         }
             
         getSession().delete(content);
     }
     
     private void removeContentCommon(ContentItem content) {
         // Add a tombstone to each parent collection to track
         // when the removal occurred.
         for (CollectionItem parent : content.getParents()) {
             parent.addTombstone(new HibItemTombstone(parent,content));
             getSession().update(parent);
         }
     }
     
     private void removeCollectionRecursive(CollectionItem collection) {
         // Removing a collection does not automatically remove
         // its children.  Instead, the association to all the
         // children is removed, and any children who have no
         // parent collection are then removed.
         for(Item item: collection.getChildren()) {
             if(item instanceof CollectionItem) {
                 removeCollectionRecursive((CollectionItem) item);
             } else if(item instanceof ContentItem) {                    
                 item.getParents().remove(collection);
                 if(item.getParents().size()==0)
                     getSession().delete(item);
             } else {
                 getSession().delete(item);
             }
         }
         
         getSession().delete(collection);
     }
     
     
     private void removeNoteItemFromCollectionInternal(NoteItem note, CollectionItem collection) {
         getSession().update(collection);
         getSession().update(note);
         
         // do nothing if item doesn't belong to collection
         if(!note.getParents().contains(collection))
             return;
         
         collection.addTombstone(new HibItemTombstone(collection, note));
         note.getParents().remove(collection);
         
         for(NoteItem mod: note.getModifications())
             removeNoteItemFromCollectionInternal(mod, collection);
         
         // If the item belongs to no collection, then it should
         // be purged.
         if(note.getParents().size()==0)
             removeItemInternal(note);
         
     }
     
     
     protected void createContentInternal(CollectionItem parent, ContentItem content) {
 
         if(parent==null)
             throw new IllegalArgumentException("parent cannot be null");
         
         if (content == null)
             throw new IllegalArgumentException("content cannot be null");
 
         if (getBaseModelObject(content) .getId()!=-1)
             throw new IllegalArgumentException("invalid content id (expected -1)");
         
         if (content.getOwner() == null)
             throw new IllegalArgumentException("content must have owner");
         
         // verify uid not in use
         checkForDuplicateUid(content);
         
         // verify icaluid not in use for collection
         if (content instanceof ICalendarItem)
             checkForDuplicateICalUid((ICalendarItem) content, parent);
         
         setBaseItemProps(content);
         
        
         // When a note modification is added, it must be added to all
         // collections that the parent note is in, because a note modification's
         // parents are tied to the parent note.
         if(isNoteModification(content)) {
             NoteItem note = (NoteItem) content;
            
             // ensure master is dirty so that etag gets updated
             note.getModifies().updateTimestamp();
             note.getModifies().addModification(note);
             
             if(!note.getModifies().getParents().contains(parent))
                 throw new IllegalArgumentException("note modification cannot be added to collection that parent note is not in");
             
             // Add modification to all parents of master
             for (CollectionItem col : note.getModifies().getParents()) {
                 if (col.removeTombstone(content) == true)
                     getSession().update(col);
                 note.getParents().add(col);
             }
         } else {
             // add parent to new content
             content.getParents().add(parent);
             
             // remove tombstone (if it exists) from parent
             if(parent.removeTombstone(content)==true)
                 getSession().update(parent);
         }
         
        
         getSession().save(content);    
     }
 
     protected void createContentInternal(Set<CollectionItem> parents, ContentItem content) {
 
         if(parents==null)
             throw new IllegalArgumentException("parent cannot be null");
         
         if (content == null)
             throw new IllegalArgumentException("content cannot be null");
 
         if (getBaseModelObject(content).getId()!=-1)
             throw new IllegalArgumentException("invalid content id (expected -1)");
         
         if (content.getOwner() == null)
             throw new IllegalArgumentException("content must have owner");
         
         
         if(parents.size()==0)
             throw new IllegalArgumentException("content must have at least one parent");
         
         // verify uid not in use
         checkForDuplicateUid(content);
         
         // verify icaluid not in use for collections
         if (content instanceof ICalendarItem)
             checkForDuplicateICalUid((ICalendarItem) content, content.getParents());
         
         setBaseItemProps(content);
         
         // Ensure NoteItem modifications have the same parents as the 
         // master note.
         if (isNoteModification(content)) {
             NoteItem note = (NoteItem) content;
             
             // ensure master is dirty so that etag gets updated
             note.getModifies().updateTimestamp();
             note.getModifies().addModification(note);
             
             if (!note.getModifies().getParents().equals(parents))
                 throw new IllegalArgumentException(
                         "Note modification parents must equal the parents of master note");
         }
         
         for(CollectionItem parent: parents) {
             content.getParents().add(parent);
             if(parent.removeTombstone(content)==true)
                 getSession().update(parent);
         }
         
       
         getSession().save(content);
     }
 
     protected void updateContentInternal(ContentItem content) {
         
         if (content == null)
             throw new IllegalArgumentException("content cannot be null");
          
         getSession().update(content);
         
         if (content.getOwner() == null)
             throw new IllegalArgumentException("content must have owner");
         
         content.updateTimestamp();
         
         if(isNoteModification(content)) {
             // ensure master is dirty so that etag gets updated
             ((NoteItem) content).getModifies().updateTimestamp();
         }
         
     }
     
     protected void updateCollectionInternal(CollectionItem collection) {
         if (collection == null)
             throw new IllegalArgumentException("collection cannot be null");
         
         getSession().update(collection);
         
         if (collection.getOwner() == null)
             throw new IllegalArgumentException("collection must have owner");
         
         collection.updateTimestamp();
     }
     
     /**
      * Override so we can handle NoteItems. Adding a note to a collection
      * requires verifying that the icaluid is unique within the collection.
      */
     @Override
     protected void addItemToCollectionInternal(Item item,
             CollectionItem collection) {
 
         // Don't allow note modifications to be added to a collection
         // When a master is added, all the modifications are added
         if (isNoteModification(item)) {
             throw new IllegalArgumentException("cannot add modification, only master");
         }
         
         if (item instanceof ICalendarItem)
             // verify icaluid is unique within collection
             checkForDuplicateICalUid((ICalendarItem) item, collection);
 
         
         super.addItemToCollectionInternal(item, collection);
         
         // Add all modifications
         if(item instanceof NoteItem) {
             for(NoteItem mod: ((NoteItem) item).getModifications())
                 super.addItemToCollectionInternal(mod, collection);
         }
     }
     
     @Override
     protected void removeItemFromCollectionInternal(Item item, CollectionItem collection) {
         if(item instanceof NoteItem) {
             // When a note modification is removed, it is really removed from
             // all collections because a modification can't live in one collection
             // and not another.  It is tied to the collections that the master
             // note is in.  Therefore you can't just remove a modification from
             // a single collection when the master note is in multiple collections.
             NoteItem note = (NoteItem) item;
             if(note.getModifies()!=null)
                 removeContentRecursive((ContentItem) item);
             else
                 removeNoteItemFromCollectionInternal((NoteItem) item, collection);
         }
         else
             super.removeItemFromCollectionInternal(item, collection);
     }
 
     protected void checkForDuplicateICalUid(ICalendarItem item, CollectionItem parent) {
 
         // TODO: should icalUid be required?  Currrently its not and all
         // items created by the webui dont' have it.
         if (item.getIcalUid() == null) 
             return;
         
         // ignore modifications
         if(item instanceof NoteItem && ((NoteItem) item).getModifies()!=null)
             return;
 
         // Lookup item by parent/icaluid
         Query hibQuery = null;
         if (item instanceof NoteItem)
             hibQuery = getSession().getNamedQuery(
                     "noteItemId.by.parent.icaluid").setParameter("parentid",
                             getBaseModelObject(parent).getId()).setParameter("icaluid", item.getIcalUid());
         else
             hibQuery = getSession().getNamedQuery(
                     "icalendarItem.by.parent.icaluid").setParameter("parentid",
                             getBaseModelObject(parent).getId()).setParameter("icaluid", item.getIcalUid());
         
         hibQuery.setFlushMode(FlushMode.MANUAL);
 
         Long itemId = (Long) hibQuery.uniqueResult();
 
         // if icaluid is in use throw exception
         if (itemId != null) {
             // If the note is new, then its a duplicate icaluid
             if (getBaseModelObject(item).getId() == -1) {
                 Item dup = (Item) getSession().load(HibItem.class, itemId);
                throw new IcalUidInUseException("icalUid" + item.getIcalUid()
                         + " already in use for collection " + parent.getUid(),
                         item.getUid(), dup.getUid());
             }
             // If the note exists and there is another note with the same
             // icaluid, then its a duplicate icaluid
             if (getBaseModelObject(item).getId().equals(itemId)) {
                 Item dup = (Item) getSession().load(HibItem.class, itemId);
                throw new IcalUidInUseException("icalUid" + item.getIcalUid()
                         + " already in use for collection " + parent.getUid(),
                         item.getUid(), dup.getUid());
             }
         }
     }
 
     protected void checkForDuplicateICalUid(ICalendarItem item,
             Set<CollectionItem> parents) {
 
         if (item.getIcalUid() == null) 
             return;
         
         // ignore modifications
         if(item instanceof NoteItem && ((NoteItem) item).getModifies()!=null)
             return;
 
         for (CollectionItem parent : parents)
             checkForDuplicateICalUid(item, parent);
     }
     
     private boolean isNoteModification(Item item) {
         if(!(item instanceof NoteItem))
             return false;
         
         return (((NoteItem) item).getModifies()!=null);
     }
 }
