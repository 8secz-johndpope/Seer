 /*
  * Copyright (C) 2009 The Android Open Source Project
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
 
 package com.android.mms;
 
 import java.io.IOException;
 
 import org.xmlpull.v1.XmlPullParser;
 import org.xmlpull.v1.XmlPullParserException;
 
 import android.content.Context;
 import android.content.res.XmlResourceParser;
 import android.net.Uri;
 import android.util.Config;
 import android.util.Log;
 
 import com.android.internal.util.XmlUtils;
 
 public class MmsConfig {
     private static final String TAG = "MmsConfig";
     private static final boolean DEBUG = false;
     private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;
 
     private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
     private static final String DEFAULT_USER_AGENT = "Android-Mms/0.1";
 
     /**
      * Whether to hide MMS functionality from the user (i.e. SMS only).
      */
     private static int mMmsEnabled = -1;        // an int so we can tell whether it's been inited
     private static int mMaxMessageSize = 0;
     private static String mUserAgent = null;
     private static String mUaProfTagName = null;
     private static String mUaProfUrl = null;
     private static String mHttpParams = null;
     private static String mHttpParamsLine1Key = null;
     private static int mMaxImageHeight = 0;
     private static int mMaxImageWidth = 0;
    
     public static void init(Context context) {
         if (LOCAL_LOGV) {
             Log.v(TAG, "MmsConfig.init()");
         }
 
         loadMmsSettings(context);
     }
     
     public static boolean getMmsEnabled() {
         return mMmsEnabled == 1 ? true : false;
     }
     
     public static int getMaxMessageSize() {
         return mMaxMessageSize;
     }
     
     public static String getUserAgent() {
         return mUserAgent;
     }
 
     public static String getUaProfTagName() {
         return mUaProfTagName;
     }
 
     public static String getUaProfUrl() {
         return mUaProfUrl;
     }
 
     public static String getHttpParams() {
         return mHttpParams;
     }
 
     public static String getHttpParamsLine1Key() {
         return mHttpParamsLine1Key;
     }
 
     public static int getMaxImageHeight() {
         return mMaxImageHeight;
     }
     
     public static int getMaxImageWidth() {
         return mMaxImageWidth;
     }
 
     private static void loadMmsSettings(Context context) {
         XmlResourceParser parser = context.getResources()
                 .getXml(R.xml.mms_config);
 
         try {
             XmlUtils.beginDocument(parser, "mms_config");
             
             while (true) {
                 XmlUtils.nextElement(parser);
                 String tag = parser.getName();
                 if (tag == null) {
                     break;
                 }
                 String name = parser.getAttributeName(0);
                 String value = parser.getAttributeValue(0);
                 String text = null;
                 if (parser.next() == XmlPullParser.TEXT) {
                     text = parser.getText();
                 }
 
                 if ("name".equalsIgnoreCase(name)) {
                     if ("bool".equals(tag)) {
                         // bool config tags go here
                         if ("enabledMMS".equalsIgnoreCase(value)) {
                             mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                         }
                     } else if ("int".equals(tag)) {
                         // int config tags go here
                         if ("maxMessageSize".equalsIgnoreCase(value)) {
                             mMaxMessageSize = Integer.parseInt(text);
                         } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                             mMaxImageHeight = Integer.parseInt(text);
                         } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                             mMaxImageWidth = Integer.parseInt(text);
                         }
                     } else if ("string".equals(tag)) {
                         // string config tags go here
                         if ("userAgent".equalsIgnoreCase(value)) {
                             mUserAgent = text;
                         } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                             mUaProfTagName = text;
                         } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                             mUaProfUrl = text;
                         } else if ("httpParams".equalsIgnoreCase(value)) {
                             mHttpParams = text;
                         } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                             mHttpParamsLine1Key = text;
                         }
                     }
                 }
             }
         } catch (XmlPullParserException e) {
         } catch (NumberFormatException e) {
         } catch (IOException e) {
         } finally {
             parser.close();
         }
 
         String errorStr = null;
 
         if (mMmsEnabled == -1) {
             errorStr = "enableMMS";
         }
         if (mMaxMessageSize == 0) {
             errorStr = "maxMessageSize";
         }
         if (mMaxImageHeight == 0) {
             errorStr = "maxImageHeight";
         }
         if (mMaxImageWidth == 0) {
             errorStr = "maxImageWidth";
         }
        if (getMmsEnabled() && mUaProfUrl == null) {
             errorStr = "uaProfUrl";
         }
         if (mUaProfTagName == null) {
             mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
         }
         if (mUserAgent == null) {
             mUserAgent = DEFAULT_USER_AGENT;
         }
 
         if (errorStr != null) {
             String err =
                 String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting", 
                         errorStr);
             Log.e(TAG, err);
             throw new ContentRestrictionException(err);
         }
     }
 
 }
