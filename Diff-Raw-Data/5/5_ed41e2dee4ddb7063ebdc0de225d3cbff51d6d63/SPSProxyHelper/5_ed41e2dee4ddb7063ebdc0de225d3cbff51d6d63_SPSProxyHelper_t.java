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
 
 package org.apache.manifoldcf.crawler.connectors.sharepoint;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.regex.*;
 
 import java.io.InputStream;
 
 import javax.xml.soap.*;
 
 import org.apache.manifoldcf.core.common.XMLDoc;
 import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
 import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
 import org.apache.manifoldcf.crawler.system.Logging;
 
 import com.microsoft.schemas.sharepoint.dsp.*;
 import com.microsoft.schemas.sharepoint.soap.*;
 
 import org.apache.commons.httpclient.protocol.Protocol;
 import org.apache.commons.httpclient.protocol.ProtocolFactory;
 import org.apache.commons.httpclient.HttpConnectionManager;
 import org.apache.axis.EngineConfiguration;
 
 import javax.xml.namespace.QName;
 
 import org.apache.axis.message.MessageElement;
 import org.apache.axis.AxisEngine;
 import org.apache.axis.ConfigurationException;
 import org.apache.axis.Handler;
 import org.apache.axis.WSDDEngineConfiguration;
 import org.apache.axis.components.logger.LogFactory;
 import org.apache.axis.deployment.wsdd.WSDDDeployment;
 import org.apache.axis.deployment.wsdd.WSDDDocument;
 import org.apache.axis.deployment.wsdd.WSDDGlobalConfiguration;
 import org.apache.axis.encoding.TypeMappingRegistry;
 import org.apache.axis.handlers.soap.SOAPService;
 import org.apache.axis.utils.Admin;
 import org.apache.axis.utils.Messages;
 import org.apache.axis.utils.XMLUtils;
 import org.w3c.dom.Document;
 
 /**
 *
 * @author Michael Cummings
 *
 */
 public class SPSProxyHelper {
 
 
   public static final String PROTOCOL_FACTORY_PROPERTY = "ManifoldCF_Protocol_Factory";
   public static final String CONNECTION_MANAGER_PROPERTY = "ManifoldCF_Connection_Manager";
 
   private String serverUrl;
   private String serverLocation;
   private String decodedServerLocation;
   private String baseUrl;
   private String userName;
   private String password;
   private ProtocolFactory myFactory;
   private EngineConfiguration configuration;
   private HttpConnectionManager connectionManager;
 
   /**
   *
   * @param serverUrl
   * @param userName
   * @param password
   */
   public SPSProxyHelper( String serverUrl, String serverLocation, String decodedServerLocation, String userName, String password,
     ProtocolFactory myFactory, Class resourceClass, String configFileName, HttpConnectionManager connectionManager )
   {
     this.serverUrl = serverUrl;
     this.serverLocation = serverLocation;
     this.decodedServerLocation = decodedServerLocation;
     if (serverLocation.equals("/"))
       baseUrl = serverUrl;
     else
       baseUrl = serverUrl + serverLocation;
     this.userName = userName;
     this.password = password;
     this.myFactory = myFactory;
     this.configuration = new ResourceProvider(resourceClass,configFileName);
     this.connectionManager = connectionManager;
   }
 
   /**
   * Get the acls for a document library.
   * @param site
   * @param guid is the list/library GUID
   * @return array of sids
   * @throws Exception
   */
   public String[] getACLs(String site, String guid )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       if ( site.compareTo("/") == 0 ) site = ""; // root case
         UserGroupWS userService = new UserGroupWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager  );
       com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap userCall = userService.getUserGroupSoapHandler( );
 
       PermissionsWS aclService = new PermissionsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
       com.microsoft.schemas.sharepoint.soap.directory.PermissionsSoap aclCall = aclService.getPermissionsSoapHandler( );
 
       com.microsoft.schemas.sharepoint.soap.directory.GetPermissionCollectionResponseGetPermissionCollectionResult aclResult = aclCall.getPermissionCollection( guid, "List" );
       org.apache.axis.message.MessageElement[] aclList = aclResult.get_any();
 
       XMLDoc doc = new XMLDoc( aclList[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:GetPermissionCollection' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:GetPermissionCollection"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:GetPermissionCollection'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);
 
       if ( nodeList.size() != 1 )
       {
         throw new ManifoldCFException( " No results found." );
       }
       parent = nodeList.get(0);
       nodeList.clear();
       doc.processPath( nodeList, "*", parent );
       java.util.HashSet sids = new java.util.HashSet();
       int i = 0;
       for (; i< nodeList.size(); i++ )
       {
         Object node = nodeList.get( i );
         String mask = doc.getValue( node, "Mask" );
         long maskValue = new Long(mask).longValue();
         if ((maskValue & 1L) == 1L)
         {
           // Permission to view
           String isUser = doc.getValue( node, "MemberIsUser" );
 
           if ( isUser.compareToIgnoreCase("True") == 0 )
           {
             // Use AD user or group
             String userLogin = doc.getValue( node, "UserLogin" );
             String userSid = getSidForUser( userCall, userLogin );
             sids.add( userSid );
           }
           else
           {
             // Role
             String[] roleSids;
             String roleName = doc.getValue( node, "RoleName" );
             if ( roleName.length() == 0)
             {
               roleName = doc.getValue(node,"GroupName");
               roleSids = getSidsForGroup(userCall, roleName);
             }
             else
             {
               roleSids = getSidsForRole(userCall, roleName);
             }
 
             int j = 0;
             for (; j < roleSids.length; j++ )
             {
               sids.add( roleSids[ j ] );
             }
           }
         }
       }
 
       return (String[]) sids.toArray( new String[0] );
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting the acls for site "+site+" guid "+guid+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+site+" did not exist; assuming list/library deleted");
             return null;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to get the acls
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to the permissions service for "+baseUrl+site+"; skipping documents within");
             return null;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+site+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The list "+guid+" in site "+site+" did not exist; assuming list/library deleted");
             return null;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting permissions for the list "+guid+" in site "+site+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return null;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting the acls for site "+site+" guid "+guid+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying", e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting the acls for site "+site+" guid "+guid+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting the acls for site "+site+" guid "+guid,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   * Get the acls for a document.
   * NOTE that this function only works for SharePoint 2007+ with the MCPermissions web service installed.
   * @param site is the encoded subsite path
   * @param file is the encoded file url (not including protocol or server or location, but including encoded subsite, library and folder/file path)
   * @return array of document SIDs
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
   public String[] getDocumentACLs(String site, String file)
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       if ( site.compareTo("/") == 0 ) site = ""; // root case
 
       // Calculate the full server-relative path of the file
       String encodedRelativePath = serverLocation + file;
       if (encodedRelativePath.startsWith("/"))
         encodedRelativePath = encodedRelativePath.substring(1);
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Getting document acls for site '"+site+"' file '"+file+"': Encoded relative path is '"+encodedRelativePath+"'");
       UserGroupWS userService = new UserGroupWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager  );
       com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap userCall = userService.getUserGroupSoapHandler( );
 
       MCPermissionsWS aclService = new MCPermissionsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
       com.microsoft.sharepoint.webpartpages.PermissionsSoap aclCall = aclService.getPermissionsSoapHandler( );
 
       com.microsoft.sharepoint.webpartpages.GetPermissionCollectionResponseGetPermissionCollectionResult aclResult = aclCall.getPermissionCollection( encodedRelativePath, "Item" );
       org.apache.axis.message.MessageElement[] aclList = aclResult.get_any();
 
       if (Logging.connectors.isDebugEnabled())
       {
         Logging.connectors.debug("SharePoint: document acls xml: '" + aclList[0].toString() + "'");
       }
 
       XMLDoc doc = new XMLDoc( aclList[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:GetPermissionCollection' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("GetPermissionCollection"))
         throw new ManifoldCFException("Bad xml - outer node is not 'GetPermissionCollection'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);
 
       if ( nodeList.size() != 1 )
       {
         throw new ManifoldCFException( " No results found." );
       }
       parent = nodeList.get(0);
       nodeList.clear();
       doc.processPath( nodeList, "*", parent );
       java.util.HashSet sids = new java.util.HashSet();
       int i = 0;
       for (; i< nodeList.size(); i++ )
       {
         Object node = nodeList.get( i );
         String mask = doc.getValue( node, "Mask" );
         long maskValue = new Long(mask).longValue();
         if ((maskValue & 1L) == 1L)
         {
           // Permission to view
           String isUser = doc.getValue( node, "MemberIsUser" );
 
           if ( isUser.compareToIgnoreCase("True") == 0 )
           {
             // Use AD user or group
             String userLogin = doc.getValue( node, "UserLogin" );
             String userSid = getSidForUser( userCall, userLogin );
             sids.add( userSid );
           }
           else
           {
             // Role
             String[] roleSids;
             String roleName = doc.getValue( node, "RoleName" );
             if ( roleName.length() == 0)
             {
               roleName = doc.getValue(node,"GroupName");
               roleSids = getSidsForGroup(userCall, roleName);
             }
             else
             {
               roleSids = getSidsForRole(userCall, roleName);
             }
 
             int j = 0;
             for (; j < roleSids.length; j++ )
             {
               sids.add( roleSids[ j ] );
             }
           }
         }
       }
 
       return (String[]) sids.toArray( new String[0] );
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting the acls for site "+site+" file "+file+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+site+" did not exist; assuming library deleted");
             return null;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to get the acls
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to the MCPermissions service for "+baseUrl+site+"; skipping documents within");
             return null;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+site+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The file "+file+" in site "+site+" did not exist; assuming file deleted");
             return null;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting permissions for the file "+file+" in site "+site+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return null;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting the acls for site "+site+" file "+file+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying", e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting the acls for site "+site+" file "+file+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting the acls for site "+site+" file "+file,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   *
   * @param site
   * @param docLibrary
   * @return an XML document
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
   public boolean getChildren(IFileStream fileStream, String site, String guid, boolean dspStsWorks )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       if ( site.equals("/") ) site = ""; // root case
       if ( dspStsWorks )
       {
         StsAdapterWS listService = new StsAdapterWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
         StsAdapterSoapStub stub = (StsAdapterSoapStub)listService.getStsAdapterSoapHandler();
 
         String[] vArray = new String[1];
         vArray[0] = "1.0";
         VersionsHeader myVersion = new VersionsHeader();
         myVersion.setVersion( vArray );
 
         stub.setHeader( "http://schemas.microsoft.com/sharepoint/dsp", "versions", myVersion );
 
         RequestHeader reqHeader = new RequestHeader();
         reqHeader.setDocument( DocumentType.content );
         reqHeader.setMethod(MethodType.query );
 
         stub.setHeader( "http://schemas.microsoft.com/sharepoint/dsp", "request", reqHeader );
 
         QueryRequest myRequest = new QueryRequest();
 
         DSQuery sQuery = new DSQuery();
         sQuery.setSelect( "/list[@id='" + guid + "']" );
         myRequest.setDsQuery( sQuery );
 
         StsAdapterSoap call = stub;
         ArrayList nodeList = new ArrayList();
 
         QueryResponse resp = call.query( myRequest );
         org.apache.axis.message.MessageElement[] list = resp.get_any();
         if (Logging.connectors.isDebugEnabled())
         {
           Logging.connectors.debug("SharePoint: list xml: '" + list[0].toString() + "'");
         }
 
         XMLDoc doc = new XMLDoc( list[0].toString() );
 
         doc.processPath(nodeList, "*", null);
         if (nodeList.size() != 1)
         {
           throw new ManifoldCFException("Bad xml - missing outer 'ns1:dsQueryResponse' node - there are "+Integer.toString(nodeList.size())+" nodes");
         }
 
         Object parent = nodeList.get(0);
         //System.out.println( "Outer NodeName = " + doc.getNodeName(parent) );
         if (!doc.getNodeName(parent).equals("ns1:dsQueryResponse"))
           throw new ManifoldCFException("Bad xml - outer node is not 'ns1:dsQueryResponse'");
 
         nodeList.clear();
         doc.processPath(nodeList, "*", parent);
 
         if ( nodeList.size() != 2 )
         {
           throw new ManifoldCFException( " No results found." );
         }
 
         // Now, extract the files from the response document
         XMLDoc docs = doc;
         ArrayList nodeDocs = new ArrayList();
 
         docs.processPath( nodeDocs, "*", null );
         parent = nodeDocs.get(0);                // ns1:dsQueryResponse
         nodeDocs.clear();
         docs.processPath(nodeDocs, "*", parent);
         Object documents = nodeDocs.get(1);
         nodeDocs.clear();
         docs.processPath(nodeDocs, "*", documents);
 
         StringBuilder sb = new StringBuilder();
         for( int j =0; j < nodeDocs.size(); j++)
         {
           Object node = nodeDocs.get(j);
           Logging.connectors.debug( node.toString() );
           String relPath = docs.getData( docs.getElement( node, "FileRef" ) );
 
           // This relative path is apparently from the domain on down; if there's a location offset we therefore
           // need to get rid of it before checking the document against the site/library tuples.  The recorded
           // document identifier should also not include it.
 
           if (!relPath.toLowerCase().startsWith(serverLocation.toLowerCase()))
           {
             // Unexpected processing error; the path to the folder or document did not start with the location
             // offset, so throw up.
             throw new ManifoldCFException("Internal error: Relative path '"+relPath+"' was expected to start with '"+
               serverLocation+"'");
           }
 
           relPath = relPath.substring(serverLocation.length());
 
           fileStream.addFile( relPath );
         }
       }
       else
       {
         // New code
         
         MCPermissionsWS itemService = new MCPermissionsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
         com.microsoft.sharepoint.webpartpages.PermissionsSoap itemCall = itemService.getPermissionsSoapHandler( );
 
         int startingIndex = 0;
         int amtToRequest = 10000;
         while (true)
         {
           com.microsoft.sharepoint.webpartpages.GetListItemsResponseGetListItemsResult itemsResult =
             itemCall.getListItems(guid,Integer.toString(startingIndex),Integer.toString(amtToRequest));
           
           MessageElement[] itemsList = itemsResult.get_any();
 
           if (Logging.connectors.isDebugEnabled()){
             Logging.connectors.debug("SharePoint: getListItems xml response: '" + itemsList[0].toString() + "'");
           }
 
           if (itemsList.length != 1)
             throw new ManifoldCFException("Bad response - expecting one outer 'GetListItems' node, saw "+Integer.toString(itemsList.length));
           
           MessageElement items = itemsList[0];
           if (!items.getElementName().getLocalName().equals("GetListItems"))
             throw new ManifoldCFException("Bad response - outer node should have been 'GetListItems' node");
           
           int resultCount = 0;
           Iterator iter = items.getChildElements();
           while (iter.hasNext())
           {
             MessageElement child = (MessageElement)iter.next();
             if (child.getElementName().getLocalName().equals("GetListItemsResponse"))
             {
               Iterator resultIter = child.getChildElements();
               while (resultIter.hasNext())
               {
                 MessageElement result = (MessageElement)resultIter.next();
                 if (result.getElementName().getLocalName().equals("GetListItemsResult"))
                 {
                   resultCount++;
                   String relPath = result.getAttribute("FileRef");
 
                   relPath = "/" + relPath;
 
                   fileStream.addFile( relPath );
                 }
               }
               
             }
           }
           
           if (resultCount < amtToRequest)
             break;
           
           startingIndex += resultCount;
         }
       }
       
       return true;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting documents for site "+site+" guid "+guid+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+site+" did not exist; assuming library deleted");
             return false;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to get the acls
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to list documents for "+baseUrl+site+"; skipping documents within");
             return false;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+site+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The list "+guid+" in site "+site+" did not exist; assuming library deleted");
             return false;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting child documents for the list "+guid+" in site "+site+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return false;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting child documents for site "+site+" guid "+guid+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying",  e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting child documents for site "+site+" guid "+guid+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(),  e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting child documents for site "+site+" guid "+guid,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   *
   * @param parentSite
   * @param docLibrary
   * @return document library ID
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
   public String getDocLibID(String parentSite, String parentSiteDecoded, String docLibrary)
     throws ServiceInterruption, ManifoldCFException
   {
     long currentTime;
     try
     {
       // The old code here used to call the lists service to find the guid, using the doc library url name as the title.
       // This did not work when the title differed from the url name.
       // On 5/8/2008 I modified the code to use the lists service to locate the correct record by matching the defaultViewUrl field,
       // so that we instead iterate through the children.  It's more expensive but it works.
       String parentSiteRequest = parentSite;
 
       if ( parentSiteRequest.equals("/"))
       {
         parentSiteRequest = ""; // root case
         parentSiteDecoded = "";
       }
 
       ListsWS listsService = new ListsWS( baseUrl + parentSiteRequest, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listsCall = listsService.getListsSoapHandler( );
 
       GetListCollectionResponseGetListCollectionResult listResp = listsCall.getListCollection();
       org.apache.axis.message.MessageElement[] lists = listResp.get_any();
 
       XMLDoc doc = new XMLDoc( lists[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:Lists' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:Lists"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:Lists'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Lists>
 
       int chuckIndex = decodedServerLocation.length() + parentSiteDecoded.length();
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
 
         String baseType = doc.getValue( o, "BaseType");
         if ( baseType.equals("1") )
         {
           // We think it's a library
 
           // This is how we display it, so this has the right path extension
           String urlPath = doc.getValue( o, "DefaultViewUrl" );
 
           // If it has no view url, we don't have any idea what to do with it
           if (urlPath != null && urlPath.length() > 0)
           {
             if (urlPath.length() < chuckIndex)
               throw new ManifoldCFException("Library view url is not in the expected form: '"+urlPath+"'");
             urlPath = urlPath.substring(chuckIndex);
             if (!urlPath.startsWith("/"))
               throw new ManifoldCFException("Library view url without site is not in the expected form: '"+urlPath+"'");
             // We're at the library name.  Figure out where the end of it is.
             int index = urlPath.indexOf("/",1);
             if (index == -1)
               throw new ManifoldCFException("Bad library view url without site: '"+urlPath+"'");
             String pathpart = urlPath.substring(1,index);
 
             if ( pathpart.equals(docLibrary) )
             {
               // We found it!
               // Return its ID
               return doc.getValue( o, "ID" );
             }
           }
         }
       }
 
       // Not found - return null
       return null;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting the library ID for site "+parentSite+" library "+docLibrary+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+parentSite+" did not exist; assuming library deleted");
             return null;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to list libraries
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to list libraries for "+baseUrl+parentSite+"; skipping");
             return null;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+parentSite+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+parentSite+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The list "+docLibrary+" in site "+parentSite+" did not exist; assuming library deleted");
             return null;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting library ID for the list "+docLibrary+" in site "+parentSite+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return null;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting library ID for site "+parentSite+" library "+docLibrary+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying", e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting library ID for site "+parentSite+" library "+docLibrary+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting library ID for site "+parentSite+" library "+docLibrary,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   *
   * @param parentSite
   * @param list name
   * @return document library ID
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
   public String getListID(String parentSite, String parentSiteDecoded, String listName)
     throws ServiceInterruption, ManifoldCFException
   {
     long currentTime;
     try
     {
       // The old code here used to call the lists service to find the guid, using the doc library url name as the title.
       // This did not work when the title differed from the url name.
       // On 5/8/2008 I modified the code to use the lists service to locate the correct record by matching the defaultViewUrl field,
       // so that we instead iterate through the children.  It's more expensive but it works.
       String parentSiteRequest = parentSite;
 
       if ( parentSiteRequest.equals("/"))
       {
         parentSiteRequest = ""; // root case
         parentSiteDecoded = "";
       }
 
       ListsWS listsService = new ListsWS( baseUrl + parentSiteRequest, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listsCall = listsService.getListsSoapHandler( );
 
       GetListCollectionResponseGetListCollectionResult listResp = listsCall.getListCollection();
       org.apache.axis.message.MessageElement[] lists = listResp.get_any();
 
       XMLDoc doc = new XMLDoc( lists[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:Lists' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:Lists"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:Lists'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Lists>
 
       int chuckIndex = decodedServerLocation.length() + parentSiteDecoded.length();
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
 
         String baseType = doc.getValue( o, "BaseType");
         if ( baseType.equals("0") )
         {
           // We think it's a list
 
           // This is how we display it, so this has the right path extension
           String urlPath = doc.getValue( o, "DefaultViewUrl" );
 
           // If it has no view url, we don't have any idea what to do with it
           if (urlPath != null && urlPath.length() > 0)
           {
             if (urlPath.length() < chuckIndex)
               throw new ManifoldCFException("List view url is not in the expected form: '"+urlPath+"'");
             urlPath = urlPath.substring(chuckIndex);
             if (!urlPath.startsWith("/"))
               throw new ManifoldCFException("List view url without site is not in the expected form: '"+urlPath+"'");
             // We're at the /Lists/listname part of the name.  Figure out where the end of it is.
             int index = urlPath.indexOf("/",1);
             if (index == -1)
               throw new ManifoldCFException("Bad list view url without site: '"+urlPath+"'");
             String pathpart = urlPath.substring(1,index);
             if("Lists".equals(pathpart))
             {
               int k = urlPath.indexOf("/",index+1);
               if (k == -1)
                 throw new ManifoldCFException("Bad list view url without 'Lists': '"+urlPath+"'");
               pathpart = urlPath.substring(index+1,k);
             }
 
             if ( pathpart.equals(listName) )
             {
               // We found it!
               // Return its ID
               return doc.getValue( o, "ID" );
             }
           }
         }
       }
 
       // Not found - return null
       return null;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting the list ID for site "+parentSite+" list "+listName+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+parentSite+" did not exist; assuming list deleted");
             return null;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to list libraries
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to list lists for "+baseUrl+parentSite+"; skipping");
             return null;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+parentSite+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+parentSite+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The list "+listName+" in site "+parentSite+" did not exist; assuming list deleted");
             return null;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting list ID for the list "+listName+" in site "+parentSite+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return null;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting list ID for site "+parentSite+" list "+listName+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying", e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting list ID for site "+parentSite+" list "+listName+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting list ID for site "+parentSite+" list "+listName,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   *
   * @param site
   * @param docPath
   * @return an XML document
   * @throws ManifoldCFException
   * @throws ServiceInterruption
   */
   public XMLDoc getVersions( String site, String docPath)
     throws ServiceInterruption, ManifoldCFException
   {
     long currentTime;
     try
     {
       if ( site.compareTo("/") == 0 ) site = ""; // root case
         VersionsWS versionsService = new VersionsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
       VersionsSoap versionsCall = versionsService.getVersionsSoapHandler( );
 
       GetVersionsResponseGetVersionsResult versionsResp = versionsCall.getVersions( docPath );
       org.apache.axis.message.MessageElement[] lists = versionsResp.get_any();
 
       //System.out.println( lists[0].toString() );
       XMLDoc doc = new XMLDoc( lists[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
 
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'results' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
 
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("results"))
         throw new ManifoldCFException("Bad xml - outer node is not 'results'");
 
       return doc;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting versions for site "+site+" docpath "+docPath+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       currentTime = System.currentTimeMillis();
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The page at "+baseUrl+site+" did not exist; assuming library deleted");
             return null;
           }
           else if (httpErrorCode.equals("401"))
           {
             // User did not have permissions for this library to get the acls
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The crawl user did not have access to get versions for "+baseUrl+site+"; skipping");
             return null;
           }
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+site+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (sharepointErrorCode.equals("0x82000006"))
           {
             // List did not exist
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: The docpath "+docPath+" in site "+site+" did not exist; assuming library deleted");
             return null;
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
             {
               org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
               String errorString = "";
               if (elem != null)
                 errorString = elem2.getFirstChild().getNodeValue().trim();
 
               Logging.connectors.debug("SharePoint: Getting versions for the docpath "+docPath+" in site "+site+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString+" - Skipping",e);
             }
             return null;
           }
         }
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Unknown SharePoint server error getting versions for site "+site+" docpath "+docPath+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
 
         throw new ServiceInterruption("Unknown SharePoint server error: "+e.getMessage()+" - retrying", e, currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unknown remote exception getting versions for site "+site+" docpath "+docPath+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString()+" - retrying",e);
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got an unexpected remote exception getting versions for site "+site+" docpath "+docPath,e);
       throw new ManifoldCFException("Unexpected remote procedure exception: "+e.getMessage(), e);
     }
   }
 
   /**
   *
   * @param userCall
   * @param userLogin
   * @return
   * @throws Exception
   */
   private String getSidForUser(com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap userCall, String userLogin )
   throws ManifoldCFException, java.net.MalformedURLException, javax.xml.rpc.ServiceException,
     java.rmi.RemoteException
   {
     com.microsoft.schemas.sharepoint.soap.directory.GetUserInfoResponseGetUserInfoResult userResp = userCall.getUserInfo( userLogin );
     org.apache.axis.message.MessageElement[] userList = userResp.get_any();
 
     XMLDoc doc = new XMLDoc( userList[0].toString() );
     ArrayList nodeList = new ArrayList();
 
     doc.processPath(nodeList, "*", null);
     if (nodeList.size() != 1)
     {
       throw new ManifoldCFException("Bad xml - missing outer 'ns1:GetUserInfo' node - there are "+Integer.toString(nodeList.size())+" nodes");
     }
     Object parent = nodeList.get(0);
     if (!doc.getNodeName(parent).equals("ns1:GetUserInfo"))
       throw new ManifoldCFException("Bad xml - outer node is not 'ns1:GetUserInfo'");
 
     nodeList.clear();
     doc.processPath(nodeList, "*", parent);  // ns1:User
 
     if ( nodeList.size() != 1 )
     {
       throw new ManifoldCFException( " No User found." );
     }
     parent = nodeList.get(0);
     nodeList.clear();
     String sid = doc.getValue( parent, "Sid" );
     return sid;
   }
 
   /**
   *
   * @param userCall
   * @param groupName
   * @return
   * @throws Exception
   */
   private String[] getSidsForGroup(com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap userCall, String groupName)
     throws ManifoldCFException, java.net.MalformedURLException, javax.xml.rpc.ServiceException, java.rmi.RemoteException
   {
     com.microsoft.schemas.sharepoint.soap.directory.GetUserCollectionFromGroupResponseGetUserCollectionFromGroupResult roleResp = userCall.getUserCollectionFromGroup(groupName);
     org.apache.axis.message.MessageElement[] roleList = roleResp.get_any();
 
     XMLDoc doc = new XMLDoc(roleList[0].toString());
     ArrayList nodeList = new ArrayList();
 
     doc.processPath(nodeList, "*", null);
     if (nodeList.size() != 1)
     {
       throw new ManifoldCFException("Bad xml - missing outer 'ns1:GetUserCollectionFromGroup' node - there are "
       + Integer.toString(nodeList.size()) + " nodes");
     }
     Object parent = nodeList.get(0);
     if (!doc.getNodeName(parent).equals("ns1:GetUserCollectionFromGroup"))
       throw new ManifoldCFException("Bad xml - outer node is not 'ns1:GetUserCollectionFromGroup'");
 
     nodeList.clear();
     doc.processPath(nodeList, "*", parent); // <ns1:Users>
 
     if (nodeList.size() != 1)
     {
       throw new ManifoldCFException(" No Users collection found.");
     }
     parent = nodeList.get(0);
     nodeList.clear();
     doc.processPath(nodeList, "*", parent); // <ns1:User>
 
     ArrayList sidsList = new ArrayList();
     String[] sids = new String[0];
     int i = 0;
     while (i < nodeList.size())
     {
       Object o = nodeList.get(i++);
       sidsList.add(doc.getValue(o, "Sid"));
     }
     sids = (String[]) sidsList.toArray((Object[]) sids);
     return sids;
   }
 
   /**
   *
   * @param userCall
   * @param roleName
   * @return
   * @throws Exception
   */
   private String[] getSidsForRole( com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap userCall, String roleName )
   throws ManifoldCFException, java.net.MalformedURLException, javax.xml.rpc.ServiceException,
     java.rmi.RemoteException
   {
 
     com.microsoft.schemas.sharepoint.soap.directory.GetUserCollectionFromRoleResponseGetUserCollectionFromRoleResult roleResp = userCall.getUserCollectionFromRole( roleName );
     org.apache.axis.message.MessageElement[] roleList = roleResp.get_any();
 
     XMLDoc doc = new XMLDoc( roleList[0].toString() );
     ArrayList nodeList = new ArrayList();
 
     doc.processPath(nodeList, "*", null);
     if (nodeList.size() != 1)
     {
       throw new ManifoldCFException("Bad xml - missing outer 'ns1:GetUserCollectionFromRole' node - there are "+Integer.toString(nodeList.size())+" nodes");
     }
     Object parent = nodeList.get(0);
     if (!doc.getNodeName(parent).equals("ns1:GetUserCollectionFromRole"))
       throw new ManifoldCFException("Bad xml - outer node is not 'ns1:GetUserCollectionFromRole'");
 
     nodeList.clear();
     doc.processPath(nodeList, "*", parent);  // <ns1:Users>
 
     if ( nodeList.size() != 1 )
     {
       throw new ManifoldCFException( " No Users collection found." );
     }
     parent = nodeList.get(0);
     nodeList.clear();
     doc.processPath( nodeList, "*", parent ); // <ns1:User>
 
     ArrayList sidsList = new ArrayList();
     String[] sids = new String[0];
     int i = 0;
     while (i < nodeList.size())
     {
       Object o = nodeList.get( i++ );
       sidsList.add( doc.getValue( o, "Sid" ) );
     }
     sids = (String[])sidsList.toArray( (Object[])sids );
     return sids;
   }
 
   /**
   *
   * @return true if connection OK
   * @throws java.net.MalformedURLException
   * @throws javax.xml.rpc.ServiceException
   * @throws java.rmi.RemoteException
   */
   public boolean checkConnection( String site, boolean sps30 )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       if (site.equals("/"))
         site = "";
 
       // Attempt a listservice call
       ListsWS listService = new ListsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listCall = listService.getListsSoapHandler();
       listCall.getListCollection();
 
       // If this is 3.0, we should also attempt to reach our custom webservice
       if (sps30)
       {
         // The web service allows us to get acls for a site, so that's what we will attempt
 
         // This fails:
         MCPermissionsWS aclService = new MCPermissionsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
         com.microsoft.sharepoint.webpartpages.PermissionsSoap aclCall = aclService.getPermissionsSoapHandler( );
 
         // This works:
         //PermissionsWS aclService = new PermissionsWS( baseUrl + site, userName, password, myFactory, configuration );
         //com.microsoft.schemas.sharepoint.soap.directory.PermissionsSoap aclCall = aclService.getPermissionsSoapHandler( );
 
         aclCall.getPermissionCollection( "/", "Web" );
       }
 
       return true;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception checking connection - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
           {
             // Page did not exist
             throw new ManifoldCFException("The site at "+baseUrl+site+" did not exist");
           }
           else if (httpErrorCode.equals("401"))
             throw new ManifoldCFException("Crawl user did not authenticate properly, or has insufficient permissions to access "+baseUrl+site+": "+e.getMessage(),e);
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Http error "+httpErrorCode+" while reading from "+baseUrl+site+" - check IIS and SharePoint security settings! "+e.getMessage(),e);
 	  else if (httpErrorCode.equals("302"))
 	    throw new ManifoldCFException("ManifoldCF's MCPermissions web service may not be installed on the target SharePoint server.  MCPermissions service is needed for SharePoint repositories version 3.0 or higher, to allow access to security information for files and folders.  Consult your system administrator.");
           else
             throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       else if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorcode"));
         if (elem != null)
         {
           elem.normalize();
           String sharepointErrorCode = elem.getFirstChild().getNodeValue().trim();
           org.w3c.dom.Element elem2 = e.lookupFaultDetail(new javax.xml.namespace.QName("http://schemas.microsoft.com/sharepoint/soap/","errorstring"));
           String errorString = "";
           if (elem != null)
             errorString = elem2.getFirstChild().getNodeValue().trim();
 
           throw new ManifoldCFException("Accessing site "+site+" failed with unexpected SharePoint error code "+sharepointErrorCode+": "+errorString,e);
         }
         throw new ManifoldCFException("Unknown SharePoint server error accessing site "+site+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString(),e);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       throw new ManifoldCFException("Got an unknown remote exception accessing site "+site+" - axis fault = "+e.getFaultCode().getLocalPart()+", detail = "+e.getFaultString(),e);
     }
     catch (java.rmi.RemoteException e)
     {
       // We expect the axis exception to be thrown, not this generic one!
       // So, fail hard if we see it.
       throw new ManifoldCFException("Got an unexpected remote exception accessing site "+site+": "+e.getMessage(),e);
     }
   }
 
   /**
   * Gets a list of field names of the given document library
   * @param site
   * @param list/library name
   * @return list of the fields
   */
   public Map<String,String> getFieldList( String site, String listName )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       Map<String,String> result = new HashMap<String,String>();
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: In getFieldList; site='"+site+"', listName='"+listName+"'");
 
       // The docLibrary must be a GUID, because we don't have  title.
 
       if ( site.compareTo( "/") == 0 ) site = "";
         ListsWS listService = new ListsWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listCall = listService.getListsSoapHandler();
 
       GetListResponseGetListResult listResponse = listCall.getList( listName );
       org.apache.axis.message.MessageElement[] List = listResponse.get_any();
 
       XMLDoc doc = new XMLDoc( List[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
 
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:List"))
         throw new ManifoldCFException("Bad xml - outer node is '" + doc.getNodeName(parent) + "' not 'ns1:List'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Fields>
 
       Object fields = nodeList.get(0);
       if ( !doc.getNodeName(fields).equals("ns1:Fields") )
         throw new ManifoldCFException( "Bad xml - child node 0 '" + doc.getNodeName(fields) + "' is not 'ns1:Fields'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", fields);
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
         // Logging.connectors.debug( i + ": " + o );
         String name = doc.getValue( o, "DisplayName" );
         String fieldName = doc.getValue( o, "Name" );
         String hidden = doc.getValue( o, "Hidden" );
         // System.out.println( "Hidden :" + hidden );
         if ( name.length() != 0 && fieldName.length() != 0 && ( !hidden.equalsIgnoreCase( "true") ) )
         {
           // make sure we don't include the same field more than once.
           // This may happen if the Library has more than one view.
           if ( result.containsKey( fieldName ) == false)
             result.put(fieldName, name);
         }
       }
       // System.out.println(result.size());
       return result;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting field list for site "+site+" listName "+listName+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
             return null;
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Remote procedure exception: "+e.getMessage(),e);
           else if (httpErrorCode.equals("401"))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: Crawl user does not have sufficient privileges to get field list for site "+site+" listName "+listName+" - skipping",e);
             return null;
           }
           throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       // I don't know if this is what you get when the library is missing, but here's hoping.
       if (e.getMessage().indexOf("List does not exist") != -1)
         return null;
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a remote exception getting field list for site "+site+" listName "+listName+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       throw new ManifoldCFException("Unexpected remote exception occurred: "+e.getMessage(),e);
     }
   }
 
   /**
   * Gets a list of field values of the given document
   * @param fieldNames
   * @param site
   * @param docId
   * @return set of the field values
   */
   public Map getFieldValues( ArrayList fieldNames, String site, String docLibrary, String docId, boolean dspStsWorks )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       HashMap result = new HashMap();
 
       if ( site.compareTo("/") == 0 ) site = ""; // root case
 
       if ( dspStsWorks )
       {
         StsAdapterWS listService = new StsAdapterWS( baseUrl + site, userName, password, myFactory, configuration, connectionManager );
         StsAdapterSoapStub stub = (StsAdapterSoapStub)listService.getStsAdapterSoapHandler();
 
         String[] vArray = new String[1];
         vArray[0] = "1.0";
         VersionsHeader myVersion = new VersionsHeader();
         myVersion.setVersion( vArray );
 
         stub.setHeader( "http://schemas.microsoft.com/sharepoint/dsp", "versions", myVersion );
 
         RequestHeader reqHeader = new RequestHeader();
         reqHeader.setDocument( DocumentType.content );
         reqHeader.setMethod(MethodType.query );
 
         stub.setHeader( "http://schemas.microsoft.com/sharepoint/dsp", "request", reqHeader );
 
         QueryRequest myRequest = new QueryRequest();
 
         DSQuery sQuery = new DSQuery();
         sQuery.setSelect( "/list[@id='" + docLibrary + "']" );
         sQuery.setResultContent(ResultContentType.dataOnly);
         myRequest.setDsQuery( sQuery );
 
         DspQuery spQuery = new DspQuery();
         spQuery.setRowLimit( 1 );
         // For the Requested Fields
         if ( fieldNames.size() > 0 )
         {
           Fields spFields = new Fields();
           Field[] fieldArray = new Field[0];
           ArrayList fields = new ArrayList();
 
           Field spField = new Field();
           //                      spField.setName( "ID" );
           //                      spField.setAlias( "ID" );
           //                      fields.add( spField );
 
           for ( int k = 0; k < fieldNames.size(); k++ )
           {
             spField = new Field();
             spField.setName( (String)fieldNames.get(k) );
             spField.setAlias( (String)fieldNames.get(k) );
             fields.add( spField );
           }
           spFields.setField( (Field[]) fields.toArray( fieldArray ));
           spQuery.setFields( spFields );
         }
         // Of this document
         DspQueryWhere spWhere = new DspQueryWhere();
 
         org.apache.axis.message.MessageElement criterion = new org.apache.axis.message.MessageElement( (String)null, "Contains" );
         SOAPElement seFieldRef = criterion.addChildElement( "FieldRef" );
         seFieldRef.addAttribute( SOAPFactory.newInstance().createName("Name") , "FileRef" );
         SOAPElement seValue = criterion.addChildElement( "Value" );
         seValue.addAttribute( SOAPFactory.newInstance().createName("Type") , "String" );
         seValue.setValue( docId );
 
         org.apache.axis.message.MessageElement[] criteria = { criterion };
         spWhere.set_any( criteria );
         spQuery.setWhere( (DspQueryWhere)spWhere );
 
         // Set Criteria
         myRequest.getDsQuery().setQuery(spQuery);
 
         StsAdapterSoap call = stub;
 
         // Make Request
         QueryResponse resp = call.query( myRequest );
         org.apache.axis.message.MessageElement[] list = resp.get_any();
 
         if (Logging.connectors.isDebugEnabled())
         {
           Logging.connectors.debug("SharePoint: list xml: '" + list[0].toString() + "'");
         }
 
         XMLDoc doc = new XMLDoc( list[0].toString() );
         ArrayList nodeList = new ArrayList();
 
         doc.processPath(nodeList, "*", null);
         if (nodeList.size() != 1)
         {
           throw new ManifoldCFException("Bad xml - missing outer 'ns1:dsQueryResponse' node - there are "+Integer.toString(nodeList.size())+" nodes");
         }
 
         Object parent = nodeList.get(0);
         //System.out.println( "Outer NodeName = " + doc.getNodeName(parent) );
         if (!doc.getNodeName(parent).equals("ns1:dsQueryResponse"))
           throw new ManifoldCFException("Bad xml - outer node is not 'ns1:dsQueryResponse'");
 
         nodeList.clear();
         doc.processPath(nodeList, "*", parent);
 
         parent = nodeList.get( 0 ); // <Shared_X0020_Documents />
 
         nodeList.clear();
         doc.processPath(nodeList, "*", parent);
 
         // Process each result (Should only be one )
         // Get each childs Value and add to return array
         for ( int i= 0; i < nodeList.size(); i++ )
         {
           Object documentNode = nodeList.get( i );
           ArrayList fieldList = new ArrayList();
 
           doc.processPath( fieldList, "*", documentNode );
           for ( int j =0; j < fieldList.size(); j++)
           {
             Object field = fieldList.get( j );
             String fieldData = doc.getData(field);
             String fieldName = doc.getNodeName(field);
             // Right now this really only works right for single-valued fields.  For multi-valued
             // fields, we'd need to know in advance that they were multivalued
             // so that we could interpret commas as value separators.
             result.put(fieldName,fieldData);
           }
         }
       }
       else
       {
         // SharePoint 2010: Get field values some other way
         // Sharepoint 2010; use Lists service instead
         ListsWS lservice = new ListsWS(baseUrl + site, userName, password, myFactory, configuration, connectionManager );
         ListsSoapStub stub1 = (ListsSoapStub)lservice.getListsSoapHandler();
         
        GetListItemsQuery q = buildMatchQuery("FileRef","Text",site + docId);
         GetListItemsViewFields viewFields = buildViewFields(fieldNames);
 
         GetListItemsResponseGetListItemsResult items =  stub1.getListItems(docLibrary, "", q, viewFields, "1", buildNonPagingQueryOptions(), null);
         if (items == null)
           return result;
 
         MessageElement[] list = items.get_any();
 
         if (Logging.connectors.isDebugEnabled()){
          Logging.connectors.debug("SharePoint: getListItems for '"+site+docId+"' xml response: '" + list[0].toString() + "'");
         }
 
         ArrayList nodeList = new ArrayList();
         XMLDoc doc = new XMLDoc(list[0].toString());
 
         doc.processPath(nodeList, "*", null);
         if (nodeList.size() != 1)
           throw new ManifoldCFException("Bad xml - expecting one outer 'ns1:listitems' node - there are " + Integer.toString(nodeList.size()) + " nodes");
 
         Object parent = nodeList.get(0);
         if (!"ns1:listitems".equals(doc.getNodeName(parent)))
           throw new ManifoldCFException("Bad xml - outer node is not 'ns1:listitems'");
 
         nodeList.clear();
         doc.processPath(nodeList, "*", parent);
 
         if (nodeList.size() != 1)
           throw new ManifoldCFException("Expected rsdata result but no results found.");
 
         Object rsData = nodeList.get(0);
 
         int itemCount = Integer.parseInt(doc.getValue(rsData, "ItemCount"));
         if (itemCount == 0)
           return result;
           
         // Now, extract the files from the response document
         ArrayList nodeDocs = new ArrayList();
 
         doc.processPath(nodeDocs, "*", rsData);
 
         if (nodeDocs.size() != itemCount)
           throw new ManifoldCFException("itemCount does not match with nodeDocs.size()");
 
         if (itemCount != 1)
           throw new ManifoldCFException("Expecting only one item, instead saw '"+itemCount+"'");
         
         Object o = nodeDocs.get(0);
         
         // Look for all the specified attributes in the record
         for (Object attrName : fieldNames)
         {
           String attrValue = doc.getValue(o,"ows_"+(String)attrName);
           if (attrValue != null)
           {
             result.put(attrName,valueMunge(attrValue));
           }
         }
       }
 
       return result;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException("Soap exception: "+e.getMessage(),e);
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting field values for site "+site+" library "+docLibrary+" document '"+docId+"' - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
             return null;
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Remote procedure exception: "+e.getMessage(),e);
           else if (httpErrorCode.equals("401"))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: Crawl user does not have sufficient privileges to get field values for site "+site+" library "+docLibrary+" - skipping",e);
             return null;
           }
           throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+site+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       // I don't know if this is what you get when the library is missing, but here's hoping.
       if (e.getMessage().indexOf("List does not exist") != -1)
         return null;
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a remote exception getting field values for site "+site+" library "+docLibrary+" document ["+docId+"] - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       throw new ManifoldCFException("Unexpected remote exception occurred: "+e.getMessage(),e);
     }
   }
 
   /**
   * Gets a list of sites given a parent site
   * @param parentSite the site to search for subsites, empty string for root
   * @return lists of sites as an arraylist of NameValue objects
   */
   public ArrayList getSites( String parentSite )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       ArrayList result = new ArrayList();
 
       if ( parentSite.equals( "/") ) parentSite = "";
         WebsWS webService = new WebsWS( baseUrl + parentSite, userName, password, myFactory, configuration, connectionManager );
       WebsSoap webCall = webService.getWebsSoapHandler();
 
       GetWebCollectionResponseGetWebCollectionResult webResp = webCall.getWebCollection();
       org.apache.axis.message.MessageElement[] webList = webResp.get_any();
 
       XMLDoc doc = new XMLDoc( webList[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:Webs' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:Webs"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:Webs'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Webs>
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
         //Logging.connectors.debug( i + ": " + o );
         //System.out.println( i + ": " + o );
         String url = doc.getValue( o, "Url" );
         String title = doc.getValue( o, "Title" );
 
         // Leave here for now
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("SharePoint: Subsite list: '"+url+"', '"+title+"'");
 
         // A full path to the site is tacked on the front of each one of these.  However, due to nslookup differences, we cannot guarantee that
         // the server name part of the path will actually match what got passed in.  Therefore, we want to look only at the last path segment, whatever that is.
         if (url != null && url.length() > 0)
         {
           int lastSlash = url.lastIndexOf("/");
           if (lastSlash != -1)
           {
             String pathValue = url.substring(lastSlash + 1);
             if (pathValue.length() > 0)
             {
               if (title == null || title.length() == 0)
                 title = pathValue;
               result.add(new NameValue(pathValue,title));
             }
           }
         }
       }
 
       return result;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting subsites for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
             return null;
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Remote procedure exception: "+e.getMessage(),e);
           else if (httpErrorCode.equals("401"))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: Crawl user does not have sufficient privileges to get subsites of site "+parentSite+" - skipping",e);
             return null;
           }
           throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+parentSite+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
 
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a remote exception getting subsites for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       throw new ManifoldCFException("Unexpected remote exception occurred: "+e.getMessage(),e);
     }
 
   }
 
   /**
   * Gets a list of document libraries given a parent site
   * @param parentSite the site to search for document libraries, empty string for root
   * @return lists of NameValue objects, representing document libraries
   */
   public ArrayList getDocumentLibraries( String parentSite, String parentSiteDecoded )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       ArrayList result = new ArrayList();
 
       String parentSiteRequest = parentSite;
 
       if ( parentSiteRequest.equals("/"))
       {
         parentSiteRequest = ""; // root case
         parentSiteDecoded = "";
       }
 
       ListsWS listsService = new ListsWS( baseUrl + parentSiteRequest, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listsCall = listsService.getListsSoapHandler( );
 
       GetListCollectionResponseGetListCollectionResult listResp = listsCall.getListCollection();
       org.apache.axis.message.MessageElement[] lists = listResp.get_any();
 
       //if ( parentSite.compareTo("/Sample2") == 0) System.out.println( lists[0].toString() );
 
       XMLDoc doc = new XMLDoc( lists[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:Lists' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:Lists"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:Lists'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Lists>
 
       int chuckIndex = decodedServerLocation.length() + parentSiteDecoded.length();
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
 
         String baseType = doc.getValue( o, "BaseType");
         if ( baseType.equals( "1" ) )
         {
           // We think it's a library
 
           // This is how we display it, so this has the right path extension
           String urlPath = doc.getValue( o, "DefaultViewUrl" );
           // This is the pretty name
           String title = doc.getValue( o, "Title" );
 
           // Leave this in for the moment
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("SharePoint: Library list: '"+urlPath+"', '"+title+"'");
 
           // It's a library.  If it has no view url, we don't have any idea what to do with it
           if (urlPath != null && urlPath.length() > 0)
           {
             if (urlPath.length() < chuckIndex)
               throw new ManifoldCFException("Library view url is not in the expected form: '"+urlPath+"'");
             urlPath = urlPath.substring(chuckIndex);
             if (!urlPath.startsWith("/"))
               throw new ManifoldCFException("Library view url without site is not in the expected form: '"+urlPath+"'");
             // We're at the library name.  Figure out where the end of it is.
             int index = urlPath.indexOf("/",1);
             if (index == -1)
               throw new ManifoldCFException("Bad library view url without site: '"+urlPath+"'");
             String pathpart = urlPath.substring(1,index);
 
             if ( pathpart.length() != 0 && !pathpart.equals("_catalogs"))
             {
               if (title == null || title.length() == 0)
                 title = pathpart;
               result.add( new NameValue(pathpart, title) );
             }
           }
         }
       }
 
       return result;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting document libraries for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
             return null;
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Remote procedure exception: "+e.getMessage(),e);
           else if (httpErrorCode.equals("401"))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: Crawl user does not have sufficient privileges to read document libraries for site "+parentSite+" - skipping",e);
             return null;
           }
           throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+parentSite+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a remote exception reading document libraries for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       throw new ManifoldCFException("Unexpected remote exception occurred: "+e.getMessage(),e);
     }
   }
 
   /**
   * Gets a list of lists given a parent site
   * @param parentSite the site to search for lists, empty string for root
   * @return lists of NameValue objects, representing lists
   */
   public ArrayList getLists( String parentSite, String parentSiteDecoded )
     throws ManifoldCFException, ServiceInterruption
   {
     long currentTime;
     try
     {
       ArrayList result = new ArrayList();
 
       String parentSiteRequest = parentSite;
 
       if ( parentSiteRequest.equals("/"))
       {
         parentSiteRequest = ""; // root case
         parentSiteDecoded = "";
       }
 
       ListsWS listsService = new ListsWS( baseUrl + parentSiteRequest, userName, password, myFactory, configuration, connectionManager );
       ListsSoap listsCall = listsService.getListsSoapHandler( );
 
       GetListCollectionResponseGetListCollectionResult listResp = listsCall.getListCollection();
       org.apache.axis.message.MessageElement[] lists = listResp.get_any();
 
       //if ( parentSite.compareTo("/Sample2") == 0) System.out.println( lists[0].toString() );
 
       XMLDoc doc = new XMLDoc( lists[0].toString() );
       ArrayList nodeList = new ArrayList();
 
       doc.processPath(nodeList, "*", null);
       if (nodeList.size() != 1)
       {
         throw new ManifoldCFException("Bad xml - missing outer 'ns1:Lists' node - there are "+Integer.toString(nodeList.size())+" nodes");
       }
       Object parent = nodeList.get(0);
       if (!doc.getNodeName(parent).equals("ns1:Lists"))
         throw new ManifoldCFException("Bad xml - outer node is not 'ns1:Lists'");
 
       nodeList.clear();
       doc.processPath(nodeList, "*", parent);  // <ns1:Lists>
 
       int chuckIndex = decodedServerLocation.length() + parentSiteDecoded.length();
 
       int i = 0;
       while (i < nodeList.size())
       {
         Object o = nodeList.get( i++ );
 
         String baseType = doc.getValue( o, "BaseType");
         if ( baseType.equals( "0" ) )
         {
           // We think it's a list
 
           // This is how we display it, so this has the right path extension
           String urlPath = doc.getValue( o, "DefaultViewUrl" );
           // This is the pretty name
           String title = doc.getValue( o, "Title" );
 
           // Leave this in for the moment
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("SharePoint: List: '"+urlPath+"', '"+title+"'");
 
           // If it has no view url, we don't have any idea what to do with it
           if (urlPath != null && urlPath.length() > 0)
           {
             if (urlPath.length() < chuckIndex)
               throw new ManifoldCFException("List view url is not in the expected form: '"+urlPath+"'");
             urlPath = urlPath.substring(chuckIndex);
             if (!urlPath.startsWith("/"))
               throw new ManifoldCFException("List view url without site is not in the expected form: '"+urlPath+"'");
             // We're at the /Lists/listname part of the name.  Figure out where the end of it is.
             int index = urlPath.indexOf("/",1);
             if (index == -1)
               throw new ManifoldCFException("Bad list view url without site: '"+urlPath+"'");
             String pathpart = urlPath.substring(1,index);
 
             if("Lists".equals(pathpart))
             {
               int k = urlPath.indexOf("/",index+1);
               if (k == -1)
                 throw new ManifoldCFException("Bad list view url without 'Lists': '"+urlPath+"'");
               pathpart = urlPath.substring(index+1,k);
             }
 
             if ( pathpart.length() != 0 && !pathpart.equals("_catalogs"))
             {
               if (title == null || title.length() == 0)
                 title = pathpart;
               result.add( new NameValue(pathpart, title) );
             }
           }
         }
 
       }
 
       return result;
     }
     catch (java.net.MalformedURLException e)
     {
       throw new ManifoldCFException("Bad SharePoint url: "+e.getMessage(),e);
     }
     catch (javax.xml.rpc.ServiceException e)
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a service exception getting lists for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Service exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,true);
     }
     catch (org.apache.axis.AxisFault e)
     {
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HTTP")))
       {
         org.w3c.dom.Element elem = e.lookupFaultDetail(new javax.xml.namespace.QName("http://xml.apache.org/axis/","HttpErrorCode"));
         if (elem != null)
         {
           elem.normalize();
           String httpErrorCode = elem.getFirstChild().getNodeValue().trim();
           if (httpErrorCode.equals("404"))
             return null;
           else if (httpErrorCode.equals("403"))
             throw new ManifoldCFException("Remote procedure exception: "+e.getMessage(),e);
           else if (httpErrorCode.equals("401"))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("SharePoint: Crawl user does not have sufficient privileges to read lists for site "+parentSite+" - skipping",e);
             return null;
           }
           throw new ManifoldCFException("Unexpected http error code "+httpErrorCode+" accessing SharePoint at "+baseUrl+parentSite+": "+e.getMessage(),e);
         }
         throw new ManifoldCFException("Unknown http error occurred: "+e.getMessage(),e);
       }
       if (e.getFaultCode().equals(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/","Server.userException")))
       {
         String exceptionName = e.getFaultString();
         if (exceptionName.equals("java.lang.InterruptedException"))
           throw new ManifoldCFException("Interrupted",ManifoldCFException.INTERRUPTED);
       }
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("SharePoint: Got a remote exception reading lists for site "+parentSite+" - retrying",e);
       currentTime = System.currentTimeMillis();
       throw new ServiceInterruption("Remote procedure exception: "+e.getMessage(), e, currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     catch (java.rmi.RemoteException e)
     {
       throw new ManifoldCFException("Unexpected remote exception occurred: "+e.getMessage(),e);
     }
   }
 
   // Regexp pattern to match 12345;#
   protected static Pattern subsPattern;
   static
   {
     try
     {
       subsPattern = Pattern.compile("[0-9]*;#.*");
     }
     catch (Exception e)
     {
       e.printStackTrace();
       System.exit(-100);
     }
   }
   
   /** Substitute progid where found */
   protected static String valueMunge(String value)
   {
     Matcher matcher = subsPattern.matcher(value);
     if (matcher.matches())
       return value.substring(value.indexOf("#") + 1);
     return value;
   }
   
   /** Build viewFields XML for the ListItems call.
   */
   protected static GetListItemsViewFields buildViewFields(ArrayList fieldNames)
     throws ManifoldCFException
   {
     try
     {
       GetListItemsViewFields rval = new GetListItemsViewFields();
       MessageElement viewFieldsNode = new MessageElement((String)null,"ViewFields");
       rval.set_any(new MessageElement[]{viewFieldsNode});
       for (Object x : fieldNames)
       {
         MessageElement child = new MessageElement((String)null,"FieldRef");
         viewFieldsNode.addChild(child);
         child.addAttribute(null,"Name",(String)x);
       }
       return rval;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
   
   /** Build a query XML object that matches a specified field and value pair.
   */
   protected static GetListItemsQuery buildMatchQuery(String fieldName, String type, String value)
     throws ManifoldCFException
   {
     try
     {
       GetListItemsQuery rval = new GetListItemsQuery();
       MessageElement queryNode = new MessageElement((String)null,"Query");
       rval.set_any(new MessageElement[]{queryNode});
       MessageElement whereNode = new MessageElement((String)null,"Where");
       queryNode.addChild(whereNode);
       MessageElement eqNode = new MessageElement((String)null,"Eq");
       whereNode.addChild(eqNode);
       MessageElement fieldRefNode = new MessageElement((String)null,"FieldRef");
       eqNode.addChild(fieldRefNode);
       fieldRefNode.addAttribute(null,"Name",fieldName);
       MessageElement valueNode = new MessageElement((String)null,"Value");
       eqNode.addChild(valueNode);
       valueNode.addAttribute(null,"Type",type);
       valueNode.addTextNode(value);
       return rval;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
   
   /** Build a query XML object that orders by an indexed column, for paging.
   */
   protected static GetListItemsQuery buildOrderedQuery(String indexedColumn)
     throws ManifoldCFException
   {
     try
     {
       GetListItemsQuery rval = new GetListItemsQuery();
       MessageElement queryNode = new MessageElement((String)null,"Query");
       rval.set_any(new MessageElement[]{queryNode});
       MessageElement orderByNode = new MessageElement((String)null,"OrderBy");
       queryNode.addChild(orderByNode);
       orderByNode.addAttribute(null,"Override","TRUE");
       orderByNode.addAttribute(null,"UseIndexForOrderBy","TRUE");
       MessageElement fieldRefNode = new MessageElement((String)null,"FieldRef");
       orderByNode.addChild(fieldRefNode);
       fieldRefNode.addAttribute(null,"Ascending","TRUE");
       fieldRefNode.addAttribute(null,"Name",indexedColumn);
       return rval;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
   
   /** Build queryOptions XML object that specifies a paging value.
   */
   protected static GetListItemsQueryOptions buildPagingQueryOptions(String pageNextString)
     throws ManifoldCFException
   {
     try
     {
       GetListItemsQueryOptions rval = new GetListItemsQueryOptions();
       MessageElement queryOptionsNode = new MessageElement((String)null,"QueryOptions");
       rval.set_any(new MessageElement[]{queryOptionsNode});
       MessageElement pagingNode = new MessageElement((String)null,"Paging");
       queryOptionsNode.addChild(pagingNode);
       pagingNode.addAttribute(null,"ListItemCollectionPositionNext",pageNextString);
       MessageElement viewAttributesNode = new MessageElement((String)null,"ViewAttributes");
       queryOptionsNode.addChild(viewAttributesNode);
       viewAttributesNode.addAttribute(null,"Scope","Recursive");
 
       return rval;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
 
   /** Build queryOptions XML object that specifies no paging value.
   */
   protected static GetListItemsQueryOptions buildNonPagingQueryOptions()
     throws ManifoldCFException
   {
     try
     {
       GetListItemsQueryOptions rval = new GetListItemsQueryOptions();
       MessageElement queryOptionsNode = new MessageElement((String)null,"QueryOptions");
       rval.set_any(new MessageElement[]{queryOptionsNode});
       MessageElement viewAttributesNode = new MessageElement((String)null,"ViewAttributes");
       queryOptionsNode.addChild(viewAttributesNode);
       viewAttributesNode.addAttribute(null,"Scope","Recursive");
 
       return rval;
     }
     catch (javax.xml.soap.SOAPException e)
     {
       throw new ManifoldCFException(e.getMessage(),e);
     }
   }
   
   /**
   * SharePoint Permissions Service Wrapper Class
   */
   protected static class PermissionsWS extends com.microsoft.schemas.sharepoint.soap.directory.PermissionsLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = -2542430113046450050L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public PermissionsWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/Permissions.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.soap.directory.PermissionsSoap getPermissionsSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.soap.directory.PermissionsSoapStub _stub = new com.microsoft.schemas.sharepoint.soap.directory.PermissionsSoapStub(endPoint, this);
       _stub.setPortName(getPermissionsSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * MC Permissions Service Wrapper Class
   */
   protected static class MCPermissionsWS extends com.microsoft.sharepoint.webpartpages.PermissionsLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = -2542430113046450051L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public MCPermissionsWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/MCPermissions.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.sharepoint.webpartpages.PermissionsSoap getPermissionsSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.sharepoint.webpartpages.PermissionsSoapStub _stub = new com.microsoft.sharepoint.webpartpages.PermissionsSoapStub(endPoint, this);
       _stub.setPortName(getPermissionsSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * SharePoint UserGroup Service Wrapper Class
   */
   protected static class UserGroupWS extends com.microsoft.schemas.sharepoint.soap.directory.UserGroupLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = -2052484076803624502L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public UserGroupWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/usergroup.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoap getUserGroupSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoapStub _stub = new com.microsoft.schemas.sharepoint.soap.directory.UserGroupSoapStub(endPoint, this);
       _stub.setPortName(getUserGroupSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * SharePoint StsAdapter (List Data Services) Service Wrapper Class
   */
   protected static class StsAdapterWS extends StsAdapterLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = -731937337802481409L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public StsAdapterWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/dspsts.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.dsp.StsAdapterSoap getStsAdapterSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.dsp.StsAdapterSoapStub _stub = new com.microsoft.schemas.sharepoint.dsp.StsAdapterSoapStub(endPoint, this);
       _stub.setPortName(getStsAdapterSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * SharePoint Lists Service Wrapper Class
   */
   protected static class ListsWS extends ListsLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = 5506842429029882999L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public ListsWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/lists.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.soap.ListsSoap getListsSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.soap.ListsSoapStub _stub = new com.microsoft.schemas.sharepoint.soap.ListsSoapStub(endPoint, this);
       _stub.setPortName(getListsSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * SharePoint Versions Service Wrapper Class
   */
   protected static class VersionsWS extends VersionsLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = 4903552161088337964L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public VersionsWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/versions.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.soap.VersionsSoap getVersionsSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.soap.VersionsSoapStub _stub = new com.microsoft.schemas.sharepoint.soap.VersionsSoapStub(endPoint, this);
       _stub.setPortName(getVersionsSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /**
   * SharePoint Webs Service Wrapper Class
   */
   protected static class WebsWS extends WebsLocator
   {
     /**
     *
     */
     private static final long serialVersionUID = 6879757392680147691L;
     private java.net.URL endPoint;
     private String userName;
     private String password;
     private ProtocolFactory myFactory;
     private HttpConnectionManager connectionManager;
 
     public WebsWS ( String siteUrl, String userName, String password, ProtocolFactory myFactory, EngineConfiguration configuration, HttpConnectionManager connectionManager )
       throws java.net.MalformedURLException
     {
       super(configuration);
       endPoint = new java.net.URL(siteUrl + "/_vti_bin/webs.asmx");
       this.userName = userName;
       this.password = password;
       this.myFactory = myFactory;
       this.connectionManager = connectionManager;
     }
 
     public com.microsoft.schemas.sharepoint.soap.WebsSoap getWebsSoapHandler( )
       throws javax.xml.rpc.ServiceException, org.apache.axis.AxisFault
     {
       com.microsoft.schemas.sharepoint.soap.WebsSoapStub _stub = new com.microsoft.schemas.sharepoint.soap.WebsSoapStub(endPoint, this);
       _stub.setPortName(getWebsSoapWSDDServiceName());
       _stub.setUsername( userName );
       _stub.setPassword( password );
       if (myFactory != null)
         _stub._setProperty( PROTOCOL_FACTORY_PROPERTY, myFactory );
       if (connectionManager != null)
         _stub._setProperty( CONNECTION_MANAGER_PROPERTY, connectionManager );
       return _stub;
     }
   }
 
   /** Implementation of EngineConfiguration that we'll use to get the wsdd file from a
   * local resource.
   */
   protected static class ResourceProvider implements WSDDEngineConfiguration
   {
     private WSDDDeployment deployment = null;
 
     private Class resourceClass;
     private String resourceName;
 
     /**
      * Constructor setting the resource name.
      */
     public ResourceProvider(Class resourceClass, String resourceName)
     {
       this.resourceClass = resourceClass;
       this.resourceName = resourceName;
     }
 
     public WSDDDeployment getDeployment() {
         return deployment;
     }
 
     public void configureEngine(AxisEngine engine)
       throws ConfigurationException
     {
       try
       {
         InputStream resourceStream = resourceClass.getResourceAsStream(resourceName);
         if (resourceStream == null)
           throw new ConfigurationException("Resource not found: '"+resourceName+"'");
         try
         {
           WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(resourceStream));
           deployment = doc.getDeployment();
 
           deployment.configureEngine(engine);
           engine.refreshGlobalOptions();
         }
         finally
         {
           resourceStream.close();
         }
       }
       catch (ConfigurationException e)
       {
         throw e;
       }
       catch (Exception e)
       {
         throw new ConfigurationException(e);
       }
     }
 
     public void writeEngineConfig(AxisEngine engine)
       throws ConfigurationException
     {
       // Do nothing
     }
 
     /**
      * retrieve an instance of the named handler
      * @param qname XXX
      * @return XXX
      * @throws ConfigurationException XXX
      */
     public Handler getHandler(QName qname) throws ConfigurationException
     {
       return deployment.getHandler(qname);
     }
 
     /**
      * retrieve an instance of the named service
      * @param qname XXX
      * @return XXX
      * @throws ConfigurationException XXX
      */
     public SOAPService getService(QName qname) throws ConfigurationException
     {
       SOAPService service = deployment.getService(qname);
       if (service == null)
       {
         throw new ConfigurationException(Messages.getMessage("noService10",
           qname.toString()));
       }
       return service;
     }
 
     /**
      * Get a service which has been mapped to a particular namespace
      *
      * @param namespace a namespace URI
      * @return an instance of the appropriate Service, or null
      */
     public SOAPService getServiceByNamespaceURI(String namespace)
       throws ConfigurationException
     {
       return deployment.getServiceByNamespaceURI(namespace);
     }
 
     /**
      * retrieve an instance of the named transport
      * @param qname XXX
      * @return XXX
      * @throws ConfigurationException XXX
      */
     public Handler getTransport(QName qname) throws ConfigurationException
     {
       return deployment.getTransport(qname);
     }
 
     public TypeMappingRegistry getTypeMappingRegistry()
         throws ConfigurationException
     {
       return deployment.getTypeMappingRegistry();
     }
 
     /**
      * Returns a global request handler.
      */
     public Handler getGlobalRequest() throws ConfigurationException
     {
       return deployment.getGlobalRequest();
     }
 
     /**
      * Returns a global response handler.
      */
     public Handler getGlobalResponse() throws ConfigurationException
     {
       return deployment.getGlobalResponse();
     }
 
     /**
      * Returns the global configuration options.
      */
     public Hashtable getGlobalOptions() throws ConfigurationException
     {
       WSDDGlobalConfiguration globalConfig = deployment.getGlobalConfiguration();
 
       if (globalConfig != null)
         return globalConfig.getParametersTable();
 
       return null;
     }
 
     /**
      * Get an enumeration of the services deployed to this engine
      */
     public Iterator getDeployedServices() throws ConfigurationException
     {
       return deployment.getDeployedServices();
     }
 
     /**
      * Get a list of roles that this engine plays globally.  Services
      * within the engine configuration may also add additional roles.
      *
      * @return a <code>List</code> of the roles for this engine
      */
     public List getRoles()
     {
       return deployment.getRoles();
     }
   }
 
 }
