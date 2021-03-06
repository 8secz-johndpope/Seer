 /**
  * Copyright (c) 2013, phandom.org
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met: 1) Redistributions of source code must retain the above
  * copyright notice, this list of conditions and the following
  * disclaimer. 2) Redistributions in binary form must reproduce the above
  * copyright notice, this list of conditions and the following
  * disclaimer in the documentation and/or other materials provided
  * with the distribution. 3) Neither the name of the phandom.org nor
  * the names of its contributors may be used to endorse or promote
  * products derived from this software without specific prior written
  * permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
  * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
  * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.phandom;
 
 import com.jcabi.aspects.Immutable;
 import com.jcabi.aspects.Loggable;
 import com.jcabi.log.VerboseProcess;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.logging.Level;
 import java.util.regex.Pattern;
 import javax.validation.constraints.NotNull;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 import lombok.EqualsAndHashCode;
 import lombok.ToString;
 import org.apache.commons.io.Charsets;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang3.CharEncoding;
 import org.w3c.dom.Document;
 import org.xml.sax.SAXException;
 
 /**
  * PhantomJS DOM.
  *
  * <p>Use it to parse XML/XHTML/HTML document using PhantomJS, for example:
  *
  * <pre>Document dom = new Phandom(
  *   "&lt;html&gt;&lt;p&gt;Hey!&lt;/p&gt;&lt;/html&gt;"
  * ).dom();
  * Element element = dom.getElementByTag("p");</pre>
  *
  * <p>The most popular use case for the class would be its usage
  * in a unit test, to make sure your HTML document (together with its
  * embedded JavaScript scripts) is renderable by a browser:
  *
  * <pre>import com.rexsl.test.XhtmlMatchers;
  * import org.hamcrest.MatcherAssert;
  * import org.junit.Assume;
  * import org.junit.Test;
  * import org.phandom.Phandom;
  * public class HtmlTest {
  *   &#64;Test
  *   public void testPageRenderability() {
  *     Assume.assumeTrue(Phandom.isInstalled());
  *     MatcherAssert.assertThat(
  *       new Phandom("&lt;html>&lt;p>Hey!&lt;/p>&lt;/html>").dom(),
  *       XhtmlMatchers.hasXPath("//body/p[.='Hey!']")
  *     );
  *   }
  * }</pre>
  *
  * @author Yegor Bugayenko (yegor@tpc2.com)
  * @version $Id$
  * @since 0.1
  */
 @Immutable
 @ToString
 @EqualsAndHashCode(of = "page")
 @Loggable(Loggable.DEBUG)
 public final class Phandom {
 
     /**
      * Name of binary phantomjs.
      */
     private static final String BIN = "phantomjs";
 
     /**
      * Pattern to match phantomjs version.
      */
     private static final Pattern VERSION =
         Pattern.compile("\\d+\\.\\d+\\.\\d+");
 
     /**
      * Content of the page to render.
      */
     private final transient String page;
 
     /**
      * Public ctor.
      * @param content Content to encapsulate
      */
     public Phandom(@NotNull final String content) {
         this.page = content;
     }
 
     /**
      * Public ctor.
      * @param stream Stream with content
      * @throws IOException If fails to read the stream
      */
     public Phandom(@NotNull final InputStream stream) throws IOException {
         this(IOUtils.toString(stream, CharEncoding.UTF_8));
     }
 
     /**
      * PhantomJS binary is installed?
      * @return TRUE if installed
      * @since 0.2
      */
     public static boolean isInstalled() {
         boolean installed;
         try {
             installed = Phandom.VERSION.matcher(
                 new VerboseProcess(
                     new ProcessBuilder(Phandom.BIN, "--version"),
                     Level.FINE, Level.FINE
                 ).stdoutQuietly()
             ).matches();
        } catch (IllegalStateException ex) {
             installed = false;
         }
         return installed;
     }
 
     /**
      * Get DOM.
      * @return DOM
      * @throws IOException If fails
      */
     public Document dom() throws IOException {
         final Process process = this.builder().start();
         process.getOutputStream().close();
         return Phandom.parse(
             new VerboseProcess(process, Level.FINE, Level.FINE).stdout()
         );
     }
 
     /**
      * Create process builder.
      * @return Builder
      * @throws IOException If fails
      */
     public ProcessBuilder builder() throws IOException {
         final InputStream src = this.getClass().getResourceAsStream("dom.js");
         try {
             return new ProcessBuilder(
                 Phandom.BIN,
                 Phandom.temp(src, ".js").getAbsolutePath(),
                 Phandom.temp(
                     new ByteArrayInputStream(
                         this.page.getBytes(Charsets.UTF_8)
                     ),
                     ".html"
                 ).getAbsolutePath()
             );
         } finally {
             src.close();
         }
     }
 
     /**
      * Parse XML into DOM.
      * @param xml XML to parse
      * @return DOM
      * @throws IOException If fails
      */
     private static Document parse(final String xml) throws IOException {
         try {
             return DocumentBuilderFactory.newInstance()
                 .newDocumentBuilder()
                 .parse(IOUtils.toInputStream(xml, CharEncoding.UTF_8));
        } catch (ParserConfigurationException ex) {
             throw new IOException(xml, ex);
        } catch (SAXException ex) {
             throw new IOException(xml, ex);
         }
     }
 
     /**
      * Make temporary file.
      * @param content Content to save there
      * @param ext Extension
      * @return File name
      * @throws IOException If fails
      */
     private static File temp(final InputStream content, final String ext)
         throws IOException {
         final File file = File.createTempFile("phandom-", ext);
         final OutputStream output = new FileOutputStream(file);
         try {
             IOUtils.copy(content, output);
         } finally {
             output.close();
         }
         return file;
     }
 
 }
