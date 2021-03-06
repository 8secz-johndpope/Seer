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
 
 package org.eclipse.jface.text.contentassist;
 
 
 import java.util.Iterator;
 import java.util.Stack;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.BusyIndicator;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.events.VerifyEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableItem;
 
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.text.TextPresentation;
 
 
 /**
  * This class is used to present context information to the user. 
  * If multiple contexts are valid at the current cursor location, 
  * a list is presented from which the user may choose one context. 
  * Once the user makes their choice, or if there was only a single 
  * possible context, the context information is shown in a tooltip like popup. <p>
  * If the tooltip is visible and the user wants to see context information of 
  * a context embedded into the one for which context information is displayed,
  * context information for the embedded context is shown. As soon as the 
  * cursor leaves the embedded context area, the context information for
  * the embedding context is shown again.
  *
  * @see IContextInformation
  * @see IContextInformationValidator
  */
 class ContextInformationPopup implements IContentAssistListener {
 	
 	
 	
 	/**
 	 * Represents the state necessary for embedding contexts.
 	 * @since 2.0
 	 */
 	static class ContextFrame {
 		public int fBeginOffset;
 		public int fOffset;
 		public int fVisibleOffset;
 		public IContextInformation fInformation;
 		public IContextInformationValidator fValidator;
 		public IContextInformationPresenter fPresenter;
 		
 		/*
 		 * @see java.lang.Object#equals(java.lang.Object)
 		 * @since 3.0
 		 */
 		public boolean equals(Object obj) {
 			if (obj instanceof ContextFrame) {
 				ContextFrame frame= (ContextFrame) obj;
 				return fInformation.equals(frame.fInformation) && fBeginOffset == frame.fBeginOffset;
 			}
 			return super.equals(obj);
 		}
 	}
 
 	private ITextViewer fViewer;
 	private ContentAssistant fContentAssistant;
 
 	private PopupCloser fPopupCloser= new PopupCloser();
 	private Shell fContextSelectorShell;
 	private Table fContextSelectorTable;
 	private IContextInformation[] fContextSelectorInput;
 	private String fLineDelimiter= null;
 
 	private Shell fContextInfoPopup;
 	private StyledText fContextInfoText;
 	private TextPresentation fTextPresentation;
 	
 	private Stack fContextFrameStack= new Stack();
 	/**
 	 * The content assist subjects.
 	 * 
 	 * @since 3.0
 	 */
 	private IContentAssistSubject fContentAssistSubject;
 	/**
 	 * The content assist subject adapter.
 	 * 
 	 * @since 3.0
 	 */
 	private ContentAssistSubjectAdapter fContentAssistSubjectAdapter;
 	
 	/**
 	 * Selection listener on the text widget which is active
 	 * while a context information pop up is shown.
 	 * 
 	 * @since 3.0
 	 */
 	private SelectionListener fTextWidgetSelectionListener;
 	
 	/**
 	 * The last removed context frame is remembered in order to not re-query the
 	 * user about which context should be used.
 	 * 
 	 * @since 3.0
 	 */
 	private ContextFrame fLastContext= null;
 	
 	/**
 	 * Creates a new context information popup.
 	 * 
 	 * @param contentAssistant the content assist for computing the context information
 	 * @param viewer the viewer on top of which the context information is shown
 	 */
 	public ContextInformationPopup(ContentAssistant contentAssistant, ITextViewer viewer) {
 		fContentAssistant= contentAssistant;
 		fViewer= viewer;
 		fContentAssistSubjectAdapter= new ContentAssistSubjectAdapter(fViewer);
 	}
 
 	/**
 	 * Creates a new context information popup.
 	 * 
 	 * @param contentAssistant the content assist for computing the context information
 	 * @param contentAssistSubject the content assist subject on top of which the context information is shown
 	 * @since 3.0
 	 */
 	public ContextInformationPopup(ContentAssistant contentAssistant, IContentAssistSubject contentAssistSubject) {
 		fContentAssistant= contentAssistant;
 		fContentAssistSubject= contentAssistSubject;
 		fContentAssistSubjectAdapter= new ContentAssistSubjectAdapter(fContentAssistSubject);
 	}
 
 	/**
 	 * Shows all possible contexts for the given cursor position of the viewer.
 	 * 
 	 * @param autoActivated <code>true</code>  if auto activated
 	 * @return  a potential error message or <code>null</code> in case of no error
 	 */
 	public String showContextProposals(final boolean autoActivated) {
 		final Control control= fContentAssistSubjectAdapter.getControl();
 		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
 			public void run() {
 				
 				int position= fContentAssistSubjectAdapter.getSelectedRange().x;
 				
 				IContextInformation[] contexts= computeContextInformation(position);
 				int count = (contexts == null ? 0 : contexts.length);
 				if (count == 1) {
 					
 					ContextFrame frame= createContextFrame(contexts[0], position);
 					if (isDuplicate(frame))
 						validateContextInformation();
 					else
 						// Show context information directly
 						internalShowContextInfo(frame);
 				
 				} else if (count > 0) {
 					
 					// if any of the proposed context matches the any of the contexts on the stack,
 					// assume that one (so, if context info is invoked repeatedly, the current
 					// info is kept)
 					for (int i= 0; i < contexts.length; i++) {
 						IContextInformation info= contexts[i];
 						ContextFrame frame= createContextFrame(info, position);
 						
 						// check top of stack and stored context
 						if (isDuplicate(frame)) {
 							validateContextInformation();
 							return;
 						}
 						
 						if (isLastFrame(frame)) {
 							internalShowContextInfo(frame);
 							return;
 						}
 						
 						// also check all other contexts
 						for (Iterator it= fContextFrameStack.iterator(); it.hasNext(); ) {
 							ContextFrame stackFrame= (ContextFrame) it.next();
 							if (stackFrame.equals(frame)) {
 								validateContextInformation();
 								return;
 							}
 						}
 					}
 					
 					// otherwise:
 					// Precise context must be selected
 					
 					if (fLineDelimiter == null)
 						fLineDelimiter= fContentAssistSubjectAdapter.getLineDelimiter();
 
 					createContextSelector();
 					setContexts(contexts);
 					displayContextSelector();
 					hideContextInfoPopup();
 					
 				} else if (!autoActivated) {
 					control.getDisplay().beep();
 				}
 			}
 		});
 		
 		return getErrorMessage();
 	}
 	
 	/**
 	 * Displays the given context information for the given offset.
 	 * 
 	 * @param info the context information
 	 * @param position the offset
 	 * @since 2.0
 	 */
 	public void showContextInformation(final IContextInformation info, final int position) {
 		Control control= fContentAssistSubjectAdapter.getControl();
 		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
 			public void run() {
 				ContextFrame frame= createContextFrame(info, position);
 				if (isDuplicate(frame))
 					validateContextInformation();
 				else
 					internalShowContextInfo(frame);
 				hideContextSelector();
 			}
 		});
 	}
 	
 	/**
 	 * Displays the given context information for the given offset.
 	 * 
 	 * @param frame the context frame tod display, or <code>null</code>
 	 * @since 3.0
 	 */
 	private void internalShowContextInfo(ContextFrame frame) {
 		if (frame != null) {
 			fContextFrameStack.push(frame);
 			if (fContextFrameStack.size() == 1)
 				fLastContext= null;
 			internalShowContextFrame(frame, fContextFrameStack.size() == 1);
 		}
 	}
 	
 	/**
 	 * Creates a context frame for the given offset.
 	 * 
 	 * @param info the context information
 	 * @param position the offset
 	 * @since 3.0
 	 */
 	private ContextFrame createContextFrame(IContextInformation information, int offset) {
 		IContextInformationValidator validator= fContentAssistSubjectAdapter.getContextInformationValidator(fContentAssistant, offset);
 		
 		if (validator != null) {
 			ContextFrame current= new ContextFrame();
 			current.fInformation= information;
 			current.fBeginOffset= (information instanceof IContextInformationExtension) ? ((IContextInformationExtension) information).getContextInformationPosition() : offset;
 			if (current.fBeginOffset == -1) current.fBeginOffset= offset;
 			current.fOffset= offset;
 			current.fVisibleOffset= fContentAssistSubjectAdapter.getWidgetSelectionRange().x - (offset - current.fBeginOffset);
 			current.fValidator= validator;
 			current.fPresenter= fContentAssistSubjectAdapter.getContextInformationPresenter(fContentAssistant, offset);
 			return current;
 		}
 		
 		return null;
 	}
 
 	/**
 	 * Compares <code>frame</code> with the top of the stack, returns <code>true</code>
 	 * if the frames are the same.
 	 * 
 	 * @param frame the frame to check
 	 * @return <code>true</code> if <code>frame</code> matches the top of the stack
 	 * @since 3.0
 	 */
 	private boolean isDuplicate(ContextFrame frame) {
 		if (frame == null)
 			return false;
 		if (fContextFrameStack.isEmpty())
 				return false;
 		// stack not empty
 		ContextFrame top= (ContextFrame) fContextFrameStack.peek();
 		return frame.equals(top);
 	}
 	
 	/**
 	 * Compares <code>frame</code> with most recently removed context frame, returns <code>true</code>
 	 * if the frames are the same.
 	 * 
 	 * @param frame the frame to check
 	 * @return <code>true</code> if <code>frame</code> matches the most recently removed
 	 * @since 3.0
 	 */
 	private boolean isLastFrame(ContextFrame frame) {
 		return frame != null && frame.equals(fLastContext);
 	}
 	
 	/**
 	 * Shows the given context frame.
 	 * 
 	 * @param frame the frane to display
 	 * @param initial <code>true</code> if this is the first frame to be displayed
 	 * @since 2.0
 	 */
 	private void internalShowContextFrame(ContextFrame frame, boolean initial) {
 		
 		fContentAssistSubjectAdapter.installValidator(frame);
 		
 		if (frame.fPresenter != null) {
 			if (fTextPresentation == null)
 				fTextPresentation= new TextPresentation();
 			fContentAssistSubjectAdapter.installContextInformationPresenter(frame);
 			frame.fPresenter.updatePresentation(frame.fOffset, fTextPresentation);
 		}
 		
 		createContextInfoPopup();
 		
 		fContextInfoText.setText(frame.fInformation.getInformationDisplayString());
 		if (fTextPresentation != null)
 			TextPresentation.applyTextPresentation(fTextPresentation, fContextInfoText);
 		resize();
 				
 		if (initial) {
 			if (fContentAssistant.addContentAssistListener(this, ContentAssistant.CONTEXT_INFO_POPUP)) {
 				if (fContentAssistSubjectAdapter.getControl() != null) {
 					fTextWidgetSelectionListener= new SelectionAdapter() {
 						/*
 						 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
 						 */
 						public void widgetSelected(SelectionEvent e) {
 							validateContextInformation();
 						}};
 					fContentAssistSubjectAdapter.addSelectionListener(fTextWidgetSelectionListener);
 				}
 				fContentAssistant.addToLayout(this, fContextInfoPopup, ContentAssistant.LayoutManager.LAYOUT_CONTEXT_INFO_POPUP, frame.fVisibleOffset);
 				fContextInfoPopup.setVisible(true);
 			}
 		} else {
 			fContentAssistant.layout(ContentAssistant.LayoutManager.LAYOUT_CONTEXT_INFO_POPUP, frame.fVisibleOffset);
 		}
 	}
 	
 	/**
 	 * Computes all possible context information for the given offset.
 	 * 
 	 * @param position the offset
 	 * @return all possible context information for the given offset
 	 * @since 2.0
 	 */
 	private IContextInformation[] computeContextInformation(int position) {
 		return fContentAssistSubjectAdapter.computeContextInformation(fContentAssistant, position);
 	}
 	
 	/**
 	 *Returns the error message generated while computing context information.
 	 * 
 	 * @return the error message
 	 */
 	private String getErrorMessage() {
 		return fContentAssistant.getErrorMessage();
 	}
 
 	/**
 	 * Creates the context information popup. This is the tooltip like overlay window.
 	 */
 	private void createContextInfoPopup() {
 		if (Helper.okToUse(fContextInfoPopup))
 			return;
 		
 		Control control= fContentAssistSubjectAdapter.getControl();
 		Display display= control.getDisplay();
 		
 		fContextInfoPopup= new Shell(control.getShell(), SWT.NO_TRIM | SWT.ON_TOP);
 		fContextInfoPopup.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
 		
 		fContextInfoText= new StyledText(fContextInfoPopup, SWT.MULTI | SWT.READ_ONLY);
 		
 		Color c= fContentAssistant.getContextInformationPopupBackground();
 		if (c == null)
 			c= display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
 		fContextInfoText.setBackground(c);
 		
 		c= fContentAssistant.getContextInformationPopupForeground();
 		if (c == null)
 			c= display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
 		fContextInfoText.setForeground(c);			
 	}
 		
 	/**
 	 * Resizes the context information popup.
 	 * @since 2.0
 	 */
 	private void resize() {
 		Point size= fContextInfoText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
 		size.x += 3;
 		fContextInfoText.setSize(size);
 		fContextInfoText.setLocation(1,1);
 		size.x += 2;
 		size.y += 2;
 		fContextInfoPopup.setSize(size);
 	}
 	
 	/**
 	 *Hides the context information popup.
 	 */
 	private void hideContextInfoPopup() {
 		
 		if (Helper.okToUse(fContextInfoPopup)) {
 			
 			int size= fContextFrameStack.size();
 			if (size > 0) {
 				fLastContext= (ContextFrame) fContextFrameStack.pop();
 				-- size;
 			}
 			
 			if (size > 0) {
 				ContextFrame current= (ContextFrame) fContextFrameStack.peek();
 				internalShowContextFrame(current, false);
 			} else {
 				
 				fContentAssistant.removeContentAssistListener(this, ContentAssistant.CONTEXT_INFO_POPUP);
 				
 				if (fContentAssistSubjectAdapter.getControl() != null)
 					fContentAssistSubjectAdapter.removeSelectionListener(fTextWidgetSelectionListener);
 				fTextWidgetSelectionListener= null;
 				
 				fContextInfoPopup.setVisible(false);
 				fContextInfoPopup.dispose();
 				fContextInfoPopup= null;
 				
 				if (fTextPresentation != null) {
 					fTextPresentation.clear();
 					fTextPresentation= null;
 				}
 			}
 		}
 		
 		if (fContextInfoPopup == null)
 			fContentAssistant.contextInformationClosed();
 	}
 	
 	/**
 	 * Creates the context selector in case the user has the choice between multiple valid contexts
 	 * at a given offset.
 	 */
 	private void createContextSelector() {
 		if (Helper.okToUse(fContextSelectorShell))
 			return;
 		
 		Control control= fContentAssistSubjectAdapter.getControl();
		fContextSelectorShell= new Shell(control.getShell(), SWT.ON_TOP | SWT.RESIZE);
 		fContextSelectorTable= new Table(fContextSelectorShell, SWT.H_SCROLL | SWT.V_SCROLL);
 
 		int height= fContextSelectorTable.getItemHeight() * 10;
 		fContextSelectorShell.setSize(302, height + 2);
 		fContextSelectorTable.setSize(300, height);
 		fContextSelectorTable.setLocation(1, 1);
 		
 		fContextSelectorShell.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_BLACK));
 		
 		Color c= fContentAssistant.getContextSelectorBackground();
 		if (c == null)
 			c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
 		fContextSelectorTable.setBackground(c);
 		
 		c= fContentAssistant.getContextSelectorForeground();
 		if (c == null)
 			c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
 		fContextSelectorTable.setForeground(c);
 		
 		fContextSelectorTable.addSelectionListener(new SelectionListener() {
 			public void widgetSelected(SelectionEvent e) {
 			}
 
 			public void widgetDefaultSelected(SelectionEvent e) {
 				insertSelectedContext();
 				hideContextSelector();
 			}
 		});
 
 		fPopupCloser.install(fContentAssistant, fContextSelectorTable);
 
 		fContextSelectorTable.setHeaderVisible(false);
 		fContentAssistant.addToLayout(this, fContextSelectorShell, ContentAssistant.LayoutManager.LAYOUT_CONTEXT_SELECTOR, fContentAssistant.getSelectionOffset());
 	}
 	
 	/**
 	 * Causes the context information of the context selected in the context selector 
 	 * to be displayed in the context information popup.
 	 */
 	private void insertSelectedContext() {
 		int i= fContextSelectorTable.getSelectionIndex();
 
 		if (i < 0 || i >= fContextSelectorInput.length)
 			return;
 		
 		int position= fContentAssistSubjectAdapter.getSelectedRange().x;
 		internalShowContextInfo(createContextFrame(fContextSelectorInput[i], position));
 	}
 	
 	/**
 	 * Sets the contexts in the context selector to the given set.
 	 * 
 	 * @param contexts the possible contexts
 	 */
 	private void setContexts(IContextInformation[] contexts) {
 		if (Helper.okToUse(fContextSelectorTable)) {
 
 			fContextSelectorInput= contexts;
 
 			fContextSelectorTable.setRedraw(false);
 			fContextSelectorTable.removeAll();
 
 			TableItem item;
 			IContextInformation t;
 			for (int i= 0; i < contexts.length; i++) {
 				t= contexts[i];
 				item= new TableItem(fContextSelectorTable, SWT.NULL);
 				if (t.getImage() != null)
 					item.setImage(t.getImage());
 				item.setText(t.getContextDisplayString());
 			}
 
 			fContextSelectorTable.select(0);
 			fContextSelectorTable.setRedraw(true);
 		}
 	}
 	
 	/**
 	 * Displays the context selector.
 	 */
 	private void displayContextSelector() {
 		if (fContentAssistant.addContentAssistListener(this, ContentAssistant.CONTEXT_SELECTOR))
 			fContextSelectorShell.setVisible(true);
 	}
 
 	/**
 	 * Hodes the context selector.
 	 */
 	private void hideContextSelector() {
 		if (Helper.okToUse(fContextSelectorShell)) {
 			fContentAssistant.removeContentAssistListener(this, ContentAssistant.CONTEXT_SELECTOR);
 			
 			fPopupCloser.uninstall();
 			fContextSelectorShell.setVisible(false);
 			fContextSelectorShell.dispose();
 			fContextSelectorShell= null;
 		}
 		
 		if (!Helper.okToUse(fContextInfoPopup))
 			fContentAssistant.contextInformationClosed();
 	}
 	
 	/**
 	 *Returns whether the context selector has the focus.
 	 *
 	 * @return <code>true</code> if teh context selector has the focus
 	 */
 	public boolean hasFocus() {
 		if (Helper.okToUse(fContextSelectorShell))
 			return (fContextSelectorShell.isFocusControl() || fContextSelectorTable.isFocusControl());
 
 		return false;
 	}
 	
 	/**
 	 * Hides context selector and context information popup.
 	 */
 	public void hide() {
 		hideContextSelector();
 		hideContextInfoPopup();
 	}
 	
 	/**
 	 * Returns whether this context information popup is active. I.e., either 
 	 * a context selector or context information is displayed.
 	 * 
 	 * @return <code>true</code> if the context selector is active
 	 */
 	public boolean isActive() {
 		return (Helper.okToUse(fContextInfoPopup) || Helper.okToUse(fContextSelectorShell));
 	}
 	
 	/* 
 	 * @see IContentAssistListener#verifyKey(VerifyEvent)
 	 */
 	public boolean verifyKey(VerifyEvent e) {
 		if (Helper.okToUse(fContextSelectorShell))
 			return contextSelectorKeyPressed(e);
 		if (Helper.okToUse(fContextInfoPopup))
 			return contextInfoPopupKeyPressed(e);
 		return true;
 	}
 	
 	/**
 	 * Processes a key stroke in the context selector.
 	 * 
 	 * @param e the verify event describing the key stroke
 	 * @return <code>true</code> if processing can be stopped
 	 */
 	private boolean contextSelectorKeyPressed(VerifyEvent e) {
 
 		char key= e.character;
 		if (key == 0) {
 			
 			int change;
 			int visibleRows= (fContextSelectorTable.getSize().y / fContextSelectorTable.getItemHeight()) - 1;
 			int selection= fContextSelectorTable.getSelectionIndex();
 
 			switch (e.keyCode) {
 				
 				case SWT.ARROW_UP:
 					change= (fContextSelectorTable.getSelectionIndex() > 0 ? -1 : 0);
 					break;
 				
 				case SWT.ARROW_DOWN:
 					change= (fContextSelectorTable.getSelectionIndex() < fContextSelectorTable.getItemCount() - 1 ? 1 : 0);
 					break;
 					
 				case SWT.PAGE_DOWN :
 					change= visibleRows;
 					if (selection + change >= fContextSelectorTable.getItemCount())
 						change= fContextSelectorTable.getItemCount() - selection;
 					break;
 					
 				case SWT.PAGE_UP :
 					change= -visibleRows;
 					if (selection + change < 0)
 						change= -selection;
 					break;
 					
 				case SWT.HOME :
 					change= -selection;
 					break;
 					
 				case SWT.END :
 					change= fContextSelectorTable.getItemCount() - selection;
 					break;
 					
 				default:
 					if (e.keyCode != SWT.MOD1 && e.keyCode != SWT.MOD2 && e.keyCode != SWT.MOD3 && e.keyCode != SWT.MOD4)
 						hideContextSelector();
 					return true;
 			}
 			
 			fContextSelectorTable.setSelection(selection + change);
 			fContextSelectorTable.showSelection();
 			e.doit= false;
 			return false;
 
 		} else if ('\t' == key) {
 			// switch focus to selector shell
 			e.doit= false;
 			fContextSelectorShell.setFocus();
 			return false;
 		} else if (key == 0x1B) {
 			// terminate on Esc
 			hideContextSelector();
 		}
 		
 		return true;
 	}
 
 	/**
 	 * Processes a key stroke while the info popup is up.
 	 * 
 	 * @param e the verify event describing the key stroke
 	 * @return <code>true</code> if processing can be stopped
 	 */
 	private boolean contextInfoPopupKeyPressed(KeyEvent e) {
 
 		char key= e.character;
 		if (key == 0) {
 			
 			switch (e.keyCode) {
 
 				case SWT.ARROW_LEFT:
 				case SWT.ARROW_RIGHT:
 					validateContextInformation();
 					break;
 				default:
 					if (e.keyCode != SWT.MOD1 && e.keyCode != SWT.MOD2 && e.keyCode != SWT.MOD3 && e.keyCode != SWT.MOD4)
 						hideContextInfoPopup();
 					break;
 			}
 			
 		} else if (key == 0x1B) {
 			// terminate on Esc
  			hideContextInfoPopup();
 		} else {
 			validateContextInformation();
 		}
 		return true;
 	}
 	
 	/*
 	 * @see IEventConsumer#processEvent(VerifyEvent)
 	 */
 	public void processEvent(VerifyEvent event) {
 		if (Helper.okToUse(fContextSelectorShell))
 			contextSelectorProcessEvent(event);
 		if (Helper.okToUse(fContextInfoPopup))
 			contextInfoPopupProcessEvent(event);
 	}
 
 	/**
 	 * Processes a key stroke in the context selector.
 	 * 
 	 * @param e the verify event describing the key stroke
 	 */
 	private void contextSelectorProcessEvent(VerifyEvent e) {
 		
 		if (e.start == e.end && e.text != null && e.text.equals(fLineDelimiter)) {
 			e.doit= false;
 			insertSelectedContext();
 		}
 
 		hideContextSelector();
 	}
 
 	/**
 	 * Processes a key stroke while the info popup is up.
 	 * 
 	 * @param e the verify event describing the key stroke
 	 */
 	private void contextInfoPopupProcessEvent(VerifyEvent e) {
 		if (e.start != e.end && (e.text == null || e.text.length() == 0))
 			validateContextInformation();
 	}
 	
 	/**
 	 * Validates the context information for the viewer's actual cursor position.
 	 */
 	private void validateContextInformation() {
 		/*
 		 * Post the code in the event queue in order to ensure that the
 		 * action described by this verify key event has already beed executed.
 		 * Otherwise, we'd validate the context information based on the 
 		 * pre-key-stroke state.
 		 */
 		fContextInfoPopup.getDisplay().asyncExec(new Runnable() {
 			
 			private ContextFrame fFrame= (ContextFrame) fContextFrameStack.peek();
 			
 			public void run() {
 				// only do this if no other frames have been added in between
 				if (!fContextFrameStack.isEmpty() && fFrame == fContextFrameStack.peek()) {
 					int offset= fContentAssistSubjectAdapter.getSelectedRange().x;
 					
 					// iterate all contexts on the stack
 					while (Helper.okToUse(fContextInfoPopup) && !fContextFrameStack.isEmpty()) {
 						ContextFrame top= (ContextFrame) fContextFrameStack.peek();
 						if (top.fValidator == null || !top.fValidator.isContextInformationValid(offset)) {
 							hideContextInfoPopup(); // loop variant: reduces the number of contexts on the stack
 						} else if (top.fPresenter != null && top.fPresenter.updatePresentation(offset, fTextPresentation)) {
 							TextPresentation.applyTextPresentation(fTextPresentation, fContextInfoText);
 							resize();
 							break;
 						} else
 							break;
 					}
 				}
 			}
 		});
 	}
 }
