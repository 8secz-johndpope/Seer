 package servlets.xgap;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import regressiontest.cluster.DataLoader;
 import app.JDBCDatabase;
 
 public class loadexampledata extends app.servlet.MolgenisServlet
 {
 
 	private static final long serialVersionUID = -6004240016846336249L;
 
 	// private static Logger logger =
 	// Logger.getLogger(LoadDatamodelServlet.class);
 
 	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 	{
 
 		boolean databaseIsAvailable = false;
 		// boolean databaseIsEmpty = false;
 		JDBCDatabase db = null;
 
 		PrintWriter out = response.getWriter();
 		response.setContentType("text/plain");
 
 		try
 		{
 			db = (JDBCDatabase) this.getDatabase();
 			databaseIsAvailable = true;
 		}
 		catch (Exception e)
 		{
 			out.print("Database unavailable.");
 			out.print("\n\n");
 			e.printStackTrace(out);
 		}
 
 		if (databaseIsAvailable)
 		{
 			new DataLoader();
			ArrayList<String> result = DataLoader.load(db);
 			for (String s : result)
 			{
 				out.println(s);
 			}
 		}
 		out.close();
 	}
 }
