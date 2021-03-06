 package org.bonitasoft.studio.validation.constraints.process;
 
 import org.bonitasoft.studio.common.repository.RepositoryManager;
 import org.bonitasoft.studio.connector.model.definition.AbstractDefinitionRepositoryStore;
 import org.bonitasoft.studio.connector.model.definition.ConnectorDefinition;
 import org.bonitasoft.studio.connectors.repository.ConnectorDefRepositoryStore;
 import org.bonitasoft.studio.model.process.Connector;
 import org.bonitasoft.studio.validation.constraints.AbstractLiveValidationMarkerConstraint;
 import org.bonitasoft.studio.validation.i18n.Messages;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.emf.validation.IValidationContext;
 
 public class ConnectorExistenceConstraint extends
		AbstractLiveValidationMarkerConstraint {
 
 
 
 	public static final String ID = "org.bonitasoft.studio.validation.constraints.connectorexistence";
	
	
 	@Override
 	protected IStatus performLiveValidation(IValidationContext context) {
 		return null;
 	}
 
 	@Override
 	protected IStatus performBatchValidation(IValidationContext context) {
 		final AbstractDefinitionRepositoryStore<?> connectorDefStore = (AbstractDefinitionRepositoryStore<?>) RepositoryManager
 				.getInstance().getRepositoryStore(ConnectorDefRepositoryStore.class);
 		Connector connector = (Connector)context.getTarget();
 		ConnectorDefinition def = connectorDefStore.getDefinition(connector.getDefinitionId(),connector.getDefinitionVersion());
 		if (def!=null){
 			return context.createSuccessStatus();
 		}  else {
		return context.createFailureStatus( Messages.bind(Messages.Validation_noConnectorDefFound,connector.getName()));
 		}
 	}
 
 	@Override
 	protected String getConstraintId() {
 		return ID;
 	}
 
 }
