 /*
  * (c) 2009
  * Damian Steer <mailto:pldms@mac.com>
  */
 
 package net.rootdev.javardfa;
 
 import com.hp.hpl.jena.query.Query;
 import com.hp.hpl.jena.query.QueryExecution;
 import com.hp.hpl.jena.query.QueryExecutionFactory;
 import com.hp.hpl.jena.query.QueryFactory;
 import com.hp.hpl.jena.rdf.model.Model;
 import com.hp.hpl.jena.rdf.model.ModelFactory;
 import com.hp.hpl.jena.util.FileManager;
 import java.io.IOException;
 import java.io.InputStream;
 import javax.xml.stream.XMLInputFactory;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.XMLReaderFactory;
 
 /**
  *
  * @author pldms
  */
 public class Scratch {
 
     private static XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
 
     public static void main(String[] args) throws SAXException, IOException {
         xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
         String base = "http://www.w3.org/2006/07/SWD/RDFa/testsuite/xhtml1-testcases/";
        String testHTML = base + "0057.xhtml";
        String testSPARQL = base + "0057.sparql";
 
         check(testHTML, testSPARQL);
     }
 
     private static void check(String testHTML, String testSPARQL) throws SAXException, IOException {
         Model model = ModelFactory.createDefaultModel();
         StatementSink sink = new JenaStatementSink(model);
         InputStream in = FileManager.get().open(testHTML);
         Parser parser = new Parser(sink);
         parser.setBase(testHTML);
         XMLReader reader = XMLReaderFactory.createXMLReader();
         reader.setContentHandler(parser);
         reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
         reader.parse(new InputSource(in));
         Query theQuery = QueryFactory.read(testSPARQL);
         QueryExecution qe = QueryExecutionFactory.create(theQuery, model);
         if (qe.execAsk()) {
             System.err.println("It worked! " + testHTML);
             return;
         }
 
         System.err.println("Failed: ");
         model.write(System.err, "TTL");
         System.err.println(theQuery);
     }
 
 }
