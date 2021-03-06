 /*
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  *
  * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
  *
  * The contents of this file are subject to the terms of either the GNU
  * General Public License Version 2 only ("GPL") or the Common Development
  * and Distribution License("CDDL") (collectively, the "License").  You
  * may not use this file except in compliance with the License. You can obtain
  * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
  * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
  * language governing permissions and limitations under the License.
  *
  * When distributing the software, include this License Header Notice in each
  * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
  * Sun designates this particular file as subject to the "Classpath" exception
  * as provided by Sun in the GPL Version 2 section of the License file that
  * accompanied this code.  If applicable, add the following below the License
  * Header, with the fields enclosed by brackets [] replaced by your own
  * identifying information: "Portions Copyrighted [year]
  * [name of copyright owner]"
  *
  * Contributor(s):
  *
  * If you wish your version of this file to be governed by only the CDDL or
  * only the GPL Version 2, indicate your decision by adding "[Contributor]
  * elects to include this software in this distribution under the [CDDL or GPL
  * Version 2] license."  If you don't indicate a single choice of license, a
  * recipient has the option to distribute your version of this file under
  * either the CDDL, the GPL Version 2 or to extend the choice of license to
  * its licensees as provided above.  However, if you add GPL Version 2 code
  * and therefore, elected the GPL Version 2 license, then the option applies
  * only if the new code is made subject to such option by the copyright
  * holder.
  */
 
 
 package com.sun.enterprise.tools.verifier.hk2;
 
 import com.sun.enterprise.module.ModuleDefinition;
 import com.sun.enterprise.module.ModuleDependency;
 import com.sun.enterprise.module.Repository;
 import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFile;
 import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoader;
 import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoaderFactory;
 import com.sun.enterprise.tools.verifier.apiscan.classfile.Util;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.text.Collator;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.jar.JarEntry;
 import java.util.jar.JarFile;
 
 import org.jvnet.hk2.osgiadapter.OSGiDirectoryBasedRepository;
 
 /**
  * A class that inspects module definitions of a bundle and processes them
  * to come up with package dependency matrix, split-packages, etc.
  * Does not handle version information, so it assumes a package or a bundle
  * has only one version at any given time in a distribution.
  *
  * @author Sanjeeb.Sahoo@Sun.COM
  */
 public class PackageAnalyser {
     private Set<Bundle> bundles;
     private Logger logger;
 
     /**
      * A dats structure to capture bundle details needed for our
      * processing.
      */
     public static class Bundle {
         ModuleDefinition md;
         /**
          * Packages exported by this bundle
          */
         Set<String> exportedPkgs = new HashSet<String>();
 
         /**
          * Packages needed by this bundle. This is NOT necessarily same as
          * what is mentioned in Import-Package header. We introspect the classes
          * of  a bundle and generate this list.
          */
         Set<String> requiredPkgs = new HashSet<String>();
 
         /**
          * List of all the required bundles.
          */
         Set<Bundle> requiredBundles = new HashSet<Bundle>();
 
         Bundle(ModuleDefinition md) {
             this.md = md;
         }
 
         @Override
         public int hashCode() {
             return md.hashCode();
         }
 
         @Override
         public boolean equals(Object obj) {
             if (obj instanceof Bundle) {
                 return this.md.equals(Bundle.class.cast(obj).md);
             }
             return false;
         }
 
         public String getName() {
             return md.getName();
         }
     }
 
     /**
      * A wire represents a connection between an exporter bundle to an importer
      * bundle for a particular package.
      */
     public static class Wire {
         /**
          * Package that is being wired
          */
         String pkg;
         /**
          * The bundle that exports this package
          */
         Bundle exporter;
 
         /**
          * The bundle that imports this package
          */
         Bundle importer;
 
         public Wire(String pkg, Bundle importer, Bundle exporter) {
             this.exporter = exporter;
             this.importer = importer;
             this.pkg = pkg;
         }
 
 
         public String getPkg() {
             return pkg;
         }
 
         public Bundle getExporter() {
             return exporter;
         }
 
         public Bundle getImporter() {
             return importer;
         }
 
         @Override
         public int hashCode() {
             return pkg.hashCode();
         }
 
         @Override
         public boolean equals(Object obj) {
             boolean b = false;
             if (obj instanceof Wire) {
                 Wire other = Wire.class.cast(obj);
                 b = pkg.equals(other.pkg);
                 if (b) {
                     if (exporter != null) {
                         b = exporter.equals(other.exporter);
                     } else {
                         b = other.exporter == null;
                     }
                     if (b) {
                         if (importer != null) {
                             b = importer.equals(other.importer);
                         } else {
                             b = other.importer == null;
                         }
                     }
                 }
             }
             return b;
         }
 
         @Override
         public String toString() {
             return "Wire [Package = " + pkg + ", Importer = " + importer.md.getName() + ", Exporter = " + exporter.md.getName() + "]";
         }
     }
 
     /**
      * Holds information about a split-package.
      * A split-package is a package whose contents come from multiple bundles.
      * Note that, a package can be exported by multiple bundles. It leads to
      * problematic scenarios when they symmetric difference of set of classes
      * for a package from the two bundles is not an empty set. In other words,
      * they should contain identical set of classes from the same package.
      */
     public static class SplitPackage {
         /*
          * TODO(Sahoo): Report packages that are really split. Currently,
          * it reports a package as split if it is exported by multiple bundles.
          */
 
         /**
          * name of package
          */
         String name;
 
         /**
          * Bundles exporting this package
          */
         Set<Bundle> exporters = new HashSet<Bundle>();
 
         public SplitPackage(String name, Set<Bundle> exporters) {
             this.name = name;
             this.exporters = exporters;
         }
 
         @Override
         public int hashCode() {
             return name.hashCode();
         }
 
         @Override
         public boolean equals(Object obj) {
             if (obj instanceof SplitPackage) {
                 return name.equals(SplitPackage.class.cast(obj));
             }
             return false;
         }
 
         @Override
         public String toString() {
             StringBuilder sb = new StringBuilder("name " + name + " (" + exporters.size() + " times):\n");
             for (Bundle b : exporters) {
                 sb.append(b.md.getName() + "\n");
             }
             return sb.toString();
         }
     }
 
     private Repository moduleRepository;
 
     public PackageAnalyser(Repository moduleRepository) {
         this(moduleRepository, Logger.getAnonymousLogger());
     }
 
     public PackageAnalyser(Repository repo, Logger logger) {
         this.moduleRepository = repo;
         this.logger = logger;
     }
 
     /**
      * Analyse the dependency of a bundle and updates it in the given bundle object.
      *
      * @param bundle to be analysed
      */
     public void analyse(Bundle bundle) throws IOException {
         bundle.requiredBundles = computeRequiredBundles(bundle);
         bundle.exportedPkgs = computeExportedPackages(bundle);
         bundle.requiredPkgs = computeRequiredPackages(bundle);
     }
 
     private Set<String> computeRequiredPackages(Bundle bundle) throws IOException {
         Set<String> requiredPkgs = new HashSet<String>();
         File moduleFile = new File(bundle.md.getLocations()[0]);
         String classpath = moduleFile.getAbsolutePath();
         JarFile moduleJar = new JarFile(moduleFile);
         ClassFileLoader cfl = ClassFileLoaderFactory.newInstance(new Object[]{classpath});
         final String classExt = ".class";
         for (Enumeration<JarEntry> entries = moduleJar.entries(); entries.hasMoreElements();) {
             JarEntry je = entries.nextElement();
             if (je.getName().endsWith(classExt)) {
                 String className = Util.convertToExternalClassName(je.getName().substring(0, je.getName().length() - classExt.length()));
                 ClassFile cf = null;
                 try {
                     cf = cfl.load(className);
                     for (String c : cf.getAllReferencedClassNames()) {
                         requiredPkgs.add(Util.getPackageName(c));
                     }
                 } catch (IOException e) {
                     logger.logp(Level.FINE, "PackageAnalyser", "computeRequiredPackages", "Skipping analysis of {0} as the following exception was thrown:\n {1}", new Object[]{className, e});
                 }
             }
         }
         return requiredPkgs;
     }
 
     private Set<String> computeExportedPackages(Bundle bundle) {
         Set<String> exportedPkgs = new HashSet<String>();
         String exportedPkgsAttr = bundle.md.getManifest().getMainAttributes().getValue("Export-Package");
         if (exportedPkgsAttr == null) return exportedPkgs;
         // The string looks like
         // Export-Package: p1;p2;version=1.4;uses:="q1,q2...,qn",p3;uses:="q1,q2";p4;p5;version=...
 
         // First remove the regions that appear between a pair of quotes (""), as that
         // can confuse the tokenizer.
         // Then, tokenize using comma(,) as that separates one group of packages from another.
         // Next, tokenize based on semicolon(;), but except that include everything upto the first
         // token that contained '='.
         while (true) {
             int i1 = exportedPkgsAttr.indexOf('\"');
             if (i1 == -1) break;
             int i2 = exportedPkgsAttr.indexOf('\"', i1 + 1);
             StringBuilder sb = new StringBuilder();
             sb.append(exportedPkgsAttr.substring(0, i1));
             sb.append(exportedPkgsAttr.substring(i2 + 1));
             exportedPkgsAttr = sb.toString();
         }
         StringTokenizer st = new StringTokenizer(exportedPkgsAttr, ",", false);
         while (st.hasMoreTokens()) {
             String pkgGroups = st.nextToken().trim();
             int idx = pkgGroups.indexOf(';');
             StringTokenizer st2 = new StringTokenizer(pkgGroups, ";", false);
             while (st2.hasMoreTokens()) {
                 String pkg = st2.nextToken();
                 if (pkg.indexOf('=') != -1) break;
                 exportedPkgs.add(pkg);
             }
         }
         return exportedPkgs;
     }
 
     private Set<Bundle> computeRequiredBundles(Bundle bundle) {
         Set<Bundle> requiredBundles = new HashSet<Bundle>();
         for (ModuleDependency dep : bundle.md.getDependencies()) {
             ModuleDefinition md = moduleRepository.find(dep.getName(), dep.getVersion());
             if (md != null) {
                 requiredBundles.add(new Bundle(md));
             } else {
                 System.out.println("WARNING: Missing dependency: [" + dep + "] for module [" + bundle.getName() + "]");
             }
         }
         return requiredBundles;
     }
 
     public Collection<Wire> analyseWirings() throws IOException {
         List<ModuleDefinition> moduleDefs =
                 moduleDefs = moduleRepository.findAll();
         bundles = new HashSet<Bundle>();
         for (ModuleDefinition moduleDef : moduleDefs) {
             Bundle bundle = new Bundle(moduleDef);
             bundles.add(bundle);
             analyse(bundle);
         }
         Set<Wire> wires = new HashSet<Wire>();
         for (Bundle importer : bundles) {
             for (String pkg : importer.requiredPkgs) {
                 for (Bundle exporter : bundles) {
                     if (exporter.exportedPkgs.contains(pkg)) {
                         Wire w = new Wire(pkg, importer, exporter);
                         wires.add(w);
                     }
                 }
             }
         }
         List<Wire> sorted = new ArrayList<Wire>(wires);
         Collections.sort(sorted, new Comparator<Wire>() {
             Collator collator = Collator.getInstance();
 
             public int compare(Wire o1, Wire o2) {
                 return collator.compare(o1.pkg, o2.pkg);
             }
         });
         return sorted;
     }
 
     /**
      * Inspects bundles and reports spli-packages.
      * Before calling this method, you must call {@link this#analyseWirings()}
      * The colection is already sorted.
      *
      * @return set of split-packages, en empty set if none is found.
      */
     public Collection<SplitPackage> findSplitPackages() {
         assert (bundles != null);
         Map<String, Set<Bundle>> packages = new HashMap<String, Set<Bundle>>();
         for (Bundle b : bundles) {
             for (String p : b.exportedPkgs) {
                 Set<Bundle> exporters = packages.get(p);
                 if (exporters == null) {
                     exporters = new HashSet<Bundle>();
                     packages.put(p, exporters);
                 }
                 exporters.add(b);
             }
         }
         Set<SplitPackage> splitPkgs = new HashSet<SplitPackage>();
         for (Map.Entry<String, Set<Bundle>> entry : packages.entrySet()) {
             if (entry.getValue().size() > 1) {
                 splitPkgs.add(new SplitPackage(entry.getKey(), entry.getValue()));
             }
         }
         List<SplitPackage> sortedSplitPkgs = new ArrayList<SplitPackage>(splitPkgs);
         Collections.sort(sortedSplitPkgs, new Comparator<SplitPackage>() {
             Collator collator = Collator.getInstance();
 
             public int compare(SplitPackage o1, SplitPackage o2) {
                 return collator.compare(o1.name, o2.name);
             }
         });
 
         return sortedSplitPkgs;
     }
 
     public Collection<String> findAllExportedPackages() {
         Set<String> packages = new HashSet<String>();
         for (Bundle b : bundles) {
             packages.addAll(b.exportedPkgs);
         }
         List<String> sorted = new ArrayList<String>(packages);
         Collections.sort(sorted, new Comparator<String>() {
             Collator collator = Collator.getInstance();
 
             public int compare(String o1, String o2) {
                 return collator.compare(o1, o2);
             }
         });
         return sorted;
     }
 
     public Set<Bundle> findAllBundles() {
         return bundles;
     }
 
     public void generateWiringReport(Collection<String> exportedPkgs, Collection<PackageAnalyser.Wire> wires, PrintStream out) {
         out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
         out.println("<?xml-stylesheet type=\"text/xsl\" href=\"wires.xsl\"?>");
         out.println("<Wires>");
         for (String p : exportedPkgs) {
             StringBuilder sb = new StringBuilder();
             sb.append("\t<Package name = \"" + p + "\">\n");
             sb.append("\t\t<Exporters>\n");
             Set<String> exporters = new HashSet<String>();
             for (PackageAnalyser.Wire w : wires) {
                 if (w.getPkg().equals(p)) {
                     exporters.add(w.getExporter().getName());
                 }
             }
             for (String e : exporters) {
                 sb.append(e + " ");
             }
             sb.append("\n\t\t</Exporters>\n");
             sb.append("\t\t<Importers>\n");
             for (PackageAnalyser.Wire w : wires) {
                 if (w.getPkg().equals(p)) {
                     sb.append(w.getImporter().getName() + " ");
                 }
             }
             sb.append("\n\t\t</Importers>\n");
             sb.append("\t</Package>");
             out.println(sb);
         }
         out.println("</Wires>");
     }
 
     public void generateBundleReport(PrintStream out) {
         out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
         out.println("<?xml-stylesheet type=\"text/xsl\" href=\"bundles.xsl\"?>");
         out.println("<Bundles>");
         for (Bundle b : bundles) {
             StringBuilder sb = new StringBuilder();
             sb.append("\t<Bundle name = \"" + b.getName() + "\">\n");
             sb.append("\t\t<Exports>\n");
             List<String> sorted = new ArrayList<String>(b.exportedPkgs);
             Collections.sort(sorted, new Comparator<String>() {
                 Collator collator = Collator.getInstance();
 
                 public int compare(String o1, String o2) {
                     return collator.compare(o1, o2);
                 }
             });
             int i = 0;
             for (String p : sorted) {
                 sb.append("\t\t\t" + p);
                 if (++i < sorted.size()) {
                     sb.append(",\\");
                 }
                 sb.append("\n");
             }
             sb.append("\t\t</Exports>\n");
             sb.append("\t</Bundle>");
             out.println(sb);
         }
         out.println("</Bundles>");
     }
 
     public static void main(String[] args) throws Exception {
         if (args.length != 4) {
             System.out.println("Usage: java " + PackageAnalyser.class.getName() +
                     " <Repository Dir Path> <output file name for bundle details>" +
                     " <output file name for wiring details> <output file name for split-packages>");
 
             System.out.println("Example(s):\n" +
                     "Following command analyses all modules in the specified repository:\n" +
                     " java " + PackageAnalyser.class.getName() +
                     " /tmp/glassfish/modules/ bundles.xml wires.xml sp.txt\n\n");
             return;
         }
         String repoPath = args[0];
         PrintStream bundleOut = new PrintStream(new FileOutputStream(args[1]));
         PrintStream wireOut = new PrintStream(new FileOutputStream(args[2]));
         PrintStream spOut = new PrintStream(new FileOutputStream(args[3]));
         File f = new File(repoPath) {
             @Override
             public File[] listFiles() {
                 List<File> files = new ArrayList<File>();
                 for (File f : super.listFiles()) {
                     if (f.isDirectory()) {
                         for (File f2 : f.listFiles()) {
                             if (f2.isFile() && f2.getName().endsWith(".jar")) {
                                 files.add(f2);
                             }
                         }
                     } else if (f.isFile() && f.getName().endsWith(".jar")) {
                         files.add(f);
                     }
                 }
                 return files.toArray(new File[files.size()]);
             }
         };
         Repository moduleRepository = new OSGiDirectoryBasedRepository("repo", f);
         moduleRepository.initialize();
 
         PackageAnalyser analyser = new PackageAnalyser(moduleRepository);
         Collection<Wire> wires = analyser.analyseWirings();
         Collection<String> exportedPkgs = analyser.findAllExportedPackages();
         analyser.generateBundleReport(bundleOut);
         analyser.generateWiringReport(exportedPkgs, wires, wireOut);
         Collection<SplitPackage> splitPkgs = analyser.findSplitPackages();
 
         for (SplitPackage p : splitPkgs) spOut.println(p + "\n");
         spOut.println("Total number of Split Packages = " + splitPkgs.size());
 
         System.out.println("******** GROSS STATISTICS *********");
         System.out.println("Total number of bundles in this repository: " + analyser.findAllBundles().size());
         System.out.println("Total number of wires = " + wires.size());
         System.out.println("Total number of exported packages = " + exportedPkgs.size());
         System.out.println("Total number of split-packages = " + splitPkgs.size());
     }
 
 }
