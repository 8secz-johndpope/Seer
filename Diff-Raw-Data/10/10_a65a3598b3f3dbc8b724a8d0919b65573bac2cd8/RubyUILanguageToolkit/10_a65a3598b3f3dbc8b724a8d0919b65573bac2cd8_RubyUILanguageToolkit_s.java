 /*******************************************************************************
  * Copyright (c) 2005, 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  
  *******************************************************************************/
 package org.eclipse.dltk.ruby.internal.ui;
 
 import org.eclipse.dltk.core.IDLTKLanguageToolkit;
 import org.eclipse.dltk.core.IModelElement;
 import org.eclipse.dltk.core.ISourceModule;
 import org.eclipse.dltk.core.IType;
 import org.eclipse.dltk.ruby.core.RubyConstants;
 import org.eclipse.dltk.ruby.core.RubyLanguageToolkit;
 import org.eclipse.dltk.ruby.internal.ui.editor.RubyEditor;
 import org.eclipse.dltk.ruby.internal.ui.preferences.SimpleRubySourceViewerConfiguration;
 import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
 import org.eclipse.dltk.ui.ScriptElementLabels;
 import org.eclipse.dltk.ui.text.ScriptSourceViewerConfiguration;
 import org.eclipse.dltk.ui.text.ScriptTextTools;
 import org.eclipse.dltk.ui.viewsupport.ScriptUILabelProvider;
 import org.eclipse.jface.dialogs.IDialogSettings;
 import org.eclipse.jface.preference.IPreferenceStore;
 
 public class RubyUILanguageToolkit implements IDLTKUILanguageToolkit {
 	private static ScriptElementLabels sInstance = new ScriptElementLabels() {
 		public void getElementLabel(IModelElement element, long flags,
 				StringBuffer buf) {
 			StringBuffer buffer = new StringBuffer(60);
 			super.getElementLabel(element, flags, buffer);
 			String s = buffer.toString();
 			if (s != null && !s.startsWith(element.getElementName())) {
 				if (s.indexOf('$') != -1) {
 					s = s.replaceAll("\\$", ".");
 				}
 			}
 			buf.append(s);
 		}
 
 		protected void getTypeLabel(IType type, long flags, StringBuffer buf) {
 			StringBuffer buffer = new StringBuffer(60);
 			super.getTypeLabel(type, flags, buffer);
 			String s = buffer.toString();
 			if (s.indexOf('$') != -1) {
 				s = s.replaceAll("\\$", "::");
 			}
 			buf.append(s);
 		}
 
 		protected char getTypeDelimiter() {
 			return '$';
 		}
 	};
 
 	private static RubyUILanguageToolkit sToolkit = null;
 
 	public static IDLTKUILanguageToolkit getInstance() {
 		if (sToolkit == null) {
 			sToolkit = new RubyUILanguageToolkit();
 		}
 		return sToolkit;
 	}
 
 	public ScriptElementLabels getScriptElementLabels() {
 		return sInstance;
 	}
 
 	public IPreferenceStore getPreferenceStore() {
 		return RubyUI.getDefault().getPreferenceStore();
 	}
 
 	public IDLTKLanguageToolkit getCoreToolkit() {
 		return RubyLanguageToolkit.getDefault();
 	}
 
 	public IDialogSettings getDialogSettings() {
 		return RubyUI.getDefault().getDialogSettings();
 	}
 
 	public String getPartitioningId() {
 		return RubyConstants.RUBY_PARTITIONING;
 	}
 
 	public String getEditorId(Object inputElement) {
 		return RubyEditor.EDITOR_ID;
 	}
 
 	public String getInterpreterContainerId() {
 		return "org.eclipse.dltk.ruby.launching.INTERPRETER_CONTAINER";
 	}
 
 	public ScriptUILabelProvider createScriptUILabelProvider() {
 		return null;
 	}
 
 	public boolean getProvideMembers(ISourceModule element) {
 		return true;
 	}
 
 	public ScriptTextTools getTextTools() {
 		return RubyUI.getDefault().getTextTools();
 	}
 
 	public ScriptSourceViewerConfiguration createSourceViewerConfiguration() {
 		return new SimpleRubySourceViewerConfiguration(getTextTools()
 				.getColorManager(), getPreferenceStore(), null,
 				getPartitioningId(), false);
 	}
 
 	private static final String INTERPRETERS_PREFERENCE_PAGE_ID = "org.eclipse.dltk.ruby.preferences.interpreters";
 	private static final String DEBUG_PREFERENCE_PAGE_ID = "org.eclipse.dltk.ruby.preferences.debug";
 
 	public String getInterpreterPreferencePage() {
 		return INTERPRETERS_PREFERENCE_PAGE_ID;
 	}
 
 	public String getDebugPreferencePage() {
 		return DEBUG_PREFERENCE_PAGE_ID;
 	}
 }
