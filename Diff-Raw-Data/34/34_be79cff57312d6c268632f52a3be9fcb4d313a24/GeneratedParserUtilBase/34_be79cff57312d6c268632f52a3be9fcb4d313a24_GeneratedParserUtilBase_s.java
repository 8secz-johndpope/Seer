 /*
  * Copyright 2011-2011 Gregory Shrago
  *
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
 package org.intellij.grammar.parser;
 
 import com.intellij.lang.LighterASTNode;
 import com.intellij.lang.PsiBuilder;
 import com.intellij.openapi.diagnostic.Logger;
 import com.intellij.openapi.util.Key;
 import com.intellij.openapi.util.Pair;
 import com.intellij.psi.tree.IElementType;
 import com.intellij.util.containers.LimitedPool;
 import org.intellij.grammar.BnfParserDefinition;
 import org.intellij.grammar.psi.BnfTypes;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.ListIterator;
 import java.util.TreeSet;
 
 /**
  * @author gregsh
  */
 public class GeneratedParserUtilBase {
 
   private static final Logger LOG = Logger.getInstance("org.intellij.grammar.parser.GeneratedParserUtilBase");
 
   public interface Parser {
     boolean parse(PsiBuilder builder);
   }
 
   public static final Parser TOKEN_ADVANCER = new Parser() {
     @Override
     public boolean parse(PsiBuilder builder) {
       if (builder.eof()) return false;
       builder.advanceLexer();
       return true;
     }
   };
 
   public static final Parser TRUE_CONDITION = new Parser() {
     @Override
     public boolean parse(PsiBuilder builder) {
       return true;
     }
   };
 
   public static boolean consumeToken(PsiBuilder builder, IElementType token) {
     ErrorState state = ErrorState.get(builder);
     IElementType tokenType = builder.getTokenType();
     if (!state.suppressErrors) {
       addVariant(state, builder, getTokenDescription(token));
     }
     if (token == tokenType) {
       builder.advanceLexer();
       return true;
     }
     return false;
   }
 
   private static String getTokenDescription(IElementType token) {
     return '\'' + token.toString() +'\'';
   }
 
   public static void addVariant(PsiBuilder builder, String text) {
     addVariant(ErrorState.get(builder), builder, text);
   }
 
   private static void addVariant(ErrorState state, PsiBuilder builder, String text) {
     addVariant(state, builder.getCurrentOffset(), text, state.predicateSign);
   }
 
   private static void addVariant(ErrorState state, int offset, String text, boolean predicateSign) {
     final Iterator<Variant> it = state.variants.descendingIterator();
     while (it.hasNext()) {
       Variant v = it.next();
       if (v.text.equals(text) && predicateSign == v.expected && offset == v.offset) return;
       if (v.offset < offset) break;
     }
     Variant variant = state.VARIANTS.alloc();
     variant.init(offset, text, predicateSign);
     state.variants.add(variant);
   }
 
   public static boolean consumeToken(PsiBuilder builder, String text) {
     ErrorState state = ErrorState.get(builder);
     String tokenText = builder.getTokenText();
     if (!state.suppressErrors) {
       addVariant(state, builder, text);
     }
     if (text.equals(tokenText)) {
       builder.advanceLexer();
       return true;
     }
     return false;
   }
 
   public static final String _SECTION_NOT_ = "_SECTION_NOT_";
   public static final String _SECTION_AND_ = "_SECTION_AND_";
   public static final String _SECTION_RECOVER_ = "_SECTION_RECOVER_";
   public static final String _SECTION_GENERAL_ = "_SECTION_GENERAL_";
 
   public static void enterErrorRecordingSection(PsiBuilder builder_, int key, @NotNull String sectionType) {
     ErrorState state = ErrorState.get(builder_);
     state.levelCheck.add(new Frame(builder_.getCurrentOffset(), key, sectionType));
     if (sectionType == _SECTION_AND_) {
       if (state.predicateCount == 0 && !state.predicateSign) {
         throw new AssertionError("Incorrect false predicate sign");
       }
       state.predicateCount++;
     }
     else if (sectionType == _SECTION_NOT_) {
       if (state.predicateCount == 0) {
         state.predicateSign = false;
       }
       else {
         state.predicateSign = !state.predicateSign;
       }
       state.predicateCount++;
     }
   }
 
   public static boolean exitErrorRecordingSection(PsiBuilder builder_,
                                                   boolean result,
                                                   int key,
                                                   boolean pinned,
                                                   @NotNull String sectionType,
                                                   @Nullable Parser eatMore) {
     ErrorState state = ErrorState.get(builder_);
 
     Frame frame = null;
     if (state.levelCheck.isEmpty() || key != (frame = state.levelCheck.removeLast()).level || !sectionType.equals(frame.section)) {
       LOG.error("Unbalanced error section: got " + new Frame(builder_.getCurrentOffset(), key, sectionType) + ", expected " + frame);
       return result;
     }
     if (sectionType == _SECTION_AND_ || sectionType == _SECTION_NOT_) {
       state.predicateCount--;
       if (sectionType == _SECTION_NOT_) state.predicateSign = !state.predicateSign;
       return result;
     }
     if (sectionType == _SECTION_RECOVER_  && !builder_.eof() && !state.suppressErrors && eatMore != null) {
       state.suppressErrors = true;
       final LighterASTNode latestDoneMarker = result || pinned ? builder_.getLatestDoneMarker() : null;
       PsiBuilder.Marker extensionMarker = null;
       IElementType extensionTokenType = null;
       try {
         if (latestDoneMarker instanceof PsiBuilder.Marker) {
           extensionMarker = ((PsiBuilder.Marker)latestDoneMarker).precede();
           extensionTokenType = latestDoneMarker.getTokenType();
           ((PsiBuilder.Marker)latestDoneMarker).drop();
         }
         boolean eatMoreFlag = eatMore.parse(builder_);
         // advance to the last error pos
         final int lastErrorPos = state.variants.isEmpty()? builder_.getCurrentOffset() : state.variants.last().offset;
         while (eatMoreFlag && builder_.getCurrentOffset() < lastErrorPos) {
           builder_.advanceLexer();
           eatMoreFlag = eatMore.parse(builder_);
         }
         if (eatMoreFlag) {
           String tokenText = builder_.getTokenText();
           String expectedText = state.getExpectedText(builder_);
           PsiBuilder.Marker mark = builder_.mark();
           try {
             builder_.advanceLexer();
           }
           finally {
             mark.error(expectedText + "got '" + tokenText + "'");
           }
           parseAsTree(builder_, BnfParserDefinition.BNF_DUMMY_BLOCK, true, TOKEN_ADVANCER, eatMore);
         }
         else if (!result && frame.offset != builder_.getCurrentOffset()) {
           String expectedText = state.getExpectedText(builder_);
           builder_.error(expectedText + "got '" + builder_.getTokenText() + "'");
         }
       }
       finally {
         if (extensionMarker != null) {
           extensionMarker.done(extensionTokenType);
         }
         state.suppressErrors = false;
       }
      if (result) {
        state.variantsMax = 0;
        for (Variant v : state.variants) {
          state.VARIANTS.recycle(v);
        }
        state.variants.clear();
       }
     }
     else if (!result && pinned) {
       String expectedText = state.getExpectedText(builder_);
       builder_.error(expectedText + "got '" + builder_.getTokenText() + "'");
     }
 
     return result;
   }
 
 
   private static final Key<ErrorState> ERROR_STATE_KEY = Key.create("ERROR_STATE_KEY");
 
   public static class ErrorState {
     int predicateCount;
     boolean predicateSign = true;
     boolean suppressErrors;
    int variantsMax;
     final LinkedList<Frame> levelCheck = new LinkedList<Frame>();
 
     TreeSet<Variant> variants = new TreeSet<Variant>();
     final LimitedPool<Variant> VARIANTS = new LimitedPool<Variant>(2000, new LimitedPool.ObjectFactory<Variant>() {
       public Variant create() {
         return new Variant();
       }
 
       public void cleanup(final Variant v) {
         v.init(0, null, false);
       }
     });
 
     public static ErrorState get(PsiBuilder builder) {
       ErrorState state = builder.getUserDataUnprotected(ERROR_STATE_KEY);
       if (state == null) {
         builder.putUserDataUnprotected(ERROR_STATE_KEY, state = new ErrorState());
       }
       return state;
     }
 
     public String getExpectedText(PsiBuilder builder) {
       int offset = builder.getCurrentOffset();
       StringBuilder sb = new StringBuilder();
       if (addExpected(sb, offset, true)) {
         sb.append(" expected, ");
       }
       else if (addExpected(sb, offset, false)) sb.append(" unexpected, ");
       return sb.toString();
     }
 
     private boolean addExpected(StringBuilder sb, int offset, boolean expected) {
       int count = 0;
       for (Variant variant : variants) {
         if (offset == variant.offset) {
           if (variant.expected != expected) continue;
           if (count++ > 0) sb.append(", ");
           sb.append(variant.text);
         }
       }
       if (count > 1) {
         int idx = sb.lastIndexOf(",");
         sb.replace(idx, idx + 1, " or");
       }
       return count > 0;
     }
   }
 
   public static class Frame {
     int offset;
     int level;
     String section;
 
     public Frame(int offset, int level, String section) {
       this.offset = offset;
       this.level = level;
       this.section = section;
     }
 
     @Override
     public String toString() {
       return "<"+offset+", "+section+", "+level+">";
     }
   }
 
 
   public static class Variant implements Comparable<Variant>{
     int offset;
     String text;
     boolean expected;
 
     public void init(int offset, String text, boolean expected) {
       this.offset = offset;
       this.text = text;
       this.expected = expected;
     }
 
     @Override
     public String toString() {
       return "<" + offset + ", " + expected + ", " + text + ">";
     }
 
     @Override
     public boolean equals(Object o) {
       if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;
 
       Variant variant = (Variant)o;
 
       if (expected != variant.expected) return false;
       if (offset != variant.offset) return false;
       if (!text.equals(variant.text)) return false;
 
       return true;
     }
 
     @Override
     public int hashCode() {
       int result = offset;
       result = 31 * result + text.hashCode();
       result = 31 * result + (expected ? 1 : 0);
       return result;
     }
 
     @Override
     public int compareTo(Variant o) {
      return offset - o.offset;
     }
   }
 
   private static final int MAX_CHILDREN_IN_TREE = 10;
 
   public static boolean parseAsTree(final PsiBuilder builder, final IElementType chunkType,
                                     final boolean checkParens, final Parser parser, final Parser eatMoreCondition) {
     final LinkedList<Pair<PsiBuilder.Marker, PsiBuilder.Marker>> parenList = new LinkedList<Pair<PsiBuilder.Marker, PsiBuilder.Marker>>();
     final LinkedList<Pair<PsiBuilder.Marker, Integer>> siblingList = new LinkedList<Pair<PsiBuilder.Marker, Integer>>();
     PsiBuilder.Marker marker = null;
 
     final Runnable checkSiblingsRunnable = new Runnable() {
       public void run() {
         main:
         while (!siblingList.isEmpty()) {
           final Pair<PsiBuilder.Marker, PsiBuilder.Marker> parenPair = parenList.peek();
           final int rating = siblingList.getFirst().second;
           int count = 0;
           for (Pair<PsiBuilder.Marker, Integer> pair : siblingList) {
             if (pair.second != rating || parenPair != null && pair.first == parenPair.second) break main;
             if (++count >= MAX_CHILDREN_IN_TREE) {
               final PsiBuilder.Marker parentMarker = pair.first.precede();
               while (count-- > 0) {
                 siblingList.removeFirst();
               }
               parentMarker.done(chunkType);
               siblingList.addFirst(Pair.create(parentMarker, rating + 1));
               continue main;
             }
           }
           break;
         }
       }
     };
 
     int totalCount = 0;
     try {
       int tokenCount = 0;
       while (true) {
         final IElementType tokenType = builder.getTokenType();
         if (checkParens && (tokenType == BnfTypes.BNF_LEFT_PAREN || tokenType == BnfTypes.BNF_RIGHT_PAREN && !parenList.isEmpty())) {
           if (marker != null) {
             marker.done(chunkType);
             siblingList.addFirst(Pair.create(marker, 1));
             marker = null;
             tokenCount = 0;
           }
           if (tokenType == BnfTypes.BNF_LEFT_PAREN) {
             final Pair<PsiBuilder.Marker, Integer> prev = siblingList.peek();
             parenList.addFirst(Pair.create(builder.mark(), prev == null ? null : prev.first));
           }
           checkSiblingsRunnable.run();
           builder.advanceLexer();
           if (tokenType == BnfTypes.BNF_RIGHT_PAREN) {
             final Pair<PsiBuilder.Marker, PsiBuilder.Marker> pair = parenList.removeFirst();
             pair.first.done(chunkType);
             // drop all markers inside parens
             while (!siblingList.isEmpty() && siblingList.getFirst().first != pair.second) {
               siblingList.removeFirst();
             }
             siblingList.addFirst(Pair.create(pair.first, 1));
             checkSiblingsRunnable.run();
           }
         }
         else if (tokenType != null) {
           if (marker == null) {
             marker = builder.mark();
           }
           final boolean result = eatMoreCondition.parse(builder) && parser.parse(builder);
           if (result) {
             tokenCount++;
             totalCount++;
           }
           else {
             break;
           }
         }
         else {
           break;
         }
 
         if (tokenCount >= MAX_CHILDREN_IN_TREE) {
           marker.done(chunkType);
           siblingList.addFirst(Pair.create(marker, 1));
           checkSiblingsRunnable.run();
           marker = null;
           tokenCount = 0;
         }
       }
     }
     finally {
       if (marker != null) {
         marker.drop();
       }
       for (Pair<PsiBuilder.Marker, PsiBuilder.Marker> pair : parenList) {
         pair.first.drop();
       }
     }
     return totalCount != 0;
   }
 }
