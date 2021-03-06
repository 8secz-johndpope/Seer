 package org.jboss.resteasy.test.finegrain.client;
 
 import org.jboss.resteasy.client.ClientResponse;
 import org.jboss.resteasy.client.ClientResponseFailure;
 import org.jboss.resteasy.client.ProxyFactory;
 import org.jboss.resteasy.core.Dispatcher;
 import org.jboss.resteasy.test.EmbeddedContainer;
 import org.jboss.resteasy.test.smoke.SimpleResource;
 import org.jboss.resteasy.util.HttpResponseCodes;
 import org.junit.AfterClass;
 import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.PUT;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.Produces;
 import javax.ws.rs.QueryParam;
 import javax.ws.rs.core.Response;
 import java.net.HttpURLConnection;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.util.Map;
 
 /**
  * Simple smoke test
  *
  * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
  * @version $Revision: 1 $
  */
 public class ClientResponseTest
 {
 
    private static Dispatcher dispatcher;
 
    @Path("/")
    public interface Client
    {
       @GET
       @Path("basic")
       @Produces("text/plain")
       ClientResponse<String> getBasic();
 
       @PUT
       @Path("basic")
       @Consumes("text/plain")
       void putBasic(String body);
 
       @PUT
       @Path("basic")
       @Consumes("text/plain")
       Response.Status putBasicReturnCode(String body);
 
       @GET
       @Path("queryParam")
       @Produces("text/plain")
       ClientResponse<String> getQueryParam(@QueryParam("param")String param);
 
       @GET
       @Path("uriParam/{param}")
       @Produces("text/plain")
       ClientResponse<Integer> getUriParam(@PathParam("param")int param);
 
       @GET
       @Path("header")
       ClientResponse<Void> getHeader();
    }
 
 
    @BeforeClass
    public static void before() throws Exception
    {
       dispatcher = EmbeddedContainer.start();
       dispatcher.getRegistry().addPerRequestResource(SimpleResource.class);
    }
 
    @AfterClass
    public static void after() throws Exception
    {
       EmbeddedContainer.stop();
    }
 
    @Test
    public void testClientResponse() throws Exception
    {
       Client client = ProxyFactory.create(Client.class, "http://localhost:8081");
 
       Assert.assertEquals("basic", client.getBasic().getEntity());
       client.putBasic("hello world");
       Assert.assertEquals("hello world", client.getQueryParam("hello world").getEntity());
       Assert.assertEquals(1234, client.getUriParam(1234).getEntity().intValue());
       Assert.assertEquals(Response.Status.OK, client.putBasicReturnCode("hello world"));
       Assert.assertEquals("headervalue", client.getHeader().getHeaders().getFirst("header"));
    }
 
    @Test
    public void testErrorResponse() throws Exception
    {
       Client client = null;
       client = ProxyFactory.create(Client.class, "http://localhost:8081/shite");
       try
       {
          client.getBasic().getStatus();
       }
       catch (ClientResponseFailure e)
       {
          Assert.assertEquals(HttpResponseCodes.SC_NOT_FOUND, e.getResponse().getStatus());
          return;
       }
 
       throw new RuntimeException("Exception should have been thrown");
 
    }
 
    @Path("/redirect")
    public static class RedirectResource
    {
       @GET
       public Response get()
       {
          try
          {
             return Response.seeOther(new URI("http://localhost:8081/redirect/data")).build();
          }
          catch (URISyntaxException e)
          {
             throw new RuntimeException(e);
          }
       }
 
       @GET
       @Path("data")
       public String getData()
       {
          return "data";
       }
    }
 
    @Path("/redirect")
    public static interface RedirectClient
    {
       @GET
       ClientResponse get();
    }
 
    @Test
    public void testRedirect() throws Exception
    {
       dispatcher.getRegistry().addPerRequestResource(RedirectResource.class);
       {
          RedirectClient client = ProxyFactory.create(RedirectClient.class, "http://localhost:8081");
          ClientResponse response = client.get();
          System.out.println("size: " + response.getHeaders().size());
          for (Object name : response.getHeaders().keySet())
          {
             System.out.print(name);
             System.out.println(":" + response.getHeaders().getFirst(name.toString()));
          }
          String uri = (String) response.getHeaders().getFirst("location");
          Assert.assertEquals(uri, "http://localhost:8081/redirect/data");
       }
       System.out.println("*****");
       {
          URL url = new URL("http://localhost:8081/redirect");
          //HttpURLConnection.setFollowRedirects(false);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setInstanceFollowRedirects(false);
          conn.setRequestMethod("GET");
          Map headers = conn.getHeaderFields();
          for (Object name : headers.keySet())
          {
             System.out.println(name);
          }
          System.out.println(conn.getResponseCode());
       }
 
    }
 
 
 }
