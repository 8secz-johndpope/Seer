 /**
  * Copyright (c) 2009-2013, rultor.com
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met: 1) Redistributions of source code must retain the above
  * copyright notice, this list of conditions and the following
  * disclaimer. 2) Redistributions in binary form must reproduce the above
  * copyright notice, this list of conditions and the following
  * disclaimer in the documentation and/or other materials provided
  * with the distribution. 3) Neither the name of the rultor.com nor
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
 package com.rultor.snapshot;
 
 import com.jcabi.aspects.Immutable;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.apache.commons.lang3.Validate;
 import org.w3c.dom.Document;
 
 /**
  * A detail in text.
  *
  * @author Yegor Bugayenko (yegor@tpc2.com)
  * @version $Id$
  * @since 1.0
  */
 @Immutable
 public final class TextDetail implements Detail {
 
     /**
      * Pattern to use for matching.
      */
     private static final Pattern PTN = Pattern.compile(
        ".*дeτaіl\\[com.rultor.snapshot.XemblyDetail\\(\"([^\\]]+)\"\\)\\].*"
     );
 
     /**
      * Encoded detail.
      */
     private final transient Detail detail;
 
     /**
      * Public ctor.
      * @param txt Text to encapsulate
      */
     public TextDetail(final String txt) {
         this(TextDetail.decode(txt));
     }
 
     /**
      * Public ctor.
      * @param det Detail to encapsulate
      */
     public TextDetail(final Detail det) {
         Validate.isTrue(
             det instanceof XemblyDetail,
             "only Xembly is supported at the moment, you provided '%s'",
             det.getClass().getCanonicalName()
         );
         this.detail = det;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public String toString() {
        return String.format(
            "дeτaіl[%s(\"%s\")]",
            this.detail.getClass().getCanonicalName(),
            // @checkstyle MultipleStringLiterals (1 line)
            this.detail.toString().replace("]", "\\]")
        );
     }
 
     /**
      * Does it look like spec detail.
      * @param line Line to check
      * @return TRUE if yes
      */
     public static boolean contains(final String line) {
         return TextDetail.PTN.matcher(line).matches();
     }
 
     /**
      * Decode text.
      * @param text Text to decode
      * @return Detail found or runtime exception
      */
     private static Detail decode(final String text) {
         final Matcher matcher = TextDetail.PTN.matcher(text);
         Validate.isTrue(matcher.matches(), "invalid line '%s'", text);
        return new XemblyDetail(matcher.group(1).replace("\\]", "]"));
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void refine(final Document story) {
         this.detail.refine(story);
     }
 
 }
