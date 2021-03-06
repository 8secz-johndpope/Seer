 package org.wso2.developerstudio.eclipse.gmf.esb.internal.persistence;
 
 import org.apache.synapse.mediators.base.SequenceMediator;
 import org.apache.synapse.mediators.builtin.DropMediator;
 import org.eclipse.core.runtime.Assert;
 import org.eclipse.emf.ecore.EObject;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbLink;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbNode;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbSequenceOutputConnector;
 import org.wso2.developerstudio.eclipse.gmf.esb.InputConnector;
 import org.wso2.developerstudio.eclipse.gmf.esb.OutputConnector;
 import org.wso2.developerstudio.eclipse.gmf.esb.persistence.EsbNodeTransformer;
 import org.wso2.developerstudio.eclipse.gmf.esb.persistence.EsbTransformerRegistry;
 import org.wso2.developerstudio.eclipse.gmf.esb.persistence.TransformationInfo;
 
 public abstract class AbstractEsbNodeTransformer implements EsbNodeTransformer {
 	
 	/**
 	 * Performs the transformation of the data-flow path begining with the
 	 * specified {@link OutputConnector}.
 	 * 
 	 * @param synapseConfiguration
 	 * @param rootService
 	 * @param parentMediator
 	 * @param outputConnector
 	 * @throws Exception
 	 */
 	protected void doTransform(TransformationInfo info, OutputConnector outputConnector) throws Exception {		
 		if (null != outputConnector) {
 			EObject previousNode = outputConnector.eContainer();
 			info.setPreviousNode(previousNode);
 			
 			// Process only if there is a link.
 			EsbLink outgoingLink = outputConnector.getOutgoingLink();
 			if (null != outgoingLink) {
 				InputConnector inputConnector = outgoingLink.getTarget();
 				Assert.isNotNull(inputConnector, "Input connector should not be null.");
 
 				EObject nextNode = inputConnector.eContainer();
 				Assert.isTrue(nextNode instanceof EsbNode, "Unknown target node.");
 
 				EsbNode esbNode = (EsbNode) nextNode;
 				EsbNodeTransformer transformer = EsbTransformerRegistry.getInstance().getTransformer(esbNode);
 				Assert.isNotNull(transformer, "No registered transformer for given node.");
 
 				transformer.transform(info, esbNode);
 			} else {
 				// TODO: Might be better to automatically log the message before dropping. 
 				if (info.getParentSequence()!=null){ //TODO temp					
 				//info.getParentSequence().addChild(new DropMediator());
 				}
 			}
 		} else {
 			// TODO: Warn about the fact that output connector is null.
 		}
 	}
 	
 	protected void doTransformWithinSequence(TransformationInfo info, EsbLink outgoingLink,SequenceMediator sequence) throws Exception {
 	
 			if (null != outgoingLink) {
 				EObject previousNode = outgoingLink.getSource().eContainer();
 				info.setPreviousNode(previousNode);
 				
 				InputConnector inputConnector = outgoingLink.getTarget();
 				Assert.isNotNull(inputConnector, "Input connector should not be null.");
 				
				if(inputConnector instanceof EsbSequenceOutputConnector){
 					if(info.currentSequence.getOutputConnector().getOutgoingLink()!=null){
 					EsbNode esbNode=(EsbNode)info.currentSequence.getOutputConnector().getOutgoingLink().getTarget().eContainer();
 					EsbNodeTransformer transformer = EsbTransformerRegistry.getInstance().getTransformer(esbNode);					
 					transformer.transform(info, esbNode);
 					}
 					else{
 						if (info.getParentSequence()!=null){ 
 							info.getParentSequence().addChild(new DropMediator());
 							}
 					}
 				}
 				else{
 					EObject nextNode = inputConnector.eContainer();
 					Assert.isTrue(nextNode instanceof EsbNode, "Unknown target node.");
 
 					EsbNode esbNode = (EsbNode) nextNode;
 					EsbNodeTransformer transformer = EsbTransformerRegistry.getInstance().getTransformer(esbNode);
 					Assert.isNotNull(transformer, "No registered transformer for given node.");
 
 					transformer.transformWithinSequence(info, esbNode,sequence);
 				}
 			} else {
 				sequence.addChild(new DropMediator());
 				/*// TODO: Might be better to automatically log the message before dropping. 
 				if (info.getParentSequence()!=null){ //TODO temp
 				info.getParentSequence().addChild(new DropMediator());
 				}*/
 			}
 		
 	}
 	
 	protected void doTransformFaultSequence(TransformationInfo info,EsbNode originNode) throws Exception {
 		if(originNode !=null){
 		EsbNodeTransformer transformer = EsbTransformerRegistry.getInstance().getTransformer(originNode);
 		Assert.isNotNull(transformer, "No registered transformer for given node.");
 
 		transformer.transform(info, originNode);
 		}
 	}
 	
 }
