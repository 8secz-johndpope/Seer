 /*******************************************************************************
  * Copyright (c) 2009-2011 CWI
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
 
  *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
  *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
  *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *******************************************************************************/
 package org.rascalmpl.interpreter.types;
 
 import org.eclipse.imp.pdb.facts.IConstructor;
 import org.eclipse.imp.pdb.facts.type.ExternalType;
 import org.eclipse.imp.pdb.facts.type.ITypeVisitor;
 import org.eclipse.imp.pdb.facts.type.Type;
 import org.rascalmpl.interpreter.asserts.ImplementationError;
 import org.rascalmpl.interpreter.utils.Symbols;
 import org.rascalmpl.values.uptr.Factory;
 import org.rascalmpl.values.uptr.ProductionAdapter;
 import org.rascalmpl.values.uptr.SymbolAdapter;
 import org.rascalmpl.values.uptr.TreeAdapter;
 
 /**
  * This is an "extension" of the PDB's type system with a special kind of type
  * that implements the mapping between SDF's sort names and Rascal types. These
  * types should never escape into the PDB, that would break a lot...
  */
 public class NonTerminalType extends ExternalType {
 	private IConstructor symbol;
 
 	/*package*/ NonTerminalType(IConstructor cons) {
 		if (cons.getType() == Factory.Symbol) {
 			this.symbol = cons;
 		}
 		else if (cons.getType() == Factory.Production) {
 			this.symbol = ProductionAdapter.getType(cons);
 		}
 		else if (cons.getConstructorType() == Factory.Tree_Appl) {
 			this.symbol = TreeAdapter.getType(cons);
 		}
 		else if (cons.getConstructorType() == Factory.Tree_Amb) {
 			IConstructor first = (IConstructor) TreeAdapter.getAlternatives(cons).iterator().next();
 			this.symbol = TreeAdapter.getType(first);
 		}
 		else {
 			throw new ImplementationError("Invalid concrete syntax type constructor");
 		}
 	}
 	
     /*package*/ NonTerminalType(org.rascalmpl.ast.Type type, boolean lex, String layout) {
 		this((IConstructor) Symbols.typeToSymbol(type, lex, layout));
 	}
 	
 	public IConstructor getSymbol() {
 		return symbol;
 	}
 	
 	@Override
 	public boolean isAbstractDataType() {
 		return true;
 	}
 	
 	@Override
 	public boolean isNodeType() {
 		return true;
 	}
 	
 	public boolean isConcreteListType() {
 		return SymbolAdapter.isAnyList(getSymbol());
 	}
 	
 	@Override
 	public String getName() {
 		return Factory.Tree.getName();
 	}
 	
 	@Override
 	public Type getTypeParameters() {
 		return Factory.Tree.getTypeParameters();
 	}
 	
 	@Override
 	public <T> T accept(ITypeVisitor<T> visitor) {
 		return visitor.visitExternal(this);
 	}
 	
 	@Override
 	public boolean isSubtypeOf(Type other) {
 		
 		if (other.equals(this)) {
 			return true;
 		}
 		
 		if (other == Factory.Tree) {
 			return true;
 		}
 		
 		if (other.isParameterType() && other.getBound().isSubtypeOf(Factory.Tree)) {
 			return true;
 		}
 		
 		if (other instanceof NonTerminalType) {
 			IConstructor otherSym = ((NonTerminalType)other).symbol;
 			if (SymbolAdapter.isPlusList(symbol) && SymbolAdapter.isStarList(otherSym)) {
 				return true; // TODO add check if they have the same element type
 			}
 			
			return otherSym.isEqual(symbol);
 		}
 		
 		if (other.isNodeType()) {
 			return true;
 		}
 		
 		if (other.isVoidType()) {
 			return false;
 		}
 		
 		if (other.isParameterType()) {
 			return isSubtypeOf(other.getBound());
 		}
 		
 		return super.isSubtypeOf(other);
 	}
 	
 	public boolean isConcreteCFList() {
 		return false;
 	}
 	
 	@Override
 	public Type lub(Type other) {
 		if (other.equals(this)) {
 			return this;
 		}
 		else if (other.isSubtypeOf(Factory.Tree)) {
 			return Factory.Tree;
 		}
 		return super.lub(other);
 	}
 	
 	@Override
 	public boolean equals(Object obj) {
 		if (obj.getClass() == getClass()) {
 			NonTerminalType other = (NonTerminalType) obj;
 			return symbol.equals(other.symbol);
 		}
 		
 		return false;
 	}
 	
 	@Override
 	public int hashCode() {
 		return symbol.hashCode();
 	}
 	
 	@Override
 	public String toString() {
 		return SymbolAdapter.toString(symbol);
 	}
 }
