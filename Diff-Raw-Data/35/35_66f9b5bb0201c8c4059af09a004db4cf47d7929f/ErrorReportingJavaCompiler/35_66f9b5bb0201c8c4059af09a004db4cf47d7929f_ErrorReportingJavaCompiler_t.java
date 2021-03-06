 /*
  * Copyright 2011 Google Inc. All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.google.errorprone;
 
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
 import com.sun.source.util.TreePathScanner;
 import com.sun.tools.javac.comp.AttrContext;
 import com.sun.tools.javac.comp.Env;
 import com.sun.tools.javac.main.JavaCompiler;
 import com.sun.tools.javac.util.Context;
 import com.sun.tools.javac.util.Context.Factory;
 
import java.util.LinkedList;
 import java.util.Queue;
 
 /**
  *
  * @author alexeagle@google.com (Alex Eagle)
  */
 public class ErrorReportingJavaCompiler extends JavaCompiler {
 
   public ErrorReportingJavaCompiler(Context context) {
     super(context);
   }
 
   /**
    * Adds an initialization hook to the Context, such that each subsequent
    * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
    * superclass, JavaCompiler) will actually construct and return our version.
    * It's necessary since many new JavaCompilers may
    * be requested for later stages of the compilation (annotation processing),
    * within the same Context. And it's the preferred way for extending behavior
    * within javac, per the documentation in {@link com.sun.tools.javac.util.Context}.
    */
   public static void preRegister(final Context context) {
     context.put(compilerKey, new Factory<JavaCompiler>() {
       //@Override for OpenJDK 7 only
       public JavaCompiler make(Context context) {
         return new ErrorReportingJavaCompiler(context);
       }
       //@Override for OpenJDK 6 only
       public JavaCompiler make() {
         return new ErrorReportingJavaCompiler(context);
       }
     });
   }
 
   @Override
   protected void flow(Env<AttrContext> attrContextEnv, Queue<Env<AttrContext>> envs) {
     super.flow(attrContextEnv, envs);
     postFlow(attrContextEnv);
   }
 
   /**
    * Run Error Prone analysis after performing dataflow checks.
    */
   public void postFlow(Env<AttrContext> env) {
     JavacErrorRefactorListener logReporter = new JavacErrorRefactorListener(log,
         env.toplevel.endPositions,
         env.enclClass.sym.sourcefile != null
             ? env.enclClass.sym.sourcefile
             : env.toplevel.sourcefile);
     VisitorState visitorState = new VisitorState(context, logReporter);
     Scanner scanner = (Scanner) context.get(TreePathScanner.class);
    
    /* Each env corresponds to a parse tree (I *think* always a top-level
     * class), not necessarily to a file as we had previously thought.  Here we
     * walk up the env to generate a TreePath for this parse tree, then scan it.
     *
     * TODO(eaftan): Note that because we are scanning parse trees and not
     * compilation units, we can never encounter file-level tree nodes, such as
     * import statements.  Problem?
     */
    
    // create list of Tree nodes from current to top
    LinkedList<Tree> pathList = new LinkedList<Tree>();
    Env<AttrContext> envForPath = env;
    pathList.addFirst(envForPath.tree);
    while (envForPath.outer != null) {
      envForPath = envForPath.outer;
      pathList.addFirst(envForPath.tree);
    }
    
    // generate TreePath based on list
    if (!(pathList.element() instanceof CompilationUnitTree)) {
      throw new IllegalStateException("Expected top of tree to be a compilation unit");
    }
    TreePath path = new TreePath((CompilationUnitTree) pathList.remove());
    for (Tree t : pathList) {
      path = new TreePath(path, t);
    }
    scanner.scan(path, visitorState);
   }
 }
