 package org.strategoxt.imp.runtime.services;
 
 import static org.spoofax.interpreter.core.Tools.isTermTuple;
 import static org.spoofax.interpreter.core.Tools.termAt;
 import static org.spoofax.interpreter.terms.IStrategoTerm.LIST;
 import static org.spoofax.interpreter.terms.IStrategoTerm.TUPLE;
 import static org.strategoxt.imp.runtime.dynamicloading.TermReader.cons;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.WeakHashMap;
 import java.util.concurrent.locks.Lock;
 
 import lpg.runtime.IAst;
 
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.imp.parser.IModelListener;
 import org.eclipse.imp.parser.IParseController;
 import org.spoofax.interpreter.core.InterpreterErrorExit;
 import org.spoofax.interpreter.core.InterpreterException;
 import org.spoofax.interpreter.core.InterpreterExit;
 import org.spoofax.interpreter.core.StackTracer;
 import org.spoofax.interpreter.core.UndefinedStrategyException;
 import org.spoofax.interpreter.library.LoggingIOAgent;
 import org.spoofax.interpreter.terms.IStrategoList;
 import org.spoofax.interpreter.terms.IStrategoString;
 import org.spoofax.interpreter.terms.IStrategoTerm;
 import org.spoofax.interpreter.terms.IStrategoTuple;
 import org.spoofax.interpreter.terms.ITermFactory;
 import org.strategoxt.HybridInterpreter;
 import org.strategoxt.IncompatibleJarException;
 import org.strategoxt.imp.generator.postprocess_feedback_results_0_0;
 import org.strategoxt.imp.generator.sdf2imp;
 import org.strategoxt.imp.runtime.Debug;
 import org.strategoxt.imp.runtime.EditorState;
 import org.strategoxt.imp.runtime.Environment;
 import org.strategoxt.imp.runtime.SWTSafeLock;
 import org.strategoxt.imp.runtime.WeakWeakMap;
 import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
 import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
 import org.strategoxt.imp.runtime.dynamicloading.IDynamicLanguageService;
 import org.strategoxt.imp.runtime.parser.SGLRParseController;
 import org.strategoxt.imp.runtime.parser.ast.AstMessageHandler;
 import org.strategoxt.imp.runtime.services.StrategoAnalysisQueue.UpdateJob;
 import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
 import org.strategoxt.imp.runtime.stratego.IMPJSGLRLibrary;
 import org.strategoxt.imp.runtime.stratego.StrategoConsole;
 import org.strategoxt.imp.runtime.stratego.StrategoTermPath;
 import org.strategoxt.imp.runtime.stratego.adapter.IStrategoAstNode;
 import org.strategoxt.imp.runtime.stratego.adapter.WrappedAstNode;
 import org.strategoxt.lang.Context;
 import org.strategoxt.lang.StrategoException;
 import org.strategoxt.stratego_aterm.implode_aterm_0_0;
 import org.strategoxt.stratego_aterm.stratego_aterm;
 import org.strategoxt.stratego_lib.set_config_0_0;
 
 /**
  * Basic Stratego feedback (i.e., errors and warnings) provider.
  * This service may also be used as a basis for other semantic services
  * such as reference resolving.
  * 
  * @author Lennart Kats <lennart add lclnet.nl>
  */
 public class StrategoObserver implements IDynamicLanguageService, IModelListener {
 	
 	// TODO: separate delay for error markers?
 	public static final int OBSERVER_DELAY = 600;
 	
 	private static Map<Descriptor, HybridInterpreter> runtimePrototypes =
 		Collections.synchronizedMap(new WeakWeakMap<Descriptor, HybridInterpreter>());
 	
 	private final Map<IResource, IStrategoTerm> resultingAsts =
 		new WeakHashMap<IResource, IStrategoTerm>();
 	
 	private final String feedbackFunction;
 	
 	private final AstMessageHandler messages = new AstMessageHandler(AstMessageHandler.ANALYSIS_MARKER_TYPE);
 	
 	private final Lock observerSchedulerLock = new SWTSafeLock(true);
 	
 	private HybridInterpreter runtime;
 	
 	private volatile Descriptor descriptor;
 	
 	private volatile boolean isUpdateStarted;
 	
 	private volatile boolean rushNextUpdate;
 	
 	private UpdateJob updateJob;
 	
 	private boolean wasExceptionLogged;
 	
 	public StrategoObserver(Descriptor descriptor, String feedbackFunction) {
 		this.descriptor = descriptor;
 		this.feedbackFunction = feedbackFunction;
 	}
 
 	public final AnalysisRequired getAnalysisRequired() {
 		return AnalysisRequired.TYPE_ANALYSIS;
 	}
 	
 	/**
 	 * Returns a value indicating whether or not an analysis has
 	 * been scheduled or completed at this point.
 	 * 
 	 * @return true if update() or asyncUpdate() have been called.
 	 */
 	public boolean isUpdateScheduled() {
 		return isUpdateStarted;
 	}
 	
 	public void setRushNextUpdate(boolean rushNextUpdate) {
 		this.rushNextUpdate = rushNextUpdate;
 	}
 	
 	public AstMessageHandler getMessages() {
 		return messages;
 	}
 	
 	public Object getSyncRoot() {
 		 // TODO: *maybe* use descriptor as syncroot? deadlocky?
 		return Environment.getSyncRoot();
 	}
 
 	public String getLog() {
 		assert Thread.holdsLock(getSyncRoot());
 		return ((EditorIOAgent) runtime.getIOAgent()).getLog().trim();
 	}
 	
 	private void initialize(IProgressMonitor monitor) {
 		assert Thread.holdsLock(getSyncRoot());
 		
 		HybridInterpreter prototype = runtimePrototypes.get(descriptor);
 		if (prototype != null) {
 			runtime = Environment.createInterpreterFromPrototype(prototype);
 			return;
 		}
 		
 		monitor.subTask("Loading analysis runtime");
 		
 		Debug.startTimer();
 		List<String> jars = new ArrayList<String>();
 		
 		for (File file : descriptor.getAttachedFiles()) {
 			String filename = file.toString();
 			if (filename.endsWith(".ctree")) {
 				createEmptyRuntime(monitor);
 				loadCTree(filename);
 			} else if (filename.endsWith(".jar")) {
 				createEmptyRuntime(monitor);
 				jars.add(filename);
 			} else if (filename.endsWith(".str")) {
 				Environment.asynOpenErrorDialog("Loading analysis components", "Cannot use .str files as a provider: please specify a .ctree or .jar file instead (usually built in /include/)", null);
 			}
 		}
 		
 		if (!jars.isEmpty()) loadJars(jars);
 		Debug.stopTimer("Loaded analysis components");
 		
 		monitor.subTask(null);
 		if (runtime != null)
 			runtimePrototypes.put(descriptor, runtime);
 	}
 	
 	/**
 	 * Uninitializes the observer.
 	 */
 	public void uninitialize() {
 		// Removing it from the shared cache is sort of pointless and could be dangerous
 		// HybridInterpreter cachedRuntime = cachedRuntimes.remove(descriptor);
 		if (runtime != null) {
 			runtime.uninit();
 			runtime = null;
 		}
 	}
 
 	private void createEmptyRuntime(IProgressMonitor monitor) {
 		assert Thread.holdsLock(getSyncRoot());
 		
 		if (runtime == null) {
 			Debug.startTimer();
 			runtime = Environment.createInterpreter(getSyncRoot() != Environment.getSyncRoot());
 			runtime.init();
 			Debug.stopTimer("Created new Stratego runtime instance");
 			monitor.subTask("Loading analysis runtime components");
 		}
 	}
 
 	private void loadCTree(String filename) {
 		try {
 			Debug.startTimer("Loading Stratego module ", filename);
 			assert getSyncRoot() == Environment.getSyncRoot() || !Thread.holdsLock(Environment.getSyncRoot());
 			synchronized (Environment.getSyncRoot()) {
 				runtime.load(descriptor.openAttachment(filename));
 			}
 			Debug.stopTimer("Successfully loaded " +  filename);
 		} catch (InterpreterException e) {
 			Environment.logException(new BadDescriptorException("Error loading compiler service provider " + filename, e));
 			if (descriptor.isDynamicallyLoaded())
 				Environment.asynOpenErrorDialog("Dynamic descriptor loading", "Error loading compiler service provider " + filename, e);
 		} catch (IOException e) {
 			Environment.logException(new BadDescriptorException("Could not load compiler service provider " + filename, e));
 			if (descriptor.isDynamicallyLoaded())
 				Environment.asynOpenErrorDialog("Dynamic descriptor loading", "Error loading compiler service provider " + filename, e);
 		}
 	}
 	
 	private void loadJars(List<String> jars) {
 		try {
 			Debug.startTimer("Loading Stratego modules " + jars);
 			URL[] classpath = new URL[jars.size()];
 			for (int i = 0; i < classpath.length; i++) {
 				classpath[i] = descriptor.getBasePath().append(jars.get(i)).toFile().toURI().toURL();
 			}
 			runtime.loadJars(classpath);
 			Debug.stopTimer("Successfully loaded " + jars);
 		} catch (SecurityException e) {
 			Environment.logException("Error loading compiler service providers " + jars, e);
 			if (descriptor.isDynamicallyLoaded())
 				Environment.asynOpenErrorDialog("Dynamic descriptor loading", "Error loading compiler service providers " + jars, e);
 		} catch (IncompatibleJarException e) {
 			Environment.logException("Error loading compiler service providers " + jars, e);
 			if (descriptor.isDynamicallyLoaded())
 				Environment.asynOpenErrorDialog("Dynamic descriptor loading", "Error loading compiler service providers " + jars, e);
 		} catch (IOException e) {
 			Environment.logException("Error loading compiler service providers " + jars, e);
 			if (descriptor.isDynamicallyLoaded())
 				Environment.asynOpenErrorDialog("Dynamic descriptor loading", "Error loading compiler service providers " + jars, e);
 		}
 	}
 
 	/**
 	 * Starts a new update() operation, asynchronously.
 	 * Can be called from multiple threads.
 	 */
 	public void scheduleUpdate(final IParseController parseController) {
 		
 		isUpdateStarted = true;
 
 		observerSchedulerLock.lock();
 		try {
 			StrategoAnalysisQueue queue = StrategoAnalysisQueueFactory.getInstance();
 			if (this.updateJob != null) {
 				this.updateJob.cancel();
 			}
 			
 			if (this.rushNextUpdate) {
 				this.updateJob = queue.queue(this, parseController, 0);
 			} else {
 				this.updateJob = queue.queue(this, parseController, OBSERVER_DELAY);
 			}
 		} finally {
 			observerSchedulerLock.unlock();
 		}
 		
 	}
 
 	public void update(IParseController parseController, IProgressMonitor monitor) {
 		isUpdateStarted = true;
 		IStrategoAstNode ast = (IStrategoAstNode) parseController.getCurrentAst();
 		if (ast == null || ast.getConstructor() == null || feedbackFunction == null
 				|| isRecoveryFailed(parseController)) {
 			messages.clearMarkers(((SGLRParseController) parseController).getResource());
 			messages.commitDeletions();
 			return;
 		}
 			
 		if (monitor.isCanceled())
 			return;
 		
 		IStrategoTerm feedback = null;
 		
 		try {
 			synchronized (getSyncRoot()) {
 				resultingAsts.remove(ast.getResource());
 				feedback = invokeSilent(feedbackFunction, makeInputTerm(ast, false), ast.getResource());
 	
 				if (feedback == null) {
 					reportRewritingFailed();
 					String log = getLog();
 					if (!wasExceptionLogged || log.length() > 0)
 						Environment.logException(log.length() == 0 ? "Analysis failed" : "Analysis failed:\n" + log);
 					messages.clearMarkers(ast.getResource());
 					messages.addMarkerFirstLine(ast.getResource(), "Analysis failed (see error log)", IMarker.SEVERITY_ERROR);
 					messages.commitAllChanges();
 				} else if (!monitor.isCanceled()) {
 					// TODO: figure out how this was supposed to be synchronized
 					presentToUser(ast.getResource(), feedback);
 				}
 			}
 		} finally {
 			// System.out.println("OBSERVED " + System.currentTimeMillis()); // DEBUG
 			// processEditorRecolorEvents(parseController);
 		}
 	}
 
 	private static boolean isRecoveryFailed(IParseController parseController) {
 		return parseController instanceof SGLRParseController
 				&& ((SGLRParseController) parseController).getErrorHandler().isRecoveryFailed();
 	}
 
 	@Deprecated
 	@SuppressWarnings("unused")
 	private void processEditorRecolorEvents(IParseController parseController) {
 		if (parseController instanceof SGLRParseController) {
 			EditorState editor = ((SGLRParseController) parseController).getEditor();
 			if (editor != null)
 				AstMessageHandler.processEditorRecolorEvents(editor.getEditor());
 		}
 		AstMessageHandler.processAllEditorRecolorEvents();
 	}
 
 	public void reportRewritingFailed() {
 		assert Thread.holdsLock(getSyncRoot());
 		StackTracer trace = runtime.getContext().getStackTracer();
 		runtime.getIOAgent().printError(
 				trace.getTraceDepth() != 0 ? "rewriting failed, trace:" : "rewriting failed");
 		trace.printStackTrace();
 		if (descriptor.isDynamicallyLoaded())
 			StrategoConsole.activateConsole();
 	}
 	
 	/* UNDONE: asynchronous feedback presentation
 	private void asyncPresentToUser(final IParseController parseController, final IStrategoTerm feedback, final String log) {
 		Job job = new WorkspaceJob("Showing feedback") {
 			{ setSystem(true); } // don't show to user
 			@Override
 			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
 				presentToUser(parseController, feedback, log);
 				return Status.OK_STATUS;
 			}
 		};
 		
 		job.setRule(parseController.getProject().getResource());
 		job.setSystem(true);
 		job.schedule();
 	}
 	*/
 
 	public void presentToUser(IResource resource, IStrategoTerm feedback) {
 		assert Thread.holdsLock(getSyncRoot());
 		assert feedback != null;
 
 		if (feedback instanceof IStrategoString) {
 			String status = ((IStrategoString)feedback).stringValue();
 			if (status.equals("BACKGROUNDED")) {
 				// Trigger update when needed
 				isUpdateStarted = false;
 				return;
 			} else {
 				throw new StrategoException("Illegal status from " + feedbackFunction + ": " + status);
 			}
 		}
 		
 		// TODO: use FileTrackingIOAgent to find out what to clear
 		// UNDONE: messages.clearAllMarkers();
 		messages.clearMarkers(resource);
 
 		try {
 			feedback = extractResultingAST(resource, feedback);
 			
 			if (feedback.getTermType() == TUPLE
 					&& termAt(feedback, 0).getTermType() == LIST
 					&& termAt(feedback, 1).getTermType() == LIST
 					&& termAt(feedback, 2).getTermType() == LIST) {
 				
 			    IStrategoList errors = termAt(feedback, 0);
 			    IStrategoList warnings = termAt(feedback, 1);
 			    IStrategoList notes = termAt(feedback, 2);
 			    feedbackToMarkers(resource, errors, IMarker.SEVERITY_ERROR);
 			    feedbackToMarkers(resource, warnings, IMarker.SEVERITY_WARNING);
 			    feedbackToMarkers(resource, notes, IMarker.SEVERITY_INFO);
 			} else {
 				// Throw an exception to trigger an Eclipse pop-up  
 				throw new StrategoException("Illegal output from " + feedbackFunction + " (should be (errors,warnings,notes) tuple: " + feedback);
 			}
 		} finally {
 			messages.commitAllChanges();
 		}
 	}
 	
 	private IStrategoTerm extractResultingAST(IResource resource, IStrategoTerm feedback) {
 		if (isTermTuple(feedback) && feedback.getSubtermCount() == 4
 				&& (!"None".equals(cons(feedback.getSubterm(0))) || feedback.getSubterm(0).getSubtermCount() > 0)) {
 			resultingAsts.put(resource, feedback.getSubterm(0));
 			
 			IStrategoTuple newFeedback = Environment.getTermFactory().makeTuple(
 					feedback.getSubterm(1), feedback.getSubterm(2), feedback.getSubterm(3));
 			return newFeedback;
 		} else {
 			resultingAsts.remove(resource);
 			return feedback;
 		}
 	}
 	
 	private final void feedbackToMarkers(IResource resource, IStrategoList feedbacks, int severity) {
 		assert Thread.holdsLock(getSyncRoot());
 		
 		Context context = runtime.getCompiledContext();
 		sdf2imp.init(context);
 		feedbacks = postProcessFeedback(feedbacks, context);
 		
 	    for (IStrategoTerm feedback : feedbacks.getAllSubterms()) {
 	        IStrategoTerm term = termAt(feedback, 0);
 			IStrategoString messageTerm = termAt(feedback, 1);
 			String message = messageTerm.stringValue();
 			
 			messages.addMarker(resource, term, message, severity);
 	    }
 	}
 
 	private IStrategoList postProcessFeedback(IStrategoList feedbacks, Context context) {
 		IStrategoList result =
 				(IStrategoList) postprocess_feedback_results_0_0.instance.invoke(context, feedbacks);
 		if (result == null) {
 			// Throw an exception to trigger an Eclipse pop-up  
 			throw new StrategoException("Illegal output from " + feedbackFunction + ": " + feedbacks);
 		}
 		return result;
 	}	
 	
 	/**
 	 * Invoke a Stratego function with a specific AST node as its input.
 	 * 
 	 * @see #getAstNode(IStrategoTerm)  To retrieve the AST node associated with the resulting term.
 	 */
 	public IStrategoTerm invoke(String function, IStrategoAstNode node)
 			throws UndefinedStrategyException, InterpreterErrorExit, InterpreterExit, InterpreterException {
 
 		IStrategoTerm input = makeInputTerm(node, true);
 		return invoke(function, input, node.getResource());
 	}
 
 	/**
 	 * Create an input term for a control rule.
 	 */
 	public IStrategoTuple makeInputTerm(IStrategoAstNode node, boolean includeSubNode) {
 		return makeInputTerm(node, includeSubNode, false);
 	}
 	
 	/**
 	 * Create an input term for a control rule.
 	 */
 	public IStrategoTuple makeInputTerm(IStrategoAstNode node, boolean includeSubNode, boolean useSourceAst) {
 		assert Thread.holdsLock(getSyncRoot());
 		
 		Context context = getRuntime().getCompiledContext();
 		IStrategoTerm resultingAst = useSourceAst ? null : resultingAsts.get(node.getResource());
 		IStrategoList termPath = StrategoTermPath.getTermPathWithOrigin(context, resultingAst, node);
 		IStrategoTerm targetTerm;
 		IStrategoTerm rootTerm;
 		
 		if (termPath != null) {
 			targetTerm = StrategoTermPath.getTermAtPath(context, resultingAst, termPath);
 			rootTerm = resultingAst;
 		} else {
 			targetTerm = node.getTerm();
 			termPath = StrategoTermPath.createPath(node);
 			rootTerm = getRoot(node).getTerm();
 		}
 		
 		ITermFactory factory = Environment.getTermFactory();
 		String path = node.getResource().getProjectRelativePath().toPortableString();
 		String absolutePath = node.getResource().getProject().getLocation().toOSString();
 		
 		if (includeSubNode) {
 			IStrategoTerm[] inputParts = {
 					targetTerm,
 					termPath,
 					rootTerm,
 					factory.makeString(path),
 					factory.makeString(absolutePath)
 				};
 			return factory.makeTuple(inputParts);
 		} else {
 			IStrategoTerm[] inputParts = {
 					node.getTerm(),
 					factory.makeString(path),
 					factory.makeString(absolutePath)
 				};
 			return factory.makeTuple(inputParts);
 		}
 	}
 
 	/**
 	 * Create an input term for a control rule,
 	 * based on the ATerm syntax of the AST of the source file.
 	 */
 	public IStrategoTuple makeATermInputTerm(IStrategoAstNode node, boolean includeSubNode, IResource resource) {
 		assert Thread.holdsLock(getSyncRoot());
 		stratego_aterm.init(runtime.getCompiledContext());
 		
 		ITermFactory factory = Environment.getTermFactory();
 		String path = resource.getProjectRelativePath().toPortableString();
 		String absolutePath = resource.getProject().getLocation().toOSString();
 		
 		if (includeSubNode) {
 			node = getImplodableNode(node);
 			IStrategoTerm[] inputParts = {
 					implodeATerm(node.getTerm()),
 					StrategoTermPath.createPathFromParsedATerm(node, runtime.getCompiledContext()),
 					implodeATerm(getRoot(node).getTerm()),
 					factory.makeString(path),
 					factory.makeString(absolutePath)
 				};
 			return factory.makeTuple(inputParts);
 		} else {
 			throw new org.spoofax.NotImplementedException();
 		}
 	}
 
 	protected IStrategoTerm implodeATerm(IStrategoTerm term) {
 		return implode_aterm_0_0.instance.invoke(runtime.getCompiledContext(), term);
 	}
 
 	protected IStrategoAstNode getImplodableNode(IStrategoAstNode node) {
 		if (node.isList() && node.getChildren().size() == 1)
 			node = (IStrategoAstNode) node.getChildren().get(0);
 		for (; node != null; node = node.getParent()) {
 			if (implodeATerm(node.getTerm()) != null)
 				return node;
 		}
 		throw new IllegalStateException("Could not identify selected AST node from ATerm editor");
 	}
 	
 	/**
 	 * Invoke a Stratego function with a specific term its input,
 	 * given a particular working directory.
 	 */
 	public IStrategoTerm invoke(String function, IStrategoTerm term, IResource resource)
 			throws UndefinedStrategyException, InterpreterErrorExit, InterpreterExit, InterpreterException {
 		
 		synchronized (getSyncRoot()) {
 			if (runtime == null) initialize(new NullProgressMonitor());
 			if (runtime == null) return null;
 			
 		    Debug.startTimer();
 			// TODO: Make Context support monitor.isCanceled()?
 			//       (e.g., overriding Context.lookupPrimitive to throw an OperationCanceledException) 
 			
 			runtime.setCurrent(term);
 			configureRuntime(resource);
 
 			((LoggingIOAgent) runtime.getIOAgent()).clearLog();
 			assert runtime.getCompiledContext().getOperatorRegistry(IMPJSGLRLibrary.REGISTRY_NAME)
 					instanceof IMPJSGLRLibrary;
 			boolean success = runtime.invoke(function);
 			
 			// Cleanup input term.
 			IStrategoTerm result = runtime.current();
 			runtime.setCurrent(null);
 			
 			Debug.stopTimer("Evaluated strategy " + function + (success ? "" : " (failed)"));
 			return success ? result : null;
 		}
 	}
 
 	/**
 	 * Invoke a Stratego function with a specific AST node as its input,
 	 * logging and swallowing all exceptions.
 	 * 
 	 * @see #getAstNode(IStrategoTerm)  To retrieve the AST node associated with the resulting term.
 	 */
 	public IStrategoTerm invokeSilent(String function, IStrategoAstNode node) {
 		try {
 			return invokeSilent(function, makeInputTerm(node, true), node.getResource());
 		} catch (RuntimeException e) {
 			if (runtime != null) runtime.getIOAgent().printError("Internal error evaluating " + function + " (" + name(e) + "; see error log)");
 			Environment.logException("Internal error evaluating strategy " + function, e);
 			if (descriptor.isDynamicallyLoaded()) StrategoConsole.activateConsole();
 			return null;
 		}
 	}
 	
 	/**
 	 * Invoke a Stratego function with a specific term its input,
 	 * given a particular working directory.
 	 * Logs and swallows all exceptions.
 	 */
 	public IStrategoTerm invokeSilent(String function, IStrategoTerm input, IResource resource) {
 		assert Thread.holdsLock(getSyncRoot());
 		IStrategoTerm result = null;
 		
 		try {
 			wasExceptionLogged = true;
 			result = invoke(function, input, resource);
 			wasExceptionLogged = false;
 		} catch (InterpreterExit e) {
 			if (descriptor.isDynamicallyLoaded()) StrategoConsole.activateConsole();
 			messages.clearMarkers(resource);
 			if (!(e instanceof InterpreterErrorExit))
 				messages.addMarkerFirstLine(resource, "Analysis failed (" + name(e) + "; see error log)", IMarker.SEVERITY_ERROR);
 			messages.commitAllChanges();
 			Environment.logException("Runtime exited when evaluating strategy " + function, e);
 		} catch (UndefinedStrategyException e) {
 			// Note that this condition may also be reached when the semantic service hasn't been loaded yet
			if (descriptor.isDynamicallyLoaded())
				runtime.getIOAgent().printError("Internal error: strategy does not exist or is defined in a module that is not imported: " + e.getMessage());
 			Environment.logException("Strategy does not exist: " + e.getMessage(), e);
 		} catch (InterpreterException e) {
			if (descriptor.isDynamicallyLoaded())
				runtime.getIOAgent().printError("Internal error evaluating " + function + " (" + name(e) + "; see error log)");
 			Environment.logException("Internal error evaluating strategy " + function, e);
 			if (descriptor.isDynamicallyLoaded()) StrategoConsole.activateConsole();
 		} catch (RuntimeException e) {
			if (runtime != null && descriptor.isDynamicallyLoaded())
				runtime.getIOAgent().printError("Internal error evaluating " + function + " (" + name(e) + "; see error log)");
 			Environment.logException("Internal error evaluating strategy " + function, e);
 			if (descriptor.isDynamicallyLoaded()) StrategoConsole.activateConsole();
 		} catch (Error e) { // e.g. NoClassDefFoundError due to bad/missing stratego jar
			if (runtime != null && descriptor.isDynamicallyLoaded())
				runtime.getIOAgent().printError("Internal error evaluating " + function + " (" + name(e) + "; see error log)");
 			Environment.logException("Internal error evaluating strategy " + function, e);
 			if (descriptor.isDynamicallyLoaded()) StrategoConsole.activateConsole();
 		}
 		
 		return result;
 	}
 	
 	private static String name(Throwable e) {
 		return e.getClass().getSimpleName();
 	}
 
 	public IAst getAstNode(IStrategoTerm term) {
 		if (term == null) return null;
 			
 		if (term instanceof WrappedAstNode) {
 			return ((WrappedAstNode) term).getNode();
 		} else {
 			if (descriptor.isDynamicallyLoaded()) {
 				Environment.logWarning("Resolved reference is not associated with an AST node " + term);
 			} else {
 				Environment.logException("Resolved reference is not associated with an AST node " + term);
 			}
 			return null;
 		}
 	}
 	
 	private void configureRuntime(IResource resource) {
 		assert Thread.holdsLock(getSyncRoot());
 		
 		try {
 			ITermFactory factory = runtime.getFactory();
 			IStrategoTuple programName = factory.makeTuple(
 					factory.makeString("program"),
 					factory.makeString(descriptor.getLanguage().getName()));
 			set_config_0_0.instance.invoke(runtime.getCompiledContext(), programName);
 		} catch (BadDescriptorException e) {
 			// Ignore; use default program name
 		}
 		
 		try {
 			EditorIOAgent io = (EditorIOAgent) runtime.getIOAgent();
 			io.setWorkingDir(resource.getLocation().removeLastSegments(1).toOSString());
 			io.setProjectPath(resource.getProject().getLocation().toOSString());
 			io.setDescriptor(descriptor);
 		} catch (IOException e) {
 			Environment.logException("Could not set Stratego working directory", e);
 			throw new RuntimeException(e);
 		}
 	}
 	
 	private static IStrategoAstNode getRoot(IStrategoAstNode node) {
 		while (node.getParent() != null)
 			node = node.getParent();
 		return node;
 	}
 	
 	public HybridInterpreter getRuntime() {
 		assert Thread.holdsLock(getSyncRoot());
 		if (runtime == null) initialize(new NullProgressMonitor());
 		if (runtime == null) createEmptyRuntime(new NullProgressMonitor()); // create empty runtime
 		return runtime;
 	}
 
 	public void prepareForReinitialize() {
 		// Do nothing
 	}
 
 	public void reinitialize(Descriptor newDescriptor) throws BadDescriptorException {
 		synchronized (getSyncRoot()) {
 			runtimePrototypes.remove(descriptor);
 			runtime = null;
 			descriptor = newDescriptor;
 		}
 	}
 
 }
