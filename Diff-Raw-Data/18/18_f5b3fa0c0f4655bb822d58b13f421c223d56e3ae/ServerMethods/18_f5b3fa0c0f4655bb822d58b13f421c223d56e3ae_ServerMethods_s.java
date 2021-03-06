 /* Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *     http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.robotframework.remoteserver;
 
 import java.io.ByteArrayOutputStream;
 import java.io.PrintStream;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.apache.commons.lang.exception.ExceptionUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 /**
  * Contains the XML-RPC methods that implement the remote library interface.
  * 
  * @author David Luu
  * 
  */
 public class ServerMethods {
 
     private Log log;
 
     public ServerMethods() {
 	log = LogFactory.getLog(ServerMethods.class);
     }
 
     /**
      * Get an array containing the names of the keywords that the library implements.
      * 
      * @return String array containing keyword names in the library
      */
     public String[] get_keyword_names() {
 	try {
 	    String[] names = Context.getLibrary().getKeywordNames();
 	    if (names == null || names.length == 0)
 		throw new RuntimeException("No keywords found in the test library");
 	    String[] newNames = Arrays.copyOf(names, names.length + 1);
 	    newNames[names.length] = "stop_remote_server";
 	    return newNames;
 	} catch (Throwable e) {
 	    log.warn("", e);
 	    throw new RuntimeException(e);
 	}
     }
 
     /**
      * Run the given keyword and return the results.
      * 
      * @param keyword
      *            keyword to run
      * @param args
      *            arguments packed in an array to pass to the keyword method
      * @return remote result Map containing the execution results
      */
     public Map<String, Object> run_keyword(String keyword, Object[] args) {
 	HashMap<String, Object> kr = new HashMap<String, Object>();
 	PrintStream outBackup = System.out;
 	ByteArrayOutputStream baos = new ByteArrayOutputStream();
 	System.setOut(new PrintStream(baos));
 	try {
 	    kr.put("status", "PASS");
 	    kr.put("error", "");
 	    kr.put("traceback", "");
 	    Object retObj = "";
 	    if (keyword.equalsIgnoreCase("stop_remote_server")) {
 		retObj = stopRemoteServer();
 	    } else {
 		retObj = Context.getLibrary().runKeyword(keyword, args);
 	    }
 	    kr.put("output", baos.toString());
 	    kr.put("return", retObj);
 	    return kr;
 	} catch (Throwable e) {
 	    log.warn("", e);
 	    kr.put("status", "FAIL");
 	    kr.put("output", baos.toString());
 	    kr.put("return", "");
 	    Throwable t = e.getCause() == null ? e : e.getCause();
	    kr.put("error", t.getMessage());
 	    kr.put("traceback", ExceptionUtils.getStackTrace(t));
 	    return kr;
 	} finally {
 	    System.setOut(outBackup);
 	}
     }
 
     /**
      * Get an array of argument descriptors for the given keyword.
      * 
      * @param keyword
      *            The keyword to lookup.
      * @return A string array of argument descriptors for the given keyword.
      */
     public String[] get_keyword_arguments(String keyword) {
 	if (keyword.equalsIgnoreCase("stop_remote_server")) {
 	    return new String[0];
 	}
 	try {
 	    String[] args = Context.getLibrary().getKeywordArguments(keyword);
 	    // TODO: is this acceptable?
 	    return args == null ? new String[0] : args;
 	} catch (Throwable e) {
 	    log.warn("", e);
 	    throw new RuntimeException(e);
 	}
     }
 
     /**
      * Get documentation for given keyword.
      * 
      * @param keyword
      *            The keyword to get documentation for.
      * @return A documentation string for the given keyword.
      */
     public String get_keyword_documentation(String keyword) {
 	if (keyword.equalsIgnoreCase("stop_remote_server")) {
 	    return "Stops the remote server.\n\nThe server may be configured so that users cannot stop it.";
 	}
 	try {
 	    String doc = Context.getLibrary().getKeywordDocumentation(keyword);
 	    return doc == null ? "" : doc;
 	} catch (Throwable e) {
 	    log.warn("", e);
 	    throw new RuntimeException(e);
 	}
     }
 
     /**
      * Stops the remote server if it is configured to allow that.
      * 
      * @return remote result Map containing the execution results
      */
     public Map<String, Object> stop_remote_server() {
 	return run_keyword("stop_remote_server", null);
     }
 
     private boolean stopRemoteServer() {
 	if (Context.getRemoteServer().getAllowRemoteStop()) {
 	    System.out.println("Robot Framework remote server stopping");
 	    Context.getRemoteServer().gracefulStop();
 	} else {
 	    System.out.println("This Robot Framework remote server does not allow stopping");
 	}
 	return true;
     }
 }
