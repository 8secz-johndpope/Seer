 /*
  * Copyright (c) 2012 European Synchrotron Radiation Facility,
  *                    Diamond Light Source Ltd.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  */ 
 package org.dawb.passerelle.common.actors;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.dawb.passerelle.common.message.DataMessageException;
 import org.dawb.passerelle.common.message.IVariable;
 import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
 import org.dawb.passerelle.common.message.IVariableProvider;
 import org.dawb.passerelle.common.message.MessageUtils;
 import org.dawb.passerelle.common.message.Variable;
 import org.dawb.workbench.jmx.UserDebugBean;
 import org.eclipse.core.resources.IProject;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import ptolemy.actor.Actor;
 import ptolemy.actor.IOPort;
 import ptolemy.actor.NoRoomException;
 import ptolemy.kernel.CompositeEntity;
 import ptolemy.kernel.util.ChangeListener;
 import ptolemy.kernel.util.ChangeRequest;
 import ptolemy.kernel.util.IllegalActionException;
 import ptolemy.kernel.util.NameDuplicationException;
 import ptolemy.kernel.util.NamedObj;
 
 import com.isencia.passerelle.actor.ProcessingException;
 import com.isencia.passerelle.actor.TriggeredSource;
 import com.isencia.passerelle.core.Port;
 import com.isencia.passerelle.message.ManagedMessage;
 import com.isencia.passerelle.workbench.model.utils.ModelUtils;
 
 public abstract class AbstractDataMessageSource extends TriggeredSource implements IVariableProvider, IProjectNamedObject {
 
 	private static final Logger logger = LoggerFactory.getLogger(AbstractDataMessageSource.class);
 	/**
 	 * 
 	 */
 	private static final long serialVersionUID = -5224572869848106075L;
 
 
 	/**
 	 * Cached variables
 	 */
 	protected ArrayList<IVariable> cachedUpstreamVariables;
 
 	public AbstractDataMessageSource(final CompositeEntity container, final String name) throws NameDuplicationException, IllegalActionException {
 		
 		super(container, name);
 		
 		this.cachedUpstreamVariables = new ArrayList<IVariable>(7);
 
 		// Any change upsteam means they are invalid.
 		container.addChangeListener(new ChangeListener() {		
 			@Override
 			public void changeFailed(ChangeRequest change, Exception exception) { }
 			@Override
 			public void changeExecuted(ChangeRequest change) {
 				cachedUpstreamVariables.clear();
 				if (!container.deepContains(AbstractDataMessageSource.this)) {
 					container.removeChangeListener(this);
 				}
 			}
 		});
 		
 		ActorUtils.createDebugAttribute(this);
 
 	}
 	
 	
 	protected final ManagedMessage getMessage() throws ProcessingException {
 		try {
 			ActorUtils.setActorExecuting(this, true);
 			ManagedMessage mm = getDataMessage();
			if (mm != null) {
				try {
					UserDebugBean bean = ActorUtils.create(this, null, MessageUtils.coerceMessage(mm));
					if (bean!=null) bean.setPortName(output.getDisplayName());
					ActorUtils.debug(this, bean);
				} catch (Exception e) {
					logger.trace("Unable to debug!", e);
				}
 			}
			return mm;
 		} finally {
 			ActorUtils.setActorExecuting(this, false);
 		}
 	}
 	
 	protected abstract ManagedMessage getDataMessage()  throws ProcessingException;
 
     /**
      * Adds the scalar and the 
      */
 	@Override
 	public List<IVariable> getOutputVariables() {
         return getScalarOutputVariables();
 	}
 	
     /**
      * By default just passes the upstream string and path variables as 
      * these are normally preserved
      */
 	protected List<IVariable> getScalarOutputVariables() {
 
 		final List<IVariable> ret = new ArrayList<IVariable>(7);
 		final List<IVariable> all = getInputVariables();
 		for (IVariable iVariable : all) {
 			if (iVariable.getVariableType()==VARIABLE_TYPE.PATH || 
 			    iVariable.getVariableType()==VARIABLE_TYPE.SCALAR|| 
 			    iVariable.getVariableType()==VARIABLE_TYPE.XML) {
 				ret.add(iVariable);
 			}
 		}
 		try {
 			ret.add(new Variable("project_name", VARIABLE_TYPE.SCALAR, ModelUtils.getProject(this).getName()));
 		} catch (Exception e) {
 			logger.error("Cannot find project!", e);
 		}
 		return ret;
 	}
 	
 	protected void sendOutputMsg(Port port, ManagedMessage message) throws ProcessingException, IllegalArgumentException {
         
 		if (port!=output) {
 			super.sendOutputMsg(port, message);
 			return;
 		}
 		
 		while(true) {
 			try {
 	        	super.sendOutputMsg(port, message);
 	        	break;
 	        	
 	        } catch (Exception ne) {
 	        	if (ne.getCause()!=null && ne.getCause() instanceof NoRoomException) {
 		        	try {
 						Thread.sleep(100);// TODO Configurable.
 						continue;
 					} catch (InterruptedException e) {
 						throw (NoRoomException)ne.getCause();
 					} 
 	        	}
 	        	throw createDataMessageException("Cannot send output!", ne);
 	        }
 		}
 		
 	}
 
 	/**
      * These are cached and cleared when the model is.
      * @return
      */
 	public List<IVariable> getInputVariables() {
 
 		synchronized (cachedUpstreamVariables) {
 			
 			if (cachedUpstreamVariables.isEmpty()) {
 				@SuppressWarnings("rawtypes")
 				final List connections = trigger.connectedPortList();
 				for (Object object : connections) {
 					final IOPort  port     = (IOPort)object;
 					final Actor connection = (Actor)port.getContainer();
 					if (connection instanceof IVariableProvider) {
 						final List<IVariable> vars = ((IVariableProvider)connection).getOutputVariables();
 						if (vars!=null) cachedUpstreamVariables.addAll(vars);
 					}
 				}
 			}
 		}
 		return cachedUpstreamVariables;
 	}
 	
 	public boolean isUpstreamVariable(final String name) {
 		final List<IVariable> up = getInputVariables();
 		for (IVariable iVariable : up) {
 			if (iVariable.getVariableName().equals(name)) return true;
 		}
 		return false;
 	}	
 
 	protected DataMessageException createDataMessageException(String msg,Throwable e) throws DataMessageException {
 		return new DataMessageException(msg, this, e);
 	}
 	
 	protected String getModelPath() {
 		if (getContainer()==null) return null;
 		final String source = getContainer().getSource();
 		return source;
 	}
 
 	public IProject getProject() {
 		try {
 			return ModelUtils.getProject(this);
 		} catch (Exception e) {
 			logger.error("Cannot get the project for actor "+getName(), e);
 			return null;
 		}
 	}
 	
 	public NamedObj getObject() {
 		return this;
 	}
 	
 }
