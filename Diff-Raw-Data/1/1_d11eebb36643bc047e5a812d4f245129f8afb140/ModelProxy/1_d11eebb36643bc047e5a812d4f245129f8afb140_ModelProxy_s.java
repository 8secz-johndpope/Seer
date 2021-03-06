 package org.integratedmodelling.modelling;
 
 import java.util.ArrayList;
 
 import org.integratedmodelling.corescience.interfaces.observation.IObservation;
 import org.integratedmodelling.modelling.interfaces.IModel;
 import org.integratedmodelling.thinklab.IntelligentMap;
 import org.integratedmodelling.thinklab.exception.ThinklabException;
 import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
 import org.integratedmodelling.thinklab.interfaces.applications.ISession;
 import org.integratedmodelling.thinklab.interfaces.knowledge.IConcept;
 import org.integratedmodelling.thinklab.interfaces.query.IConformance;
 import org.integratedmodelling.thinklab.interfaces.storage.IKBox;
 import org.integratedmodelling.utils.Polylist;
 
 /**
  * A model proxy wraps a Model so it can receive configuration through clauses.
  * 
  * @author Ferdinando Villa
  *
  */
 public class ModelProxy extends DefaultAbstractModel {
 
 	Model model = null;
 	
 	public ModelProxy(Model model) {
 		this.model = model;
 	}
 
 	@Override
 	public IConcept getCompatibleObservationType(ISession session) {
 		return model.getCompatibleObservationType(session);
 	}
 
 	@Override
 	public IModel getConfigurableClone() {
 		return new ModelProxy(model);
 	}
 
 	@Override
 	public IConcept getObservable() {
 		return model.getObservable();
 	}
 
 	@Override
 	public boolean isResolved() {
 		return model.isResolved();
 	}
 
 	@Override
 	protected void validateMediatedModel(IModel model)
 			throws ThinklabValidationException {
 		// TODO
 	}
 
 	@Override
 	public Polylist buildDefinition(IKBox kbox, ISession session) throws ThinklabException {
 		return model.buildDefinition(kbox, session);
 	}
 
 	@Override
 	public Polylist conceptualize() throws ThinklabException {
 		return model.conceptualize();
 	}
 
 	@Override
 	public ModelResult observeInternal(IKBox kbox, ISession session,
 			IntelligentMap<IConformance> cp, ArrayList<IObservation> extents)
 			throws ThinklabException {
 		return model.observeInternal(kbox, session, cp, extents);
 	}
 	
 }
