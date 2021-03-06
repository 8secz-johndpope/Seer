 /*******************************************************************************
  * Copyright (c) 2000, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.ui.synchronize;
 
 import org.eclipse.core.runtime.*;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.util.*;
 import org.eclipse.jface.viewers.IBasicPropertyConstants;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.internal.ui.*;
 import org.eclipse.team.internal.ui.Policy;
 import org.eclipse.team.internal.ui.registry.SynchronizeParticipantDescriptor;
 import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
 import org.eclipse.team.ui.TeamImages;
 import org.eclipse.ui.IMemento;
 import org.eclipse.ui.PartInitException;
 
 /**
  * This class is the abstract base class for all synchronize view participants. Clients must subclass
  * this class instead of directly implementing {@link AbstractSynchronizeParticipant}.
  * <p>
  * This class provides lifecycle support and hooks for configuration of synchronize view pages.
  * </p>
  * @see ISynchronizeParticipant
  * @since 3.0
  */
 public abstract class AbstractSynchronizeParticipant implements ISynchronizeParticipant {
 	
 	/**
 	 * Property key used in the property change event fired when the pinned
 	 * state of a participant changes.
 	 */
 	public static final String P_PINNED = "org.eclipse.team.pinned"; //$NON-NLS-1$
 	
 	// key for persisting the pinned state of a participant
 	private final static String CTX_PINNED = "root"; //$NON-NLS-1$
 	
 	// property listeners
 	private ListenerList fListeners;
 
 	private String fName;
 	private String fId;
 	private String fSecondaryId;
 	private boolean pinned;
 	private ImageDescriptor fImageDescriptor;
 	protected IConfigurationElement configElement;
 
 	/**
 	 * Notifies listeners of property changes, handling any exceptions
 	 */
 	class PropertyNotifier implements ISafeRunnable {
 
 		private IPropertyChangeListener fListener;
 		private PropertyChangeEvent fEvent;
 
 		/**
 		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
 		 */
 		public void handleException(Throwable exception) {
 			TeamUIPlugin.log(IStatus.ERROR, Policy.bind("AbstractSynchronizeParticipant.5"), exception); //$NON-NLS-1$
 		}
 
 		/**
 		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
 		 */
 		public void run() throws Exception {
 			fListener.propertyChange(fEvent);
 		}
 
 		/**
 		 * Notifies listeners of the property change
 		 * 
 		 * @param property
 		 *            the property that has changed
 		 */
 		public void notify(PropertyChangeEvent event) {
 			if (fListeners == null) {
 				return;
 			}
 			fEvent = event;
 			Object[] copiedListeners = fListeners.getListeners();
 			for (int i = 0; i < copiedListeners.length; i++) {
 				fListener = (IPropertyChangeListener) copiedListeners[i];
 				Platform.run(this);
 			}
 			fListener = null;
 		}
 	}
 
 	/**
 	 * Default constructor is a no-op. Subclasses that are persistable must support a no-arg constructor
 	 * and 
 	 */
 	public AbstractSynchronizeParticipant() {
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getName()
 	 */
 	public String getName() {
 		return fName;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getImageDescriptor()
 	 */
 	public ImageDescriptor getImageDescriptor() {
 		return fImageDescriptor;
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getId()
 	 */
 	public String getId() {
 		return fId;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getSecondaryId()
 	 */
 	public String getSecondaryId() {
 		return fSecondaryId;
 	}
 	
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#setPinned(boolean)
 	 */
 	public final void setPinned(boolean pinned) {
 		this.pinned = pinned;
 		pinned(pinned);
 		firePropertyChange(this, P_PINNED, Boolean.valueOf(!pinned), Boolean.valueOf(pinned));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#isPinned()
 	 */
 	public final boolean isPinned() {
 		return pinned;
 	}
 	
 	/**
 	 * Called when the pinned state is changed. Allows subclasses to react to pin state changes.
 	 *  
 	 * @param pinned whether the participant is pinned.
 	 */
 	protected void pinned(boolean pinned) {
 		// Subclasses can re-act to changes in the pinned state
 	}
 	
 	/* (non-Javadoc)
 	 * @see java.lang.Object#equals(java.lang.Object)
 	 */
 	public boolean equals(Object obj) {
 		if(obj == this) return true;
 		if( ! (obj instanceof ISynchronizeParticipant)) return false;
 		ISynchronizeParticipant other = (ISynchronizeParticipant)obj;
 		return getId().equals(other.getId()) && Utils.equalObject(getSecondaryId(), other.getSecondaryId());
 	}
 	
 	/* (non-Javadoc)
 	 * @see java.lang.Object#hashCode()
 	 */
 	public int hashCode() {
 		return Utils.getKey(getId(), getSecondaryId()).hashCode();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#doesSupportRefresh()
 	 */
 	public boolean doesSupportSynchronize() {
 		return true;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
 	 */
 	public void addPropertyChangeListener(IPropertyChangeListener listener) {
 		if (fListeners == null) {
 			fListeners = new ListenerList();
 		}
 		fListeners.add(listener);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
 	 */
 	public void removePropertyChangeListener(IPropertyChangeListener listener) {
 		if (fListeners != null) {
 			fListeners.remove(listener);
 		}
 	}
 
 	/**
 	 * Notify all listeners that the given property has changed.
 	 * 
 	 * @param source the object on which a property has changed
 	 * @param property identifier of the property that has changed
 	 * @param oldValue the old value of the property, or <code>null</code>
 	 * @param newValue the new value of the property, or <code>null</code>
 	 */
 	public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
 		if (fListeners == null) {
 			return;
 		}
 		PropertyNotifier notifier = new PropertyNotifier();
 		notifier.notify(new PropertyChangeEvent(source, property, oldValue, newValue));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
 	 */
 	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
 		//	Save config element.
 		configElement = config;
 
 		// Id
 		fId = config.getAttribute("id"); //$NON-NLS-1$
 		
 		// Title.
 		fName = config.getAttribute("name"); //$NON-NLS-1$
 		if (config == null) {
 			fName = "Unknown"; //$NON-NLS-1$
 		}
 
 		// Icon.
 		String strIcon = config.getAttribute("icon"); //$NON-NLS-1$
 		if (strIcon != null) {
 			fImageDescriptor = TeamImages.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), strIcon);
 		}
 	}
 	
 	protected void setInitializationData(ISynchronizeParticipantDescriptor descriptor) throws CoreException {
 		if(descriptor instanceof SynchronizeParticipantDescriptor) {
 			setInitializationData(((SynchronizeParticipantDescriptor)descriptor).getConfigurationElement(), null, null);
 		} else {
 			throw new TeamException(Policy.bind("AbstractSynchronizeParticipant.4")); //$NON-NLS-1$
 		}
 	}
 
 	/**
 	 * Sets the name of this participant to the specified value and notifies
 	 * property listeners of the change.
 	 * 
 	 * @param name the new name
 	 */
 	protected void setName(String name) {
 		String old = fName;
 		fName = name;
 		firePropertyChange(this, IBasicPropertyConstants.P_TEXT, old, name);
 	}
 	
 	/**
 	 * Sets the image descriptor for this participant to the specified value and
 	 * notifies property listeners of the change.
 	 * 
 	 * @param imageDescriptor the new image descriptor
 	 */
 	protected void setImageDescriptor(ImageDescriptor imageDescriptor) {
 		ImageDescriptor old = fImageDescriptor;
 		fImageDescriptor = imageDescriptor;
 		firePropertyChange(this, IBasicPropertyConstants.P_IMAGE, old, imageDescriptor);
 	}
 	
 	/**
 	 * Sets the secondary id for this participant.
 	 * 
 	 * @param secondaryId the secondary id for this participant.
 	 */
 	protected void setSecondaryId(String secondaryId) {
 		this.fSecondaryId = secondaryId; 
 	}
 	
 	/**
 	 * Classes that are persisted must override this method and perform
 	 * the following initialization.
 	 * <pre>
 	 * 		super.init(secondaryId, memento);
 	 * 		try {
 	 *			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(PARTICIPANT_ID);
 	 *			setInitializationData(descriptor);
 	 *		} catch (CoreException e) {
 	 *			TeamUIPlugin.log(e);
 	 *		}
 	 * </pre>
 	 * where <code>PARTICIPANT_ID</code> is the id of the particant as defined in the plugin manifest.
 	 * </p>
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(String, org.eclipse.ui.IMemento)
 	 */
 	public void init(String secondaryId, IMemento memento) throws PartInitException {
 		setSecondaryId(secondaryId);
 		pinned = Boolean.valueOf(memento.getString(CTX_PINNED)).booleanValue();
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
 	 */
 	public void saveState(IMemento memento) {
 		memento.putString(CTX_PINNED, Boolean.toString(pinned));
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPageConfiguration()
 	 */
 	public final ISynchronizePageConfiguration createPageConfiguration() {
 		SynchronizePageConfiguration configuration = new SynchronizePageConfiguration(this);
 		initializeConfiguration(configuration);
 		return configuration;
 	}
 
 	/**
 	 * This method is invoked after a page configuration is created but before it is returned by the 
 	 * <code>createPageConfiguration</code> method. Subclasses can implement this method to
 	 * tailor the configuration in ways appropriate to the participant.
 	 * 
 	 * @param configuration the newly create page configuration
 	 */
 	protected abstract void initializeConfiguration(ISynchronizePageConfiguration configuration);
 }
