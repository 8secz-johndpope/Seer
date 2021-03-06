 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jdt.debug.core;
 
 
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.model.IVariable;
 
 /**
  * A local variable, field slot, or receiver (this) in a Java virtual machine.
  * <p>
  * Clients are not intended to implement this interface.
  * </p>
  * @see org.eclipse.debug.core.model.IVariable
  */
 public interface IJavaVariable extends IVariable, IJavaModifiers {
 	
 	/**
 	 * Returns the JNI-style signature for the declared type of this
 	 * variable, or <code>null</code> if the type associated with the
 	 * signature is not yet loaded in the target VM.
 	 *
 	 * @return signature, or <code>null</code> if not accessible
 	 * @exception DebugException if this method fails.  Reasons include:
 	 * <ul><li>Failure communicating with the VM.  The DebugException's
 	 * status code contains the underlying exception responsible for
 	 * the failure.</li>
 	 * <li>The type associated with the signature is not yet loaded</li></ul>
 	 */
 	public String getSignature() throws DebugException;
 	
 	/**
 	 * Returns the generic signature as defined in the JVM
 	 * specification for the declared type of this variable,
 	 * or <code>null</code> if the type associated with the
 	 * signature is not yet loaded in the target VM.
 	 * Returns the same value as #getSignature() if the declared type
 	 * of this variable is not a generic type.
 	 *
 	 * @return generic signature, or <code>null</code> if not accessible
 	 * @exception DebugException if this method fails.  Reasons include:
 	 * <ul><li>Failure communicating with the VM.  The DebugException's
 	 * status code contains the underlying exception responsible for
 	 * the failure.</li>
 	 * <li>The type associated with the signature is not yet loaded</li></ul>
 	 */
 	public String getGenericSignature() throws DebugException;
 	
 	/**
 	 * Returns the declared type of this variable.
 	 * 
 	 * @return the declared type of this variable
 	 * @exception DebugException if this method fails.  Reasons include:
 	 * <ul><li>Failure communicating with the VM.  The DebugException's
 	 * status code contains the underlying exception responsible for
 	 * the failure.</li>
 	 * <li>The type associated with the signature is not yet loaded</li></ul>
 	 * @since 2.0
 	 */
 	public IJavaType getJavaType() throws DebugException;	
 	
 	/**
 	 * Returns whether this variable is local.
 	 * 
 	 * @return whether this variable is a local variable
 	 * @exception DebugException if this method fails.  Reasons include:
 	 * <ul><li>Failure communicating with the VM.  The DebugException's
 	 * status code contains the underlying exception responsible for
 	 * the failure.</li></ul>
 	 * @since 2.1
 	 */
 	public boolean isLocal() throws DebugException;
 	
 }
 
 
