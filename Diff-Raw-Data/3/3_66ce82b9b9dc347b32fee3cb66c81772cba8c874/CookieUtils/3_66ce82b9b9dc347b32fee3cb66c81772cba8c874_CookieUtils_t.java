 package org.ohmage.util;
 
 import java.util.LinkedList;
 import java.util.List;
 
 import javax.servlet.http.Cookie;
 import javax.servlet.http.HttpServletResponse;
 
 /**
  * Utilities to help get or set information from and to cookies.
  * 
  * @author John Jenkins
  */
 public class CookieUtils {
 	/**
 	 * Takes an array of HTTP Cookies and returns a list of values for each of
 	 * the Cookie Objects whose name was 'name'.
 	 *  
 	 * @param cookies An array of Cookies that are to be searched for items 
 	 * 				  with the name 'name'.
 	 * 
 	 * @param name The name, or key, to search for in the list of Cookies of
 	 * 			   which more than one may be present.
 	 * 
 	 * @return Returns a list of values for the 'name' from the 'cookies'
 	 * 		   array.
 	 */
 	public static List<String> getCookieValue(Cookie[] cookies, String name) {
 		List<String> results = new LinkedList<String>();
 		
 		if(cookies != null) {
 			for(int i = 0; i < cookies.length; i++) {
 				if(cookies[i].getName().equals(name)) {
 					results.add(cookies[i].getValue());
 				}
 			}
 		}
 		
 		return results;
 	}
 	
 	/**
 	 * Adds a HTTP Cookie to the response with the given name and value and a
 	 * lifetime.
 	 *  
 	 * @param response The HttpServletResponse to which the Cookie should be
 	 * 				   added.
 	 * 
 	 * @param name The name of the Cookie.
 	 * 
 	 * @param value The value for the Cookie.
 	 * 
 	 * @param lifetimeInSeconds The lifetime of the Cookie in seconds.
 	 */
 	public static void setCookieValue(HttpServletResponse response, String name, String value, int lifetimeInSeconds) {
 		Cookie authTokenCookie = new Cookie(name, value);
 		authTokenCookie.setMaxAge(lifetimeInSeconds);
 		authTokenCookie.setPath("/app");
 		response.addCookie(authTokenCookie);
 	}
}
