 /*******************************************************************************
  * Copyright (c) 2005, 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  
  *******************************************************************************/
 package org.eclipse.dltk.javascript.core;
 
 import org.eclipse.dltk.core.AbstractLanguageToolkit;
 import org.eclipse.dltk.core.IDLTKLanguageToolkit;
 
 public class JavaScriptLanguageToolkit extends AbstractLanguageToolkit {
 	private static JavaScriptLanguageToolkit sInstance = new JavaScriptLanguageToolkit();
 
 	public JavaScriptLanguageToolkit() {
 	}
 
 	public boolean languageSupportZIPBuildpath() {
 		return false;
 	}
 
 	public String getNatureId() {
 		return JavaScriptNature.NATURE_ID;
 	}
 
 	public static IDLTKLanguageToolkit getDefault() {
 		return sInstance;
 	}
 
 	public String getLanguageName() {
 		return "Javascript";
 	}
 
 	public String getLanguageContentType() {
 		return "org.eclipse.dltk.javascriptContentType";
 	}
 }
