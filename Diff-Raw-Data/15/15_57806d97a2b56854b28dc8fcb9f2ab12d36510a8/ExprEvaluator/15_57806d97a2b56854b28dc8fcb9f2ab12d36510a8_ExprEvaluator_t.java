 package com.stuffwithstuff.magpie.interpreter;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import com.stuffwithstuff.magpie.ast.*;
 import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
 import com.stuffwithstuff.magpie.ast.pattern.Pattern;
 import com.stuffwithstuff.magpie.intrinsic.IntrinsicLoader;
 import com.stuffwithstuff.magpie.util.Pair;
 
 /**
  * Implements the visitor pattern on AST nodes, in order to evaluate
  * expressions. This is the heart of the interpreter and is where Magpie code is
  * actually executed.
  */
 public class ExprEvaluator implements ExprVisitor<Obj, Scope> {
   public ExprEvaluator(Context context) {
     mContext = context;
   }
 
   /**
    * Evaluates the given expression in the given context.
    * @param   expr     The expression to evaluate.
    * @param   context  The context in which to evaluate the expression.
    * @return           The result of evaluating the expression.
    */
   public Obj evaluate(Expr expr, Scope scope) {
     if (expr == null) return null;
     return expr.accept(this, scope);
   }
 
   @Override
   public Obj visit(ArrayExpr expr, Scope scope) {
     // Evaluate the elements.
     List<Obj> elements = new ArrayList<Obj>();
     for (Expr element : expr.getElements()) {
       elements.add(evaluate(element, scope));
     }
 
     return mContext.toArray(elements);
   }
 
   @Override
   public Obj visit(AssignExpr expr, Scope scope) {
     Obj value = evaluate(expr.getValue(), scope);
 
     // Try to assign to a local variable.
     if (scope.assign(expr.getName(), value)) return value;
     
     // TODO(bob): Detect this statically.
     throw mContext.error("NoVariableError",
         "Could not find a variable named \"" + expr.getName() + "\".");
   }
 
   @Override
   public Obj visit(BoolExpr expr, Scope scope) {
     return mContext.toObj(expr.getValue());
   }
 
   @Override
   public Obj visit(BreakExpr expr, Scope scope) {
     // Outside of a loop, "break" does nothing.
     if (mLoopDepth > 0) {
       throw new BreakException();
     }
     return mContext.nothing();
   }
   
   @Override
   public Obj visit(CallExpr expr, Scope scope) {
     Multimethod multimethod = scope.lookUpMultimethod(expr.getName());
     if (multimethod == null) {
       throw mContext.error(Name.NO_METHOD_ERROR,
          "Could not find a method named \"" + expr.getName() + "\". (" +
          expr.getPosition() + ")");
     }
 
     Obj arg = evaluate(expr.getArg(), scope);
     return multimethod.invoke(expr.getName(), mContext, arg);
   }
   
   @Override
   public Obj visit(ClassExpr expr, Scope scope) {
     // Look up the parents.
     List<ClassObj> parents = new ArrayList<ClassObj>();
     for (String parentName : expr.getParents()) {
       parents.add(scope.lookUp(parentName).asClass());
     }
     
     ClassObj classObj = mContext.getInterpreter().createClass(expr.getName(),
         parents, expr.getFields(), scope, expr.getDoc());
     
     scope.define(false, expr.getName(), classObj);
 
     return classObj;
   }
 
   @Override
   public Obj visit(FnExpr expr, Scope scope) {
     return mContext.toFunction(expr, scope);
   }
 
   @Override
   public Obj visit(ImportExpr expr, Scope scope) {
     // TODO(bob): Eventually the schemes should be host-provided plug-ins.
     if (expr.getScheme() == null) {
       Module module = mContext.getInterpreter().importModule(expr.getModule());
 
       // Map names to declarations.
       Map<String, ImportDeclaration> declarations =
           new HashMap<String, ImportDeclaration>();
       for (ImportDeclaration declaration : expr.getDeclarations()) {
         declarations.put(declaration.getName(), declaration);
       }
       
       Set<String> importedNames;
       
       if (expr.isOnly()) {
         importedNames = declarations.keySet();
       } else {
         importedNames = module.getExportedNames();
       }
 
       // Import the names.
       for (String name : importedNames) {
         ImportDeclaration declaration = declarations.get(name);
         
         String rename = name;
         
         // Rename it, if given one.
         if ((declaration != null) && (declaration.getRename() != null)) {
           rename = declaration.getRename();
         }
 
         // Apply the prefix, if given.
         if (expr.getPrefix() != null) {
           rename = expr.getPrefix() + "." + rename;
         }
 
         boolean export = false;
         if (declaration != null) {
           export = declaration.isExported();
         }
         
         scope.importName(name, rename, module, export);
       }
     } else if (expr.getScheme().equals("classfile")) {
       if (!IntrinsicLoader.loadClass(expr.getModule(), scope)) {
         // TODO(bob): Throw better error.
         throw mContext.error("Error", "Could not load classfile \"" +
             expr.getModule() + "\".");
       }
     }
     
     return mContext.nothing();
   }
 
   @Override
   public Obj visit(IntExpr expr, Scope scope) {
     return mContext.toObj(expr.getValue());
   }
   
   @Override
   public Obj visit(LoopExpr expr, Scope scope) {
     try {
       mLoopDepth++;
 
       // Loop forever. A "break" expression will throw a BreakException to
       // escape this loop.
       while (true) {
         // Evaluate the body in its own scope.
         evaluate(expr.getBody(), scope.push());
       }
     } catch (BreakException ex) {
       // Nothing to do.
     } finally {
       mLoopDepth--;
     }
 
     // TODO(bob): It would be cool if loops could have "else" clauses and then
     // reliably return a value.
     return mContext.nothing();
   }
 
   @Override
   public Obj visit(MatchExpr expr, Scope scope) {
     // Push a new context so that a variable declared in the value expression
     // itself disappears after the match, i.e.:
     // match var i = 123
     // ...
     // end
     // i should be gone here
     scope = scope.push();
     
     Obj value = evaluate(expr.getValue(), scope);
     
     // Try each pattern until we get a match.
     Obj result = evaluateCases(value, expr.getCases(), scope);
     if (result != null) return result;
     
     // If we got here, no patterns matched.
     throw mContext.error(Name.NO_MATCH_ERROR, "Could not find a match for \"" +
        mContext.getInterpreter().evaluateToString(value) + "\" (" +
        expr.getPosition() + ").");
   }
 
   @Override
   public Obj visit(MethodExpr expr, Scope scope) {
     if (expr.getBody() != null) {
       Function method = new Function(
           Expr.fn(expr.getPosition(), expr.getDoc(),
               expr.getPattern(), expr.getBody()),
           scope);
       
       scope.define(expr.getName(), method);
     } else {
       // Defining the multimethod here but not adding any methods.
       scope.defineMultimethod(expr.getName(), expr.getDoc());
     }
     
     return mContext.nothing();
   }
 
   @Override
   public Obj visit(NameExpr expr, Scope scope) {
     Obj variable = scope.lookUp(expr.getName());
     if (variable != null) return variable;
     
     // TODO(bob): Detect this statically.
     throw mContext.error(Name.NO_VARIABLE_ERROR,
        "Could not find a variable named \"" + expr.getName() + "\" (" +
        expr.getPosition() + ").");
   }
 
   @Override
   public Obj visit(NothingExpr expr, Scope scope) {
     return mContext.nothing();
   }
 
   @Override
   public Obj visit(QuoteExpr expr, Scope scope) {
     return JavaToMagpie.convertAndUnquote(mContext, expr.getBody(), scope);
   }
 
   @Override
   public Obj visit(RecordExpr expr, Scope scope) {
     
     // TODO(bob): Hack, keep track of order keys appear for better pretty-
     // printing.
     List<String> keys = new ArrayList<String>();
     
     // Evaluate the fields.
     Map<String, Obj> fields = new HashMap<String, Obj>();
     for (Pair<String, Expr> entry : expr.getFields()) {
       keys.add(entry.getKey());
       Obj value = evaluate(entry.getValue(), scope);
       fields.put(entry.getKey(), value);
     }
 
     return mContext.toObj(keys, fields);
   }
 
   @Override
   public Obj visit(ReturnExpr expr, Scope scope) {
     Obj value = evaluate(expr.getValue(), scope);
     throw new ReturnException(value);
   }
 
   @Override
   public Obj visit(ScopeExpr expr, Scope scope) {
     try {
       scope = scope.push();
       return evaluate(expr.getBody(), scope);
     } catch (ErrorException err) {
       // See if we can catch it here.
       Obj result = this.evaluateCases(err.getError(), expr.getCatches(), scope);
       if (result != null) return result;
 
       // Not caught here, so just keep unwinding.
       throw err;
     }
   }
 
   @Override
   public Obj visit(SequenceExpr expr, Scope scope) {
     // Evaluate all of the expressions and return the last.
     Obj result = null;
     for (Expr thisExpr : expr.getExpressions()) {
       result = evaluate(thisExpr, scope);
     }
 
     return result;
   }
 
   @Override
   public Obj visit(StringExpr expr, Scope scope) {
     return mContext.toObj(expr.getValue());
   }
 
   @Override
   public Obj visit(ThrowExpr expr, Scope scope) {
     Obj value = evaluate(expr.getValue(), scope);
     throw new ErrorException(value);
   }
 
   @Override
   public Obj visit(UnquoteExpr expr, Scope scope) {
     throw new UnsupportedOperationException(
         "An unquoted expression cannot be directly evaluated.");
   }
 
   @Override
   public Obj visit(VarExpr expr, Scope scope) {
     Obj value = evaluate(expr.getValue(), scope);
 
     if (!PatternTester.test(mContext, expr.getPattern(), value, scope)) {
       mContext.error(Name.NO_MATCH_ERROR, "The variable pattern \"" +
           expr.getPattern() + "\" does not match the initialized value \"" +
          mContext.getInterpreter().evaluateToString(value) + "\" (" +
          expr.getPosition() + ").");
     }
     
     PatternBinder.bind(mContext, expr.isMutable(), expr.getPattern(), value,
         scope);
     return value;
   }
 
   private Obj evaluateCases(Obj value, List<MatchCase> cases, Scope scope) {
     if (cases == null) return null;
     
     for (MatchCase matchCase : cases) {
       Pattern pattern = matchCase.getPattern();
       if (PatternTester.test(mContext, pattern, value, scope)) {
         // Matched. Bind variables and evaluate the body.
         scope = scope.push();
         PatternBinder.bind(mContext, false, pattern, value, scope);
         return evaluate(matchCase.getBody(), scope);
       }
     }
     
     return null;
   }
 
   private final Context mContext;
   private int mLoopDepth = 0;
 }
