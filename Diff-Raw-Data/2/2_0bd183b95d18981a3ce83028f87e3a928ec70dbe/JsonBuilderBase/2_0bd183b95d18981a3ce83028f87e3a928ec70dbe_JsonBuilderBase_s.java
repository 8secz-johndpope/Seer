 /*
  * Copyright 2011, The gwtquery team.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.gwt.query.client.builders;
 
 import com.google.gwt.core.client.JavaScriptObject;
 import com.google.gwt.core.client.JsArray;
 import com.google.gwt.query.client.Function;
 import com.google.gwt.query.client.Properties;
 import com.google.gwt.query.client.js.JsObjectArray;
 import com.google.gwt.query.client.js.JsUtils;
 
 public abstract class JsonBuilderBase<J extends JsonBuilderBase<?>> implements JsonBuilder {
   
   protected Properties p = Properties.create();
 
   @SuppressWarnings("unchecked")
   public J parse(String json) {
     return load(JsUtils.parseJSON(json));
   }
   
   @SuppressWarnings("unchecked")
   public J parse(String json, boolean fix) {
     return fix ? parse(Properties.wrapPropertiesString(json)) : parse(json);
   }
   
   @SuppressWarnings("unchecked")
   public J load(Object prp) {
     assert prp == null || prp instanceof JavaScriptObject || prp instanceof String;
     if (prp != null && prp instanceof String) {
         return parse((String)prp);
     }
     int i = -1;
     p = prp == null ? Properties.create() : (Properties)prp;
     while (p != null && i != p.hashCode() && JsUtils.isArray(p)) {
       i = p.hashCode(); 
       p = p.get(0);
     }
     return (J)this;
   }
 
   protected <T> void setArrayBase(String n, T[] r) {
     if (r.length > 0 && r[0] instanceof JsonBuilder) {
         JsArray<JavaScriptObject> a = JavaScriptObject.createArray().cast();
         for (T o : r) {
           a.push(((JsonBuilder)o).getProperties());
         }
         p.set(n, a);
     } else {
       JsObjectArray<Object> a = JsObjectArray.create();
       a.add(r);
       p.set(n, a);
     }
   }
   
   @SuppressWarnings("unchecked")
   protected <T> T[] getArrayBase(String n, T[] r, Class<T> clazz) {
     JsObjectArray<?> a = p.getArray(n).cast();
     int l = r.length;
     for (int i = 0 ; i < l ; i++) {
       Object w = a.get(i);
       Class<?> c = w.getClass();
       do {
         if (c.equals(clazz)) {
           r[i] = (T)w;
           break;
         }
         c = c.getSuperclass();
       } while (c != null);      
     }
     return r;
   }
   
   protected Properties getPropertiesBase(String n) {
     Properties r = p.getJavaScriptObject(n);
     return r != null ? r : Properties.create();
   }
   
   protected Function getFunctionBase(String n) {
    final Object o = p.get("success");
     if (o != null) {
       if (o instanceof Function) {
         return (Function)o;
       } else if (o instanceof JavaScriptObject 
           && JsUtils.isFunction((JavaScriptObject)o)) {
         return new Function() {
           private native void exec(JavaScriptObject f, Object data) /*-{
             f(data);
           }-*/;
           public void f() {
             exec((JavaScriptObject)o, getData()[0]);
           }
         };
       }
     }
     return null;
   }
   
   public String toString() {
     return p.tostring();
   }
   
   public Properties getProperties() {
     return p;
   }
 }
