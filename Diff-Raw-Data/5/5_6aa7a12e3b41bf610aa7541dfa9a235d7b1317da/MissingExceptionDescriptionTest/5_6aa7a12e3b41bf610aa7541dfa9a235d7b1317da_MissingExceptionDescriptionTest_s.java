 /* Copyright (c) 2009  Rajarshi Guha <rajarshi.guha@gmail.com>
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  *   Redistributions of source code must retain the above copyright notice, this
  *   list of conditions and the following disclaimer.
  *
  *   Redistributions in binary form must reproduce the above copyright notice,
  *   this list of conditions and the following disclaimer in the documentation
  *   and/or other materials provided with the distribution.
  *
  *   The names of its contributors may not be used to endorse or promote
  *   products derived from this software without specific prior written
  *   permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
  * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  * POSSIBILITY OF SUCH DAMAGE.
  */
 package com.github.ojdcheck.test.content;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import com.github.ojdcheck.test.IClassDocTester;
 import com.github.ojdcheck.test.ITestReport;
 import com.github.ojdcheck.test.TestReport;
 import com.sun.javadoc.ClassDoc;
 import com.sun.javadoc.MethodDoc;
 import com.sun.javadoc.ThrowsTag;
 
 /**
  * Test that verifies that when an exception is thrown, a description of the
  * conditions is provided.
  */
 public class MissingExceptionDescriptionTest implements IClassDocTester {
 
     @Override
     public String getDescription() {
         return "Checks whether a class that throws an" +
                 "exception, describes when the exception is thrown " +
                 "with @throws or @exception";
     }
 
     @Override
     public String getName() {
         return "Missing Exception Description";
     }
 
     
     @Override
     public List<ITestReport> test(ClassDoc classDoc) {
         List<ITestReport> reports = new ArrayList<ITestReport>();
         MethodDoc[] methodDocs = classDoc.methods();
         for (MethodDoc mdoc : methodDocs) {
             ThrowsTag[] ttags = mdoc.throwsTags();
             for (ThrowsTag ttag : ttags) {
                 if (ttag.exceptionComment() == null) {
                     reports.add(
                         new TestReport(this, classDoc, 
                             "Exception was not described",
                             mdoc.position().line(), null
                         )
                     );
                 }
             }
         }
         return reports;
     }
 
     @Override
     public Priority getPriority() {
         return Priority.ERROR;
     }
 
 }
