 /*******************************************************************************
  * Copyright (c) 2004 Actuate Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *  Actuate Corporation  - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.birt.core.script;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import org.mozilla.javascript.NativeJavaObject;
 import org.mozilla.javascript.Scriptable;
 
 /**
  * Represents the scriptable object for Java object which implements the
  * interface <code>Map</code>.
  * 
 * @version $Revision: 1.5 $ $Date: 2005/05/08 06:58:29 $
  */
 class NativeJavaMap extends NativeJavaObject
 {
 
 	public NativeJavaMap( )
 	{
 	}
 
 	public NativeJavaMap( Scriptable scope, Object javaObject, Class staticType )
 	{
 		super( scope, javaObject, staticType );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String,
 	 *      org.mozilla.javascript.Scriptable)
 	 */
 
 	public boolean has( String name, Scriptable start )
 	{
 		return ( (Map) javaObject ).containsKey( name );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String,
 	 *      org.mozilla.javascript.Scriptable)
 	 */
 
 	public Object get( String name, Scriptable start )
 	{
 		// Support the array member "length".
 
 		if ( name.equalsIgnoreCase( "length" ) ) //$NON-NLS-1$
 			return new Integer( ( (Map) javaObject ).values( ).size( ) );
 
 		return ( (Map) javaObject ).get( name );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String,
 	 *      org.mozilla.javascript.Scriptable, java.lang.Object)
 	 */
 
 	public void put( String name, Scriptable start, Object value )
 	{
 		( (Map) javaObject ).put( name, value );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
 	 */
 
 	public void delete( String name )
 	{
 		( (Map) javaObject ).remove( name );
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.mozilla.javascript.Scriptable#get(int,
 	 *      org.mozilla.javascript.Scriptable)
 	 */
 
 	public Object get( int index, Scriptable start )
 	{
		return ( (Map) javaObject ).get(new Integer(index).toString());
	}
	
	public void put(int index, Scriptable start, Object value)
	{
		( (Map) javaObject ).put(new Integer(index).toString(), value);
 	}
	 
 }
