 package controllers;
 
 import java.io.Serializable;
 import java.util.List;
 
 import models.FileMove;
 import models.Rule;
 import models.User;
 import play.Logger;
 import play.modules.objectify.Datastore;
 import play.mvc.Controller;
 import play.mvc.With;
 import rules.RuleType;
 
 import com.google.common.collect.Lists;
 import com.googlecode.objectify.Objectify;
 
 import dropbox.Dropbox;
 import dropbox.client.DropboxClient;
 import dropbox.client.DropboxClient.ListingType;
 import dropbox.client.DropboxClientFactory;
 import dropbox.gson.DbxMetadata;
 
 /**
  * @author mustpax
  */
 @With(Login.class)
 public class Application extends Controller {
     
     public static final int MAX_FILE_MOVES = 10;
     
     public static void index() {
         User user = Login.getLoggedInUser();
         InitResult initResult = initSortbox(user);
         List<Rule> rules = Rule.findByUserId(user.id);
         List<FileMove> moves = Lists.newArrayList(FileMove.findByUser(user).limit(MAX_FILE_MOVES));
         render(user, rules, moves, initResult);
     }
     
     public static void dirs(String path) {
         checkAuthenticity();
         DropboxClient client = DropboxClientFactory.create(Login.getLoggedInUser());
         try {
 	        renderJSON(client.listDir(path, ListingType.DIRS));
         } catch (IllegalArgumentException e) {
             badRequest();
         }
     }
     
     public static void forceError() {
         throw new IllegalArgumentException("Just pretending to fail.");
     }
     
     /**
      * @param user the logged in user
      * @return InitResult the result of initialization
      */
     private static InitResult initSortbox(User user) {
         boolean createdSortboxDir = false;
         boolean createdCannedRules = false;
         DropboxClient client = DropboxClientFactory.create(user);
         String sortboxPath = Dropbox.getSortboxPath();
         DbxMetadata file = client.getMetadata(sortboxPath);
         if (file == null) {
             // 1. create missing Sortbox folder
             Logger.info("Sortbox folder missing for user '%s' at path '%s'", user, sortboxPath);
             createdSortboxDir = client.mkdir(sortboxPath) != null;
             if (createdSortboxDir) {
                 // 2. create canned rules
                 createdCannedRules = createCannedRules(user);
             }
         }
         return new InitResult(createdSortboxDir, createdCannedRules);
     }
 
     /**
      * Creates default set of rules if no rules exist in the Sortbox folder.
      * 
      * @param user the logged in user
      * @return true if canned rules are created
      */
     private static boolean createCannedRules(final User user) {
         if (!Rule.ruleExists(user.id)) {
             List<Rule> rules = Lists.newArrayListWithCapacity(3);
             rules.add(new Rule(RuleType.EXT_EQ, "jpg", "/Photos", 0, user.id));
             rules.add(new Rule(RuleType.NAME_CONTAINS, "Essay", "/Documents", 1, user.id));
             rules.add(new Rule(RuleType.GLOB, "Prince*.mp3", "/Music/Prince", 2, user.id));
             Datastore.put(rules);
             return true;
         }
 
         return false;
     }
     
     public static class InitResult implements Serializable {
         /** whether the app newly created the Sortbox directory */
        final public boolean createdSortboxDir;
 
         /** whether the app populated canned rules */
        final public boolean createdCannedRules;
         
         InitResult(boolean createdSortboxDir, boolean createdCannedRules) {
             this.createdSortboxDir = createdSortboxDir;
             this.createdCannedRules = createdCannedRules;
         }
     }
 }
