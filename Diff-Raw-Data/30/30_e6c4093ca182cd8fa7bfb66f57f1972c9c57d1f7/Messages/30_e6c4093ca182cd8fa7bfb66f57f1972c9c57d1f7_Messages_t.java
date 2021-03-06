 /*******************************************************************************
  * Copyright (c) 2013 Ericsson.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Matthew Khouzam - Initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.lttng2.ust.core.trace;
 
 import org.eclipse.osgi.util.NLS;
 
 /**
  * Message bundle for lttng2.kernel.core.trace
  *
  * @author Matthew Khouzam
  * @since 2.1
  */
 public class Messages extends NLS {
 
     private static final String BUNDLE_NAME = "org.eclipse.linuxtools.lttng2.ust.core.trace.messages"; //$NON-NLS-1$
 
     /**
      * The domain is not "ust"
      */
     public static String LttngUstTrace_DomainError;
 
    /**
     * Malformed trace (buffer overflow maybe?)
     */
    public static String LttngUstTrace_MalformedTrace;

    /**
     * Trace read error
     */
    public static String LttngUstTrace_TraceReadError;

     static {
         // initialize resource bundle
         NLS.initializeMessages(BUNDLE_NAME, Messages.class);
     }
 
     private Messages() {
     }
 }
