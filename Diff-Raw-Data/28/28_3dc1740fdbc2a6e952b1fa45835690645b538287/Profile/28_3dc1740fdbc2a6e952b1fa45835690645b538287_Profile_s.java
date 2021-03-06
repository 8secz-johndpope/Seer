 package controllers;
 
 import java.util.ArrayList;
 import java.util.List;
 import models.*;
 
 import org.apache.commons.lang.StringUtils;
 
 import play.Logger;
 import play.data.validation.Email;
 import play.data.validation.MaxSize;
 import play.data.validation.Required;
 import play.data.validation.Validation.ValidationResult;
 
 public class Profile extends PageController {
 
     public static void edit() {
         Member member = Member.findByLogin(Security.connected());
         Logger.info("Edition du profil " + member);
         String originalLogin = member.login;
         render(member, originalLogin);
     }
 
     public static void register(String login) {
         Member member = Member.getPreregistered(login);
         Logger.info("Création du profil %s", member);
         String originalLogin = login;
         render("Profile/edit.html", member, originalLogin);
     }
 
     public static void save(Long id, @Required String originalLogin, @Required String login, String firstname, String lastname, String company, @Required @Email String email, @Required @MaxSize(140) String shortDescription, String longDescription, String twitterName, String googlePlusId,
             String[] interests, String newInterests,
             List<SharedLink> sharedLinks) {
         Logger.info("Save Profile originalLogin {" + originalLogin + "}, firstname {" + firstname + "}, lastname {" + lastname + "}, "
                 + "email {" + email + "}, newInterests {" + newInterests + "}");
 
         boolean registration = (id == null);
         Member member = null;
         if (registration) {
             member = Member.getPreregistered(originalLogin);
         } else {
             member = Member.findById(id);
         }
         
         member.login = login;
         member.firstname = firstname;
         member.shortDescription = shortDescription;
         member.longDescription = longDescription;
         member.email = email;
         member.lastname = lastname;
         member.company = company;
 
         TwitterAccount twitter = member.getTwitterAccount();
         if (StringUtils.isNotBlank(twitterName)) {
             
             final Member other = TwitterAccount.findMemberByScreenName(twitterName);
             if (other != null && !member.equals(other)) {
                 validation.addError("twitterName", "validation.unique", twitterName, other.toString());
             }
 
             if (twitter == null) {
                 member.addAccount(new TwitterAccount(twitterName));
             } else {
                 twitter.screenName = twitterName;
             }
         } else {
             if (twitter != null) {
                 member.removeAccount(twitter);
             }
         }
 
         GoogleAccount google = member.getGoogleAccount();
         if (StringUtils.isNotBlank(googlePlusId)) {
             
             final Member other = GoogleAccount.findMemberByGoogleId(googlePlusId);
             if (other != null && !member.equals(other)) {
                 validation.addError("googlePlusId", "validation.unique", googlePlusId, other.toString());
             }
 
             if (google == null) {
                 member.addAccount(new GoogleAccount(googlePlusId));
             } else {
                 google.googleId = googlePlusId;
             }
         } else {
             if (google != null) {
                 member.removeAccount(google);
             }
         }
 
         if (interests != null) {
             member.updateInterests(interests);
         }
 
         if (newInterests != null) {
             member.addInterests(StringUtils.splitByWholeSeparator(newInterests, ","));
         }
 
         List<SharedLink> validatedSharedLinks = new ArrayList<SharedLink>(sharedLinks.size());
         for (int i = 0; i < sharedLinks.size(); i++) {
             SharedLink link = sharedLinks.get(i);
             if (StringUtils.isNotBlank(link.name) && StringUtils.isNotBlank(link.URL)) {
                 ValidationResult result = validation.valid("sharedLinks[" + i + "]", link);
                 if (result.ok) {
                     validatedSharedLinks.add(link);
                 }
             }
         }
         member.updateSharedLinks(validatedSharedLinks);
 
         Member other = Member.findByLogin(login);
         if (other != null && !member.equals(other)) {
             validation.addError("login", "validation.unique", login);
         }
 
         if (validation.hasErrors()) {
             Logger.error(validation.errors().toString());
            flash.error("Quelques erreurs doivent être corrigées dans ta saisie mon ami!");
             render("Profile/edit.html", member, originalLogin, newInterests, sharedLinks);
         }
 
         if (registration) {
             member.register(originalLogin);
         } else {
             member.updateProfile();
         }
         
         session.put("username", member.login);
 
         flash.success("Profil enregistré!");
         Logger.info("Profil %s enregistré", member.toString());
 
         show(member.login);
     }
 
     public static void show(String login) {
         Member member = Member.fetchForProfile(login);
         member.lookedBy(Member.findByLogin(Security.connected()));
         Logger.info("Show profil %s", member);
         render(member);
     }
 
     public static void delete() throws Throwable {
         Member member = Member.findByLogin(Security.connected());
         member.delete();
         Logger.info("Deleted profile %s", member);
         flash.success("Votre compte a été supprimé");
         Secure.logout();
     }
 
     public static void link(String login, String loginToLink) {
         if (login == null || login.isEmpty()) {
             redirect("/secure/login");
         }
         Member.addLink(login, loginToLink);
         flash.success("Link ajouté!");
         show(loginToLink);
     }
 
     public static void unlink(String login, String loginToLink) {
         if (login == null || login.isEmpty()) {
             redirect("/secure/login");
         }
         Member.removeLink(login, loginToLink);
         flash.success("Link supprimé!");
         show(loginToLink);
     }
 }
