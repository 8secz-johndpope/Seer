 /*
  * Copyright (c) 2011, IETR/INSA of Rennes
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
 package net.sf.orcc.df.transformations;
 
 import net.sf.orcc.df.Argument;
 import net.sf.orcc.df.Connection;
 import net.sf.orcc.df.DfFactory;
 import net.sf.orcc.df.Entity;
 import net.sf.orcc.df.Instance;
 import net.sf.orcc.df.Network;
 import net.sf.orcc.df.Port;
 import net.sf.orcc.df.Vertex;
 import net.sf.orcc.df.util.DfSwitch;
 import net.sf.orcc.ir.Expression;
 import net.sf.orcc.ir.Var;
 import net.sf.orcc.moc.MoC;
 
 import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
 
 /**
  * This class defines a transformation that transforms a network into a new
  * network where instances of actors and networks are replaced by new actors and
  * networks where the value of parameters have been appropriately replaced.
  * 
  * @author Matthieu Wipliez
  * 
  */
 public class Instantiator extends DfSwitch<Network> {
 
 	private Copier copier;
 
 	private boolean skipActors;
 
 	/**
 	 * Creates a default instantiator, equivalent to
 	 * <code>Instantiator(true)</code>.
 	 */
 	public Instantiator() {
 		this(true);
 	}
 
 	/**
 	 * Creates an instantiator that will replace instances of networks by
 	 * instantiated networks, and if <code>skipActors</code> is false, will also
 	 * replace instances of actors by instantiated actors.
 	 * 
 	 * @param skipActors
 	 *            <code>true</code> if actors should be skipped in the
 	 *            instantiation process, <code>false</code> otherwise
 	 */
 	public Instantiator(boolean skipActors) {
 		this.skipActors = skipActors;
 		copier = new Copier();
 	}
 
 	@Override
 	public Network caseNetwork(Network network) {
 		Network networkCopy = DfFactory.eINSTANCE.createNetwork();
 
 		// copy name, filename, moc
 		networkCopy.setFileName(network.getFileName());
 		networkCopy.setName(network.getName());
 		networkCopy.setMoC((MoC) copier.copy(network.getMoC()));
 
 		// copy ports, parameters, variables
 		networkCopy.getInputs().addAll(copier.copyAll(network.getInputs()));
 		networkCopy.getOutputs().addAll(copier.copyAll(network.getOutputs()));
 		networkCopy.getParameters().addAll(
 				copier.copyAll(network.getParameters()));
 		networkCopy.getVariables().addAll(
 				copier.copyAll(network.getVariables()));
 
 		// copy instances to entities/instances
 		for (Instance instance : network.getInstances()) {
 			Entity entity = instance.getEntity();
 			if (entity.isActor() && skipActors) {
 				Instance copy = (Instance) copier.copy(instance);
 				networkCopy.getInstances().add(copy);
 			} else {
 				if (entity.isNetwork()) {
 					entity = doSwitch(entity);
 				} else {
 					entity = (Entity) copier.copy(entity);
 				}
 
 				// add entity to the network's entities
 				networkCopy.getEntities().add(entity);
 
 				// set name, attributes, arguments
 				entity.setName(instance.getName());
 				entity.getAttributes().addAll(
 						copier.copyAll(instance.getAttributes()));
 				for (Argument argument : instance.getArguments()) {
 					Var var = argument.getVariable();
 					var = entity.getParameter(var.getName());
 
 					Expression value = argument.getValue();
 					Expression copyValue = (Expression) copier.copy(value);
 
 					var.setInitialValue(copyValue);
 				}
 			}
 		}
 		copier.copyReferences();
 
 		// copy connections
 		for (Connection connection : network.getConnections()) {
 			Vertex source = getCopy(connection.getSource());
 			Vertex target = getCopy(connection.getTarget());
 
 			Port sourcePort = getPort(source, connection.getSourcePort());
 			Port targetPort = getPort(target, connection.getTargetPort());
 
 			Connection copy = DfFactory.eINSTANCE.createConnection(source,
 					sourcePort, target, targetPort,
 					copier.copyAll(connection.getAttributes()));
 			networkCopy.getConnections().add(copy);
 		}
 
		copier.put(network, networkCopy);
 		return networkCopy;
 	}
 
 	private Vertex getCopy(Vertex vertex) {
 		Vertex result = (Vertex) copier.get(vertex);
 		if (result == null) {
			// instance was not copied
			Entity entity = ((Instance) vertex).getEntity();
			result = (Vertex) copier.get(entity);
 		}
 		return result;
 	}
 
 	private Port getPort(Vertex vertex, Port port) {
 		if (vertex.isEntity()) {
 			Entity entity = (Entity) vertex;
 			return entity.getPort(port.getName());
 		} else {
 			return port;
 		}
 	}
 
 }
