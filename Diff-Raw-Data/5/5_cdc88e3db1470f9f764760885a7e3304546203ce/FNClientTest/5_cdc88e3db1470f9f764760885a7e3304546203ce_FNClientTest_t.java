 package org.basex.test.query.func;
 
 import static org.basex.query.func.Function.*;
 
 import java.io.*;
 
 import org.basex.*;
 import org.basex.core.*;
 import org.basex.core.cmd.*;
 import org.basex.query.util.*;
 import org.basex.test.query.*;
 import org.basex.test.server.*;
 import org.junit.*;
 
 /**
  * This class tests the XQuery database functions prefixed with "client".
  *
  * @author BaseX Team 2005-12, BSD License
  * @author Christian Gruen
  */
 public final class FNClientTest extends AdvancedQueryTest {
   /** Server reference. */
   private static BaseXServer server;
 
   /**
    * Starts the server.
    * @throws IOException I/O exception
    */
   @BeforeClass
   public static void start() throws IOException {
     server = createServer();
   }
 
   /**
    * Stops the server.
    * @throws IOException I/O exception
    */
   @AfterClass
   public static void stop() throws IOException {
     server.stop();
   }
 
   /**
    * Test method for the client:connect() function.
    */
   @Test
   public void clientConnect() {
     check(_CLIENT_CONNECT);
     query(connect());
     query(EXISTS.args(" " + connect()));
    error(_CLIENT_CONNECT.args(Text.LOCALHOST, 9999, Text.ADMIN, ""), Err.CLCONN);
   }
 
   /**
    * Test method for the client:execute() function.
    */
   @Test
   public void clientExecute() {
     check(_CLIENT_EXECUTE);
     contains(_CLIENT_EXECUTE.args(connect(), new ShowUsers()), Text.USERHEAD[0]);
     query("let $a := " + connect() + ", $b := " + connect() + " return (" +
         _CLIENT_EXECUTE.args("$a", new XQuery("1")) + "," +
         _CLIENT_EXECUTE.args("$b", new XQuery("2")) + ")", "1 2");
   }
 
   /**
    * Test method for the client:query() function.
    */
   @Test
   public void clientQuery() {
     check(_CLIENT_QUERY);
     contains(_CLIENT_EXECUTE.args(connect(), new ShowUsers()), Text.USERHEAD[0]);
     query("let $a := " + connect() + ", $b := " + connect() + " return " +
         _CLIENT_QUERY.args("$a", "1") + "+" + _CLIENT_QUERY.args("$b", "2"), "3");
   }
 
   /**
    * Test method for the correct return of all XDM data types.
    */
   @Test
   public void clientQueryTypes() {
     // check data types
     final Object[][] types = XdmInfoTest.TYPES;
     for(int t = 0; t < types.length; t++) {
       if(types[t] == null || types[t].length < 3) continue;
       query(_CLIENT_QUERY.args(connect(), " " + "\"" + types[t][1] + "\""), types[t][2]);
     }
   }
 
   /**
    * Test method for the client:close() function.
    */
   @Test
   public void clientClose() {
     check(_CLIENT_CLOSE);
     query(connect() + " ! " + _CLIENT_CLOSE.args(" ."));
     error(_CLIENT_CLOSE.args("xs:anyURI('unknown')"), Err.CLWHICH);
   }
 
   /**
    * Returns a successful connect string.
    * @return connect string
    */
   private static String connect() {
    return _CLIENT_CONNECT.args(Text.LOCALHOST, 9999, Text.ADMIN, Text.ADMIN);
   }
 }
