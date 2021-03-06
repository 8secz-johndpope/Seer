 /*
 Copyright (c) 2011, Rockwell Collins.
 Developed with the sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 
 Permission is hereby granted, free of charge, to any person obtaining a copy of this data, 
 including any software or models in source or binary form, as well as any drawings, specifications, 
 and documentation (collectively "the Data"), to deal in the Data without restriction, including
 without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 and/or sell copies of the Data, and to permit persons to whom the Data is furnished to do so, 
 subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in all copies or 
 substantial portions of the Data.
 
 THE DATA IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS, SPONSORS, DEVELOPERS, CONTRIBUTORS, OR COPYRIGHT HOLDERS BE LIABLE 
 FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 ARISING FROM, OUT OF OR IN CONNECTION WITH THE DATA OR THE USE OR OTHER DEALINGS IN THE DATA.
 */
 
 /**
  * <copyright>
  * Copyright  2013 by Carnegie Mellon University, all rights reserved.
  * 
  * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
  * at http://www.eclipse.org/org/documents/epl-v10.html.
  * 
  * NO WARRANTY
  * 
  * ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
  * CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE ''DELIVERABLES'') ARE ON AN ''AS-IS'' BASIS.
  * CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED AS TO ANY MATTER INCLUDING,
  * BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABILITY, INFORMATIONAL CONTENT,
  * NONINFRINGEMENT, OR ERROR-FREE OPERATION. CARNEGIE MELLON UNIVERSITY SHALL NOT BE LIABLE FOR INDIRECT, SPECIAL OR
  * CONSEQUENTIAL DAMAGES, SUCH AS LOSS OF PROFITS OR INABILITY TO USE SAID INTELLECTUAL PROPERTY, UNDER THIS LICENSE,
  * REGARDLESS OF WHETHER SUCH PARTY WAS AWARE OF THE POSSIBILITY OF SUCH DAMAGES. LICENSEE AGREES THAT IT WILL NOT
  * MAKE ANY WARRANTY ON BEHALF OF CARNEGIE MELLON UNIVERSITY, EXPRESS OR IMPLIED, TO ANY PERSON CONCERNING THE
  * APPLICATION OF OR THE RESULTS TO BE OBTAINED WITH THE DELIVERABLES UNDER THIS LICENSE.
  * 
  * Licensee hereby agrees to defend, indemnify, and hold harmless Carnegie Mellon University, its trustees, officers,
  * employees, and agents from all claims or demands made against them (and any related losses, expenses, or
  * attorney's fees) arising out of, or relating to Licensee's and/or its sub licensees' negligent use or willful
  * misuse of or negligent conduct or willful misconduct regarding the Software, facilities, or other rights or
  * assistance granted by Carnegie Mellon University under this License, including, but not limited to, any claims of
  * product liability, personal injury, death, damage to property, or violation of any laws or regulations.
  * 
  * Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
  * of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
  * under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
  * documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
  * under the contract clause at 252.227.7013.
  * </copyright>
  * 
  */
 
 package org.osate.analysis.lute.language;
 
 import java.math.BigInteger;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 
 import org.osate.aadl2.BooleanLiteral;
 import org.osate.aadl2.ComponentCategory;
 import org.osate.aadl2.DirectionType;
 import org.osate.aadl2.Element;
 import org.osate.aadl2.EnumerationLiteral;
 import org.osate.aadl2.IntegerLiteral;
 import org.osate.aadl2.ListValue;
 import org.osate.aadl2.NamedElement;
 import org.osate.aadl2.NamedValue;
 import org.osate.aadl2.Property;
 import org.osate.aadl2.PropertyExpression;
 import org.osate.aadl2.RangeValue;
 import org.osate.aadl2.ReferenceValue;
 import org.osate.aadl2.StringLiteral;
 import org.osate.aadl2.impl.ContainmentPathElementImpl;
 import org.osate.aadl2.impl.NamedValueImpl;
 import org.osate.aadl2.instance.ComponentInstance;
 import org.osate.aadl2.instance.ConnectionInstance;
 import org.osate.aadl2.instance.FeatureInstance;
 import org.osate.aadl2.instance.InstanceObject;
 import org.osate.aadl2.instance.InstanceReferenceValue;
 import org.osate.aadl2.instance.SystemInstance;
 import org.osate.aadl2.properties.PropertyLookupException;
 import org.osate.aadl2.util.OsateDebug;
 import org.osate.analysis.lute.LuteException;
 import org.osate.xtext.aadl2.properties.util.EMFIndexRetrieval;
 import org.osate.xtext.aadl2.properties.util.PropertyUtils;
 
 public class FnCallExpr extends Expr {
 	final private String fn;
 	final private List<Expr> args;
 	private Environment evaluationEnvironment = null;
 	
 	public List<Expr> getArgs ()
 	{
 		return this.args;
 	}
 
 	public FnCallExpr(String fn, List<Expr> args) {
 		super();
 		this.fn = fn;
 		this.args = args;
 	}
 
 	private static ComponentInstance lookUp ( ComponentInstance ci, String refName)
 	{
 		ComponentInstance ret;
 		ret = null;
 		for (Element e : ci.getChildren())
 		{
 			if (e instanceof ComponentInstance)
 			{
 				ComponentInstance ne = (ComponentInstance) e;
 				if (ne.getName().equalsIgnoreCase(refName))
 				{
 					ret = ne;
 				}
 			}
 		}
 		return ret;
 		
 	}
 	
 	@Override
 	public Val eval(Environment env) {
 		ArrayList<Val> argValues = new ArrayList<Val>();
 		evaluationEnvironment = env;
 		for (Expr arg : args) {
 			argValues.add(arg.eval(env));
 		}
 		
 		if (fn.equals("Property")) {
 			Val result;
 			String property;
 						
 			expectArgs(2);
 
 			property = argValues.get(1).getString();
 			result = null;
 
 			
 			if (argValues.get(0) instanceof SetVal)
 			{
 				ArrayList<Val> list = new ArrayList<Val>();
 
 				SetVal set = (SetVal) argValues.get(0);
 				Iterator<Val> iter= set.getSet().iterator();
 				
 				while (iter.hasNext())
 				{
 					Val val = iter.next();
 					result = getProperty(val.getAADL(), property);
 					//OsateDebug.osateDebug("result=" + result);
 
 					list.add(result);
 				}
 				result = new SetVal (list);
 			}
 			else
 			{
 				InstanceObject aadl = argValues.get(0).getAADL();
 				result = getProperty(aadl, property);
 				//OsateDebug.osateDebug("result" + result);
 				if (result == null) { 
 					return (new StringVal (""));
 				//	throw new LuteException("Failed to find property " + property);
 				}
 			}
 			return result;
 			
 		} else if (fn.equals("Property_Exists")) {
 			expectArgs(2);
 			InstanceObject aadl = argValues.get(0).getAADL();
 			String property = argValues.get(1).getString();
 			Val result = getProperty(aadl, property);
 			return new BoolVal(result != null);
 			
 		}else if (fn.equals("Is_Of_Type")) {
 			expectArgs(2);
 			InstanceObject aadl = argValues.get(0).getAADL();
 			String typeString = argValues.get(1).getString();
 			return new BoolVal(checkType(aadl, typeString));
 			
 		}
 		
 		else if (fn.equals("Is_Bound_To")) {
 			expectArgs(2);
 
 			InstanceObject s = argValues.get(0).getAADL();
 			InstanceObject t = argValues.get(1).getAADL();
 			return new BoolVal(isBoundTo(s, t));
 			
 		} else if (fn.equalsIgnoreCase("source")) {
 			expectArgs(1);
 			InstanceObject c = argValues.get(0).getAADL();
 			if (c instanceof ConnectionInstance) {
 				ConnectionInstance conn = (ConnectionInstance) c;
 				return new AADLVal(conn.getSource());
 			}
 			throw new LuteException("Source called on non-connection object");
 			
 		} else if (fn.equalsIgnoreCase("destination")) {
 			expectArgs(1);
 			InstanceObject c = argValues.get(0).getAADL();
 			if (c instanceof ConnectionInstance) {
 				ConnectionInstance conn = (ConnectionInstance) c;
 				return new AADLVal(conn.getDestination());
 			}
 
 			throw new LuteException("Destination called on non-connection object");
 			
 		} else if (fn.equalsIgnoreCase("has_out_ports")) {
 			expectArgs(1);
 			InstanceObject c = argValues.get(0).getAADL();
 			if (c instanceof ComponentInstance) {
 				int fis = 0;
 				ComponentInstance comp = (ComponentInstance) c;
 				for (FeatureInstance fi : comp.getFeatureInstances())
 				{
 					if ((fi.getDirection() == DirectionType.OUT) ||
 						(fi.getDirection() == DirectionType.IN_OUT) )
 					{
 						fis++;
 					}
 
 				}
 				return new BoolVal(fis != 0);
 			}
 
 			throw new LuteException("has_out_ports called on non-connection object");
 			
 		} else if (fn.equalsIgnoreCase("has_in_ports")) {
 			expectArgs(1);
 			InstanceObject c = argValues.get(0).getAADL();
 			if (c instanceof ComponentInstance) {
 				int fis = 0;
 				ComponentInstance comp = (ComponentInstance) c;
 				for (FeatureInstance fi : comp.getFeatureInstances())
 				{
 					if ((fi.getDirection() == DirectionType.IN) ||
 						(fi.getDirection() == DirectionType.IN_OUT) )
 					{
 						fis++;
 					}
 
 				}
 				return new BoolVal(fis != 0);
 			}
 
 			throw new LuteException("has_in_ports called on non-connection object");
 			
 		} else if (fn.equals("Member")) {
 			expectArgs(2);
 			Val e = argValues.get(0);
 
 			Collection<Val> set = argValues.get(1).getSet();
 			return new BoolVal(set.contains(e));
 			
 		} else if (fn.equals("Owner")) {
 			expectArgs(1);
 			InstanceObject e = argValues.get(0).getAADL();
 			if (e.getOwner() instanceof InstanceObject) {
 				InstanceObject owner = (InstanceObject) e.getOwner();
 				return new AADLVal(owner);
 			}
 			throw new LuteException("Owner called on un-owned object");
 			
 		} else if (fn.equals("Is_Subcomponent_Of")) {
 			/*
 			 * Is_Subcomponent_Of looks if the component is
 			 * contained in the whole hierarchy and browse the whole
 			 * component tree, trying to look for a parent.
 			 * If you do not want to navigate through the component
 			 * hierarchy, use the Is_Direct_Subcomponent_Of
 			 * function instead.
 			 */
 			expectArgs(2);
 			InstanceObject sub = argValues.get(0).getAADL();
 
 			InstanceObject top = argValues.get(1).getAADL();
 			while (sub.getOwner() instanceof InstanceObject) {
 				InstanceObject owner = (InstanceObject) sub.getOwner();
 				if (top.equals(owner)) {
 					return new BoolVal(true);
 				}
 				sub = owner;
 			}
 			return new BoolVal(false);
 			
 		} 
 		else if (fn.equals("Is_Direct_Subcomponent_Of")) 
 		{
 			/*
 			 * Is_Direct_Subcomponent_Of look if the component
 			 * is directly a subcomponent of the other and does
 			 * not look at the entire component hierarchy.
 			 */
 			expectArgs(2);
 			InstanceObject sub = argValues.get(0).getAADL();
 
 			InstanceObject top = argValues.get(1).getAADL();
 			if (sub.getOwner() instanceof InstanceObject) {
 				InstanceObject owner = (InstanceObject) sub.getOwner();
 				if (top.equals(owner)) {
 					return new BoolVal(true);
 				}
 			}
 			return new BoolVal(false);
 		}
 		else if (fn.equals("Sum")) {
 			BigInteger retInt = new BigInteger("0");
 			//OsateDebug.osateDebug("argval" + argValues.get(0));
 			SetVal sv = (SetVal) argValues.get(0);
 			Iterator<Val> iter = sv.getSet().iterator();
 			while (iter.hasNext())
 			{
 				Val tmp = iter.next();
 
 				//OsateDebug.osateDebug("bla1=" + tmp);
 				//OsateDebug.osateDebug("bla2=" + tmp.getInt());
 				retInt = retInt.add(tmp.getInt());
 			}
 			
 			return new IntVal(retInt);
 		}
 		else if (fn.equals("Max")) {
 			if (argValues.size() == 1) {
 				Val arg = argValues.get(0);
 				if (arg instanceof SetVal) {
 					SetVal set = (SetVal) arg;
 					return max(set.getSet());
 				} else {
 					return arg;
 				}
 			} else {
 				return max(argValues);
 			}
 			
 		} else if (fn.equals("Min")) {
 			if (argValues.size() == 1) {
 				Val arg = argValues.get(0);
 				if (arg instanceof SetVal) {
 					SetVal set = (SetVal) arg;
 					return min(set.getSet());
 				} else {
 					return arg;
 				}
 			} else {
 				return min(argValues);
 			}
 			
 		} else if (fn.equals("Cardinal")) 
 		{
 			expectArgs(1);
 			Collection<Val> set = argValues.get(0).getSet();
 			return new IntVal(set.size());
 			
 		} else if (fn.equals("Lower")) {
 			expectArgs(1);
 			RangeVal range = argValues.get(0).getRange();
 			return range.getLower();
 			
 		} else if (fn.equals("Upper")) {
 			expectArgs(1);
 			RangeVal range = argValues.get(0).getRange();
 			return range.getUpper();
 			
 		} else if (fn.equals("=")) {
 			expectArgs(2);
 			Val left = argValues.get(0);
 			Val right = argValues.get(1);
 			return new BoolVal(left.equals(right));
 			
 		} else if (fn.equals("!=")) {
 			expectArgs(2);
 			Val left = argValues.get(0);
 			Val right = argValues.get(1);
 			return new BoolVal(!left.equals(right));
 			
 		} else if (fn.equals(">")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new BoolVal(left.compareTo(right) > 0);
 			
 		} else if (fn.equals("<")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			//OsateDebug.osateDebug("left=" + left);
 			//OsateDebug.osateDebug("right=" + right);
 
 			return new BoolVal(left.compareTo(right) < 0);
 			
 		} else if (fn.equals(">=")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new BoolVal(left.compareTo(right) >= 0);
 			
 		} else if (fn.equals("<=")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new BoolVal(left.compareTo(right) <= 0);
 			
 		} else if (fn.equals("+")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new IntVal(left.add(right));
 			
 		} else if (fn.equals("-")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new IntVal(left.subtract(right));
 			
 		} else if (fn.equals("*")) {
 			expectArgs(2);
 			BigInteger left = argValues.get(0).getInt();
 			BigInteger right = argValues.get(1).getInt();
 			return new IntVal(left.multiply(right));
 			
 		} else if (fn.equals("Hex")) {
 			expectArgs(1);
 			BigInteger i = argValues.get(0).getInt();
 			return new StringVal("16#" + i.toString(16) + "#");
 			
 		} else {
 			FunctionDefinition fd = env.lookupFn(fn);
 			return fd.eval(argValues);
 		}
 	}
 	
 	public String toString ()
 	{
 		String res = "";
 		res += fn + " with (";
 		for (int i = 0 ; i < args.size() ; i++)
 		{	
 			res += " " + args.get(i);
 			if (i < args.size() - 1)
 			{
 				res += " and ";
 			}
 		}
 		res += ")";
 		return res;
 	}
 	
 	
 	private IntVal max(Collection<Val> vals) {
 		if (vals.isEmpty()) {
 			throw new LuteException("Max called with no arguments");
 		}
 		BigInteger r = vals.iterator().next().getInt();
 		for (Val v : vals) {
 			if (v.getInt().compareTo(r) > 0) {
 				r = v.getInt();
 			}
 		}
 		return new IntVal(r);
 	}
 	
 	private IntVal min(Collection<Val> vals) {
 		if (vals.isEmpty()) {
 			throw new LuteException("Min called with no arguments");
 		}
 		BigInteger r = vals.iterator().next().getInt();
 		for (Val v : vals) {
 			if (v.getInt().compareTo(r) < 0) {
 				r = v.getInt();
 			}
 		}
 		return new IntVal(r);
 	}
 
 	private void expectArgs(int n) {
 		if (!(args.size() == n)) {
 			throw new LuteException("Function " + fn + " expects " + n + " arguments");
 		}
 	}
 	
 	private boolean checkType(InstanceObject aadl, String typeName)
 	{
 		boolean r;
 		//OsateDebug.osateDebug("aadl comp" + aadl.getComponentInstance().getComponentClassifier());
 		//OsateDebug.osateDebug("type=" + typeName);
 		r = aadl.getComponentInstance().getComponentClassifier().getName().toLowerCase().contains(typeName.toLowerCase());
 		//OsateDebug.osateDebug("return " + r);
 		return r;
 	}
 	
 	private Val getProperty(InstanceObject aadl, String propName) 
 	{
 		PropertyExpression expr;
 		Property property = EMFIndexRetrieval.getPropertyDefinitionInWorkspace(aadl,propName);
 		if (property == null)
 		{
 			return null;
 		}
 		try {
 			if (! property.isList())
 			{
 				expr = PropertyUtils.getSimplePropertyValue(aadl, property);
 			}
 			else
 			{
 				expr = PropertyUtils.getSimplePropertyListValue(aadl, property);
 			}
 			Val res = AADLPropertyValueToValue(expr);
 			return res;
 		} catch (PropertyLookupException e)
 		{
 			e.printStackTrace();
 			return (new StringVal (""));
 		}
 	}
 
 	private Val AADLPropertyValueToValue(PropertyExpression expr) {
 		//OsateDebug.osateDebug("expr=" + expr);
 		if (expr == null) {
 			return null;
 		} else if (expr instanceof BooleanLiteral) {
 			BooleanLiteral lit = (BooleanLiteral) expr;
 			return new BoolVal(lit.getValue());
 		} else if (expr instanceof StringLiteral) {
 			StringLiteral lit = (StringLiteral) expr;
 			return new StringVal(lit.getValue());
 		} else if (expr instanceof IntegerLiteral) {
 			IntegerLiteral lit = (IntegerLiteral) expr;
 			// FIXME: JD
 			// the getScaledValue method can raise some issues
 			// when using size.
 			//return new IntVal((long) lit.getScaledValue());
 
 			return new IntVal((long) lit.getValue());
 		} else if (expr instanceof RangeValue) {
 			RangeValue range = (RangeValue) expr;
 			return new RangeVal(
 	 			AADLPropertyValueToValue(range.getMinimumValue()),
 				AADLPropertyValueToValue(range.getMaximumValue()),
 				AADLPropertyValueToValue(range.getDelta())
 			);
 		} else if (expr instanceof InstanceReferenceValue) {
 			InstanceReferenceValue irv = (InstanceReferenceValue) expr;
 			return new AADLVal(irv.getReferencedInstanceObject());
 		}
 		else if (expr instanceof NamedValue) {
 			NamedValueImpl nv = (NamedValueImpl)expr;
 
 			if (nv.getNamedValue() instanceof EnumerationLiteral)
 			{
 				EnumerationLiteral el = (EnumerationLiteral) nv.getNamedValue();
 				Val res = new StringVal (el.getName().toLowerCase());
 				return res;
 			}
 			throw new LuteException("NamedValue not implemented now " + nv );
 		}
 		else if (expr instanceof ListValue) {
 			ListValue lv = (ListValue) expr;
 			ArrayList<Val> list = new ArrayList<Val>();
 			for (PropertyExpression pe : lv.getOwnedListElements()) {
 				list.add(AADLPropertyValueToValue(pe));
 			}
 			return new SetVal(list);
 		} 
 		else if (expr instanceof ReferenceValue) {
 			String refName;
 			ComponentInstance ref;
 			
 			ref = null;
 			
 			ReferenceValue rv = (ReferenceValue) expr;
 			
 			ContainmentPathElementImpl cpei = (ContainmentPathElementImpl)rv.getChildren().get(0);
 			NamedElement ne = cpei.getNamedElement();
 
 			refName = ne.getName();
 			Element e = cpei.getOwner();
 			while ((e != null) && (! ( e instanceof SystemInstance)))
 			{
 				if (e instanceof ComponentInstance)
 				{
 					ComponentInstance ci = (ComponentInstance)e;
 
 					ref = lookUp (ci, refName);
 					break;
 				}
 				e = e.getOwner();
 			}
 
 			return new AADLVal(ref);
 		}else 
 		{
 
 			throw new LuteException("Unknown AADL property value " + expr + " ("+expr.getOwner()+")on " + expr.getContainingClassifier());
 		}
 	}
 	
 	private boolean isBoundTo(InstanceObject s, InstanceObject t) {
 		if (t instanceof ComponentInstance) {
 			ComponentInstance platform = (ComponentInstance) t;
 			if (platform.getCategory() == ComponentCategory.PROCESSOR) {
 				return checkBinding(s, "actual_processor_binding", t);
 			} else if (platform.getCategory() == ComponentCategory.VIRTUAL_PROCESSOR) {
 				return checkBinding(s, "actual_processor_binding", t);
 			} else if (platform.getCategory() == ComponentCategory.MEMORY) {
 				return checkBinding(s, "actual_memory_binding", t);
 			} else if (platform.getCategory() == ComponentCategory.BUS) {
 				return checkBinding(s, "actual_connection_binding", t);
 			}
 		}
 		throw new LuteException("Invalid arguments to is_bound_to");
 	}
 
 	private boolean checkBinding(InstanceObject s, String bindingPropertyName, InstanceObject t) {
 		Property bindingProperty = EMFIndexRetrieval.getPropertyDefinitionInWorkspace(s,bindingPropertyName);
 		PropertyExpression bindings = PropertyUtils.getSimplePropertyValue(s, bindingProperty);
 		if (bindings instanceof ListValue) {
 			ListValue list = (ListValue) bindings;
 			for (PropertyExpression binding : list.getOwnedListElements()) {
 				if (binding instanceof InstanceReferenceValue) {
 					InstanceReferenceValue irv = (InstanceReferenceValue) binding;
 					if (t.equals(irv.getReferencedInstanceObject())) {
 						return true;
 					}
 				}
 			}
 		}
 		return false;
 	}
 	
 	public List<InstanceObject> getRelatedComponents ()
 	{
 		ArrayList<InstanceObject> ret = new ArrayList<InstanceObject>();
 		for (Expr e : args)
 		{
 			ret.addAll(e.getRelatedComponents());
 			if(evaluationEnvironment != null)
 			{
 				ret.addAll(e.eval(evaluationEnvironment).getRelatedComponents());
 			}
 		}
 		return ret;
 	}
 }
