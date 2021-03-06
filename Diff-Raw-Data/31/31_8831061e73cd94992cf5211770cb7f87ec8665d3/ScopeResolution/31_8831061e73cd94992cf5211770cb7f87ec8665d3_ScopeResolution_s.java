 package jkit.java.stages;
 
 import java.util.*;
 
 import jkit.compiler.ClassLoader;
 import jkit.compiler.FieldNotFoundException;
 import jkit.compiler.SyntaxError;
 import jkit.java.io.JavaFile;
 import jkit.java.tree.Decl;
 import jkit.java.tree.Expr;
 import jkit.java.tree.Stmt;
 import jkit.java.tree.Value;
 import jkit.java.tree.Decl.Clazz;
 import jkit.java.tree.Decl.Field;
 import jkit.java.tree.Decl.Interface;
 import jkit.java.tree.Decl.Method;
 import jkit.java.tree.Stmt.Case;
 import jkit.jil.Modifier;
 import jkit.jil.SourceLocation;
 import jkit.jil.SyntacticElement;
 import jkit.jil.Type;
 import jkit.util.Pair;
 import jkit.util.Triple;
 
 /**
  * The aim of this class is purely to eliminate ambiguities over the scope of a
  * variable. More specifically, it is to identify all UnresolvedVariable
  * instances and eliminate them. There are several different situations that can
  * arise for a given variable:
  * 
  * <ol>
  * <li>It is declared locally (this is the easiest)</li>
  * <li>It is declared as a field of the current class</li>
  * <li>It is declared as a field of a superclass for the current class</li>
  * <li>It is declared as a field of an enclosing class (i.e. the current class
  * is an inner class)</li>
  * <li>It is declared as a field of a superclass of the enclosing class</li>
  * <li>It is declared as a local variable for an enclosing method (i.e. the
  * variable is used in an anonymous inner class, and the enclosing method has
  * final variable of the appropriate name).</li>
  * </ol>
  * 
  * As an example, consider the following case:
  * 
  * <pre>
  * public class Test {
  * 	public int f = 0;
  * 
  * 	public class Inner1 {
  * 		public int f = 1;
  * 	}
  * 
  * 	public class Inner2 extends Inner1 {
  * 		public void print() {
  * 			System.out.println(f);
  * 		}
  * 	}
  * 
  * 	public static void main(final String[] args) {
  * 		Test x = new Test();
  * 		Inner2 i = x.new Inner2();
  * 		i.print();
  * 	}
  * }
  * </pre>
  * 
  * Here, the question is: <i>what gets printed?</i> The answer is "1". The
  * reason is that scope resolution priorities superclasses over enclosing
  * classes.
  * 
  * The purpose of this class is to apply the rules from the Java Language Spec
  * to determine where a variable is defined, since this is not trivial. In
  * addition to determining the scope of variables, this class also must
  * determine the scope of method calls in a similar fashion. For example,
  * consider the following variant of the above:
  * 
  * <pre>
  * public class Test {
  * 	public int f() {
  * 		return 0;
  * 	}
  * 
  * 	public class Inner1 {
  * 		public int f() {
  * 			return 1;
  * 		}
  * 	}
  * 
  * 	public class Inner2 extends Inner1 {
  * 		public void print() {
  * 			System.out.println(f());
  * 		}
  * 	}
  * 
  * 	public static void main(final String[] args) {
  * 		Test x = new Test();
  * 		Inner2 i = x.new Inner2();
  * 		i.print();
  * 	}
  * }
  * </pre>
  * 
  * In order to resolve these situations, this class introduces "this" variables
  * appropriately. Thus, it modifies the source code slightly to do this. Let us
  * consider a final example to illustrate:
  * 
  * <pre>
  * public class Test {
  * 	public int g() {
  * 		return 0;
  * 	}
  * 
  * 	public class Inner {
  * 		public int f;
  * 
  * 		public void print() {
  * 			System.out.println(f);
  * 			System.out.println(g());
  * 		}
  * 	}
  * }
  * </pre>
  * 
  * This code would be transformed into the following, which remains valid Java:
  * 
  * <pre>
  * public class Test {
  * 	public int g() {
  * 		return 0;
  * 	}
  * 
  * 	public class Inner {
  * 		public int f;
  * 
  * 		public void print() {
  * 			System.out.println(this.f);
  * 			System.out.println(Test.this.g());
  * 		}
  * 	}
  * }
  * </pre>
  * 
  * @author djp
  * 
  */
 public class ScopeResolution {
 	
 	/*
 	 * A Scope represents a declaration which defines some variables that may be
 	 * accessed directly by code contained within this scope.
 	 */
 	private static class Scope {
 		public final HashMap<String,Pair<Type,List<Modifier>>> variables;
 		public Scope() {
 			this.variables = new HashMap<String,Pair<Type,List<Modifier>>>();
 		}
 		public Scope(HashMap<String,Pair<Type,List<Modifier>>> variables) {
 			this.variables = variables;
 		}
 	}
 	
 	private static class ClassScope extends Scope {
 		public Type.Clazz type;
 		public Type.Clazz superType; 	
 		public boolean isStatic;
 		public ClassScope(Type.Clazz type, Type.Clazz superType, boolean isStatic) {			
 			this.type = type;
 			this.superType = superType;
 			this.isStatic = isStatic;
 		}
 	}
 	
 	private static class MethodScope extends Scope {
 		public boolean isStatic;
 
 		public MethodScope(
 				HashMap<String, Pair<Type, List<Modifier>>> variables,
 				boolean isStatic) {
 			super(variables);
 			this.isStatic = isStatic;
 		}
 	}
 	
 	private static class FieldScope extends Scope {
 		public boolean isStatic;
 		public FieldScope(boolean isStatic) {			
 			this.isStatic = isStatic;
 		}
 	}
 	private ClassLoader loader;
 	private TypeSystem types;
 	private int anonymousClassCount = 0;
 	private final Stack<Scope> scopes = new Stack<Scope>();
 	private final LinkedList<String> imports = new LinkedList<String>();
 	
 	public ScopeResolution(ClassLoader loader, TypeSystem types) {
 		this.loader = loader; 
 		this.types = types;
 	}
 	
 	public void apply(JavaFile file) {
 		// First, setup the imports list (in reverse order).
 		imports.add(file.pkg() + ".*");
 		for(Pair<Boolean,String> i : file.imports()) {
 			imports.add(i.second());
 		}		
 		imports.add("java.lang.*");		
 				
 		// Now, traverse the declarations
 		for(Decl d : file.declarations()) {
 			doDeclaration(d,file);
 		}
 		
 		imports.clear();
 	}
 	
 	protected void doDeclaration(Decl d, JavaFile file) {
 		if(d instanceof Interface) {
 			doInterface((Interface)d, file);
 		} else if(d instanceof Clazz) {
 			doClass((Clazz)d, file);
 		} else if(d instanceof Method) {
 			doMethod((Method)d, file);
 		} else if(d instanceof Field) {
 			doField((Field)d, file);
 		}
 	}
 	
 	protected void doInterface(Interface d, JavaFile file) {
 		doClass(d,file);
 	}
 	
 	protected void doClass(Clazz c, JavaFile file) {
 		Type.Clazz myType = (Type.Clazz) c.attribute(Type.class);
 		Type.Clazz superType = null;
 		
 		if(c.superclass() == null) {
 			// java makes writing this so icky ...
 			if (!(myType.pkg().equals("java.lang")
 					&& myType.components().size() == 1 && myType.components()
 					.get(0).first().equals("Object"))) {
 				superType = new Type.Clazz("java.lang", "Object");
 			} 
 		} else {
 			superType = (Type.Clazz) c.superclass().attribute(Type.class);
 		}
 		
 		// Create an appropriate import declaration for this class.
 		imports.addFirst(computeImportDecl(myType));
 				
 		// And, push on a scope representing this class definition.
 		scopes.add(new ClassScope(myType,superType,c.isStatic()));
 		
 		for(Decl d : c.declarations()) {
 			doDeclaration(d, file);
 		}
 		
 		imports.removeFirst();
 		scopes.pop();
 	}
 
 	protected void doMethod(Method d, JavaFile file) {
 		
 		HashMap<String,Pair<Type,List<Modifier>>> params = new HashMap();
 		int count = 1;
 		for (Triple<String, List<Modifier>, jkit.java.tree.Type> t : d
 				.parameters()) {
 			Type type = (Type) t.third().attribute(Type.class);
 
 			// if this method has variable arity, and this is the last parameter
 			// in the declaration, then we need to upgrade it's type to be an
 			// array.
 			if(count++ == d.parameters().size() && d.isVariableArity()) {				
 				type = new Type.Array(type);
 			}
 			
 			Pair<Type, List<Modifier>> p = new Pair(type, t.second());
 			params.put(t.first(), p);
 		}		
 		
 		if (!d.isStatic()) {
 			// put in a type for the special "this" variable, and "super"
 			// variable (if appropriate).
 			ArrayList<Modifier> ms = new ArrayList<Modifier>();
 			ms.add(new Modifier.Base(java.lang.reflect.Modifier.FINAL));
 			ClassScope cs = ((ClassScope) findEnclosingScope(ClassScope.class));
 			Pair<Type, List<Modifier>> p = new Pair(cs.type,ms);
 			params.put("this",p);
 			
 			// now, we'll add super as a variable (if there is a super class).
 			if(cs.superType != null) {
 				params.put("super",new Pair(cs.superType,new ArrayList()));
 			}			
 		}
 		scopes.push(new MethodScope(params,d.isStatic()));
 		
 		// Now, explore the method body for any other things to resolve.
 		doStatement(d.body(), file);
 		
 		scopes.pop(); // leaving scope
 	}
 
 	protected void doField(Field d, JavaFile file) {
 		FieldScope myScope = new FieldScope(d.isStatic());
 		
 		if (!d.isStatic()) {
 			// put in a type for the special "this" variable
 			ArrayList<Modifier> ms = new ArrayList<Modifier>();
 			ms.add(new Modifier.Base(java.lang.reflect.Modifier.FINAL));
 			ClassScope cs = ((ClassScope) findEnclosingScope(ClassScope.class)); 
 			Pair<Type, List<Modifier>> p = new Pair(cs.type,ms);
 			myScope.variables.put("this",p);
 			// now, we'll add super as a variable (if there is a super class).
 			if(cs.superType != null) {
 				myScope.variables.put("super",new Pair(cs.superType,new ArrayList()));
 			}		
 		}
 		
 		scopes.push(myScope);
 		doExpression(d.initialiser(), file);
 		scopes.pop();
 	}
 	
 	protected void doStatement(Stmt e, JavaFile file) {
 		if(e instanceof Stmt.SynchronisedBlock) {
 			doSynchronisedBlock((Stmt.SynchronisedBlock)e, file);
 		} else if(e instanceof Stmt.TryCatchBlock) {
 			doTryCatchBlock((Stmt.TryCatchBlock)e, file);
 		} else if(e instanceof Stmt.Block) {
 			doBlock((Stmt.Block)e, file);
 		} else if(e instanceof Stmt.VarDef) {
 			doVarDef((Stmt.VarDef) e, file);
 		} else if(e instanceof Stmt.Assignment) {
 			doAssignment((Stmt.Assignment) e, file);
 		} else if(e instanceof Stmt.Return) {
 			doReturn((Stmt.Return) e, file);
 		} else if(e instanceof Stmt.Throw) {
 			doThrow((Stmt.Throw) e, file);
 		} else if(e instanceof Stmt.Assert) {
 			doAssert((Stmt.Assert) e, file);
 		} else if(e instanceof Stmt.Break) {
 			doBreak((Stmt.Break) e, file);
 		} else if(e instanceof Stmt.Continue) {
 			doContinue((Stmt.Continue) e, file);
 		} else if(e instanceof Stmt.Label) {
 			doLabel((Stmt.Label) e, file);
 		} else if(e instanceof Stmt.If) {
 			doIf((Stmt.If) e, file);
 		} else if(e instanceof Stmt.For) {
 			doFor((Stmt.For) e, file);
 		} else if(e instanceof Stmt.ForEach) {
 			doForEach((Stmt.ForEach) e, file);
 		} else if(e instanceof Stmt.While) {
 			doWhile((Stmt.While) e, file);
 		} else if(e instanceof Stmt.DoWhile) {
 			doDoWhile((Stmt.DoWhile) e, file);
 		} else if(e instanceof Stmt.Switch) {
 			doSwitch((Stmt.Switch) e, file);
 		} else if(e instanceof Expr.Invoke) {
 			doInvoke((Expr.Invoke) e, file);
 		} else if(e instanceof Expr.New) {
 			doNew((Expr.New) e, file);
 		} else if(e instanceof Decl.Clazz) {
 			doClass((Decl.Clazz)e, file);
 		} else if(e != null) {
 			throw new RuntimeException("Invalid statement encountered: "
 					+ e.getClass());
 		}		
 	}
 	
 	protected void doBlock(Stmt.Block block, JavaFile file) {
 		if(block != null) {
 			scopes.push(new Scope());
 			
 			// now process every statement in this block.
 			for(Stmt s : block.statements()) {
 				doStatement(s, file);
 			}
 		
 			scopes.pop();
 		}
 	}
 	
 	protected void doCatchBlock(Stmt.CatchBlock block, JavaFile file) {
 		if(block != null) {
 			Scope myScope = new Scope();
 			scopes.push(myScope);
 			
 			myScope.variables.put(block.variable(), new Pair((Type.Clazz) block
 					.type().attribute(Type.class), new ArrayList<Modifier>())); 
 					
 			// now process every statement in this block.
 			for(Stmt s : block.statements()) {
 				doStatement(s, file);
 			}
 		
 			scopes.pop();
 		}
 	}
 	
 	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, JavaFile file) {
 		doBlock(block, file);
 		doExpression(block.expr(), file);
 	}
 	
 	protected void doTryCatchBlock(Stmt.TryCatchBlock block, JavaFile file) {
 		doBlock(block, file);		
 		doBlock(block.finaly(), file);		
 		
 		for(Stmt.CatchBlock cb : block.handlers()) {
 			doCatchBlock(cb, file);
 		}
 	}
 	
 	protected void doVarDef(Stmt.VarDef def, JavaFile file) {
 		List<Triple<String, Integer, Expr>> defs = def.definitions();
 		Scope enclosingScope = findEnclosingScope();
 		Type t = (Type) def.type().attribute(Type.class);
 		
 		for(int i=0;i!=defs.size();++i) {
 			Triple<String, Integer, Expr> d = defs.get(i);
 			Type nt = t;											
 			
 			for(int j=0;j!=d.second();++j) {
 				nt = new Type.Array(nt);
 			}
 			
 			enclosingScope.variables.put(d.first(), new Pair(nt, def
 					.modifiers()));
 									
 			Expr e = doExpression(d.third(), file);
 			defs.set(i, new Triple(d.first(),d.second(),e));
 		}		
 	}
 	
 	protected Expr doAssignment(Stmt.Assignment def, JavaFile file) {
 		def.setLhs(doExpression(def.lhs(), file));	
 		def.setRhs(doExpression(def.rhs(), file));
 		return def;
 	}
 	
 	protected void doReturn(Stmt.Return ret, JavaFile file) {
 		ret.setExpr(doExpression(ret.expr(), file));
 	}
 	
 	protected void doThrow(Stmt.Throw ret, JavaFile file) {
 		ret.setExpr(doExpression(ret.expr(), file));
 	}
 	
 	protected void doAssert(Stmt.Assert ret, JavaFile file) {
 		ret.setExpr(doExpression(ret.expr(), file));
 	}
 	
 	protected void doBreak(Stmt.Break brk, JavaFile file) {
 		// nothing	
 	}
 	
 	protected void doContinue(Stmt.Continue brk, JavaFile file) {
 		// nothing
 	}
 	
 	protected void doLabel(Stmt.Label lab, JavaFile file) {						
 		doStatement(lab.statement(), file);
 	}
 	
 	protected void doIf(Stmt.If stmt, JavaFile file) {
 		stmt.setCondition(doExpression(stmt.condition(), file));
 		doStatement(stmt.trueStatement(), file);
 		doStatement(stmt.falseStatement(), file);
 	}
 	
 	protected void doWhile(Stmt.While stmt, JavaFile file) {
 		stmt.setCondition(doExpression(stmt.condition(), file));
 		doStatement(stmt.body(), file);		
 	}
 	
 	protected void doDoWhile(Stmt.DoWhile stmt, JavaFile file) {
 		stmt.setCondition(doExpression(stmt.condition(), file));
 		doStatement(stmt.body(), file);
 	}
 	
 	protected void doFor(Stmt.For stmt, JavaFile file) {
 		scopes.push(new Scope());
 		
 		doStatement(stmt.initialiser(), file);
 		stmt.setCondition(doExpression(stmt.condition(), file));
 		doStatement(stmt.increment(), file);
 		doStatement(stmt.body(), file);
 		
 		scopes.pop();
 	}
 	
 	protected void doForEach(Stmt.ForEach stmt, JavaFile file) {
 		Scope myScope = new Scope();
 		scopes.push(myScope);
 		
 		myScope.variables.put(stmt.var(), new Pair((Type) stmt.type()
 				.attribute(Type.class), stmt.modifiers()));
 		stmt.setSource(doExpression(stmt.source(), file));
 		doStatement(stmt.body(), file);
 		
 		scopes.pop();
 	}
 	
 	protected void doSwitch(Stmt.Switch sw, JavaFile file) {
 		sw.setCondition(doExpression(sw.condition(), file));
 		for(Case c : sw.cases()) {
 			c.setCondition(doExpression(c.condition(), file));
 			for(Stmt s : c.statements()) {
 				doStatement(s, file);
 			}
 		}
 		
 		// should check that case conditions are final constants here.
 	}
 	
 	protected Expr doExpression(Expr e, JavaFile file) {	
 		if(e instanceof Value.Bool) {
 			return doBoolVal((Value.Bool)e, file);
 		} else if(e instanceof Value.Char) {
 			return doCharVal((Value.Char)e, file);
 		} else if(e instanceof Value.Int) {
 			return doIntVal((Value.Int)e, file);
 		} else if(e instanceof Value.Long) {
 			return doLongVal((Value.Long)e, file);
 		} else if(e instanceof Value.Float) {
 			return doFloatVal((Value.Float)e, file);
 		} else if(e instanceof Value.Double) {
 			return doDoubleVal((Value.Double)e, file);
 		} else if(e instanceof Value.String) {
 			return doStringVal((Value.String)e, file);
 		} else if(e instanceof Value.Null) {
 			return doNullVal((Value.Null)e, file);
 		} else if(e instanceof Value.TypedArray) {
 			return doTypedArrayVal((Value.TypedArray)e, file);
 		} else if(e instanceof Value.Array) {
 			return doArrayVal((Value.Array)e, file);
 		} else if(e instanceof Value.Class) {
 			return doClassVal((Value.Class) e, file);
 		} else if(e instanceof Expr.UnresolvedVariable) {
 			return doUnresolvedVariable((Expr.UnresolvedVariable)e, file);
 		} else if(e instanceof Expr.UnOp) {
 			return doUnOp((Expr.UnOp)e, file);
 		} else if(e instanceof Expr.BinOp) {
 			return doBinOp((Expr.BinOp)e, file);
 		} else if(e instanceof Expr.TernOp) {
 			return doTernOp((Expr.TernOp)e, file);
 		} else if(e instanceof Expr.Cast) {
 			return doCast((Expr.Cast)e, file);
 		} else if(e instanceof Expr.InstanceOf) {
 			return doInstanceOf((Expr.InstanceOf)e, file);
 		} else if(e instanceof Expr.Invoke) {
 			return doInvoke((Expr.Invoke) e, file);
 		} else if(e instanceof Expr.New) {
 			return doNew((Expr.New) e, file);
 		} else if(e instanceof Expr.ArrayIndex) {
 			return doArrayIndex((Expr.ArrayIndex) e, file);
 		} else if(e instanceof Expr.Deref) {
 			return doDeref((Expr.Deref) e, file);
 		} else if(e instanceof Stmt.Assignment) {
 			// force brackets			
 			return doAssignment((Stmt.Assignment) e, file);			
 		} else if(e != null) {
 			throw new RuntimeException("Invalid expression encountered: "
 					+ e.getClass());
 		}
 		
 		return null;
 	}
 	
 	protected Expr doDeref(Expr.Deref e, JavaFile file) {
 		Expr target = doExpression(e.target(), file);
 		
 		if(target instanceof Expr.UnresolvedVariable) {
 			// We can get here, if the target represents a package, rather than
 			// a complete class. For example, in the expression
 			// "java.lang.String.class", we initially have "java" as an
 			// unresolved variable. What we need to do is amalgamate all the
 			// package pieces together to form a proper class variable.
 			Expr.UnresolvedVariable uv = (Expr.UnresolvedVariable) target;
 			if(loader.isPackage(uv.value() + "." + e.name())) {
 				return new Expr.UnresolvedVariable(uv.value() + "." + e.name(),
 						new ArrayList(e.attributes()));
 			} else {
 				try {
 					// Ok, need to sanity test that this is indeed a class.
 					jkit.jil.Clazz c = loader.loadClass(new Type.Clazz(uv.value(),e.name()));
 					Expr r = new Expr.ClassVariable(uv.value() + "." + e.name(),new ArrayList(e.attributes()));
 					r.attributes().add(c.type());
 					return r;
 				} catch(ClassNotFoundException cne) {
 					syntax_error(cne.getMessage(),e,cne);
 				}
 			}
 		}
 		
 		if(target instanceof Expr.ClassVariable) {
 			// The question we need to consider here is. If we're dereferencing a
 			// ClassVariable, then does it actually contain the field given, or is
 			// it an inner class?						
 			Expr.ClassVariable cv = (Expr.ClassVariable) target;
 			Type.Clazz type = (Type.Clazz) target.attribute(Type.class);
 			
 			try {
 				Triple<jkit.jil.Clazz, jkit.jil.Field, Type> r = types
 						.resolveField(type, e.name(), loader);
 				// if we get here, then there is such a field.
 				//
 				// so do nothing!
 			} catch(ClassNotFoundException cne) {
 				syntax_error(cne.getMessage(),e,cne);				
 			} catch(FieldNotFoundException fne) {	
 				// Right, if we get here then there is no field ... so maybe
 				// this is actually an inner class (or a syntax error :)
 				try {								
 					Type.Clazz c = loader.resolve(cv.type().replace('.','$') + "$" + e.name(),
 							imports);
 					Expr r = new Expr.ClassVariable(cv.type() + "." + e.name(),
 							new ArrayList(e.attributes()));					
 					r.attributes().add(c);
 					return r;
 				} catch(ClassNotFoundException cne) {
 					// this must be an error...
 					syntax_error("field does not exist: " + cv.type() + "." + e.name(),e,cne);
 				}
 			}
 		}
 		
 		e.setTarget(target);		
 		return e;
 	}
 	
 	protected Expr doArrayIndex(Expr.ArrayIndex e, JavaFile file) {
 		e.setTarget(doExpression(e.target(), file));
 		e.setIndex(doExpression(e.index(), file));
 		
 		return e;
 	}
 	
 	protected Expr doNew(Expr.New e, JavaFile file) {
 		// Second, recurse through any parameters supplied ...
 		List<Expr> parameters = e.parameters();
 		for(int i=0;i!=parameters.size();++i) {
 			Expr p = parameters.get(i);
 			parameters.set(i,doExpression(p, file));
 		}
 		
 		if(e.declarations().size() > 0) {
 			// At this point, we are constructing an anonymous inner
 			// class. We need to push a class scope to indicate to any variable
 			// accesses contained inside that variables declared outside this
 			// correspond to non-local accesses.
 			
 			ClassScope cs = (ClassScope) findEnclosingScope(ClassScope.class);
 			Type.Clazz superType = (Type.Clazz) e.type().attribute(Type.class);
 			ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
 					cs.type.components());
 			ncomponents.add(new Pair(Integer.toString(++anonymousClassCount),
 					new ArrayList()));
 			Type.Clazz myType = new Type.Clazz(cs.type.pkg(), ncomponents);
 			
 			// This is a bug. I need to decide what the anonymous type is more
 			// specifically here. Otherwise, the "this" variable will not be
 			// initialised properly.
 			scopes.push(new ClassScope(myType,superType,false));
 			
 			for(Decl d : e.declarations()) {
 				doDeclaration(d, file);
 			}
 			
 			scopes.pop();
 		}
 		
 		return e;
 	}
 	
 	protected Expr doInvoke(Expr.Invoke e, JavaFile file) {				
 		Expr target = doExpression(e.target(), file);
 		
 		if(target == null && e.name().equals("super")) {
 			// Special case. We're invoking the super constructor. There's not
 			// much we can do here.
 			Type.Clazz thisType = ((ClassScope) findEnclosingScope(ClassScope.class)).type;
 			try {
 				e.attributes().add(getSuperClass(thisType));
 			} catch(ClassNotFoundException cne) {
 				syntax_error(cne.getMessage(), e, cne);
 			}
 		} else if(target == null && e.name().equals("this")) {
 			// Special case. We're invoking the super constructor. There's not
 			// much we can do here.
 			Type thisType = ((ClassScope) findEnclosingScope(ClassScope.class)).type;
 			e.attributes().add(thisType);
 		} else if(target == null) {					
 			boolean isThis = true;
 			
 			// Now, we need to determine whether or not this method invocation
 			// is from a static context. This is because, if it is, then we
 			// cannot use the "this" variable as the receiver. Instead, we'll
 			// need to use the Class itself as the receiver.
 			boolean isStatic = false;			
 			
 			// At this stage, we traverse the available scopes looking for one
 			// which contains a method of the same name. It's interesting to
 			// note that, the scope in question may not contain a method of the
 			// same name with the right types --- but we don't care and neither
 			// does javac. This leads to some interesting bits of code which
 			// seem like they should compile, but don't under javac. For
 			// example:
 			// <pre>
 			// public class Test {
 			//
 			// public void test(String z) {}
 			//
 			// public class Inner {
 			// public void test(int x) {}
 			// public void f(String y) { test(y); }
 			// }}
 			// </pre>						
 			
 			for(int i=scopes.size()-1;i>=0;--i) {
 				Scope s = scopes.get(i);
 				if(s instanceof ClassScope) {
 					// We've found a scope that may contain the method we're
 					// after ...
 					ClassScope cs = (ClassScope) s;							
 					try {
 						// Now, the method we're after may not be declared
 						// explicitly in this scope; rather it may be declared
 						// in a superclass of this class and we must account for
 						// this.
 						if(types.hasMethod(cs.type,e.name(),loader)) {
 							// Ok, we have found the relevant method in question.
 							if(isThis && !isStatic) {
 								target = new Expr.LocalVariable("this",
 										new ArrayList(e.attributes()));			
 								target.attributes().add(cs.type);
 							} else if(!isStatic) {
 								Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
 								cv.attributes().add(cs.type);
 								target = new Expr.Deref(cv, "this",
 										new ArrayList(e.attributes()));								
 							} else {															
 								target = new Expr.ClassVariable(cs.type.toString());
 								target.attributes().add(cs.type);
 							}
 							break;
 						}
 					} catch(ClassNotFoundException cne) {
 						syntax_error(cne.getMessage(), e, cne);
 					}
 					
 					isThis = false;
 					isStatic = cs.isStatic;
 				} else if(s instanceof MethodScope) {
 					isStatic = ((MethodScope)s).isStatic;
 				} else if(s instanceof FieldScope) {
 					isStatic = ((FieldScope)s).isStatic;
 				}
 			}
 			
 			if(target == null) {
 				// sanity check
 				syntax_error("internal failure (unable to determine receiver type)",e);
 			}			
 		} 
 				
 		e.setTarget(target);		
 		
 		List<Expr> parameters = e.parameters();
 		for(int i=0;i!=parameters.size();++i) {
 			Expr p = parameters.get(i);
 			parameters.set(i, doExpression(p, file));
 		}				
 		
 		return e;
 	}
 	
 	protected Expr doInstanceOf(Expr.InstanceOf e, JavaFile file) {
 		e.setLhs(doExpression(e.lhs(),file));
 		return e;
 	}
 	
 	protected Expr doCast(Expr.Cast e, JavaFile file) {
 		e.setExpr(doExpression(e.expr(),file));
 		return e;
 	}
 	
 	protected Expr doBoolVal(Value.Bool e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doCharVal(Value.Char e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doIntVal(Value.Int e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doLongVal(Value.Long e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doFloatVal(Value.Float e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doDoubleVal(Value.Double e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doStringVal(Value.String e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doNullVal(Value.Null e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doTypedArrayVal(Value.TypedArray e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doArrayVal(Value.Array e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doClassVal(Value.Class e, JavaFile file) {
 		return e;
 	}
 	
 	protected Expr doUnresolvedVariable(Expr.UnresolvedVariable e, JavaFile file) {
 		// This method is really the heart of the whole operation defined in
 		// this class. It is at this point that we have encountered a variable
 		// and we now need to determine what it's scope is. To do this, we
 		// traverse up the stack of scopes looking for an enclosing scope which
 		// contains a variable with the same name.
 				
 
 		// Now, we need to determine whether or not this method invocation
 		// is from a static context. This is because, if it is, then we
 		// cannot use the "this" variable as the receiver. Instead, we'll
 		// need to use the Class itself as the receiver.
 		boolean isStatic = false;		
 		
 		// At this stage, we traverse the available scopes looking for one
 		// which contains the field we're after. 
 		
 		boolean isThis = true;		
 		for(int i=scopes.size()-1;i>=0;--i) {
 			Scope s = scopes.get(i);
 			if(s instanceof ClassScope) {
 				// resolve field from here
 				ClassScope cs = (ClassScope) s;		
 				
 				try {
 					Triple<jkit.jil.Clazz, jkit.jil.Field, Type> r = types
 							.resolveField(cs.type, e.value(), loader);
 					
 					// Ok, this variable access corresponds to a field load.
 					if(isThis && !isStatic) {
 						Expr thisvar = new Expr.LocalVariable("this",
 								new ArrayList(e.attributes()));
 						thisvar.attributes().add(cs.type);
 						return new Expr.Deref(thisvar, e.value(), e
 							.attributes());
 					} else if(!isStatic){						
 						// Create a class access variable.
 						Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
 						cv.attributes().add(cs.type);
 						return new Expr.Deref(new Expr.Deref(cv, "this",
 								new ArrayList(e.attributes())), e.value(), e
 								.attributes());
 					} else {						
 						// Create a class access variable. A key issue we need
 						// to check for, is whether or not the variable in
 						// question is static or not (as, if not, then we have a
 						// syntax error)
 						if (!r.second().isStatic()) {
 							syntax_error("Cannot access non-static field \""
 									+ e.value() + "\" from static context", e);
 						}
 						
 						Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
 						cv.attributes().add(cs.type);
 						return new Expr.Deref(cv, e.value(), e
 								.attributes());
 					} 
 				} catch(ClassNotFoundException cne) {					
 				} catch(FieldNotFoundException fne) {					
 				}
 				isThis = false;
 				isStatic = cs.isStatic; 
 			} else if(s.variables.containsKey(e.value())) {
 				Expr r;
 				if(isThis) {				
 					r = new Expr.LocalVariable(e.value(),
 							new ArrayList(e.attributes()));
 				} else {
 					// Check whether or not the non-local variable is declared
 					// final (as this is a Java requirement).
 					if (!hasModifier(java.lang.reflect.Modifier.FINAL,
 							s.variables.get(e.value()).second())) {
 						// no it doesn't
 						syntax_error(
 								"local variable \""
 										+ e.value()
 										+ "\" accessed from inner class; needs to be declared final",
 								e);
 					}											
 				}
 				// Yes, it does.
 				r = new Expr.NonLocalVariable(e.value(), new ArrayList(e
 						.attributes()));	
 				// add the variables type here.
 				r.attributes().add(s.variables.get(e.value()).first());
 				return r;
 			} else if(s instanceof MethodScope) {
 				isStatic = ((MethodScope)s).isStatic;
 			} else if(s instanceof FieldScope) {
 				isStatic = ((FieldScope)s).isStatic;
 			}
 		}
 		
 		// If we get here, then this variable access is either a syntax error,
 		// or a static class access. For example, in "System.out" we initially
 		// have "System" marked as a variable. In practice, we need to extend
 		// this to be a ClassVariable. So, we check whether or not it actually
 		// could represent a class.
 					
 		try {						
 			Type.Clazz c = loader.resolve(e.value(), imports);
 			Expr r = new Expr.ClassVariable(e.value(),new ArrayList(e.attributes()));
 			r.attributes().add(c);
 			return r;
 		} catch(ClassNotFoundException ex) {			
 			// no, can't find any class which could represent this variable.
 			// Maybe it's a package.
 			if(loader.isPackage(e.value())) {
 				return e;
 			} else {
 				syntax_error("Cannot find symbol - variable \"" + e.value() + "\"",
 						e, ex);
 				return null; // so very dead!!!
 			}		
 		}
 	}
 
 	protected Expr doUnOp(Expr.UnOp e, JavaFile file) {		
 		e.setExpr(doExpression(e.expr(), file));
 		return e;
 	}
 		
 	protected Expr doBinOp(Expr.BinOp e, JavaFile file) {				
 		e.setLhs(doExpression(e.lhs(), file));
 		e.setRhs(doExpression(e.rhs(), file));
 		return e;
 	}
 	
 	protected Expr doTernOp(Expr.TernOp e, JavaFile file) {	
 		e.setCondition(doExpression(e.condition(), file));
 		e.setTrueBranch(doExpression(e.trueBranch(), file));
 		e.setFalseBranch(doExpression(e.falseBranch(), file));
 		return e;
 	}
 		
 	protected Scope findEnclosingScope() {
 		return scopes.get(scopes.size()-1);
 	}
 	
 	protected Scope findEnclosingScope(Class c) {
 		for(int i=scopes.size()-1;i>=0;--i) {
 			Scope s = scopes.get(i);
 			if(s.getClass().equals(c)) {
 				return s;
 			}
 		}
 		return null;
 	}		
 	
 	/**
 	 * Convert a class reference type into a proper name.
 	 */
 	protected String refName(Type.Clazz ref) {
 		String descriptor = ref.pkg();
 		if(!descriptor.equals("")) {
 			descriptor += ".";
 		}
 		boolean firstTime=true;
 		for(Pair<String,List<Type.Reference>> c : ref.components()) {
 			if(!firstTime) {
 				descriptor += "$";
 			}	
 			firstTime=false;
 			descriptor += c.first();
 		}
 		return descriptor;
 	}
 	
 	/**
 	 * Helper method.
 	 * @param modifier
 	 * @param modifiers
 	 * @return
 	 */
 	protected boolean hasModifier(int modifier, List<Modifier> modifiers) {
 		for(Modifier m : modifiers) {
 			if(m instanceof Modifier.Base) {
 				Modifier.Base b = (Modifier.Base) m;
 				if(b.modifier() == modifier) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 	
 	/**
 	 * This method simply determines the super class of the given class.
 	 * 
 	 * @param c
 	 * @return
 	 */
 	protected Type.Clazz getSuperClass(Type.Clazz c) throws ClassNotFoundException {
 		jkit.jil.Clazz cc = loader.loadClass(c);
 		return cc.superClass();
 	}
 	
 	/**
      * This method is just to factor out the code for looking up the source
      * location and throwing an exception based on that.
      * 
      * @param msg --- the error message
      * @param e --- the syntactic element causing the error
      */
 	protected void syntax_error(String msg, SyntacticElement e) {
 		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
 		throw new SyntaxError(msg,loc.line(),loc.column());
 	}
 	
 	/**
 	 * This method is just to factor out the code for looking up the source
 	 * location and throwing an exception based on that. In this case, we also
 	 * have an internal exception which has given rise to this particular
 	 * problem.
 	 * 
 	 * @param msg
 	 *            --- the error message
 	 * @param e
 	 *            --- the syntactic element causing the error
 	 * @parem ex --- an internal exception, the details of which we want to
 	 *        keep.
 	 */
 	protected void syntax_error(String msg, SyntacticElement e, Throwable ex) {
 		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
 		throw new SyntaxError(msg,loc.line(),loc.column(),ex);
 	}
 	
 	/**
 	 * The purpose of this method is to compute an import declaration from a
 	 * given class Type. For example, consider the type "mypkg.MyClass". From
 	 * this, we compute an import declaration "import mypkg.MyClass.*". This
 	 * simplifies the problem of resolving an internal class, since we can use
 	 * the existing method for looking up classes in the ClassLoader.
 	 * 
 	 * @param type
 	 * @return
 	 */
 	protected String computeImportDecl(Type.Clazz type) {
 		String decl = type.pkg();
 		if(!decl.equals("")) { decl = decl + "."; }
 		for(Pair<String,List<Type.Reference>> p : type.components()) {
 			decl = decl + p.first() + ".";
 		}
 		return decl + "*";
 	}
 }
