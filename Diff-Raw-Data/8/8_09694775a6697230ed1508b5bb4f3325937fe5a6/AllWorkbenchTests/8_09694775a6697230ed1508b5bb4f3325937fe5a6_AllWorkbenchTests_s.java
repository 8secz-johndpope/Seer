 package com.python.pydev.runalltests2;
 
 import junit.framework.Test;
 import junit.framework.TestSuite;
 
 import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaClassModuleTestWorkbench;
 import org.python.pydev.editor.codecompletion.revisited.jython.JythonCompletionWithBuiltinsTestWorkbench;
 import org.python.pydev.editor.codecompletion.revisited.jython.JythonFindDefinitionTestWorkbench;
 
 import com.python.pydev.codecompletion.JavaIntegrationPydevComTestWorkbench;
 
 public class AllWorkbenchTests {
 
     public static Test suite() {
         TestSuite suite = new TestSuite(AllWorkbenchTests.class.getName());
         suite.addTestSuite(JythonCompletionWithBuiltinsTestWorkbench.class);
         suite.addTestSuite(JythonFindDefinitionTestWorkbench.class);
         suite.addTestSuite(JavaClassModuleTestWorkbench.class); 
         suite.addTestSuite(JavaIntegrationPydevComTestWorkbench.class); 
         if (suite.countTestCases() == 0) {
             throw new Error("There are no test cases to run");
         } else {
             return suite;
         }
     }
 
 }
