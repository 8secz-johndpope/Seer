 package de.tuclausthal.submissioninterface.template.impl;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import de.tuclausthal.submissioninterface.persistence.datamodel.User;
 import de.tuclausthal.submissioninterface.template.Template;
 import de.tuclausthal.submissioninterface.util.HibernateSessionHelper;
 
 /**
  * An template for the TU-Clausthal layout
  * @author Sven Strickroth
  */
 public class TUCTemplate extends Template {
 	public TUCTemplate(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
 		super(servletRequest, servletResponse);
 	}
 
 	@Override
 	public void printTemplateHeader(String title, String breadCrum) throws IOException {
 		servletResponse.setContentType("text/html");
		servletResponse.setCharacterEncoding("iso-8859-1");
 		PrintWriter out = servletResponse.getWriter();
 		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Strict//EN\">");
 		out.println("<html>");
 		out.println("<head>");
 		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + prefix + "/screen.css\" media=\"screen\">");
 		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + prefix + "/si.css\">");
 		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + prefix + "/print.css\" media=\"print\">");
 		out.println("<title>" + title + "</title>");
 		out.println("</head>");
 		out.println("<body>");
 		out.println("<div id=\"aussen\">");
 		out.println("<div id=\"logo\">");
 		out.println("<h1><a href=\"http://www.tu-clausthal.de/\"><img src=\"" + prefix + "/tuc2005.gif\" alt=\"TU Clausthal\" border=\"0\" /></a></h1>");
 		out.println("</div>");
 		out.println("<div id=\"menu\">");
 		User user = sessionAdapter.getUser(HibernateSessionHelper.getSession());
 		if (user != null) {
 			out.println("Benutzer: " + user.getEmail());
 			if (user.isSuperUser()) {
 				out.println(" - <a href=\"" + servletResponse.encodeURL("AdminMenue") + "\">Admin-Men</a>");
 			}
 			out.println(" - <a href=\"" + servletResponse.encodeURL("Logout") + "\">LogOut</a>");
 		} else {
 			out.println("nicht eingeloggt");
 		}
 		//out.println("<a href=\"http://search.tu-clausthal.de/\" target=\"_blank\">Suche</a>");
 		out.println("</div>");
 		out.println("<hr class=\"hide\" />");
 		out.println("<div id=\"page\">");
 		out.println("<div id=\"einrichtung\">");
 		out.println("<h2><a href=\"http://www.in.tu-clausthal.de/\">Institut fr Informatik</a></h2>");
 		out.println("</div>");
 		out.println("<hr class=\"hide\" />");
 		out.println("<div id=\"pfad-wide\">");
 		out.println(breadCrum);
 		out.println("</div>");
 		out.println("<hr class=\"hide\" />");
 		out.println("<div id=\"inhalt-wide\">");
 		out.println("<h1>" + title + "</h1>");
 	}
 
 	@Override
 	public void printTemplateFooter() throws IOException {
 		PrintWriter out = servletResponse.getWriter();
 		out.println("<div style=\"clear: both;\">");
 		out.println("</div>");
 		out.println("</div>");
 		out.println("<hr class=\"hide\" />");
 		out.println("<div id=\"fuss-wide\">");
 		out.println("Layout &copy;&nbsp;TU&nbsp;Clausthal&nbsp;2006&nbsp;&middot;&nbsp;<a href=\"http://www.tu-clausthal.de/info/impressum/\" target=\"_blank\">Impressum</a>");
 		out.println("</div>");
 		out.println("</div>");
 		out.println("</div>");
 		out.println("</body>");
 		out.println("</html>");
 	}
 }
