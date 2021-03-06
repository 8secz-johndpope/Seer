 /*
  * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     bstefanescu
  *
  * $Id$
  */
 
 package org.nuxeo.ecm.webengine.model;
 
 import java.util.Locale;
 import java.util.Map;
 import java.util.MissingResourceException;
 import java.util.concurrent.ConcurrentHashMap;
 
 /**
  * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
  *
  */
 public class Messages {
 
     protected Messages parent;
     protected MessagesProvider provider;
    protected Map<String, MessagesBundle> messages;
     protected MessagesBundle defaultMessages;

    protected final static String BUILT_IN_DEFAULT_LANG="en";

     public Messages(Messages parent, MessagesProvider provider) {
         this.parent = parent;
         this.provider = provider;
         this.messages = new ConcurrentHashMap<String, MessagesBundle>();
        String serverDefaultLang = Locale.getDefault().getLanguage();
        this.defaultMessages = getMessagesBundle(serverDefaultLang);
        if ((defaultMessages.messages.size()==0) && (!BUILT_IN_DEFAULT_LANG.equals(serverDefaultLang)))
        {
            this.defaultMessages = getMessagesBundle(BUILT_IN_DEFAULT_LANG);
        }
     }

     public MessagesBundle getMessagesBundle() {
         return defaultMessages;
     }
 
     public MessagesBundle getMessagesBundle(String language) throws MissingResourceException {
         if (language == null) {
             return defaultMessages;
         }
         MessagesBundle bundle = messages.get(language);
         if (bundle == null) {
             Map<String,String> map = provider.getMessages(language);
             MessagesBundle parentBundle = parent != null ? parent.getMessagesBundle(language) : null;
             bundle = new MessagesBundle(parentBundle, map);
             messages.put(language, bundle);
         }
         return bundle;
     }
 
     public Object getObject(String key, String language) throws MissingResourceException {
         MessagesBundle bundle = getMessagesBundle(language);
         if (bundle != null) {
             return bundle.getObject(key);
         }
         throw new MissingResourceException("Can't find resource for bundle "
                 +this.getClass().getName()
                 +", key "+key,
                 Messages.class.getName(),
                 key);
     }
 
     public Object getObject(String key) throws MissingResourceException {
         return getObject(key, null);
     }
 
     public String getString(String key) throws MissingResourceException {
         return getString(key, null);
     }
 
     public String getString(String key, String language) throws MissingResourceException {
         return (String)getObject(key, language);
     }
 
     public String[] getStringArray(String key) throws MissingResourceException {
         return getStringArray(key, null);
     }
 
     public String[] getStringArray(String key, String language) throws MissingResourceException {
         return (String[])getObject(key, language);
     }
 
 }
