 /*
  * Copyright (c) 2009, IETR/INSA of Rennes
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
  *   * Neither the name of the IETR/INSA of Rennes nor the names of its
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
 package net.sf.orcc.backends.c;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import net.sf.orcc.common.Use;
 import net.sf.orcc.common.Variable;
 import net.sf.orcc.ir.NameTransformer;
 import net.sf.orcc.ir.nodes.AbstractFifoNode;
 
 /**
  * 
  * @author Matthieu Wipliez
  * 
  */
 public class VarDefPrinter {
 
 	private ListSizePrinter listSizePrinter;
 
 	private TypeToString typeVisitor;
 
 	public VarDefPrinter(TypeToString typeVisitor) {
 		this.listSizePrinter = new ListSizePrinter();
 		this.typeVisitor = typeVisitor;
 	}
 
 	/**
 	 * Returns an instance of the "vardef" template with attributes set using
 	 * the given VarDef varDef.
 	 * 
 	 * @param varDef
 	 *            a variable definition
 	 * @return a string template
 	 */
 	public Map<String, Object> applyVarDef(Variable varDef) {
 		Map<String, Object> varDefMap = new HashMap<String, Object>();
 		varDefMap.put("name", getVarDefName(varDef));
 		varDefMap.put("type", typeVisitor.toString(varDef.getType()));
 
 		// if varDef is a list, => list of dimensions
 		varDef.getType().accept(listSizePrinter);
 
 		varDefMap.put("size", listSizePrinter.getSize());
 		boolean isPort = false;
 		for (Use use : varDef.getUses()) {
 			if (use.getNode() instanceof AbstractFifoNode) {
 				AbstractFifoNode fifoNode = (AbstractFifoNode) use.getNode();
				if (varDef.getName().startsWith(fifoNode.getPort().getName())) {
 					isPort = true;
 					break;
 				}
 			}
 		}
 		varDefMap.put("isPort", isPort);
 
 		return varDefMap;
 	}
 
 	/**
 	 * Returns the full name of the given variable definition, with index and
 	 * suffix.
 	 * 
 	 * @param variable
 	 *            the variable definition
 	 * @return a string with its full name
 	 */
 	public String getVarDefName(Variable variable) {
		return NameTransformer.transform(variable.getName());
 	}
 
 }
