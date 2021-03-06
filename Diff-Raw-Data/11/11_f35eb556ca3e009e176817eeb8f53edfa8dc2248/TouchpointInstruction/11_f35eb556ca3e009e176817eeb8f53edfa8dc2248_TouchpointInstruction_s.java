 /*******************************************************************************
  * Copyright (c) 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.equinox.internal.provisional.p2.metadata;
 
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * A touchpoint instruction contains either a sequence of instruction statements
  * to be executed during a particular engine phase, or some simple string value
  * that is needed by a touchpoint to execute its phases.
  * <p>
  * The format of a touchpoint instruction statement sequence is as follows:
  * 
  *   statement-sequence :
  *     | statement ';'
  *      | statement-sequence statement
  *      ;
  *
  *Where a statement is of the format:
  *
  *  statement :
  *      | actionName '(' parameters ')'
  *      ;
  *
  *  parameters :
  *      | // empty
  *      | parameter
  *      | parameters ',' parameter
  *      ;
  *
  *   parameter : 
  *      | paramName ':' paramValue
  *      ;
  *
  * actionName, paramName, paramValue :
  *      | String 
  *      ;
  *
  * @noextend This class is not intended to be subclassed by clients.
  * @see MetadataFactory#createTouchpointInstruction(String, String)
  */
 public class TouchpointInstruction {
 
 	private final String body;
 	private final String importAttribute;
 
 	/**
 	 * Encodes an action statement in string form. This method will
 	 * take care of escaping any illegal characters in function parameter values.
 	 * 
 	 * @param actionName The name of the action.
 	 * @param parameters The function's parameters. This is a Map<String,String>
 	 * where the keys are parameter names, and the values are parameter values
 	 * @return An encoded touchpoint instruction statement
 	 */
 	public static String encodeAction(String actionName, Map parameters) {
 		StringBuffer result = new StringBuffer(actionName);
 		for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
 			Map.Entry entry = (Map.Entry) it.next();
 			result.append(entry.getKey());
 			result.append(':');
 			appendEncoded(result, (String) entry.getValue());
 			if (it.hasNext())
 				result.append(',');
 		}
 		return result.toString();
 	}
 
 	/**
 	 * Append the given value to the given buffer, encoding any illegal characters
 	 * with appropriate escape sequences.
 	 */
 	private static void appendEncoded(StringBuffer buf, String value) {
 		char[] chars = value.toCharArray();
 		for (int i = 0; i < chars.length; i++) {
 			switch (chars[i]) {
 				case '$' :
 				case ',' :
 				case ':' :
 				case ';' :
 				case '{' :
 				case '}' :
 					buf.append("${#").append(Integer.toString(chars[i])).append('}'); //$NON-NLS-1$
 					break;
 				default :
 					buf.append(chars[i]);
 			}
 		}
 	}
 
 	/**
 	 * Clients must use the factory method on {@link MetadataFactory}.
 	 */
 	TouchpointInstruction(String body, String importAttribute) {
 		this.body = body;
 		this.importAttribute = importAttribute;
 	}
 
 	public boolean equals(Object obj) {
 		if (this == obj)
 			return true;
 		if (obj == null)
 			return false;
 		if (getClass() != obj.getClass())
 			return false;
 		TouchpointInstruction other = (TouchpointInstruction) obj;
 		if (body == null) {
 			if (other.body != null)
 				return false;
 		} else if (!body.equals(other.body))
 			return false;
 		if (importAttribute == null) {
 			if (other.importAttribute != null)
 				return false;
 		} else if (!importAttribute.equals(other.importAttribute))
 			return false;
 		return true;
 	}
 
 	/**
 	 * Returns the body of this touchpoint instruction. The body is either a sequence
 	 * of instruction statements, or a simple string value.
 	 * 
 	 * @return The body of this touchpoint instruction
 	 */
 	public String getBody() {
 		return body;
 	}
 
 	//TODO What is this? Please doc
 	public String getImportAttribute() {
 		return importAttribute;
 	}
 
 	public int hashCode() {
 		final int prime = 31;
 		int result = 1;
 		result = prime * result + ((body == null) ? 0 : body.hashCode());
 		result = prime * result + ((importAttribute == null) ? 0 : importAttribute.hashCode());
 		return result;
 	}
 }
