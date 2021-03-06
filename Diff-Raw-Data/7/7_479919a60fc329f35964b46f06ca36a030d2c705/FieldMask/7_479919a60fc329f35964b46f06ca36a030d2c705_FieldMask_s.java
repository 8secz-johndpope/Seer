 //
 // $Id$
 //
 // samskivert library - useful routines for java programs
 // Copyright (C) 2001-2007 Michael Bayne
 //
 // This library is free software; you can redistribute it and/or modify it
 // under the terms of the GNU Lesser General Public License as published
 // by the Free Software Foundation; either version 2.1 of the License, or
 // (at your option) any later version.
 //
 // This library is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 // Lesser General Public License for more details.
 //
 // You should have received a copy of the GNU Lesser General Public
 // License along with this library; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 
 package com.samskivert.jdbc.jora;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Provides support for doing partial updates to objects in a JORA table.
  * A field mask can be obtained for a particular table, fields marked as
  * modified and the object subsequently updated in a fairly
  * straightforward manner:
  *
  * <pre>
  * // updating parts of a table that contains User objects
  * User user = // load user object from table
  * FieldMask mask = table.getFieldMask();
  * user.firstName = newFirstName;
  * mask.setModified("firstName");
  * user.lastName = newLastName;
  * mask.setModified("lastName");
  * table.update(user, mask);
  * </pre>
  */
 public class FieldMask
     implements Cloneable
 {
     /**
      * Creates a field mask for a {@link Table} that uses the supplied
      * field descriptors.
      */
     public FieldMask (FieldDescriptor[] descrips)
     {
         // create a mapping from field name to descriptor index
         _descripMap = new HashMap<String,Integer>();
         int dcount = (descrips == null ? 0 : descrips.length);
         for (int i = 0; i < dcount; i++) {
             _descripMap.put(descrips[i].field.getName(), Integer.valueOf(i));
         }
         // create our modified flags
         _modified = new boolean[dcount];
     }
 
     /**
      * Returns true if any of the fields in this mask are modified.
      */
     public final boolean isModified ()
     {
         int mcount = _modified.length;
         for (int ii = 0; ii < mcount; ii++) {
             if (_modified[ii]) {
                 return true;
             }
         }
         return false;
     }
 
     /**
      * Returns true if the field with the specified index is modified.
      */
     public final boolean isModified (int index)
     {
         return _modified[index];
     }
 
     /**
      * Returns true if the field with the specified name is modifed.
      */
     public final boolean isModified (String fieldName)
     {
         Integer index = _descripMap.get(fieldName);
         if (index == null) {
            String errmsg = "Passed in field not in mask.";
            throw new IllegalArgumentException(errmsg);
         }
         return _modified[index.intValue()];
     }
 
     /**
      * Returns true only if the set of modified fields is a subset of the
      * fields specified.
      */
     public final boolean onlySubsetModified (Set<String> fieldSet)
     {
         for (String field : _descripMap.keySet()) {
             if (isModified(field) && (!fieldSet.contains(field))) {
                 return false;
             }
         }
         return true;
     }
 
     /**
      * Marks the specified field as modified.
      */
     public void setModified (String fieldName)
     {
         Integer index = _descripMap.get(fieldName);
         if (index == null) {
            String errmsg = "";
            throw new IllegalArgumentException(errmsg);
         }
         _modified[index.intValue()] = true;
     }
 
     /**
      * Clears out the modification state of the fields in this mask.
      */
     public void clear ()
     {
         Arrays.fill(_modified, false);
     }
 
     /**
      * Creates a copy of this field mask, with all fields set to
      * not-modified.
      */
     @Override
     public Object clone ()
     {
         try {
             FieldMask mask = (FieldMask)super.clone();
             mask._modified = new boolean[_modified.length];
             return mask;
         } catch (CloneNotSupportedException cnse) {
             throw new RuntimeException("Oh god, the clones!");
         }
     }
 
     @Override
     public String toString ()
     {
         // return a list of the modified fields
         StringBuilder buf = new StringBuilder("FieldMask [modified={");
         boolean added = false;
         for (Map.Entry<String,Integer> entry : _descripMap.entrySet()) {
             if (_modified[entry.getValue().intValue()]) {
                 if (added) {
                     buf.append(", ");
                 } else {
                     added = true;
                 }
                 buf.append(entry.getKey());
             }
         }
         buf.append("}]");
 
         return buf.toString();
     }
 
     /** Modified flags for each field of an object in this table. */
     protected boolean[] _modified;
 
     /** A mapping from field names to field descriptor index. */
     protected HashMap<String,Integer> _descripMap;
 }
