 package org.basex.api.jaxrx;
 
 import static org.basex.util.Token.*;
 import static org.jaxrx.constants.URLConstants.*;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Set;
 import javax.ws.rs.WebApplicationException;
 import javax.ws.rs.core.Response;
 import javax.ws.rs.core.StreamingOutput;
 import javax.xml.transform.sax.SAXSource;
 import org.basex.build.MemBuilder;
 import org.basex.build.Parser;
 import org.basex.build.xml.SAXWrapper;
 import org.basex.core.Context;
 import org.basex.core.proc.CreateDB;
 import org.basex.core.proc.DropDB;
 import org.basex.core.proc.List;
 import org.basex.core.proc.Open;
 import org.basex.data.XMLSerializer;
 import org.basex.query.QueryProcessor;
 import org.basex.query.item.Item;
 import org.basex.query.iter.Iter;
 import org.jaxrx.constants.EURLParameter;
 import org.jaxrx.interfaces.IDelete;
 import org.jaxrx.interfaces.IGet;
 import org.jaxrx.interfaces.IPost;
 import org.jaxrx.interfaces.IPut;
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.InputSource;
 
 /**
  * This class is responsible to interact between the REST layer implemented via
  * JAX-RX and the BaseX database. Hence, this class gets HTTP request, evaluates
  * it and send an appropriate response afterwards.
  * 
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author Lukas Lewandowski
  */
 public final class BaseXREST implements IGet, IPost, IDelete, IPut {
  
   // /**
   // * This field the begin result element of a XQuery or XPath expression.
   // */
   // private final transient String beginResult =
   // "<jaxrx:result xmlns:jaxrx=\"http://jaxrx.org/\">";
   //
   // /**
   // * This field the end result element of a XQuery or XPath expression.
   // */
   // private final transient String endResult = "</jaxrx:result>";
 
   @Override
   public Set<EURLParameter> getAvailableParams() {
     final Set<EURLParameter> params = new HashSet<EURLParameter>();
     //params.add(EURLParameter.COMMAND);
     params.add(EURLParameter.COUNT);
     params.add(EURLParameter.OUTPUT);
     params.add(EURLParameter.QUERY);
     params.add(EURLParameter.START);
     params.add(EURLParameter.WRAP);
     return params;
   }
 
   @Override
   public StreamingOutput getResource(final String resource,
       final Map<EURLParameter, String> params) {
 
     return new StreamingOutput() {
       @Override
       public void write(final OutputStream out) {
         final Context ctx = new Context();
         if(new Open(resource).exec(ctx)) {
           query(params.get(EURLParameter.QUERY), out, ctx,
                 params.get(EURLParameter.WRAP),
                 params.get(EURLParameter.OUTPUT),
                 params.get(EURLParameter.START),
                 params.get(EURLParameter.COUNT));
         }
         ctx.close();
       }
     };
   }
 
   @Override
   public StreamingOutput getResourcesNames() {
     return new StreamingOutput() {
       @Override
       public void write(final OutputStream out) {
         final Context ctx = new Context();
         try {
           final XMLSerializer xml = new XMLSerializer(out);
           xml.openElement(token(JAXRX + ":results"));
           xml.namespace(token(JAXRX), token(URL));
 
           for(final String db : List.list(ctx)) {
             if(new Open(db).exec(ctx)) {
               xml.emptyElement(token(JAXRX + ":" +
                   (ctx.current.size() > 1 ? "collection" : "resource")),
                   token("name"), token(db));
             }
           }
           xml.closeElement();
           xml.close();
         } catch(final Exception ex) {
           // catch all kind of exceptions to get sure an exception is returned 
           throw new WebApplicationException(ex);
         } finally {
           // always close context
           try { ctx.close(); } catch(final Exception ex) { }
         }
       }
     };
   }
 
   @Override
   public Response postResource(final String resource, final Object input,
       final boolean isQuery) {
 
     StreamingOutput so = null;
     if(isQuery) {
       final Document pvDoc = (Document) input;
       so = new StreamingOutput() {
         @Override
         public void write(final OutputStream out) {
           final Context ctx = new Context();
           if(!new Open(resource).exec(ctx))
             throw new WebApplicationException(Response.Status.NOT_FOUND);
 
           final Map<String, String> params = getParams(pvDoc);
           query(params.get("query"), out, ctx,
                 params.get("wrap"),
                 params.get("output"),
                 params.get("start"),
                 params.get("count"));
           ctx.close();
         }
       };
     } else {
       final InputStream inputStream = (InputStream) input;
       final Context ctx = new Context();
       try {
         if(new Open(resource).exec(ctx)) {
           final SAXSource saxSource = new SAXSource(
               new InputSource(inputStream));
           final Parser parser = new SAXWrapper(saxSource, ctx.prop);
           ctx.data.insert(ctx.data.meta.size, -1,
               new MemBuilder(parser).build(new Date() + ".xml"));
         }
       } catch(final IOException exce) {
         throw new WebApplicationException(exce);
       } finally {
         // always close open references
         try { ctx.close(); } catch(final Exception ex) { }
       }
     }
     return Response.ok(so).build();
   }
 
   @Override
   public boolean deleteResource(final String resource) {
     final Context ctx = new Context();
     final boolean deleted = new DropDB(resource).exec(ctx);
     ctx.close();
     return deleted;
   }
 
   @Override
   public boolean createResource(final String resource,
       final InputStream input) {
 
     final Context ctx = new Context();
     final SAXSource saxSource = new SAXSource(new InputSource(input));
     final Parser parser = new SAXWrapper(saxSource, ctx.prop);
     try {
       CreateDB.xml(ctx, parser, resource);
       return true;
     } catch(final IOException exce) {
       throw new WebApplicationException(exce);
     } finally {
       // always close open references
       try { ctx.close(); } catch(final Exception ex) { }
     }
   }
 
   /**
    * This method executes the XQuery expression within BaseX.
    * @param query The XQuery expression as {@link String}
    * @param out The {@link OutputStream} that writes the result of the query
    * @param ctx The context
    * @param wrap The optional surrounding result element (needed for a
    *          XML fragment)
    * @param serialize serialization parameters
    * @param start The start value
    * @param count The maximum value of results
    * @throws WebApplicationException The exception occurred
    */
   protected void query(final String query, final OutputStream out,
       final Context ctx, final String wrap, final String serialize,
       final String start, final String count) {
 
     final String xquery = query != null ? query : ".";
     final int s = start != null ? Integer.valueOf(start) : 0;
     final int m = count != null ? Integer.valueOf(count) :
       Integer.MAX_VALUE - s;
 
     // set serialization parameters
     final Properties props = new Properties();
     if(wrap != null && Boolean.parseBoolean(wrap)) {
       props.setProperty("wrap-prefix", JAXRX);
       props.setProperty("wrap-uri", URL);
     }
    // missing... feedback for incorrect parameters
     if(serialize != null) {
       for(final String ser : serialize.split(",")) {
        final String[] sprop = ser.split(":");
        props.setProperty(sprop[0], sprop[1]);
       }
     }
 
     QueryProcessor proc = null;
     try {
       XMLSerializer xml = new XMLSerializer(out, props);
       proc = new QueryProcessor(xquery, ctx);
 
       final Iter iter = proc.iter();
       for(int c = 0; c < s + m; c++) {
         final Item it = iter.next();
         if(it == null) break;
         if(c < s) continue;
         xml.openResult();
         it.serialize(xml);
         xml.closeResult();
       }
       xml.close();
     } catch(final Exception ex) {
       // catch all kind of exceptions to get sure an exception is returned 
       throw new WebApplicationException(ex);
     } finally {
       // always close open references
       try { if(proc != null) proc.close(); } catch(final Exception ex) { }
     }
   }
 
   /**
    * This method extracts and returns query parameters from a document.
    * @param doc The XML {@link Document} containing the XQuery XML post
    *          request.
    * @return The XQuery expression as {@link String} and optional properties.
    * @throws WebApplicationException The exception occurred.
    */
   protected Map<String, String> getParams(final Document doc) {
     final Map<String, String> params = new HashMap<String, String>();
 
     params.put("query",
         doc.getElementsByTagName("text").item(0).getTextContent());
 
     final NodeList props = doc.getElementsByTagName("property");
     for(int i = 0; i < props.getLength(); i++) {
       final Node prop = props.item(i);
       params.put(
           prop.getAttributes().getNamedItem("name").getTextContent(),
           prop.getAttributes().getNamedItem("value").getTextContent());
     }
     return params;
   }
 }
