 /*******************************************************************************
  * Copyright (c) 2007 Alphonse Van Assche.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Alphonse Van Assche - initial API and implementation
  *******************************************************************************/
 package org.eclipse.linuxtools.rpmstubby;
 
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.linuxtools.rpmstubby.model.MainPackage;
 import org.eclipse.linuxtools.rpmstubby.model.PackageItem;
 import org.eclipse.linuxtools.rpmstubby.model.SubPackage;
 import org.eclipse.linuxtools.rpmstubby.preferences.PreferenceConstants;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.ide.IDE;
 
 /**
  * Generates the RPM specfile and the fetch script based on the feature and user 
  * preferences.
  *
  */
 public class StubbyGenerator {
 
 	private MainPackage mainPackage;
 	private List<SubPackage> subPackages;
 	private boolean withGCJSupport;
 	private boolean withFetchScript;
 	private boolean usePdebuildScript;
 	private IPreferenceStore store;
 
 	/**
 	 * Creates the specfile and fetch script generator for the given packages.
 	 * 
 	 * @param mainPackage The main feature.
 	 * @param subPackages The included features or plugins.
 	 */
 	public StubbyGenerator(MainPackage mainPackage, List<SubPackage> subPackages) {
 		this.mainPackage = mainPackage;
 		this.subPackages = subPackages;
 		store = StubbyPlugin.getDefault().getPreferenceStore();
 		this.withGCJSupport = store
 				.getBoolean(PreferenceConstants.P_STUBBY_WITH_GCJ);
 		this.withFetchScript = store
 				.getBoolean(PreferenceConstants.P_STUBBY_WITH_FETCH_SCRIPT);
 		this.usePdebuildScript = store
 				.getBoolean(PreferenceConstants.P_STUBBY_USE_PDEBUILD_SCRIPT);
 	}
 
 	/**
 	 * Generates a RPM specfile based on the parsed data from the feature.xml.
 	 * @return The generated specfile.
 	 */
 	public String generateSpecfile() {
 		StringBuilder buffer = new StringBuilder();
 		String simplePackageName = getPackageName(mainPackage.getName());
 		String packageName = "eclipse-" + simplePackageName;
 		if (withGCJSupport)
 			buffer.append("%define gcj_support    1\n");
 		if (withFetchScript)
 			buffer.append("%define src_repo_tag   FIXME\n");
 		buffer.append("%define eclipse_base   %{_libdir}/eclipse\n");
 		buffer.append("%define install_loc    %{_datadir}/eclipse/dropins/"
 				+ simplePackageName + "\n\n");
 		buffer.append("Name:           " + packageName + "\n");
 		buffer.append("Version:        "
 				+ mainPackage.getVersion().replaceAll("\\.qualifier", "")
 				+ "\n");
 		buffer.append("Release:        1%{?dist}" + "\n");
 		buffer.append("Summary:        " + mainPackage.getSummary() + "\n\n");
 		buffer.append("Group:          Development/Tools\n");
 		buffer.append("License:        " + mainPackage.getLicense() + "\n");
 		buffer.append("URL:            " + mainPackage.getURL() + "\n");
 		if (withFetchScript) {
 			String fetchScriptName = packageName + "-fetch-src.sh";
 			buffer.append("## sh ").append(fetchScriptName).append("\n");
 			buffer.append("Source0:        %{name}-fetched-src-%{src_repo_tag}.tar.bz2\n");
 			buffer.append("Source1:        " ).append(fetchScriptName).append("\n");
 		} else {
 			buffer.append("Source0:        FIXME\n");
 		}
 		buffer
 				.append("BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)\n\n");
 		if (withGCJSupport) {
 			buffer.append("%if %{gcj_support}\n");
 			buffer.append("BuildRequires:    gcc-java\n");
 			buffer.append("BuildRequires:    java-gcj-compat-devel\n");
 			buffer.append("Requires(post):   java-gcj-compat\n");
 			buffer.append("Requires(postun): java-gcj-compat\n");
 			buffer.append("%else\n");
 			buffer.append("BuildRequires:    java-devel >= 1.5.0\n");
 			buffer.append("%endif\n");
 			buffer.append("%if ! %{gcj_support}\n");
 			buffer.append("BuildArch: noarch\n");
 			buffer.append("%endif\n\n");
 		} else
 			buffer.append("BuildArch: noarch\n\n");
 		buffer.append("BuildRequires: eclipse-pde >= 1:3.4.0\n");
 		buffer.append("Requires: eclipse-platform >= 3.4.0\n");
 		buffer.append(getDepsOrReqs("Requires: ", mainPackage.getRequires()));
 		buffer.append("\n%description\n" + mainPackage.getDescription() + "\n");
 		for (SubPackage subPackage : subPackages) {
 			String subPackageName = getPackageName(subPackage.getName());
 			buffer.append("\n%package  " + subPackageName + "\n");
 			buffer.append("Summary:  " + subPackage.getSummary() + "\n");
 			buffer.append("Requires: %{name} = %{version}-%{release}\n");
 			buffer
 					.append(getDepsOrReqs("Requires: ", subPackage
 							.getRequires()));
 			buffer
 					.append(getDepsOrReqs("Provides: ", subPackage
 							.getProvides()));
 			buffer.append("Group: Development/Tools\n\n");
 			buffer.append("%description " + subPackageName + "\n");
 			buffer.append(subPackage.getDescription() + "\n");
 		}
 		generatePrepSection(buffer);
 
 		generateBuildSection(buffer);
 		buffer.append("%install\n");
 		buffer.append("%{__rm} -rf %{buildroot}\n");
 		buffer.append("install -d -m 755 $RPM_BUILD_ROOT%{install_loc}\n\n");
 		buffer.append("%{__unzip} -q -d $RPM_BUILD_ROOT%{install_loc} \\\n");
 		buffer.append("     build/rpmBuild/" + mainPackage.getName()
 				+ ".zip \n\n");
 		if (withGCJSupport) {
 			buffer.append("%if %{gcj_support}\n");
 			buffer.append("  %{_bindir}/aot-compile-rpm\n");
 			buffer.append("%endif\n\n");
 		}
 		buffer.append("%clean\n");
 		buffer.append("%{__rm} -rf %{buildroot}\n\n");
 		if (withGCJSupport) {
 			buffer.append("%if %{gcj_support}\n");
 			buffer.append("%post\n");
 			buffer.append("if [ -x %{_bindir}/rebuild-gcj-db ]; then\n");
 			buffer.append("  %{_bindir}/rebuild-gcj-db\n");
 			buffer.append("fi\n\n");
 			buffer.append("%preun\n");
 			buffer.append("if [ -x %{_bindir}/rebuild-gcj-db ]; then\n");
 			buffer.append("  %{_bindir}/rebuild-gcj-db\n");
 			buffer.append("fi\n");
 			buffer.append("%endif\n\n");
 		}
 		buffer.append("%files\n");
 		buffer.append("%defattr(-,root,root,-)\n");
 		buffer.append("%{install_loc}\n\n");
 		for (SubPackage subPackage : subPackages) {
 			buffer.append("%files " + getPackageName(subPackage.getName())
 					+ "\n");
 			buffer.append("%dir %{eclipse_base}/features/"
 					+ subPackage.getName() + "_*/\n");
 			buffer.append("%doc %{eclipse_base}/features/"
 					+ subPackage.getName() + "_*/*.html\n");
 			buffer.append("%{eclipse_base}/features/" + subPackage.getName()
 					+ "_*/feature.*\n");
 			buffer.append(getPackageFiles(subPackage.getProvides(),
 					withGCJSupport)
 					+ "\n");
 		}
 		buffer.append("%changelog\n\n");
 		buffer.append("FIXME\n");
 		return buffer.toString();
 	}
 
 	private void generatePrepSection(StringBuilder buffer) {
		buffer.append("\n%prep\n%setup -q -n FIXME\n\n");
 		if (!usePdebuildScript) {
 			buffer
 					.append("/bin/sh -x %{eclipse_base}/buildscripts/copy-platform SDK %{eclipse_base}\n");
 			buffer.append("mkdir home\n\n");
 		}
 	}
 
 	private void generateBuildSection(StringBuilder buffer) {
 		buffer.append("%build\n");
 		if (!usePdebuildScript) {
 			buffer.append("SDK=$(cd SDK > /dev/null && pwd)\n");
 			buffer.append("homedir=$(cd home > /dev/null && pwd)\n");
 			buffer.append("java -cp $SDK/startup.jar \\\n");
 			buffer
 					.append("     -Dosgi.sharedConfiguration.area=%{_libdir}/eclipse/configuration \\\n");
 			buffer.append("      org.eclipse.core.launcher.Main \\\n");
 			buffer
 					.append("     -application org.eclipse.ant.core.antRunner \\\n");
 			buffer.append("     -Dtype=feature \\\n");
 			buffer.append("     -Did=" + mainPackage.getName() + "\\\n");
 			buffer.append("     -DbaseLocation=$SDK \\\n");
 			buffer.append("     -DsourceDirectory=$(pwd) \\\n");
 			buffer.append("     -DbuildDirectory=$(pwd)/build \\\n");
 			buffer
 					.append("     -Dbuilder=%{eclipse_base}/plugins/org.eclipse.pde.build/templates/package-build \\\n");
 			buffer
 					.append("     -f %{eclipse_base}/plugins/org.eclipse.pde.build/scripts/build.xml \\\n");
 			buffer.append("     -vmargs -Duser.home=$homedir");
 		} else {
 			buffer.append("%{eclipse_base}/buildscripts/pdebuild -f ").append(
 					mainPackage.getName());
 		}
 		buffer.append("\n\n");
 	}
 
 	/**
 	 * Generates a fetch script. The script is better suited for cvs for now.
 	 * It lists all the plugins and features and tries to export them.
 	 * @return The generated fetch script.
 	 */
 	public String generateFetchScript() {
 		StringBuilder buffer = new StringBuilder();
 		buffer.append("#!/bin/sh\n");
 		buffer.append("usage='usage: $0 <tag>'\n");
 		buffer.append("name=eclipse-" + getPackageName(mainPackage.getName())
 				+ "\n");
 		buffer.append("tag=$1\n");
 		buffer.append("tar_name=$name-fetched-src-$tag\n\n");
 		buffer.append("# example of fetch command:\n");
 		buffer
 				.append("# fetch_cmd=cvs -d:pserver:anonymous@dev.eclipse.org:/cvsroot/dsdp \\\n");
 		buffer.append("# export -r $tag org.eclipse.tm.rse/features/$f;\n\n");
 		buffer.append("fetch_cmd=FIXME\n\n");
 		buffer.append("if [ 'x$tag'x = 'xx' ]; then\n");
 		buffer.append("   echo >&2 '$usage'\n");
 		buffer.append("   exit 1\n");
 		buffer.append("fi\n\n");
 		buffer.append("rm -fr $tar_name && mkdir $tar_name\n");
 		buffer.append("pushd $tar_name\n\n");
 		buffer.append("# Fetch plugins\n");
 		buffer.append("for f in \\\n");
 		buffer.append(getProvidesBundlesString(mainPackage.getProvides()));
 		HashSet<String> uniqueProvides = new HashSet<String>();
 		for (SubPackage subPackage : subPackages) {
 			uniqueProvides = getProvidesBundles(subPackage.getProvides(),
 					uniqueProvides);
 		}
 		buffer.append(getProvidesBundlesString(uniqueProvides));
 		buffer.append("; do\n");
 		buffer.append("$fetch_cmd\n");
 		buffer.append("done\n\n");
 		buffer.append("popd\n");
 		buffer.append("# create archive\n");
 		buffer.append("tar -cjf $tar_name.tar.bz2 $tar_name\n");
 		return buffer.toString();
 	}
 
 	/**
 	 * Returns the last meaningful part of the feature id before the feature substring.
 	 * @param packageName The feature id from which to extract the name.
 	 * @return The part of the feature id to be used for package name.
 	 */
 	public String getPackageName(String packageName) {
 		String[] packageItems = packageName.split("\\.");
 		String name = packageItems[packageItems.length - 1];
 		if (name.equalsIgnoreCase("feature")) {
 			name = packageItems[packageItems.length - 2];
 		}
 		return name;
 	}
 
 	/**
 	 * Writes the given contents to a file with the given fileName in the specified project.
 	 * @param projectName The name of the project to put the file into.
 	 * @param fileName The name of the file.
 	 * @param contents The contents of the file.
 	 * @throws CoreException Thrown when the project doesn't exist.
 	 */
 	public void writeContent(String projectName, String fileName,
 			String contents) throws CoreException {
 		InputStream contentInputStream = new ByteArrayInputStream(contents
 				.getBytes());
 		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
 		IResource resource = root.findMember(new Path(projectName));
 		if (!resource.exists() || !(resource instanceof IContainer)) {
 			throwCoreException("Project \"" + projectName
 					+ "\" does not exist.");
 		}
 		IContainer container = (IContainer) resource;
 		final IFile file = container.getFile(new Path(fileName));
 		try {
 			InputStream stream = contentInputStream;
 			if (file.exists()) {
 				file.setContents(stream, true, true, null);
 			} else {
 				file.create(stream, true, null);
 			}
 			stream.close();
 		} catch (IOException e) {
 			StubbyLog.logError(e);
 		}
 		StubbyPlugin.getActiveWorkbenchShell().getDisplay().asyncExec(
 				new Runnable() {
 					public void run() {
 						IWorkbenchPage page = PlatformUI.getWorkbench()
 								.getActiveWorkbenchWindow().getActivePage();
 						try {
 							IDE.openEditor(page, file, true);
 						} catch (PartInitException e) {
 							StubbyLog.logError(e);
 						}
 					}
 				});
 	}
 
 	private void throwCoreException(String message) throws CoreException {
 		IStatus status = new Status(IStatus.ERROR,
 				"org.eclipse.linuxtools.rpm.ui.editor", IStatus.OK, message,
 				null);
 		throw new CoreException(status);
 	}
 
 	private String getDepsOrReqs(String preString,
 			List<PackageItem> packageItems) {
 		String toRet = "";
 		for (PackageItem packageItem : packageItems) {
 			toRet += preString + packageItem.getName() + " "
 					+ packageItem.getOperator() + " "
 					+ packageItem.getVersion() + "\n";
 		}
 		return toRet;
 	}
 
 	private String getProvidesBundlesString(List<PackageItem> packageItems) {
 		String toRet = "";
 		// Cash all the names to handle the case when plugin and feature has the
 		// same id.
 		Set<String> usedNames = new HashSet<String>();
 		for (PackageItem packageItem : packageItems) {
 			if (usedNames.contains(packageItem.getName())) {
 				toRet += packageItem.getName() + "-feature\n";
 			} else {
 				toRet += packageItem.getName() + "\n";
 			}
 			usedNames.add(packageItem.getName());
 		}
 		return toRet;
 	}
 
 	private String getProvidesBundlesString(HashSet<String> uniqueProvides) {
 		String toRet = "";
 		for (String provideName : uniqueProvides) {
 			toRet += provideName + "\n";
 		}
 		return toRet;
 	}
 
 	private HashSet<String> getProvidesBundles(List<PackageItem> packageItems,
 			HashSet<String> uniqueProvides) {
 		for (PackageItem packageItem : packageItems) {
 			uniqueProvides.add(packageItem.getName());
 		}
 		return uniqueProvides;
 	}
 
 	private String getPackageFiles(List<PackageItem> packageItems,
 			boolean withGCJSupport) {
 		String toRet = "";
 		for (PackageItem packageItem : packageItems) {
 			toRet += "%{eclipse_base}/plugins/" + packageItem.getName()
 					+ "_*.jar\n";
 		}
 		if (withGCJSupport) {
 			toRet += "%if %{gcj_support}\n";
 			for (PackageItem packageItem : packageItems) {
 				toRet += "%{_libdir}/gcj/%{name}/" + packageItem.getName()
 						+ "_*\n";
 			}
 			toRet += "%endif\n";
 		}
 		return toRet;
 	}
 }
