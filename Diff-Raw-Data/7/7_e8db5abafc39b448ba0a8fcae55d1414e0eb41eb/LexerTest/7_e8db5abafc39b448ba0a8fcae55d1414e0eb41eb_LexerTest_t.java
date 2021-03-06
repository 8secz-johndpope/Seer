 package net.sf.eclipsefp.haskell.core.jparser.test;
 
 import java.io.StringReader;
 
 import de.leiffrenzel.fp.haskell.ui.util.preferences.IHaskellPreferenceProvider;
 
 import antlr.RecognitionException;
 import antlr.Token;
 import antlr.TokenStreamException;
 
 import net.sf.eclipsefp.haskell.core.jparser.HaskellLexer;
 import net.sf.eclipsefp.haskell.core.jparser.HaskellLexerTokenTypes;
 
 public class LexerTest extends TokenStreamTestCase implements HaskellLexerTokenTypes {
 	
 	private TestTokenStream fLexer;
 
 	protected void setUp() {
 		final String inStr = "module Simple where\n" +
 				             "data Underlined_stack = Empty\n";
 		
 		fLexer = new TestTokenStream(new HaskellLexer(new StringReader(inStr)));
 	}
 
 	public void testRecognition() throws TokenStreamException {
 		Token t = fLexer.nextToken();
 		
 		assertEquals(MODULE, t.getType());
 		assertEquals("module", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(CONSTRUCTOR_ID, t.getType());
 		assertEquals("Simple", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(WHERE, t.getType());
 		assertEquals("where", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(NEWLINE, t.getType());
 		assertEquals("\n", t.getText());
 
 		t = fLexer.nextToken();
 		assertEquals(DATA, t.getType());
 		assertEquals("data", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(CONSTRUCTOR_ID, t.getType());
 		assertEquals("Underlined_stack", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(EQUALS, t.getType());
 		assertEquals("=", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(CONSTRUCTOR_ID, t.getType());
 		assertEquals("Empty", t.getText());
 
 		t = fLexer.nextToken();
 		assertEquals(NEWLINE, t.getType());
 		assertEquals("\n", t.getText());
 
 		t = fLexer.nextToken();
 		assertEquals(EOF, t.getType());
 	}
 	
 	public void testRecognizeLet() throws TokenStreamException {
 		fLexer = createLexer("let in");
 		assertEquals(LET, fLexer.nextToken().getType());
 	}
 	
 	public void testCommonPrefixes() throws TokenStreamException {
 		fLexer = createLexer("main whomp modula whery .");
 		
 		assertToken(VARIABLE_ID, "main", fLexer.nextToken());
 		assertToken(VARIABLE_ID, "whomp", fLexer.nextToken());
 		assertToken(VARIABLE_ID, "modula", fLexer.nextToken());
 		assertToken(VARIABLE_ID, "whery", fLexer.nextToken());
 		assertToken(VARSYM, ".", fLexer.nextToken());
 	}
 	
 	public void testMaximalMunchRule() throws TokenStreamException  {
 		fLexer = createLexer("render doc = renderStyle style doc");
 		
 		fLexer.skipTokens(1);
 		assertToken(VARIABLE_ID, "doc", fLexer.nextToken());
 	}
 	
 	public void testKeyworkPreffixInsideIdentifier() throws TokenStreamException {
 		fLexer = createLexer("Pwho imodule");
 		
 		Token t = fLexer.nextToken();
 		assertEquals(CONSTRUCTOR_ID, t.getType());
 		assertEquals("Pwho", t.getText());
 		
 		t = fLexer.nextToken();
 		assertEquals(VARIABLE_ID, t.getType());
 		assertEquals("imodule", t.getText());
 	}
 	
 	public void testPosition() throws TokenStreamException {
 		Token t = fLexer.nextToken(); //module
 		assertEquals(0, t.getColumn());
 		assertEquals(0, t.getLine());
 		
 		t = fLexer.nextToken(); // Simple
 		assertEquals(7, t.getColumn());
 		assertEquals(0, t.getLine());
 		
 		t = fLexer.nextToken(); // where
 		assertEquals(0, t.getLine());
 	
 		t = fLexer.nextToken(); // \n
 
 		t = fLexer.nextToken();
 		assertEquals(0, t.getColumn());
 		assertEquals(1, t.getLine());
 
 		t = fLexer.nextToken();
 		assertEquals(5, t.getColumn());
 		assertEquals(1, t.getLine());
 	}
 	
 	public void testCodeWithComments() throws TokenStreamException {
 		fLexer = createLexer("--this is the main module for the app\n" +
 				             "module Main where\n" +
 				             "{- We actually need to import those\n" +
 				             "   modules here for using the network\n" +
 				             "   connection capabilities -}\n" +
 				             "import Network\n" +
 				             "\n" +
 				             "main = {- block comment inside -} putStr 'hello'\n"
 				             );
 		Token t = fLexer.nextToken();
 		assertEquals(COMMENT, t.getType());
 		assertEquals("--this is the main module for the app", t.getText());
 		
 		fLexer.skipTokens(1); // \n
 		
 		t = fLexer.nextToken(); //module
 		assertEquals(0, t.getColumn());
 		assertEquals(1, t.getLine());
 		
 		fLexer.skipTokens(3); //Main where \n
 		
 		t = fLexer.nextToken();
 		assertEquals(COMMENT, t.getType());
 		assertEquals("{- We actually need to import those\n" +
 	                 "   modules here for using the network\n" +
 	                 "   connection capabilities -}",
 	                 t.getText());
 		
 		fLexer.skipTokens(1); // \n
 
 		t = fLexer.nextToken(); //import
 		assertEquals(5, t.getLine());
 		
 		fLexer.skipTokens(5); //Network \n \n main = 
 		
 		t = fLexer.nextToken(); // {- block comment inside -}
 		assertEquals(COMMENT, t.getType());
 		assertEquals("{- block comment inside -}", t.getText());
 		
 		t = fLexer.nextToken(); //putStr
 		assertEquals("putStr", t.getText());
 		assertEquals(34, t.getColumn());
 	}
 	
 	public void testNewlineAfterComment() throws RecognitionException, TokenStreamException {
 		//inspired on darcs' source code
 		final String input = "fat 0 = 1 -- base case\n" +
 			                 "fat n = n * (fat (n - 1))";
 		
 		fLexer = createLexer(input);
 		//fat 0 = 1
 		fLexer.skipTokens(4);
 		
 		Token commentToken = fLexer.nextToken();
 		assertEquals(COMMENT, commentToken.getType());
 		assertEquals("-- base case", commentToken.getText());
 		
 		assertEquals(NEWLINE, fLexer.nextToken().getType());
 	}
 	
 	public void testSimpleStringLiteral() throws TokenStreamException {
 		final String input = "main = putStr \"Hello, world!\"";
 		fLexer = createLexer(input);
 		
 		// main = putStr
 		fLexer.skipTokens(3);
 		
 		Token helloWorldTk = fLexer.nextToken();
 		assertEquals(STRING_LITERAL, helloWorldTk.getType());
 		assertEquals("Hello, world!", helloWorldTk.getText());
 	}
 	
 	public void testMultilineString() throws TokenStreamException {
 		final String input = "main = putStr \"Hello, \\\n" +
 				             "                  \\world!\"";
 		fLexer = createLexer(input);
 		
 		// main = putStr
 		fLexer.skipTokens(3);
 		
 		Token helloWorldTk = fLexer.nextToken();
 		assertEquals(STRING_LITERAL, helloWorldTk.getType());
 		assertEquals("Hello, world!", helloWorldTk.getText());
 	}
 	
 	private TestTokenStream createLexer(String input) {
 		return createLexer(input, new HaskellPreferenceProviderStub());
 	}
 	
 	private TestTokenStream createLexer(String input, IHaskellPreferenceProvider prefs) {
 		return new TestTokenStream(
 				new HaskellLexer(new StringReader(input), prefs));
 	}
 	
 	//TODO maybe we need to recognize more comment formats. the report
 	//specifies a return char as the end of a line comment too
 
 	public void testStringWithEscapeChar() throws TokenStreamException {
 		final String input = "main = putStr \"Hello, world!\\n\" " +
 				             "\"tab\\t\" \"slash\\\\\" \"double quote\\\"\" " +
 				             "\"quote\\'\" \"backspace\\b\" \"alert\\a\" " +
 				             "\"formfeed\\f\" \"return\\r\" \"vertical tab\\v\" " +
 				             "\"null \\&char\"";
 		fLexer = createLexer(input);
 		
 		// main = putStr
 		fLexer.skipTokens(3);
 		
 		assertToken(STRING_LITERAL, "Hello, world!\n", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "tab\t", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "slash\\", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "double quote\"", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "quote'", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "backspace\b", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "alert", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "formfeed\f", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "return\r", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "vertical tab", fLexer.nextToken());
 		assertToken(STRING_LITERAL, "null char", fLexer.nextToken());
 	}
 
 	public void testCharacterLiteral() throws TokenStreamException {
 		final String input = "'a' 'b' 'Z' '\\n'";
 		fLexer = createLexer(input);
 		
 		assertToken(CHARACTER_LITERAL, "a", fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "b", fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "Z", fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "\n", fLexer.nextToken());
 	}
 	
 	public void testDoNotAcceptNullCharacter() {
 		final String input = "'\\&'";
 		fLexer = createLexer(input);
 		
 		try {
 			fLexer.nextToken();
 			fail("lexer accepted null character");
 		} catch(TokenStreamException e) {
 			// exception is expected
 		}
 	}
 	
 	public void testEscapeAscii() throws TokenStreamException {
 		final String input = "'\\NUL'";
 		fLexer = createLexer(input);
 		
 		assertToken(CHARACTER_LITERAL, "\u0000", fLexer.nextToken());
 	}
 	
 	public void testEspaceDecimal() throws TokenStreamException {
 		final String input = "'\\31' '\\139'";
 		fLexer = createLexer(input);
 		
 		assertToken(CHARACTER_LITERAL, "" + ((char) 31), fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "" + ((char) 139), fLexer.nextToken());
 	}
 	
 	public void testEscapeHexadecimal() throws TokenStreamException {
 		fLexer = createLexer("'\\x80' '\\x0F'");
 		
 		assertToken(CHARACTER_LITERAL, "" + ((char) 0x80), fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "" + ((char) 0x0F), fLexer.nextToken());
 	}
 	
 	public void testEscapeOctal() throws TokenStreamException {
 		fLexer = createLexer("'\\o40' '\\o100'");
 
 		assertToken(CHARACTER_LITERAL, "" + ((char) 040), fLexer.nextToken());
 		assertToken(CHARACTER_LITERAL, "" + ((char) 0100), fLexer.nextToken());
 	}
 	
 	public void testIgnorePreprocessor() throws TokenStreamException {
 		final String input = "#ifdef HAVE_CURL\n" +
 				             "import Foreign.C.String ( withCString, CString )\n" +
 				             "#endif";
 		
 		fLexer = createLexer(input);
 		final Token impToken = fLexer.nextToken();
 		assertTokenType(IMPORT, impToken);
 		assertEquals(1, impToken.getLine());
 	}
 	
 	public void testRecognizeContextArrow() throws TokenStreamException {
 		final String input = "Eq t => Algebraic";
 		fLexer = createLexer(input);
 		
 		fLexer.skipTokens(2);
 		assertToken(CONTEXT_ARROW, "=>", fLexer.nextToken());
 	}
 
 	public void testRecognizeVarsyms() throws TokenStreamException {
 		final String input = "<> =>$ $$";
 		fLexer = createLexer(input);
 		
 		assertToken(VARSYM, "<>", fLexer.nextToken());
 		assertToken(VARSYM, "=>$", fLexer.nextToken());
 		assertToken(VARSYM, "$$", fLexer.nextToken());
 	}
 	
 	public void testRecognizeConsyms() throws TokenStreamException {
 		final String input = ":! ::% ::";
 		fLexer = createLexer(input);
 		
 		assertToken(CONSYM, ":!", fLexer.nextToken());
 		assertToken(CONSYM, "::%", fLexer.nextToken());
 		assertToken(OFTYPE, "::", fLexer.nextToken());
 	}
 	
 	public void testRecognizeIntegerLiterals() throws TokenStreamException {
 		final String input = "0123 0x123A 0o123 0X123 0O765";
 		fLexer = createLexer(input);
 
 		assertToken(INTEGER, "0123", fLexer.nextToken());
 		assertToken(INTEGER, "0x123A", fLexer.nextToken());
 		assertToken(INTEGER, "0o123", fLexer.nextToken());
 		assertToken(INTEGER, "0X123", fLexer.nextToken());
 		assertToken(INTEGER, "0O765", fLexer.nextToken());
 	}
 	
 	public void testIdentifiersWithQuotes() throws TokenStreamException {
 		fLexer = createLexer("MyConstructor' myFunction'");
 		
 		assertToken(CONSTRUCTOR_ID, "MyConstructor'", fLexer.nextToken());
 		assertToken(VARIABLE_ID, "myFunction'", fLexer.nextToken());
 	}
 	
 	public void testDotString() throws TokenStreamException {
 		final String input = "\".\"";
 		fLexer = createLexer(input);
 		
 		assertToken(STRING_LITERAL, ".", fLexer.nextToken());
 	}
 	
 	public void testCustomTabSize() throws TokenStreamException {
 		final String input = "module Main where\n" +
 				             "\tfat 0 = 1";
 		
 		final HaskellPreferenceProviderStub prefs = new HaskellPreferenceProviderStub();
 		prefs.setTabSize(4);
 		fLexer = createLexer(input, prefs);
 		
 		// module Main where \n
 		fLexer.skipTokens(4);
 		assertEquals(4, fLexer.nextToken().getColumn());
 	}
	
	public void testRecognizeVarsymWithAlt() throws TokenStreamException {
		fLexer = createLexer("var .|.");
		
		assertToken(VARIABLE_ID, "var", fLexer.nextToken());
		assertToken(VARSYM, ".|.", fLexer.nextToken());
	}
 
 	// TODO escape  -> 	 \ ( charesc | ascii | decimal | o octal | x hexadecimal )
 	//      charesc -> 	a | b | f | n | r | t | v | \ | " | ' | &
 	
 	// TODO implement ascii escape sequences (\NUL, \RET, etc.)
 	
 	// TODO look at the qualified name examples at the 2.4 section of the report
 }
