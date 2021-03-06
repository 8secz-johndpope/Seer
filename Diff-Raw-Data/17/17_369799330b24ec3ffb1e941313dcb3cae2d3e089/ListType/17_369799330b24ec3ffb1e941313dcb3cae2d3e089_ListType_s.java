 /*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 
 *******************************************************************************/
 
 package org.eclipse.imp.pdb.facts.type;
 
 import java.util.Map;
 
 import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
 
 /*package*/ class ListType extends ValueType {
   protected final Type fEltType;
 	
 	/*package*/ ListType(Type eltType) {
 		fEltType = eltType;
 	}
 
 	@Override
 	public Type getElementType() {
 		return fEltType;
 	}
 	
 	@Override
 	public Type carrier() {
 		return TypeFactory.getInstance().setType(fEltType);
 	}
 	
 	@Override
 	public String toString() {
 	  if (fEltType.isFixedWidth()) {
 	        StringBuilder sb = new StringBuilder();
 	    sb.append("lrel[");
 	    int idx = 0;
	    for (Type elemType : fEltType.getFieldTypes()) {
 	      if (idx++ > 0)
 	        sb.append(",");
 	      sb.append(elemType.toString());
 	      if (hasFieldNames()) {
 	        sb.append(" " + fEltType.getFieldName(idx - 1));
 	      }
 	    }
 	    sb.append("]");
 	    return sb.toString();
 	  }
 	  else {
 	    return "list[" + fEltType + "]";
 	  }
 	}
 
 	@Override
 	public boolean equals(Object o) {
 		if(o == this) {
 			return true;
 		}
 		else if (o instanceof ListType) {
 			ListType other = (ListType) o;
 			return fEltType == other.fEltType;
 		}
 		
 		return false;
 	}
 	
 	@Override
 	public int hashCode() {
 		return 75703 + 104543 * fEltType.hashCode();
 	}
 	
 	@Override
 	public <T,E extends Exception> T accept(ITypeVisitor<T,E> visitor) throws E {
 		return visitor.visitList(this);
 	}
 	
 	@Override
 	public boolean isOpen() {
 	  return fEltType.isOpen();
 	}
 	
 	@Override
 	protected boolean isSupertypeOf(Type type) {
 	  return type.isSubtypeOfList(this);
 	}
 	
 	@Override
 	public Type lub(Type other) {
 	  return other.lubWithSet(this);
 	}
 	
 	@Override
 	public boolean match(Type matched, Map<Type, Type> bindings)
 			throws FactTypeUseException {
 		return super.match(matched, bindings)
 				&& getElementType().match(matched.getElementType(), bindings);
 	}
 	
 	@Override
 	public Type instantiate(Map<Type, Type> bindings) {
 		return TypeFactory.getInstance().listType(getElementType().instantiate(bindings));
 	}
 }
