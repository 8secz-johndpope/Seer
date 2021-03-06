 /*
  * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
  * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
  */
 
 package com.sun.tools.xjc;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.lang.reflect.Array;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.net.MalformedURLException;
 import java.util.ArrayList;
 import java.util.Enumeration;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 
 import com.sun.tools.xjc.api.ClassNameAllocator;
 import com.sun.tools.xjc.reader.Util;
 
 import org.apache.xml.resolver.CatalogManager;
 import org.apache.xml.resolver.tools.CatalogResolver;
 import org.xml.sax.EntityResolver;
 import org.xml.sax.InputSource;
 
 /**
  * Global options.
  * 
  * <p>
  * This class stores invocation configuration for XJC.
  * The configuration in this class shoule be abstract enough so that
  * it could be parsed from both command-line or Ant.
  */
 public class Options
 {
     /** If "-debug" is specified. */
     public boolean debugMode;
     
     /** If the "-verbose" option is specified. */
     public boolean verbose;
     
     /** If the "-quiet" option is specified. */
     public boolean quiet;
 
     /** If the -readOnly option is specified */
     public boolean readOnly;
     
     /**
      * Check the source schemas with extra scrutiny.
      * The exact meaning depends on the schema language.
      */
     public boolean strictCheck =true;
     
     /**
      * strictly follow the compatibility rules and reject schemas that
      * contain features from App. E.2, use vendor binding extensions
      */
     public static final int STRICT = 1;
     /**
      * loosely follow the compatibility rules and allow the use of vendor
      * binding extensions
      */
     public static final int EXTENSION = 2;
     
     /**
      * this switch determines how carefully the compiler will follow
      * the compatibility rules in the spec. Either <code>STRICT</code>
      * or <code>EXTENSION</code>.
      */
     public int compatibilityMode = STRICT;
 
     /** Target direcoty when producing files. */
     public File targetDir = new File(".");
     
     /**
      * Actually stores {@link CatalogResolver}, but the field
      * type is made to {@link EntityResolver} so that XJC can be
      * used even if resolver.jar is not available in the classpath.
      */
     public EntityResolver entityResolver = null;
 
     ;
 
     /**
      * Type of input schema language. One of the <code>SCHEMA_XXX</code>
      * constants.
      */
     private Language schemaLanguage = null;
     
     /**
      * The -p option that should control the default Java package that
      * will contain the generated code. Null if unspecified.
      */
     public String defaultPackage = null;
 
     /**
      * Similar to the -p option, but this one works with a lower priority,
      * and customizations overrides this. Used by JAX-RPC.
      */
     public String defaultPackage2 = null;
 
     /**
      * Input schema files as a list of {@link InputSource}s.
      */
     private final List<InputSource> grammars = new ArrayList<InputSource>();
     
     private final List<InputSource> bindFiles = new ArrayList<InputSource>();
     
     // Proxy setting.
     private String proxyHost = null;
     private String proxyPort = null;
     private String proxyUser = null;
     private String proxyPassword = null;
 
     /**
      * {@link Plugin}s that are enabled in this compilation.
      */
     public final List<Plugin> activePlugins = new ArrayList<Plugin>();
 
     /**
      * All discovered {@link Plugin}s.
      */
     public static final List<Plugin> allPlugins = new ArrayList<Plugin>();
 
     /**
      * Set of URIs that plug-ins recognize as extension bindings.
      */
     public final Set<String> pluginURIs = new HashSet<String>();
 
     /**
      * This allocator has the final say on deciding the class name.
      */
     public ClassNameAllocator classNameAllocator;
 
     /**
      * This switch controls whether or not xjc will generate package level annotations
      */
     public boolean packageLevelAnnotations = true;
 
     static {
         for( Plugin aug : findServices(Plugin.class) )
             allPlugins.add(aug);
     }
 
 
 
     
     public Language getSchemaLanguage() {
         if( schemaLanguage==null)
             schemaLanguage = guessSchemaLanguage();
         return schemaLanguage;
     }
     public void setSchemaLanguage(Language _schemaLanguage) {
         this.schemaLanguage = _schemaLanguage;
     }
     
     /** Input schema files. */
     public InputSource[] getGrammars() {
         return grammars.toArray(new InputSource[grammars.size()]);
     }
     
     /**
      * Adds a new input schema.
      */
     public void addGrammar( InputSource is ) {
         grammars.add(absolutize(is));
     }
 
     public void addGrammar( File source ) {
         try {
             String url = source.toURL().toExternalForm();
             addGrammar(new InputSource(Util.escapeSpace(url)));
         } catch (MalformedURLException e) {
             addGrammar(new InputSource(source.getPath()));
         }
     }
 
     /**
      * Recursively scan directories and add all XSD files in it.
      */
     public void addGrammarRecursive( File dir ) throws MalformedURLException {
         File[] files = dir.listFiles();
         if(files==null)     return; // work defensively
 
         for( File f : files ) {
             if(f.isDirectory())
                 addGrammarRecursive(f);
             else
             if(f.getPath().endsWith(".xsd"))
                 addGrammar(f);
         }
     }
 
 
     private InputSource absolutize(InputSource is) {
         // absolutize all the system IDs in the input,
         // so that we can map system IDs to DOM trees.
         try {
             URL baseURL = new File(".").getCanonicalFile().toURL(); 
             is.setSystemId( new URL(baseURL,is.getSystemId()).toExternalForm() );
         } catch( IOException e ) {
             ; // ignore
         }
         return is;
     }
 
     
     /** Input external binding files. */
     public InputSource[] getBindFiles() {
         return bindFiles.toArray(new InputSource[bindFiles.size()]);
     }
 
     /**
      * Adds a new input schema.
      */
     public void addBindFile( InputSource is ) {
         bindFiles.add(absolutize(is));
     }
     
     public final List<URL> classpaths = new ArrayList<URL>();
     /**
      * Gets a classLoader that can load classes specified via the
      * -classpath option.
      */
     public URLClassLoader getUserClassLoader( ClassLoader parent ) {
         return new URLClassLoader(
                 classpaths.toArray(new URL[classpaths.size()]),parent);
     }
 
     
     /**
      * Parses an option <code>args[i]</code> and return
      * the number of tokens consumed.
      * 
      * @return
      *      0 if the argument is not understood. Returning 0
      *      will let the caller report an error.
      * @exception BadCommandLineException
      *      If the callee wants to provide a custom message for an error.
      */
     protected int parseArgument( String[] args, int i ) throws BadCommandLineException, IOException {
         if (args[i].equals("-classpath") || args[i].equals("-cp")) {
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_CLASSPATH));
             classpaths.add(new File(args[++i]).toURL());
             return 2;
         }
         if (args[i].equals("-d")) {
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_DIR));
             targetDir = new File(args[++i]);
             if( !targetDir.exists() )
                 throw new BadCommandLineException(
                     Messages.format(Messages.NON_EXISTENT_DIR,targetDir));
             return 2;
         }
         if (args[i].equals("-readOnly")) {
             readOnly = true;
             return 1;
         }
         if (args[i].equals("-p")) {
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PACKAGENAME));
             defaultPackage = args[++i];
            if(defaultPackage.length()==0) { // user specified default package
                // there won't be any package to annotate, so disable them
                // automatically as a usability feature
                packageLevelAnnotations = false;
            }
             return 2;
         }
         if (args[i].equals("-debug")) {
             debugMode = true;
             return 1;
         }
         if (args[i].equals("-nv")) {
             strictCheck = false;
             return 1;
         }
         if( args[i].equals("-npa")) {
             packageLevelAnnotations = false;
             return 1;
         }
         if (args[i].equals("-verbose")) {
             verbose = true;
             return 1;
         }
         if (args[i].equals("-quiet")) {
             quiet = true;
             return 1;
         }
         if (args[i].equals("-b")) {
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_FILENAME));
             if (args[i + 1].startsWith("-")) {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_FILENAME));
             }
             addBindFile(Util.getInputSource(args[++i]));
             return 2;
         }
         if (args[i].equals("-dtd")) {
             schemaLanguage = Language.DTD;
             return 1;
         }
         if (args[i].equals("-relaxng")) {
             schemaLanguage = Language.RELAXNG;
             return 1;
         }
         if (args[i].equals("-relaxng-compact")) {
             schemaLanguage = Language.RELAXNG_COMPACT;
             return 1;
         }
         if (args[i].equals("-xmlschema")) {
             schemaLanguage = Language.XMLSCHEMA;
             return 1;
         }
         if (args[i].equals("-wsdl")) {
             schemaLanguage = Language.WSDL;
             return 1;
         }
         if (args[i].equals("-extension")) {
             compatibilityMode = EXTENSION;
             return 1;
         }
         if (args[i].equals("-proxy")) {
             if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PROXY));
             }
             // syntax is [user[:password]@]proxyHost[:proxyPort]
             String token = "([^@:]+)";
             Pattern p = Pattern.compile("(?:"+token+"(?:\\:"+token+")?\\@)?"+token+"(?:\\:"+token+")?");
 
             String text = args[++i];
             Matcher matcher = p.matcher(text);
             if(!matcher.matches())
                 throw new BadCommandLineException(Messages.format(Messages.ILLEGAL_PROXY,text));
 
             proxyUser = matcher.group(1);
             proxyPassword = matcher.group(2);
             proxyHost = matcher.group(3);
             proxyPort = matcher.group(4);
             try {
                 Integer.valueOf(proxyPort);
             } catch (NumberFormatException e) {
                 throw new BadCommandLineException(Messages.format(Messages.ILLEGAL_PROXY,text));
             }
             return 2;
         }
         if (args[i].equals("-host")) {
             // legacy option. we use -proxy for more control
             if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PROXYHOST));
             }
             proxyHost = args[++i];
             return 2;
         }
         if (args[i].equals("-port")) {
             // legacy option. we use -proxy for more control
             if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PROXYPORT));
             }
             proxyPort = args[++i];
             return 2;
         }
         if( args[i].equals("-catalog") ) {
             // use Sun's "XML Entity and URI Resolvers" by Norman Walsh
             // to resolve external entities.
             // http://www.sun.com/xml/developers/resolver/
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_CATALOG));
             
             addCatalog(new File(args[++i]));
             return 2;
         }
         if (args[i].equals("-source")) {
             if (i == args.length - 1)
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_VERSION));
             String version = args[++i];
             //For source 1.0 the 1.0 Driver is loaded
             //Hence anything other than 2.0 is defaulted to
             //2.0
             if( !version.equals("2.0") )
                 throw new BadCommandLineException(
                     Messages.format(Messages.DEFAULT_VERSION));
             return 2;
         }
         if( args[i].equals("-Xtest-class-name-allocator") ) {
             classNameAllocator = new ClassNameAllocator() {
                 public String assignClassName(String packageName, String className) {
                     System.out.printf("assignClassName(%s,%s)\n",packageName,className);
                     return className+"_Type";
                 }
             };
             return 1;
         }
 
         // see if this is one of the extensions
         for( Plugin aug : allPlugins ) {
             if( ('-'+aug.getOptionName()).equals(args[i]) ) {
                 activePlugins.add(aug);
                 pluginURIs.addAll(aug.getCustomizationURIs());
                 return 1;
             }
                     
             int r = aug.parseArgument(this,args,i);
             if(r!=0)    return r;
         }
         
         return 0;   // unrecognized
     }
     
     /**
      * Adds a new catalog file.
      */
     public void addCatalog(File catalogFile) throws IOException {
         if(entityResolver==null) {
             CatalogManager.getStaticManager().setIgnoreMissingProperties(true);
             entityResolver = new CatalogResolver(true);
         }
         ((CatalogResolver)entityResolver).getCatalog().parseCatalog(catalogFile.getPath());
     }
     
     /**
      * Parses arguments and fill fields of this object.
      * 
      * @exception BadCommandLineException
      *      thrown when there's a problem in the command-line arguments
      */
     public void parseArguments( String[] args ) throws BadCommandLineException, IOException {
 
         for (int i = 0; i < args.length; i++) {
             if(args[i].length()==0)
                 throw new BadCommandLineException();
             if (args[i].charAt(0) == '-') {
                 int j = parseArgument(args,i);
                 if(j==0)
                     throw new BadCommandLineException(
                         Messages.format(Messages.UNRECOGNIZED_PARAMETER, args[i]));
                 i += (j-1);
             } else {
                 Object src = Util.getFileOrURL(args[i]);
                 if(src instanceof URL) {
                     addGrammar(new InputSource(Util.escapeSpace(((URL)src).toExternalForm())));
                 } else {
                     File fsrc = (File)src;
                     if(fsrc.isDirectory()) {
                         addGrammarRecursive(fsrc);
                     } else {
                         addGrammar(fsrc);
                     }
                 }
             }
         }
         
         // configure proxy
         if (proxyHost != null || proxyPort != null) {
             if (proxyHost != null && proxyPort != null) {
                 System.setProperty("http.proxyHost", proxyHost);
                 System.setProperty("http.proxyPort", proxyPort);
                 System.setProperty("https.proxyHost", proxyHost);
                 System.setProperty("https.proxyPort", proxyPort);
             } else if (proxyHost == null) {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PROXYHOST));
             } else {
                 throw new BadCommandLineException(
                     Messages.format(Messages.MISSING_PROXYPORT));
             }
             if(proxyUser!=null)
                 System.setProperty("http.proxyUser", proxyUser);
             if(proxyPassword!=null)
                 System.setProperty("http.proxyPassword", proxyPassword);
 
         }
 
         if (grammars.size() == 0)
             throw new BadCommandLineException(
                 Messages.format(Messages.MISSING_GRAMMAR));
         
         if( schemaLanguage==null )
             schemaLanguage = guessSchemaLanguage();
     }
     
     
     /**
      * Guesses the schema language.
      */
     public Language guessSchemaLanguage() {
         // otherwise, use the file extension.
         // not a good solution, but very easy.
         String name = grammars.get(0).getSystemId().toLowerCase();
 
         if (name.endsWith(".rng"))
             return Language.RELAXNG;
         if (name.endsWith(".rnc"))
             return Language.RELAXNG_COMPACT;
         if (name.endsWith(".dtd"))
             return Language.DTD;
         if (name.endsWith(".wsdl"))
             return Language.WSDL;
 
         // by default, assume XML Schema
         return Language.XMLSCHEMA;
     }
 
     
     
     
     
     private static <T> T[] findServices( Class<T> clazz ) {
         return findServices( clazz, Driver.class.getClassLoader() );
     }
     
     /**
      * Looks for all "META-INF/services/[className]" files and
      * create one instance for each class name found inside this file.
      */
     private static <T> T[] findServices( Class<T> clazz, ClassLoader classLoader ) {
         // if true, print debug output
         final boolean debug = com.sun.tools.xjc.util.Util.getSystemProperty(Options.class,"findServices")!=null;
         
         String serviceId = "META-INF/services/" + clazz.getName();
 
         if(debug) {
             System.out.println("Looking for "+serviceId+" for add-ons");
         }
         
         // try to find services in CLASSPATH
         try {
             Enumeration<URL> e = classLoader.getResources(serviceId);
             if(e==null) return (T[])Array.newInstance(clazz,0);
     
             ArrayList<T> a = new ArrayList<T>();
             while(e.hasMoreElements()) {
                 URL url = e.nextElement();
                 BufferedReader reader=null;
                 
                 if(debug) {
                     System.out.println("Checking "+url+" for an add-on");
                 }
                 
                 try {
                     reader = new BufferedReader(new InputStreamReader(url.openStream()));
                     String impl;
                     while((impl = reader.readLine())!=null ) {
                         // try to instanciate the object
                         impl = impl.trim();
                         if(debug) {
                             System.out.println("Attempting to instanciate "+impl);
                         }
                         Class implClass = classLoader.loadClass(impl);
                         a.add((T)implClass.newInstance());
                     }
                     reader.close();
                 } catch( Exception ex ) {
                     // let it go.
                     if(debug) {
                         ex.printStackTrace(System.out);
                     }
                     if( reader!=null ) {
                         try {
                             reader.close();
                         } catch( IOException ex2 ) {
                         }
                     }
                 }
             }
             
             return a.toArray((T[])Array.newInstance(clazz,a.size()));
         } catch( Throwable e ) {
             // ignore any error
             if(debug) {
                 e.printStackTrace(System.out);
             }
             return (T[])Array.newInstance(clazz,0);
         }
     }
 }
