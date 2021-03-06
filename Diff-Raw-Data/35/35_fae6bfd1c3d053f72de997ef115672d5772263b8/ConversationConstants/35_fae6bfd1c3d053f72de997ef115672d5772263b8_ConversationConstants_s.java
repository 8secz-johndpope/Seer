 /*
  * Copyright  2003-2005 The Apache Software Foundation.
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *
  */
 package org.apache.ws.security.conversation;
 
 /**
  * Class ConversationConstants
  */
 public class ConversationConstants {
 
     public final static int VERSION_05_02 = 1;
     
     public final static int VERSION_05_12 = 2;
     
     public final static int DEFAULT_VERSION = VERSION_05_02; 
     
     /**
      * WS-SecConv Feb 2005 version
      */
     public final static String WSC_NS_05_02 = "http://schemas.xmlsoap.org/ws/2005/02/sc"; 
     
     /**
      * WS-Sx version
      */
     public final static String WSC_NS_05_12 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512";
     
     /**
      * Token type of DerivedKeyToken
      */
     public final static String TOKEN_TYPE_DERIVED_KEY_TOKEN =  "/dk";
     
     /**
      * Token type of SecurityContextToken
      */
     public static final String TOKEN_TYPE_SECURITY_CONTEXT_TOKEN = "/sct";
     
     /**
      * Field WSC_PREFIX
      */
     public final static String WSC_PREFIX = "wsc";
 
     /**
      * Field SECURITY_CONTEXT_TOKEN_LN
      */
     public static final String SECURITY_CONTEXT_TOKEN_LN =
             "SecurityContextToken";
 
     /**
      * Field IDENTIFIER_LN
      */
     public static final String IDENTIFIER_LN = "Identifier";
 
     /**
      * Field EXPIRES_LN
      */
     public static final String EXPIRES_LN = "Expires";
 
     /**
      * Field KEYS_LN
      */
     public static final String KEYS_LN = "Keys";
 
     /**
      * Field SECURITY_TOKEN_REFERENCE_LN
      */
     public static final String SECURITY_TOKEN_REFERENCE_LN =
             "SecurityTokenReference";
 
     /**
      * Field DERIVED_KEY_TOKEN_LN
      */
     public static final String DERIVED_KEY_TOKEN_LN = "DerivedKeyToken";
 
     /**
      * Field PROPERTIES_LN
      */
     public static final String PROPERTIES_LN = "Properties";
 
     /**
      * Field LENGTH_LN
      */
     public static final String LENGTH_LN = "Length";
 
     /**
      * Field GENERATION_LN
      */
     public static final String GENERATION_LN = "Generation";
 
     /**
      * Field OFFSET_LN
      */
     public static final String OFFSET_LN = "Offset";
 
     /**
      * Field LABEL_LN
      */
     public static final String LABEL_LN = "Label";
 
     /**
      * Field NONCE_LN
      */
     public static final String NONCE_LN = "Nonce";
 
     public static final int DIRECT_GENERATED = 1;
     public static final int STS_GENERATED = 2;
     public static final int STSREQUEST_TOKEN = 3;
     public static final int INTEROP_SCENE1 = 4;
 
     public static final String IDENTIFIER = "SCT_Identifier";
 
     public static final int DK_SIGN = 1;
     public static final int DK_ENCRYPT = 2;
     
     public final static String DEFAULT_LABEL = "WS-SecureConversation";
     
     public interface DerivationAlgorithm {
         public final static String P_SHA_1 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";
     }
     
     public static String getWSCNs(int version) throws ConversationException {
         if(VERSION_05_02 == version) {
             return WSC_NS_05_02;
         } else if(VERSION_05_12 == version) {
             return WSC_NS_05_12;
         } else {
             throw new ConversationException("unsupportedSecConvVersion");
         }
     }
     
     public static int getWSTVersion(String ns) throws ConversationException {
         if(WSC_NS_05_02.equals(ns)) {
             return VERSION_05_02;
         } else if(WSC_NS_05_12.equals(ns)) {
             return VERSION_05_12;
         } else {
             throw new ConversationException("unsupportedSecConvVersion");
         }
     }
 }
