 /*
  * Copyright (c) 2002-2009 Gargoyle Software Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.gargoylesoftware.htmlunit.html;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.StringReader;
 import java.lang.reflect.InvocationTargetException;
 import java.net.URL;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Stack;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.xerces.parsers.AbstractSAXParser;
 import org.apache.xerces.util.DefaultErrorHandler;
 import org.apache.xerces.xni.Augmentations;
 import org.apache.xerces.xni.QName;
 import org.apache.xerces.xni.XMLAttributes;
 import org.apache.xerces.xni.XNIException;
 import org.apache.xerces.xni.parser.XMLInputSource;
 import org.apache.xerces.xni.parser.XMLParseException;
 import org.apache.xerces.xni.parser.XMLParserConfiguration;
 import org.cyberneko.html.HTMLConfiguration;
 import org.cyberneko.html.HTMLEventInfo;
 import org.cyberneko.html.HTMLScanner;
 import org.cyberneko.html.HTMLTagBalancingListener;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.xml.sax.Attributes;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.Locator;
 import org.xml.sax.SAXException;
 import org.xml.sax.ext.LexicalHandler;
 
 import com.gargoylesoftware.htmlunit.BrowserVersion;
 import com.gargoylesoftware.htmlunit.BrowserVersionFeatures;
 import com.gargoylesoftware.htmlunit.ObjectInstantiationException;
 import com.gargoylesoftware.htmlunit.Page;
 import com.gargoylesoftware.htmlunit.WebAssert;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebResponse;
 import com.gargoylesoftware.htmlunit.WebWindow;
 import com.gargoylesoftware.htmlunit.javascript.host.HTMLBodyElement;
 import com.gargoylesoftware.htmlunit.javascript.host.HTMLElement;
 
 /**
  * SAX parser implementation that uses the NekoHTML {@link org.cyberneko.html.HTMLConfiguration}
  * to parse HTML into a HtmlUnit-specific DOM (HU-DOM) tree.
  * <p>
  * <em>Note that the parser currently does not handle CDATA or comment sections, i.e. these
  * do not appear in the resulting DOM tree</em>
  *
  * @version $Revision$
  * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
  * @author David K. Taylor
  * @author Chris Erskine
  * @author Ahmed Ashour
  * @author Marc Guillemot
  * @author Ethan Glasser-Camp
  * @author Sudhan Moghe
  */
 public final class HTMLParser {
 
     /** XHTML namespace. */
     public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
 
     private static final Map<String, IElementFactory> ELEMENT_FACTORIES = new HashMap<String, IElementFactory>();
     private static boolean IgnoreOutsideContent_;
 
     static {
         ELEMENT_FACTORIES.put("input", InputElementFactory.instance);
 
         final DefaultElementFactory defaultElementFactory = new DefaultElementFactory();
         ELEMENT_FACTORIES.put(HtmlAbbreviated.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlAcronym.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlAnchor.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlApplet.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlAddress.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlArea.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBackgroundSound.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBase.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBaseFont.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBidirectionalOverride.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBig.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBlink.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBlockQuote.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBody.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBold.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlBreak.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlButton.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlCanvas.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlCaption.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlCenter.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlCitation.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlCode.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDefinition.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDefinitionDescription.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDeletedText.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDirectory.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDivision.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDefinitionList.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlDefinitionTerm.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlEmbed.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlEmphasis.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlFieldSet.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlFont.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlForm.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlFrame.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlFrameSet.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading1.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading2.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading3.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading4.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading5.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHeading6.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHead.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHorizontalRule.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlHtml.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlInlineFrame.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlImage.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlInsertedText.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlIsIndex.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlItalic.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlKeyboard.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlLabel.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlLegend.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlListing.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlListItem.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlLink.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlMap.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlMarquee.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlMenu.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlMeta.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlMultiColumn.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlNoBreak.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlNoEmbed.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlNoFrames.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlNoScript.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlObject.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlOrderedList.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlOptionGroup.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlOption.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlParagraph.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlParameter.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlPlainText.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlPreformattedText.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlInlineQuotation.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlS.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSample.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlScript.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSelect.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSmall.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSpacer.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSpan.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlStrike.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlStrong.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlStyle.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSubscript.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlSuperscript.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTitle.TAG_NAME, defaultElementFactory);
 
         ELEMENT_FACTORIES.put(HtmlTable.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableColumn.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableColumnGroup.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableBody.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableDataCell.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableHeaderCell.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableRow.TAG_NAME, defaultElementFactory);
 
         ELEMENT_FACTORIES.put(HtmlTextArea.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableFooter.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTableHeader.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlTeletype.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlUnderlined.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlUnorderedList.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlVariable.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlWordBreak.TAG_NAME, defaultElementFactory);
         ELEMENT_FACTORIES.put(HtmlExample.TAG_NAME, defaultElementFactory);
     }
 
     /**
      * Sets the flag to control validation of the HTML content that is outside of the
      * BODY and HTML tags. This flag is false by default to maintain compatibility with
      * current NekoHTML defaults.
      * @param ignoreOutsideContent - boolean flag to set
      */
     public static void setIgnoreOutsideContent(final boolean ignoreOutsideContent) {
         IgnoreOutsideContent_ = ignoreOutsideContent;
     }
 
     /**
      * Gets the state of the flag to ignore content outside the BODY and HTML tags.
      * @return the current state
      */
     public static boolean getIgnoreOutsideContent() {
         return IgnoreOutsideContent_;
     }
 
     /**
      * @param tagName an HTML element tag name
      * @return a factory for creating HtmlElements representing the given tag
      */
     public static IElementFactory getFactory(final String tagName) {
         final IElementFactory result = ELEMENT_FACTORIES.get(tagName);
 
         if (result != null) {
             return result;
         }
         return UnknownElementFactory.instance;
     }
 
     /**
      * You should never need to create one of these!
      */
     private HTMLParser() {
         // Empty.
     }
 
     /**
      * Parses the HTML content from the given string into an object tree representation.
      *
      * @param parent the parent for the new nodes
      * @param source the (X)HTML to be parsed
      * @throws SAXException if a SAX error occurs
      * @throws IOException if an IO error occurs
      */
     public static void parseFragment(final DomNode parent, final String source) throws SAXException, IOException {
         final HtmlPage page = (HtmlPage) parent.getPage();
         final URL url = page.getWebResponse().getRequestUrl();
 
         final HtmlUnitDOMBuilder domBuilder = new HtmlUnitDOMBuilder(parent, url);
         domBuilder.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
 
         final XMLInputSource in = new XMLInputSource(null, url.toString(), null, new StringReader(source), null);
 
         try {
             page.registerParsingStart();
             page.registerSnippetParsingStart();
             domBuilder.parse(in);
         }
         finally {
             page.registerParsingEnd();
             page.registerSnippetParsingEnd();
         }
     }
 
     /**
      * Parses the HTML content from the given WebResponse into an object tree representation.
      *
      * @param webResponse the response data
      * @param webWindow the web window into which the page is to be loaded
      * @return the page object which forms the root of the DOM tree, or <code>null</code> if the &lt;HTML&gt;
      * tag is missing
      * @throws IOException if there is an IO error
      */
     public static HtmlPage parse(final WebResponse webResponse, final WebWindow webWindow) throws IOException {
         final URL url = webResponse.getRequestUrl();
         final HtmlPage page = new HtmlPage(url, webResponse, webWindow);
         webWindow.setEnclosedPage(page);
 
         final HtmlUnitDOMBuilder domBuilder = new HtmlUnitDOMBuilder(page, url);
         final String charSet = webResponse.getContentCharSet();
 
         final InputStream content = webResponse.getContentAsStream();
         final XMLInputSource in = new XMLInputSource(null, url.toString(), null, content, charSet);
 
         try {
             page.registerParsingStart();
             domBuilder.parse(in);
         }
         catch (final XNIException e) {
             // extract enclosed exception
             final Throwable origin = extractNestedException(e);
             throw new RuntimeException("Failed parsing content from " + url, origin);
         }
         finally {
             page.registerParsingEnd();
         }
 
         addBodyToPageIfNecessary(page, true, domBuilder.body_ != null);
 
         return page;
     }
 
     /**
      * Adds a body element to the current page, if necessary. Strictly speaking, this should
      * probably be done by NekoHTML. See the bug linked below. If and when that bug is fixed,
      * we may be able to get rid of this code.
      *
      * http://sourceforge.net/tracker/index.php?func=detail&aid=1898038&group_id=195122&atid=952178
      * @param page
      * @param originalCall
      * @param checkInsideFrameOnly true if the original page had body that was removed by JavaScript
      */
     private static void addBodyToPageIfNecessary(
             final HtmlPage page, final boolean originalCall, final boolean checkInsideFrameOnly) {
         // IE waits for the whole page to load before initializing bodies for frames.
         final boolean ie = page.getWebClient().getBrowserVersion().isIE();
         if (page.getEnclosingWindow() instanceof FrameWindow && ie && originalCall) {
             return;
         }
 
         // Find out if the document already has a body element (or frameset).
         final Element doc = page.getDocumentElement();
         boolean hasBody = false;
         for (Node child = doc.getFirstChild(); child != null; child = child.getNextSibling()) {
             if (child instanceof HtmlBody || child instanceof HtmlFrameSet) {
                 hasBody = true;
                 break;
             }
         }
 
         // If the document does not have a body, add it.
         if (!hasBody && !checkInsideFrameOnly) {
             final HtmlBody body = new HtmlBody(null, "body", page, null, false);
             doc.appendChild(body);
         }
 
         // If this is IE, we need to initialize the bodies of any frames, as well.
         // This will already have been done when emulating FF (see above).
         if (ie) {
             for (final FrameWindow frame : page.getFrames()) {
                 final Page containedPage = frame.getEnclosedPage();
                 if (containedPage instanceof HtmlPage) {
                     addBodyToPageIfNecessary((HtmlPage) containedPage, false, false);
                 }
             }
         }
     }
 
     /**
      * Extract nested exception within an XNIException (Nekohtml uses reflection and generated
      * exceptions are wrapped many times within XNIException and InvocationTargetException)
      *
      * @param e the original XNIException
      * @return the cause exception
      */
     static Throwable extractNestedException(final Throwable e) {
         Throwable originalException = e;
         Throwable cause = ((XNIException) e).getException();
         while (cause != null) {
             originalException = cause;
             if (cause instanceof XNIException) {
                 cause = ((XNIException) cause).getException();
             }
             else if (cause instanceof InvocationTargetException) {
                 cause = cause.getCause();
             }
             else {
                 cause = null;
             }
         }
         return originalException;
     }
 
     /**
      * The parser and DOM builder. This class subclasses Xerces's AbstractSAXParser and implements
      * the ContentHandler interface. Thus all parser APIs are kept private. The ContentHandler methods
      * consume SAX events to build the page DOM
      */
     static final class HtmlUnitDOMBuilder extends AbstractSAXParser
             implements ContentHandler, LexicalHandler, HTMLTagBalancingListener {
         private final HtmlPage page_;
 
         private Locator locator_;
         private final Stack<DomNode> stack_ = new Stack<DomNode>();
 
         private DomNode currentNode_;
         private StringBuilder characters_;
         private boolean headParsed_ = false;
         private boolean parsingInnerHead_ = false;
         private HtmlElement body_;
         private Augmentations augmentations_;
         private HtmlForm formWaitingForLostChildren_;
         private static final String FEATURE_AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";
         private static final String FEATURE_PARSE_NOSCRIPT
             = "http://cyberneko.org/html/features/parse-noscript-content";
 
         /**
          * Calls {@link HTMLConfiguration#pushInputSource(XMLInputSource)} on the configuration.
          * @param sourceString the string to push
          */
         public void pushInputString(final String sourceString) {
             final WebResponse webResponse = page_.getWebResponse();
             final String charSet = webResponse.getContentCharSet();
             final XMLInputSource in = new XMLInputSource(null, webResponse.getRequestUrl().toString(),
                 null, new StringReader(sourceString), charSet);
             ((HTMLConfiguration) fConfiguration).evaluateInputSource(in);
         }
 
         /**
          * Creates a new builder for parsing the given response contents
          * @param webResponse the response data
          * @param webWindow the web window into which the page is to be loaded
          */
         private HtmlUnitDOMBuilder(final DomNode page, final URL url) {
             super(createConfiguration(page.getPage().getWebClient()));
             this.page_ = (HtmlPage) page.getPage();
 
             currentNode_ = page;
             stack_.push(currentNode_);
 
             final HTMLParserListener listener = page_.getWebClient().getHTMLParserListener();
             final boolean reportErrors;
             if (listener != null) {
                 reportErrors = true;
                 fConfiguration.setErrorHandler(new HTMLErrorHandler(listener, url));
             }
             else {
                 reportErrors = false;
             }
 
             try {
                 setFeature(FEATURE_AUGMENTATIONS, true);
                 setProperty("http://cyberneko.org/html/properties/names/elems", "default");
                 setFeature("http://cyberneko.org/html/features/report-errors", reportErrors);
                 setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content",
                     IgnoreOutsideContent_);
                 setFeature(FEATURE_PARSE_NOSCRIPT, !page_.getWebClient().isJavaScriptEnabled());
 
                 setContentHandler(this);
                 setLexicalHandler(this); //comments and CDATA
             }
             catch (final SAXException e) {
                 throw new ObjectInstantiationException("unable to create HTML parser", e);
             }
         }
 
         /**
          * Create the configuration depending on the simulated browser
          * @param webClient the current WebClient
          * @return the configuration
          */
         private static XMLParserConfiguration createConfiguration(final WebClient webClient) {
             final BrowserVersion browserVersion = webClient.getBrowserVersion();
             // for IE we need a special scanner that will be able to understand conditional comments
             if (browserVersion.isIE()) {
                 return new HTMLConfiguration() {
                     @Override
                     protected HTMLScanner createDocumentScanner() {
                         return new HTMLScannerForIE(browserVersion);
                     }
                 };
             }
             return new HTMLConfiguration();
         }
 
         /**
          * @return the document locator
          */
         public Locator getLocator() {
             return locator_;
         }
 
         /** {@inheritDoc ContentHandler#setDocumentLocator} */
         public void setDocumentLocator(final Locator locator) {
             locator_ = locator;
         }
 
         /** {@inheritDoc ContentHandler#startDocument()} */
         public void startDocument() throws SAXException {
         }
 
         /** {@inheritDoc ContentHandler#startElement(String,String,String,Attributes)} */
         public void startElement(
                 final String namespaceURI, final String localName,
                 final String qName, final Attributes atts)
             throws SAXException {
 
             handleCharacters();
 
             final String tagLower = localName.toLowerCase();
 
             if (page_.isParsingHtmlSnippet() && (tagLower.equals("html") || tagLower.equals("body"))) {
                 return;
             }
 
             if (parsingInnerHead_ && page_.getWebClient().getBrowserVersion().hasFeature(
                     BrowserVersionFeatures.IGNORE_CONTENTS_OF_INNER_HEAD)) {
                 return;
             }
 
             if (tagLower.equals("head")) {
                 if (headParsed_ || page_.isParsingHtmlSnippet()) {
                     parsingInnerHead_ = true;
                     return;
                 }
                 headParsed_ = true;
             }
             // add a head if none was there
             else if (!headParsed_ && (tagLower.equals("body") || tagLower.equals("frameset"))) {
                 final IElementFactory factory = getElementFactory(namespaceURI, "head");
                 final HtmlElement newElement = factory.createElement(page_, "head", null);
                 currentNode_.appendChild(newElement);
                 headParsed_ = true;
             }
             // add a <tbody> if a <tr> is directly in <table>
             else if (tagLower.equals("tr") && currentNode_.getNodeName().equals("table")) {
                 final IElementFactory factory = getElementFactory(namespaceURI, "tbody");
                 final HtmlElement newElement = factory.createElement(page_, "tbody", null);
                 currentNode_.appendChild(newElement);
                 currentNode_ = newElement;
                 stack_.push(currentNode_);
             }
 
             // If we're adding a body element, keep track of any temporary synthetic ones
             // that we may have had to create earlier (for document.write(), for example).
             HtmlBody oldBody = null;
             if (tagLower.equals("body") && page_.getBody() instanceof HtmlBody) {
                 oldBody = (HtmlBody) page_.getBody();
             }
 
             // Add the new node.
             final IElementFactory factory = getElementFactory(namespaceURI, qName);
             final HtmlElement newElement = factory.createElementNS(page_, namespaceURI, qName, atts);
             newElement.setStartLocation(locator_.getLineNumber(), locator_.getColumnNumber());
             currentNode_.appendChild(newElement);
 
             // If we had an old synthetic body and we just added a real body element, quietly
             // remove the old body and move its children to the real body element we just added.
             if (oldBody != null) {
                 oldBody.quietlyRemoveAndMoveChildrenTo(newElement);
             }
 
             if (tagLower.equals("body")) {
                 body_ = newElement;
             }
 
             currentNode_ = newElement;
             stack_.push(currentNode_);
 
         }
 
         /** {@inheritDoc} */
         @Override
         public void endElement(final QName element, final Augmentations augs)
             throws XNIException {
             // just to have local access to the augmentations. A better way?
             augmentations_ = augs;
             super.endElement(element, augs);
         }
 
         /** {@inheritDoc ContentHandler@endElement(String,String,String)} */
         public void endElement(final String namespaceURI, final String localName, final String qName)
             throws SAXException {
 
             handleCharacters();
 
             final String tagLower = localName.toLowerCase();
 
             if (page_.isParsingHtmlSnippet() && (tagLower.equals("html") || tagLower.equals("body"))) {
                 return;
             }
 
             if (parsingInnerHead_) {
                 if (tagLower.equals("head")) {
                     parsingInnerHead_ = false;
                 }
                 if (tagLower.equals("head") || page_.getWebClient().getBrowserVersion().hasFeature(
                         BrowserVersionFeatures.IGNORE_CONTENTS_OF_INNER_HEAD)) {
                     return;
                 }
             }
 
             final DomNode previousNode = stack_.pop(); //remove currentElement from stack
             previousNode.setEndLocation(locator_.getLineNumber(), locator_.getColumnNumber());
 
             // special handling for form lost children (malformed html code where </form> is synthesized)
             if (previousNode instanceof HtmlForm
                 && ((HTMLEventInfo) augmentations_.getItem(FEATURE_AUGMENTATIONS)).isSynthesized()) {
                 formWaitingForLostChildren_ = (HtmlForm) previousNode;
             }
             else if (formWaitingForLostChildren_ != null && previousNode instanceof SubmittableElement) {
                 formWaitingForLostChildren_.addLostChild((HtmlElement) previousNode);
             }
 
             // if we have added a extra node (tbody), we should remove it
             if (!currentNode_.getLocalName().equalsIgnoreCase(localName)) {
                 stack_.pop(); //remove extra node from stack
             }
 
             if (!stack_.isEmpty()) {
                 currentNode_ = stack_.peek();
             }
 
             previousNode.onAllChildrenAddedToPage(false);
 
         }
 
         /** @inheritDoc ContentHandler#characters(char,int,int) */
         public void characters(final char[] ch, final int start, final int length) throws SAXException {
             if ((characters_ == null || characters_.length() == 0)
                     && new String(ch, start, length).trim().length() == 0
                     && page_.getWebClient().getBrowserVersion().isIE()) {
 
                 DomNode node = currentNode_.getLastChild();
                 if (currentNode_ instanceof HTMLElement.ProxyDomNode) {
                     final HTMLElement.ProxyDomNode proxyNode = (HTMLElement.ProxyDomNode) currentNode_;
                     node = proxyNode.getDomNode();
                     if (!proxyNode.isAppend()) {
                         node = node.getPreviousSibling();
                         if (node == null) {
                             node = proxyNode.getDomNode().getParentNode();
                         }
                     }
                 }
                 if (removeEmptyCharacters(node)) {
                     return;
                 }
             }
             if (characters_ == null) {
                 characters_ = new StringBuilder();
             }
             characters_.append(ch, start, length);
         }
 
         private boolean removeEmptyCharacters(final DomNode node) {
             if (node != null) {
                 if (node instanceof HtmlInput) {
                     return false;
                 }
                 if (node instanceof HtmlAnchor || node instanceof HtmlSpan || node instanceof HtmlFont) {
                     final DomNode anchorChild = node.getFirstChild();
                     if (anchorChild != null) {
                         return false;
                     }
                 }
             }
             else {
                 if (currentNode_ instanceof HtmlFont) {
                     return false;
                 }
             }
             return true;
         }
 
         /** @inheritDoc ContentHandler#ignorableWhitespace(char,int,int) */
         public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
             if (characters_ == null) {
                 characters_ = new StringBuilder();
             }
             characters_.append(ch, start, length);
         }
 
         /**
          * Picks up the character data accumulated so far and add it to the current element as a text node.
          */
         private void handleCharacters() {
             if (characters_ != null && characters_.length() > 0) {
                 if (currentNode_ instanceof HtmlHtml) {
                     // In HTML, the <html> node only has two possible children:
                     // the <head> and the <body>; any text is ignored.
                     characters_.setLength(0);
                 }
                 else {
                     // Use the normal behavior: append a text node for the accumulated text.
                     final DomText text = new DomText(page_, characters_.toString());
                     characters_.setLength(0);
                     currentNode_.appendChild(text);
                 }
             }
         }
 
         /**
          * @param namespaceURI the namespace URI
          * @param qualifiedName the qualified name
          * @return the pre-registered element factory for the tag, or an UnknownElementFactory
          */
         public static IElementFactory getElementFactory(final String namespaceURI, final String qualifiedName) {
            if ((namespaceURI.length() == 0 && !qualifiedName.contains(":")) || namespaceURI.equals(XHTML_NAMESPACE)) {
                 String tagName = qualifiedName;
                 final int index = tagName.indexOf(":");
                 if (index != -1) {
                     tagName = tagName.substring(index + 1);
                 }
                 else {
                     tagName = tagName.toLowerCase();
                 }
                 final IElementFactory factory = ELEMENT_FACTORIES.get(tagName);
 
                 if (factory != null) {
                     return factory;
                 }
             }
             return UnknownElementFactory.instance;
         }
 
         /** {@inheritDoc} */
         public void endDocument() throws SAXException {
             handleCharacters();
             final DomNode currentPage = page_;
             currentPage.setEndLocation(locator_.getLineNumber(), locator_.getColumnNumber());
         }
 
         /** {@inheritDoc} */
         public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
         }
 
         /** {@inheritDoc} */
         public void endPrefixMapping(final String prefix) throws SAXException {
         }
 
         /** {@inheritDoc} */
         public void processingInstruction(final String target, final String data) throws SAXException {
         }
 
         /** {@inheritDoc} */
         public void skippedEntity(final String name) throws SAXException {
         }
 
         // LexicalHandler methods
 
         /** {@inheritDoc} */
         public void comment(final char[] ch, final int start, final int length) {
             handleCharacters();
             final String data = String.valueOf(ch, start, length);
             if (!data.startsWith("[CDATA") || !page_.getWebClient().getBrowserVersion().isIE()) {
                 final DomComment comment = new DomComment(page_, data);
                 currentNode_.appendChild(comment);
             }
         }
 
         /** {@inheritDoc} */
         public void endCDATA() {
         }
 
         /** {@inheritDoc} */
         public void endDTD() {
         }
 
         /** {@inheritDoc} */
         public void endEntity(final String name) {
         }
 
         /** {@inheritDoc} */
         public void startCDATA() {
         }
 
         /** {@inheritDoc} */
         public void startDTD(final String name, final String publicId, final String systemId) {
             final DomDocumentType type = new DomDocumentType(page_, name, publicId, systemId);
             page_.setDocumentType(type);
         }
 
         /** {@inheritDoc} */
         public void startEntity(final String name) {
         }
 
         /**
          * {@inheritDoc}
          */
         public void ignoredEndElement(final QName element, final Augmentations augs) {
             // if real </form> is reached, don't accept fields anymore as lost children
             if (formWaitingForLostChildren_ != null && "form".equals(element.localpart)) {
                 formWaitingForLostChildren_ = null;
             }
         }
 
         /**
          * {@inheritDoc}
          */
         public void ignoredStartElement(final QName elem, final XMLAttributes attrs, final Augmentations augs) {
             // when multiple body elements are encountered, the attributes of the discarded
             // elements are used when not previously defined
             if (body_ != null && "body".equalsIgnoreCase(elem.localpart) && attrs != null) {
                 // add the attributes that don't already exist
                 final int length = attrs.getLength();
                 for (int i = 0; i < length; ++i) {
                     final String attrName = attrs.getLocalName(i).toLowerCase();
                     if (body_.getAttributes().getNamedItem(attrName) == null) {
                         body_.setAttribute(attrName, attrs.getValue(i));
                         if (attrName.startsWith("on") && body_.getScriptObject() != null) {
                             final HTMLBodyElement jsBody = (HTMLBodyElement) body_.getScriptObject();
                             jsBody.createEventHandlerFromAttribute(attrName, attrs.getValue(i));
                         }
                     }
                 }
             }
         }
         /**
          * {@inheritDoc}
          */
         @Override
         public void parse(final XMLInputSource inputSource) throws XNIException, IOException {
             final HtmlUnitDOMBuilder oldBuilder = page_.getBuilder();
             page_.setBuilder(this);
             try {
                 super.parse(inputSource);
             }
             finally {
                 page_.setBuilder(oldBuilder);
             }
         }
     }
 }
 
 /**
  * Utility to transmit parsing errors to a {@link HTMLParserListener}.
  */
 class HTMLErrorHandler extends DefaultErrorHandler {
     private final HTMLParserListener listener_;
     private final URL url_;
 
     HTMLErrorHandler(final HTMLParserListener listener, final URL url) {
         WebAssert.notNull("listener", listener);
         WebAssert.notNull("url", url);
         listener_ = listener;
         url_ = url;
     }
 
     /** @see DefaultErrorHandler#error(String,String,XMLParseException) */
     @Override
     public void error(final String domain, final String key,
             final XMLParseException exception) throws XNIException {
         listener_.error(exception.getMessage(),
                 url_,
                 exception.getLineNumber(),
                 exception.getColumnNumber(),
                 key);
     }
 
     /** @see DefaultErrorHandler#warning(String,String,XMLParseException) */
     @Override
     public void warning(final String domain, final String key,
             final XMLParseException exception) throws XNIException {
         listener_.warning(exception.getMessage(),
                 url_,
                 exception.getLineNumber(),
                 exception.getColumnNumber(),
                 key);
     }
 }
 
 class HTMLScannerForIE extends org.cyberneko.html.HTMLScanner {
     HTMLScannerForIE(final BrowserVersion browserVersion) {
         fContentScanner = new ContentScannerForIE(browserVersion);
     }
 
     class ContentScannerForIE extends HTMLScanner.ContentScanner {
         private final BrowserVersion browserVersion_;
 
         ContentScannerForIE(final BrowserVersion browserVersion) {
             browserVersion_ = browserVersion;
         }
 
         @Override
         protected void scanComment() throws IOException {
             final String s = nextContent(30); // [if ...
             if (s.startsWith("[if ") && s.contains("]>")) {
                 final String condition = StringUtils.substringBefore(s.substring(4), "]>");
                 if (IEConditionalCommentExpressionEvaluator.evaluate(condition, browserVersion_)) {
                     // skip until ">"
                     skip(">", false);
                     return;
                 }
             }
             super.scanComment();
         }
     }
 }
