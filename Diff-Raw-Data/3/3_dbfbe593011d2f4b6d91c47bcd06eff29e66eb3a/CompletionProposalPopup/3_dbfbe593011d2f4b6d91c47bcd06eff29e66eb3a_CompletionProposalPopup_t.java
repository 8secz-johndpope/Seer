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
 
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.BusyIndicator;
 import org.eclipse.swt.events.ControlEvent;
 import org.eclipse.swt.events.ControlListener;
 import org.eclipse.swt.events.DisposeEvent;
 import org.eclipse.swt.events.DisposeListener;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.events.KeyListener;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.events.VerifyEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableItem;
 
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.DocumentEvent;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IDocumentListener;
 import org.eclipse.jface.text.IRewriteTarget;
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.text.ITextViewerExtension;
 import org.eclipse.jface.text.TextUtilities;
 
 
 
 /**
  * This class is used to present proposals to the user. If additional
  * information exists for a proposal, then selecting that proposal
  * will result in the information being displayed in a secondary
  * window.
  *
  * @see org.eclipse.jface.text.contentassist.ICompletionProposal
  * @see org.eclipse.jface.text.contentassist.AdditionalInfoController
  */
 class CompletionProposalPopup implements IContentAssistListener {
 	
 	private final class MyKeyListener implements KeyListener {
 		public void keyPressed(KeyEvent e) {
 			if (!Helper.okToUse(fProposalShell))
 				return;
 			
 			if (e.character == 0 && e.keyCode == SWT.MOD1) {
 				// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
 				int index= fProposalTable.getSelectionIndex();
 				if (index >= 0)
 					selectProposal(index, true);
 			}
 		}
 
 		public void keyReleased(KeyEvent e) {
 			if (!Helper.okToUse(fProposalShell))
 				return;
 
 			if (e.character == 0 && e.keyCode == SWT.MOD1) {
 				// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
 				int index= fProposalTable.getSelectionIndex();
 				if (index >= 0)
 					selectProposal(index, false);
 			}
 		}
 	}
 
 	/** The associated text viewer */
 	private ITextViewer fViewer;
 	/** The associated content assistant */
 	private ContentAssistant fContentAssistant;
 	/** The used additional info controller */
 	private AdditionalInfoController fAdditionalInfoController;
 	/** The closing strategy for this completion proposal popup */
 	private PopupCloser fPopupCloser= new PopupCloser();
 	/** The popup shell */
 	private Shell fProposalShell;
 	/** The proposal table */
 	private Table fProposalTable;
 	/** Indicates whether a completion proposal is being inserted */
 	private boolean fInserting= false;
 	/** The key listener to control navigation */
 	private KeyListener fKeyListener;
 	/** List of document events used for filtering proposals */
 	private List fDocumentEvents= new ArrayList();
 	/** Listener filling the document event queue */
 	private IDocumentListener fDocumentListener;
 	/** Reentrance count for <code>filterProposals</code> */
 	private long fInvocationCounter= 0;
 	/** The filter list of proposals */
 	private ICompletionProposal[] fFilteredProposals;
 	/** The computed list of proposals */
 	private ICompletionProposal[] fComputedProposals;
 	/** The offset for which the proposals have been computed */
 	private int fInvocationOffset;
 	/** The offset for which the computed proposals have been filtered */
 	private int fFilterOffset;
 	/** The default line delimiter of the viewer's widget */
 	private String fLineDelimiter;
 	/** The most recently selected proposal. */
 	private ICompletionProposal fLastProposal;
 	/**
 	 * The content assist subject.
 	 * This replaces <code>fViewer</code>
 	 * 
 	 * @since 3.0
 	 */
 	private IContentAssistSubject fContentAssistSubject;
 	/**
 	 * The content assist subject adapter.
 	 * This replaces <code>fViewer</code>
 	 * 
 	 * @since 3.0
 	 */
 	private ContentAssistSubjectAdapter fContentAssistSubjectAdapter;
 	/**
 	 * Remembers the size for this completion proposal popup.
 	 * @since 3.0
 	 */
 	private Point fSize;
 	
 	/**
 	 * Creates a new completion proposal popup for the given elements.
 	 * 
 	 * @param contentAssistant the content assistant feeding this popup
 	 * @param viewer the viewer on top of which this popup appears
 	 * @param infoController the info control collaborating with this popup
 	 * @since 2.0
 	 */
 	public CompletionProposalPopup(ContentAssistant contentAssistant, ITextViewer viewer, AdditionalInfoController infoController) {
 		fContentAssistant= contentAssistant;
 		fViewer= viewer;
 		fAdditionalInfoController= infoController;
 		fContentAssistSubjectAdapter= new ContentAssistSubjectAdapter(fViewer);
 	}
 
 	/**
 	 * Creates a new completion proposal popup for the given elements.
 	 * <p>
 	 * XXX: This is work in progress and can change anytime until API for 3.0 is frozen.
 	 * </p>
 	 * @param contentAssistant the content assistant feeding this popup
 	 * @param contentAssistSubject the content assist subject on top of which this popup appears
 	 * @param infoController the info control collaborating with this popup
 	 * @since 3.0
 	 */
 	public CompletionProposalPopup(ContentAssistant contentAssistant, IContentAssistSubject contentAssistSubject, AdditionalInfoController infoController) {
 		fContentAssistant= contentAssistant;
 		fContentAssistSubject= contentAssistSubject;
 		fAdditionalInfoController= infoController;
 		fContentAssistSubjectAdapter= new ContentAssistSubjectAdapter(fContentAssistSubject);
 	}
 
 	/**
 	 * Computes and presents completion proposals. The flag indicates whether this call has
 	 * be made out of an auto activation context.
 	 * 
 	 * @param autoActivated <code>true</code> if auto activation context
 	 * @return an error message or <code>null</code> in case of no error
 	 */
 	public String showProposals(final boolean autoActivated) {
 					
 		if (fKeyListener == null) {
 			fKeyListener= new MyKeyListener();
 		}
 
 		final Control control= fContentAssistSubjectAdapter.getControl();
 		if (control != null && !control.isDisposed())
 			fContentAssistSubjectAdapter.addKeyListener(fKeyListener);
 
 		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
 			public void run() {
 				
 				fInvocationOffset= fContentAssistSubjectAdapter.getSelectedRange().x;
 				fFilterOffset= fInvocationOffset;
 				fComputedProposals= computeProposals(fInvocationOffset);
 				
 				int count= (fComputedProposals == null ? 0 : fComputedProposals.length);
 				if (count == 0) {
 					
 					if (!autoActivated)
 						control.getDisplay().beep();
 					
 					unregister();
 				
 				} else {
 					
 					if (count == 1 && !autoActivated && fContentAssistant.isAutoInserting()) {
 						
 						insertProposal(fComputedProposals[0], (char) 0, 0, fInvocationOffset);
 						unregister();
 					
 					} else {
 					
 						if (fLineDelimiter == null)
 							fLineDelimiter= fContentAssistSubjectAdapter.getLineDelimiter();
 						
 						createProposalSelector();
 						setProposals(fComputedProposals);
 						displayProposals();
 					}
 				}
 			}
 		});
 		
 		return getErrorMessage();
 	}
 	
 	/**
 	 * Returns the completion proposal available at the given offset of the
 	 * viewer's document. Delegates the work to the content assistant.
 	 * 
 	 * @param offset the offset
 	 * @return the completion proposals available at this offset
 	 */
 	private ICompletionProposal[] computeProposals(int offset) {
 		if (fContentAssistSubject != null)
 			return fContentAssistant.computeCompletionProposals(fContentAssistSubject, offset);
 		else
 			return fContentAssistant.computeCompletionProposals(fViewer, offset);
 	}
 	
 	/**
 	 * Returns the error message.
 	 * 
 	 * @return the error message
 	 */
 	private String getErrorMessage() {
 		return fContentAssistant.getErrorMessage();
 	}
 	
 	/**
 	 * Creates the proposal selector.
 	 */
 	private void createProposalSelector() {
 		if (Helper.okToUse(fProposalShell))
 			return;
 		
 		Control control= fContentAssistSubjectAdapter.getControl();
 		fProposalShell= new Shell(control.getShell(), SWT.ON_TOP | SWT.RESIZE );
 		fProposalTable= new Table(fProposalShell, SWT.H_SCROLL | SWT.V_SCROLL);
 		
 		fProposalTable.setLocation(0, 0);
 		if (fAdditionalInfoController != null)
 			fAdditionalInfoController.setSizeConstraints(50, 10, true, false);
 		
 		GridLayout layout= new GridLayout();
 		layout.marginWidth= 0;
 		layout.marginHeight= 0;		
 		fProposalShell.setLayout(layout);		
 
 		GridData data= new GridData(GridData.FILL_BOTH);
 		
 	
 		Point size= fContentAssistant.restoreCompletionProposalPopupSize();
 		if (size != null) {
 			fProposalTable.setLayoutData(data);
 			fProposalShell.setSize(size);
 		} else {
 			data.heightHint= fProposalTable.getItemHeight() * 10;
 			data.widthHint= 300;
 			fProposalTable.setLayoutData(data);
 			fProposalShell.pack();
 		}
 		
 		fProposalShell.addControlListener(new ControlListener() {
 			
 			public void controlMoved(ControlEvent e) {}
 			
 			public void controlResized(ControlEvent e) {
 				if (fAdditionalInfoController != null) {
 					// reset the cached resize constraints
 					fAdditionalInfoController.setSizeConstraints(50, 10, true, false);
 				}
 				
 				fSize= fProposalShell.getSize();
 			}
 		});
 		
 		if (!"carbon".equals(SWT.getPlatform())) //$NON-NLS-1$
 			fProposalShell.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_BLACK));
 		
 		Color c= fContentAssistant.getProposalSelectorBackground();
 		if (c == null)
 			c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
 		fProposalTable.setBackground(c);
 		
 		c= fContentAssistant.getProposalSelectorForeground();
 		if (c == null)
 			c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
 		fProposalTable.setForeground(c);
 		
 		fProposalTable.addSelectionListener(new SelectionListener() {
 			
 			public void widgetSelected(SelectionEvent e) {}
 
 			public void widgetDefaultSelected(SelectionEvent e) {
 				selectProposalWithMask(e.stateMask);
 			}
 		});
 
 		fPopupCloser.install(fContentAssistant, fProposalTable);
 		
 		fProposalShell.addDisposeListener(new DisposeListener() {
 			public void widgetDisposed(DisposeEvent e) {
 				unregister(); // but don't dispose the shell, since we're being called from its disposal event!  
 			}
 		});
 		
 		fProposalTable.setHeaderVisible(false);
 		fContentAssistant.addToLayout(this, fProposalShell, ContentAssistant.LayoutManager.LAYOUT_PROPOSAL_SELECTOR, fContentAssistant.getSelectionOffset());
 	}
 	
 	/**
 	 * Returns the proposal selected in the proposal selector.
 	 * 
 	 * @return the selected proposal
 	 * @since 2.0
 	 */
 	private ICompletionProposal getSelectedProposal() {
 		int i= fProposalTable.getSelectionIndex();
 		if (fFilteredProposals == null || i < 0 || i >= fFilteredProposals.length)
 			return null;
 		return fFilteredProposals[i];
 	}
 		
 	/**
 	 * Takes the selected proposal and applies it.
 	 * 
 	 * @param stateMask the state mask
 	 * @since 2.1
 	 */
 	private void selectProposalWithMask(int stateMask) {
 		ICompletionProposal p= getSelectedProposal();
 		hide();
 		if (p != null)
 			insertProposal(p, (char) 0, stateMask, fContentAssistSubjectAdapter.getSelectedRange().x);
 	}
 	
 	/**
 	 * Applies the given proposal at the given offset. The given character is the
 	 * one that triggered the insertion of this proposal.
 	 * 
 	 * @param p the completion proposal
 	 * @param trigger the trigger character
 	 * @param offset the offset
 	 * @since 2.1
 	 */
 	private void insertProposal(ICompletionProposal p, char trigger, int stateMask, int offset) {
 			
 		fInserting= true;
 		IRewriteTarget target= null;
 		
 		try {
 			
 			IDocument document= fContentAssistSubjectAdapter.getDocument();
 			
 			if (fViewer instanceof ITextViewerExtension) {
 				ITextViewerExtension extension= (ITextViewerExtension) fViewer;
 				target= extension.getRewriteTarget();
 			}
 			
 			if (target != null)
 				target.beginCompoundChange();
 
 			if (p instanceof ICompletionProposalExtension2 && fViewer != null) {
 				ICompletionProposalExtension2 e= (ICompletionProposalExtension2) p;
 				e.apply(fViewer, trigger, stateMask, offset);				
 			} else if (p instanceof ICompletionProposalExtension) {
 				ICompletionProposalExtension e= (ICompletionProposalExtension) p;
 				e.apply(document, trigger, offset);
 			} else {
 				p.apply(document);
 			}
 			
 			Point selection= p.getSelection(document);
 			if (selection != null) {
 				fContentAssistSubjectAdapter.setSelectedRange(selection.x, selection.y);
 				fContentAssistSubjectAdapter.revealRange(selection.x, selection.y);
 			}
 			
 			IContextInformation info= p.getContextInformation();
 			if (info != null) {				
 				
 				int position;
 				if (p instanceof ICompletionProposalExtension) {
 					ICompletionProposalExtension e= (ICompletionProposalExtension) p;
 					position= e.getContextInformationPosition();
 				} else {
 					if (selection == null)
 						selection= fContentAssistSubjectAdapter.getSelectedRange();
 					position= selection.x + selection.y;
 				}
 				
 				fContentAssistant.showContextInformation(info, position);
 			}
 		
 		} finally {
 			if (target != null)
 				target.endCompoundChange();
 			fInserting= false;
 		}
 	}
 	
 	/**
 	 * Returns whether this popup has the focus.
 	 * 
 	 * @return <code>true</code> if the popup has the focus
 	 */
 	public boolean hasFocus() {
 		if (Helper.okToUse(fProposalShell))
 			return (fProposalShell.isFocusControl() || fProposalTable.isFocusControl());
 
 		return false;
 	}
 	
 	/**
 	 * Hides this popup.
 	 */
 	public void hide() {
 
 		unregister();
 
 		if (Helper.okToUse(fProposalShell)) {
 			
 			fContentAssistant.removeContentAssistListener(this, ContentAssistant.PROPOSAL_SELECTOR);
 			
 			fPopupCloser.uninstall();
 			fProposalShell.setVisible(false);
 			fProposalShell.dispose();
 			fProposalShell= null;
 		}
 	}
 	
 	private void unregister() {
 		if (fDocumentListener != null) {
 			IDocument document= fContentAssistSubjectAdapter.getDocument();
 			if (document != null)
 				document.removeDocumentListener(fDocumentListener);
 			fDocumentListener= null;
 		}
 		fDocumentEvents.clear();		
 
 		if (fKeyListener != null && fContentAssistSubjectAdapter.getControl() != null && !fContentAssistSubjectAdapter.getControl().isDisposed())
 			fContentAssistSubjectAdapter.removeKeyListener(fKeyListener);
 		
 		if (fLastProposal != null) {
 			if (fLastProposal instanceof ICompletionProposalExtension2 && fViewer != null) {
 				ICompletionProposalExtension2 extension= (ICompletionProposalExtension2) fLastProposal;
 				extension.unselected(fViewer);
 			}
 			fLastProposal= null;
 		}
 
 		fFilteredProposals= null;
 		fComputedProposals= null;
 		
 		fContentAssistant.possibleCompletionsClosed();
 	}
 
 	/**
 	 *Returns whether this popup is active. It is active if the propsal selector is visible.
 	 *
 	 * @return <code>true</code> if this popup is active
 	 */
 	public boolean isActive() {
 		return fProposalShell != null && !fProposalShell.isDisposed();
 	}
 	
 	/**
 	 * Initializes the proposal selector with these given proposals.
 	 * 
 	 * @param proposals the proposals
 	 */
 	private void setProposals(ICompletionProposal[] proposals) {
 		if (Helper.okToUse(fProposalTable)) {
 
 			ICompletionProposal oldProposal= getSelectedProposal();
 			if (oldProposal instanceof ICompletionProposalExtension2 && fViewer != null)
 				((ICompletionProposalExtension2) oldProposal).unselected(fViewer);
 
 			fFilteredProposals= proposals;
 
 			fProposalTable.setRedraw(false);
 			fProposalTable.removeAll();
 
 			TableItem item;
 			ICompletionProposal p;
 			for (int i= 0; i < proposals.length; i++) {
 				p= proposals[i];
 				item= new TableItem(fProposalTable, SWT.NULL);
 				if (p.getImage() != null)
 					item.setImage(p.getImage());
 				item.setText(p.getDisplayString());
 				item.setData(p);
 			}
 
 			Point currentLocation= fProposalShell.getLocation();
 			Point newLocation= getLocation();
 			if ((newLocation.x < currentLocation.x && newLocation.y == currentLocation.y) || newLocation.y < currentLocation.y) 
 				fProposalShell.setLocation(newLocation);
 
 			selectProposal(0, false);
 			fProposalTable.setRedraw(true);
 		}
 	}
 	
 	/**
 	 * Returns the graphical location at which this popup should be made visible.
 	 * 
 	 * @return the location of this popup
 	 */
 	private Point getLocation() {
 		int caret= fContentAssistSubjectAdapter.getCaretOffset();
 		Point p= fContentAssistSubjectAdapter.getLocationAtOffset(caret);
 		if (p.x < 0) p.x= 0;
 		if (p.y < 0) p.y= 0;
 		p= new Point(p.x, p.y + fContentAssistSubjectAdapter.getLineHeight());
 		p= fContentAssistSubjectAdapter.getControl().toDisplay(p);
 		if (p.x < 0) p.x= 0;
 		if (p.y < 0) p.y= 0;
 		return p;
 	}
 	
 	Point getSize() {
 		return fSize;
 	}
 
 	/**
 	 * Displays this popup and install the additional info controller, so that additional info
 	 * is displayed when a proposal is selected and additional info is available.
 	 */
 	private void displayProposals() {
 		
 		if (!Helper.okToUse(fProposalShell) ||  !Helper.okToUse(fProposalTable))
 			return;
 		
 		if (fContentAssistant.addContentAssistListener(this, ContentAssistant.PROPOSAL_SELECTOR)) {
 			
 			if (fDocumentListener == null)
 				fDocumentListener=  new IDocumentListener()  {
 					public void documentAboutToBeChanged(DocumentEvent event) {
 						if (!fInserting)
 							fDocumentEvents.add(event);
 					}
 	
 					public void documentChanged(DocumentEvent event) {
 						if (!fInserting)
 							filterProposals();
 					}
 				};
 			IDocument document= fContentAssistSubjectAdapter.getDocument();
 			if (document != null)
 				document.addDocumentListener(fDocumentListener);		
 				
 			/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=52646
 			 * on GTK, setVisible and such may run the event loop
 			 * (see also https://bugs.eclipse.org/bugs/show_bug.cgi?id=47511)
 			 * Since the user may have already canceled the popup or selected
 			 * an entry (ESC or RETURN), we have to double check whether
 			 * the table is still okToUse. See comments below
 			 */
 			fProposalShell.setVisible(true); // may run event loop on GTK
 			// XXX: transfer focus since no verify key listern can be attached
 			if (!fContentAssistSubjectAdapter.supportsVerifyKeyListener() && Helper.okToUse(fProposalShell))
 				fProposalShell.setFocus(); // may run event loop on GTK ??
 			
 			if (fAdditionalInfoController != null && Helper.okToUse(fProposalTable)) {
 				fAdditionalInfoController.install(fProposalTable);		
 				fAdditionalInfoController.handleTableSelectionChanged();
 			}
 		}
 	}
 	
 	/*
 	 * @see IContentAssistListener#verifyKey(VerifyEvent)
 	 */
 	public boolean verifyKey(VerifyEvent e) {
 		if (!Helper.okToUse(fProposalShell))
 			return true;
 		
 		char key= e.character;
 		if (key == 0) {
 			int newSelection= fProposalTable.getSelectionIndex();
 			int visibleRows= (fProposalTable.getSize().y / fProposalTable.getItemHeight()) - 1;
 			boolean smartToggle= false;
 			switch (e.keyCode) {
 
 				case SWT.ARROW_LEFT :
 				case SWT.ARROW_RIGHT :
 					filterProposals();
 					return true;
 
 				case SWT.ARROW_UP :
 					newSelection -= 1;
 					if (newSelection < 0)
 						newSelection= fProposalTable.getItemCount() - 1;
 					break;
 
 				case SWT.ARROW_DOWN :
 					newSelection += 1;
 					if (newSelection > fProposalTable.getItemCount() - 1)
 						newSelection= 0;
 					break;
 					
 				case SWT.PAGE_DOWN :
 					newSelection += visibleRows;
 					if (newSelection >= fProposalTable.getItemCount())
 						newSelection= fProposalTable.getItemCount() - 1;
 					break;
 					
 				case SWT.PAGE_UP :
 					newSelection -= visibleRows;
 					if (newSelection < 0)
 						newSelection= 0;
 					break;
 					
 				case SWT.HOME :
 					newSelection= 0;
 					break;
 					
 				case SWT.END :
 					newSelection= fProposalTable.getItemCount() - 1;
 					break;
 					
 				default :
 					if (e.keyCode != SWT.MOD1 && e.keyCode != SWT.MOD2 && e.keyCode != SWT.MOD3 && e.keyCode != SWT.MOD4)
 						hide();
 					return true;
 			}
 			
 			selectProposal(newSelection, smartToggle);
 			
 			e.doit= false;
 			return false;
 
 		} else {
 			
 			switch (key) {
 				case 0x1B: // Esc
 					e.doit= false;
 					hide();
 					break;
 					
 				case '\n': // Ctrl-Enter on w2k
 				case '\r': // Enter
 					e.doit= false;
 					selectProposalWithMask(e.stateMask);
 					break;
 				
 				case '\t':
 					e.doit= false;
 					fProposalShell.setFocus();
 					return false;
 					
 				default:			
 					ICompletionProposal p= getSelectedProposal();
 					if (p instanceof ICompletionProposalExtension) {
 						ICompletionProposalExtension t= (ICompletionProposalExtension) p;
 						char[] triggers= t.getTriggerCharacters();
 						if (contains(triggers, key)) {		
 							e.doit= false;
 							hide();
 							insertProposal(p, key, e.stateMask, fContentAssistSubjectAdapter.getSelectedRange().x);
 						}
 					}
 			}
 		}
 		
 		return true;
 	}
 	
 	/**
 	 * Selects the entry with the given index in the proposal selector and feeds
 	 * the selection to the additional info controller.
 	 * 
 	 * @param index the index in the list
 	 * @param smartToggle <code>true</code> if the smart toogle key has been pressed
 	 * @since 2.1
 	 */
 	private void selectProposal(int index, boolean smartToggle) {
 
		if (fFilteredProposals == null)
			return;
		
 		ICompletionProposal oldProposal= getSelectedProposal();
 		if (oldProposal instanceof ICompletionProposalExtension2 && fViewer != null)
 			((ICompletionProposalExtension2) oldProposal).unselected(fViewer);
 
 		ICompletionProposal proposal= fFilteredProposals[index];
 		if (proposal instanceof ICompletionProposalExtension2 && fViewer != null)
 			((ICompletionProposalExtension2) proposal).selected(fViewer, smartToggle);
 		
 		fLastProposal= proposal;
 		
 		fProposalTable.setSelection(index);
 		fProposalTable.showSelection();
 		if (fAdditionalInfoController != null)
 			fAdditionalInfoController.handleTableSelectionChanged();
 	}
 	
 	/**
 	 * Returns whether the given character is contained in the given array of 
 	 * characters.
 	 * 
 	 * @param characters the list of characters
 	 * @param c the character to look for in the list
 	 * @return <code>true</code> if character belongs to the list
 	 * @since 2.0
 	 */
 	private boolean contains(char[] characters, char c) {
 		
 		if (characters == null)
 			return false;
 			
 		for (int i= 0; i < characters.length; i++) {
 			if (c == characters[i])
 				return true;
 		}
 		
 		return false;
 	}
 	
 	/*
 	 * @see IEventConsumer#processEvent(VerifyEvent)
 	 */
 	public void processEvent(VerifyEvent e) {
 	}
 	
 	/**
 	 * Filters the displayed proposal based on the given cursor position and the 
 	 * offset of the original invocation of the content assistant.
 	 */
 	private void filterProposals() {
 		++ fInvocationCounter;
 		Control control= fContentAssistSubjectAdapter.getControl();
 		control.getDisplay().asyncExec(new Runnable() {
 			long fCounter= fInvocationCounter;
 			public void run() {
 				
 				if (fCounter != fInvocationCounter) return;
 				
 				int offset= fContentAssistSubjectAdapter.getSelectedRange().x;
 				ICompletionProposal[] proposals= null;
 				try  {
 					if (offset > -1) {
 						DocumentEvent event= TextUtilities.mergeProcessedDocumentEvents(fDocumentEvents);
 						proposals= computeFilteredProposals(offset, event);
 					}
 				} catch (BadLocationException x)  {
 				} finally  {
 					fDocumentEvents.clear();
 				}
 				fFilterOffset= offset;
 				
 				if (proposals != null && proposals.length > 0)
 					setProposals(proposals);
 				else
 					hide();
 			}
 		});
 	}
 	
 	/**
 	 * Computes the subset of already computed propsals that are still valid for
 	 * the given offset.
 	 * 
 	 * @param offset the offset
 	 * @param event the merged document event
 	 * @return the set of filtered proposals
 	 * @since 2.0
 	 */
 	private ICompletionProposal[] computeFilteredProposals(int offset, DocumentEvent event) {
 		
 		if (offset == fInvocationOffset && event == null)
 			return fComputedProposals;
 			
 		if (offset < fInvocationOffset) {
 			fInvocationOffset= offset;
 			fComputedProposals= computeProposals(fInvocationOffset);
 			return fComputedProposals;
 		}
 		
 		ICompletionProposal[] proposals= fComputedProposals;
 		if (offset > fFilterOffset)
 			proposals= fFilteredProposals;
 			
 		if (proposals == null)
 			return null;
 			
 		IDocument document= fContentAssistSubjectAdapter.getDocument();
 		int length= proposals.length;
 		List filtered= new ArrayList(length);
 		for (int i= 0; i < length; i++) {
 				
 			if (proposals[i] instanceof ICompletionProposalExtension2) {
 
 				ICompletionProposalExtension2 p= (ICompletionProposalExtension2) proposals[i];				
 				if (p.validate(document, offset, event))
 					filtered.add(p);
 			
 			} else if (proposals[i] instanceof ICompletionProposalExtension) {
 								
 				ICompletionProposalExtension p= (ICompletionProposalExtension) proposals[i];
 				if (p.isValidFor(document, offset))
 					filtered.add(p);
 					
 			} else {
 				// restore original behavior
 				fInvocationOffset= offset;
 				fComputedProposals= computeProposals(fInvocationOffset);
 				return fComputedProposals;
 			}
 		}
 		
 		ICompletionProposal[] p= new ICompletionProposal[filtered.size()];
 		filtered.toArray(p); 		
 		return p;
 	}
 
 	/**
 	 * Requests the proposal shell to take focus.
 	 * 
 	 * @since 3.0
 	 */
 	public void setFocus() {
 		if (Helper.okToUse(fProposalShell)) {
 			fProposalShell.setFocus();
 		}		
 	}
 	
 	/**
 	 * Completes the common prefix of all proposals directly in the code. If no
 	 * common prefix can be found, the proposal popup is shown.
 	 * 
 	 * @return an error message if completion failed.
 	 * @since 3.0
 	 */
 	public String incrementalComplete() {
 		if (Helper.okToUse(fProposalShell) && fFilteredProposals != null) {
 			completeCommonPrefix();
 		} else {
 			final Control control= fContentAssistSubjectAdapter.getControl();
 			
 			BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
 				public void run() {
 					
 					fInvocationOffset= fContentAssistSubjectAdapter.getSelectedRange().x;
 					fFilterOffset= fInvocationOffset;
 					fFilteredProposals= computeProposals(fInvocationOffset);
 					
 					int count= (fFilteredProposals == null ? 0 : fFilteredProposals.length);
 					if (count == 0)
 						control.getDisplay().beep();
 					else if (count == 1 && fContentAssistant.isAutoInserting())
 						insertProposal(fFilteredProposals[0], (char) 0, 0, fInvocationOffset);
 					else {
 						if (fLineDelimiter == null)
 							fLineDelimiter= fContentAssistSubjectAdapter.getLineDelimiter();
 						
 						if (completeCommonPrefix())
 							unregister(); // TODO add some caching? for now: just throw away the completions
 						else {
 							if (fKeyListener == null) {
 								fKeyListener= new MyKeyListener();
 							}
 
 							if (!control.isDisposed())
 								fContentAssistSubjectAdapter.addKeyListener(fKeyListener);
 
 							fComputedProposals= fFilteredProposals;
 							createProposalSelector();
 							setProposals(fComputedProposals);
 							displayProposals();
 						}
 					}
 				}
 			});
 		}
 		return getErrorMessage();
 	}
 	
 	/**
 	 * Acts upon <code>fFilteredProposals</code>: if there is just one valid 
 	 * proposal, it is inserted, otherwise, the common prefix of all proposals
 	 * is inserted into the document. If there is no common prefix, <code>false</code>
 	 * is returned.
 	 * 
 	 * @return <code>true</code> if common prefix insertion was successful, <code>false</code> otherwise
 	 * @since 3.0
 	 */
 	private boolean completeCommonPrefix() {
 		
 		// 0: insert single proposals
 		if (fFilteredProposals.length == 1) {
 			insertProposal(fFilteredProposals[0], (char) 0, 0, fInvocationOffset);
 			hide();
 			return true;
 		}
 		
 		// 1: get the common ignore-case prefix of all remaining proposals
 		// note that the prefix still 
 		StringBuffer prefix= null; // the common prefix
 		boolean isCaseCompatible= true;
 		IDocument document= fContentAssistSubjectAdapter.getDocument();
 		int startOffset= -1; // the location where the proposals would insert (< fInvocationOffset if invoked in the middle of an ident)
 		String currentPrefix= null; // the prefix already in the document
 		int currentPrefixLen= -1; // the length of the current prefix
 		List caseFiltered= new ArrayList();
 		
 		for (int i= 0; i < fFilteredProposals.length; i++) {
 			ICompletionProposal proposal= fFilteredProposals[i];
 			CharSequence insertion= getReplacementString(proposal);
 			
 			if (currentPrefix == null) {
 				startOffset= getReplacementOffset(proposal);
 				currentPrefixLen= fFilterOffset - startOffset;
 				try {
 					// make sure we get the right case
 					currentPrefix= document.get(startOffset, currentPrefixLen);
 				} catch (BadLocationException e1) {
 					// bail out silently
 					return false;
 				}
 			}
 			
 			// prune ignore-case matches
 			if (isCaseSensitive() && !insertion.toString().startsWith(currentPrefix))
 				continue;
 			
 			caseFiltered.add(proposal);
 
 			if (prefix == null)
 				prefix= new StringBuffer(insertion.toString()); // initial
 			else 
 				isCaseCompatible &= truncatePrefix(prefix, insertion);
 			
 			// early break computation if there is nothing left to check
 			if (prefix.length() == 0)
 				break;
 		}
 		
 		if (prefix == null || currentPrefixLen > prefix.length() || prefix.toString().equals(currentPrefix))
 			return false;
 	
 		// 2: replace / insert the common prefix in the document
 		
 		if (caseFiltered.size() == 1) {
 			insertProposal((ICompletionProposal) caseFiltered.get(0), (char) 0, 0, fInvocationOffset);
 			hide();
 			return true;
 		}
 		
 		try {
 			String presentPart= prefix.substring(0, currentPrefixLen);
 			int replaceOffset;
 			int replaceLen;
 			if (isCaseCompatible && !currentPrefix.equals(presentPart)) {
 				// update case
 				currentPrefixLen= 0;
 				replaceOffset= startOffset;
 				replaceLen= fFilterOffset - startOffset;
 			} else {
 				// only insert remaining part
 				replaceOffset= fFilterOffset;
 				replaceLen= 0;
 			}
 			
 			int remainingLen= prefix.length() - currentPrefixLen;
 			String remainingPrefix= prefix.subSequence(currentPrefixLen, currentPrefixLen + remainingLen).toString();
 			
 			document.replace(replaceOffset, replaceLen, remainingPrefix);
 			
 			fContentAssistSubjectAdapter.setSelectedRange(replaceOffset + remainingLen, 0);
 			fContentAssistSubjectAdapter.revealRange(replaceOffset + remainingLen, 0);
 			
 			return true;
 		} catch (BadLocationException e) {
 			// ignore and return false
 			return false;
 		}
 	}
 
 	/**
 	 * Truncates <code>prefix</code> to the longest prefix it has in common with
 	 * <code>sequence</code> and returns <code>true</code> if the common prefix
 	 * has the same case for <code>prefix</code> and <code>sequence</code>.
 	 * 
 	 * @param prefix the previous prefix that will get truncated to the prefix it has in common with <code>sequence</code>
 	 * @param sequence the character sequence to match
 	 * @return <code>true</code> if the match is case compatible, <code>false</code> if the common prefix differs in case
 	 * @since 3.0
 	 */
 	private boolean truncatePrefix(StringBuffer prefix, CharSequence sequence) {
 		// find common prefix
 		int min= Math.min(prefix.length(), sequence.length());
 		boolean caseCompatible= true;
 		for (int c= 0; c < min; c++) {
 			char compareChar= sequence.charAt(c);
 			char prefixChar= prefix.charAt(c);
 			if (prefixChar != compareChar) {
 				if (isCaseSensitive() || Character.toLowerCase(prefixChar) != Character.toLowerCase(compareChar)) {
 					prefix.delete(c, prefix.length());
 					return caseCompatible;
 				} else 
 					caseCompatible= false;
 			}
 		}
 		
 		prefix.delete(min, prefix.length());
 		return caseCompatible;
 	}
 
 	/**
 	 * Returns whether common prefix completion should be case sensitive or not. 
 	 * Returns <code>true</code> if no proposal popup is currently showing, <code>false</code> if there is.
 	 * 
 	 * @return <code>true</code> if common prefix completion should be case sensitive, <code>false</code> otherwise
 	 * @since 3.0
 	 */
 	private boolean isCaseSensitive() {
 		return !Helper.okToUse(fProposalShell);
 	}
 
 	/**
 	 * Extracts the completion offset of an <code>ICompletionProposal</code>. If
 	 * <code>proposal</code> is a <code>ICompletionProposalExtension3</code>, its
 	 * <code>getCompletionOffset</code> method is called, otherwise, the invocation
 	 * offset of this popup is shown.
 	 * 
 	 * @param proposal the proposal to extract the offset from
 	 * @return the proposals completion offset, or <code>fInvocationOffset</code>
 	 */
 	private int getReplacementOffset(ICompletionProposal proposal) {
 		if (proposal instanceof ICompletionProposalExtension3)
 			return ((ICompletionProposalExtension3) proposal).getCompletionOffset();
 		else
 			return fInvocationOffset;	
 	}
 
 	/**
 	 * Extracts the replacement string from an <code>ICompletionProposal</code>.
 	 *  If <code>proposal</code> is a <code>ICompletionProposalExtension3</code>, its
 	 * <code>getCompletionText</code> method is called, otherwise, the display
 	 * string is used.
 	 * 
 	 * @param proposal the proposal to extract the text from
 	 * @return the proposals completion text
 	 * @since 3.0
 	 */
 	private CharSequence getReplacementString(ICompletionProposal proposal) {
 		CharSequence insertion= null;
 		if (proposal instanceof ICompletionProposalExtension3)
 			insertion= ((ICompletionProposalExtension3) proposal).getCompletionText();
 		
 		if (insertion == null)
 			insertion= proposal.getDisplayString();
 		
 		return insertion;
 	}
 }
