 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jface.text.source.projection;
 
 
 import java.util.Iterator;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.MouseMoveListener;
 import org.eclipse.swt.events.MouseTrackAdapter;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.Position;
 import org.eclipse.jface.text.source.AnnotationRulerColumn;
 import org.eclipse.jface.text.source.CompositeRuler;
 import org.eclipse.jface.text.source.IAnnotationAccess;
 import org.eclipse.jface.text.source.IAnnotationModel;
 import org.eclipse.jface.text.source.IAnnotationModelExtension;
 
 
 /**
  * A ruler column for controlling the behavior of a
  * <code>ProjectionSourceViewer</code>.
  * <p>
  * Internal class. Do not use. Public only for testing purposes.
  * 
  * @since 3.0
  */
 class ProjectionRulerColumn extends AnnotationRulerColumn {
 	
 	private ProjectionAnnotation fCurrentAnnotation;
 
 	/**
 	 * Creates a new projection ruler column.
 	 * 
 	 * @param model the column's annotation model
 	 * @param width the width in pixels
 	 * @param annotationAccess the annotation access
 	 */
 	public ProjectionRulerColumn(IAnnotationModel model, int width, IAnnotationAccess annotationAccess) {
 		super(model, width, annotationAccess);
 	}
 	
 	/**
 	 * Creates a new projection ruler column.
 	 * 
 	 * @param width the width in pixels
 	 * @param annotationAccess the annotation access
 	 */
 	public ProjectionRulerColumn(int width, IAnnotationAccess annotationAccess) {
 		super(width, annotationAccess);
 	}
 	
 	/*
 	 * @see org.eclipse.jface.text.source.AnnotationRulerColumn#mouseClicked(int)
 	 */
 	protected void mouseClicked(int line) {
 		clearCurrentAnnotation();
 		ProjectionAnnotation annotation= findAnnotation(line);
 		if (annotation != null) {
 			ProjectionAnnotationModel model= (ProjectionAnnotationModel) getModel();
 			model.toggleExpansionState(annotation);
 		}
 	}
 	
 	/**
 	 * Returns the projection annotation of the column's annotation
 	 * model that contains the given line.
 	 * 
 	 * @param line the line
 	 * @return the projection annotation containing the given line
 	 */
 	private ProjectionAnnotation findAnnotation(int line) {
 		
 		ProjectionAnnotation previousAnnotation= null;
 		
 		IAnnotationModel model= getModel();
 		if (model != null) {
 			IDocument document= getCachedTextViewer().getDocument();
 			
 			int previousDistance= Integer.MAX_VALUE;
 			
 			Iterator e= model.getAnnotationIterator();
 			while (e.hasNext()) {
 				Object next= e.next();
 				if (next instanceof ProjectionAnnotation) {
 					ProjectionAnnotation annotation= (ProjectionAnnotation) next;
 					Position p= model.getPosition(annotation);
 					
 					int distance= getDistance(p, document, line);
 					if (distance == -1)
 						continue;
 					
 					if (distance < previousDistance) {
 						previousAnnotation= annotation;
 						previousDistance= distance;
 					}
 				}
 			}
 			
 		}
 		
 		return previousAnnotation;
 	}
 	
 	/**
 	 * Returns the distance of the given line to the the start line of the given position in the given document. The distance is  
 	 * <code>-1</code> when the line is not included in the given position.
 	 * 
 	 * @param position the position
 	 * @param document the document
 	 * @param line the line
 	 * @return <code>-1</code> if line is not contained, a position number otherwise
 	 */
 	private int getDistance(Position position, IDocument document, int line) {
 		if (position.getOffset() > -1 && position.getLength() > -1) {
 			try {
 				int startLine= document.getLineOfOffset(position.getOffset());
 				int endLine= document.getLineOfOffset(position.getOffset() + position.getLength());
 				if (startLine <= line && line < endLine)
 					return line - startLine;
 			} catch (BadLocationException x) {
 			}
 		}
 		return -1;
 	}
 	
 	private boolean clearCurrentAnnotation() {
 		if (fCurrentAnnotation != null) {
 			fCurrentAnnotation.setRangeIndication(false);
 			fCurrentAnnotation= null;
 			return true;
 		}
 		return false;
 	}
 	
 	/*
 	 * @see org.eclipse.jface.text.source.IVerticalRulerColumn#createControl(org.eclipse.jface.text.source.CompositeRuler, org.eclipse.swt.widgets.Composite)
 	 */
 	public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
 		Control control= super.createControl(parentRuler, parentControl);
 		// set background
 		Display display= parentControl.getDisplay();
 		Color background= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
 		control.setBackground(background);
 		// install hover listener
 		control.addMouseTrackListener(new MouseTrackAdapter() {
			public void mouseHover(MouseEvent e) {
				boolean redraw= clearCurrentAnnotation();
				ProjectionAnnotation annotation= findAnnotation(toDocumentLineNumber(e.y));
				if (annotation != null && !annotation.isCollapsed()) {
					annotation.setRangeIndication(true);
					fCurrentAnnotation= annotation;
					redraw= true;
				}
				if (redraw)
					redraw();

			}
 			public void mouseExit(MouseEvent e) {
 				if (clearCurrentAnnotation())
 					redraw();
 			}
 		});
 		// install mouse move listener
 		control.addMouseMoveListener(new MouseMoveListener() {
 			public void mouseMove(MouseEvent e) {
				if (clearCurrentAnnotation())
 					redraw();
 			}
 		});
 		return control;
 	}
 	
 	/*
 	 * @see org.eclipse.jface.text.source.AnnotationRulerColumn#setModel(org.eclipse.jface.text.source.IAnnotationModel)
 	 */
 	public void setModel(IAnnotationModel model) {
 		if (model instanceof IAnnotationModelExtension) {
 			IAnnotationModelExtension extension= (IAnnotationModelExtension) model;
 			model= extension.getAnnotationModel(ProjectionSupport.PROJECTION);
 		}
 		super.setModel(model);
 	}
 }
