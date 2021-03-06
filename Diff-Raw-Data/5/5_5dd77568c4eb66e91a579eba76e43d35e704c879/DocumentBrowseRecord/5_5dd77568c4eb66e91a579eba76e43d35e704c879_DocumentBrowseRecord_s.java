 package info.papyri.dispatch.browse;
 
 import info.papyri.dispatch.FileUtils;
 import info.papyri.dispatch.LanguageCode;
 import info.papyri.dispatch.browse.facet.StringSearchFacet;
 import info.papyri.dispatch.browse.facet.StringSearchFacet.ClauseRole;
 import info.papyri.dispatch.browse.facet.StringSearchFacet.SearchClause;
 import java.io.UnsupportedEncodingException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLDecoder;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.apache.solr.client.solrj.SolrQuery;
 
 /**
  * The <code>DocumentBrowseRecord</code> class stores summary information regarding
  * documents retrieved during browsing or search.
  * 
  * 
  * @author thill
  */
 public class DocumentBrowseRecord extends BrowseRecord implements Comparable {
 
   private ArrayList<String> itemIds = new ArrayList<String>();
   private String preferredId;
   private URL url;
   private String documentTitle;
   private String place;
   private String date;
   private String language;
   private String translationLanguages;
   private ArrayList<String> imagePaths;
   private Boolean hasIllustration;
   private String highlightString;
   private String solrQueryString;
   private Long position;
   private Long total;
   public Pattern[] highlightTerms;
   private ArrayList<String> highlightWords;
   private FileUtils util;
   
   private SearchClause testClause;
   
   private static IdComparator documentComparator = new IdComparator();
   
   public DocumentBrowseRecord(String prefId, ArrayList<String> ids, URL url, ArrayList<String> titles, String place, String date, String lang, ArrayList<String> imgPaths, String trans, Boolean illus, ArrayList<SearchClause> sts) {
     
     util = new FileUtils("/data/papyri.info/idp.data", "/data/papyri.info/pn/idp.html");
     this.preferredId = tidyPreferredId(prefId);
     this.itemIds = ids;
     this.url = url;
     this.documentTitle = this.tidyTitles(titles);
     this.place = place;
     this.date = date;
     this.language = tidyAncientLanguageCodes(lang);
     this.translationLanguages = tidyModernLanguageCodes(trans);
     this.imagePaths = imgPaths;
     this.hasIllustration = illus;
     this.testClause = sts.size() > 0 ? sts.get(0) : null;
     this.highlightWords = new ArrayList<String>();
     this.highlightTerms = buildHighlightTerms(sts);
     this.highlightString = buildHighlightString(sts);
     
   }
   
   final URL trimURL(URL rawURL){
       
       if(rawURL.toString().length() < 2000) return rawURL;
       String bigURL = rawURL.toString();
       bigURL = bigURL.substring(0, 2000);
       try{
           
           return new URL(bigURL);
           
       } catch(MalformedURLException mue){
        
           try{
           
             return new URL(rawURL.getProtocol(), rawURL.getHost(), rawURL.getPath());
             
           } catch (MalformedURLException mue2){
               
               return null;
               
           }
           
       } 
       
   }
   
   // used for KWIC highlighting
   
   final Pattern[] buildHighlightTerms(ArrayList<SearchClause> searchClauses){
  
       ArrayList<Pattern> hilites = new ArrayList<Pattern>();
       Iterator<SearchClause> stit = searchClauses.iterator();
       while(stit.hasNext()){
           
           try{
               SearchClause searchClause = stit.next();
               String transformedString = searchClause.buildTransformedString();
               if(transformedString == null) continue;
               if("".equals(transformedString)) continue;
               if(searchClause.getAllClauseRoles().contains(ClauseRole.REGEX)){
 
                   String trimmedRegex = trimRegex(transformedString);
                   Pattern[] regexPatterns = matchRegexToDocument(trimmedRegex);
                   hilites.addAll(Arrays.asList(regexPatterns));
 
               }
               else if(searchClause.parseForSearchType() == StringSearchFacet.SearchType.PROXIMITY){
 
                   transformedString = transformedString.replaceAll("(\\d+)w", "");
                   hilites.addAll(Arrays.asList(util.getPhraseHighlightPatterns(transformedString)));
 
               }
               else if(searchClause.parseForSearchType() == StringSearchFacet.SearchType.SUBSTRING){
 
                   Pattern[] patterns = util.getSubstringHighlightPatterns(transformedString);
                   hilites.addAll(Arrays.asList(patterns));
 
               }   
               else{
 
                   Pattern[] patterns = util.getPhraseHighlightPatterns(transformedString);
                   hilites.addAll(Arrays.asList(patterns));
 
               }
 
           }catch(Exception e){}
       }
       Pattern[] patterns = new Pattern[hilites.size()];
       return hilites.toArray(patterns);
       
   }
   
   Pattern[] matchRegexToDocument(String regex){
 
       regex = interpolateTextMarksIntoRegex(regex);
       ArrayList<Pattern> patterns = new ArrayList<Pattern>();
       String fullText = util.loadTextFromId(url.toExternalForm());
       String negatedPrecedingString = getNegatedPrecedingCharacters(regex);
       String negatedFollowingString = getNegatedFollowingChars(regex);
       int precedingIndexAdjustment = negatedPrecedingString.length();
       int followingIndexAdjustment = negatedFollowingString.length();
       regex = util.substituteDiacritics(regex);
       Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNIX_LINES | Pattern.DOTALL);
       Matcher matcher = pattern.matcher(fullText);
       
       while(matcher.find()){
           int startIndex = matcher.start() - precedingIndexAdjustment;
           int endIndex = matcher.end() + followingIndexAdjustment;
           if(startIndex < 0) startIndex = 0;
           if(endIndex > fullText.length()) endIndex = fullText.length();
           String found = fullText.substring(startIndex, endIndex);
           found = anchorAssertionAtStart(found, precedingIndexAdjustment);
           found = anchorAssertionAtEnd(found, followingIndexAdjustment);
           if((precedingIndexAdjustment == 0 || !negatedPrecedingString.equals(found.substring(0, precedingIndexAdjustment + 1))) && (followingIndexAdjustment == 0 || !negatedFollowingString.equals(found.substring(found.length() - followingIndexAdjustment)))){
 
              found = found.replaceAll("([()\\[\\]{}\\.])", ".");
              found = found.replaceAll("\\s{2,}", "\\\\b");
              found = found.replaceAll("\\s", " ");
              highlightWords.add(found);
              Pattern foundPattern = Pattern.compile(found, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNIX_LINES | Pattern.DOTALL);
              patterns.add(foundPattern);        
                        
           }
              
 
       }
 
       Pattern[] arrPatterns = new Pattern[patterns.size()];
       return patterns.toArray(arrPatterns);
       
   }
   
   String getNegatedPrecedingCharacters(String regex){
       
       Pattern pattern = Pattern.compile("^\\(\\?\\<\\!(.*?)\\).*$");
       Matcher matcher = pattern.matcher(regex);
       if(!matcher.matches()) return "";
       String simplifiedPattern = matcher.group(1).replaceAll("\\\\[\\w]", "").replaceAll("[^\\p{L}]", "");
       return simplifiedPattern;
       
   }
   
   String getNegatedFollowingChars(String regex){
 
       Pattern pattern = Pattern.compile("^.*\\(\\?\\!(.*)\\).*$");
       Matcher matcher = pattern.matcher(regex);
       if(!matcher.matches()) return "";
       String simplifiedPattern = matcher.group(1).replaceAll("\\\\[\\w]", "").replaceAll("[^\\p{L}]", "");
       return simplifiedPattern;
       
   }
   
   public String anchorAssertionAtStart(String foundString, int startIndex){
       
       startIndex = startIndex - 1;
       int i;
       Boolean hasWhitespace = false;
 
       for(i = startIndex; i >= 0; i--){
           
           String nowLetter = String.valueOf(foundString.charAt(i));
           if(nowLetter != null && nowLetter.matches("^\\s$")){
 
               hasWhitespace = true;
               break;
           }         
           
       }
       if(hasWhitespace){
           
           return "\\b" + foundString.substring(i + 1);
           
       }
       return foundString;
       
   }
   
   public String anchorAssertionAtEnd(String foundString, int endCount){
       
       int i;
       Boolean hasWhitespace = false;
       
       for(i = foundString.length() - endCount; i < foundString.length(); i++){
           
           String nowLetter = String.valueOf(foundString.charAt(i));
           if(nowLetter != null && nowLetter.matches("^\\s$")){
 
               hasWhitespace = true;
               break;
           }              
           
       }
       if(hasWhitespace){
           
           return foundString.substring(0, i) + "\\b";
           
       }
       
       return foundString;
   }
   
   public String interpolateTextMarksIntoRegex(String regex){
       
       String specialChars = "(\\{|\\}|\\(|\\)|\\d|\\.|-|\\]|\\[)?";
       StringBuilder regexBuilder = new StringBuilder();
       String prevCharacter = "";
       int curlyBracesCount = 0;
       int parensCount = 0;
       Boolean insideNegLookBehind = false;
       
       for(int i = 0; i < regex.length(); i++){
           
           String nowChar = Character.toString(regex.charAt(i));
           String nextChar = i == regex.length() - 1 ? "" : Character.toString(regex.charAt(i + 1));
           if(nowChar.equals("<") && prevCharacter.equals("?") && nextChar.equals("!") && !insideNegLookBehind){
                    
               insideNegLookBehind = true;
               parensCount = 1;
           }
           if(insideNegLookBehind && parensCount == 0) insideNegLookBehind = false;
           regexBuilder.append(nowChar);
           if(!insideNegLookBehind && !prevCharacter.equals("\\") && !prevCharacter.equals("|") && !nextChar.equals("|") &&
               curlyBracesCount == 0 && nowChar.matches("[\\p{L}\\)]")){
 
               regexBuilder.append(specialChars);
               
           }
           prevCharacter = nowChar;
           if("{".equals(nowChar)) curlyBracesCount += 1;
           if("}".equals(nowChar)) curlyBracesCount -= 1;
           if(("(").equals(nowChar) && insideNegLookBehind) parensCount += 1;
           if((")").equals(nowChar) && insideNegLookBehind) parensCount -= 1;
           
       }
       return regexBuilder.toString().trim();
       
   }
 
   
   // used to build the query string (itself used by the Reader class to highlight
   // text in its displayed HTML)
   
   final String buildHighlightString(ArrayList<SearchClause> searchClauses){
       
       StringBuilder hilite = new StringBuilder();
       Iterator<SearchClause> stit = searchClauses.iterator();
       while(stit.hasNext()){
           
           try{
               
               SearchClause searchClause = stit.next();
               String term = searchClause.buildTransformedString();
               ArrayList<ClauseRole> roles = searchClause.getAllClauseRoles();
               if(!"".equals(term)){
                   
                   if(roles.contains(ClauseRole.REGEX)){
 
                       Iterator<String> hwit = highlightWords.iterator();
                       while(hwit.hasNext()){
 
                           String highlightWord = hwit.next();
                           highlightWord = buildRegexHighlightString(highlightWord);
                           try{ highlightWord = URLEncoder.encode(highlightWord, "UTF-8");} 
                           catch(UnsupportedEncodingException uee){}
                           hilite.append(highlightWord);
 
 
                       }
 
                   }
                   else if(searchClause.parseForSearchType() == StringSearchFacet.SearchType.PROXIMITY){
                       
                       String[] termbits = term.split(" ");
                       for(int i = 0; i < termbits.length; i++){
                           
                           String termbit = termbits[i];
                           try{ termbit = URLEncoder.encode(termbit, "UTF-8");} 
                           catch(UnsupportedEncodingException uee){}
                           if(!termbit.matches("\\d+w")){
                               
                               hilite.append(StringSearchFacet.SearchType.PHRASE.name());
                               hilite.append(":(");
                               hilite.append(termbit);
                               hilite.append(")");
                               
                           }
                       
                       }                   
                       
                   }
                   else if (searchClause.parseForSearchType() == StringSearchFacet.SearchType.SUBSTRING){
 
                       hilite.append(StringSearchFacet.SearchType.SUBSTRING.name());
                       hilite.append(":(");
                       try{ term = URLEncoder.encode(term, "UTF-8"); } 
                       catch (UnsupportedEncodingException uee){}
                       hilite.append(term);   
                       hilite.append(")");
 
                   } else{
 
                       hilite.append(StringSearchFacet.SearchType.PHRASE.name());
                       hilite.append(":(");
                       try{ term = URLEncoder.encode(term, "UTF-8"); } 
                       catch (UnsupportedEncodingException uee){}
                       hilite.append(term);
                       hilite.append(")");
 
                   }
                   
               
               }
 
           
           } catch(Exception e){}
           
       }
       
       return hilite.toString();
       
   }
   
   private String buildRegexHighlightString(String highlightWord){
       
       StringBuilder highlighter = new StringBuilder();
       StringSearchFacet.SearchType searchType = highlightWord.contains(" ") ? StringSearchFacet.SearchType.PHRASE : StringSearchFacet.SearchType.SUBSTRING;
       highlighter.append(StringSearchFacet.SearchType.REGEX.name());
       highlighter.append(":(");
       if(searchType == StringSearchFacet.SearchType.PHRASE){
           
           highlighter.append("\"");
           
       }
       highlighter.append(highlightWord);
       if(searchType == StringSearchFacet.SearchType.PHRASE){
           
           highlighter.append("\"");
           
       }
       highlighter.append(")");
       return highlighter.toString();
       
       
   }
   
   private String trimRegex(String rawRegex){
       
       if(rawRegex.length() < 3) return rawRegex;
       String prefix = rawRegex.substring(0, 3);
       String suffix = rawRegex.substring(rawRegex.length() - 3, rawRegex.length());
       String trimmedRegex = rawRegex;
       if("^.*".equals(prefix)) trimmedRegex = trimmedRegex.substring(3);
       if(".*$".equals(suffix)) trimmedRegex = trimmedRegex.substring(0, trimmedRegex.length() - 3);
       return trimmedRegex;
       
   }
   
   
   @Override
   public String getHTML() {
 
     StringBuilder anchor = new StringBuilder();
     anchor.append("<a href='");
     anchor.append(generateLink());    
     anchor.append("'>");
     anchor.append(getDisplayId());
     anchor.append("</a>");
     StringBuilder html = new StringBuilder("<tr class=\"result-record\"><td class=\"identifier\" title=\"");
     html.append(getAlternativeIds());
     html.append("\">");
     html.append(anchor.toString());
     html.append("</td>");
     html.append("<td class=\"doc-title\">");
     html.append(this.getTitleHTML());
     html.append("</td>");
     html.append("<td class=\"display-place\">");
     html.append(place);
     html.append("</td>");
     html.append("<td class=\"display-date\">");
     html.append(date);
     html.append("</td>");
     html.append("<td class=\"language\">");
     html.append(language);
     html.append("</td>");
     html.append("<td class=\"has-translation\">");
     html.append(translationLanguages);
     html.append("</td>");
     html.append("<td class=\"has-images\">");
     html.append(getImageHTML());
     html.append("</td>");
     html.append("</tr>");
     html.append(this.getKWIC());
     return html.toString();
 
   }
   
   public String getDisplayId() {
      
     return preferredId;
     
   }
   
   private String tidyPreferredId(String prefId){
       
       String newId = prefId.replace(" 0 ", " ");
       newId = newId.replace("hgv ", "");
       newId = newId.replace("ddbdp ", "");
       newId = newId.replace("apis ", "");
       
       return newId;
       
   }
  
   
   private String tidyModernLanguageCodes(String languageCodes){
               
         String[] codes = languageCodes.split(",");
         
         // eliminate duplicates
         
         ArrayList<String> previousCodes = new ArrayList<String>();
         
         for(int i = 0; i < codes.length; i++){
             
             String code = codes[i].trim();
             
             if(!previousCodes.contains(code))  previousCodes.add(code);
             
         }
 
         String tidiedCodes = "";
 
         Iterator<String> pcit = previousCodes.iterator();
         while(pcit.hasNext()){
             
             tidiedCodes += pcit.next();
             if(pcit.hasNext()) tidiedCodes += ", ";
             
         }
         
         return tidiedCodes;
            
   }
   
   private String tidyAncientLanguageCodes(String rawLanguageCodes){
       
       String filteredLanguages = LanguageCode.filterModernLanguageCodes(rawLanguageCodes);
       
       String[] splitLanguages = filteredLanguages.split(",");
       
       Collections.sort(Arrays.asList(splitLanguages));
       
       String alphabetized = "";
       
       for(int i = 0; i < splitLanguages.length; i++){
           
           alphabetized += splitLanguages[i];
           
           if(i < splitLanguages.length - 1) alphabetized += ", ";
           
       }
       
       return alphabetized;
       
   }
   
   private String getAlternativeIds(){
       
       String allIds = "";
       
       Iterator<String> ait = itemIds.iterator();
       
       while(ait.hasNext()){
       
           String id = ait.next();
           id = id.replaceAll(" 0 ", " ");
           id = id.replaceAll("ddbdp", "DDbDp:");
           id = id.replaceAll("hgv", "HGV:");
           id = id.replaceAll("apis", "APIS:");
           allIds += " = ";
           allIds += id;
           
       }
       
       if("".equals(allIds)) return "No other identifiers";
       
       return allIds;
   }
   
   private String tidyTitles(ArrayList<String> rawTitles){
       
       ArrayList<String> cleanTitles = new ArrayList<String>();
       for(String rawTitle : rawTitles){
           
           String trimTitle = rawTitle.trim();
           if(!cleanTitles.contains(trimTitle)) cleanTitles.add(trimTitle);
           
       }
       
       String trueTitle = "";
       
       for(String title : cleanTitles){
           
           String compTitle = title.toLowerCase();
           compTitle = compTitle.replaceAll("[\\. ]", "");
       
           Boolean titleNotRedundant = true;
           
           if(preferredId != null){
               
               String compPrefId = preferredId.toLowerCase();
               compPrefId = compPrefId.replaceAll("[\\. ]", "");
               if(compPrefId.equals(compTitle)) titleNotRedundant = false;
               
           }
           for(String id : itemIds){
               
               String compId = id.toLowerCase();
               compId = compId.replaceAll("[\\. ]", "");
               if(compId.equals(compTitle)) titleNotRedundant = false;
           
           }
           
           if(titleNotRedundant) trueTitle += title + ", ";
           
       }
      
      trueTitle = trueTitle.trim();
      if(trueTitle.length() > 0) trueTitle = trueTitle.substring(0, trueTitle.length() - 1);
      return trueTitle;
       
   }
   
   private String getTitleHTML(){
       
       if("".equals(documentTitle)){
           
           return "<div class=\"title-none\">---</div>";
           
       }
       
       StringBuilder html = new StringBuilder();
       
       html.append("<div class=\"title-long\">");
       html.append(documentTitle);
       html.append("</div><!-- closing .title-long -->");
       
       html.append("<div class=\"title-short\" title =\"");
       html.append(documentTitle);
       html.append("\">");
       
       String[] titleBits = documentTitle.split(" ");
       String shortTitle = titleBits[0];
       if(shortTitle.length() < documentTitle.length()) shortTitle += " &hellip;";
       html.append(shortTitle);
       html.append("</div><!-- closing .title-short -->");
       
       return html.toString();      
       
       
   }
 
   /**
    * Allows sorting so that it occurs (in order of priority)
    * 
    * (1) DDbDP before HGV, HGV before APIS
    * (2) Numeric by id
    * (3) Alphabetic by letters occurring after the numeric portion
    * 
    * @param o
    * @return 
    */
   
   @Override
   public int compareTo(Object o) {
 
     DocumentBrowseRecord comparandum = (DocumentBrowseRecord) o;
     String thisId = this.getDisplayId() != null ? this.getDisplayId() : "";
     String thatId = comparandum.getDisplayId() != null ? comparandum.getDisplayId() : "";
 
     return documentComparator.compare(thisId, thatId);
 
   }
   
   private String getHighlightString(){
       
       if("".equals(highlightString)) return "";
       return "/?q=" + highlightString;
       
       
   }
   
   String getKWIC(){
       
       StringBuilder html = new StringBuilder();
       try{
 
           List<String> kwix = util.highlightMatches(util.loadTextFromId(url.toExternalForm()), highlightTerms);
          html.append("<tr class=\"result-text\"><td class=\"kwic\" colspan=\"6\">");
           for(String kwic : kwix){

               html.append(kwic);
               html.append("<br/>\n");
 
           }
           html.append("</td></tr>");
       } catch (Exception e){
           
           // TODO: Need to do something sensible here with regard to highlighting
           
       }
       return html.toString();
       
   }
   
   private String getImageHTML(){
       
       String imageHTML = "";
       Iterator<String> ipit = imagePaths.iterator();
       Boolean hasExternalImgs = false;
       Boolean hasInternalImgs = false;
       String intIndicator = "<span class=\"internal-link-indicator\" title=\"Internal link: Image is available from papyri.info\">Img</span>";
       String extIndicator = "<span class=\"external-link-indicator\" title=\"External link: Links will take you out of papyri.info. Papyri.info thus cannot guarantee their presence or quality.\">Img (ext.)</span>";
       String printIndicator = "<span class=\"illustration-indicator\" title=\"Images reproduced in print publication\">Print</span>";
       while(ipit.hasNext()){
           
           String path = ipit.next();
           if(path.contains("papyri.info")){
               
               hasInternalImgs = true;
               
           }
           else{
               
               hasExternalImgs = true;
           }
           
           
       }
       
       if(hasInternalImgs){
           
           imageHTML += intIndicator;
           
       }
       if(hasExternalImgs){
           
           if(hasInternalImgs) imageHTML += ", ";
           imageHTML += extIndicator;
           
       }
       if(this.hasIllustration){
           
           if(hasInternalImgs || hasExternalImgs) imageHTML += ", ";
           imageHTML += printIndicator;
           
       }
       if(imageHTML.equals("")) imageHTML = "None";
       
       return imageHTML; 
   
   }
   
   private String generateLink(){
       
       StringBuilder html = new StringBuilder();
       
       html.append(url.toString().substring("http://papyri.info".length()));
       html.append(this.getHighlightString());      
       html.append(getSolrQueryString());
       
       return html.toString();
       
   }
   
   /**
    * Sets the Solr data required for the document to generate a Solr query string
    * returning itself and its immediate neighbours in the result set.
    * 
    * Used for linear browsing functionality
    * 
    * @param sq A Solr querystring retrieving the current record and its immediate neighbour(s)
    * @param position The position of this record in the current resultset
    * @param total The total number of records in the current resultset
    */
   
   public void setSolrData(SolrQuery sq, long position, long total){
       
       setSolrQueryString(sq);
       setPosition(position);
       setTotal(total);
       
       
   }
   
   private void setSolrQueryString(SolrQuery sq){
           
       int browserLimit = 2000;
       String sqs = sq.toString();
       
       if(sq == null || sqs.length() > browserLimit){
           
           solrQueryString = "";
           return;
           
       }
       
       solrQueryString = sqs;
       
   }
   
   /**
    * Formats the 
    * 
    * @return 
    */
   
   private String getSolrQueryString(){
       
       if("".equals(solrQueryString)) return "";
       String sq = ("".equals(highlightString) ? "?" : "&") + solrQueryString;
       sq += "&p=" + String.valueOf(position) + "&t=" + String.valueOf(total); 
       return sq;
       
   }
 
   private void setPosition(long p){
       
       position = p;
       
   }
   
   private void setTotal(long t){
       
       total = t;
       
   }
   
    
 }
