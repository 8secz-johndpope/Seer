 //--------------------------------------------------------------------------
 //  Copyright (c) 2004, Drew Davidson and Luke Blanshard
 //  All rights reserved.
 //
 //  Redistribution and use in source and binary forms, with or without
 //  modification, are permitted provided that the following conditions are
 //  met:
 //
 //  Redistributions of source code must retain the above copyright notice,
 //  this list of conditions and the following disclaimer.
 //  Redistributions in binary form must reproduce the above copyright
 //  notice, this list of conditions and the following disclaimer in the
 //  documentation and/or other materials provided with the distribution.
 //  Neither the name of the Drew Davidson nor the names of its contributors
 //  may be used to endorse or promote products derived from this software
 //  without specific prior written permission.
 //
 //  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 //  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 //  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 //  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 //  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 //  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 //  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 //  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 //  AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 //  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 //  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 //  DAMAGE.
 //--------------------------------------------------------------------------
 package org.ognl.test;
 
 import junit.framework.TestSuite;
 import org.ognl.test.objects.Root;
 import org.ognl.test.objects.Simple;
 
 public class StaticsAndConstructorsTest extends OgnlTestCase
 {
     public static final String KEY = "size";
 
     private static Root ROOT = new Root();
 
     public static class IntWrapper {
         public IntWrapper(int value)
         {
             this.value = value;
         }
 
         private final int value;
 
         public String toString()
         {
             return Integer.toString(value);
         }
 
         public boolean equals(Object o)
         {
             if (this == o)
                 return true;
             if (o == null || getClass() != o.getClass())
                 return false;
 
             IntWrapper that = (IntWrapper) o;
 
             return value == that.value;
         }
     }
 
     public static class IntObjectWrapper {
 
         public IntObjectWrapper(Integer value)
         {
             this.value = value;
         }
 
         private final Integer value;
 
         public String toString()
         {
             return value.toString();
         }
 
         public boolean equals(Object o)
         {
             if (this == o)
                 return true;
             if (o == null || getClass() != o.getClass())
                 return false;
 
             IntObjectWrapper that = (IntObjectWrapper) o;
 
             return value.equals(that.value);
         }
     }
 
     private static Object[][]       TESTS = {
             { "@java.lang.Class@forName(\"java.lang.Object\")", Object.class },
             { "@java.lang.Integer@MAX_VALUE", new Integer(Integer.MAX_VALUE) },
             { "@@max(3,4)", new Integer(4) },
             { "new java.lang.StringBuffer().append(55).toString()", "55" },
             { "class", ROOT.getClass() },
             { "@org.ognl.test.objects.Root@class", ROOT.getClass() },
             { "class.getName()", ROOT.getClass().getName() },
             { "@org.ognl.test.objects.Root@class.getName()", ROOT.getClass().getName() },
             { "@org.ognl.test.objects.Root@class.name", ROOT.getClass().getName() },
             { "class.getSuperclass()", ROOT.getClass().getSuperclass() },
             { "class.superclass", ROOT.getClass().getSuperclass() },
             { "class.name", ROOT.getClass().getName() },
             { "getStaticInt()", new Integer(Root.getStaticInt()) },
             { "@org.ognl.test.objects.Root@getStaticInt()", new Integer(Root.getStaticInt()) },
             { "new org.ognl.test.objects.Simple(property).getStringValue()", new Simple().getStringValue() },
             { "new org.ognl.test.objects.Simple(map['test'].property).getStringValue()", new Simple().getStringValue() },
             { "map.test.getCurrentClass(@org.ognl.test.StaticsAndConstructorsTest@KEY.toString())", "size stop"},
             { "new org.ognl.test.StaticsAndConstructorsTest$IntWrapper(index)", new IntWrapper(ROOT.getIndex()) },
             { "new org.ognl.test.StaticsAndConstructorsTest$IntObjectWrapper(index)", new IntObjectWrapper(ROOT.getIndex()) },
     };
 
     /*===================================================================
          Public static methods
        ===================================================================*/
     public static TestSuite suite()
     {
         TestSuite       result = new TestSuite();
 
         for (int i = 0; i < TESTS.length; i++) {
             result.addTest(new StaticsAndConstructorsTest((String)TESTS[i][0] + " (" + TESTS[i][1] + ")", ROOT, (String)TESTS[i][0], TESTS[i][1]));
         }
         return result;
     }
 
     /*===================================================================
          Constructors
        ===================================================================*/
     public StaticsAndConstructorsTest()
     {
         super();
     }
 
     public StaticsAndConstructorsTest(String name)
     {
         super(name);
     }
 
     public StaticsAndConstructorsTest(String name, Object root, String expressionString, Object expectedResult, Object setValue, Object expectedAfterSetResult)
     {
         super(name, root, expressionString, expectedResult, setValue, expectedAfterSetResult);
     }
 
     public StaticsAndConstructorsTest(String name, Object root, String expressionString, Object expectedResult, Object setValue)
     {
         super(name, root, expressionString, expectedResult, setValue);
     }
 
     public StaticsAndConstructorsTest(String name, Object root, String expressionString, Object expectedResult)
     {
         super(name, root, expressionString, expectedResult);
     }
 }
