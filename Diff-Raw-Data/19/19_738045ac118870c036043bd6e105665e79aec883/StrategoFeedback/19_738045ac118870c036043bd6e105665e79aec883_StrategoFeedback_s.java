 package org.strategoxt.imp.runtime.services;
 
 import static org.spoofax.interpreter.terms.IStrategoTerm.*;
 import static org.strategoxt.imp.runtime.dynamicloading.TermReader.*;
 
 import java.io.FileNotFoundException;
 
 import lpg.runtime.IAst;
 
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.imp.parser.IModelListener;
 import org.eclipse.imp.parser.IParseController;
 import org.spoofax.interpreter.core.Interpreter;
 import org.spoofax.interpreter.core.InterpreterException;
 import org.spoofax.interpreter.terms.IStrategoList;
 import org.spoofax.interpreter.terms.IStrategoString;
 import org.spoofax.interpreter.terms.IStrategoTerm;
 import org.spoofax.interpreter.terms.ITermFactory;
 import org.strategoxt.imp.runtime.Debug;
 import org.strategoxt.imp.runtime.Environment;
 import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
 import org.strategoxt.imp.runtime.parser.ast.AstMessageHandler;
 import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
 import org.strategoxt.imp.runtime.stratego.StrategoTermPath;
 import org.strategoxt.imp.runtime.stratego.adapter.IStrategoAstNode;
 import org.strategoxt.imp.runtime.stratego.adapter.IWrappedAstNode;
 import org.strategoxt.imp.runtime.stratego.adapter.WrappedAstNode;
 
 /**
  * Basic Stratego feedback (i.e., errors and warnings) provider.
  * 
  * @author Lennart Kats <lennart add lclnet.nl>
  */
 public class StrategoFeedback implements IModelListener {
 	private final Descriptor descriptor;
 	
 	private final Interpreter interpreter;
 	
 	private final String feedbackFunction;
 	
 	private final AstMessageHandler messages = new AstMessageHandler();
 	
 	public StrategoFeedback(Descriptor descriptor, Interpreter resolver, String feedbackFunction) {
 		this.descriptor = descriptor;
 		this.interpreter = resolver;
 		this.feedbackFunction = feedbackFunction;
 	}
 
 	public AnalysisRequired getAnalysisRequired() {
 		return AnalysisRequired.TYPE_ANALYSIS;
 	}
 
 	public void update(IParseController parseController, IProgressMonitor monitor) {
 		
 		// TODO: Threading of feedback method
 		
 		if (feedbackFunction != null) {
 			ITermFactory factory = Environment.getTermFactory();
 			IStrategoAstNode ast = (IStrategoAstNode) parseController.getCurrentAst();
 			if (ast == null) return;
 			
 			IStrategoTerm[] inputParts = {
 					ast.getTerm(),
 					factory.makeString(ast.getResourcePath().toOSString()),
 					factory.makeString(ast.getRootPath().toOSString())
 			};
 			IStrategoTerm input = factory.makeTuple(inputParts);
 
 			IStrategoTerm feedback = invoke(feedbackFunction, input, ast.getResourcePath().removeLastSegments(1));
 			
 			messages.clearAllMarkers();
 
 	        if (feedback != null
 	        		&& feedback.getTermType() == TUPLE
 	        		&& termAt(feedback, 0).getTermType() == LIST
 					&& termAt(feedback, 1).getTermType() == LIST
 					&& termAt(feedback, 2).getTermType() == LIST) {
 	        	
 	            IStrategoList errors = termAt(feedback, 0);
                 IStrategoList warnings = termAt(feedback, 1);
 	            feedbackToMarkers(parseController, errors, IMarker.SEVERITY_ERROR);
                 feedbackToMarkers(parseController, warnings, IMarker.SEVERITY_WARNING);
 	        } else {
 	            Environment.logException("Illegal output from " + feedbackFunction + ": " + feedback);
 	        }
 			
 		}
 	}
 	
 	public final void feedbackToMarkers(IParseController parseController, IStrategoList feedbacks, int severity) {
 	    for (IStrategoTerm feedback : feedbacks.getAllSubterms()) {
 	        feedbackToMarker(parseController, feedback, severity);
 	    }
 	}
 	
 	public void feedbackToMarker(IParseController parseController, IStrategoTerm feedback, int severity) {
 	    IStrategoTerm term = termAt(feedback, 0);
 	    IStrategoString message = termAt(feedback, 1);
 	    IAst node = getClosestAstNode(term);
 	    
 	    if (node == null) {
 	    	Environment.logException("ATerm is not associated with an AST node, cannot report feedback message: " + term + " - " + message);
 	    } else {
 	    	messages.addMarker(node, message.stringValue(), severity);
 	    }
 	}
 	
 	/**
 	 * Given an stratego term, give the first AST node associated
 	 * with any of its subterms, doing a depth-first search.
 	 */
 	private static IAst getClosestAstNode(IStrategoTerm term) {
 	    if (term instanceof IWrappedAstNode) {
 	        return ((IWrappedAstNode) term).getNode();
 	    } else {
 	        for (int i = 0; i < term.getSubtermCount(); i++) {
 	        	IAst result = getClosestAstNode(termAt(term, i));
 	            if (result != null) return result;
 	        }
 	        return null;
 	    }
 	}
 	
 	/**
 	 * Invoke a Stratego function with a specific AST node as its input.
 	 * 
 	 * @see #getAstNode(IStrategoTerm)  To retrieve the AST node associated with the resulting term.
 	 */
 	public IStrategoTerm invoke(String function, IStrategoAstNode node) {
 		ITermFactory factory = Environment.getTermFactory();
 		IStrategoTerm[] inputParts = {
 				getRoot(node).getTerm(),
 				factory.makeString(node.getResourcePath().toOSString()),
 				node.getTerm(),
 				StrategoTermPath.createPath(node)
 		};
 		IStrategoTerm input = factory.makeTuple(inputParts);
 		
 		return invoke(function, input, node.getResourcePath().removeLastSegments(1));
 	}
 	
 	public IStrategoTerm invoke(String function, IStrategoTerm term, IPath workingDir) {
 	    Debug.startTimer();
 		try {
 			interpreter.setCurrent(term);
 			initInterpreterPath(workingDir);
 
 			boolean success = interpreter.invoke(function);
 			
 			if (!success) {
 				Environment.logStrategyFailure("Failure reported during evaluation of function " + function, interpreter);
 				return null;
 			}
 		} catch (InterpreterException e) {
 			Environment.logException("Internal error evaluating function " + function, e);
 			return null;
 		}
 		
 		Debug.stopTimer("Invoked Stratego strategy " + function);
 		return interpreter.current();
 	}
 
 	public IAst getAstNode(IStrategoTerm term) {
 		if (term == null) return null;
 			
 		if (term instanceof WrappedAstNode) {
 			return ((WrappedAstNode) term).getNode();
 		} else {
 			Environment.logException("Resolved reference is not associated with an AST node " + interpreter.current());
 			return null;
 		}
 	}
 	
 	private void initInterpreterPath(IPath workingDir) {
 		try {
 			interpreter.getIOAgent().setWorkingDir(workingDir.toOSString());
 			((EditorIOAgent) interpreter.getIOAgent()).setDescriptor(descriptor);
 		} catch (FileNotFoundException e) {
 			Environment.logException("Could not set Stratego working directory", e);
 			throw new RuntimeException(e);
 		}
 	}
 	
 	private static IStrategoAstNode getRoot(IStrategoAstNode node) {
 		while (node.getParent() != null)
 			node = node.getParent();
 		return node;
 	}
 
 }
