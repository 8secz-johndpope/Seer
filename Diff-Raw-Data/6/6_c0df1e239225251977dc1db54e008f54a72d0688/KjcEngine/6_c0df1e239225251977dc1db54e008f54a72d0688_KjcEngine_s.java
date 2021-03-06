 import java.awt.*; // for window, temporary
 import java.awt.event.*; // also for window
 import java.io.*;
 
 import java.net.*; // the start of a bad joke
 
 import com.oroinc.text.regex.*;
 
 
 // always compile to lib directory
 // always make .java in current directory
 // can copy over (and delete) later for export
 // fix line number offset so that public class can be on a new line
 // re-enable system err catching (after lib fixes)
 
 // doesn't really need to extend kjc.Main anymore, 
 // since reportTrouble doesn't actually do any good
 
 
 public class KjcEngine extends PdeEngine {
   static String TEMP_CLASS = "Temporary";
   static final String EXTENDS = "extends BApplet ";
   static final String EXTENDS_KJC = "extends KjcApplet ";
 
   static final int COMPILING = 1;
   static final int RUNNING = 2;
   int messageMode;
 
   static final String applet_imports[] = {
     "java.applet", "java.awt", "java.awt.image", "java.awt.event",
     "java.io", "java.net", "java.text", "java.util", "java.util.zip"
   };
 
   static final String application_imports[] = {
     "java.applet", "java.awt", "java.awt.image", "java.awt.event",
     "java.io", "java.net", "java.text", "java.util", "java.util.zip",
     "javax.comm",
 
     // if jdk14 defined, jdk13 will be as well
 #ifdef JDK13
     "javax.sound.midi", "javax.sound.midi.spi",
     "javax.sound.sampled", "javax.sound.sampled.spi",
 #endif
 
 #ifdef JDK14
     "javax.xml.parsers", "javax.xml.transform", 
     "javax.xml.transform.dom", "javax.xml.transform.sax",
     "javax.xml.transform.stream", "org.xml.sax",
     "org.xml.sax.ext", "org.xml.sax.helpers"
 #endif
   };
 
   String tempClass;
   String tempFilename;
   String tempClassFilename;
   PdeException exception;
 
   PrintStream leechErr;
   KjcMessageStream messageStream;
 
   String program;
   //PdeEditor editor;
   String buildPath;
 
   boolean running;
   KjcApplet applet;
   //Window window;
 
   Process process;
   static int portnum = 8192;
   ServerSocket umbilical;
   boolean usingExternal;
 
 
   public KjcEngine(PdeEditor editor, 
 		   String program, String buildPath, 
 		   String dataPath) {
     super(editor);
 
     this.program = program;
     this.buildPath = buildPath;
     //this.buildPath = "lib" + File.separator + "build";
     //this.editor = editor;
     
     // only run cleanup if using the applications temp dir
     //if (buildPath.endsWith("build")) cleanup();
 
     File buildDir = new File(buildPath);
     if (dataPath != null) {
       File dataDir = new File(dataPath);
       if (dataDir.exists()) {
 	PdeEditor.copyDir(dataDir, buildDir);
 	/*
 	String files[] = dataDir.list();
 	for (int i = 0; i < files.length; i++) {
 	  File sourceFile = new File(dataDir, files[i]);
 	  if (sourceFile.isDirectory()) continue;  // may fix in future
 	  File targetFile = new File(buildDir, files[i]);
 	  //System.err.println("source is " + sourceFile + 
 	  //	     "  target is " + targetFile);
 	  PdeEditor.copyFile(sourceFile, targetFile);
 	}
 	*/
       }
     }
 
     usingExternal = PdeBase.getBoolean("play.external", false);
 
     /*
     // what are the chances of this working on the mac?
     // one in ninety nine? one in a 667 megahertz?
     File dir = new File(".");
     String list[] = dir.list();
     for (int i = 0; i < list.length; i++) {
       if (list[i].indexOf(TEMP_CLASS) == 0) {
 	//System.out.println("culprit1: " + list[i]);
 	File deadMan = new File(dir, list[i]);
 	deadMan.delete();
       }
     }
     dir = new File("lib");
     list = dir.list();
     for (int i = 0; i < list.length; i++) {
       if (list[i].indexOf(TEMP_CLASS) == 0) {
 	//System.out.println("culprit2: " + list[i]);
 	File deadMan = new File(dir, list[i]);
 	deadMan.delete();
       }
     }
     */
     //dir = new File(buildPath);
   }
 
 
   static final int BEGINNER     = 0;
   static final int INTERMEDIATE = 1;
   static final int ADVANCED     = 2;
 
   // writes .java file into buildPath
   public String writeJava(String name, boolean kjc) {
     //System.out.println("writing java");
     try {
       int programType = BEGINNER;
 
       // remove (encode) comments temporarily
       program = commentsCodec(program /*, true*/);
 
       // insert 'f' for all floats
       // shouldn't substitute f's for: "Univers76.vlw.gz";
       if (PdeBase.getBoolean("compiler.substitute_f", true)) {
 	/*
 	  a = 0.2 * 3
 	  (3.)
 	  (.3 * 6)
 	  (.30*7)
           float f = 0.3; 
           fill(0.3, 0.2, 0.1);
 
 	  next to white space \s or math ops +-/*() 
           or , on either side, 
           followed by ; (might as well on either side)
    
 	  // allow 3. to work (also allows x.x too)
 	  program = substipoot(program, "(\\d+\\.\\d*)(\\D)", "$1f$2");
 	  program = substipoot(program, "(\\d+\\.\\d*)ff", "$1f");
 
 	  // allow .3 to work (also allows x.x)
 	  program = substipoot(program, "(\\d*\\.\\d+)(\\D)", "$1f$2");
 	  program = substipoot(program, "(\\d*\\.\\d+)ff", "$1f");
 	*/
 
 	program = substipoot(program, "([\\s\\,\\;\\+\\-\\/\\*\\(\\)])(\\d+\\.\\d*)([\\s\\,\\;\\+\\-\\/\\*\\(\\)])", "$1$2f$3");
 	program = substipoot(program, "([\\s\\,\\;\\+\\-\\/\\*\\(\\)])(\\d*\\.\\d+)([\\s\\,\\;\\+\\-\\/\\*\\(\\)])", "$1$2f$3");
       }
 
       // allow int(3.75) instead of just (int)3.75
       if (PdeBase.getBoolean("compiler.enhanced_casting", true)) {
 	program = substipoot(program, "([^A-Za-z0-9_])byte\\((.*)\\)", "$1(byte)($2)");
 	program = substipoot(program, "([^A-Za-z0-9_])char\\((.*)\\)", "$1(char)($2)");
 	program = substipoot(program, "([^A-Za-z0-9_])int\\((.*)\\)", "$1(int)($2)");
 	program = substipoot(program, "([^A-Za-z0-9_])float\\((.*)\\)", "$1(float)($2)");
       }
 
       if (PdeBase.getBoolean("compiler.color_datatype", true)) {
 	// so that regexp works correctly in this strange edge case
 	if (program.indexOf("color") == 0) program = " " + program;
 	// swap 'color' with 'int' when used as a datatype
 	program = substipoot(program, 
			     "([;\\s])color([\\s])", "$1int$2");
 			     //"([^A-Za-z0-9_.])color([^A-Za-z0-9_\\(.])", "$1int$2");
 	// color(something) like int() and the rest is no good
 	// because there is already a function called 'color' in BGraphics
 	//program = substipoot(program, "([^A-Za-z0-9_])color\\((.*)\\)", "$1(int)($2)");
       }
 
       if (PdeBase.getBoolean("compiler.inline_web_colors", true)) {
 	// convert "= #cc9988" into "= 0xffcc9988"
 	//program = substipoot(program, "(=\\s*)\\#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])", "$1 0xff$2$3$4");
 	program = substipoot(program, "#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])", "0xff$1$2$3");
       }
 
       if ((program.indexOf("void setup()") != -1) ||
 	  (program.indexOf("void loop()") != -1) ||
 	  (program.indexOf("void draw()") != -1)) {
 	programType = INTERMEDIATE;
       }
 
       int index = program.indexOf("public class");
       if (index != -1) {
 	programType = ADVANCED;
 	// kjc will get pissed off if i call the .java file
 	// something besides the name of the class.. so here goes
 	String s = program.substring(index + "public class".length()).trim();
 	index = s.indexOf(' ');
 	name = s.substring(0, index);
 	tempClass = name;
 
 	// and we're running inside 
 	if (kjc) {  // if running inside processing...
 	  index = program.indexOf(EXTENDS); // ...and extends BApplet
 	  if (index != -1) {  // just extends object
 	    String left = program.substring(0, index);
 	    String right = program.substring(index + EXTENDS.length());
 	    // replace with 'extends KjcApplet'
 	    program = left + ((usingExternal) ? EXTENDS : EXTENDS_KJC) + right;
 	  }
 	}
       }
       tempFilename = name + ".java";
       tempClassFilename = name + ".class";
 
       PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(buildPath + File.separator + tempFilename))));
 
       //String eol = System.getProperties().getProperty("line.separator");
 
       if (programType < ADVANCED) {
 	// spew out a bunch of java imports 
 	if (kjc) {  // if running in environment, or exporting an app
 	  for (int i = 0; i < application_imports.length; i++) {
 	    writer.print("import " + application_imports[i] + ".*; ");
 	  }
 
 	} else {  // exporting an applet
 	  for (int i = 0; i < applet_imports.length; i++) {
 	    writer.println("import " + applet_imports[i] + ".*; ");
 	    //writer.print("import " + applet_imports[i] + ".*; ");
 	    //if (!kjc) writer.println();
 	  }
 	}
 
 	// add serial if running inside pde
 	//if (kjc) writer.print("import javax.comm.*;");
 	if (!kjc) writer.println();
 
 	writer.print("public class " + name + " extends " +
 		     ((kjc && !usingExternal) ? 
 		      "KjcApplet" : "BApplet") + " {");
       }
       if (programType == BEGINNER) {
 	if (!kjc) writer.println();
 
 	// hack so that the regexp below works
 	//if (program.indexOf("size(") == 0) program = " " + program;
 	if ((program.indexOf("size(") == 0) || 
 	    (program.indexOf("background(") == 0)) {
 	  program = " " + program;
 	}
 
 
 	PatternMatcher matcher = null;
 	PatternCompiler compiler = null;
 	Pattern pattern = null;
 	Perl5Substitution subst = null;
 	PatternMatcherInput input = null;
 
 
 	///////// grab (first) reference to size()
 
 
 	matcher = new Perl5Matcher();
 	compiler = new Perl5Compiler();
 	try {
 	  pattern = 
 	    compiler.compile("[\\s\\;](size\\(\\s*\\d+,\\s*\\d+\\s*\\);)");
 	    //compiler.compile("^([^A-Za-z0-9_]+)(size\\(\\s*\\d+,\\s*\\d+\\s*\\);)");
 
 	} catch (MalformedPatternException e){
 	  e.printStackTrace();
 	  //System.err.println("Bad pattern.");
 	  //System.err.println(e.getMessage());
 	  System.exit(1);
 	}
 
 	String sizeInfo = null;
 	input = new PatternMatcherInput(program);
 	if (matcher.contains(input, pattern)) {
 	  MatchResult result = matcher.getMatch();
 	  //int wide = Integer.parseInt(result.group(1).toString());
 	  //int high = Integer.parseInt(result.group(2).toString());
 	  //sizeInfo = "void setup() { " + result.group(0) + " } ";
 	  sizeInfo = result.group(0);
 
 	} else {
 	  // no size() defined, make it default
 	  sizeInfo = "size(" + BApplet.DEFAULT_WIDTH + ", " + 
 	    BApplet.DEFAULT_HEIGHT + "); ";
 	}
 
 
 	// remove references to size()
 	// this winds up removing every reference to size()
 	// not really intended, but will help things work
 
 	subst = new Perl5Substitution("", Perl5Substitution.INTERPOLATE_ALL);
 	//subst = new Perl5Substitution("$1", Perl5Substitution.INTERPOLATE_ALL);
 	program = Util.substitute(matcher, pattern, subst, program, 
 				  Util.SUBSTITUTE_ALL);
 
 
 	/////////// grab (first) reference to background()
 
 	matcher = new Perl5Matcher();
 	compiler = new Perl5Compiler();
 	try {
 	  pattern = 
 	    compiler.compile("[\\s\\;](background\\(.*\\);)");
 	  //[\\s\\;]
 	    //compiler.compile("([^A-Za-z0-9_]+)(background\\(.*\\);)");
 
 	} catch (MalformedPatternException e){
 	  //System.err.println("Bad pattern.");
 	  //System.err.println(e.getMessage());
 	  e.printStackTrace();
 	  System.exit(1);
 	}
 
 	String backgroundInfo = "";
 	input = new PatternMatcherInput(program);
 	if (matcher.contains(input, pattern)) {
 	  MatchResult result = matcher.getMatch();
 	  //int wide = Integer.parseInt(result.group(1).toString());
 	  //int high = Integer.parseInt(result.group(2).toString());
 	  //sizeInfo = "void setup() { " + result.group(0) + " } ";
 	  backgroundInfo = result.group(0);
 
 	  //} else {
 	  // no size() defined, make it default
 	  //sizeInfo = "size(" + BApplet.DEFAULT_WIDTH + ", " + 
 	  //BApplet.DEFAULT_HEIGHT + "); ";
 	}
 
 	// remove references to size()
 	// this winds up removing every reference to size()
 	// not really intended, but will help things work
 	subst = new Perl5Substitution("", Perl5Substitution.INTERPOLATE_ALL);
 	//subst = new Perl5Substitution("$1", Perl5Substitution.INTERPOLATE_ALL);
 	program = Util.substitute(matcher, pattern, subst, program, 
 				  Util.SUBSTITUTE_ALL);
 
 
 	//////// spew out the size and background info
 
 	writer.print("void setup() { ");
 	writer.print(sizeInfo);
 	writer.print(backgroundInfo);
 	writer.print("} ");
 	writer.print("void draw() {");
       }
 
       // decode comments to bring them back
       program = commentsCodec(program /*, false*/);
 
       // spew the actual program
       // this should really add extra indents, 
       // especially when not in kjc mode (!kjc == export)
 
       // things will be one line off if there's an error in the code
       if (!kjc) writer.println();
 
       writer.println(program);
       //System.out.println(program);
 
       if (programType == BEGINNER) {
 	writer.println("}");
       }
       if (programType < ADVANCED) {
 	writer.print("}");
       }
 
       writer.flush();
       writer.close();
 
     } catch (IOException e) {
       e.printStackTrace();
     }
     return name;
   }
 
   //static char encodeTable[] = new char[127];
   //static char decodeTable[] = new char[127];
   static char rotateTable[] = new char[128];
   static {
     for (int i = 0; i < 80; i++) {
       rotateTable[i+48] = (char) (48 + ((i + 40) % 80));
     }
 
     /*
     int rot = (123 - 65) / 2;
     for (int i = 65; i < 123; i++) {
       rotateTable[i] = (char) (((i - 65 + rot) % (rot*2)) + 65); // : (char)i;
     }
     */
 
     //for (int i = 33; i < 127; i++) {
       //rotateTable[i] = //Character.isAlpha((char)i) ?
       //(char) (((i - 33 + rot) % 94) + 33) : (char)i;
       
       //encodeTable[i] = (char) (i+1);
       //decodeTable[i] = (char) (i-1);
       //encodeTable[i] = (char) (((i - 33 + rot) % 94) + 33);
       //decodeTable[i] = encodeTable[i];
       //encodeTable[i] = (char) (((i - 33 + rot) % 94) + 33);
       //decodeTable[i] = (char) (((i + 33 + rot) % 94) + 33);
       //System.out.println((int) decodeTable[i]);
     //}
   }
 
   protected String commentsCodec(String program /*, boolean encode*/) {
     // need to preprocess class to remove comments
     // so tthat they don't fool this crappy parsing below
     char p[] = program.toCharArray();
     char lastp = 0;
     boolean insideComment = false;
     boolean eolComment = false;
     boolean slash = false;
     boolean insideQuote = false;
     for (int i = 0; i < p.length; i++) {
       if (insideComment) {
 	if (eolComment &&
 	    ((p[i] == '\r') || (p[i] == '\n'))) {
 	  insideComment = false;
 	  slash = false;
 
 	} else if (!eolComment &&
 		   (p[i] == '*') &&
 		   (i != (p.length-1)) &&
 		   (p[i+1] == '/')) {
 	  insideComment = false;
 	  slash = false;
 
 	} else {
 	  //if ((p[i] > 32) && (p[i] < 127)) {
 	  if ((p[i] >= 48) && (p[i] < 128)) {
 	    p[i] = rotateTable[p[i]];
 	    //p[i] = encode ? encodeTable[p[i]] : decodeTable[p[i]];
 	  }
 	  //p[i] = ' ';
 	}
       } else {  // not yet inside a comment
 	if (insideQuote) {
 	  if ((p[i] == '\"') && (lastp != '\\')) {
 	    insideQuote = !insideQuote;
 	  } else {
 	    if ((p[i] >= 48) && (p[i] < 128)) {
 	      p[i] = rotateTable[p[i]];
 	      //p[i] = encode ? encodeTable[p[i]] : decodeTable[p[i]];
 	    }
 	  }
 
 	} else {  // not inside a quote
 	  if (p[i] == '/') {
 	    if (slash) {
 	      insideComment = true;
 	      eolComment = true;
 	    } else {
 	      slash = true;
 	    }
 	  } else if (p[i] == '\"') {
 	    if (lastp != '\\') insideQuote = !insideQuote;
 
 	  } else if (p[i] == '*') {
 	    if (slash) {
 	      insideComment = true;
 	      eolComment = false;
 	    }
 	  } else {
 	    slash = false;
 	  }
 	}
       }
       lastp = p[i];
     }
     //System.out.println(new String(p));
     return new String(p);
   }
 
   protected String substipoot(String what, String incoming, String outgoing) {
     PatternMatcher matcher = new Perl5Matcher();
     PatternCompiler compiler = new Perl5Compiler();
     Pattern pattern = null;
 
     try {
       pattern = compiler.compile(incoming);
 
     } catch (MalformedPatternException e){
       System.err.println("Bad pattern.");
       System.err.println(e.getMessage());
       System.exit(1);
     }
 
     Perl5Substitution subst = 
       new Perl5Substitution(outgoing, Perl5Substitution.INTERPOLATE_ALL);
     return Util.substitute(matcher, pattern, subst, what, 
 			   Util.SUBSTITUTE_ALL);
   }
 
   //public void finalize() {
   //System.out.println("finalizing KjcEngine");
   //}
 
   boolean newMessage;
   int messageLineCount;
 
   public void message(String s) {
     // as in: lib\build\Temporary_5476_6442.java:88: caution:Assignment of an expression to itself [KOPI]
     if (s.indexOf("caution") != -1) return;
 
     //System.out.println(messageMode + " " + "msg: " + s);
 
     if (messageMode == COMPILING) {
       //System.out.println("leech2: " + new String(b, offset, length));
       //String s = new String(b, offset, length);
       //if (s.indexOf(tempFilename) == 0) {
       String fullTempFilename = buildPath + File.separator + tempFilename;
       if (s.indexOf(fullTempFilename) == 0) {
 	String s1 = s.substring(fullTempFilename.length() + 1);
 	int colon = s1.indexOf(':');
 	int lineNumber = Integer.parseInt(s1.substring(0, colon));
 	//System.out.println("pde / line number: " + lineNumber);
 
 	//String s2 = s1.substring(colon + 2);
 	int err = s1.indexOf("error:");
 	if (err != -1) {
 	  //err += "error:".length();
 	  String description = s1.substring(err + "error:".length());
 	  description = description.trim();
 	  //exception = new PdeException(description, lineNumber-2);
 	  exception = new PdeException(description, lineNumber-1);
 	  editor.error(exception);
 
 	} else {
 	  System.err.println("i suck: " + s);
 	}
 
       } else {
 	//System.err.println("don't understand: " + s);
 	exception = new PdeException("Error while compiling, " + 
 				     "please send code to bugs@proce55ing.net");
 	editor.error(exception);
       }
     } else if (messageMode == RUNNING) {
       //if (s.indexOf("MAKE WAY") != -1) {
       //System.out.println("new message coming");
       //newMessage = true;
 
       //} else {
       if (newMessage) {
 	//System.out.println("making msg of " + s);
 	exception = new PdeException(s);  // type of java ex
 	//System.out.println("setting ex type to " + s);
 	newMessage = false;
 	messageLineCount = 0;
 
       } else {
 	messageLineCount++;
 
 	int index = s.indexOf(tempFilename);
 	//System.out.println("> " + index + " " + s);
 	if (index != -1) {
 	  int len = tempFilename.length();
 	  String lineNumberStr = s.substring(index + len + 1);
 	  index = lineNumberStr.indexOf(')');
 	  lineNumberStr = lineNumberStr.substring(0, index);
 	  //System.err.println("error line is: " + lineNumberStr);
 	  try {
 	    exception.line = Integer.parseInt(lineNumberStr) - 1; //2;
 	    //System.out.println("exception in RUNNING");
 	    editor.error(exception);
 	  } catch (NumberFormatException e) {  
 	    e.printStackTrace();
 	  }
 
 	} else if ((index = s.indexOf(tempClass)) != -1) {
 	  // code to check for:
 	  // at Temporary_484_3845.loop(Compiled Code)
 	  // would also probably get:
 	  // at Temporary_484_3845.loop
 	  // which (i believe) is used by the mac and/or jview
 	  String functionStr = s.substring(index + tempClass.length() + 1);
 	  index = functionStr.indexOf('(');
 	  if (index != -1) {
 	    functionStr = functionStr.substring(0, index);
 	  }
 	  exception = new PdeException(//"inside \"" + functionStr + "()\": " +
 				       exception.getMessage() + 
 				       " inside " + functionStr + "() " +
 				       "[add Compiler.disable() to setup()]");
 	  editor.error(exception);
 	  // this will fall through in tihs example:
 	  // at Temporary_4636_9696.pootie(Compiled Code)
 	  // at Temporary_4636_9696.loop(Temporary_4636_9696.java:24)
 	  // because pootie() (re)sets the exception title
 	  // and throws it, but then the line number gets set 
 	  // because of the line that comes after
 
 	} else if (messageLineCount > 5) {
 	  // this means the class name may not be mentioned 
 	  // in the stack trace.. this is just a general purpose
 	  // error, but needs to make it through anyway.
 	  // so if five lines have gone past, might as well signal
 
 	  //System.out.println("signalling");
 	  messageLineCount = -100;
 	  exception = new PdeException(exception.getMessage());
 	  editor.error(exception);
 	}
 	//System.out.println("got it " + s);
       }
     } else {
       System.out.println("message mode not set");
     }
   }
 
 
   //public void error(Exception e) {
   //newMessage = true;
   //e.printStackTrace(leechErr);
   //}
 
 
   //public void reportTrouble(PositionedError trouble) {
   //System.out.println("trubber: " + trouble);
   //}
 
 
   public boolean compileJava() throws PdeException {
     String args[] = new String[2];
     //args[0] = "-dlib";
     //args[1] = tempFilename;
     args[0] = "-d" + buildPath;
     args[1] = buildPath + File.separator + tempFilename;
     //System.out.println("args = " + args[0] + " " + args[1]);
 
     // this will catch and parse errors during compilation
     // or should this be in the constructor?
     messageStream = new KjcMessageStream(this);
     leechErr = new PrintStream(messageStream);
     //PrintStream systemErr = System.err;
     System.setErr(leechErr); // NEED THIS
 
     messageMode = COMPILING;
 
     //boolean success = compile(args);
     boolean success = at.dms.kjc.Main.compile(args);
 
     // end of compilation error checking
     //System.setErr(systemErr);
     System.setErr(PdeEditorConsole.consoleErr);
     //System.err.println("success = " + success);
 
     return success;
   }
 
 
   // part of PdeEngine
   public void start(Point windowLocation) throws PdeException {
     int numero1 = (int) (Math.random() * 10000);
     int numero2 = (int) (Math.random() * 10000);
     tempClass = TEMP_CLASS + "_" + numero1 + "_" + numero2;
     //System.out.println("KjcEngine.started");
     writeJava(tempClass, true);
     //System.err.println("KjcEngine wrote java");
     //System.out.println("thread active count is " + Thread.activeCount());
 
     /*
     if (!success) {
       if (exception != null) {
 	throw exception;
       } else {
 	throw new PdeException("KjcEngine.start uncaught problem [1]");
       }
     } else if (exception != null) {
       throw new PdeException("KjcEngine.start uncaught problem [2]");
     }
     */
     //System.setErr(PdeEditorConsole.systemErr);
     boolean result = compileJava();
     //System.setErr(PdeEditorConsole.consoleErr);
     if (!result) {
       //System.out.println("couldn't compile");
       return;
     }
 
       //System.err.println("compiling failed.. was error caught? ");
       //System.err.println(exception);
     //if (!success) return;
 
     //System.out.println("done compiling");
 
     //System.err.println("system err is working");
 
     messageMode = RUNNING;
     Frame frame = PdeBase.frame;
     Point parentLoc = frame.getLocation();
     Insets parentInsets = frame.getInsets();
     
     int x1 = parentLoc.x - 20;
     int y1 = parentLoc.y;
 
     try {
       if (PdeBase.getBoolean("play.external", false)) {
 	String cmd = PdeBase.get("play.external.command");
 	// "cmd /c " +   not helpful?
 	process = Runtime.getRuntime().exec(cmd + " " + tempClass + 
 					    " " + tempClass +
 					    " " + x1 + " " + y1);
 	new KjcMessageSiphon(process.getInputStream(), 
 			     process.getErrorStream(), leechErr);
 
       } else {
 	// temporarily disabled
 	//KjcClassLoader loader = new KjcClassLoader(buildPath);
 	//Class c = loader.loadClass(tempClass);
 	Class c = Class.forName(tempClass);
 
 	//try {
 	applet = (KjcApplet) c.newInstance();
 	//} catch (Exception e) {
 	//  e.printStackTrace();
 	//}
 	//System.out.println(c + " " + applet);
 	//((KjcApplet)applet).errStream = leechErr;
 	applet.setEngine(this);
 	// has to be before init
 	applet.serialProperties(PdeBase.properties);
 	applet.init();
 	if (applet.exception != null) {
 	  if (applet.exception instanceof javax.comm.PortInUseException) {
 	    throw new PdeException("Another program is already " +
 				   "using the serial port.");
 	  } else {
 	    throw new PdeException(applet.exception.getMessage());
 	  }
 	}
 	applet.start();
 
 	if (editor.presenting) {
 	  window = new Window(new Frame());
 	  window.addKeyListener(new KeyAdapter() {
 	      public void keyPressed(KeyEvent e) {
 		//System.out.println("window got " + e);
 		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
 		  //editor.doClose();
 		  //new DelayedClose(editor);
 		  stop();
 		  editor.doClose();
 		}
 	      }
 	    });
 
 	} else {
 	  window = new Frame(editor.sketchName); // gonna use ugly windows instead
 	  ((Frame)window).setResizable(false);
 	  if (PdeBase.icon != null) ((Frame)window).setIconImage(PdeBase.icon);
 	  window.pack(); // to get a peer, size set later, need for insets
 
 	  window.addWindowListener(new WindowAdapter() {
 	      public void windowClosing(WindowEvent e) {
 		stop();
 		editor.doClose();
 		//new DelayedClose(editor);
 		//editor.doClose();
 		//editor.doStop();
 	      }
 	    });
 	}
 	if (!(window instanceof Frame)) y1 += parentInsets.top;
 	window.add(applet);
 
 	applet.addKeyListener(new KeyAdapter() {
 	    public void keyPressed(KeyEvent e) {
 	      //System.out.println("applet got " + e);
 	      if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
 		stop();
 		editor.doClose();
 		//new DelayedClose(editor);
 		//editor.doClose();
 	      }
 	    }
 	  });
 
 	// @#$((* java 1.3
 	// removed because didn't seem to be needed anymore
 	// also, was causing offset troubles
 	/*
 	window.addMouseListener(applet);
 	window.addMouseMotionListener(applet);
 	window.addKeyListener(applet);
 	*/
 
 	Dimension screen = 
 	  Toolkit.getDefaultToolkit().getScreenSize();
 
 	//System.out.println(SystemColor.windowBorder.toString());
 
 	//window.setLayout(new BorderLayout());
 	window.setLayout(null);
 	if (editor.presenting) {
 	  window.setBounds((screen.width - applet.width) / 2,
 			   (screen.height - applet.height) / 2,
 			   applet.width, applet.height);
 	  applet.setBounds(0, 0, applet.width, applet.height);
 
 	} else {
 	  Insets insets = window.getInsets();
 	  //System.out.println(insets);
 	  int mw = PdeBase.getInteger("run.window.width.minimum", 120);
 	  int mh = PdeBase.getInteger("run.window.height.minimum", 120);
 	  int ww = Math.max(applet.width, mw) + insets.left + insets.right;
 	  int wh = Math.max(applet.height, mh) + insets.top + insets.bottom;
 	  if (x1 - ww > 10) {  // if it fits to the left of the window
 	    window.setBounds(x1 - ww, y1, ww, wh);
 
 	  } else { // if it fits inside the editor window
 	    x1 = parentLoc.x + PdeEditor.GRID_SIZE * 2;
 	    y1 = parentLoc.y + PdeEditor.GRID_SIZE * 2;
 
 	    if ((x1 + ww > screen.width - PdeEditor.GRID_SIZE) ||
 		(y1 + wh > screen.height - PdeEditor.GRID_SIZE)) {
 	      // otherwise center on screen
 	      x1 = (screen.width - ww) / 2;
 	      y1 = (screen.height - wh) / 2;
 	    }
 	    window.setBounds(x1, y1, ww, wh);
 	  }
 
 	  Color windowBgColor = 
 	    PdeBase.getColor("run.window.bgcolor", SystemColor.control); 
 	  //new Color(102, 102, 102));
 	  window.setBackground(windowBgColor);
 	  //window.setBackground(SystemColor.windowBorder);
 	  //window.setBackground(SystemColor.control);
 
 	  applet.setBounds((ww - applet.width)/2, 
 			   insets.top + ((wh-insets.top-insets.bottom) -
 					 applet.height)/2, ww, wh);
 	}
 
 	applet.setVisible(true);  // no effect
 	if (windowLocation != null) {
 	  window.setLocation(windowLocation);
 	}
 	window.show();
 	applet.requestFocus();  // necessary for key events
       }
       //System.out.println("KJC RUNNING");
       running = true;
 
       //need to parse this code to give a decent error message
       //internal error
       //java.lang.NullPointerException
       //  at ProcessingApplet.colorMode(ProcessingApplet.java:652)
       //  at Temporary_203_1176.setup(Temporary_203_1176.java:3)
 
     } catch (Exception e) {
       // this will pass through to the first part of message
       // this handles errors that happen inside setup()
       newMessage = true;
       e.printStackTrace(leechErr);
       //if (exception != null) throw exception;
     }
   }
 
 
   protected void cleanup() {
     File buildDir = new File(buildPath);
     if (!buildDir.exists()) buildDir.mkdirs();
 
     String list[] = buildDir.list();
     for (int i = 0; i < list.length; i++) {
       if (list[i].equals("..") || list[i].equals(".")) 
 	continue;
       //System.out.println("removing: " + buildDir + " " + list[i]);
       //deadMan.delete();
       new File(buildDir, list[i]).delete();
     }
 
     /*
     try {
       //System.out.println("cleaning up");
       File file = new File("lib/" + tempClassFilename);
       //File file = new File(tempClassDir, tempClassFilename);
       if (file.exists()) file.delete();
     } catch (Exception e) {
       e.printStackTrace();
     }
 
     try {
       File file = new File(tempFilename);
       //File file = new File(tempDir, tempFilename);
       if (file.exists()) file.delete();
     } catch (Exception e) {
       e.printStackTrace();
     }
     */
   }
 
 
   public void stop() {  // part of PdeEngine
     //System.out.println("PdeEngine.stop");
     //System.out.println("KJC NOT RUNNING");
     running = false;
 
     //System.out.println();
     //System.out.println("* stopping");
 
     // in case stop is called during compilation
     if (applet != null) applet.stop();
     //if (window != null) window.hide();
 
     // above avoids NullPointerExceptions 
     // but still threading is too complex, and so
     // some boogers are being left behind
 
     applet = null;
     //window = null;
 
     if (process != null) {  // running externally
       //System.out.println("killing external process");
       //process.destroy();
 
       //System.out.println("cutting umbilical cord");
       try {
 	FileOutputStream fos = new FileOutputStream("die");
 	fos.close();
       } catch (Exception e) {
 	e.printStackTrace();
       }
       //try {
       //umbilical.close();
       //umbilical = null;
       //} catch (IOException e) {
       //e.printStackTrace();w
       //}
     }
 
     cleanup();
 
     //System.gc();
     //System.out.println("* stopped");
 
     //System.out.println("thread count: " + Thread.activeCount());
     //System.out.println();
     //System.out.println();
   }
 
 
   public void close() {  // part of PdeEngine
     //if (window != null) window.hide();
     if (window != null) {
       //System.err.println("disposing window");
       window.dispose();
       window = null;
     }
   }
 
 
   /*
 class DelayedClose {
   public DelayedClose(PdeEditor ed) {
     System.out.println("stopping");
     stop();
     System.out.println("successful");
     ed.doClose();
   }
 }
   */
 
   //public void inform(String message) { 
   //System.out.println("informing: " + message);
   //}
 }
 
 
 class KjcMessageSiphon implements Runnable {
   InputStream stdout, stderr;
   PrintStream leechErr;
   Thread thread;
 
   public KjcMessageSiphon(InputStream stdout, InputStream stderr,
 			  PrintStream leechErr) {
     this.stdout = stdout;
     this.stderr = stderr;
 
     thread = new Thread(this);
     thread.start();
   }
 
   public void run() {
     while (Thread.currentThread() == thread) {
       try {
 	if (stderr.available() > 0) {
 	  while (stderr.available() > 0) {
 	    System.out.print((char)stderr.read());
 	  }
 	  stderr = null; // get out
 	  throw new Exception();
 	}
 
 	while (stdout.available() > 0) {
 	  System.out.print((char)stdout.read());
 	}
 	Thread.sleep(100);
 
       } catch (Exception e) { 
 	if ((stderr != null) && (leechErr != null)) {
 	  System.out.println("KjcMessageSiphon err " + e);
 	}
 	thread.stop();
 	thread = null;
       }
     }
   }
 }
 
 
 class KjcMessageStream extends OutputStream {
   KjcEngine parent;
 
   public KjcMessageStream(KjcEngine parent) {
     this.parent = parent;
   }
 
   public void close() { }
 
   public void flush() { }
 
   public void write(byte b[]) { 
     System.out.println("leech1: " + new String(b));
   }
 
   public void write(byte b[], int offset, int length) {
     //System.out.println("leech2: " + new String(b));
     parent.message(new String(b, offset, length));
   }
 
   public void write(int b) {
     System.out.println("leech3: '" + ((char)b) + "'");
   }
 }
 
 
 /*
 class KjcClassLoader extends ClassLoader {
   String basePath;
 
   public KjcClassLoader(String basePath) {
     this.basePath = basePath;
   }
 
   public synchronized Class loadClass(String className, boolean resolveIt) 
     throws ClassNotFoundException {
 
     Class result;
     byte classData[];
 
     // Check the loaded class cache
     result = findLoadedClass(className);
     if (result != null) {
       // Return a cached class
       return result;
     }
 
     // Check with the primordial class loader
     try {
       result = super.findSystemClass(className);
       // Return a system class
       return result;
     } catch (ClassNotFoundException e) { }
 
     // Don't attempt to load a system file except through
     // the primordial class loader
     if (className.startsWith("java.")) {
       throw new ClassNotFoundException();
     }
 
     // Try to load it from the basePath directory.
     classData = getTypeFromBasePath(className);
     if (classData == null) {
       System.out.println("KjcClassLoader - Can't load class: " + className);
       throw new ClassNotFoundException();
     }
 
     // Parse it
     result = defineClass(className, classData, 0, classData.length);
     if (result == null) {
       System.out.println("KjcClassLoader - Class format error: " +
 			 className);
       throw new ClassFormatError();
     }
 
     if (resolveIt) {
       resolveClass(result);
     }
 
     // Return class from basePath directory
     return result;
   }
 
   private byte[] getTypeFromBasePath(String typeName) {
     FileInputStream fis;
     String fileName = basePath + File.separatorChar +
       typeName.replace('.', File.separatorChar) + ".class";
 
     try {
       fis = new FileInputStream(fileName);
     } catch (FileNotFoundException e) {
       return null;
     }
 
     BufferedInputStream bis = new BufferedInputStream(fis);
     ByteArrayOutputStream out = new ByteArrayOutputStream();
 
     try {
       int c = bis.read();
       while (c != -1) {
 	out.write(c);
 	c = bis.read();
       }
     } catch (IOException e) {
       return null;
     }
     return out.toByteArray();
   }
 }
 */
 
 
 /*
   class DelayedClose implements Runnable {
     PdeEditor ed;
     Thread thread;
 
     public DelayedClose(PdeEditor ed) {
       this.ed = ed;
       thread = new Thread(this);
       thread.start();
     }
 
     public void run() {
       boolean finished = false;
 
       while (!finished) {
 	ed.doStop();
 	System.out.println("delayed close stage 1");
 	if (Thread.currentThread() == thread) {
 	  System.out.println("delayed close stage 2");
 	  try {
 	    Thread.sleep(2000);
 	  } catch (InterruptedException e) { }
 	  System.out.println("closing now");
 	  ed.doClose();
 	  finished = true;
 	}
       }
     }
   }
 */
 
 
 /*
 class WindowDragger implements MouseListener, MouseMotionListener {
   Window window;
   int minY;
   boolean inside;
 
   int lastX, lastY;
 
   // if drag starts outside window, this will break.. easy to fix
 
   public WindowDragger(Window window, int minY) {
     this.window = window;
     this.minY = minY;
     
     window.addMouseListener(this);
     window.addMouseMotionListener(this);
   }
 
   public void mouseClicked(MouseEvent e) { }
 
   public void mousePressed(MouseEvent e) {
     lastX = e.getX();
     lastY = e.getY();
     inside = (lastY > minY);
     System.out.println("inside = " + inside);
   }
 
   public void mouseReleased(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
 
   public void mouseDragged(MouseEvent e) {
     if (!inside) return;
 
     System.out.println("inside, dragging");
     Point location = window.getLocation();
     int newX = e.getX();
     int newY = e.getY();
     window.setLocation(location.x + (newX-lastX), 
 		       location.y + (newY-lastY));
     lastX = newX;
     lastY = newY;
   }
 
   public void mouseMoved(MouseEvent e) { }
 }
 */
