 /* Copyright (C) 2011 SpringSource
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.grails.datastore.gorm.query.transform;
 
 import grails.gorm.DetachedCriteria;
 import grails.persistence.Entity;
 import grails.util.GrailsNameUtils;
 import org.codehaus.groovy.ast.*;
 import org.codehaus.groovy.ast.expr.*;
 import org.codehaus.groovy.ast.stmt.*;
 import org.codehaus.groovy.control.SourceUnit;
 import org.codehaus.groovy.control.messages.LocatedMessage;
 import org.codehaus.groovy.grails.commons.GrailsClassUtils;
 import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
 import org.codehaus.groovy.syntax.Token;
 import org.grails.datastore.mapping.query.criteria.FunctionCallingCriterion;
 import org.grails.datastore.mapping.query.Query;
 
 import java.io.File;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.net.MalformedURLException;
 import java.util.*;
 
 
 /**
  * ClassCodeVisitorSupport that transforms where methods into detached criteria queries
  *
  * @author Graeme Rocher
  * @since 1.0
  */
 public class DetachedCriteriaTransformer extends ClassCodeVisitorSupport {
 
     private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = {};
     private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
     public static final String AND_OPERATOR = "&";
     public static final String OR_OPERATOR = "|";
     public static final ClassNode DETACHED_CRITERIA_CLASS_NODE = new ClassNode(DetachedCriteria.class);
     public static final HashSet<String> CANDIDATE_METHODS_WHERE_ONLY = new HashSet<String>() {{
         add("where");
     }};
     public static final ClassNode FUNCTION_CALL_CRITERION = new ClassNode(FunctionCallingCriterion.class);
     public static final String EQUALS_OPERATOR = "==";
     public static final String IS_NULL_CRITERION = "isNull";
 
     private SourceUnit sourceUnit;
     private static final Set<String> CANDIDATE_METHODS = new HashSet<String>() {{
         add("where");
         add("findAll");
         add("find");
     }};
 
     private static final Set<String> SUPPORTED_FUNCTIONS = new HashSet<String>() {{
         add("lower");
         add("upper");
         add("trim");
         add("length");
         add("second");
         add("hour");
         add("minute");
         add("day");
         add("month");
         add("year");
 
 
     }};
 
     private static final Map<String, String> OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
         put("==", "eq");
         put("!=", "ne");
         put(">", "gt");
         put("<", "lt");
         put(">=", "ge");
         put("<=", "le");
         put("==~", "like");
         put("=~", "ilike");
         put("in", "inList");
 
     }};
     private static final Map<String, ClassNode> OPERATOR_TO_CRITERION_METHOD_MAP = new HashMap<String, ClassNode>() {{
         put("==", new ClassNode(Query.Equals.class));
         put("!=", new ClassNode(Query.NotEquals.class));
         put(">", new ClassNode(Query.GreaterThan.class));
         put("<", new ClassNode(Query.LessThan.class));
         put(">=", new ClassNode(Query.GreaterThanEquals.class));
         put("<=", new ClassNode(Query.LessThanEquals.class));
         put("==~", new ClassNode(Query.Like.class));
         put("=~", new ClassNode(Query.ILike.class));
         put("in", new ClassNode(Query.In.class));
 
     }};
     private static final Map<String, String> PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
         put("==", "eqProperty");
         put("!=", "neProperty");
         put(">", "gtProperty");
         put("<", "ltProperty");
         put(">=", "geProperty");
         put("<=", "leProperty");
 
     }};
     private static final Map<String, String> SIZE_OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
         put("==", "sizeEq");
         put("!=", "sizeNe");
         put(">", "sizeGt");
         put("<", "sizeLt");
         put(">=", "sizeGe");
         put("<=", "sizeLe");
 
     }};
     private static final Map<String, String> AGGREGATE_FUNCTIONS = new HashMap<String, String>() {{
         put("avg", "avg");
         put("max", "max");
         put("min", "min");
         put("sum", "sum");
         put("property", "property");
         put("count", "countDistinct");
     }};
 
 
     private Map<String, ClassNode> detachedCriteriaVariables = new HashMap<String, ClassNode>();
     private Map<String, ClassNode> staticDetachedCriteriaVariables = new HashMap<String, ClassNode>();
     private ClassNode currentClassNode;
 
     DetachedCriteriaTransformer(SourceUnit sourceUnit) {
         this.sourceUnit = sourceUnit;
     }
 
 
     @Override
     public void visitClass(ClassNode node) {
         try {
             this.currentClassNode = node;
             super.visitClass(node);
         } catch(Exception e){
             StringWriter stringWriter = new StringWriter();
             e.printStackTrace(new PrintWriter(stringWriter));
             sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred apply query transformations: " + e.getMessage(), Token.newString(node.getName(),node.getLineNumber(), node.getColumnNumber()), sourceUnit));
         } finally {
             currentClassNode = null;
             detachedCriteriaVariables.clear();
         }
     }
 
     @Override
     public void visitMethod(MethodNode node) {
         try {
             super.visitMethod(node);
         } finally {
             detachedCriteriaVariables.clear();
         }
     }
 
     @Override
     public void visitField(FieldNode node) {
         ClassNode classNode = node.getOwner();
         if(node.isStatic() && isDomainClass(classNode)) {
             Expression initialExpression = node.getInitialExpression();
             if(initialExpression instanceof MethodCallExpression) {
                 MethodCallExpression mce = (MethodCallExpression) initialExpression;
 
                 if(isCandidateWhereMethod(mce.getMethod(), mce.getArguments())) {
                     ArgumentListExpression args = (ArgumentListExpression) mce.getArguments();
                     List<Expression> argsExpressions = args.getExpressions();
                     int totalExpressions = argsExpressions.size();
                     if(totalExpressions > 0) {
                         Expression expression = argsExpressions.get(totalExpressions - 1);
                         if(expression instanceof ClosureExpression) {
                             ClosureExpression closureExpression = (ClosureExpression) expression;
                             transformClosureExpression(classNode, closureExpression);
 
 
                             MethodCallExpression newInitialExpression = new MethodCallExpression(new ConstructorCallExpression(DETACHED_CRITERIA_CLASS_NODE, new ArgumentListExpression(new ClassExpression(classNode))), "build", new ArgumentListExpression(closureExpression));
                             node.setInitialValueExpression(newInitialExpression);
                             node.setType(DETACHED_CRITERIA_CLASS_NODE);
                             staticDetachedCriteriaVariables.put(node.getName(), classNode);
                         }
                     }
                 }
             }
         }
         else {
             try {
                 Expression initialExpression = node.getInitialExpression();
                 ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initialExpression);
 
                 if (newClosureExpression != null) {
                     node.setInitialValueExpression(newClosureExpression);
                 }
             } catch (Exception e) {
                 sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred apply query transformations: " + e.getMessage(), Token.newString(node.getName(), node.getLineNumber(), node.getColumnNumber()), sourceUnit));
             }
         }
 
         super.visitField(node);
     }
 
 
 
     @Override
     public void visitDeclarationExpression(DeclarationExpression expression) {
         Expression initializationExpression = expression.getRightExpression();
         if(initializationExpression instanceof MethodCallExpression) {
             MethodCallExpression call = (MethodCallExpression) initializationExpression;
             Expression objectExpression = call.getObjectExpression();
             Expression method = call.getMethod();
             Expression arguments = call.getArguments();
             if(isCandidateMethod(method.getText(), arguments, CANDIDATE_METHODS_WHERE_ONLY)) {
                 ClassNode classNode = new ClassNode(DetachedCriteria.class);
                 ClassNode targetType = objectExpression.getType();
                 if(isDomainClass(targetType)) {
                     classNode.setGenericsTypes(new GenericsType[]{new GenericsType(targetType)});
 
                     expression.getVariableExpression().setType(classNode);
                     String variableName = expression.getVariableExpression().getName();
                     expression.setLeftExpression(new VariableExpression(variableName, classNode));
                     detachedCriteriaVariables.put(variableName, targetType);
                 }
             }
 
         }
         else if(initializationExpression instanceof ConstructorCallExpression) {
             String variableName = expression.getVariableExpression().getName();
             ConstructorCallExpression cce = (ConstructorCallExpression) initializationExpression;
 
             ClassNode type = cce.getType();
             if(DETACHED_CRITERIA_CLASS_NODE.getName().equals(type.getName())) {
                 Expression arguments = cce.getArguments();
                 if(arguments instanceof ArgumentListExpression) {
                     ArgumentListExpression ale = (ArgumentListExpression) arguments;
                     if(ale.getExpressions().size() == 1) {
                         Expression exp = ale.getExpression(0);
                         if(exp instanceof ClassExpression) {
                             ClassExpression clse = (ClassExpression) exp;
                             detachedCriteriaVariables.put(variableName, clse.getType());
                         }
                     }
                 }
             }
         }
         else {
             try {
                 ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initializationExpression);
                 if (newClosureExpression != null) {
                     expression.setRightExpression(newClosureExpression);
                 }
             } catch (Exception e) {
                 sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred apply query transformations: " + e.getMessage(), Token.newString(initializationExpression.getText(), initializationExpression.getLineNumber(), initializationExpression.getColumnNumber()), sourceUnit));
             }
         }
         super.visitDeclarationExpression(expression);
     }
 
     private ClosureExpression handleDetachedCriteriaCast(Expression initializationExpression) {
         ClosureExpression newClosureExpression = null;
         if ((initializationExpression instanceof CastExpression) && ((CastExpression) initializationExpression).getExpression() instanceof ClosureExpression) {
             CastExpression ce = (CastExpression) initializationExpression;
             Expression castTarget = ce.getExpression();
             ClosureExpression cle = (ClosureExpression) castTarget;
             ClassNode targetCastType = ce.getType();
             if (targetCastType.getName().equals(DetachedCriteria.class.getName())) {
                 GenericsType[] genericsTypes = targetCastType.getGenericsTypes();
                 if (genericsTypes.length > 0) {
                     ClassNode genericType = genericsTypes[0].getType();
                     transformClosureExpression(genericType, cle);
                     newClosureExpression = cle;
                 }
             }
         }
         return newClosureExpression;
     }
 
     @Override
     public void visitMethodCallExpression(MethodCallExpression call) {
         Expression objectExpression = call.getObjectExpression();
         Expression method = call.getMethod();
         Expression arguments = call.getArguments();
         try {
             if (isCandidateMethodCallForTransform(objectExpression, method, arguments)) {
                 ClassExpression ce = (ClassExpression) objectExpression;
                 ClassNode classNode = ce.getType();
                 this.currentClassNode = classNode;
                 visitMethodCall(classNode, (ArgumentListExpression) arguments);
             }
             else if(objectExpression instanceof VariableExpression) {
                 VariableExpression var = (VariableExpression) objectExpression;
                 String varName = var.getName();
 
                 ClassNode varType = detachedCriteriaVariables.get(varName);
                 if(varType != null && isCandidateWhereMethod(method, arguments)) {
                     this.currentClassNode = varType;
                     visitMethodCall(varType, (ArgumentListExpression) arguments);
                 }
             }
             else if(objectExpression instanceof PropertyExpression) {
                 PropertyExpression pe = (PropertyExpression) objectExpression;
                 String propName = pe.getPropertyAsString();
                 ClassNode classNode = pe.getObjectExpression().getType();
                 if(isDomainClass(classNode)) {
                     ClassNode propertyType = getPropertyType(classNode, propName);
                     if(propertyType != null && DETACHED_CRITERIA_CLASS_NODE.equals(propertyType)) {
                         visitMethodCall(classNode, (ArgumentListExpression) arguments);
                     }
                 }
             }
         } catch (Exception e) {
             sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred apply query transformations: " + e.getMessage(), Token.newString(call.getMethodAsString(), call.getLineNumber(), call.getColumnNumber()), sourceUnit));
         }
         super.visitMethodCallExpression(call);
     }
 
     private boolean isCandidateMethodCallForTransform(Expression objectExpression, Expression method, Expression arguments) {
         return (objectExpression instanceof ClassExpression) && isCandidateWhereMethod(method, arguments);
     }
 
     private void visitMethodCall(ClassNode classNode, ArgumentListExpression arguments) {
         if (isDomainClass(classNode)) {
             visitMethodCallOnDetachedCriteria(classNode, arguments);
         }
     }
 
     private void visitMethodCallOnDetachedCriteria(ClassNode classNode, ArgumentListExpression arguments) {
         if (arguments.getExpressions().size() > 0) {
             Expression expression = arguments.getExpression(arguments.getExpressions().size()-1);
             if (expression instanceof ClosureExpression) {
                 ClosureExpression closureExpression = (ClosureExpression) expression;
                 transformClosureExpression(classNode, closureExpression);
 
             }
         }
     }
 
     private boolean isCandidateWhereMethod(Expression method, Expression arguments) {
         String methodName = method.getText();
         return ((method instanceof ConstantExpression) && isCandidateWhereMethod(methodName, arguments));
     }
 
     private boolean isCandidateWhereMethod(String methodName, Expression arguments) {
         return isCandidateMethod(methodName, arguments, CANDIDATE_METHODS);
     }
 
     private boolean isCandidateMethod(String methodName, Expression arguments, Set<String> candidateMethods) {
         return (candidateMethods.contains(methodName)) && (arguments instanceof ArgumentListExpression);
     }
 
     @Override
     public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
         String method = call.getMethod();
         Expression arguments = call.getArguments();
         if(isCandidateWhereMethod(method,arguments)) {
             ClassNode classNode = call.getOwnerType();
             visitMethodCall(classNode, (ArgumentListExpression) arguments);
         }
         super.visitStaticMethodCallExpression(call);
     }
 
     protected void transformClosureExpression(ClassNode classNode, ClosureExpression closureExpression) {
         List<String> propertyNames = getPropertyNames(classNode);
         Statement code = closureExpression.getCode();
         BlockStatement newCode = new BlockStatement();
         boolean addAll = false;
 
         if (code instanceof BlockStatement) {
             BlockStatement bs = (BlockStatement) code;
 
             addBlockStatementToNewQuery(bs, newCode, addAll, propertyNames);
         }
 
         closureExpression.setCode(newCode);
     }
 
     private List<String> getPropertyNames(ClassNode classNode) {
         List<MethodNode> methods = classNode.getMethods();
         List<String> propertyNames = new ArrayList<String>();
         for (MethodNode method : methods) {
             if (!method.isAbstract() && !method.isStatic() && isGetter(method.getName(), method)) {
                 propertyNames.add(GrailsClassUtils.getPropertyForGetter(method.getName()));
             }
         }
         List<PropertyNode> properties = classNode.getProperties();
         for (PropertyNode property : properties) {
             propertyNames.add(property.getName());
         }
         return propertyNames;
     }
 
     private void addBlockStatementToNewQuery(BlockStatement blockStatement, BlockStatement newCode, boolean addAll, List<String> propertyNames) {
         List<Statement> statements = blockStatement.getStatements();
         for (Statement statement : statements) {
             addStatementToNewQuery(statement, newCode, addAll, propertyNames);
         }
     }
 
     private void addStatementToNewQuery(Statement statement, BlockStatement newCode, boolean addAll, List<String> propertyNames) {
         if(statement instanceof BlockStatement) {
              addBlockStatementToNewQuery((BlockStatement)statement, newCode, addAll, propertyNames);
         }
         else if (statement instanceof ExpressionStatement) {
             ExpressionStatement es = (ExpressionStatement) statement;
 
             Expression expression = es.getExpression();
             if(expression instanceof DeclarationExpression) {
                 newCode.addStatement(es);
             }
             else if (expression instanceof BinaryExpression) {
                 BinaryExpression be = (BinaryExpression) expression;
                 addBinaryExpressionToNewBody(propertyNames, newCode, be, addAll);
             } else if (expression instanceof NotExpression) {
                 NotExpression not = (NotExpression) expression;
 
                 handleNegation(propertyNames, newCode, not);
             } else if(expression instanceof MethodCallExpression) {
                 MethodCallExpression methodCall = (MethodCallExpression) expression;
 
                 handleAssociationMethodCallExpression(newCode, methodCall, propertyNames);
             }
         }
         else {
             if(statement instanceof IfStatement) {
                 IfStatement ifs = (IfStatement) statement;
                 Statement ifb = ifs.getIfBlock();
                 BlockStatement newIfBlock = new BlockStatement();
                 addStatementToNewQuery(ifb, newIfBlock, addAll, propertyNames);
                 ifs.setIfBlock(flattenStatementIfNecessary(newIfBlock));
 
                 Statement elseBlock = ifs.getElseBlock();
                 if(elseBlock != null) {
                     BlockStatement newElseBlock = new BlockStatement();
                     addStatementToNewQuery(elseBlock, newElseBlock, addAll, propertyNames);
                     ifs.setElseBlock(flattenStatementIfNecessary(newElseBlock));
                 }
                 newCode.addStatement(ifs);
             }
             else if(statement instanceof SwitchStatement) {
                 SwitchStatement sw = (SwitchStatement) statement;
 
 
                 List<CaseStatement> caseStatements = sw.getCaseStatements();
                 for (CaseStatement caseStatement : caseStatements) {
                     Statement existingCode = caseStatement.getCode();
                     BlockStatement newCaseCode = new BlockStatement();
                     addStatementToNewQuery(existingCode, newCaseCode, addAll, propertyNames);
                     caseStatement.setCode(flattenStatementIfNecessary(newCaseCode));
                 }
 
                 newCode.addStatement(sw);
             }
             else if(statement instanceof ForStatement) {
                 ForStatement fs = (ForStatement) statement;
                 Statement loopBlock = fs.getLoopBlock();
                 BlockStatement newLoopBlock = new BlockStatement();
                 addStatementToNewQuery(loopBlock, newLoopBlock, addAll, propertyNames);
                 fs.setLoopBlock(flattenStatementIfNecessary(newLoopBlock));
                 newCode.addStatement(fs);
             }
             else if(statement instanceof WhileStatement) {
                 WhileStatement ws = (WhileStatement) statement;
                 Statement loopBlock = ws.getLoopBlock();
                 BlockStatement newLoopBlock = new BlockStatement();
                 addStatementToNewQuery(loopBlock, newLoopBlock, addAll, propertyNames);
                 ws.setLoopBlock(flattenStatementIfNecessary(newLoopBlock));
                 newCode.addStatement(ws);
             }
             else if(statement instanceof TryCatchStatement) {
                 TryCatchStatement tcs = (TryCatchStatement) statement;
                 Statement tryStatement = tcs.getTryStatement();
 
                 BlockStatement newTryStatement = new BlockStatement();
                 addStatementToNewQuery(tryStatement, newTryStatement, addAll, propertyNames);
                 tcs.setTryStatement(flattenStatementIfNecessary(newTryStatement));
 
                 List<CatchStatement> catchStatements = tcs.getCatchStatements();
 
                 for (CatchStatement catchStatement : catchStatements) {
                     BlockStatement newCatchStatement = new BlockStatement();
                     Statement code = catchStatement.getCode();
                     addStatementToNewQuery(code, newCatchStatement, addAll, propertyNames);
                     catchStatement.setCode(flattenStatementIfNecessary(newCatchStatement));
                 }
 
                 Statement finallyStatement = tcs.getFinallyStatement();
                 if(finallyStatement != null) {
                     BlockStatement newFinallyStatement = new BlockStatement();
                     addStatementToNewQuery(finallyStatement, newFinallyStatement, addAll, propertyNames);
                     tcs.setFinallyStatement(flattenStatementIfNecessary(newFinallyStatement));
                 }
                 newCode.addStatement(tcs);
             }
             else {
                 newCode.addStatement(statement);
             }
         }
 
     }
 
     private Statement flattenStatementIfNecessary(BlockStatement blockStatement) {
         if (blockStatement.getStatements().size() == 1) {
             return blockStatement.getStatements().get(0);
         } else {
             return blockStatement;
         }
     }
 
     private void handleAssociationMethodCallExpression(BlockStatement newCode, MethodCallExpression methodCall, List<String> propertyNames) {
         Expression method = methodCall.getMethod();
         String methodName = method.getText();
         ArgumentListExpression arguments = methodCall.getArguments() instanceof ArgumentListExpression ? (ArgumentListExpression) methodCall.getArguments() : null;
 
         if(isAssociationMethodCall(propertyNames, methodName, arguments)) {
             ClosureAndArguments closureAndArguments = new ClosureAndArguments();
             ClosureExpression associationQuery = (ClosureExpression) arguments.getExpression(0);
             BlockStatement currentBody = closureAndArguments.getCurrentBody();
             ArgumentListExpression argList = closureAndArguments.getArguments();
             newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodName, argList)));
             Statement associationCode = associationQuery.getCode();
             if(associationCode instanceof BlockStatement) {
 
                 ClassNode type = getPropertyType(methodName);
                 List<String> associationPropertyNames = getPropertyNamesForAssociation(type);
 
                 ClassNode existing = currentClassNode;
                 try {
                     if(!associationPropertyNames.isEmpty() && !isDomainClass(type))
                         type = getAssociationTypeFromGenerics(type);
 
                     currentClassNode = type;
                     addBlockStatementToNewQuery((BlockStatement) associationCode, currentBody, associationPropertyNames == null, associationPropertyNames != null ? associationPropertyNames : Collections.<String>emptyList());
                 } finally {
                     currentClassNode = existing;
                 }
             }
         }
 //        else {
 //            sourceUnit.getErrorCollector().addError(new LocatedMessage("Method call ["+methodName+"] is invalid. Only binary expressions are allowed in queries.", Token.newString(methodName,methodCall.getLineNumber(), methodCall.getColumnNumber()), sourceUnit));
 //        }
     }
 
     private List<String> getPropertyNamesForAssociation(ClassNode type) {
         List<String> associationPropertyNames = null;
         if(type != null) {
             if(isDomainClass(type)) {
                 associationPropertyNames = getPropertyNames(type);
             }
             else {
                 ClassNode associationType = getAssociationTypeFromGenerics(type);
                 if(associationType != null)
                     associationPropertyNames = getPropertyNames(associationType);
             }
         }
         return associationPropertyNames;
     }
 
     private ClassNode getAssociationTypeFromGenerics(ClassNode type) {
         GenericsType[] genericsTypes = type.getGenericsTypes();
         ClassNode associationType = null;
         if(genericsTypes != null && genericsTypes.length == 1) {
             GenericsType genericType = genericsTypes[0];
             associationType = genericType.getType();
         }
         return associationType;
     }
 
     private ClassNode getPropertyType(String prop) {
         ClassNode classNode = this.currentClassNode;
         return getPropertyType(classNode, prop);
     }
 
     private ClassNode getPropertyType(ClassNode classNode, String prop) {
         ClassNode type = null;
         PropertyNode property = classNode.getProperty(prop);
         if(property != null) {
             type = property.getType();
         } else {
             MethodNode methodNode = currentClassNode.getMethod(GrailsNameUtils.getGetterName(prop), new Parameter[0]);
             if (methodNode != null) {
                 type = methodNode.getReturnType();
             }
         }
         return type;
     }
 
     private boolean isAssociationMethodCall(List<String> propertyNames, String methodName, ArgumentListExpression arguments) {
         return propertyNames.contains(methodName) && arguments != null && arguments.getExpressions().size()  == 1 && (arguments.getExpression(0) instanceof ClosureExpression);
     }
 
     private void handleNegation(List<String> propertyNames, BlockStatement newCode, NotExpression not) {
         Expression subExpression = not.getExpression();
         if (subExpression instanceof BinaryExpression) {
             ArgumentListExpression arguments = new ArgumentListExpression();
             BlockStatement currentBody = new BlockStatement();
             ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
             newClosureExpression.setVariableScope(new VariableScope());
             arguments.addExpression(newClosureExpression);
             addBinaryExpressionToNewBody(propertyNames, currentBody, (BinaryExpression) subExpression, false);
 
             newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "not", arguments)));
         }
         else {
             sourceUnit.getErrorCollector().addError(new LocatedMessage("You can only negate a binary expressions in queries.", Token.newString(not.getText(),not.getLineNumber(), not.getColumnNumber()), sourceUnit));
         }
     }
 
     private void addBinaryExpressionToNewBody(List<String> propertyNames, BlockStatement newCode, BinaryExpression be, boolean addAll) {
         Token operation = be.getOperation();
 
         String operator = operation.getRootText();
 
         Expression leftExpression = be.getLeftExpression();
         Expression rightExpression = be.getRightExpression();
         if (leftExpression instanceof VariableExpression) {
             VariableExpression leftVariable = (VariableExpression) leftExpression;
             String propertyName = leftVariable.getText();
             if (propertyNames.contains(propertyName) || addAll) {
                 if (OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                     addCriteriaCallMethodExpression(newCode, operator, rightExpression, propertyName, propertyNames, addAll);
                 }
                 else {
                     sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in query", operation, sourceUnit));
                 }
             }
             else {
                 sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query on property \""+propertyName+"\" - no such property on class "+currentClassNode.getName()+" exists.", Token.newString(propertyName,leftExpression.getLineNumber(), leftExpression.getColumnNumber()), sourceUnit));
             }
         }   else  {
 
             if(leftExpression instanceof MethodCallExpression) {
                 MethodCallExpression mce = (MethodCallExpression) leftExpression;
                 String methodAsString = mce.getMethodAsString();
                 Expression objectExpression = mce.getObjectExpression();
                 if("size".equals(methodAsString) && (objectExpression instanceof VariableExpression)) {
                     String propertyName = objectExpression.getText();
                     if(propertyNames.contains(propertyName)) {
                         String sizeOperator = SIZE_OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
                         if(sizeOperator != null) {
                               addCriteriaCall(newCode,operator,rightExpression, propertyName, propertyNames, addAll, sizeOperator);
                         }
                         else {
                             sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in size() query", operation, sourceUnit));
                         }
                     }
                     else {
                         sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query size of property \""+propertyName+"\" - no such property on class "+currentClassNode.getName()+" exists.", Token.newString(propertyName,leftExpression.getLineNumber(), leftExpression.getColumnNumber()), sourceUnit));
                     }
 
                     return;
                 }
                 else {
                     boolean isThis = "this".equals(objectExpression.getText());
                     Expression arguments = mce.getArguments();
                     boolean hasOneArg = arguments instanceof ArgumentListExpression ? ((ArgumentListExpression)arguments).getExpressions().size() == 1 : false;
                     if(isThis && hasOneArg && SUPPORTED_FUNCTIONS.contains(methodAsString)) {
 
                         String functionName = methodAsString;
                         ArgumentListExpression existingArgs = (ArgumentListExpression) arguments;
                         Expression propertyNameExpression = existingArgs.getExpression(0);
                         if(propertyNameExpression instanceof PropertyExpression) {
                            handleAssociationQueryViaPropertyExpression((PropertyExpression) propertyNameExpression, rightExpression,operator,newCode,propertyNames,functionName);
                         }
                         else {
 
                             handleFunctionCall(newCode, operator, rightExpression, functionName, propertyNameExpression);
                         }
                         return;
                     }
                 }
 
             }
             String methodNameToCall = null;
             if (operator.contains(AND_OPERATOR)) {
                 methodNameToCall = "and";
             } else if (operator.contains(OR_OPERATOR)) {
                 methodNameToCall = "or";
             }
             ArgumentListExpression arguments = new ArgumentListExpression();
             BlockStatement currentBody = new BlockStatement();
             handleBinaryExpressionSide(leftExpression,rightExpression, operator, currentBody, addAll, propertyNames);
             handleBinaryExpressionSide(rightExpression, rightExpression, operator,currentBody, addAll, propertyNames);
 
             ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
             newClosureExpression.setVariableScope(new VariableScope());
             arguments.addExpression(newClosureExpression);
             if (methodNameToCall != null) {
                 newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodNameToCall, arguments)));
             }
             else {
                 List<Statement> statements = currentBody.getStatements();
                 for (Statement statement : statements) {
                     newCode.addStatement(statement);
                 }
             }
 
         }
     }
 
     private void handleFunctionCall(BlockStatement newCode,String operator, Expression rightExpression, String functionName, Expression propertyNameExpression) {
         ArgumentListExpression newArgs = new ArgumentListExpression();
         ArgumentListExpression constructorArgs = new ArgumentListExpression();
         constructorArgs.addExpression(new ConstantExpression(functionName));
         ClassNode criterionClassNode = OPERATOR_TO_CRITERION_METHOD_MAP.get(operator);
         if(criterionClassNode != null) {
             ArgumentListExpression criterionConstructorArguments = new ArgumentListExpression();
             if(!(propertyNameExpression instanceof ConstantExpression)) {
                 propertyNameExpression = new ConstantExpression(propertyNameExpression.getText());
             }
             criterionConstructorArguments.addExpression(propertyNameExpression);
             criterionConstructorArguments.addExpression(rightExpression);
             constructorArgs.addExpression(new ConstructorCallExpression(criterionClassNode, criterionConstructorArguments));
             ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(FUNCTION_CALL_CRITERION, constructorArgs);
             newArgs.addExpression(constructorCallExpression );
             newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "add", newArgs)));
         }
         else {
             sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used with function call ["+functionName+"] in query", Token.newString(functionName,rightExpression.getLineNumber(), rightExpression.getColumnNumber()), sourceUnit));
         }
     }
 
     private void handleBinaryExpressionSide(Expression expressionSide, Expression oppositeSide, String operator, BlockStatement newCode, boolean addAll, List<String> propertyNames) {
         if (expressionSide instanceof BinaryExpression) {
             addBinaryExpressionToNewBody(propertyNames, newCode, (BinaryExpression) expressionSide, addAll);
         } else if(expressionSide instanceof NotExpression) {
             handleNegation(propertyNames, newCode, (NotExpression) expressionSide);
         }
         else if(expressionSide instanceof MethodCallExpression) {
             MethodCallExpression methodCallExpression = (MethodCallExpression) expressionSide;
             handleAssociationMethodCallExpression(newCode, methodCallExpression, propertyNames);
         }
         else if (expressionSide instanceof PropertyExpression) {
             PropertyExpression pe = (PropertyExpression) expressionSide;
             handleAssociationQueryViaPropertyExpression(pe, oppositeSide, operator, newCode, propertyNames, null);
         }
         else {
             // TODO: compilation error?
         }
     }
 
     private void handleAssociationQueryViaPropertyExpression(PropertyExpression pe, Expression oppositeSide, String operator, BlockStatement newCode, List<String> propertyNames, String functionName) {
         Expression objectExpression = pe.getObjectExpression();
         if (objectExpression instanceof VariableExpression) {
             String propertyName = objectExpression.getText();
             if (propertyNames.contains(propertyName)) {
                 String associationProperty = pe.getPropertyAsString();
 
                 ClassNode type = getPropertyType(propertyName);
                List<String> associationPropertyNames = getPropertyNamesForAssociation(type);
                 if(associationPropertyNames == null) {
                     associationPropertyNames = new ArrayList();
                 }
 
 
                 ClosureAndArguments closureAndArguments = new ClosureAndArguments();
                 BlockStatement currentBody = closureAndArguments.getCurrentBody();
                 ArgumentListExpression arguments = closureAndArguments.getArguments();
 
                 boolean hasNoProperties = associationPropertyNames.isEmpty();
                 if(!hasNoProperties && !associationPropertyNames.contains(associationProperty)) {
                      sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query property \""+associationProperty+"\" - no such property on class "+type.getName()+" exists.", Token.newString(propertyName, pe.getLineNumber(), pe.getColumnNumber()), sourceUnit));
                 }
                 ClassNode existing = this.currentClassNode;
                 try {
 
                     this.currentClassNode = type;
                     if(functionName != null) {
                         handleFunctionCall(currentBody, operator, oppositeSide, functionName, new ConstantExpression(associationProperty));
                     }
                     else {
                         addCriteriaCallMethodExpression(currentBody, operator, oppositeSide, associationProperty, associationPropertyNames, hasNoProperties);
                     }
                 } finally {
                     this.currentClassNode = existing;
                 }
                 newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, propertyName, arguments)));
             }
             else {
                 sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query property \""+propertyName+"\" - no such property on class "+this.currentClassNode.getName()+" exists.", Token.newString(propertyName, pe.getLineNumber(), pe.getColumnNumber()), sourceUnit));
             }
         }
 
     }
 
     private void addCriteriaCallMethodExpression(BlockStatement newCode, String operator, Expression rightExpression, String propertyName, List<String> propertyNames, boolean addAll) {
         String methodToCall = OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
         addCriteriaCall(newCode, operator, rightExpression, propertyName, propertyNames, addAll, methodToCall);
     }
 
     private void addCriteriaCall(BlockStatement newCode, String operator, Expression rightExpression, String propertyName, List<String> propertyNames, boolean addAll, String methodToCall) {
         if (rightExpression instanceof VariableExpression) {
             String rightPropertyName = rightExpression.getText();
             if ((propertyNames.contains(rightPropertyName) || addAll) && PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                 methodToCall = PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
                 rightExpression = new ConstantExpression(rightPropertyName);
             }
         } else if(rightExpression instanceof MethodCallExpression) {
             // potential aggregation
             MethodCallExpression aggregateMethodCall = (MethodCallExpression) rightExpression;
             String methodName = aggregateMethodCall.getMethodAsString();
             String functionName = AGGREGATE_FUNCTIONS.get(methodName);
             if("of".equals(methodName) && aggregateMethodCall.getObjectExpression() instanceof MethodCallExpression) {
                 ArgumentListExpression arguments = (ArgumentListExpression)aggregateMethodCall.getArguments();
                 if(arguments.getExpressions().size() == 1 && arguments.getExpression(0) instanceof ClosureExpression) {
                     ClosureExpression ce = (ClosureExpression) arguments.getExpression(0);
                     transformClosureExpression(this.currentClassNode,ce);
                     aggregateMethodCall = (MethodCallExpression) aggregateMethodCall.getObjectExpression();
 
                     functionName = AGGREGATE_FUNCTIONS.get(aggregateMethodCall.getMethodAsString());
                     ArgumentListExpression aggregateMethodCallArguments = (ArgumentListExpression)aggregateMethodCall.getArguments();
                     if(functionName != null && aggregateMethodCallArguments.getExpressions().size() == 1) {
                         Expression expression = aggregateMethodCallArguments.getExpression(0);
                         String aggregatePropertyName = null;
                         if(expression instanceof VariableExpression || expression instanceof ConstantExpression) {
                             aggregatePropertyName = expression.getText();
                         }
 
                         boolean validProperty = aggregatePropertyName != null && propertyNames.contains(aggregatePropertyName);
                         if(validProperty) {
                             BlockStatement bs = (BlockStatement) ce.getCode();
                             addProjectionToCurrentBody(bs, functionName, aggregatePropertyName);
                             rightExpression = new MethodCallExpression(new ConstructorCallExpression(DETACHED_CRITERIA_CLASS_NODE, new ArgumentListExpression(new ClassExpression(this.currentClassNode))), "build", new ArgumentListExpression(ce));
                         }
                     }
 
                 }
 
             }
             else if(functionName != null) {
                 Expression arguments = aggregateMethodCall.getArguments();
                 if(arguments instanceof ArgumentListExpression) {
                     ArgumentListExpression argList = (ArgumentListExpression) arguments;
                     List<Expression> expressions = argList.getExpressions();
                     int argCount = expressions.size();
                     if(argCount == 1) {
                         Expression expression = expressions.get(0);
                         String aggregatePropertyName = null;
                         if(expression instanceof VariableExpression || expression instanceof ConstantExpression) {
                             aggregatePropertyName = expression.getText();
                         }
 
                         boolean validProperty = aggregatePropertyName != null && propertyNames.contains(aggregatePropertyName);
                         if(validProperty) {
                                 ClosureAndArguments closureAndArguments = new ClosureAndArguments();
                                 BlockStatement currentBody = closureAndArguments.getCurrentBody();
 
                                 addProjectionToCurrentBody(currentBody, functionName, aggregatePropertyName);
 
                                 rightExpression = closureAndArguments.getClosureExpression();
 
                                 if("property".equals(functionName)) {
                                     methodToCall = methodToCall + "All";
                                 }
                         }
                         else {
                             sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot use aggregate function "+functionName+" on property \""+aggregatePropertyName+"\" - no such property on class "+this.currentClassNode.getName()+" exists.", Token.newString(propertyName,aggregateMethodCall.getLineNumber(), aggregateMethodCall.getColumnNumber()), sourceUnit));
                         }
                     }
                 }
             }
         } else {
             if ("like".equals(methodToCall) && rightExpression instanceof BitwiseNegationExpression) {
                 methodToCall = "rlike";
                 BitwiseNegationExpression bne = (BitwiseNegationExpression) rightExpression;
                 rightExpression = bne.getExpression();
             }
             else if("inList".equals(methodToCall) && rightExpression instanceof RangeExpression) {
                 methodToCall = "between";
                 RangeExpression re = (RangeExpression) rightExpression;
                 ArgumentListExpression betweenArgs = new ArgumentListExpression();
                 betweenArgs.addExpression(new ConstantExpression(propertyName))
                             .addExpression(re.getFrom())
                             .addExpression(re.getTo());
                 rightExpression = betweenArgs;
             }
         }
         ArgumentListExpression arguments;
 
         if(rightExpression instanceof ArgumentListExpression) {
             arguments = (ArgumentListExpression) rightExpression;
         }
         else if(rightExpression instanceof ConstantExpression) {
             ConstantExpression constant = (ConstantExpression) rightExpression;
             if(constant.getValue() == null) {
                 boolean singleArg = false;
                 if(operator.equals(EQUALS_OPERATOR)) {
                     singleArg = true;
                     methodToCall = IS_NULL_CRITERION;
                 }
                 else if(operator.equals("!=")) {
                     singleArg = true;
                     methodToCall = "isNotNull";
                 }
 
                 arguments = new ArgumentListExpression();
                 arguments.addExpression(new ConstantExpression(propertyName));
                 if(!singleArg) {
                     arguments.addExpression(rightExpression);
                 }
             }
             else {
                 arguments = new ArgumentListExpression();
                 arguments.addExpression(new ConstantExpression(propertyName))
                         .addExpression(rightExpression);
 
             }
         }
         else {
             arguments = new ArgumentListExpression();
             arguments.addExpression(new ConstantExpression(propertyName))
                     .addExpression(rightExpression);
         }
         newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodToCall, arguments)));
     }
 
     private void addProjectionToCurrentBody(BlockStatement currentBody, String functionName, String aggregatePropertyName) {
         ClosureAndArguments projectionsBody = new ClosureAndArguments();
         ArgumentListExpression aggregateArgs = new ArgumentListExpression();
         aggregateArgs.addExpression(new ConstantExpression(aggregatePropertyName));
         projectionsBody.getCurrentBody().addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, functionName, aggregateArgs)));
         currentBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "projections", projectionsBody.getArguments())));
     }
 
     protected boolean isDomainClass(ClassNode classNode) {
         String filePath = classNode.getModule() != null ? classNode.getModule().getDescription() : null;
         if(filePath != null) {
             try {
                 if(GrailsResourceUtils.isDomainClass(new File(filePath).toURI().toURL())) {
                     return true;
                 }
             } catch (MalformedURLException e) {
                 // ignore
             }
         }
         List<AnnotationNode> annotations = classNode.getAnnotations();
         if (annotations != null && !annotations.isEmpty()) {
             for (AnnotationNode annotation : annotations) {
                 String className = annotation.getClassNode().getName();
                 if (Entity.class.getName().equals(className)) {
                     return true;
                 }
             }
         }
         return false;
     }
 
     @Override
     protected SourceUnit getSourceUnit() {
         return this.sourceUnit;
     }
 
     private boolean isGetter(String methodName, MethodNode declaredMethod) {
         return declaredMethod.getParameters().length == 0 && GrailsClassUtils.isGetter(methodName, EMPTY_JAVA_CLASS_ARRAY);
     }
 
     private class ClosureAndArguments {
         private BlockStatement currentBody;
         private ArgumentListExpression arguments;
         private ClosureExpression closureExpression;
 
         private ClosureAndArguments() {
             build();
         }
 
         public BlockStatement getCurrentBody() {
             return currentBody;
         }
 
         public ArgumentListExpression getArguments() {
             return arguments;
         }
 
         private ClosureAndArguments build() {
             currentBody = new BlockStatement();
             closureExpression = new ClosureExpression(new Parameter[0], currentBody);
             closureExpression.setVariableScope(new VariableScope());
             closureExpression.setCode(currentBody);
 
             arguments = new ArgumentListExpression();
             arguments.addExpression(closureExpression);
             return this;
         }
 
         public ClosureExpression getClosureExpression() {
             return closureExpression;
         }
     }
 }
