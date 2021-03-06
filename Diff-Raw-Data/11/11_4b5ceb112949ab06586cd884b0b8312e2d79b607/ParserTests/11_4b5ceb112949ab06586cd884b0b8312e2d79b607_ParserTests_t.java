 package test.processing.parsing;
 
 import static org.junit.Assert.*;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.StringWriter;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import processing.app.Base;
 import processing.app.Preferences;
 import processing.app.debug.RunnerException;
 import processing.app.preproc.PdePreprocessor;
 import processing.util.exec.ProcessResult;
 import antlr.ANTLRException;
 import antlr.RecognitionException;
 
 public class ParserTests {
 
   private static final String RESOURCES = "test/resources/";
   private static final UTCompiler COMPILER;
   static {
     try {
       Base.initPlatform();
       COMPILER = new UTCompiler(new File("bin"), new File("../core/bin"));
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
   }
 
   private static File res(final String resourceName) {
     return new File(RESOURCES, resourceName);
   }
 
   @BeforeClass
   static public void initPrefs() throws Exception {
     Preferences.load(new FileInputStream(res("preferences.txt")));
   }
 
   static String read(final File f) {
     try {
       final FileInputStream fin = new FileInputStream(f);
       final InputStreamReader in = new InputStreamReader(fin, "UTF-8");
       try {
         final StringBuilder sb = new StringBuilder();
         final char[] buf = new char[1 << 12];
         int len;
         while ((len = in.read(buf)) != -1)
           sb.append(buf, 0, len);
         return sb.toString();
       } finally {
         in.close();
       }
     } catch (Exception e) {
       throw new RuntimeException("Unexpected", e);
     }
   }
 
   static String preprocess(final String name, final File resource)
       throws RunnerException, ANTLRException {
     final String program = read(resource);
     final StringWriter out = new StringWriter();
     new PdePreprocessor(name, 4).write(out, program);
     return out.toString();
   }
 
   static void expectRecognitionException(final String id,
                                          final String expectedMessage,
                                          final int expectedLine) {
     try {
       preprocess(id, res(id + ".pde"));
       fail("Expected to fail with \"" + expectedMessage + "\" on line "
           + expectedLine);
     } catch (RecognitionException e) {
       assertEquals(expectedMessage, e.getMessage());
       assertEquals(expectedLine, e.getLine());
     } catch (Exception e) {
       if (!e.equals(e.getCause()) && e.getCause() != null)
         fail(e.getCause().toString());
       else
         fail(e.toString());
     }
   }
 
   static void expectRunnerException(final String id,
                                     final String expectedMessage,
                                     final int expectedLine) {
     try {
       preprocess(id, res(id + ".pde"));
       fail("Expected to fail with \"" + expectedMessage + "\" on line "
           + expectedLine);
     } catch (RunnerException e) {
       assertEquals(expectedMessage, e.getMessage());
       assertEquals(expectedLine, e.getCodeLine());
     } catch (Exception e) {
       if (!e.equals(e.getCause()) && e.getCause() != null)
         fail(e.getCause().toString());
       else
         fail(e.toString());
     }
   }
 
   static void expectCompilerException(final String id,
                                       final String expectedMessage,
                                       final int expectedLine) {
     try {
       final String program = preprocess(id, res(id + ".pde"));
       final ProcessResult compilerResult = COMPILER.compile(id, program);
       if (compilerResult.succeeded()) {
         fail("Expected to fail with \"" + expectedMessage + "\" on line "
             + expectedLine);
       }
       final String e = compilerResult.getStderr().split("\n")[0];
       final Matcher m = Pattern.compile(":(\\d+):\\s+(.+)$").matcher(e);
       m.find();
       assertEquals(expectedMessage, m.group(2));
       assertEquals(String.valueOf(expectedLine), m.group(1));
     } catch (Exception e) {
       if (!e.equals(e.getCause()) && e.getCause() != null)
         fail(e.getCause().toString());
       else
         fail(e.toString());
     }
   }
 
   static void expectGood(final String id) {
     try {
       final String program = preprocess(id, res(id + ".pde"));
 
       final ProcessResult compilerResult = COMPILER.compile(id, program);
       if (!compilerResult.succeeded()) {
         System.err.println(program);
         System.err.println("----------------------------");
         System.err.println(compilerResult.getStderr());
         fail("Compilation failed with status " + compilerResult.getResult());
       }
 
       final File expectedFile = res(id + ".expected");
       if (expectedFile.exists()) {
         final String expected = read(expectedFile);
         assertEquals(expected, program);
       } else {
         System.err.println("WARN: " + id
             + " does not have an expected output file. Generating.");
         final FileWriter sug = new FileWriter(res(id + ".expected"));
         sug.write(program);
         sug.close();
       }
 
     } catch (Exception e) {
       if (!e.equals(e.getCause()) && e.getCause() != null)
         fail(e.getCause().toString());
       else
         fail(e.toString());
     }
   }
 
   @Test
   public void bug4() {
     expectGood("bug4");
   }
 
   @Test
   public void bug5a() {
     expectGood("bug5a");
   }
 
   @Test
   public void bug5b() {
     expectGood("bug5b");
   }
 
   @Test
   public void bug6() {
     expectRecognitionException("bug6", "expecting EOF, found '/'", 1);
   }
 
   @Test
   public void bug16() {
     expectRunnerException("bug16", "Unclosed /* comment */", 2);
   }
 
   @Test
   public void bug136() {
     expectGood("bug136");
   }
 
   @Test
   public void bug196() {
     expectRecognitionException("bug196",
       "Web colors must be exactly 6 hex digits. This looks like 5.", 4);
   }
 
   @Test
   public void bug281() {
     expectGood("bug281");
   }
 
   @Test
   public void bug481() {
     expectGood("bug481");
   }
 
   @Test
   public void bug507() {
     expectRecognitionException("bug507", "expecting EOF, found 'else'", 5);
   }
 
   @Test
   public void bug598() {
     expectGood("bug598");
   }
 
   @Test
   public void bug631() {
     expectGood("bug631");
   }
 
   @Test
   public void bug763() {
     expectRunnerException("bug763", "Unterminated string constant", 6);
   }
 
   @Test
   public void bug820() {
     expectCompilerException("bug820", "x1 is already defined in setup()", 21);
   }
 
   @Test
   public void bug1064() {
     expectGood("bug1064");
   }
 
   @Test
   public void bug1145() {
     expectCompilerException("bug1145", "'.' expected", 4);
   }
 
   @Test
   public void bug1362() {
     expectGood("bug1362");
   }
 
   @Test
   public void bug1442() {
     expectGood("bug1442");
   }
 
   @Test
   public void bug1511() {
     expectGood("bug1511");
   }
 
   @Test
  public void bug1512() {
    expectGood("bug1512");
  }

  @Test
   public void bug1514a() {
     expectGood("bug1514a");
   }
 
   @Test
   public void bug1514b() {
     expectGood("bug1514b");
   }
 
   @Test
  public void bug1515() {
    expectGood("bug1515");
  }

  @Test
   public void bug1516() {
     expectGood("bug1516");
   }
 
   @Test
   public void bug1517() {
     expectGood("bug1517");
   }
 
   @Test
   public void bug1518a() {
     expectGood("bug1518a");
   }
 
   @Test
   public void bug1518b() {
     expectGood("bug1518b");
   }
 
   @Test
   public void bug1519() {
     expectRecognitionException("bug1519", "Maybe too many > characters?", 7);
   }
 
 }
