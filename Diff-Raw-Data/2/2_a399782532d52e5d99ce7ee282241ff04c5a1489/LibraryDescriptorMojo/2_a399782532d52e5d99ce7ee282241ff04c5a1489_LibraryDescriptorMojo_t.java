 package org.mobicents.slee.tools.maven.plugins.library;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.maven.model.Plugin;
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.project.MavenProject;
 import org.codehaus.plexus.util.xml.Xpp3Dom;
 import org.mobicents.slee.tools.maven.plugins.library.pojos.LibraryRef;
 
 /**
  * Base class for creating a library jar.
  * 
  * @author <a href="brainslog@gmail.com"> Alexandre Mendonca </a>
  * @author martins
  */
 public class LibraryDescriptorMojo extends AbstractMojo {
 
 	private LibraryRef libraryId;
 
 	private List<LibraryRef> libraryRefs = new ArrayList<LibraryRef>();
 
 	private String libraryRefsInConfig = null;
 	private String eventTypeRefs = null;
 	private String profileSpecRefs = null;
 	private String raTypeRefs = null;
 	private String sbbRefs = null;
 
 	private MavenProject project;
 
 	/**
 	 * Directory to be used as the source for SLEE component jars.
 	 * 
 	 */
 	private File jarInputDirectory;
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.apache.maven.plugin.AbstractMojo#execute()
 	 */
 	public void execute() throws MojoExecutionException {
 		if (getLog().isDebugEnabled()) {
 			getLog().debug("Executing Maven JAIN SLEE 1.1 Library Plugin");
 		}
 
 		// Obtain the name#vendor#version from pom
 		this.libraryId = getLibraryRef(project);
 
 		this.libraryRefsInConfig = getRefs(project, "library");
 		getLog().info("Library Refs Configuration:\n" + libraryRefsInConfig);
 
 		this.eventTypeRefs = getRefs(project, "event-type");
 		getLog().info("EventType Refs:\n" + eventTypeRefs);
 
 		this.profileSpecRefs = getRefs(project, "profile-spec");
 		getLog().info("ProfileSpec Refs:\n" + profileSpecRefs);
 
 		this.raTypeRefs = getRefs(project, "resource-adaptor-type");
 		getLog().info("RA Type Refs:\n" + raTypeRefs);
 
 		this.sbbRefs = getRefs(project, "sbb");
 		getLog().info("SBB Refs:\n" + sbbRefs);
 
 		if (this.libraryId == null) {
 			throw new MojoExecutionException(
 					"Unable to get Library ID from pom, please verify.");
 		} else {
 			if (getLog().isDebugEnabled()) {
 				getLog().info(
 						libraryId.getName() + "#" + libraryId.getVendor() + "#"
 								+ libraryId.getVersion());
 			}
 		}
 
 		// collect jar files
 		Set<String> jars = collectFiles(jarInputDirectory, ".jar");
 
 		generateLibraryDeploymentDescritptor(jars);
 
 	}
 
 	/**
 	 * Generates the deployment descriptor for the library jar, based on the
 	 * library-ref element, library jars and jars present at resources folder.
 	 * 
 	 * @throws MojoExecutionException
 	 */
 	private void generateLibraryDeploymentDescritptor(Set<String> jars)
 			throws MojoExecutionException {
 		File libraryDescriptorDir = new File(project.getBuild()
 				.getOutputDirectory(), "META-INF");
 
 		if (!libraryDescriptorDir.exists()) {
 			libraryDescriptorDir.mkdirs();
 		}
 
 		File libraryDD = new File(libraryDescriptorDir.getAbsolutePath(),
 				"library-jar.xml");
 
 		String xml = null;
 		if (eventTypeRefs != null || profileSpecRefs != null
 				|| raTypeRefs != null || sbbRefs != null) {
 			// extended
 			xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
 					+ "<!DOCTYPE library-jar PUBLIC \r\n"
 					+ "\t\t\"-//Sun Microsystems, Inc.//DTD JAIN SLEE Ext Library 1.1//EN\" \r\n"
 					+ "\t\t\"http://mobicents.org/slee/dtd/slee-library-jar_1_1-ext.dtd\">\r\n\r\n"
 					+ "<library-jar>\r\n\t<library>\r\n";
 		} else {
 			// standard is enough
 			xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
 					+ "<!DOCTYPE library-jar PUBLIC \r\n"
 					+ "\t\"-//Sun Microsystems, Inc.//DTD JAIN SLEE Library 1.1//EN\" \r\n"
 					+ "\t\"http://java.sun.com/dtd/slee-library-jar_1_1.dtd\">\r\n"
 					+ "<library-jar>\r\n" + "\t<library>\r\n";
 		}
 
 		xml += this.libraryId.toXmlEntry();
 
 		if (eventTypeRefs != null) {
 			xml += eventTypeRefs + "\r\n";
 		}
 
 		if (libraryRefsInConfig != null) {
 			xml += libraryRefsInConfig + "\r\n";
 		}
 
 		if (libraryRefs != null && !libraryRefs.isEmpty()) {
 			for (LibraryRef libraryRef : this.libraryRefs) {
 				xml += libraryRef.toXmlEntryWithRef();
 			}
 			xml += "\r\n";
 		}
 
 		if (profileSpecRefs != null) {
 			xml += profileSpecRefs + "\r\n";
 		}
 
 		if (raTypeRefs != null) {
 			xml += raTypeRefs + "\r\n";
 		}
 
 		if (sbbRefs != null) {
 			xml += sbbRefs + "\r\n";
 		}
 
 		for (String jar : jars) {
 			xml += "\t\t<jar>\r\n" + "\t\t\t<jar-name>" + jar
					+ "</jar-name>\r\n" + "\t\t</jar>\r\n";
 		}
 
 		xml += "\t</library>\r\n</library-jar>\r\n";
 
 		getLog().info(
 				"Generated Library descriptor: " + libraryDD.getAbsolutePath()
 						+ "\n" + xml + "\n");
 
 		try {
 			BufferedWriter out = new BufferedWriter(new FileWriter(libraryDD));
 			out.write(xml);
 			out.close();
 		} catch (IOException e) {
 			getLog().error(
 					"Failed to create deployment descriptor in "
 							+ libraryDD.getAbsolutePath(), e);
 		}
 
 	}
 
 	/**
 	 * Obtains the library-name, library-vendor, library-version into a
 	 * LibraryRef element.
 	 * 
 	 * @param mavenProject
 	 *            the maven project to obtain it from
 	 * @return
 	 */
 
 	private LibraryRef getLibraryRef(MavenProject mavenProject) {
 		String libraryName;
 		String libraryVendor;
 		String libraryVersion;
 
 		String libraryDescription = null;
 
 		for (Object pObject : mavenProject.getBuildPlugins()) {
 			Plugin plugin = (Plugin) pObject;
 			if (plugin.getArtifactId().equals("maven-library-plugin")) {
 				Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
 
 				if (configuration != null) {
 					if (configuration.getChildren("library-name").length > 0) {
 						libraryName = configuration.getChildren("library-name")[0]
 								.getValue();
 					} else {
 						getLog().error(
 								"Library Name missing in plugin configuration!");
 						return null;
 					}
 
 					if (configuration.getChildren("library-vendor").length > 0) {
 						libraryVendor = configuration
 								.getChildren("library-vendor")[0].getValue();
 					} else {
 						getLog().error(
 								"Library Vendor missing in plugin configuration!");
 						return null;
 					}
 
 					if (configuration.getChildren("library-version").length > 0) {
 						libraryVersion = configuration
 								.getChildren("library-version")[0].getValue();
 					} else {
 						getLog().error(
 								"Library Version missing in plugin configuration!");
 						return null;
 					}
 
 					if (configuration.getChildren("description").length > 0) {
 						libraryDescription = configuration
 								.getChildren("description")[0].getValue();
 					}
 				} else {
 					getLog().error("Configuration missing in plugin!");
 					return null;
 				}
 
 				return new LibraryRef(libraryName, libraryVendor,
 						libraryVersion, libraryDescription);
 			}
 		}
 
 		return null;
 	}
 
 	private String getRefs(MavenProject mavenProject, String refName) {
 
 		for (Object pObject : mavenProject.getBuildPlugins()) {
 
 			Plugin plugin = (Plugin) pObject;
 
 			if (!plugin.getArtifactId().equals("maven-library-plugin")) {
 				continue;
 			}
 
 			Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
 
 			if (configuration != null) {
 				String result = null;
 				for (Xpp3Dom child : configuration
 						.getChildren(refName + "-ref")) {
 					String childXml = "\t\t<" + refName + "-ref>";
 					try {
 						childXml += "\n\t\t\t<" + refName + "-name>"
 								+ child.getChild(refName + "-name").getValue()
 								+ "</" + refName + "-name>";
 						childXml += "\n\t\t\t<"
 								+ refName
 								+ "-vendor>"
 								+ child.getChild(refName + "-vendor")
 										.getValue() + "</" + refName
 								+ "-vendor>";
 						childXml += "\n\t\t\t<"
 								+ refName
 								+ "-version>"
 								+ child.getChild(refName + "-version")
 										.getValue() + "</" + refName
 								+ "-version>";
 						childXml += "\n\t\t</" + refName + "-ref>";
 					} catch (Exception e) {
 						getLog().error("Failed to load ref of type " + refName,
 								e);
 						throw new RuntimeException(e);
 					}
 					if (result == null) {
 						result = childXml;
 					} else {
 						result += "\r\n" + childXml;
 					}
 				}
 				return result;
 			} else {
 				getLog().info("Configuration missing in plugin!");
 				return null;
 			}
 		}
 
 		return null;
 	}
 
 	private Set<String> collectFiles(File inputDirectory, String suffix) {
 
 		if (getLog().isDebugEnabled()) {
 			getLog().debug(
 					"Collecting non hidden files with " + suffix
 							+ " name sufix from directory "
 							+ inputDirectory.getAbsolutePath());
 		}
 
 		if (inputDirectory == null || !inputDirectory.exists()
 				|| !inputDirectory.isDirectory()) {
 			return Collections.emptySet();
 		}
 
 		if (getLog().isDebugEnabled()) {
 			getLog().debug(
 					"Directory " + inputDirectory.getAbsolutePath()
 							+ " sucessfully validated.");
 		}
 
 		Set<String> result = new HashSet<String>();
 
 		for (File f : inputDirectory.listFiles()) {
 			if (f.isDirectory() || f.isHidden()
 					|| !f.getName().endsWith(suffix)) {
 				continue;
 			} else {
 				if (getLog().isDebugEnabled()) {
 					getLog().debug("Collecting file " + f.getName());
 				}
 				result.add(f.getName());
 			}
 		}
 
 		return result;
 	}
 }
