 package org.crossref.pdfmark;
 
 import java.util.Iterator;
 
 import javax.xml.XMLConstants;
 import javax.xml.namespace.NamespaceContext;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpression;
 import javax.xml.xpath.XPathExpressionException;
 import javax.xml.xpath.XPathFactory;
 
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 public class CrossRefMetadata {
 	
 	public enum Type {
 		JOURNAL,
 		BOOK,
 		DISSERTATION,
 		CONFERENCE,
 		REPORT_PAPER,
 		OTHER,
 	}
 	
 	private static final String NAMESPACE_PREFIX = "cr";
 	private static final String NAMESPACE_URI 
 					= "http://www.crossref.org/xschema/1.0";
 	
 	private static XPathExpression TITLES_EXPR;
 	private static XPathExpression AUTHORS_EXPR;
 	private static XPathExpression GIVEN_NAME_EXPR;
 	private static XPathExpression SURNAME_EXPR;
 	private static XPathExpression DATE_EXPR;
 	private static XPathExpression DAY_EXPR;
 	private static XPathExpression MONTH_EXPR;
 	private static XPathExpression YEAR_EXPR;
 	private static XPathExpression JOURNAL_EXPR;
 	private static XPathExpression BOOK_EXPR;
 	private static XPathExpression DISSERTATION_EXPR;
 	private static XPathExpression CONFERENCE_EXPR;
 	private static XPathExpression REPORT_PAPER_EXPR;
 	
 	
 	private Document doc;
 	
 	private String[] titles, contributors;
 	
 	private String publishedDate;
 	
 	static {
 		XPathFactory factory = XPathFactory.newInstance();
 		XPath xpath = factory.newXPath();
 		xpath.setNamespaceContext(new NamespaceContext() {
 			@Override
 			public String getNamespaceURI(String prefix) {
 				if (prefix.equals(NAMESPACE_PREFIX)) {
 					return NAMESPACE_URI;
 				}
 				return XMLConstants.NULL_NS_URI;
 			}
 
 			@Override
 			public String getPrefix(String namespaceURI) {
 				if (namespaceURI.equals(NAMESPACE_URI)) {
 					return NAMESPACE_PREFIX;
 				}
 				return null;
 			}
 
 			@Override
 			public Iterator getPrefixes(String namespaceURI) {
 				return null;
 			}
 		});
 		
 		try {
 			TITLES_EXPR = xpath.compile("//cr:titles/cr:title");
 			AUTHORS_EXPR = xpath.compile("//cr:contributors/cr:person_name"
 					+ "[@contributor_role='author']");
 			GIVEN_NAME_EXPR = xpath.compile("//cr:given_name");
 			SURNAME_EXPR = xpath.compile("//cr:surname");
 			DATE_EXPR = xpath.compile("//cr:publication_date");
 			DAY_EXPR = xpath.compile("//cr:day");
 			MONTH_EXPR = xpath.compile("//cr:month");
 			YEAR_EXPR = xpath.compile("//cr:year");
			JOURNAL_EXPR = xpath.compile("//cr:journal");
			BOOK_EXPR = xpath.compile("//cr:book");
			DISSERTATION_EXPR = xpath.compile("//cr:dissertation");
			CONFERENCE_EXPR = xpath.compile("//cr:conference");
			REPORT_PAPER_EXPR = xpath.compile("//cr:report-paper");
 			
 		} catch (XPathExpressionException e) {
 			System.err.println("Error: Malformed XPath expressions.");
 			System.err.println(e);
 			System.exit(2);
 		}
 	}
 	
 	public CrossRefMetadata(Document doc) {
 		this.doc = doc;
 	}
 	
 	public String getDoi() {
 		return "";
 	}
 	
 	public Type getType() throws XPathExpressionException {
 		try {
 			if (JOURNAL_EXPR.evaluate(doc, XPathConstants.BOOLEAN)
 					.equals(Boolean.TRUE)) {
 				return Type.JOURNAL;
 			} else if ((Boolean) BOOK_EXPR.evaluate(doc, XPathConstants.BOOLEAN)
 					.equals(Boolean.TRUE)) {
 				return Type.BOOK;
 			} else if ((Boolean) DISSERTATION_EXPR.evaluate(doc, XPathConstants.BOOLEAN)
 					.equals(Boolean.TRUE)) {
 				return Type.DISSERTATION;
 			} else if ((Boolean) CONFERENCE_EXPR.evaluate(doc, XPathConstants.BOOLEAN)
 					.equals(Boolean.TRUE)) {
 				return Type.CONFERENCE;
 			} else if ((Boolean) REPORT_PAPER_EXPR.evaluate(doc, XPathConstants.BOOLEAN)
 					.equals(Boolean.TRUE)) {
 				return Type.REPORT_PAPER;
 			}
 		} catch (XPathExpressionException e) {
 			/* Do nothing. */
 		}
 		return Type.OTHER;
 	}
 
 	public String[] getTitles() throws XPathExpressionException {
 		if (titles != null) {
 			return titles;
 		}
 		
 		NodeList ts = (NodeList) TITLES_EXPR.evaluate(doc, XPathConstants.NODESET);
 
 		String[] strings = new String[ts.getLength()];
 
 		for (int i=0; i<ts.getLength(); i++) {
 			strings[i] = ts.item(i).getTextContent();
 		}
 
 		return titles = strings;
 	}
 
 	public String[] getContributors() throws XPathExpressionException {
 		if (contributors != null) {
 			return contributors;
 		}
 		
 		NodeList s = (NodeList) AUTHORS_EXPR.evaluate(doc, XPathConstants.NODESET);
 
 		String[] names = new String[s.getLength()];
 
 		for (int i=0; i<s.getLength(); i++) {
 			Node a = s.item(i);
 			Node given = (Node) GIVEN_NAME_EXPR.evaluate(a, XPathConstants.NODE);
 			Node surname = (Node) SURNAME_EXPR.evaluate(a, XPathConstants.NODE);
 			names[i] = given.getTextContent() + " " + surname.getTextContent();
 		}
 
 		return contributors = names;
 	}
 
 	public String getDate() throws XPathExpressionException {
 		if (publishedDate != null) {
 			return publishedDate;
 		}
 		
 		String date = "";
 		Node pubDate = (Node) DATE_EXPR.evaluate(doc, XPathConstants.NODE);
 
 		if (pubDate != null) {
 			Node year = (Node) YEAR_EXPR.evaluate(pubDate, XPathConstants.NODE);
 			Node month = (Node) MONTH_EXPR.evaluate(pubDate, XPathConstants.NODE);
 			Node day = (Node) DAY_EXPR.evaluate(pubDate, XPathConstants.NODE);
 
 			// TODO What if month and day are not two digits strings?
 			date = year.getTextContent();
 			if (!month.equals("")) {
 				date += "-" + month.getTextContent();
 				if (!day.equals("")) {
 					date += "-" + day.getTextContent();
 				}
 			}
 		}
 
 		return publishedDate = date;
 	}
 
 }
