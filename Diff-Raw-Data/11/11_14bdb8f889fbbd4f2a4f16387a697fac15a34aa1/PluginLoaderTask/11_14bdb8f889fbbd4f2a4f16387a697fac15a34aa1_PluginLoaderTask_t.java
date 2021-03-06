 package org.cytoscape.plugin.internal;
 
 
 import org.cytoscape.plugin.CyPlugin;
 import org.cytoscape.plugin.CyPluginAdapter;
 import org.cytoscape.work.AbstractTask;
 import org.cytoscape.work.Tunable;
 import org.cytoscape.work.TaskMonitor;
 
 import java.util.jar.JarFile;
 import java.util.jar.Manifest;
 import java.lang.reflect.Constructor;
 import java.io.File;
 import java.net.URLClassLoader;
 import java.net.URL;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
 public class PluginLoaderTask extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger( PluginLoaderTask.class );
 	private CyPluginAdapter adapter;
 
 	@Tunable(description="Select plugin JAR to load",params="input=true")
 	public File filename;
 
 	PluginLoaderTask(CyPluginAdapter adapter) {
 		this.adapter = adapter;
 	}
 
 	@Override
 	public void run(TaskMonitor tm) throws Exception {
 		Object o = null; 
 		JarFile jar = null; 
 		try {
 			jar = new JarFile(filename);
			logger.debug("attempting to load simple plugin jar: " + filename);
 			String name = jar.getManifest().getMainAttributes().getValue("Cytoscape-Plugin");
			logger.debug("attempting to load CyPlugin class: " + name); 	
			if ( name == null || name.isEmpty() )
				throw new IllegalArgumentException("This plugin jar is missing the \"Cytoscape-Plugin: your.package.YourCyPlugin\" entry in the META-INF/MANIFEST.MF file. Without that entry we can't start the plugin!");
 			URL jarurl = filename.toURI().toURL(); 
 			URLClassLoader ucl = URLClassLoader.newInstance( new URL[]{jarurl}, 
 			                                      PluginLoaderTask.class.getClassLoader() );
 			Class c = ucl.loadClass(name);
 			Constructor<CyPlugin> con = c.getConstructor(CyPluginAdapter.class);
 			o = con.newInstance(adapter);
 		} finally {
 			if (jar != null) 
 				jar.close();
 		}
 	}
 
 	@Override
 	public void cancel() {
 	}
 }
