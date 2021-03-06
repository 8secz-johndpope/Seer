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
 
 package org.apache.myfaces.scripting.core.dependencyScan;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 import org.apache.tools.ant.taskdefs.Classloader;
 import org.objectweb.asm.ClassReader;
 
 import java.io.IOException;
 import java.util.HashSet;
 import java.util.Set;
 
 /**
  * @author Werner Punz (latest modification by $Author$)
  * @version $Revision$ $Date$
  *          <p/>
  *          A dependency scanner for
  *          our classes
  *          <p /> this class is thread save on object level
  *          and can be used as a singleton
  *          <p/>
  */
 public class DefaultDependencyScanner implements DependencyScanner {
 
     final ClassScanVisitor cp = new ClassScanVisitor();
    Log log = LogFactory.getLog(this.getClass().getName());

 
     /**
      * @param className
      * @return
      */
     public synchronized final Set<String> fetchDependencies(String className, final Set<String> whiteList) {
         Set<String> retVal = new HashSet<String>();
        investigateInheritanceHierarchy(retVal, className, whiteList);
         return retVal;
     }
 
 
     /**
      * this investigates the classes inheritance hierarchy for
      * more dependencies, for now annotations and interfaces
      * are omitted since they are not vital to our jsf dependency checks
      * (maybe in the long run we will add interfaces and annotations as well
      * but for now we will leave them away for speed reasons)
      *
      * @param retVal
      */
    private final void investigateInheritanceHierarchy(Set<String> retVal, String className, Set<String> whiteList) {
         //we now have to fetch the parent hierarchy
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         try {
             Class toCheck = loader.loadClass(className);
            scanCurrentClass(retVal, className, whiteList);
             Class parent = toCheck.getSuperclass();
 
             while (parent != null && !ClassLogUtils.isStandard(parent.getName())) {
                scanCurrentClass(retVal, parent.getName(), whiteList);
                 parent = parent.getSuperclass();
             }
 
         } catch (ClassNotFoundException e) {
            log.error(e);
         }
     }
 
     /**
      * scans one level of the inheritance hierarchy
      *
      * @param retVal
      * @param currentClassName
      */
    private final void scanCurrentClass(Set<String> retVal, String currentClassName, Set<String> whiteList) {
         cp.setDependencyTarget(retVal);
         cp.setWhiteList(whiteList);
         ClassReader cr = null;
 
         try {
             cr = new ClassReader(currentClassName);
             cr.accept(cp, 0);
         } catch (IOException e) {
            log.error(e);
         }
     }
 
 
 }
