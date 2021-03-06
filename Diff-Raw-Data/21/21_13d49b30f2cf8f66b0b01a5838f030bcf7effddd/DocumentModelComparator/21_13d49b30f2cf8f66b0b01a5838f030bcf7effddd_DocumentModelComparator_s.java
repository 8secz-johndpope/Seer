 /*
  * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
  *
  * $Id$
  */
 
 package org.nuxeo.ecm.core.api;
 
 import java.text.Collator;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.nuxeo.ecm.core.api.model.PropertyException;
 
 /**
  * DocumentModel comparator. Uses ordering independent of case or accent. If two
  * values are integers/longs, numbering comparison is used.
  *
  * @author Florent Guillaume
  * @author Anahide Tchertchian
  */
 public class DocumentModelComparator implements Sorter {
 
     private static final long serialVersionUID = 1L;
 
     public static final String ORDER_ASC = "asc";
 
     static final Collator collator = Collator.getInstance();
 
     static {
         collator.setStrength(Collator.PRIMARY); // case+accent independent
     }
 
     final String schemaName;
 
     final Map<String, String> orderBy;
 
     /**
      * Constructor using a map of property names to compare on.
      *
      * @param orderBy map using property names as keys, and "asc" or "desc" as
      *            values. Should be a {@link LinkedHashMap} if order of criteria
      *            matters.
      */
     public DocumentModelComparator(Map<String, String> orderBy) {
         this.schemaName = null;
         this.orderBy = orderBy;
     }
 
     /**
      * Constructor using a schema and a map of field names to compare on.
      *
      * @param schemaName the schema name
      * @param orderBy map using property names as keys, and "asc" or "desc" as
      *            values. Should be a {@link LinkedHashMap} if order of criteria
      *            matters.
      */
     public DocumentModelComparator(String schemaName,
             Map<String, String> orderBy) {
         this.schemaName = schemaName;
         this.orderBy = orderBy;
     }
 
     protected int compare(Object v1, Object v2, boolean asc) {
         if (v1 == null && v2 == null) {
             return 0;
         } else if (v1 == null) {
             return asc ? -1 : 1;
         } else if (v2 == null) {
             return asc ? 1 : -1;
         }
         final int cmp;
         if (v1 instanceof Long && v2 instanceof Long) {
             cmp = ((Long) v1).compareTo((Long) v2);
         } else if (v1 instanceof Integer && v2 instanceof Integer) {
             cmp = ((Integer) v1).compareTo((Integer) v2);
         } else {
             cmp = collator.compare(v1.toString(), v2.toString());
         }
         return asc ? cmp : -cmp;
     }
 
     public int compare(DocumentModel doc1, DocumentModel doc2) {
        int cmp;
         if (schemaName != null) {
             final DataModel d1 = doc1.getDataModel(schemaName);
             final DataModel d2 = doc2.getDataModel(schemaName);
             for (Entry<String, String> e : orderBy.entrySet()) {
                 final String fieldName = e.getKey();
                 final boolean asc = ORDER_ASC.equals(e.getValue());
                 final Object v1 = d1.getData(fieldName);
                 final Object v2 = d2.getData(fieldName);
                 cmp = compare(v1, v2, asc);
                 if (cmp == 0) {
                     continue;
                 }
             }
         } else {
             for (Entry<String, String> e : orderBy.entrySet()) {
                 final String propertyName = e.getKey();
                 final boolean asc = ORDER_ASC.equals(e.getValue());
                 Object v1;
                 try {
                     v1 = doc1.getPropertyValue(propertyName);
                 } catch (PropertyException pe) {
                     v1 = null;
                 }
                 Object v2;
                 try {
                     v2 = doc2.getPropertyValue(propertyName);
                 } catch (PropertyException pe) {
                     v2 = null;
                 }
                 cmp = compare(v1, v2, asc);
                 if (cmp == 0) {
                     continue;
                 }
             }
         }
        // everything being equal, provide consistent ordering
        if (doc1.hashCode() == doc2.hashCode()) {
            return 0;
        } else if (doc1.hashCode() < doc2.hashCode()) {
            return -1;
        } else {
            return 1;
         }
     }
 }
