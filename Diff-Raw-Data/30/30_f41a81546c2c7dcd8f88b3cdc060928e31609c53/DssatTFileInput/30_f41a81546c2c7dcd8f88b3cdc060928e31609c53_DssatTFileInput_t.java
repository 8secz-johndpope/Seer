 package org.agmip.translators.dssat;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import org.agmip.core.types.AdvancedHashMap;
 
 /**
  * DSSAT TFile Data I/O API Class
  * 
  * @author Meng Zhang
  * @version 1.0
  */
 public class DssatTFileInput extends DssatCommonInput {
 
     /**
      * Constructor with no parameters
      * Set jsonKey as "observed"
      * 
      */
     public DssatTFileInput() {
         super();
         jsonKey = "observed";
     }
 
     /**
      * DSSAT TFile Data input method for Controller using
      * 
      * @param brMap  The holder for BufferReader objects for all files
      * @return result data holder object
      */
     @Override
     protected AdvancedHashMap readFile(HashMap brMap) throws IOException {
 
         AdvancedHashMap ret = new AdvancedHashMap();
         AdvancedHashMap file = new AdvancedHashMap();
         String line;
         BufferedReader brT;
         LinkedHashMap formats = new LinkedHashMap();
         ArrayList titles = new ArrayList();
         ArrayList obvData = new ArrayList();
         ArrayList obvDataSection = new ArrayList();
         String obvDataKey = "data";  // TODO the key name might change
         String obvFileKey = "time_course";  // TODO the key name might change
 
         brT = (BufferedReader) brMap.get("T");
 
         // If AFile File is no been found
         if (brT == null) {
             // TODO reprot file not exist error
             return ret;
         }
 
         ret.put(obvFileKey, file);
         file.put(obvDataKey, obvData);
         while ((line = brT.readLine()) != null) {
 
             // Get content type of line
             judgeContentType(line);
 
             // Read Observed data
             if (flg[2].equals("data")) {
 
                 // Read meta info
                if (flg[0].equals("meta") && flg[1].equals("")) {
 
                     file.put("meta", line.replaceAll(".*:", "").trim());
 
                 } // Read data info 
                 else {
                     // Set variables' formats
                     formats.clear();
                     for (int i = 0; i < titles.size(); i++) {
                         formats.put(titles.get(i), 6);
                     }
                     // Read line and save into return holder
 
                     // Read line and save into return holder
                     AdvancedHashMap tmp = readLine(line, formats);
                     // translate date from yyddd format to yyyymmdd format
                     tmp.put("date", translateDateStr((String) tmp.get("date")));
                     // Add data to the array
                     String[] keys = {"trno", "date"};
                     addToArray(obvDataSection, tmp, keys);
                 }
 
             } // Read Observed title
             else if (flg[2].equals("title")) {
 
                 titles = new ArrayList();
                 obvDataSection = new ArrayList();
                 obvData.add(obvDataSection);
                 line = line.replaceFirst("@", " ");
                 for (int i = 0; i < line.length(); i += 6) {
                     titles.add(line.substring(i, Math.min(i + 6, line.length())).trim().toLowerCase());
                 }
 
             } else {
             }
         }
 
 //        brT.close();
        compressData(ret);
 
         return ret;
     }
 
     /**
      * Set reading flgs for title lines (marked with *)
      * 
      * @param line  the string of reading line
      */
     @Override
     protected void setTitleFlgs(String line) {
         flg[0] = "meta";
         flg[1] = "";
         flg[2] = "data";
     }
 }
