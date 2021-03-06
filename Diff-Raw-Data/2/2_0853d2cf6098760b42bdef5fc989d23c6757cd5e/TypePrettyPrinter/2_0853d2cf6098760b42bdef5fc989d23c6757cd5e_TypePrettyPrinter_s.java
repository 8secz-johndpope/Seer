 // ex: set sts=4 sw=4 expandtab:
 
 /**
  * Yeti type pretty-printer.
  *
  * Copyright (c) 2010 Madis Janson
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  * 3. The name of the author may not be used to endorse or promote products
  *    derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
  * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package yeti.lang.compiler;
 
 import java.util.*;
 import yeti.lang.*;
 
 class ShowTypeFun extends Fun2 {
     Fun showType;
     String indentStep = "   ";
 
     ShowTypeFun() {
         showType = this;
     }
 
     private void hstr(StringBuffer to, boolean variant,
                       AList fields, String indent) {
         boolean useNL = false;
         AIter i = fields;
         for (int n = 0; i != null; i = i.next())
             if (++n >= 3) {
                 useNL = true;
                 break;
             }
 
         String indent_ = indent, oldIndent = indent;
         if (useNL) {
             if (!variant)
                 indent = indent.concat(indentStep);
             indent_ = indent.concat(indentStep);
         }
 
         String sep = variant
             ? useNL ? "\n" + indent + "| " : " | "
             : useNL ? ",\n".concat(indent) : ", ";
 
         for (i = fields; i != null; i = i.next()) {
             Struct field = (Struct) i.first();
             if (i != fields) // not first
                 to.append(sep);
             else if (useNL && !variant)
                 to.append('\n').append(indent);
             String doc = useNL ? (String) field.get("description") : null;
             if (doc != null && doc.length() > 0)
                 to.append("// ")
                   .append(Core.replace("\n", "\n" + indent + "//", doc))
                   .append('\n')
                   .append(indent);
             if (!variant) {
                 if (field.get("mutable") == Boolean.TRUE)
                     to.append("var ");
                 to.append(field.get("tag"));
             }
             to.append(field.get("name"))
               .append(variant ? " " : " is ")
               .append(showType.apply(indent_, field.get("type")));
         }
         if (useNL && !variant)
             to.append("\n").append(oldIndent);
     }
 
     public Object apply(Object indent, Object typeObj) {
         Tag type = (Tag) typeObj;
         String typeTag = type.name;
         if (typeTag == "Simple")
             return type.value;
         if (typeTag == "Alias") {
             Struct t = (Struct) type.value;
             return '(' + (String) showType.apply(indent, t.get("type"))
                  + " is " + t.get("alias") + ')';
         }
 
         AList typeList;
         String typeName = null;
         if (typeTag == "Parametric") {
             Struct t = (Struct) type.value;
             typeName = (String) t.get("type");
             typeList = (AList) t.get("params");
         } else {
             typeList = (AList) type.value;
         }
         if (typeList != null && typeList.isEmpty())
             typeList = null;
         AIter i = typeList;
         StringBuffer to = new StringBuffer();
 
         if (typeName != null) {
             to.append(typeName).append('<');
             for (; i != null; i = i.next()) {
                 if (i != typeList)
                     to.append(", ");
                 to.append(showType.apply(indent, i.first()));
             }
             to.append('>');
         } else if (typeTag == "Function") {
             for (AIter next; i != null; i = next) {
                 next = i.next();
                 Tag t = (Tag) i.first();
                 if (i != typeList)
                     to.append(" -> ");
                 if (next != null && t.name == "Function")
                     to.append('(')
                       .append(showType.apply(indent, t))
                       .append(')');
                 else
                     to.append(showType.apply(indent, t));
             }
         } else if (typeTag == "Struct") {
             to.append('{');
             hstr(to, false, typeList, (String) indent);
             to.append('}');
         } else if (typeTag == "Variant") {
             hstr(to, true, typeList, (String) indent);
         } else {
             throw new IllegalArgumentException("Unknown type kind: " + typeTag);
         }
         return to.toString();
     }
 }
 
 class TypeDescr {
     int type;
     String name;
     TypeDescr value;
     TypeDescr prev;
     String alias;
     Map properties;
 
     TypeDescr(String name_) {
         name = name_;
     }
 
     private static Struct pair(String name1, Object value1,
                                String name2, Object value2) {
         // low-level implementation-specific struct, don't do that ;)
         Struct3 result = new Struct3(new String[] { name1, name2 }, null);
         result._0 = value1;
         result._1 = value2;
         return result;
     }
 
     Tag force() {
         if (type == 0)
             return new Tag(name, "Simple");
         AList l = null;
         for (TypeDescr i = value; i != null; i = i.prev)
             if (i.properties != null) {
                 i.properties.put("type", i.force());
                 l = new LList(new GenericStruct(i.properties), l);
             } else {
                 l = new LList(i.force(), l);
             }
         Object val = l;
         String tag = null;
         switch (type) {
         case YetiType.FUN:
             tag = "Function"; break;
         case YetiType.MAP:
             val = pair("params", l, "type", name);
             tag = "Parametric"; break;
         case YetiType.STRUCT:
             tag = "Struct"; break;
         case YetiType.VARIANT:
             tag = "Variant"; break;
         }
         Tag res = new Tag(val, tag);
         if (alias == null)
             return res;
         return new Tag(pair("alias", alias, "type", res), "Alias");
     }
 }
 
 class TypePrettyPrinter extends YetiType {
     private Map vars = new HashMap();
     private Map refs = new HashMap();
     
     public static String toString(YType t) {
         return (String) new ShowTypeFun().apply("",
                 new TypePrettyPrinter().prepare(t).force());
     }
 
     private void hdescr(TypeDescr descr, YType tt) {
         Map m = new java.util.TreeMap();
         if (tt.partialMembers != null)
             m.putAll(tt.partialMembers);
         if (tt.finalMembers != null)
             m.putAll(tt.finalMembers);
         Iterator i = m.entrySet().iterator();
         while (i.hasNext()) {
             Map.Entry e = (Map.Entry) i.next();
             Object name = e.getKey();
             YType t = (YType) e.getValue();
             Map it = new IdentityHashMap(5);
             it.put("name", name);
            it.put("descr", t.doc());
             it.put("mutable", Boolean.valueOf(t.field == FIELD_MUTABLE));
             it.put("tag",
                 tt.finalMembers == null || !tt.finalMembers.containsKey(name)
                     ? "." :
                 tt.partialMembers != null && tt.partialMembers.containsKey(name)
                     ? "`" : "");
             TypeDescr field = prepare(t);
             field.properties = it;
             field.prev = descr.value;
             descr.value = field;
         }
     }
 
     private String getVarName(YType t) {
         String v = (String) vars.get(t);
         if (v == null) {
             // 26^7 > 2^32, should be enough ;)
             char[] buf = new char[8];
             int p = buf.length;
             int n = vars.size() + 1;
             while (n > 26) {
                 buf[--p] = (char) ('a' + n % 26);
                 n /= 26;
             }
             buf[--p] = (char) (96 + n);
             buf[--p] = (t.flags & FL_ORDERED_REQUIRED) == 0 ? '\'' : '^';
             v = new String(buf, p, buf.length - p);
             vars.put(t, v);
         }
         return v;
     }
     
     private TypeDescr prepare(YType t) {
         final int type = t.type;
         if (type == VAR) {
             if (t.ref != null)
                 return prepare(t.ref);
             return new TypeDescr(getVarName(t));
         }
         if (type < PRIMITIVES.length)
             return new TypeDescr(TYPE_NAMES[type]);
         if (type == JAVA)
             return new TypeDescr(t.javaType.str());
         if (type == JAVA_ARRAY)
             return new TypeDescr(prepare(t.param[0]).name.concat("[]"));
         TypeDescr descr = (TypeDescr) refs.get(t), item;
         if (descr != null) {
             if (descr.alias == null)
                 descr.alias = getVarName(t);
             return new TypeDescr(descr.alias);
         }
         refs.put(t, descr = new TypeDescr(null));
         descr.type = type;
         YType[] param = t.param;
         switch (type) {
             case FUN:
                 for (; t.type == FUN; param = t.param) {
                     (item = prepare(param[0])).prev = descr.value;
                     descr.value = item;
                     t = param[1].deref();
                 }
                 (item = prepare(t)).prev = descr.value;
                 descr.value = item;
                 break;
             case STRUCT:
             case VARIANT:
                 hdescr(descr, t);
                 break;
             case MAP:
                 int n = 1;
                 YType p1 = param[1].deref();
                 YType p2 = param[2].deref();
                 if (p2.type == LIST_MARKER) {
                     descr.name = p1.type == NONE ? "list" : p1.type == NUM
                                     ? "array" : "list?";
                 } else {
                     descr.name = p2.type == MAP_MARKER || p1.type != NUM
                                     && p1.type != VAR ? "hash" : "map";
                     n = 2;
                 }
                 while (--n >= 0) {
                     (item = prepare(param[n])).prev = descr.value;
                     descr.value = item;
                 }
                 break;
             default:
                 descr.name = "?" + type + '?';
                 break;
         }
         return descr;
     }
 }
