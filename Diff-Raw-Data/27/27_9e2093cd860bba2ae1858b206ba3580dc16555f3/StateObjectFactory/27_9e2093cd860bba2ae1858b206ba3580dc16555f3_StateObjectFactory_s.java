 /*******************************************************************************
  * Copyright (c) 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.osgi.service.resolver;
 
 import java.io.*;
 import java.util.Dictionary;
 import org.osgi.framework.BundleException;
 
 public interface StateObjectFactory {
 	public State createState();
 	public State createState(State state);	
 	public BundleDescription createBundleDescription(long id, String globalName, Version version, String location, BundleSpecification[] required, HostSpecification host, PackageSpecification[] packages, String[] providedPackages);
 	/**
 	 * Returns a bundle description based on the information in the supplied manifest dictionary.
 	 * The manifest should contain String keys and String values which correspond to 
 	 * proper OSGi manifest headers and values.
 	 * 
 	 * @param manifest a collection of OSGi manifest headers and values
 	 * @param location the URL location of the bundle
 	 * @param id the id of the bundle
 	 * @return a bundle description derived from the given information
 	 * @throws BundleException if an error occurs while reading the manifest 
 	 */	
 	public BundleDescription createBundleDescription(Dictionary manifest, String location, long id) throws BundleException;
 	public BundleDescription createBundleDescription(BundleDescription original);
	public BundleSpecification createBundleSpecification(BundleDescription parentBundle, String hostGlobalName, Version hostVersion, byte matchRule, boolean export, boolean optional);
 	public BundleSpecification createBundleSpecification(BundleSpecification original);	
	public HostSpecification createHostSpecification(BundleDescription parentBundle, String hostGlobalName, Version hostVersion, byte matchRule, boolean reloadHost);
 	public HostSpecification createHostSpecification(HostSpecification original);	
	public PackageSpecification createPackageSpecification(BundleDescription parentBundle, String packageName, Version packageVersion, boolean exported);
 	public PackageSpecification createPackageSpecification(PackageSpecification original);
 	/**
 	 * Persists the given state in the given output stream. Closes the stream.
 	 * @param state
 	 * @param stream
 	 * @throws IOException
 	 * @throws IllegalArgumentException if the state provided was not created by this factory
 	 */
 	public void writeState(State state, DataOutputStream stream) throws IOException;
 	/**
 	 * Reads a persisted state from the given stream. Closes the stream.
 	 * @param stream
 	 * @return
 	 * @throws IOException
 	 */
 	public State readState(DataInputStream stream) throws IOException;
 }
