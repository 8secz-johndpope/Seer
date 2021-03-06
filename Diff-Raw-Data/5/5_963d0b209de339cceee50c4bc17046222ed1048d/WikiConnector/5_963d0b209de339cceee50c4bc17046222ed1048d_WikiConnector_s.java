 /* $Id$ */
 
 /**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.apache.manifoldcf.crawler.connectors.wiki;
 
 import org.apache.manifoldcf.core.interfaces.*;
 import org.apache.manifoldcf.agents.interfaces.*;
 import org.apache.manifoldcf.crawler.interfaces.*;
 import org.apache.manifoldcf.crawler.system.Logging;
 
 import org.xml.sax.Attributes;
 
 import org.apache.manifoldcf.core.common.XMLDoc;
 import org.apache.manifoldcf.agents.common.XMLStream;
 import org.apache.manifoldcf.agents.common.XMLContext;
 import org.apache.manifoldcf.agents.common.XMLStringContext;
 import org.apache.manifoldcf.agents.common.XMLFileContext;
 
 import org.apache.commons.httpclient.*;
 import org.apache.commons.httpclient.methods.*;
 import org.apache.commons.httpclient.params.*;
 import org.apache.commons.httpclient.auth.*;
 import org.apache.commons.httpclient.protocol.*;
 
 import java.util.*;
 import java.io.*;
 import java.net.*;
 
 /** This is the repository connector for a wiki.
 */
 public class WikiConnector extends org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector
 {
   public static final String _rcsid = "@(#)$Id$";
 
   // Activities that we know about
   
   /** Fetch activity */
   protected final static String ACTIVITY_FETCH = "fetch document";
 
   /** Activities list */
   protected static final String[] activitiesList = new String[]{ACTIVITY_FETCH};
 
   /** Has setup been called? */
   protected boolean hasBeenSetup = false;
   
   /** Server name */
   protected String server = null;
   
   /** Base URL */
   protected String baseURL = null;
   
   /** Connection management */
   protected MultiThreadedHttpConnectionManager connectionManager = null;
 
   /** Constructor.
   */
   public WikiConnector()
   {
   }
 
   /** List the activities we might report on.
   */
   @Override
   public String[] getActivitiesList()
   {
     return activitiesList;
   }
 
   /** For any given document, list the bins that it is a member of.
   */
   @Override
   public String[] getBinNames(String documentIdentifier)
   {
     // Return the host name
     return new String[]{server};
   }
 
   /** Connect.
   *@param configParameters is the set of configuration parameters, which
   * in this case describe the target appliance, basic auth configuration, etc.
   */
   @Override
   public void connect(ConfigParams configParameters)
   {
     super.connect(configParameters);
     server = params.getParameter(WikiConfig.PARAM_SERVER);
   }
 
   protected void getSession()
     throws ManifoldCFException, ServiceInterruption
   {
     if (hasBeenSetup == false)
     {
       String protocol = params.getParameter(WikiConfig.PARAM_PROTOCOL);
       if (protocol == null || protocol.length() == 0)
         protocol = "http";
       String portString = params.getParameter(WikiConfig.PARAM_PORT);
       if (portString == null || portString.length() == 0)
         portString = null;
       String path = params.getParameter(WikiConfig.PARAM_PATH);
       if (path == null)
         path = "/w";
       
       baseURL = protocol + "://" + server + ((portString!=null)?":" + portString:"") + path + "/api.php?format=xml&";
 
       // Set up connection manager
       connectionManager = new MultiThreadedHttpConnectionManager();
       connectionManager.getParams().setMaxTotalConnections(1);
 
       hasBeenSetup = true;
     }
   }
   
   /** Check status of connection.
   */
   @Override
   public String check()
     throws ManifoldCFException
   {
     try
     {
       // Destroy saved session setup and repeat it
       hasBeenSetup = false;
       performCheck();
       return super.check();
     }
     catch (ServiceInterruption e)
     {
       return "Transient error: "+e.getMessage();
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         throw e;
       return "Error: "+e.getMessage();
     }
   }
 
   /** This method is periodically called for all connectors that are connected but not
   * in active use.
   */
   @Override
   public void poll()
     throws ManifoldCFException
   {
     if (connectionManager != null)
       connectionManager.closeIdleConnections(60000L);
   }
 
   /** Close the connection.  Call this before discarding the connection.
   */
   @Override
   public void disconnect()
     throws ManifoldCFException
   {
     hasBeenSetup = false;
     server = null;
     baseURL = null;
     
     if (connectionManager != null)
     {
       connectionManager.shutdown();
       connectionManager = null;
     }
 
     super.disconnect();
   }
 
   /** Get the maximum number of documents to amalgamate together into one batch, for this connector.
   *@return the maximum number. 0 indicates "unlimited".
   */
   @Override
   public int getMaxDocumentRequest()
   {
     return 20;
   }
 
   /** Queue "seed" documents.  Seed documents are the starting places for crawling activity.  Documents
   * are seeded when this method calls appropriate methods in the passed in ISeedingActivity object.
   *
   * This method can choose to find repository changes that happen only during the specified time interval.
   * The seeds recorded by this method will be viewed by the framework based on what the
   * getConnectorModel() method returns.
   *
   * It is not a big problem if the connector chooses to create more seeds than are
   * strictly necessary; it is merely a question of overall work required.
   *
   * The times passed to this method may be interpreted for greatest efficiency.  The time ranges
   * any given job uses with this connector will not overlap, but will proceed starting at 0 and going
   * to the "current time", each time the job is run.  For continuous crawling jobs, this method will
   * be called once, when the job starts, and at various periodic intervals as the job executes.
   *
   * When a job's specification is changed, the framework automatically resets the seeding start time to 0.  The
   * seeding start time may also be set to 0 on each job run, depending on the connector model returned by
   * getConnectorModel().
   *
   * Note that it is always ok to send MORE documents rather than less to this method.
   *@param activities is the interface this method should use to perform whatever framework actions are desired.
   *@param spec is a document specification (that comes from the job).
   *@param startTime is the beginning of the time range to consider, inclusive.
   *@param endTime is the end of the time range to consider, exclusive.
   */
   @Override
   public void addSeedDocuments(ISeedingActivity activities, DocumentSpecification spec,
     long startTime, long endTime)
     throws ManifoldCFException, ServiceInterruption
   {
     // Scan specification nodes and extract prefixes and namespaces
     boolean seenAny = false;
     for (int i = 0 ; i < spec.getChildCount() ; i++)
     {
       SpecificationNode sn = spec.getChild(i);
       if (sn.getType().equals(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX))
       {
         String namespace = sn.getAttributeValue(WikiConfig.ATTR_NAMESPACE);
         String titleprefix = sn.getAttributeValue(WikiConfig.ATTR_TITLEPREFIX);
         listAllPages(activities,namespace,titleprefix,startTime,endTime);
         seenAny = true;
       }
     }
     if (!seenAny)
       listAllPages(activities,null,null,startTime,endTime);
   }
 
   /** Get document versions given an array of document identifiers.
   * This method is called for EVERY document that is considered. It is
   * therefore important to perform as little work as possible here.
   *@param documentIdentifiers is the array of local document identifiers, as understood by this connector.
   *@param oldVersions is the corresponding array of version strings that have been saved for the document identifiers.
   *   A null value indicates that this is a first-time fetch, while an empty string indicates that the previous document
   *   had an empty version string.
   *@param activities is the interface this method should use to perform whatever framework actions are desired.
   *@param spec is the current document specification for the current job.  If there is a dependency on this
   * specification, then the version string should include the pertinent data, so that reingestion will occur
   * when the specification changes.  This is primarily useful for metadata.
   *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
   *@param usesDefaultAuthority will be true only if the authority in use for these documents is the default one.
   *@return the corresponding version strings, with null in the places where the document no longer exists.
   * Empty version strings indicate that there is no versioning ability for the corresponding document, and the document
   * will always be processed.
   */
   @Override
   public String[] getDocumentVersions(String[] documentIdentifiers, String[] oldVersions, IVersionActivity activities,
     DocumentSpecification spec, int jobMode, boolean usesDefaultAuthority)
     throws ManifoldCFException, ServiceInterruption
   {
     Map<String,String> versions = new HashMap<String,String>();
     getTimestamps(documentIdentifiers,versions,activities);
     String[] rval = new String[documentIdentifiers.length];
     for (int i = 0 ; i < rval.length ; i++)
     {
       rval[i] = versions.get(documentIdentifiers[i]);
     }
     return rval;
   }
   
   /** Process a set of documents.
   * This is the method that should cause each document to be fetched, processed, and the results either added
   * to the queue of documents for the current job, and/or entered into the incremental ingestion manager.
   * The document specification allows this class to filter what is done based on the job.
   *@param documentIdentifiers is the set of document identifiers to process.
   *@param versions is the corresponding document versions to process, as returned by getDocumentVersions() above.
   *       The implementation may choose to ignore this parameter and always process the current version.
   *@param activities is the interface this method should use to queue up new document references
   * and ingest documents.
   *@param spec is the document specification.
   *@param scanOnly is an array corresponding to the document identifiers.  It is set to true to indicate when the processing
   * should only find other references, and should not actually call the ingestion methods.
   *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
   */
   public void processDocuments(String[] documentIdentifiers, String[] versions, IProcessActivity activities,
     DocumentSpecification spec, boolean[] scanOnly, int jobMode)
     throws ManifoldCFException, ServiceInterruption
   {
     Map<String,String> urls = new HashMap<String,String>();
     getDocURLs(documentIdentifiers,urls);
     for (int i = 0 ; i < documentIdentifiers.length ; i++)
     {
       if (!scanOnly[i])
       {
         String url = urls.get(documentIdentifiers[i]);
         if (url != null)
           getDocInfo(documentIdentifiers[i], versions[i], url, activities);
       }
     }
   }
   
   // UI support methods.
   //
   // These support methods come in two varieties.  The first bunch is involved in setting up connection configuration information.  The second bunch
   // is involved in presenting and editing document specification information for a job.  The two kinds of methods are accordingly treated differently,
   // in that the first bunch cannot assume that the current connector object is connected, while the second bunch can.  That is why the first bunch
   // receives a thread context argument for all UI methods, while the second bunch does not need one (since it has already been applied via the connect()
   // method, above).
     
   /** Output the configuration header section.
   * This method is called in the head section of the connector's configuration page.  Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
   */
   @Override
   public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
     Locale locale, ConfigParams parameters, List<String> tabsArray)
     throws ManifoldCFException, IOException
   {
     tabsArray.add(Messages.getString(locale,"WikiConnector.Server"));
 
     out.print(
 "<script type=\"text/javascript\">\n"+
 "<!--\n"+
 "function checkConfig()\n"+
 "{\n"+
 "  if (editconnection.serverport.value != \"\" && !isInteger(editconnection.serverport.value))\n"+
 "  {\n"+
 "    alert(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.WikiServerPortMustBeAValidInteger")+"\");\n"+
 "    editconnection.serverport.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  if (editconnection.serverpath.value != \"\" && editconnection.serverpath.value.indexOf(\"/\") != 0)\n"+
 "  {\n"+
 "    alert(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.PathMustStartWithACharacter")+"\");\n"+
 "    editconnection.serverpath.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  return true;\n"+
 "}\n"+
 "\n"+
 "function checkConfigForSave()\n"+
 "{\n"+
 "  if (editconnection.servername.value == \"\")\n"+
 "  {\n"+
 "    alert(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.PleaseSupplyAValidWikiServerName")+"\");\n"+
 "    SelectTab(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.Server")+"\");\n"+
 "    editconnection.servername.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  if (editconnection.serverport.value != \"\" && !isInteger(editconnection.serverport.value))\n"+
 "  {\n"+
 "    alert(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.WikiServerPortMustBeAValidInteger")+"\");\n"+
 "    SelectTab(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.Server")+"\");\n"+
 "    editconnection.serverport.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  if (editconnection.serverpath.value != \"\" && editconnection.serverpath.value.indexOf(\"/\") != 0)\n"+
 "  {\n"+
 "    alert(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.PathMustStartWithACharacter")+"\");\n"+
 "    SelectTab(\""+Messages.getBodyJavascriptString(locale,"WikiConnector.Server")+"\");\n"+
 "    editconnection.serverpath.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  return true;\n"+
 "}\n"+
 "\n"+
 "//-->\n"+
 "</script>\n"
     );
   }
   
   /** Output the configuration body section.
   * This method is called in the body section of the connector's configuration page.  Its purpose is to present the required form elements for editing.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
   * form is "editconnection".
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@param tabName is the current tab name.
   */
   public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
     Locale locale, ConfigParams parameters, String tabName)
     throws ManifoldCFException, IOException
   {
     String protocol = parameters.getParameter(WikiConfig.PARAM_PROTOCOL);
     if (protocol == null)
       protocol = "http";
 		
     String server = parameters.getParameter(WikiConfig.PARAM_SERVER);
     if (server == null)
       server = "";
 
     String port = parameters.getParameter(WikiConfig.PARAM_PORT);
     if (port == null)
       port = "";
 
     String path = parameters.getParameter(WikiConfig.PARAM_PATH);
     if (path == null)
       path = "/w";
 
     if (tabName.equals(Messages.getString(locale,"WikiConnector.Server")))
     {
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.Protocol") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <select name=\"serverprotocol\">\n"+
 "        <option value=\"http\""+(protocol.equals("http")?" selected=\"true\"":"")+">http</option>\n"+
 "        <option value=\"https\""+(protocol.equals("https")?" selected=\"true\"":"")+">https</option>\n"+
 "      </select>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.ServerName") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <input name=\"servername\" type=\"text\" size=\"32\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server)+"\"/>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.Port") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <input name=\"serverport\" type=\"text\" size=\"5\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(port)+"\"/>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.PathName") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <input name=\"serverpath\" type=\"text\" size=\"16\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(path)+"\"/>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       // Server tab hiddens
       out.print(
 "<input type=\"hidden\" name=\"serverprotocol\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(protocol)+"\"/>\n"+
 "<input type=\"hidden\" name=\"servername\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server)+"\"/>\n"+
 "<input type=\"hidden\" name=\"serverport\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(port)+"\"/>\n"+
 "<input type=\"hidden\" name=\"serverpath\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(path)+"\"/>\n"
       );
     }
 
   }
   
   /** Process a configuration post.
   * This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been
   * posted.  Its purpose is to gather form information and modify the configuration parameters accordingly.
   * The name of the posted form is "editconnection".
   *@param threadContext is the local thread context.
   *@param variableContext is the set of variables available from the post, including binary file post information.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
   @Override
   public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
     Locale locale, ConfigParams parameters)
     throws ManifoldCFException
   {
     String protocol = variableContext.getParameter("serverprotocol");
     if (protocol != null)
       parameters.setParameter(WikiConfig.PARAM_PROTOCOL,protocol);
 		
     String server = variableContext.getParameter("servername");
     if (server != null)
       parameters.setParameter(WikiConfig.PARAM_SERVER,server);
 
     String port = variableContext.getParameter("serverport");
     if (port != null)
       parameters.setParameter(WikiConfig.PARAM_PORT,port);
 
     String path = variableContext.getParameter("serverpath");
     if (path != null)
       parameters.setParameter(WikiConfig.PARAM_PATH,path);
 
     return null;
   }
   
   /** View configuration.
   * This method is called in the body section of the connector's view configuration page.  Its purpose is to present the connection information to the user.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   */
   @Override
   public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
     Locale locale, ConfigParams parameters)
     throws ManifoldCFException, IOException
   {
     out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr>\n"+
 "    <td class=\"description\" colspan=\"1\"><nobr>"+Messages.getBodyString(locale,"WikiConnector.Parameters")+"</nobr></td>\n"+
 "    <td class=\"value\" colspan=\"3\">\n"
     );
     Iterator iter = parameters.listParameters();
     while (iter.hasNext())
     {
       String param = (String)iter.next();
       String value = parameters.getParameter(param);
       if (param.length() >= "password".length() && param.substring(param.length()-"password".length()).equalsIgnoreCase("password"))
       {
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=********</nobr><br/>\n"
         );
       }
       else if (param.length() >="keystore".length() && param.substring(param.length()-"keystore".length()).equalsIgnoreCase("keystore"))
       {
         IKeystoreManager kmanager = KeystoreManagerFactory.make("",value);
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=&lt;"+Integer.toString(kmanager.getContents().length)+Messages.getBodyString(locale,"WikiConnector.certificates")+"&gt;</nobr><br/>\n"
         );
       }
       else
       {
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value)+"</nobr><br/>\n"
         );
       }
     }
     
     out.print(
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
     );
 
   }
   
   /** Output the specification header section.
   * This method is called in the head section of a job page which has selected a repository connection of the current type.  Its purpose is to add the required tabs
   * to the list, and to output any javascript methods that might be needed by the job editing HTML.
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
   */
   @Override
   public void outputSpecificationHeader(IHTTPOutput out, Locale locale, DocumentSpecification ds, List<String> tabsArray)
     throws ManifoldCFException, IOException
   {
     tabsArray.add(Messages.getString(locale,"WikiConnector.NamespaceAndTitles"));
     
     out.print(
 "<script type=\"text/javascript\">\n"+
 "<!--\n"+
 "function checkSpecification()\n"+
 "{\n"+
 "  // Does nothing right now.\n"+
 "  return true;\n"+
 "}\n"+
 "\n"+
 "function NsDelete(k)\n"+
 "{\n"+
"  SpecOp(\"nsop\"+k, \"Delete\", \"ns_\"+k);\n"+
 "}\n"+
 "\n"+
 "function NsAdd(k)\n"+
 "{\n"+
 "  SpecOp(\"nsop\", \"Add\", \"ns_\"+k);\n"+
 "}\n"+
 "\n"+
 "function SpecOp(n, opValue, anchorvalue)\n"+
 "{\n"+
 "  eval(\"editjob.\"+n+\".value = \\\"\"+opValue+\"\\\"\");\n"+
 "  postFormSetAnchor(anchorvalue);\n"+
 "}\n"+
 "\n"+
 "//-->\n"+
 "</script>\n"
     );
   }
   
   /** Output the specification body section.
   * This method is called in the body section of a job page which has selected a repository connection of the current type.  Its purpose is to present the required form elements for editing.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
   * form is "editjob".
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   *@param tabName is the current tab name.
   */
   @Override
 
   public void outputSpecificationBody(IHTTPOutput out, Locale locale, DocumentSpecification ds, String tabName)
     throws ManifoldCFException, IOException
   {
     if (tabName.equals(Messages.getString(locale,"WikiConnector.NamespaceAndTitles")))
     {
       boolean seenAny = false;
       // Output table column headers
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.NamespaceAndTitles2") + "</nobr></td>\n"+
 "    <td class=\"boxcell\">\n"+
 "      <table class=\"formtable\">\n"+
 "        <tr class=\"formheaderrow\">\n"+
 "          <td class=\"formcolumnheader\"></td>\n"+
 "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.Namespace") + "</nobr></td>\n"+
 "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.TitlePrefix") + "</nobr></td>\n"+
 "        </tr>\n"
       );
 
       int k = 0;
       for (int i = 0 ; i < ds.getChildCount() ; i++)
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX))
         {
           String namespace = sn.getAttributeValue(WikiConfig.ATTR_NAMESPACE);
           String titlePrefix = sn.getAttributeValue(WikiConfig.ATTR_TITLEPREFIX);
           
           String nsOpName = "nsop_"+k;
           String nsNsName = "nsnsname_"+k;
           String nsTitlePrefix = "nstitleprefix_"+k;
           out.print(
 "        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              <a name=\""+"ns_"+Integer.toString(k)+"\"/>\n"+
 "              <input type=\"hidden\" name=\""+nsOpName+"\" value=\"\"/>\n"+
 "              <input type=\"hidden\" name=\""+nsNsName+"\" value=\""+((namespace==null)?"":org.apache.manifoldcf.ui.util.Encoder.attributeEscape(namespace))+"\"/>\n"+
 "              <input type=\"hidden\" name=\""+nsTitlePrefix+"\" value=\""+((titlePrefix==null)?"":org.apache.manifoldcf.ui.util.Encoder.attributeEscape(titlePrefix))+"\"/>\n"+
 "              <input type=\"button\" value=\"" + Messages.getAttributeString(locale,"WikiConnector.Delete") + "\" onClick='Javascript:NsDelete("+Integer.toString(k)+")' alt=\""+Messages.getAttributeString(locale,"WikiConnector.DeleteNamespaceTitle")+Integer.toString(k)+"\"/>\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              "+((namespace==null)?"(default)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(namespace))+"\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              "+((titlePrefix==null)?"(all titles)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(titlePrefix))+"\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "        </tr>\n"
           );
           k++;
         }
       }
 
       if (k == 0)
       {
         out.print(
 "        <tr class=\"formrow\"><td colspan=\"3\" class=\"formmessage\">" + Messages.getBodyString(locale,"WikiConnector.NoSpecification") + "</td></tr>\n"
         );
       }
 
       // Add area
       out.print(
 "        <tr class=\"formrow\"><td colspan=\"4\" class=\"formseparator\"><hr/></td></tr>\n"
       );
 
       // Obtain the list of namespaces
       Map<String,String> namespaces = new HashMap<String,String>();
       try
       {
         getNamespaces(namespaces);
         // Extract and sort the names we're going to present
         String[] nameSpaceNames = new String[namespaces.size()];
         Iterator<String> keyIter = namespaces.keySet().iterator();
         int j = 0;
         while (keyIter.hasNext())
         {
           nameSpaceNames[j++] = keyIter.next();
         }
         java.util.Arrays.sort(nameSpaceNames);
       
         out.print(
 "        <tr class=\"formrow\">\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              <a name=\""+"ns_"+Integer.toString(k)+"\"/>\n"+
 "              <input type=\"hidden\" name=\"nsop\" value=\"\"/>\n"+
 "              <input type=\"hidden\" name=\"nscount\" value=\""+Integer.toString(k)+"\"/>\n"+
 "              <input type=\"button\" value=\"" + Messages.getAttributeString(locale,"WikiConnector.Add") + "\" onClick='Javascript:NsAdd("+Integer.toString(k)+")' alt=\"" + Messages.getAttributeString(locale,"WikiConnector.AddNamespacePrefix") + "\"/>\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              <select name=\"nsnsname\">\n"+
 "                <option value=\"\" selected=\"true\">-- " + Messages.getBodyString(locale,"WikiConnector.UseDefault") + " --</option>\n"
         );
         for (int l = 0 ; l < nameSpaceNames.length ; l++)
         {
           String prettyName = nameSpaceNames[l];
           String canonicalName = namespaces.get(prettyName);
           out.print(
 "                <option value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(canonicalName)+"\">"+
   org.apache.manifoldcf.ui.util.Encoder.bodyEscape(prettyName)+"</option>\n"
           );
         }
         out.print(
 "              </select>\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              <input type=\"text\" name=\"nstitleprefix\" size=\"16\" value=\"\"/>\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "        </tr>\n"
         );
       }
       catch (ServiceInterruption e)
       {
         out.print(
 "        <tr class=\"formrow\"><td colspan=\"3\" class=\"formmessage\">" + Messages.getBodyString(locale,"WikiConnector.TransientError") + org.apache.manifoldcf.ui.util.Encoder.bodyEscape(e.getMessage())+"</td></tr>\n"
         );
       }
 
       out.print(
 "      </table>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
 
     }
     else
     {
       // Generate hiddens
       int k = 0;
       for (int i = 0 ; i < ds.getChildCount() ; i++)
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX))
         {
           String namespace = sn.getAttributeValue(WikiConfig.ATTR_NAMESPACE);
           String titlePrefix = sn.getAttributeValue(WikiConfig.ATTR_TITLEPREFIX);
           
           String nsNsName = "nsnsname_"+k;
           String nsTitlePrefix = "nstitleprefix_"+k;
 
           out.print(
 "<input type=\"hidden\" name=\""+nsNsName+"\" value=\""+((namespace == null)?"":org.apache.manifoldcf.ui.util.Encoder.attributeEscape(namespace))+"\"/>\n"+
 "<input type=\"hidden\" name=\""+nsTitlePrefix+"\" value=\""+((titlePrefix == null)?"":org.apache.manifoldcf.ui.util.Encoder.attributeEscape(titlePrefix))+"\"/>\n"
           );
           k++;
         }
       }
       out.print(
 "<input type=\"hidden\" name=\"nscount\" value=\""+new Integer(k)+"\"/>\n"
       );
     }
   }
   
   /** Process a specification post.
   * This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been
   * posted.  Its purpose is to gather form information and modify the document specification accordingly.
   * The name of the posted form is "editjob".
   *@param variableContext contains the post data, including binary file-upload information.
   *@param ds is the current document specification for this job.
   *@return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
   @Override
   public String processSpecificationPost(IPostParameters variableContext, Locale locale, DocumentSpecification ds)
     throws ManifoldCFException
   {
     String countString = variableContext.getParameter("nscount");
     if (countString != null)
     {
       for (int i = 0 ; i < ds.getChildCount() ; i++)
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX))
           ds.removeChild(i);
         else
           i++;
       }
       
       int nsCount = Integer.parseInt(countString);
       for (int i = 0 ; i < nsCount ; i++)
       {
         String nsOpName = "nsop_"+i;
         String nsNsName = "nsnsname_"+i;
         String nsTitlePrefix = "nstitleprefix_"+i;
         
         String nsOp = variableContext.getParameter(nsOpName);
        if (nsOp != null && !nsOp.equals("Delete"))
         {
           String namespaceName = variableContext.getParameter(nsNsName);
           if (namespaceName != null && namespaceName.length() == 0)
             namespaceName = null;
           String titlePrefix = variableContext.getParameter(nsTitlePrefix);
           if (titlePrefix != null && titlePrefix.length() == 0)
             titlePrefix = null;
           SpecificationNode sn = new SpecificationNode(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX);
           if (namespaceName != null)
             sn.setAttribute(WikiConfig.ATTR_NAMESPACE,namespaceName);
           if (titlePrefix != null)
             sn.setAttribute(WikiConfig.ATTR_TITLEPREFIX,titlePrefix);
           ds.addChild(ds.getChildCount(),sn);
         }
       }
       
       String newOp = variableContext.getParameter("nsop");
       if (newOp != null && newOp.equals("Add"))
       {
         String namespaceName = variableContext.getParameter("nsnsname");
         if (namespaceName != null && namespaceName.length() == 0)
           namespaceName = null;
         String titlePrefix = variableContext.getParameter("nstitleprefix");
         if (titlePrefix != null && titlePrefix.length() == 0)
           titlePrefix = null;
         SpecificationNode sn = new SpecificationNode(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX);
         if (namespaceName != null)
           sn.setAttribute(WikiConfig.ATTR_NAMESPACE,namespaceName);
         if (titlePrefix != null)
           sn.setAttribute(WikiConfig.ATTR_TITLEPREFIX,titlePrefix);
         ds.addChild(ds.getChildCount(),sn);
       }
     }
 
     return null;
   }
   
   /** View specification.
   * This method is called in the body section of a job's view page.  Its purpose is to present the document specification information to the user.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   */
   @Override
   public void viewSpecification(IHTTPOutput out, Locale locale, DocumentSpecification ds)
     throws ManifoldCFException, IOException
   {
     out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr>\n"
     );
     out.print(
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.NamespaceAndTitles2") + "</nobr></td>\n"+
 "    <td class=\"boxcell\">\n"+
 "      <table class=\"formtable\">\n"+
 "        <tr class=\"formheaderrow\">\n"+
 "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.Namespace") + "</nobr></td>\n"+
 "          <td class=\"formcolumnheader\"><nobr>" + Messages.getBodyString(locale,"WikiConnector.TitlePrefix") + "</nobr></td>\n"+
 "        </tr>\n"
     );
 
     int k = 0;
     for (int i = 0 ; i < ds.getChildCount() ; i++)
     {
       SpecificationNode sn = ds.getChild(i);
       if (sn.getType().equals(WikiConfig.NODE_NAMESPACE_TITLE_PREFIX))
       {
         String namespace = sn.getAttributeValue(WikiConfig.ATTR_NAMESPACE);
         String titlePrefix = sn.getAttributeValue(WikiConfig.ATTR_TITLEPREFIX);
         out.print(
 "        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              "+((namespace==null)?"(default)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(namespace))+"\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "          <td class=\"formcolumncell\">\n"+
 "            <nobr>\n"+
 "              "+((titlePrefix==null)?"(all documents)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(titlePrefix))+"\n"+
 "            </nobr>\n"+
 "          </td>\n"+
 "        </tr>\n"
         );
         k++;
       }
     }
     
     if (k == 0)
       out.print(
 "        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"2\">" + Messages.getBodyString(locale,"WikiConnector.AllDefaultNamespaceDocumentsIncluded") + "</td></tr>\n"
       );
     
     out.print(
 "      </table>\n"+
 "    </td>\n"
     );
 
     out.print(
 "  </tr>\n"+
 "</table>\n"
     );
   }
 
   // Protected static classes and methods
 
   /** Create and initialize an HttpClient instance */
   protected HttpClient getInitializedClient()
     throws ServiceInterruption, ManifoldCFException
   {
     HttpClient client = new HttpClient(connectionManager);
     return client;
   }
 
   /** Create and initialize an HttpMethodBase */
   protected HttpMethodBase getInitializedMethod(String URL)
   {
     GetMethod method = new GetMethod(URL);
     method.getParams().setParameter("http.socket.timeout", new Integer(300000));
     return method;
   }
 
   // -- Methods and classes to perform a "check" operation. --
 
   /** Do the check operation.  This throws an exception if anything is wrong.
   */
   protected void performCheck()
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getCheckURL());
     try
     {
       ExecuteCheckThread t = new ExecuteCheckThread(client,executeMethod);
       try
       {
         t.start();
         t.join();
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         // Drop the connection on the floor
         executeMethod = null;
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Fetch test timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Fetch test received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Fetch test connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Fetch test had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
     }
   }
 
   /** Get a URL for a check operation.
   */
   protected String getCheckURL()
     throws ManifoldCFException
   {
     return baseURL + "action=query&list=allpages&aplimit=1";
   }
   
   /** Thread to execute a "check" operation.  This thread both executes the operation and parses the result. */
   protected static class ExecuteCheckThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
 
     public ExecuteCheckThread(HttpClient client, HttpMethodBase executeMethod)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
           throw new ManifoldCFException("Unexpected response code: "+rval);
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           parseCheckResponse(is);
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         this.exception = e;
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
   }
 
   /** Parse check response, e.g.:
   * <api xmlns="http://www.mediawiki.org/xml/api/">
   *   <query>
   *     <allpages>
   *       <p pageid="19839654" ns="0" title="Kre&#039;fey" />
   *     </allpages>
   *   </query>
   *   <query-continue>
   *     <allpages apfrom="Krea" />
   *   </query-continue>
   * </api>
   */
   protected static void parseCheckResponse(InputStream is)
     throws ManifoldCFException, ServiceInterruption
   {
     // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
     XMLStream x = new XMLStream();
     WikiCheckAPIContext c = new WikiCheckAPIContext(x);
     x.setContext(c);
     try
     {
       try
       {
         x.parse(is);
         if (!c.hasResponse())
           throw new ManifoldCFException("Valid API response not detected");
       }
       catch (IOException e)
       {
         long time = System.currentTimeMillis();
         throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
       }
     }
     finally
     {
       x.cleanup();
     }
   }
 
   /** Class representing the "api" context of a "check" response */
   protected static class WikiCheckAPIContext extends SingleLevelContext
   {
     boolean responseSeen = false;
     
     public WikiCheckAPIContext(XMLStream theStream)
     {
       super(theStream,"api");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiCheckQueryContext(theStream,namespaceURI,localName,qName,atts);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       responseSeen |= ((WikiCheckQueryContext)child).hasResponse();
     }
     
     public boolean hasResponse()
     {
       return responseSeen;
     }
 
   }
 
   /** Class representing the "api/query" context of a "check" response */
   protected static class WikiCheckQueryContext extends SingleLevelContext
   {
     protected boolean responseSeen = false;
     
     public WikiCheckQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiCheckAllPagesContext(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       responseSeen |= ((WikiCheckAllPagesContext)child).hasResponse();
     }
 
     public boolean hasResponse()
     {
       return responseSeen;
     }
     
   }
 
   /** Class recognizing the "api/query/allpages" context of a "check" response */
   protected static class WikiCheckAllPagesContext extends SingleLevelContext
   {
     protected boolean responseSeen = false;
     
     public WikiCheckAllPagesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"allpages");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiCheckPContext(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       responseSeen |= true;
     }
 
     public boolean hasResponse()
     {
       return responseSeen;
     }
     
   }
   
   /** Class representing the "api/query/allpages/p" context of a "check" response */
   protected static class WikiCheckPContext extends BaseProcessingContext
   {
     public WikiCheckPContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts);
     }
   }
 
   // -- Methods and classes to perform a "list pages" operation. --
 
   /** Perform a series of listPages() operations, so that we fully obtain the documents we're looking for even though
   * we're limited to 500 of them per request.
   */
   protected void listAllPages(ISeedingActivity activities, String namespace, String prefix, long startTime, long endTime)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     String lastTitle = null;
     while (true)
     {
       activities.checkJobStillActive();
       
       // Start with the last title seen in the previous round. 
       String newLastTitle = executeListPagesViaThread(lastTitle,namespace,prefix,activities);
       if (newLastTitle == null)
         break;
       lastTitle = newLastTitle;
     }
   }
   
   /** Execute a listPages() operation via a thread.  Returns the last page title. */
   protected String executeListPagesViaThread(String startPageTitle, String namespace, String prefix, ISeedingActivity activities)
     throws ManifoldCFException, ServiceInterruption
   {
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getListPagesURL(startPageTitle,namespace,prefix));
     try
     {
       PageBuffer pageBuffer = new PageBuffer();
       ExecuteListPagesThread t = new ExecuteListPagesThread(client,executeMethod,pageBuffer,startPageTitle);
       try
       {
         t.start();
 
         // Pick up the pages, and add them to the activities, before we join with the child thread.
         while (true)
         {
           // The only kind of exceptions this can throw are going to shut the process down.
           String pageID = pageBuffer.fetch();
           if (pageID ==  null)
             break;
           // Add the pageID to the queue
           activities.addSeedDocument(pageID);
         }
         
         t.join();
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
         return t.getLastPageTitle();
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
       finally
       {
         // Make SURE buffer is dead, otherwise child thread may well hang waiting on it
         pageBuffer.abandon();
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         // Drop the connection on the floor
         executeMethod = null;
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("ListPages timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("ListPages received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("ListPages connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("ListPages had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
     }
   }
 
   /** Create a URL to obtain the next 500 pages.
   */
   protected String getListPagesURL(String startingTitle, String namespace, String prefix)
     throws ManifoldCFException
   {
     try
     {
       return baseURL + "action=query&list=allpages" +
         ((prefix != null)?"&apprefix="+URLEncoder.encode(prefix,"utf-8"):"") +
         ((namespace != null)?"&apnamespace="+URLEncoder.encode(namespace,"utf-8"):"") +
         ((startingTitle!=null)?"&apfrom="+URLEncoder.encode(startingTitle,"utf-8"):"") +
         "&aplimit=500";
     }
     catch (UnsupportedEncodingException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
 
   /** Thread to execute a list pages operation */
   protected static class ExecuteListPagesThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
     protected PageBuffer pageBuffer;
     protected String lastPageTitle = null;
     protected String startPageTitle;
 
     public ExecuteListPagesThread(HttpClient client, HttpMethodBase executeMethod, PageBuffer pageBuffer, String startPageTitle)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
       this.pageBuffer = pageBuffer;
       this.startPageTitle = startPageTitle;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
           throw new ManifoldCFException("Unexpected response code: "+rval);
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           lastPageTitle = parseListPagesResponse(is,pageBuffer,startPageTitle);
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         this.exception = e;
       }
       finally
       {
         pageBuffer.signalDone();
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
     public String getLastPageTitle()
     {
       return lastPageTitle;
     }
   }
 
   /** Parse list output, e.g.:
   * <api xmlns="http://www.mediawiki.org/xml/api/">
   *   <query>
   *     <allpages>
   *       <p pageid="19839654" ns="0" title="Kre&#039;fey" />
   *       <p pageid="30955295" ns="0" title="Kre-O" />
   *       <p pageid="14773725" ns="0" title="Kre8tiveworkz" />
   *       <p pageid="19219017" ns="0" title="Kre M&#039;Baye" />
   *       <p pageid="19319577" ns="0" title="Kre Mbaye" />
   *     </allpages>
   *   </query>
   *   <query-continue>
   *     <allpages apfrom="Krea" />
   *   </query-continue>
   * </api>
   */
   protected static String parseListPagesResponse(InputStream is, PageBuffer buffer, String startPageTitle)
     throws ManifoldCFException, ServiceInterruption
   {
     // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
     XMLStream x = new XMLStream();
     WikiListPagesAPIContext c = new WikiListPagesAPIContext(x,buffer,startPageTitle);
     x.setContext(c);
     try
     {
       try
       {
         x.parse(is);
         return c.getLastTitle();
       }
       catch (IOException e)
       {
         long time = System.currentTimeMillis();
         throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
       }
     }
     finally
     {
       x.cleanup();
     }
   }
 
   /** Class representing the "api" context of a "list all pages" response */
   protected static class WikiListPagesAPIContext extends SingleLevelContext
   {
     protected String lastTitle = null;
     protected PageBuffer buffer;
     protected String startPageTitle;
     
     public WikiListPagesAPIContext(XMLStream theStream, PageBuffer buffer, String startPageTitle)
     {
       super(theStream,"api");
       this.buffer = buffer;
       this.startPageTitle = startPageTitle;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiListPagesQueryContext(theStream,namespaceURI,localName,qName,atts,buffer,startPageTitle);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       lastTitle = ((WikiListPagesQueryContext)child).getLastTitle();
     }
     
     public String getLastTitle()
     {
       return lastTitle;
     }
 
   }
 
   /** Class representing the "api/query" context of a "list all pages" response */
   protected static class WikiListPagesQueryContext extends SingleLevelContext
   {
     protected String lastTitle = null;
     protected PageBuffer buffer;
     protected String startPageTitle;
     
     public WikiListPagesQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       PageBuffer buffer, String startPageTitle)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
       this.buffer = buffer;
       this.startPageTitle = startPageTitle;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiListPagesAllPagesContext(theStream,namespaceURI,localName,qName,atts,buffer,startPageTitle);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       lastTitle = ((WikiListPagesAllPagesContext)child).getLastTitle();
     }
     
     public String getLastTitle()
     {
       return lastTitle;
     }
     
   }
 
   /** Class recognizing the "api/query/allpages" context of a "list all pages" response */
   protected static class WikiListPagesAllPagesContext extends SingleLevelContext
   {
     protected String lastTitle = null;
     protected PageBuffer buffer;
     protected String startPageTitle;
     
     public WikiListPagesAllPagesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       PageBuffer buffer, String startPageTitle)
     {
       super(theStream,namespaceURI,localName,qName,atts,"allpages");
       this.buffer = buffer;
       this.startPageTitle = startPageTitle;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       // When we recognize allpages, we need to look for <p> records.
       return new WikiListPagesPContext(theStream,namespaceURI,localName,qName,atts,buffer,startPageTitle);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       // Update the last title from all the <p> records we saw.
       lastTitle = ((WikiListPagesPContext)child).getLastTitle();
     }
     
     public String getLastTitle()
     {
       return lastTitle;
     }
     
   }
   
   /** Class representing the "api/query/allpages/p" context of a "list all pages" response */
   protected static class WikiListPagesPContext extends BaseProcessingContext
   {
     protected String lastTitle = null;
     protected PageBuffer buffer;
     protected String startPageTitle;
     
     public WikiListPagesPContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       PageBuffer buffer, String startPageTitle)
     {
       super(theStream,namespaceURI,localName,qName,atts);
       this.buffer = buffer;
       this.startPageTitle = startPageTitle;
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("p"))
       {
         String currentTitle = atts.getValue("title");
         // Skip the record that matches the start page title (just pretend it isn't there)
         if (startPageTitle == null || !currentTitle.equals(startPageTitle))
         {
           lastTitle = currentTitle;
           String pageID = atts.getValue("pageid");
           // Add the discovered page id to the page buffer
           try
           {
             buffer.add(pageID);
           }
           catch (InterruptedException e)
           {
             throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
           }
         }
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
     public String getLastTitle()
     {
       return lastTitle;
     }
   }
 
   // -- Methods and classes to perform a "get doc urls" operation. --
   
   protected void getDocURLs(String[] documentIdentifiers, Map<String,String> urls)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getGetDocURLsURL(documentIdentifiers));
     try
     {
       ExecuteGetDocURLsThread t = new ExecuteGetDocURLsThread(client,executeMethod,urls);
       try
       {
         t.start();
         t.join();
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         // Drop the connection on the floor
         executeMethod = null;
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("URL fetch timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("URL fetch received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("URL fetch connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("URL fetch had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
     }
   }
   
   /** Create a URL to obtain multiple page's urls, given the page IDs.
   */
   protected String getGetDocURLsURL(String[] documentIdentifiers)
     throws ManifoldCFException
   {
     StringBuilder sb = new StringBuilder();
     for (int i = 0 ; i < documentIdentifiers.length ; i++)
     {
       if (i > 0)
         sb.append("|");
       sb.append(documentIdentifiers[i]);
     }
     try
     {
       return baseURL + "action=query&prop=info&pageids="+URLEncoder.encode(sb.toString(),"utf-8")+"&inprop=url";
     }
     catch (UnsupportedEncodingException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
 
   /** Thread to execute a "get timestamp" operation.  This thread both executes the operation and parses the result. */
   protected static class ExecuteGetDocURLsThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
     protected Map<String,String> urls;
 
     public ExecuteGetDocURLsThread(HttpClient client, HttpMethodBase executeMethod, Map<String,String> urls)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
       this.urls = urls;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
           throw new ManifoldCFException("Unexpected response code: "+rval);
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           parseGetDocURLsResponse(is,urls);
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         this.exception = e;
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
   }
 
   /** This method parses a response like the following:
   * <api>
   *   <query>
   *     <pages>
   *       <page pageid="27697087" ns="0" title="API" fullurl="..."/>
   *     </pages>
   *   </query>
   * </api>
   */
   protected static void parseGetDocURLsResponse(InputStream is, Map<String,String> urls)
     throws ManifoldCFException, ServiceInterruption
   {
     // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
     XMLStream x = new XMLStream();
     WikiGetDocURLsAPIContext c = new WikiGetDocURLsAPIContext(x,urls);
     x.setContext(c);
     try
     {
       try
       {
         x.parse(is);
       }
       catch (IOException e)
       {
         long time = System.currentTimeMillis();
         throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
       }
     }
     finally
     {
       x.cleanup();
     }
   }
 
   /** Class representing the "api" context of a "get timestamp" response */
   protected static class WikiGetDocURLsAPIContext extends SingleLevelContext
   {
     protected Map<String,String> urls;
     
     public WikiGetDocURLsAPIContext(XMLStream theStream, Map<String,String> urls)
     {
       super(theStream,"api");
       this.urls = urls;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocURLsQueryContext(theStream,namespaceURI,localName,qName,atts,urls);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
 
   }
 
   /** Class representing the "api/query" context of a "get timestamp" response */
   protected static class WikiGetDocURLsQueryContext extends SingleLevelContext
   {
     protected Map<String,String> urls;
     
     public WikiGetDocURLsQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> urls)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
       this.urls = urls;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocURLsPagesContext(theStream,namespaceURI,localName,qName,atts,urls);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
     
   }
 
   /** Class looking for the "api/query/pages" context of a "get timestamp" response */
   protected static class WikiGetDocURLsPagesContext extends SingleLevelContext
   {
     protected Map<String,String> urls;
     
     public WikiGetDocURLsPagesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> urls)
     {
       super(theStream,namespaceURI,localName,qName,atts,"pages");
       this.urls = urls;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocURLsPageContext(theStream,namespaceURI,localName,qName,atts,urls);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
   }
 
   /** Class looking for the "api/query/pages/page" context of a "get timestamp" response */
   protected static class WikiGetDocURLsPageContext extends BaseProcessingContext
   {
     protected Map<String,String> urls;
     
     public WikiGetDocURLsPageContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> urls)
     {
       super(theStream,namespaceURI,localName,qName,atts);
       this.urls = urls;
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("page"))
       {
         String pageID = atts.getValue("pageid");
         String fullURL = atts.getValue("fullurl");
         if (pageID != null && fullURL != null)
           urls.put(pageID,fullURL);
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
   }
 
   // -- Methods and classes to perform a "get Timestamp" operation. --
 
   /** Obtain document versions for a set of documents.
   */
   protected void getTimestamps(String[] documentIdentifiers, Map<String,String> versions, IVersionActivity activities)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getGetTimestampURL(documentIdentifiers));
     try
     {
       ExecuteGetTimestampThread t = new ExecuteGetTimestampThread(client,executeMethod,versions);
       try
       {
         t.start();
         t.join();
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         // Drop the connection on the floor
         executeMethod = null;
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Version fetch timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Version fetch received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Version fetch connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Version fetch had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
     }
   }
 
   /** Create a URL to obtain multiple page's timestamps, given the page IDs.
   */
   protected String getGetTimestampURL(String[] documentIdentifiers)
     throws ManifoldCFException
   {
     StringBuilder sb = new StringBuilder();
     for (int i = 0 ; i < documentIdentifiers.length ; i++)
     {
       if (i > 0)
         sb.append("|");
       sb.append(documentIdentifiers[i]);
     }
     try
     {
       return baseURL + "action=query&prop=revisions&pageids="+URLEncoder.encode(sb.toString(),"utf-8")+"&rvprop=timestamp";
     }
     catch (UnsupportedEncodingException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
 
   /** Thread to execute a "get timestamp" operation.  This thread both executes the operation and parses the result. */
   protected static class ExecuteGetTimestampThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
     protected Map<String,String> versions;
 
     public ExecuteGetTimestampThread(HttpClient client, HttpMethodBase executeMethod, Map<String,String> versions)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
       this.versions = versions;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
           throw new ManifoldCFException("Unexpected response code: "+rval);
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           parseGetTimestampResponse(is,versions);
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         this.exception = e;
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
   }
 
   /** This method parses a response like the following:
   * <api>
   *   <query>
   *     <pages>
   *       <page pageid="27697087" ns="0" title="API">
   *         <revisions>
   *           <rev user="Graham87" timestamp="2010-06-13T08:41:17Z" />
   *         </revisions>
   *       </page>
   *     </pages>
   *   </query>
   * </api>
   */
   protected static void parseGetTimestampResponse(InputStream is, Map<String,String> versions)
     throws ManifoldCFException, ServiceInterruption
   {
     // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
     XMLStream x = new XMLStream();
     WikiGetTimestampAPIContext c = new WikiGetTimestampAPIContext(x,versions);
     x.setContext(c);
     try
     {
       try
       {
         x.parse(is);
       }
       catch (IOException e)
       {
         long time = System.currentTimeMillis();
         throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
       }
     }
     finally
     {
       x.cleanup();
     }
   }
 
   /** Class representing the "api" context of a "get timestamp" response */
   protected static class WikiGetTimestampAPIContext extends SingleLevelContext
   {
     protected Map<String,String> versions;
     
     public WikiGetTimestampAPIContext(XMLStream theStream, Map<String,String> versions)
     {
       super(theStream,"api");
       this.versions = versions;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetTimestampQueryContext(theStream,namespaceURI,localName,qName,atts,versions);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
 
   }
 
   /** Class representing the "api/query" context of a "get timestamp" response */
   protected static class WikiGetTimestampQueryContext extends SingleLevelContext
   {
     protected Map<String,String> versions;
     
     public WikiGetTimestampQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> versions)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
       this.versions = versions;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetTimestampPagesContext(theStream,namespaceURI,localName,qName,atts,versions);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
     
   }
 
   /** Class looking for the "api/query/pages" context of a "get timestamp" response */
   protected static class WikiGetTimestampPagesContext extends SingleLevelContext
   {
     protected Map<String,String> versions;
     
     public WikiGetTimestampPagesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> versions)
     {
       super(theStream,namespaceURI,localName,qName,atts,"pages");
       this.versions = versions;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetTimestampPageContext(theStream,namespaceURI,localName,qName,atts,versions);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
   }
 
   /** Class looking for the "api/query/pages/page" context of a "get timestamp" response */
   protected static class WikiGetTimestampPageContext extends BaseProcessingContext
   {
     protected String pageID = null;
     protected Map<String,String> versions;
     
     public WikiGetTimestampPageContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> versions)
     {
       super(theStream,namespaceURI,localName,qName,atts);
       this.versions = versions;
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("page"))
       {
         pageID = atts.getValue("pageid");
         return new WikiGetTimestampRevisionsContext(theStream,namespaceURI,localName,qName,atts);
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
     protected void endTag()
       throws ManifoldCFException, ServiceInterruption
     {
       XMLContext theContext = theStream.getContext();
       String theTag = theContext.getQname();
 
       if (theTag.equals("page"))
       {
         String lastRevEdit = ((WikiGetTimestampRevisionsContext)theContext).getTimestamp();
         versions.put(pageID,lastRevEdit);
       }
       else
         super.endTag();
     }
     
   }
 
   /** Class looking for the "api/query/pages/page/revisions" context of a "get timestamp" response */
   protected static class WikiGetTimestampRevisionsContext extends SingleLevelContext
   {
     protected String timestamp = null;
     
     public WikiGetTimestampRevisionsContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"revisions");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetTimestampRevContext(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       WikiGetTimestampRevContext rc = (WikiGetTimestampRevContext)child;
       if (timestamp == null)
         timestamp = rc.getTimestamp();
     }
     
     public String getTimestamp()
     {
       return timestamp;
     }
   }
 
   /** Class looking for the "api/query/pages/page/revisions/rev" context of a "get timestamp" response */
   protected static class WikiGetTimestampRevContext extends BaseProcessingContext
   {
     protected String timestamp = null;
     
     public WikiGetTimestampRevContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("rev"))
         timestamp = atts.getValue("timestamp");
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
     public String getTimestamp()
     {
       return timestamp;
     }
   }
   
   // -- Methods and classes to perform a "get namespaces" operation. --
   
   /** Obtain the set of namespaces, as a map keyed by the canonical namespace name
   * where the value is the descriptive name.
   */
   protected void getNamespaces(Map<String,String> namespaces)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getGetNamespacesURL());
     
     try
     {
       ExecuteGetNamespacesThread t = new ExecuteGetNamespacesThread(client,executeMethod,namespaces);
       try
       {
         t.start();
         t.join();
         
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
  
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
         // Drop the connection on the floor
         executeMethod = null;
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get namespaces timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get namespaces received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get namespaces connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Get namespaces had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
     }
   }
   
   /** Thread to execute a "get namespaces" operation.  This thread both executes the operation and parses the result. */
   protected static class ExecuteGetNamespacesThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
     protected Map<String,String> namespaces;
 
     public ExecuteGetNamespacesThread(HttpClient client, HttpMethodBase executeMethod, Map<String,String> namespaces)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
       this.namespaces = namespaces;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
         {
           throw new ManifoldCFException("Unexpected response code "+rval+": "+executeMethod.getResponseBodyAsString());
         }
 
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
           //<api>
           //  <query>
           //    <namespaces>
           //      <ns id="-2" case="first-letter" canonical="Media" xml:space="preserve">Media</ns>
           //      <ns id="-1" case="first-letter" canonical="Special" xml:space="preserve">Special</ns>
           //      <ns id="0" case="first-letter" subpages="" content="" xml:space="preserve" />
           //      <ns id="1" case="first-letter" subpages="" canonical="Talk" xml:space="preserve">Talk</ns>
           //      <ns id="2" case="first-letter" subpages="" canonical="User" xml:space="preserve">User</ns>
           //      <ns id="90" case="first-letter" canonical="Thread" xml:space="preserve">Thread</ns>
           //      <ns id="91" case="first-letter" canonical="Thread talk" xml:space="preserve">Thread talk</ns>
           //    </namespaces>
           //  </query>
           //</api>
           XMLStream x = new XMLStream();
           WikiGetNamespacesAPIContext c = new WikiGetNamespacesAPIContext(x,namespaces);
           x.setContext(c);
           try
           {
             try
             {
               x.parse(is);
             }
             catch (IOException e)
             {
               long time = System.currentTimeMillis();
               throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
             }
           }
           finally
           {
             x.cleanup();
           }
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         this.exception = e;
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
   }
 
   /** Create a URL to obtain the namespaces.
   */
   protected String getGetNamespacesURL()
     throws ManifoldCFException
   {
     return baseURL + "action=query&meta=siteinfo&siprop=namespaces";
   }
 
   /** Class representing the "api" context of a "get namespaces" response */
   protected static class WikiGetNamespacesAPIContext extends SingleLevelContext
   {
     protected Map<String,String> namespaces;
     
     public WikiGetNamespacesAPIContext(XMLStream theStream, Map<String,String> namespaces)
     {
       super(theStream,"api");
       this.namespaces = namespaces;
     }
 
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetNamespacesQueryContext(theStream,namespaceURI,localName,qName,atts,namespaces);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
 
   }
 
   /** Class representing the "api/query" context of a "get namespaces" response */
   protected static class WikiGetNamespacesQueryContext extends SingleLevelContext
   {
     protected Map<String,String> namespaces;
     
     public WikiGetNamespacesQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> namespaces)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
       this.namespaces = namespaces;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetNamespacesNamespacesContext(theStream,namespaceURI,localName,qName,atts,namespaces);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
     
   }
 
   /** Class representing the "api/query/namespaces" context of a "get namespaces" response */
   protected static class WikiGetNamespacesNamespacesContext extends SingleLevelContext
   {
     protected Map<String,String> namespaces;
     
     public WikiGetNamespacesNamespacesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> namespaces)
     {
       super(theStream,namespaceURI,localName,qName,atts,"namespaces");
       this.namespaces = namespaces;
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetNamespacesNsContext(theStream,namespaceURI,localName,qName,atts,namespaces);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
     }
     
   }
 
   /** Class representing the "api/query/pages/page" context of a "get doc info" response */
   protected static class WikiGetNamespacesNsContext extends BaseProcessingContext
   {
     protected Map<String,String> namespaces;
     protected String canonical = null;
     
     public WikiGetNamespacesNsContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts,
       Map<String,String> namespaces)
     {
       super(theStream,namespaceURI,localName,qName,atts);
       this.namespaces = namespaces;
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("ns"))
       {
         canonical = atts.getValue("canonical");
         if (canonical != null)
           return new XMLStringContext(theStream,namespaceURI,localName,qName,atts);
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
     protected void endTag()
       throws ManifoldCFException, ServiceInterruption
     {
       XMLContext theContext = theStream.getContext();
       String theTag = theContext.getQname();
       if (theTag.equals("ns"))
       {
         if (canonical != null)
         {
           // Pull down the data
           XMLStringContext sc = (XMLStringContext)theContext;
           namespaces.put(sc.getValue(),canonical);
         }
         else
           super.endTag();
       }
       else
         super.endTag();
     }
 
     protected void tagCleanup()
       throws ManifoldCFException
     {
     }
    
   }
   
   // -- Methods and classes to perform a "get Docinfo" operation. --
 
   /** Get document info and index the document.
   */
   protected void getDocInfo(String documentIdentifier, String documentVersion, String fullURL, IProcessActivity activities)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     HttpClient client = getInitializedClient();
     HttpMethodBase executeMethod = getInitializedMethod(getGetDocInfoURL(documentIdentifier));
     
     String statusCode = "UNKNOWN";
     String errorMessage = null;
     long startTime = System.currentTimeMillis();
     long dataSize = 0L;
     
     try
     {
       ExecuteGetDocInfoThread t = new ExecuteGetDocInfoThread(client,executeMethod,documentIdentifier);
       try
       {
         t.start();
         t.join();
         
         statusCode = t.getStatusCode();
         errorMessage = t.getErrorMessage();
           
         Throwable thr = t.getException();
         if (thr != null)
         {
           if (thr instanceof ManifoldCFException)
           {
             if (((ManifoldCFException)thr).getErrorCode() == ManifoldCFException.INTERRUPTED)
               throw new InterruptedException(thr.getMessage());
             throw (ManifoldCFException)thr;
           }
           else if (thr instanceof ServiceInterruption)
             throw (ServiceInterruption)thr;
           else if (thr instanceof IOException)
             throw (IOException)thr;
           else if (thr instanceof RuntimeException)
             throw (RuntimeException)thr;
           else
             throw (Error)thr;
         }
  
         // Fetch all the data we need from the thread, and do the indexing.
         File contentFile = t.getContentFile();
         if (contentFile != null)
         {
           statusCode = "OK";
           try
           {
             String author = t.getAuthor();
             String comment = t.getComment();
             String title = t.getTitle();
             String lastModified = t.getLastModified();
             
             RepositoryDocument rd = new RepositoryDocument();
             dataSize = contentFile.length();
             InputStream is = new FileInputStream(contentFile);
             try
             {
               rd.setBinary(is,dataSize);
               if (comment != null)
                 rd.addField("comment",comment);
               if (author != null)
                 rd.addField("author",author);
               if (title != null)
                 rd.addField("title",title);
               if (lastModified != null)
                 rd.addField("last-modified",lastModified);
               activities.ingestDocument(documentIdentifier,documentVersion,fullURL,rd);
             }
             finally
             {
               is.close();
             }
           }
           finally
           {
             contentFile.delete();
           }
         }
       }
       catch (ManifoldCFException e)
       {
         t.interrupt();
         throw e;
       }
       catch (ServiceInterruption e)
       {
         t.interrupt();
         throw e;
       }
       catch (IOException e)
       {
         t.interrupt();
         throw e;
       }
       catch (InterruptedException e)
       {
         t.interrupt();
         // We need the caller to abandon any connections left around, so rethrow in a way that forces them to process the event properly.
         throw e;
       }
       finally
       {
         t.cleanup();
       }
     }
     catch (InterruptedException e)
     {
       // Drop the connection on the floor
       executeMethod = null;
       statusCode = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (ManifoldCFException e)
     {
       if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
       {
         // Drop the connection on the floor
         executeMethod = null;
         statusCode = null;
       }
       throw e;
     }
     catch (java.net.SocketTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get doc info timed out reading from the Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (java.net.SocketException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get doc info received a socket error reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (org.apache.commons.httpclient.ConnectTimeoutException e)
     {
       long currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Get doc info connection timed out reading from Wiki server: "+e.getMessage(),e,currentTime+300000L,currentTime+12L * 60000L,-1,false);
     }
     catch (InterruptedIOException e)
     {
       executeMethod = null;
       statusCode = null;
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Get doc info had an IO failure: "+e.getMessage(),e);
     }
     finally
     {
       if (executeMethod != null)
         executeMethod.releaseConnection();
       if (statusCode != null)
         activities.recordActivity(new Long(startTime),ACTIVITY_FETCH,new Long(dataSize),documentIdentifier,statusCode,errorMessage,null);
     }
   }
   
   /** Thread to execute a "get doc info" operation.  This thread both executes the operation and parses the result. */
   protected static class ExecuteGetDocInfoThread extends Thread
   {
     protected HttpClient client;
     protected HttpMethodBase executeMethod;
     protected Throwable exception = null;
     protected String documentIdentifier;
     protected File contentFile = null;
     protected String author = null;
     protected String title = null;
     protected String comment = null;
     protected String lastModified = null;
     
     protected String statusCode = null;
     protected String errorMessage = null;
 
     public ExecuteGetDocInfoThread(HttpClient client, HttpMethodBase executeMethod, String documentIdentifier)
     {
       super();
       setDaemon(true);
       this.client = client;
       this.executeMethod = executeMethod;
       this.documentIdentifier = documentIdentifier;
     }
 
     public void run()
     {
       try
       {
         // Call the execute method appropriately
         int rval = client.executeMethod(executeMethod);
         if (rval != 200)
         {
           statusCode = "HTTP code "+rval;
           throw new ManifoldCFException("Unexpected response code "+rval+": "+executeMethod.getResponseBodyAsString());
         }
         // Read response and make sure it's valid
         InputStream is = executeMethod.getResponseBodyAsStream();
         try
         {
           // Parse the document.  This will cause various things to occur, within the instantiated XMLContext class.
           // <api>
           //  <query>
           //    <pages>
           //      <page pageid="27697087" ns="0" title="API" touched="2011-09-27T07:00:55Z" lastrevid="367741756" counter="" length="70" redirect="" fullurl="http://en.wikipedia.org/wiki/API" editurl="http://en.wikipedia.org/w/index.php?title=API&amp;action=edit">
           //        <revisions>
           //          <rev user="Graham87" timestamp="2010-06-13T08:41:17Z" comment="Protected API: restore protection ([edit=sysop] (indefinite) [move=sysop] (indefinite))" xml:space="preserve">#REDIRECT [[Application programming interface]]{{R from abbreviation}}</rev>
           //        </revisions>
           //      </page>
           //    </pages>
           //  </query>
           //</api>
 
           XMLStream x = new XMLStream();
           WikiGetDocInfoAPIContext c = new WikiGetDocInfoAPIContext(x);
           x.setContext(c);
           try
           {
             try
             {
               x.parse(is);
               contentFile = c.getContentFile();
               title = c.getTitle();
               author = c.getAuthor();
               comment = c.getComment();
               lastModified = c.getLastModified();
               statusCode = "OK";
             }
             catch (IOException e)
             {
               long time = System.currentTimeMillis();
               throw new ServiceInterruption(e.getMessage(),e,time + 300000L,time + 12L * 60000L,-1,false);
             }
           }
           finally
           {
             x.cleanup();
           }
         }
         finally
         {
           try
           {
             is.close();
           }
           catch (IllegalStateException e)
           {
             // Ignore this error
           }
         }
       }
       catch (Throwable e)
       {
         statusCode = "Exception";
         errorMessage = e.getMessage();
         this.exception = e;
       }
     }
 
     public Throwable getException()
     {
       return exception;
     }
 
     public String getStatusCode()
     {
       return statusCode;
     }
     
     public String getErrorMessage()
     {
       return errorMessage;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
     
     public String getComment()
     {
       return comment;
     }
     
     public String getTitle()
     {
       return title;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
     public void cleanup()
     {
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
     
   }
 
   /** Create a URL to obtain a page's metadata and content, given the page ID.
   * QUESTION: Can we do multiple document identifiers at a time??
   */
   protected String getGetDocInfoURL(String documentIdentifier)
     throws ManifoldCFException
   {
     return baseURL + "action=query&prop=revisions&pageids="+documentIdentifier+"&rvprop=user%7ccomment%7ccontent%7ctimestamp";
   }
 
   /** Class representing the "api" context of a "get doc info" response */
   protected static class WikiGetDocInfoAPIContext extends SingleLevelContext
   {
     /** Title */
     protected String title = null;
     /** Content file */
     protected File contentFile = null;
     /** Author */
     protected String author = null;
     /** Comment */
     protected String comment = null;
     /** Last modified */
     protected String lastModified = null;
     
     public WikiGetDocInfoAPIContext(XMLStream theStream)
     {
       super(theStream,"api");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocInfoQueryContext(theStream,namespaceURI,localName,qName,atts);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       WikiGetDocInfoQueryContext pc = (WikiGetDocInfoQueryContext)child;
       tagCleanup();
       title = pc.getTitle();
       contentFile = pc.getContentFile();
       author = pc.getAuthor();
       comment = pc.getComment();
       lastModified = pc.getLastModified();
     }
     
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
 
     public String getTitle()
     {
       return title;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
     public String getComment()
     {
       return comment;
     }
 
   }
 
   /** Class representing the "api/query" context of a "get doc info" response */
   protected static class WikiGetDocInfoQueryContext extends SingleLevelContext
   {
     /** Title */
     protected String title = null;
     /** Content file */
     protected File contentFile = null;
     /** Author */
     protected String author = null;
     /** Comment */
     protected String comment = null;
     /** Last modified */
     protected String lastModified = null;
     
     public WikiGetDocInfoQueryContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"query");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocInfoPagesContext(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       WikiGetDocInfoPagesContext pc = (WikiGetDocInfoPagesContext)child;
       tagCleanup();
       title = pc.getTitle();
       contentFile = pc.getContentFile();
       author = pc.getAuthor();
       comment = pc.getComment();
       lastModified = pc.getLastModified();
     }
     
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
 
     public String getTitle()
     {
       return title;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
     public String getComment()
     {
       return comment;
     }
     
   }
 
   /** Class representing the "api/query/pages" context of a "get doc info" response */
   protected static class WikiGetDocInfoPagesContext extends SingleLevelContext
   {
     /** Title */
     protected String title = null;
     /** Content file */
     protected File contentFile = null;
     /** Author */
     protected String author = null;
     /** Comment */
     protected String comment = null;
     /** Last modified */
     protected String lastModified = null;
     
     public WikiGetDocInfoPagesContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"pages");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocInfoPageContext(theStream,namespaceURI,localName,qName,atts);
     }
     
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       WikiGetDocInfoPageContext pc = (WikiGetDocInfoPageContext)child;
       tagCleanup();
       title = pc.getTitle();
       contentFile = pc.getContentFile();
       author = pc.getAuthor();
       lastModified = pc.getLastModified();
       comment = pc.getComment();
     }
     
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
 
     public String getTitle()
     {
       return title;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
     public String getComment()
     {
       return comment;
     }
 
   }
 
   /** Class representing the "api/query/pages/page" context of a "get doc info" response */
   protected static class WikiGetDocInfoPageContext extends BaseProcessingContext
   {
     /** Title */
     protected String title = null;
     /** Content file */
     protected File contentFile = null;
     /** Author */
     protected String author = null;
     /** Comment */
     protected String comment = null;
     /** Last modified */
     protected String lastModified = null;
     
     public WikiGetDocInfoPageContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("page"))
       {
         title = atts.getValue("title");
         return new WikiGetDocInfoRevisionsContext(theStream,namespaceURI,localName,qName,atts);
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
     
     protected void endTag()
       throws ManifoldCFException, ServiceInterruption
     {
       XMLContext theContext = theStream.getContext();
       String theTag = theContext.getQname();
       if (theTag.equals("page"))
       {
         // Pull down the data
         WikiGetDocInfoRevisionsContext rc = (WikiGetDocInfoRevisionsContext)theContext;
         tagCleanup();
         contentFile = rc.getContentFile();
         author = rc.getAuthor();
         comment = rc.getComment();
         lastModified = rc.getLastModified();
       }
       super.endTag();
     }
 
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
 
     public String getTitle()
     {
       return title;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
     
     public String getComment()
     {
       return comment;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
   }
 
   /** Class representing the "api/query/pages/page/revisions" context of a "get doc info" response */
   protected static class WikiGetDocInfoRevisionsContext extends SingleLevelContext
   {
     protected File contentFile = null;
     protected String author = null;
     protected String comment = null;
     protected String lastModified = null;
     
     public WikiGetDocInfoRevisionsContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts,"revisions");
     }
 
     protected BaseProcessingContext createChild(String namespaceURI, String localName, String qName, Attributes atts)
     {
       return new WikiGetDocInfoRevContext(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected void finishChild(BaseProcessingContext child)
       throws ManifoldCFException
     {
       WikiGetDocInfoRevContext rc = (WikiGetDocInfoRevContext)child;
       tagCleanup();
       contentFile = rc.getContentFile();
       author = rc.getAuthor();
       comment = rc.getComment();
       lastModified = rc.getLastModified();
     }
     
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
 
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
     public String getAuthor()
     {
       return author;
     }
     
     public String getComment()
     {
       return comment;
     }
     
     public String getLastModified()
     {
       return lastModified;
     }
   }
 
   /** Class looking for the "api/query/pages/page/revisions/rev" context of a "get doc info" response */
   protected static class WikiGetDocInfoRevContext extends BaseProcessingContext
   {
     protected String author = null;
     protected String comment = null;
     protected File contentFile = null;
     protected String lastModified = null;
     
     public WikiGetDocInfoRevContext(XMLStream theStream, String namespaceURI, String localName, String qName, Attributes atts)
     {
       super(theStream,namespaceURI,localName,qName,atts);
     }
 
     protected XMLContext beginTag(String namespaceURI, String localName, String qName, Attributes atts)
       throws ManifoldCFException, ServiceInterruption
     {
       if (qName.equals("rev"))
       {
         author = atts.getValue("user");
         comment = atts.getValue("comment");
         lastModified = atts.getValue("timestamp");
         try
         {
           File tempFile = File.createTempFile("_wikidata_","tmp");
           return new XMLFileContext(theStream,namespaceURI,localName,qName,atts,tempFile);
         }
         catch (java.net.SocketTimeoutException e)
         {
           throw new ManifoldCFException("IO exception creating temp file: "+e.getMessage(),e);
         }
         catch (InterruptedIOException e)
         {
           throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
         }
         catch (IOException e)
         {
           throw new ManifoldCFException("IO exception creating temp file: "+e.getMessage(),e);
         }
       }
       return super.beginTag(namespaceURI,localName,qName,atts);
     }
 
     protected void endTag()
       throws ManifoldCFException, ServiceInterruption
     {
       XMLContext theContext = theStream.getContext();
       String theTag = theContext.getQname();
       if (theTag.equals("rev"))
       {
         // Pull down the data
         XMLFileContext rc = (XMLFileContext)theContext;
         tagCleanup();
         contentFile = rc.getCompletedFile();
       }
       else
         super.endTag();
     }
 
     protected void tagCleanup()
       throws ManifoldCFException
     {
       // Delete the contents file if it is there.
       if (contentFile != null)
       {
         contentFile.delete();
         contentFile = null;
       }
     }
     
     public String getAuthor()
     {
       return author;
     }
 
     public String getLastModified()
     {
       return lastModified;
     }
     
     public String getComment()
     {
       return comment;
     }
     
     public File getContentFile()
     {
       File rval = contentFile;
       contentFile = null;
       return rval;
     }
     
   }
   
 }
