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
 
 package com.android.email.mail.store;
 
 import com.android.email.ExchangeUtils;
 import com.android.email.mail.Folder;
 import com.android.email.mail.MessagingException;
 import com.android.email.mail.Store;
 import com.android.email.mail.StoreSynchronizer;
import com.android.email.service.EmailServiceProxy;
import com.android.email.service.IEmailService;
 
 import android.content.Context;
 import android.os.Bundle;
 import android.os.RemoteException;
 import android.text.TextUtils;
 
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.HashMap;
 
 /**
  * Our Exchange service does not use the sender/store model.  This class exists for exactly two
  * purposes, (1) to provide a hook for checking account connections, and (2) to return
  * "AccountSetupExchange.class" for getSettingActivityClass().
  */
 public class ExchangeStore extends Store {
     public static final String LOG_TAG = "ExchangeStore";
 
     private final URI mUri;
     private final ExchangeTransport mTransport;
 
     /**
      * Factory method.
      */
     public static Store newInstance(String uri, Context context, PersistentDataCallbacks callbacks)
             throws MessagingException {
         return new ExchangeStore(uri, context, callbacks);
     }
 
     /**
      * eas://user:password@server/domain
      *
      * @param _uri
      * @param application
      */
     private ExchangeStore(String _uri, Context context, PersistentDataCallbacks callbacks)
             throws MessagingException {
         try {
             mUri = new URI(_uri);
         } catch (URISyntaxException e) {
             throw new MessagingException("Invalid uri for ExchangeStore");
         }
 
         mTransport = ExchangeTransport.getInstance(mUri, context);
     }
 
     @Override
     public Bundle checkSettings() throws MessagingException {
         return mTransport.checkSettings(mUri);
     }
 
     @Override
     public Folder getFolder(String name) {
         return null;
     }
 
     @Override
     public Folder[] getPersonalNamespaces() {
         return null;
     }
 
     /**
      * Get class of SettingActivity for this Store class.
      * @return Activity class that has class method actionEditIncomingSettings()
      */
     @Override
     public Class<? extends android.app.Activity> getSettingActivityClass() {
         return com.android.email.activity.setup.AccountSetupExchange.class;
     }
 
     /**
      * Get class of sync'er for this Store class.  Because exchange Sync rules are so different
      * than IMAP or POP3, it's likely that an Exchange implementation will need its own sync
      * controller.  If so, this function must return a non-null value.
      *
      * @return Message Sync controller, or null to use default
      */
     @Override
     public StoreSynchronizer getMessageSynchronizer() {
         return null;
     }
 
     /**
      * Inform MessagingController that this store requires message structures to be prefetched
      * before it can fetch message bodies (this is due to EAS protocol restrictions.)
      * @return always true for EAS
      */
     @Override
     public boolean requireStructurePrefetch() {
         return true;
     }
 
     /**
      * Inform MessagingController that messages sent via EAS will be placed in the Sent folder
      * automatically (server-side) and don't need to be uploaded.
      * @return always false for EAS (assuming server-side copy is supported)
      */
     @Override
     public boolean requireCopyMessageToSentFolder() {
         return false;
     }
 
     public static class ExchangeTransport {
         private final Context mContext;
 
         private String mHost;
         private String mDomain;
         private String mUsername;
         private String mPassword;
 
         private static final HashMap<String, ExchangeTransport> sUriToInstanceMap =
             new HashMap<String, ExchangeTransport>();
 
         /**
          * Public factory.  The transport should be a singleton (per Uri)
          */
         public synchronized static ExchangeTransport getInstance(URI uri, Context context)
                 throws MessagingException {
             if (!uri.getScheme().equals("eas") && !uri.getScheme().equals("eas+ssl+") &&
                     !uri.getScheme().equals("eas+ssl+trustallcerts")) {
                 throw new MessagingException("Invalid scheme");
             }
 
             final String key = uri.toString();
             ExchangeTransport transport = sUriToInstanceMap.get(key);
             if (transport == null) {
                 transport = new ExchangeTransport(uri, context);
                 sUriToInstanceMap.put(key, transport);
             }
             return transport;
         }
 
         /**
          * Private constructor - use public factory.
          */
         private ExchangeTransport(URI uri, Context context) throws MessagingException {
             mContext = context.getApplicationContext();
             setUri(uri);
         }
 
         /**
          * Use the Uri to set up a newly-constructed transport
          * @param uri
          * @throws MessagingException
          */
         private void setUri(final URI uri) throws MessagingException {
             mHost = uri.getHost();
             if (mHost == null) {
                 throw new MessagingException("host not specified");
             }
 
             mDomain = uri.getPath();
             if (!TextUtils.isEmpty(mDomain)) {
                 mDomain = mDomain.substring(1);
             }
 
             final String userInfo = uri.getUserInfo();
             if (userInfo == null) {
                 throw new MessagingException("user information not specifed");
             }
             final String[] uinfo = userInfo.split(":", 2);
             if (uinfo.length != 2) {
                 throw new MessagingException("user name and password not specified");
             }
             mUsername = uinfo[0];
             mPassword = uinfo[1];
         }
 
         /**
          * Here's where we check the settings for EAS.
          * @param uri the URI of the account to create
          * @throws MessagingException if we can't authenticate the account
          */
         public Bundle checkSettings(URI uri) throws MessagingException {
             setUri(uri);
             boolean ssl = uri.getScheme().contains("+ssl");
             boolean tssl = uri.getScheme().contains("+trustallcerts");
             try {
                 int port = ssl ? 443 : 80;
                IEmailService svc = ExchangeUtils.getExchangeService(mContext, null);
                // Use a longer timeout for the validate command.  Note that the instanceof check
                // shouldn't be necessary; we'll do it anyway, just to be safe
                if (svc instanceof EmailServiceProxy) {
                    ((EmailServiceProxy)svc).setTimeout(90);
                }
                return svc.validate("eas", mHost, mUsername, mPassword, port, ssl, tssl);
             } catch (RemoteException e) {
                 throw new MessagingException("Call to validate generated an exception", e);
             }
         }
     }
 
     /**
      * We handle AutoDiscover for Exchange 2007 (and later) here, wrapping the EmailService call.
      * The service call returns a HostAuth and we return null if there was a service issue
      */
     @Override
     public Bundle autoDiscover(Context context, String username, String password)
             throws MessagingException {
         try {
             return ExchangeUtils.getExchangeService(context, null)
                 .autoDiscover(username, password);
         } catch (RemoteException e) {
             return null;
         }
     }
 }
