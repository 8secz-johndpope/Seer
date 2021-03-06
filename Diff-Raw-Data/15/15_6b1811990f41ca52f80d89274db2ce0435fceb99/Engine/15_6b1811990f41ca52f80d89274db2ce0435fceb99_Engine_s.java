 package com.floreysoft.jmte;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import com.floreysoft.jmte.token.ElseToken;
 import com.floreysoft.jmte.token.EndToken;
 import com.floreysoft.jmte.token.ForEachToken;
 import com.floreysoft.jmte.token.IfToken;
 import com.floreysoft.jmte.token.StringToken;
 
 /**
  * <p>
  * The template engine.
  * </p>
  * 
  * <p>
  * Usually this is the only class you need calling
  * {@link #transform(String, Map)}. Like this
  * </p>
  * 
  * <pre>
  * Engine engine = new Engine();
  * String transformed = engine.transform(input, model);
  * </pre>
  * 
  * <p>
  * You have to provide the template input written in the template language and a
  * model from String to Object. Maybe like this
  * </p>
  * 
  * <pre>
  * String input = &quot;${name}&quot;;
  * Map&lt;String, Object&gt; model = new HashMap&lt;String, Object&gt;();
  * model.put(&quot;name&quot;, &quot;Minimal Template Engine&quot;);
  * Engine engine = new Engine();
  * String transformed = engine.transform(input, model);
  * assert (transformed.equals(&quot;Minimal Template Engine&quot;));
  * </pre>
  * 
  * where <code>input</code> contains the template and <code>model</code> the
  * model. <br>
  * 
  * @see Lexer
  * @see ErrorHandler
  * @see Tool
  */
 public final class Engine {
 	private static final String ODD = "odd_";
 	private static final String EVEN = "even_";
 	private static final String LAST = "last_";
 	private static final String FIRST = "first_";
 	private static final String EVIL_HACKY_DOUBLE_BACKSLASH_PLACEHOLDER = "EVIL_HACKY_DOUBLE_BACKSLASH_PLACEHOLDER";
 
 	/**
 	 * Pairs of begin/end.
 	 *
 	 */
 	public static class StartEndPair {
 		public final int start;
 		public final int end;
 
 		public StartEndPair(int start, int end) {
 			this.start = start;
 			this.end = end;
 		}
 
 		@Override
 		public String toString() {
 			return "" + start + "-" + end;
 		}
 	}
 
 	/**
 	 * Replacement for {@link java.lang.String.format}. All arguments will be
 	 * put into the model having their index starting from 1 as their name.
 	 * 
 	 * @param pattern
 	 *            the template
 	 * @param args
 	 *            any number of arguments
 	 * @return the expanded template
 	 */
 	public static String format(String pattern, Object... args) {
 		Map<String, Object> model = arrayToModel(null, args);
 		Engine engine = new Engine();
 		String output = engine.transform(pattern, model);
 		return output;
 	}
 
 	/**
 	 * Transforms an array to a model using the index of the elements (starting
 	 * from 1) in the array and a prefix to form their names.
 	 * 
 	 * @param prefix
 	 *            the prefix to add to the index or <code>null</code> if none
 	 *            shall be applied
 	 * @param args
 	 *            the array to be transformed into the model
 	 * @return the model containing the arguments
 	 */
 	public static Map<String, Object> arrayToModel(String prefix,
 			Object... args) {
 		Map<String, Object> model = new HashMap<String, Object>();
 		if (prefix == null) {
 			prefix = "";
 		}
 		for (int i = 0; i < args.length; i++) {
 			Object value = args[i];
 			String name = prefix + (i + 1);
 			model.put(name, value);
 		}
 		return model;
 	}
 	
 	/**
 	 * Merges any number of named lists into a single one contained their
 	 * combined values. Can be very handy in case of a servlet request which
 	 * might contain several lists of parameters that you want to iterate over
 	 * in a combined way.
 	 * 
 	 * @param names
 	 *            the names of the variables in the following lists
 	 * @param lists
 	 *            the lists containing the values for the named variables
 	 * @return a merge list containing the combined values of the lists
 	 */
 	public static List<Map<String, Object>> mergeLists(String[] names,
 			List<Object>... lists) {
 		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
 		if (lists.length != 0) {
 			
 			// first check if all looks good
 			int expectedSize = names.length;
 			for (int i = 0; i < lists.length; i++) {
 				List<Object> list = lists[i];
 				if (list.size() != expectedSize) {
 					throw new IllegalArgumentException(
 							"All lists and array of names must have the same size!");
 				}
 			}
 
 			// yes, things are ok
 			List<Object> masterList = lists[0];
 			for (int i = 0; i < masterList.size(); i++) {
 				Map<String, Object> map = new HashMap<String, Object>();
 				for (int j = 0; j < lists.length; j++) {
 					String name = names[j];
 					List<Object> list = lists[j];
 					Object value = list.get(i);
 					map.put(name, value);
 				}
 				resultList.add(map);
 			}
 		}
 		return resultList;
 	}
 
 	private final String exprStartToken;
 	private final String exprEndToken;
 	private final double expansionSizeFactor;
 	private Lexer lexer;
 	private LinkedList<Token> scopes = new LinkedList<Token>();
 	private ErrorHandler errorHandler;
 	private Set<String> panicModelCleanupSet;
 
 	/**
 	 * Creates a new engine having <code>${</code> and <code>}</code> as start
 	 * and end strings for expressions.
 	 */
 	public Engine() {
 		this("${", "}", 1.2);
 	}
 
 	/**
 	 * Creates a new engine having custom start and end strings for expressions.
 	 * 
 	 * @param exprStartToken
 	 *            the string that starts an expression
 	 * @param exprEndToken
 	 *            the string that ends an expression
 	 */
 	public Engine(String exprStartToken, String exprEndToken) {
 		this(exprStartToken, exprEndToken, 1.2);
 	}
 
 	/**
 	 * Creates a new engine having custom start and end strings for expressions.
 	 * 
 	 * @param exprStartToken
 	 *            the string that starts an expression
 	 * @param exprEndToken
 	 *            the string that ends an expression
 	 * @param expansionSizeFactor
 	 *            the factor for the expected size of the expanded output
 	 */
 	public Engine(String exprStartToken, String exprEndToken,
 			double expansionSizeFactor) {
 		this.exprStartToken = exprStartToken;
 		this.exprEndToken = exprEndToken;
 		this.expansionSizeFactor = expansionSizeFactor;
 		this.setLexer(new DefaultLexer());
 		this.setErrorHandler(new DefaultErrorHandler());
 	}
 
 	/**
 	 * Transforms a template into an expanded output using the given model.
 	 * 
 	 * @param template
 	 *            the template to expand
 	 * @param model
 	 *            the model used to evaluate expressions inside the template
 	 * @return the expanded output
 	 */
 	public String transform(String template, Map<String, Object> model) {
 		return transform(template, model, true);
 	}
 
 	/**
 	 * Transforms a template into an expanded output using the given model.
 	 * 
 	 * @param template
 	 *            the template to expand
 	 * @param model
 	 *            the model used to evaluate expressions inside the template
 	 * @param useEscaping
 	 *            <code>true</code> if you want escaping to be applied - which
 	 *            is the default
 	 * 
 	 * @return the expanded output
 	 */
 	public String transform(String template, Map<String, Object> model,
 			boolean useEscaping) {
 		List<StartEndPair> scan = scan(template, useEscaping);
 		String transformed = transformPure(template, scan, model);
 		if (!useEscaping) {
 			return transformed;
 		} else {
 			String unescaped = applyEscapes(transformed);
 			return unescaped;
 		}
 	}
 
 	String transformPure(String input, List<StartEndPair> scan,
 			Map<String, Object> model) {
 		panicModelCleanupSet = new HashSet<String>();
 
 		try {
 			char[] inputChars = input.toCharArray();
 			StringBuilder output = new StringBuilder(
 					(int) (input.length() * expansionSizeFactor));
 			int offset = 0;
 			int i = 0;
 			while (i < scan.size()) {
 				StartEndPair startEndPair = scan.get(i);
 				int length = startEndPair.start - getExprStartToken().length()
 						- offset;
 				boolean skipMode = isSkipMode();
 				if (!skipMode) {
 					output.append(inputChars, offset, length);
 				}
 				offset = startEndPair.end + getExprEndToken().length();
 				i++;
 
 				Token token = lexer.nextToken(inputChars, startEndPair.start,
 						startEndPair.end, model, skipMode, getErrorHandler());
 				if (token instanceof StringToken) {
 					if (!skipMode) {
 						String expanded = ((StringToken) token).getValue();
 						output.append(expanded);
 					}
 				} else if (token instanceof ForEachToken) {
 					ForEachToken feToken = (ForEachToken) token;
 					if (model.containsKey(feToken.getVarName())) {
 						getErrorHandler()
 								.error(
 										String
 												.format(
 														"Foreach variable name '%s' already present in model",
 														feToken.getVarName()),
 										inputChars, startEndPair.start,
 										startEndPair.end);
 					}
 					if (!feToken.iterator().hasNext()) {
 						token = new IfToken(false);
 					} else {
 						Object value = feToken.iterator().next();
 						model.put(feToken.getVarName(), value);
 						panicModelCleanupSet.add(feToken.getVarName());
 						feToken.setScanIndex(i);
 						feToken.setOffset(offset);
 						feToken.setFirst(true);
 						feToken.setIndex(0);
 						feToken.setLast(!feToken.iterator().hasNext());
 						addSpecialVariables(feToken, model);
 					}
 					push(token);
 				} else if (token instanceof IfToken) {
 					push(token);
 				} else if (token instanceof ElseToken) {
 					Token poppedToken = peek();
 					if (!(poppedToken instanceof IfToken)) {
						getErrorHandler().error(
								"Can't use else outside of if block",
								inputChars, startEndPair.start,
								startEndPair.end);
 					}
 					push(token);
 				} else if (token instanceof EndToken) {
 					Token poppedToken = pop();
 					if (poppedToken == null) {
 						getErrorHandler().error("Unmatched end", inputChars,
 								startEndPair.start, startEndPair.end);
 					} else if (poppedToken instanceof ForEachToken) {
 						ForEachToken feToken = (ForEachToken) poppedToken;
 						if (feToken.iterator().hasNext()) {
 							Object value = feToken.iterator().next();
 							model.put(feToken.getVarName(), value);
 							panicModelCleanupSet.add(feToken.getVarName());
 							i = feToken.getScanIndex();
 							offset = feToken.getOffset();
 							startEndPair = scan.get(i);
 							push(feToken);
 							if (!skipMode && feToken.getSeparator() != null) {
 								output.append(feToken.getSeparator());
 							}
 							feToken.setFirst(false);
 							feToken.setLast(!feToken.iterator().hasNext());
 							feToken.setIndex(feToken.getIndex() + 1);
 							addSpecialVariables(feToken, model);
 						} else {
 							removeSpecialVariables(feToken, model);
 							model.remove(feToken.getVarName());
 							panicModelCleanupSet.remove(feToken.getVarName());
 						}
 					}
 				}
 			}
 
 			// do not forget to add the final chunk of pure text (might be the
 			// only
 			// chunk indeed)
 			int remainingChars = input.length() - offset;
 			output.append(inputChars, offset, remainingChars);
 			return output.toString();
 		} finally {
 			for (String varName : panicModelCleanupSet) {
 				model.remove(varName);
 			}
 		}
 	}
 
 	private void addSpecialVariables(ForEachToken feToken,
 			Map<String, Object> model) {
 		String suffix = feToken.getVarName();
 		model.put(FIRST + suffix, feToken.isFirst());
 		model.put(LAST + suffix, feToken.isLast());
 		model.put(EVEN + suffix, feToken.getIndex() % 2 == 0);
 		model.put(ODD + suffix, feToken.getIndex() % 2 == 1);
 		panicModelCleanupSet.add(FIRST + suffix);
 		panicModelCleanupSet.add(LAST + suffix);
 		panicModelCleanupSet.add(EVEN + suffix);
 		panicModelCleanupSet.add(ODD + suffix);
 	}
 
 	private void removeSpecialVariables(ForEachToken feToken,
 			Map<String, Object> model) {
 		String suffix = feToken.getVarName();
 		model.remove(FIRST + suffix);
 		model.remove(LAST + suffix);
 		model.remove(EVEN + suffix);
 		model.remove(ODD + suffix);
 		panicModelCleanupSet.remove(FIRST + suffix);
 		panicModelCleanupSet.remove(LAST + suffix);
 		panicModelCleanupSet.remove(EVEN + suffix);
 		panicModelCleanupSet.remove(ODD + suffix);
 	}
 
 	private void push(Token token) {
 		scopes.add(token);
 	}
 
 	private Token pop() {
 		if (scopes.isEmpty()) {
 			return null;
 		} else {
 			Token token = scopes.removeLast();
 			if (token instanceof ElseToken) {
 				// we need to pop off the if token as well as the end token
 				// terminates both
 				pop();
 			}
 			return token;
 		}
 	}
 
 	private Token peek() {
 		if (scopes.isEmpty()) {
 			return null;
 		} else {
 			return scopes.getLast();
 		}
 	}
 
 	private boolean isSkipMode() {
 		boolean condition = true;
 		for (Token token : scopes) {
 			if (token instanceof IfToken) {
 				condition = ((IfToken) token).getCondition();
 			} else if (token instanceof ElseToken) {
 				condition = !condition;
 			}
 		}
 		return !condition;
 	}
 
 	/**
 	 * Scans the input and spits out begin/end pairs telling you where
 	 * expressions can be found.
 	 * 
 	 * @param input
 	 *            the input
 	 * @param useEscaping
 	 *            tells the method whether to use (<code>true</code>) or ignore
 	 *            escape character \\
 	 * @return the begin/end pairs telling you where expressions can be found
 	 */
 	public List<StartEndPair> scan(String input, boolean useEscaping) {
 		List<StartEndPair> result = new ArrayList<StartEndPair>();
 		int fromIndex = 0;
 		while (true) {
 			int exprStart = input.indexOf(getExprStartToken(), fromIndex);
 			if (exprStart == -1) {
 				break;
 			}
 			if (useEscaping && isEscaped(input, exprStart)) {
 				fromIndex = exprStart + getExprStartToken().length();
 				continue;
 			}
 
 			exprStart += getExprStartToken().length();
 			int exprEnd = input.indexOf(getExprEndToken(), exprStart);
 			while (useEscaping && isEscaped(input, exprEnd)) {
 				exprEnd = input.indexOf(getExprEndToken(), exprEnd
 						+ getExprEndToken().length());
 			}
 
 			fromIndex = exprEnd + getExprEndToken().length();
 
 			StartEndPair startEndPair = new StartEndPair(exprStart, exprEnd);
 			result.add(startEndPair);
 		}
 		return result;
 	}
 
 	String applyEscapes(String input) {
 		String unescaped = input.replaceAll("\\\\\\\\",
 				EVIL_HACKY_DOUBLE_BACKSLASH_PLACEHOLDER);
 		unescaped = unescaped.replaceAll("\\\\", "");
 		unescaped = unescaped.replaceAll(
 				EVIL_HACKY_DOUBLE_BACKSLASH_PLACEHOLDER, "\\\\");
 		return unescaped;
 	}
 
 	// a character is escaped when it is preceded by an unescaped \
 	private boolean isEscaped(String input, int index) {
 		boolean escaped;
 		int leftOfIndex = index - 1;
 		if (leftOfIndex >= 0) {
 			if (input.charAt(leftOfIndex) == '\\') {
 				int leftOfleftOfIndex = leftOfIndex - 1;
 				escaped = leftOfleftOfIndex < 0
 						|| input.charAt(leftOfleftOfIndex) != '\\';
 			} else {
 				escaped = false;
 			}
 		} else {
 			escaped = false;
 		}
 		return escaped;
 	}
 
 	/**
 	 * Sets a new lexer - which might be different from standard one.
 	 * 
 	 * @param lexer
 	 *            the new lexer
 	 */
 	public void setLexer(Lexer lexer) {
 		this.lexer = lexer;
 	}
 
 	/**
 	 * Gets the currently used lexer.
 	 * 
 	 * @return the currently used lexer
 	 */
 	public Lexer getLexer() {
 		return lexer;
 	}
 
 	/**
 	 * Sets the error handler to be used in this engine
 	 * 
 	 * @param errorHandler
 	 *            the new error handler
 	 */
 	public void setErrorHandler(ErrorHandler errorHandler) {
 		this.errorHandler = errorHandler;
 	}
 
 	/**
 	 * Gets the currently used error handler
 	 * 
 	 * @return the error handler
 	 */
 	public ErrorHandler getErrorHandler() {
 		return errorHandler;
 	}
 
 	public String getExprStartToken() {
 		return exprStartToken;
 	}
 
 	public String getExprEndToken() {
 		return exprEndToken;
 	}
 
 }
