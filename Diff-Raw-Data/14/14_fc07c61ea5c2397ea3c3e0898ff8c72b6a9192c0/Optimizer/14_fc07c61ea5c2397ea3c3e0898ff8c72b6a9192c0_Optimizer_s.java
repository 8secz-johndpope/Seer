 /*
  * This source file is part of CaesarJ 
  * For the latest info, see http://caesarj.org/
  * 
  * Copyright  2003-2005 
  * Darmstadt University of Technology, Software Technology Group
  * Also see acknowledgements in readme.txt
  * 
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  * 
 * $Id: Optimizer.java,v 1.3 2005-05-12 10:38:34 meffert Exp $
  */
 
 package org.caesarj.compiler.optimize;
 
 import java.util.Vector;
 
 import org.caesarj.classfile.AccessorContainer;
 import org.caesarj.classfile.AccessorTransformer;
 import org.caesarj.classfile.BadAccessorException;
 import org.caesarj.classfile.CodeInfo;
 import org.caesarj.classfile.HandlerInfo;
 import org.caesarj.classfile.Instruction;
 import org.caesarj.classfile.InstructionAccessor;
 import org.caesarj.classfile.LineNumberInfo;
 import org.caesarj.classfile.LocalVariableInfo;
 import org.caesarj.util.InconsistencyException;
 import org.caesarj.util.Utils;
 
 /**
  * This class is the entry point for the peephole byte code optimizer
  */
 public class Optimizer implements AccessorContainer {
 
   // --------------------------------------------------------------------
   // UTILITIES
   // --------------------------------------------------------------------
 
   /**
    * Optimizes the byte code for a single method.
    */
   public static CodeInfo optimize(CodeInfo code, int level) {
     Optimizer	opt = new Optimizer(code);
 
     // do work at specified level
     boolean	codeChanged = true;
 
    while (level-- > 0 && codeChanged) {
       codeChanged = opt.optimizeCodeSequence();
     }
 
     code = opt.getCodeInfo();
 
     return code;
   }
 
   // --------------------------------------------------------------------
   // CONSTRUCTORS
   // --------------------------------------------------------------------
 
   /**
    * Constructs a new optimizer object.
    */
   public Optimizer(CodeInfo codeInfo) {
     setCodeStart(installInstructionHandles(codeInfo));
     this.handlers = codeInfo.getHandlers();
     this.localVariables = codeInfo.getLocalVariables();
   }
 
   // --------------------------------------------------------------------
   // ACCESSORS
   // --------------------------------------------------------------------
 
   /**
    * Returns a new, optimized code info structure.
    */
   public CodeInfo getCodeInfo() {
     CodeInfo	codeInfo;
 
     codeInfo = new CodeInfo(buildInstructionArray(),
 			    handlers,
 			    buildLineNumberInfo(),
 			    buildLocalVariableInfo());
 
     // replace instruction handles by actual instructions
     try {
       AccessorTransformer	transformer = new AccessorTransformer() {
 	  public InstructionAccessor transform(InstructionAccessor accessor,
 					       AccessorContainer container)
 	  {
 	    // the only accessors to resolve are instruction handles
 	    return ((InstructionHandle)accessor).getInstruction();
 	  }
 	};
 
       codeInfo.transformAccessors(transformer);
     } catch (BadAccessorException e) {
       throw new InconsistencyException(e.getMessage());	//!!!!
     }
 
     return codeInfo;
   }
 
   // --------------------------------------------------------------------
   // OPTIMIZE
   // --------------------------------------------------------------------
 
   private void buildBasicBlocks(InstructionHandle start) {
     for (InstructionHandle handle = this.codeStart; handle != null; handle = handle.getNext()) {
       handle.reset();
     }
 
     for (int i = 0; i < handlers.length; i++) {
       // !!! if (start.isReachable())
       ((InstructionHandle)handlers[i].getHandler()).addAccessor(handlers[i]);
       ((InstructionHandle)handlers[i].getStart()).addAccessor(handlers[i]);
       // !!! WHY ??? graf 010111
       // ((InstructionHandle)handlers[i].getEnd()).addAccessor(handlers[i]);
       // !!! WHY ??? graf 010111
     }
     
     
     // add local variable infos as accessors to their start and end instructions in order
     // to be notified by changes.
     for (int j = 0; localVariables != null && j < localVariables.length; j++){
     	((InstructionHandle)localVariables[j].getEnd()).addAccessor(localVariables[j]);
         ((InstructionHandle)localVariables[j].getStart()).addAccessor(localVariables[j]);
     }
   }
 
 
   private boolean optimizeCodeSequence() {
     boolean	codeChanged = false;
 
     buildBasicBlocks(codeStart);
 
     for (InstructionHandle handle = codeStart; handle != null; handle = handle.getNext()) {
       codeChanged |= Patterns.optimize(handle);
     }
 
     codeChanged |= cleanCode();
 
     return codeChanged;
   }
 
   private boolean cleanCode() {
     boolean		codeRemoved = false;
 
     for (int i = 0; i < handlers.length; i++) {
       while (!((InstructionHandle)handlers[i].getEnd()).isReached()) {
 	handlers[i].setEnd(((InstructionHandle)handlers[i].getEnd()).getPrevious());
       }
 
       // !!!if (start > end) remove handler
     }
 
     InstructionHandle	current = codeStart;
 
     for (InstructionHandle handle = current.getNext(); handle != null; handle = handle.getNext()) {
       if (handle.isReached()) {
 	current.setNext(handle);
 	current = handle;
       } else {
 	current.setNext(null);
 	codeRemoved = true;
       }
 	  /* Andreas start
       handle.clean();
       */
       handle._clean();
       // Andreas end
     }
 
     if (current == codeStart) {
       codeStart.setNext(null);
     }
 
     return codeRemoved;
   }
 
   // --------------------------------------------------------------------
   // INSTALL WRAPPERS
   // --------------------------------------------------------------------
 
   /**
    * Install handles around instructions.
    */
   private InstructionHandle installInstructionHandles(CodeInfo info) {
     Instruction[]		insns = info.getInstructions();
 
     InstructionHandle[]		handles = new InstructionHandle[insns.length];
     for (int i = 0; i < handles.length; i++) {
       // this also sets the field next in handles
       handles[i] = new InstructionHandle(insns[i], i == 0 ? null : handles[i-1]);
     }
 
     try {
       info.transformAccessors(new HandleCreator(insns, handles));
     } catch (BadAccessorException e) {
       //!!!DEBUG
       dumpCode(insns);
       //!!!DEBUG
       throw new InconsistencyException(e.getMessage());	//!!!!
     }
 
     return handles[0];
   }
 
   private void dumpCode(Instruction[] insns) {
     for (int i = 0; i < insns.length; i++) {
       insns[i].dump();
     }
     System.err.flush();
   }
 
   private void dumpCode() {
     int		i = 0;
     for (InstructionHandle handle = this.codeStart; handle != null; handle = handle.getNext()) {
       System.err.println(i++ + ":\t" + org.caesarj.classfile.OpcodeNames.getName(handle.getInstruction().getOpcode()));
     }
     System.err.flush();
   }
 
   // --------------------------------------------------------------------
   // RECONSTRUCT A CodeInfo STRUCTURE
   // --------------------------------------------------------------------
 
   /**
    * Build the array of the instructions resulting from the optimization
    * process.
    *
    * @return	the array of instructions
    */
   private Instruction[] buildInstructionArray() {
     int		length;
 
     // count size of instruction array
     length = 0;
     for (InstructionHandle handle = this.codeStart; handle != null; handle = handle.getNext()) {
       length += 1;
     }
 
     Instruction[]	insns = new Instruction[length];
 
     length = 0;
     for (InstructionHandle handle = this.codeStart; handle != null; handle = handle.getNext()) {
       insns[length] = handle.getInstruction();
       length += 1;
     }
 
     return insns;
   }
 
   /**
    * Build the array of line number information for the optimized
    * instruction sequence.
    */
   private LineNumberInfo[] buildLineNumberInfo() {
     Vector	lineNumbers = new Vector();
 
     for (InstructionHandle handle = this.codeStart; handle != null; handle = handle.getNext()) {
       handle.addLineNumberInfo(lineNumbers);
     }
 
     return (LineNumberInfo[])Utils.toArray(lineNumbers, LineNumberInfo.class);
   }
   
   /**
    * Build the array of local variables information for the optimized
    * instruction sequence.
    */
   private LocalVariableInfo[] buildLocalVariableInfo(){
   	Vector tempLocalVars = new Vector();
   	for(int i = 0; localVariables != null && i < localVariables.length; i++){
   		// Only add informations for variables with valid start and end instructions.
   		if(localVariables[i].getStart() != null 
   				&& localVariables[i].getEnd() != null){
   			tempLocalVars.add(localVariables[i]);
   		}
   	}
   	return (LocalVariableInfo[])Utils.toArray(tempLocalVars, LocalVariableInfo.class);
   }
 
   /**
    * Transforms targets (deferences to actual instructions).
    */
   public void transformAccessors(AccessorTransformer transformer) throws BadAccessorException {
     this.codeStart = (InstructionHandle)this.codeStart.transform(transformer, this);
   }
 
   public void setCodeStart(InstructionHandle handle) {
     if (codeStart != null) {
       codeStart.removeAccessor(this);
     }
     codeStart = handle;
     codeStart.addAccessor(this);
   }
 
   public InstructionHandle getCodeStart() {
     return codeStart;
   }
 
   // --------------------------------------------------------------------
   // DATA MEMBERS
   // --------------------------------------------------------------------
 
   private InstructionHandle		codeStart;
   private HandlerInfo[]			handlers;
   private LocalVariableInfo[]		localVariables;
 }
