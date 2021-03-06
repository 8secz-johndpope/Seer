 package org.jboss.errai.bus.client.json;
 
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.Map;
 
 public class JSONEncoderCli {
     boolean defer = false;
 
     public String encode(Object v) {
         return _encode(v);
     }
 
     public String _encode(Object v) {
         if (v == null) {
             return "null";
         } else if (v instanceof String) {
             return "\"" + v + "\"";
         } else if (v instanceof Number || v instanceof Boolean) {
             return String.valueOf(v);
         }
         else if (v instanceof Collection) {
             return encodeCollection((Collection) v);
         } else if (v instanceof Map) {
             return encodeMap((Map) v);
         } else if (v instanceof Object[]) {
             return encodeArray((Object[]) v);
         } else {
             defer = true;
             return null;
         }
     }
 
     public String encodeMap(Map<Object, Object> map) {
         StringBuilder mapBuild = new StringBuilder("{");
         boolean first = true;
 
         for (Map.Entry<Object, Object> entry : map.entrySet()) {
             String val = _encode(entry.getValue());
             if (!defer) {
                 if (!first) {
                     mapBuild.append(",");
                 }
                 mapBuild.append(_encode(entry.getKey()))
                         .append(":").append(val);
             } else {
                 defer = false;
             }

            first = false;
         }
 
         return mapBuild.append("}").toString();
     }
 
     private String encodeCollection(Collection col) {
         StringBuilder buildCol = new StringBuilder("[");
         Iterator iter = col.iterator();
         while (iter.hasNext()) {
             buildCol.append(_encode(iter.next()));
             if (iter.hasNext()) buildCol.append(buildCol.append(","));
         }
         return buildCol.append("]").toString();
     }
 
     private String encodeArray(Object[] array) {
         StringBuilder buildCol = new StringBuilder("[");
         for (int i = 0; i < array.length; i++) {
             buildCol.append(_encode(array[i]));
             if ((i + 1) < array.length) buildCol.append(",");
         }
         return buildCol.append("]").toString();
     }
 
 }
