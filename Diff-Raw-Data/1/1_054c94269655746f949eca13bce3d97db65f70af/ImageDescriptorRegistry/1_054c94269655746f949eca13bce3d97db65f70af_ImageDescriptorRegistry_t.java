 package org.eclipse.jdt.internal.debug.ui;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import java.util.HashMap;
 import java.util.Iterator;
 
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.util.Assert;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.widgets.Display;
 
 /**
  * A registry that maps <code>ImageDescriptors</code> to <code>Image</code>.
  */
 public class ImageDescriptorRegistry {
 
 	private HashMap fRegistry= new HashMap(10);
 	private Display fDisplay;
 	
 	/**
 	 * Creates a new image descriptor registry for the current or default display,
 	 * respectively.
 	 */
 	public ImageDescriptorRegistry() {
 		this(JDIDebugUIPlugin.getStandardDisplay());
 	}
 	
 	/**
 	 * Creates a new image descriptor registry for the given display. All images
 	 * managed by this registry will be disposed when the display gets disposed.
 	 * 
 	 * @param diaplay the display the images managed by this registry are allocated for 
 	 */
 	public ImageDescriptorRegistry(Display display) {
 		fDisplay= display;
 		Assert.isNotNull(fDisplay);
 		hookDisplay();
 	}
 	
 	/**
 	 * Returns the image associated with the given image descriptor.
 	 * 
 	 * @param descriptor the image descriptor for which the registry manages an image
 	 * @return the image associated with the image descriptor or <code>null</code>
 	 *  if the image descriptor can't create the requested image.
 	 */
 	public Image get(ImageDescriptor descriptor) {
 		if (descriptor == null)
 			descriptor= ImageDescriptor.getMissingImageDescriptor();
 			
 		Image result= (Image)fRegistry.get(descriptor);
 		if (result != null)
 			return result;
 	
 		Assert.isTrue(fDisplay == JDIDebugUIPlugin.getStandardDisplay(), DebugUIMessages.getString("ImageDescriptorRegistry.Allocating_image_for_wrong_display_1")); //$NON-NLS-1$
 		result= descriptor.createImage();
 		if (result != null)
 			fRegistry.put(descriptor, result);
 		return result;
 	}
 
 	/**
 	 * Disposes all images managed by this registry.
 	 */	
 	public void dispose() {
 		for (Iterator iter= fRegistry.values().iterator(); iter.hasNext(); ) {
 			Image image= (Image)iter.next();
 			image.dispose();
 		}
		fRegistry.clear();
 	}
 	
 	private void hookDisplay() {
 		fDisplay.disposeExec(new Runnable() {
 			public void run() {
 				dispose();
 			}	
 		});
 	}
 }
 
