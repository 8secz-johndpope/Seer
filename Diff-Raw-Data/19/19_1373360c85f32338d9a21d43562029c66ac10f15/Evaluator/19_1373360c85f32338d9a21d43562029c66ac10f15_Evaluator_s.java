 /*******************************************************************************
  * Copyright (c) 2009-2013 CWI
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
 
  *   * 
  *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
  *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
  *   * Emilie Balland - (CWI)
  *   * Anya Helene Bagge - anya@ii.uib.no (Univ. Bergen)
  *   * Paul Klint - Paul.Klint@cwi.nl - CWI
  *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
  *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
  *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI 
 *******************************************************************************/
 package org.rascalmpl.interpreter;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.Reader;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.Stack;
 import java.util.concurrent.CopyOnWriteArrayList;
 
 import org.eclipse.imp.pdb.facts.IConstructor;
 import org.eclipse.imp.pdb.facts.IInteger;
 import org.eclipse.imp.pdb.facts.IList;
 import org.eclipse.imp.pdb.facts.IListWriter;
 import org.eclipse.imp.pdb.facts.IMap;
 import org.eclipse.imp.pdb.facts.INode;
 import org.eclipse.imp.pdb.facts.IRelation;
 import org.eclipse.imp.pdb.facts.ISet;
 import org.eclipse.imp.pdb.facts.ISetWriter;
 import org.eclipse.imp.pdb.facts.ISourceLocation;
 import org.eclipse.imp.pdb.facts.IValue;
 import org.eclipse.imp.pdb.facts.IValueFactory;
 import org.eclipse.imp.pdb.facts.type.Type;
 import org.eclipse.imp.pdb.facts.type.TypeFactory;
 import org.eclipse.imp.pdb.facts.visitors.VisitorException;
 import org.rascalmpl.ast.AbstractAST;
 import org.rascalmpl.ast.Command;
 import org.rascalmpl.ast.Commands;
 import org.rascalmpl.ast.Declaration;
 import org.rascalmpl.ast.EvalCommand;
 import org.rascalmpl.ast.Expression;
 import org.rascalmpl.ast.Import;
 import org.rascalmpl.ast.Name;
 import org.rascalmpl.ast.QualifiedName;
 import org.rascalmpl.ast.Statement;
 import org.rascalmpl.interpreter.asserts.ImplementationError;
 import org.rascalmpl.interpreter.asserts.NotYetImplemented;
 import org.rascalmpl.interpreter.callbacks.IConstructorDeclared;
 import org.rascalmpl.interpreter.control_exceptions.Failure;
 import org.rascalmpl.interpreter.control_exceptions.Insert;
 import org.rascalmpl.interpreter.control_exceptions.InterruptException;
 import org.rascalmpl.interpreter.control_exceptions.Return;
 import org.rascalmpl.interpreter.control_exceptions.Throw;
 import org.rascalmpl.interpreter.debug.DebugUpdater;
 import org.rascalmpl.interpreter.debug.IRascalSuspendTrigger;
 import org.rascalmpl.interpreter.debug.IRascalSuspendTriggerListener;
 import org.rascalmpl.interpreter.env.Environment;
 import org.rascalmpl.interpreter.env.GlobalEnvironment;
 import org.rascalmpl.interpreter.env.ModuleEnvironment;
 import org.rascalmpl.interpreter.load.IRascalSearchPathContributor;
 import org.rascalmpl.interpreter.load.RascalURIResolver;
 import org.rascalmpl.interpreter.load.StandardLibraryContributor;
 import org.rascalmpl.interpreter.load.URIContributor;
 import org.rascalmpl.interpreter.matching.IBooleanResult;
 import org.rascalmpl.interpreter.matching.IMatchingResult;
 import org.rascalmpl.interpreter.result.AbstractFunction;
 import org.rascalmpl.interpreter.result.ICallableValue;
 import org.rascalmpl.interpreter.result.OverloadedFunction;
 import org.rascalmpl.interpreter.result.Result;
 import org.rascalmpl.interpreter.result.ResultFactory;
 import org.rascalmpl.interpreter.staticErrors.StaticError;
 import org.rascalmpl.interpreter.staticErrors.UndeclaredFunction;
 import org.rascalmpl.interpreter.staticErrors.UnguardedFail;
 import org.rascalmpl.interpreter.staticErrors.UnguardedInsert;
 import org.rascalmpl.interpreter.staticErrors.UnguardedReturn;
 import org.rascalmpl.interpreter.types.RascalTypeFactory;
 import org.rascalmpl.interpreter.utils.JavaBridge;
 import org.rascalmpl.interpreter.utils.Modules;
 import org.rascalmpl.interpreter.utils.Names;
 import org.rascalmpl.interpreter.utils.Profiler;
 import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
 import org.rascalmpl.library.lang.rascal.newsyntax.RascalParser;
 import org.rascalmpl.parser.ASTBuilder;
 import org.rascalmpl.parser.Parser;
 import org.rascalmpl.parser.ParserGenerator;
 import org.rascalmpl.parser.gtd.IGTD;
 import org.rascalmpl.parser.gtd.exception.ParseError;
 import org.rascalmpl.parser.gtd.io.InputConverter;
 import org.rascalmpl.parser.gtd.result.action.IActionExecutor;
 import org.rascalmpl.parser.gtd.result.out.DefaultNodeFlattener;
 import org.rascalmpl.parser.uptr.UPTRNodeFactory;
 import org.rascalmpl.parser.uptr.action.NoActionExecutor;
 import org.rascalmpl.parser.uptr.action.RascalFunctionActionExecutor;
 import org.rascalmpl.parser.uptr.recovery.Recoverer;
 import org.rascalmpl.uri.CWDURIResolver;
 import org.rascalmpl.uri.ClassResourceInputOutput;
 import org.rascalmpl.uri.FileURIResolver;
 import org.rascalmpl.uri.HomeURIResolver;
 import org.rascalmpl.uri.HttpURIResolver;
 import org.rascalmpl.uri.JarURIResolver;
 import org.rascalmpl.uri.TempURIResolver;
 import org.rascalmpl.uri.URIResolverRegistry;
 import org.rascalmpl.uri.URIUtil;
 import org.rascalmpl.values.uptr.Factory;
 import org.rascalmpl.values.uptr.ProductionAdapter;
 import org.rascalmpl.values.uptr.SymbolAdapter;
 import org.rascalmpl.values.uptr.TreeAdapter;
 import org.rascalmpl.values.uptr.visitors.IdentityTreeVisitor;
 
 public class Evaluator implements IEvaluator<Result<IValue>>, IRascalSuspendTrigger {
 	private final IValueFactory vf; // sharable
 	private static final TypeFactory tf = TypeFactory.getInstance(); // always shared
 	protected Environment currentEnvt; // not sharable
  
 	private final GlobalEnvironment heap; // shareable if frozen
 	/**
 	 * True if an interrupt has been signalled and we should abort execution
 	 */
 	private boolean interrupt = false;
 
 	private final JavaBridge javaBridge; // TODO: sharable if synchronized
 
 	/**
 	 * Used in runtime error messages
 	 */
 	private AbstractAST currentAST;
 
 	/**
 	 * True if we're doing profiling
 	 */
 	private static boolean doProfiling = false;
 	
 	/**
 	 * This flag helps preventing non-terminating bootstrapping cycles. If 
 	 * it is set we do not allow loading of another nested Parser Generator.
 	 */
 	private boolean isBootstrapper = false;
 
 	/**
 	 * The current profiler; private to this evaluator
 	 */
 	private Profiler profiler;
 
 	private final TypeDeclarationEvaluator typeDeclarator; // not sharable
 
 	private final List<ClassLoader> classLoaders; // sharable if frozen
 	private final ModuleEnvironment rootScope; // sharable if frozen
 
 	private final PrintWriter defStderr;
 	private final PrintWriter defStdout;
 	private PrintWriter curStderr = null;
 	private PrintWriter curStdout = null;
 
 	/**
 	 * Probably not sharable
 	 */
 	private ITestResultListener testReporter;
 	/**
 	 * To avoid null pointer exceptions, avoid passing this directly to other classes, use
 	 * the result of getMonitor() instead. 
 	 */
 	private IRascalMonitor monitor;
 	
 	private AbstractInterpreterEventTrigger eventTrigger; // TODO: can this be shared?	
 
 	private final List<IRascalSuspendTriggerListener> suspendTriggerListeners;	 // TODO: can this be shared?
 	
 	private Stack<Accumulator> accumulators = new Stack<Accumulator>(); // not sharable
 	private final Stack<String> indentStack = new Stack<String>(); // not sharable
 	private final RascalURIResolver rascalPathResolver; // sharable if frozen
 
 	private final URIResolverRegistry resolverRegistry; // sharable
 
 	private final Map<IConstructorDeclared,Object> constructorDeclaredListeners; // TODO: can this be shared?
 	private static final Object dummy = new Object();	
 	
 	public Evaluator(IValueFactory f, PrintWriter stderr, PrintWriter stdout, ModuleEnvironment scope, GlobalEnvironment heap) {
 		this(f, stderr, stdout, scope, heap, new ArrayList<ClassLoader>(Collections.singleton(Evaluator.class.getClassLoader())), new RascalURIResolver(new URIResolverRegistry()));
 	}
 
 	public Evaluator(IValueFactory vf, PrintWriter stderr, PrintWriter stdout, ModuleEnvironment scope, GlobalEnvironment heap, List<ClassLoader> classLoaders, RascalURIResolver rascalPathResolver) {
 		super();
 		
 		this.vf = vf;
 		this.heap = heap;
 		this.typeDeclarator = new TypeDeclarationEvaluator(this);
 		this.currentEnvt = scope;
 		this.rootScope = scope;
 		heap.addModule(scope);
 		this.classLoaders = classLoaders;
 		this.javaBridge = new JavaBridge(classLoaders, vf);
 		this.rascalPathResolver = rascalPathResolver;
 		this.resolverRegistry = rascalPathResolver.getRegistry();
 		this.defStderr = stderr;
 		this.defStdout = stdout;
 		this.constructorDeclaredListeners = new HashMap<IConstructorDeclared,Object>();
 		this.suspendTriggerListeners = new CopyOnWriteArrayList<IRascalSuspendTriggerListener>();
 		
 		updateProperties();
 
 		if (stderr == null) {
 			throw new NullPointerException();
 		}
 		if (stdout == null) {
 			throw new NullPointerException();
 		}
 
 		rascalPathResolver.addPathContributor(StandardLibraryContributor.getInstance());
 
 		// register some schemes
 		FileURIResolver files = new FileURIResolver();
 		resolverRegistry.registerInputOutput(files);
 
 		HttpURIResolver http = new HttpURIResolver();
 		resolverRegistry.registerInput(http);
 
 		CWDURIResolver cwd = new CWDURIResolver();
 		resolverRegistry.registerInputOutput(cwd);
 
 		ClassResourceInputOutput library = new ClassResourceInputOutput(resolverRegistry, "std", getClass(), "/org/rascalmpl/library");
 		resolverRegistry.registerInputOutput(library);
 
 		ClassResourceInputOutput testdata = new ClassResourceInputOutput(resolverRegistry, "testdata", getClass(), "/org/rascalmpl/test/data");
 		resolverRegistry.registerInput(testdata);
 		
 		ClassResourceInputOutput benchmarkdata = new ClassResourceInputOutput(resolverRegistry, "benchmarks", getClass(), "/org/rascalmpl/benchmark");
 		resolverRegistry.registerInput(benchmarkdata);
 		
 		resolverRegistry.registerInput(new JarURIResolver(getClass()));
 
 		resolverRegistry.registerInputOutput(rascalPathResolver);
 
 		resolverRegistry.registerInputOutput(new HomeURIResolver());
 		resolverRegistry.registerInputOutput(new TempURIResolver());
 		
 		ClassResourceInputOutput courses = new ClassResourceInputOutput(resolverRegistry, "courses", getClass(), "src/org/rascalmpl/courses");
 		resolverRegistry.registerInputOutput(courses);
 		ClassResourceInputOutput tutor = new ClassResourceInputOutput(resolverRegistry, "tutor", getClass(), "/org/rascalmpl/tutor");
 		resolverRegistry.registerInputOutput(tutor);
 		
 		// default event trigger to swallow events
 		setEventTrigger(AbstractInterpreterEventTrigger.newNullEventTrigger());
 	}
 
 	private Evaluator(Evaluator source, ModuleEnvironment scope) {
 		super();
 		
 		// this.accumulators = source.accumulators;
 		// this.testReporter = source.testReporter;
 		this.vf = source.vf;
 		this.heap = source.heap;
 		this.typeDeclarator = new TypeDeclarationEvaluator(this);
 		// TODO: this is probably not OK
 		this.currentEnvt = scope;
 		this.rootScope = scope;
 		// TODO: this is probably not OK
 		heap.addModule(scope);
 		this.classLoaders = source.classLoaders;
 		// TODO: the Java bridge is probably sharable if its methods are synchronized
 		this.javaBridge = new JavaBridge(classLoaders, vf);
 		this.rascalPathResolver = source.rascalPathResolver;
 		this.resolverRegistry = source.resolverRegistry;
 		this.defStderr = source.defStderr;
 		this.defStdout = source.defStdout;
 		this.constructorDeclaredListeners = new HashMap<IConstructorDeclared,Object>(source.constructorDeclaredListeners);
 		this.suspendTriggerListeners = new CopyOnWriteArrayList<IRascalSuspendTriggerListener>(source.suspendTriggerListeners);
 		
 		updateProperties();
 		
 		// default event trigger to swallow events
 		setEventTrigger(AbstractInterpreterEventTrigger.newNullEventTrigger());
 	}
 
 	@Override
 	public IRascalMonitor setMonitor(IRascalMonitor monitor) {
 		if (monitor == this) {
 			return monitor;
 		}
 		
 		interrupt = false;
 		IRascalMonitor old = monitor;
 		this.monitor = monitor;
 		return old;
 	}
 	
 	@Override	
 	public int endJob(boolean succeeded) {
 		if (monitor != null)
 			return monitor.endJob(succeeded);
 		return 0;
 	}
 
 	@Override	
 	public void event(int inc) {
 		if (monitor != null)
 			monitor.event(inc);
 	}
 
 	@Override	
 	public void event(String name, int inc) {
 		if (monitor != null)
 			monitor.event(name, inc);
 	}
 
 	@Override	
 	public void event(String name) {
 		if (monitor != null)
 			monitor.event(name);
 	}
 
 	@Override	
 	public void startJob(String name, int workShare, int totalWork) {
 		if (monitor != null)
 			monitor.startJob(name, workShare, totalWork);
 	}
 
 	@Override	
 	public void startJob(String name, int totalWork) {
 		if (monitor != null)
 			monitor.startJob(name, totalWork);
 	}
 	
 	@Override	
 	public void startJob(String name) {
 		if (monitor != null)
 			monitor.startJob(name);
 	}
 	
 	@Override	
 	public void todo(int work) {
 		if (monitor != null)
 			monitor.todo(work);
 	}
 	
 	@Override	
 	public boolean isCanceled() {
 		if(monitor == null)
 			return false;
 		else
 			return monitor.isCanceled();
 	}
 	
 	@Override
 	public void registerConstructorDeclaredListener(IConstructorDeclared iml) {
 		constructorDeclaredListeners.put(iml,dummy);
 	}
 	
 	@Override	
 	public void notifyConstructorDeclaredListeners() {
 		for (IConstructorDeclared iml : constructorDeclaredListeners.keySet()) {
 			if (iml != null) {
 				iml.handleConstructorDeclaredEvent();
 			}
 		}
 		constructorDeclaredListeners.clear();
 	}
 	
 	@Override
 	public List<ClassLoader> getClassLoaders() {
 		return Collections.unmodifiableList(classLoaders);
 	}
 
 	@Override	
 	public ModuleEnvironment __getRootScope() {
 		return rootScope;
 	}
 
 	@Override	
 	public PrintWriter getStdOut() {
 		return curStdout == null ? defStdout : curStdout;
 	}
 
 	@Override	
 	public TypeDeclarationEvaluator __getTypeDeclarator() {
 		return typeDeclarator;
 	}
 
 	@Override	
 	public GlobalEnvironment __getHeap() {
 		return heap;
 	}
 
 	@Override	
 	public void __setInterrupt(boolean interrupt) {
 		this.interrupt = interrupt;
 	}
 
 	@Override	
 	public boolean __getInterrupt() {
 		return interrupt;
 	}
 
 	@Override	
 	public Stack<Accumulator> __getAccumulators() {
 		return accumulators;
 	}
 
 	@Override	
 	public IValueFactory __getVf() {
 		return vf;
 	}
 
 	public static TypeFactory __getTf() {
 		return tf;
 	}
 
 	@Override	
 	public JavaBridge __getJavaBridge() {
 		return javaBridge;
 	}
 
 	@Override	
 	public void interrupt() {
 		__setInterrupt(true);
 	}
 
 	@Override	
 	public boolean isInterrupted() {
 		return interrupt;
 	}
 
 	@Override	
 	public PrintWriter getStdErr() {
 		return curStderr == null ? defStderr : curStderr;
 	}
 
 	public void setTestResultListener(ITestResultListener l) {
 		testReporter = l;
 	}
 
 	public JavaBridge getJavaBridge() {
 		return javaBridge;
 	}
 
 	@Override	
 	public URIResolverRegistry getResolverRegistry() {
 		return resolverRegistry;
 	}
 
 	@Override
 	public RascalURIResolver getRascalResolver() {
 		return rascalPathResolver;
 	}
 	
 	@Override	
 	public void indent(String n) {
 		indentStack.push(n);
 	}
 	
 	@Override	
 	public void unindent() {
 		indentStack.pop();
 	}
 	
 	@Override	
 	public String getCurrentIndent() {
 		return indentStack.peek();
 	}
 
 	/**
 	 * Call a Rascal function with a number of arguments
 	 * 
 	 * @return either null if its a void function, or the return value of the
 	 *         function.
 	 */
 	@Override
 	public IValue call(IRascalMonitor monitor, String name, IValue... args) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return call(name, args);
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	/**
 	 * Call a Rascal function with a number of arguments
 	 * 
 	 * @return either null if its a void function, or the return value of the
 	 *         function.
 	 */
 	@Override
 	public IValue call(IRascalMonitor monitor, String module, String name, IValue... args) {
 		IRascalMonitor old = setMonitor(monitor);
 		Environment oldEnv = getCurrentEnvt();
 		
 		try {
 			ModuleEnvironment modEnv = getHeap().getModule(module);
 			setCurrentEnvt(modEnv);
 			return call(name, args);
 		}
 		finally {
 			setMonitor(old);
 			setCurrentEnvt(oldEnv);
 		}
 	}
 	
 	@Override
 	public IValue call(String name, IValue... args) {
 	  RascalTypeFactory rtf = RascalTypeFactory.getInstance();
 		QualifiedName qualifiedName = Names.toQualifiedName(name);
 		OverloadedFunction func = (OverloadedFunction) getCurrentEnvt().getVariable(qualifiedName);
 
 		Type[] types = new Type[args.length];
 
 		int i = 0;
 		for (IValue v : args) {
 			Type type = v.getType();
       types[i++] = type.isSubtypeOf(Factory.Tree) ? rtf.nonTerminalType((IConstructor) v) : type;
 		}
 		
 		if (func == null) {
 			throw new UndeclaredFunction(name, types, this, getCurrentAST());
 		}
 
 		return func.call(getMonitor(), types, args, null).getValue();
 	}
 	
 	@Override	
 	public IConstructor parseObject(IConstructor startSort, IMap robust, URI location, char[] input){
 	  assert false;
 		IGTD<IConstructor, IConstructor, ISourceLocation> parser = getObjectParser(location);
 		String name = "";
 		if (SymbolAdapter.isStartSort(startSort)) {
 			name = "start__";
 			startSort = SymbolAdapter.getStart(startSort);
 		}
 		
 		if (SymbolAdapter.isSort(startSort) || SymbolAdapter.isLex(startSort) || SymbolAdapter.isLayouts(startSort)) {
 			name += SymbolAdapter.getName(startSort);
 		}
 
 		int[][] lookaheads = new int[robust.size()][];
 		IConstructor[] robustProds = new IConstructor[robust.size()];
 		initializeRecovery(robust, lookaheads, robustProds);
 		
 		__setInterrupt(false);
 		IActionExecutor<IConstructor> exec = new RascalFunctionActionExecutor(this);
 		
 		return (IConstructor) parser.parse(name, location, input, exec, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory(), robustProds.length == 0 ? null : new Recoverer(robustProds, lookaheads));
 	}
 	
 	/**
 	 * This converts a map from productions to character classes to
 	 * two pair-wise arrays, with char-classes unfolded as lists of ints.
 	 */
 	private void initializeRecovery(IMap robust, int[][] lookaheads, IConstructor[] robustProds) {
 		int i = 0;
 		
 		for (IValue prod : robust) {
 			robustProds[i] = (IConstructor) prod;
 			List<Integer> chars = new LinkedList<Integer>();
 			IList ranges = (IList) robust.get(prod);
 			
 			for (IValue range : ranges) {
 				int from = ((IInteger) ((IConstructor) range).get("begin")).intValue();
 				int to = ((IInteger) ((IConstructor) range).get("end")).intValue();
 				
 				for (int j = from; j <= to; j++) {
 					chars.add(j);
 				}
 			}
 			
 			lookaheads[i] = new int[chars.size()];
 			for (int k = 0; k < chars.size(); k++) {
 				lookaheads[i][k] = chars.get(k);
 			}
 			
 			i++;
 		}
 	}
 
 	@Override
 	public IConstructor parseObject(IRascalMonitor monitor, IConstructor startSort, IMap robust, URI location){
 		IRascalMonitor old = setMonitor(monitor);
 		
 		try{
 			char[] input = getResourceContent(location);
 			return parseObject(startSort, robust, location, input);
 		}catch(IOException ioex){
 			throw RuntimeExceptionFactory.io(vf.string(ioex.getMessage()), getCurrentAST(), getStackTrace());
 		}finally{
 			setMonitor(old);
 		}
 	}
 	
 	@Override
 	public IConstructor parseObject(IRascalMonitor monitor, IConstructor startSort, IMap robust, String input){
 		IRascalMonitor old = setMonitor(monitor);
 		try{
 			return parseObject(startSort, robust, URIUtil.invalidURI(), input.toCharArray());
 		}finally{
 			setMonitor(old);
 		}
 	}
 	
 	@Override
 	public IConstructor parseObject(IRascalMonitor monitor, IConstructor startSort, IMap robust, String input, ISourceLocation loc){
 		IRascalMonitor old = setMonitor(monitor);
 		try{
 			return parseObject(startSort, robust, loc.getURI(), input.toCharArray());
 		}finally{
 			setMonitor(old);
 		}
 	}
 	
 	private IGTD<IConstructor, IConstructor, ISourceLocation> getObjectParser(URI loc){
 		return getNewObjectParser((ModuleEnvironment) getCurrentEnvt().getRoot(), loc, false);
 	}
 
 	@SuppressWarnings("unchecked")
 	private IGTD<IConstructor, IConstructor, ISourceLocation> getNewObjectParser(ModuleEnvironment currentModule, URI loc, boolean force) {
 		if (currentModule.getBootstrap()) {
 			return new RascalParser();
 		}
 		
 		if (currentModule.hasCachedParser()) {
 			String className = currentModule.getCachedParser();
 			Class<?> clazz;
 			for (ClassLoader cl: classLoaders) {
 				try {
 					clazz = cl.loadClass(className);
 					return (IGTD<IConstructor, IConstructor, ISourceLocation>) clazz.newInstance();
 				} catch (ClassNotFoundException e) {
 					continue;
 				} catch (InstantiationException e) {
 					throw new ImplementationError("could not instantiate " + className + " to valid IGTD parser", e);
 				} catch (IllegalAccessException e) {
 					throw new ImplementationError("not allowed to instantiate " + className + " to valid IGTD parser", e);
 				}
 			}
 			throw new ImplementationError("class for cached parser " + className + " could not be found");
 		}
 
 		ParserGenerator pg = getParserGenerator();
 		IMap definitions = currentModule.getSyntaxDefinition();
 		
 		Class<IGTD<IConstructor, IConstructor, ISourceLocation>> parser = getHeap().getObjectParser(currentModule.getName(), definitions);
 
 		if (parser == null || force) {
 			String parserName = currentModule.getName(); // .replaceAll("::", ".");
 
 			parser = pg.getNewParser(this, loc, parserName, definitions);
 			getHeap().storeObjectParser(currentModule.getName(), definitions, parser);
 		}
 
 		try {
 			return parser.newInstance();
 		} catch (InstantiationException e) {
 			throw new ImplementationError(e.getMessage(), e);
 		} catch (IllegalAccessException e) {
 			throw new ImplementationError(e.getMessage(), e);
 		} catch (ExceptionInInitializerError e) {
 			throw new ImplementationError(e.getMessage(), e);
 		}
 	}
 
 	@Override
 	public IConstructor getGrammar(Environment env) {
 		ModuleEnvironment root = (ModuleEnvironment) env.getRoot();
 		return getParserGenerator().getGrammar(monitor, root.getName(), root.getSyntaxDefinition());
 	}
 	
 	@Override
 	public IConstructor getGrammar(IRascalMonitor monitor, URI uri) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			ParserGenerator pgen = getParserGenerator();
 			String main = uri.getAuthority();
 			ModuleEnvironment env = getHeap().getModule(main);
 			return pgen.getGrammar(monitor, main, env.getSyntaxDefinition());
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	public IValue diagnoseAmbiguity(IRascalMonitor monitor, IConstructor parseTree) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			ParserGenerator pgen = getParserGenerator();
 			return pgen.diagnoseAmbiguity(parseTree);
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	public IConstructor getExpandedGrammar(IRascalMonitor monitor, URI uri) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			ParserGenerator pgen = getParserGenerator();
 			String main = uri.getAuthority();
 			ModuleEnvironment env = getHeap().getModule(main);
 			return pgen.getExpandedGrammar(monitor, main, env.getSyntaxDefinition());
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	public IRelation getNestingRestrictions(IRascalMonitor monitor, IConstructor g) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			ParserGenerator pgen = getParserGenerator();
 			return pgen.getNestingRestrictions(monitor, g);
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	private ParserGenerator parserGenerator;
   
 	
 	private ParserGenerator getParserGenerator() {
 		startJob("Loading parser generator", 40);
 		if(parserGenerator == null ){
 		  if (isBootstrapper()) {
 		    throw new ImplementationError("Cyclic bootstrapping is occurring, probably because a module in the bootstrap dependencies is using the concrete syntax feature.");
 		  }
 			parserGenerator = new ParserGenerator(getMonitor(), getStdErr(), classLoaders, getValueFactory());
 		}
 		endJob(true);
 		return parserGenerator;
 	}
 
 	@Override	
 	public void setCurrentAST(AbstractAST currentAST) {
 		this.currentAST = currentAST;
 	}
 
 	@Override	
 	public AbstractAST getCurrentAST() {
 		return currentAST;
 	}
 
 	public void addRascalSearchPathContributor(IRascalSearchPathContributor contrib) {
 		rascalPathResolver.addPathContributor(contrib);
 	}
 
 	public void addRascalSearchPath(final URI uri) {
 		rascalPathResolver.addPathContributor(new URIContributor(uri));
 	}
  
 	public void addClassLoader(ClassLoader loader) {
 		// later loaders have precedence
 		classLoaders.add(0, loader);
 	}
 
 	@Override	
 	public StackTrace getStackTrace() {
 		StackTrace trace = new StackTrace();
 		Environment env = currentEnvt;
 		while (env != null) {
 			trace.add(env.getLocation(), env.getName());
 			env = env.getCallerScope();
 		}
 		return trace.freeze();
 	}
 
 	/**
 	 * Evaluate a statement
 	 * 
 	 * Note, this method is not supposed to be called within another overriden
 	 * eval(...) method, because it triggers and idle event after evaluation is
 	 * done.
 	 * 
 	 * @param stat
 	 * @return
 	 */
 	@Override	
 	public Result<IValue> eval(Statement stat) {
 		__setInterrupt(false);
 		try {
 			if (Evaluator.doProfiling) {
 				profiler = new Profiler(this);
 				profiler.start();
 
 			}
 			currentAST = stat;
 			try {
 				return stat.interpret(this);
 			} finally {
 				if (Evaluator.doProfiling) {
 					if (profiler != null) {
 						profiler.pleaseStop();
 						profiler.report();
 						profiler = null;
 					}
 				}
 				getEventTrigger().fireIdleEvent();
 			}
 		} catch (Return e) {
 			throw new UnguardedReturn(stat);
 		} catch (Failure e) {
 			throw new UnguardedFail(stat, e);
 		} catch (Insert e) {
 			throw new UnguardedInsert(stat);
 		}
 	}
 
 	/**
 	 * Parse and evaluate a command in the current execution environment
 	 * 
 	 * Note, this method is not supposed to be called within another overriden
 	 * eval(...) method, because it triggers and idle event after evaluation is
 	 * done.
 	 * 
 	 * @param command
 	 * @return
 	 */
 	@Override
 	public Result<IValue> eval(IRascalMonitor monitor, String command, URI location) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return eval(command, location);
 		}
 		finally {
 			setMonitor(old);
 			getEventTrigger().fireIdleEvent();
 		}
 	}
 	
 	/**
 	 * Parse and evaluate a command in the current execution environment
 	 * 
 	 * Note, this method is not supposed to be called within another overriden
 	 * eval(...) method, because it triggers and idle event after evaluation is
 	 * done.	 
 	 * 
 	 * @param command
 	 * @return
 	 */
 	@Override
 	public Result<IValue> evalMore(IRascalMonitor monitor, String commands, URI location) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return evalMore(commands, location);
 		}
 		finally {
 			setMonitor(old);
 			getEventTrigger().fireIdleEvent();
 		}
 	}
 
 	private Result<IValue> eval(String command, URI location)
 			throws ImplementationError {
 		__setInterrupt(false);
     IActionExecutor<IConstructor> actionExecutor =  new NoActionExecutor();
 		IConstructor tree = new RascalParser().parse(Parser.START_COMMAND, location, command.toCharArray(), actionExecutor, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory());
 		
 		if (!noBacktickOutsideStringConstant(command)) {
 		  tree = parseFragments(tree, getCurrentModuleEnvironment());
 		}
 		
 		Command stat = getBuilder().buildCommand(tree);
 		
 		if (stat == null) {
 			throw new ImplementationError("Disambiguation failed: it removed all alternatives");
 		}
 
 		return eval(stat);
 	}
 	
 	private Result<IValue> evalMore(String command, URI location)
 			throws ImplementationError {
 		__setInterrupt(false);
 		IConstructor tree;
 		
 		IActionExecutor<IConstructor> actionExecutor = new NoActionExecutor();
 		tree = new RascalParser().parse(Parser.START_COMMANDS, location, command.toCharArray(), actionExecutor, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory());
 	
 	  if (!noBacktickOutsideStringConstant(command)) {
 	    tree = parseFragments(tree, getCurrentModuleEnvironment());
 		}
 
 		Commands stat = getBuilder().buildCommands(tree);
 		
 		if (stat == null) {
 			throw new ImplementationError("Disambiguation failed: it removed all alternatives");
 		}
 
 		return eval(stat);
 	}
 
 	/*
 	 * This is dangereous, since inside embedded concrete fragments there may be unbalanced
 	 * double quotes as well as unbalanced backticks. For now it is a workaround that prevents
 	 * generation of parsers when some backtick is inside a string constant.
 	 */
 	private boolean noBacktickOutsideStringConstant(String command) {
 		boolean instring = false;
 		byte[] b = command.getBytes();
 		
 		for (int i = 0; i < b.length; i++) {
 			if (b[i] == '\"') {
 				instring = !instring;
 			}
 			else if (!instring && b[i] == '`') {
 				return false;
 			}
 		}
 		
 		return true;
 	}
 	
 	@Override
 	public IConstructor parseCommand(IRascalMonitor monitor, String command, URI location) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return parseCommand(command, location);
 		}
 		finally {
 			setMonitor(old);
 		}
 	}	
 	
 	private IConstructor parseCommand(String command, URI location) {
 		__setInterrupt(false);
     IActionExecutor<IConstructor> actionExecutor =  new NoActionExecutor();
 		IConstructor tree =  new RascalParser().parse(Parser.START_COMMAND, location, command.toCharArray(), actionExecutor, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory());
 
 		if (!noBacktickOutsideStringConstant(command)) {
 		  tree = parseFragments(tree, getCurrentModuleEnvironment());
 		}
 		
 		return tree;
 	}
 
 	@Override
 	public IConstructor parseCommands(IRascalMonitor monitor, String commands, URI location) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			__setInterrupt(false);
 		  IActionExecutor<IConstructor> actionExecutor =  new NoActionExecutor();
       IConstructor tree = new RascalParser().parse(Parser.START_COMMANDS, location, commands.toCharArray(), actionExecutor, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory());
   
 			if (!noBacktickOutsideStringConstant(commands)) {
 			  tree = parseFragments(tree, getCurrentModuleEnvironment());
 			}
 			
 			return tree;
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	@Override
 	public Result<IValue> eval(IRascalMonitor monitor, Command command) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return eval(command);
 		}
 		finally {
 			setMonitor(old);
 			getEventTrigger().fireIdleEvent();
 		}
 	}
 	
 	private Result<IValue> eval(Commands commands) {
 		__setInterrupt(false);
 		if (Evaluator.doProfiling) {
 			profiler = new Profiler(this);
 			profiler.start();
 
 		}
 		try {
 			Result<IValue> last = ResultFactory.nothing();
 			for (EvalCommand command : commands.getCommands()) {
 				last = command.interpret(this);
 			}
 			return last;
 		} finally {
 			if (Evaluator.doProfiling) {
 				if (profiler != null) {
 					profiler.pleaseStop();
 					profiler.report();
 					profiler = null;
 				}
 			}
 		}
 	}
 	
 	private Result<IValue> eval(Command command) {
 		__setInterrupt(false);
 		if (Evaluator.doProfiling) {
 			profiler = new Profiler(this);
 			profiler.start();
 
 		}
 		try {
 			return command.interpret(this);
 		} finally {
 			if (Evaluator.doProfiling) {
 				if (profiler != null) {
 					profiler.pleaseStop();
 					profiler.report();
 					profiler = null;
 				}
 			}
 		}
 	}
 
 	/**
 	 * Evaluate a declaration
 	 * 
 	 * @param declaration
 	 * @return
 	 */
 	public Result<IValue> eval(IRascalMonitor monitor, Declaration declaration) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			__setInterrupt(false);
 			currentAST = declaration;
 			Result<IValue> r = declaration.interpret(this);
 			if (r != null) {
 				return r;
 			}
 
 			throw new NotYetImplemented(declaration);
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 	
 	public void doImport(IRascalMonitor monitor, String string) {
 		IRascalMonitor old = setMonitor(monitor);
 		interrupt = false;
 		try {
 			eval("import " + string + ";", URIUtil.rootScheme("import"));
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 
 	public void reloadModules(IRascalMonitor monitor, Set<String> names, URI errorLocation) {
 		reloadModules(monitor, names, errorLocation, true);
 	}
 	
 	// TODO Update for extends; extends need to be cleared and reinterpreted.
 	private void reloadModules(IRascalMonitor monitor, Set<String> names, URI errorLocation, boolean recurseToExtending) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			Set<String> onHeap = new HashSet<String>();
 			Set<String> extendingModules = new HashSet<String>();
 
 			
 			try {
 				monitor.startJob("Cleaning modules", names.size());
 				for (String mod : names) {
 					if (heap.existsModule(mod)) {
 						//System.err.println("NOTE: will reload " + mod + " + all its dependents");
 						onHeap.add(mod);
 						if (recurseToExtending) {
 							extendingModules.addAll(heap.getExtendingModules(mod));
 						}
 						heap.removeModule(heap.getModule(mod));
 					}
 					monitor.event("Processed " + mod, 1);
 				}
 				extendingModules.removeAll(names);
 			} finally {
 				monitor.endJob(true);
 			}
 			
 			try {
 				monitor.startJob("Reloading modules", onHeap.size());
 				for (String mod : onHeap) {
 					if (!heap.existsModule(mod)) {
 						defStderr.print("Reloading module " + mod);
 						reloadModule(mod, errorLocation);
 					}
 					monitor.event("loaded " + mod, 1);
 				}
 			} finally {
 				monitor.endJob(true);
 			}
 			
 			Set<String> dependingImports = new HashSet<String>();
 			Set<String> dependingExtends = new HashSet<String>();
 			dependingImports.addAll(getImportingModules(names));
 			dependingExtends.addAll(getExtendingModules(names));
 
 			try {
 				monitor.startJob("Reconnecting importers of affected modules", dependingImports.size());
 				for (String mod : dependingImports) {
 					ModuleEnvironment env = heap.getModule(mod);
 					Set<String> todo = new HashSet<String>(env.getImports());
 					for (String imp : todo) {
 						if (names.contains(imp)) {
 							env.unImport(imp);
 							ModuleEnvironment imported = heap.getModule(imp);
 							if (imported != null) {
 								env.addImport(imp, imported);
 							}
 						}
 
 					}
 					monitor.event("Reconnected " + mod, 1);
 				}
 			}
 			finally {
 				monitor.endJob(true);
 			}
 				
 			try {
 				monitor.startJob("Reconnecting extenders of affected modules", dependingExtends.size());
 				for (String mod : dependingExtends) {
 					ModuleEnvironment env = heap.getModule(mod);
 					Set<String> todo = new HashSet<String>(env.getExtends());
 					for (String ext : todo) {
 						if (names.contains(ext)) {
 							env.unExtend(ext);
 							ModuleEnvironment extended = heap.getModule(ext);
 							if (extended != null) {
 								env.addExtend(ext);
 							}
 						}
 					}
 					monitor.event("Reconnected " + mod, 1);
 				}
 			} finally {
 				monitor.endJob(true);
 			}
 			
 			if (recurseToExtending && !extendingModules.isEmpty()) {
 				reloadModules(monitor, extendingModules, errorLocation, false);
 			}
 			
 			if (!names.isEmpty()) {
 				notifyConstructorDeclaredListeners();
 			}
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 
 	private void reloadModule(String name, URI errorLocation) {	
 		ModuleEnvironment env = new ModuleEnvironment(name, getHeap());
 		heap.addModule(env);
 
 		try {
 			ISourceLocation loc = getValueFactory().sourceLocation(errorLocation);
       org.rascalmpl.semantics.dynamic.Import.loadModule(loc, name, this);
 		} catch (StaticError e) {
 			heap.removeModule(env);
 			throw e;
 		} catch (Throw e) {
 			heap.removeModule(env);
 			throw e;
 		} 
 	}
 
 	/**
 	 * transitively compute which modules depend on the given modules
 	 * @param names
 	 * @return
 	 */
 	private Set<String> getImportingModules(Set<String> names) {
 		Set<String> found = new HashSet<String>();
 		LinkedList<String> todo = new LinkedList<String>(names);
 		
 		while (!todo.isEmpty()) {
 			String mod = todo.pop();
 			Set<String> dependingModules = heap.getImportingModules(mod);
 			dependingModules.removeAll(found);
 			found.addAll(dependingModules);
 			todo.addAll(dependingModules);
 		}
 		
 		return found;
 	}
 	
 	private Set<String> getExtendingModules(Set<String> names) {
 		Set<String> found = new HashSet<String>();
 		LinkedList<String> todo = new LinkedList<String>(names);
 		
 		while (!todo.isEmpty()) {
 			String mod = todo.pop();
 			Set<String> dependingModules = heap.getExtendingModules(mod);
 			dependingModules.removeAll(found);
 			found.addAll(dependingModules);
 			todo.addAll(dependingModules);
 		}
 		
 		return found;
 	}
 	
 	@Override	
 	public void unwind(Environment old) {
 		setCurrentEnvt(old);
 	}
 
 	@Override	
 	public void pushEnv() {
 		Environment env = new Environment(getCurrentEnvt(), currentAST != null ? currentAST.getLocation() : null, getCurrentEnvt().getName());
 		setCurrentEnvt(env);
 	}
 	
 	@Override  
 	public void pushEnv(String name) {
 		Environment env = new Environment(getCurrentEnvt(), currentAST != null ? currentAST.getLocation() : null, name);
 		setCurrentEnvt(env);
 	}
 
 	@Override	
 	public Environment pushEnv(Statement s) {
 		/* use the same name as the current envt */
 		Environment env = new Environment(getCurrentEnvt(), s.getLocation(), getCurrentEnvt().getName());
 		setCurrentEnvt(env);
 		return env;
 	}
 
 	
 	@Override	
 	public void printHelpMessage(PrintWriter out) {
 		out.println("Welcome to the Rascal command shell.");
 		out.println();
 		out.println("Shell commands:");
 		out.println(":help                      Prints this message");
 		out.println(":quit or EOF               Quits the shell");
 		out.println(":declarations              Lists all visible rules, functions and variables");
 		out.println(":set <option> <expression> Sets an option");
 		out.println("e.g. profiling    true/false");
 		out.println("     tracing      true/false");
 		out.println(":edit <modulename>         Opens an editor for that module");
 		out.println(":modules                   Lists all imported modules");
 		out.println(":test                      Runs all unit tests currently loaded");
 		out.println(":unimport <modulename>     Undo an import");
 		out.println(":undeclare <name>          Undeclares a variable or function introduced in the shell");
 		out.println(":history                   Print the command history");
 		out.println();
 		out.println("Example rascal statements and declarations:");
 		out.println("1 + 1;                     Expressions simply print their output and (static) type");
 		out.println("int a;                     Declarations allocate a name in the current scope");
 		out.println("a = 1;                     Assignments store a value in a (optionally previously declared) variable");
 		out.println("int a = 1;                 Declaration with initialization");
 		out.println("import IO;                 Importing a module makes its public members available");
 		out.println("println(\"Hello World\")     Function calling");
 		out.println();
 		out.println("Please read the manual for further information");
 		out.flush();
 	}
 
 	// Modules -------------------------------------------------------------
 	@Override	
 	public ModuleEnvironment getCurrentModuleEnvironment() {
 		if (!(currentEnvt instanceof ModuleEnvironment)) {
 			throw new ImplementationError("Current env should be a module environment");
 		}
 		return ((ModuleEnvironment) currentEnvt);
 	}
 
 	private char[] getResourceContent(URI location) throws IOException{
 		char[] data;
 		Reader textStream = null;
 		
 		try {
 			textStream = resolverRegistry.getCharacterReader(location);
 			data = InputConverter.toChar(textStream);
 		}
 		finally{
 			if(textStream != null){
 				textStream.close();
 			}
 		}
 		
 		return data;
 	}
 	
 	/**
 	 * Parse a module. Practical for implementing IDE features or features that
 	 * use Rascal to implement Rascal. Parsing a module currently has the side
 	 * effect of declaring non-terminal types in the given environment.
 	 */
 	@Override
 	public IConstructor parseModule(IRascalMonitor monitor, URI location, ModuleEnvironment env) throws IOException{
 	  // TODO remove this code and replace by facility in rascal-eclipse to retrieve the
 	  // correct file references from a rascal:// URI
 	  URI resolved = rascalPathResolver.resolve(location);
 	  if(resolved != null){
 	    location = resolved;
 	  }
 		return parseModule(monitor, getResourceContent(location), location, env);
 	}
 	
 	public IConstructor parseModule(IRascalMonitor monitor, char[] data, URI location, ModuleEnvironment env){
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			return newParseModule(data, location, env, true);
 		}
 		finally{
 			setMonitor(old);
 		}
 	}
 	
 	private IConstructor newParseModule(char[] data, URI location, ModuleEnvironment env, boolean declareImportsAndSyntax){
    __setInterrupt(false);
     IActionExecutor<IConstructor> actions = new NoActionExecutor();
 
     System.err.print("PARSING MODULE: " + location + "\r");
 
     startJob("Parsing", 10);
     event("Pre-parsing: " + location);
     IConstructor tree = new RascalParser().parse(Parser.START_MODULE, location, data, actions, new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>(), new UPTRNodeFactory());
 
     IConstructor top = TreeAdapter.getStartTop(tree);
     
     String name = Modules.getName(top);
     
     // create the current module if it does not exist yet
     if (env == null){
       env = heap.getModule(name);
       if(env == null){
         env = new ModuleEnvironment(name, heap);
         heap.addModule(env);
       }
       env.setBootstrap(needBootstrapParser(data));
     }
     
     // make sure all the imported and extended modules are loaded
     // since they may provide additional syntax definitions
     Environment old = getCurrentEnvt();
     try {
       setCurrentEnvt(env);
       env.setInitialized(true);
       
       ISet rules = Modules.getSyntax(top);
       for (IValue rule : rules) {
         evalImport((IConstructor) rule);
       }
       
       ISet imports = Modules.getImports(top);
       for (IValue mod : imports) {
         evalImport((IConstructor) mod);
       }
       
       ISet extend = Modules.getExtends(top);
       for (IValue mod : extend) {
         evalImport((IConstructor) mod);
       }
       
       ISet externals = Modules.getExternals(top);
       for (IValue mod : externals) {
         evalImport((IConstructor) mod);
       }
       
     }
     finally {
       setCurrentEnvt(old);
     }
     
     // parse the embedded concrete syntax fragments of the current module
     IConstructor result = tree;
     if (!isBootstrapper() && (needBootstrapParser(data) || (env.definesSyntax() && containsBackTick(data, 0)))) {
       result = parseFragments(tree, env);
     }
     
     if (!suspendTriggerListeners.isEmpty()) {
       result = DebugUpdater.pushDownAttributes(result);
     }
     
     return result;
   }
 	
 	private void evalImport(IConstructor mod) {
     Import imp = (Import) getBuilder().buildValue(mod);
     imp.interpret(this);
   }
 
   /**
 	 * This function will reconstruct a parse tree of a module, where all nested concrete syntax fragments
 	 * have been parsed and their original flat literal strings replaced by fully structured parse trees.
 	 * 
 	 * @param module is a parse tree of a Rascal module containing flat concrete literals
 	 * @param parser is the parser to use for the concrete literals
 	 * @return parse tree of a module with structured concrete literals, or parse errors
 	 */
 	private IConstructor parseFragments(IConstructor module, final ModuleEnvironment env) {
 	  // TODO: update source code locations!!
 	  
 	  try {
 	   return (IConstructor) module.accept(new IdentityTreeVisitor() {
 	     @Override
 	     public IConstructor visitTreeAppl(IConstructor tree) throws VisitorException {
 	       IConstructor pattern = getConcretePattern(tree);
 	       
 	       if (pattern != null) {
 	         IConstructor parsedFragment = parseFragment(env, (IConstructor) TreeAdapter.getArgs(tree).get(0));
 	         return TreeAdapter.setArgs(tree, vf.list(parsedFragment));
 	       }
 	       else {
 	         IListWriter w = vf.listWriter();
 	         IList args = TreeAdapter.getArgs(tree);
 	         for (IValue arg : args) {
 	           w.append(arg.accept(this));
 	         }
 	         args = w.done();
 	         
 	         return TreeAdapter.setArgs(tree, args);
 	       }
 	     }
 
 	     private IConstructor getConcretePattern(IConstructor tree) {
 	       String sort = TreeAdapter.getSortName(tree);
 	       if (sort.equals("Expression") || sort.equals("Pattern")) {
 	         String cons = TreeAdapter.getConstructorName(tree);
 	         if (cons.equals("concrete")) {
 	           return (IConstructor) TreeAdapter.getArgs(tree).get(0);
 	         }
 	       }
 	       return null;
       }
 
       @Override
 	     public IConstructor visitTreeAmb(IConstructor arg) throws VisitorException {
 	       throw new ImplementationError("unexpected ambiguity: " + arg);
 	     }
 	   });
 	  } catch (VisitorException e) {
 	    throw new ImplementationError("unexpected error while parsing concrete syntax fragments", e.getCause());
 	  }
   }
 	private IConstructor parseFragment(ModuleEnvironment env, IConstructor tree) {
     IConstructor symTree = TreeAdapter.getArg(tree, "symbol");
     IConstructor lit = TreeAdapter.getArg(tree, "parts");
     Map<String, IConstructor> antiquotes = new HashMap<String,IConstructor>();
     
     IGTD<IConstructor, IConstructor, ISourceLocation> parser = env.getBootstrap() ? new RascalParser() : getNewObjectParser(env, TreeAdapter.getLocation(tree).getURI(), false);
     
     try {
       String parserMethodName = getParserGenerator().getParserMethodName(symTree);
       URI uri = getCurrentAST().getLocation().getURI();
       DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation> converter = new DefaultNodeFlattener<IConstructor, IConstructor, ISourceLocation>();
       UPTRNodeFactory nodeFactory = new UPTRNodeFactory();
     
       char[] input = replaceAntiQuotesByHoles(lit, antiquotes);
       
       getStdOut().println("Parsing: [" + Arrays.toString(input) + "]");
       
       IConstructor fragment = (IConstructor) parser.parse(parserMethodName, uri, input, converter, nodeFactory);
       fragment = replaceHolesByAntiQuotes(fragment, antiquotes);
 
       IConstructor prod = TreeAdapter.getProduction(tree);
       IConstructor sym = ProductionAdapter.getDefined(prod);
       sym = SymbolAdapter.delabel(sym); 
       prod = ProductionAdapter.setDefined(prod, (IConstructor) Factory.Symbol_Label.make(vf, vf.string("$parsed"), sym));
       return TreeAdapter.setProduction(TreeAdapter.setArg(tree, "parts", fragment), prod);
     }
     catch (ParseError e) {
       // have to deal with this parse error later when interpreting the AST, for now we just reconstruct the unparsed tree.
       getStdOut().println("Failed at: " + TreeAdapter.getLocation(tree) + ", failed on: [" + new String(replaceAntiQuotesByHoles(lit, antiquotes)) + "], " + e);
       throw e;
 //      return tree;
     }
   }
 	
 	private char[] replaceAntiQuotesByHoles(IConstructor lit, Map<String, IConstructor> antiquotes) {
 	  IList parts = TreeAdapter.getArgs(lit);
 	  StringBuilder b = new StringBuilder();
 	  
 	  for (IValue elem : parts) {
 	    IConstructor part = (IConstructor) elem;
 	    String cons = TreeAdapter.getConstructorName(part);
 	    
 	    if (cons.equals("text")) {
 	      b.append(TreeAdapter.yield(part));
 	    }
 	    else if (cons.equals("newline")) {
 	      b.append('\n');
 	    }
 	    else if (cons.equals("lt")) {
 	      b.append('<');
 	    }
 	    else if (cons.equals("gt")) {
 	      b.append('>');
 	    }
 	    else if (cons.equals("bq")) {
 	      b.append('`');
 	    }
 	    else if (cons.equals("bs")) {
 	      b.append('\\');
 	    }
 	    else if (cons.equals("hole")) {
         b.append(createHole(part, antiquotes));
       }
  	  }
 	  
 	  return b.toString().toCharArray();
 	}
 
   private String createHole(IConstructor part, Map<String, IConstructor> antiquotes) {
     String ph = getParserGenerator().createHole(part, antiquotes.size());
     antiquotes.put(ph, part);
     return ph;
   }
 
   private IConstructor replaceHolesByAntiQuotes(IConstructor fragment, final Map<String, IConstructor> antiquotes) {
     try {
       return (IConstructor) fragment.accept(new IdentityTreeVisitor() {
         @Override
         public IConstructor visitTreeAppl(IConstructor tree) throws VisitorException {
           String cons = TreeAdapter.getConstructorName(tree);
           if (cons == null || !cons.equals("$MetaHole") ) {
             IListWriter w = vf.listWriter();
             IList args = TreeAdapter.getArgs(tree);
             for (IValue elem : args) {
               w.append(elem.accept(this));
             }
             args = w.done();
             
             return TreeAdapter.setArgs(tree, args);
           }
           
           IConstructor type = retrieveHoleType(tree);
           return antiquotes.get(TreeAdapter.yield(tree)).setAnnotation("holeType", type);
         }
         
         private IConstructor retrieveHoleType(IConstructor tree) {
           IConstructor prod = TreeAdapter.getProduction(tree);
           ISet attrs = ProductionAdapter.getAttributes(prod);
 
           for (IValue attr : attrs) {
             if (((IConstructor) attr).getConstructorType() == Factory.Attr_Tag) {
               IValue arg = ((IConstructor) attr).get(0);
               
               if (arg.getType().isNodeType() && ((INode) arg).getName().equals("holeType")) {
                 return (IConstructor) ((INode) arg).get(0);
               }
             }
           }
           
           throw new ImplementationError("expected to find a holeType, but did not: " + tree);
         }
 
         @Override
         public IConstructor visitTreeAmb(IConstructor arg) throws VisitorException {
           ISetWriter w = vf.setWriter();
           for (IValue elem : TreeAdapter.getAlternatives(arg)) {
             w.insert(elem.accept(this));
           }
           return arg.set("alternatives", w.done());
         }
       });
     }
     catch (VisitorException e) {
       throw new ImplementationError("failure while parsing fragments", e);
     }
   }
  
  private static boolean containsBackTick(char[] data, int offset) {
 		for (int i = data.length - 1; i >= offset; --i) {
 			if (data[i] == '`')
 				return true;
 		}
 		return false;
 	}
 	
 	public boolean needBootstrapParser(char[] input) {
 	  return new String(input).contains("@bootstrapParser");
 	}
 	
 	@Override	
 	public ASTBuilder getBuilder() {
 		return new ASTBuilder();
 	}
 	
 	@Override	
 	public boolean matchAndEval(Result<IValue> subject, Expression pat, Statement stat) {
 		boolean debug = false;
 		Environment old = getCurrentEnvt();
 		pushEnv();
 
 		try {
 			IMatchingResult mp = pat.getMatcher(this);
 			mp.initMatch(subject);
 
 			while (mp.hasNext()) {
 				pushEnv();
 				
 				if (interrupt) {
 					throw new InterruptException(getStackTrace(), getCurrentAST().getLocation());
 				}
 
 				if (mp.next()) {
 					try {
 						try {
 							stat.interpret(this);
 						} catch (Insert e) {
 							// Make sure that the match pattern is set
 							if (e.getMatchPattern() == null) {
 								e.setMatchPattern(mp);
 							}
 							throw e;
 						}
 						return true;
 					} catch (Failure e) {
 						// unwind(old); // can not clean up because you don't
 						// know how far to roll back
 					}
 				}
 			}
 		} finally {
 			if (debug)
 				System.err.println("Unwind to old env");
 			unwind(old);
 		}
 		return false;
 	}
 
 	@Override	
 	public boolean matchEvalAndReplace(Result<IValue> subject, Expression pat, List<Expression> conditions, Expression replacementExpr) {
 		Environment old = getCurrentEnvt();
 		try {
 			IMatchingResult mp = pat.getMatcher(this);
 			mp.initMatch(subject);
 
 			while (mp.hasNext()) {
 				if (interrupt)
 					throw new InterruptException(getStackTrace(), getCurrentAST().getLocation());
 				if (mp.next()) {
 					int size = conditions.size();
 					
 					if (size == 0) {
 						throw new Insert(replacementExpr.interpret(this), mp);
 					}
 					
 					IBooleanResult[] gens = new IBooleanResult[size];
 					Environment[] olds = new Environment[size];
 					Environment old2 = getCurrentEnvt();
 
 					int i = 0;
 					try {
 						olds[0] = getCurrentEnvt();
 						pushEnv();
 						gens[0] = conditions.get(0).getBacktracker(this);
 						gens[0].init();
 
 						while (i >= 0 && i < size) {
 
 							if (__getInterrupt()) {
 								throw new InterruptException(getStackTrace(), getCurrentAST().getLocation());
 							}
 							if (gens[i].hasNext() && gens[i].next()) {
 								if (i == size - 1) {
 									// in IfThen the body is executed, here we insert the expression
 									// NB: replaceMentExpr sees the latest bindings of the when clause 
 									throw new Insert(replacementExpr.interpret(this), mp);
 								}
 
 								i++;
 								gens[i] = conditions.get(i).getBacktracker(this);
 								gens[i].init();
 								olds[i] = getCurrentEnvt();
 								pushEnv();
 							} else {
 								unwind(olds[i]);
 								pushEnv();
 								i--;
 							}
 						}
 					} finally {
 						unwind(old2);
 					}
 				}
 			}
 		} finally {
 			unwind(old);
 		}
 		return false;
 	}
 
 	public static final Name IT = ASTBuilder.makeLex("Name", null, "<it>");
 	
 	@Override	
 	public void updateProperties() {
 		Evaluator.doProfiling = Configuration.getProfilingProperty();
 
 		AbstractFunction.setCallTracing(Configuration.getTracingProperty());
 	}
 
 	public Stack<Environment> getCallStack() {
 		Stack<Environment> stack = new Stack<Environment>();
 		Environment env = currentEnvt;
 		while (env != null) {
 			stack.add(0, env);
 			env = env.getCallerScope();
 		}
 		return stack;
 	}
 
 	@Override	
 	public Environment getCurrentEnvt() {
 		return currentEnvt;
 	}
 
 	@Override	
 	public void setCurrentEnvt(Environment env) {
 		currentEnvt = env;
 	}
 
 	@Override	
 	public IEvaluator<Result<IValue>> getEvaluator() {
 		return this;
 	}
 
 	@Override	
 	public GlobalEnvironment getHeap() {
 		return __getHeap();
 	}
 
 	@Override	
 	public boolean runTests(IRascalMonitor monitor) {
 		IRascalMonitor old = setMonitor(monitor);
 		try {
 			final boolean[] allOk = new boolean[] { true };
 			final ITestResultListener l = testReporter != null ? testReporter : new DefaultTestResultListener(getStdOut());
 
 			new TestEvaluator(this, new ITestResultListener() {
 
 				@Override
 				public void report(boolean successful, String test, ISourceLocation loc, String message) {
 					if (!successful)
 						allOk[0] = false;
 					l.report(successful, test, loc, message);
 				}
 
 				@Override
 				public void done() {
 					l.done();
 				}
 
 				@Override
 				public void start(int count) {
 					l.start(count);
 				}
 			}).test();
 			return allOk[0];
 		}
 		finally {
 			setMonitor(old);
 		}
 	}
 
 	@Override	
 	public IValueFactory getValueFactory() {
 		return __getVf();
 	}
 
 	public void setAccumulators(Accumulator accu) {
 		__getAccumulators().push(accu);
 	}
 
 	@Override	
 	public Stack<Accumulator> getAccumulators() {
 		return __getAccumulators();
 	}
 
 	@Override
 	public void setAccumulators(Stack<Accumulator> accumulators) {
 		this.accumulators = accumulators;
 	}
 
 	@Override
 	public IRascalMonitor getMonitor() {
 		if (monitor != null)
 			return monitor;
 		
 		return new NullRascalMonitor();
 	}
 
 	public void overrideDefaultWriters(PrintWriter newStdOut, PrintWriter newStdErr) {
 		this.curStdout = newStdOut;
 		this.curStderr = newStdErr;
 	}
 
 	public void revertToDefaultWriters() {
 		this.curStderr = null;
 		this.curStdout = null;
 	}
 
 	public Result<IValue> call(IRascalMonitor monitor, ICallableValue fun, Type[] argTypes, IValue[] argValues) {
 		if (Evaluator.doProfiling && profiler == null) {
 			profiler = new Profiler(this);
 			profiler.start();
 			try {
 				return fun.call(monitor, argTypes, argValues, null);
 			} finally {
 				if (profiler != null) {
 					profiler.pleaseStop();
 					profiler.report();
 					profiler = null;
 				}
 			}
 		}
 		else {
 			return fun.call(monitor, argTypes, argValues, null);
 		}
 	}
 	
 	@Override
 	public void addSuspendTriggerListener(IRascalSuspendTriggerListener listener) {
 		suspendTriggerListeners.add(listener);
 	}
 
 	@Override
 	public void removeSuspendTriggerListener(
 			IRascalSuspendTriggerListener listener) {
 		suspendTriggerListeners.remove(listener);
 	}
 	
 	@Override
 	public void notifyAboutSuspension(AbstractAST currentAST) {
 		if (!suspendTriggerListeners.isEmpty() && currentAST.isBreakable()) {
 			 /* 
 			  * NOTE: book-keeping of the listeners and notification takes place here,
 			  * delegated from the individual AST nodes.
 			  */
 			for (IRascalSuspendTriggerListener listener : suspendTriggerListeners) {
 				listener.suspended(this, currentAST);
 			}
 		}
 	}
 
 	public AbstractInterpreterEventTrigger getEventTrigger() {
 		return eventTrigger;
 	}
 
 	public void setEventTrigger(AbstractInterpreterEventTrigger eventTrigger) {
 		this.eventTrigger = eventTrigger;
 	}
 
   @Override
 	public void freeze() {
 		// TODO Auto-generated method stub
 	}
 
 	@Override
 	public IEvaluator<Result<IValue>> fork() {
 		return new Evaluator(this, rootScope);
 	}
 
   public void setBootstrapperProperty(boolean b) {
     this.isBootstrapper = b;
   }
   
   public boolean isBootstrapper() {
     return isBootstrapper;
   }
 }
