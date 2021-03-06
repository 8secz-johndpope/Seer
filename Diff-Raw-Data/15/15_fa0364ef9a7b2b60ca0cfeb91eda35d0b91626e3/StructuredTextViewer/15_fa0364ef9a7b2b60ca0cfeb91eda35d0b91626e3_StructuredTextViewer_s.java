 /*******************************************************************************
  * Copyright (c) 2001, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *     Jens Lukowski/Innoopract - initial renaming/restructuring
  *     
  *******************************************************************************/
 package org.eclipse.wst.sse.ui;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.IAutoEditStrategy;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IDocumentAdapter;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.ITextViewerExtension2;
 import org.eclipse.jface.text.Position;
 import org.eclipse.jface.text.Region;
 import org.eclipse.jface.text.TextSelection;
 import org.eclipse.jface.text.contentassist.IContentAssistant;
 import org.eclipse.jface.text.formatter.FormattingContext;
 import org.eclipse.jface.text.formatter.FormattingContextProperties;
 import org.eclipse.jface.text.formatter.IContentFormatterExtension;
 import org.eclipse.jface.text.formatter.IFormattingContext;
 import org.eclipse.jface.text.information.IInformationPresenter;
 import org.eclipse.jface.text.projection.ChildDocument;
 import org.eclipse.jface.text.projection.ProjectionDocument;
 import org.eclipse.jface.text.reconciler.IReconciler;
 import org.eclipse.jface.text.source.IAnnotationModel;
 import org.eclipse.jface.text.source.IOverviewRuler;
 import org.eclipse.jface.text.source.ISourceViewer;
 import org.eclipse.jface.text.source.IVerticalRuler;
 import org.eclipse.jface.text.source.SourceViewer;
 import org.eclipse.jface.text.source.SourceViewerConfiguration;
 import org.eclipse.jface.viewers.ContentViewer;
 import org.eclipse.jface.viewers.DoubleClickEvent;
 import org.eclipse.jface.viewers.IDoubleClickListener;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.VerifyEvent;
 import org.eclipse.swt.events.VerifyListener;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.texteditor.IEditorStatusLine;
 import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
 import org.eclipse.wst.sse.core.IStructuredModel;
 import org.eclipse.wst.sse.core.IndexedRegion;
 import org.eclipse.wst.sse.core.cleanup.StructuredContentCleanupHandler;
 import org.eclipse.wst.sse.core.text.IStructuredDocument;
 import org.eclipse.wst.sse.core.undo.IDocumentSelectionMediator;
 import org.eclipse.wst.sse.core.undo.UndoDocumentEvent;
 import org.eclipse.wst.sse.ui.extension.IExtendedSimpleEditor;
 import org.eclipse.wst.sse.ui.internal.Logger;
 import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
 import org.eclipse.wst.sse.ui.style.IHighlighter;
 import org.eclipse.wst.sse.ui.util.PlatformStatusLineUtil;
 import org.eclipse.wst.sse.ui.view.events.INodeSelectionListener;
 import org.eclipse.wst.sse.ui.view.events.NodeSelectionChangedEvent;
 import org.w3c.dom.Attr;
 import org.w3c.dom.Node;
 
 public class StructuredTextViewer extends SourceViewer implements INodeSelectionListener, IDoubleClickListener, IDocumentSelectionMediator {
 
 	/**
 	 * Internal verify listener.
 	 */
 	class TextVerifyListener implements VerifyListener {
 
 		/**
 		 * Indicates whether verify events are forwarded or ignored.
 		 * 
 		 * @since 2.0
 		 */
 		private boolean fForward = true;
 
 		/**
 		 * Tells the listener to forward received events.
 		 * 
 		 * @param forward
 		 *            <code>true</code> if forwarding should be enabled.
 		 * @since 2.0
 		 */
 		public void forward(boolean forward) {
 			fForward = forward;
 		}
 
 		/*
 		 * @see VerifyListener#verifyText(VerifyEvent)
 		 */
 		public void verifyText(VerifyEvent e) {
 			if (fForward) {
 				handleVerifyEvent(e);
 			}
 		}
 	}
 
 	/** Text operation codes */
 	public static final int CLEANUP_DOCUMENT = ISourceViewer.INFORMATION + 1;
 	public static final int FORMAT_ACTIVE_ELEMENTS = ISourceViewer.INFORMATION + 3;
 	private static final String FORMAT_ACTIVE_ELEMENTS_TEXT = SSEUIPlugin.getResourceString("%Format_Active_Elements_UI_"); //$NON-NLS-1$
 	public static final int FORMAT_DOCUMENT = ISourceViewer.INFORMATION + 2;
 
 	private static final String FORMAT_DOCUMENT_TEXT = SSEUIPlugin.getResourceString("%Format_Document_UI_"); //$NON-NLS-1$
 	public static final int QUICK_FIX = ISourceViewer.INFORMATION + 4;
 	private static final String TEXT_CUT = SSEUIPlugin.getResourceString("%Text_Cut_UI_"); //$NON-NLS-1$
 	private static final String TEXT_PASTE = SSEUIPlugin.getResourceString("%Text_Paste_UI_"); //$NON-NLS-1$
 	private static final String TEXT_SHIFT_LEFT = SSEUIPlugin.getResourceString("%Text_Shift_Left_UI_"); //$NON-NLS-1$ = "Text Shift Left"
 	private static final String TEXT_SHIFT_RIGHT = SSEUIPlugin.getResourceString("%Text_Shift_Right_UI_"); //$NON-NLS-1$ = "Text Shift Right"
 	private boolean fBackgroundupdateInProgress;
 	protected StructuredContentCleanupHandler fContentCleanupHandler = null;
 	protected IContentAssistant fCorrectionAssistant;
 	protected boolean fCorrectionAssistantInstalled;
 	private IDocumentAdapter fDocAdapter;
 	/**
 	 * TODO Temporary workaround for BUG44665
 	 */
 	/** The most recent widget modification as document command */
 	private StructuredDocumentCommand fDocumentCommand = new StructuredDocumentCommand();
 	private IHighlighter fHighlighter;
 	/**
 	 * @deprecated
 	 */
 	private IStructuredModel fModel;
 	// TODO: never read locally
 	boolean fRememberedStateContentAssistInstalled;
 
 	/**
 	 * TODO Temporary workaround for BUG44665
 	 */
 	/** Verify listener */
 	private TextVerifyListener fVerifyListener = new TextVerifyListener();
 
 	private ViewerSelectionManager fViewerSelectionManager;
 
 	/**
 	 * ModelViewer constructor comment.
 	 * 
 	 * @param parent
 	 *            org.eclipse.swt.widgets.Composite
 	 * @param ruler
 	 *            org.eclipse.jface.text.source.IVerticalRuler
 	 * @param styles
 	 *            int
 	 */
 	public StructuredTextViewer(Composite parent, IVerticalRuler verticalRuler, int styles) {
 
 		super(parent, verticalRuler, styles);
 	}
 
 	/**
 	 * @see org.eclipse.jface.text.source.SourceViewer#SourceViewer(Composite,
 	 *      IVerticalRuler, IOverviewRuler, boolean, int)
 	 */
 	public StructuredTextViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles) {
 
 		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
 	}
 
 	/**
 	 *  
 	 */
 	private void beep() {
 
 		getTextWidget().getDisplay().beep();
 	}
 
 	void beginBackgroundUpdate() {
 		fBackgroundupdateInProgress = true;
 		disableRedrawing();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.ITextOperationTarget#canDoOperation(int)
 	 */
 	public boolean canDoOperation(int operation) {
 		if (fBackgroundupdateInProgress) {
 			return false;
 		}
 		switch (operation) {
 			case CONTENTASSIST_PROPOSALS : {
 				// (pa) if position isn't READ_ONLY (containsReadOnly()
 				// returns false),
 				// Otherwise, you DO want content assist (return true)
 				IDocument doc = getDocument();
 				if (doc != null && doc instanceof IStructuredDocument) {
					return (!((IStructuredDocument) doc).containsReadOnly(getSelectedRange().x, 0));
 				}
 				break;
 			}
 			case CONTENTASSIST_CONTEXT_INFORMATION : {
 				return true;
 			}
 			case QUICK_FIX : {
				return true;
 			}
 			case INFORMATION : {
 				// the fInformationPresenter may not be set yet but you DO
 				// want information
 				// (this needs to be set to TRUE so menu item can become
 				// active)
 				return true;
 			}
 			case CLEANUP_DOCUMENT : {
 				return (fContentCleanupHandler != null && isEditable());
 			}
 			case FORMAT_DOCUMENT :
 			case FORMAT_ACTIVE_ELEMENTS : {
 				return (fContentFormatter != null && isEditable());
 			}
 		}
 		return super.canDoOperation(operation);
 	}
 
 	/**
 	 * Should be identical to superclass version. Also, we get the tab width
 	 * from the preference manager. Plus, we get our own special Highlighter.
 	 * Plus we uninstall before installing.
 	 */
 	public void configure(SourceViewerConfiguration configuration) {
 
 		if (getTextWidget() == null)
 			return;
 
 		setDocumentPartitioning(configuration.getConfiguredDocumentPartitioning(this));
 
 		if (configuration instanceof StructuredTextViewerConfiguration) {
 			if (fHighlighter != null) {
 				fHighlighter.uninstall();
 			}
 			fHighlighter = ((StructuredTextViewerConfiguration) configuration).getHighlighter(this);
 			fHighlighter.install(this);
 		}
 
 		// install content type independent plugins
 		if (fPresentationReconciler != null)
 			fPresentationReconciler.uninstall();
 		fPresentationReconciler = configuration.getPresentationReconciler(this);
 		if (fPresentationReconciler != null)
 			fPresentationReconciler.install(this);
 
 		IReconciler newReconciler = configuration.getReconciler(this);
 
 		if (newReconciler != fReconciler || newReconciler == null || fReconciler == null) {
 
 			if (fReconciler != null) {
 				fReconciler.uninstall();
 			}
 
 			fReconciler = newReconciler;
 
 			if (fReconciler != null) {
 				((StructuredTextReconciler) fReconciler).setIsIncrementalReconciler(false);
 				fReconciler.install(this);
 				// https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=3858
 				// still need set document on the reconciler (strategies)
 				((StructuredTextReconciler) fReconciler).setDocument(getDocument());
 				((StructuredTextReconciler) fReconciler).setIsIncrementalReconciler(true);
 			}
 		}
 
 		if (fContentAssistant != null)
 			fContentAssistant.uninstall();
 		fContentAssistant = configuration.getContentAssistant(this);
 		if (fContentAssistant != null) {
 			fContentAssistant.install(this);
 			fContentAssistantInstalled = true;
 		} else {
 			// 248036
 			// disable the content assist operation if no content assistant
 			enableOperation(CONTENTASSIST_PROPOSALS, false);
 		}
 
 		// correction assistant
 		if (configuration instanceof StructuredTextViewerConfiguration) {
 			if (fCorrectionAssistant != null)
 				fCorrectionAssistant.uninstall();
 			fCorrectionAssistant = ((StructuredTextViewerConfiguration) configuration).getCorrectionAssistant(this);
 			if (fCorrectionAssistant != null) {
 				fCorrectionAssistant.install(this);
 				fCorrectionAssistantInstalled = true;
 			} else {
 				// disable the correction assist operation if no correction
 				// assistant
 				enableOperation(QUICK_FIX, false);
 			}
 		}
 
 		fContentFormatter = configuration.getContentFormatter(this);
 
 		// do not uninstall old information presenter if it's the same
 		IInformationPresenter newInformationPresenter = configuration.getInformationPresenter(this);
 		if (newInformationPresenter == null || fInformationPresenter == null || !(newInformationPresenter.equals(fInformationPresenter))) {
 			if (fInformationPresenter != null)
 				fInformationPresenter.uninstall();
 			fInformationPresenter = newInformationPresenter;
 			if (fInformationPresenter != null)
 				fInformationPresenter.install(this);
 		}
 
 		// disconnect from the old undo manager before setting the new one
 		if (fUndoManager != null) {
 			fUndoManager.disconnect();
 		}
 		setUndoManager(configuration.getUndoManager(this));
 
 		// TODO: compare with ?new? V2 configure re:
 		// 		getTextWidget().setTabs(configuration.getTabWidth(this));
 		// see if it can replace following
 		// Set tab width to configuration setting first.
 		// Then override if model type is XML or HTML.
 		getTextWidget().setTabs(configuration.getTabWidth(this));
 		setAnnotationHover(configuration.getAnnotationHover(this));
 		setOverviewRulerAnnotationHover(configuration.getOverviewRulerAnnotationHover(this));
 		// added for V2
 		setHoverControlCreator(configuration.getInformationControlCreator(this));
 		// install content type specific plugins
 		String[] types = configuration.getConfiguredContentTypes(this);
 
 		// clear autoindent/autoedit strategies
 		fAutoIndentStrategies = null;
 		for (int i = 0; i < types.length; i++) {
 			String t = types[i];
 			setAutoIndentStrategy(configuration.getAutoIndentStrategy(this, t), t);
 			setTextDoubleClickStrategy(configuration.getDoubleClickStrategy(this, t), t);
 
 			int[] stateMasks = configuration.getConfiguredTextHoverStateMasks(this, t);
 			if (stateMasks != null) {
 				for (int j = 0; j < stateMasks.length; j++) {
 					int stateMask = stateMasks[j];
 					setTextHover(configuration.getTextHover(this, t, stateMask), t, stateMask);
 				}
 			} else {
 				setTextHover(configuration.getTextHover(this, t), t, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
 			}
 
 			String[] prefixes = configuration.getIndentPrefixes(this, t);
 			if (prefixes != null && prefixes.length > 0)
 				setIndentPrefixes(prefixes, t);
 			// removed 'defaultPrefix' for Eclipse V2 replaced with
 			// defaultPrefixes
 			/*
 			 * String prefix = configuration.getDefaultPrefix(this, t); if
 			 * (prefix != null && prefix.length() > 0)
 			 * setDefaultPrefix(prefix, t);
 			 */
 			prefixes = configuration.getDefaultPrefixes(this, t);
 			if (prefixes != null && prefixes.length > 0)
 				setDefaultPrefixes(prefixes, t);
 		}
 		activatePlugins();
 
 		// do any StructuredTextViewer-specific configuration
 		if (configuration instanceof StructuredTextViewerConfiguration) {
 			Map autoEditStrategies = ((StructuredTextViewerConfiguration) configuration).getAutoEditStrategies(this);
 			if (autoEditStrategies != null) {
 				Iterator partitionTypes = autoEditStrategies.keySet().iterator();
 				while (partitionTypes.hasNext()) {
 					String partitionType = partitionTypes.next().toString();
 					Object strategies = autoEditStrategies.get(partitionType);
 					if (strategies != null) {
 						if (strategies instanceof List) {
 							for (int i = 0; i < ((List) strategies).size(); i++) {
 								IAutoEditStrategy strategy = (IAutoEditStrategy) ((List) strategies).get(i);
 								prependAutoEditStrategy(strategy, partitionType);
 							}
 						}
 					}
 				}
 			}
 		}
 	}
 
 	/**
 	 * @param document
 	 * @param startOffset
 	 * @param endOffset
 	 * @return
 	 */
 	private boolean containsReadOnly(IDocument document, int startOffset, int endOffset) {
 
 		int start = startOffset;
 		int end = endOffset;
 		IStructuredDocument structuredDocument = null;
 		if (document instanceof IStructuredDocument) {
 			structuredDocument = (IStructuredDocument) document;
 		} else {
 			if (document instanceof ProjectionDocument) {
 				IDocument doc = ((ProjectionDocument) document).getMasterDocument();
 				if (doc instanceof IStructuredDocument) {
 					structuredDocument = (IStructuredDocument) doc;
 					int adjust = ((ProjectionDocument) document).getProjectionMapping().getCoverage().getOffset();
 					start = adjust + start;
 					end = adjust + end;
 				}
 			}
 		}
 		if (structuredDocument == null) {
 			return false;
 		} else {
 			int length = end - start;
 			return structuredDocument.containsReadOnly(start, length);
 		}
 	}
 
 	protected IDocumentAdapter createDocumentAdapter() {
 
 		fDocAdapter = new StructuredDocumentToTextAdapter(getTextWidget());
 		return fDocAdapter;
 	}
 
 	/**
 	 * TODO Temporary workaround for BUG44665
 	 */
 	protected void customizeDocumentCommand(StructuredDocumentCommand command) {
 		if (isIgnoringAutoEditStrategies())
 			return;
 
 		List strategies = (List) selectContentTypePlugin(command.offset, fAutoIndentStrategies);
 		if (strategies == null)
 			return;
 
 		switch (strategies.size()) {
 			// optimization
 			case 0 :
 				break;
 
 			case 1 :
 				((IAutoEditStrategy) strategies.iterator().next()).customizeDocumentCommand(getDocument(), command);
 				break;
 
 			// make iterator robust against adding/removing strategies from
 			// within
 			// strategies
 			default :
 				strategies = new ArrayList(strategies);
 
 				IDocument document = getDocument();
 				for (final Iterator iterator = strategies.iterator(); iterator.hasNext();)
 					((IAutoEditStrategy) iterator.next()).customizeDocumentCommand(document, command);
 
 				break;
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.ITextOperationTarget#doOperation(int)
 	 */
 	public void doOperation(int operation) {
 
 		Point selection = getTextWidget().getSelection();
 		int cursorPosition = selection.x;
 		int selectionLength = selection.y - selection.x;
 		switch (operation) {
 			case UNDO : {
 				IExtendedSimpleEditor editor = getActiveExtendedSimpleEditor();
 				if (editor != null) {
 					IStatus status = editor.validateEdit(getControl().getShell());
 					if (status != null && status.isOK())
 						undo();
 				} else
 					undo();
 				break;
 			}
 			case REDO : {
 				IExtendedSimpleEditor editor = getActiveExtendedSimpleEditor();
 				if (editor != null) {
 					IStatus status = editor.validateEdit(getControl().getShell());
 					if (status != null && status.isOK())
 						redo();
 				} else
 					redo();
 				break;
 			}
 			case CUT :
 				getModel().beginRecording(this, TEXT_CUT, TEXT_CUT, cursorPosition, selectionLength);
 				super.doOperation(operation);
 				selection = getTextWidget().getSelection();
 				cursorPosition = selection.x;
 				selectionLength = selection.y - selection.x;
 				getModel().endRecording(this, cursorPosition, selectionLength);
 				break;
 			case PASTE :
 				getModel().beginRecording(this, TEXT_PASTE, TEXT_PASTE, cursorPosition, selectionLength);
 				super.doOperation(operation);
 				selection = getTextWidget().getSelection();
 				cursorPosition = selection.x;
 				selectionLength = selection.y - selection.x;
 				getModel().endRecording(this, cursorPosition, selectionLength);
 				break;
 			case CONTENTASSIST_PROPOSALS :
 				// maybe not configured?
				if (fContentAssistant != null) {
 					// CMVC 263269
 					// need an explicit check here because the
 					// contentAssistAction is no longer being updated on
 					// cursor
 					// position
 					if (canDoOperation(CONTENTASSIST_PROPOSALS)) {
 						String err = fContentAssistant.showPossibleCompletions();
 						if (err != null) {
 							// don't wanna beep if there is no error
 							PlatformStatusLineUtil.displayErrorMessage(err);
 						}
 						PlatformStatusLineUtil.addOneTimeClearListener();
 					} else
 						beep();
 				}
 				break;
 			case CONTENTASSIST_CONTEXT_INFORMATION :
 				if (fContentAssistant != null) {
 					String err = fContentAssistant.showContextInformation();
 					PlatformStatusLineUtil.displayErrorMessage(err);
 					PlatformStatusLineUtil.addOneTimeClearListener();
 					//setErrorMessage(err);
 					//new OneTimeListener(getTextWidget(), new
 					// ClearErrorMessage());
 				}
 				break;
 			case QUICK_FIX :
				String msg = fCorrectionAssistant.showPossibleCompletions();
				setErrorMessage(msg);
 				break;
 			case SHIFT_RIGHT :
 				getModel().beginRecording(this, TEXT_SHIFT_RIGHT, TEXT_SHIFT_RIGHT, cursorPosition, selectionLength);
 				super.doOperation(SHIFT_RIGHT);
 				selection = getTextWidget().getSelection();
 				cursorPosition = selection.x;
 				selectionLength = selection.y - selection.x;
 				getModel().endRecording(this, cursorPosition, selectionLength);
 				break;
 			case SHIFT_LEFT :
 				getModel().beginRecording(this, TEXT_SHIFT_LEFT, TEXT_SHIFT_LEFT, cursorPosition, selectionLength);
 				super.doOperation(SHIFT_LEFT);
 				selection = getTextWidget().getSelection();
 				cursorPosition = selection.x;
 				selectionLength = selection.y - selection.x;
 				getModel().endRecording(this, cursorPosition, selectionLength);
 				break;
 			case FORMAT_DOCUMENT :
 				try {
 					// begin recording
 					getModel().beginRecording(this, FORMAT_DOCUMENT_TEXT, FORMAT_DOCUMENT_TEXT, cursorPosition, selectionLength);
 
 					// tell the model that we are about to make a big model
 					// change
 					fModel.aboutToChangeModel();
 
 					// format
 					IRegion region = getModelCoverage();
 					if (fContentFormatter instanceof IContentFormatterExtension) {
 						IContentFormatterExtension extension = (IContentFormatterExtension) fContentFormatter;
 						IFormattingContext context = new FormattingContext();
 						context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
 						context.setProperty(FormattingContextProperties.CONTEXT_REGION, region);
 						extension.format(getDocument(), context);
 					} else {
 						fContentFormatter.format(getDocument(), region);
 					}
 				} finally {
 					// tell the model that we are done with the big model
 					// change
 					fModel.changedModel();
 
 					// end recording
 					selection = getTextWidget().getSelection();
 					cursorPosition = selection.x;
 					selectionLength = selection.y - selection.x;
 					getModel().endRecording(this, cursorPosition, selectionLength);
 				}
 				break;
 			case FORMAT_ACTIVE_ELEMENTS :
 				try {
 					// begin recording
 					getModel().beginRecording(this, FORMAT_ACTIVE_ELEMENTS_TEXT, FORMAT_ACTIVE_ELEMENTS_TEXT, cursorPosition, selectionLength);
 
 					// tell the model that we are about to make a big model
 					// change
 					fModel.aboutToChangeModel();
 
 					// format
 					Point s = getSelectedRange();
 					IRegion region = new Region(s.x, s.y);
 					fContentFormatter.format(getDocument(), region);
 				} finally {
 					// tell the model that we are done with the big model
 					// change
 					fModel.changedModel();
 
 					// end recording
 					selection = getTextWidget().getSelection();
 					cursorPosition = selection.x;
 					selectionLength = selection.y - selection.x;
 					getModel().endRecording(this, cursorPosition, selectionLength);
 				}
 				break;
 			default :
 				super.doOperation(operation);
 		}
 	}
 
 	/**
 	 * Notifies of a double click.
 	 * 
 	 * @param event
 	 *            event object describing the double-click
 	 */
 	public void doubleClick(DoubleClickEvent event) {
 
 		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
 		int selectionSize = selection.size();
 		List selectedNodes = selection.toList();
 		IndexedRegion doubleClickedNode = null;
 		int selectionStart = 0;
 		int selectionEnd = 0;
 		if (selectionSize > 0) {
 			// something selected
 			// only one node can be double-clicked at a time
 			// so, we get node 0
 			doubleClickedNode = (IndexedRegion) selectedNodes.get(0);
 			selectionStart = doubleClickedNode.getStartOffset();
 			selectionEnd = doubleClickedNode.getEndOffset();
 			// set new selection
 			setSelectedRange(selectionStart, selectionEnd - selectionStart);
 		}
 	}
 
 	void endBackgroundUpdate() {
 		fBackgroundupdateInProgress = false;
 		enabledRedrawing();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.TextViewer#findAndSelect(int,
 	 *      java.lang.String, boolean, boolean, boolean, boolean)
 	 */
 	protected int findAndSelect(int startPosition, String findString, boolean forwardSearch, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {
 		int result = super.findAndSelect(startPosition, findString, forwardSearch, caseSensitive, wholeWord, regExSearch);
 
 		// findAndSelect calls fTextWidget.setSelectionRange(widgetPos,
 		// length) to set selection,
 		// which does not fire text widget selection event.
 		// Need to notify ViewerSelectionManager here.
 		notifyViewerSelectionManager(getSelectedRange().x, getSelectedRange().y);
 
 		return result;
 	}
 
 	protected IExtendedSimpleEditor getActiveExtendedSimpleEditor() {
 		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
 		if (window != null) {
 			IWorkbenchPage page = window.getActivePage();
 			if (page != null) {
 				IEditorPart editor = page.getActiveEditor();
 				if (editor != null && editor instanceof IExtendedSimpleEditor) {
 					return (IExtendedSimpleEditor) editor;
 				}
 			}
 		}
 		return null;
 	}
 
 
 	protected ViewerSelectionManager getDefaultViewerSelectionManager() {
 		return new ViewerSelectionManagerImpl(this);
 	}
 
 	/**
 	 * @deprecated -- will be removed in future.
 	 */
 	public IStructuredModel getModel() {
 		return fModel;
 	}
 
 	public ViewerSelectionManager getViewerSelectionManager() {
 
 		if (fViewerSelectionManager == null) {
 			ViewerSelectionManager viewerSelectionManager = getDefaultViewerSelectionManager();
 			// use setter instead of field directly, so it get initialized
 			// properly
 			setViewerSelectionManager(viewerSelectionManager);
 		}
 		return fViewerSelectionManager;
 	}
 
 	protected void handleDispose() {
 
 		Logger.trace("Source Editor", "StructuredTextViewer::handleDispose entry"); //$NON-NLS-1$ //$NON-NLS-2$
 
 		// before we dispose, we set a special "empty" selection, to prevent
 		// the "leak one document" that
 		// otherwise occurs when editor closed (since last selection stays in
 		// SelectedResourceManager.
 		// the occurance of the "leak" isn't so bad, but makes debugging other
 		// leaks very hard.
 		setSelection(TextSelection.emptySelection());
 
 		if (fViewerSelectionManager != null) {
 			fViewerSelectionManager.removeNodeDoubleClickListener(this);
 			fViewerSelectionManager.removeNodeSelectionListener(this);
 			fViewerSelectionManager.release();
 		}
 
 		if (fHighlighter != null) {
 			fHighlighter.uninstall();
 		}
 		super.handleDispose();
 		// todo: make this setModel(null)
 		fModel = null;
 		Logger.trace("Source Editor", "StructuredTextViewer::handleDispose exit"); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	/**
 	 * TODO Temporary workaround for BUG44665
 	 */
 	/**
 	 * @see VerifyListener#verifyText(VerifyEvent)
 	 */
 	protected void handleVerifyEvent(VerifyEvent e) {
 
 
 		if (fEventConsumer != null) {
 			fEventConsumer.processEvent(e);
 			if (!e.doit)
 				return;
 		}
 		if (fBackgroundupdateInProgress) {
 			e.doit = false;
 			beep();
 			return;
 		}
 		// for read-only support
 		if (containsReadOnly(getVisibleDocument(), e.start, e.end)) {
 			e.doit = false;
 			beep();
 			return;
 		}
 
 		IRegion modelRange = event2ModelRange(e);
 		fDocumentCommand.setEventStructuredDocumentEvent(e, modelRange);
 		customizeDocumentCommand(fDocumentCommand);
 		int widgetCaret = 0;
 		if (!fDocumentCommand.fillEventStructuredDocumentCommand(e, modelRange)) {
 
 			boolean compoundChange = fDocumentCommand.getCommandCount() > 1;
 			try {
 
 				fVerifyListener.forward(false);
 
 				if (compoundChange && fUndoManager != null)
 					fUndoManager.beginCompoundChange();
 
 				if (getSlaveDocumentManager() != null) {
 					IDocument visible = getVisibleDocument();
 					try {
 						getSlaveDocumentManager().setAutoExpandMode(visible, true);
 						fDocumentCommand.executeStructuredDocumentCommand(getDocument());
 					} finally {
 						getSlaveDocumentManager().setAutoExpandMode(visible, false);
 					}
 				} else {
 					fDocumentCommand.executeStructuredDocumentCommand(getDocument());
 				}
 
 				if (getTextWidget() != null) {
 					int documentCaret = fDocumentCommand.caretOffset;
 					if (documentCaret == -1) {
 						// old behavior of document command
 						documentCaret = fDocumentCommand.offset + (fDocumentCommand.text == null ? 0 : fDocumentCommand.text.length());
 					}
 
 					widgetCaret = modelOffset2WidgetOffset(documentCaret);
 					if (widgetCaret == -1) {
 						// try to move it to the closest spot
 						IRegion region = getModelCoverage();
 						if (documentCaret <= region.getOffset())
 							widgetCaret = 0;
 						else if (documentCaret >= region.getOffset() + region.getLength())
 							widgetCaret = getVisibleRegion().getLength();
 					}
 
 				}
 			} catch (BadLocationException x) {
 
 				if (TRACE_ERRORS)
 					System.out.println("TextViewer.error.bad_location.verifyText"); //$NON-NLS-1$
 
 			} finally {
 
 				if (compoundChange && fUndoManager != null)
 					fUndoManager.endCompoundChange();
 
 				if (widgetCaret != -1) {
 					// there is a valid widget caret
 					getTextWidget().setCaretOffset(widgetCaret);
 				}
 
 				getTextWidget().showSelection();
 
 				fVerifyListener.forward(true);
 
 			}
 		}
 	}
 
 	/**
 	 * TODO Temporary workaround for BUG44665
 	 */
 	/**
 	 * overridden for read-only support
 	 */
 	/*
 	 * protected void handleVerifyEvent(VerifyEvent e) { // for now, we'll let
 	 * super have a shot first // (may mess up undo stack, or something?)
 	 * 
 	 * super.handleVerifyEvent(e); if (containsReadOnly(getVisibleDocument(),
 	 * e.start, e.end)) { e.doit = false; beep(); } }
 	 */
 
 	/**
 	 * nodeSelectionChanged method comment.
 	 */
 	public void nodeSelectionChanged(NodeSelectionChangedEvent event) {
 
 		// Skip NodeSelectionChanged processing if this is the source of the
 		// event.
 		if (event.getSource().equals(this))
 			return;
 		List selectedNodes = new Vector(event.getSelectedNodes());
 		boolean attrOrTextNodeSelected = false;
 		int attrOrTextNodeStartOffset = 0;
 		for (int i = 0; i < selectedNodes.size(); i++) {
 			Object eachNode = selectedNodes.get(i);
 			// replace attribute node with its parent
 			if (eachNode instanceof Attr) {
 				attrOrTextNodeSelected = true;
 				attrOrTextNodeStartOffset = ((IndexedRegion) eachNode).getStartOffset();
 				selectedNodes.set(i, ((Attr) eachNode).getOwnerElement());
 			}
 			// replace TextNode with its parent
 			if ((eachNode instanceof Node) && (((Node) eachNode).getNodeType() == Node.TEXT_NODE)) {
 				attrOrTextNodeSelected = true;
 				attrOrTextNodeStartOffset = ((IndexedRegion) eachNode).getStartOffset();
 				selectedNodes.set(i, ((Node) eachNode).getParentNode());
 			}
 		}
 		if (nothingToSelect(selectedNodes)) {
 			removeRangeIndication();
 		} else {
 			IndexedRegion startNode = (IndexedRegion) selectedNodes.get(0);
 			IndexedRegion endNode = (IndexedRegion) selectedNodes.get(selectedNodes.size() - 1);
 			int startOffset = startNode.getStartOffset();
 			int endOffset = endNode.getEndOffset();
 			// if end node is a child node of start node
 			if (startNode.getEndOffset() > endNode.getEndOffset()) {
 				endOffset = startNode.getEndOffset();
 			}
 			int length = endOffset - startOffset;
 			// Move cursor only if the original source really came from
 			// a ContentViewer (for example, the SourceEditorTreeViewer or the
 			// XMLTableTreeViewer)
 			// or a ContentOutlinePage (for example, the XSDTreeViewer).
 			// Do not move the cursor if the source is a textWidget (which
 			// means the selection came from the text viewer) or
 			// if the source is the ViewerSelectionManager (which means the
 			// selection was set programmatically).
 			boolean moveCursor = (event.getSource() instanceof ContentViewer) || (event.getSource() instanceof IContentOutlinePage);
 			// 20031012 (pa)
 			// Changed moveCursor to "false" because it was causing the cursor
 			// to jump to the beginning of the parent node in the case that a
 			// child of the parent is deleted.
 			// We really only want to set the range indicator on the left to
 			// the range of the parent, but not move the cursor
 			//setRangeIndication(startOffset, length, false);
 			// 20040714 (nsd) Chnaged back to tru given that selection
 			// problems
 			// caused by the Outline view appear fixed.
 			setRangeIndication(startOffset, length, moveCursor);
 			if ((moveCursor) && (attrOrTextNodeSelected)) {
 				setSelectedRange(attrOrTextNodeStartOffset, 0);
 				revealRange(attrOrTextNodeStartOffset, 0);
 			}
 			//			if(moveCursor) {
 			//				System.out.print("moving");
 			//			}
 			//			else {
 			//				System.out.print("not moving");
 			//			}
 			//			System.out.println(" on NodeSelectionEvent: " +
 			// event.getSource());
 		}
 	}
 
 	/**
 	 * @param selectedNodes
 	 * @return whether the IndexedNodes within the list should form a
 	 *         selectionrange
 	 */
 	private boolean nothingToSelect(List selectedNodes) {
 		if (selectedNodes == null || selectedNodes.isEmpty() || selectedNodes.get(0) == null) // empty
 			// selections
 			return true;
 		if (getDocument() == null) // viewer shutdown
 			return true;
 		// if the range would be the entire document's length, there's nothing
 		// to show
 		IndexedRegion firstIndexedNode = (IndexedRegion) selectedNodes.get(0);
 		return firstIndexedNode.getEndOffset() - firstIndexedNode.getStartOffset() >= getDocument().getLength();
 	}
 
 	/**
 	 * Notify the ViewerSelectionManager when text is selected
 	 * programmatically, for example, by double-click processing or an editor
 	 * action like Edit->SelectAll
 	 */
 	protected void notifyViewerSelectionManager(int offset, int length) {
 		if (fViewerSelectionManager != null) {
 			Event event = new Event();
 			event.widget = getTextWidget();
 			// sometimes null while closing
 			if (event.widget != null) {
 				SelectionEvent selectionEvent = new SelectionEvent(event);
 				selectionEvent.x = offset;
 				selectionEvent.y = offset + length;
 				fViewerSelectionManager.widgetSelected(selectionEvent);
 			}
 		}
 	}
 
 	private void redo() {
 		ignoreAutoEditStrategies(true);
 		fUndoManager.redo();
 		ignoreAutoEditStrategies(false);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.source.ISourceViewer#setDocument(org.eclipse.jface.text.IDocument,
 	 *      org.eclipse.jface.text.source.IAnnotationModel, int, int)
 	 */
 	public void setDocument(IDocument document, IAnnotationModel annotationModel, int modelRangeOffset, int modelRangeLength) {
 
 
 		// partial fix for:
 		// https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=1970
 		// when our document is set, especially to null during close,
 		// immediately uninstall the reconciler.
 		// this is to avoid an unnecessary final "reconcile"
 		// that blocks display thread
 		if (document == null) {
 			if (fReconciler != null) {
 				fReconciler.uninstall();
 			}
 		}
 
 		// by setting not incremental before setting document
 		// we can ensure that the AbstractReconciler will not add to its
 		// dirty region queue which the UI thread will wait for to empty
 		// before the editor comes up
 		if (fReconciler != null && document != null) {
 			((StructuredTextReconciler) fReconciler).setIsIncrementalReconciler(false);
 			fReconciler.install(this);
 		}
 
 		super.setDocument(document, annotationModel, modelRangeOffset, modelRangeLength);
 
 		if (fReconciler != null && document != null) {
 			// set back to incremental after the editor is up
 			// after "bypassing"
 			// AbstractReconciler.BackgroundThread#suspendCallerWhileDirty(...)
 			// and all other listeners are hooked up
 			((StructuredTextReconciler) fReconciler).setIsIncrementalReconciler(true);
 		}
 
 		if (document instanceof IStructuredDocument) {
 			IStructuredDocument structuredDocument = (IStructuredDocument) document;
 
 			// notify highlighter
 			if (fHighlighter != null) {
 				fHighlighter.setDocument(structuredDocument);
 				//fHighlighter.setModel(model);
 			}
 
 			// set document in the viewer-based undo manager
 			if (fUndoManager instanceof StructuredTextViewerUndoManager) {
 				((StructuredTextViewerUndoManager) fUndoManager).setDocument(structuredDocument);
 			}
 
 		}
 	}
 
 	/**
 	 * Use the active editor to set a status line message
 	 * 
 	 * @param msg
 	 */
 	protected void setErrorMessage(String msg) {
 		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
 		if (window != null) {
 			IWorkbenchPage page = window.getActivePage();
 			if (page != null) {
 				IEditorPart editor = page.getActiveEditor();
 				if (editor != null) {
 					IEditorStatusLine statusLine = (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
 					if (statusLine != null)
 						statusLine.setMessage(true, msg, null);
 				}
 			}
 		}
 	}
 
 	public void setModel(IStructuredModel model) {
 
 		setModel(model, null);
 	}
 
 	public void setModel(IStructuredModel model, IAnnotationModel annotationModel) {
 		// due to various forms of init, sometimes
 		// the same variable is set more than once
 		// with the same data, causing unneccesary updates, so
 		// we do nothing if someones' trying to set the same
 		// model we already have
 		// 
 		if ((fModel != null) && (fModel == model) && (getDocument() == model.getStructuredDocument())) {
 			return;
 		}
 		fModel = model;
 		setDocument(model.getStructuredDocument(), annotationModel);
 
 		// CaretEvent is not sent to ViewerSelectionManager after Save As.
 		// Need to notify ViewerSelectionManager here.
 		notifyViewerSelectionManager(getSelectedRange().x, getSelectedRange().y);
 	}
 
 	public void setViewerSelectionManager(ViewerSelectionManager viewerSelectionManager) {
 
 		// disconnect from old one
 		if (fViewerSelectionManager != null) {
 			fViewerSelectionManager.removeNodeDoubleClickListener(this);
 			fViewerSelectionManager.removeNodeSelectionListener(this);
 			fViewerSelectionManager.release();
 			// No need to removeSelectionChangedListener here. Done when
 			// editor
 			// calls "new ViewerSelectionManagerImpl(ITextViewer)".
 			//removeSelectionChangedListener(fViewerSelectionManager);
 		}
 		fViewerSelectionManager = viewerSelectionManager;
 		// connect to new one
 		if (fViewerSelectionManager != null) {
 			fViewerSelectionManager.addNodeDoubleClickListener(this);
 			fViewerSelectionManager.addNodeSelectionListener(this);
 			// No need to addSelectionChangedListener here. Done when editor
 			// calls "new ViewerSelectionManagerImpl(ITextViewer)".
 			//addSelectionChangedListener(fViewerSelectionManager);
 		}
 	}
 
 	/**
 	 * Uninstalls anything that was installed by configure
 	 */
 	public void unconfigure() {
 
 		Logger.trace("Source Editor", "StructuredTextViewer::unconfigure entry"); //$NON-NLS-1$ //$NON-NLS-2$
 		if (fHighlighter != null) {
 			fHighlighter.uninstall();
 		}
 
 		// presentationreconciler, reconciler, contentassist, infopresenter
 		// are all unconfigured in superclass so think about removing from
 		// here
 		if (fPresentationReconciler != null) {
 			fPresentationReconciler.uninstall();
 			fPresentationReconciler = null;
 		}
 		if (fReconciler != null) {
 			fReconciler.uninstall();
 			fReconciler = null;
 		}
 		if (fContentAssistant != null) {
 			fContentAssistant.uninstall();
 			fContentAssistantInstalled = false;
 		}
 		if (fInformationPresenter != null)
 			fInformationPresenter.uninstall();
 
 		// doesn't seem to be handled elsewhere, so we'll be sure error
 		// messages's are cleared.
 		setErrorMessage(null);
 
 		// unconfigure recently added to super class?!
 		super.unconfigure();
 		Logger.trace("Source Editor", "StructuredTextViewer::unconfigure exit"); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	private void undo() {
 		ignoreAutoEditStrategies(true);
 		fUndoManager.undo();
 		ignoreAutoEditStrategies(false);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.wst.sse.core.undo.IDocumentSelectionMediator#undoOperationSelectionChanged(org.eclipse.wst.sse.core.undo.UndoDocumentEvent)
 	 */
 	public void undoOperationSelectionChanged(UndoDocumentEvent event) {
 		if (event.getRequester() != null && event.getRequester().equals(this) && event.getDocument().equals(getDocument()))
 			setSelectedRange(event.getOffset(), event.getLength());
 	}
 
 	/**
 	 * This method added to override super's implementation so that
 	 * setVisibleRegion will not force a start at beginning of line. This was
 	 * primarily needed when used as embedded editor. May need to make more
 	 * sophisiticated if we need it to act both ways, depending on a flag, or
 	 * something.
 	 */
 	protected boolean updateVisibleDocument(IDocument visibleDocument, int visibleRegionOffset, int visibleRegionLength) throws BadLocationException {
 		if (visibleDocument instanceof ChildDocument) {
 			ChildDocument childDocument = (ChildDocument) visibleDocument;
 			//IDocument document = childDocument.getParentDocument();
 			//int line= document.getLineOfOffset(visibleRegionOffset);
 			int offset = visibleRegionOffset; //document.getLineOffset(line);
 			int length = visibleRegionLength; //(visibleRegionOffset -
 			// offset)
 			// + visibleRegionLength;
 			Position parentRange = childDocument.getParentDocumentRange();
 			if (offset != parentRange.getOffset() || length != parentRange.getLength()) {
 				childDocument.setParentDocumentRange(offset, length);
 				return true;
 			}
 		}
 		return false;
 	}
 }
