 /*
  * Copyright (c) 2014, IETR/INSA of Rennes
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
 package net.sf.orcc.xdf.ui.features;
 
 import java.io.IOException;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 import net.sf.orcc.df.Connection;
 import net.sf.orcc.df.DfFactory;
 import net.sf.orcc.df.Entity;
 import net.sf.orcc.df.Instance;
 import net.sf.orcc.df.Network;
 import net.sf.orcc.df.Port;
 import net.sf.orcc.xdf.ui.dialogs.NewNetworkWizard;
 import net.sf.orcc.xdf.ui.layout.OrthogonalAutoLayoutFeature;
 import net.sf.orcc.xdf.ui.patterns.InputNetworkPortPattern;
 import net.sf.orcc.xdf.ui.patterns.InstancePattern;
 import net.sf.orcc.xdf.ui.patterns.OutputNetworkPortPattern;
 import net.sf.orcc.xdf.ui.util.ShapePropertiesManager;
 import net.sf.orcc.xdf.ui.util.XdfUtil;
 
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.graphiti.features.IDirectEditingInfo;
 import org.eclipse.graphiti.features.IFeatureProvider;
 import org.eclipse.graphiti.features.context.IContext;
 import org.eclipse.graphiti.features.context.ICustomContext;
 import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
 import org.eclipse.graphiti.features.context.impl.AddContext;
 import org.eclipse.graphiti.features.context.impl.CustomContext;
 import org.eclipse.graphiti.features.context.impl.DeleteContext;
 import org.eclipse.graphiti.features.context.impl.MultiDeleteInfo;
 import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
 import org.eclipse.graphiti.mm.pictograms.Anchor;
 import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
 import org.eclipse.graphiti.mm.pictograms.PictogramElement;
 import org.eclipse.graphiti.pattern.IFeatureProviderWithPatterns;
 import org.eclipse.graphiti.pattern.IPattern;
 import org.eclipse.graphiti.services.Graphiti;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.wizard.WizardDialog;
 import org.eclipse.ui.PlatformUI;
 
 /**
  * Group selected instances into a new Network located somewhere. Replace the
  * selected instances by a new one, refined on the network created.
  * 
  * @author Antoine Lorence
  * 
  */
 public class GroupInstancesFeature extends AbstractCustomFeature {
 
 	private boolean hasDoneChanges;
 
 	private final Map<String, Integer> portNamesIndexes;
 
 	public GroupInstancesFeature(IFeatureProvider fp) {
 		super(fp);
 		hasDoneChanges = false;
 		portNamesIndexes = new HashMap<String, Integer>();
 	}
 
 	@Override
 	public String getName() {
 		return "Group selected instances into new network";
 	}
 
 	@Override
 	public boolean isAvailable(IContext context) {
 		return super.isAvailable(context);
 	}
 
 	@Override
 	public boolean canExecute(ICustomContext context) {
 		final PictogramElement[] selection = context.getPictogramElements();
 		if (selection.length < 2) {
 			return false;
 		}
 
 		int cptInstances = 0;
 		for (final PictogramElement pe : selection) {
 			if (ShapePropertiesManager.isExpectedPc(pe,
 					InstancePattern.INSTANCE_ID)) {
 				cptInstances++;
 			}
 		}
 
 		return cptInstances >= 2;
 	}
 
 	@Override
 	public void execute(ICustomContext context) {
 
 		portNamesIndexes.clear();
 
 		final Network currentNetwork = (Network) getBusinessObjectForPictogramElement(getDiagram());
 
 		// Create the wizard used to select name and location for the new
 		// network
 		final NewNetworkWizard wizard = new NewNetworkWizard(false);
 
 		// Initialize the wizard with the current package location
 		final StructuredSelection networkSelection = new StructuredSelection(
 				currentNetwork.getFile().getParent());
 		wizard.init(PlatformUI.getWorkbench(), networkSelection);
 
 		final WizardDialog dialog = new WizardDialog(XdfUtil.getDefaultShell(),
 				wizard);
 		dialog.open();
 		if (dialog.getReturnCode() != Dialog.OK) {
 			return;
 		}
 
 		// The new network
 		final Network newNetwork = wizard.getCreatedNetwork();
 
 		if (newNetwork == null) {
 			return;
 		}
 
 		final IFeatureProviderWithPatterns fp = (IFeatureProviderWithPatterns) getFeatureProvider();
 
 		final Set<Instance> selection = new HashSet<Instance>();
 		for (final PictogramElement pe : context.getPictogramElements()) {
 			final Object selected = getBusinessObjectForPictogramElement(pe);
 			if (selected instanceof Instance) {
 				selection.add((Instance) selected);
 			} else {
 				continue;
 			}
 		}
 
 		// This set will be filled with connections which needs to be
 		// re-added to the diagram
 		final Set<Connection> toUpdateInDiagram = new HashSet<Connection>();
 		final Instance newInstance;
 		try {
 			// Update the current and the created network. Also create the new
 			// instance used to replace all selected elements
 			newInstance = updateNetworksAndCreateInstance(currentNetwork,
 					newNetwork, selection, toUpdateInDiagram);
 		} catch (IOException e) {
 			e.printStackTrace();
 			return;
 		}
 
 		// Adds it to the current network
 		final AddContext addContext = new AddContext();
 		addContext.setTargetContainer(getDiagram());
 		addContext.setNewObject(newInstance);
 		// We will run the layout at the end
 		addContext.setLocation(10, 10);
 		final PictogramElement newInstancePe = getFeatureProvider()
 				.addIfPossible(addContext);
 
 		// Update connections to/from the new instance
 		for (final Connection connection : toUpdateInDiagram) {
 
 			final Anchor sourceAnchor, targetAnchor;
 			final PictogramElement sourcePe = Graphiti.getLinkService()
 					.getPictogramElements(getDiagram(), connection.getSource())
 					.get(0);
 			if (ShapePropertiesManager.isExpectedPc(sourcePe,
 					InputNetworkPortPattern.INOUT_ID)) {
 				// Connection from a network port
 				final InputNetworkPortPattern spattern = (InputNetworkPortPattern) fp
 						.getPatternForPictogramElement(sourcePe);
 				sourceAnchor = spattern.getAnchor((AnchorContainer) sourcePe);
 			} else {
 				// Connection from an instance port
 				final InstancePattern spattern = (InstancePattern) fp
 						.getPatternForPictogramElement(sourcePe);
 				sourceAnchor = spattern.getAnchorForPort(sourcePe,
 						connection.getSourcePort());
 			}
 
 			final PictogramElement targetPe = Graphiti.getLinkService()
 					.getPictogramElements(getDiagram(), connection.getTarget())
 					.get(0);
 			if (ShapePropertiesManager.isExpectedPc(targetPe,
 					OutputNetworkPortPattern.INOUT_ID)) {
 				// Connection to a network port
 				final OutputNetworkPortPattern tpattern = (OutputNetworkPortPattern) fp
 						.getPatternForPictogramElement(targetPe);
 				targetAnchor = tpattern.getAnchor((AnchorContainer) targetPe);
 			} else {
 				// Connection to an instance port
 				final InstancePattern tpattern = (InstancePattern) fp
 						.getPatternForPictogramElement(targetPe);
 				targetAnchor = tpattern.getAnchorForPort(targetPe,
 						connection.getTargetPort());
 			}
 
 			final AddConnectionContext addConContext = new AddConnectionContext(
 					sourceAnchor, targetAnchor);
 			addConContext.setNewObject(connection);
 			getFeatureProvider().addIfPossible(addConContext);
 		}
 
 		// Finally remove from diagram useless elements. Inner connections
 		// are also deleted, since deleting an instance or a port from a
 		// diagram also clean related connections
 		for (final PictogramElement pe : context.getPictogramElements()) {
 			final IPattern pattern = fp.getPatternForPictogramElement(pe);
 			final DeleteContext delContext = new DeleteContext(pe);
 			delContext.setMultiDeleteInfo(new MultiDeleteInfo(false, false, 0));
 			pattern.delete(delContext);
 		}
 
 		// And layout the resulting diagram
 		final IContext layoutContext = new CustomContext();
 		final OrthogonalAutoLayoutFeature layoutFeature = new OrthogonalAutoLayoutFeature(
 				getFeatureProvider());
 		if (layoutFeature.canExecute(layoutContext)) {
 			layoutFeature.execute(layoutContext);
 		}
 
 		final IDirectEditingInfo dei = getFeatureProvider()
 				.getDirectEditingInfo();
 		dei.setMainPictogramElement(newInstancePe);
 		dei.setActive(true);
 
 		hasDoneChanges = true;
 
 	}
 
 	/**
 	 * <p>
 	 * Here is the magic. In this function, both current and new network are
 	 * updated to reflect changes of this feature.
 	 * </p>
 	 * 
 	 * <p>
 	 * All vertices (ports/instances) selected by user are duplicated. Copies
 	 * are added to the new network, originals are removed from the current
 	 * network. If selection cut connections in the current diagram, new ports
 	 * are created and correctly connected in the new network. They are also
 	 * updated to connect to the right port on the new instance.
 	 * </p>
 	 * 
 	 * <p>
 	 * This function modify networks only. It does not update corresponding
 	 * diagrams.
 	 * </p>
 	 * 
 	 * @param currentNetwork
 	 *            The network user is working on
 	 * @param newNetwork
 	 *            The network created to contains elements selected by user
 	 * @param selection
 	 *            Vertices selected.
 	 * @param toUpdateInDiagram
 	 *            A set of connections. Needs to adds all these connections to
 	 *            the current diagram
 	 * @return The instance created
 	 * @throws IOException
 	 */
 	private Instance updateNetworksAndCreateInstance(
 			final Network currentNetwork, final Network newNetwork,
 			final Set<Instance> selection,
 			final Set<Connection> toUpdateInDiagram) throws IOException {
 
 		final Map<Instance, Instance> copies = new HashMap<Instance, Instance>();
 		final Map<Connection, Port> toReconnectToTarget = new HashMap<Connection, Port>();
 		final Map<Connection, Port> toReconnectFromSource = new HashMap<Connection, Port>();
 
 		// Adds copies of selected objects to the new network
 		for (final Instance originalInstance : selection) {
 			final Instance copy = EcoreUtil.copy(originalInstance);
 			copies.put(originalInstance, copy);
 			newNetwork.add(copy);
 		}
 
 		// Manage connections
 		for (final Connection connection : currentNetwork.getConnections()) {
 			// 1 - Inner connection: connect 2 vertex both contained in the
 			// selection
 			if (selection.contains(connection.getSource())
 					&& selection.contains(connection.getTarget())) {
 				final Connection copy = EcoreUtil.copy(connection);
 
 				final Instance src = copies.get(connection.getSource());
 				final Instance tgt = copies.get(connection.getTarget());
 
 				copy.setSource(src);
 				copy.setTarget(tgt);
 				copy.setSourcePort(src.getAdapter(Entity.class).getOutput(
 						connection.getSourcePort().getName()));
 				copy.setTargetPort(tgt.getAdapter(Entity.class).getInput(
 						connection.getTargetPort().getName()));
 
 				newNetwork.add(copy);
 			}
 			// 2 - Cut connection: connected TO a vertex contained in the
 			// selection
 			else if (selection.contains(connection.getTarget())) {
 				// Create a new port
 				final Port p = DfFactory.eINSTANCE.createPort(
 						EcoreUtil.copy(connection.getTargetPort().getType()),
 						uniquePortName(connection.getTargetPort().getName()));
 				newNetwork.addInput(p);
 				// We will reconnect this connection when new instance will be
 				// created
 				toReconnectToTarget.put(connection, p);
 				// Create a new connection, ...
 				final Instance target = copies.get(connection.getTarget());
 				final Port targetPort = target.getAdapter(Entity.class)
 						.getInput(connection.getTargetPort().getName());
 				final Connection c = DfFactory.eINSTANCE.createConnection(p,
 						null, target, targetPort);
 				// ... fully contained in the new network
 				newNetwork.add(c);
 			}
 			// 3 - Cut connections: connected FROM a vertex contained in the
 			// selection
 			else if (selection.contains(connection.getSource())) {
 				// Create a new port
 				final Port p = DfFactory.eINSTANCE.createPort(
 						EcoreUtil.copy(connection.getSourcePort().getType()),
 						uniquePortName(connection.getSourcePort().getName()));
 				newNetwork.addOutput(p);
 				// We will reconnect this connection when new instance will be
 				// created
 				toReconnectFromSource.put(connection, p);
 				// Create a new connection, ...
 				final Instance source = copies.get(connection.getSource());
 				final Port sourcePort = source.getAdapter(Entity.class)
 						.getOutput(connection.getSourcePort().getName());
 
 				final Connection c = DfFactory.eINSTANCE.createConnection(
 						source, sourcePort, p, null);
 				// ... fully contained in the new network
 				newNetwork.add(c);
 			}
 		}
 
 		// Save the new network on the disk
 		newNetwork.eResource().save(Collections.EMPTY_MAP);
 
 		// Create the new instance
 		final Instance newInstance = DfFactory.eINSTANCE.createInstance(
				"newInstance", newNetwork);
 		currentNetwork.add(newInstance);
 
 		// Update existing connections. Re-connect them to the new instance, on
 		// the right port
 		for (final Map.Entry<Connection, Port> entry : toReconnectToTarget
 				.entrySet()) {
 			final Connection connection = entry.getKey();
 			final Port targetPort = entry.getValue();
 
 			connection.setTarget(newInstance);
 			connection.setTargetPort(targetPort);
 			toUpdateInDiagram.add(connection);
 		}
 
 		for (final Map.Entry<Connection, Port> entry : toReconnectFromSource
 				.entrySet()) {
 			final Connection connection = entry.getKey();
 			final Port sourcePort = entry.getValue();
 
 			connection.setSource(newInstance);
 			connection.setSourcePort(sourcePort);
 			toUpdateInDiagram.add(connection);
 		}
 
 		return newInstance;
 	}
 
 	private String uniquePortName(final String baseName) {
 		if (portNamesIndexes.containsKey(baseName)) {
 			final int index = portNamesIndexes.get(baseName) + 1;
 			portNamesIndexes.put(baseName, index);
 			return baseName + "_" + index;
 		} else {
 			portNamesIndexes.put(baseName, 0);
 			return baseName;
 		}
 	}
 
 	@Override
 	public boolean hasDoneChanges() {
 		return hasDoneChanges;
 	}
 }
