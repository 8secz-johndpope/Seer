 /*
  *  This file is part of the X10 project (http://x10-lang.org).
  *
  *  This file is licensed to You under the Eclipse Public License (EPL);
  *  You may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *      http://www.opensource.org/licenses/eclipse-1.0.php
  *
  *  (C) Copyright IBM Corporation 2006-2010.
  */
 
 package x10cpp.visit;
 
 import static x10cpp.visit.ASTQuery.getCppBoxRep;
 import static x10cpp.visit.ASTQuery.getCppRep;
 import static x10cpp.visit.SharedVarsMethods.CONSTRUCTOR;
 import static x10cpp.visit.SharedVarsMethods.DESERIALIZATION_BUFFER;
 import static x10cpp.visit.SharedVarsMethods.DESERIALIZER_METHOD;
 import static x10cpp.visit.SharedVarsMethods.DESERIALIZE_BODY_METHOD;
 import static x10cpp.visit.SharedVarsMethods.DESERIALIZE_METHOD;
 import static x10cpp.visit.SharedVarsMethods.SAVED_THIS;
 import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_BUFFER;
 import static x10cpp.visit.SharedVarsMethods.SERIALIZATION_ID_FIELD;
 import static x10cpp.visit.SharedVarsMethods.SERIALIZE_BODY_METHOD;
 import static x10cpp.visit.SharedVarsMethods.SERIALIZE_ID_METHOD;
 import static x10cpp.visit.SharedVarsMethods.SERIALIZE_METHOD;
 import static x10cpp.visit.SharedVarsMethods.THIS;
 import static x10cpp.visit.SharedVarsMethods.chevrons;
 import static x10cpp.visit.SharedVarsMethods.make_ref;
 import static x10cpp.visit.SharedVarsMethods.make_captured_lval;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import polyglot.ast.Call_c;
 import polyglot.ast.ConstructorDecl_c;
 import polyglot.ast.Expr;
 import polyglot.ast.FieldDecl_c;
 import polyglot.ast.Formal;
 import polyglot.ast.Formal_c;
 import polyglot.ast.LocalDecl_c;
 import polyglot.ast.Local_c;
 import polyglot.ast.New_c;
 import polyglot.ast.Node;
 import polyglot.ast.Receiver;
 import polyglot.ast.TypeNode;
 import polyglot.types.ClassDef;
 import polyglot.types.ClassType;
 import polyglot.types.Context;
 import polyglot.types.FieldInstance;
 import polyglot.types.Flags;
 
 import polyglot.types.Context_c;
 import polyglot.types.Def;
 import polyglot.types.Name;
 import polyglot.types.QName;
 import polyglot.types.Ref;
 import polyglot.types.SemanticException;
 import polyglot.types.Type;
 import polyglot.types.Types;
 import polyglot.types.VarDef;
 import polyglot.types.VarInstance;
 import polyglot.util.CodeWriter;
 import polyglot.util.CollectionUtil; import x10.util.CollectionFactory;
 import polyglot.util.ErrorInfo;
 import polyglot.util.InternalCompilerError;
 import polyglot.util.Position;
 import polyglot.visit.Translator;
 import x10.ast.X10ClassDecl_c;
 import x10.ast.X10MethodDecl_c;
 import x10.ast.X10Special;
 import x10.ast.X10Special_c;
 import x10.constraint.XField;
 import x10.constraint.XLit;
 import x10.constraint.XVar;
 import x10.errors.Warnings;
 import x10.extension.X10Ext;
 import x10.types.ClosureDef;
 import x10.types.ConstrainedType;
 import x10.types.FunctionType;
 import x10.types.ParameterType;
 import x10.types.X10ClassDef;
 import x10.types.X10ClassType;
 import x10.types.X10ConstructorDef;
 import x10.types.X10Context_c;
 import x10.types.X10FieldDef;
 
 import x10.types.X10LocalDef;
 import x10.types.X10MethodDef;
 import x10.types.MethodInstance;
 import x10.types.constraints.CConstraint;
 import x10.types.constraints.CField;
 import polyglot.types.TypeSystem;
 import polyglot.types.TypeSystem_c.BaseTypeEquals;
 import x10.visit.StaticNestedClassRemover;
 import x10.util.ClassifiedStream;
 import x10.util.StreamWrapper;
 import x10cpp.X10CPPCompilerOptions;
 import x10cpp.debug.LineNumberMap;
 import x10cpp.types.X10CPPContext_c;
 
 public class Emitter {
 
     private final Translator tr;
     private ASTQuery query;
     public Emitter(Translator tr) {
         this.tr = tr;
         query = new ASTQuery(tr);
     }
     private static final String[] CPP_KEYWORDS = { // Some are also X10 keywords
         "asm", "auto", "bool", "break", "case", "catch", "char", "class",
         "const", "const_cast", "continue", "default", "delete", "do", "double",
         "dynamic_cast", "else", "enum", "explicit", "export", "extern",
         "false", "float", "for", "friend", "goto", "if", "inline", "int",
         "long", "mutable", "namespace", "new", "operator", "private",
         "protected", "public", "register", "reinterpret_cast", "return",
         "restrict", // Yes, stupid xlC has a "restrict" keyword -- who knew?
         "short", "signed", "sizeof", "static", "static_cast", "struct",
         "switch", "template", "this", "throw", "true", "try", "typedef",
         "typeid", "typename", "union", "unsigned", "using", "virtual", "void",
         "volatile", "wchar_t", "while",
         // operator names
         "and", "and_eq", "not", "not_eq", "or", "or_eq", "xor", "xor_eq",
         "bitand", "bitor", "compl",
         // X10 types
         "x10_boolean", "x10_byte", "x10_char", "x10_short", "x10_int",
         "x10_long", "x10_float", "x10_double",
         // X10 implementation names
         "FMGL", "TYPENAME", "getRTT", "rtt", "RTT_H_DECLS", "RTT_CC_DECLS1",
        // macros defined by the C++ implementation
        "i386",
         // Additionally, anything starting with a '_' is reserved, and may clash
     };
     private static boolean isCPPKeyword(String name) {
         for (int i = 0; i < CPP_KEYWORDS.length; i++) {
             if (CPP_KEYWORDS[i].equals(name))
                 return true;
         }
         return false;
     }
     private static String mangle_to_cpp(String str) {
         str = x10.emitter.Emitter.mangleIdentifier(str);
         if (isCPPKeyword(str))
             str = "_kwd__" + str;
         return str.replace("$", "__");
     }
     public static String mangled_method_name(String str) {
         return mangle_to_cpp(str);
     }
     public static String mangled_non_method_name(String str) {
         return mangle_to_cpp(str);
     }
     public static String mangled_field_name(String str) {
         //return "__"+mangle_to_cpp(str);
         //return "x10__"+mangle_to_cpp(str);
         return "FMGL("+mangle_to_cpp(str)+")";
     }
     public static String mangled_parameter_type_name(String str) {
         return "FMGL("+mangle_to_cpp(str)+")";
     }
 
 
 	/**
 	 * Same as n.printBlock(child, w, tr); but without the enterScope call.
 	 */
 	void printBlock(Node n, Expr child, StreamWrapper w, Translator tr) {
 		w.begin(0);
 		child.del().translate(w, tr);
 		w.end();
 	}
 
 	/**
 	 * Translate the string representation of a fully-qualified type name.
 	 * TODO: rewrite to use QName instead
 	 */
 	public static String translateFQN(String name) {
 		return translateFQN(name, "::");
 	}
 	public static String translateFQN(String name, String delim) {
 		return name.replace("$","__").replaceAll("\\.", delim);
 	}
 	public static String translate_mangled_FQN(String name) {
 		return translate_mangled_FQN(name, "::");
 	}
 	public static String translate_mangled_FQN(String name, String delim) {
 		String src = name;
 		String dest = "";
 		int fromIndex = 0;
 		while (true) {
 			boolean finished=false;
 			int toIndex = src.indexOf('.', fromIndex);
 			if (toIndex < 0){
 				finished=true;
 				toIndex=src.length();
 			}
 			String s = src.substring(fromIndex, toIndex);
 			dest += mangled_non_method_name(s);
 			if (finished) break;
 			dest += ".";
 			fromIndex = toIndex+1;
 		}
 
 		return translateFQN(dest, delim);
 	}
 	public static String translate_mangled_NSFQN(String name) { // namespace FQN
 		String src = name;
 		String dest = "";
 		int fromIndex = 0;
 		while (true) {
 			int toIndex = src.indexOf('.', fromIndex);
 			if (fromIndex != 0 && toIndex >= 0)
 				dest += ".";
 			if (toIndex < 0)
 				break;
 			String s = src.substring(fromIndex, toIndex);
 			dest += mangled_non_method_name(s);
 			fromIndex = toIndex+1;
 		}
 
 		return translateFQN(dest);
 	}
 
 	/**
 	 * Print a type as a reference.
 	 */
 	void printType(Type type, CodeWriter w) {
 		printType(type,w,null);
 	}	
 	void printType(Type type, CodeWriter w, X10Context_c ctx) {
 		w.write(translateType(type, true, ctx));
 	}
 
 	/**
 	 * Translate a type name.
 	 */
 	public static String translateType(Type type) {
 		return translateType(type, false);
 	}
 
 	/**
 	 * Return the full name of the type, taking into account nested class mangling.
 	 * @param ct the type
 	 * @return the full name of the type
 	 */
 	public static QName fullName(X10ClassType ct) {
 	    QName full;
 	    if (ct.def().isNested()) {
 	        // This is a legitimate case.  We do not invoke StaticNestedClassRemover on
 	        // classes for which we don't generate code.  Thus, while we cannot see a
 	        // definition of a nested class, we may well see references to such classes.
 	        // At the moment, we cannot make sure that StaticNestedClassRemover runs on
 	        // all classes - when we can, we should re-enable this assert.
 	        Name mangled = StaticNestedClassRemover.mangleName(ct.def());
 	        QName pkg = ct.package_() != null ? ct.package_().fullName() : null;
 	        full = QName.make(pkg, mangled);
 	    } else
 	        full = ct.fullName();
 	    return full;
 	}
 
 	private static HashMap<String,String> exploreConstraints(X10Context_c context, ConstrainedType type) {
 		HashMap<String,String> r = new HashMap<String,String>();
 		CConstraint cc = type.getRealXClause();
 		 // FIXME: [DC] context.constraintProjection ought not to eliminate information but it seems to?
 		CConstraint projected = cc; //context.constraintProjection(cc);
 		if (!projected.consistent()) return r;
 		for (XVar xvar : projected.vars()) {
 			XVar prefixes[] = xvar.vars();
 			if (prefixes.length!=2) continue;
 			if (!prefixes[0].toString().equals("self")) continue;
 			if (!(xvar instanceof CField)) continue;
 			CField xvarf = (CField)xvar;
 			// [DC] I believe that since we are only looking at constraints of the form self.f,
 			// there is no need to check the type of the class which this field is attached to as it will
 			// always be the type we are translating.
 			String property_name = ((X10FieldDef)xvarf.field()).name().toString();
 			// resolve to another variable, keep going
 			XVar closed_xvar = projected.bindingForVar(xvar);
 			if (closed_xvar!=null && closed_xvar instanceof XLit) {
 				r.put(property_name, closed_xvar.toString());
 			}
 		}
 		return r;
 	}
 	
 	/**
 	 * Translate a type.
 	 *
 	 * @param type type to translate
 	 * @param asRef whether to make a reference
 	 * @return a string representation of the type
 	 */
 	public static String translateType(Type type, boolean asRef) {
 		return translateType(type, asRef, null);
 	}
 	public static String translateType(Type type_, boolean asRef, X10Context_c ctx) {
 		assert (type_ != null);
 		TypeSystem xts = type_.typeSystem();
 		Type type = xts.expandMacros(type_);
 		if (type.isVoid()) {
 			return "void";
 		}
 		HashMap<String,String> propertyKnowledge = null;
 		if (ctx!=null && type instanceof ConstrainedType) propertyKnowledge = exploreConstraints(ctx, (ConstrainedType)type);
 		// TODO: handle closures
 //		if (((X10TypeSystem) type.typeSystem()).isClosure(type))
 //			return translateType(((X10Type) type).toClosure().base(), asRef);
 		type = Types.baseType(type);
 		String name = null;
 		// TODO
 //		if (type instanceof ClosureType) {
 //		    ClosureType ct = (ClosureType) type;
 //		    assert (ct.typeArguments().size() != 0);
 //		    name = "x10aux::Fun";
 //		    name = translate_mangled_FQN(name);
 //		    String args = "";
 //		    if (ct.returnType().isVoid())
 //		        args += translateType(ct.returnType(), true) + ", ";
 //		    int s = ct.typeArguments().size();
 //		    for (Type t: ct.typeArguments()) {
 //		        args += translateType(t, true); // type arguments are always translated as refs
 //		        if (--s > 0)
 //		            args +=", ";
 //		    }
 //		    name += chevrons(args);
 //		} else
 		if (type.isClass()) {
 			X10ClassType ct = (X10ClassType) type.toClass();
 			if (ct.isX10Struct()) {
 				// Struct types are not boxed up as Refs.  They are always generated as just C++ class types
 				// (Note: not pointer to Class, but the actual class).
 				asRef = false;
 			}
 
 		    List<Type> typeArguments = ct.typeArguments();
 		    X10ClassDef cd = ct.x10Def();
 		    if (typeArguments == null) typeArguments = new ArrayList<Type>(cd.typeParameters());
 		    if (ct.isAnonymous()) {
 		        if (ct.interfaces().size() == 1 && ct.interfaces().get(0) instanceof FunctionType) {
 		            return translateType(ct.interfaces().get(0), asRef);
 		        } else {
 		            assert false : "unexpected anonymous type " + ct;
 		            assert ct.superClass() != null;
 		       	    return translateType(ct.superClass(), asRef);
 		       	}
 		    } else {
 				String pat = asRef ? getCppRep(cd) : getCppBoxRep(cd);
 				if (pat != null) {
 					Map<String, Object> env = new HashMap<String,Object>();
 					int counter=0;
 					env.put(Integer.toString(counter++), type);
 					Iterator<ParameterType> params = cd.typeParameters().iterator();
 					for (Type a : typeArguments) {
 						   env.put(params.next().name().toString(), a);
 						   env.put(Integer.toString(counter++), a);
 					}
 					if (propertyKnowledge!=null) for (String key : propertyKnowledge.keySet()) {
 							env.put(key, propertyKnowledge.get(key));
 					}
 					return nativeSubst("NativeRep", env, pat);
 				}
 				else {
 					name = fullName(ct).toString();
 				}
 			}
 		    name = translate_mangled_FQN(name);
 			if (typeArguments.size() != 0) {
 				String args = "";
 				int s = typeArguments.size();
 				for (Type t: typeArguments) {
 					args += translateType(t, true); // type arguments are always translated as refs
 					if (--s > 0)
 						args +=", ";
 				}
 				name += chevrons(args);
 			}
 		} else if (type instanceof ParameterType) {
 			name = ((ParameterType)type).name().toString();
 			return mangled_parameter_type_name(name); // parameter types shouldn't be refs
 		} else if (type.isNull()) {
 			return "x10aux::ref<x10::lang::NullType>"; // typedef to something sensible
 		} else {
 			assert false : type; // unhandled type.
 		}
 		assert (name != null);
 		if (!asRef)
 			return name;
 		return make_ref(name);
 	}
 	
 	public static String structMethodClass(X10ClassType ct, boolean fqn, boolean chevrons) {
 	    X10ClassDef cd = ct.x10Def();
 	    QName full = fullName(cd.asType());
 	    String name = fqn ? full.toString() : full.name().toString();
 	    name += "_methods";
 	    name = translate_mangled_FQN(name);
 	    List<Type> typeArguments = ct.typeArguments();
 	    if (typeArguments == null) typeArguments = new ArrayList<Type>(cd.typeParameters());
 	    if (chevrons && typeArguments.size() != 0) {
 	        String args = "";
 	        int s = typeArguments.size();
 	        for (Type t: typeArguments) {
 	            args += translateType(t, true); // type arguments are always translated as refs
 	            if (--s > 0)
 	                args +=", ";
 	        }
 	        name += chevrons(args);
 	    }
 	    return name;
 	}
 
 	void printArgumentList(CodeWriter w, X10CPPContext_c c) {
 		printArgumentList(w, c, false, true);
 	}
 
 	void printArgumentList(CodeWriter w, X10CPPContext_c c, boolean omitType) {
 		printArgumentList(w, c, omitType, true);
 	}
 
 	void printArgumentList(CodeWriter w, X10CPPContext_c c, boolean omitType, boolean saved_this_mechanism) {
 		for (int i = 0; i < c.variables.size(); i++) {
 			if (i > 0) {
 				w.write(",");
 				w.allowBreak(2, " ");
 			}
 			VarInstance<?> var = c.variables.get(i);
 			if (!omitType) {
 				Type t = var.type();
 				String type = translateType(t, true);
 				w.write(type + " ");
 			}
 			String name = var.name().toString();
 			if (saved_this_mechanism && name.equals(THIS))
 				name = SAVED_THIS;
 			else
 				name = mangled_non_method_name(name) ;
 
 			w.write(name);
 		}
 	}
 
 
 	public void printTemplateSignature(List<? extends Type> list, CodeWriter h) {
 		int size = list == null ? 0 : list.size();
 		if (size != 0) {
 			h.write("template<class ");
 			for (Type t: list) {
 				assert (t instanceof ParameterType);
 				h.write(translateType(t));
 				size--;
 				if (size > 0)
 					h.write(", class ");
 			}
 			h.write(">");
 			h.allowBreak(0, " ");
 		}
 	}
 
 	void printTemplateInstantiation(MethodInstance mi, CodeWriter w) {
 		if (mi.typeParameters().size() == 0)
 			return;
 		w.write("<");
 		for (Iterator<Type> i = mi.typeParameters().iterator(); i.hasNext(); ) {
 		    final Type at = i.next();
 		    w.write(translateType(at, true));
 		    if (i.hasNext()) {
 		        w.write(",");
 		        w.allowBreak(0, " ");
 		    }
 		}
 		w.write(" >");
 	}
 
 	public static String voidTemplateInstantiation(int num) {
 		if (num <= 0)
 		    return "";
 		StringBuffer b = new StringBuffer();
 		b.append("<");
 		for (int i = 0; i < num; i++) {
 			if (i > 0)
 				b.append(", ");
 			b.append("void");
 		}
 		b.append(">");
 		return b.toString();
 	}
 
 	static MethodInstance getOverridingMethod(TypeSystem xts, ClassType localClass, 
 	                                             MethodInstance mi, Context context) {
 	    try {
 	        List<Type> params = ((MethodInstance) mi).typeParameters();
 	        List<MethodInstance> overrides = xts.findAcceptableMethods(localClass, xts.MethodMatcher(localClass, mi.name(), ((MethodInstance) mi).typeParameters(), mi.formalTypes(), context));
 	        for (MethodInstance smi : overrides) {
 	            if (CollectionUtil.allElementwise(mi.formalTypes(), smi.formalTypes(), new BaseTypeEquals(context))) {
 	                List<Type> sparams =  smi.typeParameters();
 	                if (params == null && sparams == null)
 	                    return smi;
 	                if (params != null && params.equals(sparams))
 	                    return smi;
 	            }
 	        }
 	        return null;
 	    } catch (SemanticException e) {
 	        return null;
 	    }
 	}
 
 	Type findRootMethodReturnType(X10MethodDef n, Position pos, MethodInstance from) {
 		assert from != null;
 		// [IP] Optimizations
 		Flags flags = from.flags();
 		if (flags.isStatic() || flags.isPrivate() || flags.isProperty() || from.returnType().isVoid())
 			return from.returnType();
 
 		// [DC] c++ doesn't allow covariant smartptr return types so
 		// we are forced to use the same type everywhere and cast
 		// on call.  This function gets the one type that we use for
 		// all return types, from the root of the class hierarchy.
 
 		// TODO: currently we cannot handle the following code:
 		// A new approach is required.
 		/*
 		interface Cloneable {
 			def clone() : Cloneable;
 		}
 
 		interface Cloneable2 {
 			def clone() : Cloneable2;
 		}
 
 		class A implements Cloneable, Cloneable2 {
 			public def clone() { return this; }
 		}
 		*/
 
 		// [DC] TODO: There has to be a better way!
 		TypeSystem xts = (TypeSystem) tr.typeSystem();
 		Context context = tr.context();
 
 		X10ClassType classType = (X10ClassType) from.container();
 		X10ClassType superClass = (X10ClassType) Types.baseType(classType.superClass());
 		List<Type> interfaces = classType.interfaces();
 		Type returnType = null;
 
 		if (superClass != null) {
 			MethodInstance superMeth = getOverridingMethod(xts,superClass,from,context);
 			if (superMeth != null) {
 				//System.out.println(from+" overrides "+superMeth);
 				returnType = findRootMethodReturnType(n, pos, superMeth);
 			}
 		}
 
 		for (Type itf : interfaces) {
 			X10ClassType itf_ = (X10ClassType) Types.baseType(itf);
 			// same thing again for interfaces
 			MethodInstance superMeth = getOverridingMethod(xts,itf_,from,context);
 			if (superMeth != null) {
 				//System.out.println(from+" implements "+superMeth);
 				Type newReturnType = findRootMethodReturnType(n, pos, superMeth);
 
 				// check --
 				if (returnType != null && !xts.typeDeepBaseEquals(returnType, newReturnType, context)) {
 					String msg = "Two supertypes declare " + from + " with "
 						+ "different return types: " + returnType + " != " + newReturnType;
 					Warnings.issue(tr.job(), msg, pos);
 				}
 				returnType = newReturnType;
 			}
 		}
 
 		// if we couldn't find an overridden method, just use the local return type
 		return returnType != null ? returnType : from.returnType();
 	}
 
 	private static final QName NORETURN_ANNOTATION = QName.make("x10.compiler.NoReturn");
 
 	void printHeader(X10MethodDecl_c n, CodeWriter h, Translator tr, boolean qualify, boolean inlineDirective) {
 		printHeader(n, h, tr, n.name().id().toString(), n.returnType().type(), qualify, inlineDirective);
 	}
 	void printHeader(X10MethodDecl_c n, CodeWriter h, Translator tr, String name, Type ret, 
 	                 boolean qualify, boolean inlineDirective) {
 		Flags flags = n.flags().flags();
 		X10MethodDef def = (X10MethodDef) n.methodDef();
 		MethodInstance mi = (MethodInstance) def.asInstance();
 		X10ClassType container = (X10ClassType) mi.container();
 		boolean isStruct = container.isX10Struct();
 		boolean nonVirtualDispatch = flags.isStatic() || isStruct || flags.isProperty() || flags.isPrivate();
 		boolean genericVirtual = !nonVirtualDispatch && def.typeParameters().size() > 0;
 
 		h.begin(0);
 
 		if (qualify) {
 			printTemplateSignature(((X10ClassType)Types.get(n.methodDef().container())).x10Def().typeParameters(), h);
 		}
 		
 		if (inlineDirective) {
 		    h.write("inline ");
 		}
 
 		printTemplateSignature(def.typeParameters(), h);
 
 		if (!qualify) {
 			if (flags.isStatic()) {
 				h.write(flags.retain(Flags.STATIC).translateJava());
 			} else if (!(nonVirtualDispatch || genericVirtual)) {
 			    h.write("virtual ");
 			}
 		}
 		if (!qualify && !flags.isStatic() && container.isX10Struct()) h.write("static ");
 		printType(ret, h);
 		h.allowBreak(2, 2, " ", 1);
 		if (qualify) {
 		    h.write((container.isX10Struct() ? structMethodClass(container, true, true) : translateType(container))+ "::");
 		}
 		h.write(mangled_method_name(name));
 		printFormalDecls(n, h, tr, flags, container, isStruct);
 		
 		if (!qualify) {
 		    boolean noReturnPragma = false;
 		    try {
 		        TypeSystem xts = (TypeSystem)tr.typeSystem();
 		        Type annotation = xts.systemResolver().findOne(NORETURN_ANNOTATION);
 		        if (!((X10Ext) n.ext()).annotationMatching(annotation).isEmpty()) {
 		            noReturnPragma = true;
 		        }
 		    } catch (SemanticException e) { 
 		        /* Ignore exception when looking for annotation */  
 		    }		
 
 		    if (noReturnPragma) {
 		        h.write(" X10_PRAGMA_NORETURN ");
 		    }
 		}
         
 		h.end();
 		if (!qualify) {
 			assert (!flags.isNative());
 			if (n.body() == null && !flags.isProperty() && !isStruct)
 				h.write(" = 0");
 		}
 	}
 	
     private void printFormalDecls(X10MethodDecl_c n, CodeWriter h, Translator tr, Flags flags, X10ClassType container,
                                   boolean explicitThis) {
         h.write("(");
         h.allowBreak(2, 2, "", 0);
         h.begin(0);
         if (explicitThis && !flags.isStatic()) {
 		    h.write(translateType(container) +" this_");
 		    if (!n.formals().isEmpty()) h.write(", ");
 		}
 		for (Iterator<Formal> i = n.formals().iterator(); i.hasNext(); ) {
 			Formal f = i.next();
 			n.print(f, h, tr);
 			if (i.hasNext()) {
 				h.write(",");
 				h.allowBreak(0, " ");
 			}
 		}
         h.end();
         h.write(")");
     }
 
     String genericMethodDispatcherName(MethodInstance mi) {
         StringBuilder sb = new StringBuilder();
         sb.append("_gmdt_"+mangled_method_name(mi.name().toString()));
         sb.append("_"+mi.formalTypes().size());
         for (Type ft : mi.formalTypes()) {
             sb.append("_"+mangled_non_method_name(ft.name().toString()));
         }
         
         return sb.toString();
     }
     
     void generateGenericMethodDispatcher(X10MethodDecl_c n, CodeWriter header, CodeWriter genericFunctions, CodeWriter classBody, 
                                          Translator tr, String name, Type ret) {
         Flags flags = n.flags().flags();
         X10MethodDef def = (X10MethodDef) n.methodDef();
         MethodInstance mi = (MethodInstance) def.asInstance();
         X10ClassType container = (X10ClassType) mi.container();
 
         String dispatcherClassCName = genericMethodDispatcherName(mi);
         String containerCClassName = translateType(container, false);
         String containerCName = translateType(container, true);
         boolean rootMethod = mi.overrides(tr.context()).size() == 1;
         if (rootMethod) {
             String retCType = translateType(ret);
             printTemplateSignature(def.typeParameters(), header);
             header.write("class "+dispatcherClassCName+" {"); header.newline(4); header.begin(0);
             header.writeln("static int _id;");
             header.writeln("public:");
             header.writeln("static int init();");
             header.write("static "+retCType+" invoke("+containerCName+" this_");
             int argNum = 0;
             for (Type ft : mi.formalTypes()) {
                 header.write(", "+translateType(ft, true)+" arg"+argNum);
                 argNum++;
             }
             header.write(") {"); header.newline(4); header.begin(0);
             StringBuilder fpType = new StringBuilder();
             fpType.append("x10aux::dispatcher<"+retCType+"("+containerCClassName+"::*)("); 
             boolean firstTime = true;
             for (Type ft : mi.formalTypes()) {
                 if (firstTime) {
                     firstTime = false;
                 } else {
                     fpType.append(", ");
                 }
                 fpType.append(translateType(ft, true));
             }
             fpType.append(")>");
             String fpCType = fpType.toString();
             header.writeln(fpCType+" disp = ("+fpCType+")(this_->_disp_"+dispatcherClassCName+"());");
             if (!ret.isVoid()) header.write("return ");
             header.write("(this_->*(disp->get(_id)))(");
             for (argNum = 0; argNum<mi.formalTypes().size(); argNum++) {
                 header.write((argNum > 0 ? ", arg" : "arg")+argNum);
             }
             header.writeln(");");
             header.end(); header.newline();
             header.write("}"); 
             header.end(); header.newline();
             header.writeln("};");
             
             printTemplateSignature(((X10ClassType)Types.get(n.methodDef().container())).x10Def().typeParameters(), genericFunctions);
             printTemplateSignature(def.typeParameters(), genericFunctions);
             StringBuilder fqdc = new StringBuilder();
             fqdc.append(containerCClassName+"::"+dispatcherClassCName+"<");
             boolean first = true;
             for (ParameterType pt : def.typeParameters()) {
                 if (first) {
                     first = false;
                 } else {
                     fqdc.append(", ");
                 }
                 fqdc.append(translateType(pt));
             }
             fqdc.append(" >");
             String fqdcStr = fqdc.toString();
             genericFunctions.write(" int "+fqdcStr+"::_id = "+fqdcStr+"::init();"); genericFunctions.newline();
         }
 
         String dispatcherCType = "x10aux::dispatcher<void("+containerCClassName+"::*)()>";
         header.writeln("static "+dispatcherCType+" _dispTable_"+dispatcherClassCName+";");
         header.writeln("virtual void* _disp_"+dispatcherClassCName+"() { return &_dispTable_"+dispatcherClassCName+"; }");
         header.forceNewline();
         
         printTemplateSignature(((X10ClassType)Types.get(n.methodDef().container())).x10Def().typeParameters(), classBody);
         classBody.writeln(" "+dispatcherCType+" "+containerCClassName+"::_dispTable_"+dispatcherClassCName+";");
         classBody.forceNewline();
     }
 
     
 	void printHeader(Formal_c n, CodeWriter h, Translator tr, boolean qualify) {
 //		Flags flags = n.flags().flags();
 		h.begin(0);
 //		if (flags.isFinal())
 //		h.write("const ");
 		printType(n.type().type(), h, (X10Context_c)tr.context());
 		h.write(" ");
 		TypeSystem xts = (TypeSystem) tr.typeSystem();
 		Type param_type = n.type().type();
 		param_type = Types.baseType(param_type);
 		if (param_type instanceof X10ClassType) {
 			X10ClassType c = (X10ClassType)param_type;
 			if (c.isX10Struct()) {
 		        try {
 					Type annotation = xts.systemResolver().findOne(QName.make("x10.compiler.ByRef"));
 					if (!c.annotationsMatching(annotation).isEmpty()) h.write("&");
 		        } catch (SemanticException e) {
 		            assert false : e;
 		        }
 			}
 		}
 		h.write(mangled_non_method_name(n.name().id().toString()));
 		h.end();
 	}
 
 	private void printAllTemplateSignatures(X10ClassDef cd, CodeWriter h) {
 		if (cd.isNested()) {
 			assert (false) : ("Nested class alert!");
 			printAllTemplateSignatures((X10ClassDef) Types.get(cd.outer()), h);
 		}
 		printTemplateSignature(cd.typeParameters(), h);
 	}
 
 	void printRTTDefn(X10ClassType ct, CodeWriter h) {
 	    TypeSystem xts =   ct.typeSystem();
 	    X10ClassDef cd = ct.x10Def();
 	    String x10name = fullName(ct).toString().replace('$','.');
 	    int numParents = 0;
 	    if (ct.superClass() != null) {
 	        numParents++;
 	    }
 	    numParents += ct.interfaces().size();
 	    String kind;
 	    if (ct.isX10Struct()) {
 	        kind = "x10aux::RuntimeType::struct_kind";
 	    } else if (ct.flags().isInterface()) {
 	        kind = "x10aux::RuntimeType::interface_kind";
 	    } else {
 	        kind = "x10aux::RuntimeType::class_kind";
 	    }
 
 	    if (cd.typeParameters().isEmpty()) {
 	        boolean first = true;
 	        h.write("x10aux::RuntimeType "+translateType(ct)+"::rtt;"); h.newline();
 	        h.write("void "+translateType(ct)+"::_initRTT() {"); h.newline(4); h.begin(0);
 	        h.write("if (rtt.initStageOne(&rtt)) return;"); h.newline();
 	        if (numParents > 0) { 
 	            h.write("const x10aux::RuntimeType* parents["+numParents+"] = { ");
 	            if (ct.superClass() != null) {
 	                h.write("x10aux::getRTT" + chevrons(translateType(ct.superClass())) + "()");
 	                first = false;
 	            }
 	            for (Type iface : ct.interfaces()) {
 	                if (!first) h.write(", ");
 	                h.write("x10aux::getRTT"+chevrons(translateType(iface))+"()");
 	                first = false;
 	            }
 	            h.write("};"); h.newline();
 	        } else {
 	            h.write("const x10aux::RuntimeType** parents = NULL; "); h.newline();
 	        }
 	        h.write("rtt.initStageTwo(\""+x10name+"\","+kind+", "+numParents+ ", parents, 0, NULL, NULL);");
 	        if (ct.isX10Struct() && isPointerless(ct)) {
 	            h.newline(); h.write("rtt.containsPtrs = false;");
 	        }
 	        h.end(); h.newline();
 	        h.write("}"); h.newline();
 	    } else {
 	        boolean first = true;
 	        int numTypeParams = cd.typeParameters().size();
 	        printTemplateSignature(cd.typeParameters(), h);
 	        h.write("x10aux::RuntimeType "+translateType(ct)+"::rtt;"); h.newline();
 
 	        printTemplateSignature(cd.typeParameters(), h);
 	        h.write("void "+translateType(ct)+"::_initRTT() {"); h.newline(4); h.begin(0);
                 h.write("const x10aux::RuntimeType *canonical = x10aux::getRTT"+chevrons(translateType(MessagePassingCodeGenerator.getStaticMemberContainer(cd), false))+"();");
                 h.newline();
                 h.write("if (rtt.initStageOne(canonical)) return;"); h.newline();
 	        
 	        if (numParents > 0) {
 	            h.write("const x10aux::RuntimeType* parents["+numParents+"] = { ");
 	            if (ct.superClass() != null) {
 	                h.write("x10aux::getRTT" + chevrons(translateType(ct.superClass())) + "()");
 	                first = false;
 	            }
 	            for (Type iface : ct.interfaces()) {
 	                if (!first) h.write(", ");
 	                h.write("x10aux::getRTT"+chevrons(translateType(iface))+"()");
 	                first = false;
 	            }
 	            h.write("};"); h.newline();
 	        } else {
 	            h.write("const x10aux::RuntimeType** parents = NULL; "); h.newline();                
 	        }
 	        h.write("const x10aux::RuntimeType* params["+numTypeParams+"] = { ");
 	        first = true;
 	        for (Type param : cd.typeParameters()) {
 	            if (!first) h.write(", ");
 	            h.write("x10aux::getRTT"+chevrons(translateType(param))+"()");
 	            first = false;
 	        }
 	        h.write("};"); h.newline();
 
 	        h.write("x10aux::RuntimeType::Variance variances["+numTypeParams+"] = { ");
 	        first = true;
 	        for (ParameterType.Variance v : ct.x10Def().variances()) {
 	            if (!first) h.write(", ");
 	            switch(v) {
 	            case COVARIANT: h.write("x10aux::RuntimeType::covariant"); break;
 	            case CONTRAVARIANT: h.write("x10aux::RuntimeType::contravariant"); break;
 	            case INVARIANT: h.write("x10aux::RuntimeType::invariant"); break;
 	            default: assert false : "Unexpected Variance";
 	            }
 	            first = false;
 	        }
 	        h.write("};"); h.newline();
 
 	        h.write("const char *baseName = \""+x10name+"\";"); h.newline();
 	        h.write("rtt.initStageTwo(baseName, "+kind+", "+numParents+", parents, "+numTypeParams+", params, variances);"); h.end(); h.newline();
 	        h.write("}"); h.newline();
 	    }
 	    h.newline();
 	}
 
     // Helper method to recursively examine the fields of a struct and determine if they
     // are pointers.  Used to mark the RTT of the struct as pointerless, thus enabling
     // Rails and Arrays of pointerless structs to be allocated with GC_MALLOC_ATOMIC
     private boolean isPointerless(X10ClassType ct) {
         assert ct.isX10Struct() : "Only structs should be checked to see if they are pointerless";
         
         for (FieldInstance fi : ct.fields()) {
             if (fi.flags().isStatic()) continue; // ignore static fields; only care about instance fields
             if (!fi.type().isNumeric()) {
                 if (fi.type().isClass() && ((X10ClassType)fi.type().toClass()).isX10Struct()) {
                     // recursively check fields of struct type
                     if (!isPointerless(((X10ClassType)fi.type().toClass()))) return false;
                 } else {
                     // if fi.type() isn't numeric and isn't a struct, it must be a pointer.
                     return false;
                 }
             }
         }
         
         return true;
     }
                                                     
                                                     
     
     void printHeader(X10ClassDecl_c n, CodeWriter h, Translator tr) {
 		h.begin(0);
 		
 		// Handle generics
 		// If it involves Parameter Types then generate C++ templates.
 		printAllTemplateSignatures(n.classDef(), h);
 
 		h.write("class ");
 		assert (!n.classDef().isLocal());
 		if (n.classDef().isNested() && !n.classDef().isLocal()) { // FIXME: handle local classes
 			assert (false) : ("Nested class alert!");
 			h.write(translateType(n.classDef().outer().get().asType()) + "::");
 		}
 		h.write(mangled_non_method_name(n.name().id().toString()));
 
 		if (!n.flags().flags().isInterface() && n.superClass() != null) {
 			String parent = translateType(n.superClass().type());
 			h.write(" : public "+parent);
 		}
 
 		h.unifiedBreak(0);
 		h.end();
 	}
     
     void printHeaderForStructMethods(X10ClassDecl_c n, CodeWriter h, Translator tr) {
         h.begin(0);
         
         // Handle generics
         // If it involves Parameter Types then generate C++ templates.
         printAllTemplateSignatures(n.classDef(), h);
 
         h.write("class ");
         h.write(structMethodClass(n.classDef().asType(), false, false));
 
         h.unifiedBreak(0);
         h.end();
     }
     
 	void printHeader(ConstructorDecl_c n, CodeWriter h, Translator tr,
                      boolean define, boolean isMakeMethod, String rType) {
         Flags flags = n.flags().flags();
 
 		X10ClassType container = (X10ClassType) Types.get(n.constructorDef().container());
 		if (define){
 			printTemplateSignature(container.x10Def().typeParameters(), h);
 		}
 
 		// [IP] Constructors don't have type parameters, they inherit them from the container. 
 		//X10ConstructorDef def = (X10ConstructorDef) n.constructorDef();
 		//printTemplateSignature(toTypeList(def.typeParameters()), h);
 
 		h.begin(0);
 		if (!define && container.isX10Struct()) h.write("static ");
 		String typeName = translateType(container.def().asType());
         // not a virtual method, this function is called only when the static type is precise
         h.write(rType + " ");
 		if (define) {
 		    h.write((container.isX10Struct() ? structMethodClass(container, true, true) : typeName) + "::"); 
 		}
 		h.write((isMakeMethod ? SharedVarsMethods.MAKE : SharedVarsMethods.CONSTRUCTOR) + "(");
 		h.allowBreak(2, 2, "", 0);
 		h.begin(0);
 		if (container.isX10Struct() && !isMakeMethod) {
 		    h.write(typeName + "& this_");
 		    if (!n.formals().isEmpty()) h.write(", ");
 		}
 		for (Iterator<Formal> i = n.formals().iterator(); i.hasNext(); ) {
 			Formal f = i.next();
 			n.print(f, h, tr);
 			if (i.hasNext()) {
 				h.write(",");
 				h.allowBreak(0, " ");
 			}
 		}
 		h.end();
 		h.write(")");
 		h.end();
         // TODO: caller should emit this
 	}
 
 	void printHeader(FieldDecl_c n, CodeWriter h, Translator tr, boolean qualify) {
 		Flags flags = n.flags().flags();
 		h.begin(0);
 		if (!qualify) {
 			if (flags.isStatic())
 				h.write(flags.retain(Flags.STATIC).translateJava());
 		}
 
 		if (query.hasAnnotation(n, "x10.compiler.Volatile")) {
 			h.write("volatile ");
 		}
 
 		printType(n.type().type(), h);
 		h.allowBreak(2, 2, " ", 1);
 		if (qualify) {
 		    X10ClassType declClass = (X10ClassType)n.fieldDef().asInstance().container().toClass();
 			h.write((declClass.isX10Struct() ? structMethodClass(declClass, true, true) : translateType(declClass)) + "::");
 		}
 		h.write(mangled_field_name(n.name().id().toString()));
 		h.end();
 	}
 
 	void printHeader(LocalDecl_c n, CodeWriter h, Translator tr, boolean qualify) {
 		//Flags flags = n.flags().flags();
 		h.begin(0);
 		// Let us not generate constants - We will have problem in
 		// initializing away from the place where it is declared.
 		//if (flags.isFinal())            }
 		//	h.write("const ");
 		if (tr.printType()) {
 			assert (n != null);
 			assert (n.type() != null);
 			assert (n.type().type() != null);
 			printType(n.type().type(), h, (X10Context_c)tr.context());
 			h.write(" ");
 		}
 		h.write(mangled_non_method_name(n.name().id().toString()));
 		h.end();
 	}
 
 	void enterClosure(X10CPPContext_c c) {
 		c.advanceClosureId();
 	}
 	void exitClosure(X10CPPContext_c c) {
 	}
 
     void printExplicitTarget(Call_c n, Receiver target, X10CPPContext_c context, CodeWriter w) {
 		if (target instanceof X10Special_c &&
 				((X10Special_c)target).kind().equals(X10Special_c.THIS) &&
 				context.isInsideClosure())
 		{
 			w.write(SAVED_THIS);
 			if (context.isInsideClosure())
 				context.saveEnvVariableInfo(THIS);
 		}
 		else if (target instanceof Expr) {
 			boolean assoc = !(target instanceof New_c);
 			n.printSubExpr((Expr) target, assoc, w, tr);
 		}
 		else if (target != null) {
 			n.print(target, w, tr);
 		}
 
 		return;
 	}
 
 	public void printDeclarationList(CodeWriter w, X10CPPContext_c c, ArrayList<VarInstance<?>> vars, List<VarInstance<?>> refs) {
 		printDeclarationList(w, c, vars, true, false, refs);
 	}
 
 	void printDeclarationList(CodeWriter w, X10CPPContext_c c, ArrayList<VarInstance<?>> vars, boolean saved_this_mechanism, boolean writable, List<VarInstance<?>> refs) {
 		for (int i = 0; i < vars.size(); i++) {
 			VarInstance<?> var = vars.get(i);
 			VarDef def = var.def();
 			Type t = var.type();
 			String type;
 			if ((writable && !var.name().toString().equals(THIS)) || refs.contains(var)) {
 			    type = make_captured_lval(t);
 			} else {
 			    type = translateType(t, true);
 			}
 			String name = var.name().toString();
 			if (saved_this_mechanism && name.equals(THIS)) {
 				assert (c.isInsideClosure()); // FIXME: Krishna, why did you add this test?
 				name = SAVED_THIS;
 			}
 			else {
 				name = mangled_non_method_name(name);
 			}
 			w.write(type + " " + name + ";");
 			w.newline();
 			
 			if (((X10CPPCompilerOptions)tr.job().extensionInfo().getOptions()).x10_config.DEBUG)
 			{
 				String key = ((StreamWrapper)w).getStreamName(StreamWrapper.CC);
 				Map<String, LineNumberMap> fileToLineNumberMap = c.<Map<String, LineNumberMap>>findData(X10CPPTranslator.FILE_TO_LINE_NUMBER_MAP);
 			    if (fileToLineNumberMap != null) 
 			    {
 			        final LineNumberMap lineNumberMap = fileToLineNumberMap.get(key);
 			        if (lineNumberMap != null) 
 			        {
 			        	String hostClassName = translate_mangled_FQN(fullName(c.currentClass()).toString(), "_");
 			        	String wrappingClosure = MessagePassingCodeGenerator.getClosureName(hostClassName, c.closureId());
 			        	lineNumberMap.addClosureMember(name, t.toString(), wrappingClosure, c.currentCode().position().file(), c.currentCode().position().line(), c.currentCode().position().endLine());
 			        } 
 			    }
 			}
 		}
 	}
 
     void generateClassSerializationMethods(ClassType type, StreamWrapper sw) {
         X10ClassType ct = (X10ClassType) type.toClass();
         TypeSystem ts =  type.typeSystem();
         X10CPPContext_c context = (X10CPPContext_c) tr.context();
         Type parent = type.superClass();
         boolean customSerialization = type.isSubtype(ts.CustomSerialization(), context);
         ClassifiedStream w = sw.body();
         ClassifiedStream h = sw.header();
         h.forceNewline();
 
         h.write("// Serialization"); h.newline();
         String klass = translateType(type);
 
         String template = context.inTemplate() ? "template " : "";
 
         if (!type.flags().isAbstract()) {
             // _serialization_id
             h.write("public: static const x10aux::serialization_id_t "+SERIALIZATION_ID_FIELD+";"); h.newline();
             h.forceNewline();
             printTemplateSignature(ct.x10Def().typeParameters(), w);
             w.write("const x10aux::serialization_id_t "+klass+"::"+SERIALIZATION_ID_FIELD+" = ");
             w.newline(4);
             w.write("x10aux::DeserializationDispatcher::addDeserializer(");
             w.write(klass+"::"+template+DESERIALIZER_METHOD+chevrons("x10::lang::Reference")+", x10aux::CLOSURE_KIND_NOT_ASYNC);");
             w.newline(); w.forceNewline();
         }
 
         // _serialize_id()
         if (!type.flags().isAbstract()) {
             h.write("public: ");
             if (!type.flags().isFinal())
                 h.write("virtual ");
             h.write("x10aux::serialization_id_t "+SERIALIZE_ID_METHOD+"() {");
             h.newline(4); h.begin(0);
             h.write(" return "+SERIALIZATION_ID_FIELD+";"); h.end(); h.newline();
             h.write("}"); h.newline();
             h.forceNewline();
         }
 
         if (customSerialization && !((X10ClassDef)ct.def()).hasDeserializationConstructor(context)) {
             h.writeln("// autogenerated custom deserialization");
             h.writeln("void "+CONSTRUCTOR+"("+translateType(ts.SerialData(), true)+" sd) {");
             h.newline(4); h.begin(0);
             h.write(translateType(parent)+"::"+CONSTRUCTOR+"(sd);");
             h.end(); h.newline();
             h.write("}"); h.newline();
             h.forceNewline();
         }
         
         // _serialize_body()
         h.write("public: ");
         h.write("virtual ");
         h.write("void "+SERIALIZE_BODY_METHOD+"("+SERIALIZATION_BUFFER+"& buf);");
         h.newline(0); h.forceNewline();
 
         printTemplateSignature(ct.x10Def().typeParameters(), w);
         w.write("void "+klass+"::"+SERIALIZE_BODY_METHOD+
                     "("+SERIALIZATION_BUFFER+"& buf) {");
         w.newline(4); w.begin(0);
         if (customSerialization) {
             w.writeln("/* NOTE: Implements x10.io.CustomSerialization */");
             w.writeln("buf.write(this->serialize());");
         } else {
             if (parent != null && parent.isClass()) {
                 w.write(translateType(parent)+"::"+SERIALIZE_BODY_METHOD+"(buf);");
                 w.newline();
             }
             for (int i = 0; i < type.fields().size(); i++) {
                 if (i != 0)
                     w.newline();
                 FieldInstance f = (FieldInstance) type.fields().get(i);
 
                 if (f.flags().isStatic() || query.isSyntheticField(f.name().toString()))
                     continue;
                 if (f.flags().isTransient()) // don't serialize transient fields
                     continue;
                 String fieldName = mangled_field_name(f.name().toString());
                 w.write("buf.write(this->"+fieldName+");"); w.newline();
             }
         }
         w.end(); w.newline();
         w.write("}");
         w.newline(); w.forceNewline();
 
         if (!type.flags().isAbstract()) {
             // _deserializer()
             h.write("public: template<class __T> static ");
             h.write(make_ref("__T")+" "+DESERIALIZER_METHOD+"("+DESERIALIZATION_BUFFER+"& buf);");
             h.newline(); h.forceNewline();
             sw.pushCurrentStream(context.genericFunctions);
             printTemplateSignature(ct.x10Def().typeParameters(), sw);
             sw.write("template<class __T> ");
             sw.write(make_ref("__T")+" "+klass+"::"+DESERIALIZER_METHOD+"("+DESERIALIZATION_BUFFER+"& buf) {");
             sw.newline(4); sw.begin(0);
             sw.writeln(make_ref(klass)+" this_ = "+
                        "new (memset(x10aux::alloc"+chevrons(klass)+"(), 0, sizeof("+klass+"))) "+klass+"();");
             sw.writeln("buf.record_reference(this_);");
             sw.writeln("this_->"+DESERIALIZE_BODY_METHOD+"(buf);");
             sw.write("return this_;");
             sw.end(); sw.newline();
             sw.writeln("}"); sw.forceNewline();
             sw.popCurrentStream();
         }
 
         // _deserialize_body()
         h.write("public: ");
         h.write("void "+DESERIALIZE_BODY_METHOD+"("+DESERIALIZATION_BUFFER+"& buf);"); h.newline(0);
         printTemplateSignature(ct.x10Def().typeParameters(), w);
         w.write("void "+klass+"::"+DESERIALIZE_BODY_METHOD+"("+DESERIALIZATION_BUFFER+"& buf) {");
         w.newline(4); w.begin(0);
         if (customSerialization) {
             w.writeln("/* NOTE: Implements x10.io.CustomSerialization */");
             w.writeln(translateType(ts.SerialData(), true)+ "val_ = buf.read"+chevrons(translateType(ts.SerialData(), true))+"();");
             w.writeln(CONSTRUCTOR+"(val_);");
         } else {
             if (parent != null && parent.isClass()) {
                 w.write(translateType(parent)+"::"+DESERIALIZE_BODY_METHOD+"(buf);");
                 w.newline();
             }
             for (int i = 0; i < type.fields().size(); i++) {
                 if (i != 0)
                     w.newline();
                 FieldInstance f = (FieldInstance) type.fields().get(i);
                 if (f.flags().isStatic() || query.isSyntheticField(f.name().toString()))
                     continue;
                 if (f.flags().isTransient()) // don't serialize transient fields of classes
                     continue;
                 String fieldName = mangled_field_name(f.name().toString());
                 w.write(fieldName+" = buf.read"+chevrons(translateType(f.type(),true))+"();");
             }
         }
         w.end(); w.newline();
         w.write("}");
         w.newline();
         w.forceNewline();
     }
 
     void generateStructSerializationMethods(ClassType type, StreamWrapper sw) {
         X10ClassType ct = (X10ClassType) type.toClass();
         TypeSystem ts = (TypeSystem) type.typeSystem();
         X10CPPContext_c context = (X10CPPContext_c) tr.context();
         ClassifiedStream w = sw.body();
         ClassifiedStream sh = context.structHeader;
         String klass = translateType(type);
  
         sh.forceNewline();
         
         // _serialize()
         sh.write("static void "+SERIALIZE_METHOD+"("+klass+" this_, "+SERIALIZATION_BUFFER+"& buf);");
         sh.newline(0); sh.forceNewline();
 
         printTemplateSignature(ct.x10Def().typeParameters(), w);
         w.write("void "+klass+"::"+SERIALIZE_METHOD+"("+klass+" this_, "+SERIALIZATION_BUFFER+"& buf) {");
         w.newline(4); w.begin(0);
         Type parent = type.superClass();
         if (parent != null) {
             w.write(translateType(parent)+"::"+SERIALIZE_METHOD+"(this_, buf);");
             w.newline();
         }
         for (int i = 0; i < type.fields().size(); i++) {
             if (i != 0)
                 w.newline();
             FieldInstance f = (FieldInstance) type.fields().get(i);
             if (f.flags().isStatic() || query.isSyntheticField(f.name().toString()))
                 continue;
             if (f.flags().isTransient()) // don't serialize transient fields
                 continue;
             String fieldName = mangled_field_name(f.name().toString());
             w.write("buf.write(this_->"+fieldName+");"); w.newline();
         }
         w.end(); w.newline();
         w.write("}");
         w.newline(); w.forceNewline();
 
         // _deserialize()
         sh.write("static "+klass+" "+DESERIALIZE_METHOD+"("+DESERIALIZATION_BUFFER+"& buf) {");
         sh.newline(4) ; sh.begin(0);
         sh.writeln(klass+" this_;");
         sh.write("this_->"+DESERIALIZE_BODY_METHOD+"(buf);"); sh.newline();
         sh.write("return this_;");
         sh.end(); sh.newline();
         sh.write("}"); sh.newline(); sh.forceNewline();
 
         // _deserialize_body()
         sh.write("void "+DESERIALIZE_BODY_METHOD+"("+DESERIALIZATION_BUFFER+"& buf);"); sh.newline(0);
         printTemplateSignature(ct.x10Def().typeParameters(), w);
         w.write("void "+klass+"::"+DESERIALIZE_BODY_METHOD+"("+DESERIALIZATION_BUFFER+"& buf) {");
         w.newline(4); w.begin(0);
         if (parent != null) {
             w.write(translateType(parent)+"::"+DESERIALIZE_BODY_METHOD+"(buf);");
             w.newline();
         }
         for (int i = 0; i < type.fields().size(); i++) {
             if (i != 0)
                 w.newline();
             FieldInstance f = (FieldInstance) type.fields().get(i);
             if (f.flags().isStatic() || query.isSyntheticField(f.name().toString()))
                 continue;
             if (f.flags().isTransient()) // don't serialize transient fields
                 continue;
             String fieldName = mangled_field_name(f.name().toString());
             w.write(fieldName+" = buf.read"+chevrons(translateType(f.type(),true))+"();");
         }
         w.end(); w.newline();
         w.write("}");
         w.newline();
         w.forceNewline();
     }
 	
 	public void emitUniqueNS(QName name, ArrayList<String> history, CodeWriter w) {
 		if (name == null) return;
 		if  (!history.contains(name.toString())) {
 			openNamespaces(w, name);
 			closeNamespaces(w, name);
 			w.newline();
 			w.write("using namespace "+translateFQN(name.toString())+";");
 			history.add(name.toString());
 		}
 		return;
 	}
 
     public static void openNamespaces(CodeWriter h, QName name) {
         if (name == null) return;
         openNamespaces(h, name.qualifier());
         h.write("namespace "+mangle_to_cpp(name.name().toString())+" { ");
     }
     
     public static void closeNamespaces(CodeWriter h, QName name) {
         if (name == null) return;
         h.write("} ");
         closeNamespaces(h, name.qualifier());
     }
 
 	public String makeUnsignedType(Type t) {
 		// FIXME: HACK!
 		if (t.isInt())
 			return "uint32_t";
 		if (t.isLong())
 			return "uint64_t";
 		return "unsigned "+translateType(t);
 	}
 
 	public static void dumpString(String str, CodeWriter w) {
 	    int pos = 0;
 	    int nl = 0;
 	    while ((nl = str.indexOf('\n', pos)) != -1) {
 	        w.write(str.substring(pos, nl));
 	        w.newline(0);
 	        pos = nl+1;
 	    }
 	    w.write(str.substring(pos));
 	}
 
 	// Either #SOME_VAR123
 	// or ##SOME_VAR123#default value if var that key is not known#
 	// the second variation is only useful for properties
     private static final Pattern nativeSubstRegex = Pattern.compile("#(#(([0-9A-Za-z_]+)#([^#]*)#)|([0-9A-Za-z_]+))");
 
     private static String nativeSubst(String annotation, Map<String,Object> components, String pattern) {
         Matcher m = nativeSubstRegex.matcher(pattern);
         int last=0;
         StringBuffer out = new StringBuffer();
         while (m.find()) {
             out.append(pattern.substring(last,m.start()));
             last = m.end();
             String key = m.group(5);
             String default_ = null;
             if (key==null) {
             	// ## form
                 key = m.group(3);
                 default_ = m.group(4);
             }
 
             Object val = components.get(key);
             if (val==null) val = default_;
             
             if (val==null) {
     			throw new InternalCompilerError(annotation+" \""+pattern+"\" cannot find substitution for #"+key);
             }
             if (val instanceof Type) {
                 out.append(translateType((Type)val, true));
             } else {
                 out.append(val);
             }
         }
         out.append(pattern.substring(last));
         return out.toString();
 	}	
 	
 	public void nativeSubst(String annotation, Map<String, Object> components, Translator tr, String pattern, CodeWriter w) {
 	
         Matcher m = nativeSubstRegex.matcher(pattern);
         int last=0;
         while (m.find()) {
         	w.write(pattern.substring(last,m.start()));
             last = m.end();
             String key = m.group(5);
             String default_ = null;
             if (key==null) {
             	// ## form
                 key = m.group(3);
                 default_ = m.group(4);
             }
 
             Object val = components.get(key);
             if (val==null) val = default_;
 
             if (val==null) {
     			throw new InternalCompilerError(annotation+" \""+pattern+"\" cannot find substitution for #"+key);
             }
 	        if (val instanceof Node) {
 	            Node n = (Node) val;
 	            tr.print(null, n, w);
 	        } else if (val instanceof Type) {
 	            w.write(translateType((Type)val, true));
 	        } else if (val != null) {
 	            w.write(val.toString());
 	        }
         }
         w.write(pattern.substring(last));
 	}
 
 }
 // vim:tabstop=4:shiftwidth=4:expandtab
