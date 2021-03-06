 // @(#) $Id$
 
 package net.happygiraffe.jslint;
 
 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.List;
 
 import junit.framework.TestCase;
 
 public class JSLintTest extends TestCase {
 
     private JSLint lint;
 
     // small helper function.
     private List<Issue> lint(Reader reader) throws IOException {
         return lint.lint("-", reader);
     }
 
     // small helper function.
     private List<Issue> lint(String source) {
         return lint.lint("-", source);
     }
 
     @Override
     protected void setUp() throws Exception {
         lint = new JSLint();
     }
 
     public void testEmptySource() throws Exception {
         List<Issue> issues = lint("");
         assertNotNull(issues);
         assertEquals(0, issues.size());
     }
 
     public void testLintReader() throws Exception {
         Reader reader = new StringReader("var foo = 1");
         List<Issue> issues = lint(reader);
         assertNotNull(issues);
         assertEquals(1, issues.size());
         assertEquals("Missing semicolon.", issues.get(0).getReason());
     }
 
     public void testNoProblems() throws IOException {
         List<Issue> problems = lint("var foo = 1;");
         assertEquals(0, problems.size());
     }
 
     public void testNullSource() throws Exception {
         List<Issue> issues = lint((String) null);
         assertNotNull(issues);
         assertEquals(0, issues.size());
     }
 
     public void testOneProblem() throws IOException {
         // There is a missing semicolon here.
         List<Issue> problems = lint("var foo = 1");
         assertEquals(1, problems.size());
         Issue issue = problems.get(0);
         assertEquals("Missing semicolon.", issue.getReason());
     }
 
     public void testResetOptions() throws Exception {
         String eval_js = "eval('1');";
         lint.addOption(Option.EVIL);
         lint.resetOptions();
         List<Issue> issues = lint(eval_js);
         assertEquals(1, issues.size());
         assertEquals("eval is evil.", issues.get(0).getReason());
     }

     public void testSetOption() throws Exception {
         String eval_js = "eval('1');";
         // should be disallowed by default.
         List<Issue> issues = lint(eval_js);
         assertEquals("evil disallowed", 1, issues.size());
         // Now should be a problem.
         lint.addOption(Option.EVIL);
         issues = lint(eval_js);
         assertEquals("evil allowed", 0, issues.size());
     }

    // http://code.google.com/p/jslint4java/issues/detail?id=1
    public void testUnableToContinue() throws Exception {
        List<Issue> issues = lint("new Number();");
        assertEquals(2, issues.size());
        assertEquals("'new' should not be used as a statement.", issues.get(0)
                .getReason());
        assertEquals("Stopping, unable to continue. (0% scanned).", issues.get(
                1).getReason());
    }
 }
