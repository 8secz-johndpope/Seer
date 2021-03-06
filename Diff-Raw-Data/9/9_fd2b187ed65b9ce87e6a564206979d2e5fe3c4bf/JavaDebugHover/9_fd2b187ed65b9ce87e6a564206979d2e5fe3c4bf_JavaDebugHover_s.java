 package org.eclipse.jdt.internal.debug.ui;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.debug.core.model.IDebugTarget;
 import org.eclipse.debug.core.model.IValue;
 import org.eclipse.debug.core.model.IVariable;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaType;
 import org.eclipse.jdt.debug.core.IJavaValue;
 import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.ui.IEditorPart;
 
 
 public class JavaDebugHover implements IJavaEditorTextHover {
 		
 	
 	protected IEditorPart fEditor;
 	
 	
 	public JavaDebugHover() {
 	}
 
 	/**
 	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
 	 */
 	public void setEditor(IEditorPart editor) {
 		fEditor= editor;
 	}
 		
 	/**
 	 * @see ITextHover#getHoverRegion(ITextViewer, int)
 	 */
 	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
 		return JavaWordFinder.findWord(textViewer.getDocument(), offset);
 	}
 		
 	/**
 	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
 	 */
 	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
 				
 		DebugPlugin debugPlugin= DebugPlugin.getDefault();
 		if (debugPlugin == null) {
 			return null;
 		}
 			
 		ILaunchManager launchManager= debugPlugin.getLaunchManager();
 		if (launchManager == null) {
 			return null;
 		}
 			
 		IDebugTarget[] targets= launchManager.getDebugTargets();
 		if (targets != null && targets.length > 0) {
 			try {
 				
 				IDocument document= textViewer.getDocument();
 				if (document == null)
 					return null;
 					
 				String variableName= document.get(hoverRegion.getOffset(), hoverRegion.getLength());
 				
 				List javaTargetList = new ArrayList(targets.length);
 				for (int i = 0; i < targets.length; i++) {
 					IJavaDebugTarget javaTarget = (IJavaDebugTarget) targets[i].getAdapter(IJavaDebugTarget.class);
 					if (javaTarget != null) {
 						javaTargetList.add(i, javaTarget);
 					}					
 				}
 												
 				StringBuffer buffer= new StringBuffer();
 				boolean showDebugTarget = javaTargetList.size() > 1;
 				Iterator iterator = javaTargetList.iterator();
 				while (iterator.hasNext()) {
 					IJavaDebugTarget javaTarget = (IJavaDebugTarget) iterator.next();	
 					try {
 						IVariable variable= javaTarget.findVariable(variableName);
 						if (variable != null) {
 							String debugTargetName = showDebugTarget ? javaTarget.getName() : null;
 							appendVariable(buffer, variable, debugTargetName);
 						}
 					} catch (DebugException x) {
 						JDIDebugUIPlugin.log(x);
 					}
 				}
 				
 				if (buffer.length() > 0) {
 					return buffer.toString();
 				}
 			
 			} catch (BadLocationException x) {
 				JDIDebugUIPlugin.log(x);
 			}
 		}
 
 		return null;
 	}
 
 	/**
 	 * A variable gets one line for each debug target it appears in.
 	 */
 	private static void appendVariable(StringBuffer buffer, IVariable variable, String debugTargetName) throws DebugException {
 
 		buffer.append("<p>"); //$NON-NLS-1$
 		if (debugTargetName != null) {
 			buffer.append('[' + debugTargetName + "]&nbsp;"); //$NON-NLS-1$ 
 		}
 		buffer.append(makeHTMLSafe(variable.getName()));
 		buffer.append(" ="); //$NON-NLS-1$
 		
 		String type= getTypeName(variable);
 		String value= "<b>" + makeHTMLSafe(variable.getValue().getValueString().trim()) + "</b>"; //$NON-NLS-1$ //$NON-NLS-2$
 		
 		if (type == null) {
 			buffer.append(" null"); //$NON-NLS-1$
 		} else if (type.equals("java.lang.String")) { //$NON-NLS-1$
 			buffer.append(" \""); //$NON-NLS-1$
 			buffer.append(value);
 			buffer.append('"');
 		} else if (type.equals("boolean")) { //$NON-NLS-1$
 			buffer.append(' ');
 			buffer.append(value);
 		} else {
 			buffer.append(" ("); //$NON-NLS-1$
 			buffer.append(makeHTMLSafe(type));
 			buffer.append(") "); //$NON-NLS-1$
 			buffer.append(value);			
 		}		
 		buffer.append("</p>"); //$NON-NLS-1$
 	}
 
 	private static String getTypeName(IVariable variable) throws DebugException {
 		IValue value= variable.getValue();
 		if (value instanceof IJavaValue) {
 			IJavaType type= ((IJavaValue) value).getJavaType();
 			if (type == null) {
 				return null;
 			}			
 			return type.getName();
 		}
 		return value.getReferenceTypeName();
 	}
 	
 	/**
 	 * Replace any characters in the given String that would confuse an HTML 
 	 * parser with their escape sequences.
 	 */
 	private static String makeHTMLSafe(String string) {
 		StringBuffer buffer= new StringBuffer(string.length());
 	
 		for (int i= 0; i != string.length(); i++) {
 			char ch= string.charAt(i);
 			
 			switch (ch) {
 				case '&':
 					buffer.append("&amp;"); //$NON-NLS-1$
 					break;
 					
 				case '<':
 					buffer.append("&lt;"); //$NON-NLS-1$
 					break;
 
 				case '>':
 					buffer.append("&gt;"); //$NON-NLS-1$
 					break;
				/*
				case '\t':
					buffer.append("    "); //$NON-NLS-1$
					break;
 
				case '\n':
					buffer.append("<br>"); //$NON-NLS-1$
					break;
				*/
 				default:
 					buffer.append(ch);
 					break;
 			}
 		}
 
 		return buffer.toString();		
 	}
 
 }
