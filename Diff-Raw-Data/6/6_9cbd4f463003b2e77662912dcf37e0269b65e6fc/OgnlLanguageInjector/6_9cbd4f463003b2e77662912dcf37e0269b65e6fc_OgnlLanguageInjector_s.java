 /*
  * Copyright 2011 The authors
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.intellij.lang.ognl;
 
 import com.intellij.lang.injection.MultiHostRegistrar;
 import com.intellij.openapi.util.TextRange;
 import com.intellij.psi.PsiLanguageInjectionHost;
 
 /**
  * Injects OGNL language.
  *
  * @author Yann C&eacute;bron
  */
 public class OgnlLanguageInjector {
 
   private final MultiHostRegistrar registrar;
   private final PsiLanguageInjectionHost element;
 
   private OgnlLanguageInjector(final MultiHostRegistrar registrar,
                                final PsiLanguageInjectionHost element) {
     this.registrar = registrar;
     this.element = element;
   }
 
   public static void injectElement(final MultiHostRegistrar registrar,
                                    final PsiLanguageInjectionHost element) {
     new OgnlLanguageInjector(registrar, element).injectWholeXmlAttributeValue();
   }
 
   public static void injectOccurrences(final MultiHostRegistrar registrar,
                                        final PsiLanguageInjectionHost element) {
     new OgnlLanguageInjector(registrar, element).injectOccurrences();
   }
 
   private void injectWholeXmlAttributeValue() {
     final TextRange range = new TextRange(1, element.getTextLength() - 1);
     registrar.startInjecting(OgnlLanguage.INSTANCE)
              .addPlace(null, null, element, range)
              .doneInjecting();
   }
 
   private void injectOccurrences() {
     registrar.startInjecting(OgnlLanguage.INSTANCE);
 
     final String text = element.getText();
     final int textLength = text.length() - 1;
     final int lastStartPosition = Math.max(textLength, text.lastIndexOf(OgnlLanguage.EXPRESSION_SUFFIX));
 
     int startOffset = 0;
     while (startOffset < lastStartPosition) {
       startOffset = text.indexOf(OgnlLanguage.EXPRESSION_PREFIX, startOffset);
       if (startOffset == -1) {
         break;
       }
 
      final int closingBraceIdx = text.indexOf(OgnlLanguage.EXPRESSION_SUFFIX, startOffset);
       final int length = (closingBraceIdx != -1 ? closingBraceIdx + 1 : textLength) - startOffset;
       final TextRange range = TextRange.from(startOffset, length);
       registrar.addPlace(null, null, element, range);
       startOffset = startOffset + length;
     }
 
     registrar.doneInjecting();
   }
 
 }
