 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 package rewrite.org.apache.myfaces.extensions.scripting.monitor;
 
 import rewrite.org.apache.myfaces.extensions.scripting.common.ScriptingConst;
 import rewrite.org.apache.myfaces.extensions.scripting.common.util.ClassUtils;
 import rewrite.org.apache.myfaces.extensions.scripting.context.WeavingContext;
 
 import java.io.File;
 import java.util.Collection;
 import java.util.logging.Logger;
 
 /**
  * @author Werner Punz (latest modification by $Author$)
  * @version $Revision$ $Date$
  */
 
 public class ClassResource extends WatchedResource
 {
 
     Logger logger = Logger.getLogger(this.getClass().getName());
 
     /*
     * volatile due to the ram concurrency behavior
     * of the instance vars jdk 5+
     */
 
     //TODO we probably can drop the file definitions
     //the class has all meta data internally via findResource
     //on its corresponding classloader
     //caching the info however probably is faster
     volatile Class _aClass = null;
     volatile File _sourceFile;
 
     volatile int _scriptingEngine = ScriptingConst.ENGINE_TYPE_JSF_NO_ENGINE;

     volatile long _lastLoaded = -1L;
 
     //todo clean up the sourcepath and filename
 
     //--- todo move this into a separate resource handling facility
 
 
 
     @Override
     /**
      * returns the source file in this case
      */
     public File getFile()
     {
         try
         {
             return _sourceFile;
         }
         catch (NullPointerException ex)
         {
             return null;
         }
     }
 
     public void setFile(File sourceFile)
     {
         _sourceFile = sourceFile;
     }
 
     public Class getAClass()
     {
         return _aClass;
     }
 
     public void setAClass(Class aClass)
     {
         this._aClass = aClass;
     }
 
     public int getScriptingEngine()
     {
         return _scriptingEngine;
     }
 
     public void setScriptingEngine(int scriptingEngine)
     {
         this._scriptingEngine = scriptingEngine;
     }
 
     @Override
     protected Object clone() throws CloneNotSupportedException
     {
         ClassResource retVal = (ClassResource) super.clone();
         return retVal;
     }
 
     public String getSourceFile()
     {
         return _sourceFile.getAbsolutePath().substring(getSourceDir().length());
     }
 
     public String getSourceDir()
     {
         Collection<String> sourceRoots = WeavingContext.getInstance().getConfiguration().getSourceDirs(_scriptingEngine);
         String fileDir = _sourceFile.getAbsolutePath();
         fileDir = fileDir.replaceAll("\\\\", "/");
         for (String sourceRoot : sourceRoots)
         {
             sourceRoot = sourceRoot.replaceAll("\\\\", "/");
             if (fileDir.startsWith(sourceRoot))
             {
                 return sourceRoot;
             }
         }
         return null;
     }
 
     public void executeLastLoaded()
     {
         _lastLoaded = System.currentTimeMillis();
     }
 
     public long getLastLoaded()
     {
         return _lastLoaded;
     }
 
     /**
      * identifier for this resource is the classname
      *
      * @return
      */
     public String getIdentifier()
     {
         String targetDir = WeavingContext.getInstance().getConfiguration().getCompileTarget().getAbsolutePath();
         String className = ClassUtils.relativeFileToClassName(getSourceFile());
         return className;
     }
 
     public void setTainted(boolean value)
     {
         if (isTainted()) return;
         if (value)
         {
             //TODO add logging event here
             logger.info("[EXT-SCRIPTING] tainting " + getSourceFile());
         }
         tainted = true;
     }
 
     /**
      * @return true if the source file has been modified compared to its classfile
      */
     public boolean isTainted()
     {
         return tainted;
     }
 
     public boolean needsRecompile()
     {
         String targetDir = WeavingContext.getInstance().getConfiguration().getCompileTarget().getAbsolutePath();
         String className = ClassUtils.relativeFileToClassName(getSourceFile());
         className = targetDir + File.separator + className.replaceAll("\\.", File.separator) + ".class";
         File targetClass = new File(className);
         return !targetClass.exists() || targetClass.lastModified() < _sourceFile.lastModified();
     }
 
     public boolean stillExists()
     {
         return _sourceFile.exists();
     }
 
 }
