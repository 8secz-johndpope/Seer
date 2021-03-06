 /*
  * Copyright 2010 Mario Zechner (contact@badlogicgames.com), Nathan Sweet (admin@esotericsoftware.com)
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
  * License. You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
  * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
  * governing permissions and limitations under the License.
  */
 
 package com.badlogic.gdx.backends.lwjgl;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.InputStream;
 import java.lang.reflect.Method;
 
 import com.badlogic.gdx.Version;
 import com.badlogic.gdx.utils.GdxRuntimeException;
 
 final class LwjglNativesLoader {
 	public static boolean load = true;
 
 	static {
 		try {
 			Method method = Class.forName("javax.jnlp.ServiceManager").getDeclaredMethod("lookup", new Class[] {String.class});
 			method.invoke(null, "javax.jnlp.PersistenceService");
 			load = false;
 		} catch (Throwable ex) {
 			load = true;
 		}
 	}
 
 	static void load () {
 		System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
 		Version.loadLibrary();
 
 		if (!load) return;
 
 		String os = System.getProperty("os.name");
 		String arch = System.getProperty("os.arch");
 		boolean is64Bit = false;
 
 		if (arch.equals("amd64")) is64Bit = true;
 
 		if (os.contains("Windows")) loadLibrariesWindows(is64Bit);
 		if (os.contains("Linux")) loadLibrariesLinux(is64Bit);
 		if (os.contains("Mac")) loadLibrariesMac();
 
 		System.setProperty("org.lwjgl.librarypath", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
 	}
 
 	private static void loadLibrariesWindows (boolean is64Bit) {
 		String[] libNames = null;
 		if (is64Bit)
 			libNames = new String[] {"OpenAL64.dll", "lwjgl64.dll"};
 		else
 			libNames = new String[] {"OpenAL32.dll", "lwjgl.dll"};
 
 		for (String libName : libNames)
 			loadLibrary(libName, "/native/windows/", System.getProperty("java.io.tmpdir") + File.separator);
 	}
 
 	private static void loadLibrariesLinux (boolean is64Bit) {
 		String[] libNames = null;
 		if (is64Bit)
 			libNames = new String[] {"libopenal64.so", "liblwjgl64.so"};
 		else
 			libNames = new String[] {"libopenal.so", "liblwjgl.so"};
 
 		for (String libName : libNames)
 			loadLibrary(libName, "/native/linux/", System.getProperty("java.io.tmpdir") + File.separator);
 	}
 
	private static void loadLibrariesMac () {		
		String[] libNames = new String[] {"libopenal.dylib", "liblwjgl.jnilib"};
		for (String libName : libNames)
			 loadLibrary(libName, "/native/macosx/", System.getProperty("java.io.tmpdir") + File.separator);
 	}
 
 	private static void loadLibrary (String libName, String classPath, String outputPath) {
 		if (new File(outputPath + libName).exists()) return;
 
 		InputStream in = null;
 		BufferedOutputStream out = null;
 
 		try {
 			in = LwjglApplication.class.getResourceAsStream(classPath + libName);
 			out = new BufferedOutputStream(new FileOutputStream(outputPath + libName));
 			byte[] bytes = new byte[1024 * 4];
 			while (true) {
 				int read_bytes = in.read(bytes);
 				if (read_bytes == -1) break;
 
 				out.write(bytes, 0, read_bytes);
 			}
 			out.close();
 			out = null;
 			in.close();
 			in = null;
 		} catch (Throwable t) {
 			// throw new GdxRuntimeException("Couldn't load lwjgl native, " + libName, t);
 		} finally {
 			if (out != null) try {
 				out.close();
 			} catch (Exception ex) {
 			}
 			;
 			if (in != null) try {
 				in.close();
 			} catch (Exception ex) {
 			}
 		}
 	}
 
 }
