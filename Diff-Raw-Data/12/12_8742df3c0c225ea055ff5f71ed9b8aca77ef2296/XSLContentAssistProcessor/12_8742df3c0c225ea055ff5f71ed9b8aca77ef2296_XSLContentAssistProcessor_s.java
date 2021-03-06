 /*******************************************************************************
  * Copyright (c) 2008 Standards for Technology in Automotive Retail and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     David Carver - STAR - bug 213849 - initial API and implementation
  *     David Carver - STAR - bug 230958 - refactored to fix bug with getting
  *                                        the DOM Document for the current editor
  *     David Carver - STAR - bug 240170 - refactored code to help with narrowing of
  *                                        results and easier maintenance.
  *     
  *******************************************************************************/
 package org.eclipse.wst.xsl.ui.internal.contentassist;
 
 import java.util.ArrayList;
 
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.text.contentassist.ICompletionProposal;
 import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
 import org.eclipse.jface.text.contentassist.IContextInformation;
 import org.eclipse.jface.text.contentassist.IContextInformationValidator;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
 import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
 import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
 import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
 import org.eclipse.wst.sse.ui.internal.IReleasable;
 import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
 import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
 import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
 import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
 import org.eclipse.wst.xml.ui.internal.contentassist.AbstractContentAssistProcessor;
 import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
 import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;
 import org.eclipse.wst.xsl.core.XSLCore;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 
 /**
  * The XSL Content Assist Processor provides content assistance for various
  * attributes values within the XSL Editor. This includes support for xpaths on
  * select statements as well as on test and match attributes.
  * 
  * @author David Carver
  * 
  * 
  * 
  */
 public class XSLContentAssistProcessor implements IContentAssistProcessor,
 		IReleasable {
 
 	private static final String ATTR_SELECT = "select"; //$NON-NLS-1$
 	private static final String ATTR_TEST = "test"; //$NON-NLS-1$
 	private static final String ATTR_MATCH = "match"; //$NON-NLS-1$
 	private static final String ATTR_EXCLUDE_RESULT_PREFIXES = "exclude-result-prefixes"; //$NON-NLS-1$
 	/**
 	 * Retrieve all global variables in the stylesheet.
 	 */
 
 	private String errorMessage = "";
 	private ITextViewer textViewer = null;
 
 	/**
 	 * The XSL Content Assist Processor handles XSL specific functionality for
 	 * content assistance. It leverages several XPath selection variables to
 	 * help with the selection of elements and template names.
 	 * 
 	 */
 	public XSLContentAssistProcessor() {
 		super();
 	}
 
 	/**
 	 * CONTENT ASSIST STARTS HERE
 	 * 
 	 * Return a list of proposed code completions based on the specified
 	 * location within the document that corresponds to the current cursor
 	 * position within the text-editor control.
 	 * 
 	 * @param textViewer
 	 * @param documentPosition
 	 *            - the cursor location within the document
 	 * 
 	 *            an array of ICompletionProposals
 	 */
 	public ICompletionProposal[] computeCompletionProposals(
 			ITextViewer textViewer, int documentPosition) {
 		setErrorMessage(null);
 		ContentAssistRequest contentAssistRequest = null;
 
 		this.textViewer = textViewer;
 
 		IndexedRegion treeNode = ContentAssistUtils.getNodeAt(textViewer,
 				documentPosition);
 
 		Node node = (Node) treeNode;
 		while ((node != null) && (node.getNodeType() == Node.TEXT_NODE)
 				&& (node.getParentNode() != null)) {
 			node = node.getParentNode();
 		}
 		IDOMNode xmlNode = (IDOMNode) node;
 		IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(documentPosition);
 		ITextRegion completionRegion = getCompletionRegion(documentPosition,
 				node);
 
 		AbstractContentAssistProcessor processor = new XMLContentAssistProcessor();
 
 		ICompletionProposal proposals[] = processor.computeCompletionProposals(
 				textViewer, documentPosition);
 
 		String matchString = getXPathMatchString(sdRegion, completionRegion,
 				documentPosition);
 
 		if (XSLCore.isXSLNamespace(xmlNode)) {
 			proposals = getXSLProposals(textViewer, documentPosition, xmlNode,
 					sdRegion, completionRegion, proposals, matchString);
 		}
 		return proposals;
 	}
 
 	protected ICompletionProposal[] getXSLProposals(ITextViewer textViewer,
 			int documentPosition, IDOMNode xmlNode,
 			IStructuredDocumentRegion sdRegion, ITextRegion completionRegion,
 			ICompletionProposal[] proposals, String matchString) {
 		ContentAssistRequest contentAssistRequest;
 		NamedNodeMap nodeMap = xmlNode.getAttributes();
 		
 		if (this.hasAttributeAtTextRegion(ATTR_SELECT, nodeMap, completionRegion)) {
 			contentAssistRequest = new SelectAttributeContentAssist(
 					xmlNode, xmlNode.getParentNode(), sdRegion,
 					completionRegion, documentPosition, 0, matchString,
 					textViewer);
 		 proposals = contentAssistRequest.getCompletionProposals();
 		} 
 		if (this.hasAttributeAtTextRegion(ATTR_TEST, nodeMap, completionRegion)) {
 			contentAssistRequest = new TestAttributeContentAssist(
 					xmlNode, xmlNode.getParentNode(), sdRegion,
 					completionRegion, documentPosition, 0, matchString,
 					textViewer);
 			proposals = contentAssistRequest.getCompletionProposals();
 		}
 		if (this.hasAttributeAtTextRegion(ATTR_EXCLUDE_RESULT_PREFIXES, nodeMap, completionRegion)) {
 			contentAssistRequest = new ExcludeResultPrefixesContentAssist(
 					xmlNode, xmlNode.getParentNode(), sdRegion,
 					completionRegion, documentPosition, 0, matchString,
 					textViewer);
 			
 			proposals = contentAssistRequest.getCompletionProposals();
 		}
 		return proposals;
 	}
 	
 	/**
 	 * StructuredTextViewer must be set before using this.
 	 * 
 	 * @param pos
 	 * @return
 	 */
 	public IStructuredDocumentRegion getStructuredDocumentRegion(int pos) {
 		return ContentAssistUtils.getStructuredDocumentRegion(textViewer, pos);
 	}
 
 	/**
 	 * Gets the attribute name that the content assist was triggered on.
 	 * 
 	 * @param contentAssistRequest
 	 * @return
 	 */
 	private String getAttributeName(IDOMNode xmlNode, ITextRegion region) {
 		IStructuredDocumentRegion open = xmlNode
 				.getFirstStructuredDocumentRegion();
 		ITextRegionList openRegions = open.getRegions();
 		int i = openRegions.indexOf(region);
 		if (i >= 0) {
 
 			ITextRegion nameRegion = null;
 			while (i >= 0) {
 				nameRegion = openRegions.get(i--);
 				if (nameRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
 					break;
 				}
 			}
 
 			// String attributeName = nameRegion.getText();
 			return open.getText(nameRegion);
 		}
 		return null;
 	}
 
 	/**
 	 * Return the region whose content's require completion. This is something
 	 * of a misnomer as sometimes the user wants to be prompted for contents of
 	 * a non-existant ITextRegion, such as for enumerated attribute values
 	 * following an '=' sign.
 	 * 
 	 * Copied from AbstractContentAssist Processor.
 	 */
 	protected ITextRegion getCompletionRegion(int documentPosition, Node domnode) {
 		if (domnode == null) {
 			return null;
 		}
 
 		ITextRegion region = null;
 		int offset = documentPosition;
 		IStructuredDocumentRegion flatNode = null;
 		IDOMNode node = (IDOMNode) domnode;
 
 		if (node.getNodeType() == Node.DOCUMENT_NODE) {
 			if (node.getStructuredDocument().getLength() == 0) {
 				return null;
 			}
 			ITextRegion result = node.getStructuredDocument()
 					.getRegionAtCharacterOffset(offset)
 					.getRegionAtCharacterOffset(offset);
 			while (result == null) {
 				offset--;
 				result = node.getStructuredDocument()
 						.getRegionAtCharacterOffset(offset)
 						.getRegionAtCharacterOffset(offset);
 			}
 			return result;
 		}
 
 		IStructuredDocumentRegion startTag = node
 				.getStartStructuredDocumentRegion();
 		IStructuredDocumentRegion endTag = node
 				.getEndStructuredDocumentRegion();
 
 		if ((startTag != null) && (startTag.getStartOffset() <= offset)
 				&& (offset < startTag.getStartOffset() + startTag.getLength())) {
 			flatNode = startTag;
 		} else if ((endTag != null) && (endTag.getStartOffset() <= offset)
 				&& (offset < endTag.getStartOffset() + endTag.getLength())) {
 			flatNode = endTag;
 		}
 
 		if (flatNode != null) {
 			region = getCompletionRegion(offset, flatNode);
 		} else {
 			flatNode = node.getStructuredDocument().getRegionAtCharacterOffset(
 					offset);
 			if ((flatNode.getStartOffset() <= documentPosition)
 					&& (flatNode.getEndOffset() >= documentPosition)) {
 				if ((offset == flatNode.getStartOffset())
 						&& (flatNode.getPrevious() != null)
 						&& (((flatNode
 								.getRegionAtCharacterOffset(documentPosition) != null) && (flatNode
 								.getRegionAtCharacterOffset(documentPosition)
 								.getType() != DOMRegionContext.XML_CONTENT))
 								|| (flatNode.getPrevious().getLastRegion()
 										.getType() == DOMRegionContext.XML_TAG_OPEN) || (flatNode
 								.getPrevious().getLastRegion().getType() == DOMRegionContext.XML_END_TAG_OPEN))) {
 					region = flatNode.getPrevious().getLastRegion();
 				} else if (flatNode.getEndOffset() == documentPosition) {
 					region = flatNode.getLastRegion();
 				} else {
 					region = flatNode.getFirstRegion();
 				}
 			} else {
 				region = flatNode.getLastRegion();
 			}
 		}
 
 		return region;
 	}
 
 	protected ITextRegion getCompletionRegion(int offset,
 			IStructuredDocumentRegion sdRegion) {
 		ITextRegion region = sdRegion.getRegionAtCharacterOffset(offset);
 		if (region == null) {
 			return null;
 		}
 
 		if (sdRegion.getStartOffset(region) == offset) {
 			// The offset is at the beginning of the region
 			if ((sdRegion.getStartOffset(region) == sdRegion.getStartOffset())
 					&& (sdRegion.getPrevious() != null)
 					&& (!sdRegion.getPrevious().isEnded())) {
 				region = sdRegion.getPrevious().getRegionAtCharacterOffset(
 						offset - 1);
 			} else {
 				// Is there no separating whitespace from the previous region?
 				// If not,
 				// then that region is the important one
 				ITextRegion previousRegion = sdRegion
 						.getRegionAtCharacterOffset(offset - 1);
 				if ((previousRegion != null)
 						&& (previousRegion != region)
 						&& (previousRegion.getTextLength() == previousRegion
 								.getLength())) {
 					region = previousRegion;
 				}
 			}
 		} else {
 			// The offset is NOT at the beginning of the region
 			if (offset > sdRegion.getStartOffset(region)
 					+ region.getTextLength()) {
 				// Is the offset within the whitespace after the text in this
 				// region?
 				// If so, use the next region
 				ITextRegion nextRegion = sdRegion
 						.getRegionAtCharacterOffset(sdRegion
 								.getStartOffset(region)
 								+ region.getLength());
 				if (nextRegion != null) {
 					region = nextRegion;
 				}
 			} else {
 				// Is the offset within the important text for this region?
 				// If so, then we've already got the right one.
 			}
 		}
 
 		// valid WHITE_SPACE region handler (#179924)
 		if ((region != null)
 				&& (region.getType() == DOMRegionContext.WHITE_SPACE)) {
 			ITextRegion previousRegion = sdRegion
 					.getRegionAtCharacterOffset(sdRegion.getStartOffset(region) - 1);
 			if (previousRegion != null) {
 				region = previousRegion;
 			}
 		}
 
 		return region;
 	}
 
 	protected String getXPathMatchString(IStructuredDocumentRegion parent,
 			ITextRegion aRegion, int offset) {
 		if ((aRegion == null) || isCloseRegion(aRegion)) {
 			return ""; //$NON-NLS-1$
 		}
 		String matchString = null;
 		String regionType = aRegion.getType();
 		if ((regionType == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS)
 				|| (regionType == DOMRegionContext.XML_TAG_OPEN)
 				|| (offset > parent.getStartOffset(aRegion)
 						+ aRegion.getTextLength())) {
 			matchString = ""; //$NON-NLS-1$
 		} else if (regionType == DOMRegionContext.XML_CONTENT) {
 			matchString = ""; //$NON-NLS-1$
 		} else {
 			if ((parent.getText(aRegion).length() > 0)
 					&& (parent.getStartOffset(aRegion) < offset)) {
 				matchString = parent.getText(aRegion).substring(0,
 						offset - parent.getStartOffset(aRegion));
 			} else {
 				matchString = ""; //$NON-NLS-1$
 			}
 		}
 		if (matchString.startsWith("\"")) {
 			matchString = matchString.substring(1);
 		}
 		return matchString;
 	}
 
 	protected boolean isCloseRegion(ITextRegion region) {
 		String type = region.getType();
 		return ((type == DOMRegionContext.XML_PI_CLOSE)
 				|| (type == DOMRegionContext.XML_TAG_CLOSE)
 				|| (type == DOMRegionContext.XML_EMPTY_TAG_CLOSE)
 				|| (type == DOMRegionContext.XML_CDATA_CLOSE)
 				|| (type == DOMRegionContext.XML_COMMENT_CLOSE)
 				|| (type == DOMRegionContext.XML_ATTLIST_DECL_CLOSE)
 				|| (type == DOMRegionContext.XML_ELEMENT_DECL_CLOSE)
 				|| (type == DOMRegionContext.XML_DOCTYPE_DECLARATION_CLOSE) || (type == DOMRegionContext.XML_DECLARATION_CLOSE));
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
 	 *      int)
 	 */
 	public IContextInformation[] computeContextInformation(ITextViewer viewer,
 			int offset) {
 		return null;
 	}
 
 	/**
 	 * Returns the characters which when entered by the user should
 	 * automatically trigger the presentation of possible completions.
 	 * 
 	 * the auto activation characters for completion proposal or
 	 * <code>null</code> if no auto activation is desired
 	 */
 	public char[] getCompletionProposalAutoActivationCharacters() {
 		//TODO: Currently these are hard coded..need to move to preferences.
 		char[] completionProposals = { '"', '\'', ':', '[', '{', '<'};
 
 		return completionProposals;
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
 	 */
 	public char[] getContextInformationAutoActivationCharacters() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
 	 */
 	public IContextInformationValidator getContextInformationValidator() {
 		return null;
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
 	 */
 	public String getErrorMessage() {
 		return errorMessage;
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.wst.sse.ui.internal.IReleasable#release()
 	 */
 	public void release() {
 
 	}
 
 	/**
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
 	 */
 	public void propertyChange(PropertyChangeEvent event) {
 		// TODO Auto-generated method stub
 
 	}
 
 	/**
 	 * Sets the error message for why content assistance didn't complete.
 	 * 
 	 * @param errorMessage
 	 */
 	public void setErrorMessage(String errorMessage) {
 		this.errorMessage = errorMessage;
 	}
 	
 	protected boolean hasAttributeAtTextRegion(String attrName, NamedNodeMap nodeMap, ITextRegion aRegion) {
 		IDOMAttr attrNode = (IDOMAttr) nodeMap.getNamedItem(attrName);
 		return attrNode != null && attrNode.getValueRegion().getStart() == aRegion.getStart();
 	}
 	
 	protected IDOMAttr getAttributeAtTextRegion(String attrName, NamedNodeMap nodeMap, ITextRegion aRegion) {
 		IDOMAttr node = (IDOMAttr) nodeMap.getNamedItem(attrName);
 		if (node != null && node.getValueRegion().getStart() == aRegion.getStart()) {
 			return node;
 		}
 		return null;
 	}
 	
 
 }
