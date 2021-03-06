 /* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
  * This code is licensed under the LGPL 2.1 license, available at the root
  * application directory.
  */
 package org.geogit.api;
 
 import javax.annotation.Nullable;
 
 import com.google.common.base.Preconditions;
 
 /**
  * Pairing of a name and the {@link ObjectId} it currently has.
  * <p>
  * A ref in Git is (more or less) a variable that holds a single object identifier. The object
  * identifier can be any valid Git object (blob, tree, commit, annotated tag, ...).
  * <p>
  * The ref name has the attributes of the ref that was asked for as well as the ref it was resolved
  * to for symbolic refs plus the object id it points to and (for tags) the peeled target object id,
  * i.e. the tag resolved recursively until a non-tag object is referenced.
  */
 public class Ref implements Comparable<Ref> {
 
     /**
      * By convention, name of the main branch
      */
     public static final String MASTER = "refs/heads/master";
 
     /**
      * Pointer to the latest commit in the current branch
      */
     public static final String HEAD = "HEAD";
 
     /**
      * Pointer to the current tree in the staging index
      */
     public static final String STAGE_HEAD = "STAGE_HEAD";
 
     /**
      * Pointer to the current tree in the working directory
      */
     public static final String WORK_HEAD = "WORK_HEAD";
 
     /**
      * Directory prefix for refs.
      */
     public static final String REFS_PREFIX = "refs/";
 
     /**
      * Directory prefix for remotes.
      */
     public static final String REMOTES_PREFIX = REFS_PREFIX + "remotes/";
 
     /**
      * Directory prefix for tags.
      */
     public static final String TAGS_PREFIX = REFS_PREFIX + "tags/";
 
     /**
      * Directory prefix for heads.
      */
     public static final String HEADS_PREFIX = REFS_PREFIX + "heads/";
 
     /**
      * By convention, the origin of the repository
      */
     public static final String ORIGIN = "refs/remotes/origin";
 
     private String name;
 
     private RevObject.TYPE type;
 
     private ObjectId objectId;
 
     /**
      * Constructs a new Ref with the given parameters.
      * 
      * @param name name of this ref
      * @param oid object id for this ref
      * @param type object type
      * @see RevObject.TYPE
      */
     public Ref(final String name, final ObjectId oid, @Nullable final RevObject.TYPE type) {
         Preconditions.checkNotNull(name);
         Preconditions.checkNotNull(oid);
         Preconditions.checkArgument(oid.isNull() || type != null);
         this.name = name;
         this.objectId = oid;
         this.type = type;
     }
 
     /**
      * @return the name for this ref
      */
     public String getName() {
         return name;
     }
 
     /**
      * @return the name for this ref with the prefix removed
      */
     public String localName() {
         return localName(name);
     }
 
     /**
      * @param ref the ref string
      * @return the local name of the given ref string
      */
     public static String localName(final String ref) {
 
         if (ref.startsWith(HEADS_PREFIX)) {
             return ref.substring(HEADS_PREFIX.length());
         } else if (ref.startsWith(TAGS_PREFIX)) {
             return ref.substring(TAGS_PREFIX.length());
         } else if (ref.startsWith(REMOTES_PREFIX)) {
             String remoteWithBranch = ref.substring(REMOTES_PREFIX.length());
             return remoteWithBranch.substring(remoteWithBranch.indexOf('/') + 1);
         } else if (ref.startsWith(REFS_PREFIX)) {
             return ref.substring(REFS_PREFIX.length());
         }
         return ref;
     }
 
     /**
      * @return the namespace for this ref
      */
     public String namespace() {
         return namespace(name);
     }
 
     /**
      * @param ref the ref string
      * @return the namespace of the given ref string
      */
     public static String namespace(final String ref) {
         if (ref.startsWith(HEADS_PREFIX)) {
             return HEADS_PREFIX;
         } else if (ref.startsWith(TAGS_PREFIX)) {
             return TAGS_PREFIX;
         } else if (ref.startsWith(REMOTES_PREFIX)) {
             String remote = ref.substring(REMOTES_PREFIX.length());
             remote = remote.substring(0, remote.indexOf('/'));
             return REMOTES_PREFIX + "/" + remote;
         } else if (ref.startsWith(REFS_PREFIX)) {
             return REFS_PREFIX;
         }
         return ref;
     }
 
     /**
      * @return the object id being referenced
      */
     public ObjectId getObjectId() {
         return objectId;
     }
 
     /**
      * @return the type of the object this ref refers to
      * @see RevObject.TYPE
      */
     public RevObject.TYPE getType() {
         return type;
     }
 
     /**
      * @param o object to compare against
      * @return whether or not this ref is equal to the target object
      */
     @Override
     public boolean equals(Object o) {
         if (!(o instanceof Ref)) {
             return false;
         }
         Ref r = (Ref) o;
        return name.equals(r.getName())
                && (type == null ? r.getType() == null : type.equals(r.getType()))
                 && objectId.equals(r.getObjectId());
     }
 
     /**
      * @return a hash code for this ref
      */
     @Override
     public int hashCode() {
         return name.hashCode() * objectId.hashCode();
     }
 
     /**
      * @see java.lang.Comparable#compareTo(java.lang.Object)
      */
     @Override
     public int compareTo(Ref o) {
         return name.compareTo(o.getName());
     }
 
     /**
      * @return the ref represented by a readable string
      */
     @Override
     public String toString() {
         return new StringBuilder("Ref").append('[').append(name).append(" -> ").append(objectId)
                 .append(']').toString();
     }
 
     public static String append(String namespace, String child) {
         StringBuilder sb = new StringBuilder();
         if (namespace.endsWith("/")) {
             namespace = namespace.substring(0, namespace.length() - 1);
         }
         sb.append(namespace);
 
         if (child.startsWith("/")) {
             child = child.substring(1);
         }
         if (child.endsWith("/")) {
             child = child.substring(0, child.length() - 1);
         }
         if (!child.isEmpty()) {
             if (!namespace.isEmpty()) {
                 sb.append("/");
             }
             sb.append(child);
         }
         return sb.toString();
     }
 
     /**
      * @return the relative name of the ref given by its full name and the namespace to truncate
      */
     public static String child(String namespace, String ref) {
         Preconditions.checkState(ref.startsWith(namespace));
         String relative = ref.substring(namespace.length());
         if (relative.startsWith("/")) {
             relative = relative.substring(1);
         }
         return relative;
     }
 }
