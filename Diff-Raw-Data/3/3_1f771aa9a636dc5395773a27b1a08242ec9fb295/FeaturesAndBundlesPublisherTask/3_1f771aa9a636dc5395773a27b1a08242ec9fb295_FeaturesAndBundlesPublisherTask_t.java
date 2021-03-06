 /*******************************************************************************
  * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors: IBM - Initial API and implementation
  ******************************************************************************/
 package org.eclipse.equinox.internal.p2.publisher.ant;
 
 import java.io.File;
 import java.util.*;
 import org.apache.tools.ant.BuildException;
 import org.apache.tools.ant.DirectoryScanner;
 import org.apache.tools.ant.types.FileSet;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.p2.publisher.IPublisherAction;
 import org.eclipse.equinox.p2.publisher.Publisher;
 import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
 import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
 
 public class FeaturesAndBundlesPublisherTask extends AbstractPublishTask {
 	private ArrayList features = new ArrayList();
 	private ArrayList bundles = new ArrayList();
 
 	public void execute() throws BuildException {
 		try {
 			initializeRepositories(getInfo());
 		} catch (ProvisionException e) {
 			throw new BuildException("Unable to configure repositories", e); //$NON-NLS-1$
 		}
 
 		File[] f = getLocations(features);
 		File[] b = getLocations(bundles);
 
 		ArrayList actions = new ArrayList();
 		if (f.length > 0)
 			actions.add(new FeaturesAction(f));
 		if (b.length > 0)
 			actions.add(new BundlesAction(b));
 
		if (actions.size() > 0)
			new Publisher(getInfo()).publish((IPublisherAction[]) actions.toArray(new IPublisherAction[actions.size()]), new NullProgressMonitor());
 	}
 
 	private File[] getLocations(List collection) {
 		ArrayList results = new ArrayList();
 		for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
 			FileSet set = (FileSet) iterator.next();
 
 			DirectoryScanner scanner = set.getDirectoryScanner(getProject());
 			String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
 			for (int i = 0; i < 2; i++) {
 				for (int j = 0; j < elements[i].length; j++) {
 					results.add(new File(set.getDir(), elements[i][j]));
 				}
 			}
 		}
 		return (File[]) results.toArray(new File[results.size()]);
 	}
 
 	public FileSet createFeatures() {
 		FileSet set = new FileSet();
 		features.add(set);
 		return set;
 	}
 
 	public FileSet createBundles() {
 		FileSet set = new FileSet();
 		bundles.add(set);
 		return set;
 	}
 }
