 package org.mvel.util;
 
 import static org.mvel.util.ParseTools.balancedCapture;
 
 import static java.lang.Character.isWhitespace;
 import static java.lang.System.arraycopy;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * This is the inline collection sub-parser.  It produces a skeleton model of the collection which is in turn translated
  * into a sequenced AST to produce the collection efficiently at runtime, and passed off to one of the JIT's if
  * configured.
  *
  * @author Christopher Brock
  */
 public class CollectionParser {
     private char[] property;
 
     private int cursor;
     private int length;
     private int start;
 
     private int type;
 
     public static final int LIST = 0;
     public static final int ARRAY = 1;
     public static final int MAP = 2;
 
     private static final Object[] EMPTY_ARRAY = new Object[0];
 
     public CollectionParser() {
     }
 
     public CollectionParser(int type) {
         this.type = type;
     }
 
     public Object parseCollection(char[] property) {
         this.property = property;
         this.cursor = 0;
         this.length = property.length;
 
         if (length > 0)
             while (length > 0 && isWhitespace(property[length - 1]))
                 length--;
 
         return parseCollection();
     }
 
 
     private Object parseCollection() {
         if (length == 0) {
             if (type == LIST) return new ArrayList();
             else return EMPTY_ARRAY;
         }
 
         Map<Object, Object> map = null;
         List<Object> list = null;
 
         if (type != -1) {
             switch (type) {
                 case ARRAY:
                 case LIST:
                     list = new ArrayList<Object>();
                     break;
                 case MAP:
                     map = new HashMap<Object, Object>();
                     break;
             }
         }
 
         Object curr = null;
         int newType = -1;
 
         for (; cursor < length; cursor++) {
             switch (property[cursor]) {
                 case'{':
                     if (newType == -1) {
                         newType = ARRAY;
                     }
 
                 case'[':
                     if (newType == -1) {
                         newType = LIST;
                     }
 
                     /**
                      * Sub-parse nested collections.
                      */
                     Object o = new CollectionParser(newType).parseCollection(subset(property, (start = cursor) + 1,
                             cursor = balancedCapture(property, start, property[start])));
 
                     if (type == MAP) {
                         map.put(curr, o);
                     }
                     else {
                         list.add(curr = o);
                     }
 
                     if ((start = ++cursor) < (length - 1) && property[start] != ',') {
                         start = ++cursor;
                     }
 
                     continue;
 
                 case'\"':
                 case'\'':
                     if ((cursor = balancedCapture(property, start = cursor, property[start])) == -1) {
                         throw new RuntimeException("unterminated string literal starting at index " + start + " {" + property[start] + "}: " + new String(property));
                     }
 
                     break;
 
                 case',':
                     if (type != MAP) {
                         list.add(new String(property, start, cursor - start));
                     }
                     else {
                         map.put(curr, new String(property, start, cursor - start).trim());
                     }
 
                     start = cursor + 1;
 
                     break;
 
                 case':':
                     if (type != MAP) {
                         map = new HashMap<Object, Object>();
                         type = MAP;
                     }
                     curr = new String(property, start, cursor - start).trim();
 
                     start = cursor + 1;
                     break;
             }
         }
 
         if (start < length) {
             if (cursor < (length - 1)) cursor++;
 
             if (type == MAP) {
                 map.put(curr, new String(property, start, cursor - start).trim());
             }
             else {
                 if (cursor < length) cursor++;
                 list.add(new String(property, start, cursor - start).trim());
             }
         }
 
         switch (type) {
             case MAP:
                 return map;
             case ARRAY:
                 return list.toArray();
             default:
                 return list;
         }
     }
 
 
     private void skipWhitespace() {
         while (cursor < length && isWhitespace(property[cursor])) cursor++;
     }
 
     private static char[] subset(char[] property, int start, int end) {
         while (start < (end - 1) && isWhitespace(property[start]))
             start++;
 
         char[] newA = new char[end - start];
         arraycopy(property, start, newA, 0, end - start);
         return newA;
     }
 
 
     public int getCursor() {
         return cursor;
     }
 
 }
