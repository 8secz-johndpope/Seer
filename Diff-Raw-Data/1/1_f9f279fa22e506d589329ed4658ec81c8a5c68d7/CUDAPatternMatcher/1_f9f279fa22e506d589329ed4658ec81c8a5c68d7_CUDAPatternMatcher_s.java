 package x10cuda.visit;
 
 import java.util.List;
 
 import polyglot.ast.Binary;
 import polyglot.ast.Block;
 import polyglot.ast.Block_c;
 import polyglot.ast.Call;
 import polyglot.ast.CanonicalTypeNode;
 import polyglot.ast.Eval;
 import polyglot.ast.Expr;
 import polyglot.ast.For;
 import polyglot.ast.Formal;
 import polyglot.ast.IntLit;
 import polyglot.ast.Local;
 import polyglot.ast.LocalDecl;
 import polyglot.ast.Node;
 import polyglot.ast.NodeFactory;
 import polyglot.ast.Receiver;
 import polyglot.ast.Stmt;
 import polyglot.ast.Try;
 import polyglot.ast.TypeNode;
 import polyglot.frontend.Job;
 import polyglot.types.Name;
 import polyglot.types.QName;
 import polyglot.types.SemanticException;
 import polyglot.types.Type;
 import polyglot.types.TypeSystem;
 import polyglot.util.ErrorInfo;
 import polyglot.visit.ContextVisitor;
 import polyglot.visit.NodeVisitor;
 import x10.ast.Async;
 import x10.ast.AtStmt;
 import x10.ast.Closure;
 import x10.ast.Finish;
 import x10.ast.RegionMaker;
 import x10.ast.X10Binary_c;
 import x10.ast.X10Call;
 import x10.ast.X10Call_c;
 import x10.ast.X10Formal;
 import x10.ast.X10Loop;
 import x10.ast.X10Loop_c;
 import x10.ast.X10New_c;
 import x10.extension.X10Ext;
 import x10.types.X10ClassType;
 import x10.types.X10TypeSystem_c;
 import x10cpp.visit.Emitter;
 import x10cuda.types.CUDAData;
 import x10cuda.types.SharedMem;
