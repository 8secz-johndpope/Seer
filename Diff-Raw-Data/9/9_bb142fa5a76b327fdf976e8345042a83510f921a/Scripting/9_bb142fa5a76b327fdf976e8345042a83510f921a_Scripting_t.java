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
  *     bstefanescu
  *
  * $Id$
  */
 
 package org.nuxeo.ecm.webengine.scripting;
 
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.Reader;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
 import javax.script.Bindings;
 import javax.script.Compilable;
 import javax.script.CompiledScript;
 import javax.script.ScriptContext;
 import javax.script.ScriptEngine;
 import javax.script.ScriptEngineManager;
 import javax.script.ScriptException;
 import javax.script.SimpleBindings;
 import javax.script.SimpleScriptContext;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.nuxeo.common.utils.FileUtils;
 import org.nuxeo.ecm.webengine.WebContext;
 import org.python.core.PyDictionary;
 import org.python.core.PyList;
 import org.python.core.PyObject;
 import org.python.core.PyTuple;
 
 /**
  * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
  *
  */
 public class Scripting {
 
     private final static Log log = LogFactory.getLog(Scripting.class);
 
     private final ConcurrentMap<File, Entry> cache = new ConcurrentHashMap<File, Entry>();
 
     protected ScriptEngineManager scriptMgr = new ScriptEngineManager();
 
 
     public Scripting() {
     }
 
 
     public static CompiledScript compileScript(ScriptEngine engine, File file) throws ScriptException {
         if (engine instanceof Compilable) {
             Compilable comp = (Compilable)engine;
             try {
                 Reader reader = new FileReader(file);
                 try {
                     return comp.compile(reader);
                 } finally {
                     reader.close();
                 }
             } catch (IOException e) {
                 throw new ScriptException(e);
             }
         } else {
             return null;
         }
     }
 
     public Object runScript(WebContext context, ScriptFile script) throws Exception {
         return runScript(context, script, null);
     }
 
     public Object runScript(WebContext context, ScriptFile script, Bindings args) throws Exception {
         if (log.isDebugEnabled()) {
             log.debug("## Running Script: "+script.getFile());
         }
         if (args == null) {
             args = new SimpleBindings();
         }
         String ext = script.getExtension();
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@: enter script: "+ext);
        if ("groovy".equals(ext)) {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@ groovy");
            return new GroovyShell(new Binding(args)).evaluate(script.getFile());
        }
         // check for a script engine
         ScriptEngine engine = scriptMgr.getEngineByExtension(ext);
         if (engine != null) {
             ScriptContext ctx = new SimpleScriptContext();
             ctx.setBindings(args, ScriptContext.ENGINE_SCOPE);
             CompiledScript comp = getCompiledScript(engine, script.getFile()); // use cache for compiled scripts
             if (comp != null) {
                 return comp.eval(ctx);
             } // compilation not supported - eval it on the fly
             try {
                 Reader reader = new FileReader(script.getFile());
                 try { // TODO use __result__ to pass return value for engine that doesn't returns like jython
                     Object result = engine.eval(reader, ctx);
                     if (result == null) {
                         result = args.get("__result__");
                     }
                     return result;
                 } finally {
                     reader.close();
                 }
             } catch (IOException e) {
                 throw new ScriptException(e);
             }
         } else { // no script engine - may be a resource file - copy the file to the output stream
             FileInputStream in = new FileInputStream(script.getFile());
             try {
                 FileUtils.copy(in, context.getResponse().getOutputStream());
             } finally {
                 in.close();
             }
         }
         return null;
     }
 
     @SuppressWarnings("unchecked")
     public static Map convertPythonMap(PyDictionary dict) {
         PyList list = dict.items();
         Map<String, PyObject> table = new HashMap();
         for(int i = list.__len__(); i-- >  0; ) {
             PyTuple tup = (PyTuple) list.__getitem__(i);
             String key = tup.__getitem__(0).toString();
             table.put(key, tup.__getitem__(1));
         }
         return table;
     }
 
     public CompiledScript getCompiledScript(ScriptEngine engine, File file) throws ScriptException {
         Entry entry = cache.get(file);
         long tm = file.lastModified();
         if (entry != null) {
             if (entry.lastModified < tm) { // recompile
                 entry.script = compileScript(engine, file);
                 entry.lastModified = tm;
             }
             return entry.script;
         }
         CompiledScript script = compileScript(engine, file);
         if (script != null) {
             cache.putIfAbsent(file, new Entry(script, tm));
             return script;
         }
         return null;
     }
 
     class Entry {
         public CompiledScript script;
         public long lastModified;
 
         Entry(CompiledScript script, long lastModified) {
             this.lastModified = lastModified;
             this.script = script;
         }
     }
 }
