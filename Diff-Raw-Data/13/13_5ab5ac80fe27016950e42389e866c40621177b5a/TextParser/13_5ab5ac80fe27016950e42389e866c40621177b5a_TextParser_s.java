 // Parser.java
 // (C) 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 09.07.2009 on http://yacy.net
 //
 // This is a part of YaCy, a peer-to-peer based web search engine
 //
 // $LastChangedDate: 2009-03-20 16:44:59 +0100 (Fr, 20 Mrz 2009) $
 // $LastChangedRevision: 5736 $
 // $LastChangedBy: borg-0300 $
 //
 // LICENSE
 // 
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 package net.yacy.document;
 
 import java.io.BufferedInputStream;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 
 import net.yacy.document.parser.bzipParser;
 import net.yacy.document.parser.csvParser;
 import net.yacy.document.parser.docParser;
 import net.yacy.document.parser.gzipParser;
 import net.yacy.document.parser.htmlParser;
 import net.yacy.document.parser.odtParser;
 import net.yacy.document.parser.ooxmlParser;
 import net.yacy.document.parser.pdfParser;
 import net.yacy.document.parser.pptParser;
 import net.yacy.document.parser.psParser;
 import net.yacy.document.parser.rssParser;
 import net.yacy.document.parser.rtfParser;
 import net.yacy.document.parser.sevenzipParser;
 import net.yacy.document.parser.swfParser;
 import net.yacy.document.parser.tarParser;
 import net.yacy.document.parser.torrentParser;
 import net.yacy.document.parser.vcfParser;
 import net.yacy.document.parser.vsdParser;
 import net.yacy.document.parser.xlsParser;
 import net.yacy.document.parser.zipParser;
 import net.yacy.document.parser.images.genericImageParser;
 import net.yacy.kelondro.data.meta.DigestURI;
 import net.yacy.kelondro.logging.Log;
 import net.yacy.kelondro.util.FileUtils;
 
 
 public final class TextParser {
 
     private static final Log log = new Log("PARSER");
 
     private static final Map<String, Idiom> mime2parser = new ConcurrentHashMap<String, Idiom>();
     private static final Map<String, Idiom> ext2parser = new ConcurrentHashMap<String, Idiom>();
     private static final Map<String, String> ext2mime = new ConcurrentHashMap<String, String>();
     private static final Map<String, Object> denyMime = new ConcurrentHashMap<String, Object>();
     private static final Map<String, Object> denyExtensionx = new ConcurrentHashMap<String, Object>();
     
     static {
         initParser(new bzipParser());
         initParser(new csvParser());
         initParser(new docParser());
         initParser(new gzipParser());
         initParser(new htmlParser());
         initParser(new genericImageParser());
         initParser(new odtParser());
         initParser(new ooxmlParser());
         initParser(new pdfParser());
         initParser(new pptParser());
         initParser(new psParser());
         initParser(new rssParser());
         initParser(new rtfParser());
         initParser(new sevenzipParser());
         initParser(new swfParser());
         initParser(new tarParser());
         initParser(new torrentParser());
         initParser(new vcfParser());
         initParser(new vsdParser());
         initParser(new xlsParser());
         initParser(new zipParser());
     }
     
     public static Set<Idiom> idioms() {
         Set<Idiom> c = new HashSet<Idiom>();
         c.addAll(ext2parser.values());
         c.addAll(mime2parser.values());
         return c;
     }
 
     private static void initParser(Idiom parser) {
         String prototypeMime = null;
         for (String mime: parser.supportedMimeTypes()) {
             // process the mime types
             final String mimeType = normalizeMimeType(mime);
             if (prototypeMime == null) prototypeMime = mimeType;
             Idiom p0 = mime2parser.get(mimeType);
             if (p0 != null) log.logSevere("parser for mime '" + mimeType + "' was set to '" + p0.getName() + "', overwriting with new parser '" + parser.getName() + "'.");
             mime2parser.put(mimeType, parser);
             Log.logInfo("PARSER", "Parser for mime type '" + mimeType + "': " + parser.getName());
         }
         
         if (prototypeMime != null) for (String ext: parser.supportedExtensions()) {
             ext = ext.toLowerCase();
             String s = ext2mime.get(ext);
             if (s != null) log.logSevere("parser for extension '" + ext + "' was set to mime '" + s + "', overwriting with new mime '" + prototypeMime + "'.");
             ext2mime.put(ext, prototypeMime);
         }
         
         for (String ext: parser.supportedExtensions()) {
             // process the extensions
             ext = ext.toLowerCase();
             Idiom p0 = ext2parser.get(ext);
             if (p0 != null) log.logSevere("parser for extension '" + ext + "' was set to '" + p0.getName() + "', overwriting with new parser '" + parser.getName() + "'.");
             ext2parser.put(ext, parser);
             Log.logInfo("PARSER", "Parser for extension '" + ext + "': " + parser.getName());
         }
     }
     
     public static Document parseSource(
             final DigestURI location,
             final String mimeType,
             final String charset,
             final File sourceFile
         ) throws InterruptedException, ParserException {
 
         BufferedInputStream sourceStream = null;
         try {
             if (log.isFine()) log.logFine("Parsing '" + location + "' from file");
             if (!sourceFile.exists() || !sourceFile.canRead() || sourceFile.length() == 0) {
                 final String errorMsg = sourceFile.exists() ? "Empty resource file." : "No resource content available (2).";
                 log.logInfo("Unable to parse '" + location + "'. " + errorMsg);
                 throw new ParserException(errorMsg, location);
             }
             sourceStream = new BufferedInputStream(new FileInputStream(sourceFile));
             return parseSource(location, mimeType, charset, sourceFile.length(), sourceStream);
         } catch (final Exception e) {
             if (e instanceof InterruptedException) throw (InterruptedException) e;
             if (e instanceof ParserException) throw (ParserException) e;
             log.logSevere("Unexpected exception in parseSource from File: " + e.getMessage(), e);
             throw new ParserException("Unexpected exception: " + e.getMessage(), location);
         } finally {
             if (sourceStream != null)try {
                 sourceStream.close();
             } catch (final Exception ex) {}
         }
     }
     
     public static Document parseSource(
             final DigestURI location,
             String mimeType,
             final String charset,
             final byte[] content
         ) throws InterruptedException, ParserException {
         return parseSource(location, mimeType, charset, content.length, new ByteArrayInputStream(content));
     }
     
     public static Document parseSource(
             final DigestURI location,
             String mimeType,
             final String charset,
             final long contentLength,
             final InputStream sourceStream
         ) throws InterruptedException, ParserException {
         if (log.isFine()) log.logFine("Parsing '" + location + "' from stream");
         mimeType = normalizeMimeType(mimeType);
         List<Idiom> idioms = null;
         try {
             idioms = idiomParser(location, mimeType);
         } catch (ParserException e) {
             final String errorMsg = "Parser Failure for extension '" + location.getFileExtension() + "' or mimetype '" + mimeType + "': " + e.getMessage();
             log.logWarning(errorMsg);
             throw new ParserException(errorMsg, location);
         }
         assert !idioms.isEmpty();
         
         // if we do not have more than one parser or the content size is over MaxInt
         // then we use only one stream-oriented parser.
         if (idioms.size() == 1 || contentLength > Integer.MAX_VALUE) {
             // use a specific stream-oriented parser
             return parseSource(location, mimeType, idioms.get(0), charset, contentLength, sourceStream);
         }
         
         // in case that we know more parsers we first transform the content into a byte[] and use that as base
         // for a number of different parse attempts.
         try {
             return parseSource(location, mimeType, idioms, charset, FileUtils.read(sourceStream, (int) contentLength));
         } catch (IOException e) {
             throw new ParserException(e.getMessage(), location);
         }
     }
 
     private static Document parseSource(
             final DigestURI location,
             String mimeType,
             Idiom idiom,
             final String charset,
             final long contentLength,
             final InputStream sourceStream
         ) throws InterruptedException, ParserException {
         if (log.isFine()) log.logFine("Parsing '" + location + "' from stream");
         final String fileExt = location.getFileExtension();
         final String documentCharset = htmlParser.patchCharsetEncoding(charset);
         assert idiom != null;
 
         if (log.isFine()) log.logInfo("Parsing " + location + " with mimeType '" + mimeType + "' and file extension '" + fileExt + "'.");
         idiom.setContentLength(contentLength);
         try {
             return idiom.parse(location, mimeType, documentCharset, sourceStream);
         } catch (ParserException e) {
             throw new ParserException("parser failed: " + idiom.getName(), location);
         }
     }
 
     private static Document parseSource(
             final DigestURI location,
             String mimeType,
             List<Idiom> idioms,
             final String charset,
             final byte[] sourceArray
         ) throws InterruptedException, ParserException {
         final String fileExt = location.getFileExtension();
         if (log.isFine()) log.logInfo("Parsing " + location + " with mimeType '" + mimeType + "' and file extension '" + fileExt + "' from byte[]");
         final String documentCharset = htmlParser.patchCharsetEncoding(charset);
         assert !idioms.isEmpty();
 
         Document doc = null;
         HashMap<Idiom, ParserException> failedParser = new HashMap<Idiom, ParserException>();
         for (Idiom parser: idioms) {
             parser.setContentLength(sourceArray.length);
             try {
                 doc = parser.parse(location, mimeType, documentCharset, new ByteArrayInputStream(sourceArray));
             } catch (ParserException e) {
                 failedParser.put(parser, e);
                 //log.logWarning("tried parser '" + parser.getName() + "' to parse " + location.toNormalform(true, false) + " but failed: " + e.getMessage(), e);
             }
             if (doc != null) break;
         }
         
         if (doc == null) {
             if (failedParser.size() == 0) {
                 final String errorMsg = "Parsing content with file extension '" + location.getFileExtension() + "' and mimetype '" + mimeType + "' failed.";
                 //log.logWarning("Unable to parse '" + location + "'. " + errorMsg);
                 throw new ParserException(errorMsg, location);
             } else {
                 String failedParsers = "";
                 for (Map.Entry<Idiom, ParserException> error: failedParser.entrySet()) {
                     log.logWarning("tried parser '" + error.getKey().getName() + "' to parse " + location.toNormalform(true, false) + " but failed: " + error.getValue().getMessage(), error.getValue());
                     failedParsers += error.getKey().getName() + " ";
                 }
                 throw new ParserException("All parser failed: " + failedParsers, location);
             }
         }
         return doc;
     }
     
     /**
      * check if the parser supports the given content.
      * @param url
      * @param mimeType
      * @return returns null if the content is supported. If the content is not supported, return a error string.
      */
     public static String supports(final DigestURI url, String mimeType) {
         try {
             // try to get a parser. If this works, we don't need the parser itself, we just return null to show that everything is ok.
             List<Idiom> idioms = idiomParser(url, mimeType);
             return (idioms == null || idioms.isEmpty()) ? "no parser found" : null;
         } catch (ParserException e) {
             // in case that a parser is not available, return a error string describing the problem.
             return e.getMessage();
         }
     }
     
     /**
      * find a parser for a given url and mime type
      * because mime types returned by web severs are sometimes wrong, we also compute the mime type again
      * from the extension that can be extracted from the url path. That means that there are 3 criteria
      * that can be used to select a parser:
      * - the given extension
      * - the given mime type
      * - the mime type computed from the extension
      * @param url the given url
      * @param mimeType the given mime type
      * @return a list of Idiom parsers that may be appropriate for the given criteria
      * @throws ParserException
      */
     private static List<Idiom> idiomParser(final DigestURI url, String mimeType1) throws ParserException {
         List<Idiom> idioms = new ArrayList<Idiom>(2);
         
         // check extension
         String ext = url.getFileExtension();
         Idiom idiom;
         if (ext != null && ext.length() > 0) {
             ext = ext.toLowerCase();
             if (denyExtensionx.containsKey(ext)) throw new ParserException("file extension '" + ext + "' is denied (1)", url);
             idiom = ext2parser.get(ext);
             if (idiom != null) idioms.add(idiom);
         }
         
         // check given mime type
         if (mimeType1 != null) {
             mimeType1 = normalizeMimeType(mimeType1);
             if (denyMime.containsKey(mimeType1)) throw new ParserException("mime type '" + mimeType1 + "' is denied (1)", url);
             idiom = mime2parser.get(mimeType1);
             if (idiom != null && !idioms.contains(idiom)) idioms.add(idiom);
         }
         
         // check mime type computed from extension
         String mimeType2 = ext2mime.get(ext);
         if (mimeType2 == null || denyMime.containsKey(mimeType2)) return idioms; // in this case we are a bit more lazy
         idiom = mime2parser.get(mimeType2);
         if (idiom != null && !idioms.contains(idiom)) idioms.add(idiom);
         
         // finall check if we found any parser
         if (idioms.isEmpty()) throw new ParserException("no parser found for extension '" + ext + "' and mime type '" + mimeType1 + "'", url);
         
         return idioms;
     }
     
     public static String supportsMime(String mimeType) {
         if (mimeType == null) return null;
         mimeType = normalizeMimeType(mimeType);
         if (denyMime.containsKey(mimeType)) return "mime type '" + mimeType + "' is denied (2)";
         if (mime2parser.get(mimeType) == null) return "no parser for mime '" + mimeType + "' available";
         return null;
     }
     
     public static String supportsExtension(final DigestURI url) {
         String ext = url.getFileExtension().toLowerCase();
         if (ext == null || ext.length() == 0) return null;
         if (denyExtensionx.containsKey(ext)) return "file extension '" + ext + "' is denied (2)";
         String mimeType = ext2mime.get(ext);
         if (mimeType == null) return "no parser available";
         Idiom idiom = mime2parser.get(mimeType);
         assert idiom != null;
         if (idiom == null) return "no parser available (internal error!)";
         return null;
     }
     
     public static String mimeOf(DigestURI url) {
         return mimeOf(url.getFileExtension());
     }
     
     public static String mimeOf(String ext) {
         return ext2mime.get(ext.toLowerCase());
     }
     
     private static String normalizeMimeType(String mimeType) {
         if (mimeType == null) return "application/octet-stream";
         mimeType = mimeType.toLowerCase();
         final int pos = mimeType.indexOf(';');
         return ((pos < 0) ? mimeType.trim() : mimeType.substring(0, pos).trim());
     }
     
     public static void setDenyMime(String denyList) {
         denyMime.clear();
         String n;
         for (String s: denyList.split(",")) {
             n = normalizeMimeType(s);
            if (n != null && n.length() > 0) denyMime.put(n, null);
         }
     }
     
     public static String getDenyMime() {
         String s = "";
         for (String d: denyMime.keySet()) s += d + ",";
         if (s.length() > 0) s = s.substring(0, s.length() - 1);
         return s;
     }
     
     public static void grantMime(String mime, boolean grant) {
         String n = normalizeMimeType(mime);
         if (n == null || n.length() == 0) return;
        if (grant) denyMime.remove(n); else denyMime.put(n, null);
     }
     
     public static void setDenyExtension(String denyList) {
         denyExtensionx.clear();
        for (String s: denyList.split(",")) denyExtensionx.put(s, null);
     }
     
     public static String getDenyExtension() {
         String s = "";
         for (String d: denyExtensionx.keySet()) s += d + ",";
         s = s.substring(0, s.length() - 1);
         return s;
     }
     
     public static void grantExtension(String ext, boolean grant) {
        if (grant) denyExtensionx.remove(ext); else denyExtensionx.put(ext, null);
     }
 }
