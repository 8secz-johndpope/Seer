 package org.nuxeo.ecm.platform.ui.web.restAPI;
 
 /*
  * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     Nuxeo - initial API and implementation
  *
  * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
  */
 
 import static org.jboss.seam.ScopeType.EVENT;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URLDecoder;
 
import org.apache.myfaces.custom.fileupload.UploadedFileConverter;
import org.apache.myfaces.custom.fileupload.UploadedFileDefaultFileImpl;
 import org.jboss.seam.annotations.In;
 import org.jboss.seam.annotations.Name;
 import org.jboss.seam.annotations.Scope;
 import org.nuxeo.ecm.core.api.Blob;
 import org.nuxeo.ecm.core.api.ClientException;
 import org.nuxeo.ecm.core.api.CoreSession;
 import org.nuxeo.ecm.core.api.DocumentModel;
 import org.nuxeo.ecm.core.api.IdRef;
 import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
 import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
 import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
 import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
 import org.nuxeo.ecm.platform.util.RepositoryLocation;
 import org.nuxeo.runtime.api.Framework;
 import org.restlet.data.Request;
 import org.restlet.data.Response;
 
 /**
  * Restlet to help LiveEdit clients update the blob content of a document
  *
  * @author Sun Tan <stan@nuxeo.com>
  * @author Olivier Grisel <ogrisel@nuxeo.com>
  */
 @Name("uploadFileRestlet")
 @Scope(EVENT)
 public class UploadFileRestlet extends BaseNuxeoRestlet implements
         LiveEditConstants {
 
     @In(create = true)
     protected NavigationContext navigationContext;
 
     protected CoreSession documentManager;
 
     @Override
     public void handle(Request req, Response res) {
 
         String repo = (String) req.getAttributes().get("repo");
         String docid = (String) req.getAttributes().get("docid");
         String fileName = (String) req.getAttributes().get("filename");
         try {
             fileName = URLDecoder.decode(fileName, URL_ENCODE_CHARSET);
         } catch (UnsupportedEncodingException e) {
             handleError(res, e);
             return;
         }
 
         if (repo == null || repo.equals("*")) {
             handleError(res, "you must specify a repository");
             return;
         }
 
         DocumentModel dm = null;
         try {
             navigationContext.setCurrentServerLocation(new RepositoryLocation(
                     repo));
             documentManager = navigationContext.getOrCreateDocumentManager();
             if (docid != null) {
                 dm = documentManager.getDocument(new IdRef(docid));
             }
         } catch (ClientException e) {
             handleError(res, e);
             return;
         }
 
         // find the names of the fields from the optional request parameters
         // with fallback to defaults if none is provided
         String schemaName = getQueryParamValue(req, SCHEMA, DEFAULT_SCHEMA);
         String blobFieldName = getQueryParamValue(req, BLOB_FIELD,
                 DEFAULT_BLOB_FIELD);
         String filenameFieldName = getQueryParamValue(req, FILENAME_FIELD,
                 DEFAULT_FILENAME_FIELD);
 
         try {
            // the stream can be read only once, thus save it first into the
            // core before attempting to detect the mimetype that might rely on
            // binary sniffing
             Blob blob = StreamingBlob.createFromStream(req.getEntity().getStream());
            dm.setProperty(schemaName, blobFieldName, blob);
            dm.setProperty(schemaName, filenameFieldName, fileName);
            dm = documentManager.saveDocument(dm);
 
             // ask the mimetype service for the blob mimetype first according to
            // filename extension with a fallback to binary sniffing
            blob = (Blob) dm.getProperty(schemaName, blobFieldName);
             MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
             String mimetype = mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                     fileName, blob, "application/octet-stream");
             blob.setMimeType(mimetype);
 
            // reset the blob source before saving back since it might have
            // exhausted by the binary sniffing of the MimetypeRegistry service
            // (NB: LazyBlobs fetched from the Core are always resetable)
            blob.getStream().reset();
 
            // re-save the blob with the detected mimetype
            dm.setProperty(schemaName, blobFieldName, blob);
             documentManager.saveDocument(dm);
             documentManager.save();
 
         } catch (Exception e) {
             handleError(res, e);
         }
 
     }
 
 }
