 /*******************************************************************************
  * Copyright (c) 2010 Per Kr. Soreide.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Per Kr. Soreide - initial API and implementation
  *******************************************************************************/
 package bndtools.release;
 
 import java.io.BufferedInputStream;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.UnsupportedEncodingException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Properties;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.PlatformUI;
 
 import aQute.bnd.build.Project;
 import aQute.bnd.build.model.BndEditModel;
 import aQute.bnd.differ.Baseline;
 import aQute.bnd.differ.Baseline.Info;
 import aQute.bnd.osgi.Builder;
 import aQute.bnd.osgi.Constants;
 import aQute.bnd.osgi.Jar;
 import aQute.bnd.osgi.JarResource;
 import aQute.bnd.properties.Document;
 import aQute.bnd.service.RepositoryPlugin;
 import aQute.bnd.version.Version;
 import aQute.service.reporter.Reporter;
 import bndtools.release.api.IReleaseParticipant;
 import bndtools.release.api.IReleaseParticipant.Scope;
 import bndtools.release.api.ReleaseContext;
 import bndtools.release.api.ReleaseContext.Error;
 import bndtools.release.api.ReleaseUtils;
 import bndtools.release.nl.Messages;
 
 public class ReleaseHelper {
 
    public final static String  VERSION_WITH_MACRO_STRING  = "(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\$\\{[-_\\.\\da-zA-Z]+\\})";//$NON-NLS-1$
     public final static Pattern VERSION_WITH_MACRO         = Pattern.compile(VERSION_WITH_MACRO_STRING);
 
 	public static void updateProject(ReleaseContext context) throws Exception {
 
 		Collection<? extends Builder> builders = context.getProject().getBuilder(null).getSubBuilders();
 		for (Builder builder : builders) {
 
 			Baseline current = getBaselineForBuilder(builder, context);
 			if (current == null) {
 				continue;
 			}
 			for (Info info : current.getPackageInfos()) {
 			    context.getProject().setPackageInfo(info.packageName, info.suggestedVersion);
 			}
 
 			updateBundleVersion(context, current, builder);
    		}
 	}
 
 	private static void updateBundleVersion(ReleaseContext context, Baseline current, Builder builder) throws IOException, CoreException {
 
 		Version bundleVersion = current.getSuggestedVersion();
 		if (bundleVersion != null) {
 
 			File file = builder.getPropertiesFile();
 			Properties properties = builder.getProperties();
 			if (file == null) {
 				file = context.getProject().getPropertiesFile();
 				properties = context.getProject().getProperties();
 			}
 			final IFile resource = (IFile) ReleaseUtils.toResource(file);
 
 			final Document document;
 			if (resource.exists()) {
 				byte[] bytes = readFully(resource.getContents());
 				document = new Document(new String(bytes, resource.getCharset()));
 			} else {
 				document = new Document(""); //$NON-NLS-1$
 			}
 
 			final BndEditModel model = new BndEditModel();
 			model.loadFrom(document);
 
 			String currentVersion = model.getBundleVersionString();
 			String templateVersion = updateTemplateVersion(currentVersion, bundleVersion);
 			model.setBundleVersion(templateVersion);
 			properties.setProperty(Constants.BUNDLE_VERSION, templateVersion);
 
 			final Document finalDoc = document;
 			Runnable run = new Runnable() {
 				public void run() {
 					model.saveChangesTo(finalDoc);
 
 					try {
 						writeFully(finalDoc.get(), resource, false);
 						resource.refreshLocal(IResource.DEPTH_ZERO, null);
 					} catch (CoreException e) {
 						throw new RuntimeException(e);
 					}
 				}
 			};
 	        if (Display.getCurrent() == null) {
 	            Display.getDefault().syncExec(run);
 	        } else
 	            run.run();
 		}
 	}
 
 	private static Baseline getBaselineForBuilder(Builder builder, ReleaseContext context) {
 	    Baseline current = null;
 		for (Baseline jd : context.getBaselines()) {
 			if (jd.getBsn().equals(builder.getBsn())) {
 				current = jd;
 				break;
 			}
 		}
 		return current;
 	}
 
 	public static boolean release(ReleaseContext context, List<Baseline> diffs) throws Exception {
 
 		boolean ret = true;
 
 		List<IReleaseParticipant> participants = Activator.getReleaseParticipants();
 
 		if (!preUpdateProjectVersions(context, participants)) {
 			postRelease(context, participants, false);
 			displayErrors(context);
 			return false;
 		}
 
 		ReleaseHelper.updateProject(context);
 
 		IProject proj = ReleaseUtils.getProject(context.getProject());
 		proj.refreshLocal(IResource.DEPTH_INFINITE, context.getProgressMonitor());
 
 		if (context.isUpdateOnly()) {
 			return true;
 		}
 
 		if (!preRelease(context, participants)) {
 			postRelease(context, participants, false);
 			displayErrors(context);
 			return false;
 		}
 
 		for (Baseline diff : diffs) {
 			Collection<? extends Builder> builders = context.getProject().getBuilder(null).getSubBuilders();
 			Builder builder = null;
 			for (Builder b : builders) {
 				if (b.getBsn().equals(diff.getBsn())) {
 					builder = b;
 					break;
 				}
 			}
 			if (builder != null) {
 				if (!release(context, participants, builder)) {
 					ret = false;
 				}
 			}
 		}
 
 		postRelease(context, participants, ret);
 		return ret;
 	}
 
 	private static void handleBuildErrors(ReleaseContext context, Reporter reporter, Jar jar) {
 		String symbName = null;
 		String version = null;
 		if (jar != null) {
 			symbName = ReleaseUtils.getBundleSymbolicName(jar);
 			version = ReleaseUtils.getBundleVersion(jar);
 		}
 		for (String message : reporter.getErrors()) {
 			context.getErrorHandler().error(symbName, version, message);
 		}
 	}
 
 	private static void handleReleaseErrors(ReleaseContext context, Reporter reporter, String symbolicName, String version) {
 		for (String message : reporter.getErrors()) {
 			context.getErrorHandler().error(symbolicName, version, message);
 		}
 	}
 
 	private static void displayErrors(ReleaseContext context) {
 
 		final String name = context.getProject().getName();
 		final List<Error> errors = context.getErrorHandler().getErrors();
 		if (errors.size() > 0) {
 			Runnable runnable = new Runnable() {
 				public void run() {
 					Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
 					ErrorDialog error = new ErrorDialog(shell, name, errors);
 					error.open();
 				}
 			};
 
 			if (Display.getCurrent() == null) {
 				Display.getDefault().asyncExec(runnable);
 			} else {
 				runnable.run();
 			}
 
 		}
 
 	}
 
 	private static boolean release(ReleaseContext context, List<IReleaseParticipant> participants, Builder builder) throws Exception {
 
 		Jar jar = builder.build();
 
 		handleBuildErrors(context, builder, jar);
 
 		String symbName = ReleaseUtils.getBundleSymbolicName(jar);
 		String version = ReleaseUtils.getBundleVersion(jar);
 
 		boolean proceed = preJarRelease(context, participants, jar);
 		if (!proceed) {
 			postRelease(context, participants, false);
 			displayErrors(context);
 			return false;
 		}
 
 		JarResource jr = new JarResource(jar);
 		InputStream is = new BufferedInputStream(jr.openInputStream());
 		try {
 		    context.getProject().release(context.getReleaseRepository().getName(), jar.getName(), is);
 		} finally {
 		    is.close();
 		}
 
 		File file = context.getReleaseRepository().get(symbName, Version.parseVersion(version), null);
 		Jar releasedJar = null;
 		if (file != null && file.exists()) {
 			IResource resource = ReleaseUtils.toResource(file);
 			if (resource != null) {
 				resource.refreshLocal(IResource.DEPTH_ZERO, null);
 			}
 			releasedJar = jar;
 		}
 		if (releasedJar == null) {
 			handleReleaseErrors(context, context.getProject(), symbName, version);
 
 			postRelease(context, participants, false);
 			displayErrors(context);
 			return false;
 		}
 		context.addReleasedJar(releasedJar);
 
 		postJarRelease(context, participants, releasedJar);
 		return true;
 	}
 
 	private static boolean preUpdateProjectVersions(ReleaseContext context, List<IReleaseParticipant> participants) {
 		context.setCurrentScope(Scope.PRE_UPDATE_VERSIONS);
 		for (IReleaseParticipant participant : participants) {
 			if (!participant.preUpdateProjectVersions(context)) {
 				return false;
 			}
 		}
 		return true;
 	}
 
 	private static boolean preRelease(ReleaseContext context, List<IReleaseParticipant> participants) {
 		context.setCurrentScope(Scope.PRE_RELEASE);
 		for (IReleaseParticipant participant : participants) {
 			if (!participant.preRelease(context)) {
 				return false;
 			}
 		}
 		return true;
 	}
 
 	private static boolean preJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
 		context.setCurrentScope(Scope.PRE_JAR_RELEASE);
 		for (IReleaseParticipant participant : participants) {
 			if (!participant.preJarRelease(context, jar)) {
 				return false;
 			}
 		}
 		return true;
 	}
 
 	private static void postJarRelease(ReleaseContext context, List<IReleaseParticipant> participants, Jar jar) {
 		context.setCurrentScope(Scope.POST_JAR_RELEASE);
 		for (IReleaseParticipant participant : participants) {
 			participant.postJarRelease(context, jar);
 		}
 	}
 
 	private static void postRelease(ReleaseContext context, List<IReleaseParticipant> participants, boolean success) {
 		context.setCurrentScope(Scope.POST_RELEASE);
 		for (IReleaseParticipant participant : participants) {
 			participant.postRelease(context, success);
 		}
 	}
 	public static String[] getReleaseRepositories() {
 		List<RepositoryPlugin> repos = Activator.getRepositories();
 		List<String> ret = new ArrayList<String>();
 		for (RepositoryPlugin repo : repos) {
 			if (repo.canWrite()) {
 				if (repo.getName() != null) {
 					ret.add(repo.getName());
 				} else {
 					ret.add(repo.toString());
 				}
 			}
 		}
 		Collections.reverse(ret);
 		return ret.toArray(new String[ret.size()]);
 	}
 
 
 	public static void initializeProjectDiffs(List<ProjectDiff> projects) {
 		String[] repos =  getReleaseRepositories();
 		for (ProjectDiff projectDiff : projects) {
 			String repo = projectDiff.getProject().getProperty(Project.RELEASEREPO);
 			if (repo == null) {
 				if (repos.length > 0) {
 					repo = repos[0];
 				} else {
 					repo = "";
 				}
 			}
 			projectDiff.setReleaseRepository(repo);
 			projectDiff.setDefaultReleaseRepository(repo);
 		}
 	}
 
     private static byte[] readFully(InputStream stream) throws IOException {
         try {
             ByteArrayOutputStream output = new ByteArrayOutputStream();
 
             final byte[] buffer = new byte[1024];
             while (true) {
                 int read = stream.read(buffer, 0, 1024);
                 if (read == -1)
                     break;
                 output.write(buffer, 0, read);
             }
             return output.toByteArray();
         } finally {
             stream.close();
         }
     }
 
     public static void writeFully(String text, IFile file, boolean createIfAbsent) throws CoreException {
         ByteArrayInputStream inputStream;
         try {
             inputStream = new ByteArrayInputStream(text.getBytes(file.getCharset(true)));
         } catch (UnsupportedEncodingException e) {
             return;
         }
         if (file.exists()) {
             file.setContents(inputStream, false, true, null);
         } else {
             if (createIfAbsent)
                 file.create(inputStream, false, null);
             else
                 throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, String.format(Messages.fileDoesNotExist, file.getFullPath().toString()), null));
         }
     }
 
     private static String updateTemplateVersion(String currentVersion, Version newVersion) {
         String version = newVersion.toString();
         if (currentVersion == null) {
             return version;
         }
 
         Matcher m = VERSION_WITH_MACRO.matcher(currentVersion);
         if (m.matches()) {
             return newVersion.getMajor() + "." + newVersion.getMinor() + "." + newVersion.getMicro() + "." + m.group(6);
         }
         return version;
     }
 }
