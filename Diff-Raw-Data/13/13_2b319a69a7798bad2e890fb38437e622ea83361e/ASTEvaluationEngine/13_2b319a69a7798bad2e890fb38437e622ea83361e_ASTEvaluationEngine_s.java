 /*
  * (c) Copyright IBM Corp. 2002.
  * All Rights Reserved.
  */
 package org.eclipse.jdt.internal.debug.eval.ast;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jdt.core.dom.AST;
 import org.eclipse.jdt.core.dom.CompilationUnit;
 import org.eclipse.jdt.core.dom.Message;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaObject;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.debug.core.IJavaValue;
 import org.eclipse.jdt.debug.eval.IEvaluationEngine;
 import org.eclipse.jdt.debug.eval.IEvaluationListener;
 import org.eclipse.jdt.debug.eval.ast.model.ICompiledExpression;
 import org.eclipse.jdt.debug.eval.ast.model.IRuntimeContext;
 import org.eclipse.jdt.debug.eval.ast.model.IValue;
 import org.eclipse.jdt.debug.eval.ast.model.IVariable;
 import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTAPIVisitor;
 import org.eclipse.jdt.internal.debug.eval.ast.engine.InstructionSequence;
 import org.eclipse.jdt.internal.debug.core.model.JDIThread;
 import org.eclipse.jdt.internal.debug.eval.EvaluationResult;
 
 
 /**
  * @version 	1.0
  * @author
  */
 public class ASTEvaluationEngine implements IEvaluationEngine {
 
 	private IJavaProject fProject;
 	
 	private IJavaDebugTarget fDebugTarget;
 	
 	private boolean fEvaluationCancelled;
 	private boolean fEvaluationComplete;
 
 	public ASTEvaluationEngine(IJavaProject project, IJavaDebugTarget debugTarget) {
 		setJavaProject(project);
 		setDebugTarget(debugTarget);
 	}
 	
 	public void setJavaProject(IJavaProject project) {
 		fProject = project;
 	}
 
 	public void setDebugTarget(IJavaDebugTarget debugTarget) {
 		fDebugTarget = debugTarget;
 	}
 
 	/*
 	 * @see IEvaluationEngine#evaluate(String, IJavaThread, IEvaluationListener)
 	*/ 
 	public void evaluate(String snippet, IJavaThread thread, IEvaluationListener listener) throws DebugException {
 	}
 
 	/**
 	 * @see IEvaluationEngine#evaluate(String, IJavaStackFrame, IEvaluationListener)
 	 */
 	public void evaluate(String snippet, IJavaStackFrame frame, IEvaluationListener listener) throws DebugException {
 		ICompiledExpression expression= getCompiledExpression(snippet, frame);
 		evaluate(expression, frame, listener);
 	}
 	
 	/**
 	 * @see IEvaluationEngine#evaluate(ICompiledExpression, IJavaStackFrame, IEvaluationListener)
 	 */
 	public void evaluate(final ICompiledExpression expression, final IJavaStackFrame frame, final IEvaluationListener listener) {
 		RuntimeContext context = new RuntimeContext(getJavaProject(), frame);
 		evaluate(expression, context, (IJavaThread)frame.getThread(), listener);
 	}
 
 	/**
 	 * @see IEvaluationEngine#evaluate(String, IJavaObject, IJavaThread, IEvaluationListener)
 	 */
 	public void evaluate(String snippet, IJavaObject thisContext, IJavaThread thread, IEvaluationListener listener) throws DebugException {
 		ICompiledExpression expression= getCompiledExpression(snippet, thisContext, thread);
 		evaluate(expression, thisContext, thread, listener);
 	}
 
 	/**
 	 * @see IEvaluationEngine#evaluate(ICompiledExpression, IJavaObject, IJavaThread, IEvaluationListener)
 	 */
 	public void evaluate(final ICompiledExpression expression, final IJavaObject thisContext, final IJavaThread thread, final IEvaluationListener listener) {
 		IRuntimeContext context = new JavaObjectRuntimeContext(thisContext, getJavaProject(), thread);
 		evaluate(expression, context, thread, listener);
 	}
 	
 	/**
 	 * Performs the evaluation.
 	 */
 	public void evaluate(final ICompiledExpression expression, final IRuntimeContext context, final IJavaThread thread, final IEvaluationListener listener) {
 		Thread evaluationThread= new Thread(new Runnable() {
 			public void run() {
 				fEvaluationCancelled= false;
 				fEvaluationComplete= false;
 
 				EvaluationResult result = new EvaluationResult(ASTEvaluationEngine.this, expression.getSnippet(), thread);
 				if (expression.hasErrors()) {
 					Message[] errors= expression.getErrors();
 					for (int i= 0, numErrors= errors.length; i < numErrors; i++) {
 						result.addError(errors[i]);
 					}
 					listener.evaluationComplete(result);
 					return;
 				}
 		
 				IValue value = null;
 				Thread timeoutThread= new Thread(new Runnable() {
 					public void run() {
 						while (!fEvaluationComplete && !fEvaluationCancelled) {
 							try {
 								Thread.currentThread().sleep(3000); // 10 second timeout for now
 							} catch(InterruptedException e) {
 							}
							if (!fEvaluationComplete) {
								try {
									((JDIThread)thread).suspendQuiet();
									if (!listener.evaluationTimedOut(thread)) {
										fEvaluationCancelled= true;
									} else {
										// Keep waiting
										((JDIThread)thread).resumeQuiet();
									}
								} catch(DebugException e) {
								}
 							}
 						}
 					}
 				}, "Evaluation timeout thread");
 				timeoutThread.start();
 				value= expression.evaluate(context);
 				fEvaluationComplete= true;
 				if (fEvaluationCancelled) {
 					// Don't notify the listener if the evaluation has been cancelled
 					return;
 				}
 				CoreException exception= expression.getException();
 				
 				if (value != null) {
 					IJavaValue jv = ((EvaluationValue)value).getJavaValue();
 					result.setValue(jv);
 				}
 				result.setException(exception);
 				listener.evaluationComplete(result);
 			}
 		}, "Evaluation thread");
 		evaluationThread.start();
 	}
 
 	/*
 	 * @see IEvaluationEngine#getCompiledExpression(String, IJavaStackFrame)
 	 */
 	public ICompiledExpression getCompiledExpression(String snippet, IJavaStackFrame frame) throws DebugException {
 		IJavaProject javaProject = getJavaProject();
 		RuntimeContext context = new RuntimeContext(javaProject, frame);
 
 		ASTCodeSnippetToCuMapper mapper = null;
 		CompilationUnit unit = null;
 
 		try {
 			
 			IVariable[] locals = context.getLocals();
 			int numLocals= locals.length;
 			int[] localModifiers = new int[locals.length];
 			String[] localTypesNames= new String[numLocals];
 			String[] localVariables= new String[numLocals];
 			for (int i = 0; i < numLocals; i++) {
 				localVariables[i] = locals[i].getName();
 				localTypesNames[i] = ((EvaluationValue)locals[i].getValue()).getJavaValue().getReferenceTypeName();
 				localModifiers[i]= 0;
 			}
 			
 			mapper = new ASTCodeSnippetToCuMapper(new String[0], localModifiers, localTypesNames, localVariables, snippet);
 
 			unit = AST.parseCompilationUnit(mapper.getSource(frame).toCharArray(), mapper.getCompilationUnitName(), javaProject);
 		} catch (JavaModelException e) {
 			throw new DebugException(e.getStatus());
 		} catch (CoreException e) {
 			throw new DebugException(e.getStatus());
 		}
 		
 		return genExpressionFromAST(snippet, mapper, unit);
 	}
 
 	public ICompiledExpression getCompiledExpression(String snippet, IJavaObject thisContext, IJavaThread thread) throws DebugException {
 		IJavaProject javaProject = getJavaProject();
 
 		ASTCodeSnippetToCuMapper mapper = null;
 		CompilationUnit unit = null;
 
 		try {
 			mapper = new ASTCodeSnippetToCuMapper(new String[0], new int[0], new String[0], new String[0], snippet);
 
 			unit = AST.parseCompilationUnit(mapper.getSource(thisContext, javaProject).toCharArray(), mapper.getCompilationUnitName(), javaProject);
 		} catch (CoreException e) {
 			throw new DebugException(e.getStatus());
 		}
 		
 		return genExpressionFromAST(snippet, mapper, unit);
 	}
 
 	private ICompiledExpression genExpressionFromAST(String snippet, ASTCodeSnippetToCuMapper mapper, CompilationUnit unit) {
 		Message[] messages= unit.getMessages();
 		if (messages.length != 0) {
 			boolean error= false;
 			InstructionSequence errorSequence= new InstructionSequence(snippet);
 			int codeSnippetStartOffset= mapper.getStartPosition();
 			int codeSnippetEndOffset= codeSnippetStartOffset + snippet.length();
 			for (int i = 0; i < messages.length; i++) {
 				Message message= messages[i];
 				int errorOffset= message.getSourcePosition();
 				// TO DO: Internationalize "void method..." error message check
 				if (codeSnippetStartOffset <= errorOffset && errorOffset <= codeSnippetEndOffset && !"Void methods cannot return a value".equals(message.getMessage())) {
 					errorSequence.addError(message);
 					error = true;
 				}
 			}
 			if (error) {
 				return errorSequence;
 			}
 		}
 		
 		ASTAPIVisitor visitor = new ASTAPIVisitor(mapper.getStartPosition(), snippet);
 		unit.accept(visitor);
 
 		return visitor.getInstructions();
 	}
 
 	/*
 	 * @see IEvaluationEngine#getJavaProject()
 	 */
 	public IJavaProject getJavaProject() {
 		return fProject;
 	}
 
 	/*
 	 * @see IEvaluationEngine#getDebugTarget()
 	 */
 	public IJavaDebugTarget getDebugTarget() {
 		return fDebugTarget;
 	}
 
 	/*
 	 * @see IEvaluationEngine#dispose()
 	 */
 	public void dispose() {
 	}
 
 	/**
 	 * @see IEvaluationEngine#evaluate(ICompiledExpression, IJavaThread, IEvaluationListener)
 	 */
 	public void evaluate(ICompiledExpression expression, IJavaThread thread, IEvaluationListener listener) throws DebugException {
 	}
 
 	/**
 	 * @see IEvaluationEngine#getCompiledExpression(String, IJavaThread)
 	 */
 	public ICompiledExpression getCompiledExpression(String snippet, IJavaThread thread) throws DebugException {
 		return null;
 	}
 
 }
