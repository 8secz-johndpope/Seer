 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.update.ui.forms.internal.engine;
 
 import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.update.ui.forms.internal.*;
 import org.w3c.dom.*;
import org.xml.sax.*;
 
 /**
  * @version 	1.0
  * @author
  */
 public class TextModel implements ITextModel {
	private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

 	Vector paragraphs;
 	IHyperlinkListener urlListener;
 	IHyperlinkSegment[] hyperlinks;
 	int selectedLinkIndex = -1;
 	HyperlinkSettings hyperlinkSettings;
 	
 	public TextModel() {
 		reset();
 	}
 
 	class LocalHyperlinkSettings extends HyperlinkSettings {
 	}
 
 	/*
 	 * @see ITextModel#getParagraphs()
 	 */
 	public IParagraph[] getParagraphs() {
 		if (paragraphs == null)
 			return new IParagraph[0];
 		return (IParagraph[]) paragraphs.toArray(
 			new IParagraph[paragraphs.size()]);
 	}
 	
 	public String getAccessibleText() {
 		if (paragraphs == null)
 			return "";
 		StringBuffer sbuf = new StringBuffer();
 		for (int i=0; i<paragraphs.size(); i++) {
 			IParagraph paragraph  = (IParagraph)paragraphs.get(i);
 			String text = paragraph.getAccessibleText();
 			sbuf.append(text);
 		}
 		return sbuf.toString();
 	}
 
 	/*
 	 * @see ITextModel#parse(String)
 	 */
 	public void parseTaggedText(String taggedText, boolean expandURLs)
 		throws CoreException {
 		if (taggedText==null) {
 			reset();
 			return;
 		}
 		try {
 			InputStream stream =
 				new ByteArrayInputStream(taggedText.getBytes("UTF8"));
 			parseInputStream(stream, expandURLs);
 		} catch (UnsupportedEncodingException e) {
 		}
 	}
 
 	public void parseInputStream(InputStream is, boolean expandURLs)
 		throws CoreException {
			
		documentBuilderFactory.setNamespaceAware(true);
	
 		reset();
 		try {
			DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
 			InputSource source = new InputSource(is);
			Document doc = parser.parse(source);
			processDocument(doc, expandURLs);
		} catch (ParserConfigurationException e) {
			FormsPlugin.logException(e);
 		} catch (SAXException e) {
 			FormsPlugin.logException(e);
 		} catch (IOException e) {
 			FormsPlugin.logException(e);
 		}
 	}
 
 	private void processDocument(Document doc, boolean expandURLs) {
 		Node root = doc.getDocumentElement();
 		NodeList children = root.getChildNodes();
 		for (int i = 0; i < children.getLength(); i++) {
 			Node child = children.item(i);
 			if (child.getNodeType() == Node.TEXT_NODE) {
 				// Make an implicit paragraph
 				String text = child.getNodeValue();
 				if (text != null && !isIgnorableWhiteSpace(text, true)) {
 					Paragraph p = new Paragraph(true);
 					p.parseRegularText(
 						text,
 						expandURLs,
 						getHyperlinkSettings(),
 						null);
 					paragraphs.add(p);
 				}
 			} else if (child.getNodeType() == Node.ELEMENT_NODE) {
 				String tag = child.getNodeName().toLowerCase();
 				if (tag.equals("p")) {
 					IParagraph p = processParagraph(child, expandURLs);
 					if (p != null)
 						paragraphs.add(p);
 				} else if (tag.equals("li")) {
 					IParagraph p = processListItem(child, expandURLs);
 					if (p != null)
 						paragraphs.add(p);
 				}
 			}
 		}
 	}
 	private IParagraph processParagraph(Node paragraph, boolean expandURLs) {
 		NodeList children = paragraph.getChildNodes();
 		NamedNodeMap atts = paragraph.getAttributes();
 		Node addSpaceAtt = atts.getNamedItem("addVerticalSpace");
 		boolean addSpace = true;
 
 		if (addSpaceAtt != null) {
 			String value = addSpaceAtt.getNodeValue();
 			addSpace = value.equalsIgnoreCase("true");
 		}
 		Paragraph p = new Paragraph(addSpace);
 
 		processSegments(p, children, expandURLs);
 		return p;
 	}
 	private IParagraph processListItem(Node listItem, boolean expandURLs) {
 		NodeList children = listItem.getChildNodes();
 		NamedNodeMap atts = listItem.getAttributes();
 		Node addSpaceAtt = atts.getNamedItem("addVerticalSpace");
 		Node styleAtt = atts.getNamedItem("style");
 		Node valueAtt = atts.getNamedItem("value");
 		Node indentAtt = atts.getNamedItem("indent");
 		int style = IBulletParagraph.CIRCLE;
 		int indent = -1;
 		String text = null;
 		boolean addSpace = true;
 
 		if (addSpaceAtt != null) {
 			String value = addSpaceAtt.getNodeValue();
 			addSpace = value.equalsIgnoreCase("true");
 		}
 		if (styleAtt != null) {
 			String value = styleAtt.getNodeValue();
 			if (value.equalsIgnoreCase("text")) {
 				style = IBulletParagraph.TEXT;
 			} else if (value.equalsIgnoreCase("image")) {
 				style = IBulletParagraph.IMAGE;
 			}
 		}
 		if (valueAtt != null) {
 			text = valueAtt.getNodeValue();
 		}
 		if (indentAtt != null) {
 			String value = indentAtt.getNodeValue();
 			try {
 				indent = Integer.parseInt(value);
 			} catch (NumberFormatException e) {
 			}
 		}
 
 		BulletParagraph p = new BulletParagraph(addSpace);
 		p.setIndent(indent);
 		p.setBulletStyle(style);
 		p.setBulletText(text);
 
 		processSegments(p, children, expandURLs);
 		return p;
 	}
 
 	private void processSegments(
 		Paragraph p,
 		NodeList children,
 		boolean expandURLs) {
 		for (int i = 0; i < children.getLength(); i++) {
 			Node child = children.item(i);
 			IParagraphSegment segment = null;
 
 			if (child.getNodeType() == Node.TEXT_NODE) {
 				String value = child.getNodeValue();
 
 				if (value != null && !isIgnorableWhiteSpace(value, false)) {
 					p.parseRegularText(
 						value,
 						expandURLs,
 						getHyperlinkSettings(),
 						null);
 				}
 			} else if (child.getNodeType() == Node.ELEMENT_NODE) {
 				String name = child.getNodeName();
 				if (name.equalsIgnoreCase("img")) {
 					segment = processImageSegment(child);
 				} else if (name.equalsIgnoreCase("a")) {
 					segment =
 						processHyperlinkSegment(child, getHyperlinkSettings());
 				} else if (name.equalsIgnoreCase("text")) {
 					processTextSegment(p, expandURLs, child);
 				} else if (name.equalsIgnoreCase("b")) {
 					String text = getNodeText(child).trim();
 					String fontId = JFaceResources.BANNER_FONT;
 					p.parseRegularText(
 						text,
 						expandURLs,
 						getHyperlinkSettings(),
 						fontId);
 				}
 			}
 			if (segment != null) {
 				p.addSegment(segment);
 			}
 		}
 	}
 
 	private boolean isIgnorableWhiteSpace(String text, boolean ignoreSpaces) {
 		for (int i = 0; i < text.length(); i++) {
 			char c = text.charAt(i);
 			if (ignoreSpaces && c == ' ')
 				continue;
 			if (c == '\n' || c == '\r' || c == '\f')
 				continue;
 			return false;
 		}
 		return true;
 	}
 
 	private IParagraphSegment processImageSegment(Node image) {
 		ImageSegment segment = new ImageSegment();
 		NamedNodeMap atts = image.getAttributes();
 		Node id = atts.getNamedItem("href");
 		Node align = atts.getNamedItem("align");
 		if (id != null) {
 			String value = id.getNodeValue();
 			segment.setObjectId(value);
 		}
 		if (align != null) {
 			String value = align.getNodeValue().toLowerCase();
 			if (value.equals("top"))
 				segment.setVerticalAlignment(ImageSegment.TOP);
 			else if (value.equals("middle"))
 				segment.setVerticalAlignment(ImageSegment.MIDDLE);
 			else if (value.equals("bottom"))
 				segment.setVerticalAlignment(ImageSegment.BOTTOM);
 		}
 		return segment;
 	}
 
 	private String getNodeText(Node node) {
 		NodeList children = node.getChildNodes();
 		String text = "";
 		for (int i = 0; i < children.getLength(); i++) {
 			Node child = children.item(i);
 			if (child.getNodeType() == Node.TEXT_NODE) {
 				text += child.getNodeValue();
 			}
 		}
 		return text;
 	}
 
 	private IParagraphSegment processHyperlinkSegment(
 		Node link,
 		HyperlinkSettings settings) {
 		String text = getNodeText(link);
 		HyperlinkSegment segment = new HyperlinkSegment(text, settings, null);
 		NamedNodeMap atts = link.getAttributes();
 		Node href = atts.getNamedItem("href");
 		if (href != null) {
 			String value = href.getNodeValue();
 			segment.setActionId(value);
 		}
 		Node arg = atts.getNamedItem("arg");
 		if (arg != null) {
 			String value = arg.getNodeValue();
 			segment.setArg(value);
 		}
 		return segment;
 	}
 
 	private void processTextSegment(
 		Paragraph p,
 		boolean expandURLs,
 		Node textNode) {
 		String text = getNodeText(textNode).trim();
 
 		NamedNodeMap atts = textNode.getAttributes();
 		Node font = atts.getNamedItem("font");
 		String fontId = null;
 		if (font != null) {
 			fontId = font.getNodeValue();
 		}
 		p.parseRegularText(text, expandURLs, getHyperlinkSettings(), fontId);
 	}
 
 	public void parseRegularText(String regularText, boolean convertURLs)
 		throws CoreException {
 		reset();
 		
 		if (regularText==null) return;
 
 		Paragraph p = new Paragraph(true);
 		paragraphs.add(p);
 		int pstart = 0;
 
 		for (int i = 0; i < regularText.length(); i++) {
 			char c = regularText.charAt(i);
 			if (p == null) {
 				p = new Paragraph(true);
 				paragraphs.add(p);
 			}
 			if (c == '\n') {
 				String text = regularText.substring(pstart, i);
 				pstart = i + 1;
 				p.parseRegularText(
 					text,
 					convertURLs,
 					getHyperlinkSettings(),
 					null);
 				p = null;
 			}
 		}
 		if (p != null) {
 			// no new line
 			String text = regularText.substring(pstart);
 			p.parseRegularText(text, convertURLs, getHyperlinkSettings(), null);
 		}
 	}
 
 	public HyperlinkSettings getHyperlinkSettings() {
 		if (hyperlinkSettings == null) {
 			hyperlinkSettings = new LocalHyperlinkSettings();
 		}
 		return hyperlinkSettings;
 	}
 
 	public void setHyperlinkSettings(HyperlinkSettings settings) {
 		this.hyperlinkSettings = settings;
 	}
 
 	public void setURLListener(IHyperlinkListener urlListener) {
 		this.urlListener = urlListener;
 	}
 
 	private void reset() {
 		if (paragraphs == null)
 			paragraphs = new Vector();
 		paragraphs.clear();
 		selectedLinkIndex = -1;
 		hyperlinks = null;
 	}
 
 	IHyperlinkSegment[] getHyperlinks() {
 		if (hyperlinks != null || paragraphs == null)
 			return hyperlinks;
 		Vector result = new Vector();
 		for (int i = 0; i < paragraphs.size(); i++) {
 			IParagraph p = (IParagraph) paragraphs.get(i);
 			IParagraphSegment[] segments = p.getSegments();
 			for (int j = 0; j < segments.length; j++) {
 				if (segments[j] instanceof IHyperlinkSegment)
 					result.add(segments[j]);
 			}
 		}
 		hyperlinks =
 			(IHyperlinkSegment[]) result.toArray(
 				new IHyperlinkSegment[result.size()]);
 		return hyperlinks;
 	}
 
 	public IHyperlinkSegment findHyperlinkAt(int x, int y) {
 		IHyperlinkSegment[] links = getHyperlinks();
 		for (int i = 0; i < links.length; i++) {
 			if (links[i].contains(x, y))
 				return links[i];
 		}
 		return null;
 	}
 	public ITextSegment findSegmentAt(int x, int y) {
 		for (int i = 0; i < paragraphs.size(); i++) {
 			IParagraph p = (IParagraph) paragraphs.get(i);
 			ITextSegment segment = p.findSegmentAt(x, y);
 			if (segment != null)
 				return segment;
 		}
 		return null;
 	}
 
 	public IHyperlinkSegment getSelectedLink() {
 		if (selectedLinkIndex == -1)
 			return null;
 		return hyperlinks[selectedLinkIndex];
 	}
 
 	public boolean traverseLinks(boolean next) {
 		IHyperlinkSegment[] links = getHyperlinks();
 		if (links == null)
 			return false;
 		int size = links.length;
 		if (next) {
 			selectedLinkIndex++;
 		} else
 			selectedLinkIndex--;
 
 		if (selectedLinkIndex < 0 || selectedLinkIndex > size - 1) {
 			selectedLinkIndex = -1;
 		}
 		return selectedLinkIndex != -1;
 	}
 
 	public void selectLink(IHyperlinkSegment link) {
 		if (link == null)
 			selectedLinkIndex = -1;
 		else {
 			IHyperlinkSegment[] links = getHyperlinks();
 			selectedLinkIndex = -1;
 			if (links == null)
 				return;
 			for (int i = 0; i < links.length; i++) {
 				if (links[i].equals(link)) {
 					selectedLinkIndex = i;
 					break;
 				}
 			}
 		}
 	}
 
 	public boolean hasFocusSegments() {
 		IHyperlinkSegment[] links = getHyperlinks();
 		if (links.length > 0)
 			return true;
 		return false;
 	}
 
 	public void dispose() {
 		if (hyperlinkSettings instanceof LocalHyperlinkSettings)
 			hyperlinkSettings.dispose();
 	}
 }
