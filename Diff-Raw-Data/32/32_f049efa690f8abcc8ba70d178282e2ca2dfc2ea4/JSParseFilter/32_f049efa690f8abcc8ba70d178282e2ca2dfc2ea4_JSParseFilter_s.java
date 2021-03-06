 package org.apache.nutch.parse.js;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Properties;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.apache.nutch.parse.HTMLMetaTags;
 import org.apache.nutch.parse.HtmlParseFilter;
 import org.apache.nutch.parse.Outlink;
 import org.apache.nutch.parse.Parse;
 import org.apache.nutch.parse.ParseData;
 import org.apache.nutch.parse.ParseImpl;
 import org.apache.nutch.parse.ParseStatus;
 import org.apache.nutch.parse.Parser;
 import org.apache.nutch.protocol.Content;
 import org.apache.nutch.util.LogFormatter;
 import org.apache.oro.text.regex.MatchResult;
 import org.apache.oro.text.regex.Pattern;
 import org.apache.oro.text.regex.PatternCompiler;
 import org.apache.oro.text.regex.PatternMatcher;
 import org.apache.oro.text.regex.PatternMatcherInput;
 import org.apache.oro.text.regex.Perl5Compiler;
 import org.apache.oro.text.regex.Perl5Matcher;
 import org.w3c.dom.DocumentFragment;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 /**
  * This class is a heuristic link extractor for JavaScript files and
  * code snippets. The general idea of a two-pass regex matching comes from
  * Heritrix. Parts of the code come from OutlinkExtractor.java
  * by Stephan Strittmatter.
  *
  * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
  */
 public class JSParseFilter implements HtmlParseFilter, Parser {
   public static final Logger LOG =
     LogFormatter.getLogger("org.apache.nutch.parse.js.JSParseFilter");
 
   private static final int MAX_TITLE_LEN = 80;
   
   public Parse filter(Content content, Parse parse, HTMLMetaTags metaTags, DocumentFragment doc) {
     String url = content.getBaseUrl();
     ArrayList outlinks = new ArrayList();
     walk(doc, parse, metaTags, url, outlinks);
     if (outlinks.size() > 0) {
       Outlink[] old = parse.getData().getOutlinks();
       Properties metadata = parse.getData().getMetadata();
       String title = parse.getData().getTitle();
       List list = Arrays.asList(old);
       outlinks.addAll(list);
       ParseStatus status = parse.getData().getStatus();
       String text = parse.getText();
       Outlink[] newlinks = (Outlink[])outlinks.toArray(new Outlink[outlinks.size()]);
       parse = new ParseImpl(text, new ParseData(status, title, newlinks, metadata));
     }
     return parse;
   }
   
   private void walk(Node n, Parse parse, HTMLMetaTags metaTags, String base, List outlinks) {
     if (n instanceof Element) {
       String name = n.getNodeName();
       if (name.equalsIgnoreCase("script")) {
         String lang = null;
         Node lNode = n.getAttributes().getNamedItem("language");
         if (lNode == null) lang = "javascript";
         else lang = lNode.getNodeValue();
         StringBuffer script = new StringBuffer();
         NodeList nn = n.getChildNodes();
         if (nn.getLength() > 0) {
           for (int i = 0; i < nn.getLength(); i++) {
             if (i > 0) script.append('\n');
             script.append(nn.item(i).getNodeValue());
           }
           //LOG.info("script: language=" + lang + ", text: " + script.toString());
           Outlink[] links = getJSLinks(script.toString(), base, base);
           if (links != null && links.length > 0) outlinks.addAll(Arrays.asList(links));
           // no other children of interest here, go one level up.
           return;
         }
       } else {
         // process all HTML 4.0 events, if present...
         NamedNodeMap attrs = n.getAttributes();
         int len = attrs.getLength();
         for (int i = 0; i < len; i++) {
           // Window: onload,onunload
           // Form: onchange,onsubmit,onreset,onselect,onblur,onfocus
           // Keyboard: onkeydown,onkeypress,onkeyup
           // Mouse: onclick,ondbclick,onmousedown,onmouseout,onmousover,onmouseup
           Node anode = attrs.item(i);
           if (anode.getNodeName().startsWith("on")) {
            Outlink[] links = getJSLinks(anode.getNodeValue(), base, base);
            if (links != null && links.length > 0) outlinks.addAll(Arrays.asList(links));
           }
         }
       }
     }
     NodeList nl = n.getChildNodes();
     for (int i = 0; i < nl.getLength(); i++) {
       walk(nl.item(i), parse, metaTags, base, outlinks);
     }
   }
   
   public Parse getParse(Content c) {
     String type = c.getContentType();
     if (type != null && !type.toLowerCase().startsWith("application/x-javascript"))
       return new ParseStatus(ParseStatus.FAILED_INVALID_FORMAT,
               "Content not JavaScript: " + type).getEmptyParse();
     String script = new String(c.getContent());
     Outlink[] outlinks = getJSLinks(script, c.getUrl(), c.getUrl());
     if (outlinks == null) outlinks = new Outlink[0];
     // Title? use the first line of the script...
     String title;
     int idx = script.indexOf('\n');
     if (idx != -1) {
       if (idx > MAX_TITLE_LEN) idx = MAX_TITLE_LEN;
       title = script.substring(0, idx);
     } else {
       idx = Math.min(MAX_TITLE_LEN, script.length());
       title = script.substring(0, idx);
     }
     Properties metadata = new Properties();
     metadata.putAll(c.getMetadata());
     ParseData pd = new ParseData(ParseStatus.STATUS_SUCCESS, title,
             outlinks, metadata);
     Parse parse = new ParseImpl(script, pd);
     return parse;
   }
   
   private static final String STRING_PATTERN = "(\\\\*(?:\"|\'))([^\\s\"\']+?)(?:\\1)";
   // A simple pattern. This allows also invalid URL characters.
   private static final String URI_PATTERN = "(^|\\s*?)/?\\S+?[/\\.]\\S+($|\\s*)";
   // Alternative pattern, which limits valid url characters.
   //private static final String URI_PATTERN = "(^|\\s*?)[A-Za-z0-9/](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+[/.](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2})+(#([a-zA-Z0-9][a-zA-Z0-9$_.+!*,;/?:@&~=%-]*))?($|\\s*)";
   
   /**
    *  This method extracts URLs from literals embedded in JavaScript.
    */
   private static Outlink[] getJSLinks(String plainText, String anchor, String base) {
 
     final List outlinks = new ArrayList();
     URL baseURL = null;
     
     try {
       baseURL = new URL(base);
     } catch (Exception e) {
       LOG.throwing(JSParseFilter.class.getName(), "getJSLinks", e);
     }
 
     try {
       final PatternCompiler cp = new Perl5Compiler();
       final Pattern pattern = cp.compile(STRING_PATTERN,
           Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.READ_ONLY_MASK
               | Perl5Compiler.MULTILINE_MASK);
       final Pattern pattern1 = cp.compile(URI_PATTERN,
               Perl5Compiler.CASE_INSENSITIVE_MASK | Perl5Compiler.READ_ONLY_MASK
                   | Perl5Compiler.MULTILINE_MASK);
       final PatternMatcher matcher = new Perl5Matcher();
 
       final PatternMatcher matcher1 = new Perl5Matcher();
       final PatternMatcherInput input = new PatternMatcherInput(plainText);
 
       MatchResult result;
       String url;
 
       //loop the matches
       while (matcher.contains(input, pattern)) {
         result = matcher.getMatch();
         url = result.group(2);
         PatternMatcherInput input1 = new PatternMatcherInput(url);
         if (!matcher1.matches(input1, pattern1)) {
           //LOG.fine(" - invalid '" + url + "'");
           continue;
         }
         if (url.startsWith("www.")) {
             url = "http://" + url;
         } else url = new URL(baseURL, url).toString();
         url = url.replaceAll("&amp;", "&");
         LOG.fine(" - outlink from JS: '" + url + "'");
         outlinks.add(new Outlink(url, anchor));
       }
     } catch (Exception ex) {
       // if it is a malformed URL we just throw it away and continue with
       // extraction.
       LOG.throwing(JSParseFilter.class.getName(), "getJSLinks", ex);
     }
 
     final Outlink[] retval;
 
     //create array of the Outlinks
     if (outlinks != null && outlinks.size() > 0) {
       retval = (Outlink[]) outlinks.toArray(new Outlink[0]);
     } else {
       retval = new Outlink[0];
     }
 
     return retval;
   }
   
   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println(JSParseFilter.class.getName() + " file.js baseURL");
       return;
     }
     InputStream in = new FileInputStream(args[0]);
     BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
     StringBuffer sb = new StringBuffer();
     String line = null;
     while ((line = br.readLine()) != null) sb.append(line + "\n");
     Outlink[] links = getJSLinks(sb.toString(), args[1], args[1]);
     System.out.println("Outlinks extracted: " + links.length);
     for (int i = 0; i < links.length; i++)
       System.out.println(" - " + links[i]);
   }
 }
