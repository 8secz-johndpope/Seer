 /*
  * 03/21/2010
  *
  * Copyright (C) 2010 Robert Futrell
  * robert_futrell at users.sourceforge.net
  * http://fifesoft.com/rsyntaxtextarea
  *
  * This code is licensed under the LGPL.  See the "license.txt" file included
  * with this project.
  */
 package org.fife.rsta.ac.java;
 
 import java.awt.Color;
 import java.awt.Graphics;
 import java.io.File;
 import java.util.Iterator;
 import javax.swing.Icon;
 
 import org.fife.rsta.ac.java.classreader.AccessFlags;
 import org.fife.rsta.ac.java.classreader.ClassFile;
 import org.fife.rsta.ac.java.rjc.ast.CompilationUnit;
 import org.fife.rsta.ac.java.rjc.ast.TypeDeclaration;
 import org.fife.ui.autocomplete.CompletionProvider;
 
 
 /**
  * Completion for a Java class or interface.
  *
  * @author Robert Futrell
  * @version 1.0
  */
 class ClassCompletion extends AbstractJavaSourceCompletion {
 
 	private ClassFile cf;
 
 
 	public ClassCompletion(CompletionProvider provider, ClassFile cf) {
 		super(provider, cf.getClassName(false));
 		this.cf = cf;
 	}
 
 
 	public boolean equals(Object obj) {
 		return (obj instanceof ClassCompletion) &&
 			((ClassCompletion)obj).getReplacementText().equals(getReplacementText());
 	}
 
 
 	public Icon getIcon() {
 
 		// TODO: Add functionality to ClassFile to make this simpler.
 
 		boolean isInterface = false;
 		boolean isPublic = false;
 		//boolean isProtected = false;
 		//boolean isPrivate = false;
 		boolean isDefault = false;
 
 		int access = cf.getAccessFlags();
 		if ((access&AccessFlags.ACC_INTERFACE)>0) {
 			isInterface = true;
 		}
 
 		else if (org.fife.rsta.ac.java.classreader.Util.isPublic(access)) {
 			isPublic = true;
 		}
 //		else if (org.fife.rsta.ac.java.classreader.Util.isProtected(access)) {
 //			isProtected = true;
 //		}
 //		else if (org.fife.rsta.ac.java.classreader.Util.isPrivate(access)) {
 //			isPrivate = true;
 //		}
 		else {
 			isDefault = true;
 		}
 
 		IconFactory fact = IconFactory.get();
 		String key = null;
 
 		if (isInterface) {
 			if (isDefault) {
 				key = IconFactory.DEFAULT_INTERFACE_ICON;
 			}
 			else {
 				key = IconFactory.INTERFACE_ICON;
 			}
 		}
 		else {
 			if (isDefault) {
 				key = IconFactory.DEFAULT_CLASS_ICON;
 			}
 			else if (isPublic) {
 				key = IconFactory.CLASS_ICON;
 			}
 		}
 
 		return fact.getIcon(key);
 
 	}
 
 
 	public String getSummary() {
 
 		SourceCompletionProvider scp = (SourceCompletionProvider)getProvider();
 		File loc = scp.getSourceLocForClass(cf.getClassName(true));
 
 		if (loc!=null) {
 
 			CompilationUnit cu = Util.getCompilationUnitFromDisk(loc, cf);
 			if (cu!=null) {
 				for (Iterator i=cu.getTypeDeclarationIterator(); i.hasNext(); ) {
 					TypeDeclaration td = (TypeDeclaration)i.next();
 					String typeName = td.getName();
 					// Avoid inner classes, etc.
 					if (typeName.equals(cf.getClassName(false))) {
 						String summary = td.getDocComment();
 						// Be cautious - might be no doc comment (or a bug?)
 						if (summary!=null && summary.startsWith("/**")) {
 							return Util.docCommentToHtml(summary);
 						}
 					}
 				}
 			}
 
 		}
 
 		// Default to the fully-qualified class name.
 		return cf.getClassName(true);
 
 	}
 
 
 	public String getToolTipText() {
 		return "class " + getReplacementText();
 	}
 
 
 	public int hashCode() {
 		return getReplacementText().hashCode();
 	}
 
 
 	public void rendererText(Graphics g, int x, int y, boolean selected) {
 
 		StringBuffer sb = new StringBuffer();
 		sb.append(cf.getClassName(false));
 		sb.append(" - ");
 		String s = sb.toString();
 		g.drawString(s, x, y);
 		x += g.getFontMetrics().stringWidth(s);
 
 		String pkgName = cf.getClassName(true);
		int lastIndexOf = pkgName.lastIndexOf('.');
		if (lastIndexOf != -1) { // Class may not be in a package
			pkgName = pkgName.substring(0, lastIndexOf);
			Color origColor = g.getColor();
			if (!selected) {
				g.setColor(Color.GRAY);
			}
			g.drawString(pkgName, x, y);
			if (!selected) {
				g.setColor(origColor);
			}
 		}
 
 	}
 
 
 }
