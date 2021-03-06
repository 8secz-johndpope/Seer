 package controllers;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import models.Contact;
 import play.mvc.Controller;
 
 import com.google.gson.JsonElement;
 import com.google.gson.JsonParser;
 
 public class Application extends Controller {
 
 	public static void index() {
 		List<Contact> contacts = Contact.findAll();
 		render(contacts);
 	}
 
 	public static void index(List<Contact> contacts) {
 		render(contacts);
 	}
 
 	public static void inviteCallback() {
 
 		new Contact("jwermuth@gmail.com", "Jesper").save();
 		new Contact("whabula@gmail.com", "Whabula").save();
 		new Contact("wrinkle@gmail.com", "John Smith").save();
 
 		index();
 	}
 
	public static void inviteNoArg() {
		System.out.println("invite");
	}

 	public static void invite(String friendsJson) {
 		JsonElement element = new JsonParser().parse(friendsJson);
		System.out.println(element);
 	}
 
 	public static void contactsByEmail(String term) {
 		List<Contact> contacts = Contact.find("byEmailLike", "%" + term + "%").fetch();
 		List<String> emails = new ArrayList<String>();
 		for (Contact contact : contacts) {
 			emails.add(contact.email);
 		}
 		renderJSON(emails);
 	}
 
 }
