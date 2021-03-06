 /*
  * Copyright (c) 2009-2010, IETR/INSA of Rennes
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
 package net.sf.orcc.tools.classifier;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import net.sf.orcc.OrccRuntimeException;
 import net.sf.orcc.ir.Action;
 import net.sf.orcc.ir.Actor;
 import net.sf.orcc.ir.InstPhi;
 import net.sf.orcc.ir.NodeIf;
 import net.sf.orcc.ir.NodeWhile;
 import net.sf.orcc.ir.Pattern;
 import net.sf.orcc.ir.Port;
 import net.sf.orcc.ir.Type;
 import net.sf.orcc.ir.TypeList;
 import net.sf.orcc.ir.Var;
 import net.sf.orcc.ir.util.ActorInterpreter;
 import net.sf.orcc.ir.util.ValueUtil;
 import net.sf.orcc.moc.CSDFMoC;
 
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
 
 /**
  * This class defines an abstract interpreter of an actor. It refines the
  * concrete interpreter by not relying on anything that is data-dependent.
  * 
  * @author Matthieu Wipliez
  * 
  */
 public class AbstractInterpreter extends ActorInterpreter {
 
 	private Map<String, Object> configuration;
 
 	private Copier copier;
 
 	private Map<String, Boolean> portRead;
 
 	private boolean schedulableMode;
 
 	private Action scheduledAction;
 
 	/**
 	 * Creates a new abstract interpreter.
 	 * 
 	 * @param actor
 	 *            an actor
 	 */
 	public AbstractInterpreter(Actor actor) {
 		// create a copier, and copy the original actor
 		copier = new EcoreUtil.Copier();
 		Actor copyOfActor = (Actor) copier.copy(actor);
 		copier.copyReferences();
 
 		setActor(copyOfActor);
 
 		// abstract expression interpreter
 		exprInterpreter = new AbstractExpressionEvaluator();
 
 		initialize();
 	}
 
 	@Override
 	public Object caseInstPhi(InstPhi phi) {
 		if (branch != -1) {
 			return super.caseInstPhi(phi);
 		}
 		return null;
 	}
 
 	@Override
 	public Object caseNodeIf(NodeIf node) {
 		// Interpret first expression ("if" condition)
 		Object condition = exprInterpreter.doSwitch(node.getCondition());
 
 		int oldBranch = branch;
 		if (ValueUtil.isBool(condition)) {
 			if (ValueUtil.isTrue(condition)) {
 				doSwitch(node.getThenNodes());
 				branch = 0;
 			} else {
 				doSwitch(node.getElseNodes());
 				branch = 1;
 			}
 
 		} else {
 			if (schedulableMode) {
 				// only throw exception in schedulable mode
 				throw new OrccRuntimeException("null condition");
 			}
 
 			branch = -1;
 		}
 
 		doSwitch(node.getJoinNode());
 		branch = oldBranch;
 		return null;
 	}
 
 	@Override
 	public Object caseNodeWhile(NodeWhile node) {
 		int oldBranch = branch;
 		branch = 0;
 		doSwitch(node.getJoinNode());
 
 		// Interpret first expression ("while" condition)
 		Object condition = exprInterpreter.doSwitch(node.getCondition());
 
 		if (ValueUtil.isBool(condition)) {
 			branch = 1;
 			while (ValueUtil.isTrue(condition)) {
 				doSwitch(node.getNodes());
 				doSwitch(node.getJoinNode());
 
 				// Interpret next value of "while" condition
 				condition = exprInterpreter.doSwitch(node.getCondition());
 				if (schedulableMode && !ValueUtil.isBool(condition)) {
 					throw new OrccRuntimeException(
 							"Condition not boolean at line "
 									+ node.getLineNumber() + "\n");
 				}
 			}
 		} else if (schedulableMode) {
 			// only throw exception in schedulable mode
 			throw new OrccRuntimeException("condition is data-dependent");
 		}
 
 		branch = oldBranch;
 		return null;
 	}
 
 	@Override
 	public void execute(Action action) {
 		scheduledAction = action;
 		Pattern inputPattern = action.getInputPattern();
 		for (Port port : inputPattern.getPorts()) {
 			int numTokens = inputPattern.getNumTokens(port);
 			String portName = port.getName();
 			if (configuration != null && configuration.containsKey(portName)
 					&& !portRead.get(portName)) {
 				// Should we use a range of values in the spirit of
 				// "Accurate Static Branch Prediction by Value Range Propagation"?
 
 				// in the meantime, we only use the configuration value in the
 				// Peek
 
 				portRead.put(portName, true);
 			}
 
 			port.increaseTokenConsumption(numTokens);
 		}
 
 		// allocate output pattern (but not input pattern)
 		Pattern outputPattern = action.getOutputPattern();
 		allocatePattern(outputPattern);
 
 		// execute action
 		doSwitch(action.getBody());
 
 		// update token production
 		for (Port port : outputPattern.getPorts()) {
 			int numTokens = outputPattern.getNumTokens(port);
 			port.increaseTokenProduction(numTokens);
 		}
 	}
 
 	/**
 	 * Returns the latest action that was scheduled by the latest call to
 	 * {@link #schedule()}.
 	 * 
 	 * @return the latest scheduled action
 	 */
 	public Action getScheduledAction() {
 		// return the action of the original actor
 		return (Action) copier.get(scheduledAction);
 	}
 
 	@Override
 	protected boolean isSchedulable(Action action) {
 		// do not check the number of tokens present on FIFOs
 
 		Pattern pattern = action.getPeekPattern();
 		for (Port port : pattern.getPorts()) {
 			Var peeked = pattern.getVariable(port);
 			String portName = port.getName();
 			if (configuration != null && configuration.containsKey(portName)
 					&& !portRead.get(portName)) {
 				// allocates peeked variables
 				TypeList typeList = (TypeList) peeked.getType();
 				Object array = ValueUtil.createArray(typeList);
 				peeked.setValue(array);
 
 				Type type = typeList.getType();
 				Object value = configuration.get(portName);
 				ValueUtil.set(type, array, value, 0);
 			}
 		}
 
 		// enable schedulable mode
 		setSchedulableMode(true);
 		try {
 			Object result = doSwitch(action.getScheduler());
 			if (result == null) {
 				throw new OrccRuntimeException("could not determine if action "
 						+ action.toString() + " is schedulable");
 			}
 			return ValueUtil.isTrue(result);
 		} finally {
 			// disable schedulable mode
 			setSchedulableMode(false);
 		}
 	}
 
 	/**
 	 * Sets the configuration that should be used by the interpreter.
 	 * 
 	 * @param configuration
 	 *            a configuration as a map of ports and values
 	 */
 	public void setConfiguration(Map<String, Object> configuration) {
 		this.configuration = configuration;
 		portRead = new HashMap<String, Boolean>(configuration.size());
 		for (String port : configuration.keySet()) {
 			portRead.put(port, false);
 		}
 	}
 
 	/**
 	 * Sets schedulable mode. When in schedulable mode, evaluations of null
 	 * expressions is forbidden.
 	 * 
 	 * @param schedulableMode
 	 */
 	public void setSchedulableMode(boolean schedulableMode) {
 		this.schedulableMode = schedulableMode;
 		((AbstractExpressionEvaluator) exprInterpreter)
 				.setSchedulableMode(schedulableMode);
 	}
 
 	/**
 	 * Sets the token rates, i.e. token production/consumption rates, of the
 	 * given CSDF MoC. Token rates are set using the ports of the original actor
 	 * (the actor this interpreter was created with) and not the copy (the actor
 	 * on which the interpreter is working).
 	 * 
 	 * @param csdfMoc
 	 *            a CSDF MoC
 	 */
 	public void setTokenRates(CSDFMoC csdfMoc) {
 		// we use the ports of the original actor
 		Pattern inputPattern = csdfMoc.getInputPattern();
		for (Port port : actor.getInputs()) {
 			int numTokens = port.getNumTokensConsumed();
			Port originalPort = (Port) copier.get(port);
 			inputPattern.setNumTokens(originalPort, numTokens);
 		}
 
 		Pattern outputPattern = csdfMoc.getOutputPattern();
		for (Port port : actor.getOutputs()) {
 			int numTokens = port.getNumTokensProduced();
			Port originalPort = (Port) copier.get(port);
 			outputPattern.setNumTokens(originalPort, numTokens);
 		}
 	}
 
 }
