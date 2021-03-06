 package jkit.java;
 
 import java.io.*;
 import java.lang.reflect.Modifier;
 import java.util.*;
 
 import jkit.util.*; 
 
 public class JavaFileWriter {
 	private PrintWriter output;
 	private int depth = 0;
 	
 	public JavaFileWriter(Writer out) {
 		output = new PrintWriter(out);
 	}
 	
 	public JavaFileWriter(OutputStream out) {
 		output = new PrintWriter(out);
 	}
 	
 	public void write(JavaFile jf) {
 		if(jf.pkg() != null) {
 			output.println("package " + jf.pkg() + ";\n");
 		}				
 		
 		for(String imp : jf.imports()) {
 			output.println("import " + imp + ";");
 		}				
 		if(jf.imports().size() > 0) {
 			output.println("");
 		}
 		for(JavaFile.Clazz decl : jf.classes()) {
 			writeClass(decl);
 		}
 		
 		output.flush();
 	}
 	
 	public void writeClass(JavaFile.Clazz decl) {
 		
 		writeModifiers(decl.modifiers());
 		
 		if(decl.isInterface()) {
 			write("interface ");
 		} else {
 			write("class ");
 		}
 		
 		write(decl.name());
 		
 		if(decl.isInterface()) {
 			if(decl.interfaces().size() > 0) {
 				write(" extends ");
 				boolean firstTime = true;
 				for(JavaFile.Type i : decl.interfaces()) {
 					if(!firstTime) {
 						write(", ");
 					} else { firstTime = false; }
 					writeType(i);
 					write(" ");
 				}
 			}
 		} else {
 			if(decl.superclass() != null) {
 				write(" extends ");
 				writeType(decl.superclass());
 				write(" ");
 			}
 			if(decl.interfaces().size() > 0) {
 				write(" implements ");
 				boolean firstTime = true;
 				for(JavaFile.Type i : decl.interfaces()) {
 					if(!firstTime) {
 						write(", ");
 					} else { firstTime = false; }
 					writeType(i);
 					write(" ");
 				}
 			}
 		}
 		
 		write("{");
 		
 		for(JavaFile.Declaration d : decl.declarations()) {
 			if(d instanceof JavaFile.Clazz) {
 				writeClass((JavaFile.Clazz) d);
 			} else if(d instanceof JavaFile.Field) {
 				writeField((JavaFile.Field) d);
 			} else if(d instanceof JavaFile.Method) {
 				writeMethod((JavaFile.Method) d);
 			}
 		}
 		
 		write("}");
 	}
 	
 	protected void writeMethod(JavaFile.Method m) {
 		write("\n");
 		writeModifiers(m.modifiers());
 		if(m.returnType() != null) {
 			// can be null if this method is actually a constructor.
 			writeType(m.returnType());
 			write(" ");
 		}		
 		write(m.name());
 		write("(");
 		boolean firstTime=true;
 		for(Pair<String,JavaFile.Type> p : m.parameters()) {
 			if(!firstTime) {
 				write(", ");				
 			}
 			firstTime=false;
 			writeType(p.second());
 			write(" ");
 			write(p.first());
 		}
 		write(")");
 		
 		if(m.exceptions().size() > 0) {
 			write("throws ");
 			firstTime=true;
 			for(JavaFile.Type t : m.exceptions()) {
 				if(!firstTime) {
 					write(", ");				
 				}
 				firstTime=false;
 				writeType(t);
 			}
 		}
 				
 		if(m.block() != null) { 
 			writeBlock(m.block()); 
 		} else {
 			write(";");
 		}
 	}
 	
 	protected void writeField(JavaFile.Field f) {
 		
 		writeModifiers(f.modifiers());		
 		writeType(f.type());
 		write(" ");
 		write(f.name());
 		if(f.initialiser() != null) {
 			write(" = ");
 			writeExpression(f.initialiser());
 		}
 		write(";");
 	}
 	
 	protected void writeSubStatement(JavaFile.Statement e) {
 		if(e instanceof JavaFile.Block) {
 			writeStatement(e);
 		} else {
 			depth = depth + 1;
 			write("\n");
 			writeStatement(e);
 			depth = depth - 1;
 			if(e instanceof JavaFile.SimpleStatement) {
 				write(";");
 			}			
 		}
 	}
 	
 	protected void writeStatement(JavaFile.Statement e) {
 		if(e instanceof JavaFile.SynchronisedBlock) {
 			writeSynchronisedBlock((JavaFile.SynchronisedBlock)e);
 		} else if(e instanceof JavaFile.TryCatchBlock) {
 			writeTryCatchBlock((JavaFile.TryCatchBlock)e);
 		} else if(e instanceof JavaFile.Block) {
 			writeBlock((JavaFile.Block)e);
 		} else if(e instanceof JavaFile.VarDef) {
 			writeVarDef((JavaFile.VarDef) e);
 		} else if(e instanceof JavaFile.Assignment) {
 			writeAssignment((JavaFile.Assignment) e);
 		} else if(e instanceof JavaFile.Return) {
 			writeReturn((JavaFile.Return) e);
 		} else if(e instanceof JavaFile.Throw) {
 			writeThrow((JavaFile.Throw) e);
 		} else if(e instanceof JavaFile.Assert) {
 			writeAssert((JavaFile.Assert) e);
 		} else if(e instanceof JavaFile.Break) {
 			writeBreak((JavaFile.Break) e);
 		} else if(e instanceof JavaFile.Continue) {
 			writeContinue((JavaFile.Continue) e);
 		} else if(e instanceof JavaFile.Label) {
 			writeLabel((JavaFile.Label) e);
 		} else if(e instanceof JavaFile.If) {
 			writeIf((JavaFile.If) e);
 		} else if(e instanceof JavaFile.For) {
 			writeFor((JavaFile.For) e);
 		} else if(e instanceof JavaFile.ForEach) {
 			writeForEach((JavaFile.ForEach) e);
 		} else if(e instanceof JavaFile.While) {
 			writeWhile((JavaFile.While) e);
 		} else if(e instanceof JavaFile.DoWhile) {
 			writeDoWhile((JavaFile.DoWhile) e);
 		} else if(e instanceof JavaFile.Switch) {
 			writeSwitch((JavaFile.Switch) e);
 		} else if(e instanceof JavaFile.Invoke) {
 			writeInvoke((JavaFile.Invoke) e);
 		} else if(e instanceof JavaFile.New) {
 			writeNew((JavaFile.New) e);
 		} else {
 			throw new RuntimeException("Invalid statement encountered: "
 					+ e.getClass());
 		}
 	}
 	
 	protected void writeBlock(JavaFile.Block block) {		
 		write("{");
 		for(JavaFile.Statement s : block.statements()) {
 			writeStatement(s);		
 			if(s instanceof JavaFile.SimpleStatement) {
 				write(";");
 			}
 		}
 		write("}");
 	}
 	
 	protected void writeSynchronisedBlock(JavaFile.SynchronisedBlock block) {
 		
 		write("synchronized(");
 		writeExpression(block.expr());
 		write(")");
 		write("{");
 		for(JavaFile.Statement s : block.statements()) {
 			writeStatement(s);
 			if(s instanceof JavaFile.SimpleStatement) {
 				write(";");
 			}
 		}
 		write("}");
 	}
 	
 	protected void writeTryCatchBlock(JavaFile.TryCatchBlock block) {
 		
 		write("try ");		
 		write("{");
 		for(JavaFile.Statement s : block.statements()) {
 			writeStatement(s);
 			if(s instanceof JavaFile.SimpleStatement) {
 				write(";");
 			}
 		}
 		write("}");
 		
 		for(JavaFile.CatchBlock c : block.handlers()) {
 			write(" catch(");
 			writeType(c.type());
 			write(" ");
 			write(c.variable());
 			write(")");
 			write("{");
 			for(JavaFile.Statement s : c.statements()) {
 				writeStatement(s);
 				if(s instanceof JavaFile.SimpleStatement) {
 					write(";");
 				}
 			}	
 			 write("}");
 		}
 	}
 	
 	protected void writeVarDef(JavaFile.VarDef def) {				
 		for(Triple<String,JavaFile.Type,JavaFile.Expression> d : def.definitions()) {			
 			
 			writeModifiers(def.modifiers());
 			writeType(d.second());
 			write(" ");
 			write(d.first());
 			if(d.third() != null) {
 				write(" = ");
 				writeExpression(d.third());
 			}
 		}
 	}
 	
 	protected void writeAssignment(JavaFile.Assignment def) {
 		
 		writeExpression(def.lhs());
 		write(" = ");
 		writeExpression(def.rhs());		
 	}
 	
 	protected void writeReturn(JavaFile.Return ret) {
 				
 		write("return");
 		if(ret.expr() != null) {
 			write(" ");
 			writeExpression(ret.expr());
 		}
 	}
 	
 	protected void writeThrow(JavaFile.Throw ret) {
 				
 		write("throw ");				
 		writeExpression(ret.expr());		
 	}
 	
 	protected void writeAssert(JavaFile.Assert ret) {
 				
 		write("assert ");				
 		writeExpression(ret.expr());				
 	}
 	
 	protected void writeBreak(JavaFile.Break brk) {
 				
 		write("break");
 		if(brk.label() != null) {
 			write(" ");
 			write(brk.label());
 		}		
 	}
 	
 	protected void writeContinue(JavaFile.Continue brk) {
 				
 		write("continue");
 		if(brk.label() != null) {
 			write(" ");
 			write(brk.label());
 		}		
 	}
 	
 	protected void writeLabel(JavaFile.Label lab) {				
 		write(lab.label());
 		write(": ");
 		writeStatement(lab.statement());
 	}
 	
 	protected void writeIf(JavaFile.If stmt) {
 		
 		write("if(");
 		writeExpression(stmt.condition());
 		write(") ");
 		writeSubStatement(stmt.trueStatement());		
 		
 		if (stmt.falseStatement() != null) {
 			if(!(stmt.trueStatement() instanceof JavaFile.Block)) {
 				write("\n");
 			}
 			write("else");	
 			writeSubStatement(stmt.falseStatement());			
 		}
 	}
 	
 	protected void writeWhile(JavaFile.While stmt) {
 		write("while(");
 		writeExpression(stmt.condition());
 		write(") ");
 		if (stmt.body() != null) {
 			writeSubStatement(stmt.body());			
 		} else {
 			write(";");
 		}		
 	}
 	
 	protected void writeDoWhile(JavaFile.DoWhile stmt) {
 		
 		write("do ");
 		writeSubStatement(stmt.body());
 		write("while(");
 		writeExpression(stmt.condition());
 		write(");");
 	}
 	
 	protected void writeFor(JavaFile.For stmt) {
 		
 		write("for(");
		if(stmt.initialiser() != null) {
 			writeStatement(stmt.initialiser());
 		} 
 		
 		write("; ");
 				
 		if(stmt.condition() != null) {
 			writeExpression(stmt.condition());
 		} 
 		
 		write("; ");
 		
 		if(stmt.increment() != null) {
 			writeStatement(stmt.increment());
 		}
 		write(")");
 		
 		if(stmt.body() != null) {
 			writeSubStatement(stmt.body());					
 		} else {
 			write(";");
 		}
 	}
 	
 	protected void writeForEach(JavaFile.ForEach stmt) {
 		write("for(");
 		writeModifiers(stmt.modifiers());
 		write(" ");
 		writeType(stmt.type());
 		write(" ");
 		write(stmt.var());
 		write(" : ");
 		writeExpression(stmt.source());
 		write(")");
 		
 		if (stmt.body() != null) {
 			writeSubStatement(stmt.body());			
 		} else {
 			write(";");
 		}
 	}
 	
 	protected void writeSwitch(JavaFile.Switch s) {
 		write("switch(");
 		writeExpression(s.condition());
 		write(")");
 		write("{");
 		for(JavaFile.Case c : s.cases()) {
 			if(c instanceof JavaFile.DefaultCase) {
 				write("default:");
 				write("\n");
 			} else {
 				writeExpression(c.condition());
 				write(":");
 				write("\n");
 			}			
 			for(JavaFile.Statement x : c.statements()) {
 				writeStatement(x);
 				if(x instanceof JavaFile.SimpleStatement) {
 					write(";");
 				}
 			}
 		}
 		write("}");
 	}
 	
 	protected void writeExpression(JavaFile.Expression e) {
 		
 		if(e instanceof JavaFile.BoolVal) {
 			writeBoolVal((JavaFile.BoolVal)e);
 		} else if(e instanceof JavaFile.CharVal) {
 			writeCharVal((JavaFile.CharVal)e);
 		} else if(e instanceof JavaFile.IntVal) {
 			writeIntVal((JavaFile.IntVal)e);
 		} else if(e instanceof JavaFile.LongVal) {
 			writeLongVal((JavaFile.LongVal)e);
 		} else if(e instanceof JavaFile.FloatVal) {
 			writeFloatVal((JavaFile.FloatVal)e);
 		} else if(e instanceof JavaFile.DoubleVal) {
 			writeDoubleVal((JavaFile.DoubleVal)e);
 		} else if(e instanceof JavaFile.StringVal) {
 			writeStringVal((JavaFile.StringVal)e);
 		} else if(e instanceof JavaFile.NullVal) {
 			writeNullVal((JavaFile.NullVal)e);
 		} else if(e instanceof JavaFile.ArrayVal) {
 			writeArrayVal((JavaFile.ArrayVal)e);
 		} else if(e instanceof JavaFile.ClassVal) {
 			writeClassVal((JavaFile.ClassVal) e);
 		} else if(e instanceof JavaFile.Variable) {
 			writeVariable((JavaFile.Variable)e);
 		} else if(e instanceof JavaFile.UnOp) {
 			writeUnOp((JavaFile.UnOp)e);
 		} else if(e instanceof JavaFile.BinOp) {
 			writeBinOp((JavaFile.BinOp)e);
 		} else if(e instanceof JavaFile.TernOp) {
 			writeTernOp((JavaFile.TernOp)e);
 		} else if(e instanceof JavaFile.Cast) {
 			writeCast((JavaFile.Cast)e);
 		} else if(e instanceof JavaFile.InstanceOf) {
 			writeInstanceOf((JavaFile.InstanceOf)e);
 		} else if(e instanceof JavaFile.Invoke) {
 			writeInvoke((JavaFile.Invoke) e);
 		} else if(e instanceof JavaFile.New) {
 			writeNew((JavaFile.New) e);
 		} else if(e instanceof JavaFile.ArrayIndex) {
 			writeArrayIndex((JavaFile.ArrayIndex) e);
 		} else if(e instanceof JavaFile.Deref) {
 			writeDeref((JavaFile.Deref) e);
 		} else {
 			throw new RuntimeException("Invalid expression encountered: "
 					+ e.getClass());
 		}
 	}
 	
 	protected void writeDeref(JavaFile.Deref e) {
 		writeExpression(e.target());
 		write(".");
 		write(e.name());		
 	}
 	
 	protected void writeArrayIndex(JavaFile.ArrayIndex e) {
 		writeExpression(e.target());
 		write("[");
 		writeExpression(e.index());
 		write("]");
 	}
 	
 	protected void writeNew(JavaFile.New e) {
 		write("new ");
 		if(e.type().dims() > 0) {
 			// array initialiser
 			List<JavaFile.Expression> ps = e.parameters();
 			
 			boolean firstTime = true;
 			for (Pair<String, List<JavaFile.Type>> c : e.type().components()) {
 				if (!firstTime) {
 					write(".");
 				} else {
 					firstTime = false;
 				}
 				write(c.first());
 				
 				if (!c.second().isEmpty()) {
 					// yes, there are generic parameters as well.
 					write("<");
 					firstTime = true;
 					for (JavaFile.Type t : c.second()) {
 						if (!firstTime) {
 							write(",");
 						} else {
 							firstTime = false;
 						}
 						writeType(t);
 					}
 					write(">");
 				}
 			}
 			
 			for(int i=0;i!=e.type().dims();++i) {
 				if(i < ps.size()) {
 					write("[");
 					writeExpression(ps.get(i));
 					write("]");
 				} else {
 					write("[]");
 				}
 			}
 		} else {
 			writeType(e.type());
 			write("(");
 			boolean firstTime=true;
 			for(JavaFile.Expression i : e.parameters()) {
 				if(!firstTime) {
 					write(", ");
 				} else {
 					firstTime = false;
 				}
 				writeExpression(i);
 			}
 			write(")");
 
 			if(e.declarations().size() > 0) {
 				write(" { ");
 				for(JavaFile.Declaration d : e.declarations()) {				
 					if(d instanceof JavaFile.Clazz) {
 						writeClass((JavaFile.Clazz) d);
 					} else if(d instanceof JavaFile.Field) {
 						writeField((JavaFile.Field) d);
 					} else if(d instanceof JavaFile.Method) {
 						writeMethod((JavaFile.Method) d);
 					} else {
 						throw new RuntimeException(
 						"Support required for methods in anonymous inner classes");
 					}
 				}
 				write(" } ");
 			}
 		}
 	}
 	
 	protected void writeInvoke(JavaFile.Invoke e) {
 		if(e.target() != null) {
 			writeExpression(e.target());
 			write(".");
 		}
 		write(e.name());
 		if(e.typeParameters().size() > 0) {
 			write("<");
 			boolean firstTime=true;
 			for(JavaFile.Type i : e.typeParameters()) {
 				if(!firstTime) {
 					write(",");
 				} else {
 					firstTime = false;
 				}
 				writeType(i);
 			}	
 			write(">");
 		}
 		write("(");
 		boolean firstTime=true;
 		for(JavaFile.Expression i : e.parameters()) {
 			if(!firstTime) {
 				write(", ");
 			} else {
 				firstTime = false;
 			}
 			writeExpression(i);
 		}
 		write(")");
 	}
 	
 	protected void writeInstanceOf(JavaFile.InstanceOf e) {		
 		writeExpression(e.lhs());
 		write(" instanceof ");
 		writeType(e.rhs());		
 	}
 	
 	protected void writeCast(JavaFile.Cast e) {
 		write("(");
 		writeType(e.type());
 		write(") ");
 		writeExpression(e.expr());
 	}
 	
 	protected void writeBoolVal(JavaFile.BoolVal e) {
 		if(e.value()) {
 			write("true");
 		} else {
 			write("false");
 		}
 	}
 	
 	protected void writeCharVal(JavaFile.CharVal e) {
 		write("'");
 		writeWithEscapes(Character.toString(e.value()));		
 		write("'");
 	}
 	
 	protected void writeIntVal(JavaFile.IntVal e) {		
 		write(Integer.toString(e.value()));
 	}
 	
 	protected void writeLongVal(JavaFile.LongVal e) {		
 		write(Long.toString(e.value()) + "L");
 	}
 	
 	protected void writeFloatVal(JavaFile.FloatVal e) {		
 		write(Float.toString(e.value()) + "F");
 	}
 	
 	protected void writeDoubleVal(JavaFile.DoubleVal e) {		
 		write(Double.toString(e.value()));
 	}
 	
 	protected void writeStringVal(JavaFile.StringVal e) {		
 		write("\"");
 		writeWithEscapes(e.value());
 		write("\"");
 	}
 	
 	protected void writeNullVal(JavaFile.NullVal e) {		
 		write("null");
 	}
 	
 	protected void writeArrayVal(JavaFile.ArrayVal e) {		
 		boolean firstTime = true;
 		write("{");
 		for(JavaFile.Expression i : e.values()) {
 			if(!firstTime) {
 				write(", ");
 			} else {
 				firstTime = false;
 			}
 			writeExpression(i);
 		}
 		write("}");
 	}
 	
 	protected void writeClassVal(JavaFile.ClassVal e) {
 		writeType(e.value());
 		write(".class");
 	}
 	
 	protected void writeVariable(JavaFile.Variable e) {			
 		write(e.value());		
 	}
 	
 	public static final String[] unopstr={"!","~","-","++","--","++","--"};
 	
 	protected void writeUnOp(JavaFile.UnOp e) {		
 		
 		if(e.op() != JavaFile.UnOp.POSTDEC && e.op() != JavaFile.UnOp.POSTINC) {
 			write(unopstr[e.op()]);
 		}
 				
 		writeExpressionWithBracketsIfNecessary(e.expr());					
 		
 		if (e.op() == JavaFile.UnOp.POSTDEC || e.op() == JavaFile.UnOp.POSTINC) {
 			write(unopstr[e.op()]);
 		}
 	}
 	
 
 	protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
 			">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
 			"||", "++"};
 	
 	
 	protected void writeBinOp(JavaFile.BinOp e) {				
 		writeExpressionWithBracketsIfNecessary(e.lhs());					
 		write(" ");
 		write(binopstr[e.op()]);
 		write(" ");
 		writeExpressionWithBracketsIfNecessary(e.rhs());						
 	}
 	
 	protected void writeTernOp(JavaFile.TernOp e) {		
 		writeExpressionWithBracketsIfNecessary(e.condition());		
 		write(" ? ");
 		writeExpressionWithBracketsIfNecessary(e.trueBranch());
 		write(" : ");
 		writeExpressionWithBracketsIfNecessary(e.falseBranch());
 	}
 	
 	protected void writeExpressionWithBracketsIfNecessary(JavaFile.Expression e) {
 		if (e instanceof JavaFile.BinOp || e instanceof JavaFile.InstanceOf) {
 			write("(");
 			writeExpression(e);
 			write(")");
 		} else {
 			writeExpression(e);
 		}
 	}
 	
 	protected void writeType(JavaFile.Type t) {
 		boolean firstTime=true;
 		for(Pair<String,List<JavaFile.Type>> c : t.components()) {
 			if(!firstTime) {
 				write(".");
 			} else {
 				firstTime=false;
 			}
 			write(c.first());			
 			if (!c.second().isEmpty()) {
 				// yes, there are generic parameters as well.
 				write("<");
 				firstTime = true;
 				for (JavaFile.Type d : c.second()) {
 					if (!firstTime) {
 						write(",");
 					} else {
 						firstTime = false;
 					}
 					writeType(d);
 				}
 				write(">");
 			}
 		}
 		for(int i=0;i!=t.dims();++i) {
 			write("[]");
 		}		
 	}
 	
 	protected void writeModifiers(int modifiers) {
 		if((modifiers & Modifier.PRIVATE)!=0) { write("private "); }
 		if((modifiers & Modifier.PROTECTED)!=0) { write("protected "); }
 		if((modifiers & Modifier.PUBLIC)!=0) { write("public "); }
 		if((modifiers & Modifier.STATIC)!=0) { write("static "); }
 		if((modifiers & Modifier.ABSTRACT)!=0) { write("abstract "); }
 		if((modifiers & Modifier.FINAL)!=0) { write("final "); }
 		if((modifiers & Modifier.NATIVE)!=0) { write("native "); }				
 		if((modifiers & Modifier.STRICT)!=0) { write("strict "); }
 		if((modifiers & Modifier.SYNCHRONIZED)!=0) { write("synchronized "); }
 		if((modifiers & Modifier.TRANSIENT)!=0) { write("transient "); }
 		if((modifiers & Modifier.VOLATILE)!=0) { write("volatile "); }
 	}
 	
 	protected void writeWithEscapes(String s) {
 		for(int i=0;i!=s.length();++i) {
 			char c = s.charAt(i);
 			switch (c) {
 			case '\b':
 				output.write("\\b");
 				break;
 			case '\t':
 				output.write("\\t");
 				break;
 			case '\f':
 				output.write("\\f");
 				break;
 			case '\n':
 				output.write("\\n");
 				break;
 			case '\r':
 				output.write("\\r");
 				break;
 			case '\"':
 				output.write("\\\"");
 				break;
 			case '\\':
 				output.write("\\");
 				break;
 			case '\'':		
 				output.write("\\'");
 				break;
 			default:
 				if(c >= 32 && c < 128) {
 					output.write(Character.toString(c));
 				} else {
 					String str = Integer.toString(c,16);
 					int padding = 4 - str.length();
 					for(int k=0;k!=padding;++k) {
 						str = "0" + str;
 					}
 					output.write("\\u" + str);
 				}
 			}
 		}
 	}
 	
 	protected void write(String s) {		
 		if(s.equals(";")) {
 			output.println(";");
 			indent(depth);
 		} else if(s.equals("{")) {
 			output.println(" {");
 			depth = depth + 1;
 			indent(depth);
 		} else if(s.equals("}")) {
 			output.println("}");
 			depth = depth - 1;
 			indent(depth);
 		} else if(s.equals("\n")) {
 			output.print("\n");
 			indent(depth);
 		} else {
 			output.write(s);
 		}
 	}
 	
 	protected void indent(int level) {
 		for(int i=0;i<level;++i) {
 			output.print("  ");
 		}
 	}
 }
