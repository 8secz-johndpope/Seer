 /**
  * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
 package test.net.sourceforge.pmd.rules;
 
 import net.sourceforge.pmd.PMD;
 import net.sourceforge.pmd.Rule;
 import net.sourceforge.pmd.rules.XPathRule;
 import test.net.sourceforge.pmd.testframework.SimpleAggregatorTst;
 import test.net.sourceforge.pmd.testframework.TestDescriptor;
 
 public class IfElseStmtsMustUseBracesRuleTest extends SimpleAggregatorTst {
 
     private Rule rule;
 
     public void setUp() {
         rule = new XPathRule();
        rule.addProperty("xpath", "//IfStatement[count(*) > 2][not(Statement/Block)]");
     }
 
     public void testAll() {
        runTests(new TestDescriptor[] {
           new TestDescriptor(TEST1, "simple failure case", 1, rule),
            new TestDescriptor(TEST2, "ok", 0, rule),
        });
     }
 
     private static final String TEST1 =
     "public class Foo {" + PMD.EOL +
     " public void foo() {     " + PMD.EOL +
    "  if (true) " + PMD.EOL +
    "   y=2;" + PMD.EOL +
    "  else " + PMD.EOL +
    "   x=4;" + PMD.EOL +
     " }" + PMD.EOL +
     "}";
 
     private static final String TEST2 =
     "public class Foo {" + PMD.EOL +
     " public void foo() {     " + PMD.EOL +
     "  if (true) {" + PMD.EOL +
     "   x=2;" + PMD.EOL +
     "  } else {" + PMD.EOL +
     "   x=4;" + PMD.EOL +
     "  }" + PMD.EOL +
     " }" + PMD.EOL +
     "}";
 }
