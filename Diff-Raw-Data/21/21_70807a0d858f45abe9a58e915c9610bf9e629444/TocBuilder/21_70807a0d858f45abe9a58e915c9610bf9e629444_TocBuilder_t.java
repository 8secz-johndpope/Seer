 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 package org.eclipse.help.internal.toc;

 import java.util.*;
import org.eclipse.help.internal.util.*;

 public class TocBuilder {
 	// list of all toc files
 	protected Collection contributedTocFiles;
 	// list of unprocessed toc files
 	protected Collection unprocessedTocFiles;
 	// list of unprocessed toc (the target of attach_to was not available at the time)
 	protected List unprocessedTocs;
 	/**
 	 * Constructor.
 	 */
 	public TocBuilder() {
 		unprocessedTocFiles = new ArrayList();
 		unprocessedTocs = new ArrayList();
 	}
 	public Collection getBuiltTocs() {
 		// returns the list of root Toc trees
 		Collection tocCol = new ArrayList(contributedTocFiles.size());
 		for (Iterator it = contributedTocFiles.iterator(); it.hasNext();) {
 			TocFile tocFile = (TocFile) it.next();
 			Toc toc = tocFile.getToc();
 			if (toc != null && toc.getTocFile().isPrimary() && !isIntegrated(toc))
 				tocCol.add((toc));
 		}
 		return tocCol;
 	}
 	/**
 	 */
 	public void build(Collection contributedTocFiles) {
 		this.contributedTocFiles = contributedTocFiles;
 		unprocessedTocFiles.addAll(contributedTocFiles);
 		// process all the toc files.
 		// A side-effect is that linked files are also processed
 		while (!unprocessedTocFiles.isEmpty()) {
 			TocFile tocFile = (TocFile) unprocessedTocFiles.iterator().next();
 			tocFile.build(this);
 		}
 		// try processing as many toc (link_to) as possible
 		// All these toc could not be attached because the 
 		// target node was not parsed at that time
 		int remaining = unprocessedTocs.size();
 		for (int i = 0; i < remaining; i++) {
 			Toc toc = (Toc) unprocessedTocs.get(i);
 			buildToc(toc);
 		}
 	}
 	public void buildTocFile(TocFile tocFile) {
		try
		{
			unprocessedTocFiles.remove(tocFile);
			//tocFile.build(this);
			TocFileParser parser = new TocFileParser(this);
			parser.parse(tocFile);
		}
		catch(Exception e)
		{
			String msg = Resources.getString("E033",tocFile.getHref());
			Logger.logError(msg, e);
		}
 	}
 	public void buildAnchor(Anchor anchor) {
 		// cache the anchor in the toc file
 		anchor.getTocFile().addAnchor(anchor);
 	}
 	public void buildLink(Link link) {
 		// parse the linked file
 		String linkedToc = link.getToc();
 		TocFile includedTocFile = getTocFile(linkedToc);
 		if (includedTocFile == null)
 			return;
 		Toc toc = includedTocFile.getToc();
 		if (toc == null)
 			return;
 		// link the two Toc objects
 		link.addChild(toc);
 	}
 	public void buildTopic(Topic topic) {
 		// nothing to do
 	}
 	public void buildToc(Toc toc) {
 		// link toc if so specified
 		String href = toc.getLink_to();
 		if (href == null || href.equals(""))
 			return;
 		TocFile targetTocFile = getTocFile(href);
 		if (targetTocFile == null)
 			return;
 		Anchor anchor = targetTocFile.getAnchor(href);
 		if (anchor == null) {
 			unprocessedTocs.add(toc);
 			return;
 		}
 		// link the two toc objects
 		anchor.addChild(toc);
 	}
 	private TocFile getTocFile(String href) {
 		String plugin = HrefUtil.getPluginIDFromHref(href);
 		if (plugin == null)
 			return null;
 		String path = HrefUtil.getResourcePathFromHref(href);
 		if (path == null)
 			return null;
 		TocFile tocFile = null;
 		for (Iterator it = contributedTocFiles.iterator(); it.hasNext();) {
 			tocFile = (TocFile) it.next();
 			if (tocFile.getPluginID().equals(plugin)
 				&& tocFile.getHref().equals(path))
 				break;
 			else
 				tocFile = null;
 		}
 		if (tocFile == null)
 			return null;
 		if (unprocessedTocFiles.contains(tocFile))
 			buildTocFile(tocFile);
 		return tocFile;
 	}
 	/** 
 	 * Checks if navigation element has been integrated
 	 * into another TOC.
 	 */
 	private boolean isIntegrated (TocNode element){
 		// check if there if there is TOC in ancestor hierarchy (depth first)
 		for(Iterator it=element.getParents().iterator();it.hasNext();){
 			TocNode parent=(TocNode)it.next();
 			if(parent instanceof Toc && ((Toc)parent).getTocFile().isPrimary()){
 				return true;
 			}else if(isIntegrated(parent)){
 				return true;
 			}
 		}
 		// no ancestor is a TOC
 		return false;
 	}
 }