import x10cuda.visit.CUDACodeGenerator.MultipleValues;
 
 public class CUDAPatternMatcher extends NodeVisitor {
 	
 	private static final String ANN_KERNEL = "x10.compiler.CUDA";
 	private static final String ANN_DIRECT_PARAMS = "x10.compiler.CUDADirectParams";
 	private Job job;
 	private TypeSystem ts;
 	private NodeFactory nf;
 	
 	public CUDAPatternMatcher(Job job, TypeSystem ts, NodeFactory nf) {
 		this.job = job;
 		this.ts = ts;
 		this.nf = nf;
 	}
 
 	private X10TypeSystem_c xts() {
 		return (X10TypeSystem_c)ts;
 	}
 
 	// Type from name
 	private Type getType(String name) throws SemanticException {
 		return (Type) xts().systemResolver().find(QName.make(name));
 	}
 
 	// does the block have the given annotation
 	private boolean nodeHasAnnotation(Node n, String ann) {
 		X10Ext ext = (X10Ext) n.ext();
 		try {
 			return !ext.annotationMatching(getType(ann)).isEmpty();
 		} catch (SemanticException e) {
 			assert false : e;
 			return false; // in case asserts are off
 		}
 	}
 
 	// does the block have the annotation that denotes that it should be
 	// split-compiled to cuda?
 	private boolean blockIsKernel(Node n) {
 		return nodeHasAnnotation(n, ANN_KERNEL);
 	}
 
 	// does the block have the annotation that denotes that it should be
 	// compiled to use conventional cuda kernel params
 	private boolean kernelWantsDirectParams(Node n) {
 		return nodeHasAnnotation(n, ANN_DIRECT_PARAMS);
 	}
 	
 	private static class Complaint extends RuntimeException {
 	}
 
 	private void complainIfNot(boolean cond, String exp, Node n, boolean except) {
 		complainIfNot2(cond, "@CUDA Expected: " + exp, n, except);
 	}
 
 	private void complainIfNot2(boolean cond, String exp, Node n, boolean except) {
 		if (!cond) {
 			job.compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, exp, n.position());
 			if (except)
 				throw new Complaint();
 		}
 	}
 
 	private void complainIfNot(boolean cond, String exp, Node n) {
 		complainIfNot(cond, exp, n, true);
 	}
 
 	private void complainIfNot2(boolean cond, String exp, Node n) {
 		complainIfNot2(cond, exp, n, true);
 	}
 
 	private Type arrayCargo(Type typ) {
 		if (xts().isArray(typ)) {
 			typ = typ.toClass();
 			X10ClassType ctyp = (X10ClassType) typ;
 			assert ctyp.typeArguments() != null && ctyp.typeArguments().size() == 1; // Array[T]
 			return ctyp.typeArguments().get(0);
 		}
 		if (xts().isRemoteArray(typ)) {
 			typ = typ.toClass();
 			X10ClassType ctyp = (X10ClassType) typ;
 			assert ctyp.typeArguments() != null && ctyp.typeArguments().size() == 1; // RemoteRef[Array[T]]
 			Type type2 = ctyp.typeArguments().get(0);
 			X10ClassType ctyp2 = (X10ClassType) typ;
 			assert ctyp2.typeArguments() != null && ctyp2.typeArguments().size() == 1; // Array[T]
 			return ctyp2.typeArguments().get(0);
 		}
 		return null;
 
 	}
 
 	private boolean isFloatArray(Type typ) {
 		Type cargo = arrayCargo(typ);
 		return cargo != null && cargo.isFloat();
 	}
 
 	private boolean isIntArray(Type typ) {
 		Type cargo = arrayCargo(typ);
 		return cargo != null && cargo.isInt();
 	}
 
 	// Java cannot return multiple values from a function
 	class MultipleValues {
 		public Expr max;
 
 		public Name var;
 
 		public Block body;
 	}
 
 	protected MultipleValues processLoop(Block b) {
 		Node loop_ = b.statements().get(0);
 		complainIfNot(loop_ instanceof X10Loop_c,
 				"A 1-dimensional iteration of the form 0..", loop_);
 		X10Loop_c loop = (X10Loop_c) loop_;
 
 		MultipleValues r = new MultipleValues();
 		Formal loop_formal = loop.formal();
 		complainIfNot(loop_formal instanceof X10Formal,
 				"Exploded point syntax", loop);
 		X10Formal loop_x10_formal = (X10Formal) loop_formal;
 		complainIfNot(loop_x10_formal.hasExplodedVars(),
 				"Exploded point syntax", loop_formal);
 		complainIfNot(loop_x10_formal.vars().size() == 1,
 				"A 1 dimensional iteration", loop_formal);
 		r.var = loop_x10_formal.vars().get(0).name().id();
 		Expr domain = loop.domain();
 		complainIfNot(domain instanceof RegionMaker,
 				"An iteration over a region literal of the form 0..", domain);
 		RegionMaker region = (RegionMaker) domain;
 		complainIfNot(region.name().toString().equals("makeRectangular"),
 				"An iteration over a region literal of the form 0..", domain);
 		Receiver target = region.target();
 		complainIfNot(target instanceof CanonicalTypeNode,
 				"An iteration over a region literal of the form 0..", target);
 		CanonicalTypeNode target_type_node = (CanonicalTypeNode) target;
 		complainIfNot(target_type_node.nameString().equals("Region"),
 				"An iteration over a region literal of the form 0..", target);
 		complainIfNot(region.arguments().size() == 2,
 				"An iteration over a region literal of the form 0..", region);
 		Expr from_ = region.arguments().get(0);
 		Expr to_ = region.arguments().get(1);
 		complainIfNot(from_ instanceof IntLit,
 				"An iteration over a region literal of the form 0..", from_);
 		IntLit from = (IntLit) from_;
 		complainIfNot(from.value() == 0,
 				"An iteration over a region literal of the form 0..", from_);
 		r.max = to_;
 		r.body = (Block) loop.body();
 		return r;
 	}
 
 	public static boolean checkStaticCall(Expr e, String class_name, String method_name, int args) {
 		try {
 			Call call = (Call) e;
 			if (!call.name().toString().equals(method_name))
 				return false;
 			CanonicalTypeNode async_target_type_node = (CanonicalTypeNode) (call
 					.target());
 			if (!async_target_type_node.nameString().equals(class_name))
 				return false;
 			if (call.arguments().size() != args)
 				return false;
 		} catch (ClassCastException exc) {
 			return false;
 		}
 		return true;
 	}
 
 	public static boolean checkStaticCall(Stmt s, String class_name, String method_name, int args) {
 		try {
 			Expr e = ((Eval) s).expr();
 			return checkStaticCall(e, class_name, method_name, args);
 		} catch (ClassCastException e) {
 			return false;
 		}
 	}
 
 	public static Stmt checkFinish(Stmt s, boolean clocked) {
 		try {
 			Finish s1 = (Finish) s;
 			if (s1.clocked() != clocked) return null;
 			return s1.body();
 		} catch (ClassCastException e) {
 			return null;
 		}
 	}
 
 	public static Block_c checkAsync(Stmt s, boolean clocked) {
 		try {
 			Async s1 = (Async) s;
 			if (s1.clocked() != clocked) return null;
 			return (Block_c)s1.body();
 		} catch (ClassCastException e) {
 			return null;
 		}
 	}
 	
 	public Node leave(Node parent, Node old, Node child, NodeVisitor visitor) {
 		if (child instanceof Block) {
 			if (blockIsKernel(child)) {
 				try {
 					//System.out.println("Got kernel: ");
 					//parent.prettyPrint(System.out);
 					//System.out.println();
 					Block_c kernel_block = (Block_c) child;
 					boolean direct = kernelWantsDirectParams(child);
 					complainIfNot2(parent instanceof AtStmt, "@CUDA annotation must be on an at body", kernel_block);
 					
 	
 					// if there are no autoblocks/threads statemnets, this will be 1
 					complainIfNot(kernel_block.statements().size() >= 1, "A block containing at least one statement.", kernel_block);
 	
 					LocalDecl autoBlocks = null, autoThreads = null;
 					
 					// handle autoblocks/autothreads and constant memory
 					// declarations
 					SharedMem cmem = new SharedMem();
 					for (int i = 0; i < kernel_block.statements().size() - 1; ++i) {
 						Stmt ld_ = kernel_block.statements().get(i);
 						complainIfNot( ld_ instanceof LocalDecl, "val <something> = <autoBlocks/Threads or constant cache definition", ld_);
 						LocalDecl ld = (LocalDecl) ld_;
 	
 						Expr init_expr = ld.init();
 						if (init_expr instanceof X10Call) {
 							
 							X10Call_c init_call = (X10Call_c) init_expr;
 		
 							Receiver init_call_target = init_call.target();
 							if (init_call_target instanceof CanonicalTypeNode) {
 								CanonicalTypeNode init_call_target_node = (CanonicalTypeNode) init_call_target;
 								
 								String classname = init_call_target_node.nameString();
 								int targs = init_call.typeArguments().size();
 								int args = init_call.arguments().size();
 								String methodname = init_call.name().toString();
 			
 								if (classname.equals("CUDAUtilities") && targs == 0 && args == 0 && methodname.equals("autoBlocks")) {
 									complainIfNot2(autoBlocks == null, "@CUDA: Already have autoBlocks", init_call);
 									autoBlocks = ld;
 								} else if (classname.equals("CUDAUtilities") && targs == 0 && args == 0 && methodname.equals("autoThreads")) {
 									complainIfNot2(autoThreads == null, "@CUDA: Already have autoThreads", init_call);
 									autoThreads = ld;
 								} else {
 									complainIfNot(false, "A call to CUDAUtilities.autoBlocks/autoThreads", init_call);
 								}
 							} else if (init_call_target instanceof Expr) {
 								Expr arr_ = (Expr) init_call_target;
 								complainIfNot(arr_ instanceof Local, "val <something> = some_array.sequence()", arr_);
 								Local arr = (Local) arr_;
 								complainIfNot(init_call.name().id().toString().equals("sequence"), "constant cache definition to call 'sequence'", init_expr);
 								Type cargo = arrayCargo(arr.type());
 								cmem.addArrayInitArray(ld, arr,  Emitter.translateType(cargo, true));
 							} else {
 								complainIfNot(
 										false,
 										"val <something> = CUDAUtilities.autoBlocks/Threads() or constant cache definition",
 										init_call_target);
 							}
 						/* Not doing this anymore because we're using sequences instead of array (constant cache is immutable)
 						} else {
 							complainIfNot(
 									init_expr instanceof X10New_c,
 									"val <something> = new Array(...)",
 									init_expr);
 							X10New_c init_new = (X10New_c) init_expr;
 							Type instantiatedType = init_new.objectType().type();
 							complainIfNot(xts().isArray(instantiatedType),
 									"Initialisation expression to have Array[T] type.",
 									init_new);
 							TypeNode rail_type_arg_node = init_new.typeArguments().get(
 									0);
 	
 							Type rail_type_arg = rail_type_arg_node.type();
 							String rail_type_arg_ = Emitter.translateType(rail_type_arg, true);
 							// TODO: support other types
 							if (init_new.arguments().size() == 2) {
 								Expr num_elements = init_new.arguments().get(0);
 								Expr rail_init_closure = init_new.arguments().get(1);
 								cmem.addArrayInitClosure(ld, num_elements,
 										rail_init_closure, rail_type_arg_);
 							} else {
 								complainIfNot(init_new.arguments().size() == 1,
 										"val <var> = new Array[T](other_array)",
 										init_new);
 								Expr src_array = init_new.arguments().get(0);
 								complainIfNot(
 										xts().isArray(src_array.type())
 												|| xts().isRemoteArray(src_array.type()),
 										"Constant memory to be initialised from array or remote array type",
 										src_array);
 								cmem.addArrayInitArray(ld, src_array, rail_type_arg_);
 							}
 						*/
 						}
 					}
 	
 					Stmt finish = kernel_block.statements().get(kernel_block.statements().size() - 1);
 					Stmt finish_body = checkFinish(finish, false);
 					complainIfNot(finish_body != null, "A finish statement", finish);
 					complainIfNot(finish_body instanceof Block,
 							"A single loop at the root of the kernel", finish_body);
 					Block finish_body_block = (Block) finish_body;
 					complainIfNot(finish_body_block.statements().size()==1,
 							"A single loop at the root of the CUDA kernel", finish_body_block);
 	
 					MultipleValues outer = processLoop(finish_body_block);
 					Block outer_b = (Block_c) outer.body;
 	
 					outer_b = (Block_c) checkAsync(outer_b.statements().get(0), false);
 					complainIfNot(outer_b != null, "An async for the block", outer.body);
 	
 					Stmt last = outer_b.statements().get(outer_b.statements().size() - 1);
 	
 					SharedMem shm = new SharedMem();
 					// look at all but the last statement to find shm decls
 					for (Stmt st : outer_b.statements()) {
 						if (st == last)
 							continue;
 						complainIfNot(st instanceof LocalDecl,
 								"Shared memory definition", st);
 						LocalDecl ld = (LocalDecl) st;
 						Expr init_expr = ld.init();
 						// TODO: primitive vals and shared vars
 						complainIfNot(init_expr instanceof X10New_c,
 								"val <var> = new Array[T](...)", init_expr);
 						X10New_c init_new = (X10New_c) init_expr;
 						Type instantiatedType = init_new.objectType().type();
 						complainIfNot(xts().isArray(instantiatedType),
 								"Initialisation expression to have Array[T] type.",
 								init_new);
 						TypeNode rail_type_arg_node = init_new.typeArguments().get(
 								0);
 	
 						Type rail_type_arg = rail_type_arg_node.type();
 						String rail_type_arg_ = Emitter.translateType(rail_type_arg, true);
 						// TODO: support other types
 						if (init_new.arguments().size() == 2) {
 							Expr num_elements = init_new.arguments().get(0);
 							Expr rail_init_closure = init_new.arguments().get(1);
 							shm.addArrayInitClosure(ld, num_elements,
 									rail_init_closure, rail_type_arg_);
 						} else {
 							complainIfNot(init_new.arguments().size() == 1,
 									"val <var> = new Array[T](other_array)",
 									init_new);
 							Expr src_array = init_new.arguments().get(0);
 							complainIfNot(
 									xts().isArray(src_array.type())
 											|| xts().isRemoteArray(src_array.type()),
 									"SHM to be initialised from array or remote array type",
 									src_array);
 							shm.addArrayInitArray(ld, src_array, rail_type_arg_);
 						}
 					}
 	
 					Stmt for_block2_ = checkFinish(last, true);
 					complainIfNot(for_block2_ != null,
 							"A clocked finish statement", last);
 					complainIfNot(for_block2_ instanceof Block,
 							"A loop over CUDA threads", for_block2_);
 					Block for_block2 = (Block) for_block2_;
 					MultipleValues inner = processLoop(for_block2);
 					Block inner_b = (Block_c) inner.body;
 	
 					complainIfNot(inner_b.statements().size() == 1,
 							"A block with a single statement", inner_b);
 					Stmt async = inner_b.statements().get(0);
 					Block_c async_body = checkAsync(async, true);
 	
 					int tag = CUDAData.fresh();
 					async_body.cudaTag(tag);
 
 					kernel_block = (Block_c) kernel_block.copy();
 					kernel_block.cudaData(new CUDAData());
 					kernel_block.cudaData().autoBlocks = autoBlocks;
 					kernel_block.cudaData().autoThreads = autoThreads;
 					kernel_block.cudaData().blocks = outer.max;
 					kernel_block.cudaData().blocksVar = outer.var;
 					kernel_block.cudaData().threads = inner.max;
 					kernel_block.cudaData().threadsVar = inner.var;
 					kernel_block.cudaData().shm = shm;
 					kernel_block.cudaData().cmem = cmem;
 					kernel_block.cudaData().directParams = direct;
 					kernel_block.cudaData().innerStatementTag = tag;
 					return kernel_block;
 				} catch (Complaint e) {
 					e.printStackTrace();
 				}
 			}
 		}
 		
 		return child;
 	}	
 }
