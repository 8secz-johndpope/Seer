 /**
  * Copyright (c) 2012-2013 André Bargull
  * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
  *
  * <https://github.com/anba/es6draft>
  */
 package com.github.anba.es6draft.compiler;
 
 import org.objectweb.asm.Type;
 
 import com.github.anba.es6draft.ast.ArrayComprehension;
 import com.github.anba.es6draft.ast.Expression;
 import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
 import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
 
 /**
  * 11.1.4.2 Array Comprehension
  */
 class ArrayComprehensionGenerator extends ComprehensionGenerator {
     private static class Methods {
         // class: AbstractOperations
         static final MethodDesc AbstractOperations_CreateArrayFromList = MethodDesc.create(
                 MethodType.Static, Types.AbstractOperations, "CreateArrayFromList",
                 Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.List));
 
         // class: ArrayList
         static final MethodDesc ArrayList_init = MethodDesc.create(MethodType.Special,
                 Types.ArrayList, "<init>", Type.getMethodType(Type.VOID_TYPE));
 
         static final MethodDesc ArrayList_add = MethodDesc.create(MethodType.Virtual,
                 Types.ArrayList, "add", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
     }
 
     private int result = -1;
 
     ArrayComprehensionGenerator(CodeGenerator codegen) {
         super(codegen);
     }
 
     /**
      * 11.1.4.2 Array Comprehension
      * <p>
      * Runtime Semantics: Evaluation
      */
     @Override
     public Void visit(ArrayComprehension node, ExpressionVisitor mv) {
        assert result == -1 : "array-comprehension generator re-used";
 
         this.result = mv.newVariable(Types.ArrayList);
         mv.anew(Types.ArrayList);
         mv.dup();
         mv.invoke(Methods.ArrayList_init);
         mv.store(result, Types.ArrayList);
 
         node.getComprehension().accept(this, mv);
 
         mv.loadExecutionContext();
         mv.load(result, Types.ArrayList);
         mv.invoke(Methods.AbstractOperations_CreateArrayFromList);
         mv.freeVariable(result);
 
         return null;
     }
 
     /**
      * 11.1.4.2 Array Comprehension
      * <p>
      * Runtime Semantics: ComprehensionEvaluation
      * <p>
      * ComprehensionQualifierTail: AssignmentExpression
      */
     @Override
     protected Void visit(Expression node, ExpressionVisitor mv) {
         assert result != -1 : "array-comprehension generator not initialised";
 
         ValType type = expressionValue(node, mv);
         mv.toBoxed(type);
         mv.load(result, Types.ArrayList);
         mv.swap();
         mv.invoke(Methods.ArrayList_add);
         mv.pop();
 
         return null;
     }
 }
