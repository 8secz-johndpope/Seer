 package contestWebsite;
 
 import java.io.IOException;
 import java.io.StringWriter;
 import java.net.URLDecoder;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Map;
 import java.util.Properties;
 
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.velocity.VelocityContext;
 import org.apache.velocity.app.Velocity;
 
 import com.google.appengine.api.datastore.DatastoreService;
 import com.google.appengine.api.datastore.DatastoreServiceFactory;
 import com.google.appengine.api.datastore.Entity;
 import com.google.appengine.api.datastore.FetchOptions;
 import com.google.appengine.api.datastore.Query;
 import com.google.appengine.api.datastore.Query.FilterOperator;
 
 @SuppressWarnings("serial")
 public class MainPage extends HttpServlet
 {
 	@SuppressWarnings("deprecation")
 	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException
 	{
 		resp.setContentType("text/html");
 		UserCookie userCookie = UserCookie.getCookie(req);
 		Entity user = null;
 		if(userCookie != null)
 			user = userCookie.authenticateUser();
 		boolean loggedIn = userCookie != null && user != null;
 
 		if(!loggedIn && req.getParameter("refresh") != null && req.getParameter("refresh").equals("1"))
 			resp.sendRedirect("/?refresh=1");
 
 		String cookieContent = "";
 		Properties p = new Properties();
 		p.setProperty("file.resource.loader.path", "html");
 		Velocity.init(p);
 		VelocityContext context = new VelocityContext();
 		context.put("year", Calendar.getInstance().get(Calendar.YEAR));
 		context.put("loggedIn", loggedIn);
 		if(loggedIn)
 		{
 			cookieContent = URLDecoder.decode(userCookie.getValue(), "UTF-8");
 			if(!cookieContent.split("\\$")[0].equals("admin"))
 			{
 				String username = (String) user.getProperty("user-id");
 				String name = (String) user.getProperty("name");
 
 				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
 				Query query = new Query("registration").addFilter("email", FilterOperator.EQUAL, username);
 				Entity reg = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1)).get(0);
 				ArrayList<String> regData = new ArrayList<String>();
 				Map<String, Object> props = reg.getProperties();
 
 				PropNames propNames = new PropNames();
 				for(String key : props.keySet())
 					if(!key.equals("account") && propNames.propNames.get(key) != null)
 						regData.add("<b>" + propNames.propNames.get(key) + "</b>: " + props.get(key));
 				Collections.sort(regData);
 				context.put("regData" , regData);
 				context.put("name", name);
 				context.put("user", user.getProperty("user-id"));
 			}
 			else
 			{
 				context.put("user", user.getProperty("user-id"));
 				context.put("name", "Contest Administrator");
 				context.put("admin", true);
 			}
 		}
 		
 		String[] titles = { "TMSCA State Awards: 2013",
 							"UIL State Math Individual Awards: 2013",
 							"UIL State Math Team Awards: 2013",
 							"Texas A&M University Math Competition: 2013",
 							"University of Houston Math Competition: 2012",
 							"TMSCA State Math Team Awards: 2012",
 							"TMSCA State Science Team Awards: 2012",
 							"TMSCA State Awards: 2012",
 							"Math & Science Club Banquet: 2013",
 							"Math & Science Club Banquet: 2013",
 							"Math & Science Club Banquet: 2013",
 							"Math & Science Club Banquet: 2013",
 							"Math & Science Club Banquet: 2013",
 							"UIL State Team: 2013",
 							"UIL State Team: 2013",
 							"Math & Science Club Co-Presidents: 2014",
 							"UIL State Team: 2013",
 							"UIL State Science Team: 2013",
 							"Math & Science Club Officers: 2013"
 							};
 		String[] captions = { "",
 							"Bobby S. awarded first place in Math by Larry White",
 							"First place: Bobby S., Daniel F., Rik N., Ms. Sarah Cleveland",
 							"",
 							"",
 							"First place: Bobby S., Daniel F., Rik N., Mr. Feng Li",
 							"First place: Bobby S., Greg C., Daniel F., Kevin L., Dr. Drew Poche",
 							"",
 							"",
 							"",
 							"Graduating Seniors with Ms. Sarah Cleveland",
 							"Graduating Seniors with Dr. Drew Poche",
 							"Graduating Seniors",
 							"Siddarth G., Kevin L., Bobby S., Daniel F., Keerthana K., Rik N., Saiesh K., Sushain C.",
 							"Bobby S., Arjun G., Saiesh K., Sushain C., Daniel F., Mitchell H., Siddarth G., Kevin L., Keerthana K., Rik N.",
 							"Keerthana K., Kevin L.",
 							"Siddarth G., Kevin L., Bobby S.",
 							"Roy H., Emily W., Daniel H., Prashant R., Mitchell H., Kaelan Y., Arjun G., Chung H."
 							};
 		context.put("num", Math.min(titles.length, captions.length)-2);
 		context.put("titles", titles);
 		context.put("captions", captions);
 		
 		StringWriter sw = new StringWriter();
 		Velocity.mergeTemplate("main.html", context, sw);
 		sw.close();
 
 		resp.getWriter().print(sw);
 	}
 }
