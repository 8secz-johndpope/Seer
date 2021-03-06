 /*******************************************************************************
  * Copyright 2010 Mario Zechner (contact@badlogicgames.com)
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
  * License. You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
  * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
  * governing permissions and limitations under the License.
  ******************************************************************************/
 
 package com.badlogic.gdx;
 
 import com.badlogic.gdx.files.FileHandle;
 import com.badlogic.gdx.utils.GdxRuntimeException;
 
 /**
  * Provides standard access to the filesystem, classpath, Android SD card, and Android assets directory.
  * @author mzechner
  * @author Nathan Sweet <misc@n4te.com>
  */
 public interface Files {
 	/**
 	 * Indicates how to resolve a path to a file.
 	 * @author mzechner
 	 */
 	public enum FileType {
 		/**
 		 * Path relative to the root of the classpath, and if not found there, to the asset directory on Android or the
 		 * application's root directory on the desktop. Internal files are always readonly.
 		 */
 		Internal,
 
 		/**
 		 * Path relative to the root of the SD card on Android and to the home directory of the current user on the desktop.
 		 */
 		External,
 
 		/**
 		 * Path that is a fully qualified, absolute filesystem path. To ensure portability across platforms use absolute files only
 		 * when absolutely necessary.
 		 */
 		Absolute
 	}
 
 	/**
 	 * Returns a handle representing a file or directory.
 	 * @param type Determines how the path is resolved.
 	 * @throws GdxRuntimeException if the type is internal and the file does not exist.
 	 * @see FileType
 	 */
 	public FileHandle getFileHandle (String path, FileType type);
 
 	/**
 	 * Convenience method that returns an {@link FileType#Internal} file handle.
 	 */
 	public FileHandle internal (String path);
 
 	/**
 	 * Convenience method that returns an {@link FileType#External} file handle.
 	 */
 	public FileHandle external (String path);
 
 	/**
 	 * Convenience method that returns an {@link FileType#Absolute} file handle.
 	 */
 	public FileHandle absolute (String path);
 
 	/**
	 * Returns the external storage path directory. This is the SD card on Android or the home directory of the current user on the
 	 * desktop.
 	 */
 	public String getExternalStoragePath ();
 
 	/**
	 * Returns true if the external storage is ready for file i/o.
 	 */
 	public boolean isExternalStorageAvailable ();
 }
