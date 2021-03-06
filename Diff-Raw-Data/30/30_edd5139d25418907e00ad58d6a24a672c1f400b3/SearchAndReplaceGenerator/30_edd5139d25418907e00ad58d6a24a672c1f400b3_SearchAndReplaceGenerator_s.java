 package net.thomasnardone.utils.gen;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.PrintStream;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Pattern;
 
 public abstract class SearchAndReplaceGenerator extends AbstractGenerator {
 	private static final String	AUTO_COMMENT_REGEX		= "\\t \\* Auto-generated by \\{@link .+\\}";
 	private static final String	METHOD_ANNOTATION_REGEX	= "\\t(\\/\\*\\*|\\s\\*.*|\\s\\*\\/|@.+)";
	private static final String	METHOD_REGEX			= "\\s(public|protected|private)?( static)? .+ %s\\(.*\\)( throws (\\w+, )*\\w+)? \\{";
 	protected final String		className;
 	protected final Class<?>	clazz;
 	protected final String		packageName;
 	private List<String>		code;
 	private final String		fileName;
 
 	public SearchAndReplaceGenerator(final String className, final String packageName) throws ClassNotFoundException {
 		clazz = Class.forName(className);
 		this.className = clazz.getSimpleName();
 		this.packageName = packageName;
 		fileName = getSourcePath() + packageName.replaceAll("\\.", "/") + "/" + this.className + getName() + ".java";
 	}
 
 	public void generate() throws IOException {
 		getOriginalCode();
 		generateStuff();
 		writeNewCode();
 	}
 
 	protected void addAutoGenerateComment(final int index) {
 		System.out.println("Adding comment at line " + index + 1);
 		code.add(index, "\t */");
 		code.add(index, "\t * Auto-generated by {@link " + getClass().getName() + "}.");
 		code.add(index, "\t/**");
 	}
 
 	protected abstract void generateStuff() throws IOException;
 
 	protected abstract String getName();
 
 	protected String getSourcePath() {
 		return "src/";
 	}
 
 	protected final void replaceMethod(final String methodName, final List<String> newMethod, final boolean preserveAnnotations)
 			throws IOException {
 		for (int i = 0; i < code.size();) {
 			String line = code.get(i);
 			if (Pattern.matches(String.format(METHOD_REGEX, methodName), line)) {
 				int newIndex = removeMethod(i, preserveAnnotations);
 				i = addMethod(newMethod, newIndex, preserveAnnotations);
 			} else {
 				i++;
 			}
 		}
 	}
 
 	private int addMethod(final List<String> method, final int index, final boolean preserveAnnotations) throws IOException {
 		int newIndex = index;
 		if (!preserveAnnotations) {
 			addAutoGenerateComment(index);
 			newIndex = index + 3;
 		}
 		indentMethod(method);
 		code.addAll(newIndex, method);
 		return newIndex + method.size();
 	}
 
 	private void getOriginalCode() throws IOException {
 		BufferedReader reader = new BufferedReader(new FileReader(fileName));
 		code = new ArrayList<String>();
 		String line = reader.readLine();
 		while (line != null) {
 			code.add(line);
 			line = reader.readLine();
 		}
 	}
 
 	private void indentMethod(final List<String> method) throws IOException {
 		final ByteArrayOutputStream output = new ByteArrayOutputStream();
 		openWriter(new PrintStream(output));
 		indent();
 		for (String line : method) {
 			writeln(line);
 		}
 		undent();
 		closeWriter();
 		final byte[] byteArray = output.toByteArray();
 		ByteArrayInputStream input = new ByteArrayInputStream(byteArray);
 
 		method.clear();
 		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 		String line = reader.readLine();
 		while (line != null) {
 			method.add(line);
 			line = reader.readLine();
 		}
 	}
 
 	private int removeMethod(final int index, final boolean preserveAnnotations) {
		code.remove(index);
		int bracketCount = 1;
		while (bracketCount > 0) {
 			String line = code.get(index);
 			for (int i = 0; i < line.length(); i++) {
 				switch (line.charAt(i)) {
 					case '{':
 						bracketCount++;
 						break;
 					case '}':
 						bracketCount--;
 						break;
 				}
 			}
 			code.remove(index);
 		}
 
 		int i = index - 1;
 		if (preserveAnnotations) {
 			String autoComment = "\t * Auto-generated by {@link " + getClass().getName() + "}.";
 			// exact match
 			while (Pattern.matches(METHOD_ANNOTATION_REGEX, code.get(i))) {
 				if (autoComment.equals(code.get(i))) {
 					code.remove(i);
 					code.add(i, autoComment);
					return index + 1;
 				}
 				i--;
 			}
 			// different auto-generate class
 			i = index - 1;
 			boolean hasAutoComment = false;
 			while (Pattern.matches(METHOD_ANNOTATION_REGEX, code.get(i))) {
 				if (!hasAutoComment && Pattern.matches(AUTO_COMMENT_REGEX, code.get(i))) {
 					code.remove(i);
 					hasAutoComment = true;
 				} else if (hasAutoComment && "\t/**".equals(code.get(i))) {
 					code.add(i + 1, autoComment);
					code.add(i + 2, "\t * ");
					return index + 1;
 				}
 				i--;
 			}
 			// no auto-generate comment
 			i = index - 1;
 			while (Pattern.matches(METHOD_ANNOTATION_REGEX, code.get(i))) {
 				if ("\t/**".equals(code.get(i))) {
 					code.add(i + 1, autoComment);
 					code.add(i + 2, "\t * ");
 					return index + 2;
 				}
 				i--;
 			}
 			// no comment
 			i = index - 1;
 			while (Pattern.matches(METHOD_ANNOTATION_REGEX, code.get(i))) {
 				i--;
 			}
 			addAutoGenerateComment(i + 1);
 			return index + 3;
 		} else {
 			while (Pattern.matches(METHOD_ANNOTATION_REGEX, code.get(i))) {
 				code.remove(i);
 				i--;
 			}
 			return i + 1;
 		}
 	}
 
 	private void writeNewCode() throws IOException {
 		PrintStream writer = new PrintStream(fileName);
 		for (String line : code) {
 			writer.println(line);
 		}
 		writer.flush();
 		writer.close();
 	}
 }
