 package controllers;
 
 import java.util.*;
 import models.User;
import play.Play;
import play.cache.Cache;
 import play.data.validation.Error;
import play.data.validation.Required;
import play.libs.Codec;
import play.libs.Images;
 import play.mvc.*;
 //import controllers.Secure;
 
 public class Signup extends Controller {
 
   public static void signup() {
    String randomID = Codec.UUID();
    render(randomID);
  }
  
  public static void captcha(String id) {
    Images.Captcha captcha = Images.captcha();
    String code = captcha.getText("#000");
    Cache.set(id, code, "10mn");
    renderBinary(captcha);
  }
  
  public static void get_new_captcha() {
    String randomID = Codec.UUID();
    renderText(randomID);
   }
 
  public static void signup_user(String firstName, String lastName, String username, String email, String email2, String password, String password2,
      @Required(message="Please type the code") String code, String randomID) {
     validation.email(email).message("Please use a valid email address");
     validation.isTrue(email.trim().length() >= 1).message("Please use a valid email address");
     validation.equals(email, email2).message("Email addresses do match" + email + email2);
     validation.isTrue(checkUniqueEmail(email)).message("An account is already attached to this email address.  Did you forget your password?");
     validation.isTrue(firstName.trim().length() >= 1).message("Please enter a first name");
     validation.isTrue(lastName.trim().length() >= 1).message("Please enter a last name");
     username = username.trim();
     validation.isTrue(checkUniqueUsername(username)).message("This username is in use");
     validation.isTrue(username.trim().length() >= 4).message("Username must be at least 4 characters");
     validation.isTrue(password.length() >= 6).message("Password must be at least 6 characters");
     validation.match(password, ".*[A-Z]+.*").message("Password must contain an uppercase letter");
     validation.match(password, ".*[a-z]+.*").message("Password must contain a lowercase letter");
     validation.match(password, "(.*\\W+.*)|(.*[0-9]+.*)").message("Password must contain a non-letter character");
     validation.equals(password, password2).message("Passwords do match");
    if(!Play.id.equals("test")) {
      validation.equals(code, Cache.get(randomID)).message("Invalid code. Please type it again");
    }
     if (validation.hasErrors()) {
       String errors ="";
       for(Error error : validation.errors()) {
         errors += error.message() + "<br>";
       }
       renderText(errors);
     }
     else {
       final User newUser = new User(email, password, username, firstName, lastName).save();
       flash.put("success", "Registration complete!  Login below with your credentials.");
       flash.keep();
       renderText("");
     }
   }
 
   public static void isValidUserName(String name) {
     if (!checkUniqueUsername(name)) {
       renderText("This username has been taken");
     } else if (name.length() < 4) {
       renderText("The username must have at least 4 characters");
     } else {
       renderText("Username available :)");
     }
   }
 
   private static boolean checkUniqueUsername(String name) {
     if (User.count("username = ?", name) > 0) {
       return false;
     } else {
       return true;
     }
   }
 
   private static boolean checkUniqueEmail(String email) {
     if (User.count("email = ?", email) > 0) {
       return false;
     } else {
       return true;
     }
   }
 
   public static void isValidEmail(String email) {
     if (validation.email(email).ok == false || email.trim().length() == 0) {
       renderText("Please use a valid email address");
     }
     else if (!checkUniqueEmail(email)) {
       renderText("An account is already attached to this email address.  Did you forget your password?");
     } else {
       renderText(":)");
     }
   }
 }
