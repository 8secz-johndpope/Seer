 package org.basex.examples.server;
 
 import org.basex.BaseXServer;
 import org.basex.server.ClientSession;
 import org.basex.server.ClientQuery;
 
 /**
  * This class demonstrates query execution via the client/server architecture.
  *
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author BaseX Team
  */
 public final class ServerQueryExample {
   /** Session reference. */
   private static ClientSession session;
 
   /** Private constructor. */
   private ServerQueryExample() { }
 
   /**
    * Runs the example code.
    * @param args (ignored) command-line arguments
    * @throws Exception exception
    */
   public static void main(final String[] args) throws Exception {
 
     System.out.println("=== ServerQueryExample ===");
 
     // ------------------------------------------------------------------------
     // Start server on default port 1984.
     BaseXServer server = new BaseXServer();
 
     // ------------------------------------------------------------------------
     // Create a client session with host name, port, user name and password
     System.out.println("\n* Create a client session.");
 
     session = new ClientSession("localhost", 1984, "admin", "admin");
 
     // ------------------------------------------------------------------------
     // Run a query
     System.out.println("\n* Run a query:");
 
     System.out.println(session.execute("XQUERY 1"));
 
     // ------------------------------------------------------------------------
     // Run a query, specifying an output stream
     System.out.println("\n* Run a query (faster):");
 
     session.execute("XQUERY 1 to 2", System.out);
 
     // ------------------------------------------------------------------------
     // Iteratively run a query
     System.out.println("\n\n* Iterate a query:");
 
     // Create query instance
     ClientQuery query = session.query("1 to 3");
 
     // Loop through all results
     while(query.more()) {
       System.out.print(query.next());
     }
 
     // close iterator
     query.close();
 
     // ------------------------------------------------------------------------
     // Iteratively run a query
     System.out.println("\n\n* Iterate a query (faster):");
 
     // Create query instance
     query = session.query("1 to 4");
 
    // Loop through all results
     while(query.more()) {
       query.next(System.out);
     }
 
     // close iterator
     query.close();
 
     // ------------------------------------------------------------------------
     // Close the client session
     System.out.println("\n\n* Close the client session.");
 
     session.close();
 
     // ------------------------------------------------------------------------
     // Stop the server
     System.out.println("\n* Stop the server.");
 
     server.stop();
   }
 }
