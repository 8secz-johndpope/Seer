 /*******************************************************************************
  * Copyright (c) 2013 EclipseSource Muenchen GmbH.
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
 * Otto von Wesendonk, Edgar Mueller - initial API and implementation
  ******************************************************************************/
 package org.eclipse.emf.emfstore.client;
 
import org.eclipse.emf.ecore.xmi.DanglingHREFException;
 import org.eclipse.emf.emfstore.common.model.ESFactory;
 
 /**
  * Factory for creating {@link ESServer} instances.
  * 
  * @author wesendon
  * @author emueller
  */
 public interface ESServerFactory extends ESFactory {
 
 	/**
 	 * Creates a server without a local name.
	 * The created server is not automatically added to the workspace, i.e.
	 * most users will call {@link org.eclipse.emf.emfstore.client.ESWorkspace#addServer(ESServer)} in order to
	 * to avoid {@link DanglingHREFException}s upon saving.
 	 * 
 	 * @param url
 	 *            the URL of the server
 	 * @param port
 	 *            the port of the server where EMFStore is listing on
 	 * @param certificate
 	 *            the certificate alias to be used
 	 * 
 	 * @return an {@link ESServer} instance representing the remote server
	 * 
	 * @see {@link org.eclipse.emf.emfstore.client.ESWorkspace#addServer(ESServer)}
 	 */
 	ESServer createServer(String url, int port, String certificate);
 
 	/**
 	 * Creates a server with a local name.
	 * The created server is not automatically added to the workspace, i.e.
	 * most users will call {@link org.eclipse.emf.emfstore.client.ESWorkspace#addServer(ESServer)} in order to
	 * to avoid {@link DanglingHREFException}s upon saving.
 	 * 
 	 * @param name
 	 *            the local name of the server
 	 * @param url
 	 *            the URL of the server
 	 * @param port
 	 *            the port of the server where EMFStore is listing on
 	 * @param certificate
 	 *            the certificate alias to be used
 	 * 
 	 * @return an {@link ESServer} instance representing the remote server
	 * 
	 * @see {@link org.eclipse.emf.emfstore.client.ESWorkspace#addServer(ESServer)}
 	 */
 	ESServer createServer(String name, String url, int port, String certificate);
 
 }
