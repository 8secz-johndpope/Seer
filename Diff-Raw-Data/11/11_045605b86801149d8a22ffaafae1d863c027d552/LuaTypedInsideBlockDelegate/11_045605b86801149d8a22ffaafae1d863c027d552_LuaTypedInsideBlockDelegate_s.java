 /*
  * Copyright 2011 Jon S Akhtar (Sylvanaar)
  *
  *   Licensed under the Apache License, Version 2.0 (the "License");
  *   you may not use this file except in compliance with the License.
  *   You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *   Unless required by applicable law or agreed to in writing, software
  *   distributed under the License is distributed on an "AS IS" BASIS,
  *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *   See the License for the specific language governing permissions and
  *   limitations under the License.
  */
 
 package com.sylvanaar.idea.Lua.lang;
 
 import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
 import com.intellij.openapi.editor.Document;
 import com.intellij.openapi.editor.Editor;
 import com.intellij.openapi.project.Project;
 import com.intellij.psi.PsiDocumentManager;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiFile;
 import com.sylvanaar.idea.Lua.lang.psi.LuaFunctionDefinition;
 
 /**
  * Created by IntelliJ IDEA.
  * User: Jon S Akhtar
  * Date: 1/30/11
  * Time: 6:45 AM
  */
 public class LuaTypedInsideBlockDelegate extends TypedHandlerDelegate {
     @Override
     public Result charTyped(char c, final Project project, final Editor editor, final PsiFile file) {
         Document document = editor.getDocument();
         int caretOffset = editor.getCaretModel().getOffset();
 
         PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
 
         PsiElement e = file.findElementAt(caretOffset-1);
 
         PsiElement e1 = file.findElementAt(caretOffset);
 
         if (c == '(' && e1 !=null && e1.getText().equals(")")) {
            e = e1; c = ')';
         }
 
         if (c==')' && e != null &&  e.getContext() instanceof LuaFunctionDefinition ) {
                document.insertString(e.getTextOffset()+1, " end");
                 return Result.STOP;
         }
         
         return super.charTyped(c, project, editor, file);
     }
 }
