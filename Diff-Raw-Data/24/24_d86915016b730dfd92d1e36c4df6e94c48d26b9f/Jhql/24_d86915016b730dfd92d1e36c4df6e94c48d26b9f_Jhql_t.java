 package org.github.wks.jhql;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Reader;
 import java.util.Collections;
 import java.util.Map;
 
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.github.wks.jhql.factory.JhqlJsonGrammarException;
 import org.github.wks.jhql.factory.JsonQueryerFactory;
 import org.github.wks.jhql.factory.JsonException;
 import org.github.wks.jhql.query.Queryer;
 import org.htmlcleaner.HtmlCleaner;
 import org.htmlcleaner.DomSerializer;
 import org.w3c.dom.Node;
 
 public class Jhql {
 	private static final Map<String, Object> emptyMap = Collections.emptyMap();
 
 	private HtmlCleaner htmlCleaner = new HtmlCleaner();
 	private DomSerializer domSerializer = new DomSerializer(
 			htmlCleaner.getProperties());
 
 	/**
 	 * Get the underlying HtmlCleaner. Enables configuration.
 	 * 
 	 * @return the underlying HtmlCleaner.
 	 */
 	public HtmlCleaner getHtmlCleaner() {
 		return htmlCleaner;
 	}
 
 	public void setHtmlCleaner(HtmlCleaner htmlCleaner) {
 		this.htmlCleaner = htmlCleaner;
 	}
 
 	/**
 	 * Get the underlying DomSerializer. Enables configuration.
 	 * 
 	 * @return the underlying DomSerializer.
 	 */
 	public DomSerializer getDomSerializer() {
 		return domSerializer;
 	}
 
 	public void setDomSerializer(DomSerializer domSerializer) {
 		this.domSerializer = domSerializer;
 	}
 
 	/**
 	 * Make a Queryer from a File containing a JSON value.
 	 */
 	public Queryer makeQueryer(File json) throws JsonException,
 			JhqlJsonGrammarException {
 		return JsonQueryerFactory.makeQueryer(json);
 	}
 
 	/**
 	 * Make a Queryer from a Reader containing a JSON value.
 	 */
 	public Queryer makeQueryer(Reader json) throws JsonException,
 			JhqlJsonGrammarException {
 		return JsonQueryerFactory.makeQueryer(json);
 	}
 
 	/**
 	 * Make a Queryer from an InputStream containing a JSON value.
 	 */
 	public Queryer makeQueryer(InputStream json) throws JsonException,
 			JhqlJsonGrammarException {
 		return JsonQueryerFactory.makeQueryer(json);
 	}
 
 	/**
 	 * Make a Queryer from an String containing a JSON value.
 	 */
 	public Queryer makeQueryer(String json) throws JsonException,
 			JhqlJsonGrammarException {
 		return JsonQueryerFactory.makeQueryer(json);
 	}
 
 	/**
 	 * Convert a HTML document into a W3C Dom tree.
 	 */
	public Node htmlToDom(String html) throws JhqlException {
 		try {
 			return domSerializer.createDOM(htmlCleaner.clean(html));
 		} catch (ParserConfigurationException e) {
 			throw new JhqlException(e);
 		}
 	}
 
 	/**
 	 * Convert a HTML document into a W3C Dom tree.
 	 */
	public Node htmlToDom(File html) throws JhqlException, IOException {
 		try {
 			return domSerializer.createDOM(htmlCleaner.clean(html));
 		} catch (ParserConfigurationException e) {
 			throw new JhqlException(e);
 		}
 	}
 
 	/**
 	 * Convert a HTML document into a W3C Dom tree.
 	 */
	public Node htmlToDom(InputStream html) throws JhqlException, IOException {
 		try {
 			return domSerializer.createDOM(htmlCleaner.clean(html));
 		} catch (ParserConfigurationException e) {
 			throw new JhqlException(e);
 		}
 	}
 
 	/**
 	 * Convert a HTML document into a W3C Dom tree.
 	 * 
 	 * @param encoding
 	 *            The encoding of the HTML InputStream.
 	 */
 	public Node htmlToDom(InputStream html, String encoding)
 			throws JhqlException, IOException {
 		try {
 			return domSerializer.createDOM(htmlCleaner.clean(html, encoding));
 		} catch (ParserConfigurationException e) {
 			throw new JhqlException(e);
 		}
 	}
 
 	/**
 	 * Convert a HTML document into a W3C Dom tree.
 	 */
	public Node htmlToDom(Reader html) throws JhqlException, IOException {
 		try {
 			return domSerializer.createDOM(htmlCleaner.clean(html));
 		} catch (ParserConfigurationException e) {
 			throw new JhqlException(e);
 		}
 	}
 
 	/**
 	 * Make query on an HTML document.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, File html) throws IOException {
 		return queryer.query(htmlToDom(html), emptyMap);
 	}
 
 	/**
 	 * Make query on an HTML document.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, String html) {
 		return queryer.query(htmlToDom(html), emptyMap);
 	}
 
 	/**
 	 * Make query on an HTML document.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, InputStream html)
			throws IOException {
 		return queryer.query(htmlToDom(html), emptyMap);
 	}
 
 	/**
 	 * Make query on an HTML document.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, InputStream html, String encoding)
			throws IOException {
 		return queryer.query(htmlToDom(html, encoding), emptyMap);
 	}
 
 	/**
 	 * Make query on an HTML document.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, Reader html) throws IOException {
 		return queryer.query(htmlToDom(html), emptyMap);
 	}
 
 	/**
 	 * Make query on an HTML document within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, File html,
 			Map<String, Object> context) throws IOException {
 		return queryer.query(htmlToDom(html), context);
 	}
 
 	/**
 	 * Make query on an HTML document within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, String html,
 			Map<String, Object> context) {
 		return queryer.query(htmlToDom(html), context);
 	}
 
 	/**
 	 * Make query on an HTML document within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, InputStream html,
 			Map<String, Object> context) throws IOException {
 		return queryer.query(htmlToDom(html), context);
 	}
 
 	/**
 	 * Make query on an HTML document within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, InputStream html, String encoding,
 			Map<String, Object> context) throws IOException {
 		return queryer.query(htmlToDom(html), context);
 	}
 
 	/**
 	 * Make query on an HTML document within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, Reader html,
 			Map<String, Object> context) throws IOException {
 		return queryer.query(htmlToDom(html), context);
 	}
 
 	/**
 	 * Make query on a DOM Tree.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
	public Object queryHtml(Queryer queryer, Node rootNode) {
 		return queryer.query(rootNode, emptyMap);
 	}
 
 	/**
 	 * Make query on a DOM Tree within a specified context.
 	 * 
 	 * @see {@link org.github.wks.jhql.query.Queryer}
 	 */
 	public Object queryHtml(Queryer queryer, Node rootNode,
			Map<String, Object> context) {
 		return queryer.query(rootNode, context);
 	}
 
 }
