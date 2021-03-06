 /**
  * The FreeBSD Copyright
  * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
  * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are
  * met:
  *
  *    1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  *
  *    2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * The views and conclusions contained in the software and documentation
  * are those of the authors and should not be interpreted as representing
  * official policies, either expressed or implied, of the FreeBSD Project.
  */
 package com.helger.as2lib.util;
 
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
 
 import javax.annotation.Nonnull;
 import javax.annotation.concurrent.Immutable;
 
 import com.helger.as2lib.exception.OpenAS2Exception;
 import com.phloc.commons.annotations.ReturnsMutableCopy;
 import com.phloc.commons.microdom.IMicroElement;
 
 @Immutable
 public final class XMLUtil
 {
   private XMLUtil ()
   {}
 
   @Nonnull
   @ReturnsMutableCopy
   public static Map <String, String> getAttrsWithLowercaseName (@Nonnull final IMicroElement aElement)
   {
     final Map <String, String> ret = new HashMap <String, String> ();
    final Map <String, String> aAttrs = aElement.getAllAttributes ();
    if (aAttrs != null)
      for (final Map.Entry <String, String> aEntry : aAttrs.entrySet ())
        ret.put (aEntry.getKey ().toLowerCase (Locale.US), aEntry.getValue ());
     return ret;
   }
 
   @Nonnull
   @ReturnsMutableCopy
   public static Map <String, String> getAttrsWithLowercaseNameWithRequired (@Nonnull final IMicroElement aElement,
                                                                             @Nonnull final String... aRequiredAttributes) throws OpenAS2Exception
   {
     final Map <String, String> aAttributes = getAttrsWithLowercaseName (aElement);
     for (final String sRequiredAttribute : aRequiredAttributes)
       if (!aAttributes.containsKey (sRequiredAttribute))
         throw new OpenAS2Exception (aElement.getTagName () + " is missing required attribute: " + sRequiredAttribute);
     return aAttributes;
   }
 
   @Nonnull
   @ReturnsMutableCopy
   public static Map <String, String> mapAttributeNodes (@Nonnull final IMicroElement aNode,
                                                         final String sNodeName,
                                                         final String sNodeKeyName,
                                                         final String sNodeValueName) throws OpenAS2Exception
   {
     final Map <String, String> ret = new HashMap <String, String> ();
     for (final IMicroElement eChild : aNode.getAllChildElements (sNodeName))
     {
       final String sName = eChild.getAttribute (sNodeKeyName);
       if (sName == null)
         throw new OpenAS2Exception (eChild.toString () + " does not have key attribute: " + sNodeKeyName);
 
       final String sValue = eChild.getAttribute (sNodeValueName);
       if (sValue == null)
         throw new OpenAS2Exception (eChild.toString () + " does not have value attribute: " + sNodeValueName);
 
       ret.put (sName, sValue);
     }
     return ret;
   }
 }
