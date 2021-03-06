 package uk.frequencymobile.flow.server.service;
 
 import java.util.List;
 
 import javax.ws.rs.GET;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.core.Response;
 
 import uk.frequencymobile.flow.server.data.CommentDAO;
 import uk.frequencymobile.flow.server.data.UserDAO;
 import uk.frequencymobile.flow.server.model.Comment;
 import uk.frequencymobile.flow.server.model.Event;
 
 
 @Path("/comment")
 public class CommentService extends GenericService<Comment>{
 
 	CommentDAO commentDao;
 	UserDAO userDao;
 
 	public CommentService() {
 		super(new CommentDAO());
 		commentDao = (CommentDAO)dao;
 		userDao = new UserDAO();
 	}
 	
 	@GET
	@Path("/{id}")
 	public Response findByAuthor(@PathParam("id") long userId) {
 		List<Event> list = commentDao.findByAuthor(userId);
 		String output = list.toString();
 		return Response.status(200).entity(output).build();
 	}
 	
 }
