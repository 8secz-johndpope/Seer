 package contestWebsite;
 
 import java.io.IOException;
 import java.io.StringWriter;
 import java.util.Calendar;
 import java.util.List;
 import java.util.Map;
 
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.velocity.Template;
 import org.apache.velocity.VelocityContext;
 import org.apache.velocity.app.VelocityEngine;
 import org.apache.velocity.runtime.RuntimeConstants;
 
 import util.HTMLCompressor;
 import util.UserCookie;
 
 import com.google.appengine.api.datastore.DatastoreService;
 import com.google.appengine.api.datastore.DatastoreServiceFactory;
 import com.google.appengine.api.datastore.Entity;
 import com.google.appengine.api.datastore.EntityNotFoundException;
 import com.google.appengine.api.datastore.FetchOptions;
 import com.google.appengine.api.datastore.Key;
 import com.google.appengine.api.datastore.KeyFactory;
 import com.google.appengine.api.datastore.Query;
 import com.google.appengine.api.datastore.Query.FilterOperator;
 import com.google.appengine.api.datastore.Transaction;
 import com.google.appengine.api.datastore.TransactionOptions;
 
 @SuppressWarnings("serial")
 public class EditRegistration extends HttpServlet
 {
 	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
 	{
 		VelocityEngine ve = new VelocityEngine();
 		ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "html/pages, html/snippets");
 		ve.init();
 		Template t = ve.getTemplate("editRegistration.html");
 		VelocityContext context = new VelocityContext();
 		
 		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
 
 		UserCookie userCookie = UserCookie.getCookie(req);
 		boolean loggedIn = userCookie != null && userCookie.authenticate();
 
 		if(loggedIn && userCookie.isAdmin())
 		{
 			context.put("user", userCookie.getUsername());
 			context.put("admin", true);
 			context.put("loggedIn", loggedIn);
 
 			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
 			try
 			{
 				Entity registration = datastore.get(key);
 				Map<String, Object> props = registration.getProperties();
 
 				String registrationType = (String) props.get("registrationType");
 				if(registrationType.equals("coach"))
 					context.put("coach", true);
 				else
 					context.put("student", true);
 
 				String schoolLevel = (String) props.get("schoolLevel");
 				if(schoolLevel.equals("middle"))
 					context.put("middle", true);
 				else
 					context.put("high", true);
 
 				String[] subjects = {"N", "C", "M", "S"};
 				String[] numbers = { "", "one", "two", "three", "four", "five", "six", "seven",
 						"eight", "nine", "ten", "eleven", "twelve" };
 
 				for(int i = 6; i <= 12; i++)
 					for(int j = 0; j < 4; j++)
 						context.put(numbers[i] + subjects[j], props.get(i + subjects[j].toLowerCase()));
 
 				if(schoolLevel.equals("middle"))
 					for(int i = 9; i <= 12; i++)
 						for(int j = 0; j < 4; j++)
 							context.put(numbers[i] + subjects[j], 0);
 				else
 					for(int i = 6; i <= 8; i++)
 						for(int j = 0; j < 4; j++)
 							context.put(numbers[i] + subjects[j], 0);
 
 				String account = (String) props.get("account");
 				if(account.equals("yes"))
 					context.put("account", true);
 				else
 					context.put("account", false);
 
 				context.put("schoolName", props.get("schoolName"));
 				context.put("aliases", props.get("aliases"));
 				context.put("name", props.get("name"));
 				context.put("email", props.get("email"));
 				context.put("paid", props.get("paid"));
 
 				Query query = new Query("contestInfo");
 				List<Entity> infos = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
 				if(infos.size() != 0)
 				{
 					Entity info = infos.get(0);
 					if(info.getProperty("price") != null)
 						context.put("price", (Long) info.getProperty("price"));
 					else
 						context.put("price", 5);
 				}
 				else
 					context.put("price", 5);
 
 				context.put("key", key);
 				StringWriter sw = new StringWriter();
 				t.merge(context, sw);
 				sw.close();
 				resp.setContentType("text/html");
 				resp.getWriter().print(HTMLCompressor.customCompress(sw));
 			}
 			catch(EntityNotFoundException e)
 			{ 
 				e.printStackTrace();
 				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 			}
 		}
 		else
 			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
 	}
 
 	@SuppressWarnings({ "unchecked", "deprecation" })
 	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
 	{
 		UserCookie userCookie = UserCookie.getCookie(req);
 		boolean loggedIn = userCookie != null && userCookie.authenticate();
 		if(loggedIn && userCookie.isAdmin())
 		{
 			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
 			Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
			Key key = KeyFactory.createKey("registration", Long.parseLong(req.getParameter("key")));
 			try
 			{
 				Entity registration = datastore.get(key);
 				Map<String, String[]> params = req.getParameterMap();
 
 				if(params.get("delete")[0].equals("yes"))
 				{
 					if(registration.getProperty("account").equals("yes"))
 					{
 						Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
 						Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
 						datastore.delete(user.getKey());
 					}
 					datastore.delete(registration.getKey());
 					txn.commit();
 					resp.sendRedirect("/data?choice=registrations&updated=1");
 				}
 				else
 				{
 					String schoolLevel = params.get("schoolLevel")[0];
 					String name = params.get("name")[0];
 					String schoolName = params.get("schoolName")[0];
 					String email = params.get("email")[0];
 
 					String account = params.get("account")[0];
 					if(registration.getProperty("account").equals("yes") && account.equals("no"))
 					{
 						registration.setProperty("account", "no");
 						Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
 						Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
 						datastore.delete(user.getKey());
 					}
 					else if(registration.getProperty("account").equals("yes"))
 					{
 						Query query = new Query("user").addFilter("user-id", FilterOperator.EQUAL, registration.getProperty("email"));
 						Entity user = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
 						user.setProperty("name", name);
 						user.setProperty("school", schoolName);
 						user.setProperty("schoolLevel", schoolLevel);
 						user.setProperty("user-id", email);
 						datastore.put(user);
 					}
 
 					registration.setProperty("registrationType", params.get("registrationType")[0]);
 					registration.setProperty("schoolName", schoolName);
 					registration.setProperty("schoolLevel", schoolLevel);
 					registration.setProperty("name", name);
 					registration.setProperty("email", email);
 					registration.setProperty("paid", params.get("paid")[0]);
 					if(!params.get("aliases")[0].equals("$aliases"))
 						registration.setProperty("aliases", params.get("aliases")[0]);
 
 					String[] subjects = {"n", "c", "m", "s"};
 					if(schoolLevel.equals("middle"))
 						for(int i = 6; i <= 8; i++)
 							for(int j = 0; j < 4; j++)
 								registration.setProperty(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));
 					else
 						for(int i = 9; i <= 12; i++)
 							for(int j = 0; j < 4; j++)
 								registration.setProperty(i + subjects[j], new Integer(Integer.parseInt(params.get(i + subjects[j])[0])));
 
 					datastore.put(registration);
 					txn.commit();
 				}
 				
 				resp.sendRedirect("/data?choice=registrations&updated=1");
 			}
 			catch(Exception e)
 			{ 
 				e.printStackTrace();
 				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 			}
 			finally
 			{
 				if(txn.isActive())
 					txn.rollback();
 			}
 		}
 		else
 			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
 	}
 }
