 package net.i2p.router.web;
 
 import java.io.File;
 import java.util.TreeSet;
 import java.util.Set;
 
 public class ConfigUIHelper extends HelperBase {
 
     public String getSettings() {
         StringBuilder buf = new StringBuilder(512);
         String current = _context.getProperty(CSSHelper.PROP_THEME_NAME, CSSHelper.DEFAULT_THEME);
         Set<String> themes = themeSet();
         for (String theme : themes) {
             buf.append("<input type=\"radio\" class=\"optbox\" name=\"theme\" ");
             if (theme.equals(current))
                 buf.append("checked=\"true\" ");
             buf.append("value=\"").append(theme).append("\">").append(_(theme)).append("<br>\n");
         }
         return buf.toString();
     }
 
     /** @return standard and user-installed themes, sorted (untranslated) */
     private Set<String> themeSet() {
          Set<String> rv = new TreeSet();
          // add a failsafe even if we can't find any themes
          rv.add(CSSHelper.DEFAULT_THEME);
          File dir = new File(_context.getBaseDir(), "docs/themes/console");
          File[] files = dir.listFiles();
          if (files == null)
              return rv;
          for (int i = 0; i < files.length; i++) {
              String name = files[i].getName();
              if (files[i].isDirectory() && ! name.equals("images"))
                  rv.add(name);
          }
          return rv;
     }
 
    private static final String langs[] = {"de", "en", "fr", "nl", "ru", "se", "zh"};
    private static final String flags[] = {"de", "us", "fr", "nl", "ru", "se", "cn"};
     private static final String xlangs[] = {_x("German"), _x("English"), _x("French"),
                                            _x("Dutch"), _x("Russian"), _x("Swedish"), _x("Chinese")};
 
     /** todo sort by translated string */
     public String getLangSettings() {
         StringBuilder buf = new StringBuilder(512);
         String current = Messages.getLanguage(_context);
         for (int i = 0; i < langs.length; i++) {
             // we use "lang" so it is set automagically in CSSHelper
             buf.append("<input type=\"radio\" class=\"optbox\" name=\"lang\" ");
             if (langs[i].equals(current))
                 buf.append("checked=\"true\" ");
             buf.append("value=\"").append(langs[i]).append("\">")
                .append("<img height=\"11\" width=\"16\" alt=\"\" src=\"/flags.jsp?c=").append(flags[i]).append("\"> ")
                .append(_(xlangs[i])).append("<br>\n");
         }
         return buf.toString();
     }
 }
