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
 package org.eclipse.team.internal.ui.registry;
 
 import org.eclipse.core.runtime.*;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.team.internal.ui.TeamUIPlugin;
 import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
 
 public class SynchronizeParticipantDescriptor implements ISynchronizeParticipantDescriptor {
 	public  static final String ATT_ID = "id"; //$NON-NLS-1$
 	public  static final String ATT_NAME = "name"; //$NON-NLS-1$
 	public  static final String ATT_ICON = "icon"; //$NON-NLS-1$
 	public  static final String ATT_CLASS = "class"; //$NON-NLS-1$
 	private static final String ATT_TYPE = "type"; //$NON-NLS-1$	
 	private static final String ATT_PERSISTENT = "persistent"; //$NON-NLS-1$
 	
 	private String label;
 	private String className;
 	private String type;
 	private String id;
 	private boolean persistent;
 	private ImageDescriptor imageDescriptor;
 	private String description;
 	
 	private IConfigurationElement configElement;
 
 	/**
 	 * Create a new ViewDescriptor for an extension.
 	 */
 	public SynchronizeParticipantDescriptor(IConfigurationElement e, String desc) throws CoreException {
 		configElement = e;
 		description = desc;
 		loadFromExtension();
 	}
 
 	public IConfigurationElement getConfigurationElement() {
 		return configElement;
 	}
 
 	/**
 	 * Returns this view's description. This is the value of its <code>"description"</code>
 	 * attribute.
 	 * 
 	 * @return the description
 	 */
 	public String getDescription() {
 		return description;
 	}
 	
 	public String getId() {
 		return id;
 	}
 
 	public String getType() {
 		return type;
 	}
 	
 	public ImageDescriptor getImageDescriptor() {
 		if (imageDescriptor != null)
 			return imageDescriptor;
 		String iconName = configElement.getAttribute(ATT_ICON);
 		if (iconName == null)
 			return null;
 		imageDescriptor = TeamUIPlugin.getImageDescriptorFromExtension(configElement.getDeclaringExtension(), iconName);
 		return imageDescriptor;
 	}
 
 	public String getName() {
 		return label;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor#isPersistent()
 	 */
 	public boolean isPersistent() {
 		return persistent;
 	}
 	
 	/**
 	 * load a view descriptor from the registry.
 	 */
 	private void loadFromExtension() throws CoreException {
 		String identifier = configElement.getAttribute(ATT_ID);
 		label = configElement.getAttribute(ATT_NAME);
 		className = configElement.getAttribute(ATT_CLASS);
 		type = configElement.getAttribute(ATT_TYPE);
 		String persistentString = configElement.getAttribute(ATT_PERSISTENT);
 		if(persistentString == null) {
 			persistent = true;
 		} else {
 			persistent = Boolean.valueOf(persistentString).booleanValue();
 		}
 		// Sanity check.
 		if ((label == null) || (className == null) || (identifier == null)) {
			throw new CoreException(new Status(IStatus.ERROR, configElement.getDeclaringExtension().getNamespace(), 0, "Invalid extension (missing label or class name): " + identifier, //$NON-NLS-1$
 					null));
 		}
 		id = identifier;
 	}
 
 	/**
 	 * Returns a string representation of this descriptor. For debugging
 	 * purposes only.
 	 */
 	public String toString() {
 		return "Synchronize Participant(" + getId() + ")"; //$NON-NLS-2$//$NON-NLS-1$
 	}
 }
