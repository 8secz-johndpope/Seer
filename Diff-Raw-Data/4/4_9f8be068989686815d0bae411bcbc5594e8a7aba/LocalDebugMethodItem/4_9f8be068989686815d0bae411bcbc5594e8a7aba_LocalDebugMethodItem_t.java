 /*
  * [The "BSD licence"]
  * Copyright (c) 2009 Ben Gruver
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  * 3. The name of the author may not be used to endorse or promote products
  *    derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
  * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.jf.baksmali.Adaptors;
 
 import org.jf.dexlib.Util.DebugInfoDecoder;
import org.jf.dexlib.Util.Utf8Utils;
 
 public class LocalDebugMethodItem extends DebugMethodItem {
     private DebugInfoDecoder.Local local;
 
     public LocalDebugMethodItem(int offset, String template, int sortOrder, DebugInfoDecoder.Local local) {
         super(offset, template, sortOrder);
         this.local = local;
 
     }
 
     public int getRegister() {
         return local.register;
     }
 
     public String getType() {
         if (local.type == null) {
             return null;
         }
         return local.type.getTypeDescriptor();
     }
 
     public String getName() {
         if (local.name == null) {
             return null;
         }
         return local.name.getStringValue();
     }
 
     public String getSignature() {
         if (local.signature == null) {
             return null;
         }
        return Utf8Utils.escapeString(local.signature.getStringValue());
     }
 }
