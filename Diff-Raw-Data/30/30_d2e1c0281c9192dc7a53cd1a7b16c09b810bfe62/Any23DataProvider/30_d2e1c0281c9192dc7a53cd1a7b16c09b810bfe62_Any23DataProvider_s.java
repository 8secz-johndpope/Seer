 /**
  * 
  */
 package nl.vu.qa_for_lod.graph.impl;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.locks.ReentrantLock;
 
 import org.deri.any23.Any23;
 import org.deri.any23.extractor.ExtractionContext;
 import org.deri.any23.http.HTTPClient;
 import org.deri.any23.source.DocumentSource;
 import org.deri.any23.source.HTTPDocumentSource;
 import org.deri.any23.writer.TripleHandler;
 import org.deri.any23.writer.TripleHandlerException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.hp.hpl.jena.rdf.model.Model;
 import com.hp.hpl.jena.rdf.model.Property;
 import com.hp.hpl.jena.rdf.model.RDFNode;
 import com.hp.hpl.jena.rdf.model.Resource;
 import com.hp.hpl.jena.rdf.model.ResourceFactory;
 import com.hp.hpl.jena.rdf.model.Statement;
 import com.hp.hpl.jena.tdb.TDBFactory;
 
 import nl.vu.qa_for_lod.graph.DataProvider;
 
 /**
  * @author Christophe Guéret <christophe.gueret@gmail.com>
  * 
  */
 public class Any23DataProvider implements DataProvider {
 	protected class MyHandler implements TripleHandler {
 		private final List<Statement> buffer = new ArrayList<Statement>();
 		private final Resource resource;
 
 		/**
 		 * @param resource
 		 * @param model
 		 */
 		public MyHandler(Resource resource) {
 			this.resource = resource;
 		}
 
 		public void close() throws TripleHandlerException {
 		}
 
 		public void closeContext(ExtractionContext context) throws TripleHandlerException {
 		}
 
 		public void endDocument(org.openrdf.model.URI documentURI) throws TripleHandlerException {
 			lock.lock();
 			model.add(buffer);
 			model.commit();
 			lock.unlock();
 		}
 
 		public void openContext(ExtractionContext context) throws TripleHandlerException {
 		}
 
 		public void receiveNamespace(String prefix, String uri, ExtractionContext context)
 				throws TripleHandlerException {
 		}
 
 		public void receiveTriple(org.openrdf.model.Resource s, org.openrdf.model.URI p, org.openrdf.model.Value o,
 				org.openrdf.model.URI g, ExtractionContext context) throws TripleHandlerException {
			if (o instanceof org.openrdf.model.Resource && !(o instanceof org.openrdf.model.BNode)) {
				if (s.toString().equals(resource.getURI()) || o.toString().equals(resource.getURI())) {
					org.openrdf.model.Resource r = (org.openrdf.model.Resource) o;
					com.hp.hpl.jena.rdf.model.Resource jenaS = model.createResource(s.stringValue());
					com.hp.hpl.jena.rdf.model.Property jenaP = model.createProperty(p.stringValue());
					com.hp.hpl.jena.rdf.model.RDFNode jenaO = model.createResource(r.stringValue());
					buffer.add(model.createStatement(jenaS, jenaP, jenaO));
				}
 			}
 		}
 
 		public void setContentLength(long contentLength) {
 		}
 
 		public void startDocument(org.openrdf.model.URI documentURI) throws TripleHandlerException {
 		}
 
 	}
 	private final static Logger logger = LoggerFactory.getLogger(Any23DataProvider.class);
 	private final static Any23 runner = new Any23();
 	private final Property HAS_BLACK_LISTED = ResourceFactory.createProperty("http://example.org/blacklisted");
 	private final ReentrantLock lock = new ReentrantLock(false);
 	private final Model model;
 	private final Set<Resource> tempBlackList = new HashSet<Resource>();
 
 	private final Resource THIS = ResourceFactory.createResource("http://example.org/this");
 
 	/**
	 * @param cacheDir 
 	 * 
 	 */
 	public Any23DataProvider(String cacheDir) {
 		model = TDBFactory.createModel(cacheDir);
 		runner.setHTTPUserAgent("LATC QA tool prototype");
 		// model.removeAll(THIS, HAS_BLACK_LISTED, (RDFNode)null);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
 	 */
 	public void close() {
 		lock.lock();
 		model.close();
 		lock.unlock();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
 	 * )
 	 */
 	public Set<Statement> get(Resource resource) {
 		lock.lock();
 		Set<Statement> stmts = model.listStatements(resource, (Property) null, (RDFNode) null).toSet();
 		boolean blackListed = model.contains(THIS, HAS_BLACK_LISTED, resource);
 		lock.unlock();
 		if (stmts.size() == 0 && !blackListed && !tempBlackList.contains(resource)) {
 			boolean failed = false;
 			try {
 				HTTPClient httpClient = runner.getHTTPClient();
 				DocumentSource source = new HTTPDocumentSource(httpClient, resource.getURI());
 				TripleHandler handler = new MyHandler(resource);
 				runner.extract(source, handler);
 			} catch (Exception e) {
 				logger.warn("Failed to load " + resource.getURI());
 				failed = true;
 				tempBlackList.add(resource);
 			}
 			lock.lock();
 			stmts = model.listStatements(resource, (Property) null, (RDFNode) null).toSet();
 			if (stmts.size() == 0 && !failed)
 				model.add(THIS, HAS_BLACK_LISTED, resource);
 			lock.unlock();
 		}
 
 		return stmts;
 	}
 
 }
