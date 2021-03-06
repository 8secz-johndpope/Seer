 /******************************************************************************
  * Copyright (c) 2002 - 2006 IBM Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *****************************************************************************/
 package com.ibm.wala.cast.ir.ssa;
 
 import com.ibm.wala.shrikeBT.BinaryOpInstruction;
 import com.ibm.wala.shrikeBT.UnaryOpInstruction;
 
 public interface AstConstants  {
 
   public enum BinaryOp implements BinaryOpInstruction.IOperator {
     CONCAT, EQ, NE, LT, GE, GT, LE, STRICT_EQ, STRICT_NE;
 
     public String toString() {
       return super.toString().toLowerCase();
     }
   }
 
   public enum UnaryOp implements UnaryOpInstruction.IOperator {
    MINUS, BITNOT;
 
     public String toString() {
       return super.toString().toLowerCase();
     }
   }
 }
