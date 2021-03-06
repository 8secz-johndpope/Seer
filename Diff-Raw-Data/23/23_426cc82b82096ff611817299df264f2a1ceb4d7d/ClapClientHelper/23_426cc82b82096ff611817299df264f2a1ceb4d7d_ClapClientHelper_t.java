 /*
  * Copyright 2005-2008 Noelios Consulting.
  * 
  * The contents of this file are subject to the terms of the Common Development
  * and Distribution License (the "License"). You may not use this file except in
  * compliance with the License.
  * 
  * You can obtain a copy of the license at
  * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
  * language governing permissions and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL HEADER in each file and
  * include the License file at http://www.opensource.org/licenses/cddl1.txt If
  * applicable, add the following below this CDDL HEADER, with the fields
  * enclosed by brackets "[]" replaced with your own identifying information:
  * Portions Copyright [yyyy] [name of copyright owner]
  */
 
 package com.noelios.restlet.local;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 import java.util.Date;
 import java.util.logging.Level;
 
 import org.restlet.Client;
 import org.restlet.data.LocalReference;
 import org.restlet.data.Method;
 import org.restlet.data.Protocol;
 import org.restlet.data.Reference;
 import org.restlet.data.Request;
 import org.restlet.data.Response;
 import org.restlet.data.Status;
 import org.restlet.resource.InputRepresentation;
 import org.restlet.resource.Representation;
 import org.restlet.service.MetadataService;
 
 /**
  * Connector to the class loaders.<br>
  * <br>
  * Note that if you use the class authority for your CLAP URIs, you can provide
  * a custom classloader instead of the one of the connector. For this, your
  * requests need to have a "org.restlet.clap.classloader" attribute set with the
  * instance of your classloader.
  * 
  * @author Jerome Louvel (contact@noelios.com)
  */
 public class ClapClientHelper extends LocalClientHelper {
     /**
      * Constructor.
      * 
      * @param client
      *                The client to help.
      */
     public ClapClientHelper(Client client) {
         super(client);
         getProtocols().add(Protocol.CLAP);
     }
 
     /**
      * Handles a call.
      * 
      * @param request
      *                The request to handle.
      * @param response
      *                The response to update.
      */
     @Override
     public void handle(Request request, Response response) {
         String scheme = request.getResourceRef().getScheme();
 
         // Ensure that all ".." and "." are normalized into the path
         // to preven unauthorized access to user directories.
         request.getResourceRef().normalize();
 
         if (scheme.equalsIgnoreCase(Protocol.CLAP.getSchemeName())) {
             LocalReference cr = new LocalReference(request.getResourceRef());
             ClassLoader classLoader = null;
 
             if (cr.getClapAuthorityType() == LocalReference.CLAP_CLASS) {
                 // Sometimes, a specific class loader needs to be used,
                 // make sure that it can be provided as a request's attribute
                 Object classLoaderAttribute = request.getAttributes().get(
                         "org.restlet.clap.classloader");
                 if (classLoaderAttribute != null) {
                     classLoader = (ClassLoader) classLoaderAttribute;
                 } else {
                     classLoader = getClass().getClassLoader();
                 }
             } else if (cr.getClapAuthorityType() == LocalReference.CLAP_SYSTEM) {
                 classLoader = ClassLoader.getSystemClassLoader();
             } else if (cr.getClapAuthorityType() == LocalReference.CLAP_THREAD) {
                 classLoader = Thread.currentThread().getContextClassLoader();
             }
 
             handleClassLoader(request, response, classLoader);
         } else {
             throw new IllegalArgumentException(
                     "Protocol \""
                             + scheme
                             + "\" not supported by the connector. Only CLAP is supported.");
         }
     }
 
     /**
      * Handles a call with a given class loader.
      * 
      * @param request
      *                The request to handle.
      * @param response
      *                The response to update.
      */
     protected void handleClassLoader(Request request, Response response,
             ClassLoader classLoader) {
         MetadataService metadataService = getMetadataService(request);
 
         if (request.getMethod().equals(Method.GET)
                 || request.getMethod().equals(Method.HEAD)) {
             String path = request.getResourceRef().getPath();
            URL url = null;
            Date modificationDate = null;
 
             // Prepare a classloader URI, removing the leading slash
            if ((path != null) && path.startsWith("/")) {
                 path = path.substring(1);
            }

            // Get the URL to the classloader 'resource'
            if (classLoader != null) {
                // As the path may be percent-encoded, it has to be
                // percent-decoded.
                url = classLoader.getResource(Reference.decode(path));
            } else {
                getLogger()
                        .warning(
                                "Unable to get the resource. The selected classloader is null.");
            }
 
             // The ClassLoader returns a directory listing in some cases.
             // As this listing is partial, it is of little value in the context
             // of the CLAP client, so we have to ignore them.
             if (url != null) {
                 if (url.getProtocol().equals("file")) {
                     File file = new File(url.getFile());
                     modificationDate = new Date(file.lastModified());
 
                     if (file.isDirectory()) {
                         url = null;
                     }
                 }
             }
 
             if (url != null) {
                 try {
                     Representation output = new InputRepresentation(url
                             .openStream(), metadataService
                             .getDefaultMediaType());
                     output.setIdentifier(request.getResourceRef());
                     output.setModificationDate(modificationDate);
 
                     // Update the metadata based on file extensions
                     String name = path.substring(path.lastIndexOf('/') + 1);
                     updateMetadata(metadataService, name, output);
 
                     // Update the response
                     response.setEntity(output);
                     response.setStatus(Status.SUCCESS_OK);
                 } catch (IOException ioe) {
                     getLogger().log(Level.WARNING,
                             "Unable to open the representation's input stream",
                             ioe);
                     response.setStatus(Status.SERVER_ERROR_INTERNAL);
                 }
             } else {
                 response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
             }
         } else {
             response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
             response.getAllowedMethods().add(Method.GET);
             response.getAllowedMethods().add(Method.HEAD);
         }
     }
 }
