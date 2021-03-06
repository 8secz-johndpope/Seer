 package de.peeeq.wurstscript.intermediateLang.interpreter;
 
 import java.io.File;
import java.security.acl.LastOwnerException;
 
 import de.peeeq.wurstscript.ast.Annotation;
import de.peeeq.wurstscript.ast.AstElement;
 import de.peeeq.wurstscript.ast.AstElementWithModifiers;
 import de.peeeq.wurstscript.ast.Modifier;
 import de.peeeq.wurstscript.gui.WurstGui;
 import de.peeeq.wurstscript.intermediateLang.ILconst;
 import de.peeeq.wurstscript.jassIm.ImFunction;
 import de.peeeq.wurstscript.jassIm.ImProg;
 import de.peeeq.wurstscript.jassIm.ImStmt;
 import de.peeeq.wurstscript.jassIm.ImVar;
 import de.peeeq.wurstscript.jassIm.ImVoid;
 import de.peeeq.wurstscript.jassinterpreter.ReturnException;
import de.peeeq.wurstscript.parser.WPos;
 import de.peeeq.wurstscript.utils.Utils;
 
 public class ILInterpreter {
 	private ImProg prog;
 	private final ProgramState globalState;
 	
 
 	
 	public ILInterpreter(ImProg prog, WurstGui gui, File mapFile, ProgramState globalState) {
 		this.prog = prog;
 		this.globalState = globalState;
 		globalState.addNativeProvider(new BuiltinFuncs(globalState));
 		globalState.addNativeProvider(new NativeFunctions());
 	}
 
 	public ILInterpreter(ImProg prog, WurstGui gui, File mapFile) {
 		this(prog, gui, mapFile, new ProgramState(mapFile, gui));
 	}
 
 	public static LocalState runFunc(ProgramState globalState, ImFunction f, ILconst ... args) {
 		String[] parameterTypes = new String[args.length];
 		for (int i=0; i<args.length; i++) {
 			parameterTypes[i] = "" + args[i];
 		}
 		System.out.println("calling function " + f.getName() + "("+ Utils.printSep(", ", parameterTypes) +  ")");
 		
 		if (isCompiletimeNative(f)) {
 			return runBuiltinFunction(globalState, f, args);
 		}
 		
 		
 		if (f.isNative()) {
 			return runBuiltinFunction(globalState, f, args);
 		}
 		LocalState localState = new LocalState();
 		int i = 0;
 		for (ImVar p : f.getParameters()) {
 			localState.setVal(p, args[i]);
 			i++;
 		}
 		try {
 			f.getBody().runStatements(globalState, localState);
 		} catch (ReturnException e) {
 			return localState.setReturnVal(e.getVal());
 		}
 		if (f.getReturnType() instanceof ImVoid) {
 			return localState;
 		}
 		throw new InterprationError("function " + f.getName() + " did not return any value...");
 	}
 
 	private static LocalState runBuiltinFunction(ProgramState globalState, ImFunction f, ILconst... args)	throws Error, InterprationError {
 		
 		for (NativesProvider natives : globalState.getNativeProviders()) {
 			try {
 				System.out.println("Searchin func " + f.getName() + " in " + natives.getClass());
 				return new LocalState(natives.invoke(f.getName(), args));
 			} catch (NoSuchNativeException e) {
 				// ignore
 			}
 		}
		WPos source = globalState.getLastStatement().attrTrace().attrSource();
		globalState.getOutStream().println("function " + f.getName() + " not found (line " + source.getLine() + " in " + source.getFile() + ")");
		return new LocalState();
 	}
 
 	private static boolean isCompiletimeNative(ImFunction f) {
 		if (f.getTrace() instanceof AstElementWithModifiers) {
 			AstElementWithModifiers f2 = (AstElementWithModifiers) f.getTrace();
 			for (Modifier m : f2.getModifiers()) {
 				if (m instanceof Annotation) {
 					Annotation annotation = (Annotation) m;
 					if (annotation.getAnnotationType().equals("@compiletimenative")) {
 						return true;
 					}
 				}
 			}
 		}
 		return false;
 	}
 
 	public LocalState executeFunction(String funcName) {
 		for (ImFunction f : prog.getFunctions()) {
 			if (f.getName().equals(funcName)) {
 				return runFunc(globalState, f);
 			}
 		}
 		throw new Error("no function with name "+ funcName + "was found.");
 	}
 
 	public void runVoidFunc(ImFunction f) {
 		runFunc(globalState, f);
 	}
 
 	public ImStmt getLastStatement() {
 		return globalState.getLastStatement();
 	}
 
 	public void writebackGlobalState() {
 		globalState.writeBack();
 		
 	}
 
 	public ProgramState getGlobalState() {
 		return globalState;
 	}
 
 	public void addNativeProvider(NativesProvider np) {
 		globalState.addNativeProvider(np);		
 	}
 
 	public void setProgram(ImProg imProg) {
 		this.prog = imProg;
 	}
 
 
 }
