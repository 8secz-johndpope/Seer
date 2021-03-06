 /*
  * Copyright 2011 JBoss, a divison Red Hat, Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.jboss.errai.ioc.rebind.ioc.codegen.builder.impl;
 
 import javax.enterprise.util.TypeLiteral;
 
 import org.jboss.errai.ioc.rebind.ioc.codegen.BooleanExpression;
 import org.jboss.errai.ioc.rebind.ioc.codegen.Context;
 import org.jboss.errai.ioc.rebind.ioc.codegen.Statement;
 import org.jboss.errai.ioc.rebind.ioc.codegen.Variable;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.ArrayInitializationBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.CaseBlockBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.CatchBlockBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.ContextualStatementBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.ElseBlockBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.StatementBegin;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.StatementEnd;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.VariableReferenceContextualStatementBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.WhileBuilder;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.BranchCallElement;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DeclareVariable;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DynamicLoad;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DefineLabel;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.LoadClassReference;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.LoadField;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.LoadLiteral;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.LoadVariable;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.MethodCall;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.ResetCallElement;
 import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.ThrowException;
 import org.jboss.errai.ioc.rebind.ioc.codegen.control.branch.BreakStatement;
 import org.jboss.errai.ioc.rebind.ioc.codegen.control.branch.ContinueStatement;
 
 /**
  * The root of our fluent StatementBuilder API.
 * 
  * @author Christian Sadilek <csadilek@redhat.com>
  * @author Mike Brock <cbrock@redhat.com>
  */
 public class StatementBuilder extends AbstractStatementBuilder implements StatementBegin {
 
   public StatementBuilder(Context context) {
     super(context);
 
     if (context != null) {
       for (Variable v : context.getDeclaredVariables()) {
         appendCallElement(new DeclareVariable(v));
       }
       appendCallElement(new ResetCallElement());
     }
   }
 
   public static StatementBegin create() {
     return new StatementBuilder(null);
   }
 
   public static StatementBegin create(Context context) {
     return new StatementBuilder(context);
   }
 
   @Override
   public StatementBuilder addVariable(String name, Class<?> type) {
     Variable v = Variable.create(name, type);
     return addVariable(v);
   }
 
   @Override
   public StatementBuilder addVariable(String name, TypeLiteral<?> type) {
     Variable v = Variable.create(name, type);
     return addVariable(v);
   }
 
   @Override
   public StatementBuilder addVariable(String name, Object initialization) {
     Variable v = Variable.create(name, initialization);
     return addVariable(v);
   }
 
   @Override
   public StatementBuilder addVariable(String name, Class<?> type, Object initialization) {
     Variable v = Variable.create(name, type, initialization);
     return addVariable(v);
   }
 
   @Override
   public StatementBuilder addVariable(String name, TypeLiteral<?> type, Object initialization) {
     Variable v = Variable.create(name, type, initialization);
     return addVariable(v);
   }
 
   private StatementBuilder addVariable(Variable v) {
     appendCallElement(new DeclareVariable(v));
     return this;
   }
 
   @Override
   public VariableReferenceContextualStatementBuilder loadVariable(String name, Object... indexes) {
     if (name.matches("(this.)(.)*"))
       return loadClassMember(name.replaceFirst("(this.)", ""), indexes);
    
     appendCallElement(new LoadVariable(name, indexes));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
 
   @Override
   public VariableReferenceContextualStatementBuilder loadClassMember(String name, Object... indexes) {
     appendCallElement(new LoadVariable(name, true, indexes));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
  
   @Override
   public ContextualStatementBuilder loadLiteral(Object o) {
     appendCallElement(new LoadLiteral(o));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
 
   @Override
   public ContextualStatementBuilder load(Object o) {
     appendCallElement(new DynamicLoad(o));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
 
   @Override
   public ContextualStatementBuilder invokeStatic(Class<?> clazz, String methodName, Object... parameters) {
     appendCallElement(new LoadClassReference(clazz));
     appendCallElement(new MethodCall(methodName, parameters, true));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
 
   @Override
   public ContextualStatementBuilder loadStatic(Class<?> clazz, String fieldName) {
     appendCallElement(new LoadClassReference(clazz));
     appendCallElement(new LoadField(fieldName));
     return new ContextualStatementBuilderImpl(context, callElementBuilder);
   }
 
   @Override
   public ObjectBuilder newObject(Class<?> type) {
     return ObjectBuilder.newInstanceOf(type, context);
   }
 
   @Override
   public ObjectBuilder newObject(TypeLiteral<?> type) {
     return ObjectBuilder.newInstanceOf(type, context);
   }
 
   @Override
   public ArrayInitializationBuilder newArray(Class<?> componentType) {
     return new ArrayBuilderImpl(context, callElementBuilder).newArray(componentType);
   }
 
   @Override
   public ArrayInitializationBuilder newArray(Class<?> componentType, Integer... dimensions) {
     return new ArrayBuilderImpl(context, callElementBuilder).newArray(componentType, dimensions);
   }
 
   @Override
   public BlockBuilderImpl<WhileBuilder> do_() {
     return new LoopBuilderImpl(context, callElementBuilder).do_();
   }
 
   @Override
   public BlockBuilderImpl<ElseBlockBuilder> if_(BooleanExpression stmt) {
     return new IfBlockBuilderImpl(context, callElementBuilder).if_(stmt);
   }
 
   @Override
   public BlockBuilderImpl<StatementEnd> while_(BooleanExpression stmt) {
     return new LoopBuilderImpl(context, callElementBuilder).while_(stmt);
   }
 
   @Override
   public BlockBuilderImpl<StatementEnd> for_(BooleanExpression condition) {
     return new LoopBuilderImpl(context, callElementBuilder).for_(condition);
   }
 
   @Override
   public BlockBuilderImpl<StatementEnd> for_(Statement initializer, BooleanExpression condition) {
     return new LoopBuilderImpl(context, callElementBuilder).for_(initializer, condition);
   }
 
   @Override
   public BlockBuilderImpl<StatementEnd> for_(Statement initializer, BooleanExpression condition,
      Statement countingExpression) {
     return new LoopBuilderImpl(context, callElementBuilder).for_(initializer, condition, countingExpression);
   }
 
   @Override
   public CaseBlockBuilder switch_(Statement statement) {
     return new SwitchBlockBuilderImpl(context, callElementBuilder).switch_(statement);
   }
  
   @Override
   public BlockBuilderImpl<CatchBlockBuilder> try_() {
     return new TryBlockBuilderImpl(context, callElementBuilder).try_();
   }
 
   @Override
   public StatementEnd throw_(Class<? extends Throwable> throwableType, Object... parameters) {
     appendCallElement(new ThrowException(throwableType, parameters));
     return this;
   }
 
   @Override
   public StatementEnd throw_(String exceptionVarName) {
     appendCallElement(new ThrowException(exceptionVarName));
     return this;
   }
 
   @Override
   public StatementEnd label(String label) {
     appendCallElement(new DefineLabel(label));
     return this;
   }
 
   @Override
   public StatementEnd break_() {
     appendCallElement(new BranchCallElement(new BreakStatement()));
     return this;
   }
 
   @Override
   public StatementEnd break_(String label) {
     appendCallElement(new BranchCallElement(new BreakStatement(label)));
     return this;
   }
 
   @Override
   public StatementEnd continue_() {
     appendCallElement(new BranchCallElement(new ContinueStatement()));
     return this;
   }
 
   @Override
   public StatementEnd continue_(String label) {
     appendCallElement(new BranchCallElement(new ContinueStatement(label)));
     return this;
   }
 }
