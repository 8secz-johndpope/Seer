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
 package org.eclipse.jface.text;
 
 
 /**
  * Standard implementation of {@link org.eclipse.jface.text.ILineTracker}.
  * <p>
  * The line tracker considers the three common line delimiters which are '\n',
  * '\r', '\r\n'.
  * <p>
  * This class is not intended to be subclassed.
  */
 public class DefaultLineTracker extends AbstractLineTracker {
 	
 	/** The predefined delimiters of this tracker */
	public final static String[] DELIMITERS= { "\r\n", "\r", "\n"  }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 	/** A predefined delimiter information which is always reused as return value */
 	private DelimiterInfo fDelimiterInfo= new DelimiterInfo();
 	
 	
 	/**
 	 * Creates a standard line tracker.
 	 */
 	public DefaultLineTracker() {
 	}
 	
 	/*
 	 * @see org.eclipse.jface.text.ILineTracker#getLegalLineDelimiters()
 	 */
 	public String[] getLegalLineDelimiters() {
 		return TextUtilities.copy(DELIMITERS);
 	}
 
 	/*
 	 * @see org.eclipse.jface.text.AbstractLineTracker#nextDelimiterInfo(java.lang.String, int)
 	 */
 	protected DelimiterInfo nextDelimiterInfo(String text, int offset) {
 		
 		char ch;
 		int length= text.length();
 		for (int i= offset; i < length; i++) {
 			
 			ch= text.charAt(i);
 			if (ch == '\r') {
 				
 				if (i + 1 < length) {
 					if (text.charAt(i + 1) == '\n') {
						fDelimiterInfo.delimiter= DELIMITERS[0];
 						fDelimiterInfo.delimiterIndex= i;
 						fDelimiterInfo.delimiterLength= 2;
 						return fDelimiterInfo;
 					}
 				}
 				
				fDelimiterInfo.delimiter= DELIMITERS[1];
 				fDelimiterInfo.delimiterIndex= i;
 				fDelimiterInfo.delimiterLength= 1;
 				return fDelimiterInfo;
 				
 			} else if (ch == '\n') {
 				
				fDelimiterInfo.delimiter= DELIMITERS[2];
 				fDelimiterInfo.delimiterIndex= i;
 				fDelimiterInfo.delimiterLength= 1;
 				return fDelimiterInfo;
 			}
 		}
 		
 		return null;
 	}
 }
