 package hudson.plugins.warnings.parser;
 
 import java.util.regex.Matcher;
 
 import org.apache.commons.lang.StringUtils;
 
 /**
  * A parser for the ant JavaDoc compiler warnings.
  *
  * @author Ulli Hafner
  */
 public class JavaDocParser extends RegexpLineParser {
     /** Warning type of this parser. */
     static final String WARNING_TYPE = "JavaDoc";
     /** Pattern of javac compiler warnings. */
    private static final String JAVA_DOC_WARNING_PATTERN = "(?:\\s*\\[(?:javadoc|WARNING)\\]\\s*)?(?:(?:(.*):(\\d+))|(?:\\s*javadoc\\s*)):\\s*warning\\s*-\\s*(.*)";
 
     /**
      * Creates a new instance of <code>AntJavacParser</code>.
      */
     public JavaDocParser() {
         super(JAVA_DOC_WARNING_PATTERN, WARNING_TYPE);
     }
 
     /**
      * Creates a new annotation for the specified pattern.
      *
      * @param matcher the regular expression matcher
      * @return a new annotation for the specified pattern
      */
     @Override
     protected Warning createWarning(final Matcher matcher) {
         String message = matcher.group(3);
         String fileName = StringUtils.defaultIfEmpty(matcher.group(1), " - ");
 
         return new Warning(fileName, getLineNumber(matcher.group(2)), WARNING_TYPE, StringUtils.EMPTY, message);
     }
 }
 
