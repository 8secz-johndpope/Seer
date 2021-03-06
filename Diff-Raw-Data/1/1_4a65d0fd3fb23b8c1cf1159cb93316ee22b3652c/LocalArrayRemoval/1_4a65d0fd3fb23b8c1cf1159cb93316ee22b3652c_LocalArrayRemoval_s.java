 /*
  * Copyright (c) 2011, IRISA
  * All rights reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  * 
  *   * Redistributions of source code must retain the above copyright notice,
  *     this list of conditions and the following disclaimer.
  *   * Redistributions in binary form must reproduce the above copyright notice,
  *     this list of conditions and the following disclaimer in the documentation
  *     and/or other materials provided with the distribution.
  *   * Neither the name of the IRISA nor the names of its
  *     contributors may be used to endorse or promote products derived from this
  *     software without specific prior written permission.
  * 
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
  * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  */
 package net.sf.orcc.backends.xlim.transformations;
 
 import java.util.ArrayList;
 
 import net.sf.orcc.ir.Actor;
 import net.sf.orcc.ir.IrFactory;
 import net.sf.orcc.ir.Procedure;
 import net.sf.orcc.ir.Use;
 import net.sf.orcc.ir.Var;
 import net.sf.orcc.ir.util.AbstractActorVisitor;
 import net.sf.orcc.ir.util.EcoreHelper;
 
 import org.eclipse.emf.common.util.EList;
 
 /**
  * Replace local arrays by global ones.
  * 
  * @author Herve Yviquel
  * 
  */
 public class LocalArrayRemoval extends AbstractActorVisitor<Object> {
 
 	@Override
 	public Object caseProcedure(Procedure procedure) {
 		Actor actor = EcoreHelper.getContainerOfType(procedure, Actor.class);
 		EList<Var> stateVars = actor.getStateVars();
 		for (Var var : new ArrayList<Var>(procedure.getLocals())) {
 			if (var.getType().isList()) {
 				Var newVar = IrFactory.eINSTANCE.createVar(var.getType(),
 						var.getName(), true, var.getIndex());
 				newVar.setInitialValue(var.getInitialValue());
 				EList<Use> uses = var.getUses();
 				while (!uses.isEmpty()) {
 					uses.get(0).setVariable(newVar);
 				}
 				EcoreHelper.delete(var);
 				stateVars.add(newVar);
 			}
 		}
 		return null;
 	}
 }
