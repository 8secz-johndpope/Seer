 package org.apache.maven.jxr;
 
 /*
  * CodeViewer.java
  * CoolServlets.com
  * March 2000
  *
  * Copyright (C) 2000 CoolServlets.com
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * 1) Redistributions of source code must retain the above copyright notice,
  *   this list of conditions and the following disclaimer.
  * 2) Redistributions in binary form must reproduce the above copyright notice,
  *   this list of conditions and the following disclaimer in the documentation
  *   and/or other materials provided with the distribution.
  * 3) Neither the name CoolServlets.com nor the names of its contributors may be
  *   used to endorse or promote products derived from this software without
  *   specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY COOLSERVLETS.COM AND CONTRIBUTORS ``AS IS'' AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL COOLSERVLETS.COM OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 import org.apache.maven.jxr.pacman.ClassType;
 import org.apache.maven.jxr.pacman.FileManager;
 import org.apache.maven.jxr.pacman.ImportType;
 import org.apache.maven.jxr.pacman.JavaFile;
 import org.apache.maven.jxr.pacman.PackageManager;
 import org.apache.maven.jxr.pacman.PackageType;
 import org.apache.maven.jxr.util.SimpleWordTokenizer;
 import org.apache.maven.jxr.util.StringEntry;
 import org.codehaus.plexus.util.StringUtils;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.Reader;
 import java.io.Serializable;
 import java.io.Writer;
 import java.util.Hashtable;
 import java.util.Locale;
 import java.util.Vector;
 
 /**
  * Syntax highlights java by turning it into html. A codeviewer object is
  * created and then keeps state as lines are passed in. Each line passed in as
  * java test, is returned as syntax highlighted html text. Users of the class
  * can set how the java code will be highlighted with setter methods. Only valid
  * java lines should be passed in since the object maintains state and may not
  * handle illegal code gracefully. The actual system is implemented as a series
  * of filters that deal with specific portions of the java code. The filters are
  * as follows: <pre>
  *  htmlFilter
  *     |__
  *        multiLineCommentFilter -> uriFilter
  *           |___
  *              inlineCommentFilter
  *                 |___
  *                    stringFilter
  *                       |___
  *                          keywordFilter
  *                             |___
  *                                uriFilter
  *                                   |___
  *                                      jxrFilter
  *                                         |___
  *                                            importFilter
  * </pre>
  */
 public class CodeTransform
     implements Serializable
 {
 
     /**
      * show line numbers
      */
     public static final boolean LINE_NUMBERS = true;
 
     /**
      * start comment delimeter
      */
     public static final String COMMENT_START = "<em class=\"comment\">";
 
     /**
      * end comment delimeter
      */
     public static final String COMMENT_END = "</em>";
 
     /**
      * start javadoc comment delimeter
      */
     public static final String JAVADOC_COMMENT_START = "<em>";
 
     /**
      * end javadoc comment delimeter
      */
     public static final String JAVADOC_COMMENT_END = "</em>";
 
     /**
      * start String delimeter
      */
     public static final String STRING_START = "<span class=\"string\">";
 
     /**
      * end String delimeter
      */
     public static final String STRING_END = "</span>";
 
     /**
      * start reserved word delimeter
      */
     public static final String RESERVED_WORD_START = "<strong>";
 
     /**
      * end reserved word delimeter
      */
     public static final String RESERVED_WORD_END = "</strong>";
 
     /**
      * stylesheet file name
      */
     public static final String STYLESHEET_FILENAME = "stylesheet.css";
 
     /**
      * Description of the Field
      */
     public static final String[] VALID_URI_SCHEMES = {"http://", "mailto:"};
 
     /**
      * Specify the only characters that are allowed in a URI besides alpha and
      * numeric characters. Refer RFC2396 - http://www.ietf.org/rfc/rfc2396.txt
      */
     public static final char[] VALID_URI_CHARS = {'?', '+', '%', '&', ':', '/', '.', '@', '_', ';', '=', '$', ',', '-',
         '!', '~', '*', '\'', '(', ')'};
 
     /**
      * HashTable containing java reserved words
      */
     private Hashtable reservedWords = new Hashtable();
 
     /**
      * flag set to true when a multi line comment is started
      */
     private boolean inMultiLineComment = false;
 
     /**
      * flag set to true when a javadoc comment is started
      */
     private boolean inJavadocComment = false;
 
     /**
      * Set the filename that is currently being processed.
      */
     private String currentFilename = null;
 
     /**
      * The current CVS revision of the currently transformed documnt
      */
     private String revision = null;
 
     /**
      * The currently being transformed source file
      */
     private String sourcefile = null;
 
     /**
      * The currently being written destfile
      */
     private String destfile = null;
 
     /**
      * The virtual source directory that is being read from: src/java
      */
     private String sourcedir = null;
 
     private String inputEncoding = null;
 
     private String outputEncoding = null;
 
     private Locale locale = null;
 
     /**
      * Relative path to javadocs, suitable for hyperlinking
      */
     private String javadocLinkDir;
 
     /**
      * Package Manager for this project.
      */
     private PackageManager packageManager;
 
     private FileManager fileManager;
 
     /**
      * Constructor for the CodeTransform object
      *
      * @param packageManager PackageManager for this project
      */
     public CodeTransform( PackageManager packageManager )
     {
         this.packageManager = packageManager;
         loadHash();
         this.fileManager = packageManager.getFileManager();
     }
 
     /**
      * Now different method of seeing if at end of input stream, closes inputs
      * stream at end.
      *
      * @param line String
      * @return filtered line of code
      */
     public final String syntaxHighlight( String line )
     {
         return htmlFilter( line );
     }
 
     /**
      * Filter html tags into more benign text.
      *
      * @param line String
      * @return html encoded line
      */
     private final String htmlFilter( String line )
     {
         if ( line == null || line.equals( "" ) )
         {
             return "";
         }
         line = replace( line, "&", "&amp;" );
         line = replace( line, "<", "&lt;" );
         line = replace( line, "\\\\", "&#47;&#47;" );
         line = replace( line, "\\\"", "\\&quot;" );
         line = replace( line, "'\"'", "'&quot;'" );
         return multiLineCommentFilter( line );
     }
 
     /**
      * Filter out multiLine comments. State is kept with a private boolean variable.
      *
      * @param line String
      * @return String
      */
     private final String multiLineCommentFilter( String line )
     {
         if ( line == null || line.equals( "" ) )
         {
             return "";
         }
         StringBuffer buf = new StringBuffer();
         int index;
 
         //First, check for the end of a java comment.
         if ( inJavadocComment && ( index = line.indexOf( "*/" ) ) > -1 && !isInsideString( line, index ) )
         {
             inJavadocComment = false;
             buf.append( JAVADOC_COMMENT_START );
             buf.append( line.substring( 0, index ) );
             buf.append( "*/" ).append( JAVADOC_COMMENT_END );
             if ( line.length() > index + 2 )
             {
                 buf.append( inlineCommentFilter( line.substring( index + 2 ) ) );
             }
 
             return uriFilter( buf.toString() );
         }
 
         //Second, check for the end of a multi-line comment.
         if ( inMultiLineComment && ( index = line.indexOf( "*/" ) ) > -1 && !isInsideString( line, index ) )
         {
             inMultiLineComment = false;
             buf.append( COMMENT_START );
             buf.append( line.substring( 0, index ) );
             buf.append( "*/" ).append( COMMENT_END );
             if ( line.length() > index + 2 )
             {
                 buf.append( inlineCommentFilter( line.substring( index + 2 ) ) );
             }
             return uriFilter( buf.toString() );
         }
 
         //If there was no end detected and we're currently in a multi-line
         //comment, we don't want to do anymore work, so return line.
         else if ( inMultiLineComment )
         {
 
             StringBuffer buffer = new StringBuffer( line );
             buffer.insert( 0, COMMENT_START );
             buffer.append( COMMENT_END );
             return uriFilter( buffer.toString() );
         }
         else if ( inJavadocComment )
         {
 
             StringBuffer buffer = new StringBuffer( line );
             buffer.insert( 0, JAVADOC_COMMENT_START );
             buffer.append( JAVADOC_COMMENT_END );
             return uriFilter( buffer.toString() );
         }
 
         //We're not currently in a Javadoc comment, so check to see if the start
         //of a multi-line Javadoc comment is in this line.
         else if ( ( index = line.indexOf( "/**" ) ) > -1 && !isInsideString( line, index ) )
         {
             inJavadocComment = true;
             //Return result of other filters + everything after the start
             //of the multiline comment. We need to pass the through the
             //to the multiLineComment filter again in case the comment ends
             //on the same line.
             buf.append( inlineCommentFilter( line.substring( 0, index ) ) );
             buf.append( JAVADOC_COMMENT_START ).append( "/**" );
             buf.append( multiLineCommentFilter( line.substring( index + 2 ) ) );
             buf.append( JAVADOC_COMMENT_END );
             return uriFilter( buf.toString() );
         }
 
         //We're not currently in a comment, so check to see if the start
         //of a multi-line comment is in this line.
         else if ( ( index = line.indexOf( "/*" ) ) > -1 && !isInsideString( line, index ) )
         {
             inMultiLineComment = true;
             //Return result of other filters + everything after the start
             //of the multiline comment. We need to pass the through the
             //to the multiLineComment filter again in case the comment ends
             //on the same line.
             buf.append( inlineCommentFilter( line.substring( 0, index ) ) );
             buf.append( COMMENT_START ).append( "/*" );
             buf.append( multiLineCommentFilter( line.substring( index + 2 ) ) );
             buf.append( COMMENT_END );
             return uriFilter( buf.toString() );
         }
 
         //Otherwise, no useful multi-line comment information was found so
         //pass the line down to the next filter for processesing.
         else
         {
             return inlineCommentFilter( line );
         }
     }
 
     /**
      * Filter inline comments from a line and formats them properly. One problem
      * we'll have to solve here: comments contained in a string should be
      * ignored... this is also true of the multiline comments. So, we could
      * either ignore the problem, or implement a function called something like
      * isInsideString(line, index) where index points to some point in the line
      * that we need to check... started doing this function below.
      *
      * @param line String
      * @return String
      */
     private final String inlineCommentFilter( String line )
     {
         if ( line == null || line.equals( "" ) )
         {
             return "";
         }
         StringBuffer buf = new StringBuffer();
         int index;
         if ( ( index = line.indexOf( "//" ) ) > -1 && !isInsideString( line, index ) )
         {
             buf.append( stringFilter( line.substring( 0, index ) ) );
             buf.append( COMMENT_START );
             buf.append( line.substring( index ) );
             buf.append( COMMENT_END );
         }
         else
         {
             buf.append( stringFilter( line ) );
         }
         return buf.toString();
     }
 
     /**
      * Filters strings from a line of text and formats them properly.
      *
      * @param line String
      * @return String
      */
     private final String stringFilter( String line )
     {
 
         if ( line == null || line.equals( "" ) )
         {
             return "";
         }
         StringBuffer buf = new StringBuffer();
         if ( line.indexOf( "\"" ) <= -1 )
         {
             return keywordFilter( line );
         }
         int start = 0;
         int startStringIndex = -1;
         int endStringIndex = -1;
         int tempIndex;
         //Keep moving through String characters until we want to stop...
         while ( ( tempIndex = line.indexOf( "\"" ) ) > -1 )
         {
             //We found the beginning of a string
             if ( startStringIndex == -1 )
             {
                 startStringIndex = 0;
                 buf.append( stringFilter( line.substring( start, tempIndex ) ) );
                 buf.append( STRING_START ).append( "\"" );
                 line = line.substring( tempIndex + 1 );
             }
             //Must be at the end
             else
             {
                 startStringIndex = -1;
                 endStringIndex = tempIndex;
                 buf.append( line.substring( 0, endStringIndex + 1 ) );
                 buf.append( STRING_END );
                 line = line.substring( endStringIndex + 1 );
             }
         }
 
         buf.append( keywordFilter( line ) );
 
         return buf.toString();
     }
 
     /**
      * Filters keywords from a line of text and formats them properly.
      *
      * @param line String
      * @return String
      */
     private final String keywordFilter( String line )
     {
         if ( line == null || line.equals( "" ) )
         {
             return "";
         }
         StringBuffer buf = new StringBuffer();
         Hashtable usedReservedWords = new Hashtable();
         int i = 0;
         char ch;
         StringBuffer temp = new StringBuffer();
         while ( i < line.length() )
         {
             temp.setLength( 0 );
             ch = line.charAt( i );
             while ( i < line.length() && ( ( ch >= 65 && ch <= 90 ) || ( ch >= 97 && ch <= 122 ) ) )
             {
                 temp.append( ch );
                 i++;
                 if ( i < line.length() )
                 {
                     ch = line.charAt( i );
                 }
             }
             String tempString = temp.toString();
             if ( reservedWords.containsKey( tempString ) && !usedReservedWords.containsKey( tempString ) )
             {
                 usedReservedWords.put( tempString, tempString );
                 line = replace( line, tempString, ( RESERVED_WORD_START + tempString + RESERVED_WORD_END ) );
                 i += ( RESERVED_WORD_START.length() + RESERVED_WORD_END.length() );
             }
             else
             {
                 i++;
             }
         }
         buf.append( line );
         return uriFilter( buf.toString() );
     }
 
     /**
      * Replace... I made it use a stringBuffer... hope it still works :)
      *
      * @param line String
      * @param oldString String
      * @param newString String
      * @return String
      */
     private final String replace( String line, String oldString, String newString )
     {
         int i = 0;
         while ( ( i = line.indexOf( oldString, i ) ) >= 0 )
         {
             line = ( new StringBuffer().append( line.substring( 0, i ) ).append( newString ).append(
                 line.substring( i + oldString.length() ) ) ).toString();
             i += newString.length();
         }
         return line;
     }
 
     /**
      * Checks to see if some position in a line is between String start and
      * ending characters. Not yet used in code or fully working :)
      *
      * @param line String
      * @param position int
      * @return boolean
      */
     private final boolean isInsideString( String line, int position )
     {
         if ( line.indexOf( "\"" ) < 0 )
         {
             return false;
         }
         int index;
         String left = line.substring( 0, position );
         String right = line.substring( position );
         int leftCount = 0;
         int rightCount = 0;
         while ( ( index = left.indexOf( "\"" ) ) > -1 )
         {
             leftCount++;
             left = left.substring( index + 1 );
         }
         while ( ( index = right.indexOf( "\"" ) ) > -1 )
         {
             rightCount++;
             right = right.substring( index + 1 );
         }
         return ( rightCount % 2 != 0 && leftCount % 2 != 0 );
     }
 
     /**
      * Description of the Method
      */
     private final void loadHash()
     {
         reservedWords.put( "abstract", "abstract" );
         reservedWords.put( "do", "do" );
         reservedWords.put( "inner", "inner" );
         reservedWords.put( "public", "public" );
         reservedWords.put( "var", "var" );
         reservedWords.put( "boolean", "boolean" );
         reservedWords.put( "continue", "continue" );
         reservedWords.put( "int", "int" );
         reservedWords.put( "return", "return" );
         reservedWords.put( "void", "void" );
         reservedWords.put( "break", "break" );
         reservedWords.put( "else", "else" );
         reservedWords.put( "interface", "interface" );
         reservedWords.put( "short", "short" );
         reservedWords.put( "volatile", "volatile" );
         reservedWords.put( "byvalue", "byvalue" );
         reservedWords.put( "extends", "extends" );
         reservedWords.put( "long", "long" );
         reservedWords.put( "static", "static" );
         reservedWords.put( "while", "while" );
         reservedWords.put( "case", "case" );
         reservedWords.put( "final", "final" );
         reservedWords.put( "native", "native" );
         reservedWords.put( "super", "super" );
         reservedWords.put( "transient", "transient" );
         reservedWords.put( "cast", "cast" );
         reservedWords.put( "float", "float" );
         reservedWords.put( "new", "new" );
         reservedWords.put( "rest", "rest" );
         reservedWords.put( "catch", "catch" );
         reservedWords.put( "for", "for" );
         reservedWords.put( "null", "null" );
         reservedWords.put( "synchronized", "synchronized" );
         reservedWords.put( "char", "char" );
         reservedWords.put( "finally", "finally" );
         reservedWords.put( "operator", "operator" );
         reservedWords.put( "this", "this" );
         reservedWords.put( "class", "class" );
         reservedWords.put( "generic", "generic" );
         reservedWords.put( "outer", "outer" );
         reservedWords.put( "switch", "switch" );
         reservedWords.put( "const", "const" );
         reservedWords.put( "goto", "goto" );
         reservedWords.put( "package", "package" );
         reservedWords.put( "throw", "throw" );
         reservedWords.put( "double", "double" );
         reservedWords.put( "if", "if" );
         reservedWords.put( "private", "private" );
         reservedWords.put( "true", "true" );
         reservedWords.put( "default", "default" );
         reservedWords.put( "import", "import" );
         reservedWords.put( "protected", "protected" );
         reservedWords.put( "try", "try" );
     }
 
     /**
      * Description of the Method
      *
      * @param oos ObjectOutputStream
      * @throws IOException
      */
     final void writeObject( ObjectOutputStream oos )
         throws IOException
     {
         oos.defaultWriteObject();
     }
 
     /**
      * Description of the Method
      *
      * @param ois ObjectInputStream
      * @throws ClassNotFoundException
      * @throws IOException
      */
     final void readObject( ObjectInputStream ois )
         throws ClassNotFoundException, IOException
     {
         ois.defaultReadObject();
     }
 
     /**
      * Gets the header attribute of the CodeTransform object
      *
      * @return String
      */
     public String getHeader()
     {
         StringBuffer buffer = new StringBuffer();
 
         String outputEncoding = this.outputEncoding;
         if ( outputEncoding == null )
         {
             outputEncoding = "ISO-8859-1";
         }
 
         // header
         buffer
             .append(
                 "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" )
             .append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"" ).append( locale )
             .append( "\" lang=\"" ).append( locale ).append( "\">\n" ).append( "<head>\n" )
             .append( "<meta http-equiv=\"content-type\" content=\"text/html; charset=" ).append( outputEncoding )
             .append( "\" />" );
 
         // title ("classname xref")
         try
         {
             buffer.append( "<title>" ).append( fileManager.getFile( this.getCurrentFilename() )
                 .getClassType().getName() ).append( " xref</title>\n" );
 
         }
         catch ( IOException e )
         {
             buffer.append( "<title>xref</title>\n" );
             e.printStackTrace();
         }
 
         // stylesheet link
         buffer.append( "<link type=\"text/css\" rel=\"stylesheet\" href=\"" ).append( this.getPackageRoot() )
             .append( STYLESHEET_FILENAME ).append( "\" />\n" );
 
         buffer.append( "</head>\n" ).append( "<body>\n" ).append( this.getFileOverview() );
 
         // start code section
         buffer.append( "<pre>\n" );
 
         return buffer.toString();
     }
 
     /**
      * Gets the footer attribute of the CodeTransform object
      *
      * @return String
      */
     public final String getFooter()
     {
         return "</pre>\n" + "<hr/>" + "<div id=\"footer\">" + JXR.NOTICE + "</div>" + "</body>\n" + "</html>\n";
     }
 
     /**
      * This is the public method for doing all transforms of code.
      *
      * @param sourceReader Reader
      * @param destWriter Writer
      * @param locale String
      * @param inputEncoding String
      * @param outputEncoding String
      * @param javadocLinkDir String
      * @param revision String
      * @param showHeader boolean
      * @param showFooter boolean
      * @throws IOException
      */
     public final void transform( Reader sourceReader, Writer destWriter, Locale locale, String inputEncoding,
                                  String outputEncoding, String javadocLinkDir, String revision, boolean showHeader,
                                  boolean showFooter )
         throws IOException
     {
         this.locale = locale;
         this.inputEncoding = inputEncoding;
         this.outputEncoding = outputEncoding;
         this.javadocLinkDir = javadocLinkDir;
         this.revision = revision;
 
         BufferedReader in = new BufferedReader( sourceReader );
 
         PrintWriter out = new PrintWriter( destWriter );
 
         String line = "";
 
         if ( showHeader )
         {
             out.println( getHeader() );
         }
 
         int linenumber = 1;
         while ( ( line = in.readLine() ) != null )
         {
             if ( LINE_NUMBERS )
             {
                 out.print( "<a name=\"" + linenumber + "\" " + "href=\"#" + linenumber + "\">" + linenumber +
                     "</a>" + getLineWidth( linenumber ) );
             }
 
             out.println( this.syntaxHighlight( line ) );
 
             ++linenumber;
         }
 
         if ( showFooter )
         {
             out.println( getFooter() );
         }
 
         out.flush();
     }
 
     /**
      * This is the public method for doing all transforms of code.
      *
      * @param sourcefile String
      * @param destfile String
      * @param locale String
      * @param inputEncoding String
      * @param outputEncoding String
      * @param javadocLinkDir String
      * @param revision String
      * @throws IOException
      */
     public final void transform( String sourcefile, String destfile, Locale locale, String inputEncoding,
                                  String outputEncoding, String javadocLinkDir, String revision )
         throws IOException
     {
 
         this.setCurrentFilename( sourcefile );
 
         this.sourcefile = sourcefile;
         this.destfile = destfile;
 
         //make sure that the parent directories exist...
         new File( new File( destfile ).getParent() ).mkdirs();
 
         Reader fr = null;
         Writer fw = null;
         try
         {
             if ( inputEncoding != null )
             {
                 fr = new InputStreamReader( new FileInputStream( sourcefile ), inputEncoding );
             }
             else
             {
                 fr = new FileReader( sourcefile );
             }
             if ( outputEncoding != null )
             {
                 fw = new OutputStreamWriter( new FileOutputStream( destfile ), outputEncoding );
             }
             else
             {
                 fw = new FileWriter( destfile );
             }
 
             transform( fr, fw, locale, inputEncoding, outputEncoding, javadocLinkDir, revision, true, true );
         }
         catch ( RuntimeException e )
         {
             System.out.println( "Unable to processPath " + sourcefile + " => " + destfile );
             throw e;
         }
         finally
         {
             if ( fr != null )
             {
                 try
                 {
                     fr.close();
                 }
                 catch ( Exception ex )
                 {
                     ex.printStackTrace();
                 }
             }
             if ( fw != null )
             {
                 try
                 {
                     fw.close();
                 }
                 catch ( Exception ex )
                 {
                     ex.printStackTrace();
                 }
             }
         }
 
     }
 
     /**
      * Get an overview header for this file.
      *
      * @return String
      */
     private final String getFileOverview()
     {
         StringBuffer overview = new StringBuffer();
 
         // only add the header if javadocs are present
         if ( javadocLinkDir != null )
         {
 
             overview.append( "<div id=\"overview\">" );
             //get the URI to get Javadoc info.
            StringBuffer javadocURI = new StringBuffer().append( javadocLinkDir );
 
             try
             {
                 JavaFile jf = fileManager.getFile( this.getCurrentFilename() );
 
                 javadocURI.append( StringUtils.replace( jf.getPackageType().getName(), ".", "/" ) );
                 javadocURI.append( "/" );
                 if ( jf.getClassType() != null )
                 {
                     javadocURI.append( jf.getClassType().getName() );
                 }
                 else
                 {
                     System.out.println( this.getCurrentFilename() );
                 }
                 javadocURI.append( ".html" );
 
             }
             catch ( IOException e )
             {
                 e.printStackTrace();
             }
 
             String javadocHREF = "<a href=\"" + javadocURI + "\">View Javadoc</a>";
 
             //get the generation time...
             overview.append( javadocHREF );
 
             overview.append( "</div>" );
         }
         return overview.toString();
     }
 
     /**
      * Handles line width which may need to change depending on which line
      * number you are on.
      *
      * @param linenumber int
      * @return String
      */
     private final String getLineWidth( int linenumber )
     {
         if ( linenumber < 10 )
         {
             return "   ";
         }
         else if ( linenumber < 100 )
         {
             return "  ";
         }
         else
         {
             return " ";
         }
     }
 
     /**
      * Handles finding classes based on the current filename and then makes
      * HREFs for you to link to them with.
      *
      * @param line String
      * @return String
      */
     private final String jxrFilter( String line )
     {
 
         JavaFile jf = null;
 
         try
         {
 
             //if the current file isn't set then just return
             if ( this.getCurrentFilename() == null )
             {
                 return line;
             }
 
             jf = fileManager.getFile( this.getCurrentFilename() );
         }
         catch ( IOException e )
         {
             e.printStackTrace();
             return line;
         }
 
         Vector v = new Vector();
 
         //get the imported packages
         ImportType[] imports = jf.getImportTypes();
         for ( int j = 0; j < imports.length; ++j )
         {
             v.addElement( imports[j].getPackage() );
         }
 
         //add the current package.
         v.addElement( jf.getPackageType().getName() );
 
         String[] packages = new String[v.size()];
         v.copyInto( packages );
 
         StringEntry[] words = SimpleWordTokenizer.tokenize( line );
 
         //go through each word and then match them to the correct class if necessary.
         for ( int i = 0; i < words.length; ++i )
         {
 
             //each word
             StringEntry word = words[i];
 
             for ( int j = 0; j < packages.length; ++j )
             {
 
                 //get the package from teh PackageManager because this will hold
                 //the version with the classes also.
 
                 PackageType currentImport = packageManager.getPackageType( packages[j] );
 
                 //the package here might in fact be null because it wasn't parsed out
                 //this might be something that is either not included or os part
                 //of another package and wasn't parsed out.
 
                 if ( currentImport == null )
                 {
                     continue;
                 }
 
                 //see if the current word is within the package
 
                 //at this point the word could be a fully qualified package name
                 //(FQPN) or an imported package name.
 
                 String wordName = word.toString();
 
                 if ( wordName.indexOf( "." ) != -1 )
                 {
 
                     //if there is a "." in the string then we have to assume
                     //it is a package.
 
                     String fqpn_package = null;
                     String fqpn_class = null;
 
                     fqpn_package = wordName.substring( 0, wordName.lastIndexOf( "." ) );
                     fqpn_class = wordName.substring( wordName.lastIndexOf( "." ) + 1, wordName.length() );
 
                     //note. since this is a reference to a full package then
                     //it doesn't have to be explicitly imported so this information
                     //is useless.  Instead just see if it was parsed out.
 
                     PackageType pt = packageManager.getPackageType( fqpn_package );
 
                     if ( pt != null )
                     {
 
                         ClassType ct = pt.getClassType( fqpn_class );
 
                         if ( ct != null )
                         {
                             //OK.  the user specified a full package to be imported
                             //that is in the package manager so it is time to
                             //link to it.
 
                             line = xrLine( line, pt.getName(), ct );
 
                         }
 
                     }
 
                     if ( fqpn_package.equals( currentImport.getName() ) &&
                         currentImport.getClassType( fqpn_class ) != null )
                     {
 
                         //then the package we are currently in is the one specified in the string
                         //and the import class is correct.
                         line = xrLine( line, packages[j], currentImport.getClassType( fqpn_class ) );
 
                     }
 
                 }
                 else if ( currentImport.getClassType( wordName ) != null )
                 {
 
                     line = xrLine( line, packages[j], currentImport.getClassType( wordName ) );
 
                 }
 
             }
 
         }
 
         return importFilter( line );
     }
 
     /**
      * Get the current filename
      *
      * @return String
      */
     public final String getCurrentFilename()
     {
         return this.currentFilename;
     }
 
     /**
      * Set the current filename
      *
      * @param filename String
      */
     public final void setCurrentFilename( String filename )
     {
         this.currentFilename = filename;
     }
 
     /**
      * Given the current package, get an HREF to the package and class given
      *
      * @param dest String
      * @param jc ClassType
      * @return String
      */
     private final String getHREF( String dest, ClassType jc )
     {
 
         StringBuffer href = new StringBuffer();
 
         //find out how to go back to the root
         href.append( this.getPackageRoot() );
 
         //now find out how to get to the dest package
         dest = StringUtils.replace( dest, ".*", "" );
         dest = StringUtils.replace( dest, ".", "/" );
 
         href.append( dest );
 
         //now append the classname.html file
         if ( jc != null )
         {
             href.append( "/" );
             href.append( jc.getName() );
             href.append( ".html" );
         }
 
         return href.toString();
     }
 
     /**
      * Based on the destination package, get the HREF.
      *
      * @param dest String
      * @return String
      */
     private final String getHREF( String dest )
     {
         return getHREF( dest, null );
     }
 
     /**
      * <p>Given the name of a package... get the number of
      * subdirectories/subpackages there would be. </p>
      * <p>EX: org.apache.maven == 3 </p>
      *
      * @param packageName String
      * @return int
      */
     private final int getPackageCount( String packageName )
     {
 
         if ( packageName == null )
         {
             return 0;
         }
 
         int count = 0;
 
         int index = 0;
 
         while ( true )
         {
 
             index = packageName.indexOf( ".", index );
 
             if ( index == -1 )
             {
                 break;
             }
             ++index;
             ++count;
         }
 
         //need to increment this by one
         count = ++count;
 
         return count;
     }
 
     /**
      * Parse out the current link and look for package/import statements and
      * then create HREFs for them
      *
      * @param line String
      * @return String
      */
     private final String importFilter( String line )
     {
 
         int start = -1;
 
         /*
          Used for determining if this is a package declaration.  If it is
          then we can make some additional assumptions:
          - that this isn't a Class import so the full String is valid
          - that it WILL be on the disk since this is based on the current
          - file.
          */
         boolean isPackage = line.trim().startsWith( "package " );
         boolean isImport = line.trim().startsWith( "import " );
 
         if ( isImport || isPackage )
         {
 
             start = line.trim().indexOf( " " );
         }
 
         if ( start != -1 )
         {
 
             //filter out this packagename...
 
             String pkg = line.substring( start, line.length() ).trim();
 
             //specify the classname of this import if any.
             String classname = null;
 
             if ( pkg.indexOf( ".*" ) != -1 )
             {
 
                 pkg = StringUtils.replace( pkg, ".*", "" );
 
             }
             else if ( !isPackage )
             {
 
                 //this is an explicit Class import
 
                 String packageLine = pkg.toString();
 
                 // This catches a boundary problem where you have something like:
                 //
                 // Foo foo = FooMaster.getFooInstance().
                 //     danceLittleFoo();
                 //
                 // This breaks Jxr and won't be a problem when we hook
                 // in the real parser.
 
                 int a = packageLine.lastIndexOf( "." ) + 1;
                 int b = packageLine.length() - 1;
 
                 if ( a > b + 1 )
                 {
                     classname = packageLine.substring( packageLine.lastIndexOf( "." ) + 1, packageLine.length() - 1 );
 
                     int end = pkg.lastIndexOf( "." );
 
                     if ( end == -1 )
                     {
                         end = pkg.length() - 1;
                     }
 
                     pkg = pkg.substring( 0, end );
                 }
 
             }
 
             pkg = StringUtils.replace( pkg, ";", "" );
             String pkgHREF = getHREF( pkg );
             //if this package is within the PackageManager then you can create an HREF for it.
 
             if ( packageManager.getPackageType( pkg ) != null || isPackage )
             {
 
                 //Create an HREF for explicit classname imports
                 if ( classname != null )
                 {
 
                     line = StringUtils.replace( line, classname, "<a href=\"" + pkgHREF + "/" + classname + ".html" +
                         "\">" + classname + "</a>" );
 
                 }
 
                 //now replace the given package with a href
                 line = StringUtils.replace( line, pkg, "<a href=\"" + pkgHREF + "/" + DirectoryIndexer.INDEX + "\">" +
                     pkg + "</a>" );
             }
 
         }
 
         return line;
     }
 
     /**
      * From the current file, determine the package root based on the current
      * path.
      *
      * @return String
      */
     public final String getPackageRoot()
     {
 
         StringBuffer buff = new StringBuffer();
 
         JavaFile jf = null;
 
         try
         {
             jf = fileManager.getFile( this.getCurrentFilename() );
         }
         catch ( IOException e )
         {
             e.printStackTrace();
             return null;
         }
 
         String current = jf.getPackageType().getName();
 
         int count = this.getPackageCount( current );
 
         for ( int i = 0; i < count; ++i )
         {
             buff.append( "../" );
         }
 
         return buff.toString();
     }
 
     /**
      * Given a line of text, search for URIs and make href's out of them
      *
      * @param line String
      * @return String
      */
     public final String uriFilter( String line )
     {
 
         for ( int i = 0; i < VALID_URI_SCHEMES.length; ++i )
         {
 
             String scheme = VALID_URI_SCHEMES[i];
 
             int index = line.indexOf( scheme );
 
             if ( index != -1 )
             {
 
                 int start = index;
 
                 int end = -1;
 
                 for ( int j = start; j < line.length(); ++j )
                 {
 
                     char current = line.charAt( j );
 
                     if ( !Character.isLetterOrDigit( current ) && isInvalidURICharacter( current ) )
                     {
                         end = j;
                         break;
                     }
 
                     end = j;
 
                 }
 
                 //now you should have the full URI so you can replace this
                 //in the current buffer
 
                 if ( end != -1 )
                 {
 
                     String uri = line.substring( start, end );
 
                     line = StringUtils.replace( line, uri,
                                                 "<a href=\"" + uri + "\" target=\"alexandria_uri\">" + uri + "</a>" );
                 }
             }
         }
 
         //if we are in a multiline comment we should not call JXR here.
         if ( !inMultiLineComment && !inJavadocComment )
         {
             return jxrFilter( line );
         }
         else
         {
             return line;
         }
 
     }
 
     /**
      * if the given char is not one of the following in VALID_URI_CHARS then
      * return true
      *
      * @param c char to check against VALID_URI_CHARS list
      * @return <code>true</code> if c is a valid URI char
      */
     private final boolean isInvalidURICharacter( char c )
     {
 
         for ( int i = 0; i < VALID_URI_CHARS.length; ++i )
         {
             if ( VALID_URI_CHARS[i] == c )
             {
                 return false;
             }
         }
 
         return true;
     }
 
     /**
      * The current revision of the CVS module
      *
      * @return String
      */
     public final String getRevision()
     {
         return this.revision;
     }
 
     /**
      * The current source file being read
      *
      * @return source file name
      */
     public final String getSourcefile()
     {
         return this.sourcefile;
     }
 
     /**
      * The current dest file being written
      *
      * @return destination file name
      */
     public final String getDestfile()
     {
         return this.destfile;
     }
 
     /**
      * The current source directory being read from.
      *
      * @return source directory
      */
     public final String getSourceDirectory()
     {
         return this.sourcedir;
     }
 
     /**
      * Cross Reference the given line with JXR returning the new content.
      *
      * @param line String
      * @param packageName String
      * @param classType ClassType
      * @return String
      */
     public final String xrLine( String line, String packageName, ClassType classType )
     {
 
         StringBuffer buff = new StringBuffer( line );
 
         String link = null;
         String find = null;
         String href = null;
 
         if ( classType != null )
         {
             href = this.getHREF( packageName, classType );
             find = classType.getName();
         }
         else
         {
             href = this.getHREF( packageName );
             find = packageName;
         }
 
         //build out what the link would be.
         link = "<a href=\"" + href + "\">" + find + "</a>";
 
         //use the SimpleWordTokenizer to find all entries
         //that match word.  Then replace these with the link
 
         //now replace the word in the buffer with the link
 
         String replace = link;
         StringEntry[] tokens = SimpleWordTokenizer.tokenize( buff.toString(), find );
 
         for ( int l = 0; l < tokens.length; ++l )
         {
 
             int start = tokens[l].getIndex();
             int end = tokens[l].getIndex() + find.length();
 
             buff.replace( start, end, replace );
 
         }
 
         return buff.toString();
     }
 
     /**
      * Highlight the package in this line.
      *
      * @param line input line
      * @param packageName package name
      * @return input line with linked package
      */
     public final String xrLine( String line, String packageName )
     {
 
         String href = this.getHREF( packageName );
 
         String find = packageName;
 
         //build out what the link would be.
         String link = "<a href=\"" + href + "\">" + find + "</a>";
 
         return StringUtils.replace( line, find, link );
     }
 
 }
