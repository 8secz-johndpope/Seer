 package org.wikimedia.commons.modifications;
 
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class TemplateRemoveModifier extends PageModifier {
 
     public static final String MODIFIER_NAME = "TemplateRemoverModifier";
 
     public static final String PARAM_TEMPLATE_NAME = "template";
 
     public TemplateRemoveModifier(String templateName) {
         super(MODIFIER_NAME);
         try {
             params.putOpt(PARAM_TEMPLATE_NAME, templateName);
         } catch (JSONException e) {
             throw new RuntimeException(e);
         }
     }
 
     public TemplateRemoveModifier(JSONObject data) {
         super(MODIFIER_NAME);
         this.params = data;
     }
 
    // Never get into a land war in Asia.
     @Override
     public String doModification(String pageName, String pageContents) {
         String templateRawName = params.optString(PARAM_TEMPLATE_NAME);
         // Wikitext title normalizing rules. Spaces and _ equivalent
         // They also 'condense' - any number of them reduce to just one (just like HTML)
         String templateNormalized = templateRawName.trim().replaceAll("(\\s|_)+", "(\\s|_)+");
 
         // Not supporting {{ inside <nowiki> and HTML comments yet
         // (Thanks to marktraceur for reminding me of the HTML comments exception)
         Pattern templateStartPattern = Pattern.compile("\\{\\{" + templateNormalized, Pattern.CASE_INSENSITIVE);
         Matcher matcher = templateStartPattern.matcher(pageContents);
 
         while(matcher.find()) {
             int braceCount = 1;
             int startIndex = matcher.start();
             int curIndex = matcher.end();
             while(curIndex < pageContents.length()) {
                if(pageContents.substring(curIndex, curIndex + 2).equals("{{")) {
                     braceCount++;
                } else if(pageContents.substring(curIndex, curIndex + 2).equals("}}")) {
                     braceCount--;
                 }
                 curIndex++;
                 if(braceCount == 0) {
                    curIndex++; // To account for the last brace in the closing }} pair
                     break;
                 }
             }
 
             // Strip trailing whitespace
             while(curIndex < pageContents.length()) {
                 if(pageContents.charAt(curIndex) == ' ' || pageContents.charAt(curIndex) == '\n') {
                     curIndex++;
                 } else {
                     break;
                 }
             }
 
             // I am so going to hell for this, sigh
             pageContents = pageContents.substring(0, startIndex) + pageContents.substring(curIndex);
             matcher = templateStartPattern.matcher(pageContents);
         }
 
         return pageContents;
 
     }
 
     @Override
     public String getEditSumary() {
         return "Removed template " + params.optString(PARAM_TEMPLATE_NAME) + ".";
     }
 }
