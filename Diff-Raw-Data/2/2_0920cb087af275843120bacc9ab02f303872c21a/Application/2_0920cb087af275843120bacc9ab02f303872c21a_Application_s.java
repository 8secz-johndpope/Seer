 package controllers;
 
 import play.*;
 import play.mvc.*;
 import play.mvc.Http.Header;
 
 import java.util.*;
 import java.util.Map.Entry;
 
 public class Application extends Controller {
 
     public static void index() {
 	dumpRequestHeaders();
 	dumpConfiguration();
	render("login.html");
     }
     
     public static void dump() {
 	dumpRequestHeaders();
 	dumpConfiguration();
     	render("v1.html");
     }
     
     public static void vireo() {
         render("v1.html");
     }
     
     public static void submitIndex() {   
     	dumpParams();    	
         render("v2.html");
     }
     
     public static void submitLicense() {
     	dumpParams();  
         render("v3.html");
     }
     
     public static void submitDocInfo() {
     	dumpParams();  
         render("v4.html");
     }
     public static void submitFileUpload() {
     	dumpParams();  
         render("v5.html");
     }    
     
     public static void vireoAdmin() {
     	dumpParams();  
         render("va1.html");
     } 
     
     public static void settings() {
     	dumpParams();  
     	String page = params.get("page");
     	
     	if (page == null)
     		render("vaSettings.html");
     	
     	if (page.equals("admin")) {
     		Logger.info("Going to admin page");
     		render("vaSettingsAdmin.html");
     	}
         render("vaSettings.html");
     } 
     
     // When a setting changes - we get the symbol that changed:
     // LIST_GRADUATION_SEM= false
     // We should set this in the DB and then just re-render/send the page
     
     public static void postSettings() {
     	dumpParams();  
         render("vaSettings.html");
     } 
     
     private static void dumpConfiguration(){
 	Logger.info(play.Play.configuration.toString());
     }
     
     private static void dumpRequestHeaders() {
        	Logger.info("Headers ------------------");
        	Logger.info(request.toString());
        	Logger.info(session.toString());
        	Map<String, Header> rsp = request.headers;
     	for (Map.Entry<String, Header> entry : rsp.entrySet())    	{
     	    Logger.info(entry.getKey() + "= {" + entry.getValue() + "}");
     	}
     }
     
     private static void dumpParams() {
     	
     	Map<String, String> names = params.allSimple();  	
     	
     	Logger.info(session.toString());
     	
     	for (Map.Entry<String, String> entry : names.entrySet())    	{
     	    Logger.info(entry.getKey() + "= {" + entry.getValue() + "}");
     	}
     }
 }
