 package pl.psnc.dl.wf4ever.rosrsfull;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.UnknownHostException;
 import java.rmi.RemoteException;
 import java.sql.SQLException;
 
 import javax.naming.NamingException;
 import javax.servlet.http.HttpServletRequest;
 import javax.ws.rs.Consumes;
 import javax.ws.rs.DELETE;
 import javax.ws.rs.DefaultValue;
 import javax.ws.rs.GET;
 import javax.ws.rs.PUT;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.Produces;
 import javax.ws.rs.QueryParam;
 import javax.ws.rs.core.Context;
 import javax.ws.rs.core.Response;
 import javax.ws.rs.core.Response.Status;
 import javax.ws.rs.core.UriInfo;
 import javax.xml.transform.TransformerException;
 
 import org.apache.log4j.Logger;
 import org.openrdf.rio.RDFFormat;
 
 import pl.psnc.dl.wf4ever.Constants;
 import pl.psnc.dl.wf4ever.auth.AuthenticationException;
 import pl.psnc.dl.wf4ever.auth.ForbiddenException;
 import pl.psnc.dl.wf4ever.auth.SecurityFilter;
 import pl.psnc.dl.wf4ever.connection.DigitalLibraryFactory;
 import pl.psnc.dl.wf4ever.connection.SemanticMetadataServiceFactory;
 import pl.psnc.dl.wf4ever.dlibra.DigitalLibrary;
 import pl.psnc.dl.wf4ever.dlibra.DigitalLibraryException;
 import pl.psnc.dl.wf4ever.dlibra.NotFoundException;
 import pl.psnc.dl.wf4ever.dlibra.ResourceInfo;
 import pl.psnc.dl.wf4ever.dlibra.UserProfile;
 import pl.psnc.dl.wf4ever.sms.SemanticMetadataService;
 import pl.psnc.dlibra.service.AccessDeniedException;
 
 import com.google.common.collect.Multimap;
 import com.sun.jersey.core.header.ContentDisposition;
 
 /**
  * 
  * @author Piotr Hołubowicz
  * 
  */
 @Path("workspaces/{w_id}/ROs/{ro_id}/{v_id}/{filePath: .+}")
 public class AggregatedResource
 {
 
 	private final static Logger logger = Logger.getLogger(AggregatedResource.class);
 
 	@Context
 	private HttpServletRequest request;
 
 	@Context
 	private UriInfo uriInfo;
 
 	@SuppressWarnings("unused")
 	private static final String defaultWorkspaceId = "default";
 
 	@SuppressWarnings("unused")
 	private static final String defaultVersionId = "v1";
 
 
 	@GET
 	@Produces({ "application/x-turtle", "text/turtle"})
 	public Response getResourceTurtle(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original,
 			RDFFormat.TURTLE);
 	}
 
 
 	@GET
 	@Produces("application/x-trig")
 	public Response getResourceTrig(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original,
 			RDFFormat.TRIG);
 	}
 
 
 	@GET
 	@Produces("application/trix")
 	public Response getResourceTrix(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original,
 			RDFFormat.TRIX);
 	}
 
 
 	@GET
 	@Produces("text/rdf+n3")
 	public Response getResourceN3(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original,
 			RDFFormat.N3);
 	}
 
 
 	@GET
 	@Produces("application/rdf+xml")
 	public Response getResourceRdfXml(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original,
 			RDFFormat.RDFXML);
 	}
 
 
 	@GET
 	public Response getResourceAny(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original)
 		throws ClassNotFoundException, IOException, TransformerException, DigitalLibraryException, NotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		return getResource(workspaceId, researchObjectId, versionId, filePath, isContentRequested, original, null);
 	}
 
 
 	private Response getResource(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @DefaultValue("false")
 	@QueryParam("content")
 	boolean isContentRequested, @QueryParam("original")
 	String original, RDFFormat format)
 		throws IOException, TransformerException, DigitalLibraryException, NotFoundException, ClassNotFoundException,
 		NamingException, SQLException, URISyntaxException
 	{
 		UserProfile user = (UserProfile) request.getAttribute(Constants.USER);
 		SemanticMetadataService sms = SemanticMetadataServiceFactory.getService(user);
 
 		URI researchObjectURI = uriInfo.getBaseUriBuilder().path("workspaces").path(workspaceId).path("ROs")
 				.path(researchObjectId).path(versionId).path("/").build();
 		URI resourceURI = uriInfo.getAbsolutePath();
 
 		// check if request is for a specific format
 		if (original != null) {
 			resourceURI = resourceURI.resolve(original);
 			try {
 				if (sms.containsNamedGraph(resourceURI) && sms.isROMetadataNamedGraph(researchObjectURI, resourceURI)) {
 					RDFFormat returnedFormat = RDFFormat.forFileName(getFilename(uriInfo.getAbsolutePath()),
 						RDFFormat.RDFXML);
 					if (format != null && format != returnedFormat) {
 						logger.warn(String.format(
 							"Inconsistent request, filename is %s but Accept is %s, returning %s",
 							getFilename(uriInfo.getAbsolutePath()), format, returnedFormat));
 					}
 					return getNamedGraph(sms, resourceURI, returnedFormat);
 				}
				else {
					return Response.status(Status.NOT_FOUND).type("text/plain").entity("Original resource not found")
							.build();
				}
 			}
 			finally {
 				sms.close();
 			}
 		}
 
 		if (sms.containsNamedGraph(resourceURI) && sms.isROMetadataNamedGraph(researchObjectURI, resourceURI)) {
 			try {
 				RDFFormat extensionFormat = RDFFormat.forFileName(getFilename(uriInfo.getAbsolutePath()));
 				if (extensionFormat != null && extensionFormat == format) {
 					// 1. GET manifest.rdf Accept: application/rdf+xml
 					return getNamedGraph(sms, resourceURI, extensionFormat);
 				}
 				if (extensionFormat != null && format == null) {
 					// 2. GET manifest.rdf
 					return getNamedGraph(sms, resourceURI, extensionFormat);
 				}
 				if (format != null) {
 					// 3. GET manifest.rdf Accept: text/turtle
 					// 4. GET manifest Accept: application/rdf+xml
 					URI formatSpecificURI = createFormatSpecificURI(uriInfo.getAbsolutePath(), extensionFormat, format);
 					return Response.temporaryRedirect(formatSpecificURI).build();
 				}
 				// 5. GET manifest
 				URI formatSpecificURI = createFormatSpecificURI(uriInfo.getAbsolutePath(), extensionFormat,
 					RDFFormat.RDFXML);
 				return Response.temporaryRedirect(formatSpecificURI).build();
 			}
 			finally {
 				sms.close();
 			}
 		}
 
 		try {
 			if (!isContentRequested) {
 				return getResourceMetadata(sms, researchObjectURI, format != null ? format : RDFFormat.RDFXML,
 					resourceURI);
 			}
 			else {
 				if (!sms.isRoFolder(researchObjectURI, resourceURI)) {
 					return getFileContent(workspaceId, researchObjectId, versionId, filePath, user);
 				}
 				else {
 					return getFolderContent(workspaceId, researchObjectId, versionId, filePath, user);
 				}
 			}
 		}
 		finally {
 			sms.close();
 		}
 	}
 
 
 	private URI createFormatSpecificURI(URI absolutePath, RDFFormat extensionFormat, RDFFormat newFormat)
 		throws URISyntaxException
 	{
 		String path = absolutePath.getPath().toString();
 		if (extensionFormat != null) {
 			for (String extension : extensionFormat.getFileExtensions()) {
 				if (path.endsWith(extension)) {
 					path = path.substring(0, path.length() - extension.length() - 1);
 					break;
 				}
 			}
 		}
 		path = path.concat(".").concat(newFormat.getDefaultFileExtension());
 		return new URI(absolutePath.getScheme(), absolutePath.getAuthority(), path,
 				"original=".concat(getFilename(absolutePath)), null);
 	}
 
 
 	private String getFilename(URI uri)
 	{
 		return uri.resolve(".").relativize(uri).toString();
 	}
 
 
 	private Response getResourceMetadata(SemanticMetadataService sms, URI researchObjectURI, RDFFormat format,
 			URI resourceURI)
 		throws ClassNotFoundException, IOException, NamingException, SQLException
 	{
 		InputStream body = sms.getResource(researchObjectURI, resourceURI, format);
 		if (body == null) {
 			return Response.status(Status.NOT_FOUND).type("text/plain").entity("Resource not found").build();
 		}
 		String filename = resourceURI.resolve(".").relativize(resourceURI).toString();
 
 		ContentDisposition cd = ContentDisposition.type(format.getDefaultMIMEType()).fileName(filename).build();
 		return Response.ok(body).header("Content-disposition", cd).build();
 	}
 
 
 	private Response getFolderContent(String workspaceId, String researchObjectId, String versionId, String filePath,
 			UserProfile user)
 		throws RemoteException, DigitalLibraryException, NotFoundException, MalformedURLException, UnknownHostException
 	{
 		DigitalLibrary dl = DigitalLibraryFactory.getDigitalLibrary(user.getLogin(), user.getPassword());
 		InputStream body = dl.getZippedFolder(workspaceId, researchObjectId, versionId, filePath);
 		ContentDisposition cd = ContentDisposition.type("application/zip").fileName(versionId + ".zip").build();
 		return Response.ok(body).header("Content-disposition", cd).build();
 	}
 
 
 	private Response getFileContent(String workspaceId, String researchObjectId, String versionId, String filePath,
 			UserProfile user)
 		throws IOException, RemoteException, DigitalLibraryException, NotFoundException
 	{
 		DigitalLibrary dl = DigitalLibraryFactory.getDigitalLibrary(user.getLogin(), user.getPassword());
 		InputStream body = dl.getFileContents(workspaceId, researchObjectId, versionId, filePath);
 		String mimeType = dl.getFileMimeType(workspaceId, researchObjectId, versionId, filePath);
 
 		String fileName = uriInfo.getPath().substring(1 + uriInfo.getPath().lastIndexOf("/"));
 		ContentDisposition cd = ContentDisposition.type(mimeType).fileName(fileName).build();
 		return Response.ok(body).header("Content-disposition", cd).header("Content-type", mimeType).build();
 	}
 
 
 	private Response getNamedGraph(SemanticMetadataService sms, URI namedGraphURI, RDFFormat format)
 		throws ClassNotFoundException, IOException, NamingException, SQLException
 	{
 		InputStream manifest = sms.getNamedGraph(namedGraphURI, format);
 
 		String fileName = getFilename(uriInfo.getAbsolutePath());
 		ContentDisposition cd = ContentDisposition.type(format.getDefaultMIMEType()).fileName(fileName).build();
 		return Response.ok(manifest).header("Content-disposition", cd).build();
 	}
 
 
 	@PUT
 	@Consumes({ "application/x-turtle", "text/turtle"})
 	public Response createOrUpdateFileTurtle(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, RDFFormat.TURTLE);
 	}
 
 
 	@PUT
 	@Consumes("application/x-trig")
 	public Response createOrUpdateFileTrig(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, RDFFormat.TRIG);
 	}
 
 
 	@PUT
 	@Consumes("application/trix")
 	public Response createOrUpdateFileTrix(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, RDFFormat.TRIX);
 	}
 
 
 	@PUT
 	@Consumes("text/rdf+n3")
 	public Response createOrUpdateFileN3(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, RDFFormat.N3);
 	}
 
 
 	@PUT
 	@Consumes("application/rdf+xml")
 	public Response createOrUpdateFileRdfXml(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, RDFFormat.RDFXML);
 	}
 
 
 	@PUT
 	public Response createOrUpdateFileAny(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original, InputStream data)
 		throws ClassNotFoundException, AccessDeniedException, IOException, NamingException, SQLException,
 		DigitalLibraryException, NotFoundException, URISyntaxException
 	{
 		return createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, original, data, null);
 	}
 
 
 	private Response createOrUpdateFile(String workspaceId, String researchObjectId, String versionId, String filePath,
 			String original, InputStream data, RDFFormat format)
 		throws ClassNotFoundException, IOException, NamingException, SQLException, DigitalLibraryException,
 		NotFoundException, AccessDeniedException, URISyntaxException
 	{
 		UserProfile user = (UserProfile) request.getAttribute(Constants.USER);
 		if (user.getRole() == UserProfile.Role.PUBLIC) {
 			//TODO check permissions in dLibra
 			throw new AuthenticationException("Only authenticated users can do that.", SecurityFilter.REALM);
 		}
 
 		URI researchObjectURI = uriInfo.getBaseUriBuilder().path("workspaces").path(workspaceId).path("ROs")
 				.path(researchObjectId).path(versionId).path("/").build();
 		URI manifestURI = researchObjectURI.resolve(".ro/manifest.rdf");
 		URI resourceURI = uriInfo.getAbsolutePath();
 
 		DigitalLibrary dl = DigitalLibraryFactory.getDigitalLibrary(user.getLogin(), user.getPassword());
 		SemanticMetadataService sms = SemanticMetadataServiceFactory.getService(user);
 
 		if (original != null) {
 			resourceURI = resourceURI.resolve(original);
 			try {
 				if (sms.containsNamedGraph(resourceURI) && sms.isROMetadataNamedGraph(researchObjectURI, resourceURI)) {
 					RDFFormat calculatedFormat = RDFFormat.forFileName(getFilename(uriInfo.getAbsolutePath()),
 						RDFFormat.RDFXML);
 					if (format != null && format != calculatedFormat) {
 						logger.warn(String.format("Inconsistent request, filename is %s but Accept is %s, trying %s",
 							getFilename(uriInfo.getAbsolutePath()), format, calculatedFormat));
 					}
 					filePath = researchObjectURI.relativize(resourceURI).getPath();
 					sms.addNamedGraph(resourceURI, data, calculatedFormat);
 					// update the named graph copy in dLibra, the manifest is not changed
 					updateNamedGraphInDlibra(workspaceId, researchObjectId, versionId, filePath, researchObjectURI, dl,
 						sms, resourceURI);
 					updateROAttributesInDlibra(workspaceId, researchObjectId, versionId, researchObjectURI, dl, sms);
 					return Response.ok().build();
 				}
				else {
					return Response.status(Status.NOT_FOUND).type("text/plain").entity("Original resource not found")
							.build();
				}
 			}
 			finally {
 				sms.close();
 			}
 		}
 
 		if (sms.isROMetadataNamedGraph(researchObjectURI, resourceURI)) {
 			try {
 				RDFFormat extensionFormat = RDFFormat.forFileName(getFilename(uriInfo.getAbsolutePath()));
 				if (extensionFormat != null && extensionFormat == format) {
 					// 1. PUT manifest.rdf Content-Type: application/rdf+xml
 					sms.addNamedGraph(resourceURI, data, extensionFormat);
 					// update the named graph copy in dLibra, the manifest is not changed
 					updateNamedGraphInDlibra(workspaceId, researchObjectId, versionId, filePath, researchObjectURI, dl,
 						sms, resourceURI);
 					updateROAttributesInDlibra(workspaceId, researchObjectId, versionId, researchObjectURI, dl, sms);
 					return Response.ok().build();
 				}
 				if (extensionFormat != null && format == null) {
 					// 2. PUT manifest.rdf
 					sms.addNamedGraph(resourceURI, data, extensionFormat);
 					// update the named graph copy in dLibra, the manifest is not changed
 					updateNamedGraphInDlibra(workspaceId, researchObjectId, versionId, filePath, researchObjectURI, dl,
 						sms, resourceURI);
 					updateROAttributesInDlibra(workspaceId, researchObjectId, versionId, researchObjectURI, dl, sms);
 					return Response.ok().build();
 				}
 				if (format != null) {
 					// 3. PUT manifest.rdf Content-Type: text/turtle
 					// 4. PUT manifest Content-Type: application/rdf+xml
 					URI formatSpecificURI = createFormatSpecificURI(uriInfo.getAbsolutePath(), extensionFormat, format);
 					return Response.temporaryRedirect(formatSpecificURI).build();
 				}
 				// 5. PUT manifest
 				URI formatSpecificURI = createFormatSpecificURI(uriInfo.getAbsolutePath(), extensionFormat,
 					RDFFormat.RDFXML);
 				return Response.temporaryRedirect(formatSpecificURI).build();
 			}
 			finally {
 				sms.close();
 			}
 		}
 
 		try {
 			String contentType = format != null ? format.getDefaultMIMEType() : request.getContentType();
 			ResourceInfo resourceInfo = dl.createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, data,
 				contentType != null ? contentType : "text/plain");
 			sms.addResource(researchObjectURI, resourceURI, resourceInfo);
 			// update the manifest that describes the resource in dLibra
 			updateNamedGraphInDlibra(workspaceId, researchObjectId, versionId, ".ro/manifest.rdf", researchObjectURI,
 				dl, sms, manifestURI);
 		}
 		finally {
 			sms.close();
 		}
 
 		return Response.ok().build();
 	}
 
 
 	/**
 	 * @param workspaceId
 	 * @param researchObjectId
 	 * @param versionId
 	 * @param researchObjectURI
 	 * @param dl
 	 * @param sms
 	 * @throws NotFoundException
 	 * @throws DigitalLibraryException
 	 */
 	private void updateROAttributesInDlibra(String workspaceId, String researchObjectId, String versionId,
 			URI researchObjectURI, DigitalLibrary dl, SemanticMetadataService sms)
 		throws NotFoundException, DigitalLibraryException
 	{
 		Multimap<URI, Object> roAttributes = sms.getAllAttributes(researchObjectURI);
 		roAttributes.put(URI.create("Identifier"), researchObjectURI);
 		dl.storeAttributes(workspaceId, researchObjectId, versionId, roAttributes);
 	}
 
 
 	/**
 	 * @param workspaceId
 	 * @param researchObjectId
 	 * @param versionId
 	 * @param filePath
 	 * @param contentType
 	 * @param researchObjectURI
 	 * @param dl
 	 * @param sms
 	 * @param namedGraphURI
 	 *            TODO
 	 * @throws DigitalLibraryException
 	 * @throws NotFoundException
 	 * @throws AccessDeniedException
 	 */
 	private void updateNamedGraphInDlibra(String workspaceId, String researchObjectId, String versionId,
 			String filePath, URI researchObjectURI, DigitalLibrary dl, SemanticMetadataService sms, URI namedGraphURI)
 		throws DigitalLibraryException, NotFoundException, AccessDeniedException
 	{
 		RDFFormat format = RDFFormat.forFileName(filePath, RDFFormat.RDFXML);
 		InputStream dataStream = sms.getNamedGraphWithRelativeURIs(namedGraphURI, researchObjectURI, format);
 		dl.createOrUpdateFile(workspaceId, researchObjectId, versionId, filePath, dataStream,
 			format.getDefaultMIMEType());
 	}
 
 
 	@DELETE
 	public void deleteFileAny(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, null);
 	}
 
 
 	@DELETE
 	@Consumes("application/rdf+xml")
 	public void deleteFileRdfXml(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, RDFFormat.RDFXML);
 	}
 
 
 	@DELETE
 	@Consumes({ "application/x-turtle", "text/turtle"})
 	public void deleteFileTurtle(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, RDFFormat.TURTLE);
 	}
 
 
 	@DELETE
 	@Consumes("application/x-trig")
 	public void deleteFileTrig(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, RDFFormat.TRIG);
 	}
 
 
 	@DELETE
 	@Consumes("application/trix")
 	public void deleteFileTrix(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, RDFFormat.TRIX);
 	}
 
 
 	@DELETE
 	@Consumes("text/rdf+n3")
 	public void deleteFileN3(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, @QueryParam("original")
 	String original)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		deleteFile(workspaceId, researchObjectId, versionId, filePath, original, RDFFormat.N3);
 	}
 
 
 	private void deleteFile(@PathParam("w_id")
 	String workspaceId, @PathParam("ro_id")
 	String researchObjectId, @PathParam("v_id")
 	String versionId, @PathParam("filePath")
 	String filePath, String original, RDFFormat format)
 		throws DigitalLibraryException, NotFoundException, ClassNotFoundException, IOException, NamingException,
 		SQLException, URISyntaxException
 	{
 		UserProfile user = (UserProfile) request.getAttribute(Constants.USER);
 		if (user.getRole() == UserProfile.Role.PUBLIC) {
 			throw new AuthenticationException("Only authenticated users can do that.", SecurityFilter.REALM);
 		}
 		DigitalLibrary dl = DigitalLibraryFactory.getDigitalLibrary(user.getLogin(), user.getPassword());
 
 		URI researchObjectURI = uriInfo.getBaseUriBuilder().path("workspaces").path(workspaceId).path("ROs")
 				.path(researchObjectId).path(versionId).path("/").build();
 		URI manifestURI = researchObjectURI.resolve(".ro/manifest.rdf");
 		URI resourceURI = uriInfo.getAbsolutePath();
 
 		if (original != null) {
 			resourceURI = resourceURI.resolve(original);
 			filePath = researchObjectURI.relativize(resourceURI).getPath();
 		}
 
 		if (manifestURI.equals(resourceURI)) {
 			throw new ForbiddenException("Can't delete the manifest");
 		}
 
 		dl.deleteFile(workspaceId, researchObjectId, versionId, filePath);
 		SemanticMetadataService sms = SemanticMetadataServiceFactory.getService(user);
 		try {
 			if (sms.isROMetadataNamedGraph(researchObjectURI, resourceURI)) {
 				sms.removeNamedGraph(researchObjectURI, resourceURI);
 			}
 			else {
 				sms.removeResource(researchObjectURI, resourceURI);
 			}
 		}
 		finally {
 			sms.close();
 		}
 	}
 
 }
