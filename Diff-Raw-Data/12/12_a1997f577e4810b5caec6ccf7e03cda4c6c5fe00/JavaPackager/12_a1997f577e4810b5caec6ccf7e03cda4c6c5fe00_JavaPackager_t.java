 /*  Copyright (C) 2009 Mobile Sorcery AB
 
     This program is free software; you can redistribute it and/or modify it
     under the terms of the Eclipse Public License v1.0.
 
     This program is distributed in the hope that it will be useful, but WITHOUT
     ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
     more details.
 
     You should have received a copy of the Eclipse Public License v1.0 along
     with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
  */
 package com.mobilesorcery.sdk.builder.java;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.Map.Entry;
 import java.util.jar.Attributes;
 import java.util.jar.Manifest;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Status;
 
 import com.mobilesorcery.sdk.core.AbstractPackager;
 import com.mobilesorcery.sdk.core.DefaultPackager;
 import com.mobilesorcery.sdk.core.IBuildResult;
 import com.mobilesorcery.sdk.core.IBuildVariant;
 import com.mobilesorcery.sdk.core.MoSyncProject;
 import com.mobilesorcery.sdk.core.MoSyncTool;
 import com.mobilesorcery.sdk.core.Util;
 import com.mobilesorcery.sdk.core.Version;
 import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
 import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
 import com.mobilesorcery.sdk.profiles.IProfile;
 
 public class JavaPackager extends AbstractPackager {
     private String m_zipLoc;
     private String m_iconInjectorLoc;
 
     public JavaPackager() {
         MoSyncTool tool = MoSyncTool.getDefault();
         m_zipLoc = tool.getBinary("zip").toOSString();
         m_iconInjectorLoc = tool.getBinary("icon-injector").toOSString();
 
     }
 
     public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
         DefaultPackager internal = new DefaultPackager(project, variant);
         IProfile targetProfile = variant.getProfile();
        File runtimeDir = internal.resolveFile("%runtime-dir%");
        File compileOut = internal.resolveFile("%compile-output-dir%");
 
         internal.setParameters(getParameters());
         internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
 
         try {
             File packageOutputDir = internal.resolveFile("%package-output-dir%"); //$NON-NLS-1$
             packageOutputDir.mkdirs();
 
             Version appVersion = new Version(internal.getParameters().get(DefaultPackager.APP_VERSION));
             IApplicationPermissions permissions = project.getPermissions();
             
            File projectJar = internal.resolveFile("%package-output-dir%/%app-name%.jar"); //$NON-NLS-1$
            File projectJad = internal.resolveFile("%package-output-dir%/%app-name%.jad"); //$NON-NLS-1$
 
             projectJar.delete();
             projectJad.delete();
 
             String appVendorName = internal.getParameters().get(DefaultPackager.APP_VENDOR_NAME);
             String appName = internal.getParameters().get(DefaultPackager.APP_NAME);
             
            File manifest = internal.resolveFile("%compile-output-dir%/META-INF/MANIFEST.MF"); //$NON-NLS-1$
             createManifest(variant, appName, appVendorName, permissions, appVersion, manifest);
 
             // Need to set execution dir, o/w zip will not understand what we
             // really want.
             internal.getExecutor().setExecutionDirectory(manifest.getParentFile().getParent());
 
             {
                 String runtime = internal.resolve("MoSyncRuntime%D%.jar");
                 Util.copyFile(new NullProgressMonitor(), new File(runtimeDir, runtime), projectJar);
             }
             Util.copyFile(new NullProgressMonitor(), new File(runtimeDir, "config.h"), new File(packageOutputDir, "config.h"));
 
             internal.runCommandLine(m_zipLoc, "-j", "-9", projectJar.getAbsolutePath(), new File(compileOut, "program").getAbsolutePath());
             internal.runCommandLine(m_zipLoc, "-r", "-9", projectJar.getAbsolutePath(), "META-INF");
 
             File resources = new File(compileOut, "resources"); //$NON-NLS-1$
             if (resources.exists()) {
                 internal.runCommandLine(m_zipLoc, "-j", "-9", projectJar.getAbsolutePath(), resources.getAbsolutePath());
             }
 
             createJAD(variant, appName, appVendorName, permissions, appVersion, projectJad, projectJar);
 
             MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
             visitor.setProject(project.getWrappedProject());
             IResource[] iconFiles = visitor.getIconFiles();
             if (iconFiles.length > 0) {
                 Object xObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_X"); //$NON-NLS-1$
                 Object yObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_Y"); //$NON-NLS-1$
                 if (xObj != null && yObj != null) {
                     String sizeStr = ((Long) xObj) + "x" + ((Long) yObj); //$NON-NLS-1$
                     internal.runCommandLine(m_iconInjectorLoc, "-src", iconFiles[0].getLocation().toOSString(), "-size", sizeStr, "-platform", "j2me", "-dst",
                             projectJar.getAbsolutePath());
                 }
             }
 
             signPackage(internal, project, projectJad, projectJar);
 
             buildResult.setBuildResult(projectJar);
         } catch (IOException e) {
             throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.sdk.builder.java", Messages.JavaPackager_PackageError, e)); //$NON-NLS-1$
         }
     }
 
     private void signPackage(DefaultPackager internal, MoSyncProject project, File projectJad, File projectJar) throws IOException {
         List<KeystoreCertificateInfo> keystoreCertInfos = KeystoreCertificateInfo
                 .parseList(project.getProperty(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS));
         // sign jar file using jarSigner
         File mosyncBinDir = internal.resolveFile("%mosync-bin%");
         // File unsignedProjectJar = new File(projectJar.getParentFile(),
         // Util.getNameWithoutExtension(projectJar) + "_unsigned.jar");
 
         for (KeystoreCertificateInfo keystoreCertInfo : keystoreCertInfos) {
             String keystore = keystoreCertInfo.getKeystoreLocation();
             String alias = keystoreCertInfo.getAlias();
             String storepass = keystoreCertInfo.getKeystorePassword();
             if (Util.isEmpty(storepass)) {
                 throw new IllegalArgumentException("Keystore password missing");
             }
 
             String keypass = keystoreCertInfo.getKeyPassword();
             if (Util.isEmpty(keypass)) {
                 throw new IllegalArgumentException("Keystore password missing");
             }
 
             // Util.copyFile(new NullProgressMonitor(), projectJar,
             // unsignedProjectJar);
 
             /*
              * String[] jarSignerCommandLine = new String[] { "java", "-jar",
              * new File( mosyncBinDir, "android/tools-stripped.jar"
              * ).getAbsolutePath( ), "-keystore", keystore, "-storepass",
              * storepass, "-keypass", keypass, "-signedjar",
              * projectJar.getAbsolutePath( ),
              * unsignedProjectJar.getAbsolutePath( ), alias };
              * 
              * internal.runCommandLine(jarSignerCommandLine,
              * "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***");
              */
             String[] addCertCommandLine = new String[] {
                     "java",
                     "-classpath",
                     new File(mosyncBinDir, "javame/JadTool.jar").getAbsolutePath() + File.pathSeparatorChar
                             + new File(mosyncBinDir, "javame/core__environment.jar").getAbsolutePath() + File.pathSeparatorChar
                             + new File(mosyncBinDir, "javame/core__properties.jar").getAbsolutePath(), "com.sun.midp.jadtool.JadTool", "-addcert", "-keystore",
                     keystore, "-alias", alias, "-inputjad", projectJad.getAbsolutePath(), "-outputjad",
                     projectJad.getAbsolutePath() };
 
             assertOk(internal.runCommandLine(addCertCommandLine, "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***"));
 
             String[] addJarSigCommandLine = new String[] {
                     "java",
                     "-classpath",
                     new File(mosyncBinDir, "javame/JadTool.jar").getAbsolutePath() + File.pathSeparatorChar
                             + new File(mosyncBinDir, "javame/core__environment.jar").getAbsolutePath() + File.pathSeparatorChar
                             + new File(mosyncBinDir, "javame/core__properties.jar").getAbsolutePath(), "com.sun.midp.jadtool.JadTool", "-addjarsig",
                     "-keystore", keystore, "-keypass", keypass, "-alias", alias, "-jarfile", projectJar.getAbsolutePath(), "-inputjad",
                     projectJad.getAbsolutePath(), "-outputjad", projectJad.getAbsolutePath() };
 
             assertOk(internal.runCommandLine(addJarSigCommandLine, "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***"));
         }
     }
     
     private void assertOk(int errorCode) {
         if (errorCode != 0) {
             throw new IllegalArgumentException("Tool execution failed");
         }
     }
 
     private void createManifest(IBuildVariant variant, String appName, String vendorName, IApplicationPermissions permissions, Version version, File manifestFile) throws IOException {
         manifestFile.getParentFile().mkdirs();
         Manifest manifest = getManifest(variant, null, appName, vendorName, permissions, version, false);
         FileOutputStream manifestOutput = new FileOutputStream(manifestFile);
         try {
             manifest.write(manifestOutput);
         } finally {
             Util.safeClose(manifestOutput);
         }
     }
 
     private void createJAD(IBuildVariant variant, String projectName, String vendorName, IApplicationPermissions permissions, Version version, File jadFile, File jar) throws IOException {
         Manifest jad = getManifest(variant, jar, projectName, vendorName, permissions, version, true);
         Set<Entry<Object, Object>> entries = jad.getMainAttributes().entrySet();
         FileWriter jadOutput = new FileWriter(jadFile);
         try {
             for (Entry entry : entries) {
                 // JAD format: key COLON SPACE value NEWLINE
                 String key = entry.getKey().toString();
                 String value = entry.getValue().toString();
                 jadOutput.write(key + ": " + value + "\n");
             }
         } finally {
             Util.safeClose(jadOutput);
         }
      }
 
     private Manifest getManifest(IBuildVariant variant, File jar, String appName, String vendorName, IApplicationPermissions permissions, Version version, boolean isJad) throws IOException {
         IProfile profile = variant.getProfile();
         boolean isCLDC_10 = profile != null && Boolean.TRUE.equals(profile.getProperties().get("MA_PROF_SUPPORT_CLDC_10"));
         Manifest result = new Manifest();
         Attributes mainAttr = result.getMainAttributes();
         if (!isJad) {
             mainAttr.putValue("Manifest-Version", "1.0");
         }
         mainAttr.putValue("MIDlet-Vendor", vendorName);
         mainAttr.putValue("MIDlet-Name", appName);
         mainAttr.putValue("MIDlet-1", appName + ", " + appName + ".png" + ", MAMidlet");
         String[] midletPermissions = getMIDletPermissions(permissions, false);
         String[] midletOptPermissions = getMIDletPermissions(permissions, true);
         if (midletPermissions.length > 0) {
             mainAttr.putValue("MIDlet-Permissions", Util.join(midletPermissions, ", "));
         }
         if (midletOptPermissions.length > 0) {
             mainAttr.putValue("MIDlet-Permissions-Opt", Util.join(midletOptPermissions, ", "));
         }
         mainAttr.putValue("MIDlet-Version", version.asCanonicalString(Version.MICRO));
         mainAttr.putValue("MicroEdition-Configuration", isCLDC_10 ? "CLDC-1.0" : "CLDC-1.1");
         mainAttr.putValue("MicroEdition-Profile", "MIDP-2.0");
         if (isJad) {
             long jarSize = jar.length();
             String jarName = jar.getName();
             mainAttr.putValue("MIDlet-Jar-Size", Long.toString(jarSize));
             mainAttr.putValue("MIDlet-Jar-URL", jarName);
         }
         
         return result;
     }
 
     private String[] getMIDletPermissions(IApplicationPermissions permissions, boolean isOptional) {
         ArrayList<String> result = new ArrayList<String>();
         TreeSet<String> reqPermissions = new TreeSet<String>();
         TreeSet<String> optPermissions = new TreeSet<String>();
         MIDletPermissions.toMIDletPermissions(permissions, reqPermissions, optPermissions);
         result.addAll(isOptional ? optPermissions : reqPermissions);
         return result.toArray(new String[result.size()]);
     }
 
 }
