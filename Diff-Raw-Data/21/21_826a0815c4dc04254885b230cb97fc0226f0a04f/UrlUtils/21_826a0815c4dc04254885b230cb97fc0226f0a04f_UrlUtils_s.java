 /*
  * Copyright (C) 2010 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.browser;
 
 import android.net.Uri;
 import android.util.Patterns;
 import android.webkit.URLUtil;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Utility methods for Url manipulation
  */
 public class UrlUtils {
 
     static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
             "(?i)" + // switch on case insensitive matching
             "(" +    // begin group for schema
             "(?:http|https|file):\\/\\/" +
            "|(?:inline|data|about|content|javascript):" +
             ")" +
             "(.*)" );
 
     // Google search
     private final static String QUICKSEARCH_G = "http://www.google.com/m?q=%s";
     private final static String QUERY_PLACE_HOLDER = "%s";
 
     // Regular expression which matches http://, followed by some stuff, followed by
     // optionally a trailing slash, all matched as separate groups.
     private static final Pattern STRIP_URL_PATTERN = Pattern.compile("^(http://)(.*?)(/$)?");
 
     private UrlUtils() { /* cannot be instantiated */ }
 
     /**
      * Strips the provided url of preceding "http://" and any trailing "/". Does not
      * strip "https://". If the provided string cannot be stripped, the original string
      * is returned.
      *
      * TODO: Put this in TextUtils to be used by other packages doing something similar.
      *
      * @param url a url to strip, like "http://www.google.com/"
      * @return a stripped url like "www.google.com", or the original string if it could
      *         not be stripped
      */
     /* package */ static String stripUrl(String url) {
         if (url == null) return null;
         Matcher m = STRIP_URL_PATTERN.matcher(url);
         if (m.matches() && m.groupCount() == 3) {
             return m.group(2);
         } else {
             return url;
         }
     }
 
     protected static String smartUrlFilter(Uri inUri) {
         if (inUri != null) {
             return smartUrlFilter(inUri.toString());
         }
         return null;
     }
 
     /**
      * Attempts to determine whether user input is a URL or search
      * terms.  Anything with a space is passed to search.
      *
      * Converts to lowercase any mistakenly uppercased schema (i.e.,
      * "Http://" converts to "http://"
      *
      * @return Original or modified URL
      *
      */
     protected static String smartUrlFilter(String url) {
 
         String inUrl = url.trim();
         boolean hasSpace = inUrl.indexOf(' ') != -1;
 
         Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
         if (matcher.matches()) {
             // force scheme to lowercase
             String scheme = matcher.group(1);
             String lcScheme = scheme.toLowerCase();
             if (!lcScheme.equals(scheme)) {
                 inUrl = lcScheme + matcher.group(2);
             }
             if (hasSpace) {
                 inUrl = inUrl.replace(" ", "%20");
             }
             return inUrl;
         }
         if (!hasSpace) {
             if (Patterns.WEB_URL.matcher(inUrl).matches()) {
                 return URLUtil.guessUrl(inUrl);
             }
         }
 
         // FIXME: Is this the correct place to add to searches?
         // what if someone else calls this function?
 
 //        Browser.addSearchUrl(mBrowser.getContentResolver(), inUrl);
         return URLUtil.composeSearchUrl(inUrl, QUICKSEARCH_G, QUERY_PLACE_HOLDER);
     }
 
     /* package */ static String fixUrl(String inUrl) {
         // FIXME: Converting the url to lower case
         // duplicates functionality in smartUrlFilter().
         // However, changing all current callers of fixUrl to
         // call smartUrlFilter in addition may have unwanted
         // consequences, and is deferred for now.
         int colon = inUrl.indexOf(':');
         boolean allLower = true;
         for (int index = 0; index < colon; index++) {
             char ch = inUrl.charAt(index);
             if (!Character.isLetter(ch)) {
                 break;
             }
             allLower &= Character.isLowerCase(ch);
             if (index == colon - 1 && !allLower) {
                 inUrl = inUrl.substring(0, colon).toLowerCase()
                         + inUrl.substring(colon);
             }
         }
         if (inUrl.startsWith("http://") || inUrl.startsWith("https://"))
             return inUrl;
         if (inUrl.startsWith("http:") ||
                 inUrl.startsWith("https:")) {
             if (inUrl.startsWith("http:/") || inUrl.startsWith("https:/")) {
                 inUrl = inUrl.replaceFirst("/", "//");
             } else inUrl = inUrl.replaceFirst(":", "://");
         }
         return inUrl;
     }
 
 }
