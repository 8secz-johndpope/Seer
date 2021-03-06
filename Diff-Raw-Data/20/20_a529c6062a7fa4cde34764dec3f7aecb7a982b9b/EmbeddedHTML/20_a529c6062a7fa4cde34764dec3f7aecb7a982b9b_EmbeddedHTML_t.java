 /*******************************************************************************
  * Copyright (c) 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.wst.html.core.internal.modelhandler;
 
 import java.util.ArrayList;
import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.wst.css.core.internal.provisional.adapters.IStyleSelectorAdapter;
 import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeAdapterFactory;
 import org.eclipse.wst.html.core.internal.document.HTMLModelParserAdapterFactory;
 import org.eclipse.wst.html.core.internal.htmlcss.HTMLStyleSelectorAdapterFactory;
 import org.eclipse.wst.html.core.internal.htmlcss.StyleAdapterFactory;
 import org.eclipse.wst.html.core.internal.modelquery.ModelQueryAdapterFactoryForEmbeddedHTML;
 import org.eclipse.wst.sse.core.internal.ltk.modelhandler.EmbeddedTypeHandler;
 import org.eclipse.wst.sse.core.internal.ltk.parser.BlockMarker;
 import org.eclipse.wst.sse.core.internal.ltk.parser.BlockTagParser;
 import org.eclipse.wst.sse.core.internal.ltk.parser.JSPCapableParser;
 import org.eclipse.wst.sse.core.internal.model.FactoryRegistry;
 import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
 import org.eclipse.wst.sse.core.internal.util.Assert;
import org.eclipse.wst.sse.ui.internal.IReleasable;
 import org.eclipse.wst.xml.core.internal.document.DocumentTypeAdapter;
 import org.eclipse.wst.xml.core.internal.document.ModelParserAdapter;
 import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
 
 public class EmbeddedHTML implements EmbeddedTypeHandler {
 	public String ContentTypeID_EmbeddedHTML = "org.eclipse.wst.html.core.internal.contenttype.EmbeddedHTML"; //$NON-NLS-1$
 
 	// saved for removal later
	private HashMap fLocalFactories = new HashMap();
 
 	private List supportedMimeTypes;
 
 	/**
 	 * Constructor for EmbeddedHTML.
 	 */
 	public EmbeddedHTML() {
 		super();
 	}
 
 	/**
 	 * Convenience method to add tag names using BlockMarker object
 	 */
 	protected void addHTMLishTag(BlockTagParser parser, String tagname) {
 		BlockMarker bm = new BlockMarker(tagname, null, DOMRegionContext.BLOCK_TEXT, false);
 		parser.addBlockMarker(bm);
 	}
 
 	/**
 	 * @see EmbeddedContentType#getFamilyId()
 	 */
 	public String getFamilyId() {
 		return ModelHandlerForHTML.AssociatedContentTypeID;
 	}
 
 	/*
 	 * Only "model side" embedded factories can be added here.
 	 */
 	public List getAdapterFactories() {
 		List factories = new ArrayList();
 		factories.add(new ModelQueryAdapterFactoryForEmbeddedHTML());
 		// factories.addAll(PluginContributedFactoryReader.getInstance().getFactories(this));
 		return factories;
 	}
 
 	/*
 	 * @see EmbeddedContentType#initializeParser(RegionParser)
 	 */
 	public void initializeParser(JSPCapableParser parser) {
 		addHTMLishTag(parser, "script"); //$NON-NLS-1$
 		addHTMLishTag(parser, "style"); //$NON-NLS-1$
 	}
 
 	public List getSupportedMimeTypes() {
 		if (supportedMimeTypes == null) {
 			supportedMimeTypes = new ArrayList();
 			supportedMimeTypes.add("text/html"); //$NON-NLS-1$
 			supportedMimeTypes.add("text/xhtml"); //$NON-NLS-1$
 			supportedMimeTypes.add("application/xhtml+xml"); //$NON-NLS-1$
 			supportedMimeTypes.add("application/xml"); //$NON-NLS-1$
 			supportedMimeTypes.add("text/vnd.wap.wml"); //$NON-NLS-1$
 		}
 		return supportedMimeTypes;
 	}
 
 	public void initializeFactoryRegistry(FactoryRegistry registry) {
 		Assert.isNotNull(registry);
 
 		INodeAdapterFactory factory = null;
 		if (!registry.contains(DocumentTypeAdapter.class)) {
 			factory = new HTMLDocumentTypeAdapterFactory();
 			registry.addFactory(factory);
			
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95960
			// remove and release the old adapter factory
			Object o = fLocalFactories.remove(DocumentTypeAdapter.class);
			if(o != null && o instanceof IReleasable)
				((IReleasable)o).release();
			
			fLocalFactories.put(DocumentTypeAdapter.class, factory);
 		}
 		if (!registry.contains(ModelParserAdapter.class)) {
 			factory = HTMLModelParserAdapterFactory.getInstance();
 			registry.addFactory(factory);
 			factory = StyleAdapterFactory.getInstance();
 		}
 		if (!registry.contains(IStyleSelectorAdapter.class)) {
 			registry.addFactory(factory);
 			factory = HTMLStyleSelectorAdapterFactory.getInstance();
 			registry.addFactory(factory);
 		}
 	}
 
 	public void uninitializeFactoryRegistry(FactoryRegistry registry) {
 		Assert.isNotNull(registry);
 		
         if (!fLocalFactories.isEmpty()) {
            Iterator it = fLocalFactories.values().iterator();
             while (it.hasNext()) {
                 INodeAdapterFactory af = (INodeAdapterFactory) it.next();
                 af.release();
                 registry.removeFactory(af);
             }
         }
 		fLocalFactories.clear();
 //		// note this BIG assumption about factory singletons!
 //		// for this particular list, they are, but may not
 //		// be in future.
 //
 //		IAdapterFactory factory = new HTMLDocumentTypeAdapterFactory();
 //		factory.release();
 //		registry.removeFactory(factory);
 //
 //		factory = HTMLModelParserAdapterFactory.getInstance();
 //		factory.release();
 //		registry.removeFactory(factory);
 //
 //		factory = StyleAdapterFactory.getInstance();
 //		factory.release();
 //		registry.removeFactory(factory);
 //
 //		factory = HTMLStyleSelectorAdapterFactory.getInstance();
 //		factory.release();
 //		registry.removeFactory(factory);
 	}
 
 	public void uninitializeParser(JSPCapableParser parser) {
 		// I'm assuming block markers are unique based on name only
 		// we add these as full BlockMarkers, but remove based on name alone.
 		parser.removeBlockMarker("script"); //$NON-NLS-1$
 		parser.removeBlockMarker("script"); //$NON-NLS-1$
 	}
 
 	public EmbeddedTypeHandler newInstance() {
 		return new EmbeddedHTML();
 	}
 
 	/**
 	 * will someday be controlled via extension point
 	 */
 	public boolean isDefault() {
 		return true;
 	}
 }
