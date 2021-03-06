 package org.rubypeople.rdt.internal.ui.text.ruby;
 
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.ITextSelection;
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.text.TextPresentation;
 import org.eclipse.jface.text.contentassist.CompletionProposal;
 import org.eclipse.jface.text.contentassist.ContextInformation;
 import org.eclipse.jface.text.contentassist.ICompletionProposal;
 import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
 import org.eclipse.jface.text.contentassist.IContextInformation;
 import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
 import org.eclipse.jface.text.contentassist.IContextInformationValidator;
 import org.eclipse.jface.text.templates.Template;
 import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
 import org.eclipse.jface.text.templates.TemplateContextType;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.ui.IEditorPart;
 import org.rubypeople.rdt.core.IParent;
 import org.rubypeople.rdt.core.IRubyElement;
 import org.rubypeople.rdt.core.IRubyProject;
 import org.rubypeople.rdt.core.IRubyScript;
 import org.rubypeople.rdt.core.RubyModelException;
 import org.rubypeople.rdt.internal.corext.template.ruby.RubyContextType;
 import org.rubypeople.rdt.internal.ui.RubyPlugin;
 import org.rubypeople.rdt.internal.ui.RubyPluginImages;
 import org.rubypeople.rdt.internal.ui.text.template.contentassist.RubyTemplateAccess;
 import org.rubypeople.rdt.internal.ui.text.template.contentassist.TemplateEngine;
 import org.rubypeople.rdt.internal.ui.text.template.contentassist.TemplateProposal;
 import org.rubypeople.rdt.ui.IWorkingCopyManager;
 import org.rubypeople.rdt.ui.text.RubyTextTools;
 import org.rubypeople.rdt.ui.text.ruby.IRubyCompletionProposal;
 
 public class RubyCompletionProcessor extends TemplateCompletionProcessor
 		implements IContentAssistProcessor {
 
 	private static String[] keywordProposals;
 
 	protected IContextInformationValidator contextInformationValidator = new RubyContextInformationValidator();
 
 	private static String[] preDefinedGlobals = { "$!", "$@", "$_", "$.", "$&",
 			"$n", "$~", "$=", "$/", "$\\", "$0", "$*", "$$", "$?", "$:" };
 
 	private static String[] globalContexts = { "error message",
 			"position of an error occurrence", "latest read string by `gets'",
 			"latest read number of line by interpreter",
 			"latest matched string by the regexep.",
 			"latest matched string by nth parentheses of regexp.",
 			"data for latest matche for regexp",
 			"whether or not case-sensitive in string matching",
 			"input record separator", "output record separator",
 			"the name of the ruby scpript file",
 			"command line arguments for the ruby scpript",
 			"PID for ruby interpreter",
 			"status of the latest executed child process",
 			"array of paths that ruby interpreter searches for files" };
 
 	// FIXME This is an ugly hack, just hard-coding method names
 	// FIXME Create a model for Ruby core in our Ruby Model!
 	private static String[] KERNEL_METHODS = { "abort", "at_exit", "autoload",
 			"binding", "block_given?", "callcc", "caller", "catch", "chomp",
 			"chomp!", "chop", "chop!", "eval", "exec", "exit", "exit!", "fail",
 			"fork", "format", "gets", "global_variables", "gsub", "gsub!",
 			"iterator?", "lambda", "load", "local_variables", "loop", "open",
 			"p", "print", "printf", "proc", "putc", "puts", "raise", "rand",
 			"readline", "readlines", "require", "scan", "select",
 			"set_trace_func", "singleton_method_added", "sleep", "split",
 			"sprintf", "srand", "sub", "sub!", "syscall", "system", "test",
 			"throw", "trace_var", "trap", "untrace_var" };
 
 	/**
 	 * The prefix for the current content assist
 	 */
 	protected String currentPrefix = null;
 
 	/**
 	 * Cursor position, counted from the beginning of the document.
 	 * <P>
 	 * The first position has index '0'.
 	 */
 	protected int cursorPosition = -1;
 
 	/**
 	 * The text viewer.
 	 */
 	private ITextViewer viewer;
 
 	private IWorkingCopyManager fManager;
 
 	private IEditorPart fEditor;
 
 	private TemplateEngine fRubyTemplateEngine;
 
 	public RubyCompletionProcessor(IEditorPart editor) {
 		super();
 		fEditor = editor;
 		fManager = RubyPlugin.getDefault().getWorkingCopyManager();
 
 		TemplateContextType contextType = RubyPlugin.getDefault()
 				.getTemplateContextRegistry().getContextType(
 						RubyContextType.NAME);
 		if (contextType == null) {
 			contextType = new RubyContextType();
 			RubyPlugin.getDefault().getTemplateContextRegistry()
 					.addContextType(contextType);
 		}
 		if (contextType != null)
 			fRubyTemplateEngine = new TemplateEngine(contextType);
 		else
 			fRubyTemplateEngine = null;
 
 	}
 
 	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
 			int documentOffset) {
 		this.viewer = viewer;
 		ITextSelection selection = (ITextSelection) viewer
 				.getSelectionProvider().getSelection();
 		cursorPosition = selection.getOffset() + selection.getLength();
 
 		ICompletionProposal[] normal = determineRubyElementProposals(viewer,
 				documentOffset);
 		List templates = determineTemplateProposals(viewer, documentOffset);
 		ICompletionProposal[] templateArray = new ICompletionProposal[templates
 				.size()];
 		int i = 0;
 		for (Iterator iter = templates.iterator(); iter.hasNext(); i++) {
 			templateArray[i] = (ICompletionProposal) iter.next();
 		}
 		ICompletionProposal[] merged = merge(normal, templateArray);
 
 		ICompletionProposal[] keywords = determineKeywordProposals(viewer,
 				documentOffset);
 		ICompletionProposal[] mergedTwo = merge(merged, keywords);
 		return mergedTwo;
 	}
 
 	/**
 	 * @param arrayOne
 	 * @param arrayTwo
 	 * @return
 	 */
 	private ICompletionProposal[] merge(ICompletionProposal[] arrayOne,
 			ICompletionProposal[] arrayTwo) {
 		ICompletionProposal[] merged = new ICompletionProposal[arrayOne.length
 				+ arrayTwo.length];
 		System.arraycopy(arrayOne, 0, merged, 0, arrayOne.length);
 		System.arraycopy(arrayTwo, 0, merged, arrayOne.length, arrayTwo.length);
 		return merged;
 	}
 
 	/**
 	 * @param viewer
 	 * @param documentOffset
 	 * @return
 	 */
 	private ICompletionProposal[] determineRubyElementProposals(
 			ITextViewer viewer, int documentOffset) {
 		Collection completionProposals = getDocumentsRubyElements();
 		String prefix = getCurrentPrefix(viewer.getDocument().get(),
 				documentOffset);
 		// following the JDT convention, if there's no text already entered,
 		// then don't suggest imported elements
 		if (prefix.length() > 0) {
 			// FIXME Add elements from required/loaded files!
 		}
 
 		List possibleProposals = new ArrayList();
 		for (Iterator iter = completionProposals.iterator(); iter.hasNext();) {
 			String proposal = (String) iter.next();
 			if (proposal.startsWith(prefix)) {
 				String message = "{0}";
 				IContextInformation info = new ContextInformation(proposal,
 						MessageFormat
 								.format(message, new Object[] { proposal }));
 				possibleProposals
 						.add(new CompletionProposal(proposal.substring(prefix
 								.length(), proposal.length()), documentOffset,
 								0, proposal.length() - prefix.length(), null,
 								proposal, info, MessageFormat.format(
 										"Ruby keyword: {0}",
 										new Object[] { proposal })));
 			}
 		}
 		ICompletionProposal[] result = new ICompletionProposal[possibleProposals
 				.size()];
 		possibleProposals.toArray(result);
 		return result;
 	}
 
 	private Collection addKernelMethods() {
 		Collection kernelProposals = new ArrayList();
 		for (int i = 0; i < KERNEL_METHODS.length; i++) {
 			kernelProposals.add(KERNEL_METHODS[i]);
 		}
 		return kernelProposals;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
 	 */
 	protected Image getImage(Template template) {
 		return RubyPluginImages.get(RubyPluginImages.IMG_OBJS_TEMPLATE);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
 	 *      org.eclipse.jface.text.IRegion)
 	 */
 	protected TemplateContextType getContextType(ITextViewer textViewer,
 			IRegion region) {
 		return RubyTemplateAccess.getDefault().getContextTypeRegistry()
 				.getContextType(RubyContextType.NAME);
 	}
 
 	/**
 	 * @return
 	 */
 	private List determineTemplateProposals(ITextViewer refViewer,
 			int documentOffset) {
 		TemplateEngine engine = fRubyTemplateEngine;
 	
 		if (engine != null) {
 			IRubyScript unit = fManager
 					.getWorkingCopy(fEditor.getEditorInput());
 			if (unit == null)
 				return Collections.EMPTY_LIST;
 
 			engine.reset();
 			engine.complete(refViewer, documentOffset,
 					unit);
 
 			TemplateProposal[] templateProposals = engine.getResults();
 			List result = new ArrayList(Arrays.asList(templateProposals));
 
 			IRubyCompletionProposal[] keyWordResults = getKeywordProposals(documentOffset);
 			if (keyWordResults.length > 0) {
				List removals = new ArrayList();

 				// update relevance of template proposals that match with a
 				// keyword
 				// give those templates slightly more relevance than the keyword
 				// to
 				// sort them first
 				// remove keyword templates that don't have an equivalent
 				// keyword proposal
 				if (keyWordResults.length > 0) {
 					outer: for (int k = 0; k < templateProposals.length; k++) {
 						TemplateProposal curr = templateProposals[k];
 						String name = curr.getTemplate().getName();
 						for (int i = 0; i < keyWordResults.length; i++) {
 							String keyword = keyWordResults[i]
 									.getDisplayString();
 							if (name.startsWith(keyword)) {
 								curr.setRelevance(keyWordResults[i]
 										.getRelevance() + 1);
 								continue outer;
 							}
 						}
						if (isKeyword(name))
							removals.add(curr);
 					}
 				}

				result.removeAll(removals);
 			}
 			return result;
 		}
 
 		return Collections.EMPTY_LIST;
 
 	}
 
 	private IRubyCompletionProposal[] getKeywordProposals(int documentOffset) {
 		List keywords = getKeywords();
 		List fKeywords = new ArrayList();
 		for (Iterator iter = keywords.iterator(); iter.hasNext();) {
 			String keyword = (String) iter.next();
 			String prefix = getCurrentPrefix(viewer.getDocument().get(),
 					documentOffset);
 			if (prefix.length() >= keyword.length())
 				continue;
 			fKeywords.add(createKeywordProposal(keyword, prefix, documentOffset));
 		}
 		return (IRubyCompletionProposal[]) fKeywords
 				.toArray(new RubyCompletionProposal[fKeywords.size()]);
 	}
 
 	private IRubyCompletionProposal createKeywordProposal(String keyword,
 			String prefix, int documentOffset) {
 		String completion = keyword
 				.substring(prefix.length(), keyword.length());
 		return new RubyCompletionProposal(completion, documentOffset,
 				completion.length(), RubyPluginImages
 						.get(RubyPluginImages.IMG_OBJS_TEMPLATE), keyword, 0);
 	}
 
 	private List getKeywords() {
 		List list = new ArrayList();
 		String[] keywords = RubyTextTools.getKeyWords();
 		for (int i = 0; i < keywords.length; i++) {
 			list.add(keywords[i]);
 		}
 		return list;
 	}
 
 	private boolean isKeyword(String name) {
 		return getKeywords().contains(name);
 	}
 
 	/**
 	 * @param proposal
 	 * @return
 	 */
 	private String getContext(String proposal) {
 		for (int i = 0; i < preDefinedGlobals.length; i++) {
 			if (proposal.equals(preDefinedGlobals[i]))
 				return globalContexts[i];
 		}
 		return "";
 	}
 
 	/**
 	 * @param proposal
 	 * @return
 	 */
 	private boolean isPredefinedGlobal(String proposal) {
 		for (int i = 0; i < preDefinedGlobals.length; i++) {
 			if (proposal.equals(preDefinedGlobals[i]))
 				return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Gets all the distinct elements in the current RubyScript
 	 * 
 	 * @return a List of the names of all the elements in the current RubyScript
 	 */
 	private Collection getDocumentsRubyElements() {
 		IRubyScript script = fManager.getWorkingCopy(fEditor.getEditorInput());
 		// FIXME Get only the elements in the current scope!
 
 		Collection elements = getElements(script);
 		IRubyProject project = script.getRubyProject();
 		// Add all the classes and modules in the project
 		elements.addAll(addClassesAndModulesInProject(project));
 		// Add all the classes and modules in referenced projects
 		for (Iterator iter = project.getReferencedProjects().iterator(); iter
 				.hasNext();) {
 			elements.addAll(addClassesAndModulesInProject(((IRubyProject) iter
 					.next())));
 		}
 		// TODO Add all the methods defined in included modules for the class
 		// TODO Add all the methods defined in superclasses for the class/module
 
 		// always add Kernel methods
 		elements.addAll(addKernelMethods());
 		return elements;
 	}
 
 	private Collection addClassesAndModulesInProject(IRubyProject project) {
 		return getElementsOfType(project, new int[] { IRubyElement.TYPE });
 	}
 
 	private Collection getElementsOfType(IParent element, int[] types) {
 		Collection suggestions = new ArrayList();
 		try {
 			IRubyElement[] elements = element.getChildren();
 			if (elements == null)
 				return suggestions;
 			for (int x = 0; x < elements.length; x++) {
 				IRubyElement child = elements[x];
 				for (int i = 0; i < types.length; i++) {
 					if (child.getElementType() == types[i]) {
 						suggestions.add(child.getElementName());
 						break;
 					}
 				}
 				if (child instanceof IParent)
 					suggestions
 							.addAll(getElementsOfType((IParent) child, types));
 			}
 		} catch (RubyModelException e) {
 			e.printStackTrace();
 		}
 		return suggestions;
 	}
 
 	/**
 	 * @param script
 	 * @return
 	 */
 	private Collection getElements(IParent element) {
 		return getElementsOfType(element, new int[] { IRubyElement.TYPE,
 				IRubyElement.METHOD, IRubyElement.GLOBAL,
 				IRubyElement.CONSTANT, IRubyElement.CLASS_VAR,
 				IRubyElement.INSTANCE_VAR });
 	}
 
 	private ICompletionProposal[] determineKeywordProposals(ITextViewer viewer,
 			int documentOffset) {
 		initKeywordProposals();
 
 		String prefix = getCurrentPrefix(viewer.getDocument().get(),
 				documentOffset);
 		// following the JDT convention, if there's no text already entered,
 		// then don't suggest keywords
 		if (prefix.length() < 1) {
 			return new ICompletionProposal[0];
 		}
 		List completionProposals = Arrays.asList(keywordProposals);
 
 		// FIXME Refactor to combine the copied code in
 		// determineRubyElementProposals
 		List possibleProposals = new ArrayList();
 		for (int i = 0; i < completionProposals.size(); i++) {
 			String proposal = (String) completionProposals.get(i);
 			if (proposal.startsWith(prefix)) {
 				String message;
 				if (isPredefinedGlobal(proposal)) {
 					message = "{0} " + getContext(proposal);
 				} else {
 					message = "{0}";
 				}
 				IContextInformation info = new ContextInformation(proposal,
 						MessageFormat
 								.format(message, new Object[] { proposal }));
 				possibleProposals
 						.add(new CompletionProposal(proposal.substring(prefix
 								.length(), proposal.length()), documentOffset,
 								0, proposal.length() - prefix.length(), null,
 								proposal, info, MessageFormat.format(
 										"Ruby keyword: {0}",
 										new Object[] { proposal })));
 			}
 		}
 		ICompletionProposal[] result = new ICompletionProposal[possibleProposals
 				.size()];
 		possibleProposals.toArray(result);
 		return result;
 	}
 
 	/**
 	 * 
 	 */
 	private void initKeywordProposals() {
 		if (keywordProposals == null) {
 			String[] keywords = RubyTextTools.getKeyWords();
 			keywordProposals = new String[keywords.length
 					+ preDefinedGlobals.length];
 			System.arraycopy(keywords, 0, keywordProposals, 0, keywords.length);
 			System.arraycopy(preDefinedGlobals, 0, keywordProposals,
 					keywords.length, preDefinedGlobals.length);
 		}
 	}
 
 	protected String getCurrentPrefix(String documentString, int documentOffset) {
 		int tokenLength = 0;
 		while ((documentOffset - tokenLength > 0)
 				&& !Character.isWhitespace(documentString.charAt(documentOffset
 						- tokenLength - 1)))
 			tokenLength++;
 		return documentString.substring((documentOffset - tokenLength),
 				documentOffset);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
 	 */
 	protected Template[] getTemplates(String contextTypeId) {
 		return RubyTemplateAccess.getDefault().getTemplateStore()
 				.getTemplates();
 	}
 
 	public IContextInformation[] computeContextInformation(ITextViewer viewer,
 			int documentOffset) {
 		return null;
 	}
 
 	public char[] getCompletionProposalAutoActivationCharacters() {
 		return null;
 	}
 
 	public char[] getContextInformationAutoActivationCharacters() {
 		return new char[] { '#' };
 	}
 
 	public IContextInformationValidator getContextInformationValidator() {
 		return contextInformationValidator;
 	}
 
 	public String getErrorMessage() {
 		return null;
 	}
 
 	protected class RubyContextInformationValidator implements
 			IContextInformationValidator, IContextInformationPresenter {
 
 		protected int installDocumentPosition;
 
 		/**
 		 * @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#install(IContextInformation,
 		 *      ITextViewer, int)
 		 */
 		public void install(IContextInformation info, ITextViewer viewer,
 				int documentPosition) {
 			installDocumentPosition = documentPosition;
 		}
 
 		/**
 		 * @see org.eclipse.jface.text.contentassist.IContextInformationValidator#isContextInformationValid(int)
 		 */
 		public boolean isContextInformationValid(int documentPosition) {
 			return Math.abs(installDocumentPosition - documentPosition) < 1;
 		}
 
 		/**
 		 * @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#updatePresentation(int,
 		 *      TextPresentation)
 		 */
 		public boolean updatePresentation(int documentPosition,
 				TextPresentation presentation) {
 			return false;
 		}
 	}
 }
