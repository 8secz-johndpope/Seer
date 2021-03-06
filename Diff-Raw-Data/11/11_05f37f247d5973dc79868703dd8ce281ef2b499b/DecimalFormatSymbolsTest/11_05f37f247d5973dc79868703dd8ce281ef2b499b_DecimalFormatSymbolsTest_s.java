 /*
  * Copyright (C) 2010 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package libcore.java.text;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.text.DecimalFormatSymbols;
 import java.util.Locale;
 
 public class DecimalFormatSymbolsTest extends junit.framework.TestCase {
     private void checkLocaleIsEquivalentToRoot(Locale locale) {
         DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);
        assertEquals(DecimalFormatSymbols.getInstance(Locale.ROOT), dfs);
     }
     public void test_getInstance_unknown_or_invalid_locale() throws Exception {
        // TODO: we fail these tests because ROOT has "INF" for infinity but 'dfs' has "\u221e".
        // On the RI, ROOT has "\u221e" too, but DecimalFormatSymbols.equals appears to be broken;
        // it returns false for objects that -- if you compare their externally visible state --
        // are equal. It could be that they're accidentally checking the Locale.
         checkLocaleIsEquivalentToRoot(new Locale("xx", "XX"));
         checkLocaleIsEquivalentToRoot(new Locale("not exist language", "not exist country"));
     }
 }
