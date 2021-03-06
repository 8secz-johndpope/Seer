 /*******************************************************************************
  * Copyright (c) 2006, 2007, 2008 Obeo.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     Obeo - initial API and implementation
  *******************************************************************************/
 package org.eclipse.emf.compare.util;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.StringWriter;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.compare.EMFCompareMessages;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.emf.ecore.resource.ResourceSet;
 import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.emf.ecore.xmi.XMLResource;
 import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
 
 /**
  * Utility class for model loading/saving and serialization.
  * 
  * @author Laurent Goubet <a href="mailto:laurent.goubet@obeo.fr">laurent.goubet@obeo.fr</a>
  */
 public final class ModelUtils {
 	/** Constant for the file encoding system property. */
 	private static final String ENCODING_PROPERTY = "file.encoding"; //$NON-NLS-1$
 
 	/**
 	 * Utility classes don't need to (and shouldn't) be instantiated.
 	 */
 	private ModelUtils() {
 		// prevents instantiation
 	}
 
 	/**
 	 * Attaches the given {@link EObject} to a new resource created in a new {@link ResourceSet} with the
 	 * given URI.
 	 * 
 	 * @param resourceURI
 	 *            URI of the new resource to create.
 	 * @param root
 	 *            EObject to attach to a new resource.
	 * @return The resource <tt>root</tt> has been attached to.
 	 */
	public static Resource attachResource(URI resourceURI, EObject root) {
 		if (root == null)
 			throw new NullPointerException(EMFCompareMessages.getString("ModelUtils.NullRoot")); //$NON-NLS-1$
 
 		final Resource newResource = createResource(resourceURI);
 		newResource.getContents().add(root);
		return newResource;
 	}
 
 	/**
 	 * Attaches the given {@link EObject} to a new resource created in the given {@link ResourceSet} with the
 	 * given URI.
 	 * 
 	 * @param resourceURI
 	 *            URI of the new resource to create.
 	 * @param resourceSet
 	 *            ResourceSet in which to create the resource.
 	 * @param root
 	 *            EObject to attach to a new resource.
	 * @return The resource <tt>root</tt> has been attached to.
 	 */
	public static Resource attachResource(URI resourceURI, ResourceSet resourceSet, EObject root) {
 		if (root == null)
 			throw new NullPointerException(EMFCompareMessages.getString("ModelUtils.NullRoot")); //$NON-NLS-1$
 
 		final Resource newResource = createResource(resourceURI, resourceSet);
 		newResource.getContents().add(root);
		return newResource;
 	}
 
 	/**
 	 * This will create a {@link Resource} given the model extension it is intended for.
 	 * 
 	 * @param modelURI
 	 *            {@link org.eclipse.emf.common.util.URI URI} where the model is stored.
 	 * @return The {@link Resource} given the model extension it is intended for.
 	 */
 	public static Resource createResource(URI modelURI) {
 		return createResource(modelURI, new ResourceSetImpl());
 	}
 
 	/**
 	 * This will create a {@link Resource} given the model extension it is intended for and a ResourceSet.
 	 * 
 	 * @param modelURI
 	 *            {@link org.eclipse.emf.common.util.URI URI} where the model is stored.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The {@link Resource} given the model extension it is intended for.
 	 */
 	public static Resource createResource(URI modelURI, ResourceSet resourceSet) {
 		String fileExtension = modelURI.fileExtension();
 		if (fileExtension == null || fileExtension.length() == 0) {
 			fileExtension = Resource.Factory.Registry.DEFAULT_EXTENSION;
 		}
 
 		final Resource.Factory.Registry registry = Resource.Factory.Registry.INSTANCE;
 		final Object resourceFactory = registry.getExtensionToFactoryMap().get(fileExtension);
 		if (resourceFactory != null) {
 			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension,
 					resourceFactory);
 		} else {
 			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension,
 					new XMIResourceFactoryImpl());
 		}
 
 		return resourceSet.createResource(modelURI);
 	}
 
 	/**
 	 * Loads the models contained by the given directory in the given ResourceSet.
 	 * <p>
 	 * If <code>resourceSet</code> is <code>null</code>, all models will be loaded in a new resourceSet.
 	 * </p>
 	 * 
 	 * @param directory
 	 *            The directory from which to load the models.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in. If <code>null</code>, all models will be
 	 *            loaded in a new resourceSet.
 	 * @return The models contained by the given directory.
 	 * @throws IOException
 	 *             Thrown if an I/O operation has failed or been interrupted.
 	 */
 	public static List<EObject> getModelsFrom(File directory, ResourceSet resourceSet) throws IOException {
 		return getModelsFrom(directory, null, resourceSet);
 	}
 
 	/**
 	 * Loads the files with the given extension contained by the given directory as EObjects in the given
 	 * ResourceSet.
 	 * <p>
 	 * If <code>resourceSet</code> is <code>null</code>, all models will be loaded in a new resourceSet.
 	 * </p>
 	 * <p>
 	 * The argument <code>extension</code> is in fact the needed suffix for its name in order for a file to
 	 * be loaded. If it is equal to &quot;rd&quot;, a file named &quot;model.aird&quot; will be loaded, but so
 	 * would be a file named &quot;Shepherd&quot;.
 	 * </p>
 	 * <p>
 	 * The empty String or <code>null</code> will result in all the files of the given directory to be
 	 * loaded, and would then be equivalent to {@link #getModelsFrom(File)}.
 	 * </p>
 	 * 
 	 * @param directory
 	 *            The directory from which to load the models.
 	 * @param extension
 	 *            File extension of the files to load. If <code>null</code>, will consider all extensions.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in. If <code>null</code>, all models will be
 	 *            loaded in a new resourceSet.
 	 * @return The models contained by the given directory.
 	 * @throws IOException
 	 *             Thrown if an I/O operation has failed or been interrupted.
 	 */
 	public static List<EObject> getModelsFrom(File directory, String extension, ResourceSet resourceSet)
 			throws IOException {
 		final List<EObject> models = new ArrayList<EObject>();
 		final String fileExtension;
 		if (extension != null)
 			fileExtension = extension;
 		else
 			fileExtension = ""; //$NON-NLS-1$
 
 		final ResourceSet theResourceSet;
 		if (resourceSet == null)
 			theResourceSet = new ResourceSetImpl();
 		else
 			theResourceSet = resourceSet;
 
 		if (directory.exists() && directory.isDirectory() && directory.listFiles() != null) {
 			final File[] files = directory.listFiles();
 			for (int i = 0; i < files.length; i++) {
 				final File aFile = files[i];
 
 				if (!aFile.isDirectory() && aFile.getName().matches("[^.].*?\\Q" + fileExtension + "\\E")) { //$NON-NLS-1$ //$NON-NLS-2$
 					models.add(load(aFile, theResourceSet));
 				}
 			}
 		}
 
 		return models;
 	}
 
 	/**
 	 * Loads a model from a {@link java.io.File File} in a given {@link ResourceSet}.
 	 * <p>
 	 * This will return the first root of the loaded model, other roots can be accessed via the resource's
 	 * content.
 	 * </p>
 	 * 
 	 * @param file
 	 *            {@link java.io.File File} containing the model to be loaded.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The model loaded from the file.
 	 * @throws IOException
 	 *             If the given file does not exist.
 	 */
 	public static EObject load(File file, ResourceSet resourceSet) throws IOException {
 		return load(URI.createFileURI(file.getPath()), resourceSet);
 	}
 
 	/**
 	 * Loads a model from an {@link org.eclipse.core.resources.IFile IFile} in a given {@link ResourceSet}.
 	 * <p>
 	 * This will return the first root of the loaded model, other roots can be accessed via the resource's
 	 * content.
 	 * </p>
 	 * 
 	 * @param file
 	 *            {@link org.eclipse.core.resources.IFile IFile} containing the model to be loaded.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The model loaded from the file.
 	 * @throws IOException
 	 *             If the given file does not exist.
 	 */
 	public static EObject load(IFile file, ResourceSet resourceSet) throws IOException {
 		EObject result = null;
 
 		final Map<String, String> options = new HashMap<String, String>();
 		options.put(XMLResource.OPTION_ENCODING, System.getProperty(ENCODING_PROPERTY));
 		// First tries to load the IFile assuming it is in the workspace
 		Resource modelResource = createResource(URI.createPlatformResourceURI(
 				file.getFullPath().toOSString(), true), resourceSet);
 		try {
 			modelResource.load(options);
 		} catch (IOException e) {
 			// If it failed, load the file assuming it is in the plugins
 			resourceSet.getResources().remove(modelResource);
 			modelResource = createResource(
 					URI.createPlatformPluginURI(file.getFullPath().toOSString(), true), resourceSet);
 			try {
 				modelResource.load(options);
 			} catch (IOException ee) {
 				// If it fails anew, throws the first IOException
 				throw e;
 			}
 		}
 		// Returns the first root of the loaded model
 		if (modelResource.getContents().size() > 0)
 			result = modelResource.getContents().get(0);
 		return result;
 	}
 
 	/**
 	 * Load a model from an {@link java.io.InputStream  InputStream} in a given {@link ResourceSet}.
 	 * <p>
 	 * This will return the first root of the loaded model, other roots can be accessed via the resource's
 	 * content.
 	 * </p>
 	 * 
 	 * @param stream
 	 *            The inputstream to load from
 	 * @param fileName
 	 *            The original filename
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The loaded model
 	 * @throws IOException
 	 *             If the given file does not exist.
 	 */
 	public static EObject load(InputStream stream, String fileName, ResourceSet resourceSet)
 			throws IOException {
 		if (stream == null)
 			throw new NullPointerException(EMFCompareMessages.getString("ModelUtils.NullInputStream")); //$NON-NLS-1$
 		EObject result = null;
 
 		final Resource modelResource = createResource(URI.createURI(fileName), resourceSet);
 		final Map<String, String> options = new EMFCompareMap<String, String>();
 		options.put(XMLResource.OPTION_ENCODING, System.getProperty(ENCODING_PROPERTY));
 		modelResource.load(stream, options);
 		if (modelResource.getContents().size() > 0)
 			result = modelResource.getContents().get(0);
 		return result;
 	}
 
 	/**
 	 * Loads a model from an {@link IPath} in a given {@link ResourceSet}.
 	 * <p>
 	 * This will return the first root of the loaded model, other roots can be accessed via the resource's
 	 * content.
 	 * </p>
 	 * 
 	 * @param path
 	 *            {@link IPath} where the model lies.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The model loaded from the path.
 	 * @throws IOException
 	 *             If the given file does not exist.
 	 */
 	public static EObject load(IPath path, ResourceSet resourceSet) throws IOException {
 		return load(ResourcesPlugin.getWorkspace().getRoot().getFile(path), resourceSet);
 	}
 
 	/**
 	 * Loads a model from an {@link org.eclipse.emf.common.util.URI URI} in a given {@link ResourceSet}.
 	 * <p>
 	 * This will return the first root of the loaded model, other roots can be accessed via the resource's
 	 * content.
 	 * </p>
 	 * 
 	 * @param modelURI
 	 *            {@link org.eclipse.emf.common.util.URI URI} where the model is stored.
 	 * @param resourceSet
 	 *            The {@link ResourceSet} to load the model in.
 	 * @return The model loaded from the URI.
 	 * @throws IOException
 	 *             If the given file does not exist.
 	 */
 	public static EObject load(URI modelURI, ResourceSet resourceSet) throws IOException {
 		EObject result = null;
 
 		final Resource modelResource = createResource(modelURI, resourceSet);
 		final Map<String, String> options = new EMFCompareMap<String, String>();
 		options.put(XMLResource.OPTION_ENCODING, System.getProperty(ENCODING_PROPERTY));
 		modelResource.load(options);
 		if (modelResource.getContents().size() > 0)
 			result = modelResource.getContents().get(0);
 		return result;
 	}
 
 	/**
 	 * Saves a model as a file to the given path.
 	 * 
 	 * @param root
 	 *            Root of the objects to be serialized in a file.
 	 * @param path
 	 *            File where the objects have to be saved.
 	 * @throws IOException
 	 *             Thrown if an I/O operation has failed or been interrupted during the saving process.
 	 */
 	public static void save(EObject root, String path) throws IOException {
 		if (root == null)
 			throw new NullPointerException(EMFCompareMessages.getString("ModelUtils.NullSaveRoot")); //$NON-NLS-1$
 
 		final Resource newModelResource = createResource(URI.createFileURI(path));
 		newModelResource.getContents().add(root);
 		final Map<String, String> options = new EMFCompareMap<String, String>();
 		options.put(XMLResource.OPTION_ENCODING, System.getProperty(ENCODING_PROPERTY));
 		newModelResource.save(options);
 	}
 
 	/**
 	 * Serializes the given EObjet as a String.
 	 * 
 	 * @param root
 	 *            Root of the objects to be serialized.
 	 * @return The given EObjet serialized as a String.
 	 * @throws IOException
 	 *             Thrown if an I/O operation has failed or been interrupted during the saving process.
 	 */
 	public static String serialize(EObject root) throws IOException {
 		if (root == null)
 			throw new NullPointerException(EMFCompareMessages.getString("ModelUtils.NullSaveRoot")); //$NON-NLS-1$
 
 		// Copies the root to avoid modifying it
 		final EObject copyRoot = EcoreUtil.copy(root);
 		attachResource(URI.createFileURI("resource.xml"), copyRoot); //$NON-NLS-1$
 
 		final StringWriter writer = new StringWriter();
 		final Map<String, String> options = new EMFCompareMap<String, String>();
 		options.put(XMLResource.OPTION_ENCODING, System.getProperty(ENCODING_PROPERTY));
 		// Should not throw ClassCast since uri calls for an xml resource
 		((XMLResource)copyRoot.eResource()).save(writer, options);
 		final String result = writer.toString();
 		writer.flush();
 		return result;
 	}
 }
