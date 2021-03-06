 /*******************************************************************************
  * Copyright (c) 2005, 2006 QNX Software Systems and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * QNX - Initial API and implementation
  * Markus Schorn (Wind River Systems)
  * Andrew Ferguson (Symbian)
  *******************************************************************************/
 
 package org.eclipse.cdt.internal.core.pdom.dom.cpp;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.cdt.core.CCorePlugin;
 import org.eclipse.cdt.core.dom.IPDOMNode;
 import org.eclipse.cdt.core.dom.IPDOMVisitor;
 import org.eclipse.cdt.core.dom.ast.DOMException;
 import org.eclipse.cdt.core.dom.ast.IASTName;
 import org.eclipse.cdt.core.dom.ast.IBinding;
 import org.eclipse.cdt.core.dom.ast.IField;
 import org.eclipse.cdt.core.dom.ast.IScope;
 import org.eclipse.cdt.core.dom.ast.IType;
 import org.eclipse.cdt.core.dom.ast.ITypedef;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPField;
 import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
 import org.eclipse.cdt.internal.core.Util;
 import org.eclipse.cdt.internal.core.dom.parser.ProblemBinding;
 import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPSemantics;
 import org.eclipse.cdt.internal.core.index.IIndexType;
 import org.eclipse.cdt.internal.core.pdom.PDOM;
 import org.eclipse.cdt.internal.core.pdom.db.PDOMNodeLinkedList;
 import org.eclipse.cdt.internal.core.pdom.dom.IPDOMMemberOwner;
 import org.eclipse.cdt.internal.core.pdom.dom.PDOMLinkage;
 import org.eclipse.cdt.internal.core.pdom.dom.PDOMNamedNode;
 import org.eclipse.cdt.internal.core.pdom.dom.PDOMNode;
 import org.eclipse.core.runtime.CoreException;
 
 /**
  * @author Doug Schaefer
  * 
  */
 /*
  * aftodo - contract get Methods/Fields not honoured?
  */
 class PDOMCPPClassType extends PDOMCPPBinding implements ICPPClassType,
 ICPPClassScope, IPDOMMemberOwner, IIndexType {
 
 	private static final int FIRSTBASE = PDOMCPPBinding.RECORD_SIZE + 0;
 	private static final int KEY = PDOMCPPBinding.RECORD_SIZE + 4; // byte
 	private static final int MEMBERLIST = PDOMCPPBinding.RECORD_SIZE + 8;
 
 	protected static final int RECORD_SIZE = PDOMCPPBinding.RECORD_SIZE + 12;
 
 	public PDOMCPPClassType(PDOM pdom, PDOMNode parent, ICPPClassType classType)
 			throws CoreException {
 		super(pdom, parent, classType.getName().toCharArray());
 
 		try {
 			pdom.getDB().putByte(record + KEY, (byte) classType.getKey());
 		} catch (DOMException e) {
 			throw new CoreException(Util.createStatus(e));
 		}
 		// linked list is initialized by storage being zero'd by malloc
 	}
 
 	public PDOMCPPClassType(PDOM pdom, int bindingRecord) {
 		super(pdom, bindingRecord);
 	}
 
 	public void addMember(PDOMNode member) throws CoreException {
 		PDOMNodeLinkedList list = new PDOMNodeLinkedList(pdom, record + MEMBERLIST, getLinkageImpl());
 		list.addMember(member);
 	}
 
 	protected int getRecordSize() {
 		return RECORD_SIZE;
 	}
 
 	public int getNodeType() {
 		return PDOMCPPLinkage.CPPCLASSTYPE;
 	}
 
 	public PDOMCPPBase getFirstBase() throws CoreException {
 		int rec = pdom.getDB().getInt(record + FIRSTBASE);
 		return rec != 0 ? new PDOMCPPBase(pdom, rec) : null;
 	}
 
 	private void setFirstBase(PDOMCPPBase base) throws CoreException {
 		int rec = base != null ? base.getRecord() : 0;
 		pdom.getDB().putInt(record + FIRSTBASE, rec);
 	}
 
 	public void addBase(PDOMCPPBase base) throws CoreException {
 		PDOMCPPBase firstBase = getFirstBase();
 		base.setNextBase(firstBase);
 		setFirstBase(base);
 	}
 
 	public boolean isSameType(IType type) {
 		if (type instanceof PDOMNode) {
 			PDOMNode node= (PDOMNode) type;
 			if (node.getPDOM() == getPDOM()) {
 				return node.getRecord() == getRecord();
 			}
 		}
 
 		if (type instanceof ITypedef) {
 			return type.isSameType(this);
 		}
 		
 		if (type instanceof ICPPClassType && !(type instanceof ProblemBinding)) {
 			ICPPClassType ctype= (ICPPClassType) type;
 			try {
 				if (ctype.getKey() == getKey()) {
 					char[][] qname= ctype.getQualifiedNameCharArray();
 					return hasQualifiedName(qname, qname.length-1);
 				}
 			} catch (DOMException e) {
 				CCorePlugin.log(e);
 			}
 		}
 		return false;
 	}
 
 	public ICPPBase[] getBases() throws DOMException {
 		try {
 			List list = new ArrayList();
 			for (PDOMCPPBase base = getFirstBase(); base != null; base = base.getNextBase())
 				list.add(base);
 			Collections.reverse(list);
 			ICPPBase[] bases = (ICPPBase[])list.toArray(new ICPPBase[list.size()]);
 			return bases;
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 			return new ICPPBase[0];
 		}
 	}
 
 	public void accept(IPDOMVisitor visitor) throws CoreException {
 		super.accept(visitor);
 		PDOMNodeLinkedList list = new PDOMNodeLinkedList(pdom, record + MEMBERLIST, getLinkageImpl());
 		list.accept(visitor);
 	}
 
 	private static class MethodCollector implements IPDOMVisitor {
 		private final List methods;
 		private final boolean acceptImplicit;
 		private final boolean acceptAll;
 		public MethodCollector(List methods, boolean acceptImplicit) {
 			this(methods, acceptImplicit, true);
 		}
 		public MethodCollector(List methods, boolean acceptImplicit, boolean acceptExplicit) {
 			this.methods = methods == null ? new ArrayList() : methods;
 			this.acceptImplicit= acceptImplicit;
 			this.acceptAll= acceptImplicit && acceptExplicit;
 		}
 		public boolean visit(IPDOMNode node) throws CoreException {
 			if (node instanceof ICPPMethod) {
 				if (acceptAll || ((ICPPMethod) node).isImplicit() == acceptImplicit) {
 					methods.add(node);
 				}
 			}
 			return false; // don't visit the method
 		}
 		public void leave(IPDOMNode node) throws CoreException {
 		}
 		public ICPPMethod[] getMethods() {
 			return (ICPPMethod[])methods.toArray(new ICPPMethod[methods.size()]); 
 		}
 	}
 
 	public ICPPMethod[] getDeclaredMethods() throws DOMException {
 		try {
 			MethodCollector methods = new MethodCollector(null, false);
 			accept(methods);
 			return methods.getMethods();
 		} catch (CoreException e) {
 			return new ICPPMethod[0];
 		}
 	}
 
 	public ICPPMethod[] getMethods() throws DOMException {
 		try {
 			MethodCollector methods = new MethodCollector(null, true);
 			accept(methods);
 			return methods.getMethods();
 		} catch (CoreException e) {
 			return new ICPPMethod[0];
 		}
 	}
 
 	public ICPPMethod[] getImplicitMethods() {
 		try {
 			MethodCollector methods = new MethodCollector(null, true, false);
 			accept(methods);
 			return methods.getMethods();
 		} catch (CoreException e) {
 			return new ICPPMethod[0];
 		}
 	}
 
 	private void visitAllDeclaredMethods(Set visited, List methods) throws CoreException {
 		if (visited.contains(this))
 			return;
 		visited.add(this);
 
 		// Get my members
 		MethodCollector myMethods = new MethodCollector(methods, false, true);
 		accept(myMethods);
 
 		// Visit my base classes
 		for (PDOMCPPBase base = getFirstBase(); base != null; base = base.getNextBase()) {
 			try {
 				IBinding baseClass = base.getBaseClass();
 				if (baseClass != null && baseClass instanceof PDOMCPPClassType)
 					((PDOMCPPClassType)baseClass).visitAllDeclaredMethods(visited, methods);
 			} catch (DOMException e) {
 				throw new CoreException(Util.createStatus(e));
 			}
 		}
 	}
 
 	public ICPPMethod[] getAllDeclaredMethods() throws DOMException {
 		List methods = new ArrayList();
 		Set visited = new HashSet();
 		try {
 			visitAllDeclaredMethods(visited, methods);
 			return (ICPPMethod[])methods.toArray(new ICPPMethod[methods.size()]);
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 			return new ICPPMethod[0];
 		}
 	}
 	
 	private static class FieldCollector implements IPDOMVisitor {
 		private List fields = new ArrayList();
 		public boolean visit(IPDOMNode node) throws CoreException {
 			if (node instanceof IField)
 				fields.add(node);
 			return false;
 		}
 		public void leave(IPDOMNode node) throws CoreException {
 		}
 		public IField[] getFields() {
 			return (IField[])fields.toArray(new IField[fields.size()]);
 		}
 	}
 
 	public IField[] getFields() throws DOMException {
 		try {
 			FieldCollector visitor = new FieldCollector();
 			accept(visitor);
 			return visitor.getFields();
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 			return new IField[0];
 		}
 	}
 
 	private static class NestedClassCollector implements IPDOMVisitor {
 		private List nestedClasses = new ArrayList();
 		public boolean visit(IPDOMNode node) throws CoreException {
 			if (node instanceof ICPPClassType)
 				nestedClasses.add(node);
 			return false;
 		}
 		public void leave(IPDOMNode node) throws CoreException {
 		}
 		public ICPPClassType[] getNestedClasses() {
 			return (ICPPClassType[])nestedClasses.toArray(new ICPPClassType[nestedClasses.size()]);
 		}
 	}
 
 	public ICPPClassType[] getNestedClasses() throws DOMException {
 		try {
 			NestedClassCollector visitor = new NestedClassCollector();
 			accept(visitor);
 			return visitor.getNestedClasses();
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 			return new ICPPClassType[0];
 		}
 	}
 
 	public IScope getCompositeScope() throws DOMException {
 		return this;
 	}
 
 	public int getKey() throws DOMException {
 		try {
 			return pdom.getDB().getByte(record + KEY);
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 			return ICPPClassType.k_class; // or something
 		}
 	}
 
 	public boolean isGloballyQualified() throws DOMException {
 		try {
 			return getParentNode() instanceof PDOMLinkage;
 		} catch (CoreException e) {
 			return true;
 		}
 	}
 
 	public ICPPClassType getClassType() {
 		return this;
 	}
 
 	public void addChild(PDOMNode member) throws CoreException {
 		addMember(member);
 	}
 
 	private static class ConstructorCollector implements IPDOMVisitor {
 		private List fConstructors = new ArrayList();
 		public boolean visit(IPDOMNode node) throws CoreException {
 			if (node instanceof ICPPConstructor)
 				fConstructors.add(node);
 			return false;
 		}
 		public void leave(IPDOMNode node) throws CoreException {
 		}
 		public ICPPConstructor[] getConstructors() {
 			return (ICPPConstructor[])fConstructors.toArray(new ICPPConstructor[fConstructors.size()]);
 		}
 	}
 
 	public ICPPConstructor[] getConstructors() throws DOMException {
 		ConstructorCollector visitor= new ConstructorCollector();
 		try {
 			accept(visitor);
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 		}
 		return visitor.getConstructors();
 	}
 
 	
 	public boolean isFullyCached()  {
 		return true;
 	}
 
 	private static class BindingCollector implements IPDOMVisitor {
 		private List fBindings = new ArrayList();
 		private char[] fName;
 		
 		public BindingCollector(char[] name) {
 			fName= name;
 		}
 		
 		public boolean visit(IPDOMNode node) throws CoreException {
 			if (node instanceof PDOMNamedNode && node instanceof IBinding) {
 				PDOMNamedNode nn= (PDOMNamedNode) node;
 				if (nn.getDBName().equals(fName)) {
 					fBindings.add(node);
 				}
 			}
 			return false;
 		}
 		public void leave(IPDOMNode node) throws CoreException {
 		}
 		public IBinding[] getBindings() {
 			return (IBinding[])fBindings.toArray(new IBinding[fBindings.size()]);
 		}
 	}
 
 	public IBinding getBinding(IASTName name, boolean resolve) throws DOMException {
 		try {
 			BindingCollector visitor= new BindingCollector(name.toCharArray());
 			accept(visitor);
 			return CPPSemantics.resolveAmbiguities(name, visitor.getBindings());
 		} catch (CoreException e) {
 			CCorePlugin.log(e);
 		}
 		return null;
 	}
 
 	// Not implemented
 
 	public Object clone() {fail();return null;}
 	public IField findField(String name) throws DOMException {fail();return null;}
 	public IBinding[] getFriends() throws DOMException {fail();return null;}
 	public IBinding[] find(String name) throws DOMException {fail();return null;}
 	public ICPPField[] getDeclaredFields() throws DOMException {fail();return null;}
 
 	public IScope getParent() throws DOMException {
 		try {
 			IBinding parent = getParentBinding();
 			if(parent instanceof IScope) {
 				return (IScope) parent;
 			}
 		} catch(CoreException ce) {
 			CCorePlugin.log(ce);
 		}
 
 		return null;
 	}
 
 	public boolean mayHaveChildren() {
 		return true;
 	}
 }
