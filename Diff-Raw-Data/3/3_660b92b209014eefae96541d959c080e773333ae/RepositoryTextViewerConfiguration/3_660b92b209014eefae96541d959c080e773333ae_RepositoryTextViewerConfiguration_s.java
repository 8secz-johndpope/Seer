 /*******************************************************************************
  * Copyright (c) 2004, 2007 Mylyn project committers and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 
 package org.eclipse.mylyn.internal.tasks.ui.editors;
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
import org.eclipse.core.runtime.Assert;
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.jface.preference.JFacePreferences;
 import org.eclipse.jface.resource.JFaceResources;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.TextAttribute;
 import org.eclipse.jface.text.TextPresentation;
 import org.eclipse.jface.text.contentassist.ContentAssistant;
 import org.eclipse.jface.text.contentassist.IContentAssistant;
 import org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter;
 import org.eclipse.jface.text.hyperlink.IHyperlink;
 import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
 import org.eclipse.jface.text.presentation.IPresentationReconciler;
 import org.eclipse.jface.text.presentation.PresentationReconciler;
 import org.eclipse.jface.text.reconciler.IReconciler;
 import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
 import org.eclipse.jface.text.rules.IRule;
 import org.eclipse.jface.text.rules.IToken;
 import org.eclipse.jface.text.rules.MultiLineRule;
 import org.eclipse.jface.text.rules.RuleBasedScanner;
 import org.eclipse.jface.text.rules.SingleLineRule;
 import org.eclipse.jface.text.rules.Token;
 import org.eclipse.jface.text.source.ISourceViewer;
 import org.eclipse.mylyn.internal.tasks.ui.TaskListColorsAndFonts;
 import org.eclipse.mylyn.tasks.core.AbstractTask;
 import org.eclipse.mylyn.tasks.core.ITaskList;
 import org.eclipse.mylyn.tasks.core.TaskRepository;
 import org.eclipse.mylyn.tasks.ui.TaskHyperlink;
 import org.eclipse.mylyn.tasks.ui.TasksUi;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.StyleRange;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.ui.editors.text.EditorsUI;
 import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
 
 /**
  * @author Rob Elves
  * @author Frank Becker
  * @author Steffen Pingel
  * @since 3.0
  */
 public class RepositoryTextViewerConfiguration extends TextSourceViewerConfiguration {
 
 	private static final String ID_CONTEXT_EDITOR_TASK = "org.eclipse.mylyn.tasks.ui.TaskEditor";
 
 	private static final String ID_CONTEXT_EDITOR_TEXT = "org.eclipse.ui.DefaultTextEditor";
 
 	private RepositoryTextScanner scanner;
 
 	private final boolean spellCheck;
 
 	private final TaskRepository taskRepository;
 
 	public RepositoryTextViewerConfiguration(TaskRepository taskRepository, boolean spellCheck) {
 		super(EditorsUI.getPreferenceStore());
		Assert.isNotNull(taskRepository);
 		this.taskRepository = taskRepository;
 		this.spellCheck = spellCheck;
 	}
 
 	@Override
 	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
 		PresentationReconciler reconciler = new PresentationReconciler();
 		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
 
 		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getDefaultScanner());
 		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
 		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
 
 		return reconciler;
 	}
 
 	private RepositoryTextScanner getDefaultScanner() {
 		if (scanner == null) {
 			scanner = new RepositoryTextScanner();
 		}
 		return scanner;
 	}
 
 	@Override
 	@SuppressWarnings("unchecked")
 	protected Map getHyperlinkDetectorTargets(final ISourceViewer sourceViewer) {
 		IAdaptable context = new IAdaptable() {
 			public Object getAdapter(Class adapter) {
 				if (adapter == TaskRepository.class) {
 					return getTaskRepository();
 				}
 				return null;
 			}
 		};
 
 		Map targets = new HashMap();
 		targets.put(ID_CONTEXT_EDITOR_TEXT, context);
 		targets.put(ID_CONTEXT_EDITOR_TASK, context);
 		return targets;
 	}
 
 	public TaskRepository getTaskRepository() {
 		return taskRepository;
 	}
 
 	@Override
 	public IHyperlinkPresenter getHyperlinkPresenter(final ISourceViewer sourceViewer) {
 		return new TaskTextViewerHyperlinkPresenter(JFaceResources.getColorRegistry().get(
 				JFacePreferences.ACTIVE_HYPERLINK_COLOR), sourceViewer);
 	}
 
 	@Override
 	public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
 		return SWT.NONE;
 	}
 
 	@Override
 	public IReconciler getReconciler(ISourceViewer sourceViewer) {
 		if (spellCheck) {
 			return super.getReconciler(sourceViewer);
 		} else {
 			return null;
 		}
 	}
 
 	private final class TaskTextViewerHyperlinkPresenter extends DefaultHyperlinkPresenter {
 		private final ISourceViewer sourceViewer;
 
 		private IRegion activeRegion;
 
 		/**
 		 * Stores which task a tooltip is being displayed for. It is used to avoid having the same tooltip being set
 		 * multiple times while you move the mouse over a task hyperlink (bug#209409)
 		 */
 		private AbstractTask currentTaskHyperlink;
 
 		private TaskTextViewerHyperlinkPresenter(Color color, ISourceViewer sourceViewer) {
 			super(color);
 			this.sourceViewer = sourceViewer;
 		}
 
 		@SuppressWarnings("unchecked")
 		@Override
 		public void applyTextPresentation(TextPresentation textPresentation) {
 			super.applyTextPresentation(textPresentation);
 			if (activeRegion != null && currentTaskHyperlink != null && currentTaskHyperlink.isCompleted()) {
 				Iterator<StyleRange> styleRangeIterator = textPresentation.getAllStyleRangeIterator();
 				while (styleRangeIterator.hasNext()) {
 					StyleRange styleRange = styleRangeIterator.next();
 					if (activeRegion.getOffset() == styleRange.start && activeRegion.getLength() == styleRange.length) {
 						styleRange.strikeout = true;
 						break;
 					}
 				}
 			}
 		}
 
 		@Override
 		public void showHyperlinks(IHyperlink[] hyperlinks) {
 			activeRegion = null;
 			if (hyperlinks != null && hyperlinks.length > 0 && hyperlinks[0] instanceof TaskHyperlink) {
 				TaskHyperlink hyperlink = (TaskHyperlink) hyperlinks[0];
 
 				ITaskList taskList = TasksUi.getTaskListManager().getTaskList();
 				String repositoryUrl = hyperlink.getRepository().getRepositoryUrl();
 
 				AbstractTask task = taskList.getTask(repositoryUrl, hyperlink.getTaskId());
 				if (task == null) {
 					task = taskList.getTaskByKey(repositoryUrl, hyperlink.getTaskId());
 				}
 
 				if (task != null && task != currentTaskHyperlink) {
 					currentTaskHyperlink = task;
 					activeRegion = hyperlink.getHyperlinkRegion();
 					Control cursorControl = sourceViewer.getTextWidget().getDisplay().getCursorControl();
 					if (cursorControl != null) {
 						if (task.getTaskKey() == null) {
 							cursorControl.setToolTipText(task.getSummary());
 						} else {
 							cursorControl.setToolTipText(task.getTaskKey() + ": " + task.getSummary());
 						}
 					}
 				}
 			}
 			super.showHyperlinks(hyperlinks);
 		}
 
 		@Override
 		public void hideHyperlinks() {
 			Control cursorControl = sourceViewer.getTextWidget().getDisplay().getCursorControl();
 			if (cursorControl != null) {
 				cursorControl.setToolTipText(null);
 			}
 			currentTaskHyperlink = null;
 
 			super.hideHyperlinks();
 		}
 
 		@Override
 		public void uninstall() {
 			// ignore
 			super.uninstall();
 		}
 	}
 
 	private static class RepositoryTextScanner extends RuleBasedScanner {
 
 		public RepositoryTextScanner() {
 			IToken bugToken = new Token(new TextAttribute(JFaceResources.getColorRegistry().get(
 					JFacePreferences.ACTIVE_HYPERLINK_COLOR)));
 			IToken quoteToken = new Token(new TextAttribute(TaskListColorsAndFonts.COLOR_QUOTED_TEXT));
 			IRule[] rules = new IRule[16];
 			rules[0] = (new SingleLineRule("http://", " ", bugToken));
 			rules[1] = (new SingleLineRule("https://", " ", bugToken));
 			rules[2] = (new SingleLineRule("bug#", " ", bugToken));
 			rules[3] = (new SingleLineRule("bug#", "", bugToken));
 			rules[4] = (new SingleLineRule("bug #", "", bugToken));
 			rules[5] = (new SingleLineRule("http://", "\n", bugToken));
 			rules[6] = (new SingleLineRule("https://", "\n", bugToken));
 			rules[7] = (new SingleLineRule("task#", " ", bugToken));
 			rules[8] = (new MultiLineRule("task#", "\n", bugToken));
 			rules[9] = (new MultiLineRule("task# ", " ", bugToken));
 			rules[10] = (new SingleLineRule("task #", "\n", bugToken));
 			rules[11] = (new SingleLineRule("*** This bug has been ", "***", bugToken));
 			rules[12] = (new SingleLineRule("http://", "", bugToken));
 			rules[13] = (new SingleLineRule("https://", "", bugToken));
 			rules[14] = (new MultiLineRule("task #", " ", bugToken));
 			SingleLineRule quoteRule = new SingleLineRule(">", null, quoteToken, (char) 0, true);
 			quoteRule.setColumnConstraint(0);
 			rules[15] = quoteRule;
 			setRules(rules);
 		}
 
 	}
 
 	@Override
 	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
 		ContentAssistant assistant = new ContentAssistant();
 		RepositoryCompletionProcessor processor = new RepositoryCompletionProcessor(taskRepository);
 		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
 		return assistant;
 	}
 
 }
