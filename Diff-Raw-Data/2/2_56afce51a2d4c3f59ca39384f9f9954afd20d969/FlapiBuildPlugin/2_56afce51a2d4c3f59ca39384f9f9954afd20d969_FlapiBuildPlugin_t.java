 /*******************************************************************************
  Copyright 2013 Benjamin Fagin
 
      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at
 
          http://www.apache.org/licenses/LICENSE-2.0
 
      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.
 
 
      Read the included LICENSE.TXT for more information.
  ******************************************************************************/
 
 package unquietcode.tools.flapi.plugin;
 
 import org.apache.maven.plugin.AbstractMojo;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.apache.maven.plugins.annotations.LifecyclePhase;
 import org.apache.maven.plugins.annotations.Mojo;
 import org.apache.maven.plugins.annotations.Parameter;
 import org.apache.maven.plugins.annotations.ResolutionScope;
 import org.apache.maven.project.MavenProject;
 import unquietcode.tools.flapi.Descriptor;
 import unquietcode.tools.flapi.ExtractRuntime;
 import unquietcode.tools.flapi.plugin.compile.CharSequenceJavaFileObject;
 import unquietcode.tools.flapi.plugin.compile.ClassFileManager;
 
 import javax.tools.*;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.*;
 
 /**
  * Given a static method which returns a {@link Descriptor} object,
  * generate the classes and write them out to the specified output
  * directory.
  *
  * @author Ben Fagin
  * @version 2013-07-03
  */
 @Mojo(
 	name="generate",
 	defaultPhase=LifecyclePhase.PROCESS_CLASSES,
 	requiresDependencyResolution=ResolutionScope.COMPILE
 )
 public class FlapiBuildPlugin extends AbstractMojo {
 
 	@Parameter(defaultValue="${project}", required=true, readonly=true)
 	private MavenProject project;
 
 	/**
 	 * The class which contains the target method.
 	 */
 	@Parameter(required=true)
 	private String descriptorClass;
 
 	/**
 	 * The name of the method, which should be
 	 * return type 'Descriptor' with no parameters.
 	 */
 	@Parameter(required=true)
 	private String descriptorMethod;
 
 	/**
 	 * The directory to which the generated classes
 	 * will be written.
 	 */
 	@Parameter(defaultValue="${project.build.outputDirectory}")
 	private String classesDirectory;
 
 	/**
 	 * The directory to which the generated sources
 	 * will be written.
 	 */
 	@Parameter(defaultValue="${project.build.directory}/generated-sources")
 	private String sourcesDirectory;
 
 	/**
 	 * If true, the runtime classes will be written
 	 * out alongside the generated classes.
 	 */
 	@Parameter(defaultValue="false")
 	private boolean includeRuntime;
 
 	/**
 	 * If true, the sources will be written.
 	 */
 	@Parameter(defaultValue="true")
 	private boolean writeSources;
 
 	/**
 	 * If true, the sources will be compiled and written.
 	 */
 	@Parameter(defaultValue="true")
 	private boolean writeClasses;
 
 
 	@Override
 	public void execute() throws MojoExecutionException, MojoFailureException {
 		Method method;
 
 		// instantiate the class
 		URLClassLoader classLoader;
 		Class<?> _descriptorClass;
 
 		try {
 			classLoader = getCompiledClassloader();
 			_descriptorClass = classLoader.loadClass(descriptorClass);
 		} catch (Exception ex) {
 			throw new MojoExecutionException("could not load class", ex);
 		}
 
 		// lookup the method
 		try {
 			method = _descriptorClass.getMethod(descriptorMethod);
 		} catch (NoSuchMethodException ex) {
 			throw new MojoExecutionException("method cannot be found", ex);
 		}
 
 		// ensure that it returns a Descriptor
 		if (!Descriptor.class.isAssignableFrom(method.getReturnType())) {
 			throw new MojoExecutionException("method must return a Descriptor object");
 		}
 
 		// execute and get the descriptor
 		Descriptor descriptor;
 		try {
 			descriptor = (Descriptor) method.invoke(descriptorClass);
 		} catch (IllegalAccessException ex) {
 			throw new MojoExecutionException("method not accessible", ex);
 		} catch (InvocationTargetException ex) {
 			throw new MojoExecutionException("error while executing method", ex.getTargetException());
 		}
 
 		// ensure not null
 		if (descriptor == null) {
 			throw new MojoExecutionException("method returned null");
 		}
 
 		// compile and write out the classes
 		if (writeClasses) {
 			compileAndWriteClasses(descriptor, classLoader);
 
 			if (includeRuntime) {
 				ExtractRuntime.writeRequiredClasses(sourcesDirectory);
 			}
 		}
 
 		// write out the source files
 		if (writeSources) {
 			new File(sourcesDirectory).mkdirs();
 			descriptor.writeToFolder(sourcesDirectory);
 
 			if (includeRuntime) {
 				ExtractRuntime.writeRequiredSources(sourcesDirectory);
 			}
 		}
 	}
 
 	private URLClassLoader getCompiledClassloader() throws Exception {
 		List<URL> urls = new ArrayList<URL>();
 
 		for (Object object : project.getCompileClasspathElements()) {
 			String path = (String) object;
 			System.out.println(path);
 			urls.add(new File(path).toURI().toURL());
 		}
 
 		return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
 	}
 
 
 	List<JavaFileObject> getSourceFiles(Descriptor descriptor) {
 		Map<String, OutputStream> streams = descriptor.writeToStreams(new Iterator<OutputStream>() {
 			public OutputStream next() { return new ByteArrayOutputStream(); }
 			public boolean hasNext() { return true; }
 			public void remove() { throw new UnsupportedOperationException("nope"); }
 		});
 
 		final List<JavaFileObject> files = new ArrayList<JavaFileObject>();
 
 		for (Map.Entry<String, OutputStream> entry : streams.entrySet()) {
 			String name = entry.getKey();
 			name = name.substring(0, name.length()-5); // removes .java
 
 			ByteArrayOutputStream stream = (ByteArrayOutputStream) entry.getValue();
 			JavaFileObject file = new CharSequenceJavaFileObject(name, stream.toString());
 			files.add(file);
 		}
 
 		return files;
 	}
 
 	private ClassLoader compileAndWriteClasses(Descriptor descriptor, URLClassLoader classLoader) throws MojoExecutionException {
 		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
 		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
 		JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null), classesDirectory);
 
 		List<String> options = new ArrayList<String>();
 		options.add("-classpath");
 		options.add(makeClasspath(classLoader));
 
 		Iterable<? extends JavaFileObject> compilationUnits = getSourceFiles(descriptor);
 		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
 		task.call();
 
 		try {
 			fileManager.close();
 		} catch (IOException e) {
 			// nothing
 		}
 
 		boolean atLeastOneError = false;
 
 		for (Diagnostic<? extends JavaFileObject> error : diagnostics.getDiagnostics()) {
 
 			if (error.getKind() != Diagnostic.Kind.NOTE) {
 				StringBuilder message = new StringBuilder()
 					.append(error.getSource().getName())
 					.append(" (").append(error.getLineNumber()).append(",").append(error.getColumnNumber()).append(")\n")
 					.append(error.getMessage(Locale.getDefault()))
 				;
 				getLog().warn(message.toString());
 				atLeastOneError = true;
 			}
 		}
 
 		if (atLeastOneError) {
 			throw new MojoExecutionException("The compilation was completed with errors.");
 		}
 
 		return fileManager.getClassLoader(StandardLocation.CLASS_PATH);
 	}
 
 	private static String makeClasspath(URLClassLoader classLoader) {
 		StringBuilder buffer = new StringBuilder();
 
 		for (URL url : classLoader.getURLs()) {
 			buffer.append(new File(url.getPath()));
 			buffer.append(System.getProperty("path.separator"));
 		}
 
 		return buffer.toString();
 	}
 }
