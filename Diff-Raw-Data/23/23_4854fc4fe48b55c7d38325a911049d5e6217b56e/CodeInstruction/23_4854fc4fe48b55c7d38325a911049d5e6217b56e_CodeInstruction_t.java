 /*
 This file is part of jpcsp.
 
 Jpcsp is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Jpcsp is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
  */
 package jpcsp.Allegrex.compiler;
 
 import org.objectweb.asm.Label;
 import org.objectweb.asm.MethodVisitor;
 import org.objectweb.asm.Opcodes;
 
 import jpcsp.Emulator;
 import jpcsp.Allegrex.Instructions;
 import jpcsp.Allegrex.Common.Instruction;
 
 /**
  * @author gid15
  *
  */
 public class CodeInstruction {
 	private int address;
 	private int opcode;
 	private Instruction insn;
 	private boolean isBranchTarget;
 	private int branchingTo;
 	private boolean isBranching;
 	private Label label;
 
     protected CodeInstruction() {
     }
 
     public CodeInstruction(int address, int opcode, Instruction insn, boolean isBranchTarget, boolean isBranching, int branchingTo) {
 		this.address = address;
 		this.opcode = opcode;
 		this.insn = insn;
 		this.isBranchTarget = isBranchTarget;
 		this.isBranching = isBranching;
 		this.branchingTo = branchingTo;
 	}
 
 	public int getAddress() {
 		return address;
 	}
 
 	public void setAddress(int address) {
 		this.address = address;
 	}
 
 	public int getOpcode() {
 		return opcode;
 	}
 
 	public void setOpcode(int opcode) {
 		this.opcode = opcode;
 	}
 
 	public Instruction getInsn() {
 		return insn;
 	}
 
 	public void setInsn(Instruction insn) {
 		this.insn = insn;
 	}
 
 	public boolean isBranchTarget() {
 		return isBranchTarget;
 	}
 
 	public void setBranchTarget(boolean isBranchTarget) {
 		this.isBranchTarget = isBranchTarget;
 	}
 
 	public boolean isBranching() {
 		return isBranching;
 	}
 
     public void setBranching(boolean isBranching) {
         this.isBranching = isBranching;
     }
 
     public int getBranchingTo() {
 		return branchingTo;
 	}
 
 	public void setBranchingTo(int branchingTo) {
 		this.branchingTo = branchingTo;
 	}
 
     public Label getLabel(boolean isBranchTarget) {
         if (label == null) {
             label = new Label();
             if (isBranchTarget) {
             	setBranchTarget(true);
             }
         }
 
         return label;
     }
 
     public Label getLabel() {
     	return getLabel(true);
     }
 
     public boolean hasLabel() {
     	return label != null;
     }
 
     public void forceNewLabel() {
     	label = new Label();
     }
 
     private void setLabel(Label label) {
     	this.label = label;
     }
 
     protected void startCompile(CompilerContext context, MethodVisitor mv) {
         if (Compiler.log.isDebugEnabled()) {
             Compiler.log.debug("CodeInstruction.compile " + toString());
         }
 
         context.setCodeInstruction(this);
 
         context.beforeInstruction(this);
 
         if (hasLabel()) {
             mv.visitLabel(getLabel());
         }
 
         context.startInstruction(this);
     }
 
     public void compile(CompilerContext context, MethodVisitor mv) {
         startCompile(context, mv);
 
         if (context.isNativeCodeSequence()) {
         	context.compileNativeCodeSequence();
         } else if (isBranching()) {
 	        compileBranch(context, mv);
 	    } else if (insn == Instructions.JR) {
 	        compileJr(context, mv);
         } else if (insn == Instructions.JALR) {
             compileJalr(context, mv);
 //	    } else if (" ADD ADDU ADDI ADDIU AND ANDI NOR OR ORI XOR XORI SLL SLLV SRA SRAV SRL SRLV ROTR ROTRV SLT SLTI SLTU SLTIU SUB SUBU LUI SEB BITREV WSBH WSBW MOVZ MOVN MAX MIN LW SB SW ".indexOf(" " + insn.name() + " ") >= 0) {
 //    		context.compileInterpreterInstruction();
 	    } else {
 		    insn.compile(context, getOpcode());
 	    }
 
         context.endInstruction();
     }
 
     private void compileJr(CompilerContext context, MethodVisitor mv) {
         compileDelaySlot(context, mv);
         context.loadRs();
         context.visitJump();
     }
 
     private void compileJalr(CompilerContext context, MethodVisitor mv) {
         compileDelaySlot(context, mv);
         context.loadRs();
         context.visitCall(getAddress() + 8, context.getRdRegisterIndex());
     }
 
     private void compileBranch(CompilerContext context, MethodVisitor mv) {
         int branchingOpcode = getBranchingOpcode(context, mv);
 
         if (branchingOpcode != Opcodes.NOP) {
             CodeInstruction branchingToCodeInstruction = context.getCodeBlock().getCodeInstruction(getBranchingTo());
             if (branchingToCodeInstruction != null) {
                 context.visitJump(branchingOpcode, branchingToCodeInstruction);
             } else {
                 context.visitJump(branchingOpcode, getBranchingTo());
             }
         }
     }
 
     private CodeInstruction getAfterDelaySlotCodeInstruction(CompilerContext context) {
         return context.getCodeBlock().getCodeInstruction(getAddress() + 8);
     }
 
     private CodeInstruction getDelaySlotCodeInstruction(CompilerContext context) {
         return context.getCodeBlock().getCodeInstruction(getAddress() + 4);
     }
 
     private void compileDelaySlot(CompilerContext context, MethodVisitor mv) {
         CodeInstruction delaySlotCodeInstruction = getDelaySlotCodeInstruction(context);
         if (delaySlotCodeInstruction == null) {
             Compiler.log.error("Cannot find delay slot instruction at 0x" + Integer.toHexString(getAddress() + 4));
             return;
         }
 
         Label delaySlotLabel = null;
         if (delaySlotCodeInstruction.hasLabel())
         {
         	delaySlotLabel = delaySlotCodeInstruction.getLabel();
         	delaySlotCodeInstruction.forceNewLabel();
         }
         delaySlotCodeInstruction.compile(context, mv);
         if (delaySlotLabel != null) {
         	delaySlotCodeInstruction.setLabel(delaySlotLabel);
         } else if (delaySlotCodeInstruction.hasLabel()) {
         	delaySlotCodeInstruction.forceNewLabel();
         }
         context.setCodeInstruction(this);
         context.skipInstructions(1, false);
     }
 
     private int getBranchingOpcodeBranch0(CompilerContext context, MethodVisitor mv) {
         compileDelaySlot(context, mv);
 
         if (getBranchingTo() == getAddress()) {
     		context.visitLogInfo(mv, "Pausing emulator - jump to self (death loop)");
     		context.visitPauseEmuWithStatus(mv, Emulator.EMU_STATUS_JUMPSELF);
     	}
 
         return Opcodes.GOTO;
     }
 
     private int getBranchingOpcodeCall0(CompilerContext context, MethodVisitor mv) {
         compileDelaySlot(context, mv);
         context.visitCall(getBranchingTo(), getAddress() + 8, 31, false);
 
         return Opcodes.NOP;
     }
 
     private int getBranchingOpcodeBranch1(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
         context.loadRs();
         compileDelaySlot(context, mv);
 
         return branchingOpcode;
     }
 
     private int getBranchingOpcodeBranch1L(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadRs();
         CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
         context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
         compileDelaySlot(context, mv);
 
         return Opcodes.GOTO;
     }
 
     private int getBranchingOpcodeCall1(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadRs();
         compileDelaySlot(context, mv);
         CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
         context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
         context.visitCall(getBranchingTo(), getAddress() + 8, 31, true);
 
         return Opcodes.NOP;
     }
 
     private int getBranchingOpcodeCall1L(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadRs();
         CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
         context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
         compileDelaySlot(context, mv);
         context.visitCall(getBranchingTo(), getAddress() + 8, 31, true);
 
         return Opcodes.NOP;
     }
 
     private int loadRegistersForBranchingOpcodeBranch2(CompilerContext context, MethodVisitor mv, int branchingOpcode) {
     	boolean loadRs = true;
     	boolean loadRt = true;
 
     	if (context.getRsRegisterIndex() == context.getRtRegisterIndex()) {
     		if (branchingOpcode == Opcodes.IF_ICMPEQ) {
     			loadRs = false;
     			loadRt = false;
     			if (false) {
     				branchingOpcode = Opcodes.GOTO;
     			} else {
     				// The ASM library has problems with small frames having no
     				// stack (NullPointerException). Generate a dummy stack requirement.
     				// "IFEQ 0" is equivalent to GOTO
     				context.loadImm(0);
     				branchingOpcode = Opcodes.IFEQ;
     			}
     		} else if (branchingOpcode == Opcodes.IF_ICMPNE) {
     			loadRs = false;
     			loadRt = false;
     			branchingOpcode = Opcodes.NOP;
     		}
     	} else if (context.isRsRegister0()) {
     		if (branchingOpcode == Opcodes.IF_ICMPEQ) {
     			loadRs = false;
     			branchingOpcode = Opcodes.IFEQ;
     		} else if (branchingOpcode == Opcodes.IF_ICMPNE) {
     			loadRs = false;
     			branchingOpcode = Opcodes.IFNE;
     		}
     	} else if (context.isRtRegister0()) {
     		if (branchingOpcode == Opcodes.IF_ICMPEQ) {
     			loadRt = false;
     			branchingOpcode = Opcodes.IFEQ;
     		} else if (branchingOpcode == Opcodes.IF_ICMPNE) {
     			loadRt = false;
     			branchingOpcode = Opcodes.IFNE;
     		}
     	}
 
     	if (loadRs) {
     		context.loadRs();
     	}
     	if (loadRt) {
     		context.loadRt();
     	}
 
     	return branchingOpcode;
     }
 
     private int getBranchingOpcodeBranch2(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	branchingOpcode = loadRegistersForBranchingOpcodeBranch2(context, mv, branchingOpcode);
 		compileDelaySlot(context, mv);
 
     	if (branchingOpcode == Opcodes.GOTO && getBranchingTo() == getAddress()) {
     		context.visitLogInfo(mv, "Pausing emulator - branch to self (death loop)");
     		context.visitPauseEmuWithStatus(mv, Emulator.EMU_STATUS_JUMPSELF);
     	}
 
         return branchingOpcode;
     }
 
     private int getBranchingOpcodeBranch2L(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	notBranchingOpcode = loadRegistersForBranchingOpcodeBranch2(context, mv, notBranchingOpcode);
     	if (notBranchingOpcode != Opcodes.NOP) {
     		CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
     		context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
     	}
         compileDelaySlot(context, mv);
 
         return Opcodes.GOTO;
     }
 
     private int getBranchingOpcodeBC1(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadFcr31c();
         compileDelaySlot(context, mv);
 
         return branchingOpcode;
     }
 
     private int getBranchingOpcodeBC1L(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadFcr31c();
         CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
         context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
         compileDelaySlot(context, mv);
 
         return Opcodes.GOTO;
     }
 
     private int getBranchingOpcodeBV(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadVcrCc();

    	CodeInstruction delaySlotCodeInstruction = getDelaySlotCodeInstruction(context);
        if (delaySlotCodeInstruction != null && delaySlotCodeInstruction.insn == insn) {
        	// We are compiling a sequence where the delay instruction is again a BV
        	// instruction:
        	//    bvt 0, label
        	//    bvt 1, label
        	//    bvt 2, label
        	//    bvt 3, label
        	//    nop
        	// Handle the sequence by inserting nop's between the BV instructions:
        	//    bvt 0, label
        	//    nop
        	//    bvt 1, label
        	//    nop
        	//    bvt 2, label
        	//    nop
        	//    bvt 3, label
        	//    nop
        } else {
        	compileDelaySlot(context, mv);
        }
 
         return branchingOpcode;
     }
 
     private int getBranchingOpcodeBVL(CompilerContext context, MethodVisitor mv, int branchingOpcode, int notBranchingOpcode) {
     	context.loadVcrCc();
         CodeInstruction afterDelaySlotCodeInstruction = getAfterDelaySlotCodeInstruction(context);
         context.visitJump(notBranchingOpcode, afterDelaySlotCodeInstruction);
         compileDelaySlot(context, mv);
 
         return Opcodes.GOTO;
     }
 
     private int getBranchingOpcode(CompilerContext context, MethodVisitor mv) {
         int branchingOpcode = Opcodes.NOP;
 
         if (insn == Instructions.BEQ) {
             branchingOpcode = getBranchingOpcodeBranch2(context, mv, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE);
         } else if (insn == Instructions.BEQL) {
             branchingOpcode = getBranchingOpcodeBranch2L(context, mv, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE);
         } else if (insn == Instructions.BNE) {
             branchingOpcode = getBranchingOpcodeBranch2(context, mv, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPEQ);
         } else if (insn == Instructions.BNEL) {
             branchingOpcode = getBranchingOpcodeBranch2L(context, mv, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPEQ);
         } else if (insn == Instructions.BGEZ) {
             branchingOpcode = getBranchingOpcodeBranch1(context, mv, Opcodes.IFGE, Opcodes.IFLT);
         } else if (insn == Instructions.BGEZL) {
             branchingOpcode = getBranchingOpcodeBranch1L(context, mv, Opcodes.IFGE, Opcodes.IFLT);
         } else if (insn == Instructions.BGTZ) {
             branchingOpcode = getBranchingOpcodeBranch1(context, mv, Opcodes.IFGT, Opcodes.IFLE);
         } else if (insn == Instructions.BGTZL) {
             branchingOpcode = getBranchingOpcodeBranch1L(context, mv, Opcodes.IFGT, Opcodes.IFLE);
         } else if (insn == Instructions.BLEZ) {
             branchingOpcode = getBranchingOpcodeBranch1(context, mv, Opcodes.IFLE, Opcodes.IFGT);
         } else if (insn == Instructions.BLEZL) {
             branchingOpcode = getBranchingOpcodeBranch1L(context, mv, Opcodes.IFLE, Opcodes.IFGT);
         } else if (insn == Instructions.BLTZ) {
             branchingOpcode = getBranchingOpcodeBranch1(context, mv, Opcodes.IFLT, Opcodes.IFGE);
         } else if (insn == Instructions.BLTZL) {
             branchingOpcode = getBranchingOpcodeBranch1L(context, mv, Opcodes.IFLT, Opcodes.IFGE);
         } else if (insn == Instructions.J) {
             branchingOpcode = getBranchingOpcodeBranch0(context, mv);
         } else if (insn == Instructions.JAL) {
             branchingOpcode = getBranchingOpcodeCall0(context, mv);
         } else if (insn == Instructions.BLTZAL) {
             branchingOpcode = getBranchingOpcodeCall1(context, mv, Opcodes.IFLT, Opcodes.IFGE);
         } else if (insn == Instructions.BLTZALL) {
             branchingOpcode = getBranchingOpcodeCall1L(context, mv, Opcodes.IFLT, Opcodes.IFGE);
         } else if (insn == Instructions.BGEZAL) {
             branchingOpcode = getBranchingOpcodeCall1(context, mv, Opcodes.IFGE, Opcodes.IFLT);
         } else if (insn == Instructions.BGEZALL) {
             branchingOpcode = getBranchingOpcodeCall1L(context, mv, Opcodes.IFGE, Opcodes.IFLT);
         } else if (insn == Instructions.BC1F) {
             branchingOpcode = getBranchingOpcodeBC1(context, mv, Opcodes.IFEQ, Opcodes.IFNE);
         } else if (insn == Instructions.BC1FL) {
             branchingOpcode = getBranchingOpcodeBC1L(context, mv, Opcodes.IFEQ, Opcodes.IFNE);
         } else if (insn == Instructions.BC1T) {
             branchingOpcode = getBranchingOpcodeBC1(context, mv, Opcodes.IFNE, Opcodes.IFEQ);
         } else if (insn == Instructions.BC1TL) {
             branchingOpcode = getBranchingOpcodeBC1L(context, mv, Opcodes.IFNE, Opcodes.IFEQ);
         } else if (insn == Instructions.BVF) {
             branchingOpcode = getBranchingOpcodeBV(context, mv, Opcodes.IFEQ, Opcodes.IFNE);
         } else if (insn == Instructions.BVT) {
             branchingOpcode = getBranchingOpcodeBV(context, mv, Opcodes.IFNE, Opcodes.IFEQ);
         } else if (insn == Instructions.BVFL) {
             branchingOpcode = getBranchingOpcodeBVL(context, mv, Opcodes.IFEQ, Opcodes.IFNE);
         } else if (insn == Instructions.BVTL) {
             branchingOpcode = getBranchingOpcodeBVL(context, mv, Opcodes.IFNE, Opcodes.IFEQ);
         } else if (insn == Instructions.BC1F || insn == Instructions.BC1FL ||
                    insn == Instructions.BC1T || insn == Instructions.BC1TL ) {
             Compiler.log.error("Unimplemented Instruction " + insn.disasm(getAddress(), getOpcode()));
         } else {
             Compiler.log.error("CodeInstruction.getBranchingOpcode: unknown instruction " + insn.disasm(getAddress(), getOpcode()));
         }
 
         return branchingOpcode;
     }
 
     public boolean hasFlags(int flags) {
         return getInsn().hasFlags(flags);
     }
 
     public String toString() {
     	StringBuilder result = new StringBuilder();
 
     	result.append(isBranching()    ? "<" : " ");
     	result.append(isBranchTarget() ? ">" : " ");
     	result.append(" 0x");
     	result.append(Integer.toHexString(getAddress()).toUpperCase());
     	result.append(" - ");
     	result.append(getInsn().disasm(getAddress(), getOpcode()));
 
     	return result.toString();
     }
 }
