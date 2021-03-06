 /*
 // $Id$
 // Farrago is an extensible data management system.
 // Copyright (C) 2002-2005 Disruptive Tech
 // Copyright (C) 2005-2005 The Eigenbase Project
 //
 // This program is free software; you can redistribute it and/or modify it
 // under the terms of the GNU General Public License as published by the Free
 // Software Foundation; either version 2 of the License, or (at your option)
 // any later version approved by The Eigenbase Project.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 package com.disruptivetech.farrago.calc;
 
 import java.math.*;
 
 import java.util.*;
 
 import org.eigenbase.reltype.*;
 import org.eigenbase.rex.*;
 import org.eigenbase.sql.*;
 import org.eigenbase.sql.fun.*;
 import org.eigenbase.sql.type.*;
 import org.eigenbase.util.*;
 import org.eigenbase.util14.*;
 
 
 /**
  * Implementation of {@link CalcRexImplementorTable}, containing implementations
  * for all standard functions.
  *
  * @author jhyde
  * @version $Id$
  * @since June 2nd, 2004
  */
 public class CalcRexImplementorTableImpl
     implements CalcRexImplementorTable
 {
 
     //~ Static fields/initializers ---------------------------------------------
 
     protected static final SqlStdOperatorTable opTab =
         SqlStdOperatorTable.instance();
     private static final CalcRexImplementorTableImpl std =
         new CalcRexImplementorTableImpl(null).initStandard();
 
     //~ Instance fields --------------------------------------------------------
 
     /**
      * Parent implementor table, may be null.
      */
     private final CalcRexImplementorTable parent;
 
     /**
      * Maps {@link SqlOperator} to {@link CalcRexImplementor}.
      */
     private final Map<SqlOperator,CalcRexImplementor> operatorImplementationMap =
         new HashMap<SqlOperator, CalcRexImplementor>();
 
     /**
      * Maps {@link SqlAggFunction} to {@link CalcRexAggImplementor}.
      */
     private final Map<SqlAggFunction,CalcRexAggImplementor> aggImplementationMap =
         new HashMap<SqlAggFunction, CalcRexAggImplementor>();
 
     //~ Constructors -----------------------------------------------------------
 
     /**
      * Creates an empty table which delegates to another table.
      *
      * @see {@link #std}
      */
     public CalcRexImplementorTableImpl(CalcRexImplementorTable parent)
     {
         this.parent = parent;
     }
 
     //~ Methods ----------------------------------------------------------------
 
     /**
      * Returns the table of implementations of all of the standard SQL functions
      * and operators.
      */
     public static CalcRexImplementorTable std()
     {
         return std;
     }
 
     /**
      * Registers an operator and its implementor.
      *
      * <p>It is an error if the operator already has an implementor. But if the
      * operator has an implementor in a parent table, it is simply overridden.
      *
      * @pre op != null
      * @pre impl != null
      * @pre !operatorImplementationMap.containsKey(op)
      */
     public void register(
         SqlOperator op,
         CalcRexImplementor impl)
     {
         Util.pre(op != null, "op != null");
         Util.pre(impl != null, "impl != null");
         Util.pre(!operatorImplementationMap.containsKey(op),
             "!operatorImplementationMap.containsKey(op)");
         operatorImplementationMap.put(op, impl);
     }
 
     /**
      * Registers an operator which is implemented in a trivial way by a single
      * calculator instruction.
      *
      * @pre op != null
      * @pre instrDef != null
      */
     protected void registerInstr(
         SqlOperator op,
         CalcProgramBuilder.InstructionDef instrDef)
     {
         Util.pre(instrDef != null, "instrDef != null");
         register(
             op,
             new InstrDefImplementor(instrDef));
     }
 
     /**
      * Registers an aggregate function and its implementor.
      *
      * <p>It is an error if the aggregate function already has an implementor.
      * But if the operator has an implementor in a parent table, it is simply
      * overridden.
      *
      * @pre op != null
      * @pre impl != null
      * @pre !operatorImplementationMap.containsKey(op)
      */
     public void registerAgg(
         SqlAggFunction agg,
         CalcRexAggImplementor impl)
     {
         Util.pre(agg != null, "agg != null");
         Util.pre(impl != null, "impl != null");
         Util.pre(!aggImplementationMap.containsKey(agg),
             "!aggImplementationMap.containsKey(op)");
         aggImplementationMap.put(agg, impl);
     }
 
     // implement CalcRexImplementorTable
     public CalcRexImplementor get(SqlOperator op)
     {
         CalcRexImplementor implementor =
             operatorImplementationMap.get(op);
         if ((implementor == null) && (parent != null)) {
             implementor = parent.get(op);
         }
         return implementor;
     }
 
     public CalcRexAggImplementor getAgg(SqlAggFunction op)
     {
         CalcRexAggImplementor implementor =
             aggImplementationMap.get(op);
         if ((implementor == null) && (parent != null)) {
             implementor = parent.getAgg(op);
         }
         return implementor;
     }
 
     // helper methods
 
     /**
      * Creates a register to hold the result of a call.
      *
      * @param translator Translator
      * @param call Call
      *
      * @return A register
      */
     protected static CalcReg createResultRegister(
         RexToCalcTranslator translator,
         RexCall call)
     {
         CalcProgramBuilder.RegisterDescriptor resultDesc =
             translator.getCalcRegisterDescriptor(call);
         return translator.builder.newLocal(resultDesc);
     }
 
     /**
      * Implements all operands to a call, and returns a list of the registers
      * which hold the results.
      *
      * @post return != null
      */
     protected static List<CalcReg> implementOperands(
         RexCall call,
         RexToCalcTranslator translator)
     {
         return implementOperands(call, 0, call.operands.length, translator);
     }
 
     /**
      * Implements all operands to a call between start (inclusive) and stop
      * (exclusive), and returns a list of the registers which hold the results.
      *
      * @post return != null
      */
     protected static List<CalcReg> implementOperands(
         RexCall call,
         int start,
         int stop,
         RexToCalcTranslator translator)
     {
         List<CalcReg> regList = new ArrayList<CalcReg>();
         for (int i = start; i < stop; i++) {
             RexNode operand = call.operands[i];
             CalcReg reg = translator.implementNode(operand);
             regList.add(reg);
         }
         return regList;
     }
 
     /**
      * Implements a call by invoking a given instruction.
      *
      * @param instr Instruction
      * @param translator Translator
      * @param call Call to translate
      *
      * @return Register which contains result, never null
      */
     private static CalcReg implementUsingInstr(
         CalcProgramBuilder.InstructionDef instr,
         RexToCalcTranslator translator,
         RexCall call)
     {
         CalcReg [] regs = new CalcReg[call.operands.length + 1];
 
         for (int i = 0; i < call.operands.length; i++) {
             RexNode operand = call.operands[i];
             regs[i + 1] = translator.implementNode(operand);
         }
 
         regs[0] = createResultRegister(translator, call);
 
         instr.add(translator.builder, regs);
         return regs[0];
     }
 
     /**
      * Converts a binary call (two regs as operands) by converting the first
      * operand to type {@link CalcProgramBuilder.OpType#Int8} for exact types
      * and {@link CalcProgramBuilder.OpType#Double} for approximate types and
      * then back again. Logically it will do something like<br>
      * t0 = type of first operand<br>
      * CAST(CALL(CAST(op0 as INT8), op1) as t0)<br>
      * If t0 is not a numeric or is already is INT8 or DOUBLE, the CALL is
      * simply returned as is.
      */
     private static RexCall implementFirstOperandWithDoubleOrInt8(
         RexCall call,
         RexToCalcTranslator translator,
         RexNode typeNode,
         int i,
         boolean castBack)
     {
         CalcProgramBuilder.RegisterDescriptor rd =
             translator.getCalcRegisterDescriptor(typeNode);
         if (rd.getType().isExact()) {
             return
                 implementFirstOperandWithInt8(
                     call,
                     translator,
                     typeNode,
                     i,
                     castBack);
         } else if (rd.getType().isApprox()) {
             return
                 implementFirstOperandWithDouble(
                     call,
                     translator,
                     typeNode,
                     i,
                     castBack);
         }
 
         return call;
     }
 
     /**
      * Converts a binary call (two regs as operands) by converting the first
      * operand to type {@link CalcProgramBuilder.OpType#Int8} if needed and then
      * back again. Logically it will do something like<br>
      * t0 = type of first operand<br>
      * CAST(CALL(CAST(op0 as INT8), op1) as t0)<br>
      * If t0 is not an exact type or is already is INT8, the CALL is simply
      * returned as is.
      */
     private static RexCall implementFirstOperandWithInt8(
         RexCall call,
         RexToCalcTranslator translator,
         RexNode typeNode,
         int i,
         boolean castBack)
     {
         CalcProgramBuilder.RegisterDescriptor rd =
             translator.getCalcRegisterDescriptor(typeNode);
         if (rd.getType().isExact()
             && !rd.getType().equals(CalcProgramBuilder.OpType.Int8)) {
             RelDataType oldType = typeNode.getType();
             RelDataTypeFactory fac = translator.rexBuilder.getTypeFactory();
 
             //todo do a reverse lookup on OpType.Int8 instead
             RelDataType int8 = fac.createSqlType(SqlTypeName.Bigint);
             RexNode castCall1 =
                 translator.rexBuilder.makeCast(int8, call.operands[i]);
 
             RexNode newCall;
             if (SqlStdOperatorTable.castFunc.equals(call.getOperator())) {
                 newCall =
                     translator.rexBuilder.makeCast(
                         call.getType(),
                         castCall1);
             } else {
                 RexNode [] args = new RexNode[call.operands.length];
                 System.arraycopy(call.operands,
                     0,
                     args,
                     0,
                     call.operands.length);
                 args[i] = castCall1;
                 newCall =
                     translator.rexBuilder.makeCall(
                         call.getOperator(),
                         args);
             }
 
             if (castBack) {
                 newCall = translator.rexBuilder.makeCast(oldType, newCall);
             }
             return (RexCall) newCall;
         }
         return call;
     }
 
     /**
      * Same as {@link #implementFirstOperandWithInt8} but with {@link
      * CalcProgramBuilder.OpType#Double} instead TODO need to abstract and merge
      * functionality with {@link #implementFirstOperandWithInt8} since they both
      * contain nearly the same code
      */
     private static RexCall implementFirstOperandWithDouble(
         RexCall call,
         RexToCalcTranslator translator,
         RexNode typeNode,
         int i,
         boolean castBack)
     {
         //TODO this method needs cleanup, it contains redundant code
         CalcProgramBuilder.RegisterDescriptor rd =
             translator.getCalcRegisterDescriptor(typeNode);
         if (rd.getType().isApprox()
             && !rd.getType().equals(CalcProgramBuilder.OpType.Double)) {
             RelDataType oldType = typeNode.getType();
             RelDataTypeFactory fac = translator.rexBuilder.getTypeFactory();
 
             //todo do a reverse lookup on OpType.Double instead
             RelDataType db = fac.createSqlType(SqlTypeName.Double);
             RexNode castCall1 =
                 translator.rexBuilder.makeCast(db, call.operands[i]);
 
             RexNode newCall;
             if (SqlStdOperatorTable.castFunc.equals(call.getOperator())) {
                 newCall =
                     translator.rexBuilder.makeCast(
                         call.getType(),
                         castCall1);
             } else {
                 RexNode [] args = new RexNode[call.operands.length];
                 System.arraycopy(call.operands,
                     0,
                     args,
                     0,
                     call.operands.length);
                 args[i] = castCall1;
                 newCall =
                     translator.rexBuilder.makeCall(
                         call.getOperator(),
                         args);
             }
 
             if (castBack) {
                 newCall = translator.rexBuilder.makeCast(oldType, newCall);
             }
             return (RexCall) newCall;
         }
         return call;
     }
 
     /**
      * Registers the standard set of functions.
      */
     private CalcRexImplementorTableImpl initStandard()
     {
         register(
             SqlStdOperatorTable.absFunc,
             new InstrDefImplementor(ExtInstructionDefTable.abs) {
                 public CalcReg implement(
                     RexCall call,
                     RexToCalcTranslator translator)
                 {
                     RexCall newCall =
                         implementFirstOperandWithDoubleOrInt8(call,
                             translator,
                             call.operands[0],
                             0,
                             true);
                     if (newCall.equals(call)) {
                         return super.implement(call, translator);
                     }
                     return translator.implementNode(newCall);
                 }
             });
 
         register(
             SqlStdOperatorTable.plusOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.nativeAdd));
 
         registerInstr(
             SqlStdOperatorTable.andOperator,
             CalcProgramBuilder.integralNativeAnd);
 
         register(
             SqlStdOperatorTable.divideOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.nativeDiv));
 
         register(
             SqlStdOperatorTable.caseOperator,
             new CaseImplementor());
 
         register(
             SqlStdOperatorTable.castFunc,
             CastImplementor.instance);
 
         if (false) {
             // TODO eventually need an extra argument for charset, in which
             // case we will use the following code
             register(
                 SqlStdOperatorTable.characterLengthFunc,
                 new AddCharSetNameInstrImplementor("CHAR_LENGTH", -1, 3));
         } else {
             registerInstr(
                 SqlStdOperatorTable.characterLengthFunc,
                 ExtInstructionDefTable.charLength);
         }
 
         // CHAR_LENGTH shares CHARACTER_LENGTH's implementation.
         // TODO: Combine the CHAR_LENGTH and CHARACTER_LENGTH at the
         //   RexNode level (they should remain separate functions at the
         //   SqlNode level).
         register(
             SqlStdOperatorTable.charLengthFunc,
             get(SqlStdOperatorTable.characterLengthFunc));
 
         register(
             SqlStdOperatorTable.concatOperator,
             new ConcatImplementor());
 
         register(
             SqlStdOperatorTable.equalsOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeEqual));
 
         register(
             SqlStdOperatorTable.greaterThanOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeGreaterThan));
 
         register(
             SqlStdOperatorTable.greaterThanOrEqualOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeGreaterOrEqualThan));
 
         registerInstr(
             SqlStdOperatorTable.isNullOperator,
             CalcProgramBuilder.boolNativeIsNull);
 
         registerInstr(
             SqlStdOperatorTable.isNotNullOperator,
             CalcProgramBuilder.boolNativeIsNotNull);
 
         register(
             SqlStdOperatorTable.isTrueOperator,
             new IsBoolImplementor(true));
 
         register(
             SqlStdOperatorTable.isNotTrueOperator,
             new IsNotBoolImplementor(true));
 
         register(
             SqlStdOperatorTable.isFalseOperator,
             new IsBoolImplementor(false));
 
         register(
             SqlStdOperatorTable.isNotFalseOperator,
             new IsNotBoolImplementor(false));
 
         register(
             SqlStdOperatorTable.lessThanOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeLessThan));
 
         register(
             SqlStdOperatorTable.lessThanOrEqualOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeLessOrEqualThan));
 
         registerInstr(
             SqlStdOperatorTable.likeOperator,
             ExtInstructionDefTable.like);
 
         register(
             SqlStdOperatorTable.lnFunc,
             new MakeOperandsDoubleImplementor(ExtInstructionDefTable.log));
 
         register(
             SqlStdOperatorTable.log10Func,
             new MakeOperandsDoubleImplementor(ExtInstructionDefTable.log10));
 
         // TODO: need to know charset as well. When ready,
         // use same construct as with CHAR_LENGTH above
         registerInstr(
             SqlStdOperatorTable.lowerFunc,
             ExtInstructionDefTable.lower);
 
         register(
             SqlStdOperatorTable.minusOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.nativeMinus));
 
         register(
             SqlStdOperatorTable.minusDateOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.nativeMinus));
 
         register(
             SqlStdOperatorTable.modFunc,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.integralNativeMod,
                 true));
 
         register(
             SqlStdOperatorTable.multiplyOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.integralNativeMul));
 
         register(
             SqlStdOperatorTable.notEqualsOperator,
             new BinaryNumericMakeSametypeImplementor(
                 CalcProgramBuilder.boolNativeNotEqual));
 
         // todo: optimization. If using 'NOT' in front of 'IS NULL',
         // create a call to the calc instruction 'ISNOTNULL'
         registerInstr(
             SqlStdOperatorTable.notOperator,
             CalcProgramBuilder.boolNot);
 
         registerInstr(
             SqlStdOperatorTable.orOperator,
             CalcProgramBuilder.boolOr);
 
         register(
             SqlStdOperatorTable.overlayFunc,
             new BinaryStringMakeSametypeImplementor(
                 ExtInstructionDefTable.overlay));
 
         register(
             SqlStdOperatorTable.positionFunc,
             new BinaryStringMakeSametypeImplementor(
                 ExtInstructionDefTable.position));
 
         register(
             SqlStdOperatorTable.powFunc,
             new MakeOperandsDoubleImplementor(ExtInstructionDefTable.pow));
 
         registerInstr(
             SqlStdOperatorTable.prefixMinusOperator,
             CalcProgramBuilder.nativeNeg);
 
         register(
             SqlStdOperatorTable.prefixPlusOperator,
             new IdentityImplementor());
 
         register(
             SqlStdOperatorTable.reinterpretOperator,
             new ReinterpretCastImplementor());
 
         registerInstr(
             SqlStdOperatorTable.similarOperator,
             ExtInstructionDefTable.similar);
 
         registerInstr(
             SqlStdOperatorTable.substringFunc,
             ExtInstructionDefTable.substring);
 
         registerInstr(
             SqlStdOperatorTable.throwOperator,
             CalcProgramBuilder.raise);
 
         register(
             SqlStdOperatorTable.trimFunc,
             new TrimImplementor());
 
         // TODO: need to know charset aswell. When ready,
         // use same construct as with CHAR_LENGTH above
         registerInstr(
             SqlStdOperatorTable.upperFunc,
             ExtInstructionDefTable.upper);
 
         register(
             SqlStdOperatorTable.localTimeFunc,
             new TimeFunctionImplementor());
 
         register(
             SqlStdOperatorTable.localTimestampFunc,
             new TimeFunctionImplementor());
 
         register(
             SqlStdOperatorTable.currentTimeFunc,
             new TimeFunctionImplementor());
 
         register(
             SqlStdOperatorTable.currentTimestampFunc,
             new TimeFunctionImplementor());
 
         register(
             SqlStdOperatorTable.sliceOp,
             new SliceImplementor());
 
         // Register agg functions.
         registerAgg(
             SqlStdOperatorTable.sumOperator,
             new SumCalcRexImplementor());
         registerAgg(
             SqlStdOperatorTable.countOperator,
             new CountCalcRexImplementor());
 
         // Register histogram and related functions required to make min and
         // max work over windows.
         registerAgg(
             SqlStdOperatorTable.histogramAggFunction,
             new HistogramAggRexImplementor());
         register(
             SqlStdOperatorTable.histogramMinFunction,
             new HistogramResultRexImplementor(
                 SqlStdOperatorTable.minOperator));
         register(
             SqlStdOperatorTable.histogramMaxFunction,
             new HistogramResultRexImplementor(
                 SqlStdOperatorTable.maxOperator));
         register(
             SqlStdOperatorTable.histogramFirstValueFunction,
             new HistogramResultRexImplementor(
                 SqlStdOperatorTable.firstValueOperator));
         register(
             SqlStdOperatorTable.histogramLastValueFunction,
             new HistogramResultRexImplementor(
                 SqlStdOperatorTable.lastValueOperator));
 
         return this;
     }
 
     //~ Inner Classes ----------------------------------------------------------
 
     /**
      * Abstract base class for classes which implement {@link
      * CalcRexImplementor}.
      */
     public abstract static class AbstractCalcRexImplementor
         implements CalcRexImplementor
     {
         public boolean canImplement(RexCall call)
         {
             if (RexUtil.requiresDecimalExpansion(call, true)) {
                 return false;
             }
             return true;
         }
     }
 
     /**
      * Generic implementor that takes a {@link
      * CalcProgramBuilder.InstructionDef} which implements an operator by
      * generating a call to a given instruction.
      *
      * <p>If you need to tweak the arguments to the instruction, you can
      * override {@link #makeRegList}.
      */
     public static class InstrDefImplementor
         extends AbstractCalcRexImplementor
     {
         /**
          * The instruction with which to implement this operator.
          */
         protected final CalcProgramBuilder.InstructionDef instr;
 
         /**
          * Creates an instruction implementor
          *
          * @param instr The instruction with which to implement this operator,
          * must not be null
          *
          * @pre null != instr
          * @pre instr != null
          */
         InstrDefImplementor(CalcProgramBuilder.InstructionDef instr)
         {
             Util.pre(null != instr, "null != instr");
             this.instr = instr;
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             List<CalcReg> regList = makeRegList(translator, call);
 
             instr.add(translator.builder, regList);
             return regList.get(0);
         }
 
         /**
          * Creates the list of registers which will be arguments to the
          * instruction call. i.e implment all the operands of the call and
          * create a result register for the call.
          *
          * <p>The 0th argument is assumed to hold the result of the call.
          */
         protected List<CalcReg> makeRegList(
             RexToCalcTranslator translator,
             RexCall call)
         {
             List<CalcReg> regList = implementOperands(call, translator);
 
             // the result is implemented last in order to avoid changing the
             // tests
             regList.add(
                 0,
                 createResultRegister(translator, call));
 
             return regList;
         }
     }
 
     /**
      * Implements "IS TRUE" and "IS FALSE" operators.
      */
     private static class IsBoolImplementor
         implements CalcRexImplementor
     {
         private boolean boolType;
         protected RexNode res;
 
         IsBoolImplementor(boolean boolType)
         {
             this.boolType = boolType;
         }
 
         public boolean canImplement(RexCall call)
         {
             return true;
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             RexNode operand = call.operands[0];
             translator.implementNode(operand);
 
             if (operand.getType().isNullable()) {
                 RexNode notNullCall =
                     translator.rexBuilder.makeCall(
                         SqlStdOperatorTable.isNotNullOperator, operand);
                 RexNode eqCall =
                     translator.rexBuilder.makeCall(
                         SqlStdOperatorTable.equalsOperator,
                         operand,
                         translator.rexBuilder.makeLiteral(boolType));
                 res =
                     translator.rexBuilder.makeCall(
                         SqlStdOperatorTable.andOperator,
                         notNullCall,
                         eqCall);
             } else {
                 res =
                     translator.rexBuilder.makeCall(
                         SqlStdOperatorTable.equalsOperator,
                         operand,
                         translator.rexBuilder.makeLiteral(boolType));
             }
             return translator.implementNode(res);
         }
     }
 
     /**
      * Implements "IS NOT TRUE" and "IS NOT FALSE" operators.
      */
     private static class IsNotBoolImplementor
         extends IsBoolImplementor
     {
         IsNotBoolImplementor(boolean boolType)
         {
             super(boolType);
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             super.implement(call, translator);
             res =
                 translator.rexBuilder.makeCall(
                     SqlStdOperatorTable.notOperator,
                     res);
             return translator.implementNode(res);
         }
     }
 
     /**
      * A Class that gets a specified operand of a call and retrieves its charset
      * name and add it as a vc literal to the program. This of course assumes
      * the operand is a chartype. If this is not the case an assert is fired.
      */
     private static class AddCharSetNameInstrImplementor
         extends InstrDefImplementor
     {
         int operand;
 
         AddCharSetNameInstrImplementor(
             String extCall,
             int regCount,
             int operand)
         {
             super(new CalcProgramBuilder.ExtInstrDef(extCall, regCount));
             this.operand = operand;
         }
 
         /**
          * @pre SqlTypeUtil.inCharFamily(call.operands[operand].getType())
          */
         protected List<CalcReg> makeRegList(
             RexToCalcTranslator translator,
             RexCall call)
         {
             Util.pre(
                 SqlTypeUtil.inCharFamily(call.operands[operand].getType()),
                 "SqlTypeUtil.inCharFamily(call.operands[operand].getType()");
 
             List<CalcReg> regList =
                 super.makeRegList(translator, call);
             CalcReg charSetName =
                 translator.builder.newVarcharLiteral(
                     call.operands[operand].getType().getCharset().name());
             regList.add(charSetName);
             return regList;
         }
     }
 
     /**
      * Implements a call by invoking a given instruction.
      */
     private static class UsingInstrImplementor
         extends AbstractCalcRexImplementor
     {
         CalcProgramBuilder.InstructionDef instr;
 
         /**
          * @pre null != instr
          */
         UsingInstrImplementor(CalcProgramBuilder.InstructionDef instr)
         {
             Util.pre(null != instr, "null != instr");
             this.instr = instr;
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             return implementUsingInstr(instr, translator, call);
         }
     }
 
     /**
      * Implementor that will convert a {@link RexCall}'s operands to approx
      * DOUBLE if needed
      */
     private static class MakeOperandsDoubleImplementor
         extends InstrDefImplementor
     {
         MakeOperandsDoubleImplementor(CalcProgramBuilder.InstructionDef instr)
         {
             super(instr);
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             call = call.clone();
             for (int i = 0; i < call.operands.length; i++) {
                 RexNode operand = call.operands[i];
                 if (!operand.getType().getSqlTypeName().equals(
                         SqlTypeName.Double)) {
                     RelDataType oldType = operand.getType();
                     RelDataTypeFactory fac =
                         translator.rexBuilder.getTypeFactory();
 
                     //todo do a reverse lookup on OpType.Double instead
                     RelDataType doubleType =
                         fac.createSqlType(SqlTypeName.Double);
                     doubleType =
                         fac.createTypeWithNullability(
                             doubleType,
                             oldType.isNullable());
                     RexNode castCall =
                         translator.rexBuilder.makeCast(doubleType,
                             call.operands[i]);
                     call.operands[i] = castCall;
                 }
             }
             assert (0 != call.operands.length);
             return super.implement(call, translator);
         }
     }
 
     /**
      * Implementor for CAST operator.
      */
     private static class CastImplementor
         extends AbstractCalcRexImplementor
     {
         private final Map<Pair<SqlTypeName, SqlTypeName>, CalcRexImplementor>
             doubleKeyMap =
             new HashMap<Pair<SqlTypeName, SqlTypeName>, CalcRexImplementor>();
         static final CastImplementor instance = new CastImplementor();
 
         private CastImplementor()
         {
             putMM(
                 SqlTypeName.intTypes,
                 SqlTypeName.intTypes,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
             putMM(
                 SqlTypeName.intTypes,
                 SqlTypeName.approxTypes,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
             putMS(
                 SqlTypeName.datetimeTypes,
                 SqlTypeName.Bigint,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castDateToMillis));
             // REVIEW angel 2006-08-31 - allow cast from intervals to bigint?
             // TODO: Replace castDateToMillis with real cast from/to interval
             //  (Okay to use castDateToMillis for now since it just
             //   stuffs one int64 value into another)
             putMS(
                 SqlTypeName.timeIntervalTypes,
                 SqlTypeName.Bigint,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castDateToMillis));
             putSM(
                 SqlTypeName.Bigint,
                 SqlTypeName.timeIntervalTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castDateToMillis));
 
             putMM(
                 SqlTypeName.booleanTypes,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castA));
             putMM(
                 SqlTypeName.intTypes,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(ExtInstructionDefTable.castA) {
                     public CalcReg implement(
                         RexCall call,
                         RexToCalcTranslator translator)
                     {
                         RexCall newCall =
                             implementFirstOperandWithInt8(call,
                                 translator,
                                 call.operands[0],
                                 0,
                                 false);
                         if (newCall.equals(call)) {
                             return super.implement(call, translator);
                         }
                         return translator.implementNode(newCall);
                     }
                 });
 
             putMM(
                 SqlTypeName.approxTypes,
                 SqlTypeName.approxTypes,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
             putMM(
                 SqlTypeName.approxTypes,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(ExtInstructionDefTable.castA));
             putMM(
                 SqlTypeName.approxTypes,
                 SqlTypeName.intTypes,
                 new AbstractCalcRexImplementor() {
                     public CalcReg implement(
                         RexCall call,
                         RexToCalcTranslator translator)
                     {
                         assert (call.isA(RexKind.Cast));
                         CalcReg beforeRound =
                             translator.implementNode(call.operands[0]);
                         CalcProgramBuilder.RegisterDescriptor regDesc =
                             translator.getCalcRegisterDescriptor(
                                 call.operands[0]);
                         CalcReg afterRound =
                             translator.builder.newLocal(regDesc);
                         CalcProgramBuilder.round.add(
                             translator.builder,
                             new CalcReg[] {
                                 afterRound, beforeRound
                             });
                         CalcReg res = createResultRegister(translator, call);
                         CalcProgramBuilder.Cast.add(
                             translator.builder,
                             new CalcReg[] {
                                 res, afterRound
                             });
                         return res;
                     }
                 });
 
             putMS(
                 SqlTypeName.charTypes,
                 SqlTypeName.Date,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castStrAToDate));
             putSM(
                 SqlTypeName.Date,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castDateToStr));
 
             putMS(
                 SqlTypeName.charTypes,
                 SqlTypeName.Time,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castStrAToTime));
             putSM(
                 SqlTypeName.Time,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castTimeToStr));
 
             putMS(
                 SqlTypeName.charTypes,
                 SqlTypeName.Timestamp,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castStrAToTimestamp));
             putSM(
                 SqlTypeName.Timestamp,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castTimestampToStr));
 
             // Timestamp to date, time types
             putSM(
                 SqlTypeName.Timestamp,
                 SqlTypeName.datetimeTypes,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
 
             // date, time to timestamp
             put(
                 SqlTypeName.Date,
                 SqlTypeName.Timestamp,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
             put(
                 SqlTypeName.Time,
                 SqlTypeName.Timestamp,
                 new UsingInstrImplementor(CalcProgramBuilder.Cast));
 
             putMM(
                 SqlTypeName.charTypes,
                 SqlTypeName.booleanTypes,
                 new UsingInstrImplementor(
                     ExtInstructionDefTable.castA));
 
             putMM(
                 SqlTypeName.charTypes,
                 SqlTypeName.intTypes,
                 new UsingInstrImplementor(ExtInstructionDefTable.castA) {
                     public CalcReg implement(
                         RexCall call,
                         RexToCalcTranslator translator)
                     {
                         RexCall newCall =
                             implementFirstOperandWithInt8(call,
                                 translator,
                                 call,
                                 0,
                                 false);
                         if (newCall.equals(call)) {
                             return super.implement(call, translator);
                         }
                         return translator.implementNode(newCall);
                     }
                 });
             putMM(
                 SqlTypeName.charTypes,
                 SqlTypeName.approxTypes,
                 new UsingInstrImplementor(ExtInstructionDefTable.castA) {
                     public CalcReg implement(
                         RexCall call,
                         RexToCalcTranslator translator)
                     {
                         RexCall newCall =
                             implementFirstOperandWithDouble(call,
                                 translator,
                                 call,
                                 0,
                                 false);
                         if (newCall.equals(call)) {
                             return super.implement(call, translator);
                         }
                         return translator.implementNode(newCall);
                     }
                 });
 
             putMM(
                 SqlTypeName.charTypes,
                 SqlTypeName.charTypes,
                 new UsingInstrImplementor(ExtInstructionDefTable.castA));
 
             putSM(
                 SqlTypeName.Decimal,
                 SqlTypeName.charTypes,
                 new CastDecimalImplementor(
                     ExtInstructionDefTable.castADecimal));
             putMS(
                 SqlTypeName.charTypes,
                 SqlTypeName.Decimal,
                 new CastDecimalImplementor(
                     ExtInstructionDefTable.castADecimal));
         }
 
         private void putMM(
             SqlTypeName[] t1s, SqlTypeName[] t2s, CalcRexImplementor value)
         {
             for (int i = 0; i < t1s.length; i++) {
                 putSM(t1s[i], t2s, value);
             }
         }
 
         private void putMS(
             SqlTypeName[] t1s, SqlTypeName t2, CalcRexImplementor value)
         {
             for (int i = 0; i < t1s.length; i++) {
                 put(t1s[i], t2, value);
             }
         }
 
         private void putSM(
             SqlTypeName t1, SqlTypeName[] t2s, CalcRexImplementor value)
         {
             for (int i = 0; i < t2s.length; i++) {
                 put(t1, t2s[i], value);
             }
         }
 
         private void put(
             SqlTypeName t1, SqlTypeName t2, CalcRexImplementor value)
         {
             assert value != null;
             CalcRexImplementor s = doubleKeyMap.put(
                 new Pair<SqlTypeName, SqlTypeName>(t1, t2),
                 value);
             assert s == null : "key already existed";
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             Util.pre(call.operands.length == 1, "call.operands.length == 1");
 
             final RexNode operand = translator.resolve(call.operands[0]);
             if (RexLiteral.isNullLiteral(operand)) {
                 CalcProgramBuilder.RegisterDescriptor resultDesc =
                     translator.getCalcRegisterDescriptor(call);
 
                 // If the type is one which requires an explicit storage
                 // specification, tell it we need 0 bytes;
                 // otherwise keep it at -1.
                 if (resultDesc.getBytes() >= 0) {
                     resultDesc =
                         new CalcProgramBuilder.RegisterDescriptor(
                             resultDesc.getType(),
                             0);
                 }
 
                 return translator.builder.newLiteral(resultDesc, null);
             }
 
             // Figure out the source and destination types.
             RelDataType fromType = operand.getType();
             SqlTypeName fromTypeName = fromType.getSqlTypeName();
             RelDataType toType = call.getType();
             SqlTypeName toTypeName = toType.getSqlTypeName();
 
             CalcRexImplementor implentor =
                 doubleKeyMap.get(
                     new Pair<SqlTypeName, SqlTypeName>(
                         fromTypeName, toTypeName));
             if (null != implentor) {
                 return implentor.implement(call, translator);
             }
 
             if (SqlTypeUtil.sameNamedType(toType, fromType)) {
                 return translator.implementNode(operand);
             }
 
             throw Util.needToImplement(
                 "Cast from '" + fromType.toString()
                 + "' to '" + toType.toString() + "'");
         }
 
         static class Pair <T1, T2>
         {
             private final T1 o1;
             private final T2 o2;
 
             Pair(T1 o1, T2 o2)
             {
                 this.o1 = o1;
                 this.o2 = o2;
             }
 
             public boolean equals(Object obj)
             {
                 return obj instanceof Pair
                     && Util.equal(this.o1, ((Pair) obj).o1)
                     && Util.equal(this.o2, ((Pair) obj).o2);
             }
 
             public int hashCode()
             {
                 int h1 = Util.hash(0, o1);
                 return Util.hash(h1, o2);
             }
         }
 
         static class DoubleKeyMap
             extends HashMap<Pair<SqlTypeName, SqlTypeName>, CalcRexImplementor>
         {
 
         }
 
         /**
          * Implementor for casting between char and decimal types
          */
         private static class CastDecimalImplementor
             extends InstrDefImplementor
         {
             CastDecimalImplementor(CalcProgramBuilder.InstructionDef instr)
             {
                 super(instr);
             }
 
             // refine InstrDefImplementor
             protected List<CalcReg> makeRegList(
                 RexToCalcTranslator translator,
                 RexCall call)
             {
                 RelDataType decimalType;
                 Util.pre(
                     SqlTypeUtil.isDecimal(call.getType())
                     || SqlTypeUtil.isDecimal(call.operands[0].getType()),
                     "CastDecimalImplementor can only cast decimal types");
                 if (SqlTypeUtil.isDecimal(call.getType())) {
                     Util.pre(
                         SqlTypeUtil.inCharFamily(call.operands[0].getType()),
                         "CalRex cannot cast non char type to decimal");
                     decimalType = call.getType();
                 } else {
                     Util.pre(
                         SqlTypeUtil.inCharFamily(call.getType()),
                         "CalRex cannot cast from decimal to non char type");
                     decimalType = call.operands[0].getType();
                 }
                 RexLiteral precision =
                     translator.rexBuilder.makeExactLiteral(
                         BigDecimal.valueOf(decimalType.getPrecision()));
                 RexLiteral scale =
                     translator.rexBuilder.makeExactLiteral(
                         BigDecimal.valueOf(decimalType.getScale()));
 
                 List<CalcReg> regList =
                     implementOperands(call, translator);
                 regList.add(translator.implementNode(precision));
                 regList.add(translator.implementNode(scale));
                 regList.add(
                     0,
                     createResultRegister(translator, call));
                 return regList;
             }
         }
     }
 
     /**
      * Implementor for REINTERPRET operator.
      */
     private static class ReinterpretCastImplementor
         extends AbstractCalcRexImplementor
     {
         public boolean canImplement(RexCall call)
         {
             return (call.isA(RexKind.Reinterpret));
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             RexNode valueArg = call.operands[0];
             boolean checkOverflow = RexUtil.canReinterpretOverflow(call);
 
             CalcReg value = translator.implementNode(valueArg);
             if (checkOverflow) {
                 if (!SqlTypeUtil.isIntType(valueArg.getType())) {
                     valueArg =
                         translator.rexBuilder.makeReinterpretCast(
                             call.getType(),
                             valueArg,
                             translator.rexBuilder.makeLiteral(false));
                 }
 
                 // perform overflow check:
                 //     if (value is null) goto [endCheck]
                 //     bool overflowed = ( abs(value) >= overflowValue )
                 //     if (!overflowed) goto [endCheck]
                 //     throw overflow exception
                 // [endCheck]
                 String endCheck = translator.newLabel();
                 if (valueArg.getType().isNullable()) {
                     RexNode nullCheck =
                         translator.rexBuilder.makeCall(
                             SqlStdOperatorTable.isNullOperator,
                             valueArg);
                     CalcReg isNull = translator.implementNode(nullCheck);
                     translator.builder.addLabelJumpTrue(endCheck, isNull);
                 }
                 RexNode overflowValue =
                     translator.rexBuilder.makeExactLiteral(
                         new BigDecimal(
                             NumberUtil.getMaxUnscaled(
                                 call.getType().getPrecision())));
                 RexNode comparison =
                     translator.rexBuilder.makeCall(
                         SqlStdOperatorTable.greaterThanOperator,
                         translator.rexBuilder.makeCall(
                             SqlStdOperatorTable.absFunc,
                             valueArg),
                         overflowValue);
                 CalcReg overflowed = translator.implementNode(comparison);
                 translator.builder.addLabelJumpFalse(endCheck, overflowed);
                 CalcReg errorMsg =
                     translator.builder.newVarcharLiteral(
                         SqlStateCodes.NumericValueOutOfRange.getState());
                 CalcProgramBuilder.raise.add(
                     translator.builder,
                     errorMsg);
                 translator.builder.addLabel(endCheck);
             }
             return value;
         }
     }
 
     /**
      * Makes all numeric types the same before calling a given {@link
      * CalcProgramBuilder.InstructionDef instruction}. The way to make the types
      * the same is by inserting appropiate calls to cast functions depending on
      * the types. The "biggest" (least restrictive) type will always win and
      * other types will be conveted into that bigger type. For example. In the
      * expression <code>1.0+2</code>, the '+' instruction is potentially called
      * with types <code>(DOUBLE) + (INTEGER)</code> which is illegal (in terms
      * of the calculator). Therefore the expression's implementation will
      * logically end looking something like <code>1.0 + CAST(2 AS DOUBLE)</code>
      * LIMITATION: For now only Binary operators are supported with numeric types
      * If any operand is of any other type than a numeric one, the base class
      * {@link InstrDefImplementor#implement implementation} will be called
      */
     private static class BinaryNumericMakeSametypeImplementor
         extends InstrDefImplementor
     {
         // Set this to true if the result type need to be same as operands
         boolean useSameTypeResult = false;
 
         public BinaryNumericMakeSametypeImplementor(
             CalcProgramBuilder.InstructionDef instr)
         {
             super(instr);
         }
 
         public BinaryNumericMakeSametypeImplementor(
             CalcProgramBuilder.InstructionDef instr,
             boolean useSameTypeResult)
         {
             this(instr);
             this.useSameTypeResult = useSameTypeResult;
         }
 
         private int getRestrictiveness(
             CalcProgramBuilder.RegisterDescriptor rd)
         {
             switch (rd.getType().getOrdinal()) {
             case CalcProgramBuilder.OpType.Bool_ordinal:
                 return 5;
             case CalcProgramBuilder.OpType.Uint1_ordinal:
                 return 10;
             case CalcProgramBuilder.OpType.Int1_ordinal:
                 return 20;
             case CalcProgramBuilder.OpType.Uint2_ordinal:
                 return 30;
             case CalcProgramBuilder.OpType.Int2_ordinal:
                 return 40;
             case CalcProgramBuilder.OpType.Uint4_ordinal:
                 return 50;
             case CalcProgramBuilder.OpType.Int4_ordinal:
                 return 60;
             case CalcProgramBuilder.OpType.Uint8_ordinal:
                 return 70;
             case CalcProgramBuilder.OpType.Int8_ordinal:
                 return 80;
             case CalcProgramBuilder.OpType.Real_ordinal:
                 return 1000;
             case CalcProgramBuilder.OpType.Double_ordinal:
                 return 1010;
             default:
                 throw rd.getType().unexpected();
             }
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             assert (2 == call.operands.length);
             List<CalcReg> regs = implementOperands(call, translator);
             CalcProgramBuilder.RegisterDescriptor rd0 =
                 translator.getCalcRegisterDescriptor(call.operands[0]);
             CalcProgramBuilder.RegisterDescriptor rd1 =
                 translator.getCalcRegisterDescriptor(call.operands[1]);
 
             if (!rd0.getType().isNumeric() || !rd1.getType().isNumeric()) {
                 return super.implement(call, translator);
             }
 
             CalcProgramBuilder.RegisterDescriptor rd = null;
             int d = getRestrictiveness(rd0) - getRestrictiveness(rd1);
             if (d != 0) {
                 int small;
                 if (d > 0) {
                     small = 1;
                     rd = rd0;
                 } else {
                     small = 0;
                     rd = rd1;
                 }
 
                 RelDataType castToType = call.operands[(small + 1) % 2].getType();
                 // REVIEW: angel 2006-08-27 Force operations with
                 // intervals to be treated as bigint operations
                 if (SqlTypeUtil.isInterval(castToType)) {
                     RelDataTypeFactory fac = translator.rexBuilder.getTypeFactory();
                     RelDataType int8 = fac.createSqlType(SqlTypeName.Bigint);
                     castToType = int8;
                 }
                 RexNode castCall =
                     translator.rexBuilder.makeCast(
                         castToType,
                         call.operands[small]);
                 CalcReg newOp = translator.implementNode(castCall);
                 regs.set(small, newOp);
             }
 
             if (useSameTypeResult && (rd != null)) {
                 // Need to use the same type for the result as the operands
                 CalcProgramBuilder.RegisterDescriptor rdCall =
                     translator.getCalcRegisterDescriptor(call.getType());
                 if (rdCall.getType().isNumeric()
                     && (
                         (getRestrictiveness(rd) - getRestrictiveness(
                                 rdCall)) != 0
                        )) {
                     // Do operation using same type as operands
                     CalcReg tmpRes = translator.builder.newLocal(rd);
                     regs.add(0, tmpRes);
 
                     instr.add(translator.builder, regs);
 
                     // Cast back to real result type
                     // TODO: Use real cast (that handles rounding) instead of
                     // calculator cast that truncates
                     CalcReg res = createResultRegister(translator, call);
 
                     List<CalcReg> castRegs = new ArrayList<CalcReg>(2);
                     castRegs.add(res);
                     castRegs.add(tmpRes);
                     CalcProgramBuilder.Cast.add(translator.builder, castRegs);
 
                     return res;
                 }
             }
 
             CalcReg res = createResultRegister(translator, call);
             regs.add(0, res);
 
             instr.add(translator.builder, regs);
             return res;
         }
     }
 
     /**
      * Makes all string types the same before calling a given {@link
      * CalcProgramBuilder.InstructionDef instruction}. The way to make the types
      * the same is by inserting appropiate calls to cast functions depending on
      * the types. The "biggest" (least restrictive) type will always win and
      * other types will be conveted into that bigger type. For example. In the
      * expression <code>POSITION('varchar' in 'char')</code>, will end up
      * looking something like <code>POSITION('varchar' in CAST('char' AS
      * VARCHAR))</code> LIMITATION: For now only char types are supported
      */
     private static class BinaryStringMakeSametypeImplementor
         extends InstrDefImplementor
     {
         private int iFirst;
         private int iSecond;
 
         /**
          * @param iFirst Indicates which two operands in the call list that
          * should be made the same type.
          * @param iSecond Indicates which two operands in the call list that
          * should be made the same type.
          */
         public BinaryStringMakeSametypeImplementor(
             CalcProgramBuilder.InstructionDef instr,
             int iFirst,
             int iSecond)
         {
             super(instr);
             assert (iFirst != iSecond);
             this.iFirst = iFirst;
             this.iSecond = iSecond;
         }
 
         /**
          * Convenience constructor that calls {@link
          * #BinaryStringMakeSametypeImplementor(com.disruptivetech.farrago.calc.CalcProgramBuilder.InstructionDef,
          * int, int)} with the iFirst=0 and iSecond=1
          */
         public BinaryStringMakeSametypeImplementor(
             CalcProgramBuilder.InstructionDef instr)
         {
             this(instr, 0, 1);
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             List<CalcReg> regs = implementOperands(call, translator);
             CalcReg [] newRegs = {regs.get(iFirst), regs.get(iSecond)};
 
             translator.implementConversionIfNeeded(
                 call.operands[iFirst],
                 call.operands[iSecond],
                 newRegs,
                 true);
             regs.set(iFirst, newRegs[0]);
             regs.set(iSecond, newRegs[1]);
 
             CalcReg res = createResultRegister(translator, call);
             regs.add(0, res);
 
             instr.add(translator.builder, regs);
             return res;
         }
     }
 
     /**
      * Implementor for CASE operator.
      */
     private static class CaseImplementor
         extends AbstractCalcRexImplementor
     {
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             Util.pre(call.operands.length > 1, "call.operands.length>1");
             Util.pre((call.operands.length & 1) == 1,
                 "(call.operands.length&1)==1");
             CalcReg resultOfCall = createResultRegister(translator, call);
             String endOfCase = translator.newLabel();
             String next;
             boolean elseClauseOptimizedAway = false;
             for (int i = 0; i < (call.operands.length - 1); i += 2) {
                 next = translator.newLabel();
                 CalcReg compareResult =
                     translator.implementNode(call.operands[i]);
                 assert (compareResult.getOpType().equals(
                             CalcProgramBuilder.OpType.Bool));
                 if (!compareResult.getRegisterType().equals(
                         CalcProgramBuilder.RegisterSetType.Literal)) {
                     translator.builder.addLabelJumpFalse(next, compareResult);
 
                     // todo optimize away null check if type known to be non
                     // null same applies the other way (if we have a null
                     // literal or a cast(null as xxx))
                     translator.builder.addLabelJumpNull(next, compareResult);
                     implementCaseValue(
                         translator,
                         resultOfCall,
                         call.getType(),
                         call.operands[i + 1]);
                     translator.builder.addLabelJump(endOfCase);
                     translator.builder.addLabel(next);
                 } else {
                     // we can do some optimizations
                     Boolean val = (Boolean) compareResult.getValue();
                     if (val.booleanValue()) {
                         implementCaseValue(
                             translator,
                             resultOfCall,
                             call.getType(),
                             call.operands[i + 1]);
                         if (i != 0) {
                             translator.builder.addLabelJump(endOfCase);
                         }
                         translator.builder.addLabel(next);
                         elseClauseOptimizedAway = true;
                         break;
                     }
 
                     // else we dont need to do anything
                 }
             }
 
             if (!elseClauseOptimizedAway) {
                 int elseIndex = call.operands.length - 1;
                 implementCaseValue(
                     translator,
                     resultOfCall,
                     call.getType(),
                     call.operands[elseIndex]);
             }
             translator.builder.addLabel(endOfCase); //this assumes that more
                                                     //instructions will follow
             return resultOfCall;
         }
 
         private void implementCaseValue(
             RexToCalcTranslator translator,
             CalcReg resultOfCall,
             RelDataType resultDataType,
             RexNode value)
         {
             translator.newScope();
             try {
                 RelDataType valueType = value.getType();
 
                 // TODO: Don't need cast if only nullability differs
                 boolean castNeeded = !resultDataType.equals(valueType);
 
                 if (castNeeded) {
                     // Do cast from original type to result type
                     RexNode castCall =
                         translator.rexBuilder.makeCast(
                             resultDataType,
                             value);
                     CalcReg newOp = translator.implementNode(castCall);
                     CalcProgramBuilder.move.add(
                         translator.builder,
                         resultOfCall,
                         newOp);
                 } else {
                     CalcReg operand = translator.implementNode(value);
                     CalcProgramBuilder.move.add(
                         translator.builder,
                         resultOfCall,
                         operand);
                 }
             } finally {
                 translator.popScope();
             }
         }
     }
 
     /**
      * Implements the identity operator.
      *
      * <p>The prefix plus operator uses this implementor, because "+ x" is
      * always the same as "x".
      */
     private static class IdentityImplementor
         implements CalcRexImplementor
     {
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             return translator.implementNode(call.operands[0]);
         }
 
         public boolean canImplement(RexCall call)
         {
             return true;
         }
     }
 
     /**
      * Implements the TRIM function.
      */
     private static class TrimImplementor
         extends InstrDefImplementor
     {
         public TrimImplementor()
         {
             super(ExtInstructionDefTable.trim);
         }
 
         protected List<CalcReg> makeRegList(
             RexToCalcTranslator translator,
             RexCall call)
         {
             List<CalcReg> regList = new ArrayList<CalcReg>();
 
             CalcReg resultOfCall = createResultRegister(translator, call);
             RexNode op0 = call.operands[0];
             final RexLiteral literal = translator.getLiteral(op0);
             SqlTrimFunction.Flag flag =
                 (SqlTrimFunction.Flag) literal.getValue();
 
             regList.add(resultOfCall);
             regList.add(translator.implementNode(call.operands[2])); //str to trim from
             regList.add(translator.implementNode(call.operands[1])); //trim char
             regList.add(translator.builder.newInt4Literal(flag.getLeft()));
             regList.add(translator.builder.newInt4Literal(flag.getRight()));
 
             return regList;
         }
     }
 
     private static class ConcatImplementor
         extends AbstractCalcRexImplementor
     {
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             assert (!translator.containsResult(call)); //avoid reentrancy
             CalcReg resultReg = createResultRegister(translator, call);
             return implement(call, translator, resultReg);
         }
 
         private CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator,
             CalcReg resultRegister)
         {
             assert (2 == call.operands.length);
             List<CalcReg> regList = new ArrayList<CalcReg>();
             regList.add(resultRegister);
 
             if ((!(call.operands[0] instanceof RexCall)
                     || !((RexCall) call.operands[0]).getOperator().equals(
                         SqlStdOperatorTable.concatOperator))) {
                 regList.add(translator.implementNode(call.operands[0]));
             } else {
                 //recursively calling this method again
                 implement((RexCall) call.operands[0],
                     translator,
                     resultRegister);
             }
 
             regList.add(translator.implementNode(call.operands[1]));
             assert (regList.size() > 1);
             boolean castToVarchar = false;
             if (!CalcProgramBuilder.OpType.Char.equals(
                     resultRegister.getOpType())) {
                 assert (CalcProgramBuilder.OpType.Varchar.equals(
                             resultRegister.getOpType()));
                 castToVarchar = true;
             }
             for (int i = 1; i < regList.size(); i++) {
                 CalcReg reg = regList.get(i);
 
                 if (castToVarchar
                     && !reg.getOpType().equals(
                         CalcProgramBuilder.OpType.Varchar)) {
                     // cast to varchar call must be of type varchar.
                     CalcReg newReg =
                         translator.builder.newLocal(
                             translator.getCalcRegisterDescriptor(call));
 
                     ExtInstructionDefTable.castA.add(
                         translator.builder,
                         new CalcReg[] { newReg, reg });
                     regList.set(i, newReg);
                 }
             }
 
             ExtInstructionDefTable.concat.add(translator.builder, regList);
             return resultRegister;
         }
     }
 
     /**
      * Abstract base class for classes which implement {@link
      * CalcRexAggImplementor}.
      */
     public static abstract class AbstractCalcRexAggImplementor
         implements CalcRexAggImplementor
     {
         public boolean canImplement(RexCall call)
         {
             return true;
         }
     }
 
     /**
      * Implementation of the <code>COUNT</code> aggregate function, {@link
      * SqlStdOperatorTable#countOperator}.
      */
     private static class CountCalcRexImplementor
         extends AbstractCalcRexAggImplementor
     {
         public void implementInitialize(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             // O s8; V 0; T; MOVE O0, C0;
             assert (call.operands.length == 0) || (call.operands.length == 1);
 
             final CalcReg zeroReg =
                 translator.builder.newLiteral(
                     translator.getCalcRegisterDescriptor(call),
                     0);
             CalcProgramBuilder.move.add(
                 translator.builder,
                 accumulatorRegister,
                 zeroReg);
         }
 
         public void implementAdd(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             // I s8; V 1; T; ADD O0, O0, C0;
             assert (call.operands.length == 0) || (call.operands.length == 1);
 
             final CalcReg oneReg =
                 translator.builder.newLiteral(
                     translator.getCalcRegisterDescriptor(call),
                     1);
 
             // If operand is null, then it is like count(*).
             // Otherwise, it is like count(x).
             RexNode operand = null;
             if (call.operands.length == 1) {
                 operand = call.operands[0];
             }
 
             // Minor optimization where count(*) = count(x) if x cannot be null.
             // See the help for SumCalcRexImplementor.implementAdd()
             if ((operand != null) && operand.getType().isNullable()) {
                 int ordinal = translator.getNullRegisterOrdinal();
                 CalcReg isNullReg = null;
                 String wasNotNull = translator.newLabel();
                 String next = translator.newLabel();
                 if (ordinal == -1) {
                     isNullReg =
                         translator.builder.newLocal(
                             CalcProgramBuilder.OpType.Bool,
                             -1);
                     translator.setNullRegisterOrdinal(
                         translator.builder.registerSets.getSet(
                             CalcProgramBuilder.RegisterSetType.LocalORDINAL)
                         .size() - 1);
                 } else {
                     isNullReg =
                         translator.builder.getRegister(ordinal,
                             CalcProgramBuilder.RegisterSetType.Local);
                 }
 
                 CalcReg input = null;
                 final RexNode [] inputExps = translator.getInputExprs();
                 if (inputExps != null) {
                     RexNode arg = inputExps[((RexInputRef) operand).getIndex()];
                     input = translator.implementNode(arg);
                 } else {
                     input = translator.implementNode(operand);
                 }
 
                 CalcProgramBuilder.boolNativeIsNull.add(translator.builder,
                     isNullReg,
                     input);
                 translator.builder.addLabelJumpFalse(wasNotNull, isNullReg);
                 translator.builder.addLabelJump(next);
                 translator.builder.addLabel(wasNotNull);
                 CalcProgramBuilder.nativeAdd.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     oneReg);
                 translator.builder.addLabel(next);
             } else {
                 CalcProgramBuilder.nativeAdd.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     oneReg);
             }
         }
 
         public void implementDrop(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             // I s8; V 1; T; SUB O0, O0, C0;
             assert (call.operands.length == 0) || (call.operands.length == 1);
 
             final CalcReg oneReg =
                 translator.builder.newLiteral(
                     translator.getCalcRegisterDescriptor(call),
                     1);
 
             // If operand is null, then it is like count(*).
             // Otherwise, it is like count(x).
             RexNode operand = null;
             if (call.operands.length == 1) {
                 operand = call.operands[0];
             }
 
             // Minor optimization where count(*) = count(x) if x cannot be null.
             // See the help for SumCalcRexImplementor.implementDrop()
             if ((operand != null) && operand.getType().isNullable()) {
                 int ordinal = translator.getNullRegisterOrdinal();
                 CalcReg isNullReg = null;
                 String wasNotNull = translator.newLabel();
                 String next = translator.newLabel();
                 if (ordinal == -1) {
                     isNullReg =
                         translator.builder.newLocal(
                             CalcProgramBuilder.OpType.Bool,
                             -1);
                     translator.setNullRegisterOrdinal(
                         translator.builder.registerSets.getSet(
                             CalcProgramBuilder.RegisterSetType.LocalORDINAL)
                         .size() - 1);
                 } else {
                     isNullReg =
                         translator.builder.getRegister(ordinal,
                             CalcProgramBuilder.RegisterSetType.Local);
                 }
 
                 CalcReg input = null;
                 final RexNode [] inputExps = translator.getInputExprs();
                 if (inputExps != null) {
                     RexNode arg = inputExps[((RexInputRef) operand).getIndex()];
                     input = translator.implementNode(arg);
                 } else {
                     input = translator.implementNode(operand);
                 }
 
                 CalcProgramBuilder.boolNativeIsNull.add(translator.builder,
                     isNullReg,
                     input);
                 translator.builder.addLabelJumpFalse(wasNotNull, isNullReg);
                 translator.builder.addLabelJump(next);
                 translator.builder.addLabel(wasNotNull);
                 CalcProgramBuilder.nativeMinus.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     oneReg);
                 translator.builder.addLabel(next);
             } else {
                 CalcProgramBuilder.nativeMinus.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     oneReg);
             }
         }
     }
 
     /**
      * Implementation of the <code>SUM</code> aggregate function, {@link
      * SqlStdOperatorTable#sumOperator}.
      */
     private static class SumCalcRexImplementor
         extends AbstractCalcRexAggImplementor
     {
         public void implementInitialize(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             // O s8; V 0; T; MOVE O0, C0;
             assert call.operands.length == 1;
 
             final CalcReg zeroReg =
                 translator.builder.newLiteral(
                     translator.getCalcRegisterDescriptor(call),
                     0);
             CalcProgramBuilder.move.add(
                 translator.builder,
                 accumulatorRegister,
                 zeroReg);
         }
 
         public void implementAdd(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             assert call.operands.length == 1;
             final RexNode operand = call.operands[0];
 
             CalcReg input = null;
             final RexNode [] inputExps = translator.getInputExprs();
             if (inputExps != null) {
                 RexNode arg = inputExps[((RexInputRef) operand).getIndex()];
                 input = translator.implementNode(arg);
             } else {
                 input = translator.implementNode(operand);
             }
 
             // If the input can be null, then we check if the value is in fact
             // null and then skip adding it to the total. If, however, the value
             // cannot be null, we can simply perform the add operation. One
             // important point to remember here is that we should design this
             // such that multiple aggregations generate valid code too, i.e.,
             // sum(col1), sum(col2) should generate correct code by computing
             // both the sums (so we cannot have return statements and jump
             // statements in sum(col1) should jump correctly to the next
             // instruction row of a subsequent call to this method needed for
             // sum(col2). Here is the pseudo code: ISNULL nullReg, col1    /* 0
             // */ JMPF @3, nullReg        /* 1 */ JMP @4                  /* 2
             // */ ADD O0, O0, col1        /* 3 */                         /* 4
             // */ Note that a label '4' is created for a row that doesn't have
             // any instruction. This is critical both when there is sum(col2) or
             // simply a return statement.
             if (operand.getType().isNullable()) {
                 int ordinal = translator.getNullRegisterOrdinal();
                 CalcReg isNullReg = null;
                 String wasNotNull = translator.newLabel();
                 String next = translator.newLabel();
                 if (ordinal == -1) {
                     isNullReg =
                         translator.builder.newLocal(
                             CalcProgramBuilder.OpType.Bool,
                             -1);
                     translator.setNullRegisterOrdinal(
                         translator.builder.registerSets.getSet(
                             CalcProgramBuilder.RegisterSetType.LocalORDINAL)
                         .size() - 1);
                 } else {
                     isNullReg =
                         translator.builder.getRegister(ordinal,
                             CalcProgramBuilder.RegisterSetType.Local);
                 }
                 CalcProgramBuilder.boolNativeIsNull.add(
                     translator.builder,
                     isNullReg,
                     translator.implementNode(operand));
                 translator.builder.addLabelJumpFalse(wasNotNull, isNullReg);
                 translator.builder.addLabelJump(next);
                 translator.builder.addLabel(wasNotNull);
                 CalcProgramBuilder.nativeAdd.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     input);
                 translator.builder.addLabel(next);
             } else {
                 CalcProgramBuilder.nativeAdd.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     input);
             }
         }
 
         public void implementDrop(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             assert call.operands.length == 1;
             final RexNode operand = call.operands[0];
 
             CalcReg input = null;
             final RexNode [] inputExps = translator.getInputExprs();
             if (inputExps != null) {
                 RexNode arg = inputExps[((RexInputRef) operand).getIndex()];
                 input = translator.implementNode(arg);
             } else {
                 input = translator.implementNode(operand);
             }
 
             // Refer to the comments for implementAdd method above.
             // Here is the pseudo code:
             // ISNULL nullReg, col1    /* 0 */
             // JMPF @3, nullReg        /* 1 */
             // JMP @4                  /* 2 */
             // SUB O0, O0, col1        /* 3 */
             //                         /* 4 */
             if (operand.getType().isNullable() && (inputExps == null)) {
                 int ordinal = translator.getNullRegisterOrdinal();
                 CalcReg isNullReg = null;
                 String wasNotNull = translator.newLabel();
                 String next = translator.newLabel();
                 if (ordinal == -1) {
                     isNullReg =
                         translator.builder.newLocal(
                             CalcProgramBuilder.OpType.Bool,
                             -1);
                     translator.setNullRegisterOrdinal(
                         translator.builder.registerSets.getSet(
                             CalcProgramBuilder.RegisterSetType.LocalORDINAL)
                         .size() - 1);
                 } else {
                     isNullReg =
                         translator.builder.getRegister(ordinal,
                             CalcProgramBuilder.RegisterSetType.Local);
                 }
                 CalcProgramBuilder.boolNativeIsNull.add(
                     translator.builder,
                     isNullReg,
                     translator.implementNode(operand));
                 translator.builder.addLabelJumpFalse(wasNotNull, isNullReg);
                 translator.builder.addLabelJump(next);
                 translator.builder.addLabel(wasNotNull);
                 CalcProgramBuilder.nativeMinus.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     input);
                 translator.builder.addLabel(next);
             } else {
                 CalcProgramBuilder.nativeMinus.add(
                     translator.builder,
                     accumulatorRegister,
                     accumulatorRegister,
                     input);
             }
         }
     }
 
     /**
      * Implementation of the <code>$HISTOGRAM</code> aggregate function
      * ({@link SqlStdOperatorTable#histogramAggFunction), which
      * helps implement MIN, MAX, FIRST_VALUE, LAST_VALUE in a windowed
      * aggregation scenario.
      *
      * @see HistogramResultRexImplementor
      */
     private static class HistogramAggRexImplementor
         extends AbstractCalcRexAggImplementor
     {
         public HistogramAggRexImplementor()
         {
         }
 
         public void implementInitialize(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             assert call.operands.length == 1;
             CalcReg reg0 = translator.implementNode(call.operands[0]);
             ExtInstructionDefTable.histogramInit.add(
                 translator.builder,
                 new CalcReg[] { accumulatorRegister, reg0 });
         }
 
         public void implementAdd(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             assert call.operands.length == 1;
             final CalcReg reg0 = translator.implementNode(call.operands[0]);
             ExtInstructionDefTable.histogramAdd.add(
                 translator.builder,
                 new CalcReg[] { reg0, accumulatorRegister });
         }
 
         public void implementDrop(
             RexCall call,
             CalcReg accumulatorRegister,
             RexToCalcTranslator translator)
         {
             assert call.operands.length == 1;
             final CalcReg reg0 = translator.implementNode(call.operands[0]);
             ExtInstructionDefTable.histogramDrop.add(
                 translator.builder,
                 new CalcReg[] { reg0, accumulatorRegister });
         }
     }
 
     /**
      * Implementation of the operators which extract a result from a histogram:
      * <ul>
      * <li>{@link SqlStdOperatorTable#histogramMinFunction $HISTOGRAM_MIN},
      * <li>{@link SqlStdOperatorTable#histogramMaxFunction $HISTOGRAM_MAX},
      * <li>{@link SqlStdOperatorTable#histogramFirstValueFunction $HISTOGRAM_FIRST_VALUE},
      * <li>{@link SqlStdOperatorTable#histogramLastValueFunction $HISTOGRAM_LAST_VALUE}
      * </ul>
      *
      * @see HistogramAggRexImplementor
      */
     private static class HistogramResultRexImplementor
         extends AbstractCalcRexImplementor
     {
         private final SqlAggFunction function;
 
         public HistogramResultRexImplementor(SqlAggFunction function)
         {
             this.function = function;
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             // The single argument must be a histogram.
             assert call.getOperands().length == 1;
 
             CalcReg resultReg = createResultRegister(translator, call);
 
             final RexNode operand = call.operands[0];
             CalcReg reg0 = translator.implementNode(operand);
 
             final CalcProgramBuilder.ExtInstrDef instrDef;
             if (function == SqlStdOperatorTable.minOperator) {
                 instrDef = ExtInstructionDefTable.histogramGetMin;
             } else if (function == SqlStdOperatorTable.maxOperator) {
                 instrDef = ExtInstructionDefTable.histogramGetMax;
             } else if (function == SqlStdOperatorTable.firstValueOperator) {
                 instrDef = ExtInstructionDefTable.histogramGetFirstValue;
             } else if (function == SqlStdOperatorTable.lastValueOperator) {
                 instrDef = ExtInstructionDefTable.histogramGetLastValue;
             } else {
                 throw Util.newInternal("invalid function " + function);
             }
             instrDef.add(translator.builder, resultReg, reg0);
 
             return resultReg;
         }
     }
 
     /**
      * Implements the internal {@link SqlStdOperatorTable#sliceOp $SLICE}
      * operator.
      */
     private static class SliceImplementor
         extends AbstractCalcRexImplementor
     {
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             throw new UnsupportedOperationException();
         }
     }
 
     private static class TimeFunctionImplementor
         extends AbstractCalcRexImplementor
     {
         private static Map<SqlOperator, CalcProgramBuilder.ExtInstrDef> timeFuncs;
 
         static {
             HashMap<SqlOperator, CalcProgramBuilder.ExtInstrDef> map =
                 new HashMap<SqlOperator, CalcProgramBuilder.ExtInstrDef>();
 
             map.put(
                 SqlStdOperatorTable.localTimeFunc,
                 ExtInstructionDefTable.localTime);
             map.put(
                 SqlStdOperatorTable.localTimestampFunc,
                 ExtInstructionDefTable.localTimestamp);
             map.put(
                 SqlStdOperatorTable.currentTimeFunc,
                 ExtInstructionDefTable.currentTime);
             map.put(
                 SqlStdOperatorTable.currentTimestampFunc,
                 ExtInstructionDefTable.currentTimestamp);
 
             timeFuncs = map;
         }
 
         public CalcReg implement(
             RexCall call,
             RexToCalcTranslator translator)
         {
             // precision or nothing
             assert (call.operands.length <= 1);
 
             final CalcProgramBuilder progBuilder = translator.builder;
 
             SqlOperator operator = call.getOperator();
 
             CalcProgramBuilder.ExtInstrDef instruction =
                 timeFuncs.get(operator);
 
             Util.permAssert(
                 instruction != null,
                 "cannot implement " + call.toString());
 
             RexNode operand = null;
             if (call.operands.length == 1) {
                 operand = translator.resolve(call.operands[0]);
             }
 
             ArrayList<CalcReg> regList = new ArrayList<CalcReg>();
 
             CalcReg timeReg =
                progBuilder.newStatus(CalcProgramBuilder.OpType.Int8, -1);
 
             // Call will be to store time func result in local reg with
             // optional precision operand.
             regList.add(timeReg);
 
             if (operand != null) {
                 regList.add(translator.implementNode(operand));
             }
 
             String notZeroLabel = translator.newLabel();
 
             CalcReg isNotZeroReg =
                 progBuilder.newLocal(CalcProgramBuilder.OpType.Bool, -1);
 
             CalcReg zeroReg = progBuilder.newInt8Literal(0);
 
             CalcProgramBuilder.boolNativeNotEqual.add(
                 progBuilder,
                 isNotZeroReg,
                 timeReg,
                 zeroReg);
 
             progBuilder.addLabelJumpTrue(notZeroLabel, isNotZeroReg);
 
             // store time func reuslt in local reg
             instruction.add(progBuilder, regList);
 
             // dangling label on whatever comes next
             progBuilder.addLabel(notZeroLabel);
 
             return timeReg;
         }
     }
 }
 
 // End CalcRexImplementorTableImpl.java
