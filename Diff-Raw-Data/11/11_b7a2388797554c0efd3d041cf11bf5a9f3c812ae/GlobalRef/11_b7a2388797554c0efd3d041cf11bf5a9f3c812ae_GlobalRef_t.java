 /*
  *  This file is part of the X10 project (http://x10-lang.org).
  *
  *  This file is licensed to You under the Eclipse Public License (EPL);
  *  You may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *      http://www.opensource.org/licenses/eclipse-1.0.php
  *
  *  (C) Copyright IBM Corporation 2006-2010.
  */
 package x10.core;
 
 import java.lang.ref.ReferenceQueue;
 import java.lang.ref.WeakReference;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicLong;
 
 import x10.lang.Runtime.Mortal;
 import x10.rtt.Types;
 
 public final class GlobalRef<T> extends x10.core.Struct {
 
     public static final x10.rtt.RuntimeType<GlobalRef<?>> _RTT = new x10.rtt.RuntimeType<GlobalRef<?>>(
             GlobalRef.class,
             new x10.rtt.RuntimeType.Variance[] { x10.rtt.RuntimeType.Variance.INVARIANT }) {
         @Override
         public java.lang.String typeName() {
             return "x10.lang.GlobalRef";
         }
     };
 
     public x10.rtt.RuntimeType<GlobalRef<?>> getRTT() {
         return _RTT;
     }
 
     public x10.rtt.Type<?> getParam(int i) {
         if (i == 0)
             return T;
         return null;
     }
 
     private static class GlobalRefEntry {
         private final Object t;
 
         GlobalRefEntry(Object t) {
             this.t = t;
         }
 
         public int hashCode() {
             return System.identityHashCode(t);
         }
 
         public boolean equals(Object obj) {
             if (this == obj)
                 return true;
             if (!(obj instanceof GlobalRefEntry))
                 return false;
             if (((GlobalRefEntry) obj).t == t)
                 return true;
             // Note: GlobalRef does not refer structs
 //            if (Types.isStruct(((GlobalRefEntry) obj).t)
 //        		&& ((GlobalRefEntry) obj).t.equals(t))
 //            	return true;
             return false;
         }
     }
 
     private static AtomicLong lastId = new AtomicLong(0);
     private static ConcurrentHashMap<Long, Object> id2Object = new ConcurrentHashMap<Long, Object>();
     private static ConcurrentHashMap<GlobalRefEntry, Long> object2Id = new ConcurrentHashMap<GlobalRefEntry, Long>();
 
     private final x10.rtt.Type<?> T;
     final public x10.lang.Place home;
     final private long id; // place local id of referenced object
 
     public GlobalRef(final x10.rtt.Type<?> T, final T t,
             java.lang.Class<?> dummy$0) {
 
         this.T = T;
         this.home = x10.lang.Runtime.home();
 
         Long tmpId = lastId.incrementAndGet();
 
         id2Object.put(tmpId, t);//set id first.
 
         GlobalRefEntry entry = new GlobalRefEntry(t);
         Long existingId = object2Id.putIfAbsent(entry, tmpId);//set object second.
         if (existingId != null) {
             this.id = existingId;
             id2Object.remove(tmpId);
         } else {
             this.id = tmpId;
         }
     }
 
     final public T $apply$G() {
         //always get object because each id is set first and its object is set second.
         return (T) id2Object.get(id);
     }
 
     //this is not an api. only for implementing local assign in at body.
    final public T $set$G(T t) {
        id2Object.put(this.id, t);
        object2Id.put(new GlobalRefEntry(t), this.id);
        return t;
    }
 
     final public x10.lang.Place home() {
         return this.home;
     }
 
     final public java.lang.String toString() {
         return "GlobalRef(" + this.home + "," + this.id + ")";
     }
 
     final public int hashCode() {
         return (this.home.hashCode() << 18) + (int) this.id;
     }
 
     final public boolean equals(java.lang.Object other) {
         return this._struct_equals(other);
     }
 
     final public boolean equals(x10.core.GlobalRef<T> other) {
         return this._struct_equals(other);
     }
 
     final public boolean _struct_equals(java.lang.Object other) {
         if (!x10.core.GlobalRef._RTT.instanceof$(other, T)) {
             return false;
         }
         return this._struct_equals((x10.core.GlobalRef<T>) other);
     }
 
     final public boolean _struct_equals(x10.core.GlobalRef<T> other) {
         return x10.rtt.Equality.equalsequals(this.home, other.home)
                 && x10.rtt.Equality.equalsequals(this.id, other.id);
     }
 
 }
