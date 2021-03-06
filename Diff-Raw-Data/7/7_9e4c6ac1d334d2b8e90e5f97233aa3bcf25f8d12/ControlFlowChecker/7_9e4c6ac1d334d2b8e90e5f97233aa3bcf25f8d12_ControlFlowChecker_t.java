 package eu.bryants.anthony.toylanguage.compiler.passes;
 
 import java.math.BigInteger;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.Set;
 
 import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
 import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
 import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.ArrayAccessExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.ArrayCreationExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.Expression;
 import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
 import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
 import eu.bryants.anthony.toylanguage.ast.member.Constructor;
 import eu.bryants.anthony.toylanguage.ast.member.Field;
 import eu.bryants.anthony.toylanguage.ast.member.Member;
 import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.GlobalVariable;
 import eu.bryants.anthony.toylanguage.ast.metadata.MemberVariable;
 import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
 import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
 import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
 import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
 import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
 import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
 import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
 import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.Block;
 import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.BreakableStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.ForStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.ShorthandAssignStatement;
 import eu.bryants.anthony.toylanguage.ast.statement.Statement;
 import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
 import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
 import eu.bryants.anthony.toylanguage.ast.type.NamedType;
 import eu.bryants.anthony.toylanguage.ast.type.VoidType;
 import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
 
 /*
  * Created on 6 Apr 2012
  */
 
 /**
  * @author Anthony Bryant
  */
 public class ControlFlowChecker
 {
   /**
    * Checks that the control flow of the specified compilation unit is well defined.
    * @param compilationUnit - the CompilationUnit to check
    * @throws ConceptualException - if any control flow related errors are detected
    */
   public static void checkControlFlow(CompilationUnit compilationUnit) throws ConceptualException
   {
     for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
     {
       for (Constructor constructor : compoundDefinition.getConstructors())
       {
         Set<Variable> initialisedVariables = new HashSet<Variable>();
         for (Parameter p : constructor.getParameters())
         {
           initialisedVariables.add(p.getVariable());
         }
         checkControlFlow(constructor.getBlock(), initialisedVariables, new LinkedList<BreakableStatement>(), true, false);
         for (Field field : compoundDefinition.getNonStaticFields())
         {
           if (!initialisedVariables.contains(field.getMemberVariable()))
           {
             throw new ConceptualException("Constructor does not always initialise the non-static field: " + field.getName(), constructor.getLexicalPhrase());
           }
         }
       }
 
       for (Method method : compoundDefinition.getAllMethods())
       {
         Set<Variable> initialisedVariables = new HashSet<Variable>();
         for (Parameter p : method.getParameters())
         {
           initialisedVariables.add(p.getVariable());
         }
         boolean returned = checkControlFlow(method.getBlock(), initialisedVariables, new LinkedList<BreakableStatement>(), false, method.isStatic());
         if (!returned && !(method.getReturnType() instanceof VoidType))
         {
           throw new ConceptualException("Method does not always return a value", method.getLexicalPhrase());
         }
       }
     }
   }
 
   /**
    * Checks that the control flow of the specified statement is well defined.
    * @param statement - the statement to check
    * @param initialisedVariables - the set of variables which have definitely been initialised before the specified statement is executed - this will be added to with variables that are initialised in this statement
    * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement
    * @param inConstructor - true if the statement is part of a constructor call
    * @param inStaticContext - true if the statement is in a static context
    * @return true if the statement returns from its enclosing function or control cannot reach statements after it, false if control flow continues after it
    * @throws ConceptualException - if any unreachable code is detected
    */
   private static boolean checkControlFlow(Statement statement, Set<Variable> initialisedVariables, LinkedList<BreakableStatement> enclosingBreakableStack, boolean inConstructor, boolean inStaticContext) throws ConceptualException
   {
     if (statement instanceof AssignStatement)
     {
       AssignStatement assignStatement = (AssignStatement) statement;
       Assignee[] assignees = assignStatement.getAssignees();
       Set<Variable> nowInitialisedVariables = new HashSet<Variable>();
       for (int i = 0; i < assignees.length; i++)
       {
         if (assignees[i] instanceof VariableAssignee)
         {
           // it hasn't been initialised unless there's an expression
           if (assignStatement.getExpression() != null)
           {
             nowInitialisedVariables.add(((VariableAssignee) assignees[i]).getResolvedVariable());
           }
         }
         else if (assignees[i] instanceof ArrayElementAssignee)
         {
           ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
           checkUninitialisedVariables(arrayElementAssignee.getArrayExpression(), initialisedVariables, inConstructor, inStaticContext);
           checkUninitialisedVariables(arrayElementAssignee.getDimensionExpression(), initialisedVariables, inConstructor, inStaticContext);
         }
         else if (assignees[i] instanceof FieldAssignee)
         {
           FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
           FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
 
           // if the field is being accessed on 'this' or '(this)' (to any number of brackets), then accept it as initialising that member variable
           Expression expression = fieldAccessExpression.getBaseExpression();
           while (expression != null && expression instanceof BracketedExpression && inConstructor)
           {
             expression = ((BracketedExpression) expression).getExpression();
           }
           // if we're in a constructor, only check the sub-expression for uninitialised variables if it doesn't just access 'this'
           // this allows the programmer to access fields before 'this' is fully initialised
           if (expression != null && expression instanceof ThisExpression && inConstructor)
           {
             Member resolvedMember = fieldAccessExpression.getResolvedMember();
             if (resolvedMember instanceof Field)
             {
               if (((Field) resolvedMember).isStatic())
               {
                 throw new IllegalStateException("Field Assignee on 'this' resolves to a static member: " + fieldAssignee);
               }
               nowInitialisedVariables.add(((Field) resolvedMember).getMemberVariable());
             }
           }
           else if (fieldAccessExpression.getBaseExpression() != null)
           {
             // otherwise (if we aren't in a constructor, or the base expression isn't 'this', but we do have a base expression) check the uninitialised variables normally
             checkUninitialisedVariables(fieldAccessExpression.getBaseExpression(), initialisedVariables, inConstructor, inStaticContext);
           }
           else
           {
             // otherwise, we do not have a base expression, so we must have a base type, so the field must be static
             // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check anything
           }
         }
         else if (assignees[i] instanceof BlankAssignee)
         {
           // do nothing, this assignee doesn't actually get assigned to
         }
         else
         {
           throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
         }
       }
       if (assignStatement.getExpression() != null)
       {
         checkUninitialisedVariables(assignStatement.getExpression(), initialisedVariables, inConstructor, inStaticContext);
       }
       for (Variable var : nowInitialisedVariables)
       {
         initialisedVariables.add(var);
       }
       return false;
     }
     else if (statement instanceof Block)
     {
       boolean returned = false;
       for (Statement s : ((Block) statement).getStatements())
       {
         if (returned)
         {
           throw new ConceptualException("Unreachable code", s.getLexicalPhrase());
         }
         returned = checkControlFlow(s, initialisedVariables, enclosingBreakableStack, inConstructor, inStaticContext);
       }
       return returned;
     }
     else if (statement instanceof BreakStatement)
     {
       if (enclosingBreakableStack.isEmpty())
       {
         throw new ConceptualException("Nothing to break out of", statement.getLexicalPhrase());
       }
       BreakStatement breakStatement = (BreakStatement) statement;
       IntegerLiteral stepsLiteral = breakStatement.getBreakSteps();
       int breakCount = 1;
       if (stepsLiteral != null)
       {
         BigInteger value = stepsLiteral.getValue();
         if (value.signum() < 1)
         {
           throw new ConceptualException("Cannot break out of less than one statement", breakStatement.getLexicalPhrase());
         }
         if (value.bitLength() > Integer.SIZE || value.intValue() > enclosingBreakableStack.size())
         {
           throw new ConceptualException("Cannot break out of more than " + enclosingBreakableStack.size() + " statement" + (enclosingBreakableStack.size() == 1 ? "" : "s") + " at this point", breakStatement.getLexicalPhrase());
         }
         breakCount = value.intValue();
       }
       BreakableStatement breakable = enclosingBreakableStack.get(breakCount - 1);
       breakStatement.setResolvedBreakable(breakable);
       breakable.setBrokenOutOf(true);
       return true;
     }
     else if (statement instanceof ContinueStatement)
     {
       if (enclosingBreakableStack.isEmpty())
       {
         throw new ConceptualException("Nothing to continue through", statement.getLexicalPhrase());
       }
       ContinueStatement continueStatement = (ContinueStatement) statement;
       IntegerLiteral stepsLiteral = continueStatement.getContinueSteps();
       int continueCount = 1;
       if (stepsLiteral != null)
       {
         BigInteger value = stepsLiteral.getValue();
         if (value.signum() < 1)
         {
           throw new ConceptualException("Cannot continue through less than one statement", continueStatement.getLexicalPhrase());
         }
         if (value.bitLength() > Integer.SIZE || value.intValue() > enclosingBreakableStack.size())
         {
           throw new ConceptualException("Cannot continue through more than " + enclosingBreakableStack.size() + " statement" + (enclosingBreakableStack.size() == 1 ? "" : "s") + " at this point", continueStatement.getLexicalPhrase());
         }
         continueCount = value.intValue();
       }
       BreakableStatement breakable = enclosingBreakableStack.get(continueCount - 1);
       continueStatement.setResolvedBreakable(breakable);
       breakable.setContinuedThrough(true);
       // TODO: when we get switch statements, make sure continue is forbidden for them
       return true;
     }
     else if (statement instanceof ExpressionStatement)
     {
       checkUninitialisedVariables(((ExpressionStatement) statement).getExpression(), initialisedVariables, inConstructor, inStaticContext);
       return false;
     }
     else if (statement instanceof ForStatement)
     {
       ForStatement forStatement = (ForStatement) statement;
       Statement init = forStatement.getInitStatement();
       Expression condition = forStatement.getConditional();
       Statement update = forStatement.getUpdateStatement();
       Block block = forStatement.getBlock();
 
       enclosingBreakableStack.push(forStatement);
 
       if (init != null)
       {
         // check the loop initialisation variable in the block outside the loop, because it may add new variables which have now been initialised
         boolean returned = checkControlFlow(init, initialisedVariables, enclosingBreakableStack, inConstructor, inStaticContext);
         if (returned)
         {
           throw new IllegalStateException("Reached a state where a for loop initialisation statement returned");
         }
       }
       HashSet<Variable> loopVariables = new HashSet<Variable>(initialisedVariables);
       if (condition != null)
       {
         checkUninitialisedVariables(condition, loopVariables, inConstructor, inStaticContext);
       }
       boolean returned = false;
       for (Statement s : block.getStatements())
       {
         if (returned)
         {
           throw new ConceptualException("Unreachable code", s.getLexicalPhrase());
         }
         returned = checkControlFlow(s, loopVariables, enclosingBreakableStack, inConstructor, inStaticContext);
       }
 
       if (update != null)
       {
         if (returned && !forStatement.isContinuedThrough())
         {
           throw new ConceptualException("Unreachable code", update.getLexicalPhrase());
         }
         boolean updateReturned = checkControlFlow(update, loopVariables, enclosingBreakableStack, inConstructor, inStaticContext);
         if (updateReturned)
         {
           throw new IllegalStateException("Reached a state where a for loop update statement returned");
         }
       }
 
       enclosingBreakableStack.pop();
 
       // if there is no conditional and the for statement is never broken out of, then control cannot continue after the end of the loop
       return condition == null && !forStatement.isBrokenOutOf();
     }
     else if (statement instanceof IfStatement)
     {
       IfStatement ifStatement = (IfStatement) statement;
       checkUninitialisedVariables(ifStatement.getExpression(), initialisedVariables, inConstructor, inStaticContext);
       Statement thenClause = ifStatement.getThenClause();
       Statement elseClause = ifStatement.getElseClause();
       if (elseClause == null)
       {
         Set<Variable> thenClauseVariables = new HashSet<Variable>(initialisedVariables);
         checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack, inConstructor, inStaticContext);
         return false;
       }
       Set<Variable> thenClauseVariables = new HashSet<Variable>(initialisedVariables);
       Set<Variable> elseClauseVariables = new HashSet<Variable>(initialisedVariables);
       boolean thenReturned = checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack, inConstructor, inStaticContext);
       boolean elseReturned = checkControlFlow(elseClause, elseClauseVariables, enclosingBreakableStack, inConstructor, inStaticContext);
       if (!thenReturned && !elseReturned)
       {
         for (Variable var : thenClauseVariables)
         {
           if (elseClauseVariables.contains(var))
           {
             initialisedVariables.add(var);
           }
         }
       }
       else if (!thenReturned && elseReturned)
       {
         initialisedVariables.addAll(thenClauseVariables);
       }
       else if (thenReturned && !elseReturned)
       {
         initialisedVariables.addAll(elseClauseVariables);
       }
       return thenReturned && elseReturned;
     }
     else if (statement instanceof PrefixIncDecStatement)
     {
       PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
       Assignee assignee = prefixIncDecStatement.getAssignee();
       if (assignee instanceof VariableAssignee)
       {
         Variable resolvedVariable = ((VariableAssignee) assignee).getResolvedVariable();
         if (!initialisedVariables.contains(resolvedVariable))
         {
           throw new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase());
         }
       }
       else if (assignee instanceof ArrayElementAssignee)
       {
         ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
         checkUninitialisedVariables(arrayElementAssignee.getArrayExpression(), initialisedVariables, inConstructor, inStaticContext);
         checkUninitialisedVariables(arrayElementAssignee.getDimensionExpression(), initialisedVariables, inConstructor, inStaticContext);
       }
       else
       {
         // ignore blank assignees, they shouldn't be able to get through variable resolution
         throw new IllegalStateException("Unknown Assignee type: " + assignee);
       }
       return false;
     }
     else if (statement instanceof ReturnStatement)
     {
       Expression returnedExpression = ((ReturnStatement) statement).getExpression();
       if (returnedExpression != null)
       {
         checkUninitialisedVariables(returnedExpression, initialisedVariables, inConstructor, inStaticContext);
       }
       return true;
     }
     else if (statement instanceof ShorthandAssignStatement)
     {
       ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
       for (Assignee assignee : shorthandAssignStatement.getAssignees())
       {
         if (assignee instanceof VariableAssignee)
         {
           VariableAssignee variableAssignee = (VariableAssignee) assignee;
           if (!initialisedVariables.contains(variableAssignee.getResolvedVariable()))
           {
             throw new ConceptualException("Variable '" + variableAssignee.getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase());
           }
         }
         else if (assignee instanceof ArrayElementAssignee)
         {
           ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
           checkUninitialisedVariables(arrayElementAssignee.getArrayExpression(), initialisedVariables, inConstructor, inStaticContext);
           checkUninitialisedVariables(arrayElementAssignee.getDimensionExpression(), initialisedVariables, inConstructor, inStaticContext);
         }
         else if (assignee instanceof FieldAssignee)
         {
           FieldAssignee fieldAssignee = (FieldAssignee) assignee;
           // treat this as a field access, and check for uninitialised variables as normal
           checkUninitialisedVariables(fieldAssignee.getFieldAccessExpression(), initialisedVariables, inConstructor, inStaticContext);
         }
         else if (assignee instanceof BlankAssignee)
         {
           // do nothing, this assignee doesn't actually get assigned to
         }
         else
         {
           throw new IllegalStateException("Unknown Assignee type: " + assignee);
         }
       }
       checkUninitialisedVariables(shorthandAssignStatement.getExpression(), initialisedVariables, inConstructor, inStaticContext);
       return false;
     }
     else if (statement instanceof WhileStatement)
     {
       WhileStatement whileStatement = (WhileStatement) statement;
       checkUninitialisedVariables(whileStatement.getExpression(), initialisedVariables, inConstructor, inStaticContext);
 
       Set<Variable> loopVariables = new HashSet<Variable>(initialisedVariables);
       // we don't care about the result of this, as the loop could execute zero times
       enclosingBreakableStack.push(whileStatement);
       checkControlFlow(whileStatement.getStatement(), loopVariables, enclosingBreakableStack, inConstructor, inStaticContext);
       enclosingBreakableStack.pop();
       return false;
     }
     throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
   }
 
   private static void checkUninitialisedVariables(Expression expression, Set<Variable> initialisedVariables, boolean inConstructor, boolean inStaticContext) throws ConceptualException
   {
     if (expression instanceof ArithmeticExpression)
     {
       ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
       checkUninitialisedVariables(arithmeticExpression.getLeftSubExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(arithmeticExpression.getRightSubExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof ArrayAccessExpression)
     {
       ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
       checkUninitialisedVariables(arrayAccessExpression.getArrayExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(arrayAccessExpression.getDimensionExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof ArrayCreationExpression)
     {
       ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
       if (creationExpression.getDimensionExpressions() != null)
       {
         for (Expression e : creationExpression.getDimensionExpressions())
         {
           checkUninitialisedVariables(e, initialisedVariables, inConstructor, inStaticContext);
         }
       }
       if (creationExpression.getValueExpressions() != null)
       {
         for (Expression e : creationExpression.getValueExpressions())
         {
           checkUninitialisedVariables(e, initialisedVariables, inConstructor, inStaticContext);
         }
       }
     }
     else if (expression instanceof BitwiseNotExpression)
     {
       checkUninitialisedVariables(((BitwiseNotExpression) expression).getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof BooleanLiteralExpression)
     {
       // do nothing
     }
     else if (expression instanceof BooleanNotExpression)
     {
       checkUninitialisedVariables(((BooleanNotExpression) expression).getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof BracketedExpression)
     {
       checkUninitialisedVariables(((BracketedExpression) expression).getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof CastExpression)
     {
       checkUninitialisedVariables(((CastExpression) expression).getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof ComparisonExpression)
     {
       ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
       checkUninitialisedVariables(comparisonExpression.getLeftSubExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(comparisonExpression.getRightSubExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof FieldAccessExpression)
     {
       FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
       Expression subExpression = ((FieldAccessExpression) expression).getBaseExpression();
 
       // if we're in a constructor, we only want to check the sub-expression for uninitialised variables if it doesn't just access 'this'
       // this allows the programmer to access certain fields before 'this' is fully initialised
       // if it isn't accessed on 'this', we don't care about the field access itself, as we assume that all fields on other objects are initialised when they are defined
       while (subExpression != null && subExpression instanceof BracketedExpression && inConstructor)
       {
         subExpression = ((BracketedExpression) subExpression).getExpression();
       }
       if (subExpression != null && subExpression instanceof ThisExpression && inConstructor)
       {
         Member resolvedMember = fieldAccessExpression.getResolvedMember();
        if (resolvedMember instanceof Field && !((Field) resolvedMember).isStatic() && !initialisedVariables.contains(((Field) resolvedMember).getMemberVariable()))
         {
           throw new ConceptualException("Field '" + ((Field) resolvedMember).getName() + "' may not have been initialised", fieldAccessExpression.getLexicalPhrase());
         }
       }
       else if (subExpression != null)
       {
         // otherwise (if we aren't in a constructor, or the base expression isn't 'this', but we do have a base expression) check the uninitialised variables normally
         checkUninitialisedVariables(((FieldAccessExpression) expression).getBaseExpression(), initialisedVariables, inConstructor, inStaticContext);
       }
       else
       {
         // otherwise, we do not have a base expression, so we must have a base type, so the field must be static
         // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check anything
       }
     }
     else if (expression instanceof FloatingLiteralExpression)
     {
       // do nothing
     }
     else if (expression instanceof FunctionCallExpression)
     {
       FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
       if (functionCallExpression.getResolvedMethod() != null)
       {
         Expression resolvedBaseExpression = functionCallExpression.getResolvedBaseExpression();
         if (inConstructor && resolvedBaseExpression == null && !functionCallExpression.getResolvedMethod().isStatic())
         {
           // we are in a constructor, and we are calling a non-static method without a base expression (i.e. on 'this')
           // this should only be allowed if 'this' is fully initialised
           CompoundDefinition compoundDefinition = functionCallExpression.getResolvedMethod().getContainingDefinition();
           for (Field field : compoundDefinition.getNonStaticFields())
           {
             if (!initialisedVariables.contains(field.getMemberVariable()))
             {
               throw new ConceptualException("Cannot call methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(compoundDefinition) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
             }
           }
         }
         else if (resolvedBaseExpression != null)
         {
           checkUninitialisedVariables(resolvedBaseExpression, initialisedVariables, inConstructor, inStaticContext);
         }
       }
       else if (functionCallExpression.getResolvedConstructor() != null)
       {
         // this is a constructor call, which we do not need to check anything else for
       }
       else if (functionCallExpression.getResolvedBaseExpression() != null)
       {
         checkUninitialisedVariables(functionCallExpression.getResolvedBaseExpression(), initialisedVariables, inConstructor, inStaticContext);
       }
       else
       {
         throw new IllegalStateException("Unresolved function call: " + functionCallExpression);
       }
       // check that the arguments are all initialised
       for (Expression e : functionCallExpression.getArguments())
       {
         checkUninitialisedVariables(e, initialisedVariables, inConstructor, inStaticContext);
       }
     }
     else if (expression instanceof InlineIfExpression)
     {
       InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
       checkUninitialisedVariables(inlineIfExpression.getCondition(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(inlineIfExpression.getThenExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(inlineIfExpression.getElseExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof IntegerLiteralExpression)
     {
       // do nothing
     }
     else if (expression instanceof LogicalExpression)
     {
       LogicalExpression logicalExpression = (LogicalExpression) expression;
       checkUninitialisedVariables(logicalExpression.getLeftSubExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(logicalExpression.getRightSubExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof MinusExpression)
     {
       checkUninitialisedVariables(((MinusExpression) expression).getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof ShiftExpression)
     {
       ShiftExpression shiftExpression = (ShiftExpression) expression;
       checkUninitialisedVariables(shiftExpression.getLeftExpression(), initialisedVariables, inConstructor, inStaticContext);
       checkUninitialisedVariables(shiftExpression.getRightExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof ThisExpression)
     {
       if (inStaticContext)
       {
         throw new ConceptualException("'this' does not refer to anything in this static context", expression.getLexicalPhrase());
       }
       if (inConstructor)
       {
         // the type has already been resolved by the resolver, so we can access it here
         NamedType type = (NamedType) expression.getType();
         CompoundDefinition compoundDefinition = type.getResolvedDefinition();
         for (Field field : compoundDefinition.getNonStaticFields())
         {
           if (!initialisedVariables.contains(field.getMemberVariable()))
           {
             throw new ConceptualException("Cannot use 'this' here. Not all of the non-static fields of this '" + type + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
           }
         }
       }
     }
     else if (expression instanceof TupleExpression)
     {
       TupleExpression tupleExpression = (TupleExpression) expression;
       Expression[] subExpressions = tupleExpression.getSubExpressions();
       for (int i = 0; i < subExpressions.length; i++)
       {
         checkUninitialisedVariables(subExpressions[i], initialisedVariables, inConstructor, inStaticContext);
       }
     }
     else if (expression instanceof TupleIndexExpression)
     {
       TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
       checkUninitialisedVariables(indexExpression.getExpression(), initialisedVariables, inConstructor, inStaticContext);
     }
     else if (expression instanceof VariableExpression)
     {
       VariableExpression variableExpression = (VariableExpression) expression;
       Variable var = variableExpression.getResolvedVariable();
      if (!(var instanceof GlobalVariable) && !initialisedVariables.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
       {
         throw new ConceptualException("Variable '" + variableExpression.getName() + "' may not have been initialised", variableExpression.getLexicalPhrase());
       }
       if (inStaticContext && var instanceof MemberVariable)
       {
         throw new ConceptualException("Non-static member variables do not exist in static methods.", expression.getLexicalPhrase());
       }
     }
     else
     {
       throw new ConceptualException("Internal control flow checking error: Unknown expression type", expression.getLexicalPhrase());
     }
   }
 }
