 /*******************************************************************************
  * Copyright (c) 2006-2013
  * Software Technology Group, Dresden University of Technology
  * DevBoost GmbH, Berlin, Amtsgericht Charlottenburg, HRB 140026
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *   Software Technology Group - TU Dresden, Germany;
  *   DevBoost GmbH - Berlin, Germany
  *      - initial API and implementation
  ******************************************************************************/
 package org.emftext.sdk.codegen.resource.ui.generators.ui;
 
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.LINKED_HASH_MAP;
 import static org.emftext.sdk.codegen.resource.generators.IClassNameConstants.MAP;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.ABSTRACT_HANDLER;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.BAD_LOCATION_EXCEPTION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.BUSY_INDICATOR;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.DISPLAY;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.EXECUTION_EVENT;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.EXECUTION_EXCEPTION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.HANDLER_UTIL;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_DOCUMENT;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_DOCUMENT_EXTENSION_3;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_EDITOR_PART;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_REGION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_SELECTION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_TEXT_OPERATION_TARGET;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_TEXT_SELECTION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.I_TYPED_REGION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.PLATFORM_UI;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.REGION;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.SHELL;
 import static org.emftext.sdk.codegen.resource.ui.UIClassNameConstants.TEXT_UTILITIES;
 
 import org.emftext.sdk.codegen.parameters.ArtifactParameter;
 import org.emftext.sdk.codegen.resource.GenerationContext;
 import org.emftext.sdk.codegen.resource.ui.generators.UIJavaBaseGenerator;
 
 import de.devboost.codecomposers.java.JavaComposite;
 
 public class ToggleCommentHandlerGenerator extends UIJavaBaseGenerator<ArtifactParameter<GenerationContext>> {
 
 	@Override
 	public void generateJavaContents(JavaComposite sc) {
 		sc.add("package " + getResourcePackageName() + ";");sc.addLineBreak();sc.addImportsPlaceholder();
 		sc.addLineBreak();
 		
 		sc.add("public class " + getResourceClassName() + " extends " + ABSTRACT_HANDLER(sc) + " {");
 		sc.addLineBreak();
 		addFields(sc);
 		sc.addLineBreak();
 		addMethods(sc);
 		sc.add("}");
 	}
 
 	private void addFields(JavaComposite sc) {
 		sc.add("public static String[] COMMENT_PREFIXES = new String[] { \"//\" };");
 		sc.addLineBreak();
 		sc.add("private " + I_DOCUMENT(sc) + " document;");
 		sc.add("private " + I_TEXT_OPERATION_TARGET(sc) + " operationTarget;");
 		sc.add("private " + MAP + "<String, String[]> prefixesMap;");
 	}
 	
 	private void addMethods(JavaComposite sc) {
 		addIsEnableMethod(sc);
 		addExecuteMethod(sc);
 		sc.addComment("Parts of the implementation below have been copied from org.eclipse.jdt.internal.ui.javaeditor.ToggleCommentAction.");
 		addIsSelectionCommentedMethod(sc);
 		addGetTextBlockFromSelectionMethod(sc);
 		addGetFirstCompleteLineOfRegionMethod(sc);
 		addIsBlockCommentedMethod(sc);
 	}
 
 	private void addIsEnableMethod(JavaComposite sc) {
 		sc.add("@Override");
 		sc.addLineBreak();
 		sc.add("public boolean isEnabled() {");
 		sc.add(I_EDITOR_PART(sc) + " activeEditor = " + PLATFORM_UI(sc) + ".getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();");
 		sc.add("if (activeEditor instanceof " + editorClassName + ") {");
 		sc.add(I_TEXT_OPERATION_TARGET(sc) + " operationTarget = (" + I_TEXT_OPERATION_TARGET(sc) + ") activeEditor.getAdapter(" + I_TEXT_OPERATION_TARGET(sc) + ".class);");
 		sc.add("return (operationTarget != null && operationTarget.canDoOperation(" + I_TEXT_OPERATION_TARGET(sc) + ".PREFIX) && operationTarget.canDoOperation(" + I_TEXT_OPERATION_TARGET(sc) + ".STRIP_PREFIX));");
 		sc.add("}");
 		sc.add("return false;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addExecuteMethod(JavaComposite sc) {
 		sc.add("public Object execute(" + EXECUTION_EVENT(sc) + " event) throws " + EXECUTION_EXCEPTION(sc) + " {");
 		sc.add(I_EDITOR_PART(sc) + " editorPart = " + HANDLER_UTIL(sc) + ".getActiveEditor(event);");
 		sc.add(editorClassName + " editor = null;");
 		sc.addLineBreak();
 		sc.add("if (editorPart instanceof " + editorClassName + ") {");
 		sc.add("editor = (" + editorClassName + ") editorPart;");
 		sc.add("operationTarget = (" + I_TEXT_OPERATION_TARGET(sc) + ") editorPart.getAdapter(" + I_TEXT_OPERATION_TARGET(sc) + ".class);");
 		sc.add("document = editor.getDocumentProvider().getDocument(editor.getEditorInput());");
 		sc.add("}");
 		sc.add("if (editor == null || operationTarget == null || document == null) {");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 		// TODO Use default prefixes and content types from SourceViewerConfiguration if possible
 		sc.add("prefixesMap = new " + LINKED_HASH_MAP + "<String, String[]>();");
 		sc.add("prefixesMap.put(" + I_DOCUMENT(sc) + ".DEFAULT_CONTENT_TYPE, COMMENT_PREFIXES);");
 		sc.addLineBreak();
		sc.add(I_SELECTION(sc) + " currentSelection = " + HANDLER_UTIL(sc) + ".getCurrentSelection(event);");
 		sc.add("final int operationCode;");
 		sc.add("if (isSelectionCommented(currentSelection)) {");
 		sc.add("operationCode = " + I_TEXT_OPERATION_TARGET(sc) + ".STRIP_PREFIX;");
 		sc.add("} else {");
 		sc.add("operationCode = " + I_TEXT_OPERATION_TARGET(sc) + ".PREFIX;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("if (!operationTarget.canDoOperation(operationCode)) {");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add(SHELL(sc) + " shell = editorPart.getSite().getShell();");
 		sc.add(DISPLAY(sc) + " display = null;");
 		sc.add("if (shell != null && !shell.isDisposed()) {");
 		sc.add("display = shell.getDisplay();");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add(BUSY_INDICATOR(sc) + ".showWhile(display, new  Runnable() {");
 		sc.add("public void run() {");
 		sc.add("operationTarget.doOperation(operationCode);");
 		sc.add("}");
 		sc.add("});");
 		sc.addLineBreak();
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addIsSelectionCommentedMethod(JavaComposite sc) {
 		sc.add("private boolean isSelectionCommented(" + I_SELECTION(sc) + " selection) {");
 		sc.add("if (!(selection instanceof " + I_TEXT_SELECTION(sc) + ")) {");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add(I_TEXT_SELECTION(sc) + " textSelection = (" + I_TEXT_SELECTION(sc) + ") selection;");
 		sc.add("if (textSelection.getStartLine() < 0 || textSelection.getEndLine() < 0) {");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add("try {");
 		sc.add(I_REGION(sc) + " block = getTextBlockFromSelection(textSelection, document);");
 		sc.add(I_TYPED_REGION(sc) + "[] regions = " + TEXT_UTILITIES(sc) + ".computePartitioning(document, " + I_DOCUMENT_EXTENSION_3(sc) + ".DEFAULT_PARTITIONING, block.getOffset(), block.getLength(), false);");
 		sc.add("int[] lines = new int[regions.length * 2]; // [startline, endline, startline, endline, ...]");
 		sc.add("for (int i = 0, j = 0; i < regions.length; i++, j+= 2) {");
 		sc.addComment("start line of region");
 		sc.add("lines[j] = getFirstCompleteLineOfRegion(regions[i], document);");
 		sc.addComment("end line of region");
 		sc.add("int length = regions[i].getLength();");
 		sc.add("int offset = regions[i].getOffset() + length;");
 		sc.add("if (length > 0) {");
 		sc.add("offset--;");
 		sc.add("}");
 		sc.add("lines[j + 1] = (lines[j] == -1 ? -1 : document.getLineOfOffset(offset));");
 		sc.add("}");
 		sc.addComment("Perform the check");
 		sc.add("for (int i = 0, j = 0; i < regions.length; i++, j += 2) {");
 		sc.add("String[] prefixes = prefixesMap.get(regions[i].getType());");
 		sc.add("if (prefixes != null && prefixes.length > 0 && lines[j] >= 0 && lines[j + 1] >= 0) {");
 		sc.add("if (!isBlockCommented(lines[j], lines[j + 1], prefixes, document)) {");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add("return true;");
 		sc.add("} catch (" + BAD_LOCATION_EXCEPTION(sc) + " x) {");
 		sc.addComment("should not happen");
 		sc.add("}");
 		sc.add("return false;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 	
 	private void addGetTextBlockFromSelectionMethod(JavaComposite sc) {
 		sc.add("private " + I_REGION(sc) + " getTextBlockFromSelection(" + I_TEXT_SELECTION(sc) + " selection, " + I_DOCUMENT(sc) + " document) {");
 		sc.add("try {");
 		sc.add(I_REGION(sc) + " line = document.getLineInformationOfOffset(selection.getOffset());");
 		sc.add("int length = selection.getLength() == 0 ? line.getLength() : selection.getLength() + (selection.getOffset() - line.getOffset());");
 		sc.add("return new " + REGION(sc) + "(line.getOffset(), length);");
 		sc.add("} catch (" + BAD_LOCATION_EXCEPTION(sc) + " x) {");
 		sc.addComment("should not happen");
 		sc.add("}");
 		sc.add("return null;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 	
 	private void addGetFirstCompleteLineOfRegionMethod(JavaComposite sc) {
 		sc.add("private int getFirstCompleteLineOfRegion(" + I_REGION(sc) + " region, " + I_DOCUMENT(sc) + " document) {");
 		sc.add("try {");
 		sc.add("final int startLine = document.getLineOfOffset(region.getOffset());");
 		sc.add("int offset = document.getLineOffset(startLine);");
 		sc.add("if (offset >= region.getOffset()) {");
 		sc.add("return startLine;");
 		sc.add("}");
 		sc.add("final int nextLine = startLine + 1;");
 		sc.add("if (nextLine == document.getNumberOfLines()) {");
 		sc.add("return -1;");
 		sc.add("}");
 		sc.add("offset = document.getLineOffset(nextLine);");
 		sc.add("return (offset > region.getOffset() + region.getLength() ? -1 : nextLine);");
 		sc.add("} catch (" + BAD_LOCATION_EXCEPTION(sc) + " x) {");
 		sc.addComment("should not happen");
 		sc.add("}");
 		sc.add("return -1;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 
 	private void addIsBlockCommentedMethod(JavaComposite sc) {
 		sc.add("private boolean isBlockCommented(int startLine, int endLine, String[] prefixes, " + I_DOCUMENT(sc) + " document) {");
 		sc.add("try {");
 		sc.addComment("check for occurrences of prefixes in the given lines");
 		sc.add("for (int i = startLine; i <= endLine; i++) {");
 		sc.add(I_REGION(sc) + " line = document.getLineInformation(i);");
 		sc.add("String text = document.get(line.getOffset(), line.getLength());");
 		sc.add("int[] found = " + TEXT_UTILITIES(sc) + ".indexOf(prefixes, text, 0);");
 		sc.add("if (found[0] == -1) {");
 		sc.addComment("found a line which is not commented");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add("String s = document.get(line.getOffset(), found[0]);");
 		sc.add("s = s.trim();");
 		sc.add("if (s.length() != 0) {");
 		sc.addComment("found a line which is not commented");
 		sc.add("return false;");
 		sc.add("}");
 		sc.add("}");
 		sc.add("return true;");
 		sc.add("} catch (" + BAD_LOCATION_EXCEPTION(sc) + " x) {");
 		sc.addComment("should not happen");
 		sc.add("}");
 		sc.add("return false;");
 		sc.add("}");
 		sc.addLineBreak();
 	}
 }
