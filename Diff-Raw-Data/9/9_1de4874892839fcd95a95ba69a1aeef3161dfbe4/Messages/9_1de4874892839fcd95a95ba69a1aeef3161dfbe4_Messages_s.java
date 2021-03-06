 /**
  * Copyright (c) 2006 IBM Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - Initial Implementation
  *
  */
 package org.eclipse.ptp.remotetools.environment.generichost.core;
 
 import org.eclipse.osgi.util.NLS;
 
 /**
  * @author Daniel Felix Ferber
  *
  * @since 3.0
  */
 public class Messages extends NLS
 {
	private static final String BUNDLE_ID = "org.eclipse.ptp.remotetools.environment.generichost.core.messages"; //$NON-NLS-1$
 
 	public static String TargetControl_create_MonitorConnecting;
 	public static String TargetControl_resume_CannotResume;
 	public static String TargetControl_stop_CannotPause;
 	
 	static {
 		// load message values from bundle file
 		NLS.initializeMessages(BUNDLE_ID, Messages.class);
 	}
 
 	private Messages() {
 		// cannot create new instance
 	}
 }
 
 	
